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
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Basic Stateless bean that should be able inject a named ManagedBean
 * that also has injection and a PreDestroy.
 **/
@Stateless(name = "StatelessEJBforPreDestroyManaged")
@TransactionManagement(TransactionManagementType.BEAN)
public class StatelessEJBforPreDestroyManagedBean implements StatelessEJBforTestingManagedBean {
    private static final String CLASS_NAME = StatelessEJBforPreDestroyManagedBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final String BEAN = "PreDestroyManagedBean";

    @Resource(name = "EJBName")
    protected String ivEJBName = "StatelessEJBforPreDestroyManaged";

    @Resource
    private SessionContext ivEjbContext;

    @Resource(name = "ManagedBean")
    private PreDestroyManagedBean ivMB;

    /**
     * Verifies that a simple ManagedBean is properly injected per the
     * configuration of the bean, and that the ManagedBean may be
     * looked up, since it has a name.
     **/
    @Override
    public void verifyManagedBeanInjectionAndLookup() {
        svLogger.info("> " + ivEJBName + ".verifyManagedBeanInjectionAndLookup()");

        PreDestroyManagedBean globalMB = null, appMB = null, modMB = null;
        String globalLookupName = "java:global/" + APP + "/" + MOD + "/" + BEAN;
        String appLookupName = "java:app/" + MOD + "/" + BEAN;
        String modLookupName = "java:module" + "/" + BEAN;

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
            throw new EJBException("Unexpected Exception : " + ex, ex);
        }

        assertNotNull("null returned on java:app ManagedBean lookup", appMB);

        try {
            modMB = (PreDestroyManagedBean) new InitialContext().lookup(modLookupName);
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

        PreDestroyManagedBean mb = (PreDestroyManagedBean) ivEjbContext.lookup("ManagedBean");

        assertNotNull("ManagedBean ref failed on EJBContext.lookup", mb);

        assertNotSame("Injected and looked up are equal.. oops.",
                      ivMB.getIdentifier(), mb.getIdentifier());

        // -----------------------------------------------------------------------
        //
        // Verify PostConstruct was called on the ManagedBeans
        //
        // -----------------------------------------------------------------------

        assertEquals("Unexpected initial value", "PreDestroyManagedBean.INITIAL_VALUE", ivMB.getValue());
        assertEquals("Unexpected initial value", "PreDestroyManagedBean.INITIAL_VALUE", mb.getValue());

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
            ivEjbContext.lookup("ManagedBean");

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
    }
}
