package objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import objects.roles.OfficerRole;
import objects.roles.ResponseCarRole;
import objects.roles.ReportCarRole;
import objects.roles.TransportVanRole;
import sim.EmergentCrime;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;

/**
 * The Despatch object provides the interface between the CAD system and the Officer agents, filtering and 
 * forwarding information as appropriate.
 *  
 * @author swise
 *
 */
public class Despatch implements Steppable {

	private static final long serialVersionUID = 1L;
	
	EmergentCrime world;
	TreeSet <CallEvent> CAD;
	ArrayList <Officer> officers = new ArrayList <Officer> ();

	HashMap <Coordinate, Long> requestTickets = new HashMap <Coordinate, Long> ();
	HashMap <Integer, ArrayList <Officer>> despatchedResponseCars = new HashMap <Integer, ArrayList <Officer>> ();
	HashMap <Integer, CallEvent> officerArrivedYet = new HashMap <Integer, CallEvent> ();

	ArrayList <Coordinate> requestsForTransport = new ArrayList <Coordinate> ();
	
	ArrayList <Integer> eCallsTime = new ArrayList <Integer> (),
			sCallsTime = new ArrayList <Integer> (),
			iCallsTime = new ArrayList <Integer> ();
	
	public Despatch(TreeSet <CallEvent> CAD, ArrayList <Officer> officers, EmergentCrime world){
		super();
		this.CAD = CAD;
		this.officers = officers;
		this.world = world;
	}
	
	@Override
	public void step(SimState state) {
		
		boolean possiblyAvailable = true;

		while(CAD.size() > 0 && possiblyAvailable && CAD.first().getTime() <= world.schedule.getTime()){
			
			CallEvent event = CAD.first();
			
			int grade = event.getGrade();
			if(grade == 0) // TODO get rid of
				System.out.print("");
						
/*			// make sure we're not trying to assign officers to an incident that has yet to happen!
			double eventTime = event.getTime();
			double worldTime = world.schedule.getTime();
			if(event.getTime() > world.schedule.getTime()){
				System.out.println("ERROR: Despatch called to assign an event too soon");
				possiblyAvailable = false;
				continue;
			}
	*/		
			boolean tasked = false;
			int incidentID = event.getIncidentNumber();
			ArrayList <Officer> assignedResponseCars = new ArrayList <Officer> ();
			if(despatchedResponseCars.containsKey(incidentID)){
				assignedResponseCars = despatchedResponseCars.get(incidentID);
			}
			
			Bag nearbyOfficers = new Bag();
			double searchDistance = 100;
			while(nearbyOfficers.size() < officers.size() && !tasked){
				
				nearbyOfficers = world.officerLayer.getObjectsWithinDistance(event.getLocation(), searchDistance);

				for(Object obj: nearbyOfficers){
					
					Officer o = (Officer) obj;
					int oStatus = o.getStatus();
					
					// if the Officer is not in an available state, just continue
					if( !(oStatus == OfficerRole.status_available_resumePatrol || oStatus == OfficerRole.status_committedButDeployable 
							|| oStatus == OfficerRole.status_available_office || oStatus == OfficerRole.status_refs) )
						continue;

					// determine based on officer role whether they are suitable for this call event
					OfficerRole tasking = o.getRole();
					if((tasking instanceof ResponseCarRole && grade < 2) || world.rolesDisabled){
						tasking.redirectToResponse(event.getLocation().getCoordinate(), world.param_responseCarTimeCommitment, incidentID, grade);
						assignedResponseCars.add(o);
						tasked = true;
						break;
					}
					else if(tasking instanceof ReportCarRole){
						tasking.redirectToResponse(event.getLocation().getCoordinate(), world.param_responseCarTimeCommitment, incidentID, grade);
						assignedResponseCars.add(o);
						tasked = true;
						break;						
					}
				}
				
				if(tasked){	// have found an officer
					CAD.remove(event);
					if(! officerArrivedYet.containsKey(incidentID))
						officerArrivedYet.put(incidentID, event);
					despatchedResponseCars.put(incidentID, assignedResponseCars);
				}
				else // search further!!
					searchDistance *= 2;
			}
			
			if(!tasked)
				possiblyAvailable = false; // no one else is free to assign, let it go

		}

		// despatch any necessary transport vans
		possiblyAvailable = true;
		while(requestsForTransport.size() > 0 && possiblyAvailable){
			
			Coordinate location = requestsForTransport.get(0);
			
			// make sure this record actually exists
			if(location == null || requestTickets.get(location) == null){
				requestsForTransport.remove(0);
				continue;
			}
			Geometry g = world.fa.createPoint(location);
			
			boolean tasked = false;
			
			Bag nearbyOfficers = new Bag();
			double searchDistance = 100;
			while(nearbyOfficers.size() < officers.size() && !tasked){
				
				nearbyOfficers = world.officerLayer.getObjectsWithinDistance(g, searchDistance);

				boolean finished = false;
				for(Object obj: nearbyOfficers){
					
					if(finished) continue;
					Officer o = (Officer) obj;
					int oStatus = o.getStatus();
					
					// if the Officer is not in an available state, continue searching
					if( !(oStatus == OfficerRole.status_available_resumePatrol || oStatus == OfficerRole.status_committedButDeployable 
							|| oStatus == OfficerRole.status_available_office || oStatus == OfficerRole.status_refs) )
						continue;
					OfficerRole tasking = o.getRole();
					if(tasking == null){
						System.out.println("ERROR: officer has not been assigned a tasking");
						continue;
					}
					if(tasking instanceof TransportVanRole){
						Object myObject = requestTickets.get(location);
						if(myObject == null)
							System.out.println("ERROR: no open requests for service at this location");
						((TransportVanRole)tasking).redirectToResponse(location, requestTickets.get(location));
						tasked = true;
						finished = true;
						break;
					}
					
				}
				
				if(tasked){
					requestsForTransport.remove(0);
				}
				else
					searchDistance *= 2;
			}
			
			if(!tasked)
				possiblyAvailable = false;

		}
	}

