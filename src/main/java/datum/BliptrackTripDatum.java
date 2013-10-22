package datum;

import core.Time;

/**
 * This class is an extension of a TripDatum with additional info for the data
 * produced by Bliptrack.
 * 
 * @author magfr408
 */
public class BliptrackTripDatum extends TripDatum {

	private short analysisId;
	private String cod;
	private String outlierLevel;

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
			String classOfDevice, String outLierLevel) {

		super(macAddress, startReader, endReader, endTime.plus(-1.0f
				* travelTime), endTime);

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

	public String getOutlierLevel() {
		return this.outlierLevel;
	}
}
