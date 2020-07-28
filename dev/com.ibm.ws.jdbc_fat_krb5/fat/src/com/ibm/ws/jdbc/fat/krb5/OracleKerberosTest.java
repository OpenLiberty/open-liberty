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
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jdbc.fat.krb5.containers.KerberosPlatformRule;
import com.ibm.ws.jdbc.fat.krb5.containers.OracleKerberosContainer;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
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
        server.setJvmOptions(jvmOpts);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Exception firstError = null;

        try {
            server.stopServer();
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

}
