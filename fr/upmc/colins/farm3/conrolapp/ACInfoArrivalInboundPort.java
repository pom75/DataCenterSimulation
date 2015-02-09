package fr.upmc.colins.farm3.conrolapp;

import fr.upmc.components.ComponentI;
import fr.upmc.components.ComponentI.ComponentService;
import fr.upmc.components.ports.AbstractInboundPort;

public class ACInfoArrivalInboundPort 
extends	AbstractInboundPort 
implements AppControlerInfoInboundPort{

	public ACInfoArrivalInboundPort(String uri,
			ComponentI owner) throws Exception {
		super(uri, AppControlerInfoInboundPort.class, owner);
		
		assert	uri != null && owner != null ;
		assert	owner.isOfferedInterface(AppControlerInfoInboundPort.class) ;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void receiveInfo(String inf) throws Exception {
		
		final ApplicationControl ac = (ApplicationControl) this.owner ;
		final String info =  inf;
		ac.handleRequestAsync(
				new ComponentService<Void>() {
					@Override
					public Void call() throws Exception {
						ac.infoArrivalEvent(info);
						return null;
					}
				}) ;
		
	}

}
