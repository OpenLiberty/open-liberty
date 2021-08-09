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
package com.ibm.ws.injection.transaction.web;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import junit.framework.AssertionFailedError;

public class TransactionTestHelper {
    /**
     * Tests if a UserTransaction is null and basic functionality.
     *
     * This method contains assert statements and will throw
     * AssertionFailedErrors. In these tests, it should be used directly by
     * subclasses of FATServlet or indirectly through processRequest by servlet
     * listeners and filters.
     *
     * @param tx
     *            The UserTransaction to test.
     * @param name
     *            The name of the UserTransaction
     * @param Whether
     *            or not this should throw an AssertionFailedError instead of an
     *            error string.
     * @return An event string describing the success or failure of the test.
     */
    public static String testUserTransaction(UserTransaction tx, String name) {
        try {
            assertNotNull("The UserTransaction was null - " + name, tx);
            assertEquals("The thread is incorrectly associated with a transaction:" + name, Status.STATUS_NO_TRANSACTION, tx.getStatus());
            tx.begin();
            assertEquals("The transaction was not active:" + name, Status.STATUS_ACTIVE, tx.getStatus());
            tx.commit();
            // After commit the thread is no longer associated with a transaction
            assertEquals("The transaction was not commited:" + name, Status.STATUS_NO_TRANSACTION, tx.getStatus());
            tx.begin();
            tx.setRollbackOnly();
            assertEquals("The transaction was not set for rollback:" + name, Status.STATUS_MARKED_ROLLBACK, tx.getStatus());
            tx.rollback();
            // After rollback the thread is no longer associated with a transaction
            assertEquals("The transaction was not rolledback:" + name, Status.STATUS_NO_TRANSACTION, tx.getStatus());
            return "PASS:The UserTransaction was successfully tested - " + name;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Tests if a TransactionSynchronizationRegistry is null and basic
     * functionality.
     *
     * This method contains assert statements and will throw
     * AssertionFailedErrors. In these tests, it should be used directly by
     * subclasses of FATServlet or indirectly through processRequest by servlet
     * listeners and filters.
     *
     * @param tsr
     *            The TransactionSynchronizationRegistry to test.
     * @param name
     *            The name of the TransactionSynchronizationRegistry
     * @return An event string describing the success or failure of the test.
     */
    public static String testTranSynchRegistry(TransactionSynchronizationRegistry tsr, String name) {
        try {
            assertNotNull("The TranSynchRegistry was null - " + name, tsr);
            assertNull("The TranSynchRegistry incorrectly had a Transaction Key:" + name, tsr.getTransactionKey());
            assertEquals("The TranSynchRegistry incorrectly had a Transaction status other than No Transaction:" + name, Status.STATUS_NO_TRANSACTION, tsr.getTransactionStatus());
            return "PASS:The TranSynchRegistry was successfully tested - " + name;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        try {
            String event = "";
            InitialContext ctx = new InitialContext();
            Object obj = ctx.lookup(env + name);
            assertNotNull("Could not find \"" + name + "\" in the context.", obj);
            assertTrue(obj instanceof UserTransaction || obj instanceof TransactionSynchronizationRegistry);
            if (obj instanceof UserTransaction) {
                event = testUserTransaction((UserTransaction) obj, name);
            } else if (obj instanceof TransactionSynchronizationRegistry) {
                event = testTranSynchRegistry((TransactionSynchronizationRegistry) obj, name);
            } else {
                fail("The object was not of type \"UserTransaction\" or \"TransactionSynchronizationRegistry\". Instead it was \"" + obj.getClass() + "\"");
            }
            return event + " (from testJNDILookup)";
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /* METHODS ONLY FOR LISTENERS AND FILTERS */

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
            } catch (AssertionFailedError afe) {
                StringWriter sw = new StringWriter();
                afe.printStackTrace(new PrintWriter(sw));
                event = "FAIL:" + sw.toString();
            }

            WCEventTracker.addEvent(key, event);
        }
    }

    /**
     * A function that is used by listeners and filters to process the request
     * and runs the test. If the tests pass, a string will be returned. If the
     * tests fail, an error will be throw if throwFailure is true. Otherwise, a
     * failure string is returned.
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
        String event = "";
        for (String name : set) {
            try {
                Object obj = map.get(name);
                assertNotNull(obj);
                if (obj instanceof UserTransaction) {
                    event = TransactionTestHelper.testUserTransaction((UserTransaction) obj, name);
                } else if (obj instanceof TransactionSynchronizationRegistry) {
                    event = TransactionTestHelper.testTranSynchRegistry((TransactionSynchronizationRegistry) obj, name);
                } else {
                    fail("The object was not of type \"UserTransaction\" or \"TransactionSynchronizationRegistry\". Instead it was \"" + obj.getClass() + "\"");
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