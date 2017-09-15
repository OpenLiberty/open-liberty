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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.net.ssl.HttpsURLConnection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.config.mbeans.ServerSchemaGenerator;
import com.ibm.websphere.filetransfer.FileTransferMBean;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jmx.connector.client.rest.ClientProvider;
import componenttest.annotation.ExpectedFFDC;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 * FAT-Tests to test the functionality of com.ibm.websphere.config.mbeans.ServerSchemaGenerator MBean
 */
public class SchemaGeneratorMBeanTest {

    private static Class<?> logClass = SchemaGeneratorMBeanTest.class;

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.config.schemaGen.mbean");

    private static final String SUCCESS_MESSAGE = "Setup completed successfully.";
    private static final long MIN_FILE_SIZE = 500000;
    private static final long SERVER_FILE_SIZE = 5000;

    private static String outputDir;
    private static MBeanServerConnection connection;
    private static JMXConnector jmxConnector;

    private static ObjectName schemaGenObjName;
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

        // Set up the trust store
        System.setProperty("javax.net.ssl.trustStore", outputDir + "/resources/security/key.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "Liberty");

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

        schemaGenObjName = new ObjectName(ServerSchemaGenerator.OBJECT_NAME);
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
     * Invoke ServerSchemaGenerator.generateInstallSchema method
     * 
     * @param params
     * @return response from MBean invoke
     * @throws Exception
     */
    private Map<String, Object> invokeGenerateInstallSchema(Object[] params) throws Exception {
        Log.entering(logClass, "invokeGenerateInstallSchema", params);
        String[] signature = new String[] { "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" };

        @SuppressWarnings("unchecked")
        Map<String, Object> returnedMap = (Map<String, Object>) connection.invoke(schemaGenObjName, "generateInstallSchema", params, signature);

        Log.exiting(logClass, "invokeGenerateInstallSchema", returnedMap);
        return returnedMap;
    }

    /**
     * Invoke ServerSchemaGenerator.generateServerSchema method
     * 
     * @param params
     * @return response from MBean invoke
     * @throws Exception
     */
    private Map<String, Object> invokeGenerateServerSchema(Object[] params) throws Exception {
        Log.entering(logClass, "invokeGenerateServerSchema", params);
        String[] signature = new String[] { "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" };

        @SuppressWarnings("unchecked")
        Map<String, Object> returnedMap = (Map<String, Object>) connection.invoke(schemaGenObjName, "generateServerSchema", params, signature);

        Log.exiting(logClass, "invokeGenerateServerSchema", returnedMap);
        return returnedMap;
    }

    /**
     * Invoke ServerSchemaGenerator.generate method
     * 
     * @param params
     * @return response from MBean invoke
     * @throws Exception
     */
    private String invokeSchemaGenMBeanGenerate() throws Exception {

        Object[] params = new String[] {};
        String[] signature = new String[] {};

        String response = (String) connection.invoke(schemaGenObjName, "generate", params, signature);

        Log.finest(logClass, "invokeSchemaGenMBeanGenerate", "response=" + response);
        return response;
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

    @Test
    public void testDefaultValuesRestartServer() throws Exception {
        String methodName = "testDefaultValuesRestartServer";

        //Invoke schema generation
        Object[] params = new String[] { "1.0", "1", "UTF-8", Locale.getDefault().toString() }; //schemaVersion, outputVersion, encoding, locale
        Map<String, Object> returnedMap = invokeGenerateInstallSchema(params);

        //Check return code
        assertTrue((Integer) returnedMap.get(ServerSchemaGenerator.KEY_RETURN_CODE) == 0);

        //Download generated file
        String sourcePath = (String) returnedMap.get(ServerSchemaGenerator.KEY_FILE_PATH);
        String targetPath = outputDir + "/download_target/schemaDefaultValues.xsd";

        params = new Object[] { sourcePath, targetPath };
        invokeFileTransferMBeanDownloadFile(params);

        //Ensure the file is bigger than expected minimum size
        File file = new File(targetPath);
        assertTrue("targetPath file is not expected size. File=" + file.getAbsolutePath() + " : size=" + file.length(), file.length() > MIN_FILE_SIZE);

        Log.info(logClass, methodName, "Restarting server");
        server.restartServer();

        Log.info(logClass, methodName, "Waiting for 'CWWKT0016I.*IBMJMXConnectorREST'");
        assertNotNull("'CWWKT0016I.*IBMJMXConnectorREST' was not received on server",
                      server.waitForStringInLog("CWWKT0016I.*IBMJMXConnectorREST"));

        Log.info(logClass, methodName, "Waiting for 'CWWKO0219I.*ssl'");
        assertNotNull("'CWWKO0219I.*ssl' was not recieved on server",
                      server.waitForStringInLog("CWWKO0219I.*ssl"));

        // Verify that the file was cleaned up after server restart
        assertFalse("Generated schema file was not deleted after server restart. File=" + sourcePath, new File(sourcePath).exists());
    }

    @Test
    public void testEmptyStrings() throws Exception {
        //Invoke schema generation
        Object[] params = new String[] { "", "", "", "" }; //schemaVersion, outputVersion, encoding, locale
        Map<String, Object> returnedMap = invokeGenerateInstallSchema(params);

        //Check return code
        assertTrue((Integer) returnedMap.get(ServerSchemaGenerator.KEY_RETURN_CODE) == 0);

        //Ensure generated file is bigger than expected minimum size
        String filePath = (String) returnedMap.get(ServerSchemaGenerator.KEY_FILE_PATH);
        File file = new File(filePath);
        assertTrue("File is not expected size. File=" + file.getAbsolutePath() + " : size=" + file.length(), file.length() > MIN_FILE_SIZE);
    }

    @Test
    public void testGenerateDownloadAndDelete() throws Exception {
        Object[] params = new String[] { "1.1", "2", "UTF-16", "fr" }; //schemaVersion, outputVersion, encoding, locale
        Map<String, Object> returnedMap = invokeGenerateInstallSchema(params);

        //Check return code
        assertTrue((Integer) returnedMap.get(ServerSchemaGenerator.KEY_RETURN_CODE) == 0);

        //Download generated file
        String sourcePath = (String) returnedMap.get(ServerSchemaGenerator.KEY_FILE_PATH);
        String targetPath = outputDir + "/download_target/schemaNonDefault.xsd";

        params = new Object[] { sourcePath, targetPath };
        invokeFileTransferMBeanDownloadFile(params);

        //Ensure the file is bigger than expected minimum size
        File file = new File(targetPath);
        assertTrue("File is not expected size. File=" + file.getAbsolutePath() + " : size=" + file.length(), file.length() > MIN_FILE_SIZE);

        // Remotely delete the generated schema file
        RemoteFile remoteFile = server.getMachine().getFile(sourcePath);

        //first check if the file exists
        assertTrue("Generated schema file should exist at this point. remoteFile=" + remoteFile.getAbsolutePath(), remoteFile.exists());

        params = new Object[] { remoteFile.getAbsolutePath() };
        invokeFileTransferMBeanDeleteFile(params);

        //Verify the file is deleted
        assertFalse("Generated schema file should have been deleted. remoteFile=" + remoteFile.getAbsolutePath() + " : size" + remoteFile.length(), remoteFile.exists());
    }

    @Test
    public void testGenerateServerSchema() throws Exception {
        Object[] params = new String[] { null, null, null, null }; //schemaVersion, outputVersion, encoding, locale
        Map<String, Object> returnedMap = invokeGenerateServerSchema(params);

        //Check return code
        assertTrue((Integer) returnedMap.get(ServerSchemaGenerator.KEY_RETURN_CODE) == 0);
        //Download generated file
        String sourcePath = (String) returnedMap.get(ServerSchemaGenerator.KEY_FILE_PATH);
        String targetPath = outputDir + "/download_target/serverschema.xsd";

        params = new Object[] { sourcePath, targetPath };
        invokeFileTransferMBeanDownloadFile(params);

        //Ensure the file is bigger than expected minimum size
        File file = new File(targetPath);
        assertTrue("File is not expected size. File=" + file.getAbsolutePath() + " : size=" + file.length(), file.length() > SERVER_FILE_SIZE);

    }

    @ExpectedFFDC("java.lang.IllegalArgumentException")
    @Test
    public void testInvalidSchemaVersion() throws Exception {
        final String schemaVersion = "9876";
        Object[] params = new String[] { schemaVersion, null, null, null }; //schemaVersion, outputVersion, encoding, locale
        Map<String, Object> returnedMap = invokeGenerateInstallSchema(params);

        //Check return code
        assertTrue("Return code should not be 0", (Integer) returnedMap.get(ServerSchemaGenerator.KEY_RETURN_CODE) != 0);

        //Retrieve output
        String output = (String) returnedMap.get(ServerSchemaGenerator.KEY_OUTPUT);

        assertTrue("Output does not contain expected IllegalArgumentException. output=" + output, output.contains("java.lang.IllegalArgumentException: " + schemaVersion));
    }

    @ExpectedFFDC("java.lang.IllegalArgumentException")
    @Test
    public void testInvalidOutputVersion() throws Exception {
        final String outputVersion = "312";
        Object[] params = new String[] { null, outputVersion, null, null }; //schemaVersion, outputVersion, encoding, locale
        Map<String, Object> returnedMap = invokeGenerateInstallSchema(params);

        //Check return code
        assertTrue("Return code should not be 0", (Integer) returnedMap.get(ServerSchemaGenerator.KEY_RETURN_CODE) != 0);

        //Retrieve output
        String output = (String) returnedMap.get(ServerSchemaGenerator.KEY_OUTPUT);

        assertTrue("Output does not contain expected IllegalArgumentException. output=" + output, output.contains("java.lang.IllegalArgumentException: " + outputVersion));
    }

    @Test
    public void testInvalidEncoding() throws Exception {
        final String encodingValue = "encodingABC";
        Object[] params = new String[] { null, null, encodingValue, null }; //schemaVersion, outputVersion, encoding, locale
        Map<String, Object> returnedMap = invokeGenerateInstallSchema(params);

        //Check return code
        assertTrue("Return code should not be 0", (Integer) returnedMap.get(ServerSchemaGenerator.KEY_RETURN_CODE) != 0);

        //Retrieve output
        String output = (String) returnedMap.get(ServerSchemaGenerator.KEY_OUTPUT);

        assertTrue("Output does not contain expected UnsupportedEncodingException. output=" + output, output.contains("java.io.UnsupportedEncodingException: " + encodingValue));
    }

    /**
     * Schema Generator replaces invalid locale with the default locale. So an output should be generated.
     * 
     * @throws Exception
     */
    @Test
    public void testInvalidLocale() throws Exception {
        //Invoke schema generation
        final String localeValue = "localeXYZ";
        Object[] params = new String[] { null, null, null, localeValue }; //schemaVersion, outputVersion, encoding, locale
        Map<String, Object> returnedMap = invokeGenerateInstallSchema(params);

        //Check return code
        assertTrue((Integer) returnedMap.get(ServerSchemaGenerator.KEY_RETURN_CODE) == 0);

        //Ensure generated file is bigger than expected minimum size
        String filePath = (String) returnedMap.get(ServerSchemaGenerator.KEY_FILE_PATH);
        File file = new File(filePath);
        assertTrue("File is not expected size. File=" + file.getAbsolutePath() + " : size=" + file.length(), file.length() > MIN_FILE_SIZE);
    }

    @Test
    public void testSchemaGenMBeanCallAPI() throws Exception {
        ServerSchemaGenerator schemaGenMBean = JMX.newMBeanProxy(connection, schemaGenObjName, ServerSchemaGenerator.class);
        assertNotNull("We should have got access to the ServerSchemaGenerator API", schemaGenMBean);

        Map<String, Object> returnedMap = schemaGenMBean.generateInstallSchema(null, null, null, null); //schemaVersion, outputVersion, encoding, locale

        //Check return code
        assertTrue((Integer) returnedMap.get(ServerSchemaGenerator.KEY_RETURN_CODE) == 0);

        //Download generated file
        String sourcePath = (String) returnedMap.get(ServerSchemaGenerator.KEY_FILE_PATH);
        String targetPath = outputDir + "/download_target/schemaNonDefaultAPI.xsd";

        Object[] params = new Object[] { sourcePath, targetPath };
        invokeFileTransferMBeanDownloadFile(params);

        //Ensure the file is bigger than expected minimum size
        File file = new File(targetPath);
        assertTrue("File is not expected size. File=" + file.getAbsolutePath() + " : size=" + file.length(), file.length() > MIN_FILE_SIZE);
    }

    @Test
    public void testInvokeGenerateMethod() throws Exception {
        //Invoke schema generation using the ServerSchemaGenerator.generate method
        String returnedVal = invokeSchemaGenMBeanGenerate();
        assertTrue(returnedVal != null && returnedVal.length() > 0);
    }

    /**
     * Calls a servlet that uses the JMX REST client to connect to the server
     * and check a few MBeanServerConnection methods and MBean API calls.
     */
    @Test
    public void testSchemaGenAppClient() throws Exception {
        // Call AppClientServletSchemaGen, check for good result
        String encodedServerRoot = URLEncoder.encode(server.getServerRoot(), "UTF-8");
        URL url = new URL("https", server.getHostname(), server.getHttpDefaultSecurePort(), "/mbeans/appClient?serverRoot=" + encodedServerRoot);
        HttpsURLConnection con = (HttpsURLConnection) HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, 60);
        HttpUtils.findStringInHttpConnection(con, SUCCESS_MESSAGE);
    }

    private static int getSSLPort() {
        return Integer.valueOf(System.getProperty("HTTP_default.secure", "8020"));
    }

    /**
     * Schema Generator for server throws illegalargument exception for a invalid locale.
     * 
     * @throws Exception
     */
    @Test
    public void testServerSchemaInvalidLocale() throws Exception {
        //Invoke schema generation
        final String localeValue = "localeXYZ";
        Object[] params = new String[] { null, null, null, localeValue }; //schemaVersion, outputVersion, encoding, locale
        try {
            Map<String, Object> returnedMap = invokeGenerateServerSchema(params);
        } catch (Exception ie) {
            assertTrue(true);
        }

    }

    @Test
    @ExpectedFFDC("java.io.UnsupportedEncodingException")
    public void testServerSchemaInvalidEncoding() throws Exception {
        final String encodingValue = "encodingABC";
        Object[] params = new String[] { null, null, encodingValue, null }; //schemaVersion, outputVersion, encoding, locale

        Map<String, Object> returnedMap = invokeGenerateServerSchema(params);

        //Check return code
        assertTrue("Return code should not be 0", (Integer) returnedMap.get(ServerSchemaGenerator.KEY_RETURN_CODE) != 0);

        //Retrieve output
        String output = (String) returnedMap.get(ServerSchemaGenerator.KEY_OUTPUT);

        assertTrue("Output does not contain expected IOException. output=" + output, output.contains("java.io.IOException: " + encodingValue));
    }

}
