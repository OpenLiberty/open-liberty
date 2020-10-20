/*******************************************************************************
 * Copyright (c) 2011,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.zip.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.artifact.contributor.ArtifactContainerFactoryHelper;
import com.ibm.ws.artifact.zip.cache.ZipCachingService;
import com.ibm.ws.classloading.configuration.GlobalClassloadingConfiguration;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;

/**
 * ZIP file container factory.
 *
 * This is a delegate container factory which is used by the artifact file system
 * delegating container factory to attempt to create zip file type containers.
 *
 * Generally, a zip file type container can be created on files and on artifact entries,
 * when the file or entry has a valid zip type extension, and when at least one entry can
 * be read from the file or entry.
 *
 * A file or entry which has a zip type extension is expected to contain valid zip data.
 * Warnings are issued when a file or entry which has a zip type extension fails to read
 * as a zip file.
 */
public class ZipFileContainerFactory implements ArtifactContainerFactoryHelper, ContainerFactoryHolder {
    static final TraceComponent tc = Tr.register(ZipFileContainerFactory.class);

    //

    private BundleContext bundleContext;

    protected synchronized void activate(ComponentContext componentContext) {
        bundleContext = componentContext.getBundleContext();
    }

    protected synchronized void deactivate(ComponentContext componentContext) {
        rootContainerFactory = null;
        bundleContext = null;
    }

    @Trivial
    @Override
    public synchronized BundleContext getBundleContext() {
        if ( bundleContext == null ) {
            // TODO: Why throw this?
            throw new IllegalStateException();
        }
        return bundleContext;
    }

    //

    /**
     * The root delegating container factory used by this zip file container.
     *
     * The root delegating container factory is used to convert entries to
     * non-enclosed containers.
     *
     * The factory is a "root delegating" factory because it is populated by
     * declarative services to call out to specific container factories
     * (such as this {@link ZipFileContainerFactory}.  When attempting to
     * convert an entry to a container the root delegating factory iterates
     * across the specific contains it has been given and gives them each
     * a try at converting the container.
     *
     * The conversion of an entry to a container does <strong>not</strong> use
     * the root delegating container factory when a local conversion is requested.
     *
     * See {@link ZipFileEntry#convertToContainer(boolean)}
     */
    private ArtifactContainerFactory rootContainerFactory;

    protected synchronized void setContainerFactory(ArtifactContainerFactory rootContainerFactory) {
        this.rootContainerFactory = rootContainerFactory;
    }

    @SuppressWarnings("hiding")
	protected synchronized void unsetContainerFactory(
        ArtifactContainerFactory rootContainerFactory) {

        if ( this.rootContainerFactory == rootContainerFactory ) {
            this.rootContainerFactory = null;
        }
    }

    @Trivial
    @Override
    public synchronized ArtifactContainerFactory getContainerFactory() {
        if ( rootContainerFactory == null ) {
            throw new IllegalStateException();
        }
        return rootContainerFactory;
    }

    //

    // TODO: Should access to the class loading configuration be synchronized?
    //       An assignment during access could leave the reference spliced and
    //       unusable.

    private GlobalClassloadingConfiguration classLoadingConfiguration;

    protected void setGlobalClassloadingConfiguration(GlobalClassloadingConfiguration classloadingConfiguration) {
        this.classLoadingConfiguration = classloadingConfiguration;
    }

    //

    /**
     * Tell if the "jar:" protocol is to be used in archive URLs.  The default is
     * to use the "wsjar:" protocol.
     *
     * The result is obtained from the global class loading configuration.
     * See {GlobalClassLoadingConfiguration{@link #useJarUrls}.  A global
     * class loading configuration is optional.  When not available, answer
     * false.
     *
     * @return True or false telling if the "jar:" protocol is to be used instead
     *     of the "wsjar:" protocol.
     */
    @Trivial
    @Override
    public boolean useJarUrls() {
        if ( classLoadingConfiguration != null ) {
            return classLoadingConfiguration.useJarUrls();
        } else {
            return false;
        }
    }

    //

    // TODO: Should access to the zip caching service be synchronized?
    //       An assignment during access could leave the reference spliced and
    //       unusable.

    private ZipCachingService zipCachingService;

    protected void setZipCachingService(ZipCachingService zipCachingService) {
        this.zipCachingService = zipCachingService;
    }

    @Trivial
    @Override
    public ZipCachingService getZipCachingService() {
        if ( zipCachingService == null ) {
            throw new IllegalStateException();
        }
        return zipCachingService;
    }

