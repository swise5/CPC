package sim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TreeSet;

import objects.Agent;
import objects.Despatch;
import objects.FirstContact;
import objects.Officer;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.geo.GeomGridField;
import sim.field.geo.GeomVectorField;
import sim.field.grid.IntGrid2D;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.util.Bag;
import sim.util.geo.AttributeValue;
import sim.util.geo.MasonGeometry;
import swise.objects.AStar;
import swise.objects.InOutUtilities;
import swise.objects.NetworkUtilities;
import swise.objects.network.GeoNode;
import swise.objects.network.ListEdge;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LengthIndexedLine;

import ec.util.MersenneTwisterFast;

public class EmergentCrime extends SimState {
	/////////////// Model Parameters ///////////////////////////////////
	
	private static final long serialVersionUID = 1L;
	public int grid_width = 700;
	public int grid_height = 700;
	public static double resolution = 1;// the granularity of the simulation 
				// (fiddle around with this to merge nodes into one another)

	double speed = -1;
	
	boolean normaliseOutput = true;

	public static int ordering_officers = 100, ordering_civilians = 50, ordering_despatch = 25, ordering_firstContact = 1;
	
	public double param_reportProb = .25;
	public double param_transportRequestProb = .25;
	public double param_redeployProb = .75;
	public int param_reportTimeCommitment = 60;

	
	/////////////// Data Sources ///////////////////////////////////////
	
	///// TEMPORARY ///////////////////////////
	String dirName = "/Users/swise/workspace/CPC/data/";
	
	//// END TEMPORARY////////////////////////
	
	public GeomVectorField baseLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField roadLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField stationLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField networkLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField networkEdgeLayer = new GeomVectorField(grid_width, grid_height);	
	public GeomVectorField majorRoadNodesLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField trafficLayer = new GeomVectorField(grid_width, grid_height);
	
	public GeomVectorField boroughLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField officerLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField vehicleLayer = new GeomVectorField(grid_width, grid_height);
	
	public GeomVectorField crimeLayer = new GeomVectorField(grid_width, grid_height);
	
	public GeomGridField heatmap = new GeomGridField();
	
	public GeomVectorField heatEdges = new GeomVectorField(grid_width, grid_height);
	
	/////////////// Objects //////////////////////////////////////////////

	ArrayList <ArrayList <CrimeEvent>> crimeEventRecord = new ArrayList <ArrayList <CrimeEvent>> ();
	
	ArrayList <Officer> officers = new ArrayList <Officer> ();

	FirstContact firstContact;
	public Despatch despatch;
	//HashMap<MasonGeometry, TreeSet <CallEvent>> CAD = new HashMap<MasonGeometry, TreeSet <CallEvent>> ();
	TreeSet <CallEvent> urgent_CAD = new TreeSet <CallEvent> ();
	TreeSet <CallEvent> extended_CAD = new TreeSet <CallEvent> ();
	
	ArrayList <Integer> travelTimes = new ArrayList <Integer> ();
	
	public Bag roadNodes = new Bag();
	Bag districtNodes = new Bag();
	public Network roads = new Network(false);
	HashMap <MasonGeometry, ArrayList <GeoNode>> localNodes;
	ArrayList <GeoNode> stationNodes;

	public GeometryFactory fa = new GeometryFactory();
	
	long mySeed = 0;
	
	Envelope MBR = null;
	
	boolean verbose = false;
	public int taskingTypeBeingStudied = 1; // 1 = enabled, 0 = disabled, -1 = random stations
	
	/////////////// END Objects //////////////////////////////////////////

	public HashMap <Edge, Integer> edgeHeatmap = new HashMap <Edge, Integer> ();
	
	public ArrayList <String> statusChanges = new ArrayList <String> ();
	
	///////////////////////////////////////////////////////////////////////////
	/////////////////////////// BEGIN functions ///////////////////////////////
	///////////////////////////////////////////////////////////////////////////	
	/**
	 * Default constructor function
	 * @param seed
	 */
	public EmergentCrime(long seed) {
		super(seed);
		random = new MersenneTwisterFast(seed);
		mySeed = seed;
	}


