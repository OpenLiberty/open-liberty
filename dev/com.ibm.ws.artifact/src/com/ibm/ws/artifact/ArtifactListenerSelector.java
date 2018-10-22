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

import com.ibm.wsspi.artifact.ArtifactNotifier.ArtifactListener;
import com.ibm.wsspi.artifact.ArtifactNotifier.ArtifactNotification;

/**
 * Notifies the respective artifact listener that an artifact event has taken place.
 */
public class ArtifactListenerSelector implements com.ibm.ws.artifact.ArtifactNotifierExtension.ArtifactListener {

    /** The ArtifactListener instance being processed. */
    ArtifactListener listener;

    /**
     * Constructor.
     *
     * @param listener The ArtifactListener to process.
     */
    public ArtifactListenerSelector(ArtifactListener listener) {
        this.listener = listener;
    }

    /**
     * Returns the listener associated with this class.
     *
     * @return The Listener associated with this class.
     */
    public ArtifactListener getListener() {
        return listener;
    }

    /**
     * Returns True if the associated listener is capable of com.ibm.ws.artifact.ArtifactNotifierExtension.ArtifactListener which allows for listener filtering.
     *
     * @return True if the associated listener is capable of com.ibm.ws.artifact.ArtifactNotifierExtension.ArtifactListener which allows for listener filtering. False otherwise.
     */
    private boolean isListenerFilterable() {
        return (listener instanceof com.ibm.ws.artifact.ArtifactNotifierExtension.ArtifactListener);
    }

    /** {@inheritDoc} */
    @Override
    public void notifyEntryChange(ArtifactNotification added, ArtifactNotification removed, ArtifactNotification modified, String filter) {
        if (listener != null) {
            if (isListenerFilterable()) {
                ((com.ibm.ws.artifact.ArtifactNotifierExtension.ArtifactListener) listener).notifyEntryChange(added, removed, modified, filter);
            } else {
                listener.notifyEntryChange(added, removed, modified);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void notifyEntryChange(ArtifactNotification added, ArtifactNotification removed, ArtifactNotification modified) {
        listener.notifyEntryChange(added, removed, modified);
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        String id = null;
        if (isListenerFilterable()) {
            id = ((com.ibm.ws.artifact.ArtifactNotifierExtension.ArtifactListener) listener).getId();
        }

        return id;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return 31 * 11 + ((listener == null) ? 0 : listener.hashCode());
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        } else if (this == other) {
            return true;
        } else if (other instanceof ArtifactListenerSelector) {
            ArtifactListenerSelector otherAls = (ArtifactListenerSelector) other;
            ArtifactListener otherListener = otherAls.getListener();
            if (listener == null) {
                return false;
            } else if (listener == otherListener) {
                return true;
            }

            return listener.equals(otherListener);
        }

        return false;
    }
}
