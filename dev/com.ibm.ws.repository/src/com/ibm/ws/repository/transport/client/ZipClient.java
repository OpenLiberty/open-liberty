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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
public class ZipClient extends AbstractFileClient {

    private final File _zip;

    /**
     * Create a zip client which points to the specified zip file
     *
     * @param zip The zip file the client should read from
     */
    public ZipClient(File zip) {
        _zip = zip;
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
        if (!DirectoryUtils.exists(_zip)) {
            throw new FileNotFoundException("Could not find " + _zip);
        }
        // This will throw an exception if the file is not a zip
        ZipFile zip = DirectoryUtils.createZipFile(_zip);
        // if the creation of the zip file is successful ensure it is closed to release the lock
        zip.close();
    }

    /**
     * This gets an input stream to the specified attachment in the zip.
     */
    @Override
    public InputStream getAttachment(final Asset asset, final Attachment attachment) throws IOException, BadVersionException, RequestFailureException {
        final ZipFile repoZip = createZipFile();

        if (null == repoZip) {
            return null;
        }

        InputStream retInputStream = null;
        String attachmentId = attachment.get_id();

        try {
            if (attachmentId.contains("#")) {
                String assetId = asset.get_id();
                // new funky code to get an input stream to the license *inside* the main attachment. The start of
                // the assetId will point to the main attachment file.
                ZipEntry entry = createFromRelative(assetId);

                // If the entry wasn't found return null
                if (null == entry) {
                    return null;
                }

                // Get zip input stream to the asset inside the zip
                ZipInputStream zis = new ZipInputStream(repoZip.getInputStream(entry));

                // Get the input stream to the attachment inside the zip
                retInputStream = getInputStreamToLicenseInsideZip(zis, assetId, attachmentId);

            } else {
                // Get input stream to the attachment
                ZipEntry entry = createFromRelative(attachmentId);
                retInputStream = repoZip.getInputStream(entry);
            }
        } finally {
            // If we are throwing an exception the InputStream is never created so the logic below
            // to close the ZipFile when the InputStream is closed will never be called. So lets close
            // the ZipFile now as there is no InputStream to read from.
            if (retInputStream == null) {
                repoZip.close();
            }
        }

        // When the input stream gets closed we also need to close the ZipFile, however
        // the caller only has the input stream. So lets wrap the input stream and close
        // both the inputStream and the ZipFile when the caller calls close on the wrapped
        // input stream
        final InputStream is = retInputStream;
        InputStream wrappedIs = new InputStream() {

            /** {@inheritDoc} */
            @Override
            public int read(byte[] b) throws IOException {
                return is.read(b);
            }

            /** {@inheritDoc} */
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return is.read(b, off, len);
            }

            /** {@inheritDoc} */
            @Override
            public long skip(long n) throws IOException {
                return is.skip(n);
            }

            /** {@inheritDoc} */
            @Override
            public int available() throws IOException {
                return is.available();
            }

            /** {@inheritDoc} */
            @Override
            public synchronized void mark(int readlimit) {
                is.mark(readlimit);
            }

            /** {@inheritDoc} */
            @Override
            public synchronized void reset() throws IOException {
                is.reset();
            }

            /** {@inheritDoc} */
            @Override
            public boolean markSupported() {
                return is.markSupported();
            }

            /** {@inheritDoc} */
            @Override
            public int read() throws IOException {
                return is.read();
            }

            /** {@inheritDoc} */
            @Override
            public void close() throws IOException {
                // When the input stream is closed, also close the zip file
                is.close();
                repoZip.close();
            }
        };
        return wrappedIs;
    }

    /*
     * ------------------------------------------------------------------------------------------------------------------
     * PROTECTED AND OVERRIDEABLE IMPLEMENTATION METHODS
     * ------------------------------------------------------------------------------------------------------------------
     */

    /**
     * Read the zip file to see if the relative path exists
     */
    @Override
    protected boolean exists(final String relative) {
        return (createFromRelative(relative) != null);
    }

    /**
     * See if there are any assets under the specified directory in the zip
     */
    @Override
    protected boolean hasChildren(final String relative) throws IOException {
        ZipFile zip = createZipFile();
        if (null == zip) {
            return false;
        }
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if ((relative.equals("")) || entry.getName().startsWith(relative + File.separator)) {
                return true;
            }
        }

        zip.close();
        return false;
    }

    /**
     * Gets the entries under the specified directory in the zip file. This currently gets all entries
     * in all sub directories too but does not return other directories as I think this will be more
     * efficient than trying to recursively go through sub directories and we only call this method when
     * we want all entries under the specified directory, including all sub directories.
     */
    @Override
    protected Collection<String> getChildren(final String relative) throws IOException {

        ZipFile zip = createZipFile();
        if (null == zip) {
            return Collections.emptyList();
        }

        Collection<String> children = new ArrayList<String>();
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            if ((relative.equals("")) || entry.getName().startsWith(relative + File.separator)) {
                children.add(entry.getName());
            }
        }

        zip.close();
        return children;
    }

    /**
     * Gets the uncompressed size of the file specified, will return 0 if the entry was not found
     */
    @Override
    protected long getSize(final String relative) {
        ZipEntry entry = createFromRelative(relative);
        return (entry == null ? 0 : entry.getSize());
    }

    protected ZipEntry createFromRelative(final String relative) {
        ZipFile zip = null;
        ZipEntry entry = null;
        zip = createZipFile();
        if (null == zip) {
            return null;
        }

        entry = zip.getEntry(relative);

        try {
            zip.close();
        } catch (IOException e) {
            // Exception on closing, not a lot we can do
        }
        return entry;
    }

    /**
     * {@inheritDoc}
     *
     * @throws BadVersionException
     * @throws IOException
     */
    @Override
    protected Asset readJson(final String assetId) throws IOException, BadVersionException {
        ZipFile zip = createZipFile();

        ZipEntry entry = createFromRelative(assetId + ".json");

        if (entry != null) {
            InputStream is = null;
            try {
                is = zip.getInputStream(entry);
                Asset ass = processJSON(is);
                return ass;
            } finally {
                if (is != null) {
                    is.close();
                }
                zip.close();
            }
        } else {
            zip.close();
            throw new IOException("The asset " + assetId + " does not exist");
        }
    }

    @Override
    protected Map<String, Long> getLicenses(final String assetId) throws IOException {

        boolean isEsa = assetId.toLowerCase().endsWith(".esa");

        // Return map containing the license info.
        Map<String, Long> licenses = new HashMap<String, Long>();

        // Create a zip file (this is the whole repo contents).
        ZipFile repoZip = createZipFile();

        // We can't read the repo so return an empty map
        if (repoZip == null) {
            return licenses;
        }

        // Get the entry from the repo zip that points to the asset we are interested in
        ZipEntry innerZip = createFromRelative(assetId);

        String liLocation = getHeader(assetId, isEsa ? LI_HEADER_FEATURE : LI_HEADER_PRODUCT);
        String laLocation = getHeader(assetId, isEsa ? LA_HEADER_FEATURE : LA_HEADER_PRODUCT);
        if (liLocation != null || laLocation != null) {
            // Input stream to the inner zip
            ZipInputStream zis = new ZipInputStream(repoZip.getInputStream(innerZip));
            try {
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
                repoZip.close();
            }
        }

        return licenses;
    }

    @Override
    protected String getHeader(String assetId, String type) throws IOException {
        Manifest mf = getManifest(assetId);
        // If we don't have a manifest return null
        if (null == mf) {
            return null;
        }
        return getManifest(assetId).getMainAttributes().getValue(type);
    }

    /**
     * Gets the manifest for the specified asset
     *
     * @param assetId
     * @return
     * @throws IOException
     */
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
        ZipFile repoZip = null;
        try {
            repoZip = createZipFile();
            ZipEntry entry = createFromRelative(assetId);

            // Empty / non exisstent zip or manifest can't be found
            if (null == repoZip || null == entry) {
                return null;
            }

            // Input stream to the inner zip
            ZipInputStream zis = new ZipInputStream(repoZip.getInputStream(entry));
            ZipEntry innerEntry = zis.getNextEntry();
            ZipEntry subsystemEntry = null;
            while (null != innerEntry) {
                if ("OSGI-INF/SUBSYSTEM.MF".equalsIgnoreCase(innerEntry.getName())) {
                    subsystemEntry = innerEntry;
                    break;
                }
                innerEntry = zis.getNextEntry();
            }
            if (subsystemEntry == null) {
                return null;
            } else {
                return ManifestProcessor.parseManifest(zis);
            }
        } finally {
            if (repoZip != null) {
                repoZip.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException
     */
    protected Manifest getJarManifest(String assetId) throws IOException {
        ZipFile repoZip = null;
        Manifest manifest = new Manifest();
        try {
            repoZip = createZipFile();
            ZipEntry entry = createFromRelative(assetId);

            if (null == entry) {
                return null;
            }

            // Input stream to the inner zip
            ZipInputStream zis = new ZipInputStream(repoZip.getInputStream(entry));
            ZipEntry innerEntry = zis.getNextEntry();
            while (null != innerEntry) {
                if ("meta-inf/manifest.mf".equalsIgnoreCase(innerEntry.getName())) {
                    manifest.read(zis);
                    break;
                }
                innerEntry = zis.getNextEntry();
            }
        } finally {
            if (repoZip != null) {
                repoZip.close();
            }
        }
        return manifest;
    }

    protected ZipFile createZipFile() {
        try {
            ZipFile zip = DirectoryUtils.createZipFile(_zip);
            return zip;
        } catch (IOException e) {
            return null;
        }
    }

}
