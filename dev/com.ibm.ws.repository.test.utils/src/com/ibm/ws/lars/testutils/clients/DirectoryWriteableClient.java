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
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import com.ibm.ws.repository.transport.client.DirectoryClient;
import com.ibm.ws.repository.transport.client.DirectoryUtils;
import com.ibm.ws.repository.transport.exceptions.BadVersionException;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.Attachment;
import com.ibm.ws.repository.transport.model.AttachmentSummary;

/**
 *
 */
public class DirectoryWriteableClient extends AbstractFileWriteableClient {

    private final File _root;

    public DirectoryWriteableClient(File root) {
        _root = root;
        _readClient = new DirectoryClient(_root);
    }

    public static void writeDiskRepoJSONToFile(Asset asset, final File writeJsonTo) throws IllegalArgumentException, IllegalAccessException, IOException {
        FileOutputStream fos = null;
        DirectoryUtils.mkDirs(writeJsonTo.getParentFile());
        try {
            fos = DirectoryUtils.createFileOutputStream(writeJsonTo);
            asset.dumpMinimalAsset(fos);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }

    }

    @Override
    public void writeJson(Asset asset, String path) throws IllegalArgumentException, IllegalAccessException, IOException {
        File targetFile = new File(_root, path.toString() + ".json");
        writeDiskRepoJSONToFile(asset, targetFile);
    }

    public void downloadToFile(InputStream is, final File fileToWriteTo) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = DirectoryUtils.createFileOutputStream(fileToWriteTo);

            byte[] buffer = new byte[1024];
            int read = 0;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
        } finally {
            if (null != fos) {
                fos.close();
            }
            if (null != is) {
                is.close();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Attachment addAttachment(String assetId, AttachmentSummary attSummary) throws IOException, BadVersionException, RequestFailureException, SecurityException {
        File source = attSummary.getFile();
        Attachment att = attSummary.getAttachment();
        StringBuffer targetPath = new StringBuffer(assetId);
        switch (attSummary.getAttachment().getType()) {
            case CONTENT:
                // target path is right for the main attachment
                break;
            case DOCUMENTATION:
            case ILLUSTRATION:
            case THUMBNAIL:
                targetPath.append(".");
                targetPath.append(attSummary.getAttachment().getType().toString());

                // Create directory at this point
                new File(_root, targetPath.toString()).mkdir();

                targetPath.append(File.separator);
                targetPath.append(attSummary.getName());
                break;
            case LICENSE:
            case LICENSE_AGREEMENT:
            case LICENSE_INFORMATION:
                targetPath.append(".licenses");
                targetPath.append(File.separator);
                targetPath.append(attSummary.getName());

                // Create directory at this point
                new File(_root, targetPath.toString()).mkdir();

                break;
        }

        att.setAssetId(assetId);
        att.set_id(targetPath.toString());
        // We don't really use the URL but to be consistent with massive we can set it to the supplied value or create one if not specified
        att.setUrl(attSummary.getURL() == null ? targetPath.toString() : attSummary.getURL());
        att.setUploadOn(Calendar.getInstance());

        // This is done to give a random value to the attachment, it is only used as a way of ensuring that each time this method is called
        // there is some unique value stored in the attachment.
        att.setGridFSId("" + Math.random());
        InputStream is = DirectoryUtils.createFileInputStream(source);
        downloadToFile(is, new File(_root, targetPath.toString()));

        return att;
    }

    /** {@inheritDoc} */
    @Override
    public void deleteAttachment(String assetId, String attachmentId) throws IOException, RequestFailureException {
        DirectoryUtils.delete(createFromRelative(attachmentId));
    }

    /** {@inheritDoc} */
    @Override
    public void deleteAssetAndAttachments(String assetId) throws IOException, RequestFailureException {
        final File main = new File(_root, assetId);
        final String mainName = main.getName();
        File parent = main.getParentFile();
        File[] toDelete = DirectoryUtils.listFiles(parent, new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (mainName.equals(name)) {
                    return true;
                }
                if ((mainName + ".json").equals(name)) {
                    return true;
                }
                if ((mainName + ".license").equals(name)) {
                    return true;
                }
                if (name.contains(mainName + ".") && DirectoryUtils.isDirectory(dir))
                {
                    // TODO: Is this one we should delete? Another attachment type?
                }
                return false;
            }
        });

        if (toDelete != null) {
            for (File f : toDelete) {
                if (DirectoryUtils.isDirectory(f)) {
                    recursivelyDelete(f);
                }
                DirectoryUtils.delete(f);
            }
        }
    }

    private void recursivelyDelete(File f) {
        if (DirectoryUtils.isDirectory(f)) {
            File[] children = DirectoryUtils.listFiles(f);
            for (File child : children) {
                recursivelyDelete(child);
            }
        }
        DirectoryUtils.delete(f);
    }

    private File createFromRelative(final String relative) {
        return new File(_root, relative);
    }

}
