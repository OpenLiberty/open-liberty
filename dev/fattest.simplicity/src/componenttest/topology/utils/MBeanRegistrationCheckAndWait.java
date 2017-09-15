/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.utils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.ObjectName;
import javax.management.relation.MBeanServerNotificationFilter;

import com.ibm.websphere.simplicity.log.Log;
import componenttest.exception.TopologyException;

/**
 *
 */
public class MBeanRegistrationCheckAndWait {

    public static void waitForRegistrationForMBean(MBeanServerConnection mbsc, ObjectName objName) throws InstanceNotFoundException, IOException {
        (new MBeanRegistrationCheckAndWait()).waitOnMBeanRegistration(mbsc, objName);
    }

    private static final Class<?> c = MBeanRegistrationCheckAndWait.class;
    private MBeanEventListener listener;

    private MBeanRegistrationCheckAndWait() {}

    private boolean waitOnLatchRelease(MBeanEventListener listener) {
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

        listener = new MBeanEventListener();
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

        final String METHOD = "waitOnMBeanRegistration";

        if (mbsc.isRegistered(objName)) {
            Log.info(c, METHOD, "No need to add listener; as the MBean; " + objName + ", has already been registered.");
            return;
        }

        try {
            boolean waitForListener = needToWaitForListener(mbsc, objName);
            if (!waitForListener) {
                Log.info(c, METHOD, "No need to add listener, as the MBean, " + objName + ", has already been registered.");
                return;
            }
            if (!waitOnLatchRelease(listener)) {
                String exMessage = "The MBean with object name" + objName + ", cannot be registered.";
                TopologyException serverStartException = new TopologyException(exMessage);
                Log.error(c, METHOD, serverStartException, exMessage);
            }
        } finally {
            if (listener != null) {
                try {
                    mbsc.removeNotificationListener(MBeanServerDelegate.DELEGATE_NAME, listener);
                    listener = null;
                } catch (ListenerNotFoundException e) {
                    Log.info(c, METHOD, "Listener not found, no need to remove listener for object name, " + objName);
                }
            }
        }
    }
}
