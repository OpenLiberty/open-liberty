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

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.address.SipURL;

import java.io.IOException;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.servlets.OutgoingSipServletRequest;
import com.ibm.ws.sip.container.servlets.SipServletResponseImpl;
import com.ibm.ws.sip.container.util.SipUtil;

/**
 * 
 * @author Anat Fradin, Dec 6, 2006
 * Class that implements the SendingProcessor when NAPTR behaviour not needed.
 * We can hold one instance of this object in the container. This object
 * don't have a state so from the performance point of view - only one object
 * can do the work.
 */
public class Sender extends SendProcessor {

	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(Sender.class);
	
	/** Singleton */
    private static Sender s_SenderInstance = null;
	
    /**
     * Locker that used for the sync blocks
     */
    private static Object s_lock = new Object();
    
    
    /**
	 * Returns an instance of the IDomainInstance
	 */
	public static Sender getInstnace(){
		if(s_SenderInstance == null){
			synchronized (s_lock) {
				if(s_SenderInstance == null){
					s_SenderInstance = new Sender();
				}
			}
		}
		return s_SenderInstance;
	}

	/** 
	 * @see com.ibm.ws.sip.container.naptr.ISendingProcessor#responseReceived(com.ibm.ws.sip.container.servlets.SipServletResponseImpl, com.ibm.ws.sip.container.naptr.ISenderListener)
	 */
	public void responseReceived(SipServletResponseImpl resp, ISenderListener client) {
		client.responseReceived(resp);
	}

	/**
	 * @see com.ibm.ws.sip.container.naptr.ISendingProcessor#sendRequest(javax.servlet.sip.SipURI, com.ibm.ws.sip.container.servlets.OutgoingSipServletRequest)
	 */
	public void sendRequest(OutgoingSipServletRequest request, ISenderListener client)
		throws IOException
	{
		try {
			SipUtil.setDestinationHeader(client.getTarget(), request.getRequest());
		}
		catch (SipParseException e) {
			throw new IOException(e.getMessage());
		}
		try {
			// Sending the request to sipProvider. If IOException will be 
			// received - the request will be sent to next hop if present.
			// If not - _response (if exist )will be forwarded to the application.
			send(request, client);
		} 
		catch (IOException e) {
			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer buff = new StringBuffer();
				buff.append("IOException when sending request. try next destination for request: ");
				buff.append(request);
				c_logger.traceDebug(this, "sendToNextDestination",buff.toString());
			}
			client.failedToSendRequest(true);
		}		
	}

	/**
	 *  @see com.ibm.ws.sip.container.naptr.ISendingProcessor#processTimeout(com.ibm.ws.sip.container.naptr.ISenderListener)
	 */
	public void processTimeout(ISenderListener client) {
		client.failedToSendRequest(false);
	}

	/** 
	 * @see com.ibm.ws.sip.container.naptr.SendProcessor#cleanItself()
	 */
	public void cleanItself() {
		// Do nothing - this object is a Single Tone and not poolable
		super.cleanItself();
	}

	/** 
	 * @see com.ibm.ws.sip.container.naptr.ISendingProcessor#getLastUsedDestination()
	 */
	public SipURL getLastUsedDestination( ISenderListener client) {
		return client.getTarget();
	}
}
