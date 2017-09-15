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
package com.ibm.ws.jmx.connector.server.rest.notification;

import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

import com.ibm.ws.jmx.connector.converter.NotificationRecord;

/**
 * This class acts as a listener for a single MBean (ObjectName with
 * optional routing information) and for <b>multiple</b> clientIDs.
 */
public class ClientNotificationListener implements NotificationListener {

    /**
     * This map is used to get/update/delete the filter used by a particular clientID.
     */
    private final ClientNotificationFilter filter;

    /**
     * ClientArea where notifications will be pushed
     */
    private final ClientNotificationArea clientArea;

    public ClientNotificationListener(ClientNotificationArea clientArea) {
        this.filter = new ClientNotificationFilter();
        this.clientArea = clientArea;
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
        //For now our handbacks for client notifications is always null
        clientArea.addNotfication(notification);
    }

    public void handleNotificationRecord(NotificationRecord record) {
        clientArea.addNotficationRecord(record);
    }

    /**
     * This method creates/updates the filters for a given clientID
     */
    public void addClientNotification(NotificationFilter[] filters) {
        //Update the filters
        filter.updateFilters(filters);
    }

    public ClientNotificationFilter getClientWrapperFilter() {
        return filter;
    }
}
