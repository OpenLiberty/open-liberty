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

import jain.protocol.ip.sip.header.HeaderParseException;

import java.util.Iterator;
import java.util.LinkedList;

import javax.servlet.sip.SipServletResponse;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.extensions.RAckHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.RSeqHeader;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

/**
 * @author anat, Aug 27, 2005
 *
 * Object that contains all the ReliableResponses relates to the specific
 * SipSession 
 */
public class ReliableResponsesProcessor {
    
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(ReliableResponsesProcessor.class);
    
    /**
     * SipSession that this container of the reliable responses relates to
     */
    private TransactionUserWrapper _transactioUser;
    
    /** 
     * list of provisional responses waiting for first response to be PRACKed
     * First element will be the first Reliable Request that was sent
     * to the UAC and wasn't answered yet */
    private LinkedList _waitingResponses;
		
	/**
	 * Defines if the first Reliable Response was PRACKED
	 */
	private boolean _firstWasAcknowledged = false;
	
	/**
	 * Counter that will count the number of unacknowledged reliable responses 
	 * which contains an offer inside.
	 */
	private int _offersCounter = 0;
	
	/**
     * Next Rseq number for the next reliable responses.
     */
    private long m_localRSeq = 1;
    
    /**
     * Latest Rseq that was received from the remote side
     */
    private long m_lastRemoteRseg = -1;
    
    /**
     * Latest Rseq that was rAswered with PRACK response
     */
    private long m_lastRemoteAnsweredRseg = -1;
	
    
    /**
     * Ctor
     * @param session
     */
    public ReliableResponsesProcessor(TransactionUserWrapper tUser) {
        _transactioUser = tUser;
    }
    
    
    /**
     * @return Returns the offersCounter.
     */
    public int getOffersCounter() {
        return _offersCounter;
    }
    
    /**
     * @param offersCounter The offersCounter to set.
     */
    public void addOffer() {
        _offersCounter ++;
    }
    
