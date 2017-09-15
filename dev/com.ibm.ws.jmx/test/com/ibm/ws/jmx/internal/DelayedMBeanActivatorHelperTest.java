/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.JMX;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerDelegateMBean;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;
import javax.management.loading.ClassLoaderRepository;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.jmx_test.mbeans.CounterMBean;
import com.ibm.ws.jmx_test.mbeans.StandardCounterMBean;
import com.ibm.ws.jmx_test.mbeans.StandardStringHolderMBean;
import com.ibm.ws.jmx_test.mbeans.StringHolderMBean;
import com.ibm.ws.kernel.boot.jmx.internal.PlatformMBeanServer;
import com.ibm.ws.kernel.boot.jmx.internal.PlatformMBeanServerBuilder;
import com.ibm.ws.kernel.boot.jmx.internal.PlatformMBeanServerBuilderListener;
import com.ibm.ws.kernel.boot.jmx.service.MBeanServerPipeline;

/**
 *
 */
public class DelayedMBeanActivatorHelperTest {

    public static final class PlatformMBeanServerFactory {

        private static final String MBEAN_SERVER_BUILDER_PROPERTY = "javax.management.builder.initial";
        private static final String MBEAN_SERVER_BUILDER_CLASS = PlatformMBeanServerBuilder.class.getName();

        public static PlatformMBeanServer getPlatformMBeanServer(final boolean useStandardDefaultDomain) {
            System.setProperty(MBEAN_SERVER_BUILDER_PROPERTY, MBEAN_SERVER_BUILDER_CLASS);
            if (useStandardDefaultDomain) {
                return (PlatformMBeanServer) MBeanServerFactory.newMBeanServer();
            } else {
                return (PlatformMBeanServer) MBeanServerFactory.newMBeanServer("WebSphere");
            }
        }
    }

    public static final class MBeanServerPipelineHolder implements PlatformMBeanServerBuilderListener {

        private MBeanServerPipeline pipeline;

        @Override
        public void platformMBeanServerCreated(MBeanServerPipeline pipeline) {
            this.pipeline = pipeline;
        }

        public MBeanServerPipeline getMBeanServerPipeline() {
            return pipeline;
        }
    }

    protected static ObjectName OBJECT_NAME_1;
    protected static ObjectName OBJECT_NAME_2;
    protected static ObjectName OBJECT_NAME_3;
    protected static ObjectName OBJECT_NAME_4;
    protected static ObjectName OBJECT_NAME_5;
    protected static ObjectName OBJECT_NAME_RUNTIME_MXBEAN;

    protected DelayedMBeanActivatorHelper mBeanServerHelper;
    protected PlatformMBeanServer mBeanServer;
    protected MBeanServerPipeline mBeanServerPipeline;
    protected MockComponentContext context;
    protected ServiceReference<DynamicMBean> sr1;
    protected ServiceReference<DynamicMBean> sr2;
    protected ServiceReference<DynamicMBean> sr3;
    protected ServiceReference<DynamicMBean> sr4;
    private MockBundleContext bundleCtx;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        OBJECT_NAME_1 = new ObjectName("com.ibm.ws:type=counter1");
        OBJECT_NAME_2 = new ObjectName("com.ibm.ws:type=counter2,version=1.0");
        OBJECT_NAME_3 = new ObjectName("WebSphere:type=counter3,version=12");
        OBJECT_NAME_4 = new ObjectName("com.ibm.websphere:type=stringHolder1");
        OBJECT_NAME_5 = new ObjectName("com.ibm.wsi:name=baz.bar.biz");
        OBJECT_NAME_RUNTIME_MXBEAN = new ObjectName(ManagementFactory.RUNTIME_MXBEAN_NAME);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    @Before
    public void setUp() throws Exception {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("jmx.objectname", "com.ibm.ws:type=counter1");
        sr1 = new MockServiceReference<DynamicMBean>(props);

        props = new HashMap<String, Object>();
        props.put("jmx.objectname", "com.ibm.ws:type=counter2,version=1.0");
        sr2 = new MockServiceReference<DynamicMBean>(props);

        props = new HashMap<String, Object>();
        props.put("jmx.objectname", "WebSphere:type=counter3,version=12");
        sr3 = new MockServiceReference<DynamicMBean>(props);

        props = new HashMap<String, Object>();
        props.put("jmx.objectname", "com.ibm.websphere:type=stringHolder1");
        sr4 = new MockServiceReference<DynamicMBean>(props);

        context = new MockComponentContext();
        bundleCtx = (MockBundleContext) context.getBundleContext();
        bundleCtx.addService(sr1, new StandardCounterMBean());
        bundleCtx.addService(sr2, new StandardCounterMBean());
        bundleCtx.addService(sr3, new StandardCounterMBean());
//        bundleCtx.addService(sr4, new StandardStringHolderMBean());

        mBeanServerHelper = new DelayedMBeanActivatorHelper();
        mBeanServer = PlatformMBeanServerFactory.getPlatformMBeanServer(false);
        MBeanServerPipelineHolder pipelineHolder = new MBeanServerPipelineHolder();
        mBeanServer.invokePlatformMBeanServerCreated(pipelineHolder);
        mBeanServerPipeline = pipelineHolder.getMBeanServerPipeline();
        mBeanServerHelper.setMBeanServerPipeline(mBeanServerPipeline);
        mBeanServerHelper.activate(context);
        assertEquals("Expecting 4 MBeans", 4, mBeanServer.getMBeanCount().intValue());
    }

