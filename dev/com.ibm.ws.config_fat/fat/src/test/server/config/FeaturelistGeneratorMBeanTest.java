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
package test.server.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.management.JMX;
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.config.mbeans.FeatureListMBean;
import com.ibm.websphere.filetransfer.FileTransferMBean;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jmx.connector.client.rest.ClientProvider;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * FAT-Tests to test the functionality of com.ibm.websphere.config.mbeans.FeatureListMBean
 */
public class FeaturelistGeneratorMBeanTest {

    private static Class<?> logClass = FeaturelistGeneratorMBeanTest.class;

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.schemaGen.mbean");

    private static String outputDir;
    private static MBeanServerConnection connection;
    private static JMXConnector jmxConnector;

    private static ObjectName featurelistGenObjName;
    private static ObjectName fileTranObjectName;

    @BeforeClass
    public static void setUp() throws Exception {
        final String methodName = "setUp";
        Log.entering(logClass, methodName);

        outputDir = server.getServerRoot();
        Log.info(logClass, methodName, "serverRoot=" + outputDir);

        Log.info(logClass, methodName, "Starting server=" + server.getServerName());
        server.startServer();

        Log.info(logClass, methodName, "Waiting for 'CWWKT0016I.*IBMJMXConnectorREST'");
        assertNotNull("'CWWKT0016I.*IBMJMXConnectorREST' was not received on server",
                      server.waitForStringInLog("CWWKT0016I.*IBMJMXConnectorREST"));

        Log.info(logClass, methodName, "Waiting for 'CWWKO0219I.*ssl'");
        assertNotNull("'CWWKO0219I.*ssl' was not recieved on server",
                      server.waitForStringInLog("CWWKO0219I.*ssl"));

        Log.info(logClass, methodName, "Waiting for 'CWPKI0803A.*ssl'");
        assertNotNull("'CWPKI0803A.*ssl' was not generated on server",
                      server.waitForStringInLog("CWPKI0803A"));

        Log.info(logClass, methodName, "Waiting for 'CWWKS0008I: The security service is ready'");
        assertNotNull("'CWWKS0008I: The security service is ready' was not generated on server",
                      server.waitForStringInLog("CWWKS0008I"));

        Log.info(logClass, methodName, "Waiting for 'CWWKS4105I: LTPA configuration is ready'");
        assertNotNull("'CWWKS4105I: LTPA configuration is ready' was not generated on server",
                      server.waitForStringInLog("CWWKS4105I"));

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
        Log.info(logClass, methodName, "JMXServiceURL=" + url.toString());
        jmxConnector = JMXConnectorFactory.connect(url, environment);
        assertNotNull("JMXConnector should not be null", jmxConnector);
        connection = jmxConnector.getMBeanServerConnection();
        assertNotNull("MBeanServerConnection should not be null", connection);

        featurelistGenObjName = new ObjectName(FeatureListMBean.OBJECT_NAME);
        fileTranObjectName = new ObjectName(FileTransferMBean.OBJECT_NAME);

        Log.exiting(logClass, "setUp");
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        String methodName = "tearDown";
        Log.entering(logClass, methodName);

        if (server != null && server.isStarted()) {
            Log.finer(logClass, methodName, "Server is up, stopping it");
            jmxConnector.close();
            server.stopServer();
        }
        Log.exiting(logClass, methodName);
    }

    /**
     * Invoke FeaturelistMBean.generate method
     * 
     * @param params
     * @return response from MBean invoke
     * @throws Exception
     */
    private Map<String, Object> invokeFeatureListMBeanGenerate(Object[] params) throws Exception {
        Log.entering(logClass, "invokeFeatureListMBeanGenerate", params);
        String[] signature = new String[] { "java.lang.String", "java.lang.String", "java.lang.String" };

        @SuppressWarnings("unchecked")
        Map<String, Object> returnedMap = (Map<String, Object>) connection.invoke(featurelistGenObjName, "generate", params, signature);
        Log.info(logClass, "invokeFeatureListMBeanGenerate", returnedMap.toString());
        Log.exiting(logClass, "invokeFeatureListMBeanGenerate", returnedMap);
        return returnedMap;
    }

