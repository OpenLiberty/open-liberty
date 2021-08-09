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
import com.ibm.ws.repository.resources.writeable.SampleResourceWritable;
import com.ibm.ws.repository.transport.model.Asset;

public class SampleResourceImpl extends RepositoryResourceImpl implements SampleResourceWritable {

    /**
     * ----------------------------------------------------------------------------------------------------
     * INSTANCE METHODS
     * ----------------------------------------------------------------------------------------------------
     */

    public SampleResourceImpl(RepositoryConnection repoConnection) {
        this(repoConnection, null);
    }

    public SampleResourceImpl(RepositoryConnection repoConnection, Asset ass) {
        super(repoConnection, ass);
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

    // Make setType public as this represents two different types in Massive
    @Override
    public void setType(ResourceType type) {
        super.setType(type);
    }

    /** {@inheritDoc} */
    @Override
    public void setShortName(String shortName) {
        _asset.getWlpInformation().setShortName(shortName);
    }

    /** {@inheritDoc} */
    @Override
    public String getShortName() {
        return _asset.getWlpInformation().getShortName();
    }

    /**
     * Gets a lower case version of the {@link #getIbmShortName()}.
     *
     * @return
     */
    public String getLowerCaseShortName() {
        return _asset.getWlpInformation().getLowerCaseShortName();
    }

    @Override
    protected void copyFieldsFrom(RepositoryResourceImpl fromResource, boolean includeAttachmentInfo) {
        super.copyFieldsFrom(fromResource, includeAttachmentInfo);
        SampleResourceImpl sampleRes = (SampleResourceImpl) fromResource;
        setAppliesTo(sampleRes.getAppliesTo());
        setRequireFeature(sampleRes.getRequireFeature());
        setShortName(sampleRes.getShortName());
    }

}
