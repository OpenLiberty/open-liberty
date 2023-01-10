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
import java.sql.Connection;
import java.util.Set;

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
import javax.sql.DataSource;

public class JDBCResourceMBeanServlet extends HttpServlet {
    private static final long serialVersionUID = 7709282314904580350L;
    @SuppressWarnings("unused")
    private final String className = "JDBCResourceMBeanServlet";
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
     * Test JDBC Resource MBean registration.
     */
    public void testJDBCResourceMBeanRegistration(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String methodName = "testJDBCResourceMBeanRegistration";
        final String mBean_TYPE = "JDBCResourceMBean";
        final String mBean_TYPE_J2EE = "JDBCResource";
        final String rsName = "dataSource[ds1]/JDBCResource";
        final String expectedObjectName = "WebSphere:type=JDBCDataSourceMBean,j2eeType=JDBCDataSource,id=ds1,jndiName=jdbc/ds1,"
                                          + "name=dataSource[ds1],J2EEServer=" + serverName + ",JDBCResource=dataSource[ds1]/JDBCResource";

        DataSource ds1 = (DataSource) new InitialContext().lookup("jdbc/ds1");
        Connection con = ds1.getConnection();
        System.out.println("Created a connection: " + con.toString());

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + mBean_TYPE + ",j2eeType="
                                        + mBean_TYPE_J2EE + "*,name=" + rsName + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        System.out.println(methodName + ": searching for: " + mBean_TYPE);
        for (ObjectInstance bean : s) {
            // testing JDBCResourceMBean test = (JDBCResourceMBean) bean;

            System.out.println("**  Found " + bean.getObjectName().toString());

            MBeanInfo mi = mbs.getMBeanInfo(bean.getObjectName());
            MBeanAttributeInfo[] malist = mi.getAttributes();

            int testedAttributes = 0;
            for (MBeanAttributeInfo ma : malist) {
                System.out.println("Attribute for mBean_TYPE " + mBean_TYPE + " " + ma);
                String attributeToTest = ma.getName();

                if (attributeToTest.equals("jdbcDataSources")) {
                    String[] jdbcDataSources = (String[]) mbs.getAttribute(bean.getObjectName(), "jdbcDataSources");
                    if (jdbcDataSources == null) {
                        // This should not happen for this test.
                        throw new Exception(methodName + " Do not have a jdbcDataSources value, its null");
                    } else {
                        if (jdbcDataSources.length != 1) {
                            throw new Exception(methodName + " Expecting to find one datasource but found" + jdbcDataSources.length);
                        } else {
                            if (!jdbcDataSources[0].equals(expectedObjectName)) {
                                throw new Exception(methodName + " Expecting value " + expectedObjectName + "\n but found value \n" + jdbcDataSources[0]);
                            }
                        }
                        ++testedAttributes;
                    }
                }

                if (attributeToTest.equals("objectName")) {
                    String objectName = (String) mbs.getAttribute(bean.getObjectName(), "objectName");
                    if (objectName == null) {
                        // This should not happen.
                        throw new Exception(methodName + " Do not have a objectName value " + objectName);
                    } else {
                        if (!objectName.equals("WebSphere:type=JDBCResourceMBean,j2eeType=JDBCResource,id=ds1,jndiName=jdbc/ds1,name=dataSource[ds1]/JDBCResource,J2EEServer="
                                               + serverName + "")) {
                            throw new Exception(methodName
                                                + " Expecting value WebSphere:type=JDBCResourceMBean,j2eeType=JDBCResource,id=ds1,jndiName=jdbc/ds1,name=dataSource[ds1]/JDBCResource,J2EEServer="
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
            if (malist.length != 5) {
                throw new Exception(methodName + " Initial attributes tested were 5 "
                                    + "which is not equal to the actual attributes now "
                                    + malist.length + " this test will need to be updated to include or " +
                                    " remove attribute tests");
            }
        }
        System.out.println(methodName + ": s.size(): " + s.size());
        if (s.size() != 1) {
            System.out.println("No match found for the specified JDBCResource Mbean: " + mBean_TYPE);
            s = mbs.queryMBeans(null, null);
            System.out.println("Here is the full list of used MBeans");
            for (ObjectInstance bean : s)
                System.out.println("**  Found " + bean.getObjectName().toString());
            throw new Exception(methodName + ": Didn't find the specified JDBCResource Mbean: " + mBean_TYPE
                                + "\n Hint: Maybe the name of the  JDBCResource in the server.xml of this bucket was changed?"
                                + "\n The test was looking for the JDBCResource name=" + rsName);
        }
        con.close();
    }

    /**
     * Test JDBC Resource MBean registration with multiple resources defined on the server.xml
     */
    public void testJDBCResourceMBeanRegistrationMultiple(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String methodName = "testJDBCResourceMBeanRegistrationMultiple";
        final String mBean_TYPE = "JDBCResourceMBean";
        final String mBean_TYPE_J2EE = "JDBCResource";
        final String rsName1 = "dataSource[ds1]/JDBCResource";
        final String rsName2 = "dataSource[ds2]/JDBCResource";
        final String expectedDsObjectName1 = "WebSphere:type=JDBCDataSourceMBean,j2eeType=JDBCDataSource,id=ds1,jndiName=jdbc/ds1,"
                                             + "name=dataSource[ds1],J2EEServer=" + serverNameMultiple + ",JDBCResource=dataSource[ds1]/JDBCResource";
        final String expectedDsObjectName2 = "WebSphere:type=JDBCDataSourceMBean,j2eeType=JDBCDataSource,id=ds2,jndiName=jdbc/ds2,"
                                             + "name=dataSource[ds2],J2EEServer=" + serverNameMultiple + ",JDBCResource=dataSource[ds2]/JDBCResource";
        final String expectedResourceObjectName1 = "WebSphere:type=JDBCResourceMBean,j2eeType=JDBCResource,id=ds1,jndiName=jdbc/ds1,"
                                                   + "name=dataSource[ds1]/JDBCResource,J2EEServer=" + serverNameMultiple + "";
        final String expectedResourceObjectName2 = "WebSphere:type=JDBCResourceMBean,j2eeType=JDBCResource,id=ds2,jndiName=jdbc/ds2,"
                                                   + "name=dataSource[ds2]/JDBCResource,J2EEServer=" + serverNameMultiple + "";

        DataSource ds1 = (DataSource) new InitialContext().lookup("jdbc/ds1");
        Connection con1 = ds1.getConnection();
        System.out.println("Created a connection: " + con1.toString());
        DataSource ds2 = (DataSource) new InitialContext().lookup("jdbc/ds2");
        Connection con2 = ds2.getConnection();
        System.out.println("Created a connection: " + con2.toString());

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + mBean_TYPE + ",j2eeType="
                                        + mBean_TYPE_J2EE + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        System.out.println(methodName + ": searching for: " + mBean_TYPE);
        for (ObjectInstance bean : s) {
            // testing JDBCResourceMBean test = (JDBCResourceMBean) bean;

            System.out.println("**  Found " + bean.getObjectName().toString());

            String objectName = (String) mbs.getAttribute(bean.getObjectName(), "objectName");
            if (objectName == null) {
                // This should not happen.
                throw new Exception(methodName + " Do not have a objectName value " + objectName);
            } else {
                if (!(objectName.equals(expectedResourceObjectName1) || objectName.equals(expectedResourceObjectName2))) {
                    throw new Exception(methodName
                                        + " Expecting value " + expectedResourceObjectName1 + " or " + expectedResourceObjectName2 + " but found value "
                                        + objectName);
                }
            }

            String[] jdbcDataSources = (String[]) mbs.getAttribute(bean.getObjectName(), "jdbcDataSources");
            objectName = (String) mbs.getAttribute(bean.getObjectName(), "objectName");
            if (jdbcDataSources == null) {
                // This should not happen for this test.
                throw new Exception(methodName + " Do not have a jdbcDataSources value, its null");
            } else {
                if (jdbcDataSources.length != 1) {
                    throw new Exception(methodName + " Expecting to find one datasource but found" + jdbcDataSources.length);
                } else {
                    if (!((jdbcDataSources[0].equals(expectedDsObjectName1) && objectName.equals(expectedResourceObjectName1))
                          || (jdbcDataSources[0].equals(expectedDsObjectName2) && objectName.equals(expectedResourceObjectName2)))) {
                        throw new Exception(methodName + " Expecting value " + expectedDsObjectName1 + " or " + expectedDsObjectName2 + "\n but found value \n"
                                            + jdbcDataSources[0]);
                    }
                }
            }

        }
        System.out.println(methodName + ": s.size(): " + s.size());
        if (s.size() != 2) {
            System.out.println("The incorrect number of JDBC Resource MBeans was found, expecting 2 but found: " + s.size());
            s = mbs.queryMBeans(null, null);
            System.out.println("Here is the full list of used MBeans");
            for (ObjectInstance bean : s)
                System.out.println("**  Found " + bean.getObjectName().toString());
            throw new Exception(methodName + ": Didn't find the specified JDBC Resource Mbean: " + mBean_TYPE
                                + "\n Hint: Maybe the name of the JDBC Resource in the server.xml of this bucket was changed?"
                                + "\n The test was looking for the JDBC Resource name=" + rsName1 + "and" + rsName2);
        }
        con1.close();
        con2.close();
    }

    /**
     * Test JDBC Resource MBean Update.
     */
    public void testJDBCResourceMBeanUpdate1(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String methodName = "testJDBCResourceMBeanUpdate1";
        final String mBean_TYPE = "JDBCResourceMBean";
        final String mBean_TYPE_J2EE = "JDBCResource";
        final String mBean_JNDIName = "jdbc/ds1";

        /*
         * <dataSource id="ds1" jndiName="jdbc/ds1" jdbcDriverRef="FATJDBCDriver">
         * <properties.derby.embedded databaseName="memory:ds1" createDatabase="create" user="dbuser1" password="{xor}Oz0vKDtu" />
         * </dataSource>
         */

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + mBean_TYPE + ",j2eeType=" + mBean_TYPE_J2EE + "*,jndiName=" + mBean_JNDIName + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);

        if (s.size() == 0)
            throw new Exception(className + ": " + methodName + ": Didn't find the specified DataSource Mbean: " + mBean_TYPE
                                + "\n Hint: Maybe the name of the DataSource in the server.xml of this bucket was changed?"
                                + "\n The test was looking for the DataSource id=" + mBean_JNDIName);

        if (s.size() > 1) {
            System.out.println("Found more than one MBean with the same type and id.");
            for (ObjectInstance bean : s)
                System.out.println("**  Found " + bean.getObjectName().toString());
            throw new Exception(className + ": " + methodName + ": Found more than one MBean with the same type and id."
                                + "\n Hint: Maybe the name of the DataSource in the server.xml of this bucket was changed?"
                                + "\n The test was looking for the DataSource id=" + mBean_JNDIName);
        }

        System.out.println(className + ": " + methodName + ": We found the expected MBean before it's getting updated.");
    }

    /**
     * Test JDBC Resource MBean Update.
     */
    public void testJDBCResourceMBeanUpdate2(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String methodName = "testJDBCResourceMBeanUpdate2";
        final String mBean_TYPE = "JDBCResourceMBean";
        final String mBean_TYPE_J2EE = "JDBCResource";
        final String mBean_JNDIName = "jdbc/ds1new";
        final String mBeanOld_JNDIName = "jdbc/ds1";

        /*
         * <dataSource id="newds1" jndiName="jdbc/ds1" jdbcDriverRef="FATJDBCDriver">
         * <properties.derby.embedded databaseName="memory:ds1" createDatabase="create" user="dbuser1" password="{xor}Oz0vKDtu" />
         * </dataSource>
         */

        //Part 1: find the new MBean
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + mBean_TYPE + ",j2eeType=" + mBean_TYPE_J2EE + "*,jndiName=" + mBean_JNDIName + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);

        if (s.size() == 0)
            throw new Exception(className + ": " + methodName + ": Didn't find the specified DataSource Mbean: " + mBean_TYPE
                                + "\n Hint: Maybe the name of the DataSource in the server.xml of this bucket was changed?"
                                + "\n The test was looking for the DataSource jndiName=" + mBean_JNDIName);

        if (s.size() > 1) {
            System.out.println("Found more than one MBean with the same type and id.");
            for (ObjectInstance bean : s)
                System.out.println("**  Found " + bean.getObjectName().toString());
            throw new Exception(className + ": " + methodName + ": Found more than one MBean with the same type and id."
                                + "\n Hint: Maybe the name of the DataSource in the server.xml of this bucket was changed?"
                                + "\n The test was looking for the DataSource jndiName=" + mBean_JNDIName);
        }

        System.out.println(className + ": " + methodName + ": We found the expected MBean after it got updated.");

        //Part 2: Check to see if the old MBean is deleted as expected
        ObjectName obn2 = new ObjectName("WebSphere:type=" + mBean_TYPE + ",j2eeType=" + mBean_TYPE_J2EE + "*,jndiName=" + mBeanOld_JNDIName + ",*");
        Set<ObjectInstance> s2 = mbs.queryMBeans(obn2, null);
        if (s2.size() != 0) {
            for (ObjectInstance bean : s2)
                System.out.println("**  Found " + bean.getObjectName().toString());
            throw new Exception(className + ": " + methodName + ": Found the old MBean which should have been deleted after the update."
                                + "\n The old MBean id=" + mBeanOld_JNDIName);
        }

        System.out.println(className + ": " + methodName + ": We confirmed that the old MBean has been deleted.");
    }
}
