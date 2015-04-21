package objects.roles;

import java.util.ArrayList;

import objects.Agent;
import objects.Officer;
import sim.EmergentCrime;
import sim.EmergentCrime.CallEvent;
import sim.util.Bag;
import swise.objects.network.GeoNode;

import com.vividsolutions.jts.geom.Coordinate;

import ec.util.MersenneTwisterFast;

public class TransportVanRole extends OfficerRole {

	EmergentCrime world;
	MersenneTwisterFast random;
	long ticket = -1;

	public TransportVanRole(Officer o, MersenneTwisterFast random, EmergentCrime world) {
		super(o);

		this.random = random;
		this.world = world;
	}

	public double executePersonalTasking() {

		int myActivity = rolePlayer.getActivity();

		// if occupied with going to a tasking, determine next step 
		if(myActivity == activity_onWayToTasking){
			
			// if arrived at scene of incident, deal with it
			if(rolePlayer.arrivedAtGoal()){
				
				Bag possibleOthers = world.officerLayer.getObjectsWithinDistance(rolePlayer.geometry, 10000);//EmergentCrime.resolution); // TODO: wtf why
				boolean successful = false;
				for(Object o: possibleOthers){
					Officer off = (Officer) o;

					if(off.getRole() instanceof ResponseCarRole){
						successful = (successful || ((ResponseCarRole)off.getRole()).interfaceWithTransportVan(ticket));
					}
						
				}
				if(!successful){
					System.out.println("unsuccessful meetup");
				}
				rolePlayer.setActivity(activity_dealingWithTasking);
				ticket = -1;
				return 15; // 15 minutes to get them into the car and secured
			}
			else
				rolePlayer.navigate(EmergentCrime.resolution);
			
			return 1;
		}

		else if (myActivity == activity_dealingWithTasking) {
			rolePlayer.setCurrentGoal(rolePlayer.getWork());
			rolePlayer.setActivity(activity_onWayToStation);
			return 1;
		}

		else if(myActivity == activity_onWayToStation){
			if(rolePlayer.arrivedAtGoal()){
				myStatus = status_available;
				rolePlayer.setActivity(activity_noActivity);
				return 15; // deal with the stuff
			}
				
			else{
				rolePlayer.navigate(EmergentCrime.resolution);
				return 1;
			}
		}

		if (rolePlayer.getGoal() != null && !rolePlayer.arrivedAtGoal())
			rolePlayer.navigate(EmergentCrime.resolution);

		return 1;

	}

	public void redirectToResponse(Coordinate location, long ticket) {

		myStatus = status_occupied;
		rolePlayer.setActivity(activity_onWayToTasking);
		rolePlayer.setCurrentGoal(location);;
		rolePlayer.updateStatus(status_occupied);
		this.ticket = ticket;
	}
}
