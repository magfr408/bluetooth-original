package filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import core.Monitor;
import datum.TripComparator;
import datum.TripDatum;

/**
 * This class represents a list of trips. It is useful because it contains fields
 * and methods to compute statistics of a list of trips.
 * @author bobo12
 */
public class ListTrips {
    //list of trips ordered by input time

    private ArrayList<TripDatum> trips;
    private double[] sortedTravelTimes;
    //For MAD technique
    private double medianTT;
    // median absolute deviation
    private double[] medianTTDeviations;
    private double mad;
    //For IQR technique
    private double firstQuartileTT;
    private double thirdQuartileTT;

    public ListTrips(ArrayList<TripDatum> trips) {
        if (trips == null || trips.isEmpty()) {
            throw new IllegalArgumentException("The list of trips is empty");
        } else {
            Collections.sort(trips, TripComparator.Order.ByInputTime.ascending());
            this.trips = trips;
        }
    }
    
    
    /**
     * Was useful at one point but only because we had some issues with 
     * "simplified" mac_adresses while readin trips (not observations) from file.
     * 
     * Takes a set trips, say (id, t) = {{1, 14}, {2, 12}, {1, 15}}. Id = 1 has
     * two occurrences. The returned set will be {{1, (14+15)/2}, {2, 12}}.
     * 
    public void removeDuplicatesKeepAvgTT() {
    	int idx = -1;
    	int s1 = this.trips.size();
    	
    	for (int i = 0; i < s1; i++) {
        	TripDatum t1 = this.trips.get(i);
    		
    		int s2 = this.trips.size();
    		for (int j = i+1; j < s2; j++) {
    			TripDatum t2 = this.trips.get(j);
    			if (t1.getMacAddress().equals(t2.getMacAddress())) {
    				t1.setTravelTime(t1.getTravelTime()+t2.getTravelTime());
    				t1.setNbObservations(t1.getNbObservations()+1);
    				
    				this.trips.remove(j);
    				s1 = s1-1;
    				s2 = s2-1;
    			}
    		}
    		t1.setTravelTime(Math.round(t1.getTravelTime()/t1.getNbObservations()));
    	}
    }
    */
    
    
    public void filter(float precision, FilterSettings.FilterMethod filterMethod) {
        if (!trips.isEmpty()) {
        	
            getStatistics(filterMethod);
            double upperFence = Double.MAX_VALUE;
            double lowerFence = Double.MIN_VALUE;

            switch (filterMethod) {
                case MAD:                	
                    upperFence = medianTT + precision * mad;
                    if (medianTT / 2 < medianTT - precision * mad) {
                        lowerFence = medianTT - precision * mad;
                    } else {
                        lowerFence = medianTT / 2;
                    }
                    
                    break;
                case IQR:                    
                	upperFence = thirdQuartileTT + (precision - 1) * getInterquartileTT();
                    lowerFence = firstQuartileTT - (precision - 1) * getInterquartileTT();
                    break;
                default:
                    Monitor.err("you have to choose between two techniques : MAD "
                            + "and IQR \n");
                    break;
            }
            for (TripDatum trip : trips) {
                trip.setFilterMethod(filterMethod);
                if (trip.getTravelTime() > upperFence || trip.getTravelTime() < lowerFence) {
                    trip.setIsOutlier(true);
                }
            }
        }
    }

    public void getStatistics(FilterSettings.FilterMethod filtermethod) {
        sortedTravelTimes = new double[trips.size()];
        medianTTDeviations = new double[trips.size()];
        for (int i = 0; i < trips.size(); i++) {
            sortedTravelTimes[i] = trips.get(i).getTravelTime();
        }
        Arrays.sort(sortedTravelTimes);

        switch (filtermethod) {
            case MAD:
                medianTT = computeMedian(sortedTravelTimes);
                computeMAD();
                break;
            case IQR:
                computeQuartiles();
                break;
            default:
                Monitor.err("you have to choose between "
                        + "two outlier filtering techniques : MAD and IQR");
                break;
        }
    }

    /*
     * compute the percentile using the same algorithm as the percentile method
     * in the StatUtils class of apache commons math
     */
    public static double getPercentile(double[] sortedArray, double p) throws
            IllegalArgumentException {
        if (sortedArray == null || sortedArray.length == 0) {
            throw new IllegalArgumentException("empty array: cannot get any"
                    + " percentile");
        }
        if (p < 0 || p > 100) {
            throw new IllegalArgumentException("p must be between 0 and 100");
        }
        int n = sortedArray.length;
        if (n == 1) {
            return sortedArray[0];
        }
        double pos = p * (n + 1) / 100;
        int floorPos = (int) Math.floor(pos);
        double d = pos - floorPos;//fractional part of pos
        if (pos < 1) {
            return sortedArray[0];
        }
        if (pos >= n) {
            return sortedArray[n - 1];
        }
        double lower = sortedArray[floorPos - 1];
        double upper = sortedArray[floorPos];
        return lower + d * (upper - lower);
    }

    /*
     * gets the interquartile range defined by Q3-Q1
     */
    public double getInterquartileTT() {
        return thirdQuartileTT - firstQuartileTT;
    }

    /*
     * gets the median of sorted data without considering points that we know
     * a priori as outliers
     */
    public double computeMedian(double[] sortedArray) {
        return getPercentile(sortedArray, 50);
    }

    private void computeQuartiles() {
        firstQuartileTT = getPercentile(sortedTravelTimes, 25);
        thirdQuartileTT = getPercentile(sortedTravelTimes, 75);
    }

    private void computeMAD() {
        for (int i = 0; i < sortedTravelTimes.length; i++) {
            medianTTDeviations[i] = Math.abs(sortedTravelTimes[i] - medianTT);
        }
        //sort the median deviations now
        Arrays.sort(medianTTDeviations);
        mad = 1.4826f * computeMedian(medianTTDeviations);
    }

    public ArrayList<TripDatum> getTrips() {
        return trips;
    }

    public double getMedian() {
        return medianTT;
    }

    public double getFirstQuartile() {
        return firstQuartileTT;
    }

    public double getThirdQuartile() {
        return thirdQuartileTT;
    }

    public double getMAD() {
        return mad;
    }
}