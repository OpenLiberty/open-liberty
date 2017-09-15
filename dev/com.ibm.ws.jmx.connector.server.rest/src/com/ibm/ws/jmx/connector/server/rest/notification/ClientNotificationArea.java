/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.server.rest.notification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.ObjectName;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.jmx.connector.rest.ConnectorSettings;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jmx.connector.converter.JSONConverter;
import com.ibm.ws.jmx.connector.converter.NotificationRecord;
import com.ibm.ws.jmx.connector.converter.NotificationTargetInformation;
import com.ibm.ws.jmx.connector.datatypes.NotificationRegistration;
import com.ibm.ws.jmx.connector.datatypes.ServerNotificationRegistration;
import com.ibm.ws.jmx.connector.server.rest.APIConstants;
import com.ibm.ws.jmx.connector.server.rest.helpers.ErrorHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.MBeanRoutedNotificationHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.MBeanServerHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.RESTHelper;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.helper.RESTHandlerJsonException;

/**
 * Represents a single client's notification area
 */
public class ClientNotificationArea {

    /**
     * Represents the unique ID of this notification client.
     */
    public final int clientID;

    /**
     * This represents the amount of time allowed for a client to be idle and not poll the server
     * for notifications. If a client does not poll the server within this period of time the server
     * will be allowed to delete the client ID.
     * 
     * 
     * Time unit is milliseconds.
     */
    public final long inboxExpiry;

    /**
     * This represents the amount of time the server will remain in "standby" and wait for notifications
     * to arrive after it has been polled.
     * 
     * Time unit is milliseconds.
     */
    public final long deliveryInterval;

    /**
     * Timestamp for the last time this area was accessed by the client
     */
    private volatile long lastAccessTimeStamp;

    /**
     * Current unfetched list of notifications for this client
     */
    private final List<NotificationRecord> notifications;

    /**
     * Flag used to synchronize stand-by operations
     */
    private final Object waitFlag = new WaitFlag();

    /**
     * Inner class to use for the standby-lock (as suggested by findbugs)
     */
    private class WaitFlag {

    }

    /**
     * The key to this map is a NotificationTargetInformation (containing routing information
     * and the MBean's ObjectName) and the value is the corresponding MBean Listener which also
     * contains a list of clientIDs that are registered to received notifications for that MBean.
     */
    private final ConcurrentHashMap<NotificationTargetInformation, ClientNotificationListener> listeners;

    /**
     * A library of filter/handback objects used by this client in the server notifications.
     */
    private final ConcurrentHashMap<Integer, Object> objectLibrary;

    /**
     * Integer used for direct-http management of server-side notifications. Integer.MIN_VALUE is reserved for null
     */
    private int paramId = Integer.MIN_VALUE + 1;

    /**
     * A map of server notifications that we keep in order to cleanup.
     */
    private final ConcurrentHashMap<NotificationTargetInformation, List<ServerNotification>> serverNotifications;

    private static final TraceComponent tc = Tr.register(ClientNotificationArea.class,
                                                         APIConstants.TRACE_GROUP,
                                                         APIConstants.TRACE_BUNDLE_CORE);

    /**
     * Constructor
     */
    public ClientNotificationArea(long deliveryInterval, long inboxExpiry, int clientID) {
        this.clientID = clientID;
        this.deliveryInterval = deliveryInterval * 1000000L; // Convert to nanoSeconds
        this.inboxExpiry = inboxExpiry * 1000000L; // Convert to nanoSeconds
        this.lastAccessTimeStamp = System.nanoTime();
        this.notifications = Collections.synchronizedList(new ArrayList<NotificationRecord>());
        this.serverNotifications = new ConcurrentHashMap<NotificationTargetInformation, List<ServerNotification>>();
        this.listeners = new ConcurrentHashMap<NotificationTargetInformation, ClientNotificationListener>();
        this.objectLibrary = new ConcurrentHashMap<Integer, Object>();
    }

    /**
     * Check if the client has timed out
     */
    public boolean timedOut() {
        return (System.nanoTime() - lastAccessTimeStamp) > inboxExpiry;
    }

