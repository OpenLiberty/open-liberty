/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
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
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.KeyStore;
import com.ibm.websphere.simplicity.config.SSL;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.SpringBootApplication;
import com.ibm.websphere.simplicity.config.VirtualHost;
import com.ibm.websphere.simplicity.config.WebApplication;

import componenttest.containers.TestContainerSuite;
import componenttest.rules.SkipJavaSemeruWithFipsEnabled;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public abstract class AbstractSpringTests extends TestContainerSuite {

    @Rule
    public static final SkipJavaSemeruWithFipsEnabled skipJavaSemeruWithFipsEnabled = new SkipJavaSemeruWithFipsEnabled("SpringBootTests");

    // All current FAT application names.
    public static final String SPRING_BOOT_40_APP_BASE = "io.openliberty.springboot.fat40.app-0.0.1-SNAPSHOT.jar";
    public static final String SPRING_BOOT_40_APP_WAR = "io.openliberty.springboot.fat40.war.app-0.0.1-SNAPSHOT.war";
    public static final String SPRING_BOOT_40_APP_ACTUATOR = "io.openliberty.springboot.fat40.actuator.app-0.0.1-SNAPSHOT.jar";
    public static final String SPRING_BOOT_40_APP_WEBFLUX = "io.openliberty.springboot.fat40.webflux.app-0.0.1-SNAPSHOT.jar";
    public static final String SPRING_BOOT_40_APP_JAVA = "io.openliberty.springboot.fat40.java.app-0.0.1-SNAPSHOT.jar";
    public static final String SPRING_BOOT_40_APP_MULTI_CONTEXT = "io.openliberty.springboot.fat40.multicontext.app-0.0.1-SNAPSHOT.jar";
    public static final String SPRING_BOOT_40_APP_WEBANNO = "io.openliberty.springboot.fat40.webanno.app-0.0.1-SNAPSHOT.jar";
    public static final String SPRING_BOOT_40_APP_WEBSOCKET = "io.openliberty.springboot.fat40.websocket.app-0.0.1-SNAPSHOT.war";
    public static final String SPRING_BOOT_40_APP_SECURITY = "io.openliberty.springboot.fat40.security.app-0.0.1-SNAPSHOT.jar";
    public static final String SPRING_BOOT_40_APP_CONCURRENCY = "io.openliberty.springboot.fat40.concurrency.app-0.0.1-SNAPSHOT.war";
    public static final String SPRING_BOOT_40_APP_MBEAN = "io.openliberty.springboot.fat40.mbean.app-0.0.1-SNAPSHOT.war";
    public static final String SPRING_BOOT_40_APP_JMS = "io.openliberty.springboot.fat40.jms.app-0.0.1-SNAPSHOT.war";
    public static final String SPRING_BOOT_40_APP_TRANSACTIONS = "io.openliberty.springboot.fat40.transactions.app-0.0.1-SNAPSHOT.war";
    public static final String SPRING_BOOT_40_APP_DATA = "io.openliberty.springboot.fat40.data.app-0.0.1-SNAPSHOT.war";
    public static final String SPRING_BOOT_40_APP_VALIDATION = "io.openliberty.springboot.fat40.validation.app-0.0.1-SNAPSHOT.war";
    public static final String SPRING_BOOT_40_NO_CONTEXTROOT_WAR = "io.openliberty.springboot.fat40.http.app-0.0.1-SNAPSHOT.war";
    public static final String SPRING_BOOT_40_WITH_CONTEXTROOT_WAR = "io.openliberty.springboot.fat40.http.contextroot.app-0.0.1-SNAPSHOT.war";
    public static final String SPRING_BOOT_40_APP_AOP = "io.openliberty.springboot.fat40.aop.app-0.0.1-SNAPSHOT.war";
    
    // Various spring configuration property fragments.
    public static final String ID_VIRTUAL_HOST = "springBootVirtualHost-";
    public static final String ID_HTTP_ENDPOINT = "springBootHttpEndpoint-";
    public static final String ID_SSL = "springBootSsl-";
    public static final String ID_KEY_STORE = "springBootKeyStore-";
    public static final String ID_TRUST_STORE = "springBootTrustStore-";

    // Pre-JDK 9 the java.version is 1.MAJOR.MINOR, post-JDK 9 its MAJOR.MINOR
    public static final String javaVersion;
    public static final String SERVER_NAME = "SpringBootTests";
    public static final LibertyServer server;
    public static RemoteFile serverRootFile;
    public static RemoteFile dropinsFile;
    public static int DEFAULT_HTTP_PORT;
    public static int DEFAULT_HTTPS_PORT;
    public static final int EXPECTED_HTTP_PORT = 8081;
    public static final List<RemoteFile> dropinFiles = new ArrayList<>();
    public static final String SPRING_THIN_APPS_DIR = "spring.thin.apps";
    public static final String SPRING_LIB_INDEX_CACHE = "lib.index.cache";
    public static final String SPRING_WORKAREA_DIR = "workarea/spring/";
    public static final String USER_SHARED_DIR = "usr/shared/";
    public static final String SHARED_SPRING_LIB_INDEX_CACHE = "resources/" + SPRING_LIB_INDEX_CACHE;
    public static final AtomicBoolean serverStarted = new AtomicBoolean();
    public static final boolean DO_CLEANUP_APPS = true;
    /**
     * Sub-folder of "dropins" which is used by the spring application
     * when {@link AppConfigType#DROPINS_SPRING} is specified as
     * the application configuration type.
     */
    public static final String SPRING_APP_TYPE = "spring";
    public static final String DROPINS_SPRING_DIR = "dropins/" + SPRING_APP_TYPE + "/";
    /**
     * Fragment test method name: Test methods which include this text fragment
     * have the default host as an expected endpoint. See {@link #ID_DEFAULT_HOST}.
     */
    public static final String DEFAULT_HOST_WITH_APP_PORT = "DefaultHostWithAppPort";
    public static final String ID_DEFAULT_HOST = "default_host";
    /**
     * Application argument used to indicate that a spring application
     * will use the default host. Added as an application argument unless
     * {@link #useDefaultVirtualHost()} is false.
     */
    public static final String LIBERTY_USE_DEFAULT_HOST = "server.liberty.use-default-host";

    /** Control parameter: Tell if default values are added to bootstrap properties. */
    protected static final boolean DO_ADD_DEFAULT_PROPERTIES = true;
    /**
     * Extra arguments which are provided to the server. These are available
     * to subclasses. Set these on the server prior to starting the server,
     * and clear them when stopping the server.
     *
     * The intent is that any <code>@BeforeClass</code> processing will populate
     * the extra arguments before proceeding to start the server.
     */
    protected static final List<String> extraServerArgs = new ArrayList<>();

    private static final Properties bootStrapProperties = new Properties();
    private static ServerConfiguration originalServerConfig;

    static {
        javaVersion = System.getProperty("java.version");
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);

        recordDefaultPorts();
        resetDefaultPorts();
    }

    /**
     * When requested (via <code>@Rule</code>), JUnit injects the test
     * name at the beginning of the test lifecycle. Injection occurs
     * before <code>@Before</code> is invoked.
     *
     * These Spring FAT tests use the injected test name to specialize
     * test behavior. For example, this allows security to be
     * conditionally enabled.
     *
     * TODO: This is a pecululiar way to control test behavior, and is
     * not advised. Using a more direct mechanism would be much better,
     * but is hard to put in at this time.
     */
    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void saveServerConfiguration() throws Exception {
        originalServerConfig = server.getServerConfiguration().clone();
    }

    /**
     * Answer the features which are to be provisioned for this test class.
     *
     * @return The features which are to be provisioned for this test class.
     */
    public abstract Set<String> getFeatures();

    /**
     * Default features for spring web applications.
     *
     * @return Default features for spring web applications.
     *         Currently, "springBoot-4.0" and "servlet-6.1".
     */
    public Set<String> getWebFeatures() {
        Set<String> features = new HashSet<>(2);
        features.add("springBoot-4.0");
        features.add("servlet-6.1");
        return features;
    }

    /**
     * Default bootstrap properties which are used by most spring tests.
     *
     * The default properties contain an include of the standard test
     * port properties file, and include a setting to exempt the tests
     * from using java security.
     */
    private Map<String, String> getDefaultBootStrapProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("bootstrap.include", "../testports.properties");
        properties.put("websphere.java.security.exempt", "true");
        return properties;
    }

    /**
     * Subclass API: Specialize to set specific bootstrap properties
     * for use by a Spring FAT test class.
     *
     * The return value is unmodifiable: Extenders must create and
     * return new storage.
     *
     * @return The bootstrap properties used by this Spring FAT test
     *         class. This default implementation answers an empty table.
     */
    public Map<String, String> getBootStrapProperties() {
        return Collections.emptyMap();
    }

    public static RemoteFile getServerFile(String name) throws Exception {
        return server.getFileFromLibertyServerRoot(name);
    }

    public static RemoteFile getServerRootFile() throws Exception {
        if (serverRootFile == null) {
            serverRootFile = getServerFile("");
        }
        return serverRootFile;
    }

    public static RemoteFile getDropinsFile() throws Exception {
        if (dropinsFile == null) {
            dropinsFile = server.getMachine().getFile(getServerRootFile(), "dropins");
        }
        return dropinsFile;
    }

    // The default values may be changed by specific tests.
    // Record the initial defaults and restore them after each test.
    protected static void recordDefaultPorts() {
        DEFAULT_HTTP_PORT = server.getHttpDefaultPort();
        DEFAULT_HTTPS_PORT = server.getHttpDefaultSecurePort();
    }

    protected static void resetDefaultPorts() {
        server.setHttpDefaultPort(EXPECTED_HTTP_PORT);
        server.setHttpDefaultSecurePort(EXPECTED_HTTP_PORT);
    }

    public static void requireServerMessage(String msg, String regex) {
        assertNotNull(msg, server.waitForStringInLog(regex));
    }

    public static void requireServerTrace(String msg, String regex) {
        String traceMsg = server.waitForStringInTraceUsingLastOffset(regex);
        assertNotNull(msg, traceMsg);
    }

    public static void requireServerTrace(String msg, String regex, long timeout) {
        String traceMsg = server.waitForStringInTraceUsingLastOffset(regex, timeout);
        assertNotNull(msg, traceMsg);
    }

    public static void forbidServerTrace(String msg, String regex, long timeout) {
        String traceMsg = server.waitForStringInTraceUsingLastOffset(regex, timeout);
        assertNull(msg, traceMsg);
    }

    public static Collection<RemoteFile> getDropinFiles() {
        return dropinFiles;
    }

    /**
     * Record that a test class placed a file in the server
     * dropins folder, and that file must be removed at the
     * conclusion of the test class.
     *
     * @param dropinFile A file which is to be recorded.
     */
    public static void recordDropinFile(RemoteFile dropinFile) {
        dropinFiles.add(dropinFile);
    }

    protected static RemoteFile addDropinFile(RemoteFile sourceFile,
                                              RemoteFile destRootFile, String destName) throws Exception {
        RemoteFile destFile = sourceFile.getMachine().getFile(destRootFile, destName);
        sourceFile.copyToDest(destFile);
        recordDropinFile(destFile);
        return destFile;
    }

    /**
     * Delete registered dropin files. Clear the dropin file registry.
     *
     * This is intended to be done at the conclusion of each FAT test class
     * as a part of <code>@AfterClass</code> processing. See {@link #stopServer}.
     *
     * @throws Exception Thrown if a registered file exists and
     *                       could not be deleted.
     */
    protected static void deleteDropinFiles() throws Exception {
        for (RemoteFile remoteFile : getDropinFiles()) {
            if (remoteFile.exists()) {
                remoteFile.delete();
            }
        }
        dropinFiles.clear();
    }

    protected static void deleteSpringCache() throws Exception {
        server.deleteDirectoryFromLibertyServerRoot(SPRING_WORKAREA_DIR + SPRING_LIB_INDEX_CACHE);
        server.deleteDirectoryFromLibertyInstallRoot(USER_SHARED_DIR + SHARED_SPRING_LIB_INDEX_CACHE);
    }

    @Before
    public void configureServer() throws Exception {
        System.out.println("Configuring server for " + testName.getMethodName());

        if (serverStarted.compareAndSet(false, true)) {
            doConfigureServer();
        }
    }

    @After
    public void tearDownTest() {
        resetDefaultPorts();
    }

    /**
     * Optional value to append to the server log name.
     * Added to the base file name.
     *
     * @return An optional value to append to the server log name.
     */
    public String getLogMethodName() {
        return "";
    }

    /**
     * Answer the name of the server log file.
     *
     * This method alwayas answers the simple FAT test class
     * name, plus {@link #getLogMethodName()}, plus ".log".
     *
     * @return The name of the server log file.
     */
    public String getLogName() {
        return getClass().getSimpleName() + getLogMethodName() + ".log";
    }

    public void doConfigureServer() throws Exception {
        server.setExtraArgs(extraServerArgs);

        ServerConfiguration config = getServerConfiguration();

        boolean dropinsTest = doConfigureApplications(config);

        configureBootStrapProperties(dropinsTest);
        modifyServerConfiguration(config);
        server.updateServerConfiguration(config);

        server.startServer(getLogName(), true, false);
        verifyServerStart();
    }

    /**
     * Configure one or more applications for the test class.
     *
     * There are three cases:
     *
     * <ul>
     * <li><code>DROPINS_SPRING</code>: Copy the spring application into
     * the "dropins/spring" folder.
     * </li>
     * <li><code>DROPINS_ROOT</code>: Copy the spring application into the
     * "dropins" folder. Copy additional copies according to the
     * {@link #getDropinCopyNum()} value.
     * </li>
     * <li><code>SPRING_BOOT_APP_TAG</code>: Use the spring application as
     * is in the "apps" folder.
     * </li>
     * </ul>
     * The <code>SPRING_BOOT_APP_TAG</code> case is the only case which where
     * the application may be configured to not use the default host.
     *
     * @param config The server configuration, to which a spring boot application
     *                   will be added if the case is <code>SPRING_BOOT_APP_TAG</code>.
     *
     * @return True or false telling if any applications were placed in the dropins
     *         folder.
     *
     * @throws Exception Thrown if application configuration failed.
     */
    protected boolean doConfigureApplications(ServerConfiguration config) throws Exception {
        RemoteFile appFile = getApplicationFile();

        boolean dropinsTest = false;
        RemoteFile useDropinsFile = getDropinsFile();

        switch (getApplicationConfigType()) {
            case DROPINS_SPRING: {
                dropinsTest = true;
                useDropinsFile = server.getMachine().getFile(useDropinsFile, "spring/");
                useDropinsFile.mkdirs();
                addDropinFile(appFile, useDropinsFile, appFile.getName());
                break;
            }

            case DROPINS_ROOT: {
                dropinsTest = true;
                useDropinsFile.mkdirs();

                String appName = appFile.getName();
                String appHead = appName.substring(0, appName.length() - 3);

                addDropinFile(appFile, useDropinsFile, appHead + SPRING_APP_TYPE);

                int copyNum = getDropinCopyNum();
                for (int copyNo = 0; copyNo < copyNum; copyNo++) {
                    addDropinFile(appFile, dropinsFile, "app.copy" + copyNo + "." + SPRING_APP_TYPE);
                }
                break;
            }
            case DROPINS_ROOT_WAR: {
                dropinsTest = true;
                useDropinsFile.mkdirs();
                String appName = appFile.getName();
                if (!appName.endsWith(".war")) {
                    fail("Wrong application type for WAR dropins test: " + appName);
                }
                addDropinFile(appFile, useDropinsFile, appName);
                break;
            }
            case SPRING_BOOT_APP_TAG: {
                SpringBootApplication app = new SpringBootApplication();
                app.setLocation(appFile.getName());
                app.setName("testName"); // TODO: Is this the most useful name?
                modifyAppConfiguration(app);
                if (!useDefaultVirtualHost()) {
                    app.getApplicationArguments().add("--" + LIBERTY_USE_DEFAULT_HOST + "=false");
                }
                config.getSpringBootApplications().add(app);
                break;
            }
            case WEB_APP_TAG: {
                WebApplication app = new WebApplication();
                app.setLocation(appFile.getName());
                app.setName("testName");
                modifyAppConfiguration(app);
                config.getWebApplications().add(app);
                break;
            }
        }

        return dropinsTest;
    }

    /**
     * Verify the expected server start.
     *
     * Do nothing if the server is not expected to start.
     *
     * Otherwise, verify the application start, verify that all expected
     * endpoints are available, then finally verify that the server
     * itself started.
     */
    public void verifyServerStart() {
        if (expectApplicationSuccess()) {
            verifyApplication();
            verifyEndpoints();
            verifyServer();
        }
    }

    public void verifyApplication() {
        requireServerMessage("The application was not installed", "CWWKZ0001I:.*");
    }

    public void verifyEndpoints() {
        if (!expectWebApplication()) {
            return;
        }

        List<String> expectedEndpoints = getExpectedWebApplicationEndpoints();
        if (!expectedEndpoints.isEmpty()) {
            for (String ep : expectedEndpoints) {
                verifyEndpoint(ep);
            }
        } else {
            verifyEndpoint();
        }
    }

    public void verifyEndpoint(String endpoint) {
        requireServerMessage("The endpoint \"" + endpoint + "\" is not available",
                             "CWWKT0016I:.*\\b" + endpoint + "\\b.*");
    }

    public void verifyEndpoint() {
        requireServerMessage("The endpoint is not available", "CWWKT0016I:.*");
    }

    public void verifyServer() {
        requireServerMessage("Server is not ready to run", "CWWKF0011I:.*");
    }

    protected static void clearExtraArgs() {
        extraServerArgs.clear();
    }

    @AfterClass
    public static void stopServer() throws Exception {
        try {
            stopServer(DO_CLEANUP_APPS);
        } finally {
            server.updateServerConfiguration(originalServerConfig);
        }
    }

    public static void stopServer(boolean cleanupApps, String... expectedErrors) throws Exception {
        clearExtraArgs();
        clearBootStrapProperties();
        clearEnvVariables();

        boolean isActive = serverStarted.getAndSet(false);

        Exception exception = null;

        if (isActive) {
            exception = cascade("Server stop failure", exception,
                                () -> {
                                    server.stopServer(false, expectedErrors);
                                });
        }
        if (cleanupApps) {
            exception = cascade("Cleanup failure", exception,
                                () -> {
                                    deleteDropinFiles();
                                    deleteSpringCache();
                                });
        }
        if (isActive) {
            exception = cascade("Archive failure", exception,
                                () -> {
                                    server.postStopServerArchive();
                                });
        }
        exception = cascade("Delete logs failure", exception,
                            () -> {
                                server.deleteDirectoryFromLibertyServerRoot("logs/");
                            });

        if (exception != null) {
            throw exception;
        }
    }

    public static void configureEnvVariable(LibertyServer server, Map<String, String> newEnv) throws Exception {
        Properties serverEnvProperties = new Properties();
        serverEnvProperties.putAll(newEnv);
        File serverEnvFile = new File(server.getFileFromLibertyServerRoot("server.env").getAbsolutePath());
        try (OutputStream out = new FileOutputStream(serverEnvFile)) {
            serverEnvProperties.store(out, "");
        }
    }

    private static void clearEnvVariables() throws Exception {
        configureEnvVariable(server, Map.of());
    }

    @FunctionalInterface
    static interface FailureAction {
        void act() throws Exception;
    }

    public static Exception cascade(String message, Exception priorException, FailureAction action) {
        try {
            action.act();
        } catch (Exception e) {
            if (priorException != null) {
                System.out.println(message);
                e.printStackTrace(System.out);
            } else {
                priorException = e;
            }
        }
        return priorException;
    }

    /**
     * Answer the base name of the application which is to be run by this test.
     *
     * Note: While usually the name of the application which will be run, the
     * multi-application tests generate several application names, one of which
     * will be started, and that application may not be the base application.
     *
     * @return The base name of the application which is to be run by this test.
     */
    public abstract String getApplication();

    public RemoteFile getApplicationFile() throws Exception {
        return getServerFile("apps/" + getApplication());
    }

    /**
     * Optional count of applications which are to be copied into the dropins
     * folder. This default implementation answers 0. Subclasses may override.
     * A positive non-zero value will cause that count of the base application
     * to be copied into the dropins folder.
     *
     * @return The count of applications which are to be copied into the dropins
     *         folder. This default implementation answers 0.
     */
    public int getDropinCopyNum() {
        return 0;
    }

    /**
     * Values for specifying the application configuration.
     * See {@link #getApplicationConfigType}.
     */
    static enum AppConfigType {
        /** Deploy the application by placing it in the "dropins/spring" folder. */
        DROPINS_SPRING,
        /** Drop the application in the root "dropins" folder. */
        DROPINS_ROOT,
        /** Use the application as-is in the "apps" folder. */
        SPRING_BOOT_APP_TAG,
        /** Drop the application in the root dropins folder, as a war app */
        DROPINS_ROOT_WAR,
        /** Use the application as a WAR in the "apps" folder. */
        WEB_APP_TAG
    }

    /**
     * Tell the particular strategy in use for configuring
     * the application of this test class. Used by
     * {@link #doConfigureServer}.
     *
     * @return The strategy used when configuring the
     *         application of this test class.
     */
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.DROPINS_SPRING;
    }

    /**
     * Control parameter: Tell if the application must start for a
     * successful test.
     *
     * @return True or false telling if the application is expected
     *         to start.
     */
    public boolean expectApplicationSuccess() {
        return true;
    }

    /**
     * Control parameter: Tell if the test application is a web
     * application.
     *
     * @return True or false, telling if the test application is a
     *         web application.
     */
    public boolean expectWebApplication() {
        return true;
    }

    public List<String> getExpectedWebApplicationEndpoints() {
        List<String> expectedEndpoints = new ArrayList<String>();

        String testMethodName = testName.getMethodName();
        if ((testMethodName != null) && testMethodName.contains(DEFAULT_HOST_WITH_APP_PORT)) {
            expectedEndpoints.add(ID_DEFAULT_HOST);
        }

        return expectedEndpoints;
    }

    public void modifyAppConfiguration(SpringBootApplication appConfig) {
        // do nothing by default
    }

    public void modifyAppConfiguration(WebApplication appConfig) {
        // do nothing by default
    }

    public void modifyServerConfiguration(ServerConfiguration config) {
        // do nothing by default
    }

    /**
     * Tell if the spring application will use the default host. When
     * false, application argument {@link #LIBERTY_USE_DEFAULT_HOST} is
     * omitted from application arguments.
     *
     * @return True or false telling if the spring application will use the
     *         default host.
     */
    public boolean useDefaultVirtualHost() {
        return false;
    }

    /**
     * Return the server configuration, with the configuration values
     * reset to empty values, except with the features set to the
     * intended provisioned features, per {@link #getFeatures()}.
     *
     * @return The cleared server configuration.
     */
    protected ServerConfiguration getServerConfiguration() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();

        server.removeAllInstalledAppsForValidation();

        List<SpringBootApplication> applications = config.getSpringBootApplications();
        applications.clear();

        List<WebApplication> webApplications = config.getWebApplications();
        webApplications.clear();

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

        return config;
    }

    //

    protected static void clearBootStrapProperties() {
        bootStrapProperties.clear();
    }

    protected void configureBootStrapProperties(boolean dropinsTest) throws Exception {
        configureBootStrapProperties(dropinsTest, DO_ADD_DEFAULT_PROPERTIES);
    }

    /**
     * Setup bootstrap properties. Gather properties and store then in the usual
     * server bootstrap properties file.
     *
     * Conditionally add default properties from {@link #getDefaultBootStrapProperties()}.
     * Always add properties from {@link #getBootStrapProperties}. Conditionally
     * set {@link #LIBERTY_USE_DEFAULT_HOST} false when applications are in the dropins
     * folder and when {@link #useDefaultVirtualHost()} is false.
     *
     * @param dropinsTest      Control parameter: Tells if applications are in the dropins folder.
     * @param addDefaultProps: Control parameters: Tell if default property values are to be added.
     *
     * @throws Exception Thrown if the bootstrap properties could not be written.
     */
    protected void configureBootStrapProperties(boolean dropinsTest, boolean addDefaultProps) throws Exception {
        if (addDefaultProps) {
            bootStrapProperties.putAll(getDefaultBootStrapProperties());
        }
        bootStrapProperties.putAll(getBootStrapProperties());

        if (dropinsTest && !useDefaultVirtualHost()) {
            bootStrapProperties.put(LIBERTY_USE_DEFAULT_HOST, Boolean.FALSE.toString());
        }

        File bootStrapPropertiesFile = new File(getServerFile("bootstrap.properties").getAbsolutePath());
        try (OutputStream out = new FileOutputStream(bootStrapPropertiesFile)) {
            bootStrapProperties.store(out, "");
        }
    }
}
