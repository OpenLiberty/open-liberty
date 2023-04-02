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
import java.util.ArrayList;
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

public class ResourceAdapterModuleMBeanServlet extends HttpServlet {
    private static final long serialVersionUID = 7709282314904591351L;
    private final String className = "ResourceAdapterModuleMBeanServlet";
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
     * Test JCA Resource Adapter MBean registration.
     */
    public void testResourceAdapterModuleMBeanRegistration(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String methodName = "testResourceAdapterModuleMBeanRegistration";
        final String mBean_TYPE = "ResourceAdapterModuleMBean";
        final String mBean_TYPE_J2EE = "ResourceAdapterModule";
        final String ramName = "resourceAdapter[FAT1]/properties.FAT1[FAT1]";

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + mBean_TYPE + ",j2eeType="
                                        + mBean_TYPE_J2EE + ",name=" + ramName + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        System.out.println(methodName + ": searching for: " + obn.toString());
        for (ObjectInstance bean : s) {
            // testing ResourceAdapterModuleMBean test = (ResourceAdapterModuleMBean) bean;

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

                if (attributeToTest.equals("server")) {
                    String server = (String) mbs.getAttribute(bean.getObjectName(), "server");
                    if (server == null) {
                        // This should not happen for this test.
                        throw new Exception(methodName + " Do not have a server value, its null");
                    } else {
                        if (!server.equals(serverName)) {
                            throw new Exception(methodName + " Expecting value " + serverName + " but found value " + server);
                        }
                    }
                    ++testedAttributes;
                }

                if (attributeToTest.equals("resourceAdapters")) {
                    String[] resourceAdapters = (String[]) mbs.getAttribute(bean.getObjectName(), "resourceAdapters");

                    if (resourceAdapters == null) {
                        // This should not happen for this test.
                        throw new Exception(methodName + " Do not have a resourceAdapters value, its null");
                    } else {
                        // TODO - not sure if the name is right, we are using the object name JMS20150402
                        // At minimum, find one of them
                        int counter = 0;
                        for (String test : resourceAdapters) {
                            if (test.equals("WebSphere:type=ResourceAdapterMBean,j2eeType=ResourceAdapter,id=FAT1,name=resourceAdapter[FAT1]/properties.FAT1[FAT1],J2EEServer="
                                            + serverName + ",ResourceAdapterModule=JCAFAT1")) {
                                ++counter;
                            }
                        }
                        if (counter < 1) {
                            throw new Exception(methodName
                                                + " Expecting at minimum value WebSphere:type=ResourceAdapterMBean,j2eeType=ResourceAdapter,id=FAT1,name=resourceAdapter[FAT1]/properties.FAT1[FAT1],J2EEServer="
                                                + serverName + ",ResourceAdapterModule=JCAFAT1 but found value "
                                                + resourceAdapters);
                        }

                    }
                    ++testedAttributes;
                }

                if (attributeToTest.equals("javaVMs")) {
                    String[] javaVMs = (String[]) mbs.getAttribute(bean.getObjectName(), "javaVMs");
                    // TODO - need to javaVMs working JMS20150402
                    if (javaVMs == null) {
                        // This should not happen for this test.
                        throw new Exception(methodName + " Do not have a javaVMs value, its null");
                    } else {
//                        if (!javaVMs.equals("fat.jca.resourceadapter.FVTManagedConnectionFactory")) {
//                            throw new Exception(methodName + " Expecting value fat.jca.resourceadapter.FVTManagedConnectionFactory but found value " + javaVMs);
//                        }
                        // At minimum, find one of them
                        int counter = 0;
                        for (String test : javaVMs) {
                            if (test.equals("WebSphere:name=JVM,J2EEServer=" + serverName + ",j2eeType=JVM")) {
                                ++counter;
                            }
                        }
                        if (counter < 1) {
                            throw new Exception(methodName
                                                + " Expecting at minimum value WebSphere:type=ResourceAdapterMBean,j2eeType=ResourceAdapter,id=FAT1,name=resourceAdapter[FAT1]/properties.FAT1[FAT1],J2EEServer="
                                                + serverName + ",ResourceAdapterModule=JCAFAT1 but found value "
                                                + javaVMs);
                        }

                    }
                    ++testedAttributes;
                }

                if (attributeToTest.equals("deploymentDescriptor")) {
                    String deploymentDescriptor = (String) mbs.getAttribute(bean.getObjectName(), "deploymentDescriptor");

                    if (deploymentDescriptor == null) {
                        // This should not happen for this test.
                        throw new Exception(methodName + " Do not have a deploymentDescriptor value, its null");
                    } else {
                        String xmlStartsWith = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
                        if (!deploymentDescriptor.startsWith(xmlStartsWith))
                            throw new Exception(methodName + " Expecting deployment descriptor to start with " + xmlStartsWith
                                                + " but instead the deployment descriptor is " + deploymentDescriptor);
                        String xmlContains = "<connector xmlns=\"http://java.sun.com/xml/ns/javaee\""
                                             + " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee"
                                             + " http://java.sun.com/xml/ns/javaee/connector_1_6.xsd\""
                                             + " version=\"1.6\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                                             + System.lineSeparator()
                                             + "  <description>This is the ra.xml for RAR1</description>";
                        if (!deploymentDescriptor.contains(xmlContains))
                            throw new Exception(methodName + " Expecting deployment descriptor to contain " + xmlContains
                                                + " but instead the deployment descriptor is " + deploymentDescriptor);
                    }

                    ++testedAttributes;
                }

                if (attributeToTest.equals("objectName")) {
                    String objectName = (String) mbs.getAttribute(bean.getObjectName(), "objectName");
                    if (objectName == null) {
                        // This should not happen.
                        throw new Exception(methodName + " Do not have a objectName value " + objectName);
                    } else {
                        if (!objectName.equals("WebSphere:type=ResourceAdapterModuleMBean,j2eeType=ResourceAdapterModule,id=FAT1,name=resourceAdapter[FAT1]/properties.FAT1[FAT1],J2EEServer="
                                               + serverName + "")) {
                            throw new Exception(methodName
                                                + " Expecting value WebSphere:type=ResourceAdapterModuleMBean,j2eeType=ResourceAdapterModule,id=FAT1,name=resourceAdapter[FAT1]/properties.FAT1[FAT1],J2EEServer="
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
            if (malist.length != 8) {
                throw new Exception(methodName + " Initial attributes tested were 5 "
                                    + "which is not equal to the actual attributes now "
                                    + malist.length + " this test will need to be updated to include or " +
                                    " remove attribute tests");
            }
        }
        System.out.println(methodName + ": s.size(): " + s.size());
        if (s.size() != 1) {
            System.out.println("No match found for the specified Resource Adapter Module Mbean: " + mBean_TYPE);
            s = mbs.queryMBeans(null, null);
            System.out.println("Here is the full list of used MBeans");
            for (ObjectInstance bean : s)
                System.out.println("**  Found " + bean.getObjectName().toString());
            throw new Exception(className + ": " + methodName + ": Didn't find the specified Resource Adapter Mbean: " + mBean_TYPE
                                + "\n Hint: Maybe the name of the Resource Adapter in the server.xml of this bucket was changed?"
                                + "\n The test was looking for the resource adaptor module name=" + ramName);
        }

    }

    /**
     * Test JCA Resource Adapter MBean registration with multiple resource adapters defined on the server.xml.
     */
    public void testResourceAdapterModuleMBeanRegistrationMultiple(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String methodName = "testResourceAdapterModuleMBeanRegistrationMultiple";
        final String mBean_TYPE = "ResourceAdapterModuleMBean";
        final String mBean_TYPE_J2EE = "ResourceAdapterModule";
        final String ramName1 = "resourceAdapter[FAT1]/properties.FAT1[FAT1]";
        final String ramName2 = "resourceAdapter[FAT2]/properties.FAT2[FAT2]";
        final String expectedraObjectName1 = "WebSphere:type=ResourceAdapterMBean,j2eeType=ResourceAdapter,id=FAT1,name=resourceAdapter[FAT1]/properties.FAT1[FAT1],J2EEServer="
                                             + serverNameMultiple + ",ResourceAdapterModule=JCAFAT1";
        final String expectedraObjectName2 = "WebSphere:type=ResourceAdapterMBean,j2eeType=ResourceAdapter,id=FAT2,name=resourceAdapter[FAT2]/properties.FAT2[FAT2],J2EEServer="
                                             + serverNameMultiple + ",ResourceAdapterModule=JCAFAT2";
        final String expectedramObjectName1 = "WebSphere:type=ResourceAdapterModuleMBean,j2eeType=ResourceAdapterModule,id=FAT1,name=resourceAdapter[FAT1]/properties.FAT1[FAT1],J2EEServer="
                                              + serverNameMultiple + "";
        final String expectedramObjectName2 = "WebSphere:type=ResourceAdapterModuleMBean,j2eeType=ResourceAdapterModule,id=FAT2,name=resourceAdapter[FAT2]/properties.FAT2[FAT2],J2EEServer="
                                              + serverNameMultiple + "";

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + mBean_TYPE + ",j2eeType="
                                        + mBean_TYPE_J2EE + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        System.out.println(methodName + ": searching for: " + obn.toString());
        for (ObjectInstance bean : s) {
            // testing ResourceAdapterModuleMBean test = (ResourceAdapterModuleMBean) bean;

            System.out.println("**  Found " + bean.getObjectName().toString());

            String objectName = (String) mbs.getAttribute(bean.getObjectName(), "objectName");
            String[] resourceAdapters = (String[]) mbs.getAttribute(bean.getObjectName(), "resourceAdapters");

            if (resourceAdapters == null) {
                // This should not happen for this test.
                throw new Exception(methodName + " Do not have a resourceAdapters value, its null");
            } else {
                // Find two of them
                int counter = 0;
                for (String test : resourceAdapters) {
                    if (test.equals(expectedraObjectName1) && objectName.equals(expectedramObjectName1)) {
                        ++counter;
                    }
                    if (test.equals(expectedraObjectName2) && objectName.equals(expectedramObjectName2)) {
                        ++counter;
                    }
                }

                if (resourceAdapters.length != 1) {
                    throw new Exception(methodName
                                        + " Expecting to find 1 resource adapter but found " + resourceAdapters.length);
                }

                if (counter != 1) {
                    throw new Exception(methodName
                                        + "Expecting value " + expectedraObjectName1 + " or " + expectedraObjectName2 + " but found values "
                                        + resourceAdapters);
                }

            }

            if (objectName == null) {
                // This should not happen.
                throw new Exception(methodName + " Do not have a objectName value " + objectName);
            } else {
                if (!(objectName.equals(expectedramObjectName1) || objectName.equals(expectedramObjectName2))) {
                    throw new Exception(methodName
                                        + " Expecting values " + expectedramObjectName1 + " or " + expectedramObjectName2 + " but found value "
                                        + objectName);
                }
            }

        }
        System.out.println(methodName + ": s.size(): " + s.size());
        if (s.size() != 2) {
            System.out.println("The incorrect number of Resource Adapter Module MBeans was found, expecting 2 but found: " + s.size());
            s = mbs.queryMBeans(null, null);
            System.out.println("Here is the full list of used MBeans");
            for (ObjectInstance bean : s)
                System.out.println("**  Found " + bean.getObjectName().toString());
            throw new Exception(methodName + ": Didn't find the specified Resource Adapter Module Mbean: " + mBean_TYPE
                                + "\n Hint: Maybe the name of the Resource Adapter Module in the server.xml of this bucket was changed?"
                                + "\n The test was looking for the Resource Adapter Module name=" + ramName1 + "and" + ramName2);
        }

    }

    /**
     * Test JCA Resource Adapter Module MBean getdeploymentDescriptor() function with multiple unique resource adapters defined on the server.xml.
     */
    public void testResourceAdapterModuleMBeanMultipleDD(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String methodName = "testResourceAdapterModuleMBeanRegistrationMultiple";
        final String mBean_TYPE = "ResourceAdapterModuleMBean";
        final String mBean_TYPE_J2EE = "ResourceAdapterModule";
        final String expectedraObjectName1 = "WebSphere:type=ResourceAdapterModuleMBean,j2eeType=ResourceAdapterModule,id=FAT1,name=resourceAdapter[FAT1]/properties.FAT1[FAT1],J2EEServer="
                                             + serverNameMultiple + "";
        final String expectedraObjectName2 = "WebSphere:type=ResourceAdapterModuleMBean,j2eeType=ResourceAdapterModule,id=FAT2,name=resourceAdapter[FAT2]/properties.FAT2[FAT2],J2EEServer="
                                             + serverNameMultiple + "";

        final ArrayList<String> excList = new ArrayList<String>();
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + mBean_TYPE + ",j2eeType=" + mBean_TYPE_J2EE + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        System.out.println(methodName + ": searching for: " + obn.toString());
        for (ObjectInstance bean : s) {
            System.out.println("**  Found " + bean.getObjectName().toString());
            String objectName = (String) mbs.getAttribute(bean.getObjectName(), "objectName");
            if (objectName.equals(expectedraObjectName1)) {
                String deploymentDescriptor = (String) mbs.getAttribute(bean.getObjectName(), "deploymentDescriptor");
                if (deploymentDescriptor == null) {
                    throw new Exception(className + ": " + methodName + " Do not have a deploymentDescriptor value, its null");
                } else {
                    String startsWith = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
                    if (!deploymentDescriptor.startsWith(startsWith))
                        excList.add(className + ": " + methodName + ": Case 01: Expecting value start with " + startsWith + " but the found value is " + deploymentDescriptor);

                    String contains = "<connector xmlns=\"http://java.sun.com/xml/ns/javaee\""
                                      + " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee"
                                      + " http://java.sun.com/xml/ns/javaee/connector_1_6.xsd\""
                                      + " version=\"1.6\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                                      + System.lineSeparator()
                                      + "  <description>This is the ra.xml for RAR1</description>";
                    if (!deploymentDescriptor.contains(contains))
                        excList.add(className + ": " + methodName + ": Case 01: Expecting value contains " + contains
                                    + " but the found value is " + deploymentDescriptor);
                }
            } else if (objectName.equals(expectedraObjectName2)) {
                String deploymentDescriptor = (String) mbs.getAttribute(bean.getObjectName(), "deploymentDescriptor");
                if (deploymentDescriptor == null) {
                    throw new Exception(className + ": " + methodName + " Do not have a deploymentDescriptor value, its null");
                } else {
                    String startsWith = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
                    if (!deploymentDescriptor.startsWith(startsWith))
                        excList.add(className + ": " + methodName + ": Case 02: Expecting value start with " + startsWith
                                    + " but the found value is " + deploymentDescriptor);

                    String contains = "<connector xmlns=\"http://java.sun.com/xml/ns/javaee\""
                                      + " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee"
                                      + " http://java.sun.com/xml/ns/javaee/connector_1_6.xsd\""
                                      + " version=\"1.6\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                                      + System.lineSeparator()
                                      + "  <description>This is the ra.xml for RAR2</description>";
                    if (!deploymentDescriptor.contains(contains))
                        excList.add(className + ": " + methodName + ": Case 02: Expecting value contains " + contains + " but the found value is "
                                    + deploymentDescriptor);
                }
            } else
                throw new Exception(className + ": " + methodName + ": Case 03: Found Unexpected ObjectNam = " + objectName);

            if (excList.size() != 0)
                throw new Exception("Number of Exceptions is: " + excList.size() + System.lineSeparator() + excList.toString());
        }
    }
}
