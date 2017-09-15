/*******************************************************************************
 * Copyright (c) 2005, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.ssl;

import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WebSphereRuntimePermission;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ssl.JSSEProviderFactory;
import com.ibm.ws.ssl.config.SSLConfigManager;
import com.ibm.ws.ssl.config.ThreadManager;
import com.ibm.ws.ssl.core.TraceNLSHelper;

/**
 * <p>
 * This class is for components and applications to utilize the SSL configuration
 * framework for selecting SSL configurations and turning them into SSL objects
 * such as SSLContext, Properties, URLStreamHandlers, and SocketFactories.
 * </p>
 *
 * @author IBM Corporation
 * @version 1.0
 * @since WAS 6.1
 * @ibm-api
 **/

public class JSSEHelper {
    private final static TraceComponent tc = Tr.register(JSSEHelper.class, "SSL", "com.ibm.ws.ssl.resources.ssl");

    /**
     * Permission required to retrieve SSL information and objects from the runtime
     */
    private final static WebSphereRuntimePermission GET_SSLCONFIG = new WebSphereRuntimePermission("getSSLConfig");

    /**
     * Permission required to set SSL properties and information to the runtime
     */
    private final static WebSphereRuntimePermission SET_SSLCONFIG = new WebSphereRuntimePermission("setSSLConfig");

    /**
     * <p>
     * Variable used when the direction of the SSLContext is inbound. This
     * is associated to receiving requests or server-side sockets, etc. This
     * helps with validation of the required SSL attributes.
     * </p>
     **/
    public static final String DIRECTION_INBOUND = Constants.DIRECTION_INBOUND;

    /**
     * <p>
     * Variable used when the direction of the SSLContext is outbound. This
     * is associated to sending requests or client-side sockets, etc. This
     * helps with validation of the required SSL attributes.
     * </p>
     **/
    public static final String DIRECTION_OUTBOUND = Constants.DIRECTION_OUTBOUND;

    /**
     * <p>
     * Variable used when the direction of the SSLContext is not currently known.
     * This will require that a TrustStore and KeyStore are both specified.
     * </p>
     **/
    public static final String DIRECTION_UNKNOWN = Constants.DIRECTION_UNKNOWN;

    /**
     * <p>
     * EndPoint name when using IIOP protocol for outbound connections. The IIOP
     * endpoint attribute is not used in Liberty. This attribute is available for
     * compatibility purposes only.
     * </p>
     **/
    public static final String ENDPOINT_IIOP = Constants.ENDPOINT_IIOP;

    /**
     * <p>
     * EndPoint name when using HTTP protocol for outbound connections. The HTTP
     * endpoint attribute is not used Liberty. This attribute is available for
     * compatibility purposes only.
     * </p>
     **/
    public static final String ENDPOINT_HTTP = Constants.ENDPOINT_HTTP;

    /**
     * <p>
     * EndPoint name when using SIP protocol for outbound connections. The SIP
     * endpoint attribute is not used Liberty. This attribute is available for
     * compatibility purposes only.
     * </p>
     **/
    public static final String ENDPOINT_SIP = Constants.ENDPOINT_SIP;

    /**
     * <p>
     * EndPoint name when using JMS protocol for outbound connections. The JMS
     * endpoint attribute is not used in Liberty. This attribute is available for
     * compatibility purposes only.
     * </p>
     **/
    public static final String ENDPOINT_JMS = Constants.ENDPOINT_JMS;

    /**
     * <p>
     * EndPoint name when using BUS_CLIENT protocol for outbound connections. You cannot
     * use the BUS_CLIENT endpoint attribute in Liberty. This attribute is available
     * for compatibility purposes only.
     * </p>
     **/
    public static final String ENDPOINT_BUS_CLIENT = Constants.ENDPOINT_BUS_CLIENT;

    /**
     * <p>
     * EndPoint name when using ENDPOINT_BUS_TO_WEBSPHERE_MQ protocol for outbound connections.
     * The ENDPOINT_BUS_TO_WEBSPHERE_MQ endpoint attribute is not in Liberty. This
     * attribute is available for compatibility purposes only.
     * </p>
     **/
    public static final String ENDPOINT_BUS_TO_WEBSPHERE_MQ = Constants.ENDPOINT_BUS_TO_WEBSPHERE_MQ;

    /**
     * <p>
     * EndPoint name when using ENDPOINT_BUS_TO_BUS protocol for outbound connections. The
     * ENDPOINT_BUS_TO_BUS endpoint attribute is not used in Liberty. This attribute is
     * available for compatibility purposes only.
     * </p>
     **/
    public static final String ENDPOINT_BUS_TO_BUS = Constants.ENDPOINT_BUS_TO_BUS;

    /**
     * <p>
     * EndPoint name when using ENDPOINT_CLIENT_TO_WEBSPHERE_MQ protocol for outbound connections.
     * The ENDPOINT_CLIENT_TO_WEBSPHERE_MQ endpoint attribute is not used in Liberty. This
     * attribute is available for compatibility purposes only.
     * </p>
     **/
    public static final String ENDPOINT_CLIENT_TO_WEBSPHERE_MQ = Constants.ENDPOINT_CLIENT_TO_WEBSPHERE_MQ;

    /**
     * <p>
     * EndPoint name when using LDAP (JNDI) protocol for outbound connections. The LDAP
     * endpoint attribute is not used in Liberty. This attribute is available for compatibility
     * purposes only.
     * </p>
     **/
    public static final String ENDPOINT_LDAP = Constants.ENDPOINT_LDAP;

    /**
     * <p>
     * EndPoint name when using SOAP protocol from the SOAP connector for outbound connections.
     * The SOAP endpoint attribute is not used in Liberty. This attribute is available for
     * compatibility purposes only.
     * </p>
     **/
    public static final String ENDPOINT_ADMIN_SOAP = Constants.ENDPOINT_ADMIN_SOAP;

