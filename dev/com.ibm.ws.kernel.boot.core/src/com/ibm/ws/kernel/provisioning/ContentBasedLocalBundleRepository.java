/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.provisioning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.boot.cmdline.Utils;

/**
 * This class is used to select bundles from the runtime. It is primarily designed to be called by the feature
 * manager, but it is required in other places so it is in the kernel boot to allow sharing. This class implements
 * the service rules for bundles. The process will choose the bundle by first choosing the newest iFix upon the latest
 * major.minor.micro versioned bundle. Given bundles with the following symbolic names and versions:
 * <ol>
 * <li>a.b/1.0.0</li>
 * <li>a.b/1.0.1.v1</li>
 * <li>a.b/1.0.2.v2</li>
 * </ol>
 * 
 * It will choose the middle option. a.b/1.0.2.v2 is an iFix on version 1.0.2 of the bundle, but 1.0.2 does not exist, so
 * the iFix is not for this installation. a.b/1.0.0 is ignored because we have an iFix.
 * 
 * Instances of this object are NOT thread safe. Two instances MUST not be used at once accessing the same cache file.
 */
public class ContentBasedLocalBundleRepository extends AbstractResourceRepository {
    private final File _installDir;
    private final File _cacheFile;
    private final String _defaultLocation;
    private final ConcurrentMap<String, List<Resource>> _cacheBySymbolicName = new ConcurrentHashMap<String, List<Resource>>();
    private final Set<File> _bundleLocations = new HashSet<File>();
    private final Set<String> _locations = new HashSet<String>();
    private boolean _dirty;
    private final Messages _msgs;
    private boolean _cacheRead;

    /**
     * This class represents information about a Bundle. It stores the file on disk, the bundle version,
     * bundle symbolic name, base location and the lastUpdated timestamp and the size.
     */
    private final class BundleInfo extends Resource {
        private final File file;
        private final Version version;
        private final String symbolicName;
        private final long lastUpdated;
        private final long size;
        private final String baseLocation;
        private final boolean isFix;

        @Override
        protected File getFile() {
            return file;
        }

        @Override
        protected Version getVersion() {
            return version;
        }

        @Override
        protected String getBaseLocation() {
            return baseLocation;
        }

        @Override
        protected boolean isFix() {
            return isFix;
        }

        @Override
        protected String getSymbolicName() {
            return symbolicName;
        }

        /**
         * This constructor cracks open the bundle and configures this Bundle Info based on the bundle manifest
         * 
         * @param f the bundle file
         * @param baseLoc the base loc for the file
         * @throws IOException
         */
        public BundleInfo(File f, String baseLoc) throws IOException {
            file = f;
            baseLocation = baseLoc;
            this.size = file.length();
            this.lastUpdated = file.lastModified();
            JarFile jar = new JarFile(file);
            Manifest man = jar.getManifest();
            Attributes a = man.getMainAttributes();
            String bsn = getSymbolicName(a);
            symbolicName = (bsn == null) ? null : bsn.trim();
            try {
                version = Version.parseVersion(a.getValue(Constants.BUNDLE_VERSION));
                Object iFixHeader = a.getValue("IBM-Interim-Fixes");
                Object tFixHeader = a.getValue("IBM-Test-Fixes");
                isFix = (iFixHeader != null || tFixHeader != null);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid " + Constants.BUNDLE_VERSION + " in " + f + ": " + e.getMessage(), e);
            } finally {
                Utils.tryToClose(jar);
            }
        }

        /**
         * This method reads a line for the cache. If it determines the cache is out of date it re-reads the
         * data from the manifest. The cache format is
         * 
         * <p>&lt;symbolic name&gt;&lt;version&gt;&lt;;&gt;is a fix jar as boolean&lt;last update timestamp&gt;;&lt;bundle
         * file size&gt;;&lt;base location&gt;;&ltabsolute file location&gt;</p>
         * 
         * This constructor does not use split(";") to process the data because it is less efficient than just using lastIndexOf(';') to parse it.
         * When I say more efficient it saves about 150ms on 1k iterations so it is a pretty minor improvement, but since I wrote it like this and
         * it is a little faster I thought I would stick with it.
         * 
         * @param cacheLine the cache line
         * @throws IOException
         */
        public BundleInfo(String cacheLine) throws IOException {
            int beginIndex = cacheLine.lastIndexOf(';');

            file = new File(cacheLine.substring(beginIndex + 1));

            int endIndex = beginIndex;
            beginIndex = cacheLine.lastIndexOf(';', endIndex - 1);
            baseLocation = cacheLine.substring(beginIndex + 1, endIndex);
            endIndex = beginIndex;
            beginIndex = cacheLine.lastIndexOf(';', endIndex - 1);
            long size = Long.parseLong(cacheLine.substring(beginIndex + 1, endIndex));
            endIndex = beginIndex;
            beginIndex = cacheLine.lastIndexOf(';', endIndex - 1);
            long lastUpdated = Long.parseLong(cacheLine.substring(beginIndex + 1, endIndex));

            this.size = file.length();
            this.lastUpdated = file.lastModified();

            if (this.size == size && this.lastUpdated == lastUpdated) {
                endIndex = beginIndex;
                beginIndex = cacheLine.lastIndexOf(';', endIndex - 1);
                isFix = Boolean.parseBoolean(cacheLine.substring(beginIndex + 1, endIndex));
                endIndex = beginIndex;
                beginIndex = cacheLine.lastIndexOf(';', endIndex - 1);
                version = Version.parseVersion(cacheLine.substring(beginIndex + 1, endIndex));
                endIndex = beginIndex;
                beginIndex = cacheLine.lastIndexOf(';', endIndex - 1);
                symbolicName = cacheLine.substring(beginIndex + 1, endIndex);
            } else {
                Manifest man;
                try (JarFile jar = new JarFile(file)) {
                    man = jar.getManifest();
                }
                Attributes a = man.getMainAttributes();
                symbolicName = getSymbolicName(a);
                version = Version.parseVersion(a.getValue(Constants.BUNDLE_VERSION));
                Object iFixHeader = a.get("IBM-Interim-Fixes");
                Object tFixHeader = a.get("IBM-Test-Fixes");
                isFix = (iFixHeader != null || tFixHeader != null);
            }
        }

