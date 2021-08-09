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
package com.ibm.ws.jmx_test.mbeans;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;

/**
 *
 */
public class StringHolder extends NotificationBroadcasterSupport implements StringHolderMBean {

    private String value;
    private long sequenceNumber = 1;

    @Override
    public synchronized String getValue() {
        return value;
    }

    @Override
    public synchronized void setValue(String value) {
        String oldValue = this.value;
        this.value = value;
        Notification n =
                        new AttributeChangeNotification(this,
                                            sequenceNumber++,
                                            System.currentTimeMillis(),
                                            "Value changed",
                                            "Value",
                                            "String",
                                            oldValue,
                                            this.value);

        sendNotification(n);
    }

    @Override
    public synchronized void print() {
        System.out.println(value);
    }

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        String[] types = new String[] {
                        AttributeChangeNotification.ATTRIBUTE_CHANGE
        };
        String name = AttributeChangeNotification.class.getName();
        String description = "An attribute of this MBean has changed";
        MBeanNotificationInfo info =
                        new MBeanNotificationInfo(types, name, description);
        return new MBeanNotificationInfo[] { info };
    }
}
