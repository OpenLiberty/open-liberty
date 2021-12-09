/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import com.ibm.ws.kernel.boot.Debug;
import com.ibm.ws.kernel.boot.LaunchException;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.provisioning.NameBasedLocalBundleRepository;
import com.ibm.ws.kernel.provisioning.VersionUtility;

/**
 *
 */
public class KernelResolver {

    public interface KernelBundleElement {
        String getSymbolicName();

        String getRangeString();

        String getLocation();

        File getCachedBestMatch();

        void setBestMatch(File bestMatch);

        int getStartLevel();

        String toNameVersionString();
    }

    public final static String CACHE_FILE = "platform/kernel.cache";
    private final static String LOG_PROVIDER = "WebSphere-LogProvider";
    private final static String SUBSYSTEM_CONTENT = "Subsystem-Content";
    private final static String INCLUDED_FILE = "osgi.subsystem.feature";
    private static final String MANIFEST_EXPORT_PACKAGE = "Export-Package";
    private static final String INUSE_KERNEL_FEATURES = "@=";

    private final static String TYPE = "type=";
    private final static String BOOT_JAR_TYPE = "boot.jar";
    private final static String BUNDLE_TYPE = "osgi.bundle";
    private final static String BUNDLE_LINE = "--|";
    private final static String LOCATION = "location:=";
    private final static String VERSION = "version=";
    private final static String START_PHASE = "start-phase:=";

    private final static String NL = System.getProperty("line.separator");
    private static final List<File> emptyList = Collections.emptyList();
    private static final String SPLIT_CHAR = ";";
    private static final Pattern splitPattern = Pattern.compile(SPLIT_CHAR);

    private final ResolverCache cache;
    private final String logProviderClass;
    private final File kernelMf;
    private final File logProviderMf;
    private final File osExtensionMf;
    private final boolean forceCleanStart;

    private final boolean libertyBoot;
    private static final String BUNDLE_SYMBOLICNAME = "Bundle-SymbolicName";

    /**
     * @param kernelDefName
     * @param logProviderName
     * @param osExtensionName
     */
    public KernelResolver(File installRoot, File cacheFile, String kernelDefName, String logProviderName, String osExtensionName) {
        this(installRoot, cacheFile, kernelDefName, logProviderName, osExtensionName, false);
    }

