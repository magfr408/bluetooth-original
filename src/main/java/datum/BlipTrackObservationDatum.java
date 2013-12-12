package datum;

import core.Time;

public class BlipTrackObservationDatum extends ObservationDatum {
	
	private float signalStrength;
	
	public BlipTrackObservationDatum(int readerId, String userId, Time time, float signalStrength) {
		super(readerId, userId, time);
		
		this.signalStrength = signalStrength;
	}

	public float getSignalStrength() {
		return this.signalStrength;
	}
}
