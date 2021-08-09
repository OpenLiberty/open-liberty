/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

import com.ibm.ws.repository.transport.client.DirectoryUtils;
import com.ibm.ws.repository.transport.client.LooseFileClient;
import com.ibm.ws.repository.transport.exceptions.BadVersionException;
import com.ibm.ws.repository.transport.exceptions.ClientFailureException;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.Attachment;
import com.ibm.ws.repository.transport.model.AttachmentSummary;

/**
 * This class uses the same backing collection as the readable client. This way it can add assets to
 * the backing collection.
 */
public class LooseFileWriteableClient extends AbstractFileWriteableClient {

    Collection<File> _assets;
    File _root;

    public LooseFileWriteableClient(Collection<File> assets, File root) {
        _assets = assets;
        _root = root;
        _readClient = new LooseFileClient(assets);
    }

    /** {@inheritDoc} */
    @Override
    public Attachment addAttachment(String assetId, AttachmentSummary attSummary) throws IOException, BadVersionException, RequestFailureException, SecurityException {
        throw new UnsupportedOperationException("Attachment methods should not be invoked for Loose Repositories");
    }

    /** {@inheritDoc} */
    @Override
    public void deleteAttachment(String assetId, String attachmentId) throws IOException, RequestFailureException {
        throw new UnsupportedOperationException("Attachment methods should not be invoked for Loose Repositories");
    }

    /** {@inheritDoc} */
    @Override
    public void deleteAssetAndAttachments(String assetId) throws IOException, RequestFailureException {
        DirectoryUtils.delete(new File(assetId));
    }

    /** {@inheritDoc} */
    @Override
    public void writeJson(Asset asset, String path) throws IOException, IllegalArgumentException, IllegalAccessException {
        File targetFile = new File(path);
        try {
            writeDiskRepoJSONToFile(asset, targetFile);
            System.out.println("Adding asset " + path);
            _assets.add(targetFile);
        } catch (NullPointerException npe) {
            throw npe;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Override
    public Asset addAsset(Asset asset) throws IOException, BadVersionException, RequestFailureException, SecurityException, ClientFailureException {

        File location = _root;
        int dirDepth = (int) Math.round(Math.random() * 3);
        for (int currentDir = 0; currentDir < dirDepth; currentDir++) {
            location = new File(location.getAbsolutePath(), "dir" + Math.random());
            location.mkdirs();
        }
        location = new File(location, "Asset" + Math.random() + ".json");
        asset.set_id(location.getAbsolutePath());

        try {
            writeJson(asset, asset.get_id());
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

    private static void writeDiskRepoJSONToFile(Asset asset, final File writeJsonTo) throws IllegalArgumentException, IllegalAccessException, IOException {
        FileOutputStream fos = null;

        // Some loose files may point at root or just specify a file and not a path. In those cases
        // we won't have a parent directory.
        if (writeJsonTo.getParentFile() != null) {
            DirectoryUtils.mkDirs(writeJsonTo.getParentFile());
        }
        try {
            fos = DirectoryUtils.createFileOutputStream(writeJsonTo);
            asset.dumpMinimalAsset(fos);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }

    }

}
