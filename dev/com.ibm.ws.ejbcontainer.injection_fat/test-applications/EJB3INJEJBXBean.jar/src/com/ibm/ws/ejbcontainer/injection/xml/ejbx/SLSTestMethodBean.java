/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.xml.ejbx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.ejb.EJBException;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Basic Stateless Bean implementation for testing EJB Injection via XML
 **/
public class SLSTestMethodBean {
    private static final String PASSED = "Passed";

    public Object bean;
    private int injectCount = 0;

    private EJBMethodLocal SLMethodLocal1;
    public EJBMethodRemote SLMethodRemote1;
    private EJBMethodLocal SFMethodLocal1;
    public EJBMethodRemote SFMethodRemote1;
    protected EJBMethodLocal SLMethodLocal2;
    EJBMethodRemote SLMethodRemote2;
    protected EJBMethodLocal SFMethodLocal2;
    EJBMethodRemote SFMethodRemote2;
    public EJBMethodLocal SLMethodLocal3;
    private EJBMethodRemote SLMethodRemote3;
    public EJBMethodLocal SFMethodLocal3;
    private EJBMethodRemote SFMethodRemote3;
    EJBMethodLocal SLMethodLocal4;
    protected EJBMethodRemote SLMethodRemote4;
    EJBMethodLocal SFMethodLocal4;
    protected EJBMethodRemote SFMethodRemote4;

    private EJBMethod SLMethod1;
    public EJBMethod SFMethod1;

    private EJBUnique SLMethodUnique;
    public EJBMethodLocal SLMethodSame;
    private EJBMethodRemote SLMethodOther;
    public EJBMethodLocal SLMethodLocalOther;
    EJBAuto SLMethodAuto;
    EJBIdentical SLMethodOtherIdentical;
    protected EJBIdentical SLMethodIdentical;
    protected EJBNested SLMethodNested;

    @SuppressWarnings("unused")
    private void setSLMethodLocal1(EJBMethodLocal ejb) {
        SLMethodLocal1 = ejb;
        ++injectCount;
    }

    public void setSLMethodRemote1(EJBMethodRemote ejb) {
        SLMethodRemote1 = ejb;
        ++injectCount;
    }

    @SuppressWarnings("unused")
    private void setSFMethodLocal1(EJBMethodLocal ejb) {
        SFMethodLocal1 = ejb;
        ++injectCount;
    }

    public void setSFMethodRemote1(EJBMethodRemote ejb) {
        SFMethodRemote1 = ejb;
        ++injectCount;
    }

    protected void setSLMethodLocal2(EJBMethodLocal ejb) {
        SLMethodLocal2 = ejb;
        ++injectCount;
    }

    void setSLMethodRemote2(EJBMethodRemote ejb) {
        SLMethodRemote2 = ejb;
        ++injectCount;
    }

    protected void setSFMethodLocal2(EJBMethodLocal ejb) {
        SFMethodLocal2 = ejb;
        ++injectCount;
    }

    void setSFMethodRemote2(EJBMethodRemote ejb) {
        SFMethodRemote2 = ejb;
        ++injectCount;
    }

    public void setSLMethodLocal3(EJBMethodLocal ejb) {
        SLMethodLocal3 = ejb;
        ++injectCount;
    }

    @SuppressWarnings("unused")
    private void setSLMethodRemote3(EJBMethodRemote ejb) {
        SLMethodRemote3 = ejb;
        ++injectCount;
    }

    public void setSFMethodLocal3(EJBMethodLocal ejb) {
        SFMethodLocal3 = ejb;
        ++injectCount;
    }

    @SuppressWarnings("unused")
    private void setSFMethodRemote3(EJBMethodRemote ejb) {
        SFMethodRemote3 = ejb;
        ++injectCount;
    }

    void setSLMethodLocal4(EJBMethodLocal ejb) {
        SLMethodLocal4 = ejb;
        ++injectCount;
    }

    protected void setSLMethodRemote4(EJBMethodRemote ejb) {
        SLMethodRemote4 = ejb;
        ++injectCount;
    }

    void setSFMethodLocal4(EJBMethodLocal ejb) {
        SFMethodLocal4 = ejb;
        ++injectCount;
    }

