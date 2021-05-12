/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.krb5;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

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
import com.ibm.ws.jdbc.fat.krb5.containers.KerberosContainer;
import com.ibm.ws.jdbc.fat.krb5.containers.KerberosPlatformRule;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.krb5.db2.web.DB2KerberosTestServlet;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class DB2KerberosTest extends FATServletClient {

    private static final Class<?> c = DB2KerberosTest.class;

    public static final String KRB5_USER = "dbuser";

    public static final String APP_NAME = "krb5-db2-app";

    public static final DB2KerberosContainer db2 = new DB2KerberosContainer(FATSuite.network);

    @Server("com.ibm.ws.jdbc.fat.krb5")
    @TestServlet(servlet = DB2KerberosTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static KerberosPlatformRule skipRule = new KerberosPlatformRule();

    @BeforeClass
    public static void setUp() throws Exception {
        Path krbConfPath = Paths.get(server.getServerRoot(), "security", "krb5.conf");
        FATSuite.krb5.generateConf(krbConfPath);

        db2.start();

        ShrinkHelper.defaultDropinApp(server, APP_NAME, "jdbc.krb5.db2.web");

        server.addEnvVar("DB2_DBNAME", db2.getDatabaseName());
        server.addEnvVar("DB2_HOSTNAME", db2.getContainerIpAddress());
        server.addEnvVar("DB2_PORT", "" + db2.getMappedPort(50000));
        server.addEnvVar("DB2_USER", db2.getUsername());
        server.addEnvVar("DB2_PASS", db2.getPassword());
        server.addEnvVar("KRB5_USER", KRB5_USER);
        server.addEnvVar("KRB5_CONF", krbConfPath.toAbsolutePath().toString());
        List<String> jvmOpts = new ArrayList<>();
        jvmOpts.add("-Dsun.security.krb5.debug=true"); // Hotspot/OpenJ9
        jvmOpts.add("-Dcom.ibm.security.krb5.krb5Debug=true"); // IBM JDK

        if (JavaInfo.JAVA_VERSION >= 9) {
            jvmOpts.add("--illegal-access=permit"); // Java 16 JEPS 396
        }
        server.setJvmOptions(jvmOpts);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Exception firstError = null;

        try {
            server.stopServer("CWWKS4345E: .*BOGUS_KEYTAB", // expected by testBasicPassword
                              "DSRA0304E", "DSRA0302E", "WTRN0048W"); // expected by testXARecovery
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
     * Generate a ticket cache using the 'kinit' operating system command.
     * Dynamically configure the server to use REMOVE the keyTab configuration (since a bad ticket cache would fallback
     * to the keyTab) and SET the krb5TicketCache on the authData to be the file we just generated with 'kinit'.
     * Wait for a config update, and expect the getConnection test to work since the credential should be found in the ccache
     */
    @Test
    @Mode(TestMode.FULL)
    public void testTicketCache() throws Exception {
        String ccPath = Paths.get(server.getServerRoot(), "security", "krb5TicketCache_" + KRB5_USER).toAbsolutePath().toString();
        try {
            generateTicketCache(ccPath, false);
        } catch (UnsupportedOperationException e) {
            Log.info(c, testName.getMethodName(), "Skipping test because OS does not support 'kinit'");
            return;
        }

        ServerConfiguration config = server.getServerConfiguration();
        final String originalKeytab = config.getKerberos().keytab;
        try {
            Log.info(c, testName.getMethodName(), "Changing config to use 'krb5TicketCache' instead of 'keytab'");
            AuthData krb5Auth = config.getAuthDataElements().getById("krb5Auth");
            krb5Auth.krb5TicketCache = ccPath;
            Kerberos kerberos = config.getKerberos();
            kerberos.keytab = null;
            updateConfigAndWait(config);

            FATServletClient.runTest(server, APP_NAME + "/DB2KerberosTestServlet", testName);
        } finally {
            Log.info(c, testName.getMethodName(), "Restoring original config");
            config.getKerberos().keytab = originalKeytab;
            config.getAuthDataElements().getById("krb5Auth").krb5TicketCache = null;
            updateConfigAndWait(config);
        }
    }

    /**
     * Mimics testTicketCache, but using an expired cache to ensure a LoginException is thrown.
     */
    @Test
    @Mode(TestMode.FULL)
    @AllowedFFDC({ "javax.resource.ResourceException", "javax.security.auth.login.LoginException" })
    public void testTicketCacheExpired() throws Exception {
        String ccPath = Paths.get(server.getServerRoot(), "security", "krb5TicketCacheExpired_" + KRB5_USER).toAbsolutePath().toString();
        try {
            generateTicketCache(ccPath, true);
        } catch (UnsupportedOperationException e) {
            Log.info(c, testName.getMethodName(), "Skipping test because OS does not support 'kinit'");
            return;
        }

        ServerConfiguration config = server.getServerConfiguration();
        final String originalKeytab = config.getKerberos().keytab;
        try {
            Log.info(c, testName.getMethodName(), "Changing config to use 'krb5TicketCache' instead of 'keytab'");
            AuthData krb5Auth = config.getAuthDataElements().getById("krb5Auth");
            krb5Auth.krb5TicketCache = ccPath;
            Kerberos kerberos = config.getKerberos();
            kerberos.keytab = null;
            updateConfigAndWait(config);

            FATServletClient.runTest(server, APP_NAME + "/DB2KerberosTestServlet", testName);
        } finally {
            Log.info(c, testName.getMethodName(), "Restoring original config");
            config.getKerberos().keytab = originalKeytab;
            config.getAuthDataElements().getById("krb5Auth").krb5TicketCache = null;
            updateConfigAndWait(config);
        }
    }

    /**
     * Test that the 'password' attribute of an authData element can be used to supply a Kerberos password.
     * Normally a keytab file takes precedence over this, so perform dynamic config for the test to temporarily
     * set the keytab location to an invalid location to confirm that the supplied password actually gets used.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testBasicPassword() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        String originalKeytab = config.getKerberos().keytab;
        try {
            Log.info(c, testName.getMethodName(), "Changing the keystore to an invalid value so that password from the <authData> gets used");
            config.getKerberos().keytab = "BOGUS_KEYTAB";
            updateConfigAndWait(config);

            FATServletClient.runTest(server, APP_NAME + "/DB2KerberosTestServlet", testName);
        } finally {
            Log.info(c, testName.getMethodName(), "Restoring original config");
            config.getKerberos().keytab = originalKeytab;
            updateConfigAndWait(config);
        }
    }

    /**
     * Generates a ccache using kinit on the local system.
     *
     * @param ccPath  - Location to create the ccache
     * @param expired - creates the ccache with expired credentials
     * @throws Exception
     */
    private static void generateTicketCache(String ccPath, boolean expired) throws Exception {
        final String m = "generateTicketCache";
        String keytabPath = Paths.get("publish", "servers", "com.ibm.ws.jdbc.fat.krb5", "security", "krb5.keytab").toAbsolutePath().toString();

        ProcessBuilder pb = new ProcessBuilder("kinit", "-k", "-t", keytabPath, //
                        "-c", "FILE:" + ccPath, //Some linux kinit installs require FILE:
                        "-l", expired ? "1" : "604800", //Ticket lifetime, if expired set the minimum of 1s, otherwise 7 days.
                        KRB5_USER + "@" + KerberosContainer.KRB5_REALM);
        pb.environment().put("KRB5_CONFIG", Paths.get(server.getServerRoot(), "security", "krb5.conf").toAbsolutePath().toString());
        pb.redirectErrorStream(true);
        Process p = null;
        try {
            p = pb.start();
        } catch (IOException e) {
            Log.info(c, m, "Unable to start kinit due to: " + e.getMessage());
            throw new UnsupportedOperationException(e);
        }

        boolean success = p.waitFor(2, TimeUnit.MINUTES);
        String kinitResult = readInputStream(p.getInputStream());
        Log.info(c, m, "Output from creating ccache with kinit:\n" + kinitResult);
        if (success && kinitResult.length() == 0) { //kinit should return silently if successful
            Log.info(c, m, "Successfully generated a ccache at: " + ccPath);
            if (expired)
                TimeUnit.SECONDS.sleep(1); //Wait 1s to ensure ccache credentials are expired
        } else {
            Log.info(c, m, "FAILED to create ccache");
            throw new Exception("Failed to create Kerberos ticket cache. Kinit output was: " + kinitResult);
        }
    }

    private void updateConfigAndWait(ServerConfiguration config) throws Exception {
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
    }

    private static String readInputStream(InputStream is) {
        @SuppressWarnings("resource")
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
