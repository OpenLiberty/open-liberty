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
package com.ibm.ws.injection.envxml.web;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.Vector;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * This is the servlet that is used to test XML injection of env-entry of the
 * boxed types (Boolean, Integer, etc.) into the fields and methods of servlet
 * listeners and filters. When a test is called on this servlet, the appropriate
 * listener/filter should be invoked. This servlet then reads its entries from
 * the WCEventTracker and reports them back to the test case.
 *
 * @author jnowosa
 *
 */
@WebServlet("/AdvEnvXMLObjServlet")
public class AdvEnvXMLObjServlet extends FATServlet {
    private static final String CLASS_NAME = AdvEnvXMLObjServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final long serialVersionUID = 1L;

    /*
     * Test Methods
     */

    /**
     * This test case specifically tests XML injection of env-entries
     * with an XML description into an HttpSessionListener.
     *
     * This test gets the results that the HttpSessionListener put into
     * WCEventTracker and reports them back to the test case. It is
     * expected that the env-entries were properly injected and should therefore
     * get passing results.
     */
    @Test
    public void testEnvXMLObjHttpSessionListener(HttpServletRequest req, HttpServletResponse resp) {
        svLogger.info("Testing Transaction Http Session Listener Injection...");
        String key = WCEventTracker.KEY_LISTENER_CREATED_AdvEnvXMLObjHttpSessionListener;
        req.getSession();
        processEvents(key);
        WCEventTracker.clearEvents(key);
    }

    /**
     * This test case specifically tests XML injection of env-entries
     * with an XML description into an HttpSessionAttributeListener.
     *
     * This test gets the results that the HttpSessionAttribute put into
     * WCEventTracker and reports them back to the test case. To do this,
     * the test must first set, replace, and remove an attribute to generate the
     * proper events on the listener. It is expected that the env-entries were
     * properly injected and should therefore get passing results.
     */
    @Test
    public void testEnvXMLObjHttpSessionAttributeListener(HttpServletRequest req, HttpServletResponse resp) {
        svLogger.info("Testing Transaction Context Attribute Listener Injection...");
        // Set a context attribute
        String key = WCEventTracker.KEY_LISTENER_ADD_AdvEnvXMLObjHttpSessionAttributeListener;
        WCEventTracker.clearEvents(key);
        req.getSession().setAttribute(WCEventTracker.KEY_ATTRIBUTE_AdvEnvXMLObjHttpSessionAttributeListener, "initial");
        processEvents(key);
        WCEventTracker.clearEvents(key);
        // Update the context attribute
        key = WCEventTracker.KEY_LISTENER_REP_AdvEnvXMLObjHttpSessionAttributeListener;
        WCEventTracker.clearEvents(key);
        req.getSession().setAttribute(WCEventTracker.KEY_ATTRIBUTE_AdvEnvXMLObjHttpSessionAttributeListener, "replaced");
        processEvents(key);
        WCEventTracker.clearEvents(key);
        // Delete the context attribute
        key = WCEventTracker.KEY_LISTENER_DEL_AdvEnvXMLObjHttpSessionAttributeListener;
        WCEventTracker.clearEvents(key);
        req.getSession().removeAttribute(WCEventTracker.KEY_ATTRIBUTE_AdvEnvXMLObjHttpSessionAttributeListener);
        processEvents(key);
        WCEventTracker.clearEvents(key);
    }

    /**
     * This test case specifically tests XML injection of env-entries
     * with an XML description into a ServletContextListener.
     *
     * This test gets the results that the ServletContextListener put into
     * WCEventTracker and reports them back to the test case. It is
     * expected that the env-entries were properly injected and should therefore
     * get passing results.
     */
    @Test
    public void testEnvXMLObjServletContextListener() {
        svLogger.info("Testing Transaction Servlet Context Listener Injection...");
        String key = WCEventTracker.KEY_LISTENER_INIT_AdvEnvXMLObjServletContextListener;
        processEvents(key);
    }

