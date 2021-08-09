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
package com.ibm.ws.sip.container.proxy;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.ProxyAuthenticateHeader;
import jain.protocol.ip.sip.header.WWWAuthenticateHeader;
import jain.protocol.ip.sip.message.Response;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession.State;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipServletResponseImpl;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.util.SipUtil;

/**
 * @author yaronr
 *
 * Represnts the best response received bt a stateful proxy
 * 
 * From RFC 3261 (SIP standard)
 * If no 6xx class responses are present, the proxy SHOULD choose from 
 * 	the lowest response class stored in the response context.  The proxy MAY
 *  select any response within that chosen class.  The proxy SHOULD
 *  give preference to responses that provide information affecting
 *  resubmission of this request, such as 401, 407, 415, 420, and
 *  484 if the 4xx class is chosen.
 *
 */
public class StatefulProxyBestResponse
{
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = 
    					Log.get(StatefulProxyBestResponse.class);

    /**
     * Holds all known response values (range 300 to 599)
     * and their preference (the lowest get higher preferences
     */
    private static final int[][] c_responsesAndTheirPreference = {
        // 3xx response class
        {300, 300}, // Multiple Choices 
        {301, 303}, // Moved Permanently
        {302, 302}, // Moved Temporarily
        {305, 301}, // Use Proxy
        {380, 304}, // Alternative Service
        // 4xx response class
        {400, 410}, // Bad request   
        {401, 400}, // Unauthorized
        {402, 412}, // Payment Required
        {403, 414}, // Forbidden
        {404, 416}, // Not found
        {405, 418}, // Method not allowed
        {406, 420}, // Not acceptable
        {407, 401}, // Proxy Authentication required
        {408, 422}, // Request Timeout
        {409, 424}, // Conflict
        {410, 426}, // Gone
        {411, 428}, // Length required
        {413, 430}, // Request Entity Too Large
        {414, 432}, // Request-URI Too long
        {415, 402}, // Unsupported Media Type
        {420, 403}, // Bad extension
        {421, 434}, // Extension Required										 
        {480, 436}, // Temporarily Unavailable
        {481, 438}, // Call Leg/Transaction Does not exist
        {482, 440}, // Loop Detected
        {483, 442}, // Too Many hops
        {484, 404}, // Address Incomlpete
        {485, 444}, // Ambiguous
        {486, 446}, // Busy Here
        {487, 448}, // Request Canceled
        {488, 450}, // Not Acceptable Here
        // 5xx response class 
        {500, 501}, // Bad request   
        {501, 503}, // Unauthorized
        {502, 502}, // Payment Required
        {503, 506}, // Forbidden
        {504, 504}, // Service unavailable
        {505, 505}  // Version not supported
    };

    /**
     * Map for all responses and their values
     */
    private static Map c_responseMap = new HashMap(75);

    /**
     * The best response that arrived so far
     */
    private SipServletResponse m_bestResponse = null;
    
    /**
     * ProxyBranch which relates to the m_bestResponse;
     */
    private ProxyBranchImpl _proxyBranch = null;

    /**
     * Holds best reposnse preference (We prefer the lower value)
     */
    private int m_bestResponsePreference = 1000;

    static 
    {
    	
        int size = c_responsesAndTheirPreference.length;
  				
        for (int i = 0; i < size; i++)
        {
            c_responseMap.put(
                new Integer(c_responsesAndTheirPreference[i][0]),
                new Integer(c_responsesAndTheirPreference[i][1]));
        }
    }
    
    

    /**
     * Constructor
     */
    public StatefulProxyBestResponse()
    {
        
    }

    /**
     * Return the best response received so far
     * 
     * @return the best response
     */
    public SipServletResponse getBestResponse()
    {
        return m_bestResponse;
    }
    
    /**
     * Return the best response received so far
     * 
     * @return the proxyBranch of best response
     */
    public ProxyBranchImpl getProxyBranch()
    {
        return _proxyBranch;
    }

    /**
     * Helper method which compare between errorCode and existing best
     * response and decides which is better.
     * @param errorCode
     * @return -1 when errorCode shouldn't replace existing m_bestResponse.
     */
    public int getNewPreference(int errorCode){
        // -1 meaning that new errorCode is not the best response.
    	int newPrefernce = -1;
        
        // Provisional responses are not considered as best response
		if (errorCode >= 200) {
			
			newPrefernce = getPreference(errorCode);

			if(m_bestResponse != null && m_bestResponsePreference <= newPrefernce){
				newPrefernce = -1;
			}
		}
        
        return newPrefernce;
    }
    
