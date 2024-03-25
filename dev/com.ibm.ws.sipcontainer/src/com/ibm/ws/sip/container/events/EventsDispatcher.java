/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.events;

import java.util.EventListener;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipApplicationSessionActivationListener;
import javax.servlet.sip.SipApplicationSessionBindingEvent;
import javax.servlet.sip.SipApplicationSessionBindingListener;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipErrorEvent;
import javax.servlet.sip.SipErrorListener;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletContextEvent;
import javax.servlet.sip.SipServletListener;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSessionActivationListener;
import javax.servlet.sip.SipSessionBindingEvent;
import javax.servlet.sip.SipSessionBindingListener;
import javax.servlet.sip.SipSessionEvent;
import javax.servlet.sip.SipURI;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.sip.SipApplicationSessionStateListener;
import com.ibm.websphere.sip.SipSessionStateListener;
import com.ibm.websphere.sip.resolver.DomainResolverListener;
import com.ibm.websphere.sip.resolver.events.SipURILookupErrorEvent;
import com.ibm.websphere.sip.resolver.events.SipURILookupEvent;
import com.ibm.websphere.sip.resolver.exception.SipURIResolveException;
import com.ibm.websphere.sip.unmatchedMessages.UnmatchedMessageListener;
import com.ibm.websphere.sip.unmatchedMessages.events.UnmatchedRequestEvent;
import com.ibm.websphere.sip.unmatchedMessages.events.UnmatchedResponseEvent;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.failover.repository.SessionRepository;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.router.SipRouter;
import com.ibm.ws.sip.container.servlets.SipApplicationSessionImpl;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipServletResponseImpl;
import com.ibm.ws.sip.container.servlets.SipSessionImplementation;
import com.ibm.ws.sip.container.was.HttpDestroyedEvent;
import com.ibm.ws.sip.container.was.WASHttpSessionListener;
//TODO Liberty the following imports don't longer exist as we don't support HA in Liberty
//import com.ibm.ws.sip.container.failover.FailoverMgr;
//import com.ibm.ws.sip.container.failover.FailoverMgrLoader;
//TODO Liberty - replace 
//import com.ibm.wsspi.sip.channel.resolver.sipurilookupexception;
import com.ibm.wsspi.sip.channel.resolver.SipURILookupException;

/**
 * This class holds helper methods for dispatching listener events
 * multi-threaded. Listener methods that are implemented by the application composer
 * should always be executed on the threads from the application
 * thread pool. Therefore, if a listener event was not directly executed
 * on an application thread (like timer events that executes on a timer thread
 * or the activation events that are executed on an HA thread), 
 * it should be dispatched from here. 
 * 
 * @author Nitzan
 */
public class EventsDispatcher {
	/**
     * Class Logger.
     */
    private static final transient LogMgr c_logger = Log.get(EventsDispatcher.class);
    
    /**
	 * Interval in which similar errors are printed
	 */
	private static long ERR_PRINTOUT_INTERVAL = 1000; //one second
	
	/**
	 * last time an error was printed 
	 */
	private static long _lastErrorPrintoutTime = 0;
	
	/**
	 * Synchronizing the error printing 
	 */
	private static Object PRINT_SYNC = new Object();
	
    /**
     * Dispatching the no ACK received event on an SipErrorListener
     * @param response
     * @param appDescriptor
     */
    public static void noAckReceived( SipServletResponseImpl response, 
    						   SipAppDesc appDescriptor){
    	dispatchErrorListenerTasks(response, 
    			appDescriptor, ListenerTask.ERROR_NO_ACK_RECEIVED);
    }
    
    /**
     * Dispatching the no ACK received event on an SipErrorListener
     * @param response
     * @param appDescriptor
     */
    public static void unmatchedRequestReceived( SipServletRequestImpl response){
    	
    	SipRouter router = SipContainer.getInstance().getRouter();
		
    	Iterator<SipAppDesc> iter = router.getAllApps().iterator();
    	
		while(iter.hasNext()){
	        SipAppDesc desc = iter.next();
	        dispatchUnmatchedRequestListenerTasks(response, desc, ListenerTask.UNMATCHED_REQUEST_RECEIVED);
		}
		
    	
    }
    
