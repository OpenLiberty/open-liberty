/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.overlay.util.internal;

import com.ibm.websphere.ras.annotation.Trivial;

public class TimeUtil {

// TODO: Move this into a unit test.
//	    public static void main(String[] args) {
//	        long[] testValues = {
//	            0L, 1L,
//	            998L, 999L, 1000L, 1001L,
//	            999998L, 999999L, 1000000L, 1000001L,
//	            999999998L, 999999999L, 1000000000L, 1000000001,
//	            10000000000L
//	        };
//
//	        for ( long testValue : testValues ) {
//	            System.out.println("Test [ " + Long.toString(testValue) + " ]");
//	            System.out.println("     [ " + toAbsSec(testValue) + " ]");
//	        }
//	    }
    

    private static final String PAD = "0000000000";
    private static final int PAD_LEN = 10;

    @Trivial
    public static String toCount(int count) {
        return toCount(count, 6);
    }

    @Trivial
    public static String toCount(int count, int pad) {
        String digits = Integer.toString(count);
        int numDigits = digits.length();
        if ( numDigits >= pad ) {
            return digits;
        }

        int missing = pad - numDigits;
        if ( missing > PAD_LEN ) {
            missing = PAD_LEN;
        }

        return PAD.substring(0,  missing) + digits;
    }

    //

    public static final long NANO_IN_ONE = 1000 * 1000 * 1000;
    public static final long NANO_IN_MILLI = 1000 * 1000;
    public static final long MILLI_IN_ONE = 1000;

    public static final int NANO_IN_ONE_DIGITS = 9;
    public static final int NANO_IN_MILLI_DIGITS = 6;
    public static final int MILLI_IN_ONE_DIGITS = 3;

    public static final int PAD_LEFT = 6;
    public static final int PAD_RIGHT = 6;

    //

    /**
     * Display the difference between two nano-seconds values as a
     * decimal seconds value.
     *
     * Display six places after the decimal point, and left
     * the integer value with '0' to a minimum of six characters.
     *
     * @param baseNS The value to subtract from the second parameter.
     * @param actualNS The from which to subtract the QUICK parameter.
     *
     * @return The difference as a seconds value.
     */
    @Trivial
    public static String toRelSec(long baseNS, long actualNS) {
        return toAbsSec(actualNS - baseNS, PAD_LEFT);
    }

    /**
     * Display the difference between two nano-seconds values as a
     * decimal seconds value.
     *
     * Display three places after the decimal point, and left
     * the integer value with '0' to a minimum of the specified
     * number of characters.
     *
     * @param baseNS The value to subtract from the second parameter.
     * @param actualNS The from which to subtract the first parameter.
     * @param pad The number of '0' character to pad to the integer
     *    part of the value.
     *
     * @return The value as a seconds value.
     */
    @Trivial
    public static String toRelSec(long baseNS, long actualNS, int pad) {
        return toAbsSec(actualNS - baseNS, pad);
    }

    /**
     * Display a nano-seconds value as a decimal seconds value.
     *
     * Display three places after the decimal point, and left
     * the integer value with '0' to a minimum of six characters.
     *
     * Negative values are not handled.
     *
     * @param durationNS The duration in nano-seconds to display
     *    as a seconds value.
     * @param pad The number of '0' character to pad to the integer
     *    part of the value.
     * @return The value as a seconds value.
     */
    @Trivial
    public static String toAbsSec(long durationNS) {
        return toAbsSec(durationNS, PAD_LEFT);
    }

    /**
     * Display a nano-seconds value as a decimal seconds value.
     *
     * Display six places after the decimal point, and left
     * the integer value with '0' to a minimum of the specified
     * number of pad characters.
     *
     * Negative values are handled by prepending '-' to the left of the
     * display value of the positive nano-seconds value.  This will result
     * in one extra character in the display output.
     *
     * @param nano The duration in nano-seconds to display
     *    as a seconds value.
     * @param padLeft The number of '0' character to pad to the integer
     *    part of the value.
     * @return The value as a seconds value.
     */
    @Trivial
    public static String toAbsSec(long nano, int padLeft) {
        if ( nano < 0 ) {
            return "-" + toAbsSec(-1 * nano, padLeft);
        } else if ( nano == 0 ) {
            return PAD.substring(0, padLeft) + "." + PAD.substring(PAD_RIGHT);
        }

        String nanoText = Long.toString(nano);
        int nanoDigits = nanoText.length();

        if ( nanoDigits > NANO_IN_ONE_DIGITS ) { // greater than 999,999,999
            int secDigits = nanoDigits - NANO_IN_ONE_DIGITS;
            if ( secDigits >= padLeft ) {
                return nanoText.substring(0, secDigits) +
                       "." +
                       nanoText.substring(secDigits, secDigits + PAD_RIGHT);
            } else {
                int padDigits = padLeft - secDigits;
                if ( padDigits > PAD_LEN ) {
                    padDigits = PAD_LEN;
                }
                return PAD.substring(0, padDigits) + nanoText.substring(0, secDigits) +
                       "." +
                       nanoText.substring(secDigits, secDigits + PAD_RIGHT);
            }

        } else if ( nanoDigits > (NANO_IN_ONE_DIGITS - PAD_RIGHT) ) { // less than 1,000,000,000 and greater than 999
            if ( padLeft > PAD_LEN ) {
                padLeft = PAD_LEN;
            }
            int missingDigits = NANO_IN_ONE_DIGITS - nanoDigits;
            return PAD.substring(0, padLeft) +
                   "." +
                   PAD.substring(0, missingDigits) + nanoText.substring(0, PAD_RIGHT - missingDigits);

        } else { // less than 1,000, but greater than 0.
            if ( padLeft > PAD_LEN ) {
                padLeft = PAD_LEN;
            }
            return PAD.substring(0, padLeft) +
                   "." +
                   PAD.substring(PAD_RIGHT - 1) + "*";
        }
    }
}
