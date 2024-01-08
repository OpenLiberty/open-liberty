/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
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
package io.openliberty.concurrent.internal.cdi.interceptor;

import java.time.DayOfWeek;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.enterprise.concurrent.CronTrigger;
import jakarta.enterprise.concurrent.Schedule;

/**
 * Inherits from the CronTrigger class to expose the next(ZonedDateTime) method
 * so that multiple triggers can compute from the same time.
 */
class ScheduleCronTrigger extends CronTrigger {

    private static final Month[] ALL_MONTHS = Month.values();

    private static final int[] ALL_DAYS_OF_MONTH = //
                    new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                                11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                                21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
                                31 };

    private static final DayOfWeek[] ALL_DAYS_OF_WEEK = //
                    new DayOfWeek[] { DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                                      DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
                                      DayOfWeek.SATURDAY };

    private static final int[] ALL_HOURS = //
                    new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
                                12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 };

    private static final int[] ALL_MINUTES = //
                    new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                                10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
                                20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
                                30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
                                40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
                                50, 51, 52, 53, 54, 55, 56, 57, 58, 59 };

    @Trivial
    private ScheduleCronTrigger(String cron, ZoneId zoneId) {
        super(cron, zoneId);
    }

    @Trivial
    private ScheduleCronTrigger(ZoneId zoneId) {
        super(zoneId);
    }

    /**
     * Create a CronTrigger from a Schedule.
     *
     * @param schedule the schedule.
     * @return the trigger.
     */
    @Trivial
    static ScheduleCronTrigger create(Schedule schedule) {
        ScheduleCronTrigger trigger;

        String zone = schedule.zone();
        ZoneId zoneId = zone.length() == 0 ? ZoneId.systemDefault() : ZoneId.of(zone);

        String cron = schedule.cron();

        if (cron.length() > 0) {
            trigger = new ScheduleCronTrigger(cron, zoneId);
        } else {
            trigger = new ScheduleCronTrigger(zoneId);

            Month[] months = schedule.months();
            if (months.length == 0)
                months = ALL_MONTHS;

            int[] daysOfMonth = schedule.daysOfMonth();
            if (daysOfMonth.length == 0)
                daysOfMonth = ALL_DAYS_OF_MONTH;

            DayOfWeek[] daysOfWeek = schedule.daysOfWeek();
            if (daysOfWeek.length == 0)
                daysOfWeek = ALL_DAYS_OF_WEEK;

            int[] hours = schedule.hours();
            if (hours.length == 0)
                hours = ALL_HOURS;

            int[] minutes = schedule.minutes();
            if (minutes.length == 0)
                minutes = ALL_MINUTES;

            int[] seconds = schedule.seconds();
            if (seconds.length == 0)
                throw new IllegalArgumentException("seconds: {}"); // TODO NLS The seconds field cannot be ignored because no other units of Schedule are more granular.

            trigger.months(months) //
                            .daysOfMonth(daysOfMonth) //
                            .daysOfWeek(daysOfWeek) //
                            .hours(hours) //
                            .minutes(minutes) //
                            .seconds(seconds);
        }
        return trigger;
    }

    @Override
    protected ZonedDateTime next(ZonedDateTime now) {
        return super.next(now);
    }

    @Trivial
    @Override
    public String toString() {
        // ScheduleCronTrigger@...
        return "Schedule" + super.toString();
    }
}