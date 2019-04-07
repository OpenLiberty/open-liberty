/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.web;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.NoSuchEJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.InitRecoveryLogBean;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.InvalidInterface;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.MyAppException;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.NoSpecifiedTimeoutBean;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.StatefulTimeoutAnnotationOnlyBean;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.StatefulTimeoutAnnotationOnlyDefaultUnitBean;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.StatefulTimeoutAnnotationOverrideExtFileBean;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.StatefulTimeoutAsyncBean;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.StatefulTimeoutNegativeOneAnnotationBean;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.StatefulTimeoutNegativeOneXMLBean;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.StatefulTimeoutXMLNoTimeoutStanzaBean;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.StatefulTimeoutXMLOnlyBean;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.StatefulTimeoutXMLOnlyDefaultUnitBean;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.StatefulTimeoutXMLOverrideExtFileBean;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.StatefulTimeoutXMLUnitButNoTimeoutBean;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.StatefulTimeoutZeroAnnotationBean;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.StatefulTimeoutZeroXMLBean;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.StatelessWithStatefulTimeoutAnnotationBean;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.StatelessWithStatefulTimeoutXMLBean;
import com.ibm.ws.ejbcontainer.session.passivation.statefulTimeout.ejb.TimeoutInExtFileOnlyBean;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;

@SuppressWarnings("serial")
@WebServlet("/StatefulTimeoutServlet")
public class StatefulTimeoutServlet extends FATServlet {
    private final static String CLASSNAME = StatefulTimeoutServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private boolean recLogInit = false;

    private final static long BUFFER = 10 * 1000; // 10 seconds
    private final static long EXPECTED_DEFAULT_TIMEOUT = 10 * 60 * 1000; // 10 minutes
    private final static long USER_TIMEOUT = 15 * 1000; // 15 seconds
    private final static long ONE_MIN = 60 * 1000; // 60 seconds

    private final static String JNDI_USER_TRAN = "java:comp/UserTransaction";
    private final static String JNDI_INIT_RECOVERY_LOG_BEAN = "java:app/StatefulTimeoutEJB/InitRecoveryLogBean";
    private final static String JNDI_NO_SPECIFIED_TIMEOUT_BEAN = "java:app/StatefulTimeoutEJB/NoSpecifiedTimeoutBean";
    private final static String JNDI_EXT_FILE_ONLY = "java:app/StatefulTimeoutEJB/TimeoutInExtFileOnlyBean";
    private final static String JNDI_ANN_ONLY = "java:app/StatefulTimeoutEJB/StatefulTimeoutAnnotationOnlyBean";
    private final static String JNDI_ANN_DEFAULT_UNIT_BEAN = "java:app/StatefulTimeoutEJB/StatefulTimeoutAnnotationOnlyDefaultUnitBean";
    private final static String JNDI_XML_DEFAULT_UNIT_BEAN = "java:app/StatefulTimeoutEJB/StatefulTimeoutXMLOnlyDefaultUnitBean";
    private final static String JNDI_XML_ONLY = "java:app/StatefulTimeoutEJB/StatefulTimeoutXMLOnlyBean";
    private final static String JNDI_NEG_ONE_ANN_BEAN = "java:app/StatefulTimeoutEJB/StatefulTimeoutNegativeOneAnnotationBean";
    private final static String JNDI_NEG_ONE_XML_BEAN = "java:app/StatefulTimeoutEJB/StatefulTimeoutNegativeOneXMLBean";
    private final static String JNDI_NEG_TWO_ANN_BEAN = "java:app/StatefulTimeoutEJB/StatefulTimeoutNegativeTwoAnnotationBean";
    private final static String JNDI_NEG_TWO_XML_BEAN = "java:app/StatefulTimeoutEJB/StatefulTimeoutNegativeTwoXMLBean";
    private final static String JNDI_ZERO_ANN_BEAN = "java:app/StatefulTimeoutEJB/StatefulTimeoutZeroAnnotationBean";
    private final static String JNDI_ZERO_XML_BEAN = "java:app/StatefulTimeoutEJB/StatefulTimeoutZeroXMLBean";
    private final static String JNDI_INVALID_INTERFACE = "java:app/StatefulTimeoutEJB/InvalidInterfaceBean";
    private final static String JNDI_ANN_OVERRIDE_EXT_FILE = "java:app/StatefulTimeoutEJB/StatefulTimeoutAnnotationOverrideExtFileBean";
    private final static String JNDI_XML_OVERRIDE_EXT_FILE = "java:app/StatefulTimeoutEJB/StatefulTimeoutXMLOverrideExtFileBean";
    private final static String JNDI_XML_NO_TIMEOUT_STANZA = "java:app/StatefulTimeoutEJB/StatefulTimeoutXMLNoTimeoutStanzaBean";
    private final static String JNDI_XML_UNIT_NO_TIMEOUT_STANZA = "java:app/StatefulTimeoutEJB/StatefulTimeoutXMLUnitButNoTimeoutBean";
    private final static String JNDI_STATELESS_WITH_ANN_TIMEOUT = "java:app/StatefulTimeoutEJB/StatelessWithStatefulTimeoutAnnotationBean";
    private final static String JNDI_STATELESS_WITH_XML_TIMEOUT = "java:app/StatefulTimeoutEJB/StatelessWithStatefulTimeoutXMLBean";
    private final static String JNDI_XML_TIMEOUT_OVERFLOW = "java:app/StatefulTimeoutEJB/StatefulTimeoutXMLTimeoutOverflowBean";
    private final static String JNDI_ASYNC = "java:app/StatefulTimeoutEJB/StatefulTimeoutAsyncBean";