        /**
         * The Bundle Symbolic name from the header can take attributes and directives so
         * we need to strip that out if present.
         * 
         * @param attributes The attributes in the manifest header.
         * @return the symbolic name minus any attributes.
         */
        private String getSymbolicName(Attributes attributes) {
            String value = attributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
            int index = (value != null) ? value.indexOf(';') : -1;
            return (index > 0) ? value.substring(0, index).trim() : value;
        }

        /**
         * This method writes the file out in cache format. This is documented in the constructor.
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();

            builder.append(symbolicName);
            builder.append(';');
            builder.append(version);
            builder.append(';');
            builder.append(isFix);
            builder.append(';');
            builder.append(lastUpdated);
            builder.append(';');
            builder.append(size);
            builder.append(';');
            builder.append(baseLocation);
            builder.append(';');
            builder.append(file.getAbsolutePath());

            return builder.toString();
        }
    }

    private static final class NullMessages implements Messages {
        @Override
        public void warning(String key, Object... inserts) {}
    }

    public static class TrMessages implements Messages {
        private static final com.ibm.websphere.ras.TraceComponent _tc = com.ibm.websphere.ras.Tr.register(TrMessages.class, "bootstrap",
                                                                                                          "com.ibm.ws.kernel.boot.resources.LauncherMessages");

        @Override
        public void warning(String key, Object... inserts) {
            com.ibm.websphere.ras.Tr.warning(_tc, key, inserts);
        }
    }

    /**
     * Create the repository. The installDir points to the place wlp is installed to. The assumption is
     * that features are installed in the child lib/features dir or extension/lib/features dir.
     * The cache File may be null, but if it is
     * provided the cache will be used to optimize multiple calls across JVM restarts.
     * 
     * @param installDir The product install dir
     * @param cacheFile The location to store cached information.
     * @param msgs true if messages should be output to the log, false otherwise.
     */
    public ContentBasedLocalBundleRepository(File installDir, File cacheFile, boolean msgs) {
        _installDir = installDir.getAbsoluteFile();
        _msgs = msgs ? new TrMessages() : new NullMessages();
        _cacheFile = cacheFile;
        _defaultLocation = "lib/";

        includeBaseLocation(_defaultLocation);
    }

    @Override
    protected File getRootDirectory() {
        return _installDir;
    }

    @Override
    protected String getDefaultBaseLocation() {
        return _defaultLocation;
    }

    @Override
    protected List<Resource> getResourcesBySymbolicName(String symbolicName) {
        return _cacheBySymbolicName.get(symbolicName);
    }

    @Override
    protected boolean isBaseLocationIncluded(String baseLocation) {
        return _locations.contains(baseLocation);
    }

    /**
     * This method is called once provisioning is over. It flushes the cache and clears up memory.
     * After this is called future calls to public methods will fail.
     */
    public void dispose() {
        if (_cacheFile != null && _dirty) {
            PrintStream out = null;
            try {
                File parent = _cacheFile.getParentFile();
                if (parent.isDirectory() || parent.mkdirs()) {
                    out = new PrintStream(_cacheFile);
                    for (List<Resource> bundleInfoList : _cacheBySymbolicName.values()) {
                        for (Resource bundleInfo : bundleInfoList) {
                            out.println(bundleInfo);
                        }
                    }
                }
            } catch (IOException e) {
            } finally {
                Utils.tryToClose(out);
            }
        }
        _bundleLocations.clear();
        _cacheBySymbolicName.clear();
    }

