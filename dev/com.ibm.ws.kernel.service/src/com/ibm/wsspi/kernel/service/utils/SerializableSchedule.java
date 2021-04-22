/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ibm.websphere.ras.Traceable;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;

/**
 * Schedule data structure class for a scheduled time when a feature should be activated.
 */
public class SerializableSchedule implements Serializable, Traceable, FFDCSelfIntrospectable {

    private static final long serialVersionUID = 1L;

    private final boolean startup;
    private final List<DayOfWeek> validDays;
    private final LocalTime startTime;
    private final LocalTime endTime;

    /**
     * Constructor for a SerializableSchedule where the scheduled time is set by providing startDay, endDay, startTime, and endTime explicitly.
     *
     * @param startDay  - The startDay of a schedule range, or a single day if endDay is null. This value cannot be set to null using this constructor.
     * @param endDay    - the endDay of a schedule range.
     * @param startTime - The startTime of a schedule range, or a moment in time if endTime is null. This value cannot be set to null using this constructor.
     * @param endTime   - the endTime of a schedule range.
     */
    public SerializableSchedule(DayOfWeek startDay, DayOfWeek endDay, LocalTime startTime, LocalTime endTime) {

        if (startDay == null || startTime == null) {
            throw new IllegalArgumentException("Cannot instantiate a SerializableSchedule without and startDay and/or startTime");

        }

        this.validDays = getValidDays(startDay, endDay);
        this.startTime = startTime;
        this.endTime = endTime;
        this.startup = false;
    }

    /**
     * Constructor for a SerializableSchedule where a scheduled time is not set and instead a flag is set to be checked during server startup.
     */
    public SerializableSchedule() {
        this.startup = true;
        this.validDays = null;
        this.startTime = null;
        this.endTime = null;
    }

    /**
     * Constructor for a SerializableSchedule where the scheduled time is provided as a string to be parsed.
     * This string will be parsed with the following rules:
     *
     * <pre>
     * 1. If the string is "startup" the scheduled time is not set and instead a flag is set to be checked during server startup.
     * 2. Day(s) and Time(s) are delimited by a space " "
     *    - Example "MON 8:00" or "MON-FRI 8:00-13:00"
     * 3. A range of days or times are delimited by a hyphen "-"
     *    - Example "MON-FRI 8:00" or "Tues 8:00-13:00"
     * </pre>
     *
     * Once parsed this object will have the following fields:
     *
     * <pre>
     * - startup: true if scheduled time is not set and instead a flag is set to be checked during server startup, otherwise, false.
     * - startDay: Represents either the start of a range of days, or a scheduled day.
     * - endDay: Represents the end of a range of days.
     * - startTime: Represents either the start of a range of times, or a scheduled time.
     * - endTime: Represents the end of a range of times.
     * </pre>
     *
     * @param strVal - the string to be parsed
     */
    public SerializableSchedule(String strVal) {
        //Handle STARTUP case
        if (strVal.trim().equalsIgnoreCase("STARTUP")) {
            this.startup = true;
            this.validDays = null;
            this.startTime = null;
            this.endTime = null;
            return;
        } else {
            this.startup = false;
        }

        //Split MON-FRI 8:00-10:00 to [MON-FRI, 8:00-10:00] OR
        //Split MON 8:00 to [MON, 8:00]
        strVal = strVal.trim();
        String[] splitDaysTimes = strVal.split(" ");
        if (splitDaysTimes.length != 2) {
            throw new IllegalArgumentException("Could not parse configuration value as a schedule: " + strVal);
        }

        //Split "MON-FRI" and "8:00" to [MON, FRI] [8:00]
        String[] splitDays = splitDaysTimes[0].split("-");
        String[] splitTimes = splitDaysTimes[1].split("-");

        if (splitDays.length > 2 || splitDays.length < 1 || splitTimes.length > 2 || splitTimes.length < 1) {
            throw new IllegalArgumentException("Could not parse days configured in schedule: " + strVal);
        }

        this.validDays = getValidDays(evaluateDayOfWeek(splitDays[0]), splitDays.length == 2 ? evaluateDayOfWeek(splitDays[1]) : null);
        this.startTime = evaluateLocalTime(splitTimes[0]);
        this.endTime = splitTimes.length == 2 ? evaluateLocalTime(splitTimes[1]) : null;
    }

    private static List<DayOfWeek> getValidDays(DayOfWeek startDay, DayOfWeek endDay) {
        //Handle a single day
        if (endDay == null) {
            return new ArrayList<DayOfWeek>(Arrays.asList(startDay));
        }

        //
        ArrayList<DayOfWeek> result = new ArrayList<>();
        for (int d = startDay.getValue(); d <= endDay.getValue(); d = d == 7 ? 1 : d + 1) {
            result.add(DayOfWeek.of(d));
        }

        return result;
    }

