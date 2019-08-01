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
package com.ibm.ws.sip.container.was;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.appqueue.AppQueueHandler;
import com.ibm.ws.sip.container.appqueue.MessageDispatchingHandler;
import com.ibm.ws.sip.container.appqueue.NativeMessageDispatchingHandler;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.router.tasks.RequestRoutedTask;
import com.ibm.ws.sip.container.util.Queueable;
import com.ibm.ws.sip.properties.SipPropertiesMap;

/**
 * This message dispatching handler using the WCCM thread pool configuration  
 * 
 * @author Nitzan
 */
@Component(
	service = MessageDispatchingHandler.class, 
	immediate = false,
	configurationPolicy = ConfigurationPolicy.OPTIONAL,
	property = "service.vendor=IBM" 
)
public class ExecutorMessageDispatchingHandler extends NativeMessageDispatchingHandler {
	
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(ExecutorMessageDispatchingHandler.class);

	/**
	 * This executor is injected with the Liberty default ExecutorServiceImpl. Container tasks invoked with it will be executed on the 
	 * global threads pool
	 */
	private ExecutorService executorService;
	
	 /**
     * DS method to activate this component.
     * 
     * @param properties : Map containing service & config properties
     *            populated/provided by config admin
     */
	@Activate
    protected void activate(Map<String, Object> properties) {
        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("ExecutorMessageDispatchingHandler activated", properties);
        SipPropertiesMap props = PropertiesStore.getInstance().getProperties();
        props.updateProperties(properties);
        
        start();
    }

    /**
     * DS method to deactivate this component.
     * 
     * @param reason int representation of reason the component is stopping
     */
	@Deactivate
    protected void deactivate(int reason) {
        if (c_logger.isEventEnabled())
            c_logger.event("ExecutorMessageDispatchingHandler deactivated, reason=" + reason);
        stop();
    }
    
    /**
     * DS method to activate this component.
     * 
     * @param properties : Map containing service & config properties
     *            populated/provided by config admin
     */
	//TODO Implementation Dynamic change of configuration (number of concurrent tasks == number of queues)
	//will be deferred to later
//	@Modified
//    protected void modified(Map<String, Object> properties) {
//		
//        if (c_logger.isTraceDebugEnabled())
//            c_logger.traceDebug("ExecutorMessageDispatchingHandler modified", properties);
//        
//        DeclarativeServiceProperties props = PropertiesStore.getInstance().getProperties().updateProperties(properties);
//        
//        if(props.wasChanged(CoreProperties.CONCURRENT_CONTAINER_TASKS)){
//        	initThreadsArray();
//        }
//        
//    }
    
	/**
	 * @see com.ibm.ws.sip.container.appqueue.NativeMessageDispatchingHandler#start()
	 */
	public void start() {
		 if (c_logger.isTraceDebugEnabled())
	            c_logger.traceDebug("ExecutorMessageDispatchingHandler start");
		super.start();
	}
	
	/**
	 * The default Liberty executor
	 * @param executorService
	 */
	@Reference(policy = ReferencePolicy.STATIC)
	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
		if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("ExecutorMessageDispatchingHandler setExecutorService", this.executorService);
	}
	
	/**
	 * @see com.ibm.ws.sip.container.appqueue.NativeMessageDispatchingHandler#initThreadsArray()
	 */
	protected void initThreadsArray() {
		//TODO Liberty How do we refresh dispatchers count why running?
		if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("ExecutorMessageDispatchingHandler initThreadsArray");
		super.initThreadsArray();
	}
	
	/**
	 * @see com.ibm.ws.sip.container.appqueue.NativeMessageDispatchingHandler#fillArray(int)
	 */
	protected void fillArray(int maxQsize) {
		for (int i = 0; i < s_dispatchers ; i++) {
			_dispatchersArray[i]= new ContextBasedQueue(maxQsize,i, this);
		}
	}

	/**
	 * should return true iff running as Java Standalone Application. 
	 * @param msg
	 * @return false - if we are running inside WAS.
	 */
	protected boolean detectNonWASenv(Queueable msg) 
	{
		// if we are here then we are running inside WAS for sure.
		return false;
	}

	/**
	 * TODO Liberty hung threads detection is not supported at this time
	 * will notify the relevant queue that it is hanged and will 
	 * force it to break.
	 * @param threadName - the hanged thread name
	 */
	void reportHangedThread(String threadName) {
		//Moti: why threadName and not thread ID ? good question but the ThreadMonitor API
		// doesn't return what Thread.getId() returns. It return some hex string 
		// which has nothing to do with the real thread id (even when converted to Hex) 
		//method has intentionally limited package visibility
		if (c_logger.isTraceEntryExitEnabled()){
            c_logger.traceEntry("reportHangedThread",threadName);
		}
		performHungThreadProcess(threadName, _dispatchersArray);
		if (c_logger.isTraceEntryExitEnabled()){
            c_logger.traceExit(this, "reportHangedThread");
		}
	}
	
	/**
	 * TODO Liberty hung threads detection is not supported at this time
	 * @param threadName
	 * @param queue
	 */
	protected void performHungThreadProcess( String threadName, AppQueueHandler[] queue){ 
		for (int i = 0; i < s_dispatchers ; i++) {
			ContextBasedQueue current = (ContextBasedQueue)queue[i];
			if (current.reportHangedThread(threadName)) {
				Queueable relatedTask = current.getLastTask();
				if (relatedTask != null) {
					// I does has effect on performance though...
					// since it uses instanceof + casting but I prefer to tell the user which call he lost
					// at this price of performance.
					if (relatedTask instanceof RequestRoutedTask) {
						RequestRoutedTask sipMsg = (RequestRoutedTask)relatedTask;
						String  currentRunningMsgDescription = sipMsg.getRequest().getCallId();
						if (c_logger.isInfoEnabled()){
				            c_logger.info("warn.sip.queue.hung", currentRunningMsgDescription);
						}
					}
				}
				if (c_logger.isTraceDebugEnabled()){
		            c_logger.traceDebug("reportHangedThread","found a hanged queue match:"+current
							+" desc="+relatedTask);
				}
				current.finishedToExecuteTask(null);
				break; //why ? there should be only one queue that handles this thread.
			}
		}
	}
	
	/**
	 * Execute a queueable task using the appropriate threads pool
	 * @param task
	 * @throws IllegalStateException
	 * @throws InterruptedException
	 */
	public void execute( Runnable task) throws IllegalStateException, InterruptedException{
		executorService.execute(task);
	}
	
	/**
	 * Wrapping a queueable with a SignalingEndOfTask wrapper
	 * @param q
	 * @param task
	 * @return
	 */
	public SignalingEndOfTask wrapTaskWithSignaling(ContextBasedQueue q, Queueable task){
		return new SignalingEndOfTask(q, task);
	}
	
    /**
     * Injecting the PerformanceMgr DS
     * @param performanceMgr
     */
	@Reference(service=PerformanceMgr.class, cardinality = ReferenceCardinality.MANDATORY)
    protected void setPerformanceMgr(PerformanceMgr performanceMgr) {
		m_perfMgr = performanceMgr;
	}
    
	/**
	 * Unset the PerformanceMgr DS
	 * @param performanceMgr
	 */
    protected void unsetPerformanceMgr(PerformanceMgr performanceMgr) {
		if (performanceMgr == m_perfMgr) {
			m_perfMgr = null;
		}
	}
}
