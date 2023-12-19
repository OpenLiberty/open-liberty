/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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
package com.ibm.ws.logging.collector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

import org.junit.Test;

public class BurstDateFormatterTest {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final boolean aboveJava8 = !System.getProperty("java.version").startsWith("1.");

    // If this test fails, there is likely an update is needed to the getFormatter methods and to DateFormatHelper and DataFormatHelper
    // This test was added due to a change in Java 20 to add an additional space character to the format.
    // See https://bugs.openjdk.org/browse/JDK-8304925
    @Test
    public void testEnglishUnchanged() {
        String pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT, FormatStyle.MEDIUM, Chronology.ofLocale(Locale.ENGLISH), Locale.ENGLISH);
        pattern = getFormatter(pattern);
        StringBuilder sb = new StringBuilder();
        sb.append("M/d/yy");
        if (aboveJava8) {
            sb.append(',');
        }
        sb.append(" H:mm:ss:SSS z");
        assertEquals(sb.toString(), pattern);
    }

    @Test
    public void checkAllLocalesTest() {
        for (Locale locale : Locale.getAvailableLocales()) {
            // TODO: Skip because of bug in JDK 11's my* locale parsing
            if (aboveJava8 && locale.toString().equals("my") || locale.toString().startsWith("my_")) {
                System.out.println("skip locale: " + locale);
                continue;
            }
            String pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT, FormatStyle.MEDIUM, Chronology.ofLocale(locale), locale);
            pattern = getFormatter(pattern);
            BurstDateFormat burst = new BurstDateFormat(pattern, ':', locale);
            SimpleDateFormat simple = (SimpleDateFormat) getFormatter(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale));
            compareFormat(burst, burst.getDateTimeFormatter());
            Date date = new Date();
            if (simple.format(date).equals(burst.getDateTimeFormatter().format(date.toInstant()))) {
                compareFormat(burst, simple);
            } else {
                System.out.println("skip simple check for locale: " + locale);
            }
        }
    }

    @Test
    public void checkAllLocalesWithAllOffsetTest() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(Calendar.MILLISECOND, 143);
        Date initialDate = cal.getTime();

        HashMap<Locale, BurstDateFormat> localeMap = new HashMap<Locale, BurstDateFormat>();

        // Initialize the BurstDateFormatter
        for (Locale locale : Locale.getAvailableLocales()) {
            // TODO: Skip because of bug in JDK 11's my* locale parsing
            if (aboveJava8 && locale.toString().equals("my") || locale.toString().startsWith("my_")) {
                System.out.println("skip locale: " + locale);
                continue;
            }
            String pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT, FormatStyle.MEDIUM, Chronology.ofLocale(locale), locale);
            SimpleDateFormat simple = (SimpleDateFormat) getFormatter(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale));
            pattern = getFormatter(pattern);
            BurstDateFormat burst = new BurstDateFormat(pattern, ':', locale);
            localeMap.put(locale, burst);
            compareFormat(locale, initialDate, burst, burst.getDateTimeFormatter());
            if (simple.format(initialDate).equals(burst.getDateTimeFormatter().format(initialDate.toInstant()))) {
                compareFormat(locale, initialDate, burst, simple);
            } else {
                System.out.println("skip simple check for locale: " + locale);
            }
        }

        // Loop through every digit to make sure it is correct
        for (int i = 0; i < 1000; i++) {
            cal.set(Calendar.MILLISECOND, i);
            Date checkDate = cal.getTime();
            for (Locale locale : localeMap.keySet()) {
                compareFormat(locale, checkDate, localeMap.get(locale), localeMap.get(locale).getDateTimeFormatter());
            }
        }

    }

    @Test
    public void reverseTimeTest() {
        SimpleDateFormat simple = new SimpleDateFormat(DATE_FORMAT);
        BurstDateFormat burst = new BurstDateFormat(simple.toPattern().replace('y', 'u'), '.');
        GregorianCalendar cal = new GregorianCalendar();

        cal.set(Calendar.MILLISECOND, 892);
        Date checkDate = cal.getTime();
        compareFormat(checkDate, burst, simple);

        cal.set(Calendar.MILLISECOND, 384);
        checkDate = cal.getTime();
        compareFormat(checkDate, burst, simple);

        cal.set(Calendar.MILLISECOND, 192);
        checkDate = cal.getTime();
        compareFormat(checkDate, burst, simple);

        cal.add(Calendar.MILLISECOND, -500);
        checkDate = cal.getTime();
        compareFormat(checkDate, burst, simple);
        assertTrue("BurstDateFormat failed to identify valid format", !burst.invalidFormat);
    }

    @Test
    public void noMilliSecondsTest() {
        SimpleDateFormat simple = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        BurstDateFormat burst = new BurstDateFormat(simple.toPattern().replace('y', 'u'), ':');
        compareFormat(burst, simple);
    }

    @Test
    public void regularDateFormatTest() {
        SimpleDateFormat simple = new SimpleDateFormat(DATE_FORMAT);
        BurstDateFormat burst = new BurstDateFormat(simple.toPattern().replace('y', 'u'), '.');
        compareFormat(burst, simple);
        assertTrue("BurstDateFormat failed to identify valid format", !burst.invalidFormat);
    }

    @Test
    public void randomDatesTest() {
        SimpleDateFormat simple = new SimpleDateFormat(DATE_FORMAT);
        BurstDateFormat burst = new BurstDateFormat(simple.toPattern().replace('y', 'u'), '.');

        Random rand = new Random();
        for (long i = 0; i < 5000; i++) {
            compareFormat(new Date(Math.abs(rand.nextLong())), burst, simple);
            assertTrue("BurstDateFormat failed to identify valid format", !burst.invalidFormat);
        }
    }

    @Test
    public void multipleMillisecondFormatTest() {
        SimpleDateFormat simple = new SimpleDateFormat("SSS ss SSS ss");
        BurstDateFormat burst = new BurstDateFormat(simple.toPattern().replace('y', 'u'), ' ');
        GregorianCalendar cal = new GregorianCalendar();
        Date date = cal.getTime();
        compareFormat(date, burst, simple);
        assertTrue("BurstDateFormat failed to identify invalid format", burst.invalidFormat);

        // Test offset
        cal.add(Calendar.MILLISECOND, 74);
        compareFormat(date, burst, simple);
    }

    /**
     * Compares the BurstDateFormat with SimpleDateFormat
     */
    private void compareFormat(BurstDateFormat burst, SimpleDateFormat simple) {
        Date date = new Date();
        compareFormat(date, burst, simple);
    }

    /**
     * Compares the BurstDateFormat with SimpleDateFormat
     */
    private void compareFormat(BurstDateFormat burst, DateTimeFormatter formatter) {
        Date date = new Date();
        compareFormat(date, burst, formatter);
    }

    /**
     * Compares the BurstDateFormat with SimpleDateFormat
     */
    private void compareFormat(Date date, BurstDateFormat burst, SimpleDateFormat simple) {
        compareFormat("BurstDateFormat failed to match SimpleDateFormat", date, burst, simple);
    }

    /**
     * Compares the BurstDateFormat with SimpleDateFormat
     */
    private void compareFormat(Date date, BurstDateFormat burst, DateTimeFormatter formatter) {
        compareFormat("BurstDateFormat failed to match SimpleDateFormat", date, burst, formatter);
    }

    /**
     * Compares the BurstDateFormat with SimpleDateFormat
     */
    private void compareFormat(Locale locale, Date date, BurstDateFormat burst, SimpleDateFormat simple) {
        compareFormat("BurstDateFormat failed to match SimpleDateFormatfor locale " + locale, date, burst, simple);
    }

    /**
     * Compares the BurstDateFormat with DateTimeFormatter
     */
    private void compareFormat(Locale locale, Date date, BurstDateFormat burst, DateTimeFormatter dateFormatter) {
        compareFormat("BurstDateFormat failed to match SimpleDateFormatfor locale " + locale, date, burst, dateFormatter);
    }

    /**
     * Compares the BurstDateFormat with SimpleDateFormat
     */
    private void compareFormat(String msg, Date date, BurstDateFormat burst, SimpleDateFormat simple) {
        long timestamp = (date).getTime();
        String burstString = burst.format(timestamp);
        if (burstString.charAt(0) == '+') {
            burstString = burstString.substring(1);
        }
        String simpleString = simple.format(date);
        assertEquals(msg, simpleString, burstString);
    }

    /**
     * Compares the BurstDateFormat with DateTimeFormatter
     */
    private void compareFormat(String msg, Date date, BurstDateFormat burst, DateTimeFormatter dateFormatter) {
        long timestamp = (date).getTime();
        String burstString = burst.format(timestamp);
        String simpleString = dateFormatter.format(date.toInstant());
        assertEquals(msg, burstString, simpleString);
    }

    /**
     * A partial copy of DataFormatHelper code so that we can work with different locales
     */
    public DateFormat getFormatter(DateFormat formatter) {
        String pattern;
        int patternLength;
        int endOfSecsIndex;
        if (formatter instanceof SimpleDateFormat) {
            // Retrieve the pattern from the formatter, since we will need to
            // modify it.
            SimpleDateFormat sdFormatter = (SimpleDateFormat) formatter;
            pattern = sdFormatter.toPattern();
            // Append milliseconds and timezone after seconds
            patternLength = pattern.length();
            endOfSecsIndex = pattern.lastIndexOf('s') + 1;
            String newPattern = pattern.substring(0, endOfSecsIndex) + ":SSS z";
            if (endOfSecsIndex < patternLength)
                newPattern += pattern.substring(endOfSecsIndex, patternLength);
            // 0-23 hour clock (get rid of any other clock formats and am/pm)
            newPattern = newPattern.replace('h', 'H');
            newPattern = newPattern.replace('K', 'H');
            newPattern = newPattern.replace('k', 'H');
            newPattern = newPattern.replace('a', ' ');
            // Java 20 added a narrow no-break space character into the format (Unicode 202F character)
            newPattern = newPattern.replace('\u202f', ' ');
            newPattern = newPattern.trim();
            sdFormatter.applyPattern(newPattern);
            formatter = sdFormatter;
        } else {
            formatter = new SimpleDateFormat("dd/MMM/yyyy HH:mm:ss:SSS z");
        }
        return formatter;
    }

    public String getFormatter(String pattern) {
        // Append milliseconds and timezone after seconds
        int patternLength = pattern.length();
        int endOfSecsIndex = pattern.lastIndexOf('s') + 1;
        String newPattern = pattern.substring(0, endOfSecsIndex) + ":SSS z";
        if (endOfSecsIndex < patternLength)
            newPattern += pattern.substring(endOfSecsIndex, patternLength);
        // 0-23 hour clock (get rid of any other clock formats and am/pm)
        newPattern = newPattern.replace('h', 'H');
        newPattern = newPattern.replace('K', 'H');
        newPattern = newPattern.replace('k', 'H');
        newPattern = newPattern.replace('a', ' ');
        // Java 20 added a narrow no-break space character into the format (Unicode 202F character)
        newPattern = newPattern.replace('\u202f', ' ');
        newPattern = newPattern.trim();
        return newPattern;
    }
}
