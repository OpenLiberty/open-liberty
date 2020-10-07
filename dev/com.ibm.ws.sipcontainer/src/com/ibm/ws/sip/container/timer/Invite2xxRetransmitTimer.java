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

import jain.protocol.ip.sip.SipException;

import javax.servlet.sip.SipApplicationSession;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.SipProviderImpl;
import com.ibm.ws.sip.container.events.EventsDispatcher;
import com.ibm.ws.sip.container.internal.SipContainerComponent;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipServletResponseImpl;
import com.ibm.ws.sip.container.servlets.SipSessionImplementation;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.stack.transaction.SIPTransactionConstants;

/**
 * @author Amir Perlman, Mar 6, 2005
 * 
 * Timer for retransmiting 2xx final response to INVITE requests till ACK is
 * received. RFC 3261 Section 13.3.1.4
 */
public class Invite2xxRetransmitTimer extends BaseTimer {

    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(Invite2xxRetransmitTimer.class);
    /**
     * Response to be retransmited.
     */
    private SipServletResponseImpl _response;

    /**
     * Current number of retransmit attempt
     */
    private int _count = 1;

    /**
     * Time that the timer has been created.
     */
    private long _startTime = System.currentTimeMillis();

    /**
     * CSeq of the retransmit response
     */
    private long _cseq;

    /**
     * Construct a new timer for the specified response.
     * 
     * @param response
     */
    public Invite2xxRetransmitTimer(SipServletResponseImpl response) {
        _response = response;
        _cseq = ((SipServletRequestImpl)response.getRequest()).getRequest().getCSeqHeader().getSequenceNumber();
        setQueueIndex(extractQueueIndex());
    }

    /**
	 * Extracts queue index from the related application session.
	 */
	protected int extractQueueIndex() {
		int result = -1;
			if(_response != null){
				TransactionUserWrapper tuImpl = _response.getTransactionUser();
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
     * @see com.ibm.ws.sip.container.timer.BaseTimer#invoke()
     */
    public void run() {
        //Access session object directly and check if ack has already been received.
        TransactionUserWrapper transactionUser = _response.getTransactionUser();
        transactionUser.handle2xxRetransmittion(this);
    }
    
    /**
     * Method that is actually reschedule the next timer if needed
     * @param transactionUser
     */
    public void rescheduleNextTimer(TransactionUserWrapper transactionUser){
    	long cTime = System.currentTimeMillis(); 
        if (cTime - _startTime < SIPTransactionConstants._64T1) {
            retransmitResponse();

            //Schedule the next invocation to the minimum delay the between
            // the next retransmission and the overall time for retransmission
            // retries which is 64*T1 
            long nextRetransmit = (long) Math.min(SIPTransactionConstants.T1 * Math.pow(2, _count),
                                                  SIPTransactionConstants.T2);
            long delay = Math.min(_startTime + SIPTransactionConstants._64T1 - cTime, nextRetransmit);
            
            synchronized (this) {
            	if(!isCancelled()){
            		SipContainerComponent.getTimerService().schedule(this, false, delay);
                    _count++;
            	}
            	else{
//            		This timer was already canceled. Will not reschedule.
            	}
			}
        }
        else {
            //Timer has reached 64*T1 without being cancelled which means that
            //a ACK has not been received.
        	EventsDispatcher.noAckReceived(_response, 
        			transactionUser.getSipServletDesc().getSipApp());
        }    	
    }

    /**
     * Method that cancel the timer.
     */
    public synchronized void cancel(){
    	super.cancel(); 
    }
    
    
    /**
     * Retransmit by sending directly to the transport layer bypassing
     * the transaction layer which no longer has transaction previously
     * associated with this message. 
     */
    private void retransmitResponse() {
        try {
            ((SipProviderImpl)_response.getSipProvider()).sendResponse(
                									_response.getResponse());
        }
        catch (SipException e) {
            if(c_logger.isErrorEnabled())
            {
                c_logger.error("error.exception", "Send Failure", null, e);
            }
        }
    }

	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getAppName()
	 */
	public String getAppName() {
		if(_response != null){
			SipApplicationSessionImpl sApp = (SipApplicationSessionImpl)_response.getApplicationSession();
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
		if(_response != null){
			SipApplicationSessionImpl sApp = (SipApplicationSessionImpl)_response.getApplicationSession();
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
		if(_response != null){
			return _response.getApplicationSession();
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
		if(_response != null){
			SipSessionImplementation sSession = (SipSessionImplementation)_response.getSession();
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

	/**
	 * Returns a retransmitted cseq
	 * @return cseq
	 */
	public long getRetransmittedResponseCSeq() {
		return _cseq;
	}
}