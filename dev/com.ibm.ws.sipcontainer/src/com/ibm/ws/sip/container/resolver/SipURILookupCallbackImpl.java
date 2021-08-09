/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.resolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.sip.resolver.DomainResolver;
import com.ibm.websphere.sip.resolver.DomainResolverListener;
import com.ibm.websphere.sip.resolver.exception.SipURIResolveException;
import com.ibm.ws.jain.protocol.ip.sip.address.SipURLImpl;
import com.ibm.ws.sip.container.events.EventsDispatcher;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.util.SipUtil;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.ws.sip.stack.internalapi.NaptrRequestListener;
//TODO Liberty change to Liberty channel framework classes
import com.ibm.wsspi.sip.channel.resolver.SIPUri;
import com.ibm.wsspi.sip.channel.resolver.SipURILookup;
import com.ibm.wsspi.sip.channel.resolver.SipURILookupCallback;
import com.ibm.wsspi.sip.channel.resolver.SipURILookupException;

/**
 * Sip Channel callback implementation for {@linkplain DomainResolver} calls.
 * 
 * @author Noam Almog
 */
public class SipURILookupCallbackImpl implements SipURILookupCallback {

	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(SipURILookupCallbackImpl.class);
	
	/**
	 * queried uri
	 */
	private SipURI _sipUri;
	
	/**
	 * resultset
	 */
	private List <SipURI> _results;
	
	/**
	 * indicator which is set when results are ready.
	 */
	private boolean _hasResults;
	
	
	/**
	 * true if the target URI was "corrected" to comply with the standard,
	 * before passing it to the NAPTR/SRV library for DNS lookup.
	 * "corrected" means changed from sip:..;transport=tls to sips:..;transport=tcp.
	 * if it was corrected before the lookup, it needs to be "un-corrected"
	 * after returning from DNS.
	 */
	private boolean _fixTransports;
	
	
	/**
	 * synch object for synchronous API.
	 */
	private final Object _syncObj;
	
	
	/**
	 * Application listener for asynchronous API
	 */
	private final DomainResolverListener _listener;
	
	/**
	 * Sip Stack Listener
	 */
	private final NaptrRequestListener _stackListener;
	
	/**
	 * Sip session which the asynchronous API was triggered from.
	 */
	private final SipSession _sipSession;
	
	
	private SipURILookupException _exception;
	
	
	/**
	 * Constructor for synchronous API
	 */
	public SipURILookupCallbackImpl() {
		_listener = null;
		_sipSession = null;
		_stackListener = null;
		_syncObj = new Object();
	}
	
	
	/**
	 * Constructor for asynchronous API
	 */	
	public SipURILookupCallbackImpl(SipURI sipUri, boolean fixTransports, DomainResolverListener listener, SipSession sipSession) {
		_stackListener = null;
		_syncObj = null;
		_sipUri = sipUri;
		_listener = listener;
		_results = null;
		_sipSession = sipSession;
		_fixTransports = fixTransports;
	}
	
	/**
	 * Constructor for stack API
	 */
	public SipURILookupCallbackImpl(NaptrRequestListener listener, boolean fixTransports) {
		_listener = null;
		_syncObj = null;
		_sipUri = null;
		_stackListener = listener;
		_results = null;
		_sipSession = null;
		_fixTransports = fixTransports;
	}
	

	/**
	 * Used for recycled object on ThreadLocal
	 */
	public void init(SipURI sipUri, boolean fixTransports) {
		reset();
		_sipUri = sipUri;
		_fixTransports = fixTransports;

	}
	
	public void waitForResults() throws InterruptedException {
		if (_syncObj != null) {
			synchronized (_syncObj) {
				if (!_hasResults) {
					_syncObj.wait();	
				}
			}
		}
	}
	