    /**
     * Invoke FileTransferMBean.downloadFile method
     * 
     * @param params
     * @throws Exception
     */
    private void invokeFileTransferMBeanDownloadFile(Object[] params) throws Exception {
        Log.entering(logClass, "invokeFileTransferMBeanDownloadFile", params);
        String[] signature = new String[] { "java.lang.String", "java.lang.String" };
        connection.invoke(fileTranObjectName, "downloadFile", params, signature);
        Log.exiting(logClass, "invokeFileTransferMBeanDownloadFile");
    }

    /**
     * Invoke FileTransferMBean.deleteFile method
     * 
     * @param params
     * @throws Exception
     */
    private void invokeFileTransferMBeanDeleteFile(Object[] params) throws Exception {
        Log.entering(logClass, "invokeFileTransferMBeanDeleteFile", params);
        String[] signature = new String[] { "java.lang.String" };
        connection.invoke(fileTranObjectName, "deleteFile", params, signature);
        Log.exiting(logClass, "invokeFileTransferMBeanDeleteFile");
    }

    /**
     * Invoke FeaturelistMBean.generate method
     * 
     * @param encoding, locale, productExtension is empty
     * @throws Exception
     */
    @Test
    public void testEmptyStrings() throws Exception {
        //Invoke featureList generation
        Object[] params = new String[] { "", "", "" }; //encoding, locale, productExtension
        Map<String, Object> returnedMap = invokeFeatureListMBeanGenerate(params);

        //Check return code
        assertTrue((Integer) returnedMap.get(FeatureListMBean.KEY_RETURN_CODE) == 0);

        //Ensure generated file is bigger than 0.1 MB
        String filePath = returnedMap.get(FeatureListMBean.KEY_FILE_PATH).toString();
        File file = new File(filePath);
        assertTrue("File is not expected size. File=" + file.getAbsolutePath(), file.length() > 100000);
    }

    /**
     * Invoke FeaturelistMBean.generate method
     * 
     * @param encoding, locale, productExtension is empty
     * @throws Exception
     */
    @Test
    public void testInvalidEncoding() throws Exception {
        final String encodingValue = "en-Test";

        Object[] params = new String[] { encodingValue, null, null }; //encoding, locale, productExtension
        Map<String, Object> returnedMap = invokeFeatureListMBeanGenerate(params);

        //Check return code
        assertTrue("Return Code should be 21 and not 0", (Integer) returnedMap.get(FeatureListMBean.KEY_RETURN_CODE) != 0);

        //Retrieve output
        String output = (String) returnedMap.get(FeatureListMBean.KEY_OUTPUT);
        assertTrue("Output does not contain expected UnsupportedEncodingException. output=" + output, output.contains("java.io.UnsupportedEncodingException: " + encodingValue));
    }

    /**
     * Invoke FeaturelistMBean.generate method
     * 
     * @param encoding, locale is invalid, productExtension
     * @throws Exception
     */
    @Test
    public void testInvalidLocale() throws Exception {
        final String localeValue = "localeTest";

        Object[] params = new String[] { null, localeValue, null }; //encoding, locale, productExtension
        Map<String, Object> returnedMap = invokeFeatureListMBeanGenerate(params);

        //Check return code
        assertTrue("Return Code should be 0", (Integer) returnedMap.get(FeatureListMBean.KEY_RETURN_CODE) == 0);

        //Ensure generated file is bigger than 0.1 MB
        String filePath = returnedMap.get(FeatureListMBean.KEY_FILE_PATH).toString();
        File file = new File(filePath);
        assertTrue("File is not expected size. File=" + file.getAbsolutePath(), file.length() > 100000);
    }

    /**
     * Invoke FeaturelistMBean.generate method
     * 
     * @param encoding, locale, productExtension is invalid
     * @throws Exception
     */
    @Test
    public void testInvalidProductExt() throws Exception {
        final String productExtValue = "productExt";

        Object[] params = new String[] { null, null, productExtValue }; //encoding, locale, productExtension
        Map<String, Object> returnedMap = invokeFeatureListMBeanGenerate(params);

        //Check return code
        assertTrue("Return Code should be 21 and not 0", (Integer) returnedMap.get(FeatureListMBean.KEY_RETURN_CODE) == 21);

        //Retrieve output
        String output = (String) returnedMap.get(FeatureListMBean.KEY_OUTPUT);
        assertTrue("Output does not contain expected CWWKG0080E. output=" + output,
                   output.contains("CWWKG0080E"));
    }

