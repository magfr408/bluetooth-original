package filters;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import bliptrack.datahandling.CSVFileReader;
import bliptrack.datahandling.DatabaseAccessor;

import core.DatabaseException;
import core.Exceptions;
import core.Monitor;
import core.Time;
import datum.TripDatum;

/**
 * The main code for filtering (it is better to have a different run
 * configuration for that)
 * 
 * @author Boris
 * @author Magnus
 */
public class FilterManager {

	CSVFileReader csvFileReader;
	DatabaseAccessor dbAccessor;
	Time currentTime;
	FilterSettings fs;

	/**
	 * Constructor for FilterManager under data case MTF1
	 * 
	 * @param commaSeparator
	 */
	public FilterManager(FilterSettings fs) throws FileNotFoundException,
			DatabaseException, NullPointerException, IOException {

		this.fs = fs;

		this.dbAccessor = new DatabaseAccessor(this.fs.isOnLocalMachine());

		if (this.fs.getStartTime() == null && this.fs.getEndTime() == null) {
			Time newStartTime = this.dbAccessor.getStartTimeOfExp(this.fs
					.getBluetoothDeploymentId());

			Time newEndTime = this.dbAccessor.getEndTimeOfExp(this.fs
					.getBluetoothDeploymentId());

			this.fs.setStartTime(newStartTime);
			this.fs.setEndTime(newEndTime);
		}

		this.currentTime = Time.newTimeFromTime(this.fs.getStartTime());

		if (this.fs.getStartTime() == null || this.fs.getEndTime() == null) {
			throw new NullPointerException("Error reading from db, no start"
					+ " or end time for this experiment.");
		}

		if (this.fs.getDataCase().equals(FilterSettings.DataCase.MTF1)) {

			this.csvFileReader = new CSVFileReader(this.dbAccessor,
					this.fs.getDataFileName(), this.fs.getCommaSeparator(),
					this.fs.getDateFormat(), this.fs.getUserIdIdx(),
					this.fs.getStartPointNameIdx(),
					this.fs.getEndPointNameIdx(),
					this.fs.getMeasurementStartTimeIdx(),
					this.fs.getTravelTimeIdx(), this.fs.getDataCase(),
					this.fs.getBluetoothDeploymentId(), this.fs.prefilter());
		}  else {
			Monitor.info("The only implemented datacase is MTF1.");
			System.exit(0);
		}
	}

	public void runFilter() throws DatabaseException, NullPointerException,
			IOException {

		ListTrips trips = null;

		long tic = System.nanoTime();

		while (currentTime.secondsSince(this.fs.getEndTime()) < 0.0f) {

			if (this.fs.getDataCase().equals(FilterSettings.DataCase.MTF1)) {

				trips = this.csvFileReader.readFromFileWithinTimeWindow(
						this.currentTime, this.fs.getEndTime(),
						this.fs.getTimeWindow());

				if (trips == null) {
					// EOF or outside of time interval.
					break;
				} else {
					newCurrentTime(trips);
					trips.filter(2.5f, this.fs.getFilterMethod());
				}
			} else {
				Monitor.info("The only implemented datacase is MTF1.");
				break;
			}

			if (this.fs.getWriteInDB()) {
				this.writeToDatabase(trips.getTrips());
			} else {
				this.writeToConsole(trips.getTrips());
			}
		}

		Monitor.mon("Done reading trips.");
		this.dbAccessor.closeDBConnections();
		
		//Close streams to file if we are reading from any..
		if (this.fs.getDataCase().equals(FilterSettings.DataCase.MTF1)) {
			this.csvFileReader.closeStream();
		}
	}

	private void writeToConsole(ArrayList<TripDatum> trips) {
		for (TripDatum trip : trips) {
			System.out.println(trip.getMacAddress() + "	"
					+ trip.getStartReader() + "	" + trip.getEndReader() + "	"
					+ trip.getStartTime().toString() + "	"
					+ trip.getTravelTime() + "	" + trip.isOutlier() + "	"
					+ trip.getFilterType());
		}
	}

