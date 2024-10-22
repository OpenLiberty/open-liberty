/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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
package io.openliberty.data.internal.persistence.service;

import javax.naming.InitialContext;

import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;

import io.openliberty.data.internal.persistence.DataProvider;

/**
 * A resource factory that delegates to a resource reference JNDI name that was
 * specified as the dataStore for a Repository.
 */
class ResRefDelegator implements ResourceFactory {
    private final String identifier;
    private final String jndiName;
    private final DataProvider provider;

    /**
     * Construct a new instance.
     *
     * @param jndiName resource reference JNDI name to look up.
     * @param metadata metadata identifier for the application artifact
     *                     that defines the repository interface.
     * @param provider OSGi service that provides the CDI extension.
     */
    ResRefDelegator(String jndiName, String identifier, DataProvider provider) {
        this.jndiName = jndiName;
        this.identifier = identifier;
        this.provider = provider;
    }

    /**
     * Delegates to a JNDI lookup of a resource reference that was specified as the
     * dataStore for a Repository.
     *
     * @param info ignored because the resource reference that was specified by the
     *                 Repository already has its own ResourceInfo that should be
     *                 used instead.
     */
    @Override
    public Object createResource(ResourceInfo info) throws Exception {
        ComponentMetaData metadata = (ComponentMetaData) provider.metadataIdSvc.getMetaData(identifier);

        ComponentMetaDataAccessorImpl accessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
        if (metadata == null)
            accessor.beginDefaultContext();
        else
            accessor.beginContext(metadata);
        try {
            return InitialContext.doLookup(jndiName);
        } finally {
            accessor.endContext();
        }
    }

}