/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.fat_bvt.bundle.custom;

import com.ibm.wsspi.artifact.ArtifactNotifier;

/**
 * Simple NO-OP notifier used by custom containers.
 */
public class CustomNotifier implements ArtifactNotifier {
    protected static final CustomNotifier INSTANCE = new CustomNotifier();

    @Override
    public boolean registerForNotifications(ArtifactNotification targets, ArtifactListener callbackObject)
        throws IllegalArgumentException {
        return false;
    }

    @Override
    public boolean removeListener(ArtifactListener listenerToRemove) {
        return false;
    }

    @Override
    public boolean setNotificationOptions(long interval, boolean useMBean) {
        return false;
    }
}
