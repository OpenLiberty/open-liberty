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
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jaxws.jmx.test.fat.util.ClientConnector;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class CXFJMXSupportTest {

    @Server("CXFJMXSupportTestServer")
    public static LibertyServer server;

    private final static int REQUEST_TIMEOUT = 10;
    private static MBeanServerConnection mbsc = null;
    private static final String WEBSPHERE_DOMAIN_NAME = "WebSphere:feature=jaxws,";
    private static final String MBEAN_FIELD_TYPE_KEY = "type";
    private static final String MBEAN_FIELD_INSTANCE_ID_KEY = "instance.id";
    private static final String MBEAN_FIELD_BUS_ID_KEY = "bus.id";
    private static final String MBEAN_FIELD_SERVICE_KEY = "service";
    private static final String MBEAN_FIELD_PORT_KEY = "port";
    private static final String MBEAN_FIELD_NAME_KEY = "name";
    private static final String MBEAN_FIELD_NAME_VALUE_DEFAULT = "NOT_SET";
    private static final String MBEAN_FIELD_NAME_DELIMITER = "@";
    private static final String MBEAN_TYPE_BUS = "Bus";
    private static final String MBEAN_TYPE_WORK_QUEUE_MANAGER = "WorkQueueManager";
    private static final String MBEAN_TYPE_SERVICE_ENDPOINT = "Bus.Service.Endpoint";
    private static final String STR_EMPTY_STRING = "";

    private final static Class<?> thisClass = CXFJMXSupportTest.class;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultDropinApp(server, "testCXFJMXSupport", "com.ibm.ws.jaxws.test.jmx.client",
                                      "com.ibm.ws.jaxws.test.jmx.impl",
                                      "com.ibm.ws.jaxws.test.jmx.service");

        String thisMethod = "setUp()";
        // Ignore unsigned certificate: Copied from com.ibm.ws.channel.ssl.client.test.SimpleHttpsClientTest
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        } };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HostnameVerifier hv = new HostnameVerifier() {
            @Override
            public boolean verify(String urlHostName, SSLSession session) {
                System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hv);

        server.startServer("CXFJMXSupportTest.log");
        //Pause for application to start successfully
        server.waitForStringInLog("CWWKZ0001I.*testCXFJMXSupport");
        //access the service's WSDL first, then MBean for the endpoint of the service will be loaded
        //NOTE: in fact, any service request triggers its MBean's activation
        accessServiceWSDL("WSTestEndpointService");
        //access a client servlet, then client bus will be triggered to load.
        accessURL("JMXClientServlet");
        //Check to make sure jks has been created for restConnector
        server.waitForStringInLog("CWPKI0803A.*");
        //Check to see if Rest service is up
        server.waitForStringInLog("CWWKX0103I.*");

        Log.info(thisClass, thisMethod, "@TJJ before constructing Client");

        ClientConnector cc = new ClientConnector(server.getServerRoot(), server.getHostname(), server.getHttpDefaultSecurePort());
        mbsc = cc.getMBeanServer();
        if (mbsc == null) {
            fail("The MBeanServer connection is null! The test fails.");
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testCXFJMXEnablement() throws Exception {

        Set<ObjectInstance> mbSet = mbsc.queryMBeans(new ObjectName("org.apache.cxf:*"), null);

        assertTrue("The CXF JMX MBeans are not enabled in Liberty.", (mbSet.size() > 0));
    }

    @Test
    public void testAccessBusMBeans() throws Exception {

        Set<ObjectInstance> mbSet = mbsc.queryMBeans(new ObjectName("org.apache.cxf:*"), null);

        int index = 0;
        for (Iterator<ObjectInstance> it = mbSet.iterator(); it.hasNext();) {

            ObjectInstance oi = it.next();
            MBeanInfo beanInfo = mbsc.getMBeanInfo(oi.getObjectName());

            if (String.valueOf(beanInfo.getDescriptor().getFieldValue("name")).equals("Bus")) {
                index++;
            }
        }

        if (index == 0) {
            fail("No Bus MBean is found!");
        }
    }

    @Test
    public void testAccessEndpointMBeans() throws Exception {

        Set<ObjectInstance> mbSet = mbsc.queryMBeans(new ObjectName("org.apache.cxf:*"), null);

        int index = 0;
        for (Iterator<ObjectInstance> it = mbSet.iterator(); it.hasNext();) {

            ObjectInstance oi = it.next();
            MBeanInfo beanInfo = mbsc.getMBeanInfo(oi.getObjectName());
            if (beanInfo.getDescriptor().getFieldValue("name").equals("Endpoint")) {
                index++;
                for (MBeanAttributeInfo attr : beanInfo.getAttributes()) {
                    if (attr.getName().equals("State")) {
                        assertTrue("Endpoint " + oi.getObjectName() + " is not in state \"STARTED\"",
                                   String.valueOf(mbsc.getAttribute(oi.getObjectName(), attr.getName())).equals("STARTED"));
                        break;
                    }
                }
            }

        }

        if (index == 0) {
            fail("No Endpoint MBean is found!");
        }
    }

    @Test
    public void testEndpointMBeanOperations() throws Exception {

        //find the service "WSTestEndpointService" MBean
        Set<ObjectInstance> mbSet = mbsc.queryMBeans(new ObjectName("org.apache.cxf:*"), null);
        ObjectInstance oi = null;
        MBeanInfo beanInfo = null;
        for (Iterator<ObjectInstance> it = mbSet.iterator(); it.hasNext();) {

            oi = it.next();
            beanInfo = mbsc.getMBeanInfo(oi.getObjectName());
            if (oi.getObjectName().toString().indexOf("WSTestEndpointService") > -1 && beanInfo.getDescriptor().getFieldValue("name").equals("Endpoint")) {
                break;
            }
        }

        //run operation
        if (oi != null && beanInfo != null) {

            MBeanOperationInfo[] operations = beanInfo.getOperations();
            if (operations == null || operations.length == 0) {
                fail("Can't find any operation on the MBean for service \"WSTestEndpointService\".");
            }

        } else {
            fail("Can't find the MBean for service \"WSTestEndpointService\".");
        }
    }

    @Test
    public void testAccessEndpointMBeansUnderWebsphereDomain() throws Exception {

        // Get all registered MBeans from WebSphere domain
        Set<ObjectInstance> mbSet = mbsc.queryMBeans(new ObjectName("WebSphere:feature=jaxws,*"), null);

        int index = 0;
        for (Iterator<ObjectInstance> it = mbSet.iterator(); it.hasNext();) {
            ObjectInstance oiUnderWsDomain = it.next();
            MBeanInfo beanInfoUnderWsDomain = mbsc.getMBeanInfo(oiUnderWsDomain.getObjectName());
            if (oiUnderWsDomain.getObjectName().toString().indexOf("type=Bus.Service.Endpoint,") > -1
                && beanInfoUnderWsDomain.getDescriptor().getFieldValue("name").equals("Endpoint")) {
                index++;
                for (MBeanAttributeInfo attr : beanInfoUnderWsDomain.getAttributes()) {
                    if (attr.getName().equals("State")) {
                        assertTrue("Endpoint " + oiUnderWsDomain.getObjectName() + " is not in state \"STARTED\"",
                                   String.valueOf(mbsc.getAttribute(oiUnderWsDomain.getObjectName(), attr.getName())).equals("STARTED"));
                        break;
                    }
                }
            }
        }

        if (index == 0) {
            fail("No Endpoint MBean is found!");
        }
    }

    @Test
    public void testShutdownServerBusFromCxfDomain() throws Exception {

        // find the Server-Bus MBean
        Set<ObjectInstance> mbSet = mbsc.queryMBeans(new ObjectName("org.apache.cxf:*"), null);
        ObjectInstance oi = null;
        MBeanInfo beanInfo = null;
        for (Iterator<ObjectInstance> it = mbSet.iterator(); it.hasNext();) {

            oi = it.next();
            beanInfo = mbsc.getMBeanInfo(oi.getObjectName());
            if (oi.getObjectName().toString().indexOf("type=Bus,") > -1 && beanInfo.getDescriptor().getFieldValue("name").equals("Bus")) {
                break;
            }
        }

        // run operation
        if (oi != null && beanInfo != null) {

            MBeanOperationInfo[] operations = beanInfo.getOperations();
            if (operations == null || operations.length == 0) {
                fail("Can't find any operation on the MBean for Server-Bus.");
            } else {
                for (MBeanOperationInfo operation : operations) {
                    if (operation.getName().equals("shutdown")) {
                        try {
                            mbsc.invoke(oi.getObjectName(), "shutdown", new Object[] { true }, new String[] { boolean.class.getName() });
                            fail("Invoke operation shutdown on the MBean for Server-Bus, no Exception caught!");
                        } catch (Exception e) {
                            // It is good if exception is thrown.
                        }
                        break;
                    }
                }
            }

        } else {
            fail("Can't find the MBean for Server-Bus.");
        }
    }

    @Test
    public void testShutdownWorkQueueManagerFromCxfDomain() throws Exception {

        // find the WorkQueueManager MBean
        Set<ObjectInstance> mbSet = mbsc.queryMBeans(new ObjectName("org.apache.cxf:*"), null);
        ObjectInstance oi = null;
        MBeanInfo beanInfo = null;
        for (Iterator<ObjectInstance> it = mbSet.iterator(); it.hasNext();) {

            oi = it.next();
            beanInfo = mbsc.getMBeanInfo(oi.getObjectName());
            if (oi.getObjectName().toString().indexOf("type=WorkQueueManager,") > -1 && beanInfo.getDescriptor().getFieldValue("name").equals("WorkQueueManager")) {
                break;
            }
        }

        // run operation
        if (oi != null && beanInfo != null) {

            MBeanOperationInfo[] operations = beanInfo.getOperations();
            if (operations == null || operations.length == 0) {
                fail("Can't find any operation on the MBean for WorkQueueManager.");
            } else {
                for (MBeanOperationInfo operation : operations) {
                    if (operation.getName().equals("shutdown")) {
                        try {
                            mbsc.invoke(oi.getObjectName(), "shutdown", new Object[] { true }, new String[] { boolean.class.getName() });
                            fail("Invoke operation shutdown on the MBean for WorkQueueManager, no Exception caught!");
                        } catch (Exception e) {
                            // It is good if exception is thrown.
                        }
                        break;
                    }
                }
            }

        } else {
            fail("Can't find the MBean for WorkQueueManager.");
        }
    }

    @Test
    public void testDestroyEndpointFromCxfDomain() throws Exception {

        // find the Endpoint MBean
        Set<ObjectInstance> mbSet = mbsc.queryMBeans(new ObjectName("org.apache.cxf:*"), null);
        ObjectInstance oi = null;
        MBeanInfo beanInfo = null;
        for (Iterator<ObjectInstance> it = mbSet.iterator(); it.hasNext();) {

            oi = it.next();
            beanInfo = mbsc.getMBeanInfo(oi.getObjectName());
            if (oi.getObjectName().toString().indexOf("type=Bus.Service.Endpoint,") > -1 && beanInfo.getDescriptor().getFieldValue("name").equals("Endpoint")) {
                break;
            }
        }

        // run operation
        if (oi != null && beanInfo != null) {

            MBeanOperationInfo[] operations = beanInfo.getOperations();
            if (operations == null || operations.length == 0) {
                fail("Can't find any operation on the MBean for Endpoint.");
            } else {
                for (MBeanOperationInfo operation : operations) {
                    if (operation.getName().equals("destroy")) {
                        try {
                            mbsc.invoke(oi.getObjectName(), "destroy", new Object[] { true }, new String[] { boolean.class.getName() });
                            fail("Invoke operation destroy on the MBean for Endpoint, no Exception caught!");
                        } catch (Exception e) {
                            // It is good if exception is thrown.
                        }
                        break;
                    }
                }
            }

        } else {
            fail("Can't find the MBean for Endpoint.");
        }
    }

    @Test
    public void testDestroyEndpointFromWebSphereDomain() throws Exception {

        // find the Endpoint MBean
        Set<ObjectInstance> mbSet = mbsc.queryMBeans(new ObjectName("WebSphere:feature=jaxws,*"), null);
        ObjectInstance oi = null;
        MBeanInfo beanInfo = null;
        for (Iterator<ObjectInstance> it = mbSet.iterator(); it.hasNext();) {

            oi = it.next();
            beanInfo = mbsc.getMBeanInfo(oi.getObjectName());
            if (oi.getObjectName().toString().indexOf("type=Bus.Service.Endpoint,") > -1 && beanInfo.getDescriptor().getFieldValue("name").equals("Endpoint")) {
                break;
            }
        }

        // run operation
        if (oi != null && beanInfo != null) {

            MBeanOperationInfo[] operations = beanInfo.getOperations();
            if (operations == null || operations.length == 0) {
                fail("Can't find any operation on the MBean for Endpoint.");
            } else {
                for (MBeanOperationInfo operation : operations) {
                    if (operation.getName().equals("destroy")) {
                        try {
                            mbsc.invoke(oi.getObjectName(), "destroy", new Object[] { true }, new String[] { boolean.class.getName() });
                            fail("Invoke operation destroy on the MBean for Endpoint, no Exception caught!");
                        } catch (Exception e) {
                            // It is good if exception is thrown.
                        }
                        break;
                    }
                }
            }

        } else {
            fail("Can't find the MBean for Endpoint.");
        }
    }

    public static void accessServiceWSDL(String service) throws ProtocolException, IOException {
        accessURL(service + "?wsdl");
    }

    public static void accessURL(String requestURL) throws ProtocolException, IOException {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/testCXFJMXSupport/" + requestURL);
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
        BufferedReader br = null;
        try {
            br = HttpUtils.getConnectionStream(con);
            br.readLine();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private String generateObjectNameForWebSphereDomain(ObjectName name) throws MBeanRegistrationException {
        String targetNameStr = null;
        if (null != name) {
            String objectNameStr = name.toString();
            String domainName = name.getDomain();
            if ((null != objectNameStr) && (objectNameStr.length() > 0) &&
                (null != domainName) && (domainName.length() > 0)) {
                targetNameStr = objectNameStr.replaceFirst((domainName + ":"), WEBSPHERE_DOMAIN_NAME);
            }

            // Get original name attribute, if it is not present then add it.
            String nameValue = name.getKeyProperty(MBEAN_FIELD_NAME_KEY);
            if ((null == nameValue) || (nameValue.length() <= 0)) {

                // No "name" attribute, need to add it.
                String nameTargetValue = MBEAN_FIELD_NAME_VALUE_DEFAULT;
                String typeValue = name.getKeyProperty(MBEAN_FIELD_TYPE_KEY);
                if ((null != typeValue) && (typeValue.equalsIgnoreCase(MBEAN_TYPE_SERVICE_ENDPOINT))) {

                    // If it is web service endpoint, we will reformat the name field to format: <BUS>@<SERVICE>@<PORT>
                    String busIdValue = name.getKeyProperty(MBEAN_FIELD_BUS_ID_KEY);
                    String serviceValue = name.getKeyProperty(MBEAN_FIELD_SERVICE_KEY);
                    String portValue = name.getKeyProperty(MBEAN_FIELD_PORT_KEY);
                    String instanceIdValue = name.getKeyProperty(MBEAN_FIELD_INSTANCE_ID_KEY);

                    // Get url pattern, by default this is the same value as serviceName specified in WebService annotation.
                    // It can be customized by specifying <url-pattern> in web.xml
                    // In some case the customer might have multiple web service instance, which busId/service/endpoint are
                    // the same, the only difference between instances is this urlPattern.
                    // Therefore we will combine this information in name field to make it as unique for each instance.
                    String addressValue2 = null;
                    try {
                        addressValue2 = String.valueOf(mbsc.getAttribute(name, "Address"));
                    } catch (Exception e) {
                    }
                    final String addressValue = addressValue2;
                    String urlPattern = STR_EMPTY_STRING;

                    // We could get address likes "/testService" or we could get likes "http://localhost:80/TestApp/testService".
                    // In some special case the value will be null
                    if (null != addressValue) {
                        // Get only the path part of URL, get ride of protocol/hostname/port
                        urlPattern = AccessController.doPrivileged(new PrivilegedAction<String>() {
                            @Override
                            public String run() {
                                try {
                                    URL addressURL = new URL(addressValue);
                                    return addressURL.getPath();
                                } catch (MalformedURLException e) {
                                    // URL Parse Error, means the addressValue is not valid URL format, this could happen before
                                    // address value is initialized (web service endpoint was not invoked), in such case use the
                                    // address value directly.
                                    return addressValue;
                                }
                            }
                        });
                    }

                    if (((null == urlPattern) || (urlPattern.length() <= 0)) && (null != instanceIdValue)) {
                        // If urlPattern is null or empty value, but instance id from CXF is available, use it instead
                        urlPattern = instanceIdValue;
                    }

                    if ((null != busIdValue) && (null != serviceValue) && (null != portValue)) {

                        // Value of name is combination as format "<BUS>@<SERVICE>@<PORT>@<URL_PATTERN>"
                        nameTargetValue = "\"" + busIdValue.replace("\"", "") + MBEAN_FIELD_NAME_DELIMITER +
                                          serviceValue.replace("\"", "") + MBEAN_FIELD_NAME_DELIMITER +
                                          portValue.replace("\"", "") + MBEAN_FIELD_NAME_DELIMITER +
                                          urlPattern.replace("/", "") + "\"";

                        // Re-format object name for WebSphere domain, please note the instance.id is not included.
                        // The instance.id is meaningless as it changes every time when server is restarted.
                        targetNameStr = (new StringBuffer(WEBSPHERE_DOMAIN_NAME)).append(MBEAN_FIELD_BUS_ID_KEY).append("=").append(busIdValue).append(",").append(MBEAN_FIELD_TYPE_KEY).append("=").append(typeValue).append(",").append(MBEAN_FIELD_SERVICE_KEY).append("=").append(serviceValue).append(",").append(MBEAN_FIELD_PORT_KEY).append("=").append(portValue).append(",").append(MBEAN_FIELD_NAME_KEY).append("=").append(nameTargetValue).toString();
                    } else {
                        // No enough information to spell name value.
                        targetNameStr += "," + MBEAN_FIELD_NAME_KEY + "=" + nameTargetValue;
                    }
                } else {
                    // Not web service endpoint, no rule defined, so use default.
                    targetNameStr += "," + MBEAN_FIELD_NAME_KEY + "=" + nameTargetValue;
                }
            } else {
                // if "name" is already presents in object name, then no need to add it.
            }

        }
        return targetNameStr;
    }
}
