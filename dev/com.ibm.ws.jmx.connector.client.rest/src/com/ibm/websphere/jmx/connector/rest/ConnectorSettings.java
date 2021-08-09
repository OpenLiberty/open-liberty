/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.jmx.connector.rest;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.ibm.ws.jmx.connector.client.rest.ClientProvider;

/**
 * Provides constants for connection settings of the Liberty profile JMX REST connector client.
 * The settings can be adjusted in 2 ways:
 * <p>
 * <ol>
 * <li>Globally, by setting a system property with the desired value. For example, to set READ_TIMEOUT to 2 minutes (120000 milliseconds), use the Java option:
 * <p>
 *
 * <pre>
 * -Dcom.ibm.ws.jmx.connector.client.rest.readTimeout=120000
 * </pre>
 *
 * </li>
 * <li>On a per connection basis, by adding an entry to the environment Map parameter of the
 * {@link JMXConnectorFactory#newJMXConnector(javax.management.remote.JMXServiceURL, java.util.Map)} or {@link JMXConnector#connect(java.util.Map)} method.
 * <p>
 *
 * <pre>
 * <code>
 * HashMap<String, Object> environment = new HashMap<String, Object>();
 * environment.put("jmx.remote.protocol.provider.pkgs", "com.ibm.ws.jmx.connector.client");
 * environment.put(JMXConnector.CREDENTIALS, new String[] { "bob", "bobpassword" });
 * <b>environment.put(ConnectorSettings.MAX_SERVER_WAIT_TIME, 120000);</b>
 *
 * JMXServiceURL url = new JMXServiceURL("service:jmx:rest://&lt;host&gt;:&lt;port&gt;/IBMJMXConnectorREST");
 * JMXConnector connector = JMXConnectorFactory.newJMXConnector(url, <b>environment</b>);
 * connector.connect(<b>environment</b>);
 * </code>
 * </pre>
 *
 * </li>
 * </ol>
 *
 * @ibm-api
 */
public interface ConnectorSettings {

    /**
     * Boolean setting that when enabled disables hostname verification on the client connections. This can be useful for environments where the hostname used does not match the
     * one specified in the server certificate.
     */
    public static final String DISABLE_HOSTNAME_VERIFICATION = ClientProvider.CLIENT_DOMAIN + ".disableURLHostnameVerification";

    /**
     * Integer setting for the maximum amount of time in milliseconds that the server waits for new notifications before responding to a request for notifications from the client.
     * A larger value results in better notification delivery times because less time is spent establishing new connections. Normally it is not necessary to adjust this value.
     */
    public static final String NOTIFICATION_DELIVERY_INTERVAL = ClientProvider.REST_CLIENT_DOMAIN + ".notificationDeliveryInterval";

    /**
     * Integer setting for the amount of time in milliseconds that the server waits before discarding notification registrations if the client has not checked for new
     * notifications. Normally it is not necessary to adjust this value.
     */
    public static final String NOTIFICATION_INBOX_EXPIRY = ClientProvider.REST_CLIENT_DOMAIN + ".notificationInboxExpiry";

    /**
     * Integer setting for the amount of time in milliseconds that the client waits before making a new request to fetch notifications.
     */
    public static final String NOTIFICATION_FETCH_INTERVAL = ClientProvider.REST_CLIENT_DOMAIN + ".notificationFetchInterval";

    /**
     * Integer setting for the read timeout in milliseconds for all client communications with the server, except notification fetching. Adjust this value if the client throws read
     * timeout exceptions because of a slow connection or client or server process.
     */
    public static final String READ_TIMEOUT = ClientProvider.REST_CLIENT_DOMAIN + ".readTimeout";

    /**
     * Integer setting for the read timeout in milliseconds for notification fetching. Because the server might wait up to NOTIFICATION_DELIVERY_INTERVAL before responding, this
     * value must be somewhat larger, though normally it is not necessary to adjust this value.
     */
    public static final String NOTIFICATION_READ_TIMEOUT = ClientProvider.REST_CLIENT_DOMAIN + ".notificationReadTimeout";

    /**
     * Integer setting for the amount of time in milliseconds that the client waits between checks that the server is still available. To disable this behaviour, set the value to
     * a negative integer.
     *
     * This value is overridden by {@link ConnectorSettings.NOTIFICATION_FETCH_INTERVAL} whenever there are notification listeners registered with this client.
     */
    public static final String SERVER_FAILOVER_INTERVAL = ClientProvider.REST_CLIENT_DOMAIN + ".failoverInterval";

    /**
     * Integer setting for the amount of time in milliseconds that the client waits for the server to become available before the JMX connection fails and a new connection must be
     * created. If the connection is restored, any previous notification listeners are registered again. To disable this behavior, set the value to zero.
     */
    public static final String MAX_SERVER_WAIT_TIME = ClientProvider.REST_CLIENT_DOMAIN + ".maxServerWaitTime";

