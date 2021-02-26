/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejblite.interceptor.v32.mix.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.NoSuchEJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejblite.interceptor.v32.mix.ejb.CompStatefulEJBLocalHome;
import com.ibm.ejblite.interceptor.v32.mix.ejb.CompStatefulLocal;
import com.ibm.ejblite.interceptor.v32.mix.ejb.CompStatelessEJBLocalHome;
import com.ibm.ejblite.interceptor.v32.mix.ejb.CompStatelessLocal;
import com.ibm.ejblite.interceptor.v32.mix.ejb.ResultsLocal;
import com.ibm.ejblite.interceptor.v32.mix.ejb.ResultsLocalBean;
import com.ibm.ejblite.interceptor.v32.mix.ejb.SFUnspecifiedLocal;
import com.ibm.ejblite.interceptor.v32.mix.ejb.SLUnspecifiedLocal;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> UnspecifiedContextTest.
 *
 * <dt><b>EJB 3 core specification sections tested:</b>
 * <dd>"4.3.5 The Optional SessionBean Interface", "4.3.10.1 Stateful Session Beans",
 * "4.4.1 Operations Allowed in the Methods of a Stateful Session Bean Class",
 * and "12.4 Interceptors for LifeCycle Event Callbacks"
 *
 * <dt><b>Test Description:</b>
 * <dd>In section "4.3.5 The Optional SessionBean Interface", it is stated that the ejbRemove,
 * ejbActivate, and ejbPassivate methods of the SessionBean interface, and the ejbCreate
 * method of a stateless session bean be treated as PreDestroy, PostActivate, PrePassivate
 * and PostConstruct life cycle callback interceptor methods, respectively. In
 * section "4.3.10.1 Stateful Session Beans" it is stated that the ejbCreate<METHOD> of
 * a SFSB written to EJB 2.1 or earlier APIs (e.g. the SessionBean interface) be
 * considered a Init method, not a PostConstruct method. Section "12.4 Interceptors for
 * LifeCycle Event Callbacks" requires that all lifecycle callback event interceptor
 * methods execute in a unspecified transaction and security context. This is also
 * true of the Init method of a SFSB as described in "4.4.1 Operations Allowed in the
 * Methods of a Stateful Session Bean Class".
 * <p>
 * The purpose of this testcase is to verify that all of the lifecycle event callback
 * interceptor methods and the Init method of a SFSB all execute in a unspecified
 * transaction context as required by the reference sections of the EJB 3 core
 * specification. This will be done for both SLSB and SFSB and for the case
 * where the session bean is written to EJB 2.1 or earlier API and when it is not
 * written to EJB 2.1 or earlier API (implements SessionBean interface and does not
 * implement SessionBean interface).
 * <p>
 * <dt><b>Test Matrix:</b>
 * <dt>Test Matrix:
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testSLSBCallBackNOTImplSB verify SLSB lifecycle callback methods execute in unspecified context
 * when bean does NOT implement SessionBean.
 * <li>testSFSBCallBackNOTImplSB verify SFSB lifecycle callback methods execute in
 * unspecified context when bean does NOT implement SessionBean.
 * <li>testSFSBCallBackDoesImplSB verify SLSB lifecycle callback methods execute in unspecified context
 * when bean does implement SessionBean.
 * <li>testSLSBCallBackDoesImplSB verify SLSB lifecycle callback methods execute in
 * unspecified context when bean does implement SessionBean.
 * <li>testSLSFSBCallBackInUnspecifiedCtxt verify SF and SL SB lifecycle callback methods execute in
 * unspecified context rather than in the caller's global TX that is
 * started when the methods of SF and SL SB are invoked.
 * <li>testSLSFSBCallBackInUnspecifiedCtxt2 verify SF and SL SB lifecycle callback methods execute in
 * unspecified context rather than in the caller's local TX that is
 * started when the methods of SF and SL SB are invoked.
 * </ul>
 * </dl>
 */
@WebServlet("/UnspecifiedContextMixServlet")
public class UnspecifiedContextMixServlet extends FATServlet {
    private static final long serialVersionUID = 7133638203504328737L;
    private static final String LOGGER_CLASS_NAME = UnspecifiedContextMixServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(LOGGER_CLASS_NAME);

