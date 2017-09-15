/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.ejb.ScheduleExpression;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.util.ParsedScheduleExpression;
import com.ibm.ws.ejbcontainer.util.ScheduleExpressionParser;

public class ScheduleExpressionParserTest
{
    private static final TraceComponent tc = Tr.register(ScheduleExpressionParserTest.class, "EJBContainerUT", null);

    private static final String[] DAYS_OF_WEEK = new String[] { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
    private static final String[] MONTHS = new String[] { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    private static void parse(ScheduleExpression expr)
    {
        ScheduleExpressionParser.parse(expr);
    }

    private static void failParse(ScheduleExpression expr)
    {
        try
        {
            parse(expr);
            throw new Error("accepted: " + expr);
        } catch (IllegalArgumentException ex) {
        }
    }

    private static long stringToMillis(String dateTime)
    {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);

        java.util.StringTokenizer st = new java.util.StringTokenizer(dateTime, "- :");
        cal.set(Calendar.YEAR, Integer.parseInt(st.nextToken()));
        cal.set(Calendar.MONTH, Integer.parseInt(st.nextToken()) - 1);
        cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(st.nextToken()));
        if (st.hasMoreTokens())
        {
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(st.nextToken()));
            cal.set(Calendar.MINUTE, Integer.parseInt(st.nextToken()));
            cal.set(Calendar.SECOND, Integer.parseInt(st.nextToken()));

            if (st.hasMoreTokens())
            {
                cal.setTimeZone(TimeZone.getTimeZone(st.nextToken()));
            }
        }
        else
        {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
        }

