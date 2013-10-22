package deployment;

import java.util.ArrayList;

import netconfig.ModelGraphLink;
import netconfig.NavteqLink;
import netconfig.NetconfigException;
import netconfig.Network;
import netconfig.Route;
import netconfig.Spot;
import util.NetworkAnalysis;
import core.Coordinate;
import core.Database;
import core.DatabaseException;
import core.DatabaseReader;
import core.DatabaseWriter;
import core.Monitor;
import core.Time;
import datahandling.DatabaseAccessor;

/**
 * Pretty much a copy of Boris' work but for Swedish circumstances. I.e. we do
 * not necessarily have the same BT-sensors in both directions.
 * 
 * @author Boris Prodhomme
 * @author github.com/magfr408
 */

public class CreateBluetoothDeployment {
	/**
	 * Implemented SRIDs:
	 * 
	 * - 4326 (WGS84 EPSG) - 8307 (WGS84 OGC) - 0 (Cartesian)
	 */
	final static int srid = 4326;

	public static void main(String[] args) throws NetconfigException,
			java.lang.Exception {

		// To set host, port and name in CORE
		Monitor.set_db_env("mms");
		Monitor.get_db_env();
		/*
		 * This boolean is true if we actually want to write in the database
		 */

		boolean writeInDb = true;

		/*
		 * State if running from your own workstation
		 */

		boolean onLocalMachine = true;

		/*
		 * SET the nid to use (the bounding polygon must contain the GPS 
		 * locations of the sensors)
		 */
		int nid = 18;

		Network net = new Network(Network.NetworkType.MODEL_GRAPH, nid);

		/*
		 * CREATE EXPERIMENT (with a suitable name)
		 */

		String expName = "E4 Sorentorp to Haga Sodra - SB Second Try";
		Time validFrom = Time.newTimeFromBerkeleyDateTime(2013, 2, 28, 15, 0,
				0, 0);
		Time validTo = Time.newTimeFromBerkeleyDateTime(2013, 3, 31, 15, 0, 0,
				0);

		/*
		 * DECLARE GPS LOCATIONS in their correct order (first to last)
		 */
		double[] latitudes = { 17.987579, 17.999609, 18.004765, 18.009833,
				18.016819, 18.020321, 18.026327, 18.031689 };

		// The latitudes array must have the same length as the longitudes
		// array, and the same sensor order.

		double[] longitudes = { 59.393546, 59.388747, 59.385046, 59.380887,
				59.374517, 59.367502, 59.361589, 59.357137 };

		/*
		 * OPTIONAL: List of link identifiers if known TODO: Not implemeted yet.
		 */
		long[] linkIdList = null;
		// long[] linkIdList = {};

		/*
		 * OPTIONAL: List of sensor offset in relation to the link TODO: Not
		 * implemeted yet.
		 */
		float[] offsets = null;
		// float[] offsets = {};

		/*
		 * Name of sensors.
		 */
		String[] sensorNames = { "E4S 64.970", "E4S 64.090", "E4S 63.580",
				"E4S 63.040", "E4S 62.220", "E4S 61.395", "E4S 60.645",
				"E4S 60.060" };

		if (longitudes.length != latitudes.length) {
			Monitor.err("Latitudes and longitudes are of different length, "
					+ "don't know what to do.");
		}

		// Step 0. Create the coordinates array, which is ordered.
		Coordinate[] coordinates = new Coordinate[longitudes.length];
		for (int i = 0; i < longitudes.length; i++) {
			coordinates[i] = new Coordinate(srid, latitudes[i], longitudes[i]);
		}

		// Step 1. Get the routes between these coordinates.
		Route[] routes = GetRoutes(net.readDB, coordinates, net);
		if (writeInDb) {
			// write the results in the database (i.e populate the prop and
			// route tables).
			try {
				// Step 2: Create an experiment.
				BluetoothDeployment exp = new BluetoothDeployment(expName,
						validFrom, validTo, nid);

				// Step 3: Enable access to prepared statements.
				DatabaseAccessor dbAccessor = new DatabaseAccessor(
						onLocalMachine);
				// Step 4: Insert the experiment in DB. Get an id in return!
				int expId = dbAccessor.insertExperiment(exp);
				// Step 5: Update the experiment in memory with the id.
				exp.setId(expId);

				// Step 6: Set up additional prepared statements so that we can
				// save the Bluetooth deployment with corresponding data in the
				// validation schema.
				DatabaseWriter dbw = new DatabaseWriter(dbAccessor.dbr.name,
						"validation");
				prepareStatements(dbw);

				// Step 7: Populate the props table with the sensors.
				// TODO: Assumes that there are no earlier sensor records.
				int[] sensorId = populatePropTable(exp, routes, coordinates,
						dbAccessor, sensorNames);

				// Step 8 & 9: Populate the routes table with the routes (which
				// are
				// extrapolated from the sensors.
				populateRouteTables(sensorId, dbw, routes, net, dbAccessor,
						sensorNames);

				// Step 10: Populate the validation schema with this new setup.
				String ps = "update the validation.route_networks table";
				String query = "SELECT validation.populateRouteNetworkTable();";
				dbw.psCreate(ps, query);

				// Step 11: Tell us how many routes that where inserted.
				int nbInsertions = dbw.psUpdateReturningInteger(ps);
				Monitor.out(nbInsertions + " insertions have been made in the "
						+ "validation.route_networks table ");

				// Step 12: Hope that everything went smoothly.
			} catch (DatabaseException dbe) {
				Monitor.err(dbe);
				dbe.printStackTrace();
			} catch (NullPointerException npe) {
				Monitor.err(npe);
				Monitor.err("you probably forgot to give sensorIds");
				npe.printStackTrace();
			} catch (Exception exp) {
				Monitor.err(exp);
				exp.printStackTrace();
			}
		} else {
			System.out.println("ROUTES");
			for (int j = 0; j < routes.length; j++) {
				String tempString = "Route " + j + ", ";

				for (int i = 0; i < routes[j].spots.length; i++) {
					tempString = tempString + "link: "
							+ routes[j].spots[i].link.toString() + " offset: "
							+ routes[j].spots[i].offset + ", ";
				}
				Monitor.out(tempString);
			}
		}
	}

