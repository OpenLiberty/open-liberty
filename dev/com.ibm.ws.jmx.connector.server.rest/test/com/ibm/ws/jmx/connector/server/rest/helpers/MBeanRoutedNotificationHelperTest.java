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

import static org.junit.Assert.assertSame;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.management.MBeanServerDelegate;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import test.common.SharedOutputManager;

import com.ibm.ws.jmx.connector.converter.JSONConverter;
import com.ibm.ws.jmx.connector.converter.NotificationTargetInformation;
import com.ibm.ws.jmx.connector.server.rest.notification.ClientNotificationArea;
import com.ibm.ws.jmx.connector.server.rest.notification.ClientNotificationListener;
import com.ibm.wsspi.rest.handler.helper.RESTRoutingHelper;

/**
 *
 */
public class MBeanRoutedNotificationHelperTest {

    private static final String KEY_EVENT_ADMIN = "eventAdmin";
    private static final String KEY_ROUTING_HELPER = "routingHelper";
    private static final String HANDLE_NOTIFICATION_TOPIC = "com/ibm/ws/management/repository/InboxManager/handleNotification";

    private static final String HOST_NAME = "skywalker.torolab.ibm.com";
    private static final String SERVER_NAME = "myServer";
    private static final String SERVER_USER_DIR = "/dev/wlp/usr";

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final BundleContext bc = mock.mock(BundleContext.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<EventAdmin> eventAdminRef = mock.mock(ServiceReference.class, "eventAdminRef");
    private final EventAdmin eventAdmin = mock.mock(EventAdmin.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<RESTRoutingHelper> routingHelperRef = mock.mock(ServiceReference.class, "routingHelperRef");
    private final RESTRoutingHelper routingHelper = mock.mock(RESTRoutingHelper.class);
    @SuppressWarnings("unchecked")
    private final ServiceRegistration<EventHandler> eventHandlerReg = mock.mock(ServiceRegistration.class, "eventHandlerReg");
    @SuppressWarnings("unchecked")
    private final ServiceReference<MBeanRoutedNotificationHelper> routedNotificationHelperRef = mock.mock(ServiceReference.class, "routedNotificationHelperRef");

    private MBeanRoutedNotificationHelper notificationHelper;

    @Before
    public void setUp() throws Exception {

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("service.vendor", "IBM");
        props.put(EventConstants.EVENT_TOPIC, HANDLE_NOTIFICATION_TOPIC);

        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(KEY_EVENT_ADMIN, eventAdminRef);
                will(returnValue(eventAdmin));

                allowing(cc).locateService(KEY_ROUTING_HELPER, routingHelperRef);
                will(returnValue(routingHelper));

                one(cc).getBundleContext();
                will(returnValue(bc));

                one(bc).registerService(with(EventHandler.class), with(any(EventHandler.class)), with(props));
                will(returnValue(eventHandlerReg));
            }
        });

        notificationHelper = new MBeanRoutedNotificationHelper();
        notificationHelper.setEventAdminRef(eventAdminRef);
        notificationHelper.setRoutingHelper(routingHelperRef);
        notificationHelper.activate(cc);
    }

    @After
    public void tearDown() throws Exception {

        mock.checking(new Expectations() {
            {
                one(eventHandlerReg).unregister();
            }
        });

        notificationHelper.deactivate(cc);
        notificationHelper.unsetRoutingHelper(routingHelperRef);
        notificationHelper.unsetEventAdminRef(eventAdminRef);
        notificationHelper = null;

        mock.assertIsSatisfied();
    }

    @Test
    public void testGetMBeanRoutedNotificationHelper() {

        final MBeanRoutedNotificationHelper helperService = notificationHelper;

        mock.checking(new Expectations() {
            {
                one(cc).getBundleContext();
                will(returnValue(bc));

                one(bc).getServiceReference(with(MBeanRoutedNotificationHelper.class));
                will(returnValue(routedNotificationHelperRef));

                one(bc).getService(with(routedNotificationHelperRef));
                will(returnValue(helperService));
            }
        });

        MBeanRoutedNotificationHelper _helperService = MBeanRoutedNotificationHelper.getMBeanRoutedNotificationHelper();
        assertSame("Expected the same MBeanRoutedNotificationHelper instance.", helperService, _helperService);
    }

    @Test
    public void testRoutedNotificationListenerRegistration() {

        mock.checking(new Expectations() {
            {
                one(eventAdmin).postEvent(with(any(Event.class)));
                one(eventAdmin).postEvent(with(any(Event.class)));
            }
        });

        // Add and remove a notification listener.
        NotificationTargetInformation nti = getNotificationTargetInformation();
        ClientNotificationListener cnl = getClientNotificationListener();
        JSONConverter convert = JSONConverter.getConverter();
        notificationHelper.addRoutedNotificationListener(nti, cnl, convert);
        notificationHelper.removeRoutedNotificationListener(nti, cnl);
    }

    @Test
    public void testRoutedServerNotificationListenerRegistration() {

        mock.checking(new Expectations() {
            {
                one(eventAdmin).postEvent(with(any(Event.class)));
                one(eventAdmin).postEvent(with(any(Event.class)));
            }
        });

        // Add and remove a notification listener.
        NotificationTargetInformation nti = getNotificationTargetInformation();
        JSONConverter convert = JSONConverter.getConverter();
        notificationHelper.addRoutedServerNotificationListener(nti, MBeanServerDelegate.DELEGATE_NAME, null, null, convert);
        notificationHelper.removeRoutedServerNotificationListener(nti, MBeanServerDelegate.DELEGATE_NAME, null, null, convert);
    }

    private NotificationTargetInformation getNotificationTargetInformation() {
        return new NotificationTargetInformation(MBeanServerDelegate.DELEGATE_NAME, HOST_NAME, SERVER_NAME, SERVER_USER_DIR);
    }

    private ClientNotificationListener getClientNotificationListener() {
        ClientNotificationArea cna = new ClientNotificationArea(0L, 0L, 0);
        return new ClientNotificationListener(cna);
    }
}