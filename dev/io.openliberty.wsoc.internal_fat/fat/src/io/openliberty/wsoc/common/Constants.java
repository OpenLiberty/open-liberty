/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.common;

public class Constants {

    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String ON_OPEN_ID = "OnOpenId";
    public static final String USER_PROP_TEST_NUMBER = "TestNumber";
    public static final String LATCH_DOWN = "LatchDown";
    public static final String CLIENT_NUMBER = "Client Number:";

    public static final long TEST_MAX_MSG_SIZE = 32767;
    public static final long UNLIMITED_MAX_MSG_SIZE = -1;
    public static final long UNDEFINED_MAX_MSG_SIZE = -2;

    public static final int NUM_CLIENTS = 25;

    public static long longFFDCWait = 1000;

    public enum LatchTypes {
        OPEN, CONNECT, MESSAGE, ERROR, CLOSE
    }

    // short timeout should be used if the test case will have to wait and timeout to verify it is working correctly.
    public enum TimeoutType {
        SHORT_TIMEOUT,
        CONNECT_TIMEOUT,
        DEFAULT_TIMEOUT,
        LONG_TIMEOUT,
        EXTRUN_SHORT_TIMEOUT
    }

    //Liberty server direct
    // junit <==> Liberty
    static int[] directTimeouts = {
                                    15000, //SHORT
                                    15000, //CONNECT
                                    60000, //DEFAULT
                                    80000, //LONG
                                    120000 //EXTRUN_SHORT
    };

    public static int getTimeout(TimeoutType tt) {
        int[] timeouts;
        timeouts = directTimeouts;

        return timeouts[tt.ordinal()];
    }

    /**
     * @return the defaultTimeout
     */
    public static int getDefaultTimeout() {
        return getTimeout(TimeoutType.DEFAULT_TIMEOUT);

    }

    /**
     * @return the longTimeout
     */
    public static int getLongTimeout() {
        return getTimeout(TimeoutType.LONG_TIMEOUT);
    }

    /**
     * @return the shortTimeout
     */
    public static int getShortTimeout() {
        return getTimeout(TimeoutType.SHORT_TIMEOUT);
    }

    /**
     * @return the connectTimeout
     */
    public static int getConnectTimeout() {
        return getTimeout(TimeoutType.CONNECT_TIMEOUT);
    }

    /**
     * @return the extrunShortTimeout
     */
    public static int getExtrunShortTimeout() {
        return getTimeout(TimeoutType.EXTRUN_SHORT_TIMEOUT);
    }

    /**
     * @return the getClientsCount
     */
    public static int getClientsCount() {
        return NUM_CLIENTS;
    }

    public static String PING_PONG_FROM_SERVER_MSG = "Ping form server saw pong from client";

    public static String ENCODER_GENERIC_SUCCESS = "Message from EncoderTextStreamGeneric encoder - SUCCESS";
}
