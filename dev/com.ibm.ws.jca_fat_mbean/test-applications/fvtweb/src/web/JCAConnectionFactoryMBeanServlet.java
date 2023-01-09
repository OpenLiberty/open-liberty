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

public class JCAConnectionFactoryMBeanServlet extends HttpServlet {
    private static final long serialVersionUID = 7709282314904580345L;
    @SuppressWarnings("unused")
    private final String className = "JCAConnectionFactoryMBeanServlet";
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
     * Test JCA ConnectionFactory MBean registration.
     */
    public void testJCAConnectionFactoryMBeanRegistration(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String methodName = "testJCAConnectionFactoryMBeanRegistration";
        final String mBean_TYPE = "JCAConnectionFactoryMBean";
        final String mBean_TYPE_J2EE = "JCAConnectionFactory";
        final String cfName = "jmsConnectionFactory[cf1]";

        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con = cf.createConnection();
        System.out.println("Created a connenction: " + con.toString());

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + mBean_TYPE + ",j2eeType="
                                        + mBean_TYPE_J2EE + ",name=" + cfName + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        System.out.println(methodName + ": searching for: " + mBean_TYPE);
        for (ObjectInstance bean : s) {
            // testing JCAConnectionFactoryMBean test = (JCAConnectionFactoryMBean) bean;

            System.out.println("**  Found " + bean.getObjectName().toString());

            MBeanInfo mi = mbs.getMBeanInfo(bean.getObjectName());
            MBeanAttributeInfo[] malist = mi.getAttributes();
            int testedAttributes = 0;
            for (MBeanAttributeInfo ma : malist) {
                System.out.println("Attribute for mBean_TYPE " + mBean_TYPE + " " + ma);
                String attributeToTest = ma.getName();

                if (attributeToTest.equals("managedConnectionFactory")) {
                    String managedConnectionFactory = (String) mbs.getAttribute(bean.getObjectName(), "managedConnectionFactory");
                    if (managedConnectionFactory == null) {
                        // This should not happen for this test.
                        throw new Exception(methodName + " Do not have a managedConnectionFactory value, its null");
                    } else {
                        if (!managedConnectionFactory.equals("fat.jca.resourceadapter.FVTManagedConnectionFactory")) {
                            throw new Exception(methodName + " Expecting value fat.jca.resourceadapter.FVTManagedConnectionFactory but found value " + managedConnectionFactory);
                        }
                    }
                    ++testedAttributes;
                }

                if (attributeToTest.equals("objectName")) {
                    String objectName = (String) mbs.getAttribute(bean.getObjectName(), "objectName");
                    if (objectName == null) {
                        // This should not happen.
                        throw new Exception(methodName + " Do not have a objectName value " + objectName);
                    } else {
                        if (!objectName.equals("WebSphere:type=JCAConnectionFactoryMBean,j2eeType=JCAConnectionFactory,id=cf1,jndiName=jms/cf1,name=jmsConnectionFactory[cf1],J2EEServer="
                                               + serverName + ",JCAResource=FAT1")) {
                            throw new Exception(methodName
                                                + " Expecting value WebSphere:type=JCAConnectionFactoryMBean,j2eeType=JCAConnectionFactory,id=cf1,jndiName=jms/cf1,name=jmsConnectionFactory[cf1],J2EEServer="
                                                + serverName + ",JCAResource=FAT1 but found value "
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
            if (malist.length != 5) {
                throw new Exception(methodName + " Initial attributes tested were 5 "
                                    + "which is not equal to the actual attributes now "
                                    + malist.length + " this test will need to be updated to include or " +
                                    " remove attribute tests");
            }
        }
        System.out.println(methodName + ": s.size(): " + s.size());
        if (s.size() != 1) {
            System.out.println("No match found for the specified Connection Factory Mbean: " + mBean_TYPE);
            s = mbs.queryMBeans(null, null);
            System.out.println("Here is the full list of used MBeans");
            for (ObjectInstance bean : s) {
                System.out.println("**  Found " + bean.getObjectName().toString());
            }
            throw new Exception(methodName + ": Didn't find the specified Connection Factory Mbean: " + mBean_TYPE
                                + "\n Hint: Maybe the name of the Connection Factory in the server.xml of this bucket was changed?"
                                + "\n The test was looking for the connection factory name=" + cfName);
        }

    }

    /**
     * Test JCA ConnectionFactory MBean registration with Multiple Connection Factories defined on the server.xml.
     */
    public void testJCAConnectionFactoryMBeanRegistrationMultiple(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String methodName = "testJCAConnectionFactoryMBeanRegistrationMultiple";
        final String mBean_TYPE = "JCAConnectionFactoryMBean";
        final String mBean_TYPE_J2EE = "JCAConnectionFactory";
        final String cfName1 = "jmsConnectionFactory[cf1]";
        final String cfName2 = "jmsConnectionFactory[cf2]";
        final String expectedcfObjectName1 = "WebSphere:type=JCAConnectionFactoryMBean,j2eeType=JCAConnectionFactory,id=cf1,jndiName=jms/cf1,name=jmsConnectionFactory[cf1],J2EEServer="
                                             + serverNameMultiple + ",JCAResource=FAT1";
        final String expectedcfObjectName2 = "WebSphere:type=JCAConnectionFactoryMBean,j2eeType=JCAConnectionFactory,id=cf6,jndiName=jms/cf6,name=jmsConnectionFactory[cf6],J2EEServer="
                                             + serverNameMultiple + ",JCAResource=FAT2";

        ConnectionFactory cf = (ConnectionFactory) new InitialContext().lookup("java:comp/env/jms/cf1");
        Connection con = cf.createConnection();
        System.out.println("Created a connenction: " + con.toString());

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + mBean_TYPE + ",j2eeType="
                                        + mBean_TYPE_J2EE + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        System.out.println(methodName + ": searching for: " + mBean_TYPE);
        for (ObjectInstance bean : s) {
            // testing JCAConnectionFactoryMBean test = (JCAConnectionFactoryMBean) bean;

            System.out.println("**  Found " + bean.getObjectName().toString());
            String managedConnectionFactory = (String) mbs.getAttribute(bean.getObjectName(), "managedConnectionFactory");
            if (managedConnectionFactory == null) {
                // This should not happen for this test.
                throw new Exception(methodName + " Do not have a managedConnectionFactory value, its null");
            } else {
                if (!managedConnectionFactory.equals("fat.jca.resourceadapter.FVTManagedConnectionFactory")) {
                    throw new Exception(methodName + " Expecting value fat.jca.resourceadapter.FVTManagedConnectionFactory but found value " + managedConnectionFactory);
                }
            }

            String objectName = (String) mbs.getAttribute(bean.getObjectName(), "objectName");
            if (objectName == null) {
                // This should not happen.
                throw new Exception(methodName + " Do not have a objectName value " + objectName);
            } else {
                if (!(objectName.equals(expectedcfObjectName1) || objectName.equals(expectedcfObjectName2))) {
                    throw new Exception(methodName
                                        + " Expecting value " + expectedcfObjectName1 + " or " + expectedcfObjectName2 + " but found value " + objectName);
                }
            }
        }
        System.out.println(methodName + ": s.size(): " + s.size());
        if (s.size() != 2) {
            System.out.println("The incorrect number of JCA Connection Factory MBeans was found, expecting 2 but found: " + s.size());
            s = mbs.queryMBeans(null, null);
            System.out.println("Here is the full list of used MBeans");
            for (ObjectInstance bean : s)
                System.out.println("**  Found " + bean.getObjectName().toString());
            throw new Exception(methodName + ": Didn't find the specified JCA Connection Factory Mbean: " + mBean_TYPE
                                + "\n Hint: Maybe the name of the JCA Connection Factory in the server.xml of this bucket was changed?"
                                + "\n The test was looking for the JCA Connection Factory=" + cfName1 + " and " + cfName2);
        }

    }
}