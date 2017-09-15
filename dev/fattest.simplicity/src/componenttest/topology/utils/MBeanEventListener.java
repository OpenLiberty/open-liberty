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

import java.util.concurrent.CountDownLatch;

import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;

/**
 *
 */
public class MBeanEventListener implements NotificationListener {

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
     */
    private boolean registered = false;

    public final CountDownLatch latchForListener = new CountDownLatch(1);

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
     */
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
