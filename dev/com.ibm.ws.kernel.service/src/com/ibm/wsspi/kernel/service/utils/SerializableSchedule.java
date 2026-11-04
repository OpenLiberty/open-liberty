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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

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
    private final SortedSet<DayOfWeek> validDays;
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
    SerializableSchedule(DayOfWeek startDay, DayOfWeek endDay, LocalTime startTime, LocalTime endTime, ZoneId timezone) {

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
            throw new IllegalArgumentException(Tr.formatMessage(tc, "invalidScheduleDelimit", originalStr, "MON-FRI 8-17:00 America/Chicago"));
        }

        //Parse DayOfWeek
        //Split "MON-FRI" to [MON, FRI] or "MON" to [MON]
        String[] splitDays = splitDaysTimesZone[0].split("-");

        // Ensure only one or two values
        if (splitDays.length < 1 || splitDays.length > 2)
            throw new IllegalArgumentException(Tr.formatMessage(tc, "invalidScheduleDelimitRange", splitDaysTimesZone[0], "MON-FRI 17:00 Asia/Ho_Chi_Minh"));

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
            throw new IllegalArgumentException(Tr.formatMessage(tc, "invalidScheduleDelimitRange", splitDaysTimesZone[1], "MON 8:00-17:00 Africa/Porto-Novo"));

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
    public SortedSet<DayOfWeek> getValidDays() {
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

    private static SortedSet<DayOfWeek> evaluateValidDays(DayOfWeek startDay, DayOfWeek endDay) {
        TreeSet<DayOfWeek> result = new TreeSet<>();

        //Handle a single day
        if (endDay == null) {
            result.add(startDay);
            return result;
        }

        DayOfWeekIterator iterator = new DayOfWeekIterator(startDay, endDay);

        if (iterator.isComplete()) {
            result.addAll(Arrays.asList(DayOfWeek.values()));
            return result;
        }

        while (iterator.hasNext()) {
            result.add(iterator.next());
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
        boolean validDaysBool = this.validDays == null ? c.validDays == null : (this.validDays.equals(c.validDays));
        boolean startTimeBool = this.startTime == null ? c.startTime == null : this.startTime.equals(c.startTime);
        boolean endTimeBool = this.endTime == null ? c.endTime == null : this.endTime.equals(c.endTime);
        boolean timezoneBool = this.timezone == null ? c.timezone == null : this.timezone.equals(c.timezone);

        return startupBool && validDaysBool && startTimeBool && endTimeBool && timezoneBool;
    }

    /**
     * Method to check if the current server time is within the schedule represented by this object.
     * Note: This method will automatically adjust to use the schedule's timezone, and not the server's timezone.
     *
     * @return boolean - true the current time is within the scheduled represented by this object, false otherwise.
     */
    public boolean checkNow() {
        return checkInstant(ZonedDateTime.now());
    }

    /**
     * <pre>
     * Method to check if a ZonedDateTime (ZDT) is within the schedule represented by this object.
     *
     * To check the current server time use {@link #checkNow()} instead.
     *
     * This method will convert the ZDT provided to the timezone that this schedule uses.
     * This method will use the date associated with the ZDT provided to actualize the conceptual start/end times this schedule represents.
     * Then it will check if the provided ZDT is within the schedule.
     *
     * If this schedule represents the moment the server starts, then withinSchedule will always return false.
     * </pre>
     *
     * @param now - ZonedDateTime representing an instance in time
     * @return boolean - true the time provided is within the scheduled represented by this object, false otherwise.
     */
    public boolean checkInstant(ZonedDateTime now) {
        if (isStartup()) {
            return false;
        }

        //Adjust provided time to the same timezone as this object
        if (now.getZone() != timezone) {
            now = now.withZoneSameInstant(timezone);
        }

        // If day is not valid don't check time
        if (!validDays.contains(now.getDayOfWeek())) {
            return false;
        }

        // Actualize the conceptual start/end times of this object into a real world ZDT
        // That way we can compare them with the ZDT provided to this method.
        LocalDate today = now.toLocalDate();
        ZonedDateTime zonedStartTime = ZonedDateTime.of(startTime.atDate(today), timezone);
        ZonedDateTime zonedEndTime = ZonedDateTime.of(endTime.atDate(today), timezone);

        return zonedStartTime.isBefore(zonedEndTime) ? // Don't need to worry about wrapped time
                        (zonedStartTime.isBefore(now) && now.isBefore(zonedEndTime)) : //  [ xxx start --- now --- end xxx ]
                        (now.isBefore(zonedEndTime) || zonedStartTime.isBefore(now)); //  [ --- now --- end xxx start --- now --- ]
    }

    /**
     * Iterator that has a circular cursor that will traverse days of the
     * week from a start to an end.
     *
     * <pre>
     * Example:
     * Input: start=FRI end=TUE
     *
     * Day of week      | MON | TUE | WED | THU | FRI | SAT | SUN |
     * Day value        |  1  |  2  |  3  |  4  |  5  |  6  |  7  |
     * Cursor index     |  0  |  1  |  2  |  3  |  4  |  5  |  6  |
     * Cursor Positions --------end^             ^start -----------
     * </pre>
     *
     */
    static class DayOfWeekIterator implements Iterator<DayOfWeek> {

        //DayOfWeek values are 1-7, this iterator uses indices 0-6
        private static final int DayIndexOffset = 1;

        //Index should wrap at at this boundary
        private static final int DayIndexBoundary = 7;

        private final int start;
        private final int end;
        private final boolean linearMode;

        private int cursor;

        public DayOfWeekIterator(DayOfWeek start, DayOfWeek end) {
            if (completeAlgorithm(start.getValue() - DayIndexOffset, end.getValue() - DayIndexOffset)) {
                //All days are represented by iterator therefore order doesn't matter and we don't need to cycle
                this.linearMode = true;
                this.start = 0;
                this.end = 6;
                this.cursor = this.start - 1;
            } else {
                //Order does matter and we need to make sure the cursor will cycle
                this.linearMode = false;
                this.start = start.getValue() - DayIndexOffset;
                this.end = end.getValue() - DayIndexOffset;
                this.cursor = Math.floorMod(this.start - 1, DayIndexBoundary);
            }
        }

        @Override
        public boolean hasNext() {
            return cursor != end;
        }

        @Override
        public DayOfWeek next() {
            if (linearMode) {
                cursor++;
            } else {
                cursor = (cursor + 1) % DayIndexBoundary;
            }
            return DayOfWeek.of(cursor + DayIndexOffset);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Unsupported method remove");
        }

        /**
         * Heuristic algorithm to determine if the iterator contains all possible elements
         */
        public boolean isComplete() {
            return completeAlgorithm(this.start, this.end);
        }

        private static boolean completeAlgorithm(int start, int end) {
            return (end + 1) % DayIndexBoundary == start;
        }
    }
}
