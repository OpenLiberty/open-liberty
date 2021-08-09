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
package com.ibm.ws.metadata.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.Test;

/**
 * Unit tests for com.ibm.ws.metadata.ejb.AutomaticTimerBean.DateHelper.parse(String)
 */
public class AutoTimerBeanTest {

    @Test
    public void testBadInputParseSchedule() throws Exception {
        testBadInputParseScheduleHelper(false);
    }

    @Test
    public void testBadInputParseScheduleJavaTimeMethod() throws Exception {
        testBadInputParseScheduleHelper(true);
    }

    /**
     * Throws a bunch of bad input and makes sure we fail
     */
    private void testBadInputParseScheduleHelper(boolean isJavaTimeMethod) throws Exception {

        // (c)entury (y)ear (M)onth (d)ay (h)our (m)inute (s)econd
        testInvalidInput("2", "y", isJavaTimeMethod);
        testInvalidInput("20", "yy", isJavaTimeMethod);
        testInvalidInput("202", "cyy", isJavaTimeMethod);
        // ccyy -> valid
        testInvalidInput("2020-", "ccyy-", isJavaTimeMethod);
        testInvalidInput("2020-2", "ccyy-M", isJavaTimeMethod);
        // ccyy-MM -> valid
        testInvalidInput("2020-02-", "ccyy-MM-", isJavaTimeMethod);
        testInvalidInput("2020-02-2", "ccyy-MM-d", isJavaTimeMethod);
        // ccyy-MM-dd -> valid
        testInvalidInput("2020-02-02T", "ccyy-MM-ddT", isJavaTimeMethod);
        testInvalidInput("2020-02-02T0", "ccyy-MM-ddTh", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00", "ccyy-MM-ddThh", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:", "ccyy-MM-ddThh:", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:0", "ccyy-MM-ddThh:m", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:00", "ccyy-MM-ddThh:mm", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:00:", "ccyy-MM-ddThh:mm:", isJavaTimeMethod);
        // ccyy-MM-ddThh:mm:s -> valid
        // ccyy-MM-ddThh:mm:ss -> valid
        testInvalidInput("2020-02-02t00:02:00", "ccyy-MM-ddthh:mm:ss", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:2:00", "ccyy-MM-ddThh:m:ss", isJavaTimeMethod);
        testInvalidInput("2020-02-02T2:02:00", "ccyy-MM-ddTh:mm:ss", isJavaTimeMethod);
        testInvalidInput("2020-02-2T02:02:00", "ccyy-MM-dThh:mm:ss", isJavaTimeMethod);
        testInvalidInput("2020-2-02T02:02:00", "ccyy-M-ddThh:mm:ss", isJavaTimeMethod);
        testInvalidInput("20-02-02T02:02:00", "yy-MM-ddThh:mm:ss", isJavaTimeMethod);
        testInvalidInput("20200202T02:02:00", "ccyyMMddThh:mm:ss", isJavaTimeMethod);
        testInvalidInput("20200202T020200", "ccyyMMddThhmmss", isJavaTimeMethod);
        testInvalidInput("2020-02-02T020200", "ccyy-MM-ddThhmmss", isJavaTimeMethod);

        testInvalidInput("2020-02-02T00:00:00:", "ccyy-MM-ddThh:mm:ss:", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:00:00:0", "ccyy-MM-ddThh:mm:ss:m", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:00:00:00", "ccyy-MM-ddThh:mm:ss:mm", isJavaTimeMethod);
        testInvalidInput("2020-02T00:00:00", "ccyy-MMThh:mm:ss", isJavaTimeMethod);
        testInvalidInput("2020T00:00:00", "ccyyThh:mm:ss", isJavaTimeMethod);

        testInvalidInput("HammerTime", "HammerTime", isJavaTimeMethod);
        testInvalidInput("0000", "ccyy", isJavaTimeMethod);
        testInvalidInput("0000-00", "ccyy-MM", isJavaTimeMethod);
        testInvalidInput("0000-00-00", "ccyy-MM-dd", isJavaTimeMethod);
        testInvalidInput("2020-20", "ccyy-MM", isJavaTimeMethod);

        testInvalidInput("T02:02", "Thh:mm", isJavaTimeMethod);
        // hh:mm:ss -> valid
        // hh:mm:s -> valid
        testInvalidInput("T02:02:02", "Thh:mm:ss", isJavaTimeMethod);
        testInvalidInput("020202", "hhmmss", isJavaTimeMethod);

        // Z = UTC timezone
        // ccyy-MM-ddThh:mm:ssZ -> valid
        // ccyy-MM-ddThh:mm:sZ -> valid
        testInvalidInput("2020-02-02T02:02:02z", "ccyy-MM-ddThh:mm:ssz", isJavaTimeMethod);
        testInvalidInput("2020-02-02t02:02:02z", "ccyy-MM-ddthh:mm:ssz", isJavaTimeMethod);
        // hh:mm:ssZ -> valid
        // hh:mm:sZ -> valid
        // ccyy-MM-ddZ -> valid
        testInvalidInput("02:02:02Z+05:00", "hh:mm:ssZ+hh:mm", isJavaTimeMethod);
        testInvalidInput("2020-02-02Z+05:00", "ccyy-MM-ddZ+hh:mm", isJavaTimeMethod);
        testInvalidInput("2020-02-02T02:02:02Z+05:00", "ccyy-MM-ddThh:mm:ssZ+hh:mm", isJavaTimeMethod);

        testInvalidInput("2020-093", "ccyy-ddd", isJavaTimeMethod);

        // (w)eek
        testInvalidInput("2020-W01", "ccyy-Www", isJavaTimeMethod);
        testInvalidInput("2020-W01-4", "ccyy-Www-d", isJavaTimeMethod);

        // .(m)illiseconds
        // ccyy-MM-ddThh:mm:ss.m -> valid
        testInvalidInput("2020-02-02T02:02:2.m", "ccyy-MM-ddThh:mm:s.m", isJavaTimeMethod);
        // ccyy-MM-ddThh:mm:ss.mm -> valid
        // ccyy-MM-ddThh:mm:ss.mmm -> valid
        // ccyy-MM-ddThh:mm:ss.mmmm -> valid
        // ccyy-MM-ddThh:mm:ss.mmmmm -> valid
        // ccyy-MM-ddThh:mm:ss.mmmmmm+infinity -> valid
        // hh:mm:ss.m -> valid
        // hh:mm:s.m -> valid

        // Timezones
        testInvalidInput("2020-02-02T00:02:00+", "ccyy-MM-ddThh:mm:ss+", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:02:00-", "ccyy-MM-ddThh:mm:ss-", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:02:00+5", "ccyy-MM-ddThh:mm:ss+h", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:02:00+05", "ccyy-MM-ddThh:mm:ss+hh", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:02:00+05:", "ccyy-MM-ddThh:mm:ss+hh:", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:02:00+05:0", "ccyy-MM-ddThh:mm:ss+hh:m", isJavaTimeMethod);
        // ccyy-MM-ddThh:mm:ss+hh:mm -> valid
        // ccyy-MM-ddThh:mm:s+hh:mm -> valid
        // ccyy-MM-ddThh:mm:ss.m+hh:mm -> valid
        // ccyy-MM-ddThh:mm:s.m+hh:mm -> valid
        // ccyy-MM-ddThh:mm:s.mm+hh:mm -> valid
        testInvalidInput("2020-02-02T00:02:00+5:05", "ccyy-MM-ddThh:mm:ss+h:mm", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:02:00?05:05", "ccyy-MM-ddThh:mm:ss?hh:mm", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:02:00+05:005", "ccyy-MM-ddThh:mm:ss+hh:mmm", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:02:00+05:00:", "ccyy-MM-ddThh:mm:ss+hh:mm:", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:02:00+05:00:5", "ccyy-MM-ddThh:mm:ss+hh:mm:s", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:02:00+05:00:05", "ccyy-MM-ddThh:mm:ss+hh:mm:ss", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:02:00+05:00:050", "ccyy-MM-ddThh:mm:ss+hh:mm:sss", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:02:00+05:00.5", "ccyy-MM-ddThh:mm:ss+hh:mm.s", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:02:00+05:00.05", "ccyy-MM-ddThh:mm:ss+hh:mm.ss", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:02:00+05:00:05.5", "ccyy-MM-ddThh:mm:ss+hh:mm:ss.m", isJavaTimeMethod);
        testInvalidInput("2020-02-02T00:02:00+05:00:05.55", "ccyy-MM-ddThh:mm:ss+hh:mm:ss.mm", isJavaTimeMethod);
        // hh:mm:ss+hh:mm -> valid
        // hh:mm:ss.m+hh:mm -> valid
        // hh:mm:s+hh:mm -> valid
        // hh:mm:s.m+hh:mm -> valid
        // ccyy-MM-dd+hh:mm -> valid

        //out of bounds
        testInvalidInput("2020-02-40", "ccyy-MM-(dd) (out of bounds)", isJavaTimeMethod);
        testInvalidInput("2020-15-04", "ccyy-(MM)-dd (out of bounds)", isJavaTimeMethod);
        // (ccyy)-MM-dd (out of bounds) -> valid
        testInvalidInput("30:15:04", "(hh):mm:ss (out of bounds)", isJavaTimeMethod);
        testInvalidInput("10:72:04", "hh:(mm):ss (out of bounds)", isJavaTimeMethod);
        testInvalidInput("10:15:72", "hh:mm:(ss) (out of bounds)", isJavaTimeMethod);

    }

    @Test
    public void testParseScheduleJavaTimeMethod() throws Exception {
        testParseScheduleHelper(true);
    }

    /**
     * Tests that valid input is correctly parsed and returns expected values
     */
    private void testParseScheduleHelper(boolean isJavaTimeMethod) throws Exception {
        // (c)entury (y)ear (M)onth (d)ay (h)our (m)inute (s)econd

        //basic
        // ccyy
        testReturnedEpochTime("2020", 1577836800, 2020, 1, 1, isJavaTimeMethod);
        // ccyy-MM
        testReturnedEpochTime("2020-01", 1577836800, 2020, 1, 1, isJavaTimeMethod);
        // ccyy-MM-dd
        testReturnedEpochTime("2020-01-01", 1577836800, 2020, 1, 1, isJavaTimeMethod);
        testReturnedEpochTime("2020-06-23", 1592870400, 2020, 6, 23, isJavaTimeMethod);
        // ccyy-MM-ddThh:mm:s
        testReturnedEpochTime("2020-06-23T10:23:3", 1592907783, 2020, 6, 23, isJavaTimeMethod);
        // ccyy-MM-ddThh:mm:ss
        testReturnedEpochTime("2020-06-23T10:23:03", 1592907783, 2020, 6, 23, isJavaTimeMethod);
        // hh:mm:ss
        testReturnedEpochTime("23:23:03", 84183, 1970, 1, 1, isJavaTimeMethod);
        // hh:mm:s
        testReturnedEpochTime("23:23:3", 84183, 1970, 1, 1, isJavaTimeMethod);

        // Z = UTC timezone
        // ccyy-MM-ddThh:mm:ssZ
        testReturnedEpochTimeSetOffset("2020-06-23T10:23:03Z", 1592907783, 0, isJavaTimeMethod);
        // ccyy-MM-ddThh:mm:sZ
        testReturnedEpochTimeSetOffset("2020-06-23T10:23:3Z", 1592907783, 0, isJavaTimeMethod);
        // hh:mm:ssZ
        testReturnedEpochTimeSetOffset("23:23:03Z", 84183, 0, isJavaTimeMethod);
        // hh:mm:sZ
        testReturnedEpochTimeSetOffset("23:23:3Z", 84183, 0, isJavaTimeMethod);
        // ccyy-MM-ddZ
        testReturnedEpochTimeSetOffset("2020-06-23Z", 1592870400, 0, isJavaTimeMethod);

        // .(m)illiseconds
        // ccyy-MM-ddThh:mm:ss.m
        testReturnedEpochTimeWithMilli("2020-06-23T10:23:03.5", 1592907783, 2020, 6, 23, isJavaTimeMethod, 500);
        // ccyy-MM-ddThh:mm:ss.mm
        testReturnedEpochTimeWithMilli("2020-06-23T10:23:03.55", 1592907783, 2020, 6, 23, isJavaTimeMethod, 550);
        testReturnedEpochTimeWithMilli("2020-06-23T10:23:03.05", 1592907783, 2020, 6, 23, isJavaTimeMethod, 50);
        // ccyy-MM-ddThh:mm:ss.mmm
        testReturnedEpochTimeWithMilli("2020-06-23T10:23:03.555", 1592907783, 2020, 6, 23, isJavaTimeMethod, 555);
        testReturnedEpochTimeWithMilli("2020-06-23T10:23:03.005", 1592907783, 2020, 6, 23, isJavaTimeMethod, 5);
        // ccyy-MM-ddThh:mm:ss.mmmm
        testReturnedEpochTimeWithMilli("2020-06-23T10:23:03.5555", 1592907783, 2020, 6, 23, isJavaTimeMethod, 555);
        // ccyy-MM-ddThh:mm:ss.mmmmm
        testReturnedEpochTimeWithMilli("2020-06-23T10:23:03.55555", 1592907783, 2020, 6, 23, isJavaTimeMethod, 555);
        testReturnedEpochTimeWithMilli("2020-06-23T10:23:03.55555555555555555555555555555555555", 1592907783, 2020, 6, 23, isJavaTimeMethod, 555);
        // hh:mm:ss.m
        testReturnedEpochTimeWithMilli("23:23:03.5", 84183, 1970, 1, 1, isJavaTimeMethod, 500);
        testReturnedEpochTimeWithMilli("23:23:03.55", 84183, 1970, 1, 1, isJavaTimeMethod, 550);
        testReturnedEpochTimeWithMilli("23:23:03.555", 84183, 1970, 1, 1, isJavaTimeMethod, 555);
        testReturnedEpochTimeWithMilli("23:23:03.5555555555555555555555555555555", 84183, 1970, 1, 1, isJavaTimeMethod, 555);
        // hh:mm:s.m
        testReturnedEpochTimeWithMilli("23:23:3.5", 84183, 1970, 1, 1, isJavaTimeMethod, 500);
        testReturnedEpochTimeWithMilli("23:23:3.55", 84183, 1970, 1, 1, isJavaTimeMethod, 550);
        testReturnedEpochTimeWithMilli("23:23:3.555", 84183, 1970, 1, 1, isJavaTimeMethod, 555);
        testReturnedEpochTimeWithMilli("23:23:3.555555555555555555555555555555555", 84183, 1970, 1, 1, isJavaTimeMethod, 555);
        // ccyy-MM-ddThh:mm:s.m
        testReturnedEpochTimeWithMilli("2020-06-23T10:23:3.5", 1592907783, 2020, 6, 23, isJavaTimeMethod, 500);
        testReturnedEpochTimeWithMilli("2020-06-23T10:23:3.55", 1592907783, 2020, 6, 23, isJavaTimeMethod, 550);
        testReturnedEpochTimeWithMilli("2020-06-23T10:23:3.555", 1592907783, 2020, 6, 23, isJavaTimeMethod, 555);
        testReturnedEpochTimeWithMilli("2020-06-23T10:23:3.555555555555555555555555", 1592907783, 2020, 6, 23, isJavaTimeMethod, 555);

        // Timezones
        // ccyy-MM-ddThh:mm:ss+hh:mm
        testReturnedEpochTimeSetOffset("2020-06-23T10:23:03+05:00", 1592907783, 18000, isJavaTimeMethod);
        testReturnedEpochTimeSetOffset("2020-06-23T10:23:03-05:00", 1592907783, -18000, isJavaTimeMethod);
        testReturnedEpochTimeSetOffset("2020-06-23T10:23:03+05:30", 1592907783, 19800, isJavaTimeMethod);
        // ccyy-MM-ddThh:mm:s+hh:mm
        testReturnedEpochTimeSetOffset("2020-06-23T10:23:3+05:00", 1592907783, 18000, isJavaTimeMethod);
        testReturnedEpochTimeSetOffset("2020-06-23T10:23:3-05:00", 1592907783, -18000, isJavaTimeMethod);
        // hh:mm:ss+hh:mm
        testReturnedEpochTimeSetOffset("23:23:03+05:00", 84183, 18000, isJavaTimeMethod);
        testReturnedEpochTimeSetOffset("23:23:03-05:00", 84183, -18000, isJavaTimeMethod);
        // hh:mm:s+hh:mm
        testReturnedEpochTimeSetOffset("23:23:3+05:00", 84183, 18000, isJavaTimeMethod);
        testReturnedEpochTimeSetOffset("23:23:3-05:00", 84183, -18000, isJavaTimeMethod);
        // ccyy-MM-dd+hh:mm
        testReturnedEpochTimeSetOffset("2020-01-01+05:00", 1577836800, 18000, isJavaTimeMethod);
        testReturnedEpochTimeSetOffset("2020-01-01-05:00", 1577836800, -18000, isJavaTimeMethod);

        // Timezones + .(m)illiseconds
        // ccyy-MM-ddThh:mm:ss.m+hh:mm
        testReturnedEpochTimeSetOffsetWithMilli("2020-06-23T10:23:03.5+05:00", 1592907783, 18000, isJavaTimeMethod, 500);
        testReturnedEpochTimeSetOffsetWithMilli("2020-06-23T10:23:03.5-05:00", 1592907783, -18000, isJavaTimeMethod, 500);
        // ccyy-MM-ddThh:mm:s.m+hh:mm
        testReturnedEpochTimeSetOffsetWithMilli("2020-06-23T10:23:3.5+05:00", 1592907783, 18000, isJavaTimeMethod, 500);
        testReturnedEpochTimeSetOffsetWithMilli("2020-06-23T10:23:3.5-05:00", 1592907783, -18000, isJavaTimeMethod, 500);
        // ccyy-MM-ddThh:mm:s.mm+hh:mm
        testReturnedEpochTimeSetOffsetWithMilli("2020-06-23T10:23:3.55-05:00", 1592907783, -18000, isJavaTimeMethod, 550);
        testReturnedEpochTimeSetOffsetWithMilli("2020-06-23T10:23:3.55+05:00", 1592907783, 18000, isJavaTimeMethod, 550);
        testReturnedEpochTimeSetOffsetWithMilli("2020-06-23T10:23:3.555-05:00", 1592907783, -18000, isJavaTimeMethod, 555);
        testReturnedEpochTimeSetOffsetWithMilli("2020-06-23T10:23:3.5555-05:00", 1592907783, -18000, isJavaTimeMethod, 555);
        testReturnedEpochTimeSetOffsetWithMilli("2020-06-23T10:23:3.555555555555555555555555-05:00", 1592907783, -18000, isJavaTimeMethod, 555);
        // hh:mm:ss.m+hh:mm
        testReturnedEpochTimeSetOffsetWithMilli("23:23:03.5+05:00", 84183, 18000, isJavaTimeMethod, 500);
        testReturnedEpochTimeSetOffsetWithMilli("23:23:03.5-05:00", 84183, -18000, isJavaTimeMethod, 500);
        testReturnedEpochTimeSetOffsetWithMilli("23:23:03.55+05:00", 84183, 18000, isJavaTimeMethod, 550);
        testReturnedEpochTimeSetOffsetWithMilli("23:23:03.555+05:00", 84183, 18000, isJavaTimeMethod, 555);
        testReturnedEpochTimeSetOffsetWithMilli("23:23:03.5555555555555555555555555555555555+05:00", 84183, 18000, isJavaTimeMethod, 555);
        // hh:mm:s.m+hh:mm
        testReturnedEpochTimeSetOffsetWithMilli("23:23:3.5+05:00", 84183, 18000, isJavaTimeMethod, 500);
        testReturnedEpochTimeSetOffsetWithMilli("23:23:3.5-05:00", 84183, -18000, isJavaTimeMethod, 500);
        testReturnedEpochTimeSetOffsetWithMilli("23:23:3.55555555555555555555555555555555555-05:00", 84183, -18000, isJavaTimeMethod, 555);

        // Z(UTC) + .(m)illeseconds
        // ccyy-MM-ddThh:mm:ss.mZ
        testReturnedEpochTimeSetOffsetWithMilli("2020-06-23T10:23:03.5Z", 1592907783, 0, isJavaTimeMethod, 500);
        // ccyy-MM-ddThh:mm:s.mZ
        testReturnedEpochTimeSetOffsetWithMilli("2020-06-23T10:23:3.5Z", 1592907783, 0, isJavaTimeMethod, 500);
        testReturnedEpochTimeSetOffsetWithMilli("2020-06-23T10:23:3.55Z", 1592907783, 0, isJavaTimeMethod, 550);
        testReturnedEpochTimeSetOffsetWithMilli("2020-06-23T10:23:3.5555555555555555555555Z", 1592907783, 0, isJavaTimeMethod, 555);
        // ccyy-MM-ddThh:mm:s.mmZ
        testReturnedEpochTimeSetOffsetWithMilli("2020-06-23T10:23:3.55Z", 1592907783, 0, isJavaTimeMethod, 550);
        // hh:mm:ss.m+hh:mm
        testReturnedEpochTimeSetOffsetWithMilli("23:23:03.5555555555555555555555555555555555Z", 84183, 0, isJavaTimeMethod, 555);

        // "1944-06-06" (negative epoch)
        testReturnedEpochTime("1944-06-06", -806976000, 1944, 6, 6, isJavaTimeMethod);

    }

    private void testReturnedEpochTime(String dateTime, int expectedEpochInSeconds, int yyyy, int mm, int dd, boolean isJavaTimeMethod) throws Exception {
        testReturnedEpochTimeWithMilli(dateTime, expectedEpochInSeconds, yyyy, mm, dd, isJavaTimeMethod, 0);
    }

    private void testReturnedEpochTimeWithMilli(String dateTime, int expectedEpochInSeconds, int yyyy, int mm, int dd, boolean isJavaTimeMethod,
                                                int milleseconds) throws Exception {

        long time = AutomaticTimerBean.parse(dateTime);

        // pass specific date to account for DST
        int offsetSeconds = getOffsetSeconds(yyyy, mm, dd);

        //Epoch time of  minus offset converted to milliseconds
        long expectedTime = ((expectedEpochInSeconds - offsetSeconds) * 1000L) + milleseconds;
        assertEquals("AutomaticTimerBean$DateHelper parser did not return expected time for dateTime " + dateTime +
                     ", expected: " + expectedTime + " parsedTime: " + time, expectedTime, time);
    }

    private void testReturnedEpochTimeSetOffset(String dateTime, int expectedEpochInSeconds, int offsetSeconds, boolean isJavaTimeMethod) throws Exception {
        testReturnedEpochTimeSetOffsetWithMilli(dateTime, expectedEpochInSeconds, offsetSeconds, isJavaTimeMethod, 0);
    }

    private void testReturnedEpochTimeSetOffsetWithMilli(String dateTime, int expectedEpochInSeconds, int offsetSeconds, boolean isJavaTimeMethod,
                                                         int milleseconds) throws Exception {
        long time = AutomaticTimerBean.parse(dateTime);

        //Epoch time of  minus offset converted to milliseconds
        long expectedTime = ((expectedEpochInSeconds - offsetSeconds) * 1000L) + milleseconds;
        assertEquals("AutomaticTimerBean$DateHelper parser did not return expected time for dateTime " + dateTime +
                     ", expected: " + expectedTime + " parsedTime: " + time, expectedTime, time);
    }

    private void testInvalidInput(String dateTime, String inputRepresentation, boolean isJavaTimeMethod) throws Exception {
        try {
            AutomaticTimerBean.parse(dateTime);
            fail("AutomaticTimerBean$DateHelper.parse should have failed for " + inputRepresentation);
        } catch (Exception e) {

        }
    }

    private int getOffsetSeconds(int yyyy, int mm, int dd) throws Exception {
        return LocalDate.of(yyyy, mm, dd).atStartOfDay(ZoneId.systemDefault()).getOffset().getTotalSeconds();
    }

}
