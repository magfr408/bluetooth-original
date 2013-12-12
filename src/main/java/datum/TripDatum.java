package datum;

import core.Time;
import filters.FilterManager;
import filters.FilterSettings;

/**
 * This class represents what we call a trip (between two Bluetooth readers). It
 * is a copy of bobo12/Boris blufaxFeed / src / main / java / datum /
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
	private FilterSettings.FilterMethod filterType;
	private int nbObservations;

	/**
	 * @param macAddress
	 * @param startReader
	 * @param endReader
	 * @param startTime
	 * @param endTime
	 */
	public TripDatum(String macAddress, int startReader, int endReader,
			Time startTime, Time endTime, FilterSettings.FilterMethod filterType) {

		this(macAddress, startReader, endReader, startTime, endTime, endTime
				.secondsSince(startTime), filterType, false, 1);
	}

	public TripDatum(String macAddress, int startReader, int endReader,
			Time startTime, Time endTime, float travelTime,
			FilterSettings.FilterMethod filterType) {

		this(macAddress, startReader, endReader, startTime, endTime,
				travelTime, filterType, false, 1);
	}

	public TripDatum(String macAddress, int startReader, int endReader,
			Time startTime, Time endTime, float travelTime,
			FilterSettings.FilterMethod filterType, boolean isOutlier,
			int nbObservations) {

		this.macAddress = macAddress;
		this.startReader = startReader;
		this.endReader = endReader;
		this.startTime = startTime;
		this.endTime = endTime;
		this.travelTime = travelTime;
		this.filterType = filterType;

		this.isOutlier = isOutlier;

		this.nbObservations = nbObservations;
	}

	@Override
	public TripDatum clone() {
		return new TripDatum(new String(this.getMacAddress()),
				(int) this.startReader, (int) this.endReader,
				Time.newTimeFromTime(this.getStartTime()),
				Time.newTimeFromTime(this.getEndTime()), 
				this.getTravelTime(), this.getFilterType(),
				this.isOutlier, this.getNbObservations());
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

	public FilterSettings.FilterMethod getFilterType() {
		return filterType;
	}

	public void setIsOutlier(boolean isOutlier) {
		this.isOutlier = isOutlier;
	}

	public void setFilterMethod(FilterSettings.FilterMethod fm) {
		this.filterType = fm;
	}

	public void setNbObservations(int n) {
		this.nbObservations = n;
	}

	public int getNbObservations() {
		return this.nbObservations;
	}

	public void setTravelTime(float t) {
		this.travelTime = t;
	}
}
