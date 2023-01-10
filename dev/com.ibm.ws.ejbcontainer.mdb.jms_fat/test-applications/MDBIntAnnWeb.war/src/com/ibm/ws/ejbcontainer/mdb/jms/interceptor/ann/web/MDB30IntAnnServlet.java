/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.mdb.jms.interceptor.ann.web;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;
import com.ibm.ws.ejbcontainer.mdb.jms.interceptor.ann.ejb.InterceptorMDB01Bean;
import com.ibm.ws.ejbcontainer.mdb.jms.interceptor.ann.ejb.InterceptorMDB02Bean;
import com.ibm.ws.ejbcontainer.mdb.jms.interceptor.ann.ejb.InterceptorMDB03Bean;
import com.ibm.ws.ejbcontainer.mdb.jms.interceptor.ann.ejb.InterceptorMDB04Bean;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> MDB30IntAnnTest
 *
 * <dt><b>Test Description:</b>
 * <dd>In this testcase, the functionality of interceptors is tested with the use of MDB.
 * <p>
 * Highlight of testcase:
 * <ol>
 * <li>Verifying invocation order for around invoke and callback interceptors
 * <li>Visibility of interceptor methods (public, private, protected or package protected)
 * <li>Multiple interceptors (annotation or XML DD)
 * <li>Annotation, XML DD and mixed augmented scenarios
 * <li>Default interceptors with XML DD
 * <li>Exclude default interceptors at class and/or method level
 * <li>Exclude class interceptors
 * <li>Callback interceptors (PostConstruct and PreDestroy for MDB)
 * <li>Interceptor instance life cycle
 * <li>InvocationContext API verification
 * <li>Verify that a simple Stateless EJB can be successfully injected into an MDB at the class, field, and method levels
 * </ol>
 *
 * <p>
 * <dt><b>Test Matrix:</b>
 * <dt>Test Matrix:
 * <dd>
 * <br>
 * Sub-tests
 * <ul>
 * <li>test1 verifies post construct callback interceptor invocation order, around invoke interceptor invocation order, interceptor and bean instance life cycle. Also verifies that
 * a simple Stateless
 * EJB can be successfully injected into an MDB at the class level.
 * <li>test2 verifies post construct callback interceptor invocation order, around invoke interceptor invocation order, and the use of exclude class interceptors. Also verifies
 * that a simple Stateless
 * EJB can be successfully injected into an MDB at the field level.
 * <li>test2m verifies the use of InvocationContext API, e.g. getTarget, getMethod, and getParameters, along with the passing of context data among interceptors
 * <li>test3 verifies post construct callback interceptor invocation order, around invoke interceptor invocation order, and the use of exclude class interceptors. Also verifies
 * that a simple Stateless
 * EJB can be successfully injected into an MDB at the method level.
 * <li>test4 verifies post construct callback interceptor invocation order, around invoke interceptor invocation order, and the use of exclude class interceptors. Also verifies
 * that a simple Stateless
 * EJB can be successfully injected into an MDB at the field level with specified values for the name and beanInterface values of the EJB annotation.
 * </ul>
 * <br>
 * Data Sources
 * </dl>
 */
@WebServlet("/MDB30IntAnnServlet")
@SuppressWarnings("serial")
public class MDB30IntAnnServlet extends FATServlet {
    private final static String CLASS_NAME = MDB30IntAnnServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    // JNDI for JMS Resources
    String qcfName = "jms/TestQCF";
    String testResultQueueName = "jms/TestResultQueue";
    String InterceptorMDBReqQueueName = "jms/MDBReqQueue";
    String InterceptorMDB02ReqQueueName = "jms/MDB02ReqQueue";
    String InterceptorMDB03ReqQueueName = "jms/MDB03ReqQueue";
    String InterceptorMDB04ReqQueueName = "jms/MDB04ReqQueue";

    private static String newline = System.getProperty("line.separator");

    /**
     * Expected around invoke strings for testMDB01Bean.
     */
    private static final String[] EXPECTED_testMDB01Bean_AROUND_INVOKE = new String[] { "Interceptor01.aroundInvoke", "Interceptor02.aroundInvoke", "Interceptor03.aroundInvoke",
                                                                                        "Interceptor04.aroundInvoke", "InterceptorMDB01Bean.aroundInvoke",
                                                                                        "InterceptorMDB01Bean.onMessage" };

