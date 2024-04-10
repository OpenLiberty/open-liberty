/*******************************************************************************
 * Copyright (c) 2010,2024 IBM Corporation and others.
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
package com.ibm.ws.kernel.boot.internal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.ibm.ws.kernel.boot.LaunchException;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.provisioning.NameBasedLocalBundleRepository;

/**
 * Repository and resolver of kernel features.
 *
 * Includes caching function.
 */
public class KernelResolver {
    public final static String CACHE_FILE = "platform/kernel.cache";

    //

    private final boolean forceCleanStart;

    /**
     * Tell if OSGi should be started cleanly. This is necessary
     * when bundles have been added or removed, or when the bundle
     * configuration has changed.
     */
    public boolean getForceCleanStart() {
        return forceCleanStart;
    }

    /**
     * Control parameter.
     *
     * The exact usage is unclear.
     *
     *
     */
    private final boolean libertyBoot;

    //

    /**
     * Standard initializer. The resolver is set as not being for liberty boot.
     */
    public KernelResolver(File installRoot, File cacheFile,
                          String kernelDefName,
                          String logProviderName,
                          String osExtensionName) {
        this(installRoot, cacheFile, kernelDefName, logProviderName, osExtensionName, !IS_LIBERTY_BOOT);
    }

    public static final boolean IS_LIBERTY_BOOT = true;

    /**
     * Create a kernel resolver for a liberty installation.
     *
     * Kernel features are expected in the provider directory, "lib/platform",
     * relative to the installation directory.
     *
     * A kernel feature and a log provider feature are required. The OS
     * extension feature is optional.
     *
     * The liberty boot parameter ... TBD
     *
     * @param installRoot     The root directory of the liberty installation.
     * @param cacheFile       A cache file of resolver data.
     * @param kernelDefName   The symbolic name of the kernel feature.
     * @param logProviderName The symbolic name of the log provider feature.
     * @param osExtensionName Optional symbolic name of a liberty
     *                            extension feature.
     * @param libertyBoot     Control parameter: Is the resolver for liberty bootstrapping.
     */
    public KernelResolver(File installRoot, File cacheFile,
                          String kernelDefName,
                          String logProviderName,
                          String osExtensionName,
                          boolean libertyBoot) {

        this.libertyBoot = libertyBoot;

        boolean cleanStart = false;

        boolean cacheAvailable = ((cacheFile != null) && cacheFile.exists());

        cache = new KernelResolverCache(cacheFile, libertyBoot);
        cache.load(); // Does nothing if the cache does not exist.  An expected case.

        List<String> kernelFeatures = new ArrayList<>(3);
        kernelFeatures.add(logProviderName);
        kernelFeatures.add(kernelDefName);
        if (osExtensionName != null) {
            kernelFeatures.add(osExtensionName);
        }

        // The cache must be discarded if the features changed.
        if (cache.hasFeatures(kernelFeatures)) {
            cache.dispose();
            cleanStart = true;
        }

        try {
            // Use a simple repository for bootstrapping.
            NameBasedLocalBundleRepository repo = new NameBasedLocalBundleRepository(installRoot);

            File platformDir = new File(installRoot, "lib/platform");

            // Validate core values ...

            if ((logProviderName == null) || logProviderName.trim().isEmpty()) {
                throw KernelUtils.logException("Log provider definition not found (Tr/FFDC)", "error.rasProvider");
            }
            logProviderManifest = new File(platformDir, logProviderName + ".mf");
            if (!logProviderManifest.exists()) {
                throw KernelUtils.logException("Kernel definition could not be found: " + logProviderManifest.getAbsolutePath(),
                                               "error.kernelDefFile", logProviderName);
            }
            KernelManifestElement logProviderElement = cache.checkEntry(logProviderManifest, KernelResolverCache.FOLLOW_INCLUDES, repo);

            logProviderClass = logProviderElement.getLogProviderClass();
            if (logProviderClass == null) {
                throw KernelUtils.logException("A log provider implementation was not defined", "error.rasProvider");
            }

            if ((kernelDefName == null) || kernelDefName.trim().isEmpty()) {
                throw KernelUtils.logException("Could not find kernel definition", "error.kernelDef");
            }
            kernelManifest = new File(platformDir, kernelDefName + ".mf");
            if (!kernelManifest.exists()) {
                throw KernelUtils.logException("Kernel definition could not be found: " + kernelManifest.getAbsolutePath(),
                                               "error.kernelDefFile", kernelDefName);
            }
            cache.checkEntry(kernelManifest, !KernelResolverCache.FOLLOW_INCLUDES, repo);

            if (osExtensionName != null) {
                osExtensionManifest = new File(platformDir, osExtensionName + ".mf");
                if (!osExtensionManifest.exists()) {
                    throw KernelUtils.logException("Kernel definition could not be found: " + osExtensionManifest.getAbsolutePath(),
                                                   "error.kernelDefFile", osExtensionName);
                }
                cache.checkEntry(osExtensionManifest, KernelResolverCache.FOLLOW_INCLUDES, repo);
            } else {
                osExtensionManifest = null;
            }

            cache.setFeatures(kernelFeatures);

            cleanStart |= cache.isDirty();

        } catch (LaunchException e) {
            cache.delete();
            throw e;
        }

        // If a cache file was available, but something changed,
        // we should use a framework clean start to ensure bundles
        // are properly re-loaded.
        this.forceCleanStart = cacheAvailable && cleanStart;
    }

    //

    private final KernelResolverCache cache;

    public KernelResolverCache getCache() {
        return cache;
    }

    //

    private final File kernelManifest;

