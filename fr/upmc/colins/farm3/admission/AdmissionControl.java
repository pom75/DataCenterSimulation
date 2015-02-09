package fr.upmc.colins.farm3.admission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fr.upmc.colins.farm3.conrolapp.ApplicationControl;
import fr.upmc.colins.farm3.coordCpu.CooridationCoreInCPU;
import fr.upmc.colins.farm3.core.ControlRequestArrivalI;
import fr.upmc.colins.farm3.cpu.ControlRequestGeneratorOutboundPort;
import fr.upmc.colins.farm3.dispatcher.dynamic.DynamicRequestDispatcher;
import fr.upmc.colins.farm3.objects.Application;
import fr.upmc.colins.farm3.vm.dynamic.DynamicVM;
import fr.upmc.components.AbstractComponent;
import fr.upmc.components.cvm.AbstractCVM;
import fr.upmc.components.cvm.pre.dcc.DynamicComponentCreationConnector;
import fr.upmc.components.cvm.pre.dcc.DynamicComponentCreationI;
import fr.upmc.components.cvm.pre.dcc.DynamicComponentCreationOutboundPort;
import fr.upmc.components.cvm.pre.dcc.DynamicallyConnectableComponentI;
import fr.upmc.components.exceptions.ComponentShutdownException;
import fr.upmc.components.exceptions.ComponentStartException;

/**
 * The class <code>AdmissionControl</code> implements a component that simulate
 * an admission control. 
 * 
 * <p><strong>Description</strong></p>
 * An admission control take care of serving application
 * upload from a consumer's JVM (eg. from the request generator). 
 * When uploading an application the admission control allocate a predefined number
 * of virtual machines and a dedicated request dispatcher. The admission control then
 * return the uri of the request dispatcher.
 *  
 * <p>Created on : december 2014</p>
 * 
 * @author	Colins-Alasca
 * @version	$Name$ -- $Revision$ -- $Date$
 */
public class AdmissionControl extends AbstractComponent {


	/** log constant	 													*/
	private static final String logId = "[ AdmControl ]";
	
	/**	total number of cores												*/
	protected Long nrofCores;
	/**	number of cores allocated per virtual machine						*/
	protected int nrofCoresPerVM;
	/**	number of virtual machines allocated per request dispatcher			*/
	protected int nrofVMPerDispatcher;
	
	/** inbound port to be connected to the request generator 				*/
	protected ApplicationRequestArrivalInboundPort applicationRequestArrivalInboundPort;
	/** outbound ports for each cpu							 				*/
	protected List<ControlRequestGeneratorOutboundPort> controlRequestGeneratorOutboundPorts;

	/** prefix uri of the request arrival inbound port of instanciated vm	*/
	protected static final String VM_RAIP_PREFIX = "vm-raip-";
	/** prefix uri of the request generator outbound port to each core		*/
	protected static final String VM_RGOP_PREFIX = "vm-rgop-";
	/** count of virtual machines instanciated				 				*/
	protected int virtualMachineCount ;
	
	/** prefix uri of the request arrival inbound port of the dispatchers	*/
	protected static final String RD_RAIP_PREFIX = "rd-raip-";
	/** prefix uri of the request generator outbound port to each vm		*/
	protected static final String RD_RGOP_PREFIX = "rd-rgop-";

	/** count of request dispatchers instanciated				 			*/
	protected int requestDispatcherCount ;
	
	/** list of the uris of the core request arrival inbound port 			*/
	protected List<String> coreRequestArrivalInboundPortUris;
	/** list of the used uris of the core request arrival inbound port		*/
	protected List<String> usedCoreRequestArrivalInboundPortUris;
	
	/** dynamic component creation outbound port to the provider's JVM		*/
	protected DynamicComponentCreationOutboundPort portToProviderJVM;
	
	/** Liste des URI controleur CPU / coeur d'un CPU */
	protected HashMap<String,ArrayList<String>> cpuCoreInboundPortUris;
	
	/** Liste des URI app / coeur d'un CPU */
	protected HashMap<String,ArrayList<String>> appCoreInboundPortUris = new HashMap<String, ArrayList<String>>();
	
	/** Liste des URI app / VM */
	protected HashMap<String,ArrayList<String>> appVMInboundPortUris = new HashMap<String, ArrayList<String>>();

	private Long nbCore;
	
	private boolean bool = true;




