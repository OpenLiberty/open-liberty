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

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.wsspi.adaptable.module.Container;

class MetaDataEventChecker<M extends MetaData> extends TypeSafeMatcher<MetaDataEvent<M>> {
    M metaData;
    Container container;

    MetaDataEventChecker(M metaData, Container container) {
        init(metaData, container);
    }

    public void init(M metaData, Container container) {
        this.metaData = metaData;
        this.container = container;
    }

    @Override
    public boolean matchesSafely(MetaDataEvent<M> event) {
        return event.getMetaData() == metaData &&
               event.getContainer() == container;
    }

    @Override
    public void describeTo(Description desc) {
        desc.appendText("a MetaDataEvent with specified metadata and adaptable");
    }
}
