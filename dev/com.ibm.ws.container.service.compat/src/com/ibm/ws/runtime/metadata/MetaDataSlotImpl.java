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
package com.ibm.ws.runtime.metadata;

/**
 * Internal. This class is not intended to be used by clients of the metadata infrastructure.
 */
public class MetaDataSlotImpl implements MetaDataSlot {
    /**
     * The id for this slot, which is used to index an array on {@link MetaDataImpl}.
     */
    final int id;

    /**
     * True if the slot has been destroyed because the bundle that reserved the slot has been uninstalled.
     */
    volatile boolean destroyed;

    /**
     * The slot interface.
     */
    final Class<? extends MetaData> metadataIntf;

    /**
     * The owning MetaDataManager.
     */
    private final Object manager;

    /**
     * The owning bundle, for diagnostic purposes.
     */
    private final Object bundle;

    public MetaDataSlotImpl(int id, Class<? extends MetaData> metadataIntf, Object manager, Object bundle) {
        this.id = id;
        this.metadataIntf = metadataIntf;
        this.manager = manager;
        this.bundle = bundle;
    }

    @Override
    public String toString() {
        return super.toString() + "[" + metadataIntf.getSimpleName() + ":" + id + (destroyed ? " (destroyed)" : "") + ":" + bundle + ']';
    }

    public int getID() {
        return id;
    }

    public Object getManager() {
        return manager;
    }

    public synchronized void destroy() {
        destroyed = true;
    }

    public void destroy(MetaDataImpl metaData) {
        metaData.setMetaData(this, null, true);
    }
}
