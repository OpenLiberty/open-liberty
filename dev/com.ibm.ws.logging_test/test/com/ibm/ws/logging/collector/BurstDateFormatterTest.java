/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.collector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

    @Test
    public void checkAllLocalesTest() {
        for (Locale locale : Locale.getAvailableLocales()) {
            // TODO: Skip because of bug in JDK 11's my* locale parsing
            if (aboveJava8 && locale.toString().equals("my") || locale.toString().startsWith("my_")) {
                System.out.println("skip locale: " + locale);
                continue;
            }
            SimpleDateFormat simple = (SimpleDateFormat) getFormatter(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale));
            BurstDateFormat burst = new BurstDateFormat(simple);
            compareFormat(burst, simple);
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
            SimpleDateFormat simple = (SimpleDateFormat) getFormatter(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale));
            BurstDateFormat burst = new BurstDateFormat(simple);
            localeMap.put(locale, burst);
            compareFormat(locale, initialDate, burst, simple);
        }

        // Loop through every digit to make sure it is correct
        for (int i = 0; i < 1000; i++) {
            cal.set(Calendar.MILLISECOND, i);
            Date checkDate = cal.getTime();
            for (Locale locale : localeMap.keySet()) {
                compareFormat(locale, checkDate, localeMap.get(locale), localeMap.get(locale).getSimpleDateFormat());
            }
        }

    }

    @Test
    public void reverseTimeTest() {
        SimpleDateFormat simple = new SimpleDateFormat(DATE_FORMAT);
        BurstDateFormat burst = new BurstDateFormat(simple);
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
        BurstDateFormat burst = new BurstDateFormat(simple);
        compareFormat(burst, simple);
    }

    @Test
    public void regularDateFormatTest() {
        SimpleDateFormat simple = new SimpleDateFormat(DATE_FORMAT);
        BurstDateFormat burst = new BurstDateFormat(simple);
        compareFormat(burst, simple);
        assertTrue("BurstDateFormat failed to identify valid format", !burst.invalidFormat);
    }

    @Test
    public void randomDatesTest() {
        SimpleDateFormat simple = new SimpleDateFormat(DATE_FORMAT);
        BurstDateFormat burst = new BurstDateFormat(simple);

        Random rand = new Random();
        for (long i = 0; i < 5000; i++) {
            compareFormat(new Date(rand.nextLong()), burst, simple);
            assertTrue("BurstDateFormat failed to identify valid format", !burst.invalidFormat);
        }
    }

    @Test
    public void multipleMillisecondFormatTest() {
        SimpleDateFormat simple = new SimpleDateFormat("SSS ss SSS ss");
        BurstDateFormat burst = new BurstDateFormat(simple);
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
    private void compareFormat(Date date, BurstDateFormat burst, SimpleDateFormat simple) {
        compareFormat("BurstDateFormat failed to match SimpleDateFormat", date, burst, simple);
    }

    /**
     * Compares the BurstDateFormat with SimpleDateFormat
     */
    private void compareFormat(Locale locale, Date date, BurstDateFormat burst, SimpleDateFormat simple) {
        compareFormat("BurstDateFormat failed to match SimpleDateFormatfor locale " + locale, date, burst, simple);
    }

    /**
     * Compares the BurstDateFormat with SimpleDateFormat
     */
    private void compareFormat(String msg, Date date, BurstDateFormat burst, SimpleDateFormat simple) {
        long timestamp = (date).getTime();
        String burstString = burst.format(timestamp);
        String simpleString = simple.format(date);
        assertEquals(msg, burstString, simpleString);
    }

    /**
     * A partial copy of DateFormatHelper.getDateFormat so that we can work with different locales
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
            newPattern = newPattern.trim();
            sdFormatter.applyPattern(newPattern);
            formatter = sdFormatter;
        } else {
            formatter = new SimpleDateFormat("dd/MMM/yyyy HH:mm:ss:SSS z");
        }
        return formatter;
    }
}
