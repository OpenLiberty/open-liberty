/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import componenttest.annotation.SkipIfSysProp;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Parameterized.class)
public class PackageLooseContentsTest extends AbstractLooseConfigTest {

    // The full list of available configurations is:
    //   DefaultArchive.war.xml
    //   SimpleElements.war.xml
    //   ArchivedElements.war.xml
    //   EarArchive.ear.xml
    // Plus:
    //   SkipInvalidEntries.war.xml
    //   EmptyArchive.war.xml

    private static final List<Object[]> CONFIGS = new ArrayList<>(5);
    static {
        CONFIGS.add(new Object[] { "DefaultArchive.war.xml", "true" });
        CONFIGS.add(new Object[] { "SimpleElements.war.xml", "true" });
        CONFIGS.add(new Object[] { "ArchivedElements.war.xml", "true" });
        CONFIGS.add(new Object[] { "EarArchive.ear.xml", "true" });

        CONFIGS.add(new Object[] { "SkipInvalidEntries.war.xml", "false" });
        CONFIGS.add(new Object[] { "EmptyArchive.war.xml", "false" });
    };

    @Parameters
    public static Collection<Object[]> data() {
        return CONFIGS;
    }

    private final String moduleLooseConfig;
    private String moduleLooseConfigPath;
    private final String moduleArchiveName;
    private String moduleExpandedPath;
    private final boolean verifyApp;

    public PackageLooseContentsTest(String config, String verifyApp) {
        this.moduleLooseConfig = config;
        this.moduleLooseConfigPath = null;
        this.moduleArchiveName = config.substring(0, config.lastIndexOf('.'));
        this.moduleExpandedPath = null;

        this.verifyApp = Boolean.valueOf(verifyApp);
    }

    //

    @Override
    public String getAppsTargetDir() {
        return APPS_DIR;
    }

    //

    @Rule
    public TestName testName = new TestName();

    @Before
    public void before() throws Exception {
        System.out.println(testName.getMethodName());

        setServer(LibertyServerFactory.getLibertyServer(SERVER_NAME));

        String appsPath = getServer().getServerRoot() + '/' + getAppsTargetDir() + '/';
        moduleLooseConfigPath = appsPath + moduleLooseConfig;
        moduleExpandedPath = appsPath + "expanded/" + moduleArchiveName;

        System.out.println("  Module loose config: " + moduleLooseConfig);
        System.out.println("  Module loose config path: " + moduleLooseConfigPath);
        System.out.println("  Module archive: " + moduleArchiveName);
        System.out.println("  Module expanded path: " + moduleExpandedPath);

        getServer().deleteFileFromLibertyServerRoot(moduleArchiveName);
    }

    @After
    public void clean() throws Exception {
        new File(moduleLooseConfigPath).delete();
    }

    //

    @Test
    @SkipIfSysProp("os.name=z/OS") // ZipFile verifyContents code cant open pax on Z/OS
    public void testUsr() throws Exception {
        String[] packageCmd = new String[] {
                                             "--archive=" + SERVER_NAME,
                                             "--include=usr",
                                             "--server-root=" + SERVER_ROOT };
        String archivePath = packageServer(moduleLooseConfig, SERVER_NAME_ZIP, packageCmd);
        // Because server-root and include=usr are specified,
        // packaging shifts the server folder up one directory.  The
        // 'usr' directory is excised from the path.
        verifyContents(archivePath,
                       SERVER_ROOT, !INCLUDE_USR, SERVER_NAME,
                       moduleArchiveName, verifyApp);
    }

