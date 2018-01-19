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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.aries.util.manifest.ManifestProcessor;

import com.ibm.ws.repository.transport.exceptions.BadVersionException;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.Attachment;

/**
 *
 */
public class DirectoryClient extends AbstractFileClient {

    private final File _root;

    public DirectoryClient(File root) {
        _root = root;
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
        if (!exists(null)) {
            throw new FileNotFoundException("Could not find " + _root);
        }
        if (!hasChildren(null)) {
            throw new IOException("The root (" + _root + " is not a directory ");
        }
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getAttachment(final Asset asset, final Attachment attachment) throws IOException, BadVersionException, RequestFailureException {
        String attachmentId = attachment.get_id();
        if (attachmentId.contains("#")) {
            // new funky code to get an input stream to the license *inside* the main attachment. The start of
            // the assetId will point to the main attachment file.
            String assetId = asset.get_id();
            File zipFile = new File(_root, assetId);
            ZipFile zip = DirectoryUtils.createZipFile(zipFile);

            FileInputStream fis = DirectoryUtils.createFileInputStream(zipFile);
            ZipInputStream zis = new ZipInputStream(fis);

            try {
                return getInputStreamToLicenseInsideZip(zis, assetId, attachmentId);
            } finally {
                zip.close();
            }
        } else {
            return DirectoryUtils.createFileInputStream(createFromRelative(attachmentId));
        }
    }

    /*
     * ------------------------------------------------------------------------------------------------------------------
     * PROTECTED AND OVERRIDEABLE IMPLEMENTATION METHODS
     * ------------------------------------------------------------------------------------------------------------------
     */

    @Override
    protected boolean exists(final String relative) {
        return DirectoryUtils.exists(createFromRelative(relative));
    }

    @Override
    protected boolean hasChildren(final String relative) {
        return DirectoryUtils.isDirectory(createFromRelative(relative));
    }

    @Override
    protected long getSize(final String relative) {
        return DirectoryUtils.length(createFromRelative(relative));
    }

    @Override
    protected Collection<String> getChildren(final String relative) {
        String[] fullPath = DirectoryUtils.list(createFromRelative(relative));

        // Return an empty list if there are no children
        if (fullPath == null) {
            return Collections.emptyList();
        }

        // If we are in a sub directory then prefix relative path to this point. Don't do this if we
        // are at the root directory (relative is the empty string)
        if (relative.length() > 0) {
            for (int i = 0; i < fullPath.length; i++) {
                fullPath[i] = relative + File.separator + fullPath[i];
            }
        }
        ArrayList<String> children = new ArrayList<String>();

        children.addAll(Arrays.asList(fullPath));

        for (String s : fullPath) {
            if (hasChildren(s)) {
                children.addAll(getChildren(s));
            }

        }

        return children;
    }

    protected File createFromRelative(final String relative) {
        return relative == null ? _root : new File(_root, relative);
    }

    /**
     * {@inheritDoc}
     *
     * @throws BadVersionException
     * @throws IOException
     */
    @Override
    protected Asset readJson(final String assetId) throws IOException, BadVersionException {
        FileInputStream fis = null;
        try {
            fis = DirectoryUtils.createFileInputStream(createFromRelative(assetId + ".json"));
            Asset ass = processJSON(fis);
            return ass;
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    @Override
    protected Map<String, Long> getLicenses(final String assetId) throws IOException {

        boolean isEsa = assetId.toLowerCase().endsWith(".esa");
        File zipFile = new File(_root, assetId);
        ZipFile zip = DirectoryUtils.createZipFile(zipFile);
        Map<String, Long> licenses = new HashMap<String, Long>();

        String liLocation = getHeader(assetId, isEsa ? LI_HEADER_FEATURE : LI_HEADER_PRODUCT);
        String laLocation = getHeader(assetId, isEsa ? LA_HEADER_FEATURE : LA_HEADER_PRODUCT);
        if (liLocation != null || laLocation != null) {
            FileInputStream fis = null;
            ZipInputStream zis = null;
            try {
                fis = DirectoryUtils.createFileInputStream(zipFile);
                zis = new ZipInputStream(fis);
                ZipEntry ze = zis.getNextEntry();
                while (ze != null) {
                    if (ze.isDirectory()) {
                        //do nothing
                    } else {
                        if ((liLocation != null && ze.getName().startsWith(liLocation)) ||
                            (laLocation != null && ze.getName().startsWith(laLocation))) {
                            licenses.put(ze.getName().replace("/", File.separator), ze.getSize());
                        }
                    }
                    ze = zis.getNextEntry();
                }
            } finally {
                if (zis != null) {
                    zis.closeEntry();
                    zis.close();
                }
                if (zip != null) {
                    zip.close();
                }
                if (fis != null) {
                    fis.close();
                }
            }
        }

        return licenses;

    }

    @Override
    protected String getHeader(String assetId, String type) throws IOException {
        return getManifest(assetId).getMainAttributes().getValue(type);
    }

    protected Manifest getManifest(String assetId) throws IOException {
        boolean isEsa = assetId.toLowerCase().endsWith(".esa");
        if (isEsa) {
            return getSubsystemManifest(assetId);
        } else {
            return getJarManifest(assetId);
        }
    }

    /**
     * Gets the manifest file from an ESA
     *
     * @param assetId
     * @return
     * @throws IOException
     */
    protected Manifest getSubsystemManifest(String assetId) throws IOException {
        ZipFile zip = null;
        try {
            zip = DirectoryUtils.createZipFile(new File(_root, assetId));
            Enumeration<? extends ZipEntry> zipEntries = zip.entries();
            ZipEntry subsystemEntry = null;
            while (zipEntries.hasMoreElements()) {
                ZipEntry nextEntry = zipEntries.nextElement();
                if ("OSGI-INF/SUBSYSTEM.MF".equalsIgnoreCase(nextEntry.getName())) {
                    subsystemEntry = nextEntry;
                    break;
                }
            }
            if (subsystemEntry == null) {
                return null;
            } else {
                return ManifestProcessor.parseManifest(zip.getInputStream(subsystemEntry));
            }
        } finally {
            if (zip != null) {
                zip.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException
     */
    protected Manifest getJarManifest(String assetId) throws IOException {
        File zipFile = new File(_root, assetId);
        ZipFile zip = DirectoryUtils.createZipFile(zipFile);

        String mfName = "meta-inf/manifest.mf";
        Manifest manifest = new Manifest();
        FileInputStream fis = DirectoryUtils.createFileInputStream(zipFile);
        ZipInputStream zis = new ZipInputStream(fis);
        try {
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                if (ze.isDirectory()) {
                    // Do nothing with pure directory entries
                } else {
                    String fileName = ze.getName();
                    if (fileName.equalsIgnoreCase(mfName)) {
                        InputStream is = zip.getInputStream(ze);
                        manifest.read(is);
                    }
                }
                ze = zis.getNextEntry();
            }
        } finally {
            zis.closeEntry();
            zis.close();
            zip.close();
        }

        return manifest;
    }
}
