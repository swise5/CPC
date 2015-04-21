package supplemental;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import swise.visualization.AttributePolyPortrayal;
import swise.visualization.SegmentedColorMap;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;

public class MergeKMLs extends GUIState{
	
	public Display2D display;
	public JFrame displayFrame;
	
	public int grid_width = 700, grid_height = 700;
	public GeomVectorField baseLayer = new GeomVectorField(grid_width, grid_height);

	
	// Map visualization objects
	private GeomVectorFieldPortrayal map = new GeomVectorFieldPortrayal();

	public class Dummy extends SimState{

		public Dummy(long seed) {
			super(seed);
		}
		
	}
	
	public MergeKMLs(SimState state) {
		super(state);
		this.state = new Dummy(123);
		setup();
	}
	
	/** Begins the simulation */
	public void start() {
		super.start();
		
		// set up portrayals
		setupPortrayals();
	}


	public void setupPortrayals() {
		
		map.setField(baseLayer);
//		map.setPortrayalForAll(new GeomPortrayal(new Color(70,70,70), false));
/*		map.setPortrayalForAll(new AttributePolyPortrayal(
						new SimpleColorMap(-5,5, new Color(0,0,255), new Color(255,0,0)),
						"weight", new Color(0,0,0,0), true, 5));
	*/
		map.setPortrayalForAll(new AttributePolyPortrayal(
				new SegmentedColorMap(new double[]{-100,100}, new Color[]{Color.blue, Color.red}),
//				new SegmentedColorMap(new double[]{-5,0,5}, new Color[]{Color.blue, new Color(100,0,100,255), Color.red}),
	//			new SimpleColorMap(-10,10, new Color(0,255,0), new Color(255,0,0)),
				//new SimpleColorMap(-10,10, new Color(0,255,0), new Color(255,0,0)),
				"weight", new Color(0,0,0,0), true, 5));



		// reschedule the displayer
		display.reset();
//		display.setBackdrop(new Color(191,216, 236));
		display.setBackdrop(Color.black);

		// redraw the display
		display.repaint();

	}

	/** Initializes the simulation visualization */
	public void init(Controller c) {
		super.init(c);

		// the map visualization
		display = new Display2D((int)(1.5 * grid_width), (int)(1.5 * grid_height), this);

		display.attach(map, "Heatmap", true);
		
		displayFrame = display.createFrame();
		c.registerFrame(displayFrame); // register the frame so it appears in the "Display" list
		displayFrame.setVisible(true);

	}

	/** Quits the simulation and cleans up.*/
	public void quit() {
		super.quit();

		if (displayFrame != null)
			displayFrame.dispose();
		displayFrame = null; // let gc
		display = null; // let gc
	}

	/** Runs the simulation */
	public static void main(String[] args) {
		MergeKMLs gui =  new MergeKMLs(null);		
		Console console = new Console(gui);
		console.setVisible(true);
	}

	/** Returns the name of the simulation */
	public static String getName() { return "Viewer"; }

