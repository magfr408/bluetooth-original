package datum;

import core.Time;
import filters.FilterSettings;

/**
 * This class is an extension of a TripDatum with additional info for the data
 * produced by Bliptrack in some cases.
 * 
 * @author magfr408
 */
public class BliptrackTripDatum extends TripDatum {

	private short analysisId;
	private String cod;
	private int outlierLevel;

	/**
	 * Constructor for a BlipTrackDatum
	 * 
	 * @param macAddress
	 * @param startReader
	 * @param endReader
	 * @param endTime
	 */
	public BliptrackTripDatum(String macAddress, int startReader,
			int endReader, Time endTime, short analysisId, float travelTime,
			String classOfDevice, int outLierLevel, 
			FilterSettings.FilterMethod filterMethod) {

		super(macAddress, startReader, endReader, endTime.plus(-1.0f
				* travelTime), endTime, filterMethod);

		this.analysisId = analysisId;
		this.cod = classOfDevice;
		this.outlierLevel = outLierLevel;
	}

	public short getAnalysisId() {
		return this.analysisId;
	}

	public String getClassOfDevice() {
		return this.cod;
	}

	public int getOutlierLevel() {
		return this.outlierLevel;
	}
}