    /**
     * This test case specifically tests XML injection of env-entries
     * with an XML description into a ServletContextAttributeListener.
     *
     * This test gets the results that the ServletContextAttributeListener put into
     * WCEventTracker and reports them back to the test case. To do this,
     * the test must first set, replace, and remove an attribute to generate the
     * proper events on the listener. It is expected that the env-entries were
     * properly injected and should therefore get passing results.
     */
    @Test
    public void testEnvXMLObjServletContextAttributeListener() {
        svLogger.info("Testing Transaction Context Attribute Listener Injection...");
        // Set a context attribute
        String key = WCEventTracker.KEY_LISTENER_ADD_AdvEnvXMLObjContextAttributeListener;
        WCEventTracker.clearEvents(key);
        getServletContext().setAttribute(WCEventTracker.KEY_ATTRIBUTE_AdvEnvXMLObjContextAttributeListener, "initial");
        processEvents(key);
        WCEventTracker.clearEvents(key);
        // Update the context attribute
        key = WCEventTracker.KEY_LISTENER_REP_AdvEnvXMLObjContextAttributeListener;
        WCEventTracker.clearEvents(key);
        getServletContext().setAttribute(WCEventTracker.KEY_ATTRIBUTE_AdvEnvXMLObjContextAttributeListener, "replaced");
        processEvents(key);
        WCEventTracker.clearEvents(key);
        // Delete the context attribute
        key = WCEventTracker.KEY_LISTENER_DEL_AdvEnvXMLObjContextAttributeListener;
        WCEventTracker.clearEvents(key);
        getServletContext().removeAttribute(WCEventTracker.KEY_ATTRIBUTE_AdvEnvXMLObjContextAttributeListener);
        processEvents(key);
        WCEventTracker.clearEvents(key);
    }

    /**
     * This test case specifically tests XML injection of env-entries
     * with an XML description into a ServletRequestListener.
     *
     * This test gets the results that the RequestListener put into WCEventTracker
     * and reports them back to the test case. It is expected that the
     * env-entries were properly injected and should therefore get passing
     * results.
     */
    @Test
    public void testEnvXMLObjRequestListener() {
        svLogger.info("Testing Transaction Request Listener Injection...");
        String key = WCEventTracker.KEY_LISTENER_INIT_AdvEnvXMLObjServletRequestListener;
        processEvents(key);
        WCEventTracker.clearEvents(key);
    }

    /**
     * This test case specifically tests XML injection of env-entries
     * with an XML description into a ServletRequestAttributeListener.
     *
     * This test gets the results that the ServletRequestAttributeListener put into
     * WCEventTracker and reports them back to the test case. To do this,
     * the test must first set, replace, and remove an attribute to generate the
     * proper events on the listener. It is expected that the env-entries were
     * properly injected and should therefore get passing results.
     */
    @Test
    public void testEnvXMLObjServletRequestAttributeListener(HttpServletRequest req, HttpServletResponse resp) {
        svLogger.info("Testing Transaction Request Attribute Listener Injection...");
        // Set a context attribute
        String key = WCEventTracker.KEY_LISTENER_ADD_AdvEnvXMLObjServletRequestAttributeListener;
        WCEventTracker.clearEvents(key);
        req.setAttribute(WCEventTracker.KEY_ATTRIBUTE_AdvEnvXMLObjServletRequestAttributeListener, "initial");
        processEvents(key);
        WCEventTracker.clearEvents(key);
        // Update the context attribute
        key = WCEventTracker.KEY_LISTENER_REP_AdvEnvXMLObjServletRequestAttributeListener;
        WCEventTracker.clearEvents(key);
        req.setAttribute(WCEventTracker.KEY_ATTRIBUTE_AdvEnvXMLObjServletRequestAttributeListener, "replaced");
        processEvents(key);
        WCEventTracker.clearEvents(key);
        // Delete the context attribute
        key = WCEventTracker.KEY_LISTENER_DEL_AdvEnvXMLObjServletRequestAttributeListener;
        WCEventTracker.clearEvents(key);
        req.removeAttribute(WCEventTracker.KEY_ATTRIBUTE_AdvEnvXMLObjServletRequestAttributeListener);
        processEvents(key);
        WCEventTracker.clearEvents(key);
    }

    /**
     * This test case specifically tests XML injection of env-entries
     * with an XML description into a Filter.
     *
     * This test gets the results that the Filter put into WCEventTracker and reports
     * them back to the test case. It is expected that the env-entries
     * were properly injected and should therefore get passing results.
     */
    @Test
    public void testEnvXMLObjServletFilter() {
        String key = WCEventTracker.KEY_FILTER_DOFILTER_AdvEnvXMLObjFilter;
        processEvents(key);
        WCEventTracker.clearEvents(key);
    }

    /*
     * This is a helper method to process the WCEventTracker results that are
     * retrieved by the methods above.
     */
    private void processEvents(String key) {
        Vector<String> events = WCEventTracker.getEvents(key);

        String event = "";
        if (events != null) {
            for (int i = 0; i < events.size(); i++) {
                event = events.get(i);
                svLogger.info("Result: " + key + " - " + event);
                String[] results = WCEventTracker.splitEvent(event);
                assertTrue(results[1], results[0].equals("PASS"));
            }
        } else {
            fail("No events for key \"" + key + "\"");
        }
    }
}