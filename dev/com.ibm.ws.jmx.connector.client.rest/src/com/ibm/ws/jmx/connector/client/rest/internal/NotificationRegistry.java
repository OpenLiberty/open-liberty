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
package com.ibm.ws.jmx.connector.client.rest.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.RuntimeOperationsException;
import javax.net.ssl.HttpsURLConnection;

import com.ibm.ws.jmx.connector.client.rest.internal.ClientConstants.HttpMethod;
import com.ibm.ws.jmx.connector.client.rest.internal.RESTMBeanServerConnection.PollingMode;
import com.ibm.ws.jmx.connector.client.rest.internal.resources.RESTClientMessagesUtil;
import com.ibm.ws.jmx.connector.converter.JSONConverter;
import com.ibm.ws.jmx.connector.converter.NotificationTargetInformation;
import com.ibm.ws.jmx.connector.datatypes.NotificationArea;
import com.ibm.ws.jmx.connector.datatypes.NotificationRegistration;
import com.ibm.ws.jmx.connector.datatypes.NotificationSettings;
import com.ibm.ws.jmx.connector.datatypes.ServerNotificationRegistration;
import com.ibm.ws.jmx.connector.datatypes.ServerNotificationRegistration.Operation;

class NotificationRegistry {

    private static final Logger logger = Logger.getLogger(NotificationRegistry.class.getName());

    private final RESTMBeanServerConnection serverConnection;
    private DynamicURL inboxURL, registrationsURL, serverRegistrationsURL, notificationClientURL;
    private final Map<NotificationTargetInformation, ClientNotificationRegistration> registrationMap = Collections.synchronizedMap(new HashMap<NotificationTargetInformation, ClientNotificationRegistration>());
    private final List<ServerNotificationListenerEntry> serverRegistrationList = Collections.synchronizedList(new ArrayList<ServerNotificationListenerEntry>());
    private ObjectIdentityCache identityCache;
    private final JSONConverter converter = JSONConverter.getConverter();