	public void setup() {
//sweeping_allTaskings_5.0_corrected.kml",//
		String fin = "/Users/swise/postdoc/cpc/data/dataForPaper/sanityCheck/random_10.kml",//stoppageExploration/rawdata/timeTest_allTaskings_10.0.kml",//dataForPaper/sanityCheck/taskings_10.kml",//random.kml",//sweeping_allTaskings_5.0_corrected_longerIncidents.kml",//"/Users/swise/postdoc/cpc/data/2011march_taskings_noNorm.kml",tasksAllEdges_run1.kml",// 
				fin2 = "/Users/swise/postdoc/cpc/data/dataForPaper/sanityCheck/goldStandard_raw.kml",//random_10.kml",//allEdgesHeatmapSNAPPED_PATHS_30mres_oneUsage.kml", 
				fout = "/Users/swise/postdoc/cpc/data/dataForPaper/sanityCheck/trash.kml";//snappedHeatMinusTaskings.kml";

		GeometryFactory fa = new GeometryFactory();
		try {
			// Open the tracts file
			FileInputStream fstream = new FileInputStream(fin);

			// Convert our input stream to a BufferedReader
			BufferedReader d = new BufferedReader(new InputStreamReader(fstream));
			BufferedWriter w = new BufferedWriter(new FileWriter(fout));

			String s;
			String header = "<?xml version='1.0' encoding='UTF-8'?><kml xmlns='http://earth.google.com/kml/2.1'>"
					+ "<Document><open>1</open>\n\n";
			String footer = "</Document></kml>";

			boolean inLoop = false;

			//
			// Assemble all coordinates for comparison; also store description and name
			//

			// storage
			HashMap <String, String> geoToName = new HashMap <String, String> ();
			HashMap <String, Double> geoToDescript = new HashMap <String, Double> ();
			
			// individual records
			String myName = "";
			String myCoords = "";
			double description = 0;

			double totalRecords = 0;
			
			int firstLoops = 0, secondLoops = 0;
			
			// loop over records
			while ((s = d.readLine()) != null) {

				if(s.startsWith("<coordinates>")){
					inLoop = true;
					firstLoops++;
				}

				if(inLoop){
					myCoords += s + "\n";
				}

				if(s.contains("</coordinates>")){
					inLoop = false;
					// MERGE DUPLICATE RECORDS, SUMMING THEIR VALUES
					if(geoToName.containsKey(myCoords)){
						double myValue = geoToDescript.get(myCoords) + description;
						geoToDescript.put(myCoords, myValue);
						totalRecords += myValue;
					}
					else {						
						geoToName.put(myCoords, myName);
						geoToDescript.put(myCoords, description);
						totalRecords += description;
					}
					myCoords = "";
				}

				else if(s.startsWith("<description>")){
					String [] bits = s.split("</description>");
					description = Double.parseDouble(bits[0].substring(13));
				}
				else if(s.startsWith("<Placemark><name>"))
					myName = s;
			}
			d.close();
			
			//
			// Go through the second file and compare all coordinates; check on name and aggregate description
			//
			double allMyVals = 0;
			for(String name: geoToName.keySet()){
				allMyVals += geoToDescript.get(name);
				//
//				geoToDescript.put(name, geoToDescript.get(name)/3390314);//2347163.0);//3109329.5);
			}
			
			System.out.println(allMyVals);
			// Open the file
			fstream = new FileInputStream(fin2);
			d = new BufferedReader(new InputStreamReader(fstream));
			
			myCoords = "";
			
			
			boolean preDescript = true;
			String myRecord = "";
			double sumOfValues = 0;
			
			ArrayList <Coordinate> points = new ArrayList <Coordinate> ();
			
			// loop over records
			while ((s = d.readLine()) != null) {
				if(s.startsWith("<coordinates>")){
					inLoop = true;
					secondLoops++;
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
//					double myValue = Math.max(0, Math.log(description));// / 1217472.0;// / 1242647;//24521;
					double myValue = description;// / 3109329.5;// / 1217472.0;// / 1242647;//24521;
					MasonGeometry mg = new MasonGeometry(fa.createLineString(points.toArray(new Coordinate [points.size()])));

					if(geoToName.containsKey(myCoords)){
//						double otherValue = Math.max(0, Math.log(geoToDescript.get(myCoords)));// / allMyVals;
						double otherValue = geoToDescript.get(myCoords);// / allMyVals;
						sumOfValues += myValue;
						double tempVal = myValue - otherValue;//Math.log10(myValue + 1) - Math.log10(otherValue + 1);
					//	myValue = Math.log(Math.max(1, Math.abs(tempVal)));
					//	if(tempVal < 0) myValue *= -1;
					//	myValue = tempVal;
						System.out.print(myValue + "\t");
						//						myValue = (Math.log(myValue + 1) - Math.log(otherValue + 1)) / 7;

//						myValue = Math.pow((myValue - otherValue ), 1);
//						myValue = otherValue;
								//(description - geoToDescript.get(myCoords)) / (description + geoToDescript.get(myCoords));
							//Math.max(.000000001, description);//(10 * description - geoToDescript.get(myCoords)) / 274110;
						if(myValue == Double.NaN)
							myValue = 0;
//						w.write(geoToDescript.get(myCoords) + "\t" + description + "\n");
//						w.write(myValue + "\t"+ geoToDescript.get(myCoords) + "\t" + description + "\n");
//						System.out.println(myValue + "\t"+ geoToDescript.get(myCoords) + "\t" + description);
						w.write(otherValue + "\t" + myValue + "\t" + mg.getGeometry().getLength() + "\t" +//description + "\t" + 
									myCoords.replaceAll("\\s", "_") + "\n");
					}
					else{
					//	System.out.println("PROBLEM");
						//w.write(myCoords + "\t" + description + "\n");
						//System.out.print("\t" + description);
					}
//					w.write("<description>" + myValue + "</description><LineString>\n");
//					w.write(myRecord);
					myCoords = "";
					preDescript = true;
					myRecord = "";
					mg.addIntegerAttribute("weight", (int)(10 * myValue ));//1206754));//myValue);//(int) Math.max(0, Math.log(myValue)));
//					mg.addIntegerAttribute("weight", (int)(10000 * description / 1206754));//myValue);//(int) Math.max(0, Math.log(myValue)));
					baseLayer.addGeometry(mg);
					points = new ArrayList <Coordinate> ();
				}

				else if(s.startsWith("<description>")){
					preDescript = false;
					String [] bits = s.split("</description>");
					description = Double.parseDouble(bits[0].substring(13));
				}
				else if(s.startsWith("<Placemark><name>"))
					myName = s;
				
	/*			if(preDescript)
					w.write(s + "\n");
				else if(!s.startsWith("<descr"))
					myRecord += s + "\n";
*/
			}
			
			Envelope MBR = new Envelope();
			MBR.init(523800, 531800, 180090, 188090); // diff: 8000, 8000
			baseLayer.setMBR(MBR);

		//	System.out.println("\n" + firstLoops + "\t" + secondLoops + "\t" + geoToName.size());
			System.out.println(sumOfValues);
			// clean up
			w.close();
			d.close();
		} catch (Exception e) {
			System.err.println("File input error");
		}

	}
	
}