package fr.upmc.colins.farm3.conrolapp;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;

import fr.upmc.colins.farm3.connectors.ControlRequestServiceConnector;
import fr.upmc.colins.farm3.core.ControlRequestArrivalI;
import fr.upmc.components.AbstractComponent;
import fr.upmc.components.cvm.AbstractCVM;

public class ApplicationControl extends AbstractComponent {
	
	/** log constant	 													*/
	protected String logId;
	protected Integer id;
	private ACInfoArrivalInboundPort iip;
	
	/** prefix uri of the information inbound port of the appication contorleur	*/
	protected static final String AC_IIP_PREFIX = "ac-iip-";
	
	/** prefix uri of the proc inbount port of the appication contorleur to cpu	*/
	protected static final String AC_CIP_PREFIX = "cpu-craip-";
	
	/** prefix uri of the controleur outbound port of the appication contorleur to cpu	*/
	protected static final String AC_COP_PREFIX = "appc-cop-";
	
	/** Liste des URI CPU / coeur uniquement dans le cpu et bond a l'app */
	protected HashMap<String,ArrayList<String>> cpuCoreInboundPortUris = new HashMap<String, ArrayList<String>>();
	
	/** mean time 															*/
	protected Double meanTime = -1.0;
	
	/** Time expected by the app 															*/
	protected Double timeExp ;
	
	/** outbound ports to the cpu											*/
	protected ArrayList<ControlCpuOutBoundPort> cop = new ArrayList<ControlCpuOutBoundPort>();
	
	
	public ApplicationControl(Integer id, 
			Double time,
			HashMap<String,ArrayList<String>> cpuCoreInboundPortUris
			) throws Exception{
		super(true, true) ;

		assert id != null;
		
		this.logId = MessageFormat.format("[  AppC {0} ]", String.format("%04d", id));
		this.id = id ;
		this.timeExp = time;
		this.meanTime = time;
		this.cpuCoreInboundPortUris = cpuCoreInboundPortUris;
		
		
		this.addRequiredInterface(ControlRequestArrivalI.class);
		
		//On connecte tous les cpu a AppControl
		int cpt = 0;
		int i =0;
		while(cpt < cpuCoreInboundPortUris.size()){
			if(cpuCoreInboundPortUris.containsKey(AC_CIP_PREFIX + i)){
				ControlCpuOutBoundPort buff = new ControlCpuOutBoundPort(AC_COP_PREFIX + id +"-"+ cpt, this);
				cop.add(buff);
				this.addPort(buff);
				if (AbstractCVM.isDistributed) {
					buff.publishPort() ;
				} else {
					buff.localPublishPort() ;
				}
				buff.doConnection(AC_CIP_PREFIX + i, ControlRequestServiceConnector.class.getCanonicalName());
				buff.majClockSpeed(0.5, cpuCoreInboundPortUris.get(AC_CIP_PREFIX + i));
				cpt++;
			}
			i++;
		}
		
		
		
		
		// inbound port for info arrival
		this.addOfferedInterface(AppControlerInfoInboundPort.class) ;
		this.iip = new ACInfoArrivalInboundPort(AC_IIP_PREFIX+id, this) ;
		this.addPort(this.iip) ;
		if (AbstractCVM.isDistributed) {
			this.iip.publishPort() ;
		} else {
			this.iip.localPublishPort() ;
		}
		
		System.out.println(logId + " Crée ");
	}


	public void infoArrivalEvent(String info) {
		
		this.meanTime = (this.meanTime + Double.parseDouble(info)) / 2.0;
			
		System.out.println(logId + " New mean time : " + this.meanTime);
		System.out.println(logId + " Time expected : " + this.timeExp);
	}

}