    NotificationRegistry(RESTMBeanServerConnection serverConnection) throws IOException {
        this.serverConnection = serverConnection;

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "init", "Initializing registry " + RESTClientMessagesUtil.getObjID(this) + " within connection: "
                                                               + serverConnection.getConnector().getConnectionId());
        }

        try {
            setupNotificationArea();
        } catch (IOException io) {
            //ignore IOException at this point, because we will do a connection recovery later on inside the notification thread's run method.
        } catch (Throwable t) {
            throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.UNEXPECTED_SERVER_THROWABLE), t);
        }
    }

    //NOTE: we don't call recoverConnection from this method, because this method is already called from recoverConnection so
    //we want to avoid cycles.
    protected void setupNotificationArea() throws Throwable {
        final String sourceMethod = "setupNotificationArea";

        URL notificationsURL = null;
        HttpsURLConnection connection = null;
        try {
            // Get URL for creating a notification area
            notificationsURL = serverConnection.getNotificationsURL();

            if (logger.isLoggable(Level.FINEST)) {
                logger.logp(Level.FINEST, logger.getName(), sourceMethod, "[" + RESTClientMessagesUtil.getObjID(this) + "] About to call notificationURL: " + notificationsURL);
            }

            // Get connection to server
            connection = serverConnection.getConnection(notificationsURL, HttpMethod.POST, true);

            // Create NotificationSettings object
            NotificationSettings ns = new NotificationSettings();
            ns.deliveryInterval = serverConnection.getConnector().getNotificationDeliveryInterval();
            ns.inboxExpiry = serverConnection.getConnector().getNotificationInboxExpiry();

            // Write CreateMBean JSON to connection output stream
            OutputStream output = connection.getOutputStream();
            converter.writeNotificationSettings(output, ns);
            output.flush();
        } catch (ConnectException ce) {
            // Server is down; not a client bug
            throw ce;
        } catch (IOException io) {
            throw serverConnection.getRequestErrorException(sourceMethod, io, notificationsURL);
        }

        // Check response code from server
        final int responseCode = connection.getResponseCode();

        if (logger.isLoggable(Level.FINEST)) {
            logger.logp(Level.FINEST, logger.getName(), sourceMethod, "Received responseCode: " + responseCode);
        }

        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:
                JSONConverter converter = JSONConverter.getConverter();
                try {
                    // Process and return server response, which should be a NotificationArea
                    NotificationArea area = converter.readNotificationArea(connection.getInputStream());

                    inboxURL = new DynamicURL(serverConnection.connector, area.inboxURL);
                    registrationsURL = new DynamicURL(serverConnection.connector, area.registrationsURL);
                    serverRegistrationsURL = new DynamicURL(serverConnection.connector, area.serverRegistrationsURL);
                    notificationClientURL = new DynamicURL(serverConnection.connector, area.clientURL);

                    if (logger.isLoggable(Level.FINER)) {
                        logger.logp(Level.FINER, logger.getName(), "setupNotificationArea", "Successfully setup inboxURL: " + inboxURL.getURL());
                    }
                    break;
                } catch (Exception e) {
                    throw serverConnection.getResponseErrorException(sourceMethod, e, notificationsURL);
                } finally {
                    JSONConverter.returnConverter(converter);
                }
            case HttpURLConnection.HTTP_NOT_FOUND:
                throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.URL_NOT_FOUND));
            case HttpURLConnection.HTTP_UNAVAILABLE:
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                // Server response should be a serialized Throwable
                throw serverConnection.getServerThrowable(sourceMethod, connection);
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw serverConnection.getBadCredentialsException(responseCode, connection);
            default:
                throw serverConnection.getResponseCodeErrorException(sourceMethod, responseCode, connection);
        }
    }

    synchronized void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, IOException {
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "addNotificationListener", "[" + RESTClientMessagesUtil.getObjID(this) + "] objectName: " + name + " | listener: "
                                                                                  + listener + " | filter: " + filter + " | handback: "
                                                                                  + handback);
        }
        // Check if the MBean is registered and throw InstanceNotFoundException if not
        // (it is possible the registration will not need updating, so we do this check first)
        if (!serverConnection.isRegistered(name)) {
            throw new InstanceNotFoundException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.INSTANCE_NOT_FOUND, name));
        }

        // Build identifier for accessing the registration map.
        // If a routing context has been set up, include the routing info (host name, server name, server user dir) as part of the key.
        final NotificationTargetInformation nti;
        if (serverConnection.isServerLevelRouting()) {
            nti = new NotificationTargetInformation(name, serverConnection.mapRouting);
        } else {
            nti = new NotificationTargetInformation(name);
        }

        NotificationListenerEntry set = new NotificationListenerEntry(listener, filter, handback);
        ClientNotificationRegistration localRegistration = registrationMap.get(nti);
        if (localRegistration == null) {
            localRegistration = new ClientNotificationRegistration(nti);
            registrationMap.put(nti, localRegistration);
        }

        localRegistration.addNotificationListenerEntry(set);

        //ensure we're polling for notifications (will be no-op if we're already polling for notifications)
        serverConnection.setPollingMode(PollingMode.NOTIFICATION);
    }

    synchronized void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, IOException {
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "addNotificationListener", "[" + RESTClientMessagesUtil.getObjID(this) + "] objectName: " + name + " | listener: "
                                                                                  + listener + " | filter: " + filter + " | handback: " + handback);
        }

        // Build identifier for accessing the server registration list.
        // If a routing context has been set up, include the routing info (host name, server name, server user dir) as part of the key.
        final NotificationTargetInformation nti;
        if (serverConnection.isServerLevelRouting()) {
            nti = new NotificationTargetInformation(name, serverConnection.mapRouting);
        } else {
            nti = new NotificationTargetInformation(name);
        }

        try {
            updateServerNotificationRegistration(nti, listener, filter, handback, Operation.Add);
            serverRegistrationList.add(new ServerNotificationListenerEntry(nti, listener, filter, handback));
        } catch (ListenerNotFoundException lnf) {
            throw new IOException(lnf); // Should never happen
        }

        //ensure we're polling for notifications (will be no-op if we're already polling for notifications)
        serverConnection.setPollingMode(PollingMode.NOTIFICATION);
    }

    synchronized void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "removeNotificationListener", "[" + RESTClientMessagesUtil.getObjID(this) + "] objectName: " + name + " | listener: "
                                                                                     + listener);
        }

        // Build identifier for accessing the server registration list.
        // If a routing context has been set up, include the routing info (host name, server name, server user dir) as part of the key.
        final NotificationTargetInformation nti;
        if (serverConnection.isServerLevelRouting()) {
            nti = new NotificationTargetInformation(name, serverConnection.mapRouting);
        } else {
            nti = new NotificationTargetInformation(name);
        }

        updateServerNotificationRegistration(nti, listener, null, null, Operation.RemoveAll);
        Iterator<ServerNotificationListenerEntry> iterator = serverRegistrationList.iterator();
        while (iterator.hasNext()) {
            ServerNotificationListenerEntry entry = iterator.next();
            // Remove all registrations for the listener on the given MBean (ObjectName + routing info).
            if (entry.nti.equals(nti) && entry.listener.equals(listener)) {
                iterator.remove();
            }
        }

        cleanupIfEmpty();
    }

    synchronized void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "removeNotificationListener", "[" + RESTClientMessagesUtil.getObjID(this) + "] objectName: " + name + " | listener: "
                                                                                     + listener + " | filter: " + filter + " | handback: "
                                                                                     + handback);
        }

        // Build identifier for accessing the server registration list.
        // If a routing context has been set up, include the routing info (host name, server name, server user dir) as part of the key.
        final NotificationTargetInformation nti;
        if (serverConnection.isServerLevelRouting()) {
            nti = new NotificationTargetInformation(name, serverConnection.mapRouting);
        } else {
            nti = new NotificationTargetInformation(name);
        }

        updateServerNotificationRegistration(nti, listener, filter, handback, Operation.RemoveSpecific);
        serverRegistrationList.remove(new ServerNotificationListenerEntry(nti, listener, filter, handback));

        cleanupIfEmpty();
    }

    synchronized void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        removeNotificationListener(name, listener, null, null, true);
    }

    synchronized void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        removeNotificationListener(name, listener, filter, handback, false);
    }

    void close() {
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "close", "Closing registry " + RESTClientMessagesUtil.getObjID(this));
        }

        if (serverConnection.isConnected()) {
            sendClosingSignal();
        }

        JSONConverter.returnConverter(converter);
    }

    //This method sends a signal to the server-side indicating that the inbox can be deleted, because this client is being closed.
    //We don't throw any errors because the connector is about to be closed.
    private void sendClosingSignal() {
        URL clientURL = null;
        HttpsURLConnection connection = null;
        try {
            // Get the appropriate URL to delete notification client
            if (serverConnection.serverVersion >= 4) {
                //V4+ clients use /{clientID} to delete the notification client
                clientURL = getNotificationClientURL();
            } else {
                //Pre-V4 clients use /{clientID}/inbox to delete the notification client
                clientURL = getInboxURL();
            }

            if (logger.isLoggable(Level.FINEST)) {
                logger.logp(Level.FINEST,
                            logger.getName(),
                            "sendClosingSignal",
                            "Making a call to delete inbox [" + clientURL + "] from ["
                                            + RESTClientMessagesUtil.getObjID(this) + "]");
            }
            // Get connection to server
            connection = serverConnection.getConnection(clientURL, HttpMethod.DELETE, true);
            connection.setReadTimeout(serverConnection.getConnector().getReadTimeout());

            // Check response code from server
            int responseCode = 0;
            try {
                responseCode = connection.getResponseCode();
            } catch (ConnectException ce) {
                logger.logp(Level.FINE, logger.getName(), "sendClosingSignal", ce.getMessage(), ce);
            }

            if (logger.isLoggable(Level.FINEST)) {
                logger.logp(Level.FINEST, logger.getName(), "sendClosingSignal", "Response code: " + responseCode);
            }

        } catch (IOException io) {
            logger.logp(Level.FINE, logger.getName(), "sendClosingSignal", io.getMessage(), io);
        }
    }

    synchronized void connectionFailed(Throwable t) {
        serverConnection.connectionFailed(t);
    }

    private void updateServerNotificationRegistration(NotificationTargetInformation nti, ObjectName listener, NotificationFilter filter, Object handback, Operation operation) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        final String sourceMethod = "updateServerNotificationRegistration";

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), sourceMethod, "[" + RESTClientMessagesUtil.getObjID(this) + "] objectName: " + nti.getName() + " | listener: "
                                                                     + listener + " | filter: " + filter + " | handback: "
                                                                     + handback + " | operation: " + operation);
        }

        URL serverRegistrationsURL = null;
        HttpsURLConnection connection = null;
        try {
            // Get URL for creating/updating ServerNotificationRegistrations
            serverRegistrationsURL = getServerRegistrationsURL();

            // Get connection to server
            connection = serverConnection.getConnection(serverRegistrationsURL, HttpMethod.POST, true, nti.getRoutingInformation());

            // Create ServerNotificationRegistration object
            ServerNotificationRegistration snr = new ServerNotificationRegistration();
            snr.objectName = nti.getName();
            snr.listener = listener;
            if (operation == Operation.Add) {
                snr.filter = filter;
                snr.handback = handback;
            }
            if (operation != Operation.RemoveAll) {
                if (identityCache == null)
                    identityCache = new ObjectIdentityCache();
                snr.filterID = identityCache.getObjectIdentity(filter);
                snr.handbackID = identityCache.getObjectIdentity(handback);
            }
            snr.operation = operation;

            // Write NotificationRegsitration JSON to connection output stream
            OutputStream output = connection.getOutputStream();
            converter.writeServerNotificationRegistration(output, snr);
            output.flush();
            output.close();
        } catch (ConnectException ce) {
            // Server is down; not a client bug
            throw ce;
        } catch (IOException io) {
            throw serverConnection.getRequestErrorException(sourceMethod, io, serverRegistrationsURL);
        }

        // Check response code from server
        int responseCode = 0;
        try {
            responseCode = connection.getResponseCode();
        } catch (ConnectException ce) {
            serverConnection.recoverConnection(ce);
            // Server is down; not a client bug
            throw ce;
        }

        switch (responseCode) {
            case HttpURLConnection.HTTP_NO_CONTENT:
                // Nothing to do; no response expected
                break;
            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_NOT_FOUND:
                try {
                    // Server response should be a serialized Throwable, and if the notification area is gone,
                    // this connection is no longer valid
                    throw serverConnection.getServerThrowable(sourceMethod, connection);
                } catch (IOException io) {
                    serverConnection.recoverConnection(io);
                    throw io;
                } catch (Throwable t) {
                    throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.UNEXPECTED_SERVER_THROWABLE), t);
                }
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                try {
                    // Server response should be a serialized Throwable
                    throw converter.readThrowable(connection.getErrorStream());
                } catch (RuntimeOperationsException roe) {
                    throw roe;
                } catch (ListenerNotFoundException lnf) {
                    throw lnf;
                } catch (InstanceNotFoundException inf) {
                    throw inf;
                } catch (IOException io) {
                    throw io;
                } catch (ClassNotFoundException cnf) {
                    throw new IOException(cnf);
                } catch (Throwable t) {
                    throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.UNEXPECTED_SERVER_THROWABLE), t);
                }
            case HttpURLConnection.HTTP_UNAUTHORIZED:
            case HttpURLConnection.HTTP_FORBIDDEN:
                throw serverConnection.getBadCredentialsException(responseCode, connection);
            default:
                throw serverConnection.getResponseCodeErrorException(sourceMethod, responseCode, connection);
        }
    }

    private void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback, boolean matchListenerOnly) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "removeNotificationListener", "[" + RESTClientMessagesUtil.getObjID(this) + "] objectName: " + name + " | listener: "
                                                                                     + listener + " | filter: " + filter + " | handback: "
                                                                                     + handback + " | matchListenerOnly: " + matchListenerOnly);
        }

        // Check if the MBean is registered and throw InstanceNotFoundException if not
        // (it is possible the registration will not need updating, so we do this check first)
        if (!serverConnection.isRegistered(name)) {
            throw new InstanceNotFoundException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.INSTANCE_NOT_FOUND, name));
        }

        // Build identifier for accessing the registration map.
        // If a routing context has been set up, include the routing info (host name, server name, server user dir) as part of the key.
        final NotificationTargetInformation nti;
        if (serverConnection.isServerLevelRouting()) {
            nti = new NotificationTargetInformation(name, serverConnection.mapRouting);
        } else {
            nti = new NotificationTargetInformation(name);
        }

        ClientNotificationRegistration localRegistration = registrationMap.get(nti);
        if (localRegistration == null)
            throw new ListenerNotFoundException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.LISTENER_NOT_FOUND, name));

        localRegistration.removeNotificationListenerEntries(name, listener, filter, handback, matchListenerOnly);
        if (localRegistration.isEmpty()) {
            registrationMap.remove(nti);
        }

        cleanupIfEmpty();
    }

    /**
     * If there are no more registered listeners then we cleanup this registry and stop notification polling
     */
    private void cleanupIfEmpty() {
        if (registrationMap.isEmpty() && serverRegistrationList.isEmpty()) {
            serverConnection.discardNotificationRegistry();
        }
    }

    protected synchronized boolean restoreNotificationRegistrations(boolean sendRestoreNotification) {
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "restoreNotificationRegistrations", "[" + RESTClientMessagesUtil.getObjID(this) + "]");
        }

        List<NotificationTargetInformation> clientRemovalList = new ArrayList<NotificationTargetInformation>();
        List<ServerNotificationListenerEntry> serverRemovalList = new ArrayList<ServerNotificationListenerEntry>();
        List<Exception> exceptions = null;
        Iterator<Map.Entry<NotificationTargetInformation, ClientNotificationRegistration>> cnrIterator = registrationMap.entrySet().iterator();
        // Try to restore the registration for the notification listeners we've accumulated in the registration map.
        while (cnrIterator.hasNext()) {
            Map.Entry<NotificationTargetInformation, ClientNotificationRegistration> entry = cnrIterator.next();
            try {
                entry.getValue().createNotificationRegistration();
            } catch (IOException io) {
                // Unsuccessful
                return false;
            } catch (InstanceNotFoundException inf) {
                // Can continue, but need to note that some could not be restored
                if (exceptions == null)
                    exceptions = new ArrayList<Exception>();

                clientRemovalList.add(entry.getKey());
                exceptions.add(inf);
            }
        }

        for (ServerNotificationListenerEntry snl : serverRegistrationList) {
            try {
                updateServerNotificationRegistration(snl.nti, snl.listener, snl.filter, snl.handback, Operation.Add);
            } catch (IOException io) {
                // Unsuccessful
                return false;
            } catch (OperationsException oe) {
                // Can continue, but need to note that some could not be restored
                if (exceptions == null)
                    exceptions = new ArrayList<Exception>();

                serverRemovalList.add(snl);
                exceptions.add(oe);
            }
        }

        if (exceptions != null) {
            for (NotificationTargetInformation clientEntry : clientRemovalList)
                registrationMap.remove(clientEntry);
            for (ServerNotificationListenerEntry snl : serverRemovalList)
                serverRegistrationList.remove(snl);
        }

        if (sendRestoreNotification) {
            serverConnection.getConnector().connectionRestored(exceptions != null ? exceptions.toArray(new Exception[exceptions.size()]) : null);
        }

        return true;
    }

    protected URL getInboxURL() throws IOException {
        return inboxURL.getURL();
    }

    protected URL getNotificationClientURL() throws IOException {
        return notificationClientURL.getURL();
    }

    protected DynamicURL getRegistrationsURL() throws IOException {
        return registrationsURL;
    }

    protected URL getServerRegistrationsURL() throws IOException {
        return serverRegistrationsURL.getURL();
    }

    class ClientNotificationRegistration {
        private final NotificationTargetInformation listenerKey;
        private DynamicURL registrationURL;
        private final List<NotificationListenerEntry> entries;
        private List<NotificationFilter> serverFilters;

        ClientNotificationRegistration(NotificationTargetInformation listenerKey) {
            this.listenerKey = listenerKey;
            entries = new ArrayList<NotificationListenerEntry>();

            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, logger.getName(), "ClientNotificationRegistrion init", "[" + RESTClientMessagesUtil.getObjID(NotificationRegistry.this)
                                                                                                + "] | [" + RESTClientMessagesUtil.getObjID(this)
                                                                                                + "] | objectName: " + listenerKey.getName());
            }
        }

        synchronized void addNotificationListenerEntry(NotificationListenerEntry entry) throws InstanceNotFoundException, IOException {
            final boolean serverFiltersUpdated;

            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, logger.getName(), "addNotificationListenerEntry", "[" + RESTClientMessagesUtil.getObjID(this) + "]");
            }

            // Receive all notifications if there is no filter, or if it cannot be transmitted
            // to the server for server-side filtering
            final boolean receiveAllNotifications = entry.filter == null || !converter.isSupportedNotificationFilter(entry.filter);

            if (receiveAllNotifications) {
                if (serverFilters != null) {
                    serverFilters = null;
                    serverFiltersUpdated = true;
                } else {
                    serverFiltersUpdated = false;
                }
            } else if (serverFilters != null || entries.size() == 0) {
                // if there are already server-side filters, or there are no other notification sets,
                // the current filter can be added to the list
                if (serverFilters == null)
                    serverFilters = new ArrayList<NotificationFilter>();
                serverFilters.add(entry.filter);
                serverFiltersUpdated = true;
            } else {
                // there is already at least one notification set that cannot rely on
                // server side filtering
                serverFiltersUpdated = false;
            }

            entries.add(entry);

            if (registrationURL == null) {
                createNotificationRegistration();
            } else if (serverFiltersUpdated) {
                updateNotificationRegistration();
            }
        }

        synchronized void removeNotificationListenerEntries(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback, boolean matchListenerOnly) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
            boolean listenerFound = false;

            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, logger.getName(), "removeNotificationListenerEntries", "[" + RESTClientMessagesUtil.getObjID(this) + "] | objectName: "
                                                                                                + name + " | listener: " + listener + " | filter: " + filter + " | handback: "
                                                                                                + handback + " | matchListenerOnly: " + matchListenerOnly);
            }

            Iterator<NotificationListenerEntry> entryIterator = entries.iterator();
            while (entryIterator.hasNext()) {
                NotificationListenerEntry entry = entryIterator.next();
                if (entry.listener == listener && (matchListenerOnly || (entry.filter == filter && entry.handback == handback))) {
                    listenerFound = true;
                    entryIterator.remove();
                    if (!matchListenerOnly)
                        break;
                }
            }

            if (!listenerFound)
                throw new ListenerNotFoundException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.LISTENER_NOT_FOUND, name));

            List<NotificationFilter> newServerFilters = null;
            for (NotificationListenerEntry entry : entries) {
                if (entry.filter == null || !converter.isSupportedNotificationFilter(entry.filter)) {
                    newServerFilters = null;
                    break;
                } else {
                    if (newServerFilters == null)
                        newServerFilters = new ArrayList<NotificationFilter>();
                    newServerFilters.add(entry.filter);
                }
            }

            final boolean serverFiltersUpdated;
            if ((serverFilters != null && !serverFilters.equals(newServerFilters)) ||
                (serverFilters == null && newServerFilters != null)) {
                serverFiltersUpdated = true;
                serverFilters = newServerFilters;
            } else {
                serverFiltersUpdated = false;
            }

            if (isEmpty()) {
                deleteNotificationRegistration();
            } else if (serverFiltersUpdated) {
                updateNotificationRegistration();
            }
        }

        private NotificationFilter[] getServerFilters() {
            if (serverFilters == null)
                return null;
            NotificationFilter[] serverFilterArray = new NotificationFilter[serverFilters.size()];
            return serverFilters.toArray(serverFilterArray);
        }

        private URL getRegistrationURL() throws IOException {
            return registrationURL.getURL();
        }

        boolean isEmpty() {
            return entries.isEmpty();
        }

        synchronized void handleNotification(Notification notification) {
            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, logger.getName(), "handleNotification", "[" + RESTClientMessagesUtil.getObjID(this) +
                                                                                 " | Notification: " + notification);
            }

            for (NotificationListenerEntry entry : entries)
                entry.handleNotification(notification);
        }

        synchronized void createNotificationRegistration() throws InstanceNotFoundException, IOException {
            final String sourceMethod = "createNotificationRegistration";

            URL registrationsURL = null;
            HttpsURLConnection connection = null;
            try {
                // Get URL for creating/updating NotificationRegistrations
                registrationsURL = getRegistrationsURL().getURL();

                // Get connection to server
                connection = serverConnection.getConnection(registrationsURL, HttpMethod.POST, true, listenerKey.getRoutingInformation());

                // Create NotificationRegistration object
                NotificationRegistration nr = new NotificationRegistration();
                nr.objectName = listenerKey.getName();
                nr.filters = getServerFilters();

                if (logger.isLoggable(Level.FINER)) {
                    logger.logp(Level.FINER, logger.getName(), sourceMethod, RESTClientMessagesUtil.getObjID(this));
                }

                // Write NotificationRegsitration JSON to connection output stream
                OutputStream output = connection.getOutputStream();
                converter.writeNotificationRegistration(output, nr);
                output.flush();
                output.close();
            } catch (ConnectException ce) {
                serverConnection.recoverConnection(ce);
                // Server is down; not a client bug
                throw ce;
            } catch (IOException io) {
                throw serverConnection.getRequestErrorException(sourceMethod, io, registrationsURL);
            }

            // Check response code from server
            int responseCode = 0;
            try {
                responseCode = connection.getResponseCode();
            } catch (ConnectException ce) {
                serverConnection.recoverConnection(ce);
                // Server is down; not a client bug
                throw ce;
            }

            switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                    try {
                        // Process and return server response, which should be a URL
                        String url = converter.readString(connection.getInputStream());
                        registrationURL = new DynamicURL(serverConnection.connector, url);
                        break;
                    } catch (Exception e) {
                        throw serverConnection.getResponseErrorException(sourceMethod, e, registrationsURL);
                    }
                case HttpURLConnection.HTTP_GONE:
                case HttpURLConnection.HTTP_NOT_FOUND:
                    IOException ioe = serverConnection.getResponseCodeErrorException(sourceMethod, responseCode, connection);
                    serverConnection.recoverConnection(ioe);
                    throw ioe;
                case HttpURLConnection.HTTP_BAD_REQUEST:
                case HttpURLConnection.HTTP_INTERNAL_ERROR:
                    try {
                        // Server response should be a serialized Throwable
                        throw serverConnection.getServerThrowable(sourceMethod, connection);
                    } catch (RuntimeOperationsException roe) {
                        throw roe;
                    } catch (InstanceNotFoundException inf) {
                        throw inf;
                    } catch (IOException io) {
                        throw io;
                    } catch (ClassNotFoundException cnf) {
                        throw new IOException(cnf);
                    } catch (Throwable t) {
                        throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.UNEXPECTED_SERVER_THROWABLE), t);
                    }
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                case HttpURLConnection.HTTP_FORBIDDEN:
                    throw serverConnection.getBadCredentialsException(responseCode, connection);
                default:
                    throw serverConnection.getResponseCodeErrorException(sourceMethod, responseCode, connection);
            }

        }

        private void updateNotificationRegistration() throws InstanceNotFoundException, IOException {
            final String sourceMethod = "updateNotificationRegistration";

            URL registrationURL = null;
            HttpsURLConnection connection = null;
            try {
                // Get URL for this NotificationRegistration
                registrationURL = getRegistrationURL();

                if (logger.isLoggable(Level.FINER)) {
                    logger.logp(Level.FINER, logger.getName(), sourceMethod, RESTClientMessagesUtil.getObjID(this));
                }

                // Get connection to server
                connection = serverConnection.getConnection(registrationURL, HttpMethod.PUT, true, listenerKey.getRoutingInformation());

                // Write array of NotificationFilters to JSON to connection output stream
                OutputStream output = connection.getOutputStream();
                converter.writeNotificationFilters(output, getServerFilters());
                output.flush();
                output.close();
            } catch (ConnectException ce) {
                serverConnection.recoverConnection(ce);
                // Server is down; not a client bug
                throw ce;
            } catch (IOException io) {
                throw serverConnection.getRequestErrorException(sourceMethod, io, registrationURL);
            }

            // Check response code from server
            int responseCode = 0;
            try {
                responseCode = connection.getResponseCode();
            } catch (ConnectException ce) {
                serverConnection.recoverConnection(ce);
                // Server is down; not a client bug
                throw ce;
            }

            if (logger.isLoggable(Level.FINEST)) {
                logger.logp(Level.FINEST, logger.getName(), sourceMethod, "response code: " + responseCode);
            }
            switch (responseCode) {
                case HttpURLConnection.HTTP_NO_CONTENT:
                    // Nothing to do; no response expected
                    break;
                case HttpURLConnection.HTTP_GONE:
                case HttpURLConnection.HTTP_NOT_FOUND:
                    IOException ioe = serverConnection.getResponseCodeErrorException(sourceMethod, responseCode, connection);
                    serverConnection.recoverConnection(ioe);
                    throw ioe;
                case HttpURLConnection.HTTP_BAD_REQUEST:
                case HttpURLConnection.HTTP_INTERNAL_ERROR:
                    try {
                        // Server response should be a serialized Throwable
                        throw converter.readThrowable(connection.getErrorStream());
                    } catch (InstanceNotFoundException inf) {
                        throw inf;
                    } catch (IOException io) {
                        throw io;
                    } catch (ClassNotFoundException cnf) {
                        throw new IOException(cnf);
                    } catch (Throwable t) {
                        throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.UNEXPECTED_SERVER_THROWABLE), t);
                    }
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                case HttpURLConnection.HTTP_FORBIDDEN:
                    throw serverConnection.getBadCredentialsException(responseCode, connection);
                default:
                    throw serverConnection.getResponseCodeErrorException(sourceMethod, responseCode, connection);
            }
        }

        private void deleteNotificationRegistration() throws InstanceNotFoundException, ListenerNotFoundException, IOException {
            final String sourceMethod = "deleteNotificationRegistration";

            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, logger.getName(), sourceMethod, RESTClientMessagesUtil.getObjID(this));
            }

            URL registrationURL = null;
            HttpsURLConnection connection = null;
            try {
                // Get URL for NotificationRegistration
                registrationURL = getRegistrationURL();

                // Get connection to server
                connection = serverConnection.getConnection(registrationURL, HttpMethod.DELETE, true, listenerKey.getRoutingInformation());
            } catch (IOException io) {
                throw serverConnection.getRequestErrorException(sourceMethod, io, registrationURL);
            }

            // Check response code from server
            int responseCode = 0;
            try {
                responseCode = connection.getResponseCode();
            } catch (ConnectException ce) {
                serverConnection.recoverConnection(ce);
                // Server is down; not a client bug
                throw ce;
            }

            if (logger.isLoggable(Level.FINEST)) {
                logger.logp(Level.FINEST, logger.getName(), sourceMethod, "Response code: " + responseCode);
            }

            switch (responseCode) {
                case HttpURLConnection.HTTP_NO_CONTENT:
                    // Nothing to do; no response expected
                    break;
                case HttpURLConnection.HTTP_GONE:
                case HttpURLConnection.HTTP_NOT_FOUND:
                    IOException ioe = serverConnection.getResponseCodeErrorException(sourceMethod, responseCode, connection);
                    serverConnection.recoverConnection(ioe);
                    throw ioe;
                case HttpURLConnection.HTTP_BAD_REQUEST:
                case HttpURLConnection.HTTP_INTERNAL_ERROR:
                    try {
                        // Server response should be a serialized Throwable
                        throw converter.readThrowable(connection.getInputStream());
                    } catch (InstanceNotFoundException inf) {
                        throw inf;
                    } catch (ListenerNotFoundException lnf) {
                        throw lnf;
                    } catch (IOException io) {
                        throw io;
                    } catch (Throwable t) {
                        throw new IOException(RESTClientMessagesUtil.getMessage(RESTClientMessagesUtil.UNEXPECTED_SERVER_THROWABLE), t);
                    }
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                case HttpURLConnection.HTTP_FORBIDDEN:
                    throw serverConnection.getBadCredentialsException(responseCode, connection);
                default:
                    throw serverConnection.getResponseCodeErrorException(sourceMethod, responseCode, connection);
            }
        }

    }

    /**
     * @return the registrationMap
     */
    public Map<NotificationTargetInformation, ClientNotificationRegistration> getRegistrationMap() {
        return registrationMap;
    }
}