    /**
     * Reference to ResultsLocal bean instance.
     */
    private static ResultsLocal ivResultsLocal;

    private final static String SF_JNDI_NAME = "java:app/EJB3INTMBean/SFUnspecifiedContextBean!com.ibm.ejblite.interceptor.v32.mix.ejb.SFUnspecifiedLocal"; // TJB: Task35

    private final static String SL_JNDI_NAME = "java:app/EJB3INTMBean/SLUnspecifiedContextBean!com.ibm.ejblite.interceptor.v32.mix.ejb.SLUnspecifiedLocal"; // TJB: Task35

    private final static String COMP_SF_HOME_JNDI_NAME = "java:app/EJB3INTMBean/CompStatefulLocalBean!com.ibm.ejblite.interceptor.v32.mix.ejb.CompStatefulEJBLocalHome";

    private final static String COMP_SL_HOME_JNDI_NAME = "java:app/EJB3INTMBean/CompStatelessLocalBean!com.ibm.ejblite.interceptor.v32.mix.ejb.CompStatelessEJBLocalHome";

    private static final String[] SL_UNSPECIFIED_TRAN_POST_CONSTRUCT = new String[] { "UnspecifiedInterceptor.postConstruct:true", "SLUnspecifiedContextBean.postConstruct:true" };

    private static final String[] SL_UNSPECIFIED_TRAN_PRE_DESTROY = new String[] { "UnspecifiedInterceptor.postConstruct:true", "SLUnspecifiedContextBean.postConstruct:true",
                                                                                   "UnspecifiedInterceptor.preDestroy:true", "SLUnspecifiedContextBean.preDestroy:true", };

    private static final String[] SF_UNSPECIFIED_TRAN_POST_CONSTRUCT = new String[] { "UnspecifiedInterceptor.postConstruct:true", "SFUnspecifiedContextBean.postConstruct:true",
                                                                                      "UnspecifiedInterceptor.prePassivate:true", "SFUnspecifiedContextBean.prePassivate:true" };

    private static final String[] SF_UNSPECIFIED_TRAN_DO_NOTHING = new String[] { "UnspecifiedInterceptor.postActivate:true", "SFUnspecifiedContextBean.postActivate:true",
                                                                                  "UnspecifiedInterceptor.prePassivate:true", "SFUnspecifiedContextBean.prePassivate:true" };

    private static final String[] SF_UNSPECIFIED_TRAN_PRE_DESTROY = new String[] { "UnspecifiedInterceptor.postActivate:true", "SFUnspecifiedContextBean.postActivate:true",
                                                                                   "UnspecifiedInterceptor.preDestroy:true", "SFUnspecifiedContextBean.preDestroy:true" };
    private static final String[] COMP_SF_UNSPECIFIED_TRAN_POST_CONSTRUCT = new String[] { "UnspecifiedInterceptor.postConstruct:true", "CompStatefulLocalBean.postConstruct:true",
                                                                                           "UnspecifiedInterceptor.prePassivate:true", "CompStatefulLocalBean.prePassivate:true" };

    private static final String[] COMP_SF_UNSPECIFIED_TRAN_DO_NOTHING = new String[] { "UnspecifiedInterceptor.postActivate:true", "CompStatefulLocalBean.postActivate:true",
                                                                                       "UnspecifiedInterceptor.prePassivate:true", "CompStatefulLocalBean.prePassivate:true" };

    private static final String[] COMP_SF_UNSPECIFIED_TRAN_PRE_DESTROY = new String[] { "UnspecifiedInterceptor.postActivate:true", "CompStatefulLocalBean.postActivate:true",
                                                                                        "UnspecifiedInterceptor.preDestroy:true", "CompStatefulLocalBean.preDestroy:true" };

    private static final String[] COMP_SL_UNSPECIFIED_TRAN_POST_CONSTRUCT = new String[] { "UnspecifiedInterceptor.postConstruct:true",
                                                                                           "CompStatelessLocalBean.postConstruct:true" };

