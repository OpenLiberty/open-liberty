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
package com.ibm.ws.sip.container.timer;

import java.util.concurrent.ScheduledFuture;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.properties.CoreProperties;

/**
 * A scheduled task, created the SIp container timer service
 * 
 * @author Nitzan Nissim
 */
public class SipTimerTask implements Runnable {

	/**
	 * Class Logger.
	 */
	private static final LogMgr c_logger = Log.get(SipTimerTask.class);

	/**
	 * Reference to the BaseTimer object that will be used to
	 * activate it.
	 */
	private BaseTimer m_timer;

	/** statistics reporting - interval, in milliseconds, for reporting statistics */
	private static final int s_reportInterval;

	/** Task Future which allows task cancellation */
	private ScheduledFuture<?> scheduledFuture;

	static {
		s_reportInterval = PropertiesStore.getInstance().getProperties().
				getInt(CoreProperties.TIMER_STAT_REPORT_INTERVAL);
	}

	/** statistics reporting - timr of last report */
	private static long s_lastReport = 0;

	/** statistics reporting - total instances */
	private static int s_instances = 0;

	/** statistics reporting - total invoked timers */
	private static int s_invoked = 0;

	/** statistics reporting - total cancelled timers */
	private static int s_cancelled = 0;

	/** statistics reporting - number of known timer types */
	private static final int NTYPES = 6;

	/** statistics reporting - instances distribution by type */
	private static int[] s_instancesByType = new int[NTYPES];

	/** statistics reporting - invocations distribution by type */
	private static int[] s_invokedByType = new int[NTYPES];

	/** statistics reporting - cancellations distribution by type */
	private static int[] s_cancelledByType = new int[NTYPES];

	/** statistics reporting - lock for synchronizing the counters */
	private static final Object s_reportLock = new Object();

	/**
	 * reports statistics to system out
	 */
	private static void report() {
		// <type> [<total val>/<total val>]<space><id num1>-<val>/<val><space><id num2>-<val>/<val><space> ... <id numn>-<val>/<val><space>
		// statistics enabled
		long now = System.currentTimeMillis();
		if (now - s_lastReport < s_reportInterval) {
			return;
		}
		s_lastReport = now;
		StringBuffer report = new StringBuffer(1024);
		report.append("SipTimerTask [").append(s_invoked);
		report.append('/').append(s_cancelled);
		report.append('/').append(s_instances).append(']');
		for (int i = 0; i < NTYPES; i++) {
			report.append(' ');
			report.append(i).append('-').append(s_invokedByType[i]);
			report.append('/').append(s_cancelledByType[i]);
			report.append('/').append(s_instancesByType[i]);
		}
		System.out.println(report.toString());
	}

	/** gets a numeric type given the timer instance */
	static private int timerType(BaseTimer timer) {
		int type;
		final String className = timer.getClass().getName();
		if (className.endsWith(".ExpirationTimer")) {
			type = 1;
		}
		else if (className.endsWith(".Invite2xxRetransmitTimer")) {
			type = 2;
		}
		else if (className.endsWith(".ReliabeResponseRetransmitTimer")) {
			type = 3;
		}
		else if (className.endsWith(".SequencialSearchTimer")) {
			type = 4;
		}
		else if (className.endsWith(".ServletTimerImpl")) {
			type = 5;
		}
		else {
			type = 0;
		}
		return type;
	}

	/**
	 * Ctor
	 * 
	 * @param timer Reference to the Base timer object
	 */
	public SipTimerTask(BaseTimer timer) {
		m_timer = timer;

		if (s_reportInterval > 0) {
			int type = timerType(timer);
			synchronized (s_reportLock) {
				s_instances++;
				s_instancesByType[type]++;
			}
			report();
		}
	}

		/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(null, "run");
		}
		BaseTimer timer = m_timer;
		if (timer != null){
			if (s_reportInterval > 0) {
				int type = timerType(timer);
				synchronized (s_reportLock) {
					s_invoked++;
					s_invokedByType[type]++;
				}
				report();
			}

			if (!timer.isCancelled()) {
				try {
					timer.fire();
				} catch (Throwable e) {
					FFDCFilter.processException(e, "com.ibm.ws.sip.container.timer.SipTimerTask.run", "1", this);
					if (c_logger.isErrorEnabled()) {
						c_logger.error("error.exception",
								Situation.SITUATION_REPORT_LOG, null, e);
					}
				}
			}
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceExit(null, "run");
			}
		}
	}

	/**
	 * @see java.util.TimerTask#cancel()
	 */
	public boolean cancel() {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(null, "cancel");
		}
		BaseTimer timer = m_timer;
		if ( timer == null) {
			return false;
		}
		boolean canceledSuccessfully = scheduledFuture.cancel(false);
		// nullify m_timer reference, so until the task is removed it's references will be able to be GCed
		m_timer = null;
		int type = timerType(timer);
		synchronized (s_reportLock) {
			s_cancelled++;
			s_cancelledByType[type]++;
		}
		if (canceledSuccessfully && s_reportInterval > 0) {
			report();
		}
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(null, "cancel", canceledSuccessfully);
		}
		return canceledSuccessfully;
	}

	/**
	 * Setting the task Future Allows for task cancellation.
	 * 
	 * @param sFuture
	 */
	public void setScheduledFuture(ScheduledFuture<?> sFuture) {
		scheduledFuture = sFuture;
	}
}