	// Step 1. Get the routes between these coordinates.
	private static Route[] GetRoutes(DatabaseReader dbr,
			Coordinate[] coordinates, Network net) throws java.lang.Exception {

		NetworkAnalysis netA = new NetworkAnalysis(net);
		// Spots have a link, lane and an offset.
		Spot[] spots = new Spot[coordinates.length];

		// We hopefully have one route less than we have sensors...
		Route[] routes = new Route[coordinates.length - 1];

		// Search for the spot on a link in the net which has the shortest
		// euclidean distance to the coordinate and that is no more than 100
		// meters from it.
		for (int i = 0; i < spots.length; i++) {
			spots[i] = projectPointOnNet(dbr, net, coordinates[i].lat,
					coordinates[i].lon, 100);
		}

		// Now get the routes between these ordered spots and return them.
		for (int j = 0; j < routes.length; j++) {
			routes[j] = netA.extractRoute(spots[j], spots[j + 1]);
		}
		return routes;
	}

	private static void prepareStatements(DatabaseWriter dbw)
			throws DatabaseException {

		String ps = "insert a route in the validation.route table";
		String query = " INSERT INTO validation.route(name)" + " VALUES (?)"
				+ " RETURNING id";
		dbw.psCreate(ps, query);

		ps = "insert a route in the route_spots table";
		query = "INSERT INTO validation.route_spots("
				+ "fk_route_id, idx, fk_lid, off_set)" + " VALUES (?,?,?,?)";
		dbw.psCreate(ps, query);
	}

