package objects;

import java.util.ArrayList;

import objects.roles.OfficerRole;
import objects.roles.ReportCarRole;
import objects.roles.ResponseCarRole;
import objects.roles.TransportVanRole;
import sim.EmergentCrime;
import sim.engine.SimState;
import sim.util.geo.MasonGeometry;
import swise.objects.network.GeoNode;

import com.vividsolutions.jts.geom.Coordinate;

public class Officer extends Agent {

	int myStatus = 0;
	OfficerRole role = null;

	Vehicle currentVehicle = null;
	
	int [] timeOnStatus = new int[13];
	double lastStatusChange = 0;
	
	int taskTime = -1;
	int ticksSpentTraveling = -1;
//	int timeOnDuty = -1;
	int shiftEndTime= -1;
	int goalMealBreakTime=-1;
	
	int lastTimeTraveled = -61;
	String lastEdgeTravelled = "";
	public String positionRecord = "";
	
	public static double defaultSpeed = 200 * EmergentCrime.temporalResolution_minutesPerTick;//270;//535;
	public static double topSpeed = 1000 * EmergentCrime.temporalResolution_minutesPerTick;//2000;
	
	int laggedStatus = OfficerRole.status_offDuty;

	public Officer(String id, Coordinate homeStation, Coordinate mainStation, EmergentCrime world, double speed, String taskingType) {
		super(id, homeStation, mainStation, world, world.officerLayer, world.roads);
		
		this.addIntegerAttribute("status", myStatus);
		this.speed = speed;
		defaultSpeed = speed;
//		this.myShift = world.random.nextInt(2);
	
		this.base = homeStation;//((GeoNode) world.roadNodes.get(world.random.nextInt(world.roadNodes.size()))).geometry.getCoordinate();
		this.work = mainStation;//(Coordinate)work.clone();

		world.schedule.scheduleOnce(world.schedule.getTime() + 1, //this.getTime(4 + 8 * myShift, 0), 
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
					world.networkLayer.getObjectsWithinDistance(world.fa.createPoint(homeStation), distanceToPoints), world);
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
			if(edge != null && !edge.toString().equals(lastEdgeTravelled)){ // TODO make this less bad
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

	// HANDLING STATUS INFO
	
	public void updateStatusTimes(){ 
		timeOnStatus[myStatus] += world.schedule.getTime() - lastStatusChange;
		lastStatusChange = world.schedule.getTime();
	}
	public int [] getStatusTimes(){ return timeOnStatus; }
	public void resetStatusTimes(){ timeOnStatus = new int [13]; }
	
	public void updateStatus(int newStatus){
		updateStatusTimes();
		myStatus = newStatus;
		this.addIntegerAttribute("status", newStatus);
	}

	// STEPPERS

	public double externalStep(SimState state){
		
		double nextActivation = role.activate(state.schedule.getTime());//chooseActivityForTick();
		int currentStatus = ((OfficerRole)role).getStatus();
		if(myStatus != currentStatus)
			updateStatus(currentStatus);
		return nextActivation;
	}
	
	public void step(SimState state){
		
		world.statusChanges.add(((int)state.schedule.getTime()) + "\t" + myStatus  + "\t" + this.myID  + "\t" + 
				this.role.getClass() + "\t" + this.geometry.getCoordinate().toString() + "\t" + ((MasonGeometry)this.edge.getInfo()).getStringAttribute("FID_1") + "\n");

		double nextActivation = role.activate(state.schedule.getTime());//chooseActivityForTick();
		if(nextActivation <= state.schedule.getTime()){
			nextActivation = state.schedule.getTime() + 1;
			System.out.println(this.getActivity() + "\t" + this.arrivedAtGoal());
		}
		int currentStatus = ((OfficerRole)role).getStatus();
		
/*		// update status information
		if(myStatus != currentStatus){
			//updateStatusTimes();
			updateStatus(currentStatus);
		}
	*/
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