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
package com.ibm.ws.repository.transport.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.ibm.ws.repository.transport.exceptions.BadVersionException;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;
import com.ibm.ws.repository.transport.model.AppliesToFilterInfo;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.Attachment;
import com.ibm.ws.repository.transport.model.WlpInformation;

/**
 * This client is a file based repository which just stores json files to represent the assets.
 * No attachments are stored in this repository and attempting to invoke methods that deal
 * with attachments will result in an UnsupportedOperationException
 */
public class LooseFileClient extends AbstractRepositoryClient {

    private final Collection<File> assets;

    /**
     * @param assets The collection of files this repository represents. Note that
     *            the collection represents the asset names (i.e. the file names without the
     *            .json extension at the end. The .json extension is automatically appended
     *            when assets are read from the repository).
     */
    public LooseFileClient(Collection<File> assets) {
        this.assets = assets;
    }

    /*
     * ------------------------------------------------------------------------------------------------------------------
     * PUBLIC METHODS OVERRIDEN FROM INTERFACE
     * ------------------------------------------------------------------------------------------------------------------
     */
    /**
     * Checks the repository availability
     *
     * @return This will return void if all is ok but will throw an exception if
     *         there are any problems
     * @throws FileNotFoundException
     */
    @Override
    public void checkRepositoryStatus() throws IOException {
        for (File f : assets) {
            if (!f.exists()) {
                throw new FileNotFoundException("Could not find " + f + " in the LooseFileRepository");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Asset> getAllAssets() throws IOException, RequestFailureException {
        Collection<Asset> allAssets = new ArrayList<Asset>();
        for (File f : assets) {
            try {
                allAssets.add(getAsset(f));
            } catch (BadVersionException e) {
                // Ignore assets with unknown versions
            }
        }
        return allAssets;
    }

    /** {@inheritDoc} */
    @Override
    public Asset getAsset(final String assetId) throws IOException, BadVersionException, RequestFailureException {
        return getAsset(new File(assetId));
    }

    @Override
    public InputStream getAttachment(final Asset asset, final Attachment attachment) throws IOException, BadVersionException, RequestFailureException {
        throw new UnsupportedOperationException("Loose config repositories do not support attachments");
    }

    protected Asset getAsset(final File asset) throws IOException, BadVersionException, RequestFailureException {
        Asset ass = readJson(asset);
        ass.set_id(asset.getAbsolutePath());

        // We always get a wlp info when read back from Massive so create one if there isnt already one
        WlpInformation wlpInfo = ass.getWlpInformation();
        if (wlpInfo == null) {
            wlpInfo = new WlpInformation();
            ass.setWlpInformation(wlpInfo);
        }
        if (wlpInfo.getAppliesToFilterInfo() == null) {
            wlpInfo.setAppliesToFilterInfo(Collections.<AppliesToFilterInfo> emptyList());
        }
        return ass;
    }

    /**
     * @throws BadVersionException
     * @throws IOException
     */
    protected Asset readJson(final File asset) throws IOException, BadVersionException {
        FileInputStream fis = null;
        try {
            fis = DirectoryUtils.createFileInputStream(asset);
            Asset ass = JSONAssetConverter.readValue(fis);
            return ass;
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

}
