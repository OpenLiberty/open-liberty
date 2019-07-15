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
package com.ibm.ws.sip.container.events;

import java.util.EventListener;
import java.util.EventObject;

import javax.servlet.ServletContext;
import javax.servlet.sip.ConvergedHttpSession;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSessionActivationListener;
import javax.servlet.sip.SipApplicationSessionBindingEvent;
import javax.servlet.sip.SipApplicationSessionBindingListener;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipErrorEvent;
import javax.servlet.sip.SipErrorListener;
import javax.servlet.sip.SipServletContextEvent;
import javax.servlet.sip.SipServletListener;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSessionActivationListener;
import javax.servlet.sip.SipSessionBindingEvent;
import javax.servlet.sip.SipSessionBindingListener;
import javax.servlet.sip.SipSessionEvent;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.sip.SipApplicationSessionStateListener;
import com.ibm.websphere.sip.SipSessionStateListener;
import com.ibm.websphere.sip.resolver.DomainResolverListener;
import com.ibm.websphere.sip.resolver.events.SipURILookupErrorEvent;
import com.ibm.websphere.sip.resolver.events.SipURILookupEvent;
import com.ibm.websphere.sip.unmatchedMessages.UnmatchedMessageListener;
import com.ibm.websphere.sip.unmatchedMessages.events.UnmatchedRequestEvent;
import com.ibm.websphere.sip.unmatchedMessages.events.UnmatchedResponseEvent;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.pmi.TaskDurationMeasurer;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.servlets.SipServletMessageImpl;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipSessionImplementation;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.util.Queueable;
import com.ibm.ws.sip.container.was.HttpDestroyedEvent;
import com.ibm.ws.sip.container.was.WASHttpSessionListener;
import com.ibm.ws.sip.parser.util.ObjectPool;

/**
 * This class represent a Runnable object that calls different listeners methods 
 * in a multi-threaded manner.
 * 
 * @author Nitzan
 */
public class ListenerTask implements Queueable {
	
	/**
     * Class Logger.
     */
    private static final transient LogMgr c_logger = Log.get(ListenerTask.class);
    
    /**
     * Pool for ListenerTasks
     */
	private static ObjectPool pool = new ObjectPool(ListenerTask.class);
	
	//Task types
	public static final int APP_SESSION_ACTIVATED_TASK =1;
	public static final int APP_SESSION_PASSIVATE_TASK =2;
	public static final int SESSION_ACTIVATED_TASK =3;
	public static final int SESSION_PASSIVATE_TASK =4;
	public static final int SESSION_ATTR_ACTIVATED_TASK =5;
	public static final int SESSION_ATTR_PASSIVATE_TASK =6;
	public static final int ERROR_NO_ACK_RECEIVED =7;
	public static final int ERROR_NO_PRACK_RECEIVED =8;
	public static final int SIP_SERVLET_INITIATED =9;
	public static final int SESSION_ATTRIBUTE_BOUND = 10;
	public static final int SESSION_ATTRIBUTE_UNBOUND = 11;
	public static final int APP_SESSION_ATTRIBUTE_BOUND = 12;
	public static final int APP_SESSION_ATTRIBUTE_UNBOUND = 13;
	public static final int URI_LOOKUP_COMPLETED = 14;
	public static final int URI_LOOKUP_ERROR = 15;
	public static final int HTTP_SESSION_DESTROYED = 16;
	public static final int UNMATCHED_REQUEST_RECEIVED = 17;
	public static final int UNMATCHED_RESPONSE_RECEIVED = 18;

	
	/**
	 * The listener that will be notified on event
	 */
	private EventListener _listener;
	/**
	 * The event object that will be passed to the listener
	 */
	private EventObject _event;
	
	/**
	 * The current task identifier 
	 */
	private int _taskNum;
	
	/**
	 * SipAppDesc needed for init listener event
	 */
	private SipAppDesc _appDesc;
	
	/**
	 * default queue to send, this keeps a counter that would round robin all the app queues 
	 */
	private int _appQueueIndex = -1;
	
	/**
	 * which app queue to use
	 */
	private static int _listenerQueue = 0;
	
