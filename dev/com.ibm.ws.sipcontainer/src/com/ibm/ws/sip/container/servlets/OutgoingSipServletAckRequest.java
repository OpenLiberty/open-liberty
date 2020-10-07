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
import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.address.NameAddress;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.address.URI;
import jain.protocol.ip.sip.header.CSeqHeader;
import jain.protocol.ip.sip.header.CallIdHeader;
import jain.protocol.ip.sip.header.ContactHeader;
import jain.protocol.ip.sip.header.FromHeader;
import jain.protocol.ip.sip.header.HeaderFactory;
import jain.protocol.ip.sip.header.MaxForwardsHeader;
import jain.protocol.ip.sip.header.RouteHeader;
import jain.protocol.ip.sip.header.ToHeader;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.jain.protocol.ip.sip.ListeningPointImpl;
import com.ibm.ws.jain.protocol.ip.sip.SipJainFactories;
import com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.RequestImpl;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.proxy.SipProxyInfo;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;
import com.ibm.ws.sip.stack.util.SipStackUtil;

/**
 * @author Amir Perlman, Apr 6, 2003
 *
 * A Ack Request to an Incoming Response.
 * This Request is used by servlets acting as UACs in order to 
 * acknowledge 2xx final responses to INVITE requests. 
 */
public class OutgoingSipServletAckRequest extends OutgoingSipServletRequest
{
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger =
        Log.get(OutgoingSipServletAckRequest.class);
    
    /**
     * configuration flag specifying whether or not to clone the
     * To and From headers when creating ACK for 2xx response.
     * A value of true 
     */
    private static final boolean cloneToFromInAck = PropertiesStore.getInstance().getProperties()
		.getBoolean(CoreProperties.CLONE_TO_FROM_IN_ACK);

    /**
     * flag indicating whether this ACK request was sent out, at least once.
     * it initializes to false, and set to true upon the first transmission.
     * this is used when handling retransmissions of 2xx, to check if it's
     * safe to re-send the ACK. the code handling the 2xx response might
     * trigger from a different thread than the application code sending it
     * out the first time, and the 2 threads might attempt to send the request
     * at the same time, typically trying to add a branch parameter twice.
     * to avoid that, the 2xx response handling does not retransmit the ACK
     * unless it was fully sent by the application thread.
     * @see com.ibm.ws.sip.container.tu.TransactionUserImpl#processUACInvite2xxRetransmission
     */
    private boolean m_sent = false;
    
    /**
     * public no-arg constructor to satisfy Externalizable.readExternal()
     */
    public OutgoingSipServletAckRequest() {
    }

    /**
     * Create a ACK Sip Request for the given response.  
     * @param response An incoming Sip Servlet Response that this request 
     * is acknowledging	 
     */
    public OutgoingSipServletAckRequest(IncomingSipServletResponse response)
    {
        super();
        
        setSipProvider(response.getSipProvider());
        setIsInital(false);
        setIsCommited(false);

        setTransactionUser(response.getTransactionUser());

        RequestImpl req = (RequestImpl)((SipServletRequestImpl) 
                        response.getRequest()).getRequest();
        Response res = response.getResponse();
        Vector routeHeaders = response.getTransactionUser().getRouteHeaders();
        
        //Use Amirk's methods bellow for generating a ACK request for the
        //same dialog. Can not use the stack create ACK method since
        //that will only work for non 2xx response which do not establish 
        //a dialog. 
        try
        {
        	SipProvider provider = getSipProvider();
            ListeningPointImpl lp = (ListeningPointImpl)provider.getListeningPoint();
            Request ack = createClientDialogReq(req,
                    							res,
                    							routeHeaders,
                    							Request.ACK,
                								lp);

            //Bind the newly created request to this Sip Servlet Request
            setMessage(ack);
        }
        catch (SipParseException e)
        {
            if(c_logger.isErrorEnabled())
            {
                c_logger.error("error.exception", Situation.SITUATION_CREATE, 
                        		null, e);
            }
        }
        
        // the ACK is sent on the dialog if the response code is 2xx
        boolean subsequent = response.getStatus() / 100 == 2;
        setIsSubsequentRequest(subsequent);
    }

    
    /** 
     * @see com.ibm.ws.sip.container.servlets.OutgoingSipServletRequest#updateParamAccordingToDestination()
     */
    public void updateParamAccordingToDestination() throws IOException {
		// Should not to be here. ACK request sent over the same transport
		// as Original INVITE.
	}

    
    /**
     * @see com.ibm.ws.sip.container.servlets.OutgoingSipServletRequest#setupParametersBeforeSent()
     */
    public void setupParametersBeforeSent(SipURL target, boolean isLoopBack) throws IOException {
        try {
			// before sending the ACK, check if it is part of application
			// composition. in some cases, the original INVITE is sent out on 
        	// loopback, but not the ACK.
            // for example, consider a non-record-routing proxy app fronted by a B2B UA:
            // UAC->B2B->Proxy->UAS
            // in such case, the INVITE is sent from the B2B on loopback to the Proxy,
            // while the ACK is sent from the B2B directly to the UAS.
            // in other words, we need to check each ACK on its own for app composition.
        	if (checkIsLoopback()) {
				((MessageImpl)getRequest()).setLoopback(true);
			}
        	
            //	Here we tack on the PO header to tell the proxy which interface to send on.
            //	We have to do this here to ensure the application has had time to set the interface on the session.
    		//	Note that tu is null when an application is proxying messages.
        	TransactionUserWrapper tu = getTransactionUser();
        	if (tu != null)
    			SipProxyInfo.getInstance().addPreferedOutboundHeader(this, tu.getPreferedOutboundIface(getTransport()));
		} 
        catch (SipParseException e) {
        	if (c_logger.isErrorEnabled())
            {
                Object[] args = { this };
                c_logger.error(
                    "error.send.request",
                    Situation.SITUATION_REQUEST,
                    args,
                    e);
            }
            logExceptionToSessionLog(SipSessionSeqLog.ERROR_SEND_REQ, e);
            throw (new IOException(e.getMessage()));
		}
	}

