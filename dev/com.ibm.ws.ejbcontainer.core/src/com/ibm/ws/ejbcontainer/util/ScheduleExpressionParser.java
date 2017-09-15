/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.util;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.TimeZone;

import javax.ejb.ScheduleExpression;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * The <tt>ScheduleExpressionParser</tt> is constructs a {@link ParsedScheduleExpression} object from a <tt>ScheduleExpression</tt>. A
 * unique instance of this class is used for each schedule expression to be
 * parsed. Each attribute is parsed by resetting state at the beginning of {@link parseAttribute}, which handles scanning ({@link skipWhitespace} and {@link scanSingleValueAtom}),
 * lexical analysis ({@link readSingleValue}),
 * and parsing ({@link parseAttribute}).
 *
 * <p>The implementation uses java.util.Calendar for data calculations, and it
 * stores constraint values in bit fields. Consequently, the implementation
 * must keep three range values in mind for each attribute: the spec range, the
 * java.util.Calendar range, and the bit offset range. These ranges are:
 *
 * <pre>
 * Attribute EJB spec java.util.Calendar ParsedScheduleExpression
 * ----------- ------------ ------------------ ------------------------
 * seconds 0-59 0-61 bits 0-59
 * SECOND
 * Leap seconds
 *
 * minutes 0-59 0-59 bits 0-59
 * MINUTE
 *
 * hours 0-23 0-23 bits 0-23
 * HOUR_OF_DAY
 *
 * daysOfMonth 1-31 1-31 bits 0-30
 * DAY_OF_MONTH
 *
 * last -7--1, Last N/A bits 0-7
 * daysOfMonth (0 is -7, 1 is -6, etc,
 * and 7 is Last)
 *
 * daysOfWeek 0-7 1-7 bits 0-6
 * Sun-Sat, Sun Sun-Sat (0 is Sun, etc)
 * DAY_OF_WEEK
 *
 * weeksOfMonth 1st-5th, Last 1-5 multiplier 0-4, 5
 * DAY_OF_WEEK_IN_MONTH (0 is 1st, 1 is 2nd, etc,
 * and 5 is Last)
 *
 * months 1-12 0-11 bits 0-11
 * Jan-Dec Jan-Dec
 * MONTH
 *
 * years 1-9999 1-(big) BitSet
 * YEAR (0 is 0, etc)
 * </pre>
 */
