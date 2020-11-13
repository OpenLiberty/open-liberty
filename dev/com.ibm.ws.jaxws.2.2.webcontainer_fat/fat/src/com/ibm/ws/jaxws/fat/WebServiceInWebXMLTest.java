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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;


public class WebServiceInWebXMLTest extends WebServiceInWebXMLTest_Lite {
    private final Class<?> c = WebServiceInWebXMLTest.class;

    @Server("WebServiceInWebXMLTestServerWithSharedLib")
    public static LibertyServer serverWithSharedLib;

    protected static JavaArchive lib = null;

    @BeforeClass
    public static void moreSetUp() throws Exception {
        lib = ShrinkHelper.buildJavaArchive("sharedLib", "com.ibm.ws.samples.jaxws.sharedlib");
    }

    /**
     * TestDescription: the test is simply testing the following condition and result.
     * condition:
     * - web service defined in web.xml as servlet
     * - multiple valid url-patterns are specified in web.xml
     * - metadata-complete is true in web.xml
     * result:
     * - the web service can be accessed via the any of the valid url-pattern specified in web.xml
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testConfigWebServiceInWebXml_MultipleURL_metadataComplete() throws Exception {
        setWebXml("multipleURLPatternComplete/web.xml");
        server.startServer();
        // Pause for application to start successfully
        server.waitForStringInLog("CWWKZ0001I.*converter");
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/converter/converterAgain?wsdl");
        Log.info(c, "testConfigWebServiceInWebXml_MultipleURL_metadataComplete",
                 "Calling converter Application with URL=" + url.toString());
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        //If there is output then the Application automatically installed correctly
        assertNotNull("The output is null", line);
        assertTrue("The output did not contain the automatically installed message",
                   line.contains("xml version="));
    }

    /**
     * TestDescription: the test is simply testing the following condition and result.
     * condition:
     * - web service defined in web.xml as servlet
     * - no valid url-pattern is specified in web.xml
     * - metadata-complete is true in web.xml
     * result:
     * - the web service can be accessed via the serviceName attribute specified in WebService annotation
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testConfigWebServiceInWebXml_NoServletMapping_metadataComplete() throws Exception {
        setWebXml("noServletMappingComplete/web.xml");
        server.startServer();
        // Pause for application to start successfully
        server.waitForStringInLog("CWWKZ0001I.*converter");
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/converter/ConverterSvcName?wsdl");
        Log.info(c, "testConfigWebServiceInWebXml_NoServletMapping_metadataComplete",
                 "Calling converter Application with URL=" + url.toString());
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        //If there is output then the Application automatically installed correctly
        assertNotNull("The output is null", line);
        assertTrue("The output did not contain the automatically installed message",
                   line.contains("xml version="));
    }

    /**
     * TestDescription: the test is simply testing the following condition and result.
     * condition:
     * - web service class locates in shared lib of web app
     * - web service defined in web.xml as servlet
     * - valid url-pattern is specified in web.xml
     * result:
     * - the web service can be accessed via the valid url-pattern specified in web.xml
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testConfigWebServiceInWebXml_SharedLib() throws Exception {
        ShrinkHelper.exportToServer(serverWithSharedLib, "lib/apple", lib, DeployOptions.OVERWRITE);
        setWebXml(serverWithSharedLib, "sharedLib/web.xml");
        serverWithSharedLib.startServer();
        // Pause for application to start successfully
        serverWithSharedLib.waitForStringInLog("CWWKZ0001I.*converter");
        URL url = new URL("http://" + server.getHostname() + ":" + serverWithSharedLib.getHttpDefaultPort() + "/converter/calculator?wsdl");
        Log.info(c, "testConfigWebServiceInWebXml_SharedLib",
                 "Calling converter Application with URL=" + url.toString());
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        //If there is output then the Application automatically installed correctly
        assertNotNull("The output is null", line);
        assertTrue("The output did not contain the automatically installed message",
                   line.contains("xml version="));
    }

    /**
     * TestDescription: the test is simply testing the following condition and result.
     * condition:
     * - web service class locates in shared lib of web app
     * - web service defined in web.xml as servlet
     * - valid url-pattern is specified in web.xml
     * - metadata-complete is true in web.xml
     * result:
     * - the web service can be accessed via the valid url-pattern specified in web.xml
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testConfigWebServiceInWebXml_SharedLibComplete() throws Exception {
        ShrinkHelper.exportToServer(serverWithSharedLib, "lib/apple", lib, DeployOptions.OVERWRITE);
        setWebXml(serverWithSharedLib, "sharedLibComplete/web.xml");
        serverWithSharedLib.startServer();
        // Pause for application to start successfully
        serverWithSharedLib.waitForStringInLog("CWWKZ0001I.*converter");
        URL url = new URL("http://" + server.getHostname() + ":" + serverWithSharedLib.getHttpDefaultPort() + "/converter/calculatorAgain?wsdl");
        Log.info(c, "testConfigWebServiceInWebXml_SharedLibComplet",
                 "Calling converter Application with URL=" + url.toString());
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        //If there is output then the Application automatically installed correctly
        assertNotNull("The output is null", line);
        assertTrue("The output did not contain the automatically installed message",
                   line.contains("xml version="));
    }

    protected void setWebXml(LibertyServer server, String webxml) throws Exception {
        app.setWebXML(new File(webXmlDir + webxml));
        ShrinkHelper.exportAppToServer(server, app, DeployOptions.OVERWRITE);
    }

    @After
    public void moreTearDown() throws Exception {
        if (serverWithSharedLib != null && serverWithSharedLib.isStarted()) {
            serverWithSharedLib.stopServer("CWWKW0035E");
        }
    }
}