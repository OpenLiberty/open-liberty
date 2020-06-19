/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.Debug;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.archive.Archive;
import com.ibm.ws.kernel.boot.archive.ArchiveEntryConfig;
import com.ibm.ws.kernel.boot.archive.ArchiveFactory;
import com.ibm.ws.kernel.boot.archive.DirEntryConfig;
import com.ibm.ws.kernel.boot.archive.DirPattern.PatternStrategy;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.FileUtils;
import com.ibm.ws.kernel.boot.internal.commands.ProcessorUtils.LooseConfig;
import com.ibm.ws.kernel.provisioning.ProductExtension;
import com.ibm.ws.kernel.provisioning.ProductExtensionInfo;

/**
 * execute the package command
 */
public class PackageProcessor implements ArchiveProcessor {

    private final String processName;

    private final File packageFile;

    private final BootstrapConfig bootProps;

    private final Map<PackageOption, String> options;

    protected static final String DEFAULT_CONFIG_LOCATION_KEY = "configLocation";

    private final File wlpUserDir;

    private final File processConfigDir;

    private final File workAreaTmpDir;

    private final Set<File> looseFiles = new HashSet<File>();// the loose files

    private final Set<String> processContent;

    final File installRoot;
    final String wlpProperty = "/lib/versions/WebSphereApplicationServer.properties";
    final String wlpPropertyBackup = "WebSphereApplicationServer.properties.bak";

    protected static final String PACKAGE_ARCHIVE_PREFIX = "wlp/";
    public String packageArchiveEntryPrefix = PACKAGE_ARCHIVE_PREFIX;

    public boolean isServerRootOptionSet = false;

    public PackageProcessor(String processName, File packageFile, BootstrapConfig bootProps, List<Pair<PackageOption, String>> options, Set<String> processContent) {
        this.processName = processName;
        this.packageFile = packageFile;
        this.bootProps = bootProps;
        this.installRoot = bootProps.getInstallRoot();

        this.options = new HashMap<PackageOption, String>();
        if (options != null) {
            for (Pair<PackageOption, String> option : options) {
                this.options.put(option.getPairKey(), option.getPairValue());
            }
        }

        this.wlpUserDir = bootProps.getUserRoot();
        this.processConfigDir = bootProps.getConfigFile(null);

        this.processContent = processContent;

        this.workAreaTmpDir = new File(bootProps.get(BootstrapConstants.LOC_PROPERTY_SRVTMP_DIR));
        this.workAreaTmpDir.mkdirs();
    }

    /**
     * @return true if --include=usr was specified on the package command.
     */
    private boolean isIncludeOptionEqualToUsr() {
        String val = options.get(PackageOption.INCLUDE);

        return IncludeOption.USR.matches(val);
    }

    /**
     * Create a proper manifest file for the --include=usr option. The manifest is a copy
     * of the given installation manifest, with the following edits:
     *
     * Removed: License-Agreement
     * Removed: License-Information
     * Added: Applies-To: com.ibm.websphere.appserver
     * Added: Extract-Installer: false
     *
     * @return the manifest file
     */
    protected File buildManifestForIncludeEqualsUsr(File installationManifest) throws IOException {
        Manifest mf = new Manifest();
        mf.read(new FileInputStream(installationManifest));

        mf.getMainAttributes().remove(new Attributes.Name("License-Agreement"));
        mf.getMainAttributes().remove(new Attributes.Name("License-Information"));
        mf.getMainAttributes().remove(new Attributes.Name("Applies-To"));
        mf.getMainAttributes().remove(new Attributes.Name("Extract-Installer"));
        mf.getMainAttributes().putValue("Applies-To", "com.ibm.websphere.appserver");
        mf.getMainAttributes().putValue("Extract-Installer", "false");

        File newMani = new File(workAreaTmpDir, "MANIFEST.usrinclude.tmp");
        mf.write(new FileOutputStream(newMani));

        return newMani;
    }

    /**
     * @return true if --include=runnable was specified on the package command.
     */
    private boolean doesIncludeOptionHaveRunnable() {
        String val = options.get(PackageOption.INCLUDE);

        return IncludeOption.RUNNABLE.matches(val);
    }