	/**
	 * Constructor
	 * 
	 * @param nrofCores
	 * @param nrofCoresPerVM,
	 * @param nrofVMPerDispatcher,
	 * @param outboundPortUri
	 * @param inboundPortUri
	 * @param coreRequestArrivalInboundPortUris 
	 * @param cupRequestArrivalInboundPortUris 
	 * @param isDistributed
	 * @throws Exception
	 */
	public AdmissionControl(
			Long nrofCores, 
			Long nbCore,
			Integer nrofCoresPerVM,
			Integer nrofVMPerDispatcher,
			String outboundPortUri,
			String inboundPortUri, 
			ArrayList<String> coreRequestArrivalInboundPortUris,
			HashMap<String,ArrayList<String>> cpuRequestArrivalInboundPortUris
			) throws Exception 
	{
		super(true, true);

		this.nrofCores = nrofCores;
		this.nbCore =nbCore;
		this.nrofCoresPerVM = nrofCoresPerVM;
		this.nrofVMPerDispatcher = nrofVMPerDispatcher;
		this.controlRequestGeneratorOutboundPorts = new ArrayList<ControlRequestGeneratorOutboundPort>();
		this.cpuCoreInboundPortUris = cpuRequestArrivalInboundPortUris;
		
		// this is for the outbounds port towards each cpu (managed by the
		// admission control)
		this.addRequiredInterface(ControlRequestArrivalI.class);
		for (int i = 0; i < nrofCores; i++) {
			ControlRequestGeneratorOutboundPort crgop = new ControlRequestGeneratorOutboundPort(
					outboundPortUri + i, this);
			this.addPort(crgop);
			if (AbstractCVM.isDistributed) {
				crgop.publishPort();
			} else {
				crgop.localPublishPort();
			}
			controlRequestGeneratorOutboundPorts.add(crgop);
		}

		this.addOfferedInterface(ApplicationRequestArrivalI.class);
		this.applicationRequestArrivalInboundPort = new ApplicationRequestArrivalInboundPort(inboundPortUri,
				this);
		this.addPort(this.applicationRequestArrivalInboundPort);
		if (AbstractCVM.isDistributed) {
			this.applicationRequestArrivalInboundPort.publishPort();
		} else {
			this.applicationRequestArrivalInboundPort.localPublishPort();
		}
		
		this.virtualMachineCount = 0;
		this.requestDispatcherCount = 0;
		
		this.coreRequestArrivalInboundPortUris = coreRequestArrivalInboundPortUris;
		this.usedCoreRequestArrivalInboundPortUris = new ArrayList<>();
		
		// for the dynamic stuff below
		this.addRequiredInterface(DynamicComponentCreationI.class) ;
		this.addRequiredInterface(DynamicallyConnectableComponentI.class) ;
		
		System.out.println(logId + " Admission control created");
	}
	
	
	
	
	
