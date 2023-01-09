/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package suite.r80.base.jca16.ann;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import junit.framework.AssertionFailedError;
import suite.r80.base.jca16.RarTests;
import suite.r80.base.jca16.TestSetupUtils;

/**
 * Class <CODE>ConnectionDefinitionsMergeActionTest</CODE> verifies the
 * correctness of JCA 1.6 RAR metadata emitted by WAS merge operations
 * for JavaBeans annotated as @ConnectionDefinitions. The relation between
 * the annotation and the expected metadata is shown below.
 * 
 * <CODE>
 * ...
 * @ConnectionDefinitions(
 * value={@ConnectionDefinition(
 * connectionDefinition=CD_1_CONNFACT_INTF,
 * connectionFactory=CD_1_CONNFACT_IMPL,
 * connection=CD_1_CONN_INTF,
 * connectionFactory=CD_1_CONN_IMPL),
 * @ConnectionDefinition(
 * connectionDefinition=CD_2_CONNFACT_INTF,
 * connectionFactory=CD_2_CONNFACT_IMPL,
 * connection=CD_2_CONN_INTF,
 * connectionFactory=CD_2_CONN_IMPL) } )
 * class MANAGEDCONNFACT_JAVABEAN
 * ...
 * </CODE>
 * 
 * Is equivalent to the merged DD:
 * 
 * <CODE>
 * ...
 * <outbound-resourceadapter>
 * <connection-definition>
 * <managedconnectionfactory-class>MANAGEDCONNFACT_JAVABEAN</managedconnectionfactory-class>
 * <connectionfactory-interface>CD_CONNFACT_INTF_1</connectionfactory-interface>
 * <connectionfactory-impl-class>CD_CONNFACT_IMPL_1</connectionfactory-impl-class>
 * <connection-interface>CD_CONN_INTF_1</connection-interface>
 * <connection-impl-class>CD_CONN_IMPL_1</connection-impl-class></connection-definition>
 * <connection-definition>
 * <connection-definition>
 * <managedconnectionfactory-class>MANAGEDCONNFACT_JAVABEAN</managedconnectionfactory-class>
 * <connectionfactory-interface>CD_CONNFACT_INTF_2</connectionfactory-interface>
 * <connectionfactory-impl-class>CD_CONNFACT_IMPL_2</connectionfactory-impl-class>
 * <connection-interface>CD_CONN_INTF_2</connection-interface>
 * <connection-impl-class>CD_CONN_IMPL_2</connection-impl-class></connection-definition>
 * <connection-definition>
 * </outbound-resourceadapter>
 * ...
 * </CODE>
 * 
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class ConnectionDefinitionsMergeActionTest {

    private final static String CLASSNAME = ConnectionDefinitionsMergeActionTest.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASSNAME);

    private final static Class<?> c = ConnectionDefinitionsMergeActionTest.class;
    protected static LibertyServer server;

    public ConnectionDefinitionsMergeActionTest() {
    }

    ////////////////////
    // Merge action tests

    int mergedConnDefs = 0;
    int nExpectedConnDefs = 0;

    String metatype = null;

    String rarDisplayName = null;
    public AnnUtils annUtils = new AnnUtils();

    final String CD_1 = "com.ibm.tra.ann.ConnDefAnn1",
                    CD_1_CONNFACT_INTF = "javax.resource.cci.ConnectionFactory",
                    CD_1_CONNFACT_IMPL = "com.ibm.tra.outbound.impl.J2CConnectionFactory",
                    CD_1_CONN_INTF = "javax.resource.cci.Connection",
                    CD_1_CONN_IMPL = "com.ibm.tra.outbound.impl.J2CConnection",

                    CD_2 = "com.ibm.tra.ann.ConnDefAnn2",
                    CD_2_CONNFACT_INTF = "com.ibm.tra.outbound.base.ConnectionFactoryBase",
                    CD_2_CONNFACT_IMPL = "com.ibm.tra.outbound.impl.J2CConnectionFactory",
                    CD_2_CONN_INTF = "com.ibm.tra.outbound.base.ConnectionBase",
                    CD_2_CONN_IMPL = "com.ibm.tra.outbound.impl.J2CConnection";

    private StringBuilder runInServlet(String test, String servlet,
                                       String webmodule) throws IOException {
        URL url = new URL("http://" + server.getHostname() + ":"
                          + server.getHttpDefaultPort() + "/" + webmodule + "/" + servlet
                          + "?test=" + test);
        Log.info(getClass(), "runInServlet", "URL is " + url);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(c, "runInServlet", line);
            }

            // Look for success message, otherwise fail test
            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0) {
                Log.info(c, "runInServlet",
                         "failed to find completed successfully message");
                fail("Missing success message in output. " + lines);
            }
            return lines;
        } catch (IOException x) {
            throw x;
        } finally {
            con.disconnect();
        }

    }

    @BeforeClass
    public static void setUp() throws Exception {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "setUp");
        }
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jca.fat.regr", null, false);
        server.setServerConfigurationFile("ConnectionDefinitionsMergeActionTest_server.xml");

        TestSetupUtils.setupAnnapp(server);

//      Package TRA_jca16_ann_ConnectionDefinitionsMergeAction_NoCD.rar
        JavaArchive traAnnEjsj2CResourceAdapter_jar = TestSetupUtils.getTraAnnEjsResourceAdapter_jar();
        traAnnEjsj2CResourceAdapter_jar.addPackages(true, "com.ibm.ws.j2c");

        JavaArchive connectionDefinitions_NoCD_jar = ShrinkWrap.create(JavaArchive.class, "ConnectionDefinitions_NoCD.jar");
        connectionDefinitions_NoCD_jar.addClass("com.ibm.tra.ann.ConnDefsAnn1");

        ResourceAdapterArchive TRA_jca16_ann_ConnectionDefinitionsMergeAction_NoCD = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                       RarTests.TRA_jca16_ann_ConnectionDefinitionsMergeAction_NoCD
                                                                                                                                     + ".rar").addAsLibrary(traAnnEjsj2CResourceAdapter_jar).addAsLibrary(connectionDefinitions_NoCD_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                         + RarTests.TRA_jca16_ann_ConnectionDefinitionsMergeAction_NoCD
                                                                                                                                                                                                                                                                         + "-ra.xml"),
                                                                                                                                                                                                                                                                "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConnectionDefinitionsMergeAction_NoCD);

//      Package TRA_jca16_ann_ConnectionDefinitionsMergeAction_SingleCD.rar
        JavaArchive connectionDefinitions_SingleCD_jar = ShrinkWrap.create(JavaArchive.class, "ConnectionDefinitions_SingleCD.jar");
        connectionDefinitions_SingleCD_jar.addClass("com.ibm.tra.ann.ConnDefsAnn2");

        ResourceAdapterArchive TRA_jca16_ann_ConnectionDefinitionsMergeAction_SingleCD = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                           RarTests.TRA_jca16_ann_ConnectionDefinitionsMergeAction_SingleCD
                                                                                                                                         + ".rar").addAsLibrary(traAnnEjsj2CResourceAdapter_jar).addAsLibrary(connectionDefinitions_SingleCD_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                 + RarTests.TRA_jca16_ann_ConnectionDefinitionsMergeAction_SingleCD
                                                                                                                                                                                                                                                                                 + "-ra.xml"),
                                                                                                                                                                                                                                                                        "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConnectionDefinitionsMergeAction_SingleCD);

        server.startServer("ConnectionDefinitionsMergeActionTest.log");
        server.waitForStringInLogUsingMark("CWWKE0002I");

        assertNotNull("FeatureManager should report update is complete",
                      server.waitForStringInLogUsingMark("CWWKF0008I"));

        assertNotNull("Server should report it has started",
                      server.waitForStringInLogUsingMark("CWWKF0011I"));

        String msg = server.waitForStringInLog("J2CA7001I:.*TRA_jca16_ann_ConnectionDefinitionsMergeAction_NoCD.*",
                                               server.getMatchingLogFile("messages.log"));
        assertNotNull("Could not find resource adapter successful install message for TRA_jca16_ann_ConnectionDefinitionsMergeAction_NoCD.rar ",
                      msg);

        msg = server.waitForStringInLog("J2CA7001I:.*TRA_jca16_ann_ConnectionDefinitionsMergeAction_SingleCD.*",
                                        server.getMatchingLogFile("messages.log"));
        assertNotNull("Could not find resource adapter successful install message for TRA_jca16_ann_ConnectionDefinitionsMergeAction_SingleCD.rar ",
                      msg);

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "tearDown");
        }
        if (server.isStarted()) {
            server.stopServer("CWWKE0701E");// EXPECTED
        }
    }

    /**
     * Case 1: Validate that if @ConnectionDefinitions is specified with a single
     * 
     * @ConnectionDefinition in the value[] and that no connection-definition
     *                       object exists in the DD, that a single emf connectionDefinition is created
     *                       correctly and added to the correct location in the DD.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testSingleNoConnDefInDD() throws Throwable {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testSingleNoConnDefInDD");
        }

        rarDisplayName = RarTests.TRA_jca16_ann_ConnectionDefinitionsMergeAction_NoCD;
        metatype = annUtils.getMetatype(server, rarDisplayName);

        mergedConnDefs = 0;

        mergedConnDefs += annUtils.getConnectionDefinitions(metatype, "com.ibm.tra.ann.ConnDefsAnn1");
        nExpectedConnDefs = 1; // DD contains no <c-d>, ConnDefsAnn1(value=CD_1)

        assertEquals(
                     "The outbound-resourceadapter does not contain " + nExpectedConnDefs + " connection-definition(s).",
                     nExpectedConnDefs,
                     mergedConnDefs);

        runInServlet("testSingleNoConnDefInDD", "AnnTestServlet", "annweb");

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testSingleNoConnDefInDD");
        }
    }

    /**
     * Case 2: Validate that if @ConnectionDefinitions is specified with a single
     * 
     * @ConnectionDefinition in the value[] and that a connection-definition object
     *                       exists in the DD but with a different connectionfactory-interface, that
     *                       a single emf connectionDefinition is created correctly and added to the
     *                       correct location in the DD in addition to the existing connection-definition
     *                       object.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testSingleConnDefDiffCfInDD() throws Throwable {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testSingleConnDefSameCfInDD");
        }

        rarDisplayName = RarTests.TRA_jca16_ann_ConnectionDefinitionsMergeAction_SingleCD;
        metatype = annUtils.getMetatype(server, rarDisplayName);

        mergedConnDefs = 0;

        mergedConnDefs += annUtils.getConnectionDefinitions(metatype, "com.ibm.tra.outbound.impl.J2CManagedConnectionFactory");
        mergedConnDefs += annUtils.getConnectionDefinitions(metatype, "com.ibm.tra.ann.ConnDefsAnn2");

        //mergedConnDefs = annUtils.getConnectionDefinitions(metatype, "com.ibm.tra.ann.ConnDefsAnn1");
        nExpectedConnDefs = 2; // DD contains CD_1, ConnDefAnn1[CD_1] (same CF_INTF)
                               //                   ConnDefAnn2[cd_1,CD_2] (diff CF_INTF)

        assertEquals(
                     "The outbound-reosurceadapter does not contain " + nExpectedConnDefs + " connection-definitions.",
                     nExpectedConnDefs,
                     mergedConnDefs);

        runInServlet("testSingleConnDefDiffCfInDD", "AnnTestServlet", "annweb");

        // This case nearly verifies by case 3, below.  The RAR contains @CD_1 
        // containing elements identical to the single <cd_1> in the DD, and @CD_2 
        // having different elements (i.e. connectionFactory).  If test case 3 passes,
        // then the merge action did not create an additional <c-d>, provided the 
        // MCF_CLS matches that in <cd_1>, not CD_1.  Let's verify that now and 
        // skip test case 3.

        String DD_CD_MCF_CLS = "com.ibm.tra.outbound.impl.J2CManagedConnectionFactory";

        assertTrue(
                   "The <managedconnectionfactory-class> in ra.xml does not override the @ConnectionDefinition class.", annUtils.getMCF(metatype, rarDisplayName, DD_CD_MCF_CLS));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testSingleConnDefSameCfInDD");
        }
    }

    /**
     * Case 3: Validate that if @ConnectionDefinitions is specified with multiple
     * 
     * @ConnectionDefinition in the value[] and that a connection-definition object
     *                       exists in the DD with the same connectionfactory-interface as one of them,
     *                       that the existing connection-definition object is not changed and new
     *                       connection-definition objects are created for the others.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMultiConnDefDiffCfInDD() throws Throwable {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testSingleConnDefSameCfInDD");
        }

        rarDisplayName = RarTests.TRA_jca16_ann_ConnectionDefinitionsMergeAction_SingleCD;
        metatype = annUtils.getMetatype(server, rarDisplayName);

        mergedConnDefs = 0;

        mergedConnDefs += annUtils.getConnectionDefinitions(metatype, "com.ibm.tra.ann.ConnDefsAnn2");
        mergedConnDefs += annUtils.getConnectionDefinitions(metatype, "com.ibm.tra.outbound.impl.J2CManagedConnectionFactory");
        nExpectedConnDefs = 2; // DD contains CD_1, ConnDefsAnn1[CD_1]      (same CF_INTF)
                               //                   ConnDefsAnn2[CD_1,CD_2] (diff CF_INTF)

        assertEquals(
                     "The outbound-reosurceadapter does not contain " + nExpectedConnDefs + " connection-definitions.",
                     nExpectedConnDefs,
                     mergedConnDefs);

        // TODO: Order is not guranteed.  Make a utility method that matches
        // CDs based on elements.  This also eliminates need for validation
        // method!

        runInServlet("testSingleConnDefDiffCfInDD", "AnnTestServlet", "annweb");

        // This case nearly verifies by case 3, below.  The RAR contains @CD_1 
        // containing elements identical to the single <cd_1> in the DD, and @CD_2 
        // having different elements (i.e. connectionFactory).  If test case 3 passes,
        // then the merge action did not create an additional <c-d>, provided the 
        // MCF_CLS matches that in <cd_1>, not CD_1.  Let's verify that now and 
        // skip test case 3.

        String DD_CD_MCF_CLS = "com.ibm.tra.outbound.impl.J2CManagedConnectionFactory";

        assertTrue(
                   "The <managedconnectionfactory-class> in ra.xml does not override the @ConnectionDefinition class.", annUtils.getMCF(metatype, rarDisplayName, DD_CD_MCF_CLS));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testSingleConnDefSameCfInDD");
        }
    }

    // Merge action tests
    ////////////////////

}