    private static final String[] COMP_SL_UNSPECIFIED_TRAN_PRE_DESTROY = new String[] { "UnspecifiedInterceptor.postConstruct:true", "CompStatelessLocalBean.postConstruct:true",
                                                                                        "UnspecifiedInterceptor.preDestroy:true", "CompStatelessLocalBean.preDestroy:true" };

    private static final String[] COMP_SF_UNSPECIFIED_TRAN_TX_REQUIRED_LOOKUP = new String[] { "UnspecifiedInterceptor.postActivate:true",
                                                                                               "CompStatefulLocalBean.postActivate:true",
                                                                                               "UnspecifiedInterceptor.postConstruct:true",
                                                                                               "SFUnspecifiedContextBean.postConstruct:true",
                                                                                               "UnspecifiedInterceptor.prePassivate:true",
                                                                                               "SFUnspecifiedContextBean.prePassivate:true",
                                                                                               "UnspecifiedInterceptor.postActivate:true",
                                                                                               "SFUnspecifiedContextBean.postActivate:true",
                                                                                               "UnspecifiedInterceptor.postConstruct:true",
                                                                                               "SLUnspecifiedContextBean.postConstruct:true",
                                                                                               "UnspecifiedInterceptor.postConstruct:true",
                                                                                               "SLUnspecifiedContextBean.postConstruct:true",
                                                                                               "UnspecifiedInterceptor.preDestroy:true", "SLUnspecifiedContextBean.preDestroy:true",
                                                                                               "UnspecifiedInterceptor.prePassivate:true",
                                                                                               "CompStatefulLocalBean.prePassivate:true",
                                                                                               "UnspecifiedInterceptor.prePassivate:true",
                                                                                               "SFUnspecifiedContextBean.prePassivate:true" };

    private static final String[] COMP_SF_UNSPECIFIED_TRAN_TX_NOT_SUPPORTED_LOOKUP = new String[] { "UnspecifiedInterceptor.postActivate:true",
                                                                                                    "CompStatefulLocalBean.postActivate:true",
                                                                                                    "UnspecifiedInterceptor.postConstruct:true",
                                                                                                    "SFUnspecifiedContextBean.postConstruct:true",
                                                                                                    "UnspecifiedInterceptor.prePassivate:true",
                                                                                                    "SFUnspecifiedContextBean.prePassivate:true",
                                                                                                    "UnspecifiedInterceptor.postActivate:true",
                                                                                                    "SFUnspecifiedContextBean.postActivate:true",
                                                                                                    "UnspecifiedInterceptor.prePassivate:true",
                                                                                                    "SFUnspecifiedContextBean.prePassivate:true",
                                                                                                    "UnspecifiedInterceptor.postActivate:true",
                                                                                                    "SFUnspecifiedContextBean.postActivate:true",
                                                                                                    "UnspecifiedInterceptor.preDestroy:true",
                                                                                                    "SFUnspecifiedContextBean.preDestroy:true",
                                                                                                    "UnspecifiedInterceptor.postConstruct:true",
                                                                                                    "SLUnspecifiedContextBean.postConstruct:true",
                                                                                                    "UnspecifiedInterceptor.postConstruct:true",
                                                                                                    "SLUnspecifiedContextBean.postConstruct:true",
                                                                                                    "UnspecifiedInterceptor.preDestroy:true",
                                                                                                    "SLUnspecifiedContextBean.preDestroy:true",
                                                                                                    "UnspecifiedInterceptor.prePassivate:true",
                                                                                                    "CompStatefulLocalBean.prePassivate:true" };

    public void setUp() throws Exception {
        ivResultsLocal = ResultsLocalBean.setupSFBean();
    }

    public void tearDown() throws Exception {
        if (ivResultsLocal != null) {
            ivResultsLocal.remove();
        }
    }

    /**
     * Ensure SLSB pool is empty for next test variation by calling the
     * discard method to force the SLSB that was in the pool to be
     * discarded.
     */
    private void resetSLUnspecifiedLocal() {
        try {
            InitialContext ictx = new InitialContext();
            final SLUnspecifiedLocal bean = (SLUnspecifiedLocal) ictx.lookup(SL_JNDI_NAME);
            bean.discard();
        } catch (Throwable t) {
            // Just eat it.
        }
    }

