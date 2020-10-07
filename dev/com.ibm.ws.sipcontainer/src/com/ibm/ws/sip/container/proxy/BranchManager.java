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

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;

import javax.servlet.sip.*;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;

import com.ibm.sip.util.log.*;
import com.ibm.ws.jain.protocol.ip.sip.ListeningPointImpl;
import com.ibm.ws.jain.protocol.ip.sip.address.AddressFactoryImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.ReasonHeaderImpl;
import com.ibm.ws.jain.protocol.ip.sip.extensions.SupportedHeader;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.protocol.OutboundProcessor;
import com.ibm.ws.sip.container.servlets.*;
import com.ibm.ws.sip.container.sessions.SipTransactionUserTable;
import com.ibm.ws.sip.container.transaction.ClientTransactionListener;
import com.ibm.ws.sip.container.tu.TransactionUserImpl;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.util.OutboundInterface;
import com.ibm.ws.sip.container.util.SipUtil;
import com.ibm.ws.sip.container.was.ThreadLocalStorage;
import com.ibm.ws.sip.parser.SipConstants;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.ws.sip.stack.properties.StackProperties;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.sip.stack.util.SipStackUtil;

import jain.protocol.ip.sip.*;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.header.*;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

/**
 * 
 * @author anat
 * 
 * Class which is a base class for StatefullProxy and ProxyBranch.
 * This class will be responsible to  manage all branches, select
 * best response and update the "parent" about it.
 * Parent can be ProxyBranch (if this object was created as result of 3xx 
 * received) or StatefullProxy when this branch was created by StatefullProxy
 * explicitly, or ProxyDirector if this object represents StatefullProxy.
 *
 */
abstract class BranchManager implements ProxyParent
{
    /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(BranchManager.class);

    /**
	 * Holds all ProxyBranches
	 */
	protected ArrayList <ProxyBranchImpl> _proxyBranches = new ArrayList <ProxyBranchImpl> (5);
    
	
	//	[StartedProxyBranch][StartedProxyBranch][NewlyCreated_PB][NewlyCreated_PB][NewlyCreated_PB]
	// 	When StartedProxyBranch is a branch on which call StartProxying or proxyTo() was called.
	//  When NewlyCreated_PB is a branch that was only created by the application and before it started.
	
	/**
	 * Index of last executed Branch
	 */
	protected int _nextBranchToExecute  = 0;
    
    /**
	 * Holds list of header field name which should NOT be copied
	 * 	to the new request while proxying
	 */
	private static Hashtable <String,String> c_notCopiedRequestHeader = new Hashtable <String,String>();

	/**
	 * Holds list of header field name which should NOT be copied
	 * 	to responses while proxying
	 */
	private static Hashtable <String,String> c_notCopiedResponseHeader = new Hashtable <String,String>();

    /**
	 * Number of open branches that have not been cancelled or completed. 
	 */
	protected int _activeBranchCount = 0; 
	
	/**
	 * flag specifying whether the servlet engine will automatically recurse 
	 * or not. If recursion is enabled the servlet engine will automatically 
	 * attempt to proxy to contact addresses received in redirect (3xx) 
	 * responses.
	 */
	protected boolean _isRecurse = true;  
	

	/**
	 * Parameter which indicates if this Proxy was started  - proxyTo()
	 * or startProxyin() called; When started some of the flags cannot be
	 * set.
	 */
	protected boolean _started = false;

	/**
	 * The original Sip Servlet Request. 
	 */
	protected SipServletRequestImpl _originalReq; 
    
	
    /**
     * Handle the Path header string used to add Path header to the 
     * outgoing request.
     */
    protected SipURI _pathUri = null;  
	
    
    /**
	 * The record route URI 
	 */
	protected SipURI _recordRouteURI;

	
    /**
     * The best response that arrived so far
     */
    protected StatefulProxyBestResponse _bestResponse =
        new StatefulProxyBestResponse();

    /**
	 * String which represents basic information about this object
	 */
	private String _myInfo = null;

	/**
	 * Flag will be true when this Branch will get timeOut notification 
	 * from parent
	 */
	protected boolean _parentTimedOut = false;
	
	/**
	 * Holds a reference to the listening point host
	 */
	protected String _host = null;

	/**
	 * Holds a reference to the listening point port
	 */
	protected int _port = -1;

	/**
	 * Holds a reference to the listening point transport
	 */
	protected String _transport;

	/**
	 * prefered outbound interface. an index into the list stored in class
	 * SipProxyInfo. The SIP container should be able to accept from the SIP
	 * Proxy a list of outbound interfaces and expose it to any SIP application
	 * (i.e. Siplets) via new API defined in JSR 289. SIP proxy will provide the
	 * SIP container a list of those interfaces. The SIP container, upon future
	 * outgoing requests will include a new header ,[PreferedOutbound] or PO in
	 * short, with an integer (an index to the SIP Proxy interface list). This
	 * is done to preserve memory footprint of the SIP sessions and requests.
	 * The SIP proxy will parse that integer and will send the message out via
	 * the relevant outbound interface.
	 * 
	 */
	protected int _preferedOutBoundIfaceIdxUDP = SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED; 
	protected int _preferedOutBoundIfaceIdxTCP = SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED; 
	protected int _preferedOutBoundIfaceIdxTLS = SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED; 

	//
	// Constants
	//
	private static final String TLS = "TLS";

	/**
	 * flag controlling whether the application stays on the signaling path 
	 * for this dialog or not. This should be set to true if the application
	 *  wishes to see subsequent requests belonging to the dialog.
	 */
	protected boolean _isRecordRoute = false;
	
	/**
	 * Ctor
	 * @param director
	 */
    protected BranchManager(SipServletRequestImpl originalReq){
    	_originalReq = originalReq;

    	//	The SLSP always sends default interfaces through UCF
		//	attributes that are sent to the container. This routine pulls
		//	all the listening point info from the original request.
    	getListeningPointParams();
	}
    
    /**
	 * Ctor
	 * @param director
	 */
    protected BranchManager(SipServletRequestImpl originalReq, 
    						StatefullProxy proxy){
    	_originalReq = originalReq;
		_isRecurse = proxy.getRecurse();

		//	The SLSP always sends default interfaces through UCF
		//	attributes that are sent to the container. This routine pulls
		//	all the listening point info from the original request.
    	getListeningPointParams();
	}
    
    /**
     * Helper method which return (and initialized if needed) the _myInfo 
     * parameter. Only when in debug mode this parameter will be initialized.
     * @return
     */
    protected String getMyInfo(){
    	if(_myInfo == null){
    		StringBuffer  buff = new StringBuffer();
    		buff.append("<");
    		buff.append(this);
    		buff.append(">");

    		_myInfo = buff.toString();
    	}
    	return _myInfo;
    }
	
	/**
	 * Initialize the non copied header lists
	 */
	static 
	{
		c_notCopiedRequestHeader.put(ToHeader.name, "");
		c_notCopiedRequestHeader.put(FromHeader.name, "");
		c_notCopiedRequestHeader.put(CallIdHeader.name, "");
		c_notCopiedRequestHeader.put(ContentLengthHeader.name, "");
		c_notCopiedRequestHeader.put(CSeqHeader.name, "");
		c_notCopiedRequestHeader.put(ContentTypeHeader.name, "");
		c_notCopiedRequestHeader.put(MaxForwardsHeader.name, "");
		
		c_notCopiedResponseHeader.put(CallIdHeader.name, "");
		c_notCopiedResponseHeader.put(ContentLengthHeader.name, "");
		c_notCopiedResponseHeader.put(CSeqHeader.name, "");		
		c_notCopiedResponseHeader.put(ViaHeader.name, "");
		c_notCopiedResponseHeader.put(ContentTypeHeader.name, "");
		c_notCopiedResponseHeader.put(RouteHeader.name, "");
	}
		
