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

import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.wsspi.adaptable.module.Container;

public class MetaDataEventImpl<M extends MetaData> implements MetaDataEvent<M> {

    private final M metaData;
    private final Container container;

    MetaDataEventImpl(M metaData, Container container) {
        this.metaData = metaData;
        this.container = container;
    }

    @Override
    public String toString() {
        return super.toString() + '[' + metaData + ", " + container + ']';
    }

    @Override
    public M getMetaData() {
        return metaData;
    }

    @Override
    public Container getContainer() {
        return container;
    }

}
