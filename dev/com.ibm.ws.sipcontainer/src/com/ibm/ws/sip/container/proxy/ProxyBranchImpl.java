/*******************************************************************************
 * Copyright (c) 2003, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.sip.container.proxy;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.header.ContactHeader;
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.message.Message;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;

import javax.servlet.sip.Proxy;
import javax.servlet.sip.ProxyBranch;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.jain.protocol.ip.sip.extensions.ReasonHeaderImpl;
import com.ibm.ws.sip.container.internal.SipContainerComponent;
import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.servlets.OutgoingSipServletRequest;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipServletResponseImpl;
import com.ibm.ws.sip.container.servlets.SipServletsFactoryImpl;
import com.ibm.ws.sip.container.transaction.ClientTransactionListener;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.util.SipUtil;
import com.ibm.ws.sip.parser.SipConstants;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;

/**
 * @author anatf
 * 
 * Represent a branch on the Proxy
 */
public class ProxyBranchImpl extends BranchManager 
							 implements ClientTransactionListener,
										ProxyBranch
{
    /**
      * Class Logger. 
      */
    private static final LogMgr c_logger = Log.get(ProxyBranchImpl.class);

    /**
     * Branch states
     */
    private static final int PB_STATE_INIT = 0;
    private static final int PB_STATE_SENT = 1;
    private static final int PB_STATE_TRYING = 2;
    private static final int PB_STATE_TIMED_OUT = 3;
    private static final int PB_STATE_COMPLETED = 4;
	private static final int PB_STATE_CANCEL_PENDING = 5; // state after the app called cancel() and before receiving a provisional response
	private static final int PB_STATE_CANCELED = 6;
	private static final int PB_STATE_REDIRECTING_REQUEST = 7;
	private static final int PB_STATE_FAILED_T0_BE_SENT = 8;
	

    /**
     * This branch URI
     */
    private URI _uri;

    
    /**
     * Transaction state
     */
    private int _state = PB_STATE_INIT;

    /**
     * The outgoing request
     */
    private SipServletRequestImpl _request;
    
    /**
     * Holds the information about the destination where the INVITE (if it is INVITE)
     * sent and provisional response was received on it. Will be used for next
     * CANCEL request it it will coming.
     */
    private SipURL _latestDestination;
    
    
    /**
	 * the time the container waits for a final response before it cancels
	 * this branch. 
	 */
	private int _proxyBranchTimeOut = SipConstants.DEFAULT_PROXY_TIMEOUT_SECONDS;

	/**
	 * Flag indicating whether the cancel operation has been called on this
	 * proxying operation. Cancel maybe called externally by the application
	 * or internally once a final response has been received from one of the
	 * branches. In any case the no new recurse branches can be created after 
	 * this proxyBranch has been cancelled. 
	 */
    private boolean _isCancelled; 

	/**
     * A reference to the owner director
     */
    protected StatefullProxy _proxy;
    
	/**
     * The owner proxy of this branch
     */
    protected ProxyParent _parent;

    /**
     * Reference to the last received response on this ProxyBranch.
     */
    private SipServletResponseImpl _lastResponse;

	/**
	 * The branch can be created using
	 * Proxy.createProxyBranches(List) and may be started at a later time by
	 * using Proxy.startProxy(). This method tells if the given branch has been
	 * started yet or not. The branches created as a result of proxyTo are
	 * always started on creation.
	 */
	private boolean _isStarted = false;
	
	/**
	 * Whether the application adds a Path header to the REGISTER request. 
	 * Default value will be taken from the Proxy when ProxyBranch created.
	 */
	private boolean _appPath = false;  
    
	
	/**
	 * A reference to the ProxyBranchTimer - Application can change it's value 
	 * at any time.
	 */
	ProxyBranchTimer _timer = null;
	
	/**
	 * Flag which indicates if timer on this ProxyBranch should be started.
	 * If this is a parent - it should started.
	 * If Application asked to start it - should be started.
	 */
	boolean _shouldStartTimer = false;
    
	/**
	 * Whether this branch was created as a result of a response created by the proxy itself
	 */
	private boolean _isVirtual = false;
	
	/**
	 * The latest final (according to the rfc 200 - 7xx) response received on the branch
	 */
	private int _latestFinalResponseStatus = -1;
	
	/** Reference the transaction users this branch relates to 
	 * (could be more then one in case of a derived session) 
	 */
	private ArrayList<TransactionUserWrapper> relatedTUs = new ArrayList<TransactionUserWrapper>();

	/**
	 * When first response with "remote Tag" received, SipContainer will relate to this specific
	 * ProxyBranch original TU (in case this is a first response received for this Proxy)
	 * or Derived TU (when additional response received for particular Proxy object)
	 */
	public boolean _someTUAlredyRelatedToThisBranch = false;
	
	/**
	 * Reference to the TU which is associated to this Proxy Branch.
	 * The "first" response with ToTag that received on the branch associates
	 * TU or DTU.
	 * "relatedTU" are ussually TU which were created by "downstream proxy" on the way.
	 * So , basically ProxyBranch knows about them but ProxyBranch has only 1 outgoing
	 * Client Transaction which relates to this ProxyBranch
	 */
	public TransactionUserWrapper _mainAssociatedTu =  null;
	
	/**
	 * will be true if this ProxyBranch was created as result of 3xx respnse
	 * when "recurse" flag is true.
	 */
	public boolean _amIRecurseBranch = false;
	
	/**
	 * This flag become true when related CT was removed from original TU when
	 * transaction was terminated.
	 * We can remove CT transaction from TU only once.
	 */
	boolean _removedFromOrigTU = false;
	/**
     * Constructor
     * 
     * @param uri the target uri of this branch 
     * @param proxy the owner proxy
     */
    public ProxyBranchImpl(	URI uri, 
    						BranchManager parent,
    						StatefullProxy proxyMgr,
    						boolean createdByProxyTo,
    						boolean isVirtual)
    {
    	super((SipServletRequestImpl)proxyMgr.getOriginalRequest(),proxyMgr);
    	
    	_uri = uri;
    	if (uri != null) {
    		_uri = uri.clone();
    	}
    	
        _parent = parent;
        _proxy = proxyMgr;
        _isStarted = createdByProxyTo;
        _appPath = proxyMgr.getAddToPath();
        _isVirtual = isVirtual;
        
        SipServletRequestImpl request = (SipServletRequestImpl)_proxy.getOriginalRequest();
		SipServletDesc siplet = request.getTransactionUser().getSipServletDesc();
		if(_originalReq.getTransactionUser().isRelatedToBranch()){
			_originalReq.getTransactionUser().addExtraTransaction();
		}
		else{
			relateTU(_originalReq.getTransactionUser());
		}
		
		if(!isVirtual){
			//	Let base to do the proxying the branch will be the listener	
	   	 	// Create a new request, based on the original
	       _request = createOutgoingRequest(_originalReq);
	       _request.setRequestURI(uri);
	       
	       //adding the proxy object to the outgoing request
	       ((OutgoingSipServletRequest)_request).setProxy(proxyMgr);
	       
	       //before receiving any responses, the transaction user of all branches will be equal to this of the 
	       //original incoming response.
	       _request.setTransactionUser(_originalReq.getTransactionUser());
		}
		
		if(siplet != null)
		{
		    _proxyBranchTimeOut = siplet.getSipApp().getProxyTimeout();
		}
		
		setRecordRoute(proxyMgr.getRecordRoute());
        
	    //	Here we need to make sure that any outbound interfaces set on proxy manager
	    //	are passed to the newly created branch.
    	_preferedOutBoundIfaceIdxUDP = proxyMgr.getPreferedOutboundIface("udp");
    	_preferedOutBoundIfaceIdxTCP = proxyMgr.getPreferedOutboundIface("tcp");
    	_preferedOutBoundIfaceIdxTLS = proxyMgr.getPreferedOutboundIface("tls");
    	
    	//	We have to get the stateful proxy's RR URI here in case the application
    	//	has modified it. If this is a branch created by the application we must
    	//	set this back to null if changes come in that would affect record routing
    	//	(such as the setting of a preferred outbound interface).
    	if (proxyMgr.getRecordRoute() == true)
    		_recordRouteURI = proxyMgr.getRecordRouteURI();
		
        if (c_logger.isTraceDebugEnabled()) {
			StringBuffer buff = new StringBuffer();
			buff.append(getMyInfo());
			buff.append("New ProxyBranch created:");
			buff.append(" uri = ");
			buff.append(_uri);
			buff.append(" parent = ");
			buff.append(_parent);
			buff.append(" isStarted = ");
			buff.append(_isStarted);
			buff.append(" appPath = ");
			buff.append(_appPath);
			buff.append(" ");
			c_logger.traceDebug(this, "ProxyBranchImpl", buff.toString());
		}
    }
    /**
     * 
     * @return an unmodifiable iterator to the list of "relatedTus"
     */
    public Iterator<TransactionUserWrapper> getRelatedTUs() {
    	return Collections.unmodifiableList(relatedTUs).iterator();
    }

    /**
     * Add references between the TU and this branch
     * @param tu
     */
    public void relateTU(TransactionUserWrapper tu){
    	relatedTUs.remove(tu);//make sure this only added once
    	relatedTUs.add(tu);
    	
    	if(_request!=null) {
    		_request.setTransactionUser(tu);
    	}
    	//note that in a parallel forking case,  
    	// the request might got sent with the original TU, but the first response arrived on a different branch   
    	//(at the beginning, all branches and outgoing requests relates to the original TU). In this case the branch and  
    	//the response needs to be re-related to a different TU, once another response arrived creating a derived session. 
    	tu.setBranch(this);
    	if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceDebug(this, "relateTU", "Relating TU= " + tu +
    				" to branch= " + this + ". Number of TUs related to this branch = "+ relatedTUs.size());
      	}
    }
    
    /**
     * Detach the reference of the TU from this branch
     * This would be used when the original session gets first response from a branch other then the first one
     * it was related to at first. This can happen on a parallel forking.In this case will get unrelated to this branch
     * and related to the branch that carried the first response. 
     * @param tu
     */
    public void unrelateTU(TransactionUserWrapper tu){
    	relatedTUs.remove(tu);
    	if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceDebug(this, "unrelateTU", "Unrelating TU= " + tu +
    				" from branch= " + this + ". Number of TUs related to this branch = "+ relatedTUs.size());
      	}
    }
    
    /**
     * Will check whether any related TU got a final response.
     */
    public boolean hasAnyTUGotFinalResponse(){ 
    	 if (c_logger.isTraceEntryExitEnabled()) {
    		 c_logger.traceEntry(this, "hasAnyTUGotFinalResponse", "Related TU number = "+ relatedTUs.size());
    	 }
    	 boolean result = false;
    	 for(TransactionUserWrapper tu : relatedTUs){
    		 if (c_logger.isTraceDebugEnabled()) {
    				c_logger.traceDebug(this, "hasAnyTUGotFinalResponse", 
    						"checking for " + tu);
    			}
    		 if(tu.isProxyReceivedFinalResponse()){
    			 result = true;
    			 break;
    		 }
    	 }
    	 
    	 if (c_logger.isTraceEntryExitEnabled()) {
    		 c_logger.traceExit(this, "hasAnyTUGotFinalResponse", new Boolean(result));
    	 }
    	 return result;
    }
    
    /**
     * Incrementing the transaction counter of all related TUs
     */
    public void incrementTransactionCounters(){ 
    	 if (c_logger.isTraceEntryExitEnabled()) {
    		 c_logger.traceEntry(this, "incrementTransactionCounters", "Related TU number = "+ relatedTUs.size());
    	 }
    	 
    	 for(TransactionUserWrapper tu : relatedTUs){
    		 if (c_logger.isTraceDebugEnabled()) {
    				c_logger.traceDebug(this, "incrementTransactionCounters", 
    						"Incrementing transaction count for " + tu);
    			}
    		 tu.incrementTransactions();
    	 }
    	 
    	 if (c_logger.isTraceEntryExitEnabled()) {
    		 c_logger.traceExit(this, "incrementTransactionCounters");
    	 }
    }
    
    /**
     * Decrementing the transaction counter of all related TUs
     */
    public void decrementTransactionCounters(){ 
    	 if (c_logger.isTraceEntryExitEnabled()) {
    		 c_logger.traceEntry(this, "decrementTransactionCounters", "Related TU number = "+ relatedTUs.size());
    	 }
    	 
    	 for(TransactionUserWrapper tu : relatedTUs){
    		 if (c_logger.isTraceDebugEnabled()) {
    				c_logger.traceDebug(this, "decrementTransactionCounters", 
    						"Decrementing transaction count for " + tu);
    			}
    		 tu.decrementTransactions();
    	 }
    	 
    	 if (c_logger.isTraceEntryExitEnabled()) {
    		 c_logger.traceExit(this, "decrementTransactionCounters");
    	 }
    }
    
   /**
    * Send the associated request from this ProxyBranch
    * @throws IOException
    */
    public void continueAndSend()throws IOException{

		proxy((OutgoingSipServletRequest)_request,_uri, this); 
        
        if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "continueAndSend", 
					getMyInfo() + " ProxyBranch will expire in " + _proxyBranchTimeOut + " seconds");
		}
    }
    
    /**
     * helper method which decides if this ProxyBranch is active or not
     * @return
     */
    public boolean isActive(){
    	
    	if(!isInitial()){
    		return false;
    	}
    	
    	if(_state == PB_STATE_INIT ||
    		_state == PB_STATE_SENT ||
    		_state == PB_STATE_REDIRECTING_REQUEST ||
    		_state == PB_STATE_TRYING){
    			return true;
    		}
    	
    	return false;
    }
    
    
    /**
     * helper method which decides 200OK should be retransmitted on the 
     * current branch according to the branch state,
     * this is exactly like the isActive() method except that 
     * Retransmission for completed branches should be also allowed
     * @return
     */
    public boolean isRetransmission(){
    	
    	if(!isInitial()){
    		return false;
    	}
    	
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "isRetransmission", "branch state: " + _state);
		}
    	
    	if(_state == PB_STATE_INIT ||
    		_state == PB_STATE_SENT ||
    		_state == PB_STATE_REDIRECTING_REQUEST ||
    		_state == PB_STATE_TRYING ||
    		_state == PB_STATE_COMPLETED){
    			return true;
    		}
    	
    	return false;
    }
    
    /** 
     * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#processResponse(javax.servlet.sip.SipServletResponse)
     * This method called from ClientTransaction.
     */
    public void processResponse(SipServletResponseImpl response)
    {
        if(c_logger.isTraceDebugEnabled())
        {
            StringBuffer buffer = new StringBuffer();
            buffer.append(getMyInfo());
            buffer.append("response is ");
            buffer.append(response.getStatus());
            buffer.append(response.getReasonPhrase());
			c_logger.traceDebug(this, "processResponse", buffer.toString() );
        }

        updateStatusFromLastResponse(response);
        _lastResponse = response;
        int status = response.getStatus();
        if( response.getRequest().isInitial() && 
        		status >= 200 &&
        		((SipServletRequestImpl)response.getRequest()).isJSR289Application()) {
        	//Proxy Final response is not committed.
        	response.setIsCommited(false);
        	associateResponseWithSipSession(response, this);
        	//Don't send branch response to application on 2xx & 6xx response
        	if (treat2xx6xxAsBestResponse(response.getStatus())) {  
        		notifyIntermediateBranchResponse(response);
        	}
        }

        //Response to CANCEL requests are not passed up to the proxy
		if(!response.getMethod().equals(Request.CANCEL))
		{
			// What kind of response is it?
	        
	        if (status < 200)
	        {
	            //the branch was CANCELED before the trying response was 
	            //received - send CANCEL request on this branch
	        	synchronized (this) {
		            if(_state == PB_STATE_CANCEL_PENDING){
		                _state = PB_STATE_CANCELED;
		                cancelRequest(_request, this, null);
		            }
		            else{
		                _state = PB_STATE_TRYING;
		        	    //Setup the timer for this ProxyBranch
		                
		                if(_shouldStartTimer){
		                	setupProxyBranchTimer();
		            	}
		             }
	        	}
	            _parent.process1xxResponse(response,this);
	        } else if ((status >= 300) && (status < 400)){
	            if (_isRecurse){
	            	_state = PB_STATE_REDIRECTING_REQUEST;
	            	redirectResponse(response);
	                return;
	            }
	            else{
	            	if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "processResponse", 
								getMyInfo() + "ProxyBranch is not in recurse mode");
					}
	            	_state = PB_STATE_COMPLETED;
	            	_parent.processResponse(this,response);
	            }
	        }	        
	        else{
	        	// got response which completes this branch.
	        	updateBestResponse(_lastResponse,this);
	        	allBranchesCompleted();
	        }
		}
		else
		{
			if(c_logger.isTraceDebugEnabled())
            {
            	c_logger.traceDebug(this, 
				 					"processResponse" ,
				 					getMyInfo() + "Received response to CANCEL request: " + 
				 					response.getStatus());
            }
		}
    }

    /**
     * This method will store the latest final response received on this ProxyBranch
     * @param response
     */
    private void updateStatusFromLastResponse(SipServletResponse response) {
    	int status = response.getStatus();
    	
    	if(c_logger.isTraceDebugEnabled()){
        	c_logger.traceDebug(this, "updateStatusFromLatestResponse" ,"received status = " + status);
        }
    	
    	if(status > 199){
    		_latestFinalResponseStatus = status;
    	}
	}

    /**
     * Return latest final response status receive on this ProxyBranchImpl
     * @return
     */
	public int getLatestFinalResponseStatus() {
		return _latestFinalResponseStatus;
	}
    
	/** called when a final response arrives on the branch, to notify the
     * (JSR 289) application, of intermediate final responses.
     * @param response the final response arriving on the branch
     * @see javax.servlet.sip.SipServlet#doBranchResponse
     */
    protected void notifyIntermediateBranchResponse(SipServletResponseImpl response) {
    	// get the root proxy instance, to tell if it is supervised.
    	// only supervised proxy should notify the app with doBranchResponse.
    	StatefullProxy root = null;
    	ProxyParent node = this;

    	do {
    		if (node instanceof StatefullProxy) {
    			root = (StatefullProxy)node;
    			break;
    		}
   			node = node.getParent();
    	} while (node != null);

    	// notify the application
    	if (root == null) {
    		if (c_logger.isErrorEnabled()) {
    			c_logger.error("error.exception", null, null, new RuntimeException("no root"));
    		}
    	}
    	else if (root.getSupervised()) {
    		TransactionUserWrapper tu = response.getTransactionUser();
    		if (tu == null) {
        		if (c_logger.isTraceDebugEnabled()) {
        			c_logger.traceDebug(this, "notifyIntermediateBranchResponse",
        				"no transaction user");
        		}
    		}
    		else {
        		if (c_logger.isTraceDebugEnabled()) {
        			c_logger.traceDebug(this, "notifyIntermediateBranchResponse",
        				"invoking intermediate branch response notification");
        		}
        		
        		//this is the only place that we need to set the response as a branch response
        		//the response is passed to the application as an intermediate proxy response
        		//and will be changed to a regular response when the application callback is done
        		response.setIsBranchResponse(this);
	    		tu.sendResponseToApplication(response, null);
	    		response.setIsBranchResponse(null);
    		}
    	}
    }

    /**
     * Helper Method which creates a ProxyBranchTimer.
     */
    private void setupProxyBranchTimer() {
		if (_timer != null && !_timer.isCancelled()) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setupProxyBranchTimer", getMyInfo()
						+ "Warning. Timer is active.");
			}
		} 
		else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setupProxyBranchTimer", getMyInfo());
			}
			_timer = new ProxyBranchTimer(this);
			SipContainerComponent.getTimerService().schedule(_timer, false,
					_proxyBranchTimeOut * 1000);
			
			_shouldStartTimer = false;
		}
	}

    /**
	 * @see com.ibm.ws.sip.container.proxy.
	 *      ProxyParent#process1xxResponse(javax.servlet.sip.SipServletResponse)
	 */
	public void process1xxResponse(SipServletResponse response,ProxyBranchImpl branch){
		_parent.process1xxResponse(response,branch);
	}
	
	/**
	 * @see com.ibm.ws.sip.container.proxy.ProxyParent#handleBestResponse(
	 * 				com.ibm.ws.sip.container.proxy.ProxyBranchImpl,
	 *      		javax.servlet.sip.SipServletResponse) 
	 * This method called from "child" ProxyBranch. 
	 */
    public void processResponse(ProxyBranchImpl branch, SipServletResponse response){
    	
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "processResponse", 
					getMyInfo() + "Response from child branch = " + branch + " response " + response );
		}
    	
    	updateStatusFromLastResponse(response);
    	int status = response.getStatus();
        if (status < 200 ) {
            if (status != 100){
                _parent.process1xxResponse(response,branch);
            }
            return;
        }

        // "...When a 2xx or 6xx response is received the server CANCELs all
        //	outstanding branches and will not create new branches"
        if (isFinalResponse(status)) {
        	// When Proxy branch received this kind of response - it should
        	// cancel all it's recurse branches and forward the 
        	// response as best response to it's parent.
            updateBestResponse(response,this);
            cancel();
            cancelProxyBranchTimer();
            allBranchesCompleted();
            return;
        }

        // OK, nothing left to do beside going to next hop
        if(!_proxy.getParallel()){
        	cancelProxyBranchTimer();
        }   
        
        if(branch.isCompleted()){
        	if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "processResponse", getMyInfo() + "This branch is completed");
			}
        	branchCompleted(branch, response);
        }
        
    }

    /**
     * Helper method which cancelled the ProxyBranchTimer.
     *
     */
    private void cancelProxyBranchTimer() {
    	if(_timer != null && ! _timer.isCancelled()){
    		_timer.cancel();
    		_timer = null;
    	}
    	else{
    		if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "cancelProxyBranchTimer", getMyInfo() + "Timer is null or cancelled = " + _timer);
			}
    	}
		
	}

	/**
     * Redirect the request to the location in the indirect response
     * 
     * @param response
     */
    protected void redirectResponse(SipServletResponse response)
    {
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "redirectResponse", getMyInfo() + " response = " + response);
		}
    	
        // According to spec. there are two situations: Multiple choice and all the others, 
        // but in both cases all contacts are in the "contact" header field
        // so in cases of 301, 302, 305 and 380 there is only one element in list but
        // the spec is not completely clear so we are allowing multiple contacts
        //on all responses. 

    	ContactHeader contact = null;
            
        List <ContactHeader> contacts = getContacts(response);
        
        if(contacts.size() > 0){
	        SipServletsFactoryImpl factory = SipServletsFactoryImpl.getInstance();
	        // Proxy to all contacts in the list
	        for (int j = 0; j < contacts.size(); j++) {
				contact = ((ContactHeader) contacts.get(j));
				URI uri = factory
						.generateURI(contact.getNameAddress().getAddress());
	
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "redirectResponse",
							getMyInfo() + "Adding Contact: " + contact);
				}

				removeHandledContactFromResponse(response,contact);

				//if a proxy branch was already created for the uri, we should move on to the next
				if (proxyBranchExists(uri)){
					continue;
				}
				// Add hop to waiting ProxyBranches list
				createBranch(uri, true,_proxy);			
			}
	        send();
        }
        else{
        	if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "redirectResponse", "No Contacts to redirect");
			}
        	updateBestResponse(_request,SipServletResponse.SC_FORBIDDEN,this);
        	allBranchesCompleted();
        }
        
    }
        
    /**
     * helper method that searches and removes duplicate contact headers
     *  
     * @param response - the response
     * @param contact - the contact header
     */
    private void removeHandledContactFromResponse(SipServletResponse response, ContactHeader contact) {
    	//get the jain response
    	Message jainResponse = ((SipServletResponseImpl)response).getMessage();
    	//get header iterator over the contact headers
    	HeaderIterator itr = jainResponse.getHeaders(SipConstants.CONTACT);
    	//if the iterator does not exist, we can finish
    	if (itr == null){
    		return;
    	}
    	//go over all contact headers in the message
    	while (itr.hasNext()){
    		try {
    			//if we find this contact header in the message, 
    			//we remove it from the message
				if (itr.next().equals(contact)){
					itr.remove();
					break;
				}
			} catch (Exception e) {
				continue;
			}
    	}
		
	}

	/**
     * Helper method which extracts contacts from received response
     * and orders it according to priority
     * @param response
     * @return
     */
    private List<ContactHeader> getContacts(SipServletResponse response) {
    	
    	Response jainResponse = ((SipServletResponseImpl)response).getResponse();
        
        HeaderIterator hIter = jainResponse.getContactHeaders();
        
    	List <ContactHeader> contacts = new ArrayList <ContactHeader> (3);
        ContactHeader contact = null;
        while(hIter.hasNext())
        {
            try {
                contact = (ContactHeader)hIter.next();
                float currentQValue; 
                float otherQValue; 
                for(int j=0; j<contacts.size(); j++)
                {
                    currentQValue = contact.getQValue();
                    if(currentQValue < 0)
                    {
                        //Use the default value of 1 if value not available
                        currentQValue = (float) 1.0; 
                    }
                    otherQValue = ((ContactHeader)contacts.get(j)).getQValue();
                    if(otherQValue < 0)
                    {
                        otherQValue = (float) 1.0; 
                    }
                    
                    //Sort contacts according to Q value - highest values come first
                    if( currentQValue> otherQValue) 
                    {
                        contacts.add(j, contact);
                        contact = null;
                        break;
                    }
                }
                
                if(contact != null)
                {
                    //Was not added yet. Add at the end of the list. 
                    contacts.add(contact);
                }
            }
            catch (HeaderParseException e) 
            {
                logException(e);
            }
            catch (NoSuchElementException e) 
            {
                logException(e);
            }
            
        }       
        return contacts;
	}

	/**
	 * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#processTimeout(javax.servlet.sip.SipServletRequest)
	 */
    public void processTimeout(SipServletRequestImpl request)
    {
        if(c_logger.isTraceDebugEnabled())
        {
			c_logger.traceDebug(this, 
								"processTimeout", 
								getMyInfo() + "request is " + request.getMethod());
        }
       
        // Process the timeout response to the proxy application 
        // For response on a request different than the original _request (CANCEL, PRACK, UPDATE)
        if (request.getMethod().equals(_request.getMethod())) {
        	executeTimeOut(true);
        }
        else {
        	SipServletResponse response = generateResponse(request,SipServletResponse.SC_REQUEST_TIMEOUT);
        	processResponse(this, response);
        }
    }

    /**
     * Application router exception caused 500 response downstream
     */
	public void processCompositionError(SipServletRequestImpl request) {

        if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "processCompositionError");
		}

        _state = PB_STATE_FAILED_T0_BE_SENT;
       
        updateBestResponse(request,SipServletResponse.SC_SERVER_INTERNAL_ERROR,this);
        
        allBranchesCompleted();
        
        if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "processCompositionError");
		}
        
	}
 
    /**
     * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#onSendingRequest(javax.servlet.sip.SipServletRequest)
     */
    public boolean onSendingRequest(SipServletRequestImpl request)
    {
    	 setupTimeOut();
        _state = PB_STATE_SENT;
        _parent.onSendingRequest(this, request);
        
        return true;
    }
    
    
    
    /**
     * Return the SipServletRequest associated with this branch.
     * @see com.ibm.ws.sip.container.proxy.BranchManager#getRequestForInternalUse()
     */
    SipServletRequest getRequestForInternalUse() {
        return _request;
    }

    /**
     * Is this branch is waiting for response
     * @return true if this branch is waiting for response, false otherwise
     */
    public boolean waitingForResponse()
    {
        if ((PB_STATE_INIT == _state)
            || (PB_STATE_COMPLETED == _state)
            || (PB_STATE_TIMED_OUT == _state)
            || (PB_STATE_CANCEL_PENDING == _state)
            || (PB_STATE_CANCELED == _state)
    		|| (PB_STATE_FAILED_T0_BE_SENT == _state))
        {
            return false;
        }

        return true;
    }

    /**
     * Is this branch has been completed
     *  
     * @return true if this branch has been completed, false otherwise
     */
    public boolean isCompleted()
    {
        return !waitingForResponse();
    }

  /**
   * @see com.ibm.ws.sip.container.proxy.BranchManager#cancel()
   */
    public void cancel()
    {
    	cancel(null,null,null);
    	
    }

    /**
     *  @see javax.servlet.sip.ProxyBranch#cancel(java.lang.String[], int[], java.lang.String[])
     */
    public void cancel(String[] protocol, int[] reasonCode, String[] reasonText){
    	if (c_logger.isTraceDebugEnabled()){
    		Object[] params = {  protocol, reasonCode,reasonText,getMyInfo()};
			c_logger.traceEntry(this, "cancel", params);
        }
        
        if(_isCancelled)
	    {
        	if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "cancel", getMyInfo() + "already cancelled");
			}
	        return;
	    }
        
        cancelProxyBranchTimer();
    	
        synchronized (this) {
        	switch (_state) {
	        
				case PB_STATE_REDIRECTING_REQUEST:
					if (c_logger.isTraceDebugEnabled()) {
						c_logger
								.traceDebug(
										this,
										"cancel",
										getMyInfo()
												+ "This proxy is in recurse mode - cancel all branches");
					}
					// Iterate through all branches
					for (int i = 0; i < _proxyBranches.size(); i++) {
						// Get the next branch
						ProxyBranchImpl branch = (ProxyBranchImpl)_proxyBranches.get(i);
						if(branch.isActive()) {
							branch.cancel(protocol,reasonCode,reasonText);
						}
					}
					break;
					
				case PB_STATE_TRYING:
					// CANCEL can be sent only after the trying response received
					List <ReasonHeaderImpl> reasons = null;
			    	
			        if(protocol != null){
			        	reasons = new Vector<ReasonHeaderImpl>(protocol.length);
			        	for(int i = 0;i<protocol.length;i++){
			        		
			        		try {
								ReasonHeaderImpl reason  =new ReasonHeaderImpl(protocol[i],
																				reasonCode[i],
																				reasonText[i]);
								reasons.add(reason);
							} catch (SipParseException e) {
								//should not get here. Creation can't fail.
								e.printStackTrace();
							}
			        	}
			        	
			        }
					_state = PB_STATE_CANCELED;
					cancelRequest(_request, this,reasons);
					break;
				
				case PB_STATE_INIT:
				case PB_STATE_SENT:
					_state = PB_STATE_CANCEL_PENDING;
					break;
	
			default:
				break;
			}
        }
            
        // Remove all branches
		_isCancelled = true;
    }
    
    /**
     * Cancels the branch with the reasons given as a string array - reasons can be in both formats - separated to diffrent cell in the array or
     * separated with a ',' 
     * @param origReasons the reasons that need to be added to the outgoing cancel method
     */
    public void cancel(String[] origReasons) {
      	if (c_logger.isTraceDebugEnabled()) {
    		Object[] params = { origReasons,getMyInfo()};
			c_logger.traceEntry(this, "cancel", params);
        }
        
        if(_isCancelled) {
        	if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "cancel", getMyInfo() + " already cancelled");
			}
	        return;
	    }
        
        cancelProxyBranchTimer();
    	
        synchronized (this) {
        	switch (_state) {
	        
				case PB_STATE_REDIRECTING_REQUEST:
					if (c_logger.isTraceDebugEnabled()) {
						c_logger
								.traceDebug(
										this,
										"cancel",
										getMyInfo()
												+ " This proxy is in recurse mode - cancel all branches");
					}
					// Iterate through all branches
					for (int i = 0; i < _proxyBranches.size(); i++) {
						// Get the next branch
						ProxyBranchImpl branch = (ProxyBranchImpl)_proxyBranches.get(i);
						if(branch.isActive()) {
							branch.cancel(origReasons);
						}
					}
					break;
					
				case PB_STATE_TRYING:
					// CANCEL can be sent only after the trying response received
					List<ReasonHeaderImpl> reasons = null;
					try {
						reasons = SipUtil.parseReasons(origReasons);
					} catch (SipParseException e) {
						if (c_logger.isErrorEnabled()) {
							c_logger.traceDebug(this, "cancel", getMyInfo()	+ e.getLocalizedMessage());
						}
					}
				    	
					_state = PB_STATE_CANCELED;
					cancelRequest(_request, this, reasons);
					break;
				
				case PB_STATE_INIT:
				case PB_STATE_SENT:
					_state = PB_STATE_CANCEL_PENDING;
					break;
	
			default:
				break;
			}
        }
            
        // Remove all branches
		_isCancelled = true;
    }
  
    /**
     * Notification received from the timer / or from the StatefulProxy and
     * indicating that this branch should
     * be dropped due to ProxyBranchTimer or ProxyTimer timeout. 
     */
    public void proxyBranchTimeout() {
    	
        if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "proxyBranchTimeout " + getMyInfo());
		}
      
        executeTimeOut(false);
	}
    
    
    /**
     * Helper method which is actually performs timeOut call to this
     * object children or cancels itself.
     *
     */
    private void executeTimeOut(boolean isTimeout) {
    	if (c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "executeTimeOut", isTimeout);
        }
    	
    	
    	
    	cancelProxyBranchTimer();
		
        if(_state != PB_STATE_REDIRECTING_REQUEST){
        	// This ProxyBranch doesn't have recurse branches.
        	// Cancel itself.
        	cancel();

        	//terminate the branch only if the transaction has timed out 
        	//if the branch/proxy timer timed out we only need to send cancel
        	if (isTimeout){
        		// Change our state
        		_state = PB_STATE_TIMED_OUT;

        		updateBestResponse(_request,SipServletResponse.SC_REQUEST_TIMEOUT,this);

        		allBranchesCompleted();
        	}
        }        
        else {
        	timeoutAllChildBranches(isTimeout);
        }   
	}

	/**
	 * Method called from StatefullProxy only and it is a notification that this
	 * BranchMethod was timed out. If this object belongs to parallel proxy -
	 * all related branches should be canceled, and TimedOut response should be
	 * forwarded to the parent. In case it is not a parallel proxy - next Branch
	 * should be used to send the request out.
	 * 
	 */

	//remove synchronized as it caused deadlock 
	public void proxyTimedOut(boolean isTimeout){
    	
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "proxyTimedOut", getMyInfo() );
		}
    	
    	_parentTimedOut = true;
    	
    	executeTimeOut(isTimeout);
	}

    /**
     * Gets the branch Id of this Proxy Branch from the top via header. 
     * @return
     * @throws IllegalArgumentException
     * @throws HeaderParseException
     */
    public String getBranchId() throws HeaderParseException, IllegalArgumentException
    {
        Request req = ((SipServletRequestImpl)_request).getRequest();
        ViaHeader via = (ViaHeader) req.getHeader(ViaHeader.name, true);
        return via.getBranch();
    }
    
    /**
     * @see com.ibm.ws.sip.container.transaction.ClientTransactionListener#clientTransactionTerminated(javax.servlet.sip.SipServletRequest)
     */
	public void clientTransactionTerminated(SipServletRequestImpl request) {

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "clientTransactionTerminated",
					"Notify all related TUs");
		}
		// Transaction was terminated.
		// Notify all related TUs.
		// We have related TUs in case of downstream proxy response.
		for (TransactionUserWrapper toToNotify : relatedTUs) {
			
			toToNotify.clientTransactionTerminated(request);
			
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "clientTransactionTerminated",
						"Notify DTU " + toToNotify.getId());
			}
		}
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

	/** 
	 * @see javax.servlet.sip.ProxyBranch#getProxyBranchTimeout()
	 */
	public int getProxyBranchTimeout() {
		return _proxyBranchTimeOut;
	}

	/**
	 *  @see javax.servlet.sip.ProxyBranch#setProxyBranchTimeout(int)
	 */
	public void setProxyBranchTimeout(int seconds) throws IllegalArgumentException {
		
		if(seconds < 1){
			throw new IllegalArgumentException ("setProxyBranchTimeout cannot handle value " + seconds);
		}
		
		if(_proxy.getParallel()){
			//calculate the remaining proxy seconds
			int proxyTimerTime = _proxy.getProxyTimeout();
			if (_proxy.getTimer() != null){
				proxyTimerTime = _proxy.getTimer().getTimeRemaining();
			}
			
			if (proxyTimerTime < seconds) {
				// From Servlet API 1.1:
				// Application called to setProxyBranchTimeout() method. When
				// the ProxyBranchImpl represents parallel proxy - the new
				// parameter of timeout should be lower than 
				// Proxy.getProxyTimeout()
			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer buff = new StringBuffer();
				buff.append(getMyInfo());
				buff.append("Failed to set timeout for parallel ProxyBranch");
				buff.append(" Proxy timeout = ");
				buff.append(proxyTimerTime);
				buff.append(" requests timeout for this branch = ");
				buff.append(seconds);
				c_logger.traceDebug(this, "setProxyBranchTimeout", buff.toString());
			}
				throw new IllegalArgumentException ("Value should be lower than ProxyTimeout in parallel case");
			}
		}

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setProxyBranchTimeout", 
					getMyInfo() + "ProxyBranch expiration timeout = " + seconds + "seconds");
		}		
		
		_proxyBranchTimeOut = seconds;

		// Set new timer only if state is trying in sequential mode or started in parallel mode. Otherwise when it will 
		// become a TRYING or started - this timer will use a new _proxyBranchTimeOut value;
		if(_state == PB_STATE_TRYING || (isStarted() && _proxy.getParallel())){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setProxyBranchTimeout", 
						getMyInfo() + "Reschedule the timer for " + seconds + " from now");
			}
			if(_timer != null){
				_timer.cancel();
			}
			setupProxyBranchTimer();
		}
		else{
			// start the timer when state will be changed to PB_STATE_TRYING or starting in parallel mode
			_shouldStartTimer = true;
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setProxyBranchTimeout", 
						"Timer will start when State will be TRYING or starting in parallel mode.");
			}
		}
	}

	/**
	 *  @see javax.servlet.sip.ProxyBranch#getResponse()
	 */
	public SipServletResponse getResponse() {
		// TODO should be clarified in JSR 289:
		// ProxyBranch.getResponse() method should return the last
		// response received on this branch. Does it mean that response received
		// from the recurse branch should not be saved ? Is it saved as latest
		// response on the recurse ProxyBranch ? For now it will not return
		// responses from recurse ProxyBranches.
		return _lastResponse;
	}
	
	/**
	 * Return status of the branch, if branch was never sent but proxy was started
	 * it will be true, if proxy wasn't started than it will be false.
	 * @return
	 */
	public boolean isInitial() {
		return _isStarted ;

	}
	/**
	 *  @see javax.servlet.sip.ProxyBranch#isStarted()
	 */
	public boolean isStarted() {
		return (_state != PB_STATE_INIT);		
	}

	/**
	 *  @see javax.servlet.sip.ProxyBranch#getRecursedProxyBranches()
	 */
	public List<ProxyBranch> getRecursedProxyBranches() {
		List<ProxyBranch> allBranches = Collections.emptyList(); 
			
		
    	if(_proxyBranches == null || _proxyBranches.isEmpty()){
    		if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getRecursedProxyBranches", 
						getMyInfo() + " No ProxyBranches were created");
			}
    		return allBranches;
    	}
    	
    	for (ProxyBranch branch : _proxyBranches) {
    		if (branch.getRecurse()) {
    			if (allBranches.size() == 0) {
    				allBranches = new ArrayList<ProxyBranch>();
    			}
    			allBranches.add(branch);
    		}
    	}
    	
    	return allBranches;
	}

	/**
	 *  @see com.ibm.ws.sip.container.jain289API.ProxyBranch#getProxy()
	 */
	public Proxy getProxy() {
		return _proxy;
	}

	
	/**
	 *  @see com.ibm.ws.sip.container.jain289API.ProxyBranch#setAddToPath(boolean)
	 */
	public void setAddToPath(boolean p) {
		_appPath = p;
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setAddToPath", getMyInfo() + " Add to path = " + _appPath);
		}
	}

	/**
	 *  @see com.ibm.ws.sip.container.jain289API.ProxyBranch#getAddToPath()
	 */
	public boolean getAddToPath(){
		return _appPath;
	}
	
	/**
	 *  @see com.ibm.ws.sip.container.jain289API.ProxyBranch#getRecurse()
	 */
	public boolean getRecurse() {
		return _isRecurse;
	}

	/**
	 *  @see com.ibm.ws.sip.container.jain289API.ProxyBranch#setRecurse(boolean)
	 */
	public void setRecurse(boolean recurse) {
		_isRecurse = recurse;
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setRecurse", 
					getMyInfo() +" This ProxyBranch is recurse = " + _isRecurse);
		}
	}

	/**
	 * Method which is responsible to find ProxyBranch according to the
	 * branchId. Lookup performed in all recursed branches.
	
	 * @param branchId
	 
	 * @return
	 */
	public ProxyBranchImpl findRecurseBranch(String branchId) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "findRecurseBranch", getMyInfo()
					+ " branchID = " + branchId);
		}	
		
		// In this case we should not compare this branch ID to the given
		// branchId as it was compared in the parent findRecurseBranch() or
		// handle2xxRetransmission() methods. Look in ActiveBranches only
		try {
			ProxyBranchImpl foundProxyBranch = null;
			
			for (int i = 0; i < _proxyBranches.size() && foundProxyBranch != null; i++) {
				
				ProxyBranchImpl proxyBranch = (ProxyBranchImpl)_proxyBranches.get(i);
				if(proxyBranch.isActive()){
					
					if (proxyBranch.getBranchId().equals(branchId)) {
						foundProxyBranch = proxyBranch;
					} 
					else {
						foundProxyBranch = proxyBranch.findRecurseBranch(branchId);
					}	
				}				
			}			
		} 
		catch (HeaderParseException e) {
			logException(e);
			e.printStackTrace();
		}

		return null;
	}
	
	/**
	 * Helper method which is looking for recurse branch associated with uri.
	 * 
	 * @param uri
	 * @return
	 */
	public ProxyBranchImpl findRecurseBranchByUri(URI uri) {
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "findRecurseBranchByUri", 
					getMyInfo() + " uri = " + uri);
		}
		// In this case we should not compare this branch ID to the given
		// branchId as it was compared in the parent findRecurseBranch() or
		// getProxyBranch() methods.
		
		ProxyBranchImpl foundProxyBranch = null;
		
		for (int i = 0; i < _proxyBranches.size() && foundProxyBranch == null; i++) {
			ProxyBranchImpl branch = _proxyBranches.get(i);
			if (branch.getUri().equals(uri)) {
				foundProxyBranch = branch;
			} 
			else {
				foundProxyBranch = branch.findRecurseBranchByUri(uri);
			}
		}
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "findRecurseBranchByUri", 
					getMyInfo() + " FoundBranch = " + foundProxyBranch);
		}
		
		return foundProxyBranch;
	}

	/**
	 * @see com.ibm.ws.sip.container.proxy.ProxyParent#
	 * 	onSendingRequest(	com.ibm.ws.sip.container.proxy.ProxyBranchImpl,
	 *      				javax.servlet.sip.SipServletRequest) 
	 *      In ProxyBranch object this will forward the call to the parent.
	 */
	public void onSendingRequest(ProxyBranchImpl branch, SipServletRequest request){
		setupTimeOut();
		_parent.onSendingRequest(branch,request);
	}
	
	/**
	 *  @see com.ibm.ws.sip.container.proxy.BranchManager#allBranchesCompleted()
	 *  
	 *  ProxyBranch should forward it's best response to it's parent.
	 *  
	 */
    public void allBranchesCompleted() {
    	
    	if(_state != PB_STATE_COMPLETED){
    		
    		cancelProxyBranchTimer();
    		
			SipServletResponse response = _bestResponse.getBestResponse();

			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer buff = new StringBuffer();
				buff.append(getMyInfo());
				buff.append("Forwarding the Best Response = ");
				buff.append(response);
				buff.append(" to Parent = ");
				buff.append(_parent);
				c_logger.traceDebug(this, "allBranchesCompleted", buff
						.toString());
			}

			_state = PB_STATE_COMPLETED;
			
			_parent.processResponse(this, response);
			
		}
    	else{
    		if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "allBranchesCompleted", 
						getMyInfo() + " parent was already updated about this branch");
			}
    	}
	}

	/**
	 *  @see com.ibm.ws.sip.container.proxy.BranchManager#setupTimeOut()
	 */
    void setupTimeOut() {
    	if(_state == PB_STATE_REDIRECTING_REQUEST || ((_proxy.getParallel() && _shouldStartTimer))){
    		setupProxyBranchTimer();
    	}
	}

	/**
	 * Returns an URI used when this ProxyBranch created.
	 * @return
	 */
    public URI getUri() {
		return _uri;
	}

   /**
    *  @see com.ibm.ws.sip.container.proxy.BranchManager#getStatefullProxy()
    */
    protected Proxy getStatefullProxy(){
    	return getProxy();
    }
    
    /**
     *  @see com.ibm.ws.sip.container.proxy.BranchManager#getIsRecordRoute()
     */
    protected boolean getIsRecordRoute(){
    	return _isRecordRoute;
    }
    
   /**
    *  @see com.ibm.ws.sip.container.proxy.BranchManager#getIsParallel()
    */
    protected boolean getIsParallel(){
		return _proxy.getParallel();
	}

	/**
	 * update state of this ProxyBranch
	 * to be PB_STATE_FAILED_T0_BE_SENT
	 */
    public void failedToSend() {
		_state = PB_STATE_FAILED_T0_BE_SENT;
	}

	/**
	 * Helper method which sets the _isStarted member
	 * to true
	 *
	 */
    public void setStarted() {
		_isStarted = true;
	}

	/**
	 *  @see com.ibm.ws.sip.container.proxy.BranchManager#getPathAddress()
	 */
    SipURI getPathAddress(String transport) {
		return _proxy.getPathAddress(transport);
	}

	/**
	 *  @see com.ibm.ws.sip.container.proxy.BranchManager#getAddToPathValue()
	 */
    boolean getAddToPathValue() {
		return getAddToPath();
	}

    /**
     * This method is not currently supported. We will leave it implemented for now but it
     * should never be called until the JSR 289 interface is exposed.
     * 
     * @see javax.servlet.sip.Proxy#setOutboundInterface()
     * @author mordechai
     */
	public void setOutboundInterface(InetSocketAddress address) throws IllegalStateException, IllegalArgumentException, NullPointerException 
	{
        if (address != null) {
        	//	Here we need to set the record route URI back to null to insure that
        	//	it will be recalculated.
        	_recordRouteURI = null;

        	TransactionUserWrapper tu = _originalReq.getTransactionUser();
			if(!tu.isValid() || tu.isInvalidating()){
				throw new IllegalStateException("Session is already invalidated");
			}
			
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setOutboundInterface", "Attempting to set outbound interface to: " + address);
			}

			boolean isSet = false;
			int index = SipProxyInfo.getInstance().getIndexOfIface(address, "udp");
			if (index != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
				isSet = true;
				_preferedOutBoundIfaceIdxUDP = index;
			}
			
			index = SipProxyInfo.getInstance().getIndexOfIface(address, "tcp");
			if (index != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
				isSet = true;
				_preferedOutBoundIfaceIdxTCP = index;
			}
			
			index = SipProxyInfo.getInstance().getIndexOfIface(address, "tls");
			if (index != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
				isSet = true;
				_preferedOutBoundIfaceIdxTLS = index;
			}
			
			if (!isSet)
			{
				throw new IllegalArgumentException("address: " + address + " is not listed as allowed outbound interface.");
			}
		}
        else
			throw new NullPointerException("Invalid address = null");
	}
	
	/**
	 * @see javax.servlet.sip.ProxyBranch#setOutboundInterface(java.net.InetAddress)
	 */
	public void setOutboundInterface(InetAddress address) throws IllegalStateException, IllegalArgumentException, NullPointerException
	{
        if (address != null) {
        	
        	//	Here we need to set the record route URI back to null to insure that
        	//	it will be recalculated.
        	_recordRouteURI = null;
        	
        	TransactionUserWrapper tu = _originalReq.getTransactionUser();
			if(!tu.isValid()){
				throw new IllegalStateException("Session is already invalidated");
			}
			
			
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setOutboundInterface", "Attempting to set outbound interface to: " + address);
			}

			boolean isSet = false;
			int index = SipProxyInfo.getInstance().getIndexOfIface(address, "udp");
			if (index != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
				isSet = true;
				_preferedOutBoundIfaceIdxUDP = index;
			}
			
			index = SipProxyInfo.getInstance().getIndexOfIface(address, "tcp");
			if (index != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
				isSet = true;
				_preferedOutBoundIfaceIdxTCP = index;
			}
			
			index = SipProxyInfo.getInstance().getIndexOfIface(address, "tls");
			if (index != SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED) {
				isSet = true;
				_preferedOutBoundIfaceIdxTLS = index;
			}
			
			if (!isSet)
			{
				throw new IllegalArgumentException("address:" + address + " is not listed as allowed outbound interface.");
			}
		}
        else
			throw new NullPointerException("Invalid address = null");
	}

	/**
	 * 
	 */
	public int getPreferedOutboundIface(String transport) {
		int returnValue = SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED;
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "getPreferedOutboundIface", "transport = " + transport);
		}
		
		if (SIPTransactionStack.instance().getConfiguration().getSentByHost() != null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getPreferedOutboundIface", "Return OUTBOUND_INTERFACE_NOT_DEFINED since the sentByHost property is set");
			}
			return SipConstants.OUTBOUND_INTERFACE_NOT_DEFINED;
		}

		if ((transport == null) || (transport.equalsIgnoreCase("udp") == true))
		{
			returnValue = _preferedOutBoundIfaceIdxUDP;
		}
		else if (transport.equalsIgnoreCase("tcp") == true)
		{
			returnValue = _preferedOutBoundIfaceIdxTCP;
		}
		else if (transport.equalsIgnoreCase("tls") == true)
		{
			returnValue = _preferedOutBoundIfaceIdxTLS;
		}
		
		return returnValue;
	}
	
	@Override
	protected boolean proxyBranchExists(URI nextHop) {
		return _proxy.proxyBranchExists(nextHop);
	}

	/**
	 * @return the _isVirtual
	 */
	public boolean isVirtual() {
		return _isVirtual;
	}

	/**
	 * @see javax.servlet.sip.ProxyBranch#getPathURI() 
	 */
	public SipURI getPathURI() {
		return _proxy.getPathURI();
	}

	/**
	 * @see javax.servlet.sip.ProxyBranch#getRecordRoute() 
	 */	
	public boolean getRecordRoute() {
		return _isRecordRoute;
	}

	/**
	 * @see javax.servlet.sip.ProxyBranch#setRecordRoute(boolean) 
	 */	
	public void setRecordRoute(boolean includeRecordRoute) {
		_isRecordRoute = includeRecordRoute;
	}

	/**
	 * @see com.ibm.ws.sip.container.proxy.ProxyParent#getParent()
	 */
	public ProxyParent getParent() {
		return _parent;
	}
	
    /**
     * @return SipServletRequest
     */
    public SipServletRequest getRequest()
    {
        return _request;
    }

	@Override
	public void addTransaction(String method) {
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "addTransaction", " ProxyBranch = "+ getMyInfo());
		 }
		
		if(method != null && method.equals(Request.CANCEL)){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "addTransaction",
						"Do NOT count transaction for CANCEL request ProxyBranch " + getMyInfo());
			}
			return;
		}
		
		if(_mainAssociatedTu != null){
			//When we already have associated 
			_mainAssociatedTu.addTransaction(method);
		}
		else{
			_request.getTransactionUser().addTransaction(method);
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "addTransaction", " ProxyBranch = "+ getMyInfo());
		 }
	}
	
	@Override
	public void removeTransaction(String method) {
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "removeTransaction", "From method = "+ method+ " ProxyBranch = "+ getMyInfo());
		 }
		
		if( method != null && method.equals(Request.CANCEL)){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "removeTransaction",
						"Do NOT count transaction for CANCEL request ProxyBranch " + getMyInfo());
			}
			return;
		}
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "removeTransaction",
					"Remove transaction from all related TU for ProxyBranch = " + getMyInfo());
		}
		
		TransactionUserWrapper origTU = _originalReq.getTransactionUser();
		
		
		// Transaction was ended and removed from TransactionTable.
		// Notify all related TUs.
		// We have related TUs in case of downstream proxy response.
		for (TransactionUserWrapper tuToClose : relatedTUs) {
			if (!_removedFromOrigTU && tuToClose == origTU) {
				_removedFromOrigTU = true;
			}
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "removeTransaction",
						"Remove transaction from all related TUs" + tuToClose);
			}
			tuToClose.removeTransaction(method);
			
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "removeTransaction",
						"Remove transaction from TU " + tuToClose.getId());
			}
		}

		//When this ProxyBranch was related to DerivedTU - as a second branch that received response with ToTag
	    //we should remove it's transaction from original TU since Original TU counted this transaction
		// as ClientTransaction when ProxyBranch was created.
		if(!_removedFromOrigTU){
			origTU.removeTransaction(method);
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "removeTransaction",
						"Remove transaction from Original TU as well. OrigTU = " +origTU.getId());
			}
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "removeTransaction", " ProxyBranch = "+ getMyInfo());
		 }
		
	}
	/**
	 * Set this flag to true. Meaning that this CT was removed from original TU responsibility.
	 */
	public void setRemoveFromOriginalTU() {
		this._removedFromOrigTU = true;
	}
	
	/**
	 * Return true if this branch was already canceled.
	 * @return
	 */
	public boolean isCancelled() {
		return _isCancelled;
	}
	
}
