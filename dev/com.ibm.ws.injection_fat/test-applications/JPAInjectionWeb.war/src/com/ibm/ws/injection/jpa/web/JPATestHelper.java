/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection.jpa.web;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import junit.framework.AssertionFailedError;

public class JPATestHelper {
    /**
     * First, checks to see if the EntityManager is null. Then calls
     * setFlushMode on it and checks to see that the FlushMode was set. This is
     * just to ensure that an EntityManager was injected.
     *
     * This method contains assert statements and will throw
     * AssertionFailedErrors. In these tests, it should be used directly by
     * subclasses of FATServlet or indirectly through processRequest by servlet
     * listeners and filters.
     *
     * @param em
     * @param phrase
     */
    public static String testEntityManager(EntityManager em, String name) {
        assertNotNull("The " + name + " persistence context was null.", em);
        Object delegate = em.getDelegate();
        assertNotNull("The Underlying implementation of " + name + " was null.", delegate);
        return "PASS:EntityManager succesfully injected - " + name;
    }

    /**
     * First tests if the EntityManagerFactory is null. Then attempts to create
     * a EntityManager. This check is to insure an EntityManagerFactory was
     * injected.
     *
     * This method contains assert statements and will throw
     * AssertionFailedErrors. In these tests, it should be used directly by
     * subclasses of FATServlet or indirectly through processRequest by servlet
     * listeners and filters.
     *
     * @param emf
     *            The EntityManagerFactory to test
     * @param phrase
     *            A Phrase describing the EntityManagerFactory for error
     *            messages
     */
    public static String testEntityManagerFactory(EntityManagerFactory emf, String name) {
        assertNotNull("The " + name + " persistence unit was null.", emf);
        EntityManager em = emf.createEntityManager();
        assertNotNull("The entity manager created from the " + name + " persistence unit was null:", em);
        em.close();
        return "PASS:EntityManagerFactory succesfully injected - " + name;
    }

    /**
     * Takes a name in the JNDI namespace relative to java:comp/env/. It will
     * ensure that name can be found in the JNDI namespace and proceed to test
     * the resource obtained from the lookup.
     *
     * This method contains assert statements and will throw
     * AssertionFailedErrors. In these tests, it should be used directly by
     * subclasses of FATServlet or indirectly through testJNDILookupWrapper by
     * servlet listeners and filters.
     *
     * @param name
     *            The JNDI to lookup
     * @return A WCEventTracker event string.
     */
    public static String testJNDILookup(String name) {
        String env = "java:comp/env/";
        String event = "";
        try {
            InitialContext ctx = new InitialContext();
            Object obj = ctx.lookup(env + name);
            assertNotNull("Could not find \"" + name + "\" in the context.", obj);
            if (obj instanceof EntityManager) {
                event = testEntityManager((EntityManager) obj, name);
            } else if (obj instanceof EntityManagerFactory) {
                event = testEntityManagerFactory((EntityManagerFactory) obj, name);
            } else {
                fail("The resulting object from the lookup was an invalid type - " + obj.getClass());
            }
            return event + "(from testJNDILookup)";
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /* THESE METHODS ARE FOR LISTENER AND FILTER USE ONLY */

    /**
     * This is a wrapper method for testJNDILookup for use by servlet listeners
     * and filters. It takes an array of JNDI names and tests each one through
     * testJNDILookup. It then adds the results to WCEventTracker with the given
     * key.
     *
     * This method handles AssertionFailedErrors and should therefore not be
     * used by subclasses of FATServlet.
     */
    public static void testJNDILookupWrapper(String key, String[] names) {
        String event;
        for (int i = 0; i < names.length; i++) {
            try {
                event = testJNDILookup(names[i]);
                WCEventTracker.addEvent(key, event);
            } catch (AssertionFailedError afe) {
                StringWriter sw = new StringWriter();
                afe.printStackTrace(new PrintWriter(sw));
                event = "FAIL:" + sw.toString();
            }
            WCEventTracker.addEvent(key, event);
            event = "";
        }
    }

    /**
     * A function that is used by listeners and filters to process the request
     * and runs the test.
     *
     * This method handles AssertionFailedErrors and should therefore not be
     * used by subclasses of FATServlet.
     *
     * @param key
     *            The key for WCEventTracker
     * @param map
     *            A HashMap of the objects to be tested with the key being the
     *            object's name
     */
    public static void processRequest(String key, HashMap<String, Object> map) {
        Set<String> set = map.keySet();
        String event;
        for (String name : set) {
            try {
                Object obj = map.get(name);
                assertNotNull(obj);
                if (obj instanceof EntityManager) {
                    event = JPATestHelper.testEntityManager((EntityManager) obj, name);
                } else if (obj instanceof EntityManagerFactory) {
                    event = JPATestHelper.testEntityManagerFactory((EntityManagerFactory) obj, name);
                } else {
                    event = "FAIL:Test entity not of type \"EntityManager\" or \"EntityManagerFactory\"";
                }
            } catch (AssertionFailedError afe) {
                StringWriter sw = new StringWriter();
                afe.printStackTrace(new PrintWriter(sw));
                event = "FAIL:" + sw.toString();
            }
            WCEventTracker.addEvent(key, event);
            event = "";
        }
    }
}