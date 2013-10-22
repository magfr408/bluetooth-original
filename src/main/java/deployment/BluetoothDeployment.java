package deployment;

import core.Time;

/**
 * This function represents a certain deployment of the Bliptrack sensors, as it is
 * explained in the documentation of the Bluetooth Validation Project. Pretty
 * much a copy of Boris work but for Swedish circumstances (a Bluetooth sensor
 * does not have to cover both directions of the network).
 * 
 * @author Boris Prodhomme
 * @author github.com/magfr408
 */
public class BluetoothDeployment {
	// id is set to 0 if we don't consider the id
	private int id;
	private String name;
	private Time validFrom;
	private Time validTo;
	private int nid;

	public BluetoothDeployment(String name, Time validFrom, Time validTo,
			int nid) {
		this.name = name;
		this.validFrom = validFrom;
		this.validTo = validTo;
		this.nid = nid;
	}

	/*
	 * Getters and setters
	 */
	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public Time getStartValidity() {
		return this.validFrom;
	}

	public Time getEndValidity() {
		return this.validTo;
	}

	public int getNid() {
		return this.nid;
	}
}