    /**
     * This method will be called by the NotificationListener once the MBeanServer pushes a notification.
     */
    public void addNotfication(Notification notification) {
        Object source = notification.getSource();
        NotificationRecord nr;
        if (source instanceof ObjectName) {
            nr = new NotificationRecord(notification, (ObjectName) source);
        } else {
            nr = new NotificationRecord(notification, (source != null) ? source.toString() : null);
        }
        addNotficationRecord(nr);
    }

    /**
     * This method will be called by addNotfication(Notification) or by the NotificationListener
     * once the Target-Client Manager pushes a routed notification.
     */
    public void addNotficationRecord(NotificationRecord record) {
        final boolean wasEmpty = notifications.isEmpty();
        notifications.add(record);

        if (wasEmpty) {
            //The list was previously empty, so notify a possible waiting thread
            synchronized (waitFlag) {
                waitFlag.notifyAll();
            }
        }
    }

    /**
     * The client requested to fetch the notification. If the list is not empty, we build the array right away.
     * If the list is empty, we wait for whichever event comes first: (1) we time out, (2) we receive a notification
     */
    public NotificationRecord[] fetchNotifications() {
        if (notifications.isEmpty()) {
            //Need to have a timedOut flag because of possible spurious wakeup
            final long timedOut = System.nanoTime() + deliveryInterval;

            synchronized (waitFlag) {
                while (notifications.isEmpty() && (System.nanoTime() < timedOut)) {
                    try {
                        waitFlag.wait(deliveryInterval / 1000000L);
                    } catch (InterruptedException e) {
                        //keep waiting until conditions aren't met anymore
                    }
                }
            }
        }

        NotificationRecord[] fetchedNotifications = null;

        if (!notifications.isEmpty()) {
            synchronized (notifications) {
                fetchedNotifications = notifications.toArray(new NotificationRecord[notifications.size()]);
                notifications.clear();
            }
        }

        //Update the timestamp
        lastAccessTimeStamp = System.nanoTime();

        return fetchedNotifications;
    }

    /**
     * Builds an instance of NotificationTargetInformation from the headers of an RESTRequest and a JMX ObjectName (as a string).
     */
    private NotificationTargetInformation toNotificationTargetInformation(RESTRequest request, String objectName) {
        //Handle incoming routing context (if applicable)
        String[] routingContext = RESTHelper.getRoutingContext(request, false);

        if (routingContext != null) {
            return new NotificationTargetInformation(objectName, routingContext[0], routingContext[2], routingContext[1]);
        }

        return new NotificationTargetInformation(objectName);
    }

    /**
     * Fetch or create a new listener for the given object name
     */
    public void addClientNotificationListener(RESTRequest request, NotificationRegistration notificationRegistration, JSONConverter converter) {
        String objectNameStr = notificationRegistration.objectName.getCanonicalName();

        NotificationTargetInformation nti = toNotificationTargetInformation(request, objectNameStr);

        // Get the listener
        ClientNotificationListener listener = listeners.get(nti);
        if (listener == null) {
            listener = new ClientNotificationListener(this);
            ClientNotificationListener mapListener = listeners.putIfAbsent(nti, listener);

            if (mapListener != null) {
                listener = mapListener;
            }
        }

        // Grab the wrapper filter which will be registered in the MBeanServer
        NotificationFilter filter = listener.getClientWrapperFilter();

        // Check whether the producer of the notification is local or remote.
        if (nti.getRoutingInformation() == null) {
            // Add the notification in the MBeanServer        
            MBeanServerHelper.addClientNotification(notificationRegistration.objectName,
                                                    listener,
                                                    filter,
                                                    null,
                                                    converter);
        } else {
            // Add the notification listener to the Target-Client Manager through EventAdmin
            MBeanRoutedNotificationHelper helper = MBeanRoutedNotificationHelper.getMBeanRoutedNotificationHelper();
            helper.addRoutedNotificationListener(nti, listener, converter);
        }

        // Add the filters to the listener
        listener.addClientNotification(notificationRegistration.filters);
    }

