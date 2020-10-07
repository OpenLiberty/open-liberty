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
import jain.protocol.ip.sip.address.AddressFactory;
import jain.protocol.ip.sip.address.NameAddress;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.header.HeaderFactory;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.RouteHeader;
import jain.protocol.ip.sip.message.Request;

import java.io.IOException;

import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.container.protocol.OutboundProcessor;
import com.ibm.ws.sip.container.router.SipRouter;
import com.ibm.ws.sip.container.servlets.IncomingSipServletRequest;
import com.ibm.ws.sip.container.servlets.IncomingSipServletResponse;
import com.ibm.ws.sip.container.servlets.OutgoingSipServletRequest;
import com.ibm.ws.sip.container.servlets.OutgoingSipServletResponse;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipSessionSeqLog;
import com.ibm.ws.sip.container.tu.TransactionUserImpl;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.stack.properties.StackProperties;
import com.ibm.ws.sip.stack.util.SipStackUtil;

/**
 * @author Amir Perlman, Dec 9, 2004
 *
 * Proxy for subsequest request on a dialog when proxying in Record Route mode.
 * 
 *  <p> RFC 3261 16.12 Summary of Proxy Route Processing <p> 

    In the absence of local policy to the contrary, the processing a proxy 
    performs on a request containing a Route header field can be summarized in 
    the following steps. 

      1.  The proxy will inspect the Request-URI.  If it indicates a
          resource owned by this proxy, the proxy will replace it with
          the results of running a location service.  Otherwise, the
          proxy will not change the Request-URI. 
          This action is NOT performed by the container as it is expected be 
          handled by the application prior to starting the proxy operation. 

      2.  The proxy will inspect the URI in the topmost Route header
          field value.  If it indicates this proxy, the proxy removes it
          from the Route header field (this route node has been
          reached).

      3.  The proxy will forward the request to the resource indicated
          by the URI in the topmost Route header field value or in the
          Request-URI if no Route header field is present.  The proxy
          determines the address, port and transport to use when
          forwarding the request by applying the procedures in [4] to
          that URI. 
 */
public class RecordRouteProxy {

    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(RecordRouteProxy.class);
    
    /**
     * Defintion for the "lr" parameter
     */
    public static final String LR = "lr";
    
    /**
     * Jain SIP header factory
     */
    private static final HeaderFactory c_hdrFactory = 
        					StackProperties.getInstance().getHeadersFactory();
    
    /**
     * Jain SIP Address factory
     */
    private static final AddressFactory c_addrFactory = 
        					StackProperties.getInstance().getAddressFactory();
    
    /**
     * Proxies the specified response upstream. This function should BEFORE the 
     * application received the response.  
     * @param origRequest The request to be proxied
     */
    public final static void proxyResponse(IncomingSipServletRequest origRequest, 
                                     SipServletResponse inResponse) {
        if (c_logger.isTraceEntryExitEnabled()) {
            Object[] params = { origRequest.getMethod(), inResponse.getReasonPhrase() };
            c_logger.traceEntry(RecordRouteProxy.class, "proxyResponse", params);
        }
        
        // Proxy Provisional response is not committed.
		((IncomingSipServletResponse)inResponse).setIsCommited(false);
		
		
        //Log incoming resopnse to Session's sequnence log
        TransactionUserWrapper tu = origRequest.getTransactionUser();
        tu.logToContext(SipSessionSeqLog.PROXY_SUBSEQUENT_RESP, 
            				 inResponse.getStatus(), inResponse);
        
        if (inResponse.getStatus() > 100){
        	OutgoingSipServletResponse outResponse = 
        		BranchManager.createOutgoingResponse(origRequest, inResponse);

        	//Pass the response back to the session for processing by the 
        	//siplet in case we are in supervised mode. 
        	origRequest.getTransactionUser().processSubsequentProxyResponse(outResponse);
        }else{
        	if (c_logger.isTraceDebugEnabled()) {
        		c_logger.traceDebug(RecordRouteProxy.class, "proxyResponse", "ignoring 100 response for subsequent request");
        	}
        }
    }
    
