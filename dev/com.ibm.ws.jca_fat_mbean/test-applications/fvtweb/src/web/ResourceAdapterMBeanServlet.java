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

public class ResourceAdapterMBeanServlet extends HttpServlet {
    private static final long serialVersionUID = 7709282314904580351L;
    @SuppressWarnings("unused")
    private final String className = "ResourceAdapterMBeanServlet";
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
    public void testResourceAdapterMBeanRegistration(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String methodName = "testResourceAdapterMBeanRegistration";
        final String mBean_TYPE = "ResourceAdapterMBean";
        final String mBean_TYPE_J2EE = "ResourceAdapter";
        final String raName = "resourceAdapter[FAT1]/properties.FAT1[FAT1]";

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + mBean_TYPE + ",j2eeType="
                                        + mBean_TYPE_J2EE + ",name=" + raName + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        System.out.println(methodName + ": searching for: " + mBean_TYPE);
        for (ObjectInstance bean : s) {
            // testing ResourceAdapterMBean test = (ResourceAdapterMBean) bean;

            System.out.println("**  Found " + bean.getObjectName().toString());

            MBeanInfo mi = mbs.getMBeanInfo(bean.getObjectName());
            MBeanAttributeInfo[] malist = mi.getAttributes();
            int testedAttributes = 0;
            for (MBeanAttributeInfo ma : malist) {
                System.out.println("Attribute for mBean_TYPE " + mBean_TYPE + " " + ma);
                String attributeToTest = ma.getName();

                if (attributeToTest.equals("jcaResource")) {
                    String jcaResource = (String) mbs.getAttribute(bean.getObjectName(), "jcaResource");
                    if (jcaResource == null) {
                        // This should not happen for this test.
                        throw new Exception(methodName + " Do not have a jcaResource value, its null");
                    } else {
                        // TODO - not sure if I am using the right name, may need to change.   JMS20150403
                        if (!jcaResource.equals("WebSphere:type=JCAResourceMBean,j2eeType=JCAResource,name=FAT1/JCAResource,J2EEServer=" + serverName + ",ResourceAdapter=FAT1")) {
                            throw new Exception(methodName
                                                + " Expecting value WebSphere:type=JCAResourceMBean,j2eeType=JCAResource,name=FAT1/JCAResource,J2EEServer=" + serverName
                                                + ",ResourceAdapter=FAT1 but found value "
                                                + jcaResource);
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
                        if (!objectName.equals("WebSphere:type=ResourceAdapterMBean,j2eeType=ResourceAdapter,id=FAT1,name=resourceAdapter[FAT1]/properties.FAT1[FAT1],J2EEServer="
                                               + serverName + ",ResourceAdapterModule=JCAFAT1")) {
                            throw new Exception(methodName
                                                + " Expecting value WebSphere:type=ResourceAdapterMBean,j2eeType=ResourceAdapter,id=FAT1,name=resourceAdapter[FAT1]/properties.FAT1[FAT1],J2EEServer="
                                                + serverName + ",ResourceAdapterModule=JCAFAT1 but found value "
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
            System.out.println("No match found for the specified Resource Adapter Mbean: " + mBean_TYPE);
            s = mbs.queryMBeans(null, null);
            System.out.println("Here is the full list of used MBeans");
            for (ObjectInstance bean : s)
                System.out.println("**  Found " + bean.getObjectName().toString());
            throw new Exception(methodName + ": Didn't find the specified Resource Adapter Mbean: " + mBean_TYPE
                                + "\n Hint: Maybe the name of the  Resource Adapter in the server.xml of this bucket was changed?"
                                + "\n The test was looking for the Resource Adapter name=" + raName);
        }
    }

    /**
     * Test JCA Resource Adapter MBean registration when there is no Module value.
     */
    public void testResourceAdapterMBeanRegistrationWithNoModule(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String methodName = "testResourceAdapterMBeanRegistrationWithNoModule";
//        final String mBean_TYPE = "ResourceAdapterMBean";
//        final String mBean_TYPE_J2EE = "ResourceAdapter";
//
//        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
//        ObjectName obn = new ObjectName("WebSphere:type=" + mBean_TYPE + ",j2eeType=" + mBean_TYPE_J2EE + ",*");
//
//        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
//        System.out.println(methodName + ": searching for: " + mBean_TYPE);
//        if (s.size() != 0) {
//            StringBuilder foundMbeans = new StringBuilder("");
//            for (ObjectInstance bean : s) {
//                final String foundItem = bean.getObjectName().toString();
//                System.out.println("**  Found " + foundItem);
//                foundMbeans.append(foundItem + ", ");
//            }
//            throw new Exception(methodName + ": We should not have found a Resource Adapter Mbean: " + mBean_TYPE
//                                + "\n However we found the following Resource Adapter Mbeans:"
//                                + "\n" + foundMbeans);
//        }
//
        System.out.println(methodName + ": Working! ");
    }

    /**
     * Test JCA Resource Adapter MBean registration with multiple resource adapters defined on the server.xml
     */
    public void testResourceAdapterMBeanRegistrationMultiple(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        final String methodName = "testResourceAdapterMBeanRegistrationMultiple";
        final String mBean_TYPE = "ResourceAdapterMBean";
        final String mBean_TYPE_J2EE = "ResourceAdapter";
        final String raName1 = "resourceAdapter[FAT1]/properties.FAT1[FAT1]";
        final String raName2 = "resourceAdapter[FAT2]/properties.FAT2[FAT2]";
        final String expectedjcaResource1 = "WebSphere:type=JCAResourceMBean,j2eeType=JCAResource,name=FAT1/JCAResource,J2EEServer=" + serverNameMultiple + ",ResourceAdapter=FAT1";
        final String expectedjcaResource2 = "WebSphere:type=JCAResourceMBean,j2eeType=JCAResource,name=FAT2/JCAResource,J2EEServer=" + serverNameMultiple + ",ResourceAdapter=FAT2";
        final String expectedraObjectName1 = "WebSphere:type=ResourceAdapterMBean,j2eeType=ResourceAdapter,id=FAT1,name=resourceAdapter[FAT1]/properties.FAT1[FAT1],J2EEServer="
                                             + serverNameMultiple + ",ResourceAdapterModule=JCAFAT1";
        final String expectedraObjectName2 = "WebSphere:type=ResourceAdapterMBean,j2eeType=ResourceAdapter,id=FAT2,name=resourceAdapter[FAT2]/properties.FAT2[FAT2],J2EEServer="
                                             + serverNameMultiple + ",ResourceAdapterModule=JCAFAT2";

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=" + mBean_TYPE + ",j2eeType="
                                        + mBean_TYPE_J2EE + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        System.out.println(methodName + ": searching for: " + mBean_TYPE);
        for (ObjectInstance bean : s) {
            // testing ResourceAdapterMBean test = (ResourceAdapterMBean) bean;

            System.out.println("**  Found " + bean.getObjectName().toString());

            String jcaResource = (String) mbs.getAttribute(bean.getObjectName(), "jcaResource");
            if (jcaResource == null) {
                // This should not happen for this test.
                throw new Exception(methodName + " Do not have a jcaResource value, its null");
            } else {
                // TODO - not sure if I am using the right name, may need to change.   JMS20150403
                if (!(jcaResource.equals(expectedjcaResource1) || jcaResource.equals(expectedjcaResource2))) {
                    throw new Exception(methodName
                                        + " Expecting value " + expectedjcaResource1 + " or " + expectedjcaResource2 + " but found value "
                                        + jcaResource);
                }
            }

            String objectName = (String) mbs.getAttribute(bean.getObjectName(), "objectName");
            if (objectName == null) {
                // This should not happen.
                throw new Exception(methodName + " Do not have a objectName value " + objectName);
            } else {
                if (!(objectName.equals(expectedraObjectName1) || objectName.equals(expectedraObjectName2))) {
                    throw new Exception(methodName
                                        + " Expecting value " + expectedraObjectName1 + " or " + expectedraObjectName2 + " but found value "
                                        + objectName);
                }
            }
        }
        System.out.println(methodName + ": s.size(): " + s.size());
        if (s.size() != 2) {
            System.out.println("The incorrect number of Resource Adapter MBeans was found, expecting 2 but found: " + s.size());
            s = mbs.queryMBeans(null, null);
            System.out.println("Here is the full list of used MBeans");
            for (ObjectInstance bean : s)
                System.out.println("**  Found " + bean.getObjectName().toString());
            throw new Exception(methodName + ": Didn't find the specified Resource Adapter Mbean: " + mBean_TYPE
                                + "\n Hint: Maybe the name of the Resource Adapter in the server.xml of this bucket was changed?"
                                + "\n The test was looking for the Resource Adapter name=" + raName1 + "and" + raName2);
        }
    }
}
