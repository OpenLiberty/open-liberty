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
public class AdministeredObjectMergeActionTest {

    private final static String CLASSNAME = AdministeredObjectMergeActionTest.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASSNAME);
    protected static LibertyServer server;

    String rarDisplayName = null;
    public AnnUtils annUtils = new AnnUtils();
    String metatype = null;

    public AdministeredObjectMergeActionTest() {
    }

    String adminObjectClass = null; // An <adminObject>
    String[] adminObjectTypes = null; // The types of adminObjects that are
                                      // supported

    final String AO_INTF_0 = "com.ibm.tra.inbound.base.TRAAdminObject",
                    AO_INTF_1 = "com.ibm.tra.inbound.base.TRAAdminObject1",
                    AO_INTF_2 = "com.ibm.tra.inbound.base.TRAAdminObject2",

                    AO_CLS_0 = "com.ibm.tra.ann.AdminObjectAnn0",
                    AO_CLS_1 = "com.ibm.tra.ann.AdminObjectAnn1",
                    AO_CLS_2 = "com.ibm.tra.ann.AdminObjectAnn2",
                    AO_CLS_3 = "com.ibm.tra.ann.AdminObjectAnn3",
                    AO_CLS_4 = "com.ibm.tra.ann.AdminObjectAnn4",
                    AO_CLS_5 = "com.ibm.tra.ann.AdminObjectAnn5",
                    AO_CLS_6 = "com.ibm.tra.ann.AdminObjectAnn6",
                    AO_CLS_7 = "com.ibm.tra.ann.AdminObjectAnn7",
                    AO_CLS_8 = "com.ibm.tra.ann.AdminObjectAnn8",
                    AO_CLS_9 = "com.ibm.tra.ann.AdminObjectAnn9",
                    AO_CLS_10 = "com.ibm.tra.ann.AdminObjectAnn10",
                    AO_CLS_11 = "com.ibm.tra.ann.AdminObjectAnn11",
                    AO_CLS_12 = "com.ibm.tra.ann.AdminObjectAnn12";

    @BeforeClass
    public static void setUp() throws Exception {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "setUp");
        }
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jca.fat.regr", null, false);

        server.setServerConfigurationFile("TRA_jca16_ann_AdministeredObjectMergeAction_server.xml");

//      Package TRA_jca16_ann_AdministeredObjectMergeAction_NoAOT TRA_jca16_ann_AdministeredObjectMergeAction_NoAOT.rar
        JavaArchive traAnnEjsResourceAdapter_jar = TestSetupUtils.getTraAnnEjsResourceAdapter_jar();

        JavaArchive administeredObject_NoAOT_jar = ShrinkWrap.create(JavaArchive.class, "AdministeredObject_NoAOT.jar");
        Filter<ArchivePath> packageFilter = new Filter<ArchivePath>() {
            @Override
            public boolean include(final ArchivePath object) {
                final String currentPath = object.get();

                Pattern pattern = Pattern.compile("(com/ibm/tra/ann/AdminObjectAnn[01234589].class)",
                                                  Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(currentPath);
                boolean included = matcher.find();

                //System.out.println("AdministeredObject_NoAOT.jar included: " + included + " packageFilter object name: " + currentPath);
                return included;

            }
        };
        administeredObject_NoAOT_jar.addPackages(true, packageFilter, "com.ibm.tra.ann");

        ResourceAdapterArchive TRA_jca16_ann_AdministeredObjectMergeAction_NoAOT = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                     RarTests.TRA_jca16_ann_AdministeredObjectMergeAction_NoAOT
                                                                                                                                   + ".rar").addAsLibrary(traAnnEjsResourceAdapter_jar).addAsLibrary(administeredObject_NoAOT_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                  + RarTests.TRA_jca16_ann_AdministeredObjectMergeAction_NoAOT
                                                                                                                                                                                                                                                                  + "-ra.xml"),
                                                                                                                                                                                                                                                         "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_AdministeredObjectMergeAction_NoAOT);

