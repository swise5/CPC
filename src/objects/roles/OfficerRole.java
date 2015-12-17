package objects.roles;

import com.vividsolutions.jts.geom.Coordinate;

import objects.Agent;
import objects.Officer;
import sim.EmergentCrime;

public class OfficerRole {
	
	int myStatus = 0;
	int shiftStartTime = -1;
	int shiftEndTime = -1;
	Coordinate station = null;
	
	int nextTaskingCost = -1;
	int myIncident = -1;

	
	double vehicle = -1;

	// STATUSES ////////////
	
	public static int status_onDuty = 1;
	public static int status_available_resumePatrol = 2;
	public static int status_available_office = 3;
	public static int status_refs = 4;
	public static int status_enRouteToIncident = 5;
	public static int status_atSceneOfIncident = 6;
	public static int status_committedButDeployable = 7;
	public static int status_committedAndUnavailable = 8;
	public static int status_prisonerEscort = 9;
	public static int status_atCourt = 10;
	public static int status_offDuty = 11;
	
	// ACTIVITIES ////////////

	public static int activity_briefing = 2;
	public static int activity_carCheck = 4;
	public static int activity_finalPaperwork = 6;
	
	public static int activity_patrolling = 11;
	public static int activity_dealingWithTasking = 10;
	public static int activity_refs = 5;
	public static int activity_onWayToTasking = 9;
	public static int activity_onWayToStation = 12;
	public static int activity_waiting = 13;
	
	public static int activity_noActivity = 0;
	boolean verbose = false;
	
	Agent rolePlayer = null;

	
	public OfficerRole(Officer o){
		rolePlayer = o;
		if(o != null)
			station = rolePlayer.getBase();
	}
	
	/**
	 * Needs to:
	 * 
	 * > set status
	 * > set goal location
	 * > set movement rule
	 * 
	 * return: time of next activation
	 */
	public double activate(double time){
		
		// ************** BEGINNING THE DAY *********************
		int myActivity = this.rolePlayer.getActivity();
		
		// if activated while off-duty, it's time for work
		if (myStatus == status_offDuty && rolePlayer.geometry.getCoordinate().distance(station) < EmergentCrime.spatialResolution) {
			// disabled while doing vehicles only
			// myStatus = status_occupied;
			// rolePlayer.setActivity(activity_briefing);
			if(verbose)
				System.out.println(rolePlayer.getTime() + "\t" + rolePlayer + "goes on duty");
			myStatus = status_available_resumePatrol;
			rolePlayer.setActivity(activity_noActivity);
			// rolePlayer.setActivity(activity_patrolling);
			shiftEndTime = (int) (time + (60 * 8 / EmergentCrime.temporalResolution_minutesPerTick));
			return time + 1;
			// TODO: proper briefing time
			// return time + 30;
		}

		// if just finished with the car check, ready to go! Execute personal tasking
		if(myActivity == activity_carCheck){
			myStatus = status_available_resumePatrol;
			rolePlayer.setActivity(activity_noActivity);
			shiftEndTime = (int)(time + (60 * 8 / EmergentCrime.temporalResolution_minutesPerTick));
			if(verbose)
				System.out.println(rolePlayer.getTime() + "\t" + rolePlayer + "car check");
			return time + executePersonalTasking();
		}
	
		// ************** END OF DAY *********************

		// deal with shift being over
		if(time >= shiftEndTime){
			
			// if already at the station, go back! 
			if((myActivity == activity_onWayToStation || myActivity == activity_noActivity) // either on way or already there
					&& rolePlayer.geometry.getCoordinate().distance(station) < EmergentCrime.spatialResolution){ // already there

				if(verbose)
					System.out.println(rolePlayer.getTime() + "\t" + rolePlayer + "-- change shift");

				myStatus = status_committedAndUnavailable;
				rolePlayer.setActivity(activity_carCheck);
				return time + 15 / EmergentCrime.temporalResolution_minutesPerTick;
			}
			// if on the way to the stations and not there yet, keep moving
			else if(myActivity == activity_onWayToStation && !rolePlayer.arrivedAtGoal()){
				rolePlayer.navigate(EmergentCrime.spatialResolution);
				return time + 1;
			}
			// otherwise return to the station
			else{
				myStatus = status_committedAndUnavailable;
				rolePlayer.setActivity(activity_onWayToStation);
				rolePlayer.setCurrentGoal(station);

				if(verbose)
					System.out.println(rolePlayer.getTime() + "\t" + rolePlayer + "return to station");

				return time + 1;
			}
				
		}

		// time to go back after shift is over
		else if(time + (60 / EmergentCrime.temporalResolution_minutesPerTick) > shiftEndTime 
				&& myActivity != activity_onWayToStation && myActivity != activity_onWayToTasking 
				&& rolePlayer.getBase().distance(rolePlayer.geometry.getCoordinate()) > EmergentCrime.spatialResolution){
			// if it's time to go back to the station: set activity, set destination, and 
			// schedule self to go back
			myStatus = status_committedButDeployable;
			rolePlayer.setActivity(activity_onWayToStation);
			rolePlayer.setCurrentGoal(station);

			if(verbose)
				System.out.println(rolePlayer.getTime() + "\t" + rolePlayer + "return to station");

			return time + 1;
		}

	
		// ************** PERSONAL TASKING *********************

		return time + executePersonalTasking();

//		System.out.println("Problem with Officer");
//		return 0;
	}
	
	public void redirectToResponse(Coordinate location, int time, int incident, int grading) {

		myIncident = incident;
		myStatus = OfficerRole.status_enRouteToIncident;
		
		rolePlayer.setActivity(activity_onWayToTasking);
		rolePlayer.setCurrentGoal(location);
		rolePlayer.updateStatus(OfficerRole.status_enRouteToIncident);

		if(grading == 0){
			rolePlayer.setSpeed(rolePlayer.getWorld().param_topSpeed);
			rolePlayer.setMovementRule(Agent.movementRule_roadsOnlyNoLaws);
		}
		else {
			rolePlayer.setSpeed(rolePlayer.getWorld().param_defaultSpeed);
			rolePlayer.setMovementRule(Agent.movementRule_roadsOnly);
		}
		
		nextTaskingCost = time;

		if(verbose)
			System.out.println(rolePlayer.getTime() + "\t" + rolePlayer.toString() + " respond to " + location.toString());
	}

	public double executePersonalTasking(){
		System.out.println("job job job");
		return 1;
	}
	
	public int getStatus(){ return myStatus; }
}