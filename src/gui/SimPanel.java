package gui;

import gui.SimulationParameters;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import sim.EmergentCrime;
import sim.display.Display2D;
import sim.field.geo.GeomVectorField;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.util.Bag;
import swise.objects.InOutUtilities;
import swise.visualization.holders.StaticDisplay2D;

public class SimPanel extends JPanel {

	EmergentCrimeWithGUI parent = null;
	String myName = "Scenario 1";
	
	// PANELS
	
	JPanel sliderPanel = new JPanel();
	
	// BUTTONS
	
	JButton chooseCADButton = new JButton("Select CAD file");
	JButton chooseDirButton = new JButton("Select data directory");
	JButton runSimulationButton = new JButton("RUN THE SIMULATION");

	// SLIDERS
	
	JSlider reportProbSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 10);
	JSlider transportProbSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 10);
	JSlider reportTimeSlider = new JSlider(JSlider.HORIZONTAL, 0, 240, 15);

	// TEXT FIELDS
	
	JTextField reportProbSliderText = new JTextField(3);
	JTextField transportProbSliderText = new JTextField(3);
	JTextField reportTimeSliderText = new JTextField(3);

	JTextArea log = new JTextArea();
	
	// SIMULATION SETUP

	SimulationParameters settings = new SimulationParameters();

	// UTILITIES

	final JFileChooser fileChooser = new JFileChooser(),
			dirChooser = new JFileChooser(); // TODO: set up so it starts from this directory

	
	
	public SimPanel(EmergentCrimeWithGUI myParent) {
		this(myParent, "Scenario X");
	}

	public SimPanel(EmergentCrimeWithGUI myParent, String name) {

		super();

		parent = myParent;
		myName = name;
		
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setPreferredSize(new Dimension(350, 800));
		setBackground(Color.cyan);

		add(new JLabel(myName));
		
		add(new JLabel("Select Parameters for the Simulation"));

		setupDataInputs();
		setupSliders();

		attachSimulation();
		add(new JLabel("Log"));
		//add(log);
		addLog();
		// VISUALISE THE RESULTS
	}
	
	/**
	 * Add a logger which tracks what the user has done for this particular parameter setting
	 */
	public void addLog(){
		log.setSize(400,400);    

		log.setLineWrap(true);
		log.setEditable(false);
		log.setVisible(true);

	    JScrollPane scroll = new JScrollPane (log);
	    scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	          scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

	    add(scroll);
	}

	public void attachSimulation() {
		//
		// RUNNING THE SIMULATION!!!!
		//

		runSimulationButton.setToolTipText("Run the simulation with the specified parameters");
		runSimulationButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				// Handle open button action.
				if (event.getSource() == runSimulationButton) {

					for(int i = 0; i < parent.numRuns; i++){
						
						log.append("Beginning simulation run " + (i + 1) + " of " + parent.numRuns + "...\n");
						
						EmergentCrime ec = new EmergentCrime(settings.seed + i);
						ec.taskingTypeBeingStudied = settings.rolesEnabled;
						if (ec.taskingTypeBeingStudied == 0)
							ec.rolesDisabled = true;

						ec.dataDirName = settings.dataDir + "/";
						ec.fileName = settings.filename;
						ec.param_reportProb = settings.reportProb;
						ec.param_transportRequestProb = settings.transportProb;
						ec.param_reportTimeCommitment = settings.reportTime;
						ec.cadFile = settings.cadFile;

						ec.start();
						while (ec.schedule.getTime() < parent.duration) {
							ec.schedule.step(ec);
						}
						log.append("Writing to file...\n");
						log.repaint();
						ec.finish();
					}
					
					log.append("Finished!\n");

				}
			}
		});
		
		add(runSimulationButton);
	}

	// IDENTIFY THE DATA INPUTS TO THIS MODEL
	public void setupDataInputs() {

		chooseCADButton
				.setToolTipText("The file listing the calls for service. SHOULD INCLUDE DESCRIPTION OF FILE FORMAT!!!");
		chooseCADButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				// Handle open button action.
				if (event.getSource() == chooseCADButton) {
					int returnVal = fileChooser.showOpenDialog(SimPanel.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = fileChooser.getSelectedFile();
						settings.cadFile = file.getAbsolutePath();
					}
				}
			}
		});

		dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooseDirButton
				.setToolTipText("The directory in which the relevant datafiles are stored");
		chooseDirButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				// Handle open button action.
				if (event.getSource() == chooseDirButton) {

					int returnVal = dirChooser.showOpenDialog(SimPanel.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = dirChooser.getSelectedFile();
						settings.dataDir = file.getAbsolutePath();
					}
				}
			}
		});
		
		add(chooseCADButton);
		add(chooseDirButton);

	}


	/**
	 * Set up the sliders for the parameters
	 */
	public void setupSliders() {

		// ----------- PANEL SETUP ----------
		
//		sliderPanel.setSize(new Dimension(300,600));
		
		// ------------ REPORT % ------------
		
		// Turn on labels at major tick marks.
		reportProbSlider.setMajorTickSpacing(20);
		reportProbSlider.setMinorTickSpacing(5);
		reportProbSlider.setPaintTicks(true);
		reportProbSlider.setPaintLabels(true);
		reportProbSlider.setSnapToTicks(true);

		reportProbSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				if (event.getSource() == reportProbSlider) {
					settings.reportProb = reportProbSlider.getValue() / 100.;
					reportProbSliderText.setText("" + reportProbSlider.getValue());
				}
			}
		});

		reportProbSliderText.setText("" + (int)(settings.reportProb * 100));
		reportProbSliderText.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent event) {
				if(event.getSource() == reportProbSliderText){
					int textVal = Integer.parseInt(reportProbSliderText.getText());
					settings.reportProb = (textVal / 100.);
					reportProbSlider.setValue(textVal);
				}
				
			}
			
		});

		addSlider(reportProbSlider, reportProbSliderText, "Report %");

		// ------------ REPORT TIME ------------
		
		// Turn on labels at major tick marks.
		reportTimeSlider.setMajorTickSpacing(60);
		reportTimeSlider.setMinorTickSpacing(15);
		reportTimeSlider.setPaintTicks(true);
		reportTimeSlider.setPaintLabels(true);
		reportTimeSlider.setSnapToTicks(true);

		reportTimeSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				if (event.getSource() == reportTimeSlider) {
					settings.reportTime = reportTimeSlider.getValue();
					reportTimeSliderText.setText("" + reportTimeSlider.getValue());
				}
			}
		});

		reportTimeSliderText.setText("" + settings.reportTime);
		reportTimeSliderText.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent event) {
				if(event.getSource() == reportTimeSliderText){
					int textVal = Integer.parseInt(reportTimeSliderText.getText());
					settings.reportTime = textVal;
					reportTimeSlider.setValue(textVal);
				}
				
			}
			
		});

		addSlider(reportTimeSlider, reportTimeSliderText, "Report Time");
		
		// ------------ TRANSPORT % ------------
		
		// Turn on labels at major tick marks.
		transportProbSlider.setMajorTickSpacing(20);
		transportProbSlider.setMinorTickSpacing(5);
		transportProbSlider.setPaintTicks(true);
		transportProbSlider.setPaintLabels(true);
		transportProbSlider.setSnapToTicks(true);

		transportProbSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent event) {
				if (event.getSource() == transportProbSlider) {
					settings.transportProb = transportProbSlider.getValue() / 100.;
					transportProbSliderText.setText("" + transportProbSlider.getValue());
				}
			}
		});

		transportProbSliderText.setText("" + (int)(settings.transportProb * 100));
		transportProbSliderText.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent event) {
				if(event.getSource() == transportProbSliderText){
					int textVal = Integer.parseInt(transportProbSliderText.getText());
					settings.transportProb = (textVal / 100.);
					transportProbSlider.setValue(textVal);
				}
				
			}
			
		});

		addSlider(transportProbSlider, transportProbSliderText, "Transport %");
		
		// -------- PANEL ADDING -----------
		
		add(sliderPanel);

	}

	/**
	 * Helper function
	 * @param s - the slider
	 * @param t - the textfield with the value
	 * @param description - name of this field
	 */
	public void addSlider(JSlider s, JTextField t, String description) {
		JPanel panel = new JPanel();
		panel.add(s);
		panel.add(t);
		panel.add(new JLabel(description));
		sliderPanel.add(panel);
	}

}