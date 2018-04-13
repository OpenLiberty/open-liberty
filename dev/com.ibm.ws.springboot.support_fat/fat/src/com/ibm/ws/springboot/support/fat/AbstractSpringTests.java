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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
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

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.SpringBootApplication;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public abstract class AbstractSpringTests {
    static enum AppConfigType {
        DROPINS_SPR,
        DROPINS_ROOT,
        SPRING_BOOT_APP_TAG
    }

    public static final String SPRING_BOOT_15_APP_BASE = "com.ibm.ws.springboot.support.version15.test.app.jar";
    public static final String SPRING_BOOT_15_APP_WAR = "com.ibm.ws.springboot.support.version15.test.war.app-0.0.1-SNAPSHOT.war";
    public static final String SPRING_BOOT_15_APP_JAVA = "com.ibm.ws.springboot.support.version15.test.java.app.jar";
    public static final String SPRING_BOOT_15_APP_WEBANNO = "com.ibm.ws.springboot.support.version15.test.webanno.app.jar";
    public static final String SPRING_BOOT_20_APP_BASE = "com.ibm.ws.springboot.support.version20.test.app-0.0.1-SNAPSHOT.jar";

    public static final String SPRING_LIB_INDEX_CACHE = "lib.index.cache";
    public static final String SPRING_WORKAREA_DIR = "workarea/spring/";
    public static final String SHARED_SPRING_LIB_INDEX_CACHE = "resources/" + SPRING_LIB_INDEX_CACHE;
    public static final String SPRING_THIN_APPS_DIR = "spring.thin.apps";
    public static final int EXPECTED_HTTP_PORT = 8081;

    public static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.springboot.support.fat.SpringBootTests");
    static {
        // NOTE we set the port to the expected ports according to the test application.properties
        // Tests can change this, but it will be reset by the @After method resetDefaultPorts
        server.setHttpDefaultPort(EXPECTED_HTTP_PORT);
        server.setHttpDefaultSecurePort(EXPECTED_HTTP_PORT);
    }
    public static final AtomicBoolean serverStarted = new AtomicBoolean();
    public static final Collection<RemoteFile> dropinFiles = new ArrayList<>();
    private static final Properties bootStrapProperties = new Properties();
    private static File bootStrapPropertiesFile;

    @AfterClass
    public static void stopServer() throws Exception {
        boolean isActive = serverStarted.getAndSet(false);
        try {
            // don't archive until after stopping and removing the lib.index.cache
            if (isActive) {
                server.stopServer(false);
            }
        } finally {
            try {
                server.deleteDirectoryFromLibertyServerRoot(SPRING_WORKAREA_DIR + SPRING_LIB_INDEX_CACHE);
                for (RemoteFile remoteFile : dropinFiles) {
                    remoteFile.delete();
                }
                server.deleteDirectoryFromLibertyInstallRoot("usr/shared/" + SHARED_SPRING_LIB_INDEX_CACHE);
                //clear bootstrap.properties
                bootStrapProperties.clear();
                try (OutputStream out = new FileOutputStream(bootStrapPropertiesFile)) {
                    bootStrapProperties.store(out, "");
                }
            } catch (Exception e) {
                // ignore
            } finally {
                dropinFiles.clear();
                if (isActive) {
                    server.postStopServerArchive();
                }
            }
        }
    }

    public abstract Set<String> getFeatures();

    public abstract String getApplication();

    public RemoteFile getApplicationFile() throws Exception {
        return server.getFileFromLibertyServerRoot("apps/" + getApplication());
    }

    public AppConfigType getApplicationConfigType() {
        return AppConfigType.DROPINS_SPR;
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

    @Before
    public void configureServer() throws Exception {
        if (serverStarted.compareAndSet(false, true)) {
            configureBootStrapProperties();
            ServerConfiguration config = server.getServerConfiguration();
            List<SpringBootApplication> applications = config.getSpringBootApplications();
            applications.clear();
            Set<String> features = config.getFeatureManager().getFeatures();
            features.clear();
            features.addAll(getFeatures());
            RemoteFile appFile = getApplicationFile();
            switch (getApplicationConfigType()) {
                case DROPINS_SPR: {
                    new File(new File(server.getServerRoot()), "dropins/spr/").mkdirs();
                    appFile.copyToDest(server.getFileFromLibertyServerRoot("dropins/spr/"));
                    RemoteFile dest = new RemoteFile(server.getFileFromLibertyServerRoot("dropins/spr/"), appFile.getName());
                    appFile.copyToDest(dest);
                    dropinFiles.add(dest);
                    break;
                }
                case DROPINS_ROOT: {
                    new File(new File(server.getServerRoot()), "dropins/").mkdirs();
                    String appName = appFile.getName();
                    appName = appName.substring(0, appName.length() - 3) + "spr";
                    RemoteFile dest = new RemoteFile(server.getFileFromLibertyServerRoot("dropins/"), appName);
                    appFile.copyToDest(dest);
                    dropinFiles.add(dest);
                    break;
                }
                case SPRING_BOOT_APP_TAG: {
                    SpringBootApplication app = new SpringBootApplication();
                    app.setLocation(appFile.getName());
                    app.setName("testName");
                    applications.add(app);
                    break;
                }
                default:
                    break;
            }

            server.updateServerConfiguration(config);
            server.startServer(getClass().getSimpleName() + ".log", true, false);

            if (expectApplicationSuccess()) {
                assertNotNull("The application was not installed", server
                                .waitForStringInLog("CWWKZ0001I:.*"));
                if (expectWebApplication()) {
                    assertNotNull("The endpoint is not available", server
                                    .waitForStringInLog("CWWKT0016I:.*"));
                }
            }
        }
    }

    @After
    public void resetDefaultPorts() {
        server.setHttpDefaultPort(EXPECTED_HTTP_PORT);
        server.setHttpDefaultSecurePort(EXPECTED_HTTP_PORT);
    }

    private void configureBootStrapProperties() throws Exception {
        bootStrapPropertiesFile = new File(server.getFileFromLibertyServerRoot("bootstrap.properties").getAbsolutePath());
        try (InputStream in = new FileInputStream(bootStrapPropertiesFile)) {
            bootStrapProperties.load(in);
        }
        bootStrapProperties.putAll(getDefaultBootStrapProperties());
        bootStrapProperties.putAll(getBootStrapProperties());

        try (OutputStream out = new FileOutputStream(bootStrapPropertiesFile)) {
            bootStrapProperties.store(out, "");
        }

    }

}