	/**
	 * Read in data and set up the simulation
	 */
	public void start()
    {
		super.start();
		try {
			
			///////////// READING IN DATA ////////////////
		
			InOutUtilities.readInVectorLayer(baseLayer, 
					dirName + "CamdenBorough/CamdenBorough.shp", "census tracts", new Bag());
			InOutUtilities.readInVectorLayer(roadLayer, 
//					dirName + "itnFromToby/camden_itn_buff100.shp", "road network", new Bag());
//					dirName + "ITN_Camden/ITN_Camden.shp", "road network", new Bag());
//					dirName + "londonRoads/camdenRoadsOnly.shp", "road network", new Bag());
					dirName + "itn/camden_itn_buff100pl2.shp", "road network", new Bag());
			readInStationLayer(dirName + "PoliceStationsMoved.csv", "police stations", fa);
			GeomVectorField trafficLights = new GeomVectorField(grid_width, grid_height);
			InOutUtilities.readInVectorLayer(trafficLights, 
					dirName + "TrafficLights/camdenLights.shp", "lights", new Bag());
			
			
			////////////////// CLEANUP ///////////////////

			// standardize the MBRs so that the visualization lines up
			
			MBR = baseLayer.getMBR();
			MBR.init(523800, 531800, 180090, 188090); // diff: 8000, 8000
			baseLayer.setMBR(MBR);

			this.grid_width = baseLayer.fieldWidth;
			this.grid_height = baseLayer.fieldHeight;

			baseLayer.setMBR(MBR);
			
			heatmap = new GeomGridField();
			heatmap.setMBR(MBR);
			heatmap.setGrid(new IntGrid2D((int)(MBR.getWidth() / 20), (int)(MBR.getHeight() / 20), 100));


			// clean up the road network
			System.out.print("Cleaning the road network...");
			
			roads = NetworkUtilities.multipartNetworkCleanup(roadLayer, roadNodes, resolution, fa, random, 0);
			roadNodes = (Bag)roads.getAllNodes().clone();
			
			roadLayer = new GeomVectorField(grid_width, grid_height);
			GeomVectorField tempNodes = new GeomVectorField(grid_width, grid_height);
			for(Object o: roadNodes){
				GeoNode n = (GeoNode) o;
				tempNodes.addGeometry(n);
			}
			
			// associate traffic lights with appropriate road nodes
			HashSet <GeoNode> trafficNodes = new HashSet <GeoNode> ();
			for(Object o: trafficLights.getGeometries()){
				MasonGeometry g = (MasonGeometry) o;
				Bag nearbyNodes = tempNodes.getObjectsWithinDistance(g.geometry, 10);
				if(nearbyNodes.size() > 0){
					GeoNode closestNode = (GeoNode) nearbyNodes.remove(0);
					while(trafficNodes.contains(closestNode) && nearbyNodes.size() > 0)
						closestNode = (GeoNode) nearbyNodes.remove(0);
					if(! trafficNodes.contains(closestNode))
						trafficNodes.add(closestNode);
				}
			}

			// set up the network information in layers so that agents can access it in a convenient way
			for(Object o: roadNodes){
				
				GeoNode n = (GeoNode) o;
				
				// if it's a traffic node, add this as an attribute
				if(trafficNodes.contains(n)){
					n.addIntegerAttribute("delay", 100);
				}
				
				networkLayer.addGeometry(n);

				// convert the ListEdges into meaningful, accessible objects with which agents can interact
				Bag roadLinks = (Bag) roads.getEdgesOut(o).clone();
				for(Object ed: roadLinks){
					ListEdge edge = (ListEdge) ed;
					
					((MasonGeometry)edge.info).addStringAttribute("open", "OPEN");
					((MasonGeometry)edge.info).addIntegerAttribute("weight", 0);
					networkEdgeLayer.addGeometry( (MasonGeometry) edge.info);
					edgeHeatmap.put(edge, 0);
					roadLayer.addGeometry((MasonGeometry) edge.info);
					((MasonGeometry)edge.info).addAttribute("ListEdge", edge);					
				}

			}
			
			// addendum to help with visualising decaying heatmap
			for(Object o: roadLayer.getGeometries()){
				MasonGeometry mg = (MasonGeometry) o;
				MasonGeometry newMg = new MasonGeometry(mg.geometry);
				newMg.addIntegerAttribute("weight", 0);
				heatEdges.addGeometry(newMg);
			}

			roadNodes = (Bag)roads.getAllNodes().clone();

			// standardise MBRs
			roadLayer.setMBR(MBR);
			networkLayer.setMBR(MBR);
			networkEdgeLayer.setMBR(MBR);
			roadLayer.setMBR(MBR);
			crimeLayer.setMBR(MBR);
			stationLayer.setMBR(MBR);
			heatmap.setMBR(MBR);
			trafficLayer.setMBR(MBR);
			heatEdges.setMBR(MBR);
			officerLayer.setMBR(MBR);
			vehicleLayer.setMBR(MBR);


			/////////////////////
			////////////////////
			////////////////////
			/////////////////////
					
			// identify the nodes contained within the borough
			districtNodes = networkLayer.getCoveredObjects((MasonGeometry) baseLayer.getGeometries().get(0));
			
			// identify the nodes assocaited with the police stations
			stationNodes = new ArrayList <GeoNode> ();
			for(Object o: stationLayer.getGeometries()){
				MasonGeometry mg = (MasonGeometry) o;
				GeoNode closestNode = this.getClosestGeoNode(mg.getGeometry().getCoordinate(), 10);//EmergentCrime.resolution);
				stationNodes.add(closestNode);
			}

			System.out.println("done reading in environment");

			////////////////// AGENTS ///////////////////
			//
			// set up the agents in the simulation
			//
			
			// set up the administration
			firstContact = new FirstContact(this);
			firstContact.setUrgentCAD(urgent_CAD);
			firstContact.setExtendedCAD(extended_CAD);
	
			despatch = new Despatch(urgent_CAD, officers, this);
			schedule.scheduleRepeating(despatch, EmergentCrime.ordering_despatch, 1);
			
			if(taskingTypeBeingStudied == 1)
				readInCADData(dirName + "CAD/cadMarch2011.txt", "CAD data"); // MUST go after initialization of roads!!!!

			// depending on tasking being tested, set up agents accordingly
			
			if(taskingTypeBeingStudied == 0)
				testAgentsALLPATROL(stationNodes);
			
			else if(taskingTypeBeingStudied == 1)
				testAgents(stationNodes);

			else if(taskingTypeBeingStudied == -1)
				testAgentsCOMPLETELYRANDOM(stationNodes);

			else
				System.out.println("ERROR: no tasking option selected");
			
			seedRandom(System.currentTimeMillis());
//			setupReporter();

			/*// decaying heatmap for display purposes
	        schedule.scheduleRepeating(Schedule.EPOCH,1, new Steppable()
            {
            public void step(SimState state) { 
            	((IntGrid2D)heatmap.getGrid()).add(1); 
            	((IntGrid2D)heatmap.getGrid()).upperBound(100);
            	for(Object o: heatEdges.getGeometries()){
            		int d = ((MasonGeometry)o).getIntegerAttribute("weight");
            		if(d <= 10) d--;
            		else
            			d =(int) (d * .95);
            		((MasonGeometry)o).addIntegerAttribute("weight",d);
            	}
            }
            }, 1);
			 */
		} catch (Exception e) { e.printStackTrace();}
		
		
    }

