package fr.upmc.colins.farm3.dispatcher;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.math3.distribution.NormalDistribution;

import fr.upmc.colins.farm3.connectors.InfoControlAppServiceConnector;
import fr.upmc.colins.farm3.connectors.RequestServiceConnector;
import fr.upmc.colins.farm3.conrolapp.AppControlerInfoInboundPort;
import fr.upmc.colins.farm3.conrolapp.ApplicationControl;
import fr.upmc.colins.farm3.core.RequestArrivalI;
import fr.upmc.colins.farm3.core.ResponseArrivalI;
import fr.upmc.colins.farm3.generator.RequestGeneratorOutboundPort;
import fr.upmc.colins.farm3.objects.Request;
import fr.upmc.colins.farm3.objects.Response;
import fr.upmc.colins.farm3.utils.TimeProcessing;
import fr.upmc.components.AbstractComponent;
import fr.upmc.components.cvm.AbstractCVM;
import fr.upmc.components.cvm.pre.dcc.DynamicComponentCreationOutboundPort;
import fr.upmc.components.exceptions.ComponentShutdownException;

/**
 * The class <code>RequestDispatcher</code> implements a request dispatcher
 *
 * <p>
 * <strong>Description</strong>
 * </p>
 * A request dispatcher is a component that will forward received requests to
 * its dedicated virtual machine. The request dispatcher choose the least
 * recently used virtual machine when forwarding request.
 * 
 * 
 * <p>
 * Created on : jan. 2015
 * </p>
 * 
 * @author Colins-Alasca
 * @version $Name$ -- $Revision$ -- $Date$
 */