    @After
    public void tearDown() throws Exception {
        mBeanServerHelper.unsetMBean(sr3);
        mBeanServerHelper.unsetMBean(sr2);
        mBeanServerHelper.unsetMBean(sr1);
        mBeanServerHelper.deactivate(context);
        mBeanServerHelper.unsetMBeanServerPipeline(mBeanServerPipeline);
    }

    @Test
    public void testWLPMBeanServer() {
        DelayedMBeanActivatorHelper helper = new DelayedMBeanActivatorHelper();
        MBeanServer server = helper.getMBeanServer();
        assertTrue("Expected " + OBJECT_NAME_RUNTIME_MXBEAN + " registered.", server.isRegistered(OBJECT_NAME_RUNTIME_MXBEAN));
    }

    @Test
    public void testVanillaJavaMBeanServer() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("jmx.objectname", "com.ibm.wsi:name=baz.bar.biz");
        MockServiceReference<DynamicMBean> sr = new MockServiceReference<DynamicMBean>(props);

        MockComponentContext compContext = new MockComponentContext();
        compContext.addService("dynamicMBean", sr, new StandardCounterMBean());

        DelayedMBeanActivatorHelper helper = new DelayedMBeanActivatorHelper();
        PlatformMBeanServer server = PlatformMBeanServerFactory.getPlatformMBeanServer(true);
        MBeanServerPipelineHolder pipelineHolder = new MBeanServerPipelineHolder();
        server.invokePlatformMBeanServerCreated(pipelineHolder);
        MBeanServerPipeline pipeline = pipelineHolder.getMBeanServerPipeline();
        helper.setMBeanServerPipeline(pipeline);
        helper.activate(compContext);
        helper.setMBean(sr);
        assertTrue("Expected " + OBJECT_NAME_5 + " registered.", server.isRegistered(OBJECT_NAME_5));
        helper.unsetMBean(sr);
        assertFalse("Expected " + OBJECT_NAME_5 + " NOT registered.", server.isRegistered(OBJECT_NAME_5));
        helper.deactivate(compContext);
        helper.unsetMBeanServerPipeline(pipeline);
    }

    @Test
    public void testIsRegistered() throws Exception {
        assertTrue("Expected " + OBJECT_NAME_1 + " registered.", mBeanServer.isRegistered(OBJECT_NAME_1));
        assertTrue("Expected " + OBJECT_NAME_2 + " registered.", mBeanServer.isRegistered(OBJECT_NAME_2));
        assertTrue("Expected " + OBJECT_NAME_3 + " registered.", mBeanServer.isRegistered(OBJECT_NAME_3));
        assertFalse("Expected " + OBJECT_NAME_4 + " unregistered.", mBeanServer.isRegistered(OBJECT_NAME_4));

        // Trigger delayed registration of OBJECT_NAME_1 and check again.
        ObjectInstance oi = mBeanServer.getObjectInstance(OBJECT_NAME_1);
        assertEquals("Class names must be equal.", "com.ibm.ws.jmx_test.mbeans.Counter", oi.getClassName());
        assertEquals("ObjectNames must be equal.", OBJECT_NAME_1, oi.getObjectName());
        assertTrue("Expected " + OBJECT_NAME_1 + " registered.", mBeanServer.isRegistered(OBJECT_NAME_1));

        // Trigger delayed registration of OBJECT_NAME_2 and check again.
        MBeanInfo mBeanInfo = mBeanServer.getMBeanInfo(OBJECT_NAME_2);
        assertEquals("Class names must be equal.", "com.ibm.ws.jmx_test.mbeans.Counter", mBeanInfo.getClassName());
        assertTrue("Expected " + OBJECT_NAME_2 + " registered.", mBeanServer.isRegistered(OBJECT_NAME_2));

        mBeanServerHelper.setMBean(sr4);
        assertEquals("Expecting 5 MBeans", 5, mBeanServer.getMBeanCount().intValue());
        assertTrue("Expected " + OBJECT_NAME_4 + " registered.", mBeanServer.isRegistered(OBJECT_NAME_4));
        mBeanServerHelper.unsetMBean(sr4);
        assertEquals("Expecting 4 MBeans", 4, mBeanServer.getMBeanCount().intValue());
        assertFalse("Expected " + OBJECT_NAME_4 + " unregistered.", mBeanServer.isRegistered(OBJECT_NAME_4));
    }

    @Test
    public void testGetDefaultDomain() {
        assertEquals("Expecting the default domain to be WebSphere", "WebSphere", mBeanServer.getDefaultDomain());
    }

    @Test
    public void testGetDomains() {
        String[] domains = mBeanServer.getDomains();
        Set<String> expected = new HashSet<String>(Arrays.asList(new String[] { "WebSphere", "com.ibm.ws", "JMImplementation" }));
        assertEquals("Expecting: " + expected, expected, new HashSet<String>(Arrays.asList(domains)));

        mBeanServerHelper.setMBean(sr4);
        assertEquals("Expecting 5 MBeans", 5, mBeanServer.getMBeanCount().intValue());

        domains = mBeanServer.getDomains();
        expected = new HashSet<String>(Arrays.asList(new String[] { "WebSphere", "com.ibm.ws", "JMImplementation", "com.ibm.websphere" }));
        assertEquals("Expecting: " + expected, expected, new HashSet<String>(Arrays.asList(domains)));

        mBeanServerHelper.unsetMBean(sr4);
        assertEquals("Expecting 4 MBeans", 4, mBeanServer.getMBeanCount().intValue());
    }

    @Test
    public void testCountersMBeans() {
        // Trigger delayed registration of OBJECT_NAME_1 through MBeanServer.getAttribute().
        CounterMBean c1 = JMX.newMBeanProxy(mBeanServer, OBJECT_NAME_1, CounterMBean.class);
        assertEquals("The value of the counter is expected to be 0", 0, c1.getValue());

        // Trigger delayed registration of OBJECT_NAME_2 through MBeanServer.invoke().
        CounterMBean c2 = JMX.newMBeanProxy(mBeanServer, OBJECT_NAME_2, CounterMBean.class);
        c2.increment();
        assertEquals("The value of the counter is expected to be 1", 1, c2.getValue());

        // Trigger delayed registration of OBJECT_NAME_3 through MBeanServer.invoke().
        CounterMBean c3 = JMX.newMBeanProxy(mBeanServer, OBJECT_NAME_3, CounterMBean.class);
        c3.increment();
        c3.reset();
        c3.increment();
        c3.increment();
        c3.increment();
        assertEquals("The value of the counter is expected to be 3", 3, c3.getValue());
    }

    @Test
    public void testStringHolderMBean() {
        bundleCtx.addService(sr4, new StandardStringHolderMBean());
        mBeanServerHelper.setMBean(sr4);
        assertEquals("Expecting 5 MBeans", 5, mBeanServer.getMBeanCount().intValue());

        // Trigger delayed registration of OBJECT_NAME_4 through MBeanServer.setAttribute().
        StringHolderMBean sh = JMX.newMBeanProxy(mBeanServer, OBJECT_NAME_4, StringHolderMBean.class);
        final String hello = "Hello World!";
        sh.setValue(hello);
        assertEquals("The string value is expected to be: " + hello, hello, sh.getValue());

        bundleCtx.removeService(sr4);
        assertEquals("Expecting 4 MBeans", 4, mBeanServer.getMBeanCount().intValue());
    }

    @Test
    public void testStringHolderMBean2() {
        bundleCtx.addService(sr4, new StandardStringHolderMBean());
        assertEquals("Expecting 5 MBeans", 5, mBeanServer.getMBeanCount().intValue());

        // Trigger delayed registration of OBJECT_NAME_4 through MBeanServer.addNotificationListener().
        StringHolderMBean sh = JMX.newMBeanProxy(mBeanServer, OBJECT_NAME_4, StringHolderMBean.class);
        NotificationRecorder notificationRecorder = new NotificationRecorder();
        sh.addNotificationListener(notificationRecorder, null, notificationRecorder.handback);

        String newValue = "foo";
        sh.setValue(newValue);
        assertEquals("The string value is expected to be: " + newValue, newValue, sh.getValue());

        newValue = "bar";
        sh.setValue(newValue);
        assertEquals("The string value is expected to be: " + newValue, newValue, sh.getValue());

        newValue = "baz";
        sh.setValue(newValue);
        assertEquals("The string value is expected to be: " + newValue, newValue, sh.getValue());

        final List<Notification> notifications = notificationRecorder.notifications;
        final int notificationsLength = notifications.size();
        assertEquals("Expecting 3 notifications", 3, notificationsLength);

        final String[] expected = new String[] { null, "foo", "bar", "baz" };
        for (int i = 0; i < notificationsLength; ++i) {
            AttributeChangeNotification acn = (AttributeChangeNotification) notifications.get(i);
            assertEquals("The old string value is expected to be: " + expected[i], expected[i], acn.getOldValue());
            assertEquals("The new string value is expected to be: " + expected[i + 1], expected[i + 1], acn.getNewValue());
        }

        bundleCtx.removeService(sr4);
        assertEquals("Expecting 4 MBeans", 4, mBeanServer.getMBeanCount().intValue());
    }

    @Test
    public void testStringHolderMBean3() throws Exception {
        bundleCtx.addService(sr4, new StandardStringHolderMBean());
        assertEquals("Expecting 5 MBeans", 5, mBeanServer.getMBeanCount().intValue());

        // Trigger delayed registration of OBJECT_NAME_4 through MBeanServer.setAttributes().
        final String hello = "Hello World!";
        Attribute attr = new Attribute("Value", hello);
        AttributeList attrList = new AttributeList(1);
        attrList.add(attr);
        mBeanServer.setAttributes(OBJECT_NAME_4, attrList);

        AttributeList attrList2 = mBeanServer.getAttributes(OBJECT_NAME_4, new String[] { "Value" });
        assertEquals("The attribute list should only contain one attribute", 1, attrList2.size());
        assertEquals("The string value is expected to be: " + hello, hello, attrList2.asList().get(0).getValue());

        bundleCtx.removeService(sr4);
        assertEquals("Expecting 4 MBeans", 4, mBeanServer.getMBeanCount().intValue());
    }

    @Test
    public void testCreateMBeanMethods() throws Exception {
        // Testing MBeanServer.createMBean(String className, ObjectName name)
        ObjectInstance oi = mBeanServer.createMBean("com.ibm.ws.jmx_test.mbeans.StandardStringHolderMBean", OBJECT_NAME_4);
        assertEquals("Expecting 5 MBeans", 5, mBeanServer.getMBeanCount().intValue());
        assertTrue("Expected " + OBJECT_NAME_4 + " registered.", mBeanServer.isRegistered(OBJECT_NAME_4));
        assertEquals("Expected " + OBJECT_NAME_4, OBJECT_NAME_4, oi.getObjectName());
        assertTrue("Expected class name is StandardStringHolderMBean.",
                   mBeanServer.isInstanceOf(OBJECT_NAME_4, "com.ibm.ws.jmx_test.mbeans.StandardStringHolderMBean"));

        mBeanServer.unregisterMBean(OBJECT_NAME_4);
        assertEquals("Expecting 4 MBeans", 4, mBeanServer.getMBeanCount().intValue());
        assertFalse("Expected " + OBJECT_NAME_4 + " unregistered.", mBeanServer.isRegistered(OBJECT_NAME_4));

        // Testing MBeanServer.createMBean(String className, ObjectName name, Object[] params, String[] signature)
        oi = mBeanServer.createMBean("com.ibm.ws.jmx_test.mbeans.StandardStringHolderMBean", OBJECT_NAME_4, null, null);
        assertEquals("Expecting 5 MBeans", 5, mBeanServer.getMBeanCount().intValue());
        assertTrue("Expected " + OBJECT_NAME_4 + " registered.", mBeanServer.isRegistered(OBJECT_NAME_4));
        assertEquals("Expected " + OBJECT_NAME_4, OBJECT_NAME_4, oi.getObjectName());
        assertTrue("Expected class name is StandardStringHolderMBean.",
                   mBeanServer.isInstanceOf(OBJECT_NAME_4, "com.ibm.ws.jmx_test.mbeans.StandardStringHolderMBean"));

        mBeanServer.unregisterMBean(OBJECT_NAME_4);
        assertEquals("Expecting 4 MBeans", 4, mBeanServer.getMBeanCount().intValue());
        assertFalse("Expected " + OBJECT_NAME_4 + " unregistered.", mBeanServer.isRegistered(OBJECT_NAME_4));
    }

    @Test
    public void testInstantiateMethods() throws Exception {
        // Testing MBeanServer.instantiate(String className)
        Object o = mBeanServer.instantiate("com.ibm.ws.jmx_test.mbeans.StandardStringHolderMBean");
        assertTrue("Expecting instance of StandardStringHolderMBean", o instanceof StandardStringHolderMBean);

        mBeanServer.registerMBean(o, OBJECT_NAME_4);
        assertEquals("Expecting 5 MBeans", 5, mBeanServer.getMBeanCount().intValue());
        assertSame("Expecting same ClassLoader", o.getClass().getClassLoader(),
                   mBeanServer.getClassLoaderFor(OBJECT_NAME_4));

        mBeanServer.unregisterMBean(OBJECT_NAME_4);
        assertEquals("Expecting 4 MBeans", 4, mBeanServer.getMBeanCount().intValue());

        // Testing MBeanServer.instantiate(String className, Object[] params, String[] signature)
        o = mBeanServer.instantiate("com.ibm.ws.jmx_test.mbeans.StandardStringHolderMBean", null, null);
        assertTrue("Expecting instance of StandardStringHolderMBean", o instanceof StandardStringHolderMBean);
    }

    @Test
    public void testGetClassLoaderRepository() throws Exception {
        ClassLoaderRepository clr = mBeanServer.getClassLoaderRepository();
        Class<?> c = clr.loadClass("com.ibm.ws.jmx_test.mbeans.StandardStringHolderMBean");
        assertSame("Expecting same Class object.", StandardStringHolderMBean.class, c);
    }

    @Test
    public void testQueryNames() throws Exception {

        ObjectName pattern = new ObjectName("com.ibm.ws:*");

        // Return a set containing the ObjectNames of all MBeans whose domain is com.ibm.ws:*.
        Set<ObjectName> queryResult = mBeanServer.queryNames(pattern, null);

        Set<ObjectName> expected = new HashSet<ObjectName>();
        expected.add(OBJECT_NAME_1);
        expected.add(OBJECT_NAME_2);
        assertEquals("Expecting: " + expected, expected, queryResult);

        // Return a set containing the ObjectNames of all registered MBeans.
        queryResult = mBeanServer.queryNames(null, null);

        expected.add(OBJECT_NAME_3);
        expected.add(MBeanServerDelegate.DELEGATE_NAME);
        assertEquals("Expecting: " + expected, expected, queryResult);

        // Return a set containing the ObjectNames of all MBeans with a "Value" attribute equal to 0.
        QueryExp query = Query.eq(Query.attr("Value"), Query.value(0));
        queryResult = mBeanServer.queryNames(null, query);

        expected.remove(MBeanServerDelegate.DELEGATE_NAME);
        assertEquals("Expecting: " + expected, expected, queryResult);
    }

    @Test
    public void testQueryMBeans() throws Exception {
        Set<ObjectInstance> queryResult = mBeanServer.queryMBeans(OBJECT_NAME_1, null);
        assertEquals("Excpecting only on item in the set", 1, queryResult.size());
        assertEquals("Expected " + OBJECT_NAME_1, OBJECT_NAME_1, queryResult.iterator().next().getObjectName());
    }

    @Test
    public void testMBeanServerDelegateMBeanNotifications() throws Exception {
        MBeanServerDelegateMBean sd = JMX.newMBeanProxy(mBeanServer, MBeanServerDelegate.DELEGATE_NAME, MBeanServerDelegateMBean.class, true);
        NotificationRecorder notificationRecorder = new NotificationRecorder();
        ((NotificationEmitter) sd).addNotificationListener(notificationRecorder, null, notificationRecorder.handback);

        // Trigger "unregistration" event for a delayed MBean.
        mBeanServerHelper.unsetMBean(sr3);
        assertEquals("Expecting 3 MBeans", 3, mBeanServer.getMBeanCount().intValue());

        final List<Notification> notifications = notificationRecorder.notifications;
        int notificationsLength = notifications.size();
        assertEquals("Expecting 1 notification", 1, notificationsLength);

        MBeanServerNotification serverNotification = (MBeanServerNotification) notifications.get(0);
        assertEquals("Expecting type: " + MBeanServerNotification.UNREGISTRATION_NOTIFICATION, MBeanServerNotification.UNREGISTRATION_NOTIFICATION, serverNotification.getType());
        assertEquals("Expecting source: " + MBeanServerDelegate.DELEGATE_NAME, MBeanServerDelegate.DELEGATE_NAME, serverNotification.getSource());
        assertEquals("Expecting ObjectName: " + OBJECT_NAME_3, OBJECT_NAME_3, serverNotification.getMBeanName());

        notifications.clear();

        // Trigger "registration" event for a delayed MBean.
        mBeanServerHelper.setMBean(sr3);
        assertEquals("Expecting 4 MBeans", 4, mBeanServer.getMBeanCount().intValue());

        notificationsLength = notifications.size();
        assertEquals("Expecting 1 notification", 1, notificationsLength);

        serverNotification = (MBeanServerNotification) notifications.get(0);
        assertEquals("Expecting type: " + MBeanServerNotification.REGISTRATION_NOTIFICATION, MBeanServerNotification.REGISTRATION_NOTIFICATION, serverNotification.getType());
        assertEquals("Expecting source: " + MBeanServerDelegate.DELEGATE_NAME, MBeanServerDelegate.DELEGATE_NAME, serverNotification.getSource());
        assertEquals("Expecting ObjectName: " + OBJECT_NAME_3, OBJECT_NAME_3, serverNotification.getMBeanName());

        notifications.clear();

        // Trigger delayed registration of OBJECT_NAME_3 through MBeanServer.getAttribute(). Expecting no "registration" event.
        CounterMBean c3 = JMX.newMBeanProxy(mBeanServer, OBJECT_NAME_3, CounterMBean.class);
        assertEquals("The value of the counter is expected to be 0", 0, c3.getValue());

        notificationsLength = notifications.size();
        assertEquals("Expecting 0 notifications", 0, notificationsLength);

        // Direct registration of OBJECT_NAME_4. Should trigger a "registration" event.
        mBeanServer.registerMBean(new StandardStringHolderMBean(), OBJECT_NAME_4);
        assertEquals("Expecting 5 MBeans", 5, mBeanServer.getMBeanCount().intValue());

        notificationsLength = notifications.size();
        assertEquals("Expecting 1 notification", 1, notificationsLength);

        serverNotification = (MBeanServerNotification) notifications.get(0);
        assertEquals("Expecting type: " + MBeanServerNotification.REGISTRATION_NOTIFICATION, MBeanServerNotification.REGISTRATION_NOTIFICATION, serverNotification.getType());
        assertEquals("Expecting source: " + MBeanServerDelegate.DELEGATE_NAME, MBeanServerDelegate.DELEGATE_NAME, serverNotification.getSource());
        assertEquals("Expecting ObjectName: " + OBJECT_NAME_4, OBJECT_NAME_4, serverNotification.getMBeanName());

        notifications.clear();

        // Direct unregistration of OBJECT_NAME_4. Should trigger an "unregistration" event.
        mBeanServer.unregisterMBean(OBJECT_NAME_4);
        assertEquals("Expecting 4 MBeans", 4, mBeanServer.getMBeanCount().intValue());

        notificationsLength = notifications.size();
        assertEquals("Expecting 1 notification", 1, notificationsLength);

        serverNotification = (MBeanServerNotification) notifications.get(0);
        assertEquals("Expecting type: " + MBeanServerNotification.UNREGISTRATION_NOTIFICATION, MBeanServerNotification.UNREGISTRATION_NOTIFICATION, serverNotification.getType());
        assertEquals("Expecting source: " + MBeanServerDelegate.DELEGATE_NAME, MBeanServerDelegate.DELEGATE_NAME, serverNotification.getSource());
        assertEquals("Expecting ObjectName: " + OBJECT_NAME_4, OBJECT_NAME_4, serverNotification.getMBeanName());

        notifications.clear();
        ((NotificationEmitter) sd).removeNotificationListener(notificationRecorder, null, notificationRecorder.handback);
    }
}
