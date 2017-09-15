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
package com.ibm.ws.artifact.bundle.internal;

import com.ibm.wsspi.artifact.ArtifactNotifier;

/**
 * This is the simplest implementation of the notfier interface.
 * By returning false, it claims it is unable to support notification for any request.
 * This may need updating to allow web container to listen to web-inf etc.
 */
public class BundleNotifier implements ArtifactNotifier {

    /** {@inheritDoc} */
    @Override
    public boolean registerForNotifications(ArtifactNotification targets, ArtifactListener callbackObject) throws IllegalArgumentException {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeListener(ArtifactListener listenerToRemove) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean setNotificationOptions(long interval, boolean useMBean) {
        return false;
    }

}