    private static DayOfWeek evaluateDayOfWeek(String strVal) {
        try {
            //Will return successfully for monday, MONDAY, Monday
            return DayOfWeek.valueOf(strVal.toUpperCase());
        } catch (IllegalArgumentException e) {
            switch (strVal.toUpperCase()) {
                case "MON":
                    return DayOfWeek.MONDAY;
                case "TUE":
                case "TUES":
                    return DayOfWeek.TUESDAY;
                case "WED":
                    return DayOfWeek.WEDNESDAY;
                case "THU":
                case "THURS":
                    return DayOfWeek.THURSDAY;
                case "FRI":
                    return DayOfWeek.FRIDAY;
                case "SAT":
                    return DayOfWeek.SATURDAY;
                case "SUN":
                    return DayOfWeek.SUNDAY;
                default:
                    throw e;
            }
        }
    }

    /**
     * Try to parse string representation of time to meet format requirements in
     * {@link java.time.format.DateTimeFormatter#ISO_LOCAL_TIME}
     */
    private static LocalTime evaluateLocalTime(String strVal) {
        try {
            //Optimistic string was configured correctly
            return LocalTime.parse(strVal);
        } catch (DateTimeParseException e) {
            //Need to call LocalTime.parse with leading 0
            //Example: 8:00 will throw exception, need to provide "08:00"
            if (strVal.length() == 4) {
                //Safe use of recursion
                //If this fails again the length will no longer be 4 and an exception will be thrown.
                return evaluateLocalTime("0" + strVal);
            }
            throw e;
        }
    }

    @Override
    public String[] introspectSelf() {
        return new String[] { "startup=" + startup, "validDays=" + validDays, "startTime=" + startTime, "endTime=" + endTime };
    }

    @Override
    public String toTraceString() {
        return toString();
    }

    @Override
    public String toString() {
        return "SerializableSchedule [startup=" + startup + ", validDays=" + validDays + ", startTime=" + startTime + ", endTime=" + endTime + "]";
    }

    @Override
    public boolean equals(Object o) {

        if (!(o instanceof SerializableSchedule)) {
            return false;
        }

        if (o == this) {
            return true;
        }

        SerializableSchedule c = (SerializableSchedule) o;

        //Compare fields taking into account possible null values
        boolean startupBool = Boolean.compare(this.startup, c.startup) == 0;
        boolean validDays = this.validDays == null ? c.validDays == null : this.validDays.equals(c.validDays);
        boolean startTimeBool = this.startTime == null ? c.startTime == null : this.startTime.equals(c.startTime);
        boolean endTimeBool = this.endTime == null ? c.endTime == null : this.endTime.equals(c.endTime);

        return startupBool && validDays && startTimeBool && endTimeBool;
    }

    /**
     * <pre>
     * Method to determine if we are within the scheduled day and time within a margin of time.
     * The margin provided should represent the number of seconds between this call and the next time you plan to check the schedule.
     *
     * Example1: if the schedule is "MON 8:00" and you are checking the schedule every 6 minutes then calling
     * this method between 7:57 and 8:03 will return true on Monday.
     *
     * Example2: if the schedule is "MON 8:00-17:00" and you are checking the schedule ever 6 minutes then calling
     * this method between 7:57 and 17:03 will return true Monday.
     *
     * NOTE: if the schedule is "MON 17:00-8:00" this is equivalent to "MON 17:00-0:00" and "MON 00:00-8:00"
     *
     * </pre>
     *
     * @param marginNS - Amount of nanoseconds to set as a margin
     * @return true we are within the schedule, false otherwise.
     */
    public boolean isNowWithinTheSchedule(long marginNS) {

        // If day is not valid don't check time
        if (validDays.contains(LocalDate.now().getDayOfWeek()))
            return false;

        LocalTime startAdjusted = startTime.minusNanos(marginNS / 2);
        LocalTime endAdjusted = endTime.plusNanos(marginNS / 2);
        LocalTime now = LocalTime.now();

        return startAdjusted.isBefore(endAdjusted) ? // Dont need to worry about wrapped time
                        (startAdjusted.isBefore(now) && now.isBefore(endAdjusted)) : // [ xxx start --- now --- end xxx ]
                        (now.isBefore(endAdjusted) || startAdjusted.isBefore(now)); //  [ --- now --- end xxx start --- now --- ]

    }
}
