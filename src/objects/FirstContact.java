package objects;

import java.util.HashMap;
import java.util.TreeSet;

import sim.EmergentCrime;
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
	TreeSet <CallEvent> CAD;
	
	public FirstContact(EmergentCrime world){
		this.world = world;
	}
	
	public void setUrgentCAD(TreeSet <CallEvent> CAD){
		this.CAD = CAD;
	}
	
	@Override
	public void step(SimState state) {

		
	}

	public int receiveCall(Agent caller, CallEvent call){
		if(call.getLocation() != null){
			
			if(call.getGrade() == 3) // it was redirected - no need to take any action
				return 0;
			if(!world.rolesDisabled)
				CAD.add(call);

		}
		return 0;
	}
}