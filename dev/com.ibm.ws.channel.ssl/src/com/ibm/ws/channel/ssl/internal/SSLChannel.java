/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channel.ssl.internal;

import java.net.InetSocketAddress;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSessionContext;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.JSSEProvider;
import com.ibm.websphere.ssl.SSLConfig;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.Discriminator;
import com.ibm.wsspi.channelfw.InboundChannel;
import com.ibm.wsspi.channelfw.OutboundChannel;
import com.ibm.wsspi.channelfw.OutboundProtocol;
import com.ibm.wsspi.channelfw.OutboundVirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.tcpchannel.TCPConnectRequestContext;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * The class represents an SSL Channel that will be used in the Channel Framework.
 */
public class SSLChannel implements InboundChannel, OutboundChannel, Discriminator {

    /** Trace component for WAS */
    private static final TraceComponent tc = Tr.register(SSLChannel.class,
                                                         SSLChannelConstants.SSL_TRACE_NAME,
                                                         SSLChannelConstants.SSL_BUNDLE);

    /** Key to store the discriminator state in the VC's state map. */
    public static final String SSL_DISCRIMINATOR_STATE = "SSLDiscState";

    /** Discrimination process used to determine the next channel to use for inbound. */
    protected DiscriminationProcess discProcess = null;
    /** Channel specific configuration data */
    protected SSLChannelData sslConfig = null;
    /** Initialized boolean */
    private boolean isInitialized = false;
    /** Object to track and handle SSL handshake failures. */
    protected SSLHandshakeErrorTracker handshakeErrorTracker = null;
    /** Name of security repertoire from this channel's config. */
    protected String alias = null;
    /** Valid for inbound only, the endPoint name from serverIndex.xml. */
    protected String endPointName = null;
    /** Valid for inbound only, the host being listened on. */
    protected String inboundHost = null;
    /** Valid for inbound only, the port being listened on. */
    protected String inboundPort = null;
    /** Provider used to access SSL Context. */
    protected JSSEProvider jsseProvider;
    /** Provider used to access SSL Context. */
    protected JSSEHelper jsseHelper;
    /** Platform check for Z */
    protected boolean isZOS = false;
    /** PK16095 - save the last SSLSessionContext created for this channel */
    private SSLSessionContext sessionContext = null;
    /** Factory used to create this channel */
    private SSLChannelFactoryImpl myFactory = null;
    /**
     * PI52696 - Timeout value for which the SSL closing handshake loop will attempt to complete final handshake
     * write before giving up.
     */
    private int timeoutValueInSSLClosingHandshake = 30;

    private static Boolean useH2ProtocolAttribute = null;

    /** Flag on whether stop with no quiese has been called after the last start call */
    volatile private boolean stop0Called = false;

    /**
     * Constructor.
     *
     * @param inputData Input channel configuration information
     * @param factory Factory used to create this channel instance
     * @throws ChannelException
     */
    public SSLChannel(ChannelData inputData, SSLChannelFactoryImpl factory) throws ChannelException {
        this(inputData);
        this.myFactory = factory;
        this.handshakeErrorTracker = createSSLHandshakeErrorTracker(inputData);

        try {
            // Get access to the provider -- might throw illegal state exception
            this.jsseProvider = SSLChannelProvider.getJSSEProvider();
            // Get access to the provider -- might throw illegal state exception
            this.jsseHelper = SSLChannelProvider.getJSSEHelper();
        } catch (IllegalStateException ise) {
            // Required services could not be found
            ChannelException ce = new ChannelException("SSL channel could not be created, required services not found " + ise.getMessage(), ise);
            ce.suppressFFDC(true);
            throw ce;
        }

        // TODO z/os
        // PlatformHelper osHelper = PlatformHelperFactory.getPlatformHelper();
        // if (osHelper != null) {
        // this.isZOS = osHelper.isZOS();
        // }
    }

    // This default constructor is intended to be called by the main constructor (above)
    // during runtime.  It is separated out of the main ctor to facilitate unit testing,
    // thus avoiding a lot of object mocking.
    SSLChannel(ChannelData inputData) throws ChannelException {
        this.sslConfig = new SSLChannelData(inputData);
    }

