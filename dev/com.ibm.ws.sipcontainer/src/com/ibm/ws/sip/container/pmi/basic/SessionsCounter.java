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
package com.ibm.ws.sip.container.pmi.basic;

import jain.protocol.ip.sip.message.Request;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.sip.SipServletResponse;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.message.RequestImpl;
import com.ibm.ws.sip.container.pmi.RequestModuleInterface;
import com.ibm.ws.sip.container.pmi.ResponseModuleInterface;
import com.ibm.ws.sip.container.pmi.SipMessagePMIEntry;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.properties.CoreProperties;

/**
 * @author anat, Mar 22, 2005
 */
public class SessionsCounter {
  
	/**
     * Class Logger.
     */
    private static final transient LogMgr c_logger = Log.get(SessionsCounter.class);
    
	private static final String IN = "Inbound ";
	private static final String OUT = "Outbound ";
	private static final String REQ = " Request";
	private static final String RES = " Response";
	
	/**
     * Other methods key
     */
	private static final String OTHER_METHOD = "OTHER_METHOD";

	/**
     * Other codes key
     */
	private static final int OTHER_CODE = 0;
    
    /**  Sip Application Sessions counter that count SipApplicationSessions */
    private long _sipAppSessions = 0;
    
    /**  Sip Sessions counter that count SipApplicationSessions */
    private long _sipSessions = 0;
    
    /**  Sip Sessions counter lock */
    private Object _sipSessionsSynchronizer = new Object();
    
    /**  Sip App. Sessions counter lock */
    private Object _sipAppSessionsSynchronizer = new Object();
    
    /**
	 * Table of inbound Request counters.
	 * The key is the method name and the value is SipMessagePMIEntry.
	 */
	private Map<String, SipMessagePMIEntry> _inReqTable = new HashMap<String, SipMessagePMIEntry>();
	
	/**
	 * Table of outbound Request counters.
	 * The key is the method name and the value is SipMessagePMIEntry.
	 */
	private Map<String, SipMessagePMIEntry> _outReqTable = new HashMap<String, SipMessagePMIEntry>();
	
	/**
	 * Table of inbound Response counters.
	 * The key is the response code and the value is SipMessagePMIEntry.
	 */
	private Map<String, SipMessagePMIEntry> _inResTable = new HashMap<String, SipMessagePMIEntry>();
	
	/**
	 * Table of outbound Request counters.
	 * The key is the response code and the value is SipMessagePMIEntry.
	 */
	private Map<String, SipMessagePMIEntry> _outResTable = new HashMap<String, SipMessagePMIEntry>();
	
	/**
	 * Table of all SIP Message indexed by statistic ID
	 * The key is the statistic ID and the value is SipMessagePMIEntry.
	 * <String key, SipMessagePMIEntry value>
	 */
	private Map<Integer, SipMessagePMIEntry> _messageTable = new HashMap<Integer, SipMessagePMIEntry>();
         
    /**
     * Enables printing messages to the SystemOut.log whenever the 
     * counter % _tracePMIModulus == 0 . Enables printing load messages without
     * causing performance degradation. A value of -1 indicates that no messages
     * should be printed.  
     */
    private int _tracePMIModulus = CoreProperties.TRACE_PMI_MODULUS_DEFAULT; 
    
    /**
     * Value of last printed session count to log. 
     */
    private long _lastSipSessionCountPrinted = 0; 
    
    /**
     * Value of last printed application session count to log. 
     */
    private long _lastAppSessionCountPrinted = 0;
    
    /**
     * Array of response codes in String value.
     * This was created in order to avoid unnecessary String creation
     * for example: _responseCodes[200]="200";
     */
    private String[] _responseCodes = new String[1000];
    
    /**
     * CTOR
     */
    public SessionsCounter() {
        //read additional configuration setting
        readConfigSettings();
        
        // init all SIP Messages tables
        initRequestTables();
        initResponseTables();
    }

    /**
     * Increment number of the SipApplicationSessions
     *  
     */
    public void sipAppSessionIncrement() {
    	synchronized(_sipAppSessionsSynchronizer){
    		_sipAppSessions ++;
    	}
        traceAppSessionCount();
    }
    
