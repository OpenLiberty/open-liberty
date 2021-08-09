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

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.address.URI;
import jain.protocol.ip.sip.header.CSeqHeader;
import jain.protocol.ip.sip.header.CallIdHeader;
import jain.protocol.ip.sip.header.FromHeader;
import jain.protocol.ip.sip.header.Header;
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.RouteHeader;
import jain.protocol.ip.sip.header.ToHeader;
import jain.protocol.ip.sip.message.Request;

import java.io.IOException;
import java.util.List;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.RequestImpl;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.proxy.SipProxyInfo;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;

/**
 * @author Amir Perlman, Apr 2, 2003
 * 
 * A CANCEL request for a Sip Request that originated from a local UAC.
 */
public class OutgoingSipServletCancelRequest extends OutgoingSipServletRequest
{

    /**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log
            .get(OutgoingSipServletCancelRequest.class);

    /**
     * public no-arg constructor to satisfy Externalizable.readExternal()
     */
    public OutgoingSipServletCancelRequest() {
    }

    /**
     * Create a CANCEL Sip Request for the given request.
     * 
     * @param request
     *            An outgoing Sip Servlet Request that was already sent.
     */
    public OutgoingSipServletCancelRequest(OutgoingSipServletRequest request)
    {
        super();
        generateJainCancelRequest(request);
        
        TransactionUserWrapper tUser = request.getTransactionUser(); 
        setSipProvider(request.getSipProvider());
        setIsInital(false);
        setIsCommited(false);
        setIsSubsequentRequest(true);
        
        setTransactionUser(tUser);
        
    }

    /**
     * Generate the Jain Request wrapped by this Servlet Request.
     * 
     * @param request
     */
    private void generateJainCancelRequest(
            OutgoingSipServletRequest servletRequest)
    {
        RequestImpl inviteReq = (RequestImpl) servletRequest.getRequest();

        URI reqUri;
        try
        {
            reqUri = inviteReq.getRequestURI();
            CallIdHeader callHeader = inviteReq.getCallIdHeader();
            CSeqHeader cseq = (CSeqHeader)inviteReq.getCSeqHeader().clone();
            cseq.setMethod(Request.CANCEL);

            FromHeader from = inviteReq.getFromHeader();
            TransactionUserWrapper tu = servletRequest.getTransactionUser();
            
//            if (tu != null && tu.getRemoteTag() != null){
//            	// TU will be null when we are in the proxy condition.
//            	// If it is Proxy - Tag will be set later by BranchManager.
//            	from.setTag(tu.getRemoteTag());
//            }
            
            ToHeader to = inviteReq.getToHeader();
            List viaList = SIPStackUtil.headerIteratorToList(inviteReq
                    .getViaHeaders());

            Request cancelReq = getMessageFactory().createRequest(reqUri,
                    Request.CANCEL, callHeader, cseq, from, to, viaList);
            
            if (inviteReq.hasRouteHeaders()) {
				HeaderIterator iter = inviteReq.getRouteHeaders();
				if (iter != null) {
					while (iter.hasNext()) {
						cancelReq.addHeader((RouteHeader) iter.next(), false);
					}
				}
			}
            
            // copy the IBM-Destination/IBM-PO header from the INVITE to the CANCEL.
            // this ensures the CANCEL goes out to the same hop as the INVITE did.
            Header destination = servletRequest.getDestinationURI();
            if (destination != null) {
            	cancelReq.setHeader(destination, true);
            }
            Header poHeader = servletRequest.getPreferredOutbound();
            if (poHeader != null) {
            	cancelReq.setHeader(poHeader, true);
            }
            
            boolean isLoopBack = inviteReq.isLoopback();
            ((MessageImpl)cancelReq).setLoopback(isLoopBack);

            setMessage(cancelReq);
        }
        catch (SipParseException e)
        {
            if(c_logger.isErrorEnabled())
            {
                c_logger.error("error.exception", Situation.SITUATION_CREATE, 
                        		null, e);
            }
        }

    }

    /**
     * @see com.ibm.ws.sip.container.servlets.OutgoingSipServletRequest#setupParametersBeforeSent()
     */
    public void setupParametersBeforeSent(SipURL target, boolean isLoopBack) throws IOException {
		// In the CANCEL request no new parameters should be added before
		// the request is actually sent. Parameters like Via should remain the
    	// same as in initial INVITE request.
    	
        //	Here we tack on the PO header to tell the proxy which interface to send on.
        //	We have to do this here to ensure the application has had time to set the interface on the session.
		//	Note that tu is null when an application is proxying messages.
    	TransactionUserWrapper tu = getTransactionUser();
    	
    	//NMO
    	boolean outboundEnable = PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.ENABLE_SET_OUTBOUND_INTERFACE);
    	String	ibmPOHeader = getHeader(SipProxyInfo.PEREFERED_OUTBOUND_HDR_NAME);
        if (c_logger.isTraceDebugEnabled()){
        	c_logger.traceDebug(this, "setupParametersBeforeSent", "current IBM-PO =  " + ibmPOHeader);
        }
    	if(ibmPOHeader == null || !outboundEnable ){
    		
    		if (tu != null ){
    			SipProxyInfo.getInstance().addPreferedOutboundHeader(this, tu.getPreferedOutboundIface(getTransport()));
    		}
    	}
    }
    /** 
     * @see com.ibm.ws.sip.container.servlets.OutgoingSipServletRequest#updateParamAccordingToDestination()
     */
    public void updateParamAccordingToDestination() throws IOException {
		// Should not to be here. ACK request sent over the same transport
		// as Original INVITE.
	}
    
    @Override
    protected boolean shouldCreateContactIfNotExist() {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "shouldCreateContactIfNotExist", "Contact header won't be added to the outgoing CANCEL message");
		}
		return false;
    }

}