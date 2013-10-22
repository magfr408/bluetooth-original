package datahandling;

import java.util.ArrayList;
import java.util.HashMap;
import datum.BliptrackTripDatum;
import datum.TripDatum;
import deployment.BluetoothSensor;
import deployment.BluetoothDeployment;
import core.Coordinate;
import core.Database;
import core.DatabaseException;
import core.DatabaseReader;
import core.DatabaseWriter;
import java.util.LinkedList;

import netconfig.ModelGraphLink;
import netconfig.Spot;
import core.TimeInterval;
import core.Monitor;
import core.Time;
import deployment.BluetoothRoute;

/**
 * This class employs CORE's database classes to read and write to the database.
 * Pretty much a copy of bobo12 blufaxFeed / src / main / java /
 * run_configurations / DatabaseAccessor.java, but rewritten for Swedish
 * circumstances.
 * 
 * @author bobo12
 * @author magfr408
 */
public class DatabaseAccessor {
	public DatabaseReader dbr;
	private DatabaseWriter dbw;

	/**
	 * Default constructor
	 * 
	 * @param onLocalMachine
	 *            boolean, set to true if you're on your own workstation.
	 */
	public DatabaseAccessor(boolean onLocalMachine) {
		try {
			// TODO: Are these connection parameters correct?
			if (onLocalMachine) {
				this.dbr = new DatabaseReader(Monitor.get_db_env(),
						"bluetoothStockholm");
				this.dbw = new DatabaseWriter(Monitor.get_db_env(),
						"bluetoothStockholm");
			} else {
				this.dbr = new DatabaseReader();
				this.dbw = new DatabaseWriter();
			}

			/*
			 * A long list of prepared reading statements.
			 */
			String schema = "bluetooth";
			String ps, query;

			ps = "Select all start readers corresponding to the experiment";
			query = "SELECT id FROM " + schema + ".prop "
					+ " WHERE (prop.fk_exp_id = ?) ORDER BY prop.id";
			this.dbr.psCreate(ps, query);

			ps = "select routes for a list of start readers";
			query = "SELECT route.id FROM " + schema + ".prop"
					+ " INNER JOIN route"
					+ " ON (prop.id = route.fk_start_id AND prop.id = ANY(?))"
					+ " ORDER BY route.id";
			this.dbr.psCreate(ps, query);

			ps = "select start and end readers for a route";
			query = "SELECT fk_start_id, fk_end_id FROM " + schema + ".route"
					+ " WHERE route.id = ?";
			this.dbr.psCreate(ps, query);

			ps = "select the start and end times corresponding to"
					+ " the experiment";
			query = "SELECT valid_from, valid_to FROM " + schema
					+ ".experiments WHERE id = ?";
			this.dbr.psCreate(ps, query);

			// FIXME: Update to correspond to Bliptrack deployment.
			ps = "select all mac addresses in a time interval";
			query = "SELECT DISTINCT hash_mac" + " FROM " + schema + ".raw"
					+ " WHERE date >= ?" + " AND date < ?";
			this.dbr.psCreate(ps, query);

			// FIXME: Update to correspond to Bliptrack deployment.
			ps = "select raw data corresponding to a MAC address in a time interval"
					+ " by order descending";
			query = " SELECT fk_id, date, scan_duration FROM " + schema
					+ ".raw" + " WHERE hash_mac = ? AND date >= ?"
					+ " AND date < ?" + " ORDER BY date DESC";
			this.dbr.psCreate(ps, query);

			ps = "get the route id corresponding to two prop id's";
			query = "SELECT id FROM " + schema + ".route"
					+ " WHERE fk_start_id = ? AND fk_end_id = ?";
			this.dbr.psCreate(ps, query);

			ps = "select all bliptrack readers in a time interval";
			query = "SELECT DISTINCT fk_id FROM " + schema + ".raw"
					+ " WHERE date >= ? AND date < ?";
			this.dbr.psCreate(ps, query);

			/*
			 * A long list of prepared writing statements
			 */

			ps = "clear filtered table for a certain start id";
			query = "DELETE FROM filtered WHERE fk_start_id = ?";
			this.dbw.psCreate(ps, query);

			// TODO: COULD NOT OUTLIER BE A BOOLEAN IN filtered?
			ps = "clear outliers table for a certain start id";
			query = "DELETE FROM outliers WHERE fk_start_id = ?";
			this.dbw.psCreate(ps, query);

			//TODO: COLUMN WITH OUTLIER BOOLEAN
			ps = "insert a travel time datum in the filtered table";
			query = "INSERT INTO " + schema + ".filtered ("
					+ " fk_hash_mac, fk_start_id, fk_end_id, date, traveltime)"
					+ " VALUES ( ?, ?, ?, ?, ?)";
			this.dbw.psCreate(ps, query);

			// TODO: COULD NOT OUTLIER BE A BOOLEAN IN filtered? We have that
			// information in the trip datum
			ps = "insert a travel time datum in the outliers table";
			query = "INSERT INTO " + schema + ".outliers ("
					+ " fk_hash_mac, fk_start_id, fk_end_id, date, traveltime)"
					+ " VALUES ( ?, ?, ?, ?, ?)";
			this.dbw.psCreate(ps, query);

			// FIXME
			ps = "update a travel time datum in the filtered table";
			query = "UPDATE filtered SET traveltime = ? WHERE"
					+ " fk_hash_mac = ? AND fk_start_id = ? AND"
					+ " fk_end_id = ? AND date = ?";
			this.dbw.psCreate(ps, query);

			// FIXME: UPDATE FOR BLIPTRACK
			ps = "update a travel time datum in the outliers table";
			query = "UPDATE outliers SET traveltime = ? WHERE"
					+ " fk_hash_mac = ? AND fk_start_id = ? AND"
					+ " fk_end_id = ? AND date = ?";
			this.dbw.psCreate(ps, query);

			// FIXME: UPDATE FOR BLIPTRACK
			ps = "insert a raw datum in the raw table";
			query = "INSERT INTO "
					+ schema
					+ ".raw ("
					+ " fk_id, detection_number, hash_mac, scan_duration, date)"
					+ " VALUES ( ?, ?, ?, ?, ?)";
			this.dbw.psCreate(ps, query);
			
			ps = "insert an experiment in the experiments table";
			query = "INSERT INTO " + schema + ".experiments("
					+ "name, valid_from, valid_to, fk_nid)"
					+ " VALUES ( ?, ?, ?, ?)" + " RETURNING id";
			this.dbw.psCreate(ps, query);

			ps = "insert a bliptrack sensor in the prop table";
			// TODO: Do we have mac_adress and time_tag? No :-)
			query = "INSERT INTO " + schema + ".prop("
					+ " name, geom, fk_lid, off_set, fk_exp_id)"
					+ " VALUES (?, ?, ?, ?, ?)" + " RETURNING id";
			this.dbw.psCreate(ps, query);

			ps = "insert a route in the route table";
			query = "INSERT INTO "
					+ schema
					+ ".route("
					+ "fk_start_id, fk_end_id, name, fk_validation_route_id, route_length, description)"
					+ " VALUES ( ?, ?, ?, ?, ?, ?)" + " RETURNING id";
			this.dbw.psCreate(ps, query);

			ps = "clear filtered table for a certain exp id";
			query = " DELETE FROM " + schema
					+ ".filtered USING experiments, prop"
					+ " WHERE fk_start_id = prop.id AND prop.fk_exp_id = ?";
			this.dbw.psCreate(ps, query);

		} catch (DatabaseException dbEx) {
			Monitor.err(dbEx, "There's someting wrong with the "
					+ "DB-connection, don't know what to do.");
		}
	}

