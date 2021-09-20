/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.netty.internal.tcp;

/**
 * Utility class for validation the TCP channel and factory configuration
 * values.
 * 
 * Taken from {@link com.ibm.ws.tcpchannel.internal.ValidateUtils}
 */
public class ValidateUtils implements TCPConfigConstants {

    protected static final int VALIDATE_OK = 0;
    protected static final int VALIDATE_ERROR = 1;
    protected static final int VALIDATE_NOT_EQUAL = 2;

    protected static final int LINGER_MIN = -1;
    protected static final int LINGER_MAX = 3600; // in seconds
    protected static final int ACCEPT_THREAD_MIN = 0;
    protected static final int ACCEPT_THREAD_MAX = 1;
    protected static final int NEW_BUFF_SIZE_MIN = 32;
    protected static final int NEW_BUFF_SIZE_MAX = 65536;
    protected static final int LISTEN_BACKLOG_MIN = 0;
    protected static final int LISTEN_BACKLOG_MAX = 512;
    protected static final int KEYS_PER_SELECTOR_MIN = 1;
    protected static final int KEYS_PER_SELECTOR_MAX = 20000;
    protected static final int CHANNEL_SELECTOR_IDLE_TIMEOUT_MIN = 0;
    protected static final int CHANNEL_SELECTOR_IDLE_TIMEOUT_MAX = 3600000; // in
                                                                            // milliseconds
    protected static final int PORT_OPEN_RETRIES_MIN = 0;
    protected static final int PORT_OPEN_RETRIES_MAX = 100000; // 86400 seconds in a day, so round up to 100000

    protected static final int CHANNEL_SELECTOR_WAIT_TO_TERMINATE_MIN = 0;
    protected static final int CHANNEL_SELECTOR_WAIT_TO_TERMINATE_MAX = 3600; // in
                                                                              // seconds
    protected static final int NUMBER_NONBLOCKING_ACCEPT_THREADS_MIN = 1;
    protected static final int NUMBER_NONBLOCKING_ACCEPT_THREADS_MAX = 100;

    protected static final int SELECTOR_WAKEUP_OPTION_MIN = 1;
    protected static final int SELECTOR_WAKEUP_OPTION_MAX = 3;
    protected static final int SELECTOR_WAKEUP_WHEN_NEEDED = 1;
    protected static final int SELECTOR_WAKEUP_NEVER = 2;
    protected static final int SELECTOR_WAKEUP_IF_NO_FORCE_QUEUE = 3;
    protected static final int MIN_SELECTOR_THREADS = 1;
    protected static final int MAX_SELECTOR_THREADS = 1000;
    protected static final int MIN_CONNECTION_THRESHOLD = 1;
    protected static final int MAX_CONNECTION_THRESHOLD = 5000;
    protected static final int INACTIVITY_TIMEOUT_NO_TIMEOUT = 0;
    protected static final int MIN_INBOUND_READ_SELECTORS_TO_START = 1;
    protected static final int MAX_INBOUND_READ_SELECTORS_TO_START = 1000;

    protected static final int KEY_TYPE_INT = 0;
    protected static final int KEY_TYPE_STRING = 1;
    protected static final int KEY_TYPE_BOOLEAN = 2;
    protected static final int KEY_TYPE_ACCESS_LIST = 3;
    protected static final int KEY_OBJECT = 4;

    protected static final int COMM_OPTION_MIN = 0;
    protected static final int COMM_OPTION_MAX = 1;

    protected static final int DUMP_STATS_INTERVAL_MIN = 0;
    protected static final int DUMP_STATS_INTERVAL_MAX = 3600;

    /**
     * Test the input value against the allowed min and max.
     *
     * @param value
     * @param min
     * @param max
     * @return int
     */
    public static int testInt(int value, int min, int max) {
        if ((value < min) || (value > max)) {
            return VALIDATE_ERROR;
        }
        return VALIDATE_OK;
    }

