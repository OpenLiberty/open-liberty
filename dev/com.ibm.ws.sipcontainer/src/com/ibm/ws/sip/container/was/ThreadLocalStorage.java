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
package com.ibm.ws.sip.container.was;

import java.util.Vector;

import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.appqueue.NativeMessageDispatchingHandler;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.proxy.ProxyBranchImpl;
import com.ibm.ws.sip.container.resolver.SipURILookupCallbackImpl;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.tu.TUKey;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.was.message.SipMessage;

/**
 * @author Nitzan, May 27 2005
 * Stores variables staticly in a thread scope. 
 * @update Moti: 28 JAn 2009 updated TUKey doc
 */
public class ThreadLocalStorage 
{
	private static final LogMgr c_logger = Log.get(ThreadLocalStorage.class);
	private static ThreadLocal<SipMessage> _sipMessage = new ThreadLocal<SipMessage>();
	
	private static ThreadLocal<String> _sipResponseToHeader = new ThreadLocal<String>();
	
	private static ThreadLocal<Integer> _queueId = new ThreadLocal<Integer>();
	
	/**
	 * Used to optimize DomainResolver DNS queries
	 */
	private static ThreadLocal<SipURILookupCallbackImpl> _uriLookupCallBack = new ThreadLocal<SipURILookupCallbackImpl>();
	
	/** 
	 * temporary TU Key Object instead of creating one for each compare
	 * or search operations we do in the SIP container.
	 * (insert is a different story). 
	 * Beware ! not to use this variable inside loops for creating lists
	 * of TUKey as this temporary TUKey is being overriden by many methods and
	 * classes.
	 * If you need more than one TUKey at a time use TUKey.clone() to
	 * save intermediate results;
	 */
	private static ThreadLocal<TUKey> _tuKey = new ThreadLocal<TUKey>();
	
	private static ThreadLocal<Vector<TransactionUserWrapper>>  _tuToinvalidate  = new ThreadLocal<Vector<TransactionUserWrapper>>();
	
	/**
	 * Reference to the SIP Application Session (SAS). 
	 */
	private static ThreadLocal<SipApplicationSession> _appSession = new ThreadLocal<SipApplicationSession>();
	
	/**
	 * Reference to the TransactionUserWrapper. 
	 */
	private static ThreadLocal<TransactionUserWrapper> _tuWrapper = new ThreadLocal<TransactionUserWrapper>();
	
	/**
	 * Reference to the branch being processed. 
	 */
	private static ThreadLocal<ProxyBranchImpl> _curBranch = new ThreadLocal<ProxyBranchImpl>();

	/**
	 * Stores message
	 * @param req Request to store
	 */
	public static void setTuForInvalidate( TransactionUserWrapper tu)
	{
		traceThreadModelValidity(tu);
		Vector<TransactionUserWrapper> tuList = _tuToinvalidate.get();
		if(tuList == null){
			tuList = new Vector<TransactionUserWrapper>();
			_tuToinvalidate.set(tuList);
		}
		tuList.add(tu);
	}
	
	/**
	 * The only purpose of this method is to perform tracing in the case a problem where the setTuForInvalidate method was called
	 * from a non-SIP thread or from a wrong SIP thread.
	 */
	private static void traceThreadModelValidity(TransactionUserWrapper tu) {
		
		if (c_logger.isTraceFailureEnabled()) {
			try {
				int tuQueueIndex = SipApplicationSessionImpl.extractAppSessionCounter(tu.getApplicationId()) % NativeMessageDispatchingHandler.s_dispatchers;
				
				if(ThreadLocalStorage.getQueueId() == null || ! ThreadLocalStorage.getQueueId().equals(tuQueueIndex)) {
					c_logger.traceFailure(tu, "traceThreadModelValidity", "Threading model violation! " +
								"current thread queue_id = " + ThreadLocalStorage.getQueueId() +
								" violation on access to SAS_id = " + tu.getApplicationId() +
								" violated SAS_id queue index = " + tuQueueIndex +
								"The TU is " + tu.getId()
								, new Throwable());
					
					if(tu.getTuImpl() != null) {
						c_logger.traceFailure(tu, "traceThreadModelValidity", "Violated SAS's message = " + tu.getTuImpl().getSipMessage());
					}
					else {
						c_logger.traceFailure(tu, "traceThreadModelValidity", "TUImpl is null");
					}
				}
				
			} catch (Exception e) {
				if (c_logger.isTraceFailureEnabled()) {
					c_logger.traceFailure(tu, "traceThreadModelValidity", "unexpected exception", e);
				}
			}
		}
	}
	