	/**
	 * Object that measures the duration of the task in the container queue
	 */
	private TaskDurationMeasurer _sipContainerQueueDuration= null;
	
	/**
	 * Object that measures the duration of the task in the application code
	 */
	private TaskDurationMeasurer _sipContainerApplicationCodeDuration= null;
	
	/**
	 * Use this method to initialize the task data before dispatching
	 * @param listener
	 * @param event
	 * @param taskNum
	 */
	public void init( EventListener listener, 
					  EventObject event,
					  int taskNum){
		_listener = listener;
		_event = event;
		_taskNum = taskNum;
	}
	
	
	/**
	 * Use this method to initialize the task data before dispatching
	 * @param listener
	 * @param event
	 * @param taskNum
	 */
	public void init( EventListener listener, 
					  EventObject event,
					  int taskNum,
					  SipAppDesc appDesc,
					  int appQueueIndex){
		init(listener, event, taskNum);
		this._appDesc = appDesc;
		this._appQueueIndex = appQueueIndex;
	}	
	
	/**
	 * Get a ListenerTask from pool
	 * @return
	 */
	public static ListenerTask getAvailableInstance(){ 
		return (ListenerTask)pool.get(); 
	}
	
	/**
	 * This method will be executed by one of the threads in the thread pool,
	 * that the task will be dispatched to.  
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceEntry(this, "run", new Object[]{_listener, _event, new Integer(_taskNum)});
    	}
		EventListener listener = _listener; 
		EventObject event = _event;
		int task = _taskNum;
		SipAppDesc appDesc = _appDesc;

		recycle();
		
		runTask(listener, event, task, appDesc);
		
		if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceExit(this, "run");
    	}		
	}
	
	
	private static void runTask(EventListener listener, EventObject event,
								int task, SipAppDesc appDesc) {
		//We are now establishing the application context for the thread that will invoke the 
		//listener.
		ContextEstablisher contextEstablisher = null;
		if( event.getSource() instanceof SipApplicationSession){ 
			contextEstablisher = ((SipApplicationSessionImpl)event.getSource()).getAppDescriptor().getContextEstablisher();
		}else if( event.getSource() instanceof SipSessionImplementation){
			SipApplicationSessionImpl as = (SipApplicationSessionImpl)((SipSessionImplementation)event.getSource()).getApplicationSession();
			contextEstablisher = as.getAppDescriptor().getContextEstablisher();
		}else if ( event.getSource() instanceof ServletContext 
				|| event instanceof UnmatchedRequestEvent 
				|| event instanceof UnmatchedResponseEvent){
			contextEstablisher = appDesc.getContextEstablisher();
		}else if ( event.getSource() instanceof ConvergedHttpSession) {
			ConvergedHttpSession session = (ConvergedHttpSession)event.getSource();
			SipApplicationSessionImpl as = (SipApplicationSessionImpl)session.getApplicationSession();
			contextEstablisher = as.getAppDescriptor().getContextEstablisher();
		}else{
			SipApplicationSessionImpl as = (SipApplicationSessionImpl)((SipServletRequestImpl)event.getSource()).getApplicationSession();
			contextEstablisher = as.getAppDescriptor().getContextEstablisher();
		}
		
		ClassLoader currentThreadClassLoader = Thread.currentThread().getContextClassLoader();
		
		try{
			if(contextEstablisher != null){
				contextEstablisher.establishContext(listener.getClass().getClassLoader());
			}
			
			switch(task){
				case APP_SESSION_ACTIVATED_TASK:
					if (listener instanceof SipApplicationSessionStateListener){
						((SipApplicationSessionStateListener)listener).sessionDidActivate((SipApplicationSessionEvent)event);
					}else{
						((SipApplicationSessionActivationListener)listener).sessionDidActivate((SipApplicationSessionEvent)event);
					}
					break;
				case APP_SESSION_PASSIVATE_TASK:
					if (listener instanceof SipApplicationSessionStateListener){
						((SipApplicationSessionStateListener)listener).sessionWillPassivate((SipApplicationSessionEvent)event);
					}else{
						((SipApplicationSessionActivationListener)listener).sessionWillPassivate((SipApplicationSessionEvent)event);
					}
					break;
				case SESSION_ACTIVATED_TASK:
					((SipSessionStateListener)listener).sessionDidActivate((SipSessionEvent)event);
					break;
				case SESSION_PASSIVATE_TASK:
					((SipSessionStateListener)listener).sessionWillPassivate((SipSessionEvent)event);
					break;
				case SESSION_ATTR_ACTIVATED_TASK:
					((SipSessionActivationListener)listener).sessionDidActivate((SipSessionEvent)event);
					break;
				case SESSION_ATTR_PASSIVATE_TASK:
					((SipSessionActivationListener)listener).sessionWillPassivate((SipSessionEvent)event);
					break;
				case ERROR_NO_ACK_RECEIVED:
					((SipErrorListener)listener).noAckReceived((SipErrorEvent)event);
					break;
				case ERROR_NO_PRACK_RECEIVED:
					((SipErrorListener)listener).noPrackReceived((SipErrorEvent)event);
					break;
				case SIP_SERVLET_INITIATED:
					((SipServletListener)listener).servletInitialized((SipServletContextEvent)event);
					break;
				case SESSION_ATTRIBUTE_BOUND:
					((SipSessionBindingListener) listener).valueBound((SipSessionBindingEvent)event);
					break;	
				case SESSION_ATTRIBUTE_UNBOUND:
					((SipSessionBindingListener) listener).valueUnbound((SipSessionBindingEvent)event);
					break;
				case APP_SESSION_ATTRIBUTE_BOUND:
					((SipApplicationSessionBindingListener) listener).valueBound((SipApplicationSessionBindingEvent)event);
					break;	
				case APP_SESSION_ATTRIBUTE_UNBOUND:
					((SipApplicationSessionBindingListener) listener).valueUnbound((SipApplicationSessionBindingEvent)event);
					break;
				case URI_LOOKUP_COMPLETED:
					((DomainResolverListener) listener).handleResults((SipURILookupEvent)event);
					break;
				case URI_LOOKUP_ERROR:
					((DomainResolverListener) listener).error((SipURILookupErrorEvent)event);					
					break;
				case HTTP_SESSION_DESTROYED:
					((WASHttpSessionListener) listener).handleHttpSessionDestoyEvent(((HttpDestroyedEvent)event).getHTTPSession());
					break;
				case UNMATCHED_REQUEST_RECEIVED:
					((UnmatchedMessageListener) listener).unmatchedRequestReceived((UnmatchedRequestEvent)event);
					break;	
				case UNMATCHED_RESPONSE_RECEIVED:
					((UnmatchedMessageListener) listener).unmatchedResponseReceived((UnmatchedResponseEvent)event);
					break;
				default:
					if( c_logger.isTraceDebugEnabled()){
						c_logger.error("ListenerTask.dispatch: Task number does not exists!: " + task); 
					}
			}
		}finally{
			if(contextEstablisher != null){
				//Removing the application specific context from thread 
				contextEstablisher.removeContext(currentThreadClassLoader);
			}
		}
	}
	
	/**
	 * Returns the task description for logging purposes 
	 * @param taskNum
	 * @return
	 */
	static public String getTaskName(int taskNum){
		
		switch(taskNum){
			case APP_SESSION_ACTIVATED_TASK:
				return "Application Session activation";
			case APP_SESSION_PASSIVATE_TASK:
				return "Application Session passivation";
			case SESSION_ACTIVATED_TASK:
				return "SIP Session activation";
			case SESSION_PASSIVATE_TASK:
				return "SIP Session passivation";
			case SESSION_ATTR_ACTIVATED_TASK:
				return "SIP Session activation";
			case SESSION_ATTR_PASSIVATE_TASK:
				return "SIP Session passivation";
			case ERROR_NO_ACK_RECEIVED:
				return "No ACK received";
			case ERROR_NO_PRACK_RECEIVED:
				return "No PRACK received";
			case SIP_SERVLET_INITIATED:
				return "Sip servlet initiated";
			case SESSION_ATTRIBUTE_BOUND:
				return "SIP session atribute bounded";
			case SESSION_ATTRIBUTE_UNBOUND:
				return "SIP session atribute unbounded";
			case APP_SESSION_ATTRIBUTE_BOUND:
				return "Application Session atribute bounded";	
			case APP_SESSION_ATTRIBUTE_UNBOUND:
				return "Application Session atribute unbounded";
			case URI_LOOKUP_COMPLETED:
				return "URI Lookup complete";
			case URI_LOOKUP_ERROR:
				return "URI Lookup failed";
			case HTTP_SESSION_DESTROYED:
				return "HTTP session destroyed";
			case UNMATCHED_REQUEST_RECEIVED:
				return "Unmatched Request received";
			case UNMATCHED_RESPONSE_RECEIVED:
				return "Unmatched Response received";
			default:
				if( c_logger.isTraceDebugEnabled()){
					c_logger.error("SipApplicationSessionListenerTask.getTaskName: Task number does not exists!");
				}
		}
		return null;
	}

