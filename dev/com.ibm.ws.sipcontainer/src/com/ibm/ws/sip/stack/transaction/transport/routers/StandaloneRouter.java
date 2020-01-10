/*******************************************************************************
 * Copyright (c) 2003,2009 IBM Corporation and others.
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
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.address.URI;
import jain.protocol.ip.sip.header.NameAddressHeader;
import jain.protocol.ip.sip.header.RouteHeader;
import jain.protocol.ip.sip.message.Request;

import java.util.ArrayList;
import java.util.List;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.header.GenericNameAddressHeaderImpl;
import com.ibm.ws.sip.stack.transaction.transport.Hop;
import com.ibm.ws.sip.stack.util.SipStackUtil;

/**
 * @author Amirk
 */
public class StandaloneRouter implements Router 
{

	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(StandaloneRouter.class);

	//default route for outbound proxy
	private Hop m_defaultHop;
	
	public List getNextHops(Request sipRequest) {                
		Hop nextHop;

		if (m_defaultHop != null) {
			// use outbound proxy
			nextHop = m_defaultHop;
		}
		else {
			try {
				URI uri;
				GenericNameAddressHeaderImpl nameAddress =
					(GenericNameAddressHeaderImpl)sipRequest.getHeader(SipStackUtil.DESTINATION_URI, true);
				if (nameAddress == null) {
					// use either the request-uri or the top route uri
					NameAddressHeader topRoute = (NameAddressHeader)sipRequest.getHeader(RouteHeader.name, true);
					if (topRoute == null) {
						// no route header, next hop is the request-uri
						uri = sipRequest.getRequestURI();
					}
					else {
						// examine top route
						uri = topRoute.getNameAddress().getAddress();
						if (uri instanceof SipURL) {
							SipURL url = (SipURL)uri;
							boolean strictRouting = url.hasParameter(STRICT_ROUTING_PARAM);
							if (strictRouting) {
								// next hop is a strict router.
								// route to request-uri.
								// remove our proprietary param before sending out
								url.removeParameter(STRICT_ROUTING_PARAM);
								uri = sipRequest.getRequestURI();
							}
						}
						else {
							if (c_logger.isTraceDebugEnabled()) {
								c_logger.traceDebug("Error: getNextHops - top route uri is not a SipURL");
							}
							uri = null;
						}
					}
				}
				else {
					// use proprietary address header
					uri = nameAddress.getNameAddress().getAddress();
					sipRequest.removeHeader(SipStackUtil.DESTINATION_URI, true);
				}
				
				// convert uri to a hop object
				if (uri == null) {
					nextHop = null;
				}
				else if (uri instanceof SipURL) {
					SipURL reqUrl = (SipURL)uri;
					nextHop = Hop.getHop(reqUrl);
				}
				else {
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug("Error: getNextHops - uri is not a SipURL");
					}
					nextHop = null;
				}
			}
			catch (SipParseException e) {
				if( c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "getNextHops", e.getMessage(), e);
				}
				nextHop = null;
			}
		}
		
		// return a list of 1 hop element
		List hops = new ArrayList(1);
		if (nextHop != null) {
			hops.add(nextHop);
		}
		return hops;
        
	}

	
	public Hop getOutboundProxy() 
	{
		return m_defaultHop;
	}
	
	public void setOutboundProxy( Hop outProxy ) 
	{
		m_defaultHop = outProxy;
	}
	
	public void processRequest(Request req) throws SipParseException
	{
		//not implemented , only SLSP
	}
			
	public void removeConnectionHop(  Hop value )
	{
		//not implemented , only SLSP		
	}
}
