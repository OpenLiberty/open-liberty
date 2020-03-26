/*******************************************************************************
 * Copyright (c) 2011,2020 IBM Corporation and others.
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.artifact.zip.cache.ZipCachingProperties;
import com.ibm.ws.artifact.zip.cache.ZipFileHandle;
import com.ibm.ws.artifact.zip.internal.ZipFileContainerUtils.ZipEntryData;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.ArtifactNotifier;
import com.ibm.wsspi.kernel.service.utils.ParserUtils;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 * Root zip file type container; entries are zip file entries.
 *
 * Zip file entries are of three types:
 *
 * <ul><li>Zip file entries which have a non-directory type zip entry.</li>
 *     <li>Zip file entries which have a directory type zip entry.</li>
 *     <li>Zip file entries which do not have a zip entry.</li>
 * </ul>
 *
 * The second and third type of entry are directory type entries.  Both
 * always convert only to nested directory zip file containers.
 *
 * The first type of entry may convert to an archive through the artifact
 * file system.
 *
 * Several operations provide the ability to transition from zip file type
 * entities to other artifact file entities:
 *
 * ZipFileContainer.getEnclosingContainer
 * ZipFileContainer.getEntryInEnclosingContainer
 * -- These provide access to the exterior artifact file system.  Null
 *    will be returned if the zip container is a root-of-roots.  Generally,
 *    the enclosing container will be either an exposed file or will be a
 *    zip type container.
 *
 * ZipFileContainer.getEntry
 * -- This provides direct access to a nested entry of the zip container.
 *    Unlike access from directory type containers, access may skip intermediate
 *    containers.
 *
 * ZipFileContainer.iterator.next
 * -- Answers an immediate child of the container.  This is always a
 *    zip file type entry.
 *
 *  ZipFileEntry.getRoot
 *  -- Answers the root zip container of the entry.
 *  ZipFileEntry.getEntry
 *  -- This is a call-through to the root container, with the target path adjusted
 *     to be relative to the entry.  The entry is always a zip type entry.
 *  ZipFileEntry.getEnclosingContainer
 *  -- The container immediately enclosing this entry.  May be the root
 *     zip container or a nested zip container.
 *  ZipFileEntry.convertToLocalContainer
 *  -- This entry converted to a nested zip container.
 *  ZipFileEntry.convertToContainer
 *  -- Either, this entry converted to a nested zip container, or this
 *     entry converted through the artifact file system.  The result is
 *     either a nested zip type container, or an artifact container.
 *
 *  ZipFileNestedDirContainer.getRoot
 *  -- Answers the root zip file container of the nested container.
 *  ZipFileNestedDirContainer.getEntry
 *  -- This is a call-through to the root container, with the target path adjusted
 *     to be relative to the nested container.  The entry is always a zip type entry.
 *  ZipFileNestedDirContainer.getEntryInEnclosingContainer
 *  -- The entry which was interpreted to create this nested container.
 *  ZipFileNestedDirContainer.getEnclosingContainer
 *  -- The container holding the entry which was interpreted to create this
 *     nested container.  Will be either a root zip container or a nested zip
 *     container.
 *  ZipFileNestedDirContainer.iterator.next
 * -- Answers an immediate child of the container.  This is always a
 *    zip file type entry.
 *
 * ---
 *
 * Entries for zip file containers are accessed through two APIs: Directly,
 * through either {@link ZipFileContainer#getEntry(String)} or
 * {@link ZipFileNestedDirContainer#getEntry(String)}, or indirectly,
 * using {@link ZipFileContainer#iterator}) or
 * {@link ZipFileNestedDirContainer#iterator}).
 *
 * Both direct cases obtain entries which have an initially unassigned (null)
 * enclosing container.
 *
 * Both indirect cases have a an initially assigned (non-null) enclosing
 * container, which is provided from the iterator, which is always created
 * with an enclosing container reference.
 *
 * At the point of obtaining an entry, three cases are possible.  Which case
 * occurs depends on the features of the zip file entry which is reached by
 * path of the entry:
 *
 * In the most basic case, the path reachs a zip file entry, and that entry
 * is not a directory entry.  A new zip entry is created for the path.  (A new
 * entry is <strong>always</strong> created.)
 *
 * As a first nested container case, the path reaches a zip file entry, and that
 * entry is a directory entry.  A nested container entry is obtained for the path.
 * Nested container entries are retained by the root container, which means that
 * repeat calls on the same path will obtain the same nested container entry.
 *
 * As a second nested container case, the path does not reach a zip file entry.
 * A nested container entry is still obtained for the path, and as in the first
 * nested container case, the nested container entry is retained by the root
 * container and repeat calls on the same path will obtain the same nested container
 * entry.
 */
public class ZipFileContainer implements com.ibm.wsspi.artifact.ArtifactContainer {
    static final TraceComponent tc = Tr.register(ZipFileContainer.class);

    /**
     * Tuning parameter: Used to enable and disable the use
     * of a hash map as a faster way to locate paths.  Enabling
     * the map makes for faster lookups, but at a cost of allocating
     * the extra map structure.  Disabling the map makes for slower
     * lookups, but avoids allocating the extra map structure.
     * 
     * This tuning is based on profile data, which shows that a very
     * large (between 5% and 10%) of startup time can be spent in
     * {@link #locatePath(String)}.
     */
    public static final boolean USE_EXTRA_PATH_CACHE =
        ZipCachingProperties.ARTIFACT_ZIP_USE_EXTRA_PATH_CACHE;

    public static final boolean COLLECT_TIMINGS =
        ZipCachingProperties.ARTIFACT_ZIP_COLLECT_TIMINGS;

    @Trivial
    private static String toAbsMilliSec(long eventAt) {
        return ZipCachingProperties.toAbsMilliSec(eventAt);
    }

    @Trivial
    private static String dualTiming(long eventAt) {
        return ZipCachingProperties.dualTiming(eventAt);
    }

    private static enum Timings {
        ARRAY_CREATE_TIME,
        MAP_CREATE_TIME,
        ARRAY_LOOKUP_TIME,
        MAP_LOOKUP_TIME,
        EXTRACT_TIME;
    }

    private static final Integer timingsLock = ( COLLECT_TIMINGS ? new Integer(0) : null );

    private static int[] timingCounts = ( COLLECT_TIMINGS ? new int[ Timings.values().length ] : null );
    private static long[] timingTotals= ( COLLECT_TIMINGS ? new long[ Timings.values().length ] : null );

    static {
        if ( COLLECT_TIMINGS ) {
            System.out.println("ZFC Timings Key");
            System.out.println("ZFC [ " + Timings.ARRAY_CREATE_TIME + " ] [ Time creating the zip entry data array ]");
            System.out.println("ZFC [ " + Timings.MAP_CREATE_TIME + " ] [ Time creating zip entry data map ]");
            System.out.println("ZFC [ " + Timings.ARRAY_LOOKUP_TIME + " ] [ Time doing array lookups ]");
            System.out.println("ZFC [ " + Timings.MAP_LOOKUP_TIME + " ] [ Time doing map lookups ]");
            System.out.println("ZFC [ " + Timings.EXTRACT_TIME + " ] [ Time extracting nested containers ]");
        }
    }

    private static final int MAP_LOOKUP_FREQUENCY = 100;
    private static final int ARRAY_LOOKUP_FREQUENCY = 100;

    private static final int EXTRACT_FREQUENCY = 10;

