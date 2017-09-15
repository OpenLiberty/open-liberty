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
package com.ibm.ws.container.service.metadata.internal;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.MetaDataImpl;
import com.ibm.ws.runtime.metadata.MetaDataSecrets;
import com.ibm.ws.runtime.metadata.MetaDataSlotImpl;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;

/**
 * The manager for a type of metadata.
 * 
 * @param <M> the metadata type
 * @param <L> the metadata listener type
 */
abstract class MetaDataManager<M extends MetaData, L> {
    /**
     * The listeners for this metadata type.
     */
    protected final ConcurrentServiceReferenceSet<L> listeners;

    /**
     * The ID allocator for metadatas. The monitor for this object must be held while calling methods on it.
     */
    private final IndexList metaDataIDs = new IndexList();

    /**
     * The list of all created metadatas. The monitor for {@link #metaDataIDs} must be held while accessing this field.
     */
    private final List<MetaDataImpl> metaDatas = new ArrayList<MetaDataImpl>();

    /**
     * The ID allocator for metadata slots. The monitor for this object must be held while calling methods on it.
     */
    private final IndexList slotIDs = new IndexList();

    MetaDataManager(String listenerRefName) {
        listeners = new ConcurrentServiceReferenceSet<L>(listenerRefName);
    }

    void activate(ComponentContext cc) {
        listeners.activate(cc);
    }

    void deactivate(ComponentContext cc) {
        listeners.deactivate(cc);
    }

    final void addListener(ServiceReference<L> ref) {
        listeners.addReference(ref);
    }

    final void removeListener(ServiceReference<L> ref) {
        listeners.removeReference(ref);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected static <M extends MetaData> MetaDataEvent<M> createMetaDataEvent(M metaData, Container container) {
        return new MetaDataEventImpl(metaData, container);
    }

    private MetaDataImpl getMetaDataImpl(M metaData) {
        if (metaData == null) {
            throw new IllegalArgumentException("metaData");
        }

        // All MetaData objects with events must extend MetaDataImpl.
        return (MetaDataImpl) metaData;
    }

    final void fireMetaDataCreated(M metaData, Container container) throws MetaDataException {
        MetaDataImpl metaDataImpl = getMetaDataImpl(metaData);

        MetaDataEvent<M> event = null;
        for (L listener : listeners.services()) {
            if (event == null) {
                event = createMetaDataEvent(metaData, container);
            }

            try {
                fireMetaDataCreated(listener, event);
            } catch (Throwable t) {
                // It's not worth the effort to keep track of the list of
                // listeners that were successfully called, so simply call
                // destroyed for all listeners.
                fireMetaDataDestroyedImpl(metaData);

                if (t instanceof MetaDataException) {
                    throw (MetaDataException) t;
                }
                throw new MetaDataException(t);
            }
        }

        // Remember the MetaDataImpl so we can clear data from destroyed slots.
        synchronized (metaDataIDs) {
            int id = metaDataIDs.reserve();
            MetaDataSecrets.setID(metaDataImpl, id);
            if (id == metaDatas.size()) {
                metaDatas.add(metaDataImpl);
            } else {
                metaDatas.set(id, metaDataImpl);
            }
        }
    }

    void fireMetaDataDestroyed(M metaData) {
        MetaDataImpl metaDataImpl = getMetaDataImpl(metaData);

        fireMetaDataDestroyedImpl(metaData);

        // Remove the MetaDataImpl from the list if it was previously added.
        synchronized (metaDataIDs) {
            int id = MetaDataSecrets.getID(metaDataImpl);
            if (id >= 0) {
                metaDatas.set(id, null);
                metaDataIDs.unreserve(id);
            }
        }
    }

    private void fireMetaDataDestroyedImpl(M metaData) {
        MetaDataEvent<M> event = null;
        for (L listener : listeners.services()) {
            if (event == null) {
                event = createMetaDataEvent(metaData, null);
            }

            try {
                fireMetaDataDestroyed(listener, event);
            } catch (Throwable t) {
                // Nothing (except automatically inserted FFDC).
            }
        }
    }

    abstract void fireMetaDataCreated(L listener, MetaDataEvent<M> event) throws MetaDataException;

    abstract void fireMetaDataDestroyed(L listener, MetaDataEvent<M> event);

    abstract Class<M> getMetaDataClass();

    final MetaDataSlotImpl reserveMetaDataSlot(Object bundle) {
        int id;
        synchronized (slotIDs) {
            id = slotIDs.reserve();
        }

        return new MetaDataSlotImpl(id, getMetaDataClass(), this, bundle);
    }

    final void destroyMetaDataSlot(MetaDataSlotImpl slot) {
        // Destroy the slot to prevent it from writing new metadata.
        slot.destroy();

        // Remove the slot data from all metadata.
        synchronized (metaDataIDs) {
            for (MetaDataImpl metaData : metaDatas) {
                if (metaData != null) {
                    slot.destroy(metaData);
                }
            }
        }

        // Allow the slot to be reused.
        int id = slot.getID();
        synchronized (slotIDs) {
            slotIDs.unreserve(id);
        }
    }
}