    /**
     * Dispatching the no ACK received event on an SipErrorListener
     * @param response
     * @param appDescriptor
     */
    public static void unmatchedResponseReceived( SipServletResponseImpl response){
    	
    	SipRouter router = SipContainer.getInstance().getRouter();
		
    	Iterator<SipAppDesc> iter = router.getAllApps().iterator();
    	
		while(iter.hasNext()){
	        SipAppDesc desc = iter.next();
	        dispatchUnmatchedResponseListenerTasks(response, desc, ListenerTask.UNMATCHED_RESPONSE_RECEIVED);
		}
    }
    
    /**
     * Dispatching the no PRACK received event on an SipErrorListener
     * @param response
     * @param appDescriptor
     */
    public static void noPrackReceived( SipServletResponseImpl response, 
			   					SipAppDesc appDescriptor){
		dispatchErrorListenerTasks(response, 
				appDescriptor, ListenerTask.ERROR_NO_PRACK_RECEIVED);
	}
    
    /**
     * Dispatching the session did activate event on a 
     * SipApplicationSessionStateListener
     * @param response
     * @param appDescriptor
     */
    public static void AppSessionActivated(SipApplicationSessionImpl appSession){
    	dispatchAppSessionStateListenerTasks(appSession, 
    						ListenerTask.APP_SESSION_ACTIVATED_TASK);
    }

    /**
     * Dispatching the session will passivate event on a 
     * SipApplicationSessionStateListener
     * @param response
     * @param appDescriptor
     */
    public static void AppSessionWillPassivate(SipApplicationSessionImpl appSession){
    	dispatchAppSessionStateListenerTasks(appSession, 
    						ListenerTask.APP_SESSION_PASSIVATE_TASK);
    }
    
    /**
     * Dispatching the session did activate event on a 
     * SipSessionStateListener and on the attributes that 
     * implements SipSessionActivationListener
     * @param response
     * @param appDescriptor
     */
    public static void SipSessionActivated(SipSessionImplementation session,
									SipAppDesc appDescriptor){
    	
    	dispatchSipSessionStateListenerTasks( session, appDescriptor,
    							ListenerTask.SESSION_ACTIVATED_TASK);
    }


    /**
     * Dispatch un/bound event to all attributes of Sip Session 
     * @param session
     * @param appDescriptor
     * @param isBound
     */
    public static void SipSessionAllAttributeUnbounding(SipSession session, SipAppDesc appDescriptor) {
    	Map attrMap = SessionRepository.getInstance().getAttributes(session.getId()); 

    	for( Iterator itr = attrMap.keySet().iterator(); itr.hasNext();){
    		String attrib = (String)itr.next();
    		SipSessionBindingEvent evt = new SipSessionBindingEvent(session, attrib);

    		dispatchAttributeListener(attrMap, attrib, appDescriptor, evt, true, ListenerTask.SESSION_ATTRIBUTE_UNBOUND);
    	}
    }
    
    
    /**
     * Dispatch un/bound event to sip session attribute
     * 
     * @param session
     * @param attribute
     * @param appDescriptor
     * @param isBound
     */
    public static void SipSessionAttributeBounding(SipSession session, String attribute, SipAppDesc appDescriptor, boolean isBound) {
    	Map attrMap = SessionRepository.getInstance().getAttributes(session.getId()); 
    	
    	SipSessionBindingEvent evt = new SipSessionBindingEvent(session, attribute);
    	
    	int taskNum = isBound ? ListenerTask.SESSION_ATTRIBUTE_BOUND : ListenerTask.SESSION_ATTRIBUTE_UNBOUND; 
    	
		dispatchAttributeListener(attrMap, attribute, appDescriptor, evt, true, taskNum);
    }
    
    /**
     * Dispatch bound/unbound event on attribute listener of SipApplicationSession
     * 
     * @param appSession
     * @param attribute
     * @param appDescriptor
     * @param isBound
     */
    public static void AppSessionAttributeBounding(SipApplicationSession appSession, String attribute, SipAppDesc appDescriptor, boolean isBound) {
    	Map attrMap = SessionRepository.getInstance().getAttributes(appSession); 
    	
    	SipApplicationSessionBindingEvent evt = new SipApplicationSessionBindingEvent(appSession, attribute);
    	
    	int taskNum = isBound ? ListenerTask.APP_SESSION_ATTRIBUTE_BOUND : ListenerTask.APP_SESSION_ATTRIBUTE_UNBOUND; 
    	
		dispatchAttributeListener(attrMap, attribute, appDescriptor, evt, true, taskNum);
    }
    

    
    /**
     * Dispatching the session will passivate event on a 
     * SipSessionStateListener and on the attributes that 
     * implements SipSessionActivationListener
     * @param response
     * @param appDescriptor
     */
    public static void SipSessionWillPassivate(SipSessionImplementation session,
											SipAppDesc appDescriptor){
    	dispatchSipSessionStateListenerTasks(session, appDescriptor,
    							ListenerTask.SESSION_PASSIVATE_TASK);
    }
    