    //

    /**
     * Attempt to create a root-of-roots zip file type container.
     *
     * Anser null if the container data is not a file or is not a valid
     * zip file.
     *
     * @return A new root-of-roots zip file type container.
     */
    @Override
    public ArtifactContainer createContainer(File cacheDir, Object containerData) {
        if ( !(containerData instanceof File) ) {
            return null;
        }

        File fileContainerData = (File) containerData;
        if ( !FileUtils.fileIsFile(fileContainerData) ) {
            return null;
        }

        if ( !isZip(fileContainerData) ) {
            return null;
        }

        return new ZipFileContainer(cacheDir, fileContainerData, this);
    }

    /**
     * Attempt to create an enclosed root zip file type container.
     *
     * The entry from which the container is to be created must container
     * data for a zip file.
     *
     * The container data, if supplied and a file, will be tested.  Otherwise,
     * the supplied entry will be tested.
     *
     * Answer null if the container data is a file but is not a zip file, or
     * if the entry is not a zip entry.
     *
     * @return The new enclosed root zip file container.  Null if the attempt
     *     fails.
     */
    @Override
    public ArtifactContainer createContainer(
        File cacheDir,
        ArtifactContainer enclosingContainer,
        ArtifactEntry entryInEnclosingContainer,
        Object containerData) {

        if ( (containerData instanceof File) && FileUtils.fileIsFile((File) containerData) ) {
            File fileContainerData = (File) containerData;
            if ( isZip(fileContainerData) ) {
                return new ZipFileContainer(
                    cacheDir,
                    enclosingContainer, entryInEnclosingContainer,
                    fileContainerData,
                    this);
            } else {
                return null;
            }

        } else {
            if ( isZip(entryInEnclosingContainer) ) {
                return new ZipFileContainer(
                    cacheDir,
                    enclosingContainer, entryInEnclosingContainer,
                    null,
                    this);
            } else {
                return null;
            }
        }
    }

    //

    // TODO: There is an implementation conflict.  These are also provided
    //       as the service property "handlesEntry".  More appropriately,
    //       this test would use the service property value, instead of
    //       hard coding the accepted extensions.

    private static final String[] ZIP_EXTENSIONS = new String[] {
        "jar",
        "zip",
        "ear", "war", "rar",
        "eba", "esa",
        "sar"
    };

    private static final String ZIP_EXTENSION_SPRING = "spring";

    private static final boolean IGNORE_CASE = true;

    /**
     * Tell if a file name has a zip file type extension.
     * 
     * These are: "jar", "zip", "ear", "war", "rar", "eba", "esa",
     * "sar", and "spring".
     * 
     * See also the service property "handlesEntry".
     *
     * @param name The file name to test.
     * 
     * @return True or false telling if the file has one of the
     *     zip file type extensions.
     */
    private static boolean hasZipExtension(String name) {
        int nameLen = name.length();

        // Need '.' plus at least three characters.

        if ( nameLen < 4 ) {
            return false;
        }

        // Need '.' plus at least six characters for ".spring".

        if ( nameLen >= 7 ) {
            if ( (name.charAt(nameLen - 7) == '.') &&
                 name.regionMatches(IGNORE_CASE, nameLen - 6, ZIP_EXTENSION_SPRING, 0, 6) ) {
                return true;
            }
        }

        if ( name.charAt(nameLen - 4) != '.' ) {
            return false;
        } else {
            for ( String ext : ZIP_EXTENSIONS ) {
                if ( name.regionMatches(IGNORE_CASE, nameLen - 3, ext, 0, 3) ) { // ignore case
                    return true;
                }
            }
            return false;
        }

        // return name.matches("(?i:(.*)\\.(ZIP|[SEJRW]AR|E[BS]A|SPRING))");
    }

    // If we caught an exception, it's not a zip file.
    // Or, it is a broken zip file.
    // Or, the disk is failing