	public void closeDBConnections() {
		if (this.dbr != null) {
			this.dbr.close();
		}
		if (this.dbw != null) {
			this.dbw.close();
		}
	}

	/**
	 * @param routeIds
	 * @return A Hashmap<Integer, Integer> with the mapping between x and the
	 *         route
	 * @throws DatabaseException
	 * @Deprecated Is this supposed to return the start or end sensor?
	 */
	public HashMap<Integer, Integer> getMappingEndToRoute(
			ArrayList<Integer> routeIds) throws DatabaseException {

		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();

		String ps = "select start and end readers for a route";

		for (Integer routeId : routeIds) {
			this.dbr.psSetInteger(ps, 1, routeId);
			this.dbr.psQuery(ps);

			while (this.dbr.psRSNext(ps)) {
				// TODO: Confusing variable name..
				int startReader = this.dbr.psRSGetInteger(ps, "fk_end_id");

				if (map.containsKey(startReader)) {
					Monitor.err("there should be a unique route for"
							+ " one start reader id");
					return null;
				} else {
					map.put(startReader, routeId);
				}
			}
		}
		return map;
	}

	/**
	 * @param routeIds
	 * @return
	 * @throws DatabaseException
	 */
	public HashMap<Integer, HashMap<Integer, Integer>> getMappingReaderToRoute(
			ArrayList<Integer> routeIds) throws DatabaseException {

		HashMap<Integer, HashMap<Integer, Integer>> map = new HashMap<Integer, HashMap<Integer, Integer>>();

		String ps = "select start and end readers for a route";

		for (Integer routeId : routeIds) {
			this.dbr.psSetInteger(ps, 1, routeId);
			this.dbr.psQuery(ps);

			while (this.dbr.psRSNext(ps)) {
				int startReader = this.dbr.psRSGetInteger(ps, "fk_start_id");
				int endReader = this.dbr.psRSGetInteger(ps, "fk_end_id");

				if (map.containsKey(startReader)) {
					map.get(startReader).put(endReader, routeId);
					Monitor.out("Msg from Bluetooth.datahandling."
							+ "DatabaseAccesor: Already had the current"
							+ " startReader");
				} else {
					map.put(startReader, new HashMap<Integer, Integer>());
					map.get(startReader).put(endReader, routeId);
				}
			}
		}
		return map;
	}

