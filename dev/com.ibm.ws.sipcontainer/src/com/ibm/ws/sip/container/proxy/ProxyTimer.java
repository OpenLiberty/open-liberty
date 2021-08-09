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
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipSessionImplementation;
import com.ibm.ws.sip.container.timer.BaseTimer;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

public class ProxyTimer extends BaseTimer{

	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(ProxyTimer.class);
       
    /**
     * Proxy branch associated with this timer. 
     */
    private StatefullProxy _proxy;
    
    /**
     * Reference to the original request.
     */
    SipServletRequestImpl _origReqImp ;

    /**
     * Construct a new sequencial timer 
     * @param proxy
     */
    public ProxyTimer(StatefullProxy proxy) {
        _proxy = proxy;  
        if(_proxy != null){
        	_origReqImp = (SipServletRequestImpl)_proxy.getOriginalRequest();
        }
        else{
        	if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceEntry(this, "realted proxy is null...");
    		}
        }
        if (c_logger.isTraceDebugEnabled()) {
            c_logger.traceDebug(this, "ProxyTimer", 
                				"Created For Branch: " + proxy);
        }
        setQueueIndex(extractQueueIndex());
    }
    
	
	 /**
     * This method might be executed multi-threaded or single threaded,
     * depends on the TasksInvoker definition 
     * @see com.ibm.ws.sip.container.events.TasksInvoker
     * @see com.ibm.ws.sip.container.SipContainer#setTimerInvoker()
     * @see java.lang.Runnable#run()
     */
    public void run() {
        if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "invoke");
		}
        PerformanceMgr perfMgr = PerformanceMgr.getInstance();
		if (perfMgr != null) {
			perfMgr.updateSipTimersInvocationsCounter();
		}
		_proxy.proxyTimeout();
		
	}
	
	/**
	 * return the time in seconds until the next timer invocation
	 * 
	 * @return
	 */
	public int getTimeRemaining() {
		long scheduled = m_nextExecution;
		long now = System.currentTimeMillis();
		int remaining = (int) (scheduled - now)/1000;

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "getTimeRemaining",
				"scheduled [" + scheduled +
				"] now [" + now +
				"] remaining [" + remaining + ']');
		}
		
		return remaining;
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
	 *  @see com.ibm.ws.sip.container.timer.BaseTimer#priority()
	 */
	public int priority() {
		// TODO Auto-generated method stub
		return 0;
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
