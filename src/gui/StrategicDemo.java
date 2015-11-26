package gui;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class StrategicDemo extends JFrame {
	
	EmergentCrimeWithGUI gui;
	
	JPanel panel = new JPanel();
	
	JButton quitButton = new JButton("Quit");
	JButton changeInResourcesButton = new JButton("Change in Resources");
	JButton changeInCrimeButton = new JButton("Change in Crime Level");
	JButton kingsCrossButton = new JButton("Major Incident");
	
	public StrategicDemo (){
		
  		//setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
  		setLayout(new FlowLayout(FlowLayout.CENTER));

		setTitle("Strategic Planning Demo");
		setSize(400, 200);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		JLabel instructions = new JLabel();
		instructions.setText("Select from the following preestablished scenarios:");
		panel.add(instructions);
		
		// Scenario: Change in Resources
		
  		changeInResourcesButton.setToolTipText("Vary the levels of resources available to the borough");
  		changeInResourcesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if(event.getSource() == changeInResourcesButton){
					gui = new EmergentCrimeWithGUI();
					
					gui.addNewScenario("Scenario 2: < Resource", "Scenario: a number of patrolling vehicles are lost due to age or accident. "
							+ "The simulation explores how this decrease in the pool of available vehicles influences the impact on response times,"
							+ " activities, and area coverage.\n\n");
					SimPanel sp = gui.getScenario(1);
					sp.settings.rolesEnabled = 2; // set for fewer resources
					
					gui.setVisible(true);
					StrategicDemo.this.setVisible(false);
					//gui.addNewScenario();
				}
			}
		});
  		panel.add(changeInResourcesButton);

		// Scenario: Change in Crime
		
  		changeInCrimeButton.setToolTipText("Vary the levels of crime experienced by the borough");
  		changeInCrimeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if(event.getSource() == changeInCrimeButton){
					gui = new EmergentCrimeWithGUI();
					
					gui.addNewScenario("Scenario 2: > Crime", "Scenario: The number of crimes occuring in the borough increases. The simulation explores how"
							+ " the current number of vehicles and set of officer responsibilities will translate into this new environment.\n\n");
					SimPanel sp = gui.getScenario(1);
					sp.settings.cadFile = "/Users/swise/workspace/CPC/data/CAD/cadMarch2011_INCREASE.txt"; // set for more crime
					
					gui.setVisible(true);
					StrategicDemo.this.setVisible(false);
					//gui.addNewScenario();
				}
			}
		});
  		panel.add(changeInCrimeButton);

  		
		// Scenario: Change in Crime
		
  		kingsCrossButton.setToolTipText("Vary the levels of crime experienced by the borough");
  		kingsCrossButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if(event.getSource() == kingsCrossButton){
					gui = new EmergentCrimeWithGUI();
					
					gui.addNewScenario("Scenario 2: Incident", "Scenario: There is a major incident near Kings Cross. Officers are diverted from their normal"
							+ "activities in order to cover the incident.\n\n");
					SimPanel sp = gui.getScenario(1);
					sp.settings.cadFile = "/Users/swise/workspace/CPC/data/CAD/cadMarch2011_KINGSCROSS.txt"; // set for more crime
					
					gui.setVisible(true);
					StrategicDemo.this.setVisible(false);
					//gui.addNewScenario();
				}
			}
		});
  		panel.add(kingsCrossButton);

		// be able to quit!
		
		quitButton.setToolTipText("Quit!");
		quitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				System.exit(0);
			}
		});
  		panel.add(quitButton);

  		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
  		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
  		panel.setMaximumSize(new Dimension(300, 0));

  		add(panel);
	}
	
	public static void main(String [] args){
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				StrategicDemo example = new StrategicDemo();
				example.setVisible(true);
			}
		});
	}
	
}