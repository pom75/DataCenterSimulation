package fr.upmc.components.examples.basic_cs;

import fr.upmc.components.cvm.AbstractCVM;
import fr.upmc.components.examples.basic_cs.components.URIConsumer;
import fr.upmc.components.examples.basic_cs.components.URIProvider;
import fr.upmc.components.ports.PortI;

/**
 * The class <code>CVM</code>
 *
 * <p><strong>Description</strong></p>
 * 
 * <p><strong>Invariant</strong></p>
 * 
 * <pre>
 * invariant	true
 * </pre>
 * 
 * <p>Created on : 22 janv. 2014</p>
 * 
 * @author	<a href="mailto:Jacques.Malenfant@lip6.fr">Jacques Malenfant</a>
 * @version	$Name$ -- $Revision$ -- $Date$
 */
public class			CVM
extends		AbstractCVM
{
	protected static final String	URIGetterOutboundPortURI = "oport" ;
	protected static final String	URIProviderInboundPortURI = "iport" ;

	/**
	 * instantiate the components, publish their port and interconenct them.
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	true				// no more preconditions.
	 * post	true				// no more postconditions.
	 * </pre>
	 * 
	 * @see fr.upmc.components.cvm.AbstractCVM#deploy()
	 */
	@Override
	public void			deploy() throws Exception
	{
		URIProvider	uriProvider ;
		URIConsumer	uriConsumer ;

		// Creation phase

		// create the provider component
		uriProvider =
				new URIProvider("myURI", URIProviderInboundPortURI, false) ;
		// add it to the deployed components
		this.deployedComponents.add(uriProvider) ;
		// create the consumer component
		uriConsumer = new URIConsumer(URIGetterOutboundPortURI) ;
		// add it to the deployed components
		this.deployedComponents.add(uriConsumer) ;
		
		// Connection phase

		// do the connection
		PortI consumerOutboundPort =
						uriConsumer.findPortFromURI(URIGetterOutboundPortURI) ;
		consumerOutboundPort.doConnection(
				URIProviderInboundPortURI,
				"fr.upmc.components.examples.basic_cs.URIServiceConnector") ;

		// deployment done
		super.deploy();
	}

	/**
	 * disconnect the components and then call the base shutdown method.
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	true				// no more preconditions.
	 * post	true				// no more postconditions.
	 * </pre>
	 * 
	 * @see fr.upmc.components.cvm.AbstractCVM#shutdown()
	 */
	@Override
	public void				shutdown() throws Exception
	{
		// any disconnection not done yet should be performed here

		super.shutdown();
	}

	public static void		main(String[] args)
	{
		CVM a = new CVM() ;
		try {
			a.deploy() ;
			System.out.println("starting...") ;
			a.start() ;
			Thread.sleep(15000L) ;
			System.out.println("shutting down...") ;
			System.out.print("\007"); System.out.flush();
			a.shutdown() ;
			System.out.println("ending...") ;
			System.exit(0) ;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
