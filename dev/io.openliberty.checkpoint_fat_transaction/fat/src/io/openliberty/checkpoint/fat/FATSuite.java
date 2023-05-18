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
import java.util.Map;
import java.util.Properties;

import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

//import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

@RunWith(Suite.class)
@SuiteClasses({
//                AlwaysPassesTest.class,
                ServletTest.class
//                ServletStartupTest.class,
//                StartupBeanTest.class,
//                TransactionalBeanTest.class,
//                TransactionScopedBeanTest.class,
//                TransactionLogTest.class,
//                TransactionManagerTest.class
//                RecoveryTest.class
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
            fail("Unknown test name: " + testName);
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

    static void stopServer(LibertyServer server, String... ignoredFailuresRegExps) {
        if (server.isStarted()) {
            try {
                server.stopServer(ignoredFailuresRegExps);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void deleteTranlogDir(LibertyServer server) throws Exception {
        deleteTranlogDir(server, "/tranlog");
    }

    /**
     * Delete the transaction log directory
     *
     * By default the transaction manager service logs transactions to files in directory
     * <code>${server.output.dir}/tranlog</code>. The directory may be otherwise specified
     * using <code><transaction/></code> config property <code>transactionLogDirectory</code>.
     */
    static void deleteTranlogDir(LibertyServer server, String dir) throws Exception {
        if (server.fileExistsInLibertyServerRoot(dir)) {
            server.deleteDirectoryFromLibertyServerRoot(dir);
        }
    }

    static void deleteTranlogDb(LibertyServer server) throws Exception {
        deleteTranlogDb(server, "/usr/shared/resources/data");
    }

    /**
     * Delete the database that stores transaction logs
     *
     * Requires <code><transacton/></code> config property <code>dataSourceRef</code> is
     * set to a non-transactional datasource where the transaction logs will be stored.
     */
    static void deleteTranlogDb(LibertyServer server, String dir) throws Exception {
        if (server.fileExistsInLibertyInstallRoot(dir)) {
            server.deleteDirectoryFromLibertyInstallRoot(dir);
        }
    }
}
