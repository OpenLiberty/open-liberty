/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.util.am;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 *
 */
public class AlarmManager {

	private static ScheduledExecutorService executorService;

	/**
	 * Scheduled executor service for deferrable alarms.
	 */
	public static final AtomicServiceReference<ScheduledExecutorService> executorServiceRef = new AtomicServiceReference<ScheduledExecutorService>(
			"scheduledExecutor");

	private static class AlarmRunnable implements Runnable {
		AlarmListener alarmListener = null;
		Object context = null;

		AlarmRunnable(AlarmListener alarmListener, Object context) {
			this.alarmListener = alarmListener;
			this.context = context;
		}

		public void run() {
			alarmListener.alarm(context);
		}
	}

	public static Alarm createNonDeferrable(long delay,
			AlarmListener alarmListener) {
		return createNonDeferrable(delay, alarmListener, null);
	}

	public static Alarm createDeferrable(long delay, AlarmListener alarmListener) {
		return createDeferrable(delay, alarmListener, null);
	}

	public static Alarm createNonDeferrable(long delay,
			AlarmListener alarmListener, Object context) {
		ScheduledFuture future = null;
		AlarmImpl alarm = null;
		if (executorService != null) {
			future = executorService.schedule(new AlarmRunnable(alarmListener,
					context), delay, TimeUnit.MILLISECONDS);
			if (future != null)
				alarm = new AlarmImpl(future);
		}
		return alarm;
	}

	public static Alarm createDeferrable(long delay,
			AlarmListener alarmListener, Object context) {
		ScheduledFuture future = null;
		AlarmImpl alarm = null;
		if (executorService != null) {
			future = executorService.schedule(new AlarmRunnable(alarmListener,
					context), delay, TimeUnit.MILLISECONDS);
			if (future != null)
				alarm = new AlarmImpl(future);
		}
		return alarm;
	}

	/**
	 * Declarative Services method to activate this component. Best practice:
	 * this should be a protected method, not public or private
	 * 
	 * @param context
	 *            context for this component
	 */
	protected void activate(ComponentContext context) {
		executorServiceRef.activate(context);
		executorService = executorServiceRef.getServiceWithException();
	}

	protected void deactivate(ComponentContext context) {
		executorServiceRef.deactivate(context);
		executorService = null;
	}

	/**
	 * Declarative Services method for setting the deferrable scheduled executor
	 * service reference.
	 * 
	 * @param ref
	 *            reference to the service
	 */
	protected void setScheduledExecutor(
			ServiceReference<ScheduledExecutorService> ref) {
		executorServiceRef.setReference(ref);
	}

	/**
	 * Declarative Services method for unsetting the non-deferrable scheduled
	 * executor service reference.
	 * 
	 * @param ref
	 *            reference to the service
	 */
	protected void unsetScheduledExecutor(
			ServiceReference<ScheduledExecutorService> ref) {
		executorServiceRef.unsetReference(ref);
	}

}
