/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.cal.ejb;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ejb.ScheduleExpression;

public interface NextTimeoutPersistIntf {

    public static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    public static final String nl = System.getProperty("line.separator");
    public static final boolean PERSISTENT = true;
    public static final boolean NON_PERSISTENT = !PERSISTENT;

    public void testYear();

    public void testMonth();

    public void testDayOfMonth();

    public void testDayOfWeek();

    public void testHour();

    public void testMinute();

    public void testSecond();

    public void testTimezone(String timezoneID);

    public void testStart();

    public void testGetSchedule();

    public void testStartLTend();

    public void testStartGEend();

    public Date createCalTimer(ScheduleExpression se, Serializable info);

    public void waitForTimer(long maxWaitTime);

    public Date getNextTimeoutFromExpiration();

    public String getNextTimeoutFailureFromExpiration();

    public String getScheduleString();

    public void clearAllTimers();

}