	/**
	 * @param route
	 * @return
	 * @throws DatabaseException
	 */
	public int getRouteId(BluetoothRoute route) throws DatabaseException {

		String ps = "get the route id corresponding to two prop id's";

		this.dbr.psSetInteger(ps, 1, route.getStartReader());
		this.dbr.psSetInteger(ps, 2, route.getEndReader());
		this.dbr.psQuery(ps);

		if (this.dbr.psRSNext(ps)) {
			int id = this.dbr.psRSGetSmallInt(ps, "id");
			this.dbr.psRSDestroy(ps);
			return id;
		}
		return -1;
	}

	/**
	 * @param timeInterval
	 * @return
	 * @throws DatabaseException
	 */
	public ArrayList<Integer> getAllBliptrackReaders(TimeInterval timeInterval)
			throws DatabaseException {

		Monitor.mon("Getting all active Bliptrack readers in "
				+ timeInterval.toString());

		ArrayList<Integer> listReaders = new ArrayList<Integer>();

		String psReaders = "select all bliptrack readers in a time interval";

		this.dbr.psSetTimestamp(psReaders, 1, timeInterval.get_start_time());
		this.dbr.psSetTimestamp(psReaders, 2, timeInterval.get_end_time());
		this.dbr.psQuery(psReaders);

		while (this.dbr.psRSNext(psReaders)) {
			int readerId = this.dbr.psRSGetInteger(psReaders, "fk_id");
			listReaders.add(readerId);
		}
		Monitor.mon("Bliptrack reader ids cached in memory.");
		return listReaders;
	}

	/**
	 * @param listReaders
	 * @return
	 * @throws DatabaseException
	 */
	public ArrayList<Integer> getAllRoutes(ArrayList<Integer> listReaders)
			throws DatabaseException {

		ArrayList<Integer> listRoutes = new ArrayList<Integer>();

		String psRoutes = "select routes for a list of start readers";

		this.dbr.psSetArrayInteger(psRoutes, 1,
				listReaders.toArray(new Integer[0]));
		this.dbr.psQuery(psRoutes);

		while (this.dbr.psRSNext(psRoutes)) {
			int routeId = this.dbr.psRSGetInteger(psRoutes, "id");
			listRoutes.add(routeId);
		}
		return listRoutes;
	}


