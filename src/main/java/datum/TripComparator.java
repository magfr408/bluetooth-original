package datum;

import java.util.Comparator;
import java.util.Collections;

/**
 * This class enables us to compare trips by their input times, output times or
 * travel times
 * @author Boris Prodhomme
 */
public class TripComparator {

    public static enum Order implements Comparator<TripDatum> {

        ByInputTime() {
            public int compare(TripDatum t1, TripDatum t2) {
                if (t1.getStartTime().compareTo(t2.getStartTime()) > 0)
                    return 1;
                else if (t1.getStartTime().compareTo(t2.getStartTime()) < 0)
                    return -1;
                else return 0;
            }
        },
        
        ByOutputTime() {
            public int compare(TripDatum t1, TripDatum t2) {
                if (t1.getEndTime().compareTo(t2.getEndTime()) > 0) 
                    return 1;
                else if (t1.getEndTime().compareTo(t2.getEndTime()) < 0)
                    return -1;
                else return 0;
            }
        },
        
        ByTravelTime() {
            public int compare(TripDatum t1, TripDatum t2){
                if(t1.getTravelTime() > t2.getTravelTime()) 
                    return 1;
                else if (t1.getTravelTime() < t2.getTravelTime())
                    return -1;
                else return 0;
            }
        };

        public Comparator ascending() {
            return this;
        }

        public Comparator descending() {
            return Collections.reverseOrder(this);
        }
    }
}