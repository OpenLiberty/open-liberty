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

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.util.Queueable;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.ws.sip.properties.SipPropertiesMap;

/**
 * Message dispatching handler that uses a private implementation of
 * a thread pool 
 * 
 * @author Nitzan
 * @update Moti 15May2008 : fix printstat casting.
 */
public class NativeMessageDispatchingHandler implements MessageDispatchingHandler{


	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(NativeMessageDispatchingHandler.class);

    /** 
     * An injected PerformanceMgr declarative service 
     */
    protected PerformanceMgr m_perfMgr;

	/**
	 * Defines the maximum number of messages which can be dispatches in all
	 * queues at the same time.
	 * Each queue will have maximum number of 
	 * messages = s_maxDispatchPerThread / number of queues.
	 */
	static int s_maxDispatchMessages = CoreProperties.MAX_MSG_QUEUE_SIZE_DEFAULT;

	/**
	 * Defines the initial size of the queue in the each DispatcherWorkingThread
	 */
	static int s_queueSizeInDispatcher = CoreProperties.MSG_QUEUE_INITIAL_SIZE_DEFAULT;

	/**
	 * Array of messages dispatcher each one with a dedicated thread for 
	 * dispatching events into WAS asynchronously. 
	 */
	protected AppQueueHandler []_dispatchersArray = null;

	/**
	 * Number of concurrent dispatchers that defines the number of concurrent tasks that can be executed 
	 * through the implementation thread pool 
	 */
	static public int s_dispatchers = 15;

	/**
	 * Defines the number of maximum messages per thread;
	 */
	static int s_maxMsgPerThread;

	/**
	 * When passed the threshold defined by s_onlyCriricalMessagesGetsIn -  only 
	 * critical messages gets in
	 */
	static int s_onlyCriricalMessagesGetsIn;

	/**
	 * When passed the low s_denieLowPeiorityMsgs - low priority 
	 * messages are denied.
	 */
	static int s_rejectLowPriorityMsgs;

	/**
	 * Ctor
	 */
	public NativeMessageDispatchingHandler()
	{
		super();
		if(c_logger.isTraceDebugEnabled()){
			c_logger.traceEntry(this, "pool handler created: " + this);
		}
	}

	/**
	 * @see com.ibm.ws.sip.container.appqueue.MessageDispatchingHandler#start()
	 */
	public void start() {
		initThreadsArray();
	}

	/**
	 * Method that initiates the _dispatchersArray.
	 */
	protected void initThreadsArray() {

		SipPropertiesMap props = PropertiesStore.getInstance().getProperties();
		try{
			s_maxDispatchMessages  = props.getInt( CoreProperties.MAX_MSG_QUEUE_SIZE);

			s_queueSizeInDispatcher = props.getInt(CoreProperties.MSG_QUEUE_INITIAL_SIZE);
			
			s_dispatchers = props.getInt(CoreProperties.CONCURRENT_CONTAINER_TASKS);

		}
		catch(NumberFormatException e){
			c_logger.traceDebug("WASXMessageDispatcher.getThreadPool(): custom property  must be numeric!"); 
		}

		changeDispatcherCount(s_dispatchers);

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this,"initThreadsArray","initializing SIP Container Application queues number:" +s_dispatchers
					+" each queue size:" + s_queueSizeInDispatcher);
		}
		fillArray(s_queueSizeInDispatcher);
	}

	/**
	 * Change the number of known Sip Application queues
	 * @param dispatchers
	 * @author mordechai
	 */
	protected void changeDispatcherCount(int dispatchers){
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "changeDispatcherCount" ,dispatchers);
		}
		s_maxMsgPerThread = s_maxDispatchMessages / s_dispatchers;
		s_onlyCriricalMessagesGetsIn = s_maxDispatchMessages;
		s_rejectLowPriorityMsgs = s_maxMsgPerThread >> 1; //Moti: devide by 2
		_dispatchersArray = new AppQueueHandler[s_dispatchers];
	}

	protected void fillArray(int maxQsize) {
		for (int i = 0; i < _dispatchersArray.length; i++) {
			_dispatchersArray[i] = new DispatcherWorkingThread(maxQsize,i,this);
		}
	}

	/**
	 * @see com.ibm.ws.sip.container.appqueue.MessageDispatchingHandler#stop()
	 */
	public void stop() {
		// TODO Liberty reset existing queues, or wait till drained

	}

	/**
	 * @see com.ibm.ws.sip.container.appqueue.MessageDispatchingHandler#dispatch(com.ibm.ws.sip.container.util.Queueable)
	 */
	public boolean dispatch(Queueable msg){
		return dispatch( msg, -1);
	}	

	/**
	 * @see com.ibm.ws.sip.container.appqueue.MessageDispatchingHandler#dispatch(com.ibm.ws.sip.container.util.Queueable, long)
	 */
	public boolean dispatch(Queueable msg, long blockTimeout) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "dispatch message = " + msg +" blockTime="+blockTimeout);
		}

		int index = msg.getQueueIndex();
		if(index < 0){
			//this should never happen.
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "dispatch", "got negative queue index. check flow. message omitted:"+msg);
			}
			return false;
		}

		boolean result = getQueueToProcess(index, msg).dispatchMessage(msg, blockTimeout);
		if (result) {
			PerformanceMgr.getInstance().incrementInvokeCounter();
		}
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "dispatch result=" + result+" for="+msg);
		}
		return result;
	}
	
	/**
	 * returning the queue according to the queueable index
	 * @param index
	 * @return
	 */
	protected AppQueueHandler getQueueToProcess(int index, Queueable msg){
		index = index % s_dispatchers;
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "getQueueToProcess", "sending msg to queue no:"+index + " msg="+msg +
					" handler Q:"+_dispatchersArray[index].getClass().getName());
		}
		return _dispatchersArray[index];
	}

	/**
	 *  @see com.ibm.ws.sip.container.was.QueueLoadListener#queueChanged(int)
	 */
	public void queueChanged() {

		long allQueuesSize = 0;

		for (int i = 0; i < _dispatchersArray.length; i++) {
			AppQueueHandler dispatcher = _dispatchersArray[i];
			allQueuesSize += dispatcher.getQueueLoad();
		}
		//note that this method is not synchronized, and so allQueuesSize will 
		//not be completely accurate at all times, but it will be accurate enough to 
		//always eventually show the maximum value for weight calculation. It is possible that when 
		//all load is off counter will show size of 1....
		
		PerformanceMgr.getInstance().setQueueSize(allQueuesSize);
	}

	/**
	 * This method prints information about all the DispatcherWorkingThread.
	 */
	public void printState() {
		StringBuffer buff = new StringBuffer("Statistic:");
		for (int i = 0; i < _dispatchersArray.length; i++) {
			AppQueueHandler thread = _dispatchersArray[i];
			buff.append(thread.getInfo());
			buff.append('|');
		}
		c_logger.info(buff.toString(), Situation.SITUATION_REPORT_STATUS);
	}
}
