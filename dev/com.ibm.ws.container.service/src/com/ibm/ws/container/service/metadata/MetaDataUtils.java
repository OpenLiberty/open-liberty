/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.metadata;

import com.ibm.ws.container.service.app.deploy.ClientModuleInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

public class MetaDataUtils {
    /**
     * Copy slot data from a primary module metadata to a nested module
     * metadata. This is necessary for containers that want to share
     * module-level data for all components in a module, because nested modules
     * have their own distinct metadata.
     * 
     * @param event event from {@link ModuleMetaDataListener#moduleMetaDataCreated}
     * @param slot the slot to copy
     * @return true if the data was copied, or false if this is the primary metadata
     *         and the caller must set the slot data
     * @throws IllegalStateException if the primary metadata slot was not set
     */
    public static boolean copyModuleMetaDataSlot(MetaDataEvent<ModuleMetaData> event, MetaDataSlot slot) {
        Container container = event.getContainer();
        MetaData metaData = event.getMetaData();

        try {
            // For now, we just need to copy from WebModuleInfo, and ClientModuleInfo
            // Supports EJB in WAR and ManagedBean in Client
            ExtendedModuleInfo moduleInfo = (ExtendedModuleInfo) container.adapt(NonPersistentCache.class).getFromCache(WebModuleInfo.class);
            if (moduleInfo == null) {
                moduleInfo = (ExtendedModuleInfo) container.adapt(NonPersistentCache.class).getFromCache(ClientModuleInfo.class);
            }
            if (moduleInfo != null) {
                ModuleMetaData primaryMetaData = moduleInfo.getMetaData();

                if (metaData != primaryMetaData) {
                    Object slotData = primaryMetaData.getMetaData(slot);
                    if (slotData == null) {
                        // The caller is required to populate slot data.
                        throw new IllegalStateException();
                    }

                    metaData.setMetaData(slot, slotData);
                    return true;
                }
            }
        } catch (UnableToAdaptException e) {
            throw new UnsupportedOperationException(e);
        }

        return false;
    }
}