    /**
     * Integer setting for the amount of time in milliseconds that the client waits between checks that the server is available again when MAX_SERVER_WAIT_TIME is non-zero.
     * Normally it is not necessary to adjust this value.
     */
    public static final String SERVER_STATUS_POLLING_INTERVAL = ClientProvider.REST_CLIENT_DOMAIN + ".serverStatusPollingInterval";

    /**
     * Indicates that the {@link JMXConnector.CREDENTIALS} will be handled by SSL certificate based authentication.
     */
    public static final String CERTIFICATE_AUTHENTICATION = ClientProvider.CLIENT_DOMAIN + ".CLIENT_CERT_AUTH";

    /**
     * Indicates that all JMX connections will use the specified SSLSocketFactory
     */
    public static final String CUSTOM_SSLSOCKETFACTORY = ClientProvider.CLIENT_DOMAIN + ".CUSTOM_SSLSOCKETFACTORY";

    /**
     * <h3>Work Load Management Endpoints</h3>
     * List of Strings setting to indicate which endpoints, in addition
     * to the host and port in the {@link JMXServiceURL}, are valid for
     * establishing a connection. An endpoint is defined as "host:port".
     * <p>
     * When the work load management endpoint list is set, the initial
     * connection is made using one of the endpoints in the complete set of
     * available endpoints. The complete set of endpoints is the host and port
     * specified to the {@link JMXServiceURL} as well as the contents of the
     * work load management endpoint list.
     * <p>
     * This property is only supported in the programmatic JMX environment.
     *
     * <pre>
     * <code>
     * List&lt;String&gt; endpoints = new ArrayList&lt;String&gt;
     * endpoints.add("&lt;host2&gt;:&lt;port&gt;");
     *
     * HashMap<String, Object> environment = new HashMap<String, Object>();
     * environment.put("jmx.remote.protocol.provider.pkgs", "com.ibm.ws.jmx.connector.client");
     * environment.put(JMXConnector.CREDENTIALS, new String[] { "bob", "bobpassword" });
     * <b>environment.put(ConnectorSettings.WLM_ENDPOINTS, endpoints);</b>
     *
     * JMXServiceURL url = new JMXServiceURL("service:jmx:rest://&lt;host&gt;:&lt;port&gt;/IBMJMXConnectorREST");
     * JMXConnector connector = JMXConnectorFactory.newJMXConnector(url, <b>environment</b>);
     * connector.connect(<b>environment</b>);
     * </code>
     * </pre>
     *
     * The preceding code would establish the available endpoint set as host:port
     * and host2:port.
     * <p>
     * No ordering guarantees are made regarding which endpoint is ultimately
     * used for the connection, but all endpoints will be tried in order to
     * establish a connection. Only when all endpoints in the complete set are
     * inaccessible is a connection considered to be unobtainable.
     * <p>
     * Fail-over retry will occur if the invoked operation could not be
     * started. If the connection is lost during an operation, no retry will
     * be done so as to not issue the same command twice. In other words, if
     * we loose the connection before the operation can report success, an
     * IOException will be thrown and the operation will not be re-tried.
     */
    public static final String WLM_ENDPOINTS = ClientProvider.CLIENT_DOMAIN + ".wlm.endpoints";

    /**
     * This parameter represents the host name to be used in a routing context.
     *
     * This parameter is not applicable to the settings passed into the client-side of the JMX REST connector.
     * It is used only by the file transfer RESTful endpoint and the Routing MBean, exposed by the server-side of the JMX REST connector.
     */
    public static final String ROUTING_KEY_HOST_NAME = "com.ibm.websphere.jmx.connector.rest.routing.hostName";

    /**
     * This parameter represents the server name to be used in a routing context.
     *
     * This parameter is not applicable to the settings passed into the client-side of the JMX REST connector.
     * It is used only by the file transfer RESTful endpoint and the Routing MBean, exposed by the server-side of the JMX REST connector.
     */
    public static final String ROUTING_KEY_SERVER_NAME = "com.ibm.websphere.jmx.connector.rest.routing.serverName";

    /**
     * This parameter represents the server user directory to be used in a routing context.
     *
     * This parameter is not applicable to the settings passed into the client-side of the JMX REST connector.
     * It is used only by the file transfer RESTful endpoint and the Routing MBean, exposed by the server-side of the JMX REST connector.
     */
    public static final String ROUTING_KEY_SERVER_USER_DIR = "com.ibm.websphere.jmx.connector.rest.routing.serverUserDir";

