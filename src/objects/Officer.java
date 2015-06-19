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
	
	
	int taskTime = -1;
	int ticksSpentTraveling = -1;
//	int timeOnDuty = -1;
	int shiftEndTime= -1;
	int goalMealBreakTime=-1;
	
	int lastTimeTraveled = -61;
	String lastEdgeTravelled = "";
	public String positionRecord = "";
	
	public static double defaultSpeed = 200;//270;//535;
	public static double topSpeed = 1000;//2000;
	
	int myShift = 0;
	int laggedStatus = OfficerRole.status_offDuty;

	public Officer(String id, Coordinate homeStation, Coordinate mainStation, EmergentCrime world, double speed, String taskingType) {
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
		minSpeed = 50;//135; // ~ 5mph
		speed = defaultSpeed; // ~ 20mph
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


	public double externalStep(SimState state){
		
		double nextActivation = role.activate(state.schedule.getTime());//chooseActivityForTick();
		int currentStatus = ((OfficerRole)role).getStatus();
		if(myStatus != currentStatus)
			updateStatus(currentStatus);
		return nextActivation;
	}
	
	public void step(SimState state){
		
		double nextActivation = role.activate(state.schedule.getTime());//chooseActivityForTick();
		int currentStatus = ((OfficerRole)role).getStatus();
		if(myStatus != currentStatus)
			updateStatus(currentStatus);
		state.schedule.scheduleOnce(nextActivation, this);

		if(laggedStatus != myStatus){
			world.statusChanges.add(((int)state.schedule.getTime()) + "\t" + myStatus  + "\t" + this.myID  + "\t" + this.geometry.getCoordinate().toString() + "\n");
		}
		laggedStatus = myStatus;
		
//		timeOnDuty++;
		/*
		if(myStatus == status_offduty){
			updateStatus(status_available);
			System.out.println(world.schedule.getTime() + ": " + this.toString() + " CAME ON DUTY");
		}
		
		// check to see if the Officer is involved in some tasking
		if(path == null && hasTasking == true && taskTime > 0){
			
			taskTime--; // if so, spend this step on the task
			
			if(taskTime == 0){ // finished after this step! Reset
				updateStatus(status_available);
				hasTasking = false;
				taskTime = -1;
				world.recordTravelTime(ticksSpentTraveling);
				System.out.println(world.schedule.getTime() + ": " + this.toString() + " FINISHED at " + this.geometry.toString());
			}
			
			world.schedule.scheduleOnce(this, EmergentCrime.ordering_officers);
			return;
		}
		
		/*
		Bag crimes = world.crimeLayer.getObjectsWithinDistance(this, crimeDetectionRadius);
		if(crimes.size() > 0){
			CrimeEvent ce = (CrimeEvent) crimes.get(0);
			Agent a = ce.getOffenders().iterator().next();
			int success = detain(a);
		}
		*/
		
/*		// done with a shift? Go off duty!
		if(timeOnDuty > 96){
			
			// already at the station? Just go off duty!
			if(geometry.getCoordinate().distance(work) < EmergentCrime.resolution){
				updateStatus(status_offduty);
				int time = this.getTime(4 + 8 * myShift, 0);
				world.schedule.scheduleOnce(time, EmergentCrime.ordering_officers, this);
				System.out.println(world.schedule.getTime() + ": " + this.toString() + " WENT OFF DUTY");
				timeOnDuty = 0;
				return;
			}
			
			// otherwise head back to the station
			else {
				//updateStatus(status_occupied);
				headFor(work, familiarRoadNetwork);
			}
		}
		
		// if the Officer has a place to go, go to it!
		if(path != null){
			navigate(world.resolution);
			if(hasTasking == true)
				ticksSpentTraveling++;
		}

		// if no such course exists, pick a random new point and move to it
		else {
			GeoNode gn = (GeoNode) world.roadNodes.get(world.random.nextInt(world.roadNodes.size()));
			headFor(gn.geometry.getCoordinate(), familiarRoadNetwork);
		}

		world.schedule.scheduleOnce(this, EmergentCrime.ordering_officers);
*/	}
	
	public int getStatus(){ return myStatus; }
	public OfficerRole getRole(){ return this.role;}
	public void setRole(OfficerRole newRole){ this.role = newRole; }
	
	public void enterVehicle(Vehicle v){
		v.acquireOfficer(this);
		currentVehicle = v;
	}
	
	public void leaveVehicle(){
		if(currentVehicle == null){
			System.out.println("ERROR: NOT IN VEHICLE");
			return;
		}
		currentVehicle.loseOfficer(this);
		currentVehicle = null;
	}

	public boolean inVehicle(){ return currentVehicle != null; }
}