/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedbeans.fat.mb.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/ManagedBeanServlet")
public class ManagedBeanServlet extends FATServlet {

    private static final long serialVersionUID = 7800020198391473374L;
    private static final String SERVLET_NAME = ManagedBeanServlet.class.getSimpleName();
    private static final String CLASS_NAME = ManagedBeanServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static final String APP = "ManagedBeanWebApp";
    private static final String MOD = "ManagedBeanWeb";
    private static final String SIMPLE_BEAN = "SimpleManagedBean";
    private static final String NAMED_BEAN = "NamedManagedBean";
    private static final String INJ_BEAN = "InjectionManagedBean";
    private static final String POST_BEAN = "PostConstructManagedBean";
    private static final String PRE_BEAN = "PreDestroyManagedBean";
    private static final String INT_BEAN = "InterceptorManagedBean";

    String globalLookupName = "java:global/" + APP + "/" + MOD + "/" + NAMED_BEAN;
    String appLookupName = "java:app/" + MOD + "/" + NAMED_BEAN;
    String modLookupName = "java:module" + "/" + NAMED_BEAN;

    @Resource(name = "TestDSfromWAR", lookup = "jdbc/TestDS")
    DataSource testDSfromWAR;

    @Resource(name = "SimpleManagedBean")
    private SimpleManagedBean ivSMB;

    @Resource(name = "NamedManagedBean", lookup = "java:module/NamedManagedBean")
    private NamedManagedBean ivNMB;

    @Resource(name = "InjManagedBean")
    private InjectionManagedBean ivInjMB;

    @Resource(name = "PostManagedBean")
    private PostConstructManagedBean ivPostMB;

    @Resource(name = "PreManagedBean")
    private PreDestroyManagedBean ivPreMB;

    @Resource(name = "IntManagedBean")
    private InterceptorManagedBean ivIntMB;

