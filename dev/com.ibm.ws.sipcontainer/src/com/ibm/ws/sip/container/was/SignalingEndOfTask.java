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

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sip.container.appqueue.NativeMessageDispatchingHandler;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.util.Queueable;
import com.ibm.ws.sip.properties.CoreProperties;

/**
 * This class is created 1 per each RoutedTask (or Queueable/Runnable)
 * SIP container message. This is actually a wrapper class for a RoutedTask.
 * The wrapping has a special purpose:
 * Its sole purpose is to signal its parent when this task has been executed
 * by the owner thread pool.
 * We could use wait/notify but I rather let the code run
 * and not block. 
 * @author mordechai
 *
 */
class SignalingEndOfTask  implements Runnable
{
	
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(SignalingEndOfTask.class);
	
	/**
	 * A reference to the caller of this class so we can signal
	 * it when the msg we execute (in the WAS thread pool) is done. 
	 */
	ContextBasedQueue m_parent;

	/**
	 * The message to be executed.
	 */
	protected Queueable m_msg ;

	public SignalingEndOfTask(ContextBasedQueue parent, Queueable msg )
	{
		m_parent = parent;
		m_msg = msg;
	}

	/**
	 * This method will be called by the WAS thread pool
	 * executor. It will cause the Message to do its task.
	 * after that it will signal the parent Queue that it is done
	 */
	public void run() 
	{
		boolean hangedDetected = false;
		try{
			//store the queue id on the current thread
			ThreadLocalStorage.setQueueId(m_msg.getQueueIndex() % NativeMessageDispatchingHandler.s_dispatchers);
			setLogExtOnThread();
			
			m_parent.reportToFailoverServiceStart();
			m_parent.recordThreadID();
			if (c_logger.isTraceDebugEnabled() ){
				c_logger.traceDebug(this,"run","going to run queueable: " + m_msg);
			}
			m_msg.run();
			if (c_logger.isTraceDebugEnabled() ){
				c_logger.traceDebug(this,"run","finished to run queueable: " + m_msg);
			}
			if (m_parent.getRecordedThreadID() == null) {
				if (c_logger.isTraceDebugEnabled() ){
					c_logger.traceDebug(this,"run","this thread was marked as hung.");
				}
				hangedDetected = true;
			}
			m_parent.unrecordThreadID();
		}catch (IllegalStateException e) {
        	FFDCFilter.processException(e, "com.ibm.ws.sip.container.was.SignalingEndOfTask.run", "1");
		} catch (Throwable th){
			 //moti: instead of having the thread killed and creating a new one
			// in the pool we just print the exception.
			if (c_logger.isErrorEnabled()) {
				c_logger.error(th.getLocalizedMessage(),Situation.SITUATION_REPORT,null,th);
			}
		} finally{
			removeLogExtFromThread();
			
			if (!hangedDetected) {
				m_parent.finishedToExecuteTask(m_msg);
			}
			m_msg = null;
			
			//remove the queue id from the current thread
			ThreadLocalStorage.setQueueId(null);
		}
		
	}
	
	/**
	 * @return the wrapped task
	 */
	public Queueable getTask(){
		return m_msg;
	}
	
	/**
	 * Sets some SIP information on thread local for 
	 * HPEl log extension feature.
	 */
	private void setLogExtOnThread() {
		//Use custom property to enable/disable the feature
    	if (PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.ENABLE_HPEL_SIP_LOG_EXTENSION)) {
    		//Store the SIP Application Session or TUWrapper on the current thread (for log debugging)
			if (m_msg.getApplicationSession() != null) {
				ThreadLocalStorage.setApplicationSession(m_msg.getApplicationSession());
			} else {
				ThreadLocalStorage.setTuWrapper(m_msg.getTuWrapper());
			}
    	}
	}
	
	/**
	 * Removes the HPEl log extension information
	 * from thread local.
	 */
	private void removeLogExtFromThread() {
		//Use custom property to enable/disable the feature
    	if (PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.ENABLE_HPEL_SIP_LOG_EXTENSION)) {
    		//Remove the SIP Application Session & TUWrapper from the current thread
			ThreadLocalStorage.setApplicationSession(null);
			ThreadLocalStorage.setTuWrapper(null);
    	}
	}

}