    /**
     * For a EJB 3 SLSB that does not implement SessionBean interface, verify the
     * PostConstruct and PreDestroy lifecycle callback event interceptors execute
     * in unspecified TX context as required by the EJB 3 spec.
     */
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    @Test
    public void testSLSBCallBackNOTImplSB_Mix() throws Exception {
        setUp();
        SLUnspecifiedLocal bean = null;
        try {
            // --------------------------------------------------------------------
            // Locate SFSB Local Business Interface Factory and execute the test
            // --------------------------------------------------------------------
            InitialContext ictx = new InitialContext();
            bean = (SLUnspecifiedLocal) ictx.lookup(SL_JNDI_NAME);
            assertNotNull("1 ---> SLSB created successfully.", bean);

            // Invoke a doNothing business method.
            boolean expectedTranContext = bean.doNothing();
            assertTrue("2 ---> SLSB doNothing ran in expected tran context.", expectedTranContext);

            // Verify post activate results.
            List<String> expectedList = Arrays.asList(SL_UNSPECIFIED_TRAN_POST_CONSTRUCT);
            ArrayList<String> actualList = ivResultsLocal.getTransactionContextData();
            svLogger.info("expected post construct transaction context data: " + expectedList);
            svLogger.info("  actual post construct transaction context data: " + actualList);
            assertEquals("3 ---> post construct transaction context data", expectedList, actualList);

            // Clear Results lists.
            ivResultsLocal.clearLists();

            // Invoke a doNothing business method.
            expectedTranContext = bean.doNothing(bean);
            assertTrue("4 ---> SLSB doNothing ran in expected tran context.", expectedTranContext);

            // Verify pre destroy results.
            expectedList = Arrays.asList(SL_UNSPECIFIED_TRAN_PRE_DESTROY);
            actualList = ivResultsLocal.getTransactionContextData();
            svLogger.info("expected pre destroy transaction context data: " + expectedList);
            svLogger.info("  actual pre destroy transaction context data: " + actualList);
            // This assertion expects that the server has been configured to limit the bean pool size to 1.
            // If the pool size is not set to 1, the predestroy methods won't show up in the "actualList".
            // Liberty sets this through a jvm.options file for the test server.
            assertEquals("5 ---> pre destroy transaction context data", expectedList, actualList);
        } finally {
            // reset SLSB pool
            resetSLUnspecifiedLocal();
            tearDown();
        }
    }

    /**
     * For a EJB 3 SFSB that does not implement SessionBean interface, verify the PostConstruct,
     * PostActivate, PrePassivate, and PreDestroy lifecycle callback event interceptors
     * execute in unspecified TX context as required by the EJB 3 spec.
     */
    @Test
    public void testSFSBCallBackNOTImplSB_Mix() throws Exception {
        setUp();
        // --------------------------------------------------------------------
        // Locate SFSB Local Business Interface Factory and execute the test
        // --------------------------------------------------------------------
        InitialContext ictx = new InitialContext();
        final SFUnspecifiedLocal bean = (SFUnspecifiedLocal) ictx.lookup(SF_JNDI_NAME);
        assertNotNull("1 ---> SFSB created successfully.", bean);

        // Verify post activate results.
        List<String> expectedList = Arrays.asList(SF_UNSPECIFIED_TRAN_POST_CONSTRUCT);
        ArrayList<String> actualList = ivResultsLocal.getTransactionContextData();
        svLogger.info("expected post construct transaction context data: " + expectedList);
        svLogger.info("  actual post construct transaction context data: " + actualList);
        assertEquals("2 ---> post construct transaction context data", expectedList, actualList);

        // Clear Results lists.
        ivResultsLocal.clearLists();

        // Invoke a doNothing business method.
        boolean expectedTranContext = bean.doNothing();
        assertTrue("3 ---> SFSB doNothing ran in expected tran context.", expectedTranContext);

        expectedList = Arrays.asList(SF_UNSPECIFIED_TRAN_DO_NOTHING);
        actualList = ivResultsLocal.getTransactionContextData();
        svLogger.info("expected post activate and pre passivate transaction context data: " + expectedList);
        svLogger.info("  actual post activate and pre passivate transaction context data: " + actualList);
        assertEquals("4 ---> post activate/pre passivate transaction context data", expectedList, actualList);

        // Clear Results lists.
        ivResultsLocal.clearLists();

        // Invoke a doNothing business method.
        expectedTranContext = bean.remove();
        assertTrue("5 ---> SLSB remove ran in expected tran context.", expectedTranContext);

        // Verify pre destroy results.
        expectedList = Arrays.asList(SF_UNSPECIFIED_TRAN_PRE_DESTROY);
        actualList = ivResultsLocal.getTransactionContextData();
        svLogger.info("expected pre destroy transaction context data: " + expectedList);
        svLogger.info("  actual pre destroy transaction context data: " + actualList);
        assertEquals("6 ---> pre destroy transaction context data", expectedList, actualList);

        // Verify we get NoSuchEJBException since bean was destroyed.
        try {
            bean.doNothing();
            fail("7 ---> expected NoSuchEJBException did not occur after @Remove method invoked");
        } catch (NoSuchEJBException ex) {
            svLogger.info("7 ---> NoSuchEJBException occured as expected");
        }
        tearDown();
    }

