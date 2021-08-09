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
import java.util.Date;

import com.ibm.ws.repository.common.enums.DisplayPolicy;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.resources.writeable.IfixResourceWritable;
import com.ibm.ws.repository.transport.model.Asset;

public class IfixResourceImpl extends RepositoryResourceImpl implements IfixResourceWritable {

    /**
     * ----------------------------------------------------------------------------------------------------
     * INSTANCE METHODS
     * ----------------------------------------------------------------------------------------------------
     */

    public IfixResourceImpl(RepositoryConnection repoConnection) {
        this(repoConnection, null);
    }

    public IfixResourceImpl(RepositoryConnection repoConnection, Asset ass) {
        super(repoConnection, ass);

        if (ass == null) {
            setType(ResourceType.IFIX);
            setDisplayPolicy(DisplayPolicy.HIDDEN);
            setWebDisplayPolicy(DisplayPolicy.HIDDEN);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setProvideFix(Collection<String> provides) {
        _asset.getWlpInformation().setProvideFix(provides);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> getProvideFix() {
        return _asset.getWlpInformation().getProvideFix();
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

    /**
     * The {@link DisplayPolicy} to use
     */
    @Override
    public void setWebDisplayPolicy(DisplayPolicy policy) {
        _asset.getWlpInformation().setWebDisplayPolicy(policy);
    }

    /**
     * Get the {@link DisplayPolicy}
     *
     * @return {@link DisplayPolicy} in use
     */
    @Override
    public DisplayPolicy getWebDisplayPolicy() {
        if (_asset.getWlpInformation() == null) {
            return null;
        }
        return _asset.getWlpInformation().getWebDisplayPolicy();
    }

    /** {@inheritDoc} */
    @Override
    public Date getDate() {
        return _asset.getWlpInformation().getDate();
    }

    /** {@inheritDoc} */
    @Override
    public void setDate(Date date) {
        _asset.getWlpInformation().setDate(date);
    }

    @Override
    protected void copyFieldsFrom(RepositoryResourceImpl fromResource, boolean includeAttachmentInfo) {
        super.copyFieldsFrom(fromResource, includeAttachmentInfo);
        IfixResourceImpl iFixRes = (IfixResourceImpl) fromResource;
        setWebDisplayPolicy(iFixRes.getWebDisplayPolicy());
        setAppliesTo(iFixRes.getAppliesTo());
        setProvideFix(iFixRes.getProvideFix());
        setDate(iFixRes.getDate());
    }

}
