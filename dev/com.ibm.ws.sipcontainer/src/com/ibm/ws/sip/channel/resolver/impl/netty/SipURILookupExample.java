/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.channel.resolver.impl.netty;

import com.ibm.wsspi.sip.channel.resolver.SIPUri;
import com.ibm.wsspi.sip.channel.resolver.SipURILookup;
import com.ibm.wsspi.sip.channel.resolver.SipURILookupCallback;
import com.ibm.wsspi.sip.channel.resolver.SipURILookupException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Hashtable;
import java.lang.Thread;
import java.lang.Integer;

/**
 * Class used to demonstrate Interface for RFC 3263 support.  RFC 3263
 * defines how to resolve Sip URI's to a contactable Address, Port, Transport 
 * 3 tuple via a naming service. 
 * <p>
 * This class implements the {@link SipURILookupCallback} interface which 
 * is used to receive asynchronous responses from the resolver.
 */
public class SipURILookupExample implements SipURILookupCallback, Runnable {
	
	/** Hashtable used to store outstanding resolver requests */
	private Hashtable<Integer, SipURILookup> _requestTable;
	
	/** Constructor */
	private SipURILookupExample (){
		_requestTable = new Hashtable(); // initial capacity 11
	}
	
	/**
	  * Main 
	  */
	public static void main(String [] args){
		/** Construct the class and start it running */
		SipURILookupExample re = new SipURILookupExample();
		new Thread(re).start();
	}
	/**
	  *  Handles the construction of the SipURILookup and any synchronous
	  *  responses which may come back from the resolver.
	  *  <P>
	  *  Caches the request for later retrieval when a response 
	  *  comes back or for timeout logic.
	  *  
	  *  @param uri the SIPUri which we want to resolve via a naming service
	  */
	private void resolveUri (SIPUri uri) {
		
		/** Construct the request */
		SipURILookup request = SipResolverService.getInstance((SipURILookupCallback)this, uri);
		/** Invoke the naming service and check for synchronous response */
		try {
			if (request.lookup()){
				/** Get the answer */
				handleSuccessfulResolution(request);
			}
			else {
			/** Response will be returned asynchronously; cache the request */
			_requestTable.put(new Integer(request.hashCode()),request );
			}
		}
		catch (SipURILookupException e){
			System.out.println(e.getMessage());
		}	
	
	}
	
	/**
	  *  This is the success callback from the {@link SipURILookupCallback} Interface.  
	  *  When the resolver has a response to a request, the resolver will
	  *  dispatch the SipURILookup object back to the caller on a resolver thread via
	  *  this method
	  *    
	  *  @param sl the original SipURILookup object
	  */
	public void complete(SipURILookup sl) {
		
		/** do something with a successful resolution */
		handleSuccessfulResolution(sl);
				
		/** remove the request from the Hashtable */
		_requestTable.remove(new Integer (sl.hashCode()));
	}
	
	/**
	  *  This is the exception callback from the {@link SipURILookupCallback} Interface.  
	  *  When the resolver generates an exception to a request, the resolver will
	  *  dispatch the SipURILookup object back to the caller on a resolver thread via
	  *  this method
	  *    
	  *  @param sl the original SipURILookup object
	  *  @param e the exception generated by the SipURILookup
	  */
	public void error(SipURILookup sl, SipURILookupException e){
		/** local used if the request did not resolve */
		SIPUri         suri = null;
				
		suri = (SIPUri)sl.getSipURI();
		System.out.println(suri.getHost() + ": " +  e.getMessage());
		
		/** remove the request from the Hashtable */
		_requestTable.remove(new Integer (sl.hashCode()));

	}
	
	/**
	  *  Local method to handle a successful naming resolution for a SIPUri.
	  *  <p>
	  *  Merely prints to system out what the URI resolved to.
	  *    
	  *  @param request  request which triggered this event
	  *  @param response response to the request
	  */
    private void handleSuccessfulResolution(SipURILookup request){
		/** Used to hold the array of SIPUri objects in the response */
		ArrayList 	   al = null;
		/** Iterator */
		Iterator  	   i = null;
		/** The SIPUri object which holds the host, port and transport members */
		SIPUri  	   answer = null;
		/** The SIPUri object in the request */
		SIPUri         suri = null;
		
		/** get the SIPUri which was in the request */
		suri = (SIPUri)request.getSipURI();
		
		System.out.println("SIPUri sip:user@" + suri.getHost() + " resolved to");
		
		/** An ArrayList of SIPUri objects will be passed back in a successful response */
		/** The SIPUri objects will contain the host, port, and transport for what the  */
		/** original SIPUri resolved to.  See RFC 3263									*/
		al = (ArrayList)request.getAnswer();
		i = al.iterator();
		while (i.hasNext()){
			answer = (SIPUri)i.next();
			System.out.println("Transport: " + answer.getTransport() + "\n" +
							   "Address: " + answer.getHost() + "\n" +
							   "Port: " + answer.getPort());
		}
	}
	
	/**
	  *  
	  *  run method called from main 
	  */
	public void run(){
		System.out.println("Entering SipResolverExample::run");
		
		SIPUri suri = SIPUri.createSIPUri(null);
		suri.setHost("example.com");
		suri.setTransport(null);
		suri.setPortInt(-1);
		
		resolveUri(suri);
		
		SIPUri suri2 = SIPUri.createSIPUri(null);
		suri2.setHost("another.com");
		suri2.setTransport(null);
		suri2.setPortInt(-1);
		
		resolveUri(suri2);
		
		SIPUri suri3 = SIPUri.createSIPUri(null);
		suri3.setHost("doesnotresolve.com");
		suri3.setTransport(null);
		suri3.setPortInt(-1);
		
		resolveUri(suri3);

		System.out.println("Exiting SipResolverExample::run");		
	}

}
