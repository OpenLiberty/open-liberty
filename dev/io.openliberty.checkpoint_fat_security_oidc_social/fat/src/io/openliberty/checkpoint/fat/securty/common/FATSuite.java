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
package io.openliberty.checkpoint.fat.securty.common;

import static componenttest.topology.utils.FATServletClient.getTestMethodSimpleName;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Variable;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.checkpoint.fat.security.oidc.OidcClientBasicTests;
import io.openliberty.checkpoint.fat.security.social.LibertyOP_BasicTests_oauth_usingSocialConfig;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                LibertyOP_BasicTests_oauth_usingSocialConfig.class,
                OidcClientBasicTests.class
})

public class FATSuite {

    public static void configureEnvVariable(LibertyServer server, Map<String, String> newEnv) throws Exception {
        Properties serverEnvProperties = new Properties();
        serverEnvProperties.putAll(newEnv);
        File serverEnvFile = new File(server.getFileFromLibertyServerRoot("server.env").getAbsolutePath());
        try (OutputStream out = new FileOutputStream(serverEnvFile)) {
            serverEnvProperties.store(out, "");
        }
    }

    public static String getTestMethodNameOnly(TestName testName) {
        String testMethodSimpleName = getTestMethodSimpleName(testName);
        // Sometimes the method name includes the class name; remove the class name.
        int dot = testMethodSimpleName.indexOf('.');
        if (dot != -1) {
            testMethodSimpleName = testMethodSimpleName.substring(dot + 1);
        }
        return testMethodSimpleName;
    }

    public static <T extends Enum<T>> T getTestMethod(Class<T> type, TestName testName) {
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

    public static void updateVariableConfig(LibertyServer server, String name, String value) throws Exception {
        // change config of variable for restore
        ServerConfiguration config = removeTestKeyVar(server.getServerConfiguration(), name);
        config.getVariables().add(new Variable(name, value));
        server.updateServerConfiguration(config);
    }

    public static ServerConfiguration removeTestKeyVar(ServerConfiguration config, String key) {
        for (Iterator<Variable> iVars = config.getVariables().iterator(); iVars.hasNext();) {
            Variable var = iVars.next();
            if (var.getName().equals(key)) {
                iVars.remove();
            }
        }
        return config;
    }

}
