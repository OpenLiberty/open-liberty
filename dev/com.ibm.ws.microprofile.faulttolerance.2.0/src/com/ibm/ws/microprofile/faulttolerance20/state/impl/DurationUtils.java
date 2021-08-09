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
package com.ibm.ws.microprofile.faulttolerance20.state.impl;

import java.time.Duration;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Utilities for working with {@link Duration}
 */
public class DurationUtils {

    // No public constructor, static utility methods only
    private DurationUtils() {}

    /**
     * Convert a duration to nanoseconds, clamping between MIN_VALUE and MAX_LONG
     * <p>
     * Needed in case a user specifies something silly like delay = 5 MILLENNIA
     * <p>
     * protected only to allow unit testing
     *
     * @param duration the duration to convert
     * @return duration as nanoseconds, clamped if required
     */
    @FFDCIgnore(ArithmeticException.class)
    public static long asClampedNanos(Duration duration) {
        try {
            return duration.toNanos();
        } catch (ArithmeticException e) {
            // Treat long overflow as an exceptional case
            if (duration.isNegative()) {
                return Long.MIN_VALUE;
            } else {
                return Long.MAX_VALUE;
            }
        }
    }

}
