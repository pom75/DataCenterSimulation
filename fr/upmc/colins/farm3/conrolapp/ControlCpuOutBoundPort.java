package fr.upmc.colins.farm3.conrolapp;

import java.util.ArrayList;

import fr.upmc.colins.farm3.core.ControlRequestArrivalI;
import fr.upmc.components.ComponentI;
import fr.upmc.components.ports.AbstractOutboundPort;

public class ControlCpuOutBoundPort
extends AbstractOutboundPort
implements ControlRequestArrivalI {

	public ControlCpuOutBoundPort(
			String uri,
			ComponentI owner
			) throws Exception {
		super(uri, ControlRequestArrivalI.class, owner);
		
		assert	uri != null ;
		assert	owner.isRequiredInterface(ControlRequestArrivalI.class) ;
	}

	@Override
	public boolean updateClockSpeed(Double clockSpeed,ArrayList<String> listCore) throws Exception {
		return ((ControlRequestArrivalI)this.connector).updateClockSpeed(clockSpeed, listCore);
	}

	@Override
	public ArrayList<String> getCoresRequestArrivalInboundPortUris()
			throws Exception {
		return ((ControlRequestArrivalI)this.connector).getCoresRequestArrivalInboundPortUris();
	}

	
	
	@Override
	public boolean majClockSpeed(String prio, Double clockSpeed, ArrayList<String> cpuUri)
			throws Exception {
		return ((ControlRequestArrivalI)this.connector).majClockSpeed(prio, clockSpeed , cpuUri);

	}

}