    /**
     * Create a proper manifest file for the --include=execute option. The manifest is a copy
     * of the given installation manifest, with the following edits:
     *
     * Change
     * from:
     * Main-Class: wlp.lib.extract.SelfExtract
     * to:
     * Main-Class: wlp.lib.extract.SelfExtractRun
     * add:
     * Server-Name: <processName>
     *
     * @return the manifest file
     */
    protected File buildManifestForIncludeHasRunnable(File installationManifest) throws IOException {
        Manifest mf = new Manifest();
        mf.read(new FileInputStream(installationManifest));

        mf.getMainAttributes().remove(new Attributes.Name("License-Agreement"));
        mf.getMainAttributes().remove(new Attributes.Name("License-Information"));
        mf.getMainAttributes().putValue("Main-Class", "wlp.lib.extract.SelfExtractRun");
        mf.getMainAttributes().putValue("Server-Name", processName);

        // For Java 9, we need to apply the /wlp/lib/platform/java/java9.options to the manifest.
        if (System.getProperty("java.specification.version") != null && !System.getProperty("java.specification.version").startsWith("1.")) {
            HashMap<String, String> map = readJava9Options();
            mf.getMainAttributes().putValue("Add-Exports", map.get("exports"));
            mf.getMainAttributes().putValue("Add-Opens", map.get("opens"));
        }

        File newMani = new File(workAreaTmpDir, "MANIFEST.usrinclude.tmp");
        mf.write(new FileOutputStream(newMani));

        return newMani;
    }