	// Step 7: Populate the props table with the sensors.
	// TODO: Assumes that there are no earlier sensor records.
	// TODO: Should we link the sensors to an experiment?
	private static int[] populatePropTable(BluetoothDeployment exp,
			Route[] routes, Coordinate[] coordinates,
			DatabaseAccessor dbAccessor, String[] sensorNames)
			throws DatabaseException, NetconfigException {

		int[] sensorId = new int[routes.length + 1];

		Spot<NavteqLink> spot = routes[0].getFirstSpot().toNavteqSpot();

		BluetoothSensor sensor = new BluetoothSensor(sensorNames[0],
				coordinates[0], spot.link.id, spot.offset, (short) exp.getId());

		// actually insert the sensor in the prop table
		sensorId[0] = dbAccessor.insertProp(sensor);

		for (int i = 1; i < routes.length + 1; i++) {
			spot = routes[i - 1].getLastSpot().toNavteqSpot();

			sensor = new BluetoothSensor(sensorNames[i], coordinates[i],
					spot.link.id, spot.offset, (short) exp.getId());
			// actually insert the sensor in the prop table
			sensorId[i] = dbAccessor.insertProp(sensor);
		}
		return sensorId;
	}

	// Step 8: Populate the two route tables with the routes (which are
	// extrapolated from the sensors. Schemas: bluetooth, validation
	private static void populateRouteTables(int[] sensorIds,
			DatabaseWriter dbWriter, Route[] routes, Network net,
			DatabaseAccessor dbAccessor, String[] sensorNames)
			throws DatabaseException, NetconfigException {

		// A new array to store the IDs returned by DB
		int[] routeIds = new int[routes.length];

		// FIXME: This should have been checked earlier.
		if (routes.length != sensorIds.length - 1) {
			throw new IllegalArgumentException();
		}

		BluetoothRoute btRoute;

		// For each route; make a new entry in validation.route and get a unique
		// id in return, then insert the route spots (start and end) in
		// validation.route_spots. Finally update the Bluetooth route with the
		// returned information and save as a Bluetooth route in
		// bluetooth.routes (get a unique identifier in return).
		for (int j = 0; j < routes.length; j++) {
			// Create a route from sensor1 to sensor2, with name
			btRoute = new BluetoothRoute(sensorIds[j], sensorIds[j + 1],
					"from " + sensorNames[j] + " to " + sensorNames[j + 1]);

			// Post value to the prepared statement
			String ps = "insert a route in the validation.route table";
			dbWriter.psSetVarChar(ps, 1, btRoute.getName());
			try {
				// Update with new table entry, the column validation.route.id
				// has auto increment
				routeIds[j] = dbWriter.psUpdateReturningInteger(ps);
			} catch (DatabaseException dbe) {
				System.out.println("problem when creation of the route in "
						+ "validation_route table");
				dbe.printStackTrace();
			}

			// add spots to route (FIXME: why?)
			insertRouteSpotsWithoutRepetition(dbWriter, routeIds[j],
					routes[j].toModelGraphRoute(net));
			// set route length
			btRoute.setLength(routes[j].getRouteLength());
			// write to blufax_feed.route table
			dbAccessor.insertBTRoute(btRoute, routeIds[j]);
		}
	}

