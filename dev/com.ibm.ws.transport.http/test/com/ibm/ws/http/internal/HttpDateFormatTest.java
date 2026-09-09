/*******************************************************************************
 * Copyright (c) 2023, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

import com.ibm.wsspi.http.HttpDateFormat;

/**
 * After changing from using SimpleDateFormat to DateTimeFormatter, this test was created
 * in order to confirm that the formatted date time was unchanged.
 */
public class HttpDateFormatTest {

    private static final HttpDateFormat formatter = HttpDateFormatImpl.getInstance();
    private static final TimeZone gmt = TimeZone.getTimeZone("GMT");

    private void validateFormatter(Date originalDate, String formattedDateTime, Date parseDate, Date parseLower, Date parseUpper,
                                   SimpleDateFormat simpleFormatter) throws Exception {

        long originalTime = originalDate.getTime();
        originalTime -= (originalTime % 1000);

        String simpleFormattedTime = simpleFormatter.format(new Date(originalTime));
        assertEquals(simpleFormattedTime, formattedDateTime);

        assertEquals(parseDate, parseLower);
        assertEquals(parseDate, parseUpper);
        assertEquals(simpleFormatter.parse(formattedDateTime.toLowerCase(Locale.US)), parseLower);
        assertEquals(simpleFormatter.parse(formattedDateTime.toUpperCase(Locale.US)), parseUpper);

        assertEquals(originalTime, parseDate.getTime());
        assertEquals(new Date(originalTime), parseDate);

        Date parseDate2 = formatter.parseTime(formattedDateTime);

        assertEquals(parseDate, parseDate2);
    }

    @Test
    public void testMismatchDate() throws Exception {
        // Invalid date.  The day of the week is not correct for the date.
        // Parsing the date should not get an error.  DateTimeFormatter does a cross check by default and throws
        // an exception.
        String invalidDate = "Wednesday, 29-AUG-22 18:00:40 GMT";
        Date simpleDate = new SimpleDateFormat("EEEEEEEEE, dd-MMM-yy HH:mm:ss z", Locale.US).parse(invalidDate);
        Date parseDate = formatter.parseRFC1036Time(invalidDate);
        Date parseDate2 = formatter.parseTime(invalidDate);
        assertEquals(simpleDate, parseDate);
        assertEquals(simpleDate, parseDate2);
    }

    @Test
    public void testASCIITime() throws Exception {
        SimpleDateFormat simpleFormatter = new SimpleDateFormat("EEE MMM  d HH:mm:ss yyyy", Locale.US);
        simpleFormatter.setTimeZone(gmt);
        Date date = new Date();

        String asciiTime = formatter.getASCIITime(date);
        Date parseDate = formatter.parseASCIITime(asciiTime);
        Date parseDateLower = formatter.parseASCIITime(asciiTime.toLowerCase(Locale.US));
        Date parseDateUpper = formatter.parseASCIITime(asciiTime.toUpperCase(Locale.US));

        validateFormatter(date, asciiTime, parseDate, parseDateLower, parseDateUpper, simpleFormatter);

        int tries = 0;
        boolean success = false;
        while (!success && tries < 40) {
            long start = System.currentTimeMillis();
            long noMillis = start - (start % 1000);
            String asciiTime2 = formatter.getASCIITime();
            String asciiTime3 = formatter.getASCIITime();
            byte[] bytes = formatter.getASCIITimeAsBytes();
            long end = System.currentTimeMillis();
            success = noMillis == (end - (end % 1000));
            if (success) {
                String simpleTime = simpleFormatter.format(new Date(noMillis));
                assertEquals(simpleTime, asciiTime2);
                assertEquals(simpleTime, new String(bytes));
                assertSame(asciiTime2, asciiTime3);
                assertEquals(new Date(noMillis), formatter.parseTime(bytes));
            } else {
                tries++;
                Thread.sleep(50);
            }
        }
    }