    @Trivial
    private static void record(
        Timings timing, long startTimeNs, long endTimeNs,
        String data, int frequency) {

        int timingOrd = timing.ordinal();
        long durationNs = endTimeNs - startTimeNs;

        int newCount;
        long newTotal;

        synchronized(timingsLock) {
            newCount = timingCounts[timingOrd] += 1;
            newTotal = timingTotals[timingOrd] += durationNs;
        }

        if ( data != null ) {
            String startText = dualTiming(startTimeNs);
            String durationText = toAbsMilliSec(durationNs);
            System.out.println("ZFC [ " + data + " ] Timing [ " + timing + " ]" +
                               " Count [ " + newCount + " ]" +
                               " at [ " + startText + " ] Duration [ " + durationText + "ms ]");
        }

        if ( newCount % frequency == 0 ) {
            String totalText = toAbsMilliSec(newTotal);
            System.out.println("ZFC Timing [ " + timing + " ] Count [ " + newCount + " ]" +
                               " Total Duration [ " + totalText + "ms ]");
        }
    }

    private final String archiveName;

    // Multi-release support ...

    private static final int CURRENT_JAVA_VERSION = JavaInfo.majorVersion();

    // JDK-defined system property for controlling MR. Possible values: true (default), force, false

    public static final String JDK_DISABLE_MULTI_RELEASE_PROPERTY_NAME = "jdk.util.jar.enableMultiRelease";
    public static final String JDK_DISABLE_MULTI_RELEASE_DEFAULT_VALUE = "true";

    private static final boolean JDK_DISABLE_MULTI_RELEASE;

    static {
        SecureAction privGet = AccessController.doPrivileged( SecureAction.get() );

        String mrProp = privGet.getProperty(
            JDK_DISABLE_MULTI_RELEASE_PROPERTY_NAME,
            JDK_DISABLE_MULTI_RELEASE_DEFAULT_VALUE);

        JDK_DISABLE_MULTI_RELEASE = "false".equalsIgnoreCase(mrProp);
    }

    // Assign 'isMultiRelease' using double locking ...

    private final Integer multiReleaseLock = new Integer(0);
    private volatile Boolean isMultiRelease = null;

