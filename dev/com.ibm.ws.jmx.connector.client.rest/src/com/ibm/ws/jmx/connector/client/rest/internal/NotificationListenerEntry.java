/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.client.rest.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

import com.ibm.ws.jmx.connector.client.rest.internal.resources.RESTClientMessagesUtil;

class NotificationListenerEntry {
    public final NotificationListener listener;
    public final NotificationFilter filter;
    public final Object handback;

    private static final Logger logger = Logger.getLogger(NotificationListenerEntry.class.getName());

    /**
     * @param listener
     * @param filter
     * @param handback
     */
    NotificationListenerEntry(NotificationListener listener, NotificationFilter filter, Object handback) {
        this.listener = listener;
        this.filter = filter;
        this.handback = handback;

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "NotificationListenerEntry", "[" + RESTClientMessagesUtil.getObjID(this) + "] | listener: " + listener + " | filter: "
                                                                                    + filter + " | handback: " + handback);
        }
    }

    void handleNotification(Notification notification) {
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, logger.getName(), "handleNotification", "[" + RESTClientMessagesUtil.getObjID(this) + "] | Notification: " + notification);
        }

        if (filter == null || filter.isNotificationEnabled(notification)) {
            listener.handleNotification(notification, handback);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NotificationListenerEntry))
            return false;
        NotificationListenerEntry other = (NotificationListenerEntry) o;
        return listener.equals(other.listener) && filter == other.filter && handback == other.handback;
    }

    @Override
    public int hashCode() {
        return listener.hashCode()
               + (filter != null ? filter.hashCode() : 0)
               + (handback != null ? handback.hashCode() : 0);
    }

}