    /**
     * Send listener siplet initiated event
     * 
     * @param appDescriptor
     * @param sipServlet
     * @param context
     * @param appQueueIndex
     */
    public static void sipServletInitiated(SipAppDesc appDescriptor, SipServlet sipServlet, ServletContext context, long appQueueIndex) {
		if ( appDescriptor.getSipServletListeners().isEmpty()) {
			if (c_logger.isTraceDebugEnabled()) {
	    		c_logger.traceExit(null, "dispatchSipServletListeners, no listeners to call");
	    	}
			return;
		}
		
		if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceExit(null, "dispatchSipServletListeners calling listeners");
    	}
		
    	EventObject evt = new SipServletContextEvent(context, sipServlet);		
        Iterator<SipServletListener> iter = appDescriptor.getSipServletListeners().iterator();

        dispatchTasksOnCurrentThread(iter, evt, appDescriptor, ListenerTask.SIP_SERVLET_INITIATED, appQueueIndex);

    }
    
    
    public static void uriLookupComplete(DomainResolverListener listener, SipSession session, SipURI sipUri, List <SipURI> results, boolean isOnCurrentThread) {
		EventObject evt = new SipURILookupEvent(session, sipUri, results);
		
		dispatchTask(listener, evt, isOnCurrentThread, ListenerTask.URI_LOOKUP_COMPLETED);
    }
    
    public static void uriLookupError(DomainResolverListener listener, SipSession session, SipURI sipUri,  SipURILookupException exception, boolean isOnCurrentThread) {
		EventObject evt = new SipURILookupErrorEvent(session, sipUri, new SipURIResolveException(exception));
		
		dispatchTask(listener, evt, isOnCurrentThread, ListenerTask.URI_LOOKUP_ERROR);
    }
    
    public static void httpSessionDestroyed(WASHttpSessionListener listener, HttpSession session) {
		EventObject evt = new HttpDestroyedEvent(session);
		
		dispatchTask(listener, evt, false, ListenerTask.HTTP_SESSION_DESTROYED);
    }
    
    /**
     * Notify on error events
     * @param appSession
     */
    private static void dispatchUnmatchedRequestListenerTasks( SipServletRequestImpl request, 
    										 SipAppDesc appDescriptor, 
    										 int taskNum){
    	if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceEntry(null, "dispatchUnmatchedRequestListenerTasks", new Object[]{request, appDescriptor, new Integer(taskNum)});
    	}
		if ( appDescriptor.getUnmatchedMessagesListeners().isEmpty()) {
			if (c_logger.isTraceDebugEnabled()) {
	    		c_logger.traceExit(null, "dispatchErrorListenerTasks, no listeners to call");
	    	}
			return;
		}
		
		EventObject evt = new UnmatchedRequestEvent(request,appDescriptor.getServletContext());

        Iterator<UnmatchedMessageListener> iter = appDescriptor.getUnmatchedMessagesListeners().iterator();
        	
        dispatchTasks(iter, evt, taskNum, appDescriptor);
              
        
        if (c_logger.isTraceDebugEnabled()) {
			StringBuffer buff = new StringBuffer(100);
			buff.append("dispatchUnmatchedRequestListenerTasks sent: ");
			buff.append(ListenerTask.getTaskName(taskNum));
			buff.append("request = ");
			buff.append(request.getMethod());
			buff.append("id = ");
			buff.append(request.getCallId());
			c_logger.traceDebug(null, "EventsDispatcher.dispatchUnmatchedRequestListenerTasks",buff.toString());
		}
        if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceExit(null, "dispatchUnmatchedRequestListenerTasks");
    	}
	}
    
    /**
     * Notify on error events
     * @param appSession
     */
    private static void dispatchUnmatchedResponseListenerTasks( SipServletResponseImpl response, 
    										 SipAppDesc appDescriptor, 
    										 int taskNum){
    	if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceEntry(null, "dispatchUnmatchedRequestListenerTasks", new Object[]{response, appDescriptor, new Integer(taskNum)});
    	}
		if ( appDescriptor.getUnmatchedMessagesListeners().isEmpty()) {
			if (c_logger.isTraceDebugEnabled()) {
	    		c_logger.traceExit(null, "dispatchUnmatchedResponseListenerTasks, no listeners to call");
	    	}
			return;
		}
		EventObject evt = new UnmatchedResponseEvent(response,appDescriptor.getServletContext());

        Iterator<UnmatchedMessageListener> iter = appDescriptor.getUnmatchedMessagesListeners().iterator();
        	
        dispatchTasks(iter, evt, taskNum, appDescriptor);
        
        if (c_logger.isTraceDebugEnabled()) {
			StringBuffer buff = new StringBuffer(100);
			buff.append("dispatchUnmatchedResponseListenerTasks sent: ");
			buff.append(ListenerTask.getTaskName(taskNum));
			buff.append("response = ");
			buff.append(response.getMethod());
			buff.append("id = ");
			buff.append(response.getCallId());
			c_logger.traceDebug(null, "dispatchUnmatchedResponseListenerTasks",buff.toString());
		}
        if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceExit(null, "dispatchUnmatchedResponseListenerTasks");
    	}
	}
    
    /**
     * Notify on error events
     * @param appSession
     */
    private static void dispatchErrorListenerTasks( SipServletResponseImpl response, 
    										 SipAppDesc appDescriptor, 
    										 int taskNum){
    	if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceEntry(null, "dispatchErrorListenerTasks", new Object[]{response, appDescriptor, new Integer(taskNum)});
    	}
		if ( appDescriptor.getErrorListeners().isEmpty()) {
			if (c_logger.isTraceDebugEnabled()) {
	    		c_logger.traceExit(null, "dispatchErrorListenerTasks, no listeners to call");
	    	}
			return;
		}
		EventObject evt = new SipErrorEvent(response.getRequest(),response);

        Iterator<SipErrorListener> iter = appDescriptor.getErrorListeners().iterator();
        	
        dispatchTasks(iter, evt, taskNum);
        
        if (c_logger.isTraceDebugEnabled()) {
			StringBuffer buff = new StringBuffer(100);
			buff.append("Error notifications sent: ");
			buff.append(ListenerTask.getTaskName(taskNum));
			buff.append("response = ");
			buff.append(response.getMethod());
			buff.append("id = ");
			buff.append(response.getCallId());
			c_logger.traceDebug(null, "EventsDispatcher.dispatchErrorListenerTasks",buff.toString());
		}
        if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceExit(null, "dispatchErrorListenerTasks");
    	}
	}
    
    /**
     * Notify on APP session state events
     * @param appSession
     */
    private static void dispatchAppSessionStateListenerTasks( SipApplicationSessionImpl appSession, 
    												   int taskNum){
    	if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceEntry(null, "dispatchAppSessionStateListenerTasks", new Object[]{appSession, new Integer(taskNum)});
    	}
		
            
		EventObject evt = new SipApplicationSessionEvent(appSession);

		//dispatch appSessionStateListeners events
		SipAppDesc appDescriptor = appSession.getAppDescriptor();
        Iterator<SipApplicationSessionStateListener> iter = appDescriptor.getAppSessionStateListeners().iterator(); 
        dispatchTasks(iter, evt, taskNum);

        //dispatch appSessionActivationListeners events - this listener api replaces the appSessionStateListeners in jsr289
        Iterator<SipApplicationSessionActivationListener> iter2 = appDescriptor.getAppSessionActivationListeners().iterator(); 
        dispatchTasks(iter2, evt, taskNum);
        
		Map attributes = SessionRepository.getInstance().getAttributes(appSession);
		
        if( attributes == null || attributes.isEmpty()){
        	if (c_logger.isTraceDebugEnabled()) {
        		c_logger.traceDebug( "dispatchAppSessionStateListenerTasks, no attribute listeners to call");
        	}
		}else{
	        synchronized (attributes) {
	        	dispatchAttributesListeners(attributes, appDescriptor, evt, taskNum);
	        }
		}
        
        if (c_logger.isTraceDebugEnabled()) {
			StringBuffer buff = new StringBuffer(100);
			buff.append("App Session notifications Sent: ");
			buff.append(ListenerTask.getTaskName(taskNum));
			buff.append("Id = ");
			buff.append(appSession.getId());
			c_logger.traceDebug(null, "EventsDispatcher.dispatchAppSessionStateListenerTasks",buff.toString());
		}
        if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceExit(null, "dispatchAppSessionStateListenerTasks");
    	}
	}

    /**
     * Notify on session state events
     * @param appSession
     */
    private static void dispatchSipSessionStateListenerTasks( SipSessionImplementation session,
    												SipAppDesc appDescriptor,
    												int taskNum){
    	if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceEntry(null, "dispatchSipSessionStateListenerTasks", new Object[]{appDescriptor, new Integer(taskNum)});
    	}
    	EventObject evt = null;
    	
		if ( appDescriptor.getSessionStateListeners().isEmpty()) {
			if (c_logger.isTraceDebugEnabled()) {
	    		c_logger.traceDebug("dispatchSipSessionStateListenerTasks, no listeners to call");
	    	}
		}else{
            
			evt = new SipSessionEvent(session);
	
	        Iterator<SipSessionStateListener> iter = appDescriptor.getSessionStateListeners().iterator(); 
	        
	        dispatchTasks(iter, evt, taskNum);
        
	        if (c_logger.isTraceDebugEnabled()) {
				StringBuffer buff = new StringBuffer(100);
				buff.append("SIP Session notifications Sent: ");
				buff.append(ListenerTask.getTaskName(taskNum));
				buff.append("Id = ");
				buff.append(session.getId());
				c_logger.traceDebug(null, "EventsDispatcher.dispatchTasks",buff.toString());
			}
		}
		
		Map attributes = SessionRepository.getInstance().getAttributes(session.getId());
		
        if( attributes == null || attributes.isEmpty()){
        	if (c_logger.isTraceDebugEnabled()) {
        		c_logger.traceDebug( "dispatchSipSessionStateListenerTasks, no attribute listeners to call");
        	}
		}else{
			if(evt == null){
				evt = new SipSessionEvent(session);
			}
	        synchronized (attributes) {
	        	dispatchAttributesListeners(attributes, appDescriptor, evt, taskNum);
	        }
		}
        
        if (c_logger.isTraceDebugEnabled()) {
    		c_logger.traceExit(null, "dispatchSipSessionStateListenerTasks");
    	}
	}
    
    /**
     * Dispatching all listener attribute tasks
     * @param attributes
     * @param appDescriptor
     * @param evt
     * @param taskNum
     */
    private static void dispatchAttributesListeners( Map attributes, SipAppDesc appDescriptor, EventObject evt, int taskNum){
    	for( Iterator itr = attributes.keySet().iterator(); itr.hasNext();){
    		dispatchAttributeListener(attributes, (String)itr.next(), appDescriptor, evt, false, taskNum);
    	}
    }
    
    /**
     * Returning the attribute if it is a listener
     * @param attributes
     * @param name
     * @return
     */
    private static EventListener getAttributeListener(Map attributes, String name){
    	Object value = attributes.get(name);
    	if ( value == null || !(value instanceof EventListener)){
    		return null;
    	}
    	
    	return (EventListener)value;
    }
    
    /**
     * Dispatching an attribute listener callback as a container task 
     * @param attributes
     * @param attribute
     * @param appDescriptor
     * @param evt
     * @param isOnCurrentThread
     * @param taskNum
     */
    private static void dispatchAttributeListener(Map attributes, String attribute, SipAppDesc appDescriptor, EventObject evt, boolean isOnCurrentThread, int taskNum){
    	ContextEstablisher contextEstablisher = appDescriptor.getContextEstablisher();
    	if (contextEstablisher == null) {
    		return; // running in standalone
    	}
    	
    	EventListener listener = getAttributeListener(attributes, attribute);
		if( listener == null){
			return;
		}

		switch (taskNum) {
		case ListenerTask.SESSION_ACTIVATED_TASK: 
			if (listener instanceof SipSessionActivationListener) {
				dispatchTask(listener, evt, isOnCurrentThread, ListenerTask.SESSION_ATTR_ACTIVATED_TASK);
			}
			break;
			
		case ListenerTask.SESSION_PASSIVATE_TASK:
			if (listener instanceof SipSessionActivationListener) {
				dispatchTask(listener, evt, isOnCurrentThread, ListenerTask.SESSION_ATTR_PASSIVATE_TASK);
			}
			
			break;
			
		case ListenerTask.SESSION_ATTRIBUTE_BOUND:
			if (listener instanceof SipSessionBindingListener) {
				dispatchTask(listener, evt, isOnCurrentThread, ListenerTask.SESSION_ATTRIBUTE_BOUND);
			}
			
			break;
			
		case ListenerTask.SESSION_ATTRIBUTE_UNBOUND:
			if (listener instanceof SipSessionBindingListener) {
				dispatchTask(listener, evt, isOnCurrentThread, ListenerTask.SESSION_ATTRIBUTE_UNBOUND);
			}
			
			break;
			
		case ListenerTask.APP_SESSION_ACTIVATED_TASK:
			if (listener instanceof SipApplicationSessionActivationListener) {
				dispatchTask(listener, evt, isOnCurrentThread, ListenerTask.APP_SESSION_ACTIVATED_TASK);
			}
			
			break;
		case ListenerTask.APP_SESSION_PASSIVATE_TASK:
			if (listener instanceof SipApplicationSessionActivationListener) {
				dispatchTask(listener, evt, isOnCurrentThread, ListenerTask.APP_SESSION_PASSIVATE_TASK);
			}			
			
			break;
			
		case ListenerTask.APP_SESSION_ATTRIBUTE_BOUND:
			if (listener instanceof SipApplicationSessionBindingListener) {
				dispatchTask(listener, evt, isOnCurrentThread, ListenerTask.APP_SESSION_ATTRIBUTE_BOUND); 
			} 
			
			break;
		case ListenerTask.APP_SESSION_ATTRIBUTE_UNBOUND:
			if (listener instanceof SipApplicationSessionBindingListener) {
				dispatchTask(listener, evt, isOnCurrentThread, ListenerTask.APP_SESSION_ATTRIBUTE_UNBOUND); 
			} 
			
			break;
		default:
				return;
			}
    		
		}
    		
    private static void dispatchTask(EventListener listener, EventObject evt, boolean isOnCurrentThread, int taskNum) {
		ListenerTask task = ListenerTask.getAvailableInstance();
		task.init( listener, evt, taskNum);

		if (!isOnCurrentThread)
			dispatch(task);
		else
			task.run();
    	
    }    
    
    /**
     * Iterate over a list of listener and dispatch them with the given 
     * event object
     * 
     * @param listenerIter listeners iterator
     * @param evt event to send
     * @param appDesc application descriptor
     * @param taskNum task type
     * @param appQueueIndex queue number (when possible)
     */
    private static void dispatchTasksOnCurrentThread( Iterator<? extends EventListener> listenerIter, EventObject evt, SipAppDesc appDesc, int taskNum, long appQueueIndex){
    	for ( ;listenerIter.hasNext();) {
    		EventListener listener = listenerIter.next();
    		    		
	    	ListenerTask task = ListenerTask.getAvailableInstance();
	    	task.init( listener, evt, taskNum, appDesc, appQueueIndex);
	    	
	    	task.run();
	    }
    }
    
    
    /**
     * Iterate over a list of listener and dispatch them with the given 
     * event object
     * @param appSession
     */
    private static void dispatchTasks( Iterator<? extends EventListener> listenerIter, EventObject evt, int taskNum){
    	for ( ;listenerIter.hasNext();) {
    		EventListener listener = listenerIter.next();
    		    		
	    	ListenerTask task = ListenerTask.getAvailableInstance();
	    	task.init( listener, evt, taskNum);
	    	
	        dispatch(task);
	    }
    }
    
    /**
     * Iterate over a list of listener and dispatch them with the given 
     * event object
     * @param appSession
     */
   private static void dispatchTasks( Iterator<? extends EventListener> listenerIter, EventObject evt, int taskNum,SipAppDesc appDesc){
   	for ( ;listenerIter.hasNext();) {
   		EventListener listener = listenerIter.next();
   		    		
   		ListenerTask task = ListenerTask.getAvailableInstance();
    	task.init( listener, evt, taskNum, appDesc, -1);
	    	
	        dispatch(task);
	    }
   }
	/**
	 * Dispatch the event to the container thread pool
	 * @param event
	 */
	private static void dispatch( ListenerTask event){
		if(!SipContainer.getTasksInvoker().invokeTask(event)){
			printDispatchError(event);
			event.recycle();
		}
	}
	
	/**
     * Printing the dispatch error
     * @param msg
     */
    private static void printDispatchError( ListenerTask event){
    	if(_lastErrorPrintoutTime < System.currentTimeMillis() - ERR_PRINTOUT_INTERVAL){
    		synchronized(PRINT_SYNC){
    			if(_lastErrorPrintoutTime < System.currentTimeMillis() - ERR_PRINTOUT_INTERVAL){
	    			c_logger.error("Dispatch queue overloaded listener task " 
	    							+ event.getThisTaskname() + " rejected!");
	    			_lastErrorPrintoutTime = System.currentTimeMillis();
    			}
    		}
    	}
    }
}
