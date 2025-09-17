/*******************************************************************************
 * Copyright (c) 2012,2025 IBM Corporation and others.
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
package test.client.jca;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Basic tests covering use of a generic resource adapter.
 */
@MinimumJavaLevel(javaLevel = 11)
@RunWith(FATRunner.class)
public class JCABVTTest extends FATServletClient {

    @Server("com.ibm.ws.jca.fat.bvt")
    public static LibertyServer server;

    /**
     * Set up application and resource adapter.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "bvtapp.war")
                        .addPackages(true, "web")
                        .addAsWebInfResource(new File("test-applications/bvtapp/resources/WEB-INF/web.xml"))
                        .addAsWebInfResource(new File("test-applications/bvtapp/resources/WEB-INF/ibm-web-bnd.xml"))
                        .addAsWebInfResource(new File("test-applications/bvtapp/resources/WEB-INF/ibm-ejb-jar-bnd.xml"));
        ShrinkHelper.exportDropinAppToServer(server, war);

        ResourceAdapterArchive rar1 = ShrinkWrap.create(ResourceAdapterArchive.class, "JCARAR1.rar")
                        .addAsLibraries(ShrinkWrap.create(JavaArchive.class)
                                        .addPackage("test.jca.adapter"))
                        .addAsManifestResource(new File("test-resourceadapters/JCARAR1/resources/META-INF/ra.xml"))
                        .addAsManifestResource(new File("test-resourceadapters/JCARAR1/resources/META-INF/wlp-ra.xml"));
        ShrinkHelper.exportToServer(server, "resourceadapters", rar1);

        ResourceAdapterArchive rar2 = ShrinkWrap.create(ResourceAdapterArchive.class, "JCARAR2.rar")
                        .addAsLibraries(ShrinkWrap.create(JavaArchive.class)
                                        .addPackage("test.jca.adapter"))
                        .addAsManifestResource(new File("test-resourceadapters/JCARAR2/resources/META-INF/ra.xml"));
        ShrinkHelper.exportToServer(server, "resourceadapters", rar2);

        server.startServer();
    }

    /**
     * Final teardown work when class is exiting.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        server.stopServer("CWWKS1864W", // warning for using {aes} AES-128 password
                          "CWWKE0700W", // permanent workaround for Derby per RTC 290586/github 25902
                          "J2CA0027E", // intentionally caused to require XA recovery
                          "J2CA8625E.*UnsupportedContext", // error path test for unsupported work context type
                          "J2CA8688E.*J2CA8624E.*CollectionContext", // error path test for duplicate work context
                          "J2CA8688E.*J2CA8687E.*LONGRUNNING", // error path test for HintsContext with wrong data type
                          "J2CA8688E.*J2CA8687E.*NAME", // error path test for HintsContext with wrong data type
                          "WTRN0048W.*RMFAIL" // intentionally caused to require XA recovery
        );
    }

    @Test
    public void testActivationSpec() throws Exception {
        runTest(server, "bvtapp", "testActivationSpec");
    }

    @Test
    public void testAdminObjects() throws Exception {
        runTest(server, "bvtapp", "testAdminObjects");
    }

    @Test
    public void testContainerManagedAuth() throws Exception {
        runTest(server, "bvtapp", "testContainerManagedAuth");
    }

    @Test
    public void testConnectionFactory() throws Exception {
        runTest(server, "bvtapp", "testConnectionFactory");
    }

    @Test
    public void testDirectLookups() throws Exception {
        runTest(server, "bvtapp", "testDirectLookups");
    }

    @Test
    public void testSharing() throws Exception {
        runTest(server, "bvtapp", "testSharing");
    }

    @Test
    public void testTimer() throws Exception {
        runTest(server, "bvtapp", "testTimer");
    }

    @Test
    public void testWorkContext() throws Exception {
        runTest(server, "bvtapp", "testWorkContext");
    }

    @AllowedFFDC({
                   "java.util.concurrent.ExecutionException", // error path test for unsupported work context type
                   "java.util.concurrent.RejectedExecutionException", // error path test for intentionally caused failure on context apply
                   "jakarta.resource.spi.work.WorkCompletedException", // error path test for unsupported work context type
                   "jakarta.resource.spi.work.WorkRejectedException" // error path test with ExecutionContext and WorkContext both specified
    })
    @Test
    public void testWorkContextInflow() throws Exception {
        runTest(server, "bvtapp", "testWorkContextInflow");
    }

    @AllowedFFDC({
                   "javax.transaction.xa.XAException" // intentionally caused to require XA recovery
    })
    @Test
    public void testXARecovery() throws Exception {
        runTest(server, "bvtapp", "testXARecovery");
    }

    @Test
    public void testService_ObjectClassMatching() throws Exception {
        assertTrue(server.findStringsInLogs("CFReference successfully bound resource factory").size() > 0);
    }
}
