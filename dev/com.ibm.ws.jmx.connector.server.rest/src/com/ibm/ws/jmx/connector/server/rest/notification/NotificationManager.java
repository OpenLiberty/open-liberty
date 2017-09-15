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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.NotificationFilter;
import javax.management.ObjectName;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jmx.connector.converter.JSONConverter;
import com.ibm.ws.jmx.connector.converter.NotificationRecord;
import com.ibm.ws.jmx.connector.datatypes.NotificationArea;
import com.ibm.ws.jmx.connector.datatypes.NotificationRegistration;
import com.ibm.ws.jmx.connector.datatypes.NotificationSettings;
import com.ibm.ws.jmx.connector.datatypes.ServerNotificationRegistration;
import com.ibm.ws.jmx.connector.datatypes.ServerNotificationRegistration.Operation;
import com.ibm.ws.jmx.connector.server.rest.APIConstants;
import com.ibm.ws.jmx.connector.server.rest.helpers.ErrorHelper;
import com.ibm.ws.jmx.connector.server.rest.helpers.RESTHelper;
import com.ibm.wsspi.rest.handler.RESTRequest;

/**
 * This is the main/central class for the server connector's notification framework.
 */
public class NotificationManager {

    /**
     * The key to this map is the clientID and the value is the corresponding notification inbox for that client;
     */
    private final Map<Integer, ClientNotificationArea> inboxes;

    /**
     * This field is used to generate our client IDs
     */
    private int clientIDGenerator;

    private static final TraceComponent tc = Tr.register(NotificationManager.class,
                                                         APIConstants.TRACE_GROUP,
                                                         APIConstants.TRACE_BUNDLE_CORE);

    // -- Singleton access

    private NotificationManager() {
        inboxes = new ConcurrentHashMap<Integer, ClientNotificationArea>();
        clientIDGenerator = Integer.MIN_VALUE;
    }

    public static NotificationManager getNotificationManager() {
        return NotificationManagerSingleton.SINGLETON;
    }

    // This singleton pattern guarantees thread-safely and laziness.
    private static class NotificationManagerSingleton {
        public static final NotificationManager SINGLETON = new NotificationManager();
    }

    // -- Public Methods

    /**
     * Creates a new clientID, inbox and NotificationArea
     */
    public NotificationArea createNotificationArea(RESTRequest request, String basePath, NotificationSettings notificationSettings) {
        //Cleanup old clients
        cleanUp(request);

        //Fetch a new ID
        int clientID = getNewClientID();

        //Make the new inbox and insert into map
        ClientNotificationArea newInbox = new ClientNotificationArea(notificationSettings.deliveryInterval, notificationSettings.inboxExpiry, clientID);

        //Don't need to use "putIfAbsent" here because the key is guaranteed to be unique
        inboxes.put(clientID, newInbox);

        //Create the notification area to be returned to the client
        NotificationArea newArea = new NotificationArea();
        newArea.clientURL = basePath + clientID;
        newArea.inboxURL = newArea.clientURL + "/inbox";
        newArea.registrationsURL = newArea.clientURL + "/registrations";
        newArea.serverRegistrationsURL = newArea.clientURL + "/serverRegistrations";

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created notification area for client ID " + clientID);
        }

