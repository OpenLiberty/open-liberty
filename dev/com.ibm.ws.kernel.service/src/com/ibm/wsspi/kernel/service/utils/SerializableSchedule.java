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
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.Traceable;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;

/**
 * Schedule data structure class for a scheduled time when a feature should be activated.
 */
public class SerializableSchedule implements Serializable, Traceable, FFDCSelfIntrospectable {

    static final TraceComponent tc = Tr.register(SerializableSchedule.class);

    private static final long serialVersionUID = 1L;

    private final boolean startup;
    private final List<DayOfWeek> validDays;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final ZoneId timezone;

    /**
     * Constructor for a SerializableSchedule where the scheduled time is set by explicitly providing all fields.
     *
     * @param startDay  - The startDay of a schedule range, or a single day if endDay is null. This value cannot be set to null using this constructor.
     * @param endDay    - the endDay of a schedule range.
     * @param startTime - The startTime of a schedule range, or a moment in time if endTime is null. This value cannot be set to null using this constructor.
     * @param endTime   - the endTime of a schedule range.
     * @param timezone  - the timezone of a schedule. If null will use server timezone.
     */
    public SerializableSchedule(DayOfWeek startDay, DayOfWeek endDay, LocalTime startTime, LocalTime endTime, ZoneId timezone) {

        //This constructor is only used in testing.  No need to translate this exception.
        if (startDay == null || startTime == null) {
            throw new IllegalArgumentException("Cannot instantiate a SerializableSchedule without and startDay and or startTime");
        }

        this.validDays = evaluateValidDays(startDay, endDay);
        this.startTime = startTime;
        this.endTime = endTime;
        this.timezone = timezone == null ? ZoneId.systemDefault() : timezone;
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
        this.timezone = null;
    }

    /**
     * Constructor for a SerializableSchedule where the scheduled time is provided as a string to be parsed.
     * This string will be parsed with the following rules:
     *
     * <pre>
     * 1. If the string is "startup" the scheduled time is not set and instead a flag is set to be checked during server startup.
     * 2. Day(s), time(s), and timezone are delimited by a space " "
     *    - Example "MON 8:00" or "MON-FRI 8:00-13:00 America/Chicago"
     * 3. A range of days or times are delimited by a hyphen "-"
     *    - Example "MON-FRI 8:00" or "Tues 8:00-13:00"
     * </pre>
     *
     * @param strVal - the string to be parsed
     */
    public SerializableSchedule(String strVal) {
        //Sanitize string "  mon - fri  8:00  -  17:00  GMT+6  " -> "mon-fri 8:00-17:00 GMT+6"
        String originalStr = strVal;
        strVal = strVal.trim().replaceAll("( )+", " ").replaceAll("( - )", "-");

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Sanitize Input", originalStr, strVal);
        }

        //Handle STARTUP case
        if (strVal.equalsIgnoreCase("STARTUP")) {
            this.startup = true;
            this.validDays = null;
            this.startTime = null;
            this.endTime = null;
            this.timezone = null;
            return;
        } else {
            this.startup = false;
        }

        //Split "MON-FRI 8:00-10:00 America/Chicago"  to [MON-FRI, 8:00-10:00, America/Chicago]
        String[] splitDaysTimesZone = strVal.split(" ");

        if (splitDaysTimesZone.length < 2 || splitDaysTimesZone.length > 3) {
            throw new IllegalArgumentException(Tr.formatMessage(tc, "invalidScheduleDelimit", originalStr, "MON-FRI 8-17:00 GMT-6"));
        }

        //Parse DayOfWeek
        //Split "MON-FRI" to [MON, FRI] or "MON" to [MON]
        String[] splitDays = splitDaysTimesZone[0].split("-");

        // Ensure only one or two values
        if (splitDays.length < 1 || splitDays.length > 2)
            throw new IllegalArgumentException(Tr.formatMessage(tc, "invalidScheduleDelimitRange", splitDaysTimesZone[0], "MON-FRI"));

        //Attempt to parse
        try {
            this.validDays = evaluateValidDays(evaluateDayOfWeek(splitDays[0]), splitDays.length == 2 ? evaluateDayOfWeek(splitDays[1]) : null);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(Tr.formatMessage(tc, "invalidScheduleParse", DayOfWeek.class, splitDaysTimesZone[0], DayOfWeek.values()), e);
        }

        //Parse LocalTime
        //Split "8:00-17:00" to [8:00, 17:00] or "8:00" to [8:00]
        String[] splitTimes = splitDaysTimesZone[1].split("-");

        //Ensure only one or two values
        if (splitTimes.length < 1 || splitTimes.length > 2)
            throw new IllegalArgumentException(Tr.formatMessage(tc, "invalidScheduleDelimitRange", splitDaysTimesZone[1], "8:00-17:00"));

        //Attempt to parse
        try {
            this.startTime = evaluateLocalTime(splitTimes[0]);
            this.endTime = splitTimes.length == 2 ? evaluateLocalTime(splitTimes[1]) : null;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(Tr.formatMessage(tc, "invalidScheduleParse", LocalTime.class, splitDaysTimesZone[1], "[8:00, 17:00]"), e);
        }

