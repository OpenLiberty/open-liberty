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
package com.ibm.ws.sip.stack.dispatch;

import java.util.TimerTask;

/**
 * a scheduled task.
 * can be scheduled from any thread.
 * always notified from the dispatch thread.
 * concrete timers should override onExecute() to handle the timer event.
 * 
 * @author ran
 */
public abstract class TimerEvent extends TimerTask implements Event
{
	/**
	 * SIP Call Id associated with the transaction that created this timer.
	 */
	private String _callId;
	
	
	/**
	 * Constructor.
	 *
	 * @param callId SIP Call Id associated with the transaction that created this timer
	 */
	public TimerEvent(String callId) {
		_callId = callId;
	}
	
	/**
	 * called from the timer thread when time has elapsed
	 * @see java.util.TimerTask#run()
	 */
	public final void run() {
		Dispatcher.instance().queueTimerEvent(this);
	}
	
	/**
	 * @return the SIP Call Id associated with the transaction that created this timer
	 */
	public String getCallId() {
		return _callId;
	}
}
