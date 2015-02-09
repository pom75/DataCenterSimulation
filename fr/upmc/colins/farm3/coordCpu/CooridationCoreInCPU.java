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
	
	//Priorité max dans l'app
	protected int maxP = -1;
	
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
		String p =  idAp; //prio.split("-")[1];  HACK par defaut  prio de l'app = a son id 

		if(maxP < Integer.parseInt(p)){
			maxP = Integer.parseInt(p);
		}
		
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
					return true;
				}
			}else{
				if(incpp.majClockSpeed(prio, fcs, listCore) && fcs + coreFre.get(Integer.parseInt(listCore.get(i).split("-")[4])) < 3.0){
					response = true;
				}
				System.out.println(logId + " _________________________Contrainte de Diff! Impossible de chnager le coeur "+Integer.parseInt(listCore.get(i).split("-")[4]));
			}
			
		}
		// si on a response = true c'est qu'on peut peut-etre forcé la maj
		if(response){
			System.out.println(logId + " ________________________________________Tentative de changement par la priorité");
			//Si je suis la priorité maximum ou égale a une autre je fais se que je veux ! 
			if(maxP <= Integer.parseInt(p)){
				System.out.println(logId + " Priorité MAX Tentative de changement des autres coeurs");
				//On change tous les autres coeurs 
				if( incpp.updateClockSpeed(fcs, listCore) ){
					System.out.println(logId + "  Autres coeur changé");
					//On refait la boucle d'avant poru re upe les coeur
					for(int i = 0; i<listCore.size(); i++){
						buffList = new ArrayList<String>();
						buffList.add(listCore.get(i));
						if(canUp(Integer.parseInt(listCore.get(i).split("-")[4]), fcs)){
							if(incpp.majClockSpeed(prio, fcs, listCore)){
								response = true;
								System.out.println(logId + " *********************************** Tentative de changement de fréquence réussite ");
								coreFre.set(Integer.parseInt(listCore.get(i).split("-")[4]), coreFre.get(Integer.parseInt(listCore.get(i).split("-")[4])) + fcs );
								return true;
							}
						}else{
							if(incpp.majClockSpeed(prio, fcs, listCore) && fcs + coreFre.get(Integer.parseInt(listCore.get(i).split("-")[4])) > 3.0){
								response = true;
							}
							System.out.println(logId + " ********************* Contrainte de Diff! Impossible de chnager le coeur "+Integer.parseInt(listCore.get(i).split("-")[4]));
						}
						
					}
				}
			}
		}
		
		
		
		
		return false;
		
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

	public boolean updateClockSpeed(Double fcs, ArrayList<String> listCore) {
		// TODO Auto-generated method stub
		return false;
	}

	public ArrayList<String> getCoresRequestArrivalInboundPortUris() {
		// TODO Auto-generated method stub
		return null;
	}
}