    /**
     * Test the input value against the allowed min and max.
     *
     * @param value
     * @param min
     * @param max
     * @return int
     */
    public static int testLong(long value, long min, long max) {
        if ((value < min) || (value > max)) {
            return VALIDATE_ERROR;
        }
        return VALIDATE_OK;
    }

    /**
     * Test the input string as an int value.
     *
     * @param value
     * @param min
     * @param max
     * @return omt
     */
    public static int testStringAsInt(String value, int min, int max) {

        int number;

        try {
            number = Integer.parseInt(value);
        } catch (NumberFormatException x) {
            return VALIDATE_ERROR;
        }

        if ((number < min) || (number > max)) {
            return VALIDATE_ERROR;
        }
        return VALIDATE_OK;
    }

    /**
     * Test the input port value for the allowed range.
     *
     * @param value
     * @return int
     */
    public static int testPort(int value) {
        return testInt(value, PORT_MIN, PORT_MAX);
    }

    /**
     * Test the TCP max-connections value against the allowed range.
     *
     * @param value
     * @return int
     */
    public static int testMaxConnections(int value) {
        return testInt(value, MAX_CONNECTIONS_MIN, MAX_CONNECTIONS_MAX);
    }

    /**
     * Test the TCP max-connections value against the allowed range.
     *
     * @param value
     * @return int
     */
    public static int testMaxConnections(String value) {
        return testStringAsInt(value, MAX_CONNECTIONS_MIN, MAX_CONNECTIONS_MAX);
    }

    /**
     * Test the TCP solinger value.
     *
     * @param value
     * @return int
     */
    public static int testLinger(int value) {
        return testInt(value, LINGER_MIN, LINGER_MAX);
    }

    /**
     * Test the TCP solinger value.
     *
     * @param value
     * @return int
     */
    public static int testLinger(String value) {
        return testStringAsInt(value, LINGER_MIN, LINGER_MAX);
    }

    /**
     * Test the configuration flag that controls whether the TCP channel
     * is using a dedicated accept selector or not.
     *
     * @param value
     * @return int
     */
    public static int testAcceptThread(int value) {
        return testInt(value, ACCEPT_THREAD_MIN, ACCEPT_THREAD_MAX);
    }

    /**
     * Test the configuration flag that controls whether the TCP channel
     * is using a dedicated accept selector or not.
     *
     * @param value
     * @return int
     */
    public static int testAcceptThread(String value) {
        return testStringAsInt(value, ACCEPT_THREAD_MIN, ACCEPT_THREAD_MAX);
    }

    /**
     * Test the buffer size used on the initial read for a new incoming
     * connection.
     *
     * @param value
     * @return int
     */
    public static int testNewBuffSize(int value) {

        return testInt(value, NEW_BUFF_SIZE_MIN, NEW_BUFF_SIZE_MAX);
    }

    /**
     * Test the number of retries to successfully open a port before signaling that the port can not be open
     *
     * @param value
     * @return int
     */
    public static int testPortOpenRetries(int value) {

        return testInt(value, PORT_OPEN_RETRIES_MIN, PORT_OPEN_RETRIES_MAX);
    }

    /**
     * Test the buffer size used on the initial read for a new incoming
     * connection.
     *
     * @param value
     * @return int
     */
    public static int testNewBuffSize(String value) {
        return testStringAsInt(value, NEW_BUFF_SIZE_MIN, NEW_BUFF_SIZE_MAX);
    }

    /**
     * Test the size of the listen backlog to configure for this channel.
     *
     * @param value
     * @return int
     */
    public static int testListenBacklog(int value) {
        return testInt(value, LISTEN_BACKLOG_MIN, LISTEN_BACKLOG_MAX);
    }

    /**
     * Test the size of the listen backlog to configure for this channel.
     *
     * @param value
     * @return int
     */
    public static int testListenBacklog(String value) {
        return testStringAsInt(value, LISTEN_BACKLOG_MIN, LISTEN_BACKLOG_MAX);
    }