    /**
     * Create a root zip file type container which is not an enclosed container.
     * The file of the container must not be null.
     *
     * @param cacheDir The directory in which to extract nested entries.
     * @param archiveFile The ZIP file.
     * @param containerFactoryHolder Factory of this container.
     */
    @Trivial
    ZipFileContainer(File cacheDir, File archiveFile, ContainerFactoryHolder containerFactoryHolder) {
        String methodName = "<init>";

        this.containerFactoryHolder = containerFactoryHolder;

        // TODO: This should be assigned on demand.  Many zip file containers will never
        //       extract files, and assigning a cache directory to them when they are created
        //       is premature.

        this.cacheDir = cacheDir;
        this.nestedCacheDir = new File(cacheDir, ".cache");

        // Root-of-roots: The enclosing container and enclosing entry must be null.
        this.enclosingContainer = null;
        this.entryInEnclosingContainer = null;

        // Root-of-roots: 'archiveFile' must not be null.
        this.archiveFileLock = null;
        this.archiveFile = archiveFile;
        String useArchivePath = archiveFile.getAbsolutePath();
        this.archiveFileURI = getURI(archiveFile);
        this.archiveFilePath = useArchivePath;
        this.archiveName = archiveFile.getName();

        this.zipFileNotifier = new ZipFileArtifactNotifier(this, useArchivePath);

        this.nestedContainerEntries = new HashMap<String, ZipFileEntry>();

        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, methodName + " ZipContainer (root-of-root)");
            Tr.debug(tc, methodName + " Archive file [ " + archiveFilePath + " ]");
            Tr.debug(tc, methodName + " Cache [ " + cacheDir.getAbsolutePath() + " ]");
        }

        if ( COLLECT_TIMINGS ) {
            System.out.println("ZFC ZipContainer [ " + archiveFilePath + " ]");
        }
    }

    /**
     * Create a root ZIP file container which is an enclosed container.  The
     * container is the result of interpreting an entry of the enclosed container
     * as a ZIP file type container.  The archive file, if non-null, is the entry
     * of the enclosed container extracted to the cache directory.  The archive file
     * may be null, in which case the entry will be extracted when needed from
     * the enclosing container.
     *
     * @param cacheDir The directory in which to extract nested entries.
     * @param enclosingContainer The container enclosing this container.
     * @param entryInEnclosingContainer The entry which was interpreted
     *     as this container.
     * @param archiveFile The archive file.
     * @param containerFactoryHolder Factory of this container.
     */
    @Trivial
    ZipFileContainer(
        File cacheDir,
        ArtifactContainer enclosingContainer, ArtifactEntry entryInEnclosingContainer,
        File archiveFile,
        ContainerFactoryHolder containerFactoryHolder) {

        String methodName = "<init>";

        this.containerFactoryHolder = containerFactoryHolder;

        this.cacheDir = cacheDir;
        this.nestedCacheDir = new File( new File(cacheDir, ".cache"), entryInEnclosingContainer.getName());

        // Enclosed root: The enclosing container and enclosing entry must not be null.
        this.enclosingContainer = enclosingContainer;
        this.entryInEnclosingContainer = entryInEnclosingContainer;

        // Enclosed root: Null if nested; non-null if exposed on disk.
        // If nested, possibly but unlikely already expanded to disk.
        // (Pre-expansion to disk is not recommended.)
        this.archiveFile = archiveFile;
        if ( archiveFile != null ) {
            this.archiveFileLock = null;
            String useArchivePath =  archiveFile.getAbsolutePath();
            this.archiveFilePath = useArchivePath;
            this.archiveFileURI = getURI(archiveFile);
            this.zipFileNotifier = new ZipFileArtifactNotifier(this, useArchivePath);
            this.archiveName = archiveFile.getName();
        } else {
            this.archiveFileLock = new Integer(1);
            this.zipFileNotifier = new ZipFileArtifactNotifier(this, entryInEnclosingContainer);
            this.archiveName = entryInEnclosingContainer.getName();
        }

        this.nestedContainerEntries = new HashMap<String, ZipFileEntry>();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, methodName + " ZipContainer (enclosed root)");
            Tr.debug(tc, methodName + " Enclosing container [ " + enclosingContainer.getPath() + " ]");
            Tr.debug(tc, methodName + " Enclosing entry [ " + entryInEnclosingContainer.getName() + " ]");
            if ( archiveFile != null ) {
                Tr.debug(tc, methodName + " Archive file [ " + archiveFilePath + " ]");
            }
        }

        if ( COLLECT_TIMINGS ) {
            System.out.println("ZFC ZipContainer [ " + enclosingContainer.getPath() + " : " + entryInEnclosingContainer.getName() + " ]");
        }
    }

    /**
     * Subclass API: Finalize this zip file container.
     *
     * Extend to reset all fast mode requests.
     *
     * @throws Throwable Thrown by the delegate call.
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            resetFastMode();
        } finally {
            super.finalize(); // throws Throwable
        }
    }

    //

    @Trivial
    @Override
    public boolean isRoot() {
        return true;
    }

    @Trivial
    @Override
    public String getName() {
        return "/";
    }

    @Trivial
    @Override
    public String getPath() {
        return "/";
    }

    @Trivial
    public String getAbsolutePath() {
        return "/";
    }

    @Trivial
    public String getRelativePath() {
        return "";
    }

    @Trivial
    @Override
    public ZipFileContainer getRoot() {
        return this;
    }

    //

    private final ContainerFactoryHolder containerFactoryHolder;

    @Trivial
    public ContainerFactoryHolder getContainerFactoryHolder() {
        return containerFactoryHolder;
    }

    /**
     * The protocol to be used in URIs which reach entries in this
     * container.
     *
     * Usually, "wsjar" is used.  Optionally, "jar" may be used.  See
     * {@link ContainerFactoryHolder#useJarUrls}.
     * 
     * The protocol value is not cached: Zip file containers must respond
     * to dynamic chances to the class loading configuration, which
     * is managed by the container factory holder.</p>
     *
     * @return The protocol to be used in URIs which reach entries in
     *     this container.
     */
    @Trivial
    private String getProtocol() {
        return ( containerFactoryHolder.useJarUrls() ? "jar" : "wsjar" );
    }

    /**
     * Answer a zip file handle from the caching service.  The service may
     * create and open a new zip file handle, or may return a previously
     * opened handle.
     *
     * The zip file handle manages concurrent access to the same zip file,
     * keeping the zip file in memory until the last active open is closed.
     *
     * @param path The path to the zip file.  Callers should agree to use the
     *     same path format to ensure multiple callers who are accessing the
     *     same zip file obtain the same handle.  Current implementations
     *     use the canonical path to the zip file, with a fall back to the
     *     absolute path of the zip file when retrieval of the canonical path
     *     fails.
     *
     * @return A zip file handle for the specified path.
     *
     * @throws IOException If the attempt to open the zip file handle failed.
     */
    private ZipFileHandle getZipFileHandle(String path) throws IOException {
        return getContainerFactoryHolder().getZipCachingService().openZipFile(path);
        // 'openZipFile' throws IOException
    }

    // Every root container is given a cache directory when created.
    // The cache directory stores entries extracted from the root container.
    //
    // zip container uses this cache dir in this manner:
    //  cachedir/zipfilecontainer.jar        - file, used to hold the extracted bytes that represent the jar
    //                                         present when the zipfilecontainer would otherwise be a nested jar datastream
    //  cachedir/.cache                      - directory, shared by all containers with the same enclosing container as this one.
    //  cachedir/.cache/zipfilecontainer.jar - directory, used host the cache directories for nested archives within zipfilecontainer
    //                                         every nested archive within zipfilecontainer will have its own cache directory, present
    //                                         at a location equivalent to it's path within the zipfilecontainer
    //
    // this zip container has a parent, and thus may have other zip containers to share the cachedir with.
    // we use our zip containers entry name in the parent as a subdir under the cachedir to ensure we are good.

    private final File cacheDir;
    private final File nestedCacheDir; // Cache this: It is needed for every descendant entry.  These can be numerous.

    @Trivial
    File getCacheDir() {
        return cacheDir;
    }

    @Trivial
    public File getNestedCacheDir() {
        return nestedCacheDir;
    }

    @Trivial
    protected File extract(ZipFileEntry entry) throws IOException {
        return extractEntry( entry, getCacheDir() ); // throws IOException
    }

    protected File getCacheDir(ZipFileEntry entry) {
        File useNestedCacheDir = getNestedCacheDir();

        String r_entryPath = entry.getRelativePath();
        int slashLoc = r_entryPath.lastIndexOf('/');
        if ( slashLoc != -1 ) { // Not immediately beneath the root.
            // Since the entry path is relative, 'slashLoc' cannot be 0.
            String r_entryParentPath = r_entryPath.substring(0,  slashLoc);
            useNestedCacheDir = new File(useNestedCacheDir, r_entryParentPath);
        }

        return useNestedCacheDir;
    }

    // Non-local containment:
    //
    // A zip file container is always a root container.
    //
    // A zip file container may be a root-of-roots container, in which case the
    // enclosing container and the entry in the enclosing container are both null.
    //
    // A zip file container may be an enclosed container, which means that the container
    // was created by interpreting an entry.  Most commonly, the entry will be a file on
    // disk or an entry in an enclosing zip file.  When a zip file container is an enclosed
    // container the enclosing container and the entry in the enclosing container are
    // both non-null.
    //
    // The types of the enclosing container the interpreted entry in the enclosing
    // container are basic types of the artifact file system: {@link ArtifactContainer} and
    // {@link ArtifactEntry}.  The enclosing types are independent of the zip file artifact
    // container type.

    private final ArtifactContainer enclosingContainer;
    private final ArtifactEntry entryInEnclosingContainer;

    @Trivial
    @Override
    public ArtifactContainer getEnclosingContainer() {
        return enclosingContainer;
    }

    @Trivial
    @Override
    public ArtifactEntry getEntryInEnclosingContainer() {
        return entryInEnclosingContainer;
    }

    // The zip file as an archive file.
    //
    // The zip file is assigned in three different modes:
    //
    // First, a zip file container which is a root-of-roots container
    // must be given a zip file.  A root-of-roots zip file container has
    // only the zip file as a means of accessing its contents.
    //
    // Second, a zip file container may be created as an interpreted
    // container, in which case the zip file may be available when the
    // zip file container was constructed, or may be obtained by extracting
    // the enclosing entry to disk.
    //
    // Extraction is performed on demand, and can fail, for example, because
    // of a corruption of the enclosing entry, because cache storage is full,
    // because the extraction location is too deeply nested, because of a
    // permissions problem with the extraction location, or because of a problem
    // using the container name as a file name.  In all of these cases, the
    // failure is handled by issuing an error, then processing with the archive
    // being empty.

    // Null if the container started with an archive file.
    private final Integer archiveFileLock;

    private boolean archiveFileFailed;
    private File archiveFile;
    // Get this once: the usual java file implementation does not
    // cache file absolute paths.
    private String archiveFilePath;
    private URI archiveFileURI;

    /**
     * Answer the archive file.  Extract it if necessary.  Answer null
     * if extraction fails.
     *
     * If this container was created as a root-of-roots container, the archive
     * will already be set.
     *
     * Otherwise, the container is a nested root container, and the archive may
     * not have not yet been set.  In that case, extract the archive, then answer
     * the extracted file as the archive file.
     *
     * Extraction may fail, in which case null will be returned.
     *
     * Extraction is only attempted once: Access to the archive file after a failure
     * immediately answer null.
     *
     * @return The archive file.  Extracted if necessary.  Null if extraction failed.
     */
    @Trivial
    private File getArchiveFile() {
        String methodName = "getArchiveFile";

        if ( archiveFileLock != null ) {
            synchronized ( archiveFileLock ) {
                if ( (archiveFile == null) && !archiveFileFailed ) {
                    try {
                        archiveFile = extractEntry( entryInEnclosingContainer, getCacheDir() );
                        // 'extractEntry' throws IOException

                        if ( archiveFile != null ) {
                            archiveFilePath = archiveFile.getAbsolutePath();
                            archiveFileURI = getURI(archiveFile);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                                Tr.debug(tc, methodName + " Archive file [ " + archiveFilePath + " ]");
                            }
                        } else {
                            archiveFileFailed = true;
                            Tr.error(tc, "extract.cache.null", entryInEnclosingContainer.getPath());
                        }

                    } catch ( IOException e ) {
                        archiveFileFailed = true;
                        Tr.error(tc, "extract.cache.fail", e.getMessage());
                    }
                }
            }
        }

        return archiveFile;
    }

    /**
     * Answer the absolute path to the archive file.  Do an extraction if this
     * is a nested archive and the file is not yet extracted.  Answer null
     * if extraction fails.
     *
     * @return The absolute path to the archive file.
     */
    private String getArchiveFilePath() {
        if ( archiveFileLock == null ) {
            return archiveFilePath;
        } else {
            synchronized( archiveFileLock ) {
                @SuppressWarnings("unused")
                File useArchiveFile = getArchiveFile();
                return archiveFilePath;
            }
        }
    }

    /**
     * Subclass API: Answer the physical path to the on-disk storage of this
     * container.
     *
     * Zip file type contains map to archive files on disk, potentially which
     * initially were nested archives and which were extracted to the cache
     * directory.
     *
     * Answer the absolute path of the mapped archive.  If the archive is a nested
     * archive and not yet extracted, extract it.
     *
     * Answer null if extraction fails.
     *
     * @return The physical path of this container, obtained as the absolute
     *     path of the mapped archive of this container.
     */
    @Override
    @Trivial
    @Deprecated
    public String getPhysicalPath() {
        return getArchiveFilePath();
    }

    //

    private final Integer zipFileHandleLock = new Integer(2);
    private boolean zipFileHandleFailed;
    private ZipFileHandle zipFileHandle;

    /**
     * Answer the handle to the archive file of this container.
     *
     * @return The zip file handle of this container.
     *
     * @throws IOException Thrown if the zip file handle could not be
     *     obtained.
     *
     * TODO: That the zip file container retains a reference to its
     *       handle is a problem, as the zip file handle cache has
     *       a maximum size.  Zip file container might hold stale
     *       handles.
     *
     *       This is not an immediate problem: Both APIs which use
     *       the zip file handle ({@link #openZipFileHandle()} and
     *       {@link ZipFileEntry#getInputStream()} immediately
     *       open the zip file handle.
     *
     *       The problem occurs after the zip file handle is closed
     *       (either {@link #closeZipFileHandle()} or when the
     *       entry input stream is closed.  After the close, the
     *       zip file handle is free to be removed by the zip file
     *       handle cache.  However, the zip file container does
     *       not clear and re-acquire its zip file handle.
     *
     *       The consequence is according to whether the zip file reaper
     *       is active.  When the zip file reaper is inactive, each
     *       zip file handle obtains its own zip file.  Continued use
     *       of a stale zip file handle will result in multiple opens
     *       of the same zip file.  That is inefficient, but no failures
     *       will occur.
     *
     *       That multiple zip file handles might be created is possible
     *       because zip file containers are not guaranteed to be unique.
     *       Non-unique but equivalent zip file containers will result
     *       because entries are not unique: Each call to obtain a non-
     *       nested container entry from an artifact container obtains a
     *       new entry instance.  Each of the non-unique but equivalent
     *       entries will obtain a different container.
     *
     *       When the zip file reaper is active, the problem of multiple
     *       non-unique, equivalent zip files cannot occur.  The problem
     *       is avoided because, first, a zip file handle cannot perform
     *       a close on a zip file through the zip file reaper without
     *       first having opened the zip file handle, and second, because
     *       a zip file handle cannot perform more closes than it has
     *       performed opens.
     */
    ZipFileHandle getZipFileHandle() throws IOException {
        synchronized( zipFileHandleLock ) {
            if ( zipFileHandleFailed ) {
                return null;
            } else if ( zipFileHandle != null ) {
                return zipFileHandle;
            }

            File useArchiveFile = getArchiveFile();
            if ( useArchiveFile == null ) {
                zipFileHandleFailed = true;
                throw new FileNotFoundException( entryInEnclosingContainer.getPath() );
            }

            try {
                String useCanonicalPath = getCanonicalPath(useArchiveFile); // throws IOException
                zipFileHandle = getZipFileHandle(useCanonicalPath); // throws IOException
            } catch ( IOException e ) {
                zipFileHandleFailed = true;
                throw e;
            }
            return zipFileHandle;
        }
    }

    private ZipFile openZipFileHandle() {
        ZipFileHandle useZipFileHandle;
        try {
            useZipFileHandle = getZipFileHandle();
        } catch ( IOException e ) {
            useZipFileHandle = null;
            Tr.error(tc, "Failed to open", getArchiveFilePath());
        }
        if ( useZipFileHandle == null ) {
            return null;
        }

        try {
            return useZipFileHandle.open();
        } catch ( IOException e ) {
            // FFDC
            Tr.error(tc, "Failed to open", getArchiveFilePath());
            return null;
        }
    }

    private void closeZipFileHandle() {
        // TODO: The zip file handle should never be null when
        //       a close request is made.

        synchronized(zipFileHandleLock) {
            if ( zipFileHandle != null ) {
                zipFileHandle.close();
            }
        }
    }

    //

    private final Integer fastModeLock = new Integer(3);
    private int fastModeCount;

    @Override
    public void useFastMode() {
        synchronized ( fastModeLock ) {
            fastModeCount++;
            @SuppressWarnings("unused")
            ZipFile useZipFile = openZipFileHandle();
        }
    }

    @Override
    public void stopUsingFastMode() {
        synchronized ( fastModeLock ) {
            // TODO: The fast mode count should never be zero when
            //       a request is made to stop using fast mode.
            if ( fastModeCount <= 0 ) {
                return;
            }
            fastModeCount--;
            closeZipFileHandle();
        }
    }

    protected void resetFastMode() {
        synchronized ( fastModeLock ) {
            while ( fastModeCount > 0 ) {
                fastModeCount--;
                closeZipFileHandle();
            }
        }
    }

    //

    private final Integer zipEntryDataLock = new Integer(4);
    private volatile ZipEntryData[] zipEntryData;
    private Map<String, ZipEntryData> zipEntryDataMap;

    private String getTimingName() {
        if ( archiveFileLock == null ) {
            return archiveFilePath;
        } else {
            return enclosingContainer.getPath() + " : " + entryInEnclosingContainer.getName();
        }
    }

    @Trivial
    private void setZipEntryData() {
        if ( zipEntryData == null ) {
            synchronized( zipEntryDataLock ) {
                if ( zipEntryData == null ) {
                    String timingName = (COLLECT_TIMINGS ? getTimingName() : null);

                    long dataStart = (COLLECT_TIMINGS ? System.nanoTime() : -1L);
                    ZipEntryData[] useZipEntryData = createZipEntryData();
                    if ( COLLECT_TIMINGS ) {
                        record(Timings.ARRAY_CREATE_TIME, dataStart, System.nanoTime(), timingName, 1);
                    }

                    if ( USE_EXTRA_PATH_CACHE ) {
                        long mapStart = (COLLECT_TIMINGS ? System.nanoTime() : -1L);
                        zipEntryDataMap = ZipFileContainerUtils.setLocations(useZipEntryData);
                        if ( COLLECT_TIMINGS ) {
                            record(Timings.MAP_CREATE_TIME, mapStart, System.nanoTime(), timingName, 1);
                        }
                    }

                    zipEntryData = useZipEntryData; 
                }
            }
        }
    }

    // Used externally by:
    //   com.ibm.ws.artifact.zip.internal.ZipFileArtifactNotifier.collectRegisteredPaths(String, List<String>)
    //   com.ibm.ws.artifact.zip.internal.ZipFileArtifactNotifier.loadZipEntries()
    //   com.ibm.ws.artifact.zip.internal.ZipFileNestedDirContainer.iterator()
    // And several times from within ZipFileContainer itself.
    @Trivial
    protected ZipEntryData[] getZipEntryData() {
        setZipEntryData();
        return zipEntryData;
    }

    /**
     * Fast entry lookup.  For use only when {@link #USE_EXTRA_PATH_CACHE} is true.
     * 
     * @param r_path The path to lookup.
     * 
     * @return The data stored under the path.
     */
    @Trivial
    private ZipEntryData fastGetZipEntryData(String r_path) {
        setZipEntryData();
        return zipEntryDataMap.get(r_path);
    }

    // 'locatePath' is modal, depending on whether 'isMultiRelease' has
    // been initialized.
    //
    // Initialization of 'isMultiRelease' occurs in:
    //   ZipFileContainer.getEntry(String, boolean)
    //
    // Calls to 'locatePath' occur from:
    //   ZipFileArtifactNotifier.collectRegisteredPaths(String, List<String>)
    //   ZipFileContainer.createEntry(String, String)
    //   ZipFileContainer.getEntry(String, boolean)
    //
    // Calls to 'collectRegisteredPaths' occur only from:
    //   ZipFileArtifactNotifier.notifyAllListeners(boolean, String)
    //
    // It is not clear whether calls to 'notifyAllListeners only occur
    // after 'isMultiRelease' has been initialized.
    //
    // Calls to 'createEntry' occur from 'createEntry' and 'getEntry'.
    // That collapses to the call from 'getEntry'.  The call from
    // 'getEntry' is almost always preceeded with a call to initialize
    // 'isMultiRelease'.  The exception is when the target location
    // is beneath 'META-INF', which is never subject to MR lookups.
    // Net, the call from 'getEntry' is safe.
    //
    // However, there is also a call from:
    //     ZipFileContainerUtils.ZipFileEntryIterator.next()
    // And that call does not force initialization 'isMultiRelease'.

    @Trivial
    public int locatePath(String r_path) {
        // If this is an MRJAR, check under /META-INF/versions/n/<r_path>
        if ( (isMultiRelease != null) && isMultiRelease.booleanValue() ) {
            int mrLocation = locateMultiReleasePath(r_path);
            if ( mrLocation >= 0 ) {
                return mrLocation;
            }
        }

        // The direct lookup is not an 'else' case: The multi-release
        // location is an alternate to the direct location.  If the
        // multi-release location is not present, fall back to the direct
        // location.

        return locateDirectPath(r_path);
    }

    @Trivial
    private int locateDirectPath(String r_path) { 
        if ( USE_EXTRA_PATH_CACHE ) {
            long mapLookupStart = (COLLECT_TIMINGS ? System.nanoTime() : -1L);
            ZipEntryData entryData = fastGetZipEntryData(r_path);
            if ( COLLECT_TIMINGS ) {
                record(Timings.MAP_LOOKUP_TIME, mapLookupStart, System.nanoTime(), null, MAP_LOOKUP_FREQUENCY);
            }
            if ( entryData != null ) {
                return entryData.getOffset();
            }
        }

        // Have to fall back to the locate call: The path might be of a virtual
        // directory entry which is the parent of an entry.
        long lookupStart = (COLLECT_TIMINGS ? System.nanoTime() : -1L);        
        int location = ZipFileContainerUtils.locatePath( getZipEntryData(), r_path );
        if ( COLLECT_TIMINGS ) {
            record(Timings.ARRAY_LOOKUP_TIME, lookupStart, System.nanoTime(), null, ARRAY_LOOKUP_FREQUENCY);
        }
        return location;
    }

    @Trivial
    private int locateMultiReleasePath(String path) {
        // Multi-release files may be at /com/foo/A.class and /META-INF/versions/9/com/foo/A.class
        // Since we commonly blindly try-load META-INF and java.* entries, optimize those out
        if ( (path == null) || path.startsWith("META-INF") || path.startsWith("java/") ) {
            return -1;
        }

        for ( int currentVersion = CURRENT_JAVA_VERSION; currentVersion >= 9; currentVersion-- ) {
            int versionPath = locateDirectPath("META-INF/versions/" + currentVersion + "/" + path);
            if ( versionPath >= 0 ) {
                if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    Tr.debug(tc, "Found MR path: META-INF/versions/" + currentVersion + "/" + path);
                }
                return versionPath;
            }
        } 
        return -1;
    }

    @Trivial
    public boolean isChildOf(String parentPath, String childPath) {
        // Test for childPath = parentPath + "/" + childSubPath
        int parentPathLen = parentPath.length();
        int childPathLen = childPath.length();
        if ( childPathLen <= parentPathLen ) {
            return false;
        } else if ( childPath.charAt(parentPathLen) != '/' ) {
            return false;
        } else if ( !childPath.startsWith(parentPath) ) {
            return false;
        } else {
            return true;
        }
    }

    @Trivial
    private ZipEntryData[] createZipEntryData() {
        ZipFile useZipFile = openZipFileHandle();
        if ( useZipFile == null ) {
            return new ZipEntryData[0];
        }

        try {
            return ZipFileContainerUtils.collectZipEntries(useZipFile);
        } finally {
            closeZipFileHandle(); // throws IOException
        }
    }

    private boolean initializeMultiRelease() {
        if ( isMultiRelease != null ) {
            return isMultiRelease;
        }

        // Multi-Release jars only apply to JDK 9+
        if ( CURRENT_JAVA_VERSION < 9 ) {
            isMultiRelease = Boolean.FALSE;
            return isMultiRelease;
        }

        // Multi-Release jars can be disabled with JDK system properties
        if ( JDK_DISABLE_MULTI_RELEASE ) {
            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                Tr.debug(tc, "JDK system property set to globally disable multi-release jars");
            }
           isMultiRelease = Boolean.FALSE;
           return isMultiRelease;
        }

        // Only .jar files can be multi-release (not .war or .ear)
        if ( !archiveName.endsWith(".jar") ) {
            isMultiRelease = Boolean.FALSE;
            return isMultiRelease;
        }

        // Only synchronize the expensive check against the manifest file.
        //
        // The earlier checks are simple and quick and always produce the same
        // assignment.  There is no harm if they are performed multiple times.
        //
        // On the other hand, the manifest check is quite expensive, and must
        // not be performed more than once.

        synchronized ( multiReleaseLock ) {
            if ( isMultiRelease != null ) { // Use double-locking.
                return isMultiRelease;
            }

            // Multi-release is disabled for this JAR unless
            // a main attribute 'Multi-Release' is present and is "true".

            ZipFileEntry mfEntry = getEntry("META-INF/MANIFEST.MF");
            if ( mfEntry == null ) {
                if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    Tr.debug(tc, "No MANIFEST.MF found in container. Assuming not multi-release");
                }
                isMultiRelease = Boolean.FALSE;
                return isMultiRelease;

            } else {
                Boolean isMR;

                try ( InputStream mfStream = mfEntry.getInputStream() ) {
                    Manifest mf = new Manifest(mfStream);
                    String isMultiReleaseAttr = mf.getMainAttributes().getValue("Multi-Release");
                    if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                        Tr.debug(tc, "Raw value for 'Multi-Release' attribute: " + isMultiReleaseAttr);
                    }
                    if ( isMultiReleaseAttr != null ) {
                        isMR = Boolean.valueOf( isMultiReleaseAttr.trim() );
                    } else {
                        isMR = Boolean.FALSE;
                    }
                } catch ( IOException e ) {
                    // FFDC
                    isMR = Boolean.FALSE;
                }

                isMultiRelease = isMR;
            }

            return isMultiRelease;
        }
    }

    private final Integer iteratorDataLock = new Integer(5);

    private volatile Map<String, ZipFileContainerUtils.IteratorData> iteratorData;

    @Trivial
    protected Map<String, ZipFileContainerUtils.IteratorData> getIteratorData() {
        String methodName = "getIteratorData";
        if ( iteratorData == null ) {
            synchronized(iteratorDataLock) {
                if ( iteratorData == null ) {
                    ZipEntryData[] useZipEntryData = getZipEntryData();
                    if ( useZipEntryData.length == 0 ) {
                        iteratorData = Collections.emptyMap();
                    } else {
                        iteratorData = ZipFileContainerUtils.collectIteratorData(useZipEntryData);
                    }
                    Tr.debug(tc, methodName + " [ " + Integer.valueOf(useZipEntryData.length) + " ] entries");
                }
            }
        }
        return iteratorData;
    }

    //

    private final Integer nestedContainerEntriesLock = new Integer(6);
    private Map<String, ZipFileEntry> nestedContainerEntries;

    @Trivial
    protected ZipFileEntry createEntry(
        ArtifactContainer nestedContainer,
        String entryName, String a_entryPath,
        ZipEntryData useZipEntryData) {

        if ( (useZipEntryData != null) && !useZipEntryData.isDirectory() ) {
            return new ZipFileEntry(
                this, nestedContainer,
                useZipEntryData,
                entryName, a_entryPath);

        } else {
            String r_entryPath = a_entryPath.substring(1);
            synchronized ( nestedContainerEntriesLock ) {
                ZipFileEntry nestedContainerEntry = nestedContainerEntries.get(r_entryPath);
                if ( nestedContainerEntry == null ) {
                    nestedContainerEntry = new ZipFileEntry(
                        this, nestedContainer,
                        useZipEntryData,
                        entryName, a_entryPath);                   
                    nestedContainerEntries.put(r_entryPath,  nestedContainerEntry);
                }
                return nestedContainerEntry;
            }
        }
    }

    /**
     * Answer the zip entry for the zip file entry at the specified path.
     * The zip file entry may be virtual.
     *
     * @param entryName The simple name of the entry.
     * @param a_entryPath The absolute path of the entry.
     *
     * @return The zip entry for the zip file entry at the specified path.
     */
    @Trivial
    protected ZipFileEntry createEntry(String entryName, String a_entryPath) {
        ZipEntryData[] useZipEntries = getZipEntryData();
        if ( useZipEntries.length == 0 ) {
            return null;
        }

        String r_entryPath = a_entryPath.substring(1);
        int location = locatePath(r_entryPath);

        ZipEntryData entryData;
        if ( location < 0 ) {
            location  = ( (location + 1) * -1 );
            entryData = null;
        } else {
            entryData = useZipEntries[location];
        }

        return createEntry(
            null,
            entryName, a_entryPath,
            entryData);
    }

    //

    @Trivial
    @Override
    public Iterator<ArtifactEntry> iterator() {
        ZipEntryData[] useEntryData = getZipEntryData();
        if ( useEntryData.length == 0 ) {
            return Collections.emptyIterator();
        }

        Map<String, ZipFileContainerUtils.IteratorData> allIteratorData = getIteratorData();
        ZipFileContainerUtils.IteratorData thisIteratorData = allIteratorData.get("");

        return new ZipFileContainerUtils.ZipFileEntryIterator(this, this, useEntryData, thisIteratorData);
    }

    @Trivial
    @Override
    public ZipFileEntry getEntry(String path) {
        return getEntry(path, IS_NOT_NORMALIZED);
    }

    public static final boolean IS_NORMALIZED = true;
    public static final boolean IS_NOT_NORMALIZED = false;

    public ZipFileEntry getEntry(String entryPath, boolean normalized) {
        ZipEntryData[] useEntryData = getZipEntryData();
        if ( useEntryData.length == 0 ) {
            return null; // Prior failure to list entries.
        }

        // Try once with the initial path ...

        int pathLen = entryPath.length();
        if ( (pathLen == 0) || ((pathLen == 1) && (entryPath.charAt(0) == '/')) ) {
            return null; // "" and "/" reach the root; the root container has no entry.
        }

        // Not empty, and not just a single slash; removing a trailing slash is safe.
        if ( entryPath.charAt(pathLen - 1) == '/' ) {
            entryPath = entryPath.substring(0, pathLen - 1);
            pathLen = pathLen - 1;
        }

        String a_entryPath;
        String r_entryPath;
        if ( entryPath.charAt(0) == '/' ) {
            a_entryPath = entryPath;
            r_entryPath = entryPath.substring(1); // The entries table uses relative paths.
        } else {
            a_entryPath = null; // Maybe won't need it.
            r_entryPath = entryPath; // The path is already relative.
        }

        // 'getEntry' is invoked from 'initializeMultiRelease'!
        // The guard on the path is absolutely necessary to avoid
        // infinite recursion.

        if ( (isMultiRelease == null) && !entryPath.startsWith("META-INF") ) {
            initializeMultiRelease();
        }

        int location = locatePath(r_entryPath);

        if ( (location < 0) && !normalized ) {
            // Try again after forcing the path to be normalized.

            entryPath = PathUtils.normalizeUnixStylePath(entryPath);
            pathLen = entryPath.length();

            if ( (pathLen == 0) || (pathLen == 1) && (entryPath.charAt(0) == '/') ) {
                return null; // "" and "/" reach the root; the root container has no entry.
            }

            if ( !PathUtils.isNormalizedPathAbsolute(entryPath) ) {
                return null; // Leading ".." or "/.."; the path reaches outside of this container.
            }

            if ( entryPath.charAt(0) == '/' ) {
                a_entryPath = entryPath;
                r_entryPath = entryPath.substring(1); // The entries table uses relative paths.
            } else {
                a_entryPath = null; // Maybe won't need it.
                r_entryPath = entryPath; // The path is already relative.
            }

            location = locatePath(r_entryPath);
        }

        ZipEntryData useZipEntryData;
        if ( location < 0 ) {
            location = ( (location + 1) * -1 );
            // An in-exact match ...
            if ( location == useEntryData.length ) {
                return null; // There is no next entry; cannot match even partially.
            } else {
                ZipEntryData nextEntryData = useEntryData[location];
                if ( !isChildOf(r_entryPath, nextEntryData.r_getPath() ) ) {
                     // There is a next entry, but it is not a child of the target.
                    return null;
                } else {
                    // There is a next entry and it is a child of the target.
                    // Create an implied entry.
                    useZipEntryData = null;
                }
            }
        } else {
            useZipEntryData = useEntryData[location];
        }

        String entryName = PathUtils.getName(entryPath);

        // For this create, the nested container which is the immediate parent
        // of the new entry is not known.  The call to 'getEntry' is allowed to
        // directly jump to any entry of the zip file.

        if ( a_entryPath == null ) {
            a_entryPath = '/' + entryPath; // Can't put this off any longer.
        }

        // The null first parameter means that the enclosing container of the
        // entry is not known, and will need to be created if requested.

        return createEntry(
            null,
            entryName, a_entryPath,
            useZipEntryData);
    }

    @Override
    public Collection<URL> getURLs() {
        File useArchiveFile = getArchiveFile();
        if ( useArchiveFile == null ) {
            return Collections.emptySet();
        }

        try {
            return Collections.singleton( archiveFileURI.toURL() );
        } catch ( MalformedURLException e ) {
            // FFDC
            return Collections.emptySet();
        }
    }

    @Trivial
    protected URI createEntryUri(String r_entryPath) {
        File useArchiveFile = getArchiveFile();
        if ( useArchiveFile == null ) {
            return null;
        }
        return createEntryUri(r_entryPath, archiveFileURI);
    }

    /**
     * Create and return a URI for an entry of an archive.
     *
     * Usually, "wsjar" is used as the protocol:
     *
     * <code>wsjar:&lt;archiveUri&gt;!/&lt;entryPath&gt;</code>
     *
     * Optionally, the protocol can be changed to "jar":
     *
     * <code>jar:&lt;archiveUri&gt;!/&lt;entryPath&gt;</code>
     *
     * @param absPath The path to the entry.
     * @param useArchiveFile The archive file containing the entry.
     *
     * @return The URI of the entry in the archive file.
     *
     * TODO: Has default protection, but should not.  Having default
     *       protection allows it to be used from
     *       "com.ibm.ws.artifact.zip.internal.ZipFileContainerTest".
     */
    URI createEntryUri(String r_entryPath, URI archiveUri) {
        if ( archiveUri == null ) {
            return null;
        }

        if ( r_entryPath.isEmpty() ) {
            return null;
        }

        // URLs for jar/zip data now use wsjar to avoid locking issues via jar protocol.
        //
        // The single string constructor is used to control the encoding an ddecoding.
        //
        // The return value of 'parentUri.toString()' is an encoded string.  The
        // handler (usually WSJarURLStreamHandler) must decode that string.
        //
        // See: http://stackoverflow.com/questions/9419658/normalising-possibly-encoded-uri-strings-in-java.

        String encodedUriText =
            getProtocol() + ":" +
            archiveUri.toString() + "!/" +
            ParserUtils.encode(r_entryPath);

        try {
            return new URI(encodedUriText);
        } catch ( URISyntaxException e ) {
            // FFDC
            return null;
        }
    }

    // TODO: Move these to utilities

    private final ZipFileArtifactNotifier zipFileNotifier;

    @Override
    public ArtifactNotifier getArtifactNotifier() {
        return zipFileNotifier;
    }

    // File utility ...

    @Trivial
    private static URI getURI(final File file) {
        return AccessController.doPrivileged( new PrivilegedAction<URI>() {
            @Override
            public URI run() {
                return file.toURI();
            }
        } );
    }

    // TODO: Move this to utilities.

    @Trivial
    @FFDCIgnore(PrivilegedActionException.class)
    private static String getCanonicalPath(final File file) throws IOException{
        if ( System.getSecurityManager() == null ) {
            return file.getCanonicalPath(); // throws IOException

        } else {
            try {
                return AccessController.doPrivileged( new PrivilegedExceptionAction<String>() {
                    @Override
                    public String run() throws IOException {
                        return file.getCanonicalPath(); // throws IOException
                    }
                } );
            } catch ( PrivilegedActionException e ) {
                throw (IOException) e.getException();
            }
        }
    }

    // Extraction utility ...

    private static final Integer extractionsLock = new Integer(7);

    private static final Map<String, CountDownLatch> extractionLocks =
        new HashMap<String, CountDownLatch>();

    /**
     * Make sure a completion latch exists for a specified path.
     *
     * Create and store one if necessary.
     *
     * Answer the completion latch encapsulated in an extraction latch,
     * which brings together the completion latch with the path and
     * with a setting of whether the extraction is a primary (doing
     * the extraction) or secondary (waiting for the primary to do the
     * extraction).
     *
     * @param path The path which is being extracted.
     *
     * @return An extraction latch for the path which encapsulates
     *     the extraction state (primary or secondary), the path,
     *     and the completion latch for the extraction.
     */
    private ExtractionGuard placeExtractionGuard(String path) {
        boolean isPrimary;
        CountDownLatch completionLatch;

        synchronized( extractionsLock ) {
            completionLatch = extractionLocks.get(path);

            if ( completionLatch != null ) {
                isPrimary = false;

            } else {
                isPrimary = true;

                completionLatch = new CountDownLatch(1);
                extractionLocks.put(path,  completionLatch);
            }
        }

        return new ExtractionGuard(path, isPrimary, completionLatch);
    }

    // TODO: Not sure on the timing of removing the latch and
    //       unblocking secondary extractions.

    private void releaseExtractionGuard(ExtractionGuard extractionLatch) {
        synchronized( extractionsLock ) {
            extractionLocks.remove( extractionLatch.path );
        }
        extractionLatch.completionLatch.countDown();
    }

    private class ExtractionGuard {
        public final String path;
        public final boolean isPrimary;
        public final CountDownLatch completionLatch;

        public ExtractionGuard(String path, boolean isPrimary, CountDownLatch completionLatch) {
            this.path = path;
            this.isPrimary = isPrimary;
            this.completionLatch = completionLatch;
        }

        //

        /**
         * Wait for the extraction which is being timed by the completion
         * latch completes.  Answer true if the extraction completes before
         * the latch times out.  Answer false if the latch times out, or
         * if the thread is interrupted.
         *
         * The timeout is set to 60 seconds.
         *
         * TODO: Should this timeout value be tunable?
         *
         * @return True or false telling if the latch completed before
         *     timing out.
         */
        @FFDCIgnore(InterruptedException.class)
        public boolean waitForCompletion() {
            try {
                return completionLatch.await(60, TimeUnit.SECONDS);
            } catch ( InterruptedException e ) {
                return false;
            }
        }
    }

    /**
     * Extract data from an entry to a target directory.
     *
     * The target directory must be the same for calls made on the same
     * entry.  (The target directory is currently always the cache
     * directory of the zip file container.)
     *
     * Extractions which are performed on the same zip file container
     * are aware of each other.  Two extractions on the same path will
     * never be performed at the same time.
     *
     * Extraction requests are aware of previous extractions.  There are
     * several cases:
     *
     * <ul>
     * <li>No extraction exists.  Extract the file.</li>
     * <li>An extraction exists, and is not a file.  Delete
     *     the prior extraction, then extract.</li>
     * <li>An extraction exists, and is out-of-date.  Delete
     *     the prior extraction, then extract.</li>
     * <li>An extraction exists and is up-to-date.  Do not delete
     *     the prior extraction.  Do not extract.</li>
     * </ul>
     *
     * @param inputEntry The entry which is to be extracted.
     * @param outputDir The directory to which to extract the data.
     *
     * @return The file which was created to which the entry was extracted.
     *
     * @throws IOException Thrown if extraction fails.
     */
    private File extractEntry(ArtifactEntry inputEntry, File outputDir) throws IOException {
        if ( !FileUtils.ensureDirExists(outputDir) ) {
            throw new IOException("Extraction directory could not be created [ " + outputDir.getAbsolutePath() + " ]");
        }

        File outputFile = new File( outputDir, inputEntry.getName() );
        String outputPath = getCanonicalPath(outputFile); // throws IOException
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, "Extraction: [ " + outputPath + " ]");
        }

        ExtractionGuard extractionGuard = placeExtractionGuard(outputPath);

        // If we are secondary, wait for the primary to complete, then
        // verify and use the primary extraction.

        if ( !extractionGuard.isPrimary ) {
            if ( !extractionGuard.waitForCompletion() ) {
                // Tr.warning(tc, "Secondary extraction timeout [ " + outputPath + " ]");
                Tr.warning(tc, "extract.secondary.timeout", outputPath);
                throw new IOException("Secondary extraction timeout [ " + outputPath + " ]");

            } else {
                if ( !FileUtils.fileExists(outputFile) ) {
                    // Tr.warning(tc, "Failed secondary extraction [ " + outputPath + " ]");
                    Tr.warning(tc, "extract.secondary.failed", outputPath);
                    throw new IOException("Failed secondary extraction [ " + outputPath + " ]");
                } else if ( !FileUtils.fileIsFile(outputFile) ) {
                    // Tr.warning(tc, "Secondary extraction did not create a simple file [ " + outputPath + " ]");
                    Tr.warning(tc, "extract.secondary.notfile", outputPath);
                    throw new IOException("Secondary extraction did not create a simple file [ " + outputPath + " ]");

                } else {
                    if ( isModified(inputEntry, outputFile) ) {
                        // Tr.warning(tc, "Secondary extraction inconsistent times [ " + outputPath + " ]");
                        Tr.warning(tc, "extract.secondary.inconsistent", outputPath);
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                            Tr.debug(tc, "Secondary extraction: [ " + outputPath + " ]");
                        }
                    }
                    return outputFile;
                }
            }
        }

        // Otherwise, we are primary: Try to use any prior extraction.  If we can't,
        // remove it, and do the extraction.  Then unblock any secondary extractions
        // waiting on us.

        try {
            boolean doRemove;
            boolean doExtract;
            String extractCase;

            if ( FileUtils.fileExists(outputFile) ) {
                if ( FileUtils.fileIsFile(outputFile) ) {
                    if ( !isModified(inputEntry, outputFile) ) {
                        doRemove = false;
                        doExtract = false;
                        extractCase = "Normal: Prior extraction is up to date";
                    } else {
                        doRemove = true;
                        doExtract = true;
                        extractCase = "Normal: Prior extraction is out of date";
                    }
                } else if ( FileUtils.fileIsDirectory(outputFile) ) {
                    doRemove = true;
                    doExtract = true;
                    extractCase = "Abnormal: Prior extraction is a directory";
                    Tr.warning(tc, "extract.primary.directory", outputPath);
                } else {
                    doRemove = true;
                    doExtract = true;
                    extractCase = "Abnormal: Prior extraction is untyped";
                    Tr.warning(tc, "extract.primary.untyped", outputPath);
                }
            } else {
                doRemove = false;
                doExtract = true;
                extractCase = "Normal: No prior extraction";
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                Tr.debug(tc, "Primary extraction: [ " + outputPath + " ] (" + extractCase + ")");
            }

            long extractStart = (COLLECT_TIMINGS ? System.nanoTime() : -1L);

            if ( doRemove ) {
                deleteAll(outputFile);
            }

            if ( doExtract ) {
                transfer(inputEntry, outputFile);
                setLastModified(inputEntry, outputFile, outputPath);
            }

            if ( COLLECT_TIMINGS ) {
                record(Timings.EXTRACT_TIME, extractStart, System.nanoTime(), null, EXTRACT_FREQUENCY);
            }
            
            return outputFile;

        } finally {
            releaseExtractionGuard(extractionGuard);
        }
    }

    /**
     * Tell if an entry is modified relative to a file.  That is if
     * the last modified times are different.
     *
     * File last update times are accurate to about a second.  Allow
     * the last modified times to match if they are that close.
     *
     * @param entry The entry to test.
     * @param file The file to test.
     *
     * @return True or false telling if the last modified times of the
     *     file and entry are different.
     */
    private boolean isModified(ArtifactEntry entry, File file) {
        long fileLastModified = FileUtils.fileLastModified(file);
        long entryLastModified = entry.getLastModified();

        // File 100K  entry  10K  delta 90k  true   (entry is much older than the file)
        // File  10k  entry 100k  delta 90k  true   (file is much older than the entry)
        // File  10k  entry   9k  delta  1k  false  (entry is slightly older than the file)
        // File   9k  entry  10k  delta  1k  false  (file is slightly older than the entry)
        // File   9k  entry   9k  delta  0k  false  (file and entry are exactly the same age)

        return ( Math.abs(fileLastModified - entryLastModified) >= 1010L );
    }

    private void transfer(ArtifactEntry inputEntry, File outputFile) throws IOException {
        // TODO: When is an IOException thrown, and when is null returned?
        InputStream inputStream = inputEntry.getInputStream(); // throws IOException
        if ( inputStream == null ) {
            return;
        }

        try {
            FileOutputStream outputStream = FileUtils.getFileOutputStream(outputFile);

            try {
                byte transferBuffer[] = new byte[1024 * 16];

                int bytesRead;
                while ( (bytesRead = inputStream.read(transferBuffer)) > 0 ) {
                    outputStream.write(transferBuffer, 0, bytesRead);
                }

            } finally {
                outputStream.close(); // throws IOException
            }

        } finally {
            inputStream.close(); // throws IOException
        }
    }

    private void setLastModified(ArtifactEntry inputEntry, File outputFile, String outputPath) {
        if ( !FileUtils.fileSetLastModified( outputFile, inputEntry.getLastModified() ) ) {
            Tr.error(tc, "Failed to set last modified [ " + outputPath + " ]");
        }
    }

    // TODO: Move this into FileUtils; move the FileUtils wrapper calls into
    //       a single consolidated wrapper.

    @Trivial
    private boolean deleteAll(File rootFile) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, "Delete [ " + rootFile.getAbsolutePath() + " ]");
        }

        if ( FileUtils.fileIsFile(rootFile) ) {
            boolean didDelete = FileUtils.fileDelete(rootFile);
            if ( !didDelete ) {
                Tr.error(tc, "Could not delete file [ " + rootFile.getAbsolutePath() + " ]");
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    Tr.debug(tc, "Deleted");
                }
            }
            return didDelete;

        } else {
            boolean didDeleteAll = true;
            int deleteCount = 0;

            File childFiles[] = FileUtils.listFiles(rootFile);
            int childCount;
            if ( childFiles != null ) {
                childCount = childFiles.length;
                for ( File childFile : childFiles ) {
                    // Keep iterating even if one of the deletes fails.
                    // Delete as much as possible.
                    if ( !deleteAll(childFile) ) {
                        didDeleteAll = false;
                    } else {
                        deleteCount++;
                    }
                }
            } else {
                childCount = 0;
                deleteCount = 0;
            }

            if ( didDeleteAll ) {
                didDeleteAll = FileUtils.fileDelete(rootFile);
            }
            if ( !didDeleteAll ) {
                Tr.error(tc, "Could not delete directory [ " + rootFile.getAbsolutePath() + " ]");
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                Tr.debug(tc, "Deleted [ " + Integer.valueOf(deleteCount) + " ]" +
                             " of [ " + Integer.valueOf(childCount) + " ]");
            }

            return didDeleteAll;
        }
    }
}
