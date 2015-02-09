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
	
	/** prefix uri of the proc inbount port of the appication contorleur to cooridaiton cpu	*/
	protected static final String AC_CIP_PREFIX = "cc-";
	
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
	
	protected Integer prio;
	
	private int marge = 0;
	
	
	public ApplicationControl(
			Integer id, 
			Double time,
			HashMap<String,ArrayList<String>> cpuCoreInboundPortUris,
			Integer prio
			) throws Exception{
		super(true, true) ;

		assert id != null;
		
		this.logId = MessageFormat.format("[  AppC {0} ]", String.format("%04d", id));
		this.id = id ;
		this.timeExp = time;
		this.meanTime = time;
		this.cpuCoreInboundPortUris = cpuCoreInboundPortUris;
		
		
		
		
		//On connecte tous les cpu a AppControl
		this.addRequiredInterface(ControlRequestArrivalI.class);
		int cpt = 0;
		int i =0;
		while(cpt < cpuCoreInboundPortUris.size()){
			//lien cpu <-> coordcpu
			if(cpuCoreInboundPortUris.containsKey("cpu-craip-" + i)){
				ControlCpuOutBoundPort buff = new ControlCpuOutBoundPort(AC_COP_PREFIX + id +"-"+ cpt, this);
				cop.add(buff);
				this.addPort(buff);
				if (AbstractCVM.isDistributed) {
					buff.publishPort() ;
				} else {
					buff.localPublishPort() ;
				}
				buff.doConnection(AC_CIP_PREFIX + i, ControlRequestServiceConnector.class.getCanonicalName());
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
		double pourcent = 10.0;
		double pHz = 0.2;
		
		this.meanTime = (this.meanTime + Double.parseDouble(info)) / 2.0;
			
		System.out.println(logId + " New mean time : " + this.meanTime);
		System.out.println(logId + " Time expected : " + this.timeExp);
	
		//Si la marge est atteinte et on est en / haut dessus du temps cible 
		if (marge % 1 == 0 && (pourcent/100.0 * this.timeExp) + this.timeExp < this.meanTime){
			this.meanTime = Double.parseDouble(info);// on réquilibre la coubre
			//On parcourt les cpu 1 à 1 jusqu'a pouvoir changer la freq d'un coeur ( contraint max + diff 0.5) 
			for(int i = 0; i< cop.size(); i++ ){
				try {
					if(cop.get(i).majClockSpeed(id+"-"+prio , pHz, cpuCoreInboundPortUris.get("cpu-craip-"+cop.get(i).getServerPortURI().split("-")[1]))){
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
				if(i == cop.size() -1){
					System.out.println(logId + " N'a pas pu changer la fréquence des coeurs ");
				}
			}
			
		}else if(marge % 1 == 0 &&  this.timeExp - (pourcent/100.0 * this.timeExp)  > this.meanTime){
			this.meanTime = Double.parseDouble(info);
			for(int i = 0; i< cop.size(); i++ ){
				try {
					if(cop.get(i).majClockSpeed(id+"-"+prio , - pHz, cpuCoreInboundPortUris.get("cpu-craip-"+cop.get(i).getServerPortURI().split("-")[1]))){
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
				if(i == cop.size() -1){
					System.out.println(logId + " N'a pas pu changer la fréquence des coeurs ");
				}
			}
		}
		
		
		marge ++;
	}

}
