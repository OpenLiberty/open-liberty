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

import static org.junit.Assert.assertNotNull;

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

import componenttest.annotation.AllowedFFDC;
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
public class AdministeredObjectValidatorTest {

    private final static String CLASSNAME = AdministeredObjectValidatorTest.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASSNAME);
    protected static LibertyServer server;

    public AdministeredObjectValidatorTest() {
    }

    @BeforeClass
    public static void setUp() throws Exception {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "setUp");
        }
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jca.fat.regr", null, false);

        server.setServerConfigurationFile("AdministeredObjectValidatorTest_server.xml");

//      Package TRA_jca16_ann_AdministeredObjectValidator_MultipleInterfacesEmptyArray TRA_jca16_ann_AdministeredObjectValidator_MultipleInterfacesEmptyArray.rar
        JavaArchive traAnnEjsResourceAdapter_jar = TestSetupUtils.getTraAnnEjsResourceAdapter_jar();

        JavaArchive administeredObject_MultipleInterfacesEmptyArray_jar = ShrinkWrap.create(JavaArchive.class, "AdministeredObject_MultipleInterfacesEmptyArray.jar");
        administeredObject_MultipleInterfacesEmptyArray_jar.addClass("com.ibm.tra.ann.AdminObjectAnn11");

        ResourceAdapterArchive TRA_jca16_ann_AdministeredObjectValidator_MultipleInterfacesEmptyArray = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                          RarTests.TRA_jca16_ann_AdministeredObjectValidator_MultipleInterfacesEmptyArray
                                                                                                                                                        + ".rar").addAsLibrary(traAnnEjsResourceAdapter_jar).addAsLibrary(administeredObject_MultipleInterfacesEmptyArray_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                              + RarTests.TRA_jca16_ann_AdministeredObjectValidator_MultipleInterfacesEmptyArray
                                                                                                                                                                                                                                                                                                              + "-ra.xml"),
                                                                                                                                                                                                                                                                                                     "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_AdministeredObjectValidator_MultipleInterfacesEmptyArray);

//      Package TRA_jca16_ann_AdministeredObjectValidator_MultipleInterfacesNoElement TRA_jca16_ann_AdministeredObjectValidator_MultipleInterfacesNoElement.rar
        JavaArchive administeredObject_MultipleInterfacesNoElement_jar = ShrinkWrap.create(JavaArchive.class, "AdministeredObject_MultipleInterfacesNoElement.jar");
        administeredObject_MultipleInterfacesNoElement_jar.addClass("com.ibm.tra.ann.AdminObjectAnn12");

        ResourceAdapterArchive TRA_jca16_ann_AdministeredObjectValidator_MultipleInterfacesNoElement = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                         RarTests.TRA_jca16_ann_AdministeredObjectValidator_MultipleInterfacesNoElement
                                                                                                                                                       + ".rar").addAsLibrary(traAnnEjsResourceAdapter_jar).addAsLibrary(administeredObject_MultipleInterfacesNoElement_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                            + RarTests.TRA_jca16_ann_AdministeredObjectValidator_MultipleInterfacesNoElement
                                                                                                                                                                                                                                                                                                            + "-ra.xml"),
                                                                                                                                                                                                                                                                                                   "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_AdministeredObjectValidator_MultipleInterfacesNoElement);

