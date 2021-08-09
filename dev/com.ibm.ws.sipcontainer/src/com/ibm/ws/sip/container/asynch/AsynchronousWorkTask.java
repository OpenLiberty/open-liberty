/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.asynch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.NoSuchElementException;

import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.websphere.sip.AsynchronousWork;
import com.ibm.websphere.sip.AsynchronousWorkListener;
import com.ibm.ws.jain.protocol.ip.sip.message.SipResponseCodes;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.appqueue.MessageDispatcher;
import com.ibm.ws.sip.container.appqueue.NativeMessageDispatchingHandler;
import com.ibm.ws.sip.container.events.ContextEstablisher;
import com.ibm.ws.sip.container.failover.repository.SessionRepository;
import com.ibm.ws.sip.container.osgi.AsynchronousWorkDispatcher;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.router.tasks.RoutedTask;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipServletsFactoryImpl;
import com.ibm.ws.sip.container.was.ThreadLocalStorage;
//TODO Liberty the following imports don't longer exist as we don't support HA in Liberty
//import com.ibm.ws.sip.container.failover.FailoverMgr;
//import com.ibm.ws.sip.container.failover.FailoverMgrLoader;
//TODO Liberty probably have to delete this include, as we don't support HA in Liberty
//import com.ibm.ws.sip.hamanagment.util.SipClusterUtil;

/**
 * This class is used for asynchronous invocation to execute a runnable object on the right place.
 * 
 * An application that extends this class should implement <code>doAsyncTask</code> abstract method that returns a <code>Serializable</code> object. 
 * This method will be invoked on the right server and an application performs the asynch work here.
 *
 * In order to execute a work asynchronously, an application creates a AsynchronousWorkListener object and passes it calling dispatch method.
 * When the work is completed, the AsynchronousWorkListener::onCompleted is invoked
 * 
 * @author Galina Rubinshtein, Dec 2008
 */
public class AsynchronousWorkTask extends RoutedTask implements AsynchronousWorkDispatcher {

	private AsynchronousWork _appAsynchWorkObject;
	
	private AsynchronousWorkListener _appAsynchWorkListener;

    /**
	 * Constant used in encoded URI to identify a specific application session.
	 * The information will be added as a parameter to the URI and its value
	 * will be mapped to a SessionID of a specific SIP Application Session instance
	 */
	public final static String ENCODED_APP_SESSION_ID = "ibmappid";
	
	/**
	 * SIP method used to pass the asynch work request to another server
	 */
	public final static String SIP_METHOD = "ASYNWORK";
	
	/**
	 * Content type used to send the asynch work as a body
	 */
	public final static String CONTENT_TYPE = "asynchwork/type";
		
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(AsynchronousWorkTask.class);
    
    private String _appSessionID;
    
    private AsynchronousWorkListenerWrapper _listenerWrapper = null;
    
    //this object is used for the wait for response mechanism 
    private Object _lockObj = new Object();
    
    //indicate if response for this task was already received 
    private boolean _isResponseRecived = false;
    
    //indicate if we are waiting for response to be received 
    private boolean _isWaitForResponse = false;
    
    //the response of the async work task
    private Object _result = null;
    
    //the application class loader, used for de-serialization of the response return object  
    private ClassLoader _cl;
    
