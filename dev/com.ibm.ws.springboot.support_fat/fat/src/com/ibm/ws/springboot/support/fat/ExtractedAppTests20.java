/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.ws.springboot.support.fat.utility.SpringBootUtilityScriptUtils;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

@RunWith(FATRunner.class)
@Mode(FULL)
public class ExtractedAppTests20 extends CommonWebServerTests {
    private final static String PROPERTY_KEY_INSTALL_DIR = "install.dir";

    private final static String SPRING_BOOT_20_APP_BASE_THIN = SPRING_BOOT_20_APP_BASE.substring(0, SPRING_BOOT_20_APP_BASE.length() - 3) + SPRING_APP_TYPE;
    private final static String SPRING_BOOT_20_APP_BASE_EXTRACTED = SPRING_BOOT_20_APP_BASE.substring(0, SPRING_BOOT_20_APP_BASE.length() - 3) + "dir";
    private final static String SPRING_BOOT_20_APP_BASE_THIN_EXTRACTED = SPRING_BOOT_20_APP_BASE.substring(0, SPRING_BOOT_20_APP_BASE.length() - 3) + SPRING_APP_TYPE + ".dir";
    private final static String SPRING_BOOT_20_APP_BASE_LOOSE = SPRING_BOOT_20_APP_BASE.substring(0, SPRING_BOOT_20_APP_BASE.length() - 3) + "xml";

    private static String installDir = null;
    private static RemoteFile sharedResourcesDir;
    private static RemoteFile appsDir;

    private String application = null;

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-3.1"));
    }

    @Override
    public String getApplication() {
        return application;
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.SPRING_BOOT_APP_TAG;
    }

    @AfterClass
    public static void resetInstallDir() {
        if (installDir == null) {
            System.clearProperty(PROPERTY_KEY_INSTALL_DIR);
        } else {
            System.setProperty(PROPERTY_KEY_INSTALL_DIR, installDir);
        }
        installDir = null;
    }

    @BeforeClass
    public static void thinAndExtractApplications() throws Exception {
        installDir = System.setProperty(PROPERTY_KEY_INSTALL_DIR, server.getInstallRoot());

        // make sure the usr/shared/resources folder exists
        sharedResourcesDir = new RemoteFile(server.getFileFromLibertyInstallRoot(""), "usr/shared/resources");
        sharedResourcesDir.mkdirs();
        appsDir = server.getFileFromLibertyServerRoot("apps");

        RemoteFile sourceApp = new RemoteFile(server.getFileFromLibertyServerRoot("/apps"), SPRING_BOOT_20_APP_BASE);
        RemoteFile thinApp = new RemoteFile(server.getFileFromLibertyServerRoot("/apps"), SPRING_BOOT_20_APP_BASE_THIN);

        List<String> cmd = new ArrayList<>();
        cmd.add("thin");
        cmd.add("--sourceAppPath=" + sourceApp.getAbsolutePath());
        cmd.add("--targetLibCachePath=" + new RemoteFile(sharedResourcesDir, SPRING_LIB_INDEX_CACHE).getAbsolutePath());
        cmd.add("--targetThinAppPath=" + thinApp.getAbsolutePath());
        List<String> output = SpringBootUtilityScriptUtils.execute(null, cmd);

        Assert.assertTrue("Failed to thin the application",
                          SpringBootUtilityScriptUtils.findMatchingLine(output, "Thin application: .*\\." + SPRING_APP_TYPE));

        RemoteFile baseExtracted = new RemoteFile(server.getFileFromLibertyServerRoot("/apps"), SPRING_BOOT_20_APP_BASE_EXTRACTED);
        extract(sourceApp, baseExtracted);
        extract(thinApp, new RemoteFile(server.getFileFromLibertyServerRoot("/apps"), SPRING_BOOT_20_APP_BASE_THIN_EXTRACTED));
        createLoose(baseExtracted, new RemoteFile(server.getFileFromLibertyServerRoot("/apps"), SPRING_BOOT_20_APP_BASE_LOOSE));
    }

    private static void createLoose(RemoteFile extractedApp, RemoteFile looseApp) throws FileNotFoundException {
        File appFolder = new File(extractedApp.getAbsolutePath());
        String orgPath = new File(appFolder, "org").getAbsolutePath();
        String metaInfPath = new File(appFolder, "META-INF").getAbsolutePath();
        String appClassesPath = new File(appFolder, "BOOT-INF/classes").getAbsolutePath();
        String bootLibPath = new File(appFolder, "BOOT-INF/lib").getAbsolutePath();

        // Very basic loose application that points to content from the extracted app
        StringBuilder builder = new StringBuilder();
        builder.append("<archive>").append('\n');
        addLooseDir(builder, "/META-INF", metaInfPath);
        addLooseDir(builder, "/BOOT-INF/classes", appClassesPath);
        addLooseDir(builder, "/BOOT-INF/lib", bootLibPath);
        addLooseDir(builder, "/org", orgPath);
        builder.append("</archive>").append('\n');

        try (PrintWriter pw = new PrintWriter(looseApp.getAbsolutePath())) {
            pw.write(builder.toString());
        }
    }

    private static void addLooseDir(StringBuilder builder, String target, String source) {
        builder.append("  <dir").append('\n');
        builder.append("     targetInArchive=\"").append(target).append("\"").append('\n');
        builder.append("     sourceOnDisk=\"").append(source).append("\" />").append('\n');
    }

    private static void extract(RemoteFile source, RemoteFile destination) throws Exception {
        destination.mkdirs();
        unzip(source.getAbsolutePath(), destination.getAbsolutePath());
    }

    private static final int BUFFER_SIZE = 4096;

    public static void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                extractFile(zipIn, filePath);
            } else {
                new File(filePath).mkdirs();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    @AfterClass
    public static void deleteThinAndExtractedAppsAndStopServer() throws Exception {
        new RemoteFile(appsDir, SPRING_BOOT_20_APP_BASE_THIN).delete();
        server.deleteDirectoryFromLibertyServerRoot("apps/" + SPRING_BOOT_20_APP_BASE_EXTRACTED);
        server.deleteDirectoryFromLibertyServerRoot("apps/" + SPRING_BOOT_20_APP_BASE_THIN_EXTRACTED);
        server.deleteDirectoryFromLibertyServerRoot("apps/" + SPRING_LIB_INDEX_CACHE);
        server.deleteFileFromLibertyServerRoot("apps/" + SPRING_BOOT_20_APP_BASE_LOOSE);
        // note that stop server also deletes the shared and workarea library caches
        stopServer();
    }

    @Override
    @Before
    public void configureServer() throws Exception {
        // make sure server is stopped
        stopServer(false);
        // don't do anything other than reset the application
        application = null;
    }

    @Override
    public String getLogMethodName() {
        return "-" + testName.getMethodName();
    }

    public void configureServerApp(String app) throws Exception {
        // now really configure
        application = app;
        super.configureServer();
    }

    @Test
    public void testExtractedThinApp() throws Exception {
        configureServerApp(SPRING_BOOT_20_APP_BASE_THIN_EXTRACTED);
        super.testBasicSpringBootApplication();
    }

    @Test
    public void testExtractedFatApp() throws Exception {
        configureServerApp(SPRING_BOOT_20_APP_BASE_EXTRACTED);
        super.testBasicSpringBootApplication();
    }

    @Test
    public void testLooseApp() throws Exception {
        configureServerApp(SPRING_BOOT_20_APP_BASE_LOOSE);
        super.testBasicSpringBootApplication();
    }
}
