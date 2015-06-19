package objects;

import java.util.ArrayList;

import objects.roles.OfficerRole;
import objects.roles.ReportCarRole;
import objects.roles.ResponseCarRole;
import objects.roles.TransportVanRole;
import sim.EmergentCrime;
import sim.engine.SimState;
import swise.objects.network.GeoNode;

import com.vividsolutions.jts.geom.Coordinate;

public class Officer extends Agent {

	Officer commander;
	ArrayList <Officer> subordinates;
	
	int crimeDetectionRadius = 100;
	
	int myStatus = 0;
	OfficerRole role = null;

	Vehicle currentVehicle = null;
	boolean inVehicle = false;
	
	
	int taskTime = -1;
	int ticksSpentTraveling = -1;
//	int timeOnDuty = -1;
	int shiftEndTime= -1;
	int goalMealBreakTime=-1;
	
	int lastTimeTraveled = -61;
	String lastEdgeTravelled = "";
	public String positionRecord = "";
	
	public static double minSpeed = 50;
	public static double defaultSpeed = 200;//270;//535;
	public static double topSpeed = 1000;//2000;
	
	int myShift = 0;
	

	public Officer(String id, Coordinate homeStation, Coordinate mainStation, EmergentCrime world, double speed, String taskingType, Vehicle vehicle) {
		super(id, homeStation, mainStation, world, world.officerLayer, world.roads);
		
		this.addIntegerAttribute("status", myStatus);
		this.speed = speed;
		defaultSpeed = speed;
//		this.myShift = world.random.nextInt(2);
	
		this.base = homeStation;//((GeoNode) world.roadNodes.get(world.random.nextInt(world.roadNodes.size()))).geometry.getCoordinate();
		this.work = mainStation;//(Coordinate)work.clone();

		world.schedule.scheduleOnce(60 * 12 * myShift, //this.getTime(4 + 8 * myShift, 0), 
				EmergentCrime.ordering_officers, this);
		
		path = null;
		
		int distanceToPoints = 2000;//10000;
		
		int myTaskingType = 0;
		if(taskingType.equals("patrol")){
			role = new ResponseCarRole(this, 
					world.networkLayer.getObjectsWithinDistance(world.fa.createPoint(homeStation), distanceToPoints), world.random, world.despatch);
			myTaskingType = 1;
		}
		else if(taskingType.equals("report")){
			role = new ReportCarRole(this, 
					world.networkLayer.getObjectsWithinDistance(world.fa.createPoint(homeStation), distanceToPoints), world.random, world);
			myTaskingType = 2;
		}
		else if(taskingType.equals("transport")){
			role = new TransportVanRole(this, world.random, world);
			myTaskingType = 3;
		}
		else
			System.out.println("ERROR: failed tasking!!!");
		
		this.addIntegerAttribute("taskingType", myTaskingType);
		speed = defaultSpeed; // ~ 20mph
		currentVehicle = vehicle;
//		tasking = new OfficerRole(this);
	}
	
	
	public int navigate(double resolution){
		
		if(world.schedule.getTime() - 10 > lastTimeTraveled){
			if(positionRecord.length() > 0)
				positionRecord += "\t</coordinates>\n</LineString>\n</Placemark>\n";
			positionRecord += "<Placemark><name>" + myID + 
					"</name>\n<LineString>\n<extrude>1</extrude>\n<tessellate>1</tessellate>\n<altitudeMode>absolute</altitudeMode>\n<coordinates>";

		}
		else {
			Coordinate mypos = geometry.getCoordinate();
			positionRecord += mypos.x + "," + mypos.y + ",1\n";
		}

		lastTimeTraveled = (int)world.schedule.getTime();
		
//		world.incrementHeatmap(this.geometry);
		myLastSpeed = -1;
		
		if(path == null)
			return -1;
		

		double time = 1;//speed;
		while(path != null && time > 0){
			time = move(time, speed, resolution);
			if(edge != null && !edge.toString().equals(lastEdgeTravelled)){
				world.updateEdgeHeatmap(edge);

			}
			world.incrementHeatmap(this.geometry);
		}

		if(edge != null)
			lastEdgeTravelled = edge.toString();
		
		if(segment != null)
			updateLoc(segment.extractPoint(currentIndex));				

		if(time < 0){
			return -1;
		}
		else
			return 1;
	}

	public void updateStatus(int newStatus){
		myStatus = newStatus;
		this.addIntegerAttribute("status", newStatus);
	}


	public void step(SimState state){
		
		double nextActivation = role.activate(state.schedule.getTime());//chooseActivityForTick();
		
		if(inVehicle) currentVehicle.updateAllOfficerPositions();
		
		int currentStatus = ((OfficerRole)role).getStatus();
		if(myStatus != currentStatus){
			updateStatus(currentStatus);
			if(inVehicle) currentVehicle.updateAllOfficersStatus(currentStatus);
		}
		
		state.schedule.scheduleOnce(nextActivation, this);
	}
	
	public int getStatus(){ return myStatus; }
	public OfficerRole getRole(){ return this.role;}
	public void setRole(OfficerRole newRole){ this.role = newRole; }
	public boolean inVehicle(){ return currentVehicle != null; }

	public void movedTo(Coordinate c){
		updateLoc(c);
	}

	///////////// VEHICLE RELATED TOOLS /////////////
	
	public void assignedToNewVehicle(Vehicle v){
		if(currentVehicle != null && currentVehicle.containsOfficer(this))
			currentVehicle.loseOfficer(this);
		currentVehicle = v;
	}
	
	public void enterVehicle(){
		currentVehicle.acquireOfficer(this);
		inVehicle = true;
	}
	
	
	public void leaveVehicle(){
		if(currentVehicle == null){
			System.out.println("ERROR: NO VEHICLE");
			return;
		}
		else if(!inVehicle){
			System.out.println("ERROR: NOT IN VEHICLE");
			return;
		}
		currentVehicle.loseOfficer(this);
		inVehicle = false;
	}
	
	///////////// END VEHICLE RELATED TOOLS /////////////

}