    @Test
    public void testNCSATime() throws Exception {
        SimpleDateFormat simpleFormatter = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z", Locale.US);
        Date date = new Date();

        String ncsaTime = formatter.getNCSATime(date);

        String simpleFormattedTime = simpleFormatter.format(date);
        assertEquals(simpleFormattedTime, ncsaTime);

        try {
            formatter.parseTime(ncsaTime);
            fail("Expected exception for NCSA time since we don't have a parser API for it.");
        } catch (ParseException e) {
            // expected
        }

        int tries = 0;
        boolean success = false;
        while (!success && tries < 40) {
            long start = System.currentTimeMillis();
            long noMillis = start - (start % 1000);
            String ncsaTime2 = formatter.getNCSATime();
            String ncsaTime3 = formatter.getNCSATime();
            byte[] bytes = formatter.getNCSATimeAsBytes();
            long end = System.currentTimeMillis();
            success = noMillis == (end - (end % 1000));
            if (success) {
                String simpleTime = simpleFormatter.format(new Date(noMillis));
                assertEquals(simpleTime, ncsaTime2);
                assertEquals(simpleTime, new String(bytes));
                assertSame(ncsaTime2, ncsaTime3);
                try {
                    formatter.parseTime(bytes);
                    fail("Expected exception for NCSA time since we don't have a parser API for it.");
                } catch (ParseException e) {
                    // expected
                }
            } else {
                tries++;
                Thread.sleep(50);
            }
        }
    }

    @Test
    public void testRFC1036Time() throws Exception {
        SimpleDateFormat simpleFormatter = new SimpleDateFormat("EEEEEEEEE, dd-MMM-yy HH:mm:ss z", Locale.US);
        simpleFormatter.setTimeZone(gmt);
        Date date = new Date();

        String RFC1036Time = formatter.getRFC1036Time(date);
        Date parseDate = formatter.parseRFC1036Time(RFC1036Time);
        Date parseDateLower = formatter.parseRFC1036Time(RFC1036Time.toLowerCase(Locale.US));
        Date parseDateUpper = formatter.parseRFC1036Time(RFC1036Time.toUpperCase(Locale.US));

        validateFormatter(date, RFC1036Time, parseDate, parseDateLower, parseDateUpper, simpleFormatter);

        int tries = 0;
        boolean success = false;
        while (!success && tries < 40) {
            long start = System.currentTimeMillis();
            long noMillis = start - (start % 1000);
            String RFC1036Time2 = formatter.getRFC1036Time();
            String RFC1036Time3 = formatter.getRFC1036Time();
            byte[] bytes = formatter.getRFC1036TimeAsBytes();
            long end = System.currentTimeMillis();
            success = noMillis == (end - (end % 1000));
            if (success) {
                String simpleTime = simpleFormatter.format(new Date(noMillis));
                assertEquals(simpleTime, RFC1036Time2);
                assertEquals(simpleTime, new String(bytes));
                assertSame(RFC1036Time2, RFC1036Time3);
                assertEquals(new Date(noMillis), formatter.parseTime(bytes));
            } else {
                tries++;
                Thread.sleep(50);
            }
        }
    }

    @Test
    public void testRFC1123Time() throws Exception {
        SimpleDateFormat simpleFormatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        simpleFormatter.setTimeZone(gmt);
        Date date = new Date();

        String RFC1123Time = formatter.getRFC1123Time(date);
        Date parseDate = formatter.parseRFC1123Time(RFC1123Time);
        Date parseDateLower = formatter.parseRFC1123Time(RFC1123Time.toLowerCase(Locale.US));
        Date parseDateUpper = formatter.parseRFC1123Time(RFC1123Time.toUpperCase(Locale.US));

        validateFormatter(date, RFC1123Time, parseDate, parseDateLower, parseDateUpper, simpleFormatter);

        int tries = 0;
        boolean success = false;
        while (!success && tries < 40) {
            long start = System.currentTimeMillis();
            long noMillis = start - (start % 1000);
            String RFC1123Time2 = formatter.getRFC1123Time();
            String RFC1123Time3 = formatter.getRFC1123Time();
            byte[] bytes = formatter.getRFC1123TimeAsBytes();
            long end = System.currentTimeMillis();
            success = noMillis == (end - (end % 1000));
            if (success) {
                String simpleTime = simpleFormatter.format(new Date(noMillis));
                assertEquals(simpleTime, RFC1123Time2);
                assertEquals(simpleTime, new String(bytes));
                assertSame(RFC1123Time2, RFC1123Time3);
                assertEquals(new Date(noMillis), formatter.parseTime(bytes));
            } else {
                tries++;
                Thread.sleep(50);
            }
        }
    }