    /**
     * This method selects bundles based on the input criteria. The first parameter is the baseLocation. This
     * can be null, the empty string, a directory, a comma separated list of directories, or a file path relative
     * to the install. If it is a file path then that exact bundle is returned irrespective of other selection
     * parameters. If it is null or the empty string it is assumed to be a bundle in lib dir. If it is a directory,
     * or a comma separated directory then the subsequent matching rules will be used to choose a matching bundle
     * in the specified directories.
     * 
     * <p>Assuming a baseLocation of "dev/,lib/" a symbolic name of "a.b" and a version range of "[1,1.0.100)" then all
     * bundles in dev and lib will be searched looking for the highest versioned bundle with a symbolic name of a.b. Note
     * highest versioned excludes iFixes where the iFix base version is not located. So given identified bundles:
     * <ol>
     * <li>a.b/1.0.0</li>
     * <li>a.b/1.0.1.v1</li>
     * <li>a.b/1.0.2.v2</li>
     * </ol>
     * The middle bundle will be chosen.
     * </p>
     * 
     * @param baseLocation The base location.
     * @param symbolicName The desired symbolic name.
     * @param versionRange The range of versions that can be selected.
     * @return The file representing the chosen bundle.
     */
    public File selectBundle(String baseLocation, final String symbolicName, final VersionRange versionRange) {
        readCache();
        return selectResource(baseLocation, symbolicName, versionRange);
    }

    /**
     * This method can be used to obtain the 'base' bundle for a given 'selected' bundle.
     * The {@link ContentBasedLocalBundleRepository#selectBundle} Will select a Bundle, which may or
     * may not be an ifix.
     * When the selectedBundle is an ifix, this method will return the corresponding bundle that has been 'ifixed'.
     * If the selectedBundle is not an ifix, this method will return the selected bundle.
     * 
     * @param baseLocation The base location.
     * @param symbolicName The desired symbolic name.
     * @param versionRange The range of versions that can be selected.
     * @return The file representing the chosen bundle.
     */
    public File selectBaseBundle(String baseLocation, final String symbolicName, final VersionRange versionRange) {
        readCache();
        return selectResource(baseLocation, symbolicName, versionRange,
                              true, //performURICheck=true
                              true //selectBaseBundle=true
        );
    }

    /**
     * This method scans the provided location under the install dir. It looks at all the jar files in that directory
     * and provided the file isn't in the cache it'll read the manifest and add it into memory.
     * 
     * @param baseLocation the directory in the install to scan.
     */
    @Override
    protected synchronized void includeBaseLocation(String baseLocation) {
        _locations.add(baseLocation);

        // only do the processing if the cache has been read.
        if (_cacheRead) {
            File[] files = new File(_installDir, baseLocation).listFiles(new FileFilter() {
                @Override
                public boolean accept(File arg0) {
                    // select files only if they end .jar and they aren't in the cache already.
                    return arg0.getName().endsWith(".jar") && !!!_bundleLocations.contains(arg0);
                }
            });

            if (files != null) {
                for (File f : files) {
                    BundleInfo bInfo;
                    try {
                        bInfo = new BundleInfo(f, baseLocation);
                        if (bInfo.symbolicName != null) {
                            _dirty = true;
                            addToCache(bInfo);
                        }
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    /**
     * This method reads the cache. Each line in the file represents a different bundle. The BundleInfo
     * constructor reads the cache line and validates the data re-reading from the bundle manifest if required.
     * 
     * @param cache
     */
    @FFDCIgnore(IOException.class)
    // The feature manager and kernel only do provisioning from a single thread, so this is paranoia, but should
    // never be contended.
    private synchronized void readCache() {
        if (!!!_cacheRead) {
            BufferedReader reader = null;
            try {
                if (_cacheFile != null && _cacheFile.exists() && _cacheFile.isFile()) {
                    reader = new BufferedReader(new FileReader(_cacheFile));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        try {
                            BundleInfo bInfo = new BundleInfo(line);
                            if (bInfo.file.exists() && bInfo.symbolicName != null) {
                                addToCache(bInfo);
                            }
                        } catch (IOException e) {
                            // IOException creating BundleInfo.
                        }
                    }
                }
            } catch (IOException e) {
                // If we can't load the cache then we just throw everything 
                // away and do the more expensive read from original data.
                _bundleLocations.clear();
                _cacheBySymbolicName.clear();
            } finally {
                Utils.tryToClose(reader);
            }
            _cacheRead = true;
            // now we have read the cache we want to insure all base locations are processed.
            for (String loc : new HashSet<String>(_locations)) {
                includeBaseLocation(loc);
            }
        }
    }

    /**
     * This method adds into the cache. Adding into the cache requires updating a map and a set.
     * 
     * @param bInfo the bundle info to add into.
     */
    private void addToCache(BundleInfo bInfo) {
        List<Resource> info = _cacheBySymbolicName.get(bInfo.symbolicName);
        if (info == null) {
            info = new ArrayList<Resource>();
            info.add(bInfo);
        }

        info = _cacheBySymbolicName.putIfAbsent(bInfo.symbolicName, info);

        if (info != null) {
            synchronized (info) {
                info.add(bInfo);
            }
        }

        _bundleLocations.add(bInfo.file);
    }

    @Override
    protected void warnThatAnIFixWasIgnored(String fileName, String symbolicName, int majorVersion, int minorVersion, int microVersion) {
        _msgs.warning("warn.ifix.ignored", fileName, symbolicName + '_' + majorVersion + '.' + minorVersion + '.' + microVersion + ".jar");
    }

}