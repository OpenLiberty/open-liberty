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
public class SLSTestFieldBean {
    private static final String PASSED = "Passed";

    public Object bean;

    private EJBFieldLocal SLFieldLocal1;
    public EJBFieldRemote SLFieldRemote1;
    private EJBFieldLocal SFFieldLocal1;
    public EJBFieldRemote SFFieldRemote1;
    protected EJBFieldLocal SLFieldLocal2;
    EJBFieldRemote SLFieldRemote2;
    protected EJBFieldLocal SFFieldLocal2;
    EJBFieldRemote SFFieldRemote2;
    public EJBFieldLocal SLFieldLocal3;
    private EJBFieldRemote SLFieldRemote3;
    public EJBFieldLocal SFFieldLocal3;
    private EJBFieldRemote SFFieldRemote3;
    EJBFieldLocal SLFieldLocal4;
    protected EJBFieldRemote SLFieldRemote4;
    EJBFieldLocal SFFieldLocal4;
    protected EJBFieldRemote SFFieldRemote4;

    private EJBField SLField1;
    public EJBField SFField1;

    private EJBUnique SLFieldUnique;
    public EJBFieldLocal SLFieldSame;
    private EJBFieldRemote SLFieldOther;
    public EJBFieldLocal SLFieldLocalOther;
    EJBAuto SLFieldAuto;
    EJBIdentical SLFieldOtherIdentical;
    protected EJBIdentical SLFieldIdentical;
    protected EJBNested SLFieldNested;

    private SessionContext ivContext;

    /**
     * Verify EJB Injection (field or method) occurred properly.
     **/
    public String verifyEJB30Injection(int testpoint) {
        String envName = null;
        @SuppressWarnings("unused")
        String currBean = null;

        // Assert that all of the EJBs are injected properly from XML

        currBean = "SLFieldLocal1";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldLocal1 is Local", SLFieldLocal1.isLocal());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldLocal1 is a Field injection", SLFieldLocal1.isFieldLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLFieldLocal1 is SLBean1", SLFieldLocal1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean1");
        ++testpoint;