    @Test
    public void testRFC2109Time() throws Exception {
        SimpleDateFormat simpleFormatter = new SimpleDateFormat("EEE, dd-MMM-yy HH:mm:ss z", Locale.US);
        simpleFormatter.setTimeZone(gmt);
        Date date = new Date();

        String RFC2109Time = formatter.getRFC2109Time(date);
        Date parseDate = formatter.parseRFC2109Time(RFC2109Time);
        Date parseDateLower = formatter.parseRFC2109Time(RFC2109Time.toLowerCase(Locale.US));
        Date parseDateUpper = formatter.parseRFC2109Time(RFC2109Time.toUpperCase(Locale.US));

        validateFormatter(date, RFC2109Time, parseDate, parseDateLower, parseDateUpper, simpleFormatter);

        int tries = 0;
        boolean success = false;
        while (!success && tries < 40) {
            long start = System.currentTimeMillis();
            long noMillis = start - (start % 1000);
            String RFC2109Time2 = formatter.getRFC2109Time();
            String RFC2109Time3 = formatter.getRFC2109Time();
            byte[] bytes = formatter.getRFC2109TimeAsBytes();
            long end = System.currentTimeMillis();
            success = noMillis == (end - (end % 1000));
            if (success) {
                String simpleTime = simpleFormatter.format(new Date(noMillis));
                assertEquals(simpleTime, RFC2109Time2);
                assertEquals(simpleTime, new String(bytes));
                assertSame(RFC2109Time2, RFC2109Time3);
                assertEquals(new Date(noMillis), formatter.parseTime(bytes));
            } else {
                tries++;
                Thread.sleep(50);
            }
        }
    }

    /**
     * RFC 7231 §7.1.1.1: a 2-digit year that appears more than 50 years in the
     * future must be interpreted as the most recent past year with the same digits.
     */
    @Test
    public void testRFC1036TwoDigitYearRollover() throws Exception {
        assertTwoDigitYearWindow((String input) -> formatter.parseRFC1036Time(input),
                                 "Saturday, 28-Jul-%s 16:11:14 GMT");
    }

    @Test
    public void testRFC2109TwoDigitYearRollover() throws Exception {
        assertTwoDigitYearWindow((String input) -> formatter.parseRFC2109Time(input),
                                 "Sat, 28-Jul-%s 16:11:14 GMT");
    }

    /**
     * Verifies the full RFC 7231 §7.1.1.1 window for a 2-digit-year parser.
     * The formatter base is (currentYear - 49), giving the window [currentYear-49, currentYear+50].
     * RFC 7231 requires roll-back only for dates *more than* 50 years in the future (strict),
     * so exactly 50 years out must NOT roll back.
     * Iterates all 100 possible 2-digit values and checks each maps to the correct full year.
     */
    private void assertTwoDigitYearWindow(ParseFunction parser, String inputTemplate) throws Exception {
        int fullCurrentYear = Year.now().getValue();
        int baseYear = fullCurrentYear - 49; // formatter base: window is [currentYear-49, currentYear+50]

        for (int i = 0; i < 100; ++i) {
            int twoDigitValue = (fullCurrentYear + i) % 100; // wraps at 100
            int expectedYear = baseYear + ((twoDigitValue - (baseYear % 100) + 100) % 100);
            String input = String.format(inputTemplate, String.format("%02d", twoDigitValue));
            Date parsed = parser.parse(input);
            Date expected = Date.from(ZonedDateTime.of(expectedYear, 7, 28, 16, 11, 14, 0, ZoneOffset.UTC).toInstant());
            assertEquals("Mismatch for 2-digit year " + String.format("%02d", twoDigitValue), expected, parsed);
        }

        // Explicit boundary checks per RFC 7231 §7.1.1.1:
        // exactly 50 years out must NOT roll back; 51 years out must roll back.
        int twoDigitAt50 = (fullCurrentYear + 50) % 100;
        int twoDigitAt51 = (fullCurrentYear + 51) % 100;

        Date parsedAt50 = parser.parse(String.format(inputTemplate, String.format("%02d", twoDigitAt50)));
        assertEquals("Exactly 50 years out must not roll back",
                     Date.from(ZonedDateTime.of(fullCurrentYear + 50, 7, 28, 16, 11, 14, 0, ZoneOffset.UTC).toInstant()),
                     parsedAt50);

        Date parsedAt51 = parser.parse(String.format(inputTemplate, String.format("%02d", twoDigitAt51)));
        assertEquals("51 years out must roll back",
                     Date.from(ZonedDateTime.of(fullCurrentYear - 49, 7, 28, 16, 11, 14, 0, ZoneOffset.UTC).toInstant()),
                     parsedAt51);
    }

    @FunctionalInterface
    private interface ParseFunction {
        Date parse(String input) throws Exception;
    }
}
