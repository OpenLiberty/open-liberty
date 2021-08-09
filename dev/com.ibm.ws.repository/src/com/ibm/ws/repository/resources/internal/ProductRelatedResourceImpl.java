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
import java.util.HashSet;
import java.util.List;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.GenericMetadata;
import org.osgi.resource.Requirement;

import com.ibm.ws.repository.common.enums.DisplayPolicy;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.resources.writeable.ProductRelatedResourceWritable;
import com.ibm.ws.repository.transport.model.Asset;

public abstract class ProductRelatedResourceImpl extends RepositoryResourceImpl implements ProductRelatedResourceWritable {

    public ProductRelatedResourceImpl(RepositoryConnection repoConnection) {
        this(repoConnection, null);
    }

    public ProductRelatedResourceImpl(RepositoryConnection repoConnection, Asset ass) {
        super(repoConnection, ass);
    }

    /** {@inheritDoc} */
    @Override
    public void setProductId(String productId) {
        _asset.getWlpInformation().setProductId(productId);
    }

    /** {@inheritDoc} */
    @Override
    public String getProductId() {
        return _asset.getWlpInformation().getProductId();
    }

    /** {@inheritDoc} */
    @Override
    public void setProductEdition(String edition) {
        _asset.getWlpInformation().setProductEdition(edition);
    }

    /** {@inheritDoc} */
    @Override
    public String getProductEdition() {
        return _asset.getWlpInformation().getProductEdition();
    }

    /** {@inheritDoc} */
    @Override
    public void setProductInstallType(String productInstallType) {
        _asset.getWlpInformation().setProductInstallType(productInstallType);
    }

    /** {@inheritDoc} */
    @Override
    public String getProductInstallType() {
        return _asset.getWlpInformation().getProductInstallType();
    }

    /** {@inheritDoc} */
    @Override
    public void setProductVersion(String version) {
        _asset.getWlpInformation().setProductVersion(version);
    }

    /** {@inheritDoc} */
    @Override
    public String getProductVersion() {
        return _asset.getWlpInformation().getProductVersion();
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
    public void setProvideFeature(Collection<String> provideFeature) {
        _asset.getWlpInformation().setProvideFeature(provideFeature);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> getProvideFeature() {
        return _asset.getWlpInformation().getProvideFeature();
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

    /** {@inheritDoc} */
    @Override
    public Collection<Requirement> getGenericRequirements() {
        //converts a string format of the generic requirements into a collection of Requirement objects
        String requirementStr = _asset.getWlpInformation().getGenericRequirements();
        Collection<Requirement> requirements = new HashSet<Requirement>();
        List<GenericMetadata> genMetList = ManifestHeaderProcessor.parseRequirementString(requirementStr);
        for (GenericMetadata genMet : genMetList) {
            Requirement req = new GenericRequirement(genMet);
            requirements.add(req);
        }
        return requirements;
    }

    /** {@inheritDoc} */
    @Override
    public void setGenericRequirements(String genericRequirements) {
        _asset.getWlpInformation().setGenericRequirements(genericRequirements);

        //If we are setting a osgi.native requirement we also need to increase the WLPInformationVersion so old versions of WDT don't pick up platform specific assets
        List<GenericMetadata> genMetList = ManifestHeaderProcessor.parseRequirementString(genericRequirements);
        for (GenericMetadata genMet : genMetList) {
            if (genMet.getNamespace().equals("osgi.native")) {
                _asset.getWlpInformation().setWlpInformationVersion(Float.toString(2.0f));
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getPackagedJava() {
        return _asset.getWlpInformation().getPackagedJava();
    }

    /** {@inheritDoc} */
    @Override
    public void setPackagedJava(String packagedJava) {
        _asset.getWlpInformation().setPackagedJava(packagedJava);
    }

    @Override
    protected void copyFieldsFrom(RepositoryResourceImpl fromResource, boolean includeAttachmentInfo) {
        super.copyFieldsFrom(fromResource, includeAttachmentInfo);
        ProductRelatedResourceImpl prodRes = (ProductRelatedResourceImpl) fromResource;
        setProductId(prodRes.getProductId());
        setProductEdition(prodRes.getProductEdition());
        setProductInstallType(prodRes.getProductInstallType());
        setProductVersion(prodRes.getProductVersion());
        setWebDisplayPolicy(prodRes.getWebDisplayPolicy());
        setProvideFeature(prodRes.getProvideFeature());
        setRequireFeature(prodRes.getRequireFeature());
        setGenericRequirements(prodRes._asset.getWlpInformation().getGenericRequirements());
        setPackagedJava(prodRes.getPackagedJava());
    }
}