    /**
     * Decrement number of the SipApplicationSessions
     *  
     */
    public void sipAppSessionDecrement() {
    	synchronized(_sipAppSessionsSynchronizer){
    		_sipAppSessions --;
    	}
        traceAppSessionCount();
    }
    
    /**
     * Increment number of the SipSessions
     *  
     */
    public void sipSessionIncrement() {
    	synchronized(_sipSessionsSynchronizer){
    		_sipSessions ++;
    	}
        traceSipSessionsCount();
    }
    
    /**
     * Decrement number of the SipSessions
     *  
     */
    public void sipSessionDecrement() {
    	synchronized(_sipSessionsSynchronizer){
    		_sipSessions --;
    	}
        traceSipSessionsCount();
    }
    
    /**
     * Increment number of the inbound requests in counter according 
     * to method name
     *  
     */
    public void inboundRequest(String method) {
    	updateRequestTable(_inReqTable, method);
    }
    
    /**
     * Increment number of the outbound requests in counter according 
     * to method name
     *  
     */
    public void outboundRequest(String method) {
    	updateRequestTable(_outReqTable, method);
    }
    
    /**
     * Update request method counter in specified table
     * @param requestTable
     * @param method
     */
    private void updateRequestTable(Map<String, SipMessagePMIEntry> requestTable, 
    								String method) {
		SipMessagePMIEntry entry = requestTable.get(method);
		if(entry == null){
			entry = requestTable.get(OTHER_METHOD);
		}
		
		entry.increment();
		
		traceCounter(entry.getDescription(), entry.getCounter());
	}
    
    /**
     * Increment number of the inbound responses in counter according 
     * to state code
     *  
     */
    public void inboundResponse(int code) {
    	updateResponseTable(_inResTable, code, IN);
    }
    
    /**
     * Increment number of the outbound responses in counter according 
     * to state code
     *  
     */
    public void outboundResponse(int code) {
    	updateResponseTable(_outResTable, code, OUT);
    }
    
    public void updateCounters(){
    	Iterator<SipMessagePMIEntry> iter = _messageTable.values().iterator();
    	while (iter.hasNext()) {
			SipMessagePMIEntry element = iter.next();
			if(element!= null){
				element.update();
			}
		}
    }
    
    /**
     * Update request method counter in specified table
     * @param responseTable
     * @param code
     */
    private void updateResponseTable(Map<String, SipMessagePMIEntry> responseTable, 
    								int code, 
    								String direction) {
		String key = getResponseCode(code);
		SipMessagePMIEntry entry = responseTable.get(key);
		if(entry == null){
			entry = responseTable.get(getResponseCode(OTHER_CODE));
		}
		
		entry.increment();
		
		traceCounter(entry.getDescription(), entry.getCounter());
	}
    
    /**
     * Retrieve response code String value.
     * 
     * @param code The response code
     * @return The response code String value
     */
    private String getResponseCode(int code) {
		String respCode = _responseCodes[code];
		
		// create String value and save it for next usage
		if(respCode == null){
			respCode = Integer.toString(code);
			_responseCodes[code] = respCode;
		}
		
		return respCode;
	}
    
