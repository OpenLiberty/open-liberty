/*******************************************************************************
 * Copyright (c) 2017, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.persistent.osgi.internal;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.ejb.ScheduleExpression;

import junit.framework.Assert;

import org.junit.Test;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.service.metadata.internal.J2EENameImpl;
import com.ibm.ws.ejbcontainer.util.ParsedScheduleExpression;
import com.ibm.ws.ejbcontainer.util.ScheduleExpressionParser;

public class PersistentTimerTaskHandlerImplTest {

    private static Date utcDate(int year, int month, int day, int hour, int minute, int second, int millis) {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(GregorianCalendar.MILLISECOND, millis);
        return cal.getTime();
    }

    /**
     * Creates a handler for a calendar timer that has a single expiration at
     * 2020-01-01 00:00:05 UTC, with the specified schedule end date. Since the
     * schedule is in the past, getFirstTimeout() always returns -1.
     */
    private static PersistentTimerTaskHandlerImpl createPastCalendarHandler(Date end) {
        ScheduleExpression expr = new ScheduleExpression().timezone("UTC").year("2020").month("1").dayOfMonth("1").hour("0").minute("0").second("5");
        if (end != null) {
            expr.end(end);
        }
        ParsedScheduleExpression parsedExpr = ScheduleExpressionParser.parse(expr);
        J2EEName j2eeName = new J2EENameImpl("PersistentTimerApp", "PTimerEJB", "Bean");
        return new PersistentTimerTaskHandlerImpl(j2eeName, null, parsedExpr);
    }

    /**
     * The timer creation time passed to getNextTimeout is when the task was added to the
     * persistent store, which may be after the end of the schedule. The catch up run must
     * not be later than the end, and the timer must have no more timeouts after that run.
     */
    @Test
    public void testGetNextTimeoutCatchUpCreationAfterEnd() {
        // end is rounded up to the nearest second: 2020-01-01 00:00:06
        PersistentTimerTaskHandlerImpl handler = createPastCalendarHandler(utcDate(2020, 1, 1, 0, 0, 5, 500));
        Date expectedEnd = utcDate(2020, 1, 1, 0, 0, 6, 0);

        // Task added to the persistent store long after the schedule end
        Date creationTime = new Date();
        Date catchUp = handler.getNextTimeout(null, creationTime);
        Assert.assertEquals(expectedEnd, catchUp);

        // No more timeouts after the catch up run; must not throw IllegalArgumentException
        Assert.assertNull(handler.getNextTimeout(catchUp, null));
    }

    /**
     * When the timer creation time is before the end of the schedule, the catch up run
     * is the next second after the creation time (milliseconds removed).
     */
    @Test
    public void testGetNextTimeoutCatchUpCreationBeforeEnd() {
        PersistentTimerTaskHandlerImpl handler = createPastCalendarHandler(utcDate(2020, 1, 1, 0, 0, 10, 0));

        Date catchUp = handler.getNextTimeout(null, utcDate(2020, 1, 1, 0, 0, 1, 250));
        Assert.assertEquals(utcDate(2020, 1, 1, 0, 0, 2, 0), catchUp);
    }

    /**
     * When the schedule has no end, the catch up run is the next second after
     * the creation time (milliseconds removed).
     */
    @Test
    public void testGetNextTimeoutCatchUpNoEnd() {
        PersistentTimerTaskHandlerImpl handler = createPastCalendarHandler(null);

        Date catchUp = handler.getNextTimeout(null, utcDate(2026, 6, 1, 12, 30, 15, 750));
        Assert.assertEquals(utcDate(2026, 6, 1, 12, 30, 16, 0), catchUp);
    }

    @Test
    public void testGetTaskName() {
        J2EEName j2eeName = new J2EENameImpl("PersistentTimerApp", "PTimerEJB", "Bean");
        PersistentTimerTaskHandlerImpl testHandler = new PersistentTimerTaskHandlerImpl(j2eeName, null, new Date(), 10000L);
        Assert.assertEquals("!EJBTimerP!PTimerEJB#Bean", testHandler.getTaskName());

        j2eeName = new J2EENameImpl("PersistentTimerApp", "PTimerEJB", "Bean_");
        testHandler = new PersistentTimerTaskHandlerImpl(j2eeName, null, new Date(), 10000L);
        Assert.assertEquals("!EJBTimerP!PTimerEJB#Bean_", testHandler.getTaskName());

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P_TimerEJB", "Bean_");
        testHandler = new PersistentTimerTaskHandlerImpl(j2eeName, null, new Date(), 10000L);
        Assert.assertEquals("!EJBTimerP!P_TimerEJB#Bean_", testHandler.getTaskName());

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P\\TimerEJB", "Bean%");
        testHandler = new PersistentTimerTaskHandlerImpl(j2eeName, null, new Date(), 10000L);
        Assert.assertEquals("!EJBTimerP!P\\TimerEJB#Bean%", testHandler.getTaskName());

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P__TimerEJB", "Bean\\%");
        testHandler = new PersistentTimerTaskHandlerImpl(j2eeName, null, new Date(), 10000L);
        Assert.assertEquals("!EJBTimerP!P__TimerEJB#Bean\\%", testHandler.getTaskName());

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P\\_%_TimerEJB", "Bean_%\\");
        testHandler = new PersistentTimerTaskHandlerImpl(j2eeName, null, new Date(), 10000L);
        Assert.assertEquals("!EJBTimerP!P\\_%_TimerEJB#Bean_%\\", testHandler.getTaskName());
    }

    @Test
    public void testGetTaskNameModulePattern() {
        Assert.assertEquals("!EJBTimer_!PTimerEJB#%", PersistentTimerTaskHandlerImpl.getTaskNameModulePattern("PTimerEJB"));
        Assert.assertEquals("!EJBTimer_!P\\_TimerEJB#%", PersistentTimerTaskHandlerImpl.getTaskNameModulePattern("P_TimerEJB"));
        Assert.assertEquals("!EJBTimer_!P\\%TimerEJB#%", PersistentTimerTaskHandlerImpl.getTaskNameModulePattern("P%TimerEJB"));
        Assert.assertEquals("!EJBTimer_!P\\\\TimerEJB#%", PersistentTimerTaskHandlerImpl.getTaskNameModulePattern("P\\TimerEJB"));
        Assert.assertEquals("!EJBTimer_!P\\_\\_TimerEJB#%", PersistentTimerTaskHandlerImpl.getTaskNameModulePattern("P__TimerEJB"));
        Assert.assertEquals("!EJBTimer_!P\\_\\\\\\_TimerEJB#%", PersistentTimerTaskHandlerImpl.getTaskNameModulePattern("P_\\_TimerEJB"));
        Assert.assertEquals("!EJBTimer_!P\\_\\%\\\\\\_TimerEJB#%", PersistentTimerTaskHandlerImpl.getTaskNameModulePattern("P_%\\_TimerEJB"));
    }

    @Test
    public void testGetTaskNameBeanPattern() {
        J2EEName j2eeName = new J2EENameImpl("PersistentTimerApp", "PTimerEJB", "Bean");
        Assert.assertEquals("!EJBTimer_!PTimerEJB#Bean", PersistentTimerTaskHandlerImpl.getTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "PTimerEJB", "Bean_");
        Assert.assertEquals("!EJBTimer_!PTimerEJB#Bean\\_", PersistentTimerTaskHandlerImpl.getTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P_TimerEJB", "Bean");
        Assert.assertEquals("!EJBTimer_!P\\_TimerEJB#Bean", PersistentTimerTaskHandlerImpl.getTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P_TimerEJB", "Bean_");
        Assert.assertEquals("!EJBTimer_!P\\_TimerEJB#Bean\\_", PersistentTimerTaskHandlerImpl.getTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P%TimerEJB", "Bean%");
        Assert.assertEquals("!EJBTimer_!P\\%TimerEJB#Bean\\%", PersistentTimerTaskHandlerImpl.getTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P\\%TimerEJB", "Bean\\");
        Assert.assertEquals("!EJBTimer_!P\\\\\\%TimerEJB#Bean\\\\", PersistentTimerTaskHandlerImpl.getTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P__TimerEJB", "Bean__");
        Assert.assertEquals("!EJBTimer_!P\\_\\_TimerEJB#Bean\\_\\_", PersistentTimerTaskHandlerImpl.getTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P_\\_TimerEJB", "Bean_%_");
        Assert.assertEquals("!EJBTimer_!P\\_\\\\\\_TimerEJB#Bean\\_\\%\\_", PersistentTimerTaskHandlerImpl.getTaskNameBeanPattern(j2eeName));
    }

    @Test
    public void testGetAutomaticTimerTaskNameBeanPattern() {
        J2EEName j2eeName = new J2EENameImpl("PersistentTimerApp", "PTimerEJB", "Bean");
        Assert.assertEquals("!EJBTimerA!PTimerEJB#Bean", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "PTimerEJB", "Bean_");
        Assert.assertEquals("!EJBTimerA!PTimerEJB#Bean\\_", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P_TimerEJB", "Bean");
        Assert.assertEquals("!EJBTimerA!P\\_TimerEJB#Bean", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P_TimerEJB", "Bean_");
        Assert.assertEquals("!EJBTimerA!P\\_TimerEJB#Bean\\_", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P%TimerEJB", "Bean%");
        Assert.assertEquals("!EJBTimerA!P\\%TimerEJB#Bean\\%", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P\\%TimerEJB", "Bean\\");
        Assert.assertEquals("!EJBTimerA!P\\\\\\%TimerEJB#Bean\\\\", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P__TimerEJB", "Bean__");
        Assert.assertEquals("!EJBTimerA!P\\_\\_TimerEJB#Bean\\_\\_", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameBeanPattern(j2eeName));

        j2eeName = new J2EENameImpl("PersistentTimerApp", "P_\\_TimerEJB", "Bean_%_");
        Assert.assertEquals("!EJBTimerA!P\\_\\\\\\_TimerEJB#Bean\\_\\%\\_", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameBeanPattern(j2eeName));
    }

    @Test
    public void getAutomaticTimerTaskNameModulePattern() {
        Assert.assertEquals("!EJBTimerA!PTimerEJB#%", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameModulePattern("PTimerEJB"));
        Assert.assertEquals("!EJBTimerA!P\\_TimerEJB#%", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameModulePattern("P_TimerEJB"));
        Assert.assertEquals("!EJBTimerA!P\\%TimerEJB#%", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameModulePattern("P%TimerEJB"));
        Assert.assertEquals("!EJBTimerA!P\\\\TimerEJB#%", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameModulePattern("P\\TimerEJB"));
        Assert.assertEquals("!EJBTimerA!P\\_\\_TimerEJB#%", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameModulePattern("P__TimerEJB"));
        Assert.assertEquals("!EJBTimerA!P\\_\\\\\\_TimerEJB#%", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameModulePattern("P_\\_TimerEJB"));
        Assert.assertEquals("!EJBTimerA!P\\_\\%\\\\\\_TimerEJB#%", PersistentTimerTaskHandlerImpl.getAutomaticTimerTaskNameModulePattern("P_%\\_TimerEJB"));
    }

    @Test
    public void getAutomaticTimerPropertyName() {
        Assert.assertEquals("!EJBTimerA!PTimerEJB#Bean", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyName("PTimerEJB", "Bean"));
        Assert.assertEquals("!EJBTimerA!PTimerEJB#Bean_", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyName("PTimerEJB", "Bean_"));
        Assert.assertEquals("!EJBTimerA!P_TimerEJB#Bean", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyName("P_TimerEJB", "Bean"));
        Assert.assertEquals("!EJBTimerA!P%TimerEJB#Bean\\", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyName("P%TimerEJB", "Bean\\"));
        Assert.assertEquals("!EJBTimerA!P%_TimerEJB#Bean\\_", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyName("P%_TimerEJB", "Bean\\_"));
        Assert.assertEquals("!EJBTimerA!P__TimerEJB#Bean_%\\_", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyName("P__TimerEJB", "Bean_%\\_"));
    }

    @Test
    public void getAutomaticTimerPropertyPattern() {
        Assert.assertEquals("!EJBTimerA!PTimerEJB#Bean", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyPattern("PTimerEJB", "Bean"));
        Assert.assertEquals("!EJBTimerA!PTimerEJB#Bean\\_", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyPattern("PTimerEJB", "Bean_"));
        Assert.assertEquals("!EJBTimerA!P\\_TimerEJB#Bean", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyPattern("P_TimerEJB", "Bean"));
        Assert.assertEquals("!EJBTimerA!P\\%TimerEJB#Bean\\\\", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyPattern("P%TimerEJB", "Bean\\"));
        Assert.assertEquals("!EJBTimerA!P\\%\\_TimerEJB#Bean\\\\\\_", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyPattern("P%_TimerEJB", "Bean\\_"));
        Assert.assertEquals("!EJBTimerA!P\\_\\_TimerEJB#Bean\\_\\%\\\\\\_", PersistentTimerTaskHandlerImpl.getAutomaticTimerPropertyPattern("P__TimerEJB", "Bean_%\\_"));
    }
}