    /**
     * <p>
     * EndPoint name when using IPC protocol from the IPC connector for outbound connections.
     * The IPC endpoint attribute is not used in Liberty. This attribute is available for
     * compatibility purposes only.
     * </p>
     **/
    public static final String ENDPOINT_ADMIN_IPC = Constants.ENDPOINT_ADMIN_IPC;

    /**
     * <p>
     * Variable used for the connection information to determine SSLContext validation rules.
     * Connection information mapping is not used in Liberty. This variable is available
     * for compatibility purposes only.
     * </p>
     **/
    public static final String CONNECTION_INFO_DIRECTION = Constants.CONNECTION_INFO_DIRECTION;

    /**
     * <p>
     * Property used in the connection information Map to define the endpoint. Connection
     * information mapping is not used in Liberty. This property is available for
     * compatibility purposes only.
     * </p>
     **/
    public static final String CONNECTION_INFO_ENDPOINT_NAME = Constants.CONNECTION_INFO_ENDPOINT_NAME;

    /**
     * <p>
     * Property used in the connection information Map to define the remote host which is
     * being connected to outbound connections. Connection information mapping is not
     * used in Liberty. This property is available for compatibility purposes only.
     * </p>
     **/
    public final static String CONNECTION_INFO_REMOTE_HOST = Constants.CONNECTION_INFO_REMOTE_HOST;

    /**
     * <p>
     * Property used in the connection information Map to define the remote port which is
     * being connected to outbound connections. Connection information mapping is not
     * used in Liberty. This property is available for compatibility purposes only.
     * </p>
     **/
    public final static String CONNECTION_INFO_REMOTE_PORT = Constants.CONNECTION_INFO_REMOTE_PORT;

    /**
     * <p>
     * Property used in the connection information Map to define the host which is being
     * connected to outbound connections. Connection information mapping is not used
     * in Liberty. This property is available for compatibility purposes only.
     * </p>
     **/
    public final static String CONNECTION_INFO_CERT_MAPPING_HOST = "com.ibm.ssl.certMappingHost";

    /**
     * <p>
     * Property used to determine if the connection is a Web Container
     * inbound connection.
     * </p>
     **/
    public final static String CONNECTION_INFO_IS_WEB_CONTAINER_INBOUND = "com.ibm.ssl.isWebContainerInbound";

    private static final class JSSESingleton {
        private final static JSSEHelper instance;
        static {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Creating new instance of JSSEHelper.");
            instance = new JSSEHelper();
        }
    }

    /**
     * Constructor.
     */
    public JSSEHelper() {
        // do nothing
    }

    /**
     * <p>
     * This method returns an instance of the JSSEHelper class. This is the
     * proper way to get a reference of this API class.
     * </p>
     *
     * @return JSSEHelper
     * @ibm-api
     **/
    public static JSSEHelper getInstance() {
        return JSSESingleton.instance;
    }

