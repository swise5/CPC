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

		String [] names = new String []{"completeRandom", "allTaskings"};
//		String [] names = new String []{"allTaskings"};
		double numRuns = 10;
		HashMap <String, Edge> myCopyOfWeightings = new HashMap <String, Edge> ();
		for(int j = 1; j < 2; j++){

			HashMap <String, Integer> edges = new HashMap <String, Integer> ();
			
			System.gc();
			
			for(int i = 0; i < numRuns; i++){
				long seed = rand.nextLong();
				EmergentCrime ec = new EmergentCrime(seed);
				ec.taskingTypeBeingStudied = j; 
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
				ec.finish();
			}
		
			BufferedWriter myKmls = new BufferedWriter(new FileWriter("/Users/swise/postdoc/cpc/data/scratch/timeTest_fewerRides_" + names[j] + "_" 
					+ numRuns + ".kml"));

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