/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.zip.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipFile;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.artifact.ExtractableArtifactEntry;
import com.ibm.ws.artifact.zip.cache.ZipFileHandle;
import com.ibm.ws.artifact.zip.internal.ZipFileContainerUtils.ZipEntryData;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 * An entry within a zip file container.
 *
 * Zip file entries are of three types:
 *
 * <ul>
 * <li>Entries which have a zip entry and which are not directory entries.</li>
 * <li>Entries which have a zip entry and which are directory entries.</li>
 * <li>Entries which do not have a zip entry, which are always directory
 *     entries.</li>
 * </ul>
 */
public class ZipFileEntry implements ExtractableArtifactEntry {
    static final TraceComponent tc = Tr.register(ZipFileEntry.class);

    /**
     * Create an entry of a zip file type container.
     *
     * The zip entry will be null when the zip file entry is an implied entry.
     *
     * A zip entry is a directory entry and will convert to a
     * {@link ZipFileNestedDirContainer} when the zip entry is null or is a
     * directory entry.
     *
     * The enclosing container is either a {@link ZipFileContainer} or a
     * {@link ZipFileNestedDirContainer}.  The common type of these is
     * {@link ArtifactContainer}.  (TODO: The common type does not
     * express the commonality between the actually possible types.)
     *
     * @param rootContainer The root zip file type container of this entry.
     * @param enclosingContainer The container enclosing this entry.
     * @param zipEntryData The zip entry of this entry.
     * @param name The name of this entry.
     * @param a_path The absolute path of this entry.
     */
    @Trivial
    protected ZipFileEntry(
        ZipFileContainer rootContainer,
        ArtifactContainer enclosingContainer,
        ZipEntryData zipEntryData,
        String name, String a_path) {

        this.rootContainer = rootContainer;
        this.enclosingContainer = enclosingContainer;

        this.zipEntryData = zipEntryData;

        this.name = name;
        this.a_path = a_path;
    }

    //

    private final ZipFileContainer rootContainer;

    @Trivial
    @Override
    public ArtifactContainer getRoot() {
        return rootContainer;
    }

    @Trivial
    public ContainerFactoryHolder getContainerFactoryHolder() {
        return rootContainer.getContainerFactoryHolder();
    }

    @Override
    public File extract() throws IOException {
        return rootContainer.extract(this);
    }

    //

    private final ZipEntryData zipEntryData;

    @Trivial
    public ZipEntryData getZipEntryData() {
        return zipEntryData;
    }