	/**
	 * Step 9: Insert the route spots in validation.route_spots.
	 * 
	 * @param routeId
	 *            a reference to the primary key of the route entry in
	 *            validation.route. If no such entry exists in that table, the
	 *            method will catch the error internally and nothing will
	 *            happen.
	 */
	public static void insertRouteSpotsWithoutRepetition(DatabaseWriter dbw,
			int routeId, Route<ModelGraphLink> route) {
		try {

			// CREATE THE ARRAY LIST OF SPOTS WITH NO REPEATED LINKS
			ArrayList<Spot<ModelGraphLink>> UsefulSpots = 
				new ArrayList<Spot<ModelGraphLink>>();

			UsefulSpots.add(route.spots[0]);
			for (int i = 1; i < route.spots.length - 1; i++) {
				if (route.spots[i].link.compareTo(route.spots[i - 1].link) 
						!= 0) {
					Spot<ModelGraphLink> spot = new Spot<ModelGraphLink>(
							route.spots[i].link, 0, route.spots[i].lane);
					UsefulSpots.add(spot);
				}
			}
			if (route.spots[route.spots.length - 1].link.compareTo(UsefulSpots
					.get(UsefulSpots.size() - 1).link) != 0) {
				UsefulSpots.add(route.spots[route.spots.length - 1]);
			} else {
				UsefulSpots.remove(UsefulSpots.size() - 1);
				UsefulSpots.add(route.spots[route.spots.length - 1]);
			}

			// now insert the route in the route spots table
			String ps = "insert a route in the route_spots table";
			dbw.psSetInteger(ps, 1, routeId);
			// This should be ok since the primary key is (fk_route_id, idx)
			short idx = 0;
			for (Spot<ModelGraphLink> spot : UsefulSpots) {
				dbw.psSetSmallInt(ps, 2, idx);
				dbw.psSetInteger(ps, 3, spot.link.id);
				dbw.psSetReal(ps, 4, spot.offset);
				dbw.psUpdate(ps);
				idx++;
			}
		} catch (DatabaseException e) {
			Monitor.err(e);
		} catch (NetconfigException e) {
			Monitor.err(e);
		}
	}

	/**
	 * Projects a latitude/longitude to the nearest (Euclidean distance) link
	 * in this network
	 * 
	 * @param lat
	 *            latitude of the point
	 * @param lon
	 *            longitude of the point
	 * @param radius
	 *            maximum distance in meters to search for links
	 * @return a Spot on the nearest link with the appropriate offset and a 0
	 *         lane.
	 * @throws Exception
	 */
	private static Spot<ModelGraphLink> projectPointOnNet(DatabaseReader dbr,
			Network net, double lat, double lon, int radius) throws Exception,
			DatabaseException {

		// String sql = "SELECT (p).distance, (p).lid, (p).off_set" +
		// " FROM (SELECT model_graph.project_point_on_network2("+
		// " ST_SetSRID(ST_Point(?, ?),4326)::geometry, ?, ?))" +
		// " AS tmp(p)";

		String sql = "SELECT (ret).distance, (ret).lid, (ret).off_set FROM"
				+ " (SELECT model_graph.project_point_on_network2("
				+ "ST_SetSRID(ST_Point(?, ?),4326)," + " ?," + " ?))"
				+ " AS tmp(ret)";

		Spot<ModelGraphLink> retval = null;
		String psKey = "projectpointlalala" + lat + lon + Database.psVersion;
		if (!dbr.psHasPS(psKey))
			dbr.psCreate(psKey, sql);

		dbr.psSetDouble(psKey, 1, lat);
		dbr.psSetDouble(psKey, 2, lon);
		dbr.psSetInteger(psKey, 3, net.nid);
		dbr.psSetInteger(psKey, 4, radius);

		dbr.psQuery(psKey);

		while (dbr.psRSNext(psKey)) {
			double distance = dbr.psRSGetDouble(psKey, "distance");
			int lid = dbr.psRSGetInteger(psKey, "lid");
			float offset = dbr.psRSGetReal(psKey, "off_set");
			// Monitor.out(String.format("Projected %f,%f to lid %d", lat, lon,
			// lid));
			retval = new Spot<ModelGraphLink>(net.getLinkWithID(lid), offset,
					(short) 0);
			System.out.println("Lid: " + net.getLinkWithID(lid) + " offset: "
					+ offset);
			dbr.psRSDestroy(psKey);
			return retval;
		}
		return retval;
	}

}