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

public class ReportCarRole extends OfficerRole {

	EmergentCrime world;
	Bag roadNodes = null;
	MersenneTwisterFast random;
	
	public static double param_reportProb = .25;
	public static int param_reportTimeCommitment = 60;


	public ReportCarRole(Officer o, Bag roadNodes,
			MersenneTwisterFast random, EmergentCrime world) {
		super(o);
		try {
			this.roadNodes = (Bag) roadNodes.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		this.random = random;
		this.world = world;
	}

	public double executePersonalTasking() {

		int myActivity = rolePlayer.getActivity();

		if(myActivity == activity_onWayToStation){
			rolePlayer.navigate(EmergentCrime.resolution);
			return 1;
		}

		// if occupied with going to a tasking, determine next step 
		if(myActivity == activity_onWayToTasking){
			
			// if arrived at scene of incident, deal with it
			if(rolePlayer.arrivedAtGoal()){
				
				rolePlayer.setActivity(activity_dealingWithTasking);
				
				return (random.nextInt(4) + 1) * 15;
			}
			else
				rolePlayer.navigate(EmergentCrime.resolution);
			
			return 1;
		}

		// at the end of a meeting, may need to return to station
		if(myActivity == activity_dealingWithTasking){
			// return to station?
			if(random.nextDouble() < param_reportProb){
				rolePlayer.setActivity(activity_onWayToTasking);
				rolePlayer.setCurrentGoal(rolePlayer.getWork());
				return 1;
			}
		}
		
		if (world.hasExtendedEvent()) {

			CallEvent callEvent = world.getNextExtendedEvent();

			// if there's no CallEvent in the queue, just patrol the area
			if (callEvent != null) {

				myStatus = status_occupied;
				rolePlayer.setActivity(activity_onWayToTasking);
				rolePlayer.setCurrentGoal(callEvent.getLocation().getCoordinate());
				rolePlayer.updateStatus(OfficerRole.status_occupied);

				return 1;
			}
		}

		// otherwise transition into patrolling
		if(myActivity != activity_patrolling){
			myActivity = activity_patrolling;
			myStatus = status_available;
		}
		
		if (rolePlayer.getGoal() != null && !rolePlayer.arrivedAtGoal())
			rolePlayer.navigate(EmergentCrime.resolution);
		else {
			GeoNode gn = (GeoNode) roadNodes.get(random.nextInt(roadNodes.size()));
			rolePlayer.setCurrentGoal(gn.geometry.getCoordinate());
			while (rolePlayer.getPath() == null && !rolePlayer.arrivedAtGoal()) {
				gn = (GeoNode) roadNodes.get(random.nextInt(roadNodes.size()));
				rolePlayer.setCurrentGoal(gn.geometry.getCoordinate());
			}
		}

		return 1;

	}

}