    /**
     * @param installRoot
     * @param workareaFile
     * @param kernelDefinition
     * @param logProviderDefinition
     * @param osExtensionDefinition
     * @param libertyBoot
     */
    public KernelResolver(File installRoot, File cacheFile, String kernelDefName, String logProviderName, String osExtensionName, boolean libertyBoot) {
        this.libertyBoot = libertyBoot;
        boolean cacheAvailable = cacheFile != null && cacheFile.exists();
        boolean cleanStart = false;

        // Try to load the cache file, if we can't, it isn't a dire event.
        cache = new ResolverCache(cacheFile, libertyBoot);
        cache.load();

        List<String> kernelFeatures = new ArrayList<String>();
        kernelFeatures.add(logProviderName);
        kernelFeatures.add(kernelDefName);
        if (osExtensionName != null)
            kernelFeatures.add(osExtensionName);

        // If the cache was populated, and something in the specified kernel features has changed,
        // we need to force a clean start..
        if (kernelFeaturesHaveChanged(cache.featuresInUse, kernelFeatures)) {
            // clean what we've read previously so we start over
            cache.dispose();
            cleanStart = true;
        }

        try {
            // Use a simple repo for bootstrap purposes.
            NameBasedLocalBundleRepository repo = new NameBasedLocalBundleRepository(installRoot);

            File platformDir = new File(installRoot, "lib/platform");

            // Make sure we can find the log provider
            if (logProviderName == null || logProviderName.trim().length() == 0)
                logThrowLaunchException(new LaunchException("Log provider definition not found (Tr/FFDC)", BootstrapConstants.messages.getString("error.rasProvider")));

            logProviderMf = new File(platformDir, logProviderName + ".mf");
            if (!logProviderMf.exists())
                logThrowLaunchException(new LaunchException("Kernel definition could not be found: "
                                                            + logProviderMf.getAbsolutePath(), MessageFormat.format(BootstrapConstants.messages.getString("error.kernelDefFile"),
                                                                                                                    logProviderName)));

            // The log provider also most specify the class we should be creating so we have logging!
            ManifestCacheElement entry = cache.checkEntry(logProviderMf, true, repo);
            logProviderClass = entry.getLogProviderClass();
            if (logProviderClass == null)
                logThrowLaunchException(new LaunchException("A log provider implementation was not defined", BootstrapConstants.messages.getString("error.rasProvider")));

            // Make sure we can find the kernel definition
            if (kernelDefName == null || kernelDefName.trim().length() == 0)
                logThrowLaunchException(new LaunchException("Could not find kernel definition", BootstrapConstants.messages.getString("error.kernelDef")));

            kernelMf = new File(platformDir, kernelDefName + ".mf");
            if (!kernelMf.exists())
                logThrowLaunchException(new LaunchException("Kernel definition could not be found: "
                                                            + kernelMf.getAbsolutePath(), MessageFormat.format(BootstrapConstants.messages.getString("error.kernelDefFile"),
                                                                                                               kernelDefName)));
            cache.checkEntry(kernelMf, false, repo);

            // If we have an os extension to work with, make sure we can find it, too
            if (osExtensionName != null) {
                osExtensionMf = new File(platformDir, osExtensionName + ".mf");
                if (!osExtensionMf.exists())
                    logThrowLaunchException(new LaunchException("Kernel definition could not be found: "
                                                                + osExtensionMf.getAbsolutePath(), MessageFormat.format(BootstrapConstants.messages.getString("error.kernelDefFile"),
                                                                                                                        osExtensionName)));

                cache.checkEntry(osExtensionMf, true, repo);
            } else {
                osExtensionMf = null;
            }

            cleanStart |= cache.isDirty;
            cache.featuresInUse = kernelFeatures;
        } catch (LaunchException e) {
            cache.delete();
            throw e; // re-throw
        }

        // If a cache file was available, but something changed, we should use a framework clean
        // start to ensure bundles are properly re-loaded.
        forceCleanStart = cacheAvailable && cleanStart;
    }

    /**
     * @param featuresInUse
     * @param kernelFeatures
     * @return
     */
    private boolean kernelFeaturesHaveChanged(List<String> featuresInUse, List<String> kernelFeatures) {
        // If the list from the cache is empty, then the cache was empty, so we don't have to start clean
        // as we likely already are...
        if (featuresInUse.isEmpty())
            return false;

        if (featuresInUse.size() == kernelFeatures.size()) {
            // return true if the contents of the list do not match, not paying attention to order..
            return !featuresInUse.containsAll(kernelFeatures);
        }
        return true;
    }

    private void logThrowLaunchException(LaunchException e) {
        Debug.printStackTrace(e);
        throw e;
    }

    /**
     * @return true if the OSGi framework should be started cleanly
     *         to ensure new/changed bundles are properly detected and installed
     */
    public boolean getForceCleanStart() {
        return forceCleanStart;
    }

    /**
     * @return
     */
    public String getLogProvider() {
        return logProviderClass;
    }

    /**
     * Add any boot.jar resources specified by the kernel, log provider, or os extension definition files
     *
     * @param urlList target list for additional URLs.
     * @see ResolverCache#getJarFiles(File)
     */
    public void addBootJars(List<URL> urlList) {
        addBootJars(cache.getJarFiles(kernelMf), urlList);
        addBootJars(cache.getJarFiles(logProviderMf), urlList);

        if (osExtensionMf != null)
            addBootJars(cache.getJarFiles(osExtensionMf), urlList);
    }

    /**
     * Given the list of files, add the URL for each file to the list of URLs
     *
     * @param jarFiles List of Files (source)
     * @param urlList List of URLs (target)
     */
    private void addBootJars(List<File> jarFiles, List<URL> urlList) {
        for (File jarFile : jarFiles) {
            try {
                urlList.add(jarFile.toURI().toURL());
            } catch (MalformedURLException e) {
                // Unlikely: we're making URLs for files we know exist
            }
        }
    }

