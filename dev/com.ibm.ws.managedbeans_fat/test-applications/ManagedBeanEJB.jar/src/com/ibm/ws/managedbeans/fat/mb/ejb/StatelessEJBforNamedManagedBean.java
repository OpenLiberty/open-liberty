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
package com.ibm.ws.managedbeans.fat.mb.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Basic Stateless bean that should be able inject a named ManagedBean.
 **/
@Stateless(name = "StatelessEJBforNamedManaged")
public class StatelessEJBforNamedManagedBean implements StatelessEJBforTestingManagedBean {
    private static final String CLASS_NAME = StatelessEJBforNamedManagedBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final String BEAN = "NamedManagedBean";

    @Resource(name = "EJBName")
    protected String ivEJBName = "StatelessEJBforNamedManaged";

    @Resource
    private SessionContext ivEjbContext;

    @Resource(name = "ManagedBean")
    private NamedManagedBean ivMB;

    /**
     * Verifies that a simple ManagedBean is properly injected per the
     * configuration of the bean, and that the ManagedBean may be
     * looked up, since it has a name.
     **/
    @Override
    public void verifyManagedBeanInjectionAndLookup() {
        svLogger.info("> " + ivEJBName + ".verifyManagedBeanInjectionAndLookup()");

        NamedManagedBean globalMB = null, appMB = null, modMB = null;
        String globalLookupName = "java:global/" + APP + "/" + MOD + "/" + BEAN;
        String appLookupName = "java:app/" + MOD + "/" + BEAN;
        String modLookupName = "java:module" + "/" + BEAN;

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
            throw new EJBException("Unexpected Exception : " + ex, ex);
        }

        assertNotNull("null returned on java:app ManagedBean lookup", appMB);

        try {
            modMB = (NamedManagedBean) new InitialContext().lookup(modLookupName);
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

        NamedManagedBean mb = (NamedManagedBean) ivEjbContext.lookup("ManagedBean");

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

        svLogger.info("< " + ivEJBName + ".verifyManagedBeanInjectionAndLookup()");
    }
}