    public void initRecoveryLog() throws Exception {
        // Call a method on a bean to initialize the server recovery log
        svLogger.info("initialize the server recovery log");
        InitRecoveryLogBean bean = (InitRecoveryLogBean) new InitialContext().lookup(JNDI_INIT_RECOVERY_LOG_BEAN);
        bean.getInvocationTime();

        recLogInit = true;
    }

    /**
     * Tests that if the bean contains no specified session timeout at all
     * (i.e. no extension file, no @StatefulTimeout annotation, no
     * stateful-timeout element in the XML, etc.), that the default timeout will
     * be in place - currently that default is 10 minutes.
     */
    @Test
    @Mode(FULL)
    @SkipForRepeat(SkipForRepeat.EE7_FEATURES)
    public void testNoSpecifiedTimeout() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        NoSpecifiedTimeoutBean bean = (NoSpecifiedTimeoutBean) new InitialContext().lookup(JNDI_NO_SPECIFIED_TIMEOUT_BEAN);
        long invokeTime = bean.getInvocationTime();
        FATHelper.sleep(BUFFER);
        try {
            invokeTime = bean.getInvocationTime();
        } catch (NoSuchEJBException ex) {
            fail("Bean timed out after " + BUFFER + " ms, when the default timeout is 10 minutes.");
        }

