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
package com.ibm.ws.injection.resref.web;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import junit.framework.AssertionFailedError;

public class ResRefTestHelper {
    /*
     * MAIN TEST METHODS. SHOULD NOT BE CALLED DIRECTLY BY LISTENERS AND
     * FILTERS. INSTEAD, USE THE WRAPPERS PROVIDED BELOW. ONE MAY CALL THESE
     * DIRECTLY FROM A SERVLET.
     */

    /**
     * Tests basic functionality of a DataSource by attempting to get a non-null
     * connection from it.
     *
     * This method contains assert statements and will throw
     * AssertionFailedErrors. In these tests, it should be used directly by
     * subclasses of FATServlet or indirectly through processRequest by servlet
     * listeners and filters.
     *
     * @param name
     *            The name of the datasource variable
     * @param ds
     *            The datasource to test
     * @param throwFailure
     *            Whether or not to throw an AssertionFailedError instead of
     *            returning an error string.
     * @return
     */
    public static String testDataSource(DataSource ds, String name) {
        Connection conn = null;
        try {
            assertNotNull("ERROR - The DataSource is null - " + name, ds);
            conn = ds.getConnection();
            assertNotNull("ERROR - The connection was null from - " + name, conn);
            conn.close();
            return "PASS:The DataSource was successfully injected - " + name;
        } catch (SQLException e) {
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
        String event;
        try {
            InitialContext ctx = new InitialContext();
            Object obj = ctx.lookup(env + name);
            assertNotNull("Could not find \"" + name + "\" in the context.", obj);
            assertTrue(obj instanceof DataSource);
            event = testDataSource((DataSource) obj, name);
            return event + "(from testJNDILookup)";
        } catch (NamingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /* METHODS FOR LISTENERS AND FILTERS ONLY */

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
     * and runs the test.
     *
     * This method handles AssertionFailedErrors and should therefore not be
     * used by subclasses of FATServlet.
     *
     * @param key
     *            The key for WCEventTracker
     * @param map
     *            A HashMap of the DataSources to be tested with the key being
     *            the object's name
     */
    public static void processRequest(String key, HashMap<String, DataSource> map) {
        Set<String> set = map.keySet();
        String event = "";
        for (String name : set) {
            try {
                event = ResRefTestHelper.testDataSource(map.get(name), name);
            } catch (AssertionFailedError afe) {
                StringWriter sw = new StringWriter();
                afe.printStackTrace(new PrintWriter(sw));
                event = "FAIL:" + sw.toString();
            }

            WCEventTracker.addEvent(key, event);
        }
    }
}