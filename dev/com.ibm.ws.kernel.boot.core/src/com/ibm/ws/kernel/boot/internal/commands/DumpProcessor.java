/*******************************************************************************
 * Copyright (c) 2012-2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.archive.Archive;
import com.ibm.ws.kernel.boot.archive.ArchiveEntryConfig;
import com.ibm.ws.kernel.boot.archive.ArchiveFactory;
import com.ibm.ws.kernel.boot.archive.DirEntryConfig;
import com.ibm.ws.kernel.boot.archive.DirPattern.PatternStrategy;
import com.ibm.ws.kernel.boot.archive.FileEntryConfig;
import com.ibm.ws.kernel.boot.archive.FilteredDirEntryConfig;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;

/**
 * execute the dump command
 */
public class DumpProcessor implements ArchiveProcessor {
    private final String serverName;

    private final File dumpFile;

    private final BootstrapConfig bootProps;

    private final List<String> javaDumps;

    public DumpProcessor(String serverName, File dumpFile, BootstrapConfig bootProps, List<String> javaDumps) {
        this.serverName = serverName;
        this.dumpFile = dumpFile;
        this.bootProps = bootProps;
        this.javaDumps = javaDumps;

    }

    public ReturnCode execute() {

        Archive archive = null;
        try {
            // make archive
            archive = ArchiveFactory.create(dumpFile); // The archive will collect the infos from server.out.dir, and the entryPrefix is null
            archive.addEntryConfigs(createDumpConfigs(serverName));
            archive.create();
        } catch (IOException e) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.unableZipDir"), e));
            return ReturnCode.ERROR_SERVER_DUMP;
        } finally {
            // must close the archive so that the create can complete
            Utils.tryToClose(archive);
        }

        return ReturnCode.OK;

    }

    private List<ArchiveEntryConfig> createDumpConfigs(String serverName) throws IOException {
        List<ArchiveEntryConfig> entryConfigs = new ArrayList<ArchiveEntryConfig>();

        // avoid any special characters in serverName when construct patterns
        String regexServerName = Pattern.quote(serverName);

        /*
         * Add server config directory
         */
        File serverConfigDir = new File(bootProps.getUserRoot(), "servers/" + serverName);

        // Add filtered server.xml
        FilteredDirEntryConfig configFiles = new FilteredDirEntryConfig(serverConfigDir, false, PatternStrategy.ExcludePreference);
        entryConfigs.add(configFiles);
        configFiles.include(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + ".*\\.xml"));
        configFiles.include(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + ".*\\.properties"));

        // Include filtered configDropins
        configFiles.include(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "configDropins"));

        // Exclude dump directory
        configFiles.exclude(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "dump_" + REGEX_TIMESTAMP));
        configFiles.exclude(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "autopd"));

        DirEntryConfig serverConfigDirConfig = new DirEntryConfig("", serverConfigDir, true, PatternStrategy.ExcludePreference);
        entryConfigs.add(serverConfigDirConfig);

        // Exclude user apps
        serverConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "dropins"));
        serverConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "apps"));

        // Exclude security-sensitive files under resources/security.
        serverConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + "resources" + REGEX_SEPARATOR + "security"));
        // As a best effort, try to avoid packaging security-sensitive .jks and .p12 files.
        serverConfigDirConfig.exclude(Pattern.compile("\\.jks$"));
        serverConfigDirConfig.exclude(Pattern.compile("\\.p12$"));

        // {server.config.dir} may be equal {server.output.dir}, so let's first exclude
        // Exclude dump directory
        serverConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "dump_" + REGEX_TIMESTAMP));
        serverConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "autopd"));
        // Exclude workarea and logs directoriese
        serverConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "logs"));
        serverConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "workarea"));
        // Exclude server package and dump files.
        serverConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexServerName + "\\.(zip|pax)$"));
        serverConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexServerName + "\\.dump-" + REGEX_TIMESTAMP + "\\.(zip|pax)$"));
        serverConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "core\\.[^\\\\/]+\\.dmp"));
        serverConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "heapdump\\.[^\\\\/]+\\.phd"));
        serverConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "java\\.[^\\\\/]+\\.hprof"));
        serverConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "javacore\\.[^\\\\/]+\\.txt"));
        serverConfigDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "javadump\\.[^\\\\/]+\\.txt"));

        /*
         * Add server output directory
         */
        File serverOutputDir = bootProps.getOutputFile(null);
        DirEntryConfig serverOutputDirConfig = new DirEntryConfig("", serverOutputDir, false, PatternStrategy.ExcludePreference);
        entryConfigs.add(serverOutputDirConfig);

        serverOutputDirConfig.include(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "dump_" + REGEX_TIMESTAMP));
        serverOutputDirConfig.include(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "autopd"));
        serverOutputDirConfig.include(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "logs"));
        serverOutputDirConfig.include(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "workarea"));
        serverOutputDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "workarea" + REGEX_SEPARATOR + "\\.sLock$"));
        serverOutputDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "workarea" + REGEX_SEPARATOR + "\\.sCommand$"));
        // As the sub-osgi system will also create some locked files under .manager directory,
        // exclude all the org.eclipse.osgi/.manager/*.*
        serverOutputDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "workarea" + REGEX_SEPARATOR
                                                      + ".*" + "org\\.eclipse\\.osgi"
                                                      + REGEX_SEPARATOR + "\\.manager"));
        // exclude the cache of an application
        serverOutputDirConfig.exclude(Pattern.compile(REGEX_SEPARATOR + regexServerName + REGEX_SEPARATOR + "workarea" + REGEX_SEPARATOR
                                                      + "org\\.eclipse\\.osgi" + REGEX_SEPARATOR + "bundles" + REGEX_SEPARATOR
                                                      + "\\d+" + REGEX_SEPARATOR + "data" + REGEX_SEPARATOR
                                                      + ".*com\\.ibm\\.ws\\.app\\.manager_gen"));

        // Add any java dumps
        if (!!!javaDumps.isEmpty()) {
            for (String javaDump : javaDumps) {
                File f = new File(javaDump);
                if (f.exists()) {
                    FileEntryConfig feg = new FileEntryConfig("", f);
                    entryConfigs.add(feg);
                } else {
                    System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.missingDumpFile"), javaDump));
                }
            }

        }

        return entryConfigs;

    }
}