        FATHelper.sleep(EXPECTED_DEFAULT_TIMEOUT + BUFFER);
        try {
            long waitTime = bean.getInvocationTime() - invokeTime;
            fail("Expected NoSuchEJBException was not thrown after waiting longer than default timeout - " + waitTime + " ms");
        } catch (NoSuchEJBException ex) {
            svLogger.info("Caught expected NoSuchEJBException - PASS");
        }
    }

    /**
     * Tests that container still honors the timeout specified in the
     * ibm-ejb-jar-ext.xml extension file - this is the legacy stateful timeout;
     * it is overridden by the <code>@StatefulTimeout</code> annotation or the
     * stateful-timeout element in the deployment descriptor, but in the absence
     * of these, the container should use the value from the ext file.
     */
    @Test
    @Mode(FULL)
    public void testTimeoutSpecifiedInExtFileOnly() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        TimeoutInExtFileOnlyBean bean = (TimeoutInExtFileOnlyBean) new InitialContext().lookup(JNDI_EXT_FILE_ONLY);
        long invokeTime = bean.getInvocationTime();
        FATHelper.sleep(BUFFER);
        try {
            invokeTime = bean.getInvocationTime();
        } catch (NoSuchEJBException ex) {
            fail("Bean timed out after " + BUFFER + " ms, when the ibm-ejb-jar-ext.xml file specified a 15 second timeout.");
        }

        FATHelper.sleep(USER_TIMEOUT + BUFFER);
        try {
            long elapsedTime = bean.getInvocationTime() - invokeTime;
            fail("Timeout should have occurred after 15 seconds (specified in ibm-ejb-jar-ext.xml file) but did not timeout after waiting " + elapsedTime + " ms.");
        } catch (NoSuchEJBException ex) {
            svLogger.info("Caught expected NoSuchEJBException - PASS");
        }
    }

    /**
     * Tests that the container times out a SFSB using the timeout specified in
     * the <code>@StatefulTimeout</code> annotation (15 seconds).
     */
    @Test
    @Mode(FULL)
    public void testAnnotationOnly() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatefulTimeoutAnnotationOnlyBean bean = (StatefulTimeoutAnnotationOnlyBean) new InitialContext().lookup(JNDI_ANN_ONLY);
        long invokeTime = bean.getInvocationTime();
        FATHelper.sleep(BUFFER);
        try {
            invokeTime = bean.getInvocationTime();
        } catch (NoSuchEJBException ex) {
            fail("Timeout was " + USER_TIMEOUT + " ms, but bean timed out after sleeping " + BUFFER + " ms.");
        }

        FATHelper.sleep(USER_TIMEOUT + BUFFER);
        try {
            long elapsedTime = bean.getInvocationTime() - invokeTime;
            fail("Timeout was " + USER_TIMEOUT + " ms, but bean did not time out after sleeping " + elapsedTime + " ms.");
        } catch (NoSuchEJBException ex) {
            svLogger.info("Caught expected NoSuchEJBException - PASS");
        }
    }

    /**
     * Tests that the container times out a SFSB using the timeout specified in
     * the stateful-timeout stanza in the deployment descriptor (15 seconds).
     */
    @Test
    public void testXMLOnly() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatefulTimeoutXMLOnlyBean bean = (StatefulTimeoutXMLOnlyBean) new InitialContext().lookup(JNDI_XML_ONLY);
        long invokeTime = bean.getInvocationTime();
        FATHelper.sleep(BUFFER);
        try {
            invokeTime = bean.getInvocationTime();
        } catch (NoSuchEJBException ex) {
            fail("Timeout was " + USER_TIMEOUT + " ms, but bean timed out after sleeping " + BUFFER + " ms.");
        }

        FATHelper.sleep(USER_TIMEOUT + BUFFER);
        try {
            long elapsedTime = bean.getInvocationTime() - invokeTime;
            fail("Timeout was " + USER_TIMEOUT + " ms, but bean did not time out after sleeping " + elapsedTime + " ms.");
        } catch (NoSuchEJBException ex) {
            svLogger.info("Caught expected NoSuchEJBException - PASS");
        }
    }

    /**
     * Tests that the container uses the default time unit (minutes) when no
     * unit is specified in the <code>@StatefulTimeout</code> annotation.
     */
    @Test
    @Mode(FULL)
    public void testAnnotationTimeoutWithNoSpecifiedUnit() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatefulTimeoutAnnotationOnlyDefaultUnitBean bean = (StatefulTimeoutAnnotationOnlyDefaultUnitBean) new InitialContext().lookup(JNDI_ANN_DEFAULT_UNIT_BEAN);
        // bean specified timeout is "1" with no unit specified - expecting unit to be Minutes
        long invokeTime = bean.getInvocationTime();
        FATHelper.sleep(BUFFER);
        try {
            invokeTime = bean.getInvocationTime();
        } catch (NoSuchEJBException ex) {
            fail("Expected timeout to be one minute, but bean timed out after " + BUFFER + " ms.");
        }

        FATHelper.sleep(ONE_MIN + BUFFER);
        try {
            long elapsedTime = bean.getInvocationTime() - invokeTime;
            fail("Expected to timeout after waiting one minute, but did not after sleeping " + elapsedTime + " ms.");
        } catch (NoSuchEJBException ex) {
            svLogger.info("Caught expected NoSuchEJBException - PASS");
        }
    }

    /**
     * Tests that the container uses the default time unit (minutes) when no
     * unit is specified in the stateful-timeout stanza in the XML DD.
     */
    @Test
    @Mode(FULL)
    public void testXMLTimeoutWithNoSpecifiedUnit() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatefulTimeoutXMLOnlyDefaultUnitBean bean = (StatefulTimeoutXMLOnlyDefaultUnitBean) new InitialContext().lookup(JNDI_XML_DEFAULT_UNIT_BEAN);
        // bean specified timeout is "1" with no unit specified - expecting unit to be Minutes
        long invokeTime = bean.getInvocationTime();
        FATHelper.sleep(BUFFER);
        try {
            invokeTime = bean.getInvocationTime();
        } catch (NoSuchEJBException ex) {
            fail("Expected timeout to be one minute, but bean timed out after " + BUFFER + " ms.");
        }

        FATHelper.sleep(ONE_MIN + BUFFER);
        try {
            long elapsedTime = bean.getInvocationTime() - invokeTime;
            fail("Expected to timeout after waiting one minute, but did not after sleeping " + elapsedTime + " ms.");
        } catch (NoSuchEJBException ex) {
            svLogger.info("Caught expected NoSuchEJBException - PASS");
        }
    }

    /**
     * Tests the container does not time out SFSBs with a -1 (infinite) timeout
     * in the <code>@StatefulTimeout</code> annotation.
     * NOTE: This test only waits a little longer than the default container
     * timeout setting (10 minutes) rather than waiting for infinity - the test
     * has to end at some point! :-)
     */
    @Test
    @Mode(FULL)
    @SkipForRepeat(SkipForRepeat.EE7_FEATURES)
    public void testNegativeOneValueInAnnotation() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatefulTimeoutNegativeOneAnnotationBean bean = (StatefulTimeoutNegativeOneAnnotationBean) new InitialContext().lookup(JNDI_NEG_ONE_ANN_BEAN);
        long invokeTime = bean.getInvocationTime();
        FATHelper.sleep(EXPECTED_DEFAULT_TIMEOUT + BUFFER);
        try {
            long waitTime = bean.getInvocationTime() - invokeTime;
            svLogger.info("Bean with @StatefulTimeout(-1) has not expired after " + waitTime + " ms. PASS");
        } catch (NoSuchEJBException ex) {
            fail("Bean with @StatefulTimeout(-1) should never time out, but did...");
        }
    }

    /**
     * Tests the container does not time out SFSBs with a -1 (infinite) timeout
     * in the stateful-timeout stanza in the XML DD.
     * NOTE: This test only waits a little longer than the default container
     * timeout setting (10 minutes) rather than waiting for infinity - the test
     * has to end at some point! :-)
     */
    @Test
    @Mode(FULL)
    @SkipForRepeat(SkipForRepeat.EE7_FEATURES)
    public void testNegativeOneValueInXML() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatefulTimeoutNegativeOneXMLBean bean = (StatefulTimeoutNegativeOneXMLBean) new InitialContext().lookup(JNDI_NEG_ONE_XML_BEAN);
        long invokeTime = bean.getInvocationTime();
        FATHelper.sleep(EXPECTED_DEFAULT_TIMEOUT + BUFFER);
        try {
            long waitTime = bean.getInvocationTime() - invokeTime;
            svLogger.info("Bean with <stateful-timeout><timeout>-1</timeout></stateful-timeout> in ejb-jar.xml has not expired after " + waitTime + " ms. PASS");
        } catch (NoSuchEJBException ex) {
            fail("Bean with <stateful-timeout><timeout>-1</timeout></stateful-timeout> in ejb-jar.xml should never time out, but did...");
        }
    }

    /**
     * Tests the container immediately times out the SFSB after it is created
     * when the annotation timeout value is 0.
     */
    @Test
    public void testZeroValueInAnnotation() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatefulTimeoutZeroAnnotationBean bean = (StatefulTimeoutZeroAnnotationBean) new InitialContext().lookup(JNDI_ZERO_ANN_BEAN);
        FATHelper.sleep(16); // zero is not immediate, but rather a 1ms timeout, sleep here to account for that
        try {
            bean.getInvocationTime();
            fail("Bean with @StatefulTimeout(0) did not expire immediately after lookup, but before first invocation");
        } catch (NoSuchEJBException ex) {
            svLogger.info("Caught expected NoSuchEJBException - PASS");
        }
    }

    /**
     * Tests the container immediately times out the SFSB after it is created
     * when the XML DD timeout value is 0.
     */
    @Test
    @Mode(FULL)
    public void testZeroValueInXML() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatefulTimeoutZeroXMLBean bean = (StatefulTimeoutZeroXMLBean) new InitialContext().lookup(JNDI_ZERO_XML_BEAN);
        FATHelper.sleep(16); // zero is not immediate, but rather a 1ms timeout, sleep here to account for that
        try {
            bean.getInvocationTime();
            fail("Bean with <stateful-timeout><timeout>0</timeout></stateful-timeout> in ejb-jar.xml did not expire after lookup, but before first invocation.");
        } catch (NoSuchEJBException ex) {
            svLogger.info("Caught expected NoSuchEJBException - PASS");
        }
    }

    /**
     * Tests the container immediately times out the SFSB after it is created
     * in a global transaction when the annotation timeout value is 0.
     */
    @Test
    @Mode(FULL)
    public void testZeroValueInAnnotationInGlobalTx() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        UserTransaction tran = (UserTransaction) new InitialContext().lookup(JNDI_USER_TRAN);
        StatefulTimeoutZeroAnnotationBean bean;
        try {
            tran.begin();
            bean = (StatefulTimeoutZeroAnnotationBean) new InitialContext().lookup(JNDI_ZERO_ANN_BEAN);
            FATHelper.sleep(16);
            try {
                bean.getInvocationTime();
                fail("Bean with @StatefulTimeout(0) did not expire before first invocation");
            } catch (NoSuchEJBException ex) {
                svLogger.info("Caught expected NoSuchEJBException - PASS");
            }
        } finally {
            tran.commit();
        }
    }

    /**
     * Tests the container immediately times out the SFSB after it is created
     * in a global transaction when the XML DD timeout value is 0.
     */
    @Test
    @Mode(FULL)
    public void testZeroValueInXMLInGlobalTx() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        UserTransaction tran = (UserTransaction) new InitialContext().lookup(JNDI_USER_TRAN);
        StatefulTimeoutZeroXMLBean bean;
        try {
            tran.begin();
            bean = (StatefulTimeoutZeroXMLBean) new InitialContext().lookup(JNDI_ZERO_XML_BEAN);
            FATHelper.sleep(16);
            try {
                bean.getInvocationTime();
                fail("Bean with <stateful-timeout><timeout>0</timeout></stateful-timeout> in ejb-jar.xml did not expire before first invocation");
            } catch (NoSuchEJBException ex) {
                svLogger.info("Caught expected NoSuchEJBException - PASS");
            }
        } finally {
            tran.commit();
        }
    }

    /**
     * Tests that the container logs the CNTR0306W warning when the
     * <code>@StatefulTimeout</code> annotation appears on an interface and
     * when EJBContainer=all tracing is enabled.
     */
    public void testAnnotationOnInterfaceLogsWarning() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        InvalidInterface bean = null;

        try {
            bean = (InvalidInterface) new InitialContext().lookup(JNDI_INVALID_INTERFACE);
            bean.getInvocationTime();
        } finally {
            if (bean != null) {
                bean.remove();
            }
        }
    }

    /**
     * Tests that the container correctly fails to create a bean with a negative
     * timeout value (less than -1) in its annotation. The container should fail
     * the JNDI lookup and log a CNTR0311E error.
     */
    public void testNegativeTwoValueInAnnotationLogsError() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        try {
            new InitialContext().lookup(JNDI_NEG_TWO_ANN_BEAN);
            fail("Bean was allowed to init with a @StatefulTimeout(-2) annotation.");
        } catch (NamingException ex) {
            svLogger.info("Caught expected NamingException");
        }
    }

    /**
     * Tests that the container correctly fails to create a bean with a negative
     * timeout value (less than -1) in the XML DD. The container should fail the
     * JNDI lookup and log a CNTR0311E error.
     */
    public void testNegativeTwoValueInXMLLogsError() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        try {
            new InitialContext().lookup(JNDI_NEG_TWO_XML_BEAN);
            fail("Bean was allowed to init with a -2 stateful timeout in ejb-jar.xml.");
        } catch (NamingException ex) {
            svLogger.info("Caught expected NamingException");
        }
    }

    /**
     * Tests the container uses the value specified in the annotation instead
     * of the value specified in the ext file - since annotation value overrides
     * ext file value.
     */
    @Test
    @Mode(FULL)
    public void testAnnotationOverridesExtFile() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatefulTimeoutAnnotationOverrideExtFileBean bean = (StatefulTimeoutAnnotationOverrideExtFileBean) new InitialContext().lookup(JNDI_ANN_OVERRIDE_EXT_FILE);
        // timeout specified in ibm-ejb-jar-ext.xml is 60 seconds
        // timeout specified in annotation is 15 seconds
        long invokeTime = bean.getInvocationTime();
        FATHelper.sleep(BUFFER);
        try {
            invokeTime = bean.getInvocationTime();
        } catch (NoSuchEJBException ex) {
            fail("Expected timeout to be 15 seconds, but bean timed out after " + BUFFER + " ms.");
        }

        FATHelper.sleep(USER_TIMEOUT + BUFFER);
        try {
            long elapsedTime = bean.getInvocationTime() - invokeTime;
            fail("Timeout was " + USER_TIMEOUT + " ms, but bean did not time out after sleeping " + elapsedTime + " ms.");
        } catch (NoSuchEJBException ex) {
            svLogger.info("Caught expected NoSuchEJBException - PASS");
        }
    }

    /**
     * Tests the container uses the value specified in the XML DD instead of the
     * value specified in the ext file - since annotation value overrides ext
     * file value.
     */
    @Test
    @Mode(FULL)
    public void testXMLOverridesExtFile() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatefulTimeoutXMLOverrideExtFileBean bean = (StatefulTimeoutXMLOverrideExtFileBean) new InitialContext().lookup(JNDI_XML_OVERRIDE_EXT_FILE);
        // timeout specified in ibm-ejb-jar-ext.xml is 60 seconds
        // timeout specified in ejb-jar.xml is 15 seconds
        long invokeTime = bean.getInvocationTime();
        FATHelper.sleep(BUFFER);
        try {
            invokeTime = bean.getInvocationTime();
        } catch (NoSuchEJBException ex) {
            fail("Expected timeout to be 15 seconds, but bean timed out after " + BUFFER + " ms.");
        }

        FATHelper.sleep(USER_TIMEOUT + BUFFER);
        try {
            long elapsedTime = bean.getInvocationTime() - invokeTime;
            fail("Timeout was " + USER_TIMEOUT + " ms, but bean did not time out after sleeping " + elapsedTime + " ms.");
        } catch (NoSuchEJBException ex) {
            svLogger.info("Caught expected NoSuchEJBException - PASS");
        }
    }

    /**
     * This test verifies that the container uses the default timeout (10
     * minutes) when stateful-timeout contains neither a unit nor a timeout
     * element (contrary to the ejb-jar.xml XSD).
     */
    @Test
    @Mode(FULL)
    @SkipForRepeat(SkipForRepeat.EE7_FEATURES)
    public void testNoTimeoutStanzaInStatefulTimeoutXML() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatefulTimeoutXMLNoTimeoutStanzaBean bean = (StatefulTimeoutXMLNoTimeoutStanzaBean) new InitialContext().lookup(JNDI_XML_NO_TIMEOUT_STANZA);
        assertNotNull("Failed to lookup StatefulTimeoutXMLNoTimeoutStanzaBean", bean);

        long invokeTime = bean.getInvocationTime();
        FATHelper.sleep(BUFFER);
        try {
            invokeTime = bean.getInvocationTime();
        } catch (NoSuchEJBException ex) {
            fail("Bean with <stateful-timeout /> timed out after " + BUFFER + " ms, when the default timeout is 10 minutes.");
        }

        FATHelper.sleep(EXPECTED_DEFAULT_TIMEOUT + BUFFER);
        try {
            long waitTime = bean.getInvocationTime() - invokeTime;
            fail("Expected NoSuchEJBException was not thrown after waiting longer than default timeout - " + waitTime + " ms");
        } catch (NoSuchEJBException ex) {
            svLogger.info("Caught expected NoSuchEJBException - PASS");
        }
    }

    /**
     * This test verifies that the container uses the default timeout (10
     * minutes) when stateful-timeout contains a unit element but not a timeout
     * element (contrary to the ejb-jar.xml XSD).
     */
    @Test
    @Mode(FULL)
    @SkipForRepeat(SkipForRepeat.EE7_FEATURES)
    public void testUnitButNoTimeoutSpecifiedInStatefulTimeoutXML() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatefulTimeoutXMLUnitButNoTimeoutBean bean = (StatefulTimeoutXMLUnitButNoTimeoutBean) new InitialContext().lookup(JNDI_XML_UNIT_NO_TIMEOUT_STANZA);
        assertNotNull("Failed to lookup StatefulTimeoutXMLUnitButNoTimeoutBean", bean);

        long invokeTime = bean.getInvocationTime();
        FATHelper.sleep(BUFFER);
        try {
            invokeTime = bean.getInvocationTime();
        } catch (NoSuchEJBException ex) {
            fail("Bean with <unit>Seconds</unit> but no <timeout> stanza timed out after " + BUFFER + " ms, when the default timeout is 10 minutes.");
        }

        FATHelper.sleep(EXPECTED_DEFAULT_TIMEOUT + BUFFER);
        try {
            long waitTime = bean.getInvocationTime() - invokeTime;
            fail("Expected NoSuchEJBException was not thrown after waiting longer than default timeout - " + waitTime + " ms");
        } catch (NoSuchEJBException ex) {
            svLogger.info("Caught expected NoSuchEJBException - PASS");
        }
    }

    /**
     * Tests that the container logs a CNTR0304W warning when a stateless bean
     * contains a <code>@StatefulTimeout</code> annotation (and EJBContainer
     * trace is enabled).
     */
    public void testLogWarningOnStatefulTimeoutOnStatelessBeanAnnotation() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatelessWithStatefulTimeoutAnnotationBean bean = (StatelessWithStatefulTimeoutAnnotationBean) new InitialContext().lookup(JNDI_STATELESS_WITH_ANN_TIMEOUT);
        bean.getInvocationTime();
    }

    /**
     * Tests that the container logs a CNTR0310W warning when a stateless bean
     * is configured with a stateful-timeout element in the XML DD (and
     * EJBContainer trace is enabled).
     */
    public void testLogWarningOnStatefulTimeoutOnStatelessBeanXML() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatelessWithStatefulTimeoutXMLBean bean = (StatelessWithStatefulTimeoutXMLBean) new InitialContext().lookup(JNDI_STATELESS_WITH_XML_TIMEOUT);
        bean.getInvocationTime();
    }

    /**
     * Tests that the container fails to start a SFSB that is configured with
     * too high of a stateful-timeout value in the XML DD. A value of
     * 999999999999 days converts to a long integer that is greater than MAX
     * LONG.
     */
    public void testXMLTimeoutSpecifiedOverflow() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        try {
            new InitialContext().lookup(JNDI_XML_TIMEOUT_OVERFLOW);
            fail("Container did not fail to start bean with super high timeout (999999999999 Days)");
        } catch (NamingException ex) {
            svLogger.info("Caught expected NamingException");
        }
    }

    /**
     * Tests that the container resets the SFSB timeout timer after the
     * execution of an asynchronous method.
     */
    @Test
    @Mode(FULL)
    public void testAsyncMethodResetsTimeoutTimer() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatefulTimeoutAsyncBean bean = (StatefulTimeoutAsyncBean) new InitialContext().lookup(JNDI_ASYNC);
        Future<Long> f = bean.async(-1); // no sleep time
        f.get();
        FATHelper.sleep(BUFFER);
        try {
            bean.sync();
        } catch (NoSuchEJBException ex) {
            fail("Bean timed out too early after async method invocation");
        }
        bean.remove(false);

        bean = (StatefulTimeoutAsyncBean) new InitialContext().lookup(JNDI_ASYNC);
        f = bean.async(-1); // no sleep time again
        f.get();
        FATHelper.sleep(USER_TIMEOUT + BUFFER);
        try {
            bean.sync();
            fail("Bean with @StatefulTimeout(15 seconds) did not time out 25 seconds after async method invocation.");
        } catch (NoSuchEJBException ex) {
            svLogger.info("Caught expected NoSuchEJBException - PASS");
        }
    }

    /**
     * Tests that the container resets the SFSB timeout timer during post-invoke
     * of an asynchronous method, and not during pre-invoke. This is tested by
     * ensuring that the asynchronous method takes longer to execute than the
     * SFSB timeout.
     */
    @Test
    @Mode(FULL)
    public void testTimeoutTimerResetsAftersAsyncMethodExits() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatefulTimeoutAsyncBean bean = (StatefulTimeoutAsyncBean) new InitialContext().lookup(JNDI_ASYNC);
        Future<Long> f = bean.async(25 * 1000); // sleep inside async method for 25 seconds
        try {
            f.get();
        } catch (InterruptedException ex) {
            svLogger.logp(Level.SEVERE, CLASSNAME, "testTimeoutTimerResetsAftersAsyncMethodExits",
                          "Future<Long>.get received an InterruptedException", ex);
            throw ex;
        } catch (ExecutionException ex) {
            svLogger.logp(Level.SEVERE, CLASSNAME, "testTimeoutTimerResetsAftersAsyncMethodExits",
                          "Future<Long>.get received an ExecutionException", ex);
            if (ex.getCause() instanceof NoSuchEJBException) {
                fail("NoSuchEJBException throw in async method that took longer than the stateful timeout to execute");
            } else {
                throw ex;
            }
        }

        try {
            bean.sync();
        } catch (NoSuchEJBException ex) {
            fail("Bean timed out too early after async method completed invocation");
        }
    }

    /**
     * Tests that the container resets the timeout timer after a Remove method
     * with retainIfException is true is invoked and throws an exception.
     */
    @Test
    @Mode(FULL)
    public void testStatefulBeanTimeoutAfterRemoveWithException() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatefulTimeoutAsyncBean bean = (StatefulTimeoutAsyncBean) new InitialContext().lookup(JNDI_ASYNC);

        try {
            bean.remove(true); // expect an app exception
        } catch (MyAppException ex) {
            svLogger.info("Caught expected app exception - msg: " + ex.getMessage());
        }

        try {
            bean.sync();
        } catch (NoSuchEJBException ex) {
            fail("Bean with 15 second stateful timeout timed out immediately after invoking a remove method that threw an exception - should have waited 15 seconds");
        }

        bean = (StatefulTimeoutAsyncBean) new InitialContext().lookup(JNDI_ASYNC);
        try {
            bean.remove(true); // expect an app exception
        } catch (MyAppException ex) {
            svLogger.info("Caught expected app exception - msg: " + ex.getMessage());
        }

        FATHelper.sleep(USER_TIMEOUT + BUFFER);
        try {
            bean.sync();
            fail("Bean with 15 second stateful timeout did not time out 25 seconds after invoking a remove method that threw an exception");
        } catch (NoSuchEJBException ex) {
            svLogger.info("Caught expected NoSuchEJBException - PASS");
        }
    }

    /**
     * Tests that the container resets the timeout timer after an asynchronous
     * Remove method with retainIfException is true is invoked and throws an
     * exception.
     */
    @Test
    @Mode(FULL)
    public void testStatefulBeanTimeoutAfterAsyncRemoveWithException() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatefulTimeoutAsyncBean bean = (StatefulTimeoutAsyncBean) new InitialContext().lookup(JNDI_ASYNC);
        Future<String> f = bean.removeAsync(true); // expect a runtime exception - client will never see it since it is fire and forget
        try {
            f.get();
            fail("Pre-condition failed - async method threw exception but future.get() did not throw an ExecutionException");
        } catch (ExecutionException ex) {
            svLogger.info("Caught expected ExecutionException");
        }

        try {
            bean.remove(false);
        } catch (NoSuchEJBException ex) {
            fail("Bean with 15 second stateful timeout timed out immediately after invoking an async remove method that threw an exception - should have waited 15 seconds");
        }

        bean = (StatefulTimeoutAsyncBean) new InitialContext().lookup(JNDI_ASYNC);
        bean.removeAsync(true); // expect a runtime exception - client will never see it since it is fire and forget

        FATHelper.sleep(USER_TIMEOUT + BUFFER);

        try {
            bean.remove(false);
            fail("Bean with 15 second stateful timeout did not time out 25 seconds after invoking an async remove method that threw an exception");
        } catch (NoSuchEJBException ex) {
            svLogger.info("Caught expected NoSuchEJBException - PASS");
        }
    }

    /**
     * Tests that if an asynchronous fire-and-forget method throws an unchecked
     * exception, that the bean is removed immediately.
     */
    @Test
    @Mode(FULL)
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    public void testUncheckedExceptionInAsyncFAFMethodRemovesBean() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatefulTimeoutAsyncBean bean = (StatefulTimeoutAsyncBean) new InitialContext().lookup(JNDI_ASYNC);
        bean.throwUncheckedExceptionInFAFAsyncMethod();
        FATHelper.sleep(2000);
        try {
            bean.sync();
            fail("Unchecked exception in fire-and-forget async method did not remove the stateful bean.");
        } catch (NoSuchEJBException ex) {
            svLogger.info("Caught expected NoSuchEJBException - PASS");
        }
    }

    /**
     * Tests that if an asynchronous fire-and-forget Remove method throws an
     * unchecked exception, that the bean is removed immediately.
     */
    @Test
    @Mode(FULL)
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    public void testUncheckedExceptionInAsyncFAFRemoveMethodRemovesBean() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatefulTimeoutAsyncBean bean = (StatefulTimeoutAsyncBean) new InitialContext().lookup(JNDI_ASYNC);
        bean.throwUncheckedExceptionInFAFAsyncRemoveMethod();
        FATHelper.sleep(2000);
        try {
            bean.sync();
            fail("Unchecked exception in fire-and-forget async remove method did not remove the stateful bean.");
        } catch (NoSuchEJBException ex) {
            svLogger.info("Caught expected NoSuchEJBException - PASS");
        }
    }

    /**
     * Tests that if an asynchronous fire-and-forget Remove (with retain if
     * exception = true) method throws an unchecked exception, that the bean is
     * removed immediately.
     */
    @Test
    @Mode(FULL)
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    public void testUncheckedExceptionInAsyncFAFRemoveWithRetainMethodRemovesBean() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatefulTimeoutAsyncBean bean = (StatefulTimeoutAsyncBean) new InitialContext().lookup(JNDI_ASYNC);
        bean.throwUncheckedExceptionInFAFAsyncRemoveWithRetainMethod();
        FATHelper.sleep(2000);
        try {
            bean.sync();
            fail("Unchecked exception in fire-and-forget async remove method did not remove the stateful bean.");
        } catch (NoSuchEJBException ex) {
            svLogger.info("Caught expected NoSuchEJBException - PASS");
        }
    }

    /**
     * Tests that if an asynchronous fire-and-return method throws an unchecked
     * exception, that the bean is removed immediately.
     */
    @Test
    @Mode(FULL)
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    public void testUncheckedExceptionInAsyncFARMethodRemovesBean() throws Exception {
        if (!recLogInit)
            initRecoveryLog();

        StatefulTimeoutAsyncBean bean = (StatefulTimeoutAsyncBean) new InitialContext().lookup(JNDI_ASYNC);
        Future<String> f = bean.throwUncheckedExceptionInFARAsyncMethod();
        try {
            f.get();
            fail("Pre-condition failed - bean method should have thrown exception so Future.get() should have thrown ExecutionException");
        } catch (ExecutionException ex) {
            svLogger.info("Caught expected ExecutionException");
        }

        try {
            bean.sync();
            fail("Unchecked exception in fire-and-return async method did not remove the stateful bean.");
        } catch (NoSuchEJBException ex) {
            svLogger.info("Caught expected NoSuchEJBException - PASS");
        }
    }
}