    protected void setSFMethodRemote4(EJBMethodRemote ejb) {
        SFMethodRemote4 = ejb;
        ++injectCount;
    }

    @SuppressWarnings("unused")
    private void setSLMethod1(EJBMethodLocal ejb) {
        SLMethod1 = ejb;
        ++injectCount;
    }

    public void setSFMethod1(EJBMethodRemote ejb) {
        SFMethod1 = ejb;
        ++injectCount;
    }

    @SuppressWarnings("unused")
    private void setSLMethodUnique(EJBUnique ejb) {
        SLMethodUnique = ejb;
        ++injectCount;
    }

    public void setSLMethodSame(EJBMethodLocal ejb) {
        SLMethodSame = ejb;
        ++injectCount;
    }

    @SuppressWarnings("unused")
    private void setSLMethodOther(EJBMethodRemote ejb) {
        SLMethodOther = ejb;
        ++injectCount;
    }

    public void setSLMethodLocalOther(EJBMethodLocal ejb) {
        SLMethodLocalOther = ejb;
        ++injectCount;
    }

    void setSLMethodAuto(EJBAuto ejb) {
        SLMethodAuto = ejb;
        ++injectCount;
    }

    void setSLMethodOtherIdentical(EJBIdentical ejb) {
        SLMethodOtherIdentical = ejb;
        ++injectCount;
    }

    protected void setSLMethodIdentical(EJBIdentical ejb) {
        SLMethodIdentical = ejb;
        ++injectCount;
    }

    protected void setSLMethodNested(EJBNested ejb) {
        SLMethodNested = ejb;
        ++injectCount;
    }

    private SessionContext ivContext;

    /**
     * Verify EJB Injection (field or method) occurred properly.
     **/
    public String verifyEJB30Injection(int testpoint) {
        String envName = null;
        @SuppressWarnings("unused")
        String currBean = null;

        // Assert that 20 of the injection methods were called
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "20 Injection Methods called : 20 : " + injectCount,
                     20, injectCount);
        ++testpoint;

