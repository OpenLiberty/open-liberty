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
package com.ibm.ws.sip.container.router;

import jain.protocol.ip.sip.SipException;
import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.SipProvider;
import jain.protocol.ip.sip.TransactionDoesNotExistException;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.ParametersHeader;
import jain.protocol.ip.sip.header.ToHeader;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.ar.SipApplicationRouter;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;
import javax.servlet.sip.ar.SipTargetedRequestInfo;
import javax.servlet.sip.ar.SipTargetedRequestType;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.websphere.sip.AsynchronousWork;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jain.protocol.ip.sip.SipProviderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.JoinHeader;
import com.ibm.ws.jain.protocol.ip.sip.extensions.ReplacesHeader;
import com.ibm.ws.jain.protocol.ip.sip.header.ContactHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.RequestImpl;
import com.ibm.ws.jain.protocol.ip.sip.message.SipResponseCodes;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.appqueue.MessageDispatcher;
import com.ibm.ws.sip.container.asynch.AsynchronousWorkTask;
import com.ibm.ws.sip.container.asynch.AsynchronousWorkTaskListener;
import com.ibm.ws.sip.container.asynch.AsynchronousWorkTasksManager;
import com.ibm.ws.sip.container.events.ContextEstablisher;
import com.ibm.ws.sip.container.events.EventsDispatcher;
import com.ibm.ws.sip.container.failover.repository.SessionRepository;
import com.ibm.ws.sip.container.matching.SipServletsMatcher;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.pmi.LoadManager;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.proxy.ProxyBranchImpl;
import com.ibm.ws.sip.container.router.tasks.EmulateProxyRoute;
import com.ibm.ws.sip.container.router.tasks.InitialRequestRoutedTask;
import com.ibm.ws.sip.container.router.tasks.ResponseRoutedTask;
import com.ibm.ws.sip.container.router.tasks.RoutedTask;
import com.ibm.ws.sip.container.router.tasks.StrayResponseRoutedTask;
import com.ibm.ws.sip.container.router.tasks.SubsequentRequestRoutedTask;
import com.ibm.ws.sip.container.router.tasks.TimeoutRoutedTask;
import com.ibm.ws.sip.container.servlets.IncomingSipServletRequest;
import com.ibm.ws.sip.container.servlets.IncomingSipServletResponse;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipServletResponseImpl;
import com.ibm.ws.sip.container.servlets.SipSessionImplementation;
import com.ibm.ws.sip.container.sessions.SipTransactionUserTable;
import com.ibm.ws.sip.container.transaction.ClientTransaction;
import com.ibm.ws.sip.container.transaction.ServerTransaction;
import com.ibm.ws.sip.container.transaction.SipTransaction;
import com.ibm.ws.sip.container.transaction.TransactionTable;
import com.ibm.ws.sip.container.tu.TUKey;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.util.SipUtil;
import com.ibm.ws.sip.container.util.ThreadContextInputStream;
import com.ibm.ws.sip.container.was.ThreadLocalStorage;
import com.ibm.ws.sip.container.was.WebsphereInvoker;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.ws.webcontainer.webapp.WebApp;
/*TODO Liberty import com.ibm.ws.sip.hamanagment.util.SipClusterUtil;*/

/**
 * 
 * @author Amir Perlman, Jul 2, 2003
 * 
 * Routes the Sip Servlets Requests & Response to SIP Servlets or Proxies.
 */
public class SipRouter {
   
    /**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(SipRouter.class);
    
    /**
     * Singelton
     */
    private static SipRouter s_instance = new SipRouter();

    /**
     * Inovoker for Sip Servlets
     */
    private WebsphereInvoker m_sipletsInvoker;
        
    /**
     * Composition manager (JSR 289)
     */
    private ApplicationPathSelector appPathSelector;

    /**
     * Table for Sip Sessions.
     */
    private SipTransactionUserTable m_transactionUserTable = SipTransactionUserTable.getInstance();

    /**
     * Table for Sip Transactions.
     */
    private TransactionTable m_transactionTable = TransactionTable
        .getInstance();

    /**
     * Indicates whether the SIP router is initialized 
     */
	private boolean m_initialized = false;


    /**
     * Construct an new router.
     * 
     * @param invoker
     *            The Siplets invoker tjat will called to invoke specific
     *            siplets.
     */
    private SipRouter() {
    }
    
    public static SipRouter getInstance() {
    	return s_instance;
    }