    /**
     * Proxies the specified request down stream. This function should be called 
     * after the  application received the request and modified the request. 
     * @param inRequest The request to be proxied
     * @parma sipSession The SIP Session associated with this request/transaction 
     */
    public final static void proxyRequest(SipServletRequest inRequest, 
                                          TransactionUserWrapper transactionUser) 
    {
        if (c_logger.isTraceEntryExitEnabled()) {
            Object[] params = { inRequest.getMethod() };
            c_logger.traceEntry(RecordRouteProxy.class, "proxyRequest", params);
        }
        
        //Create an outgoing request based on this request 
        SipServletRequestImpl inRequestImpl = (SipServletRequestImpl) inRequest;
        TransactionUserWrapper tu = inRequestImpl.getTransactionUser();
        OutgoingSipServletRequest outRequest = 
        		BranchManager.createOutgoingRequest(inRequestImpl);
        outRequest.setTransactionUser(tu);
        outRequest.setIsSubsequentRequest(true);
        
        try {
            //Update path according to route headers
            updateRoutingPath(inRequest, outRequest, transactionUser);
        }
        catch (SipParseException e) {
            logException(e);
        }catch (ServletParseException e) {
            logException(e);
        }

        // if the original request contains a flow token in the top
        // Route header field value, set it as the IBM-Destination header
        // in the outbound request. this tells the stack (or the proxy in
        // cluster) to route the request over an existing connection.
        OutboundProcessor outboundProcessor = OutboundProcessor.instance();
        outboundProcessor.forwardingRequest(inRequestImpl, outRequest, null);

        //Create a listener for this transaction to catch the responses
        SubsequentRequestListener listener = 
            				new SubsequentRequestListener(inRequest);
        outRequest.setClientTransactionListener(listener);
        
        tu.logToContext(SipSessionSeqLog.PROXY_SUBSEQUENT_REQ, 
            				 outRequest.getRequestURI(), outRequest);
        //Send the request
        sendRequestDownStream(outRequest);
    }
    
    
    /**
     * Send the specifed request down stream.  
     * @param request
     */
    private static void sendRequestDownStream(SipServletRequest request)
    {
        if (c_logger.isTraceEntryExitEnabled()) 
        {
            Object[] params = { request.getMethod() };
            c_logger.traceEntry(RecordRouteProxy.class, "sendRequestDownStream", 
                                params);
        }
        
        try 
        {
            request.send();
        }
        catch (IOException e) 
        {
            if (c_logger.isErrorEnabled())
            {
                c_logger.error(
                    "error.exception",
                    Situation.SITUATION_REQUEST,
                    null,
                    e);
            }
        }
    }
    
