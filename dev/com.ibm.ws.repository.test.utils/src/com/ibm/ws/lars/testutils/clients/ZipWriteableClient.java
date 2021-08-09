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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.ibm.ws.repository.transport.client.DirectoryUtils;
import com.ibm.ws.repository.transport.client.ZipClient;
import com.ibm.ws.repository.transport.exceptions.BadVersionException;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.Attachment;
import com.ibm.ws.repository.transport.model.AttachmentSummary;

/**
 *
 */
public class ZipWriteableClient extends AbstractFileWriteableClient {

    private final File _zip;

    public ZipWriteableClient(File root) {
        _zip = root;
        _readClient = new ZipClient(_zip);
    }

    /**
     *
     * @param zip
     * @param pathToWriteTo Do not include this file when making a copy of the zip as the calling
     *            code is about to write a new vesion of it
     * @return
     * @throws IOException
     */
    public static ZipOutputStream appendToZip(final File zip, String pathToWriteTo) throws IOException {

        if (!zip.exists()) {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip));
            zos.putNextEntry(new ZipEntry("dummy"));
            zos.write((byte) 1);
            return zos;
        }

        File temp = File.createTempFile("tempRepo", ".zip", zip.getParentFile());
        if (!temp.delete()) {
            throw new IOException("Unable to delete temp file " + temp.getAbsolutePath());
        }
        org.apache.commons.io.FileUtils.moveFile(zip, temp);

        // Copy contents into new zip file
        ZipInputStream readFrom = new ZipInputStream(new FileInputStream(temp));
        ZipOutputStream writeTo = new ZipOutputStream(new FileOutputStream(zip));

        ZipEntry readEntry = readFrom.getNextEntry();
        byte[] buffer = new byte[1024];
        while (readEntry != null) {
            boolean copyFile = true;
            if (pathToWriteTo.endsWith("*")) {
                // Ignore this file as the called is about to replace it (if this is the asset itself then all attachments will be deleted too
                // as they all startWith the asset name). If pathToWrite to is an attachment then only that attachment should be deleted.
                copyFile = !readEntry.getName().startsWith(pathToWriteTo.substring(0, pathToWriteTo.length() - 1));
            } else {
                // Non wild card ignore so only delete the one file
                copyFile = !readEntry.getName().equals(pathToWriteTo);
            }
            if (copyFile) {
                writeTo.putNextEntry(new ZipEntry(readEntry.getName()));
                if (!readEntry.isDirectory()) {
                    int bytesRead;
                    while ((bytesRead = readFrom.read(buffer)) > 0) {
                        writeTo.write(buffer, 0, bytesRead);
                    }
                }
            }
            readEntry = readFrom.getNextEntry();
        }

        // We've finished reading so can close the input stream now...still need output one
        readFrom.close();
        temp.delete();

        return writeTo;
    }

    public static void writeDiskRepoJSONToFile(final Asset asset, final File zip, final String path) throws IllegalArgumentException, IllegalAccessException, IOException {

        ZipOutputStream zos = appendToZip(zip, path + ".json");
        try {
            zos.putNextEntry(new ZipEntry(path + ".json"));
            asset.dumpMinimalAsset(zos);
        } finally {
            if (null != zos) {
                zos.close();
            }
        }
    }

    @Override
    public void writeJson(Asset asset, String path) throws IllegalArgumentException, IllegalAccessException, IOException {
        writeDiskRepoJSONToFile(asset, _zip, path);
    }

    public void downloadToFile(InputStream is, final String pathToWriteTo) throws IOException {
        // TODO: THIS BLATS THE ZIP FILE, FIND A WAY TO APPEND
        System.out.println("Writing " + pathToWriteTo);
        ZipOutputStream zos = appendToZip(_zip, pathToWriteTo);
        try {
            zos.putNextEntry(new ZipEntry(pathToWriteTo));
            byte[] buffer = new byte[1024];
            int read = 0;
            while ((read = is.read(buffer)) != -1) {
                zos.write(buffer, 0, read);
            }
        } finally {
            if (null != zos) {
                zos.close();
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
                targetPath.append(File.separator);
                targetPath.append(attSummary.getName());
                break;
            case LICENSE:
            case LICENSE_AGREEMENT:
            case LICENSE_INFORMATION:
                targetPath.append(".licenses");
                targetPath.append(File.separator);
                targetPath.append(attSummary.getName());
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
        downloadToFile(is, targetPath.toString());

        return att;
    }

    /** {@inheritDoc} */
    @Override
    public void deleteAttachment(String assetId, String attachmentId) throws IOException, RequestFailureException {
        ZipOutputStream zos = appendToZip(_zip, attachmentId);
        zos.close();
    }

    /** {@inheritDoc} */
    @Override
    public void deleteAssetAndAttachments(String assetId) throws IOException, RequestFailureException {
        ZipOutputStream zos = appendToZip(_zip, assetId + "*");
        zos.close();
    }

}