	/**
	 * Staring to actual send on the ProxyBranch
	 *
	 */
	public void send(){
	    
	    if(c_logger.isTraceDebugEnabled())
        {
	        c_logger.traceDebug(this, "startSending", getMyInfo() + "sending to all branches.");
        }
	    
	    if(getIsParallel()){
	    	sendToStartedURIsInParallelMode();
	    }
	    else{
	    	sendToNextURIInSequentialMode();	    	
	    }	   
	}
	
	/**
	 * Helper method which sends the request to all started
	 * ProxyBrances.
	 *
	 */
	private void sendToStartedURIsInParallelMode() {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "sendToStartedURIsInParallelMode");
		}
//		we can get here when Application call to createBranches 
    	// (not only in the first time) and the StartProxy(). Or when
    	// proxyTo(uris)

		boolean sent = false;
    	for(;_nextBranchToExecute  < _proxyBranches.size();_nextBranchToExecute ++){
    		ProxyBranchImpl branch = _proxyBranches.get(_nextBranchToExecute);
    		if(branch.isInitial()){
    			sent |= sendOnTheBranch(branch);
    		}
    	}
    	
    	if (!sent){
    		if(_bestResponse.getBestResponse() == null){
    			updateBestResponse(getRequestForInternalUse(),SipServletResponse.SC_DECLINE,null);
    		}
    		allBranchesCompleted();
    	}
    	if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "sendToStartedURIsInParallelMode");
		}
	}

	/**
	 * Helper method which send the request to the next URI in
	 * sequential mode.
	 *
	 */
	private void sendToNextURIInSequentialMode() {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "sendToNextURIInSequentialMode");
		}
