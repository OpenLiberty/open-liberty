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
package com.ibm.ws.sip.container.appqueue;

import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.sip.SipServletResponse;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.router.SipRouter;
import com.ibm.ws.sip.container.router.tasks.RequestRoutedTask;
import com.ibm.ws.sip.container.router.tasks.RoutedTask;
import com.ibm.ws.sip.properties.CoreProperties;

/**
 * @author Amir Perlman, Jan 12, 2004
 *
 * Dispatches messages to the Web Container using a dedicated local thread.  
 */
public class MessageDispatcher {
	
	/**
     * Class Logger. 
     */
	//TODO change logger
		private static final TraceComponent tc = Tr.register(MessageDispatcher.class);
    private static final LogMgr c_logger = Log.get(MessageDispatcher.class);
    
	/**
	 * Interval in which similar errors are printed
	 */
	private static long ERR_PRINTOUT_INTERVAL = 1000; //one second
	
	/**
	 * last time an error was printed 
	 */
	private static long _lastErrorPrintoutTime = 0;
		
	/**
	 * When 0 (default) - no statistic will be printed.
	 * When 2 - print only in case of queue overloaded and throwing the message.
	 * When 1 - periodically this MessageDispatcher will call each thread to print it's 
	 * state.
	 */
	public static int _shouldPrintQueueState = CoreProperties.TO_PRINT_QUEUE_STATE_DEFAULT;
	
	/**
	 * The time period in seconds over which MessageDispatcher will call 
	 * each thread to print it's state
	 */
	public static int _printTempo = CoreProperties.PRINTING_TEMPO_DEFAULT;
	
	
	private static Timer s_pmiTimer = new Timer(true);
	/**
	 * This is the dispatching handler. It is responsible of the way 
	 * the messages are dispatched. Its injected as a declarative service.
	 */
	private static MessageDispatchingHandler s_messageDispatchingHandler;

	/**
     * Setting the messageDispatchingHandler
     * @param messageDispatchingHandler
     */
	public static void setMessageDispatchingHandler( MessageDispatchingHandler messageDispatchingHandler) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "MessageDispatcher setMessageDispatchingHandler, messageDispatchingHandler=" + messageDispatchingHandler);
		s_messageDispatchingHandler = messageDispatchingHandler;
	}
	
	/**
     * returning the messageDispatchingHandler
     * @param messageDispatchingHandler
     */
	public static MessageDispatchingHandler getMessageDispatchingHandler() {
		return s_messageDispatchingHandler;
	}
	
	 /**
     * Start the invoker 
     *
     */
    public void start()
    {
    	//TODO this thing seems completely messed up. 1 or 2 is only on overload? seems that
    	//the documentation of the property has got it wrong. Do we really want to keep this?
    	_shouldPrintQueueState = 
    		PropertiesStore.getInstance().getProperties().getInt(CoreProperties.TO_PRINT_QUEUE_STATE);
		
    	if (_shouldPrintQueueState == 2) {
			
    		_printTempo = PropertiesStore.getInstance().getProperties()
					.getInt(CoreProperties.PRINTING_TEMPO);
			
			// Create and run a timer for update the statistic periodically
			s_pmiTimer.schedule(new PrintDispatcherListener(),
					_printTempo, _printTempo);
		}
		
		Object[] params = { _shouldPrintQueueState, _printTempo };
		c_logger.info("info.sip.queue.stats", Situation.SITUATION_REPORT_STATUS, params);
			
		//TODO Liberty: start will happen when the handler is injected to the main service
    	//s_messageDispatchingHandler.start();
    }
    
    /**
     * @see com.ibm.ws.sip.container.router.SipServletsInvoker#stop()
     */
    public void stop()
    {
    	s_messageDispatchingHandler.stop();
    }
    
    public static void dispatchRoutedTask(RoutedTask task){
    	if( !task.forDispatching()){
    		task.executeOnCurrentThread();
    		return;
    	}
    	
        if( !s_messageDispatchingHandler.dispatch( task)){
	    	if (c_logger.isErrorEnabled()){
	    		printDispatchError(task);
			}
			if (task instanceof RequestRoutedTask) {
				RequestRoutedTask requestTask = (RequestRoutedTask)task;
				if(!requestTask.isAck()){
					// queue is too full to handle the request.
					// must return some final response, to clean up the transaction.
					SipRouter.sendErrorResponse( requestTask.getRequest(), 
												 SipServletResponse.SC_TEMPORARLY_UNAVAILABLE);
				}
			}
        }
    }
    
    /**
     * Printing the dispatch error
     * @param msg
     */
    private static void printDispatchError(RoutedTask task) {
		if (_lastErrorPrintoutTime < System.currentTimeMillis()- ERR_PRINTOUT_INTERVAL) {
			if (_lastErrorPrintoutTime < System.currentTimeMillis()- ERR_PRINTOUT_INTERVAL) {
				//c_logger.error("error.container.queue.overloaded", null, task.getMethod());
				//TODO change the string of the CWSCT0346E in the sip.util in WAS 7.0
				StringBuffer buff = new StringBuffer();
				buff.append("CWSCT0346E: One container thread queue reached it's maximum capacity");
				buff.append(task.getMethod());
				c_logger.error(buff.toString(), null, null);
				if(_shouldPrintQueueState > 0 ){
					printDispatcerInfo();
				}
				_lastErrorPrintoutTime = System.currentTimeMillis();
			}
		}
	}
    
    /**
     * @author Anat Fradin , Nov 3, 2004 Implement the TimerTask. Timer will
	 *         notify it periodically. When the timer is fired the
	 *         PerformanceMgr will update statistics in the PMI according to the
	 *         update time.
	 */
    class PrintDispatcherListener extends TimerTask {
    	/**
    	 * @see java.util.TimerTask#run()
    	 */
    	public void run() {
    		try {
    			printDispatcerInfo();
    		} catch (Throwable th) {
    			if (c_logger.isErrorEnabled()) {
    				c_logger.error("error.exception", null, null, th);
    			}
    		}
    	}
    }

	/**
	 * Helper method which prints the information about this dispatcher.
	 *
	 */
	private static void printDispatcerInfo() {
		s_messageDispatchingHandler.printState();
	}
}