	/**
	 *  @see com.ibm.ws.sip.container.util.QueueIndex#getQueueIndex()
	 */
	public int getQueueIndex() {
		
		int index = -1;
		// will try to get queue index for following well known type of events
		// otherwise index 0 will be returned.
		if (_event instanceof SipApplicationSessionBindingEvent) {
			SipApplicationSessionBindingEvent event = (SipApplicationSessionBindingEvent) _event;
			SipApplicationSessionImpl app = (SipApplicationSessionImpl)event.getApplicationSession();
			index = app.extractAppSessionCounter();
		} else if (_event instanceof SipSessionBindingEvent) {
			SipSessionBindingEvent event = (SipSessionBindingEvent) _event;
			SipSessionImplementation session = (SipSessionImplementation)event.getSession();
			index = SipApplicationSessionImpl.extractAppSessionCounter(session.getApplicationSessionId());
		} else if (_event instanceof SipApplicationSessionEvent) {
			SipApplicationSessionEvent event = (SipApplicationSessionEvent) _event;
			SipApplicationSessionImpl app = (SipApplicationSessionImpl)event.getApplicationSession();
			index = app.extractAppSessionCounter();
		} else if (_event instanceof SipErrorEvent) {
			SipErrorEvent event = (SipErrorEvent) _event;
			SipServletMessageImpl request = (SipServletMessageImpl)event.getRequest();
			TransactionUserWrapper tu = request.getTransactionUser();
			SipApplicationSessionImpl appSess  =tu.getAppSessionForInternalUse();
			if (appSess != null) {
				// Moti: I assume here that every TU has an SipAppSessionImpl. I'm not sure if thats
				// a true assumption.
				index = appSess.extractAppSessionCounter();
			}  else {
				index = SipApplicationSessionImpl.extractAppSessionCounter(tu.getApplicationId());
			}
		} else if (_event instanceof SipSessionEvent) {
			SipSessionEvent event = (SipSessionEvent) _event;
			SipSessionImplementation session = (SipSessionImplementation)event.getSession();
			index = SipApplicationSessionImpl.extractAppSessionCounter(session.getApplicationSessionId());
		} else if (_event instanceof SipServletContextEvent || 
					_event instanceof UnmatchedRequestEvent || 
					_event instanceof UnmatchedResponseEvent) {
			// Noam: this can be a problem which will allow multiple threads handle the same sip app session
			// we are currently only round-robin the queues for this.
			if (_appQueueIndex > 0)
				index = _appQueueIndex;
			else
				index = ++_listenerQueue;

		} else if (_event instanceof SipURILookupEvent || _event instanceof SipURILookupErrorEvent) {
			SipSessionImplementation session = (SipSessionImplementation)_event.getSource();
			index = SipApplicationSessionImpl.extractAppSessionCounter(session.getApplicationSessionId());
		} else if (_event instanceof HttpDestroyedEvent) {
			ConvergedHttpSession session = (ConvergedHttpSession)_event.getSource();
			SipApplicationSessionImpl appSession = (SipApplicationSessionImpl)session.getApplicationSession();
			index = appSession.extractAppSessionCounter();
			
		} else {
			if( c_logger.isTraceDebugEnabled()){
				c_logger.error("getQueueIndex is returning a queue index of -1, unknown event was sent.");
			}
			
		}
		

		return index;
	}
	
