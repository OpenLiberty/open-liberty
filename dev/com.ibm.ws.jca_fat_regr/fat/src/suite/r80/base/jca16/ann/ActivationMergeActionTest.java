/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import junit.framework.AssertionFailedError;
import suite.r80.base.jca16.RarTests;
import suite.r80.base.jca16.TestSetupUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class ActivationMergeActionTest {

    private final static String CLASSNAME = ActivationMergeActionTest.class.getName();
    private final static Logger logger = Logger.getLogger(CLASSNAME);
    protected static LibertyServer server;
    // private final static Class<?> c = ActivationMergeActionTest.class;
    public AnnUtils annUtils = new AnnUtils();

    String activationSpec = null; // An <activation-spec> of a
                                  // <message-listener>
    String[] messageListenerTypes = null; // A <messagelistener-type> of a
                                          // <message-listener>
    String rarDisplayName = null;
    String metatype = null;
    int nExpectedMsgLstnrs = 0;

    final String AS_CLS_1 = "com.ibm.tra.ann.ActivationAnn1",
                    AS_CLS_2 = "com.ibm.tra.ann.ActivationAnn2",
                    AS_CLS_3 = "com.ibm.tra.ann.ActivationAnn3",
                    AS_CLS_4 = "com.ibm.tra.ann.ActivationAnn4",
                    AS_CLS_5 = "com.ibm.tra.ann.ActivationAnn5",
                    AS_CLS_6a = "com.ibm.tra.ann.ActivationAnn6a",
                    AS_CLS_6b = "com.ibm.tra.ann.ActivationAnn6b",
                    AS_CLS_7 = "com.ibm.tra.ann.ActivationAnn7",

                    ML_TYPE_DEF = "javax.jms.MessageListener",
                    ML_TYPE_1 = "com.ibm.tra.inbound.impl.TRAMessageListener1",
                    ML_TYPE_2 = "com.ibm.tra.inbound.impl.TRAMessageListener2",
                    ML_TYPE_3 = "com.ibm.tra.inbound.impl.TRAMessageListener3",
                    ML_TYPE_4 = "com.ibm.tra.inbound.impl.TRAMessageListener4",
                    ML_TYPE_5 = "com.ibm.tra.inbound.impl.TRAMessageListener5",
                    ML_TYPE_6 = "com.ibm.tra.inbound.impl.TRAMessageListener6",
                    ML_TYPE_7 = "com.ibm.tra.inbound.impl.TRAMessageListener7",
                    ML_TYPE_8 = "com.ibm.tra.inbound.impl.TRAMessageListener8";

    @BeforeClass
    public static void setUp() throws Exception {

        if (logger.isLoggable(Level.FINER)) {
            logger.entering(CLASSNAME, "setUp");
        }
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jca.fat.regr", null, false);

        server.setServerConfigurationFile("ActivationMergeActionTest_server.xml");

//      Package TRA_jca16_ann_ActivationMergeAction_ML TRA_jca16_ann_ActivationMergeAction_ML.rar
        JavaArchive resourceAdapterEjs_jar = TestSetupUtils.getTraAnnEjsResourceAdapter_jar();

        JavaArchive activation_ML_jar = ShrinkWrap.create(JavaArchive.class, "Activation_ML.jar");
        Filter<ArchivePath> packageFilter2 = new Filter<ArchivePath>() {
            @Override
            public boolean include(final ArchivePath object) {
                final String currentPath = object.get();

                Pattern pattern = Pattern.compile("(com/ibm/tra/ann/ActivationAnn..class)|(com/ibm/tra/ann/ActivationAnn...class)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(currentPath);
                boolean included = matcher.find();

                //System.out.println("Activation_ML.jar included: " + included + " packageFilter object name: " + currentPath);
                return included;

            }
        };
        activation_ML_jar.addPackages(true, packageFilter2, "com.ibm.tra.ann");

        ResourceAdapterArchive TRA_jca16_ann_ActivationMergeAction_ML = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                          RarTests.TRA_jca16_ann_ActivationMergeAction_ML
                                                                                                                        + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(activation_ML_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                      + RarTests.TRA_jca16_ann_ActivationMergeAction_ML
                                                                                                                                                                                                                                      + "-ra.xml"),
                                                                                                                                                                                                                             "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ActivationMergeAction_ML);

//      Package TRA_jca16_ann_ActivationMergeAction_NoML TRA_jca16_ann_ActivationMergeAction_NoML.rar
        JavaArchive activation_NoMLjar = ShrinkWrap.create(JavaArchive.class, "Activation_NoML.jar");
        Filter<ArchivePath> packageFilter3 = new Filter<ArchivePath>() {
            @Override
            public boolean include(final ArchivePath object) {
                final String currentPath = object.get();

                Pattern pattern = Pattern.compile("(com/ibm/tra/ann/ActivationAnn5.class)|(com/ibm/tra/ann/ActivationAnn2.class)|(com/ibm/tra/ann/ActivationAnn...class)",
                                                  Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(currentPath);
                boolean included = matcher.find();

                //System.out.println("Activation_NoML.jar included: " + included + " packageFilter object name: " + currentPath);
                return included;

            }
        };
        activation_NoMLjar.addPackages(true, packageFilter3, "com.ibm.tra.ann");

        ResourceAdapterArchive TRA_jca16_ann_ActivationMergeAction_NoML = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                            RarTests.TRA_jca16_ann_ActivationMergeAction_NoML
                                                                                                                          + ".rar").addAsLibrary(resourceAdapterEjs_jar).addAsLibrary(activation_NoMLjar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                         + RarTests.TRA_jca16_ann_ActivationMergeAction_NoML
                                                                                                                                                                                                                                         + "-ra.xml"),
                                                                                                                                                                                                                                "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ActivationMergeAction_NoML);

        server.startServer("ActivationMergeActionTest.log");
        server.waitForStringInLogUsingMark("CWWKE0002I");
        assertNotNull("FeatureManager should report update is complete",
                      server.waitForStringInLogUsingMark("CWWKF0008I"));
        assertNotNull("Server should report it has started",
                      server.waitForStringInLogUsingMark("CWWKF0011I"));

        String msg = server.waitForStringInLog("J2CA7001I:.*TRA_jca16_ann_ActivationMergeAction_ML.*",
                                               server.getMatchingLogFile("messages.log"));
        assertNotNull("Could not find resource adapter successful install message for TRA_jca16_ann_ActivationMergeAction_ML.rar ",
                      msg);

        msg = server.waitForStringInLog("J2CA7001I:.*TRA_jca16_ann_ActivationMergeAction_NoML.*",
                                        server.getMatchingLogFile("messages.log"));
        assertNotNull("Could not find resource adapter successful install message for TRA_jca16_ann_ActivationMergeAction_NoML.rar ",
                      msg);

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (logger.isLoggable(Level.FINER)) {
            logger.entering(CLASSNAME, "tearDown");
        }

        if (server.isStarted()) {
            server.stopServer("CWWKE0701E", // EXPECTED
                              "CWWKE0700W");// EXPECTED
        }
    }

    /**
     * Case 1: Validate that if @Activation is specified with a single class in
     * the messageListeners element, and that no messagelistener exists in the
     * DD, that a single xml messagelistener is created with the specified
     * messagelistener-type and the correct activationspec-class and added to
     * the correct location in the DD.
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */

    @Test
    public void testSingleMessageListenerNoMLInDD() throws Exception {

        System.out.println("*********** enter : testSingleMessageListenerNoMLInDD");

        rarDisplayName = RarTests.TRA_jca16_ann_ActivationMergeAction_NoML;
        activationSpec = AS_CLS_2;
        messageListenerTypes = new String[] { ML_TYPE_2 };
        nExpectedMsgLstnrs = 1;

        metatype = annUtils.getMetatype(server, rarDisplayName);

        assertTrue("No ML exists containing ML-type=" + messageListenerTypes[0]
                   + " and AS=" + activationSpec + " does not exist",
                   annUtils.getMessageListener(metatype, rarDisplayName,
                                               messageListenerTypes[0], activationSpec));

        nExpectedMsgLstnrs = 1;
        assertEquals("Incorrect number of MessageListeners contain ML-type="
                     + messageListenerTypes[0] + " and AS=" + activationSpec,
                     nExpectedMsgLstnrs,
                     annUtils.getMessageListeners(metatype, activationSpec));

        System.out.println("*********** end : testSingleMessageListenerNoMLInDD");
    }

    /**
     * Case 2: Validate that if @Activation is specified with a multiple classes
     * in the messageListeners element, and that no messagelistener exists in
     * the DD, that multiple xml messagelisteners are created with the specified
     * messagelistener-type and the correct activationspec-class and added to
     * the correct location in the DD.
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */

    @Test
    public void testMultiMessageListenerNoMLInDD() throws Exception {

        System.out.println("*********** enter : testMultiMessageListenerNoMLInDD");

        rarDisplayName = RarTests.TRA_jca16_ann_ActivationMergeAction_NoML;

        activationSpec = AS_CLS_5;
        messageListenerTypes = new String[] { ML_TYPE_4, ML_TYPE_5 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        assertTrue("No ML exists containing ML-type=" + messageListenerTypes[0]
                   + " and AS=" + activationSpec,
                   annUtils.getMessageListener(
                                               metatype, rarDisplayName, messageListenerTypes[0],
                                               activationSpec));

        assertTrue("No ML exists containing ML-type=" + messageListenerTypes[1]
                   + " and AS=" + activationSpec,
                   annUtils.getMessageListener(
                                               metatype, rarDisplayName, messageListenerTypes[1],
                                               activationSpec));

        nExpectedMsgLstnrs = 2;
        assertEquals("Incorrect number of listeners contain ML-type="
                     + messageListenerTypes[0] + " and AS=" + activationSpec,
                     nExpectedMsgLstnrs,
                     annUtils.getMessageListeners(metatype, activationSpec));

        System.out.println("*********** exit : testMultiMessageListenerNoMLInDD");

    }

    /**
     * Case 3:. Validate that if @Activation is specified within multiple
     * different classes, each with a single class in the messageListeners
     * element, and that no messagelistener exists in the DD, that multiple xml
     * messagelisteners are created with the specified messagelistener-type and
     * the correct activationspec-class and added to the correct location in the
     * DD.
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */

    @Test
    public void testMulti2MessageListenerNoMLInDD() throws Exception {

        System.out.println("*********** enter : testMulti2MessageListenerNoMLInDD");

        rarDisplayName = RarTests.TRA_jca16_ann_ActivationMergeAction_NoML;

        activationSpec = AS_CLS_6a;
        messageListenerTypes = new String[] { ML_TYPE_6 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        assertTrue("No ML exists containing ML-type=" + messageListenerTypes[0]
                   + " and AS=" + activationSpec,
                   annUtils.getMessageListener(
                                               metatype, rarDisplayName, messageListenerTypes[0],
                                               activationSpec));

        nExpectedMsgLstnrs = 1;
        assertEquals("Incorrect number of message listeners contain ML-type="
                     + messageListenerTypes[0] + " and AS=" + activationSpec,
                     nExpectedMsgLstnrs,
                     annUtils.getMessageListeners(metatype, activationSpec));

        activationSpec = AS_CLS_6b;
        messageListenerTypes = new String[] { ML_TYPE_7 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        assertTrue("No ML exists containing ML-type=" + messageListenerTypes[0]
                   + " and AS=" + activationSpec,
                   annUtils.getMessageListener(
                                               metatype, rarDisplayName, messageListenerTypes[0],
                                               activationSpec));

        nExpectedMsgLstnrs = 1;
        assertEquals("Incorrect number of message listeners contain ML-type="
                     + messageListenerTypes[0] + " and AS=" + activationSpec,
                     nExpectedMsgLstnrs,
                     annUtils.getMessageListeners(metatype, activationSpec));

        System.out.println("*********** exit : testMulti2MessageListenerNoMLInDD");
    }

    /**
     * Case 4. Validate that if @Activation is specified with a single class in
     * the messageListeners element, and that a messagelistener with the same
     * messagelistener-type exists in the DD, that the annotation is ignored and
     * nothing added to the DD.
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */

    @Test
    public void testSingleMessageListenerMLInDD() throws Exception {

        System.out.println("*********** enter : testSingleMessageListenerMLInDD");

        rarDisplayName = RarTests.TRA_jca16_ann_ActivationMergeAction_ML;

        activationSpec = AS_CLS_3;
        messageListenerTypes = new String[] { ML_TYPE_1 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        assertFalse("A messagelistener exists with ML-type="
                    + messageListenerTypes[0] + " and AS=" + activationSpec,
                    annUtils.getMessageListener(metatype, rarDisplayName,
                                                messageListenerTypes[0], activationSpec));

        nExpectedMsgLstnrs = 0;
        assertEquals("Incorrect number of Messagelisteners contain ML-type="
                     + messageListenerTypes[0] + " and AS=" + activationSpec,
                     nExpectedMsgLstnrs,
                     annUtils.getMessageListeners(metatype, activationSpec));

        System.out.println("*********** end : testSingleMessageListenerMLInDD");

    }

    /**
     * Case 5: Validate that if @Activation is specified with a single class in
     * the messageListeners element, and that a messagelistener with a different
     * messagelistener-type exists in the DD, that a single xml messagelistener
     * is created with the specified messagelistener-type and the correct
     * activationspec-class and added to the correct location in the DD.
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */

    @Test
    public void testSingle2MessageListenerMLInDD() throws Exception {

        System.out.println("*********** enter : testSingle2MessageListenerMLInDD");

        rarDisplayName = RarTests.TRA_jca16_ann_ActivationMergeAction_ML;
        activationSpec = AS_CLS_4;
        messageListenerTypes = new String[] { ML_TYPE_3 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        assertTrue("No ML exists containing ML-type=" + messageListenerTypes[0]
                   + " and AS=" + activationSpec,
                   annUtils.getMessageListener(
                                               metatype, rarDisplayName, messageListenerTypes[0],
                                               activationSpec));

        nExpectedMsgLstnrs = 1;
        assertEquals("Incorrect number of message listeners contain ML-type="
                     + messageListenerTypes[0] + " and AS=" + activationSpec,
                     nExpectedMsgLstnrs,
                     annUtils.getMessageListeners(metatype, activationSpec));

        System.out.println("*********** exit : testSingle2MessageListenerMLInDD");

    }

    /**
     * Case 6: Validate that if @Activation is specified with a multiple classes
     * in the messageListeners element, and that a messagelistener with the same
     * messagelistener-type as one of the annotation elements exists in the DD
     * that multiple xml messagelisteners are created with the specified
     * messagelistener-type and the correct activationspec-class and added to
     * the correct location in the DD and that the annotation messagelistener
     * which matches the DD is ignored and not added to the DD.
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */

    @Test
    public void testMultiMessageListenerMLInDD() throws Exception {

        System.out.println("*********** enter : testMultiMessageListenerMLInDD");

        rarDisplayName = RarTests.TRA_jca16_ann_ActivationMergeAction_ML;

        activationSpec = AS_CLS_7;
        messageListenerTypes = new String[] { ML_TYPE_4, ML_TYPE_8 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        assertFalse("A ML exists containing ML-type=" + messageListenerTypes[0]
                    + " and AS=" + activationSpec,
                    annUtils.getMessageListener(
                                                metatype, rarDisplayName, messageListenerTypes[0],
                                                activationSpec));

        assertTrue("No ML exists containing ML-type=" + messageListenerTypes[1]
                   + " and AS=" + activationSpec,
                   annUtils.getMessageListener(
                                               metatype, rarDisplayName, messageListenerTypes[1],
                                               activationSpec));

        nExpectedMsgLstnrs = 1;
        assertEquals("Incorrect number of message listeners contain ML-type="
                     + messageListenerTypes[1] + " and AS=" + activationSpec,
                     nExpectedMsgLstnrs,
                     annUtils.getMessageListeners(metatype, activationSpec));

        System.out.println("*********** exit : testMultiMessageListenerMLInDD");
    }

}