    /**
     * For a EJB 3 SFSB that does implement SessionBean interface (e.g a 2.1 SFSB in a EJB 3 module),
     * the ejbCreate, ejbActivate, ejbPassivate, and ejbRemove container callback
     * methods are executed in unspecified TX context as required by the EJB 3 spec. The EJB 3 spec
     * requires that the ejbCreate be considered an Init method for this case and the other methods be
     * considered PostActivate, PrePassivate, and PreDestroy despite none of the methods being annotated
     * as such.
     */
    @Test
    public void testSFSBCallBackDoesImplSB_Mix() throws Exception {
        setUp();
        // --------------------------------------------------------------------
        // Locate SFSB Local Business Interface Factory and execute the test
        // --------------------------------------------------------------------
        InitialContext ictx = new InitialContext();
        final CompStatefulEJBLocalHome home = (CompStatefulEJBLocalHome) ictx.lookup(COMP_SF_HOME_JNDI_NAME);
        assertNotNull("1 ---> lookup of SFSB local home successful.", home);

        final CompStatefulLocal bean = home.create();
        assertNotNull("2 ---> SFSB create successful.", bean);

        // Verify post activate results.
        List<String> expectedList = Arrays.asList(COMP_SF_UNSPECIFIED_TRAN_POST_CONSTRUCT);
        ArrayList<String> actualList = ivResultsLocal.getTransactionContextData();
        svLogger.info("expected post construct transaction context data: " + expectedList);
        svLogger.info("  actual post construct transaction context data: " + actualList);
        assertEquals("3 ---> post construct transaction context data", expectedList, actualList);

        // Clear Results lists.
        ivResultsLocal.clearLists();

        // Invoke a doNothing business method.
        boolean expectedTranContext = bean.doNothing();
        assertTrue("4 ---> SFSB doNothing ran in expected tran context.", expectedTranContext);

        expectedList = Arrays.asList(COMP_SF_UNSPECIFIED_TRAN_DO_NOTHING);
        actualList = ivResultsLocal.getTransactionContextData();
        svLogger.info("expected post activate and pre passivate transaction context data: " + expectedList);
        svLogger.info("  actual post activate and pre passivate transaction context data: " + actualList);
        assertEquals("5 ---> post activate/pre passivate transaction context data", expectedList, actualList);

        // Clear Results lists.
        ivResultsLocal.clearLists();

        // Invoke remove method.
        bean.remove();

        // Verify pre destroy results.
        expectedList = Arrays.asList(COMP_SF_UNSPECIFIED_TRAN_PRE_DESTROY);
        actualList = ivResultsLocal.getTransactionContextData();
        svLogger.info("expected pre destroy transaction context data: " + expectedList);
        svLogger.info("  actual pre destroy transaction context data: " + actualList);
        assertEquals("6 ---> pre destroy transaction context data", expectedList, actualList);

        // Verify we catch NoSuchObjectLocalException since bean was
        // destroyed and we are using 2.1 SessionBean view.
        try {
            bean.doNothing();
            fail("7 ---> expected NoSuchObjectLocalException did not occur after remove method invoked");
        } catch (NoSuchObjectLocalException ex) {
            svLogger.info("7 ---> NoSuchObjectLocalException occured as expected");
        }
        tearDown();
    }

