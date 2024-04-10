/*******************************************************************************
 * Copyright (c) 2010, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class KernelManifestElement {
    /**
     * Standard constructor. Construct a cache element
     * for a kernel feature manifest.
     *
     * @param cache            The cache holding this manifest element.
     * @param manifestFile     The kernel feature's manifest file.
     * @param bootJarFiles     The boot jar files associated with the manifest file.
     * @param includedFiles    The files included by the manifest file.
     * @param logProviderClass The log provider class of the manifest file.
     * @param bundleElements   The bundle entries, as cache entries, of the manifest file.
     */
    protected KernelManifestElement(KernelResolverCache cache,
                                    File manifestFile,
                                    List<File> bootJarFiles,
                                    List<File> includedFiles,
                                    String logProviderClass,
                                    Map<String, KernelBundleElementImpl> bundleElements) {

        this.cache = cache;

        this.name = manifestFile.getName();
        this.location = manifestFile.getAbsolutePath();
        this.lastModified = manifestFile.lastModified();
        this.fileSize = manifestFile.length();

        this.bootJarFiles = ((bootJarFiles == null) ? KernelUtils.emptyFiles : bootJarFiles);
        this.includedFiles = ((includedFiles == null) ? KernelUtils.emptyFiles : includedFiles);
        this.logProviderClass = logProviderClass;

        this.bundleElements = bundleElements;
    }

    private static final String MANIFEST_SPLIT = ";";
    private static final Pattern MANIFEST_SPLIT_PATTERN = Pattern.compile(MANIFEST_SPLIT);

    private static final String FILE_SPLIT = ",";

    /**
     * Deserializing initializer. Create a manifest cache element by
     * parsing line from the serialized cache.
     *
     * This initializer sets an empty collection of bundle elements.
     *
     * @param cache The cache holding this manifest element;
     * @param line  The line which is to parsed.
     */
    protected KernelManifestElement(KernelResolverCache cache, String line) {
        this.cache = cache;

        int index = line.indexOf('=');
        name = line.substring(0, index);

        String[] parts = MANIFEST_SPLIT_PATTERN.split(line.substring(index + 1));

        location = (parts.length > 0) ? parts[0] : null;
        lastModified = (parts.length > 1) ? Long.parseLong(parts[1]) : -1;
        fileSize = (parts.length > 2) ? Long.parseLong(parts[2]) : -1;

        bootJarFiles = KernelUtils.parseFiles(((parts.length > 3) ? parts[3] : null), FILE_SPLIT);
        includedFiles = KernelUtils.parseFiles(((parts.length > 4) ? parts[4] : null), FILE_SPLIT);
        logProviderClass = (parts.length > 5) ? parts[5] : null;

        bundleElements = new LinkedHashMap<>();
    }

    //

    private final KernelResolverCache cache;

    /**
     * Answer the cache holding this manifest element.
     *
     * @return
     */
    public KernelResolverCache getCache() {
        return cache;
    }

    //

    private final String name;

    /**
     * Answer the name of the bundle manifest file.
     *
     * @return The name of the bundle manifest file.
     */
    public String getName() {
        return name;
    }

    private final String location;

    /**
     * Answer the absolute path of the bundle manifest file.
     *
     * @return The absolute path of the bundle manifest file.
     */
    public String getLocation() {
        return location;
    }

    private final long lastModified;

    public long getLastModified() {
        return lastModified;
    }

    private final long fileSize;

    public long getFileSize() {
        return fileSize;
    }

    //

    private final List<File> bootJarFiles;

    public List<File> getBootJarFiles() {
        return bootJarFiles;
    }

    private final List<File> includedFiles;

    public List<File> getIncludedFiles() {
        return includedFiles;
    }

    private final String logProviderClass;

    public String getLogProviderClass() {
        return logProviderClass;
    }

    //

    private final Map<String, KernelBundleElementImpl> bundleElements;

    protected void putBundleElement(String symbolicName, KernelBundleElementImpl bundleElement) {
        bundleElements.put(symbolicName, bundleElement);
    }

    protected void putBundleElement(KernelBundleElementImpl bundleElement) {
        bundleElements.put(bundleElement.getSymbolicName(), bundleElement);
    }

    public Collection<KernelBundleElementImpl> getBundleElements() {
        return bundleElements.values();
    }

    public void dispose() {
        bundleElements.clear();
    }

    //

    /**
     * Write this manifest element to a print writer.
     *
     * This is used to serialize manifest elements to the kernel cache.
     *
     * The format must be deserializable. See
     * {@link KernelManifestElement#KernelManifestElement(String)}
     * for deserialization steps.
     *
     * The format is very simple, with ';' delimited values on a single line.
     * The name and location are written together with a '=' separator. The boot
     * jar list and the included resource list are written as comma delimited lists.
     * Empty lists are written as empty strings.
     *
     * <pre>
     * NAME=LOCATION;LAST_MODIFIED;SIZE;
     *   bootJar1.jar,bootJar2.jar,...;
     *   included1,included2,...;
     *   logProviderClass
     * </pre>
     *
     * @param writer A print writer.
     */
    public void write(PrintWriter writer) {
        writer.write(name);
        writer.write('=');
        writer.write(location);

        writer.write(MANIFEST_SPLIT);
        writer.write(Long.toString(lastModified));

        writer.write(MANIFEST_SPLIT);
        writer.write(Long.toString(fileSize));

        writer.write(MANIFEST_SPLIT);
        writer.write(KernelUtils.toString(bootJarFiles, FILE_SPLIT));

        writer.write(MANIFEST_SPLIT);
        writer.write(KernelUtils.toString(includedFiles, FILE_SPLIT));

        writer.write(MANIFEST_SPLIT);
        if (logProviderClass != null) {
            writer.write(logProviderClass);
        }
    }

    //

    /**
     * Tell if this element is unchanged, based on the source
     * manifest file and on included files.
     *
     * @param file The source manifest file of this element.
     *
     * @return True or false telling if the element is unchanged.
     */
    public boolean isUnchanged(File file) {
        if (!location.equals(file.getAbsolutePath()) ||
            (lastModified != file.lastModified()) ||
            (fileSize != file.length())) {
            return false;
        }

        for (File includedFile : includedFiles) {
            KernelManifestElement entry = cache.get(includedFile);
            if (entry == null) {
                return false;
            }
            if (!includedFile.exists()) {
                return false;
            }

            if (!entry.isUnchanged(includedFile)) {
                return false;
            }
        }

        return true;
    }
}