    private final AtomicBoolean jarProtocolVirtualDirWarning = new AtomicBoolean();
    /**
     * Answer the URL of this entry.
     *
     * Directory type entries have a trailing "/".  That is required by
     * {@link ClassLoader#getResource(String)}.
     *
     * Answer null if a malformed URL is obtained.
     *
     * @return The URL for this entry.
     */
    @Override
    @FFDCIgnore(MalformedURLException.class)
    public URL getResource() {
        String useRelPath = getRelativePath();

        URI entryUri = rootContainer.createEntryUri(useRelPath);
        if ( entryUri == null ) {
            return null;
        } else if (zipEntryData == null && "jar".equals(rootContainer.getProtocol())) {
            // If the zipEntryData is null this is a "virtualized" entry that doesn't actually
            // exist in the ZIP file.  When using the "jar" protocol we should not return
            // URLs for virtualized entries because the JarURLConnection will not be able to
            // connect to the non-existing JAR entry.
            
            // Post a warning the first time this happens
            if (jarProtocolVirtualDirWarning.compareAndSet(false, true)) {
                Tr.warning(tc, "CWWKM0129W.missing.dir.entry", rootContainer.getArchiveFilePath(), useRelPath);
            }
            return null;
        }

        try {
            return entryUri.toURL(); // throws MalformedURLException
        } catch ( MalformedURLException e ) {
            // In some cases an attempt is made to get a resource using the wsjar protocol
            // after the protocol has been deregistered.  It would be too much of a behavior change
            // to properly enforce the dependency on the wsjar protocol for all components.
            // Instead, only log a debug statement if a MalformedURLException is caught during
            // shutdown.
            if ( FrameworkState.isStopping() ) {
                if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    Tr.debug(tc, "MalformedURLException during OSGi framework stop.", e.getMessage());
                } else {
                    FFDCFilter.processException(e, getClass().getName(), "269");
                }
            }
            return null;
        }
    }

    /**
     * Obtain an input stream for the entry.
     *
     * Answer null for directory entries.  That is, when the zip entry is not
     * available and when the zip entry is a directory entry.
     *
     * The input stream which is obtained should be closed as soon as possible
     * following use.
     *
     * @return An input stream for the entry.
     */
    @Override
    public InputStream getInputStream() throws IOException {
        if ( (zipEntryData == null) || zipEntryData.isDirectory() ) {
            return null;
        }

        final ZipFileHandle zipFileHandle = rootContainer.getZipFileHandle(); // throws IOException

        ZipFile zipFile = zipFileHandle.open();

        // The open must have a balancing close.  That should be done by the caller.
        // In the worst case, 'finalize' makes sure it happens.

        final InputStream baseInputStream;
        try {
            baseInputStream = zipFileHandle.getInputStream( zipFile, zipEntryData.getPath() ); // throws IOException
        } catch ( Throwable th ) {
            // Need to close here, since the caller never receives a wrapped
            // input stream to close.
            zipFileHandle.close();
            throw th;
        }

        if ( baseInputStream == null ) {
        	throw new FileNotFoundException(
        		"Zip file [ " + zipFile.getName() + " ]" +
        		" failed to provide input stream for entry [ " + zipEntryData.getPath() + " ]");
        }

        InputStream inputStream = new InputStream() {
            private final InputStream wrappedInputStream = baseInputStream;

            // Object lifecycle ...

            @Override
            public synchronized void finalize() throws Throwable {
                close(); // throws IOException

                super.finalize(); // throws Throwable
            }

            // Close ...

            private volatile boolean isClosed;

            @Override
            public void close() throws IOException {
                if ( !isClosed ) {
                    synchronized( this ) {
                        if ( !isClosed ) {
                            try {
                                wrappedInputStream.close(); // throws IOException
                            } catch ( IOException e ) {
                                // FFDC
                            }
                            zipFileHandle.close();
                            isClosed = true;
                        }
                    }
                }
            }

            // Delegate methods ...

            @Trivial
            @Override
            public int read(byte[] b) throws IOException {
                return wrappedInputStream.read(b);
            }

            @Trivial
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return wrappedInputStream.read(b, off, len); // throws IOException
            }

            @Trivial
            @Override
            public long skip(long n) throws IOException {
                return wrappedInputStream.skip(n); // throws IOException
            }

            @Trivial
            @Override
            public int available() throws IOException {
                return wrappedInputStream.available(); // throws IOException
            }

            @SuppressWarnings("sync-override")
            @Trivial
            @Override
            public void mark(int readlimit) {
                wrappedInputStream.mark(readlimit);
            }

            @SuppressWarnings("sync-override")
            @Trivial
            @Override
            public void reset() throws IOException {
                wrappedInputStream.reset(); // throws IOException
            }

            @Trivial
            @Override
            public boolean markSupported() {
                return wrappedInputStream.markSupported();
            }

            @Override
            public int read() throws IOException {
                return wrappedInputStream.read(); // throws IOException
            }
        };

        return inputStream;
    }

    @Trivial
    @Override
    public long getSize() {
        if ( zipEntryData != null ) {
            return zipEntryData.getSize();
        } else {
            return 0L;
        }
    }

    @Trivial
    @Override
    public long getLastModified() {
        if ( zipEntryData != null ) {
            return zipEntryData.getTime();
        } else {
            return 0L;
        }
    }

    //

    private final String name;
    private final String a_path;

    @Trivial
    @Override
    public String getName() {
        return name;
    }

    @Trivial
    @Override
    public String getPath() {
        return a_path;
    }

    @Trivial
    public String getAbsolutePath() {
        return a_path;
    }

    @Trivial
    private String getRelativePath() {
        if (zipEntryData != null) {
            // If the zip entry data is available always use its source of truth
            // for the relative path within the zip file.
            String rPath = zipEntryData.r_getPath();
            if (zipEntryData.isDirectory()) {
                rPath += "/";
            }
            return rPath;
        }
        // Remove the leading '/' and assume this is a virtual directory with trailing '/'
        return a_path.substring(1) + "/";
    }

    /**
     * Answer null: Zip file entries live inside archives and have n
     * physical path themselves.
     *
     * TODO: This is not entirely true, as a zip file entry can be extracted
     * to a file in the cache directory.  Should this method not be aware of
     * that extraction, and make use of it?
     *
     * @return The physical path of this entry.  This implementation always
     *     answers null.
     */
    @Trivial
    @Override
    @Deprecated
    public String getPhysicalPath() {
        return null;
    }

    // Note on interpreting zip file entries as containers:
    //
    // Nested directory zip entries are only ever interpreted as zip file nested
    // directory containers.  Nested directory entries never use the delegating
    // zip file factory method of the root container.

    private volatile ZipFileNestedDirContainer localContainer;

    protected ZipFileNestedDirContainer convertToLocalContainer() {
        if ( (zipEntryData != null) && !zipEntryData.isDirectory() ) {
            return null;
        } else {
            if ( localContainer == null ) {
                synchronized ( this) { // Having a new object to guard 'localContainer' is too many objects.
                	if ( localContainer == null ) {
                		localContainer = new ZipFileNestedDirContainer(rootContainer, this, name, a_path);
                	}
                }
            }
            return localContainer;
        }
    }

    //

    private volatile ArtifactContainer enclosingContainer;

    /**
     * Answer the enclosing container of this entry.
     *
     * Obtaining the enclosing container is expensive.  The implementation
     * works very hard to avoid having to obtain the enclosing container.
     * First, the zip entry is created with the enclosing container when
     * the container is already available.  Second, the implementation only
     * asks for the enclosing container when a call is made to interpret
     * an entry as a non-local container.  That is, other than the case of
     * interpreting an entry as a nested directory container.
     *
     * @return The enclosing container of this entry.  Since this entry is
     *     a zip file type entry, the enclosing container is either a root
     *     zip type container or a nested directory zip type container.
     */
    @Override
    public ArtifactContainer getEnclosingContainer() {
        // The enclosing container may be set when the entry is
        // created, in which case the enclosing container lock is null
        // and is never needed.
        //
        // The entry can be created in these ways:
        //
        // ZipFileContainer.createEntry(ArtifactContainer, String, String, String, int, ZipEntry)
        // -- A caching factory method of zip file container entries.
        // -- Caches intermediate entries.  Non-container leaf entries are not cached.
        //
        // That is invoked in several ways:
        //
        // By:
        // ZipFileContainer.createEntry(String, String, String)
        // Which is invoked by:
        // ZipFileEntry.getEnclosingContainer()
        // -- Used when the enclosing container is not set when the entry was
        //    created.  This happens when the entry was created with a null
        //    enclosing container, which only happens when the entry is created
        //    from 'ZipFileContainer.getEntry'.
        // -- This is the core non-trivial step of resolving the enclosing container.
        // -- As a first step, if the parent is the root zip container, that is
        //    obtained as the enclosing container.
        // -- As a second step, the enclosing entry of this entry is obtained, then
        //    the enclosing container is obtained by interpreting that entry as a
        //    container.
        // -- The enclosing container must be obtained from the enclosing entry
        //    since those are cached and re-used, and since the reference to those
        //    keep a reference to their interpreted container.
        //
        // By zip container iterators:
        //
        // -- com.ibm.ws.artifact.zip.internal.ZipFileEntry.getEnclosingContainer()
        // ZipFileContainer.RootZipFileEntryIterator.next()
        // -- always provides the root zip container as the enclosing container
        // ZipFileNestedDirContainer.NestedZipFileEntryIterator.next()
        // -- always provides the nested zip container as the enclosing container
        //
        // ZipFileContainer.getEntry(String, boolean)
        // -- always provides null as the enclosing container
        //
        // As a public API, 'getEnclosingContainer' may be invoked externally.
        // Locally, 'getEnclosingContainer' is only invoked from:
        // ZipFileEntry.convertToContainer(boolean)
        // That is also a public API.
        // Locally, that is only invoked from:
        // ZipFileEntry.convertToContainer(boolean)
        // That is also a public API.

        if ( enclosingContainer == null ) {
            synchronized( this ) { // Having a new object to guard 'enclosingContainer' is too many objects.
                if ( enclosingContainer == null ) {
                    String a_enclosingPath = PathUtils.getParent(a_path);
                    int parentLen = a_enclosingPath.length();
                    if ( parentLen == 1 ) { // a_enclosingPath == "/"
                        enclosingContainer = rootContainer;
                    } else {
                        String r_enclosingPath = a_enclosingPath.substring(1);
                        int lastSlash = r_enclosingPath.lastIndexOf('/');
                        String enclosingName;
                        if ( lastSlash == -1 ) {
                            enclosingName = r_enclosingPath; // r_enclosingPath = "name"
                        } else {
                            enclosingName = r_enclosingPath.substring(lastSlash + 1); // r_enclosingPath = "parent/child/name"
                        }
                        ZipFileEntry entryInEnclosingContainer =
                            rootContainer.createEntry(enclosingName, a_enclosingPath, r_enclosingPath);
                        enclosingContainer = entryInEnclosingContainer.convertToLocalContainer();
                    }
                }
            }
        }
        return enclosingContainer;
    }

    @Override
    public ArtifactContainer convertToContainer() {
        return convertToContainer(LOCAL_AND_REMOTE);
    }

    public static final boolean LOCAL_ONLY = true;
    public static final boolean LOCAL_AND_REMOTE = false;

    private volatile Boolean conversion;

    @Override
    public ArtifactContainer convertToContainer(boolean localOnly) {
        ZipFileNestedDirContainer useLocalContainer = convertToLocalContainer();
        if ( (useLocalContainer != null) || localOnly ) {
            return useLocalContainer;
        }

        // Remember if a prior conversion failed.
        //
        // Full synchronization is not needed on 'conversion', since
        // each call is expected to obtain the same conversion
        // result.
        //
        // The entry *could* hold its conversion result.  That is not done
        // to minimize the entries which are retained.  That this is the
        // better policy is not clear, as container interpretation is
        // expensive.

        if ( (conversion != null) && !conversion.booleanValue() ) {
            return null;
        }

        // TODO: The calls to get the cache directory and to get the enclosing
        //       container should be done within the call to the delegating
        //       'getContainer': The cache directory and enclosing container
        //       are both expensive steps, and are often needed only if the
        //       delegation request succeeds.

        File newCacheDir = rootContainer.getCacheDir(this);
        ArtifactContainer container = getContainerFactoryHolder().getContainerFactory().getContainer(
                newCacheDir,
                getEnclosingContainer(), this,
                zipEntryData);

        if ( conversion == null ) {
            conversion = ( (container == null) ? Boolean.FALSE : Boolean.TRUE );
        }

        return container;
    }
}
