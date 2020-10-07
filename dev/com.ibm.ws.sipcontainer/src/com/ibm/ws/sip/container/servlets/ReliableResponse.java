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
package com.ibm.ws.sip.container.servlets;

import jain.protocol.ip.sip.SipException;
import jain.protocol.ip.sip.header.CSeqHeader;
import jain.protocol.ip.sip.header.HeaderParseException;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.SipProviderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.RSeqHeader;
import com.ibm.ws.sip.container.events.EventsDispatcher;
import com.ibm.ws.sip.container.internal.SipContainerComponent;
import com.ibm.ws.sip.container.timer.ReliabeResponseRetransmitTimer;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.stack.transaction.SIPTransactionConstants;

/**
 * @author anat, Aug 29, 2005
 *
 * Contains the Reliable Response that was sent to the UAC 
 * and it's timer that is responsible to retransimt the response
 * it it wasn't PRACKed
 */
public class ReliableResponse {
    
    public final static String RELIABLY_PARAM = "100rel";
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(ReliableResponse.class);
    
    /**
     * Represents latest timer that is running on the _waitingForPrackResponse
     */
	private ReliabeResponseRetransmitTimer _timer;
    
	/**
	 * Reference to the Response that was sent reliable 
	 */
	private SipServletResponseImpl _response;
	
	/**
	 * Rseg value from the Response
	 */
	private long _rSeq = 0;

    /**
     * Rseg value from the Response
     */
    private long _cSeq = 0;
    

    /**
     * Reference to reliable responses contianer that contians this ReliableResponseOblj
     */
	private ReliableResponsesProcessor _responseProcessor;
    
    /**
     * Ctor that received the reliable request and creates a timer for 
     * it's retransmission
     * @param response
     */
    public ReliableResponse(SipServletResponseImpl response,
                               ReliableResponsesProcessor container){
        _response = response;
        _responseProcessor = container;
        try {
            RSeqHeader h = (RSeqHeader)response.getResponse().getHeader(RSeqHeader.name,true);
            if(h!= null){
                _rSeq = h.getResponseNumber();
            }
            CSeqHeader ch = (CSeqHeader)response.getResponse().getHeader(CSeqHeader.name,true);
            if(h!= null){
                _cSeq = ch.getSequenceNumber();
            }
        } catch (HeaderParseException e) {
            // We should not get here - because we had created this RSeq in the SendReliable() method
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "ReliableResponse", "Somthing was wrong in the RSeqHeader");
            }
        } catch (IllegalArgumentException e) {
            // We should not get here - because we had created this RSeq in the SendReliable() method
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "ReliableResponse", "Somthing was wrong2 in the RSeqHeader");
            }
        }
        
        if(hasOffer()){
           _responseProcessor.addOffer();
        }
        _timer = new ReliabeResponseRetransmitTimer(this);
        SipContainerComponent.getTimerService().schedule(_timer, false, SIPTransactionConstants.T1);
    }
    
    /**
     * @return Returns the response that was sent reliably.
     */
    public SipServletResponseImpl getServletResponse() {
        return _response;
    }

    /**
     * The retransmission of this response was timedOut - notify the APP about it
     */
    public void retransmissionTimedOut() {

        //Timer has reached 64*T1 without being cancelled which means that
        //a ACK has not been received.
    	TransactionUserWrapper tUser = _response.getTransactionUser();
    	EventsDispatcher.noPrackReceived(_response, 
    			tUser.getSipServletDesc().getSipApp());
    }

    /**
     * Sends of Retransmits the responses
     */
    public void retransmitResponse() {
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
     * @return Returns the rSeq of the original Response.
     */
    public long getRSeq() {
        return _rSeq;
    }

    /**
     * @return Returns the CSeq of the original Response.
     */
    public long getCSeq() {
        return _cSeq;
    }

    /**
     * Notification that this Reliable Response was Acknowledged with PRACK
     */
    public void acknowledged() {
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "acknowledged", "This Response was acknowledged");
        }
        removeThisRequest();
    }

    /**
     * This Response was cancel because PRACK with higher RAck wa received
     */
    public void cancel() {
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "cancell", " This Response was canceled");
        }
        removeThisRequest();
    }
    
    /**
     * This method will remove cancel the timer and decrease offers counter
     * in the _responsesCOntainer if this response has a body
     */
    private void removeThisRequest(){
        if(hasOffer()){
            _responseProcessor.removeOffer();
        }
        _timer.cancel();
    }
    
    /**
     * @return if the original response conatins body or not
     */
    public boolean hasOffer(){
        return _response.getResponse().hasBody();
    }
}
