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
package com.ibm.ws.lars.testutils.clients;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.ibm.ws.repository.common.enums.AttachmentType;
import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.StateAction;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.client.RepositoryWriteableClient;
import com.ibm.ws.repository.transport.exceptions.BadVersionException;
import com.ibm.ws.repository.transport.exceptions.ClientFailureException;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;
import com.ibm.ws.repository.transport.model.AppliesToFilterInfo;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.Attachment;
import com.ibm.ws.repository.transport.model.AttachmentSummary;
import com.ibm.ws.repository.transport.model.FilterVersion;

/**
 *
 */
public abstract class AbstractFileWriteableClient implements RepositoryWriteableClient, RepositoryReadableClient {

    protected RepositoryReadableClient _readClient;

    /**
     * {@inheritDoc}
     *
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Override
    public Asset addAsset(Asset asset) throws IOException, BadVersionException, RequestFailureException, SecurityException, ClientFailureException {

        StringBuffer relative = new StringBuffer();
        if (asset.getType() != null) {
            relative.append(asset.getType().getURLForType());
            relative.append(File.separator);
        }
        FilterVersion minVer = null;
        if (asset.getWlpInformation() != null) {
            Collection<AppliesToFilterInfo> appliesTo = asset.getWlpInformation().getAppliesToFilterInfo();
            if (appliesTo != null && !appliesTo.isEmpty()) {
                minVer = appliesTo.iterator().next().getMinVersion();
            }
            if (minVer != null) {
                relative.append(minVer.getValue());
                relative.append(File.separator);
            }
        }

        Collection<Attachment> attachs = asset.getAttachments();
        String mainAttachmentName = null;
        if (attachs != null) {
            for (Attachment at : attachs) {
                if (at.getType() == AttachmentType.CONTENT) {
                    mainAttachmentName = at.getName();
                    break;
                }
            }
        }

        if (mainAttachmentName != null) {
            relative.append(mainAttachmentName);
        } else if (asset.getName() != null) {
            String assetName = asset.getName().replace("/", "");
            assetName = assetName.replace(":", "");
            relative.append(assetName);
        } else {
            // No where to get the name from so create a random one
            relative.append("unnamed" + Math.random());
        }
        asset.set_id(relative.toString());

        try {
            writeJson(asset, relative.toString());
        } catch (IllegalArgumentException e) {
            throw new ClientFailureException("Failed to write the asset to disk", asset.get_id(), e);
        } catch (IllegalAccessException e) {
            throw new ClientFailureException("Failed to write the asset to disk", asset.get_id(), e);
        }

        // For some reason the file.io operations aren't always finished....why dont they block?

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // ignore it
        }
        return _readClient.getAsset(asset.get_id());
    }

    /**
     * {@inheritDoc}
     *
     * @throws ClientFailureException
     */
    @Override
    public Asset updateAsset(final Asset asset) throws IOException, BadVersionException, RequestFailureException, SecurityException, ClientFailureException {
        deleteAssetAndAttachments(asset.get_id());
        return addAsset(asset);
    }

    /** {@inheritDoc} */
    @Override
    public Attachment updateAttachment(String assetId, AttachmentSummary summary) throws IOException, BadVersionException, RequestFailureException, SecurityException {
        deleteAttachment(assetId, summary.getAttachment().get_id());
        return addAttachment(assetId, summary);
    }

    /** {@inheritDoc} */
    @Override
    public void updateState(String assetId, StateAction action) throws IOException, RequestFailureException {
        // null op for file based repos
    }

    public abstract void writeJson(Asset asset, String path) throws IOException, IllegalArgumentException, IllegalAccessException;

    @Override
    public Asset getAsset(String assetId) throws IOException, BadVersionException, RequestFailureException {
        return _readClient.getAsset(assetId);
    }

    @Override
    public Collection<Asset> getAllAssets() throws IOException, RequestFailureException {
        return _readClient.getAllAssets();
    }

    @Override
    public Collection<Asset> getAssets(ResourceType type) throws IOException, RequestFailureException {
        return _readClient.getAssets(type);
    }

    @Override
    public Collection<Asset> getAssets(Collection<ResourceType> types, Collection<String> productIds, Visibility visibility, Collection<String> productVersions) throws IOException, RequestFailureException {
        return _readClient.getAssets(types, productIds, visibility, productVersions);
    }

    @Override
    public Collection<Asset> getAssetsWithUnboundedMaxVersion(Collection<ResourceType> types, Collection<String> rightProductIds, Visibility visibility) throws IOException, RequestFailureException {
        return _readClient.getAssetsWithUnboundedMaxVersion(types, rightProductIds, visibility);
    }

    @Override
    public InputStream getAttachment(Asset asset, Attachment attachment) throws IOException, BadVersionException, RequestFailureException {
        return _readClient.getAttachment(asset, attachment);
    }

    @Override
    public List<Asset> findAssets(String searchString, Collection<ResourceType> types) throws IOException, RequestFailureException {
        return _readClient.findAssets(searchString, types);
    }

    @Override
    public Collection<Asset> getFilteredAssets(Map<FilterableAttribute, Collection<String>> filters) throws IOException, RequestFailureException {
        return _readClient.getFilteredAssets(filters);
    }

    @Override
    public void checkRepositoryStatus() throws IOException, RequestFailureException {
        _readClient.checkRepositoryStatus();
    }

}
