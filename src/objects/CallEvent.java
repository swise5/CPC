package objects;

import com.vividsolutions.jts.geom.Geometry;

public class CallEvent implements Comparable {

	int grading = 0; // Grading: 0 = I, 1 = S, 2 = E, 3 = R
	Geometry location = null;
	int incidentNumber;
	double time = -1;
	int key = -1;
	
	public CallEvent(int grade, Geometry location, double time, int incidentNumber, int key){
		this.time = time;//System.currentTimeMillis();
		grading = grade;
		this.location = location;
		this.incidentNumber = incidentNumber;
		this.key = key;
	}
	
	@Override
	public int compareTo(Object arg0) {
		if(arg0 instanceof CallEvent){
			CallEvent ce = (CallEvent) arg0;
			if(ce.grading > grading) return -1;
			else if(ce.time > time) return -1;
			else if(ce.time < time) return 1;
			else if(ce.key < key) return -1;
			else return 0;
		}
		return 0;
	}
	
	public Geometry getLocation(){ return location; }		
	public int getGrade(){ return grading; }
	public int getIncidentNumber(){ return incidentNumber; }
	public double getTime(){ return time; }
}