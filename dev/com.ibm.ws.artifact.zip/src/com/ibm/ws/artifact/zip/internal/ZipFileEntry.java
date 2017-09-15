/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.zip.internal;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.NavigableMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.artifact.ExtractableArtifactEntry;
import com.ibm.ws.artifact.zip.cache.ZipFileHandle;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 * Represents an Entry underpinned by data in a Zip file.
 * <p>
 * As the Container/Entry api requires entries for all directories, and Zip does not,
 * sometimes, this ZipEntry may be representing a 'virtual' directory entry within the
 * Zip archive. These are created when entries are present in the Zip within a subdir, where
 * some, or all of the parent dirs for the entry are nto present as dir entries within the zip.
 */
public class ZipFileEntry implements ExtractableArtifactEntry {

    static final TraceComponent tc = Tr.register(ZipFileEntry.class);

    private ArtifactContainer enclosingContainer;
    private final ZipEntry zipEntry;
    private final ZipFileContainer rootContainer;
    private final NavigableMap<String, ZipEntry> allEntries;
    private final String name;
    private final String path;

    private final File archiveFile; //may stay null for nested archives

    private final ContainerFactoryHolder containerFactoryHolder;

    /**
     * Create a zip entry for the fs being represented by the ZipFileContainer.<p>
     * 
     * @param zc The ZipFileContainer this entry belongs to.
     * @param f The ZipEntry representing this Entry (may be null if entry is virtual).
     * @param name The name of this Entry
     * @param path The path of this Entry
     * @param af The File holding the Zip data (may be null for non File based Zip data).
     * @param parentEntry The Entry representing the Zip data in an enclosing Container. (may be null for File based Zips opened directly)
     * @param allEntries The sorted map containing Paths -> ZipEntries for this ZipFileContainer
     */
    @Trivial
    ZipFileEntry(ZipFileContainer zc, ZipEntry f, String name, String path, File af, NavigableMap<String, ZipEntry> allEntries,
                 ContainerFactoryHolder cfh) {

        //Injected logging also logs the contents of allEntries. This appears to cause either a very large
        //or infinite amount of trace. Moving this constructor to trivial and printing out the size
        //of allEntries instead should still be somewhat useful.
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "<init>", zc, f, name, path, af, (allEntries == null ? null : allEntries.size()), cfh);
        }

        this.zipEntry = f;
        this.rootContainer = zc;
        this.archiveFile = af;
        this.allEntries = allEntries;
        this.name = name;
        this.path = path;
        this.containerFactoryHolder = cfh;

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "<init>");
        }
    }

    @Override
    public ArtifactContainer getEnclosingContainer() {
        if (enclosingContainer == null) {
            String parent = PathUtils.getParent(path);
            if (parent == null || "".equals(parent) || "/".equals(parent)) {
                enclosingContainer = rootContainer;
            } else {
                //slightly recursive call here, will propagate back to the closest real container (or the root)
                //causing creation of the chain of parent entry/containers to return.
                ArtifactEntry parentEntry = rootContainer.getEntry(parent, true);
                enclosingContainer = parentEntry.convertToContainer();
            }
        }

        return enclosingContainer;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ArtifactContainer convertToContainer() {
        return convertToContainer(false);
    }

    @Override
    public ArtifactContainer convertToContainer(boolean localOnly) {
        ArtifactContainer rv = null;

        if (zipEntry != null) {
            if (zipEntry.isDirectory()) {
                //entry is non null, AND is a directory in the zip, so build one for it.                
                rv = new ZipFileNestedDirContainer(rootContainer, getEnclosingContainer(), archiveFile, this, allEntries, name, containerFactoryHolder);
            }
        }

        if ((zipEntry != null && !zipEntry.isDirectory()) && rv == null && !localOnly) {
            //entry is non null, and is not a directory.. try to use services to convert it to a container.
            File newCacheDir = rootContainer.getNewCacheDirForEntry(this);

            //            System.out.println("Zip Container delegating to container factory for convert.."
            //                               + "\n - CacheDir:" + newCacheDir.getAbsolutePath()
            //                               + "\n - Path:" + this.getPath()
            //                              );
            rv = containerFactoryHolder.getContainerFactory().getContainer(newCacheDir, this.getEnclosingContainer(), this, zipEntry); //passes zipentry.. might be useful.
        }

        //if entry is null, we represent a fake directory entry.. 
        if (zipEntry == null && rv == null) {
            rv = new ZipFileNestedDirContainer(rootContainer, getEnclosingContainer(), archiveFile, this, allEntries, name, containerFactoryHolder);
        }

        return rv;
    }

    @Override
    public InputStream getInputStream() throws IOException {

        if ((zipEntry == null) || zipEntry.isDirectory()) {
            return null;
        }

        // either we get the inputstream from a ZipFile over the File,
        // or we get it via a ZipInputStream over the parentEntry.getInputStream.

        // The fast mode setting and the zip file must be obtained in a single operation.

        // An exception here has not yet opened the zip file, meaning a close is not yet necessary.
        final ZipFileHandle zfh = rootContainer.getZipFileHandle();
        final ZipFile zf = zfh.open();

        // A zip file has been obtained.  That zip file must eventually be closed.

        InputStream baseInputStream;
        try {
            baseInputStream = zfh.getInputStream(zf, zipEntry); // throws IOException
        } catch (IOException e) {
            // A failure to obtain the zip entry input stream does an immediate return
            // through a thrown exception.  Since the caller was not successfully given
            // the input stream, and has not yet assumed responsibility for the code,
            // this code must sill do the close.
            zfh.close(); // throws IOException
            throw e;
        }

        // Once the base input stream is successfully obtained, there are
        // no additional possible failures.  The caller is given the requirement
        // to close the result input stream.

        InputStream wrappedInputStream = new FilterInputStream(baseInputStream) {

            boolean isClosed = false;

            @Override
            public synchronized void close() throws IOException {
                if (!isClosed) {
                    try {
                        super.close();
                    } catch (IOException e) { /* swallow */
                    }
                    zfh.close();
                    isClosed = true;
                }
            }
        };
        return wrappedInputStream;

    }

    /** {@inheritDoc} */
    @Override
    public long getSize() {
        if (zipEntry != null) {
            return zipEntry.getSize();
        } else {
            return 0L;
        }
    }

    /** {@inheritDoc} */
    @Override
    public ArtifactContainer getRoot() {
        return rootContainer;
    }

    /** {@inheritDoc} */
    @Override
    public long getLastModified() {
        if (zipEntry != null) {
            return zipEntry.getTime();
        } else {
            return 0L;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @throws MalformedURLException
     */
    @Override
    @FFDCIgnore(MalformedURLException.class)
    public URL getResource() {
        //Makes path to directories within zip hierarchy, have urls ending in /

        //this is required, as classloader resource urls for directories should end in /
        //and the result of this getResource call is now returned by the classloader
        String path = getPath();
        //if zipEntry is null, we are a 'fake' entry, representing a directory
        // ('fake' entries are only use for directories)
        //if zipEntry is not null, we can ask it if it is a directory.
        if (zipEntry == null || zipEntry.isDirectory()) {
            path += "/";
        }

        // We are in a JAR so use the jar:<url>!/<path_in_jar> sytax for our URI
        URI entryUri = rootContainer.createEntryUri(path, archiveFile);// rootContainer);
        if (entryUri != null) {
            try {
                return entryUri.toURL();
            } catch (MalformedURLException e) {
                // In some cases an attempt is made to get a resource using the wsjar protocol
                // after the protocol has been deregistered.  It would be too much of a behavior change
                // to properly enforce the dependency on the wsjar protocol for all components. 
                // Instead, only log a debug statement if a MalformedURLException is caught during shutdown
                if (FrameworkState.isStopping()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "MalformedURLException during OSGi framework stop.", e.getMessage());
                } else {
                    FFDCFilter.processException(e, getClass().getName(), "269");
                }
                return null;
            }
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getPhysicalPath() {
        return null;
    }

    @Override
    public File extract() throws IOException {
        return rootContainer.extractEntryToCacheFile(this);
    }
}
