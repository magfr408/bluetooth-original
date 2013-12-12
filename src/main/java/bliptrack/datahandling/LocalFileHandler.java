/**
 * 
 */
package bliptrack.datahandling;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import core.DatabaseException;
import core.Monitor;
import core.Time;
import datum.TripDatum;
import filters.FilterManager;
import filters.FilterSettings;

/**
 * This class is supposed to read csv-files received from Bliptrack and store
 * the entries to either bluetooth.raw or bluetooth.filtered depending on the
 * type.
 * 
 * @author SEMGFN
 *
 */
public class LocalFileHandler {
	
	//the separator in the file from which we read from.
	String commaSeparator, dateFormat;
	BufferedReader br;
	//Element indexes...
	int userIdIdx, startPointNameIdx, endPointNameIdx, 
			measurementStartTimeIdx, travelTimeIdx;
	DatabaseAccessor dbAccessor;
	boolean writeInDB;
	
	/**
	 * Default constructor if the csv-file has the format RouteName;RouteID;
	 * MeasuredTimeTimestamp;UserId;StartPointNumber;EndPointNumber;
	 * StartPointName;EndPointName;MeasuredTime;ClassOfDevice;outlierLevel
	 * @param fileName
	 * @param commaSeparator
	 * @throws FileNotFoundException
	 */
	public LocalFileHandler(String fileName, boolean writeInDB) 
		throws FileNotFoundException {
		
		this(fileName, 
				";", 
				true, 
				writeInDB, 
				"YYYY-MMM-DD HH:MM:SS", 
				3, 6, 7, 2 ,8);
	}
	
	public LocalFileHandler(String fileName, 
							String commaSeparator,
							boolean onLocalMachine,
							boolean writeInDB,
							String dateFormat,
							int userIdIdx,
							int startPointNameIdx,
							int endPointNameIdx,
							int measurementStartTimeIdx,
							int travelTimeIdx) 
		throws FileNotFoundException {

		this.commaSeparator = commaSeparator;
		this.br = new BufferedReader(new FileReader(fileName));
		this.dateFormat = dateFormat;
		this.writeInDB = writeInDB;
		this.dbAccessor = new DatabaseAccessor(onLocalMachine);
		
		this.userIdIdx = userIdIdx;
		this.startPointNameIdx = startPointNameIdx;
		this.endPointNameIdx = endPointNameIdx;
		this.measurementStartTimeIdx = measurementStartTimeIdx;
		this.travelTimeIdx = travelTimeIdx;
	}
	
	private void readDataFromFileAndSaveToDB() 
		throws FileNotFoundException, IOException, DatabaseException {

		//Read past the first line.
		this.br.readLine();
		//Actually start getting the data
		String currentRow = this.br.readLine();
		
		if(!this.writeInDB) {
			System.out.println(
					"macaddress;startprop;endprop;starttime;traveltime");
		}
		
		//Get the data and store it to working memory
		while (currentRow != null) {
			String[] currentRowArray = currentRow.split(";");
			
			TripDatum trip = convertToTripDatum(currentRowArray, 7);
			if(this.writeInDB) {
				this.dbAccessor.insertTrip(trip);
			} else {
				System.out.println(currentRow);
				System.out.println(trip.getMacAddress() + ";" 
						+ trip.getStartReader() + ";"
						+ trip.getEndReader() + ";" 
						+ trip.getStartTime() + ";"
						+ trip.getTravelTime());
			}
			currentRow = this.br.readLine();
		}
	}
	
