/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.resources.internal;

import java.util.Collection;

import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.resources.writeable.ConfigSnippetResourceWritable;
import com.ibm.ws.repository.transport.model.Asset;

public class ConfigSnippetResourceImpl extends RepositoryResourceImpl implements ConfigSnippetResourceWritable {

    /**
     * ----------------------------------------------------------------------------------------------------
     * INSTANCE METHODS
     * ----------------------------------------------------------------------------------------------------
     */

    public ConfigSnippetResourceImpl(RepositoryConnection repoConnection) {
        this(repoConnection, null);
    }

    public ConfigSnippetResourceImpl(RepositoryConnection repoConnection, Asset ass) {
        super(repoConnection, ass);
        if (ass == null) {
            setType(ResourceType.CONFIGSNIPPET);
        }
    }

    @Override
    public void setAppliesTo(String appliesTo) {
        _asset.getWlpInformation().setAppliesTo(appliesTo);
    }

    /** {@inheritDoc} */
    @Override
    public String getAppliesTo() {
        return _asset.getWlpInformation().getAppliesTo();
    }

    /** {@inheritDoc} */
    @Override
    public void setRequireFeature(Collection<String> requireFeature) {
        _asset.getWlpInformation().setRequireFeature(requireFeature);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> getRequireFeature() {
        return _asset.getWlpInformation().getRequireFeature();
    }

    @Override
    protected void copyFieldsFrom(RepositoryResourceImpl fromResource, boolean includeAttachmentInfo) {
        super.copyFieldsFrom(fromResource, includeAttachmentInfo);
        ConfigSnippetResourceImpl configSnipRes = (ConfigSnippetResourceImpl) fromResource;
        setAppliesTo(configSnipRes.getAppliesTo());
        setRequireFeature(configSnipRes.getRequireFeature());
    }

}
