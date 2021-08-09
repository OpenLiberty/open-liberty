/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.util.alarm;

public interface AlarmManager
{
	/**
	 * <p>
	 * Schedules an alarm.
	 * </p>
	 * 
	 * <p>
	 * Upon the given millisecond time period elapsing the given command
	 * will be run unless the alarm has been cancelled.
	 * </p>
	 *
	 * @param millisecondDelay The time delay in milliseconds after which
	 *                         the command will be run
	 * @param command The listener to notify when the alarm fires
	 * @param context The context to pass to the listener when the alarm fires
	 *                         
	 * @return The scheduled alarm
	 */
	public Alarm scheduleAlarm(long millisecondDelay, AlarmListener listener, Object context);
	
	/**
	 * <p>
	 * Schedules an alarm.
	 * </p>
	 * 
	 * <p>
	 * Upon the given millisecond time period elapsing the given command
	 * will be run unless the alarm has been cancelled.
	 * </p>
	 *
	 * @param millisecondDelay The time delay in milliseconds after which
	 *                         the command will be run
	 * @param command The listener to notify when the alarm fires
	 * @param context The context to pass to the listener when the alarm fires
	 *                         
	 * @return The scheduled alarm
	 */
	public Alarm scheduleAlarm(long millisecondDelay, AlarmListener listener);
	
	/**
	 * <p>
	 * Schedules an alarm that may be deferred. An alarm that is deferrable
	 * may not be run after the given delay period if the system identifies
	 * that there is no workload. In such cases the running of the alarm may
	 * be deferred until the system next has work to process.
	 * </p>
	 * 
	 * <p>
	 * Upon the given millisecond time period elapsing the given command
	 * will be run unless the alarm has been cancelled.
	 * </p>
	 *
	 * @param millisecondDelay The time delay in milliseconds after which
	 *                         the command will be run
	 * @param command The listener to notify when the alarm fires
	 *                         
	 * @return The scheduled alarm
	 */
	public Alarm scheduleDeferrableAlarm(long millisecondDelay, AlarmListener listener, Object context);
	
	/**
	 * <p>
	 * Schedules an alarm that may be deferred. An alarm that is deferrable
	 * may not be run after the given delay period if the system identifies
	 * that there is no workload. In such cases the running of the alarm may
	 * be deferred until the system next has work to process.
	 * </p>
	 * 
	 * <p>
	 * Upon the given millisecond time period elapsing the given command
	 * will be run unless the alarm has been cancelled.
	 * </p>
	 *
	 * @param millisecondDelay The time delay in milliseconds after which
	 *                         the command will be run
	 * @param command The listener to notify when the alarm fires
	 *                         
	 * @return The scheduled alarm
	 */
	public Alarm scheduleDeferrableAlarm(long millisecondDelay, AlarmListener listener);
    
    public void shutdown();
    
    public void shutdownNow();
}
