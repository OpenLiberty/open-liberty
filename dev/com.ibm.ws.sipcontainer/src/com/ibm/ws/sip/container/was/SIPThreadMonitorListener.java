/*******************************************************************************
 * Copyright (c) 2003,2013 IBM Corporation and others.
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
//TODO Liberty import com.ibm.ws.runtime.service.ThreadMonitor;

/**
 * This class listens for all WebSphere thread pools to see if some thread is hung.
 * This class is part of a solution for defect 445746 - Detection and recovery of hung 
 * SIP applications.
 * @author mordechai 5/Mar/2008
 *
 */
public class SIPThreadMonitorListener /*TODO Liberty implements ThreadMonitor.Listener*/ {

	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(SIPThreadMonitorListener.class);
	
	/**
	 * a back java reference so we can notify the Message dispatcher
	 * of a hung thread.
	 */
	private ExecutorMessageDispatchingHandler m_parent;
	
	public SIPThreadMonitorListener(ExecutorMessageDispatchingHandler parent)
	{
		m_parent= parent;
	}
	
	
	public void threadIsClear(Thread thread,String threadId,long msHunged)
	{
		if( c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "threadIsClear", new Object[] {thread.getName(),threadId,msHunged });
	    }
	}

	/**
	 * @deprecated - old API - left here for backword compatibility
	 */
	public void threadIsClear(String threadName, String threadId, long msHunged) {
		if( c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "threadIsClear", new Object[] {threadName,threadId,msHunged });
	    }
	}

	/**
	 * This method is invoked when a thread is believed to be hung.
	 */
	public void threadIsHung(Thread thread, String threadNumber, long timeActiveInMillis)
	{
		String threadName = thread.getName();
		if( c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "threadIsHung", new Object[] {threadName,threadNumber,timeActiveInMillis });
	    }
		m_parent.reportHangedThread(threadName);
	}
	
	/**
	 * This method is invoked when a thread is believed to be hung.
	 * @deprecated - old API - left here for backword compatibility
	 */
	public void threadIsHung(String threadName, String threadId, long msHunged) {
		if( c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "threadIsHung", new Object[] {threadName,threadId,msHunged });
	    }
		m_parent.reportHangedThread(threadName);
	}

	/**
	 * This method is invoked when a hung thread is dumped.
	 */
	public String threadIsDumped(Thread thread, String threadNumber, long msHunged) { // F82074-63388
		if( c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(this, "threadIsDumped", new Object[] {thread,threadNumber,msHunged });
		}
		return "";
	}

}
