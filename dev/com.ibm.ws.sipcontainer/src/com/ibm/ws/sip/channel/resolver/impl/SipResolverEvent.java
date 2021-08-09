/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.channel.resolver.impl;

import java.util.EventListener;

import com.ibm.ws.sip.channel.resolver.dns.impl.DnsMessage;

/**
 * Class to represent a Sip Resolver Event
 * <p>
 * Within the Sip Resolver Event class is a type of event the class represents, 
 * request, response and a listener, interested in the reponse.
 *  
 */
public class SipResolverEvent {
	/** type of event */
	private short _type;
	/** listener interested in this event */
	private EventListener _listener;
	/** request which triggered this event */
	private DnsMessage  _req;
	/** response to request */
	private DnsMessage _resp;
	
	/** Constants representing event types */
	protected static final short NAMING_RESOLUTION 		  = 0x0001;
	protected static final short NAMING_EXCEPTION         = 0x0002;
	protected static final short NAMING_TRANSPORT_RETRY   = 0x0003;
	protected static final short NAMING_TRANSPORT_FAILURE = 0x0004;
    protected static final short NAMING_TRY_TCP           = 0x0005;
    protected static final short NAMING_FAILURE           = 0x0006;
	
	/** 
	 * Constructor 
	 */
	protected SipResolverEvent() {
	}
	
	/**
	 * Method to set the event type
	 * 
	 * @param s the event type to set
	 */
	protected void setType(short s ){
		_type = s;
	}
	
	/**
	 * Method to return the event type 
	 * 
	 * @return the event type
	 */
	protected short getType(){
		return _type;
	}
	
	/**
	 * Method to set the listener for this event 
	 * 
	 * @param el the listener who implements the {@link EventListener} interface
	 */
	protected void setListener(EventListener el){
		_listener = el;
	}
	
	/**
	 * Method to get the listener for this event
	 * 
	 * @return the listener
	 */
	public EventListener getListener(){
		return _listener;
	}
	
	/**
	 * Method to set the request for this event
	 * 
	 * @param rm {@DnsMessage} which represents the request
	 */
	protected void setRequest(DnsMessage msg){
		_req = msg;
	}
	
	/**
	 * Method to get the request for this event
	 *  
	 * @return {@DnsMessage} which represents the request
	 */
	public DnsMessage getRequest(){
		return _req;
	}
	
	/**
	 * Method to set the response for this event
	 * 
	 * @param rm {@DnsMessage} which represents the response
	 */
	protected void setResponse(DnsMessage msg){
		_resp = msg;
	}
	
	/**
	 * Method to get the response for this event
	 * 
	 * @return {DnsMessage} representing the response
	 */
	public DnsMessage getResponse(){
		return _resp;
	}
	
	/**
	 * Method to indicate whether the event represents a successful 
	 * naming resolution.
	 * 
	 * @return true if event is a successful resolution, false otherwise
	 */
	public boolean successfulResolution() {
		if (this.getType() == SipResolverEvent.NAMING_RESOLUTION){
			return true;
		}
		else {
			return false;
		}
	}
}