	/**
     * Constructor 
     * 
     * @param appSessionId
     */
    public AsynchronousWorkTask(String appSessionId){
    	super();
    	_appSessionID = appSessionId;  
    	
    	if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "AsynchronousWorkTask",
					"Creating async work task for sip application id: " + _appSessionID);
		}
    	
    	try {
			_index = SipApplicationSessionImpl
					.extractAppSessionCounter(_appSessionID);
		} catch (NoSuchElementException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "AsynchronousWorkTask",
						"AppSessionId is not valid, appSessionId: " + _appSessionID);
			}
			
			throw new IllegalArgumentException("AppSessionId is not valid, appSessionId: " + _appSessionID);
		}
		
		//get the application class loader from the current thread
    	_cl = Thread.currentThread().getContextClassLoader();
    }
	
    /**
	 * @see com.ibm.ws.sip.container.util.Queueable#getQueueIndex()
	 */
	public int getQueueIndex() {
		if (_index < 0){
			throw new RuntimeException("Dispatching error, transaction-user not found!");
		}		
		return _index;
	}
	
	public int priority() {
		return PRIORITY_NORMAL;
	}
	
	@Override
	public String toString() {
		return "AsynchronousWorkTask user task for application ID: " + _appSessionID;
	}
	
	/**
	 * wait until the response is received, this method will cause the current thread to be locked
	 * until the async work task is finished
	 * 
	 * @param time - the maximum time to wait in milliseconds, 0 is for waiting forever 
	 * @return Object - the result of the async task or null if it was not finished yet
	 */
	public Object waitForResponse(long time){
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "waitForResponse", time);
		}
		
		synchronized (_lockObj) {
			if (! _isResponseRecived){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "waitForResponse",
							"going to wait for respone, applicationid=" + _appSessionID);
				}
				
				_isWaitForResponse = true;
				try {
					_lockObj.wait(time);
				} catch (InterruptedException e) {
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "waitForResponse",
								"Async work was interrupted, applicationid=" + _appSessionID);
					}
				}
				
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "waitForResponse",
							"respone was received thread is notified, applicationid=" + _appSessionID);
				}
			}
		}
		
		if(c_logger.isTraceEntryExitEnabled() ){
    		c_logger.traceExit(this,"waitForResponse");
    	}
		
		return _result;
	}
	
	/**
	 * notify the waiting thread that used the waitForResponse() method
	 * 
	 */
	private void notifyWaitThread() {
		if(c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "notifyWaitThread");
		}
		
		synchronized (_lockObj) {
			_isResponseRecived = true;
			
			if (_isWaitForResponse){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "notifyWaitThread",
							"respone was received going to notify waiting thread, applicationid=" + _appSessionID);
				}
				
				_isWaitForResponse = false;
				_lockObj.notify();
			}
		}
		
		if(c_logger.isTraceEntryExitEnabled() ){
    		c_logger.traceExit(this,"notifyWaitThread");
    	}
	}

	/**
	 * Dispatch the asynch work task 
	 */
	public void dispatch(AsynchronousWork obj, AsynchronousWorkListener listener) {
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "dispatch", new Object[]{obj, listener});
		}
		
		_appAsynchWorkObject = obj;
		_appAsynchWorkListener = listener;

		//get the current queue id of the running thread,
		Integer currentQueueId = ThreadLocalStorage.getQueueId();
		
		SipApplicationSession sipAppSession = SipApplicationSessionImpl.getAppSession(_appSessionID);
		
		if(sipAppSession == null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("dispatch: sipAppSession is null, sending Async work remotely");
			}
			
			if (currentQueueId != null){
				if (! (_appAsynchWorkListener instanceof AsynchronousWorkTaskListener)){
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "dispatch", "This is a custom async listener, Wrapping the async listener for dispaching");
					}
					_listenerWrapper = new AsynchronousWorkListenerWrapper(_appAsynchWorkListener, currentQueueId.intValue());
				}
			}
			
			// Create a new message to send to the sip proxy.
			// 		ASYNWORK sip:example@ibm.com;ibmappid=_appSessionID
			// The SIP proxy will transport the message to the right server where the session is managed.
			// So the MessageDispatcher.dispatchRoutedTask(this); will be invoked on the right server.

			int reason = sendAsynchronousWorkRequest();
			if (reason != SipResponseCodes.OK) {
				sendFailedResponse(reason);
			}
		}
		else {	
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "dispatch",
						"Async work will be dispached to local server, SAS was found localy, applicationid=" + _appSessionID);
			}
			
			//check if we run on a container thread
			if (currentQueueId != null){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "dispatch",
							"Current thread is running from Sip container queue, queueid=" + currentQueueId.intValue());
				}
				
				//calculate the queue id of the async work
				int asyncQueueId = this.getQueueIndex() % NativeMessageDispatchingHandler.s_dispatchers;
				
				if (asyncQueueId == currentQueueId.intValue()){
					//the async work should run on the same queue as the current thread, run it now
					setForDispatching(false);
					
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "dispatch",
								"Async work will run on current thread, asyncQueueId=" + asyncQueueId);
					}
				}else{
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "dispatch",
								"Async work will be dispached to a different SIP queue than the current thread queue, asyncQueueId=" + asyncQueueId);
					}
					
					if (! (_appAsynchWorkListener instanceof AsynchronousWorkTaskListener)){
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(this, "dispatch", "This is a custom async listener, Wrapping the async listener for dispaching");
						}
						_listenerWrapper = new AsynchronousWorkListenerWrapper(_appAsynchWorkListener, currentQueueId.intValue());
					}
				}
			}
			
			MessageDispatcher.dispatchRoutedTask(this);
		}
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "dispatch");
		}
		
	}
	
	/**
	 * Send successful response to the application listener
	 * 
	 * @param result
	 */
	public void sendCompletedResponse(Serializable result) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "sendCompletedResponse", result);
		}
		_result = result;
		
		//notify waiting thread if needed
		notifyWaitThread();
		
		if (_appAsynchWorkListener != null) {
			if(_listenerWrapper != null) {
				//when _listenerWrapper is not null we need to dispatch the listener work to the correct queue.
				//this case will only happen if:
				//	1. the async work was dispatched locally
				//  2. the async work was dispatched to a different queue than the caller queue
				_listenerWrapper.setResult(result);
				_listenerWrapper.setMode(AsynchronousWorkListenerWrapper.ON_COMPLETE);
				
				MessageDispatcher.dispatchRoutedTask(_listenerWrapper);
				
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "dispatch",
							"Async Listener was dispached to the correct queue=" + _listenerWrapper.getQueueIndex());
				}
			}else{
				_appAsynchWorkListener.onCompleted(result);
			}
		}
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "sendCompletedResponse");
		}
	}
	
	/**
	 * Send a failure response to the application listener
	 * 
	 * @param result
	 */
	public void sendFailedResponse(int reason) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "sendFailedResponse", reason);
		}
		_result = reason;
		
		//notify waiting thread if needed
		notifyWaitThread();
		
		if (_appAsynchWorkListener != null) {
			if(_listenerWrapper != null) {
				//when _listenerWrapper is not null we need to dispatch the listener work to the correct queue.
				//this case will only happen if:
				//	1. the async work was dispatched locally
				//  2. the async work was dispatched to a different queue than the caller queue
				_listenerWrapper.setReason(reason);
				_listenerWrapper.setMode(AsynchronousWorkListenerWrapper.ON_FAIL);
			
				MessageDispatcher.dispatchRoutedTask(_listenerWrapper);
				
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "dispatch",
							"Async Listener was dispached to the correct queue=" + _listenerWrapper.getQueueIndex());
				}
			}else{
				_appAsynchWorkListener.onFailed(reason, SipResponseCodes.getResponseCodeText(reason));
			}
		}
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "sendFailedResponse");
		}
	}
	
	/**
	 * Returns the SipApplicationSession object.
	 */
	public SipApplicationSession getSipApplicationSession() {
		return SessionRepository.getInstance().getAppSession(_appSessionID);
	}
	
	// Override RoutedTask abstract methods
	public void doTask() {
		if (c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this,"doTask",new Object[]{});
		}
		
		try {
			SipApplicationSession appsession = getSipApplicationSession();
			ContextEstablisher contextEstablisher = null;
	        ClassLoader currentThreadClassLoader = null;
	        Serializable result = null;
	        try{
				if(appsession != null){
					if (c_logger.isTraceDebugEnabled()){
						c_logger.traceDebug("doTask switching context for async task before invoking. appsession="+appsession);
					}
					SipAppDesc sipAD = SipContainer.getInstance().getRouter().getSipApp(appsession.getApplicationName());
					contextEstablisher = sipAD.getContextEstablisher();			 
					
					if (contextEstablisher != null){
						currentThreadClassLoader = 
								contextEstablisher.getThreadCurrentClassLoader();
						contextEstablisher.establishContext();
					}else{
						if (c_logger.isTraceDebugEnabled()){
							c_logger.traceDebug("doTask couldn't switch context for async task before invoking. contextEstablisher is null. appsession="+appsession);
						}
					}
					result = _appAsynchWorkObject.doAsyncTask();
				}else{
					sendFailedResponse(SipResponseCodes.CLIENT_CALL_OR_TRANSACTION_DOES_NOT_EXIST);
				}
	        }finally{
				if (contextEstablisher != null) {
					contextEstablisher.removeContext(currentThreadClassLoader);
				}
				if (c_logger.isTraceDebugEnabled()){
					c_logger.traceDebug("doTask switching context back. appsession="+appsession);
				}
	        }
			
			if (c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("doAsyncTask: returned: " + result);
			}
			
			//a notification is sent even if the result is empty, this is for a case 
			//when a listener is waiting on a remote server and for releasing the transaction
			//object and timer of the Async request in the remote server
			if(result == null){
				sendCompletedResponse("");
			}else{
				sendCompletedResponse(result);
			}
		} catch (Throwable e) {
			//the doTask implementation failed, the error listener should be invoked
			if (c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("Application async task threw exception: " + e.getMessage() + " ,applicationId: "  + _appSessionID);
			}
			com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sip.container.asynch.AsynchronousWorkTask", "1", this);
			sendFailedResponse(SipResponseCodes.CLIENT_BAD_REQUEST);
		}
		
		if (c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(this,"doTask");
		}
	}
	
	public String getMethod() {
		return "Asynch Work Task method";
	}
	
	/**
	 * This method is to create a new SIP message for sending the work to the right server in the cluster.
	 * 
	 * @return reason
	 */
	private int sendAsynchronousWorkRequest() {
		//check if we are not in a cluster environment we dont need to sent it, just return error
		//TODO Liberty probably has to remove this condition, as we no longer support HA in Liberty
		/*if (!SipClusterUtil.isZServerInCluster()){
			if (c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("Can not send asynchronous work. appSessionId is not found, appsessionid=" + _appSessionID);
			}
			
			return SipResponseCodes.CLIENT_CALL_OR_TRANSACTION_DOES_NOT_EXIST;
		}*/
		
		String host = null;
		int port = -1;
		
		/*TODO Liberty FailoverMgr failoverMgr = FailoverMgrLoader.getMgrInstance();
		if(failoverMgr != null)
        {
            //First try to use the network dispatcher
            host = failoverMgr.getNetDispatchHost("udp"); 
            port = failoverMgr.getNetDispatchPort("udp"); 
        }*/
		if (host == null) {
			if (c_logger.isErrorEnabled()){
				c_logger.error("error.asynchwork.host.unknown",Situation.SITUATION_CONNECT_INUSE,null);
			}
			return SipResponseCodes.SERVER_INTERNAL_FAILURE;
		}
		
		AsynchronousWorkTasksManager manager = AsynchronousWorkTasksManager.instance();
		//get the asynch SAS
		SipApplicationSessionImpl appSession = manager.getAsynchSipApp();
    	
		try {
    		if(c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("sendAsynchronousWorkRequest: Sending Async work to remote server");
			}
    		SipServletRequestImpl request = 
    			(SipServletRequestImpl) SipServletsFactoryImpl.getInstance().
    				createRequest(appSession, SIP_METHOD, "sip:server1@ibm.com", "sip:server2@ibm.com");
    		
    		request.setContent(dataToByteArray(_appAsynchWorkObject), CONTENT_TYPE);
    		
    		SipURI requestURI = SipServletsFactoryImpl.getInstance().createSipURI("task", host);
    		requestURI.setPort(port);
    		requestURI.setTransportParam("udp");    		
    		encodeURI(requestURI, _appSessionID);
    		request.setRequestURI(requestURI);   
    		
    		// Add this task to the AsynchronousWorkTaskManager for response
    		manager.putAsynchronousWorkTask(request.getCallId(), this);
    		
    		// Now send the request
    		request.send();
    		
		} catch (ServletParseException e) {
			if (c_logger.isErrorEnabled()){
				c_logger.error("error.exception.spe",Situation.SITUATION_REQUEST,null,e);        	
			}
			return SipResponseCodes.SERVER_INTERNAL_FAILURE;
		} catch (UnsupportedEncodingException e) {
			if (c_logger.isErrorEnabled()){
				c_logger.error("error.exception.uee",Situation.SITUATION_REQUEST,null,e);        	
			}
			return SipResponseCodes.SERVER_INTERNAL_FAILURE;
		} catch (IllegalArgumentException e) {
			if (c_logger.isErrorEnabled()){
				c_logger.error("error.exception.iae",Situation.SITUATION_REQUEST,null,e);        	
			}
			return SipResponseCodes.SERVER_INTERNAL_FAILURE;
		} catch (IOException e) {
			if (c_logger.isErrorEnabled()){
				c_logger.error("error.exception.io",Situation.SITUATION_REQUEST,null,e);        	
			}
			return SipResponseCodes.SERVER_INTERNAL_FAILURE;
		}
		return SipResponseCodes.OK;
	}
	
	/**
	 * @see javax.servlet.sip.SipApplicationSession#encodeURI(javax.servlet.sip.URI)
	 */
	private void encodeURI(URI uri, String sessionId) {
		if (uri.isSipURI()) {
			((SipURI) uri).setParameter(ENCODED_APP_SESSION_ID, sessionId);
		}
		else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "encodeURI",
						"Can not encode URI, Not a SIP URI " + uri);
			}
			throw new IllegalArgumentException(
			"Not a SIP a URI, Can not encode session information");
		}		
	}
    
	/**
	 * 
	 * @param data
	 * @return
	 * @throws IOException
	 */
	private byte[] dataToByteArray(Object obj) throws IOException{
		if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(this,"dataToByteArray",new Object[]{obj});
    	}
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bout);
		out.writeObject(obj);
		byte[] bits = bout.toByteArray();
		if (c_logger.isTraceEntryExitEnabled()){
    		c_logger.traceExit(this,"dataToByteArray",bits);
    	}
		return bits;
	}	
	
	/**
	 * 
	 * @return the application class loader
	 */
	public ClassLoader getCl() {
		return _cl;
	}
	
	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getServiceSynchronizer()
	 */
	public Object getServiceSynchronizer(){
		SipApplicationSessionImpl sipAppSession = (SipApplicationSessionImpl)getSipApplicationSession();
		return sipAppSession.getServiceSynchronizer();
	}
	
	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getApplicationSession()
	 */
	@Override
	public SipApplicationSession getApplicationSession() {
		return getSipApplicationSession();
	}
}