    public String appendExtraSystemPackages(String packages) throws IOException {
        if (libertyBoot) {
            // for liberty boot we do not have real boot jars on disk to read
            packages = appendLibertyBootJarPackages(packages);
        } else {
            packages = appendExtraSystemPackages(cache.getJarFiles(kernelMf), packages);
            packages = appendExtraSystemPackages(cache.getJarFiles(logProviderMf), packages);
            if (osExtensionMf != null)
                packages = appendExtraSystemPackages(cache.getJarFiles(osExtensionMf), packages);
        }
        return packages;
    }

    private String appendLibertyBootJarPackages(String packages) throws IOException {
        for (ManifestCacheElement element : cache.getManifestElements()) {
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
                    throw new LaunchException("Exception loading log provider jar " + (bootManifestURL) + ", "
                                              + e, MessageFormat.format(BootstrapConstants.messages.getString("error.rasProviderResolve"),
                                                                        bootManifestURL.toString()), e);
                }
            }
        }
        return packages;
    }

    /**
     * @param libertyBootSymbolicNames
     * @return
     * @throws IOException
     */
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

    private String appendExtraSystemPackages(List<File> files, String packages) {
        for (File file : files) {
            // skip the OSGi framework bundle itself
            if (file.getName().contains("org.eclipse.osgi"))
                continue;

            JarFile jarFile = null;
            Manifest manifest = null;
            Attributes attrs = null;
            try {
                jarFile = new JarFile(file);
                manifest = jarFile.getManifest();
                attrs = manifest.getMainAttributes();

                // Look for exported packages in manifest: append to value
                String mPackages = attrs.getValue(MANIFEST_EXPORT_PACKAGE);
                if (mPackages != null && !mPackages.isEmpty()) {
                    packages = (packages == null) ? mPackages : packages + "," + mPackages;
                }
            } catch (IOException e) {
                throw new LaunchException("Exception loading log provider jar " + (jarFile != null ? jarFile.getName() : "null") + ", "
                                          + e, MessageFormat.format(BootstrapConstants.messages.getString("error.rasProviderResolve"),
                                                                    (jarFile != null ? jarFile.getName() : "null")), e);
            } finally {
                Utils.tryToClose(jarFile);
            }
        }
        return packages;
    }

    /**
     * @return A list of all of the kernel bundles
     */
    public List<KernelBundleElement> getKernelBundles() {
        List<KernelBundleElement> elements = new ArrayList<KernelBundleElement>();

        // Add all of the values from the nested bundle entry cache
        for (ManifestCacheElement mEntry : cache.getManifestElements()) {
            elements.addAll(mEntry.getBundleElements());
        }

        return elements;
    }

    /**
     * Store the cache contents to disk and clear the maps so the storage can be reclaimed
     */
    public void dispose() {
        cache.store();
        cache.dispose();
    }

    /**
     * The internal cache for what the kernel uses. The outer is a map: kernel-manifest-name to an entry that
     * stores information about what that manifest contained. We have a few *.mf files comprising the kernel:
     * the kernel "core", log provider, and os extensions. The log provider and os extension manifests can include
     * other manifests (e.g. the definition for hpel includes the definition for default logging).
     */
    private static class ResolverCache {
        private final Map<String, ManifestCacheElement> cacheEntries = new LinkedHashMap<String, ManifestCacheElement>();
        private final File cacheFile;
        private final boolean libertyBoot;

        private List<String> featuresInUse = Collections.emptyList();
        private boolean isDirty = false;

        ResolverCache(File cacheFile, boolean libertyBoot) {
            this.cacheFile = cacheFile;
            this.libertyBoot = libertyBoot;
        }

        /**
         * Try to pre-load from the cache file, if we can.
         */
        @SuppressWarnings("resource")
        public void load() {
            if (cacheFile != null && cacheFile.exists()) {
                String line;
                BufferedReader reader = null;
                try {
                    ManifestCacheElement currentEntry = null;

                    // loop through each line of the file... The cache file should contain
                    // something like:
                    //   kernelManifest-1.0=kernel;manifest;information
                    //   --|kernelManifest-1.0|bundle.symbolic.name....
                    // It is expected that the manifest line will precede the lines with the bundle..
                    reader = new BufferedReader(new InputStreamReader(new FileInputStream(cacheFile), StandardCharsets.UTF_8));
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith(BUNDLE_LINE)) {
                            // If line starts with --|, this is a line describing a kernel bundle
                            if (currentEntry == null) {
                                throw new IOException("Cache file contents corrupted");
                            } else {
                                // --|kernelCore-1.0|....
                                line = line.substring(BUNDLE_LINE.length()); // skip --|
                                if (line.startsWith(currentEntry.mfSymbolicName)) {
                                    // skip kernelCore-1.0|
                                    line = line.substring(currentEntry.mfSymbolicName.length() + 1);

                                    // The rest of the line is a bundle entry..
                                    BundleCacheElement bEntry = new BundleCacheElement(this, line);
                                    currentEntry.addBundleEntry(bEntry.getSymbolicName(), bEntry);
                                } else {
                                    throw new IOException("Cache file contents corrupted");
                                }
                            }
                        } else if (line.startsWith(INUSE_KERNEL_FEATURES)) {
                            String[] parts = line.substring(INUSE_KERNEL_FEATURES.length()).split(";");
                            if (parts.length > 0)
                                featuresInUse = Arrays.asList(parts);
                            else
                                featuresInUse = Collections.emptyList();
                        } else {
                            currentEntry = new ManifestCacheElement(line);
                            cacheEntries.put(currentEntry.mfSymbolicName, currentEntry);
                        }
                    }
                } catch (IOException e) {
                    // no cache. BOO. Start clean if something happened halfway through the read
                    cacheEntries.clear();
                } finally {
                    Utils.tryToClose(reader);
                }
            }
        }

        /**
         * Write out the cache file
         */
        public void store() {
            if (cacheFile != null && isDirty) {
                PrintWriter writer = null;
                try {
                    boolean parentExists = true;

                    File parent = cacheFile.getParentFile();
                    if (!parent.exists())
                        parentExists = parent.mkdirs();

                    if (!parentExists)
                        throw new IOException("Unable to create parent(s) of file " + cacheFile.getAbsolutePath());

                    writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(cacheFile), StandardCharsets.UTF_8));
                    for (ManifestCacheElement entry : cacheEntries.values()) {
                        entry.write(writer);
                        writer.write(NL);
                        for (BundleCacheElement bEntry : entry.getBundleElements()) {
                            writer.write(BUNDLE_LINE); // bundle line marker
                            writer.write(entry.mfSymbolicName); // sanity check
                            writer.write('|');
                            bEntry.write(writer);
                            writer.write(NL);
                        }
                    }
                    writer.write(INUSE_KERNEL_FEATURES);
                    boolean isFirst = true;
                    for (String s : featuresInUse) {
                        if (isFirst) {
                            isFirst = false;
                        } else {
                            writer.write(';');
                        }
                        writer.write(s);
                    }
                    writer.write(NL);
                    writer.flush();
                    isDirty = false;
                } catch (IOException e) {
                    System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("warning.noPlatformCache"),
                                                            cacheFile.getName(),
                                                            e));
                } finally {
                    Utils.tryToClose(writer);
                }
            }
        }

        public void delete() {
            if (cacheFile != null && cacheFile.exists()) {
                cacheFile.delete();
            }
        }

        /**
         * Check each cache entry: if it exists, and the mf file hasn't altered
         * since we last read it, keep what we have.
         * Otherwise, SKIM the *.mf file looking either for a LogProvider header,
         * an included subsystem, an "osgi.bundle", or a "boot.jar".
         * <p>
         * This is rough-and-dirty parsing: we aren't using a proper manifest
         * parser because we need one that isn't sensitive to the traditional
         * manifest's line-wrapping constraints, and we are about to bootstrap an
         * environment that has a plethora of them.
         *
         * @param mfFile *.mf file to retrieve boot.jar files from. The returned
         *            list will include any elements provided by an included *.mf file.
         */
        public ManifestCacheElement checkEntry(File mfFile, boolean followIncludes, NameBasedLocalBundleRepository repo) {
            ManifestCacheElement entry = cacheEntries.get(mfFile.getName());
            if (entry != null && entry.isUsable(mfFile, this)) {
                return entry;
            } else {
                isDirty = true;

                // No cache, so we start fresh...  *sigh*
                String line;
                boolean inSubsystemContent = false;
                BufferedReader reader = null;
                String logProvider = null;
                List<File> jarList = new ArrayList<File>();
                List<File> fileList = new ArrayList<File>();
                Map<String, BundleCacheElement> bundleEntries = new LinkedHashMap<String, BundleCacheElement>();

                try {
                    reader = new BufferedReader(new InputStreamReader(new FileInputStream(mfFile)));
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty() || line.startsWith("#")) {
                            inSubsystemContent = false; // we're not in subsystem content anymore
                            continue;
                        } else if (line.startsWith(LOG_PROVIDER) && logProvider == null) {
                            // WebSphere-LogProvider: com.ibm.ws.logging.internal.hpel.HpelLogProviderImpl
                            if (line.length() > LOG_PROVIDER.length() + 1) {
                                logProvider = line.substring(LOG_PROVIDER.length() + 1).trim();
                            }
                            continue; // ONWARDS TO NEXT LINE...
                        } else if (line.startsWith(SUBSYSTEM_CONTENT)) {
                            // If this line starts with the SubsystemContent header, trim it
                            // off and fall down into the next sections..
                            line = line.substring(SUBSYSTEM_CONTENT.length() + 1).trim();
                            inSubsystemContent = true;
                        } else {
                            line = line.trim();
                        }

                        if (inSubsystemContent) {
                            if (!line.endsWith(",")) {
                                inSubsystemContent = false; // we're not in subsystem content anymore
                            }

                            if (line.contains(BOOT_JAR_TYPE)) {

                                if (libertyBoot) {
                                    // Marking these as start-phase LIBERTY_BOOT to indicate that they are not really bundles
                                    // com.ibm.ws.logging; version="[1,1.0.100)"; type="boot.jar"; start-phase:=LIBERTY_BOOT
                                    SubsystemContentElement element = new SubsystemContentElement(line + "; start-phase:=LIBERTY_BOOT");

                                    // this might throw an IllegalArgumentException (unknown start phase)
                                    BundleCacheElement cacheElement = new BundleCacheElement(this, element);

                                    // Use the symbolic name as a key for the new element
                                    bundleEntries.put(element.symbolicName, cacheElement);

                                } else {

                                    // com.ibm.ws.logging; version="[1,1.0.100)"; type="boot.jar"
                                    SubsystemContentElement element = new SubsystemContentElement(line);

                                    // Now we do the work to find the right jar given the version range.
                                    // We're very very low-level here, so we're using a straight-up naming
                                    // convention to find the jars we have to choose from. Using a
                                    // ContentBasedLocalBundleRepository comes in the next layer, when
                                    // we're finding the kernel bundles.
                                    File bestMatchFile = repo.selectBundle(element.symbolicName,
                                                                           VersionUtility.stringToVersionRange(element.vrangeString));
                                    if (bestMatchFile == null) {
                                        throw new LaunchException("Could not find bundle for " + element
                                                                  + ".", BootstrapConstants.messages.getString("error.missingBundleException"));
                                    } else {
                                        // Add to the list of boot jars...
                                        jarList.add(bestMatchFile);
                                    }
                                }
                            } else if (followIncludes && line.contains(INCLUDED_FILE)) {
                                // com.ibm.websphere.appserver.logging-1.0; location="lib/platform/defaultLogging-1.0.mf"; type="osgi.subsystem.feature"
                                SubsystemContentElement element = new SubsystemContentElement(line);

                                File includedManifest = null;
                                if (element.location != null)
                                    includedManifest = new File(repo.getRootDirectory(), element.location);

                                if (includedManifest != null && includedManifest.exists()) {
                                    ManifestCacheElement included = checkEntry(includedManifest, followIncludes, repo);

                                    // Add to our list of included files
                                    fileList.add(includedManifest);

                                    // add jar files from the included entry
                                    if (included.bootJarList != null)
                                        jarList.addAll(included.bootJarList);

                                    // add included files from the included entry
                                    if (included.includedFileList != null)
                                        fileList.addAll(included.includedFileList);
                                }
                            } else if (line.contains(BUNDLE_TYPE) || !line.contains(TYPE)) {
                                // the default subsystem content type is "osgi.bundle".. so if we don't have a type,
                                // we assume it's one of these.

                                // com.ibm.ws.org.objectweb.asm.all.4.0; version="[1,1.0.100)"; start-phase:=BOOTSTRAP,
                                SubsystemContentElement element = new SubsystemContentElement(line);

                                // this might throw an IllegalArgumentException (unknown start phase)
                                BundleCacheElement cacheElement = new BundleCacheElement(this, element);

                                // Use the symbolic name as a key for the new element
                                bundleEntries.put(element.symbolicName, cacheElement);
                            }
                        }
                    }

                    // Create the new manifest cache entry with what we've read...
                    entry = new ManifestCacheElement(mfFile, jarList, fileList, logProvider, bundleEntries);

                    // Add the information we've gathered to the cache
                    cacheEntries.put(mfFile.getName(), entry);
                    return entry;
                } catch (IOException e) {
                    throw new LaunchException("Kernel definition could not be read: "
                                              + mfFile.getAbsolutePath(), MessageFormat.format(BootstrapConstants.messages.getString("error.unknownException"), e));
                } finally {
                    Utils.tryToClose(reader);
                }
            }
        }

        public List<File> getJarFiles(File mfFile) {
            if (mfFile != null) {
                ManifestCacheElement entry = cacheEntries.get(mfFile.getName());
                if (entry != null) {
                    return entry.bootJarList;
                }
            }
            return Collections.emptyList();
        }

        private ManifestCacheElement get(File f) {
            return cacheEntries.get(f.getName());
        }

        public Collection<ManifestCacheElement> getManifestElements() {
            return cacheEntries.values();
        }

        public void dispose() {
            for (ManifestCacheElement element : cacheEntries.values()) {
                element.dispose();
            }
            cacheEntries.clear();
        }
    }

    private static final class ManifestCacheElement {

        private final Map<String, BundleCacheElement> bundleEntries;

        /** The name of this *.mf file */
        private final String mfSymbolicName;

        /** THe absolute path of this *.mf file */
        private final String location;

        /** When this *.mf was last modified */
        private final long lastModified;

        /** The size of this *.mf file */
        private final long fileSize;

        /** The list of boot.jars required */
        private final List<File> bootJarList;

        /** Included resources */
        private final List<File> includedFileList;

        /** A log provider class, if specified */
        private final String logProviderClass;

        ManifestCacheElement(File newFile, List<File> jarFiles, List<File> includedFiles, String lpClass, Map<String, BundleCacheElement> bundleEntries) {
            this.mfSymbolicName = newFile.getName();
            this.location = newFile.getAbsolutePath();
            this.lastModified = newFile.lastModified();
            this.fileSize = newFile.length();
            this.bootJarList = jarFiles;
            this.includedFileList = includedFiles;
            this.logProviderClass = lpClass;
            this.bundleEntries = bundleEntries;
        }

        ManifestCacheElement(String str) {
            int index = str.indexOf('=');
            mfSymbolicName = str.substring(0, index);

            String[] parts = splitPattern.split(str.substring(index + 1));
            location = (parts.length > 0) ? parts[0] : null;
            lastModified = (parts.length > 1) ? Long.parseLong(parts[1]) : -1;
            fileSize = (parts.length > 2) ? Long.parseLong(parts[2]) : -1;
            bootJarList = (parts.length > 3) ? toFileList(parts[3]) : emptyList;
            includedFileList = (parts.length > 4) ? toFileList(parts[4]) : emptyList;
            logProviderClass = (parts.length > 5) ? parts[5] : null;

            bundleEntries = new LinkedHashMap<String, BundleCacheElement>();
        }

        void addBundleEntry(String symbolicName, BundleCacheElement value) {
            bundleEntries.put(symbolicName, value);
        }

        void write(PrintWriter writer) {
            writer.write(mfSymbolicName);
            writer.write('=');
            writer.write(location);
            writer.write(SPLIT_CHAR);
            writer.write(String.valueOf(lastModified));
            writer.write(SPLIT_CHAR);
            writer.write(String.valueOf(fileSize));
            writer.write(SPLIT_CHAR);
            writer.write(fileListToString(bootJarList));
            writer.write(SPLIT_CHAR);
            writer.write(fileListToString(includedFileList));
            writer.write(SPLIT_CHAR);
            writer.write(logProviderClass == null ? "" : logProviderClass);
        }

        /**
         * @return the log provider class read from this manifest
         */
        public String getLogProviderClass() {
            return logProviderClass;
        }

        /**
         * @return true if the value for this cache entry can be used
         *         because the file it was based on hasn't changed
         */
        public boolean isUsable(File compareFile, ResolverCache cache) {
            return location.equals(compareFile.getAbsolutePath())
                   && lastModified == compareFile.lastModified()
                   && fileSize == compareFile.length()
                   && usableIncludes(cache);
        }

        /**
         * If this file includes other files.. check to make sure they are
         * still usable as well.
         *
         * @return true if we can use what we know...
         */
        private boolean usableIncludes(ResolverCache cache) {
            boolean usable = true;
            for (File f : includedFileList) {
                ManifestCacheElement entry = cache.get(f);
                if (!f.exists() || entry == null)
                    return false;
                usable |= entry.isUsable(f, cache);
            }
            return usable;
        }

        /**
         * @param files List of Files
         * @return Comma-separated string
         */
        private String fileListToString(List<File> files) {
            if (files == null || files.isEmpty())
                return "";

            StringBuilder s = new StringBuilder();
            for (File f : files) {
                s.append(f.getAbsolutePath()).append(",");
            }
            return s.substring(0, s.length() - 1);
        }

        /**
         * @param str Comma-separated string
         * @return List of Files
         */
        private List<File> toFileList(String str) {
            if (str != null && !str.isEmpty()) {
                String[] parts = str.split(",");
                if (parts.length > 0) {
                    List<File> list = new ArrayList<File>(parts.length);
                    for (String s : parts) {
                        if (!s.isEmpty()) {
                            list.add(new File(s));
                        }
                    }
                    return list;
                }
            }
            return Collections.emptyList();
        }

        public Collection<BundleCacheElement> getBundleElements() {
            return bundleEntries.values();
        }

        public void dispose() {
            bundleEntries.clear();
        }
    }

    /**
     * Element representing a kernel bundle read from the kernel manifest
     */
    private static class BundleCacheElement implements KernelBundleElement {
        /** The name of this *.mf file */
        private final SubsystemContentElement element;
        private final int startLevel;
        private final ResolverCache cache;
        private File bestMatchFile;

        /**
         * @param cache Owning cache
         * @param element SubsystemContentElement read from the kernel manifest
         * @throws IOException If the value read from the cache is an invalid start phase
         */
        public BundleCacheElement(ResolverCache cache, SubsystemContentElement element) throws IOException {
            this.cache = cache;
            this.element = element;
            this.bestMatchFile = null;

            if (element.startPhase != null) {
                try {
                    // throw IllegalArgumentException on mismatch
                    KernelStartLevel level = KernelStartLevel.valueOf(element.startPhase);
                    this.startLevel = level.getLevel();
                } catch (IllegalArgumentException e) {
                    throw new IOException("Invalid value for start phase " + element.startPhase);
                }
            } else {
                this.startLevel = KernelStartLevel.ACTIVE.startLevel; // default
            }
        }

        /**
         * @param cache Owning cache
         * @param str String from the kernel feature manifest
         * @throws IOException
         */
        BundleCacheElement(ResolverCache cache, String str) throws IOException {
            this.cache = cache;
            int index = str.indexOf('|');
            element = new SubsystemContentElement(str.substring(0, index));
            bestMatchFile = (index + 1 < str.length()) ? new File(str.substring(index + 1)) : null;

            if (element.startPhase != null) {
                try {
                    // throw IllegalArgumentException on mismatch
                    KernelStartLevel level = KernelStartLevel.valueOf(element.startPhase);
                    this.startLevel = level.startLevel;
                } catch (IllegalArgumentException e) {
                    throw new IOException("cache invalid or out of sync, invalid value for start phase " + element.startPhase);
                }
            } else {
                this.startLevel = KernelStartLevel.ACTIVE.startLevel;
            }
        }

        /**
         * @param writer
         */
        public void write(PrintWriter writer) {
            writer.write(element.toString());
            writer.write('|');
            writer.write(bestMatchFile == null ? "" : bestMatchFile.getAbsolutePath());
        }

        /**
         * @return the nested SubsystemContentElement's symbolic name
         */
        @Override
        public String getSymbolicName() {
            return element.symbolicName;
        }

        @Override
        public String getRangeString() {
            return element.vrangeString;
        }

        @Override
        public String getLocation() {
            return element.location;
        }

        @Override
        public File getCachedBestMatch() {
            return bestMatchFile;
        }

        @Override
        public void setBestMatch(File f) {
            bestMatchFile = f;
            cache.isDirty = true; // mark parent dirty
        }

        @Override
        public int getStartLevel() {
            return startLevel;
        }

        /**
         * Re-stringify the subsystem content element: this won't compare exactly
         * to what was in the original manifest, we may have dropped something we
         * don't care about.
         */
        @Override
        public String toString() {
            return element.toString()
                   + (bestMatchFile == null ? "" : ";path=\"" + bestMatchFile.getAbsolutePath() + '"');
        }

        @Override
        public String toNameVersionString() {
            return element.symbolicName + ";" + VERSION + '"' + element.vrangeString + '"';
        }
    }

    /**
     * This represents one line in the Subsystem-Content of a feature manifest, it stores
     * only the attributes we care about, and does no validation.
     */
    static class SubsystemContentElement {
        final String symbolicName;
        final String type;
        final String location;
        final String vrangeString;
        final String startPhase;

        /**
         * Parse a subsystem content element. Assumes leading/trailing spaces have been trimmed
         *
         * <pre>
         * org.apache.aries.util; version="[1,1.0.100)"; type="boot.jar"
         * com.ibm.wsspi.org.osgi.cmpn; location="dev/spi/spec/"; version="[5.0, 5.1)"; start-phase:=BOOTSTRAP
         * </pre>
         *
         * @param line
         */
        SubsystemContentElement(String line) {
            String name = null;
            String type = null;
            String version = null;
            String location = null;
            String startPhase = null;

            if (line.endsWith(",")) {
                line = line.substring(0, line.length() - 1);
            }

            String[] parts = line.split(";");
            if (parts.length > 1) {
                name = parts[0].trim();
                for (int i = 1; i < parts.length; i++) {
                    String part = parts[i].trim();
                    if (part.startsWith(TYPE)) {
                        type = part.substring(TYPE.length());
                    } else if (part.startsWith(VERSION)) {
                        version = part.substring(VERSION.length());
                    } else if (part.startsWith(LOCATION)) {
                        location = part.substring(LOCATION.length());
                    } else if (part.startsWith(START_PHASE)) {
                        startPhase = part.substring(START_PHASE.length());
                    }
                }
            }

            this.symbolicName = name;
            this.type = stripQuotes(type);
            this.location = stripQuotes(location);
            this.vrangeString = stripQuotes(version);
            this.startPhase = stripQuotes(startPhase);
        }

        /**
         * Strip the quotes from around attributes. Given "boot.jar" will return boot.jar.
         *
         * @param in
         * @return
         */
        private String stripQuotes(String in) {
            if (in != null && in.length() > 1) {
                if (in.length() > 1 && in.startsWith("\"") && in.endsWith("\"")) {
                    return in.substring(1, in.length() - 1);
                }
            }
            return in;
        }

        /**
         * Re-stringify the subsystem content element: this won't compare exactly
         * to what was in the original manifest, we may have dropped something we
         * don't care about.
         */
        @Override
        public String toString() {
            String result = symbolicName + ";"
                            + (vrangeString == null ? "" : VERSION + '"' + vrangeString + "\";")
                            + (location == null ? "" : LOCATION + '"' + location + "\";")
                            + (type == null ? "" : TYPE + '"' + type + "\";")
                            + (startPhase == null ? "" : START_PHASE + '"' + startPhase + "\";");
            return result.substring(0, result.length() - 1);
        }
    }
}
