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
package com.ibm.ws.sip.container.timer;

import javax.servlet.sip.SipApplicationSession;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.internal.SipContainerComponent;
import com.ibm.ws.sip.container.servlets.ReliableResponse;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.servlets.SipServletResponseImpl;
import com.ibm.ws.sip.container.servlets.SipSessionImplementation;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.stack.transaction.SIPTransactionConstants;

/**
 * @author anat, Aug 27, 2005
 *
 * This object is responsible to retransmit the 1xx reliable response
 *
 */
public class ReliabeResponseRetransmitTimer extends BaseTimer {

   /**
    * Class Logger.
    */
    private static final LogMgr c_logger = Log
        .get(ReliabeResponseRetransmitTimer.class);

    /**
     * Current number of retransmit attempt
     */
    private int _count = 1;

    /**
     * Time that the timer has been created.
     */
    private long _startTime = System.currentTimeMillis();

    /**
     * Holds the reference to the ReliableResponseObject;
     */
    private ReliableResponse _responseObj = null;

  
    /**
     * Construct a new timer for the specified response.
     * 
     * @param response
     */
    public ReliabeResponseRetransmitTimer(ReliableResponse responseObj) {
        _responseObj = responseObj;
        setQueueIndex(extractQueueIndex());
    }

    /**
	 * Extracts queue index from the related application session.
	 */
	protected int extractQueueIndex(){
   		int result = -1;
   		
   			if(_responseObj != null){
   				TransactionUserWrapper tuImpl = _responseObj.getServletResponse().getTransactionUser();
   				if(tuImpl != null){
   					String sessId = tuImpl.getApplicationId();
   			    	result = SipApplicationSessionImpl.extractAppSessionCounter(sessId);
   			    	if (c_logger.isTraceDebugEnabled()) {
   						c_logger.traceEntry(this, "getQueueIndex - sessId = " + sessId + " QueueIndex result = " + result);
   					}
   				}
   				else {
   					if (c_logger.isTraceDebugEnabled()) {
   						c_logger.traceEntry(this, "getQueueIndex - can't find the appropriate TU for incoming request ...");
   					}
   				}
   			}
   			else {
   				if (c_logger.isTraceDebugEnabled()) {
   					c_logger.traceEntry(this, "getQueueIndex - can't find incoming request ...");
   				}
   			}
   		return result;
   	}
   	
    /**
     * @see com.ibm.workplace.sip.container.timer.BaseTimer#invoke()
     */
    public void run() {
        long cTime = System.currentTimeMillis(); 
        if (cTime - _startTime < SIPTransactionConstants._64T1) {
            _responseObj.retransmitResponse();

            //Scheduale the next invocation to the minimum delay the between
            // the next retransmission and the overall time for retransmission
            // retries which is 64*T1 
            long nextRetransmit = (long)(SIPTransactionConstants.T1 * Math.pow(2, _count));
            long delay = Math.min(_startTime + SIPTransactionConstants._64T1 - cTime, nextRetransmit);
            
            SipContainerComponent.getTimerService().schedule(this, false, delay);
            _count++;
        }
        else {
            _responseObj.retransmissionTimedOut();
        }

    }

	@Override
	public String getAppName() {
		SipServletResponseImpl resp = _responseObj.getServletResponse();
		if(resp != null){
			SipApplicationSessionImpl sApp = (SipApplicationSessionImpl)resp.getApplicationSession();
			if(sApp != null && sApp.getAppDescriptor() != null) {
				return sApp.getAppDescriptor().getAppName();
			}
		}
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceEntry(this, "realted response is null...");
		}
		return null;
	}

	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getAppIndexForPMI()
	 */
	public Integer getAppIndexForPMI() {
		SipServletResponseImpl resp = _responseObj.getServletResponse();
		if(resp != null){
			SipApplicationSessionImpl sApp = (SipApplicationSessionImpl)resp.getApplicationSession();
			if(sApp != null && sApp.getAppDescriptor() != null) {
				return sApp.getAppDescriptor().getAppIndexForPmi();
			}
		}
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceEntry(this, "realted response is null...");
		}
		return null;
	}

	/** 
	 * @see com.ibm.ws.sip.container.util.Queueable#getApplicationSession()
	 */
	public SipApplicationSession getApplicationSession() {
		SipServletResponseImpl resp = _responseObj.getServletResponse();
		if(resp != null){
			return resp.getApplicationSession();
		}
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceEntry(this, "realted response is null...");
		}
		return null;
	}

	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getTuWrapper()
	 */
	public TransactionUserWrapper getTuWrapper() {
		if(_responseObj != null){
			SipServletResponseImpl resp = _responseObj.getServletResponse();
			SipSessionImplementation sSession = (SipSessionImplementation)resp.getSession();
			if(sSession != null) {
				return sSession.getInternalTuWrapper();
			}
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceEntry(this, "sipSession is null...");
			}
		}
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceEntry(this, "realted response is null...");
		}
		return null;
	}
	
}
