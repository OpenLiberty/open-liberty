/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.appqueue;

/**
 * @author anat
 * 
 * Class which is a base for DispatcherWorkingThread and for 
 * ContextBasedQueue objects.
 */
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.container.pmi.TaskDurationMeasurer;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.util.Queueable;
import com.ibm.ws.sip.container.was.ThreadLocalStorage;
import com.ibm.ws.sip.properties.CoreProperties;
//TODO Liberty the following imports don't longer exist as we don't support HA in Liberty
//import com.ibm.ws.sip.container.failover.FailoverMgr;
//import com.ibm.ws.sip.container.failover.FailoverMgrLoader;

public abstract class AppQueueHandler {
	
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(AppQueueHandler.class);

	/**
	 * The dispatching queue
	 */
	protected ArrayBlockingQueue _queue;
	
	/**
	 * Represents the Id of this thread in the NativeMessageDispatcher
	 */
	private int _id;

	/**
	 * Listener that should be updated by this Dispatcher about changes in the
	 * queue load
	 */
	private QueueLoadListener _lstr;
	
	/**
	 * Counter which will calculate the queue size. To prevent call to 
	 * _queue.size() which has sync inside.
	 * @update varibale atomice :Moti: made this atomic
	 */
	private AtomicInteger _msgCounter = new AtomicInteger(0);
	//private long _msgCounter = 0;
	
	/**
     * This object handle will be the synchronize blocker of a thread that sent 
     * low priority message, when blocking mode is requested 
     */
	private Object LOW_PRIORITY_BLOCKER = new Object();
    
    /**
     * This object handle will be the synchronize blocker of a thread that sent 
     * normal priority message, when blocking mode is requested 
     */
	private Object NORMAL_PRIORITY_BLOCKER = new Object();

	/**
     * Failover manager - used for failover mechanism
     */ 
    /*TODO Liberty private static final transient FailoverMgr s_failoverMgr = FailoverMgrLoader.getMgrInstance();*/

    /**
     * Moti: added counters here to get improved stats regarding the rate
     * of msg processing
     */
    private int m_msgsProcessedSinceLastStats = 0;
    private long m_lastStatsReportTime  = 0;
        
