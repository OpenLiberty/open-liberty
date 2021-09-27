/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.ejb;

import java.util.Collection;

public interface AnnotationTxLocal {
    public static enum TstName {
        TEST_CREATE_AND_CANCEL_IN_SAME_METHOD,
        TEST_CREATE_TWO_TIMERS_IN_SAME_METHOD,
        TEST_CREATE_AND_CANCEL_IN_SAME_METHOD_ROLLBACK,
        TEST_CREATE_TWO_TIMERS_IN_SAME_METHOD_ROLLBACK,
        TEST_CREATE_TIMER,
        TEST_CANCEL_TIMER,
        TEST_EXPIRATION_IN_PAST,
        TEST_NEGATIVE_DURATION,
        TEST_OVERFLOW_DURATION,
        TEST_OVERFLOW_EXPIRATION,
        TEST_OVERFLOW_INTERVAL,
        TEST_INVALID_EXPIRATION,
        TEST_GET_HANDLE,
        TEST_CREATE_EXPIRED_TIMER,
        TEST_CANCEL_EXPIRED_TIMER,
        TEST_INTERVAL_TIMER_DURATION,
        TEST_INTERVAL_TIMER_DATE,
        TEST_TIMER_API,
        TEST_TIMER_SERVICE_API,
        TEST_CANCEL_ROLLBACK_1,
        TEST_CANCEL_ROLLBACK_2,
        TEST_CREATE_CALENDAR_AND_FIND_IN_SAME_METHOD
    };

    public final static String TIMEOUT_METHOD = "myTimeout";
    public final static String DEFAULT_INFO = "default_info";
    public final static String HANDLE_INFO = "handle_info";
    public final static String EXPIRED_INFO = "expired_info";
    public final static String INTERVAL_INFO = "interval_info";
    public final static String CANCEL_ROLLBACK_INFO = "cancel_rollback_info";

    public final static long DURATION = 2000;
    public final static long INTERVAL = 2000;
    public final static long BUFFER = 1000;
    public final static long MAX_WAIT_TIME = 3 * 60 * 1000;

    public void executeTest(TstName tstName);

    public void executeTestNoTx(TstName tstName);

    public void waitForTimer(long maxWaitTime);

    public void clearAllTimers();

    public Collection<String> getInfoOfAllTimers();
}