//      Package TRA_jca16_ann_AdministeredObjectValidator_SuperClassTwoInterfaces TRA_jca16_ann_AdministeredObjectValidator_SuperClassTwoInterfaces.rar
        JavaArchive administeredObject_SuperClassTwoInterfaces_jar = ShrinkWrap.create(JavaArchive.class, "AdministeredObject_SuperClassTwoInterfaces.jar");
        administeredObject_SuperClassTwoInterfaces_jar.addClass("com.ibm.tra.ann.AdminObjectAnn15");
        administeredObject_SuperClassTwoInterfaces_jar.addClass("com.ibm.tra.ann.AdminObjectAnnSuperClass2");

        ResourceAdapterArchive TRA_jca16_ann_AdministeredObjectValidator_SuperClassTwoInterfaces = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                                     RarTests.TRA_jca16_ann_AdministeredObjectValidator_SuperClassTwoInterfaces
                                                                                                                                                   + ".rar").addAsLibrary(traAnnEjsResourceAdapter_jar).addAsLibrary(administeredObject_SuperClassTwoInterfaces_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                                                    + RarTests.TRA_jca16_ann_AdministeredObjectValidator_SuperClassTwoInterfaces
                                                                                                                                                                                                                                                                                                    + "-ra.xml"),
                                                                                                                                                                                                                                                                                           "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_AdministeredObjectValidator_SuperClassTwoInterfaces);

        server.startServer("AdministeredObjectValidatorTest.log");
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
            server.stopServer("J2CA9926E"); // EXPECTED
        }

    }

    /**
     * Case 1: Validate that if @AdministeredObject is specified with no classes
     * in the adminObjectInterfaces[] element (empty array), and the annotated
     * class implements multiple interfaces and no matching element exists in
     * the DD for the class and any of the implemented interfaces that a
     * validation exception is thrown.
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalStateException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testValidationOfEmptyInterfaceList() throws Throwable {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testValidationOfEmptyInterfaceList");
        }

        try {
            String message = "J2CA9926E.*@AdministeredObject.*com.ibm.tra.ann.AdminObjectAnn11";
            assertNotNull("The error J2CA9926E was not logged",
                          (server.waitForStringInLog(
                                                     RarTests.INSTALL_FAILED_J2CA9926E,
                                                     server.getMatchingLogFile("messages.log"))));
            assertNotNull(
                          "The error message for J2CA9926E was logged",
                          (server.waitForStringInLog(message,
                                                     server.getMatchingLogFile("messages.log"))));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testValidationOfEmptyInterfaceList");
        }
    }

    /**
     * Case 2: Validate that if @AdministeredObject is specified without an
     * adminObjectInterfaces[] element, and the annotated class implements
     * multiple interfaces and no matching element exists in the DD for the
     * class and any of the implemented interfaces that a validation exception
     * is thrown.
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalStateException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testValidationOfUnspecifiedInterface() throws Throwable {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testValidationOfUnspecifiedInterface");
        }

        try {
            assertNotNull("The error J2CA9926E was not logged",
                          (server.waitForStringInLog(
                                                     RarTests.INSTALL_FAILED_J2CA9926E,
                                                     server.getMatchingLogFile("messages.log"))));
            String message = "J2CA9926E.* @AdministeredObject.*com.ibm.tra.ann.AdminObjectAnn12";
            assertNotNull(
                          "The error message for J2CA9926E was logged",
                          (server.waitForStringInLog(message,
                                                     server.getMatchingLogFile("messages.log"))));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testValidationOfUnspecifiedInterface");
        }
    }

    /**
     * Case 3: Validate that if @AdministeredObject is specified without an
     * adminObjectInterfaces[] element on a class that extends a class which
     * implements 2 interfaces then a validation exception is thrown
     * 
     * @throws AssertionFailedError
     *                                  when the test case fails.
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalStateException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testValidationOfAOSuperClass() throws Throwable {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testValidationOfAOSuperClass");
        }
        try {
            assertNotNull("The error J2CA9926E was not logged",
                          (server.waitForStringInLog(
                                                     RarTests.INSTALL_FAILED_J2CA9926E,
                                                     server.getMatchingLogFile("messages.log"))));
            String message = "J2CA9926E.*@AdministeredObject.*com.ibm.tra.ann.AdminObjectAnn15";
            assertNotNull(
                          "The error message for J2CA9926E was logged",
                          (server.waitForStringInLog(message,
                                                     server.getMatchingLogFile("messages.log"))));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testValidationOfAOSuperClass");
        }
    }
}
