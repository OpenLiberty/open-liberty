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

/**
 * A resource factory that delegates to a JNDI name that was specified
 * as the dataStore for a Repository. The ResourceInfo is ignored because
 * the JNDI name is likely to already be a resource reference lookup
 * with its own ResourceInfo that should be used instead.
 */
class DelegatingResourceFactory implements ResourceFactory {
    private final String jndiName;
    private final ComponentMetaData metadata;

    /**
     * Construct a new instance.
     *
     * @param jndiName JNDI name to look up obtaining resources.
     * @param metadata metadata that was on the thread for the CDI extension when processing repositories.
     */
    DelegatingResourceFactory(String jndiName, ComponentMetaData metadata) {
        this.jndiName = jndiName;
        this.metadata = metadata;
    }

    @Override
    public Object createResource(ResourceInfo info) throws Exception {
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