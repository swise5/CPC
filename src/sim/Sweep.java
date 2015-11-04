package sim;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.Map.Entry;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

import sim.field.network.Edge;
import sim.util.geo.MasonGeometry;


public class Sweep {

	public static void main(String[] args) throws IOException {

		Random rand = new Random();
		String dir = "/Users/swise/postdoc/cpc/data/scratch/conditions/";

		int [] roles = new int []{0, 1};
		String [] CADs = new String []{"CAD/cadMarch2011.txt", "CAD/cadMarch2010_NONE.txt"};
		double numRuns = 30;
		double rProb = .2, tProb = .05;
		int timeCommit = 60;
		HashMap <String, Edge> myCopyOfWeightings = new HashMap <String, Edge> ();
		for(int role: roles){
			for(String CAD: CADs){
					HashMap <String, Integer> edges = new HashMap <String, Integer> ();
					
					String myFileName = "conditions_" + role + "_" + CAD.charAt(15)+ "_" + (rProb * 100) + "_" + timeCommit + "_" + (tProb * 100) + numRuns;
					System.out.println(myFileName);
					
					System.gc();
					
					for(int i = 0; i < numRuns; i++){
						long seed = rand.nextLong();
						EmergentCrime ec = new EmergentCrime(seed);
						ec.taskingTypeBeingStudied = role;
						if(role == 0)
							ec.rolesDisabled = true;
						ec.param_reportProb = rProb;
						ec.param_reportTimeCommitment = timeCommit;
						ec.param_transportRequestProb = tProb;
						ec.cadFile = CAD;
						ec.start();
						while(ec.schedule.getTime() < 43200)
							ec.schedule.step(ec);
						
						for(Entry<Edge, Integer> entry: ec.edgeHeatmap.entrySet()){
							String myEntryName = ((MasonGeometry)entry.getKey().info).getStringAttribute("FID_1");
							if(!edges.containsKey(myEntryName)){
								edges.put(myEntryName, entry.getValue());
								myCopyOfWeightings.put(myEntryName, entry.getKey());
							}
							else{
								int myVal = edges.get(myEntryName);
								edges.put(myEntryName, myVal + entry.getValue());
							}
							
						}
						System.out.println(edges.size());
						try {
							ec.writeOutStatusChanges(dir + myFileName + "_" + i + ".txt");
						} catch (Exception e) {
							e.printStackTrace();
						}
						//ec.finish();
					}
				
					BufferedWriter myKmls = new BufferedWriter(new FileWriter(dir + myFileName + ".kml"));

					// write a header
					myKmls.write("<?xml version='1.0' encoding='UTF-8'?><kml xmlns='http://earth.google.com/kml/2.1'><Document><open>1</open>\n\n");

					// create a record for each of the LineStrings 
					for(Entry<String, Integer> entry: edges.entrySet()){
						
						// get the edge itself
						Edge e = myCopyOfWeightings.get(entry.getKey());
						String myName = ((MasonGeometry)e.info).getStringAttribute("ROADNAME");
						
						// take the average value over the number of runs
						double normValue = entry.getValue() / numRuns;
						
						// create the string that represents the record
						String edgey = "<Placemark><name>" + myName 
								+ "</name>\n<description>" + normValue + "</description>"+
								"<LineString>\n<extrude>" + normValue + "</extrude>\n<tessellate>1</tessellate>\n<altitudeMode>absolute</altitudeMode>\n<coordinates>";
						LineString ls = (LineString)((MasonGeometry)e.info).geometry;
						for(Coordinate c: ls.getCoordinates()){
							edgey += c.x + "," + c.y + ",1\n";
						}
						edgey += "\t</coordinates>\n</LineString>\n</Placemark>\n";
						
						// write the record out
						myKmls.write(edgey);

					}

					myKmls.write("</Document></kml>");
					myKmls.close();					
				}
		}

	}
	
