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
	
	double vehicle = -1;
	public static double speed_vehicle = 2000;
	public static double speed_vehicle_noLights = 3000;
	public static double speed_foot = 800;

	// STATUSES ////////////
	
	public static int status_offduty = 0;
	public static int status_occupied = 1;
	public static int status_refs = 2;
	public static int status_available = 3;
	
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
		if (myStatus == status_offduty && rolePlayer.geometry.getCoordinate().distance(station) < EmergentCrime.resolution) {
			// disabled while doing vehicles only
			// myStatus = status_occupied;
			// rolePlayer.setActivity(activity_briefing);
			if(verbose)
				System.out.println(rolePlayer.getTime() + "\t" + rolePlayer + "goes on duty");
			myStatus = status_available;
			rolePlayer.setActivity(activity_noActivity);
			// rolePlayer.setActivity(activity_patrolling);
			shiftEndTime = (int) (time + 60 * 8);
			return time + 1;
			// TODO: proper briefing time
			// return time + 30;
		}

		// if just finished with the car check, ready to go! Execute perosonal tasking
		if(myActivity == activity_carCheck){
			myStatus = status_available;
			rolePlayer.setActivity(activity_noActivity);
			shiftEndTime = (int)(time + 60 * 8);
			if(verbose)
				System.out.println(rolePlayer.getTime() + "\t" + rolePlayer + "car check");
			return time + executePersonalTasking();
		}
	
		// ************** END OF DAY *********************

		// deal with shift being over
		if(time >= shiftEndTime){ //TODO need to add more paperwork time, come back in time for paperwork
			
			// if already at the station, go back! 
			if((myActivity == activity_onWayToStation || myActivity == activity_noActivity) // either on way or already there
					&& rolePlayer.geometry.getCoordinate().distance(station) < EmergentCrime.resolution){ // already there

				if(verbose)
					System.out.println(rolePlayer.getTime() + "\t" + rolePlayer + "-- change shift");

				myStatus = status_occupied;
				rolePlayer.setActivity(activity_carCheck);
				return time + 15;
			}
			else if(myActivity == activity_onWayToStation && !rolePlayer.arrivedAtGoal()){
				rolePlayer.navigate(EmergentCrime.resolution);
				return time + 1;
			}
			else{
				myStatus = status_occupied;
				rolePlayer.setActivity(activity_onWayToStation);
				rolePlayer.setCurrentGoal(station);

				if(verbose)
					System.out.println(rolePlayer.getTime() + "\t" + rolePlayer + "return to station");

				return time + 1;
			}
				
		}

		// time to go back after shift is over
		else if(time + 60 > shiftEndTime 
				&& myActivity != activity_onWayToStation && myActivity != activity_onWayToTasking 
				&& rolePlayer.getBase().distance(rolePlayer.geometry.getCoordinate()) > EmergentCrime.resolution){
			// if it's time to go back to the station: set activity, set destination, and 
			// schedule self to go back
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
	
	
	public double executePersonalTasking(){
		System.out.println("job job job");
		return 1;
	}
	
	public int getStatus(){ return myStatus; }
}