    /**
     * Update the listener for the given object name with the provided filters
     */
    public void updateClientNotificationListener(RESTRequest request, String objectNameStr, NotificationFilter[] filters, JSONConverter converter) {
        NotificationTargetInformation nti = toNotificationTargetInformation(request, objectNameStr);

        ClientNotificationListener listener = listeners.get(nti);

        if (listener == null) {
            throw ErrorHelper.createRESTHandlerJsonException(new RuntimeException("There are no listeners registered for " + nti), converter, APIConstants.STATUS_BAD_REQUEST);
        }

        //Make the update
        listener.addClientNotification(filters);
    }

    public int getParamId(Object obj) {
        if (obj == null) {
            return Integer.MIN_VALUE;
        }

        if (paramId == Integer.MAX_VALUE) {
            //we have reached the end of our values.  Go through the keys of our library and use
            //the first spot that has null as the value

            for (int i = Integer.MIN_VALUE + 1; i < Integer.MAX_VALUE; i++) {
                if (objectLibrary.get(i) == null) {
                    return i;
                }
            }

            //No IDs are available, throw error message
            IOException ioe = new IOException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                           APIConstants.TRACE_BUNDLE_CORE,
                                                                           "JMX_HTTP_MBEAN_LISTENER_LIMIT_ERROR",
                                                                           new Object[] { clientID },
                                                                           "CWWKX0104W: The notification client with id {0} has reached its limit of concurrent MBean listener registrations."));
            throw ErrorHelper.createRESTHandlerJsonException(ioe, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }

