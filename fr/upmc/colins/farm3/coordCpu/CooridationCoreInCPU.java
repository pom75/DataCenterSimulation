package fr.upmc.colins.farm3.coordCpu;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;

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
	
	protected HashMap<String, String>  idPrio = new HashMap<String, String>();
	protected HashMap<String, ArrayList<String>>  idCore = new HashMap<String, ArrayList<String>>();
	
	public CooridationCoreInCPU(
			Integer id,
			ArrayList<Double> coreFre,
			String uriCPU
			) throws Exception{
		super(true, true);
		

		this.id = id;
		this.coreFre = coreFre;
		this.cpuUriInboundPort = uriCPU;
		this.logId = MessageFormat.format("[ CoCPU {0} ]", String.format("%04d", id));

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
		String idAp = prio.split("-")[0];
		String p = prio.split("-")[1];
		
		idPrio.put(idAp, p);
		idCore.put(idAp, listCore);
		
		boolean response = false;
		ArrayList<String> buffList ; 
		for(int i = 0; i<listCore.size(); i++){
			buffList = new ArrayList<String>();
			buffList.add(listCore.get(i));
			if(canUp(Integer.parseInt(listCore.get(i).split("-")[4]), fcs)){
				if(incpp.majClockSpeed(prio, fcs, listCore)){
					response = true;
					System.out.println(logId + " Tentative de changement de fréquence réussite ");
					coreFre.set(Integer.parseInt(listCore.get(i).split("-")[4]), coreFre.get(Integer.parseInt(listCore.get(i).split("-")[4])) + fcs );
					break;
				}
			}else{
				System.out.println(logId + " Contrainte de Diff! Impossible de chnager le coeur "+Integer.parseInt(listCore.get(i).split("-")[4]));
				System.out.println(logId + " Tentative de changement par la priorité");

			}
			
		}
		return incpp.majClockSpeed(prio, fcs, listCore);
		
	}
	
	//Contrainte si diff   ->    >=0.5 || <= 0.5
		public boolean canUp(int i,Double fcs){
			Double buff = coreFre.get(i) + fcs;
			boolean resp = true;
			
			for(int j = 0; j < coreFre.size(); j++){
				if(!(buff -  coreFre.get(j) <= 0.5 && buff -  coreFre.get(j) >= -0.5)){
					resp = false;
					break;
				}
			}
			
			return resp;
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
