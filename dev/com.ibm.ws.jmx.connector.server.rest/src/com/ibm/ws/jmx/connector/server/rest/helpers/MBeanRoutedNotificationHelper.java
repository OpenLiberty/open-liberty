/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.server.rest.helpers;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.ObjectName;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jmx.connector.client.rest.ClientProvider;
import com.ibm.ws.jmx.connector.converter.JSONConverter;
import com.ibm.ws.jmx.connector.converter.NotificationRecord;
import com.ibm.ws.jmx.connector.converter.NotificationTargetInformation;
import com.ibm.ws.jmx.connector.server.rest.APIConstants;
import com.ibm.ws.jmx.connector.server.rest.notification.ClientNotificationListener;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.rest.handler.helper.RESTRoutingHelper;

/**
 * This service component provides support for exchanging EventAdmin events
 * with the Target-Client Manager (in the collective / management component)
 * for registering / unregistering JMX notification listeners and for
 * reception / forwarding of routed notifications.
 */
@Component(service = { MBeanRoutedNotificationHelper.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM" })
public class MBeanRoutedNotificationHelper {

    private static final TraceComponent tc = Tr.register(MBeanRoutedNotificationHelper.class);
    private static final String KEY_ROUTING_HELPER = "routingHelper";
    private static final String KEY_EVENT_ADMIN = "eventAdmin";

    /**
     * Event-related topics and properties.
     */
    private static final String REGISTER_JMX_NOTIFICATION_LISTENER_TOPIC = "com/ibm/ws/management/repository/TargetClientManager/registerJmxNotificationListener";
    private static final String HANDLE_NOTIFICATION_TOPIC = "com/ibm/ws/management/repository/InboxManager/handleNotification";

    private static final String OPERATION_PROP = "operation";
    private static final String OPERATION_PROP_VALUE_ADD = "add";
    private static final String OPERATION_PROP_VALUE_REMOVE = "remove";
    private static final String OPERATION_PROP_VALUE_ADD_SERVER_NOTIFICATION = "addServerSideNotificationListener";
    private static final String OPERATION_PROP_VALUE_REMOVE_SERVER_NOTIFICATION = "removeServerSideNotificationListener";

    private static final String OBJECT_NAME_PROP = "objectName";
    private static final String LISTENER_OBJECT_NAME_PROP = "listenerObjectName";
    private static final String NOTIFICATION_FILTER_PROP = "filter";
    private static final String HANDBACK_OBJECT_PROP = "handback";
    private static final String NOTIFICATION_PROP = "notification";

    /**
     * Map containing all active ClientNotificationListeners that receive notifications from routed targets.
     */
    private final Map<NotificationTargetInformation, Set<ClientNotificationListener>> listenerMap = new HashMap<NotificationTargetInformation, Set<ClientNotificationListener>>();

    private final AtomicServiceReference<RESTRoutingHelper> routingHelperRef = new AtomicServiceReference<RESTRoutingHelper>(KEY_ROUTING_HELPER);
    private final AtomicServiceReference<EventAdmin> eventAdminRef = new AtomicServiceReference<EventAdmin>(KEY_EVENT_ADMIN);
    private final AtomicReference<ServiceRegistration<EventHandler>> eventHandlerServiceRegRef = new AtomicReference<ServiceRegistration<EventHandler>>();

    private static ComponentContext componentContext;

    @Activate
    protected void activate(ComponentContext cc) {
        routingHelperRef.activate(cc);
        eventAdminRef.activate(cc);

        // Set up an EventHandler for Notifications from the Target-Client Manager.
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("service.vendor", "IBM");
        props.put(EventConstants.EVENT_TOPIC, HANDLE_NOTIFICATION_TOPIC);
        BundleContext bc = cc.getBundleContext();
        ServiceRegistration<EventHandler> eventHandlerServiceReg =
                        bc.registerService(EventHandler.class, new NotificationEventHandler(), props);
        eventHandlerServiceRegRef.set(eventHandlerServiceReg);

        if (tc.isEventEnabled()) {
            Tr.event(tc, this.getClass().getSimpleName() + " has been activated.");
        }

        componentContext = cc;
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        if (tc.isEventEnabled()) {
            Tr.event(tc, this.getClass().getSimpleName() + " has been deactivated.");
        }

        routingHelperRef.deactivate(cc);
        eventAdminRef.deactivate(cc);

        // Unregister the NotifcationEventHandler when the MBeanRoutedNotificationHelper is deactivated.
        ServiceRegistration<EventHandler> eventHandlerServiceReg = eventHandlerServiceRegRef.get();
        if (eventHandlerServiceReg != null) {
            eventHandlerServiceRegRef.set(null);
            eventHandlerServiceReg.unregister();
        }

        componentContext = null;
    }

    @Reference(name = KEY_EVENT_ADMIN, service = EventAdmin.class)
    protected void setEventAdminRef(ServiceReference<EventAdmin> ref) {
        eventAdminRef.setReference(ref);
    }

    protected void unsetEventAdminRef(ServiceReference<EventAdmin> ref) {
        eventAdminRef.unsetReference(ref);
    }

    @Reference(service = RESTRoutingHelper.class,
               name = KEY_ROUTING_HELPER,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setRoutingHelper(ServiceReference<RESTRoutingHelper> routingHelper) {
        routingHelperRef.setReference(routingHelper);
    }

    protected void unsetRoutingHelper(ServiceReference<RESTRoutingHelper> routingHelper) {
        routingHelperRef.unsetReference(routingHelper);
    }

    private RESTRoutingHelper getRoutingHelper() throws IOException {
        RESTRoutingHelper routingHelper = routingHelperRef.getService();

        if (routingHelper == null) {
            IOException ioe = new IOException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                           APIConstants.TRACE_BUNDLE_FILE_TRANSFER,
                                                                           "OSGI_SERVICE_ERROR",
                                                                           new Object[] { "RESTRoutingHelper" },
                                                                           "CWWKX0122E: OSGi service is not available."));
            throw ErrorHelper.createRESTHandlerJsonException(ioe, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }

        return routingHelper;
    }

    public void addRoutedNotificationListener(NotificationTargetInformation nti, ClientNotificationListener listener, JSONConverter converter) {
        // Add the ClientNotificationListener to the map so that we can dispatch
        // notifications to it when we receive events from the Target-Client Manager.
        try {
            // Ensure that the CollectivePlugin is available before proceeding.
            // If plugin is not available then we should fail immediately.
            RESTRoutingHelper helper = getRoutingHelper();
            if (helper != null) {
                final boolean modified;
                synchronized (listenerMap) {
                    Set<ClientNotificationListener> listeners = listenerMap.get(nti);
                    if (listeners == null) {
                        listeners = new HashSet<ClientNotificationListener>();
                        listenerMap.put(nti, listeners);
                    }
                    modified = listeners.add(listener);
                }
                if (modified) {
                    // Send "add" NotificationListener event to EventAdmin
                    postRoutedNotificationListenerRegistrationEvent(OPERATION_PROP_VALUE_ADD, nti);
                }
            }
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, converter, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
    }

    public void removeRoutedNotificationListener(NotificationTargetInformation nti, ClientNotificationListener listener) {
        // Remove ClientNotificationListener from the map as it is being unregistered.
        try {
            // A listener could only have been added to this map if collective plugin was available.
            // Even if collective plugin has gone down we should clean up the map. It doesn't hurt
            // to send out the remove event either.
            final boolean modified;
            synchronized (listenerMap) {
                Set<ClientNotificationListener> listeners = listenerMap.get(nti);
                if (listeners != null) {
                    modified = listeners.remove(listener);
                    if (listeners.isEmpty()) {
                        listenerMap.remove(nti);
                    }
                } else {
                    modified = false;
                }
            }
            if (modified) {
                // Send "remove" NotificationListener event to EventAdmin
                postRoutedNotificationListenerRegistrationEvent(OPERATION_PROP_VALUE_REMOVE, nti);
            }
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Post an event to EventAdmin, instructing the Target-Client Manager to register or unregister a listener for a given target.
     */
    private void postRoutedNotificationListenerRegistrationEvent(String operation, NotificationTargetInformation nti) {
        Map<String, Object> props = createListenerRegistrationEvent(operation, nti);
        safePostEvent(new Event(REGISTER_JMX_NOTIFICATION_LISTENER_TOPIC, props));
    }

    public void addRoutedServerNotificationListener(NotificationTargetInformation nti, ObjectName listener, NotificationFilter filter,
                                                    Object handback, JSONConverter converter) {
        try {
            postRoutedServerNotificationListenerRegistrationEvent(OPERATION_PROP_VALUE_ADD_SERVER_NOTIFICATION, nti, listener, filter, handback);
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
    }

    public void removeRoutedServerNotificationListener(NotificationTargetInformation nti, ObjectName listener, NotificationFilter filter,
                                                       Object handback, JSONConverter converter) {
        try {
            postRoutedServerNotificationListenerRegistrationEvent(OPERATION_PROP_VALUE_REMOVE_SERVER_NOTIFICATION, nti, listener, filter, handback);
        } catch (Exception e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Post an event to EventAdmin, instructing the Target-Client Manager to register or unregister a listener MBean on a given target.
     */
    private void postRoutedServerNotificationListenerRegistrationEvent(String operation, NotificationTargetInformation nti, ObjectName listener,
                                                                       NotificationFilter filter, Object handback) {
        Map<String, Object> props = createServerListenerRegistrationEvent(operation, nti, listener, filter, handback);
        safePostEvent(new Event(REGISTER_JMX_NOTIFICATION_LISTENER_TOPIC, props));
    }

    private Map<String, Object> createListenerRegistrationEvent(String operation, NotificationTargetInformation nti) {

        Map<String, Object> props = new HashMap<String, Object>();

        // Set operation.
        props.put(OPERATION_PROP, operation);

        // Add notification info.
        props.put(OBJECT_NAME_PROP, nti.getNameAsString());

        Map<String, Object> routingInfo = nti.getRoutingInformation();
        assert routingInfo != null : "Routing information is required to be passed in by the caller.";

        // Add target info.
        props.put(ClientProvider.ROUTING_KEY_HOST_NAME, routingInfo.get(ClientProvider.ROUTING_KEY_HOST_NAME));
        props.put(ClientProvider.ROUTING_KEY_SERVER_NAME, routingInfo.get(ClientProvider.ROUTING_KEY_SERVER_NAME));
        props.put(ClientProvider.ROUTING_KEY_SERVER_USER_DIR, routingInfo.get(ClientProvider.ROUTING_KEY_SERVER_USER_DIR));

        return props;
    }

    private Map<String, Object> createServerListenerRegistrationEvent(String operation, NotificationTargetInformation nti, ObjectName listener,
                                                                      NotificationFilter filter, Object handback) {
        Map<String, Object> props = createListenerRegistrationEvent(operation, nti);

        // Add server-side specific listener properties.
        props.put(LISTENER_OBJECT_NAME_PROP, listener != null ? listener.getCanonicalName() : null);
        props.put(NOTIFICATION_FILTER_PROP, filter);
        props.put(HANDBACK_OBJECT_PROP, handback);

        return props;
    }

    /**
     * Null-safe event posting to eventAdminRef.
     * 
     * @param event
     */
    private void safePostEvent(Event event) {
        EventAdmin ea = eventAdminRef.getService();
        if (ea != null) {
            ea.postEvent(event);
        } else if (tc.isEventEnabled()) {
            Tr.event(tc, "The EventAdmin service is unavailable. Unable to post the Event: " + event);
        }
    }

    public static MBeanRoutedNotificationHelper getMBeanRoutedNotificationHelper() {
        BundleContext bc = componentContext.getBundleContext();
        ServiceReference<MBeanRoutedNotificationHelper> mbeanRoutedNotificationHelperRef = bc.getServiceReference(MBeanRoutedNotificationHelper.class);

        MBeanRoutedNotificationHelper mbeanRoutedNotificationHelper = mbeanRoutedNotificationHelperRef != null ? bc.getService(mbeanRoutedNotificationHelperRef) : null;

        if (mbeanRoutedNotificationHelper == null) {
            // REVISIT: This requires a proper NLS message.
            IOException ioe = new IOException("OSGi service is not available.");
            throw ErrorHelper.createRESTHandlerJsonException(ioe, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }

        return mbeanRoutedNotificationHelper;
    }

    /**
     * Handler for routed notification events. This EventHandler listens to the HANDLE_NOTIFICATION_TOPIC
     * for events produced by the Target-Client Manager. Each Notification is fired to the appropriate
     * ClientNotificationListener based on their ObjectName and routing information.
     */
    public class NotificationEventHandler implements EventHandler {

        @Override
        public void handleEvent(Event event) {
            if (event == null) {
                if (tc.isEventEnabled()) {
                    Tr.event(tc, "Unable to complete handleEvent, Event is null.");
                }
                return;
            }
            String topic = event.getTopic();
            if (HANDLE_NOTIFICATION_TOPIC.equals(topic)) {
                String objectName = (String) event.getProperty(OBJECT_NAME_PROP);
                if (objectName != null) {
                    Notification n = (Notification) event.getProperty(NOTIFICATION_PROP);
                    if (n != null) {
                        String hostName = (String) event.getProperty(ClientProvider.ROUTING_KEY_HOST_NAME);
                        String serverName = (String) event.getProperty(ClientProvider.ROUTING_KEY_SERVER_NAME);
                        String serverUserDir = (String) event.getProperty(ClientProvider.ROUTING_KEY_SERVER_USER_DIR);
                        if (hostName != null && serverName != null && serverUserDir != null) {
                            NotificationRecord record = new NotificationRecord(n, objectName, hostName, serverName, serverUserDir);
                            NotificationTargetInformation nti = record.getNotificationTargetInformation();
                            // Invoke each notification listener that's correlated with the given ObjectName and routing information.
                            synchronized (listenerMap) {
                                Set<ClientNotificationListener> listeners = listenerMap.get(nti);
                                if (listeners != null) {
                                    for (ClientNotificationListener listener : listeners) {
                                        NotificationFilter filter = listener.getClientWrapperFilter();
                                        // Only propagate the notification if it matches the filter.
                                        if (filter.isNotificationEnabled(n)) {
                                            listener.handleNotificationRecord(record);
                                        }
                                    }
                                } else if (tc.isEventEnabled()) {
                                    Tr.event(tc, "Ignoring received event. A notification Event was received for " + nti + " but no listeners were registered to receive it.");
                                }
                            }
                        } else if (tc.isEventEnabled()) {
                            Tr.event(tc, "Ignoring received event. A notification Event was received but was missing routing information.");
                        }
                    } else if (tc.isEventEnabled()) {
                        Tr.event(tc, "Ignoring received event. A notification Event was received but was missing the Notification.");
                    }
                } else if (tc.isEventEnabled()) {
                    Tr.event(tc, "Ignoring received event. A notification Event was received but was missing the ObjectName.");
                }
            } else if (tc.isEventEnabled()) {
                Tr.event(tc, "Ignoring received event. Unrecognized topic '" + topic + "'");
            }
        }
    }
}
