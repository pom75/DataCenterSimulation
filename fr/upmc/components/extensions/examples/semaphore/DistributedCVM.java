package fr.upmc.components.extensions.examples.semaphore;

import fr.upmc.components.ComponentI;
import fr.upmc.components.cvm.AbstractDistributedCVM;
import fr.upmc.components.extensions.examples.countDownLatch.components.ClientComponentSynchronized;
import fr.upmc.components.extensions.synchronizer.components.DistributedSynchronizerManager;
import fr.upmc.components.extensions.synchronizer.components.SynchronizerManager;

public class 		DistributedCVM
extends		AbstractDistributedCVM
{


protected final String	synchronizerManagerURI = "sm_uri";
	
	
	// Les deux composants 
	protected final String	clientComponent1URI = "client1_uri";
	protected final String	clientComponent2URI = "client2_uri";
	

	protected final String groupID1 = 
			SynchronizerManager.encodeCountDownLatchID("group1_uri", 2);


	
	public				DistributedCVM(String[] args)
	throws Exception
	{
		super(args);
	}


	/**
	 * do some initialisation before any can go on.
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	true				// no more preconditions.
	 * post	true				// no more postconditions.
	 * </pre>
	 * 
	 * @see fr.upmc.components.cvm.AbstractDistributedCVM#initialise()
	 */
	@Override
	public void			initialise()
	throws Exception
	{
		super.initialise();
	}


	/**
	 * instantiate components and publish their ports.
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	true				// no more preconditions.
	 * post	true				// no more postconditions.
	 * </pre>
	 * 
	 * @see fr.upmc.components.cvm.AbstractDistributedCVM#instantiateAndPublish()
	 */
	@Override
	public void			instantiateAndPublish()
	throws Exception
	{
		ComponentI cps = null;
		
		if(thisJVMURI.equals(clientComponent1URI))
		{
			cps = new ClientComponentSynchronized(
						clientComponent1URI,
						synchronizerManagerURI,
						groupID1, 4, false);
		}
		else if(thisJVMURI.equals(clientComponent2URI))
		{
			cps = new ClientComponentSynchronized(
						clientComponent2URI,
						synchronizerManagerURI,
						groupID1, 6, false);
		}
		else if(thisJVMURI.equals(synchronizerManagerURI))
		{
			cps = new DistributedSynchronizerManager(
						synchronizerManagerURI, true);
		} 
		
		if(cps == null) {
			System.out.println("Unknow JVM URI : " + thisJVMURI);
		}
		else {
			// add it to the deployed components
			this.deployedComponents.add(cps);
		}
		
		super.instantiateAndPublish();
	}


	/**
	 * interconnect the components.
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	true				// no more preconditions.
	 * post	true				// no more postconditions.
	 * </pre>
	 * 
	 * @see fr.upmc.components.cvm.AbstractDistributedCVM#interconnect()
	 */
	@Override
	public void			interconnect()
	throws Exception
	{
		assert this.instantiationAndPublicationDone;

		super.interconnect();
	}


	/**
	 * 
	 * <p><strong>Contract</strong></p>
	 * 
	 * <pre>
	 * pre	true				// no more preconditions.
	 * post	true				// no more postconditions.
	 * </pre>
	 * 
	 * @see fr.upmc.components.cvm.AbstractDistributedCVM#shutdown()
	 */
	@Override
	public void			shutdown()
	throws Exception
	{
		super.shutdown();
	}


	public static void	main(String[] args)
	{
		System.out.println("Main thread beginning");
		try {
			DistributedCVM da = new DistributedCVM(args);
			da.deploy();
			da.start();
			Thread.sleep(25000);
			da.shutdown();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		System.out.println("Main thread ending") ;
		System.exit(0);
	}
}