    /**
     * Invoke FeaturelistMBean.generate method and using FileTransferMBean remotely to download
     * feature list file and deletes it
     * 
     * @param encoding, locale, productExtension is empty
     * @throws Exception
     */
    @Test
    public void testGenerateDownloadAndDelete() throws Exception {
        Object[] params = new String[] { "UTF-16", "fr", "" }; //encoding, locale, productExtension
        Map<String, Object> returnedMap = invokeFeatureListMBeanGenerate(params);

        //Check return code
        assertTrue((Integer) returnedMap.get(FeatureListMBean.KEY_RETURN_CODE) == 0);

        //Download generated file
        String sourcePath = returnedMap.get(FeatureListMBean.KEY_FILE_PATH).toString();
        String targetPath = outputDir + "/download_target/featurelistNonDefault.xml";

        params = new Object[] { sourcePath, targetPath };
        invokeFileTransferMBeanDownloadFile(params);

        //Ensure the file is bigger than 0.1 MB
        File file = new File(targetPath);
        assertTrue("File is not expected size. File=" + file.getAbsolutePath(), file.length() > 100000);

        // Remotely delete the generated featurelist file
        RemoteFile remoteFile = server.getMachine().getFile(sourcePath);

        //first check if the file exists
        assertTrue("Generated featurelist file should exist at this point. remoteFile=" + remoteFile.getAbsolutePath(), remoteFile.exists());

        params = new Object[] { remoteFile.getAbsolutePath() };
        invokeFileTransferMBeanDeleteFile(params);

        //Verify the file is deleted
        assertFalse("Generated featurelist file should have been deleted. remoteFile=" + remoteFile.getAbsolutePath(), remoteFile.exists());
    }

    /**
     * Invoke FeaturelistMBean.generate method with correct parameters
     * 
     * @param encoding, locale, productExtension
     * @throws Exception
     */
    @Test
    public void testGenerateSuccessfulFeaturelist() throws Exception {
        Object[] params = new String[] { "UTF-8", Locale.getDefault().toString(), null }; //encoding, locale, productExtension
        Map<String, Object> returnedMap = invokeFeatureListMBeanGenerate(params);

        //Check return code
        assertTrue((Integer) returnedMap.get(FeatureListMBean.KEY_RETURN_CODE) == 0);

        //Ensure generated file is bigger than 0.1 MB
        String filePath = returnedMap.get(FeatureListMBean.KEY_FILE_PATH).toString();
        File file = new File(filePath);
        assertTrue("File is not expected size. File=" + file.getAbsolutePath(), file.length() > 100000);

    }

    /**
     * Invoke FeaturelistMBean.generate method using the JMX MBean API
     * downloads the feature list and transfers it over using FileTransfer MBean
     * 
     * @param encoding, locale, productExtension
     * @throws Exception
     */
    @Test
    public void testFeaturelistGenMBeanCallAPI() throws Exception {
        FeatureListMBean featurelistGenMbean = JMX.newMBeanProxy(connection, featurelistGenObjName, FeatureListMBean.class);
        assertNotNull("We should have got access to the FeatureListMBean API", featurelistGenMbean);

        Map<String, Object> returnedMap = featurelistGenMbean.generate(null, null, null); //encoding, locale, productExtension

        //Check return code
        assertTrue((Integer) returnedMap.get(FeatureListMBean.KEY_RETURN_CODE) == 0);

        //Download generated file
        String sourcePath = returnedMap.get(FeatureListMBean.KEY_FILE_PATH).toString();
        String targetPath = outputDir + "/download_target/featurelistNonDefault.xml";

        Object[] params = new Object[] { sourcePath, targetPath };
        invokeFileTransferMBeanDownloadFile(params);

        //Ensure the file is bigger than 0.1 MB
        File file = new File(targetPath);
        assertTrue("File is not expected size. File=" + file.getAbsolutePath(), file.length() > 100000);
    }

    private static int getSSLPort() {
        return Integer.valueOf(System.getProperty("HTTP_default.secure", "8020"));
    }

}