	/**
	 * reset all members, used for synchronously synch object.
	 */
	public void reset() {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "reset", "Reset called.");
		}
		
		_exception = null;
		_results = null;
		_hasResults = false;
		_sipUri = null;
		_fixTransports = false;
	}
	
	
	/**
	 * @see SipURILookupCallback#complete(SipURILookup)
	 */
	public void complete(SipURILookup request) {
		complete(request, false);
	}
	
	/**
	 * Success response handler
	 * 
	 * @param request
	 * @param exception
	 * @param isOnCurrentThread indicator which will signal EventListener if to dispatch on current
	 * app queue or dispatch the event.
	 */
	public void complete(SipURILookup request, boolean isOnCurrentThread) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "complete", "Success response received.");
		}
		
		if (_stackListener != null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "complete", "Dispatching stack success response");
			}
			
			// clone the response so we can remove elements from it without
			// affecting the instance that is cached in the library
			List<SIPUri> response = (List)request.getAnswer().clone();

			if (_fixTransports) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "complete", "fixing result set transport");
				}
				
				for (SIPUri sipUri : response) {
					// correct results
					sipUri.setScheme(SipUtil.SIP_SCHEME);
					sipUri.setTransport(SipUtil.TLS_TRANSPORT);
				}
			}			
			
			
			_stackListener.handleResolve(response);
		} else {
			// stack listener doesn't need to convert result, no need to go through
			// the whole cycle like in Sync/ASync scenario
			_results = convertResults(request.getAnswer());
			
			notifyWaitingThreads();
			
			if (_listener != null) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "complete", "Dispatching success response");
				}			
				EventsDispatcher.uriLookupComplete(_listener, _sipSession, _sipUri, _results, isOnCurrentThread);
			}
		}
	}
	

	/**
	 * Utility method used to convert Sip channel URI implementation to the JSR implementation.
	 * 
	 * @param response List of sip channel objects
	 * @return List of SipURI objects
	 */
	private final List<SipURI> convertResults(List<SIPUri> response) {
		if (c_logger.isTraceDebugEnabled()) {
			int num = (response != null) ? response.size() : 0;
			c_logger.traceDebug(this, "convertResults", "Received " + num + " results.");
		}
		
		if (response == null || response.isEmpty()) {
			return Collections.emptyList();
		}
		
		List<SipURI> result = new ArrayList<SipURI>(response.size());
		
		for (SIPUri sipUri : response) {
			SipURI tempUri = (SipURI)_sipUri.clone();
			
			tempUri.setHost(sipUri.getHost());
			tempUri.setPort(sipUri.getPortInt());
			
			String additionalParameters = sipUri.getAdditionalParms();
			
			//add the ibmttl to the uri if the custom property is enabled
			if (PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.SIP_RFC3263_ADD_TTL)){
				String ttl = getIBMTTL(additionalParameters);

				if (ttl != null) {
					tempUri.setParameter(DomainResolver.IBM_TTL_PARAM, ttl);
				}
			}
			
			// correct results
			if (_fixTransports) {
				sipUri.setScheme(SipUtil.SIP_SCHEME);
				sipUri.setTransport(SipUtil.TLS_TRANSPORT);
			}
			
			if (sipUri.getTransport() != null) {
				tempUri.setTransportParam(sipUri.getTransport());	
			} else {
				tempUri.removeParameter(SipURLImpl.TRANSPORT);
			}

			result.add(tempUri);
		}
		
		return result;
	}
	
	/**
	 * get the current ttl according to the library custom parameter 
	 * @param additionalParameters
	 * @return
	 */
	private String getIBMTTL(String additionalParameters) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "getIBMTTL", additionalParameters);
		}	
		String ttl = null;
		if (additionalParameters != null && additionalParameters.length() > 1) {
			int idx = additionalParameters.indexOf(DomainResolver.IBM_TTL_PARAM);
			if (idx > -1){
				additionalParameters = additionalParameters.substring(idx);
				int idx2 = additionalParameters.indexOf(";");
				if (idx2 > -1){
					additionalParameters = additionalParameters.substring(0, idx2);
				}
				String[] ttlArray = additionalParameters.split("=");
				if (ttlArray.length == 2){
					ttl = ttlArray[1].trim();
				}
			}
		}
		
		//set the ttl according to the current time if it was cached
		if (ttl != null){
			String[] ttlSplit = ttl.split("_");
			long ttlTime = Long.parseLong(ttlSplit[0]);
			long ttlDate = Long.parseLong(ttlSplit[1]);
			long ttlOffset = (System.currentTimeMillis() - ttlDate)/1000l;
			
			long ttlSec = Math.max(0, ttlTime - ttlOffset);
			
			ttl = String.valueOf(ttlSec);
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "getIBMTTL", ttl);
		}	
		return ttl;
	}
	
	/**
	 * Notify synchronous objects.
	 */
	private void notifyWaitingThreads() {
		if (_syncObj != null) {
			synchronized (_syncObj) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "notifyWaitingThreads", "Results ready.");
				}				
				_hasResults = true;
				_syncObj.notify();	
			}
		}
	}
	
	/**
	 * Indicator for query status.   
	 * @return true if query completed
	 */
	public boolean hasResults() {
		return _hasResults;
	}
	
	/**
	 * Getter for result set
	 */
	public List<SipURI> getResults() throws SipURIResolveException {
		if (!_hasResults)
			throw new SipURIResolveException("Results not ready.");
		
		return _results;
	}

	/**
	 * @see SipURILookupCallback#error(SipURILookup, SipURILookupException)
	 */	
	public void error(SipURILookup request, SipURILookupException exception) {
		error(request, exception, false);
	}

	/**
	 * Error response handler
	 * 
	 * @param request
	 * @param exception
	 * @param isOnCurrentThread indicator which will signal EventListener if to dispatch on current
	 * app queue or dispatch the event.
	 */
	 public void error(SipURILookup request, SipURILookupException exception, boolean isOnCurrentThread) {
		_exception = exception;
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "error", "Received exception " + exception);
			
			if (exception != null)
				exception.printStackTrace();
		}
		
		notifyWaitingThreads();
		
		if (_listener != null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "error", "Dispatching error response");
			}			
			EventsDispatcher.uriLookupError(_listener, _sipSession, _sipUri, exception, isOnCurrentThread);
		}
		
		if (_stackListener != null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "error", "Dispatching error response");
			}
			_stackListener.error(exception);
		}		
	}
	
	 public SipURILookupException getErrorException() {
		return _exception;
	}
	
	public boolean isErrorResponse() {
		return (_exception != null);
		//return true;//random - need to change
	}

}
