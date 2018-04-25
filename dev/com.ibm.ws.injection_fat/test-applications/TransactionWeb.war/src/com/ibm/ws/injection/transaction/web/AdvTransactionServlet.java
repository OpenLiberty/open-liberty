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

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.Vector;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/AdvTransactionServlet")
public class AdvTransactionServlet extends FATServlet {
    private static final String CLASS_NAME = AdvTransactionServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final long serialVersionUID = 1L;

    /**
     * This test case specifically tests class-level @Resource declaration @Resource
     * and XML injection of resource-env-ref UserTran and TranSynchRegistry into
     * an HttpSessionListener.
     */
    @Test
    public void testTransactionHttpSessionListener(HttpServletRequest req, HttpServletResponse resp) {
        svLogger.info("Testing Transaction Http Session Listener Injection...");
        String key = WCEventTracker.KEY_LISTENER_CREATED_AdvTransactionHttpSessionListener;
        req.getSession();
        processEvents(key);
        WCEventTracker.clearEvents(key);
    }

    /**
     * This test case specifically tests class-level @Resource declaration @Resource
     * and XML injection of resource-env-ref UserTran and TranSynchRegistry into
     * an HttpSessionAttributeListener.
     */
    @Test
    public void testTransactionHttpSessionAttributeListener(HttpServletRequest req, HttpServletResponse resp) {
        svLogger.info("Testing Transaction Context Attribute Listener Injection...");
        // Set a context attribute
        String key = WCEventTracker.KEY_LISTENER_ADD_AdvTransactionHttpSessionAttributeListener;
        WCEventTracker.clearEvents(key);
        req.getSession().setAttribute(WCEventTracker.KEY_ATTRIBUTE_AdvTransactionHttpSessionAttributeListener, "initial");
        processEvents(key);
        WCEventTracker.clearEvents(key);
        // Update the context attribute
        key = WCEventTracker.KEY_LISTENER_REP_AdvTransactionHttpSessionAttributeListener;
        WCEventTracker.clearEvents(key);
        req.getSession().setAttribute(WCEventTracker.KEY_ATTRIBUTE_AdvTransactionHttpSessionAttributeListener, "replaced");
        processEvents(key);
        WCEventTracker.clearEvents(key);
        // Delete the context attribute
        key = WCEventTracker.KEY_LISTENER_DEL_AdvTransactionHttpSessionAttributeListener;
        WCEventTracker.clearEvents(key);
        req.getSession().removeAttribute(WCEventTracker.KEY_ATTRIBUTE_AdvTransactionHttpSessionAttributeListener);
        processEvents(key);
        WCEventTracker.clearEvents(key);
    }

    /**
     * This test case specifically tests class-level @Resource declaration @Resource
     * and XML injection of resource-env-ref UserTran and TranSynchRegistry into
     * a ServletContextListener.
     */
    @Test
    public void testTransactionServletContextListener() {
        svLogger.info("Testing Transaction Servlet Context Listener Injection...");
        String key = WCEventTracker.KEY_LISTENER_INIT_AdvTransactionServletContextListener;
        processEvents(key);
    }

    /**
     * This test case specifically tests class-level @Resource declaration @Resource
     * and XML injection of resource-env-ref UserTran and TranSynchRegistry into
     * a ServletContextAttributeListener.
     */
    @Test
    public void testTransactionServletContextAttributeListener() {
        svLogger.info("Testing Transaction Context Attribute Listener Injection...");
        // Set a context attribute
        String key = WCEventTracker.KEY_LISTENER_ADD_AdvTransactionContextAttributeListener;
        WCEventTracker.clearEvents(key);
        getServletContext().setAttribute(WCEventTracker.KEY_ATTRIBUTE_AdvTransactionContextAttributeListener, "initial");
        processEvents(key);
        WCEventTracker.clearEvents(key);
        // Update the context attribute
        key = WCEventTracker.KEY_LISTENER_REP_AdvTransactionContextAttributeListener;
        WCEventTracker.clearEvents(key);
        getServletContext().setAttribute(WCEventTracker.KEY_ATTRIBUTE_AdvTransactionContextAttributeListener, "replaced");
        processEvents(key);
        WCEventTracker.clearEvents(key);
        // Delete the context attribute
        key = WCEventTracker.KEY_LISTENER_DEL_AdvTransactionContextAttributeListener;
        WCEventTracker.clearEvents(key);
        getServletContext().removeAttribute(WCEventTracker.KEY_ATTRIBUTE_AdvTransactionContextAttributeListener);
        processEvents(key);
        WCEventTracker.clearEvents(key);
    }

    /**
     * This test case specifically tests class-level @Resource declaration @Resource
     * and XML injection of resource-env-ref UserTran and TranSynchRegistry into
     * a ServletRequestListener.
     */
    @Test
    public void testTransactionRequestListener() {
        svLogger.info("Testing Transaction Request Listener Injection...");
        String key = WCEventTracker.KEY_LISTENER_INIT_AdvTransactionServletRequestListener;
        processEvents(key);
        WCEventTracker.clearEvents(key);
    }

    /**
     * This test case specifically tests class-level @Resource declaration @Resource
     * and XML injection of resource-env-ref UserTran and TranSynchRegistry into
     * a ServletRequestAttributeListener.
     */
    @Test
    public void testTransactionServletRequestAttributeListener(HttpServletRequest req, HttpServletResponse resp) {
        svLogger.info("Testing Transaction Request Attribute Listener Injection...");
        // Set a context attribute
        String key = WCEventTracker.KEY_LISTENER_ADD_AdvTransactionServletRequestAttributeListener;
        WCEventTracker.clearEvents(key);
        req.setAttribute(WCEventTracker.KEY_ATTRIBUTE_AdvTransactionServletRequestAttributeListener, "initial");
        processEvents(key);
        WCEventTracker.clearEvents(key);
        // Update the context attribute
        key = WCEventTracker.KEY_LISTENER_REP_AdvTransactionServletRequestAttributeListener;
        WCEventTracker.clearEvents(key);
        req.setAttribute(WCEventTracker.KEY_ATTRIBUTE_AdvTransactionServletRequestAttributeListener, "replaced");
        processEvents(key);
        WCEventTracker.clearEvents(key);
        // Delete the context attribute
        key = WCEventTracker.KEY_LISTENER_DEL_AdvTransactionServletRequestAttributeListener;
        WCEventTracker.clearEvents(key);
        req.removeAttribute(WCEventTracker.KEY_ATTRIBUTE_AdvTransactionServletRequestAttributeListener);
        processEvents(key);
        WCEventTracker.clearEvents(key);
    }

    /**
     * This test case specifically tests class-level @Resource declaration and @Resource
     * and XML injection of resource-env-ref UserTran and TranSynchRegistry into
     * a Filter.
     */
    @Test
    public void testTransactionServletFilter() {
        String key = WCEventTracker.KEY_FILTER_DOFILTER_AdvTransactionFilter;
        processEvents(key);
        WCEventTracker.clearEvents(key);
    }

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