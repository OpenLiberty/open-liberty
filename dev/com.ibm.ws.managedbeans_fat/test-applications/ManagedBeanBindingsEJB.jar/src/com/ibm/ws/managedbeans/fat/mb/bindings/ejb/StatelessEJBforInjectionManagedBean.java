/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedbeans.fat.mb.bindings.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Basic Stateless bean that should be able inject a named ManagedBean
 * that also has injection.
 **/
@Stateless(name = "StatelessEJBforInjectionManaged")
@TransactionManagement(TransactionManagementType.BEAN)
public class StatelessEJBforInjectionManagedBean implements StatelessEJBforTestingManagedBean {
    private static final String CLASS_NAME = StatelessEJBforInjectionManagedBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final String BEAN = "InjectionManagedBean";

    @Resource(name = "EJBName")
    protected String ivEJBName = "StatelessEJBforInjectionManaged";

    @Resource
    private SessionContext ivEjbContext;

    @Resource(name = "ManagedBean")
    private InjectionManagedBean ivMB;

    /**
     * Verifies that a simple ManagedBean is properly injected per the
     * configuration of the bean, and that the ManagedBean may be
     * looked up, since it has a name.
     **/
    @Override
    public void verifyManagedBeanInjectionAndLookup() {
        svLogger.info("> " + ivEJBName + ".verifyManagedBeanInjectionAndLookup()");

        InjectionManagedBean globalMB = null, appMB = null, modMB = null;
        String globalLookupName = "java:global/" + APP + "/" + MOD + "/" + BEAN;
        String appLookupName = "java:app/" + MOD + "/" + BEAN;
        String modLookupName = "java:module" + "/" + BEAN;

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
            throw new EJBException("Unexpected Exception : " + ex, ex);
        }

        assertNotNull("null returned on java:app ManagedBean lookup", appMB);

        try {
            modMB = (InjectionManagedBean) new InitialContext().lookup(modLookupName);
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            throw new EJBException("Unexpected Exception : " + ex, ex);
        }

        assertNotNull("null returned on java:mod ManagedBean lookup", modMB);

        // -----------------------------------------------------------------------
        //
        // Verify injected, and ref may be looked up (and not equal)
        //
        // -----------------------------------------------------------------------

        assertNotNull("ManagedBean not injected", ivMB);

        InjectionManagedBean mb = (InjectionManagedBean) ivEjbContext.lookup("ManagedBean");

        assertNotNull("ManagedBean ref failed on EJBContext.lookup", mb);

        assertNotSame("Injected and looked up are equal.. oops.",
                      ivMB.getIdentifier(), mb.getIdentifier());

        // -----------------------------------------------------------------------
        //
        // Verify ManagedBeans are stateful
        //
        // -----------------------------------------------------------------------

        ivMB.setValue("Scooby");
        mb.setValue("Shaggy");

        assertEquals("State of injected ManagedBean not correct",
                     "Scooby", ivMB.getValue());
        assertEquals("State of looked up ManagedBean not correct",
                     "Shaggy", mb.getValue());

        // -----------------------------------------------------------------------
        //
        // Verify UserTransaction was injected into ManagedBean
        //
        // -----------------------------------------------------------------------

        assertNotNull("UserTransaction not injected into ManagedBean", ivMB.getUserTransaction());

        svLogger.info("< " + ivEJBName + ".verifyManagedBeanInjectionAndLookup()");
    }
}