    /**
     * Tell if an artifact entry is valid to be interpreted as a zip file
     * container.
     *
     * The entry must have a valid zip file extension (see {@link #hasZipExtension}.
     *
     * The entry must open as a {@link ZipInputStream}, and at least one entry
     * must be readable from that stream.
     *
     * A zip stream is used instead of a zip file: Opening a zip file on the
     * entry would force the entry to be extracted, and would cause the table
     * of entries of the zip file to be loaded.
     *
     * @param artifactEntry The entry to test as a zip file container.
     *
     * @return True or false telling if the entry can be interpreted as
     *     a zip file container.
     */
    private static boolean isZip(ArtifactEntry artifactEntry) {
        if ( !hasZipExtension( artifactEntry.getName() ) ) {
            return false;
        }

        boolean validZip = false;

        InputStream entryInputStream = null;
        try {
            entryInputStream = artifactEntry.getInputStream();
            if ( entryInputStream == null ) {
                return false;
            }

            ZipInputStream zipInputStream = new ZipInputStream(entryInputStream);
            try {
                ZipEntry entry = zipInputStream.getNextEntry();
                if ( entry == null ) {
                    Tr.error(tc, "bad.zip.data", getPhysicalPath(artifactEntry));
                } else {
                    validZip = true;
                }
            } catch ( IOException e ) {
                String entryPath = getPhysicalPath(artifactEntry);
                Tr.error(tc, "bad.zip.data", entryPath);
            }

            try {
                // attempt to close the zip, ignoring any error because we can't recover.
                zipInputStream.close();
            } catch (IOException ioe) {
                // FFDC
            }
        } catch ( IOException e1 ) {
            // FFDC
            return false;
        }

        return validZip;
    }

    /**
     * Answer they physical path of an artifact entry.
     *
     * If the entry has a physical path (for example, because it was
     * extracted), directly answer that path.  In that case, the path
     * of the entry may be in a cache directory, and may not have a
     * relationship to enclosing containers physical path.
     *
     * If the entry does not have a physical path, walk upwards until
     * an enclosing container has a physical path, then append the
     * path of enclosing entry which reaches the entry, placing "!"
     * after each enclosing path.
     *
     * @param artifactEntry The entry for which to obtain a physical path.
     *
     * @return The physical path of the entry.
     */
    @SuppressWarnings("deprecation")
    private static String getPhysicalPath(ArtifactEntry artifactEntry) {
        String physicalPath = artifactEntry.getPhysicalPath();
        if ( physicalPath != null ) {
            return physicalPath;
        }

        String entryPath = artifactEntry.getPath();
        String rootPath = artifactEntry.getRoot().getPhysicalPath();
        if ( rootPath != null ) {
            return rootPath + "!" + entryPath;
        }

        while ( (artifactEntry = artifactEntry.getRoot().getEntryInEnclosingContainer()) != null ) {
            String nextPhysicalPath = artifactEntry.getPhysicalPath();
            if ( nextPhysicalPath != null ) {
                return nextPhysicalPath + "!" + entryPath;
            }
            entryPath = artifactEntry.getPath() + "!" + entryPath;
        }

        return entryPath;
    }

    /**
     * Tell if a file is valid to used to create a zip file container.
     *
     * The file must have a valid zip file extension (see {@link #hasZipExtension}.
     *
     * The file must open as a {@link ZipInputStream}, and at least one entry
     * must be readable from that stream.
     *
     * A zip stream is used instead of a zip file: Opening a zip file on the
     * file would cause the table of entries of the zip file to be loaded.
     *
     * @param file The file to test as a zip file container.
     *
     * @return True or false telling if the file can be interpreted as
     *     a zip file container.
     */
    @FFDCIgnore( { IOException.class, FileNotFoundException.class } )
    private static boolean isZip(File file) {
        if ( !hasZipExtension( file.getName() ) ) {
            return false;
        }

        InputStream inputStream = null;

        try {
            inputStream = new FileInputStream(file); // throws FileNotFoundException

            ZipInputStream zipInputStream = new ZipInputStream(inputStream);

            try {
                ZipEntry entry = zipInputStream.getNextEntry(); // throws IOException
                if ( entry == null ) {
                    //First Check if has script in header
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF8"));
                    if (reader.ready()) {
                        String line = reader.readLine();
                        if(line.startsWith("#!/bin/bash"))
                            return true;
                    }
                    Tr.error(tc, "bad.zip.data", file.getAbsolutePath());
                    return false;
                }
                return true;
            } catch ( IOException e ) {
                Tr.error(tc, "bad.zip.data", file.getAbsolutePath());
                return false;
            }

        } catch ( FileNotFoundException e ) {
            Tr.error(tc, "Missing zip file " + file.getAbsolutePath());
            return false;

        } finally {
            if ( inputStream != null ) {
                try {
                    inputStream.close();
                } catch ( IOException e ) {
                    // IGNORE
                }
            }
        }
    }
}
