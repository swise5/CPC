package supplemental;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import javax.swing.JFrame;

import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.field.geo.GeomVectorField;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.FieldPortrayal2D;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.util.geo.MasonGeometry;
import sim.util.gui.SimpleColorMap;
import swise.objects.AStar;
import swise.visualization.AttributePolyPortrayal;
import swise.visualization.SegmentedColorMap;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;

public class CompareBatchKMLs {

	String dirname = "/Users/swise/postdoc/cpc/data/stoppageExploration/rawdata/sweepFineRprobTprob/";
	String fout = "/Users/swise/postdoc/cpc/data/stoppageExploration/rawdata/sweepFineRprobTprob/myOutputComparisonTest.csv";
	String fileType = ".kml";

	public CompareBatchKMLs() throws Exception {

		// prep storage
		HashMap <String, HashMap <String, Double>> allRoadWeights = new HashMap <String, HashMap <String, Double>> ();

		
		// extract all files from directory
		File folder = new File(dirname);
		File[] runFiles = folder.listFiles();
		
		GeometryFactory fa = new GeometryFactory();
		
		ArrayList <String> allFiles = new ArrayList <String> ();

	    // for each kml, extract its readings
		for (File f : runFiles) {

			String filename = f.getName();
			if (!filename.endsWith(fileType)) // only process specific kinds of files
				continue;
			allFiles.add(filename);
			
			// Open the file as an input stream
			FileInputStream fstream;
			fstream = new FileInputStream(f.getAbsoluteFile());

			// Convert our input stream to a BufferedReader
			BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

			String s;
			boolean inLoop = false;

			//
			// Assemble all coordinates for comparison; also store description and name
			//

			// individual records
			String myName = "";
			String myCoords = "";
			double description = 0;

			double totalRecords = 0;
			
			int firstLoops = 0, secondLoops = 0;
			ArrayList <Coordinate> points = new ArrayList <Coordinate> ();
			
			// loop over records
			while ((s = d.readLine()) != null) {

				if(s.startsWith("<coordinates>")){
					inLoop = true;
					firstLoops++;
				}

				if(inLoop){
					myCoords += s + "\n";
					String [] coords = s.split(",");
					Coordinate c = null;
					if(s.startsWith("<coord"))
						c = new Coordinate(Double.parseDouble((coords[0].split("<coordinates>"))[1]), Double.parseDouble(coords[1]));
					else if(!s.endsWith("</coordinates>"))
						c = new Coordinate(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]));
					if(c != null)
					points.add(c);
				}

				if(s.contains("</coordinates>")){
					inLoop = false;

					if(!allRoadWeights.containsKey(myCoords))
						allRoadWeights.put(myCoords, new HashMap <String, Double> ());

					double length = fa.createLineString(points.toArray(new Coordinate [points.size()])).getLength();
					if(length <= 1)
						System.out.println("hmm");
					description *= length;
					points = new ArrayList <Coordinate> ();
					
					if(allRoadWeights.get(myCoords).containsKey(filename)){
						double temp = allRoadWeights.get(myCoords).get(filename);
						allRoadWeights.get(myCoords).put(filename, description + temp);
					}
					else
						allRoadWeights.get(myCoords).put(filename, description);
					myCoords = "";
				}

				else if(s.startsWith("<description>")){
					String [] bits = s.split("</description>");
					description = Double.parseDouble(bits[0].substring(13));
				}
				else if(s.startsWith("<Placemark><name>")){
					myName = s;
				}
			}
			
			// clean up
			d.close();
		}
	
		
		// clean up
		BufferedWriter w = new BufferedWriter(new FileWriter(fout));
		ArrayList <String> allRoads = new ArrayList <String> (allRoadWeights.keySet());
		//Collections.sort(allRoads);
		for(String file: allFiles)
			w.write(file + "\t");
		w.newLine();
		for(String road: allRoads){
			HashMap <String, Double> roadVals = allRoadWeights.get(road);
			for(String file: allFiles){
				if(!roadVals.containsKey(file)) w.write("0\t");
				else w.write(roadVals.get(file) + "\t");
			}
			w.newLine();
		}
		w.close();
	}
	
	/** Runs the simulation */
	public static void main(String[] args) {
		try {
			CompareBatchKMLs gui =  new CompareBatchKMLs();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
}