    /**
     * Creates entries for all Inbound and Outbound request and init tables.
     */
    private void initRequestTables() {
    	initReqEntry(OTHER_METHOD, 
    			RequestModuleInterface.INBOUND_OTHER,
    			RequestModuleInterface.OUTBOUND_OTHER);
    	
    	initReqEntry(Request.REGISTER,
    			RequestModuleInterface.INBOUND_REGISTER,
    			RequestModuleInterface.OUTBOUND_REGISTER);
    	
    	initReqEntry(Request.INVITE,
    			RequestModuleInterface.INBOUND_INVITE,
    			RequestModuleInterface.OUTBOUND_INVITE);
    	
    	initReqEntry(Request.ACK, 
    			RequestModuleInterface.INBOUND_ACK,
    			RequestModuleInterface.OUTBOUND_ACK);
    	
    	initReqEntry(Request.OPTIONS, 
    			RequestModuleInterface.INBOUND_OPTIONS,
    			RequestModuleInterface.OUTBOUND_OPTIONS);
    	
    	initReqEntry(Request.BYE, 
    			RequestModuleInterface.INBOUND_BYE,
    			RequestModuleInterface.OUTBOUND_BYE);
    	
    	initReqEntry(Request.CANCEL, 
    			RequestModuleInterface.INBOUND_CANCEL,
    			RequestModuleInterface.OUTBOUND_CANCEL);
    	
    	initReqEntry(RequestImpl.PRACK, 
    			RequestModuleInterface.INBOUND_PRACK,
    			RequestModuleInterface.OUTBOUND_PRACK);
    	
    	initReqEntry(RequestImpl.INFO, 
    			RequestModuleInterface.INBOUND_INFO,
    			RequestModuleInterface.OUTBOUND_INFO);
    	
    	initReqEntry(RequestImpl.SUBSCRIBE, 
    			RequestModuleInterface.INBOUND_SUBSCRIBE,
    			RequestModuleInterface.OUTBOUND_SUBSCRIBE);
    	
    	initReqEntry(RequestImpl.NOTIFY, 
    			RequestModuleInterface.INBOUND_NOTIFY,
    			RequestModuleInterface.OUTBOUND_NOTIFY);
    	
    	initReqEntry(RequestImpl.MESSAGE, 
    			RequestModuleInterface.INBOUND_MESSAGE,
    			RequestModuleInterface.OUTBOUND_MESSAGE);
    	
    	initReqEntry(RequestImpl.PUBLISH, 
    			RequestModuleInterface.INBOUND_PUBLISH,
    			RequestModuleInterface.OUTBOUND_PUBLISH);
    	
    	initReqEntry(RequestImpl.REFER, 
    			RequestModuleInterface.INBOUND_REFER,
    			RequestModuleInterface.OUTBOUND_REFER);
    	
    	initReqEntry(RequestImpl.UPDATE, 
    			RequestModuleInterface.INBOUND_UPDATE,
    			RequestModuleInterface.OUTBOUND_UPDATE);
	}
    
    /**
     * Creates statistic entries and init tables.
     * 
     * @param table The specific message type table
     * @param key The key for the specific message type table
     * @param statisticId The statistic ID
     * @param desc The Statistic description
     */
    private void initEntry(Map<String, SipMessagePMIEntry> table,
						  String key, 
						  int statisticId, 
						  String desc) {
    	SipMessagePMIEntry entry = new SipMessagePMIEntry(statisticId,desc);
    	table.put(key,entry);
    	_messageTable.put(statisticId, entry);
	}
    
