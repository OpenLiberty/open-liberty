/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tcpchannel.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.ChannelFrameworkConstants;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.tcpchannel.TCPConfigConstants;

/**
 * Configuration object for an individual TCP channel instance.
 */
public class TCPChannelConfiguration implements TCPConfigConstants, FFDCSelfIntrospectable {

    protected static final String NEW_BUFF_SIZE = "newConnectionBufferSize";
    protected static final String LINGER = "soLinger";
    protected static final String NO_DELAY = "tcpNoDelay";
    protected static final String REUSE_ADDR = "soReuseAddr";
    protected static final String KEEP_ALIVE = "keepAlive";
    protected static final String BACKLOG = "listenBacklog";
    protected static final String DIRECT_BUFFS = "allocateBuffersDirect";
    protected static final String ACCEPT_THREAD = "acceptThread";
    protected static final String WAIT_TO_ACCEPT = "waitToAccept";
    protected static final String COMM_OPTION = "commOption";
    protected static final String DUMP_STATS_INTERVAL = "dumpStatsInterval";
    protected static final String GROUPNAME = "workGroup";

    // internal custom properties
    protected static final String ENDPOINT_NAME = "endPointName";
    protected static final String CHAIN_TYPE_KEY = "chainType";
    protected static final String ZAIO_RESOLVE_FOREIGN_HOSTNAMES_KEY = "zaioResolveForeignHostnames";
    protected static final String ZAIO_FREE_INITIAL_BUFFER_KEY = "zaioFreeInitialBuffers";

