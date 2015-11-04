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
import sim.EmergentCrime.CallEvent;
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
	ArrayList <Coordinate> requestsForTransport = new ArrayList <Coordinate> ();
	
	public Despatch(TreeSet <CallEvent> CAD, ArrayList <Officer> officers, EmergentCrime world){
		super();
		this.CAD = CAD;
		this.officers = officers;
		this.world = world;
	}
	
	@Override
	public void step(SimState state) {
		
		boolean possiblyAvailable = true;

		while(CAD.size() > 0 && possiblyAvailable){
			
			CallEvent event = CAD.first();
//			int numOfficersRequested = 3 - event.getGrade(); // 3 for I, 2 for S, 1 for E
			int tasked = 0;
			int incidentID = event.getIncidentNumber();
			ArrayList <Officer> assignedResponseCars = new ArrayList <Officer> ();
			if(despatchedResponseCars.containsKey(incidentID)){
				assignedResponseCars = despatchedResponseCars.get(incidentID);
			}
			
			Bag nearbyOfficers = new Bag();
			double searchDistance = 100;
			while(nearbyOfficers.size() < officers.size() && tasked < 1){//numOfficersRequested){
				
				nearbyOfficers = world.officerLayer.getObjectsWithinDistance(event.getLocation(), searchDistance);

				for(Object obj: nearbyOfficers){
					
					Officer o = (Officer) obj;
					int oStatus = o.getStatus();
					
					// if the Officer is not in an available state, just continue
					if( !(oStatus == OfficerRole.status_available_resumePatrol || oStatus == OfficerRole.status_committedButDeployable 
							|| oStatus == OfficerRole.status_available_office || oStatus == OfficerRole.status_refs) )
						continue;
//					if(o.getSpeed() < 1000 && searchDistance > 1000) // past a certain distance, only take cars
//						continue;
					OfficerRole tasking = o.getRole();
					if(tasking instanceof ResponseCarRole || world.rolesDisabled){
						tasking.redirectToResponse(event.getLocation().getCoordinate(), world.param_responseCarTimeCommitment, incidentID);
						//3 + state.random.nextInt(22));
						assignedResponseCars.add(o);
						tasked++;
						break;
					}
//					if(tasked >= numOfficersRequested)
//						break;
				}
				
				if(tasked >= 1 ||//numOfficersRequested ||	// have found enough officers
						tasked > 0 && officers.size() == nearbyOfficers.size()){ // only one officer available, will have to do! 
					CAD.remove(event);
					despatchedResponseCars.put(incidentID, assignedResponseCars);
					tasked = 10; // break out of the loop
				}
				else // search further!!
					searchDistance *= 2;
			}
			
			if(tasked < 1)//numOfficersRequested)
				possiblyAvailable = false; // no one else is free to assign, let it go

		}

		// despatch any necessary transport vans
		possiblyAvailable = true;
		while(requestsForTransport.size() > 0 && possiblyAvailable){
			Coordinate location = requestsForTransport.get(0);
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
					
					// if the Officer is not in an available state, just continue
					if( !(oStatus == OfficerRole.status_available_resumePatrol || oStatus == OfficerRole.status_committedButDeployable 
							|| oStatus == OfficerRole.status_available_office || oStatus == OfficerRole.status_refs) )
						continue;
					OfficerRole tasking = o.getRole();
					if(tasking == null){
						System.out.println("LOLWUT");
						continue;
					}
					if(tasking instanceof TransportVanRole){
						Object myObject = requestTickets.get(location);
						if(myObject == null)
							System.out.println("why");
						((TransportVanRole)tasking).redirectToResponse(location, requestTickets.get(location));
						tasked = true;
						finished = true;
						break;
					}
					
				}
				
				if(tasked){
					requestsForTransport.remove(0);
//					requestTickets.remove(location);
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
}