    /**
     * Update the path of the reques/response according to the route headers
     * within the message as specified in RFC 3261 Section 16.12.x
     * @param inRequest incoming request
     * @param outRequest out going request
     * @param tUser Proxying session asssociated with thses requests
     * @throws SipParseException
     * @throws ServletParseException
     */
    private final static void updateRoutingPath(SipServletRequest inRequest,
                                          OutgoingSipServletRequest outRequest,
                                          TransactionUserWrapper tUser) 
    	throws SipParseException,ServletParseException
    {
        if (c_logger.isTraceEntryExitEnabled()) 
        {
            Object[] params = { tUser };
            c_logger.traceEntry(RecordRouteProxy.class, "updateRoutingPath", 
                                params);
        }
        
        String topRouteSidParam = null;
        String multihomeIndexParam = null;
        boolean routeExist = false;
        
        //if we in JSR289 application the top route should be taken from the request
        //popped route
        boolean isJSR289 = tUser.getSipServletDesc().getSipApp().isJSR289Application();
        if (isJSR289){
        	Address routeAddr = inRequest.getPoppedRoute();
        	if (routeAddr != null){
        		topRouteSidParam = routeAddr.getURI().getParameter(TransactionUserImpl.SESSION_RR_PARAM_KEY);
        		routeExist = true;
        	}
        }else{
        	RouteHeader topRoute = getTopRoute(inRequest);
        	if (topRoute != null){
        		topRouteSidParam = getSessionIdParamFromRoute(topRoute);
        		routeExist = true;
        	}
        }
        
        if (routeExist){
        	//Get the session's unique identifier
            // If this was created as derived session we need to look for
            // different ID in the RR.
            String sessionId = tUser.getSharedIdForDS();
            
            if(sessionId.equals(topRouteSidParam))
            {
                //Session Id exists in top route - we are in loose routing mode
                processLooseRouting(inRequest, outRequest, sessionId, isJSR289);
            }
            else
            {	
                //either a problem/bug or we are have Strict router in front
                //of us. 
                processStrictRouting(inRequest, outRequest, sessionId);
            }
           
        }
        else
        {
            if(c_logger.isErrorEnabled())
            {
                Object[] args = { inRequest }; 
                c_logger.error("error.route.header.unavailable", 
                               Situation.SITUATION_CREATE, args);
            }
            
            generateErrorResponse(inRequest);
        }
    }
    
    
    /**
     * Performs Loose routing as specifid in RFC 3261. This function assumes 
     * that we are in Loose routing and does not perform any additional checks.
     * @param inRequest
     * @param outRequest
     * @param sessionId
     * @throws SipParseException
     * @throws IllegalArgumentException
     */
    private final static void processLooseRouting(SipServletRequest inRequest, 
                       OutgoingSipServletRequest outRequest, String sessionId, boolean isJSR289) 
    throws IllegalArgumentException, SipParseException, ServletParseException 
    {
        if (c_logger.isTraceEntryExitEnabled()) 
        {
            Object[] params = { sessionId };
            c_logger.traceEntry(RecordRouteProxy.class, "processLooseRouting", 
                                params);
        }
       
        
        Request outRequestJain = ((SipServletRequestImpl)outRequest).getRequest();
        Request inRequestJain = ((SipServletRequestImpl)inRequest).getRequest();
        
        //when in JSR116 application the top route needs to be removed here, after the application code was invoked
        //JSR289 applications removes the route header before the application is called
        if(! isJSR289){
	        // Remove the top route header as it is ours
	        RouteHeader currentRoute = getTopRoute(outRequest);
	        outRequestJain.removeHeader(RouteHeader.name, true);
	        
	        
	        // In cases where we switch transport on an outgoing message we add two record
	        // route headers. One for each transport.
	        // On the next in dialog requests we will have two Route headers
	        // when we get a route header we will check for the next header to see if we have
	        // a case of two Route headers and remove it as well.
	        String sessionKey = getSessionIdParamFromRoute(currentRoute);
	        if (sessionKey != null) {
	        	RouteHeader nextRoute = getTopRoute(outRequest);
	        	if (nextRoute != null) {
	                String nextSession = getSessionIdParamFromRoute(nextRoute);
	            	
	            	if (sessionKey.equals(nextSession)) {
	                    if (c_logger.isTraceDebugEnabled()) {
	                        c_logger.traceDebug(RecordRouteProxy.class, "processLooseRouting", 
	                          "found two record route headers with the same session id, removing both of them (" + sessionKey + ")");
	                    }
	            		
	            		outRequestJain.removeHeader(RouteHeader.name, true);
	            	}
	        	}
	        }
	        
        }
        
        RouteHeader nextRoute = getTopRoute(outRequest);
        if(null != nextRoute && isStrictRouter(nextRoute))
        {
            //Switch from Loose routing (RFC 3261) to Strict Routing 
            //(RFC 2543)
            
            if (c_logger.isTraceDebugEnabled()) 
            {
                c_logger.traceDebug(RecordRouteProxy.class, "processLooseRouting", 
                  "Switching from loose routing to strict routing" + nextRoute);
            }
                        
            //Remove next route header and push it into the request uri
            outRequestJain.removeHeader(RouteHeader.name, true);
            outRequestJain.setRequestURI(nextRoute.getNameAddress().getAddress());
            
            //Add the request URI as another route header at the end
            //of the list of route headers.
            NameAddress nameAddr = c_addrFactory.createNameAddress(inRequestJain.getRequestURI());
            outRequestJain.addHeader(c_hdrFactory.createRouteHeader(nameAddr), false); 

            //Mark the request as for strict routing. 
            markRequestAsStrictRouting(outRequest);
            
        }
        else
        {
            //No need to do anything we are in loose routing mode
            //next route points to the next hop.
        }
        
    }



    /**
     * Perform Strict Routing - RFC 2543. This function should only be called
     * when are already in Strict Routing mode which means that some proxy 
     * before us on the path is a Strict Route. 
     * @param inRequest
     * @param outRequest
     * @param sessionId
     * @throws SipParseException
     * @throws ServletParseException
     */
    private final static void processStrictRouting(SipServletRequest inRequest, 
                     OutgoingSipServletRequest outRequest, String sessionId) 
    throws SipParseException, ServletParseException 
    {
        if (c_logger.isTraceEntryExitEnabled()) 
        {
            Object[] params = { inRequest, outRequest, sessionId };
            c_logger.traceEntry(RecordRouteProxy.class, 
                                "processStrictRouting", params);
        }
        Request jainInReq = ((SipServletRequestImpl)inRequest).getRequest();
        Request jainOutReq = ((SipServletRequestImpl)outRequest).getRequest();
        
        String reqURISidParam = 
            		getSessionIdParamFromURI(jainInReq.getRequestURI());
        
        //We should have our own id in the request URI otherwise something
        //is wrong here. 
        if(sessionId.equals(reqURISidParam))
        {        	
            //Remove next route header and push it into the request uri 
            RouteHeader nextRoute = getTopRoute(outRequest);
            jainOutReq.removeHeader(RouteHeader.name, true);
            jainOutReq.setRequestURI(nextRoute.getNameAddress().getAddress());
            
            //Mark the request as for strict routing. 
            markRequestAsStrictRouting(outRequest);
        }
        else
        {
            if(c_logger.isErrorEnabled())
            {
                Object[] args = {inRequest }; 
                c_logger.error("error.processing.strict.routing", 
                                Situation.SITUATION_CREATE, args);
            }
            
            generateErrorResponse(inRequest);
        }
        
    }