    @Override
    protected void verifyContents(
                                  String archivePath,
                                  String serverRoot, boolean includeUsr, String serverName,
                                  String moduleName, boolean verifyApp) throws IOException {

        String methodName = "verifyContents";

        super.verifyContents(archivePath,
                             serverRoot, includeUsr, serverName,
                             moduleName, verifyApp);

        if (!verifyApp) {
            return;
        }

        String packedPath = serverRoot;
        if (includeUsr) {
            packedPath += "/usr";
        }
        packedPath += "/servers/" + serverName + '/' +
                      getAppsTargetDir() + '/' +
                      moduleName;

        String unpackedPrefix = serverRoot;
        if (includeUsr) {
            unpackedPrefix += "/usr";
        }
        unpackedPrefix += "/servers/" + serverName + '/' +
                          getAppsTargetDir() + "/expanded/" +
                          moduleName + '/';
        int unpackedPrefixLen = unpackedPrefix.length();

        System.out.println(methodName + ":  Packed archive [ " + packedPath + " ]");
        System.out.println(methodName + ":  Unpacked archive [ " + unpackedPrefix + " ]");

        Map<String, Integer> packedMapping = null;
        Map<String, Integer> unpackedMapping = null;

        try (ZipFile packageZip = new ZipFile(archivePath)) {
            int unpackedOffset = 0;

            String lastEntry = null;
            int lastSlash = -1;

            Enumeration<? extends ZipEntry> entries = packageZip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                int slash = entryName.lastIndexOf('/');
                boolean doLog = ((lastEntry == null) ||
                                 (slash != lastSlash) ||
                                 !entryName.regionMatches(0, lastEntry, 0, lastSlash));
                if (doLog) {
                    lastEntry = entryName;
                    lastSlash = slash;
                    System.out.println("Entry [ " + entryName + " ]");
                }

                if (entryName.equals(packedPath)) {
                    if (packedMapping != null) {
                        fail("Archive [ " + archivePath + " ] has duplicates of entry [ " + packedPath + " ]");
                        return;
                        // Never used; added to avoid a compiler null value warning:
                        // The compiler doesn't know that 'fail' never returns.
                    }
                    packedMapping = new HashMap<String, Integer>();

                    try (InputStream nestedStream = packageZip.getInputStream(entry);
                                    ZipInputStream nestedZipStream = new ZipInputStream(nestedStream);) {

                        ZipEntry nestedEntry;
                        for (int offset = 0; (nestedEntry = nestedZipStream.getNextEntry()) != null; offset++) {
                            packedMapping.put(nestedEntry.getName(), Integer.valueOf(offset));
                        }
                    }

                } else {
                    // '<=' is deliberate: We don't want the entry for
                    // the directory of the unpacked archive.
                    if (entryName.length() <= unpackedPrefixLen) {
                        // ignore this entry
                    } else if (entryName.startsWith(unpackedPrefix)) {
                        if (unpackedMapping == null) {
                            unpackedMapping = new HashMap<String, Integer>();
                        }
                        String suffix = entryName.substring(unpackedPrefix.length());
                        unpackedMapping.put(suffix, Integer.valueOf(unpackedOffset++));
                    } else {
                        // ignore this entry ...
                    }
                }
            }
        }

        if (unpackedMapping == null) {
            fail("Archive [ " + archivePath + " ] has no unpacked module entries [ " + unpackedPrefix + " ]");
            return;
            // Never used; added to avoid a compiler null value warning:
            // The compiler doesn't know that 'fail' never returns.
        }

        if (packedMapping == null) {
            fail("Archive [ " + archivePath + " ] has no packed module [ " + packedPath + " ]");
            return;
            // Never used; added to avoid a compiler null value warning:
            // The compiler doesn't know that 'fail' never returns.
        }

        int failures = 0;

        for (Map.Entry<String, Integer> packedEntry : packedMapping.entrySet()) {
            String packedName = packedEntry.getKey();
            Integer packedOffset = packedEntry.getValue();

            Integer unpackedOffset = unpackedMapping.get(packedName);

            if (unpackedOffset == null) {
                System.out.println("Extra packed entry [ " + packedName + " ]");
                failures++;
            } else {
                if (packedOffset.intValue() != unpackedOffset.intValue()) {
                    System.out.println("Packed entry [ " + packedName + " ] changed offset from [ " + packedOffset.intValue() + " ] to [ " + unpackedOffset.intValue() + " ]");
                    failures++;
                }
            }
        }

        for (String unpackedName : unpackedMapping.keySet()) {
            if (!packedMapping.containsKey(unpackedName)) {
                System.out.println("Extra unpacked entry [ " + unpackedName + " ]");
                failures++;
            } else {
                // The offsets were already verified
            }
        }

        if (failures != 0) {
            fail("Archive [ " + archivePath + " ] packed archive [ " + packedPath + " ] has [ " + failures + " ] content errors");
        }
    }
}