    private static final TraceComponent tc = Tr.register(TCPChannelConfiguration.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    private ChannelData channelData = null;
    private Map<Object, Object> channelProperties = null;

    // Initialize to the default values
    private int maxOpenConnections = TCPConfigConstants.MAX_CONNECTIONS_DEFAULT;
    private String workGroupName = "Default";
    private int listenBacklog = 511;
    private int newConnectionBufferSize = 8192;
    private int port = 80;
    private String hostname = "*";
    private int inactivityTimeout = TCPConfigConstants.INACTIVITY_TIMEOUT_DEFAULT_MSECS; // use
    // msec
    // in
    // code.
    private String addressExcludeList[] = null;
    private String hostNameExcludeList[] = null;
    private String addressIncludeList[] = null;
    private String hostNameIncludeList[] = null;
    private boolean allocateBuffersDirect = true;
    private boolean tcpNoDelay = true;
    private int soLinger = -1;
    private boolean soReuseAddress = true;
    private boolean keepAlive = true;
    private int receiveBufferSize = -1;
    private int sendBufferSize = -1;
    private boolean acceptThread = false;
    private boolean waitToAccept = false;
    private int dumpStatsInterval = 0;
    private String endPointName = null;

    private static final int COMM_OPTION_FORCE_NIO = 0;
    private static final int COMM_OPTION_DONT_FORCE_NIO = 1;
    private int commOption = COMM_OPTION_DONT_FORCE_NIO;

    private final boolean inbound;
    private String externalName = null;

    private boolean caseInsensitiveHostnames = true; // F184719

    /**
     * Constructor.
     *
     * @param chanData
     * @throws ChannelException
     */
    public TCPChannelConfiguration(ChannelData chanData) throws ChannelException {
        this.channelData = chanData;
        this.inbound = chanData.isInbound();
        this.channelProperties = chanData.getPropertyBag();
        this.externalName = chanData.getExternalName();

        if (this.channelProperties != null) {
            // read in values now, to save time reading them in each time
            // they are requested
            setValues();
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "TCPChannelConfiguration object constructed with null properties");
            }
            throw new ChannelException("TCPChannelConfiguration constructed with null properties");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            outputConfigToTrace();
        }
    }

    private boolean skipKey(String key) {
        return key.equals("id") ||
               key.equals("type") ||
               key.startsWith("service.") ||
               key.startsWith("component.") ||
               key.startsWith("config.") ||
               key.startsWith("objectClass");
    }

    private void setValues() throws ChannelException {

        String key = null;
        Object value = null;
        int result = ValidateUtils.VALIDATE_OK;
        int minValue = 0;
        int maxValue = 0;
        int keyType = 0;
        ChannelException e = null;

        // F184719 - If the custom property is set, it is necessary for this to
        // be loaded before the access lists are examined. This property name is
        // not case insensitive as the properties in the set below.

        if (channelProperties.containsKey(CASE_INSENSITIVE_HOSTNAMES)) {

            key = ((String) channelProperties.get(CASE_INSENSITIVE_HOSTNAMES));

            this.caseInsensitiveHostnames = Boolean.parseBoolean(key);
        }

        for (Entry<Object, Object> entry : this.channelProperties.entrySet()) {
            if (ValidateUtils.VALIDATE_OK != result) {
                break;
            }
            key = (String) entry.getKey();
            value = entry.getValue();

            if (skipKey(key)) {
                // skip osgi standard properties
                continue;
            }

            if (key.equalsIgnoreCase(CASE_INSENSITIVE_HOSTNAMES)) {
                // Already handled above, so continue to the next key
                continue;
            }

            try {
                if (isInbound()) {
                    if (key.equalsIgnoreCase(HOST_NAME)) {
                        // convert and check
                        keyType = ValidateUtils.KEY_TYPE_STRING;
                        this.hostname = (String) value;
                        if (value == null) {
                            result = ValidateUtils.VALIDATE_ERROR;
                        }
                        continue;
                    }

                    if (key.equalsIgnoreCase(PORT)) {
                        // convert and check
                        keyType = ValidateUtils.KEY_TYPE_INT;
                        minValue = PORT_MIN;
                        maxValue = PORT_MAX;
                        this.port = convertIntegerValue(value);
                        result = ValidateUtils.testPort(this.port);
                        continue;
                    }

                    if (key.equalsIgnoreCase(MAX_CONNS)) {
                        // convert and check
                        keyType = ValidateUtils.KEY_TYPE_INT;
                        minValue = MAX_CONNECTIONS_MIN;
                        maxValue = MAX_CONNECTIONS_MAX;
                        this.maxOpenConnections = convertIntegerValue(value);
                        result = ValidateUtils.testMaxConnections(this.maxOpenConnections);
                        continue;
                    }

                    if (key.equalsIgnoreCase(NEW_BUFF_SIZE)) {
                        // convert and check
                        keyType = ValidateUtils.KEY_TYPE_INT;
                        minValue = ValidateUtils.NEW_BUFF_SIZE_MIN;
                        maxValue = ValidateUtils.NEW_BUFF_SIZE_MAX;
                        this.newConnectionBufferSize = convertIntegerValue(value);
                        result = ValidateUtils.testNewBuffSize(this.newConnectionBufferSize);
                        continue;
                    }

                    if (key.equalsIgnoreCase(BACKLOG)) {
                        // convert and check
                        keyType = ValidateUtils.KEY_TYPE_INT;
                        minValue = ValidateUtils.LISTEN_BACKLOG_MIN;
                        maxValue = ValidateUtils.LISTEN_BACKLOG_MAX;
                        this.listenBacklog = convertIntegerValue(value);
                        result = ValidateUtils.testListenBacklog(this.listenBacklog);
                        continue;
                    }

                    if (key.equalsIgnoreCase(ADDR_EXC_LIST)) {
                        keyType = ValidateUtils.KEY_TYPE_ACCESS_LIST;
                        if (value instanceof String) {
                            this.addressExcludeList = convertToArray((String) value);
                        } else {
                            this.addressExcludeList = (String[]) value;
                        }
                        result = ValidateUtils.testIsStringIPAddressesValid(this.addressExcludeList);
                        if (result != ValidateUtils.VALIDATE_OK) {
                            Tr.error(tc, TCPChannelMessageConstants.ADDRESS_EXCLUDE_LIST_INVALID, new Object[] { this.externalName });
                            e = new ChannelException("An entry in the address exclude list for a TCP Channel was not valid.  Valid values consist of a valid String. Channel Name: "
                                                     + this.externalName);
                            break;
                        }
                        continue;
                    }

                    if (key.equalsIgnoreCase(ADDR_INC_LIST)) {
                        keyType = ValidateUtils.KEY_TYPE_ACCESS_LIST;
                        if (value instanceof String) {
                            this.addressIncludeList = convertToArray((String) value);
                        } else {
                            this.addressIncludeList = (String[]) value;
                        }
                        result = ValidateUtils.testIsStringIPAddressesValid(this.addressIncludeList);
                        if (result != ValidateUtils.VALIDATE_OK) {
                            Tr.error(tc, TCPChannelMessageConstants.ADDRESS_INCLUDE_LIST_INVALID, new Object[] { this.externalName });
                            e = new ChannelException("An entry in the address include list for a TCP Channel was not valid.  Valid values consist of a valid String. Channel Name: "
                                                     + this.externalName);
                            break;
                        }
                        continue;
                    }

                    if (key.equalsIgnoreCase(NAME_EXC_LIST)) {
                        keyType = ValidateUtils.KEY_TYPE_ACCESS_LIST;
                        if (value instanceof String) {
                            //F184719 - Set any defined list  of excluded hostnames to lower casing.
                            if (this.caseInsensitiveHostnames) {
                                this.hostNameExcludeList = convertToArray(((String) value).toLowerCase());
                            } else {
                                this.hostNameExcludeList = convertToArray((String) value);
                            }
                        } else {
                            this.hostNameExcludeList = (String[]) value;
                            //F184719 - Need to iterate and lower case everything
                            if (this.caseInsensitiveHostnames) {
                                for (int i = 0; i < this.hostNameExcludeList.length; i++) {
                                    if (this.hostNameExcludeList[i] != null) {
                                        this.hostNameExcludeList[i] = this.hostNameExcludeList[i].toLowerCase();
                                    }
                                }
                            }
                        }

                        continue;
                    }

                    if (key.equalsIgnoreCase(NAME_INC_LIST)) {
                        keyType = ValidateUtils.KEY_TYPE_ACCESS_LIST;
                        if (value instanceof String) {
                            this.hostNameIncludeList = convertToArray((String) value);
                        } else {
                            this.hostNameIncludeList = (String[]) value;
                        }
                        continue;
                    }

                    if (key.equalsIgnoreCase(ACCEPT_THREAD)) {
                        // convert and check
                        keyType = ValidateUtils.KEY_TYPE_BOOLEAN;
                        this.acceptThread = convertBooleanValue(value);
                        result = ValidateUtils.VALIDATE_OK;

                        // waitToAccept overrides and forces acceptThread to be true
                        if (this.waitToAccept == true) {
                            this.acceptThread = true;
                        }

                        continue;
                    }

                    if (key.equalsIgnoreCase(WAIT_TO_ACCEPT)) {
                        // convert and check
                        keyType = ValidateUtils.KEY_TYPE_BOOLEAN;
                        this.waitToAccept = convertBooleanValue(value);
                        result = ValidateUtils.VALIDATE_OK;

                        // waitToAccept overrides and forces acceptThread to be true
                        if (this.waitToAccept == true) {
                            this.acceptThread = true;
                        }

                        continue;
                    }

                    if (key.equalsIgnoreCase(DIRECT_BUFFS)) {
                        // convert and check
                        keyType = ValidateUtils.KEY_TYPE_BOOLEAN;
                        this.allocateBuffersDirect = convertBooleanValue(value);
                        result = ValidateUtils.VALIDATE_OK;
                        continue;
                    }

                    if (key.equalsIgnoreCase(ENDPOINT_NAME)) {
                        // this parameter only has to be present if
                        // WAS is running,
                        keyType = ValidateUtils.KEY_TYPE_STRING;
                        this.endPointName = (String) value;
                        if (value == null) {
                            result = ValidateUtils.VALIDATE_ERROR;
                        }
                        continue;
                    }

                    if (key.equalsIgnoreCase(TCPConfigConstants.LISTENING_PORT)) {
                        // just ignore this
                        continue;
                    }
                } else {
                    // outbound only configuration parameters
                    if (key.equalsIgnoreCase(ADDR_EXC_LIST)) {
                        //This is a valid configuration option but outbound channels do not use it
                        //Adding this prevents a message from being output saying it's invalid
                        continue;
                    }

                    if (key.equalsIgnoreCase(ADDR_INC_LIST)) {
                        //This is a valid configuration option but outbound channels do not use it
                        //Adding this prevents a message from being output saying it's invalid
                        continue;
                    }

                    if (key.equalsIgnoreCase(NAME_EXC_LIST)) {
                        //This is a valid configuration option but outbound channels do not care use it
                        //Adding this prevents a message from being output saying it's invalid
                        continue;
                    }

                    if (key.equalsIgnoreCase(NAME_INC_LIST)) {
                        //This is a valid configuration option but outbound channels do not use it
                        //Adding this prevents a message from being output saying it's invalid
                        continue;
                    }

                    if (key.equalsIgnoreCase(WAIT_TO_ACCEPT)) {
                        //This is a valid configuration option but outbound channels do not use it
                        //Adding this prevents a message from being output saying it's invalid
                        continue;
                    }

                    if (key.equalsIgnoreCase(ACCEPT_THREAD)) {
                        //This is a valid configuration option but outbound channels do not use it
                        //Adding this prevents a message from being output saying it's invalid
                        continue;
                    }
                }

                // PK37541 - move reuse_addr to common config
                if (key.equalsIgnoreCase(REUSE_ADDR)) {
                    // convert and check
                    keyType = ValidateUtils.KEY_TYPE_BOOLEAN;
                    this.soReuseAddress = convertBooleanValue(value);
                    result = ValidateUtils.VALIDATE_OK;
                    continue;
                }

                if (key.equalsIgnoreCase(NO_DELAY)) {
                    // convert and check
                    keyType = ValidateUtils.KEY_TYPE_BOOLEAN;
                    this.tcpNoDelay = convertBooleanValue(value);
                    result = ValidateUtils.VALIDATE_OK;
                    continue;
                }

                if (key.equalsIgnoreCase(LINGER)) {
                    // convert and check
                    keyType = ValidateUtils.KEY_TYPE_INT;
                    minValue = ValidateUtils.LINGER_MIN;
                    maxValue = ValidateUtils.LINGER_MAX;
                    this.soLinger = convertIntegerValue(value);
                    result = ValidateUtils.testLinger(this.soLinger);
                    // PK43770 - soLinger is supposed to be in seconds when set
                    // on the Java Socket object so don't convert to milliseconds
                    continue;
                }

                if (key.equalsIgnoreCase(KEEP_ALIVE)) {
                    // convert and check
                    keyType = ValidateUtils.KEY_TYPE_BOOLEAN;
                    this.keepAlive = convertBooleanValue(value);
                    result = ValidateUtils.VALIDATE_OK;
                    continue;
                }

                if (key.equalsIgnoreCase(RCV_BUFF_SIZE)) {
                    // convert and check
                    keyType = ValidateUtils.KEY_TYPE_INT;
                    minValue = RECEIVE_BUFFER_SIZE_MIN;
                    maxValue = RECEIVE_BUFFER_SIZE_MAX;
                    this.receiveBufferSize = convertIntegerValue(value);
                    result = ValidateUtils.testReceiveBufferSize(this.receiveBufferSize);
                    continue;
                }

                if (key.equalsIgnoreCase(SEND_BUFF_SIZE)) {
                    // convert and check
                    keyType = ValidateUtils.KEY_TYPE_INT;
                    minValue = SEND_BUFFER_SIZE_MIN;
                    maxValue = SEND_BUFFER_SIZE_MAX;
                    this.sendBufferSize = convertIntegerValue(value);
                    result = ValidateUtils.testSendBufferSize(this.sendBufferSize);
                    continue;
                }

                if (key.equalsIgnoreCase(GROUPNAME)) {
                    keyType = ValidateUtils.KEY_TYPE_STRING;
                    this.workGroupName = (String) value;
                    if (null == this.workGroupName) {
                        result = ValidateUtils.VALIDATE_ERROR;
                    }
                    continue;
                }

                if (key.equalsIgnoreCase(INACTIVITY_TIMEOUT)) {
                    // convert and check
                    keyType = ValidateUtils.KEY_TYPE_INT;
                    minValue = INACTIVITY_TIMEOUT_MIN;
                    maxValue = INACTIVITY_TIMEOUT_MAX;
                    this.inactivityTimeout = convertIntegerValue(value);
                    result = ValidateUtils.testInactivityTimeout(this.inactivityTimeout);

                    continue;
                }
                if (key.equalsIgnoreCase(COMM_OPTION)) {
                    // convert and check
                    keyType = ValidateUtils.KEY_TYPE_INT;
                    minValue = ValidateUtils.COMM_OPTION_MIN;
                    maxValue = ValidateUtils.COMM_OPTION_MAX;
                    this.commOption = convertIntegerValue(value);
                    result = ValidateUtils.testCommOption(this.commOption);
                    continue;
                }

                if (key.equalsIgnoreCase(DUMP_STATS_INTERVAL)) {
                    // convert and check
                    keyType = ValidateUtils.KEY_TYPE_INT;
                    minValue = ValidateUtils.DUMP_STATS_INTERVAL_MIN;
                    maxValue = ValidateUtils.DUMP_STATS_INTERVAL_MAX;
                    this.dumpStatsInterval = convertIntegerValue(value);
                    result = ValidateUtils.testDumpStatsInterval(this.dumpStatsInterval);
                    continue;
                }

                if (key.equalsIgnoreCase(ChannelFrameworkConstants.CHAIN_DATA_KEY)) {
                    // this parameter is made available by the channel framework
                    // for any channel that needs it. This channel does not need it
                    // so ignore and move on.
                    continue;
                }

                if (key.equalsIgnoreCase(CHAIN_TYPE_KEY)) {
                    // this parameter is an internal optional property used to identify
                    // the type of chain being processed. If found simply bypass to
                    // suppress warning messages.
                    continue;
                }

                if (key.equalsIgnoreCase(ZAIO_RESOLVE_FOREIGN_HOSTNAMES_KEY)) {
                    // this parameter is an internal optional property used to identify
                    // whether the ZAioTCPChannel should resolve IP addresses to hostnames
                    continue;
                }

                if (key.equalsIgnoreCase(ZAIO_FREE_INITIAL_BUFFER_KEY)) {
                    // this parameter is an internal optional property used to identify
                    // whether the ZAioTCPChannel should take ownership and free the
                    // initial buffer on the connection
                    continue;
                }

                if (value instanceof String) {
                    Tr.warning(tc, TCPChannelMessageConstants.CONFIG_KEY_NOT_VALID, new Object[] { this.externalName, key, value });
                } else {
                    Tr.warning(tc, TCPChannelMessageConstants.CONFIG_KEY_NOT_VALID, new Object[] { this.externalName, key, "" });
                }

            } catch (NumberFormatException x) {
                // handle error knowing key and value
                Tr.error(tc, TCPChannelMessageConstants.CONFIG_VALUE_NUMBER_EXCEPTION, new Object[] { this.externalName, key, value });
                e = new ChannelException("TCP Channel Caught a NumberFormatException processing property, Channel Name: " + this.externalName + " Property name: " + key
                                         + " value: "
                                         + value, x);
                FFDCFilter.processException(e, getClass().getName(), "101", this);
                throw e;
            }
        }

        if (result != ValidateUtils.VALIDATE_OK) {
            // handle error knowing key and value
            if (keyType == ValidateUtils.KEY_TYPE_INT) {
                Tr.error(tc, TCPChannelMessageConstants.CONFIG_VALUE_NOT_VALID_INT,
                         new Object[] { this.externalName, key, value, String.valueOf(minValue), String.valueOf(maxValue) });
                e = new ChannelException("A TCP Channel has been constructed with incorrect configuration property value, Channel Name: " + this.externalName + " name: " + key
                                         + " value: " + value + " minimum Value: " + minValue + " maximum Value: " + maxValue);
            } else if (keyType == ValidateUtils.KEY_TYPE_BOOLEAN) {
                Tr.error(tc, TCPChannelMessageConstants.CONFIG_VALUE_NOT_VALID_BOOLEAN, new Object[] { this.externalName, key, value });
                e = new ChannelException("A TCP Channel has been constructed with incorrect configuration property value, Channel Name: " + this.externalName + " name: " + key
                                         + " value: " + value + " Valid Range: false, true");
            } else if (keyType == ValidateUtils.KEY_TYPE_STRING) {
                if (value == null) {
                    Tr.error(tc, TCPChannelMessageConstants.CONFIG_VALUE_NOT_VALID_NULL_STRING, new Object[] { this.externalName, key });
                    e = new ChannelException("A TCP Channel has been constructed with incorrect configuration property value, Channel Name: " + this.externalName + " name: " + key
                                             + " value: null");
                } else {
                    Tr.error(tc, TCPChannelMessageConstants.CONFIG_VALUE_NOT_VALID_STRING, new Object[] { this.externalName, key, value });
                    e = new ChannelException("A TCP Channel has been constructed with incorrect configuration property value, Channel Name: " + this.externalName + " name: " + key
                                             + " value: " + value);
                }
            }

            if (e != null) {
                FFDCFilter.processException(e, getClass().getName(), "102", this);
                throw e;
            }
        }

    }

    protected boolean checkAndSetValues(ChannelData chanData) {

        boolean _inbound = chanData.isInbound();
        String key = null;
        Object value = null;
        int result = ValidateUtils.VALIDATE_OK;
        int minValue = 0;
        int maxValue = 0;
        int keyType = 0;
        int oldValue = 0;
        String strOldValue = null;
        boolean oldBool = false;
        boolean update = true;

        // config vars which can be changed
        int maxOpenConnectionsNew = this.maxOpenConnections;
        int inactivityTimeoutNew = this.inactivityTimeout;
        String[] addressExcludeListNew = null;
        String[] addressIncludeListNew = null;
        String[] hostNameExcludeListNew = null;
        String[] hostNameIncludeListNew = null;

        for (Entry<Object, Object> entry : chanData.getPropertyBag().entrySet()) {
            if (ValidateUtils.VALIDATE_OK != result) {
                break;
            }
            key = (String) entry.getKey();
            value = entry.getValue();

            if (skipKey(key)) {
                // skip osgi standard properties
                continue;
            }

            try {
                if (_inbound) {
                    if (key.equalsIgnoreCase(HOST_NAME)) {
                        // convert and check
                        keyType = ValidateUtils.KEY_TYPE_STRING;
                        strOldValue = this.hostname;
                        if (!(((String) value).equals(strOldValue))) {
                            result = ValidateUtils.VALIDATE_NOT_EQUAL;
                        }
                        continue;
                    }

                    if (key.equalsIgnoreCase(PORT)) {
                        // convert and check
                        keyType = ValidateUtils.KEY_TYPE_INT;
                        oldValue = this.port;
                        if (convertIntegerValue(value) != oldValue) {
                            result = ValidateUtils.VALIDATE_NOT_EQUAL;
                        }
                        continue;
                    }

                    if (key.equalsIgnoreCase(MAX_CONNS)) {
                        // convert and check
                        keyType = ValidateUtils.KEY_TYPE_INT;
                        minValue = MAX_CONNECTIONS_MIN;
                        maxValue = MAX_CONNECTIONS_MAX;
                        maxOpenConnectionsNew = convertIntegerValue(value);
                        result = ValidateUtils.testMaxConnections(maxOpenConnectionsNew);
                        continue;
                    }

                    if (key.equalsIgnoreCase(NEW_BUFF_SIZE)) {
                        // convert and check
                        keyType = ValidateUtils.KEY_TYPE_INT;
                        oldValue = this.newConnectionBufferSize;
                        if (convertIntegerValue(value) != oldValue) {
                            result = ValidateUtils.VALIDATE_NOT_EQUAL;
                        }
                        continue;
                    }

                    if (key.equalsIgnoreCase(BACKLOG)) {
                        // convert and check
                        keyType = ValidateUtils.KEY_TYPE_INT;
                        oldValue = this.listenBacklog;
                        if (convertIntegerValue(value) != oldValue) {
                            result = ValidateUtils.VALIDATE_NOT_EQUAL;
                        }
                        continue;
                    }

                    if (key.equalsIgnoreCase(ADDR_EXC_LIST)) {
                        keyType = ValidateUtils.KEY_TYPE_ACCESS_LIST;
                        if (value instanceof String) {
                            addressExcludeListNew = convertToArray((String) value);
                        } else {
                            addressExcludeListNew = (String[]) value;
                        }
                        result = ValidateUtils.testIsStringIPAddressesValid(addressExcludeListNew);
                        if (result != ValidateUtils.VALIDATE_OK) {
                            Tr.error(tc, TCPChannelMessageConstants.ADDRESS_EXCLUDE_LIST_INVALID, new Object[] { this.externalName });
                            break;
                        }
                        continue;
                    }

                    if (key.equalsIgnoreCase(ADDR_INC_LIST)) {
                        keyType = ValidateUtils.KEY_TYPE_ACCESS_LIST;
                        if (value instanceof String) {
                            addressIncludeListNew = convertToArray((String) value);
                        } else {
                            addressIncludeListNew = (String[]) value;
                        }
                        result = ValidateUtils.testIsStringIPAddressesValid(addressExcludeListNew);
                        if (result != ValidateUtils.VALIDATE_OK) {
                            Tr.error(tc, TCPChannelMessageConstants.ADDRESS_INCLUDE_LIST_INVALID, new Object[] { this.externalName });
                            break;
                        }
                        continue;
                    }

                    if (key.equalsIgnoreCase(NAME_EXC_LIST)) {
                        keyType = ValidateUtils.KEY_TYPE_ACCESS_LIST;
                        if (value instanceof String) {
                            hostNameExcludeListNew = convertToArray((String) value);
                        } else {
                            hostNameExcludeListNew = (String[]) value;
                        }
                        continue;
                    }

                    if (key.equalsIgnoreCase(NAME_INC_LIST)) {
                        keyType = ValidateUtils.KEY_TYPE_ACCESS_LIST;
                        if (value instanceof String) {
                            hostNameIncludeListNew = convertToArray((String) value);
                        } else {
                            hostNameIncludeListNew = (String[]) value;
                        }
                        continue;
                    }

                    if (key.equalsIgnoreCase(ENDPOINT_NAME)) {
                        // can't dynamically change endpoint name
                        keyType = ValidateUtils.KEY_TYPE_STRING;
                        strOldValue = this.endPointName;
                        if (!(((String) value).equals(strOldValue))) {
                            result = ValidateUtils.VALIDATE_NOT_EQUAL;
                        }
                        continue;
                    }

                    if (key.equalsIgnoreCase(TCPConfigConstants.LISTENING_PORT)) {
                        // just ignore this
                        continue;
                    }
                } else {
                    // outbound only parameters
                }

                if (key.equalsIgnoreCase(NO_DELAY)) {
                    // convert and check
                    keyType = ValidateUtils.KEY_TYPE_BOOLEAN;
                    oldBool = this.tcpNoDelay;
                    if (convertBooleanValue(value) != oldBool) {
                        result = ValidateUtils.VALIDATE_NOT_EQUAL;
                    }
                    continue;
                }

                // PK37541 - move reuse_addr to common config
                if (key.equalsIgnoreCase(REUSE_ADDR)) {
                    // convert and check
                    keyType = ValidateUtils.KEY_TYPE_BOOLEAN;
                    oldBool = this.soReuseAddress;
                    if (convertBooleanValue(value) != oldBool) {
                        result = ValidateUtils.VALIDATE_NOT_EQUAL;
                    }
                    continue;
                }

                if (key.equalsIgnoreCase(LINGER)) {
                    // convert and check
                    keyType = ValidateUtils.KEY_TYPE_INT;
                    oldValue = this.soLinger;
                    if (convertIntegerValue(value) != oldValue) {
                        result = ValidateUtils.VALIDATE_NOT_EQUAL;
                    }
                    continue;
                }

                if (key.equalsIgnoreCase(ACCEPT_THREAD)) {
                    // convert and check
                    keyType = ValidateUtils.KEY_TYPE_BOOLEAN;
                    oldBool = this.acceptThread;
                    if (convertBooleanValue(value) != oldBool) {
                        result = ValidateUtils.VALIDATE_NOT_EQUAL;
                    }
                    continue;
                }

                if (key.equalsIgnoreCase(WAIT_TO_ACCEPT)) {
                    // convert and check
                    keyType = ValidateUtils.KEY_TYPE_BOOLEAN;
                    oldBool = this.waitToAccept;
                    if (convertBooleanValue(value) != oldBool) {
                        result = ValidateUtils.VALIDATE_NOT_EQUAL;
                    }
                    continue;
                }

                if (key.equalsIgnoreCase(DIRECT_BUFFS)) {
                    // convert and check
                    keyType = ValidateUtils.KEY_TYPE_BOOLEAN;
                    oldBool = this.allocateBuffersDirect;
                    if (convertBooleanValue(value) != oldBool) {
                        result = ValidateUtils.VALIDATE_NOT_EQUAL;
                    }
                    continue;
                }

                if (key.equalsIgnoreCase(KEEP_ALIVE)) {
                    // convert and check
                    keyType = ValidateUtils.KEY_TYPE_BOOLEAN;
                    oldBool = this.keepAlive;
                    if (convertBooleanValue(value) != oldBool) {
                        result = ValidateUtils.VALIDATE_NOT_EQUAL;
                    }
                    continue;
                }

                if (key.equalsIgnoreCase(RCV_BUFF_SIZE)) {
                    // convert and check
                    keyType = ValidateUtils.KEY_TYPE_INT;
                    oldValue = this.receiveBufferSize;
                    if (convertIntegerValue(value) != oldValue) {
                        result = ValidateUtils.VALIDATE_NOT_EQUAL;
                    }
                    continue;
                }

                if (key.equalsIgnoreCase(SEND_BUFF_SIZE)) {
                    // convert and check
                    keyType = ValidateUtils.KEY_TYPE_INT;
                    oldValue = this.sendBufferSize;
                    if (convertIntegerValue(value) != oldValue) {
                        result = ValidateUtils.VALIDATE_NOT_EQUAL;
                    }
                    continue;
                }

                if (key.equalsIgnoreCase(GROUPNAME)) {
                    keyType = ValidateUtils.KEY_TYPE_STRING;
                    strOldValue = this.workGroupName;
                    this.workGroupName = (String) value;
                    if (null == this.workGroupName) {
                        result = ValidateUtils.VALIDATE_ERROR;
                    }
                    continue;
                }

                if (key.equalsIgnoreCase(INACTIVITY_TIMEOUT)) {
                    // convert and check
                    keyType = ValidateUtils.KEY_TYPE_INT;
                    inactivityTimeoutNew = convertIntegerValue(value);
                    result = ValidateUtils.testInactivityTimeout(inactivityTimeoutNew);

                    continue;
                }

                if (key.equalsIgnoreCase(COMM_OPTION)) {
                    // convert and check
                    keyType = ValidateUtils.KEY_TYPE_INT;
                    oldValue = this.commOption;
                    if (convertIntegerValue(value) != oldValue) {
                        result = ValidateUtils.VALIDATE_NOT_EQUAL;
                    }
                    continue;
                }

                if (key.equalsIgnoreCase(ChannelFrameworkConstants.CHAIN_DATA_KEY)) {
                    // this parameter is made available by the channel framework
                    // for any channel that needs it. This channel does not need it
                    // so ignore and move on.
                    continue;
                }

                if (key.equalsIgnoreCase(CHAIN_TYPE_KEY)) {
                    // this parameter is an internal optional property used to identify
                    // the type of chain being processed. If found simply bypass to
                    // suppress warning messages.
                    continue;
                }

                if (key.equalsIgnoreCase(ZAIO_RESOLVE_FOREIGN_HOSTNAMES_KEY)) { // @352132A - if block
                    // this parameter is an internal optional property used to identify
                    // whether the ZAioTCPChannel should resolve IP addresses to hostnames
                    continue;
                }

                if (key.equalsIgnoreCase(ZAIO_FREE_INITIAL_BUFFER_KEY)) { // @PK36998A - if block
                    // this parameter is an internal optional property used to identify
                    // whether the ZAioTCPChannel should take ownership and free the
                    // initial buffer on the connection
                    continue;
                }

                if (value instanceof String) {
                    Tr.warning(tc, TCPChannelMessageConstants.CONFIG_KEY_NOT_VALID, new Object[] { this.externalName, key, value });
                } else {
                    Tr.warning(tc, TCPChannelMessageConstants.CONFIG_KEY_NOT_VALID, new Object[] { this.externalName, key, "" });
                }

            } catch (NumberFormatException x) {
                update = false;
                // handle error knowing key and value
                Tr.error(tc, TCPChannelMessageConstants.CONFIG_VALUE_NUMBER_EXCEPTION, new Object[] { this.externalName, key, value });
            }
        }

        if (result == ValidateUtils.VALIDATE_ERROR) {
            update = false;
            // handle error knowing key and value
            if (keyType == ValidateUtils.KEY_TYPE_INT) {
                Tr.error(tc, TCPChannelMessageConstants.CONFIG_VALUE_NOT_VALID_INT,
                         new Object[] { this.externalName, key, value, String.valueOf(minValue), String.valueOf(maxValue) });
            } else if (keyType == ValidateUtils.KEY_TYPE_BOOLEAN) {
                Tr.error(tc, TCPChannelMessageConstants.CONFIG_VALUE_NOT_VALID_BOOLEAN, new Object[] { this.externalName, key, value });
            } else if (keyType == ValidateUtils.KEY_TYPE_STRING) {
                if (value == null) {
                    Tr.error(tc, TCPChannelMessageConstants.CONFIG_VALUE_NOT_VALID_NULL_STRING, new Object[] { this.externalName, key });
                } else {
                    Tr.error(tc, TCPChannelMessageConstants.CONFIG_VALUE_NOT_VALID_STRING, new Object[] { this.externalName, key, value });
                }
            }

            Tr.error(tc, TCPChannelMessageConstants.UPDATED_CONFIG_NOT_IMPLEMENTED, new Object[] { this.externalName });
        }

        if (result == ValidateUtils.VALIDATE_NOT_EQUAL) {
            update = false;
            // handle error knowing key and value
            if (keyType == ValidateUtils.KEY_TYPE_INT) {
                Tr.error(tc, TCPChannelMessageConstants.NEW_CONFIG_VALUE_NOT_EQUAL, new Object[] { this.externalName, key, String.valueOf(oldValue), value });
            } else if (keyType == ValidateUtils.KEY_TYPE_STRING) {
                Tr.error(tc, TCPChannelMessageConstants.NEW_CONFIG_VALUE_NOT_EQUAL, new Object[] { this.externalName, key, strOldValue, value });
            } else if (keyType == ValidateUtils.KEY_TYPE_BOOLEAN) {
                Tr.error(tc, TCPChannelMessageConstants.NEW_CONFIG_VALUE_NOT_EQUAL, new Object[] { this.externalName, key, Boolean.toString(oldBool), value });
            }
            Tr.error(tc, TCPChannelMessageConstants.UPDATED_CONFIG_NOT_IMPLEMENTED, new Object[] { this.externalName });
        }

        if (update) {
            this.maxOpenConnections = maxOpenConnectionsNew;
            this.inactivityTimeout = inactivityTimeoutNew;
            this.addressExcludeList = addressExcludeListNew;
            this.addressIncludeList = addressIncludeListNew;
            this.hostNameExcludeList = hostNameExcludeListNew;
            this.hostNameIncludeList = hostNameIncludeListNew;
        }

        return update;
    }

    private String[] convertToArray(String allEntries) {
        // convert String allEntries (which has comma seperators)
        // into an array of strings.

        int start = 0;
        int end = 0;
        String newAddress = null;
        if (allEntries == null) {
            return null;
        }

        int length = allEntries.length();

        List<String> entryList = new ArrayList<String>();

        while (start != length) {
            end = allEntries.indexOf(',', start);
            if (end > start) {
                newAddress = allEntries.substring(start, end);
                newAddress = newAddress.trim();
                entryList.add(newAddress);
            } else {
                // if end == start, then the substring is only a comma, so ignore
                // otherwise end is -1, so just go to the end
                if (end != start) {
                    newAddress = allEntries.substring(start);
                    newAddress = newAddress.trim();
                    entryList.add(newAddress);
                }
            }

            if (end == -1) {
                break; // no more data in string
            }
            start = end + 1;
        }

        if (entryList.isEmpty()) {
            return null;
        }
        return entryList.toArray(new String[entryList.size()]);
    }

    private int convertIntegerValue(Object value) {
        if (value instanceof Integer)
            return (Integer) value;
        return Integer.parseInt(value.toString().trim());
    }

    private boolean convertBooleanValue(Object value) {
        if (value instanceof Boolean)
            return (Boolean) value;
        return Boolean.parseBoolean(value.toString().trim());
    }

    protected ChannelData getChannelData() {
        return this.channelData;
    }

    protected int getMaxOpenConnections() {
        return this.maxOpenConnections;
    }

    /**
     * Query the work group name for this channel.
     *
     * @return String
     */
    public String getWorkGroupName() {
        return this.workGroupName;
    }

    protected int getListenBacklog() {
        return this.listenBacklog;
    }

    protected int getNewConnectionBufferSize() {
        return this.newConnectionBufferSize;
    }

    protected boolean getAllocateBuffersDirect() {
        return this.allocateBuffersDirect;
    }

    protected boolean getTcpNoDelay() {
        return this.tcpNoDelay;
    }

    protected boolean getSoReuseAddress() {
        return this.soReuseAddress;
    }

    protected boolean getKeepAlive() {
        return this.keepAlive;
    }

    /**
     * Query the hostname for this channel, returning null if it was a wildcard.
     *
     * @return String
     */
    public String getHostname() {
        if (this.hostname.equals("*")) {
            // CFEndPointImpl in CF.service depends on "*" specifically,
            // so if we ever allow leading or trailing spaces, we will need to adjust
            // that to.
            return null;
        }
        return this.hostname;
    }

    public String getDisplayableHostname() {
        return this.hostname;
    }

    /**
     * Returns the number of milliseconds a socket can remain inactive for
     * before it is automatically closed. A negative value indicates that the
     * socket will never be automatically closed.
     *
     * @return int
     */
    protected int getInactivityTimeout() {
        return this.inactivityTimeout;
    }

    /*
     * @see ChannelData#getChannelId()
     */
    protected String getChannelId() {
        return "TCPChannel";
    }

    protected String[] getAddressExcludeList() {
        return this.addressExcludeList;
    }

    protected String[] getHostNameExcludeList() {
        return this.hostNameExcludeList;
    }

    protected String[] getAddressIncludeList() {
        return this.addressIncludeList;
    }

    protected String[] getHostNameIncludeList() {
        return this.hostNameIncludeList;
    }

    /**
     * Get the port property.
     *
     * @return int
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Query whether the configuration is forcing NIO.
     *
     * @return boolean
     */
    public boolean isNIOOnly() {
        return (this.commOption == COMM_OPTION_FORCE_NIO);
    }

    protected void outputConfigToTrace() {
        Tr.debug(tc, "Config parameters for TCP Channel: " + getChannelData().getExternalName());
        if (isInbound()) {
            // inbound specific values
            Tr.debug(tc, HOST_NAME + ": " + getHostname());
            Tr.debug(tc, PORT + ": " + getPort());
            Tr.debug(tc, MAX_CONNS + ": " + getMaxOpenConnections());
            Tr.debug(tc, ADDR_EXC_LIST + ": " + debugStringArray(getAddressExcludeList()));
            Tr.debug(tc, NAME_EXC_LIST + ": " + debugStringArray(getHostNameExcludeList()));
            Tr.debug(tc, ADDR_INC_LIST + ": " + debugStringArray(getAddressIncludeList()));
            Tr.debug(tc, NAME_INC_LIST + ": " + debugStringArray(getHostNameIncludeList()));
            Tr.debug(tc, BACKLOG + ": " + getListenBacklog());
            Tr.debug(tc, NEW_BUFF_SIZE + ": " + getNewConnectionBufferSize());
            Tr.debug(tc, CASE_INSENSITIVE_HOSTNAMES + ": " + getCaseInsensitiveHostnames());
        } else {
            // outbound specific values
        }

        // both inbound and outbound
        Tr.debug(tc, REUSE_ADDR + ": " + getSoReuseAddress());
        Tr.debug(tc, INACTIVITY_TIMEOUT + ": " + getInactivityTimeout());
        Tr.debug(tc, GROUPNAME + ": " + getWorkGroupName());
        Tr.debug(tc, DIRECT_BUFFS + ": " + getAllocateBuffersDirect());
        Tr.debug(tc, NO_DELAY + ": " + getTcpNoDelay());
        Tr.debug(tc, RCV_BUFF_SIZE + ": " + getReceiveBufferSize());
        Tr.debug(tc, SEND_BUFF_SIZE + ": " + getSendBufferSize());
        Tr.debug(tc, COMM_OPTION + ": " + this.commOption);
        Tr.debug(tc, LINGER + ": " + getSoLinger());
    }

    @Override
    public String[] introspectSelf() {
        List<String> output = new ArrayList<String>();
        if (isInbound()) {
            output.add(HOST_NAME + "=" + this.hostname);
            output.add(PORT + "=" + this.port);
            output.add(MAX_CONNS + "=" + this.maxOpenConnections);
            output.add(ADDR_EXC_LIST + "=" + debugStringArray(this.addressExcludeList));
            output.add(NAME_EXC_LIST + "=" + debugStringArray(this.hostNameExcludeList));
            output.add(ADDR_INC_LIST + "=" + debugStringArray(this.addressIncludeList));
            output.add(NAME_INC_LIST + "=" + debugStringArray(this.hostNameIncludeList));
            output.add(BACKLOG + "=" + this.listenBacklog);
            output.add(NEW_BUFF_SIZE + "=" + this.newConnectionBufferSize);
        } else {
            // outbound
        }

        // common parms for both inbound and outbound
        output.add(REUSE_ADDR + "=" + this.soReuseAddress);
        output.add(INACTIVITY_TIMEOUT + "=" + this.inactivityTimeout);
        output.add(GROUPNAME + "=" + this.workGroupName);
        output.add(DIRECT_BUFFS + "=" + this.allocateBuffersDirect);
        output.add(NO_DELAY + "=" + this.tcpNoDelay);
        output.add(RCV_BUFF_SIZE + "=" + this.receiveBufferSize);
        output.add(SEND_BUFF_SIZE + "=" + this.sendBufferSize);
        output.add(COMM_OPTION + "=" + this.commOption);
        output.add(LINGER + "=" + this.soLinger);
        return output.toArray(new String[output.size()]);
    }

    private String debugStringArray(String[] list) {
        if (null == list || 0 == list.length) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(list[0]);
        for (int i = 1; i < list.length; i++) {
            sb.append(',').append(list[i]);
        }
        return sb.toString();
    }

    /**
     * @return boolean
     */
    public boolean isInbound() {
        return this.inbound;
    }

    /**
     * @return int
     */
    protected int getReceiveBufferSize() {
        return this.receiveBufferSize;
    }

    /**
     * @return int
     */
    protected int getSendBufferSize() {
        return this.sendBufferSize;
    }

    /**
     * @return int
     */
    protected int getSoLinger() {
        return this.soLinger;
    }

    /**
     * @return int
     */
    public int getDumpStatsInterval() {
        return this.dumpStatsInterval;
    }

    /**
     * Query whether this TCP channel is using a dedicated accept thread
     * or sharing that selector with others.
     *
     * @return boolean (false means shared, true means dedicated)
     */
    protected boolean getAcceptThread() {
        return this.acceptThread;
    }

    protected boolean getCaseInsensitiveHostnames() {
        return caseInsensitiveHostnames;
    }

    /**
     * Query whether this TCP channel is using going to delay accepting connections until
     * the server is known to be completely started.
     *
     * If set to true, this options will override the acceptThread option, meaning the acceptThread option will be
     * treated as "true" for this endpoint even if it is set to false (which is the default).
     *
     * @return boolean (false means do not wait, true means to wait). false is the default.
     */
    protected boolean getWaitToAccept() {
        return this.waitToAccept;
    }

}