    /**
     * Expected post construct strings for testMDB01Bean.
     */
    private static final String[] EXPECTED_testMDB01Bean_POST_CONSTRUCT = new String[] { "Interceptor01.postConstruct", "Interceptor02.postConstruct",
                                                                                         "InterceptorMDB01Bean.postConstruct" };

    /**
     * Expected pre destroy strings for testMDB01Bean.
     */
    private static final String[] EXPECTED_testMDB01Bean_PRE_DESTROY = new String[] { "Interceptor01.preDestroy", "Interceptor02.preDestroy", "InterceptorMDB01Bean.preDestroy" };

    /**
     * Expected CALLINJEJB string for testMDB01Bean.
     */
    private static final String EXPECTED_testMDB01Bean_CALLINJEJB = "Just completed " + "lookup of ejb/SLEnvInjectTest_local_biz." + newline + "Successfully "
                                                                    + "looked up the injected EJB and received expected message: success from the " + "method call.";
    /**
     * Expected around invoke strings for testMDB02Bean.
     */
    private static final String[] EXPECTED_testMDB02Bean_AROUND_INVOKE = new String[] { "Interceptor04.aroundInvoke", "Interceptor03.aroundInvoke",
                                                                                        "InterceptorMDB02Bean.aroundInvoke",
                                                                                        "InterceptorMDB02Bean.onMessage" };

    /**
     * Expected around invoke strings for testMDB02Bean.
     */
    private static final String[] EXPECTED_testMDB02Bean_POST_CONSTRUCT = new String[] { "Interceptor01.postConstruct", "Interceptor02.postConstruct",
                                                                                         "InterceptorMDB02Bean.postConstruct" };

    /**
     * Expected around invoke strings for testMDB02Bean.
     */
    private static final String[] EXPECTED_testMDB02Bean_PRE_DESTROY = new String[] { "Interceptor01.preDestroy", "Interceptor02.preDestroy", "InterceptorMDB02Bean.preDestroy" };

    /**
     * Expected CALLINJEJB string for testMDB02Bean.
     */
    private static final String EXPECTED_testMDB02Bean_CALLINJEJB = "Successfully invoked " + "the getString() on the injected bean and it returned: success from the "
                                                                    + "method call." + newline
                                                                    + "Just completed lookup of com.ibm.ws.ejbcontainer.mdb.jms.interceptor.ann.ejb"
                                                                    + ".InterceptorMDB02Bean/injectedRef."
                                                                    + newline + "Successfully looked "
                                                                    + "up the injected EJB via the default ENC created by injecting the EJB at the "
                                                                    + "field level and received expected message: success from the method call.";

    /**
     * Expected around invoke strings for testMDB03Bean.
     */
    private static final String[] EXPECTED_testMDB03Bean_AROUND_INVOKE = new String[] { "Interceptor02.aroundInvoke", "Interceptor01.aroundInvoke",
                                                                                        "InterceptorMDB03Bean.aroundInvoke",
                                                                                        "InterceptorMDB03Bean.onMessage" };

    /**
     * Expected around invoke strings for testMDB03Bean.
     */
    private static final String[] EXPECTED_testMDB03Bean_POST_CONSTRUCT = new String[] { "Interceptor02.postConstruct", "Interceptor01.postConstruct",
                                                                                         "InterceptorMDB03Bean.postConstruct" };

    /**
     * Expected around invoke strings for testMDB03Bean.
     */
    private static final String[] EXPECTED_testMDB03Bean_PRE_DESTROY = new String[] { "Interceptor02.preDestroy", "Interceptor01.preDestroy", "InterceptorMDB03Bean.preDestroy" };

    /**
     * Expected CALLINJEJB string for testMDB03Bean.
     */
    private static final String EXPECTED_testMDB03Bean_CALLINJEJB = "Successfully invoked " + "the getString() on the injected bean and it returned: success from the method call."
                                                                    + newline
                                                                    + "Just completed lookup of com.ibm.ws.ejbcontainer.mdb.jms.interceptor.ann.ejb.InterceptorMDB03Bean/simpleSL."
                                                                    + newline
                                                                    + "Successfully looked up the injected EJB via the default ENC created by injecting the EJB"
                                                                    + " at the method level and received expected message: success from the method call.";