    /**
     * @see OutgoingSipServletRequest#send()
     */
    public void send() throws IOException {
        super.send();
        m_sent = true;
    }
    
    /**
     * called by the code handling a 2xx retransmission, to determine whether
     * this ACK was already sent out, at least once.
     * it's only safe to re-send if it was already sent out once.
     * 
     * @return true if already sent, false otherwise
     */
    public boolean wasSent() {
    	return m_sent;
    }
    
    /**
     * create a client dialog request
     * 
     * follow instractions of 12.1.2 , 12.2.1.1
     * @author Function provided by Amirk
     * @param req
     * @param res
     * @return Request
     * @throws SipParseException
     */
    public Request createClientDialogReq(
        Request req,
        Response res,
        Vector routeHeaders,
        String method,
        ListeningPointImpl lp)
        throws SipParseException
    {

        // variable for dialog creation

        String callId;
        ToHeader remoteParty;
        FromHeader localParty;
        long sequenceNumber;
        LinkedList routeSet = null;
        NameAddress remoteTarget = null;

        // Generate Route Set       
        if (routeHeaders != null) {
        	routeSet = new LinkedList();
        	for(Iterator itr = routeHeaders.iterator(); itr.hasNext();) {
        		String value = (String)itr.next();
        		RouteHeader routeHeader = (RouteHeader)
					SipJainFactories.getInstance()
						.getHeaderFactory()
						.createHeader( RouteHeader.name, value);

				// Add first so we reverse the order
                routeSet.addLast(routeHeader);
            }
    	}

        ContactHeader contact = (ContactHeader) res.getHeader(ContactHeader.name, true); 
        if(contact == null)
        {
            String msg = "Error, Contact not available in response. Can not send ACK";
            
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "createClientDialogReq", msg);
            }
            throw new SipParseException(msg);
        }
        remoteTarget = contact.getNameAddress();
        
        
        //set rest of parameters
        sequenceNumber = req.getCSeqHeader().getSequenceNumber();
        callId = req.getCallIdHeader().getCallId();
        localParty = req.getFromHeader();
        remoteParty = res.getToHeader();

        //create the request
        return createRequest(
            lp,
            method,
            callId,
            remoteParty,
            localParty,
            sequenceNumber,
            routeSet,
            remoteTarget,
            res.getStatusCode());
    }

    /**
     *  Follows 12.2.1.1 section in UAC
     *  Create a request within a dialog 
     * @author Function provided by Amirk
     */
    private Request createRequest(
        ListeningPointImpl lp,
        String method,
        String callId,
        ToHeader remoteParty,
        FromHeader localParty,
        long localSequenceNumber,
        LinkedList routeSet,
        NameAddress remoteTarget,
        int responseStatusCode)
        throws SipParseException
    {
        Request retVal = null;

        //fields needed to create the message threw the dialog
        ToHeader toHeader;
        FromHeader fromHeader;
        CallIdHeader callIdHeader;
        CSeqHeader cseqHeader;
        URI reqUri = null;

        HeaderFactory headerFactory =
            SipJainFactories.getInstance().getHeaderFactory();
        
        toHeader = remoteParty;
        fromHeader = localParty;
        if (cloneToFromInAck) {
        	toHeader = (ToHeader)toHeader.clone();
            fromHeader = (FromHeader)fromHeader.clone();
        }

        callIdHeader = headerFactory.createCallIdHeader(callId);

        cseqHeader =
            headerFactory.createCSeqHeader(localSequenceNumber, method);

        if (routeSet == null)
        {
            reqUri = remoteTarget.getAddress();
        }
        else
        {
        	RouteHeader topRoute = (RouteHeader) routeSet.getFirst();
        	URI topRouteUri = topRoute.getNameAddress().getAddress();
        	if (!(topRouteUri instanceof SipURL)) {
        		// some proxy added an invalid Record-Route, violating 3261-16.6-4,
        		// and it was returned in response to INVITE, and now the
        		// application is creating an ACK. throw this to the application:
        		throw new IllegalStateException("route set contains non-SIP URI ["
                    + topRouteUri + ']');
        	}
        	else if (((SipURL)topRouteUri).hasParameter("lr"))
            {
                reqUri = remoteTarget.getAddress();
            }
            else
            {
            	// Strict routing
                reqUri = topRoute.getNameAddress().getAddress();

                routeSet.removeFirst();
				RouteHeader routeHeader =
					SipJainFactories.getInstance()
						.getHeaderFactory()
						.createRouteHeader(remoteTarget);
                routeSet.addLast(routeHeader);
                
                //Mark the message for strict routing by the stack/slsp
                //the next hop will the address in the request uri and not the
                //the top routen
                topRoute = (RouteHeader) routeSet.getFirst();
                ((SipURL)topRouteUri).setParameter(
                			SipStackUtil.STRICT_ROUTING_PARAM, "");
            }
        }

        //create as written , we follow now procedures of 8.1.1 
        retVal =
            createRequest(
                lp,
                method,
                toHeader,
                fromHeader,
                callIdHeader,
                cseqHeader,
                reqUri,
                responseStatusCode);

        if ((routeSet!=null)&&(!routeSet.isEmpty()))
        {
            retVal.setRouteHeaders(routeSet);
        }
        return retVal;
    }

    /**
     * Section 8.1.1 general message creation
     * @author Function provided by Amirk
     * @param lp - for via header
     * @param method - method of message
     * @param toHeader - to whom
     * @param fromHeader - from whom
     * @param callIdHeader - the callId
     * @param cseqHeader - cseq
     * @param reqUri - the target uri
     * @return Request = request built
     * @throws SipParseException - if there is a problem during request creation
     */
    private Request createRequest(
        ListeningPointImpl lp,
        String method,
        ToHeader toHeader,
        FromHeader fromHeader,
        CallIdHeader callIdHeader,
        CSeqHeader cseqHeader,
        URI reqUri,
        int responseStatusCode)
        throws SipParseException
    {

        Request retVal;

        HeaderFactory headerFactory =
            SipJainFactories.getInstance().getHeaderFactory();

        //section 8.1.1.6 - Max-Forwards
        MaxForwardsHeader maxForwardHeader =
            headerFactory.createMaxForwardsHeader(70);

        //section 8.1.1.7 - Via
        ViaHeader viaHeader = null;
        String transport =
            lp.isSecure()
                ? ListeningPointImpl.TRANSPORT_TLS
                : lp.getTransport();
        viaHeader =
            headerFactory.createViaHeader(
                lp.getSentBy(),
                lp.getPort(),
                transport);

        //AmirP 27/07/04 - RFC 17.1.1.3, In case a ACK is generated for none-2xx
        //response - The ACK MUST contain a single Via header field, and this 
        //MUST be equal to the top Via header field of the original request.

        //add branch
        if (responseStatusCode >= 200
            && responseStatusCode < 300
            && !Request.ACK.equals(method)
            && !Request.CANCEL.equals(method))
        {
            viaHeader.setBranch(SIPStackUtil.generateBranchId());
        }
        else
        {
            //TODO
        }
        List viaHeaders = new Vector(10);
        viaHeaders.add(viaHeader);

        retVal =
            SipJainFactories.getInstance().getMesssageFactory().createRequest(
                reqUri,
                method,
                callIdHeader,
                cseqHeader,
                fromHeader,
                toHeader,
                viaHeaders);
        retVal.setMaxForwardsHeader(maxForwardHeader);
        return retVal;

    }
}
