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

import java.util.ArrayList;
import java.util.List;

import javax.management.Notification;
import javax.management.NotificationListener;

/**
 *
 */
public class NotificationRecorder implements NotificationListener {

    public final Object handback = new Object();
    public final List<Notification> notifications = new ArrayList<Notification>();

    @Override
    public void handleNotification(Notification notification, Object handback) {
        if (this.handback == handback) {
            notifications.add(notification);
        }
    }
}
