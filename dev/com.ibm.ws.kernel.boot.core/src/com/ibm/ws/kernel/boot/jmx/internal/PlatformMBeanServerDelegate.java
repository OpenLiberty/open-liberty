/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.jmx.internal;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.ObjectName;

import com.ibm.ws.kernel.boot.jmx.service.DelayedMBeanHelper;
import com.ibm.ws.kernel.boot.jmx.service.MBeanServerNotificationSupport;

final class PlatformMBeanServerDelegate extends MBeanServerDelegate implements MBeanServerNotificationSupport {

    protected static final String MBEANSERVER_ID = "WebSphere";
    private static final AtomicInteger count = new AtomicInteger(0);

    private final String id;
    private final CopyOnWriteArraySet<DelayedMBeanHelper> delayedMBeanHelpers = new CopyOnWriteArraySet<DelayedMBeanHelper>();

    public PlatformMBeanServerDelegate() {
        final int currentCount = count.getAndIncrement();
        if (currentCount > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(MBEANSERVER_ID);
            sb.append('_');
            sb.append(currentCount);
            id = sb.toString();
        } else {
            id = MBEANSERVER_ID;
        }
    }

    void addDelayedMBeanHelper(DelayedMBeanHelper helper) {
        delayedMBeanHelpers.add(helper);
    }

    void removeDelayedMBeanHelper(DelayedMBeanHelper helper) {
        delayedMBeanHelpers.remove(helper);
    }

    @Override
    public String getMBeanServerId() {
        return id;
    }

    @Override
    public void sendNotification(Notification notification) {
        if (notification instanceof MBeanServerNotification) {
            MBeanServerNotification mBeanServerNotification = (MBeanServerNotification) notification;
            // Ignore registration notification for a delayed MBean. We've already sent it.
            if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(notification.getType())) {
                Iterator<DelayedMBeanHelper> i = delayedMBeanHelpers.iterator();
                while (i.hasNext()) {
                    final DelayedMBeanHelper helper = i.next();
                    if (helper != null && helper.isDelayedMBean(mBeanServerNotification.getMBeanName())) {
                        return;
                    }
                }
            }
        }
        super.sendNotification(notification);
    }

    //
    // MBeanServerNotificationSupport methods
    //

    @Override
    public void sendRegisterNotification(ObjectName objectName) {
        sendNotificationInternal(MBeanServerNotification.REGISTRATION_NOTIFICATION, objectName);
    }

    @Override
    public void sendUnregisterNotification(ObjectName objectName) {
        sendNotificationInternal(MBeanServerNotification.UNREGISTRATION_NOTIFICATION, objectName);
    }

    private void sendNotificationInternal(String type, ObjectName objectName) {
        super.sendNotification(new MBeanServerNotification(type, MBeanServerDelegate.DELEGATE_NAME, 0L, objectName));
    }
}