        return newArea;
    }

    public String[] getRegisteredClientNotifications(RESTRequest request, int clientID, JSONConverter converter) {
        ClientNotificationArea clientArea = getInboxIfAvailable(clientID, converter);

        return clientArea.getRegisteredListeners(request, converter);
    }

    public NotificationFilter[] getRegisteredFilters(RESTRequest request, int clientID, String objectName, JSONConverter converter) {
        ClientNotificationArea clientArea = getInboxIfAvailable(clientID, converter);

        return clientArea.getRegisteredFilters(request, objectName, converter);
    }

    /**
     * Add a new client notification for the given client
     */
    public String addClientNotification(RESTRequest request, int clientID, NotificationRegistration notificationRegistration, JSONConverter converter) {
        //Grab the client inbox
        ClientNotificationArea clientArea = getInboxIfAvailable(clientID, converter);

        //Create the client notification
        clientArea.addClientNotificationListener(request, notificationRegistration, converter);

        //Create the returning URL
        String encodedObjectName = null;
        try {
            encodedObjectName = URLEncoder.encode(notificationRegistration.objectName.getCanonicalName(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
        String registrationURL = APIConstants.JMX_CONNECTOR_API_ROOT_PATH + "/notifications/" + clientID + "/registrations/" + encodedObjectName;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Client[" + clientID + "] registered a client-side notification for ObjectName " + notificationRegistration.objectName.getCanonicalName());
        }

        return registrationURL;
    }

    /**
     * Update the list of filters for the client/MBean pair
     */
    public void updateClientNotification(RESTRequest request, int clientID, String objectNameStr, NotificationFilter[] filters, JSONConverter converter) {
        //Grab the client inbox
        ClientNotificationArea clientArea = getInboxIfAvailable(clientID, null);

        //Update the filters inside the listener
        clientArea.updateClientNotificationListener(request, objectNameStr, filters, converter);
    }

    /**
     * Removes a corresponding client notification for the given ObjectName
     */
    public void removeClientNotification(RESTRequest request, ObjectName objectName, int clientID) {
        //Get the client area 
        ClientNotificationArea clientArea = getInboxIfAvailable(clientID, null);

        //Remove the corresponding listener (will also remove from MBeanServer)
        clientArea.removeClientNotificationListener(request, objectName);
    }

    /**
     * Retrieve the list of client notifications
     */
    public NotificationRecord[] getClientInbox(int clientID) {
        //Fetch the inbox
        ClientNotificationArea inbox = getInboxIfAvailable(clientID, null);

        //Fetch the notifications (may wait if empty)
        return inbox.fetchNotifications();
    }

    /**
     * Handle a server notification registration.
     * This method is only invoked by our java client.
     */
    public void handleServerNotificationRegistration(RESTRequest request, int clientID, ServerNotificationRegistration serverNotificationRegistration,
                                                     JSONConverter converter) {
        //Get the client area
        ClientNotificationArea clientArea = getInboxIfAvailable(clientID, converter);

        if (serverNotificationRegistration.operation == Operation.RemoveAll) {
            //Unregister with our client area (will also unregister with the MBeanServer)
            clientArea.removeServerNotificationListener(request, serverNotificationRegistration, true, converter, false);

        } else {
            if (serverNotificationRegistration.operation == Operation.Add) {
                //Register with our client area (will also register with the MBeanServer)
                clientArea.addServerNotificationListener(request, serverNotificationRegistration, converter);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Added server-side notification");
                }

            } else {
                //Unregister with our client area (will also unregister with the MBeanServer)
                clientArea.removeServerNotificationListener(request, serverNotificationRegistration, false, converter, false);
            }
        }
    }

    /**
     * Add a new server notification registration.
     * This method is only invoked by clients other than our java client.
     */
    public String addServerNotificationHTTP(RESTRequest request, int clientID, ServerNotificationRegistration serverNotificationRegistration,
                                            JSONConverter converter) {
        //Get the client area
        ClientNotificationArea clientArea = getInboxIfAvailable(clientID, converter);

        //Since this is not coming from our java client, the registration object will not have any IDs on them. So set the IDs before continuing.
        serverNotificationRegistration.filterID = clientArea.getParamId(serverNotificationRegistration.filter);
        serverNotificationRegistration.handbackID = clientArea.getParamId(serverNotificationRegistration.handback);

        //Register with our client area (will also register with the MBeanServer)
        clientArea.addServerNotificationListener(request, serverNotificationRegistration, converter);

        //We encode the filter and handback IDs in the returning URL so that we can match these during removal time
        String returningURL = APIConstants.JMX_CONNECTOR_API_ROOT_PATH +
                              "/notifications/" +
                              clientID +
                              "/serverRegistrations/" +
                              RESTHelper.URLEncoder(serverNotificationRegistration.objectName.getCanonicalName(), converter) +
                              "/listeners/" +
                              RESTHelper.URLEncoder(serverNotificationRegistration.listener.getCanonicalName(), converter) +
                              "/ids/" +
                              serverNotificationRegistration.filterID + "_" + serverNotificationRegistration.handbackID;

        return returningURL;
    }

    /**
     * Return the filter and handback that were registered for the given server-side notification, represented
     * by the 3-tuple {source mbean, listener mbean, registrationID ID}.
     */
    public ServerNotificationRegistration getSpecificServerRegistration(RESTRequest request,
                                                                        int clientID,
                                                                        String source_objName,
                                                                        String listener_objName,
                                                                        String registrationID) {
        //Get the client area
        ClientNotificationArea clientArea = getInboxIfAvailable(clientID, null);

        List<ServerNotification> registrations = clientArea.getServerRegistrations(request, source_objName);

        if (registrations != null) {
            //Get the IDs from the request
            String[] artifacts = registrationID.split("_");
            int filterID = Integer.parseInt(artifacts[0]);
            int handbackID = Integer.parseInt(artifacts[1]);

            //loop registrations to match request
            for (ServerNotification registration : registrations) {
                if (registration.listener.getCanonicalName().equals(listener_objName) &&
                    registration.filter == filterID &&
                    registration.handback == handbackID) {
                    //found a matching registration, so build and return filter/handback objects (could be null too)
                    ServerNotificationRegistration returningRegistration = new ServerNotificationRegistration();
                    returningRegistration.filter = (NotificationFilter) clientArea.getObject(filterID, null, null);
                    returningRegistration.handback = clientArea.getObject(handbackID, null, null);
                    return returningRegistration;
                }
            }
        }

        return null;
    }

    /**
     * Delete server-side notifications, represented by the 3-tuple {source mbean, listener mbean, registration ID}.
     * Called from direct HTTP users.
     */
    public void deleteServerRegistrationHTTP(RESTRequest request,
                                             int clientID,
                                             ServerNotificationRegistration serverNotificationRegistration,
                                             String registrationID,
                                             JSONConverter converter) {
        //Get the client area
        ClientNotificationArea clientArea = getInboxIfAvailable(clientID, null);

        if (registrationID != null) {
            String[] artifacts = registrationID.split("_");
            int filterID = Integer.parseInt(artifacts[0]);
            int handbackID = Integer.parseInt(artifacts[1]);

            //Since this is not coming from our java client, the registration object will not have any IDs on them. So set the IDs before continuing.
            serverNotificationRegistration.filterID = filterID;
            serverNotificationRegistration.handbackID = handbackID;
        }

        clientArea.removeServerNotificationListener(request, serverNotificationRegistration, serverNotificationRegistration.operation == Operation.RemoveAll, converter,
                                                    true);
    }

    /**
     * Delete all registered server-side notifications for the given object name.
     * This can only be called from HTTP-direct clients
     */
    public void deleteRegisteredListeners(RESTRequest request, int clientID, ObjectName source_objName, JSONConverter converter) {
        //Get the client area
        ClientNotificationArea clientArea = getInboxIfAvailable(clientID, null);

        clientArea.removeAllListeners(request, source_objName, converter);
    }

    /**
     * Return an array of IDs (strings) that represent server-side notifications from the given source mbean to the given listener mbean.
     */
    public String[] getRegisteredIDs(RESTRequest request,
                                     int clientID,
                                     String source_objName,
                                     String listener_objName) {
        //Get the client area
        ClientNotificationArea clientArea = getInboxIfAvailable(clientID, null);

        List<ServerNotification> registrations = clientArea.getServerRegistrations(request, source_objName);

        if (registrations != null) {
            List<String> ids = new ArrayList<String>();

            //loop registrations to match request
            for (ServerNotification registration : registrations) {
                if (registration.listener.getCanonicalName().equals(listener_objName)) {
                    //found a matching registration, so add ID to list
                    ids.add(registration.filter + "_" + registration.handback);
                }
            }

            return ids.toArray(new String[ids.size()]);
        }

        return null;
    }

    /**
     * Returns an array of listener ObjectNames (as strings) that are listening for server-side notification from the
     * given source mbean.
     */
    public String[] getRegisteredListeners(RESTRequest request,
                                           int clientID,
                                           String source_objName) {
        //Get the client area
        ClientNotificationArea clientArea = getInboxIfAvailable(clientID, null);

        List<ServerNotification> registrations = clientArea.getServerRegistrations(request, source_objName);

        if (registrations != null) {
            List<String> listeners = new ArrayList<String>();

            //loop registrations to match request
            for (ServerNotification registration : registrations) {
                listeners.add(registration.listener.getCanonicalName());
            }

            return listeners.toArray(new String[listeners.size()]);
        }

        return null;
    }

    /**
     * Cleanup specified clientID
     */
    public void cleanUp(RESTRequest request, int clientID) {
        ClientNotificationArea inbox = inboxes.get(clientID);
        //If somehow the inbox timed out before we came here, then just exit, because the end result is the same.
        if (inbox != null) {
            inbox.cleanUp(request);
            inboxes.remove(clientID);
        }
    }

    public void removeAllClientRegistrations(RESTRequest request, int clientID) {
        ClientNotificationArea clientArea = getInboxIfAvailable(clientID, null);
        clientArea.remoteClientRegistrations(request);
    }

    public void removeAllServerRegistrations(RESTRequest request, int clientID) {
        ClientNotificationArea clientArea = getInboxIfAvailable(clientID, null);
        clientArea.remoteServerRegistrations(request);
    }

    /**
     * Clean up timed out clients.
     * 
     * TODO: If all clients leave and nobody ever comes up, then we won't cleanup. Need to
     * think of a place outside of notifications package that could trigger a cleanup.
     */
    public void cleanUp(RESTRequest request) {
        //We need to loop through the clients but we don't want to synchronize
        //the entire map, which would block new clients from entering and block existing 
        //clients from continuing their work.  So we iterate and possibly remove by just
        //using the built-in concurrency of ConcurrentHashMap.
        final Iterator<Entry<Integer, ClientNotificationArea>> clients = inboxes.entrySet().iterator();

        while (clients.hasNext()) {
            Entry<Integer, ClientNotificationArea> client = clients.next();

            //Check for time outs
            ClientNotificationArea inbox = client.getValue();
            if (inbox.timedOut()) {
                //cleanup
                inbox.cleanUp(request);

                //Emit the warning
                Tr.warning(tc, "jmx.connector.server.rest.notification.timeout.warning", client.getKey());

                //remove the client inbox from the map (using the Iterator.remove() method)
                clients.remove();
            }
        }

    }

    // -- Private Methods

    /**
     * This method returns the client inbox or throws an error if the client ID has timed out
     */
    private ClientNotificationArea getInboxIfAvailable(int clientID, JSONConverter converter) {
        final ClientNotificationArea inbox = inboxes.get(clientID);

        if (inbox == null) {
            throw ErrorHelper.createRESTHandlerJsonException(new RuntimeException("The requested clientID is no longer available because it timed out."),
                                                             converter,
                                                             APIConstants.STATUS_GONE);
        }

        return inbox;
    }

    /**
     * This synchronized method will assign an unique ID to a new notification client
     */
    private synchronized int getNewClientID() {
        if ((clientIDGenerator + 1) >= Integer.MAX_VALUE) {
            //if we can't find a reusable ID within the first 100 slots, then fail.
            //Revisit: I don't want to loop through all 2+ billion entries to find that they are all taken, as this
            //could be seem as a DoS, but maybe some better heuristics for finding an empty ID? not sure if we will ever be
            //in this situation...
            final int limit = Integer.MIN_VALUE + 100;
            for (int i = Integer.MIN_VALUE; i < limit; i++) {
                if (inboxes.get(i) == null) {
                    return i;
                }
            }

            //Emit the warning
            Tr.warning(tc, "jmx.connector.server.rest.notification.limit.warning");

            throw ErrorHelper.createRESTHandlerJsonException(new RuntimeException("The server has reached its limit of new client notifications."),
                                                             JSONConverter.getConverter(),
                                                             APIConstants.STATUS_SERVICE_UNAVAILABLE);
        }

        return clientIDGenerator++;
    }

}
