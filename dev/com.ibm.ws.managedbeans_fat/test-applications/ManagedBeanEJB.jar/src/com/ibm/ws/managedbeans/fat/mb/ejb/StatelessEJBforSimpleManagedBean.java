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
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Basic Stateless bean that should be able inject a simple ManagedBean.
 **/
@Stateless(name = "StatelessEJBforSimpleManaged")
public class StatelessEJBforSimpleManagedBean implements StatelessEJBforTestingManagedBean {
    private static final String CLASS_NAME = StatelessEJBforSimpleManagedBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final String BEAN = "SimpleManagedBean";

    @Resource(name = "EJBName")
    protected String ivEJBName = "StatelessEJBforSimpleManaged";

    @Resource
    private SessionContext ivEjbContext;

    @Resource(name = "ManagedBean")
    private SimpleManagedBean ivMB;

    /**
     * Verifies that a simple ManagedBean is properly injected per the
     * configuration of the bean, and that the ManagedBean may not be
     * looked up, since it has no name.
     **/
    @Override
    public void verifyManagedBeanInjectionAndLookup() {
        svLogger.info("> " + ivEJBName + ".verifyManagedBeanInjectionAndLookup()");

        SimpleManagedBean mb = null;
        String globalLookupName = "java:global/" + APP + "/" + MOD + "/" + BEAN;
        String appLookupName = "java:app/" + MOD + "/" + BEAN;
        String modLookupName = "java:module" + "/" + BEAN;

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

        assertNotNull("ManagedBean not injected", ivMB);

        mb = (SimpleManagedBean) ivEjbContext.lookup("ManagedBean");

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
