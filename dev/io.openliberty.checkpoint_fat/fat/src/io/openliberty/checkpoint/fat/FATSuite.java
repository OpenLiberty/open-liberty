/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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

import static componenttest.topology.utils.FATServletClient.getTestMethodSimpleName;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Variable;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                BasicServletTest.class,
                CheckpointFailTest.class,
                CheckpointLauncherArgsTest.class,
                CheckpointPhaseTest.class,
                LogsVerificationTest.class,
                OSGiConsoleTest.class,
                LocalEJBTest.class,
                CheckpointSPITest.class,
                CheckpointWithSecurityManager.class,
                SSLTest.class,
                PasswordUtilsTest.class,
                MPOpenTracingJaegerTraceTest.class,
                MPFaultToleranceTimeoutTest.class,
                ValidFeaturesTest.class,
                RESTclientTest.class,
                JNDITest.class,
                CRIULogLevelTest.class,
                AppsecurityTest.class,
                WebSocketTest.class,
                FacesTest.class,
                FacesBeanTest.class,
                WebProfileJSPWithELTest.class,
                OpenAPITest.class,
                MPJWTTest.class,
                MPMetricsTest.class,
                MPTelemetryTest.class,
                WebProfileEARtest.class,
                MPHealthTest.class,
                SlowAppStartTest.class,
                JsonbTest.class,
                JsonpTest.class,
                ManagedBeansTest.class,
                BellsTest.class,
                JaxWSVirtualHostTest.class,
                WebAppMessageTest.class,
                URAPIs_Federation_2LDAPsTest.class,
                JavaInfoIsCriuSupportedTest.class,
                RestConnectorTest.class,
                AuditTest.class,
                ConcurrencyTest.class,
                MapCacheTest.class,
                WebCacheTest.class,
                XMLbindingsTest.class,
                LocalConnectorTest.class,
                WebProfileJSPtest.class,
                MPTelemetryJaxRsIntegrationTest.class,
                LogsChangeTraceSpecTest.class,
                OpenAPIConfigTest.class
})

public class FATSuite {
    public static void copyAppsAppToDropins(LibertyServer server, String appName) throws Exception {
        RemoteFile appFile = server.getFileFromLibertyServerRoot("apps/" + appName + ".war");
        LibertyFileManager.createRemoteFile(server.getMachine(), server.getServerRoot() + "/dropins").mkdir();
        appFile.copyToDest(server.getFileFromLibertyServerRoot("dropins"));
    }

    /**
     * Gets only the test method of the TestName without the class name
     * and without the repeating rule name.
     *
     * @param testName
     * @return the test method only
     */
    static String getTestMethodNameOnly(TestName testName) {
        String testMethodSimpleName = getTestMethodSimpleName(testName);
        // Sometimes the method name includes the class name; remove the class name.
        int dot = testMethodSimpleName.indexOf('.');
        if (dot != -1) {
            testMethodSimpleName = testMethodSimpleName.substring(dot + 1);
        }
        return testMethodSimpleName;
    }

    static public <T extends Enum<T>> T getTestMethod(Class<T> type, TestName testName) {
        String simpleName = getTestMethodNameOnly(testName);
        try {
            T t = Enum.valueOf(type, simpleName);
            Log.info(FATSuite.class, testName.getMethodName(), "got test method: " + t);
            return t;
        } catch (IllegalArgumentException e) {
            Log.info(type, simpleName, "No configuration enum: " + testName);
            fail("Unknown test name: " + testName.getMethodName());
            return null;
        }
    }

    static public void configureBootStrapProperties(LibertyServer server, Map<String, String> properties) throws Exception, IOException, FileNotFoundException {
        Properties bootStrapProperties = new Properties();
        File bootStrapPropertiesFile = new File(server.getFileFromLibertyServerRoot("bootstrap.properties").getAbsolutePath());
        if (bootStrapPropertiesFile.isFile()) {
            try (InputStream in = new FileInputStream(bootStrapPropertiesFile)) {
                bootStrapProperties.load(in);
            }
        }
        bootStrapProperties.putAll(properties);
        try (OutputStream out = new FileOutputStream(bootStrapPropertiesFile)) {
            bootStrapProperties.store(out, "");
        }
    }

    static void configureEnvVariable(LibertyServer server, Map<String, String> newEnv) throws Exception {
        Properties serverEnvProperties = new Properties();
        serverEnvProperties.putAll(newEnv);
        File serverEnvFile = new File(server.getFileFromLibertyServerRoot("server.env").getAbsolutePath());
        try (OutputStream out = new FileOutputStream(serverEnvFile)) {
            serverEnvProperties.store(out, "");
        }
    }

    static void updateVariableConfig(LibertyServer server, String name, String value) throws Exception {
        // change config of variable for restore
        ServerConfiguration config = removeTestKeyVar(server.getServerConfiguration(), name);
        config.getVariables().add(new Variable(name, value));
        server.updateServerConfiguration(config);
    }

    static ServerConfiguration removeTestKeyVar(ServerConfiguration config, String key) {
        for (Iterator<Variable> iVars = config.getVariables().iterator(); iVars.hasNext();) {
            Variable var = iVars.next();
            if (var.getName().equals(key)) {
                iVars.remove();
            }
        }
        return config;
    }

    public static void transformApps(LibertyServer myServer, String... apps) {
        if (JakartaEEAction.isEE9OrLaterActive()) {
            for (String app : apps) {
                Path someArchive = Paths.get(myServer.getServerRoot() + File.separatorChar + "dropins" + File.separatorChar + app);
                JakartaEEAction.transformApp(someArchive);
            }
        }
    }

    public static RepeatTests defaultMPRepeat(String serverName) {
        return MicroProfileActions.repeat(serverName,
                                          MicroProfileActions.MP61, // first test in LITE mode
                                          MicroProfileActions.MP41, // rest are FULL mode
                                          MicroProfileActions.MP50);
    }
}