	private TripDatum convertToTripDatum(String[] cRA, int expId) 
		throws DatabaseException {
		
		String mac_adress = cRA[this.userIdIdx];
		
		//Try to get the sensor names...
		int startPointId = 
			this.dbAccessor.getSensorIdBySensorNameAndExpId(
					cRA[this.startPointNameIdx], expId);
		int endPointId = 
			this.dbAccessor.getSensorIdBySensorNameAndExpId(
					cRA[this.endPointNameIdx], expId);
		
		if (startPointId == -1 || endPointId == -1) {
			throw new DatabaseException(null, "No such combination of" +
					" experiment and sensor name in the prop table.", 
					this.dbAccessor.dbr,
					"N/A");
		}
		
		int year, month, day, hour, minute, second;
									
		if (this.dateFormat.equals("YYYY-MMM-DD HH:MM:SS")) {
			year =  Integer.parseInt(
					cRA[this.measurementStartTimeIdx].substring(0, 4));
			String monthStr = 
					cRA[this.measurementStartTimeIdx].substring(5, 8);
			
			if (monthStr.equals("jan") || monthStr.equals("JAN")) {
				month = 1;			
			} else if (monthStr.equals("feb") || monthStr.equals("FEB")) {
				month = 2;				
			} else if (monthStr.equals("mar") || monthStr.equals("MAR")) {
				month = 3;				
			} else if (monthStr.equals("apr") || monthStr.equals("APR")) {
				month = 4;				
			} else if (monthStr.equals("may") || monthStr.equals("MAY")) {
				month = 5;				
			} else if (monthStr.equals("jun") || monthStr.equals("JUN")) {
				month = 6;				
			} else if (monthStr.equals("jul") || monthStr.equals("JUL")) {
				month = 7;			
			} else if (monthStr.equals("aug") || monthStr.equals("AUG")) {
				month = 8;				
			} else if (monthStr.equals("sep") || monthStr.equals("SEP")) {
				month = 9;				
			} else if (monthStr.equals("oct") || monthStr.equals("OCT")) {
				month = 10;				
			} else if (monthStr.equals("nov") || monthStr.equals("NOV")) {
				month = 11;				
			} else if (monthStr.equals("dec") || monthStr.equals("DEC")) {
				month = 12;	
			} else {
				//Don't know what to do.
				return null;
			}
			
			day = Integer.parseInt(
					cRA[this.measurementStartTimeIdx].substring(9, 11));
			hour = Integer.parseInt(
					cRA[this.measurementStartTimeIdx].substring(12, 14));
			minute = Integer.parseInt(
					cRA[this.measurementStartTimeIdx].substring(15, 17));
			second = Integer.parseInt(
					cRA[this.measurementStartTimeIdx].substring(18, 20));
		} else {
			//No other cases implemented.
			return null;
		}
		
		Time startTime = Time.newTimeFromBerkeleyDateTime(
				year, 
				month, 
				day, 
				hour, 
				minute,
				second, 
				0);
		
		float travelTime = Float.parseFloat(cRA[this.travelTimeIdx]);
		
		Time endTime = startTime.plus(1.0f*1);

		return new TripDatum(
				mac_adress, 
				startPointId, 
				endPointId, 
				startTime, 
				endTime, 
				travelTime,
				FilterSettings.FilterMethod.IQR);
		}
	
		
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// 1. State the format string
		
		//RouteName	RouteID	MeasuredTimeTimestamp	UserId	StartPointNumber	EndPointNumber	StartPointName	EndPointName	MeasuredTime	ClassOfDevice	outlierLevel
		//E4S 60.645 => E4S 60.060	8639	2013-mar-18 15:39:25	1,57E+11	4004	3241	E4S 60.645	E4S 60.060	34	6160900	0

		// To set host, port and name in CORE
		Monitor.set_db_env("mms");
		Monitor.get_db_env();
		
		String fileName = "C:/Users/SEMGFN/Dropbox/mms_liu/Data/Bluetooth/Bluetooth-E4S_60.645_=-_E4S_60.060_ut_20130317-20130331.csv";
		
		boolean writeInDB = false;
		
		LocalFileHandler myLFH;
		try {
			myLFH = new LocalFileHandler(fileName, writeInDB);

			myLFH.readDataFromFileAndSaveToDB();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (DatabaseException dbe) {
			dbe.printStackTrace();
		}
	}
}