    /**
     * Method which investigates incoming response and decides
     * if it should replace existing best response.
     * @param response
     */
    public void updateBestResponse(SipServletResponse response,ProxyBranchImpl branch)
    {
    	if (c_logger.isTraceDebugEnabled()) 
    	{
            int current = -1; 
            if(m_bestResponse != null)
            {
                current = m_bestResponse.getStatus();
            }
    	    c_logger.traceDebug(this, 
    	                        "updateBestResponse", "Current: " + current +
    	                        (m_bestResponse == null ? "" 
    	                        		: ", TU=" + ((SipServletResponseImpl)m_bestResponse).getTransactionUser())
    	                        );
        }
        
        boolean needToAggregateAuthHeaders = false;
        
        if(m_bestResponse != null){
        	//In case we get multiple 401 and 407 responses we need to 
	        //Aggregate Authorization Header Field Values RFC 3261 16.7.6
	        if((m_bestResponse.getStatus() == 401 ||  
	            m_bestResponse.getStatus() == 407) &&
	            response.getStatus() == 401 ||  response.getStatus() == 407)
	        {
	            needToAggregateAuthHeaders = true;
	        }
        }
        int newPrefernce = getNewPreference(response.getStatus()); 
        
        if (newPrefernce != -1) {
			// Best response should be saved
			if (needToAggregateAuthHeaders) {
				aggregateAuthHeaders(m_bestResponse, response);
			}
			
			// update old branch session state
			if (m_bestResponse != null) {
				TransactionUserWrapper tuw = ((SipServletResponseImpl)m_bestResponse).getTransactionUser();
				if (tuw != null) {
					if (SipUtil.isErrorResponse(m_bestResponse.getStatus())) {
						tuw.setSessionState(State.TERMINATED, m_bestResponse.getRequest());
						//The TU of the original (incoming) request should not be invalidated here.
						//We might need it for creating further derived sessions on future branches, and 
						//its inbound transaction will get terminated when best response is actually forwarded upstream.
						TransactionUserWrapper origTU = 
							((SipServletRequestImpl)_proxyBranch.getProxy().getOriginalRequest()).getTransactionUser();
						if (c_logger.isTraceDebugEnabled()) 
				    	{
							c_logger.traceDebug(this, 
	    	                        "updateBestResponse", "origRequestTU: " + origTU +
	    	                        ", previous best=" +tuw);
				    	}
						if(!origTU.equals(tuw)){
		    				tuw.transactionCompleted();
							tuw.invalidateIfReady();
						}
					}
				}
			}
			
	
			m_bestResponse = response;
			_proxyBranch = branch;
			m_bestResponsePreference = newPrefernce;
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "updateBestResponse", "Updated To: "
						+ m_bestResponse.getStatus());
			}

		}
    	else {
    		if(needToAggregateAuthHeaders){
    			aggregateAuthHeaders(response, m_bestResponse);
    		}
    		
    		//this branch is not the best response, we need to invalidate it, take the tuw from the branch 
    		TransactionUserWrapper tuw = ((SipServletResponseImpl)response).getTransactionUser();
    		if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "updateBestResponse", "invalidating tu for branch who is not the best response, tu=" + tuw);
			}
    		if (tuw != null) {
    			if (SipUtil.isErrorResponse(response.getStatus())) {
    				tuw.setSessionState(State.TERMINATED, response);
    				tuw.transactionCompleted();
    				tuw.invalidateIfReady();
    			}
    		}
    	}
    }

    /**
     * If the selected response is a 401 (Unauthorized) or 407 (Proxy
     *    Authentication Required), the proxy MUST collect any WWW-
     *    Authenticate and Proxy-Authenticate header field values from
     *    all other 401 (Unauthorized) and 407 (Proxy Authentication
     *    Required) responses received so far in this response context
     *    and add them to this response without modification before
     *   forwarding.  The resulting 401 (Unauthorized) or 407 (Proxy
     *    Authentication Required) response could have several WWW-
     *    Authenticate AND Proxy-Authenticate header field values.
     * @param source
     * @param target
     */
    private void aggregateAuthHeaders(SipServletResponse source, 
                                      SipServletResponse target) 
    {
        Response src = ((SipServletResponseImpl)source).getResponse();
        Response trg = ((SipServletResponseImpl)target).getResponse();
        
        try
        {
	        HeaderIterator iter = src.getHeaders(WWWAuthenticateHeader.name);
	        while(null != iter && iter.hasNext())
	        {
	            trg.addHeader(iter.next(), false);
	        }
	        
	        iter = src.getHeaders(ProxyAuthenticateHeader.name);
	        while(null != iter && iter.hasNext())
	        {
	            trg.addHeader(iter.next(), false);
	        }
        }
        catch(SipParseException e)
        {
            BranchManager.logException(e);
        }
    }

    /**
     * Return the preference value of this response 
     *
     * @param responseValue the value of the response
     * @return The preference value of this status
     */
    private static int getPreference(int responseValue)
    {
		Integer preference = (Integer)c_responseMap.get
										(new Integer(responseValue));
		
		// If we don't recognize this value, will give it the lowest 
		// prefernce in its class 
		if(null == preference)
		{
		    if(c_logger.isTraceDebugEnabled())
		    {
				c_logger.traceDebug(null, "getPreference", 
						"Response value not exist, value is " + responseValue);
		    }
			int i = responseValue / 100;
			preference = new Integer(i * 100 + 99);
		}
		
		if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(null, "getPreference", "Returned: " + preference);
        }
		return preference.intValue();
    }
}
