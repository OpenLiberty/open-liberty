/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.KeyStore;
import com.ibm.websphere.simplicity.config.SSL;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.SpringBootApplication;
import com.ibm.websphere.simplicity.config.VirtualHost;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public abstract class AbstractSpringTests {

    @Rule
    public TestName testName = new TestName();

    static enum AppConfigType {
        DROPINS_SPRING,
        DROPINS_ROOT,
        SPRING_BOOT_APP_TAG
    }

    public static final String ID_VIRTUAL_HOST = "springBootVirtualHost-";
    public static final String ID_HTTP_ENDPOINT = "springBootHttpEndpoint-";
    public static final String ID_SSL = "springBootSsl-";
    public static final String ID_KEY_STORE = "springBootKeyStore-";
    public static final String ID_TRUST_STORE = "springBootTrustStore-";

    public static final String SPRING_BOOT_15_APP_BASE = "com.ibm.ws.springboot.support.version15.test.app.jar";
    public static final String SPRING_BOOT_20_APP_WAR = "com.ibm.ws.springboot.support.version20.test.war.app-0.0.1-SNAPSHOT.war";
    public static final String SPRING_BOOT_20_APP_JAVA = "com.ibm.ws.springboot.support.version20.test.java.app-0.0.1-SNAPSHOT.jar";
    public static final String SPRING_BOOT_20_APP_WEBANNO = "com.ibm.ws.springboot.support.version20.test.webanno.app-0.0.1-SNAPSHOT.jar";
    public static final String SPRING_BOOT_20_APP_WEBSOCKET = "com.ibm.ws.springboot.support.version20.test.websocket.app-0.0.1-SNAPSHOT.jar";
    public static final String SPRING_BOOT_20_APP_ACTUATOR = "com.ibm.ws.springboot.support.version20.test.actuator.app-0.0.1-SNAPSHOT.jar";
    public static final String SPRING_BOOT_20_APP_MULTI_CONTEXT = "com.ibm.ws.springboot.support.version20.test.multi.context.app-0.0.1-SNAPSHOT.jar";
    public static final String SPRING_BOOT_20_APP_BASE = "com.ibm.ws.springboot.support.version20.test.app-0.0.1-SNAPSHOT.jar";
    public static final String SPRING_BOOT_20_APP_WEBFLUX = "com.ibm.ws.springboot.support.version20.test.webflux.app-0.0.1-SNAPSHOT.jar";
    public static final String SPRING_BOOT_20_APP_WEBFLUX_WRONG_VERSION = "com.ibm.ws.springboot.support.version20.test.webflux.wrong.version.app-0.0.1-SNAPSHOT.jar";
    public static final String SPRING_BOOT_21_APP_BASE = "com.ibm.ws.springboot.support.version21.test.app-0.0.1-SNAPSHOT.jar";
    public static final String SPRING_BOOT_22_APP_BASE = "com.ibm.ws.springboot.support.version22.test.app-0.0.1-SNAPSHOT.jar";
    public static final String LIBERTY_USE_DEFAULT_HOST = "server.liberty.use-default-host";
    public static final String SPRING_LIB_INDEX_CACHE = "lib.index.cache";
    public static final String SPRING_WORKAREA_DIR = "workarea/spring/";
    public static final String SHARED_SPRING_LIB_INDEX_CACHE = "resources/" + SPRING_LIB_INDEX_CACHE;
    public static final String SPRING_THIN_APPS_DIR = "spring.thin.apps";
    public static final String SPRING_APP_TYPE = "spring";
    public static final int EXPECTED_HTTP_PORT = 8081;
    public static final int DEFAULT_HTTP_PORT;
    public static final int DEFAULT_HTTPS_PORT;
    public static final String javaVersion;
    protected static final String DEFAULT_HOST_WITH_APP_PORT = "DefaultHostWithAppPort";

    public static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.springboot.support.fat.SpringBootTests");
    static {
        DEFAULT_HTTP_PORT = server.getHttpDefaultPort();
        DEFAULT_HTTPS_PORT = server.getHttpDefaultSecurePort();
        // NOTE we set the port to the expected ports according to the test application.properties
        // Tests can change this, but it will be reset by the @After method resetDefaultPorts
        server.setHttpDefaultPort(EXPECTED_HTTP_PORT);
        server.setHttpDefaultSecurePort(EXPECTED_HTTP_PORT);
        javaVersion = System.getProperty("java.version"); // Pre-JDK 9 the java.version is 1.MAJOR.MINOR, post-JDK 9 its MAJOR.MINOR

    }
    public static final AtomicBoolean serverStarted = new AtomicBoolean();
    public static final Collection<RemoteFile> dropinFiles = new ArrayList<>();
    private static final Properties bootStrapProperties = new Properties();
    private static File bootStrapPropertiesFile;
    protected static final List<String> extraServerArgs = new ArrayList<>();

    @AfterClass
    public static void stopServer() throws Exception {
        stopServer(true);
    }

    public static void stopServer(boolean cleanupApps, String... expectedFailuresRegExps) throws Exception {
        extraServerArgs.clear();
        boolean isActive = serverStarted.getAndSet(false);
        try {
            // don't archive until after stopping and removing the lib.index.cache
            if (isActive) {
                server.stopServer(false, expectedFailuresRegExps);
            }
        } finally {
            try {
                if (cleanupApps) {
                    server.deleteDirectoryFromLibertyServerRoot(SPRING_WORKAREA_DIR + SPRING_LIB_INDEX_CACHE);
                    for (RemoteFile remoteFile : dropinFiles) {
                        remoteFile.delete();
                    }

                    server.deleteDirectoryFromLibertyInstallRoot("usr/shared/" + SHARED_SPRING_LIB_INDEX_CACHE);
                }
                // always clear bootstrap.properties
                bootStrapProperties.clear();
            } catch (Exception e) {
                // ignore
            } finally {
                if (cleanupApps) {
                    dropinFiles.clear();
                }
                if (isActive) {
                    server.postStopServerArchive();
                }
                // always clear logs after archiving
                server.deleteDirectoryFromLibertyServerRoot("logs/");
            }
        }
    }

    public abstract Set<String> getFeatures();

    public abstract String getApplication();

    public RemoteFile getApplicationFile() throws Exception {
        return server.getFileFromLibertyServerRoot("apps/" + getApplication());
    }

    public AppConfigType getApplicationConfigType() {
        return AppConfigType.DROPINS_SPRING;
    }

    public Map<String, String> getBootStrapProperties() {
        return Collections.emptyMap();
    }

    private Map<String, String> getDefaultBootStrapProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("bootstrap.include", "../testports.properties");
        properties.put("websphere.java.security.exempt", "true");
        return properties;
    }

    public boolean expectApplicationSuccess() {
        return true;
    }

    public boolean expectWebApplication() {
        return true;
    }

    public void modifyAppConfiguration(SpringBootApplication appConfig) {
        // do nothing by default
    }

    public void modifyServerConfiguration(ServerConfiguration config) {
        // do nothing by default
    }

    public boolean useDefaultVirtualHost() {
        return false;
    }

    public String getLogMethodName() {
        return "";
    }

    public int getDropinCopyNum() {
        return 0;
    }

    @Before
    public void configureServer() throws Exception {
        System.out.println("Configuring server for " + testName.getMethodName());
        if (serverStarted.compareAndSet(false, true)) {
            server.setExtraArgs(extraServerArgs);

            ServerConfiguration config = getServerConfiguration();

            RemoteFile appFile = getApplicationFile();
            boolean dropinsTest = false;
            switch (getApplicationConfigType()) {
                case DROPINS_SPRING: {
                    String dropinsSpring = "dropins/" + SPRING_APP_TYPE + "/";
                    new File(new File(server.getServerRoot()), dropinsSpring).mkdirs();
                    appFile.copyToDest(server.getFileFromLibertyServerRoot(dropinsSpring));
                    RemoteFile dest = new RemoteFile(server.getFileFromLibertyServerRoot(dropinsSpring), appFile.getName());
                    dropinFiles.add(dest);
                    dropinsTest = true;
                    break;
                }
                case DROPINS_ROOT: {
                    new File(new File(server.getServerRoot()), "dropins/").mkdirs();
                    String appName = appFile.getName();
                    appName = appName.substring(0, appName.length() - 3) + SPRING_APP_TYPE;
                    RemoteFile dest = new RemoteFile(server.getFileFromLibertyServerRoot("dropins/"), appName);
                    appFile.copyToDest(dest);
                    dropinFiles.add(dest);

                    int copyNum = getDropinCopyNum();
                    for (int i = 0; i < copyNum; i++) {
                        int lastDot = dest.getName().lastIndexOf(".");
                        String copyName = "app.copy" + i + appName.substring(lastDot);
                        RemoteFile copyDest = new RemoteFile(server.getFileFromLibertyServerRoot("dropins/"), copyName);
                        appFile.copyToDest(copyDest);
                        dropinFiles.add(copyDest);
                    }
                    dropinsTest = true;
                    break;
                }
                case SPRING_BOOT_APP_TAG: {
                    SpringBootApplication app = new SpringBootApplication();
                    app.setLocation(appFile.getName());
                    app.setName("testName");
                    modifyAppConfiguration(app);
                    if (!useDefaultVirtualHost()) {
                        app.getApplicationArguments().add("--" + LIBERTY_USE_DEFAULT_HOST + "=false");
                    }
                    config.getSpringBootApplications().add(app);
                    break;
                }
                default:
                    break;
            }
            configureBootStrapProperties(dropinsTest);
            modifyServerConfiguration(config);
            server.updateServerConfiguration(config);
            String methodName = getLogMethodName();
            String logName = getClass().getSimpleName() + methodName + ".log";
            server.startServer(logName, true, false);

            if (expectApplicationSuccess()) {
                assertNotNull("The application was not installed", server
                                .waitForStringInLog("CWWKZ0001I:.*"));
                if (expectWebApplication()) {
                    String testMethodName = testName.getMethodName();
                    if (testMethodName != null && testMethodName.contains(DEFAULT_HOST_WITH_APP_PORT)) {
                        assertNotNull("The endpoint not available on default_host", server
                                        .waitForStringInLog("CWWKT0016I:.*\\bdefault_host\\b.*"));
                    } else {
                        assertNotNull("The endpoint is not available", server
                                        .waitForStringInLog("CWWKT0016I:.*"));
                    }
                }
            }
        }
    }

    @After
    public void resetDefaultPorts() {
        server.setHttpDefaultPort(EXPECTED_HTTP_PORT);
        server.setHttpDefaultSecurePort(EXPECTED_HTTP_PORT);
    }

    protected void configureBootStrapProperties(boolean dropinsTest) throws Exception {
        configureBootStrapProperties(dropinsTest, true);

    }

    protected void configureBootStrapProperties(boolean dropinsTest, boolean addDefaultProps) throws Exception {
        bootStrapPropertiesFile = new File(server.getFileFromLibertyServerRoot("bootstrap.properties").getAbsolutePath());
        if (addDefaultProps) {
            bootStrapProperties.putAll(getDefaultBootStrapProperties());
        }
        bootStrapProperties.putAll(getBootStrapProperties());
        if (dropinsTest && !useDefaultVirtualHost()) {
            bootStrapProperties.put(LIBERTY_USE_DEFAULT_HOST, Boolean.FALSE.toString());
        }
        try (OutputStream out = new FileOutputStream(bootStrapPropertiesFile)) {
            bootStrapProperties.store(out, "");
        }
    }

    protected ServerConfiguration getServerConfiguration() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();

        // START CLEAR out configs from previous tests
        List<SpringBootApplication> applications = config.getSpringBootApplications();
        applications.clear();
        Set<String> features = config.getFeatureManager().getFeatures();
        features.clear();
        features.addAll(getFeatures());
        List<VirtualHost> virtualHosts = config.getVirtualHosts();
        virtualHosts.clear();
        List<HttpEndpoint> endpoints = config.getHttpEndpoints();
        endpoints.clear();
        List<SSL> ssls = config.getSsls();
        ssls.clear();
        List<KeyStore> keystores = config.getKeyStores();
        keystores.clear();
        // END CLEAR

        return config;
    }

}