    /**
     * Create an SSLHandshakeErrorTracker using the properties in the property
     * bag. These properties may or may not be there if the channel is created
     * programmatically, and as such this provides defaults which the map will
     * be created with. These defaults should match the defaults from metatype.
     *
     * @param inputData
     */
    private SSLHandshakeErrorTracker createSSLHandshakeErrorTracker(ChannelData inputData) {
        Map<Object, Object> bag = inputData.getPropertyBag();
        // Even though they are of type Boolean and Long in the metatype, they are
        // going to be a String in the property bag (not sure why though).
        boolean suppressHandshakeError = SSLChannelConstants.DEFAULT_HANDSHAKE_FAILURE;
        Object value = bag.get(SSLChannelProvider.SSL_CFG_SUPPRESS_HANDSHAKE_ERRORS);
        if (value != null) {
            suppressHandshakeError = convertBooleanValue(value);
        }

        long maxLogEntries = SSLChannelConstants.DEFAULT_HANDSHAKE_FAILURE_STOP_LOGGING;
        value = bag.get(SSLChannelProvider.SSL_CFG_SUPPRESS_HANDSHAKE_ERRORS_COUNT);
        if (value != null) {
            maxLogEntries = convertLongValue(value);
        }
        return new SSLHandshakeErrorTracker(!suppressHandshakeError, maxLogEntries);
    }

    private long convertLongValue(Object value) {
        if (value instanceof Long)
            return (Long) value;
        return Long.parseLong(value.toString());
    }