    /**
     * @param offersCounter The offersCounter to set.
     */
    public void removeOffer() {
        _offersCounter --;
    }
    
    
    /**
     * Method that decides if the response should be sent or the exception should 
     * thrown as a reason of the first Reliable Response that wasn't acknowledged
     * @param response
     * @return
     */
    public void sendResponse(SipServletResponseImpl response){

        ReliableResponse responseObj = null;
        synchronized (this) {
            // This is a first reliable response that sent from UAS to UAC
            if (_waitingResponses == null) {
                _waitingResponses = new LinkedList();
                responseObj = new ReliableResponse(response,this);
                _waitingResponses.addFirst(responseObj);
            }
            else {
                if(_firstWasAcknowledged == true){
                    // The APP can send several 1xx reliable responses after the 
                    // first was acknowledged
                    responseObj = new ReliableResponse(response,this);
                    _waitingResponses.addLast(responseObj);
                }
                else{
                    // The UAS MUST NOT send a second reliable provisional
                    // response until the first is acknowledged.  After the first, it is
                    // RECOMMENDED that the UAS not send an additional reliable provisional
                    // response until the previous is acknowledged.
                    throw new IllegalStateException(
	                      "The next Reliable Response can be sent only after the first one will be answered: "
	                      + this);
                }
            }
        }
                
        if (c_logger.isTraceDebugEnabled()) {
            StringBuffer buff = new StringBuffer();
            buff.append("Provisional response with CallId = <");
            buff.append(response.getCallId());
            buff.append("> and RSeq = <");
            String rseq = response.getHeader(RSeqHeader.name);
            buff.append(rseq);
            buff.append("> will be sent now ");
            c_logger.traceDebug(this, "sendResponse", buff.toString());
        }
     
    }
    
    
    /**
     * Helper method that finds and returns the RAckHeader 
     * @param request
     * @return
     */
    private RAckHeader getRackHeader(SipServletRequestImpl request){
        RAckHeader rack = null;
        try {
            rack = (RAckHeader) request.getRequest().getHeader(RAckHeader.name, true);
        } catch (HeaderParseException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return rack;
    }
    
    
    /**
     * Try to match the received PRACK to the first reliable response that was
     * sent.
     * @param request
     * @return
     */
    private int matchFirstPrack(SipServletRequestImpl request, RAckHeader rack){
        int rc = SipServletResponse.SC_CALL_LEG_DONE;
//      The first Reliable Response will be always a first object in the list
        ReliableResponse response = (ReliableResponse) _waitingResponses.getFirst();
        if (response != null) {
            if(!response.getServletResponse().getMethod().equals(rack.getMethod())||
                    response.getCSeq()!= rack.getSequenceNumber() ){
                rc = SipServletResponse.SC_BAD_REQUEST;
                if (c_logger.isTraceDebugEnabled()) {
                    c_logger.traceDebug(this, "isLegalPrack",
                                    "The SCeq parameters inside RAck header are wrong");
                }
            }
            else if (rack.getResponseNumber() == response.getRSeq()) {
                _firstWasAcknowledged = true;
                response.acknowledged();
                _waitingResponses.remove(response);
                rc = SipServletResponse.SC_OK;
                if (c_logger.isTraceDebugEnabled()) {
                    c_logger.traceDebug(this, "isLegalPrack",
                                    "The PRACK on the first reliable response was received");
                }
            }
        }
        return rc;
    }
    
    
    /**
     * Try to match the received PRACK to the first reliable response that was
     * sent.
     * @param request
     * @return
     */
    private int matchPrack(SipServletRequestImpl request, RAckHeader rack){
        int rc = SipServletResponse.SC_CALL_LEG_DONE;
        
        // Goes through the all waiting for the PRACK reliable responses .
        // If it will find the response that it's RSeq is higher that PRACK's
        // this response will be cancelled and removed form the list.
        // rfc 3262. If it will lower - this PRACK should be dropped.
        ReliableResponse rObj = null;
        // the method of the PRACK request will be tested only once. If it will be
        // wrong - then PRACK will be rejected with the 400 BAD Request response
        boolean testMethod = true;
        
        for (Iterator iter = _waitingResponses.iterator(); iter.hasNext();) {
            rObj = (ReliableResponse) iter.next();
            if(testMethod){
                if(!rObj.getServletResponse().getMethod().equals(rack.getMethod())||
                        rObj.getCSeq()!= rack.getSequenceNumber() ){
                    rc = SipServletResponse.SC_BAD_REQUEST;
                    if (c_logger.isTraceDebugEnabled()) {
                        c_logger.traceDebug(this, "isLegalPrack",
                                        "The SCeq parameters inside RAck header are wrong");
                    }
                    
                    break;
                }
                testMethod = false;
            }
            
            if (rack.getResponseNumber() == rObj.getRSeq()) {
                rObj.acknowledged();
                iter.remove();
                rc = SipServletResponse.SC_OK;
                break;
            }
            else if (rack.getResponseNumber() > rObj.getRSeq()) {
                rObj.cancel();
                iter.remove();
            }
            else {
                rc = SipServletResponse.SC_CALL_LEG_DONE;
                break;
            }
        }

        return rc;
    }
    
    
    /**
     * Method that will return as an answer 
     * if the PRACK that received is a legal one and if it should be processed
     * @param request
     * @return
     */
    public int isLegalPrack(SipServletRequestImpl request) {
        int rc = SipServletResponse.SC_OK;
        synchronized (this) {
            // Synchronized came to prevent _waitingResponses to be created twice
            // In addition in the matchFirstPrack() and matchPrack() methods
            // Objects can be removed from the _waitingResponses list but on the other
            // side the objects can be added in the sendResponse() method
            if (_waitingResponses != null && _waitingResponses.size() > 0) {
                RAckHeader rack = getRackHeader(request);
                // Get the RAck header from the PRACK request
                if (rack == null) {
                    if (c_logger.isTraceDebugEnabled()) {
                        c_logger.traceDebug(this, "isLegalPrackReceived",
                                            "Filed to find RSeq in the PRACK");
                    }
                    rc = SipServletResponse.SC_BAD_REQUEST;
                    
                }
                // Try to match the received PRACK to one on the waiting responses
                else {
                    
                    if (_firstWasAcknowledged == false) {
                        //If the first reliable response was not answerers with 
                        // the appropriate PRACK - we should try to match the received
                        // PRACK to the first response
                        rc = matchFirstPrack(request, rack);
                    }
                    else {
                        // Try to match the RPACK to one of the waiting response
                        rc = matchPrack(request, rack);
                    }
                    
                }
            }
        }
        
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "isLegalPrack", "isLegal return error " + rc);
        }
        
