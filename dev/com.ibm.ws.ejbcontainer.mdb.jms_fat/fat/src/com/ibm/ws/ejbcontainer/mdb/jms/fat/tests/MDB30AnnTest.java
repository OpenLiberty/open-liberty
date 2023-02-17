/*******************************************************************************
 * Copyright (c) 2015, 2023 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.mdb.jms.fat.tests;

import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class MDB30AnnTest extends FATServletClient {

    @Server("ejbcontainer.mdb.jms.fat.mdb30.ann")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("ejbcontainer.mdb.jms.fat.mdb30.ann")).andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers("ejbcontainer.mdb.jms.fat.mdb30.ann")).andWith(new JakartaEE9Action().fullFATOnly().forServers("ejbcontainer.mdb.jms.fat.mdb30.ann")).andWith(new JakartaEE10Action().fullFATOnly().forServers("ejbcontainer.mdb.jms.fat.mdb30.ann"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### MDBAnnApp.ear
        JavaArchive MDBAnnEJB = ShrinkHelper.buildJavaArchive("MDBAnnEJB.jar", "com.ibm.ws.ejbcontainer.mdb.jms.ann.ejb.");
        MDBAnnEJB = (JavaArchive) ShrinkHelper.addDirectory(MDBAnnEJB, "test-applications/MDBAnnEJB.jar/resources");
        WebArchive MDBAnnWeb = ShrinkHelper.buildDefaultApp("MDBAnnWeb.war", "com.ibm.ws.ejbcontainer.mdb.jms.ann.web.");

        EnterpriseArchive MDBAnnApp = ShrinkWrap.create(EnterpriseArchive.class, "MDBAnnApp.ear");
        MDBAnnApp.addAsModule(MDBAnnEJB).addAsModule(MDBAnnWeb);
        MDBAnnApp = (EnterpriseArchive) ShrinkHelper.addDirectory(MDBAnnApp, "test-applications/MDBAnnApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, MDBAnnApp, DeployOptions.SERVER_ONLY);

        //#################### MDBIntAnnApp.ear
        JavaArchive MDBIntAnnEJB = ShrinkHelper.buildJavaArchive("MDBIntAnnEJB.jar", "com.ibm.ws.ejbcontainer.mdb.jms.interceptor.ann.ejb.");
        MDBIntAnnEJB = (JavaArchive) ShrinkHelper.addDirectory(MDBIntAnnEJB, "test-applications/MDBIntAnnEJB.jar/resources");
        WebArchive MDBIntAnnWeb = ShrinkHelper.buildDefaultApp("MDBIntAnnWeb.war", "com.ibm.ws.ejbcontainer.mdb.jms.interceptor.ann.web.");

        EnterpriseArchive MDBIntAnnApp = ShrinkWrap.create(EnterpriseArchive.class, "MDBIntAnnApp.ear");
        MDBIntAnnApp.addAsModule(MDBIntAnnEJB).addAsModule(MDBIntAnnWeb);
        MDBIntAnnApp = (EnterpriseArchive) ShrinkHelper.addDirectory(MDBIntAnnApp, "test-applications/MDBIntAnnApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, MDBIntAnnApp, DeployOptions.SERVER_ONLY);

        // Finally, start server
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("CNTR0047E", "WTRN0017W");
    }

    private final void runTest(boolean intTest) throws Exception {
        if (intTest) {
            FATServletClient.runTest(server, "MDBIntAnnWeb/MDB30IntAnnServlet", getTestMethodSimpleName());
        } else {
            FATServletClient.runTest(server, "MDBAnnWeb/MDB30AnnServlet", getTestMethodSimpleName());
        }
    }

    private void runTest(String testStep) throws Exception {
        FATServletClient.runTest(server, "MDBAnnWeb/MDB30AnnServlet", testStep);
    }

    /*
     * testBMTIA()
     *
     * MIA02 - Illegal access for BMT
     * MTX14 - BMT: UserTransaction.begin() before committing the previous transaction
     */
    @Test
    @ExpectedFFDC({ "javax.transaction.NotSupportedException" })
    public void testAnnBMTIA() throws Exception {
        runTest(false);
    }

    /*
     * testBMTNoCommit()
     *
     * send a message to MDB BMTBeanNoCommit
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSITransactionRolledbackException", "javax.ejb.TransactionRolledbackLocalException" })
    public void testAnnBMTNoCommit() throws Exception {
        runTest(false);
    }

    /*
     * testCMTIA()
     *
     * MIA01 - Illegal access for CMT
     * MCM08 - MDB to access SLL and SLR
     */
    @Test
    public void testAnnCMTIA() throws Exception {
        runTest(false);
    }

    /*
     * testNonDurableTopic()
     *
     * MCM03 - Test non-durable is the default settings for Topic
     */
    @Test
    public void testAnnNonDurableTopic() throws Exception {
        runTest(false);
    }

    /*
     * testDurableTopic()
     *
     * MCM04 - Stop the server and receive message
     */
    @Test
    public void testAnnDurableTopic() throws Exception {
        runTest("testAnnDurableTopic1");
        try {
            server.stopServer("CNTR0047E", "WTRN0017W");
        } finally {
            server.startServer();
            assertNotNull(server.waitForStringInLog("CWWKF0011I"));
            runTest("testAnnDurableTopic2");
        }
    }

    /*
     * testMessageSelector()
     *
     * MCM02 - Make an MDB using the msg selector
     */
    @Test
    public void testAnnMessageSelector() throws Exception {
        runTest(false);
    }

    /*
     * testCMTNotSupported()
     *
     * MTX15 - CMT's onMsg() with 'NotSupported' invoking getGlobalTransaction to see onMsg is not part of any transaction context
     * MTX17 - CMT's onMsg() with 'Required' transaction attribute accessing a CMTD SLL with T attribute 'supports', check no Tx context is passed to SL
     * MTX20 - CMT's onMsg() with 'notsupported' throws IllegalStateExp if invoking setRollbackOnly
     * MTX21 - CMT's onMsg() with 'notsupported' throws IllegalStateExp if invoking getRollbackOnly
     */
    @Test
    public void testAnnCMTNotSupported() throws Exception {
        runTest(false);
    }

    /*
     * testCMTRequired()
     *
     * MTX17 - CMT's onMsg() with 'Required' getting GlobalTransaction to see a transaction context, and accessing a CMTD SLL with T attribute 'supports', check the Tx context is
     * passed to SL
     * MTX19 - CMT's onMsg() with 'Required' invoking getRollbackOnly
     */
    @Test
    public void testAnnCMTRequired() throws Exception {
        runTest(false);
    }

    /*
     * testCMTRequiredRollback()
     *
     * MTX19 - CMT's onMsg() with 'Required', the Tx is not committed (DB update is not committed)
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void testAnnCMTRequiredRollback() throws Exception {
        // The FFDC for this test occurs every time, but sometimes isn't detected until after the test completes
        server.setMarkToEndOfLog();
        runTest(false);
        assertNotNull(server.waitForStringInLogUsingMark("com.ibm.websphere.csi.CSITransactionRolledbackException"));
        Thread.sleep(500);
    }

    /*
     * testMDB01Bean verifies interceptor invocation order. Two annotated at class level, two annotated at method level, around invoke within bean class. In particular, it tests
     * the use of around
     * invoke with the onMessage method of MDB. In addition, ExcludeDefaultInterceptor is placed at class level along with some testing on bean instance life cycle.
     *
     * This test also verifies that a simple Stateless EJB can be successfully injected into an MDB at the class level. This uses the "CallInjEJB" section of the onMessage method
     * of the MDB. The
     * injected bean is looked up and then a method is called on the instance of the injected EJB. The combined results of these steps are sent back to the TestResultQueue to be
     * compared to the
     * expected results.
     *
     * The test uses the MDB "InterceptorMDB01Bean".
     */
    @Test
    public void testAnnMDB01Bean() throws Exception {
        runTest(true);
    }

    /*
     * testMDB02Bean verifies interceptor invocation order. Two annotated at class level, two annotated at method level (with different order), around invoke within bean class. In
     * particular, it tests
     * the use of around invoke with the onMessage method of MDB. In addition, ExcludeDefaultInterceptor is placed at method level along with ExcludeClassInterceptors.
     *
     * This test also verifies that a simple Stateless EJB can be successfully injected into an MDB at the field level. This uses the "CallInjEJB" section of the onMessage method
     * of the MDB. A method
     * is called on the injectedRef to verify it. Then a new instance, obj, is created by doing a lookup using the default ENC that should have been created when the EJB was
     * injected at the field
     * level. This new ejbref, obj, is verified by calling a method on it. The combined results of these steps are sent back to the TestResultQueue to be compared to the expected
     * results.
     *
     * The test uses the MDB "InterceptorMDB02Bean".
     */
    @Test
    public void testAnnMDB02Bean() throws Exception {
        runTest(true);
    }

    /*
     * testMDB02Beanm verifies the use InvocationContext API, e.g. getTarget, getMethod, getParameters. It also vierfies the passing of context data among interceptors with
     * getContextData.
     *
     * The test uses the MDB "InterceptorMDB02Bean".
     */
    @Test
    public void testAnnMDB02Beanm() throws Exception {
        runTest(true);
    }

    /*
     * testMDB03Bean verifies interceptor invocation order. Two annotated at class level (with different order), around invoke within bean class. In particular, it tests the use of
     * around invoke with
     * the onMessage method of MDB. In addition, ExcludeDefaultInterceptor is placed at class level along with ExcludeClassInterceptors.
     *
     * This test also verifies that a simple Stateless EJB can be successfully injected into an MDB at the method level. This uses the "CallInjEJB" section of the onMessage method
     * of the MDB. A method
     * is called on the injectedRef to verify it. Then a new instance, obj, is created by doing a lookup using the default ENC that should have been created when the EJB was
     * injected at the field
     * level. This new ejbref, obj, is verified by calling a method on it. The combined results of these steps are sent back to the TestResultQueue to be compared to the expected
     * results.
     *
     * The test uses the MDB "InterceptorMDB03Bean".
     */
    @Test
    public void testAnnMDB03Bean() throws Exception {
        runTest(true);
    }

    /*
     * testMDB04Bean verifies interceptor invocation order. Two annotated at class level (with different order), around invoke within bean class. In particular, it tests the use of
     * around invoke with
     * the onMessage method of MDB. In addition, ExcludeDefaultInterceptor is placed at method level along.
     *
     * This test also verifies that a simple Stateless EJB can be successfully injected into an MDB at the field level with given values for name and beanInterface attributes of
     * the EJB annotation.
     * This uses the "CallInjEJB" section of the onMessage method of the MDB. A method is called on the injectedRef to verify it. Then a new instance, obj, is created by doing a
     * lookup using the ENC
     * that was specified in the name attribute when the EJB was injected at the field level. This new ejbref, obj, is verified by calling a method on it. The combined results of
     * these steps are sent
     * back to the TestResultQueue to be compared to the expected results.
     *
     * The test uses the MDB "InterceptorMDB04Bean".
     */
    @Test
    public void testAnnMDB04Bean() throws Exception {
        runTest(true);
    }
}