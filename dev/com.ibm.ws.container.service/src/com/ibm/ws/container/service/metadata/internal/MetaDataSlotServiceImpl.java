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
package com.ibm.ws.container.service.metadata.internal;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.container.service.metadata.MetaDataSlotService;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.MetaDataSlotImpl;

/**
 * A per-bundle service for reserving slots. A new instance of the service is
 * created for each bundle so we can track how slots are created.
 */
public class MetaDataSlotServiceImpl implements MetaDataSlotService {

    private MetaDataServiceImpl metaDataService;
    private Bundle bundle;
    private List<MetaDataSlotImpl> slots = new ArrayList<MetaDataSlotImpl>();

    public void setMetaDataService(MetaDataServiceImpl metaDataService) {
        this.metaDataService = metaDataService;
    }

    public void unsetMetaDataService(MetaDataServiceImpl metaDataService) {}

    // declarative service
    public void activate(ComponentContext cc) {
        bundle = cc.getUsingBundle();
    }

    // declarative service
    public synchronized void deactivate() {
        // Allow garbage collection of all slots reserved by this component.
        // Clean up all the metadata used by the slots.
        for (MetaDataSlotImpl slot : slots) {
            MetaDataManager<?, ?> manager = (MetaDataManager<?, ?>) slot.getManager();
            manager.destroyMetaDataSlot(slot);
        }
        slots = null;
    }

    @Override
    public MetaDataSlot reserveMetaDataSlot(Class<? extends MetaData> metaDataClass) {
        MetaDataManager<?, ?> manager = metaDataService.getMetaDataManager(metaDataClass);
        Bundle bundle = this.bundle;

        MetaDataSlotImpl slot;
        synchronized (this) {
            List<MetaDataSlotImpl> slots = this.slots;
            if (slots == null) {
                throw new IllegalStateException();
            }

            slot = manager.reserveMetaDataSlot(bundle);
            slots.add(slot);
        }

        return slot;
    }

}