        return rc;
    }
    
    
    /**
     * Helper method that cancel all the timers and remove itself from
     * the related SipSession
     */
    private void stopSendReliableResponses(){
    	if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceEntry(this,"stopSendReliableResponses", _transactioUser.getId());
        }
        if (_waitingResponses != null && _waitingResponses.size() > 0) {
            
            synchronized (this) {
                for (Iterator iter = _waitingResponses.iterator(); iter.hasNext();) {
                    ReliableResponse rObj = (ReliableResponse) iter.next();
                    rObj.cancel();
                }
            }
            _waitingResponses = null;
        }
        _transactioUser.cleanReliableObject();
        _transactioUser = null;
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceExit(this,"stopSendReliableResponses");
        }
    }
    
    /**
     * Checkes if the PRACK request contains all defined headers
     * @param request
     */
    public void checkPrack(SipServletRequestImpl request){

//      Because the APP is responsible to add the RAck header
//      the SipSession should check if it is done
    	if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceEntry(this,"checkPrack", request);
        }
        RAckHeader rack;
        try {
            rack = (RAckHeader) request.getRequest().getHeader(RAckHeader.name, true);
        
	        if (rack == null) {
	            throw new IllegalStateException(
	                "The PRACK request SHOULD contain RAck header " + this);
	        }
	        updateLastAnsweredRemoteCseq(rack.getResponseNumber());
        } catch (HeaderParseException e) {
            throw new IllegalStateException(e.getMessage() + ' ' + this);
        }
        finally{
        	if (c_logger.isTraceDebugEnabled())
            {
                c_logger.traceExit(this,"checkPrack", "m_lastRemoteAnsweredRseg="+m_lastRemoteAnsweredRseg);
            }
        }
    }

    /**
     * Method that will return as an answer 
     * if the Final response should be processed or will be send later
     * after the PRACK will be received on the previous reliable response 
     * that was sent
     * 
     * @param request final response
     * @return
     */
    public boolean sendFinalResponse(SipServletResponseImpl response){
        
        if(response.getStatus() >= 200 && response.getStatus() < 300){
            if (_offersCounter > 0){
                throw new IllegalStateException (
                "Unable to send 2xx final response prior to receiving acknowledgement outstanding reliable provisional responses that has an offer per RFC 3262 section 3" +
                this);
            }
        }
        
        else {
            //Other final response
            stopSendReliableResponses();
        }
        return true;        
    }


    /**
     * @return next RSeq that should be used to send the next Reliable
     */
    public synchronized long getNextRseg() {
        return m_localRSeq++;
    }

    /**
     * Responsible to identify if the response is a new and correct response 
     * or it is a retransmission response
     * @param response
     * @return
     */
    public boolean processReliableResponse(SipServletResponseImpl response) {
    	if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceEntry(this,"processReliableResponse", response);
        }
        boolean isOk = false;
        
         try {
            RSeqHeader rseq = (RSeqHeader) response.getResponse().getHeader(RSeqHeader.name, true);
            if(rseq != null){
	            long newRseq = rseq.getResponseNumber();
	            if (newRseq > m_lastRemoteRseg && newRseq != m_lastRemoteAnsweredRseg) {
	                updateLastRemoteCseq(newRseq);
	                isOk = true;
	            }
            }
        } 
         catch (HeaderParseException e) {
           if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "processReliableResponse", 
							"Exception " + e.getMessage());
           }
        } 
         catch (IllegalArgumentException e) {
             if (c_logger.isTraceDebugEnabled()) {
     			c_logger.traceDebug(this, "processReliableResponse", 
     							"Exception " + e.getMessage());
                }
        }
         finally{
        	if (c_logger.isTraceDebugEnabled())
            {
                c_logger.traceExit(this,"processReliableResponse", new Boolean(isOk));
            }
        }
        return isOk;
        
    }

    
    /**
     * Sets the m_lastRemoteRseg with the new value
     * @param newCseq
     */
    private void updateLastRemoteCseq (long newCseq){
        m_lastRemoteRseg = newCseq;
    }
    
    /**
     * Sets the m_lastRemoteAnsweredRseg with the new value
     * @param newCseq
     */
    private void updateLastAnsweredRemoteCseq (long newCseq){
        m_lastRemoteAnsweredRseg = newCseq;
    }
}
