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

import static java.time.temporal.ChronoUnit.MILLENNIA;
import static java.time.temporal.ChronoUnit.YEARS;
import static org.junit.Assert.assertEquals;

import java.time.Duration;

import org.junit.Test;

public class DurationUtilsTest {

    @Test
    public void testClampedNanos() {
        Duration tinyDuration = Duration.ofNanos(100);
        assertEquals(tinyDuration.toNanos(), DurationUtils.asClampedNanos(tinyDuration));

        Duration smallDuration = Duration.ofSeconds(5);
        assertEquals(smallDuration.toNanos(), DurationUtils.asClampedNanos(smallDuration));

        Duration mediumDuration = Duration.ofDays(5000);
        assertEquals(mediumDuration.toNanos(), DurationUtils.asClampedNanos(mediumDuration));

        // Note: Duration.of(500, YEARS) is not permitted because years don't have an exact duration
        // We're happy with an estimated duration, so we're using this alternative construction for our very large durations
        Duration largeDuration = YEARS.getDuration().multipliedBy(500);
        assertEquals(Long.MAX_VALUE, DurationUtils.asClampedNanos(largeDuration));

        Duration hugeDuration = MILLENNIA.getDuration().multipliedBy(7000);
        assertEquals(Long.MAX_VALUE, DurationUtils.asClampedNanos(hugeDuration));

        Duration negativeDuration = Duration.ofSeconds(-5);
        assertEquals(negativeDuration.toNanos(), DurationUtils.asClampedNanos(negativeDuration));

        Duration largeNegativeDuration = YEARS.getDuration().multipliedBy(-500);
        assertEquals(Long.MIN_VALUE, DurationUtils.asClampedNanos(largeNegativeDuration));
    }

}
