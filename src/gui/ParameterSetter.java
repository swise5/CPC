package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ParameterSetter extends JComponent {
	
	SimulationParameters settings = null;
	
	JPanel sliderPanel = new JPanel();
	Box box;

	int numRuns = 1;
	int duration = 1440; // a day


	// BUTTONS
	
	JButton chooseCADButton = new JButton("Select CAD file");
	JButton chooseDirButton = new JButton("Select data directory");

	// SLIDERS
	
	JSlider reportProbSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 10);
	JSlider transportProbSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 10);
	JSlider reportTimeSlider = new JSlider(JSlider.HORIZONTAL, 0, 240, 15);

	// TEXT FIELDS
	
	JTextField numRunsText = new JTextField(3);
	JTextField durationText = new JTextField(3);

	
	JTextField reportProbSliderText = new JTextField(3);
	JTextField transportProbSliderText = new JTextField(3);
	JTextField reportTimeSliderText = new JTextField(3);

	final JFileChooser fileChooser = new JFileChooser("."),
			dirChooser = new JFileChooser(".");

	public ParameterSetter(SimulationParameters params){
		super();
		settings = params;
		
		box = Box.createVerticalBox();
		box.setSize(350, 800);
		box.setAlignmentX(LEFT_ALIGNMENT);
		
		// TODO: set from settings!!!!!!!
	}
	
	public void setupDataInputs() {

		
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
  		box.add(new JLabel("Num runs!"));
  		box.add(numRunsText);

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
  		box.add(new JLabel("Duration!"));
  		box.add(durationText);
  		
		chooseCADButton
				.setToolTipText("The file listing the calls for service. SHOULD INCLUDE DESCRIPTION OF FILE FORMAT!!!");
		chooseCADButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				// Handle open button action.
				if (event.getSource() == chooseCADButton) {
					int returnVal = fileChooser.showOpenDialog(ParameterSetter.this);
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

					int returnVal = dirChooser.showOpenDialog(ParameterSetter.this);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = dirChooser.getSelectedFile();
						settings.dataDir = file.getAbsolutePath();
					}
				}
			}
		});
		
//		add(chooseCADButton);
//		add(chooseDirButton);
		box.add(chooseCADButton);
		box.add(chooseDirButton);

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
		
//		add(sliderPanel);
		box.add(sliderPanel);

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