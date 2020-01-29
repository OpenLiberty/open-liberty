/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.cdi.simple.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.cdi.simple.CDISimpleTestLogic;
import com.ibm.ws.jpa.fvt.cdi.simple.CDITestComponent;
import com.ibm.ws.jpa.fvt.cdi.simple.model.LoggingService;
import com.ibm.ws.jpa.fvt.cdi.simple.model.Widget;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@WebServlet(urlPatterns = { "/JPACDISimpleServlet" })
public class JPACDISimpleServlet extends JPATestServlet implements CDITestComponent {
    private static final long serialVersionUID = -3888645553607380940L;
    private static final String CLASS_NAME = JPACDISimpleServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final Context ctx;

    @Inject
    // used for checking callbacks to entity listener
    private LoggingService logger;

    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "TestCDI")
    private EntityManager cmtsEM;

    // Application Managed JTA
    @PersistenceUnit(unitName = "TestCDI")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "TestCDI_RL")
    private EntityManagerFactory amrlEmf;

    public JPACDISimpleServlet() {
        super();

        Context c = null;
        try {
            c = new InitialContext();
        } catch (NamingException e) {
            e.printStackTrace();
        }
        ctx = c;
    }

    @Override
    public void init() throws ServletException {
        checkForTransformer();
    }

    @Override
    public List<String> getEntityListenerMessages() {
        return logger.getAndClearMessages();
    }

//    @Override
    public void doGetOrig(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doPostOrig(req, resp);
    }

//    @Override
    public void doPostOrig(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter pw = resp.getWriter();
        String op = req.getParameter("testMethod");
        int rc = 500;
        UserTransaction tran = null;
        try {

            tran = (UserTransaction) ctx.lookup("java:comp/UserTransaction");
            if ("insert".equals(op)) {
                rc = insert(req, pw, tran);
            } else if ("update".equals(op)) {
                rc = update(req, pw, tran);
            } else if ("delete".equals(op)) {
                rc = delete(req, pw, tran);
            } else if ("find".equals(op)) {
                rc = find(req, pw, tran);
            } else if ("clearAll".equals(op)) {
                rc = clearAll(req, pw, tran);
            } else {
                // unknown operation - error out
                svLogger.logp(Level.SEVERE, CLASS_NAME, "doPost", "Unknown operation: " + op);
                pw.println("FAIL: Unknown operation: " + op);
                rc = 500;
            }
        } catch (Throwable t) {
            svLogger.logp(Level.SEVERE, CLASS_NAME, "doPost", "Unexpected exception: ", t);
            pw.println("FAIL: Unexpected exception: " + t);
            rc = 500;
        }
        resp.setStatus(rc);
    }

    private static void printEntityListenerMessages(LoggingService logSvc, PrintWriter out) {
        for (String s : logSvc.getAndClearMessages()) {
            out.println(s);
        }
    }

    private int insert(HttpServletRequest req, PrintWriter out, UserTransaction tran) throws Exception {
        try {
            tran.begin();
            String name = req.getParameter("name");
            String description = req.getParameter("description");
            Widget w = new Widget();
            w.setName(name);
            w.setDescription(description);
            cmtsEM.persist(w);
            svLogger.logp(Level.INFO, CLASS_NAME, "insert", "persisted " + w);
            out.println("persisted " + w);
        } finally {
            tran.commit();
        }
        printEntityListenerMessages(logger, out);
        return 200;
    }

    private int update(HttpServletRequest req, PrintWriter out, UserTransaction tran) throws Exception {
        try {
            tran.begin();
            int id = Integer.parseInt(req.getParameter("id"));
            String name = req.getParameter("name");
            String description = req.getParameter("description");

            Widget w = cmtsEM.find(Widget.class, id);
            svLogger.logp(Level.INFO, CLASS_NAME, "update", "found " + w);
            w.setName(name);
            w.setDescription(description);
            cmtsEM.persist(w);
            svLogger.logp(Level.INFO, CLASS_NAME, "update", "updated to " + w);
            out.println("updated " + w);
        } finally {
            tran.commit();
        }
        printEntityListenerMessages(logger, out);
        return 200;
    }

    private int delete(HttpServletRequest req, PrintWriter out, UserTransaction tran) throws Exception {
        try {
            tran.begin();
            int id = Integer.parseInt(req.getParameter("id"));
            Widget w = cmtsEM.find(Widget.class, id);
            svLogger.logp(Level.INFO, CLASS_NAME, "delete", "found " + w);
            cmtsEM.remove(w);
            svLogger.logp(Level.INFO, CLASS_NAME, "delete", "deleted " + w);
            out.println("deleted " + w);
        } finally {
            tran.commit();
        }
        printEntityListenerMessages(logger, out);
        return 200;
    }

    private int find(HttpServletRequest req, PrintWriter out, UserTransaction tran) throws Exception {
        try {
            tran.begin();
            int id = Integer.parseInt(req.getParameter("id"));
            Widget w = cmtsEM.find(Widget.class, id);
            svLogger.logp(Level.INFO, CLASS_NAME, "find", "found " + w);
            out.println("found " + w);
        } finally {
            tran.commit();
        }
        printEntityListenerMessages(logger, out);
        return 200;
    }

    private int clearAll(HttpServletRequest req, PrintWriter out, UserTransaction tran) throws Exception {
        try {
            tran.begin();
            Query q = cmtsEM.createNativeQuery("delete from Widget");
            int rows = q.executeUpdate();
            svLogger.logp(Level.INFO, CLASS_NAME, "clearAll", "deleted " + rows + " rows");
        } finally {
            tran.commit();
        }
        return 200;
    }

    private void checkForTransformer() {
        Class<?>[] interfaces = Widget.class.getInterfaces();
        for (Class<?> i : interfaces) {
            if (i.getName().contains("eclipse")) {
                return;
            }
        }
        throw new RuntimeException("Entity class " + Widget.class.getName() + " should implement more than zero "
                                   + "EclipseLink interfaces. Most likely a transformer problem! Found : "
                                   + Arrays.toString(interfaces));
    }

    @PostConstruct
    private void initFAT() {
        testClassName = CDISimpleTestLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEM"));
    }

    @Test
    public void jpa_spec21_cdi_testInjectionOccursBeforePostConstructAndInsertionCallbacks_WEB_SL_AMJTA() throws Exception {
        final String testName = "jpa_spec21_cdi_testInjectionOccursBeforePostConstructAndInsertionCallbacks_WEB_SL_AMJTA";
        final String testMethod = "testInjectionOccursBeforePostConstructAndInsertionCallbacks";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec21_cdi_testInjectionOccursBeforePostConstructAndInsertionCallbacks_WEB_SL_AMRL() throws Exception {
        final String testName = "jpa_spec21_cdi_testInjectionOccursBeforePostConstructAndInsertionCallbacks_WEB_SL_AMRL";
        final String testMethod = "testInjectionOccursBeforePostConstructAndInsertionCallbacks";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec21_cdi_testInjectionOccursBeforePostConstructAndInsertionCallbacks_WEB_SL_CMTS() throws Exception {
        final String testName = "jpa_spec21_cdi_testInjectionOccursBeforePostConstructAndInsertionCallbacks_WEB_SL_CMTS";
        final String testMethod = "testInjectionOccursBeforePostConstructAndInsertionCallbacks";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