        currBean = "SLFieldRemote1";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldRemote1 is Remote", SLFieldRemote1.isRemote());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldRemote1 is a Field injection", SLFieldRemote1.isFieldLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLFieldRemote1 is SLBean1", SLFieldRemote1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean1");
        ++testpoint;

        currBean = "SFFieldLocal1";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldLocal1 is Local", SFFieldLocal1.isLocal());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldLocal1 is a Field injection", SFFieldLocal1.isFieldLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SFFieldLocal1 is SFBean1", SFFieldLocal1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean1");
        ++testpoint;

        currBean = "SFFieldRemote1";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldRemote1 is Remote", SFFieldRemote1.isRemote());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldRemote1 is a Field injection", SFFieldRemote1.isFieldLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SFFieldRemote1 is SFBean1", SFFieldRemote1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean1");
        ++testpoint;

        currBean = "SLFieldLocal2";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldLocal2 is Local", SLFieldLocal2.isLocal());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldLocal2 is a Field injection", SLFieldLocal2.isFieldLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLFieldLocal2 is SLBean2", SLFieldLocal2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean2");
        ++testpoint;

        currBean = "SLFieldRemote2";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldRemote2 is Remote", SLFieldRemote2.isRemote());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldRemote2 is a Field injection", SLFieldRemote2.isFieldLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLFieldRemote2 is SLBean2", SLFieldRemote2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean2");
        ++testpoint;

        currBean = "SFFieldLocal2";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldLocal2 is Local", SFFieldLocal2.isLocal());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldLocal2 is a Field injection", SFFieldLocal2.isFieldLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SFFieldLocal2 is SFBean2", SFFieldLocal2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean2");
        ++testpoint;

        currBean = "SFFieldRemote2";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldRemote2 is Remote", SFFieldRemote2.isRemote());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldRemote2 is a Field injection", SFFieldRemote2.isFieldLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SFFieldRemote2 is SFBean2", SFFieldRemote2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean2");
        ++testpoint;

        currBean = "SLFieldLocal3";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldLocal3 is Local", SLFieldLocal3.isLocal());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldLocal3 is a Field injection", SLFieldLocal3.isFieldLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLFieldLocal3 is SLBean3", SLFieldLocal3.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean3");
        ++testpoint;

        currBean = "SLFieldRemote3";
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldRemote3 was not injected", SLFieldRemote3);
        ++testpoint;

        currBean = "SFFieldLocal3";
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldLocal3 was not injected", SFFieldLocal3);
        ++testpoint;

        currBean = "SFFieldRemote3";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldRemote3 is Remote", SFFieldRemote3.isRemote());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldRemote3 is a Field injection", SFFieldRemote3.isFieldLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SFFieldRemote3 is SFBean3", SFFieldRemote3.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean3");
        ++testpoint;

        currBean = "SLFieldLocal4";
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldLocal4 was not injected", SLFieldLocal4);
        ++testpoint;

        currBean = "SLFieldRemote4";
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldRemote4 was not injected", SLFieldRemote4);
        ++testpoint;

        currBean = "SFFieldLocal4";
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldLocal4 was not injected", SFFieldLocal4);
        ++testpoint;

        currBean = "SFFieldRemote4";
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldRemote4 was not injected", SFFieldRemote4);
        ++testpoint;

        currBean = "SLField1";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLField1 is a Field injection", SLField1.isFieldLevel());
        ++testpoint;

        currBean = "SFField1";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFField1 is a Field injection", SFField1.isFieldLevel());
        ++testpoint;

        // Next, ensure the above may be looked up in the global namespace
        try {
            Context initCtx = new InitialContext();
            Context myEnv = (Context) initCtx.lookup("java:comp/env");

            envName = "ejb/SLFLocal1";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldLocal jndiSLFL1 = (EJBFieldLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLFL1 is SLBean1", jndiSLFL1.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean1");
            ++testpoint;

            envName = "ejb/SLF1";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldRemote jndiSLFR1 = (EJBFieldRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLFR1 is SLBean1", jndiSLFR1.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean1");
            ++testpoint;

            envName = "ejb/SFFLocal1";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldLocal jndiSFFL1 = (EJBFieldLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFFL1 is SFBean1", jndiSFFL1.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean1");
            ++testpoint;

            envName = "ejb/SFF1";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldRemote jndiSFFR1 = (EJBFieldRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFFR1 is SFBean1", jndiSFFR1.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean1");
            ++testpoint;

            envName = "ejb/SLFLocal2";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldLocal jndiSLFL2 = (EJBFieldLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLFL2 is SLBean2", jndiSLFL2.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean2");
            ++testpoint;

            envName = "ejb/SLF2";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldRemote jndiSLFR2 = (EJBFieldRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLFR2 is SLBean2", jndiSLFR2.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean2");
            ++testpoint;

            envName = "ejb/SFFLocal2";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldLocal jndiSFFL2 = (EJBFieldLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFFL2 is SFBean2", jndiSFFL2.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean2");
            ++testpoint;

            envName = "ejb/SFF2";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldRemote jndiSFFR2 = (EJBFieldRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFFR2 is SFBean2", jndiSFFR2.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean2");
            ++testpoint;

            envName = "ejb/SLFLocal3";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldLocal jndiSLFL3 = (EJBFieldLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLFL3 is SLBean3", jndiSLFL3.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean3");
            ++testpoint;

            envName = "ejb/SLF3";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldRemote jndiSLFR3 = (EJBFieldRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLFR3 is SLBean3", jndiSLFR3.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean3");
            ++testpoint;

            envName = "ejb/SFFLocal3";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldLocal jndiSFFL3 = (EJBFieldLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFFL3 is SFBean3", jndiSFFL3.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean3");
            ++testpoint;

            envName = "ejb/SFF3";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldRemote jndiSFFR3 = (EJBFieldRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFFR3 is SFBean3", jndiSFFR3.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean3");
            ++testpoint;

            envName = "ejb/SLFLocal4";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldLocal jndiSLFL4 = (EJBFieldLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLFL4 is SLBean4", jndiSLFL4.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean4");
            ++testpoint;

            envName = "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLSTestFieldBean/SLFieldRemote4";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldRemote jndiSLFR4 = (EJBFieldRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLFR4 is SLBean4", jndiSLFR4.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean4");
            ++testpoint;

            envName = "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLSTestFieldBean/SFFieldLocal4";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldLocal jndiSFFL4 = (EJBFieldLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFFL4 is SFBean4", jndiSFFL4.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean4");
            ++testpoint;

            envName = "ejb/SFF4";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldRemote jndiSFFR4 = (EJBFieldRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFFR4 is SFBean4", jndiSFFR4.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean4");
            ++testpoint;

            envName = "ejb/SLFAssign";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBField jndiSLFAssign = (EJBField) bean;
            bean = null;
            assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                       "jndiSLFAssign is Field", jndiSLFAssign.isFieldLevel());
            ++testpoint;

            envName = "ejb/SFFAssign";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBField jndiSFFAssign = (EJBField) bean;
            bean = null;
            assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                       "jndiSFFAssign is Field", jndiSFFAssign.isFieldLevel());
            ++testpoint;
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            throw new EJBException("Unexpected NamingException : " + ex);
        }

        // Next, ensure the above may be looked up from the SessionContext
        envName = "ejb/SLFLocal1";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldLocal ctxSLFL1 = (EJBFieldLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLFL1 is SLBean1", ctxSLFL1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean1");
        ++testpoint;

        envName = "ejb/SLF1";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldRemote ctxSLFR1 = (EJBFieldRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLFR1 is SLBean1", ctxSLFR1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean1");
        ++testpoint;

        envName = "ejb/SFFLocal1";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldLocal ctxSFFL1 = (EJBFieldLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFFL1 is SFBean1", ctxSFFL1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean1");
        ++testpoint;

        envName = "ejb/SFF1";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldRemote ctxSFFR1 = (EJBFieldRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFFR1 is SFBean1", ctxSFFR1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean1");
        ++testpoint;

        envName = "ejb/SLFLocal2";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldLocal ctxSLFL2 = (EJBFieldLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLFL2 is SLBean2", ctxSLFL2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean2");
        ++testpoint;

        envName = "ejb/SLF2";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldRemote ctxSLFR2 = (EJBFieldRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLFR2 is SLBean2", ctxSLFR2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean2");
        ++testpoint;

        envName = "ejb/SFFLocal2";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldLocal ctxSFFL2 = (EJBFieldLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFFL2 is SFBean2", ctxSFFL2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean2");
        ++testpoint;

        envName = "ejb/SFF2";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldRemote ctxSFFR2 = (EJBFieldRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFFR2 is SFBean2", ctxSFFR2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean2");
        ++testpoint;

        envName = "ejb/SLFLocal3";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldLocal ctxSLFL3 = (EJBFieldLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLFL3 is SLBean3", ctxSLFL3.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean3");
        ++testpoint;

        envName = "ejb/SLF3";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldRemote ctxSLFR3 = (EJBFieldRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLFR3 is SLBean3", ctxSLFR3.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean3");
        ++testpoint;

        envName = "ejb/SFFLocal3";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldLocal ctxSFFL3 = (EJBFieldLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFFL3 is SFBean3", ctxSFFL3.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean3");
        ++testpoint;

        envName = "ejb/SFF3";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldRemote ctxSFFR3 = (EJBFieldRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFFR3 is SFBean3", ctxSFFR3.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean3");
        ++testpoint;

        envName = "ejb/SLFLocal4";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldLocal ctxSLFL4 = (EJBFieldLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLFL4 is SLBean4", ctxSLFL4.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean4");
        ++testpoint;

        envName = "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLSTestFieldBean/SLFieldRemote4";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldRemote ctxSLFR4 = (EJBFieldRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLFR4 is SLBean4", ctxSLFR4.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean4");
        ++testpoint;

        envName = "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLSTestFieldBean/SFFieldLocal4";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldLocal ctxSFFL4 = (EJBFieldLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFFL4 is SFBean4", ctxSFFL4.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean4");
        ++testpoint;

        envName = "ejb/SFF4";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldRemote ctxSFFR4 = (EJBFieldRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFFR4 is SFBean4", ctxSFFR4.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean4");
        ++testpoint;

        envName = "ejb/SLFAssign";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBLocal ctxSLFAssign = (EJBLocal) bean;
        bean = null;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "ctxSLFAssign is Local", ctxSLFAssign.isLocal());
        ++testpoint;

        envName = "ejb/SFFAssign";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBRemote ctxSFFAssign = (EJBRemote) bean;
        bean = null;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "ctxSFFAssign is Remote", ctxSFFAssign.isRemote());
        ++testpoint;

        // Finally, reset all of the fields to insure injection does not occur
        // when object is re-used from the pool.
        SLFieldLocal1 = null;
        SLFieldRemote1 = null;
        SFFieldLocal1 = null;
        SFFieldRemote1 = null;
        SLFieldLocal2 = null;
        SLFieldRemote2 = null;
        SFFieldLocal2 = null;
        SFFieldRemote2 = null;
        SLFieldLocal3 = null;
        SLFieldRemote3 = null;
        SFFieldLocal3 = null;
        SFFieldRemote3 = null;
        SLFieldLocal4 = null;
        SLFieldRemote4 = null;
        SFFieldLocal4 = null;
        SFFieldRemote4 = null;

        SLField1 = null;
        SFField1 = null;

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
                   "SLFieldLocal1 was not injected twice", SLFieldLocal1);
        ++testpoint;
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldRemote1 was not injected twice", SLFieldRemote1);
        ++testpoint;

        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldLocal1 was not injected twice", SFFieldLocal1);
        ++testpoint;
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldRemote1 was not injected twice", SFFieldRemote1);
        ++testpoint;

        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldLocal2 was not injected twice", SLFieldLocal2);
        ++testpoint;
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldRemote2 was not injected twice", SLFieldRemote2);
        ++testpoint;

        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldLocal2 was not injected twice", SFFieldLocal2);
        ++testpoint;
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldRemote2 was not injected twice", SFFieldRemote2);
        ++testpoint;

        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldLocal3 was not injected twice", SLFieldLocal3);
        ++testpoint;
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldRemote3 was never injected", SLFieldRemote3);
        ++testpoint;

        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldLocal3 was never injected", SFFieldLocal3);
        ++testpoint;
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldRemote3 was not injected twice", SFFieldRemote3);
        ++testpoint;

        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldLocal4 was never injected", SLFieldLocal4);
        ++testpoint;
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldRemote4 was never injected", SLFieldRemote4);
        ++testpoint;

        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldLocal4 was never injected", SFFieldLocal4);
        ++testpoint;
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFFieldRemote4 was never injected", SFFieldRemote4);
        ++testpoint;

        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLField1 was not injected twice", SLField1);
        ++testpoint;
        assertNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SFField1 was not injected twice", SFField1);
        ++testpoint;

        // Next, ensure the above may be looked up in the global namespace
        // and return the original injected value (not modified value)
        try {
            Context initCtx = new InitialContext();
            Context myEnv = (Context) initCtx.lookup("java:comp/env");

            envName = "ejb/SLFLocal1";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldLocal jndiSLFL1 = (EJBFieldLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLFL1 is SLBean1", jndiSLFL1.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean1");
            ++testpoint;

            envName = "ejb/SLF1";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldRemote jndiSLFR1 = (EJBFieldRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLFR1 is SLBean1", jndiSLFR1.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean1");
            ++testpoint;

            envName = "ejb/SFFLocal1";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldLocal jndiSFFL1 = (EJBFieldLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFFL1 is SFBean1", jndiSFFL1.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean1");
            ++testpoint;

            envName = "ejb/SFF1";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldRemote jndiSFFR1 = (EJBFieldRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFFR1 is SFBean1", jndiSFFR1.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean1");
            ++testpoint;

            envName = "ejb/SLFLocal2";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldLocal jndiSLFL2 = (EJBFieldLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLFL2 is SLBean2", jndiSLFL2.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean2");
            ++testpoint;

            envName = "ejb/SLF2";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldRemote jndiSLFR2 = (EJBFieldRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLFR2 is SLBean2", jndiSLFR2.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean2");
            ++testpoint;

            envName = "ejb/SFFLocal2";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldLocal jndiSFFL2 = (EJBFieldLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFFL2 is SFBean2", jndiSFFL2.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean2");
            ++testpoint;

            envName = "ejb/SFF2";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldRemote jndiSFFR2 = (EJBFieldRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFFR2 is SFBean2", jndiSFFR2.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean2");
            ++testpoint;

            envName = "ejb/SLFLocal3";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldLocal jndiSLFL3 = (EJBFieldLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLFL3 is SLBean3", jndiSLFL3.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean3");
            ++testpoint;

            envName = "ejb/SLF3";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldRemote jndiSLFR3 = (EJBFieldRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLFR3 is SLBean3", jndiSLFR3.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean3");
            ++testpoint;

            envName = "ejb/SFFLocal3";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldLocal jndiSFFL3 = (EJBFieldLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFFL3 is SFBean3", jndiSFFL3.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean3");
            ++testpoint;

            envName = "ejb/SFF3";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldRemote jndiSFFR3 = (EJBFieldRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFFR3 is SFBean3", jndiSFFR3.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean3");
            ++testpoint;

            envName = "ejb/SLFLocal4";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldLocal jndiSLFL4 = (EJBFieldLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLFL4 is SLBean4", jndiSLFL4.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean4");
            ++testpoint;

            envName = "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLSTestFieldBean/SLFieldRemote4";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldRemote jndiSLFR4 = (EJBFieldRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLFR4 is SLBean4", jndiSLFR4.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean4");
            ++testpoint;

            envName = "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLSTestFieldBean/SFFieldLocal4";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldLocal jndiSFFL4 = (EJBFieldLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFFL4 is SFBean4", jndiSFFL4.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean4");
            ++testpoint;

            envName = "ejb/SFF4";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldRemote jndiSFFR4 = (EJBFieldRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSFFR4 is SFBean4", jndiSFFR4.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean4");
            ++testpoint;

            envName = "ejb/SLFAssign";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBLocal jndiSLFAssign = (EJBLocal) bean;
            bean = null;
            assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                       "jndiSLFAssign is Local", jndiSLFAssign.isLocal());
            ++testpoint;

            envName = "ejb/SFFAssign";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBRemote jndiSFFAssign = (EJBRemote) bean;
            bean = null;
            assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                       "jndiSFFAssign is Remote", jndiSFFAssign.isRemote());
            ++testpoint;
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            throw new EJBException("Unexpected NamingException : " + ex);
        }

        // Next, ensure the above may be looked up from the SessionContext
        // and return the original injected value (not modified value)

        envName = "ejb/SLFLocal1";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldLocal ctxSLFL1 = (EJBFieldLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLFL1 is SLBean1", ctxSLFL1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean1");
        ++testpoint;

        envName = "ejb/SLF1";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldRemote ctxSLFR1 = (EJBFieldRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLFR1 is SLBean1", ctxSLFR1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean1");
        ++testpoint;

        envName = "ejb/SFFLocal1";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldLocal ctxSFFL1 = (EJBFieldLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFFL1 is SFBean1", ctxSFFL1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean1");
        ++testpoint;

        envName = "ejb/SFF1";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldRemote ctxSFFR1 = (EJBFieldRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFFR1 is SFBean1", ctxSFFR1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean1");
        ++testpoint;

        envName = "ejb/SLFLocal2";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldLocal ctxSLFL2 = (EJBFieldLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLFL2 is SLBean2", ctxSLFL2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean2");
        ++testpoint;

        envName = "ejb/SLF2";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldRemote ctxSLFR2 = (EJBFieldRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLFR2 is SLBean2", ctxSLFR2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean2");
        ++testpoint;

        envName = "ejb/SFFLocal2";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldLocal ctxSFFL2 = (EJBFieldLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFFL2 is SFBean2", ctxSFFL2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean2");
        ++testpoint;

        envName = "ejb/SFF2";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldRemote ctxSFFR2 = (EJBFieldRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFFR2 is SFBean2", ctxSFFR2.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean2");
        ++testpoint;

        envName = "ejb/SLFLocal3";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldLocal ctxSLFL3 = (EJBFieldLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLFL3 is SLBean3", ctxSLFL3.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean3");
        ++testpoint;

        envName = "ejb/SLF3";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldRemote ctxSLFR3 = (EJBFieldRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLFR3 is SLBean3", ctxSLFR3.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean3");
        ++testpoint;

        envName = "ejb/SFFLocal3";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldLocal ctxSFFL3 = (EJBFieldLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFFL3 is SFBean3", ctxSFFL3.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean3");
        ++testpoint;

        envName = "ejb/SFF3";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldRemote ctxSFFR3 = (EJBFieldRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFFR3 is SFBean3", ctxSFFR3.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean3");
        ++testpoint;

        envName = "ejb/SLFLocal4";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldLocal ctxSLFL4 = (EJBFieldLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLFL4 is SLBean4", ctxSLFL4.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean4");
        ++testpoint;

        envName = "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLSTestFieldBean/SLFieldRemote4";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldRemote ctxSLFR4 = (EJBFieldRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLFR4 is SLBean4", ctxSLFR4.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLBean4");
        ++testpoint;

        envName = "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SLSTestFieldBean/SFFieldLocal4";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldLocal ctxSFFL4 = (EJBFieldLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFFL4 is SFBean4", ctxSFFL4.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean4");
        ++testpoint;

        envName = "ejb/SFF4";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldRemote ctxSFFR4 = (EJBFieldRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSFFR4 is SFBean4", ctxSFFR4.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SFBean4");
        ++testpoint;

        envName = "ejb/SLFAssign";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBField ctxSLFAssign = (EJBField) bean;
        bean = null;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "ctxSLFAssign is Field", ctxSLFAssign.isFieldLevel());
        ++testpoint;

        envName = "ejb/SFFAssign";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBField ctxSFFAssign = (EJBField) bean;
        bean = null;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "ctxSFFAssign is Field", ctxSFFAssign.isFieldLevel());
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
        currBean = "SLFieldUnique";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldUnique is injected, so auto-link within jar works", SLFieldUnique.isUnique());
        ++testpoint;

        currBean = "SLFieldSame";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldSame is injected", SLFieldSame.isLocal());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLFieldSame is SameBean7", SLFieldSame.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SameBean7");
        ++testpoint;

        currBean = "SLFieldLocalOther";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldLocalOther is Local", SLFieldLocalOther.isLocal());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldLocalOther is a Field injection", SLFieldLocalOther.isFieldLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLFieldLocalOther is OtherBean", SLFieldLocalOther.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbo.OtherBean");
        ++testpoint;

        currBean = "SLFieldOther";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldOther is Remote", SLFieldOther.isRemote());
        ++testpoint;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldOther is a Field injection", SLFieldOther.isFieldLevel());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLFieldOther is OtherBean", SLFieldOther.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbo.OtherBean");
        ++testpoint;

        currBean = "SLFieldAuto";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldAuto is injected, so auto-link to another jar works", SLFieldAuto.isAuto());
        ++testpoint;

        currBean = "SLFieldOtherIdentical";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldOtherIdentical is injected", SLFieldOtherIdentical.isIdentical());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLFieldOtherIdentical is OtherIdenticalBean", SLFieldOtherIdentical.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbo.OtherIdenticalBean");
        ++testpoint;

        currBean = "SLFieldIdentical";
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "SLFieldIdentical is injected", SLFieldIdentical.isIdentical());
        ++testpoint;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLFieldIdentical is IdenticalBean", SLFieldIdentical.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.IdenticalBean");
        ++testpoint;

        currBean = "SLFieldNested";
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "SLFieldNested.addField() = 321: " + SLFieldNested.addField(0), SLFieldNested.addField(0), 321);
        ++testpoint;

        // Next, ensure the above may be looked up in the global namespace
        try {
            Context initCtx = new InitialContext();
            Context myEnv = (Context) initCtx.lookup("java:comp/env");

            envName = "ejb/SLFUnique";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBUnique jndiSLFUnique = (EJBUnique) bean;
            bean = null;
            assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                       "jndiSLFUnique is Unique", jndiSLFUnique.isUnique());
            ++testpoint;

            envName = "ejb/SameName";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldLocal jndiSame = (EJBFieldLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSame is SameBean7", jndiSame.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SameBean7");
            ++testpoint;

            envName = "ejb/SLFOtherLocal";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldLocal jndiSLFOtherL = (EJBFieldLocal) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLFOtherL is OtherBean", jndiSLFOtherL.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbo.OtherBean");
            ++testpoint;

            envName = "ejb/SLFOther";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBFieldRemote jndiSLFOther = (EJBFieldRemote) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLFOther is OtherBean", jndiSLFOther.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbo.OtherBean");
            ++testpoint;

            envName = "ejb/SLFAuto";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBAuto jndiSLFAuto = (EJBAuto) bean;
            bean = null;
            assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                       "jndiSLFAuto is Auto", jndiSLFAuto.isAuto());
            ++testpoint;

            envName = "ejb/SLFIdentical1";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBIdentical jndiSLFIdentical1 = (EJBIdentical) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLFIdentical1 is OtherIdenticalBean", jndiSLFIdentical1.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbo.OtherIdenticalBean");
            ++testpoint;

            envName = "ejb/SLFIdentical2";
            bean = myEnv.lookup(envName);
            assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                          "lookup:" + envName, bean);
            ++testpoint;

            EJBIdentical jndiSLFIdentical2 = (EJBIdentical) bean;
            bean = null;
            assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                         "jndiSLFIdentical2 is IdenticalBean", jndiSLFIdentical2.getBeanName(),
                         "com.ibm.ws.ejbcontainer.injection.xml.ejbx.IdenticalBean");
            ++testpoint;
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            throw new EJBException("Unexpected NamingException : " + ex);
        }

        // Next, ensure the above may be looked up from the SessionContext
        envName = "ejb/SLFUnique";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBUnique ctxSLFUnique = (EJBUnique) bean;
        bean = null;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "ctxSLFUnique is Unique", ctxSLFUnique.isUnique());
        ++testpoint;

        envName = "ejb/SameName";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldLocal ctxSame = (EJBFieldLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSame is SameBean7", ctxSame.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbx.SameBean7");
        ++testpoint;

        envName = "ejb/SLFOtherLocal";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldLocal ctxSLFOtherL = (EJBFieldLocal) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLFOtherL is OtherBean", ctxSLFOtherL.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbo.OtherBean");
        ++testpoint;

        envName = "ejb/SLFOther";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBFieldRemote ctxSLFOther = (EJBFieldRemote) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLFOther is OtherBean", ctxSLFOther.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbo.OtherBean");
        ++testpoint;

        envName = "ejb/SLFAuto";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBAuto ctxSLFAuto = (EJBAuto) bean;
        bean = null;
        assertTrue(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                   "ctxSLFAuto is Auto", ctxSLFAuto.isAuto());
        ++testpoint;

        envName = "ejb/SLFIdentical1";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBIdentical ctxSLFIdentical1 = (EJBIdentical) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLFIdentical1 is OtherIdenticalBean", ctxSLFIdentical1.getBeanName(),
                     "com.ibm.ws.ejbcontainer.injection.xml.ejbo.OtherIdenticalBean");
        ++testpoint;

        envName = "ejb/SLFIdentical2";
        bean = ivContext.lookup(envName);
        assertNotNull(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                      "lookup:" + envName, bean);
        ++testpoint;

        EJBIdentical ctxSLFIdentical2 = (EJBIdentical) bean;
        bean = null;
        assertEquals(testpoint + (testpoint > 9 ? " --> " : " ---> ") +
                     "ctxSLFIdentical2 is IdenticalBean", ctxSLFIdentical2.getBeanName(),
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
        throw new EJBException("discardInstance");
    }

    public SLSTestFieldBean() {
        // intentionally blank
    }
}
