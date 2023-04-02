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

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JCAResourceMBeanServlet extends HttpServlet {
    private static final long serialVersionUID = 7709282314904580347L;
    @SuppressWarnings("unused")
    private final String className = "JCAResourceMBeanServlet";
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
     * Test JCA Resource MBean registration.
     */
    public void testJCAResourceMBeanRegistration(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String methodName = "testJCAResourceMBeanRegistration";
        final String mBean_TYPE = "JCAResourceMBean";//type=JCAResourceMBean,j2eeType=JCAResource,name=
        final String mBean_TYPE_J2EE = "JCAResource";
        final String jcarName = "jmsConnectionFactory[cf6]/JCAResource";
        final String expectedObjectName1 = "WebSphere:type=JCAConnectionFactoryMBean,j2eeType=JCAConnectionFactory,id=cf1,jndiName=jms/cf1,"
                                           + "name=jmsConnectionFactory[cf1],J2EEServer=" + serverName + ",JCAResource=FAT1";
        final String expectedObjectName2 = "WebSphere:type=JCAConnectionFactoryMBean,j2eeType=JCAConnectionFactory,id=cf6,jndiName=jms/cf6,"
                                           + "name=jmsConnectionFactory[cf6],J2EEServer=" + serverName + ",JCAResource=FAT1";

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + mBean_TYPE + ",j2eeType=" + mBean_TYPE_J2EE + ",*"); //+ ",name=" + jcarName + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        System.out.println(methodName + ": searching for: " + mBean_TYPE);
        for (ObjectInstance bean : s)
            System.out.println("**  Found " + bean.getObjectName().toString());
        System.out.println(methodName + ": s.size(): " + s.size());
        if (s.size() > 0) {
            for (ObjectInstance bean : s) {
                // testing JCAResourceMBean test = (JCAResourceMBean) bean;

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

                    if (attributeToTest.equals("connectionFactories")) {
                        String[] connectionFactories = (String[]) mbs.getAttribute(bean.getObjectName(), "connectionFactories");
                        if (connectionFactories == null) {
                            // This should not happen for this test.
                            throw new Exception(methodName + " Do not have a connectionFactories value, its null");
                        } else {
                            // At minimum, find two of them
                            int counter = 0;
                            for (String test : connectionFactories) {
                                if (test.equals(expectedObjectName1)) {
                                    ++counter;
                                }
                                if (test.equals(expectedObjectName2)) {
                                    ++counter;
                                }
                            }
                            if (counter < 2) {
                                StringBuilder foundValues = new StringBuilder("");
                                for (String foundItem : connectionFactories)
                                    foundValues.append(foundItem + ",");
                                throw new Exception(methodName + " Expecting at minimum values " + expectedObjectName1 + "\n and "
                                                    + expectedObjectName2 + "\n but found values \n" + foundValues);
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
                            if (!objectName.equals("WebSphere:type=JCAResourceMBean,j2eeType=JCAResource,name=FAT1/JCAResource,J2EEServer=" + serverName
                                                   + ",ResourceAdapter=FAT1")) {
                                throw new Exception(methodName
                                                    + " Expecting value WebSphere:type=JCAResourceMBean,j2eeType=JCAResource,name=FAT1/JCAResource,J2EEServer=" + serverName
                                                    + ",ResourceAdapter=FAT1 but found value "
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
        } else {
            System.out.println("No match found for the specified Resource Adapter Mbean: " + mBean_TYPE);
            s = mbs.queryMBeans(null, null);
            System.out.println("Here is the full list of used MBeans");
            for (ObjectInstance bean : s) {
                System.out.println("**  Found " + bean.getObjectName().toString());
            }
            throw new Exception(methodName + ": Didn't find the specified Resource Adapter Mbean: " + mBean_TYPE
                                + "\n Hint: Maybe the name of the Resource Adapter in the server.xml of this bucket was changed?"
                                + "\n The test was looking for the jca resource name=" + jcarName);

        }
    }

    /**
     * Test JCA Resource MBean registration with multiple resources defined on the server.xml.
     */
    public void testJCAResourceMBeanRegistrationMultiple(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String methodName = "testJCAResourceMBeanRegistrationMultiple";
        final String mBean_TYPE = "JCAResourceMBean";//type=JCAResourceMBean,j2eeType=JCAResource,name=
        final String mBean_TYPE_J2EE = "JCAResource";
        final String jcarName1 = "jmsConnectionFactory[cf1]/JCAResource";
        final String jcarName2 = "jmsConnectionFactory[cf6]/JCAResource";
        final String expectedcfObjectName1 = "WebSphere:type=JCAConnectionFactoryMBean,j2eeType=JCAConnectionFactory,id=cf1,jndiName=jms/cf1,"
                                             + "name=jmsConnectionFactory[cf1],J2EEServer=" + serverNameMultiple + ",JCAResource=FAT1";
        final String expectedcfObjectName2 = "WebSphere:type=JCAConnectionFactoryMBean,j2eeType=JCAConnectionFactory,id=cf6,jndiName=jms/cf6,"
                                             + "name=jmsConnectionFactory[cf6],J2EEServer=" + serverNameMultiple + ",JCAResource=FAT2";
        final String expectedResourceObjectName1 = "WebSphere:type=JCAResourceMBean,j2eeType=JCAResource,name=FAT1/JCAResource,J2EEServer=" + serverNameMultiple
                                                   + ",ResourceAdapter=FAT1";
        final String expectedResourceObjectName2 = "WebSphere:type=JCAResourceMBean,j2eeType=JCAResource,name=FAT2/JCAResource,J2EEServer=" + serverNameMultiple
                                                   + ",ResourceAdapter=FAT2";
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + mBean_TYPE + ",j2eeType=" + mBean_TYPE_J2EE + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        System.out.println(methodName + ": searching for: " + mBean_TYPE);
        for (ObjectInstance bean : s)
            System.out.println("**  Found " + bean.getObjectName().toString());
        System.out.println(methodName + ": s.size(): " + s.size());
        if (s.size() > 0) {
            for (ObjectInstance bean : s) {
                // testing JCAResourceMBean test = (JCAResourceMBean) bean;

                System.out.println("**  Found " + bean.getObjectName().toString());
                String objectName = (String) mbs.getAttribute(bean.getObjectName(), "objectName");
                String[] connectionFactories = (String[]) mbs.getAttribute(bean.getObjectName(), "connectionFactories");
                if (connectionFactories == null) {
                    // This should not happen for this test.
                    throw new Exception(methodName + " Do not have a connectionFactories value, its null");
                } else {
                    // Find the associated connectionfactory
                    int counter = 0;
                    for (String test : connectionFactories) {
                        if (test.equals(expectedcfObjectName1) && objectName.equals(expectedResourceObjectName1)) {
                            ++counter;
                        }
                        if (test.equals(expectedcfObjectName2) && objectName.equals(expectedResourceObjectName2)) {
                            ++counter;
                        }
                    }
                    if (connectionFactories.length != 1) {
                        throw new Exception(methodName + "Expecting to find 1 connection factory, but found " + connectionFactories.length);
                    }
                    if (counter != 1) {
                        StringBuilder foundValues = new StringBuilder("");
                        for (String foundItem : connectionFactories)
                            foundValues.append(foundItem + ",");
                        throw new Exception(methodName + " Expecting value " + expectedcfObjectName1 + "\n or "
                                            + expectedcfObjectName2 + "\n but found value \n" + foundValues);
                    }
                }

                if (objectName == null) {
                    // This should not happen.
                    throw new Exception(methodName + " Do not have a objectName value " + objectName);
                } else {
                    if (!(objectName.equals(expectedResourceObjectName1)
                          || objectName.equals(expectedResourceObjectName2))) {
                        throw new Exception(methodName
                                            + " Expecting value " + expectedResourceObjectName1 + " or " + expectedResourceObjectName2 + " but found value "
                                            + objectName);
                    }
                }
            }
        } else {
            System.out.println("No match found for the specified Resource Adapter Mbean: " + mBean_TYPE);
            s = mbs.queryMBeans(null, null);
            System.out.println("Here is the full list of used MBeans");
            for (ObjectInstance bean : s) {
                System.out.println("**  Found " + bean.getObjectName().toString());
            }
            throw new Exception(methodName + ": Didn't find the specified Resource Adapter Mbean: " + mBean_TYPE
                                + "\n Hint: Maybe the name of the Resource Adapter in the server.xml of this bucket was changed?"
                                + "\n The test was looking for the jca resource name=" + jcarName1 + " and " + jcarName2);

        }
    }
}
