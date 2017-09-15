/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.config.mbeans.ServerSchemaGenerator;
import com.ibm.websphere.filetransfer.FileTransferMBean;
import com.ibm.ws.jmx.connector.client.rest.ClientProvider;

@SuppressWarnings("serial")
public class AppClientServletSchemaGen extends HttpServlet {

    private static MBeanServerConnection mbsc;
    private static final String SUCCESS_MESSAGE = "Setup completed successfully.";

    private static final long MIN_FILE_SIZE = 500000;

    /**
     * Uses the JMX REST connector client from within a user-application to test
     * the functionality of com.ibm.websphere.config.mbeans.ServerSchemaGenerator
     */
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        boolean successful = false;
        String failureMessage = null;

        try {

            String serverRoot = URLDecoder.decode(request.getParameter("serverRoot"), "UTF-8");
            System.out.println("Server root:" + serverRoot);
            System.setProperty("javax.net.ssl.trustStore", serverRoot + "/resources/security/key.jks");
            System.setProperty("javax.net.ssl.trustStorePassword", "Liberty");
            Map<String, Object> environment = new HashMap<String, Object>();
            environment.put("jmx.remote.protocol.provider.pkgs", "com.ibm.ws.jmx.connector.client");
            environment.put(JMXConnector.CREDENTIALS, new String[] { "theUser", "thePassword" });
            environment.put(ClientProvider.DISABLE_HOSTNAME_VERIFICATION, true);
            environment.put(ClientProvider.READ_TIMEOUT, 2 * 60 * 1000);
            JMXServiceURL url = new JMXServiceURL("REST", request.getServerName(), request.getServerPort(), "/IBMJMXConnectorREST");
            JMXConnector jmxConnector = JMXConnectorFactory.connect(url, environment);
            mbsc = jmxConnector.getMBeanServerConnection();

            //---------------------------------------=====
            // Invoke MBean using MBeanServerConnection
            //---------------------------------------=====

            //Invoke schema generation
            Object[] params = new String[] { "1.0", "1", "UTF-8", Locale.getDefault().toString() }; //schemaVersion, outputVersion, encoding, locale
            String[] signature = new String[] { "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" };

            final ObjectName SCHEMA_GEN_MBEAN = new ObjectName(ServerSchemaGenerator.OBJECT_NAME);

            @SuppressWarnings("unchecked")
            Map<String, Object> returnedMap = (Map<String, Object>) mbsc.invoke(SCHEMA_GEN_MBEAN, "generateInstallSchema", params, signature);

            //Check return code
            if ((Integer) returnedMap.get(ServerSchemaGenerator.KEY_RETURN_CODE) != 0) {
                throw new RuntimeException("Invoke MBean - Generate schema call did not return successful return_code");
            }

            //Ensure the file is bigger than expected minimum size
            String filePath = (String) returnedMap.get(ServerSchemaGenerator.KEY_FILE_PATH);
            File file = new File(filePath);
            if (file.length() < MIN_FILE_SIZE) {
                throw new RuntimeException("Invoke MBean - Generated schema file is less than expected size. File=" + file.getAbsolutePath() +
                                           " : size=" + file.length());
            }

            //---------------------------------------
            // Call operations using the API
            //---------------------------------------

            ServerSchemaGenerator schemaGenMBean = JMX.newMBeanProxy(mbsc, SCHEMA_GEN_MBEAN, ServerSchemaGenerator.class);
            returnedMap = schemaGenMBean.generateInstallSchema(null, null, null, null); //schemaVersion, outputVersion, encoding, locale

            if ((Integer) returnedMap.get(ServerSchemaGenerator.KEY_RETURN_CODE) != 0) {
                throw new RuntimeException("MBean via API - Generate schema call did not return successful return_code");
            }

            //Ensure the file is bigger than expected minimum size
            String sourcePath = (String) returnedMap.get(ServerSchemaGenerator.KEY_FILE_PATH);
            file = new File(sourcePath);

            if (file.length() < MIN_FILE_SIZE) {
                throw new RuntimeException("MBean via API - Generated schema file is less than expected size. File=" + file.getAbsolutePath() +
                                           " : size=" + file.length());
            }

            //Download generated file (typical user operation)
            String targetPath = serverRoot + "/download_target/schemaServletCallAPI.xsd";

            final ObjectName FILE_TRN_MBEAN = new ObjectName(FileTransferMBean.OBJECT_NAME);
            FileTransferMBean fileTrnMBean = JMX.newMBeanProxy(mbsc, FILE_TRN_MBEAN, FileTransferMBean.class);
            fileTrnMBean.downloadFile(sourcePath, targetPath);

            //Ensure downloaded file is bigger than expected minimum size
            file = new File(targetPath);

            if (file.length() < MIN_FILE_SIZE) {
                throw new RuntimeException("MBean via API - Downloaded schema file is less than expected size. File=" + file.getAbsolutePath() +
                                           " : size=" + file.length());
            }

            // Delete the source file under logs/state
            file = new File(sourcePath);

            // First check if the file exists
            if (!file.exists()) {
                throw new RuntimeException("MBean via API - Generated schema file does not exist (before deletion). File=" + file.getAbsolutePath());
            }

            fileTrnMBean.deleteFile(sourcePath);

            //Verify the file is deleted
            if (file.exists()) {
                throw new RuntimeException("MBean via API - Generated schema file was not deleted. File=" + file.getAbsolutePath() + " : size=" + file.length());
            }

            jmxConnector.close();

            successful = true;

        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            failureMessage = sw.toString();
        }

        PrintWriter writer = response.getWriter();
        writer.println(successful ? SUCCESS_MESSAGE : failureMessage);
        writer.flush();
        writer.close();
    }

}
