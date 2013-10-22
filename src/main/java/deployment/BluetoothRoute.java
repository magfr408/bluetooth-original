/**
 * 
 */
package deployment;

/**
 * Represents a route as we refer to in the Bluetooth Validation project, that
 * is to say the given start and end prop id's (sensor reader+location). Pretty
 * much a copy of Boris' work but for Swedish circumstances.
 * 
 * @author Boris Prodhomme
 * @author github.com/magfr408
 */
public class BluetoothRoute {
	int startReader;
	int endReader;
	private String name;
	private String description;
	private float length;

	public BluetoothRoute(int startReader, int endReader) {
		this.startReader = startReader;
		this.endReader = endReader;
		this.name = "no name";
		description = "no description";
	}

	public BluetoothRoute(int startReader, int endReader, String name) {
		this.startReader = startReader;
		this.endReader = endReader;
		this.name = name;
		this.description = "no description";
	}

	public BluetoothRoute(int startReader, int endReader, String name,
			String description) {
		this.startReader = startReader;
		this.endReader = endReader;
		this.name = name;
		this.description = description;
	}

	public int getStartReader() {
		return startReader;
	}

	public int getEndReader() {
		return endReader;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public float getLength() {
		return length;
	}

	public void setLength(float length) {
		this.length = length;
	}
}