//		sequential mode - send to next URI.
    	
    	if(_activeBranchCount > 0 ){
			// The last branch has not completed yet. Exit.
			if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "sendToNextURIInSequentialMode", 
                 getMyInfo() + "sequencial proxy mode - not sending till previous branch completes.");
            }
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceExit(this, "sendToNextURIInSequentialMode");
			}
    		return;
    	}
    	
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "sendToNextURIInSequentialMode", getMyInfo());
		}
    	
    	boolean sent = false;
    	
    	while(!sent && _nextBranchToExecute < _proxyBranches.size()){
    		// try send on branch until first branch will not throw the
    		// exception.
	    	ProxyBranchImpl branch = _proxyBranches.get(_nextBranchToExecute++);
	    	if(branch.isInitial()){
	    		sent = sendOnTheBranch(branch);
	    	}
	    	if(!sent){
	    		if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "sendToNextURIInSequentialMode",
							getMyInfo() + "branch wasn't sent");
				}
	    	}
    	}
    	
    	if(_nextBranchToExecute == _proxyBranches.size() && ! sent){
    		// If we didn't find a branch to perform successful send - 
    		// complete all branches.
    		if(_bestResponse.getBestResponse() == null){
    			updateBestResponse(getRequestForInternalUse(),
									SipServletResponse.SC_DECLINE,null);
    		}
    		allBranchesCompleted();
    	}
    	if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "sendToNextURIInSequentialMode");
		}
	}

	/**
	 * Each derived object will return its basic request. StatefullProxy will
	 * return original request and  ProxyBranch the request which is
	 * related to this ProxyBranch object.
	 * @return
	 */
	abstract SipServletRequest getRequestForInternalUse();

	//	Each derived object needs to return the appropriate outbound interface to use for the branch.
	abstract int getPreferedOutboundIface(String transport);
	

	/**
	 * @return the outbound interface for this branch if it's set 
	 * according to the given transport type. 
	 * Otherwise extract the outbound interface from the session object
	 */
	private int getOutboundIface(String transport) {
		int outboundIface;
		
        outboundIface = getPreferedOutboundIface(transport);
        if (outboundIface == SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
    		IncomingSipServletRequest origRequest = (IncomingSipServletRequest) getOriginalRequest();
            TransactionUserWrapper tu = origRequest.getTransactionUser();
        	if (tu != null) {
        		outboundIface = tu.getPreferedOutboundIface(transport);
        	}
        }
        
        return outboundIface;
	}

	/**
	 * Helper method which is actually sends the request.
	 * 
	 * @param branch
	 * 
	 * @return true if was sent;
	 */
	private boolean sendOnTheBranch(ProxyBranchImpl branch) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "sendOnTheBranch", getMyInfo()+ "branch" + branch);
		}
		try {
			
			_activeBranchCount++;
			branch.continueAndSend();
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceExit(this, "sendOnTheBranch", Boolean.TRUE);
			}
			return true;
		} 
		catch (IOException e) {
			if (c_logger.isTraceDebugEnabled()) {
				
				c_logger.traceDebug(this, "startSending", 
						getMyInfo() + "IOException on branch " + branch + " try next");
			}
			
			if (c_logger.isErrorEnabled()) {
				c_logger.error("error.create.request", null, branch.getRequestForInternalUse());
			}
			
			// If we have failed to send out message from this 
			// ProxyBranch - setup flag in ProxyBrnach - this will
			// prevent canceling.
			branch.failedToSend();
			updateBestResponse(branch.getRequestForInternalUse(),SipServletResponse.SC_DECLINE,branch);
			_activeBranchCount--;
		}
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "sendOnTheBranch", Boolean.FALSE);
		}
		return false;
	}

	/**
	 * More than one branches are associated with a proxy when proxyTo(List) or
	 * createProxyBranches(List) is used. This method returns the top level
	 * branches thus created. If recursion is enabled on proxy or on any of its
	 * branches then on receipt of a 3xx class response on that branch, the
	 * branch may recurse into sub-branches. This method returns just the top
	 * level branches started.
	 * 
	 * @return all the the top level branches associated with this proxy
	 */
    public List<ProxyBranch> getAllBranches() {
    	
    	if(_proxyBranches.isEmpty()){
    		if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getAllBranches", 
						getMyInfo() + "No ProxyBranches were created");
			}
    		return null;
    	}
    	List <ProxyBranch> allBranches = 
    		new Vector <ProxyBranch> (_proxyBranches.size());
    	
    	allBranches.addAll(_proxyBranches);
    	
    	return allBranches;
	}
    
    /**
     * Helper method which collects all newly create proxyBranches which
     * (not started)
     * @return
     */
    protected List<ProxyBranch> getAllNewlyCreatedProxyBranches() {
    	
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "getAllNewlyCreatedProxyBranches", 
										getMyInfo());
		}
    	List <ProxyBranch> allBranches = 
    		new Vector <ProxyBranch> (3);
    	
    	for (int i = 0; i < _proxyBranches.size(); i++) {
			ProxyBranchImpl element = _proxyBranches.get(i);
			if(!element.isInitial()){
				allBranches.add(element);
			}
		}
		
    	return allBranches;
	}

  	/**
     * Start the sending process (waiting URIs are stored in m_waitingURIs)
     */
    public synchronized void startSending() throws IllegalStateException
	{
//    	 We need to listen to new transaction (catch cancel, RecordRoute)
        // in case cancel will arrived
        TransactionUserWrapper tu = 
            ((SipServletMessageImpl)_originalReq).getTransactionUser();
        
        tu.setIsProxying(true);
        tu.setIsRRProxying(_isRecordRoute);
                
       
        boolean temp = false;
        for (ProxyBranchImpl pb : _proxyBranches) {
        	temp |= pb.getRecordRoute();
        }
	    if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "startSending", "Noam: " + temp);
        }
	    tu.setIsRRProxying(temp);
	    send();
	}
    
   
    /**
     * Helper method which investigates bestResponse - and decides if it
     * is final.
     * @param status
     * @return
     */
    protected boolean isFinalResponse(int status){
    	if (((status >= 200) && (status < 300))
                || ((status >= 600) && (status < 700))) {
    		return true;
        }
    	
    	return false;
    }
    
       
    /**
     * Generate the timeout response for the given request. Needed as we get
     * a timeout event from stack without a response object that can be passed
     * on for further processing. 
     * @param request
     * @return
     */
    protected IncomingSipServletResponse generateResponse(SipServletRequest req, int errorCode ) 
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "generateResponse", getMyInfo() + "Error - " + errorCode);
		}
        IncomingSipServletResponse response =  null;
        SipServletRequestImpl request = (SipServletRequestImpl)req;

        try {
        	response = SipUtil.createResponse(errorCode, request);
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
        
        return response;
    }
  
	/**
	 * Are all branches in this proxy are have been completed
	 * @return boolean
	 */
	protected boolean areAllBranchesCompleted()
	{
	    if (c_logger.isTraceDebugEnabled() && _activeBranchCount < 0) {
            c_logger.traceDebug(this, "areAllBranchesCompleted", 
            		getMyInfo() + "Error invalid active branch count: " + _activeBranchCount);
        }
	    return (_activeBranchCount == 0) && _started;
	}
	
	
	/**
	 * Method which will first check if this error Code is the best.
	 * If yes - Response will be created and updated.
	 * @param response
	 * @param errorCode
	 */
	protected void updateBestResponse(	SipServletRequest request,
										int errorCode,
										ProxyBranchImpl branch){
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "updateBestResponse", new Object[]{request, new Integer(errorCode), branch});
		}
		if( branch.getRecurse() && 
			errorCode >= 300 && errorCode < 400)
        {
            //RFC 3261 Section 16.7 step 4, 
            //If the proxy chooses to recurse on any contacts in a 3xx response 
            //by adding them to the target set, it MUST remove them from the 
            //response before adding the response to the response context...
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "updateBestResponse", 
                		getMyInfo() + "Not updating best response on a redirect response when proxy in recurse mode");
            }
        }
        else
        {
        	
    		SipServletResponseImpl response = null;
    		if( request.isInitial() && 
	    		((SipServletRequestImpl)request).isJSR289Application()) {
				response = generateResponse(request,errorCode);
	        	// Proxy Final response is not committed.
	        	response.setIsCommited(false);
	        	associateResponseWithSipSession(response, branch);
	        	//Don't send branch response to application on 2xx & 6xx response
	        	if (treat2xx6xxAsBestResponse(response.getStatus())) { 
	        		branch.notifyIntermediateBranchResponse(response);
	        	}
    	    }
        	if(_bestResponse.getNewPreference(errorCode) != -1){
        		if(response == null) response = generateResponse(request,errorCode);
        		_bestResponse.updateBestResponse(response,branch);
        	}
        }
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "updateBestResponse");
		}
	}
	
	 /**
     * Checks whether to send branch response to application.
     * If the custom property is set to true and this is a 2xx/6xx response,
     * the branch response won't be sent to the application and the 2xx/6xx
     * will be treated as the best response.
     * This is according to RFC 3261 section 16.7, steps 5&6.
     * 
     * @param status the response status code
     * 
     * @return <tt>true</tt> - if branch response should be sent to the application,
     *         <tt>false</tt> - otherwise.
     */
    protected boolean treat2xx6xxAsBestResponse(int status) {
    	//Use custom property since this is behavior change
		boolean prop = PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.WAS80_TREAT_2XX_6XX_AS_BEST_RESPONSE);
		
    	if ((prop) && ((status >= 200 && status < 300) || (status >= 600 && status < 700)) ) {
    		//Don't send branch response to application on 2xx & 6xx response
    		if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this, "treat2xx6xxAsBestResponse", "got " + status + " response, not sending branch response to application");
    		}
    		return false;
    	}
    	return true;
    }
	
	
    /**
     * A response with a status code of 2xx till 6xx arrived to this branch 
     * @param status
     * @param response
     */
    protected void updateBestResponse(SipServletResponse response,ProxyBranchImpl branch)
    {
        if (c_logger.isTraceDebugEnabled())
        {
            String last = null;
            SipServletResponse best = _bestResponse.getBestResponse();
            if (null != best)
            {
                last = best.toString();
            }

            if (null == last)
            {
                last = "null";
            }
            
            StringBuffer buffer = new StringBuffer("best: ");
            buffer.append(last);
            buffer.append(", current: ");
            buffer.append(response.getStatus());
             c_logger.traceDebug(this, getMyInfo() + "updateBestResponse", buffer.toString());
  
        }
        
        if(branch.getRecurse() && 
           response.getStatus() >= 300 && response.getStatus() < 400)
        {
            //RFC 3261 Section 16.7 step 4, 
            //If the proxy chooses to recurse on any contacts in a 3xx response 
            //by adding them to the target set, it MUST remove them from the 
            //response before adding the response to the response context...
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "updateBestResponse", 
                		getMyInfo() + "Not updating best response on a redirect response when proxy in recurse mode");
            }
        }
        else
        {
            _bestResponse.updateBestResponse(response,branch);
            
            if(SipSessionSeqLog.isEnabled())
            {
                int bestResponse = -1;
                SipServletResponse resp = _bestResponse.getBestResponse();
                if(null != resp)
                {
                    bestResponse = resp.getStatus();
                }
                ((SipServletRequestImpl)_originalReq).
                	getTransactionUser().logToContext(
                						SipSessionSeqLog.PROXY_UPDATE_BEST_RESP, 
                						bestResponse);
            }
        }
    }
    
    
    
	/**
	 * @see com.ibm.ws.sip.container.proxy.StatefullProxy#branchCompleted(
	 * 							javax.servlet.sip.SipServletResponse)
	 */
	protected void branchCompleted(ProxyBranchImpl branch, SipServletResponse response)
	{
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "branchCompleted", 
					getMyInfo() + "completed branch " + branch + " response"  + response);
		}
		// Update the best response
		updateBestResponse(response,branch);
		

		//only non-virtual brnaches are considered active
		if (!branch.isVirtual()){
//			Remove from the list of open branches 
			_activeBranchCount--;
		}
		
		//If we still have target URIs - keep creating outbound branches.  
		if(!_parentTimedOut  && _nextBranchToExecute  <= _proxyBranches.size() && !getIsParallel())
		{
			send();
		    return; 
		}
		
		// Should we forward the best response?
		if (areAllBranchesCompleted())
		{
			allBranchesCompleted();
		}
		
	}
	
	
    /**
	 * Proxy a request to specific URI, 
     * 	add a listener to the created transaction
	 *
	 * @param uri the target URI
	 * @param listener listener to the client transaction
	 * @return the created request
	 * @throws IllegalStateException
	 * @throws IOException
     */
    protected OutgoingSipServletRequest proxy(
        	OutgoingSipServletRequest request,
            URI uri,
            ClientTransactionListener listener)
            throws IllegalStateException, IOException
        {
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "proxy", getMyInfo() +  uri);
            }
            
            //Check if strict routing is applied. If so top route header is removed. 
            boolean strictRoutingApplied = checkForStrictRouting(request, uri);
            
            if(!strictRoutingApplied) {
            	//Check if we are changing from Strict to Loose routing mode
            	checkForLooseRouting(request, uri);
            }
            
            SipServletRequestImpl origRequest =
                (SipServletRequestImpl) _originalReq;
            // Should we record route
            SipURI rr_uri = null;
            if (_isRecordRoute) {
            	SipURL target = (SipURL)SipStackUtil.createTargetFromMessage(request.getRequest());
            	SipURI targetURI = new SipURIImpl(target);
                String targetTransport = SipUtil.getTransport(targetURI);

                SipProvider provider = request.getSipProvider();
        		ListeningPoint lp = provider.getListeningPoint();
        		int targetPort = lp.getPort();

        		rr_uri = getRecordRouteURI(targetTransport, targetPort);
        		// an application can set parameters into the Record-Route header field
        		// copy parameters /  headers from the original Record-Route header field
        		rr_uri = copyParams(rr_uri);
        		// update the Record-Route header field
        		_recordRouteURI = rr_uri;
        		setSchemeAccordingToDestination(request, rr_uri);
                //Set the unique identifier of the session within record route header
                //Used for identifying new requests on the same dialog
                rr_uri.setParameter(TransactionUserImpl.SESSION_RR_PARAM_KEY, 
                                    origRequest.getTransactionUser().getId());
                
                //Indicates loose routing - RFC 3261
                rr_uri.setParameter("lr", "");
                
                // used for double Record-Route when incoming and outgoing transport are different 
                // or when incoming and outgoing interfaces are different 
                SipURI inbound_rr_uri = null;
                
                // Here we check to see if the record-route matches the interface that
                // the message arrived on. If no we set a proprietary parameter
                // which is used to indicate that the record-route needs to be modified
                // when the response arrives.
                SipURI receivedOnInterfaceURI = (SipURI) SipProxyInfo.getInstance().extractReceivedOnInterface(_originalReq).clone();
                if (receivedOnInterfaceURI != null) {
                	int inboundIfaceIndex = SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED;
                    OutboundInterface inboundIface = getOutboundInterface(
                    		new InetSocketAddress(receivedOnInterfaceURI.getHost(), receivedOnInterfaceURI.getPort()));
            		if (inboundIface != null && SIPTransactionStack.instance().getConfiguration().getSentByHost() == null) {
            			inboundIfaceIndex = inboundIface.getOutboundInterface(receivedOnInterfaceURI.getTransportParam());
            		}
                	inbound_rr_uri = getRecordRouteURI(receivedOnInterfaceURI.getTransportParam(), 
                			receivedOnInterfaceURI.getPort(),
                			inboundIfaceIndex);

                	if (!SipUtil.isSameTransport(inbound_rr_uri, rr_uri) ||
                			!SipUtil.isSameHost(inbound_rr_uri, rr_uri)) {
                        if (c_logger.isTraceDebugEnabled()) {
                            c_logger.traceDebug(this, "proxy", "either outgoing transport is different than " +
                            		"incoming transport or outbound interface is different than incoming interface, " +
                            		"adding a second Record-Route header");
                        }
                    	
                    	// set an outgoing provider according to the target transport
                    	request.setProvider(target);
                    	
                		convertToCanonicalSipsURI(inbound_rr_uri);
						//Set the unique identifier of the session within record route header
		                //Used for identifying new requests on the same dialog
						inbound_rr_uri.setParameter(TransactionUserImpl.SESSION_RR_PARAM_KEY, 
		                                    origRequest.getTransactionUser().getId());
						//Indicates loose routing - RFC 3261
						inbound_rr_uri.setParameter("lr", "");
						//set ibmdrr parameter so we can later remove both route headers with
						//this parameter in subsequent requests 
						rr_uri.setParameter(SipProxyInfo.IBM_DOUBLE_RECORD_ROUTE_PARAMETER, "");
						inbound_rr_uri.setParameter(SipProxyInfo.IBM_DOUBLE_RECORD_ROUTE_PARAMETER, "");
                	}
                	else {
                		inbound_rr_uri = null;
                	}
                } 
                else {
                    if (c_logger.isTraceDebugEnabled()) {
                        c_logger.traceDebug(this, "proxy", "received on interface is null.");
                    }
                }
                
                // if in double Record Route scenario push the second Route as well
                if(inbound_rr_uri != null) {
                	request.pushRecordRoute(inbound_rr_uri);
                }
                request.pushRecordRoute(rr_uri);
            }

            if(shouldAddToPath()) {
			SipURI pathURI =  getPathAddress(request.getTransport());

                //Indicates loose routing - RFC 3327
    			pathURI.setParameter("lr", "");
    			SipServletsFactoryImpl f = SipServletsFactoryImpl.getInstance();
    			Address address = f.createAddress(pathURI);
            	request.pushPath(address);
            }
            
            //Associate the proxy with request might be used later (e.g. redirect)
            request.setProxy(getStatefullProxy());
            
            //	Here we set the prefered outbound interface in a multi-homed environment if needed.
        	if (getPreferedOutboundIface(request.getTransport()) >= 0)
        	{
                if (c_logger.isTraceDebugEnabled())
                {
                    c_logger.traceDebug(this, "proxy", "adding this preferred outboundIF from the proxy: " + getPreferedOutboundIface(request.getTransport()));
                }
        		//	An outbound interface is set on the proxy object.
        		SipProxyInfo.getInstance().addPreferedOutboundHeader(request,getPreferedOutboundIface(request.getTransport()));
        	}
        	else if (_originalReq.getTransactionUser() != null && 
        			_originalReq.getTransactionUser().getPreferedOutboundIface(request.getTransport()) >= 0)
        	{
                if (c_logger.isTraceDebugEnabled())
                {
                    c_logger.traceDebug(this, "proxy", "adding this preferred outboundIF from the TU: " + 
                    		origRequest.getTransactionUser().getPreferedOutboundIface(request.getTransport()));
                }
        		//	An outbound interface is set on the session object or this will be based
        		//	off of the interface the original request arrived on.
        		SipProxyInfo.getInstance().addPreferedOutboundHeader(request,origRequest.getTransactionUser().getPreferedOutboundIface(request.getTransport()));
        	}

            // if needed, apply RFC 5626 section 5 processing to the forwarded request.
            OutboundProcessor outboundProcessor = OutboundProcessor.instance();
            outboundProcessor.forwardingRequest(origRequest, request, rr_uri);
    		
            // Send the request, use the private API for adding the listener
            request.send(listener);

            return request;
        }
    
    /**
     * Copy parameters / headers from the original Record-Route header field 
     * into the given SIP URI
     */
    private SipURI copyParams(SipURI rrHeader) {
    	if (_recordRouteURI == null) {
    		return rrHeader;
    	}
    	SipURI returnUri = (SipURI) _recordRouteURI.clone();
    	returnUri.setHost(rrHeader.getHost());
    	returnUri.setPort(rrHeader.getPort());
    	returnUri.setTransportParam(rrHeader.getTransportParam());
		returnUri.setSecure(rrHeader.isSecure());
		
		return returnUri;
    }
    
    /**
     * Helper method which is responsible to decide if Paht header
     * should be added to the outging message.
     * @return
     */
    private boolean shouldAddToPath() {
    	// If application wan't to add PATH header - Proxy should test 
    	// "Supported" header to look "path" parameter inside.
    	
    	if(getAddToPathValue() && _originalReq.getMethod().equals(Request.REGISTER)){
    		
    		String h_supported = _originalReq.getHeader(SupportedHeader.name);
    	
    		if(h_supported != null){
    			if(h_supported.indexOf(SipUtil.PATH_PARAM)!= -1) {
    				return true;
    			}
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "shouldAddToPath", 
							getMyInfo() + "Origianl Request doesn't support path option");
				}
    		}
    	}
    	
    	return false;
	}
    
    /**
	 *  @see com.ibm.ws.sip.jain289API.ProxyBranch#getAddToPath()
	 */
	abstract boolean getAddToPathValue();
	
    /**
     * returns a reference to the Proxy object
     * @return
     */
    abstract Proxy getStatefullProxy();
    
	/**
	 * All branches were completed - now each derived class should
	 * decide what to do.	 *
	 */
	abstract void allBranchesCompleted();
	 
    /**
	 * Helper method which will be override by child objects and 
	 * returns if this BranchManager is in recordRoute mode.
	 * @return
	 */