    /**
     * <p>
     * This has the highest precedence in terms of selection rules. When the SSL
     * runtime finds SSL properties on the thread, this should be used before
     * anything else in the selection process. Using SSL properties from the thread
     * is not support in the Liberty profile. This method exists for compatibility purposes.
     * </p>
     * <p>
     * It's important to clear the thread after use, especially where thread
     * pools are used. It is not cleared up automatically. Pass in "null" to
     * this API to clear it.
     * </p>
     * <p>
     * When Java 2 Security is enabled, access to call this method requires
     * WebSphereRuntimePermission "setSSLConfig" to be granted.
     * </p>
     *
     * @param props
     * @ibm-api
     **/
    public void setSSLPropertiesOnThread(Properties props) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            String debug = "Clearing thread properties.";
            if (props != null) {
                debug = props.getProperty(Constants.SSLPROP_ALIAS);
                if (null == debug) {
                    debug = props.toString();
                }
            }
            Tr.entry(tc, "setSSLPropertiesOnThread", new Object[] { debug });
        }

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Performing Java 2 Security Permission Check ...");
                Tr.debug(tc, "Expecting : " + SET_SSLCONFIG.toString());
            }
            sm.checkPermission(SET_SSLCONFIG);
        }

        if (props != null) {
            String alias = props.getProperty(Constants.SSLPROP_ALIAS);

            if (alias != null) {
                SSLConfig config = SSLConfigManager.getInstance().getSSLConfig(alias);

                // add the properties to the Map so they can be used if not already there.
                if (config == null) {
                    config = new SSLConfig(props);

                    try {
                        SSLConfigManager.getInstance().addSSLConfigToMap(alias, config);
                    } catch (Exception e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            Tr.exit(tc, "The following exception occurred in setSSLPropertiesOnThread().", new Object[] { e });
                        FFDCFilter.processException(e, getClass().getName(), "setSSLPropertiesOnThread", this);
                    }
                }
            }
        }

        ThreadManager.getInstance().setPropertiesOnThread(props);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "setSSLPropertiesOnThread");
    }

    /**
     * <p>
     * This method allows the retrieving of SSL properties on the thread of execution.
     * This can be used for verification purposes or to communicate SSL properties
     * among components running on the same thread. Getting SSL properties form the
     * thread is not used in Liberty. This method exits for compatibility purposes
     * only.
     * </p>
     * <p>
     * When Java 2 Security is enabled, access to call this method requires
     * WebSphereRuntimePermission "getSSLConfig" to be granted.
     * </p>
     *
     * @return Properties
     * @ibm-api
     **/
    public Properties getSSLPropertiesOnThread() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getSSLPropertiesOnThread");

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Performing Java 2 Security Permission Check ...");
                Tr.debug(tc, "Expecting : " + GET_SSLCONFIG.toString());
            }
            sm.checkPermission(GET_SSLCONFIG);
        }

        Properties props = ThreadManager.getInstance().getPropertiesOnThread();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            String debug = "Thread properties are NULL.";
            if (props != null) {
                debug = props.getProperty(Constants.SSLPROP_ALIAS);
                if (null == debug) {
                    debug = props.toString();
                }
            }
            Tr.exit(tc, "getSSLPropertiesOnThread", new Object[] { debug });
        }

        return props;
    }

    /**
     * <p>
     * This method returns the SSL properties given a specific SSL configuration
     * alias.
     * </p>
     * <p>
     * When Java 2 Security is enabled, access to call this method requires
     * WebSphereRuntimePermission "getSSLConfig" to be granted.
     * </p>
     *
     * @param sslAliasName - Name of the SSL configuration to get properties for.
     * @return Properties
     * @throws com.ibm.websphere.ssl.SSLException
     *
     * @ibm-api
     **/
    public Properties getProperties(String sslAliasName) throws SSLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getProperties", new Object[] { sslAliasName });

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Performing Java 2 Security Permission Check ...");
                Tr.debug(tc, "Expecting : " + GET_SSLCONFIG.toString());
            }
            sm.checkPermission(GET_SSLCONFIG);
        }

        try {
            // direct selection
            if (sslAliasName != null && sslAliasName.length() > 0) {
                Properties directSelectionProperties = SSLConfigManager.getInstance().getProperties(sslAliasName);

                if (directSelectionProperties != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.exit(tc, "getProperties -> direct");
                    return directSelectionProperties;
                }
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "getProperties", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "The following exception occurred in getProperties().", new Object[] { e });
            throw asSSLException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getProperties -> null");
        return null;
    }

    /**
     * <p>
     * This method creates an SSLContext given the SSL properties needed to create
     * the SSLContext. The properties can be retrieved from the SSL configuration
     * using the getProperties API in this class.
     * </p>
     *
     * @param connectionInfo - contains information about the connection direction, host, port, etc.
     * @param props - the SSL properties
     * @return SSLContext
     * @throws com.ibm.websphere.ssl.SSLException
     * @ibm-api
     **/
    public SSLContext getSSLContext(Map<String, Object> connectionInfo, Properties props) throws SSLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getSSLContext", new Object[] { connectionInfo });

        if (props != null) {
            SSLConfig newConfig = new SSLConfig(props);
            String contextProvider = props.getProperty(Constants.SSLPROP_CONTEXT_PROVIDER);
            SSLContext context = null;
            try {
                context = JSSEProviderFactory.getInstance(contextProvider).getSSLContext(connectionInfo, newConfig);
            } catch (Exception e) {
                FFDCFilter.processException(e, getClass().getName(), "getSSLContext", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "The following exception occurred getting the SSLContext.", new Object[] { e });
                throw asSSLException(e);
            }

            if (context == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "The SSLContext is null.  Throwing exception.");
                throw new SSLException("The SSLContext returned is null.  Validate the Properties passed in.");
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getSSLContext");
            return context;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "SSL client config properties are missing. The property 'com.ibm.SSL.ConfigURL' may not be set properly.");

        String message = TraceNLSHelper.getInstance().getString("ssl.no.properties.error.CWPKI0315E",
                                                                "SSL configuration properites are null. Could be a problem parsing the SSL client configuraton.");

        throw new SSLException(message);
    }

    /**
     * <p>
     * This method creates a URLStreamHandler specific SSL properties. The
     * URLStreamHandler is used for outbound URL connections.
     * </p>
     *
     * @param props - the SSL properties (connectionInfo derived from URL)
     * @return URLStreamHandler
     * @throws com.ibm.websphere.ssl.SSLException
     * @ibm-api
     **/
    public URLStreamHandler getURLStreamHandler(Properties props) throws SSLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getURLStreamHandler");

        try {
            SSLConfig newConfig = new SSLConfig(props);
            String contextProvider = newConfig.getProperty(Constants.SSLPROP_CONTEXT_PROVIDER);

            URLStreamHandler rc = JSSEProviderFactory.getInstance(contextProvider).getURLStreamHandler(newConfig);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getURLStreamHandler: " + rc);
            return rc;
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "getURLStreamHandler", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "The following exception occurred getting the SSLContext.", new Object[] { e });
            throw asSSLException(e);
        }
    }

    /**
     * <p>
     * This method creates an SSLServerSocketFactory given the SSL configuration
     * properties specified. The properties can be retrieved from the SSL
     * configuration using the getProperties API in this class.
     * </p>
     *
     * @param props
     * @return SSLServerSocketFactory
     * @throws com.ibm.websphere.ssl.SSLException
     * @ibm-api
     **/
    public SSLServerSocketFactory getSSLServerSocketFactory(Properties props) throws SSLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getSSLServerSocketFactory");

        try {
            SSLConfig newConfig = new SSLConfig(props);
            String contextProvider = newConfig.getProperty(Constants.SSLPROP_CONTEXT_PROVIDER);

            SSLServerSocketFactory rc = JSSEProviderFactory.getInstance(contextProvider).getSSLServerSocketFactory(newConfig);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getSSLServerSocketFactory: " + rc);
            return rc;
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "getSSLServerSocketFactory", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "The following exception occurred in getSSLServerSocketFactory().", new Object[] { e });
            throw asSSLException(e);
        }
    }

    /**
     * <p>
     * This method creates an SSLContext based on the SSL properties specified.
     * The properties can be retrieved from the SSL configuration using the
     * getProperties API in this class.
     *
     * </p>
     *
     * @param sslAliasName - Used in direct selection. The alias name of a
     *            specific SSL configuration (optional). You can pass in "null" here.
     *            If sslAliasName is provided but does not exist it will check
     *            connection information for a match. Then look for a default if no
     *            match with the connection information.
     * @param connectionInfo - This refers to the remote connection information. The
     *            current properties known by the runtime include:
     *            <p>
     *            Example OUTBOUND case (endpoint refers more to protocol used since
     *            outbound names are not well-known):
     *            <ul>
     *            <li>com.ibm.ssl.remoteHost="hostname.ibm.com"</li>
     *            <li>com.ibm.ssl.remotePort="9809"</li>
     *            <li>com.ibm.ssl.direction="outbound"</li>
     *            </ul></p>
     *            <p>
     *            Example INBOUND case (endpoint name matches serverindex endpoint):
     *            <code>
     *            com.ibm.ssl.direction="inbound"
     *            </code></p>
     *            It's highly recommended to supply these properties when possible.
     * @return SSLSocketFactory
     * @throws com.ibm.websphere.ssl.SSLException
     * @ibm-api
     **/
    public SSLSocketFactory getSSLSocketFactory(Map<String, Object> connectionInfo, Properties props) throws SSLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getSSLSocketFactory");

        try {
            SSLConfig newConfig = null;
            String contextProvider = null;
            if (props != null && !props.isEmpty()) {
                newConfig = new SSLConfig(props);
                contextProvider = newConfig.getProperty(Constants.SSLPROP_CONTEXT_PROVIDER);
            } else {
                if (connectionInfo == null || connectionInfo.isEmpty()) {
                    connectionInfo = new HashMap<String, Object>();
                    connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
                    if (newConfig == null) {
                        Properties sslProps = getProperties(null, connectionInfo, null, true);
                        newConfig = new SSLConfig(sslProps);
                        contextProvider = sslProps.getProperty(Constants.SSLPROP_CONTEXT_PROVIDER);
                    }
                }
            }

            SSLSocketFactory rc = JSSEProviderFactory.getInstance(contextProvider).getSSLSocketFactory(connectionInfo, newConfig);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getSSLSocketFactory: " + rc);
            return rc;
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "getSSLSocketFactory", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "The following exception occurred in getSSLSocketFactory().", new Object[] { e });
            throw asSSLException(e);
        }
    }

    /**
     * <p>
     * This method creates an SSLContext for use by an SSL application or component.
     * Precedence logic will determine which parameters are used for creating the
     * SSLContext. The selection precedence rules are:
     * </p>
     *
     * <ol>
     * <li> Direct - The sslAliasName parameter, when specified, will be used to choose
     * the alias directly from the SSL configurations.</li>
     *
     * <li> Dynamic - The remoteHost/remotePort String(s) will contain the target
     * host, or host and port. A SSL configuration to be use for an outbound connection
     * will be selected based on the host or host and port configured.</li>
     * </ol>
     *
     *
     * <p>
     * When Java 2 Security is enabled, access to call this method requires
     * WebSphereRuntimePermission "getSSLConfig" to be granted.
     * </p>
     *
     * @param sslAliasName - Used in direct selection. The alias name of a
     *            specific SSL configuration (optional). You can pass in "null" here.
     *            If sslAliasName is provided but does not exist it will check
     *            connection information for a match. Then look for a default if no
     *            match with the connection information.
     * @param connectionInfo - This refers to the remote connection information. The
     *            current properties known by the runtime include:
     *            <p>
     *            Example OUTBOUND case (endpoint refers more to protocol used since
     *            outbound names are not well-known):
     *            <ul>
     *            <li>com.ibm.ssl.remoteHost="hostname.ibm.com"</li>
     *            <li>com.ibm.ssl.remotePort="9809"</li>
     *            <li>com.ibm.ssl.direction="outbound"</li>
     *            </ul></p>
     *            <p>
     *            Example INBOUND case (endpoint name matches serverindex endpoint):
     *            <code>
     *            com.ibm.ssl.direction="inbound"
     *            </code></p>
     *            It's highly recommended to supply these properties when possible.
     * @param listener - This is used to notify the
     *            caller of this API that the SSL configuration changed in the runtime.
     *            It's up to the caller to decide if they want to call this API again
     *            to get the new SSLContext for the configuration. Passing in NULL
     *            indicates no notification is desired. See the
     *            com.ibm.websphere.ssl.SSLConfigChangeListener interface for more
     *            information.
     * @return SSLContext
     * @throws com.ibm.websphere.ssl.SSLException
     * @ibm-api
     **/
    public SSLContext getSSLContext(String sslAliasName, Map<String, Object> connectionInfo, SSLConfigChangeListener listener) throws SSLException {
        return getSSLContext(sslAliasName, connectionInfo, listener, true);
    }

    /**
     * Like {@link #getSSLContext(String, Map, SSLConfigChangeListener)},
     * failing over to the default configuration is a choice.
     *
     * @param sslAliasName - Used in direct selection. The alias name of a
     *            specific SSL configuration (optional). You can pass in "null" here.
     * @param connectionInfo - This refers to the remote connection information. The
     *            current properties known by the runtime include:
     *            <p>
     *            Example OUTBOUND case (endpoint refers more to protocol used since
     *            outbound names are not well-known):
     *            <ul>
     *            <li>com.ibm.ssl.remoteHost="hostname.ibm.com"</li>
     *            <li>com.ibm.ssl.remotePort="9809"</li>
     *            <li>com.ibm.ssl.direction="outbound"</li>
     *            </ul></p>
     *            <p>
     *            Example INBOUND case (endpoint name matches serverindex endpoint):
     *            <code>
     *            com.ibm.ssl.direction="inbound"
     *            </code></p>
     *            It's highly recommended to supply these properties when possible.
     * @param listener - This is used to notify the
     *            caller of this API that the SSL configuration changed in the runtime.
     *            It's up to the caller to decide if they want to call this API again
     *            to get the new SSLContext for the configuration. Passing in NULL
     *            indicates no notification is desired. See the
     *            com.ibm.websphere.ssl.SSLConfigChangeListener interface for more
     *            information.
     * @param tryDefault if the specified alias is not available, {@code true} indicates the default configuration should be tried.
     * @return
     * @throws SSLException
     */
    public SSLContext getSSLContext(String sslAliasName, Map<String, Object> connectionInfo,
                                    SSLConfigChangeListener listener, boolean tryDefault) throws SSLException, SSLConfigurationNotAvailableException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getSSLContext", new Object[] { sslAliasName, connectionInfo, listener });

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Performing Java 2 Security Permission Check ...");
                Tr.debug(tc, "Expecting : " + GET_SSLCONFIG.toString());
            }
            sm.checkPermission(GET_SSLCONFIG);
        }

        try {
            SSLConfig props = (SSLConfig) getProperties(sslAliasName, connectionInfo, listener, tryDefault);

            if (props != null) {
                String contextProvider = props.getProperty(Constants.SSLPROP_CONTEXT_PROVIDER);
                SSLContext context = JSSEProviderFactory.getInstance(contextProvider).getSSLContext(connectionInfo, props);

                if (context != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.exit(tc, "getSSLContext");
                    return context;
                }
                throw new SSLException("SSLContext could not be created from specified SSL properties.");
            }
            // If we get here, the requested configuration is not available.
            // Either:
            // 1. The config is NOT valid and will never be there (not defined)
            // 2. The config has not been processed YET
            // We guard on tryDefault here to ensure we don't regress existing behaviour
            if (tryDefault) {
                throw new SSLException("SSLContext could not be created due to null SSL properties.");
            } else {
                throw new SSLConfigurationNotAvailableException("SSLContext could not be created for alias '" + sslAliasName + "', the configuration is not present.");
            }
        } catch (SSLConfigurationNotAvailableException e) {
            throw e;
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "getSSLContext (2)", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "The following exception occurred in getSSLContext().", new Object[] { e });
            throw asSSLException(e);
        }
    }

    /**
     * <p>
     * This method creates a URLStreamHandler for use by an SSL application or component.
     * Precedence logic will determine which parameters are used for creating the
     * URLStreamHandler. See the JavaDoc for getSSLContext with the same parameters for
     * more info on the behavior of this API.
     * </p>
     * <p>
     * When Java 2 Security is enabled, access to call this method requires
     * WebSphereRuntimePermission "getSSLConfig" to be granted.
     * </p>
     *
     * @param sslAliasName
     * @param connectionInfo
     * @param listener
     * @return URLStreamHandler
     * @throws SSLException
     * @ibm-api
     **/
    public URLStreamHandler getURLStreamHandler(String sslAliasName, Map<String, Object> connectionInfo, SSLConfigChangeListener listener) throws SSLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getURLStreamHandler", new Object[] { sslAliasName, connectionInfo, listener });

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Performing Java 2 Security Permission Check ...");
                Tr.debug(tc, "Expecting : " + GET_SSLCONFIG.toString());
            }
            sm.checkPermission(GET_SSLCONFIG);
        }

        URLStreamHandler urlStreamHandler = null;

        try {
            Properties sslProperties = getProperties(sslAliasName, connectionInfo, listener);
            String contextProvider = null;

            if (sslProperties != null) {
                contextProvider = sslProperties.getProperty(Constants.SSLPROP_CONTEXT_PROVIDER);
            }

            urlStreamHandler = JSSEProviderFactory.getInstance(contextProvider).getURLStreamHandler((SSLConfig) sslProperties);
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "getURLStreamHandler (2)", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "The following exception occurred in getURLStreamHandler().", new Object[] { e });
            throw asSSLException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getURLStreamHandler");
        return urlStreamHandler;
    }

    /**
     * <p>
     * This method creates an SSLSocketFactory for use by an SSL application or
     * component. Precedence logic will determine which parameters are used for
     * creating the SSLSocketFactory. See the JavaDoc for getSSLContext with the
     * same parameters for more info on the behavior of this API.
     * </p>
     * <p>
     * When Java 2 Security is enabled, access to call this method requires
     * WebSphereRuntimePermission "getSSLConfig" to be granted.
     * </p>
     *
     * @param sslAliasName - Used in direct selection. The alias name of a
     *            specific SSL configuration (optional). You can pass in "null" here.
     *            If sslAliasName is provided but does not exist it will check
     *            connection information for a match. Then look for a default if no
     *            match with the connection information.
     * @param connectionInfo - This refers to the remote connection information. The
     *            current properties known by the runtime include:
     *            <p>
     *            Example OUTBOUND case (endpoint refers more to protocol used since
     *            outbound names are not well-known):
     *            <ul>
     *            <li>com.ibm.ssl.remoteHost="hostname.ibm.com"</li>
     *            <li>com.ibm.ssl.remotePort="9809"</li>
     *            <li>com.ibm.ssl.direction="outbound"</li>
     *            </ul></p>
     *            It's highly recommended to supply these properties when possible.
     * @param listener - This is used to notify the
     *            caller of this API that the SSL configuration changed in the runtime.
     *            It's up to the caller to decide if they want to call this API again
     *            to get the new SSLContext for the configuration. Passing in NULL
     *            indicates no notification is desired. See the
     *            com.ibm.websphere.ssl.SSLConfigChangeListener interface for more
     *            information.
     * @return SSLSocketFactory
     * @throws com.ibm.websphere.ssl.SSLException
     * @ibm-api
     **/
    public SSLSocketFactory getSSLSocketFactory(String sslAliasName, Map<String, Object> connectionInfo, SSLConfigChangeListener listener) throws SSLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getSSLSocketFactory", new Object[] { sslAliasName, connectionInfo, listener });

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Performing Java 2 Security Permission Check ...");
                Tr.debug(tc, "Expecting : " + GET_SSLCONFIG.toString());
            }
            sm.checkPermission(GET_SSLCONFIG);
        }

        SSLSocketFactory sslSocketFactory = null;

        if (sslAliasName == null && (connectionInfo == null || connectionInfo.isEmpty())) {
            connectionInfo = new HashMap<String, Object>();
            connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
        }

        try {
            SSLConfig sslProperties = (SSLConfig) getProperties(sslAliasName, connectionInfo, listener);
            String contextProvider = sslProperties.getProperty(Constants.SSLPROP_CONTEXT_PROVIDER);
            sslSocketFactory = JSSEProviderFactory.getInstance(contextProvider).getSSLSocketFactory(connectionInfo, sslProperties);
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "getSSLSocketFactory (2)", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "The following exception occurred in getSSLSocketFactory().", new Object[] { e });
            throw asSSLException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getSSLSocketFactory");
        return sslSocketFactory;
    }

    /**
     * <p>
     * This method creates an SSLSocketFactory for use by an SSL application or
     * component. Precedence logic will determine which parameters are used for
     * creating the SSLSocketFactory. See the JavaDoc for getSSLContext with the
     * same parameters for more info on the behavior of this API.
     * </p>
     * <p>
     * When Java 2 Security is enabled, access to call this method requires
     * WebSphereRuntimePermission "getSSLConfig" to be granted.
     * </p>
     *
     * @param sslAliasName - Used in direct selection. The alias name of a
     *            specific SSL configuration (optional). You can pass in "null" here.
     *            If sslAliasName is provided but does not exist it will check
     *            connection information for a match. Then look for a default if no
     *            match with the connection information.
     * @param connectionInfo - This refers to the remote connection information. The
     *            current properties known by the runtime include:
     *            <p>
     *            Example INBOUND case (endpoint name matches serverindex endpoint):
     *            <code>
     *            com.ibm.ssl.direction="inbound"
     *            </code></p>
     *            It's highly recommended to supply these properties when possible.
     * @param listener - This is used to notify the
     *            caller of this API that the SSL configuration changed in the runtime.
     *            It's up to the caller to decide if they want to call this API again
     *            to get the new SSLContext for the configuration. Passing in NULL
     *            indicates no notification is desired. See the
     *            com.ibm.websphere.ssl.SSLConfigChangeListener interface for more
     *            information.
     * @return SSLServerSocketFactory
     * @throws com.ibm.websphere.ssl.SSLException
     * @ibm-api
     **/
    public SSLServerSocketFactory getSSLServerSocketFactory(String sslAliasName, Map<String, Object> connectionInfo, SSLConfigChangeListener listener) throws SSLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getSSLServerSocketFactory", new Object[] { sslAliasName, connectionInfo, listener });

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Performing Java 2 Security Permission Check ...");
                Tr.debug(tc, "Expecting : " + GET_SSLCONFIG.toString());
            }
            sm.checkPermission(GET_SSLCONFIG);
        }
        SSLServerSocketFactory sslServerSocketFactory = null;

        try {
            SSLConfig sslProperties = (SSLConfig) getProperties(sslAliasName, connectionInfo, listener);
            String contextProvider = sslProperties.getProperty(Constants.SSLPROP_CONTEXT_PROVIDER);
            sslServerSocketFactory = JSSEProviderFactory.getInstance(contextProvider).getSSLServerSocketFactory(sslProperties);
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "getSSLServerSocketFactory (2)", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "The following exception occurred in getSSLServerSocketFactory().", new Object[] { e });
            throw asSSLException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getSSLServerSocketFactory");
        return sslServerSocketFactory;
    }

    /**
     * <p>
     * This method returns the effective SSL properties object for use by an SSL
     * application or component.
     * </p>
     * <p>
     * When Java 2 Security is enabled, access to call this method requires
     * WebSphereRuntimePermission "getSSLConfig" to be granted.
     * </p>
     *
     * @param sslAliasName - Used in direct selection. The alias name of a
     *            specific SSL configuration (optional). You can pass in "null" here.
     *            If sslAliasName is provided but does not exist it will check
     *            connection information for a match. Then look for a default if no
     *            match with the connection information.
     * @param connectionInfo - This refers to the remote connection information. The
     *            current properties known by the runtime include:
     *            <p>
     *            Example OUTBOUND case (endpoint refers more to protocol used since
     *            outbound names are not well-known):
     *            <ul>
     *            <li>com.ibm.ssl.remoteHost="hostname.ibm.com"</li>
     *            <li>com.ibm.ssl.remotePort="9809"</li>
     *            <li>com.ibm.ssl.direction="outbound"</li>
     *            </ul></p>
     *            <p>
     *            Example INBOUND case (endpoint name matches serverindex endpoint):
     *            <code>
     *            com.ibm.ssl.direction="inbound"
     *            </code></p>
     *            It's highly recommended to supply these properties when possible.
     * @param listener - This is used to notify the
     *            caller of this API that the SSL configuration changed in the runtime.
     *            It's up to the caller to decide if they want to call this API again
     *            to get the new SSLContext for the configuration. Passing in NULL
     *            indicates no notification is desired. See the
     *            com.ibm.websphere.ssl.SSLConfigChangeListener interface for more
     *            information.
     * @return Properties for the requested sslAliasName.
     *         If the requested sslAliasName is not avialable, the default properties will be returned.
     *         If the default properties are not available, null is returned.
     * @throws com.ibm.websphere.ssl.SSLException
     *
     * @ibm-api
     **/
    public Properties getProperties(String sslAliasName, Map<String, Object> connectionInfo, SSLConfigChangeListener listener) throws SSLException {
        return getProperties(sslAliasName, connectionInfo, listener, true);
    }

    /**
     * Like {@link #getProperties(String, Map, SSLConfigChangeListener)},
     * except failing over to the default configuration is a choice.
     *
     * @param sslAliasName - Used in direct selection. The alias name of a
     *            specific SSL configuration (optional). You can pass in "null" here.
     *            If sslAliasName is provided but does not exist it will check
     *            connection information for a match. Then look for a default if no
     *            match with the connection information.
     * @param connectionInfo - This refers to the remote connection information. The
     *            current properties known by the runtime include:
     *            <p>
     *            Example OUTBOUND case (endpoint refers more to protocol used since
     *            outbound names are not well-known):
     *            <ul>
     *            <li>com.ibm.ssl.remoteHost="hostname.ibm.com"</li>
     *            <li>com.ibm.ssl.remotePort="9809"</li>
     *            <li>com.ibm.ssl.direction="outbound"</li>
     *            </ul></p>
     *            <p>
     *            Example INBOUND case (endpoint name matches serverindex endpoint):
     *            <code>
     *            com.ibm.ssl.direction="inbound"
     *            </code></p>
     *            It's highly recommended to supply these properties when possible.
     * @param listener - This is used to notify the
     *            caller of this API that the SSL configuration changed in the runtime.
     *            It's up to the caller to decide if they want to call this API again
     *            to get the new SSLContext for the configuration. Passing in NULL
     *            indicates no notification is desired. See the
     *            com.ibm.websphere.ssl.SSLConfigChangeListener interface for more
     *            information.
     * @param tryDefault if the specified alias is not available, {@code true} indicates the default configuration should be tried.
     * @return Properties for the requested sslAliasName.
     *         If the requested sslAliasName properties are not available, null is returned.
     * @throws SSLException
     */
    public Properties getProperties(String sslAliasName, Map<String, Object> connectionInfo, SSLConfigChangeListener listener, boolean tryDefault) throws SSLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getProperties", new Object[] { sslAliasName, connectionInfo, listener });

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Performing Java 2 Security Permission Check ...");
                Tr.debug(tc, "Expecting : " + GET_SSLCONFIG.toString());
            }
            sm.checkPermission(GET_SSLCONFIG);
        }

        boolean isOutbound = true;
        if (SSLConfigManager.getInstance().isTransportSecurityEnabled()) {
            if (connectionInfo != null) {
                String direction = (String) connectionInfo.get(Constants.CONNECTION_INFO_DIRECTION);
                if (direction != null && direction.equals(Constants.DIRECTION_INBOUND))
                    isOutbound = false;
            }
        }

        try {
            // programmatic selection
            Properties programmaticProperties = getSSLPropertiesOnThread();
            if (programmaticProperties != null) {
                // convert to SSLConfig for validation of customer properties
                SSLConfig programmaticConfig = new SSLConfig(programmaticProperties);

                if (listener != null) {
                    String alias = programmaticConfig.getProperty(Constants.SSLPROP_ALIAS);
                    if (alias == null)
                        alias = sslAliasName;
                    registerEvent(listener, alias, programmaticConfig, Constants.SELECTION_TYPE_THREAD, connectionInfo);
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "getProperties -> programmatic");
                return programmaticConfig;
            }

            // direct selection
            if (sslAliasName != null && sslAliasName.length() > 0) {
                Properties directSelectionProperties = SSLConfigManager.getInstance().getProperties(sslAliasName);

                if (directSelectionProperties == null) { // added for issue 876
                    String origAliasName = SSLConfigManager.getInstance().getPossibleActualID(sslAliasName);
                    if (origAliasName != null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "could not find properties with " + sslAliasName + " trying properties with " + origAliasName);
                        directSelectionProperties = SSLConfigManager.getInstance().getProperties(origAliasName);
                    }
                }

                if (directSelectionProperties != null) {
                    directSelectionProperties = SSLConfigManager.getInstance().determineIfCSIv2SettingsApply(directSelectionProperties, connectionInfo);

                    if (listener != null)
                        registerEvent(listener, sslAliasName, directSelectionProperties, Constants.SELECTION_TYPE_DIRECT, connectionInfo);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.exit(tc, "getProperties -> direct");
                    return directSelectionProperties;
                }
            }

            // dynamic selection
            if (SSLConfigManager.getInstance().isTransportSecurityEnabled() && isOutbound) {
                SSLConfig dynamicSelectionConfig = (SSLConfig) SSLConfigManager.getInstance().getPropertiesFromDynamicSelectionInfo(connectionInfo);
                if (dynamicSelectionConfig != null) {

                    if (listener != null) {
                        String alias = dynamicSelectionConfig.getProperty(Constants.SSLPROP_ALIAS);
                        if (alias == null)
                            alias = sslAliasName;
                        registerEvent(listener, alias, dynamicSelectionConfig, Constants.SELECTION_TYPE_DYNAMIC, connectionInfo);
                    }

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.exit(tc, "getProperties -> dynamic");
                    return dynamicSelectionConfig;
                }
            }

            if (tryDefault) {
                if (SSLConfigManager.getInstance().isTransportSecurityEnabled() && isOutbound) {
                    Properties props = SSLConfigManager.getInstance().getOutboundDefaultSSLConfig();
                    if (props != null && !props.isEmpty()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                            Tr.exit(tc, "getProperties -> outbound default");
                        }
                        return props;
                    }
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "getProperties -> default");
                }
                return SSLConfigManager.getInstance().getDefaultSSLConfig();
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "getProperties -> null");
                }
                return null;
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "getProperties (2)", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "The following exception occurred in getProperties().", new Object[] { e });
            throw asSSLException(e);
        }
    }

    private void registerEvent(SSLConfigChangeListener listener, String alias, Properties config, String selection, Map<String, Object> connInfo) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "registerEvent", new Object[] { listener, alias, selection });

        if (listener != null) {
            SSLConfigChangeEvent event = new SSLConfigChangeEvent(alias, config, selection, connInfo);

            SSLConfigManager.getInstance().registerSSLConfigChangeListener(listener, event);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "registerEvent");
    }

    /**
     * <p>
     * This method registers an SSLConfigChangeListener for the specific SSL
     * configuration chosen based upon the parameters passed in using the
     * precedence logic described in the JavaDoc for the getSSLContext API. The
     * SSLConfigChangeListener must be deregistered by
     * deregisterSSLConfigChangeListener when it is no longer needed.
     * </p>
     *
     * @param sslAliasName
     * @param connectionInfo
     * @param listener
     * @throws com.ibm.websphere.ssl.SSLException
     *
     * @ibm-api
     **/
    public void registerSSLConfigChangeListener(String sslAliasName, Map<String, Object> connectionInfo, SSLConfigChangeListener listener) throws SSLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "registerSSLConfigChangeListener", new Object[] { sslAliasName, connectionInfo, listener });
        getProperties(sslAliasName, connectionInfo, listener);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "registerSSLConfigChangeListener");
    }

    /**
     * <p>
     * This method removes the specific SSLConfigChangeListener from the list of
     * active listeners.
     * </p>
     *
     * @param listener
     * @throws com.ibm.websphere.ssl.SSLException
     *
     * @ibm-api
     **/
    public void deregisterSSLConfigChangeListener(SSLConfigChangeListener listener) throws SSLException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "deregisterSSLConfigChangeListener", new Object[] { listener });
        SSLConfigManager.getInstance().deregisterSSLConfigChangeListener(listener);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "deregisterSSLConfigChangeListener");
    }

    /**
     * <p>
     * This method checks to ensure the SSL configuration name is known.
     * </p>
     *
     * @param sslAliasName - Name of the SSL configuration to check to see if it exits.
     * @return boolean
     *
     * @ibm-api
     **/
    public boolean doesSSLConfigExist(String sslAliasName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "doesSSLConfigExist", new Object[] { sslAliasName });

        if (sslAliasName == null)
            throw new IllegalArgumentException("sslAliasName is null.");

        boolean exists = (null != SSLConfigManager.getInstance().getProperties(sslAliasName));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "doesSSLConfigExist -> " + exists);
        return exists;
    }

    /**
     * <p>
     * This method is not implemented.
     * </p>
     *
     * @ibm-api
     **/
    public void reinitializeClientDefaultSSLProperties() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "reinitializeClientDefaultSSLProperties");

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "reinitializeClientDefaultSSLProperties");
    }

    /**
     * <p>
     * This method attempts to create an SSLContext using the properties provided.
     * It is assumed the API is called on the node where the KeyStore information
     * specified in the properties resides.
     * </p>
     *
     * @param props
     * @throws com.ibm.websphere.ssl.SSLException
     *
     * @ibm-api
     **/
    public void validateSSLProperties(Properties props) throws SSLException {
        SSLConfig testConfig = new SSLConfig(props);

        try {
            testConfig.validateSSLConfig();
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "validateSSLProperties", this);
            throw asSSLException(e);
        }
    }

    /**
     * <p>
     * This method is used to obtain information about the connection on the
     * thread of execution. This connection information can then be used from
     * Custom Key and Trust Managers.
     * </p>
     *
     * @return Map<String,Object>
     *
     * @ibm-api
     **/
    public Map<String, Object> getInboundConnectionInfo() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getInboundConnectionInfo");
        Map<String, Object> inboundConnectionInfo = ThreadManager.getInstance().getInboundConnectionInfo();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getInboundConnectionInfo", inboundConnectionInfo);
        return inboundConnectionInfo;
    }

    /**
     * <p>
     * This method sets information about the connection on the thread
     * of execution. This connection information can then be used from
     * Custom Key and Trust Managers. This method is invoked prior to
     * an SSL handshake.
     * </p>
     * <p>
     * It's important to clear the thread after use, especially where thread
     * pools are used. It is not cleared up automatically. Pass in "null" to
     * this API to clear it.
     * </p>
     *
     * @param connectionInfo - This refers to the inbound connection
     *            information.
     *
     * @ibm-api
     **/
    public void setInboundConnectionInfo(Map<String, Object> connectionInfo) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "setInboundConnectionInfo", connectionInfo);
        ThreadManager.getInstance().setInboundConnectionInfo(connectionInfo);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "setInboundConnectionInfo");
    }

    /**
     * <p>
     * This method is used to obtain information about the connection on the
     * thread of execution. This connection information can then be used to set the
     * connection information prior to creating and SSL socket.
     * </p>
     *
     * @return Map<String,Object>
     *
     * @ibm-api
     */
    public Map<String, Object> getOutboundConnectionInfo() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getOutboundConnectionInfo");
        Map<String, Object> outboundConnectionInfo = ThreadManager.getInstance().getOutboundConnectionInfo();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getOutboundConnectionInfo", outboundConnectionInfo);
        return outboundConnectionInfo;
    }

    /**
     * <p>
     * This method sets information about the connection on the thread
     * of execution. This method is invoked prior to creating an SSL socket.
     * </p>
     *
     * <p>
     * It's important to clear the thread after use, especially where thread
     * pools are used. It is not cleared up automatically. Pass in "null" to
     * this API to clear it.
     * </p>
     *
     * @param connectionInfo
     */
    public void setOutboundConnectionInfo(Map<String, Object> connectionInfo) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "setOutboundConnectionInfo", connectionInfo);
        ThreadManager.getInstance().setOutboundConnectionInfo(connectionInfo);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "setOutboundConnectionInfo");
    }

    /**
     * Convenience method to wrap any other exception as an {@link SSLException}.
     *
     * @param e
     *            The exception to be wrapped or re-thrown.
     * @return the original exception, or a new {@link SSLException} to wrap it
     */
    private static SSLException asSSLException(Exception e) throws SSLException {
        if (e instanceof SSLException)
            return (SSLException) e;
        return new SSLException(e.getMessage(), e);
    }
}