	/**
	 * Getting the task name for logging
	 * @return
	 */
	public String getThisTaskname(){
		return getTaskName(_taskNum);
	}
	
	/**
	 * Return this task to pull
	 */
	public void recycle(){
		this._appDesc = null;
		this._appQueueIndex = -1;
		this._event = null;
		this._listener = null;
		this._sipContainerApplicationCodeDuration = null;
		this._sipContainerQueueDuration = null;
		pool.putBack(this);
	}

	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#priority()
	 */
	public int priority(){
		return PRIORITY_NORMAL;
	}
	
	/**
     * @see com.ibm.ws.sip.container.util.Queueable#getSipContainerQueueDuration()
     */
	public TaskDurationMeasurer getSipContainerQueueDuration() {
		return _sipContainerQueueDuration;
	}
	
	/**
     * @see com.ibm.ws.sip.container.util.Queueable#getApplicationCodeDuration()
     */
	public TaskDurationMeasurer getApplicationCodeDuration() {
		return _sipContainerApplicationCodeDuration;
	}
	
	/**
     * @see com.ibm.ws.sip.container.util.Queueable#getAppName()
     */
	public String getAppName() {
		if(_appDesc != null) { 
			return _appDesc.getApplicationName();
		}
		else return null;
	}

	/**
     * @see com.ibm.ws.sip.container.util.Queueable#getAppIndexForPMI()
     */
	public Integer getAppIndexForPMI() {
		if(_appDesc != null) {
			return _appDesc.getAppIndexForPmi();
		}
		else return null;
	}
	
