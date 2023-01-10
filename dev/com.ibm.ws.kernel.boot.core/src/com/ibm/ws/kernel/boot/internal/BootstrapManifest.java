/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.osgi.framework.Version;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.LaunchException;
import com.ibm.ws.kernel.boot.cmdline.Utils;

/**
 * Contain the informations in bootstrap jar's manifest
 */
public class BootstrapManifest {

    static final String BUNDLE_VERSION = "Bundle-Version";
    static final String JAR_PROTOCOL = "jar";

    /** prefix for system-package files */
    static final String SYSTEM_PKG_PREFIX = "OSGI-OPT/websphere/system-packages_";

    /** suffix for system-package files */
    static final String SYSTEM_PKG_SUFFIX = ".properties";

    /**
     * Manifest header designating packages that should be exported into the
     * framework by this jar
     */
    static final String MANIFEST_EXPORT_PACKAGE = "Export-Package";

    private static BootstrapManifest instance = null;

    private final Attributes manifestAttributes;
    private final boolean libertyBoot;

    public static BootstrapManifest readBootstrapManifest(boolean libertyBoot) throws IOException {
        BootstrapManifest manifest = instance;
        if (manifest == null) {
            manifest = instance = new BootstrapManifest(libertyBoot);
        }
        return manifest;
    }

    /** Clean up: allow garbage collection to clean up resources we don't need post-bootstrap */
    public static void dispose() {
        instance = null;
    }

    protected BootstrapManifest() throws IOException {
        this(false);
    }

    /**
     * In the case of liberty boot the manifest is discovered
     * by looking up the jar URL for this class.
     *
     * @param libertyBoot enables liberty boot
     * @throws IOException if here is an error reading the manifest
     */
    protected BootstrapManifest(boolean libertyBoot) throws IOException {
        this.libertyBoot = libertyBoot;
        manifestAttributes = libertyBoot ? getLibertyBootAttributes() : getAttributesFromBootstrapJar();
    }

    private static Attributes getAttributesFromBootstrapJar() throws IOException {
        JarFile jf = null;
        try {
            jf = new JarFile(KernelUtils.getBootstrapJar());
            Manifest mf = jf.getManifest();
            return mf.getMainAttributes();
        } catch (IOException e) {
            throw e;
        } finally {
            Utils.tryToClose(jf);
        }
    }

    private static Attributes getLibertyBootAttributes() {
        JarFile jf = getLibertBootJarFile();
        Manifest mf;
        try {
            mf = jf.getManifest();
            return mf.getMainAttributes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            Utils.tryToClose(jf);
        }
    }

    private static JarFile getLibertBootJarFile() {
        // here we assume we can lookup our own .class resource to find the JarFile
        return getJarFile(BootstrapManifest.class.getResource(BootstrapManifest.class.getSimpleName() + ".class"));
    }

    private static JarFile getJarFile(URL url) {
        if (JAR_PROTOCOL.equals(url.getProtocol())) {
            try {
                URLConnection conn = url.openConnection();
                if (conn instanceof JarURLConnection) {
                    return ((JarURLConnection) conn).getJarFile();
                }
            } catch (IOException e) {
                throw new IllegalStateException("No jar file found: " + url, e);
            }
        }
        throw new IllegalArgumentException("Not a jar URL: " + url);
    }

    /**
     * @return
     * @throws IOException
     */
    private JarFile getBootJar() throws IOException {
        // For liberty boot don't try to find the bootstrap jar.
        return libertyBoot ? getLibertBootJarFile() : new JarFile(KernelUtils.getBootstrapJar());
    }

    /**
     * @return the bundleVersion
     */
    public String getBundleVersion() {
        return manifestAttributes.getValue(BUNDLE_VERSION);
    }

