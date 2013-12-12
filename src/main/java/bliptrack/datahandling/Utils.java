package bliptrack.datahandling;

import core.Time;

public class Utils {

	public static Time timestringToTime(String timeString, String dateFormat) {
		int year, month, day, hour, minute, second;

		if (dateFormat.equals("YYYY-MMM-DD HH:MM:SS")) {

			year = Integer.parseInt(timeString.substring(0, 4));
			String monthStr = timeString.substring(5, 8);

			if (monthStr.equals("jan") || monthStr.equals("JAN")
					|| monthStr.equals("Jan")) {
				month = 1;
			} else if (monthStr.equals("feb") || monthStr.equals("FEB")
					|| monthStr.equals("Feb")) {
				month = 2;
			} else if (monthStr.equals("mar") || monthStr.equals("MAR")
					|| monthStr.equals("Mar")) {
				month = 3;
			} else if (monthStr.equals("apr") || monthStr.equals("APR")
					|| monthStr.equals("Apr")) {
				month = 4;
			} else if (monthStr.equals("may") || monthStr.equals("MAY")
					|| monthStr.equals("May")) {
				month = 5;
			} else if (monthStr.equals("jun") || monthStr.equals("JUN")
					|| monthStr.equals("Jun")) {
				month = 6;
			} else if (monthStr.equals("jul") || monthStr.equals("JUL")
					|| monthStr.equals("Jul")) {
				month = 7;
			} else if (monthStr.equals("aug") || monthStr.equals("AUG")
					|| monthStr.equals("Aug")) {
				month = 8;
			} else if (monthStr.equals("sep") || monthStr.equals("SEP")
					|| monthStr.equals("Sep")) {
				month = 9;
			} else if (monthStr.equals("oct") || monthStr.equals("OCT")
					|| monthStr.equals("Oct")) {
				month = 10;
			} else if (monthStr.equals("nov") || monthStr.equals("NOV")
					|| monthStr.equals("Nov")) {
				month = 11;
			} else if (monthStr.equals("dec") || monthStr.equals("DEC")
					|| monthStr.equals("Dec")) {
				month = 12;
			} else {
				// Don't know what to do.
				throw new IllegalArgumentException();
			}
			
			day = Integer.parseInt(timeString.substring(9, 11));
			hour = Integer.parseInt(timeString.substring(12, 14));
			minute = Integer.parseInt(timeString.substring(15, 17));
			second = Integer.parseInt(timeString.substring(18, 20));
		} else if (dateFormat.equals("YYYY-MM-DD HH:MM:SS")) {

			year = Integer.parseInt(timeString.substring(0, 4));
			month = Integer.parseInt(timeString.substring(5, 7));
			day = Integer.parseInt(timeString.substring(8, 10));
			hour = Integer.parseInt(timeString.substring(11, 13));
			minute = Integer.parseInt(timeString.substring(14, 16));
			second = Integer.parseInt(timeString.substring(17, 19));

		} else {
			// No other cases implemented.
			return null;
		}

		return Time.newTimeFromBerkeleyDateTime(year, month, day, hour, minute,
				second, 0);
	}

}
