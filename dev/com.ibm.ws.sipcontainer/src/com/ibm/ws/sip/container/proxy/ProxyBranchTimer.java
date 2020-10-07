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

import javax.servlet.sip.SipApplicationSession;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipSessionImplementation;
import com.ibm.ws.sip.container.timer.BaseTimer;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

/**
 * 
 * @author anat
 *
 * Class which represents the ProxyBranch timer - member of ProxyBranchImpl
 * object.
 */
public class ProxyBranchTimer extends BaseTimer{

	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(ProxyBranchTimer.class);
    /**
     * Proxy branch associated with this timer. 
     */
    private ProxyBranchImpl _branch;
    
    /**
     * Reference to the original request.
     */
    SipServletRequestImpl _origReqImp;
    
     /**
     * Construct a new sequencial timer 
     * @param proxy
     */
    public ProxyBranchTimer(ProxyBranchImpl branch) {
    	
        _branch = branch;  
        _origReqImp = (SipServletRequestImpl)_branch.getOriginalRequest();
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "ProxyBranchTimer", 
                				"Created For Branch: " + branch);
        }
        
        setQueueIndex(extractQueueIndex());
    }
    
    /**
	 * Extracts queue index from the related application session.
	 */
	protected int extractQueueIndex() {
		int result = -1;
		if(_origReqImp != null){
			TransactionUserWrapper tuImpl = _origReqImp.getTransactionUser();
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
	 *  @see com.ibm.ws.sip.container.timer.BaseTimer#invoke()
	 */
	public void run() {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "run");
		}
		_branch.proxyBranchTimeout();
		
	}

	/**
	 *  @see com.ibm.ws.sip.container.util.Queueable#getAppName()
	 */
	public String getAppName() {
		if(_origReqImp != null){
			SipApplicationSessionImpl sApp = (SipApplicationSessionImpl)_origReqImp.getApplicationSession();
			if(sApp != null && sApp.getAppDescriptor() != null) {
				return sApp.getAppDescriptor().getAppName();
			}
		}
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceEntry(this, "realted origReqImp is null...");
		}
		return null;
	}

	/**
	 *  @see com.ibm.ws.sip.container.util.Queueable#getAppIndexForPMI()
	 */
	public Integer getAppIndexForPMI() {
		if(_origReqImp != null){
			SipApplicationSessionImpl sApp = (SipApplicationSessionImpl)_origReqImp.getApplicationSession();
			if(sApp != null && sApp.getAppDescriptor() != null) {
				return sApp.getAppDescriptor().getAppIndexForPmi();
			}
		}
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceEntry(this, "realted origReqImp is null...");
		}
		return null;
	}

	/**
	 *  @see com.ibm.ws.sip.container.util.Queueable#getApplicationSession()
	 */
	public SipApplicationSession getApplicationSession() {
		if(_origReqImp != null){
			return _origReqImp.getApplicationSession();
		}
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceEntry(this, "realted origReqImp is null...");
		}
		return null;
	}

	/**
	 *  @see com.ibm.ws.sip.container.util.Queueable#getTuWrapper()
	 */
	public TransactionUserWrapper getTuWrapper() {
		if(_branch != null && _branch.isCancelled()){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceEntry(this, "Related ProxyBranch was already canceled. Timeout is irrelevant.");
			}
			return null;
		}
		if(_origReqImp != null){
			SipSessionImplementation sSession = (SipSessionImplementation)_origReqImp.getSession();
			if(sSession != null) {
				return sSession.getInternalTuWrapper();
			}
		}
		else if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceEntry(this, "realted origReqImp is null...");
		}
		return null;
	}

}