    @SuppressWarnings("unchecked")
    private String calculateSystemPackages(String javaVersion) {
        try {
            javaVersion = javaVersion.split("\\.")[0];
            int version = Integer.parseInt(javaVersion);
            if (version < 9) {
                return null;
            }

            Method classGetModule = Class.class.getMethod("getModule"); //$NON-NLS-1$
            Class<?> moduleLayerClass = Class.forName("java.lang.ModuleLayer"); //$NON-NLS-1$
            Method boot = moduleLayerClass.getMethod("boot"); //$NON-NLS-1$
            Method modules = moduleLayerClass.getMethod("modules"); //$NON-NLS-1$
            Class<?> moduleClass = Class.forName("java.lang.Module"); //$NON-NLS-1$
            Method getDescriptor = moduleClass.getMethod("getDescriptor"); //$NON-NLS-1$
            Class<?> moduleDescriptorClass = Class.forName("java.lang.module.ModuleDescriptor"); //$NON-NLS-1$
            Method exports = moduleDescriptorClass.getMethod("exports"); //$NON-NLS-1$
            Method isAutomatic = moduleDescriptorClass.getMethod("isAutomatic"); //$NON-NLS-1$
            Method packagesMethod = moduleDescriptorClass.getMethod("packages"); //$NON-NLS-1$
            Class<?> exportsClass = Class.forName("java.lang.module.ModuleDescriptor$Exports"); //$NON-NLS-1$
            Method isQualified = exportsClass.getMethod("isQualified"); //$NON-NLS-1$
            Method source = exportsClass.getMethod("source"); //$NON-NLS-1$

            // The reflective code below is the equivalent of the following code:
            // @formatter:off - turns off eclipse formatter
            /*
            ModuleLayer bootLayer = ModuleLayer.boot();
            Set<Module> bootModules = bootLayer.modules();
            Module thisModule = getClass().getModule();
            Set<String> packages = new TreeSet<>();

            for (Module m : bootModules) {
                if (m.equals(thisModule)) {
                    // Do not calculate the exports from the framework module.
                    // This is to handle the case where the framework is on the module path
                    // to avoid double exports from the system.bundles
                    continue;
                }
                ModuleDescriptor descriptor = m.getDescriptor();
                if (descriptor.isAutomatic()) {
                     // Automatic modules are supposed to export all their packages.
                     // However, java.lang.module.ModuleDescriptor::exports returns an empty set for them.
                     // Add all their packages (as returned by java.lang.module.ModuleDescriptor::packages)
                     // to the list of VM supplied packages.
                    packages.addAll(descriptor.packages());
                } else {
                    for (Exports export : descriptor.exports()) {
                        String pkg = export.source();
                        if (!(export.isQualified())) {
                            packages.add(pkg);
                        }
                    }
                }
            }
            */
            // @formatter:on - turns back on eclipse formatter.

            // TODO when Java 8 support ends we can replace the following reflective code with the code commented out above.

            // bootLayer is type java.lang.ModuleLayer
            Object bootLayer = boot.invoke(null);
            // bootModules is type Set<java.lang.Module>
            Set<?> bootModules = (Set<?>) modules.invoke(bootLayer);
            // thisModule is type java.lang.Module
            Object thisModule = classGetModule.invoke(getClass());
            Set<String> packages = new TreeSet<>();

            // m is type java.lang.Module
            for (Object m : bootModules) {
                if (m.equals(thisModule)) {
                    // Do not calculate the exports from the framework module.
                    // This is to handle the case where the framework is on the module path
                    // to avoid double exports from the system.bundles
                    continue;
                }
                // descriptor is type java.lang.module.ModuleDescriptor
                Object descriptor = getDescriptor.invoke(m);
                if ((Boolean) isAutomatic.invoke(descriptor)) {
                    /*
                     * Automatic modules are supposed to export all their packages.
                     * However, java.lang.module.ModuleDescriptor::exports returns an empty set for them.
                     * Add all their packages (as returned by java.lang.module.ModuleDescriptor::packages)
                     * to the list of VM supplied packages.
                     */
                    packages.addAll((Set<String>) packagesMethod.invoke(descriptor));
                } else {
                    // export is type java.lang.module.ModuleDescriptor$Exports
                    for (Object export : (Set<?>) exports.invoke(descriptor)) {
                        String pkg = (String) source.invoke(export);
                        if (!((boolean) isQualified.invoke(export))) {
                            packages.add(pkg);
                        }
                    }
                }
            }

            // HACK ALERT always add javax.xml.soap to keep the incorrect behavior
            packages.add("javax.xml.soap");
            // HACK ALERT always add these IBM packages to keep incorrect behavior
            packages.add("com.ibm.tools.attach");
            packages.add("com.ibm.security.jgss");
            packages.add("com.ibm.security.auth.module");
            packages.add("com.ibm.security.auth.callback");
            StringBuilder result = new StringBuilder();
            for (String pkg : packages) {
                if (result.length() != 0) {
                    result.append(',').append(' ');
                }
                result.append(pkg);
                result.append("; ibm-api-type=spec");
                if ("javax.transaction.xa".equals(pkg)) {
                    result.append("; javax.transaction=JavaSE; mandatory:=javax.transaction");
                }
            }
            return result.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param bootProps
     * @throws IOException
     */
    public void prepSystemPackages(BootstrapConfig bootProps) {
        // Look for _extra_ system packages
        String packages = bootProps.get(BootstrapConstants.INITPROP_OSGI_EXTRA_PACKAGE);

        // Look for system packages set in bootstrap properties first
        String syspackages = bootProps.get(BootstrapConstants.INITPROP_OSGI_SYSTEM_PACKAGES);

        // Look for exported packages in manifest: append to bootstrap packages
        String mPackages = manifestAttributes.getValue(MANIFEST_EXPORT_PACKAGE);
        if (mPackages != null) {
            packages = (packages == null) ? mPackages : packages + "," + mPackages;

            // save new "extra" packages
            if (packages != null)
                bootProps.put(BootstrapConstants.INITPROP_OSGI_EXTRA_PACKAGE, packages);
        }

        // system packages are replaced, not appended
        // so we only go look for our list of system packages if it hasn't already been set in bootProps
        // (that's the difference, re: system packages vs. "Extra" packages.. )
        if (syspackages == null) {
            // Look for system packages property file in the jar
            String javaVersion = System.getProperty("java.version", "1.6.0");
            // the java version may have an update modifier in the version string so we need to remove it.
            int index = javaVersion.indexOf('_');
            index = (index == -1) ? javaVersion.indexOf('-') : index;
            javaVersion = (index == -1) ? javaVersion : javaVersion.substring(0, index);
            String calculatedPackages = calculateSystemPackages(javaVersion);
            if (calculatedPackages != null) {
                // save the calculated system packages
                bootProps.put(BootstrapConstants.INITPROP_OSGI_SYSTEM_PACKAGES, calculatedPackages);
                return;
            }
            // TODO this code below can be removed when support for Java 8 ends
            String pkgListFileName = SYSTEM_PKG_PREFIX + javaVersion + SYSTEM_PKG_SUFFIX;

            JarFile jarFile = null;
            try {
                jarFile = getBootJar();

                List<String> systemPackageFileNames = new ArrayList<String>();

                Enumeration<JarEntry> bootstrapJarEntries = jarFile.entries();
                while (bootstrapJarEntries.hasMoreElements()) {
                    JarEntry entry = bootstrapJarEntries.nextElement();
                    if (entry != null && entry.getName().startsWith(SYSTEM_PKG_PREFIX) && entry.getName().endsWith(SYSTEM_PKG_SUFFIX)) {
                        //was one of the system package properties files, add to the list
                        systemPackageFileNames.add(entry.getName());
                    }
                }

                int numNames = systemPackageFileNames.size();
                //if we found any package files then work out the appropriate one
                //otherwise try the default which will produce a nice error message
                if (numNames != 0) {
                    //sort the files by version (high to low)
                    if (numNames > 1) {
                        Collections.sort(systemPackageFileNames, new Comparator<String>() {
                            @Override
                            public int compare(String name1, String name2) {
                                //elements can't be null because we don't allow
                                //null elements to be added to the list
                                //use OSGi versions so we can cope easily with, for example, java 10
                                Version oneVersion = getVersion(name1);
                                Version twoVersion = getVersion(name2);
                                //!!NOTE reverse the comparison order to get high to low ordering
                                return twoVersion.compareTo(oneVersion);
                            }

                            private Version getVersion(String name) {
                                //remove the prefix
                                String version = name.substring(SYSTEM_PKG_PREFIX.length(), name.length());
                                //remove the suffix
                                version = version.substring(0, version.indexOf(SYSTEM_PKG_SUFFIX, 0));
                                return new Version(version);
                            }
                        });
                    }

                    //check if we have a package file for the version of Java we are using
                    int indexOfPackageFileToUse = systemPackageFileNames.indexOf(pkgListFileName);
                    // If not found, check for a more generic version string
                    if (indexOfPackageFileToUse < 0 && javaVersion.indexOf('.') > 0) {
                        // If exact version match is not found, strip the minor/micro versions leaving just the major version
                        String genericPkgListFileName = SYSTEM_PKG_PREFIX + javaVersion.split("\\.")[0] + SYSTEM_PKG_SUFFIX;
                        indexOfPackageFileToUse = systemPackageFileNames.indexOf(genericPkgListFileName);
                    }

                    //if we don't, then we should use the highest available package list instead
                    //unless there are no files at all, we don't worry about the case of not having
                    //a matching file for a lower version because the minimum execution environment
                    //means we will always be running on the minimum supported level.
                    if (indexOfPackageFileToUse < 0)
                        indexOfPackageFileToUse = 0;
                    //cut down the list to be from the current java version to the oldest version
                    //we will read all the files and append the properties to save maintenance effort on the package lists
                    systemPackageFileNames = systemPackageFileNames.subList(indexOfPackageFileToUse, numNames);
                } else {
                    //default system package file name
                    systemPackageFileNames = Arrays.asList(new String[] { pkgListFileName });
                }

                syspackages = getMergedSystemProperties(jarFile, systemPackageFileNames);

                // save new system packages
                if (syspackages != null)
                    bootProps.put(BootstrapConstants.INITPROP_OSGI_SYSTEM_PACKAGES, syspackages);

            } catch (IOException ioe) {
                throw new LaunchException("Unable to find or read specified properties file; "
                                          + pkgListFileName, MessageFormat.format(BootstrapConstants.messages.getString("error.unknownException"), ioe.toString()), ioe);
            } finally {
                Utils.tryToClose(jarFile);
            }
        }
    }

    private String getMergedSystemProperties(JarFile jarFile, List<String> pkgListFileNames) throws IOException {
        String packages = null;
        for (String pkgListFileName : pkgListFileNames) {
            ZipEntry propFile = jarFile.getEntry(pkgListFileName);
            if (propFile != null) {
                // read org.osgi.framework.system.packages property value from the file
                Properties properties = new Properties();
                InputStream is = jarFile.getInputStream(propFile);
                try {
                    properties.load(is);
                    String loadedPackages = properties.getProperty(BootstrapConstants.INITPROP_OSGI_SYSTEM_PACKAGES);
                    if (loadedPackages != null) {
                        packages = (packages == null) ? loadedPackages : packages + "," + loadedPackages;
                    }
                } finally {
                    Utils.tryToClose(is);
                }
            } else {
                throw new IOException("Unable to find specified properties file; " + pkgListFileName);
            }
        }
        return packages;
    }
}