    /**
     * For a EJB 3 SLSB that does implement SessionBean interface (e.g a 2.1 SLSB in a EJB 3 module),
     * the ejbCreate, ejbActivate, ejbPassivate, and ejbRemove container callback methods are executed in
     * unspecified TX context as required by the EJB 3 spec. The EJB 3 spec requires that the ejbCreate
     * be considered a PostConstruct method for this case and the other methods be considered
     * PostActivate, PrePassivate, and PreDestroy despite none of the methods being annotated as such.
     */
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    @Test
    public void testSLSBCallBackDoesImplSB_Mix() throws Exception {
        setUp();
        CompStatelessLocal bean = null;
        try {
            // --------------------------------------------------------------------
            // Locate SFSB Local Business Interface Factory and execute the test
            // --------------------------------------------------------------------
            InitialContext ictx = new InitialContext();
            final CompStatelessEJBLocalHome home = (CompStatelessEJBLocalHome) ictx.lookup(COMP_SL_HOME_JNDI_NAME);
            assertNotNull("1 ---> lookup of SLSB local home successful.", home);

            bean = home.create();
            assertNotNull("2 ---> SLSB create successful.", bean);

            // Invoke a doNothing business method since create is actually deferred
            // until first method call.
            boolean expectedTranContext = bean.doNothing();
            assertTrue("3 ---> SLSB doNothing ran in expected tran context.", expectedTranContext);

            // Verify post construct results.
            List<String> expectedList = Arrays.asList(COMP_SL_UNSPECIFIED_TRAN_POST_CONSTRUCT);
            ArrayList<String> actualList = ivResultsLocal.getTransactionContextData();
            svLogger.info("expected post construct transaction context data: " + expectedList);
            svLogger.info("  actual post construct transaction context data: " + actualList);
            assertEquals("4 ---> post construct transaction context data", expectedList, actualList);

            // Clear Results lists.
            ivResultsLocal.clearLists();

            // Invoke a doNothing business method and verify not lifecycle event callbacks
            // occured since bean obtained from pool and activate/passivate does not
            // occur for SLSB.
            expectedTranContext = bean.doNothing();
            assertTrue("5 ---> SLSB doNothing ran in expected tran context.", expectedTranContext);

            actualList = ivResultsLocal.getTransactionContextData();
            svLogger.info("  actual transaction context data: " + actualList);
            assertTrue("6 ---> transaction context data is empty", actualList.isEmpty());

            // Clear Results lists.
            ivResultsLocal.clearLists();

            // Invoke method that causes 2nd instance to be created, but not returned to
            // pool since pool size is limited to 1. Which means we should see
            // both postConstruct and preDestroy being called for this 2nd instance.
            expectedTranContext = bean.doNothing(bean);
            assertTrue("7 ---> SLSB doNothing ran in expected tran context.", expectedTranContext);

            // Verify pre destroy results.
            expectedList = Arrays.asList(COMP_SL_UNSPECIFIED_TRAN_PRE_DESTROY);
            actualList = ivResultsLocal.getTransactionContextData();
            svLogger.info("expected pre destroy transaction context data: " + expectedList);
            svLogger.info("  actual pre destroy transaction context data: " + actualList);
            assertEquals("8 ---> pre destroy transaction context data", expectedList, actualList);

        } finally {
            // Ensure SLSB created is discarded so that pool is returned to the
            // empty state in case we run this test more than once without stopping
            // the server.
            if (bean != null) {
                try {
                    bean.discard();
                } catch (Throwable t) {
                    // eat it.
                }
            }
            tearDown();
        }
    }