    /**
     * Test the size of the receive buffer to configure at the socket layer.
     *
     * @param value
     * @return int
     */
    public static int testReceiveBufferSize(int value) {
        return testInt(value, RECEIVE_BUFFER_SIZE_MIN, RECEIVE_BUFFER_SIZE_MAX);
    }

    /**
     * Test the size of the receive buffer to configure at the socket layer.
     *
     * @param value
     * @return int
     */
    public static int testReceiveBufferSize(String value) {
        return testStringAsInt(value, RECEIVE_BUFFER_SIZE_MIN, RECEIVE_BUFFER_SIZE_MAX);
    }

    /**
     * Test the size of the send buffer to configure at the socket layer.
     *
     * @param value
     * @return int
     */
    public static int testSendBufferSize(int value) {
        return testInt(value, SEND_BUFFER_SIZE_MIN, SEND_BUFFER_SIZE_MAX);
    }

    /**
     * Test the size of the send buffer to configure at the socket layer.
     *
     * @param value
     * @return int
     */
    public static int testSendBufferSize(String value) {
        return testStringAsInt(value, SEND_BUFFER_SIZE_MIN, SEND_BUFFER_SIZE_MAX);
    }

    /**
     * Test the inactivity timeout to use for the initial read.
     *
     * @param value
     * @return int
     */
    public static int testInactivityTimeout(long value) {
        return testLong(value, INACTIVITY_TIMEOUT_MIN, INACTIVITY_TIMEOUT_MAX);
    }

    /**
     * Test the configuration used for the allowed number of keys per selector.
     *
     * @param value
     * @return int
     */
    public static int testKeysPerSelector(int value) {
        return testInt(value, KEYS_PER_SELECTOR_MIN, KEYS_PER_SELECTOR_MAX);
    }

    /**
     * Test the configuration used for the allowed number of keys per selector.
     *
     * @param value
     * @return int
     */
    public static int testKeysPerSelector(String value) {
        return testStringAsInt(value, KEYS_PER_SELECTOR_MIN, KEYS_PER_SELECTOR_MAX);
    }

    /**
     * Test the default timeout used during the selector.select() calls.
     *
     * @param value
     * @return int
     */
    public static int testChannelSelectorIdleTimeout(long value) {
        return testLong(value, CHANNEL_SELECTOR_IDLE_TIMEOUT_MIN, CHANNEL_SELECTOR_IDLE_TIMEOUT_MAX);
    }

    /**
     * Test the default timeout used during the selector.select() calls.
     *
     * @param value
     * @return int
     */
    public static int testChannelSelectorIdleTimeout(String value) {
        return testStringAsInt(value, CHANNEL_SELECTOR_IDLE_TIMEOUT_MIN, CHANNEL_SELECTOR_IDLE_TIMEOUT_MAX);
    }

    /**
     * Test the selector configuration for the last timeout.
     *
     * @param value
     * @return int
     */
    public static int testChannelSelectorWaitToTerminate(long value) {
        return testLong(value, CHANNEL_SELECTOR_WAIT_TO_TERMINATE_MIN, CHANNEL_SELECTOR_WAIT_TO_TERMINATE_MAX);
    }

    /**
     * Test the selector configuration for the last timeout.
     *
     * @param value
     * @return int
     */
    public static int testChannelSelectorWaitToTerminate(String value) {
        return testStringAsInt(value, CHANNEL_SELECTOR_WAIT_TO_TERMINATE_MIN, CHANNEL_SELECTOR_WAIT_TO_TERMINATE_MAX);
    }

    /**
     * Test the selector configuration option for waking up with new work.
     *
     * @param value
     * @return int
     */
    public static int testChannelSelectorWakeupOption(int value) {
        return testInt(value, SELECTOR_WAKEUP_OPTION_MIN, SELECTOR_WAKEUP_OPTION_MAX);
    }

