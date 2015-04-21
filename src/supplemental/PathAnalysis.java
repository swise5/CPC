package supplemental;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

import sim.EmergentCrime;
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
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LengthIndexedLine;

import ec.util.MersenneTwisterFast;

public class PathAnalysis {

	String dataDir = "/Users/swise/postdoc/cpc/data/vehicleTraces/partition copy/";
	String fileType = ".csv";

	/**
	 * 1) Filter out points which likely represent sampling errors (movement of > 200mph necessary between readings)
	 * 2) Break paths where stationary for more than 10 minutes
	 * 3) Break paths where the shoulder number changes
	 * 4) Filter out points recorded more frequently than 1/minute

	 * @param dirname
	 * @param outname
	 */
	public void generateHeatmapWITHPATHS(String dirname, String outdir, String outsuffix, String fileType){
		try{
		File folder = new File(dirname);
		File[] runFiles = folder.listFiles();
	    SimpleDateFormat ft = new SimpleDateFormat ("dd/MM/yyyy HH:mm:ss");
	    Coordinate centerOfArea = new Coordinate(527900, 184400);
	    CoordinateConversion cv = new CoordinateConversion();

	    AStar pathfinder = new AStar();
	    
	    int omittedDiffs = 0, sameDate = 0, outOfRange = 0;
	    
		int nullCount = 0;
		int plainCount = 0;

	    // for each car, process its file
		for (File f : runFiles) {

			String filename = f.getName();
			if (!filename.endsWith(fileType)) // only process specific kinds of files
				continue;
			
/*			if(filename.equals("M01_5375.csv"))
				System.out.println("yeahhh");
			else
				continue;
	*/		
			// Open the file as an input stream
			FileInputStream fstream;
			fstream = new FileInputStream(f.getAbsoluteFile());

			// Convert our input stream to a BufferedReader
			BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

			// Create the output file
//			BufferedWriter w = new BufferedWriter(new FileWriter(outdir + filename.split(fileType)[0] + outsuffix + ".kml"));
			
			// if outputting as KML, set up the header
//			w.write("<?xml version='1.0' encoding='UTF-8'?><kml xmlns='http://earth.google.com/kml/2.1'><Document><open>1</open>\n\n");
			
			// set up containers for information
			String s = d.readLine(); // get rid of the header (FOR KML)
			String lastTime = null;
			HashMap <String, Coordinate> lastPosRec = new HashMap <String, Coordinate> ();
			HashMap <String, ListEdge> lastEdgeRec = new HashMap <String, ListEdge> ();
			HashMap <String, Long> lastTimeRec = new HashMap <String, Long> ();

			//
			// read in the file line by line
			//
			while ((s = d.readLine()) != null) {

				plainCount++;
				// EXTRACT MEANINGFUL VARIABLES ---------------------------------------------
				
				String[] bits = s.split(","); // split into columns

				String date = bits[18]; // FOOT VERSION
//				String date = bits[2]; // CAR VERSION
				if(date.length() == 10) 
					date += " 00:00:00";
				else if(date.length() == 16) 
					date += ":00";
				Date myDate = ft.parse(date);	

//				if( !bits[1].equals("VAN")) 
//					continue;

	/*			if(date == lastTime){
					sameDate++;
					continue;
				}*/
/* CAR				if(bits[19].equals("NULL")){
					nullCount++;
					continue;
				}
		*/		
				lastTime = date;
				double [] coords = new double []{Double.parseDouble(bits[9]), Double.parseDouble(bits[10])}; // FOOT
// 				double [] coords = new double []{Double.parseDouble(bits[18]), Double.parseDouble(bits[19])}; // CAR
//				double [] coords = cv.wgs84toOSGB36(Double.parseDouble(bits[7]), Double.parseDouble(bits[8]));
				Coordinate c = new Coordinate(coords[0], coords[1]);

				int speed = Integer.parseInt(bits[10]);
				if(bits.length < 16){
					System.out.println("Problem with the data: not enough columns!");
					continue;
				}
				//String shoulderNumber = bits[15];
				
				// DEAL WITH SAMPLING ERRORS ------------------------------------------------
				
				// get rid of sampling errors which place the point far outside the area
				if(c.distance(centerOfArea) > 100000){
					outOfRange++;
					continue;
				}
				
				// get rid of sampling errors which suggest that the vehicle is moving more than 300kph
/*				if(speed > 300){
					continue; // NOTHING FASTER THAN ~200mph
				}
	*/			
				// POSSIBLY CREATE A NEW PATH -----------------------------------------------
				
				long timeDiff = 0;
//				String callSign = bits[0]; // CAR
				String callSign = bits[2]; // FOOT

				if(lastTimeRec.containsKey(callSign))
						timeDiff = myDate.getTime() - lastTimeRec.get(callSign);
				else
					timeDiff = 1;
				
				ListEdge closestEdge = getClosestEdge(c, 30);//EmergentCrime.resolution);
				ListEdge lastEdge = lastEdgeRec.get(callSign);
				
		//		if(closestEdge == null)
		//			System.out.println(bits[17]);
				
//				if(lastEdge != null && closestEdge != null && closestEdge.equals(lastEdge))// && timeDiff < 60000)
//					continue;
				
				lastPosRec.put(callSign, c);
				lastTimeRec.put(callSign, myDate.getTime());

				if(closestEdge == lastEdge && timeDiff > 1000 * 60 * 10){
					if(closestEdge != null) updateEdgeHeatmap(closestEdge);
				}
				else if(closestEdge != null && lastEdge != null){
					if(closestEdge == lastEdge) continue;
					if(closestEdge.getTo() == lastEdge.getTo() || closestEdge.getFrom() == lastEdge.getTo() || 
							closestEdge.getTo() == lastEdge.getFrom() || closestEdge.getFrom() == lastEdge.getFrom())
						updateEdgeHeatmap(closestEdge);
					else{
						ArrayList <Edge> myPath = pathfinder.astarPath((GeoNode)lastEdge.getTo(), (GeoNode)closestEdge.getFrom(), roads);
						if(!myPath.contains(closestEdge))
							myPath.add(closestEdge);
						for(Edge edgy: myPath)
							updateEdgeHeatmap(edgy);	
						System.out.print(myPath.size() + "\t");
					}
				}
				else if(timeDiff < 1000*60*10){
					omittedDiffs++;
				}
				
				/*
				if(closestEdge == lastEdge && timeDiff > 1000 * 60 * 60){
					if(closestEdge != null) updateEdgeHeatmap(closestEdge);
				}
				else if(closestEdge != null && lastEdge != null && timeDiff < 1000 * 60){
					ArrayList <Edge> myPath = pathfinder.astarPath((GeoNode)lastEdge.getTo(), (GeoNode)closestEdge.getFrom(), roads);
					if(!myPath.contains(closestEdge))
						myPath.add(closestEdge);
					for(Edge edgy: myPath)
						updateEdgeHeatmap(edgy);
				}
				else if(closestEdge != null){
					updateEdgeHeatmap(closestEdge);
				}
				else if(timeDiff < 1000*60*10){
					omittedDiffs++;
				}
				 */

	
				lastEdgeRec.put(callSign, closestEdge);

				//w.write(s + "\n");
			}

		}

	    System.out.print("\n");
		System.out.println(plainCount + "\t" + nullCount + "\t" + omittedDiffs + "\t" + sameDate + "\t" + outOfRange);
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 1) Filter out points which likely represent sampling errors (movement of > 200mph necessary between readings)
	 * 2) Break paths where stationary for more than 10 minutes
	 * 3) Break paths where the shoulder number changes
	 * 4) Filter out points recorded more frequently than 1/minute

	 * @param dirname
	 * @param outname
	 */
	public void generateHeatmap(String dirname, String outdir, String outsuffix, String fileType){
		try{
		File folder = new File(dirname);
		File[] runFiles = folder.listFiles();
	    SimpleDateFormat ft = new SimpleDateFormat ("dd/MM/yyyy HH:mm:ss");
	    Coordinate centerOfArea = new Coordinate(527900, 184400);
	    CoordinateConversion cv = new CoordinateConversion();

		int nullCount = 0;
	    // for each car, process its file
		for (File f : runFiles) {

			String filename = f.getName();
			if (!filename.endsWith(fileType)) // only process specific kinds of files
				continue;
			
/*			if(filename.equals("M01_5375.csv"))
				System.out.println("yeahhh");
			else
				continue;
	*/		
			// Open the file as an input stream
			FileInputStream fstream;
			fstream = new FileInputStream(f.getAbsoluteFile());

			// Convert our input stream to a BufferedReader
			BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

			// Create the output file
//			BufferedWriter w = new BufferedWriter(new FileWriter(outdir + filename.split(fileType)[0] + outsuffix + ".kml"));
			
			// if outputting as KML, set up the header
//			w.write("<?xml version='1.0' encoding='UTF-8'?><kml xmlns='http://earth.google.com/kml/2.1'><Document><open>1</open>\n\n");
			
			// set up containers for information
			String s = d.readLine(); // get rid of the header (FOR KML)
			String lastTime = null;
			HashMap <String, Coordinate> lastPosRec = new HashMap <String, Coordinate> ();
			HashMap <String, ListEdge> lastEdgeRec = new HashMap <String, ListEdge> ();
			HashMap <String, Long> lastTimeRec = new HashMap <String, Long> ();

			//
			// read in the file line by line
			//
			while ((s = d.readLine()) != null) {

				// EXTRACT MEANINGFUL VARIABLES ---------------------------------------------
				
				String[] bits = s.split(","); // split into columns
				String date = bits[2];//bits[2] + " " + bits[3];
				if(bits[2].length() == 10) 
					date += " 00:00:00";
				else if(bits[2].length() == 16) 
					date += ":00";
				Date myDate = ft.parse(date);	

//				if( !bits[1].equals("VAN")) 
//					continue;

				if(date == lastTime) continue;
				if(bits[19].equals("NULL")){
					nullCount++;
					continue;
				}
				
				lastTime = date;
				double [] coords = new double []{Double.parseDouble(bits[18]), Double.parseDouble(bits[19])};
//				double [] coords = cv.wgs84toOSGB36(Double.parseDouble(bits[7]), Double.parseDouble(bits[8]));
				Coordinate c = new Coordinate(coords[0], coords[1]);

				int speed = Integer.parseInt(bits[10]);
				if(bits.length < 16){
					System.out.println("Problem with the data: not enough columns!");
					continue;
				}
				//String shoulderNumber = bits[15];
				
				// DEAL WITH SAMPLING ERRORS ------------------------------------------------
				
				// get rid of sampling errors which place the point far outside the area
				if(c.distance(centerOfArea) > 100000)
					continue;
				
				// get rid of sampling errors which suggest that the vehicle is moving more than 300kph
				if(speed > 300){
					continue; // NOTHING FASTER THAN ~200mph
				}
				
				// POSSIBLY CREATE A NEW PATH -----------------------------------------------
				
				long timeDiff = 0;
				if(lastTimeRec.containsKey(bits[0]))
						timeDiff = myDate.getTime() - lastTimeRec.get(bits[0]);
				else
					timeDiff = 1;
				
				ListEdge closestEdge = getClosestEdge(c, 10);//EmergentCrime.resolution);
				ListEdge lastEdge = lastEdgeRec.get(bits[0]);
				
				if(closestEdge == null)
					System.out.println(bits[17]);
				
				if(lastEdge != null && closestEdge != null && closestEdge.equals(lastEdge))// && timeDiff < 60000)
					continue;
				
				if(closestEdge != null)
					updateEdgeHeatmap(closestEdge);

	
				lastPosRec.put(bits[0], c);
				lastTimeRec.put(bits[0], myDate.getTime());
				lastEdgeRec.put(bits[0], closestEdge);

				//w.write(s + "\n");
			}

		}

		System.out.println(nullCount);
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 1) Filter out points which likely represent sampling errors (movement of > 200mph necessary between readings)
	 * 2) Break paths where stationary for more than 10 minutes
	 * 3) Break paths where the shoulder number changes
	 * 4) Filter out points recorded more frequently than 1/minute

	 * @param dirname
	 * @param outname
	 */
	public void omitSamplingErrors(String dirname, String outdir, String outsuffix, String fileType){
		try{
		File folder = new File(dirname);
		File[] runFiles = folder.listFiles();
	    SimpleDateFormat ft = new SimpleDateFormat ("dd/MM/yyyy HH:mm:ss");
	    Coordinate centerOfArea = new Coordinate(527900, 184400);
	    CoordinateConversion cv = new CoordinateConversion();
	    
		// Create the output file
		BufferedWriter w = new BufferedWriter(new FileWriter(outdir + "DUMMYmyOutputTestFile" + outsuffix + ".kml"));
		
		// if outputting as KML, set up the header
		w.write("<?xml version='1.0' encoding='UTF-8'?><kml xmlns='http://earth.google.com/kml/2.1'><Document><open>1</open>\n\n");

	    // for each car, process its file
		for (File f : runFiles) {

			String filename = f.getName();
			if (!filename.endsWith(fileType)) // only process specific kinds of files
				continue;
			
			// Open the file as an input stream
			FileInputStream fstream;
			fstream = new FileInputStream(f.getAbsoluteFile());

			// Convert our input stream to a BufferedReader
			BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

			// Create the output file
//			BufferedWriter w = new BufferedWriter(new FileWriter(outdir + filename.split(fileType)[0] + outsuffix + ".kml"));
			
			// if outputting as KML, set up the header
//			w.write("<?xml version='1.0' encoding='UTF-8'?><kml xmlns='http://earth.google.com/kml/2.1'><Document><open>1</open>\n\n");
			
			// set up containers for information
			String s = d.readLine(); // get rid of the header (FOR KML)
			String lastTime = null;
			String lastShoulderNumber = null;
			Date stopped = null;
			HashMap <String, Coordinate> lastPosRec = new HashMap <String, Coordinate> ();
			HashMap <String, Long> lastTimeRec = new HashMap <String, Long> ();
			Coordinate myLastPos;
			int placemark = 0;
		    String nextPlacemark = "<Placemark><name>" + placemark + //filename.split(fileType)[0] + 
		    		"</name>\n<LineString>\n<extrude>1</extrude>\n<tessellate>1</tessellate>\n<altitudeMode>absolute</altitudeMode>\n<coordinates>";
		    int numCoordsInPlacemark = 0;

		    String outme = "";
			//
			// read in the file line by line
			//
			while ((s = d.readLine()) != null) {

				// EXTRACT MEANINGFUL VARIABLES ---------------------------------------------
				
				String[] bits = s.split(","); // split into columns
				String date = bits[2] + " " + bits[3];
				if(bits[3].length() == 0) date += "00:00:00";
				Date myDate = ft.parse(date);	

//				if( !bits[1].equals("VAN")) 
//					continue;

				if(date == lastTime) continue;
				lastTime = date;
				double [] coords = cv.wgs84toOSGB36(Double.parseDouble(bits[7]), Double.parseDouble(bits[8]));
				Coordinate c = new Coordinate(coords[0], coords[1]);
				//int bearing = Integer.parseInt(bits[8]);
				int speed = Integer.parseInt(bits[10]);
				if(bits.length < 16){
					System.out.println("Problem with the data: not enough columns!");
					continue;
				}
				String shoulderNumber = bits[15];
				
				// DEAL WITH SAMPLING ERRORS ------------------------------------------------
				
				// get rid of sampling errors which place the point far outside the area
				if(c.distance(centerOfArea) > 100000)
					continue;
				
				// get rid of sampling errors which suggest that the vehicle is moving more than 300kph
				myLastPos = lastPosRec.get(bits[0]);
				double velocity = speed;
				if(myLastPos != null){
					velocity = 60000 * 60. * myLastPos.distance(c) / (1000 * Math.max(1, (myDate.getTime() - lastTimeRec.get(bits[0]))));
//					System.out.print(speed + "\t" + (int)(velocity * .621) + "\t");
					if(velocity > 200)
						continue; // NOTHING FASTER THAN ~200mph
				}
				
				// POSSIBLY CREATE A NEW PATH -----------------------------------------------
				
				long timeDiff = 0;
				if(lastTimeRec.containsKey(bits[0]))
						timeDiff = myDate.getTime() - lastTimeRec.get(bits[0]);
				else
					timeDiff = 1;
				
				int placemarkChange = placemark;
				
				// if it's been more than 10 minutes since the last reading, create a new path. THIS VALUE TAKEN FROM TWIDDLING
				if(timeDiff > 60000. * 10){
					if(numCoordsInPlacemark > 1){
						nextPlacemark += "\t</coordinates>\n</LineString>\n</Placemark>\n";
						w.write(nextPlacemark + "\n");
						if(!shoulderNumber.equals(lastShoulderNumber))
							System.out.println(outme + "\t" + lastShoulderNumber);
					}
					outme = myDate.toString();
					placemark++;
					nextPlacemark = formatPlacemarkBeginning(placemark, myDate, shoulderNumber);
					numCoordsInPlacemark = 0;
				}
				else if(timeDiff == 0) //don't record multiple records from the same second
					continue;
				
				// if the vehicle has stopped for at least 10 minutes, create a new path
				if(velocity == 0){
					if(stopped == null) stopped = myDate;
					else if((myDate.getTime() - stopped.getTime()) >= 60000. * 10){
						if(numCoordsInPlacemark > 1){
							nextPlacemark += "\t</coordinates>\n</LineString>\n</Placemark>\n";
							w.write(nextPlacemark + "\n");
							if(!shoulderNumber.equals(lastShoulderNumber))
								System.out.println(outme + "\t" + lastShoulderNumber);
						}
						outme = myDate.toString();
						placemark++;
						nextPlacemark = formatPlacemarkBeginning(placemark, myDate, shoulderNumber);
						numCoordsInPlacemark = 0;
					}
				}
				else stopped = null;

				// if the officer staffing the car changes, create a new path
				if(!shoulderNumber.equals(lastShoulderNumber)){
					if(numCoordsInPlacemark > 1){
						nextPlacemark += "\t</coordinates>\n</LineString>\n</Placemark>\n";
						w.write(nextPlacemark + "\n");
						System.out.println(outme  + "\t" + lastShoulderNumber);
					}
					outme = myDate.toString();
					placemark++;
					nextPlacemark = formatPlacemarkBeginning(placemark, myDate, shoulderNumber);
					numCoordsInPlacemark = 0;
				}

//				if(placemarkChange - placemark != 0)
//					System.out.println("NEW POINT");
//				System.out.println(timeDiff / 60000.);
				nextPlacemark += c.x + "," + c.y + ",1\n";
				numCoordsInPlacemark++;
				ListEdge closestEdge = getClosestEdge(c, EmergentCrime.resolution);
				if(closestEdge != null)
					updateEdgeHeatmap(closestEdge);

	
				lastPosRec.put(bits[0], c);
				lastTimeRec.put(bits[0], myDate.getTime());
				lastShoulderNumber = shoulderNumber;
				//w.write(s + "\n");
			}
			nextPlacemark += "\t</coordinates>\n</LineString>\n</Placemark>\n";
			if(numCoordsInPlacemark > 1){
				w.write(nextPlacemark);
				System.out.println(outme  + "\t" + lastShoulderNumber);
			}

//			w.write("\n</Document></kml>");
//			w.close();
		}
		w.write("\n</Document></kml>");
		w.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	String formatPlacemarkBeginning(int placemark, Date myDate, String shoulderNumber){
		return "<Placemark><name>" + placemark //filename 
				+ "</name>\n<description>\"" + myDate + "_" + shoulderNumber + "\"</description>"+
				"<LineString>\n<extrude>1</extrude>\n<tessellate>1</tessellate>\n<altitudeMode>absolute</altitudeMode>\n<coordinates>";
	}
	
	public void emergencyPathsFromCAD(String dirname, String outdir, String outsuffix, String fileType){
		try{
		File folder = new File(dataDir);
		File[] runFiles = folder.listFiles();
	    SimpleDateFormat ft = new SimpleDateFormat ("dd/MM/yyyy HH:mm:ss");
	    Coordinate centerOfArea = new Coordinate(527900, 184400);
	    CoordinateConversion cv = new CoordinateConversion();
	    
	    // read the CAD times
		// Convert our input stream to a BufferedReader
		FileInputStream fstream = new FileInputStream("/Users/swise/postdoc/cpc/data/vehicleTraces/camdenCAD_Igrade_cleaned_times.csv");
		BufferedReader cadReader = new BufferedReader(new InputStreamReader(fstream));
		String raw = cadReader.readLine();
		
		HashMap <Integer, ArrayList <String>> journeyStarts = new HashMap <Integer, ArrayList <String>> (); 
		while((raw = cadReader.readLine()) != null){
			String[] bits = raw.split(","); // split into columns
			String date = bits[1] + " " + bits[2];
			if(bits[2].length() == 0) date += "00:00:00";
			Date myDate = ft.parse(date);	
	
			int myTime = (int) (myDate.getTime() / (60000 * 5));
			if(! journeyStarts.containsKey(myTime))
				journeyStarts.put(myTime, new ArrayList <String> ());
			journeyStarts.get(myTime).add(bits[0]);
		}
		
	    
		// Create the output file
		BufferedWriter w = new BufferedWriter(new FileWriter(outdir + "CAD_emergency_paths" + outsuffix + ".kml"));
		
		// if outputting as KML, set up the header
		w.write("<?xml version='1.0' encoding='UTF-8'?><kml xmlns='http://earth.google.com/kml/2.1'><Document><open>1</open>\n\n");
		

	    // for each car, process its file
		for (File f : runFiles) {

			String filename = f.getName();
			if (!filename.endsWith(fileType)) // only process specific kinds of files
				continue;
			
			ArrayList <String> emergencyPaths = new ArrayList <String> ();
			
			// Open the file as an input stream
			fstream = new FileInputStream(f.getAbsoluteFile());

			// Convert our input stream to a BufferedReader
			BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

			// set up containers for information
			String s = d.readLine(); // get rid of the header (FOR KML)
			String lastTime = null;
			String lastShoulderNumber = null;
			Date stopped = null;
			HashMap <String, Coordinate> lastPosRec = new HashMap <String, Coordinate> ();
			HashMap <String, Long> lastTimeRec = new HashMap <String, Long> ();
			Coordinate myLastPos;
			int placemark = 0;
		    String nextPlacemark = "<Placemark><name>" + placemark + //filename.split(fileType)[0] + 
		    		"</name>\n<LineString>\n<extrude>1</extrude>\n<tessellate>1</tessellate>\n<altitudeMode>absolute</altitudeMode>\n<coordinates>";
		    int numCoordsInPlacemark = 0;
		    int emergency = 0;
		    int emergencyThreshold = 5;
		    boolean recordThisOne = false, startRecord = false;
		    Coordinate startPoint = null;
		    
			//
			// read in the file line by line
			//
			while ((s = d.readLine()) != null) {

				// EXTRACT MEANINGFUL VARIABLES ---------------------------------------------
				
				String[] bits = s.split(","); // split into columns
				String date = bits[2] + " " + bits[3];
				if(bits[3].length() == 0) date += "00:00:00";
				Date myDate = ft.parse(date);	

				if(date == lastTime) continue;
				lastTime = date;
				double [] coords = cv.wgs84toOSGB36(Double.parseDouble(bits[7]), Double.parseDouble(bits[8]));
				Coordinate c = new Coordinate(coords[0], coords[1]);
				//int bearing = Integer.parseInt(bits[8]);
				int speed = Integer.parseInt(bits[10]);
				if(bits.length < 16){
					System.out.println("Problem with the data: not enough columns!");
					continue;
				}
				String shoulderNumber = bits[15];
				
				// DEAL WITH SAMPLING ERRORS ------------------------------------------------
				
				// get rid of sampling errors which place the point far outside the area
				if(c.distance(centerOfArea) > 100000)
					continue;
				
				// get rid of sampling errors which suggest that the vehicle is moving more than 300kph
				myLastPos = lastPosRec.get(bits[0]);
				double velocity = speed;
				if(myLastPos != null){
					velocity = 60000 * 60. * myLastPos.distance(c) / (1000 * Math.max(1, (myDate.getTime() - lastTimeRec.get(bits[0]))));
					if(velocity > 200)
						continue; // NOTHING FASTER THAN ~200mph
				}
				
				// POSSIBLY CREATE A NEW PATH -----------------------------------------------
				
				long timeDiff = 0;
				if(lastTimeRec.containsKey(bits[0]))
						timeDiff = myDate.getTime() - lastTimeRec.get(bits[0]);
				else
					timeDiff = 1;
				
				int placemarkChange = placemark;

				ArrayList <String> starts = journeyStarts.get( (int) (myDate.getTime() / (60000 * 5)));
				if(starts != null){
					for(String start: starts)
						if(start.equals(shoulderNumber)) 
							startRecord = true;
				}

				if(startRecord){
					nextPlacemark = "<Placemark><name>" + shoulderNumber //filename 
							+ "</name>\n<description>\"" + date + "\"</description>"+
							"<LineString>\n<extrude>1</extrude>\n<tessellate>1</tessellate>\n<altitudeMode>absolute</altitudeMode>\n<coordinates>";
					numCoordsInPlacemark = 0;
					recordThisOne = true;
					startRecord = false;
					System.out.println("<Placemark><Point><coordinates>" + c.x + "," + c.y + ",0</coordinates></Point></Placemark>");
				}
//				else if(!recordThisOne)
//					continue;
				
				// if it's been more than 10 minutes since the last reading, create a new path. THIS VALUE TAKEN FROM TWIDDLING
				if(timeDiff > 60000. * 10){
					if(numCoordsInPlacemark > 1 && recordThisOne){
						nextPlacemark += "\t</coordinates>\n</LineString>\n</Placemark>\n";
						w.write(nextPlacemark + "\n");
					}
					System.out.println("<Placemark><Point><coordinates>" + c.x + "," + c.y + ",0</coordinates></Point></Placemark>");
					recordThisOne = false;
				}
				else if(timeDiff == 0) //don't record multiple records from the same second
					continue;
				
				// if the vehicle has stopped for at least 10 minutes, create a new path
				if(velocity == 0 && recordThisOne){
					if(stopped == null) stopped = myDate;
					else if((myDate.getTime() - stopped.getTime()) >= 60000. * 2){
						if(numCoordsInPlacemark > 1 && recordThisOne){
							nextPlacemark += "\t</coordinates>\n</LineString>\n</Placemark>\n";
							w.write(nextPlacemark + "\n");
							System.out.println();
						}
						recordThisOne = false;
						System.out.println("<Placemark><Point><coordinates>" + c.x + "," + c.y + ",0</coordinates></Point></Placemark>");
					}
				}
				else stopped = null;

				// if the officer staffing the car changes, create a new path
				if(!shoulderNumber.equals(lastShoulderNumber) && recordThisOne){
					if(numCoordsInPlacemark > 1 && recordThisOne){
						nextPlacemark += "\t</coordinates>\n</LineString>\n</Placemark>\n";
						w.write(nextPlacemark + "\n");
					}
					recordThisOne = false;
					System.out.println("<Placemark><Point><coordinates>" + c.x + "," + c.y + ",0</coordinates></Point></Placemark>");
				}

				nextPlacemark += c.x + "," + c.y + ",1\n"; 
				numCoordsInPlacemark++;
	
				lastPosRec.put(bits[0], c);
				lastTimeRec.put(bits[0], myDate.getTime());
				lastShoulderNumber = shoulderNumber;
			}

/*			if(numCoordsInPlacemark > 1 && recordThisOne){
				nextPlacemark += "\t</coordinates>\n</LineString>\n</Placemark>\n";
				w.write(nextPlacemark + "\n");
			}
*/
		}
		w.write("\n</Document></kml>");
		w.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}	
	public void candidateEmergencyPaths(String dirname, String outdir, String outsuffix, String fileType){
		try{
		File folder = new File(dataDir);
		File[] runFiles = folder.listFiles();
	    SimpleDateFormat ft = new SimpleDateFormat ("dd/MM/yyyy HH:mm:ss");
	    Coordinate centerOfArea = new Coordinate(527900, 184400);
	    CoordinateConversion cv = new CoordinateConversion();
	    
		// Create the output file
		BufferedWriter w = new BufferedWriter(new FileWriter(outdir + "emergency_paths" + outsuffix + ".kml"));
		
		// if outputting as KML, set up the header
		w.write("<?xml version='1.0' encoding='UTF-8'?><kml xmlns='http://earth.google.com/kml/2.1'><Document><open>1</open>\n\n");
		

	    // for each car, process its file
		for (File f : runFiles) {

			String filename = f.getName();
			if (!filename.endsWith(fileType)) // only process specific kinds of files
				continue;
			
			ArrayList <String> emergencyPaths = new ArrayList <String> ();
			
			// Open the file as an input stream
			FileInputStream fstream;
			fstream = new FileInputStream(f.getAbsoluteFile());

			// Convert our input stream to a BufferedReader
			BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

			// set up containers for information
			String s = d.readLine(); // get rid of the header (FOR KML)
			String lastTime = null;
			String lastShoulderNumber = null;
			Date stopped = null;
			HashMap <String, Coordinate> lastPosRec = new HashMap <String, Coordinate> ();
			HashMap <String, Long> lastTimeRec = new HashMap <String, Long> ();
			Coordinate myLastPos;
			int placemark = 0;
		    String nextPlacemark = "<Placemark><name>" + placemark + //filename.split(fileType)[0] + 
		    		"</name>\n<LineString>\n<extrude>1</extrude>\n<tessellate>1</tessellate>\n<altitudeMode>absolute</altitudeMode>\n<coordinates>";
		    int numCoordsInPlacemark = 0;
		    int emergency = 0;
		    int emergencyThreshold = 5;
		    
			//
			// read in the file line by line
			//
			while ((s = d.readLine()) != null) {

				// EXTRACT MEANINGFUL VARIABLES ---------------------------------------------
				
				String[] bits = s.split(","); // split into columns
				String date = bits[2] + " " + bits[3];
				if(bits[3].length() == 0) date += "00:00:00";
				Date myDate = ft.parse(date);	

				if(date == lastTime) continue;
				lastTime = date;
				double [] coords = cv.wgs84toOSGB36(Double.parseDouble(bits[7]), Double.parseDouble(bits[8]));
				Coordinate c = new Coordinate(coords[0], coords[1]);
				//int bearing = Integer.parseInt(bits[8]);
				int speed = Integer.parseInt(bits[10]);
				if(bits.length < 16){
					System.out.println("Problem with the data: not enough columns!");
					continue;
				}
				String shoulderNumber = bits[15];
				
				// DEAL WITH SAMPLING ERRORS ------------------------------------------------
				
				// get rid of sampling errors which place the point far outside the area
				if(c.distance(centerOfArea) > 100000)
					continue;
				
				// get rid of sampling errors which suggest that the vehicle is moving more than 300kph
				myLastPos = lastPosRec.get(bits[0]);
				double velocity = speed;
				if(myLastPos != null){
					velocity = 60000 * 60. * myLastPos.distance(c) / (1000 * Math.max(1, (myDate.getTime() - lastTimeRec.get(bits[0]))));
//					System.out.print(speed + "\t" + (int)(velocity * .621) + "\t");
					if(velocity > 200)
						continue; // NOTHING FASTER THAN ~200mph
				}
//				System.out.print((int)velocity + ", ");
				
				// POSSIBLY CREATE A NEW PATH -----------------------------------------------
				
				long timeDiff = 0;
				if(lastTimeRec.containsKey(bits[0]))
						timeDiff = myDate.getTime() - lastTimeRec.get(bits[0]);
				else
					timeDiff = 1;
				
				int placemarkChange = placemark;
				
				// if it's been more than 10 minutes since the last reading, create a new path. THIS VALUE TAKEN FROM TWIDDLING
				if(timeDiff > 60000. * 10){
					if(numCoordsInPlacemark > 1 && emergency > emergencyThreshold){
						nextPlacemark += "\t</coordinates>\n</LineString>\n</Placemark>\n";
						w.write(nextPlacemark + "\n");
					}
					placemark++;
					emergency = 0;
					nextPlacemark = "<Placemark><name>" + shoulderNumber //filename 
							+ "</name>\n<description>\"" + date + "\"</description>"+
							"<LineString>\n<extrude>1</extrude>\n<tessellate>1</tessellate>\n<altitudeMode>absolute</altitudeMode>\n<coordinates>";
					numCoordsInPlacemark = 0;
				}
				else if(timeDiff == 0) //don't record multiple records from the same second
					continue;
				
				// if the vehicle has stopped for at least 10 minutes, create a new path
				if(velocity == 0){
					if(stopped == null) stopped = myDate;
					else if((myDate.getTime() - stopped.getTime()) >= 60000. * 10){
						if(numCoordsInPlacemark > 1 && emergency > emergencyThreshold){
							nextPlacemark += "\t</coordinates>\n</LineString>\n</Placemark>\n";
							w.write(nextPlacemark + "\n");
						}
						placemark++;
						emergency = 0;
						nextPlacemark = "<Placemark><name>" + shoulderNumber //filename 
								+ "</name>\n<description>\"" + date + "\"</description>"+
								"<LineString>\n<extrude>1</extrude>\n<tessellate>1</tessellate>\n<altitudeMode>absolute</altitudeMode>\n<coordinates>";
						numCoordsInPlacemark = 0;
					}
				}
				else stopped = null;

				// if the officer staffing the car changes, create a new path
				if(!shoulderNumber.equals(lastShoulderNumber)){
					if(numCoordsInPlacemark > 1 && emergency > emergencyThreshold){
						nextPlacemark += "\t</coordinates>\n</LineString>\n</Placemark>\n";
						w.write(nextPlacemark + "\n");
					}
					placemark++;
					emergency = 0;
					nextPlacemark = "<Placemark><name>" + shoulderNumber //filename 
							+ "</name>\n<description>\"" + date + "\"</description>"+
							"<LineString>\n<extrude>1</extrude>\n<tessellate>1</tessellate>\n<altitudeMode>absolute</altitudeMode>\n<coordinates>";
					numCoordsInPlacemark = 0;
				}

				if(velocity > 100)
					emergency++;
				
				nextPlacemark += c.x + "," + c.y + ",1\n"; 
				numCoordsInPlacemark++;
	
				lastPosRec.put(bits[0], c);
				lastTimeRec.put(bits[0], myDate.getTime());
				lastShoulderNumber = shoulderNumber;
			}
			nextPlacemark += "\t</coordinates>\n</LineString>\n</Placemark>\n";
			if(numCoordsInPlacemark > 1 && emergency > emergencyThreshold)
				w.write(nextPlacemark);

		}
		w.write("\n</Document></kml>");
		w.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 1) Filter out points which likely represent sampling errors (movement of > 200mph necessary between readings)
	 * 2) Break paths where stationary for more than 10 minutes
	 * 3) Break paths where the shoulder number changes
	 * 4) Filter out points recorded more frequently than 1/minute

	 * @param dirname
	 * @param outname
	 */
	public void omitSamplingErrorsSTARTEND(String dirname, String outdir, String outsuffix, String fileType){
		try{
		File folder = new File(dataDir);
		File[] runFiles = folder.listFiles();
	    SimpleDateFormat ft = new SimpleDateFormat ("dd/MM/yyyy HH:mm:ss");
	    Coordinate centerOfArea = new Coordinate(527900, 184400);
	    CoordinateConversion cv = new CoordinateConversion();
	    
	    // for each car, process its file
		for (File f : runFiles) {

			String filename = f.getName();
			if (!filename.endsWith(fileType)) // only process specific kinds of files
				continue;
			
			// Open the file as an input stream
			FileInputStream fstream;
			fstream = new FileInputStream(f.getAbsoluteFile());

			// Convert our input stream to a BufferedReader
			BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

			// set up containers for information
			String s = d.readLine(); // get rid of the header (FOR KML)
			String lastTime = null;
			String lastShoulderNumber = null;
			Date stopped = null;
			Coordinate lastPosRec = null;
			HashMap <String, Long> lastTimeRec = new HashMap <String, Long> ();
			int placemark = 0;
		    int numCoordsInPlacemark = 0;

		    Coordinate startPoint = null;
		    String startTime = null;
		    double lastSpeed = Double.MAX_VALUE;
		    
			//
			// read in the file line by line
			//
			while ((s = d.readLine()) != null) {

				// EXTRACT MEANINGFUL VARIABLES ---------------------------------------------
				
				String[] bits = s.split(","); // split into columns
				String date = bits[2] + " " + bits[3];
				if(bits[3].length() == 0) date += "00:00:00";
				Date myDate = ft.parse(date);	

				if(date == lastTime) continue;
				lastTime = date;
				double [] coords = cv.wgs84toOSGB36(Double.parseDouble(bits[7]), Double.parseDouble(bits[8]));
				Coordinate c = new Coordinate(coords[0], coords[1]);
				//int bearing = Integer.parseInt(bits[8]);
				int speed = Integer.parseInt(bits[10]);
				if(bits.length < 16){
					System.out.println("Problem with the data: not enough columns!");
					continue;
				}
				String shoulderNumber = bits[15];
				
				// DEAL WITH SAMPLING ERRORS ------------------------------------------------
				
				// get rid of sampling errors which place the point far outside the area
				if(c.distance(centerOfArea) > 100000)
					continue;
				
				// get rid of sampling errors which suggest that the vehicle is moving more than 300kph
				double velocity = speed;
				if(lastPosRec != null){
					velocity = 60000 * 60. * lastPosRec.distance(c) / (1000 * Math.max(1, (myDate.getTime() - lastTimeRec.get(bits[0]))));
					if(velocity > 200)
						continue; // NOTHING FASTER THAN ~200mph
					if(velocity > 60 && lastSpeed < 30){ // SPEEDING, MAYBE A THING!!!
						if(numCoordsInPlacemark > 1){
							System.out.println((int)startPoint.x + "\t" + (int)startPoint.y + "\t" + (int)c.x + "\t" + (int)c.y + "\t" + shoulderNumber + "\t" + startTime);
							startPoint = null;
							startTime = null;
						}
						placemark++;
						numCoordsInPlacemark = 0;

					}
				}
				
				// POSSIBLY CREATE A NEW PATH -----------------------------------------------
				
				long timeDiff = 0;
				if(lastTimeRec.containsKey(bits[0]))
						timeDiff = myDate.getTime() - lastTimeRec.get(bits[0]);
				else
					timeDiff = 1;
				
				int placemarkChange = placemark;
				
				// if it's been more than 10 minutes since the last reading, create a new path. THIS VALUE TAKEN FROM TWIDDLING
				if(timeDiff > 60000. * 10){
					if(numCoordsInPlacemark > 1){
						System.out.println((int)startPoint.x + "\t" + (int)startPoint.y + "\t" + (int)c.x + "\t" + (int)c.y + "\t" + shoulderNumber + "\t" + startTime);
						startPoint = null;
						startTime = null;
					}
					placemark++;
					numCoordsInPlacemark = 0;
				}
				else if(timeDiff == 0) //don't record multiple records from the same second
					continue;
				
				// if the vehicle has stopped for at least 10 minutes, create a new path
				if(velocity == 0){
					if(stopped == null) stopped = myDate;
					else if((myDate.getTime() - stopped.getTime()) >= 60000. * 10){
						if(numCoordsInPlacemark > 1){
							System.out.println((int)startPoint.x + "\t" + (int)startPoint.y + "\t" + (int)c.x + "\t" + (int)c.y + "\t" + shoulderNumber + "\t" + startTime);
							startPoint = null;
							startTime = null;
						}
						placemark++;
						numCoordsInPlacemark = 0;
					}
				}
				else stopped = null;

				// if the officer staffing the car changes, create a new path
				if(!shoulderNumber.equals(lastShoulderNumber)){
					if(numCoordsInPlacemark > 1){
						System.out.println((int)startPoint.x + "\t" + (int)startPoint.y + "\t" + (int)c.x + "\t" + (int)c.y + "\t" + shoulderNumber + "\t" + startTime);
						startPoint = null;
						startTime = null;
					}
					placemark++;
					numCoordsInPlacemark = 0;
				}

				numCoordsInPlacemark++;	
				if(startPoint == null){
					startPoint = c;
					startTime = date;
				}
				lastPosRec = c;
				lastTimeRec.put(bits[0], myDate.getTime());
				lastShoulderNumber = shoulderNumber;
				lastSpeed = velocity;
				//w.write(s + "\n");
			}
			if(numCoordsInPlacemark > 1)
				System.out.println((int)startPoint.x + "\t" + (int)startPoint.y + "\t" + (int)lastPosRec.x + "\t" + (int)lastPosRec.y + "\t" + lastShoulderNumber + "\t" + startTime);

		}
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public void exportUncleaned(String dirname, String outdir, String outsuffix, String fileType){
		try{
		File folder = new File(dataDir);
		File[] runFiles = folder.listFiles();
	    CoordinateConversion cv = new CoordinateConversion();
	    
		// Create the output file
		BufferedWriter w = new BufferedWriter(new FileWriter(outdir + "VANS" + outsuffix + ".kml"));
		
		// if outputting as KML, set up the header
		w.write("<?xml version='1.0' encoding='UTF-8'?><kml xmlns='http://earth.google.com/kml/2.1'><Document><open>1</open>\n\n");
		

	    // for each car, process its file
		for (File f : runFiles) {

			String filename = f.getName();
			if (!filename.endsWith(fileType)) // only process specific kinds of files
				continue;
			
			// Open the file as an input stream
			FileInputStream fstream;
			fstream = new FileInputStream(f.getAbsoluteFile());

			// Convert our input stream to a BufferedReader
			BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

			// set up containers for information
			String s = d.readLine(); // get rid of the header (FOR KML)
		    w.write("<Placemark><name>" + filename.split(fileType)[0] + 
		    		"</name>\n<LineString>\n<extrude>1</extrude>\n<tessellate>1</tessellate>\n<altitudeMode>absolute</altitudeMode>\n<coordinates>");

			//
			// read in the file line by line
			//
			while ((s = d.readLine()) != null) {

				// EXTRACT MEANINGFUL VARIABLES ---------------------------------------------
				
				String[] bits = s.split(","); // split into columns
				
				if(! bits[1].equals("VAN")) 
					continue;
				
				double [] coords = cv.wgs84toOSGB36(Double.parseDouble(bits[7]), Double.parseDouble(bits[8]));
				Coordinate c = new Coordinate(coords[0], coords[1]);
				if(bits.length < 16){
					System.out.println("Problem with the data: not enough columns!");
					continue;
				}
				w.write(c.x + "," + c.y + ",1\n"); 
			}
			w.write("\t</coordinates>\n</LineString>\n</Placemark>\n");

		}
		w.write("\n</Document></kml>");
		w.close();
		} catch (Exception e){
			e.printStackTrace();
		}

	}
	
	public void partitionCars(String inname, String outdir, String outsuffix, int idIndex){
		try {
			File folder = new File(dataDir);
			File[] runFiles = folder.listFiles();
			SimpleDateFormat ft = new SimpleDateFormat("dd/MM/yyyy HH:mm");
			HashMap<String, BufferedWriter> outputs = new HashMap<String, BufferedWriter>();

			// Open the file as an input stream
			FileInputStream fstream;
			fstream = new FileInputStream(inname);

			// Convert our input stream to a BufferedReader
			BufferedReader d = new BufferedReader(
					new InputStreamReader(fstream));

			// set up containers for information
			String s = d.readLine(); // get the header
			String header = s;

/*			BufferedWriter w = new BufferedWriter(new FileWriter(outdir + "catchall" + outsuffix + ".csv"));
			w.write(header + "\n");
			outputs.put("blah", w);
	*/		
			//
			// read in the file line by line
			//
			while ((s = d.readLine()) != null) {

				String[] bits = s.split(","); // split into columns

				String name = bits[idIndex].replace("/", "_");
	//			if(!bits[15].startsWith("EW"))
	//				continue;
				
				if (!outputs.containsKey(name)) {
					// Create the output file
					BufferedWriter w = new BufferedWriter(new FileWriter(outdir + name + outsuffix + ".csv"));
					w.write(header + "\n");
					outputs.put(name, w);
				}

				outputs.get(name).write(s + "\n");
//				w.write(s + "\n");
			}

			for (BufferedWriter o : outputs.values())
				o.close();

			d.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public int grid_width = 700;
	public int grid_height = 700;
	String dirName = "/Users/swise/workspace/CPC/data/";
	
	public GeomVectorField roadLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField networkEdgeLayer = new GeomVectorField(grid_width, grid_height);	
	public GeomVectorField majorRoadNodesLayer = new GeomVectorField(grid_width, grid_height);

	public Bag roadNodes = new Bag();
	public Network roads = new Network(false);

	public GeometryFactory fa = new GeometryFactory();
	
	MersenneTwisterFast random = new MersenneTwisterFast();
	
	HashMap <Edge, Integer> edgeHeatmap = new HashMap <Edge, Integer> ();
	
	long mySeed = 0;
	
	Envelope MBR = null;

	
	public PathAnalysis() {
		try {
			
			InOutUtilities.readInVectorLayer(roadLayer, 
					dirName + "itn/camden_itn_buff100pl2.shp", "road network", new Bag());
			
			////////////////// CLEANUP ///////////////////

			// standardize the MBRs so that the visualization lines up
			
			// clean up the road network
			System.out.print("Cleaning the road network...");
			
			roads = NetworkUtilities.multipartNetworkCleanup(roadLayer, roadNodes, EmergentCrime.resolution, fa, random, 0);
			roadNodes = roads.getAllNodes();

			MBR = roadLayer.getMBR();
			MBR.init(523800, 531800, 180090, 188090); // diff: 8000, 8000

			for(Object o: roadNodes){
				GeoNode n = (GeoNode) o;

				for(Object ed: roads.getEdgesOut(n)){
					ListEdge edge = (ListEdge) ed;
					networkEdgeLayer.addGeometry( (MasonGeometry) edge.info);
					edgeHeatmap.put(edge, 0);
					((MasonGeometry)edge.info).addAttribute("ListEdge", edge);
				}
			}
			
//			partitionCars("/Users/swise/Downloads/vehicle_010311_010411_snapped.csv", "/Users/swise/postdoc/cpc/data/vehicleTraces/allSNAPPED/", "", 0);//"/Users/swise/postdoc/cpc/data/vehicleTraces/CamdenMarch2011_VehiclesTime.csv", "/Users/swise/postdoc/cpc/data/vehicleTraces/ekCallsigns/", "ew");
//			partitionCars("/Users/swise/postdoc/cpc/data/footPatrol/CamdenAPLSData.csv", "/Users/swise/postdoc/cpc/data/footPatrol/partitionedTraces/", "", 2);
//			omitSamplingErrors(dataDir, dataDir + "cleaned/", "_all", fileType);//"_noSampErr_VANS", fileType);
//			omitSamplingErrors("/Users/swise/postdoc/cpc/data/vehicleTraces/ekCallsigns/", "/Users/swise/postdoc/cpc/data/vehicleTraces/ekCallsigns/cleaned/", "_ew", "ew" + fileType);//"_noSampErr_VANS", fileType);
//			generateHeatmap("/Users/swise/postdoc/cpc/data/vehicleTraces/ekCallsigns/", "/Users/swise/postdoc/cpc/data/vehicleTraces/ekCallsigns/cleaned/", "_ew", "ew" + fileType);//"_noSampErr_VANS", fileType);
//			generateHeatmap("/Users/swise/postdoc/cpc/data/vehicleTraces/allSNAPPED/", "/Users/swise/postdoc/cpc/data/vehicleTraces/allSNAPPED/cleaned/", "", "" + fileType);//"_noSampErr_VANS", fileType);
//			generateHeatmapWITHPATHS("/Users/swise/postdoc/cpc/data/vehicleTraces/allSNAPPED/", "/Users/swise/postdoc/cpc/data/vehicleTraces/allSNAPPED/cleaned/", "", "" + fileType);
			generateHeatmapWITHPATHS("/Users/swise/postdoc/cpc/data/footPatrol/partitionedTraces/", "/Users/swise/postdoc/cpc/data/footPatrol/cleanedTraces/", "", "" + fileType);			
			
//			generateHeatmap("/Users/swise/postdoc/cpc/data/vehicleTraces/ekCallsigns/", "/Users/swise/postdoc/cpc/data/vehicleTraces/ekCallsigns/cleaned/", "_ew", "ew" + fileType);//"_noSampErr_VANS", fileType);
			//exportUncleaned(dataDir, dataDir + "cleaned/", "_noSampErr_DELETEME", fileType);
//			candidateEmergencyPaths(dataDir, dataDir + "cleaned/", "_emergency", fileType);
//			emergencyPathsFromCAD(dataDir, dataDir + "cleaned/", "boops", fileType);
			
			BufferedWriter myKmls = new BufferedWriter(new FileWriter("/Users/swise/postdoc/cpc/data/testFootPatrol.kml"));

			// write a header
			myKmls.write("<?xml version='1.0' encoding='UTF-8'?><kml xmlns='http://earth.google.com/kml/2.1'><Document><open>1</open>\n\n");
			double count = 0;
			for(Integer i: edgeHeatmap.values()){
				count += i;
			}
			
			for(Entry<Edge, Integer> entry: edgeHeatmap.entrySet()){
				Edge e = entry.getKey();
				String myName = ((MasonGeometry)e.info).getStringAttribute("ROADNAME");
				double value = edgeHeatmap.get(e);// / count;
				String edgey = "<Placemark><name>" + myName //filename 
						+ "</name>\n<description>" + value + "</description>"+
						"<LineString>\n<extrude>" + value + "</extrude>\n<tessellate>1</tessellate>\n<altitudeMode>absolute</altitudeMode>\n<coordinates>";
				LineString ls = (LineString)((MasonGeometry)e.info).geometry;
				for(Coordinate c: ls.getCoordinates()){
					edgey += c.x + "," + c.y + ",1\n";
				}
				edgey += "\t</coordinates>\n</LineString>\n</Placemark>\n";
				myKmls.write(edgey);
			}
			myKmls.write("</Document></kml>");

			myKmls.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static double distance(double x1, double y1, double x2, double y2){
		return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2,  2));
	}
	
	public static float distFrom(float lat1, float lng1, float lat2, float lng2) {
	    double earthRadius = 3958.75;
	    double dLat = Math.toRadians(lat2-lat1);
	    double dLng = Math.toRadians(lng2-lng1);
	    double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
	               Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
	               Math.sin(dLng/2) * Math.sin(dLng/2);
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	    double dist = earthRadius * c;

	    int meterConversion = 1609;

	    return (float) (dist * meterConversion);
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

	public void updateEdgeHeatmap(Edge e){
		int weight = edgeHeatmap.get(e);
		edgeHeatmap.put(e, weight + 1);
	}

	public static void main(String [] args){
		PathAnalysis pa = new PathAnalysis();
	}
}