    private boolean convertBooleanValue(Object value) {
        if (value instanceof Boolean)
            return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Get access to the object that tracks SSL handshake failures.
     *
     * @return SSLHandshakeErrorTracker
     */
    public SSLHandshakeErrorTracker getHandshakeErrorTracker() {
        return this.handshakeErrorTracker;
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#getDiscriminator()
     */
    @Override
    public Discriminator getDiscriminator() {
        return this;
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#getDiscriminationProcess()
     */
    @Override
    public DiscriminationProcess getDiscriminationProcess() {
        return this.discProcess;
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#setDiscriminationProcess(com.ibm.wsspi.channelfw.DiscriminationProcess)
     */
    @Override
    public void setDiscriminationProcess(DiscriminationProcess dp) {
        this.discProcess = dp;
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#getDiscriminatoryType()
     */
    @Override
    public Class<?> getDiscriminatoryType() {
        return WsByteBuffer.class;
    }

    /*
     * @see com.ibm.wsspi.channelfw.OutboundChannel#getDeviceAddress()
     */
    @Override
    public Class<?> getDeviceAddress() {
        return TCPConnectRequestContext.class;
    }

    /*
     * @see com.ibm.wsspi.channelfw.OutboundChannel#getApplicationAddress()
     */
    @Override
    public Class<?>[] getApplicationAddress() {
        return new Class[] { TCPConnectRequestContext.class };
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#getConnectionLink(com.ibm.wsspi.channelfw.VirtualConnection)
     */
    @Override
    public ConnectionLink getConnectionLink(VirtualConnection vc) {
        // Double check that the channel was initialized. It may have been delayed.
        if (!this.isInitialized) {
            try {
                init();
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception caught while getting SSL connection link: " + e);
                }
                FFDCFilter.processException(e, getClass().getName(), "148", this, new Object[] { vc });
                throw new RuntimeException(e);
            }
        }

        SSLConnectionLink link = new SSLConnectionLink(this);
        // Initialize appropriate fields.
        link.init(vc);
        return link;
    }

    /**
     * This method is overloaded from the base class in order to determine the host and port
     * of the connection required by the calls to the core security code which will eventually
     * return an SSLContext to use. This is only used by inbound connections.
     *
     * @param link
     * @param vc
     * @return SSLContext
     * @throws ChannelException
     */
    public SSLContext getSSLContextForInboundLink(SSLConnectionLink link, VirtualConnection vc) throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getSSLContextForInboundLink");
        }

        SSLContext context = getSSLContextForLink(vc, this.inboundHost,
                                                  this.inboundPort, this.endPointName, false, link);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getSSLContextForInboundLink");
        }
        return context;
    }

    /**
     * This method is overloaded from the base class in order to determine the host and port
     * of the connection required by the calls to the core security code which will eventually
     * return an SSLContext to use. This is only used by outbound connections.
     *
     * @param link
     * @param vc
     * @param address
     * @return SSLContext
     * @throws ChannelException
     */
    public SSLContext getSSLContextForOutboundLink(SSLConnectionLink link, VirtualConnection vc, Object address) throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getSSLContextForOutboundLink");
        }
        TCPConnectRequestContext tcpRequest = (TCPConnectRequestContext) address;
        InetSocketAddress socket = tcpRequest.getRemoteAddress();
        // Determine the protocol of the outbound connection. Default to HTTP.
        String protocol = Constants.ENDPOINT_HTTP;
        // Look for a protocol set in the state map of the VC.
        String mapProtocol = (String) vc.getStateMap().get(OutboundProtocol.PROTOCOL);
        if (mapProtocol != null) {
            // Found the protocol specified in the VC state map.
            protocol = mapProtocol;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "OutboundProtocol=" + protocol + " specified by in VC");
            }
        } else {
            // Check for the interface which provides this information.
            Object channelAccessor = ((OutboundVirtualConnection) vc).getChannelAccessor();
            if (channelAccessor instanceof OutboundProtocol) {
                protocol = ((OutboundProtocol) channelAccessor).getProtocol();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "OutboundProtocol=" + protocol + " specified by " + channelAccessor.getClass().getName());
                }
            }
        }
        SSLContext context = getSSLContextForLink(vc, socket.getHostName(), Integer.toString(socket.getPort()), protocol, Boolean.FALSE, link);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getSSLContextForOutboundLink");
        }
        return context;
    }

    /**
     * This method overloads the one in the parent class. It enables the ability
     * to have unique SSLContexts per connection. The security service is
     * leveraged for this.
     *
     * @param vc
     * @param host
     * @param port
     * @param endPoint
     * @param isZWebContainerChain
     * @param link
     * @return SSLContext
     * @throws ChannelException
     */
    protected SSLContext getSSLContextForLink(VirtualConnection vc,
                                              String host, String port, String endPoint, Boolean isZWebContainerChain,
                                              SSLConnectionLink link) throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "host=" + host + " port=" + port + " endPoint=" + endPoint);
        }

        // Set up the parameters needed to call into the JSSEHelper to extract official SSL Props.
        String direction = (getConfig().isInbound()) ? Constants.DIRECTION_INBOUND : Constants.DIRECTION_OUTBOUND;
        final Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, direction);
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_HOST, host);
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_PORT, port);
        connectionInfo.put(Constants.CONNECTION_INFO_ENDPOINT_NAME, endPoint);

        // PK40641 - handle channel props that do not need JSSEHelper
        Properties props = null;
        boolean useJSSEHelper = (null != this.alias);
        if (!useJSSEHelper) {
            // 436920 - check for ssl properties put programmatically on the thread.
            // Calling into JSSEHelper.getProperties() does this same action
            try {
                props = AccessController.doPrivileged(new PrivilegedExceptionAction<Properties>() {
                    @Override
                    public Properties run() throws Exception {
                        return jsseHelper.getSSLPropertiesOnThread();
                    }
                });
            } catch (Exception e) {
                // no ffdc required
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught exception looking for on-thread props; e=" + e);
                }
            }
            if (null == props || 0 == props.size()) {
                // if alias is null, then if keystore and truststore are found then
                // simply use the existing config properties. If either one is
                // missing, call into the jssehelper for the default repertoire (null alias)
                props = null;
                useJSSEHelper = !getConfig().getProperties().containsKey(Constants.SSLPROP_KEY_STORE)
                                || !getConfig().getProperties().containsKey(Constants.SSLPROP_TRUST_STORE);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found on-thread ssl properties");
                }
            }
        }
        if (useJSSEHelper) {
            try {
                // Extract the official SSL props based on config information.
                final String aliasFinal = this.alias;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Querying security service for alias=[" + aliasFinal + "]");
                }
                props = AccessController.doPrivileged(new PrivilegedExceptionAction<Properties>() {

                    @Override
                    public Properties run() throws Exception {
                        return jsseHelper.getProperties(aliasFinal, connectionInfo, null);
                    }
                });
            } catch (

            Exception e) {
                // no FFDC required
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception getting SSL properties from alias: " + this.alias);
                }
                throw new ChannelException(e);
            }
        }

        // at this point, we have a set of properties to use, which might be
        // repertoire based or on-thread config... merge the channel props into
        // them without overwriting any
        if (null != props) {
            Enumeration<?> names = getConfig().getProperties().propertyNames();
            String key = null;
            String value = null;
            while (names.hasMoreElements()) {
                key = (String) names.nextElement();
                value = getConfig().getStringProperty(key);
                if (null != value && !props.containsKey(key)) {
                    props.put(key, value);
                }
            }
        } else {
            // otherwise we just use the channel config
            props = getConfig().getProperties();
        }

        // "SSSL" is a zOS repertoire type that is not supported by SSLChannel
        // We only support "JSSE"
        String sslType = (String) props.get(Constants.SSLPROP_SSLTYPE);
        if (null != sslType) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "SSLConfig type: " + sslType);
            }
            if (sslType.equals("SSSL")) {
                throw new ChannelException("Invalid SSLConfig type: " + sslType);
            }
        }

        // if debug is enabled, print out the properties we're going to use
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "SSL configuration <null value means non-string>:");
            Enumeration<?> names = props.propertyNames();
            while (names.hasMoreElements()) {
                String key = (String) names.nextElement();
                String value = props.getProperty(key);
                if (-1 == key.toLowerCase().indexOf("password")) {
                    Tr.debug(tc, "\t" + key + " = " + value);
                } else {
                    // for nicer debug... print 1 * per character in the password
                    StringBuilder output = new StringBuilder(4 + key.length() + value.length());
                    output.append("\t").append(key).append(" = ");
                    for (int i = 0; i < value.length(); i++) {
                        output.append("*");
                    }
                    Tr.debug(tc, output.toString());
                }
            }
        }

        SSLContext context = null;
        try {
            SSLConfig config = new SSLConfig(props);
            context = this.jsseProvider.getSSLContext(connectionInfo, config);
            SSLLinkConfig linkConfig = new SSLLinkConfig(props);
            if (null == link) {
                // discrimination path
                vc.getStateMap().put(SSLConnectionLink.LINKCONFIG, linkConfig);
            } else {
                link.setLinkConfig(linkConfig);
            }
        } catch (Exception e) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception getting SSLContext from properties.", new Object[] { e });
            }
            throw new ChannelException(e);
        }
        return context;
    }

    /**
     * PI52696 - Timeout value for which the SSL closing handshake loop will attempt to complete final handshake
     * write before giving up.
     *
     * @return
     */
    public int getTimeoutValueInSSLClosingHandshake() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "timeoutValueInSSLClosingHandshake : " + this.timeoutValueInSSLClosingHandshake);
        }
        return this.timeoutValueInSSLClosingHandshake;
    }

    /**
     * Indicates whether the SSL Channel is configured to use HTTP/2
     *
     * @return
     */
    public Boolean getUseH2ProtocolAttribute() {
        return this.useH2ProtocolAttribute;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#start()
     */
    @Override
    public void start() throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "start");
        }

        stop0Called = false;

        try {
            // If this is an inbound channel, we can pull out the listening host/port/endpointName.
            if (getConfig().isInbound()) {
                // Get access to the ChainData object for future use.
                ChainData chainData = SSLChannelProvider.getCfw().getInternalRunningChains(getConfig().getName())[0];
                ChannelData channelData = chainData.getChannelList()[0];
                Map<?, ?> channelProperties = channelData.getPropertyBag();
                this.inboundHost = (String) channelProperties.get("hostname");
                this.inboundPort = (String) channelProperties.get("port");
                // End point name will only ever get a value when running in was. Otherwise, null is fine.
                this.endPointName = (String) channelProperties.get("endPointName");
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "inboundHost = " + this.inboundHost
                                 + " inboundPort = " + this.inboundPort
                                 + " endPointName = " + this.endPointName);

                }
            }
        } catch (Exception e) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception during start, throwing up stack.  " + e);
            }
            throw new ChannelException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "start");
        }
    }

    /**
     * @return stop 0 quiesce state of the channel
     */
    public boolean getstop0Called() {
        return stop0Called;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#stop(long)
     */
    @Override
    public void stop(long millisec) throws ChannelException {
        // Nothing needed here. Once the channel framework pulls out discrimination
        // in the device side channel the stop is complete.

        if (millisec == 0) {

            // set this to true after doing other stop processing, which right now is none.
            stop0Called = true;
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#init()
     */
    @Override
    public void init() throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "init");
        }

        // Prevent duplicate initialization.
        if (this.isInitialized) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "init");
            }
            return;
        }

        // Extract the channel properties.
        try {
            Properties channelProps = getConfig().getProperties();
            // Handle a potentially null property map.
            if (channelProps != null) {
                this.alias = channelProps.getProperty(SSLChannelData.ALIAS_KEY);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    if (this.alias != null) {
                        Tr.debug(tc, "Found alias in SSL properties, " + this.alias);
                    } else {
                        Tr.debug(tc, "No alias found in SSL properties");
                    }
                }
                //PI52696 - Timeout value for which the SSL closing handshake loop will attempt to complete final handshake
                //write before giving up.
                String timeoutValueInSSLClosingHandshake = channelProps.getProperty(SSLChannelConstants.TIMEOUT_VALUE_IN_SSL_CLOSING_HANDSHAKE);
                if (timeoutValueInSSLClosingHandshake != null) {
                    this.timeoutValueInSSLClosingHandshake = Integer.parseInt(timeoutValueInSSLClosingHandshake);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found timeoutValueInSSLClosingHandshake in SSL properties, " + this.timeoutValueInSSLClosingHandshake);
                    }

                }

                //Check for system property so all SSL Channels can have the property enabled

                //APAR PI70332 - Add Java custom property to allow this property to be set on all inbound/outbound channels
                //without need of SSL configuration.

                String timeoutValueSystemProperty = AccessController.doPrivileged(new java.security.PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        return (System.getProperty(SSLChannelConstants.TIMEOUT_VALUE_IN_SSL_CLOSING_HANDSHAKE));
                    }
                });

                if (timeoutValueSystemProperty != null) {
                    this.timeoutValueInSSLClosingHandshake = Integer.parseInt(timeoutValueSystemProperty);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found timeoutValueInSSLClosingHandshake in SSL system properties, " + this.timeoutValueInSSLClosingHandshake);
                    }
                }

                String protocolVersion = channelProps.getProperty(SSLChannelConstants.PROPNAME_PROTOCOL_VERSION);
                if (protocolVersion != null) {

                    if (SSLChannelConstants.PROTOCOL_VERSION_11.equalsIgnoreCase(protocolVersion)) {
                        this.useH2ProtocolAttribute = Boolean.FALSE;
                    } else if (SSLChannelConstants.PROTOCOL_VERSION_2.equalsIgnoreCase(protocolVersion)) {
                        this.useH2ProtocolAttribute = Boolean.TRUE;
                    }

                    if ((TraceComponent.isAnyTracingEnabled()) && (tc.isEventEnabled()) && useH2ProtocolAttribute != null) {
                        Tr.event(tc, "SSL Channel Config: versionProtocolOption has been set to " + protocolVersion.toLowerCase(Locale.ENGLISH));
                    }
                }

            }
        } catch (Exception e) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "init received exception handling properties; " + e);
            }
            throw new ChannelException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "jsseProvider=" + this.jsseProvider);
        }

        // Indicate that initialization is complete.
        this.isInitialized = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "init");
        }
    }

