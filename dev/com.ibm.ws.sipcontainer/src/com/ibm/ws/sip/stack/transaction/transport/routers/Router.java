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
package com.ibm.ws.sip.stack.transaction.transport.routers;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.message.Request;

import java.util.List;

import com.ibm.ws.sip.stack.transaction.transport.Hop;

/**
 * 
 * @author Amirk
 *
 * The Router interface defines accessor methods to retrieve the default Route and Outbound Proxy of this SipStack.
 * The Outbound Proxy and default Route are made up one or more Hop's. 
 * This Router information is user-defined in an object that implements this interface. 
 * The location of the user-defined Router object is supplied in the Properties object passed to the prperties of the stack by the 
 */
public interface Router 
{
	/**
	 * Parameter added to the top route header
	 * in outgoing requests, to indicate that the next hop is
	 * a strict router, and therefore the message should be
	 * routed to the request-uri instead of the top route,
	 * and the final destination is the bottom route, instead
	 * of the request-uri.
	 * the component sending the request should remove this
	 */
	public static String STRICT_ROUTING_PARAM = "ibmsr";

    /**
     * Gets the Outbound Proxy parameter of this Router, 
     * this method may return null if no outbound proxy is defined.
     * @return Hop - the Outbound Proxy of this Router.
     */
    public Hop getOutboundProxy();

	
	/**
	 * sets the Outbound Proxy parameter of this Router.
	 * @param proxy - the Outbound Proxy of this Router.
	 */
	public void setOutboundProxy( Hop proxy );

    /**
     * return List of Hops for the request.
     * why a list?
     * in case of routing that will handle an DNS\SRV call , there could be multiple Hops that we shoult try.
     * the List should be sorted in an order of acsending order ( try to connect from the first to the last)
     * @param request - the Request message that determines the default route.
     * @return List - the List over all the hops of this Router.
     */
    public List getNextHops(Request request);
    
	/**
	 * prosses the incomming request
	 * @param req - request
	 * @param connection
	 * @throws SipParseException
	 */
	public void processRequest(Request req)	throws SipParseException;
			
	public void removeConnectionHop(  Hop value );
}
