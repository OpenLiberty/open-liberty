/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
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
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import web.mdb.DynaCfgMessageDrivenBean;

@WebServlet("/*")
public class DynamicConfigTestServlet extends HttpServlet {
    private static final long serialVersionUID = 7709282314904580334L;

    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    public static final String SUCCESS_MESSAGE = "SUCCESS";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("testMethod");
        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");
        System.out.println("-----> " + test + " starting");
        try {
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request, response);
            System.out.println("<----- " + test + " successful");
            out.println(test + " " + SUCCESS_MESSAGE);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            System.out.println("<----- " + test + " failed:");
            x.printStackTrace(System.out);
            out.println("<pre>ERROR in " + test + ":");
            x.printStackTrace(out);
            out.println("</pre>");
        }
    }

    /**
     * Verify that a message driven bean is only invoked for a value of 0
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testActivationSpec_MessageOn_0(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Note: we abuse the setLoginTimeout interface to trigger sending a message to the MDB
        CommonDataSource ds = (CommonDataSource) new InitialContext().lookup("eis/cf");
        DynaCfgMessageDrivenBean.messages.clear();
        ds.setLoginTimeout(-1);
        ds.setLoginTimeout(0);
        ds.setLoginTimeout(1);

        if (DynaCfgMessageDrivenBean.messages.isEmpty())
            throw new Exception("Message not sent to MDB");

        if (DynaCfgMessageDrivenBean.messages.size() > 1)
            throw new Exception("Too many messages sent to MDB: " + DynaCfgMessageDrivenBean.messages);

        String message = DynaCfgMessageDrivenBean.messages.poll();
        if (!message.endsWith(" 0"))
            throw new Exception("Unexpected message sent to MDB: " + message);
    }

    /**
     * Verify that a message driven bean is only invoked for a values within 0 to 100
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testActivationSpec_MessageOn_0_100(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Note: we abuse the setLoginTimeout interface to trigger sending a message to the MDB
        CommonDataSource ds = (CommonDataSource) new InitialContext().lookup("eis/cf");
        DynaCfgMessageDrivenBean.messages.clear();
        ds.setLoginTimeout(-1);
        ds.setLoginTimeout(0);

        if (DynaCfgMessageDrivenBean.messages.isEmpty())
            throw new Exception("Message not sent to MDB");

        if (DynaCfgMessageDrivenBean.messages.size() > 1)
            throw new Exception("Too many messages sent to MDB: " + DynaCfgMessageDrivenBean.messages);

        String message = DynaCfgMessageDrivenBean.messages.poll();
        if (!message.endsWith(" 0"))
            throw new Exception("Unexpected message sent to MDB: " + message);

        ds.setLoginTimeout(100);
        ds.setLoginTimeout(101);

        if (DynaCfgMessageDrivenBean.messages.isEmpty())
            throw new Exception("Second message not sent to MDB");

        if (DynaCfgMessageDrivenBean.messages.size() > 1)
            throw new Exception("For values 100 and 101, too many messages sent to MDB: " + DynaCfgMessageDrivenBean.messages);

        message = DynaCfgMessageDrivenBean.messages.poll();
        if (!message.endsWith(" 100"))
            throw new Exception("Unexpected second message sent to MDB: " + message);
    }

    /**
     * Verify that a message driven bean is only invoked for a values within 5 to 50
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testActivationSpec_MessageOn_5_50(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Note: we abuse the setLoginTimeout interface to trigger sending a message to the MDB
        CommonDataSource ds = (CommonDataSource) new InitialContext().lookup("eis/cf");
        DynaCfgMessageDrivenBean.messages.clear();
        ds.setLoginTimeout(4);
        ds.setLoginTimeout(5);

        if (DynaCfgMessageDrivenBean.messages.isEmpty())
            throw new Exception("Message not sent to MDB");

        if (DynaCfgMessageDrivenBean.messages.size() > 1)
            throw new Exception("Too many messages sent to MDB: " + DynaCfgMessageDrivenBean.messages);

        String message = DynaCfgMessageDrivenBean.messages.poll();
        if (!message.endsWith(" 5"))
            throw new Exception("Unexpected message sent to MDB: " + message);

        ds.setLoginTimeout(50);
        ds.setLoginTimeout(51);

        if (DynaCfgMessageDrivenBean.messages.isEmpty())
            throw new Exception("Second message not sent to MDB");

        if (DynaCfgMessageDrivenBean.messages.size() > 1)
            throw new Exception("For values 50 and 51, too many messages sent to MDB: " + DynaCfgMessageDrivenBean.messages);

        message = DynaCfgMessageDrivenBean.messages.poll();
        if (!message.endsWith(" 50"))
            throw new Exception("Unexpected second message sent to MDB: " + message);
    }

    /**
     * Verify that a message driven bean is not invoked
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testActivationSpec_NoMessages(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Note: we abuse the setLoginTimeout interface to trigger sending a message to the MDB
        CommonDataSource ds = (CommonDataSource) new InitialContext().lookup("eis/cf");
        DynaCfgMessageDrivenBean.messages.clear();
        ds.setLoginTimeout(-1000);
        ds.setLoginTimeout(0);
        ds.setLoginTimeout(10);
        ds.setLoginTimeout(1000);
        if (!DynaCfgMessageDrivenBean.messages.isEmpty())
            throw new Exception("Unepxected messages sent to MDB: " + DynaCfgMessageDrivenBean.messages);
    }

    /**
     * Verify that an admin object of type java.util.Date is set to December 1, 2013.
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testAdminObject_Date_2013_Dec_1(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Calendar cal = new GregorianCalendar();
        cal.setTime((Date) new InitialContext().lookup("eis/myAdminObject"));
        int year = cal.get(Calendar.YEAR);
        if (year != 2013)
            throw new Exception("Unexpected year: " + year + " for date " + cal);
        int month = cal.get(Calendar.MONTH);
        if (month != Calendar.DECEMBER)
            throw new Exception("Unexpected month: " + month + " for date " + cal);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        if (dayOfMonth != 1)
            throw new Exception("Unexpected day of month: " + dayOfMonth + " for date " + cal);
    }

    /**
     * Verify that an admin object of type java.util.Date is set to January 1, 2013.
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testAdminObject_Date_2013_Jan_1(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Calendar cal = new GregorianCalendar();
        cal.setTime((Date) new InitialContext().lookup("eis/myAdminObject"));
        int year = cal.get(Calendar.YEAR);
        if (year != 2013)
            throw new Exception("Unexpected year: " + year + " for date " + cal);
        int month = cal.get(Calendar.MONTH);
        if (month != Calendar.JANUARY)
            throw new Exception("Unexpected month: " + month + " for date " + cal);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        if (dayOfMonth != 1)
            throw new Exception("Unexpected day of month: " + dayOfMonth + " for date " + cal);
    }

    /**
     * Verify that an admin object is available as type java.util.List implemented by java.util.LinkedList.
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testAdminObject_List(HttpServletRequest request, HttpServletResponse response) throws Exception {
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) new InitialContext().lookup("eis/myAdminObject");
        list.add("testAdminObject_List");
        ((LinkedList<?>) list).removeFirst();
    }

    /**
     * Verify that an admin object is available as type java.util.List implemented by java.util.LinkedList.
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testAdminObject_List2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) new InitialContext().lookup("eis/list2");
        list.add("testAdminObject_List");
        ((LinkedList<?>) list).removeFirst();
    }

    /**
     * Verify that an admin object is not available.
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testAdminObject_NoList(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) new InitialContext().lookup("eis/myAdminObject");
            throw new Exception();
        } catch (NameNotFoundException e) {
        }
    }

    /**
     * Verify that connection factory "eis/cf" has loginTimeout=100
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testCommonDataSource_LoginTimeout_100(HttpServletRequest request, HttpServletResponse response) throws Exception {
        CommonDataSource ds = (CommonDataSource) new InitialContext().lookup("eis/cf");
        int loginTimeout = ds.getLoginTimeout();
        if (loginTimeout != 100)
            throw new Exception("Unexpected loginTimeout: " + loginTimeout);
    }

    /**
     * Verify that connection factory "eis/cf" has loginTimeout=60
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testCommonDataSource_LoginTimeout_60(HttpServletRequest request, HttpServletResponse response) throws Exception {
        CommonDataSource ds = (CommonDataSource) new InitialContext().lookup("eis/cf");
        int loginTimeout = ds.getLoginTimeout();
        if (loginTimeout != 60)
            throw new Exception("Unexpected loginTimeout: " + loginTimeout);
    }

    /**
     * Verify that connection factory "eis/cf" has loginTimeout=80
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testCommonDataSource_LoginTimeout_80(HttpServletRequest request, HttpServletResponse response) throws Exception {
        CommonDataSource ds = (CommonDataSource) new InitialContext().lookup("eis/cf");
        int loginTimeout = ds.getLoginTimeout();
        if (loginTimeout != 80)
            throw new Exception("Unexpected loginTimeout: " + loginTimeout);
    }

    /**
     * Verify that a connection factory is not available as "eis/cf"
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testCommonDataSource_None(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            Object cf = new InitialContext().lookup("eis/cf");
            throw new Exception("unexpected lookup result: " + cf);
        } catch (NameNotFoundException e) {
        }
    }

    public void testRARUsable(HttpServletRequest request, HttpServletResponse response) throws Exception {
        new InitialContext().lookup("eis/myAdminObject");
    }

    /**
     * Verify that connection factory "eis/cf" does not have a default container auth alias
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testDataSource_ContainerAuthData_None(HttpServletRequest request, HttpServletResponse response) throws Exception {
        DataSource ds = (DataSource) new InitialContext().lookup("java:comp/env/eis/cfref");
        Connection con = ds.getConnection();
        try {
            String user = con.getMetaData().getUserName();
            if (user != null)
                throw new Exception("Unexpected user name: " + user);
        } finally {
            con.close();
        }
    }

    /**
     * Verify that the default container auth alias for connection factory "eis/cf" is user1
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testDataSource_ContainerAuthData_User1(HttpServletRequest request, HttpServletResponse response) throws Exception {
        DataSource ds = (DataSource) new InitialContext().lookup("java:comp/env/eis/cfref");
        Connection con = ds.getConnection();
        try {
            String user = con.getMetaData().getUserName();
            if (!"user1".equals(user))
                throw new Exception("Unexpected or missing user name: " + user);
        } finally {
            con.close();
        }
    }

    /**
     * Verify that the default container auth alias for connection factory "eis/cf" is user2
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testDataSource_ContainerAuthData_User2(HttpServletRequest request, HttpServletResponse response) throws Exception {
        DataSource ds = (DataSource) new InitialContext().lookup("java:comp/env/eis/cfref");
        Connection con = ds.getConnection();
        try {
            String user = con.getMetaData().getUserName();
            if (!"user2".equals(user))
                throw new Exception("Unexpected or missing user name: " + user);
        } finally {
            con.close();
        }
    }
}