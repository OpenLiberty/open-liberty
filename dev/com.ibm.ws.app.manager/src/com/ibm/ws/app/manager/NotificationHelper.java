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
package com.ibm.ws.app.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;

import com.ibm.websphere.application.ApplicationMBean;

public class NotificationHelper {

    private final static AtomicLong sequence = new AtomicLong();

    public static void broadcastChange(NotificationBroadcasterSupport mbeanSupport, String appName, String operation, Boolean result, String msg) {
        if (mbeanSupport != null) {
            //Make and send notification
            Notification notification = new Notification(operation, appName, sequence.incrementAndGet(), msg);
            Map<String, Object> map = new HashMap<String, Object>();
            map.put(ApplicationMBean.STATE_CHANGE_NOTIFICATION_KEY_STATUS, result);
            notification.setUserData(map);
            mbeanSupport.sendNotification(notification);
        }
    }
}