    /**
     * Creates entries for all Inbound and Outbound responses and init tables.
     */
    private void initResponseTables() {
    	initResEntry(OTHER_CODE,
    			ResponseModuleInterface.INBOUND_OTHER,
    			ResponseModuleInterface.OUTBOUND_OTHER);
    	
    	// Informational status codes - 1xx
    	initResEntry(SipServletResponse.SC_TRYING, 
    			ResponseModuleInterface.INBOUND_TRYING,
    			ResponseModuleInterface.OUTBOUND_TRYING);
    	
		initResEntry(SipServletResponse.SC_RINGING, 
				ResponseModuleInterface.INBOUND_RINGING,
				ResponseModuleInterface.OUTBOUND_RINGING);
		
		initResEntry(SipServletResponse.SC_CALL_BEING_FORWARDED, 
				ResponseModuleInterface.INBOUND_CALL_BEING_FORWARDED, 
				ResponseModuleInterface.OUTBOUND_CALL_BEING_FORWARDED );
		
		initResEntry(SipServletResponse.SC_CALL_QUEUED, 
				ResponseModuleInterface.INBOUND_CALL_QUEUED, 
				ResponseModuleInterface.OUTBOUND_CALL_QUEUED );
		
		initResEntry(SipServletResponse.SC_SESSION_PROGRESS, 
				ResponseModuleInterface.INBOUND_SESSION_PROGRESS, 
				ResponseModuleInterface.OUTBOUND_SESSION_PROGRESS );
		
		// Success status codes - 2xx
		initResEntry(SipServletResponse.SC_OK, 
				ResponseModuleInterface.INBOUND_OK, 
				ResponseModuleInterface.OUTBOUND_OK );
		
		initResEntry(SipServletResponse.SC_ACCEPTED, 
				ResponseModuleInterface.INBOUND_ACCEPTED, 
				ResponseModuleInterface.OUTBOUND_ACCEPTED );
		
		initResEntry(SipServletResponse.SC_NO_NOTIFICATION,
				ResponseModuleInterface.INBOUND_NO_NOTIFICATION,
				ResponseModuleInterface.OUTBOUND_NO_NOTIFICATION );
		
		// Redirection status codes - 3xx
		initResEntry(SipServletResponse.SC_MULTIPLE_CHOICES, 
				ResponseModuleInterface.INBOUND_MULTIPLE_CHOICES, 
				ResponseModuleInterface.OUTBOUND_MULTIPLE_CHOICES );
		
		initResEntry(SipServletResponse.SC_MOVED_PERMANENTLY, 
				ResponseModuleInterface.INBOUND_MOVED_PERMANENTLY, 
				ResponseModuleInterface.OUTBOUND_MOVED_PERMANENTLY );
		
		initResEntry(SipServletResponse.SC_MOVED_TEMPORARILY, 
				ResponseModuleInterface.INBOUND_MOVED_TEMPORARILY, 
				ResponseModuleInterface.OUTBOUND_MOVED_TEMPORARILY );
		
		initResEntry(SipServletResponse.SC_USE_PROXY, 
				ResponseModuleInterface.INBOUND_USE_PROXY, 
				ResponseModuleInterface.OUTBOUND_USE_PROXY );
		
		initResEntry(SipServletResponse.SC_ALTERNATIVE_SERVICE,
				ResponseModuleInterface.INBOUND_ALTERNATIVE_SERVICE, 
				ResponseModuleInterface.OUTBOUND_ALTERNATIVE_SERVICE );
		
		// Client failure - 4xx
		initResEntry(SipServletResponse.SC_BAD_REQUEST, 
				ResponseModuleInterface.INBOUND_BAD_REQUEST, 
				ResponseModuleInterface.OUTBOUND_BAD_REQUEST );
		
		initResEntry(SipServletResponse.SC_UNAUTHORIZED, 
				ResponseModuleInterface.INBOUND_UNAUTHORIZED, 
				ResponseModuleInterface.OUTBOUND_UNAUTHORIZED );
		
		initResEntry(SipServletResponse.SC_PAYMENT_REQUIRED, 
				ResponseModuleInterface.INBOUND_PAYMENT_REQUIRED, 
				ResponseModuleInterface.OUTBOUND_PAYMENT_REQUIRED );
		
		initResEntry(SipServletResponse.SC_FORBIDDEN, 
				ResponseModuleInterface.INBOUND_FORBIDDEN, 
				ResponseModuleInterface.OUTBOUND_FORBIDDEN );
		
		initResEntry(SipServletResponse.SC_NOT_FOUND, 
				ResponseModuleInterface.INBOUND_NOT_FOUND, 
				ResponseModuleInterface.OUTBOUND_NOT_FOUND );
		
		initResEntry(SipServletResponse.SC_METHOD_NOT_ALLOWED, 
				ResponseModuleInterface.INBOUND_METHOD_NOT_ALLOWED, 
				ResponseModuleInterface.OUTBOUND_METHOD_NOT_ALLOWED );
		
		initResEntry(SipServletResponse.SC_NOT_ACCEPTABLE, 
				ResponseModuleInterface.INBOUND_NOT_ACCEPTABLE, 
				ResponseModuleInterface.OUTBOUND_NOT_ACCEPTABLE );
		
		initResEntry(SipServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED, 
				ResponseModuleInterface.INBOUND_PROXY_AUTHENTICATION_REQUIRED,
				ResponseModuleInterface.OUTBOUND_PROXY_AUTHENTICATION_REQUIRED );
		
		initResEntry(SipServletResponse.SC_REQUEST_TIMEOUT, 
				ResponseModuleInterface.INBOUND_REQUEST_TIMEOUT, 
				ResponseModuleInterface.OUTBOUND_REQUEST_TIMEOUT );
		
		initResEntry(SipServletResponse.SC_CONFLICT, 
				ResponseModuleInterface.INBOUND_CONFLICT, 
				ResponseModuleInterface.OUTBOUND_CONFLICT );
		
		initResEntry(SipServletResponse.SC_GONE, 
				ResponseModuleInterface.INBOUND_GONE, 
				ResponseModuleInterface.OUTBOUND_GONE );
		
		initResEntry(SipServletResponse.SC_CONDITIONAL_REQUEST_FAILED, 
				ResponseModuleInterface.INBOUND_CONDITIONAL_REQUEST_FAILED,
				ResponseModuleInterface.OUTBOUND_CONDITIONAL_REQUEST_FAILED );
		
		initResEntry(SipServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, 
				ResponseModuleInterface.INBOUND_REQUEST_ENTITY_TOO_LARGE, 
				ResponseModuleInterface.OUTBOUND_REQUEST_ENTITY_TOO_LARGE );
		
		initResEntry(SipServletResponse.SC_REQUEST_URI_TOO_LONG, 
				ResponseModuleInterface.INBOUND_REQUEST_URI_TOO_LONG, 
				ResponseModuleInterface.OUTBOUND_REQUEST_URI_TOO_LONG );
		
		initResEntry(SipServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, 
				ResponseModuleInterface.INBOUND_UNSUPPORTED_MEDIA_TYPE, 
				ResponseModuleInterface.OUTBOUND_UNSUPPORTED_MEDIA_TYPE );
		
		initResEntry(SipServletResponse.SC_UNSUPPORTED_URI_SCHEME, 
				ResponseModuleInterface.INBOUND_UNSUPPORTED_URI_SCHEME, 
				ResponseModuleInterface.OUTBOUND_UNSUPPORTED_URI_SCHEME );
		
		initResEntry(SipServletResponse.SC_UNKNOWN_RESOURCE_PRIORITY,
				ResponseModuleInterface.INBOUND_UNKNOWN_RESOURCE_PRIORITY,
				ResponseModuleInterface.OUTBOUND_UNKNOWN_RESOURCE_PRIORITY );
		
		initResEntry(SipServletResponse.SC_BAD_EXTENSION, 
				ResponseModuleInterface.INBOUND_BAD_EXTENSION, 
				ResponseModuleInterface.OUTBOUND_BAD_EXTENSION );
		
		initResEntry(SipServletResponse.SC_EXTENSION_REQUIRED, 
				ResponseModuleInterface.INBOUND_EXTENSION_REQUIRED, 
				ResponseModuleInterface.OUTBOUND_EXTENSION_REQUIRED );
		
		initResEntry(SipServletResponse.SC_SESSION_INTERVAL_TOO_SMALL, 
				ResponseModuleInterface.INBOUND_SESSION_INTERVAL_TOO_SMALL,
				ResponseModuleInterface.OUTBOUND_SESSION_INTERVAL_TOO_SMALL );
		
		initResEntry(SipServletResponse.SC_INTERVAL_TOO_BRIEF, 
				ResponseModuleInterface.INBOUND_INTERVAL_TOO_BRIEF, 
				ResponseModuleInterface.OUTBOUND_INTERVAL_TOO_BRIEF );
		
		initResEntry(SipServletResponse.SC_BAD_LOCATION_INFORMATION, 
				ResponseModuleInterface.INBOUND_BAD_LOCATION_INFORMATION,
				ResponseModuleInterface.OUTBOUND_BAD_LOCATION_INFORMATION );	
		
		initResEntry(SipServletResponse.SC_USE_IDENTITY_HEADER, 
				ResponseModuleInterface.INBOUND_USE_IDENTITY_HEADER,
				ResponseModuleInterface.OUTBOUND_USE_IDENTITY_HEADER );
		
		initResEntry(SipServletResponse.SC_PROVIDE_REFERER_IDENTITY, 
				ResponseModuleInterface.INBOUND_PROVIDE_REFERRER_IDENTITY,
				ResponseModuleInterface.OUTBOUND_PROVIDE_REFERRER_IDENTITY );
		
		initResEntry(SipServletResponse.SC_ANONYMILY_DISALLOWED, 
				ResponseModuleInterface.INBOUND_ANONYMILY_DISALLOWED,
				ResponseModuleInterface.OUTBOUND_ANONYMILY_DISALLOWED );
		
		initResEntry(SipServletResponse.SC_BAD_IDENTITY_INFO, 
				ResponseModuleInterface.INBOUND_BAD_IDENTITY_INFO,
				ResponseModuleInterface.OUTBOUND_BAD_IDENTITY_INFO );
		
		initResEntry(SipServletResponse.SC_UNSUPPORTED_CERTIFICATE, 
				ResponseModuleInterface.INBOUND_UNSUPPORTED_CERTIFICATE,
				ResponseModuleInterface.OUTBOUND_UNSUPPORTED_CERTIFICATE );
		
		initResEntry(SipServletResponse.SC_INVALID_IDENTITY_HEADER, 
				ResponseModuleInterface.INBOUND_INVALID_IDENTITY_HEADER,
				ResponseModuleInterface.OUTBOUND_INVALID_IDENTITY_HEADER );
		
		initResEntry(SipServletResponse.SC_TEMPORARLY_UNAVAILABLE, 
				ResponseModuleInterface.INBOUND_TEMPORARLY_UNAVAILABLE, 
				ResponseModuleInterface.OUTBOUND_TEMPORARLY_UNAVAILABLE );
		
		initResEntry(SipServletResponse.SC_CALL_LEG_DONE, 
				ResponseModuleInterface.INBOUND_CALL_LEG_DONE, 
				ResponseModuleInterface.OUTBOUND_CALL_LEG_DONE );
		
		initResEntry(SipServletResponse.SC_LOOP_DETECTED, 
				ResponseModuleInterface.INBOUND_LOOP_DETECTED, 
				ResponseModuleInterface.OUTBOUND_LOOP_DETECTED );
		
		initResEntry(SipServletResponse.SC_TOO_MANY_HOPS, 
				ResponseModuleInterface.INBOUND_TOO_MANY_HOPS, 
				ResponseModuleInterface.OUTBOUND_TOO_MANY_HOPS );
		
		initResEntry(SipServletResponse.SC_ADDRESS_INCOMPLETE,
				ResponseModuleInterface.INBOUND_ADDRESS_INCOMPLETE, 
				ResponseModuleInterface.OUTBOUND_ADDRESS_INCOMPLETE );
		
		initResEntry(SipServletResponse.SC_AMBIGUOUS, 
				ResponseModuleInterface.INBOUND_AMBIGUOUS, 
				ResponseModuleInterface.OUTBOUND_AMBIGUOUS );
		
		initResEntry(SipServletResponse.SC_BUSY_HERE, 
				ResponseModuleInterface.INBOUND_BUSY_HERE, 
				ResponseModuleInterface.OUTBOUND_BUSY_HERE );
		
		initResEntry(SipServletResponse.SC_REQUEST_TERMINATED, 
				ResponseModuleInterface.INBOUND_REQUEST_TERMINATED, 
				ResponseModuleInterface.OUTBOUND_REQUEST_TERMINATED );
		
		initResEntry(SipServletResponse.SC_NOT_ACCEPTABLE_HERE, 
				ResponseModuleInterface.INBOUND_NOT_ACCEPTABLE_HERE, 
				ResponseModuleInterface.OUTBOUND_NOT_ACCEPTABLE_HERE );
		
		initResEntry(SipServletResponse.SC_BAD_EVENT, 
				ResponseModuleInterface.INBOUND_BAD_EVENT,
				ResponseModuleInterface.OUTBOUND_BAD_EVENT );
		
		initResEntry(SipServletResponse.SC_REQUEST_PENDING, 
				ResponseModuleInterface.INBOUND_REQUEST_PENDING, 
				ResponseModuleInterface.OUTBOUND_REQUEST_PENDING );
		
		initResEntry(SipServletResponse.SC_UNDECIPHERABLE, 
				ResponseModuleInterface.INBOUND_UNDECIPHERABLE, 
				ResponseModuleInterface.OUTBOUND_UNDECIPHERABLE );
		
		initResEntry(SipServletResponse.SC_SECURITY_AGREEMENT_REQUIRED, 
				ResponseModuleInterface.INBOUND_SECURITY_AGREEMENT_REQUIRED,
				ResponseModuleInterface.OUTBOUND_SECURITY_AGREEMENT_REQUIRED );
		
		// Server failure - 5xx
		initResEntry(SipServletResponse.SC_SERVER_INTERNAL_ERROR, 
				ResponseModuleInterface.INBOUND_SERVER_INTERNAL_ERROR, 
				ResponseModuleInterface.OUTBOUND_SERVER_INTERNAL_ERROR );
		
		initResEntry(SipServletResponse.SC_NOT_IMPLEMENTED, 
				ResponseModuleInterface.INBOUND_NOT_IMPLEMENTED, 
				ResponseModuleInterface.OUTBOUND_NOT_IMPLEMENTED );
		
		initResEntry(SipServletResponse.SC_BAD_GATEWAY, 
				ResponseModuleInterface.INBOUND_BAD_GATEWAY, 
				ResponseModuleInterface.OUTBOUND_BAD_GATEWAY );
		
		initResEntry(SipServletResponse.SC_SERVICE_UNAVAILABLE, 
				ResponseModuleInterface.INBOUND_SERVICE_UNAVAILABLE, 
				ResponseModuleInterface.OUTBOUND_SERVICE_UNAVAILABLE );
		
		initResEntry(SipServletResponse.SC_SERVER_TIMEOUT, 
				ResponseModuleInterface.INBOUND_SERVER_TIMEOUT, 
				ResponseModuleInterface.OUTBOUND_SERVER_TIMEOUT );
		
		initResEntry(SipServletResponse.SC_VERSION_NOT_SUPPORTED, 
				ResponseModuleInterface.INBOUND_VERSION_NOT_SUPPORTED, 
				ResponseModuleInterface.OUTBOUND_VERSION_NOT_SUPPORTED );
		
		initResEntry(SipServletResponse.SC_MESSAGE_TOO_LARGE,
				ResponseModuleInterface.INBOUND_MESSAGE_TOO_LARGE, 
				ResponseModuleInterface.OUTBOUND_MESSAGE_TOO_LARGE );
		
		// Global failure - 6xx
		initResEntry(SipServletResponse.SC_BUSY_EVERYWHERE, 
				ResponseModuleInterface.INBOUND_BUSY_EVERYWHERE, 
				ResponseModuleInterface.OUTBOUND_BUSY_EVERYWHERE );
		
		initResEntry(SipServletResponse.SC_DECLINE, 
				ResponseModuleInterface.INBOUND_DECLINE, 
				ResponseModuleInterface.OUTBOUND_DECLINE );
		
		initResEntry(SipServletResponse.SC_DOES_NOT_EXIT_ANYWHERE, 
				ResponseModuleInterface.INBOUND_DOES_NOT_EXIT_ANYWHERE, 
				ResponseModuleInterface.OUTBOUND_DOES_NOT_EXIT_ANYWHERE );
		
		initResEntry(SipServletResponse.SC_NOT_ACCEPTABLE_ANYWHERE, 
				ResponseModuleInterface.INBOUND_NOT_ACCEPTABLE_ANYWHERE, 
				ResponseModuleInterface.OUTBOUND_NOT_ACCEPTABLE_ANYWHERE );
	}
    