	/**
	 * Ctor
	 * @param waitingQueueSize
	 * @param id
	 * @param lstr
	 */
	public AppQueueHandler(int waitingQueueSize, int id, QueueLoadListener lstr){
		_id = id;
		_lstr = lstr;
		
		//Retrieve the burst factor of the message queue
		int queueBurstFactor = PropertiesStore.getInstance().getProperties().getInt(CoreProperties.MESSAGE_QUEUE_BURST_FACTOR);
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "AppQueueHandler", "Message queue burst factor is: " + queueBurstFactor);
		}
		
		_queue = new ArrayBlockingQueue( //We set capacity to s_maxDispatchMessages * burstFactor which is the limit for sum of all queues, 
										//since we do not want the queue to be blocked on capacity 
				NativeMessageDispatchingHandler.s_maxDispatchMessages * queueBurstFactor, true);
		
	}
	
	/**
	 * This method adding new message that should be dispatch the siplet to the
	 * _waitingRequestsQueue queue
	 * This method is not blocking
	 * @param msg
	 * @return false if capacity reached and message couldn't be process, true otherwise
	 */
	public boolean dispatchMessage(Queueable msg) {
		return dispatchMessage(msg, -1);
	}
	
	/**
	 * Updates all the listeners about finishing the work on the Runnable.	 *
	 */
	protected void finishToExecuteRunnable() {
		
		try {
			reportToFailoverServiceEnded();
		} catch(Throwable e) {
			//catch and continue so that exceptions on the replication will not kill the thread and 
			//make the queue it was working on never processed again, and filled up forever
			if(c_logger.isErrorEnabled()){
				c_logger.error("End of service replicatin failed ", null, e);
			}
		}	
		
		try {
			//At this point the message starts to be handled by one of the threads.
			//we inform the performance manager that there is one less message in the queue.
			updatePerfrmance();
			
			// At this point message was already removed from the queue - 
			// update _lstr about this change;
			//Moti: testing: disable counters locks
	    	msgExecuted();
	    	
	    	unblockWaitingThreads();

		} catch (Throwable th) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this,"finishToExecuteRunnable", th.getMessage(), th);
			}	
		}
	}
	
	/**
	 * Set Invalidate When Ready to TUs*
	 */
	protected void invalidateWhenReadyTU() {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "invalidateWhenReadyTU");
		}
		try{
			Vector tu = ThreadLocalStorage.getTuForInvalidate();
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this,"invalidateWhenReadyTU", "tu list =" + tu);
			}
			if(tu != null){
				for (Iterator iter = tu.iterator(); iter.hasNext();) {
					TransactionUserWrapper element = (TransactionUserWrapper) iter.next();
					// when we get here the transaction might be already invalidated by
					// the application.
					boolean valid = element.isValid();
					boolean hasTransactions = element.hasOngoingTransactions();
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this,"invalidateWhenReadyTU", "check tu =" + element +
								", valid="+valid + ", hasTransactions="+hasTransactions);
					}
					if (valid && !hasTransactions) {
						element.invalidateWhenReady();
					}
				}
				
			}
			
		} catch (Throwable th){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this,"invalidateWhenReadyTU", th.getMessage(), th);
			}
		} finally {
			ThreadLocalStorage.cleanTuForInvalidate();
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceExit(this, "invalidateWhenReadyTU");
			}
		}
	}
	
	/**
	 * Extracts the message from the _queue.
	 * @return
	 */
	protected Queueable getRunnableObj(){
		Queueable msg = null;		
		try{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getRunnableObj",
				"waiting for queue to be filled with something.");
			}
			//Moti: OG: while working with ObjectGrid
			// we encounter a state where take fails and throws InterruptedException
			// we should handle such situation more gracefully.
			msg = (Queueable)_queue.take(); // getNextRunnableObj();
	
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getRunnableObj",
						"Q had:"+(_queue.size()+1)  +" elements. Dispatching= " + msg);
			}
		}catch (InterruptedException ex) {
			//Moti: if we got here then someone called thread.interrupt()
			// the return value will be null so expect a NPE in the flow too.
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getRunnableObj",
						" had InterruptedException:"+ex.getLocalizedMessage());
			}
			//Moti:10/Nov/2008 clear the interrupted flag. 
			Thread.currentThread().interrupted();
			
		}
		return msg;
	}

	/**
	 * This method adding new message that should be dispatch the siplet to the
	 * _waitingRequestsQueue queue
	 * @param msg
	 * @param time in MS to block thread when capacity reached
	 * @return false if capacity reached and message couldn't be process, true otherwise
	 */
	public boolean dispatchMessage(Queueable msg, long blockingTimeout) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "dispatchMessage(2)", new Object[] {_id,_queue.size()});
		}
			
		if(!canMessageBeProcessed(msg, blockingTimeout)){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "dispatchMessage", "can not process message:"+msg + 
						" msg will not be queued.");
			}
			return false;
		}
		if (_queue.offer(msg)) {
			msgAdded();
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceExit(this, "dispatchMessage(2)",new Integer[] { _id, _queue.size()});
			}

		    PerformanceMgr perfMgr = PerformanceMgr.getInstance();
			
			if (perfMgr != null && perfMgr.isTaskDurationProcessingQueuePMIEnabled() && msg != null) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "dispatchMessage",
							"Start measuring new task duration in SIP container queue");
				}
				msg.setSipContainerQueueDuration(new TaskDurationMeasurer());
				//start measuring task duration in the container queue 
				msg.getSipContainerQueueDuration().startMeasuring();
			}
			
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "dispatchMessage",
						"Update QueueMonitoringModule processing queue statistics - task queued");
			}
			if (perfMgr != null) {
				perfMgr.updateQueueMonitoringTaskQueuedInProcessingQueue();
			}
			
			return true;
		}
		//If the queue is full, don't block it, just fail the message
		return false;
	}
	


	/**
	 * Check whether the message can be processed 
	 * @param msg
	 * @return
	 * @throws InterruptedException 
	 */
	private boolean canMessageBeProcessed(Queueable msg, long blockingTimeout){
		try{
			int currentState = getQueueLoad();
			if (currentState > NativeMessageDispatchingHandler.s_onlyCriricalMessagesGetsIn
				&& msg.priority() <= Queueable.PRIORITY_NORMAL){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "canMessageBeProcessed", "we passed the threshold.");
				}
				//we passed the threshold, now only critical messages gets in.
				if( blockingTimeout >= 0){
					block(NORMAL_PRIORITY_BLOCKER, blockingTimeout);
					return true;
				}
				return false;
			}
			
			if (currentState > NativeMessageDispatchingHandler.s_rejectLowPriorityMsgs && 
				msg.priority() <= Queueable.PRIORITY_LOW){
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "canMessageBeProcessed", "we passed low watermark.");
				}
				//we passed the low watermark, low priority messages are denied.
				if( blockingTimeout >= 0){
					if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "canMessageBeProcessed","Q load:"+currentState
							+" threshold:"+NativeMessageDispatchingHandler.s_rejectLowPriorityMsgs+
							" Qid="+_id);
					}
					block(LOW_PRIORITY_BLOCKER, blockingTimeout);
					return true;
				}
				return false;
			}
		} catch (InterruptedException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "canMessageBeProcessed", "block interrupted", e);
			}
		}
		return true;
	}

	/**
	 * Unblocking waiting threads, according to priority 
	 */
	protected void unblockWaitingThreads(){
		int currentQstate= getQueueLoad();
		if (currentQstate < NativeMessageDispatchingHandler.s_maxMsgPerThread){
			unblock( NORMAL_PRIORITY_BLOCKER);
		}
		if( currentQstate < NativeMessageDispatchingHandler.s_rejectLowPriorityMsgs){
			unblock( LOW_PRIORITY_BLOCKER);
		}
	}
	
	/**
	 * Unblocking the threads that are waiting on the blocker handle
	 * @param blocker
	 */
	private void unblock( Object blocker){
		synchronized(blocker){
			blocker.notifyAll();
		}
	}
	
	/**
	 * Make the calling thread wait on a blocker handle 
	 * @param blocker
	 * @param blockingTimeout
	 * @throws InterruptedException
	 */
	protected void block( Object blocker, long blockingTimeout) throws InterruptedException{
		synchronized(blocker){
			blocker.wait(blockingTimeout);
		}
	}
	
	abstract protected void extractAmsgAndExecute();
	
	/**
	 * Helper method which notifies listener about queue changed.
	 *
	 */
	private void notifyQueueChanged(){
		_lstr.queueChanged();
	}
	
	/**
	 * Number of messages pending in queue
	 * @return
	 */
	public int getQueueLoad(){
		return _msgCounter.get();
	}

	/**
	 * Helper method which called after the message executed.
	 */
	protected void msgExecuted(){
		_msgCounter.decrementAndGet();
		m_msgsProcessedSinceLastStats++;
		notifyQueueChanged();
	}
	
	/**
	 * Helper method which called when new message added to
	 * the working thread
	 */
	protected void msgAdded(){
		_msgCounter.incrementAndGet();
		notifyQueueChanged();
	}

	/**
	 * Return the ID
	 * @return 
	 */
	public int getId() {
		return _id;
	}

	/**
	 * Updates the Failover about Start of executing
	 * 
	 */
	public void reportToFailoverServiceStart() {
		/*TODO Liberty s_failoverMgr.reportServiceStart();*/
	}
	
	/**
	* Updates the Failover about End of executing
	* 
	*/
	protected void reportToFailoverServiceEnded() {
		/*TODO Liberty s_failoverMgr.reportEndOfService();*/
	}
		
	protected void updatePerfrmance() {
		PerformanceMgr perfMgr = PerformanceMgr.getInstance();
		if (perfMgr != null) {
			//At this point the message starts to be handled by one of the threads.
			//we inform the performance manager that there is one less message in the queue.
			perfMgr.decrementInvokeCounter();
		}

	}
	
	/**
	 * Method which creates current statistic info about this DispatcherWorkingThread.
	 * @return
	 */
	public String getInfo()
	{
		long now = System.currentTimeMillis();
		int secPassed  = (int)((now - m_lastStatsReportTime)/1000);
		int rate = 0;
		if (secPassed > 0) {
			 rate = m_msgsProcessedSinceLastStats/secPassed;
		}
		m_msgsProcessedSinceLastStats = 0;
		m_lastStatsReportTime = now;
		StringBuffer msg = new StringBuffer(10);
		msg.append(getQueueLoad());
		if (rate >0) {
			msg.append(',').append(rate).append("m/s"); //messages per seconds
		}
		return  msg.toString();
	}
	
}
