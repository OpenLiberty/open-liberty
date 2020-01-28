/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
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
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Resource;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.resource.cci.ConnectionFactory;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

@WebServlet(urlPatterns = "/JCAEnterpriseAppTestServlet")
public class JCAEnterpriseAppTestServlet extends HttpServlet {
    private static final long serialVersionUID = 2803499654909072856L;

    @Resource(name = "eis/cf1")
    ConnectionFactory cf1;

    @Resource(name = "eis/ds1")
    DataSource ds1;

    @Resource(name = "java:app/env/eis/ds1refWithLoginModule", lookup = "eis/ds1")
    DataSource ds1refWithLoginModule;

    @Resource(name = "eis/map1")
    Map<String, String> map1;

    @Resource(name = "eis/map2")
    Map<String, Integer> map2;

    @Resource(name = "eis/queue1")
    Queue<String> queue1;

    private static String MBEAN_TYPE = "com.ibm.ws.jca.cm.mbean.ConnectionManagerMBean";

    /**
     * Sanity check test to make sure the servlet is available
     */
    public void checkSetupTest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.getWriter().println("Check setup test is working.");
        System.out.println("Check setup test is working");
    }

    /**
     * Test that an injected admin object is usable
     */
    public void testAdminObjectInjected(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            map1.put("testCase", "testAdminObjectInjected");
            Entry<String, String> entry = map1.entrySet().iterator().next();

            if (!"testCase".equals(entry.getKey()))
                throw new Exception("Unexpected key: " + entry.getKey());

            if (!"testAdminObjectInjected".equals(entry.getValue()))
                throw new Exception("Unexpected value: " + entry.getValue());
        } finally {
            map1.clear();
        }
    }

    /**
     * Test that an admin object found via jndi lookup is usable
     */
    public void testAdminObjectLookup(HttpServletRequest request, HttpServletResponse response) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) (new InitialContext().lookup("eis/map1"));
        try {
            map.put("testCase", "testAdminObjectLookup");
            Entry<String, String> entry = map.entrySet().iterator().next();

            if (!"testCase".equals(entry.getKey()))
                throw new Exception("Unexpected key: " + entry.getKey());

            if (!"testAdminObjectLookup".equals(entry.getValue()))
                throw new Exception("Unexpected value: " + entry.getValue());
        } finally {
            map.clear();
        }
    }

    /**
     * testConnectionFactoryUsesLoginModule - Verify that a connection factory that is obtained from the loginModRA resource adapter has the
     * user name that is assigned by its login module.
     */
    public void testConnectionFactoryUsesLoginModule(HttpServletRequest request, HttpServletResponse response) throws Exception {
        javax.resource.cci.Connection con = cf1.getConnection();
        try {
            String userPwd = con.getMetaData().getUserName();
            if (!"lmuser/lmpwd".equals(userPwd))
                throw new Exception("Unexpected user/password " + userPwd + " found on connection. Was the login module used?");
        } finally {
            con.close();
        }
    }

    /**
     * Check that an injected datasource is usable. Verify settings/configuration (such as username).
     */
    public void testDataSourceInjected(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Connection conn = ds1.getConnection();
        try {
            String expectedUser = "DS1USER";
            String user = conn.getMetaData().getUserName();
            if (!expectedUser.equals(user)) {
                throw new Exception("Expected user to be " + expectedUser + " but instead was " + user);
            }

            Statement stmt = conn.createStatement();
            try {
                ResultSet result = conn.createStatement().executeQuery("values sign(-3.14159)");

                if (!result.next())
                    throw new Exception("Missing result of query");

                int value = result.getInt(1);
                if (value != -1)
                    throw new Exception("Unexpected value: " + value);
            } finally {
                stmt.close();
            }
        } finally {
            if (conn != null)
                conn.close();
        }
    }

    /**
     * Check that a datasource found by jndi lookup is usable. Verify settings/configuration (such as username).
     */
    public void testDataSourceLookup(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Connection conn = ((DataSource) (new InitialContext().lookup("eis/ds1"))).getConnection("lookupUser", "pass");
        try {
            String expectedUser = "lookupUser";
            String user = conn.getMetaData().getUserName();
            if (!expectedUser.equals(user)) {
                throw new Exception("Expected user to be " + expectedUser + " but instead was " + user);
            }

            Statement stmt = conn.createStatement();
            try {
                ResultSet result = conn.createStatement().executeQuery("values sign(-3.14159)");

                if (!result.next())
                    throw new Exception("Missing result of query");

                int value = result.getInt(1);
                if (value != -1)
                    throw new Exception("Unexpected value: " + value);
            } finally {
                stmt.close();
            }
        } finally {
            if (conn != null)
                conn.close();
        }
    }

    /**
     * Verify that the user computed by the login module is used by the JCA data source when the resource reference indicates to use the login module.
     */
    public void testDataSourceUsingLoginModule(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Connection conn = ds1refWithLoginModule.getConnection();
        try {
            String user = conn.getMetaData().getUserName();
            if (!"LOGIN1USER".equals(user)) {
                throw new Exception("Unexpected user name " + user + ". Was the login module used?");
            }
        } finally {
            conn.close();
        }
    }

    /**
     * An MBean should be created for ds1 at servlet startup time.
     * Verify that we can find this MBean and there is exactly one of them.
     */
    public void testSimpleMBeanCreation(HttpServletRequest request, HttpServletResponse response) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + MBEAN_TYPE + ",jndiName=eis/ds1,*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        for (ObjectInstance bean : s) {
            System.out.println("**  Found " + bean.getObjectName().toString());
            String poolContents = (String) mbs.invoke(bean.getObjectName(), "showPoolContents", null, null);
            System.out.println(poolContents);
        }
        if (s.size() != 1) {
            throw new Exception("Expected to find exactly 1 MBean!");
        }
    }

    /**
     * Use a Map<String,Integer> to store username to userid mappings. Userid's are assigned in the order that
     * visitors visit. For the 3rd visitor, print a message indicating they win a prize.
     */
    public void testPrizeWinner(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        String username = request.getParameter("username");

        // If a user is returning, print out the username and id
        if (map2.containsKey(username)) {
            int userid = map2.get(username);
            out.println("Welcome back " + username + " (userid=" + userid + ")");
        }
        // If a user is visiting for the first time, add assign them a unique id and print name/id
        else {
            int userid = map2.size() + 1;
            map2.put(username, userid);
            out.println("Welcome for the first time " + username + " (userid=" + map2.get(username) + ")");
        }

        if (map2.get(username) == 3) {
            out.println("Contratulations! You are the 3rd visitor, you win a PRIZE!");
            if (!username.equals("user3"))
                throw new Exception("Expected 3rd visitor to be 'user3' but instead was: " + username);
        }
    }

    /**
     * Simulates a checkout line with customers.
     *
     * @param customer: the name of the customer for the operation
     * @param function: ADD, REMOVE, or CONTAINS which correspond to the Map methods.
     */
    public void testCheckoutLine(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        String customer = request.getParameter("customer");
        String function = request.getParameter("function");

        if (function.equals("ADD")) {
            queue1.add(customer);
            out.println("Customer " + customer + " has entered the checkout line.");
        } else if (function.equals("REMOVE"))
            out.println("Customer " + queue1.remove() + " has finished checking out.");
        else if (function.equals("CONTAINS")) {
            if (queue1.contains(customer))
                out.println("Customer " + customer + " is in line.");
            else
                out.println("Customer " + customer + " is NOT in line.");
        }

        out.println("The size of the checkout line is size=" + queue1.size());
    }

    /**
     * Message written to servlet to indicate that is has been successfully
     * invoked.
     */
    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    /**
     * Invokes test name found in "test" parameter passed to servlet.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println(" ---> JCAEnterpriseAppTest is starting " + test + "<br>");
        System.out.println(" ---> JCAEnterpriseAppTest is starting test: " + test);

        try {
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request, response);
            out.println(" <--- " + test + " " + SUCCESS_MESSAGE);
            System.out.println(" <--- " + test + " " + SUCCESS_MESSAGE);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            out.println("<pre>ERROR in " + test + ":");
            x.printStackTrace(out);
            out.println("</pre>");
            x.printStackTrace();
        } finally {
            out.flush();
            out.close();
        }
    }
}
