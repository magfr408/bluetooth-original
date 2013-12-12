package bliptrack.datahandling;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import core.DatabaseException;
import core.Monitor;

import datum.BlipTrackObservationDatum;
import datum.TripDatum;
import filters.FilterSettings;

public class RawReader {

	// the separator in the file from which we read from.
	private String commaSeparator, dateFormat, format;
	private BufferedReader br;
	// Element indexes...
	private int blueToothDeploymentId, userIdIdx, timeIdx, sigStrIdx, zoneIdx;

	private static final String format1 = "Insert into EXPORT_TABLE (USERID,TIMESTAMP,RADIOSIGNALSTRENGTH,ZONE) values ('USERID',to_date('TIMESTAMP','YYYY-MM-DD HH24:MI:SS'),'RADIOSIGNALSTRENGTH','ZONE')";
	private static final String format2 = "USERID,TIMESTAMP,RADIOSIGNALSTRENGTH,ZONE";
	private static final String zoneFormat = "peek.E4.#.##_###_BN####_#";
	private DatabaseAccessor dbAccessor;

	public RawReader(DatabaseAccessor dbAccessor, String fileName,
			String commaSeparator, String dateFormat,
			int blueToothDeploymentId, String format, String zoneFormat)
			throws FileNotFoundException, IOException, IllegalArgumentException {

		this.format = format;
		this.dbAccessor = dbAccessor;
		this.commaSeparator = commaSeparator;
		this.br = new BufferedReader(new FileReader(fileName));
		this.dateFormat = dateFormat;

		if (this.format.equals(format1)) {
			// TODO: Might not be true in all cases.
			// Assumes two rows of non-data.
			this.commaSeparator = ",";
			this.br.readLine();
			this.br.readLine();
			this.userIdIdx = 1;
			this.timeIdx = 2;
			this.sigStrIdx = 3;
			this.zoneIdx = 4;
		} else if (this.format.equals(format2)) {
			this.commaSeparator = ",";
			this.br.readLine();
		} else {
			throw new IllegalArgumentException(
					"Unknown format case. Don't know what to do.");
		}

		this.blueToothDeploymentId = blueToothDeploymentId;

		// FORMAT 1: ('USERID',to_date('TIMESTAMP','YYYY-MM-DD
		// HH24:MI:SS'),'RADIOSIGNALSTRENGTH','ZONE');
		// FORMAT 2: 220079126237176,18-APR-13,-81,"peek.E4.N.62_070_BN4006_1"
		// "USERID","TIMESTAMP","RADIOSIGNALSTRENGTH","ZONE"

		// Insert into EXPORT_TABLE (USERID,TIMESTAMP,RADIOSIGNALSTRENGTH,ZONE)
		// values ('13307089817',to_date('2013-03-18 15:38:28','YYYY-MM-DD
		// HH24:MI:SS'),'-82','peek.E4.N.64_970_BN3242_2');
	}

	private BlipTrackObservationDatum readSingleLine() throws IOException,
			DatabaseException, NullPointerException {

		String currentRow = this.br.readLine();

		if (currentRow == null) {
			// EOF
			Monitor.mon("EOF: Closing stream.");
			this.br.close();
			return null;
		} else if (this.format.equals(format1)) {
			// FROM FORMAT: Insert into EXPORT_TABLE
			// (USERID,TIMESTAMP,RADIOSIGNALSTRENGTH,ZONE) values
			// ('13307089817',to_date('2013-03-18 15:38:28','YYYY-MM-DD
			// HH24:MI:SS'),'-82','peek.E4.N.64_970_BN3242_2');
			currentRow
					.replace(
							"Insert into EXPORT_TABLE (USERID,TIMESTAMP,RADIOSIGNALSTRENGTH,ZONE) values (",
							"");
			currentRow.replace("to_date(", "");
			currentRow.replace(",'YYYY-MM-DD HH24:MI:SS')", "");
			currentRow.replace(")", "");
			currentRow.replaceAll("'", "");
		} else if (this.format.equals(format2)) {
			// FROM FORMAT:
			// 13307089817,18-MAR-13,-82,"peek.E4.N.64_970_BN3242_2"
			// "E4S 64.970"
			currentRow.replace("\"", "");
		}
		currentRow.replace("peek.E4.", "E4 ");
		currentRow.replaceAll(".", " ");
		currentRow = currentRow.substring(0, currentRow.length() - 1 - 9);
		// Current format: USERID,TIMESTAMP,RADIOSIGNALSTRENGTH,ZONE

		String[] currentRowArray = currentRow.split(this.commaSeparator);

		return toBlipTrackObservationDatum(currentRowArray,
				this.blueToothDeploymentId);
	}

	private BlipTrackObservationDatum toBlipTrackObservationDatum(
			String[] separatedRow, int id) {
		/**
		 * // Try to get the sensor names... int startPointId =
		 * this.dbAccessor.getSensorIdBySensorNameAndExpId(
		 * separatedRow[this.startPointNameIdx], id);
		 * 
		 * if (startPointId == -1) { throw new
		 * NullPointerException("No such combination of" +
		 * " experiment and sensor name in the prop table."); }
		 * 
		 * return null; }
		 */
		return null;
	}
}