	String acceptApplication(Application a) throws Exception {
		
		
		if(bool){
			bool = false;
			try {
				this.creatCoord();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		System.out.println(logId + " Begin creation of application "
				+ a.getUri());
		
		Integer requestDispatcherId = requestDispatcherCount++;
		ArrayList<String> rdRequestGeneratorOutboundPortUris = new ArrayList<>();
		for (int i = 0; i < nrofVMPerDispatcher; i++) {
			rdRequestGeneratorOutboundPortUris.add(RD_RGOP_PREFIX + requestDispatcherId + i);
		}


		ArrayList<String> vmRequestArrivalInboundPortUris = new ArrayList<>();
		ArrayList<String> allCoeurapp = new ArrayList<>();
		
		for (int i = 0; i < nrofVMPerDispatcher; i++) {

			// build the vm
			// FIXME select only available cores
			// TODO select from cpu instead of cores
			Integer virtualMachineId = virtualMachineCount++;
			ArrayList<String> vmRequestGeneratorOutboundPortUris = new ArrayList<>();
			ArrayList<String> assignedCoreRequestArrivalInboundPortUris = new ArrayList<>();
			for (int j = 0; j < nrofCoresPerVM; j++) {
				vmRequestGeneratorOutboundPortUris.add(VM_RGOP_PREFIX + virtualMachineId + j);
				if(this.coreRequestArrivalInboundPortUris.size() <= 0){
					// TODO we assume the number of cores available is always positive
					// the case when we run out of free cores is not yet implemented
					// returning the empty string as an uri will throw an exception when
					// the request generator will try to connect to it.
					System.err.println("The cluster ran out of available cores, sorry.");
					return "";
				}
				String uri = this.coreRequestArrivalInboundPortUris.remove(0);
				assignedCoreRequestArrivalInboundPortUris.add(uri);
				this.usedCoreRequestArrivalInboundPortUris.add(uri);
			}
			//On prends tous les couers des  toutes les VM
			allCoeurapp.addAll(assignedCoreRequestArrivalInboundPortUris);


			//On ajoute le liste VM / Coeur
			appVMInboundPortUris.put(a.getUri()+"", assignedCoreRequestArrivalInboundPortUris);

			this.portToProviderJVM.createComponent(
					DynamicVM.class.getCanonicalName(),
					new Object[]{ 
						virtualMachineId, 
						VM_RAIP_PREFIX + virtualMachineId, 
						vmRequestGeneratorOutboundPortUris,
						assignedCoreRequestArrivalInboundPortUris
					}
					);

			vmRequestArrivalInboundPortUris.add(VM_RAIP_PREFIX + virtualMachineId);
			// the connection between the cores and the vm are done in the constructor
			// of the virtual machine

		}

		//On ajoute les vm a une app
		appVMInboundPortUris.put(a.getUri()+"", vmRequestArrivalInboundPortUris);

		
		//Liste des cpu/core aloué pour une app
		HashMap<String,ArrayList<String>> cpuCoreDist = new HashMap<String, ArrayList<String>>();
		for(int i = 0;i < allCoeurapp.size(); i++){
			ArrayList<String> listCore = new ArrayList<String>();
			String key = "cpu-craip-"+ allCoeurapp.get(i).split("-")[1]; 
			
			if(cpuCoreDist.containsKey(key)){
				listCore = cpuCoreDist.get(key);
				listCore.add(allCoeurapp.get(i));
				cpuCoreDist.put(key, listCore );
				
				
			}else{
				listCore.add(allCoeurapp.get(i));
				cpuCoreDist.put(key, listCore);
				
			}
		}
		
		
		

		
		// build the request dispatcher
		this.portToProviderJVM.createComponent(
				ApplicationControl.class.getCanonicalName(),
				new Object[]{ 
					a.getUri(),
					((Double)a.getExpectedTime()),
					cpuCoreDist,
					a.getPrio() //priorité de l'app
				}
				);
		
		
		
		// build the request dispatcher
		this.portToProviderJVM.createComponent(
				DynamicRequestDispatcher.class.getCanonicalName(),
				new Object[]{ 
					a.getUri(),
					RD_RAIP_PREFIX + requestDispatcherId,
					rdRequestGeneratorOutboundPortUris,
					vmRequestArrivalInboundPortUris,
					a.getMeanNrofInstructions(),
					a.getStandardDeviation()
					}
				);
		
	

		

		System.out
				.println(logId + " End creation of application " + a.getUri());
		System.out
		.println(logId + " Deployed application " + a.getUri() + " is available from " + RD_RAIP_PREFIX + requestDispatcherId);

		return RD_RAIP_PREFIX + requestDispatcherId;
	}

	/**
	 * @see fr.upmc.components.AbstractComponent#start()
	 */
	@Override
	public void			start() throws ComponentStartException
	{
		try {
			this.portToProviderJVM =
								new DynamicComponentCreationOutboundPort(this) ;
			this.portToProviderJVM.localPublishPort() ;
			this.addPort(this.portToProviderJVM) ;
			String jvmURI = (AbstractCVM.isDistributed) ? "provider" : "";
			this.portToProviderJVM.doConnection(
					jvmURI +
					AbstractCVM.DYNAMIC_COMPONENT_CREATOR_INBOUNDPORT_URI,
					DynamicComponentCreationConnector.class.getCanonicalName()) ;
			
		} catch (Exception e) {
			e.printStackTrace() ;
			throw new ComponentStartException() ;
		}

		super.start() ;
		
	}
	
	
	private void creatCoord() throws Exception {
		ArrayList<Double> coreFre = new ArrayList<Double>();
		for(int j = 0 ; j< nbCore ; j++){
			coreFre.add(1.0);
		}
		
		
		for(int i = 0; i< nrofCores/nbCore;i++)
			//FULL HACK
			this.portToProviderJVM.createComponent(
					CooridationCoreInCPU.class.getCanonicalName(),
					new Object[]{ 
						i, 
						coreFre, //HACK
						"cpu-craip-"+i //HACK
					}
					);
	}





	/**
	 * @see fr.upmc.components.AbstractComponent#shutdown()
	 */
	@Override
	public void 		shutdown() throws ComponentShutdownException {
		try {
			if (this.portToProviderJVM.connected()) {
				this.portToProviderJVM.doDisconnection() ;
			}
		} catch (Exception e) {
			throw new ComponentShutdownException() ;
		}		
		super.shutdown();
	}
	
	

}
