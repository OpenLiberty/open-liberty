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

import com.ibm.ws.container.service.metadata.ApplicationMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;

class ApplicationMetaDataManager extends MetaDataManager<ApplicationMetaData, ApplicationMetaDataListener> {
    ApplicationMetaDataManager(String listenerRefName) {
        super(listenerRefName);
    }

    @Override
    public Class<ApplicationMetaData> getMetaDataClass() {
        return ApplicationMetaData.class;
    }

    @Override
    public void fireMetaDataCreated(ApplicationMetaDataListener listener, MetaDataEvent<ApplicationMetaData> event) throws MetaDataException {
        listener.applicationMetaDataCreated(event);
    }

    @Override
    public void fireMetaDataDestroyed(ApplicationMetaDataListener listener, MetaDataEvent<ApplicationMetaData> event) {
        listener.applicationMetaDataDestroyed(event);
    }
}
