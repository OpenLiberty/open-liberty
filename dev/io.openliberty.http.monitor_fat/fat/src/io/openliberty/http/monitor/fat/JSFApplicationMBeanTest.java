/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor.fat;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import jakarta.ws.rs.HttpMethod;

@RunWith(FATRunner.class)
public class JSFApplicationMBeanTest extends BaseTestClass {

    private static Class<?> c = JSFApplicationMBeanTest.class;

    public static final String SERVER_NAME = "MBeanServer";

    @ClassRule
    public static RepeatTests rt = FATSuite.testRepeatMBeanTests(SERVER_NAME);

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        trustAll();
        WebArchive testWAR = ShrinkWrap
                        .create(WebArchive.class, "jsfApp.war")
                        .addAsWebInfResource(new File("test-applications/JsfApp/resources/WEB-INF/web.xml"))
                        .addAsWebInfResource(new File("test-applications/JsfApp/resources/WEB-INF/faces-config.xml"))
                        .addAsWebInfResource(new File("test-applications/JsfApp/resources/WEB-INF/beans.xml"))
                        .add(new FileAsset(new File("test-applications/JsfApp/resources/index.xhtml")), "/index.xhtml")
                        .add(new FileAsset(new File("test-applications/JsfApp/resources/normal.html")), "/normal.html")
                        .add(new FileAsset(new File("test-applications/JsfApp/resources/resources/css/style.css")), "/resources/css/style.css")
                        .add(new FileAsset(new File("test-applications/JsfApp/resources/resources/js/script.js")), "/resources/js/script.js")
                        .addPackage(
                                    "io.openliberty.http.monitor.fat.jsfApp");

        WebArchive mbeanWar = ShrinkWrap
                        .create(WebArchive.class, "MBeanGetter.war")
                        .addPackage(
                                    "io.openliberty.http.monitor.fat.mbeanGetter");

        ShrinkHelper.exportDropinAppToServer(server, mbeanWar,
                                             DeployOptions.SERVER_ONLY);

        ShrinkHelper.exportDropinAppToServer(server, testWAR,
                                             DeployOptions.SERVER_ONLY);

        server.startServer();

        //Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");
        server.setMarkToEndOfLog();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        //catch if a server is still running.
        if (server != null && server.isStarted()) {
            server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWPMI2006W", "CWMMC0013E", "CWWKG0033W", "SRVE0315E");
        }
    }

    private void loadJSFPage(LibertyServer server, String route) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
        try (WebClient webClient = new WebClient()) {

            String url = "http://" + server.getHostname() + ":"
                         + server.getHttpDefaultPort() + route;
            HtmlPage page = (HtmlPage) webClient.getPage(url);

        }
    }

    @Test
    public void testXHTML_mbean() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.JSF_CONTEXT_ROOT + "/index.xhtml";
        String expectedRoute = Constants.JSF_CONTEXT_ROOT + "/\\*.xhtml";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        loadJSFPage(server, route);

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        checkMBeanRegistered(server, objectName);

    }

    @Test
    public void testJSF_mbean() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.JSF_CONTEXT_ROOT + "/index.jsf";
        String expectedRoute = Constants.JSF_CONTEXT_ROOT + "/\\*.jsf";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        loadJSFPage(server, route);

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        checkMBeanRegistered(server, objectName);

    }

    @Test
    public void testFaces_mbean() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.JSF_CONTEXT_ROOT + "/index.faces";
        String expectedRoute = Constants.JSF_CONTEXT_ROOT + "/\\*.faces";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        loadJSFPage(server, route);

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        checkMBeanRegistered(server, objectName);

    }

    @Test
    public void testFacesNode_mbean() throws Exception {

        assertTrue(server.isStarted());

        String route = Constants.JSF_CONTEXT_ROOT + "/faces/index.xhtml";
        String expectedRoute = Constants.JSF_CONTEXT_ROOT + "/faces/\\*";
        String requestMethod = HttpMethod.GET;
        String responseStatus = "200";

        loadJSFPage(server, route);

        String objectName = "WebSphere:type=HttpServerStats,name=\"method:" + requestMethod + ";status:" + responseStatus + ";httpRoute:" + expectedRoute + "\"";
        checkMBeanRegistered(server, objectName);

    }

}
