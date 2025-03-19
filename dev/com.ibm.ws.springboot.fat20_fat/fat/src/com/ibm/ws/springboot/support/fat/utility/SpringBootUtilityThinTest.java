/*******************************************************************************
 * Copyright (c) 2016, 2025 IBM Corporation and others.
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
package com.ibm.ws.springboot.support.fat.utility;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.springboot.support.fat.CommonWebServerTests;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@Mode(FULL)
public class SpringBootUtilityThinTest extends CommonWebServerTests {
    private final static String PROPERTY_KEY_INSTALL_DIR = "install.dir";
    private static String SPRING_BOOT_20_BASE_THIN = SPRING_BOOT_20_APP_BASE.substring(0, SPRING_BOOT_20_APP_BASE.length() - 3) + SPRING_APP_TYPE;
    private static String SPRING_BOOT_20_WAR_THIN = SPRING_BOOT_20_APP_WAR.substring(0, SPRING_BOOT_20_APP_WAR.length() - 3) + SPRING_APP_TYPE;
    private static String installDir = null;
    private static boolean wlpLibExtractCreated;
    private String application = SPRING_BOOT_20_APP_BASE;
    private RemoteFile sharedResourcesDir;
    private RemoteFile appsDir;

    @BeforeClass
    public static void setUp() throws Exception {
        installDir = System.setProperty(PROPERTY_KEY_INSTALL_DIR, server.getInstallRoot());
        createWLPLibExtract();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (installDir == null) {
            System.clearProperty(PROPERTY_KEY_INSTALL_DIR);
        } else {
            System.setProperty(PROPERTY_KEY_INSTALL_DIR, installDir);
        }
        installDir = null;

        //Delete the wlp/lib/extract folder
        if (wlpLibExtractCreated) {
            RemoteFile extract = server.getFileFromLibertyInstallRoot("lib/extract");
            extract.delete();
        }
    }

    @Override
    @Before
    public void configureServer() throws Exception {
        // don't do anything other than reset the application and
        // get the shared resources and apps dirs
        application = SPRING_BOOT_20_APP_BASE;
        // make sure the usr/shared/resources folder exists
        sharedResourcesDir = server.getMachine().getFile(server.getFileFromLibertyInstallRoot(""), "usr/shared/resources");
        sharedResourcesDir.mkdirs();
        appsDir = server.getFileFromLibertyServerRoot("apps");
    }

    @After
    public void deleteThinAppsAndStopServer() throws Exception {
        server.getMachine().getFile(appsDir, SPRING_BOOT_20_BASE_THIN).delete();
        server.getMachine().getFile(appsDir, SPRING_BOOT_20_WAR_THIN).delete();
        server.deleteDirectoryFromLibertyServerRoot("apps/" + SPRING_LIB_INDEX_CACHE);
        // note that stop server also deletes the shared and workarea library caches
        String methodName = testName.getMethodName();
        if (methodName != null && methodName.contains(DEFAULT_HOST_WITH_APP_PORT)) {
            super.stopServer(true, "CWWKT0015W");
        } else {
            super.stopServer();
        }
    }

    @Override
    public Set<String> getFeatures() {
        Set<String> features = new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-3.1"));
        String methodName = testName.getMethodName();
        if (methodName != null && methodName.contains(DEFAULT_HOST_WITH_APP_PORT)) {
            features.add("transportSecurity-1.0");
        }
        return features;
    }

    @Override
    public String getApplication() {
        return application;
    }

    @Override
    public String getLogMethodName() {
        return "-" + testName.getMethodName();
    }

    @Override
    public boolean expectApplicationSuccess() {
        String methodName = testName.getMethodName();
        if ("testInvalidLibertyUberJar".equals(methodName) || "testErrorOccursWhenAppNotConfiguredInLibertyUberJar".equals(methodName)) {
            return false;
        }
        return true;
    }

    @Override
    public Map<String, String> getBootStrapProperties() {
        Map<String, String> properties = new HashMap<>(super.getBootStrapProperties());
        String methodName = testName.getMethodName();
        if (methodName != null && methodName.contains(DEFAULT_HOST_WITH_APP_PORT)) {
            properties.put("server.ssl.key-store", "classpath:server-keystore.jks");
            properties.put("server.ssl.key-store-password", "secret");
            properties.put("server.ssl.key-password", "secret");
        }
        properties.put("com.ibm.ws.logging.trace.specification", "*=info:com.ibm.ws.app.manager.springboot.util.SpringBootThinUtil=all");
        return properties;
    }

    @Override
    public boolean useDefaultVirtualHost() {
        String methodName = testName.getMethodName();
        if (methodName != null && methodName.contains(DEFAULT_HOST_WITH_APP_PORT)) {
            return true;
        }
        return false;
    }

    /**
     * As of today, the FAT environment's installation of WLP does not include lib/extract directory.
     * The package command requires that the lib/extract directory exists, as this directory
     * contains a required manifest, self extractable classes, etc. Copy the wlp.lib.extract.jar
     * contents to wlp/lib/extract folder.
     *
     * @throws Exception
     */
    private static void createWLPLibExtract() throws Exception {
        try {
            server.getFileFromLibertyInstallRoot("lib/extract");
            return;
        } catch (FileNotFoundException ex) {
            //expected - the directory does not exist - so proceed.
        }
        RemoteFile libExtractDir = LibertyFileManager.createRemoteFile(server.getMachine(), server.getInstallRoot() + "/lib/extract");
        libExtractDir.mkdirs();

        JarFile libExtractJar = new JarFile("lib/LibertyFATTestFiles/wlp.lib.extract.jar");

        for (Enumeration<JarEntry> entries = libExtractJar.entries(); entries.hasMoreElements();) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            if ("wlp/".equals(entryName) || "wlp/lib/".equals(entryName) || "wlp/lib/extract/".equals(entryName)) {
                continue;
            }
            File libExtractFile = new File(libExtractDir.getAbsolutePath() + "/" + entryName);

            //Jar contains some contents in wlp/lib/extract folder. Copy those contents in libExtractDir directly.
            if (entryName.startsWith("wlp/lib/extract")) {
                libExtractFile = new File(libExtractDir.getAbsolutePath() + "/" + entryName.substring(entryName.lastIndexOf("extract/") + 8));
            }

            if (entryName.endsWith("/")) {
                libExtractFile.mkdirs();
            } else if (!entryName.endsWith("/")) {
                writeFile(libExtractJar, entry, libExtractFile);
            }
        }
        wlpLibExtractCreated = true;
    }

    private static void writeFile(JarFile jar, JarEntry entry, File file) throws IOException, FileNotFoundException {
        try (InputStream is = jar.getInputStream(entry)) {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int read = -1;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
            }
        }
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
        Assert.assertTrue("Failed to move the lib cache to the shared area", libCache.rename(server.getMachine().getFile(sharedResourcesDir, SPRING_LIB_INDEX_CACHE)));

        configureServerThin();
        super.testBasicSpringBootApplication();
    }

    @Test
    public void testSetTargets() throws Exception {
        RemoteFile thinApp = server.getMachine().getFile(server.getFileFromLibertyServerRoot("/"), "thinnedApp." + SPRING_APP_TYPE);

        List<String> cmd = new ArrayList<>();
        cmd.add("thin");
        cmd.add("--sourceAppPath=" + getApplicationFile().getAbsolutePath());
        cmd.add("--targetLibCachePath=" + server.getMachine().getFile(sharedResourcesDir, SPRING_LIB_INDEX_CACHE).getAbsolutePath());
        cmd.add("--targetThinAppPath=" + thinApp.getAbsolutePath());
        List<String> output = SpringBootUtilityScriptUtils.execute(null, cmd);

        Assert.assertTrue("Failed to thin the application: " + output,
                          SpringBootUtilityScriptUtils.findMatchingLine(output, "Thin application: .*thinnedApp\\." + SPRING_APP_TYPE));

        // Move over the thin app to the apps/ folder from the destination.
        Assert.assertTrue("Expected thin app does not exist: " + thinApp.getAbsolutePath(), thinApp.isFile());
        Assert.assertTrue("Failed to move the thinApp to the apps folder", thinApp.rename(server.getMachine().getFile(appsDir, SPRING_BOOT_20_BASE_THIN)));

        configureServerThin();
        super.testBasicSpringBootApplication();
    }

    @Test
    public void testLibertyUberJarThinning() throws Exception {
        String dropinsSpring = "dropins/" + SPRING_APP_TYPE + "/";
        new File(new File(server.getServerRoot()), dropinsSpring).mkdirs();
        RemoteFile thinApp = server.getMachine().getFile(server.getFileFromLibertyServerRoot(dropinsSpring), "springBootApp.jar");

        // NOTE this is mimicking what the boost plugin does when doing a 'package'
        // The current support for thinning Liberty Uber JAR is very limited and expects the
        // single app to be in the dropins/spring/ folder and to already be thinned with
        // a lib.index.cache available in the usr/shared/resources/lib.index.cache/ folder.
        // first need to thin the normal application
        List<String> cmd = new ArrayList<>();
        cmd.add("thin");
        cmd.add("--sourceAppPath=" + getApplicationFile().getAbsolutePath());
        cmd.add("--targetLibCachePath=" + server.getMachine().getFile(sharedResourcesDir, SPRING_LIB_INDEX_CACHE).getAbsolutePath());
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
        Assert.assertTrue("Failed to move the Liberty uber JAR to the apps folder", libertyUberJar.rename(server.getMachine().getFile(appsDir, SPRING_BOOT_20_BASE_THIN)));
        thinApp.delete();

        configureServerThin();
        super.testBasicSpringBootApplication();
    }

    @Test
    public void testDefaultHostWithAppPortRunLibertyUberJarWithSSL() throws Exception {
        String dropinsSpring = "dropins/" + SPRING_APP_TYPE + "/";
        new File(new File(server.getServerRoot()), dropinsSpring).mkdirs();
        RemoteFile thinApp = server.getMachine().getFile(server.getFileFromLibertyServerRoot(dropinsSpring), "springBootApp.jar");

        List<String> cmd = new ArrayList<>();
        cmd.add("thin");
        cmd.add("--sourceAppPath=" + getApplicationFile().getAbsolutePath());
        cmd.add("--targetLibCachePath=" + server.getMachine().getFile(sharedResourcesDir, SPRING_LIB_INDEX_CACHE).getAbsolutePath());
        cmd.add("--targetThinAppPath=" + thinApp.getAbsolutePath());
        List<String> output = SpringBootUtilityScriptUtils.execute(null, cmd);
        dropinFiles.add(thinApp);

        Assert.assertTrue("Failed to thin the application: " + output,
                          SpringBootUtilityScriptUtils.findMatchingLine(output, "Thin application: .*springBootApp\\.jar"));

        Assert.assertTrue("Expected thin app does not exist: " + thinApp.getAbsolutePath(), thinApp.isFile());

        configureBootStrapProperties(true, false);

        ServerConfiguration config = getServerConfiguration();

        //set defaultHttpEndpoint ports to -1 to use application port
        List<HttpEndpoint> endpoints = config.getHttpEndpoints();
        endpoints.clear();
        HttpEndpoint endpoint = new HttpEndpoint();
        endpoints.add(endpoint);
        endpoint.setId("defaultHttpEndpoint");
        endpoint.setHost("*");
        endpoint.setHttpPort("-1");
        endpoint.setHttpsPort("-1");

        server.updateServerConfiguration(config);

        // now create the Liberty uber JAR
        SpringBootUtilityScriptUtils.execute("server", null,
                                             Arrays.asList("package", server.getServerName(), "--include=runnable,minify", "--archive=libertyUber.jar"), false);

        RemoteFile libertyUberJar = server.getFileFromLibertyServerRoot("libertyUber.jar");
        Assert.assertTrue("Expected Liberty uber JAR does not exist: " + libertyUberJar.getAbsolutePath(), libertyUberJar.isFile());

        //Run libertyUberJar using java -jar command
        Process proc = Runtime.getRuntime().exec("java -jar " + libertyUberJar.getAbsolutePath());

        String line = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            line = readTimeout(reader);
            Log.info(getClass(), "testRunLibertyUberJarWithSSL", line);
            while (line != null) {
                Log.info(getClass(), "testRunLibertyUberJarWithSSL", line);
                if (line.contains("CWWKT0016I")) {
                    break;
                }
                line = readTimeout(reader);
            }
        }
        assertNotNull("The endpoint is not available", line);
        assertTrue("Expected log not found", line.contains("CWWKT0016I") && line.contains("default_host"));

        int start = line.indexOf("https");
        String url = line.substring(start);

        String result = sendHttpsGet(url, server);
        assertNotNull(result);
        assertEquals("Expected response not found.", "HELLO SPRING BOOT!!", result);
        proc.destroy();
    }

    String readTimeout(BufferedReader reader) throws IOException {
        // Timeout after 300*1000 ms (5 minutes)
        for (int i = 0; i < 300; i++) {
            if (reader.ready()) {
                return reader.readLine();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        }
        return null;
    }

    private String sendHttpsGet(String path, LibertyServer server) throws Exception {
        String result = null;
        SSLContext sslContext = SSLContext.getInstance("SSL");

        TrustManager[] trustManagers = getTrustManager();
        sslContext.init(null, trustManagers, null);

        URL requestUrl = new URL(path);
        Log.info(getClass(), "sendHttpsGet", requestUrl.toString());

        HttpsURLConnection httpsConn = (HttpsURLConnection) requestUrl.openConnection();
        httpsConn.setSSLSocketFactory(sslContext.getSocketFactory());
        HostnameVerifier hostnamVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        httpsConn.setHostnameVerifier(hostnamVerifier);
        httpsConn.setRequestMethod("GET");
        httpsConn.setDoOutput(false);
        httpsConn.setDoInput(true);

        int code = httpsConn.getResponseCode();
        assertEquals("Expected response code not found.", 200, code);

        BufferedReader in = new BufferedReader(new InputStreamReader(httpsConn.getInputStream()));
        String temp = in.readLine();

        while (temp != null) {
            if (result != null)
                result += temp;
            else
                result = temp;
            temp = in.readLine();
        }
        return result;
    }

    private static TrustManager[] getTrustManager() {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(
                                           java.security.cert.X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(
                                           java.security.cert.X509Certificate[] certs, String authType) {
            }
        } };

        return trustAllCerts;
    }

    @Test
    public void testInvalidLibertyUberJar() throws Exception {
        String dropinsSpring = "dropins/" + SPRING_APP_TYPE + "/";
        new File(new File(server.getServerRoot()), dropinsSpring).mkdirs();
        RemoteFile thinApp = server.getMachine().getFile(server.getFileFromLibertyServerRoot(dropinsSpring), "springBootApp.jar");

        List<String> cmd = new ArrayList<>();
        cmd.add("thin");
        cmd.add("--sourceAppPath=" + getApplicationFile().getAbsolutePath());
        //Put lib.index.cache in wrong location
        cmd.add("--targetLibCachePath=" + server.getMachine().getFile(sharedResourcesDir, "libraries/" + SPRING_LIB_INDEX_CACHE).getAbsolutePath());
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
        Assert.assertTrue("Failed to move the Liberty uber JAR to the apps folder", libertyUberJar.rename(server.getMachine().getFile(appsDir, SPRING_BOOT_20_BASE_THIN)));
        thinApp.delete();

        configureServerThin();
        List<String> logMessages = server.findStringsInLogs("CWWKC0265E");
        assertTrue("Expected log not found containing CWWKC0265E", !logMessages.isEmpty());
        assertTrue("Expected error message CWWKC0265E not found", logMessages.get(0).contains("CWWKC0265E"));
        server.stopServer(false, "CWWKZ0002E", "CWWKC0265E");
        server.deleteDirectoryFromLibertyInstallRoot("usr/shared/resources/libraries/");
    }

    @Test
    public void testErrorOccursWhenAppNotConfiguredInLibertyUberJar() throws Exception {
        //Configure app in wrong location
        String dropinsSpring = "thin/";
        new File(new File(server.getServerRoot()), dropinsSpring).mkdirs();
        RemoteFile thinApp = server.getMachine().getFile(server.getFileFromLibertyServerRoot(dropinsSpring), "springBootApp.jar");

        List<String> cmd = new ArrayList<>();
        cmd.add("thin");
        cmd.add("--sourceAppPath=" + getApplicationFile().getAbsolutePath());
        cmd.add("--targetLibCachePath=" + server.getMachine().getFile(sharedResourcesDir, SPRING_LIB_INDEX_CACHE).getAbsolutePath());
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
        Assert.assertTrue("Failed to move the Liberty uber JAR to the apps folder", libertyUberJar.rename(server.getMachine().getFile(appsDir, SPRING_BOOT_20_BASE_THIN)));
        thinApp.delete();

        configureServerThin();
        List<String> logMessages = server.findStringsInLogs("CWWKC0266E");
        assertTrue("Expected log not found containing CWWKC0266E", !logMessages.isEmpty());
        assertTrue("Expected error message CWWKC0266E not found", logMessages.get(0).contains("CWWKC0266E"));
        server.stopServer(false, "CWWKZ0002E", "CWWKC0266E");
        server.deleteDirectoryFromLibertyServerRoot("thin/");
    }

    @Test
    public void testParentCache() throws Exception {
        // prime the parent lib cache
        RemoteFile parentLibCache = server.getMachine().getFile(sharedResourcesDir, SPRING_LIB_INDEX_CACHE);
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