    /**
     * This parameter represents a comma-separated list of host names within the collective.
     *
     * This parameter is not applicable to the settings passed into the client-side of the JMX REST connector.
     * It is used only by the file transfer RESTful endpoint, exposed by the server-side of the JMX REST connector.
     */
    public static final String COLLECTIVE_HOST_NAMES = "com.ibm.websphere.collective.hostNames";

    /**
     * This parameter represents a request for an asynchronous execution within the collective.
     *
     * This parameter is not applicable to the settings passed into the client-side of the JMX REST connector.
     * It is used only by the file transfer RESTful endpoint, exposed by the server-side of the JMX REST connector.
     */
    public static final String ASYNC_EXECUTION = "com.ibm.websphere.jmx.connector.rest.asyncExecution";

    /**
     * This parameter represents the set of credentials to be used for the transfer action.
     * The payload of the header is a JSON object with attributes from the CollectiveRegistrationMBean.
     * Only credentials (user name, password, keys) are defined. The host and port attributes are not supported.
     *
     * This parameter is not applicable to the settings passed into the client-side of the JMX REST connector.
     * It is used only by the file transfer RESTful endpoint, exposed by the server-side of the JMX REST connector.
     */
    public static final String TRANSFER_CREDENTIALS = "com.ibm.websphere.jmx.connector.rest.transferCredentials";

    /**
     * This parameter represents the request for a pre transfer action within the collective.
     *
     * This parameter is not applicable to the settings passed into the client-side of the JMX REST connector.
     * It is used only by the file transfer RESTful endpoint, exposed by the server-side of the JMX REST connector.
     */
    public static final String PRE_TRANSFER_ACTION = "com.ibm.websphere.jmx.connector.rest.preTransferAction";

    /**
     * This parameter represents the request for a post transfer action within the collective.
     *
     * This parameter is not applicable to the settings passed into the client-side of the JMX REST connector.
     * It is used only by the file transfer RESTful endpoint, exposed by the server-side of the JMX REST connector.
     */
    public static final String POST_TRANSFER_ACTION = "com.ibm.websphere.jmx.connector.rest.postTransferAction";

    /**
     * This parameter represents the default value for the collective pre-transfer-action.
     *
     * This parameter is not applicable to the settings passed into the client-side of the JMX REST connector.
     * It is used only by the file transfer RESTful endpoint, exposed by the server-side of the JMX REST connector.
     */
    public static final String PRE_TRANSFER_ACTION_DEFAULT = "com.ibm.websphere.jmx.connector.rest.preTransferAction.remove";

    /**
     * This parameter represents the default value for the collective post-transfer-action.
     *
     * This parameter is not applicable to the settings passed into the client-side of the JMX REST connector.
     * It is used only by the file transfer RESTful endpoint, exposed by the server-side of the JMX REST connector.
     */
    public static final String POST_TRANSFER_ACTION_DEFAULT = "com.ibm.websphere.jmx.connector.rest.postTransferAction.join";

    /**
     * This parameter represents the action of extarcting a severName from a Liberty zip package for the collective post-transfer-action.
     *
     * This parameter is not applicable to the settings passed into the client-side of the JMX REST connector.
     * It is used only by the file transfer RESTful endpoint, exposed by the server-side of the JMX REST connector.
     */
    public static final String POST_TRANSFER_ACTION_FIND_SERVER_NAME = "com.ibm.websphere.jmx.connector.rest.postTransferAction.findServerName";

    /**
     * This parameter represents the options pertaining to the collective pre-transfer-action.
     *
     * This parameter is not applicable to the settings passed into the client-side of the JMX REST connector.
     * It is used only by the file transfer RESTful endpoint, exposed by the server-side of the JMX REST connector.
     */
    public static final String PRE_TRANSFER_ACTION_OPTIONS = "com.ibm.websphere.jmx.connector.rest.preTransferAction.options";

    /**
     * This parameter represents the options pertaining to the collective post-transfer-action.
     *
     * This parameter is not applicable to the settings passed into the client-side of the JMX REST connector.
     * It is used only by the file transfer RESTful endpoint, exposed by the server-side of the JMX REST connector.
     */
    public static final String POST_TRANSFER_ACTION_OPTIONS = "com.ibm.websphere.jmx.connector.rest.postTransferAction.options";

    /**
     * This parameter represents a set of environment variables and their corresponding values.
     * The payload of the header is a JSON object that has envKey : envValue pairs.
     *
     * This parameter is not applicable to the settings passed into the client-side of the JMX REST connector.
     * It is used only by the file transfer RESTful endpoint, exposed by the server-side of the JMX REST connector.
     */
    public static final String TRANSFER_ENV_VARS = "com.ibm.websphere.jmx.connector.rest.transferEnvVars";

}
