package sim;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JFrame;

import org.jfree.data.xy.XYSeries;

import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.portrayal.grid.FastValueGridPortrayal2D;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.FieldPortrayal2D;
import sim.util.gui.SimpleColorMap;
import sim.util.media.chart.TimeSeriesChartGenerator;
import swise.objects.network.GeoNode;
import swise.objects.network.GeoObjectNode;
import swise.visualization.AttributePolyPortrayal;
import swise.visualization.GeomNetworkFieldPortrayal;
import swise.visualization.SegmentedColorMap;

/**
 * A visualization of the Hotspots simulation.
 * 
 * @author swise
 */
public class EmergentCrimeWithUI extends GUIState {

	EmergentCrime sim;
	public Display2D display;
	public JFrame displayFrame;
	
	// Map visualization objects
	private GeomVectorFieldPortrayal map = new GeomVectorFieldPortrayal();
	private GeomVectorFieldPortrayal roads = new GeomVectorFieldPortrayal();
	private GeomVectorFieldPortrayal stations = new GeomVectorFieldPortrayal();
	private GeomVectorFieldPortrayal officers = new GeomVectorFieldPortrayal();
	private GeomVectorFieldPortrayal network = new GeomVectorFieldPortrayal();
	private GeomVectorFieldPortrayal crimes = new GeomVectorFieldPortrayal();
	
	private FastValueGridPortrayal2D heatmap = new FastValueGridPortrayal2D();	
	private GeomVectorFieldPortrayal heatEdges = new GeomVectorFieldPortrayal();

	
	// Hashtag chart visualization objects
	public TimeSeriesChartGenerator dummyChart;
	ArrayList <XYSeries> dummyTrends = new ArrayList <XYSeries> ();
	ArrayList <String> dummyTrendNames = new ArrayList <String> ();
	
	///////////////////////////////////////////////////////////////////////////
	/////////////////////////// BEGIN functions ///////////////////////////////
	///////////////////////////////////////////////////////////////////////////	
	
	/** Default constructor */
	public EmergentCrimeWithUI(SimState state) {
		super(state);
		sim = (EmergentCrime) state;
	}

	/** Begins the simulation */
	public void start() {
		super.start();
		
		// set up portrayals
		setupPortrayals();
	}

	/** Loads the simulation from a point */
	public void load(SimState state) {
		super.load(state);
		
		// we now have new grids. Set up the portrayals to reflect that
		setupPortrayals();
	}

	/**
	 * Sets up the portrayals of objects within the map visualization. This is called by both start() and by load()
	 */
	public void setupPortrayalsFULL() {
		
		EmergentCrime world = (EmergentCrime) state;
//		map.setField(world.baseLayer);
//		map.setPortrayalForAll(new GeomPortrayal(new Color(69, 148, 199)));
/*		map.setPortrayalForAll(new AttributePolyPortrayal(
				new SimpleColorMap(0,13000, new Color(100,80,30), new Color(240,220,200)),
				"gid", new Color(0,0,0,0), true));
		map.setImmutableField(true);
	*/	
		roads.setField(world.roadLayer);
//		roads.setPortrayalForAll(new GeomPortrayal(new Color(8, 61, 126), false));
		roads.setPortrayalForAll(new GeomPortrayal(new Color(70,70,70), false));
		
		network.setField(world.networkLayer);
		//network.setPortrayalForClass(GeoNode.class, new GeomPortrayal(new Color(70,70,200), false));
		network.setPortrayalForAll(//new GeomPortrayal(new Color(255,0,0), 5));
				new AttributePolyPortrayal(
						new SimpleColorMap(0,1, new Color(255,0,0), new Color(255,0,0)),
						"delay", new Color(0,0,0,0), true, 5));
		

		officers.setField(world.officerLayer);
		officers.setPortrayalForAll( //new GeomPortrayal(Color.yellow, 10));
				new AttributePolyPortrayal(
					new SegmentedColorMap( new double []{0, 1, 2, 3}, 
				//	new Color[] { Color.gray, Color.yellow, Color.gray, Color.orange}),
				//"status", new Color(0,0,0,0), true, 10));
				new Color[] { Color.gray, Color.red, Color.green, Color.blue}),
				"taskingType", new Color(0,0,0,0), true, 10));

		stations.setField(world.stationLayer);
		stations.setPortrayalForAll( new GeomPortrayal(Color.white, 30));

		
		heatmap.setField(world.heatmap.getGrid()); 
		heatmap.setMap(new SimpleColorMap(0, 10, Color.black, Color.yellow));
		
		
		crimes.setField(world.crimeLayer);
		crimes.setPortrayalForAll( new GeomPortrayal( new Color(255, 0, 0, 100), 15));
		// reset stuff
		// reschedule the displayer
		display.reset();
//		display.setBackdrop(new Color(191,216, 236));
		display.setBackdrop(Color.black);

		// redraw the display
		display.repaint();

//		dummyChart.removeAllSeries();
//		state.schedule.scheduleRepeating(new HashtagTracker());
	}
	
