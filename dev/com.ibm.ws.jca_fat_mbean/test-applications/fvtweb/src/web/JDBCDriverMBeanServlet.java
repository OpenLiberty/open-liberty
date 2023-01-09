/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JDBCDriverMBeanServlet extends HttpServlet {
    private static final long serialVersionUID = 7709282314904580349L;
    @SuppressWarnings("unused")
    private final String className = "JDBCDriverMBeanServlet";
    private final String serverName = "com.ibm.ws.jca.jdbc.mbean.fat.app.mBeanTest";
    private final String serverNameMultiple = "com.ibm.ws.jca.jdbc.mbean.fat.app.mBeanMultiple";

    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    public static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");
        System.out.println("-----> " + test + " starting");
        try {
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request, response);
            System.out.println("<----- " + test + " successful");
            out.println(test + " COMPLETED SUCCESSFULLY");
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
     * Test JDBC Driver MBean registration.
     */
    public void testJDBCDriverMBeanRegistration(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String methodName = "testJDBCDriverMBeanRegistration";
        final String mBean_TYPE = "JDBCDriverMBean";
        final String mBean_TYPE_J2EE = "JDBCDriver";
        final String drName = "jdbcDriver[FATJDBCDriver]";

        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con = cf.createConnection();
        System.out.println("Created a connection: " + con.toString());

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + mBean_TYPE + ",j2eeType="
                                        + mBean_TYPE_J2EE + ",name=" + drName + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        System.out.println(methodName + ": searching for: " + mBean_TYPE);
        for (ObjectInstance bean : s) {
            // testing JDBCDriverMBean test = (JDBCDriverMBean) bean;

            System.out.println("**  Found " + bean.getObjectName().toString());

            MBeanInfo mi = mbs.getMBeanInfo(bean.getObjectName());
            MBeanAttributeInfo[] malist = mi.getAttributes();
            for (MBeanAttributeInfo ma : malist) {
                System.out.println("Attributes for mBean_TYPE " + mBean_TYPE + " " + ma);
            }
            int testedAttributes = 0;
            for (MBeanAttributeInfo ma : malist) {
                System.out.println("Attribute for mBean_TYPE " + mBean_TYPE + " " + ma);
                String attributeToTest = ma.getName();

                if (attributeToTest.equals("objectName")) {
                    String objectName = (String) mbs.getAttribute(bean.getObjectName(), "objectName");
                    if (objectName == null) {
                        // This should not happen.
                        throw new Exception(methodName + " Do not have a objectName value " + objectName);
                    } else {
                        if (!objectName.equals("WebSphere:type=JDBCDriverMBean,j2eeType=JDBCDriver,id=FATJDBCDriver,name=jdbcDriver[FATJDBCDriver],J2EEServer=" + serverName
                                               + "")) {
                            throw new Exception(methodName
                                                + " Expecting value WebSphere:type=JDBCDriverMBean,j2eeType=JDBCDriver,id=FATJDBCDriver,name=jdbcDriver[FATJDBCDriver],J2EEServer="
                                                + serverName + " but found value "
                                                + objectName);
                        }
                    }
                    ++testedAttributes;
                }

                if (attributeToTest.equals("eventProvider")) {
                    Boolean eventProvider = (Boolean) mbs.getAttribute(bean.getObjectName(), "eventProvider");
                    if (eventProvider == null) {
                        throw new Exception(methodName + " Do not have a eventProvider value, its null");
                    }
                    if (eventProvider) {
                        // TODO - if true, we need to do more more testing.
                    } else {
                        // Nothing to do.
                    }
                    ++testedAttributes;
                }

                if (attributeToTest.equals("stateManageable")) {
                    Boolean stateManageable = (Boolean) mbs.getAttribute(bean.getObjectName(), "stateManageable");
                    if (stateManageable == null) {
                        throw new Exception(methodName + " Do not have a stateManageable value, its null");
                    }
                    if (stateManageable) {
                        // TODO - if true, we need to do more more testing.
                    } else {
                        // Nothing to do.
                    }
                    ++testedAttributes;
                }

                if (attributeToTest.equals("statisticsProvider")) {
                    Boolean statisticsProvider = (Boolean) mbs.getAttribute(bean.getObjectName(), "statisticsProvider");
                    if (statisticsProvider == null) {
                        throw new Exception(methodName + " Do not have a statisticsProvider value, its null");
                    }
                    if (statisticsProvider) {
                        // TODO - if true, we need to do more more testing.
                    } else {
                        // Nothing to do.
                    }
                    ++testedAttributes;
                }

            }
            /*
             * Check the number tested with the length and make sure expected tests are tested.
             */
            if (testedAttributes != malist.length) {
                throw new Exception(methodName + " Tested attributes " + testedAttributes
                                    + " is not equal to the actual attributes "
                                    + malist.length + " this test will need to be updated to include the missing attribute test");
            }
            /*
             * Check to see if the interface was changed to add or remove attributes
             */
            if (malist.length != 4) {
                throw new Exception(methodName + " Initial attributes tested were 5 "
                                    + "which is not equal to the actual attributes now "
                                    + malist.length + " this test will need to be updated to include or " +
                                    " remove attribute tests");
            }
        }
        System.out.println(methodName + ": s.size(): " + s.size());
        if (s.size() != 1) {
            System.out.println("No match found for the specified JDBCDriver Mbean: " + mBean_TYPE);
            s = mbs.queryMBeans(null, null);
            System.out.println("Here is the full list of used MBeans");
            for (ObjectInstance bean : s)
                System.out.println("**  Found " + bean.getObjectName().toString());
            throw new Exception(methodName + ": Didn't find the specified JDBCDriver Mbean: " + mBean_TYPE
                                + "\n Hint: Maybe the name of the JDBCDriver in the server.xml of this bucket was changed?"
                                + "\n The test was looking for the JDBCDriver name=" + drName);
        }
    }

    /**
     * Test JDBC Driver MBean registration with multiple JDBC drivers defined on the server.xml.
     */
    public void testJDBCDriverMBeanRegistrationMultiple(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String methodName = "testJDBCDriverMBeanRegistrationMultiple";
        final String mBean_TYPE = "JDBCDriverMBean";
        final String mBean_TYPE_J2EE = "JDBCDriver";
        final String drName1 = "jdbcDriver[FATJDBCDriver]";
        final String drName2 = "jdbcDriver[FATJDBCDriver1]";
        final String expecteddrObjectName1 = "WebSphere:type=JDBCDriverMBean,j2eeType=JDBCDriver,id=FATJDBCDriver,name=jdbcDriver[FATJDBCDriver],J2EEServer=" + serverNameMultiple
                                             + "";
        final String expecteddrObjectName2 = "WebSphere:type=JDBCDriverMBean,j2eeType=JDBCDriver,id=FATJDBCDriver1,name=jdbcDriver[FATJDBCDriver1],J2EEServer=" + serverNameMultiple
                                             + "";

        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con = cf.createConnection();
        System.out.println("Created a connection: " + con.toString());

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + mBean_TYPE + ",j2eeType="
                                        + mBean_TYPE_J2EE + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        System.out.println(methodName + ": searching for: " + mBean_TYPE);
        for (ObjectInstance bean : s) {
            // testing JDBCDriverMBean test = (JDBCDriverMBean) bean;

            System.out.println("**  Found " + bean.getObjectName().toString());

            MBeanInfo mi = mbs.getMBeanInfo(bean.getObjectName());

            String objectName = (String) mbs.getAttribute(bean.getObjectName(), "objectName");
            if (objectName == null) {
                // This should not happen.
                throw new Exception(methodName + " Do not have a objectName value " + objectName);
            } else {
                if (!(objectName.equals(expecteddrObjectName1) || objectName.equals(expecteddrObjectName2))) {
                    throw new Exception(methodName
                                        + "Expecting value " + expecteddrObjectName2 + " or " + expecteddrObjectName2 + " but found value "
                                        + objectName);
                }
            }
        }
        System.out.println(methodName + ": s.size(): " + s.size());
        if (s.size() != 2) {
            System.out.println("The incorrect number of JDBCDriver MBeans was found, expecting 2 but found: " + s.size());
            s = mbs.queryMBeans(null, null);
            System.out.println("Here is the full list of used MBeans");
            for (ObjectInstance bean : s)
                System.out.println("**  Found " + bean.getObjectName().toString());
            throw new Exception(methodName + ": Didn't find the specified JDBCDriver Mbean: " + mBean_TYPE
                                + "\n Hint: Maybe the name of the JDBC Driver in the server.xml of this bucket was changed?"
                                + "\n The test was looking for the JDBC Driver name=" + drName1 + "and" + drName2);
        }
    }
}
