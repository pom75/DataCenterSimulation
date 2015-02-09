package fr.upmc.colins.farm3.conrolapp;

import fr.upmc.components.interfaces.TwoWayI;

public interface AppControlerInfoInboundPort extends TwoWayI {
	
	void receiveInfo(String response) throws Exception;

}
