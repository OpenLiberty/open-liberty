/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.metadata.internal;

import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

public class ModuleMetaDataManager extends MetaDataManager<ModuleMetaData, ModuleMetaDataListener> {
    ModuleMetaDataManager(String listenerRefName) {
        super(listenerRefName);
    }

    @Override
    public Class<ModuleMetaData> getMetaDataClass() {
        return ModuleMetaData.class;
    }

    @Override
    public void fireMetaDataCreated(ModuleMetaDataListener listener, MetaDataEvent<ModuleMetaData> event) throws MetaDataException {
        listener.moduleMetaDataCreated(event);
    }

    @Override
    public void fireMetaDataDestroyed(ModuleMetaDataListener listener, MetaDataEvent<ModuleMetaData> event) {
        listener.moduleMetaDataDestroyed(event);
    }
}
