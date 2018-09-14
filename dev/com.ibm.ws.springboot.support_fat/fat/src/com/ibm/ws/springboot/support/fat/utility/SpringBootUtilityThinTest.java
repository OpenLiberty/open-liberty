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
package com.ibm.ws.springboot.support.fat.utility;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.ws.springboot.support.fat.CommonWebServerTests;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

@RunWith(FATRunner.class)
@Mode(FULL)
public class SpringBootUtilityThinTest extends CommonWebServerTests {
    private final static String PROPERTY_KEY_INSTALL_DIR = "install.dir";
    private static String SPRING_BOOT_20_BASE_THIN = SPRING_BOOT_20_APP_BASE.substring(0, SPRING_BOOT_20_APP_BASE.length() - 3) + SPRING_APP_TYPE;
    private static String SPRING_BOOT_20_WAR_THIN = SPRING_BOOT_20_APP_WAR.substring(0, SPRING_BOOT_20_APP_WAR.length() - 3) + SPRING_APP_TYPE;
    private static String installDir = null;
    private String application = SPRING_BOOT_20_APP_BASE;
    private RemoteFile sharedResourcesDir;
    private RemoteFile appsDir;

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-3.1"));
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.springboot.support.fat.AbstractSpringTests#getApplication()
     */
    @Override
    public String getApplication() {
        return application;
    }

    @BeforeClass
    public static void getInstallDir() {
        installDir = System.setProperty(PROPERTY_KEY_INSTALL_DIR, server.getInstallRoot());
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

    @Override
    @Before
    public void configureServer() throws Exception {
        // don't do anything other than reset the application and
        // get the shared resources and apps dirs
        application = SPRING_BOOT_20_APP_BASE;
        // make sure the usr/shared/resources folder exists
        sharedResourcesDir = new RemoteFile(server.getFileFromLibertyInstallRoot(""), "usr/shared/resources");
        sharedResourcesDir.mkdirs();
        appsDir = server.getFileFromLibertyServerRoot("apps");
    }

    @Override
    public String getLogMethodName() {
        return "-" + testName.getMethodName();
    }

    @After
    public void deleteThinAppsAndStopServer() throws Exception {
        new RemoteFile(appsDir, SPRING_BOOT_20_BASE_THIN).delete();
        new RemoteFile(appsDir, SPRING_BOOT_20_WAR_THIN).delete();
        server.deleteDirectoryFromLibertyServerRoot("apps/" + SPRING_LIB_INDEX_CACHE);
        // note that stop server also deletes the shared and workarea library caches
        stopServer();
    }

    public void configureServerThin() throws Exception {
        // now really configure
        application = SPRING_BOOT_20_BASE_THIN;
        super.configureServer();
    }

    @Test
    public void testDefaultTargets() throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add("thin");
        cmd.add("--sourceAppPath=" + getApplicationFile().getAbsolutePath());
        List<String> output = SpringBootUtilityScriptUtils.execute(null, cmd);

        Assert.assertTrue("Failed to thin the application: " + output,
                          SpringBootUtilityScriptUtils.findMatchingLine(output, "Thin application: .*\\." + SPRING_APP_TYPE));

        // Move over the lib index from the default location it got stored
        RemoteFile libCache = server.getFileFromLibertyServerRoot("apps/" + SPRING_LIB_INDEX_CACHE);
        Assert.assertTrue("Expected lib cache does not exist: " + libCache.getAbsolutePath(), libCache.isDirectory());
        Assert.assertTrue("Failed to move the lib cache to the shared area", libCache.rename(new RemoteFile(sharedResourcesDir, SPRING_LIB_INDEX_CACHE)));

        configureServerThin();
        super.testBasicSpringBootApplication();
    }

    @Test
    public void testSetTargets() throws Exception {
        RemoteFile thinApp = new RemoteFile(server.getFileFromLibertyServerRoot("/"), "thinnedApp." + SPRING_APP_TYPE);

        List<String> cmd = new ArrayList<>();
        cmd.add("thin");
        cmd.add("--sourceAppPath=" + getApplicationFile().getAbsolutePath());
        cmd.add("--targetLibCachePath=" + new RemoteFile(sharedResourcesDir, SPRING_LIB_INDEX_CACHE).getAbsolutePath());
        cmd.add("--targetThinAppPath=" + thinApp.getAbsolutePath());
        List<String> output = SpringBootUtilityScriptUtils.execute(null, cmd);

        Assert.assertTrue("Failed to thin the application: " + output,
                          SpringBootUtilityScriptUtils.findMatchingLine(output, "Thin application: .*thinnedApp\\." + SPRING_APP_TYPE));

        // Move over the thin app to the apps/ folder from the destination.
        Assert.assertTrue("Expected thin app does not exist: " + thinApp.getAbsolutePath(), thinApp.isFile());
        Assert.assertTrue("Failed to move the thinApp to the apps folder", thinApp.rename(new RemoteFile(appsDir, SPRING_BOOT_20_BASE_THIN)));

        configureServerThin();
        super.testBasicSpringBootApplication();
    }

