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
package com.ibm.ws.sip.container.was;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.appqueue.AppQueueHandler;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.container.pmi.TaskDurationMeasurer;
import com.ibm.ws.sip.container.util.Queueable;

/**
 * This class push messages into the WAS thread pool iff
 * it hasn't already has a msg pushed in the thread pool.
 * There is a flag that tells us if we pushed a task:m_isQhasTaskInThreadPool
 * So basically only one message from this class will be 
 * processed by WAS thread pool at any given time. 
 * The assumption is that all messages belong to the a SIP dialog will be
 * executed by the same ContextBasedQueue instance.
 * This is done to guarntee SIP dialog order and proper processing.
 * This class creates instances of SignalingEndOfTask which are runnables and perform
 * the actuall SIP message processing. We must be carefull not to create simultanious
 * SignalingEndOfTasks (otherwise we might get unexpected order).
 * When the msg is finished executed it will call finishedToExecuteTask
 * which may leave the task:m_isQhasTaskInThreadPool as true and get the next
 * available (if any) task to execute.
 * Since we are using WAS threadpool we have no way to know which threadID is going 
 * to execute our next SignalingEndOfTask. it is randomly chosen from the thread pool.
 * In order to support hung thread detection we 
 * record the last known running thread name. When WAS framework notifies about a hung thread
 * we search if its our thread that got hung. if yes, we make our best effort to ignore 
 * any furture actions from this thread (as it still may be false alarm and the thread might 
 * comes back to life).
 * 
 * @update March 2008: Moti: add support for hanged thread detection.
 * @update March 2008 : Moti: defect 505319 changes in sync block inside dispatchMessage
 * @update May 2008 : Moti: defect 513136.1 changes in locking mechanism due to dead locks
 * race conditions and other bad things.
 */
class ContextBasedQueue extends AppQueueHandler
{
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(ContextBasedQueue.class);

	/**
	 * tells us is WAS thread pool is working on a task 
	 * that was in the m_msgQ. 
	 */
	boolean m_isQhasTaskInThreadPool ;

	/**
	 * A set of all known hanged queues we have.
	 * This is part of hanged application detection and recovery solution.
	 * When the thread monitor indetifies a hanged thread (not active for some time)
	 * This hashset will take negative logic: it will hold a list of 
	 * only the active! threads (i.e. active ContextBasedQueue).
	 * a. we unblock the relevant for other message 
	 * b. if it does wake up we won't crash ourself...
	 * @author mordechai
	 */
	String m_recordedId = null;

	/**
	 * The last task we were processing (if any)
	 */
	private Queueable m_lastExecutedTask;

	private Object m_sipQmutex =  new Object();
	
	private ExecutorMessageDispatchingHandler _handler;
	
	/**
	 * Save the queue id the current thread handles.
	 * This will be used to identify when a new task is about to be dispatched from an application code
	 * that it would be handled on this queue according to task application session.
	 * in this case there is no need to dispatch it and it will be executed once the application calls it 
	 * on the same thread.
	 */
	//TODO might need this
//	private ThreadLocal<Integer> currentQueue = new ThreadLocal<Integer>();