    private final File logProviderManifest;
    private final String logProviderClass;

    private final File osExtensionManifest;

    public String getLogProvider() {
        return logProviderClass;
    }

    /**
     * Add URLs for resources specified for the kernel feature, for the log provider feature,
     * and optionally for the extension feature.
     *
     * @param Storage for the resource URLs.
     */
    public void addBootJars(List<URL> urls) {
        KernelUtils.appendURLs(cache.getJarFiles(kernelManifest), urls);

        KernelUtils.appendURLs(cache.getJarFiles(logProviderManifest), urls);

        if (osExtensionManifest != null) {
            KernelUtils.appendURLs(cache.getJarFiles(osExtensionManifest), urls);
        }
    }

    public String appendExtraSystemPackages(String packages) throws IOException {
        if (libertyBoot) {
            // for liberty boot we do not have real boot jars on disk to read
            packages = appendLibertyBootJarPackages(packages);

        } else {
            packages = appendExports(cache.getJarFiles(kernelManifest), packages);
            packages = appendExports(cache.getJarFiles(logProviderManifest), packages);
            if (osExtensionManifest != null) {
                packages = appendExports(cache.getJarFiles(osExtensionManifest), packages);
            }
        }

        return packages;
    }

    private static final String MANIFEST_EXPORT_PACKAGE = "Export-Package";

    private String appendLibertyBootJarPackages(String packages) throws IOException {
        for (KernelManifestElement element : cache.getManifestElements()) {
            Set<String> libertyBootSymbolicNames = new HashSet<String>();
            for (KernelBundleElement b : element.getBundleElements()) {
                // For boot.jar types liberty boot uses the LIBERTY_BOOT startlevel
                if (b.getStartLevel() == KernelStartLevel.LIBERTY_BOOT.getLevel()) {
                    libertyBootSymbolicNames.add(b.getSymbolicName());
                }
            }
            Collection<URL> bootManifestURLs = getLibertyBootManifests(libertyBootSymbolicNames);
            for (URL bootManifestURL : bootManifestURLs) {
                try {
                    Manifest manifest = new Manifest(bootManifestURL.openStream());
                    // Look for exported packages in manifest: append to value
                    String mPackages = manifest.getMainAttributes().getValue(MANIFEST_EXPORT_PACKAGE);
                    if (mPackages != null && !mPackages.isEmpty()) {
                        packages = (packages == null) ? mPackages : packages + "," + mPackages;
                    }
                } catch (IOException e) {
                    throw KernelUtils.logException(e,
                                                   "Exception loading log provider jar " + bootManifestURL + ", " + e,
                                                   "error.rasProviderResolve", bootManifestURL.toString());
                }
            }
        }
        return packages;
    }

    private static final String BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName";

    private Collection<URL> getLibertyBootManifests(Set<String> libertyBootSymbolicNames) throws IOException {
        Collection<URL> result = new ArrayList<URL>();
        ClassLoader cl = KernelResolver.class.getClassLoader();
        Enumeration<URL> classpathManifests = cl.getResources("META-INF/MANIFEST.MF");
        while (classpathManifests.hasMoreElements()) {
            URL manifestURL = classpathManifests.nextElement();
            Manifest manifest = new Manifest(manifestURL.openStream());
            Attributes attr = manifest.getMainAttributes();
            String bsn = attr.getValue(BUNDLE_SYMBOLICNAME);
            if (bsn != null) {
                String[] bsnElements = bsn.split(";");
                if (libertyBootSymbolicNames.contains(bsnElements[0])) {
                    result.add(manifestURL);
                }
            }
        }

        return result;
    }

    /**
     * Accumulate all of the packages exported by feature resource
     * jars.
     *
     * @param featureJars Feature resource jar files.
     * @param packages    Accumulated exported packages.
     *
     * @return The initial packages plus any added by the feature jars.
     */
    private String appendExports(List<File> featureJars, String packages) {
        for (File featureJar : featureJars) {
            String featureJarName = featureJar.getName();
            if (featureJarName.contains("org.eclipse.osgi")) {
                continue;
            }
            String mPackages = getExports(featureJar);
            if (mPackages != null) {
                packages = (packages == null) ? mPackages : packages + "," + mPackages;
            }
        }
        return packages;
    }

    /**
     * Retrieve the {@link #MANIFEST_EXPORT_PACKAGE} manifest
     * main attribute value from the target feature jar resource.
     *
     * @param featureJar A target feature jar.
     *
     * @return The manifest export package value from the manifest.
     *         Null if no export value is present as a manifest main
     *         attribute.
     */
    private static String getExports(File featureJar) {
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(featureJar);

            Manifest manifest = jarFile.getManifest();
            Attributes attrs = manifest.getMainAttributes();
            return attrs.getValue(MANIFEST_EXPORT_PACKAGE);

        } catch (IOException e) {
            throw KernelUtils.logException(e,
                                           "Exception loading log provider jar " + featureJar.getName() + ", " + e,
                                           "error.rasProviderResolve", featureJar.getName());

        } finally {
            Utils.tryToClose(jarFile);
        }
    }

    /**
     * Collect all of the bundle elements of the cached manifest elements.
     *
     * @return All of the bundle elements of the manifest elements.
     */
    public List<KernelBundleElement> getKernelBundles() {
        List<KernelBundleElement> bundleElements = new ArrayList<>();

        // Add all of the values from the nested bundle entry cache
        for (KernelManifestElement manifestElement : cache.getManifestElements()) {
            bundleElements.addAll(manifestElement.getBundleElements());
        }

        return bundleElements;
    }

    public void dispose() {
        cache.store();
        cache.dispose();
    }
}
