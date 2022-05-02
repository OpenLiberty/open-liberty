/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS.client.concurrent;

import org.jboss.resteasy.spi.concurrent.ThreadContext;

import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * Implements https://github.com/resteasy/resteasy/blob/main/resteasy-core-spi/src/main/java/org/jboss/resteasy/spi/concurrent/ThreadContext.java
 */
public class LibertyRestfulWSThreadContextImpl implements ThreadContext<ComponentMetaData> {

    /**
     * The component metadata accessor.
     */
    private static final ComponentMetaDataAccessorImpl compMetadataAccessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();

    @Override
    public ComponentMetaData capture() {
        return compMetadataAccessor.getComponentMetaData();
    }

    @Override
    public void push(ComponentMetaData context) {
        compMetadataAccessor.beginContext(context);
    }

    @Override
    public void reset(ComponentMetaData context) {
        compMetadataAccessor.endContext();
    }
}
