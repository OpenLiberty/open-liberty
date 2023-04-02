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

import static org.junit.Assert.assertFalse;
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

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import junit.framework.AssertionFailedError;
import suite.r80.base.jca16.J2cJavaBeanUtils;
import suite.r80.base.jca16.RarTests;
import suite.r80.base.jca16.TestSetupUtils;

/**
 * Class <code>ConnectionDefinitionValidatorTest</code> verifies the
 * validation of JCA 1.6 @ConnectionDefinition annotation elements.
 * 
 * @see com.ibm.ejs.j2c.metadata.annotations.ConnectionDefinitionValidator
 * @see com.ibm.ejs.j2c.metadata.annotations.ConnectionDefinitionMergeAction
 * @see suite.r80.base.jca16.ann.ConnectionDefinitionMergeActionTest
 */
@RunWith(FATRunner.class)
public class ConnectionDefinitionValidatorTest {

    private final static String CLASSNAME = ConnectionDefinitionValidatorTest.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASSNAME);

    protected static LibertyServer server;

    public ConnectionDefinitionValidatorTest() {
    }

    ////////////////////
    // Validator tests

    final String CD_1_NonJB = "com.ibm.tra.ann.ConnDefAnnNonJB1", // Non-JavaBean    
                    CD_1_NonMCF = "com.ibm.tra.ann.ConnDefAnnNonMCF1"; // Non-ManagedConnectionFactory    

    @BeforeClass
    public static void setUp() throws Exception {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "setUp");
        }
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jca.fat.regr", null, false);
        server.setServerConfigurationFile("ConnectionDefinitionValidatorTest_server.xml");

//      Package TRA_jca16_ann_ConnectionDefinitionValidator_NonMCF1.rar
        JavaArchive traAnnEjsResourceAdapter_jar = TestSetupUtils.getTraAnnEjsResourceAdapter_jar();

        JavaArchive connectionDefinition_NonMCF1_jar = ShrinkWrap.create(JavaArchive.class, "ConnectionDefinition_NonMCF1.jar");
        connectionDefinition_NonMCF1_jar.addClass("com.ibm.tra.ann.ConnDefAnnNonMCF1");

        ResourceAdapterArchive TRA_jca16_ann_ConnectionDefinitionValidator_NonMCF1 = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                                       RarTests.TRA_jca16_ann_ConnectionDefinitionValidator_NonMCF1
                                                                                                                                     + ".rar").addAsLibrary(traAnnEjsResourceAdapter_jar).addAsLibrary(connectionDefinition_NonMCF1_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                                        + RarTests.TRA_jca16_ann_ConnectionDefinitionValidator_NonMCF1
                                                                                                                                                                                                                                                                        + "-ra.xml"),
                                                                                                                                                                                                                                                               "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConnectionDefinitionValidator_NonMCF1);

        server.startServer("ConnectionDefinitionValidatorTest.log");
        assertNotNull("Server should report it has started",
                      server.waitForStringInLogUsingMark("CWWKF0011I"));

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "tearDown");
        }
        if (server.isStarted()) {
            server.stopServer("J2CA9903E: .*ConnDefAnnNonMCF1"); // EXPECTED
        }
    }

    /**
     * Case 1: Validate that if a class annotated with @ConnectionDefinition
     * does not implement the ManagedConnectionFactory interface, a validation
     * exception is thrown.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    public void testIsaManagedConnectionFactory() throws Throwable {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testIsaManagedConnectionFactory");
        }

        final String rarId = "FAT1";

        // The message can appear before or after server start has been detected,
        // so examine the whole console log and fail after a reasonable amount of time has passed
        assertNotNull(
                      "The log does not contain message J2CA9903E",
                      server.waitForStringInLog("J2CA7002E.*" + rarId + ".*J2CA9903E.*javax.resource.spi.ManagedConnectionFactory.*"));

        // This is unnecessary, really.  But if we get here, then the 
        // RAR installed and we can check for a false-positive.
        assertFalse(
                    "The ConnectionDefinition class implements MCF",
                    J2cJavaBeanUtils.isAssignableFrom(javax.resource.spi.ManagedConnectionFactory.class, CD_1_NonMCF));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testIsaManagedConnectionFactory");
        }
    }

    // Validator tests
    ////////////////////

}