public class ScheduleExpressionParser
{
    private static final TraceComponent tc = Tr.register(ScheduleExpressionParser.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    /**
     * The named values from the specification for the months. These values are
     * searched case-insensitively by findNamedValue.
     */
    static final String[] MONTHS = {
                                    "Jan",
                                    "Feb",
                                    "Mar",
                                    "Apr",
                                    "May",
                                    "Jun",
                                    "Jul",
                                    "Aug",
                                    "Sep",
                                    "Oct",
                                    "Nov",
                                    "Dec",
    };

    /**
     * The named values from the specification for the days of the week. These
     * values are searched case-insensitively by findNamedValue.
     */
    static final String[] DAYS_OF_WEEK = {
                                          "Sun",
                                          "Mon",
                                          "Tue",
                                          "Wed",
                                          "Thu",
                                          "Fri",
                                          "Sat",
    };

    /**
     * The named values from the specification for the weeks of the month.
     * These values are searched case-insensitively by findNamedValue.
     */
    static final String[] WEEKS_OF_MONTH = {
                                            "1st",
                                            "2nd",
                                            "3rd",
                                            "4th",
                                            "5th",
                                            "Last",
    };

    /**
     * The index in {@link ScheduleExpressionParser.WEEKS_OF_MONTH} corresponding to <tt>"Last"</tt>.
     */
    static final int LAST_WEEK_OF_MONTH = 5;

    /**
     * The internal value used to represent a wild card.
     */
    private static final int ENCODED_WILD_CARD = -1;

    /**
     * The internal offset for a dayOfMonth attribute value at which
     * <tt>"Last"</tt>, <tt>-1</tt>, etc are encoded. Days <tt>1</tt> through
     * <tt>31</tt> are represented literally. <tt>"Last"</tt> is encoded as
     * <tt>ENCODED_NTH_LAST_DAY_OF_MONTH</tt>, <tt>-1</tt> as
     * <tt>ENCODED_NTH_LAST_DAY_OF_MONTH + 1</tt>, etc.
     *
     * @see #readSingleValue
     * @see ScheduleExpressionParser.Attribute#DAY_OF_WEEK
     * @see ParsedScheduleExpression.VariableDayOfMonthRange
     */
    static final int ENCODED_NTH_LAST_DAY_OF_MONTH = 32;

    /**
     * The internal offset for a dayOfMonth attribute value at which <tt>"1st
     * <i>day</i>"</tt>, <tt>"Last <i>day</i>"</tt>, etc are encoded. Values
     * are encoded as <tt>ENCODED_NTH_DAY_OF_WEEK_IN_MONTH + (<i>weekOfMonth</i>
     * * 7) + <i>dayOfWeek</i>.
     *
     * @see #readSingleValue
     * @see ScheduleExpressionParser.Attribute#DAY_OF_WEEK
     * @see ParsedScheduleExpression.VariableDayOfMonthRange
     */
    static final int ENCODED_NTH_DAY_OF_WEEK_IN_MONTH = (ENCODED_NTH_LAST_DAY_OF_MONTH + 7) + 1;

    /**
     * The minimum year supported.
     */
    static final int MINIMUM_YEAR = 1000; // d665298

    /**
     * The maximum year supported.
     */
    static final int MAXIMUM_YEAR = 9999; // d665298

    /**
     * The attribute type currently being parsed.
     */
    private Attribute ivAttr;

    /**
     * The attribute value currently being parsed.
     */
    private String ivString;

    /**
     * The position within the attribute value currently being parsed.
     */
    private int ivPos;

    /**
     * Only {@link ScheduleExpressionParser.parse} should create instances.
     */
    private ScheduleExpressionParser() {}

    /**
     * Converts a <tt>ScheduleExpression</tt> to a string that is formatted for
     * diagnostics.
     *
     * @param expr the expression
     * @return a formatted representation
     */
    public static String toString(ScheduleExpression expr) // F743-506
    {
        return Util.identity(expr)
               + "[start=" + toString(expr.getStart())
               + ", end=" + toString(expr.getEnd())
               + ", timezone=" + expr.getTimezone()
               + ", seconds=" + (expr.getSecond() == null ? null : "\"" + expr.getSecond() + "\"")
               + ", minutes=" + (expr.getMinute() == null ? null : "\"" + expr.getMinute() + "\"")
               + ", hours=" + (expr.getHour() == null ? null : "\"" + expr.getHour() + "\"")
               + ", dayOfMonth=" + (expr.getDayOfMonth() == null ? null : "\"" + expr.getDayOfMonth() + "\"")
               + ", month=" + (expr.getMonth() == null ? null : "\"" + expr.getMonth() + "\"")
               + ", dayOfWeek=" + (expr.getDayOfWeek() == null ? null : "\"" + expr.getDayOfWeek() + "\"")
               + ", year=" + (expr.getYear() == null ? null : "\"" + expr.getYear() + "\"")
               + "]";
    }

    /**
     * Converts a <tt>Date</tt> to a string that is formatted for diagnostics.
     *
     * @param date the date
     * @return a formatted representation
     */
    private static String toString(Date date) // d666295
    {
        if (date == null)
        {
            return null;
        }

        long millis = date.getTime();
        if (millis <= 0 || millis % 1000 == 0)
        {
            return date.toString();
        }

        return date + " (" + millis % 1000 + "ms)";
    }

    /**
     * Parses the specified schedule expression and returns the result.
     *
     * @param expr the expression to parse
     * @return the parsed schedule expression
     * @throws ScheduleExpressionParserException if the expression contains an
     *             invalid attribute value
     */
    public static ParsedScheduleExpression parse(ScheduleExpression expr)
    {
        ParsedScheduleExpression parsedExpr = new ParsedScheduleExpression(expr);
        parse(parsedExpr);
        return parsedExpr;
    }

    /**
     * Parses the schedule expression contained within the
     * ParsedScheduleExpression object and store the results in that object.
     *
     * @param parsedExpr the parsed schedule expression to populate
     * @throws ScheduleExpressionParserException if the expression contains an
     *             invalid attribute value
     */
    static void parse(ParsedScheduleExpression parsedExpr) // d639610
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "parse: " + toString(parsedExpr.getSchedule()));

        ScheduleExpression expr = parsedExpr.getSchedule();
        ScheduleExpressionParser parser = new ScheduleExpressionParser();

