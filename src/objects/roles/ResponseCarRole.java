package objects.roles;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import ec.util.MersenneTwisterFast;
import objects.Agent;
import objects.Despatch;
import objects.Officer;
import sim.EmergentCrime;
import sim.util.Bag;
import swise.objects.network.GeoNode;

public class ResponseCarRole extends OfficerRole {

	Bag roadNodes = null;
	MersenneTwisterFast random;
	int nextTaskingCost = -1;
	Despatch despatch = null;
	long ticket = -1;
	int myIncident = -1;

	public ResponseCarRole(Officer o, Bag roadNodes, MersenneTwisterFast random, Despatch despatch) {
		super(o);
		try {
			this.roadNodes = (Bag) roadNodes.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		this.random = random;
		this.despatch = despatch;
	}

	public double executePersonalTasking() {

		int myActivity = rolePlayer.getActivity();

		// waiting for meetup
		if(ticket != -1){
			return 1;
		}

		if(myActivity == activity_onWayToStation){
			rolePlayer.navigate(EmergentCrime.resolution);
			return 1;
		}

		// some chance that while dealing with activity, must call for transport and may need to
		// return to station to make a report: check if this is so
		if(myActivity == activity_dealingWithTasking && ticket == -1){
			
			// transport request?
			if(random.nextDouble() < rolePlayer.getWorld().param_transportRequestProb){
				ticket = despatch.receiveRequestForTransport(rolePlayer.geometry.getCoordinate());
				if(verbose)
					System.out.println(rolePlayer.getTime() + "\t" + rolePlayer + "request transport");
				return 1;
			}
			
			// return to station?
			if(random.nextDouble() < rolePlayer.getWorld().param_reportProb){
				rolePlayer.setActivity(activity_onWayToTasking);
				rolePlayer.setCurrentGoal(rolePlayer.getWork());
				return 1;
			}
		}
		
		// if occupied with going to a tasking, determine next step 
		if(myActivity == activity_onWayToTasking){
			
			// if arrived at scene of incident, deal with it
			if(rolePlayer.arrivedAtGoal()){
				myStatus = status_atSceneOfIncident;
				rolePlayer.setActivity(activity_dealingWithTasking);
				
				if(random.nextDouble() < rolePlayer.getWorld().param_redeployProb)
					despatch.receiveReportOfDowngradeInSeverity(myIncident, (Officer)rolePlayer);
				
				if(verbose)
					System.out.println(rolePlayer.getTime() + "\t" + rolePlayer + "deal with incident");

				return nextTaskingCost;
			}
			else
				rolePlayer.navigate(EmergentCrime.resolution);
			
			return 1;
		}

		else if (myActivity != activity_patrolling) {
			myStatus = status_available_resumePatrol;

			rolePlayer.setActivity(activity_patrolling);
			rolePlayer.setSpeed(Officer.defaultSpeed);
			rolePlayer.setMovementRule(Agent.movementRule_roadsOnly);
			nextTaskingCost = -1;
			
			if(verbose)
				System.out.println(rolePlayer.getTime() + "\t" + rolePlayer + " start patrolling");

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

	public void redirectToResponse(Coordinate location, int time, int incident) {

		myIncident = incident;
		myStatus = OfficerRole.status_enRouteToIncident;
		
		rolePlayer.setActivity(activity_onWayToTasking);
		rolePlayer.setSpeed(Officer.topSpeed);
		rolePlayer.setCurrentGoal(location);
		rolePlayer.setMovementRule(Agent.movementRule_roadsOnlyNoLaws);
		rolePlayer.updateStatus(OfficerRole.status_enRouteToIncident);
		nextTaskingCost = time;

		if(verbose)
			System.out.println(rolePlayer.getTime() + "\t" + rolePlayer.toString() + " respond to " + location.toString());
	}
	
	public boolean interfaceWithTransportVan(long ticket){
		if(this.ticket == ticket){
			/*
			myStatus = status_available;
			rolePlayer.setActivity(activity_patrolling);
			*/
			myStatus = OfficerRole.status_committedButDeployable;

			rolePlayer.updateStatus(OfficerRole.status_committedButDeployable);
			rolePlayer.setActivity(activity_onWayToTasking);
			rolePlayer.setCurrentGoal(rolePlayer.getWork());

			rolePlayer.setMovementRule(Agent.movementRule_roadsOnly);
			nextTaskingCost = rolePlayer.getWorld().param_reportTimeCommitment;
			this.ticket = -1;

			if(verbose)
				System.out.println(rolePlayer.getTime() + "\t" + rolePlayer + " interacted with transport van");

			return true;
		}
		else{
			return false;
		}
	}
	
	public long getTicket(){ return ticket; }
}

