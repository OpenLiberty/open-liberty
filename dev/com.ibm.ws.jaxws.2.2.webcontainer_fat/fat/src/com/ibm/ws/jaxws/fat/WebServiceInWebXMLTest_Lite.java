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

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@SkipForRepeat("jaxws-2.3")
public class WebServiceInWebXMLTest_Lite {

    public static final int CONN_TIMEOUT = 5;

    @Server("WebServiceInWebXMLTestServer")
    public static LibertyServer server;

    protected static WebArchive app = null;
    protected static String webXmlDir = "lib/LibertyFATTestFiles/WebServiceInWebXMLTest/";

    private final Class<?> c = WebServiceInWebXMLTest_Lite.class;

    @BeforeClass
    public static void setUp() throws Exception {
        app = ShrinkHelper.buildDefaultApp("converter", "com.ibm.samples.jaxws.converter",
                                           "com.ibm.samples.jaxws.converter.bindtype",
                                           "com.ibm.ws.liberty.test.wscontext");
    }

    /**
     * TestDescription: the test is simply testing the following condition and result.
     * condition:
     * - web service defined in web.xml as servlet
     * - a valid url-pattern is specified in web.xml
     * result:
     * - the web service can be accessed via the url-pattern specified in web.xml
     */
    @Test
    public void testConfigWebServiceInWebXml_SingleURL() throws Exception {
        setWebXml("singleURLPattern/web.xml");
        server.startServer();
        // Pause for application to start successfully
        server.waitForStringInLog("CWWKZ0001I.*converter");
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/converter/converter?wsdl");
        Log.info(c, "testConfigWebServiceInWebXml_SingleURL",
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
     * - multiple valid url-patterns are specified in web.xml
     * result:
     * - the web service can be accessed via the any of the valid url-pattern specified in web.xml
     */
    @Test
    public void testConfigWebServiceInWebXml_MultipleURL() throws Exception {
        setWebXml("multipleURLPattern/web.xml");
        server.startServer();
        // Pause for application to start successfully
        server.waitForStringInLog("CWWKZ0001I.*converter");
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/converter/converterAgain?wsdl");
        Log.info(c, "testConfigWebServiceInWebXml_MultipleURL",
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
     * result:
     * - the web service can be accessed via the serviceName attribute specified in WebService annotation
     */
    @Test
    public void testConfigWebServiceInWebXml_NoServletMapping() throws Exception {
        setWebXml("noServletMapping/web.xml");
        server.startServer();
        // Pause for application to start successfully
        server.waitForStringInLog("CWWKZ0001I.*converter");
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/converter/ConverterSvcName?wsdl");
        Log.info(c, "testConfigWebServiceInWebXml_NoServletMapping",
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
     * - a valid url-pattern is specified in web.xml
     * - metadata-complete is true in web.xml
     * result:
     * - the web service can be accessed via the url-pattern specified in web.xml
     */
    @Test
    public void testConfigWebServiceInWebXml_SingleURL_metadataComplete() throws Exception {
        setWebXml("singleURLPatternComplete/web.xml");
        server.startServer();
        // Pause for application to start successfully
        server.waitForStringInLog("CWWKZ0001I.*converter");
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/converter/converter?wsdl");
        Log.info(c, "testConfigWebServiceInWebXml_SingleURL_metadataComplete",
                 "Calling converter Application with URL=" + url.toString());
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        //If there is output then the Application automatically installed correctly
        assertNotNull("The output is null", line);
        assertTrue("The output did not contain the automatically installed message",
                   line.contains("xml version="));
    }

    @Test
    public void testSameWebServiceDiffBindingType_WSDL() throws Exception {
        String method = "testSameWebServiceDiffBindingType_WSDL";
        app.addAsWebInfResource(new File(webXmlDir + "webserviceBindType/converter-bindtype-default.wsdl"), "wsdl/converter-bindtype-default.wsdl");
        setWebXml("singleURLPattern/web.xml");
        server.startServer(method + ".log");

        //Pause for application to start successfully
        server.waitForStringInLog("CWWKZ0001I.*converter");

        //Web service with default binding
        checkWSDLGenerated(method, "/ConverterSvcName-bindtype-default?wsdl");
        //Web service with soap1.1 binding
        checkWSDLGenerated(method, "/ConverterSvcName-bindtype-soap11?wsdl");
        checkWSDLGenerated(method, "/ConverterSvcName-bindtype-soap11mtom?wsdl");
        //Web service with wsdl location
        checkWSDLGenerated(method, "/ConverterSvcName-bindtype-wsdllocation?wsdl");
        //Web service with http binding
        checkWSDLNotGenerated(method, "/ConverterSvcName-bindtype-http?wsdl");
        //Web service with xformat binding
        checkWSDLNotGenerated(method, "/ConverterSvcName-bindtype-xformat?wsdl");
        //Web service with soap1.2 binding
        checkWSDLNotGenerated(method, "/ConverterSvcName-bindtype-soap12?wsdl");
        checkWSDLNotGenerated(method, "/ConverterSvcName-bindtype-soap12mtom?wsdl");
    }

    private void checkWSDLNotGenerated(String method, String wsdlPath) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/converter" + wsdlPath);
        Log.info(this.c, method, "Calling test Application with URL=" + url.toString());
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_NOT_FOUND, 5);
        BufferedReader br = HttpUtils.getErrorStream(con);
        String line = br.readLine();
        Log.info(this.c, method, "LINE: " + line);
        assertTrue("The response was did not contain the expected servlet output",
                   line.contains("CWWKW0037E"));
    }

    private void checkWSDLGenerated(String method, String wsdlPath) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/converter" + wsdlPath);
        Log.info(this.c, method, "Calling test Application with URL=" + url.toString());
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, 5);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        Log.info(this.c, method, "LINE: " + line);
        assertTrue("The response was did not contain the expected servlet output",
                   line.contains("<wsdl:definitions"));
    }

    protected void setWebXml(String webxml) throws Exception {
        app.setWebXML(new File(webXmlDir + webxml));
        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.OVERWRITE);
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKW0035E");
        }
    }

}