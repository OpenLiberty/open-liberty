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
import jain.protocol.ip.sip.address.SipURL;
import javax.servlet.sip.*;

import com.ibm.ws.sip.container.servlets.*;
import com.ibm.ws.sip.container.transaction.ClientTransactionListener;
import com.ibm.ws.sip.container.util.SipUtil;
import com.ibm.sip.util.log.*;

/**
 * @author yaronr
 * 
 * Used by the Stateful proxy to map responses to subsequent requests
 *  
 */
public class SubsequentRequestListener implements ClientTransactionListener {
    /**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log
            .get(SubsequentRequestListener.class);

    /**
     * The original request
     */
    private IncomingSipServletRequest _originalRequest;
    
    /**
     * Holds the information about the destination where the INVITE (if it is INVITE)
     * sent and provisional response was received on it. Will be used for next
     * CANCEL request if it will coming. In this case it is used for reInvite.
     */
    private SipURL _latestDestination;
    

    /**
     * Constructor
     * 
     * @param request
     *            the original request
     */
    protected SubsequentRequestListener(SipServletRequest request) {
        _originalRequest = (IncomingSipServletRequest) request;
    }

    /**
     * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#processResponse(javax.servlet.sip.SipServletResponse)
     */
    public void processResponse(SipServletResponseImpl response) {
        RecordRouteProxy.proxyResponse(_originalRequest, response);

    }

    /**
     * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#processTimeout(javax.servlet.sip.SipServletRequest)
     */
    public void processTimeout(SipServletRequestImpl request) {
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "processTimeout",
                                "Subsequent request timedout: "
                                        + _originalRequest);
        }
        IncomingSipServletResponse response =  null;
        try {
            //get the timeout response
        	response = SipUtil.createResponse(SipServletResponse.SC_REQUEST_TIMEOUT, request);
        }
        catch (IllegalArgumentException e) {
            if (c_logger.isErrorEnabled()) {
                c_logger.error("error.exception",
                               Situation.SITUATION_CREATE, null, e);
            }
        }
        catch (SipParseException e) {
            if (c_logger.isErrorEnabled()) {
                c_logger.error("error.exception",
                               Situation.SITUATION_CREATE, null, e);
            }
        }
        RecordRouteProxy.proxyResponse( _originalRequest, response);
    }
    
    

    /**
     * That method is exist because subsequent request listener is 
     * client transaction listener also but the method should not be
     * invoked cause application router is not invoked by subsequent 
     * requests.
     * 
     */
	public void processCompositionError(SipServletRequestImpl request) {
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "processCompositionError",
                                "Subsequent request should not be there: "
                                        + _originalRequest);
        }
	}

	/**
     * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#onSendingRequest(javax.servlet.sip.SipServletRequest)
     */
    public boolean onSendingRequest(SipServletRequestImpl request) {
        return request.getTransactionUser().onSendingRequest(request);
    }
    
    /**
     * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#clientTransactionTerminated(javax.servlet.sip.SipServletRequest)
     */
    public void clientTransactionTerminated(SipServletRequestImpl request) {
    	request.getTransactionUser().clientTransactionTerminated(request);
	}

	/**
	 *  @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#setUsedDestination(jain.protocol.ip.sip.address.SipURL)
	 */
    public void setUsedDestination(SipURL lastUsedDestination) {
    		_latestDestination = lastUsedDestination;
	}

	/** 
	 * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#getUsedDestination()
	 */
    public SipURL getUsedDestination() {
		return _latestDestination;
	}

	@Override
	/**
	 * @see com.ibm.ws.sip.container.transaction.TransactionListener#removeTransaction(com.ibm.ws.sip.container.servlets.SipServletResponseImpl)
	 */
	public void removeTransaction(String method) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "removeTransaction",
					"removeTransaction from TU = " + _originalRequest.getTransactionUser().getId());
		}
		
		// In Subsequent , request and response are already related to the correct TU.
		_originalRequest.getTransactionUser().removeTransaction(method);
		
	}

	@Override
	/**
	 * @see com.ibm.ws.sip.container.transaction.TransactionListener#addTransaction()
	 */
	public void addTransaction(String method) {

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "addTransaction",
					"add transaction to TU = " + _originalRequest.getTransactionUser().getId());
		}
		_originalRequest.getTransactionUser().addTransaction(method);
	}
	
}