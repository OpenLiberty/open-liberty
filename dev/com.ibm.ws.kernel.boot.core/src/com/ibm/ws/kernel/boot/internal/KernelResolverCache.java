/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.provisioning.NameBasedLocalBundleRepository;
import com.ibm.ws.kernel.provisioning.VersionUtility;

/**
 * Cache of kernel features, read from the feature manifests.
 *
 * These are the kernel core, a log provider, and optional extensions.
 *
 * The features can include other manifest and other resources.
 */
public class KernelResolverCache {

    /**
     * Standard constructor.
     *
     * The new cache starts clean, but unloaded.
     *
     * @param cacheFile   The persistence file of the cache.
     * @param libertyBoot Control parameter. Tells if boot marks are to be
     *                        added to child content elements.
     */
    protected KernelResolverCache(File cacheFile, boolean libertyBoot) {
        this.cacheFile = cacheFile;
        this.cachePath = ((cacheFile == null) ? null : cacheFile.getAbsolutePath());

        this.libertyBoot = libertyBoot;

        this.features = new ArrayList<>(0);
        this.manifestElements = new LinkedHashMap<>();
    }

    //

    private final File cacheFile;
    private final String cachePath;

    /**
     * Answer the cache file.
     *
     * The cache file may be null, in which case most caching
     * function is disabled.
     *
     * The cache file may not initially exist.
     *
     * @return The cache file.
     */
    public File getCacheFile() {
        return cacheFile;
    }

    /**
     * Answer the absolute path to the cache file. Null will
     * be returned if the cache file is not set.
     *
     * @return The absolute path to the cache file.
     */
    public String getCachePath() {
        return cachePath;
    }

    /**
     * Ensure that the parent of the cache folder exists and is a directory.
     *
     * Create the parent if necessary.
     *
     * Do not delete an existing parent, even if it is not a directory. Do
     * not test for the cache file itself.
     *
     * @throws IOException Thrown if the parent could not be created or is
     *                         not a directory.
     */
    private void ensureCachePath() throws IOException {
        File parent = cacheFile.getParentFile();
        if (parent.exists()) {
            if (parent.isDirectory()) {
                return;
            }
            throw new IOException("Parent of kernel cache is not a directory [ " + cachePath + " ]");
        }

        parent.mkdirs();
        if (parent.exists()) {
            if (parent.isDirectory()) {
                return;
            }
            throw new IOException("Parent of kernel cache is not a directory [ " + cachePath + " ]");
        }

        throw new IOException("Failed to create parents of kernel cache [ " + cachePath + " ]");
    }

    public void delete() {
        if ((cacheFile == null) || !cacheFile.exists()) {
            return;
        }

        cacheFile.delete();
    }

    //

    private final boolean libertyBoot;

    /**
     * Answer the 'liberty boot' control parameter value.
     *
     * @return the 'liberty boot' control parameter value.
     */
    public boolean isLibertyBoot() {
        return libertyBoot;
    }

    //

    private final ArrayList<String> features;

    /**
     * Answer the features stored in this cache.
     *
     * @return The features stored in this cache.
     */
    public List<String> getFeatures() {
        return features;
    }

    /**
     * Set the features stored in this cache.
     *
     * @param features The features stored in this cache.
     */
    protected void setFeatures(List<String> features) {
        this.features.clear();
        this.features.ensureCapacity(features.size());
        this.features.addAll(features);
    }

    /**
     * Set the features stored in this cache.
     *
     * @param features The features stored in this cache.
     */
    protected void setFeatures(String[] features) {
        this.features.clear();
        this.features.ensureCapacity(features.length);
        for (String feature : features) {
            this.features.add(feature);
        }
    }

    /**
     * Tell if this cache has specified features.
     *
     * Answer false if this cache has no features, even if the
     * specified features are also empty.
     *
     * @param otherFeatures Features to test against this cache.
     *
     * @return True or false telling if this cache has the specified
     *         features.
     */
    public boolean hasFeatures(List<String> otherFeatures) {
        // If the list from the cache is empty, then the cache was empty,
        // so we don't have to start clean, as we likely already are...

        if (features.isEmpty()) {
            return false;
        } else if (features.size() != otherFeatures.size()) {
            return true;
        } else {
            return !features.containsAll(otherFeatures);
        }
    }

    //

    private final Map<String, KernelManifestElement> manifestElements;

    /**
     * Store a manifest element in the cache.
     *
     * The stored element is keyed on the element name, which is the
     * manifest file name of the element.
     *
     * @param manifestElement A manifest element which is to be stored.
     */
    private void putManifestElement(KernelManifestElement manifestElement) {
        manifestElements.put(manifestElement.getName(), manifestElement);
    }

