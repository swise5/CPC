package objects;

import java.util.ArrayList;
import java.util.HashMap;

import sim.EmergentCrime;
import sim.engine.SimState;

import com.vividsolutions.jts.geom.Coordinate;

public class Vehicle extends Agent {
	
	ArrayList <Officer> officers = new ArrayList <Officer> ();
	static public double vehicle_DefaultSpeed = 200;
	static public double vehicle_MaxSpeed = 1000;
	
	
	public Vehicle(String id, Coordinate position, Coordinate station, EmergentCrime world, double speed, String taskingType){
		super(id, position, station, world, vehicle_DefaultSpeed, world.vehicleLayer, world.roads);
	}
	
	public void acquireOfficer(Officer o){
		officers.add(o);
		o.speed = vehicle_DefaultSpeed;
	}
	
	public void loseOfficer(Officer o){
		if(!officers.contains(o)){
			System.out.println("ERROR: officer not in car!");
			return;
		}

		if(officers.size() > 1){
			HashMap <String, Object> position = officers.get(0).getPositionalInformation();
			for(int i = 1; i < officers.size(); i++)
				officers.get(i).setPositionalInformation(position);
		}
		
		o.speed = Officer.defaultSpeed;
		officers.remove(o);
	}
	
	public void step(SimState state){
		
		if(officers.size() == 0){
			System.out.println("ERROR: empty vehicle activated");
			return;
		}
		
		Officer driver = officers.get(0);
		double nextActivation = driver.externalStep(state);
		
		Coordinate ourCurrentPos = driver.geometry.getCoordinate();
		
		for(int i = 1; i < officers.size(); i++){
			Officer passenger = officers.get(i);
			passenger.moveTo(ourCurrentPos);
		}
		
		updateLoc(ourCurrentPos);
		
	}
}