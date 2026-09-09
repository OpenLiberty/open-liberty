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
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
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
        Date oneMinuteAgo = new Date(Instant.now().minusSeconds(60).toEpochMilli());
        assertTwoDigitYearWindow((String input) -> formatter.parseRFC1036Time(input),
                                 getSubstituteYearString(formatter.getRFC1036Time(oneMinuteAgo)), false);

        Date oneMinuteFromNow = new Date(Instant.now().plusSeconds(60).toEpochMilli());
        assertTwoDigitYearWindow((String input) -> formatter.parseRFC1036Time(input),
                                 getSubstituteYearString(formatter.getRFC1036Time(oneMinuteFromNow)), true);
    }

    /**
     * Converts an actual two digit Data string to use %s for the year so the
     * year can be substituted.
     */
    private String getSubstituteYearString(String actualFormattedString) {
        int hyphenIndex = actualFormattedString.lastIndexOf('-');
        int spaceIndex = actualFormattedString.indexOf(' ', hyphenIndex);
        assertEquals(3, spaceIndex - hyphenIndex);
        String returnString = actualFormattedString.substring(0, hyphenIndex + 1) + "%s" + actualFormattedString.substring(spaceIndex);
        assertEquals(actualFormattedString,
                     String.format(returnString, String.format("%02d", Integer.parseInt(actualFormattedString.substring(hyphenIndex + 1, spaceIndex)))));
        return returnString;
    }

    @Test
    public void testRFC2109TwoDigitYearRollover() throws Exception {
        Date oneMinuteAgo = new Date(Instant.now().minusSeconds(60).toEpochMilli());
        assertTwoDigitYearWindow((String input) -> formatter.parseRFC2109Time(input),
                                 getSubstituteYearString(formatter.getRFC2109Time(oneMinuteAgo)), false);

        Date oneMinuteFromNow = new Date(Instant.now().plusSeconds(60).toEpochMilli());
        assertTwoDigitYearWindow((String input) -> formatter.parseRFC2109Time(input),
                                 getSubstituteYearString(formatter.getRFC2109Time(oneMinuteFromNow)), true);
    }

    /**
     * Verifies the full RFC 7231 §7.1.1.1 window for a 2-digit-year parser.
     * The formatter base is (currentYear - 49), giving the window [currentYear-49, currentYear+50].
     * RFC 7231 requires roll-back only for dates *more than* 50 years in the future (strict),
     * so exactly 50 years out must NOT roll back.
     * Iterates all 100 possible 2-digit values and checks each maps to the correct full year.
     */
    private void assertTwoDigitYearWindow(ParseFunction parser, String inputTemplate, boolean isFuture) throws Exception {
        int fullCurrentYear = Year.now().getValue();
        // If isFuture is true, we need to treat it like it is next year because it is more than 50 years since the date
        // is already in the future from now.
        int baseYear = fullCurrentYear - (isFuture ? 50 : 49); // formatter base: window is [currentYear-49, currentYear+50]

        for (int i = 0; i < 100; ++i) {
            int twoDigitValue = (fullCurrentYear + i) % 100; // wraps at 100
            int expectedYear = baseYear + ((twoDigitValue - (baseYear % 100) + 100) % 100);
            String input = String.format(inputTemplate, String.format("%02d", twoDigitValue));
            Date parsed = parser.parse(input);
            assertEquals("Mismatch for 2-digit year " + String.format("%02d", twoDigitValue), expectedYear, parsed.toInstant().atZone(ZoneId.of("GMT")).getYear());
        }

        // Explicit boundary checks per RFC 7231 §7.1.1.1:
        // exactly 50 years out must NOT roll back; 51 years out must roll back.
        int twoDigitAt49 = (fullCurrentYear + 49) % 100;
        int twoDigitAt50 = (fullCurrentYear + 50) % 100;
        int twoDigitAt51 = (fullCurrentYear + 51) % 100;

        Date parsedAt49 = parser.parse(String.format(inputTemplate, String.format("%02d", twoDigitAt49)));
        assertEquals("Exactly 49 / 50 years out must not roll back", fullCurrentYear + 49, parsedAt49.toInstant().atZone(ZoneId.of("GMT")).getYear());

        Date parsedAt50 = parser.parse(String.format(inputTemplate, String.format("%02d", twoDigitAt50)));
        // If isFuture is true, we need to treat it like it is next year because it is more than 50 years since the date
        // is already in the future from now.
        int expectedYear = isFuture ? fullCurrentYear - 50 : fullCurrentYear + 50;
        assertEquals("Exactly 50 / 51 years out must not roll back or rollback depending on time", expectedYear, parsedAt50.toInstant().atZone(ZoneId.of("GMT")).getYear());

        Date parsedAt51 = parser.parse(String.format(inputTemplate, String.format("%02d", twoDigitAt51)));
        assertEquals("Exactly 51 / 52 years out must not roll back", fullCurrentYear - 49, parsedAt51.toInstant().atZone(ZoneId.of("GMT")).getYear());
    }

    @FunctionalInterface
    private interface ParseFunction {
        Date parse(String input) throws Exception;
    }
}
