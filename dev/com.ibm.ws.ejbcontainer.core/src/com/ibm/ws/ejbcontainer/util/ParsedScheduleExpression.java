/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.BitSet;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import javax.ejb.ScheduleExpression;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * A representation of the parsed values from a <tt>ScheduleExpression</tt>,
 * which can be used to calculate timeouts.
 */
public class ParsedScheduleExpression implements Serializable {
    private static final TraceComponent tc = Tr.register(ParsedScheduleExpression.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    private static final long serialVersionUID = 5940938222835797932L; // d639610

    /**
     * The serialization version.
     */
    private static final int VERSION = 1; // d639610

    /**
     * Determine if the bitmask <tt>haystack</tt> contains the set bit
     * <tt>needle</tt>.
     *
     * @param haystack a bitmask of values
     * @param needle   a 0-based position
     * @return <tt>true</tt> if <tt>haystack</tt> contains <tt>needle</tt>
     */
    private static boolean contains(long haystack, int needle) {
        return (haystack & (1L << needle)) != 0;
    }

    /**
     * Returns a bitmask containing all of the bits in <tt>haystack</tt> with
     * higher positions than <tt>needle</tt>.
     *
     * @param haystack a bitmask of values
     * @param needle   a 0-based position
     * @return a bitmask of all bits in haystack with higher positions than
     *         needle.
     */
    private static long higher(long haystack, int needle) {
        // Mask off the low bits.
        return haystack & ~((1L << (needle + 1)) - 1);
    }

    /**
     * Returns the position of the lowest set bit in <tt>haystack</tt>.
     *
     * @param haystack a non-zero bitmask
     * @return a 0-based index of the next set bit
     */
    private static int first(long haystack) {
        return Long.numberOfTrailingZeros(haystack);
    }

    /**
     * Returns the position of the lowest set bit in <tt>haystack</tt>, or 0 if
     * haystack is {@link #WILD_CARD}.
     *
     * @param haystack a possibly zero bitmask
     * @return the lowest set bit
     */
    private static int firstOfWildCard(long haystack) {
        return haystack == WILD_CARD ? 0 : first(haystack);
    }

    /**
     * Converts a <tt>java.util.Calendar</tt> to a string that is formatted for
     * diagnostics.
     *
     * @param cal the calendar instance
     * @return a readable format
     */
    private static String toString(Calendar cal) {
        return String.format("%04d-%02d-%02d %02d:%02d:%02d %s",
                             cal.get(Calendar.YEAR),
                             cal.get(Calendar.MONTH) + 1,
                             cal.get(Calendar.DAY_OF_MONTH),
                             cal.get(Calendar.HOUR_OF_DAY),
                             cal.get(Calendar.MINUTE),
                             cal.get(Calendar.SECOND),
                             cal.getTimeZone().getID());
    }

    /**
     * Value used by all bitmask fields to represent the wild card value. Note
     * that this is the default value that a field will be initialized, which
     * required for fields that do not correspond directly to field in
     * ScheduleExpression, such as lastDaysOfMonth.
     */
    // F7437591.codRev
    static final int WILD_CARD = 0;

    /**
     * The schedule expression that was parsed to create this object.
     */
    private transient ScheduleExpression ivSchedule;

    /**
     * The inclusive millisecond lower bound for the first timeout. This value
     * has been rounded up to the nearest second.
     */
    transient long start;

    /**
     * The inclusive millisecond upper bound for the last timeout. This value
     * has been rounded up to the nearest second.
     */
    transient long end;

    /**
     * A bitmask of seconds to match (0-59), or WILD_CARD.
     */
    transient long seconds;

    /**
     * A bitmask of minutes to match (0-59), or WILD_CARD.
     */
    transient long minutes;

    /**
     * A bitmask of hours to match (0-23), or WILD_CARD.
     */
    transient int hours;

    /**
     * A bitmask of days of the week to match (0-6, Sun-Sat), or WILD_CARD.
     */
    transient int daysOfWeek;

    /**
     * A bitmask of days of the month to match (0-30), or WILD_CARD.
     */
    transient int daysOfMonth;

    /**
     * A bitmask of the last days of the month to match (0-7 representing
     * -7-Last), or WILD_CARD. For example, if "Last, -6, -7" were specified,
     * the bitmask would look like:
     *
     * <pre>
     * 0b1000 0011 (LSB)
     * ^ ^^
     * Last -6 -7
     * </pre>
     */
    transient int lastDaysOfMonth;

    /**
     * A bitmask of the days of the week in the month to match, or WILD_CARD. A
     * position within into the bitmask is (weekInMonth * 7) + dayOfWeek. For
     * example, if "1st Sun, 2nd Mon, 3rd Mon" were specified, the indices would
     * be 0*7+0=0, 1*7+1=8, 2*7+1=15, and the low word would look like:
     *
     * <pre>
     * .. 0000 0000 0000 0000 1000 0001 0000 0001 (LSB)
     * ^ ^ ^
     * 3rd Mon 2nd Mon 1st Sun
     * </pre>
     */
    transient long daysOfWeekInMonth;

    /**
     * A bitmask of the last days of the week to match (0-6 representing
     * Sun-Sat), or WILD_CARD.
     */
    transient int lastDaysOfWeekInMonth;

    /**
     * A list of day of the month ranges that contain a variable upper or
     * lower bound ("Last" or "nth Day").
     */
    transient List<VariableDayOfMonthRange> variableDayOfMonthRanges; // d659945

    /**
     * A bitmask of the months to match (0-11), or WILD_CARD.
     */
    transient int months;

    /**
     * The set of years above 1000 to match (0-8999), or null for wild card.
     * For example, the year 1000 is bit 0 and the year 2000 is bit 1000.
     */
    transient BitSet years;

    /**
     * The non-null timezone to use for calculating timeouts.
     */
    transient TimeZone timeZone;

    /**
     * Only {@link ScheduleExpressionParser.parse} should create instances.
     *
     * @param schedule the expression being parsed
     */
    ParsedScheduleExpression(ScheduleExpression schedule) {
        ivSchedule = schedule;
    }

    @Override
    public String toString() {
        final String nl = System.getProperty("line.separator");
        StringBuilder out = new StringBuilder();
        out.append(super.toString());
        out.append(nl).append("  start:        ").append(start == 0 ? "<none>" : toString(createCalendar(start)));
        out.append(nl).append("  end:          ").append(end == Long.MAX_VALUE ? "<none>" : toString(createCalendar(end)));
        out.append(nl).append("  timeZone:     ").append(timeZone.getID());
        toString(out.append(nl).append("  seconds:      "), seconds, 0, null);
        toString(out.append(nl).append("  minutes:      "), minutes, 0, null);
        toString(out.append(nl).append("  hours:        "), hours, 0, null);
        toString(out.append(nl).append("  daysOfWeek:   "), daysOfWeek, 0, ScheduleExpressionParser.DAYS_OF_WEEK);
        toString(out.append(nl).append("  daysOfMonth:  "), daysOfMonth, 1, null);
        out.append(nl).append("    last:       ");
        toStringLastDaysOfMonth(out);
        out.append(nl).append("    variable:   "); // d659945
        toStringVariableDayOfMonthRanges(out,
                                         "                ", nl);
        out.append(nl).append("  dowInMonth:   ");
        toStringDaysOfWeekInMonth(out);
        out.append(nl).append("    last:       ");
        toStringLastDaysOfWeekInMonth(out);
        toString(out.append(nl).append("  months:       "), months, 0, ScheduleExpressionParser.MONTHS);
        out.append(nl).append("  years:        ");
        toStringYears(out);

        return out.toString();
    }

    private static boolean toStringAddComma(StringBuilder out, boolean any) {
        if (any) {
            out.append(", ");
        }

        return true;
    }

    private static void toString(StringBuilder out, long value, int min, String[] values) {
        if (value == WILD_CARD) {
            out.append("*");
        } else {
            boolean any = false;

            for (int i = 0; i < 64; i++) {
                if (contains(value, i)) {
                    any = toStringAddComma(out, any);
                    out.append(values != null ? values[i] : Integer.toString(i + min));
                }
            }

            if (!any) {
                out.append("<none>");
            }
        }
    }

    private void toStringLastDaysOfMonth(StringBuilder out) {
        if (lastDaysOfMonth == WILD_CARD) {
            out.append("<none>");
        } else {
            boolean any = false;

            for (int i = 0; i <= 7; i++) {
                if (contains(lastDaysOfMonth, i)) {
                    any = toStringAddComma(out, any);
                    out.append(i == 7 ? "Last" : Integer.toString(i - 7));
                }
            }
        }
    }

    private void toStringDaysOfWeekInMonth(StringBuilder out) {
        if (daysOfWeekInMonth == WILD_CARD) {
            out.append("<none>");
        } else {
            boolean any = false;

            for (int weekInMonth = 0; weekInMonth < ScheduleExpressionParser.LAST_WEEK_OF_MONTH; weekInMonth++) {
                for (int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
                    if (contains(daysOfWeekInMonth, weekInMonth * 7 + dayOfWeek)) {
                        any = toStringAddComma(out, any);
                        out.append(ScheduleExpressionParser.WEEKS_OF_MONTH[weekInMonth]).append(' ').append(ScheduleExpressionParser.DAYS_OF_WEEK[dayOfWeek]);
                    }
                }
            }
        }
    }

    private void toStringLastDaysOfWeekInMonth(StringBuilder out) {
        if (lastDaysOfWeekInMonth == WILD_CARD) {
            out.append("<none>");
        } else {
            boolean any = false;

            for (int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
                if (contains(lastDaysOfWeekInMonth, dayOfWeek)) {
                    any = toStringAddComma(out, any);
                    out.append("Last ").append(ScheduleExpressionParser.DAYS_OF_WEEK[dayOfWeek]);
                }
            }
        }
    }

    private void toStringYears(StringBuilder out) {
        if (years == null) {
            out.append('*');
        } else {
            boolean any = false;

            for (int i = ScheduleExpressionParser.MINIMUM_YEAR; i <= ScheduleExpressionParser.MAXIMUM_YEAR; i++) {
                if (years.get(i - ScheduleExpressionParser.MINIMUM_YEAR)) {
                    int end = i;
                    while (end + 1 <= ScheduleExpressionParser.MAXIMUM_YEAR &&
                           years.get(end + 1 - ScheduleExpressionParser.MINIMUM_YEAR)) {
                        end++;
                    }

                    any = toStringAddComma(out, any);
                    if (i != end) {
                        out.append(i).append('-').append(end);
                        i = end;
                    } else {
                        out.append(i);
                    }
                }
            }
        }
    }

    private void toStringVariableDayOfMonthRanges(StringBuilder out, String indent, String nl) {
        if (variableDayOfMonthRanges == null) {
            out.append("<none>");
        } else {
            boolean any = false;

            for (int i = 0; i < variableDayOfMonthRanges.size(); i++) {
                if (any) {
                    out.append(nl).append(indent);
                } else {
                    any = true;
                }

                out.append(variableDayOfMonthRanges.get(i));
            }
        }
    }

    /**
     * Returns expression that was parsed to create this object.
     *
     * @return the expression that was parsed to create this object
     */
    public ScheduleExpression getSchedule() {
        return ivSchedule;
    }

    /**
     * Determines the first timeout of the schedule expression.
     *
     * @return the first timeout in milliseconds, or -1 if there are no timeouts
     *         for the expression
     */
    public long getFirstTimeout() {
        long lastTimeout = Math.max(System.currentTimeMillis(), start);

        // If we're already past the end date, then there is no timeout.
        if (lastTimeout > end) {
            return -1;
        }

        long firstTimeout = getTimeout(lastTimeout, false);

        // If no timeout found, allow for a timer being created to expire one time
        // on the current second boundary; effectively rounding ms off current time
        if (firstTimeout == -1 && lastTimeout != start) {
            firstTimeout = getTimeout(lastTimeout - 999, false);
        }

        return firstTimeout;
    }

    /**
     * Determines the next timeout for the schedule expression.
     *
     * @param lastTimeout the last timeout in milliseconds
     * @return the next timeout in milliseconds, or -1 if there are no more
     *         future timeouts for the expression
     * @throws IllegalArgumentException if lastTimeout is before the start time
     *                                      of the expression
     */
    public long getNextTimeout(long lastTimeout) {
        // Perform basic validation of lastTimeout, which should be a value that
        // was previously returned from getFirstTimeout.

        if (lastTimeout < start) {
            throw new IllegalArgumentException("last timeout " + lastTimeout + " is before start time " + start);
        }

        if (lastTimeout > end) {
            throw new IllegalArgumentException("last timeout " + lastTimeout + " is after end time " + end);
        }

        if (lastTimeout % 1000 != 0) {
            throw new IllegalArgumentException("last timeout " + lastTimeout + " is mid-second");
        }

        return getTimeout(lastTimeout, true);
    }

    /**
     * Creates a calendar object for the specified time using the time zone of
     * the schedule expression.
     *
     * @param the time in milliseconds
     * @return a calendar object
     */
    private Calendar createCalendar(long time) {
        // The specification accounts for gregorian dates only.
        Calendar cal = new GregorianCalendar(timeZone); // d639610
        cal.setTimeInMillis(time);

        return cal;
    }

    /**
     * Determines the next timeout for the schedule expression.
     *
     * @param lastTimeout the last timeout in milliseconds, or the current time
     *                        if reschedule is false
     * @param reschedule  <tt>true</tt> if lastTimeout is an exclusive lower
     *                        bound for the next timeout
     * @return the next timeout in milliseconds, or -1 if there are no more
     *         future timeouts for the expression
     * @throws IllegalArgumentException if the expression contains an invalid
     *                                      attribute value
     */
    private long getTimeout(long lastTimeout, boolean reschedule) {
        Calendar cal = createCalendar(lastTimeout);

        // If this expression is being rescheduled, then add a second to make
        // progress towards the next timeout.
        if (reschedule) {
            cal.add(Calendar.SECOND, 1);
        }
        // d666295 - Otherwise, if this method is called from getFirstTimeout,
        // we're using the current time rather than the start time, and the
        // current time is mid-second, then we also want to advance to the next
        // second.  Note that the parser has already guaranteed that the start
        // time has been rounded.
        else if (lastTimeout != start && lastTimeout % 1000 != 0) {
            cal.set(Calendar.MILLISECOND, 0);
            cal.add(Calendar.SECOND, 1);
        }

        if (!advance(cal)) {
            return -1;
        }

        return cal.getTimeInMillis();
    }

    /**
     * Moves the values of the fields of the specified calendar forward in
     * time until the date/time of the calendar satisfies the constraints of
     * this expression.
     *
     * @param cal the current time
     * @return <tt>true</tt> if the date/time already satisfies the
     *         constraints or could be adjusted forward to match the constraints;
     *         <tt>false</tt> if there are no future dates/times that match the
     *         constraints
     */
    private boolean advance(Calendar cal) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "advance: " + toString(cal));

