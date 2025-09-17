/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Container.ExecResult;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jdbc.fat.krb5.containers.OracleKerberosContainer;
import com.ibm.ws.jdbc.fat.krb5.rules.IBMJava8Rule;
import com.ibm.ws.jdbc.fat.krb5.rules.KerberosPlatformRule;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.krb5.oracle.web.OracleKerberosTestServlet;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
//TODO The current Oracle JDBC driver (ojdbc11.jar v23.4.0.24.05) only supports Java 11-21
//modify/remove this line once it or another driver release supports 23+
@MaximumJavaLevel(javaLevel = 21)
public class OracleKerberosTest extends FATServletClient {

    private static final Class<?> c = OracleKerberosTest.class;

    public static final String APP_NAME = "krb5-oracle-app";

    @Server("com.ibm.ws.jdbc.fat.krb5.oracle")
    @TestServlet(servlet = OracleKerberosTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    public static OracleKerberosContainer oracle = new OracleKerberosContainer(FATSuite.network);

    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(KerberosPlatformRule.instance()).around(IBMJava8Rule.instance()).around(oracle);

    @BeforeClass
    public static void setUp() throws Exception {
        // Generate krb5.conf in server/security directory
        Path krbConfPath = Paths.get(server.getServerRoot(), "security", "krb5.conf");
        FATSuite.krb5.generateConf(krbConfPath);

        // Generate krb5.keytab in KDC container, and then copy it to server/security directory
        Path krb5KeytabPath = Paths.get(server.getServerRoot(), "security", "krb5.keytab");
        ExecResult result = FATSuite.krb5.execInContainer("kadmin.local", "-q", "ktadd -k /tmp/client_krb5.keytab ORACLEUSR");
        if (result.getExitCode() != 0) {
            Log.info(c, "setup", "STDOUT: " + result.getStdout());
            Log.info(c, "setup", "STDERR: " + result.getStderr());
        }
        FATSuite.krb5.copyFileFromContainer("/tmp/client_krb5.keytab", krb5KeytabPath.toAbsolutePath().toString());

        // Dropin application
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "jdbc.krb5.oracle.web");

        // Setup environment variables
        server.addEnvVar("ORACLE_DRIVER", getDriverName());
        server.addEnvVar("ORACLE_DBNAME", oracle.getDatabaseName());
        server.addEnvVar("ORACLE_HOSTNAME", oracle.getHost());
        server.addEnvVar("ORACLE_PORT", "" + oracle.getMappedPort(1521));
        server.addEnvVar("ORACLE_USER", oracle.getUsername());
        server.addEnvVar("ORACLE_PASS", oracle.getPassword());
        server.addEnvVar("KRB5_USER", oracle.getKerberosUsername());
        server.addEnvVar("KRB5_KEYTAB", krb5KeytabPath.toAbsolutePath().toString());
        server.addEnvVar("KRB5_CONF", krbConfPath.toAbsolutePath().toString());

        // Add JVM properties
        List<String> jvmOpts = new ArrayList<>();
        jvmOpts.add("-Dsun.security.krb5.debug=true"); // Hotspot/OpenJ9
        jvmOpts.add("-Dsun.security.jgss.debug=true");
        jvmOpts.add("-Dcom.ibm.security.krb5.krb5Debug=true"); // IBM JDK
        server.setJvmOptions(jvmOpts);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKS4345E: .*BOGUS_KEYTAB"); // expected by testKerberosUsingPassword);
    }

    /**
     * Get driver name based on the Java version
     *
     * @return name of JDBC driver
     */
    private static String getDriverName() {
        if (JavaInfo.JAVA_VERSION >= 1.8 && JavaInfo.JAVA_VERSION < 11) {
            return "ojdbc8.jar";
        }

        if (JavaInfo.JAVA_VERSION >= 11 && JavaInfo.JAVA_VERSION < 21) {
            return "ojdbc11.jar";
        }

        // TODO update to next ojdbc version that supports Java 23
        // for now use ojdbc11.jar as a place holder for local testing.
        return "ojdbc11.jar";
    }

    /**
     * Test that the 'password' attribute of an authData element can be used to supply a Kerberos password.
     * Normally a keytab file takes precedence over this, so perform dynamic config for the test to temporarily
     * set the keytab location to an invalid location to confirm that the supplied password actually gets used.
     *
     * NOTE: It is not clear to me which head is responsible for this behavior.
     * It seems like the client (Liberty) should be performing this failback to using password based authentication
     * but the login module fails with:
     * Caused by: javax.security.auth.login.LoginException: Pre-authentication information was invalid (24) - PREAUTH_FAILED
     * at jdk.security.auth/com.sun.security.auth.module.Krb5LoginModule.attemptAuthentication(Krb5LoginModule.java:789)
     * at jdk.security.auth/com.sun.security.auth.module.Krb5LoginModule.login(Krb5LoginModule.java:597)
     * at com.ibm.ws.security.kerberos.auth.Krb5LoginModuleWrapper.login(Krb5LoginModuleWrapper.java:171)
     * at com.ibm.ws.security.kerberos.auth.KerberosService.doKerberosLogin(KerberosService.java:269)
     * at com.ibm.ws.security.kerberos.auth.KerberosService.getOrCreateSubject(KerberosService.java:231)
     * at com.ibm.ws.security.jca.internal.AuthDataServiceImpl.obtainSubject(AuthDataServiceImpl.java:167)
     * at com.ibm.ws.security.jca.internal.AuthDataServiceImpl.createSubjectUsingAuthData(AuthDataServiceImpl.java:146)
     * at com.ibm.ws.security.jca.internal.AuthDataServiceImpl.getSubject(AuthDataServiceImpl.java:129)
     * at com.ibm.ejs.j2c.ConnectionManager$1.run(ConnectionManager.java:1850)
     * at com.ibm.ejs.j2c.ConnectionManager$1.run(ConnectionManager.java:1847)
     * at java.base/java.security.AccessController.doPrivileged(AccessController.java:748)
     * at com.ibm.ejs.j2c.ConnectionManager.getFinalSubject(ConnectionManager.java:1847)
     * ... 38 more
     * Caused by: KrbException: Pre-authentication information was invalid (24) - PREAUTH_FAILED
     * at java.security.jgss/sun.security.krb5.KrbAsRep.<init>(KrbAsRep.java:82)
     * at java.security.jgss/sun.security.krb5.KrbAsReqBuilder.send(KrbAsReqBuilder.java:345)
     * at java.security.jgss/sun.security.krb5.KrbAsReqBuilder.action(KrbAsReqBuilder.java:498)
     * at jdk.security.auth/com.sun.security.auth.module.Krb5LoginModule.attemptAuthentication(Krb5LoginModule.java:751)
     * ... 49 more
     * Caused by: KrbException: Identifier doesn't match expected value (906)
     * at java.security.jgss/sun.security.krb5.internal.KDCRep.init(KDCRep.java:157)
     * at java.security.jgss/sun.security.krb5.internal.ASRep.init(ASRep.java:64)
     * at java.security.jgss/sun.security.krb5.internal.ASRep.<init>(ASRep.java:59)
     * at java.security.jgss/sun.security.krb5.KrbAsRep.<init>(KrbAsRep.java:60)
     */
    @Test
    @AllowedFFDC //Servlet attempts getConnection multiple times until the kerberos service is up.  Expect FFDCs
    @Ignore("TODO This test now fails after the Pre-Authentication step fails")
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

    private void updateConfigAndWait(ServerConfiguration config) throws Exception {
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME));
    }

}
