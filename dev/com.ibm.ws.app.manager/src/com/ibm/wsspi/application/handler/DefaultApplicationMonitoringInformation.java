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
package com.ibm.wsspi.application.handler;

import java.util.Collection;

import com.ibm.wsspi.adaptable.module.Notifier.Notification;

/**
 * Default implementation of {@link ApplicationMonitoringInformation}
 */
public class DefaultApplicationMonitoringInformation implements ApplicationMonitoringInformation {

    private final Collection<Notification> notificationsToMonitor;
    private final boolean listeningForRootStructuralChanges;

    /**
     * @param notificationsToMonitor
     * @param listenForRootStructuralChanges
     */
    public DefaultApplicationMonitoringInformation(Collection<Notification> notificationsToMonitor, boolean listeningForRootStructuralChanges) {
        this.notificationsToMonitor = notificationsToMonitor;
        this.listeningForRootStructuralChanges = listeningForRootStructuralChanges;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<Notification> getNotificationsToMonitor() {
        return notificationsToMonitor;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isListeningForRootStructuralChanges() {
        return listeningForRootStructuralChanges;
    }

}
