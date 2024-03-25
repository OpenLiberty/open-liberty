/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat;

import static io.openliberty.checkpoint.fat.FATSuite.configureEnvVariable;
import static io.openliberty.checkpoint.fat.FATSuite.getTestMethod;
import static io.openliberty.checkpoint.fat.FATSuite.removeTestKeyVar;
import static io.openliberty.checkpoint.fat.FATSuite.updateVariableConfig;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import mpapp1.MPConfigServlet;

@RunWith(FATRunner.class)
@CheckpointTest
public class MPConfigTest extends FATServletClient {

    private static final String SERVER_NAME = "checkpointMPConfig";

    public static final String APP_NAME = "mpapp1";

    @Server(SERVER_NAME)
    @TestServlet(servlet = MPConfigServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    public TestMethod testMethod;

    @ClassRule
    public static RepeatTests repeatTest = FATSuite.defaultMPRepeat(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, APP_NAME);
        FATSuite.copyAppsAppToDropins(server, APP_NAME);
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false,
                             server -> {
                                 assertNotNull("'SRVE0169I: Loading Web Module: " + APP_NAME + "' message not found in log before rerstore",
                                               server.waitForStringInLogUsingMark("SRVE0169I: .*" + APP_NAME, 0));
                                 assertNotNull("'CWWKZ0001I: Application " + APP_NAME + " started' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I: .*" + APP_NAME, 0));
                             });
        server.startServer();
    }

    @Before
    public void beforeTest() throws Exception {
        testMethod = getTestMethod(TestMethod.class, testName);

        configureBeforeRestore();
        server.setArchiveMarker(testMethod + ".marker");
        server.checkpointRestore();
    }

    private void configureBeforeRestore() {
        try {
            new File(server.getServerRoot(), "variables").mkdirs();
            server.saveServerConfiguration();
            Log.info(getClass(), testName.getMethodName(), "Configuring: " + testMethod);
            switch (testMethod) {
                // MPConfigBean bean
                case envValueTest:
                    configureEnvVariable(server, singletonMap("req_scope_key", "envValue"));
                    break;
                case serverValueTest:
                    updateVariableConfig(server, "req_scope_key", "serverValue");
                    break;
                case annoValueTest:
                    removeVariableConfig("req_scope_key");
                    break;
                case varDirValueTest:
                    server.copyFileToLibertyServerRoot("variables", "configVariables/req_scope_key");
                    break;
                case providerEnvValueTest:
                    configureEnvVariable(server, singletonMap("provider_req_scope_key", "providerEnvValue"));
                    break;
                case noDefaultEnvValueTest:
                    configureEnvVariable(server, singletonMap("optional_req_scope_key", "optionalEnvValue"));
                    break;
                case noDefaultServerValueTest:
                    updateVariableConfig(server, "optional_req_scope_key", "optionalServerValue");
                    break;

                // MPConfigBeanWithApplicationScope bean
                case appScopeEnvValueTest:
                    configureEnvVariable(server, singletonMap("app_scope_key", "envValue"));
                    break;
                case appScopeServerValueTest:
                    updateVariableConfig(server, "app_scope_key", "serverValue");
                    break;
                case appScopeAnnoValueTest:
                    removeVariableConfig("app_scope_key");
                    break;
                case appScopeVarDirValueTest:
                    server.copyFileToLibertyServerRoot("variables", "configVariables/app_scope_key");
                    break;
                case appScopeProviderEnvValueTest:
                    configureEnvVariable(server, singletonMap("provider_app_scope_key", "providerEnvValue"));
                    break;
                case appScopeNoDefaultEnvValueTest:
                    configureEnvVariable(server, singletonMap("optional_app_scope_key", "optionalEnvValue"));
                    break;
                case appScopeNoDefaultServerValueTest:
                    updateVariableConfig(server, "optional_app_scope_key", "optionalServerValue");
                    break;

                // ApplicationScopedOnCheckpointBeanWithConfigObject bean
                case configObjectAppScopeEnvValueTest:
                    configureEnvVariable(server, singletonMap("config_object_app_scope_key", "envValue"));
                    break;
                case configObjectAppScopeServerValueTest:
                    updateVariableConfig(server, "config_object_app_scope_key", "serverValue");
                    break;
                case configObjectAppScopeAnnoValueTest:
                    removeVariableConfig("config_object_app_scope_key");
                    break;

                // ApplicationScopedOnCheckpointBeanWithConfigObjectProperties bean
                case configObjectPropertiesAppScopeEnvValueTest:
                    configureEnvVariable(server, singletonMap("config_object_properties_app_scope_key", "envValue"));
                    break;
                case configObjectPropertiesAppScopeServerValueTest:
                    updateVariableConfig(server, "config_object_properties_app_scope_key", "serverValue");
                    break;

                // ApplicationScopedOnCheckpointBean bean
                case appScopeEarlyAccessValueTest:
                    updateVariableConfig(server, "early_access_app_scope_key", "serverValue");
                    break;
                case appScopeEarlyAccessNoDefaultEnvValueTest:
                    configureEnvVariable(server, singletonMap("early_access_optional_app_scope_key", "optionalEnvValue"));
                    break;
                case appScopeEarlyAccessNoDefaultServerValueTest:
                    updateVariableConfig(server, "early_access_optional_app_scope_key", "optionalServerValue");
                    break;
                case appScopeEarlyAccessNoDefaultProviderEnvValueTest:
                    configureEnvVariable(server, singletonMap("early_access_provider_optional_app_scope_key", "providerEnvValue"));
                    break;
                case appScopeEarlyAccessNoDefaultProviderServerValueTest:
                    updateVariableConfig(server, "early_access_provider_optional_app_scope_key", "providerServerValue");
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

    private void checkForLogsAndStopServer() throws Exception {
        Log.info(getClass(), testName.getMethodName(), "checkForLogsAndStopServer: " + testMethod);
        String expectedWarning = null;
        try {
            switch (testMethod) {
                // MPConfigBean bean
                case envValueTest:
                case serverValueTest:
                case annoValueTest:
                case varDirValueTest:
                case providerEnvValueTest:
                case noDefaultEnvValueTest:
                case noDefaultServerValueTest:
                    assertNull("CWWKC0651W message not expected in logs", server.waitForStringInLog("CWWKC0651W:.*", 100));
                    break;

                // MPConfigBeanWithApplicationScope bean
                case appScopeEnvValueTest:
                case appScopeServerValueTest:
                case appScopeAnnoValueTest:
                case appScopeVarDirValueTest:
                case appScopeProviderEnvValueTest:
                case appScopeNoDefaultEnvValueTest:
                case appScopeNoDefaultServerValueTest:
                    assertNull("CWWKC0651W message not expected in logs", server.waitForStringInLog("CWWKC0651W:.*", 100));
                    break;

                //ApplicationScopedOnCheckpointBeanWithConfigObject bean
                case configObjectAppScopeEnvValueTest:
                case configObjectAppScopeServerValueTest:
                case configObjectAppScopeAnnoValueTest:
                    assertNotNull("CWWKC0651W message expected in logs", server.waitForStringInLog("CWWKC0651W:.*config_object_app_scope_key*", 100));
                    expectedWarning = "CWWKC0651W";
                    break;

                // ApplicationScopedOnCheckpointBeanWithConfigObjectProperties bean
                case configObjectPropertiesAppScopeEnvValueTest:
                case configObjectPropertiesAppScopeServerValueTest:
                    assertNotNull("CWWKC0651W message expected in logs", server.waitForStringInLog("CWWKC0651W:.*config_object_properties_app_scope_key*", 100));
                    expectedWarning = "CWWKC0651W";
                    break;

                // ApplicationScopedOnCheckpointBean bean
                case appScopeEarlyAccessValueTest:
                    assertNotNull("CWWKC0651W message expected in logs", server.waitForStringInLog("CWWKC0651W:.*early_access_app_scope_key*", 100));
                    expectedWarning = "CWWKC0651W";
                    break;
                case appScopeEarlyAccessNoDefaultEnvValueTest:
                case appScopeEarlyAccessNoDefaultServerValueTest:
                    assertNotNull("CWWKC0651W message expected in logs", server.waitForStringInLog("CWWKC0651W:.*early_access_optional_app_scope_key*", 100));
                    expectedWarning = "CWWKC0651W";
                    break;

                case appScopeEarlyAccessNoDefaultProviderEnvValueTest:
                case appScopeEarlyAccessNoDefaultProviderServerValueTest:
                    assertNotNull("CWWKC0651W message expected in logs", server.waitForStringInLog("CWWKC0651W:.*early_access_provider_optional_app_scope_key*", 100));
                    expectedWarning = "CWWKC0651W";
                    break;

                // Default tests in all beans
                case defaultValueTest:
                case appScopeDefaultValueTest:
                case configObjectAppScopeDefaultValueTest:
                case configObjectPropertiesAppScopeDefaultValueTest:
                    assertNull("CWWKC0651W message not expected in logs", server.waitForStringInLog("CWWKC0651W:.*", 100));
                    break;
                default:
                    break;
            }
        } finally {
            if (expectedWarning != null) {
                server.stopServer(expectedWarning);
            } else {
                server.stopServer();
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        try {
            checkForLogsAndStopServer();
        } finally {
            server.restoreServerConfiguration();
            configureEnvVariable(server, emptyMap());
            server.deleteFileFromLibertyServerRoot("variables/app_scope_key");
            server.deleteFileFromLibertyServerRoot("variables/req_scope_key");
        }
    }

    static enum TestMethod {
        envValueTest,
        serverValueTest,
        annoValueTest,
        defaultValueTest,
        varDirValueTest,
        providerEnvValueTest,
        noDefaultEnvValueTest,
        noDefaultServerValueTest,

        appScopeEnvValueTest,
        appScopeServerValueTest,
        appScopeAnnoValueTest,
        appScopeDefaultValueTest,
        appScopeVarDirValueTest,
        appScopeProviderEnvValueTest,
        appScopeNoDefaultEnvValueTest,
        appScopeNoDefaultServerValueTest,

        appScopeEarlyAccessValueTest,
        appScopeEarlyAccessNoDefaultEnvValueTest,
        appScopeEarlyAccessNoDefaultServerValueTest,
        appScopeEarlyAccessNoDefaultProviderEnvValueTest,
        appScopeEarlyAccessNoDefaultProviderServerValueTest,

        configObjectAppScopeEnvValueTest,
        configObjectAppScopeServerValueTest,
        configObjectAppScopeAnnoValueTest,
        configObjectAppScopeDefaultValueTest,

        configObjectPropertiesAppScopeEnvValueTest,
        configObjectPropertiesAppScopeServerValueTest,
        configObjectPropertiesAppScopeDefaultValueTest,
        unknown
    }
}
