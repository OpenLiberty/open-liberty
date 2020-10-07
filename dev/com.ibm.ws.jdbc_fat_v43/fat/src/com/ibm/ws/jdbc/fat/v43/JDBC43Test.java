/*******************************************************************************
 * Copyright (c) 2018,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.v43;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.test.d43.jdbc.D43Driver;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.fat.v43.web.HandleListTestServlet;
import jdbc.fat.v43.web.JDBC43TestServlet;

@RunWith(FATRunner.class)
public class JDBC43Test extends FATServletClient {
    public static final String APP_NAME = "app43";

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .withoutModification()
                    .andWith(new JakartaEE9Action());

    @Server("com.ibm.ws.jdbc.fat.v43")
    @TestServlets({
                    @TestServlet(servlet = JDBC43TestServlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = HandleListTestServlet.class, contextRoot = APP_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "jdbc.fat.v43.web");

        RemoteFile derby = server.getFileFromLibertySharedDir("resources/derby/derby.jar");

        JavaArchive derbyJar = ShrinkWrap.create(ZipImporter.class, "derby.jar")
                        .importFrom(new File(derby.getAbsolutePath()))
                        .as(JavaArchive.class);

        JavaArchive d43driver = ShrinkWrap.create(JavaArchive.class, "d43driver.jar")
                        .addPackage("org.test.d43.jdbc")
                        .merge(derbyJar)
                        .addAsServiceProvider(java.sql.Driver.class, D43Driver.class);

        ShrinkHelper.exportToServer(server, "drivers", d43driver);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("DSRA0302E.*XA_RBOTHER", // raised by mock JDBC driver for XA operations after abort
                          "DSRA0302E.*XA_RBROLLBACK", // expected when invoking end on JDBC driver during an abort
                          "DSRA0304E", // expected when invoking end on JDBC driver during an abort
                          "DSRA8790W", // expected for begin/endRequest invoked by application being ignored
                          "J2CA0045E.*poolOf1", // expected when testing that no more connections are available
                          "J2CA0081E", // TODO why does Derby think a transaction is still active?
                          "WLTC0018E", // TODO remove once transactions bug is fixed
                          "WLTC0032W", // local tran rolled back, expected when trying to keep unshared connection open across servlet boundary
                          "WLTC0033W.*poolOf1"); // same reason as above
    }

    /**
     * testCompletionStageCachesUnsharedAutocommitConnectionAcrossServletBoundary - obtains an unshared connection in
     * one servlet method and uses it, but does not close it, instead allowing it to continue to execution operations
     * asynchronously via a completion stage. After the servlet method ends and goes out of scope, invokes a second
     * servlet method, which under a new scope, accesses the completion stage, joins it, and checks that the operations
     * executed successfully.
     * This behavior is currently possible without the HandleList implementation in place to prevent it.
     * Although we do not recommend this pattern, some users might be relying upon it, and there needs to be a way
     * to avoid breaking these users when HandleList is put in place to clean up connections when requests go out of scope.
     */
    @Test
    public void testCompletionStageCachesUnsharedAutocommitConnectionAcrossServletBoundary() throws Exception {
        runTest(server, "app43/JDBC43TestServlet", "testCompletionStageCachesUnsharedAutocommitConnectionAcrossServletBoundaryPart1");
        runTest(server, "app43/JDBC43TestServlet", "testCompletionStageCachesUnsharedAutocommitConnectionAcrossServletBoundaryPart2");
    }

    /**
     * testCompletionStageCachesUnsharedManualCommitConnectionAcrossServletBoundary - obtains an unshared connection in
     * one servlet method and uses it, but does not commit it or close it, instead allowing it to continue to execution
     * operations under the same transaction asynchronously via a completion stage, which ultimately commits the transaction
     * if successful and closes the connection. After the servlet method ends and goes out of scope, invokes a second servlet method,
     * which under a new scope, accesses the completion stage, joins it, and checks that the operations executed successfully.
     * When the servlet goes out of scope, the unresolved database local transaction is automatically rolled back per the
     * default action for unresolved transactions. The connection appears to be usable after this, with autocommit still false.
     * This behavior is currently possible without the HandleList implementation in place to prevent it.
     * TODO Is it necessary to preserve this behavior of leaving the connection open for further use?
     * It is a bad practice to ever have code that relies on the unresolved action to resolve transactions.
     */
    @ExpectedFFDC("com.ibm.ws.LocalTransaction.RolledbackException")
    @Test
    public void testCompletionStageCachesUnsharedManualCommitConnectionAcrossServletBoundary() throws Exception {
        runTest(server, "app43/JDBC43TestServlet", "testCompletionStageCachesUnsharedManualCommitConnectionAcrossServletBoundaryPart1");
        runTest(server, "app43/JDBC43TestServlet", "testCompletionStageCachesUnsharedManualCommitConnectionAcrossServletBoundaryPart2");
    }

    /**
     * testHandleListClosesLeakedConnectionsFromSeparateRequests - make two separate servlet requests that each intentionally leak a connection,
     * using up all of the connections in the pool. Make a third servlet request that requires a connection and expect it to work because the
     * HandleList enabled the two leaked connections to be closed out and returned to the connection pool.
     */
    //@Test TODO enable once HandleList is added
    public void testHandleListClosesLeakedConnectionsFromSeparateRequests() throws Exception {
        runTest(server, "app43/JDBC43TestServlet", "testLeakConnection");
        runTest(server, "app43/JDBC43TestServlet", "testLeakConnection");
        runTest(server, "app43/JDBC43TestServlet", "testLeakedConnectionsWereReturned&invokedBy=testHandleListClosesLeakedConnectionsFromSeparateRequests");
    }

    /**
     * testHandleListClosesLeakedConnectionsFromSingleRequests - make a single servlet request that each intentionally leaks both of the
     * connections from the pool. Make second servlet request that requires a connection and expect it to work because the
     * HandleList enabled the two leaked connections to be closed out and returned to the connection pool.
     */
    //@Test TODO enable once HandleList is added
    public void testHandleListClosesLeakedConnectionsFromSingleRequest() throws Exception {
        runTest(server, "app43/JDBC43TestServlet", "testLeakConnections");
        runTest(server, "app43/JDBC43TestServlet", "testLeakedConnectionsWereReturned&invokedBy=testHandleListClosesLeakedConnectionsFromSingleRequest");
    }
}
