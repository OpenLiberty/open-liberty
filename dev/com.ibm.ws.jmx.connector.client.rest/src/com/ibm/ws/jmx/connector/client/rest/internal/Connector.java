/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.client.rest.internal;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import javax.net.ssl.SSLSocketFactory;
import javax.security.auth.Subject;

import com.ibm.websphere.jmx.connector.rest.ConnectorSettings;
import com.ibm.ws.jmx.connector.client.rest.ClientProvider;
import com.ibm.ws.jmx.connector.client.rest.internal.resources.RESTClientMessagesUtil;
import com.ibm.ws.jmx.connector.converter.JSONConverter;
import com.ibm.ws.jmx.connector.datatypes.ConversionException;

public class Connector implements JMXConnector {

    private static final Logger logger = Logger.getLogger(Connector.class.getName());

    private final JMXServiceURL serviceURL;
    private Map<String, ?> environment;
    private long connectionId;

    private List<String> wlmList;

    private String currentEndpoint;

    private boolean hostnameVerificationDisabled;
    private int notificationDeliveryInterval, notificationFetchInterval, notificationInboxExpiry, readTimeout, notificationReadTimeout, serverFailoverInterval, maxServerWaitTime,
                    serverStatusPollingInterval;
    private String user;
    private String basicAuthHeader = null;

    private RESTMBeanServerConnection connection = null;
    private boolean closed = false;

    private List<NotificationListenerEntry> connectionListeners = null;

    private static long nextConnectionNumber = 1;

    private SSLSocketFactory customSSLSocketFactory = null;

    private boolean isCollectiveUtilConnection = false;
    private boolean logFailovers = false;

    /**
     * @return the customSSLSocketFactory
     */
    public SSLSocketFactory getCustomSSLSocketFactory() {
        return customSSLSocketFactory;
    }

    /**
     * @param customSSLSocketFactory the customSSLSocketFactory to set
     */
    public void setCustomSSLSocketFactory(SSLSocketFactory customSSLSocketFactory) {
        this.customSSLSocketFactory = customSSLSocketFactory;
    }

    private synchronized static long getConnectionNumber() {
        return nextConnectionNumber++;
    }

    private static long nextNotificationNumber = 1;

    private static long getNotificationNumber() {
        synchronized (Connector.class) {
            return nextNotificationNumber++;
        }
    }

