/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.bvt;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;

import com.ibm.websphere.config.mbeans.FeatureListMBean;
import com.ibm.websphere.config.mbeans.ServerSchemaGenerator;
import com.ibm.websphere.filetransfer.FileTransferMBean;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jmx.connector.client.rest.ClientProvider;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

/**
 *
 */
public class SchemaMBeanTest {

    private static Class<?> logClass = SchemaMBeanTest.class;

    private static String outputDir;
    private static MBeanServerConnection connection;
    private static JMXConnector jmxConnector;
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule managerRule = outputMgr;

    @BeforeClass
    public static void setUp() throws Exception {
        final String methodName = "setUp";
        Log.entering(logClass, methodName);

        String installDir = System.getProperty("install.dir");
        WsLocationAdmin locMgr = (WsLocationAdmin) SharedLocationManager.createDefaultLocations(installDir, getProfile());

        outputDir = locMgr.resolveString("${server.output.dir}");
        Log.info(logClass, methodName, "serverRoot=" + outputDir);

        // Set up the trust store
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
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hv);

        Map<String, Object> environment = new HashMap<String, Object>();
        environment.put("jmx.remote.protocol.provider.pkgs", "com.ibm.ws.jmx.connector.client");
        environment.put(JMXConnector.CREDENTIALS, new String[] { "theUser", "thePassword" });
        environment.put(ClientProvider.DISABLE_HOSTNAME_VERIFICATION, true);
        environment.put(ClientProvider.READ_TIMEOUT, 2 * 60 * 1000);
        JMXServiceURL url = new JMXServiceURL("REST", "localhost", getSSLPort(), "/IBMJMXConnectorREST");
        Log.info(logClass, methodName, "JMXServiceURL: " + url);
        jmxConnector = JMXConnectorFactory.connect(url, environment);
        connection = jmxConnector.getMBeanServerConnection();
        assertNotNull("MBeanServerConnection should not be null", connection);
    }

    @Test
    public void testSchemaMBean() throws Exception {
        String methodName = "testSchemaMBean";
        Log.entering(logClass, methodName);

        //Invoke schema generation
        ObjectName name = new ObjectName(ServerSchemaGenerator.OBJECT_NAME);
        String[] signature = new String[] { "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" };
        Object[] params = new String[] { null, null, null, null };
        @SuppressWarnings("unchecked")
        Map<String, Object> returnedMap = (Map<String, Object>) connection.invoke(name, "generateInstallSchema", params, signature);
        Log.info(logClass, methodName, returnedMap.toString());
        Log.info(logClass, methodName, "Return code from Returned Map should not be 0: " + returnedMap.get(FeatureListMBean.KEY_RETURN_CODE));

        //Check return code
        assertTrue((Integer) returnedMap.get(ServerSchemaGenerator.KEY_RETURN_CODE) == 0);

        //Download generated file
        String sourcePath = (String) returnedMap.get(ServerSchemaGenerator.KEY_FILE_PATH);
        String targetPath = outputDir + "/download_target/schema.xsd";
        Log.info(logClass, methodName, "sourcePath: " + sourcePath + "targetPath " + targetPath);

        signature = new String[] { "java.lang.String", "java.lang.String" };
        params = new Object[] { sourcePath, targetPath };
        ObjectName objName = new ObjectName(FileTransferMBean.OBJECT_NAME);
        connection.invoke(objName, "downloadFile", params, signature);

        //Ensure the file is bigger than 1 MB
        File file = new File(targetPath);
        Log.info(logClass, methodName, "Returned file (targetPath): " + file.length());
        assertTrue("Returned file is empty", file.length() > 1000000);
        Log.exiting(logClass, methodName);
    }

    private static int getSSLPort() {
        return Integer.valueOf(System.getProperty("HTTP_default.secure", "8020"));
    }

    private static String getProfile() {
        return System.getProperty("profile", "com.ibm.ws.config.bvt.schema");

    }

}
