package fr.upmc.colins.farm3.connectors;

import java.io.Serializable;

import fr.upmc.colins.farm3.conrolapp.AppControlerInfoInboundPort;
import fr.upmc.components.connectors.AbstractConnector;

public class InfoControlAppServiceConnector
extends		AbstractConnector
implements AppControlerInfoInboundPort, Serializable{

	@Override
	public void receiveInfo(String response) throws Exception {
		
		((AppControlerInfoInboundPort)this.offering).receiveInfo(response) ;
		
	}

}