	/**
	 * Stores message
	 * @param req Request to store
	 */
	public static void cleanTuForInvalidate()
	{
		Vector<TransactionUserWrapper> tuList = _tuToinvalidate.get();
		if(tuList != null){
			tuList.clear();
		}	
		//Moti: added nullify so we don't hold reference to big sip messages...
		setSipMessage(null);
		setSipResponseToHeader(null);
	}
	
	/**
	 * Returns stored message
	 * @param req Request to store
	 */
	public static Vector<TransactionUserWrapper> getTuForInvalidate()
	{
		return _tuToinvalidate.get();
	}
	
	/**
	 * Stores message
	 * @param req Request to store
	 */
	public static void setSipMessage( SipMessage msg)
	{
		_sipMessage.set( msg);
	}
	
	/**
	 * Returns stored message
	 * @param req Request to store
	 */
	public static SipMessage getSipMessage()
	{
		return _sipMessage.get();
	}
	
	/**
	 * Stores queue id
	 * @param queueId to store
	 */
	public static void setQueueId(Integer queueId)
	{
		_queueId.set(queueId);
	}
	
	/**
	 * Returns stored queue id
	 */
	public static Integer getQueueId()
	{
		return _queueId.get();
	}
	
	/**
	 * Stores TUKey
	 * @param req Request to store
	 */
	public static void setTUKey( TUKey key)
	{
		_tuKey.set( key);
	}
	
	/**
	 * Returns stored TUKey
	 * @param stored TUKey 
	 */
	public static TUKey getTUKey()
	{ //TODO Liberty - DO NOT USE THIS KEY. cREATE NEW ONE WHEN NEEDED
		TUKey key = _tuKey.get();
		if(key == null){
			setTUKey(new TUKey());
		}
		return _tuKey.get();
	}
	
	/**
	 * Get stored SipServletRequest
	 * @return SipServletRequest object
	 */
	public static SipServletRequest getSipServletRequest()
	{
		SipMessage message = _sipMessage.get();
		if(message == null){
			return null;
		}
		return message.getRequest();
	}
	
	/**
	 * Get stored SipServletResponse
	 * @return SipServletResponse object
	 */
	public static SipServletResponse getSipServletResponse()
	{
		SipMessage message = _sipMessage.get();
		if(message == null){
			return null;
		}
		return message.getResponse();
	}
	
	/**
	 * Get stored SipAppDesc
	 * @return SipAppDesc object
	 */
	public static SipAppDesc getSipAppDesc()
	{
		SipMessage message = _sipMessage.get();
		if(message == null){
			return null;
		}
		return message.getSipAppDesc();
	}
	
	/**
	 * get the Sip ServletName 
	 * @return
	 */
	public static String getSipServletName() {
		SipMessage message = _sipMessage.get();
		if(message == null){
			return null;
		}
		return message.getServletName();
	}
	
	/**
     * @param responseToHeader The _sipResponseToHeader to set.
     */
    public static void setSipResponseToHeader( String responseToHeader) {
        _sipResponseToHeader.set(responseToHeader);
    }
    
    /**
     * @return Returns the _sipResponseToHeader.
     */
    public static String getSipResponseToHeader() {
        return _sipResponseToHeader.get();
    }

    /**
     * Return the SipURILookupCallbackImpl cached object.
     */
    public static SipURILookupCallbackImpl getURILookupCallback() {
    	SipURILookupCallbackImpl callback = _uriLookupCallBack.get();
    	if (callback == null) {
    		callback = new SipURILookupCallbackImpl();
    		_uriLookupCallBack.set(callback);
    	}
    	return callback;
    }
    
    /**
     * @return the SIP Application Session.
     */
    public static SipApplicationSession getApplicationSession() {
    	return _appSession.get();
    }
    
    /**
     * Sets a new value to the SIP Application Session.
     * 
     * @param sas the new SIP Application Session to set.
     */
    public static void setApplicationSession(SipApplicationSession sas) {
    	_appSession.set(sas);
    }   
    
    /**
     * @return the TransactionUserWrapper.
     */
    public static TransactionUserWrapper getTuWrapper() {
    	return _tuWrapper.get();
    }
    
    /**
     * Sets a new value to the TransactionUserWrapper.
     * 
     * @param sas the new TransactionUserWrapper to set.
     */
    public static void setTuWrapper(TransactionUserWrapper tuWrapper) {
    	_tuWrapper.set(tuWrapper);
    }

	/**
	 * The proxy branch that is processed
	 */
	public static ProxyBranchImpl getCurrentBranch() {
		return _curBranch.get();
	}

	/**
	 * Set the branch that is processed enter null if it's not in branch context
	 */
	public static void setCurrentBranch(ProxyBranchImpl curBranch) {
		_curBranch.set(curBranch);
	}   
}