//	abstract boolean getIsRecordRoute();
	
	/**
	 * @return Record-Route URI
	 * @return
	 */
//	abstract SipURI getRRUri();
	/**
	 * @return Path header address
	 */
	abstract SipURI getPathAddress(String transport);
    
	/**
	 * Returns if the Proxy is in Parallel mode or not
	 * @return
	 */
	abstract boolean getIsParallel();
    
    /**
     * Check if the bottom route header matches the request URI. 
     * In that case we should be in case of pre-loaded route headers
     * with a strict element in front of us and a loose element in front of us. 
     * Remove the bottom route header as it is no longer need 
     * (should be our own address) and is likely to get us 
     * into a loop or spiral scenario.   
     * @param request
     * @param uri
     */
    private final void checkForLooseRouting(OutgoingSipServletRequest outReq, URI uri) {
        if(!uri.isSipURI())
        {
            //non sip URI do not continue checking
            return;
        }
        
        Request jainReq = outReq.getRequest(); 
        NameAddressHeader bottomRoute;
        try {
            bottomRoute = (NameAddressHeader) jainReq.getHeader(RouteHeader.name, false);
            if(bottomRoute != null && bottomRoute.getNameAddress().getAddress() instanceof SipURL)
            {
            	SipURL routeURL = (SipURL) bottomRoute.getNameAddress().getAddress();
                SipURI reqURI = (SipURI) uri;
                
                String routeHost = routeURL.getHost();
                int routePort = routeURL.getPort();
                boolean routeSecure = routeURL.getScheme().equalsIgnoreCase("sips");
                //if the route header was added as part of the route back jsr289 mechanism 
                //we do not need to remove it.
                if(!routeURL.hasParameter(SipUtil.IBM_ROUTE_BACK_PARAM) &&
                    routePort == reqURI.getPort()&&
                    routeHost.equals(reqURI.getHost()) &&
                   (outReq.checkIsLocalListeningPoint(routeHost, routePort, routeSecure)))
                {
                    if (c_logger.isTraceDebugEnabled()) {
                        c_logger.traceDebug(this, "checkForLooseRouting", 
                        getMyInfo() + "Removing bottom route as it matches the request uri: " + bottomRoute);
                    }
                    
                    //We have bottom route that matches the request uri - remove it
                    jainReq.removeHeader(RouteHeader.name, false);
                }
            }
        }
        catch (HeaderParseException e) {
            logException(e);
        }
        catch (IllegalArgumentException e) {
            logException(e);
        }

    }
    
    /**
     * Helper method which sends timeOut notification to child branches
     *
     */
    protected void timeoutAllChildBranches(boolean isTimeout) {

		for (int i = 0; i < _proxyBranches.size(); i++) {

			ProxyBranchImpl branch = (ProxyBranchImpl) _proxyBranches.get(i);

			if (branch.isActive()) {
				branch.proxyTimedOut(isTimeout);
				if(!getIsParallel()){
					break;
				}
			}
		}
	}
    
    /**
     * Check if the top route header is a strict route header and it matches the
     * request's URI. In that case we should be in case of pre-loaded route headers
     * with a strict element in front of us. Remove the top route header as it
     * is no longer need (should be our own address). Note that due to a limitation
     * in the JSR 116 that will only work for a single pre-loaded route header.  
     * @param request
     * @param uri
     * @return true if strict routing policy has been applied and pre-loaded
     * route headers has been removed from the request
     */
    private final boolean checkForStrictRouting(OutgoingSipServletRequest outReq, URI uri) {
        boolean rc = false;
    	if(!uri.isSipURI())
        {
            //non sip URI do not continue checking
            return rc;
        }
        
        Request jainReq = outReq.getRequest(); 
        NameAddressHeader topRoute;
        try {
            topRoute = (NameAddressHeader) jainReq.getHeader(RouteHeader.name, true);
            if(topRoute != null && topRoute.getNameAddress().getAddress() instanceof SipURL)
            {
            	SipURL routeURL = (SipURL) topRoute.getNameAddress().getAddress();
                SipURI reqURI = (SipURI) uri;
                
                if(!routeURL.hasParameter("lr") && 
                   routeURL.getPort()== reqURI.getPort()&&
                   routeURL.getHost().equals(reqURI.getHost()))
                {
                    
                    if (c_logger.isTraceDebugEnabled()) {
                        c_logger.traceDebug(this, "checkForStrictRouting", 
                        getMyInfo() + "Removing top route as it matches the request uri: " + topRoute);
                    }
                    
                    //We have top non-lr route that matches the request uri - remove it
                    jainReq.removeHeader(RouteHeader.name, true);
                    
                    //Mark the next header if available for strict routing so the stack/slsp will 
                    //send the request according to the request uri and not the top route
                    topRoute = (NameAddressHeader) jainReq.getHeader(RouteHeader.name, true);
                    if(topRoute != null && topRoute.getNameAddress().getAddress() instanceof SipURL) {
                    	routeURL = (SipURL) topRoute.getNameAddress().getAddress();
                    	routeURL.setParameter(SipStackUtil.STRICT_ROUTING_PARAM, "");
                    }
                    rc = true;
                }
            }
        }
        catch (HeaderParseException e) {
            logException(e);
        }
        catch (IllegalArgumentException e) {
            logException(e);
        } catch (SipParseException e) {
        	logException(e);
		}
        
        return rc; 
    }
    
    /** 
	 * Create a new virtual branch and add it to our list
	 *
	 * @param proxy - the proxy handling
	 * @return The newly created proxy branch or null if failed to create the
	 * branch
	 */
    protected ProxyBranchImpl createVirtualBranch(StatefullProxy proxy,OutgoingSipServletResponse response){
    	if (c_logger.isTraceEntryExitEnabled()) {
            Object[] params = { response.getTransactionUser().getCallId()};
            c_logger.traceEntry(this, "createVirtualBranch", params);
        }
    	ProxyBranchImpl branch = createBranch(null, false, proxy, true);
    	return branch;
    }

    /** 
	 * Create a new non-virtual branch and add it to our list
	 *
	 * @param nextHop - the target URI
	 * @return The newly created proxy branch or null if failed to create the
	 * branch
	 */
	public ProxyBranchImpl createBranch(javax.servlet.sip.URI nextHop,
			boolean createdViaProxyTo,
			StatefullProxy proxy){
		return createBranch(nextHop, createdViaProxyTo, proxy, false);
	}

	/** 
	 * Create a new branch and add it to our list
	 *
	 * @param nextHop - the target URI
	 * @return The newly created proxy branch or null if failed to create the
	 * branch
	 */
	private ProxyBranchImpl createBranch(javax.servlet.sip.URI nextHop,
										boolean createdViaProxyTo,
										StatefullProxy proxy,
										boolean isVirtual)
	{
	    if (c_logger.isTraceEntryExitEnabled()) {
            Object[] params = { nextHop };
            c_logger.traceEntry(this, "createBranch", params);
        }

	    if(!isVirtual){
		    if(!("sip".equals(nextHop.getScheme()) || "sips".equals(nextHop.getScheme()))){
				throw new IllegalArgumentException ("Unsupported Scheme");
			}
	    }
	    ProxyBranchImpl curBranch =  ThreadLocalStorage.getCurrentBranch();
	    if((curBranch != null) && curBranch.isCancelled()){
	    	throw new IllegalStateException("Cannot create Branches on a canceled proxy branch during doBranchResponse");
	    }

		
	    //if this is a virtual branch we need special treatment
    	if (isVirtual) {
    		
    		if (proxy.isVirtualBranchExists()) {
    	        //if the virtual branch exists already, we should throw IllegalStateException
    			throw new IllegalStateException("Virtual Branch Already exists"); 			
        	}
    	}else {
    		//check that no branch was created for this destination
    		//this check is relevant only for regular branches
    		if (proxyBranchExists(nextHop)) {
    			throw new IllegalStateException("Duplicate Branch - Attempt to proxy message to the same URI again: " + nextHop);
    		}
    	}

	    // Create a new branch and send the request
	    ProxyBranchImpl branch = 
	    	new ProxyBranchImpl(nextHop, this, proxy, createdViaProxyTo,isVirtual);
	    
	    //if this is a virtual branch we need special treatment
    	if (isVirtual){
    		proxy.setVirtualBranch(branch);
    	}else{
    		//add to proxy branch list
    		_proxyBranches.add(branch);
    	}

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "createBranch",
					getMyInfo() + "Branch created successfully: " + nextHop);
		}
				
		return branch;
	}

	/**
	 * Helper method that gets the listening points from the
	 * failover or from provider.
	 * 
	 */
	private void getListeningPointParams() {

		if (_host != null) {
			// meaning that this method was already called ones and all
			// information is already exists.
			return;
		}

		SipProvider provider = _originalReq.getSipProvider();
		ListeningPoint lp = provider.getListeningPoint();

		_host = lp.getSentBy();
		_port = lp.getPort();

		// Take transport from listening point to make sure they match
		_transport = ((ListeningPointImpl) lp).isSecure() ? TLS : lp.getTransport();
	}

	/**
	 * @see javax.servlet.sip.Proxy#getRecordRouteURI()
	 */
	public SipURI getRecordRouteURI() 
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "getRecordRouteURI");
		}
		
		
		if(_isRecordRoute == false){
			throw new IllegalStateException("Record-routing is not enabled");
		}
		
		if ( null == _recordRouteURI ) {
			_recordRouteURI = getRecordRouteURI(_transport, _port);
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "getRecordRouteURI");
		}
		return _recordRouteURI;
	}
	
	public SipURI getRecordRouteURI(String transport, int port) 
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = {transport, port};
			c_logger.traceEntry(this, "getRecordRouteURI", params);
		}
		
		
		if(_isRecordRoute == false){
			throw new IllegalStateException("Record-routing is not enabled");
		}
		
		SipURI recordRouteURI = null;
		
		try {
			int outboundIface = getOutboundIface(transport);
        	if (outboundIface != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
        		recordRouteURI = (SipURI)SipProxyInfo.getInstance()
        				.getOutboundInterface(outboundIface, transport).clone();
        	}
        	else {
				SipURL url = AddressFactoryImpl.createSipURL(null,null,null,_host,port,
				    									     null,null,null,transport);
				recordRouteURI = new SipURIImpl(url);
        	}
        }
        catch (IllegalArgumentException e) {
        	Object[] args = { e };
        	if(c_logger.isErrorEnabled()) {
        	    c_logger.error("error.create.record.route.uri", 
								Situation.SITUATION_REQUEST, args, e);
		    }
        }
        catch (SipParseException e) {
			Object[] args = { e };
			c_logger.error("error.create.record.route.uri", 
								Situation.SITUATION_REQUEST, args, e);
        }
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "getRecordRouteURI", recordRouteURI);
		}
		return recordRouteURI;
	}
	
	public SipURI getRecordRouteURI(String transport, int port, int outboundIface) {
		if (c_logger.isTraceEntryExitEnabled()) {
			Object[] params = {transport, port, outboundIface};
			c_logger.traceEntry(this, "getRecordRouteURI", params);
		}
		
		
		if(_isRecordRoute == false){
			throw new IllegalStateException("Record-routing is not enabled");
		}
		
		SipURI recordRouteURI = null;
		
		try {
        	if (outboundIface != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
        		recordRouteURI = (SipURI)SipProxyInfo.getInstance()
        				.getOutboundInterface(outboundIface, transport).clone();
        	}
        	else {
				SipURL url = AddressFactoryImpl.createSipURL(null,null,null,_host,port,
				    									     null,null,null,transport);
				recordRouteURI = new SipURIImpl(url);
        	}
        }
        catch (IllegalArgumentException e) {
        	Object[] args = { e };
        	if(c_logger.isErrorEnabled()) {
        	    c_logger.error("error.create.record.route.uri", 
								Situation.SITUATION_REQUEST, args, e);
		    }
        }
        catch (SipParseException e) {
			Object[] args = { e };
			c_logger.error("error.create.record.route.uri", 
								Situation.SITUATION_REQUEST, args, e);
        }
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "getRecordRouteURI", recordRouteURI);
		}
		return recordRouteURI;
	}

	/**
	 * @see javax.servlet.sip.Proxy#getOriginalRequest()
	 */
	public SipServletRequest getOriginalRequest()
	{
		return _originalReq;
	}

	/**
	 * checks if a certain proxy branch already exists for 
	 * a certain URI
	 * 
	 * @param nextHop - the uri to be checked against
	 * @return true if the branch already exists, otherwise false
	 */
	protected abstract boolean proxyBranchExists(URI nextHop);

	/**
     * Helper method that creates derived session if deeded.
     * @param origRequest The original request that comes from the UAC
     * @param response Response that was received on the forked request
     */
    protected void associateResponseWithSipSession( SipServletResponseImpl response,
												  ProxyBranchImpl relatedBranch) {
    	if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this,
					"associateResponseWithSipSession", new Object[]{response, relatedBranch });
		}
    	SipServletRequestImpl branchRequest = ((SipServletRequestImpl)relatedBranch.getRequestForInternalUse());
    	
    	if( branchRequest == null){
    		//could only be in case of a virtual branch. Original request will be used.
    		branchRequest = ((SipServletRequestImpl)relatedBranch.getOriginalRequest());
    		if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this,
    					"associateResponseWithSipSession",
    					"Using original (incoming) request related transaction user. Branch is virtual = "  + relatedBranch.isVirtual());
    		}
    	}
    	
    	TransactionUserWrapper origTU = branchRequest.getTransactionUser();
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this,
					"associateResponseWithSipSession",
					"Branch request related transaction user=" + origTU);
		}
    	
		TransactionUserWrapper tu = ((SipServletResponseImpl)response).getTransactionUser();
		String toTag = ((SipServletResponseImpl) response).getResponse().getToHeader().getTag();
		
		if (tu == null) {
			if (origTU.getRemoteTag_2() != null) {
				tu = SipTransactionUserTable.getInstance().getTransactionUserInboundResponse(response.getResponse());
				if(tu == null){
					tu = origTU.createDerivedTU(
									((SipServletResponseImpl) response).getResponse(),
									" StatefullProxy - response with different tag received");
					tu.setIsRRProxying(relatedBranch.getIsRecordRoute());
					tu.addToTransactionUsersTable();
					relatedBranch.relateTU(tu);
					if(!relatedBranch._amIRecurseBranch && relatedBranch._mainAssociatedTu == null){
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(this,
									"associateResponseWithSipSession",
									"First Derived Session was associate with this ProxyBranch =" + origTU);
						}
						origTU.removeTransaction(response.getMethod());
						relatedBranch._mainAssociatedTu = tu;
						relatedBranch.setRemoveFromOriginalTU();
					}
				}
			} else {
				// There were no response with tag received before
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this,
							"associateResponseWithSipSession",
							"First response with remote tag received for this dialog");
				}
				tu = origTU;
				tu.setRemoteTag_2(toTag);
				tu.setIsRRProxying(relatedBranch.getIsRecordRoute());
				relatedBranch.relateTU(tu);//this will unrelate another branch that was related to this tu
											//in case there was one.
				relatedBranch._mainAssociatedTu = tu;
			}
				
			response.setTransactionUser(tu);
			branchRequest.setTransactionUser(tu);
			// setting the request with the correct session
			//((SipServletRequestImpl) response.getRequest()).setTransactionUser(tu);

			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this,
						"associateResponseWithSipSession",
						"associated tag = " + toTag + " tu = " + tu);
			}
		} else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this,
						"associateResponseWithSipSession", getMyInfo()
								+ "response was already associated with TU " + tu);
			}
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this,
					"associateResponseWithSipSession");
		}
	}
    
	/**
	 * @param outgoingResponse
	 * @param response
	 */
	private static void copyResponseHeaders(
		OutgoingSipServletResponse outgoingResponse,
		SipServletResponse response,
		IncomingSipServletRequest origRequest)
	{
        Response jainResponseIn = ((SipServletResponseImpl)response).getResponse();
        Response jainResponseOut = outgoingResponse.getResponse();
        
	    //Remove the default contact, RR, To and From headers as they will be add when
	    //copying headers
        jainResponseOut.removeHeaders(RecordRouteHeader.name); 
        jainResponseOut.removeHeaders(ContactHeader.name);
        jainResponseOut.removeHeaders(FromHeader.name);
        jainResponseOut.removeHeaders(ToHeader.name);
        
        // Copy necessary  headers 
        HeaderIterator hIterator = jainResponseIn.getHeaders();
        if (hIterator != null) 
        {
            Header h;
            while (hIterator.hasNext()) 
            {
                try 
                {
                    h = hIterator.next();

                    // Don't copy the following headers: Via, Record-Route, CSeq,
                    // To, From, CallId
                    if (!c_notCopiedResponseHeader.containsKey(h.getName())) 
                    {
                        jainResponseOut.addHeader(h, false);
                    }
                }
                catch (HeaderParseException e) 
                {
                    logException(e);
                    break;
                }
                catch (NoSuchElementException e) 
                {
                    logException(e);
                }
            }

        }
    }
	
	 /**
     * Create an outgoing sip response based on the original request and the
     * response.
     * 
     * @param origRequest
     *            the original request
     * @param response
     *            the response that should be copied to the new response
     * @return an outgoing sip servlet response
     */
    protected static OutgoingSipServletResponse createOutgoingResponse(
        IncomingSipServletRequest origRequest,
        SipServletResponse response)
    {
        // get the to tag so we can create a proper response
        AddressImpl to = (AddressImpl) response.getTo();
        String toTag = to.getTag();
        TransactionUserWrapper transactionUser = ((IncomingSipServletResponse)response).getTransactionUser();
        // create the response
        OutgoingSipServletResponse outgoingResponse =
			origRequest.createResponse(
				response.getStatus(),
				response.getReasonPhrase(),
				toTag,
				transactionUser);
        
        outgoingResponse.markAsProxyResponse();
        
        // Copy content
		copyContent(outgoingResponse, response);

		// copy headers to the outgoing response
		copyResponseHeaders(outgoingResponse, response, origRequest);
				
		
//		TransactionUserWrapper origTU = origRequest.getTransactionUser();
//		if (origTU != null && origTU != transactionUser) {
//			outgoingResponse.setTransactionUser(transactionUser);
//			if (c_logger.isTraceDebugEnabled()) {
//				c_logger.traceDebug(BranchManager.class, "createOutgoingResponse",
//				"response TU was changed to: " + transactionUser);
//			}
//		}
		
		if (origRequest.getTransaction().isTerminated()){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(BranchManager.class, "createOutgoingResponse",
				"This response will be sent directly over the SipStack.");
			}
			outgoingResponse.setShouldBeSentWithoutST(true);
		}
		
		return outgoingResponse;
	}
    

	 /**
    * Helper function. Copies all headers from the original request to the 
    * new request except for the following headers: to, from callid , cseq, 
    * content length
    * @param origRequest
    * @param request
    */
	private static void copyRequestHeaders(
       SipServletRequest origRequest,
       OutgoingSipServletRequest request)
   {
       // Copy all headers except the to and from 
       HeaderIterator iterator =
           ((SipServletMessageImpl) origRequest).getJainHeaders();
       if (iterator != null)
       {
           while (iterator.hasNext())
           {
               try
               {
					Header header = iterator.next();
					if (!c_notCopiedRequestHeader.containsKey(header.getName()))
                   {
                       request.addHeader(header, false);
                   }

               }
               catch (HeaderParseException e)
               {
                   if(c_logger.isErrorEnabled())
                   {
                       Object[] args = { request };
                   
	                    c_logger.error(
	                        "error.create.request",
	                        Situation.SITUATION_REQUEST,
	                        args,
	                        e);
                   }
                   break;
               }
               catch (NoSuchElementException e)
               {
                   
                   if(c_logger.isErrorEnabled())
                   {
                       Object[] args = { request };
                   
                       c_logger.error(
	                        "error.create.request",
	                        Situation.SITUATION_REQUEST,
	                        args,
	                        e);
                   }
               }

           }
       }
   }   
  
	   /**
	 * Copy the content of source message to destination message
	 *
	 * @param outgoingResponse the destination response
	 * @param response the source response
	 */
	private static void copyContent(
		SipServletMessage destination,
		SipServletMessage source)
	{
	    jain.protocol.ip.sip.message.Message jainDest = 
	        	((SipServletMessageImpl)destination).getMessage();
	    
	    jain.protocol.ip.sip.message.Message jainSrc = 
	        	((SipServletMessageImpl)source).getMessage();
	    try
		{
			byte[] body = jainSrc.getBodyAsBytes();
			ContentTypeHeader contentType = jainSrc.getContentTypeHeader();
	        if(null != body && null != contentType)
	        {
	            jainDest.setBody(body, contentType);
	        }
	        else
	        {
	            if (c_logger.isTraceDebugEnabled()) {
                    c_logger.traceDebug(BranchManager.class, 
                                        "copyContent", 
                                        "Unable to copy content: " + 
                                        contentType + " " + Arrays.toString(body));
                }
	        }
		}
		catch (HeaderParseException e) 
		{
		    if(c_logger.isErrorEnabled())
	        {
				Object[] args = { source };
				c_logger.error(
					"error.forward.response",
					Situation.SITUATION_REQUEST,
					args,
					e);
	        }
        }
        catch (IllegalArgumentException e) 
        {
            if(c_logger.isErrorEnabled())
	        {
				Object[] args = { source };
				c_logger.error(
					"error.forward.response",
					Situation.SITUATION_REQUEST,
					args,
					e);
	        }
        }
        catch (SipParseException e) 
        {
            if(c_logger.isErrorEnabled())
	        {
				Object[] args = { source };
				c_logger.error(
					"error.forward.response",
					Situation.SITUATION_REQUEST,
					args,
					e);
	        }
        }
	}
	
	
	/**
     * Create an out going sip servlet request copied from the originalRequest
     * 
     * @param origRequest
     *            the original request
     * @return An outgoing request, based on the original request
     */
	public static OutgoingSipServletRequest createOutgoingRequest(SipServletRequestImpl origRequest)
    {
        // Create the request, same callid, to and from tags
        OutgoingSipServletRequest request =
            new OutgoingSipServletRequest(
                origRequest.getMethod(),
                origRequest.getFrom(),
                origRequest.getTo(),
                origRequest.getCallId(),
                origRequest.getSipProvider(),
                origRequest, true);

        // Copy content and sets the Content-Type header
		copyContent(request, origRequest);
		
        // Copy request URI
        request.setRequestURI(origRequest.getRequestURI());

        // Copy all headers
		copyRequestHeaders(origRequest, request);
		
		// Copy the state of composition
		request.setStateInfo(origRequest.getStateInfo());
		request.setDirective(SipApplicationRoutingDirective.CONTINUE);
		
		//Set the max forwards header, it is available by default on the 
		//new request and therefore it is not copied to avoid duplicate headers
		int maxForwards = origRequest.getMaxForwards();
		if (maxForwards < 0) {
			// rfc 3261 16.6 section 3
			maxForwards = 70;
		}
		else if (maxForwards > 255) {
			maxForwards = 255;
		}
		request.setMaxForwards(maxForwards);
		
		request.setIbmClientAddress(origRequest.getIbmClientAddress());

        // decrease the max forwards value
        if (decreaseMaxForwards(request) == false)
        {
            if(c_logger.isTraceDebugEnabled() )
            {
	            c_logger.traceDebug(
	                BranchManager.class,
	                "createOutgoingRequest",
	                "Message will not be created");
            }
            return null;
        }
        
        return request;
    }
		
	/**
     * Helper function. Set the max forwards header (create it if needed)
     * according to the siplet API proxying rules 
     * @param request the request to be set
     */
    private static boolean decreaseMaxForwards(OutgoingSipServletRequest request)
    {
        boolean rc = true;
        Request jainRequest = request.getRequest();
        
        // Get the max forwards header from the request
        MaxForwardsHeader maxH;
        try {
            maxH = jainRequest.getMaxForwardsHeader();
            
            // ...If the copy does not contain a Max-Forwards header field, the
            // proxy MUST add one with a field value, which SHOULD be 70.
            if (maxH == null)
            {
                maxH = getHeadersFactory().createMaxForwardsHeader(70);
                jainRequest.setHeader(maxH, true);
            }
            else if (maxH.getMaxForwards() > 0)
            {
                maxH.decrementMaxForwards();
            }
            else
            {
                // We should never get here!!!!!!!
                // Too many hops, response and return
                if(c_logger.isTraceDebugEnabled())
                {
    	            c_logger.traceDebug(
    	                BranchManager.class,
    	                "createOutgoingRequest",
    	                "Max forwards is 0!");
                }

                rc = false;
            }

        }
        catch (HeaderParseException e) 
        {
            logException(e);
        }
        catch (SipParseException e) 
        {
            logException(e);
        }
        catch (SipException e) 
        {
            logException(e);
        }
        
        return rc;
    }
    
    /**
     * Returns the Jain Sip Header Factory. 
     */
    private final static HeaderFactory getHeadersFactory()
    {
        return StackProperties.getInstance().getHeadersFactory();
    }
    
    /**
     * cancel an outgoing request
     * 
     * @param request the request to be canceled
     * @param reasons 
     * @param listener a listener for responses
     */
    public static void cancelRequest(
        SipServletRequest request,
        ProxyBranchImpl proxyBranch, 
        List<ReasonHeaderImpl> reasons)
    {
        if (c_logger.isTraceDebugEnabled())
        {
            c_logger.traceDebug(proxyBranch, "cancelRequest", 
								"Request is " + request.getMethod());
        }

        // send a cancel to the request
        OutgoingSipServletRequest cancel =
            (OutgoingSipServletRequest) request.createCancel();
        if(reasons != null){
        	for(ReasonHeaderImpl reason : reasons){
        		cancel.addHeader(reason, true);
        	}
        }
		
//		// Copy the state of composition from the original request
        //TODO need to change this. CANCEL needs to be treated as a subsequent request. 
        cancel.setStateInfo(((SipServletRequestImpl)request).getStateInfo());
        cancel.setDirective(SipApplicationRoutingDirective.CONTINUE);
//        cancel.setIsSubsequentRequest(true);
        try
        { 
        	
            cancel.send(proxyBranch);
        }
        catch (IOException e)
        {
            if(c_logger.isErrorEnabled())
            {
                Object[] args = { cancel };
	            c_logger.error(
	                "error.proxy.cancel.request",
	                Situation.SITUATION_REQUEST,
	                args,
	                e);
            }
        } catch (IllegalArgumentException e) {
			if(c_logger.isErrorEnabled())
            {
	            Object[] args = { cancel };
	            c_logger.error(
	                "error.proxy.cancel.request",
	                Situation.SITUATION_REQUEST,
	                args,
	                e);
            }
		} 

    }
    
    /**
     *  Helper function to determine what is the destination scheme, we need this function to know what scheme of Record-Route to 
     *  choose. This is done according to RFC 3261 16.6.4
     *  @param req
     */
	private static String getSchemeFromDestination(SipServletRequestImpl req) {
		//default result is sip
		Request request = req.getRequest();
		String topRouteScheme = null;
		String requestUriScheme = req.getRequestURI().getScheme();
		if("sips".equalsIgnoreCase(requestUriScheme)) {
			return "sips";
		}
		else {
	 		try {
				Header topRoute = request.getHeader(RouteHeader.name, true);
				if(topRoute != null) {
					NameAddressHeader route = (NameAddressHeader) topRoute;
					jain.protocol.ip.sip.address.URI topRouteURI = route.getNameAddress().getAddress();
					if(topRouteURI instanceof SipURL) {
						SipURL topRouteSipURL = (SipURL) topRouteURI;
						topRouteScheme = topRouteSipURL.getScheme();
					}
				}
				if("sips".equalsIgnoreCase(topRouteScheme)) {
					return "sips";
				}
			} catch (HeaderParseException e1) {
				logException(e1);
			} catch (IllegalArgumentException e2) {
				logException(e2);
			}
		}
		return "sip";
	}
	
	/**
	 * Helper function to set SipURI to secure scheme if the destination is implemented according to "sips" scheme 
	 */
	private static void setSchemeAccordingToDestination(SipServletRequestImpl req, SipURI uri) {
		if("sips".equalsIgnoreCase(getSchemeFromDestination(req))) {
			//we need to remove "transport=tls" parameter because it's incompatible with "sips" scheme
			if("tls".equalsIgnoreCase(uri.getTransportParam())) {
				uri.removeParameter("transport");
			}
			uri.setSecure(true);
		}
	}
	
	private static void convertToCanonicalSipsURI(SipURI uri) {
		String transportParam = uri.getParameter("transport");
		if ("tls".equalsIgnoreCase(transportParam)) {
			uri.removeParameter("transport");
			uri.setSecure(true);
		}
	}
	/**
	 * Utility function for logging exceptions. 
     * @param e
     */
    public static void logException(Exception e) 
    {
        if(c_logger.isErrorEnabled())
        {
			c_logger.error("error.exception", Situation.SITUATION_REQUEST,
			               	null, e);
        }
    }

	public OutboundInterface getOutboundInterface(InetSocketAddress address) {
		OutboundInterface outboundIf = null;
		
		if (address == null) {
			throw new IllegalArgumentException("Invalid address = null");
		}
			
		boolean isSet = false;
		int preferedOutBoundIfaceIdxUDP, preferedOutBoundIfaceIdxTCP, preferedOutBoundIfaceIdxTLS;
		
		//note that the same address can be matched to more then one transport....
		preferedOutBoundIfaceIdxUDP = SipProxyInfo.getInstance().getIndexOfIface(address, "udp");
		if (preferedOutBoundIfaceIdxUDP != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
			isSet = true;
		}
		
		preferedOutBoundIfaceIdxTCP = SipProxyInfo.getInstance().getIndexOfIface(address, "tcp");
		if (preferedOutBoundIfaceIdxTCP != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
			isSet = true;
		}
		
		preferedOutBoundIfaceIdxTLS = SipProxyInfo.getInstance().getIndexOfIface(address, "tls");
		if (preferedOutBoundIfaceIdxTLS != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
			isSet = true;
		}
		
		if (isSet) {
			outboundIf = new OutboundInterface(preferedOutBoundIfaceIdxUDP, 
					preferedOutBoundIfaceIdxTCP, preferedOutBoundIfaceIdxTLS);
		}
		
		return outboundIf;
	}

}
