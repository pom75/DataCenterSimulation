package fr.upmc.colins.farm3.coordCpu;

import java.util.ArrayList;

import fr.upmc.colins.farm3.connectors.ControlRequestServiceConnector;
import fr.upmc.colins.farm3.conrolapp.ControlCpuOutBoundPort;
import fr.upmc.colins.farm3.core.ControlRequestArrivalI;
import fr.upmc.components.AbstractComponent;
import fr.upmc.components.cvm.AbstractCVM;

public class CooridationCoreInCPU extends AbstractComponent {

	protected String logId;
	/** liste des puissance de coeur									 */
	protected ArrayList<Double> coreFre = new ArrayList<Double>();
	/** inbound por pour se connecté sur le proc 						 */
	protected String cpuUriInboundPort;
	protected static final String CC_PREFIX = "cc-";
	protected static final String CC_PREFIX_OUT = "cco-";
	/** id du CC = au id CPU */
	protected Integer id ;
	/** port du CC vers le CPU */
	protected CoordControlRequestArrivalInboundPort ccobp ;
	//cf nom varibale
	protected ControlCpuOutBoundPort incpp;
	
	public CooridationCoreInCPU(
			Integer id,
			ArrayList<Double> coreFre,
			String uriCPU
			) throws Exception{
		super(true, true);
		this.id = id;
		this.coreFre = coreFre;
		this.cpuUriInboundPort = uriCPU;
		
		//On connecte le CC au CPU 
		this.addOfferedInterface(ControlRequestArrivalI.class);
		this.addRequiredInterface(ControlRequestArrivalI.class);
		
		
		
		ccobp = new CoordControlRequestArrivalInboundPort(CC_PREFIX + id , this);
		this.addPort(ccobp);
		if (AbstractCVM.isDistributed) {
			this.ccobp.publishPort() ;
		} else {
			this.ccobp.localPublishPort() ;
		}
		
		incpp = new ControlCpuOutBoundPort(CC_PREFIX_OUT + id, this);
		this.addPort(incpp);
		if (AbstractCVM.isDistributed) {
			incpp.publishPort() ;
		} else {
			incpp.localPublishPort() ;
		}
		incpp.doConnection(cpuUriInboundPort, ControlRequestServiceConnector.class.getCanonicalName());
		
	}
	
	public boolean majClockSpeed(String prio, Double fcs, ArrayList<String> listCore) throws Exception {
		return incpp.majClockSpeed(prio, fcs, listCore);
		
	}

	public boolean updateClockSpeed(Double fcs) {
		// TODO Auto-generated method stub
		return false;
	}

	public ArrayList<String> getCoresRequestArrivalInboundPortUris() {
		// TODO Auto-generated method stub
		return null;
	}
}