    public Connector(JMXServiceURL serviceURL, Map<String, ?> environment) {
        this.serviceURL = serviceURL;
        this.environment = environment;
        this.isCollectiveUtilConnection = getBooleanSetting("isCollectiveUtil", false);
        this.logFailovers = getBooleanSetting("logFailovers", false);

        if (serviceURL == null) {
            throw new IllegalArgumentException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.NULL_SERVICE_URL));
        }

        //We initialize our WLM list to have the connecting URL.  In non-WLM cases, this is our only URL.
        this.wlmList = new ArrayList<String>();
        this.wlmList.add(serviceURL.getHost() + ":" + serviceURL.getPort());
    }

    /** {@inheritDoc} */
    @Override
    public void connect() throws IOException {
        connect(null);
    }

    private Object getCredentials() {
        Object credentials = environment.get(JMXConnector.CREDENTIALS);
        if (credentials != null) {
            //We found a non-null key in our map, so that's the credentials we'll use
            return credentials;
        }

        //Our map was empty, but before we fail, check if we can fetch a String from the system properties
        credentials = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(JMXConnector.CREDENTIALS);
            }
        });

        return credentials;
    }

    private boolean getBooleanSetting(final String key, boolean defaultValue) {
        // Check environment first for per-connection setting
        if (environment != null) {
            Object setting = environment.get(key);
            if (setting != null && setting instanceof Boolean)
                return (Boolean) setting;
        }

        // Next, check system property for global setting
        String setting = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(key);
            }
        });
        if (setting != null) {
            return Boolean.parseBoolean(setting);
        }

        // Otherwise, return default value
        return defaultValue;
    }

    private int getIntegerSetting(final String key, int defaultValue) {
        // Check environment first for per-connection setting
        if (environment != null) {
            Object setting = environment.get(key);
            if (setting != null && setting instanceof Integer)
                return (Integer) setting;
        }

        // Next, check system property for global setting
        String setting = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(key);
            }
        });
        if (setting != null) {
            try {
                return Integer.parseInt(setting);
            } catch (NumberFormatException nfe) {
                // ignore value
            }
        }

        // Otherwise, return default value
        return defaultValue;
    }

    /**
     * Validate the possible endpoint String.
     *
     * @param possibleEndpoint
     * @throws IllegalArgumentException if the String is not a valid endpoint
     */
    private void validatePossibleEndpoint(String possibleEndpoint) throws IllegalArgumentException {
        if (!possibleEndpoint.contains(":")) {
            throw new IllegalArgumentException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.INVALID_ENDPOINT, possibleEndpoint));
        }
        String[] components = possibleEndpoint.split(":");
        if (components.length != 2) {
            throw new IllegalArgumentException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.INVALID_ENDPOINT, possibleEndpoint));
        }
        try {
            Integer.valueOf(components[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.INVALID_ENDPOINT, possibleEndpoint));
        }
    }

    /**
     * Validates the list contains nothing but Strings, and that those Strings
     * are in the endpoint format. If the List contains non-String elements,
     * or a String that does not follow the endpoint host:port format,
     * an IllegalArgumentException will be thrown.
     *
     * @param list A generic List of Objects to be validated
     * @throws IllegalArgumentException if the List contains a non-String
     *             element or if the String is not in endpoint format
     */
    private void validateEndpointList(List<?> list) throws IllegalArgumentException {
        if (list.isEmpty()) {
            return;
        }

        for (Object element : list) {
            if (element == null) {
                throw new IllegalArgumentException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.INVALID_ENDPOINT, "null"));
            } else if (element instanceof String) {
                String elemStr = (String) element;
                validatePossibleEndpoint((String) element);
                //Add endpoint to our list.  Filter duplicates.
                if (!wlmList.contains(elemStr)) {
                    wlmList.add(elemStr);
                }
            } else {
                throw new IllegalArgumentException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.INVALID_ENDPOINT, element.toString()));
            }
        }

        //Randomize the list if we have more than 1 element.  This is our basic load balancer.
        if (wlmList.size() > 1) {
            Collections.shuffle(wlmList, new Random(System.currentTimeMillis()));
        }

        //We don't want anybody changing our list of endpoints from this point forward
        this.wlmList = Collections.unmodifiableList(this.wlmList);

    }

    /**
     * Return the configured List of endpoints
     */
    List<String> getEndpointList() {
        return this.wlmList;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void connect(Map<String, ?> env) throws IOException {
        // Connector can only be used once
        if (closed)
            throw new IOException();

        // connect has no effect if already connected
        if (connection != null)
            return;

        if (env != null)
            environment = env;

        // User credentials will either be available as a String[] for basic auth, OR
        //as a String (equals to ConnectorSettings.CERTIFICATE_AUTHENTICATION), which could also come from a system property
        Object credentials = getCredentials();
        if (!areRequiedCredentialsSet(credentials)) {
            throw new IOException(JMXConnector.CREDENTIALS + " not provided. Set to a String[2] {user,password}.");
        }
        // If the credentials are not certificate, extract and set the basicAuthHeader
        if (!ConnectorSettings.CERTIFICATE_AUTHENTICATION.equals(credentials)) {
            String[] userPass = (String[]) credentials;
            user = userPass[0];
            JSONConverter converter = JSONConverter.getConverter();
            try {
                basicAuthHeader = "Basic " + converter.encodeStringAsBase64(user + ":" + userPass[1]);
            } catch (ConversionException ce) {
                throw new IOException("Failure encoding credentials", ce);
            } finally {
                JSONConverter.returnConverter(converter);
            }
        }

        hostnameVerificationDisabled = getBooleanSetting(ConnectorSettings.DISABLE_HOSTNAME_VERIFICATION, ClientConstants.DISABLE_HOSTNAME_VERIFICATION_DEFAULT);
        notificationDeliveryInterval = getIntegerSetting(ConnectorSettings.NOTIFICATION_DELIVERY_INTERVAL, ClientConstants.NOTIFICATION_DELIVERY_INTERVAL_DEFAULT);
        notificationFetchInterval = getIntegerSetting(ConnectorSettings.NOTIFICATION_FETCH_INTERVAL, ClientConstants.NOTIFICATION_FETCH_INTERVAL_DEFAULT);
        notificationInboxExpiry = getIntegerSetting(ConnectorSettings.NOTIFICATION_INBOX_EXPIRY, ClientConstants.NOTIFICATION_INBOX_EXPIRY_DEFAULT);
        readTimeout = getIntegerSetting(ConnectorSettings.READ_TIMEOUT, ClientConstants.READ_TIMEOUT_DEFAULT);
        notificationReadTimeout = getIntegerSetting(ConnectorSettings.NOTIFICATION_READ_TIMEOUT,
                                                    notificationDeliveryInterval > 0 ? 2 * notificationDeliveryInterval : ClientConstants.READ_TIMEOUT_DEFAULT);
        serverFailoverInterval = getIntegerSetting(ConnectorSettings.SERVER_FAILOVER_INTERVAL, ClientConstants.SERVER_FAILOVER_INTERVAL_DEFAULT);
        maxServerWaitTime = getIntegerSetting(ConnectorSettings.MAX_SERVER_WAIT_TIME, ClientConstants.MAX_SERVER_WAIT_TIME_DEFAULT);
        serverStatusPollingInterval = getIntegerSetting(ConnectorSettings.SERVER_STATUS_POLLING_INTERVAL, ClientConstants.SERVER_STATUS_POLLING_INTERVAL_DEFAULT);

        //Validate endpoints if applicable
        Object list = environment.get(ConnectorSettings.WLM_ENDPOINTS);
        if (list != null && list instanceof List) {
            validateEndpointList((List<Object>) list);
        }

        // -- Logging
        if (logger.isLoggable(Level.FINER)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Endpoints:");
            for (String endpoint : wlmList) {
                sb.append(endpoint);
                sb.append("  ");
            }
            logger.logp(Level.FINER, logger.getName(), "connect", sb.toString());

            if (logger.isLoggable(Level.FINEST)) {
                sb = new StringBuilder();
                sb.append("notificationDeliveryInterval");
                sb.append("=");
                sb.append(notificationDeliveryInterval);
                sb.append("\n");

                sb.append("notificationFetchInterval");
                sb.append("=");
                sb.append(notificationFetchInterval);
                sb.append("\n");

                sb.append("notificationInboxExpiry");
                sb.append("=");
                sb.append(notificationInboxExpiry);
                sb.append("\n");

                sb.append("readTimeout");
                sb.append("=");
                sb.append(readTimeout);
                sb.append("\n");

                sb.append("notificationReadTimeout");
                sb.append("=");
                sb.append(notificationReadTimeout);
                sb.append("\n");

                sb.append("serverFailoverInterval");
                sb.append("=");
                sb.append(serverFailoverInterval);
                sb.append("\n");

                sb.append("maxServerWaitTime");
                sb.append("=");
                sb.append(maxServerWaitTime);
                sb.append("\n");

                sb.append("serverStatusPollingInterval");
                sb.append("=");
                sb.append(serverStatusPollingInterval);
                sb.append("\n");

                logger.logp(Level.FINEST, logger.getName(), "connect", sb.toString());
            }
        }

        connectionId = getConnectionNumber();

        Object o = environment.get(ConnectorSettings.CUSTOM_SSLSOCKETFACTORY);
        if (o != null && o instanceof SSLSocketFactory) {
            SSLSocketFactory sslf = (SSLSocketFactory) o;
            this.setCustomSSLSocketFactory(sslf);
        }

        // No longer need environment
        environment = null;

        connection = new RESTMBeanServerConnection(this);

        connectionOpened();
    }

    /**
     * @param credentials
     * @return
     */
    private boolean areRequiedCredentialsSet(Object credentials) {
        if (credentials == null) {
            return false;
        }
        if (ConnectorSettings.CERTIFICATE_AUTHENTICATION.equals(credentials)) {
            return true;
        }
        if ((credentials instanceof String[]) && (((String[]) credentials).length == 2)) {
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized MBeanServerConnection getMBeanServerConnection() throws IOException {
        // if not yet connected, throw IOException
        if (connection == null)
            throw new IOException();

        return connection;
    }

    /** {@inheritDoc} */
    @Override
    public MBeanServerConnection getMBeanServerConnection(Subject delegationSubject) throws IOException {
        // this use-case not supported
        throw new IOException();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void close() throws IOException {
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "close", "Closing connector");
        }

        if (!closed) {
            // It is possible for close to be called prior to connecting.
            // If so, we behave as a no-op.
            if (connection != null) {
                connection.close();
            }
            connection = null;
            closed = true;
            if (connectionListeners != null) {
                JMXConnectionNotification notification = new JMXConnectionNotification(JMXConnectionNotification.CLOSED, this, getConnectionId(), getNotificationNumber(), null, null);
                for (NotificationListenerEntry entry : connectionListeners)
                    entry.handleNotification(notification);
            }
        }
    }

    synchronized void connectionOpened() {
        if (connectionListeners != null) {
            JMXConnectionNotification notification = new JMXConnectionNotification(JMXConnectionNotification.OPENED, this, getConnectionId(), getNotificationNumber(), null, null);
            for (NotificationListenerEntry entry : connectionListeners)
                entry.handleNotification(notification);
        }
    }

    synchronized void connectionFailed(Throwable t) {
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "connectionFailed", "Failed connection: " + t);
        }
        closed = true;
        if (connectionListeners != null) {
            JMXConnectionNotification notification = new JMXConnectionNotification(JMXConnectionNotification.FAILED, this, getConnectionId(), getNotificationNumber(), RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.CONNECTION_FAILED), t);
            for (NotificationListenerEntry entry : connectionListeners)
                entry.handleNotification(notification);
        }
    }

    synchronized void notificationLost(Notification n) {
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "notificationLost", "Lost notification: " + n);
        }

        if (connectionListeners != null) {
            JMXConnectionNotification notification = new JMXConnectionNotification(JMXConnectionNotification.NOTIFS_LOST, this, getConnectionId(), getNotificationNumber(), RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.NOTIFICATION_LOST), n);
            for (NotificationListenerEntry entry : connectionListeners)
                entry.handleNotification(notification);
        }
    }

    synchronized void connectionTemporarilyLost(Throwable t) {
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "connectionTemporarilyLost", "Lost connection:" + t.getMessage());
        }

        if (connectionListeners != null) {
            JMXConnectionNotification notification = new JMXConnectionNotification(ClientProvider.CONNECTION_TEMPORARILY_LOST, this, getConnectionId(), getNotificationNumber(), RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.CONNECTION_TEMPORARILY_LOST), t);
            for (NotificationListenerEntry entry : connectionListeners)
                entry.handleNotification(notification);
        }
    }

    synchronized void connectionRestored(Exception[] exceptions) {
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "connectionRestored", "Connection restored");
        }

        if (connectionListeners != null) {
            JMXConnectionNotification notification;
            if (exceptions != null) {
                notification = new JMXConnectionNotification(ClientProvider.CONNECTION_RESTORED_WITH_EXCEPTIONS, this, getConnectionId(), getNotificationNumber(), RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.CONNECTION_RESTORED_WITH_EXCEPTIONS), exceptions);
            } else {
                notification = new JMXConnectionNotification(ClientProvider.CONNECTION_RESTORED, this, getConnectionId(), getNotificationNumber(), RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.CONNECTION_RESTORED), null);
            }
            for (NotificationListenerEntry entry : connectionListeners)
                entry.handleNotification(notification);
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void addConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "addConnectionNotificationListener", "listener: " + listener + " | filter: " + filter + " | handback: " + handback);
        }

        if (connectionListeners == null)
            connectionListeners = new ArrayList<NotificationListenerEntry>();

        connectionListeners.add(new NotificationListenerEntry(listener, filter, handback));
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void removeConnectionNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "removeConnectionNotificationListener", "listener: " + listener);
        }

        boolean listenerFound = false;
        if (connectionListeners != null) {
            Iterator<NotificationListenerEntry> entryIterator = connectionListeners.iterator();
            while (entryIterator.hasNext()) {
                if (entryIterator.next().listener == listener) {
                    listenerFound = true;
                    entryIterator.remove();
                }
            }
        }

        if (!listenerFound)
            throw new ListenerNotFoundException();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void removeConnectionNotificationListener(NotificationListener l, NotificationFilter f, Object handback) throws ListenerNotFoundException {
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "removeConnectionNotificationListener", "listener: " + l + " | filter: " + f + " | handback: " + handback);
        }
        boolean listenerFound = false;
        if (connectionListeners != null) {
            listenerFound = connectionListeners.remove(new NotificationListenerEntry(l, f, handback));
        }

        if (!listenerFound)
            throw new ListenerNotFoundException();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized String getConnectionId() {
        return serviceURL.getProtocol() + ":" + getCurrentEndpoint() + " " + user + " " + connectionId;
    }

    JMXServiceURL getServiceURL() {
        return serviceURL;
    }

    boolean isHostnameVerificationDisabled() {
        return hostnameVerificationDisabled;
    }

    int getNotificationDeliveryInterval() {
        return notificationDeliveryInterval;
    }

    int getNotificationFetchInterval() {
        return notificationFetchInterval;
    }

    int getNotificationInboxExpiry() {
        return notificationInboxExpiry;
    }

    int getReadTimeout() {
        return readTimeout;
    }

    int getNotificationReadTimeout() {
        return notificationReadTimeout;
    }

    int getMaxServerWaitTime() {
        return maxServerWaitTime;
    }

    int getServerFailoverInterval() {
        return serverFailoverInterval;
    }

    int getServerStatusPollingInterval() {
        return serverStatusPollingInterval;
    }

    String getBasicAuthHeader() {
        return basicAuthHeader;
    }

    String getCurrentEndpoint() {
        return currentEndpoint;
    }

    boolean isCollectiveUtilConnection() {
        return isCollectiveUtilConnection;
    }

    void setCurrentEndpoint(String currentEndpoint) {
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "setCurrentEndpoint", "Changing endpoint from " + this.currentEndpoint + " to " + currentEndpoint);
        }
        this.currentEndpoint = currentEndpoint;
    }

    /**
     * @return the logFailovers
     */
    public boolean logFailovers() {
        return logFailovers;
    }

}
