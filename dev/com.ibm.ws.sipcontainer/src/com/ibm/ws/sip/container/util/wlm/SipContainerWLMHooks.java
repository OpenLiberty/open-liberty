/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.util.wlm;

/**
 * The SIP container implements this, to enable
 * a client to extend the container's processing
 * of incoming messages.
 * 
 * @author dror yaffe
 * 
 */
public interface SipContainerWLMHooks {

	/**
	 * This method registers a transformer.
	 * This method should be called at most once
	 * during the lifetime of the process;
	 * the second and later calls will
	 * throw an Exception rather than register
	 * the transformer.
	 * 
	 * @param qt the QueueableTransformer that the container will use henceforth
	 */
	public void setQueueableTransformer(QueueableTransformer qt)
			throws Exception;
	
	/**
	 * This method registers a failover listener.
	 * This method should be called at most once
	 * during the lifetime of the process;
	 * the second and later calls will
	 * throw an Exception rather than register
	 * the listner.
	 * 
	 * @param listener the WLMDialogFailoverListner that the container will use henceforth
	 */
	public void setWLMDailogFailoverListner(WLMDialogFailoverListner listener)
			throws Exception;
	
	/**
	 * 
	 * @return the QueueableTransformer, can return null
	 */
	public QueueableTransformer getQueueableTransformer();
	
	/**
	 * 
	 * @return the WLMDialogFailoverListner, can return null
	 */
	public WLMDialogFailoverListner getWLMDailogFailoverListner();
}
