/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.ejbcontainer.mdb.checkpoint.fat;

import static componenttest.topology.utils.FATServletClient.getTestMethodSimpleName;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.ejbcontainer.mdb.ra.checkpoint.fat.tests.ConfigChangesTest;
import io.openliberty.ejbcontainer.mdb.ra.checkpoint.fat.tests.MsgEndpointTest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                MsgEndpointTest.class,
                ConfigChangesTest.class
})
public class FATSuite {

    // Support mock checkpoint and restore
    static final List<String> CHECKPOINT_INACTIVE = Collections.emptyList();
    static final List<String> CHECKPOINT_BEFORE_APP_START = Arrays.asList("--internal-checkpoint-at=beforeAppStart");
    static final List<String> CHECKPOINT_AFTER_APP_START = Arrays.asList("--internal-checkpoint-at=afterAppStart");

    public static void setCheckpointPhase(LibertyServer server, CheckpointPhase phase) throws Exception {
        Map<String, String> jvmOptions = server.getJvmOptionsAsMap();
        switch (phase) {
            case BEFORE_APP_START:
                jvmOptions.put("-Dio.openliberty.checkpoint.stub.criu", "true");
                server.setExtraArgs(CHECKPOINT_BEFORE_APP_START);
                break;
            case AFTER_APP_START:
                jvmOptions.put("-Dio.openliberty.checkpoint.stub.criu", "true");
                server.setExtraArgs(CHECKPOINT_AFTER_APP_START);
                break;
            default:
                jvmOptions.remove("-Dio.openliberty.checkpoint.stub.criu");
                server.setExtraArgs(CHECKPOINT_INACTIVE);
        }
        server.setJvmOptions(jvmOptions);
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
            fail("Unknown test name: " + testName);
            return null;
        }
    }

    static public void configureBootstrapProperties(LibertyServer server, Map<String, String> properties) throws Exception, IOException, FileNotFoundException {
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

    static public void configureEnvVariables(LibertyServer server, Map<String, String> newEnv) throws Exception {
        Properties serverEnvProperties = new Properties();
        serverEnvProperties.putAll(newEnv);
        File serverEnvFile = new File(server.getFileFromLibertyServerRoot("/server.env").getAbsolutePath());
        try (OutputStream out = new FileOutputStream(serverEnvFile)) {
            serverEnvProperties.store(out, "");
        }
    }
}
