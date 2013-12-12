package filters;

import core.Time;

public class FilterSettings {

	public enum FilterMethod {
		MAD, IQR;
	}

	public enum DataCase {
		MTF1, RAW;

		/**
		 * MTF1, "Matched Trips on File case 1". Use this if you have matched
		 * trips with each route in a separate csv-file with the format
		 * RouteName;RouteID;MeasuredTimeTimestamp;UserId;StartPointNumber;
		 * EndPointNumber;StartPointName;EndPointName;MeasuredTime;
		 * ClassOfDevice;outlierLevel WITH THIS HEADING IN THE FIRST ROW.
		 * 
		 * RAW stands for just that, it filters the individual measurements in
		 * the bluetooth.raw table into bluetooth.filtered (or to system out).
		 */
	}

	private String dataFile;
	private String commaSeparator;
	private String dateFormat;
	private int userIdIdx;
	private int startPointNameIdx;
	private int endPointNameIdx;
	private int measurementStartTimeIdx;
	private int travelTimeIdx;
	private boolean onLocalMachine;
	private boolean writeInDB;
	private boolean prefilter;
	private int bluetoothDeploymentId;
	private float timeWindow;
	private FilterMethod filterMethod;
	private DataCase datacase;
	private Time startTime;
	private Time endTime;

	public FilterSettings(String dataFile, String commaSeparator,
			String dateFormat, int userIdIdx, int startPointNameIdx,
			int endPointNameIdx, int measurementStartTimeIdx,
			int travelTimeIdx, boolean onLocalMachine, boolean writeInDB,
			int bluetoothDeploymentId, float timeWindow,
			FilterMethod filterMethod, DataCase datacase) {

		this(dataFile, commaSeparator, dateFormat, userIdIdx,
				startPointNameIdx, endPointNameIdx, measurementStartTimeIdx,
				travelTimeIdx, onLocalMachine, writeInDB,
				bluetoothDeploymentId, timeWindow, filterMethod, datacase,
				null, null, false);
	}
	
	public FilterSettings(String dataFile, String commaSeparator,
			String dateFormat, int userIdIdx, int startPointNameIdx,
			int endPointNameIdx, int measurementStartTimeIdx,
			int travelTimeIdx, boolean onLocalMachine, boolean writeInDB,
			int bluetoothDeploymentId, float timeWindow,
			FilterMethod filterMethod, DataCase datacase, boolean prefilter) {

		this(dataFile, commaSeparator, dateFormat, userIdIdx,
				startPointNameIdx, endPointNameIdx, measurementStartTimeIdx,
				travelTimeIdx, onLocalMachine, writeInDB,
				bluetoothDeploymentId, timeWindow, filterMethod, datacase,
				null, null, prefilter);
	}

	public FilterSettings(String dataFile, String commaSeparator,
			String dateFormat, int userIdIdx, int startPointNameIdx,
			int endPointNameIdx, int measurementStartTimeIdx,
			int travelTimeIdx, boolean onLocalMachine, boolean writeInDB,
			int bluetoothDeploymentId, float timeWindow,
			FilterMethod filterMethod, DataCase datacase, Time startTime,
			Time endTime, boolean prefilter) {

		this.dataFile = dataFile;
		this.commaSeparator = commaSeparator;
		this.dateFormat = dateFormat;
		this.userIdIdx = userIdIdx;
		this.startPointNameIdx = startPointNameIdx;
		this.endPointNameIdx = endPointNameIdx;
		this.measurementStartTimeIdx = measurementStartTimeIdx;
		this.travelTimeIdx = travelTimeIdx;
		this.onLocalMachine = onLocalMachine;
		this.writeInDB = writeInDB;
		this.bluetoothDeploymentId = bluetoothDeploymentId;
		this.timeWindow = timeWindow;
		this.filterMethod = filterMethod;
		this.datacase = datacase;
		this.startTime = startTime;
		this.endTime = endTime;
		this.prefilter = prefilter;
	}

	public float getTimeWindow() {
		return timeWindow;
	}

	public String getDataFileName() {
		return dataFile;
	}

	public String getCommaSeparator() {
		return commaSeparator;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public int getUserIdIdx() {
		return userIdIdx;
	}

	public int getStartPointNameIdx() {
		return startPointNameIdx;
	}

	public int getEndPointNameIdx() {
		return endPointNameIdx;
	}

	public int getMeasurementStartTimeIdx() {
		return measurementStartTimeIdx;
	}

	public int getTravelTimeIdx() {
		return travelTimeIdx;
	}

	public boolean isOnLocalMachine() {
		return onLocalMachine;
	}

	public boolean getWriteInDB() {
		return writeInDB;
	}

	public int getBluetoothDeploymentId() {
		return bluetoothDeploymentId;
	}

	public FilterMethod getFilterMethod() {
		return filterMethod;
	}

	public DataCase getDataCase() {
		return datacase;
	}
	
	public Time getStartTime() {
		return startTime;
	}
	
	public Time getEndTime() {
		return endTime;
	}
	
	public void setStartTime(Time t) {
		this.startTime = Time.newTimeFromTime(t);
	}
	
	public void setEndTime(Time t) {
		this.endTime = Time.newTimeFromTime(t);
	}
	
	public void setPrefilter(boolean p) {
		this.prefilter = true;
	}
	
	public boolean prefilter() {
		return prefilter;
	}
}