/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

import io.openliberty.data.internal.persistence.cdi.DataExtensionProvider;

/**
 * A resource factory that delegates to a JNDI name that was specified
 * as the dataStore for a Repository. The ResourceInfo is ignored because
 * the JNDI name is likely to already be a resource reference lookup
 * with its own ResourceInfo that should be used instead.
 */
class DelegatingResourceFactory implements ResourceFactory {
    private final String identifier;
    private final String jndiName;
    private final DataExtensionProvider provider;

    /**
     * Construct a new instance.
     *
     * @param jndiName JNDI name to look up obtaining resources.
     * @param metadata metadata identifier, computed from the class loader identifier
     *                     of the class loader of the repository interface.
     * @param provider OSGi service that provides the CDI extension.
     */
    DelegatingResourceFactory(String jndiName, String identifier, DataExtensionProvider provider) {
        this.jndiName = jndiName;
        this.identifier = identifier;
        this.provider = provider;
    }

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