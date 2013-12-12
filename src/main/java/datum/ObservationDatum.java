package datum;

import core.Time;

/**
 * This class represents an observation. 
 * @author Boris Prodhomme
 * @author Magnus Fransson
 */
public class ObservationDatum {
    private int readerId;
    private String userId;
    private Time time;
    
    public ObservationDatum(int readerId, String userId, Time time){
        this.readerId = readerId;
        this.userId = userId;
        this.time = time;
    }
    
    //Getters and Setters
    public int getReaderId(){
        return readerId;
    }
    
    public Time getDate() {
    	return Time.newTimeFromTime(this.time);
    }
    
    public String getUsedId() {
    	return this.userId;
    }
}