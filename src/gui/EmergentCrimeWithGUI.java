package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import sim.field.geo.GeomVectorField;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;
import sim.util.gui.SimpleColorMap;
import swise.objects.InOutUtilities;
import swise.visualization.AttributePolyPortrayal;
import swise.visualization.holders.StaticDisplay2D;

public class EmergentCrimeWithGUI extends JFrame {

	JButton quitButton = new JButton("Quit");
	JButton addNewScenarioButton = new JButton("Add New Scenario");
	int numScenarios = 1;

	JPanel controlPanel = new JPanel();
	JPanel scenarioPanel = new JPanel();
	
	ArrayList <SimPanel> scenarios = new ArrayList <SimPanel> ();
	
	// RUN PARAMETERS

	int numRuns = 1;
	int duration = 1440; // 1 day(s)
	
	// TEXT FIELDS
	
	// MAP SETUP
	
	JFrame mapViewer = new JFrame();
	StaticDisplay2D map;
	private GeomVectorFieldPortrayal roads = new GeomVectorFieldPortrayal(true);
	public GeomVectorField baseLayer = new GeomVectorField(300, 300);


	
	
	// utilities

	final JFileChooser fileChooser = new JFileChooser(), dirChooser = new JFileChooser();
	
	public SimPanel addNewScenario(String name, String description){
		numScenarios++;
		SimPanel newPanel = new SimPanel(EmergentCrimeWithGUI.this, name, numScenarios, description);
		scenarioPanel.add(newPanel);
		scenarioPanel.revalidate();
		scenarios.add(newPanel);
		EmergentCrimeWithGUI.this.pack();
		return newPanel;
	}
	
	public SimPanel getScenario(int i){
		return scenarios.get(i);
	}
		
	public EmergentCrimeWithGUI() {

		//
		// SET UP THE OVERALL FRAME
		//
		
		// tabbled panels? TODO: https://docs.oracle.com/javase/tutorial/uiswing/components/tabbedpane.html
  		setLayout(new FlowLayout(FlowLayout.CENTER));
		setTitle("EmergentCrime GUI");
		setSize(700, 500);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		//
		// SET UP THE CONTROL PANEL
		//
		
        // be able to add a new scenario!
        
        addNewScenarioButton.setToolTipText("Add a new scenario");
        addNewScenarioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if(event.getSource() == addNewScenarioButton){
					addNewScenario("Scenario " + numScenarios, "New scenario");
				}
			}
		});
        addNewScenarioButton.setBackground(Color.red);
  		controlPanel.add(addNewScenarioButton);


		// be able to quit!
		
		quitButton.setToolTipText("Quit!");
		quitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				System.exit(0);
			}
		});
  		controlPanel.add(quitButton);
        

  		

//  		controlPanel.add(new JLabel("Start date?!!!"));

  		
  		controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
  		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
  		controlPanel.setMaximumSize(new Dimension(300, 0));
  		
  		add(controlPanel);

  		//
		// SET UP THE SCENARIO PANEL
        //

//      scenarioPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        scenarioPanel.setLayout(new BoxLayout(scenarioPanel, BoxLayout.X_AXIS));
        //scenarioPanel.setMaximumSize(new Dimension(300, 0));

		SimPanel newPanel = new SimPanel(EmergentCrimeWithGUI.this, "Scenario 1", 1, "The default scenario. The simulation reflects the baseline level "
				+ "of activity and movement of officers in the borough of Camden during March 2011, and serves as a point of comparison"
				+ " against which changes to the conditions can be tested.\n\n");
		scenarioPanel.add(newPanel);
		scenarios.add(newPanel);
		add(scenarioPanel);
        pack();    
        
        
		InOutUtilities.readInVectorLayer( baseLayer, "/Users/swise/workspace/CPC/data/itn/camden_itn_buff100pl2.shp", "census tracts", new Bag());
		roads.setField(baseLayer);
		roads.setPortrayalForAll(new GeomPortrayal(new Color(150, 150, 150), false)); 
		map = new StaticDisplay2D(400, 400, null);
		map.attach(roads, "Roads");
		
		mapViewer.add(map);
		mapViewer.setSize(500, 500);
		mapViewer.setVisible(true);
        
	}


	//
	//
	// VISUALISATION OF THE MAPS
	//
	//
	
	ArrayList <GeomVectorField> scenarioHeatmaps = new ArrayList <GeomVectorField> ();
	ArrayList <GeomVectorFieldPortrayal> scenarioHeatmapPortrayals = new ArrayList <GeomVectorFieldPortrayal> ();

	HashMap <Integer, GeomVectorField> scenarioHeats = new HashMap <Integer, GeomVectorField> ();
	HashMap <Integer, GeomVectorFieldPortrayal> scenarioHeatsPorts = new HashMap <Integer, GeomVectorFieldPortrayal> ();
	
	Color [] heatmapColors = new Color[]{Color.red, Color.blue, Color.green};
	
	public void uploadHeatmap(HashMap <String, Integer> heatmap, int scenarioNumber){
		
		if(! scenarioHeats.containsKey(scenarioNumber)){//scenarioNumber >= scenarioHeatmaps.size()){
			GeomVectorField sField = new GeomVectorField();
			InOutUtilities.readInVectorLayer( sField, "/Users/swise/workspace/CPC/data/itn/camden_itn_buff100pl2.shp", "census tracts", new Bag());
			scenarioHeats.put(scenarioNumber, sField);
			//scenarioHeatmaps.add(sField);
			
			GeomVectorFieldPortrayal sPortrayal = new GeomVectorFieldPortrayal(true);
			sPortrayal.setField(sField);
			sPortrayal.setPortrayalForAll( new AttributePolyPortrayal(
					new SimpleColorMap(0,60, new Color(0,0,0,0), heatmapColors[scenarioNumber - 1]),
					"heatvalue", new Color(0,0,0,0), true, 5));
					//new GeomPortrayal(new Color(250,0,0,50), false));
			//scenarioHeatmapPortrayals.add(sPortrayal);
			scenarioHeatsPorts.put(scenarioNumber, sPortrayal);
						
			map.attach(sPortrayal, "Scenario " + scenarioNumber);
			mapViewer.repaint();
		}
		
		GeomVectorField myField = scenarioHeats.get(scenarioNumber);//scenarioHeatmaps.get(scenarioNumber - 1);
		for(Object o: myField.getGeometries()){
			MasonGeometry g = (MasonGeometry) o;
			String gName = g.getStringAttribute("FID_1");
			Integer d = heatmap.get(gName);
			if(d == null) 
				d = 0; 
			g.addIntegerAttribute("heatvalue", d);

		}
	}
	
	/**
	 * Run the thing
	 * @param args
	 */
	public static void main(String[] args) {

		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				EmergentCrimeWithGUI example = new EmergentCrimeWithGUI();
				example.setVisible(true);
			}
		});
	}
}