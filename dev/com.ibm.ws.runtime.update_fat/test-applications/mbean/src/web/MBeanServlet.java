/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.relation.MBeanServerNotificationFilter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.runtime.update.RuntimeUpdateNotificationMBean;

@SuppressWarnings("serial")
public class MBeanServlet extends HttpServlet {

    private static ObjectName MBEAN_NAME;
    static {
        try {
            MBEAN_NAME = new ObjectName(RuntimeUpdateNotificationMBean.OBJECT_NAME);
        } catch (Exception e) {
            System.out.println("exception while making object name: " + e);
        }
    }

    public static final String MBeanMessage = "This is a servlet for the RuntimeUpdateNotificationMBean.";
    private static final AtomicReference<RuntimeUpdateNotificationListener> notificationListener = new AtomicReference<RuntimeUpdateNotificationListener>();
    private static final String PASS = "PASS";
    private static final String FAIL = "FAIL: ";

    /**
     * A simple servlet that when it received a request it simply outputs the message
     * as defined by the static field.
     */
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain");
        writer.println(MBeanMessage);

        String test = request.getQueryString();

        try {

            if (test.equals("setupNotificationListener")) {
                setupNotificationListener(writer);
            } else if (test.equals("checkForNotifications")) {
                checkForNotifications(writer);
            } else {
                writer.println(FAIL + "Unrecognized test name");
            }

        } catch (Exception e) {
            writer.println(FAIL + e.getMessage());
            e.printStackTrace(writer);
        }

