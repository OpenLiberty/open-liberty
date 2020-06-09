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

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Network;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class JDBCKerberosTest extends FATServletClient {

    public static final String KRB5_REALM = "EXAMPLE.COM";
    public static final String KRB5_KDC = "kerberos";
    public static final String KRB5_PASS = "password";

    public static final String APP_NAME = "db2fat";

    public static Network network = Network.newNetwork();
    public static KerberosContainer krb5 = new KerberosContainer(network);
    public static DB2KerberosContainer db2 = new DB2KerberosContainer(network);

//    @Server("com.ibm.ws.jdbc.fat.db2")
//    @TestServlet(servlet = JDBCKerberosTestServlet.class, path = APP_NAME + '/' + SERVLET_NAME)
//    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        //Allows local tests to switch between using a local docker client, to using a remote docker client.
//      ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();

        krb5.start();
        db2.start();
//
//        ShrinkHelper.defaultApp(server, APP_NAME, "db2.web");
//
//        server.addEnvVar("DB2_DBNAME", db2.getDatabaseName());
//        server.addEnvVar("DB2_HOSTNAME", db2.getContainerIpAddress());
//        server.addEnvVar("DB2_PORT", String.valueOf(db2.getMappedPort(50000)));
//        server.addEnvVar("DB2_USER", db2.getUsername());
//        server.addEnvVar("DB2_PASS", db2.getPassword());
//
//        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        List<Exception> errors = new ArrayList<>();

        doSafely(errors, () -> krb5.stop());
        doSafely(errors, () -> db2.stop());

        if (!errors.isEmpty())
            throw errors.get(0);
    }

    private static void doSafely(List<Exception> errors, Runnable action) {
        try {
            action.run();
        } catch (Exception t) {
            Log.error(JDBCKerberosTest.class, "tearDown", t);
            errors.add(t);
        }
    }

    @Test
    public void dummyTest() {
        Log.info(getClass(), "@AGG", "Dummy test");
    }
}
