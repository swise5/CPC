package objects;

import java.util.HashMap;
import java.util.TreeSet;

import sim.EmergentCrime;
import sim.EmergentCrime.CallEvent;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;

/**
 * The FirstContact object provides the interface between the incoming calls and the CAD system, filtering and 
 * forwarding information as appropriate.
 *  
 * @author swise
 *
 */
public class FirstContact implements Steppable {

	private static final long serialVersionUID = 1L;
	
	EmergentCrime world = null;
	TreeSet <CallEvent> urgent_CAD;
	TreeSet <CallEvent> extended_CAD;
	
	public FirstContact(EmergentCrime world){
		this.world = world;
	}
	
	public void setUrgentCAD(TreeSet <CallEvent> CAD){
		this.urgent_CAD = CAD;
	}
	
	public void setExtendedCAD(TreeSet <CallEvent> extended_CAD){
		this.extended_CAD = extended_CAD;
	}
	
	@Override
	public void step(SimState state) {

		
	}

	public int receiveCall(Agent caller, CallEvent call){
		if(call.getLocation() != null){
			
			if(call.getGrade() == 3) // it was redirected - no need to take any action
				return 0;
			if(call.getGrade() == 2 && !world.rolesDisabled)
				extended_CAD.add(call);
			else
				urgent_CAD.add(call);

		}
		return 0;
	}
}