        // Loop until all conditions are satisfied.  Whenever the value of a
        // field is advanced, all of the more specific fields are reset, and the
        // loop is restarted to recheck constraints.
        //
        // For example, if now="2000-01-01 11:00:00" and hour="1,12",
        // year="2001", we first advance to "2001-01-01 00:00:00" and then to
        // "2001-01-01 01:00:00".  Note that it would be incorrect to not
        // clear the hour field as this would result in "2001-01-01 12:00:00".
        //
        // Note that if MONTH is being advanced, DAY_OF_MONTH must be reset
        // before advancing MONTH to avoid overflow when transitioning from
        // 31-Jan to Feb.  Theoretically, leap seconds would require SECOND to be
        // reset before modifying other values, but java.util.Calendar does not
        // recognize leap seconds, so this is done for consistency only.
        //
        // Since SECOND, MINUTE, HOUR_OF_DAY, and MONTH can only be range values,
        // we reset them to the lowest value in the allowed range to avoid
        // useless loop iterations.  For example, if now="2000-01-01 11:00:00"
        // and second="10" day="2", we first advance SECOND to 10, then we
        // advance the DAY_OF_MONTH to 2.  At this point, there is no reason to
        // reset SECOND to 0 since we will just loop and advance it back to 10.
        //
        // Because this implementation uses java.util.Calendar, spring DST will
        // skip hour(2) (display time jumps from 1:59:59 to 3:00:00), and autumn
        // DST will execute hour(1) only once (adding a second to a Calendar with
        // time 1:00:00 results in 2:00:00 regardless of whether it is the
        // "first" or "second" 1AM).
        for (;;) {
            if (cal.getTimeInMillis() > end) {
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "advance: failed: " + toString(cal));
                return false;
            }