/*
 * @see com.ibm.wsspi.channelfw.Channel#destroy()
 */
    @Override
    public void destroy() throws ChannelException {
        if (null != this.myFactory && null != getConfig()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Removing channel from factory; " + getConfig().getName());
            }
            this.myFactory.removeChannel(getConfig().getName());
            this.myFactory = null;
        }
        // Null out instance variables.
        this.discProcess = null;
        this.sslConfig = null;
        this.sessionContext = null; // PK16095
    }

/*
 * @see com.ibm.wsspi.channelfw.Channel#getName()
 */
    @Override
    public String getName() {
        return this.sslConfig.getName();
    }

/*
 * @see com.ibm.wsspi.channelfw.Channel#getApplicationInterface()
 */
    @Override
    public Class<?> getApplicationInterface() {
        return TCPConnectionContext.class;
    }

/*
 * @see com.ibm.wsspi.channelfw.Channel#getDeviceInterface()
 */
    @Override
    public Class<?> getDeviceInterface() {
        return TCPConnectionContext.class;
    }

/*
 * @see com.ibm.wsspi.channelfw.Channel#update(com.ibm.websphere.channelfw.ChannelData)
 */
    @Override
    public void update(ChannelData inputData) {
        this.handshakeErrorTracker = createSSLHandshakeErrorTracker(inputData);
        this.sslConfig.updateChannelData(inputData);
    }

    /**
     * This method will call the ssl engine and attempt to unencrypt the data that
     * has arrived. To do this, an SSL engine will be created. When success results,
     * a reference to the SSL engine is placed in the virtual connection for use by
     * the connection link.
     *
     * @see com.ibm.wsspi.channelfw.Discriminator#discriminate(VirtualConnection, Object)
     */
    @Override
    public int discriminate(VirtualConnection vc, Object discrimData) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "discriminate");
        }

        // Check for null discrimData
        if (discrimData == null) {
            // Can't handle this condition.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Received null discrim data.  Returning NO from discriminator.");
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "discriminate");
            }
            return Discriminator.NO;
        }

        // Get data read in on device side. Note, it will always be an array of one element.
        WsByteBuffer netBuffer = ((WsByteBuffer[]) discrimData)[0];
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "netBuffer: " + SSLUtils.getBufferTraceInfo(netBuffer));
        }

        // Check for empty discrimData
        if (0 == netBuffer.position()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Received empty discrim data.  Returning MAYBE from discriminator.");
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "discriminate");
            }
            // No data to discriminate on.
            return Discriminator.MAYBE;
        }

        // Ensure initialize has taken place first.
        if (!this.isInitialized) {
            try {
                init();
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception caught while getting SSL connection link: " + e);
                }
                FFDCFilter.processException(e, getClass().getName(), "148", this, new Object[] { vc });
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "discriminate");
                }
                return Discriminator.NO;
            }
        }

        // Initial position of networkBuffer.
        int initialNetBufPosition = netBuffer.position();
        // Initial limit of networkBuffer.
        int initialNetBufLimit = netBuffer.limit();
        // Unencrypted buffer from the ssl engine output to be handed up to the application.
        WsByteBuffer decryptedNetBuffer = null;
        // State map from the VC.
        Map<Object, Object> stateMap = vc.getStateMap();
        // The SSL engine.
        SSLEngine sslEngine = null;
        // The SSL context for this connection.
        SSLContext vcSSLContext = null;
        // Result output from the SSL engine.
        SSLEngineResult sslResult = null;
        // Result from discrimination.
        int result = Discriminator.YES;
        // State saved from potentially former call to discriminator for this connection.
        SSLDiscriminatorState discState = null;

        try {
            // Prepare the networkBuffer for the call to unwrap. Align data between pos and lim.
            netBuffer.flip();
            // Determine if this connection has already been through discrimination once.
            discState = (SSLDiscriminatorState) stateMap.get(SSL_DISCRIMINATOR_STATE);
            if (discState == null) {
                // Create the sslContext based on the virtual connection.
                vcSSLContext = getSSLContextForInboundLink(null, vc);
                // This is the first call to discriminate. Build a new SSL engine for this connection.
                sslEngine = SSLUtils.getSSLEngine(vcSSLContext, FlowType.INBOUND,
                                                  (SSLLinkConfig) vc.getStateMap().get(SSLConnectionLink.LINKCONFIG),
                                                  (SSLConnectionLink) getConnectionLink(vc));
                // Line up all the buffers needed for a call to unwrap.
                decryptedNetBuffer = SSLUtils.allocateByteBuffer(sslEngine.getSession().getApplicationBufferSize(),
                                                                 getConfig().getDecryptBuffersDirect());
            } else {
                // This is NOT the first call to discriminate. Extract the ssl engine and context.
                sslEngine = discState.getEngine();
                vcSSLContext = discState.getSSLContext();
                // Extract output buffer used during discrimination.
                decryptedNetBuffer = discState.getDecryptedNetBuffer();
            }

            // Note: Can't call handshake code because we can't write to the client. Unwrap discrim data.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "before unwrap: \r\n\tnetBuf: " + SSLUtils.getBufferTraceInfo(netBuffer)
                             + "\r\n\tdecNetBuf: " + SSLUtils.getBufferTraceInfo(decryptedNetBuffer));
            }

            // Note, net and decNet buffers will always be one buffer for discrimination.
            // Protect JSSE from potential SSL packet sizes that are too big.
            int savedLimit = SSLUtils.adjustBufferForJSSE(netBuffer,
                                                          sslEngine.getSession().getPacketBufferSize());

            // Have the SSL engine inspect the first packet.
            sslResult = sslEngine.unwrap(netBuffer.getWrappedByteBuffer(),
                                         decryptedNetBuffer.getWrappedByteBuffer());
            // we should be in a handshake stage so I don't expect bytes to be
            // produced, but if there are, then the buffer needs to flip
            if (0 < sslResult.bytesProduced()) {
                decryptedNetBuffer.flip();
            }

            // If adjustments were made for the JSSE, restore them.
            if (-1 != savedLimit) {
                netBuffer.limit(savedLimit);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "after unwrap: \r\n\tnetBuf: " + SSLUtils.getBufferTraceInfo(netBuffer)
                             + "\r\n\tdecNetBuf: " + SSLUtils.getBufferTraceInfo(decryptedNetBuffer)
                             + "\r\n\tstatus=" + sslResult.getStatus()
                             + " HSstatus=" + sslResult.getHandshakeStatus()
                             + " consumed=" + sslResult.bytesConsumed()
                             + " produced=" + sslResult.bytesProduced());
            }

            if (sslResult.getStatus() == Status.BUFFER_UNDERFLOW) {
                // More data needed to make decision.
                result = Discriminator.MAYBE;
            } else {
                result = Discriminator.YES;
                if (netBuffer.remaining() == 0) {
                    netBuffer.clear();
                }
            }
        } catch (Throwable t) {
            // No FFDC needed. Some exceptions expected and handled elsewhere.
            // Input was not SSL.
            result = Discriminator.NO;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught Exception during discriminate: " + t);
            }
        }

        // Take appropriate action based on result to be returned to caller.
        switch (result) {
            case Discriminator.YES: {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Discriminator returning YES");
                }
                // Save discriminator state for the ready method.
                // Note, if this isn't the first call to discriminator for this connection, the SSL engine and
                // decrypted network buffer will already be there. For speed, use single method and all for
                // redundant setting of engine and buffer.
                // Save the position and limit of networkBuffer since we must return with an unmodified buffer.
                // Note, the networkBuffer is reset at the bottom of this method.
                if (discState == null) {
                    discState = new SSLDiscriminatorState();
                }
                discState.updateState(vcSSLContext, sslEngine, sslResult,
                                      decryptedNetBuffer, netBuffer.position(), netBuffer.limit());
                stateMap.put(SSL_DISCRIMINATOR_STATE, discState);
                break;
            }
            case Discriminator.NO: {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Discriminator returning NO");
                }
                // If this wasn't the first time discriminate was called, clean up the state map.
                if (discState != null) {
                    // Remove reference to discriminator state.
                    stateMap.remove(SSL_DISCRIMINATOR_STATE);
                }
                if (null != sslEngine) {
                    // PK13349 - close the discrimination engine
                    closeEngine(sslEngine);
                }
                if (null != decryptedNetBuffer) {
                    // Release the unwrap output buffer back to the pool.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Releasing decryptedNetworkBuffer");
                    }
                    decryptedNetBuffer.release();
                    decryptedNetBuffer = null;
                }
                break;
            }
            default: { // Discriminator.MAYBE
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Discriminator returning MAYBE");
                }
                // If this is the first time discrimination has run, save the SSL Engine and output buffer in the VC
                // Otherwise, the references are already there.
                if (discState == null) {
                    // Note no need to update position and limit since they weren't modified by unwrap.
                    discState = new SSLDiscriminatorState();
                    discState.updateState(vcSSLContext, sslEngine, sslResult,
                                          decryptedNetBuffer, netBuffer.position(), netBuffer.limit());
                    stateMap.put(SSL_DISCRIMINATOR_STATE, discState);
                }
                break;
            }
        }
        // Reset the position and limit of networkBuffer, required by discrimination process.
        netBuffer.limit(initialNetBufLimit);
        netBuffer.position(initialNetBufPosition);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "discriminate");
        }
        return result;
    }

    /**
     * This method will be called from the Channel Framework indicating that no
     * further calls will be made to the discriminator. All data stored or
     * allocated during previous discrimination calls on the input virtual
     * connection should be cleaned up.
     *
     * @param vc
     */
    @Override
    public void cleanUpState(VirtualConnection vc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "cleanUpState");
        }

        // Remove the discriminator state from the state map.
        SSLDiscriminatorState discState = (SSLDiscriminatorState) vc.getStateMap().remove(SSL_DISCRIMINATOR_STATE);
        // PK13349 - close the discrimination engine
        closeEngine(discState.getEngine());
        WsByteBuffer decryptedNetBuffer = discState.getDecryptedNetBuffer();
        // Release the decrypted network buffer back to the pool. This shouldn't
        // ever be null, but check anyhow.
        if (decryptedNetBuffer != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Releasing decryptedNetworkBuffer");
            }
            decryptedNetBuffer.release();
            decryptedNetBuffer = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "cleanUpState");
        }
    }

    /**
     * Utility method to be called when an SSL handshake has finished.
     *
     * @param engine
     */
    protected void onHandshakeFinish(SSLEngine engine) {
        // PK16095 - control the SSLSession cache inside the JSSE2 code

        // security is creating the contexts, which should not change but might.
        // we keep the last seen in an attempt to set these values only once
        // per context. The alternative is to keep a list of each unique one
        // seen but who knows how large that might grow to, so simply update
        // based on last seen.
        SSLSessionContext context = null;
        try {
            final SSLEngine localEngine = engine;
            context = AccessController.doPrivileged(new PrivilegedExceptionAction<SSLSessionContext>() {
                @Override
                public SSLSessionContext run() throws Exception {
                    return localEngine.getSession().getSessionContext();
                }
            });
        } catch (Exception e) {
            FFDCFilter.processException(e,
                                        getClass().getName() + ".onHandshakeFinish", "814");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception querying sessioncontext; " + e);
            }
            return;
        }
        if (null == context || context.equals(this.sessionContext)) {
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Channel [" + this + "] saving context: " + context);
        }
        this.sessionContext = context;
        context.setSessionCacheSize(getConfig().getSSLSessionCacheSize());
        context.setSessionTimeout(getConfig().getSSLSessionTimeout());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Session cache size set to " + context.getSessionCacheSize());
            Tr.debug(tc, "Session timeout set to " + context.getSessionTimeout());
        }
    }

    /**
     * Close the inbound and outbound sides of the engine created during the
     * discrimination path. This engine was not used during an actual connection,
     * only the discrimination, as such, it has no data to flush.
     *
     * @param engine
     */
    private void closeEngine(SSLEngine engine) {
        // PK13349 - close the engine we created during discrimination. We do not
        // need to flush any data however
        if (null != engine) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Closing discrimination engine");
            }
            engine.closeOutbound();
            if (!engine.isInboundDone()) {
                try {
                    engine.closeInbound();
                } catch (SSLException se) {
                    // no ffdc required
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Error closing inbound engine side; " + se);
                    }
                }
            }
        }
    }

/*
 * @see com.ibm.wsspi.channelfw.Discriminator#getDiscriminatoryDataType()
 */
    @Override
    public Class<?> getDiscriminatoryDataType() {
        return WsByteBuffer.class;
    }

/*
 * @see com.ibm.wsspi.channelfw.Discriminator#getChannel()
 */
    @Override
    public Channel getChannel() {
        return this;
    }

/*
 * @see com.ibm.wsspi.channelfw.Discriminator#getWeight()
 */
    @Override
    public int getWeight() {
        return this.sslConfig.getWeight();
    }

    /**
     * Access the configuration data associated with this SSL channel.
     *
     * @return SSLChannelData
     */
    public SSLChannelData getConfig() {
        return this.sslConfig;
    }

    /**
     * @return
     */
    public JSSEHelper getJsseHelper() {
        return this.jsseHelper;
    }

}
