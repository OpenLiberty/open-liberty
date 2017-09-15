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
package com.ibm.ws.kernel.provisioning;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import com.ibm.ws.kernel.boot.internal.BootstrapConstants;

/**
 * Slimmed down for use in very early bootstrap: This does not read the contents
 * of all files in the lib dir. Instead, it requires that the JAR prefix match
 * the symbolic name (e.g., it assumes com.ibm.abc_1.0.jar has a MANIFEST.MF
 * with Bundle-SymbolicName of com.ibm.abc).
 */
public class NameBasedLocalBundleRepository extends AbstractResourceRepository {

    private static final String DEFAULT_LOCATION = "lib/";

    private final File rootDirectory;
    private final Messages msgs;
    private final Map<String, SymbolicNameResources> resourcesBySymbolicName = new HashMap<String, SymbolicNameResources>(1024);
    private final Set<String> locations = new HashSet<String>();

    public NameBasedLocalBundleRepository(File installDir) {
        rootDirectory = installDir;

        msgs = new Messages() {
            @Override
            public void warning(String key, Object... inserts) {
                String msg = BootstrapConstants.messages.getString(key);
                if (inserts == null || inserts.length == 0) {
                    System.out.println(msg);
                } else {
                    System.out.println(MessageFormat.format(msg, inserts));
                }
            }
        };

        includeBaseLocation(DEFAULT_LOCATION);
    }

    public File selectBundle(final String symbolicName, final VersionRange versionRange) {
        return selectResource("", symbolicName, versionRange);
    }

    @Override
    public File getRootDirectory() {
        return rootDirectory;
    }

    @Override
    protected String getDefaultBaseLocation() {
        return DEFAULT_LOCATION;
    }

    @Override
    protected List<Resource> getResourcesBySymbolicName(final String symbolicName) {
        SymbolicNameResources resources = resourcesBySymbolicName.get(symbolicName);
        return resources == null ? Collections.<Resource> emptyList() : resources.getResources();
    }

    @Override
    protected void includeBaseLocation(String baseLocation) {
        locations.add(baseLocation);

        File[] files = new File(rootDirectory, baseLocation).listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (name.endsWith(".jar")) {
                    int index = name.lastIndexOf('_');
                    if (index != -1) {
                        String symbolicName = name.substring(0, index);
                        SymbolicNameResources resources = new SymbolicNameResources(file, baseLocation);
                        SymbolicNameResources oldResources = resourcesBySymbolicName.put(symbolicName, resources);
                        if (oldResources != null) {
                            resourcesBySymbolicName.put(symbolicName, oldResources);
                            oldResources.addFile(file, baseLocation);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected boolean isBaseLocationIncluded(String baseLocation) {
        return locations.contains(baseLocation);
    }

    @Override
    protected void warnThatAnIFixWasIgnored(String fileName, String symbolicName, int majorVersion, int minorVersion, int microVersion) {
        msgs.warning("warn.ifix.ignored", fileName, symbolicName + '_' + majorVersion + '.' + minorVersion + '.' + microVersion + ".jar");
    }

    private class SymbolicNameResources {
        private final File file;
        private final String baseLocation;
        private List<File> files;
        private List<String> baseLocations;
        private List<Resource> resources;

        SymbolicNameResources(File file, String baseLocation) {
            this.file = file;
            this.baseLocation = baseLocation;
        }

        void addFile(File file, String baseLocation) {
            if (files == null) {
                files = new ArrayList<File>();
                files.add(this.file);
                baseLocations = new ArrayList<String>();
                baseLocations.add(this.baseLocation);
            }
            files.add(file);
            baseLocations.add(baseLocation);
            resources = null;
        }

        List<Resource> getResources() {
            if (resources == null) {
                List<File> files = this.files != null ? this.files : Collections.singletonList(file);
                List<String> baseLocations = this.baseLocations != null ? this.baseLocations : Collections.singletonList(baseLocation);
                resources = new ArrayList<Resource>(files.size());
                for (int i = 0; i < files.size(); i++) {
                    try {
                        File file = files.get(i);
                        String baseLocation = baseLocations.get(i);
                        resources.add(new BootstrapResource(file, baseLocation));
                    } catch (IOException e) {
                    }
                }
            }

            return resources;
        }
    }

    private class BootstrapResource extends Resource {

        private final File file;
        private final String baseLocation;
        private final Version version;
        private final boolean isFix;
        private final String symbolicName;

        /**
         * Create a bootstrap resource
         * 
         * @param file Resource to add..
         * @throws IOException
         */
        public BootstrapResource(File file, String baseLocation) throws IOException {
            this.file = file;
            this.baseLocation = baseLocation;

            JarFile jar = new JarFile(file);
            Manifest man = jar.getManifest();
            jar.close();
            Attributes a = man.getMainAttributes();
            symbolicName = getSymbolicName(a);
            version = Version.parseVersion(a.getValue("Bundle-Version"));
            Object iFixHeader = a.getValue("IBM-Interim-Fixes");
            Object tFixHeader = a.getValue("IBM-Test-Fixes");
            isFix = (iFixHeader != null || tFixHeader != null);
        }

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
         * The Bundle Symbolic name from the header can take attributes and directives so
         * we need to strip that out if present.
         * 
         * @param attributes The attributes in the manifest header.
         * @return the symbolic name minus any attributes.
         */
        private String getSymbolicName(Attributes attributes) {
            String value = attributes.getValue("Bundle-SymbolicName");
            int index = (value != null) ? value.indexOf(';') : -1;
            return (index > 0) ? value.substring(0, index).trim() : value;
        }
    }
}