    /**
     * Expected around invoke strings for testMDB04Bean.
     */
    private static final String[] EXPECTED_testMDB04Bean_AROUND_INVOKE = new String[] { "InterceptorMDB04Bean.aroundInvoke", "InterceptorMDB04Bean.onMessage" };

    /**
     * Expected around invoke strings for testMDB04Bean.
     */
    private static final String[] EXPECTED_testMDB04Bean_POST_CONSTRUCT = new String[] { "Interceptor03.postConstruct", "Interceptor02.postConstruct",
                                                                                         "Interceptor01.postConstruct",
                                                                                         "InterceptorMDB04Bean.postConstruct" };

    /**
     * Expected around invoke strings for testMDB04Bean.
     */
    private static final String[] EXPECTED_testMDB04Bean_PRE_DESTROY = new String[] { "Interceptor03.preDestroy", "Interceptor02.preDestroy", "Interceptor01.preDestroy",
                                                                                      "InterceptorMDB04Bean.preDestroy" };

    /**
     * Expected CALLINJEJB string for testMDB04Bean.
     */
    private static final String EXPECTED_testMDB04Bean_CALLINJEJB = "Successfully invoked the getString() on the injected bean and it returned: " + "success from the method call."
                                                                    + newline
                                                                    + "Just completed lookup of java:comp/env/ejb/yoMama." + newline
                                                                    + "Successfully looked up the injected EJB via the ENC entry that "
                                                                    + "was defined in the name option of EJB annotation at the field level "
                                                                    + "and received expected message: success from the method call.";