//      Package TRA_jca16_ann_AdministeredObjectMergeAction_SingleElementSingleAOT TRA_jca16_ann_AdministeredObjectMergeAction_SingleElementSingleAOT.rar
        JavaArchive administeredObject_SingleElement = ShrinkWrap.create(JavaArchive.class, "AdministeredObject_SingleElement.jar");
        Filter<ArchivePath> packageFilter2 = new Filter<ArchivePath>() {
            @Override
            public boolean include(final ArchivePath object) {
                final String currentPath = object.get();

                Pattern pattern = Pattern.compile("(com/ibm/tra/ann/AdminObjectAnn[023456789].class)", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(currentPath);
                boolean included = matcher.find();

                //System.out.println("AdministeredObject_SingleElement.jar included: " + included + " packageFilter object name: " + currentPath);
                return included;

            }
        };
        administeredObject_SingleElement.addPackages(true, packageFilter2, "com.ibm.tra.ann");

        ResourceAdapterArchive TRA_jca16_ann_AdministeredObjectMergeAction_SingleElementSingleAOT = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                      RarTests.TRA_jca16_ann_AdministeredObjectMergeAction_SingleElementSingleAOT
                                                                                                                                                    + ".rar").addAsLibrary(traAnnEjsResourceAdapter_jar).addAsLibrary(administeredObject_SingleElement).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                       + RarTests.TRA_jca16_ann_AdministeredObjectMergeAction_SingleElementSingleAOT
                                                                                                                                                                                                                                                                                       + "-ra.xml"),
                                                                                                                                                                                                                                                                              "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_AdministeredObjectMergeAction_SingleElementSingleAOT);

        server.startServer("AdministeredObjectMergeActionTest.log");
        server.waitForStringInLogUsingMark("CWWKE0002I");
        assertNotNull("FeatureManager should report update is complete",
                      server.waitForStringInLogUsingMark("CWWKF0008I"));
        assertNotNull("Server should report it has started",
                      server.waitForStringInLogUsingMark("CWWKF0011I"));

        String msg = server.waitForStringInLog("J2CA7001I:.*TRA_jca16_ann_AdministeredObjectMergeAction_NoAOT.*",
                                               server.getMatchingLogFile("messages.log"));
        assertNotNull("Could not find resource adapter successful install message for TRA_jca16_ann_AdministeredObjectMergeAction_NoAOT.rar ",
                      msg);

        msg = server.waitForStringInLog("J2CA7001I:.*TRA_jca16_ann_AdministeredObjectMergeAction_SingleElementSingleAOT.*",
                                        server.getMatchingLogFile("messages.log"));
        assertNotNull("Could not find resource adapter successful install message for TRA_jca16_ann_AdministeredObjectMergeAction_SingleElementSingleAOT.rar ",
                      msg);

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "tearDown");
        }

        if (server.isStarted()) {
            server.stopServer("CWWKE0701E", // EXPECTED
                              "CWWKE0700W");// EXPECTED
        }
    }

    /**
     * Case 1: Validate that if @AdministeredObject is specified with no
     * elements that the correct default values are set when the adminobject
     * element is created in the DD.
     * 
     * In this scenario we use the com.ibm.tra.ann.AdminObjectAnn1 class which
     * has an @AdministeredObject annotation with no elements and implements the
     * com.ibm.tra.inbound.base.TRAAdminObject interface. This should result in
     * a single EMF adminobject with:
     * 
     * AdminObjectClass = com.ibm.tra.ann.AdminObjectAnn1 AdminObjectInterface =
     * com.ibm.tra.inbound.base.TRAAdminObject
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */
    @Test
    public void testNoElementsNoAOTInDD() throws Exception {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testNoElementsNoAOTInDD");
        }

        rarDisplayName = RarTests.TRA_jca16_ann_AdministeredObjectMergeAction_NoAOT;

        adminObjectClass = AO_CLS_1;
        adminObjectTypes = new String[] { AO_INTF_0 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        assertTrue(
                   "An AdminObject with the default adminobject-interface="
                   + adminObjectTypes[0] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(
                                                  metatype, rarDisplayName, adminObjectClass,
                                                  adminObjectTypes[0]));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testNoElementsNoAOTInDD");
        }
    }

    /**
     * Case 2: Validate that if @AdministeredObject is specified with a single
     * class in the adminObjectInterfaces[], and no matching element exists in
     * the DD, that a single emf adminObject is created correctly (correct class
     * name and correct interface name) and added to the correct location in the
     * DD.
     * 
     * Here we use the class com.ibm.tra.ann.AdminObjectAnn2 which has the
     * following annotation @AdministeredObject(adminObjectInterfaces =
     * {TRAAdminObject.class}) packaged in a RAR which does not have a
     * corresponding <adminobject> tag in the DD and this should result in a
     * single EMF admin object with:
     * 
     * AdminObjectClass = com.ibm.tra.ann.AdminObjectAnn2 AdminObjectInterface =
     * com.ibm.tra.inbound.base.TRAAdminObject
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */
    @Test
    public void testSingleElementNoAOTInDD() throws Exception {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testSingleElementNoAOTInDD");
        }

        rarDisplayName = RarTests.TRA_jca16_ann_AdministeredObjectMergeAction_NoAOT;

        adminObjectClass = AO_CLS_2;
        adminObjectTypes = new String[] { AO_INTF_0 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        // Only one AO is present in the RA descriptor.
        assertEquals("Incorrect number of AdminObject is present in the merged DD.", 1,
                     annUtils.getAdministeredObjects(metatype, adminObjectClass));

        // The AO is the one we expect.
        assertTrue("An AdminObject with the adminobject-interface="
                   + adminObjectTypes[0] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(metatype,
                                                  rarDisplayName, adminObjectClass, adminObjectTypes[0]));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testSingleElementNoAOTInDD");
        }
    }

    /**
     * Case 3: Validate that if @AdministeredObject is specified with mulitple
     * classes in the adminObjectInterfaces[], and no matching elements exist in
     * the DD, that multiple adminObjects are created correctly (correct class
     * name and correct interface name) and added to the correct location in the
     * DD.
     * 
     * In this test case we create an RA that contains the
     * com.ibm.tra.ann.AdminObjectAnn4 class which contains the following
     * Annotation and no <adminobject> element in the DD
     * 
     * @AdministeredObject(adminObjectInterfaces =
     *                                           {TRAAdminObject.class,TRAAdminObject1
     *                                           .class})
     * 
     *                                           This should result in 2 EMF
     *                                           admin objects with:
     * 
     *                                           AdminObjectClass =
     *                                           com.ibm.tra.ann.AdminObjectAnn4
     *                                           AdminObjectInterface =
     *                                           com.ibm.tra
     *                                           .inbound.base.TRAAdminObject,
     *                                           com.ibm.tra.inbound.base.
     *                                           TRAAdminObject1
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */
    @Test
    public void testMultipleElementNoAOTInDD() throws Exception {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testMultipleElementNoAOTInDD");
        }

        rarDisplayName = RarTests.TRA_jca16_ann_AdministeredObjectMergeAction_NoAOT;

        adminObjectClass = AO_CLS_4;
        adminObjectTypes = new String[] { AO_INTF_0, AO_INTF_1 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        assertEquals("Incorrect number of AdminObject are present in the merged DD", 2,
                     annUtils.getAdministeredObjects(metatype, adminObjectClass));

        // The first AO is the one we expect.
        assertTrue("The AdminObject with adminobject-interface="
                   + adminObjectTypes[0] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(metatype,
                                                  rarDisplayName, adminObjectClass, adminObjectTypes[0]));

        // The second AO is the one we expect.
        assertTrue(
                   "The second AdminObject with the adminobject-interface="
                   + adminObjectTypes[1] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(
                                                  metatype, rarDisplayName, adminObjectClass,
                                                  adminObjectTypes[1]));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testMultipleElementNoAOTInDD");
        }
    }

    /**
     * Case 4: Validate that if @AdministeredObject is specified with no classes
     * in the adminObjectInterfaces[], and the annotated class implements a
     * single interface and no matching element exists in the DD, that a single
     * EMF adminObject is created correctly (correct class name and correct
     * interface name) and added to the correct location in the DD.
     * 
     * In this case we use an RA which contains the
     * com.ibm.tra.ann.AdminObjectAnn5 class. This class has an empty annotation
     * 
     * @AdministeredObject(adminObjectInterfaces = {}) and implements a single
     *                                           interface
     *                                           com.ibm.tra.inbound.base
     *                                           .TRAAdminObject. The RA does
     *                                           not have a matching element in
     *                                           the DD. On deployment this
     *                                           should result in a single EMF
     *                                           AdminObject with:
     * 
     *                                           AdminObjectClass =
     *                                           com.ibm.tra.ann.AdminObjectAnn5
     *                                           AdminObjectInterface =
     *                                           com.ibm.tra
     *                                           .inbound.base.TRAAdminObject
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */
    @Test
    public void testNoElementNoAOTInDD() throws Exception {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testNoElementNoAOTInDD");
        }

        rarDisplayName = RarTests.TRA_jca16_ann_AdministeredObjectMergeAction_NoAOT;

        adminObjectClass = AO_CLS_5;
        adminObjectTypes = new String[] { AO_INTF_0 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        assertEquals("Incorrect number of AdminObject are present in the merged DD", 1,
                     annUtils.getAdministeredObjects(metatype, adminObjectClass));

        assertTrue("An AdminObject with the adminobject-interface="
                   + adminObjectTypes[0] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(metatype,
                                                  rarDisplayName, adminObjectClass, adminObjectTypes[0]));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testNoElementNoAOTInDD");
        }
    }

    /**
     * Case 5: Validate that if @AdministeredObject is specified without any
     * interfaces and the class implements 3 interfaces two of which are
     * java.io.Serializable and java.io.Externalizable and no matching element
     * exists in the DD that a single emf adminObject is created correctly
     * (correct class name and correct interface name) and added to the correct
     * location in the DD
     * 
     * In this case we use an RA which contains the
     * com.ibm.tra.ann.AdminObjectAnn3 class. This class has an empty annotation @AdministeredObject
     * and implements a single interface com.ibm.tra.inbound.base.TRAAdminObject
     * in addition to the java.io.Externalizable and java.io.Serializable
     * interfaces . The RA does not have a matching element in the DD. On
     * deployment this should result in a single EMF AdminObject with:
     * 
     * AdminObjectClass = com.ibm.tra.ann.AdminObjectAnn3 AdminObjectInterface =
     * com.ibm.tra.inbound.base.TRAAdminObject
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */
    @Test
    public void testNoElementMultipleInterfacesNoMatchingAOTInDD() throws Exception {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testSingleElementNoAOTInDD");
        }

        rarDisplayName = RarTests.TRA_jca16_ann_AdministeredObjectMergeAction_NoAOT;

        adminObjectClass = AO_CLS_3;
        adminObjectTypes = new String[] { AO_INTF_0 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        assertEquals("Incorrect number of AdminObject are present in the merged DD", 1,
                     annUtils.getAdministeredObjects(metatype, adminObjectClass));

        assertTrue("An AdminObject with the adminobject-interface="
                   + adminObjectTypes[0] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(metatype,
                                                  rarDisplayName, adminObjectClass, adminObjectTypes[0]));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testSingleElementNoAOTInDD");
        }
    }

    /**
     * Case 6: Validate that if @AdministeredObject is specified with any 3
     * interfaces and if 2 of those interfaces are implemented by the superclass
     * and no matching element exists in the DD that 3 emf adminObject is
     * created correctly (correct class name and correct interface name) and
     * added to the correct location in the DD
     * 
     * In this case we use an RA which contains the
     * com.ibm.tra.ann.AdminObjectAnn0 class. This class has an @AdministeredObject
     * annotation with 3 elements and it implements the interface
     * com.ibm.tra.inbound.base.TRAAdminObject2 and extends the object
     * com.ibm.tra.ann.AdminObjectAnn4 and there is no matching element in the
     * DD for the class.
     * 
     * On deployment this should result in an EMF AdminObject for each of the
     * interfaces as shown:
     * 
     * AdminObjectClass = com.ibm.tra.ann.AdminObjectAnn0 AdminObjectInterface =
     * com.ibm.tra.inbound.base.TRAAdminObject
     * 
     * AdminObjectClass = com.ibm.tra.ann.AdminObjectAnn0 AdminObjectInterface =
     * com.ibm.tra.inbound.base.TRAAdminObject1
     * 
     * AdminObjectClass = com.ibm.tra.ann.AdminObjectAnn0 AdminObjectInterface =
     * com.ibm.tra.inbound.base.TRAAdminObject2
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */
    @Test
    public void testNoElementMultipleInterfaceSuperClassInDD() throws Exception {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME,
                            "testNoElementMultipleInterfaceSuperClassInDD");
        }

        rarDisplayName = RarTests.TRA_jca16_ann_AdministeredObjectMergeAction_NoAOT;

        adminObjectClass = AO_CLS_0;
        adminObjectTypes = new String[] { AO_INTF_0, AO_INTF_1, AO_INTF_2 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        // Only one AO is present in the RA descriptor.
        assertEquals(
                     "Incorrect number of AdminObject are present in the merged DD ",
                     3, annUtils.getAdministeredObjects(metatype, adminObjectClass));

        assertTrue(
                   "An AdminObject with the adminobject-interface="
                   + adminObjectTypes[0] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(
                                                  metatype, rarDisplayName, adminObjectClass,
                                                  adminObjectTypes[0]));

        assertTrue(
                   "An AdminObject with the adminobject-interface="
                   + adminObjectTypes[1] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(
                                                  metatype, rarDisplayName, adminObjectClass,
                                                  adminObjectTypes[1]));

        assertTrue(
                   "An AdminObject with the adminobject-interface="
                   + adminObjectTypes[2] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(
                                                  metatype, rarDisplayName, adminObjectClass,
                                                  adminObjectTypes[2]));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME,
                           "testNoElementMultipleInterfaceSuperClassInDD");
        }
    }

    /**
     * Case 7: Validate that if @AdministeredObject is specified with a single
     * class in the adminObjectInterfaces[], and a matching element exists in
     * the DD, that no new emf object is created
     * 
     * Here we use the class com.ibm.tra.ann.AdminObjectAnn2 which has the
     * following annotation @AdministeredObject(adminObjectInterfaces =
     * {TRAAdminObject.class}) packaged in a RAR which has the following
     * <adminobject> tag in the DD:
     * 
     * <adminobject>
     * <adminobject-interface>com.ibm.tra.inbound.base.TRAAdminObject
     * </adminobject-interface>
     * <adminobject-class>com.ibm.tra.ann.AdminObjectAnn2</adminobject-class>
     * </adminobject>
     * 
     * This should result in a single EMF admin object with:
     * 
     * AdminObjectClass = com.ibm.tra.ann.AdminObjectAnn2 AdminObjectInterface =
     * com.ibm.tra.inbound.base.TRAAdminObject
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */
    @Test
    public void testSingleElementSingleAOTInDD() throws Exception {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testSingleElementSingleAOTInDD");
        }

        rarDisplayName = RarTests.TRA_jca16_ann_AdministeredObjectMergeAction_SingleElementSingleAOT;

        adminObjectClass = AO_CLS_2;
        adminObjectTypes = new String[] { AO_INTF_0 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        assertEquals("Incorrect number of AdminObjects are present in the merged DD", 1,
                     annUtils.getAdministeredObjects(metatype, adminObjectClass));

        // The AO is the one we expect.
        assertTrue("An AdminObject with the adminobject-interface="
                   + adminObjectTypes[0] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(metatype,
                                                  rarDisplayName, adminObjectClass, adminObjectTypes[0]));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testSingleElementSingleAOTInDD");
        }
    }

    /**
     * Case 8: Validate that if @AdministeredObject is specified with mulitple
     * classes in the adminObjectInterfaces[], and matching elements exist in
     * the DD, that no new emf objects are created.
     * 
     * In this test case we create an RA that contains the
     * com.ibm.tra.ann.AdminObjectAnn4 class which contains the following
     * Annotation
     * 
     * @AdministeredObject(adminObjectInterfaces =
     *                                           {TRAAdminObject.class,TRAAdminObject1
     *                                           .class}) and the following
     *                                           <adminobject> elements in the
     *                                           DD:
     * 
     *                                           <adminobject>
     *                                           <adminobject-interface
     *                                           >com.ibm.tra
     *                                           .inbound.base.TRAAdminObject
     *                                           </adminobject-interface>
     *                                           <adminobject
     *                                           -class>com.ibm.tra.ann
     *                                           .AdminObjectAnn4
     *                                           </adminobject-class>
     *                                           </adminobject> <adminobject>
     *                                           <adminobject
     *                                           -interface>com.ibm.tra
     *                                           .inbound.base
     *                                           .TRAAdminObject1</adminobject
     *                                           -interface>
     *                                           <adminobject-class>com
     *                                           .ibm.tra.ann
     *                                           .AdminObjectAnn4</adminobject
     *                                           -class> </adminobject>
     * 
     *                                           This should result in 2 EMF
     *                                           admin objects with:
     * 
     *                                           AdminObjectClass =
     *                                           com.ibm.tra.ann.AdminObjectAnn4
     *                                           AdminObjectInterface =
     *                                           com.ibm.tra
     *                                           .inbound.base.TRAAdminObject,
     *                                           com.ibm.tra.inbound.base.
     *                                           TRAAdminObject1
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */
    @Test
    public void testMultipleElementMultipleAOTInDD() throws Exception {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testMultipleElementMultipleAOTInDD");
        }

        rarDisplayName = RarTests.TRA_jca16_ann_AdministeredObjectMergeAction_SingleElementSingleAOT;

        adminObjectClass = AO_CLS_4;
        adminObjectTypes = new String[] { AO_INTF_0, AO_INTF_1 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        assertEquals("Incorrect number of AdminObjects are present in the merged DD", 2,
                     annUtils.getAdministeredObjects(metatype, adminObjectClass));

        assertTrue(
                   "The first AdminObject with the adminobject-interface="
                   + adminObjectTypes[0] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(
                                                  metatype, rarDisplayName, adminObjectClass,
                                                  adminObjectTypes[0]));

        assertTrue(
                   "The second AdminObject with the adminobject-interface="
                   + adminObjectTypes[1] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(
                                                  metatype, rarDisplayName, adminObjectClass,
                                                  adminObjectTypes[1]));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testMultipleElementMultipleAOTInDD");
        }
    }

    /**
     * Case 9: Validate that if @AdministeredObject is specified with no classes
     * in the adminObjectInterfaces[], and the annotated class implements a
     * single interface and a matching element exists in the DD, that no new emf
     * object is created.
     * 
     * In this case we use an RA which contains the
     * com.ibm.tra.ann.AdminObjectAnn5 class. This class has an empty annotation
     * 
     * @AdministeredObject(adminObjectInterfaces = {}) and implements a single
     *                                           interface
     *                                           com.ibm.tra.inbound.base
     *                                           .TRAAdminObject. The RA has a
     *                                           matching element in the DD.
     * 
     *                                           <adminobject>
     *                                           <adminobject-interface
     *                                           >com.ibm.tra
     *                                           .inbound.base.TRAAdminObject
     *                                           </adminobject-interface>
     *                                           <adminobject
     *                                           -class>com.ibm.tra.ann
     *                                           .AdminObjectAnn5
     *                                           </adminobject-class>
     *                                           </adminobject>
     * 
     *                                           On deployment this should
     *                                           result in a single EMF
     *                                           AdminObject with:
     * 
     *                                           AdminObjectClass =
     *                                           com.ibm.tra.ann.AdminObjectAnn5
     *                                           AdminObjectInterface =
     *                                           com.ibm.tra
     *                                           .inbound.base.TRAAdminObject
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */
    @Test
    public void testNoElementSingleInterfaceSingleAOTInDD() throws Exception {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME,
                            "testNoElementSingleInterfaceSingleAOTInDD");
        }

        rarDisplayName = RarTests.TRA_jca16_ann_AdministeredObjectMergeAction_SingleElementSingleAOT;

        adminObjectClass = AO_CLS_5;
        adminObjectTypes = new String[] { AO_INTF_0 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        assertEquals("Incorrect number of AdminObjects are present in the merged DD", 1,
                     annUtils.getAdministeredObjects(metatype, adminObjectClass));

        assertTrue("An AdminObject with the adminobject-interface="
                   + adminObjectTypes[0] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(metatype,
                                                  rarDisplayName, adminObjectClass, adminObjectTypes[0]));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME,
                           "testNoElementSingleInterfaceSingleAOTInDD");
        }
    }

    /**
     * Case 10 : Validate that if @AdministeredObject is specified with no classes
     * in the adminObjectInterfaces[] element (empty array), and the annotated
     * class implements multiple interfaces and a matching element exists in the
     * DD for the class and one of the implemented interfaces that no new emf
     * object is created.
     * 
     * In this case we use an RA which contains the
     * com.ibm.tra.ann.AdminObjectAnn6 class. This class has an empty annotation
     * 
     * @AdministeredObject(adminObjectInterfaces = {}) and implements two
     *                                           interfaces
     *                                           com.ibm.tra.inbound.base
     *                                           .TRAAdminObject and
     *                                           com.ibm.tra.inbound.
     *                                           base.TRAAdminObject1. The RA
     *                                           has a matching element in the
     *                                           DD.
     * 
     *                                           <adminobject>
     *                                           <adminobject-interface
     *                                           >com.ibm.tra
     *                                           .inbound.base.TRAAdminObject
     *                                           </adminobject-interface>
     *                                           <adminobject
     *                                           -class>com.ibm.tra.ann
     *                                           .AdminObjectAnn6
     *                                           </adminobject-class>
     *                                           </adminobject>
     * 
     *                                           On deployment this should
     *                                           result in a single EMF
     *                                           AdminObject with:
     * 
     *                                           AdminObjectClass =
     *                                           com.ibm.tra.ann.AdminObjectAnn6
     *                                           AdminObjectInterface =
     *                                           com.ibm.tra.ann.TRAAdminObject
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */
    @Test
    public void testNoElementMultipleInterfacesSingleAOTInDD() throws Exception {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME,
                            "testNoElementMultipleInterfacesSingleAOTInDD");
        }

        rarDisplayName = RarTests.TRA_jca16_ann_AdministeredObjectMergeAction_SingleElementSingleAOT;

        adminObjectClass = AO_CLS_6;
        adminObjectTypes = new String[] { AO_INTF_0 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        assertEquals("Incorrect number of AdminObjects are present in the merged DD", 1,
                     annUtils.getAdministeredObjects(metatype, adminObjectClass));

        assertTrue("An AdminObject with the adminobject-interface="
                   + adminObjectTypes[0] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(metatype,
                                                  rarDisplayName, adminObjectClass, adminObjectTypes[0]));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME,
                           "testNoElementMultipleInterfacesSingleAOTInDD");
        }
    }

    /**
     * Case 11 : Validate that if @AdministeredObject is specified without any
     * interfaces and the class implements 3 interfaces two of which are
     * java.io.Serializable and java.io.Externalizable and a matching element
     * exists in the DD that no new emf object is created.
     * 
     * In this case we use an RA which contains the
     * com.ibm.tra.ann.AdminObjectAnn3 class. This class has an empty annotation @AdministeredObject
     * and implements a single interface com.ibm.tra.inbound.base.TRAAdminObject
     * in addition to the java.io.Externalizable and java.io.Serializable
     * interfaces . The RA has a matching element in the DD.
     * 
     * <adminobject>
     * <adminobject-interface>com.ibm.tra.inbound.base.TRAAdminObject
     * </adminobject-interface>
     * <adminobject-class>com.ibm.tra.ann.AdminObjectAnn3</adminobject-class>
     * </adminobject>
     * 
     * On deployment this should result in a single EMF AdminObject with:
     * 
     * AdminObjectClass = com.ibm.tra.ann.AdminObjectAnn3 AdminObjectInterface =
     * com.ibm.tra.ann.TRAAdminObject
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */
    @Test
    public void testNoElementMultipleInterfacesNoAOTInDD() throws Exception {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME,
                            "testNoElementMultipleInterfacesNoAOTInDD");
        }
        rarDisplayName = RarTests.TRA_jca16_ann_AdministeredObjectMergeAction_SingleElementSingleAOT;

        adminObjectClass = AO_CLS_3;
        adminObjectTypes = new String[] { AO_INTF_0 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        assertEquals("Incorrect number of AdminObjects are present in the merged DD", 1,
                     annUtils.getAdministeredObjects(metatype, adminObjectClass));

        assertTrue("An AdminObject exists with the adminobject-interface="
                   + adminObjectTypes[0] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(metatype,
                                                  rarDisplayName, adminObjectClass, adminObjectTypes[0]));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME,
                           "testNoElementMultipleInterfacesNoAOTInDD");
        }
    }

    /**
     * Case 12 : Validate that if @AdministeredObject is specified without an
     * adminObjectInterfaces[] element, and the annotated class implements
     * multiple interfaces and a matching element exists in the DD for the class
     * and one of the implemented interfaces that no new emf object is created.
     * 
     * In this case we use an RA which contains the
     * com.ibm.tra.ann.AdminObjectAnn7 class. This class has an @AdministeredObject
     * annotation with no elements and implements two interfaces
     * com.ibm.tra.inbound.base.TRAAdminObject and com.ibm.tra.inbound.
     * base.TRAAdminObject1. The RA has a matching element in the DD.
     * 
     * <adminobject>
     * <adminobject-interface>com.ibm.tra.inbound.base.TRAAdminObject
     * </adminobject-interface>
     * <adminobject-class>com.ibm.tra.ann.AdminObjectAnn7</adminobject-class>
     * </adminobject>
     * 
     * On deployment this should result in a single EMF AdminObject with:
     * 
     * AdminObjectClass = com.ibm.tra.ann.AdminObjectAnn7 AdminObjectInterface =
     * com.ibm.tra.inbound.base.TRAAdminObject
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */
    @Test
    public void testNoElementMultipleInterfacesMatchingAOTInDD() throws Exception {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME,
                            "testNoElementMultipleInterfacesMatchingAOTInDD");
        }

        rarDisplayName = RarTests.TRA_jca16_ann_AdministeredObjectMergeAction_SingleElementSingleAOT;

        adminObjectClass = AO_CLS_7;
        adminObjectTypes = new String[] { AO_INTF_0 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        assertEquals("Incorrect number of AdminObjects are present in the merged DD", 1,
                     annUtils.getAdministeredObjects(metatype, adminObjectClass));

        assertTrue("An AdminObject with the adminobject-interface="
                   + adminObjectTypes[0] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(metatype,
                                                  rarDisplayName, adminObjectClass, adminObjectTypes[0]));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME,
                           "testNoElementMultipleInterfacesMatchingAOTInDD");
        }
    }

    /**
     * Case 13 : Validate that if @AdministeredObject is specified without an
     * adminObjectInterfaces[] element, and the annotated class implements a
     * single interface and a matching element exists in the DD for the class
     * but not the implemented interface that an adminobject is created and
     * added to the DD in the correct place. Thus there are 2 entries in the DD
     * for this scenario.
     * 
     * In this case we use an RA which contains the
     * com.ibm.tra.ann.AdminObjectAnn8 class. This class has an @AdministeredObject
     * annotation with no elements and implements a single interface
     * com.ibm.tra.inbound.base.TRAAdminObject. The RA has a matching element in
     * the DD for the class but with the interface
     * com.ibm.tra.inbound.base.TRAAdminObject1.
     * 
     * <adminobject>
     * <adminobject-interface>com.ibm.tra.inbound.base.TRAAdminObject1
     * </adminobject-interface>
     * <adminobject-class>com.ibm.tra.ann.AdminObjectAnn8</adminobject-class>
     * </adminobject>
     * 
     * On deployment this should result in an EMF AdminObject for each of the
     * interfaces as shown:
     * 
     * AdminObjectClass = com.ibm.tra.ann.AdminObjectAnn8 AdminObjectInterface =
     * com.ibm.tra.inbound.base.TRAAdminObject
     * 
     * AdminObjectClass = com.ibm.tra.ann.AdminObjectAnn8 AdminObjectInterface =
     * com.ibm.tra.inbound.base.TRAAdminObject1
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */
    @Test
    public void testNoElementSingleInterfaceMatchingCAOTInDD() throws Exception {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME,
                            "testNoElementSingleInterfaceMatchingCAOTInDD");
        }

        rarDisplayName = RarTests.TRA_jca16_ann_AdministeredObjectMergeAction_SingleElementSingleAOT;

        adminObjectClass = AO_CLS_8;
        adminObjectTypes = new String[] { AO_INTF_1, AO_INTF_0 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        assertEquals(
                     "Incorrect number of AdminObjects are present in the merged DD",
                     2, annUtils.getAdministeredObjects(metatype, adminObjectClass));

        assertTrue("An AdminObject with the adminobject-interface="
                   + adminObjectTypes[0] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(metatype,
                                                  rarDisplayName, adminObjectClass, adminObjectTypes[0]));

        assertTrue("An AdminObject with the adminobject-interface="
                   + adminObjectTypes[1] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(metatype,
                                                  rarDisplayName, adminObjectClass, adminObjectTypes[1]));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME,
                           "testNoElementSingleInterfaceMatchingCAOTInDD");
        }
    }

    /**
     * Case 14 : Validate that if @AdministeredObject is specified without an
     * adminObjectInterfaces[] element, and the annotated class implements a
     * single interface and a matching element (same class and interface) exists
     * in the DD for the class and the implemented interface that no new emf
     * object is created.
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */
    @Test
    public void testNoElementSingleInterfaceMatchingCIAOTInDD() throws Exception {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME,
                            "testNoElementSingleInterfaceMatchingCIAOTInDD");
        }

        rarDisplayName = RarTests.TRA_jca16_ann_AdministeredObjectMergeAction_SingleElementSingleAOT;

        adminObjectClass = AO_CLS_9;
        adminObjectTypes = new String[] { AO_INTF_0 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        // Only one AO is present in the RA descriptor.
        assertEquals("Incorrect number of AdminObjects are present in the merged DD", 1,
                     annUtils.getAdministeredObjects(metatype, adminObjectClass));

        assertTrue("An AdminObject with the adminobject-interface="
                   + adminObjectTypes[0] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(metatype,
                                                  rarDisplayName, adminObjectClass, adminObjectTypes[0]));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME,
                           "testNoElementSingleInterfaceMatchingCIAOTInDD");
        }
    }

    /**
     * Case 15 : Validate that if @AdministeredObject is specified with any 3
     * interfaces and if 2 of those interfaces are implemented by the superclass
     * and a single matching element exists in the DD that two emf adminObjects
     * are created correctly (correct class name and correct interface name) and
     * added to the correct location in the DD
     * 
     * In this case we use an RA which contains the
     * com.ibm.tra.ann.AdminObjectAnn0 class. This class has an @AdministeredObject
     * annotation with 3 elements and it implements the interface
     * com.ibm.tra.inbound.base.TRAAdminObject2 and extends the class
     * com.ibm.tra.ann.AdminObjectAnn0 and a matching element in the DD for the
     * class.
     * 
     * <adminobject>
     * <adminobject-interface>com.ibm.tra.inbound.base.TRAAdminObject
     * </adminobject-interface>
     * <adminobject-class>com.ibm.tra.ann.AdminObjectAnn0</adminobject-class>
     * </adminobject>
     * 
     * On deployment this should result in an EMF AdminObject for each of the
     * interfaces as shown:
     * 
     * AdminObjectClass = com.ibm.tra.ann.AdminObjectAnn0 AdminObjectInterface =
     * com.ibm.tra.inbound.base.TRAAdminObject
     * 
     * AdminObjectClass = com.ibm.tra.ann.AdminObjectAnn0 AdminObjectInterface =
     * com.ibm.tra.inbound.base.TRAAdminObject1
     * 
     * AdminObjectClass = com.ibm.tra.ann.AdminObjectAnn0 AdminObjectInterface =
     * com.ibm.tra.inbound.base.TRAAdminObject2
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */
    @Test
    public void testNoSingleElementMultipleInterfaceSuperClassInDD() throws Exception {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME,
                            "testNoSingleElementMultipleInterfaceSuperClassInDD");
        }

        rarDisplayName = RarTests.TRA_jca16_ann_AdministeredObjectMergeAction_SingleElementSingleAOT;

        adminObjectClass = AO_CLS_0;
        adminObjectTypes = new String[] { AO_INTF_0, AO_INTF_1, AO_INTF_2 };

        metatype = annUtils.getMetatype(server, rarDisplayName);

        assertEquals(
                     "Incorrect number of AdminObjects are present in the merged DD",
                     3, annUtils.getAdministeredObjects(metatype, adminObjectClass));

        assertTrue(
                   "An AdminObject with the adminobject-interface="
                   + adminObjectTypes[0] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(
                                                  metatype, rarDisplayName, adminObjectClass,
                                                  adminObjectTypes[0]));

        assertTrue(
                   "An AdminObject with the adminobject-interface="
                   + adminObjectTypes[1] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(
                                                  metatype, rarDisplayName, adminObjectClass,
                                                  adminObjectTypes[1]));

        assertTrue(
                   "An AdminObject with the adminobject-interface="
                   + adminObjectTypes[2] + " and adminobject-class="
                   + adminObjectClass + " does not exist",
                   annUtils.getAdministeredObject(
                                                  metatype, rarDisplayName, adminObjectClass,
                                                  adminObjectTypes[2]));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME,
                           "testNoSingleElementMultipleInterfaceSuperClassInDD");
        }
    }

}