	public void setupPortrayals() {
		
		EmergentCrime world = (EmergentCrime) state;
//		map.setField(world.baseLayer);
//		map.setPortrayalForAll(new GeomPortrayal(new Color(69, 148, 199)));

		roads.setField(world.roadLayer);
		roads.setPortrayalForAll(new GeomPortrayal(new Color(150,150,150), false));
		

		heatEdges.setField(world.heatEdges);
		heatEdges.setPortrayalForAll(
				new AttributePolyPortrayal(
						new SimpleColorMap(0,100, new Color(0,0,0,0), new Color(255,0,0)),
						"weight", new Color(0,0,0,0), false, 5));


		network.setField(world.networkLayer);
		network.setPortrayalForAll(
				new AttributePolyPortrayal(
						new SimpleColorMap(0,1, new Color(255,0,0), new Color(255,0,0)),
						"delay", new Color(0,0,0,0), true, 5));
		
		officers.setField(world.officerLayer);
		officers.setPortrayalForAll(
				new AttributePolyPortrayal(
					new SegmentedColorMap( new double []{0, 1, 2, 3}, 
				new Color[] { Color.gray, new Color(150,150,255), new Color(50,75,255), new Color(75,50,255)}),//Color.red, Color.green, Color.blue}),
				"taskingType", new Color(0,0,0,0), true, 20));

		stations.setField(world.stationLayer);
		stations.setPortrayalForAll( new GeomPortrayal(new Color(50,50,50), 30, true));

		
		heatmap.setField(world.heatmap.getGrid()); 
		heatmap.setMap(new SimpleColorMap(0, 100, Color.red, new Color(0,0,0,0)));
		
		crimes.setField(world.crimeLayer);
		crimes.setPortrayalForAll( new GeomPortrayal( new Color(255, 0, 0, 100), 15));
		
		// reset stuff
		// reschedule the displayer
		display.reset();
//		display.setBackdrop(new Color(191,216, 236));
		display.setBackdrop(Color.white);

		// redraw the display
		display.repaint();
	}

	/** Initializes the simulation visualization */
	public void init(Controller c) {
		super.init(c);

		// the map visualization
		display = new Display2D((int)(1.5 * sim.grid_width), (int)(1.5 * sim.grid_height), this);

		display.attach(heatmap, "Heatmap", false);
//		display.attach(map, "Landscape");
		display.attach(roads, "Roads");
		display.attach(heatEdges, "Heat Edges");
		display.attach(stations, "Police Stations");
//		display.attach(network, "Network");
		display.attach(crimes, "Crimes");
//		display.attach(citizens, "Agents");
		display.attach(officers, "Officers");
		
		
		// ---TIMESTAMP---
		display.attach(new FieldPortrayal2D()
	    {
		    Font font = new Font("SansSerif", 0, 24);  // keep it around for efficiency
		    SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd HH:mm zzz");
		    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
		        {
		        String s = "";
		        if (state !=null) // if simulation has not begun or has finished, indicate this
		            s = state.schedule.getTimestamp("Before Simulation", "Simulation Finished");
		        graphics.setColor(Color.gray);
		        if (state != null){
		        	// specify the timestep here
		        	Date startDate;
					try {
						startDate = ft.parse("2011-03-01 0:00 GMT");
				        Date time = new Date((int)state.schedule.getTime() * 60000 + startDate.getTime());
				        s = ft.format(time);	
					} catch (ParseException e) {
						e.printStackTrace();
					}
		        }

		        graphics.drawString(s, (int)info.clip.x + 10, 
		                (int)(info.clip.y + 10 + font.getStringBounds(s,graphics.getFontRenderContext()).getHeight()));

		        }
		    }, "Time");
		
		displayFrame = display.createFrame();
		c.registerFrame(displayFrame); // register the frame so it appears in the "Display" list
		displayFrame.setVisible(true);
		
		// the hashtag chart visualization
		
/*		dummyChart = new TimeSeriesChartGenerator();
		dummyChart.setTitle("Trending Dummies");
		dummyChart.setXAxisLabel("Time");
		dummyChart.setYAxisLabel("Usage Rate");
		JFrame chartFrame = dummyChart.createFrame(this);
		chartFrame.pack();
		controller.registerFrame(chartFrame);
		*/
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
		EmergentCrimeWithUI gui =  null;
		
		try {
			EmergentCrime lb = new EmergentCrime(System.currentTimeMillis());
			gui = new EmergentCrimeWithUI(lb);
		} catch (Exception ex){
			System.out.println(ex.getStackTrace());
		}
		
		Console console = new Console(gui);
		console.setVisible(true);
	}

	/** Returns the name of the simulation */
	public static String getName() { return "EmergentCrime"; }

	/** Allows for users to modify the simulation using the model tab */
	public Object getSimulationInspectedObject() { return state; }

}