        //Parse ZoneID
        try {
            this.timezone = splitDaysTimesZone.length == 3 ? ZoneId.of(splitDaysTimesZone[2]) : ZoneId.systemDefault();
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(Tr.formatMessage(tc, "invalidScheduleParse", ZoneId.class, splitDaysTimesZone[2],
                                                                ZoneId.systemDefault().getDisplayName(TextStyle.SHORT, Locale.getDefault())), e);
        }
    }

    /**
     * @return the startup
     */
    public boolean isStartup() {
        return startup;
    }

    /**
     * @return the validDays
     */
    public List<DayOfWeek> getValidDays() {
        return validDays;
    }

    /**
     * @return the startTime
     */
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * @return the endTime
     */
    public LocalTime getEndTime() {
        return endTime;
    }

    /**
     * @return the timezone
     */
    public ZoneId getTimezone() {
        return timezone;
    }

    private static List<DayOfWeek> evaluateValidDays(DayOfWeek startDay, DayOfWeek endDay) {
        //Handle a single day
        if (endDay == null) {
            return Arrays.asList(startDay);
        }

        //Need to iterate through days of week. Easier to use position in a circular array [0-6]
        int startDayPosition = startDay.getValue() - 1; //Adjust since DayOfWeek uses values 1-7
        int endDayPosition = endDay.getValue() % 7; //Adjust if endDay is Sunday (7) we want to iterate till Monday (0).

        //User has specified all days
        if (startDayPosition == endDayPosition) {
            return Arrays.asList(DayOfWeek.values());
        }

        ArrayList<DayOfWeek> result = new ArrayList<>();

        //Increment through circular array [0-6]
        for (int day = startDayPosition; day != endDayPosition; day = (day + 1) % 7) {
            result.add(DayOfWeek.of(day + 1));
        }

        return result;
    }

    private static DayOfWeek evaluateDayOfWeek(String strVal) throws IllegalArgumentException {
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
                return DayOfWeek.valueOf(strVal.toUpperCase());

        }
    }

    /**
     * Try to parse string representation of time to meet format requirements in
     * {@link java.time.format.DateTimeFormatter#ISO_LOCAL_TIME}
     *
     * @param strVal represents a unit of time, not null
     */
    private static LocalTime evaluateLocalTime(String strVal) throws DateTimeParseException {
        char[] chars = strVal.toCharArray();

        //Do our best to accommodate different ways of providing a local time.
        if (strVal.length() == 1) { //assume user provided only an hour "8"
            strVal = "0" + strVal + ":00";
        } else if (strVal.length() == 2 && chars[1] != ':') { //assume user provided "11" or "08"
            strVal = strVal + ":00";
        } else if (strVal.length() == 4 && chars[1] == ':') { //assume user provided "8:00"
            strVal = "0" + strVal;
        }

        return LocalTime.parse(strVal);

    }

    @Override
    public String[] introspectSelf() {
        return new String[] { "startup=" + startup, "validDays=" + validDays, "startTime=" + startTime, "endTime=" + endTime, "timezone=" + timezone };
    }

    @Override
    public String toTraceString() {
        return toString();
    }

    @Override
    public String toString() {
        return "SerializableSchedule [startup=" + startup + ", validDays=" + validDays + ", startTime=" + startTime + ", endTime=" + endTime + ", timezone=" + timezone + "]";
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
        boolean validDaysBool = this.validDays == null ? c.validDays == null : (this.validDays.containsAll(c.validDays) && c.validDays.containsAll(this.validDays));
        boolean startTimeBool = this.startTime == null ? c.startTime == null : this.startTime.equals(c.startTime);
        boolean endTimeBool = this.endTime == null ? c.endTime == null : this.endTime.equals(c.endTime);
        boolean timezoneBool = this.timezone == null ? c.timezone == null : this.timezone.equals(c.timezone);

        return startupBool && validDaysBool && startTimeBool && endTimeBool && timezoneBool;
    }

    /**
     * Method to determine if the current server time is within the schedule represented by this object.
     * Note: This method will automatically adjust to use the schedule's timezone, and not the server's timezone.
     *
     * @return boolean - true the time provided is within the scheduled represented by this object, false otherwise.
     */
    public boolean withinSchedule() {
        return withinSchedule(ZonedDateTime.now());
    }

    /**
     * Method to determine if a LocalDateTime is within the schedule represented by this object.
     * Note: This method assumes that the LocalDateTime provided represents a ZonedDateTime within the timezone of this schedule.
     *
     * @param time - LocalDateTime
     * @return boolean - true the time provided is within the scheduled represented by this object, false otherwise.
     */
    public boolean withinSchedule(LocalDateTime time) {
        return withinSchedule(time.atZone(timezone));
    }

    /**
     * <pre>
     * Method to determine if a ZonedDateTime is within the schedule represented by this object.
     *
     * To check the current server time use {@link #withinSchedule()} instead.
     * To check a LocalDateTime without the timezone component use {@link #withinSchedule(LocalDateTime)} instead.
     *
     * This method will convert the time provided to the timezone that this schedule uses.
     * Then it will check if that time is within the schedule.
     *
     * If this schedule represents the moment the server starts, then withinSchedule will always return false.
     * </pre>
     *
     * @param time - ZonedDateTime
     * @return boolean - true the time provided is within the scheduled represented by this object, false otherwise.
     */
    public boolean withinSchedule(ZonedDateTime time) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "withinSchedule", time);
        }

        if (isStartup()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "withinSchedule", "No moment in time will ever be within this schedule since it represents the server startup time.", this);
            }
            return false;
        }

        //Adjust provided time to the same timezone as this object
        if (time.getZone() != timezone) {
            time = time.withZoneSameInstant(timezone);
        }

        final boolean result;

        // If day is not valid don't check time
        if (!validDays.contains(time.getDayOfWeek())) {
            result = false;
        } else {
            LocalTime check = time.toLocalTime();
            result = startTime.isBefore(endTime) ? // Dont need to worry about wrapped time
                            (startTime.isBefore(check) && check.isBefore(endTime)) : // [ xxx start --- check --- end xxx ]
                            (check.isBefore(endTime) || startTime.isBefore(check)); //  [ --- check --- end xxx start --- check --- ]
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "withinSchedule", result);
        }

        return result;

    }

}