    /**
     * Answer the manifest element matching a specified manifest file.
     *
     * Lookup using the name of the manifest file.
     *
     * @param manifestFile A manifest file to locate in this cache.
     *
     * @return The manifest element associated with the name of the
     *         manifest file.
     */
    public KernelManifestElement get(File manifestFile) {
        return manifestElements.get(manifestFile.getName());
    }

    /**
     * Answer the stored manifest elements.
     *
     * @return The stored manifest elements.
     */
    public Collection<KernelManifestElement> getManifestElements() {
        return manifestElements.values();
    }

    /**
     * Answer the jar files of a manifest element.
     *
     * @param manifestFile The file of the target manifest element.
     *
     * @return The jar files of the manifest element matching the
     *         target manifest file. An empty list if no element is
     *         present for the manifest file.
     */
    public List<File> getJarFiles(File manifestFile) {
        if (manifestFile != null) {
            KernelManifestElement manifestElement = get(manifestFile);
            if (manifestElement != null) {
                return manifestElement.getBootJarFiles();
            }
        }
        return Collections.emptyList();
    }

    //

    private boolean isDirty;

    /**
     * Mark that this cache has changes relative to the persisted
     * cache.
     */
    protected void setDirty() {
        isDirty = true;
    }

    /**
     * Mark that this cache has no changes relative to the persisted
     * cache.
     */
    protected void setClean() {
        isDirty = false;
    }

    /**
     * Tell if this cache has changes relative to the persisted cache.
     *
     * @return True or false telling if this cache has changes.
     */
    public boolean isDirty() {
        return isDirty;
    }

    //

    private final static String BUNDLE_LINE = "--|";
    private static final String INUSE_KERNEL_FEATURES = "@=";
    private final static String NL = System.getProperty("line.separator");

    /**
     * Load this cache from the cache file.
     *
     * Do nothing if the cache file is not set, or does not exist.
     *
     * The cache file should have the format:
     *
     * <pre>
     * kernelManifest-1.0=kernel;manifest;information
     * --|kernelManifest-1.0|bundle.symbolic.name...
     *
     * The cache is not marked as clean at the conclusion of the load.
     * The dirty mark must be cleared as an additional step.
     */
    @SuppressWarnings("resource")
    public void load() {
        if ((cacheFile == null) || !cacheFile.exists()) {
            return;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(cacheFile), StandardCharsets.UTF_8));

