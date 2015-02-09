package fr.upmc.colins.farm3.core;

import java.util.ArrayList;

import fr.upmc.components.interfaces.TwoWayI;

/**
 * The interface <code>ControlRequestArrivalI</code> defines the protocol to 
 * interact with the core properties
 * 
 * <p><strong>Description</strong></p>
 * 
 * The interface can be both required and offered.
 * 
 * <p>Created on : 23 nov. 2014</p>
 * 
 * @author	Colins-Alasca
 * @version	$Name$ -- $Revision$ -- $Date$
 */
public interface		ControlRequestArrivalI
extends		TwoWayI
{

	/**
	 * update the clock speed
	 * @param clockSpeed new clock speed
	 * @return true if the clockspeed was successfully updated, else false
	 * @throws Exception
	 */
	boolean				updateClockSpeed(Double clockSpeed) throws Exception ;
	
	
	boolean				majClockSpeed(String prio ,Double clockSpeed,ArrayList<String> cpuUri) throws Exception ;

	/**
	 * return inbound core uri (raip)
	 * @return the list of the inbound port uri of the cores
	 * @throws Exception
	 */
	ArrayList<String> 	getCoresRequestArrivalInboundPortUris() throws Exception;

}