    /**
     * Creates entries for all Inbound and Outbound responses and init tables.
     * @param code The response code
     * @param statisticId The statistic ID
     */
    private void initResEntry(int code, int statisticInId, int statisticOutId) {
    	initEntry(_inResTable,getResponseCode(code), 
    			statisticInId, IN + code + RES);
    	
    	initEntry(_outResTable, getResponseCode(code), 
    			statisticOutId, OUT + code + RES);
    }
    
    /**
     * Creates entries for all Inbound and Out Bound requests and init tables.
     * @param method The request method
     * @param statisticId The statistic ID
     */
    private void initReqEntry(String method, int statisticInId, int statisticOutId) {
    	initEntry(_inReqTable, method, statisticInId, IN + method + REQ);
    	initEntry(_outReqTable,method, statisticOutId, OUT + method + REQ);
    }
    
    /**
     * @return Returns the sipAppSessions.
     */
    public long getSipAppSessions() {
        synchronized(_sipAppSessionsSynchronizer) {
        return _sipAppSessions;
        }
    }
    
    /**
     * @return Returns the sipSessions.
     */
    public long getSipSessions() {
    	synchronized(_sipSessionsSynchronizer) {
        return _sipSessions;
    	}
    }
    
    /**
     * @return Reset the sipAppSessions counter value.
     */
    public void resetSipAppSessions() {
    	synchronized(_sipAppSessionsSynchronizer) {
    		_sipAppSessions = 0;
    	}
    }
    