    /**
     * Create CompStatefulLocal instance and invoke a method annotated to with the REQUIRED TX attribute.
     * This business method then does a JNDI lookup of SFUnspecifiedLocal, which causes PostConstruct
     * followed by PrePassivate of the SFUnspecifiedLocal bean to be invoked. CompStatefulLocal calls
     * doNothing method, which causes PostActivate and PrePassivate to occur on SFUnspecifiedLocal. Since
     * we are running in global TX, we can not remove the SFUnspecifiedLocal bean until after global TX
     * is completed. We then do JNDI lookup of SLUnspecifiedLocal and call the
     * doNothing( SLUnspecifiedLocal ) method. This causes a 2nd SLUnspecifiedLocal to be created and
     * then destroyed since max pool size is set to 1. Verify that all of the PostConstruct, PostActivate,
     * PrePassivate, and PreDestroy methods for both SFUnspecifiedLocal and SLUnspecifiedLocal execute
     * in their own unspecified TX context rather than in the global TX that was started for the
     * CompStatefulLocal method that was called.
     */
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    @Test
    public void testSLSFSBCallBackInUnspecifiedCtxt_Mix() throws Exception {
        setUp();
        // --------------------------------------------------------------------
        // Locate SFSB Local Business Interface Factory and execute the test
        // --------------------------------------------------------------------
        try {
            InitialContext ictx = new InitialContext();
            final CompStatefulEJBLocalHome home = (CompStatefulEJBLocalHome) ictx.lookup(COMP_SF_HOME_JNDI_NAME);
            assertNotNull("1 ---> lookup of SFSB local home successful.", home);

            final CompStatefulLocal bean = home.create();
            assertNotNull("2 ---> SFSB create successful.", bean);

            // Clear Results lists.
            ivResultsLocal.clearLists();

            // Invoke a doNothing business method.
            boolean expectedTranContext = bean.txRequiredLookup();
            assertTrue("3 ---> SFSB doNothing ran in expected tran context.", expectedTranContext);

            List<String> expectedList = Arrays.asList(COMP_SF_UNSPECIFIED_TRAN_TX_REQUIRED_LOOKUP);
            ArrayList<String> actualList = ivResultsLocal.getTransactionContextData();
            svLogger.info("expected post activate and pre passivate transaction context data: " + expectedList);
            svLogger.info("  actual post activate and pre passivate transaction context data: " + actualList);
            assertEquals("4 ---> post activate/pre passivate transaction context data", expectedList, actualList);

            // Invoke remove method.
            bean.remove();

        } finally {
            // reset SLSB pool
            resetSLUnspecifiedLocal();
            tearDown();
        }
    }

    /**
     * Same as testSLSFSBCallBackInUnspecifiedCtxt exception that the CompStatefulLocal method invoked
     * is annotated to with the NOT_SUPPORTED TX attribute. The idea here is to verify that lifecycle
     * callback event methods execute in their own unspecified TX context rather than in the local TX
     * that was started for the CompStatefulLocal method that is invoked.
     */
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    @Test
    public void testSLSFSBCallBackInUnspecifiedCtxt2_Mix() throws Exception {
        setUp();
        // --------------------------------------------------------------------
        // Locate SFSB Local Business Interface Factory and execute the test
        // --------------------------------------------------------------------
        try {
            InitialContext ictx = new InitialContext();
            final CompStatefulEJBLocalHome home = (CompStatefulEJBLocalHome) ictx.lookup(COMP_SF_HOME_JNDI_NAME);
            assertNotNull("1 ---> lookup of SFSB local home successful.", home);

            final CompStatefulLocal bean = home.create();
            assertNotNull("2 ---> SFSB create successful.", bean);

            // Clear Results lists.
            ivResultsLocal.clearLists();

            // Invoke a doNothing business method.
            boolean expectedTranContext = bean.txNotSupportedLookup();
            assertTrue("3 ---> SFSB doNothing ran in expected tran context.", expectedTranContext);

            List<String> expectedList = Arrays.asList(COMP_SF_UNSPECIFIED_TRAN_TX_NOT_SUPPORTED_LOOKUP);
            ArrayList<String> actualList = ivResultsLocal.getTransactionContextData();
            svLogger.info("expected post activate and pre passivate transaction context data: " + expectedList);
            svLogger.info("  actual post activate and pre passivate transaction context data: " + actualList);
            assertEquals("4 ---> post activate/pre passivate transaction context data", expectedList, actualList);

            // Invoke remove method.
            bean.remove();

        } finally {
            // reset SLSB pool
            resetSLUnspecifiedLocal();
            tearDown();
        }
    }

}
