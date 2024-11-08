/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.tls.impl;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.Map.Entry;

import javax.net.ssl.*;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.*;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.tls.NettyTlsProvider;
import io.netty.handler.ssl.SslContext;

/**
 * Creates {@link io.netty.handler.ssl.SslContext} objects via
 * {@link io.netty.handler.ssl.JdkSslContext.JdkSslContext} using active Liberty
 * SSL configurations
 * 
 * TODO: this logic should be made generic and put in a separate bundle so that
 * the SIP feature does not have a SSL runtime dependency
 * 
 * Adapted from
 * {@link com.ibm.ws.channel.ssl.internal.SSLChannel.getSSLContextForLink(VirtualConnection,
 * String, String, String, Boolean, SSLConnectionLink)}
 *
 */
@Component(configurationPid = "io.openliberty.netty.internal.tls", immediate = true, service = NettyTlsProvider.class)
public class NettyTlsProviderImpl implements NettyTlsProvider {

    private static final TraceComponent tc = Tr.register(NettyTlsProviderImpl.class);

    static final String ALIAS_KEY = "alias";
    static final String SSLPROP_CLIENT_AUTHENTICATION = "com.ibm.ssl.clientAuthentication";
    static final String SSLPROP_CLIENT_AUTHENTICATION_SUPPORTED = "com.ibm.ssl.clientAuthenticationSupported";

    
    /**
     * DS activate
     * 
     * @param ctx
     * @param reason
     */
    @Activate
    protected void activate(ComponentContext ctx){
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "NettyTlsProviderImpl activate " + this);
        }
    }
    
    /**
     * DS modified
     * 
     * @param ctx
     * @param reason
     */
    @Modified
    protected void modified(ComponentContext ctx){
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "NettyTlsProviderImpl modified " + this);
        }
    }


    /**
     * DS deactivate
     * 
     * @param ctx
     * @param reason
     */
    @Deactivate
    protected void deactivate(ComponentContext ctx, int reason){
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "NettyTlsProviderImpl deactivate " + this);
        }
    }

    /**
     * Build a {@link io.netty.handler.ssl.SslContext} for an outbound connection
     * 
     * @param Map<String, Object> sslOptions
     * @param String host
     * @param String port
     * @return SslContext
     */
    public SslContext getOutboundSSLContext(Map<String, Object> sslOptions, String host,
            String port) {

        SSLContext jdkContext;
        SSLConfig sslConfig;
        String[] protocols;
        try {
        	Properties props = createProps(sslOptions);
        	String alias = (String)props.getProperty(ALIAS_KEY);
            AbstractMap.SimpleEntry<SSLContext, SSLConfig> sslContextConfigPair = getSSLContextAndConfig(alias, props, true, host, port, port, false);
            if (sslContextConfigPair == null){ 
                if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                    Tr.warning(tc, "getInboundALPNSSLContext exception caught creating SSLContext");
                }
                return null;
            }
            jdkContext = sslContextConfigPair.getKey();
            sslConfig = sslContextConfigPair.getValue();
            String protocol = sslConfig.getProperty(Constants.SSLPROP_PROTOCOL);
            protocols = getSSLProtocol(protocol);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getOutboundSSLContext SSLContext:", jdkContext);
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "getOutboundSSLContext exception caught creating SSLContext: " + e);
            }
            return null;
        }

        try {
            SslContext nettyContext = new JdkSslContext(jdkContext, true, Arrays.asList(jdkContext.getDefaultSSLParameters().getCipherSuites()), SupportedCipherSuiteFilter.INSTANCE,
                    null, getClientAuth(sslConfig), protocols, false);
            return nettyContext;
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "getOutboundSSLContext exception caught creating JdkSslContext: " + e);
            }
            return null;
        }
    }

    /**
     * Build a {@link io.netty.handler.ssl.SslContext} for an inbound connection
     * 
     * @param Map<String, Object> sslOptions
     * @param String host
     * @param String port
     * @return SslContext
     */
    public SslContext getInboundSSLContext(Map<String, Object> sslOptions, String host, String port) {

        SSLContext jdkContext;
        SSLConfig sslConfig;
        String[] protocols;
        try {
        	Properties props = createProps(sslOptions);
        	String alias = (String)props.getProperty(ALIAS_KEY);
            AbstractMap.SimpleEntry<SSLContext, SSLConfig> sslContextConfigPair = getSSLContextAndConfig(alias, props, true, host, port, port, false);
            if (sslContextConfigPair == null){ 
                if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                    Tr.warning(tc, "getInboundALPNSSLContext exception caught creating SSLContext");
                }
                return null;
            }
            jdkContext = sslContextConfigPair.getKey();
            sslConfig = sslContextConfigPair.getValue();
            String protocol = sslConfig.getProperty(Constants.SSLPROP_PROTOCOL);
            protocols = getSSLProtocol(protocol);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "getInboundSSLContext exception caught creating SSLContext: " + e);
            }
            return null;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getInboundSSLContext SSLContext: " + jdkContext);
        }
        try {
            SslContext nettyContext = new JdkSslContext(jdkContext, false, Arrays.asList(jdkContext.getDefaultSSLParameters().getCipherSuites()), SupportedCipherSuiteFilter.INSTANCE,
                    null, getClientAuth(sslConfig), protocols, false);
            return nettyContext;
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "getInboundSSLContext exception caught creating JdkSslContext: " + e);
            }
            return null;
        }
    }
    
    /**
     * Build a {@link io.netty.handler.ssl.SslContext} for an H2 Inbound connection
     * with ALPN for H1 and H2
     * 
     * @param Map<String, Object> sslOptions
     * @param String host
     * @param String port
     * @return SslContext
     */
    public SslContext getInboundALPNSSLContext(Map<String, Object> sslOptions, String host, String port) {

        SSLContext jdkContext;
        SSLConfig sslConfig;
        String[] protocols;
        try {
        	Properties props = createProps(sslOptions);
        	String alias = (String)props.getProperty(ALIAS_KEY);
            AbstractMap.SimpleEntry<SSLContext, SSLConfig> sslContextConfigPair = getSSLContextAndConfig(alias, props, true, host, port, port, false);
            if (sslContextConfigPair == null){ 
                if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                    Tr.warning(tc, "getInboundALPNSSLContext exception caught creating SSLContext");
                }
                return null;
            }
            jdkContext = sslContextConfigPair.getKey();
            sslConfig = sslContextConfigPair.getValue();
            String protocol = sslConfig.getProperty(Constants.SSLPROP_PROTOCOL);
            protocols = getSSLProtocol(protocol);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "getInboundALPNSSLContext exception caught creating SSLContext: " + e);
            }
            return null;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getInboundALPNSSLContext SSLContext: " + jdkContext);
        }
        try {
            ApplicationProtocolConfig apn = new ApplicationProtocolConfig(Protocol.ALPN,
                    // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
                    SelectorFailureBehavior.NO_ADVERTISE,
                    // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                    SelectedListenerFailureBehavior.ACCEPT,
                    // Add Supported Protocols here for negotiation
                    ApplicationProtocolNames.HTTP_2,
                    ApplicationProtocolNames.HTTP_1_1);
            SslContext nettyContext = new JdkSslContext(jdkContext, false, Arrays.asList(jdkContext.getDefaultSSLParameters().getCipherSuites()), SupportedCipherSuiteFilter.INSTANCE,
            apn, getClientAuth(sslConfig), protocols, false);
            return nettyContext;
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "getInboundALPNSSLContext exception caught creating JdkSslContext: " + e);
            }
            return null;
        }
    }
    
    private String[] getSSLProtocol(String protocol) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getSSLProtocol");
        }

        // protocol(s) need to be in an array
        String[] protocols = protocol.split(",");

        // we only want to set the protocol on the socket if it a specific protocol name
        // don't set to TLS or SSL
        if (protocols.length == 1) {
            if (protocols[0].equals(Constants.PROTOCOL_TLS) || protocols[0].equals(Constants.PROTOCOL_SSL)) {
                protocols = null;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getSSLProtocol " + protocols);
        }
        return protocols;
    }
    
    private ClientAuth getClientAuth(SSLConfig config) {
    	// TODO Besides client auth we should also check cipher suite order with Constants.SSLPROP_ENFORCE_CIPHER_ORDER
    	boolean clientAuth = getBooleanProperty(SSLPROP_CLIENT_AUTHENTICATION, config);
    	if(clientAuth)
    		return ClientAuth.REQUIRE;
    	else if(getBooleanProperty(SSLPROP_CLIENT_AUTHENTICATION_SUPPORTED, config))
    		return ClientAuth.OPTIONAL;
    	return ClientAuth.NONE;
    }
    
    /**
     * Query the boolean property value of the input name.
     *
     * @param propertyName
     * @return boolean
     */
    private boolean getBooleanProperty(String propertyName, Properties sslConfig) {
    	boolean booleanValue = false;

    	// Pull the key from the property map
    	Object objectValue = sslConfig.get(propertyName);
    	if (objectValue != null) {
    		if (objectValue instanceof Boolean) {
    			booleanValue = ((Boolean) objectValue).booleanValue();
    		} else if (objectValue instanceof String) {
    			booleanValue = Boolean.parseBoolean((String) objectValue);
    		}
    	}
    	return booleanValue;
    }

    /**
     * Adapted from
     * {@link com.ibm.ws.channel.ssl.internal.SSLChannel.getSSLContextForLink(VirtualConnection,
     * String, String, String, Boolean, SSLConnectionLink)}
     * 
     * @param alias
     * @param properties
     * @param isInbound
     * @param host
     * @param port
     * @param endPoint
     * @param isZWebContainerChain
     * @return
     * @throws Exception
     */
    @FFDCIgnore({Exception.class})
    private static AbstractMap.SimpleEntry<SSLContext, SSLConfig> getSSLContextAndConfig(String alias, Properties properties, boolean isInbound, String host,
            String port, String endPoint, Boolean isZWebContainerChain) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "host=" + host + " port=" + port + " endPoint=" + endPoint);
        }

        // Set up the parameters needed to call into the JSSEHelper to extract official
        // SSL Props.
        String direction = (isInbound) ? Constants.DIRECTION_INBOUND : Constants.DIRECTION_OUTBOUND;
        final Map<String, Object> connectionInfo = new HashMap<String, Object>();
        connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, direction);
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_HOST, host);
        connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_PORT, port);
        connectionInfo.put(Constants.CONNECTION_INFO_ENDPOINT_NAME, endPoint);
        // PK40641 - handle channel props that do not need JSSEHelper
        Properties props = null;
        boolean useJSSEHelper = (null != alias);
        if (!useJSSEHelper) {
            // 436920 - check for ssl properties put programmatically on the thread.
            // Calling into JSSEHelper.getProperties() does this same action
            try {
                props = AccessController.doPrivileged(new PrivilegedExceptionAction<Properties>() {
                    @Override
                    public Properties run() throws Exception {
                        return com.ibm.websphere.ssl.JSSEHelper.getInstance().getSSLPropertiesOnThread();
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
                useJSSEHelper = !properties.containsKey(Constants.SSLPROP_KEY_STORE)
                        || !properties.containsKey(Constants.SSLPROP_TRUST_STORE);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found on-thread ssl properties");
                }
            }
        }
        if (useJSSEHelper) {
            try {
                // Extract the official SSL props based on config information.
                final String aliasFinal = alias;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Querying security service for alias=[" + aliasFinal + "]");
                }
                props = AccessController.doPrivileged(new PrivilegedExceptionAction<Properties>() {

                    @Override
                    public Properties run() throws Exception {
                        return com.ibm.websphere.ssl.JSSEHelper.getInstance().getProperties(aliasFinal, connectionInfo,
                                null);
                    }
                });
            } catch (

            Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception getting SSL properties from alias: " + alias);
                }
                throw e;
            }
        }

        // at this point, we have a set of properties to use, which might be
        // repertoire based or on-thread config... merge the channel props into
        // them without overwriting any
        if (null != props) {
            Enumeration<?> names = properties.propertyNames();
            String key = null;
            String value = null;
            while (names.hasMoreElements()) {
                key = (String) names.nextElement();
                value = properties.getProperty(key);
                if (value instanceof String && null != value && !props.containsKey(key)) {
                    props.put(key, value);
                }
            }
        } else {
            // otherwise we just use the channel config
            props = properties;
        }

        // "SSSL" is a zOS repertoire type that is not supported by SSLChannel
        // We only support "JSSE"
        String sslType = (String) props.get(Constants.SSLPROP_SSLTYPE);
        if (null != sslType) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "SSLConfig type: " + sslType);
            }
            if (sslType.equals("SSSL")) {
                throw new Exception("Invalid SSLConfig type: " + sslType);
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
        SSLConfig config =  null;
        try {
            config = new SSLConfig(props);
            context = com.ibm.websphere.ssl.JSSEHelper.getInstance().getSSLContext(connectionInfo, config);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception getting SSLContext from properties.", new Object[] { e });
            }
            return null;
        }
        return new AbstractMap.SimpleEntry<SSLContext, SSLConfig>(context, config);
    }

    private static Properties createProps(Map<String, Object> map) {
        Properties properties = new Properties();
        if (map != null) {
            for (Entry<String, Object> entry : map.entrySet()) {
                properties.put(entry.getKey(), entry.getValue());
            }
        }
        return properties;
    }
}
