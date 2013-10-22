/**
 * 
 */
package deployment;

import core.Coordinate;

/**
 * 
 * Pretty much a copy of Boris' work but for Swedish circumstances. This could
 * hopefully be used as a superclass for all BT-sensors.
 * 
 * @author Boris Prodhomme
 * @author github.com/magfr408
 */
public class BluetoothSensor {
	// Name of the sensor, goes in db.
	private String name;
	// Sensor location

	// identifier should be automatically incremented by the DB upon creation.

	private Coordinate coordinate;
	// Corresponding navteq(?) link
	private long linkId;
	// Offset on that link
	private float offset;
	// Experiment identifier, db entry, should have the time period.
	private short expId;

	/**
	 * Constructor
	 * 
	 * @param name
	 *            Some sensible name for this sensor.
	 * @param coordinate
	 *            Latitude and Longitude.
	 * @param linkId
	 *            The X link identifier.
	 * @param offset
	 *            How far down the link lies the sensor.
	 * @param expId
	 *            The deployment (experiment) number.
	 */
	public BluetoothSensor(String name, Coordinate coordinate, long linkId,
			float offset, short expId) {
		this.name = name;
		this.coordinate = coordinate;
		this.linkId = linkId;
		this.offset = offset;
		this.expId = expId;
	}

	/*
	 * Getters
	 */

	public String getName() {
		return name;
	}

	public Coordinate getCoordinate() {
		return coordinate;
	}

	public long getLinkId() {
		return linkId;
	}

	public float getOffset() {
		return offset;
	}

	public short getExpId() {
		return expId;
	}

}
