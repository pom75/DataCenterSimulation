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
		assert	owner.isRequiredInterface(AppControlerInfoInboundPort.class) ;
	}

	@Override
	public boolean updateClockSpeed(Double clockSpeed) throws Exception {
		return ((ControlRequestArrivalI)this.connector).updateClockSpeed(clockSpeed);
	}

	@Override
	public ArrayList<String> getCoresRequestArrivalInboundPortUris()
			throws Exception {
		return ((ControlRequestArrivalI)this.connector).getCoresRequestArrivalInboundPortUris();
	}

	@Override
	public boolean majClockSpeed(Double clockSpeed, ArrayList<String> cpuUri)
			throws Exception {
		return ((ControlRequestArrivalI)this.connector).majClockSpeed(clockSpeed , cpuUri);

	}

}