    private Archive createArchive(final File file) throws IOException {

        if (System.getSecurityManager() == null) {
            return ArchiveFactory.create(file);
        } else {
            try {
                return AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<Archive>() {

                    @Override
                    public Archive run() throws IOException {
                        return ArchiveFactory.create(file);
                    }
                });
            } catch (PrivilegedActionException e) {
                e.printStackTrace();
                throw (IOException) e.getException();
            }
        }
    }

    public ReturnCode execute(boolean runtimeOnly) {
        Archive archive = null;
        ReturnCode rc = backupWebSphereApplicationServerProperty(installRoot);
        if (!rc.equals(ReturnCode.OK)) {
            return rc;
        }
        try {

            // Dont allow --include=usr and --archive=*.jar combination
            if (isIncludeOptionEqualToUsr() && isArchiveJar()) {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.package.usr.jar"), processName));
                return ReturnCode.ERROR_SERVER_PACKAGE;
            }

            // Create the default archive
            archive = ArchiveFactory.create(packageFile, java2SecurityEnabled());

            // for a Jar archive, the manifest must be first.
            if (isArchiveJar()) {
                File manifest = new File(bootProps.getInstallRoot(), "lib/extract/META-INF/MANIFEST.MF");
                if (!manifest.exists()) {
                    //maybe user didnt extract file with jar -jar, but unzipped..
                    manifest = new File(bootProps.getInstallRoot().getParentFile(), "META-INF/MANIFEST.MF");
                }
                if (!manifest.exists()) {
                    System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.minify.missing.manifest"), processName));
                    return ReturnCode.ERROR_SERVER_PACKAGE;
                }

                if (isIncludeOptionEqualToUsr()) {
                    // Build a special manifest for --include=usr.
                    archive.addFileEntry("META-INF/MANIFEST.MF", buildManifestForIncludeEqualsUsr(manifest));
                } else if (doesIncludeOptionHaveRunnable()) {
                    // Build a special manifest for --include=runnable
                    archive.addFileEntry("META-INF/MANIFEST.MF", buildManifestForIncludeHasRunnable(manifest));
                } else {
                    archive.addFileEntry("META-INF/MANIFEST.MF", manifest);
                }

                //add any meta-inf folder content, and the auto-extract code.
                archive.addEntryConfigs(createSelfExtractEntryConfigs());
            }
            if (options.isEmpty()) {
                archive.addEntryConfigs(createAllConfigs(processName, runtimeOnly));
            } else {
                String val = options.get(PackageOption.INCLUDE);
                // process all the options here
                if (includeAllorNoMinifyRunnable(val)) {
                    archive.addEntryConfigs(createAllConfigs(processName, runtimeOnly));
                } else if (includeUsr(val)) {
                    archive.addEntryConfigs(createUsrConfigs(processName, true));
                } else if (bootProps.getProcessType() != BootstrapConstants.LOC_PROCESS_TYPE_CLIENT &&
                           includeMinifyorMinifyRunnable(val)) {
                    archive.addEntryConfigs(createMinifyConfigs(processName));
                } else {
                    System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("warn.packageServer.include.unknownOption"), val));
                    archive.addEntryConfigs(createAllConfigs(processName, runtimeOnly));
                }
            }
            archive.create();
        } catch (IOException e) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.unableZipDir"), e));
            Debug.printStackTrace(e);
            return ReturnCode.ERROR_SERVER_PACKAGE;
        } finally {
            // must close the archive so that the create can complete
            Utils.tryToClose(archive);
            restoreWebSphereApplicationServerProperty(installRoot);
            // clean temporary files
            FileUtils.recursiveClean(workAreaTmpDir);
        }
        return ReturnCode.OK;

    }

    /*
     * Return true for include values of:
     * include=all
     * include=runnable
     * include=all,runnable
     *
     * Otherwise return false.
     */
    private boolean includeAllorNoMinifyRunnable(String val) {
        return IncludeOption.ALL.matches(val) || (IncludeOption.RUNNABLE.matches(val) && !IncludeOption.MINIFY.matches(val));
    }

    /*
     * Return true for include values of:
     * include=minify
     * include=minify,runnable
     *
     * Otherwise return false.
     */
    private boolean includeMinifyorMinifyRunnable(String val) {
        return IncludeOption.MINIFY.matches(val);
    }

    /*
     * Return true for include value of:
     * include=usr
     *
     * Otherwise return false.
     */
    private boolean includeUsr(String val) {
        return IncludeOption.USR.matches(val);
    }

    private List<ArchiveEntryConfig> createSelfExtractEntryConfigs() throws IOException {
        List<ArchiveEntryConfig> entryConfigs = new ArrayList<ArchiveEntryConfig>();
        File metaInf = new File(bootProps.getInstallRoot(), "lib/extract/META-INF");
        if (!metaInf.exists()) {
            //maybe user didnt extract file with jar -jar, but unzipped..
            //so look for META-INF above WLP Root !!
            File aboveRoot = bootProps.getInstallRoot().getParentFile();
            metaInf = new File(aboveRoot, "META-INF");
        }
        //metaInf will not be null now, as manifest has already been found not null using the
        //same tests within the calling 'execute' method.
        DirEntryConfig metaInfDirConfig = new DirEntryConfig("META-INF/", metaInf, false, PatternStrategy.IncludePreference);
        //exclude the manifest from the add, as it's already been added due to needing to be 1st.
        metaInfDirConfig.exclude(Pattern.compile(Pattern.quote(new File(metaInf, "MANIFEST.MF").getAbsolutePath())));
        entryConfigs.add(metaInfDirConfig);

        addLibExtractDir(entryConfigs);

        return entryConfigs;
    }

    private List<ArchiveEntryConfig> createMinifyConfigs(String processName) throws IOException {
        List<ArchiveEntryConfig> entryConfigs = new ArrayList<ArchiveEntryConfig>();

        Set<String> featureResourcePaths = processContent;

        /*
         * Create an empty (includeByDefault==false) config for the root dir of liberty
         */
        DirEntryConfig rootDirConfig = new DirEntryConfig(packageArchiveEntryPrefix, bootProps.getInstallRoot(), false, PatternStrategy.ExcludePreference);
        entryConfigs.add(rootDirConfig);

        List<DirEntryConfig> extensionDirConfigs = new ArrayList<DirEntryConfig>();
        for (ProductExtensionInfo info : ProductExtension.getProductExtensions()) {
            File extensionDir = new File(info.getLocation());
            if (!extensionDir.isAbsolute()) {
                File parentDir = bootProps.getInstallRoot().getParentFile();
                extensionDir = ProcessorUtils.getFileFromDirectory(parentDir, info.getLocation());
            }

            DirEntryConfig looseExtensionDirConfig = new DirEntryConfig(info.getLocation(), extensionDir, false, PatternStrategy.ExcludePreference);
            extensionDirConfigs.add(looseExtensionDirConfig);
        }
        /*
         * Add back all the parts of liberty that we were told to by the runtime.
         */
        for (String s : featureResourcePaths) {
            String match = Pattern.quote(s);
            Pattern featurePattern = Pattern.compile(match);
            rootDirConfig.include(featurePattern);
            for (DirEntryConfig extensionDirConfig : extensionDirConfigs) {
                extensionDirConfig.include(featurePattern);
                entryConfigs.add(extensionDirConfig);
            }
        }

        boolean isJarPackage = packageFile.getName().endsWith(".jar");
        /*
         * Add back the lafiles directory
         * these are 'owned' by the installer.
         */
        File lafilesDir = new File(bootProps.getInstallRoot(), "lafiles");
        if (lafilesDir.exists()) {
            DirEntryConfig lafilesDirConfig = new DirEntryConfig(packageArchiveEntryPrefix + "lafiles", lafilesDir, true, PatternStrategy.IncludePreference);
            entryConfigs.add(lafilesDirConfig);
        }

        /*
         * Add back the templates directory
         * (these are orphans that need resolution still)
         */
        File templatesDir = new File(bootProps.getInstallRoot(), "templates");
        DirEntryConfig templatesDirConfig = new DirEntryConfig(packageArchiveEntryPrefix + "templates", templatesDir, true, PatternStrategy.IncludePreference);
        entryConfigs.add(templatesDirConfig);

        /*
         * Add back the templates directory (if building a jar, it's already added)
         * these are 'owned' by the installer.
         */
        if (!isJarPackage) {
            addLibExtractDir(entryConfigs);
        }

        /*
         * Add usr content.
         */
        entryConfigs.addAll(createUsrConfigs(processName, false));

        // add the package_<timestamp>.txt
        entryConfigs.addAll(createPkgInfoConfigs(processName));

        return entryConfigs;
    }

    private void addLibExtractDir(List<ArchiveEntryConfig> entryConfigs) throws IOException {
        try {
            File libExtractDir = new File(bootProps.getInstallRoot(), "lib/extract");
            DirEntryConfig libExtractDirConfig = new DirEntryConfig(packageArchiveEntryPrefix + "lib/extract", libExtractDir, true, PatternStrategy.IncludePreference);
            entryConfigs.add(libExtractDirConfig);
        } catch (FileNotFoundException ex) {
            System.out.println(BootstrapConstants.messages.getString("error.package.missingLibExtractDir"));
            throw ex;
        }
    }

    private List<ArchiveEntryConfig> createAllConfigs(String processName, boolean runtimeOnly) throws IOException {
        List<ArchiveEntryConfig> entryConfigs = new ArrayList<ArchiveEntryConfig>();

        // avoid any special characters in InstallRootName when construct patterns
        String regexInstallRootName = Pattern.quote(bootProps.getInstallRoot().getName());

        /*
         * Add wlp's root directory
         */
        DirEntryConfig rootDirConfig = new DirEntryConfig(packageArchiveEntryPrefix, bootProps.getInstallRoot(), true, PatternStrategy.IncludePreference);

        entryConfigs.add(rootDirConfig);

        // include all underneath install-root except usr directory
        rootDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexInstallRootName + REGEX_SEPARATOR + BootstrapConstants.LOC_AREA_NAME_USR));

        // if we are building a jar, we have already included the lib/extract content!
        if (packageFile.getName().endsWith(".jar")) {
            File libExtract = new File(bootProps.getInstallRoot(), "lib/extract");
            rootDirConfig.exclude(Pattern.compile(Pattern.quote(libExtract.getAbsolutePath())));
        }

        // exclude the server.usr.dir and server.output.dir, because they maybe specified underneath install-root
        String installRootAbsPath = bootProps.getInstallRoot().getAbsolutePath();
        String userRootAbsPath = bootProps.getUserRoot().getAbsolutePath();
        String processOutputAbsPath = bootProps.getOutputFile(null).getAbsolutePath();
        if (userRootAbsPath.contains(installRootAbsPath)) {
            rootDirConfig.exclude(Pattern.compile(Pattern.quote(userRootAbsPath)));
        }
        if (processOutputAbsPath.contains(installRootAbsPath)) {
            rootDirConfig.exclude(Pattern.compile(Pattern.quote(processOutputAbsPath)));
        }

        if (!runtimeOnly) {
            /*
             * Add usr directory
             */
            entryConfigs.addAll(createUsrConfigs(processName, true));
        }

        // add the package_<timestamp>.txt
        entryConfigs.addAll(createPkgInfoConfigs(processName));

        // Add product extensions
        File prodExtDir = ProcessorUtils.getFileFromDirectory(wlpUserDir.getParentFile(), "/etc/extensions");
        if (prodExtDir.exists()) {
            DirEntryConfig prodExtDirConfig = new DirEntryConfig(packageArchiveEntryPrefix + "etc/extensions", prodExtDir, true, PatternStrategy.IncludePreference);
            entryConfigs.add(prodExtDirConfig);
        }
        for (ProductExtensionInfo info : ProductExtension.getProductExtensions()) {
            File extensionDir = new File(info.getLocation());
            if (!extensionDir.isAbsolute()) {
                File parentDir = bootProps.getInstallRoot().getParentFile();
                extensionDir = ProcessorUtils.getFileFromDirectory(parentDir, info.getLocation());
            }

            DirEntryConfig looseExtensionDirConfig = new DirEntryConfig(info.getLocation(), extensionDir, true, PatternStrategy.IncludePreference);
            entryConfigs.add(looseExtensionDirConfig);
        }

        return entryConfigs;
    }

    private List<ArchiveEntryConfig> createUsrConfigs(String processName, boolean addUsrExtension) throws IOException {
        List<ArchiveEntryConfig> entryConfigs = new ArrayList<ArchiveEntryConfig>();

        /*
         * Add the archived loose files
         */
        getReferencedResources(entryConfigs);

        /*
         * Add process config directory (actually <userRoot>/servers/<processName> or <userRoot>/clients/<processName>)
         */
        String locAreaName = BootstrapConstants.LOC_AREA_NAME_SERVERS;
        if (bootProps.getProcessType() == BootstrapConstants.LOC_PROCESS_TYPE_CLIENT) {
            locAreaName = BootstrapConstants.LOC_AREA_NAME_CLIENTS;
        }

        DirEntryConfig processConfigDirConfig = null;
        // if --server-root set, then don't add /usr/ in path
        if (isServerRootOptionSet && (includeUsr(options.get(PackageOption.INCLUDE)))) {
            processConfigDirConfig = new DirEntryConfig(packageArchiveEntryPrefix
                                                        + locAreaName + "/"
                                                        + processName + "/", processConfigDir, true, PatternStrategy.IncludePreference);
        } else {
            processConfigDirConfig = new DirEntryConfig(packageArchiveEntryPrefix
                                                        + BootstrapConstants.LOC_AREA_NAME_USR + "/"
                                                        + locAreaName + "/"
                                                        + processName + "/", processConfigDir, true, PatternStrategy.IncludePreference);
        }
        entryConfigs.add(processConfigDirConfig);

        // avoid any special characters in processName when construct patterns
        String regexProcessName = Pattern.quote(processName);

        // {server.config.dir} may be equal {server.output.dir},
        // Exclude workarea and logs directories
        processConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexProcessName + REGEX_SEPARATOR + "workarea"));
        processConfigDirConfig.include(Pattern.compile(REGEX_SEPARATOR + regexProcessName + REGEX_SEPARATOR + "workarea" + REGEX_SEPARATOR + "\\.sLock$"));
        processConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexProcessName + REGEX_SEPARATOR + "logs"));
        // Exclude dump directory
        processConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexProcessName + REGEX_SEPARATOR + "dump_" + REGEX_TIMESTAMP));
        // Exclude javadump outputs
        processConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexProcessName + REGEX_SEPARATOR + "core\\.[^\\\\/]+\\.dmp"));
        processConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexProcessName + REGEX_SEPARATOR + "heapdump\\.[^\\\\/]+\\.phd"));
        processConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexProcessName + REGEX_SEPARATOR + "java\\.[^\\\\/]+\\.hprof"));
        processConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexProcessName + REGEX_SEPARATOR + "javacore\\.[^\\\\/]+\\.txt"));
        processConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexProcessName + REGEX_SEPARATOR + "javadump\\.[^\\\\/]+\\.txt"));
        // Exclude server package and dump files.
        processConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexProcessName + "\\.(zip|pax|jar)$"));
        processConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexProcessName + "\\.dump-" + REGEX_TIMESTAMP + "\\.(zip|pax)$"));
        // Exclude the package_<timestamp>.txt file, will add it later
        processConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexProcessName + REGEX_SEPARATOR + "package_" + REGEX_TIMESTAMP + "\\.txt"));

        /*
         * exclude loose xml files from server config directory
         */
        for (File app : looseFiles) {
            String appName = "." + app.getName().replace(".", "\\.");
            processConfigDirConfig.exclude(Pattern.compile(appName));
        }

        /*
         * Add shared directory
         */
        File sharedDir = ProcessorUtils.getFileFromDirectory(wlpUserDir, "shared");
        if (sharedDir.exists()) {
            DirEntryConfig serverSharedDirConfig = null;
            if (isServerRootOptionSet && (includeUsr(options.get(PackageOption.INCLUDE)))) {
                serverSharedDirConfig = new DirEntryConfig(packageArchiveEntryPrefix
                                                           + BootstrapConstants.LOC_AREA_NAME_SHARED + "/", sharedDir, true, PatternStrategy.IncludePreference);
            } else {
                serverSharedDirConfig = new DirEntryConfig(packageArchiveEntryPrefix
                                                           + BootstrapConstants.LOC_AREA_NAME_USR + "/"
                                                           + BootstrapConstants.LOC_AREA_NAME_SHARED + "/", sharedDir, true, PatternStrategy.IncludePreference);
            }
            entryConfigs.add(serverSharedDirConfig);
            // exclude security sensitive files
            serverSharedDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + "resources" + REGEX_SEPARATOR + "security" + REGEX_SEPARATOR + "key.jks"));
            serverSharedDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + "resources" + REGEX_SEPARATOR + "security" + REGEX_SEPARATOR + "key.p12"));

            /*
             * exclude loose xml files from shared directory
             */
            for (File app : looseFiles) {
                String appName = "." + app.getName().replace(".", "\\.");
                if (FileUtils.isUnderDirectory(app, sharedDir)) {
                    serverSharedDirConfig.exclude(Pattern.compile(appName));
                }
            }
        }

        /*
         * Add /usr/extension directory...aka user features
         */
        if (addUsrExtension) {
            File extensionDir = ProcessorUtils.getFileFromDirectory(wlpUserDir, BootstrapConstants.LOC_AREA_NAME_EXTENSION);
            DirEntryConfig serverExtensionDirConfig = null;
            if (extensionDir.exists()) {
                if (isServerRootOptionSet && (includeUsr(options.get(PackageOption.INCLUDE)))) {
                    serverExtensionDirConfig = new DirEntryConfig(packageArchiveEntryPrefix
                                                                  + BootstrapConstants.LOC_AREA_NAME_EXTENSION
                                                                  + "/", extensionDir, true, PatternStrategy.IncludePreference);
                } else {
                    serverExtensionDirConfig = new DirEntryConfig(packageArchiveEntryPrefix
                                                                  + BootstrapConstants.LOC_AREA_NAME_USR + "/"
                                                                  + BootstrapConstants.LOC_AREA_NAME_EXTENSION
                                                                  + "/", extensionDir, true, PatternStrategy.IncludePreference);
                }
                entryConfigs.add(serverExtensionDirConfig);
            }

        }

        return entryConfigs;

    }

    private List<ArchiveEntryConfig> createPkgInfoConfigs(String processName) throws IOException {
        List<ArchiveEntryConfig> entryConfigs = new ArrayList<ArchiveEntryConfig>();
        // avoid any special characters in processName when construct patterns
        String regexProcessName = Pattern.quote(processName);
        // Include the package_<timestamp>.txt that generated in server output dir, and must be move into lib/versions
        DirEntryConfig processPkgInfoConfig = new DirEntryConfig(packageArchiveEntryPrefix
                                                                 + BootstrapConstants.LOC_AREA_NAME_LIB + "/"
                                                                 + "versions" + "/", bootProps.getOutputFile(null), false, PatternStrategy.IncludePreference);
        entryConfigs.add(processPkgInfoConfig);

        processPkgInfoConfig.include(Pattern.compile(REGEX_SEPARATOR + regexProcessName + REGEX_SEPARATOR + "package_" + REGEX_TIMESTAMP + "\\.txt"));
        return entryConfigs;
    }

    private void getReferencedResources(List<ArchiveEntryConfig> entryConfigs) throws IOException {
        // Add the loose application resources' pattern strings
        getLooseApplications(entryConfigs);
        // Add other resources' pattern strings
        // ...
    }

    private void getLooseApplications(List<ArchiveEntryConfig> entryConfigs) throws IOException {
        looseFiles.addAll(ProcessorUtils.getLooseConfigFiles(bootProps));

        Iterator<File> it = looseFiles.iterator();
        while (it.hasNext()) {
            File lf = it.next();
            LooseConfig looseConfig = null;
            try {
                looseConfig = ProcessorUtils.convertToLooseConfig(lf);
                if (looseConfig != null) {
                    try {
                        entryConfigs.addAll(ProcessorUtils.createLooseExpandedArchiveEntryConfigs(looseConfig, lf, bootProps, packageArchiveEntryPrefix,
                                                                                                  includeUsr(options.get(PackageOption.INCLUDE))));

                    } catch (FileNotFoundException e) {
                        // If any exception occurs when creating loose file archive, just skip it and create the next one.
                        System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("warning.unableToPackageLooseConfigFileMissingPath"), lf));
                        Debug.printStackTrace(e);
                        it.remove();
                    }
                } else {
                    it.remove();
                }
            } catch (Exception e) {
                // If any exception occurs when parsing a loose file, just skip it and parse next loose file.
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("warn.package.invalid.looseFile"), lf));
                Debug.printStackTrace(e);
                it.remove();
            }
        }
    }

    private ReturnCode restoreWebSphereApplicationServerProperty(File wlpRoot) {
        File propertyBackupFile = new File(workAreaTmpDir, wlpPropertyBackup);
        File propertyFile = new File(wlpRoot, wlpProperty);
        if (propertyBackupFile != null && propertyBackupFile.exists() && propertyFile != null && propertyFile.exists()) {
            Properties wlpProp = new Properties();
            FileInputStream fio = null;
            FileOutputStream propertyOutput = null;
            try {
                fio = new FileInputStream(propertyBackupFile);
                wlpProp.load(fio);
                propertyOutput = new FileOutputStream(propertyFile);
                wlpProp.store(propertyOutput, null);
            } catch (Exception e) {
                Debug.printStackTrace(e);
                return ReturnCode.RUNTIME_EXCEPTION;
            } finally {
                if (fio != null) {
                    try {
                        fio.close();
                    } catch (IOException e) {
                        Debug.printStackTrace(e);
                        return ReturnCode.RUNTIME_EXCEPTION;
                    }
                }
                if (propertyOutput != null) {
                    try {
                        propertyOutput.close();
                    } catch (IOException e) {
                        Debug.printStackTrace(e);
                        return ReturnCode.RUNTIME_EXCEPTION;
                    }
                }
                propertyBackupFile.delete();
            }
        }
        return ReturnCode.OK;
    }

    private ReturnCode backupWebSphereApplicationServerProperty(File wlpRoot) {
        File propertyFile = new File(wlpRoot, wlpProperty);
        if (propertyFile != null && propertyFile.exists()) {
            File propertyBackupFile = new File(workAreaTmpDir, wlpPropertyBackup);
            Properties wlpProp = new Properties();
            FileInputStream fio = null;
            FileOutputStream propertyOutput = null;
            FileOutputStream propertyBackupFileOutput = null;
            try {
                fio = new FileInputStream(propertyFile);
                wlpProp.load(fio);
                if (wlpProp.getProperty("com.ibm.websphere.productInstallType").equals("Archive")) {
                    return ReturnCode.OK;
                }
                propertyOutput = new FileOutputStream(propertyFile);
                propertyBackupFileOutput = new FileOutputStream(propertyBackupFile);
                wlpProp.store(propertyBackupFileOutput, null);
                wlpProp.setProperty("com.ibm.websphere.productInstallType", "Archive");
                wlpProp.store(propertyOutput, "com.ibm.websphere.productInstallType=InstallationManager");

            } catch (Exception e) {
                Debug.printStackTrace(e);
                return ReturnCode.RUNTIME_EXCEPTION;
            } finally {
                if (fio != null) {
                    try {
                        fio.close();
                    } catch (IOException e) {
                        Debug.printStackTrace(e);
                        return ReturnCode.RUNTIME_EXCEPTION;
                    }
                }
                if (propertyOutput != null) {
                    try {
                        propertyOutput.close();
                    } catch (IOException e) {
                        Debug.printStackTrace(e);
                        return ReturnCode.RUNTIME_EXCEPTION;
                    }
                }
                if (propertyBackupFileOutput != null) {
                    try {
                        propertyBackupFileOutput.close();
                    } catch (IOException e) {
                        Debug.printStackTrace(e);
                        return ReturnCode.RUNTIME_EXCEPTION;
                    }
                }
            }
        }
        return ReturnCode.OK;
    }

    public enum PackageOption {
        INCLUDE;
    }

    // include option values
    public enum IncludeOption {
        ALL("all"), USR("usr"), MINIFY("minify"), WLP("wlp"), RUNNABLE("runnable");
        private final String value;

        private IncludeOption(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public boolean matches(String optionValue) {
            if (optionValue != null) {
                String[] optionValues = optionValue.split(",");
                for (String option : optionValues) {
                    if (option.equalsIgnoreCase(value)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public boolean hasProductExtentions() {
        File prodExtDir = ProcessorUtils.getFileFromDirectory(wlpUserDir.getParentFile(), "/etc/extension");
        if (prodExtDir.exists()) {
            return true;
        } else {
            return false;
        }
    }

    public void setArchivePrefix(String prefix) {
        packageArchiveEntryPrefix = prefix + "/";
        isServerRootOptionSet = true;
    }

    // Check if Java 2 Security is enabled
    private boolean java2SecurityEnabled() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            return true;
        else
            return false;
    }

    private boolean isArchiveJar() {
        return packageFile.getName().endsWith(".jar");
    }

    // Reads the java9.options file
    private HashMap<String, String> readJava9Options() throws IOException {
        HashMap<String, String> hm = new HashMap<String, String>();
        StringBuffer exports = new StringBuffer();
        StringBuffer opens = new StringBuffer();
        BufferedReader r = new BufferedReader(new FileReader(installRoot.getAbsolutePath() + File.separator + "lib" + File.separator + "platform" + File.separator + "java"
                                                             + File.separator
                                                             + "java9.options"));
        String line = r.readLine();
        while (line != null) {
            if (!line.startsWith("#")) {
                if (line.contains("--add-export")) {
                    line = r.readLine();
                    exports.append(getValue(line) + " ");
                } else if (line.contains("--add-open")) {
                    line = r.readLine();
                    opens.append(getValue(line) + " ");
                }
            }
            line = r.readLine();
        }

        hm.put("exports", exports.toString().trim());
        hm.put("opens", opens.toString().trim());

        r.close();

        return hm;
    }

    private String getValue(String line) {
        int loc = line.indexOf("=");
        return line.substring(0, loc);
    }
}