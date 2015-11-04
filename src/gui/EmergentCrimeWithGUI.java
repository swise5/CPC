package gui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import sim.EmergentCrime;
import sim.field.geo.GeomVectorField;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.util.Bag;
import swise.objects.InOutUtilities;
import swise.visualization.holders.StaticDisplay2D;

public class EmergentCrimeWithGUI extends JFrame {

	JButton quitButton = new JButton("Quit");
	JButton addNewScenarioButton = new JButton("Add New Scenario");
	int scenarios = 1;

	JPanel controlPanel = new JPanel();
	JPanel scenarioPanel = new JPanel();
	
	// RUN PARAMETERS

	int numRuns = 5;
	int duration = 1440; // a day
	
	// TEXT FIELDS
	
	JTextField numRunsText = new JTextField(3);
	JTextField durationText = new JTextField(3);

	// MAP SETUP
	
	JFrame mapViewer = new JFrame();
	StaticDisplay2D map;
	private GeomVectorFieldPortrayal roads = new GeomVectorFieldPortrayal(true);
	public GeomVectorField baseLayer = new GeomVectorField(100, 100);


	
	
	// utilities

	final JFileChooser fileChooser = new JFileChooser(), dirChooser = new JFileChooser();
	
	
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
					scenarios++;
					SimPanel newPanel = new SimPanel(EmergentCrimeWithGUI.this, "Scenario " + scenarios);
					scenarioPanel.add(newPanel);
					scenarioPanel.revalidate();
					EmergentCrimeWithGUI.this.pack();
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
        
  		// set the number of runs to do
  		
  		numRunsText.setText("" + numRuns);
		numRunsText.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent event) {
				if(event.getSource() == numRunsText){
					int textVal = Integer.parseInt(numRunsText.getText());
					numRuns = textVal;
				}
				
			}
			
		});
  		controlPanel.add(new JLabel("Num runs!!!"));
  		controlPanel.add(numRunsText);

  		// set the desired simulation duration
  		
  		durationText.setText("" + duration);
  		durationText.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent event) {
				if(event.getSource() == durationText){
					int textVal = Integer.parseInt(durationText.getText());
					duration = textVal;
				}
				
			}
			
		});
  		controlPanel.add(new JLabel("Duration!!!"));
  		controlPanel.add(durationText);
  		
  		

  		controlPanel.add(new JLabel("Start date?!!!"));

  		
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

		SimPanel newPanel = new SimPanel(EmergentCrimeWithGUI.this, "Scenario 1");
		scenarioPanel.add(newPanel);
		add(scenarioPanel);
        pack();    
        
        
		InOutUtilities.readInVectorLayer( baseLayer, "/Users/swise/workspace/CPC/data/itn/camden_itn_buff100pl2.shp", "census tracts", new Bag());
		roads.setField(baseLayer);
		roads.setPortrayalForAll(new GeomPortrayal(new Color(150, 150, 150), false)); 
		map = new StaticDisplay2D(150, 150, null);
		map.attach(roads, "Roads");
		//add(map);
		
		mapViewer.add(map);
		mapViewer.setVisible(true);

        
	}

	
/**	JButton chooseCADButton = new JButton("Select CAD file");
	JButton chooseDirButton = new JButton("Select data directory");
	JButton runSimulationButton = new JButton("RUN THE SIMULATION");

	JSlider reportProbSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 10);

	SimulationParameters settings = new SimulationParameters();;

	private JPanel parameterSetting() {

		// IDENTIFY THE DATA INPUTS TO THIS MODEL

		chooseCADButton.setToolTipText("The file listing the calls for service. SHOULD INCLUDE DESCRIPTION OF FILE FORMAT!!!");
		chooseCADButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
			    //Handle open button action.
			    if (event.getSource() == chooseCADButton) {
			        int returnVal = fileChooser.showOpenDialog(EmergentCrimeWithGUI.this);
			        if (returnVal == JFileChooser.APPROVE_OPTION) {
			            File file = fileChooser.getSelectedFile();
			            settings.cadFile = file.getAbsolutePath();
			        }
			   }
			}
		});
		
		dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooseDirButton.setToolTipText("The directory in which the relevant datafiles are stored");
		chooseDirButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
			    //Handle open button action.
			    if (event.getSource() == chooseDirButton) {
			    	
			        int returnVal = dirChooser.showOpenDialog(EmergentCrimeWithGUI.this);
			        if (returnVal == JFileChooser.APPROVE_OPTION) {
			            File file = dirChooser.getSelectedFile();
			            settings.dataDir = file.getAbsolutePath();
			        }
			   }
			}
		});
		
		// SLIDERS FOR SETTABLE PARAMETERS

		reportProbSlider.addChangeListener(new ChangeListener(){

			@Override
			public void stateChanged(ChangeEvent event) {
				// TODO Auto-generated method stub
				if(event.getSource() == reportProbSlider){
					settings.reportProb = reportProbSlider.getValue() / 100.; 
				}
			}
		});

		//Turn on labels at major tick marks.
		reportProbSlider.setMajorTickSpacing(20);
		reportProbSlider.setMinorTickSpacing(5);
		reportProbSlider.setPaintTicks(true);
		reportProbSlider.setPaintLabels(true);
		
		//
		// RUNNING THE SIMULATION!!!!
		//
		
		runSimulationButton.setToolTipText("Run the simulation with the specified parameters");
		runSimulationButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
			    //Handle open button action.
			    if (event.getSource() == runSimulationButton) {
			    	EmergentCrime ec = new EmergentCrime(settings.seed);
					ec.taskingTypeBeingStudied = settings.rolesEnabled;
					if(ec.taskingTypeBeingStudied == 0)
						ec.rolesDisabled = true;
					
					ec.dirName = settings.dataDir + "/";
					ec.fileName = settings.filename;
					ec.param_reportProb = settings.reportProb;
					ec.param_transportRequestProb = settings.transportProb;
					ec.param_reportTimeCommitment = settings.reportTime;
					ec.cadFile = settings.cadFile;

			    	ec.start();
			    	while(ec.schedule.getTime() < 10){
			    		ec.schedule.step(ec);
			    	}
			    	ec.finish();
			   }
			}
		});
		
		//
		// Add a new panel!
		//

		
		// Set up the panel to hold the buttons

		JPanel setupPanel = new JPanel();

        setupPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setupPanel.setLayout(new BoxLayout(setupPanel, BoxLayout.Y_AXIS));
        setupPanel.setMaximumSize(new Dimension(300, 0));

        setupPanel.add(new JLabel("Select Parameters for the Simulation"));
        setupPanel.add(chooseCADButton);
        setupPanel.add(chooseDirButton);
        setupPanel.add(reportProbSlider);
        setupPanel.add(runSimulationButton);
        
        return setupPanel;        
	}
	*/
	
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