	/**
	 * Officers assigned in approximately correct numbers and taskings per station
	 * 
	 * @param stationNodes
	 */
	void testAgents(ArrayList <GeoNode> stationNodes){

		// ALBANY = 0
		// HAMPSTEAD = 1
		// HOLBORN = 2
		// KENTISH TOWN = 3
		// WEST HAMPSTEAD = 4
 
		int [] patrolStationList = {3, 3, 3, 3, 3, 4, 4, 4, 2, 2, 1};
		for(int i: patrolStationList){
			GeoNode myNode = stationNodes.get(i);
			Coordinate c = (Coordinate) myNode.geometry.getCoordinate().clone();
			Officer off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
			officers.add(off);				
		}
		
		int [] reportStationList = {3, 3, 4, 0, 2, 2};
		for(int i: reportStationList){
			GeoNode myNode = stationNodes.get(i);
			Coordinate c = (Coordinate) myNode.geometry.getCoordinate().clone();
			Officer off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "report");
			officers.add(off);				
		}

		int [] transportStationList = {3, 2};
		for(int i: transportStationList){
			GeoNode myNode = stationNodes.get(i);
			Coordinate c = (Coordinate) myNode.geometry.getCoordinate().clone();
			Officer off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "report");
			officers.add(off);				
		}

