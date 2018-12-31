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

import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.MethodMetaDataListener;
import com.ibm.ws.runtime.metadata.MethodMetaData;

public class MethodMetaDataManager extends MetaDataManager<MethodMetaData, MethodMetaDataListener> {
    MethodMetaDataManager(String listenerRefName) {
        super(listenerRefName);
    }

    @Override
    public Class<MethodMetaData> getMetaDataClass() {
        return MethodMetaData.class;
    }

    @Override
    public void fireMetaDataCreated(MethodMetaDataListener listener, MetaDataEvent<MethodMetaData> event) throws MetaDataException {
        listener.methodMetaDataCreated(event);
    }

    @Override
    public void fireMetaDataDestroyed(MethodMetaDataListener listener, MetaDataEvent<MethodMetaData> event) {
        listener.methodMetaDataDestroyed(event);
    }
}