    /**
     * @return Reset the sipSessions counter value.
     */
    public void resetSipSessions() {
    	synchronized(_sipSessionsSynchronizer){
        	_sipSessions = 0;
    	}
    }
    
    /**
     * Returns the inbound request count according to method name.
     * 
     * @return counter The long value of inbound request that were 
     * collected till now. In case there is no counter - return zero. 
     */
    public long getInboundRequestCount(String methodKey) {
    	long count = 0;
    	SipMessagePMIEntry entry = _inReqTable.get(methodKey);
    	if (entry!= null){
    		count = entry.getCounter();
    	}
    	return count;
    }
    
    /**
     * Returns the outbound request count according to method name.
     * 
     * @return counter The long value of inbound request that were 
     * collected till now. In case there is no counter - return zero. 
     */
    public long getOutboundRequestCount(String methodKey) {
    	long count = 0;
    	SipMessagePMIEntry entry = _outReqTable.get(methodKey);
    	if (entry!= null){
    		count = entry.getCounter();
    	}
    	return count;
    }
        
    /**
     * Returns the inbound response count according to response code number.
     * @return counter The long value of inbound response that were 
     * collected till now. In case there is no counter - return zero. 
     */
    public long getInboundResponseCount(int code) {
    	long count = 0;
    	SipMessagePMIEntry entry = _inResTable.get(getResponseCode(code));
    	if (entry!= null){
    		count = entry.getCounter();
    	}
    	return count;
    }
    
