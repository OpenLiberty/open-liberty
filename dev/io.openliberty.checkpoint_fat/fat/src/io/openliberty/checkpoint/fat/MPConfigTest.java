/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.fat;

import static io.openliberty.checkpoint.fat.FATSuite.getTestMethod;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Variable;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import mpapp1.MPConfigServlet;

@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class MPConfigTest extends FATServletClient {

    public static final String APP_NAME = "mpapp1";

    @Server("checkpointMPConfig")
    @TestServlet(servlet = MPConfigServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    public TestMethod testMethod;

    private static final Properties serverEnvProperties = new Properties();
    private static File serverEnvFile;

    @BeforeClass
    public static void copyAppToDropins() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, APP_NAME);
        FATSuite.copyAppsAppToDropins(server, APP_NAME);
    }

    @Before
    public void setUp() throws Exception {
        testMethod = getTestMethod(TestMethod.class, testName);
        configureBeforeCheckpoint();
        server.setCheckpoint(CheckpointPhase.APPLICATIONS, true,
                             server -> {
                                 assertNotNull("'SRVE0169I: Loading Web Module: " + APP_NAME + "' message not found in log before rerstore",
                                               server.waitForStringInLogUsingMark("SRVE0169I: .*" + APP_NAME, 0));
                                 assertNotNull("'CWWKZ0001I: Application " + APP_NAME + " started' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I: .*" + APP_NAME, 0));
                                 configureBeforeRestore();
                             });
        server.startServer(getTestMethod(TestMethod.class, testName) + ".log");
    }

    private void configureBeforeCheckpoint() throws Exception {
        switch (testMethod) {
            case envValueChangeTest:
                configureEnvVariable("req_scope_key", "envValue");
                break;
            case appScopeEnvValueChangeTest:
                configureEnvVariable("app_scope_key", "envValue");
                break;
            case configObjectAppScopeEnvValueChangeTest:
                configureEnvVariable("config_object_app_scope_key", "envValue");
                break;
            case configObjectPropertiesAppScopeEnvValueChangeTest:
                configureEnvVariable("config_object_properties_app_scope_key", "envValue");
                break;
            default:
                break;
        }
    }

    private void configureEnvVariable(String key, String value) throws Exception {
        serverEnvProperties.clear();
        if (key != null && value != null) {
            serverEnvProperties.put(key, value);
        }
        serverEnvFile = new File(server.getFileFromLibertyServerRoot("server.env").getAbsolutePath());
        try (OutputStream out = new FileOutputStream(serverEnvFile)) {
            serverEnvProperties.store(out, "");
        }
    }

    private void configureBeforeRestore() {
        try {
            server.saveServerConfiguration();
            Log.info(getClass(), testName.getMethodName(), "Configuring: " + testMethod);
            switch (testMethod) {
                // MPConfigBean bean
                case envValueTest:
                    configureEnvVariable("req_scope_key", "envValue");
                    break;
                case serverValueTest:
                    updateVariableConfig("req_scope_key", "serverValue");
                    break;
                case annoValueTest:
                    removeVariableConfig("req_scope_key");
                    break;
                case envValueChangeTest:
                    configureEnvVariable("req_scope_key", "envValueChange");
                    break;

                // MPConfigBeanWithApplicationScope bean
                case appScopeEnvValueTest:
                    configureEnvVariable("app_scope_key", "envValue");
                    break;
                case appScopeServerValueTest:
                    updateVariableConfig("app_scope_key", "serverValue");
                    break;
                case appScopeAnnoValueTest:
                    removeVariableConfig("app_scope_key");
                    break;
                case appScopeEnvValueChangeTest:
                    configureEnvVariable("app_scope_key", "envValueChange");
                    break;

                // ApplicationScopedOnCheckpointBeanWithConfigObject bean
                case configObjectAppScopeEnvValueTest:
                    configureEnvVariable("config_object_app_scope_key", "envValue");
                    break;
                case configObjectAppScopeServerValueTest:
                    updateVariableConfig("config_object_app_scope_key", "serverValue");
                    break;
                case configObjectAppScopeAnnoValueTest:
                    removeVariableConfig("config_object_app_scope_key");
                    break;
                case configObjectAppScopeEnvValueChangeTest:
                    configureEnvVariable("config_object_app_scope_key", "envValueChange");
                    break;

                // ApplicationScopedOnCheckpointBeanWithConfigObjectProperties bean
                case configObjectPropertiesAppScopeEnvValueTest:
                    configureEnvVariable("config_object_properties_app_scope_key", "envValue");
                    break;
                case configObjectPropertiesAppScopeServerValueTest:
                    updateVariableConfig("config_object_properties_app_scope_key", "serverValue");
                    break;
                case configObjectPropertiesAppScopeEnvValueChangeTest:
                    configureEnvVariable("config_object_properties_app_scope_key", "envValueChange");
                    break;

                // ApplicationScopedOnCheckpointBean bean
                case applicationScopedValueTest:
                    updateVariableConfig("early_access_app_scope_key", "serverValue");
                    break;

                // Default tests in all beans
                case defaultValueTest:
                case appScopeDefaultValueTest:
                case configObjectAppScopeDefaultValueTest:
                case configObjectPropertiesAppScopeDefaultValueTest:
                    // Just fall through and do the default (no configuration change)
                    // should use the defaultValue from server.xml
                default:
                    Log.info(getClass(), testName.getMethodName(), "No configuration required: " + testMethod);
                    break;
            }

        } catch (Exception e) {
            throw new AssertionError("Unexpected error configuring test.", e);
        }
    }

    private void removeVariableConfig(String name) throws Exception {
        // remove variable for restore, fall back to default value on annotation
        server.updateServerConfiguration(removeTestKeyVar(server.getServerConfiguration(), name));
    }

    private void updateVariableConfig(String name, String value) throws Exception {
        // change config of variable for restore
        ServerConfiguration config = removeTestKeyVar(server.getServerConfiguration(), name);
        config.getVariables().add(new Variable(name, value));
        server.updateServerConfiguration(config);
    }

    private ServerConfiguration removeTestKeyVar(ServerConfiguration config, String key) {
        for (Iterator<Variable> iVars = config.getVariables().iterator(); iVars.hasNext();) {
            Variable var = iVars.next();
            if (var.getName().equals(key)) {
                iVars.remove();
            }
        }
        return config;
    }

    private void checkForLogsAndStopServer() throws Exception {
        switch (testMethod) {
            // MPConfigBean bean
            case envValueTest:
            case serverValueTest:
            case annoValueTest:
            case envValueChangeTest:
                assertNull("CWWKC0651W message not expected in logs", server
                                .waitForStringInLog("CWWKC0651W:.*"));
                server.stopServer();
                break;

            // MPConfigBeanWithApplicationScope bean
            case appScopeEnvValueTest:
            case appScopeServerValueTest:
            case appScopeAnnoValueTest:
            case appScopeEnvValueChangeTest:
                assertNull("CWWKC0651W message not expected in logs", server
                                .waitForStringInLog("CWWKC0651W:.*"));
                server.stopServer();
                break;

            //ApplicationScopedOnCheckpointBeanWithConfigObject bean
            case configObjectAppScopeEnvValueTest:
            case configObjectAppScopeServerValueTest:
            case configObjectAppScopeAnnoValueTest:
            case configObjectAppScopeEnvValueChangeTest:
                assertNotNull("CWWKC0651W message expected in logs", server
                                .waitForStringInLog("CWWKC0651W:.*config_object_app_scope_key*"));
                server.stopServer("CWWKC0651W");
                break;

            // ApplicationScopedOnCheckpointBeanWithConfigObjectProperties bean
            case configObjectPropertiesAppScopeEnvValueTest:
            case configObjectPropertiesAppScopeServerValueTest:
            case configObjectPropertiesAppScopeEnvValueChangeTest:
                assertNotNull("CWWKC0651W message expected in logs", server
                                .waitForStringInLog("CWWKC0651W:.*config_object_properties_app_scope_key*"));
                server.stopServer("CWWKC0651W");
                break;

            // ApplicationScopedOnCheckpointBean bean
            case applicationScopedValueTest:
                assertNotNull("CWWKC0651W message expected in logs", server
                                .waitForStringInLog("CWWKC0651W:.*early_access_app_scope_key*"));
                server.stopServer("CWWKC0651W");
                break;

            // Default tests in all beans
            case defaultValueTest:
            case appScopeDefaultValueTest:
            case configObjectAppScopeDefaultValueTest:
            case configObjectPropertiesAppScopeDefaultValueTest:
                assertNull("CWWKC0651W message not expected in logs", server
                                .waitForStringInLog("CWWKC0651W:.*"));
                server.stopServer();
                break;
            default:
                server.stopServer();
                break;
        }
    }

    @After
    public void tearDown() throws Exception {
        checkForLogsAndStopServer();
        server.restoreServerConfiguration();
        configureEnvVariable(null, null);
    }

    static enum TestMethod {
        envValueTest,
        envValueChangeTest,
        serverValueTest,
        annoValueTest,
        defaultValueTest,
        appScopeEnvValueTest,
        appScopeEnvValueChangeTest,
        appScopeServerValueTest,
        appScopeAnnoValueTest,
        appScopeDefaultValueTest,
        applicationScopedValueTest,
        configObjectAppScopeEnvValueTest,
        configObjectAppScopeEnvValueChangeTest,
        configObjectAppScopeServerValueTest,
        configObjectAppScopeAnnoValueTest,
        configObjectAppScopeDefaultValueTest,
        configObjectPropertiesAppScopeEnvValueTest,
        configObjectPropertiesAppScopeEnvValueChangeTest,
        configObjectPropertiesAppScopeServerValueTest,
        configObjectPropertiesAppScopeDefaultValueTest,
        unknown
    }
}
