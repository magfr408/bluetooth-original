package bliptrack.datahandling;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import core.DatabaseException;
import core.Monitor;
import core.Time;
import datum.TripDatum;
import filters.FilterSettings;
import filters.ListTrips;

public class CSVFileReader {

	// the separator in the file from which we read from.
	private String commaSeparator, dateFormat;
	private BufferedReader br;
	// Element indexes...
	private int userIdIdx, startPointNameIdx, endPointNameIdx,
			measurementStartTimeIdx, travelTimeIdx, blueToothDeploymentId;

	private FilterSettings.DataCase dataCase;

	private DatabaseAccessor dbAccessor;

	private TripDatum latestTrip;

	private boolean initialized = false;
	private boolean prefilter;

	private int startReader;
	private int endReader;
	private Float routeLength;
	private float minTT, maxTT;
	private float maxSpeed = 110.0f;
	private float minSpeed = 5.0f;

	public CSVFileReader(DatabaseAccessor dbAccessor, String fileName,
			String commaSeparator, String dateFormat, int userIdIdx,
			int startPointNameIdx, int endPointNameIdx,
			int measurementStartTimeIdx, int travelTimeIdx,
			FilterSettings.DataCase dataCase, int blueToothDeploymentId,
			boolean prefilter) throws FileNotFoundException, IOException {

		this.dbAccessor = dbAccessor;
		this.commaSeparator = commaSeparator;
		this.br = new BufferedReader(new FileReader(fileName));
		this.dateFormat = dateFormat;

		this.userIdIdx = userIdIdx;
		this.startPointNameIdx = startPointNameIdx;
		this.endPointNameIdx = endPointNameIdx;
		this.measurementStartTimeIdx = measurementStartTimeIdx;
		this.travelTimeIdx = travelTimeIdx;

		this.dataCase = dataCase;

		// Read past the first line.
		// TODO: Might not be true in all cases.
		this.br.readLine();

		this.blueToothDeploymentId = blueToothDeploymentId;

		this.prefilter = prefilter;
	}

	/**
	 * @param startTime
	 *            the start time of the current window (oldest acceptable
	 *            sample).
	 * @param windowSize
	 *            window size in seconds.
	 * @return A list of all trips within the window. ATTENTION: If the file did
	 *         not contain any data in the given time window, the window will
	 *         increment to startTime+n*windowSize where n is how many windows
	 *         away the next measurement is. This is only noticeable in the
	 *         start times of the TripDatums within the returned arraylist.
	 * @throws IOException
	 *             if something went wrong in the stream.
	 * @throws DatabaseException
	 *             if something went wrong in the database.
	 * @throws NullPointerException
	 *             if there's no such combination of experiment and sensor name
	 *             in the prop table that is indicated by the local data file.
	 */
	public ListTrips readFromFileWithinTimeWindow(Time startTime, Time endTime,
			float windowSize) throws IOException, DatabaseException,
			NullPointerException {

		ArrayList<TripDatum> trips = new ArrayList<TripDatum>();

		if (!this.initialized) {
			TripDatum trip = this.readSingleLine();
			this.initialized = true;
			this.latestTrip = trip.clone();
		}

		if (this.latestTrip.getStartTime().secondsSince(
				endTime.plus(windowSize)) > 0.0f) {
			// Read past the time interval of interest, return null.
			return null;
		} else if (this.latestTrip.getStartTime().secondsSince(
				startTime.plus(windowSize)) > 0.0f) {
			// We have an empty time window. Increase window and continue.
			float diff = this.latestTrip.getStartTime().secondsSince(startTime);

			float nbWindows = (float) Math.floor(diff / windowSize);

			startTime.add(nbWindows * windowSize);

			trips.add(this.latestTrip.clone());
		} else {
			// Otherwise we assume that the time window only increased by one
			// step and add the left-overs from the last round.
			trips.add(this.latestTrip.clone());
		}

		// Then get more data from the file. Break if EOF or if outside of
		// window.
		while (true) {
			TripDatum trip = this.readSingleLine();

			if (trip == null) {
				// EOF break.
				break;
			} else if (startTime.secondsSince(trip.getStartTime()) >= 0.0f) {
				Monitor.out("CSVFileReader: Trip started earlier than the time window. Read past it.");
			} else if (trip.getStartTime().secondsSince(
					startTime.plus(windowSize)) > 0.0f) {
				this.latestTrip = trip.clone();
				if (!this.initialized) {
					this.initialized = true;
				}
				break;
			} else {
				trips.add(trip.clone());
			}
		}

		return new ListTrips(trips);
	}

	public void closeStream() throws IOException {
		this.br.close();
	}

	/**
	 * Sort of an interface to the formatting method that reads data. It will
	 * also prefilter the trips if the settings say so.
	 * 
	 * @return
	 * @throws IOException
	 *             if something went wrong in the stream.
	 * @throws DatabaseException
	 *             if something went wrong with the db.
	 * @throws NullpointerException
	 *             if there's no such sensor pair in the db.
	 */
	private TripDatum readSingleLine() throws IOException, DatabaseException,
			NullPointerException {

		while (true) {
			String currentRow = this.br.readLine();

			if (currentRow == null) {
				// EOF
				return null;
			}

			String[] currentRowArray = currentRow.split(this.commaSeparator);

			TripDatum trip = toTripDatum(currentRowArray,
					this.blueToothDeploymentId);

			// Check if the pre-filter option is checked. If it is,
			// the method will check if the travel time is without bounds.
			if (this.prefilter) {
				if (!prefilter(trip)) {
					return trip;
				}
			} else {
				return trip;
			}
		}
	}

	private boolean prefilter(TripDatum trip) throws DatabaseException {
		if (this.routeLength != null) {
			if (trip.getTravelTime() < this.minTT
					|| trip.getTravelTime() > this.maxTT) {
				return true;
			}
		}
		return false;
	}

	private TripDatum toTripDatum(String[] cRA, int expId)
			throws NullPointerException, DatabaseException {

		String mac_adress = cRA[this.userIdIdx];

		if (!this.initialized) {
			// Try to get the sensor names...
			this.startReader = this.dbAccessor.getSensorIdBySensorNameAndExpId(
					cRA[this.startPointNameIdx], expId);
			this.endReader = this.dbAccessor.getSensorIdBySensorNameAndExpId(
					cRA[this.endPointNameIdx], expId);

			if (this.startReader == -1 || this.endReader == -1) {
				throw new NullPointerException("No such combination of"
						+ " experiment and sensor name in the prop table.");
			}

			if (this.prefilter) {

				this.routeLength = this.dbAccessor.getRouteLength(
						this.startReader, this.endReader);

				this.minTT = this.routeLength / (this.maxSpeed / 3.6f);
				this.maxTT = this.routeLength / (this.minSpeed / 3.6f);
			}

			System.out.println("CSVFileReader is loading trips between sensor "
					+ this.startReader + " and " + this.endReader);
		}

		Time startTime = Utils.timestringToTime(
				cRA[this.measurementStartTimeIdx], this.dateFormat);

		float travelTime = Float.parseFloat(cRA[this.travelTimeIdx]);

		Time endTime = startTime.plus(1.0f * travelTime);

		// The trip is not filtered yet.
		return new TripDatum(mac_adress, this.startReader, this.endReader,
				startTime, endTime, travelTime, null);
	}
}