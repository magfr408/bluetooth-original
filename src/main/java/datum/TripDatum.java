package datum;

import core.Time;

/**
 * This class represents what we call a trip (between two Bluetooth readers).
 * It is a copy of bobo12/Boris blufaxFeed / src / main / java / datum /
 * 
 * @author Boris
 * @author magfr408
 */
public class TripDatum {

	private String macAddress;
	private int startReader;
	private int endReader;
	private Time startTime;
	private Time endTime;
	private float travelTime;
	private boolean isOutlier;

	/**
	 * @param macAddress
	 * @param startReader
	 * @param endReader
	 * @param startTime
	 * @param endTime
	 */
	public TripDatum(String macAddress, int startReader, int endReader,
			Time startTime, Time endTime) {

		this(macAddress, startReader, endReader, startTime, endTime, endTime
				.secondsSince(startTime));
	}

	public TripDatum(String macAddress, int startReader, int endReader,
			Time startTime, Time endTime, float travelTime) {

		this.macAddress = macAddress;
		this.startReader = startReader;
		this.endReader = endReader;
		this.startTime = startTime;
		this.endTime = endTime;
		this.travelTime = travelTime;

		this.isOutlier = false;
	}

	public int getStartReader() {
		return startReader;
	}

	public int getEndReader() {
		return endReader;
	}

	public String getMacAddress() {
		return macAddress;
	}

	public Time getStartTime() {
		return startTime;
	}

	public boolean isOutlier() {
		return isOutlier;
	}

	public Time getEndTime() {
		return endTime;
	}

	public float getTravelTime() {
		return travelTime;
	}

	public void setIsOutlier(boolean isOutlier) {
		this.isOutlier = isOutlier;
	}
}