        writer.flush();
        writer.close();
    }

    private void setupNotificationListener(PrintWriter writer) throws Exception {
        MBeanServer mbs = getMBS();
        MBeanRegistrationCheckAndWait.waitForRegistrationForMBean(mbs, MBEAN_NAME);
        RuntimeUpdateNotificationListener listener = new RuntimeUpdateNotificationListener();
        notificationListener.set(listener);
        mbs.addNotificationListener(MBEAN_NAME, listener, null, null);
        writer.println(PASS);
    }

    private void checkForNotifications(PrintWriter writer) throws Exception {
        final RuntimeUpdateNotificationListener listener = notificationListener.get();
        if (listener == null) {
            writer.println(FAIL + "The notification listener stored in the servlet is null.");
            return;
        }
        notificationListener.set(null);
        try {
            final boolean latchReleased = waitOnLatchRelease(listener);
            if (!latchReleased) {
                writer.println(FAIL + "ConfigUpdatesDelivered notification was not received.");
                return;
            }
            if (!listener.configUpdatesDelivered()) {
                writer.println(FAIL + "ConfigUpdatesDelivered notification was received but the server.xml updates did not complete successfully.");
                return;
            }
        } finally {
            MBeanServer mbs = getMBS();
            try {
                mbs.removeNotificationListener(MBEAN_NAME, listener);
            }
            // No listener was registered. Ignore it.
            catch (ListenerNotFoundException e) {
            }
        }
        writer.println(PASS);
    }

    private boolean waitOnLatchRelease(RuntimeUpdateNotificationListener listener) {
        boolean done = false;
        boolean countDownLatchReachZero = false;
        do {
            try {
                countDownLatchReachZero = listener.latchForListener.await(120, TimeUnit.SECONDS);
                done = true;
            } catch (InterruptedException e) {
            }
        } while (!done);
        return countDownLatchReachZero;
    }

    private MBeanServer getMBS() {
        return ManagementFactory.getPlatformMBeanServer();
    }

    public static class RuntimeUpdateNotificationListener implements NotificationListener {

        private boolean configUpdatesDelivered = false;
        public final CountDownLatch latchForListener = new CountDownLatch(1);

        @Override
        public synchronized void handleNotification(Notification notification, Object handback) {
            if (RuntimeUpdateNotificationMBean.RUNTIME_UPDATE_NOTIFICATION_TYPE.equals(notification.getType()) && latchForListener.getCount() > 0) {
                // UserData for "com.ibm.websphere.runtime.update.notification" type will always be a Map<String,Object>
                @SuppressWarnings("unchecked")
                Map<String, Object> userData = (Map<String, Object>) notification.getUserData();
                // Filter on runtime update notifications with the "ConfigUpdatesDelivered" name.
                if (userData != null &&
                    "ConfigUpdatesDelivered".equals(userData.get(RuntimeUpdateNotificationMBean.RUNTIME_UPDATE_NOTIFICATION_KEY_NAME))) {
                    configUpdatesDelivered = Boolean.TRUE.equals(userData.get(RuntimeUpdateNotificationMBean.RUNTIME_UPDATE_NOTIFICATION_KEY_STATUS));
                    configUpdatesDelivered = configUpdatesDelivered &&
                                             userData.get(RuntimeUpdateNotificationMBean.RUNTIME_UPDATE_NOTIFICATION_KEY_MESSAGE) == null;
                    latchForListener.countDown();
                }
            }
        }

        public boolean configUpdatesDelivered() {
            return configUpdatesDelivered;
        }
    }

    public static class MBeanServerNotificationListener implements NotificationListener {

        private boolean registered = false;
        public final CountDownLatch latchForListener = new CountDownLatch(1);

        @Override
        public void handleNotification(Notification notification, Object handback) {
            if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(notification.getType())) {
                registered = true;
                latchForListener.countDown();
            }
        }

        public boolean isRegistered() {
            return registered;
        }
    }

    public static class MBeanRegistrationCheckAndWait {

        public static void waitForRegistrationForMBean(MBeanServerConnection mbsc, ObjectName objName) throws InstanceNotFoundException, IOException {
            (new MBeanRegistrationCheckAndWait()).waitOnMBeanRegistration(mbsc, objName);
        }

        private MBeanServerNotificationListener listener;

        private MBeanRegistrationCheckAndWait() {}

        private boolean waitOnLatchRelease(MBeanServerNotificationListener listener) {
            boolean done = false;
            boolean countDownLatchReachZero = false;
            do {
                try {
                    countDownLatchReachZero = listener.latchForListener.await(120, TimeUnit.SECONDS);
                    done = true;
                } catch (InterruptedException e) {
                }
            } while (!done);
            return countDownLatchReachZero;
        }

        private synchronized boolean needToWaitForListener(MBeanServerConnection mbsc, ObjectName objName) throws IOException, InstanceNotFoundException {

            listener = new MBeanServerNotificationListener();
            // MBeanServerDelegate.DELEGATE_NAME;
            MBeanServerNotificationFilter registerFilter = new MBeanServerNotificationFilter();
            registerFilter.enableObjectName(objName);
            registerFilter.disableType(MBeanServerNotification.UNREGISTRATION_NOTIFICATION);

            mbsc.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, listener, registerFilter, null);
            // check again right after the listener is added
            if (mbsc.isRegistered(objName)) {
                return false;
            }
            return true;
        }

        private void waitOnMBeanRegistration(MBeanServerConnection mbsc, ObjectName objName) throws InstanceNotFoundException, IOException {
            if (mbsc.isRegistered(objName)) {
                return;
            }
            try {
                boolean waitForListener = needToWaitForListener(mbsc, objName);
                if (!waitForListener) {
                    return;
                }
                if (!waitOnLatchRelease(listener)) {
                    String exMessage = "The MBean with object name" + objName + ", cannot be registered.";
                    throw new IOException(exMessage);
                }
            } finally {
                if (listener != null) {
                    try {
                        mbsc.removeNotificationListener(MBeanServerDelegate.DELEGATE_NAME, listener);
                        listener = null;
                    }
                    // No listener was registered. Ignore it.
                    catch (ListenerNotFoundException e) {
                    }
                }
            }
        }
    }
}
