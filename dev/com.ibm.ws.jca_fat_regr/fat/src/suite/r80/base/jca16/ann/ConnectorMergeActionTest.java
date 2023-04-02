/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
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
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import junit.framework.AssertionFailedError;
import suite.r80.base.jca16.RarTests;

/**
 *
 */
@RunWith(FATRunner.class)
public class ConnectorMergeActionTest {
    private final static String CLASSNAME = ConnectorMergeActionTest.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASSNAME);
    @Server("com.ibm.ws.jca.fat.regr")
    public static LibertyServer server;
    public AnnUtils annUtils = new AnnUtils();

    private static final String raDir = "test-resourceadapters/adapter.regr/resources/META-INF/";

    public ConnectorMergeActionTest() {
    }

    private static ServerConfiguration originalServerConfig;

    String Property = null;
    String Value = null;
    String rarDisplayName = null;

    ////////////////////
    // @Connector

    final String DD_RA_CLS = "com.ibm.tra.SimpleRAImpl",
                    CONN_DEF_0_CLS = "com.ibm.tra.ann.ConnectorAnnDef0",
                    CONN_DEF_1_CLS = "com.ibm.tra.ann.ConnectorAnnDef1",
                    CONN_DEF_2_CLS = "com.ibm.tra.ann.ConnectorAnnDef2",
                    CONN_DEF_3_CLS = "com.ibm.tra.ann.ConnectorAnnDef3",
                    CONN_0_CLS = "com.ibm.tra.ann.ConnectorAnn0",
                    CONN_1_CLS = "com.ibm.tra.ann.ConnectorAnn1",
                    CONN_2_CLS = "com.ibm.tra.ann.ConnectorAnn2",
                    CONN_3_CLS = "com.ibm.tra.ann.ConnectorAnn3",
                    CONN_4_CLS = "com.ibm.tra.ann.ConnectorAnn4",
                    CONN_5_CLS = "com.ibm.tra.ann.ConnectorAnn5";

    final Boolean DEF_REAUTHSUPPORT = false,
                    DEF_LICREQ = false;
    final String DD_REAUTHSUPPORT = "true",
                    CONN_REAUTHSUPPORT = "true";

    final String DEF_LIC = "",
                    DEF_VERSION = "",
                    DEF_EISTYPE = "",
                    DEF_VENDORNAME = "";

    @BeforeClass
    public static void setUp() throws Exception {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "setUp");
        }

        JavaArchive resourceAdapter_jar = ShrinkWrap.create(JavaArchive.class, "cmatResourceAdapter.jar");
        resourceAdapter_jar.addPackages(false, "com.ibm.tra");
        resourceAdapter_jar.addPackages(false, "com.ibm.tra.inbound.base");
        resourceAdapter_jar.addPackages(false, "com.ibm.tra.inbound.impl");
        resourceAdapter_jar.addPackages(false, "com.ibm.tra.outbound.base");
        resourceAdapter_jar.addPackages(false, "com.ibm.tra.outbound.impl");
        resourceAdapter_jar.addPackages(false, "com.ibm.tra.trace");
        resourceAdapter_jar.addPackages(false, "com.ibm.tra.work");
        resourceAdapter_jar.addPackages(true, "com.ibm.ejs.ras");

        JavaArchive connector_DefaultElement0NoneInDD_jar = ShrinkWrap.create(JavaArchive.class, "Connector_DefaultElement0NoneInDD.jar");
        connector_DefaultElement0NoneInDD_jar.addClass("com.ibm.tra.ann.ConnectorAnnDef0");
        ResourceAdapterArchive rar1 = ShrinkWrap.create(ResourceAdapterArchive.class, RarTests.TRA_jca16_ann_ConnectorMergeAction_DefaultElement0NoneInDD
                                                                                      + ".rar").addAsLibrary(resourceAdapter_jar).addAsLibrary(connector_DefaultElement0NoneInDD_jar).addAsManifestResource(new File(raDir
                                                                                                                                                                                                                     + RarTests.TRA_jca16_ann_ConnectorMergeAction_DefaultElement0NoneInDD
                                                                                                                                                                                                                     + "-ra.xml"),
                                                                                                                                                                                                            "ra.xml");

        ShrinkHelper.exportToServer(server, "connectors", rar1);

        JavaArchive connector_MultiElement0NoneInDD_jar = ShrinkWrap.create(JavaArchive.class, "Connector_MultiElement0NoneInDD.jar");
        connector_MultiElement0NoneInDD_jar.addClass("com.ibm.tra.ann.ConnectorAnn3");
        ResourceAdapterArchive rar2 = ShrinkWrap.create(ResourceAdapterArchive.class, RarTests.TRA_jca16_ann_ConnectorMergeAction_MultiElement0NoneInDD
                                                                                      + ".rar").addAsLibrary(resourceAdapter_jar).addAsLibrary(connector_MultiElement0NoneInDD_jar).addAsManifestResource(new File(raDir
                                                                                                                                                                                                                   + RarTests.TRA_jca16_ann_ConnectorMergeAction_MultiElement0NoneInDD
                                                                                                                                                                                                                   + "-ra.xml"),
                                                                                                                                                                                                          "ra.xml");

        ShrinkHelper.exportToServer(server, "connectors", rar2);

        JavaArchive connector_MultiElementSameInDD_jar = ShrinkWrap.create(JavaArchive.class, "Connector_MultiElementSameInDD.jar");
        connector_MultiElementSameInDD_jar.addClass("com.ibm.tra.ann.ConnectorAnn3");
        ResourceAdapterArchive rar3 = ShrinkWrap.create(ResourceAdapterArchive.class, RarTests.TRA_jca16_ann_ConnectorMergeAction_MultiElementSameInDD
                                                                                      + ".rar").addAsLibrary(resourceAdapter_jar).addAsLibrary(connector_MultiElementSameInDD_jar).addAsManifestResource(new File(raDir
                                                                                                                                                                                                                  + RarTests.TRA_jca16_ann_ConnectorMergeAction_MultiElementSameInDD
                                                                                                                                                                                                                  + "-ra.xml"),
                                                                                                                                                                                                         "ra.xml");

        ShrinkHelper.exportToServer(server, "connectors", rar3);

        JavaArchive connector_SingleElementNoneInDD_jar = ShrinkWrap.create(JavaArchive.class, "Connector_SingleElementNoneInDD.jar");
        connector_SingleElementNoneInDD_jar.addClass("com.ibm.tra.ann.ConnectorAnn2");
        ResourceAdapterArchive rar4 = ShrinkWrap.create(ResourceAdapterArchive.class, RarTests.TRA_jca16_ann_ConnectorMergeAction_SingleElementNoneInDD
                                                                                      + ".rar").addAsLibrary(resourceAdapter_jar).addAsLibrary(connector_SingleElementNoneInDD_jar).addAsManifestResource(new File(raDir
                                                                                                                                                                                                                   + RarTests.TRA_jca16_ann_ConnectorMergeAction_SingleElementNoneInDD
                                                                                                                                                                                                                   + "-ra.xml"),
                                                                                                                                                                                                          "ra.xml");

        ShrinkHelper.exportToServer(server, "connectors", rar4);

        JavaArchive connector_SingleElementSameInDD_jar = ShrinkWrap.create(JavaArchive.class, "Connector_SingleElementSameInDD.jar");
        connector_SingleElementSameInDD_jar.addClass("com.ibm.tra.ann.ConnectorAnn1");
        ResourceAdapterArchive rar5 = ShrinkWrap.create(ResourceAdapterArchive.class, RarTests.TRA_jca16_ann_ConnectorMergeAction_SingleElementSameInDD
                                                                                      + ".rar").addAsLibrary(resourceAdapter_jar).addAsLibrary(connector_SingleElementSameInDD_jar).addAsManifestResource(new File(raDir
                                                                                                                                                                                                                   + RarTests.TRA_jca16_ann_ConnectorMergeAction_SingleElementSameInDD
                                                                                                                                                                                                                   + "-ra.xml"),
                                                                                                                                                                                                          "ra.xml");

        ShrinkHelper.exportToServer(server, "connectors", rar5);

        JavaArchive connector_SingleElementSingleInDD = ShrinkWrap.create(JavaArchive.class, "Connector_SingleElementSingleInDD.jar");
        connector_SingleElementSingleInDD.addClass("com.ibm.tra.ann.ConnectorAnn2");
        ResourceAdapterArchive rar6 = ShrinkWrap.create(ResourceAdapterArchive.class, RarTests.TRA_jca16_ann_ConnectorMergeAction_SingleElementSingleInDD
                                                                                      + ".rar").addAsLibrary(resourceAdapter_jar).addAsLibrary(connector_SingleElementSingleInDD).addAsManifestResource(new File(raDir
                                                                                                                                                                                                                 + RarTests.TRA_jca16_ann_ConnectorMergeAction_SingleElementSingleInDD
                                                                                                                                                                                                                 + "-ra.xml"),
                                                                                                                                                                                                        "ra.xml");

        ShrinkHelper.exportToServer(server, "connectors", rar6);

        server.setServerConfigurationFile("TRA_jca16_ann_ConnectorMergeAction_server.xml");
        originalServerConfig = server.getServerConfiguration().clone();
        server.startServer("ConnectorMergeActionTest.log");
        server.waitForStringInLog("CWWKE0002I");
        assertNotNull("FeatureManager should report update is complete",
                      server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Server should report it has started",
                      server.waitForStringInLog("CWWKF0011I"));

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "tearDown");
        }
        if (server.isStarted()) {
            server.stopServer("J2CA7022W: Resource adapter TRA_jca16_ann_ConnectorMergeAction_MultiElementSameInDD has not installed"); // EXPECTED
        }
    }

    /**
     * After running each test, restore to the original configuration.
     * 
     * @throws Exception
     */
    //@After
    public void cleanUpPerTest() throws Exception {
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(originalServerConfig);
        server.waitForConfigUpdateInLogUsingMark(null, new String[0]);
        Log.info(getClass(), "cleanUpPerTest", "server configuration restored");
    }

    /**
     * Case 1: Validate that if a @Connector is specified with no elements and
     * no connector object exists in the DD that the correct default values are
     * set when the connector object is created in DD.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testDefaultConnNoneInDD() throws Throwable {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testDefaultConnNoneInDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConnectorMergeAction_DefaultElement0NoneInDD;
        String msg = server.waitForStringInLog("J2CA7001I:.*TRA_jca16_ann_ConnectorMergeAction_DefaultElement0NoneInDD.*",
                                               server.getMatchingLogFile("messages.log"));
        assertNotNull("Could not find resource adapter successful install message for TRA_jca16_ann_ConnectorMergeAction_DefaultElement0NoneInDD.rar ", msg);
        String metatype = null;
        metatype = annUtils.getMetatype(server, rarDisplayName);
        assertTrue("A Connector element does not exist with resourceadapter-class " + CONN_DEF_0_CLS, annUtils.getConnector(metatype, rarDisplayName, CONN_DEF_0_CLS));
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testDefaultConnNoneInDD");
        }

        Property = "userName1";
        assertTrue("The Connector userName1 property is not present.",
                   (annUtils.getConfigPropertyFromRA(metatype, rarDisplayName, Property)));

        Property = "password1";
        assertTrue("The Connector password1 property is not present.",
                   (annUtils.getConfigPropertyFromRA(metatype, rarDisplayName, Property)));

        Property = "serverName";
        assertTrue("The Connector serverName property is not present.",
                   (annUtils.getConfigPropertyFromRA(metatype, rarDisplayName, Property)));

    }

    /**
     * Case 2: Validate that if a @Connector is specified and a connector
     * object exists in the DD with a resourceadapter-class specified other
     * than the annotated JavaBean that the @Connector object is ignored.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testSingleConnSingleInDD() throws Throwable {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testSingleConnSingleInDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConnectorMergeAction_SingleElementSingleInDD;
        String metatype = null;
        metatype = annUtils.getMetatype(server, rarDisplayName);
        assertNotNull("A Connector element does not exist with resourceadapter-class " + DD_RA_CLS + ".",
                      annUtils.getConnector(metatype, rarDisplayName, DD_RA_CLS));

        // The DD supports re-authentication. 
        Property = "reauthentication-support";
        Value = "true";
        assertTrue(
                   "The Connector does not support reauthentication.",
                   (annUtils.getConfigPropertyFromRA(metatype, rarDisplayName, Property, Value)));

        // The DD supports local transactions, @Connector does not; ignore @Connector
        Property = "transaction-support";
        Value = "LocalTransaction";
        assertTrue(
                   "The Connector does not support local transactions.",
                   (annUtils.getConfigPropertyFromRA(metatype, rarDisplayName, Property, Value)));

        // The DD lacks a description, the @Connector does not; ignore @Connector
        /// check for description with method
        Property = "description";
        assertEquals(
                     "The Connector does not contains description.", "",
                     annUtils.getAttributeValueFromRA(metatype, rarDisplayName, Property));

        // The DD lacks required work contexts, the @Connector does not; ignore @Connector
        Property = "requiredContextProvider";
        assertFalse(
                    "The Connector does not contain " + 0 + " required-work-contexts (default).",
                    annUtils.getConfigPropertyFromRA(metatype, rarDisplayName, Property));

        // The DD has a version, so does the @Connector; ignore @Connector
        Property = "DD_VERSION";

        assertNotNull("The connector name is not present", annUtils.getAttributeValueFromRA(metatype, rarDisplayName, "name"));
        assertTrue(
                   "The Connector version is not DD_VERSION",
                   annUtils.getAttributeValueFromRA(metatype, rarDisplayName, "name").contains(Property));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testSingleConnSingleInDD");
        }
    }

    /**
     * Case 3: Validate that if a @Connector is specified and a connector object
     * exists in the DD with the resourceadapter-class specified as the same class
     * as the annotated JavaBean, that the @Connector object is processed.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testSingleConnSameInDD() throws Throwable {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testSingleConnSameInDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConnectorMergeAction_SingleElementSameInDD;
        String metatype = null;
        metatype = annUtils.getMetatype(server, rarDisplayName);
        assertNotNull("A connector element exists with resourceadapter-class " + CONN_1_CLS + ".",
                      annUtils.getConnector(metatype, rarDisplayName, CONN_1_CLS));
        String name = annUtils.getAttributeValueFromRA(metatype, rarDisplayName, "name");
        // The DD has a display name, so does the @Connector
        // DD display overrides @Connector
        assertTrue("The connector display name is DD_DISPNAME_1",
                   name.contains("DD_DISPNAME_1"));

        // The DD and @Connector supports reauthentication. 
        Property = "reauthentication-support";
        assertTrue(
                   "The Connector does not support reauthentication.",
                   annUtils.getConfigPropertyFromRA(metatype, rarDisplayName, Property, DD_REAUTHSUPPORT));

        // The DD and @Connector support local transactions.
        Property = "transaction-support";
        Value = "LocalTransaction";
        assertTrue(
                   "The Connector does not support local transactions.",
                   (annUtils.getConfigPropertyFromRA(metatype, rarDisplayName, Property, Value)));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testSingleConnSameInDD");
        }
    }

    final String DD_REQWRKCTX_CLS = "com.ibm.tra.work.TRAWorkContext",
                    CONN_REQWRKCTX_1_CLS = "javax.resource.spi.work.TransactionContext",
                    CONN_REQWRKCTX_2_CLS = "javax.resource.spi.work.SecurityContext";

    /**
     * Case 4: Validate that if the requiredWorkContexts element is specified
     * with a single WorkContext object and no required-work-context objects exist
     * in the DD, that a new xml required-work-context object is created and added
     * to the appropriate place in the DD.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testSingleReqWrkCtxNoneInDD() throws Throwable {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testSingleReqWrkCtxNoneInDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConnectorMergeAction_SingleElementNoneInDD;
        String metatype = null;
        metatype = annUtils.getMetatype(server, rarDisplayName);
        assertTrue("The Connector does not contain required-work-context " + CONN_REQWRKCTX_1_CLS, annUtils.getReqCtx(metatype, rarDisplayName, CONN_REQWRKCTX_1_CLS));
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testSingleReqWrkCtxNoneInDD");
        }
    }

    /**
     * Case 5: Validate that if the requiredWorkContexts element is specified
     * with a single RequiredWorkContext class and a required-work-context element
     * exists in the DD, that a new xml required-work-context object is created and
     * added to the appropriate place in the DD and that existing required-work-
     * context objects are not overwritten.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testSingleReqWrkCtxSameInDD() throws Throwable {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testSingleReqWrkCtxSameInDD");
        }
        // The test case implies the DD resourceadapter-class is the same as the 
        // @Connector class, or that the DD does not declare a resourceadapter-class.
        String rarDisplayName = RarTests.TRA_jca16_ann_ConnectorMergeAction_SingleElementSameInDD;
        String metatype = null;
        metatype = annUtils.getMetatype(server, rarDisplayName);
        assertTrue(
                   "The Connector does not contain required-work-contexts " + CONN_REQWRKCTX_1_CLS,
                   (annUtils.getReqCtx(metatype, rarDisplayName, CONN_REQWRKCTX_1_CLS)));

        assertTrue(
                   "The Connector does not contain required-work-contexts " + CONN_REQWRKCTX_2_CLS,
                   (annUtils.getReqCtx(metatype, rarDisplayName, CONN_REQWRKCTX_2_CLS)));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testSingleReqWrkCtxSameInDD");
        }
    }

    /**
     * Case 6: Validate that if the requiredWorkContexts element is specified with
     * multiple WorkContext objects and no required-work-context objects exist in
     * the DD, that a new xml required-work-context object is created for each
     * element specified in the requiredWorkContexts of the annotation and added
     * to the appropriate place in the DD.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMultiReqWrkCtxNoneInDD() throws Throwable {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testMultiReqWrkCtxNoneInDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConnectorMergeAction_MultiElement0NoneInDD;
        String metatype = null;
        metatype = annUtils.getMetatype(server, rarDisplayName);
        assertTrue(
                   "The Connector does not contain required-work-context " + CONN_REQWRKCTX_1_CLS,
                   (annUtils.getReqCtx(metatype, rarDisplayName, CONN_REQWRKCTX_1_CLS)));

        assertTrue(
                   "The Connector does not contain required-work-context " + CONN_REQWRKCTX_2_CLS,
                   (annUtils.getReqCtx(metatype, rarDisplayName, CONN_REQWRKCTX_2_CLS)));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testMultiReqWrkCtxNoneInDD");
        }
    }

    /**
     * Case 7: Validate that if the requiredWorkContexts element is specified with
     * multiple RequriedWorkContext objects and required-work-context objects exist
     * in the DD, that a new xml required-work-context object is created for each
     * element specified in the requiredWorkContexts of the annotation and added
     * to the appropriate place in the DD and that existing required-work-context
     * objects are not overwritten.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testMultiReqWrkCtxSameInDD() throws Throwable {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testMultiReqWrkCtxSameInDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConnectorMergeAction_MultiElementSameInDD;
        String metatype = null;
        metatype = annUtils.getMetatype(server, rarDisplayName);
        assertTrue(
                   "The Connector does not contain required-work-context " + DD_REQWRKCTX_CLS,
                   (annUtils.getReqCtx(metatype, rarDisplayName, DD_REQWRKCTX_CLS)));

        assertTrue(
                   "The Connector does not contain required-work-context " + CONN_REQWRKCTX_1_CLS,
                   (annUtils.getReqCtx(metatype, rarDisplayName, CONN_REQWRKCTX_1_CLS)));

        assertTrue(
                   "The Connector does not contain required-work-context " + CONN_REQWRKCTX_2_CLS,
                   (annUtils.getReqCtx(metatype, rarDisplayName, CONN_REQWRKCTX_2_CLS)));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testMultiReqWrkCtxSameInDD");
        }
    }

    String DD_TXSUPPORT = "LocalTransaction";

    String CONN_TXSUPPORT = "XATransaction";

    /**
     * Case 31: Validate that if the transactionSupport element is specified
     * and a transactionSupport object exists in the DD, that the contents
     * of the DD are not overwritten in the existing trasactionSupport emf object.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testTxSupportSameInDD() throws Throwable {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testTxSupportSameInDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConnectorMergeAction_SingleElementSameInDD;
        String metatype = null;
        metatype = annUtils.getMetatype(server, rarDisplayName);
        Property = "transaction-support";
        String Value = DD_TXSUPPORT;
        assertTrue("The Connector does not support LOCAL transactions.",
                   annUtils.getConfigPropertyFromRA(metatype, rarDisplayName, Property, Value));
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testTxSupportSameInDD");
        }
    }

    /**
     * Case 32: Validate that if the transactionSupport element is specified and
     * no transactionSupport object exists in the DD, that the contents of the
     * DD are updated correctly in the existing transactionSupport emf object.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testTxSupportNoneInDD() throws Throwable {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testTxSupportNoneInDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConnectorMergeAction_SingleElementNoneInDD;
        String metatype = null;
        metatype = annUtils.getMetatype(server, rarDisplayName);
        Property = "transaction-support";
        String Value = CONN_TXSUPPORT;
        assertTrue("The Connector does not support XA transactions.",
                   annUtils.getConfigPropertyFromRA(metatype, rarDisplayName, Property, Value));
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testTxSupportNoneInDD");
        }
    }

    /**
     * Case 33: Validate that if a transactionSupport element is specified and
     * no OutboundResourceAdapter object exists in the DD, that an OutboundResourceAdapter
     * object is created, and attached properly to the existing ResourceAdapter
     * emf object.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testTxSupportNoOraInDD() throws Throwable {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testTxSupportNoOraInDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConnectorMergeAction_MultiElement0NoneInDD;
        String metatype = null;
        metatype = annUtils.getMetatype(server, rarDisplayName);
        Property = "transaction-support";
        String Value = CONN_TXSUPPORT;
        assertTrue("The Connector does not support XA transactions.",
                   annUtils.getConfigPropertyFromRA(metatype, rarDisplayName, Property, Value));
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testTxSupportNoOraInDD");
        }
    }

    /**
     * Case 34: Validate that if the reauthenticationSupport element is specified
     * and a reauthenticationSupport object exists in the DD, that the contents of
     * the DD are not overwritten in the existing OutboundResourceAdapter emf object.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testReauthSupportSameInDD() throws Throwable {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testReauthSupportSameInDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConnectorMergeAction_SingleElementSameInDD;
        String metatype = null;
        metatype = annUtils.getMetatype(server, rarDisplayName);
        String expectedReauthSupport = DD_REAUTHSUPPORT;
        Property = "reauthentication-support";
        assertTrue("The Connector does not support reauthentication.",
                   annUtils.getConfigPropertyFromRA(metatype, rarDisplayName, Property, expectedReauthSupport));
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testReauthSupportSameInDD");
        }
    }

    /**
     * Case 35: Validate that if the reauthenticationSupport element is specified
     * and no reauthenticationSupport object exists in the DD, that the contents
     * of the DD are updated correctly in the existing OutboundResourceAdapter
     * emf object.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testReauthSupportNoneInDD() throws Throwable {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testReauthSupportNoneInDD");
        }
        String rarDisplayName = RarTests.TRA_jca16_ann_ConnectorMergeAction_SingleElementNoneInDD;
        String metatype = null;
        metatype = annUtils.getMetatype(server, rarDisplayName);
        String expectedReauthSupport = CONN_REAUTHSUPPORT;
        Property = "reauthentication-support";
        assertTrue("The Connector *does not* support reauthentication.",
                   annUtils.getConfigPropertyFromRA(metatype, rarDisplayName, Property, expectedReauthSupport));
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testReauthSupportNoneInDD");
        }
    }

}