	/**
	 * Ctor
	 * @param maxQueueSize
	 * @param id
	 * @param lstr
	 * @param threadPool
	 */
	public ContextBasedQueue(int maxQueueSize, int id, ExecutorMessageDispatchingHandler handler)
	{
		//Moti: 24/June/2007: changed the method signature to use com.ibm.ws.util namespace
		// it is needed fpr defect 442479 so it will use WAS runtime threads which also
		// have by default hanged thread detection.
		super(maxQueueSize,id,handler);
		m_isQhasTaskInThreadPool = false;
		_handler = handler;
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "ContextBasedQueue", "maxQSize="
					+ maxQueueSize);
		}
	}


	protected void  extractAmsgAndExecute() {
		try {

			Queueable msg = null;
			while (msg == null) {
				// added for defect 564203
				// if we got here exactly once then this is the normal flow of code.
				// However,in WAS XD stress tests we have seen we got InterruptedException
				// in an inner level. then we get NULL message. we decided to ignore this exception
				// and wait on the queue once more.
				msg = getRunnableObj(); 
			}
			
			PerformanceMgr perfMgr = PerformanceMgr.getInstance();
			if (perfMgr != null) {
			
				if (perfMgr.isTaskDurationProcessingQueuePMIEnabled()) {
					//Update the perfromance manager that the task is out of the queue
					if (msg.getSipContainerQueueDuration() != null) {
						perfMgr.measureTaskDurationProcessingQueue(msg
							.getSipContainerQueueDuration().takeTimeMeasurement());
					}
					else if (c_logger.isTraceDebugEnabled()) {
						c_logger.traceDebug(this, "extractAmsgAndExecute", "'takeTimeMeasurment' wasn't called since SipContainerQueueDuration is null");
					}
				}
				
				if(perfMgr.isApplicationDurationPMIEnabled() && msg != null) {
					msg.setApplicationCodeDuration(new TaskDurationMeasurer());
					//update PMI start measuring task duration in application codes
					msg.getApplicationCodeDuration().startMeasuring();
				}
				
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "extractAmsgAndExecute",
							"Update QueueMonitoringModule processing queue statistics - task dequeued");
				}
				perfMgr.updateQueueMonitoringTaskDequeuedFromProcessingQueue();
			}
			
			executeSignaledTask(msg);

		} catch( Throwable e){
			if (c_logger.isErrorEnabled()) {
				c_logger.error("error.exception", null, null, e);
			}
		}
	}

	/**
	 * This method will take the Message (msg)
	 * will wrap it in a SignalingEndOfTask instance so 
	 * it will know when its job is done.
	 * @param msg - a SIP Message. instance of RoutedTask
	 * @throws InterruptedException 
	 * @throws IllegalStateException 
	 */
	private void executeSignaledTask(Queueable msg) throws IllegalStateException, InterruptedException 
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			StringBuffer buff = new StringBuffer();
			buff.append("QId=");
			buff.append(getId());
			buff.append(" Message =  ");
			buff.append(msg);

			c_logger.traceEntry(this, "executeSignaledTask", buff.toString());
		}
		//Moti: no need to lock access to the boolean flag
		// m_isQhasTaskInThreadPool, since this method
		// is already called in a synchronized context.

		m_isQhasTaskInThreadPool = true; 
		//Moti:part of defect 445736 I am adding here code to identify a hanged call . 
		m_lastExecutedTask = msg;
		SignalingEndOfTask task = _handler.wrapTaskWithSignaling(this,msg);

		_handler.execute(task);
	}

	/**
	 * signals that this queue has no tasks for
	 * WAS thread pool. 
	 **/
	public void finishedToExecuteTask(Queueable msg )
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			StringBuffer buff = new StringBuffer();
			buff.append("QId=");
			buff.append(getId());
			buff.append(" Message =  ");
			buff.append(msg);
			//buff.append("Queue size =");
			//buff.append(_queue.size());
			c_logger.traceEntry(this, "finishedToExecuteTask", buff.toString());
		}
		
		PerformanceMgr perfMgr = PerformanceMgr.getInstance();
		if (perfMgr != null && perfMgr.isApplicationDurationPMIEnabled() && msg != null && msg.getApplicationCodeDuration() != null && msg.getAppName() != null && msg.getAppIndexForPMI() != null) {
			perfMgr.measureInApplicationTaskDuration(msg.getAppName(), msg.getAppIndexForPMI(), 
					msg.getApplicationCodeDuration().takeTimeMeasurement());
		}
		
		invalidateWhenReadyTU();
		synchronized (this) {
	//		currentQueue.set(null);
			finishToExecuteRunnable();
			this.notifyAll();
			
			unrecordThreadID();
			
			if (_queue.isEmpty()) {
				m_isQhasTaskInThreadPool = false;
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this,"finishedToExecuteTask", "no more messages in Qid="+ getId() 
							+" .releasing flag");
				}
				return;
			}
			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer buff = new StringBuffer();
				buff.append("QId=").append(getId()).append(" has ");
				buff.append(_queue.size());
				buff.append(" more messages.");
				buff.append(msg);
				c_logger.traceDebug(this,"finishedToExecuteTask", buff.toString());
			}
			//go fetch another task.
			extractAmsgAndExecute();
	
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceExit(this, "finishedToExecuteTask");
			}
		}
	}


	/**
	 * @return true iff the message was buffered and executed.
	 */
	public synchronized boolean dispatchMessage(Queueable msg, long blockingTimeout) {
		//Moti: defect 519215 : need to prevent multiply threads pulling object 
		//from the same queue, the order of execution might become wrong.
		//thus two or more threads will invoke new SignalingTask.
		if (c_logger.isTraceEntryExitEnabled()) {

			StringBuffer buff = new StringBuffer();
			buff.append("QId=");
			buff.append(getId());
			buff.append(" block=");
			buff.append(blockingTimeout);
			buff.append(" taskInQ ");
			buff.append(m_isQhasTaskInThreadPool);
			c_logger.traceEntry(this, "dispatchMessage", buff.toString());
		}
//		currentQueue.set(getId());
		
		// this method is synchornized anyway so only one thread (thread A) can enter it.
		// however , the flag is also depends on a (lets call it thread B) from the 
		// WAS Thread pool (thread B simply got here a minute earlier).
		// This thread (B) eventually calls finishToExecuteTask(...)
		// imagine the "last message" scenario:
		// B was running for a while. The flag is true during that time.
		// B check if queue is empty. suddenly it is empty. now context switch...
		// A enters this method (dispatchMessage). checks the flag: still true
		// therefore , will simply perform _queue.put  . now context switch...
		// B, with the old info that _queue is empty, set the flag to false.
		// until further message arrives , no one will take care of that
		// poor message that is now in the _queue.
		boolean queued  = false;
		queued  = super.dispatchMessage(msg, -1);

		if (!queued) {
			boolean shouldBlock = blockingTimeout >=0;
			boolean finallyQueued = false;
			if (shouldBlock) {
				finallyQueued = blockMessage(msg);
			}
			if (!finallyQueued) return false;
			queued = true;
		}
	

		//if we got here then queue is not empty
		//synchronized (_queueSizeCounterMutex) {
		// Moti: defect 505319 proved me that I need to sync also the buffer.put
		// which is done by calling super.dispatchMessage(...)
		// sometimes the put is a bit slower than we might think so we must
		// put it inside the sync block.
		// if not : a different thread in the finish2ExecuteTask() may see an "empty" queue.
		if (m_isQhasTaskInThreadPool) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this,"dispatchMessage", 
						"message queued since there is already another message being executed right now. QId=" + 
						getId());
			}
			//we cant execute any new task since
			// there is already some task, orginated by this class
			// which is being executed in the parent thread pool.
			// therefore, simply put it in the App queue and wait.
			return queued;
		}
			
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this,"dispatchMessage", "a new task in queue.will try to process it now.");
		}
		//if we got here then we have a new task in the queue AND the queue is not
		//executing anything. i.e. queue size is 1.
		extractAmsgAndExecute();
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this,"dispatchMessage");
		}
		return true;
	}

	/**
	 * Block this message until someone frees something from the queue.
	 * @param msg
	 * @return false if there was an exception and message wasn't queued.
	 */
	private boolean blockMessage(Queueable msg)
	{
		boolean queued = false;
		try {
			while (!queued) {
				this.wait(); 
				queued  = super.dispatchMessage(msg, -1);
			}
		} catch (InterruptedException e) {
			if (c_logger.isErrorEnabled()) {
				c_logger.error("error.exception", null, null, e);
			}
		}
		return queued;
	}

	/**
	 * 
	 * @param threadId - assumed the thread id is in hex number.
	 * @return true iff the thread (name) was invoked by this queue.
	 * (BTW: iff in computer science is abbreviation to : if and only if
	 */
	boolean reportHangedThread(String threadName)
	{

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this,"reportHangedThread",new Object[] { threadName, m_recordedId});
		}
		return (threadName.equals(m_recordedId));
	}

	void recordThreadID() {
		// when we get here m_recordedId must be non-initialized.
		if (m_recordedId != null) {
			throw new IllegalStateException("There is a different thread still reported running here.");
		}
		//m_recordedId = Thread.currentThread().getId();
		m_recordedId = Thread.currentThread().getName();
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this,"recordThreadID ",m_recordedId);
		}
	}

	void unrecordThreadID() {
		m_recordedId = null;
		m_lastExecutedTask = null;
	}

	/**
	 * @return If there is an active thread running from this queue 
	 * this method will return its threadID.
	 * If this thread was detected hung or current queue is idle it will return -1;
	 */
	String getRecordedThreadID()
	{
		return m_recordedId;
	}

	/**
	 * @return the last known task this queue has executed. 
	 */
	Queueable getLastTask()
	{
		return m_lastExecutedTask;
	}


}