/**		GeoNode myNode = stationNodes.get(3);
		Coordinate c = (Coordinate) myNode.geometry.getCoordinate().clone();
		Officer off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		myNode = stationNodes.get(3);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		myNode = stationNodes.get(3);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		myNode = stationNodes.get(3);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		myNode = stationNodes.get(3);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		myNode = stationNodes.get(3);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "report");
		officers.add(off);				

		myNode = stationNodes.get(3);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "report");
		officers.add(off);				

		myNode = stationNodes.get(3);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "transport");
		officers.add(off);				

		myNode = stationNodes.get(4);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);
		
		myNode = stationNodes.get(4);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);

		myNode = stationNodes.get(4);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);

		myNode = stationNodes.get(4);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "report");
		officers.add(off);
		

		myNode = stationNodes.get(0);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "report");
		officers.add(off);				

		
		myNode = stationNodes.get(2);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "transport");
		officers.add(off);				

		myNode = stationNodes.get(2);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		myNode = stationNodes.get(2);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		myNode = stationNodes.get(2);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "report");
		officers.add(off);				

		myNode = stationNodes.get(2);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "report");
		officers.add(off);				

		
		myNode = stationNodes.get(1);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);
		*/
	}
	
	/**
	 * Officers assigned in approximately correct numbers, with all being assigned to patrolling
	 * 
	 * @param stationNodes
	 */
	void testAgentsALLPATROL(ArrayList <GeoNode> stationNodes){

		// KENTISH TOWN
		GeoNode myNode = stationNodes.get(3);
		Coordinate c = (Coordinate) myNode.geometry.getCoordinate().clone();
		Officer off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		myNode = stationNodes.get(3);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		myNode = stationNodes.get(3);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		myNode = stationNodes.get(3);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		myNode = stationNodes.get(3);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		myNode = stationNodes.get(3);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		myNode = stationNodes.get(3);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		myNode = stationNodes.get(3);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		// WEST HAMPSTEAD
		myNode = stationNodes.get(4);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);
		
		myNode = stationNodes.get(4);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);

		myNode = stationNodes.get(4);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);

		myNode = stationNodes.get(4);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);
		
		// ALBANY

		myNode = stationNodes.get(0);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		// HOLBORN
		
		myNode = stationNodes.get(2);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		myNode = stationNodes.get(2);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		myNode = stationNodes.get(2);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		myNode = stationNodes.get(2);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		myNode = stationNodes.get(2);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);				

		// HAMPSTEAD
		
		myNode = stationNodes.get(1);
		c = (Coordinate) myNode.geometry.getCoordinate().clone();
		off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
		officers.add(off);
	}
	
	void testAgentsCOMPLETELYRANDOM(ArrayList <GeoNode> stationNodes){

		for(int i = 0; i < 19; i++){
			GeoNode myNode = (GeoNode) roadNodes.get(random.nextInt(roadNodes.size()));
			Coordinate c = (Coordinate) myNode.geometry.getCoordinate().clone();
			Officer off = new Officer("id" + random.nextLong(), c, c,  this, Officer.defaultSpeed, "patrol");
			officers.add(off);							
		}


	}
	
	AStar pathfinder = new AStar();
	void connectToMajorNetwork(GeoNode n, Network existingNetwork) {

		try {
			Bag majorNodes;			
			majorNodes = (Bag) existingNetwork.allNodes.clone();
			
			ArrayList <Edge> edges = pathfinder.astarPath(n, new ArrayList <GeoNode> (majorNodes), roads);
			
			if(edges == null) return;
			for(Edge e: edges){
				GeoNode a = (GeoNode) e.getFrom(), b = (GeoNode) e.getTo();
				if(!existingNetwork.nodeExists(a) || !existingNetwork.nodeExists(b))
					existingNetwork.addEdge(a, b, e.info);
			}

		} catch (CloneNotSupportedException e1) {
			e1.printStackTrace();
		}
	}
	
	public void writeEdgesOut(BufferedWriter outWriter, double weight) throws IOException{
		double count = 0;
		for(int i: edgeHeatmap.values())
			count += (double) i;
		
		for(Entry<Edge, Integer> entry: edgeHeatmap.entrySet()){
			Edge e = entry.getKey();
			String myName = ((MasonGeometry)e.info).getStringAttribute("ROADNAME");
			double normValue = weight * edgeHeatmap.get(e);// / count;
			String edgey = "<Placemark><name>" + myName //filename 
					+ "</name>\n<description>" + normValue + "</description>"+
					"<LineString>\n<extrude>" + normValue + "</extrude>\n<tessellate>1</tessellate>\n<altitudeMode>absolute</altitudeMode>\n<coordinates>";
//					+ "</name>\n<description>" + edgeHeatmap.get(e) + "</description>"+
//					"<LineString>\n<extrude>" + edgeHeatmap.get(e) + "</extrude>\n<tessellate>1</tessellate>\n<altitudeMode>absolute</altitudeMode>\n<coordinates>";
			LineString ls = (LineString)((MasonGeometry)e.info).geometry;
			for(Coordinate c: ls.getCoordinates()){
				edgey += c.x + "," + c.y + ",1\n";
			}
			edgey += "\t</coordinates>\n</LineString>\n</Placemark>\n";
			outWriter.write(edgey);
		}
	}
	
	public void finish(){
		super.finish();
		try{
/*
			record_heatmap = new BufferedWriter(new FileWriter(dirName + record_heatmap_filename + mySettings + mySeed + ".txt"));
			IntGrid2D myHeatmap = ((IntGrid2D) this.heatmap.getGrid());

			// write a header
			record_heatmap.write(myHeatmap.getWidth() + "\t" + myHeatmap.getHeight() + "\t" + (int)schedule.getTime() + "\n");
			for(int i = 0; i < myHeatmap.getWidth(); i++){
				String output = "";
				for(int j = 0; j < myHeatmap.getHeight(); j++){
					output += myHeatmap.field[i][j] + "\t";
				}
				record_heatmap.write(output + "\n");
			}
			record_heatmap.close();
*/

			BufferedWriter myKmls = new BufferedWriter(new FileWriter("/Users/swise/postdoc/cpc/data/myGeneratedHeatmap_"+ mySeed +  ".kml"));

			// write a header
			myKmls.write("<?xml version='1.0' encoding='UTF-8'?><kml xmlns='http://earth.google.com/kml/2.1'><Document><open>1</open>\n\n");
			double count = 0;
			for(int i: edgeHeatmap.values())
				count += (double) i;
			
			if(!normaliseOutput)
				count = 1;
			
			for(Entry<Edge, Integer> entry: edgeHeatmap.entrySet()){
				Edge e = entry.getKey();
				String myName = ((MasonGeometry)e.info).getStringAttribute("ROADNAME");
				double normValue = edgeHeatmap.get(e) / count;
				String edgey = "<Placemark><name>" + myName //filename 
						+ "</name>\n<description>" + normValue + "</description>"+
						"<LineString>\n<extrude>" + normValue + "</extrude>\n<tessellate>1</tessellate>\n<altitudeMode>absolute</altitudeMode>\n<coordinates>";
//						+ "</name>\n<description>" + edgeHeatmap.get(e) + "</description>"+
//						"<LineString>\n<extrude>" + edgeHeatmap.get(e) + "</extrude>\n<tessellate>1</tessellate>\n<altitudeMode>absolute</altitudeMode>\n<coordinates>";
				LineString ls = (LineString)((MasonGeometry)e.info).geometry;
				for(Coordinate c: ls.getCoordinates()){
					edgey += c.x + "," + c.y + ",1\n";
				}
				edgey += "\t</coordinates>\n</LineString>\n</Placemark>\n";
				myKmls.write(edgey);
			}
			myKmls.write("</Document></kml>");

			myKmls.close();
			
			BufferedWriter myStatusChanges = new BufferedWriter(new FileWriter("/Users/swise/postdoc/cpc/data/statusChanges_"+ mySeed +  ".txt"));
			for(String s: statusChanges){
				myStatusChanges.write(s);
			}
			myStatusChanges.close();

		} catch (Exception e){
			e.printStackTrace();
		}
	}


	public void writeOutStatuses(String dir, String filename) throws Exception {
		BufferedWriter myStatusChanges = new BufferedWriter(new FileWriter(dir + filename));
		for(String s: statusChanges){
			myStatusChanges.write(s);
		}
		myStatusChanges.close();
	}
	
	//////////////////////////////////////////////
	////////// UTILITIES /////////////////////////
	//////////////////////////////////////////////


	public Network extractMajorRoads(){
		Network majorRoads = new Network();
		for(Object o: roads.getAllNodes()){
			GeoNode n = (GeoNode) o;
			for(Object p: roads.getEdgesOut(n)){
				sim.field.network.Edge e = (sim.field.network.Edge) p;
				String type = ((MasonGeometry)e.info).getStringAttribute("class");
				if(type.equals("major"))
						majorRoads.addEdge(e.from(), e.to(), e.info);
			}
		}
		
		NetworkUtilities.attachUnconnectedComponents(majorRoads, roads);
		
		return majorRoads;
	}
		
	/**
	 * Convenient method for incrementing the heatmap
	 * @param geom - the geometry of the object that is impacting the heatmap
	 */
	public void incrementHeatmap(Geometry geom){
		Point p = geom.getCentroid();
		int x = heatmap.getGridWidth() - (int)(heatmap.getGrid().getWidth()*(MBR.getMaxX() - p.getX())/(MBR.getMaxX() - MBR.getMinX())), 
				y = (int)(heatmap.getGrid().getHeight()*(MBR.getMaxY() - p.getY())/(MBR.getMaxY() - MBR.getMinY()));
		if(x >= 0 && y >= 0 && x < heatmap.getGrid().getWidth() && y < heatmap.getGrid().getHeight()){
			((IntGrid2D) this.heatmap.getGrid()).field[x][y] -= 20;
		}
		MasonGeometry myEdge = getClosestEdge(geom.getCoordinate(), this.resolution, heatEdges);
		int myWeight = myEdge.getIntegerAttribute("weight");
		myEdge.addIntegerAttribute("weight", myWeight + 20);
	}
	
	public GeoNode getClosestGeoNode(Coordinate c, double resolution){
		Bag objects = networkLayer.getObjectsWithinDistance(fa.createPoint(c), resolution);
		if(objects == null || networkLayer.getGeometries().size() <= 0) return null;
		
		objects = networkLayer.getObjectsWithinDistance(fa.createPoint(c), resolution);
		
		double bestDist = resolution; // MUST be within resolution to count
		GeoNode best = null;
		for(Object o: objects){
			double dist = ((GeoNode)o).geometry.getCoordinate().distance(c);
			if(dist < bestDist){
				bestDist = dist;
				best = ((GeoNode)o);
			}
		}
		if(best != null && bestDist <= resolution) 
			return best;
		
		ListEdge edge = getClosestEdge(c);
		
		if(edge == null){
			edge = getClosestEdge(c, resolution * 10);
			if(edge == null)
				return null;
		}
		
		GeoNode n1 = (GeoNode) edge.getFrom();
		GeoNode n2 = (GeoNode) edge.getTo();
		
		if(n1.geometry.getCoordinate().distance(c) <= n2.geometry.getCoordinate().distance(c))
			return n1;
		else 
			return n2;
	}
	
	public ListEdge getClosestEdge(Coordinate c){
		Bag objects = networkEdgeLayer.getObjectsWithinDistance(fa.createPoint(c), resolution);
		if(objects == null || networkEdgeLayer.getGeometries().size() <= 0) return null;
		
		Point point = fa.createPoint(c);
		
		double bestDist = resolution;
		ListEdge bestEdge = null;
		for(Object o: objects){
			double dist = ((MasonGeometry)o).getGeometry().distance(point);
			if(dist < bestDist){
				bestDist = dist;
				bestEdge = (ListEdge) ((AttributeValue) ((MasonGeometry) o).getAttribute("ListEdge")).getValue();
			}
		}
		
		if(bestEdge != null)
			return bestEdge;
		
		else
			return null;
	}
	
	public ListEdge getClosestEdge(Coordinate c, double resolution){
		Bag objects = networkEdgeLayer.getObjectsWithinDistance(fa.createPoint(c), resolution);
		if(objects == null || networkEdgeLayer.getGeometries().size() <= 0) return null;
		
		Point point = fa.createPoint(c);
		
		double bestDist = resolution;
		ListEdge bestEdge = null;
		for(Object o: objects){
			double dist = ((MasonGeometry)o).getGeometry().distance(point);
			if(dist < bestDist){
				bestDist = dist;
				bestEdge = (ListEdge) ((AttributeValue) ((MasonGeometry) o).getAttribute("ListEdge")).getValue();
			}
		}
		
		if(bestEdge != null)
			return bestEdge;
		
		else
			return null;
	}
	
	public MasonGeometry getClosestEdge(Coordinate c, double resolution, GeomVectorField space){
		Bag objects = space.getObjectsWithinDistance(fa.createPoint(c), resolution);
		if(objects == null || space.getGeometries().size() <= 0) return null;
		
		Point point = fa.createPoint(c);
		
		double bestDist = resolution;
		MasonGeometry bestEdge = null;
		for(Object o: objects){
			double dist = ((MasonGeometry)o).getGeometry().distance(point);
			if(dist < bestDist){
				bestDist = dist;
				bestEdge = (MasonGeometry) o;
			}
		}
		
		if(bestEdge != null)
			return bestEdge;
		
		else
			return null;
	}
	
	void seedRandom(long number){
		random = new MersenneTwisterFast(number);
		mySeed = number;
	}
	
	public class CrimeEvent extends MasonGeometry {
		String type;
		HashSet <Object> targets;
		HashSet <Agent> offenders;
		
		public CrimeEvent(String type, HashSet <Object> targets, HashSet <Agent> offenders){
			this.type = type;
			this.targets = targets;
			this.offenders = offenders;
			
			this.geometry = fa.createPoint(offenders.iterator().next().geometry.getCoordinate());
		}
		
		public CrimeEvent(String type, Object target, Agent offender){
			HashSet <Object> targets = new HashSet <Object> (); 
			targets.add(target);
			HashSet <Agent> offenders = new HashSet <Agent> (); 
			offenders.add(offender);

			this.type = type;
			this.targets = targets;
			this.offenders = offenders;
			
			this.geometry = fa.createPoint(offender.geometry.getCoordinate());
		}
		
		public HashSet <Agent> getOffenders(){ return offenders; }
		public HashSet <Object> getTargets(){ return targets; }
	}
	
	public class CallEvent implements Comparable {

		int grading = 0; // Grading: 0 = I, 1 = S, 2 = E, 3 = R
		Geometry location = null;
		long time = -1;
		
		public CallEvent(){
			this(0, null);
		}
		
		public CallEvent(int grade){
			this(grade, null);
		}
		
		public CallEvent(int grade, Geometry location){
			time = System.currentTimeMillis();
			grading = grade;
			this.location = location;
		}
		
		@Override
		public int compareTo(Object arg0) {
			if(arg0 instanceof CallEvent){
				CallEvent ce = (CallEvent) arg0;
				if(ce.grading > grading || ce.time > time) return -1;
				else if(ce.time < time) return 1;
				else return 0;
			}
			return 0;
		}
		
		public Geometry getLocation(){ return location; }		
		public int getGrade(){ return grading; }
	}

	public void recordTravelTime(int i){ travelTimes.add(i); }
	
	public void readInStationLayer(String filename, String layerDescription, GeometryFactory fa){
		try {
			System.out.print("Reading in " + layerDescription + "...");
			stationLayer = new GeomVectorField();
			FileInputStream fstream = new FileInputStream(filename);
			BufferedReader d = new BufferedReader(
					new InputStreamReader(fstream));
			
			String s;
			
			d.readLine(); // get rid of header
			
			while ((s = d.readLine()) != null) {
				String [] bits = s.split(",");
				int x = Integer.parseInt(bits[2]);
				int y = Integer.parseInt(bits[3]);
				stationLayer.addGeometry(new MasonGeometry(fa.createPoint(new Coordinate(x, y))));
			}
			
			d.close();
			
			System.out.println("done");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	ArrayList <CallEvent> callData = new ArrayList <CallEvent> ();
	TreeSet <CallEventWrapper> callTimes = new TreeSet <CallEventWrapper> ();
	
	class CallEventWrapper implements Comparable {
		CallEvent call;
		double time;
		
		public CallEventWrapper(CallEvent call, double time){
			this.call = call;
			this.time = time;
		}
		
		public int compareTo(Object arg0){
			if(!(arg0 instanceof CallEventWrapper)) return -1;
			
			CallEventWrapper cew = (CallEventWrapper) arg0;
			if(cew.time < time) return 1;
			else if(cew.time > time) return -1;
			else return 0;
		}
	}
	
	public void readInCADData(String filename, String layerDescription){
		try {
			System.out.print("Reading in " + layerDescription + "...");

			FileInputStream fstream = new FileInputStream(filename);
			BufferedReader d = new BufferedReader(new InputStreamReader(fstream));
			
			String s;
			
			d.readLine(); // get rid of header
			
			double earliestTime = Double.MAX_VALUE;
			
			while ((s = d.readLine()) != null) {
				String [] bits = s.split("\t");
				int day = Integer.parseInt(bits[3]);// - 15; // TODO: TAKE THIS BACK OUT
				int hour = Integer.parseInt(bits[4]);
				int minute = Integer.parseInt(bits[5]) / 12;
				
				if(bits[7].trim().length() < 1 || bits[8].trim().length() < 1){
	//				System.out.println("CAD record with no location information: " + s);
					continue;
				}
				
				final int x = Integer.parseInt(bits[7]);
				final int y = Integer.parseInt(bits[8]);
				
				final String level = bits[9];
				
				final int code;

				if(level.equals("I ")) code = 0;
				else if(level.equals("S ")) code = 1;
				else if(level.equals("E ")) code = 2;
				else
					continue;
/*				else if(level.equals("S ")) code = 1;
				else code = 3;
	*/			
//				if(Integer.parseInt(bits[0]) == 11847)
//					System.out.println("loooong");
				
				GeoNode closestNode = getClosestGeoNode(new Coordinate(x,y), 50);
				if(closestNode == null) 
					continue;
				
//				System.out.println(closestNode.geometry.getCoordinate().distance(new Coordinate(x,y)));// + " " + bits[0]);
//				double time = (double) (day * 288 + hour * 12 + minute);
				double time = (double) (day * 1440 + hour * 60 + minute);
				callTimes.add(new CallEventWrapper(new CallEvent(code, (Geometry) closestNode.geometry.clone()), time));
				
				if(time < earliestTime)
					earliestTime = time;
			}

			schedule.scheduleOnce(earliestTime, EmergentCrime.ordering_firstContact, new Steppable(){

				@Override
				public void step(SimState state) {
					CallEvent call = callTimes.pollFirst().call;

					if(verbose)
						System.out.println(state.schedule.getTime() + ": Level " + call.getGrade() + " CALL RECEIVED from " + call.getLocation().toString());
					firstContact.receiveCall(null, call);
					crimeLayer.addGeometry(new MasonGeometry(call.getLocation()));
					
					if(!callTimes.isEmpty())
						state.schedule.scheduleOnce(callTimes.first().time, EmergentCrime.ordering_firstContact, this);
				}
				
			});

			d.close();
			
			System.out.println("done");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	public Coordinate snapPointToRoadNetwork(Coordinate c){
		ListEdge myEdge = null;
		double resolution = EmergentCrime.resolution;
		
		if(networkEdgeLayer.getGeometries().size() == 0) 
			return null;
		
		while(myEdge == null && resolution < Double.MAX_VALUE){
			myEdge = getClosestEdge(c, resolution);
			resolution *= 10;
		}
		if(resolution == Double.MAX_VALUE)
			return null;
		
		LengthIndexedLine closestLine = new LengthIndexedLine((LineString) (((MasonGeometry)myEdge.info).getGeometry()));
		double myIndex = closestLine.indexOf(c);
		return closestLine.extractPoint(myIndex);
	}

	
	public void updateEdgeHeatmap(Edge e){
		int weight = edgeHeatmap.get(e);
		edgeHeatmap.put(e, weight + 1);
	}

	public boolean hasExtendedEvent(){
		if(extended_CAD.isEmpty()) 
			return false;
		else if(extended_CAD.size() == 0)
			return false;
		else 
			return true;
	}
	
	public CallEvent getNextExtendedEvent(){
		if(extended_CAD.isEmpty()) 
			return null;
		CallEvent result = extended_CAD.first();
		if(result != null)
			extended_CAD.remove(result);
		return result;
	}
	
	/**
	 * To run the model without visualization
	 */
/*	public static void main(String[] args)
    {
		doLoop(EmergentCrime.class, args);
    }*/
}