    /**
     * Test the selector configuration option for waking up with new work.
     *
     * @param value
     * @return int
     */
    public static int testChannelSelectorWakeupOption(String value) {
        return testStringAsInt(value, SELECTOR_WAKEUP_OPTION_MIN, SELECTOR_WAKEUP_OPTION_MAX);
    }

    /**
     * Test the configuration for the number of selector threads to create
     * for the sync manager.
     *
     * @param value
     * @return int
     */
    public static int testMaxSelectorThreads(int value) {
        return testInt(value, MIN_SELECTOR_THREADS, MAX_SELECTOR_THREADS);
    }

    /**
     * Test the configuration for the number of selector threads to create
     * for the sync manager.
     *
     * @param value
     * @return int
     */
    public static int testMaxSelectorThreads(String value) {
        return testStringAsInt(value, MIN_SELECTOR_THREADS, MAX_SELECTOR_THREADS);
    }

    /**
     * Test the sync manager configuration for the number of keys to allow
     * per selector.
     *
     * @param value
     * @return int
     */
    public static int testConnectionThreshold(int value) {
        return testInt(value, MIN_CONNECTION_THRESHOLD, MAX_CONNECTION_THRESHOLD);
    }

    /**
     * Test the sync manager configuration for the number of keys to allow
     * per selector.
     *
     * @param value
     * @return int
     */
    public static int testConnectionThreshold(String value) {
        return testStringAsInt(value, MIN_CONNECTION_THRESHOLD, MAX_CONNECTION_THRESHOLD);
    }

    /**
     * Test the configuration for the number of read selectors to start.
     *
     * @param value
     * @return int
     */
    public static int testInboundReadSelectorsToStart(int value) {
        return testInt(value, MIN_INBOUND_READ_SELECTORS_TO_START, MAX_INBOUND_READ_SELECTORS_TO_START);
    }

    /**
     * Test the configuration for the number of read selectors to start.
     *
     * @param value
     * @return int
     */
    public static int testInboundReadSelectorsToStart(String value) {
        return testStringAsInt(value, MIN_INBOUND_READ_SELECTORS_TO_START, MAX_INBOUND_READ_SELECTORS_TO_START);
    }

    /**
     * Test the internal RAS audit level.
     *
     * @param value
     * @return int
     */
    public static int testAuditLevel(String value) {

        if (value.equalsIgnoreCase("INFO")) {
            return ValidateUtils.VALIDATE_OK;
        } else if (value.equalsIgnoreCase("FINE")) {
            return ValidateUtils.VALIDATE_OK;
        } else {
            return ValidateUtils.VALIDATE_ERROR;
        }
    }

    /**
     * Test the NIO vs AIO comm option.
     *
     * @param value
     * @return int
     */
    public static int testCommOption(int value) {
        return testInt(value, COMM_OPTION_MIN, COMM_OPTION_MAX);
    }

    /**
     * Test the NIO vs AIO comm option.
     *
     * @param value
     * @return int
     */
    public static int testCommOption(String value) {
        return testStringAsInt(value, COMM_OPTION_MIN, COMM_OPTION_MAX);
    }

    /**
     * Test the debug stats interval configuration.
     *
     * @param value
     * @return int
     */
    public static int testDumpStatsInterval(int value) {
        return testInt(value, DUMP_STATS_INTERVAL_MIN, DUMP_STATS_INTERVAL_MAX);
    }

    /**
     * Test the debug stats interval configuration.
     *
     * @param value
     * @return int
     */
    public static int testDumpStatsInterval(String value) {
        return testStringAsInt(value, DUMP_STATS_INTERVAL_MIN, DUMP_STATS_INTERVAL_MAX);
    }

    /**
     * Test the IP filter configuration values.
     *
     * @param value
     * @return int
     */
    public static int testIsStringIPAddressesValid(String[] value) {
        FilterList f = new FilterList();
        try {
            if (value != null) {
                f.buildData(value, true);
            }
        } catch (NumberFormatException x) {
            return VALIDATE_ERROR;
        }

        return VALIDATE_OK;
    }

}