        return paramId++;
    }

    /**
     * Only to be used by removal coming from direct-http calls, because calls from the jmx client
     * can still reference this key for other notifications.
     */
    private Object removeObject(Integer key) {
        if (key == null) {
            return null;
        }
        return objectLibrary.remove(key);
    }

    /**
     * Clears the entire object library. This happens if all the server notifications pertaining to this client are being unregistered.
     */
    private void clearObjectLibrary() {
        objectLibrary.clear();
    }

    /**
     * Fetch the object (or put if absent) corresponding to the given ID
     */
    public Object getObject(Integer key, Object newValue, JSONConverter converter) {
        if (newValue == null && key == Integer.MIN_VALUE) {
            return null;
        }

        Object previousValue = objectLibrary.get(key);

        if (previousValue == null) {

            if (newValue == null) {
                throw ErrorHelper.createRESTHandlerJsonException(new ListenerNotFoundException("Could not match Filter and Handback objects"),
                                                                 converter,
                                                                 APIConstants.STATUS_BAD_REQUEST);
            }

            previousValue = objectLibrary.putIfAbsent(key, newValue);

            if (previousValue == null) {
                previousValue = newValue;
            }
        }

        return previousValue;
    }

    /**
     * Remove the given NotificationListener
     */
    public void removeClientNotificationListener(RESTRequest request, ObjectName name) {
        NotificationTargetInformation nti = toNotificationTargetInformation(request, name.getCanonicalName());

        // Remove locally
        ClientNotificationListener listener = listeners.remove(nti);

        // Check whether the producer of the notification is local or remote.
        if (nti.getRoutingInformation() == null) {
            // Remove the notification from the MBeanServer
            MBeanServerHelper.removeClientNotification(name, listener);
        } else {
            // Remove the notification listener from the Target-Client Manager through EventAdmin
            MBeanRoutedNotificationHelper helper = MBeanRoutedNotificationHelper.getMBeanRoutedNotificationHelper();
            helper.removeRoutedNotificationListener(nti, listener);
        }
    }

    /**
     * Remove the appropriate server notifications from our list
     */
    public void removeAllListeners(RESTRequest request, ObjectName source_objName, JSONConverter converter) {
        NotificationTargetInformation nti = toNotificationTargetInformation(request, source_objName.getCanonicalName());

        //Get the corresponding List for the given ObjectName
        List<ServerNotification> notifications = serverNotifications.get(nti);

        if (notifications == null) {
            throw ErrorHelper.createRESTHandlerJsonException(new ListenerNotFoundException("There are no listeners registered for ObjectName: " + source_objName.getCanonicalName()),
                                                             converter,
                                                             APIConstants.STATUS_BAD_REQUEST);
        }

        Iterator<ServerNotification> notificationsIter = notifications.iterator();
        while (notificationsIter.hasNext()) {
            ServerNotification currentRegistration = notificationsIter.next();

            if (nti.getRoutingInformation() == null) {
                //Remove from MBeanServer               
                MBeanServerHelper.removeServerNotification(source_objName, //no need to make another ObjectName
                                                           currentRegistration.listener,
                                                           (NotificationFilter) getObject(currentRegistration.filter, null, converter),
                                                           getObject(currentRegistration.handback, null, converter),
                                                           converter);
            } else {
                // Remove the notification listener from the Target-Client Manager through EventAdmin
                MBeanRoutedNotificationHelper helper = MBeanRoutedNotificationHelper.getMBeanRoutedNotificationHelper();
                helper.removeRoutedServerNotificationListener(nti,
                                                              currentRegistration.listener,
                                                              (NotificationFilter) getObject(currentRegistration.filter, null, converter),
                                                              getObject(currentRegistration.handback, null, converter),
                                                              converter);
            }
            //Remove from local list (through iterator)
            notificationsIter.remove();

            //Clean up library
            removeObject(currentRegistration.filter);
            removeObject(currentRegistration.handback);
        }
    }

    /**
     * Remove the appropriate server notifications from our list
     */
    public void removeServerNotificationListener(RESTRequest request, ServerNotificationRegistration removedRegistration, final boolean removeAll,
                                                 JSONConverter converter, boolean cleanupHttpIDs) {
        NotificationTargetInformation nti = toNotificationTargetInformation(request, removedRegistration.objectName.getCanonicalName());

        //Get the corresponding List for the given ObjectName
        List<ServerNotification> notifications = serverNotifications.get(nti);

        if (notifications == null) {
            //MBean is registered, but listener is not
            throw ErrorHelper.createRESTHandlerJsonException(new ListenerNotFoundException("There are no listeners registered for ObjectName: "
                                                                                           + removedRegistration.objectName.getCanonicalName()),
                                                             converter,
                                                             APIConstants.STATUS_BAD_REQUEST);

        }

        Iterator<ServerNotification> notificationsIter = notifications.iterator();
        boolean foundMatch = false;
        while (notificationsIter.hasNext()) {
            ServerNotification currentRegistration = notificationsIter.next();

            //Check if we match the Listener
            if (currentRegistration.listener.equals(removedRegistration.listener)) {
                if (removeAll ||
                    (currentRegistration.filter == removedRegistration.filterID &&
                    currentRegistration.handback == removedRegistration.handbackID)) {
                    // Check whether the producer of the notification is local or remote.
                    if (nti.getRoutingInformation() == null) {
                        //Remove from MBeanServer
                        MBeanServerHelper.removeServerNotification(removedRegistration.objectName, //no need to make another ObjectName
                                                                   currentRegistration.listener,
                                                                   (NotificationFilter) getObject(currentRegistration.filter, null, converter),
                                                                   getObject(currentRegistration.handback, null, converter),
                                                                   converter);
                    } else {
                        // Remove the notification listener from the Target-Client Manager through EventAdmin
                        MBeanRoutedNotificationHelper helper = MBeanRoutedNotificationHelper.getMBeanRoutedNotificationHelper();
                        helper.removeRoutedServerNotificationListener(nti,
                                                                      currentRegistration.listener,
                                                                      (NotificationFilter) getObject(currentRegistration.filter, null, converter),
                                                                      getObject(currentRegistration.handback, null, converter),
                                                                      converter);
                    }
                    //Remove from local list (through iterator)
                    notificationsIter.remove();

                    if (cleanupHttpIDs) {
                        removeObject(currentRegistration.filter);
                        removeObject(currentRegistration.handback);
                    }

                    foundMatch = true;
                }
            }
        }

        if (!foundMatch) {
            if (removeAll) {
                throw ErrorHelper.createRESTHandlerJsonException(new ListenerNotFoundException("Could not match given Listener to ObjectName: "
                                                                                               + removedRegistration.objectName.getCanonicalName()),
                                                                 converter,
                                                                 APIConstants.STATUS_BAD_REQUEST);
            } else {
                throw ErrorHelper.createRESTHandlerJsonException(new ListenerNotFoundException("Could not match given Listener, Filter and Handback to ObjectName: "
                                                                                               + removedRegistration.objectName.getCanonicalName()),
                                                                 converter,
                                                                 APIConstants.STATUS_BAD_REQUEST);
            }
        }
    }

    public List<ServerNotification> getServerRegistrations(RESTRequest request, String objectName) {
        NotificationTargetInformation nti = toNotificationTargetInformation(request, objectName);
        return serverNotifications.get(nti);
    }

    /**
     * Add the server notification to our internal list so we can cleanup afterwards
     */
    public void addServerNotificationListener(RESTRequest request, ServerNotificationRegistration serverNotificationRegistration, JSONConverter converter) {
        NotificationTargetInformation nti = toNotificationTargetInformation(request, serverNotificationRegistration.objectName.getCanonicalName());

        //Fetch the filter/handback objects
        NotificationFilter filter = (NotificationFilter) getObject(serverNotificationRegistration.filterID, serverNotificationRegistration.filter, converter);
        Object handback = getObject(serverNotificationRegistration.handbackID, serverNotificationRegistration.handback, converter);

        // Check whether the producer of the notification is local or remote.
        if (nti.getRoutingInformation() == null) {
            //Add to the MBeanServer
            MBeanServerHelper.addServerNotification(serverNotificationRegistration.objectName,
                                                    serverNotificationRegistration.listener,
                                                    filter,
                                                    handback,
                                                    converter);
        } else {
            // Add the notification listener to the Target-Client Manager through EventAdmin
            MBeanRoutedNotificationHelper helper = MBeanRoutedNotificationHelper.getMBeanRoutedNotificationHelper();
            helper.addRoutedServerNotificationListener(nti,
                                                       serverNotificationRegistration.listener,
                                                       filter,
                                                       handback,
                                                       converter);
        }

        //Make the local ServerNotification
        ServerNotification serverNotification = new ServerNotification();
        serverNotification.listener = serverNotificationRegistration.listener;
        serverNotification.filter = serverNotificationRegistration.filterID;
        serverNotification.handback = serverNotificationRegistration.handbackID;

        //See if there's a list already
        List<ServerNotification> list = serverNotifications.get(nti);

        if (list == null) {
            list = Collections.synchronizedList(new ArrayList<ServerNotification>());
            List<ServerNotification> mapList = serverNotifications.putIfAbsent(nti, list);

            if (mapList != null) {
                list = mapList;
            }
        }

        //Add the new notification into the list
        list.add(serverNotification);
    }

    /**
     * Remove all the client notifications
     */
    public synchronized void remoteClientRegistrations(RESTRequest request) {
        Iterator<Entry<NotificationTargetInformation, ClientNotificationListener>> clientListeners = listeners.entrySet().iterator();

        try {
            while (clientListeners.hasNext()) {
                Entry<NotificationTargetInformation, ClientNotificationListener> clientListener = clientListeners.next();
                NotificationTargetInformation nti = clientListener.getKey();
                ClientNotificationListener listener = clientListener.getValue();

                // Check whether the producer of the notification is local or remote.
                if (nti.getRoutingInformation() == null) {
                    // Remove the notification from the MBeanServer
                    ObjectName objName = RESTHelper.objectNameConverter(nti.getNameAsString(), false, null);

                    if (MBeanServerHelper.isRegistered(objName)) {
                        try {
                            MBeanServerHelper.removeClientNotification(objName, listener);
                        } catch (RESTHandlerJsonException exception) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Received exception while cleaning up: " + exception);
                            }
                        }
                    }
                    else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "The MBean " + objName + " is not registered with the MBean server.");
                        }
                    }
                } else {
                    // Remove the notification listener from the Target-Client Manager through EventAdmin
                    MBeanRoutedNotificationHelper helper = MBeanRoutedNotificationHelper.getMBeanRoutedNotificationHelper();
                    helper.removeRoutedNotificationListener(nti, listener);
                }
            }
        } finally {
            //Clear the map
            listeners.clear();
        }
    }

    /**
     * Remove all the server notifications
     */
    public synchronized void remoteServerRegistrations(RESTRequest request) {
        Iterator<Entry<NotificationTargetInformation, List<ServerNotification>>> serverNotificationsIter = serverNotifications.entrySet().iterator();
        while (serverNotificationsIter.hasNext()) {
            Entry<NotificationTargetInformation, List<ServerNotification>> entry = serverNotificationsIter.next();
            NotificationTargetInformation nti = entry.getKey();

            // Check whether the producer of the notification is local or remote.
            if (nti.getRoutingInformation() == null) {
                //Traverse the list for that ObjectName
                ObjectName objName = RESTHelper.objectNameConverter(nti.getNameAsString(), false, null);

                if (MBeanServerHelper.isRegistered(objName)) {
                    for (ServerNotification notification : entry.getValue()) {
                        MBeanServerHelper.removeServerNotification(objName,
                                                                   notification.listener,
                                                                   (NotificationFilter) getObject(notification.filter, null, null),
                                                                   getObject(notification.handback, null, null),
                                                                   null);
                    }
                }
                else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "The MBean is not registered with the MBean server.");
                    }
                }
            } else {
                // Remove the notification listener from the Target-Client Manager through EventAdmin
                MBeanRoutedNotificationHelper helper = MBeanRoutedNotificationHelper.getMBeanRoutedNotificationHelper();
                for (ServerNotification notification : entry.getValue()) {
                    helper.removeRoutedServerNotificationListener(nti,
                                                                  notification.listener,
                                                                  (NotificationFilter) getObject(notification.filter, null, null),
                                                                  getObject(notification.handback, null, null),
                                                                  null);
                }
            }
        }
        //Clear the map
        serverNotifications.clear();

        //We don't have any more server notifications, so we can clear our library
        clearObjectLibrary();
    }

    /**
     * Remove the client and server notifications that this client registered
     */
    public void cleanUp(RESTRequest request) {
        remoteClientRegistrations(request);
        remoteServerRegistrations(request);
    }

    public NotificationFilter[] getRegisteredFilters(RESTRequest request, String objectNameStr, JSONConverter converter) {
        NotificationTargetInformation nti = toNotificationTargetInformation(request, objectNameStr);

        ClientNotificationListener listener = listeners.get(nti);

        if (listener == null) {
            return null;
        }

        return listener.getClientWrapperFilter().getFilters();
    }

    public String[] getRegisteredListeners(RESTRequest request, JSONConverter converter) {
        Set<NotificationTargetInformation> keys = listeners.keySet();

        if (!keys.isEmpty()) {
            //Get the routing context, if any
            final String[] routingContext = RESTHelper.getRoutingContext(request, false);

            //This set will hold the object names that match our routing context detail (could be null, in which 
            //case we don't want to return routed registrations)
            final Set<String> objectNames = new HashSet<String>();

            for (NotificationTargetInformation key : keys) {
                Map<String, Object> registeredRoutingInfo = key.getRoutingInformation();
                if (routingContext == null && registeredRoutingInfo == null) {
                    //we didn't request routing and we found a registration that didn't have routing, so add to set
                    objectNames.add(key.getNameAsString());

                } else if (routingContext != null &&
                           registeredRoutingInfo != null &&
                           routingContext[0].equals(registeredRoutingInfo.get(ConnectorSettings.ROUTING_KEY_HOST_NAME)) &&
                           routingContext[1].equals(registeredRoutingInfo.get(ConnectorSettings.ROUTING_KEY_SERVER_USER_DIR)) &&
                           routingContext[2].equals(registeredRoutingInfo.get(ConnectorSettings.ROUTING_KEY_SERVER_NAME))) {
                    //this registration matched our routing information, so add to set
                    objectNames.add(key.getNameAsString());

                }
            }

            //Return the list
            return objectNames.toArray(new String[objectNames.size()]);
        }

        return null;
    }
}
