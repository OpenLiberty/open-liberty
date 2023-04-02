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
public class MDB30XMLTest extends FATServletClient {

    @Server("ejbcontainer.mdb.jms.fat.mdb30.xml")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("ejbcontainer.mdb.jms.fat.mdb30.xml")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("ejbcontainer.mdb.jms.fat.mdb30.xml")).andWith(new JakartaEE9Action().fullFATOnly().forServers("ejbcontainer.mdb.jms.fat.mdb30.xml")).andWith(new JakartaEE10Action().fullFATOnly().forServers("ejbcontainer.mdb.jms.fat.mdb30.xml"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### MDBXMLApp.ear
        JavaArchive MDBXMLEJB = ShrinkHelper.buildJavaArchive("MDBXMLEJB.jar", "com.ibm.ws.ejbcontainer.mdb.jms.xml.ejb.");
        MDBXMLEJB = (JavaArchive) ShrinkHelper.addDirectory(MDBXMLEJB, "test-applications/MDBXMLEJB.jar/resources");
        JavaArchive MDBXMLEJB2 = ShrinkHelper.buildJavaArchive("MDBXMLEJB2.jar", "com.ibm.ws.ejbcontainer.mdb.jms.xml.ejb2.");
        MDBXMLEJB2 = (JavaArchive) ShrinkHelper.addDirectory(MDBXMLEJB2, "test-applications/MDBXMLEJB2.jar/resources");
        WebArchive MDBXMLWeb = ShrinkHelper.buildDefaultApp("MDBXMLWeb.war", "com.ibm.ws.ejbcontainer.mdb.jms.xml.web.");

        EnterpriseArchive MDBXMLApp = ShrinkWrap.create(EnterpriseArchive.class, "MDBXMLApp.ear");
        MDBXMLApp.addAsModule(MDBXMLEJB).addAsModule(MDBXMLEJB2).addAsModule(MDBXMLWeb);
        MDBXMLApp = (EnterpriseArchive) ShrinkHelper.addDirectory(MDBXMLApp, "test-applications/MDBXMLApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, MDBXMLApp, DeployOptions.SERVER_ONLY);

        //#################### MDBIntXMLApp.ear
        JavaArchive MDBIntXMLEJB = ShrinkHelper.buildJavaArchive("MDBIntXMLEJB.jar", "com.ibm.ws.ejbcontainer.mdb.jms.interceptor.xml.ejb.");
        MDBIntXMLEJB = (JavaArchive) ShrinkHelper.addDirectory(MDBIntXMLEJB, "test-applications/MDBIntXMLEJB.jar/resources");
        WebArchive MDBIntXMLWeb = ShrinkHelper.buildDefaultApp("MDBIntXMLWeb.war", "com.ibm.ws.ejbcontainer.mdb.jms.interceptor.xml.web.");

        EnterpriseArchive MDBIntXMLApp = ShrinkWrap.create(EnterpriseArchive.class, "MDBIntXMLApp.ear");
        MDBIntXMLApp.addAsModule(MDBIntXMLEJB).addAsModule(MDBIntXMLWeb);
        MDBIntXMLApp = (EnterpriseArchive) ShrinkHelper.addDirectory(MDBIntXMLApp, "test-applications/MDBIntXMLApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, MDBIntXMLApp, DeployOptions.SERVER_ONLY);

        // Finally, start server
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("CNTR0047E", "WTRN0017W");
    }

    private final void runTest(boolean intTest) throws Exception {
        if (intTest) {
            FATServletClient.runTest(server, "MDBIntXMLWeb/MDB30IntXMLServlet", getTestMethodSimpleName());
        } else {
            FATServletClient.runTest(server, "MDBXMLWeb/MDB30XMLServlet", getTestMethodSimpleName());
        }
    }

    private void runTest(String testStep) throws Exception {
        FATServletClient.runTest(server, "MDBXMLWeb/MDB30XMLServlet", testStep);
    }

    /*
     * testBMTIA()
     *
     * MIA02 - Illegal access for BMT
     * MTX14 - BMT: UserTransaction.begin() before committing the previous transaction
     */
    @Test
    @ExpectedFFDC({ "javax.transaction.NotSupportedException" })
    public void testXMLBMTIA() throws Exception {
        runTest(false);
    }

    /*
     * testBMTNoCommit()
     *
     * send a message to MDB BMTBeanNoCommit
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSITransactionRolledbackException", "javax.ejb.TransactionRolledbackLocalException" })
    public void testXMLBMTNoCommit() throws Exception {
        runTest(false);
    }

    /*
     * testCMTIA()
     *
     * MIA01 - Illegal access for CMT
     * MCM08 - MDB to access SLL and SLR
     */
    @Test
    public void testXMLCMTIA() throws Exception {
        runTest(false);
    }

    /*
     * testCommonMDB()
     */
    @Test
    public void testXMLCommonMDB() throws Exception {
        runTest(false);
    }

    /*
     * testNonDurableTopic()
     *
     * MCM03 - Test non-durable is the default settings for Topic
     */
    @Test
    public void testXMLNonDurableTopic() throws Exception {
        runTest(false);
    }

    /*
     * testDurableTopic()
     *
     * MCM04 - Stop the server and receive message
     */
    @Test
    public void testXMLDurableTopic() throws Exception {
        runTest("testXMLDurableTopic1");
        try {
            server.stopServer("CNTR0047E", "WTRN0017W");
        } finally {
            server.startServer();
            assertNotNull(server.waitForStringInLog("CWWKF0011I"));
            runTest("testXMLDurableTopic2");
        }
    }

    /*
     * testMessageSelector()
     *
     * MCM02 - Make an MDB using the msg selector
     */
    @Test
    public void testXMLMessageSelector() throws Exception {
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
    public void testXMLCMTNotSupported() throws Exception {
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
    public void testXMLCMTRequired() throws Exception {
        runTest(false);
    }

    /*
     * testCMTRequiredRollback()
     *
     * MTX19 - CMT's onMsg() with 'Required', the Tx is not committed (DB update is not committed)
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void testXMLCMTRequiredRollback() throws Exception {
        // The FFDC for this test occurs every time, but sometimes isn't detected until after the test completes
        server.setMarkToEndOfLog();
        runTest(false);
        assertNotNull(server.waitForStringInLogUsingMark("com.ibm.websphere.csi.CSITransactionRolledbackException"));
        Thread.sleep(500);
    }

    @Test
    public void testXMLCommonMDBTopic() throws Exception {
        runTest(false);
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
    public void testXMLMDB01Bean() throws Exception {
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
    public void testXMLMDB02Bean() throws Exception {
        runTest(true);
    }

    /*
     * testMDB02Beanm verifies the use InvocationContext API, e.g. getTarget, getMethod, getParameters. It also vierfies the passing of context data among interceptors with
     * getContextData.
     *
     * The test uses the MDB "InterceptorMDB02Bean".
     */
    @Test
    public void testXMLMDB02Beanm() throws Exception {
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
    public void testXMLMDB03Bean() throws Exception {
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
    public void testXMLMDB04Bean() throws Exception {
        runTest(true);
    }
}