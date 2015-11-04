package objects.roles;

import objects.Officer;
import sim.EmergentCrime;

import com.vividsolutions.jts.geom.Coordinate;

public class StationaryTasking extends OfficerRole {

		Coordinate taskLocation;
		
		public StationaryTasking (Officer o, Coordinate c){
			super(o);
			taskLocation = c;
		}
		
		public Coordinate getLocation(){ return taskLocation; }
		
		public double executePersonalTasking(){
			
			// if not already at tasking, navigate there
			if(rolePlayer.geometry.getCoordinate().distance(taskLocation) > EmergentCrime.spatialResolution){
				if(rolePlayer.arrivedAtGoal())
					rolePlayer.headFor(taskLocation);
				else
					rolePlayer.navigate(EmergentCrime.spatialResolution);
			}
			// TODO: this is meaningless
			return 0;
		}

}