    /*
     * All testcases in this test basically utilize the same mechanism of implementation. The test work as client to submit queue request message, which also invokes the onMessage
     * and the around
     * invoke/callback interceptors. Based on the different request, the MDB will check the singleton global "datastore" for the recorded invocation sequence or other recorded info
     * to be verified by
     * the MDB or client. Eventually the data or result is returned to the client through message queue too.
     */

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
    public void testAnnMDB01Bean() throws Exception {
        String actualInvSeq, expectedInvSeq;

        FATMDBHelper.emptyQueue(qcfName, testResultQueueName);
        FATMDBHelper.putQueueMessage("PostConstruct", qcfName, InterceptorMDBReqQueueName);
        actualInvSeq = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        expectedInvSeq = Arrays.toString(EXPECTED_testMDB01Bean_POST_CONSTRUCT);
        svLogger.info("Expected post construct invocation sequence: " + expectedInvSeq);
        svLogger.info("Actual post construct invocation sequence: " + actualInvSeq);
        assertEquals("Comparing post construct invocation sequences", expectedInvSeq, actualInvSeq);
        InterceptorMDB01Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("ClearAll", qcfName, InterceptorMDBReqQueueName);
        FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        InterceptorMDB01Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("AroundInvoke", qcfName, InterceptorMDBReqQueueName);
        actualInvSeq = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        expectedInvSeq = Arrays.toString(EXPECTED_testMDB01Bean_AROUND_INVOKE);
        svLogger.info("Expected around invoke invocation sequence: " + expectedInvSeq);
        svLogger.info("Actual around invoke invocation sequence: " + actualInvSeq);
        assertEquals("Comparing around invoke invocation sequences", expectedInvSeq, actualInvSeq);
        InterceptorMDB01Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("ClearAll", qcfName, InterceptorMDBReqQueueName);
        FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        InterceptorMDB01Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("AroundInvoke", qcfName, InterceptorMDBReqQueueName);
        actualInvSeq = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        expectedInvSeq = Arrays.toString(EXPECTED_testMDB01Bean_AROUND_INVOKE);
        svLogger.info("Expected around invoke invocation sequence: " + expectedInvSeq);
        svLogger.info("Actual around invoke invocation sequence: " + actualInvSeq);
        assertEquals("Comparing around invoke invocation sequences", expectedInvSeq, actualInvSeq);
        InterceptorMDB01Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("ClearAll", qcfName, InterceptorMDBReqQueueName);
        FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        InterceptorMDB01Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("CallInjEJB", qcfName, InterceptorMDBReqQueueName);
        actualInvSeq = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        expectedInvSeq = EXPECTED_testMDB01Bean_CALLINJEJB;
        svLogger.info("Expected: " + expectedInvSeq);
        svLogger.info("Actual: " + actualInvSeq);
        svLogger.info("Comparing expected and actual strings, if they match test passes.");
        assertEquals("Bean injectedRef --> Lookup using the ENC name derived from class level EJB injection " + "options was successsful.", expectedInvSeq, actualInvSeq);
        assertEquals("Bean injectedRef --> Method call on injectedRef returned expected results.", expectedInvSeq, actualInvSeq);
        InterceptorMDB01Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("ClearAll", qcfName, InterceptorMDBReqQueueName);
        FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        InterceptorMDB01Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("PreDestroy", qcfName, InterceptorMDBReqQueueName);
        actualInvSeq = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        expectedInvSeq = Arrays.toString(EXPECTED_testMDB01Bean_PRE_DESTROY);
        svLogger.info("Expected pre destroy invocation sequence: " + expectedInvSeq);
        svLogger.info("Actual pre destroy invocation sequence: " + actualInvSeq);
        // expecting populated construct because mdb pooling is disabled
        assertEquals("Comparing pre destroy invocation sequences", expectedInvSeq, actualInvSeq);
        InterceptorMDB01Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("ClearAll", qcfName, InterceptorMDBReqQueueName);
        FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        InterceptorMDB01Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);
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
    public void testAnnMDB02Bean() throws Exception {
        String actualInvSeq, expectedInvSeq;

        FATMDBHelper.emptyQueue(qcfName, testResultQueueName);
        FATMDBHelper.putQueueMessage("PostConstruct", qcfName, InterceptorMDB02ReqQueueName);
        actualInvSeq = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        expectedInvSeq = Arrays.toString(EXPECTED_testMDB02Bean_POST_CONSTRUCT);
        svLogger.info("Expected post construct invocation sequence: " + expectedInvSeq);
        svLogger.info("Actual post construct invocation sequence: " + actualInvSeq);
        assertEquals("Comparing post construct invocation sequences", expectedInvSeq, actualInvSeq);
        InterceptorMDB02Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("ClearAll", qcfName, InterceptorMDB02ReqQueueName);
        FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        InterceptorMDB02Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("AroundInvoke", qcfName, InterceptorMDB02ReqQueueName);
        actualInvSeq = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        expectedInvSeq = Arrays.toString(EXPECTED_testMDB02Bean_AROUND_INVOKE);
        svLogger.info("Expected around invoke invocation sequence: " + expectedInvSeq);
        svLogger.info("Actual around invoke invocation sequence: " + actualInvSeq);
        assertEquals("Comparing around invoke invocation sequences", expectedInvSeq, actualInvSeq);
        InterceptorMDB02Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("ClearAll", qcfName, InterceptorMDB02ReqQueueName);
        FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        InterceptorMDB02Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("CallInjEJB", qcfName, InterceptorMDB02ReqQueueName);
        actualInvSeq = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        expectedInvSeq = EXPECTED_testMDB02Bean_CALLINJEJB;
        svLogger.info("Expected: " + expectedInvSeq);
        svLogger.info("Actual: " + actualInvSeq);
        svLogger.info("Comparing expected and actual strings, if they match test passes.");
        assertEquals("Bean injectedRef --> Method call on injectedRef returned expected results.", expectedInvSeq, actualInvSeq);
        assertEquals("ctx.lookup() --> Lookup using the default ENC entry that should " + "have been added by default via the field level injection was successsful.",
                     expectedInvSeq, actualInvSeq);
        assertEquals("Bean obj --> Method call on obj returned expected results.", expectedInvSeq, actualInvSeq);
        InterceptorMDB02Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("ClearAll", qcfName, InterceptorMDB02ReqQueueName);
        FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        InterceptorMDB02Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("PreDestroy", qcfName, InterceptorMDB02ReqQueueName);
        actualInvSeq = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        expectedInvSeq = Arrays.toString(EXPECTED_testMDB02Bean_PRE_DESTROY);
        svLogger.info("Expected pre destroy invocation sequence: " + expectedInvSeq);
        svLogger.info("Actual pre destroy invocation sequence: " + actualInvSeq);
        // expecting populated construct because mdb pooling is disabled
        assertEquals("Comparing pre destroy invocation sequences", expectedInvSeq, actualInvSeq);
        InterceptorMDB02Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("ClearAll", qcfName, InterceptorMDB02ReqQueueName);
        FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        InterceptorMDB02Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);
    }

    /*
     * testMDB02Beanm verifies the use InvocationContext API, e.g. getTarget, getMethod, getParameters. It also vierfies the passing of context data among interceptors with
     * getContextData.
     *
     * The test uses the MDB "InterceptorMDB02Bean".
     */
    public void testAnnMDB02Beanm() throws Exception {
        String actualChkStr, expectedChkStr = "true";

        FATMDBHelper.emptyQueue(qcfName, testResultQueueName);
        FATMDBHelper.putQueueMessage("Target", qcfName, InterceptorMDB02ReqQueueName);
        actualChkStr = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        assertEquals("Verifying InvocationContext.getTarget()", expectedChkStr, actualChkStr);
        InterceptorMDB02Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("ClearAll", qcfName, InterceptorMDB02ReqQueueName);
        FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        InterceptorMDB02Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("Method", qcfName, InterceptorMDB02ReqQueueName);
        actualChkStr = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        assertEquals("Verifying InvocationContext.getMethod()", expectedChkStr, actualChkStr);
        InterceptorMDB02Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("ClearAll", qcfName, InterceptorMDB02ReqQueueName);
        FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        InterceptorMDB02Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("Parameters", qcfName, InterceptorMDB02ReqQueueName);
        actualChkStr = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        assertEquals("Verifying InvocationContext.getParameters()", expectedChkStr, actualChkStr);
        InterceptorMDB02Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("ClearAll", qcfName, InterceptorMDB02ReqQueueName);
        FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        InterceptorMDB02Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);
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
    public void testAnnMDB03Bean() throws Exception {
        String actualInvSeq, expectedInvSeq;

        FATMDBHelper.emptyQueue(qcfName, testResultQueueName);
        FATMDBHelper.putQueueMessage("PostConstruct", qcfName, InterceptorMDB03ReqQueueName);
        actualInvSeq = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        expectedInvSeq = Arrays.toString(EXPECTED_testMDB03Bean_POST_CONSTRUCT);
        svLogger.info("Expected post construct invocation sequence: " + expectedInvSeq);
        svLogger.info("Actual post construct invocation sequence: " + actualInvSeq);
        assertEquals("Comparing post construct invocation sequences", expectedInvSeq, actualInvSeq);
        InterceptorMDB03Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("ClearAll", qcfName, InterceptorMDB03ReqQueueName);
        FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        InterceptorMDB03Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("AroundInvoke", qcfName, InterceptorMDB03ReqQueueName);
        actualInvSeq = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        expectedInvSeq = Arrays.toString(EXPECTED_testMDB03Bean_AROUND_INVOKE);
        svLogger.info("Expected around invoke invocation sequence: " + expectedInvSeq);
        svLogger.info("Actual around invoke invocation sequence: " + actualInvSeq);
        assertEquals("Comparing around invoke invocation sequences", expectedInvSeq, actualInvSeq);
        InterceptorMDB03Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("ClearAll", qcfName, InterceptorMDB03ReqQueueName);
        FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        InterceptorMDB03Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("CallInjEJB", qcfName, InterceptorMDB03ReqQueueName);
        actualInvSeq = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        expectedInvSeq = EXPECTED_testMDB03Bean_CALLINJEJB;
        svLogger.info("Expected: " + expectedInvSeq);
        svLogger.info("Actual: " + actualInvSeq);
        svLogger.info("Comparing expected and actual strings, if they match test passes.");
        assertEquals("Bean injectedRef --> Method call on injectedRef returned expected results.", expectedInvSeq, actualInvSeq);
        assertEquals("ctx.lookup() --> Lookup using the default ENC entry that should " + "have been added by default via the method level injection was successsful.",
                     expectedInvSeq, actualInvSeq);
        assertEquals("Bean obj --> Method call on obj returned expected results.", expectedInvSeq, actualInvSeq);
        InterceptorMDB03Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("ClearAll", qcfName, InterceptorMDB03ReqQueueName);
        FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        InterceptorMDB03Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("PreDestroy", qcfName, InterceptorMDB03ReqQueueName);
        actualInvSeq = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        expectedInvSeq = Arrays.toString(EXPECTED_testMDB03Bean_PRE_DESTROY);
        svLogger.info("Expected pre destroy invocation sequence: " + expectedInvSeq);
        svLogger.info("Actual pre destroy invocation sequence: " + actualInvSeq);
        // expecting populated construct because mdb pooling is disabled
        assertEquals("Comparing pre destroy invocation sequences", expectedInvSeq, actualInvSeq);
        InterceptorMDB03Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("ClearAll", qcfName, InterceptorMDB03ReqQueueName);
        FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        InterceptorMDB03Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);
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
    public void testAnnMDB04Bean() throws Exception {
        String actualInvSeq, expectedInvSeq;

        FATMDBHelper.emptyQueue(qcfName, testResultQueueName);
        FATMDBHelper.putQueueMessage("PostConstruct", qcfName, InterceptorMDB04ReqQueueName);
        actualInvSeq = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        expectedInvSeq = Arrays.toString(EXPECTED_testMDB04Bean_POST_CONSTRUCT);
        svLogger.info("Expected post construct invocation sequence: " + expectedInvSeq);
        svLogger.info("Actual post construct invocation sequence: " + actualInvSeq);
        assertEquals("Comparing post construct invocation sequences", expectedInvSeq, actualInvSeq);
        InterceptorMDB04Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("ClearAll", qcfName, InterceptorMDB04ReqQueueName);
        FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        InterceptorMDB04Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("AroundInvoke", qcfName, InterceptorMDB04ReqQueueName);
        actualInvSeq = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        expectedInvSeq = Arrays.toString(EXPECTED_testMDB04Bean_AROUND_INVOKE);
        svLogger.info("Expected around invoke invocation sequence: " + expectedInvSeq);
        svLogger.info("Actual around invoke invocation sequence: " + actualInvSeq);
        assertEquals("Comparing around invoke invocation sequences", expectedInvSeq, actualInvSeq);
        InterceptorMDB04Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("ClearAll", qcfName, InterceptorMDB04ReqQueueName);
        FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        InterceptorMDB04Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("CallInjEJB", qcfName, InterceptorMDB04ReqQueueName);
        actualInvSeq = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        expectedInvSeq = EXPECTED_testMDB04Bean_CALLINJEJB;
        svLogger.info("Expected: " + expectedInvSeq);
        svLogger.info("Actual: " + actualInvSeq);
        svLogger.info("Comparing expected and actual strings, if they match test passes.");
        assertEquals("Bean injectedRef --> Method call on injectedRef returned expected results.", expectedInvSeq, actualInvSeq);
        assertEquals("ctx.lookup() --> Lookup using the ENC entry that was defined in the name option " + "of EJB annotation was successsful.", expectedInvSeq, actualInvSeq);
        assertEquals("Bean obj --> Method call on obj returned expected results.", expectedInvSeq, actualInvSeq);
        InterceptorMDB04Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("ClearAll", qcfName, InterceptorMDB04ReqQueueName);
        FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        InterceptorMDB04Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("PreDestroy", qcfName, InterceptorMDB04ReqQueueName);
        actualInvSeq = (String) FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        expectedInvSeq = Arrays.toString(EXPECTED_testMDB04Bean_PRE_DESTROY);
        svLogger.info("Expected pre destroy invocation sequence: " + expectedInvSeq);
        svLogger.info("Actual pre destroy invocation sequence: " + actualInvSeq);
        // expecting populated construct because mdb pooling is disabled
        assertEquals("Comparing pre destroy invocation sequences", expectedInvSeq, actualInvSeq);
        InterceptorMDB04Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);

        FATMDBHelper.putQueueMessage("ClearAll", qcfName, InterceptorMDB04ReqQueueName);
        FATMDBHelper.getQueueMessage(qcfName, testResultQueueName);
        InterceptorMDB04Bean.svDestroyLatch.await(30, TimeUnit.SECONDS);
    }
}