        return cal.getTimeInMillis();
    }

    private static String toString(Calendar cal)
    {
        return String.format("%04d-%02d-%02d %02d:%02d:%02d",
                             cal.get(Calendar.YEAR),
                             cal.get(Calendar.MONTH) + 1,
                             cal.get(Calendar.DAY_OF_MONTH),
                             cal.get(Calendar.HOUR_OF_DAY),
                             cal.get(Calendar.MINUTE),
                             cal.get(Calendar.SECOND));
    }

    private static String millisToString(long now, TimeZone timezone)
    {
        Calendar cal = Calendar.getInstance(timezone);
        cal.setTimeInMillis(now);

        return toString(cal);
    }

    private static String millisToString(long now)
    {
        return millisToString(now, TimeZone.getDefault());
    }

    private static void verifyFirstTimeout(ScheduleExpression expr, String expected)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "verifyFirstTimeout: " + ScheduleExpressionParser.toString(expr) + " ?= \"" + expected + "\"");

        long firstMillis = ScheduleExpressionParser.parse(expr).getFirstTimeout();
        String first = firstMillis == -1 ? "" : millisToString(firstMillis);
        expected = expected == null ? "" : millisToString(stringToMillis(expected));

        if (!expected.equals(first))
        {
            throw new Error(ScheduleExpressionParser.toString(expr) + " = \"" + first + "\" != \"" + expected + "\"");
        }
    }

    private static void verifyNextTimeout(ScheduleExpression expr, String now, String expected)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "verifyNextTimeout: \"" + now + "\" + " + ScheduleExpressionParser.toString(expr) + " ?= \"" + expected + "\"");

        long nowMillis = stringToMillis(now);
        long nextMillis = ScheduleExpressionParser.parse(expr).getNextTimeout(nowMillis);
        String next = nextMillis == -1 ? "" : millisToString(nextMillis);
        expected = expected == null ? "" : millisToString(stringToMillis(expected));

        if (!expected.equals(next))
        {
            throw new Error("\"" + millisToString(nowMillis) + "\" + " + ScheduleExpressionParser.toString(expr) + " = \"" + next + "\" != \"" + expected + "\"");
        }
    }

    @Test
    public void testParseDefault()
    {
        parse(new ScheduleExpression());
        Assert.assertNull(new ScheduleExpression().getStart());
        Assert.assertNull(new ScheduleExpression().getEnd());
        Assert.assertNull(new ScheduleExpression().getTimezone());
        Assert.assertEquals("0", new ScheduleExpression().getSecond());
        Assert.assertEquals("0", new ScheduleExpression().getMinute());
        Assert.assertEquals("0", new ScheduleExpression().getHour());
        Assert.assertEquals("*", new ScheduleExpression().getDayOfMonth());
        Assert.assertEquals("*", new ScheduleExpression().getMonth());
        Assert.assertEquals("*", new ScheduleExpression().getDayOfWeek());
        Assert.assertEquals("*", new ScheduleExpression().getYear());
    }

    @Test
    public void testParseStart() // F7437591.codRev
    {
        parse(new ScheduleExpression().start(null));
        parse(new ScheduleExpression().start(new Date(0)));
        parse(new ScheduleExpression().start(new Date(Long.MIN_VALUE)));
        parse(new ScheduleExpression().start(new Date(Long.MAX_VALUE)));
    }

    @Test
    public void testParseEnd() // F7437591.codRev
    {
        parse(new ScheduleExpression().end(null));
        parse(new ScheduleExpression().end(new Date(0)));
        parse(new ScheduleExpression().end(new Date(Long.MIN_VALUE)));
        parse(new ScheduleExpression().end(new Date(Long.MAX_VALUE)));
    }

    @Test
    public void testParseTimeZone() // F7437591.codRev
    {
        parse(new ScheduleExpression().timezone(null));
        parse(new ScheduleExpression().timezone("America/New_York"));
        parse(new ScheduleExpression().timezone(" \t\r\nAmerica/New_York \t\r\n"));
        //parse(new ScheduleExpression().timezone("america/new_york"));   d634062
        parse(new ScheduleExpression().timezone("GMT-8"));
        parse(new ScheduleExpression().timezone("GMT+12:34"));
        failParse(new ScheduleExpression().timezone("doesnotexist"));
    }

    @Test
    public void testParseSecond()
    {
        parse(new ScheduleExpression().second("*"));
        parse(new ScheduleExpression().second(0));
        parse(new ScheduleExpression().second("0"));
        parse(new ScheduleExpression().second(59));
        parse(new ScheduleExpression().second("59"));
        parse(new ScheduleExpression().second(" \t\r\n30 \t\r\n"));
        failParse(new ScheduleExpression().second(null));
        failParse(new ScheduleExpression().second(""));
        failParse(new ScheduleExpression().second(-1));
        failParse(new ScheduleExpression().second("-1"));
        failParse(new ScheduleExpression().second(60));
        failParse(new ScheduleExpression().second("60"));
    }

    @Test
    public void testParseMinute()
    {
        parse(new ScheduleExpression().minute("*"));
        parse(new ScheduleExpression().minute(0));
        parse(new ScheduleExpression().minute("0"));
        parse(new ScheduleExpression().minute(59));
        parse(new ScheduleExpression().minute("59"));
        parse(new ScheduleExpression().minute(" \t\r\n30 \t\r\n"));
        failParse(new ScheduleExpression().minute(null));
        failParse(new ScheduleExpression().minute(""));
        failParse(new ScheduleExpression().minute(-1));
        failParse(new ScheduleExpression().minute("-1"));
        failParse(new ScheduleExpression().minute(60));
        failParse(new ScheduleExpression().minute("60"));
    }

    @Test
    public void testParseHour()
    {
        parse(new ScheduleExpression().hour("*"));
        parse(new ScheduleExpression().hour(0));
        parse(new ScheduleExpression().hour("0"));
        parse(new ScheduleExpression().hour(23));
        parse(new ScheduleExpression().hour("23"));
        parse(new ScheduleExpression().hour(" \t\r\n12 \t\r\n"));
        failParse(new ScheduleExpression().hour(null));
        failParse(new ScheduleExpression().hour(""));
        failParse(new ScheduleExpression().hour(-1));
        failParse(new ScheduleExpression().hour("-1"));
        failParse(new ScheduleExpression().hour(24));
        failParse(new ScheduleExpression().hour("24"));
    }

    @Test
    public void testParseDayOfMonth()
    {
        parse(new ScheduleExpression().dayOfMonth("*"));
        parse(new ScheduleExpression().dayOfMonth(1));
        parse(new ScheduleExpression().dayOfMonth("1"));
        parse(new ScheduleExpression().dayOfMonth(31));
        parse(new ScheduleExpression().dayOfMonth("31"));
        parse(new ScheduleExpression().dayOfMonth(" \t\r\n15 \t\r\n"));
        failParse(new ScheduleExpression().dayOfMonth(null));
        failParse(new ScheduleExpression().dayOfMonth(""));
        failParse(new ScheduleExpression().dayOfMonth(0));
        failParse(new ScheduleExpression().dayOfMonth("0"));
        failParse(new ScheduleExpression().dayOfMonth(32));
        failParse(new ScheduleExpression().dayOfMonth("32"));
    }

    @Test
    public void testParseLastDayOfMonth()
    {
        parse(new ScheduleExpression().dayOfMonth("Last"));
        parse(new ScheduleExpression().dayOfMonth("last"));
        parse(new ScheduleExpression().dayOfMonth("LAST"));
        parse(new ScheduleExpression().dayOfMonth(" Last"));
        parse(new ScheduleExpression().dayOfMonth("Last\t"));
        parse(new ScheduleExpression().dayOfMonth(" \t\nLast \t\n"));
        failParse(new ScheduleExpression().dayOfMonth(""));
        failParse(new ScheduleExpression().dayOfMonth("xLast"));
    }

    @Test
    public void testParseNthDayOfMonth()
    {
        for (String ordinal : new String[] { "1st", "2nd", "3rd", "4th", "5th", "Last" })
        {
            for (String dayOfWeek : DAYS_OF_WEEK)
            {
                parse(new ScheduleExpression().dayOfMonth(ordinal + " " + dayOfWeek));
                parse(new ScheduleExpression().dayOfMonth(ordinal.toLowerCase() + " " + dayOfWeek.toLowerCase()));
                parse(new ScheduleExpression().dayOfMonth(ordinal.toUpperCase() + " " + dayOfWeek.toUpperCase()));
                parse(new ScheduleExpression().dayOfMonth(" " + ordinal + " " + dayOfWeek));
                parse(new ScheduleExpression().dayOfMonth(ordinal + " " + dayOfWeek + " "));
                failParse(new ScheduleExpression().dayOfMonth(ordinal + " " + dayOfWeek + " x"));
            }

            failParse(new ScheduleExpression().dayOfMonth(ordinal + " x"));
        }

        failParse(new ScheduleExpression().dayOfMonth("0st Sun"));
        failParse(new ScheduleExpression().dayOfMonth("0nd Sun"));
        failParse(new ScheduleExpression().dayOfMonth("0rd Sun"));
        failParse(new ScheduleExpression().dayOfMonth("0th Sun"));
        failParse(new ScheduleExpression().dayOfMonth("1nd Sun"));
        failParse(new ScheduleExpression().dayOfMonth("1rd Sun"));
        failParse(new ScheduleExpression().dayOfMonth("1th Sun"));
        failParse(new ScheduleExpression().dayOfMonth("2st Sun"));
        failParse(new ScheduleExpression().dayOfMonth("2rd Sun"));
        failParse(new ScheduleExpression().dayOfMonth("2th Sun"));
        failParse(new ScheduleExpression().dayOfMonth("3st Sun"));
        failParse(new ScheduleExpression().dayOfMonth("3nd Sun"));
        failParse(new ScheduleExpression().dayOfMonth("3th Sun"));
        failParse(new ScheduleExpression().dayOfMonth("4st Sun"));
        failParse(new ScheduleExpression().dayOfMonth("4nd Sun"));
        failParse(new ScheduleExpression().dayOfMonth("4rd Sun"));
        failParse(new ScheduleExpression().dayOfMonth("5st Sun"));
        failParse(new ScheduleExpression().dayOfMonth("5nd Sun"));
        failParse(new ScheduleExpression().dayOfMonth("5rd Sun"));
        failParse(new ScheduleExpression().dayOfMonth("6st Sun"));
        failParse(new ScheduleExpression().dayOfMonth("6nd Sun"));
        failParse(new ScheduleExpression().dayOfMonth("6rd Sun"));
        failParse(new ScheduleExpression().dayOfMonth("6th Sun"));
        failParse(new ScheduleExpression().dayOfMonth("Last Last"));

        failParse(new ScheduleExpression().dayOfMonth(""));
        failParse(new ScheduleExpression().dayOfMonth("Last Last"));
    }

    @Test
    public void testParseNthLastDayOfMonth()
    {
        parse(new ScheduleExpression().dayOfMonth(-1));
        parse(new ScheduleExpression().dayOfMonth("-1"));
        parse(new ScheduleExpression().dayOfMonth(-7));
        parse(new ScheduleExpression().dayOfMonth("-7"));
        parse(new ScheduleExpression().dayOfMonth(" \t\r\n-4 \t\r\n"));
        failParse(new ScheduleExpression().dayOfMonth(""));
        failParse(new ScheduleExpression().dayOfMonth(0));
        failParse(new ScheduleExpression().dayOfMonth("0"));
        failParse(new ScheduleExpression().dayOfMonth(-8));
        failParse(new ScheduleExpression().dayOfMonth("-8"));
    }

    @Test
    public void testParseNumericMonth()
    {
        parse(new ScheduleExpression().month("*"));
        parse(new ScheduleExpression().month(1));
        parse(new ScheduleExpression().month("1"));
        parse(new ScheduleExpression().month(12));
        parse(new ScheduleExpression().month("12"));
        parse(new ScheduleExpression().month(" \t\r\n6 \t\r\n"));
        failParse(new ScheduleExpression().month(null));
        failParse(new ScheduleExpression().month(""));
        failParse(new ScheduleExpression().month(0));
        failParse(new ScheduleExpression().month("0"));
        failParse(new ScheduleExpression().month(13));
        failParse(new ScheduleExpression().month("13"));
    }

    @Test
    public void testParseNamedMonth()
    {
        for (String month : MONTHS)
        {
            parse(new ScheduleExpression().month(month));
            parse(new ScheduleExpression().month(month.toLowerCase()));
            parse(new ScheduleExpression().month(month.toUpperCase()));
            parse(new ScheduleExpression().month(month + " "));
            parse(new ScheduleExpression().month(" " + month));
        }
    }

    @Test
    public void testParseNumericDayOfWeek()
    {
        parse(new ScheduleExpression().dayOfWeek("*"));
        parse(new ScheduleExpression().dayOfWeek(0));
        parse(new ScheduleExpression().dayOfWeek("0"));
        parse(new ScheduleExpression().dayOfWeek(7));
        parse(new ScheduleExpression().dayOfWeek("7"));
        parse(new ScheduleExpression().dayOfWeek(" \t\r\n4 \t\r\n"));
        failParse(new ScheduleExpression().dayOfWeek(null));
        failParse(new ScheduleExpression().dayOfWeek(""));
        failParse(new ScheduleExpression().dayOfWeek(-1));
        failParse(new ScheduleExpression().dayOfWeek("-1"));
        failParse(new ScheduleExpression().dayOfWeek(8));
        failParse(new ScheduleExpression().dayOfWeek("8"));
    }

    @Test
    public void testParseNamedDayOfWeek()
    {
        for (String dayOfWeek : DAYS_OF_WEEK)
        {
            parse(new ScheduleExpression().dayOfWeek(dayOfWeek));
            parse(new ScheduleExpression().dayOfWeek(dayOfWeek.toLowerCase()));
            parse(new ScheduleExpression().dayOfWeek(dayOfWeek.toUpperCase()));
            parse(new ScheduleExpression().dayOfWeek(dayOfWeek + " "));
            parse(new ScheduleExpression().dayOfWeek(" " + dayOfWeek));
        }
    }

    @Test
    public void testParseYear()
    {
        parse(new ScheduleExpression().year("*"));

        for (int i = 1000; i <= 9999; i++)
        {
            parse(new ScheduleExpression().year(i));
            parse(new ScheduleExpression().year(Integer.toString(i)));
            parse(new ScheduleExpression().year(Integer.toString(i) + " "));
            parse(new ScheduleExpression().year(" " + Integer.toString(i)));
        }

        failParse(new ScheduleExpression().year(null));
        failParse(new ScheduleExpression().year(""));

        for (int i = 0; i < 1000; i++)
        {
            failParse(new ScheduleExpression().year(i));
            failParse(new ScheduleExpression().year(Integer.toString(i)));
        }

        failParse(new ScheduleExpression().year(10000));
        failParse(new ScheduleExpression().year("10000"));
    }

    @Test
    public void testParseListAttributes()
    {
        parse(new ScheduleExpression().second("1, 1"));
        parse(new ScheduleExpression().minute("1, 1"));
        parse(new ScheduleExpression().hour("1, 1"));
        parse(new ScheduleExpression().dayOfMonth("1, 1"));
        parse(new ScheduleExpression().month("1, 1"));
        parse(new ScheduleExpression().dayOfWeek("1, 1"));
        parse(new ScheduleExpression().year("2000, 2000"));
    }

    @Test
    public void testParseListValues()
    {
        parse(new ScheduleExpression().second("0, 0"));
        failParse(new ScheduleExpression().second("*, 0"));
        failParse(new ScheduleExpression().second("0, *"));
        failParse(new ScheduleExpression().second("1/2, 0"));
        failParse(new ScheduleExpression().second("0, 1/2"));
    }

    @Test
    public void testParseRangeAttributes()
    {
        parse(new ScheduleExpression().second("1-1"));
        parse(new ScheduleExpression().minute("1-1"));
        parse(new ScheduleExpression().hour("1-1"));
        parse(new ScheduleExpression().dayOfMonth("1-1"));
        parse(new ScheduleExpression().month("1-1"));
        parse(new ScheduleExpression().dayOfWeek("1-1"));
        parse(new ScheduleExpression().year("2000-2000"));
    }

    @Test
    public void testParseRangeValues()
    {
        failParse(new ScheduleExpression().second("*-0"));
        failParse(new ScheduleExpression().second("0-*"));
        failParse(new ScheduleExpression().second("1/2-0"));
        failParse(new ScheduleExpression().second("0-1/2"));
        parse(new ScheduleExpression().dayOfWeek("0-7"));
    }

    @Test
    public void testParseSecondRange()
    {
        parse(new ScheduleExpression().second("0-59"));
        parse(new ScheduleExpression().second("59-0"));
        failParse(new ScheduleExpression().second("-1-59"));
        failParse(new ScheduleExpression().second("59--1"));
        failParse(new ScheduleExpression().second("0-61"));
        failParse(new ScheduleExpression().second("61-0"));
    }

    @Test
    public void testParseMinuteRange()
    {
        parse(new ScheduleExpression().minute("0-59"));
        parse(new ScheduleExpression().minute("59-0"));
        failParse(new ScheduleExpression().minute("-1-59"));
        failParse(new ScheduleExpression().minute("59--1"));
        failParse(new ScheduleExpression().minute("0-61"));
        failParse(new ScheduleExpression().minute("61-0"));
    }

    @Test
    public void testParseHourRange()
    {
        parse(new ScheduleExpression().hour("0-23"));
        parse(new ScheduleExpression().hour("23-0"));
        failParse(new ScheduleExpression().hour("-1-23"));
        failParse(new ScheduleExpression().hour("23--1"));
        failParse(new ScheduleExpression().hour("0-24"));
        failParse(new ScheduleExpression().hour("24-0"));
    }

    @Test
    public void testParseDayOfMonthRange()
    {
        parse(new ScheduleExpression().dayOfMonth("1-31"));
        parse(new ScheduleExpression().dayOfMonth("31-1"));
        failParse(new ScheduleExpression().dayOfMonth("0-31"));
        failParse(new ScheduleExpression().dayOfMonth("31-0"));
        failParse(new ScheduleExpression().dayOfMonth("1-32"));
        failParse(new ScheduleExpression().dayOfMonth("32-1"));
        parse(new ScheduleExpression().dayOfMonth("1-Last"));
        parse(new ScheduleExpression().dayOfMonth("Last-1"));
        parse(new ScheduleExpression().dayOfMonth("1st Sun-Last Sat"));
        parse(new ScheduleExpression().dayOfMonth("Last Sat-1st Sun"));
        parse(new ScheduleExpression().dayOfMonth("5th Sun-Last"));
        parse(new ScheduleExpression().dayOfMonth("Last-5th Sun"));
        parse(new ScheduleExpression().dayOfMonth("-7--1"));
        parse(new ScheduleExpression().dayOfMonth("-1--7"));
        failParse(new ScheduleExpression().dayOfMonth("-8--1"));
        failParse(new ScheduleExpression().dayOfMonth("-1--8"));
        failParse(new ScheduleExpression().dayOfMonth("-7-0"));
        failParse(new ScheduleExpression().dayOfMonth("0--7"));
        parse(new ScheduleExpression().dayOfMonth("-1-1"));
        parse(new ScheduleExpression().dayOfMonth("1--1"));
    }

    @Test
    public void testParseMonthRange()
    {
        parse(new ScheduleExpression().month("1-12"));
        parse(new ScheduleExpression().month("12-1"));
        failParse(new ScheduleExpression().month("0-12"));
        failParse(new ScheduleExpression().month("12-0"));
        failParse(new ScheduleExpression().month("1-13"));
        failParse(new ScheduleExpression().month("13-1"));
    }

    @Test
    public void testParseDayOfWeekRange()
    {
        parse(new ScheduleExpression().dayOfWeek("0-7"));
        parse(new ScheduleExpression().dayOfWeek("7-0"));
        failParse(new ScheduleExpression().dayOfWeek("-1-7"));
        failParse(new ScheduleExpression().dayOfWeek("7--1"));
        failParse(new ScheduleExpression().dayOfWeek("0-8"));
        failParse(new ScheduleExpression().dayOfWeek("8-0"));
    }

    @Test
    public void testParseYearRange()
    {
        parse(new ScheduleExpression().year("1000-9999"));
        parse(new ScheduleExpression().year("9999-1000"));
        failParse(new ScheduleExpression().year("999-9999"));
        failParse(new ScheduleExpression().year("9999-999"));
        failParse(new ScheduleExpression().year("1000-10000"));
        failParse(new ScheduleExpression().year("10000-1000"));
    }

    @Test
    public void testParseIncrementAttributes()
    {
        parse(new ScheduleExpression().second("0/1"));
        parse(new ScheduleExpression().minute("0/1"));
        parse(new ScheduleExpression().hour("0/1"));
        failParse(new ScheduleExpression().dayOfMonth("1/1"));
        failParse(new ScheduleExpression().month("1/1"));
        failParse(new ScheduleExpression().dayOfWeek("1/1"));
        failParse(new ScheduleExpression().year("2000/1"));
    }

    @Test
    public void testParseIncrementValues()
    {
        parse(new ScheduleExpression().second("*/0"));
        failParse(new ScheduleExpression().second("0/*"));
    }

    @Test
    public void testParseSecondIncrement()
    {
        parse(new ScheduleExpression().second("0/59"));
        parse(new ScheduleExpression().second("59/1"));
        failParse(new ScheduleExpression().second("-1/59"));
        failParse(new ScheduleExpression().second("59/-1"));
        failParse(new ScheduleExpression().second("0/61"));
        failParse(new ScheduleExpression().second("61/1"));
    }

    @Test
    public void testParseMinuteIncrement()
    {
        parse(new ScheduleExpression().minute("0/59"));
        parse(new ScheduleExpression().minute("59/1"));
        failParse(new ScheduleExpression().minute("-1/59"));
        failParse(new ScheduleExpression().minute("59/-1"));
        failParse(new ScheduleExpression().minute("0/61"));
        failParse(new ScheduleExpression().minute("61/1"));
    }

    @Test
    public void testParseHourIncrement()
    {
        parse(new ScheduleExpression().hour("0/23"));
        parse(new ScheduleExpression().hour("23/1"));
        failParse(new ScheduleExpression().hour("-1/23"));
        failParse(new ScheduleExpression().hour("23/-1"));
        failParse(new ScheduleExpression().hour("0/24"));
        failParse(new ScheduleExpression().hour("24/1"));
    }

    @Test
    public void testParseRules()
    {
        failParse(new ScheduleExpression().dayOfMonth("- 1"));
    }

    @Test
    public void testParseExamples()
    {
        // 18.2.1.1.1
        parse(new ScheduleExpression().second("10"));
        parse(new ScheduleExpression().month("Sep"));

        // 18.2.1.1.2
        parse(new ScheduleExpression().second("*"));
        parse(new ScheduleExpression().dayOfWeek("*"));

        // 18.2.1.1.3
        parse(new ScheduleExpression().second("10,20,30"));
        parse(new ScheduleExpression().dayOfWeek("Mon,Wed,Fri"));
        parse(new ScheduleExpression().minute("0-10,30,40"));

        // 18.2.1.1.4
        parse(new ScheduleExpression().second("1-10"));
        parse(new ScheduleExpression().dayOfWeek("Fri-Mon"));
        parse(new ScheduleExpression().dayOfMonth("27-3"));
        parse(new ScheduleExpression().dayOfMonth("27-Last , 1-3"));

        // 18.2.1.1.5
        parse(new ScheduleExpression().minute("*/5"));
        parse(new ScheduleExpression().minute("0,5,10,15,20,25,30,35,40,45,50,55"));
        parse(new ScheduleExpression().second("30/10"));
        parse(new ScheduleExpression().second("30,40,50"));
        parse(new ScheduleExpression().minute("*/14").hour("1,2"));
        parse(new ScheduleExpression().minute("0,14,28,42,56").hour("1,2"));

        // 18.2.1.3.1
        parse(new ScheduleExpression().dayOfWeek("Mon"));
        parse(new ScheduleExpression().second("0").minute("0").hour("0").dayOfMonth("*").month("*").dayOfWeek("*").dayOfWeek("Mon").year("*"));

        // 18.2.1.3.2
        parse(new ScheduleExpression().minute("15").hour("3").dayOfWeek("Mon-Fri"));

        // 18.2.1.3.3
        parse(new ScheduleExpression().minute("15").hour("3").timezone("America/New_York"));

        // 18.2.1.3.4
        parse(new ScheduleExpression().minute("*").hour("*"));

        // 18.2.1.3.5
        parse(new ScheduleExpression().second("30").hour("12").dayOfWeek("Mon,Wed,Fri"));

        // 18.2.1.3.6
        parse(new ScheduleExpression().minute("*/5").hour("*"));
        parse(new ScheduleExpression().minute("0,5,10,15,20,25,30,35,40,45,50,55").hour("*"));

        // 18.2.1.3.7
        parse(new ScheduleExpression().hour("14").dayOfMonth("Last Thu").month("Nov"));

        // 18.2.1.3.8
        parse(new ScheduleExpression().hour("1").dayOfMonth("-1"));

        // 18.2.1.3.9
        parse(new ScheduleExpression().hour("12/2").dayOfMonth("2nd Tue"));
    }

    // ----- JAN 9996 -----
    // Su Mo Tu We Th Fr Sa
    //     1  2  3  4  5  6
    //  7  8  9 10 11 12 13
    // 14 15 16 17 18 19 20
    // 21 22 23 24 25 26 27
    // 28 29 30 31      
    //
    // ----- FEB 9996 -----
    // Su Mo Tu We Th Fr Sa
    //              1  2  3
    //  4  5  6  7  8  9 10
    // 11 12 13 14 15 16 17
    // 18 19 20 21 22 23 24
    // 25 26 27 28 29
    //
    // ----- Mar 9996 -----
    // Su Mo Tu We Th Fr Sa
    //                 1  2
    //  3  4  5  6  7  8  9
    // 10 11 12 13 14 15 16
    // 17 18 19 20 21 22 23
    // 24 25 26 27 28 29 30
    // 31
    //
    // ----- Apr 9996 -----
    // Su Mo Tu We Th Fr Sa
    //     1  2  3  4  5  6
    //  7  8  9 10 11 12 13
    // 14 15 16 17 18 19 20
    // 21 22 23 24 25 26 27
    // 28 29 30
    //
    // ----- May 9996 -----
    // Su Mo Tu We Th Fr Sa
    //           1  2  3  4
    //  5  6  7  8  9 10 11
    // 12 13 14 15 16 17 18
    // 19 20 21 22 23 24 25
    // 26 27 28 29 30 31
    //
    // ...
    // ----- Nov 9996 -----
    // Su Mo Tu We Th Fr Sa
    //                 1  2
    //  3  4  5  6  7  8  9
    // 10 11 12 13 14 15 16
    // 17 18 19 20 21 22 23
    // 24 25 26 27 28 29 30
    // 31 32 33
    //
    // ...
    //
    // ----- Dec 9996 -----
    // Su Mo Tu We Th Fr Sa
    //  1  2  3  4  5  6  7
    //  8  9 10 11 12 13 14
    // 15 16 17 18 19 20 21
    // 22 23 24 25 26 27 28
    // 29 30 31
    //
    // ----- Jan 9997 -----
    // Su Mo Tu We Th Fr Sa
    //           1  2  3  4
    //  5  6  7  8  9 10 11
    // 12 13 14 15 16 17 18
    // 19 20 21 22 23 24 25
    // 26 27 28 29 30 31
    //
    // ...
    //
    // ----- Nov 9997 -----
    // Su Mo Tu We Th Fr Sa
    //                    1
    //  2  3  4  5  6  7  8
    //  9 10 11 12 13 14 15
    // 16 17 18 19 20 21 22
    // 23 24 25 26 27 28 29
    // 30 31

    @Test
    public void testFirstTimeout() throws Exception
    {
        // Advance time so that we land on the 0th millisecond.  This test will
        // probably only be useful on Windows with its low-resolution timers.
        Thread.sleep(1000 - System.currentTimeMillis() % 1000);

        Calendar cal = Calendar.getInstance();
        ParsedScheduleExpression parsedExpr = ScheduleExpressionParser.parse(
                        new ScheduleExpression().second(cal.get(Calendar.SECOND)).minute("*").hour("*"));
        long firstTimeout = parsedExpr.getFirstTimeout();

        long now = cal.getTimeInMillis();
        if (firstTimeout < now)
        {
            throw new Error("firstTimeout=" + firstTimeout + " < now=" + now);
        }
        else if (now % 1000 != 0)
        {
            Tr.info(tc, "testFirstTimeout might not have been useful: now=" + now + ", firstTimeout=" + firstTimeout);
        }
    }

    @Test
    public void testSecond()
    {
        verifyNextTimeout(new ScheduleExpression().second(1),
                          "9996-01-01 00:00:00",
                          "9996-01-01 00:00:01");
        verifyNextTimeout(new ScheduleExpression().second(1),
                          "9996-01-01 00:00:01",
                          "9996-01-02 00:00:01");
        verifyNextTimeout(new ScheduleExpression().second("*"),
                          "9996-01-01 00:00:00",
                          "9996-01-01 00:00:01");
        verifyNextTimeout(new ScheduleExpression().second("*"),
                          "9996-01-01 00:00:01",
                          "9996-01-01 00:00:02");
    }

    @Test
    public void testSecondRollover()
    {
        verifyNextTimeout(new ScheduleExpression().second("*"),
                          "9996-01-01 00:00:59",
                          "9996-01-02 00:00:00");
        verifyNextTimeout(new ScheduleExpression().second("*").minute(1),
                          "9996-01-01 00:00:59",
                          "9996-01-01 00:01:00");
        verifyNextTimeout(new ScheduleExpression().second("*"),
                          "9996-01-01 00:59:59",
                          "9996-01-02 00:00:00");
        verifyNextTimeout(new ScheduleExpression().second("*").hour(1),
                          "9996-01-01 00:59:59",
                          "9996-01-01 01:00:00");
        verifyNextTimeout(new ScheduleExpression().second("*"),
                          "9996-01-01 23:59:59",
                          "9996-01-02 00:00:00");
        verifyNextTimeout(new ScheduleExpression().second("*"),
                          "9996-01-31 23:59:59",
                          "9996-02-01 00:00:00");
        verifyNextTimeout(new ScheduleExpression().second("*"),
                          "9996-12-31 23:59:59",
                          "9997-01-01 00:00:00");
    }

    @Test
    public void testSecondRange()
    {
        verifyNextTimeout(new ScheduleExpression().second("8-10"),
                          "9996-01-01 00:00:00",
                          "9996-01-01 00:00:08");
        verifyNextTimeout(new ScheduleExpression().second("8-10"),
                          "9996-01-01 00:00:08",
                          "9996-01-01 00:00:09");
        verifyNextTimeout(new ScheduleExpression().second("8-10"),
                          "9996-01-01 00:00:09",
                          "9996-01-01 00:00:10");
        verifyNextTimeout(new ScheduleExpression().second("8-10"),
                          "9996-01-01 00:00:10",
                          "9996-01-02 00:00:08");
        verifyNextTimeout(new ScheduleExpression().second("58-1"),
                          "9996-01-01 00:00:00",
                          "9996-01-01 00:00:01");
        verifyNextTimeout(new ScheduleExpression().second("58-1"),
                          "9996-01-01 00:00:01",
                          "9996-01-01 00:00:58");
        verifyNextTimeout(new ScheduleExpression().second("58-1"),
                          "9996-01-01 00:00:58",
                          "9996-01-01 00:00:59");
        verifyNextTimeout(new ScheduleExpression().second("58-1"),
                          "9996-01-01 00:00:59",
                          "9996-01-02 00:00:00");
    }

    @Test
    public void testSecondIncrement()
    {
        for (String second : new String[] { "0/15", "*/15" })
        {
            verifyNextTimeout(new ScheduleExpression().second(second),
                              "9996-01-01 00:00:00",
                              "9996-01-01 00:00:15");
            verifyNextTimeout(new ScheduleExpression().second(second),
                              "9996-01-01 00:00:15",
                              "9996-01-01 00:00:30");
            verifyNextTimeout(new ScheduleExpression().second(second),
                              "9996-01-01 00:00:30",
                              "9996-01-01 00:00:45");
            verifyNextTimeout(new ScheduleExpression().second(second),
                              "9996-01-01 00:00:45",
                              "9996-01-02 00:00:00");
        }

        verifyNextTimeout(new ScheduleExpression().second("29/31"),
                          "9996-01-01 00:00:00",
                          "9996-01-01 00:00:29");
        verifyNextTimeout(new ScheduleExpression().second("29/31"),
                          "9996-01-01 00:00:29",
                          "9996-01-02 00:00:29");
        verifyNextTimeout(new ScheduleExpression().second("30/0"),
                          "9996-01-01 00:00:00",
                          "9996-01-01 00:00:30");
        verifyNextTimeout(new ScheduleExpression().second("30/0"),
                          "9996-01-01 00:00:30",
                          "9996-01-02 00:00:30");
    }

    @Test
    public void testSecondList()
    {
        verifyNextTimeout(new ScheduleExpression().second("10, 20, 30"),
                          "9996-01-01 00:00:00",
                          "9996-01-01 00:00:10");
        verifyNextTimeout(new ScheduleExpression().second("10, 20, 30"),
                          "9996-01-01 00:00:10",
                          "9996-01-01 00:00:20");
        verifyNextTimeout(new ScheduleExpression().second("10, 20, 30"),
                          "9996-01-01 00:00:20",
                          "9996-01-01 00:00:30");
        verifyNextTimeout(new ScheduleExpression().second("10, 20, 30"),
                          "9996-01-01 00:00:30",
                          "9996-01-02 00:00:10");
    }

    @Test
    public void testMinute()
    {
        verifyNextTimeout(new ScheduleExpression().minute(1),
                          "9996-01-01 00:00:56",
                          "9996-01-01 00:01:00");
        verifyNextTimeout(new ScheduleExpression().minute(1),
                          "9996-01-01 00:01:00",
                          "9996-01-02 00:01:00");
        verifyNextTimeout(new ScheduleExpression().minute("*"),
                          "9996-01-01 00:00:00",
                          "9996-01-01 00:01:00");
        verifyNextTimeout(new ScheduleExpression().minute("*"),
                          "9996-01-01 00:00:01",
                          "9996-01-01 00:01:00");
        verifyNextTimeout(new ScheduleExpression().minute("*"),
                          "9996-01-01 00:01:00",
                          "9996-01-01 00:02:00");
        verifyNextTimeout(new ScheduleExpression().minute("*"),
                          "9996-01-01 00:01:59",
                          "9996-01-01 00:02:00");
        verifyNextTimeout(new ScheduleExpression().second(1).minute("*"),
                          "9996-01-01 00:00:00",
                          "9996-01-01 00:00:01");
        verifyNextTimeout(new ScheduleExpression().second(1).minute("*"),
                          "9996-01-01 00:00:01",
                          "9996-01-01 00:01:01");
    }

    @Test
    public void testMinuteRollover()
    {
        verifyNextTimeout(new ScheduleExpression().minute("*"),
                          "9996-01-01 00:59:00",
                          "9996-01-02 00:00:00");
        verifyNextTimeout(new ScheduleExpression().minute("*").hour(1),
                          "9996-01-01 00:59:00",
                          "9996-01-01 01:00:00");
        verifyNextTimeout(new ScheduleExpression().minute("*").hour(1),
                          "9996-01-01 00:59:01",
                          "9996-01-01 01:00:00");
        verifyNextTimeout(new ScheduleExpression().minute("*"),
                          "9996-01-01 23:59:00",
                          "9996-01-02 00:00:00");
        verifyNextTimeout(new ScheduleExpression().minute("*"),
                          "9996-01-01 23:59:01",
                          "9996-01-02 00:00:00");
        verifyNextTimeout(new ScheduleExpression().minute("*"),
                          "9996-01-31 23:59:00",
                          "9996-02-01 00:00:00");
        verifyNextTimeout(new ScheduleExpression().minute("*"),
                          "9996-01-31 23:59:01",
                          "9996-02-01 00:00:00");
        verifyNextTimeout(new ScheduleExpression().minute("*"),
                          "9996-12-31 23:59:00",
                          "9997-01-01 00:00:00");
        verifyNextTimeout(new ScheduleExpression().minute("*"),
                          "9996-12-31 23:59:01",
                          "9997-01-01 00:00:00");
    }

    @Test
    public void testMinuteRange()
    {
        verifyNextTimeout(new ScheduleExpression().minute("8-10"),
                          "9996-01-01 00:00:56",
                          "9996-01-01 00:08:00");
        verifyNextTimeout(new ScheduleExpression().minute("8-10"),
                          "9996-01-01 00:08:00",
                          "9996-01-01 00:09:00");
        verifyNextTimeout(new ScheduleExpression().minute("8-10"),
                          "9996-01-01 00:09:56",
                          "9996-01-01 00:10:00");
        verifyNextTimeout(new ScheduleExpression().minute("8-10"),
                          "9996-01-01 00:10:56",
                          "9996-01-02 00:08:00");
        verifyNextTimeout(new ScheduleExpression().minute("58-1"),
                          "9996-01-01 00:00:56",
                          "9996-01-01 00:01:00");
        verifyNextTimeout(new ScheduleExpression().minute("58-1"),
                          "9996-01-01 00:01:00",
                          "9996-01-01 00:58:00");
        verifyNextTimeout(new ScheduleExpression().minute("58-1"),
                          "9996-01-01 00:58:56",
                          "9996-01-01 00:59:00");
        verifyNextTimeout(new ScheduleExpression().minute("58-1"),
                          "9996-01-01 00:59:56",
                          "9996-01-02 00:00:00");
    }

    @Test
    public void testMinuteIncrement()
    {
        for (String minute : new String[] { "0/15", "*/15" })
        {
            verifyNextTimeout(new ScheduleExpression().minute(minute),
                              "9996-01-01 00:00:56",
                              "9996-01-01 00:15:00");
            verifyNextTimeout(new ScheduleExpression().minute(minute),
                              "9996-01-01 00:15:00",
                              "9996-01-01 00:30:00");
            verifyNextTimeout(new ScheduleExpression().minute(minute),
                              "9996-01-01 00:30:56",
                              "9996-01-01 00:45:00");
            verifyNextTimeout(new ScheduleExpression().minute(minute),
                              "9996-01-01 00:45:00",
                              "9996-01-02 00:00:00");
        }

        verifyNextTimeout(new ScheduleExpression().minute("29/31"),
                          "9996-01-01 00:00:00",
                          "9996-01-01 00:29:00");
        verifyNextTimeout(new ScheduleExpression().minute("29/31"),
                          "9996-01-01 00:29:56",
                          "9996-01-02 00:29:00");
    }

    @Test
    public void testMinuteList()
    {
        verifyNextTimeout(new ScheduleExpression().minute("10, 20, 30"),
                          "9996-01-01 00:00:00",
                          "9996-01-01 00:10:00");
        verifyNextTimeout(new ScheduleExpression().minute("10, 20, 30"),
                          "9996-01-01 00:10:00",
                          "9996-01-01 00:20:00");
        verifyNextTimeout(new ScheduleExpression().minute("10, 20, 30"),
                          "9996-01-01 00:20:00",
                          "9996-01-01 00:30:00");
        verifyNextTimeout(new ScheduleExpression().minute("10, 20, 30"),
                          "9996-01-01 00:30:00",
                          "9996-01-02 00:10:00");
    }

    @Test
    public void testHour()
    {
        verifyNextTimeout(new ScheduleExpression().hour(1),
                          "9996-01-01 00:34:56",
                          "9996-01-01 01:00:00");
        verifyNextTimeout(new ScheduleExpression().hour(1),
                          "9996-01-01 01:00:00",
                          "9996-01-02 01:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("*"),
                          "9996-01-01 00:00:00",
                          "9996-01-01 01:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("*"),
                          "9996-01-01 00:00:01",
                          "9996-01-01 01:00:00");
        verifyNextTimeout(new ScheduleExpression().second(1).hour("*"),
                          "9996-01-01 00:00:01",
                          "9996-01-01 01:00:01");
        verifyNextTimeout(new ScheduleExpression().hour("*"),
                          "9996-01-01 00:01:00",
                          "9996-01-01 01:00:00");
        verifyNextTimeout(new ScheduleExpression().minute(1).hour("*"),
                          "9996-01-01 00:01:00",
                          "9996-01-01 01:01:00");
        verifyNextTimeout(new ScheduleExpression().hour("*"),
                          "9996-01-01 01:00:00",
                          "9996-01-01 02:00:00");
    }

    @Test
    public void testHourRollover()
    {
        verifyNextTimeout(new ScheduleExpression().hour("*"),
                          "9996-01-01 23:00:00",
                          "9996-01-02 00:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("*"),
                          "9996-01-01 23:00:01",
                          "9996-01-02 00:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("*"),
                          "9996-01-01 23:01:00",
                          "9996-01-02 00:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("*"),
                          "9996-01-31 23:00:00",
                          "9996-02-01 00:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("*"),
                          "9996-01-31 23:00:01",
                          "9996-02-01 00:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("*"),
                          "9996-01-31 23:01:00",
                          "9996-02-01 00:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("*"),
                          "9996-12-31 23:00:00",
                          "9997-01-01 00:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("*"),
                          "9996-12-31 23:00:01",
                          "9997-01-01 00:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("*"),
                          "9996-12-31 23:01:00",
                          "9997-01-01 00:00:00");
    }

    @Test
    public void testHourRange()
    {
        verifyNextTimeout(new ScheduleExpression().hour("8-10"),
                          "9996-01-01 00:34:56",
                          "9996-01-01 08:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("8-10"),
                          "9996-01-01 08:00:00",
                          "9996-01-01 09:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("8-10"),
                          "9996-01-01 09:34:56",
                          "9996-01-01 10:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("8-10"),
                          "9996-01-01 10:34:56",
                          "9996-01-02 08:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("22-1"),
                          "9996-01-01 00:34:56",
                          "9996-01-01 01:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("22-1"),
                          "9996-01-01 01:00:00",
                          "9996-01-01 22:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("22-1"),
                          "9996-01-01 22:34:56",
                          "9996-01-01 23:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("22-1"),
                          "9996-01-01 23:34:56",
                          "9996-01-02 00:00:00");
    }

    @Test
    public void testHourIncrement()
    {
        for (String hour : new String[] { "0/6", "*/6" })
        {
            verifyNextTimeout(new ScheduleExpression().hour(hour),
                              "9996-01-01 00:00:00",
                              "9996-01-01 06:00:00");
            verifyNextTimeout(new ScheduleExpression().hour(hour),
                              "9996-01-01 06:34:56",
                              "9996-01-01 12:00:00");
            verifyNextTimeout(new ScheduleExpression().hour(hour),
                              "9996-01-01 12:00:00",
                              "9996-01-01 18:00:00");
            verifyNextTimeout(new ScheduleExpression().hour(hour),
                              "9996-01-01 18:34:56",
                              "9996-01-02 00:00:00");
        }

        verifyNextTimeout(new ScheduleExpression().hour("11/13"),
                          "9996-01-01 00:00:00",
                          "9996-01-01 11:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("11/13"),
                          "9996-01-01 11:34:56",
                          "9996-01-02 11:00:00");
    }

    @Test
    public void testHourList()
    {
        verifyNextTimeout(new ScheduleExpression().hour("4, 8, 12"),
                          "9996-01-01 00:00:00",
                          "9996-01-01 04:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("4, 8, 12"),
                          "9996-01-01 04:00:00",
                          "9996-01-01 08:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("4, 8, 12"),
                          "9996-01-01 08:00:00",
                          "9996-01-01 12:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("4, 8, 12"),
                          "9996-01-01 12:00:00",
                          "9996-01-02 04:00:00");
    }

    @Test
    public void testDayOfMonth()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(2),
                          "9996-01-01 12:34:56",
                          "9996-01-02 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(2),
                          "9996-01-02 00:00:00",
                          "9996-02-02 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(30),
                          "9996-01-01 12:34:56",
                          "9996-01-30 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(30),
                          "9996-01-31 12:34:56",
                          "9996-03-30 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(29).month(2),
                          "9992-01-01 12:34:56",
                          "9992-02-29 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(29).month(2),
                          "9992-02-29 00:00:00",
                          "9996-02-29 00:00:00");
        verifyNextTimeout(new ScheduleExpression(),
                          "9996-01-01 12:34:56",
                          "9996-01-02 00:00:00");
        verifyNextTimeout(new ScheduleExpression(),
                          "9996-01-02 12:34:56",
                          "9996-01-03 00:00:00");
    }

    @Test
    public void testNthLastDayOfMonth()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-1"),
                          "9996-01-01 12:34:56",
                          "9996-01-30 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-1"),
                          "9996-01-30 12:34:56",
                          "9996-02-28 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-7"),
                          "9996-01-01 12:34:56",
                          "9996-01-24 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-7"),
                          "9996-01-24 12:34:56",
                          "9996-02-22 00:00:00");
    }

    @Test
    public void testLastDayOfMonth()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last"),
                          "9996-01-01 12:34:56",
                          "9996-01-31 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last"),
                          "9996-01-31 12:34:56",
                          "9996-02-29 00:00:00");
    }

    @Test
    public void testNthDayOfMonth()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("1st Sun"),
                          "9996-01-01 12:34:56",
                          "9996-01-07 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("1st Sun"),
                          "9996-01-07 12:34:56",
                          "9996-02-04 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("3rd Wed"),
                          "9996-01-01 12:34:56",
                          "9996-01-17 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("5th Fri"),
                          "9996-01-01 12:34:56",
                          "9996-03-29 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("5th Fri"),
                          "9996-03-29 12:34:56",
                          "9996-05-31 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("5th Thu"),
                          "9996-12-01 12:34:56",
                          "9997-01-30 00:00:00");
    }

    @Test
    public void testLastDayOfWeekOfMonth()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last Sun"),
                          "9996-01-01 12:34:56",
                          "9996-01-28 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last Sun"),
                          "9996-01-28 12:34:56",
                          "9996-02-25 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last Mon"),
                          "9996-01-01 12:34:56",
                          "9996-01-29 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last Mon"),
                          "9996-01-29 12:34:56",
                          "9996-02-26 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last Tue"),
                          "9996-01-01 12:34:56",
                          "9996-01-30 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last Tue"),
                          "9996-01-30 12:34:56",
                          "9996-02-27 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last Wed"),
                          "9996-01-01 12:34:56",
                          "9996-01-31 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last Wed"),
                          "9996-01-31 12:34:56",
                          "9996-02-28 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last Thu"),
                          "9996-01-01 12:34:56",
                          "9996-01-25 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last Thu"),
                          "9996-01-25 12:34:56",
                          "9996-02-29 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last Fri"),
                          "9996-01-01 12:34:56",
                          "9996-01-26 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last Fri"),
                          "9996-01-26 12:34:56",
                          "9996-02-23 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last Sat"),
                          "9996-01-01 12:34:56",
                          "9996-01-27 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last Sat"),
                          "9996-01-27 12:34:56",
                          "9996-02-24 00:00:00");
    }

    @Test
    public void testDayOfMonthRange()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("8-10"),
                          "9996-01-01 12:34:56",
                          "9996-01-08 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("8-10"),
                          "9996-01-08 00:00:00",
                          "9996-01-09 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("8-10"),
                          "9996-01-09 12:34:56",
                          "9996-01-10 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("8-10"),
                          "9996-01-10 12:34:56",
                          "9996-02-08 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("30-2"),
                          "9996-01-01 12:34:56",
                          "9996-01-02 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("30-2"),
                          "9996-01-02 00:00:00",
                          "9996-01-30 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("30-2"),
                          "9996-01-30 00:00:00",
                          "9996-01-31 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("30-2"),
                          "9996-01-31 12:34:56",
                          "9996-02-01 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("30-2"),
                          "9996-02-15 12:34:56",
                          "9996-03-01 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("29-30"),
                          "9996-02-15 12:34:56",
                          "9996-02-29 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("29-30"),
                          "9996-02-29 00:00:00",
                          "9996-03-29 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("30-31"),
                          "9996-02-29 00:00:00",
                          "9996-03-30 00:00:00");
    }

    @Test
    public void testDayToNthLastDayOfMonthRange()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("28--1"),
                          "9996-01-01",
                          "9996-01-28");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("28--1"),
                          "9996-01-28",
                          "9996-01-29");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("28--1"),
                          "9996-01-29",
                          "9996-01-30");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("28--1"),
                          "9996-01-30",
                          "9996-02-28");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("30--1"),
                          "9996-02-01",
                          "9996-03-30");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("30--1"),
                          "9996-03-30",
                          "9996-05-30");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("30--7"),
                          "9996-01-01",
                          null);
    }

    @Test
    public void testDayToLastDayOfMonthRange()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("30-Last"),
                          "9996-01-01",
                          "9996-01-30");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("30-Last"),
                          "9996-01-30",
                          "9996-01-31");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("30-Last"),
                          "9996-01-31",
                          "9996-03-30");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("30-Last"),
                          "9996-03-30",
                          "9996-03-31");
    }

    @Test
    public void testDayOfMonthToNthDayOfWeekRange()
    {
        for (int i = 1; i < 7; i++)
        {
            verifyNextTimeout(new ScheduleExpression().dayOfMonth("7-1st Sun"),
                              "9996-01-" + i,
                              "9996-01-07");
        }
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("4-1st Sun"),
                          "9996-01-07",
                          "9996-02-04");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("1-1st Mon"),
                          "9996-01-01",
                          "9996-02-01");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("2-1st Tue"),
                          "9996-01-01",
                          "9996-01-02");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("3-1st Wed"),
                          "9996-01-01",
                          "9996-01-03");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("4-1st Thu"),
                          "9996-01-01",
                          "9996-01-04");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("5-1st Fri"),
                          "9996-01-01",
                          "9996-01-05");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("6-1st Sat"),
                          "9996-01-01",
                          "9996-01-06");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("17-3rd Wed"),
                          "9996-01-01",
                          "9996-01-17");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("15-3rd Thu"),
                          "9996-02-03",
                          "9996-02-15");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("15-3rd Thu"),
                          "9996-02-04",
                          "9996-02-15");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("31-5th Fri"), // 31-Inv => 31-31
                          "9996-01-01",
                          "9996-01-31");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("30-5th Sat"), // Feb:31(Inv)-30(Inv), Mar:30-30
                          "9996-02-01",
                          "9996-03-30");
    }

    @Test
    public void testDayOfMonthToLastDayOfWeekRange()
    {
        for (int i = 1; i <= 27; i++)
        {
            verifyNextTimeout(new ScheduleExpression().dayOfMonth("28-Last Sun"),
                              "9996-01-" + i,
                              "9996-01-28");
        }

        for (int i = 28; i <= 31; i++)
        {
            verifyNextTimeout(new ScheduleExpression().dayOfMonth("28-Last Sun"),
                              "9996-01-" + i,
                              "9996-03-28");
        }

        for (int i = 28; i <= 30; i++)
        {
            verifyNextTimeout(new ScheduleExpression().dayOfMonth("28-Last Sun"),
                              "9996-03-" + i,
                              "9996-03-" + (i + 1));
        }
    }

    @Test
    public void testNthLastDayToDayOfMonthRange()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-1-31, 5th Sat-5th Sun"),
                          "9996-01-01",
                          "9996-01-30");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-1-31"),
                          "9996-01-30",
                          "9996-01-31");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-1-31"),
                          "9996-01-31",
                          "9996-02-28");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-7-31"),
                          "9996-01-01",
                          "9996-01-24");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-1-30"),
                          "9996-01-01",
                          "9996-01-30");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-1-29"),
                          "9996-02-01",
                          "9996-02-28");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-1-29"),
                          "9996-02-28",
                          "9996-02-29");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-1-29"),
                          "9996-02-29",
                          "9996-04-29");
    }

    @Test
    public void testNthLastDayToNthLastDayOfMonth()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-1--1"),
                          "9996-01-01",
                          "9996-01-30");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-1--1"),
                          "9996-01-30",
                          "9996-02-28");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-7--7"),
                          "9996-01-01",
                          "9996-01-24");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-7--7"),
                          "9996-01-24",
                          "9996-02-22");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-5--3"),
                          "9996-01-01",
                          "9996-01-26");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-5--3"),
                          "9996-01-26",
                          "9996-01-27");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-5--3"),
                          "9996-01-27",
                          "9996-01-28");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-5--3"),
                          "9996-01-28",
                          "9996-02-24");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-3--5"),
                          "9996-01-01",
                          null);
    }

    @Test
    public void testNthLastDayToLastDayOfMonthRange()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-1-Last"),
                          "9996-01-01",
                          "9996-01-30");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-1-Last"),
                          "9996-01-30",
                          "9996-01-31");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-1-Last"),
                          "9996-01-31",
                          "9996-02-28");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-1-Last"),
                          "9996-02-28",
                          "9996-02-29");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-1-Last"),
                          "9996-02-29",
                          "9996-03-30");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-7-Last"),
                          "9996-01-01",
                          "9996-01-24");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-7-Last"),
                          "9996-01-24",
                          "9996-01-25");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-7-Last"),
                          "9996-01-31",
                          "9996-02-22");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-7-Last"),
                          "9996-02-29",
                          "9996-03-24");
    }

    @Test
    public void testNthLastDayToNthDayOfWeekRange()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-7-4th Fri"), // Jan:25-26
                          "9996-01-01",
                          "9996-01-24");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-7-4th Fri"), // Jan:25-26
                          "9996-01-25",
                          "9996-01-26");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("-7-4th Fri"), // Jan:25-26, Feb:23-23
                          "9996-01-26",
                          "9996-02-22");
    }

    @Test
    public void testLastToDayOfMonthRange()
    {
        for (int i = 1; i <= 30; i++)
        {
            verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last-30"),
                              "9996-01-" + i,
                              "9996-02-29");
        }
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last-30"),
                          "9996-02-01",
                          "9996-02-29");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last-30"),
                          "9996-02-28",
                          "9996-02-29");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last-30"),
                          "9996-02-29",
                          "9996-04-30");
    }

    @Test
    public void testLastToNthLastDayOfMonthRange()
    {
        for (int i = 1; i <= 7; i++)
        {
            verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last--" + i),
                              "9996-01-01",
                              null);
            verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last--" + i),
                              "9996-01-31",
                              null);
        }
    }

    @Test
    public void testLastToLastDayOfMonthRange()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last-Last"),
                          "9996-01-01",
                          "9996-01-31");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last-Last"),
                          "9996-01-31",
                          "9996-02-29");
    }

    @Test
    public void testLastDayOfMonthToNthDayOfWeekRange()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last-5th Wed"),
                          "9996-01-05",
                          "9996-01-31");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last-1st Fri"),
                          "9996-01-31",
                          null);
    }

    @Test
    public void testNthDayOfWeekToDayOfMonthRange()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("1st Mon-1"),
                          "9996-01-01",
                          "9996-04-01");
    }

    @Test
    public void testNthDayOfWeekToNthLastDayOfMonthRange()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("5th Mon--1"), // Jan:29-30
                          "9996-01-01",
                          "9996-01-29");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("5th Mon--1"),
                          "9996-01-30",
                          "9996-04-29");
    }

    @Test
    public void testNthDayOfWeekToLastDayOfMonthRange()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("5th Tue-Last"), // Jan:30-31
                          "9996-01-01",
                          "9996-01-30");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("5th Tue-Last"),
                          "9996-01-31",
                          "9996-04-30");
    }

    @Test
    public void testNthDayOfWeekToNthDayOfWeekRange()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("1st Fri-3rd Wed"), // Jan:5-17
                          "9996-01-01",
                          "9996-01-05");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("1st Fri-3rd Wed"), // Jan:5-17, Feb:2-21
                          "9996-01-17",
                          "9996-02-02");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last Fri-Last Sat"), // Feb:23-24
                          "9996-02-01",
                          "9996-02-23");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("Last Sat-Last Thu"), // Feb:24-29
                          "9996-02-01",
                          "9996-02-24");
    }

    @Test
    public void testDayOfMonthList()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("5, 10, 15"),
                          "9996-01-01 00:00:00",
                          "9996-01-05 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("5, 10, 15"),
                          "9996-01-05 00:00:00",
                          "9996-01-10 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("5, 10, 15"),
                          "9996-01-10 00:00:00",
                          "9996-01-15 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("5, 10, 15"),
                          "9996-01-15 00:00:00",
                          "9996-02-05 00:00:00");
    }

    @Test
    public void testMonth()
    {
        verifyNextTimeout(new ScheduleExpression().month(1).dayOfMonth(1),
                          "9996-01-01 12:34:56",
                          "9997-01-01 00:00:00");
        verifyNextTimeout(new ScheduleExpression().month("Jan").dayOfMonth(1),
                          "9996-01-01 12:34:56",
                          "9997-01-01 00:00:00");

        for (int i = 1; i < MONTHS.length - 1; i++)
        {
            for (int j = 0; j < 2; j++)
            {
                String month = j == 0 ? Integer.toString(i + 1) : MONTHS[i];
                verifyNextTimeout(new ScheduleExpression().month(month).dayOfMonth(1),
                                  "              9996-  01-01 12:34:56",
                                  String.format("9996-%02d-01 00:00:00", i + 1));
                verifyNextTimeout(new ScheduleExpression().month(month).dayOfMonth(1),
                                  String.format("9996-%02d-01 12:34:56", i + 1),
                                  String.format("9997-%02d-01 00:00:00", i + 1));
            }
        }

        verifyNextTimeout(new ScheduleExpression().month(12).dayOfMonth(1),
                          "9996-01-01 12:34:56",
                          "9996-12-01 00:00:00");
        verifyNextTimeout(new ScheduleExpression().month(12).dayOfMonth(1),
                          "9996-12-01 12:34:56",
                          "9997-12-01 00:00:00");
        verifyNextTimeout(new ScheduleExpression().month("Dec").dayOfMonth(1),
                          "9996-01-01 12:34:56",
                          "9996-12-01 00:00:00");
        verifyNextTimeout(new ScheduleExpression().month("Dec").dayOfMonth(1),
                          "9996-12-01 12:34:56",
                          "9997-12-01 00:00:00");
    }

    @Test
    public void testMonthRange()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(1).month("8-10"),
                          "9996-01-01",
                          "9996-08-01");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(1).month("8-10"),
                          "9996-08-01",
                          "9996-09-01");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(1).month("8-10"),
                          "9996-09-01",
                          "9996-10-01");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(1).month("8-10"),
                          "9996-10-01",
                          "9997-08-01");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(1).month("11-2"),
                          "9996-01-01",
                          "9996-02-01");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(1).month("11-2"),
                          "9996-02-01",
                          "9996-11-01");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(1).month("11-2"),
                          "9996-11-01",
                          "9996-12-01");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(1).month("11-2"),
                          "9996-12-01",
                          "9997-01-01");
    }

    @Test
    public void testMonthList()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(1).month("Feb, Apr, Jun"),
                          "9996-01-01 00:00:00",
                          "9996-02-01 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(1).month("Feb, Apr, Jun"),
                          "9996-02-01 00:00:00",
                          "9996-04-01 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(1).month("Feb, Apr, Jun"),
                          "9996-04-01 00:00:00",
                          "9996-06-01 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(1).month("Feb, Apr, Jun"),
                          "9996-06-01 00:00:00",
                          "9997-02-01 00:00:00");
    }

    @Test
    public void testDayOfWeek()
    {
        for (String dayOfWeek : new String[] { "0", "7", "Sun" })
        {
            verifyNextTimeout(new ScheduleExpression().dayOfWeek(dayOfWeek),
                              "9996-01-01 12:34:56",
                              "9996-01-07 00:00:00");
            verifyNextTimeout(new ScheduleExpression().dayOfWeek(dayOfWeek),
                              "9996-01-07 12:34:56",
                              "9996-01-14 00:00:00");
            verifyNextTimeout(new ScheduleExpression().hour("*").dayOfWeek(dayOfWeek),
                              "9996-01-31 00:00:00",
                              "9996-02-04 00:00:00");
        }

        for (String dayOfWeek : new String[] { "1", "Mon" })
        {
            verifyNextTimeout(new ScheduleExpression().dayOfWeek(dayOfWeek),
                              "9996-01-02 12:34:56",
                              "9996-01-08 00:00:00");
            verifyNextTimeout(new ScheduleExpression().dayOfWeek(dayOfWeek),
                              "9996-01-08 12:34:56",
                              "9996-01-15 00:00:00");
            verifyNextTimeout(new ScheduleExpression().dayOfWeek(dayOfWeek),
                              "9996-01-31 12:34:56",
                              "9996-02-05 00:00:00");
        }

        for (String dayOfWeek : new String[] { "2", "Tue" })
        {
            verifyNextTimeout(new ScheduleExpression().dayOfWeek(dayOfWeek),
                              "9996-01-01 12:34:56",
                              "9996-01-02 00:00:00");
            verifyNextTimeout(new ScheduleExpression().dayOfWeek(dayOfWeek),
                              "9996-01-02 12:34:56",
                              "9996-01-09 00:00:00");
            verifyNextTimeout(new ScheduleExpression().dayOfWeek(dayOfWeek),
                              "9996-01-31 12:34:56",
                              "9996-02-06 00:00:00");
        }

        for (String dayOfWeek : new String[] { "3", "Wed" })
        {
            verifyNextTimeout(new ScheduleExpression().dayOfWeek(dayOfWeek),
                              "9996-01-01 12:34:56",
                              "9996-01-03 00:00:00");
            verifyNextTimeout(new ScheduleExpression().dayOfWeek(dayOfWeek),
                              "9996-01-03 12:34:56",
                              "9996-01-10 00:00:00");
            verifyNextTimeout(new ScheduleExpression().dayOfWeek(dayOfWeek),
                              "9996-01-31 12:34:56",
                              "9996-02-07 00:00:00");
        }

        for (String dayOfWeek : new String[] { "4", "Thu" })
        {
            verifyNextTimeout(new ScheduleExpression().dayOfWeek(dayOfWeek),
                              "9996-01-01 12:34:56",
                              "9996-01-04 00:00:00");
            verifyNextTimeout(new ScheduleExpression().dayOfWeek(dayOfWeek),
                              "9996-01-04 12:34:56",
                              "9996-01-11 00:00:00");
            verifyNextTimeout(new ScheduleExpression().dayOfWeek(dayOfWeek),
                              "9996-01-31 12:34:56",
                              "9996-02-01 00:00:00");
        }

        for (String dayOfWeek : new String[] { "5", "Fri" })
        {
            verifyNextTimeout(new ScheduleExpression().dayOfWeek(dayOfWeek),
                              "9996-01-01 12:34:56",
                              "9996-01-05 00:00:00");
            verifyNextTimeout(new ScheduleExpression().dayOfWeek(dayOfWeek),
                              "9996-01-05 12:34:56",
                              "9996-01-12 00:00:00");
            verifyNextTimeout(new ScheduleExpression().dayOfWeek(dayOfWeek),
                              "9996-01-31 12:34:56",
                              "9996-02-02 00:00:00");
        }

        for (String dayOfWeek : new String[] { "6", "Sat" })
        {
            verifyNextTimeout(new ScheduleExpression().dayOfWeek(dayOfWeek),
                              "9996-01-01 12:34:56",
                              "9996-01-06 00:00:00");
            verifyNextTimeout(new ScheduleExpression().dayOfWeek(dayOfWeek),
                              "9996-01-06 12:34:56",
                              "9996-01-13 00:00:00");
            verifyNextTimeout(new ScheduleExpression().dayOfWeek(dayOfWeek),
                              "9996-01-31 12:34:56",
                              "9996-02-03 00:00:00");
        }
    }

    @Test
    public void testDayOfWeekRange()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("Tue-Thu"),
                          "9996-01-07",
                          "9996-01-09");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("Tue-Thu"),
                          "9996-01-09",
                          "9996-01-10");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("Tue-Thu"),
                          "9996-01-10",
                          "9996-01-11");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("Tue-Thu"),
                          "9996-01-11",
                          "9996-01-16");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("Tue-Thu"),
                          "9996-01-31",
                          "9996-02-01");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("Fri-Mon"),
                          "9996-01-07",
                          "9996-01-08");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("Fri-Mon"),
                          "9996-01-08",
                          "9996-01-12");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("Fri-Mon"),
                          "9996-01-12",
                          "9996-01-13");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("Fri-Mon"),
                          "9996-01-13",
                          "9996-01-14");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("Wed-Sun"),
                          "9996-01-31",
                          "9996-02-01");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("5-1"),
                          "9996-01-01",
                          "9996-01-05");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("5-1"),
                          "9996-01-05",
                          "9996-01-06");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("5-1"),
                          "9996-01-06",
                          "9996-01-07");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("5-1"),
                          "9996-01-07",
                          "9996-01-08");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("5-1"),
                          "9996-01-08",
                          "9996-01-12");
    }

    @Test
    public void test7thDayOfWeekRange()
    {
        for (int i = 1; i < 7; i++)
        {
            verifyNextTimeout(new ScheduleExpression().dayOfWeek("0-7"),
                              "9996-01-" + i,
                              "9996-01-" + (i + 1));
        }

        verifyNextTimeout(new ScheduleExpression().dayOfWeek("0-7"),
                          "9996-01-07",
                          "9996-01-08");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("6-7"),
                          "9996-01-01",
                          "9996-01-06");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("6-7"),
                          "9996-01-06",
                          "9996-01-07");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("6-7"),
                          "9996-01-07",
                          "9996-01-13");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("6-7"),
                          "9996-01-13",
                          "9996-01-14");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("7-7"),
                          "9996-01-07",
                          "9996-01-14");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("7-0"),
                          "9996-01-01",
                          "9996-01-07");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("7-0"),
                          "9996-01-07",
                          "9996-01-14");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("7-1"),
                          "9996-01-01",
                          "9996-01-07");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("7-1"),
                          "9996-01-07",
                          "9996-01-08");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("7-1"),
                          "9996-01-08",
                          "9996-01-14");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("7-7"),
                          "9996-01-01",
                          "9996-01-07");
    }

    @Test
    public void testDayOfWeekList()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("Tue, Thu, Sat"),
                          "9996-01-01 00:00:00",
                          "9996-01-02 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("Tue, Thu, Sat"),
                          "9996-01-02 00:00:00",
                          "9996-01-04 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("Tue, Thu, Sat"),
                          "9996-01-04 00:00:00",
                          "9996-01-06 00:00:00");
        verifyNextTimeout(new ScheduleExpression().dayOfWeek("Tue, Thu, Sat"),
                          "9996-01-06 00:00:00",
                          "9996-01-09 00:00:00");
    }

    @Test
    public void testDayOfMonthAndWeek()
    {
        // 18.2.1.2
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("4, 5").dayOfWeek("Tue, Fri"),
                          "9996-01-01",
                          "9996-01-02");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("4, 5").dayOfWeek("Tue, Fri"),
                          "9996-01-02",
                          "9996-01-04");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("4, 5").dayOfWeek("Tue, Fri"),
                          "9996-01-04",
                          "9996-01-05");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("4, 5").dayOfWeek("Tue, Fri"),
                          "9996-01-05",
                          "9996-01-09");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("4, 5").dayOfWeek("Tue, Fri"),
                          "9996-01-31",
                          "9996-02-02");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("3").dayOfWeek("0-6"),
                          "9996-01-01",
                          "9996-01-02");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("3").dayOfWeek("0-6"),
                          "9996-01-02",
                          "9996-01-03");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("3").dayOfWeek("0-6"),
                          "9996-01-03",
                          "9996-01-04");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("3").dayOfWeek("0-7"), // 18.2.1.1.4 says "0-7" is wildcard
                          "9996-01-01",
                          "9996-01-03");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth("3").dayOfWeek("0-7"),
                          "9996-01-03",
                          "9996-02-03");
    }

    @Test
    public void testYear()
    {
        verifyNextTimeout(new ScheduleExpression().year(9996),
                          "9996-01-01 12:34:56",
                          "9996-01-02 00:00:00");
        verifyNextTimeout(new ScheduleExpression().year(9997),
                          "9996-01-01 12:34:56",
                          "9997-01-01 00:00:00");
    }

    @Test
    public void testYearRange()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(1).month(1).year("9990-9999"),
                          "9996-01-01",
                          "9997-01-01");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(1).month(1).year("9999-9990"),
                          "9996-01-01",
                          "9999-01-01");
    }

    @Test
    public void testYearList()
    {
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(1).month(1).year("9997, 9999"),
                          "9996-01-01",
                          "9997-01-01");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(1).month(1).year("9997, 9999"),
                          "9997-01-01",
                          "9999-01-01");
        verifyNextTimeout(new ScheduleExpression().dayOfMonth(1).month(1).year("9997, 9999"),
                          "9999-01-01",
                          null);
    }

    @Test
    public void testStart()
    {
        verifyFirstTimeout(new ScheduleExpression().start(new Date(0)).year(9996),
                           "9996-01-01");
        verifyFirstTimeout(new ScheduleExpression().start(new Date(Long.MIN_VALUE)).year(9996),
                           "9996-01-01");
        verifyFirstTimeout(new ScheduleExpression().start(new Date(Long.MAX_VALUE)),
                           null);

        verifyFirstTimeout(new ScheduleExpression().start(new Date(stringToMillis("9996-01-01"))),
                           "9996-01-01");
        verifyFirstTimeout(new ScheduleExpression().start(new Date(stringToMillis("9996-01-01 00:00:01"))),
                           "9996-01-02");

        verifyFirstTimeout(new ScheduleExpression().start(new Date(1 + stringToMillis("9996-01-01"))),
                           "9996-01-02");
    }

    @Test
    public void testEnd()
    {
        verifyFirstTimeout(new ScheduleExpression().end(new Date(0)),
                           null);
        verifyFirstTimeout(new ScheduleExpression().end(new Date(Long.MIN_VALUE)),
                           null);
        verifyFirstTimeout(new ScheduleExpression().end(new Date(Long.MAX_VALUE)).year(9996),
                           "9996-01-01");
        verifyFirstTimeout(new ScheduleExpression().end(new Date(stringToMillis("2000-01-01"))),
                           null);

        verifyNextTimeout(new ScheduleExpression().end(new Date(stringToMillis("9996-01-02"))),
                          "9996-01-01",
                          "9996-01-02");
        verifyNextTimeout(new ScheduleExpression().end(new Date(stringToMillis("9996-01-02"))),
                          "9996-01-02",
                          null);

        verifyNextTimeout(new ScheduleExpression().end(new Date(1 + stringToMillis("9996-01-01 23:59:59"))),
                          "9996-01-01",
                          "9996-01-02");
    }

    @Test
    public void testStartAndEnd()
    {
        verifyNextTimeout(new ScheduleExpression().start(new Date(stringToMillis("2000-01-01"))).end(new Date(stringToMillis("9999-12-31"))),
                          "9996-01-01",
                          "9996-01-02");
        verifyFirstTimeout(new ScheduleExpression().start(new Date(stringToMillis("9999-12-31"))).end(new Date(stringToMillis("2000-01-01"))),
                           null);

        {
            Date date = new Date(1 + stringToMillis("9996-01-01"));
            verifyFirstTimeout(new ScheduleExpression().start(date).end(date).second("*"),
                               "9996-01-01 00:00:01");
        }
    }

    @Test
    public void testDaylightSavingsTime()
    {
        // This test relies on US-specific DST transitions in 2009.
        verifyNextTimeout(new ScheduleExpression().hour("0-4").timezone("America/Chicago"),
                          "2009-03-08 01:00:00 America/Chicago",
                          "2009-03-08 03:00:00 America/Chicago");

        final TimeZone timezone = TimeZone.getTimeZone("America/Chicago");
        final ParsedScheduleExpression parsedAllHours = ScheduleExpressionParser.parse(
                        new ScheduleExpression().hour("*").timezone("America/Chicago"));

        final long t2009_11_01__00_00_00 = 1257051600000L;
        if (!millisToString(t2009_11_01__00_00_00, timezone).equals("2009-11-01 00:00:00"))
        {
            throw new Error(millisToString(t2009_11_01__00_00_00, timezone));
        }

        final long t2009_11_01__01_00_00 = parsedAllHours.getNextTimeout(t2009_11_01__00_00_00);
        if (t2009_11_01__01_00_00 != t2009_11_01__00_00_00 + (1 * 60 * 60 * 1000))
        {
            throw new Error(t2009_11_01__01_00_00 + " " + millisToString(t2009_11_01__01_00_00));
        }
        if (!millisToString(t2009_11_01__01_00_00, timezone).equals("2009-11-01 01:00:00"))
        {
            throw new Error(millisToString(t2009_11_01__01_00_00, timezone));
        }

        final long t2009_11_01__02_00_00 = parsedAllHours.getNextTimeout(t2009_11_01__01_00_00);
        if (t2009_11_01__02_00_00 != t2009_11_01__00_00_00 + (3 * 60 * 60 * 1000))
        {
            throw new Error(t2009_11_01__00_00_00 + " " + millisToString(t2009_11_01__02_00_00));
        }
        if (!millisToString(t2009_11_01__02_00_00, timezone).equals("2009-11-01 02:00:00"))
        {
            throw new Error(millisToString(t2009_11_01__02_00_00, timezone));
        }
    }

    @Test
    public void testExamples()
    {
        // 18.2.1.3.1
        for (ScheduleExpression expr : new ScheduleExpression[] {
                                                                 new ScheduleExpression().dayOfWeek("Mon"),
                                                                 new ScheduleExpression().second("0").minute("0").hour("0").dayOfMonth("*").month("*").dayOfWeek("*").dayOfWeek("Mon").year("*"),
        })
        {
            verifyNextTimeout(expr,
                              "9996-01-01 00:00:00",
                              "9996-01-08 00:00:00");
            verifyNextTimeout(expr,
                              "9996-01-08 12:34:56",
                              "9996-01-15 00:00:00");
            verifyNextTimeout(expr,
                              "9996-01-15 00:00:00",
                              "9996-01-22 00:00:00");
            verifyNextTimeout(expr,
                              "9996-01-22 12:34:56",
                              "9996-01-29 00:00:00");
            verifyNextTimeout(expr,
                              "9996-01-29 00:00:00",
                              "9996-02-05 00:00:00");
        }

        // 18.2.1.3.2
        verifyNextTimeout(new ScheduleExpression().minute("15").hour("3").dayOfWeek("Mon-Fri"),
                          "9996-01-01 00:00:00",
                          "9996-01-01 03:15:00");
        verifyNextTimeout(new ScheduleExpression().minute("15").hour("3").dayOfWeek("Mon-Fri"),
                          "9996-01-01 03:15:00",
                          "9996-01-02 03:15:00");
        verifyNextTimeout(new ScheduleExpression().minute("15").hour("3").dayOfWeek("Mon-Fri"),
                          "9996-01-02 12:34:56",
                          "9996-01-03 03:15:00");
        verifyNextTimeout(new ScheduleExpression().minute("15").hour("3").dayOfWeek("Mon-Fri"),
                          "9996-01-03 03:15:00",
                          "9996-01-04 03:15:00");
        verifyNextTimeout(new ScheduleExpression().minute("15").hour("3").dayOfWeek("Mon-Fri"),
                          "9996-01-04 12:34:56",
                          "9996-01-05 03:15:00");
        verifyNextTimeout(new ScheduleExpression().minute("15").hour("3").dayOfWeek("Mon-Fri"),
                          "9996-01-05 03:15:00",
                          "9996-01-08 03:15:00");

        // 18.2.1.3.3
        verifyNextTimeout(new ScheduleExpression().minute("15").hour("3").timezone("America/New_York"),
                          "9996-01-01 00:00:00 America/Chicago",
                          "9996-01-01 02:15:00 America/Chicago");
        verifyNextTimeout(new ScheduleExpression().minute("15").hour("3").timezone("America/New_York"),
                          "9996-01-01 02:15:00 America/Chicago",
                          "9996-01-02 02:15:00 America/Chicago");

        // 18.2.1.3.4
        verifyNextTimeout(new ScheduleExpression().minute("*").hour("*"),
                          "9996-01-01 00:00:00",
                          "9996-01-01 00:01:00");
        verifyNextTimeout(new ScheduleExpression().minute("*").hour("*"),
                          "9996-01-01 00:01:56",
                          "9996-01-01 00:02:00");
        verifyNextTimeout(new ScheduleExpression().minute("*").hour("*"),
                          "9996-01-01 00:02:00",
                          "9996-01-01 00:03:00");
        verifyNextTimeout(new ScheduleExpression().minute("*").hour("*"),
                          "9996-01-01 00:59:56",
                          "9996-01-01 01:00:00");
        verifyNextTimeout(new ScheduleExpression().minute("*").hour("*"),
                          "9996-01-01 01:00:00",
                          "9996-01-01 01:01:00");
        verifyNextTimeout(new ScheduleExpression().minute("*").hour("*"),
                          "9996-01-01 01:01:56",
                          "9996-01-01 01:02:00");
        verifyNextTimeout(new ScheduleExpression().minute("*").hour("*"),
                          "9996-01-01 23:59:00",
                          "9996-01-02 00:00:00");

        // 18.2.1.3.5
        verifyNextTimeout(new ScheduleExpression().second("30").hour("12").dayOfWeek("Mon,Wed,Fri"),
                          "9996-01-01 00:00:00",
                          "9996-01-01 12:00:30");
        verifyNextTimeout(new ScheduleExpression().second("30").hour("12").dayOfWeek("Mon,Wed,Fri"),
                          "9996-01-01 12:00:30",
                          "9996-01-03 12:00:30");
        verifyNextTimeout(new ScheduleExpression().second("30").hour("12").dayOfWeek("Mon,Wed,Fri"),
                          "9996-01-03 12:34:56",
                          "9996-01-05 12:00:30");
        verifyNextTimeout(new ScheduleExpression().second("30").hour("12").dayOfWeek("Mon,Wed,Fri"),
                          "9996-01-05 12:00:30",
                          "9996-01-08 12:00:30");
        verifyNextTimeout(new ScheduleExpression().second("30").hour("12").dayOfWeek("Mon,Wed,Fri"),
                          "9996-01-31 12:00:30",
                          "9996-02-02 12:00:30");

        // 18.2.1.3.6
        for (ScheduleExpression expr : new ScheduleExpression[] {
                                                                 new ScheduleExpression().minute("*/5").hour("*"),
                                                                 new ScheduleExpression().minute("0,5,10,15,20,25,30,35,40,45,50,55").hour("*"),
        })
        {
            verifyNextTimeout(expr,
                              "9996-01-01 00:00:00",
                              "9996-01-01 00:05:00");
            verifyNextTimeout(expr,
                              "9996-01-01 00:05:00",
                              "9996-01-01 00:10:00");
            verifyNextTimeout(expr,
                              "9996-01-01 00:10:00",
                              "9996-01-01 00:15:00");
            verifyNextTimeout(expr,
                              "9996-01-01 00:15:00",
                              "9996-01-01 00:20:00");
            verifyNextTimeout(expr,
                              "9996-01-01 00:20:00",
                              "9996-01-01 00:25:00");
            verifyNextTimeout(expr,
                              "9996-01-01 00:25:00",
                              "9996-01-01 00:30:00");
            verifyNextTimeout(expr,
                              "9996-01-01 00:30:00",
                              "9996-01-01 00:35:00");
            verifyNextTimeout(expr,
                              "9996-01-01 00:35:00",
                              "9996-01-01 00:40:00");
            verifyNextTimeout(expr,
                              "9996-01-01 00:40:00",
                              "9996-01-01 00:45:00");
            verifyNextTimeout(expr,
                              "9996-01-01 00:45:00",
                              "9996-01-01 00:50:00");
            verifyNextTimeout(expr,
                              "9996-01-01 00:50:00",
                              "9996-01-01 00:55:00");
            verifyNextTimeout(expr,
                              "9996-01-01 00:55:00",
                              "9996-01-01 01:00:00");
            verifyNextTimeout(expr,
                              "9996-01-01 23:55:00",
                              "9996-01-02 00:00:00");
        }

        // 18.2.1.3.7
        verifyNextTimeout(new ScheduleExpression().hour("14").dayOfMonth("Last Thu").month("Nov"),
                          "9996-01-01 00:00:00",
                          "9996-11-28 14:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("14").dayOfMonth("Last Thu").month("Nov"),
                          "9996-11-28 14:00:00",
                          "9997-11-27 14:00:00");

        // 18.2.1.3.8
        verifyNextTimeout(new ScheduleExpression().hour("1").dayOfMonth("-1"),
                          "9996-01-01 00:00:00",
                          "9996-01-30 01:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("1").dayOfMonth("-1"),
                          "9996-01-30 01:00:00",
                          "9996-02-28 01:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("1").dayOfMonth("-1"),
                          "9996-02-28 01:00:00",
                          "9996-03-30 01:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("1").dayOfMonth("-1"),
                          "9996-12-30 01:00:00",
                          "9997-01-30 01:00:00");

        // 18.2.1.3.9
        verifyNextTimeout(new ScheduleExpression().hour("12/2").dayOfMonth("2nd Tue"),
                          "9996-01-01 00:00:00",
                          "9996-01-09 12:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("12/2").dayOfMonth("2nd Tue"),
                          "9996-01-09 12:00:00",
                          "9996-01-09 14:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("12/2").dayOfMonth("2nd Tue"),
                          "9996-01-09 14:00:00",
                          "9996-01-09 16:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("12/2").dayOfMonth("2nd Tue"),
                          "9996-01-09 16:00:00",
                          "9996-01-09 18:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("12/2").dayOfMonth("2nd Tue"),
                          "9996-01-09 18:00:00",
                          "9996-01-09 20:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("12/2").dayOfMonth("2nd Tue"),
                          "9996-01-09 20:00:00",
                          "9996-01-09 22:00:00");
        verifyNextTimeout(new ScheduleExpression().hour("12/2").dayOfMonth("2nd Tue"),
                          "9996-01-09 22:00:00",
                          "9996-02-13 12:00:00");
    }
}