	/**
     * @see com.ibm.ws.sip.container.util.Queueable#setSipContainerQueueDuration(TaskDurationMeasurer)
     */
	public void setSipContainerQueueDuration(TaskDurationMeasurer tm) {
		_sipContainerQueueDuration = tm;
		
	}

	/**
     * @see com.ibm.ws.sip.container.util.Queueable#setApplicationCodeDuration(TaskDurationMeasurer)
     */
	public void setApplicationCodeDuration(TaskDurationMeasurer tm) {
		_sipContainerApplicationCodeDuration = tm;
		
	}
	
	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getApplicationSession()
	 */
	@Override
	public SipApplicationSession getApplicationSession() {
		if (_event == null) {
			return null;
		}
		
		Object source = _event.getSource();
		
		if (source instanceof SipApplicationSession) {
			return (SipApplicationSession)source;
		}
		if (source instanceof SipSessionImplementation) {
			return ((SipSessionImplementation)source).getApplicationSession(false);
		}
		if (source instanceof SipServletRequestImpl) {
			return ((SipServletRequestImpl)source).getApplicationSession(false);
		}
		//Event source is from ServletContext or ConvergedHttpSession type (can't get the SAS)
		return null;
	}


	/**
	 * @see com.ibm.ws.sip.container.util.Queueable#getTuWrapper()
	 */
	@Override
	public TransactionUserWrapper getTuWrapper() {
		Object source = _event.getSource();
		if (_event instanceof UnmatchedResponseEvent || _event instanceof UnmatchedRequestEvent){
			if( c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("getTuWrapper is always null for unmatched messages");
			}
			return null;
		}
		if(source instanceof SipSession){
			return ((SipSessionImplementation)source).getInternalTuWrapper();
		}
		if (source instanceof SipServletMessageImpl){
			SipSession session = ((SipServletMessageImpl)source).getSession();
			if (session != null) {
				return ((SipSessionImplementation)session).getInternalTuWrapper();
			}
		}
		if (_event instanceof SipErrorEvent) {
			SipErrorEvent event = (SipErrorEvent)_event;
			SipServletMessageImpl request = (SipServletMessageImpl)event.getRequest();
			return request.getTransactionUser();
		}
		return null;
	}


	@Override
	public Object getServiceSynchronizer() {
		// Depracated method! noone calls it!
		return null;
	}
}
