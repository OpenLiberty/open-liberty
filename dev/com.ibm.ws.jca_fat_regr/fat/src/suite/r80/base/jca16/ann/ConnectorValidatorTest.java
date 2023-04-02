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

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import junit.framework.AssertionFailedError;
import suite.r80.base.jca16.J2cJavaBeanUtils;
import suite.r80.base.jca16.RarTests;
import suite.r80.base.jca16.TestSetupUtils;

/**
 * Class <code>ConnectorValidatorTest</code> verifies the
 * validation of JCA 1.6 @Connector annotation elements.
 * 
 * @see com.ibm.ejs.j2c.metadata.annotations.ConnectorValidator
 * @see com.ibm.ejs.j2c.metadata.annotations.ConnectorMergeAction
 * @see suite.r80.base.jca16.ann.ConnectorMergeActionTest
 * 
 */
@RunWith(FATRunner.class)
public class ConnectorValidatorTest {

    private final static String CLASSNAME = ConnectorValidatorTest.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASSNAME);

    protected static LibertyServer server;

    public ConnectorValidatorTest() {
    }

    ////////////////////
    // Validator tests

    final String CONN_NONJB_0_CLS = "com.ibm.tra.ann.ConnectorAnnNonJB0",
                    CONN_NONRA_0_CLS = "com.ibm.tra.ann.ConnectorAnnNonRA0";

    @BeforeClass
    public static void setUp() throws Exception {

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "setUp");
        }
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jca.fat.regr", null, false);
        TestSetupUtils.setUpGwcApp(server);

//      Package TRA_jca16_ann_ConnectorValidator_NonRA0.rar
        JavaArchive traAnnEjsResourceAdapter_jar = TestSetupUtils.getTraAnnEjsResourceAdapter_jar();

        JavaArchive connector_NonRA0_jar = ShrinkWrap.create(JavaArchive.class, "Connector_NonRA0.jar");
        connector_NonRA0_jar.addClass("com.ibm.tra.ann.ConnectorAnnNonRA0");

        ResourceAdapterArchive TRA_jca16_ann_ConnectorValidator_NonRA0 = ShrinkWrap.create(ResourceAdapterArchive.class,
                                                                                           RarTests.TRA_jca16_ann_ConnectorValidator_NonRA0
                                                                                                                         + ".rar").addAsLibrary(traAnnEjsResourceAdapter_jar).addAsLibrary(connector_NonRA0_jar).addAsManifestResource(new File(TestSetupUtils.raDir
                                                                                                                                                                                                                                                + RarTests.TRA_jca16_ann_ConnectorValidator_NonRA0
                                                                                                                                                                                                                                                + "-ra.xml"),
                                                                                                                                                                                                                                       "ra.xml");
        ShrinkHelper.exportToServer(server, "connectors", TRA_jca16_ann_ConnectorValidator_NonRA0);

        server.startServer("ConnectorValidatorTest.log");

        assertNotNull("Server should report it has started",
                      server.waitForStringInLogUsingMark("CWWKF0011I"));

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "tearDown");
        }
        if (server.isStarted()) {
            server.stopServer("J2CA9903E: .*ConnectorAnnNonRA0", "CWWKS0005E: .*", "CWWKG0033W: .*"); // EXPECTED
        }
    }

    /**
     * Case 1: Validate that if the annotated class does not implement the
     * ResourceAdapter interface that a validation exception is thrown.
     * 
     * @throws AssertionFailedError when the test case fails.
     */
    @Test
    @ExpectedFFDC({ "javax.resource.spi.ResourceAdapterInternalException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testIsaResourceAdapter() throws Throwable {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.entering(CLASSNAME, "testIsaResourceAdapter");
        }

        final String rarId = "FAT1";

        server.setServerConfigurationFile("ConnectorValidatorTest_server.xml");

        assertNotNull(
                      "The log does not contain message J2CA9903E",
                      server.waitForStringInLogUsingMark(rarId + ".*J2CA9903E:.*javax.resource.spi.ResourceAdapter",
                                                         server.getMatchingLogFile("trace.log")));

        // This is unecessary, really.  But if we get here, then the 
        // RAR installed and we can check for a false-positive.
        assertFalse(
                    "The Connector class implements ResourceAdapter",
                    J2cJavaBeanUtils.isAssignableFrom(
                                                      javax.resource.spi.ResourceAdapter.class,
                                                      CONN_NONRA_0_CLS));

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.exiting(CLASSNAME, "testIsaResourceAdapter");
        }
    }

    // Validator tests
    ////////////////////

}
