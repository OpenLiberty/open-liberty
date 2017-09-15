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

import com.ibm.ws.container.service.metadata.ComponentMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.runtime.metadata.ComponentMetaData;

class ComponentMetaDataManager extends MetaDataManager<ComponentMetaData, ComponentMetaDataListener> {
    ComponentMetaDataManager(String listenerRefName) {
        super(listenerRefName);
    }

    @Override
    public Class<ComponentMetaData> getMetaDataClass() {
        return ComponentMetaData.class;
    }

    @Override
    public void fireMetaDataCreated(ComponentMetaDataListener listener, MetaDataEvent<ComponentMetaData> event) throws MetaDataException {
        listener.componentMetaDataCreated(event);
    }

    @Override
    public void fireMetaDataDestroyed(ComponentMetaDataListener listener, MetaDataEvent<ComponentMetaData> event) {
        listener.componentMetaDataDestroyed(event);
    }
}
