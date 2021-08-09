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
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.message.Request;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.stack.transaction.transport.Hop;

/**
 * a router that forwards all requests destined to the same target
 * through the same SLSP.
 * 
 * @author ran
 */
public class SLSPAffinityRouter extends SLSPRouter
{
	/** class Logger */
	private static final LogMgr c_logger = Log.get(SLSPAffinityRouter.class);

	/**
	 * map of SLSPs indexed by mid-way hops.
	 * this allows finding an SLSP given any Via on the path.
	 */
	private HashMap m_userAddressToConnectionTable;

	/**
	 * map of all mid-way hops indexed by SLSP.
	 * this is used when an SLSP connection goes down,
	 * to clear all the hops associated with it in the other table.
	 */
	private HashMap m_connectionToUserAddressTable;

	/**
	 *  constructor
	 */
	public SLSPAffinityRouter() {
		super();
		m_userAddressToConnectionTable = new HashMap();
		m_connectionToUserAddressTable = new HashMap();
	}

	/**
	 * called when a new request comes in,
	 * to update the internal routing tables
	 * 
	 * @param request the new incoming request
	 * @throws SipParseException
	 */
	public void processRequest(Request request)
		throws SipParseException
	{
		// 1. iterate all Via headers on the path, and add each one
		//    to point to this SLSP.
		// 2. ensure all Via headers are listed as dependants of this SLSP.
		
		super.processRequest(request);
		
		Hop slspHop = null;
		HashSet dependants = null;
		HeaderIterator viaHeaders = request.getViaHeaders();
		
		synchronized (this) {
			while (viaHeaders.hasNext()) {
				ViaHeader via = (ViaHeader)viaHeaders.next();
				Hop hop = new Hop(via);
				
				if (slspHop == null) {
					// top via is the slsp
					slspHop = hop;
					
					dependants = (HashSet)m_connectionToUserAddressTable.get(slspHop);
					if (dependants == null) {
						// request from unknown SLSP
						dependants = new HashSet();
						m_connectionToUserAddressTable.put(slspHop, dependants);
					}
				}
				dependants.add(hop);
				m_userAddressToConnectionTable.put(hop, slspHop);
			}
		}
	}

	/**
	 * called when a connection to an SLSP was closed,
	 * to update the internal routing tables
	 * @param connectionHop SLSP connection that was closed
	 */
	public synchronized void removeConnectionHop(Hop connectionHop) {
		super.removeConnectionHop(connectionHop);
		
		// remove from the table of SLSPs
		HashSet dependants = (HashSet)m_connectionToUserAddressTable.remove(connectionHop);

		// remove dependants
		if (dependants != null) {
			Iterator i = dependants.iterator();
			while (i.hasNext()) {
				Hop hop = (Hop)i.next();
				m_userAddressToConnectionTable.remove(hop);
			}
		}
	}

	/**
	 * gets the next hop for the url by affinity.
	 * if this url is used for the first time use round-robin slsp.
	 * 
	 * @param url remote target
	 * @return next hop to send the request, null on error
	 */
	protected Hop getNextHop(SipURL url) {
		Hop slsp;
		Hop target = Hop.getHop(url);
		
		synchronized (this) {
			slsp = (Hop)m_userAddressToConnectionTable.get(target);
			if (slsp == null) {
				// unknown target. get some SLSP using round-robin
				slsp = super.getNextHop(url);
				if (slsp != null) {
					// set the key to this SLSP
					m_userAddressToConnectionTable.put(target, slsp);
				}
			}
			else {
				// target has previously sent a request through this slsp
			}
		}
		return slsp;
	}

	/**
	 * adds another SLSP
	 * @param slsp the SLSP to be added
	 */
	public synchronized void addSLSP(Hop slsp) {
		if (!m_connectionToUserAddressTable.containsKey(slsp)) {
			HashSet dependants = new HashSet();
			m_connectionToUserAddressTable.put(slsp, dependants);
		}
		super.addSLSP(slsp);
	}

	/**
	 * removes an SLSP from the list of known SLSPs
	 * @param slsp the SLSP to be removed
	 */
	public synchronized void removeSLSP(Hop slsp) {
		removeConnectionHop(slsp);
	}

	/**
	 * removes all known SLSPs from the list
	 */
	public synchronized void removeSLSPs() {
		m_connectionToUserAddressTable.clear();
		m_userAddressToConnectionTable.clear();
		super.removeSLSPs();
	}
}
