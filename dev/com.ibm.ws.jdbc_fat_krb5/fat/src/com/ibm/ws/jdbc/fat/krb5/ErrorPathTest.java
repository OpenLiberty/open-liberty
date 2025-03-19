/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
package com.ibm.ws.jdbc.fat.krb5;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.AuthData;
import com.ibm.websphere.simplicity.config.Kerberos;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jdbc.fat.krb5.containers.DB2KerberosContainer;
import com.ibm.ws.jdbc.fat.krb5.containers.KerberosPlatformRule;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ErrorPathTest extends FATServletClient {

    private static final Class<?> c = ErrorPathTest.class;

    @Server("com.ibm.ws.jdbc.fat.krb5")
    public static LibertyServer server;

    private static final DB2KerberosContainer db2 = DB2KerberosTest.db2;

    private static final String APP_NAME = DB2KerberosTest.APP_NAME;

    @ClassRule
    public static KerberosPlatformRule skipRule = new KerberosPlatformRule();

    private static ServerConfiguration originalConfig;

    @BeforeClass
    public static void setUp() throws Exception {
        Path krbConfPath = Paths.get(server.getServerRoot(), "security", "krb5.conf");
        FATSuite.krb5.generateConf(krbConfPath);

        //TODO switch
        Path krbKeytabPath = Paths.get("publish", "servers", "com.ibm.ws.jdbc.fat.krb5", "security", "krb5.keytab");
//        krbKeytabPath = Paths.get(server.getServerRoot(), "security", "krb5.keytab");

        db2.start();

        ShrinkHelper.defaultDropinApp(server, APP_NAME, "jdbc.krb5.db2.web");

        server.addEnvVar("DB2_DBNAME", db2.getDatabaseName());
        server.addEnvVar("DB2_HOSTNAME", db2.getHost());
        server.addEnvVar("DB2_PORT", "" + db2.getMappedPort(50000));
        server.addEnvVar("DB2_USER", db2.getUsername());
        server.addEnvVar("DB2_PASS", db2.getPassword());
        server.addEnvVar("KRB5_USER", DB2KerberosTest.KRB5_USER);
        server.addEnvVar("KRB5_CONF", krbConfPath.toAbsolutePath().toString());
        server.addEnvVar("KRB5_KEYTAB", krbKeytabPath.toAbsolutePath().toString());
        List<String> jvmOpts = new ArrayList<>();
        jvmOpts.add("-Dsun.security.krb5.debug=true"); // Hotspot/OpenJ9
        jvmOpts.add("-Dcom.ibm.security.krb5.krb5Debug=true"); // IBM JDK

        server.setJvmOptions(jvmOpts);

        server.startServer();

        originalConfig = server.getServerConfiguration().clone();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Exception firstError = null;

        try {
            server.stopServer("CWWKS4345E: .*bogus.conf"); // expected by testConfigFileInvalid
        } catch (Exception e) {
            firstError = e;
            Log.error(c, "tearDown", e);
        }
        try {
            db2.stop();
        } catch (Exception e) {
            if (firstError == null)
                firstError = e;
            Log.error(c, "tearDown", e);
        }

        if (firstError != null)
            throw firstError;
    }

    /**
     * Test what happens when the krb5TicketCache is set to an invalid path
     * Expect that getting a connection fails in the test app with a LoginException
     */
    @Test
    @AllowedFFDC
    public void testTicketCacheInvalid() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        try {
            Log.info(c, testName.getMethodName(), "Changing config to use 'krb5TicketCache' instead of 'keytab'");
            AuthData krb5Auth = config.getAuthDataElements().getById("krb5Auth");
            krb5Auth.krb5TicketCache = "${server.config.dir}/bogus_cCache";
            Kerberos kerberos = config.getKerberos();
            kerberos.keytab = null;
            updateConfigAndWait(config);

            FATServletClient.runTest(server, APP_NAME + "/DB2KerberosTestServlet", testName);
        } finally {
            Log.info(c, testName.getMethodName(), "Restoring original config");
            updateConfigAndWait(originalConfig);
        }
    }

    /**
     * Test what happens when <kerberos configFile="..."/> is set to an invalid path
     * Expect a server error message CWWKS4345E to be output indicating that the config file does not exist
     */
    @Test
    public void testConfigFileInvalid() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        try {
            Log.info(c, testName.getMethodName(), "Changing config to use bogus value for 'configFile'");
            Kerberos kerberos = config.getKerberos();
            kerberos.configFile = "${server.config.dir}/bogus.conf";
            updateConfigAndWait(config);
            server.waitForStringInLogUsingMark("CWWKS4345E");
        } finally {
            Log.info(c, testName.getMethodName(), "Restoring original config");
            updateConfigAndWait(originalConfig);
        }
    }

    /**
     * Test what happens when krb5TicketCache not configured, keytab file can’t be found, invalid password configured
     * Expect that getConnection() in the test app fails with a LoginException
     */
    @Test
    @AllowedFFDC
    public void testPasswordInvalid() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        try {
            Log.info(c, testName.getMethodName(), "Changing config to use bogus authData password");
            Kerberos kerberos = config.getKerberos();
            kerberos.keytab = null;

            AuthData krb5Auth = config.getAuthDataElements().getById("krb5Auth");
            krb5Auth.krb5TicketCache = null;
            krb5Auth.setPassword("bogusPassword");

            updateConfigAndWait(config);

            FATServletClient.runTest(server, APP_NAME + "/DB2KerberosTestServlet", testName);
        } finally {
            Log.info(c, testName.getMethodName(), "Restoring original config");
            updateConfigAndWait(originalConfig);
        }
    }

    private void updateConfigAndWait(ServerConfiguration config) throws Exception {
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
    }

}
