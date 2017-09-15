/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.converter;

import javax.management.Notification;
import javax.management.ObjectName;

/**
 * Data structure containing a JMX Notification and information about which target it came from.
 */
public final class NotificationRecord {

    private final Notification n;
    private final NotificationTargetInformation nti;

    public NotificationRecord(Notification n, ObjectName name) {
        this.n = n;
        this.nti = new NotificationTargetInformation(name);
    }

    public NotificationRecord(Notification n, String name) {
        this.n = n;
        this.nti = new NotificationTargetInformation(name);
    }

    public NotificationRecord(Notification n, ObjectName name, String hostName, String serverName, String serverUserDir) {
        this.n = n;
        this.nti = new NotificationTargetInformation(name, hostName, serverName, serverUserDir);
    }

    public NotificationRecord(Notification n, String name, String hostName, String serverName, String serverUserDir) {
        this.n = n;
        this.nti = new NotificationTargetInformation(name, hostName, serverName, serverUserDir);
    }

    public Notification getNotification() {
        return n;
    }

    public NotificationTargetInformation getNotificationTargetInformation() {
        return nti;
    }
}
