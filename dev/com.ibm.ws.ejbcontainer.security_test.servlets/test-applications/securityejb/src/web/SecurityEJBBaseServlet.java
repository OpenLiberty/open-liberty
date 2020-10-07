/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.ejbcontainer.security.test.SecurityEJBInterface;
import com.ibm.ws.ejbcontainer.security.test.SecurityEJBStatefulInterface;

/**
 * Base security EJB servlet which the other EJB test servlets extend.
 */

@SuppressWarnings("serial")
public abstract class SecurityEJBBaseServlet extends HttpServlet {

    /**
     * Invoke EJB method
     *
     * @param write
     * @param testMethod
     */

    protected interface Invoke {
        String go(SecurityEJBInterface securityEJBInterface);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if ("CUSTOM".equalsIgnoreCase(req.getMethod()))
            doCustom(req, res);
        else
            super.service(req, res);
    }

    void doCustom(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp);
    }

    /**
     * Common logic to handle any of the various requests this servlet supports.
     * The actual business logic can be customized by overriding performTask.
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();

        writer.println("\nServletName: " + servletName());

        StringBuffer sb = new StringBuffer();
        try {
            performTask(req, resp, sb);
        } catch (Throwable t) {
            t.printStackTrace(writer);
        }

        writer.write(sb.toString());
        writer.flush();
        writer.close();
    }

    abstract String servletName();

    /**
     * Default action for the servlet if not overridden.
     *
     * @param req
     * @param resp
     * @param writer
     * @throws ServletException
     * @throws IOException
     */
    protected void performTask(HttpServletRequest req, HttpServletResponse resp, StringBuffer sb) throws ServletException, IOException {

        //Get parameters from URL link
        String testMethod = req.getParameter("testMethod");
        String testInstance = req.getParameter("testInstance");

        if (testMethod == null) {
            writeLine(sb, "Usage: ?testInstance=<testInstance>&testMethod=<testMethod>");
        } else {
            invokeEJBMethod(sb, testInstance, testMethod);
        }
    }

    abstract void invokeEJBMethod(StringBuffer sb, String testInstance, String testMethod);

    /**
     * "Writes" the msg out to the client. This actually appends the msg
     * and a line delimiters to the running StringBuffer. This is necessary
     * because if too much data is written to the PrintWriter before the
     * logic is done, a flush() may get called and lock out changes to the
     * response.
     *
     * @param sb Running StringBuffer
     * @param msg Message to write
     */
    protected void writeLine(StringBuffer sb, String msg) {
        sb.append(msg + "\n");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp);
    }

// New interfaces for refactoring to handle stateful beans - below

    StatelessSecurityBeans stateless = new StatelessSecurityBeans();
    StatefulSecurityBeans stateful = new StatefulSecurityBeans();

    protected interface SecurityBeans {
        SecurityBean lookup(String beanname);
    }

    protected interface SecurityBean {
        SecurityEJBInterface get();

        void cleanup();
    }

    protected class StatelessSecurityBeans implements SecurityBeans {
        Map<String, SecurityEJBInterface> buildMap = new HashMap<String, SecurityEJBInterface>();

        void populate(Map<String, SecurityEJBInterface> beans) {
            buildMap.clear();
            buildMap.putAll(beans);
        }

        @Override
        public SecurityBean lookup(String beanname) {
            SecurityEJBInterface b = buildMap.get(beanname);
            if (b == null)
                return null;

            return new StatelessSecurityBean(b);
        }
    }

    protected class StatefulSecurityBeans implements SecurityBeans {
        Map<String, String> buildMap = new HashMap<String, String>();

        void populate(Map<String, String> beans) {
            buildMap.clear();
            buildMap.putAll(beans);
        }

        @Override
        public SecurityBean lookup(String beanname) {
            String ctxt = buildMap.get(beanname);
            if (ctxt == null)
                return null;

            SecurityEJBStatefulInterface b = null;
            try {
                b = (SecurityEJBStatefulInterface) new InitialContext().lookup(ctxt);
            } catch (NamingException e) {
                e.printStackTrace();
            }
            if (b == null)
                return null;

            return new StatefulSecurityBean(b);
        }
    }

    protected class StatelessSecurityBean implements SecurityBean {
        SecurityEJBInterface myStatelessBean;

        StatelessSecurityBean(SecurityEJBInterface statelessBean) {
            myStatelessBean = statelessBean;
        }

        @Override
        public void cleanup() {
            // nothing to do for Stateless bean cleanup
        }

        @Override
        public SecurityEJBInterface get() {

            return myStatelessBean;
        }

    }

    protected class StatefulSecurityBean implements SecurityBean {
        SecurityEJBStatefulInterface myStatefulBean;

        StatefulSecurityBean(SecurityEJBStatefulInterface statefulBean) {
            myStatefulBean = statefulBean;
        }

        @Override
        public void cleanup() {
            myStatefulBean.remove();
        }

        @Override
        public SecurityEJBStatefulInterface get() {

            return myStatefulBean;
        }

    }

    abstract Map<String, SecurityEJBInterface> statelessBeans();

    abstract Map<String, String> statefulBeans();

}
