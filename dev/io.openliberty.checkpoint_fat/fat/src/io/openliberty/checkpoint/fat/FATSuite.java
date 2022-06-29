/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.fat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;

import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                BasicServletTest.class,
                CheckpointPhaseTest.class,
                LogsVerificationTest.class,
                OSGiConsoleTest.class,
                LocalEJBTest.class,
                CheckpointSPITest.class,
                MPConfigTest.class,
                SSLTest.class,
                MPOpenTracingJaegerTraceTest.class,
                MPFaultToleranceTimeoutTest.class,
                ValidFeaturesTest.class
})
public class FATSuite {
    public static void copyAppsAppToDropins(LibertyServer server, String appName) throws Exception {
        RemoteFile appFile = server.getFileFromLibertyServerRoot("apps/" + appName + ".war");
        LibertyFileManager.createRemoteFile(server.getMachine(), server.getServerRoot() + "/dropins").mkdir();
        appFile.copyToDest(server.getFileFromLibertyServerRoot("dropins"));
    }

    static String getTestMethodName(TestName testName) {
        String testMethodSimpleName = testName.getMethodName();
        int dot = testMethodSimpleName.indexOf('.');
        if (dot != -1) {
            testMethodSimpleName = testMethodSimpleName.substring(dot + 1);
        }
        return testMethodSimpleName;
    }

    static public <T extends Enum<T>> T getTestMethod(Class<T> type, TestName testName) {
        String testMethodSimpleName = getTestMethodName(testName);
        try {
            return Enum.valueOf(type, testMethodSimpleName);
        } catch (IllegalArgumentException e) {
            Log.info(type, testName.getMethodName(), "No configuration enum: " + testName.getMethodName());
            return Enum.valueOf(type, "unknown");
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
}