    /**
     * Start the router
     * 
     * @param invoker -
     *            the siplet invoker of the sip container
     */
    public void initialize( SipApplicationRouter applicationRouter) {
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "initialize");
        }

        synchronized (this) {
        	if (!m_initialized) {
        		m_initialized  = true;
        	} else {
        		if (c_logger.isTraceDebugEnabled()) {
        			c_logger.traceDebug(this, "initialize",
	                        "The SIP Router has already been initialized");
        		}
        		return;
        	}

            // initialize application path selector
            SipServletsMatcher sipletMatcher = new SipServletsMatcher();
            appPathSelector = new ApplicationPathSelector(sipletMatcher, applicationRouter);
		}

        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "initialize");
        }
    }
    
    
    /**
     * Set the Servlet invoker for the router
     * @param invoker
     */
    public void setInvoker(WebsphereInvoker invoker) {
    	  if (c_logger.isTraceEntryExitEnabled()) {
              c_logger.traceEntry(this, "setInvoker", "Invoker=" +invoker);
          }
    	m_sipletsInvoker = invoker;
    	
    }
    /**
     * Stop the Invoker
     */
    public void stop() {
    	if (m_sipletsInvoker != null) {
    		m_sipletsInvoker.stop();
    	}
    }

    /**
     * Handle a sip request received from the Sip protocol layer (the network).
     * 
     * @param request
     */
    public void handleRequest(SipServletRequestImpl request) {
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "handleRequest", "Request=" + request.getMethod() + " ,callID=" + request.getCallId());
        }
        try{
        	PerformanceMgr perfMgr = PerformanceMgr.getInstance();
    		if (perfMgr != null) {
    			// update performance manager about incoming SIP request
    			perfMgr.requestReceived();
    		}
	
	        // set arrive time of the Request
	        request.setArrivedTime(System.currentTimeMillis());
	
	        try {        
	        	if (request.getMethod().equals(Request.CANCEL)) {
	        		handleCancelRequest(request);
	        	}
	        	else if (SipUtil.isSubsequestRequest(request.getMethod())) {
	        		handleSubsequentRequest(request);
	        	}
	        	else if (request.getMethod().equals(Request.ACK)) {
	        		handleAckRequest(request);
	        	}
	        	else {
	        		handleInitialRequest(request);
	        	}
	        } catch (IllegalStateException e) {
	        	FFDCFilter.processException(e, "com.ibm.ws.sip.container.router.SipRouter.handleRequest", "1");
	        }
        }finally{
        	if (c_logger.isTraceEntryExitEnabled()) {
	            c_logger.traceExit(this, "handleRequest");
	        }
        }
    }
    
    /**
     * Process an Initial request
     * @param request
     */
    private void handleInitialRequest(SipServletRequestImpl request){
    	//Try to locate a session in case these message is part of a
        // dialog
    	TransactionUserWrapper transactionUser = m_transactionUserTable.getTransactionUserForInboundRequest(request);
    	
    	//handle Join/Replaces only if it's an initial request
		//otherwise it can be a Join/Replaces header added by mistake to
		// an established dialog request
    	if ((request.getHeader(JoinHeader.name) != null ||
        		request.getHeader(ReplacesHeader.name) != null) && transactionUser == null) {
    		
    		handleJoinReplaceRequest(request);
    		
        }else if (request.getMethod().equals("ASYNWORK")) {
        	handleAsynchWorkRequest(request);
        }
       else {
            ServerTransaction serverTransaction = null;
            
            if ( !validateInitalDialogRequest(request)){
            	sendErrorResponse(request,
                        SipServletResponse.SC_BAD_REQUEST);
            	return;
            }

            if (transactionUser == null) {
                String toTag = request.getRequest().getToHeader().getTag();
                if (toTag != null) {
                    // To Tag exist - this mean that request belongs to some
                    // dialog and if it's and sipSession wasn't found 
                    // 481 error should be returned
                    sendErrorResponse(request,
                                      SipServletResponse.SC_CALL_LEG_DONE);
                    forwardUnmatchedRequest(request);
                    return;
                }
                
            }
            //New request coming from the network should start a new
            // server transaction other then the case of the Ack for invite (see
            // ACK handling).
            serverTransaction = m_transactionTable.createServerTransaction(request);

            handleRequest(request, transactionUser, serverTransaction);
            
            if (c_logger.isTraceEntryExitEnabled()) {
                c_logger.traceExit(this, "handleRequest");
            }
        }
    }
    
    /**
     * Helper method which is validating if the incoming request
     * is fit the rfc 3261 request definition.
     * @param request
     * @return false if validation failed.
     */
    private boolean validateInitalDialogRequest(SipServletRequestImpl request) {
		if (SipUtil.isDialogInitialRequest(request.getMethod())){
			if(request.getHeader(ContactHeaderImpl.name) == null){
				 if (c_logger.isTraceDebugEnabled()) {
		                c_logger.traceDebug(this, "validateInitalDialogRequest",
		                        "This is dialog inital request - should contain CONTACT header");
		            }
				return false;
			}
		}
		else{
			if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "validateInitalDialogRequest",
                        "This is not dialog inital request.");
            }	
		}
    	return true;
	}
    
    /**
     * Helper method which is responsible to handle request with
     * Join or Replace headers.
     * @param request
     */
    private void handleJoinReplaceRequest(SipServletRequestImpl request) {
    	if( hasValidJoinReplaceHeaders(request)){
    		try {
				processJoinReplaceRequest(request);
			} catch (HeaderParseException e) {
				if (c_logger.isTraceDebugEnabled()) {
	                c_logger.traceDebug(this, "handleJoinReplaceRequest",
	                        "The request contains parsing errors for Join/Replace headers");
	            }
				sendErrorResponse(request,SipServletResponse.SC_BAD_REQUEST);
			} catch (IllegalArgumentException e) {
				if (c_logger.isTraceDebugEnabled()) {
	                c_logger.traceDebug(this, "handleJoinReplaceRequest",
	                        "The request contains parsing errors for Join/Replace headers");
	            }
				sendErrorResponse(request,SipServletResponse.SC_BAD_REQUEST);
			}
    	}
    	else{
			// If more than one Replaces/Join header field is present in an 
    		// INVITE, or if a Replaces/Join header field is present in a request 
    		// other than INVITE, the UAS MUST reject the request with a 
    		// 400 Bad Request response. If both a Replaces header field and 
    		// another header field (Join) with contradictory semantics are 
    		// present in a request, the request MUST be rejected with
    		// a 400 "Bad Request" response.
			if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "handleJoinReplaceRequest",
                        "The request contains illegal number of Join/Replace headers");
            }
            sendErrorResponse(request,SipServletResponse.SC_BAD_REQUEST);
    	} 	
	}
    
    /**
     * Helper method which handles the request contains  "Replace" header.
     * @param request
     * @throws IllegalArgumentException 
     * @throws HeaderParseException 
     */
    private void processJoinReplaceRequest(SipServletRequestImpl request) throws HeaderParseException, IllegalArgumentException {

    	boolean isJoin = false;

    	ParametersHeader header = null;

    	header = (ParametersHeader) request.getRequest().getHeader(
    			JoinHeader.name, true);

    	if (header != null) {
    		isJoin = true;
    	} 
    	else { // Replace Header
    		header = (ParametersHeader) request.getRequest().getHeader(
    				ReplacesHeader.name, true);
    	}
    	/*
    	 * RFC 3891: 
    	 * The Replaces header has specific call control semantics.  If both a
   			Replaces header field and another header field with contradictory
		   semantics are present in a request, the request MUST be rejected with
		   a 400 "Bad Request" response.
		   
		   RFC 3911:
		   If more than one Join header field is present in an INVITE, or if a
		   Join header field is present in a request other than INVITE, the UAS
		   MUST reject the request with a 400 Bad Request response.
		   The Join header has specific call control semantics.  If both a Join
		   header field and another header field with contradictory semantics
		   (for example a Replaces [8] header field) are present in a request,
		   the request MUST be rejected with a 400 "Bad Request" response.
		   
		   TODO need to make more checking here!
    	 */
    	TransactionUserWrapper transactionUser = 
    		m_transactionUserTable.geTUFromJoinReplace(header);
    	
    	if (transactionUser != null){

    		if( ! transactionUser.isTerminated()){
    			if(transactionUser.getInitialDialogMethod().equals(Request.INVITE)) {
    				processJoinReplace(request,transactionUser,header,isJoin);
    			}
    			else {
    				//						 Defect 458974 and 458987 .IF transactionUser doesn't 
    				// represent an INVITE dialog error should be send.
    				sendErrorResponse(request,SipServletResponse.SC_CALL_LEG_DONE);
    				forwardUnmatchedRequest(request);
    			}
    		}
    		else{
    			// Defect 458987.1
    			// If the Replaces header field matches a dialog which has already
    			// terminated, the UA SHOULD decline the request with a 603 Declined
    			// response.
    			sendErrorResponse(request,SipServletResponse.SC_DECLINE);
    		}

    	} 
    	else {
    		processJoinReplace(request,header,isJoin);
    	}
	}

 
    /**
     * Helper method which is handles the INVITE request which contains "Join"
     * or "Replace" header and when TransactionUser was not found
     * @param request
     */
    private void processJoinReplace(SipServletRequestImpl request,
									ParametersHeader header, 
									boolean isJoin) {
    	boolean foundSession = false;
    	TransactionUserWrapper transactionUser = null;
    	
		// No transactionUser was found according to toTag and fromTag
		if (isJoin) {
			transactionUser = m_transactionUserTable.getTuAccordingToUri(request);
			if (transactionUser != null) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger
							.traceDebug(this, "processJoinReplaceRequest",
									"Request will be processed as subsequent request...");
				}
				processSubsequentRequest(request, transactionUser);
				foundSession = true;
			}
		}
		if (!foundSession) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "processJoinReplaceRequest",
						"Failed to find session related session - Forward to Application." +
						" Req = " + request);
			}
			request.setIsInital(true);
			processLegalJoinReplaceRequest(request,null);
		}

	}
    
    /**
     * Helper method which is handles the INVITE request which contains "Join"
     * or "Replaced" header and after TransactionUser was identified from
     * those headers.
     * @param request Incoming INVITE request
     * @param transactionUser Identified TU
     * @param header Join or Replaced header
     * @param isJoin 
     */
    private void processJoinReplace(SipServletRequestImpl request, 
    		TransactionUserWrapper transactionUser, 
    		ParametersHeader header,
    		boolean isJoin) {    	
    	try {
    		// IllegalStateException will be thrown if this TU is not
    		// in the active mode = terminated.
    		transactionUser.ensureTUActive();
    		
    		boolean toContinue = true;
    		
    		if ( !isJoin ) { // Replace header
    			// When this is "Replace" we should check if the Replace
    			// header contains parameter "early-only" - which meaning 
    			// that INVITE can be accepted on the EARLY dialog only.
				if (transactionUser.getState() == SipSessionImplementation.State.CONFIRMED) {
    				if (header.getParameter(SipUtil.EARLY_ONLY_TAG) != null) {
    					sendErrorResponse( request,
    							SipServletResponse.SC_BUSY_HERE);
    					toContinue = false;
    				}
    			}
    			else if ((transactionUser.getState() == SipSessionImplementation.State.EARLY)&& 
    					transactionUser.isServerTransaction()){
    				// defect 458636
    				// If the Replaces header field matches an early dialog that was not
    				// initiated by this UA, it returns a 481 
    				if (c_logger.isTraceDebugEnabled()) {
    					c_logger.traceDebug(this, "processJoinReplace", 
    							"State is EARLY but container is not" +
    					"the initiator of this request");
    				}
    				toContinue = false;
    			}
    		}
    		// If no error was sent in previous if - continue;
    		if (toContinue == true){
    			processLegalJoinReplaceRequest(request,transactionUser);
    		}
    		else{
    			sendErrorResponse(request, SipServletResponse.SC_CALL_LEG_DONE);
    			forwardUnmatchedRequest(request);
    		}
    	} 
    	catch (IllegalStateException e) {
    		if (c_logger.isTraceDebugEnabled()) {
    			StringBuffer buff = new StringBuffer();
    			buff.append("The Dialog was already terminated = ");
    			buff.append(transactionUser);
    			c_logger.traceDebug(this, "processJoinReplaceRequest", buff
    					.toString());
    		}
    		sendErrorResponse(request, SipServletResponse.SC_DECLINE);
    	}
    	
    }

	/**
	 * After the request which contains "Join" or "Replace" header
	 * passed content validation - it can be processed.
	 * @param request
	 * @param transactionUser
	 */
    private void processLegalJoinReplaceRequest(SipServletRequestImpl request, 
    											TransactionUserWrapper transactionUser) {
		
		TransactionUserWrapper tuForNewIncomingRequest = null;
		SipApplicationSessionImpl appSession = null;
		String relatedHeader = null;
		
		SipServletDesc sipDesc = null;
		
        // First, will try to find SipServletDesc for this Request
		if (transactionUser != null){
			
			String applicationName = "";
			SipTargetedRequestType sipTargetedRequestType = null; 

			if (request.getHeader(JoinHeader.name)     != null){
				sipTargetedRequestType = SipTargetedRequestType.JOIN;
				relatedHeader = JoinHeader.name;
			}
			if (request.getHeader(ReplacesHeader.name) != null){
				sipTargetedRequestType = SipTargetedRequestType.REPLACES;
				relatedHeader = ReplacesHeader.name;
			}
			
			applicationName = transactionUser.getAppName();
			
			SipTargetedRequestInfo sipTargetedRequestInfo = 
				new SipTargetedRequestInfo(sipTargetedRequestType, applicationName);  
			
	        sipDesc = appPathSelector.findSippletMatch(request, sipTargetedRequestInfo);
	        
		} else {
	        sipDesc = appPathSelector.findSippletMatch(request, null);
			
		}

        if(transactionUser != null){
        	
			// This new request should be connected to the same
			// Application Session as found TransactionUser.
			appSession = (SipApplicationSessionImpl) transactionUser
										.getApplicationSession(true);
			
			if(sipDesc != null && sipDesc.getSipApp() == appSession.getAppDescriptor()){
				
				tuForNewIncomingRequest = m_transactionUserTable.createTransactionUserWrapper(
						request, true,appSession, (appSession != null));
				
				// Create sipSession in the transactionUser - it will be created
				// anyway by the application. We need it to get this sipSessionID 
				// and save it as related sessionId in the new tuForNewIncomingRequest.
				SipSession sipsession = transactionUser.getSipSessionFromBase(true);

				//Link between new session and related  session defined in "Join"
				// or "Replace" headers
				tuForNewIncomingRequest.setRelatedSessionData(sipsession.getId(), relatedHeader);
			}
			else{
				if (c_logger.isTraceDebugEnabled()) {
					StringBuffer buff = new StringBuffer();
					buff.append("Matched application = ");
					buff.append(sipDesc);
					buff.append(" different then related to the TU according to Replaces / Join header, app = ");
					buff.append(appSession.getAppDescriptor());
					c_logger.traceDebug(this, "processLegalJoinReplaceRequest",
							buff.toString());
				}
	        }				
		}		
        if (tuForNewIncomingRequest == null) {
        	
			// if we didn't find TU according to the Replaces or Join header ,
			// or if TU was found but it had different Application then matched
			// Application
        	if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "processLegalJoinReplaceRequest", 
						"Create a new TU for this incoming request");
			}
        	tuForNewIncomingRequest = m_transactionUserTable.createTransactionUserWrapper(
    												request, true, null, (appSession != null));
        }		
        		
        // New request coming from the network should start a new
        // server transaction other then the case of the Ack for invite (see
        // ACK handling).
		ServerTransaction serverTransaction = 
			m_transactionTable.createServerTransaction(request);
		request.setTransaction(serverTransaction);
		request.setIsInital(true); 
		RoutedTask task = InitialRequestRoutedTask.getInstance(
				tuForNewIncomingRequest, serverTransaction, request,
				sipDesc);
		
		if( task != null){
        	MessageDispatcher.dispatchRoutedTask(task);
        }      
	}

	/**
     * Helper method which checks if there is right number of Join or
     * Replace headers, and that the request is INVITE.
     * @param request
     * @return
     */
    private boolean hasValidJoinReplaceHeaders(SipServletRequestImpl request){
    	boolean isOK = true;
		boolean hasJoinHeader = false;

		if (request.getMethod() == Request.INVITE) {

			ListIterator headers = request.getHeaders(JoinHeader.name);
			if (headers != null && headers.hasNext()) {
				hasJoinHeader = true;
				headers.next();
				if (headers.hasNext()) {
					isOK = false;
				}
			}
			if (isOK) {
				headers = request.getHeaders(ReplacesHeader.name);
				if (headers != null && headers.hasNext()) {
					if (hasJoinHeader) {
						isOK = false;
					} 
					else {
						headers.next();
						if (headers.hasNext()) {
							isOK = false;
						}
					}
				}
			}
		}
		else{
			isOK = false;
		}
		
		if (!isOK) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger
						.traceDebug(this, "hasMultipleJoinReplaceHeaders",
								"This request has illegal number of Join / Replace headers.");
			}
		}
		return isOK;
    }

	/**
	 * Process an incoming ACK request.
	 * 
	 * @param request
	 */
    private void handleAckRequest(SipServletRequestImpl request) {

        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "handleAckRequest");
        }
    	
    	TransactionUserWrapper transactionUser = null;
        ServerTransaction serverTransaction = null;

        // Check if we have a transaction for the incoming request -
        // ACKs for non 2xx response to invites will go on the same transaction
        // as the dialog is not created in case of failure.
        serverTransaction = (ServerTransaction) m_transactionTable
                .getTransaction(request.getTransactionId());
        
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "handleAckRequest",
                    "serverTransaction=" + serverTransaction);
        }

        if (null == serverTransaction) {
            //Note that the search for the transaction could still fail
            //if the invite was completed with a 2xx response. Look for a dialog.
            transactionUser = m_transactionUserTable.getTransactionUserForInboundRequest(request);
            
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "handleAckRequest",
                        "serverTransaction=null tranasctionUser=" + transactionUser);
            }

            if (transactionUser != null) {
                //We have a dialog, create a new transaction.
                serverTransaction = m_transactionTable
                        .createServerTransaction(request);
            }