            String line;
            KernelManifestElement currentEntry = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith(BUNDLE_LINE)) {
                    // If line starts with "--|", this is a line describing a kernel bundle
                    if (currentEntry == null) {
                        throw new IOException("Cache file contents corrupted");
                    } else {
                        // "--|kernelCore-1.0|...."
                        line = line.substring(BUNDLE_LINE.length()); // skip --|
                        if (line.startsWith(currentEntry.getName())) {
                            // skip "kernelCore-1.0|"
                            line = line.substring(currentEntry.getName().length() + 1);

                            // The rest of the line is a bundle entry.
                            KernelBundleElementImpl bEntry = new KernelBundleElementImpl(this, line);
                            currentEntry.putBundleElement(bEntry.getSymbolicName(), bEntry);
                        } else {
                            throw new IOException("Cache file contents corrupted");
                        }
                    }
                } else if (line.startsWith(INUSE_KERNEL_FEATURES)) {
                    String[] parts = line.substring(INUSE_KERNEL_FEATURES.length()).split(";");
                    setFeatures(parts);

                } else {
                    putManifestElement(currentEntry = new KernelManifestElement(this, line));
                }
            }

        } catch (IOException e) {
            // no cache. BOO. Start clean if something happened halfway through the read
            manifestElements.clear();

        } finally {
            Utils.tryToClose(reader);
        }
    }

    /**
     * Write the cache to file.
     *
     * Do nothing if the cache file is null, or if the cache is unchanged.
     *
     * Overwrite any current cache file.
     *
     * Capture and log any thrown IO exception. Do not throw the exception.
     *
     * At the conclusion of a successful write, mark the cache as clean.
     */
    public void store() {
        if ((cacheFile == null) || !isDirty()) {
            return;
        }

        PrintWriter writer = null;
        try {
            ensureCachePath();

            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(cacheFile), StandardCharsets.UTF_8));

            writer.flush();

            setClean();

        } catch (IOException e) {
            KernelUtils.logException(e,
                                     "Failure writing kernel cache to [ " + cachePath + " ]",
                                     "warning.noPlatformCache", cachePath);

        } finally {
            Utils.tryToClose(writer);
        }
    }

    /**
     * Write the cache to the cache file.
     *
     * @param writer The writer that will receive the cache.
     */
    protected void basicWrite(PrintWriter writer) {
        for (KernelManifestElement manifestElement : manifestElements.values()) {
            manifestElement.write(writer);
            writer.write(NL);
            for (KernelBundleElementImpl bundleElement : manifestElement.getBundleElements()) {
                writer.write(BUNDLE_LINE);
                writer.write(manifestElement.getName());
                writer.write('|');
                bundleElement.write(writer);
                writer.write(NL);
            }
        }

        writer.write(INUSE_KERNEL_FEATURES);
        boolean isFirst = true;
        for (String feature : features) {
            if (isFirst) {
                isFirst = false;
            } else {
                writer.write(';');
            }
            writer.write(feature);
        }
        writer.write(NL);
    }

    //

    public static final boolean FOLLOW_INCLUDES = true;

    private final static String LOG_PROVIDER = "WebSphere-LogProvider";
    private final static String SUBSYSTEM_CONTENT = "Subsystem-Content";
    private final static String INCLUDED_FILE = "osgi.subsystem.feature";

    private final static String BOOT_JAR_TYPE = "boot.jar";
    private final static String BUNDLE_TYPE = "osgi.bundle";

    /**
     * Retrieve the manifest element for a specified manifest file.
     *
     * If an element exists for the manifest, and hasn't changed relative
     * to the manifest file, answer that manifest element.
     *
     * Otherwise, mark the cache as dirty and create (or recreate) the
     * manifest element by reading the manifest file.
     *
     * Optionally, read included manifests. See {@link #readManifestElement}.
     *
     * @param manifestFile   A kernel feature manifest.
     * @param followIncludes Control parameter: Are feature includes to be followed?
     * @param repo           A bundle repository.
     *
     * @return The cache element for the feature manifest.
     */
    public KernelManifestElement checkEntry(File manifestFile,
                                            boolean followIncludes,
                                            NameBasedLocalBundleRepository repo) {

        KernelManifestElement manifestElement = get(manifestFile);
        if ((manifestElement != null) && manifestElement.isUnchanged(manifestFile)) {
            return manifestElement;
        }

        setDirty();

        manifestElement = readManifestElement(manifestFile, followIncludes, repo);

        manifestElements.put(manifestFile.getName(), manifestElement);

        return manifestElement;
    }

    /**
     * Read a manifest element from a manifest file. Conditionally, read manifest
     * elements for included manifests.
     *
     * Use the repository to select the version of any boot jars of the manifest
     * element.
     *
     * The read is minimal, skimming the manifest looking for a log provider,
     * for included subsystems, for bundle information, or for boot jar information.
     *
     * This is rough-and-dirty parsing: we aren't using a proper manifest
     * parser because we need one that isn't sensitive to the traditional
     * manifest's line-wrapping constraints, and we are about to bootstrap an
     * environment that has many of them.
     *
     * The read of the manifest element is sensitive to whether bootstrap
     * mode is enabled. When bootstrap mode is enabled, bootstrap jars are
     * marked with start phase LIBERTY_BOOT.
     *
     * @param manifestFile   A kernel feature manifest.
     * @param followIncludes Control parameter: Should elements be created
     *                           for included manifests.
     * @param repo           A bundle repository.
     *
     * @return The newly read manifest element.
     */
    protected KernelManifestElement readManifestElement(File manifestFile,
                                                        boolean followIncludes,
                                                        NameBasedLocalBundleRepository repo) {

        String logProvider = null;
        List<File> bootJars = new ArrayList<>();
        List<File> includedFiles = new ArrayList<>();

        Map<String, KernelBundleElementImpl> bundleEntries = new LinkedHashMap<>();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(manifestFile)));

            String line;
            boolean inSubsystemContent = false;

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) {
                    inSubsystemContent = false;
                    continue;

                } else if ((logProvider == null) && line.startsWith(LOG_PROVIDER)) {
                    // WebSphere-LogProvider: com.ibm.ws.logging.internal.hpel.HpelLogProviderImpl
                    if (line.length() > (LOG_PROVIDER.length() + 1)) {
                        logProvider = line.substring(LOG_PROVIDER.length() + 1).trim();
                    }
                    continue;

                } else if (line.startsWith(SUBSYSTEM_CONTENT)) {
                    line = line.substring(SUBSYSTEM_CONTENT.length() + 1);
                    inSubsystemContent = true;

                } else if (!inSubsystemContent) {
                    continue;
                }

                line = line.trim();

                if (!line.endsWith(",")) {
                    inSubsystemContent = false;
                }

                if (line.contains(BOOT_JAR_TYPE)) {
                    // com.ibm.ws.logging; version="[1,1.0.100)";
                    //   type="boot.jar"

                    // Either, process the line as fake bundle element, or as a boot jar.
                    //
                    // In either case, start by creating a content element for the line.
                    //
                    // In liberty boot mode, a fake bundle element is created for the content
                    // element.  Otherwise, a bundle jar is selected for the element and
                    // is added as boot jar.

                    if (libertyBoot) {
                        // Adjust the parsed line to add LIBERTY_BOOT as the boot jar's start-phase:
                        //
                        // com.ibm.ws.logging; version="[1,1.0.100)";
                        //   type="boot.jar";
                        //   start-phase:=LIBERTY_BOOT

                        KernelContentElement element = new KernelContentElement(line + "; start-phase:=LIBERTY_BOOT");
                        KernelBundleElementImpl cacheElement = new KernelBundleElementImpl(this, element);
                        bundleEntries.put(element.getSymbolicName(), cacheElement);

                    } else {
                        KernelContentElement element = new KernelContentElement(line);
                        bootJars.add(selectBundle(element, repo));
                    }

                } else if (followIncludes && line.contains(INCLUDED_FILE)) {
                    // com.ibm.websphere.appserver.logging-1.0;
                    //   location="lib/platform/defaultLogging-1.0.mf";
                    //   type="osgi.subsystem.feature"

                    // Conditionally, process included files.
                    //
                    // Ignore any inclusion which does not have a location, or which
                    // does not exist.
                    //
                    // Inclusions are located relative to the repository root directory.
                    //
                    // Processing is recursive.  There is no current check for include
                    // cycles.
                    //
                    // Processing not only adds an entry for the included element, but also
                    // rolls up the boot jars of the inclusion to the current element, and
                    // rolls up the inclusions of the inclusion to the current element.

                    KernelContentElement element = new KernelContentElement(line);

                    if (element.getLocation() != null) {
                        File includedManifest = new File(repo.getRootDirectory(), element.getLocation());
                        if (includedManifest.exists()) {
                            KernelManifestElement includedElement = checkEntry(includedManifest, followIncludes, repo);
                            includedFiles.add(includedManifest);
                            bootJars.addAll(includedElement.getBootJarFiles());
                            includedFiles.addAll(includedElement.getIncludedFiles());
                        } else {
                            // Ignore it!
                        }
                    } else {
                        // Ignore it!
                    }

                } else if (line.contains(BUNDLE_TYPE) ||
                           !line.contains(KernelContentElement.TYPE)) {

                    // com.ibm.ws.org.objectweb.asm.all.4.0;
                    //   version="[1,1.0.100)";
                    //   start-phase:=BOOTSTRAP,

                    // Default the content to "osgi.bundle".

                    KernelContentElement element = new KernelContentElement(line);
                    KernelBundleElementImpl cacheElement = new KernelBundleElementImpl(this, element);
                    bundleEntries.put(element.getSymbolicName(), cacheElement);

                } else {
                    // Ignore this line!
                }
            }

        } catch (IOException e) {
            throw KernelUtils.logException(e,
                                           "Kernel definition could not be read: " + manifestFile.getAbsolutePath(),
                                           "error.unknownException");
        } finally {
            Utils.tryToClose(reader);
        }

        return new KernelManifestElement(this, manifestFile, bootJars, includedFiles, logProvider, bundleEntries);
    }

    // Now we do the work to find the right jar given the version range.
    // We're very very low-level here, so we're using a straight-up naming
    // convention to find the jars we have to choose from. Using a
    // ContentBasedLocalBundleRepository comes in the next layer, when
    // we're finding the kernel bundles.

    /**
     * Select the "best fit" bundle file for a subsystem content element, based on
     * the symbolic name of the element and the version range of the element.
     *
     * This is done by the repository. See {@link NameBasedLocalBundleRepository#selectBundle(String, org.osgi.framework.VersionRange)}.
     *
     * Throw an exception if no best match file can be selected.
     *
     * @param element A subsystem content element.
     * @param repo    A bundle repository.
     *
     * @return The best fit bundle file for the element.
     */
    private File selectBundle(KernelContentElement element,
                              NameBasedLocalBundleRepository repo) {

        File bestMatchFile = repo.selectBundle(element.getSymbolicName(),
                                               VersionUtility.stringToVersionRange(element.getRange()));
        if (bestMatchFile == null) {
            throw KernelUtils.logException("Could not find bundle for " + element + ".",
                                           "error.missingBundleException");
        }

        return bestMatchFile;
    }

    /**
     * Dispose of the manifest elements.
     *
     * Dispose of each of the individual manifest elements, then clear
     * the collection of manifest elements.
     */
    public void dispose() {
        for (KernelManifestElement manifestElement : manifestElements.values()) {
            manifestElement.dispose();
        }
        manifestElements.clear();
    }
}