    /**
     * Tests that a simple ManagedBean (with no injection or interceptors) may
     * be injected into a servlet. A lookup should fail, since the ManagedBean
     * does not have a name. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testSimpleManagedBeanInjectionAndLookupInWar() throws Exception {
        svLogger.info("> " + SERVLET_NAME + ".testSimpleManagedBeanInjectionAndLookupInWar()");

        SimpleManagedBean mb = null;
        String globalLookupName = "java:global/" + APP + "/" + MOD + "/" + SIMPLE_BEAN;
        String appLookupName = "java:app/" + MOD + "/" + SIMPLE_BEAN;
        String modLookupName = "java:module" + "/" + SIMPLE_BEAN;

        // -----------------------------------------------------------------------
        //
        // Attempt to lookup in java:global/app/module
        //
        // -----------------------------------------------------------------------
        try {
            mb = (SimpleManagedBean) new InitialContext().lookup(globalLookupName);
            svLogger.info("-- ManagedBean found = " + mb);
            fail("lookup in java:global should have failed; ManagedBean never bound there.");
        } catch (NamingException ex) {
            svLogger.info("-- expected NamingException for java:global lookup " + ex);
        }

        try {
            mb = (SimpleManagedBean) new InitialContext().lookup(appLookupName);
            svLogger.info("-- ManagedBean found = " + mb);
            fail("lookup in java:app should have failed; ManagedBean has no name.");
        } catch (NamingException ex) {
            svLogger.info("-- expected NamingException for java:app lookup " + ex);
        }

        try {
            mb = (SimpleManagedBean) new InitialContext().lookup(modLookupName);
            svLogger.info("-- ManagedBean found = " + mb);
            fail("lookup in java:mod should have failed; ManagedBean has no name.");
        } catch (NamingException ex) {
            svLogger.info("-- expected NamingException for java:mod lookup " + ex);
        }

        // -----------------------------------------------------------------------
        //
        // Verify injected, and ref may be looked up (and not equal)
        //
        // -----------------------------------------------------------------------

        assertNotNull("Simple ManagedBean was not injected", ivSMB);

        mb = (SimpleManagedBean) new InitialContext().lookup("java:comp/env/SimpleManagedBean");

        assertNotNull("SimpleManagedBean ref failed on Context.lookup", mb);

        assertNotSame("Injected and looked up are equal.. oops.",
                      ivSMB.getIdentifier(), mb.getIdentifier());

        // -----------------------------------------------------------------------
        //
        // Verify ManagedBeans are stateful
        //
        // -----------------------------------------------------------------------

        ivSMB.setValue("Scooby");
        mb.setValue("Shaggy");

        assertEquals("State of injected ManagedBean not correct",
                     "Scooby", ivSMB.getValue());
        assertEquals("State of looked up ManagedBean not correct",
                     "Shaggy", mb.getValue());

        svLogger.info("< " + SERVLET_NAME + ".testSimpleManagedBeanInjectionAndLookupInWar()");
    }

    /**
     * Tests that a named ManagedBean (with no injection or interceptors) may
     * be injected into a servlet and looked up in both java:app and java:module. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testNamedManagedBeanInjectionAndLookupInWar() throws Exception {
        svLogger.info("> " + SERVLET_NAME + ".testNamedManagedBeanInjectionAndLookupInWar()");

        NamedManagedBean globalMB = null, appMB = null, modMB = null;
        String globalLookupName = "java:global/" + APP + "/" + MOD + "/" + NAMED_BEAN;
        String appLookupName = "java:app/" + MOD + "/" + NAMED_BEAN;
        String modLookupName = "java:module" + "/" + NAMED_BEAN;

        // -----------------------------------------------------------------------
        //
        // Attempt to lookup in java:global/app/module
        //
        // -----------------------------------------------------------------------

        try {
            globalMB = (NamedManagedBean) new InitialContext().lookup(globalLookupName);
            svLogger.info("-- ManagedBean found = " + globalMB);
            fail("lookup in java:global should have failed; ManagedBean never bound there.");
        } catch (NamingException ex) {
            svLogger.info("-- expected NamingException for java:global lookup " + ex);
        }

        try {
            appMB = (NamedManagedBean) new InitialContext().lookup(appLookupName);
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }

        assertNotNull("null returned on java:app ManagedBean lookup", appMB);

        try {
            modMB = (NamedManagedBean) new InitialContext().lookup(modLookupName);
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }

        assertNotNull("null returned on java:mod ManagedBean lookup", modMB);

        // -----------------------------------------------------------------------
        //
        // Verify injected, and ref may be looked up (and not equal)
        //
        // -----------------------------------------------------------------------

        assertNotNull("ManagedBean not injected", ivNMB);

        NamedManagedBean mb = (NamedManagedBean) new InitialContext().lookup("java:comp/env/NamedManagedBean");

        assertNotNull("ManagedBean ref failed on Context.lookup", mb);

        assertNotSame("Injected and looked up are equal.. oops.",
                      ivNMB.getIdentifier(), mb.getIdentifier());

        // -----------------------------------------------------------------------
        //
        // Verify ManagedBeans are stateful
        //
        // -----------------------------------------------------------------------

        ivNMB.setValue("Scooby");
        mb.setValue("Shaggy");

        assertEquals("State of injected ManagedBean not correct",
                     "Scooby", ivNMB.getValue());
        assertEquals("State of looked up ManagedBean not correct",
                     "Shaggy", mb.getValue());

        svLogger.info("< " + SERVLET_NAME + ".testNamedManagedBeanInjectionAndLookupInWar()");
    }

    /**
     * Tests that a named ManagedBean with simple injection (no interceptors) may
     * be injected into a servlet and looked up in both java:app and java:module. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testInjectionManagedBeanInjectionAndLookupInWar() throws Exception {
        svLogger.info("> " + SERVLET_NAME + ".testInjectionManagedBeanInjectionAndLookupInWar()");

        InjectionManagedBean globalMB = null, appMB = null, modMB = null;
        String globalLookupName = "java:global/" + APP + "/" + MOD + "/" + INJ_BEAN;
        String appLookupName = "java:app/" + MOD + "/" + INJ_BEAN;
        String modLookupName = "java:module" + "/" + INJ_BEAN;

        // -----------------------------------------------------------------------
        //
        // Attempt to lookup in java:global/app/module
        //
        // -----------------------------------------------------------------------

        try {
            globalMB = (InjectionManagedBean) new InitialContext().lookup(globalLookupName);
            svLogger.info("-- ManagedBean found = " + globalMB);
            fail("lookup in java:global should have failed; ManagedBean never bound there.");
        } catch (NamingException ex) {
            svLogger.info("-- expected NamingException for java:global lookup " + ex);
        }

        try {
            appMB = (InjectionManagedBean) new InitialContext().lookup(appLookupName);
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }

        assertNotNull("null returned on java:app ManagedBean lookup", appMB);

        try {
            modMB = (InjectionManagedBean) new InitialContext().lookup(modLookupName);
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }

        assertNotNull("null returned on java:mod ManagedBean lookup", modMB);

        // -----------------------------------------------------------------------
        //
        // Verify injected, and ref may be looked up (and not equal)
        //
        // -----------------------------------------------------------------------

        assertNotNull("ManagedBean not injected", ivInjMB);

        InjectionManagedBean mb = (InjectionManagedBean) new InitialContext().lookup("java:comp/env/InjManagedBean");

        assertNotNull("ManagedBean ref failed on Context.lookup", mb);

        assertNotSame("Injected and looked up are equal.. oops.",
                      ivInjMB.getIdentifier(), mb.getIdentifier());

        // -----------------------------------------------------------------------
        //
        // Verify ManagedBeans are stateful
        //
        // -----------------------------------------------------------------------

        ivInjMB.setValue("Scooby");
        mb.setValue("Shaggy");

        assertEquals("State of injected ManagedBean not correct",
                     "Scooby", ivInjMB.getValue());
        assertEquals("State of looked up ManagedBean not correct",
                     "Shaggy", mb.getValue());

        // -----------------------------------------------------------------------
        //
        // Verify UserTransaction and DataSources were injected into ManagedBean
        //
        // -----------------------------------------------------------------------

        assertNotNull("UserTransaction not injected into ManagedBean", ivInjMB.getUserTransaction());
        assertNotNull("DataSource not injected into ManagedBean", ivInjMB.getDataSource());
        assertNotNull("DataSource defined by WAR not injected into ManagedBean", ivInjMB.getWARdefinedDataSource());

        // -----------------------------------------------------------------------
        //
        // Verify EJB was NOT injected into ManagedBean because ejbLite is not
        // enabled in the ManagedBeansServer
        //
        // -----------------------------------------------------------------------

        assertNull("EJB should not be injected into ManagedBean", ivInjMB.ejbRef);

        svLogger.info("< " + SERVLET_NAME + ".testInjectionManagedBeanInjectionAndLookupInWar()");
    }

    /**
     * Tests that a named ManagedBean with simple injection and PostConstruct
     * (no around invoke interceptors) may be injected into a servlet and looked
     * up in both java:app and java:module. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testPostConstructManagedBeanInjectionAndLookupInWar() throws Exception {
        svLogger.info("> " + SERVLET_NAME + ".testPostConstructManagedBeanInjectionAndLookupInWar()");

        PostConstructManagedBean globalMB = null, appMB = null, modMB = null;
        String globalLookupName = "java:global/" + APP + "/" + MOD + "/" + POST_BEAN;
        String appLookupName = "java:app/" + MOD + "/" + POST_BEAN;
        String modLookupName = "java:module" + "/" + POST_BEAN;

        // -----------------------------------------------------------------------
        //
        // Attempt to lookup in java:global/app/module
        //
        // -----------------------------------------------------------------------

        try {
            globalMB = (PostConstructManagedBean) new InitialContext().lookup(globalLookupName);
            svLogger.info("-- ManagedBean found = " + globalMB);
            fail("lookup in java:global should have failed; ManagedBean never bound there.");
        } catch (NamingException ex) {
            svLogger.info("-- expected NamingException for java:global lookup " + ex);
        }

        try {
            appMB = (PostConstructManagedBean) new InitialContext().lookup(appLookupName);
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }

        assertNotNull("null returned on java:app ManagedBean lookup", appMB);

        try {
            modMB = (PostConstructManagedBean) new InitialContext().lookup(modLookupName);
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }

        assertNotNull("null returned on java:mod ManagedBean lookup", modMB);

        // -----------------------------------------------------------------------
        //
        // Verify injected, and ref may be looked up (and not equal)
        //
        // -----------------------------------------------------------------------

        assertNotNull("ManagedBean not injected", ivPostMB);

        PostConstructManagedBean mb = (PostConstructManagedBean) new InitialContext().lookup("java:comp/env/PostManagedBean");

        assertNotNull("ManagedBean ref failed on Context.lookup", mb);

        assertNotSame("Injected and looked up are equal.. oops.",
                      ivPostMB.getIdentifier(), mb.getIdentifier());

        // -----------------------------------------------------------------------
        //
        // Verify PostConstruct was called on the ManagedBeans
        //
        // -----------------------------------------------------------------------

        assertEquals("Unexpected initial value", "PostConstructManagedBean.INITIAL_VALUE", ivPostMB.getValue());
        assertEquals("Unexpected initial value", "PostConstructManagedBean.INITIAL_VALUE", mb.getValue());

        // -----------------------------------------------------------------------
        //
        // Verify ManagedBeans are stateful
        //
        // -----------------------------------------------------------------------

        ivPostMB.setValue("Scooby");
        mb.setValue("Shaggy");

        assertEquals("State of injected ManagedBean not correct",
                     "Scooby", ivPostMB.getValue());
        assertEquals("State of looked up ManagedBean not correct",
                     "Shaggy", mb.getValue());

        // -----------------------------------------------------------------------
        //
        // Verify UserTransaction was injected into ManagedBean
        //
        // -----------------------------------------------------------------------

        assertNotNull("UserTransaction not injected into ManagedBean", ivPostMB.getUserTransaction());

        svLogger.info("< " + SERVLET_NAME + ".testPostConstructManagedBeanInjectionAndLookupInWar()");
    }

    /**
     * Tests that a named ManagedBean with simple injection and PreDestroy
     * (no around invoke interceptors) may be injected into a servlet and looked
     * up in both java:app and java:module. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testPreDestroyManagedBeanInjectionAndLookupInWar() throws Exception {
        svLogger.info("> " + SERVLET_NAME + ".testPreDestroyManagedBeanInjectionAndLookupInWar()");

        PreDestroyManagedBean globalMB = null, appMB = null, modMB = null;
        String globalLookupName = "java:global/" + APP + "/" + MOD + "/" + PRE_BEAN;
        String appLookupName = "java:app/" + MOD + "/" + PRE_BEAN;
        String modLookupName = "java:module" + "/" + PRE_BEAN;

        // -----------------------------------------------------------------------
        //
        // Attempt to lookup in java:global/app/module
        //
        // -----------------------------------------------------------------------

        try {
            globalMB = (PreDestroyManagedBean) new InitialContext().lookup(globalLookupName);
            svLogger.info("-- ManagedBean found = " + globalMB);
            fail("lookup in java:global should have failed; ManagedBean never bound there.");
        } catch (NamingException ex) {
            svLogger.info("-- expected NamingException for java:global lookup " + ex);
        }

        try {
            appMB = (PreDestroyManagedBean) new InitialContext().lookup(appLookupName);
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }

        assertNotNull("null returned on java:app ManagedBean lookup", appMB);

        try {
            modMB = (PreDestroyManagedBean) new InitialContext().lookup(modLookupName);
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }

        assertNotNull("null returned on java:mod ManagedBean lookup", modMB);

        // -----------------------------------------------------------------------
        //
        // Verify injected, and ref may be looked up (and not equal)
        //
        // -----------------------------------------------------------------------

        assertNotNull("ManagedBean not injected", ivPreMB);

        PreDestroyManagedBean mb = (PreDestroyManagedBean) new InitialContext().lookup("java:comp/env/PreManagedBean");

        assertNotNull("ManagedBean ref failed on Context.lookup", mb);

        assertNotSame("Injected and looked up are equal.. oops.",
                      ivPreMB.getIdentifier(), mb.getIdentifier());

        // -----------------------------------------------------------------------
        //
        // Verify PostConstruct was called on the ManagedBeans
        //
        // -----------------------------------------------------------------------

        assertEquals("Unexpected initial value", "PreDestroyManagedBean.INITIAL_VALUE", ivPreMB.getValue());
        assertEquals("Unexpected initial value", "PreDestroyManagedBean.INITIAL_VALUE", mb.getValue());

        // -----------------------------------------------------------------------
        //
        // Verify ManagedBeans are stateful
        //
        // -----------------------------------------------------------------------

        ivPreMB.setValue("Scooby");
        mb.setValue("Shaggy");

        assertEquals("State of injected ManagedBean not correct",
                     "Scooby", ivPreMB.getValue());
        assertEquals("State of looked up ManagedBean not correct",
                     "Shaggy", mb.getValue());

        // -----------------------------------------------------------------------
        //
        // Verify UserTransaction was injected into ManagedBean
        //
        // -----------------------------------------------------------------------

        assertNotNull("UserTransaction not injected into ManagedBean", ivPreMB.getUserTransaction());

        // -----------------------------------------------------------------------
        //
        // Verify PreDestroy was called on the ManagedBeans
        //
        // -----------------------------------------------------------------------

        // first.... null out all references, to make eligible for cleanup
        globalMB = null;
        appMB = null;
        modMB = null;
        mb = null;

        // Create a bunch of these, and don't hold references
        int numDestroyed = 0;
        for (int i = 1; i < 50000; i++) {
            new InitialContext().lookup("java:comp/env/PreManagedBean");

            if (i % 100 == 0) {
                try {
                    Thread.sleep(1000); // sleep for a short time
                } catch (InterruptedException ex) {
                    ex.printStackTrace(System.out);
                }

                numDestroyed = PreDestroyManagedBean.getDestroyCount();
                if (numDestroyed > 0) {
                    break;
                }
            }
        }

        svLogger.info("Number of PreDestroy calls = " + numDestroyed);

        if (numDestroyed == 0) {
            fail("PreDestroy was never called");
        }

        svLogger.info("< " + SERVLET_NAME + ".testPreDestroyManagedBeanInjectionAndLookupInWar()");
    }

    /**
     * Tests that a named ManagedBean with simple injection and AroundInvoke
     * interceptors may be injected into a servlet and looked up in both java:app
     * and java:module. <p>
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testInterceptorManagedBeanInjectionAndLookupInWar() throws Exception {
        svLogger.info("> " + SERVLET_NAME + ".testInterceptorManagedBeanInjectionAndLookupInWar()");

        InterceptorManagedBean globalMB = null, appMB = null, modMB = null;
        String globalLookupName = "java:global/" + APP + "/" + MOD + "/" + INT_BEAN;
        String appLookupName = "java:app/" + MOD + "/" + INT_BEAN;
        String modLookupName = "java:module" + "/" + INT_BEAN;

        // -----------------------------------------------------------------------
        //
        // Attempt to lookup in java:global/app/module
        //
        // -----------------------------------------------------------------------

        try {
            globalMB = (InterceptorManagedBean) new InitialContext().lookup(globalLookupName);
            svLogger.info("-- ManagedBean found = " + globalMB);
            fail("lookup in java:global should have failed; ManagedBean never bound there.");
        } catch (NamingException ex) {
            svLogger.info("-- expected NamingException for java:global lookup " + ex);
        }

        try {
            appMB = (InterceptorManagedBean) new InitialContext().lookup(appLookupName);
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }

        assertNotNull("null returned on java:app ManagedBean lookup", appMB);

        try {
            modMB = (InterceptorManagedBean) new InitialContext().lookup(modLookupName);
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }

        assertNotNull("null returned on java:mod ManagedBean lookup", modMB);

        // -----------------------------------------------------------------------
        //
        // Verify injected, and ref may be looked up (and not equal)
        //
        // -----------------------------------------------------------------------

        assertNotNull("ManagedBean not injected", ivIntMB);

        InterceptorManagedBean mb = (InterceptorManagedBean) new InitialContext().lookup("java:comp/env/IntManagedBean");

        assertNotNull("ManagedBean ref failed on Context.lookup", mb);

        assertNotSame("Injected and looked up are equal.. oops.",
                      ivIntMB.getIdentifier(), mb.getIdentifier());

        // -----------------------------------------------------------------------
        //
        // Verify PostConstruct was called on the ManagedBeans
        //
        // -----------------------------------------------------------------------

        assertEquals("Unexpected initial value", "InterceptorManagedBean.INITIAL_VALUE", ivIntMB.getValue());
        assertEquals("Unexpected initial value", "InterceptorManagedBean.INITIAL_VALUE", mb.getValue());

        // -----------------------------------------------------------------------
        //
        // Verify ManagedBeans are stateful
        //
        // -----------------------------------------------------------------------

        ivIntMB.setValue("Scooby");
        mb.setValue("Shaggy");

        assertEquals("State of injected ManagedBean not correct",
                     "Scooby", ivIntMB.getValue());
        assertEquals("State of looked up ManagedBean not correct",
                     "Shaggy", mb.getValue());

        // -----------------------------------------------------------------------
        //
        // Verify UserTransaction was injected into ManagedBean
        //
        // -----------------------------------------------------------------------

        assertNotNull("UserTransaction not injected into ManagedBean", ivIntMB.getUserTransaction());

        // -----------------------------------------------------------------------
        //
        // Verify the proper interceptors were called
        //
        // -----------------------------------------------------------------------

        mb = (InterceptorManagedBean) new InitialContext().lookup("java:comp/env/IntManagedBean");
        mb.verifyInterceptorCalls(new ArrayList<String>());

        svLogger.info("< " + SERVLET_NAME + ".testInterceptorManagedBeanInjectionAndLookupInWar()");
    }

    /**
     * Tests that a ManagedBean can declare java:global, java:app, and
     * java:module resources
     *
     * @throws Exception when an assertion failure occurs.
     */
    @Test
    public void testNonCompManagedBeanInWar() {
        for (String name : new String[] { "java:global/env/tsr", "java:app/env/tsr", "java:module/env/tsr" }) {
            try {
                new InitialContext().lookup(name);
            } catch (NamingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