//            System.out.println("Debug: serverTransaction was null handleAckRequest request = " + request + ", new serverTransaction = " + serverTransaction + ", transactionUser = " + transactionUser);
        }
        else {
        	((IncomingSipServletRequest)request).setAckOnErrorResponse(true);
        	
            //Use the same sipsession for Invite and its matching Ack
            transactionUser = serverTransaction.getOriginalRequest()
                    .getTransactionUser();
            
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "handleAckRequest",
                        "serverTransaction=" + serverTransaction + 
                        " tranasctionUser=" + transactionUser + 
                        " on the error response");
            }
        }
        
        //Continue processing
        handleRequest(request, transactionUser, serverTransaction);

        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "handleAckRequest");
        }
    }
    
    /**
     * Process an ASYNWORK timeout.
     * 
     * @param transaction
     */
    private void handleAsynchTimeOut(SipTransaction transaction) {
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(this, "handleAsynchTimeOut");
    	}

    	//get the tu from the original request, at this stage it is not found on the response
        TransactionUserWrapper transactionUser = transaction.getOriginalRequest().getTransactionUser();
        
    	String callid = transaction.getOriginalRequest().getCallId();

    	//remove the transaction from the transaction table
    	if ((transaction instanceof ClientTransaction)){
    		if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this, "handleAsynchTimeOut", "removing transaction from table, call-id: " + callid);
    		}
    		((ClientTransaction) transaction).clearTransaction();

    		//remove the tuWraaper from the thread local, the async thread should not use the 
    		//ready to invalidate mechanism
    		ThreadLocalStorage.cleanTuForInvalidate();
    	}

    	AsynchronousWorkTask task = 
    		AsynchronousWorkTasksManager.instance().getAsynchronousWorkTask(callid);
    	if (task == null) {
    		if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this, "handleAsynchTimeOut", "Can not find AsynchronousWorkTask for call-id" + callid);
    		}
    		return;
    	}

    	try{
    		task.sendFailedResponse(SipServletResponse.SC_REQUEST_TIMEOUT);
    	}finally {
    		if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this, "handleAsynchTimeOut", "Response is sent. Removing AsynchronousWorkTask for call-id" + callid);
    		}
    		AsynchronousWorkTasksManager.instance().removeAsynchronousWorkTask(callid);
    		
    		//invalidate the Asynch sip TU
    		transactionUser.invalidateTU(true, true);
    	}

    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceExit(this, "handleAsynchTimeOut");
    	}
    }

    /**
	 * Process an incoming ASYNWORK response.
	 * 
	 * @param response
	 */
    private void handleAsynchWorkResponse(SipServletResponse response, SipTransaction transaction) {
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "handleAsynchWorkResponse");
        }
        
        //get the tu from the original request, at this stage it is not found on the response
        TransactionUserWrapper transactionUser = transaction.getOriginalRequest().getTransactionUser();
        
		//remove the transaction from the transaction table
		if ((transaction instanceof ClientTransaction)){
			if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "handleAsynchWorkResponse", "removing transaction from table, call-id: " + response.getCallId());
            }
			((ClientTransaction) transaction).clearTransaction();
			
			//remove the tuWraaper from the thread local, the async thread should not use the 
			//ready to invalidate mechanism
			ThreadLocalStorage.cleanTuForInvalidate();
		}
        
        AsynchronousWorkTask task = 
        	AsynchronousWorkTasksManager.instance().getAsynchronousWorkTask(response.getCallId());
        if (task == null) {
        	if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "handleAsynchWorkResponse", "Can not find AsynchronousWorkTask for call-id" + response.getCallId());
            }
        	return;
        }
        ClassLoader currentThreadClassLoader = null;
        
        //save the current class loader so we will set it back after we finished the de-serialization
        currentThreadClassLoader = Thread.currentThread().getContextClassLoader();
        
        //switch to the application class loader
		Thread.currentThread().setContextClassLoader(task.getCl());
        
		// Now if the task is existing we have to send a response to an application listener
        try {
        	if (is2xx(response.getStatus())) {
    		   byte [] data = response.getRawContent();
    		   Serializable obj = null;
    		   if(data != null){
	    		   ByteArrayInputStream bin = new ByteArrayInputStream(data);
	    		   ObjectInputStream ois = new ThreadContextInputStream(bin);
	    		   
	    		   obj = (Serializable)ois.readObject();
    		   }

    		   task.sendCompletedResponse(obj);
        	}
        	else {
    		   task.sendFailedResponse(response.getStatus());
        	}
        } catch (IOException e) {
        	if (c_logger.isErrorEnabled()){
				c_logger.error("error.exception.io",Situation.SITUATION_UNKNOWN,null,e);        	
			}
		} catch (ClassNotFoundException e) {
			if (c_logger.isErrorEnabled()){
				c_logger.error("error.exception.cnf",Situation.SITUATION_UNKNOWN,null,e);        	
			}
		}
		finally {
			if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "handleAsynchWorkResponse", "Response is sent. Removing AsynchronousWorkTask for call-id" + response.getCallId());
            }
			AsynchronousWorkTasksManager.instance().removeAsynchronousWorkTask(response.getCallId());
			 if(currentThreadClassLoader != null){
				 Thread.currentThread().setContextClassLoader(currentThreadClassLoader);
			 }
			 
			 //invalidate the Asynch sip TU
			 transactionUser.invalidateTU(true, true);
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "handleAsynchWorkResponse");
        }
    }
    
    /**
	 * Process an incoming ASYNWORK request.
	 * 
	 * @param request
	 */
    private void handleAsynchWorkRequest(SipServletRequestImpl request) {

        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "handleAsynchWorkRequest");
        }
    	
        ServerTransaction serverTransaction = m_transactionTable.createServerTransaction(request);
        
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "handleAsynchWorkRequest",
                    "serverTransaction=" + serverTransaction);
        }
        
        // Bind the request to the transaction
        request.setTransaction(serverTransaction);

        
        
        // Continue processing
        processAsynchWorkRequest(request);
        
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "handleAsynchWorkRequest");
        }
    }
    
    /**
     * Continue processing the ASYNWORK request.
     * 
     * @param request
     */
	private void processAsynchWorkRequest(SipServletRequestImpl request) {
		if (isRetransmission(request) == true) {
			if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "processAsynchWorkRequest", 
                		"Got retrasmitted asynchronois work request, do nothing");
			}
			return;
		}
		
		byte[] body = request.getMessage().getBodyAsBytes();
		SipURI uri = (SipURI) request.getRequestURI();
        String appSessionId = uri.getParameter("ibmappid");
        
        SipApplicationSessionImpl sipAppSession = 
        	(SipApplicationSessionImpl)SipApplicationSessionImpl.getAppSession(appSessionId);
        
        TransactionUserWrapper transactionUser = 
        		m_transactionUserTable.createTransactionUserWrapper(request, true, null, false);
        request.setTransactionUser(transactionUser);
		ContextEstablisher contextEstablisher = null;
        ClassLoader currentThreadClassLoader = null;
        Object obj = null;
        
		try {
			// Send a SIP error if the session is not owned by this server
			if(sipAppSession == null) {
				if (c_logger.isErrorEnabled()){
					c_logger.error("SipApplicationSession wasn't found for id:" + appSessionId);
				}
				sendErrorResponse(request, SipResponseCodes.CLIENT_CALL_OR_TRANSACTION_DOES_NOT_EXIST);
				return;
			}
				
			//	first, stop retransmissions
			//SipServletResponse response = request.createResponse(SipResponseCodes.INFO_TRYING);
			//response.send();
			// it does not stop the retransmition since SIPTransactionStack defines it as non-INVITE message
	        
	        String appName = sipAppSession.getApplicationId();
			SipAppDesc sipAD = SipContainer.getInstance().getRouter().getSipApp(appName);
			
	       	ByteArrayInputStream bin = new ByteArrayInputStream(body);
	       	ThreadContextInputStream tcis = new ThreadContextInputStream(bin);
			contextEstablisher = sipAD.getContextEstablisher();			 
			
			if (contextEstablisher != null){
				currentThreadClassLoader = 
						contextEstablisher.getThreadCurrentClassLoader();
				contextEstablisher.establishContext();
			}
			
		    obj = tcis.readObject();
		    
		} catch (IOException e) {
			if (c_logger.isErrorEnabled()){
				c_logger.error("error.exception.io",Situation.SITUATION_UNKNOWN,null,e);        	
			}
		} catch (ClassNotFoundException e) {
			if (c_logger.isErrorEnabled()) {
				c_logger.error("error.exception.cnf" , Situation.SITUATION_UNKNOWN, null, e);        	
            }
		}finally {
			if (obj != null) {
				((AsynchronousWork)obj).dispatch(new AsynchronousWorkTaskListener(request));
			}
			
			if (contextEstablisher != null) {
				contextEstablisher.removeContext(currentThreadClassLoader);
			}
		}
	}
    
	private boolean isRetransmission(SipServletRequestImpl request) {
		return
			(AsynchronousWorkTasksManager.instance().getAsynchronousWorkTask(request.getCallId()) != null);
	}
    
    /**
     * Process an incoming BYE Subsequent request on the dialog
     * @param request
     */
    private void handleSubsequentRequest(SipServletRequestImpl request) {
    	TransactionUserWrapper transactionUser = null;
        
        //Try to locate a session in case these message is part of a  dialog
        transactionUser = m_transactionUserTable.getTransactionUserForInboundRequest(request);
        
        if(transactionUser == null){
            if (c_logger.isTraceDebugEnabled()) {
                StringBuffer buff = new StringBuffer();
                buff.append("Unable to find dialog for Subsequent request ");
                buff.append(request.getMethod());
                buff.append(" with Call-Id");
                buff.append(request.getCallId());
				c_logger.traceDebug(this, "handleSubsequentRequest",buff.toString());
            }
            if(request.getMethod().equals(RequestImpl.NOTIFY)){
            	//This is an initial unsolicited NOTIFY. We need to allow it to 
                //start a dialog.
            	handleInitialRequest(request);
            	return;
            }
            sendErrorResponse(request, SipServletResponse.SC_CALL_LEG_DONE);
            forwardUnmatchedRequest(request);
        }
        else {
        	if (transactionUser.isTransactionUserInvalidated() || transactionUser.isInvalidating()){
    			if( request.getMethod().equals(Request.ACK)){
    				if (c_logger.isTraceDebugEnabled()) {
    					c_logger.traceDebug(this, "handleSubsequentRequest",
    							"ACK request (callid="+ request.getCallId() +")will not be processed since "
    							+ transactionUser + " was already invalidated");
    				}
    				return; //Cannot send response to an ACK< but the process should be aborted.
    			}
    			sendErrorResponse(request, SipServletResponse.SC_CALL_LEG_DONE);
    			forwardUnmatchedRequest(request);
    			return;
    		}
        	processSubsequentRequest(request,transactionUser);
        }
    }
    
    /**
     * Helper method which is actually processing the Subsequent request
     * when TransactionUser was already found.
     * @param request
     * @param transactionUser
     */
    private void processSubsequentRequest(SipServletRequestImpl request,
    		TransactionUserWrapper transactionUser){
    	ServerTransaction serverTransaction = null;

    	//Moti: defect 581678 :: when a BYE (or any subsequent req) comes
    	// on an INVITE which was cancelled (or any dialog which was set
    	// to INITIAL state) - we must return 481.
    	boolean isInitial = transactionUser.getState() == SipSessionImplementation.State.INITIAL;
    	
    	//We must exclude Notify from this test since Notify is a special case, Subsequent Notify requests can create a dialog for
    	//Initial Subscribe just like 200OK (rfc3265) 
    	if (isInitial && !request.getMethod().equals(RequestImpl.NOTIFY)) {
    		if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this, "processSubsequentRequest",
    					"found sub request on a dialog in INITIAL state. aborting. returning 481");
    		}
    		sendErrorResponse(request, SipServletResponse.SC_CALL_LEG_DONE);
    		forwardUnmatchedRequest(request);
    		return;
    	}else{
    		if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this, "processSubsequentRequest", "state initial " + isInitial + " ,request method: " + request.getMethod());
    		}
    	}

    	if (transactionUser.getState() != SipSessionImplementation.State.CONFIRMED
    			&& ! SipUtil.canReceiveOnDialog(request.getMethod(),
    					transactionUser)) {
    		// This transaction user was not confirmed yet with the final
    		// response.
    		// Only the PRACK request can be sent on non CONFIRMED dialog
    		if (c_logger.isTraceDebugEnabled()) {
    			StringBuffer buff = new StringBuffer();
    			buff.append("This dialog is not confirmed yet. Is it UAS = ");
    			buff.append(transactionUser.isServerTransaction());
    			c_logger.traceDebug(this, "handleSubsequentRequest", buff
    					.toString());
    		}
    	}
    	serverTransaction = m_transactionTable.createServerTransaction(request);
    	// Continue processing
    	handleRequest(request, transactionUser, serverTransaction);
    }

    /**
     * Process an incoming CANCEL request.
     * @param request A CANCEL request
     */
    private void handleCancelRequest(SipServletRequestImpl request) {
        //Cancel request for invites are sent on a different transaction
        //from the Original invite. We need to track the original server
        //transaction send cancel request on a new transaction for the
        //same session as the invite.
        //We needed to add a patch in the stack to allow more easily to
        // track
        //down the original invite transaction without having to search
        //through all the available transaction or maintain another data
        //structure for all transactions which is already maintained by the
        //stack. We should be able to get away from this once we move
        //to jain 1.1 and the entire dialog will be managed at one
        //place - the stack
        
    	TransactionUserWrapper transactoinUser = null;
        ServerTransaction serverTransaction = null;
        ServerTransaction inviteTr;
        RequestImpl jainReq = (RequestImpl) request.getRequest();

        //Get the transaction id for the original INVITE that is being
        //cancelled.
        long trId = jainReq.getOriginInviteTransaction();
        inviteTr = (ServerTransaction) m_transactionTable
            .getTransaction(trId);
        if (null != inviteTr) {
            transactoinUser = inviteTr.getOriginalRequest().getTransactionUser();
        }
        
        // trying to retrieve TU from request tags, we do this because this part of the stack
        // is not synchronized and an invite may not set the request in some cases.
        if (transactoinUser == null) {
        	transactoinUser = m_transactionUserTable.getTransactionUserForInboundRequest(request);            	
        }
        
        if(transactoinUser != null && !transactoinUser.isInvalidating()){
        	// Defect 439923. When CANCEL received on TU which is already invalidated
        	// in case when TU sent error response and still waitng for ACK - 
        	// CANCEL should be answered with SC_CALL_LEG_DONE.
            serverTransaction = m_transactionTable.createServerTransaction(request);
            //Continue processing 
            handleRequest(request, transactoinUser, serverTransaction);
        } else {            
            //send an Error response on Cancel request that was recieved
            // on the terminated transaction - Invite was already answered 
            // with final response
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "handleRequest",
                        "Error processing CANCEL request, unmatched INVITE transaction");
            }
           sendErrorResponse(request,SipServletResponse.SC_CALL_LEG_DONE);
           forwardUnmatchedRequest(request);
        }
    }
    
    /**
     * This method is used to process all errors not related to the request but to the container
     * (For example, overload, quiesce etc...) and all processing that's related to container state.
     * 
     * @param request
     * @param transactionUser
     * @param serverTransaction
     * @return
     */
    private boolean handleContainerWarningsAndErrors(SipServletRequestImpl request, 
            TransactionUserWrapper transactionUser, 
            ServerTransaction serverTransaction) {
    	
        if (null == serverTransaction) {
            //Still no server transaction this must an error/bug we must have
            //a matching transaction by now

            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "handleRequest",
                    "Error processing request, unmatched transaction/dialog");
            }
            //The error response should NOT be sent on ACK
            if (!request.getMethod().equals(Request.ACK)) {
                sendErrorResponse(request,
                    SipServletResponse.SC_REQUEST_TERMINATED);
            }

            return true;
        }


    	if (LoadManager.getInstance().shouldThrowMsgs() && transactionUser == null) {
        	//Check if container is not overloaded and send error response
        	//if it is....
        	//The error response should NOT be sent on ACK
    		
    		if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this, "handleRequest",
    			"The container is overload - error will be sent...");
    		}

    		if(!request.getMethod().equals(Request.ACK)) {
    			sendErrorResponse(request,
    					SipServletResponse.SC_TEMPORARLY_UNAVAILABLE);
    		}
    		return true;
    	}
    	
    	if (SipContainer.getInstance().isInQuiesce() /*TODO Liberty && !SipClusterUtil.isServerInCluster() - replace with true*/) {
    		// blocking all initial transactions when server is quienced
    		// this is a specific handling for standalone, cluster is handled
    		// through the proxy which will stop diverting messages to the container.
    		if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this, "handleRequest",
    			"Container in QUIESCE_MODE. No new calls are allowed - error will be sent...");
    		}


    		sendErrorResponse(request, SipServletResponse.SC_SERVICE_UNAVAILABLE);
    		if (c_logger.isTraceEntryExitEnabled()) {
    			c_logger.traceExit(this, "handleRequest");
    		}
    		return true;
    	}
    	
    	
    	return false;
    }
    
    

	/**
	 * Continue processing of an incoming request after a transaction/session have
	 * been associated with the request. 
	 * @param request
	 * @param transactionUser Could be null in that case a new session will be created
	 * @param serverTransaction Must exist otherwise the request will be rejected
	 */    
	private void handleRequest(SipServletRequestImpl request, 
	                           TransactionUserWrapper transactionUser, 
	                           ServerTransaction serverTransaction)
	 {
        //Bind the request to the transaction
        request.setTransaction(serverTransaction);

		SipServletDesc sipDesc = null;
		
		if (handleContainerWarningsAndErrors(request, transactionUser, serverTransaction)) {
			return;
		}
        
        RoutedTask task = null;
        
        if (transactionUser != null) {        
            //if (transactionUser != null) {
            	task = 
            		SubsequentRequestRoutedTask.getInstance( transactionUser, 
            				serverTransaction, 
            				request); 
            	
        } else {
        	/*
        	 * Allocate a new SIP Session for the incoming request associated with the
        	 * specified transaction.After the session is created and matched to a
        	 * specific siplet/app according to the configuration from the sip.xml,
        	 * listeners will be notified of the newly created session.
        	 */
        	// If there is no next application selected, 
        	// first, will try to find SipServletDesc for this Request

        	//we mark the request as intial at this point. 
        	//It has to happen here so that implementors of  
        	//custom SIP Application Routers will get an initial request.
        	request.setIsInital(true);

        	// Handle encoded uri case. Initial request can have a information
        	// according which application session it should reffer.
        	SipApplicationSessionImpl sipApplication = SipUtil.getApplicationSessionAccordingToEncodedUri(request);

        	SipTargetedRequestInfo sipTargetedRequestInfo = null;

        	String applicationName = null;
        	if (sipApplication != null){
        		Serializable stateInfo = (Serializable) sipApplication.getAttribute(SipUtil.IBM_STATE_INFO_ATTR);
        		if (stateInfo != null){
        			if (c_logger.isTraceDebugEnabled()) {
        				c_logger.traceDebug(this, "handleRequest", "handling route back request with stateInfo: " + stateInfo);
        			}  
        			//this is a route back request, set the saved stateInfo on the request for the AR to use 
        			request.setStateInfo(stateInfo);
        			request.setDirective(SipApplicationRoutingDirective.CONTINUE);
        			sipApplication.invalidate();
        			sipApplication = null;
        		}else{
        			applicationName = sipApplication.getAppDescriptor().getApplicationName();

            		sipTargetedRequestInfo = new SipTargetedRequestInfo(
            				SipTargetedRequestType.ENCODED_URI  , applicationName);
        		}
        	}

        	// Find application siplet for invocation
			sipDesc = 
				appPathSelector.findSippletMatch(request, sipTargetedRequestInfo);
        	
        	if (sipDesc == null && !request.isExternalRoute()) {
        		if (c_logger.isErrorEnabled()) {
        			c_logger.error("error.mapping.to.nonexisting.siplet",
        					Situation.SITUATION_REQUEST, new Object[]{"Method="+request.getMethod() + 
        					", callID=" + request.getCallId() , "Unknown"});
        		}
        		//Request did not match any matching rule. This is a configuration
        		//problem
        		int errorCode = PropertiesStore.getInstance().getProperties().getInt(CoreProperties.SIP_NO_ROUTE_ERROR_CODE_PROPERTY);
        		
        		sendErrorResponse(request, errorCode);
        		return;
        	}
        	
        	if (request.isExternalRoute()) {
        		transactionUser = m_transactionUserTable.createTransactionUserWrapper(request, true, sipApplication, (sipApplication != null));

        		request.setTransactionUser(transactionUser);

        		task = EmulateProxyRoute.getInstance(request);
        	}
        	
        	if( null != sipDesc) {
        		
            	// if application router chooses another application to handle this request
            	if(applicationName != null && !applicationName.equals(sipDesc.getSipApp().getApplicationName())){
            		sipApplication = null;
            	}
        		
            	// retrieve sip application session id using the sip session key based key
            	// only applicable for 289 applications.
            	String sipAppKeyBase = null;
            	if (sipDesc.getSipApp().isJSR289Application() && sipApplication == null) {
            		sipAppKeyBase = SipUtil.getKeyBaseTargetingKey(sipDesc.getSipApp(), request);

            		if (sipAppKeyBase != null) {
            			if (c_logger.isTraceDebugEnabled()) {
            				c_logger.traceDebug(this, "handleRequest", "found session key base key: " + sipAppKeyBase);
            			}                    	

            			String sipAppID = SessionRepository.getInstance().getKeyBaseAppSession(sipAppKeyBase);
            			
            			if (c_logger.isTraceDebugEnabled()) {
            				c_logger.traceDebug(this, "handleRequest", "sipAppID = " + sipAppID);
            			}
            			
            			if (sipAppID != null) {
            				if (c_logger.isTraceDebugEnabled()) {
            					c_logger.traceDebug(this, "handleRequest", "found session app id: " + sipAppID);
            				}                    		
            				sipApplication = SipApplicationSessionImpl.getAppSession(sipAppID);
            			}
            		}
            		else{
            			if (c_logger.isTraceDebugEnabled()) {
            				c_logger.traceDebug(this, "handleRequest", "There is no session key method defined for this application");
            			}   
            		}
            	}
        		
        		//Create a new session and the target siplet according to the
        		//triggering rules in the sip.xml files.
        		transactionUser = m_transactionUserTable.createTransactionUserWrapper(request, true, sipApplication, (sipApplication != null));

        		request.setTransactionUser(transactionUser);
        		
        		if (sipAppKeyBase != null && sipApplication == null) {
        			transactionUser.setSessionKeyBase(sipAppKeyBase);
        		}
        		
    			task = 
    				InitialRequestRoutedTask.getInstance( transactionUser, 
    						serverTransaction, 
    						request, sipDesc); 
        	}
        }
        
		// update PMI with new inbound request
		// this will happen only if custom property "pmi.count.all.massages" is enabled
		// otherwise - PMI will update only with inbound request in non proxy application that is forwarded to the application
        PerformanceMgr perfMgr = PerformanceMgr.getInstance();
		if (perfMgr != null  && PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.PMI_COUNT_ALL_MESSAGES)){
			perfMgr.updatePmiInRequest(request.getMethod(), transactionUser, sipDesc);
		}
        if( task != null){
        	MessageDispatcher.dispatchRoutedTask(task);
        }
        if (c_logger.isTraceEntryExitEnabled()) {
        	c_logger.traceExit(this, "handleRequest");
        }
    }
    
	/**
     * Send the Error respone to the UAC if the incommng Request cannot be
     * handled by the Container
     * 
     * @param request
     *            Original request
	 * @param response
     *            Response error
     */
	public static void sendErrorResponse(SipServletRequestImpl request, int response) {
		sendErrorResponse(request, response, null);
	}
	
	
	/**
     * Send the Error respone to the UAC if the incommng Request cannot be
     * handled by the Container
     * 
     * @param request
     *            Original request
	 * @param response
     *            Response error
	 * @param reasonPhrase 
	 * 			  reason phrase
     */
    public static void sendErrorResponse(SipServletRequestImpl request, int response, String reasonPhrase) {
        try {
        	if (c_logger.isTraceEntryExitEnabled()){
    			c_logger.traceEntry(SipRouter.class, "com.ibm.ws.sip.container.router.SipRouter.sendErrorResponse", response);
    		}
        	PerformanceMgr perfMgr = PerformanceMgr.getInstance();
    		if (perfMgr != null) {
        		perfMgr.updateRejectedMessagesCounter();
    		}
        	//if this is an initial request (does not have a to tag) we need to add the to tag
        	//according to RFC3261 12.1.1
        	String toTag = ((SipServletRequestImpl) request).getRequest().getToHeader().getTag();
        	if (toTag == null || toTag.length() == 0){
        		//add to tag to the request before sending the error response so the response will be created correctly
        		String tag = null;
        		if (request.getTransactionUser() != null){
        			tag = request.getTransactionUser().generateLocalTag();
        		}else{
        			//generate a random number tag
        			StringBuilder builder = new StringBuilder();
        			builder.append(Math.random());
        			//trim the 0. from the tag
        			tag = builder.substring(2);
        		}

        		if (c_logger.isTraceDebugEnabled()){
        			c_logger.traceDebug(SipRouter.class, "com.ibm.ws.sip.container.router.SipRouter.sendErrorResponse", "To-Tag was added to an error initial request, tag:" + tag);
        		}
        		request.getRequest().getToHeader().setTag(tag);
        	}else{
        		if (c_logger.isTraceDebugEnabled()){
        			c_logger.traceDebug(SipRouter.class, "com.ibm.ws.sip.container.router.SipRouter.sendErrorResponse", "To-Tag was found, no need to add to-tag, tag:" + toTag);
        		}
        	}
        	
            request.getSipProvider()
                .sendResponse(request.getTransactionId(), response, reasonPhrase);
            boolean avayaDebug = PropertiesStore.getInstance().getProperties().getBoolean("avaya.debug.load");
            
            TransactionTable transactionTable = TransactionTable.getInstance();
    		SipTransaction transaction = request.getTransaction();
    		if(transaction == null){
    			if(avayaDebug){
    				System.out.println("transaction was not found on request when sending error: "+ response);
    			}
    			if (c_logger.isTraceDebugEnabled())
                {
	    			c_logger.traceDebug(null, 
	            			"sendErrorResponse", 
	            			"transaction was not found on request when sending error: "+ response);
                }
    		}else{
	    		if(avayaDebug){
					System.out.println("removing transaction after error response send: "+ response);
				}
	    		if (c_logger.isTraceDebugEnabled())
	            {
	                c_logger.traceDebug(null, 
	                        			"sendErrorResponse", 
	                        			"removing transaction after error response send: "+ response);
	            }
	    		transaction.markAsTerminated();
	    		
	    		// this is the place when transaction is usually was already removed from the table.
	    		// if "removed = false" - it is not necessary means that this transaction remains in table
	    		// or listener was not updated about the transactions counter
	    		boolean removed = transactionTable.removeTransaction(transaction);
	    		
	    		
	    		if(c_logger.isTraceDebugEnabled())
	    		{
	    			c_logger.traceDebug(SipRouter.class, "sendErrorResponse" ,
	    								"Transaction was removed from table = " + removed);
	    		}
	    		
    		}
        } catch (TransactionDoesNotExistException e) {
        	// Galina: fix defect #598406
        	FFDCFilter.processException(e, "com.ibm.ws.sip.container.router.SipRouter.sendErrorResponse", "1");
        	if (c_logger.isTraceDebugEnabled()) { // print the exception into the log only if trace debug enabled 
        		c_logger.traceDebug(SipRouter.class, "com.ibm.ws.sip.container.router.SipRouter.sendErrorResponse", "error.sending.response", e);
            }
        } catch (SipParseException e) {
            if (c_logger.isErrorEnabled()) {
                c_logger.error("error.sending.response",
                               Situation.SITUATION_CREATE, null, e);
            }
        } catch (SipException e) {
            if (c_logger.isErrorEnabled()) {
                c_logger.error("error.sending.response",
                               Situation.SITUATION_CREATE, null, e);
            }
        }
        
        if (c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(SipRouter.class, "com.ibm.ws.sip.container.router.SipRouter.sendErrorResponse");
		}
    }
   
    /**
     * Handle a sip response received from the Sip protocol layer (the network).
     * 
     * @param response
     */
    public void handleResponse(SipServletResponseImpl response) {
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntry(this, "handleResponse");
        }
        //set arrive time of the Request
        response.setArrivedTime(System.currentTimeMillis());
        
        PerformanceMgr perfMgr = PerformanceMgr.getInstance();
		if (perfMgr != null) {
        	perfMgr.responseReceived();
		}
        SipTransaction transaction = m_transactionTable.getTransaction(response.getTransactionId());
        if (null == transaction) {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(
                    this,
                    "handleResponse",
                    "got response for null transaction: " + response);
            }
        }
        else {
        	if (response.getMessage().getCSeqHeader().getMethod().equals("ASYNWORK")) {
            	handleAsynchWorkResponse(response, transaction);
            }
        	else {
        		TransactionUserWrapper transactionUser = transaction.getOriginalRequest().getTransactionUser();
        		String sessionId = null;
				Response resp = (Response)response.getMessage();
				if (transactionUser == null) {

					sessionId = m_transactionUserTable.getTransactionUserIdAccordingToTopVia(resp);

					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "handleResponse", "Retrieved TU from response: " + sessionId);
					}
				}
				/** The following code goal is to detect a response from different branch in proxy with the same to-tag:*/
				int generatedContainerErrorOnToTagDuplication = PropertiesStore.getInstance().getProperties().getInt(CoreProperties.GENERATED_CONTAINER_ERROR_ON_TO_TAG_DUPLICATION);
				if (generatedContainerErrorOnToTagDuplication != 0) {
					detectDuplicatedToTag(response, transaction, generatedContainerErrorOnToTagDuplication);
        		}

        		RoutedTask task;
        		if (sessionId != null) {
        			task = ResponseRoutedTask.getInstance(transaction, sessionId, response);
        		} else {
        			task = ResponseRoutedTask.getInstance(transaction, transactionUser, response);
        		}
        		
        		MessageDispatcher.dispatchRoutedTask(task);
				// update PMI with new inbound response
				// this will happen only if custom property "" is enabled
				// otherwise - PMI will update only with inbound response in non proxy application        		
        		if (perfMgr != null && PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.PMI_COUNT_ALL_MESSAGES)){
					perfMgr.updatePmiInResponse(response.getStatus(), transactionUser);
				}
				
			}

		}

        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "handleResponse");
        }
    }
	/**
	 * The method detects a case when there are 2 proxy response on different branches with the same to-tag.
	 * When detected, the response to-tag is being replaces with a unique tag and an error message is created
	 * @param response
	 * @param transaction
	 * @param generatedContainerErrorOnToTagDuplication - the error code to replace the response with the duplicated to-tag
	 */
	private void detectDuplicatedToTag(SipServletResponseImpl response, SipTransaction transaction, int generatedContainerErrorOnToTagDuplication){
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "detectDuplicatedToTag");
		}
		
		Response resp = (Response)response.getMessage();
		String sessId = m_transactionUserTable.getTransactionUserIdAccordingToTopVia(resp);
		String fromTag =	resp.getFromHeader().getTag();
		String toTag =		resp.getToHeader().getTag();
		if (fromTag != null && toTag != null && sessId != null){
			TUKey key = ThreadLocalStorage.getTUKey();
			key.setup(fromTag, toTag, sessId, true);
			// checks if a transaction user with the same key as the response already exists
			// if it exist it means that there was a previous response with the same to-tag as the current response
			TransactionUserWrapper respTuw = SessionRepository.getInstance().getTuWrapper(key);
			if (respTuw != null && transaction instanceof ClientTransaction){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "detectDuplicatedToTag", "Found the same transaction of the response already exist in client transaction mode");
				}
				ClientTransaction clientTran = (ClientTransaction)transaction;
				// if this is a case of proxy
				if (clientTran.getListener() instanceof ProxyBranchImpl){
					ProxyBranchImpl pbi = (ProxyBranchImpl) clientTran.getListener();
					try {
						// verify that the 2 responses with the same to-tag are on different branches
						// (2 responses with same to-tag on the same branch are possible, for instance in the case of 180 and 200)
						if (!pbi.getBranchId().equals(respTuw.getBranch().getBranchId())){
							if (c_logger.isErrorEnabled()) {
								c_logger.error("error.same.to.tag");
								if (c_logger.isTraceDebugEnabled()){
									c_logger.traceDebug("Detected 2 responses for proxy with the same to-tag in different branches! branch1=  " + pbi.getBranchId() + ", branch2= "+respTuw.getBranch().getBranchId());
								}
								//set the response code to 408
								response.setStatus(generatedContainerErrorOnToTagDuplication, "Invalid dialog ID received");
								//replace the same wrong to tag with a unique tag
								ToHeader toHeader = resp.getToHeader();
								toHeader.setTag(generateToHeaderForSameToTag(toHeader.getTag()));
								resp.setToHeader(toHeader);

							}
						}
					}catch (SipParseException e) {
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(this, "detectDuplicatedToTag", "parse error: "+e.getMessage(), e);
						}
						return;
					}catch (IllegalArgumentException e) {
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(this, "detectDuplicatedToTag", "illegal argument error: "+e.getMessage(), e);
						}
						return;
					}
				}
			}
		}

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "detectDuplicatedToTag");
		}
	}
	
	/**
	 * Helper method for generating a unique to-tag to replace a duplicated same to tag on responses from different branches
	 * @return generatedToTag - a unique generated to-tag
	 */
	private String generateToHeaderForSameToTag(String original_duplicated_tag){
		String generatedToTag = "DuplicatedTag(" + original_duplicated_tag + ")" + System.nanoTime()/100000L;
		return generatedToTag; 
	}

    /**
     * Handle transaction timeout.
     * 
     * @param transactionId
     *            Transaction the timed out.
     */
    public void handleTimeout(long transactionId) {
        if (c_logger.isTraceEntryExitEnabled()) {
            Object[] params = { Long.toString(transactionId) };
            c_logger.traceEntry(this, "handleTimeout", params);
        }

        SipTransaction transaction = m_transactionTable.getTransaction(transactionId);
        
        if (null != transaction) {
        	if (transaction.getOriginalRequest().getMethod().equals("ASYNWORK")) {
            	handleAsynchTimeOut(transaction);
            }else{
	        	RoutedTask task = TimeoutRoutedTask.getInstance(transaction);
	        	MessageDispatcher.dispatchRoutedTask(task);
            }
        }
        else {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger
                    .traceDebug(
                                this,
                                "handleTimeout",
                                "Timeout for non existing transaction, " + transactionId);
            }
        }

        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceExit(this, "handleTimeout");
        }
    }


 

    /**
     * Invoke the specified Sip Servlet.
     * 
     * @param request
     *            The Sip Servlet Request if available otherwise null.
     * @param response
     *            The Sip Servlet Response if available otherwise null.
     * @param sipServletDescParam
     *            The siplet to be invoked.
     * @param listener
     *            Get notification after invoking the siplet
     *  
     */
    public void invokeSipServlet(SipServletRequest request,
                                 SipServletResponse response,
                                 SipServletDesc sipServletDescParam,
                                 SipServletInvokerListener listener) {
        SipServletDesc sipServletDesc = sipServletDescParam;

        if (null == sipServletDesc) {
            appPathSelector.getDefaultHandler();
        }

        if (null != sipServletDesc) {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "invokeSipServlet",
                                    "Invoking Siplet: " + sipServletDesc);

            }

            m_sipletsInvoker.invokeSipServlet(request, response,
                                              sipServletDesc, listener);
        }
        else {
            if (c_logger.isErrorEnabled()) {
                c_logger.error("error.default.invoke.siplet.not.found",
                               Situation.SITUATION_REQUEST, null);
            }
        }

    }

    /**
     * Unload the specified application from the list of application currentlly
     * running in the container.
     * 
     * @param appName
     */
    public void unloadAppConfiguration(String appName) {
    	if(appPathSelector != null) {
    		appPathSelector.unloadApplicationConfiguration(appName);
    	}
    }

    /**
     * Gets a Sip Servlet Desc object according to the siplet's names. Searches
     * throught all application and tries to find matching siplet.
     * 
     * @param name
     *            The name of the siplet as appears in the sip.xml file.
     * @return The matching Sip Servlet Descriptior if available othewise null.
     */
    public SipServletDesc getSipletByName(String name) {
        return appPathSelector.getSipletByName(name);
    }

    /**
     * Checks if the specified request is participates in a chain of Application
     * Composition.
     * 
     * @param request
     * @return the name of the application to be invoked next
     *         otherwise null
     */
    public String checkForApplicationComposition(SipServletRequestImpl request) {
        
        String nextApplication = null;
        
        SipServletDesc sipplet =  appPathSelector.findSippletMatch(request, null);
        if (sipplet != null){
        	nextApplication = sipplet.getSipApp().getApplicationName();
        }

        return nextApplication;
    }
    /**
     * Returns all active applications
     * @return
     */
    public LinkedList<SipAppDesc> getAllApps() {
    	if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "getAllApps");
		}
    	LinkedList<SipAppDesc> list = null;
    	if(appPathSelector != null) {
    		
    		list = appPathSelector.getAllApps();
    	}else{
    		if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this,"getAllApps" ,"appPathSelector is null");
    		}
    	}
    	return  list;
	}

    /**
     * Gets the SIP App descriptor for the given application name.
     * 
     * @param name
     *            The name of the SIP Application.
     * @return The SIP App Descriptor if available, otherwise null
     */
    public SipAppDesc getSipApp(String name) {
    	SipAppDesc appDesc = null;
    	
    	if (appPathSelector != null) {
    		appDesc = appPathSelector.getSipApp(name);
    		// If appDesc is null - this is because this application was not loaded yet.
    		//Try to look in SipAppDescManager;
    	}
    	else {
    		//TODO Change this to error when the translation process is opened again
        	if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "getSipApp", "appPathSelector is null, probably because the SIP Container was not started. Check that there is a SIP module installed.");
            }
    	}
    	if(appDesc == null){
    		if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "getSipApp", "Try to get this application from SipAppDescManager.");
            }
    		appDesc = SipAppDescManager.getInstance().getSipAppDescByAppName(name);
    		if(appDesc == null) {
    			appDesc = SipAppDescManager.getInstance().getSipAppDesc(name);
    		}
    	}
    	return appDesc;
    }

    /**
     * RFC 18.1.2, 13.3.1.4
     * We are expecting to get stray responses in the case the original was
     * INVITE that was proxied and the UAS receiving the request is retransmitting
     * the 2xx response. In that case we will just send it back to the transport
     * layer so it will be send to its destination e.g. UAC that generated the
     * request. 
     * @param response
     * @param provider
     */
    public void handleStrayResponses(Response response, SipProvider provider) {
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "hanldeStrayResponses", response.toString());
        }
        
        if(!isExpectedStrayResponse(response))
        {
        	forwardUnmatchedResponse(response, provider);
        	PerformanceMgr perfMgr = PerformanceMgr.getInstance();
    		if (perfMgr != null) {
        		perfMgr.updateRejectedMessagesCounter();
    		}
            return;
        }
        
        TransactionUserWrapper transactionUser = 
            		   m_transactionUserTable.getTransactionUserInboundResponse(response);
        RoutedTask task = null;
        if(null != transactionUser)
        {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "hanldeStrayResponses", 
                		"Transaction: " + transactionUser.getId());
            }
        	task = StrayResponseRoutedTask.getInstance(transactionUser, response, provider,false);
        }
        else if ((transactionUser = m_transactionUserTable.getBaseTUForDerived(response))!= null){
        	if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, 
                					"hanldeStrayResponses", 
                					"New derived should be created for base = " +transactionUser.getId());
            }
        	task = StrayResponseRoutedTask.getInstance(transactionUser, response, provider,true);
        }
        if (task == null)
        {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "hanldeStrayResponses", "No transaction");
            }
            //We should be here if we are in proxy mode, In this we will try
             //to pass the response upstream according to the via headers. 
            sendResponseDirectlyToTransport(provider, response,true);
        }
        else{
        	MessageDispatcher.dispatchRoutedTask(task);
        }
        
    }

    /**
     * Send the resonse directly to the transport layer bypassing the transaction
     * Will be used by 2xx retransmission from a UAS downstream through our
     * proxy to the UAC upstream. 
     * @param provider
     * @param response
     * @param containsOurVia 
     */
    public static void sendResponseDirectlyToTransport(SipProvider provider, 
														Response response,
														boolean containsOurVia) {
    	PerformanceMgr perfMgr = PerformanceMgr.getInstance();
		
    	if(!response.hasViaHeaders())
        {
            //Avoid sending the response as it no via headers. We could be
            //in some kind of a loop - Drop it.
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(SipRouter.class, "sendResponseDirectlyToTransport", 
                    "Dropping response, no via headers available - 1, call Id" + 
                     response.getCallIdHeader().getCallId());
            }
            forwardUnmatchedResponse(response,provider);
    		if (perfMgr != null) {
    			perfMgr.updateRejectedMessagesCounter();
    		}
            return;
        }
        
        if(containsOurVia){
					
        	// When response is a stray response which received when
					// Application Acts as a proxy - it has an Via header which represents
					// this container and should be removed before send. 
        	// If response is a response created by the 
        	// application it has only the Via header represents UAC 
        	// that should receive this response.
	        
        	//Remove the top via header as it should be ours
	        response.removeViaHeader();
	        
	        //Check again as it could have just a single via header. 
	        if(!response.hasViaHeaders())
	        {
	            //Avoid sending the response as it no via headers. We could be
	            //in some kind of a loop - Drop it.
	            if (c_logger.isTraceDebugEnabled()) {
	                c_logger.traceDebug(SipRouter.class, "sendResponseDirectlyToTransport", 
	                    "Dropping response, no more via headers available - 2, call Id" + 
	                     response.getCallIdHeader().getCallId());
	            }
	           forwardUnmatchedResponse(response,provider); 
	           if (perfMgr != null) {
	        	   perfMgr.updateRejectedMessagesCounter();	            
	           }
	           return;
	        }
        }
        
        try {
            //Send back to the transport layer. 
            ((SipProviderImpl)provider).sendResponse(response);
        }
        catch (SipException e) {
            if(c_logger.isErrorEnabled())
            {
                c_logger.error("error.exception", "Send Failure", null, e);
            }
        }
    }

    /**
     * Call to all appropriate listeners that unmatched request received in the SipContainer.
     * @param req
     */
    public static void forwardUnmatchedRequest(SipServletRequestImpl req) {

    	if(req == null){
    		return;
    	}
    	
    	if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(SipRouter.class, "forwardUnmatchedRequest", 
                "Unmatched request" + req);
    	}
    	IncomingSipServletRequest inReq = ((IncomingSipServletRequest)req);
    	inReq.setUnmatchedReqeust();
    	EventsDispatcher.unmatchedRequestReceived(inReq);
	}

    /**
     * Notify listeners about unmatched stray response receive by the SipContainer.
     * @param response
     */
    private static void forwardUnmatchedResponse(Response response, SipProvider provider) {

    	if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(SipRouter.class, "forwardUnmatchedStrayResponse", 
                "Unmatched response" + response);
    	}
    	
    	SipServletResponseImpl servletResponse = 
            new IncomingSipServletResponse(response, -1, provider);
       
    	// ANAT: ???? Why ?
        servletResponse.setIsCommited(false); 
        
        EventsDispatcher.unmatchedResponseReceived(servletResponse);
	}
    
	/**
     * Check if the response is 2xx for INVITE or it is a retransmission for
     * 1xx Reliable response - otherwise ignore since we don't expect to get 
     * other response. 
     * @param response
     * @return
     */
    private boolean isExpectedStrayResponse(Response response) {
        boolean rc = false;
        try {
            int statusCode = response.getStatusCode();
            
            if (is2xx(statusCode)) {
                if (response.getCSeqHeader().getMethod().equals(Request.INVITE)) {
                    if (c_logger.isTraceDebugEnabled()) {
                        c_logger.traceDebug(this, "hanldeStrayResponses",
                                        "Handled");
                    }
                    rc = true;
                }
            }            
        }
        catch (SipParseException e) {
            if(c_logger.isErrorEnabled())
            {
                c_logger.error("error.exception", "Parse Failure", null, e);
            }
        }
        
        return rc; 
    }

    /**
     * Checks whether the specified reason code is a 2xx response. 
     * @param statusCode
     * @return
     */
    private boolean is2xx(int statusCode) {
        if(statusCode >= 200 && statusCode < 300)
        {
            return true;
        }
        
        return false;
    }
    

    /**
	 * Load the application configuration for the the SIP application matching the WebAPp
	 * @param webApp
	 */
	public void loadAppConfiguration(WebApp webApp) {
		if(appPathSelector != null){
		 appPathSelector.loadAppConfiguration(webApp);
		
		 if (c_logger.isTraceDebugEnabled()) {
	            c_logger.traceDebug(this, "loadAppConfiguration",
	                                "App installed: " + webApp.getName());
	        }
		}
		else{
			if (c_logger.isTraceDebugEnabled()) {
	            c_logger.traceDebug(this, "loadAppConfiguration",
	                                "appPathSelector is not initialized yet, will update about loaded applications later in  notifyRouterOnDeployedApps() method.");
	        }
		}

	}

	/**
	 * Notify the SipApplicationRouter about all the deployed applications and load thier configuration
	 * This should be called only when the SipApplicationRouter is initialized and not on later applications
	 */
	public void notifyRouterOnDeployedApps() {
		if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "notifyRouterOnDeployedApps","");
        }
		this.appPathSelector.notifyRouterOnDeployedApps();
		
	}
	
	
	/**
	 * Whether the SipRouter was already initialized or not
	 * @return
	 */
	public synchronized boolean isInitialized(){
		return m_initialized;
	}
    
}