        try
        {
            parser.parseTimeZone(parsedExpr, expr.getTimezone());

            parser.parseAttribute(parsedExpr, Attribute.SECOND, expr.getSecond());
            parser.parseAttribute(parsedExpr, Attribute.MINUTE, expr.getMinute());
            parser.parseAttribute(parsedExpr, Attribute.HOUR, expr.getHour());
            parser.parseAttribute(parsedExpr, Attribute.DAY_OF_MONTH, expr.getDayOfMonth());
            parser.parseAttribute(parsedExpr, Attribute.MONTH, expr.getMonth());
            parser.parseAttribute(parsedExpr, Attribute.DAY_OF_WEEK, expr.getDayOfWeek());
            parser.parseAttribute(parsedExpr, Attribute.YEAR, expr.getYear());
        } catch (IllegalArgumentException ex)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.exit(tc, "parse", ex);
            throw ex;
        }

        parsedExpr.start = parser.parseDate(expr.getStart(), 0); // d666295
        parsedExpr.end = parser.parseDate(expr.getEnd(), Long.MAX_VALUE); // d666295

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "parse", parsedExpr);
    }

    /**
     * Parses the time zone ID and stores the result in parsedExpr. If the
     * parsed schedule already has a time zone, then it is kept. If the time
     * zone ID is null, then the default time zone is used.
     *
     * @param parsedExpr the object in which to store the result
     * @param string the time zone ID
     * @throws ScheduleExpressionParserException if the expression contains an
     *             invalid attribute value
     */
    private void parseTimeZone(ParsedScheduleExpression parsedExpr, String string)
    {
        if (parsedExpr.timeZone == null) // d639610
        {
            if (string == null)
            {
                parsedExpr.timeZone = TimeZone.getDefault(); // d639610
            }
            else
            {
                parsedExpr.timeZone = TimeZone.getTimeZone(string.trim()); // d664511

                // If an invalid time zone is specified, getTimeZone will always
                // return a TimeZone with an ID of "GMT".  Do not allow this unless it
                // was specifically requested.
                if (parsedExpr.timeZone.getID().equals("GMT") && !string.equalsIgnoreCase("GMT"))
                {
                    throw new ScheduleExpressionParserException(ScheduleExpressionParserException.Error.VALUE, "timezone", string); // F743-506
                }
            }
        }
    }

    /**
     * Parses the date to milliseconds and rounds up to the nearest second.
     *
     * @param date the date to parse
     * @param defaultValue the default value if the date is null
     * @return the rounded milliseconds
     */
    private long parseDate(Date date, long defaultValue) // d666295
    {
        if (date == null)
        {
            return defaultValue;
        }

        long value = date.getTime();

        if (value > 0)
        {
            // Round up to the nearest second.
            long remainder = value % 1000;
            if (remainder != 0)
            {
                // Protect against overflow.
                long newValue = value - remainder + 1000;
                value = newValue > 0 || value < 0 ? newValue : Long.MAX_VALUE;
            }
        }

        return value;
    }

    /**
     * Issues a lexical analysis or parsing error.
     *
     * @param message the error message
     * @throws ScheduleExpressionParserException
     */
    private void error(ScheduleExpressionParserException.Error error) // F743-506
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "parse error in " + ivAttr + " at " + ivPos);

        throw new ScheduleExpressionParserException(error, ivAttr.getDisplayName(), ivString); // F7437591.codRev, F743-506
    }

    /**
     * Parses the specified attribute of this parsers schedule expression and
     * stores the result in parsedExpr. This method sets the ivAttr, ivString,
     * and ivPos member variables that are accessed by other parsing methods.
     * Only ivPos should be modified by those other methods.
     *
     * @param parsedExpr the object in which to store the result
     * @param attr the attribute being parsed
     * @param string the value of the attribute from the schedule expression
     * @throws ScheduleExpressionParserException if the expression contains an
     *             invalid attribute value
     */
    private void parseAttribute(ParsedScheduleExpression parsedExpr,
                                Attribute attr,
                                String string)
    {
        // Reset state.
        ivAttr = attr;
        ivString = string;
        ivPos = 0;

        if (string == null)
        {
            // d660135 - Per CTS, null values must be rejected rather than being
            // translated to the default attribute value.
            error(ScheduleExpressionParserException.Error.VALUE);
        }

        // Loop for lists: "x, x, ..."
        for (boolean inList = false;; inList = true)
        {
            skipWhitespace();
            int value = readSingleValue();
            skipWhitespace();

            // Ranges: "x-x"
            if (ivPos < ivString.length() && ivString.charAt(ivPos) == '-')
            {
                ivPos++;
                skipWhitespace();

                int maxValue = readSingleValue();
                skipWhitespace();

                if (value == ENCODED_WILD_CARD || maxValue == ENCODED_WILD_CARD)
                {
                    error(ScheduleExpressionParserException.Error.RANGE_BOUND); // F743-506
                }

                attr.addRange(parsedExpr, value, maxValue);
            }
            // Increments: "x/x"
            else if (ivPos < ivString.length() && ivString.charAt(ivPos) == '/')
            {
                ivPos++;
                skipWhitespace();

                int interval = readSingleValue();
                skipWhitespace();

                if (interval == ENCODED_WILD_CARD)
                {
                    error(ScheduleExpressionParserException.Error.INCREMENT_INTERVAL); // F743-506
                }

                if (inList)
                {
                    error(ScheduleExpressionParserException.Error.LIST_VALUE); // F743-506
                }

                if (!ivAttr.isIncrementable())
                {
                    error(ScheduleExpressionParserException.Error.UNINCREMENTABLE); // F743-506
                }

                if (value == ENCODED_WILD_CARD)
                {
                    value = 0;
                }

                if (interval == 0)
                {
                    // "30/0" is interpreted as the single value "30".
                    attr.add(parsedExpr, value);
                }
                else
                {
                    for (; value <= ivAttr.getMax(); value += interval)
                    {
                        attr.add(parsedExpr, value);
                    }
                }
                break;
            }
            // Wild cards: "*"
            else if (value == ENCODED_WILD_CARD)
            {
                if (inList)
                {
                    error(ScheduleExpressionParserException.Error.LIST_VALUE); // F743-506
                }

                attr.setWildCard(parsedExpr);
                break;
            }
            // Single values
            else
            {
                attr.add(parsedExpr, value);
            }

            if (ivPos >= ivString.length() || ivString.charAt(ivPos) != ',')
            {
                break;
            }

            // Skip the comma
            ivPos++;
        }

        skipWhitespace();
        if (ivPos < ivString.length())
        {
            error(ScheduleExpressionParserException.Error.VALUE); // F743-506
        }
    }

    /**
     * Skip any whitespace at the current position in the parse string.
     */
    private void skipWhitespace()
    {
        while (ivPos < ivString.length() && Character.isWhitespace(ivString.charAt(ivPos)))
        {
            ivPos++;
        }
    }

    /**
     * Parses a single value at the current position in the parse string.
     * Whitespace before the value is not skipped. A "single value" is defined
     * per the spec and includes:
     *
     * <ul>
     * <li>Non-negative integers within the attribute's range. The return
     * value will be that value literally.
     * <li>Names corresponding to integers. For example, "Mon" for dayOfWeek
     * week or "Jan" for month. The return value will be the corresponding
     * literal value. For example, 0 for "Mon" and 1 for "Jan".
     * <li>For dayOfMonth, the word "Last". The return value will be {@link ENCODED_NTH_LAST_DAY_OF_MONTH}.
     * <li>For dayOfMonth, negative integers in the range [-7, -1]. The
     * return value will be encoded relative to {@link ENCODED_NTH_LAST_DAY_OF_MONTH}.
     * <li>For dayOfMonth, a sequence of two words representing the nth day of
     * week in a month. For example, "1st Sun" or "Last Sat". The return
     * value will be encoded as specified by {@link ENCODED_NTH_DAY_OF_WEEK_IN_MONTH}.
     * <li>The wild card character "*". The return value will be {@link ENCODED_WILD_CARD}.
     * </ul>
     *
     * @throws ScheduleExpressionParserException if the expression contains an
     *             invalid attribute value
     */
    private int readSingleValue()
    {
        if (ivPos < ivString.length() && ivString.charAt(ivPos) == '*')
        {
            ivPos++;
            return ENCODED_WILD_CARD;
        }

        int begin = scanToken();
        String[] namedValues = ivAttr.getNamedValues();
        int result = namedValues == null ? -1 : findNamedValue(begin, namedValues, ivAttr.getMin());

        if (result == -1)
        {
            try
            {
                result = Integer.valueOf(ivString.substring(begin, ivPos));
            } catch (NumberFormatException ex)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "parse failed", ex);

                error(ScheduleExpressionParserException.Error.VALUE); // F7437591.codRev, F743-506
            }

            if (ivAttr == Attribute.DAY_OF_MONTH && result >= -7 && result <= -1)
            {
                result = ENCODED_NTH_LAST_DAY_OF_MONTH + -result;
            }
            else if (result < ivAttr.getMin() || result > ivAttr.getMax())
            {
                error(ScheduleExpressionParserException.Error.VALUE_RANGE); // F743-506
            }
        }
        // The named values for dayOfMonth are "1st", "2nd", etc, and "Last".
        // Next, we need to search for a day of the week.
        else if (ivAttr == Attribute.DAY_OF_MONTH)
        {
            // findNamedValue will have added ivAttr.getMin, which we don't want.
            int weekOfMonth = result - ivAttr.getMin();

            // Since "Last" might not be followed by a day of the week, we need to
            // peek at the next atom.  Primitively implement this by saving and
            // restoring the position in the parse string.
            skipWhitespace();
            int savedPos = ivPos;
            int dayOfWeek = findNamedValue(scanToken(), DAYS_OF_WEEK, 0);

            if (dayOfWeek == -1)
            {
                if (weekOfMonth != LAST_WEEK_OF_MONTH)
                {
                    error(ScheduleExpressionParserException.Error.MISSING_DAY_OF_WEEK); // F743-506
                }

                // We parsed "Last x" hoping that x would be a day of the week, but
                // it wasn't.  Restore the position within the parse string.
                ivPos = savedPos;

                result = ENCODED_NTH_LAST_DAY_OF_MONTH;
            }
            else
            {
                result = ENCODED_NTH_DAY_OF_WEEK_IN_MONTH + (weekOfMonth * 7) + dayOfWeek;
            }
        }

        return result;
    }

    /**
     * Skip the minimum number of characters necessary to form a single unit of
     * information within a value. Whitespace before the value is not skipped.
     *
     * @return the position in the parse string before skipping; if no atom
     *         was found, this value will be the same as <tt>ivPos</tt>
     */
    private int scanToken()
    {
        int begin = ivPos;

        if (ivPos < ivString.length())
        {
            if (ivString.charAt(ivPos) == '-')
            {
                // dayOfWeek allows tokens to begin with "-" (e.g., "-7").  We
                // cannot add "-" to isTokenChar or else "1-2" would be parsed as a
                // single atom.
                ivPos++;
            }

            while (ivPos < ivString.length() && isTokenChar(ivString.charAt(ivPos)))
            {
                ivPos++;
            }
        }

        return begin;
    }

    /**
     * Determines whether a character can belong to a token.
     *
     * @param ch the character
     * @return <tt>true</tt> if ch is a single value atom character
     * @see scanSingleValueAtom
     */
    private boolean isTokenChar(char ch)
    {
        return (ch >= '0' && ch <= '9')
               || (ch >= 'a' && ch <= 'z')
               || (ch >= 'A' && ch <= 'Z');
    }

    /**
     * Case-insensitively search the specified list of named values for a
     * substring of the parse string. The substring of the parse string is the
     * range [begin, ivPos).
     *
     * @param begin the beginning index of the substring, inclusive
     * @param namedValues the values to search
     * @param min the non-negative adjustment for the return value
     * @return <tt>min</tt> plus the position in the values list matching the
     *         substring, or <tt>-1</tt> if the value was not found
     */
    private int findNamedValue(int begin, String[] namedValues, int min)
    {
        int length = ivPos - begin;

        for (int i = 0; i < namedValues.length; i++)
        {
            String namedValue = namedValues[i];

            if (length == namedValue.length() && ivString.regionMatches(true, begin, namedValue, 0, length))
            {
                return min + i;
            }
        }

        return -1;
    }

    /**
     * An enumeration of the attribute values specified by a schedule
     * expression. This class is responsible for populating the contents of an {@link ParsedScheduleExpression} with the values provided by the parser.
     */
    private static enum Attribute
    {
        SECOND("second", true, 0, 59)
        {
            @Override
            void add(ParsedScheduleExpression parsedExpr, int value)
            {
                parsedExpr.seconds = addBit(parsedExpr.seconds, value);
            }

            @Override
            void addRangeImpl(ParsedScheduleExpression parsedExpr, int min, int max)
            {
                parsedExpr.seconds = addBits(parsedExpr.seconds, min, max);
            }

            @Override
            void setWildCard(ParsedScheduleExpression parsedExpr)
            {
                parsedExpr.seconds = ParsedScheduleExpression.WILD_CARD;
            }
        },

        MINUTE("minute", true, 0, 59)
        {
            @Override
            void add(ParsedScheduleExpression parsedExpr, int value)
            {
                parsedExpr.minutes = addBit(parsedExpr.minutes, value);
            }

            @Override
            void addRangeImpl(ParsedScheduleExpression parsedExpr, int min, int max)
            {
                parsedExpr.minutes = addBits(parsedExpr.minutes, min, max);
            }

            @Override
            void setWildCard(ParsedScheduleExpression parsedExpr)
            {
                parsedExpr.minutes = ParsedScheduleExpression.WILD_CARD;
            }
        },

        HOUR("hour", true, 0, 23)
        {
            @Override
            void add(ParsedScheduleExpression parsedExpr, int value)
            {
                parsedExpr.hours = addBit(parsedExpr.hours, value);
            }

            @Override
            void addRangeImpl(ParsedScheduleExpression parsedExpr, int min, int max)
            {
                parsedExpr.hours = addBits(parsedExpr.hours, min, max);
            }

            @Override
            void setWildCard(ParsedScheduleExpression parsedExpr)
            {
                parsedExpr.hours = ParsedScheduleExpression.WILD_CARD;
            }
        },

        // The parser handles DAY_OF_MONTH specially but relies on WEEKS_OF_MONTH
        // being specified for named values to handle "Nth dayOfWeek".
        DAY_OF_MONTH("dayOfMonth", false, 1, 31, WEEKS_OF_MONTH)
        {
            @Override
            void add(ParsedScheduleExpression parsedExpr, int value)
            {
                if (value < ENCODED_NTH_LAST_DAY_OF_MONTH)
                {
                    parsedExpr.daysOfMonth = addBit(parsedExpr.daysOfMonth, value - 1);
                }
                else if (value < ENCODED_NTH_DAY_OF_WEEK_IN_MONTH)
                {
                    parsedExpr.lastDaysOfMonth = addBit(parsedExpr.lastDaysOfMonth, 7 - (value - ENCODED_NTH_LAST_DAY_OF_MONTH));
                }
                else
                {
                    int encoded = value - ENCODED_NTH_DAY_OF_WEEK_IN_MONTH;
                    int weekOfMonth = encoded / 7;
                    int dayOfWeek = encoded % 7;

                    if (weekOfMonth == LAST_WEEK_OF_MONTH)
                    {
                        parsedExpr.lastDaysOfWeekInMonth = addBit(parsedExpr.lastDaysOfWeekInMonth, dayOfWeek);
                    }
                    else
                    {
                        parsedExpr.daysOfWeekInMonth = addBit(parsedExpr.daysOfWeekInMonth, (weekOfMonth * 7) + dayOfWeek);
                    }
                }
            }

            @Override
            void addRange(ParsedScheduleExpression parsedExpr, int min, int max) // d659945
            {
                if (min == max)
                {
                    // Optimization to avoid variable range.
                    add(parsedExpr, min);
                }
                else if (min < ENCODED_NTH_LAST_DAY_OF_MONTH && max < ENCODED_NTH_LAST_DAY_OF_MONTH)
                {
                    // Optimization to avoid variable range.
                    super.addRange(parsedExpr, min, max);
                }
                else
                {
                    if (parsedExpr.variableDayOfMonthRanges == null)
                    {
                        parsedExpr.variableDayOfMonthRanges = new ArrayList<ParsedScheduleExpression.VariableDayOfMonthRange>();
                    }

                    parsedExpr.variableDayOfMonthRanges.add(new ParsedScheduleExpression.VariableDayOfMonthRange(min, max));
                }
            }

            @Override
            void addRangeImpl(ParsedScheduleExpression parsedExpr, int min, int max)
            {
                parsedExpr.daysOfMonth = addBits(parsedExpr.daysOfMonth, min - 1, max - 1);
            }

            @Override
            void setWildCard(ParsedScheduleExpression parsedExpr)
            {
                parsedExpr.daysOfMonth = ParsedScheduleExpression.WILD_CARD;
            }
        },

        MONTH("month", false, 1, 12, MONTHS)
        {
            @Override
            void add(ParsedScheduleExpression parsedExpr, int value)
            {
                parsedExpr.months = addBit(parsedExpr.months, value - 1);
            }

            @Override
            void addRangeImpl(ParsedScheduleExpression parsedExpr, int min, int max)
            {
                parsedExpr.months = addBits(parsedExpr.months, min - 1, max - 1);
            }

            @Override
            void setWildCard(ParsedScheduleExpression parsedExpr)
            {
                parsedExpr.months = ParsedScheduleExpression.WILD_CARD;
            }
        },

        DAY_OF_WEEK("dayOfWeek", false, 0, 7, DAYS_OF_WEEK)
        {
            @Override
            void add(ParsedScheduleExpression parsedExpr, int value)
            {
                parsedExpr.daysOfWeek = addBit(parsedExpr.daysOfWeek, value == 7 ? 0 : value);
            }

            @Override
            void addRange(ParsedScheduleExpression parsedExpr, int min, int max)
            {
                // Translate "7" as a range bound to "0".

                if (min == 7)
                {
                    if (max == 7)
                    {
                        add(parsedExpr, 0);
                    }
                    else
                    {
                        addRangeImpl(parsedExpr, 0, max);
                    }
                }
                else if (max == 7)
                {
                    if (min == 0)
                    {
                        // 18.2.1.2 says that if both dayOfMonth and dayOfWeek are
                        // non-wildcard, then only one needs to match.  However,
                        // 18.2.1.2 says that dayOfWeek("0-7").dayOfMonth("3") is
                        // dayOfWeek("*").dayOfMonth("3"), which means that
                        // dayOfMonth("3") must match.
                        setWildCard(parsedExpr);
                    }
                    else
                    {
                        add(parsedExpr, 0);
                        addRangeImpl(parsedExpr, min, 6);
                    }
                }
                else
                {
                    if (min > max)
                    {
                        addRangeImpl(parsedExpr, min, 6);
                        addRangeImpl(parsedExpr, 0, max);
                    }
                    else
                    {
                        addRangeImpl(parsedExpr, min, max);
                    }
                }
            }

            @Override
            void addRangeImpl(ParsedScheduleExpression parsedExpr, int min, int max)
            {
                parsedExpr.daysOfWeek = addBits(parsedExpr.daysOfWeek, min, max);
            }

            @Override
            void setWildCard(ParsedScheduleExpression parsedExpr)
            {
                parsedExpr.daysOfWeek = ParsedScheduleExpression.WILD_CARD;
            }
        },

        YEAR("year", false, MINIMUM_YEAR, MAXIMUM_YEAR) // d665298
        {
            @Override
            void add(ParsedScheduleExpression parsedExpr, int value)
            {
                updateYears(parsedExpr).set(value - MINIMUM_YEAR); // d665298
            }

            @Override
            void addRangeImpl(ParsedScheduleExpression parsedExpr, int min, int max)
            {
                updateYears(parsedExpr).set(min - MINIMUM_YEAR, max + 1 - MINIMUM_YEAR); // d665298
            }

            private BitSet updateYears(ParsedScheduleExpression parsedExpr)
            {
                if (parsedExpr.years == null)
                {
                    parsedExpr.years = new BitSet();
                }

                return parsedExpr.years; // d665298
            }

            @Override
            void setWildCard(ParsedScheduleExpression parsedExpr)
            {
                parsedExpr.years = null;
            }
        };

        /**
         * A name appropriate for display in error messages.
         */
        private String ivDisplayName;

        /**
         * <tt>true</tt> if this attribute supports increments (i.e., "* / 5")
         */
        private boolean ivIncrementable;

        /**
         * The minimum value that can be specified by a schedule expression.
         */
        private int ivMin;

        /**
         * The maximum value that can be specified by a schedule expression.
         */
        private int ivMax;

        /**
         * The named values used
         */
        private String[] ivNamedValues;

        Attribute(String displayName, boolean incrementable, int min, int max, String[] namedValues)
        {
            ivDisplayName = displayName; // F7437591.codRev
            ivIncrementable = incrementable;
            ivMin = min;
            ivMax = max;
            ivNamedValues = namedValues;
        }

        Attribute(String displayName, boolean incrementable, int min, int max)
        {
            this(displayName, incrementable, min, max, null);
        }

        /**
         * Returns a name appropriate for display in error messages.
         *
         * @return a name appropriate for display in error messages
         */
        String getDisplayName()
        {
            return ivDisplayName;
        }

        /**
         * Returns <tt>true</tt> if this attribute can have an increment value.
         *
         * @return <tt>true</tt> if this attribute can have an increment value
         */
        boolean isIncrementable()
        {
            return ivIncrementable;
        }

        /**
         * Returns the minimum value for single integer values.
         *
         * @return the minimum value for single integer values.
         */
        int getMin()
        {
            return ivMin;
        }

        /**
         * Returns the maximum value for single integer values.
         *
         * @return the maximum value for single integer values.
         */
        int getMax()
        {
            return ivMax;
        }

        /**
         * Returns the named values for this attribute.
         *
         * @return the named values for this attribute.
         */
        String[] getNamedValues()
        {
            return ivNamedValues;
        }

        /**
         * Adds a single value to the specified expression. The value must be
         * within the range [getMin(), getMax()].
         *
         * @param parsedExpr the expression to modify
         * @param value the value as encoded by {@link readSingleValue}
         */
        abstract void add(ParsedScheduleExpression parsedExpr, int value);

        /**
         * Adds a range of values to the specified expression. The values must
         * not be {@link ENCODED_WILD_CARD}. Implementations that accept encoded
         * values must override this method. The call will be call will be
         * forwarded to {@link addRangeImpl}. If <tt>min</tt> is greater than
         * <tt>max</tt>, then two calls will be forwarded:
         *
         * <pre>
         * addRangeImpl(parsedExpr, getMin(), max);
         * addRangeImpl(parsedExpr, min, getMax());
         * </pre>
         *
         * @param parsedExpr the expression to modify
         * @param min the lower bound as encoded by {@link readSingleValue}
         * @param max the upper bound as encoded by {@link readSingleValue}
         */
        void addRange(ParsedScheduleExpression parsedExpr, int min, int max)
        {
            if (min > max)
            {
                addRangeImpl(parsedExpr, getMin(), max);
                addRangeImpl(parsedExpr, min, getMax());
            }
            else
            {
                addRangeImpl(parsedExpr, min, max);
            }
        }

        /**
         * Implementations must add the specified value to the corresponding list
         * of values in the specified expression. The values will be within the
         * range [getMin(), getMax()], and <tt>min</tt> will not be greater than
         * <tt>max</tt>.
         *
         * @param parsedExpr the expression to modify
         * @param min the inclusive lower bound of the range
         * @param max the inclusive upper bound of the range
         */
        abstract void addRangeImpl(ParsedScheduleExpression parsedExpr, int min, int max);

        /**
         * Sets the value of the corresponding attribute to the wild card value.
         *
         * @param parsedExpr the expression to modify
         */
        abstract void setWildCard(ParsedScheduleExpression parsedExpr);

        /**
         * Adds a value to a bitmask. The value must be in the range [0, 32).
         *
         * @param bitmask the bitmask
         * @param value the value to add
         * @return the resulting bitmask
         */
        static final int addBit(int bitmask, int value)
        {
            return (int) addBit((long) bitmask, value);
        }

        /**
         * Adds a range of values to a bitmask. The values must be in the range
         * [0, 32). The minimum value must not be greater than the maximum
         * value.
         *
         * @param bitmask the bitmask
         * @param min the lower bound of the range
         * @param max the upper bound of the range
         * @return the resulting bitmask
         */
        static final int addBits(int bitmask, int min, int max)
        {
            return (int) (addBits((long) bitmask, min, max) & 0x7fffffff);
        }

        /**
         * Adds a value to a bitmask. The value must be in the range [0, 64).
         *
         * @param bitmask the bitmask
         * @param value the value to add
         * @return the resulting bitmask
         */
        static final long addBit(long bitmask, int value)
        {
            return bitmask | (1L << value);
        }

        /**
         * Adds a range of values to a bitmask. The values must be in the range
         * [0, 64). The minimum value must not be greater than the maximum
         * value.
         *
         * @param bitmask the bitmask
         * @param min the lower bound of the range
         * @param max the upper bound of the range
         * @return the resulting bitmask
         */
        static final long addBits(long bitmask, int min, int max)
        {
            long bits = (1L << (max + 1)) - 1;
            if (min != 0)
            {
                bits &= ~((1L << min) - 1);
            }

            return bitmask | bits;
        }
    }
}