	/**
	 * @param sensor
	 * @return the auto incremented identifier from the DB on insert.
	 * @throws DatabaseException
	 */
	public int insertProp(BluetoothSensor sensor) throws DatabaseException {

		//String ps = "insert a bliptrack sensor in the prop table";
		//this.dbw.psSetVarChar(ps, 1, sensor.getName());

		//this.dbw.psSetPostGISPoint(ps, 2, sensor.getCoordinate());
		//this.dbw.psSetBigInt(ps, 3, sensor.getLinkId());
		//this.dbw.psSetReal(ps, 4, sensor.getOffset());
		//this.dbw.psSetSmallInt(ps, 5, sensor.getExpId());
		
		/**FIXME: The commented code above should be the way to do this. 
		 * However, Database.psSetPostGISPoint(
		 *           String name, int index, Coordinate v) says 
		 *   org.postgis.Point p = new org.postgis.Point(v.lon, v.lat);
		 * when it should be 
		 *   org.postgis.Point p = new org.postgis.Point(v.lat, v.lon);
		 * Don't know if any other classes uses this method. 
		 * But should be easy fix.
		 *  
		 */
		String ps = "insert a bliptrack sensor in the prop table bad-fix";
		if (!this.dbw.psHasPS(ps)) {
			String query = "INSERT INTO bluetooth.prop("
				+ "name, geom, fk_lid, off_set, fk_exp_id)"
				+ " VALUES ( ? , ST_SetSRID(ST_Point(?, ?), 4326), ? , ? , ?)"
				+ " RETURNING id";
			this.dbw.psCreate(ps, query);
		}
		
		this.dbw.psSetVarChar(ps, 1, sensor.getName());
		this.dbw.psSetDouble(ps, 2, sensor.getCoordinate().lat);
		this.dbw.psSetDouble(ps, 3, sensor.getCoordinate().lon);
		this.dbw.psSetBigInt(ps, 4, sensor.getLinkId());
		this.dbw.psSetReal(ps, 5, sensor.getOffset());
		this.dbw.psSetSmallInt(ps, 6, sensor.getExpId());
		
		return this.dbw.psUpdateReturningInteger(ps);
	}

	/**
	 * @param route
	 * @param fkValidationRouteId
	 *            the unique identifier that preferably was returned by
	 *            BLUETOOTH
	 *            /deployment/CreateBluetoothDeployment/populateRouteTables()
	 *            This identifier was returned from the DB upon creation of a
	 *            new entry in validation.route
	 * @return primary key of the new bluetooth.route entry, is auto increment.
	 * @throws DatabaseException
	 */
	public int insertBTRoute(BluetoothRoute route, int fkValidationRouteId)
			throws DatabaseException {

		String ps = "insert a route in the route table";

		this.dbw.psSetSmallInt(ps, 1, (short) route.getStartReader());
		this.dbw.psSetSmallInt(ps, 2, (short) route.getEndReader());
		this.dbw.psSetVarChar(ps, 3, route.getName());
		this.dbw.psSetInteger(ps, 4, fkValidationRouteId);
		this.dbw.psSetReal(ps, 5, route.getLength());
		this.dbw.psSetVarChar(ps, 6, route.getDescription());

		return this.dbw.psUpdateReturningInteger(ps);
	}

	/**
	 * Insert a new Bluetooth deployment/experiment into 
	 * the database. The returned value is the auto incremented 
	 * experiment id.
	 * @param exp, a deployment/experiment with a name, 
	 * start - and end times a corresponding network id.
	 * @return Experiment identifier.
	 * @throws DatabaseException
	 */
	public int insertExperiment(BluetoothDeployment exp)
			throws DatabaseException {

		String ps = "insert an experiment in the experiments table";
		this.dbw.psSetVarChar(ps, 1, exp.getName());
		this.dbw.psSetTimestamp(ps, 2, exp.getStartValidity());
		this.dbw.psSetTimestamp(ps, 3, exp.getEndValidity());
		this.dbw.psSetInteger(ps, 4, exp.getNid());

		return this.dbw.psUpdateReturningInteger(ps);
	}
	
	
	/**
	 * TODO: Methods from Blufax_feed that are not implemented.
	 * MAY NOT USE ALL OF THEM.
	 * 
	 * public LinkedList<String> getAllMacAddressesInInterval(
	 *		TimeInterval timeInterval) throws DatabaseException {}
	 * 
	 * public boolean insertTrip(BliptrackTripDatum trip) 
	 * 		throws DatabaseException {}
	 * 
	 * public void updateTrip(BliptrackTripDatum trip) 
	 * 		throws DatabaseException {}
	 * 
	 * public void updateOutlier(TripDatum trip)
	 * 		throws DatabaseException {}
	 * 
	 * public boolean insertOutlier(TripDatum trip) 
	 * 		throws DatabaseException {}
	 * 
	 * public Time[] getStartEndTime(int expId) 
	 * 		throws DatabaseException {}
	 * 
	 * public void ClearFilteredWithExpId(int expId) 
	 * 		throws DatabaseException {}
	 */
}