    /**
     * Marks the request so it should routed according to strict routing - 2543.
     * If we have an additional route header then mark the next route as 
     * strict routing for the stack/slsp so the request's destination will 
     * be selected according to the request uri instead of the top route 
     * header.
     * @param outRequest
     */
    private static void markRequestAsStrictRouting(OutgoingSipServletRequest outRequest) {
         
        RouteHeader nextRoute = getTopRoute(outRequest);
        if(null != nextRoute)
        {
            jain.protocol.ip.sip.address.URI uri= nextRoute.getNameAddress().getAddress();
            if(uri instanceof SipURL)
            {
	            try {
	                ((SipURL)uri).setParameter(SipStackUtil.STRICT_ROUTING_PARAM, "");
	            }
	            catch (IllegalArgumentException e) {
	                logException(e);
	            }
	            catch (SipParseException e) {
	                logException(e);
	            }
            }
        }
        
    }



    /**
     * Generate error response back up stream as we are unable to process 
     * request. 
     * @param inRequest
     */
    private final static void generateErrorResponse(SipServletRequest inRequest) 
    {
        if(inRequest.getMethod().equals(Request.ACK))
        {
            //Can not send a respone to a ACK
            return;
        }
        
        SipRouter.sendErrorResponse((SipServletRequestImpl)inRequest, SipServletResponse.SC_BAD_REQUEST);   
    }

    /**
     * Checks whether the specified route header indicates a Strict Router
     * RF2543. 
     * @param route Route to check 
     * @return
     */
    private final static boolean isStrictRouter(RouteHeader route) 
    {
        boolean rc = false; 
        jain.protocol.ip.sip.address.URI uri = route.getNameAddress().getAddress();
        
        if(uri instanceof SipURL)
        {
            rc = ((SipURL)uri).getParameter(LR) == null;
        }
        else
        {
            if(c_logger.isErrorEnabled())
            {
                Object[] args = { route }; 
                c_logger.error("error.unkown.uri.type", 
                               Situation.SITUATION_CREATE, args);
            }
        }

        return rc;
    }



    /**
     * Gets the Session Id parameter from the given route header. 
     * 
     * @param address Address Header to look within its parameters for the 
     * session id. 
     * @return The session identifier or null if not available
     */
    public final static String getSessionIdParamFromRoute(RouteHeader route) 
    {
        return getSessionIdParamFromURI(route.getNameAddress().getAddress());
    }

    
    /**
     * Gets the Session Id parameter from the given uri.  
     * @param uri
     * @return The session paramater if available otherwise null
     */
    public final static String getSessionIdParamFromURI(jain.protocol.ip.sip.address.URI jainUri) 
    {
        String sid = null;
        if(jainUri instanceof SipURL)
        {
            sid = ((SipURL)jainUri).getParameter(TransactionUserImpl.SESSION_RR_PARAM_KEY);
        }
        else
        {
            //Session Ids are expect only SIP URIs since we put it in there
            if (c_logger.isTraceDebugEnabled()) 
            {
                c_logger.traceDebug("Can't obtain session ID from non sip URI: " + jainUri);
            }
        }

        return sid;

    }
    

    /**
     * Helper function to get the top route header of a request. 
     * @param inRequest
     * @return
     */
    public final static RouteHeader getTopRoute(SipServletRequest request) 
    {
        RouteHeader topRoute = null; 
        
        Request jainReq = ((SipServletRequestImpl) request).getRequest();
        try 
        {
            //Get the first route header
            topRoute = (RouteHeader) jainReq.getHeader(RouteHeader.name, true);
        }
        catch (HeaderParseException e) 
        {
            if (c_logger.isErrorEnabled()) 
            {
                c_logger.error("error.failed.get.route.header",
                    Situation.SITUATION_CREATE, null, e);
            }

        }
        catch (IllegalArgumentException e) 
        {
            if (c_logger.isErrorEnabled()) 
            {
                c_logger.error("error.failed.get.route.header",
                    Situation.SITUATION_CREATE, null, e);
            }
        }
        
        return topRoute;
    }
    
    /**
     * Utilitiy function for logging exceptions.
     * 
     * @param e
     */
    protected static void logException(Exception e) 
    {
        if(c_logger.isErrorEnabled())
        {
            c_logger.error("error.exception", 
                           Situation.SITUATION_CREATE, 
                    	   null, e);
        }
    }
}
