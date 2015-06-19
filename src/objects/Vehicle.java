package objects;

import java.util.ArrayList;
import java.util.HashMap;

import sim.EmergentCrime;
import sim.engine.SimState;
import swise.agents.MobileAgent;

import com.vividsolutions.jts.geom.Coordinate;

public class Vehicle extends MobileAgent {
	
	EmergentCrime state;
	ArrayList <Officer> officers = new ArrayList <Officer> ();
	static public double vehicle_DefaultSpeed = 200;
	static public double vehicle_MaxSpeed = 1000;
	
	
	public Vehicle(Coordinate position, EmergentCrime world){
		super(position);
		state = world;
	}

	/**
	 * Add a new Officer to the Vehicle
	 * 
	 * @param o - the Officer which joins the vehicle
	 */
	public void acquireOfficer(Officer o){
		officers.add(o);
	}
	
	/**
	 * Removes the specified Officer from the Vehicle, updating the Officer's position (and potentially
	 * that of the other Officers in the Vehicle, scheduling new Officers to drive as necessary)
	 *  
	 * @param o - the Officer to remove
	 */
	public void loseOfficer(Officer o){
		if(!officers.contains(o)){
			System.out.println("ERROR: officer not in car!");
			return;
		}

		if(officers.indexOf(o) == 0){
			HashMap <String, Object> position = officers.get(0).getPositionalInformation();
			for(int i = 1; i < officers.size(); i++)
				officers.get(i).setPositionalInformation(position);
			
			if(officers.size() > 1){
				officers.get(1).schedule(state.schedule.getTime() + 1);				
			}
		}
		else{
			HashMap <String, Object> position = officers.get(0).getPositionalInformation();
			o.setPositionalInformation(position);
		}
			
		officers.remove(o);
	}
	
	/**
	 * Runs through all the Officers in the Vehicle and updates their positions
	 */
	public void updateAllOfficerPositions(){
		if(officers.size() < 1) return;
		
		Coordinate c = officers.get(0).geometry.getCoordinate();
		for(int i = 1; i < officers.size(); i++)
			officers.get(i).movedTo(c);
	}
	
	public void updateAllOfficersStatus(int status){
		for(int i = 0; i < officers.size(); i++)
			officers.get(i).updateStatus(status);
		
	}
	
	public void activateAllOfficers(){
		//TODO
	}
	
	public boolean containsOfficer(Officer o){
		return officers.contains(o);
	}
}