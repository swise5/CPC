package objects;

import java.util.ArrayList;
import java.util.HashMap;

import objects.roles.OfficerRole;
import objects.roles.ReportCarRole;
import objects.roles.ResponseCarRole;
import objects.roles.TransportVanRole;
import sim.EmergentCrime;
import sim.engine.SimState;
import sim.field.network.Edge;
import sim.util.geo.MasonGeometry;
import swise.objects.network.GeoNode;

import com.vividsolutions.jts.geom.Coordinate;

public class Officer extends Agent {

	int myStatus = 0;
	OfficerRole role = null;

	int [] timeOnStatus = new int[13];
	HashMap <String, Double> timeOnStatusPerRoad = new HashMap <String, Double> ();
	double lastStatusChange = 0;
	
	int taskTime = -1;
	int ticksSpentTraveling = -1;
	int shiftEndTime= -1;
	int goalMealBreakTime=-1;
	
	int lastTimeTraveled = -61;
	Edge lastEdgeTravelled = null;
	public String positionRecord = "";
	
	int laggedStatus = OfficerRole.status_offDuty;

	public Officer(String id, Coordinate homeStation, Coordinate mainStation, EmergentCrime world, double speed, String taskingType) {
		super(id, homeStation, mainStation, world, world.officerLayer, world.roads);
		
		this.addIntegerAttribute("status", myStatus);
		this.speed = speed;
	
		this.base = (Coordinate)homeStation.clone();
		this.work = (Coordinate)mainStation.clone();

		world.schedule.scheduleOnce(world.schedule.getTime() + 1, //this.getTime(4 + 8 * myShift, 0), 
				EmergentCrime.ordering_officers, this);
		
		path = null;
		
		int distanceToPoints = 2000;
		
		int myTaskingType = 0;
		if(taskingType.equals("patrol")){
			role = new ResponseCarRole(this, 
					world.networkLayer.getGeometries(),//world.networkLayer.getObjectsWithinDistance(world.fa.createPoint(homeStation), distanceToPoints), 
					world.random, world.despatch);
			myTaskingType = 1;
		}
		else if(taskingType.equals("report")){
			role = new ReportCarRole(this, 
					world.networkLayer.getGeometries(),//world.networkLayer.getObjectsWithinDistance(world.fa.createPoint(homeStation), distanceToPoints), 
					world);
			myTaskingType = 2;
		}
		else if(taskingType.equals("transport")){
			role = new TransportVanRole(this, world.random, world);
			myTaskingType = 3;
		}
		else
			System.out.println("ERROR: failed tasking!!!");
		
		this.addIntegerAttribute("taskingType", myTaskingType);
		minSpeed = 0;
		speed = world.param_defaultSpeed;
	}
	
	
	public int navigate(double resolution){
		
		if(world.verbose){
			if(world.schedule.getTime() - (10 / world.temporalResolution_minutesPerTick) > lastTimeTraveled){
				if(positionRecord.length() > 0)
					positionRecord += "\t</coordinates>\n</LineString>\n</Placemark>\n";
				positionRecord += "<Placemark><name>" + myID + 
						"</name>\n<LineString>\n<extrude>1</extrude>\n<tessellate>1</tessellate>\n<altitudeMode>absolute</altitudeMode>\n<coordinates>";

			}
			else {
				Coordinate mypos = geometry.getCoordinate();
				positionRecord += mypos.x + "," + mypos.y + ",1\n";
			}
		}

		lastTimeTraveled = (int)world.schedule.getTime();
		
//		world.incrementHeatmap(this.geometry);
		myLastSpeed = -1;
		
		if(path == null)
			return -1;
		

		double time = 1;//speed;
		while(path != null && time > 0){
			time = move(time, speed, resolution);
			if(world.verbose && edge != null && !edge.equals(lastEdgeTravelled)){
				world.updateEdgeHeatmap(edge);
				// TODO consider updating here!!!!
			}
			if(world.verbose)
				world.incrementHeatmap(this.geometry);
		}

		if(edge != null)
			lastEdgeTravelled = edge;
		
		if(segment != null)
			updateLoc(segment.extractPoint(currentIndex));				

		if(time < 0){
			return -1;
		}
		else
			return 1;
	}

	// HANDLING STATUS INFO
	
	public void updateStatusTimes(){ 
		double timeOnThisStatus = world.schedule.getTime() - lastStatusChange;
		timeOnStatus[myStatus] += timeOnThisStatus;
		
		String thisUnitKey = ((MasonGeometry)edge.getInfo()).getStringAttribute("FID_1") + "_" + myStatus;
		Double prevTimeOnThisRoad = timeOnStatusPerRoad.get(thisUnitKey);
		if(prevTimeOnThisRoad == null)
			prevTimeOnThisRoad = 0.;
		timeOnStatusPerRoad.put(thisUnitKey, prevTimeOnThisRoad + timeOnThisStatus);
			
		lastStatusChange = world.schedule.getTime();
	}
	public int [] getStatusTimes(){ return timeOnStatus; }
	public HashMap <String, Double> getStatusRoadTimes(){ return timeOnStatusPerRoad; }
	public void resetStatusTimes(){ 
		timeOnStatus = new int [13];
		timeOnStatusPerRoad = new HashMap <String, Double> ();
	}
	
	public void updateStatus(int newStatus){
		//updateStatusTimes();
		myStatus = newStatus;
		this.addIntegerAttribute("status", newStatus);
	}

	// STEPPERS

	public void step(SimState state){
		
		if(world.verbose)
			world.statusChanges.add(((int)state.schedule.getTime()) + "\t" + myStatus  + "\t" + this.myID  + "\t" + 
				this.role.getClass() + "\t" + this.geometry.getCoordinate().toString() + "\t" + ((MasonGeometry)this.edge.getInfo()).getStringAttribute("FID_1") + "\n");

		updateStatusTimes();
		
		double nextActivation = role.activate(state.schedule.getTime());//chooseActivityForTick();
		if(nextActivation <= state.schedule.getTime()){
			nextActivation = state.schedule.getTime() + 1;
			System.out.println(this.getActivity() + "\t" + this.arrivedAtGoal());
		}
		int currentStatus = ((OfficerRole)role).getStatus();
		
		updateStatus(currentStatus);

		state.schedule.scheduleOnce(nextActivation, this);

/*		if(laggedStatus != myStatus){
			world.statusChanges.add(((int)state.schedule.getTime()) + "\t" + myStatus  + "\t" + this.myID  + "\t" + 
					this.role.getClass() + "\t" + this.geometry.getCoordinate().toString() + "\t" + ((MasonGeometry)this.edge.getInfo()).getStringAttribute("FID_1") + "\n");
		}
	*/	laggedStatus = myStatus;
		
}
	
	public int getStatus(){ return myStatus; }
	public OfficerRole getRole(){ return this.role;}
	public void setRole(OfficerRole newRole){ this.role = newRole; }

}