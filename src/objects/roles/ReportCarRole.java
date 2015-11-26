package objects.roles;

import java.util.ArrayList;

import objects.Agent;
import objects.Despatch;
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
	Despatch despatch;


	public ReportCarRole(Officer o, Bag roadNodes, EmergentCrime world) {
		super(o);
		try {
			this.roadNodes = (Bag) world.roadNodes.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		random = world.random;
		this.world = world;
		despatch = world.despatch;
	}

	public double executePersonalTasking() {

		int myActivity = rolePlayer.getActivity();

		if(myActivity == activity_onWayToStation){
			rolePlayer.navigate(EmergentCrime.spatialResolution);
			return 1;
		}

		// if occupied with going to a tasking, determine next step 
		if(myActivity == activity_onWayToTasking){
			
			// if arrived at scene of incident, deal with it
			if(rolePlayer.arrivedAtGoal()){
				
				if(myStatus == status_enRouteToIncident)
					despatch.recordResponseTime(myIncident);

				if(rolePlayer.getGoal().equals(rolePlayer.getWork()))
					myStatus = status_committedAndUnavailable;//status_available_office;
				else
					myStatus = status_atSceneOfIncident;
				
				rolePlayer.updateStatus(myStatus);
				rolePlayer.setActivity(activity_dealingWithTasking);

		//		myStatus = status_committedButDeployable;
		//		rolePlayer.setActivity(activity_dealingWithTasking);

				if(myStatus == status_atSceneOfIncident && random.nextDouble() < rolePlayer.getWorld().param_redeployProb && myIncident != 1) // TODO: take this hack back out!!!!
					despatch.receiveReportOfDowngradeInSeverity(myIncident, (Officer)rolePlayer);
				
				if(verbose)
					System.out.println(rolePlayer.getTime() + "\t" + rolePlayer + "deal with incident");

				return world.param_reportTimeCommitment;//(random.nextInt(4) + 1) * 15;
			}
			else
				rolePlayer.navigate(EmergentCrime.spatialResolution);
			
			return 1;
		}

		// at the end of a meeting, may need to return to station
		if(myActivity == activity_dealingWithTasking){
			// return to station?
			if(random.nextDouble() < rolePlayer.getWorld().param_reportProb){
				myActivity = activity_onWayToTasking;
				rolePlayer.setActivity(activity_onWayToTasking);
				rolePlayer.setCurrentGoal(rolePlayer.getWork());
				return 1;
			}
		}
		
/*		if (world.hasExtendedEvent()) {

			CallEvent callEvent = world.getNextExtendedEvent();

			// if there's no CallEvent in the queue, just patrol the area
			if (callEvent != null) {

				myStatus = status_committedAndUnavailable;
				
				rolePlayer.setActivity(activity_onWayToTasking);
				rolePlayer.setCurrentGoal(callEvent.getLocation().getCoordinate());
				rolePlayer.updateStatus(OfficerRole.status_committedAndUnavailable);

				return 1;
			}
		}
*/
		// otherwise transition into patrolling
		if(myActivity != activity_patrolling){
			myActivity = activity_patrolling;
			rolePlayer.setActivity(activity_patrolling);
			myStatus = status_available_resumePatrol;
		}
		
		if (rolePlayer.getGoal() != null && !rolePlayer.arrivedAtGoal())
			rolePlayer.navigate(EmergentCrime.spatialResolution);
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
