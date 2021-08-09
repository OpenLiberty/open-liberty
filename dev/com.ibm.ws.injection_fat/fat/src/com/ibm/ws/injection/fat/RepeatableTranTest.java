/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection.fat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * This test case ensures that repeated @Resource and XML can be declared
 * and inject a resource-env-ref (UserTransaction and
 * TransactionSynchronizationRegistry) into the fields and methods of servlet
 * listeners and filters. It also checks that @Resource can be declared at the
 * class-level of servlet listeners and filters and will create a JNDI resource;
 *
 * To perform the test, a servlet is invoked in the web module with a listener
 * or filter declared in the web.xml. The expected result is that the listener
 * or filter is created and injected an appropriate UserTransaction of
 * TransactionSynchronizationRegistry.
 *
 * @author bmdecker
 *
 */
@RunWith(FATRunner.class)
public class RepeatableTranTest extends FATServletClient {

    private static final String SERVLET_BASIC_TRAN_SYNC = "RepeatableTransactionWeb/BasicRepeatableTranSynchRegistryServlet";
    private static final String SERVLET_BASIC_USER_TRAN = "RepeatableTransactionWeb/BasicRepeatableUserTransactionServlet";
    private static final String SERVLET_ADV_TRAN = "RepeatableTransactionWeb/AdvRepeatableTransactionServlet";

    @Server("com.ibm.ws.injection.fat.RepeatableTranServer")
//    @TestServlets({ @TestServlet(servlet = BasicRepeatableTranSynchRegistryServlet.class, contextRoot = "RepeatableTransactionWeb"),
//                    @TestServlet(servlet = BasicRepeatableUserTransactionServlet.class, contextRoot = "RepeatableTransactionWeb"),
//                    @TestServlet(servlet = AdvRepeatableTransactionServlet.class, contextRoot = "RepeatableTransactionWeb")
//    })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(new JakartaEE9Action().forServers("com.ibm.ws.injection.fat.RepeatableTranServer").fullFATOnly());

    @BeforeClass
    public static void setUp() throws Exception {
        // Because of a bug / gap in function in the JDK 11 compiler, we cannot compile these applications with a source=8
        // and also override the bootclasspath to use javax.annotation 1.3. For this reason, we are going to check in the
        // app as binaries compiled on Java 8 so we can continue to have coverage on JDK 8+
        // If these apps ever need to be changed:
        //   1) add the app path back to the 'src' list in bnd.bnd
        //   2) add the app path back to the .classpath
        //   3) un-comment the Shrinkwrap code to build the app in the respective test class

//        // Use ShrinkHelper to build the ears
//        WebArchive RepeatableTransactionWeb = ShrinkHelper.buildDefaultApp("RepeatableTransactionWeb.war", "com.ibm.ws.injection.repeatable.transaction.web.");
//        EnterpriseArchive RepeatableTransactionTest = ShrinkWrap.create(EnterpriseArchive.class, "RepeatableTransactionTest.ear");
//        RepeatableTransactionTest.addAsModule(RepeatableTransactionWeb);
//
//        ShrinkHelper.exportDropinAppToServer(server, RepeatableTransactionTest);

        // Since not using ShrinkWrap, manually transform the application if required
        if (JakartaEE9Action.isActive()) {
            transformJakartaEE9App(server, "dropins", "RepeatableTransactionTest.ear");

            // And switch to JDBC 4.1 just for some variety (JDBC not tied to EE level)
            ServerConfiguration config = server.getServerConfiguration();
            Set<String> features = config.getFeatureManager().getFeatures();
            for (String feature : features) {
                if (feature.startsWith("jdbc-")) {
                    features.remove(feature);
                }
            }
            features.add("jdbc-4.1");
            server.updateServerConfiguration(config);
        }

        server.addInstalledAppForValidation("RepeatableTransactionTest");

        server.startServer();
    }

    private static void transformJakartaEE9App(LibertyServer server, String path, String filename) throws Exception {
        String localLocation = "publish/servers/" + server.getServerName() + "/" + path;

        Path localAppPath = Paths.get(localLocation + "/" + filename);
        JakartaEE9Action.transformApp(localAppPath);

        server.copyFileToLibertyServerRoot(localLocation, path, filename);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    private final void runTest(String path) throws Exception {
        FATServletClient.runTest(server, path, getTestMethodSimpleName());
    }

    @Test
    public void testRepeatableTranSynchRegistryFldAnnInjection() throws Exception {
        runTest(SERVLET_BASIC_TRAN_SYNC);
    }

    @Test
    public void testRepeatableTranSynchRegistryFldXMLInjection() throws Exception {
        runTest(SERVLET_BASIC_TRAN_SYNC);
    }

    @Test
    public void testRepeatableTranSynchRegistryMthdAnnInjection() throws Exception {
        runTest(SERVLET_BASIC_TRAN_SYNC);
    }

    @Test
    public void testRepeatableTranSynchRegistryMthdXMLInjection() throws Exception {
        runTest(SERVLET_BASIC_TRAN_SYNC);
    }

    @Test
    public void testRepeatableTranSynchRegistyClassLevelResourceInjection() throws Exception {
        runTest(SERVLET_BASIC_TRAN_SYNC);
    }

    @Test
    public void testRepeatableUserTransactionClassLevelResourceInjection() throws Exception {
        runTest(SERVLET_BASIC_USER_TRAN);
    }

    @Test
    public void testRepeatableUserTransactionFldAnnInjection() throws Exception {
        runTest(SERVLET_BASIC_USER_TRAN);
    }

    @Test
    public void testRepeatableUserTransactionFldXMLInjection() throws Exception {
        runTest(SERVLET_BASIC_USER_TRAN);
    }

    @Test
    public void testRepeatableUserTransactionMthdAnnInjection() throws Exception {
        runTest(SERVLET_BASIC_USER_TRAN);
    }

    @Test
    public void testRepeatableUserTransactionMthdXMLInjection() throws Exception {
        runTest(SERVLET_BASIC_USER_TRAN);
    }

    @Test
    public void testRepeatableTransactionHttpSessionAttributeListener() throws Exception {
        runTest(SERVLET_ADV_TRAN);
    }

    @Test
    public void testRepeatableTransactionHttpSessionListener() throws Exception {
        runTest(SERVLET_ADV_TRAN);
    }

    @Test
    public void testRepeatableTransactionRequestListener() throws Exception {
        runTest(SERVLET_ADV_TRAN);
    }

    @Test
    public void testRepeatableTransactionServletContextAttributeListener() throws Exception {
        runTest(SERVLET_ADV_TRAN);
    }

    @Test
    public void testRepeatableTransactionServletContextListener() throws Exception {
        runTest(SERVLET_ADV_TRAN);
    }

    @Test
    public void testRepeatableTransactionServletFilter() throws Exception {
        runTest(SERVLET_ADV_TRAN);
    }

    @Test
    public void testRepeatableTransactionServletRequestAttributeListener() throws Exception {
        runTest(SERVLET_ADV_TRAN);
    }
}