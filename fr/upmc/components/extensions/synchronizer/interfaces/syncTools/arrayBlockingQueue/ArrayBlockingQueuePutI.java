package fr.upmc.components.extensions.synchronizer.interfaces.syncTools.arrayBlockingQueue;

import java.io.Serializable;

import fr.upmc.components.interfaces.OfferedI;

public interface ArrayBlockingQueuePutI
extends OfferedI
{
	/**
	 * Inserts the specified element at the tail of this requests, waiting for 
	 * space to become available if the requests is full.
	 * @param e object to add to the array
	 * @throws Exception
	 */
	public void 		put(Serializable e) throws Exception;
}
