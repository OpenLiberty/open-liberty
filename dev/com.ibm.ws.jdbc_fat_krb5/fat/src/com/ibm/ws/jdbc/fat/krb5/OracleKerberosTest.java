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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jdbc.fat.krb5.containers.KerberosPlatformRule;
import com.ibm.ws.jdbc.fat.krb5.containers.OracleKerberosContainer;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.JavaInfo.Vendor;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.krb5.oracle.web.OracleKerberosTestServlet;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class OracleKerberosTest extends FATServletClient {

    private static final Class<?> c = OracleKerberosTest.class;

    public static final String APP_NAME = "krb5-oracle-app";

    public static final OracleKerberosContainer oracle = new OracleKerberosContainer(FATSuite.network);

    @Server("com.ibm.ws.jdbc.fat.krb5.oracle")
    @TestServlet(servlet = OracleKerberosTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static KerberosPlatformRule skipRule = new KerberosPlatformRule();

    @ClassRule
    public static IBMJava8Rule skipOnIBMJava8 = new IBMJava8Rule();

    @BeforeClass
    public static void setUp() throws Exception {
        Path krbConfPath = Paths.get(server.getServerRoot(), "security", "krb5.conf");
        FATSuite.krb5.generateConf(krbConfPath);

        oracle.start();

        ShrinkHelper.defaultDropinApp(server, APP_NAME, "jdbc.krb5.oracle.web");

        server.addEnvVar("ORACLE_DBNAME", oracle.getDatabaseName());
        server.addEnvVar("ORACLE_HOSTNAME", oracle.getContainerIpAddress());
        server.addEnvVar("ORACLE_PORT", "" + oracle.getMappedPort(1521));
        server.addEnvVar("ORACLE_USER", oracle.getUsername());
        server.addEnvVar("ORACLE_PASS", oracle.getPassword());
        server.addEnvVar("KRB5_USER", oracle.getKerberosUsername());
        server.addEnvVar("KRB5_CONF", krbConfPath.toAbsolutePath().toString());
        List<String> jvmOpts = new ArrayList<>();
        jvmOpts.add("-Dsun.security.krb5.debug=true"); // Hotspot/OpenJ9
        jvmOpts.add("-Dsun.security.jgss.debug=true");
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
            server.stopServer("CWWKS4345E: .*BOGUS_KEYTAB"); // expected by testKerberosUsingPassword);
        } catch (Exception e) {
            firstError = e;
            Log.error(c, "tearDown", e);
        }
        try {
            oracle.stop();
        } catch (Exception e) {
            if (firstError == null)
                firstError = e;
            Log.error(c, "tearDown", e);
        }

        if (firstError != null)
            throw firstError;
    }

    /**
     * Test that the 'password' attribute of an authData element can be used to supply a Kerberos password.
     * Normally a keytab file takes precedence over this, so perform dynamic config for the test to temporarily
     * set the keytab location to an invalid location to confirm that the supplied password actually gets used.
     */
    @Test
    @AllowedFFDC //Servlet attempts getConnection multiple times until the kerberos service is up.  Expect FFDCs
    public void testKerberosUsingPassword() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        String originalKeytab = config.getKerberos().keytab;
        try {
            Log.info(c, testName.getMethodName(), "Changing the keystore to an invalid value so that password from the <authData> gets used");
            config.getKerberos().keytab = "BOGUS_KEYTAB";
            updateConfigAndWait(config);

            FATServletClient.runTest(server, APP_NAME + "/OracleKerberosTestServlet", testName);
        } finally {
            Log.info(c, testName.getMethodName(), "Restoring original config");
            config.getKerberos().keytab = originalKeytab;
            updateConfigAndWait(config);
        }
    }

    private static class IBMJava8Rule implements TestRule {

        @Override
        public Statement apply(Statement stmt, Description desc) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    if (shouldRun(desc)) {
                        stmt.evaluate();
                    }
                }
            };
        }

        public static boolean shouldRun(Description desc) {
            Class<?> c = desc == null ? KerberosPlatformRule.class : desc.getTestClass();
            String m = (desc == null || desc.getMethodName() == null) ? "shouldRun" : desc.getMethodName();

            JavaInfo java = JavaInfo.forCurrentVM();
            if (java.majorVersion() == 8 && java.vendor() == Vendor.IBM) {
                Log.info(c, m, "Skipping tests because Oracle JDBC driver does not work with IBM JDK 8");
                return false;
            }

            return true;
        }

    }

    private void updateConfigAndWait(ServerConfiguration config) throws Exception {
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
    }

}