	private void writeToDatabase(ArrayList<TripDatum> trips)
			throws DatabaseException {

		int nbTrips = 0;
		int nbOutliers = 0;

		long tic = System.nanoTime();
		Monitor.mon("Writing filtered trips in the database.");

		for (TripDatum trip : trips) {
			nbTrips++;
			if (trip.isOutlier()) {
				nbOutliers++;
			}
			dbAccessor.updateTrip(trip);
		}

		long toc = System.nanoTime();
		Monitor.duration("Writing filtered trips", toc - tic);
		Monitor.mon("Writing has terminated");
		Monitor.mon(nbTrips + " trips were filtered , " + nbOutliers
				+ " was marked as outliers.");
	}

	private void newCurrentTime(ListTrips trips) {

		float diff = (Time.newTimeFromTime(trips.getTrips().get(0)
				.getStartTime())).secondsSince(this.currentTime);

		float nbWindows = (float) Math.floor(diff / this.fs.getTimeWindow());

		if (nbWindows == 0) {
			this.currentTime.add(this.fs.getTimeWindow());
		} else {
			this.currentTime.add(nbWindows * this.fs.getTimeWindow());
		}
	}

	public static void main(String[] args) {

		int bluetoothDeploymentId = 8;

		String myDataCase = "MTF1";

		String myFilterMethod = "IQR";
		String path = "C:/Users/SEMGFN/Dropbox/mms_liu/Data/Bluetooth/";
		// Path to data file if the case is MTF#
		String[] dataFile = {
				"Bluetooth-E4S_64.970_=-_E4S_64.090_ut_20130317-20130331.csv",
				"Bluetooth-E4S_64.090_=-_E4S_63.580_ut_20130317-20130331.csv",
				"Bluetooth-E4S_63.580_=-_E4S_63.040_ut_20130317-20130331.csv",
				"Bluetooth-E4S_63.040_=-_E4S_62.220_ut_20130317-20130331.csv",
				"Bluetooth-E4S_62.220_=-_E4S_61.395_ut_20130317-20130331.csv",
				"Bluetooth-E4S_61.395_=-_E4S_60.645_ut_20130317-20130331.csv",
				"Bluetooth-E4S_60.645_=-_E4S_60.060_ut_20130317-20130331.csv" };

		boolean writeInDB = true;
		boolean onLocalMachine = true;

		float timeWindow = 300.0f;

		try {
			String df;
			for (int i = 0; i < dataFile.length; i++) {
				df = path + dataFile[i];
				if (FilterSettings.DataCase.valueOf(myDataCase).equals(
						FilterSettings.DataCase.MTF1)) {

					FilterSettings myFilterSettings = new FilterSettings(
							df,
							";",
							"YYYY-MMM-DD HH:MM:SS",
							3,
							6,
							7,
							2,
							8,
							onLocalMachine,
							writeInDB,
							bluetoothDeploymentId,
							timeWindow,
							FilterSettings.FilterMethod.valueOf(myFilterMethod),
							FilterSettings.DataCase.MTF1, true);

					FilterManager myFM = new FilterManager(myFilterSettings);

					myFM.runFilter();
				} else if (myDataCase.equals(FilterSettings.DataCase.RAW)) {
					// TODO: Implement.
					Monitor.out("RAW is not implemented.");
				} else {
					Monitor.err("No such data environment.");
				}
			}
		} catch (FileNotFoundException fne) {
			Monitor.err(fne, "No such file.");
		} catch (IllegalArgumentException iae) {
			Monitor.err(Exceptions.getStackTraceAndMessage(iae));
		} catch (NullPointerException npe) {
			Monitor.err(Exceptions.getStackTraceAndMessage(npe));
		} catch (DatabaseException dbe) {
			Monitor.err(Exceptions.getStackTraceAndMessage(dbe));
		} catch (IOException ioe) {
			Monitor.err(Exceptions.getStackTraceAndMessage(ioe));
		}
	}
}