    @Test
    public void testLibertyUberJarThinning() throws Exception {
        String dropinsSpring = "dropins/" + SPRING_APP_TYPE + "/";
        new File(new File(server.getServerRoot()), dropinsSpring).mkdirs();
        RemoteFile thinApp = new RemoteFile(server.getFileFromLibertyServerRoot(dropinsSpring), "springBootApp.jar");

        // NOTE this is mimicking what the boost plugin does when doing a 'package'
        // The current support for thinning Liberty Uber JAR is very limited and expects the
        // single app to be in the dropins/spring/ folder and to already be thinned with
        // a lib.index.cache available in the usr/shared/resources/lib.index.cache/ folder.
        // first need to thin the normal application
        List<String> cmd = new ArrayList<>();
        cmd.add("thin");
        cmd.add("--sourceAppPath=" + getApplicationFile().getAbsolutePath());
        cmd.add("--targetLibCachePath=" + new RemoteFile(sharedResourcesDir, SPRING_LIB_INDEX_CACHE).getAbsolutePath());
        cmd.add("--targetThinAppPath=" + thinApp.getAbsolutePath());
        List<String> output = SpringBootUtilityScriptUtils.execute(null, cmd);
        dropinFiles.add(thinApp);

        Assert.assertTrue("Failed to thin the application: " + output,
                          SpringBootUtilityScriptUtils.findMatchingLine(output, "Thin application: .*springBootApp\\.jar"));

        Assert.assertTrue("Expected thin app does not exist: " + thinApp.getAbsolutePath(), thinApp.isFile());

        // now create the Liberty uber JAR
        SpringBootUtilityScriptUtils.execute("server", null,
                                             Arrays.asList("package", server.getServerName(), "--include=runnable,minify", "--archive=libertyUber.jar"), false);

        RemoteFile libertyUberJar = server.getFileFromLibertyServerRoot("libertyUber.jar");
        // Move over the Liberty uber JAR to apps/ folder using the thin app name
        Assert.assertTrue("Expected Liberty uber JAR does not exist: " + libertyUberJar.getAbsolutePath(), libertyUberJar.isFile());
        Assert.assertTrue("Failed to move the Liberty uber JAR to the apps folder", libertyUberJar.rename(new RemoteFile(appsDir, SPRING_BOOT_20_BASE_THIN)));
        thinApp.delete();

        configureServerThin();
        super.testBasicSpringBootApplication();
    }

    @Test
    public void testParentCache() throws Exception {
        // prime the parent lib cache
        RemoteFile parentLibCache = new RemoteFile(sharedResourcesDir, SPRING_LIB_INDEX_CACHE);
        List<String> cmd = new ArrayList<>();
        cmd.add("thin");
        cmd.add("--sourceAppPath=" + getApplicationFile().getAbsolutePath());
        cmd.add("--targetLibCachePath=" + parentLibCache.getAbsolutePath());
        List<String> output = SpringBootUtilityScriptUtils.execute(null, cmd);

        Assert.assertTrue("Thin application message not found: " + output,
                          SpringBootUtilityScriptUtils.findMatchingLine(output, "Thin application: .*\\." + SPRING_APP_TYPE));

        // run command again using the primed parent cache
        cmd = new ArrayList<>();
        cmd.add("thin");
        cmd.add("--sourceAppPath=" + getApplicationFile().getAbsolutePath());
        cmd.add("--parentLibCachePath=" + parentLibCache.getAbsolutePath());
        output = SpringBootUtilityScriptUtils.execute(null, cmd);

        // the generated lib cache should be empty since we are using a parent cache that has all the libraries
        RemoteFile libCache = server.getFileFromLibertyServerRoot("apps/" + SPRING_LIB_INDEX_CACHE);
        Assert.assertTrue("Expected lib cache does not exist: " + libCache.getAbsolutePath(), libCache.isDirectory());
        Assert.assertEquals("Lib Cache should be empty.", 0, libCache.list(false).length);

        Assert.assertTrue("Thin application message not found: " + output,
                          SpringBootUtilityScriptUtils.findMatchingLine(output, "Thin application: .*\\." + SPRING_APP_TYPE));
    }

    @Test
    public void testThinWarRemovesLibProvided() throws Exception {
        RemoteFile warApp = server.getFileFromLibertyServerRoot("apps/" + SPRING_BOOT_20_APP_WAR);
        List<String> cmd = new ArrayList<>();
        cmd.add("thin");
        cmd.add("--sourceAppPath=" + warApp.getAbsolutePath());
        List<String> output = SpringBootUtilityScriptUtils.execute(null, cmd);

        Assert.assertTrue("Thin application message not found: " + output,
                          SpringBootUtilityScriptUtils.findMatchingLine(output, "Thin application: .*\\." + SPRING_APP_TYPE));

        RemoteFile warThin = server.getFileFromLibertyServerRoot("apps/" + SPRING_BOOT_20_WAR_THIN);
        Assert.assertTrue("Thin WAR app does not exist: " + warThin.getAbsolutePath(), warThin.isFile());
        try (JarFile jar = new JarFile(warThin.getAbsolutePath())) {
            for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().startsWith("WEB-INF/lib-provided/")) {
                    Assert.fail("Found lib-provided content: " + entry.getName());
                }
            }
        }
    }
}
