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
package com.ibm.ws.sip.container.naptr;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.internal.SipContainerComponent;
import com.ibm.ws.sip.container.util.ContainerObjectPool;
import com.ibm.ws.sip.properties.StackProperties;

/**
 * Class that creates appropriate object for the INAPTRProcessor interface.
 * It will examine the target SipURI and decides which implementation
 * should be used.
 * @author Anat Fradin, Dec 6, 2006
 *
 */
public class SenderFactory {
	/**
	 * Class Logger.
	 */
	private static final LogMgr c_logger = Log.get(SenderFactory.class);

    /**
     * Parameter that can change the number of pooled objects in the
     * s_napts_naptrSenderPool.
     */
    private final static String MAX_NAPTR_SENDER_POOL_SIZE = StackProperties.MAX_NAPTR_SENDER_POOL_SIZE;
    
    /** pool of NaptrSender objects */
    public static ContainerObjectPool s_naptrSenderPool = 
    		new ContainerObjectPool(NaptrSenderContainer.class,MAX_NAPTR_SENDER_POOL_SIZE);
    
	/**
	 * Ctor
	 */
    private SenderFactory(){
    }

	/**
	 * Decides which implementation of the INAPTRProcessor should be created.
	 * @return
	 */
	public static SendProcessor getNaptrProcessor(boolean useKnownDestination) {
		
		if (SipContainerComponent.getDomainResolverService().isNaptrAutoResolveEnabled() && !useKnownDestination) {
			SendProcessor processor = (SendProcessor) s_naptrSenderPool.get();
			processor.setIsPoolable(true);
			return processor;
		} 
		else {
			// Will return Sender if Naptr is disabled.
			return Sender.getInstnace();
		}
	}
	
	 /**
	  * Method that notifies the SenderFactory that Client ended to use
	  * the sender as final response was received.
	  *
	  */
	 public static void finishToUseSender(SendProcessor sender){
		 if (sender.isPoolable()) {
			 // clean and put back to the queue only if poolabe
			sender.cleanItself();
			s_naptrSenderPool.putBack(sender);
		}
	 }
	
}
