package fr.upmc.colins.farm3.dispatcher;

import fr.upmc.colins.farm3.conrolapp.AppControlerInfoInboundPort;
import fr.upmc.components.ComponentI;
import fr.upmc.components.ports.AbstractOutboundPort;

public class InfoOutboundPort
extends		AbstractOutboundPort
implements AppControlerInfoInboundPort{

	public InfoOutboundPort(String uri,
			ComponentI owner) throws Exception {
		super(uri, AppControlerInfoInboundPort.class, owner);
		
		
		assert	uri != null ;
		assert	owner.isRequiredInterface(AppControlerInfoInboundPort.class) ;
	}

	@Override
	public void receiveInfo(String response) throws Exception {
		
		((AppControlerInfoInboundPort)this.connector).receiveInfo(response) ;
		
	}

}