public class			RequestDispatcher
extends		AbstractComponent
{

	protected String logId;
    
	// -------------------------------------------------------------------------
	// Constructors and instance variables
	// -------------------------------------------------------------------------


	/** identifier 															*/
	protected final Integer id;
	/** a normal distribution generator used to generate number of instructions */
	protected NormalDistribution nd;
	/** true if the request dispatcher is idle otherwise false.				*/
	protected boolean					dispatcherIdle ;
	/** request currently being serviced, null if any.						*/
	protected Request					servicing ;
	/** queue of pending requests.											*/
	protected BlockingQueue<Request>	requestsQueue ;
	/** sum of the service time of all completed requets.					*/
	protected long						totalServicingTime ;
	/** total number of completely serviced requests.						*/
	protected int						totalNumberOfServicedRequests ;
	
	/** prefix uri of the information inbound port of the appication contorleur	*/
	protected static final String AC_IIP_PREFIX = "ac-iip-";
	/** prefix uri of the information outbound port of the appication contorleur	*/
	protected static final String AC_OIP_PREFIX = "ac-oip-";
	
	ApplicationControl ap;
	
	/** outbound ports to the core											*/
	protected InfoOutboundPort iop;
	
	
	/** dynamic component creation outbound port to the provider's JVM		*/
	protected DynamicComponentCreationOutboundPort portToProviderJVM;
	
	
	/** inbound port for the request generator								*/
	protected RDRequestArrivalInboundPort raip;
	/** outbound ports to the core											*/
	protected Queue<RequestGeneratorOutboundPort> rgops;
	
	/** inbound ports for each cores (to obtain the response) 				*/
	protected ArrayList<RDResponseArrivalInboundPort> respAips;

	/** mean time 															*/
	protected Double meanTime = -1.0;
	
	/** wtf variable															*/
	protected boolean wtf = true;
	
	/**
	 * create a request dispatcher
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	true			// no precondition.
	 * post	true			// no postcondition.
	 * </pre>
	 *
	 * @param id					
	 * 				identifier of the request dispatcher
	 * @param inboundPortURI		
	 * 				URI of the port used to received requests, 
	 * 				linked to a request generator
	 * @param outboundPortURIs		
	 * 				URI of the ports used to send requests,
	 * 				linked to the virtual machines
	 * @param vmRequestArrivalInboundPortUris 
	 * 				URI of the ports to each virtual machines
	 * @param meanNrofInstructions 
	 * 				the mean number of instructions
	 * @param standardDeviation 
	 * 				the standard deviation
	 * @throws Exception
	 */
	public				RequestDispatcher(
		Integer id,
		String inboundPortURI,
		ArrayList<String> outboundPortURIs,
		ArrayList<String> vmRequestArrivalInboundPortUris, 
		Double meanNrofInstructions, 
		Double standardDeviation
		) throws Exception
	{
		super(true, true) ;

		assert id != null;
		assert inboundPortURI != null;
		assert outboundPortURIs != null;
		
		this.logId = MessageFormat.format("[   RD {0}  ]", String.format("%04d", id));
		this.id = id ;
		this.dispatcherIdle = true ;
		this.servicing = null ;
		this.requestsQueue = new LinkedBlockingQueue<Request>() ;
		this.totalServicingTime = 0L ;
		this.totalNumberOfServicedRequests = 0 ;

		// inbound port for request arrival
		this.addOfferedInterface(RequestArrivalI.class) ;
		this.raip = new RDRequestArrivalInboundPort(inboundPortURI, this) ;
		this.addPort(this.raip) ;
		if (AbstractCVM.isDistributed) {
			this.raip.publishPort() ;
		} else {
			this.raip.localPublishPort() ;
		}
		
		this.rgops = new LinkedList<>(); 
		this.respAips = new ArrayList<>();
		// interface is added once.
		this.addRequiredInterface(RequestArrivalI.class) ;

		// receive response from the virtual machines
		this.addOfferedInterface(ResponseArrivalI.class) ;
		
		for (int i = 0; i < outboundPortURIs.size(); i++) {
			String outboundPortURI = outboundPortURIs.get(i);
			// outbound port for request departure (into a virtual machine)
			RequestGeneratorOutboundPort rgop = new RequestGeneratorOutboundPort(outboundPortURI, this);
			this.rgops.add(rgop);
			this.addPort(rgop) ;
			if (AbstractCVM.isDistributed) {
				rgop.publishPort() ;
			} else {
				rgop.localPublishPort();
			}
			rgop.doConnection(vmRequestArrivalInboundPortUris.get(i),
					RequestServiceConnector.class.getCanonicalName());
			System.out.println(logId + " Connect the request dispatcher to the virtual machine (via "
					+ rgop.getPortURI() + ")");

			// the URI used here, doesn't really matter, it should just be unique
			String rdResponseArrivalInboundPortUri = "rd-resp-raip-" + java.util.UUID.randomUUID();
			
			// create an inbound port for response from this virtual machine
			RDResponseArrivalInboundPort respAip = new RDResponseArrivalInboundPort(rdResponseArrivalInboundPortUri, this);
			this.respAips.add(respAip);
			this.addPort(respAip);
			if(AbstractCVM.isDistributed){
				respAip.publishPort();
			}else{
				respAip.localPublishPort();
			}
		}

		
		this.addRequiredInterface(AppControlerInfoInboundPort.class) ;
		
		iop = new InfoOutboundPort(AC_OIP_PREFIX + id, this);
		this.addPort(this.iop) ;
		if (AbstractCVM.isDistributed) {
			this.iop.publishPort() ;
		} else {
			this.iop.localPublishPort() ;
		}
		iop.doConnection(AC_IIP_PREFIX + id, InfoControlAppServiceConnector.class.getCanonicalName());

		this.nd = new NormalDistribution(meanNrofInstructions,
				standardDeviation);

		System.out.println(logId + " Request dispatcher (id " + id + ") created") ;
		assert	id != null;
		assert	!dispatcherIdle || (servicing == null) ;
		assert	dispatcherIdle || (servicing != null) ;
		assert	totalServicingTime >= 0 && totalNumberOfServicedRequests >= 0 ;
	}

	// -------------------------------------------------------------------------
	// Component life-cycle
	// -------------------------------------------------------------------------

	/**
	 * shut down the component after canceling any pending end request
	 * processing task, and output the average service time of the completed
	 * service requests.
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	true				// no more preconditions.
	 * post	true				// no more postconditions.
	 * </pre>
	 * 
	 * @see fr.upmc.components.AbstractComponent#shutdown()
	 */
	@Override
	public void			shutdown() throws ComponentShutdownException
	{
		// disconnect rgops
		for (RequestGeneratorOutboundPort rgop : this.rgops) {
			try {
				if (rgop.connected()) {
					rgop.doDisconnection();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		super.shutdown();
	}
	
	
	

	// -------------------------------------------------------------------------
	// Component internal services
	// -------------------------------------------------------------------------

	/**
	 * process a request arrival event, queueing the request and the processing
	 * a begin sericing event if the server is currently idle.
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	r != null
	 * post	true			// no postcondition.
	 * </pre>
	 *
	 * @param r
	 * @throws Exception
	 */
	public void			requestArrivalEvent(Request r) throws Exception
	{
		assert	r != null ;
		long t = System.currentTimeMillis() ;
		Long nrofInstructions = (long) nd.sample();
		
		// ask the virtual machines to do the connection to the inbound port of the request dispatcher (for response)
		
		if( wtf == true ){
			wtf = false;
			System.out.println(logId + " Linking virtual machines to the request dispatcher for response connection");
			RequestGeneratorOutboundPort[] rgopsArray = rgops.toArray(new RequestGeneratorOutboundPort[0]);
			for (int i = 0; i < respAips.size(); i++) {
				RDResponseArrivalInboundPort rdRespAip = respAips.get(i);
				if (rgopsArray[i].connected()) {
					try {
						rgopsArray[i].connectResponseConnection(rdRespAip
								.getServerPortURI());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		r.setNrofInstructions(nrofInstructions);
		System.out.println(logId + " Accepting request       " + r + " at "
				+ TimeProcessing.toString(t) + " with " + nrofInstructions
				+ " instructions");
		r.setArrivalTime(t) ;
		this.requestsQueue.add(r) ;
		if (!this.dispatcherIdle) {
			System.out.println(logId + " Queueing request " + r) ;
		} else {
			this.beginServicingEvent() ;
		}
	}

	
	/**
	 * process a begin servicing event, e.g. schedule a end servicing event
	 * after a delay of the processing time of the request.
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	true			// no precondition.
	 * post	true			// no postcondition.
	 * </pre>
	 *
	 */
	public void			beginServicingEvent()
	{
		this.servicing = this.requestsQueue.remove() ; 
		scheduleServicing(this.servicing);
	}
	
	/**
	 * schedule the request contained in the servicing field as a task.
	 * called by beginServicingEvent and also by endServicingEvent
	 * @param request the request to pass to a core
	 */
	private void		scheduleServicing(Request request){
		
		this.dispatcherIdle = false ;
		try {
			System.out.println(logId + " Dispatching request     "
					+ this.servicing + " at "
					+ TimeProcessing.toString(System.currentTimeMillis())) ;
			RequestGeneratorOutboundPort rgop = this.rgops.poll();		
			rgop.acceptRequest(request);
			this.rgops.add(rgop);
			this.endServicingEvent() ;
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
	/**
	 * process a end servicing event, e.g. update the statistics for the average
	 * service time, and then if the queue is not empty execute a begin
	 * servicing event immediately.
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	true			// no precondition.
	 * post	true			// no postcondition.
	 * </pre>
	 *
	 * @throws Exception
	 */
	public void			endServicingEvent() throws Exception
	{
		long t = System.currentTimeMillis() ;
		long st = t - this.servicing.getArrivalTime() ; 
		this.totalServicingTime += st ;
		this.totalNumberOfServicedRequests++ ;
		
		if (this.requestsQueue.isEmpty()) {
			this.servicing = null ;
			this.dispatcherIdle = true ;
		} else {
			this.beginServicingEvent() ;
		}
		
		
	}

	/**
	 * update the mean time of request processing (from the virtual machine)
	 * TODO and forward the mean of mean time to a controller
	 * @param response the received response
	 */
	public void responseArrivalEvent(Response response) {
		/*
		if(this.meanTime < 0){
			// first set
			this.meanTime = (double) response.getDuration();
		}else{
			this.meanTime = (this.meanTime + response.getDuration()) / 2.0;
		}		
		System.out.println(logId + " New mean time : " + this.meanTime);
		*/
		try {
			iop.receiveInfo(response.getDuration()+"");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		//response.setDuration(this.meanTime);
		// TODO send the new mean time of the request dispatcher to a controller

	}
}
