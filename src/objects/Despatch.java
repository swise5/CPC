package objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import objects.roles.OfficerRole;
import objects.roles.ResponseCarRole;
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
	ArrayList <Coordinate> requestsForTransport = new ArrayList <Coordinate> ();

	public static int param_responseCarTimeCommitment = 60;
	
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
			boolean tasked = false;
			
			Bag nearbyOfficers = new Bag();
			double searchDistance = 100;
			while(nearbyOfficers.size() < officers.size() && !tasked){
				
				nearbyOfficers = world.officerLayer.getObjectsWithinDistance(event.getLocation(), searchDistance);

				for(Object obj: nearbyOfficers){
					
					Officer o = (Officer) obj;
					if(o.getStatus() != OfficerRole.status_available) 
						continue;
//					if(o.getSpeed() < 1000 && searchDistance > 1000) // past a certain distance, only take cars
//						continue;
					OfficerRole tasking = o.getRole();
					if(tasking instanceof ResponseCarRole){
						((ResponseCarRole)tasking).redirectToResponse(event.getLocation().getCoordinate(), param_responseCarTimeCommitment);//3 + state.random.nextInt(22));
					}
					tasked = true;
					break;
					
				}
				
				if(tasked)
					CAD.remove(event);
				else
					searchDistance *= 2;
			}
			
			if(!tasked)
				possiblyAvailable = false;

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
					if(o.getStatus() != OfficerRole.status_available) 
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
}