            if (seconds != WILD_CARD) {
                int second = cal.get(Calendar.SECOND);
                if (!contains(seconds, second)) {
                    advance(cal, Calendar.SECOND, Calendar.MINUTE, seconds, second);

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "advanced second: " + toString(cal));
                    continue;
                }
            }

            if (minutes != WILD_CARD) {
                int minute = cal.get(Calendar.MINUTE);
                if (!contains(minutes, minute)) {
                    cal.set(Calendar.SECOND, firstOfWildCard(seconds)); // d659945.1
                    advance(cal, Calendar.MINUTE, Calendar.HOUR_OF_DAY, minutes, minute);

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "advanced minute: " + toString(cal));
                    continue;
                }
            }

            if (hours != WILD_CARD) {
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                if (!contains(hours, hour)) {
                    // d659945.1 - Reset fields using firstOfWildCard.
                    cal.set(Calendar.SECOND, firstOfWildCard(seconds));
                    cal.set(Calendar.MINUTE, firstOfWildCard(minutes));
                    advance(cal, Calendar.HOUR_OF_DAY, Calendar.DAY_OF_MONTH, hours, hour);

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "advanced hour: " + toString(cal));
                    continue;
                }
            }

            if (advanceDayIfNeeded(cal)) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "advanced day: " + toString(cal));

                // d659945.2 - Variable range bounds can result in expressions that
                // never match for any month (for example, "30--2"), so ensure that
                // we terminate when we advance past year 9999.
                int year = cal.get(Calendar.YEAR);
                if (year > ScheduleExpressionParser.MAXIMUM_YEAR) {
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "advance: failed: " + toString(cal));
                    return false;
                }

                continue;
            }

            if (months != WILD_CARD) {
                int month = cal.get(Calendar.MONTH);
                if (!contains(months, month)) {
                    // d659945.1 - Reset fields using firstOfWildCard.
                    cal.set(Calendar.SECOND, firstOfWildCard(seconds));
                    cal.set(Calendar.MINUTE, firstOfWildCard(minutes));
                    cal.set(Calendar.HOUR_OF_DAY, firstOfWildCard(hours));
                    cal.set(Calendar.DAY_OF_MONTH, 1);

                    advance(cal, Calendar.MONTH, Calendar.YEAR, months, month);

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "advanced month: " + toString(cal));
                    continue;
                }
            }

            int year = cal.get(Calendar.YEAR);
            if (years == null) {
                if (year > ScheduleExpressionParser.MAXIMUM_YEAR) {
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "advance: failed: " + toString(cal));
                    return false;
                }
            } else {
                if (!years.get(year - ScheduleExpressionParser.MINIMUM_YEAR)) {
                    // d659945.1 - Reset fields using firstOfWildCard.
                    cal.set(Calendar.SECOND, firstOfWildCard(seconds));
                    cal.set(Calendar.MINUTE, firstOfWildCard(minutes));
                    cal.set(Calendar.HOUR_OF_DAY, firstOfWildCard(hours));
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.MONTH, firstOfWildCard(months));

                    if (!advanceYear(cal, year)) {
                        if (isTraceOn && tc.isEntryEnabled())
                            Tr.exit(tc, "advance: failed to advance past year " + year);
                        return false;
                    }

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "advanced year: " + toString(cal));
                    continue;
                }
            }

            break;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "advance: " + toString(cal));
        return true;
    }

    /**
     * A value greater than the 0-based last day of any month and less than
     * Integer.MAX_VALUE. This is used by the "getNext" methods to indicate
     * that the month should be advanced.
     */
    static final int ADVANCE_TO_NEXT_MONTH = 99;

    /**
     * Checks whether the day field matches the constraints, and advances its
     * value if it does not.
     *
     * @param cal the current time
     * @return <tt>true</tt> if the day field was advanced
     */
    private boolean advanceDayIfNeeded(Calendar cal) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        // Try all of the different mechanisms for matching a day, but only if
        // they are not wild cards.  If one of the "getNext" methods returns
        // the current day, then we are done.  Otherwise, each method will
        // have returned the nearest day of the current month that would
        // satisfy the constraint, or a number >= the last day of the month to
        // indicate there are no matching days this month.  The
        // ADVANCE_TO_NEXT_MONTH constant is defined for convenience.

        int day = cal.get(Calendar.DAY_OF_MONTH) - 1;
        int nextDay = Integer.MAX_VALUE;

        if (daysOfMonth != WILD_CARD) {
            int result = getNextDayOfMonth(day);

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "next dayOfMonth = " + result);

            if (result == day) {
                return false;
            }

            nextDay = Math.min(nextDay, result);
        }

        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH) - 1;

        if (lastDaysOfMonth != WILD_CARD) {
            int result = getNextLastDayOfMonth(day, lastDay);

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "next lastDayOfMonth = " + result);

            if (result == day) {
                return false;
            }

            nextDay = Math.min(nextDay, result);
        }

        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;

        if (daysOfWeek != WILD_CARD) {
            int result = getNextDayOfWeek(day, dayOfWeek);

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "next daysOfWeek = " + result);

            if (result == day) {
                return false;
            }

            nextDay = Math.min(nextDay, result);
        }

        if (daysOfWeekInMonth != WILD_CARD) {
            int result = getNextDayOfWeekInMonth(day, lastDay, dayOfWeek);

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "daysOfWeekInMonth includes " + result);

            if (result == day) {
                return false;
            }

            nextDay = Math.min(nextDay, result);
        }

        if (lastDaysOfWeekInMonth != WILD_CARD) {
            int result = getNextLastDayOfWeekInMonth(day, lastDay, dayOfWeek);

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "next lastDaysOfWeekInMonth = " + result);

            if (result == day) {
                return false;
            }

            nextDay = Math.min(nextDay, result);
        }

        if (variableDayOfMonthRanges != null) {
            int result = getNextVariableDay(day, lastDay, dayOfWeek);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "next variableDayOfMonthRanges = " + result);

            if (result == day) {
                return false;
            }

            nextDay = Math.min(nextDay, result);
        }

        if (nextDay == Integer.MAX_VALUE) {
            // No day constraints are specified for this expression.
            return false;
        }

        // d659945.1 - Reset fields using firstOfWildCard.
        cal.set(Calendar.SECOND, firstOfWildCard(seconds));
        cal.set(Calendar.MINUTE, firstOfWildCard(minutes));
        cal.set(Calendar.HOUR_OF_DAY, firstOfWildCard(hours));

        if (nextDay <= lastDay) {
            // A constraint matched a day within the current month.
            cal.set(Calendar.DAY_OF_MONTH, nextDay + 1);
        } else {
            // There were no matches for the rest of the days in the current
            // month.  Advance to the next month and try again.
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.add(Calendar.MONTH, 1);
        }

        return true;
    }

    /**
     * Moves the value of the specified field forward in time to satisfy
     * constraints of the specified bitmask. If a position higher than
     * needle is found in haystack, then that higher position is set as the
     * value in field. Otherwise, the lowest value in haystack is set as the
     * value in field, and the value in nextField is incremented.
     *
     * <p>For example, if field=Calendar.SECOND, haystack is {30, 40, 50},
     * and needle is 35, then the seconds field will be set to 40, and the
     * minutes field will be unchanged. However, if haystack={10, 20, 30},
     * then the value in the seconds field will be set to 10, and the value of
     * the minutes field will be incremented.
     *
     * @param cal      the current time
     * @param field    a field from <tt>java.util.Calendar</tt>
     * @param field    the next coarser field after <tt>field</tt>; for example,
     *                     if field=Calendar.SECOND, nextField=Calendar.MINUTE
     * @param haystack the bitmask
     * @param needle   the current position in the bitmask; only higher
     */
    private static void advance(Calendar cal, int field, int nextField, long haystack, int needle) {
        long higher = higher(haystack, needle);
        if (higher != 0) {
            cal.set(field, first(higher));
        } else {
            cal.set(field, first(haystack));
            cal.add(nextField, 1);
        }
    }

    /**
     * Returns the next day of the month after <tt>day</tt> that satisfies the
     * dayOfMonth constraint.
     *
     * @param day the current 0-based day of the month
     * @return a value greater than or equal to <tt>day</tt>
     */
    private int getNextDayOfMonth(int day) {
        if (!contains(daysOfMonth, day)) {
            long higher = higher(daysOfMonth, day);
            if (higher != 0) {
                return first(higher);
            }

            return ADVANCE_TO_NEXT_MONTH;
        }

        return day;
    }

    /**
     * Returns the next day of the month after <tt>day</tt> that satisfies
     * the lastDayOfMonth constraint.
     *
     * @param day     the current 0-based day of the month
     * @param lastDay the current 0-based last day of the month
     * @return a value greater than or equal to <tt>day</tt>
     */
    private int getNextLastDayOfMonth(int day, int lastDay) {
        // lastDaysOfMonth = 0b1000 0011  (LSB) = Last, -6, -7
        // Shift left so that "Last" aligns with the last day of the month.
        // For example, for Jan, we want the Last bit in position 31, so we
        // shift 30-7=23 (note: 0-based last day for Jan is 30) resulting in:
        //            bits = 0b0100 0001 1000 0000 0000 0000 0000 0000  (LSB)
        int offset = lastDay - 7;
        long bits = lastDaysOfMonth << offset;

        if (!contains(bits, day)) {
            long higher = higher(bits, day);
            if (higher != 0) {
                return first(higher);
            }

            return ADVANCE_TO_NEXT_MONTH;
        }

        return day;
    }

    /**
     * Returns the next day of the month after <tt>day</tt> that satisfies
     * the dayOfWeek constraint.
     *
     * @param day       the current 0-based day of the month
     * @param dayOfWeek the current 0-based day of the week
     * @return a value greater than or equal to <tt>day</tt>
     */
    private int getNextDayOfWeek(int day, int dayOfWeek) {
        if (!contains(daysOfWeek, dayOfWeek)) {
            long higher = higher(daysOfWeek, dayOfWeek);
            if (higher != 0) {
                return day + (first(higher) - dayOfWeek);
            }

            return day + (7 - dayOfWeek) + first(daysOfWeek);
        }

        return day;
    }

    /**
     * Returns the next day of the month after <tt>day</tt> that satisfies
     * the lastDayOfWeek constraint.
     *
     * @param day       the current 0-based day of the month
     * @param lastDay   the current 0-based last day of the month
     * @param dayOfWeek the current 0-based day of the week
     * @return a value greater than or equal to <tt>day</tt>
     */
    private int getNextDayOfWeekInMonth(int day, int lastDay, int dayOfWeek) {
        int weekInMonth = day / 7;

        for (;;) {
            if (contains(daysOfWeekInMonth, weekInMonth * 7 + dayOfWeek)) {
                return day;
            }

            day++;
            if (day > lastDay) {
                return ADVANCE_TO_NEXT_MONTH;
            }

            if ((day % 7) == 0) {
                weekInMonth++;
            }

            dayOfWeek = (dayOfWeek + 1) % 7;
        }
    }

    /**
     * Returns the next day of the month after <tt>day</tt> that satisfies
     * the lastDaysOfWeekInMonth constraint.
     *
     * @param day       the current 0-based day of the month
     * @param lastDay   the current 0-based last day of the month
     * @param dayOfWeek the current 0-based day of the week
     * @return a value greater than or equal to <tt>day</tt>
     */
    private int getNextLastDayOfWeekInMonth(int day, int lastDay, int dayOfWeek) {
        for (;;) {
            if (lastDay - day < 7 && contains(lastDaysOfWeekInMonth, dayOfWeek)) {
                return day;
            }

            day++;
            if (day > lastDay) {
                return ADVANCE_TO_NEXT_MONTH;
            }

            dayOfWeek = (dayOfWeek + 1) % 7;
        }
    }

    /**
     * Returns the next day of the month after <tt>day</tt> that satisfies
     * the variableDayOfMonthRanges constraint.
     *
     * @param day       the current 0-based day of the month
     * @param lastDay   the current 0-based last day of the month
     * @param dayOfWeek the current 0-based day of the week
     * @return a value greater than or equal to <tt>day</tt>
     */
    private int getNextVariableDay(int day, int lastDay, int dayOfWeek) {
        int nextDay = ADVANCE_TO_NEXT_MONTH;

        for (int i = 0; i < variableDayOfMonthRanges.size(); i++) {
            int result = variableDayOfMonthRanges.get(i).getNextDay(day, lastDay, dayOfWeek);
            if (result == day) {
                return day;
            }

            nextDay = Math.min(nextDay, result);
        }

        return nextDay;
    }

    /**
     * Moves the value of the year field forward in time to satisfy the year
     * constraint.
     *
     * @param year the current year
     * @return <tt>true</tt> if a higher year was found
     */
    private boolean advanceYear(Calendar cal, int year) {
        year = years.nextSetBit(year + 1 - ScheduleExpressionParser.MINIMUM_YEAR); // d665298
        if (year >= 0) {
            cal.set(Calendar.YEAR, year + ScheduleExpressionParser.MINIMUM_YEAR); // d665298
            return true;
        }

        return false;
    }

    private void writeObject(ObjectOutputStream out) // d639610
                    throws IOException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "writeObject");

        out.defaultWriteObject();
        out.writeInt(VERSION);
        out.writeObject(ivSchedule);
        out.writeObject(timeZone.getID());

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "writeObject");
    }

    private void readObject(ObjectInputStream in) // d639610
                    throws IOException, ClassNotFoundException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "readObject");

        in.defaultReadObject();

        int version = in.readInt();
        if (version != VERSION) {
            throw new IOException("invalid version: " + version);
        }

        ivSchedule = (ScheduleExpression) in.readObject();
        String timeZoneID = (String) in.readObject();
        timeZone = TimeZone.getTimeZone(timeZoneID);

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "timezone: " + timeZoneID);

        ScheduleExpressionParser.parse(this);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "readObject");
    }

    /**
     * This class represents a dayOfMonth range with at least one bound
     * containing "Last", "Last Day", or [-7, -1].
     */
    static class VariableDayOfMonthRange {
        /**
         * The inclusive lower bound of the range as encoded by {@link ScheduleExpressionParser#readSingleValue}.
         */
        private final int ivEncodedMin;

        /**
         * The inclusive upper bound of the range as encoded by {@link ScheduleExpressionParser#readSingleValue}.
         */
        private final int ivEncodedMax;

        /**
         * Constructs a new dayOfMonth range.
         *
         * @param encodedMin the lower bound as encoded by {@link ScheduleExpressionParser#readSingleValue}
         * @param encodedMax the upper bound as encoded by {@link ScheduleExpressionParser#readSingleValue}
         */
        VariableDayOfMonthRange(int encodedMin, int encodedMax) {
            ivEncodedMin = encodedMin;
            ivEncodedMax = encodedMax;
        }

        @Override
        public String toString() {
            StringBuilder out = new StringBuilder();
            out.append('[');
            toString(out, ivEncodedMin);
            out.append(", ");
            toString(out, ivEncodedMax);
            out.append(']');

            return out.toString();
        }

        private static void toString(StringBuilder out, int value) {
            if (value < ScheduleExpressionParser.ENCODED_NTH_LAST_DAY_OF_MONTH) {
                out.append(value);
            } else if (value == ScheduleExpressionParser.ENCODED_NTH_LAST_DAY_OF_MONTH) {
                out.append("Last");
            } else if (value < ScheduleExpressionParser.ENCODED_NTH_DAY_OF_WEEK_IN_MONTH) {
                out.append(ScheduleExpressionParser.ENCODED_NTH_LAST_DAY_OF_MONTH - value);
            } else {
                int encoded = value - ScheduleExpressionParser.ENCODED_NTH_DAY_OF_WEEK_IN_MONTH;
                int weekOfMonth = encoded / 7;
                int dayOfWeek = encoded % 7;

                out.append(ScheduleExpressionParser.WEEKS_OF_MONTH[weekOfMonth]).append(' ').append(ScheduleExpressionParser.DAYS_OF_WEEK[dayOfWeek]);
            }
        }

        /**
         * Returns the next day of the month after <tt>day</tt> that is within
         * the bounds of this range.
         *
         * @param day       the current 0-based day of the month
         * @param lastDay   the current 0-based last day of the month
         * @param dayOfWeek the current 0-based day of the week
         * @return a value greater than or equal to <tt>day</tt>
         */
        public int getNextDay(int day, int lastDay, int dayOfWeek) {
            // The EJB 3.1 spec does not state how to handle variable range
            // bounds.  This implementation translates the bounds to actual days
            // of the current month and then applies the section 18.2.1.1.4 rule:
            //
            //   In range "x-y", if x is larger than y, the range is equivalent to
            //   "x-max, min-y", where max is the largest value of the
            //   corresponding attribute and min is the smallest.
            //
            // Out of bounds values (like "5th Sun" in a month without a 5th
            // Sunday) are considered to be infinite.  This means that the range
            // "1-5th Sat" becomes "1-Last", and the range "5th Sat-3" becomes
            // "5th Sat-Inf, 1-3", which reduces to "1-3".

            int minDay = actualDayOfMonth(ivEncodedMin, day, lastDay, dayOfWeek);
            int maxDay = actualDayOfMonth(ivEncodedMax, day, lastDay, dayOfWeek);
            int result;

            if (minDay > maxDay) {
                // d659945.2 - The spec gives no guidance on what to do for
                // inverted variable range bounds.  Unlike normal range bounds, it
                // makes more sense to just advance to the next month, so that's
                // what this implementation does.
                result = ADVANCE_TO_NEXT_MONTH;
            } else {
                // Check if the current day is in the range.
                if (day >= minDay && day <= maxDay) {
                    result = day;
                }
                // If the current day is before the range, advance to the range
                // minimum if it's a valid day in the current month.
                else if (day < minDay && minDay <= lastDay) {
                    result = minDay;
                } else {
                    result = ADVANCE_TO_NEXT_MONTH;
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "variable range " + toString() +
                             " = [" + minDay + ", " + maxDay + "]" +
                             ", last = " + lastDay +
                             ", result = " + result);

            return result;
        }

        /**
         * Converts an encoded upper or lower bound value to an actual day of the
         * current month.
         *
         * @param encodedValue the upper or lower bound value as encoded by {@link ScheduleExpressionParser#readSingleValue}
         * @param day          the current 0-based day of the month
         * @param lastDay      the current 0-based last day of the month
         * @param dayOfWeek    the current 0-based day of the week
         * @return a value greater than or equal to <tt>day</tt>
         */
        private static int actualDayOfMonth(int encodedValue, int day, int lastDay, int dayOfWeek) {
            if (encodedValue < ScheduleExpressionParser.ENCODED_NTH_LAST_DAY_OF_MONTH) {
                // Simple day of month.
                return encodedValue - 1;
            }

            if (encodedValue < ScheduleExpressionParser.ENCODED_NTH_DAY_OF_WEEK_IN_MONTH) {
                // Nth last day of month.
                return lastDay - (encodedValue - ScheduleExpressionParser.ENCODED_NTH_LAST_DAY_OF_MONTH);
            }

            // Nth day of week.
            int encoded = encodedValue - ScheduleExpressionParser.ENCODED_NTH_DAY_OF_WEEK_IN_MONTH;
            int targetWeekOfMonth = encoded / 7;
            int targetDayOfWeek = encoded % 7;

            if (targetWeekOfMonth == ScheduleExpressionParser.LAST_WEEK_OF_MONTH) {
                // The day of the week for the last day of the month.
                int lastDayOfWeek = (dayOfWeek + lastDay - day) % 7;

                if (targetDayOfWeek <= lastDayOfWeek) {
                    // For example, the target day of week is "Last Mon" and the day
                    // of the week of the last day of the month is Wed.  In that
                    // case, Wed "minus" Mon is 2 days, so subtract that from the
                    // last day of the month.
                    return lastDay - (lastDayOfWeek - targetDayOfWeek);
                }

                // For example, the target day of the week is "Last Sat" and the
                // day of the week of the last day of the month is Wed.  In that
                // case, Sat "minus" Wed is 3 days, so add that to the last day
                // of the month (which would be an invalid date in the next month),
                // and then subtract a week to re-enter the current month.
                return lastDay + (targetDayOfWeek - lastDayOfWeek) - 7;
            }

            // The day of the current month for the first instance of target day
            // of the week.
            //
            // To do so, we adjust the current day of the month to find a nearby
            // day of the month that is the target day of the week.  For example,
            // the target day of the week is Sat, and the current day is Thu the
            // 16th.  In that case, Sat "minus" Thu is 2 days, so add that to the
            // current day to get Sat the 18th.  Taking the remainder to remove
            // excess weeks results in Sat the 4th.
            //
            // We always add 7 to handle negative day of week adjustments.  For
            // example, the target day is Mon and the current day is Fri the 1st.
            // In that case, Mon "minus" Fri is -4 days, so we add 7 to get 3
            // (0-based), which results in Mon the 4th.
            int firstDayForTargetDayOfWeek = (day + targetDayOfWeek - dayOfWeek + 7) % 7;

            return targetWeekOfMonth * 7 + firstDayForTargetDayOfWeek;
        }
    }
}
