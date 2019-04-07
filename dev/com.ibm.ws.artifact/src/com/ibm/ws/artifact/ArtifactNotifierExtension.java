/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact;

import com.ibm.wsspi.artifact.ArtifactNotifier;
import com.ibm.wsspi.artifact.ArtifactNotifier.ArtifactNotification;

public interface ArtifactNotifierExtension extends ArtifactNotifier {

    /**
     * {@inheritDoc}
     */
    public interface ArtifactListener extends ArtifactNotifier.ArtifactListener {

        /**
         * Called to inform specific listeners that changes have happened to entries and they need processing.
         * <p>
         * Each notification contains the container it's associated paths are intended for, in case a listener is registered to multiple containers.<br>
         * Paths within the notifications will always be absolute, and will never contain the '!' prefix used when registering to request non-recursive registrations.<br>
         * The 3 parameters will never be null.
         *
         * @param added The added artifacts.
         * @param removed The removed artifacts.
         * @param modified The modified artifacts.
         * @param filter The filter string that allows only those artifact listeners with a matching id to be called to process the artifact event.
         */
        public void notifyEntryChange(ArtifactNotification added, ArtifactNotification removed, ArtifactNotification modified, String filter);

        /**
         * Returns the notification listener's ID.
         *
         * @return the notification listener's ID.
         */
        public String getId();
    }
}
