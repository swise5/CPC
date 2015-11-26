package gui;

import gui.SimulationParameters;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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

public class SimPanel extends JPanel {

	EmergentCrimeWithGUI parent = null;
	String myName = "Scenario 1";
	int myNumber = -1;
	
	// PANELS
	
	Box box;

	// BUTTONS
	
	JButton runSimulationButton = new JButton("RUN SIM");

	// OTHER OBJECTS
	
	JTextArea log = new JTextArea();
	
	// SIMULATION SETUP

	SimulationParameters settings = new SimulationParameters();

	// UTILITIES

	final JFileChooser fileChooser = new JFileChooser("."),
			dirChooser = new JFileChooser(".");

	public void setName(String s){
		myName = s;
	}
	
	public SimPanel(EmergentCrimeWithGUI myParent) {
		this(myParent, "Scenario: Default Scenario", 1, "The default scenario.");
	}

	public SimPanel(EmergentCrimeWithGUI myParent, String name, int number, String description) {

		super();

		parent = myParent;
		myName = name;
		myNumber = number;
		
		box = Box.createVerticalBox();
		box.setSize(350, 800);
		box.setAlignmentX(LEFT_ALIGNMENT);
		
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setPreferredSize(new Dimension(350, 800));
		setBackground(Color.white);
//		setAlignmentX(1);

		JTextArea nameLabel = new JTextArea(myName);
		//nameLabel.setText(myName);
		
		nameLabel.setMaximumSize(new Dimension(325,50));
		nameLabel.setWrapStyleWord(true);
		nameLabel.setEditable(false);
		nameLabel.setFocusable(false);
		//nameLabel.setBackground(Color.cyan);
		nameLabel.setFont(new Font(nameLabel.getFont().getName(), Font.PLAIN, 24));
		nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
//		add(nameLabel);
		box.add(nameLabel);
		

/*		JLabel nameLabel = new JLabel(myName);
		nameLabel.setFont(new Font(nameLabel.getFont().getName(), Font.PLAIN, 24));
		nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
//		add(nameLabel);
		box.add(nameLabel);
		
	
		JTextArea descriptionLabel = new JTextArea(6,20);
		descriptionLabel.setText(description);
		//paramsLabel.setBounds(5, 100, 800, 100);
		descriptionLabel.setMinimumSize(new Dimension(320, 50));
		descriptionLabel.setMaximumSize(new Dimension(650, 50));
		descriptionLabel.setWrapStyleWord(true);
		descriptionLabel.setEditable(false);
		descriptionLabel.setAlignmentX(1);
//		add(paramsLabel);
		box.add(descriptionLabel);
*/
//		setupDataInputs();
//		setupSliders();

		attachSimulation();
//		add(new JLabel("Log", JLabel.LEFT));
		JLabel logLabel = new JLabel("Log", JLabel.LEFT);
		logLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		//box.add(logLabel);
		//add(log);
		addLog();
		log.append(description);
		log.append("======Results======\n");
		add(box, BorderLayout.NORTH);
		// VISUALISE THE RESULTS
	}
	
	/**
	 * Add a logger which tracks what the user has done for this particular parameter setting
	 */
	public void addLog(){
		log.setMinimumSize(new Dimension(300,400));    

		log.setLineWrap(true);
		log.setWrapStyleWord(true);
		log.setEditable(false);
		log.setVisible(true);

	    JScrollPane scroll = new JScrollPane (log);
	    scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	          scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

//	    add(scroll);
	  	    box.add(scroll);
	}

	ArrayList <HashMap <String, Integer>> results = new ArrayList <HashMap <String, Integer>> ();
	ArrayList <int []> statusResults = new ArrayList <int []> ();
	ArrayList <double []> responseTimeResults = new ArrayList <double []>(); 
	
	
	public void attachSimulation() {
		//
		// RUNNING THE SIMULATION!!!!
		//

		runSimulationButton.setToolTipText("Run the simulation with the specified parameters");
		runSimulationButton.setMinimumSize(new Dimension(200,20));

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
						statusResults.add(ec.statusRecord);
						responseTimeResults.add(ec.getDespatch().getAverageResponseTimes());
						results.add(ec.getHeatmap());
						
					}
					
					log.append("Finished running simulations!\n");
					log.append("\nTotal time spent on Status...\n");
					int [] statusResult = statusResults.get(0);
					//for(int i: new int[]{2, 3, 5, 6, 8, 9}){
					for(int i: new int[]{2, 5, 6, 8}){
						log.append(" > " + i + ": " + statusResult[i] + " minutes\n");
					}
					
					log.append("\nAverage response times for...\n");
					String [] responseNames = new String [] {" Immediate", " Severe", " Extended"};
					for(int i = 0; i <= 2; i++){
						String formatMe = String.format("%.1f", responseTimeResults.get(0)[i]);
						log.append(" > " + formatMe + " minutes - " + responseNames[i] + "\n");
					}
					
					log.append("\nHeatmap uploaded to visualisation...\n");
					log.repaint();
					
					parent.uploadHeatmap(results.get(0), myNumber);
				}
			}
		});
		
		//add(runSimulationButton);
		box.add(runSimulationButton);
	}

	// IDENTIFY THE DATA INPUTS TO THIS MODEL



}