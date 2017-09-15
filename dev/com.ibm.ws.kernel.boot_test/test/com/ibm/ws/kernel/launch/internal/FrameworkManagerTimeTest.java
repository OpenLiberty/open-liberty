/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.launch.internal;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

/**
 *
 */
public class FrameworkManagerTimeTest {
    static SharedOutputManager outputMgr;
    static Locale saveLocale;
    static FrameworkManager frameworkManager;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
        saveLocale = Locale.getDefault();
        frameworkManager = new FrameworkManager();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
        Locale.setDefault(saveLocale);

    }

    @Test
    public void testgetElapsedTimeAsStringFromFrameworkManager_English() {
        @SuppressWarnings("unused")
        final String m = "testgetElapsedTimeAsStringFromFrameworkManager_English";
        String loc = "en"; //example: "pl" or "en"
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(new Locale(loc));
        String days = " days, ";
        String hours = " hours, ";
        String min = " minutes, ";
        String sec = " seconds";
        String decSep = decimalFormatSymbols.getDecimalSeparator() + ""; //Decimal Separator in English is '.'
        String grpSep = decimalFormatSymbols.getGroupingSeparator() + ""; //Grouping Separator in English is ','

        testgetElapsedTimeAsStringFromFrameworkManager(loc, days, hours, min, sec, decSep, grpSep);

    }

    @Test
    public void testgetElapsedTimeAsStringFromFrameworkManager_Polish() {
        @SuppressWarnings("unused")
        final String m = "testgetElapsedTimeAsStringFromFrameworkManager_Polish";
        String loc = "pl"; //example: "pl" or "en"
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(new Locale(loc));
        String days = " days, "; // dn.
        String hours = " hours, ";//  godz.
        String min = " minutes, ";// min.
        String sec = " seconds";// sek.
        String decSep = decimalFormatSymbols.getDecimalSeparator() + ""; //Decimal Separator in Polish is ','
        String grpSep = decimalFormatSymbols.getGroupingSeparator() + ""; //Grouping Separator

        testgetElapsedTimeAsStringFromFrameworkManager(loc, days, hours, min, sec, decSep, grpSep);

    }

    private void testgetElapsedTimeAsStringFromFrameworkManager(String locale, String days, String hours, String min, String sec, String decSep, String grpSep) {
        @SuppressWarnings("unused")
        final String m = "testgetElapsedTimeAsStringFromFrameworkManager";
        Locale.setDefault(new Locale(locale)); //example: "pl" or "en"
        long elapsedTimeVerify;
        String[] processedTime = new String[48];
        String[] expectedResults = new String[48];

        //Testing the first branch of the 'if' statement
        //First Group- testing the mixed units
        elapsedTimeVerify = 2007208124L;//The milliseconds of 23 days, 5 hours, 33 minutes, 28.124 seconds
        expectedResults[0] = "23" + days + "5" + hours + "33" + min + "28" + decSep + "124" + sec;
        processedTime[0] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);
        elapsedTimeVerify = 1989208124L;//The milliseconds of 23 days, 33 minutes, 28.124 seconds
        expectedResults[1] = "23" + days + "33" + min + "28" + decSep + "124" + sec;
        processedTime[1] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);
        elapsedTimeVerify = 2005228124L;//The milliseconds of 23 days, 5 hours, 28.124 seconds
        expectedResults[2] = "23" + days + "5" + hours + "28" + decSep + "124" + sec;
        processedTime[2] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);
        elapsedTimeVerify = 1987228124L;//The milliseconds of 23 days, 28.124 seconds
        expectedResults[3] = "23" + days + "28" + decSep + "124" + sec;
        processedTime[3] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);
        elapsedTimeVerify = 20008124L; //The milliseconds of 5 hours, 33 minutes, 28.124 seconds
        expectedResults[4] = "5" + hours + "33" + min + "28" + decSep + "124" + sec;
        processedTime[4] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);
        elapsedTimeVerify = 18028124L; //The milliseconds of 5 hours, 28.124 seconds
        expectedResults[5] = "5" + hours + "28" + decSep + "124" + sec;
        processedTime[5] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);
        elapsedTimeVerify = 2008124L; //The milliseconds of 33 minutes, 28.124 seconds
        expectedResults[6] = "33" + min + "28" + decSep + "124" + sec;
        processedTime[6] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);
        elapsedTimeVerify = 28124L; //The milliseconds of 28.124 seconds
        expectedResults[7] = "28" + decSep + "124" + sec;
        processedTime[7] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);
        elapsedTimeVerify = 28120L; //The milliseconds of 28.12 seconds
        expectedResults[8] = "28" + decSep + "12" + sec;
        processedTime[8] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);
        elapsedTimeVerify = 28100L; //The milliseconds of 28.1 seconds
        expectedResults[9] = "28" + decSep + "1" + sec;
        processedTime[9] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);
        elapsedTimeVerify = 28000L; //The milliseconds of 28 seconds
        expectedResults[10] = "28" + sec;
        processedTime[10] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);
        elapsedTimeVerify = 2000L; //The milliseconds of 2 seconds
        expectedResults[11] = "2" + sec;
        processedTime[11] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);
        elapsedTimeVerify = 200L; //The milliseconds of 0.2 seconds
        expectedResults[12] = "0" + decSep + "2" + sec;
        processedTime[12] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);
        elapsedTimeVerify = 20L; //The milliseconds of 0.02 seconds
        expectedResults[13] = "0" + decSep + "02" + sec;
        processedTime[13] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);
        elapsedTimeVerify = 2L; //The milliseconds of 0.002 seconds
        expectedResults[14] = "0" + decSep + "002" + sec;
        processedTime[14] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);
        elapsedTimeVerify = 1009L; //The milliseconds of 1.009 seconds
        expectedResults[15] = "1" + decSep + "009" + sec;
        processedTime[15] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);

        //Second Group - testing with zero seconds
        elapsedTimeVerify = 0L; //The milliseconds of 0 seconds
        expectedResults[16] = "0" + sec;
        processedTime[16] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);
        elapsedTimeVerify = 60000L; //The milliseconds of 1 minutes, 0 seconds
        expectedResults[17] = "1" + min + "0" + sec;
        processedTime[17] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);
        elapsedTimeVerify = 1987200000L;//The milliseconds of 23 days, 0 seconds
        expectedResults[18] = "23" + days + "0" + sec;
        processedTime[18] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);
        elapsedTimeVerify = 18000000L; //The milliseconds of 5 hours, 0 seconds
        expectedResults[19] = "5" + hours + "0" + sec;
        processedTime[19] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);
        elapsedTimeVerify = 1980000L; //The milliseconds of 33 minutes, 0 seconds
        expectedResults[20] = "33" + min + "0" + sec;
        processedTime[20] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);

        //Third Group - testing with maximum value of Long
        elapsedTimeVerify = Long.MAX_VALUE; //The milliseconds of 106,751,991,167 days, 7 hours, 12 minutes 55.635 seconds
        //(Seconds rounded from 55.634688) However due in difference in calculation accuracy when the Remainder '%' is used on Long, the
        // Value will be 55.807
        //Long.MAX_VALUE = 9223372036854775807
        expectedResults[21] = "106" + grpSep + "751" + grpSep + "991" + grpSep + "167" + days + "7" + hours + "12" + min + "55" + decSep + "807" + sec;
        processedTime[21] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);

        //Third Group - Negative testing
        elapsedTimeVerify = -1L; // No Time in negative value - Currently return 0 seconds
        expectedResults[22] = "0" + sec;
        processedTime[22] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);
        elapsedTimeVerify = Long.MIN_VALUE; // No Time in negative value - Currently return 0 seconds
        // Long.MIN_VALUE = -9223372036854775808
        expectedResults[23] = "0" + sec;
        processedTime[23] = frameworkManager.getElapsedTime(true, elapsedTimeVerify);

        //Testing the second branch of the 'if' statement, the 'else'
        //First Group
        elapsedTimeVerify = 1009L; //The milliseconds of 1.009 seconds
        expectedResults[24] = "1" + decSep + "009" + sec;
        processedTime[24] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = 0L; //The milliseconds of 0 seconds
        expectedResults[25] = "0" + sec;
        processedTime[25] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = 2L; //The milliseconds of 0.002 seconds
        expectedResults[26] = "0" + decSep + "002" + sec;
        processedTime[26] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = 20L; //The milliseconds of 0.02 seconds
        expectedResults[27] = "0" + decSep + "02" + sec;
        processedTime[27] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = 200L; //The milliseconds of 0.2 seconds
        expectedResults[28] = "0" + decSep + "2" + sec;
        processedTime[28] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = 2000L; //The milliseconds of 2 seconds
        expectedResults[29] = "2" + sec;
        processedTime[29] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = 20000L; //The milliseconds of 20 seconds
        expectedResults[30] = "20" + sec;
        processedTime[30] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = 200000L; //The milliseconds of 200 seconds
        expectedResults[31] = "200" + sec;
        processedTime[31] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = 2000000L; //The milliseconds of 2,000 seconds
        expectedResults[32] = "2" + grpSep + "000" + sec;
        processedTime[32] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = 20000000L; //The milliseconds of 20,000 seconds
        expectedResults[33] = "20" + grpSep + "000" + sec;
        processedTime[33] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = 200000000L; //The milliseconds of 200,000 seconds
        expectedResults[34] = "200" + grpSep + "000" + sec;
        processedTime[34] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = 2000000000L; //The milliseconds of 2,000,000 seconds
        expectedResults[35] = "2" + grpSep + "000" + grpSep + "000" + sec;
        processedTime[35] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = 20000000000L; //The milliseconds of 20,000,000 seconds
        expectedResults[36] = "20" + grpSep + "000" + grpSep + "000" + sec;
        processedTime[36] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = 200000000000L; //The milliseconds of 200,000,000 seconds
        expectedResults[37] = "200" + grpSep + "000" + grpSep + "000" + sec;
        processedTime[37] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = 2000000000000L; //The milliseconds of 2,000,000,000 seconds
        expectedResults[38] = "2" + grpSep + "000" + grpSep + "000" + grpSep + "000" + sec;
        processedTime[38] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = 20000000000000L; //The milliseconds of 20,000,000,000 seconds
        expectedResults[39] = "20" + grpSep + "000" + grpSep + "000" + grpSep + "000" + sec;
        processedTime[39] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = 200000000000000L; //The milliseconds of 200,000,000,000 seconds
        expectedResults[40] = "200" + grpSep + "000" + grpSep + "000" + grpSep + "000" + sec;
        processedTime[40] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = 2000000000000000L; //The milliseconds of 2,000,000,000,000 seconds
        expectedResults[41] = "2" + grpSep + "000" + grpSep + "000" + grpSep + "000" + grpSep + "000" + sec;
        processedTime[41] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = 20000000000000000L; //The milliseconds of 20,000,000,000,000 seconds
        expectedResults[42] = "20" + grpSep + "000" + grpSep + "000" + grpSep + "000" + grpSep + "000" + sec;
        processedTime[42] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = 200000000000000000L; //The milliseconds of 200,000,000,000,000 seconds
        expectedResults[43] = "200" + grpSep + "000" + grpSep + "000" + grpSep + "000" + grpSep + "000" + sec;
        processedTime[43] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = 2000000000000000000L; //The milliseconds of 2,000,000,000,000,000 seconds
        expectedResults[44] = "2" + grpSep + "000" + grpSep + "000" + grpSep + "000" + grpSep + "000" + grpSep + "000" + sec;
        processedTime[44] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        //Second Group - testing with maximum value of Long
        elapsedTimeVerify = Long.MAX_VALUE; //The milliseconds of 9,223,372,036,854,775.807 seconds
        // Long.MAX_VALUE = 9223372036854775807
        // The double will convert these seconds to 9,223,372,036,854,776 seconds
        expectedResults[45] = "9" + grpSep + "223" + grpSep + "372" + grpSep + "036" + grpSep + "854" + grpSep + "776" + sec;
        processedTime[45] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        //Third Group - Negative testing
        elapsedTimeVerify = -1L; // No Time in negative value - Currently return 0 seconds
        expectedResults[46] = "0" + sec;
        processedTime[46] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);
        elapsedTimeVerify = Long.MIN_VALUE; // No Time in negative value - Currently return 0 seconds
        // Long.MIN_VALUE = -9223372036854775808
        expectedResults[47] = "0" + sec;
        processedTime[47] = frameworkManager.getElapsedTime(false, elapsedTimeVerify);

        // Assisting the results
        String whiteSpacePattern = "\\s";
        //int i = 47;
        for (int i = 0; i < expectedResults.length; i++) {
            boolean result = expectedResults[i].replaceAll(whiteSpacePattern, "").
                            equalsIgnoreCase(processedTime[i].replaceAll(whiteSpacePattern, ""));
            String errMsg = "Elapsed Time is not formated correctly for the " + Locale.getDefault() + " language.\n"
                            + "The expected result at test # [" + i + "] is: " + expectedResults[i] + "\n"
                            + "The actual result is: " + processedTime[i];
            Assert.assertEquals(errMsg, true, result);
        }
    }
}
