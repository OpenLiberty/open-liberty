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
package com.ibm.ws.jdbc.fat.db2;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Network;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;
import componenttest.topology.utils.FATServletClient;
import db2.web.JDBCKerberosTestServlet;

@RunWith(FATRunner.class)
public class JDBCKerberosTest extends FATServletClient {

    private static final Class<?> c = JDBCKerberosTest.class;

    public static final String KRB5_REALM = "EXAMPLE.COM";
    public static final String KRB5_KDC = "kerberos";
    public static final String KRB5_USER = "dbuser";
    public static final String KRB5_PASS = "password";

    public static final String APP_NAME = "db2fat";

    public static final Network network = Network.newNetwork();
    public static final KerberosContainer krb5 = new KerberosContainer(network);
    public static final DB2KerberosContainer db2 = new DB2KerberosContainer(network);

    @Server("com.ibm.ws.jdbc.fat.krb5")
    @TestServlet(servlet = JDBCKerberosTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static KerberosPlatformRule skipRule = new KerberosPlatformRule();

    @BeforeClass
    public static void setUp() throws Exception {
        // Allows local tests to switch between using a local docker client, to using a remote docker client.
        ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();

        krb5.start();
        Path krbConfPath = Paths.get(server.getServerRoot(), "security", "krb5.conf");
        krb5.generateConf(krbConfPath);
        //krb5.configureKerberos();
        //krb5.generateKeytab();

        db2.start();

        ShrinkHelper.defaultApp(server, APP_NAME, "db2.web");

        server.addEnvVar("DB2_DBNAME", db2.getDatabaseName());
        server.addEnvVar("DB2_HOSTNAME", db2.getContainerIpAddress());
        server.addEnvVar("DB2_PORT", "" + db2.getMappedPort(50000));
        server.addEnvVar("DB2_USER", db2.getUsername());
        server.addEnvVar("DB2_PASS", db2.getPassword());
        server.addEnvVar("KRB5_USER", KRB5_USER);
        List<String> jvmOpts = new ArrayList<>();
        jvmOpts.add("-Djava.security.krb5.conf=" + krbConfPath.toAbsolutePath());
        jvmOpts.add("-Dsun.security.krb5.debug=true");
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
            db2.stop();
        } catch (Exception e) {
            if (firstError == null)
                firstError = e;
            Log.error(c, "tearDown", e);
        }
        try {
            krb5.stop();
        } catch (Exception e) {
            if (firstError == null)
                firstError = e;
            Log.error(c, "tearDown", e);
        }
        if (!FATRunner.FAT_TEST_LOCALRUN) {
            network.close();
        }

        if (firstError != null)
            throw firstError;
    }

}