        // Assert that all of the EJBs are injected properly from XML
        currBean = "SLMethodLocal1";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodLocal1 is Local", SLMethodLocal1.isLocal());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodLocal1 is a Method injection", SLMethodLocal1.isMethodLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLMethodLocal1 is SLBean1", SLMethodLocal1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean1");
        ++testpoint;

        currBean = "SLMethodRemote1";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodRemote1 is Remote", SLMethodRemote1.isRemote());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodRemote1 is a Method injection", SLMethodRemote1.isMethodLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLMethodRemote1 is SLBean1", SLMethodRemote1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean1");
        ++testpoint;

        currBean = "SFMethodLocal1";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodLocal1 is Local", SFMethodLocal1.isLocal());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodLocal1 is a Method injection", SFMethodLocal1.isMethodLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SFMethodLocal1 is SFBean1", SFMethodLocal1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean1");
        ++testpoint;

        currBean = "SFMethodRemote1";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodRemote1 is Remote", SFMethodRemote1.isRemote());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodRemote1 is a Method injection", SFMethodRemote1.isMethodLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SFMethodRemote1 is SFBean1", SFMethodRemote1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean1");
        ++testpoint;

        currBean = "SLMethodLocal2";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodLocal2 is Local", SLMethodLocal2.isLocal());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodLocal2 is a Method injection", SLMethodLocal2.isMethodLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLMethodLocal2 is SLBean2", SLMethodLocal2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean2");
        ++testpoint;

        currBean = "SLMethodRemote2";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodRemote2 is Remote", SLMethodRemote2.isRemote());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodRemote2 is a Method injection", SLMethodRemote2.isMethodLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLMethodRemote2 is SLBean2", SLMethodRemote2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean2");
        ++testpoint;

        currBean = "SFMethodLocal2";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodLocal2 is Local", SFMethodLocal2.isLocal());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodLocal2 is a Method injection", SFMethodLocal2.isMethodLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SFMethodLocal2 is SFBean2", SFMethodLocal2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean2");
        ++testpoint;

        currBean = "SFMethodRemote2";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodRemote2 is Remote", SFMethodRemote2.isRemote());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodRemote2 is a Method injection", SFMethodRemote2.isMethodLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SFMethodRemote2 is SFBean2", SFMethodRemote2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean2");
        ++testpoint;

        currBean = "SLMethodLocal3";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodLocal3 is Local", SLMethodLocal3.isLocal());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodLocal3 is a Method injection", SLMethodLocal3.isMethodLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLMethodLocal3 is SLBean3", SLMethodLocal3.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean3");
        ++testpoint;

        currBean = "SLMethodRemote3";
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodRemote3 was not injected", SLMethodRemote3);
        ++testpoint;

        currBean = "SFMethodLocal3";
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodLocal3 was not injected", SFMethodLocal3);
        ++testpoint;

        currBean = "SFMethodRemote3";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodRemote3 is Remote", SFMethodRemote3.isRemote());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodRemote3 is a Method injection", SFMethodRemote3.isMethodLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SFMethodRemote3 is SFBean3", SFMethodRemote3.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean3");
        ++testpoint;

        currBean = "SLMethodLocal4";
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodLocal4 was not injected", SLMethodLocal4);
        ++testpoint;

        currBean = "SLMethodRemote4";
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodRemote4 was not injected", SLMethodRemote4);
        ++testpoint;

        currBean = "SFMethodLocal4";
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodLocal4 was not injected", SFMethodLocal4);
        ++testpoint;

        currBean = "SFMethodRemote4";
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodRemote4 was not injected", SFMethodRemote4);
        ++testpoint;

        currBean = "SLMethod1";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethod1 is a Method injection", SLMethod1.isMethodLevel());
        ++testpoint;

        currBean = "SFMethod1";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethod1 is a Method injection", SFMethod1.isMethodLevel());
        ++testpoint;

        // Next, ensure the above may be looked up in the global namespace
        try {
            Context initCtx = new InitialContext();
            Context myEnv = (Context) initCtx.lookup("java:comp/env");

            envName = "ejb/SLMLocal1";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodLocal jndiSLML1 = (EJBMethodLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLML1 is SLBean1", jndiSLML1.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean1");
            ++testpoint;

            envName = "ejb/SLM1";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodRemote jndiSLMR1 = (EJBMethodRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLMR1 is SLBean1", jndiSLMR1.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean1");
            ++testpoint;

            envName = "ejb/SFMLocal1";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodLocal jndiSFML1 = (EJBMethodLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFML1 is SFBean1", jndiSFML1.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean1");
            ++testpoint;

            envName = "ejb/SFM1";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodRemote jndiSFMR1 = (EJBMethodRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFMR1 is SFBean1", jndiSFMR1.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean1");
            ++testpoint;

            envName = "ejb/SLMLocal2";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodLocal jndiSLML2 = (EJBMethodLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLML2 is SLBean2", jndiSLML2.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean2");
            ++testpoint;

            envName = "ejb/SLM2";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodRemote jndiSLMR2 = (EJBMethodRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLMR2 is SLBean2", jndiSLMR2.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean2");
            ++testpoint;

            envName = "ejb/SFMLocal2";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodLocal jndiSFML2 = (EJBMethodLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFML2 is SFBean2", jndiSFML2.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean2");
            ++testpoint;

            envName = "ejb/SFM2";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodRemote jndiSFMR2 = (EJBMethodRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFMR2 is SFBean2", jndiSFMR2.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean2");
            ++testpoint;

            envName = "ejb/SLMLocal3";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodLocal jndiSLML3 = (EJBMethodLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLML3 is SLBean3", jndiSLML3.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean3");
            ++testpoint;

            envName = "ejb/SLM3";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodRemote jndiSLMR3 = (EJBMethodRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLMR3 is SLBean3", jndiSLMR3.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean3");
            ++testpoint;

            envName = "ejb/SFMLocal3";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodLocal jndiSFML3 = (EJBMethodLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFML3 is SFBean3", jndiSFML3.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean3");
            ++testpoint;

            envName = "ejb/SFM3";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodRemote jndiSFMR3 = (EJBMethodRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFMR3 is SFBean3", jndiSFMR3.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean3");
            ++testpoint;

            envName = "ejb/SLMLocal4";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodLocal jndiSLML4 = (EJBMethodLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLML4 is SLBean4", jndiSLML4.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean4");
            ++testpoint;

            envName = "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLSTestMethodBean/SLMethodRemote4";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodRemote jndiSLMR4 = (EJBMethodRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLMR4 is SLBean4", jndiSLMR4.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean4");
            ++testpoint;

            envName = "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLSTestMethodBean/SFMethodLocal4";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodLocal jndiSFML4 = (EJBMethodLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFML4 is SFBean4", jndiSFML4.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean4");
            ++testpoint;

            envName = "ejb/SFM4";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodRemote jndiSFMR4 = (EJBMethodRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFMR4 is SFBean4", jndiSFMR4.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean4");
            ++testpoint;

            envName = "ejb/SLMAssign";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethod jndiSLMAssign = (EJBMethod) bean;
            bean = null;
            assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                       "jndiSLMAssign is Method", jndiSLMAssign.isMethodLevel());
            ++testpoint;

            envName = "ejb/SFMAssign";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethod jndiSFMAssign = (EJBMethod) bean;
            bean = null;
            assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                       "jndiSFMAssign is Method", jndiSFMAssign.isMethodLevel());
            ++testpoint;
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            throw new EJBException("Unexpected NamingException : " + ex);
        }

        // Next, ensure the above may be looked up from the SessionContext
        envName = "ejb/SLMLocal1";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodLocal ctxSLML1 = (EJBMethodLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLML1 is SLBean1", ctxSLML1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean1");
        ++testpoint;

        envName = "ejb/SLM1";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodRemote ctxSLMR1 = (EJBMethodRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLMR1 is SLBean1", ctxSLMR1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean1");
        ++testpoint;

        envName = "ejb/SFMLocal1";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodLocal ctxSFML1 = (EJBMethodLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFML1 is SFBean1", ctxSFML1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean1");
        ++testpoint;

        envName = "ejb/SFM1";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodRemote ctxSFMR1 = (EJBMethodRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFMR1 is SFBean1", ctxSFMR1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean1");
        ++testpoint;

        envName = "ejb/SLMLocal2";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodLocal ctxSLML2 = (EJBMethodLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLML2 is SLBean2", ctxSLML2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean2");
        ++testpoint;

        envName = "ejb/SLM2";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodRemote ctxSLMR2 = (EJBMethodRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLMR2 is SLBean2", ctxSLMR2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean2");
        ++testpoint;

        envName = "ejb/SFMLocal2";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodLocal ctxSFML2 = (EJBMethodLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFML2 is SFBean2", ctxSFML2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean2");
        ++testpoint;

        envName = "ejb/SFM2";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodRemote ctxSFMR2 = (EJBMethodRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFMR2 is SFBean2", ctxSFMR2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean2");
        ++testpoint;

        envName = "ejb/SLMLocal3";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodLocal ctxSLML3 = (EJBMethodLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLML3 is SLBean3", ctxSLML3.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean3");
        ++testpoint;

        envName = "ejb/SLM3";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodRemote ctxSLMR3 = (EJBMethodRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLMR3 is SLBean3", ctxSLMR3.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean3");
        ++testpoint;

        envName = "ejb/SFMLocal3";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodLocal ctxSFML3 = (EJBMethodLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFML3 is SFBean3", ctxSFML3.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean3");
        ++testpoint;

        envName = "ejb/SFM3";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodRemote ctxSFMR3 = (EJBMethodRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFMR3 is SFBean3", ctxSFMR3.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean3");
        ++testpoint;

        envName = "ejb/SLMLocal4";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodLocal ctxSLML4 = (EJBMethodLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLML4 is SLBean4", ctxSLML4.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean4");
        ++testpoint;

        envName = "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLSTestMethodBean/SLMethodRemote4";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodRemote ctxSLMR4 = (EJBMethodRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLMR4 is SLBean4", ctxSLMR4.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean4");
        ++testpoint;

        envName = "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLSTestMethodBean/SFMethodLocal4";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodLocal ctxSFML4 = (EJBMethodLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFML4 is SFBean4", ctxSFML4.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean4");
        ++testpoint;

        envName = "ejb/SFM4";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodRemote ctxSFMR4 = (EJBMethodRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFMR4 is SFBean4", ctxSFMR4.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean4");
        ++testpoint;

        envName = "ejb/SLMAssign";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBLocal ctxSLMAssign = (EJBLocal) bean;
        bean = null;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "ctxSLMAssign is Local", ctxSLMAssign.isLocal());
        ++testpoint;

        envName = "ejb/SFMAssign";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBRemote ctxSFMAssign = (EJBRemote) bean;
        bean = null;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "ctxSFMAssign is Remote", ctxSFMAssign.isRemote());
        ++testpoint;

        // Finally, reset all of the fields to insure injection does not occur
        // when object is re-used from the pool.
        SLMethodLocal1 = null;
        SLMethodRemote1 = null;
        SFMethodLocal1 = null;
        SFMethodRemote1 = null;
        SLMethodLocal2 = null;
        SLMethodRemote2 = null;
        SFMethodLocal2 = null;
        SFMethodRemote2 = null;
        SLMethodLocal3 = null;
        SLMethodRemote3 = null;
        SFMethodLocal3 = null;
        SFMethodRemote3 = null;
        SLMethodLocal4 = null;
        SLMethodRemote4 = null;
        SFMethodLocal4 = null;
        SFMethodRemote4 = null;

        SLMethod1 = null;
        SFMethod1 = null;

        return PASSED;
    }

    /**
     * Verify No Environment Injection (field or method) occurred when
     * an method is called using an instance from the pool.
     **/
    public String verifyNoEJB30Injection(int testpoint) {
        String envName = null;

        // Assert that none of the EJBs are injected from XML for a pooled instance.
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodLocal1 was not injected twice", SLMethodLocal1);
        ++testpoint;
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodRemote1 was not injected twice", SLMethodRemote1);
        ++testpoint;

        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodLocal1 was not injected twice", SFMethodLocal1);
        ++testpoint;
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodRemote1 was not injected twice", SFMethodRemote1);
        ++testpoint;

        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodLocal2 was not injected twice", SLMethodLocal2);
        ++testpoint;
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodRemote2 was not injected twice", SLMethodRemote2);
        ++testpoint;

        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodLocal2 was not injected twice", SFMethodLocal2);
        ++testpoint;
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodRemote2 was not injected twice", SFMethodRemote2);
        ++testpoint;

        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodLocal3 was not injected twice", SLMethodLocal3);
        ++testpoint;
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodRemote3 was never injected", SLMethodRemote3);
        ++testpoint;

        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodLocal3 was never injected", SFMethodLocal3);
        ++testpoint;
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodRemote3 was not injected twice", SFMethodRemote3);
        ++testpoint;

        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodLocal4 was never injected", SLMethodLocal4);
        ++testpoint;
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodRemote4 was never injected", SLMethodRemote4);
        ++testpoint;

        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodLocal4 was never injected", SFMethodLocal4);
        ++testpoint;
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethodRemote4 was never injected", SFMethodRemote4);
        ++testpoint;

        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethod1 was not injected twice", SLMethod1);
        ++testpoint;
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFMethod1 was not injected twice", SFMethod1);
        ++testpoint;

        // Next, ensure the above may be looked up in the global namespace
        // and return the original injected value (not modified value)
        try {
            Context initCtx = new InitialContext();
            Context myEnv = (Context) initCtx.lookup("java:comp/env");

            envName = "ejb/SLMLocal1";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodLocal jndiSLML1 = (EJBMethodLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLML1 is SLBean1", jndiSLML1.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean1");
            ++testpoint;

            envName = "ejb/SLM1";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodRemote jndiSLMR1 = (EJBMethodRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLMR1 is SLBean1", jndiSLMR1.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean1");
            ++testpoint;

            envName = "ejb/SFMLocal1";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodLocal jndiSFML1 = (EJBMethodLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFML1 is SFBean1", jndiSFML1.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean1");
            ++testpoint;

            envName = "ejb/SFM1";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodRemote jndiSFMR1 = (EJBMethodRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFMR1 is SFBean1", jndiSFMR1.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean1");
            ++testpoint;

            envName = "ejb/SLMLocal2";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodLocal jndiSLML2 = (EJBMethodLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLML2 is SLBean2", jndiSLML2.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean2");
            ++testpoint;

            envName = "ejb/SLM2";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodRemote jndiSLMR2 = (EJBMethodRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLMR2 is SLBean2", jndiSLMR2.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean2");
            ++testpoint;

            envName = "ejb/SFMLocal2";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodLocal jndiSFML2 = (EJBMethodLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFML2 is SFBean2", jndiSFML2.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean2");
            ++testpoint;

            envName = "ejb/SFM2";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodRemote jndiSFMR2 = (EJBMethodRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFMR2 is SFBean2", jndiSFMR2.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean2");
            ++testpoint;

            envName = "ejb/SLMLocal3";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodLocal jndiSLML3 = (EJBMethodLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLML3 is SLBean3", jndiSLML3.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean3");
            ++testpoint;

            envName = "ejb/SLM3";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodRemote jndiSLMR3 = (EJBMethodRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLMR3 is SLBean3", jndiSLMR3.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean3");
            ++testpoint;

            envName = "ejb/SFMLocal3";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodLocal jndiSFML3 = (EJBMethodLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFML3 is SFBean3", jndiSFML3.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean3");
            ++testpoint;

            envName = "ejb/SFM3";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodRemote jndiSFMR3 = (EJBMethodRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFMR3 is SFBean3", jndiSFMR3.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean3");
            ++testpoint;

            envName = "ejb/SLMLocal4";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodLocal jndiSLML4 = (EJBMethodLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLML4 is SLBean4", jndiSLML4.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean4");
            ++testpoint;

            envName = "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLSTestMethodBean/SLMethodRemote4";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodRemote jndiSLMR4 = (EJBMethodRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLMR4 is SLBean4", jndiSLMR4.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean4");
            ++testpoint;

            envName = "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLSTestMethodBean/SFMethodLocal4";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodLocal jndiSFML4 = (EJBMethodLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFML4 is SFBean4", jndiSFML4.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean4");
            ++testpoint;

            envName = "ejb/SFM4";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodRemote jndiSFMR4 = (EJBMethodRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFMR4 is SFBean4", jndiSFMR4.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean4");
            ++testpoint;

            envName = "ejb/SLMAssign";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBLocal jndiSLMAssign = (EJBLocal) bean;
            bean = null;
            assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                       "jndiSLMAssign is Local", jndiSLMAssign.isLocal());
            ++testpoint;

            envName = "ejb/SFMAssign";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBRemote jndiSFMAssign = (EJBRemote) bean;
            bean = null;
            assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                       "jndiSFMAssign is Remote", jndiSFMAssign.isRemote());
            ++testpoint;
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            throw new EJBException("Unexpected NamingException : " + ex);
        }

        // Next, ensure the above may be looked up from the SessionContext
        // and return the original injected value (not modified value)
        envName = "ejb/SLMLocal1";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodLocal ctxSLML1 = (EJBMethodLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLML1 is SLBean1", ctxSLML1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean1");
        ++testpoint;

        envName = "ejb/SLM1";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodRemote ctxSLMR1 = (EJBMethodRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLMR1 is SLBean1", ctxSLMR1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean1");
        ++testpoint;

        envName = "ejb/SFMLocal1";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodLocal ctxSFML1 = (EJBMethodLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFML1 is SFBean1", ctxSFML1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean1");
        ++testpoint;

        envName = "ejb/SFM1";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodRemote ctxSFMR1 = (EJBMethodRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFMR1 is SFBean1", ctxSFMR1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean1");
        ++testpoint;

        envName = "ejb/SLMLocal2";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodLocal ctxSLML2 = (EJBMethodLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLML2 is SLBean2", ctxSLML2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean2");
        ++testpoint;

        envName = "ejb/SLM2";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodRemote ctxSLMR2 = (EJBMethodRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLMR2 is SLBean2", ctxSLMR2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean2");
        ++testpoint;

        envName = "ejb/SFMLocal2";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodLocal ctxSFML2 = (EJBMethodLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFML2 is SFBean2", ctxSFML2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean2");
        ++testpoint;

        envName = "ejb/SFM2";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodRemote ctxSFMR2 = (EJBMethodRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFMR2 is SFBean2", ctxSFMR2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean2");
        ++testpoint;

        envName = "ejb/SLMLocal3";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodLocal ctxSLML3 = (EJBMethodLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLML3 is SLBean3", ctxSLML3.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean3");
        ++testpoint;

        envName = "ejb/SLM3";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodRemote ctxSLMR3 = (EJBMethodRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLMR3 is SLBean3", ctxSLMR3.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean3");
        ++testpoint;

        envName = "ejb/SFMLocal3";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodLocal ctxSFML3 = (EJBMethodLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFML3 is SFBean3", ctxSFML3.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean3");
        ++testpoint;

        envName = "ejb/SFM3";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodRemote ctxSFMR3 = (EJBMethodRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFMR3 is SFBean3", ctxSFMR3.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean3");
        ++testpoint;

        envName = "ejb/SLMLocal4";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodLocal ctxSLML4 = (EJBMethodLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLML4 is SLBean4", ctxSLML4.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean4");
        ++testpoint;

        envName = "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLSTestMethodBean/SLMethodRemote4";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodRemote ctxSLMR4 = (EJBMethodRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLMR4 is SLBean4", ctxSLMR4.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean4");
        ++testpoint;

        envName = "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLSTestMethodBean/SFMethodLocal4";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodLocal ctxSFML4 = (EJBMethodLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFML4 is SFBean4", ctxSFML4.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean4");
        ++testpoint;

        envName = "ejb/SFM4";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodRemote ctxSFMR4 = (EJBMethodRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFMR4 is SFBean4", ctxSFMR4.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean4");
        ++testpoint;

        envName = "ejb/SLMAssign";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethod ctxSLMAssign = (EJBMethod) bean;
        bean = null;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "ctxSLMAssign is Method", ctxSLMAssign.isMethodLevel());
        ++testpoint;

        envName = "ejb/SFMAssign";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethod ctxSFMAssign = (EJBMethod) bean;
        bean = null;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "ctxSFMAssign is Method", ctxSFMAssign.isMethodLevel());
        ++testpoint;

        return PASSED;
    }

    /**
     * Verify EJB Injection (field or method) occurred properly.
     **/
    public String verifyEJB21Injection(int testpoint) {
        return PASSED;
    }

    /**
     * Verify No EJB Injection (field or method) occurred when
     * an method is called using an instance from the pool (sl) or cache (sf).
     **/
    public String verifyNoEJB21Injection(int testpoint) {
        return PASSED;
    }

    /**
     * Verify EJB Injection (field or method) occurred properly.
     **/
    public String verifyAdvEJB30Injection(int testpoint) {
        String envName = null;
        @SuppressWarnings("unused")
        String currBean = null;

        // Assert that all of the EJBs are injected properly from XML
        currBean = "SLMethodUnique";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodUnique is injected, so auto-link within jar works", SLMethodUnique.isUnique());
        ++testpoint;

        currBean = "SLMethodSame";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodSame is injected", SLMethodSame.isLocal());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLMethodSame is SameBean4", SLMethodSame.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SameBean4");
        ++testpoint;

        currBean = "SLMethodLocalOther";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodLocalOther is Local", SLMethodLocalOther.isLocal());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodLocalOther is a Method injection", SLMethodLocalOther.isMethodLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLMethodLocalOther is OtherBean", SLMethodLocalOther.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbo.OtherBean");
        ++testpoint;

        currBean = "SLMethodOther";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodOther is Remote", SLMethodOther.isRemote());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodOther is a Method injection", SLMethodOther.isMethodLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLMethodOther is OtherBean", SLMethodOther.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbo.OtherBean");
        ++testpoint;

        currBean = "SLMethodAuto";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodAuto is injected, so auto-link to another jar works", SLMethodAuto.isAuto());
        ++testpoint;

        currBean = "SLMethodOtherIdentical";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodOtherIdentical is injected", SLMethodOtherIdentical.isIdentical());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLMethodOtherIdentical is OtherIdenticalBean", SLMethodOtherIdentical.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbo.OtherIdenticalBean");
        ++testpoint;

        currBean = "SLMethodIdentical";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLMethodIdentical is injected", SLMethodIdentical.isIdentical());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLMethodIdentical is IdenticalBean", SLMethodIdentical.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.IdenticalBean");
        ++testpoint;

        currBean = "SLMethodNested";
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLMethodNested.addMethod() = 642: " + SLMethodNested.addMethod(0), SLMethodNested.addMethod(0), 642);
        ++testpoint;

        // Next, ensure the above may be looked up in the global namespace
        try {
            Context initCtx = new InitialContext();
            Context myEnv = (Context) initCtx.lookup("java:comp/env");

            envName = "ejb/SLMUnique";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBUnique jndiSLMUnique = (EJBUnique) bean;
            bean = null;
            assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                       "jndiSLMUnique is Unique", jndiSLMUnique.isUnique());
            ++testpoint;

            envName = "ejb/SameName";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodLocal jndiSame = (EJBMethodLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSame is SameBean4", jndiSame.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SameBean4");
            ++testpoint;

            envName = "ejb/SLMOtherLocal";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodLocal jndiSLMOtherL = (EJBMethodLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLMOtherL is OtherBean", jndiSLMOtherL.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbo.OtherBean");
            ++testpoint;

            envName = "ejb/SLMOther";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBMethodRemote jndiSLMOther = (EJBMethodRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLMOther is OtherBean", jndiSLMOther.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbo.OtherBean");
            ++testpoint;

            envName = "ejb/SLMAuto";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBAuto jndiSLMAuto = (EJBAuto) bean;
            bean = null;
            assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                       "jndiSLMAuto is Auto", jndiSLMAuto.isAuto());
            ++testpoint;

            envName = "ejb/SLMIdentical1";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBIdentical jndiSLMIdentical1 = (EJBIdentical) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLMIdentical1 is OtherIdenticalBean", jndiSLMIdentical1.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbo.OtherIdenticalBean");
            ++testpoint;

            envName = "ejb/SLMIdentical2";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBIdentical jndiSLMIdentical2 = (EJBIdentical) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLMIdentical2 is IdenticalBean", jndiSLMIdentical2.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.IdenticalBean");
            ++testpoint;
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            throw new EJBException("Unexpected NamingException : " + ex);
        }

        // Next, ensure the above may be looked up from the SessionContext
        envName = "ejb/SLMUnique";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBUnique ctxSLMUnique = (EJBUnique) bean;
        bean = null;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "ctxSLMUnique is Unique", ctxSLMUnique.isUnique());
        ++testpoint;

        envName = "ejb/SameName";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodLocal ctxSame = (EJBMethodLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSame is SameBean4", ctxSame.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SameBean4");
        ++testpoint;

        envName = "ejb/SLMOtherLocal";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodLocal ctxSLMOtherL = (EJBMethodLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLMOtherL is OtherBean", ctxSLMOtherL.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbo.OtherBean");
        ++testpoint;

        envName = "ejb/SLMOther";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBMethodRemote ctxSLMOther = (EJBMethodRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLMOther is OtherBean", ctxSLMOther.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbo.OtherBean");
        ++testpoint;

        envName = "ejb/SLMAuto";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBAuto ctxSLMAuto = (EJBAuto) bean;
        bean = null;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "ctxSLMAuto is Auto", ctxSLMAuto.isAuto());
        ++testpoint;

        envName = "ejb/SLMIdentical1";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBIdentical ctxSLMIdentical1 = (EJBIdentical) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLMIdentical1 is OtherIdenticalBean", ctxSLMIdentical1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbo.OtherIdenticalBean");
        ++testpoint;

        envName = "ejb/SLMIdentical2";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBIdentical ctxSLMIdentical2 = (EJBIdentical) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLMIdentical2 is IdenticalBean", ctxSLMIdentical2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.IdenticalBean");
        ++testpoint;

        return PASSED;
    }

    /**
     * Verify EJB Injection (field or method) occurred properly.
     **/
    public String verifyAdvEJB21Injection(int testpoint) {
        return PASSED;
    }

    /* Added this method for SLSB to provide a way for a client to remove a bean from the cache */
    public void discardInstance() {
        throw new javax.ejb.EJBException("discardInstance");
    }

    public SLSTestMethodBean() {
        // intentionally blank
    }
}