	public void setCAD(TreeSet <CallEvent> CAD){
		this.CAD = CAD;
	}
	
	public long receiveRequestForTransport(Coordinate location){
		long ticket = world.random.nextLong();
		if(requestTickets.containsKey(location))
			ticket = requestTickets.get(location);
		else
			requestTickets.put(location, ticket);
		
		requestsForTransport.add(location);
		return ticket;
	}
	
	/**
	 * 
	 * @param incidentID
	 * @param o - the responding officer who has found the incident does not warrant multiple officers
	 */
	public void receiveReportOfDowngradeInSeverity(int incidentID, Officer o){
		ArrayList <Officer> assigned = despatchedResponseCars.get(incidentID);
		for(Officer offs: assigned){
			if(offs == o) continue; // don't update the guy who called
			if(offs.arrivedAtGoal()){ // anyone who's already there can hang out until they need to be despatched again
				offs.updateStatus(OfficerRole.status_committedButDeployable);
				offs.setActivity(OfficerRole.activity_dealingWithTasking);
			}
			else {
				offs.updateStatus(OfficerRole.status_available_resumePatrol);
				offs.setActivity(OfficerRole.activity_patrolling);
			}
		}
	}
	
	
	public void recordResponseTime(int incidentID){
		
		// make sure that we don't record the times of the second, third, etc. officer to respond
		if(!officerArrivedYet.containsKey(incidentID)){
			return;			
		}
		
		// calculate the time between now and the call for service and add to the appropriate pile
		CallEvent event = officerArrivedYet.remove(incidentID);
		int time = (int)(world.schedule.getTime() - event.getTime());
		
		// add this value to the appropriate list
		int grade = event.getGrade();
		
		if(grade == 0 && time > 10)
			System.out.println("why");
		
		if(grade == 0) iCallsTime.add(time);
		else if(grade == 1) sCallsTime.add(time);
		else if(grade == 2) eCallsTime.add(time);
		else System.out.println("ERROR: unhandled call grade in Despatch's recordResponseTime function");
	}

	public double[] getAverageResponseTimes() {
		double [] responseTimes = new double [3];
		double result = 0;
		for(int i: iCallsTime){
			result += i;
		}
		responseTimes[0] = result / Math.max(1, iCallsTime.size());
		
		result = 0;
		for(int i: sCallsTime){
			result += i;
		}
		responseTimes[1] = result / Math.max(1, sCallsTime.size());
		
		result = 0;
		for(int i: eCallsTime){
			result += i;
		}
		responseTimes[2] = result / Math.max(1, eCallsTime.size());
		
		return responseTimes;
	}

}