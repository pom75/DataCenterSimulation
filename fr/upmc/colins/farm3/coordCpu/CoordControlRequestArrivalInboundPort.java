package fr.upmc.colins.farm3.coordCpu;

import java.util.ArrayList;

import fr.upmc.colins.farm3.core.ControlRequestArrivalI;
import fr.upmc.components.ComponentI;
import fr.upmc.components.ComponentI.ComponentService;
import fr.upmc.components.ports.AbstractInboundPort;

/**
 * The class <code>CpuControlRequestArrivalInboundPort</code> implements the inbound port
 * for a cpu receiving control request from other components.
 *
 * <p><strong>Description</strong></p>
 * 
 * The port implements the <code>ControlRequestArrivalI</code> interface as offered
 * and upon a call, passes it to the owner component that must also implement
 * the method <code>updateClockSpeed</code>.
 * 
 * <p>Created on : 23 nov. 2014</p>
 * 
 * @author	Colins-Alasca
 * @version	$Name$ -- $Revision$ -- $Date$
 */
public class			CoordControlRequestArrivalInboundPort
extends		AbstractInboundPort
implements	ControlRequestArrivalI
{
	private static final long serialVersionUID = 1L;

	/**
	 * create an inbound port.
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	uri != null && owner != null
	 * pre	owner.isOfferedInterface(ControlRequestArrivalI.class)
	 * post	true			// no postcondition.
	 * </pre>
	 *
	 * @param uri			URI of the port.
	 * @param owner			owner component of the port.
	 * @throws Exception
	 */
	public				CoordControlRequestArrivalInboundPort(
		String uri,
		ComponentI owner
		) throws Exception
	{
		super(uri, ControlRequestArrivalI.class, owner) ;

		assert	uri != null && owner != null ;
		assert	owner.isOfferedInterface(ControlRequestArrivalI.class) ;
	}
	

	@Override
	public boolean updateClockSpeed(Double clockSpeed, ArrayList<String> listCore) throws Exception {
		final CooridationCoreInCPU fc = (CooridationCoreInCPU) this.owner ;
		final Double fcs = clockSpeed ;
		return fc.updateClockSpeed(fcs, listCore);
	}


	@Override
	public ArrayList<String> getCoresRequestArrivalInboundPortUris() throws Exception {
		final CooridationCoreInCPU fc = (CooridationCoreInCPU) this.owner;
		ArrayList<String> res = fc
				.handleRequestSync(new ComponentService<ArrayList<String>>() {
					@Override
					public ArrayList<String> call() throws Exception {
						return fc.getCoresRequestArrivalInboundPortUris();
					}
				});

		return res;
	}


	@Override
	public boolean majClockSpeed(String prio, Double clockSpeed, ArrayList<String> cpuUri)
			throws Exception {
		final CooridationCoreInCPU fc = (CooridationCoreInCPU) this.owner ;
		final Double fcs = clockSpeed ;
		final ArrayList<String> cpuU = cpuUri ;
		return fc.majClockSpeed(prio, fcs,cpuU);
	}

}