	/** PARAMETER SWEEP

		public static void main(String[] args) throws IOException {

		Random rand = new Random();
		String dir = "/Users/swise/postdoc/cpc/data/scratch/statusChecks/";

//		String [] names = new String []{"completeRandom", "allTaskings"};
//		String [] names = new String []{"allTaskings"};
		double numRuns = 20;
		HashMap <String, Edge> myCopyOfWeightings = new HashMap <String, Edge> ();
		for(double rProb = .15; rProb <= .25; rProb += .1){
			for(int timeCommit = 15; timeCommit <= 60; timeCommit += 15){
				for(double tProb = .05; tProb <= .15; tProb += .05){
					HashMap <String, Integer> edges = new HashMap <String, Integer> ();
					
					String myFileName = "statusChecks_" + (rProb * 100) + "_" + timeCommit + "_" + (tProb * 100) + numRuns;

					System.gc();
					
					for(int i = 0; i < numRuns; i++){
						long seed = rand.nextLong();
						EmergentCrime ec = new EmergentCrime(seed);
						ec.taskingTypeBeingStudied = 1; 
						ec.param_reportProb = rProb;
						ec.param_reportTimeCommitment = timeCommit;
						ec.param_transportRequestProb = tProb;
						ec.start();
						while(ec.schedule.getTime() < 43200)
							ec.schedule.step(ec);
						
						for(Entry<Edge, Integer> entry: ec.edgeHeatmap.entrySet()){
							String myEntryName = ((MasonGeometry)entry.getKey().info).getStringAttribute("FID_1");
							if(!edges.containsKey(myEntryName)){
								edges.put(myEntryName, entry.getValue());
								myCopyOfWeightings.put(myEntryName, entry.getKey());
							}
							else{
								int myVal = edges.get(myEntryName);
								edges.put(myEntryName, myVal + entry.getValue());
							}
							
						}
						System.out.println(edges.size());
						try {
							ec.writeOutStatuses(dir, myFileName + "_" + i + ".txt");
						} catch (Exception e) {
							e.printStackTrace();
						}
						//ec.finish();
					}
				
					BufferedWriter myKmls = new BufferedWriter(new FileWriter(dir + myFileName + ".kml"));

					// write a header
					myKmls.write("<?xml version='1.0' encoding='UTF-8'?><kml xmlns='http://earth.google.com/kml/2.1'><Document><open>1</open>\n\n");

					// create a record for each of the LineStrings 
					for(Entry<String, Integer> entry: edges.entrySet()){
						
						// get the edge itself
						Edge e = myCopyOfWeightings.get(entry.getKey());
						String myName = ((MasonGeometry)e.info).getStringAttribute("ROADNAME");
						
						// take the average value over the number of runs
						double normValue = entry.getValue() / numRuns;
						
						// create the string that represents the record
						String edgey = "<Placemark><name>" + myName 
								+ "</name>\n<description>" + normValue + "</description>"+
								"<LineString>\n<extrude>" + normValue + "</extrude>\n<tessellate>1</tessellate>\n<altitudeMode>absolute</altitudeMode>\n<coordinates>";
						LineString ls = (LineString)((MasonGeometry)e.info).geometry;
						for(Coordinate c: ls.getCoordinates()){
							edgey += c.x + "," + c.y + ",1\n";
						}
						edgey += "\t</coordinates>\n</LineString>\n</Placemark>\n";
						
						// write the record out
						myKmls.write(edgey);

					}

					myKmls.write("</Document></kml>");
					myKmls.close();					
				}
			}
		}

	}
	/**
		double numRuns = 10;
		HashMap <String, Edge> myCopyOfWeightings = new HashMap <String, Edge> ();
		for(double rProb = .05; rProb <= .20; rProb += .05){
			for(int timeCommit = 60; timeCommit <= 61; timeCommit += 61){
				for(double tProb = .05; tProb <= .20; tProb += .05){
					HashMap <String, Integer> edges = new HashMap <String, Integer> ();
					
	 * 
	 */
}