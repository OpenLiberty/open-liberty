/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance.metrics.app;

/**
 *
 */
public class TestConstants {
    //a basic unit of time, in ms, so that test timeouts and thresholds can be scaled together
    public static final long TEST_TIME_UNIT = 1000;

    //A tiny increment of time, used to ensure this test runs correctly on the test servers.
    public static final long TEST_TWEAK_TIME_UNIT = TEST_TIME_UNIT / 10;

    //This is the ammount of time, in ms, that we allow for a Future to be returned. Normally it should come back almost instantly
    //but we are finding that it can take a bit longer when running in the build.
    // 13/07/17 - Initially it was 1000, increasing to 2000.
    public static final long FUTURE_THRESHOLD = 2 * TEST_TIME_UNIT;

    //the bulkhead timeout value.
    public static final long TIMEOUT = 2 * TEST_TIME_UNIT;

    //the amount of time that async tasks will sleep if they are simulating 'work'
    public static final long WORK_TIME = 5 * TEST_TIME_UNIT;

    public static final long EXECUTION_THRESHOLD = WORK_TIME + FUTURE_THRESHOLD;

    //a test timeout used to prevent a test hang when things go wrong
    public static final long TEST_TIMEOUT = 10 * TEST_TIME_UNIT;
}