    /**
     * Returns the outbound response count according to response code number.
     * @return counter The long value of inbound response that were 
     * collected till now. In case there is no counter - return zero. 
     */
    public long getOutboundResponseCount(int code) {
    	long count = 0;
    	SipMessagePMIEntry entry = _outResTable.get(getResponseCode(code));
    	if (entry!= null){
    		count = entry.getCounter();
    	}
    	return count;
    }
    
	/**
     * Traces Session count to log according to the specified configuration settings
     */
    private void traceSipSessionsCount() {
    	synchronized(_sipSessionsSynchronizer){
	        if (_tracePMIModulus > 0
	        		&& _lastSipSessionCountPrinted != _sipSessions
	                && _sipSessions % _tracePMIModulus == 0) {
	            _lastSipSessionCountPrinted = _sipSessions;
	        }
    	}
    }

    /**
     * Traces App Session count to log according to the specified configuration settings
     */
    private void traceAppSessionCount() {
    	synchronized(_sipAppSessionsSynchronizer){
	        if (_tracePMIModulus > 0
	                && _lastAppSessionCountPrinted != _sipAppSessions
	                && _sipAppSessions % _tracePMIModulus == 0) {
	            _lastAppSessionCountPrinted = _sipAppSessions;
	        }
    	}
    }
        
    /**
     * Traces counter to log according to the specified configuration settings
     * @param description
     * @param counter
     */
    private void traceCounter(String description, long counter) {
        if (_tracePMIModulus > 0 ) {
        	// TODO to check and save last printed value
            System.out.println(this + description + " : " + counter);
        }
    }

    /**
     * Update the PMI trace modulus from configuration. 
     */
    private void readConfigSettings() {
        PropertiesStore store = PropertiesStore.getInstance();
        _tracePMIModulus = store.getProperties().getInt(
        		CoreProperties.TRACE_PMI_MODULUS);
    }	
}
