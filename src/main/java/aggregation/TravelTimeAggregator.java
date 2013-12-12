package aggregation;

import core.DatabaseException;
import core.DatabaseReader;
import core.DatabaseWriter;
import core.Monitor;
import core.Time;
import netconfig.NetconfigException;

/**
 * This class holds the code that performs all travel
 * time aggregation.  We separe into 2 methods : one for the
 * HWY travel times, and another for the BT travel times.
 */
/**
 *
 * @author Agathe
 * @author Magnus
 */
public class TravelTimeAggregator {

    protected DatabaseReader dbr = null;
    protected DatabaseWriter dbw = null;
    private String schema = "bluetooth";

   
    /*
     * Constructor to avoid to many database objects.
     */
    public TravelTimeAggregator(DatabaseReader dbr, DatabaseWriter dbw) 
    	throws DatabaseException {
    	
    	this.dbr = dbr;
    	this.dbw = dbw;
    	
    	 String getperiodsPS = "get aggregation periods";

         if (!dbr.psHasPS(getperiodsPS)) {
             dbr.psCreate(getperiodsPS, 
             	"SELECT aggregation_period AS period"
                 + " FROM " + this.schema + ".aggregation"
                 + " ORDER BY period DESC");
         }
    }
    
    /*
     * Call this method in order to aggregate the filtered 
     * Bluetooth travel times between start and end time.
     */
    public void BTAggregate(Time start, Time end, String filterMethod) 
    	throws NetconfigException, DatabaseException {
        
    	String getperiodsPS = "get aggregation periods";
        
    	dbr.psQuery(getperiodsPS);
        
    	while (dbr.psRSNext(getperiodsPS)) {
            Integer aggregationperiod = 
            	dbr.psRSGetInteger(getperiodsPS, "period");
            this.AggregatingBTTravelTimes(aggregationperiod, start, end, filterMethod);
        }
        
    	Monitor.info("Aggregated without errors.");
    }

    
    private void AggregatingBTTravelTimes(
    		int aggregationperiod, Time start, Time end,
    		String filterMethod) 
    	throws NetconfigException, DatabaseException {
        
    	//Post info.
    	Monitor.info(
        	String.format(
        		"Begin aggregation of BT output traveltimes and insertion in "
        		+ this.schema + 
        		".aggregated_traveltime table for agg period %d...", 
        		aggregationperiod));

    	Monitor.info("TravelTimeAggregator: Removing old entries from DB.");
    	
        String deleteVelPS = "delete old BT travel times";
        
        if (!dbw.psHasPS(deleteVelPS)) {
            dbw.psCreate(deleteVelPS, 
            		"DELETE FROM "
                    + this.schema + ".aggregated_traveltime "
                    + "WHERE date BETWEEN ? AND ? "
                    + "AND fk_aggregation_period = ?");
        }

        String getVelPS = "get and aggregate BT travel times" + aggregationperiod;

        if (!dbw.psHasPS(getVelPS)) {
            dbw.psCreate(getVelPS, String.format(
            		"INSERT INTO " + this.schema + ".aggregated_traveltime "
                    + "SELECT"
                    +		" rout AS fk_id,"
                    +		" %d AS fk_aggregation_period,"
                    +		" dat AS date,"
                    +		" averageTT AS bt_traveltime,"
                    +		" stddev AS bt_std_dev"
                    + " FROM"
                    + 		" (SELECT "
                    + 			" ts_round(date, ? * 60) AS dat,"
                    + 			" id AS rout,"
                    + 			" avg(traveltime) AS averageTT,"
                    + 			" COALESCE(stddev(traveltime), 0) AS stddev"
                    + 		" FROM "
                    + 			this.schema + ".filtered AS f, "
                    + 			this.schema + ".route AS r "
                    + 		" WHERE"
                    + 			" f.fk_start_id = r.fk_start_id"
                    + 			" AND NOT(f.isoutlier)"
                    + 			" AND f.filtermethod = ?"
                    + 			" AND date BETWEEN ? AND ?"
                    + 			" AND f.fk_end_id = r.fk_end_id"
                    + 			" GROUP BY dat,rout"
                    + 		" ORDER BY dat,rout) AS subq", aggregationperiod));
        }

        dbw.psSetInteger(getVelPS, 1, aggregationperiod);
        dbw.psSetVarChar(getVelPS, 2, filterMethod);
        dbw.psSetTimestamp(getVelPS, 3, start);
        dbw.psSetTimestamp(getVelPS, 4, end);
        dbw.psSetTimestamp(deleteVelPS, 1, start);
        dbw.psSetTimestamp(deleteVelPS, 2, end);
        dbw.psSetInteger(deleteVelPS, 3, aggregationperiod);

        // delete ant duplicate rows first
        dbw.psUpdate(deleteVelPS);
        // Actually do the query
        dbw.psUpdate(getVelPS);

        Monitor.info(
        	String.format(
        		"...aggregation of traveltimes for agg period %d complete",
        		aggregationperiod));
    }
    

    public static void main(String[] args) {
    	Monitor.set_db_env("mms");
    	
    	try {
            
            TravelTimeAggregator test = new TravelTimeAggregator(
            		new DatabaseReader(Monitor.get_db_env(), "bluetoothStockholm"),
            		new DatabaseWriter(Monitor.get_db_env(), "bluetoothStockholm"));
            
            Time start = 
            	Time.newTimeFromBerkeleyDateTime(2013, 3, 18, 00, 0, 0, 0);
            Time end = 
            	Time.newTimeFromBerkeleyDateTime(2013, 3, 26, 00, 0, 0, 0);
            String filterMethod = "IQR";
            
            test.BTAggregate(start, end, filterMethod);
    	} catch (DatabaseException dbe) {
    		Monitor.err(dbe);
    	} catch (NetconfigException nce) {
    		Monitor.err(nce);
    	}
    }
}