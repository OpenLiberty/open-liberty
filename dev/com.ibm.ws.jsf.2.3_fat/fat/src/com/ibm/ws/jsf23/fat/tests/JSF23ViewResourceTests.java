/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.tests;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf23.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * A test class to test JSF 2.3 Spec Issue 1435: https://github.com/javaee/javaserverfaces-spec/issues/1435
 *
 * In JSF 2.3 the following methods are available to get a Stream of View Resources:
 *
 * ResourceHandler -> java.util.stream.Stream<java.lang.String> getViewResources(FacesContext facesContext, java.lang.String path, int maxDepth, ResourceVisitOption... options)
 * ResourceHandler -> java.util.stream.Stream<java.lang.String> getViewResources(FacesContext facesContext, java.lang.String path, ResourceVisitOption... options)
 *
 * ViewHandler -> java.util.stream.Stream<java.lang.String> getViews(FacesContext facesContext, java.lang.String path, int maxDepth, ViewVisitOption... options)
 * ViewHandler -> java.util.stream.Stream<java.lang.String> getViews(FacesContext facesContext, java.lang.String path, ViewVisitOption... options)
 *
 * ViewDeclarationLanguage -> java.util.stream.Stream<java.lang.String> getViews(FacesContext facesContext, java.lang.String path, int maxDepth, ViewVisitOption... options)
 * ViewDeclarationLanguage -> java.util.stream.Stream<java.lang.String> getViews(FacesContext facesContext, java.lang.String path, ViewVisitOption... options)
 *
 * There are currently some behavior differences between MyFaces and Mojarra for ResourceVisitOption.TOP_LEVEL_VIEWS_ONLY.
 * The following specification issue was opened for clarification: https://github.com/javaee/javaserverfaces-spec/issues/1461
 *
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF23ViewResourceTests {

    protected static final Class<?> c = JSF23ViewResourceTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf23CDIServer")
    public static LibertyServer jsf23CDIServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsf23CDIServer, "JSF23ViewResource-Spec1435.war", "com.ibm.ws.jsf23.fat.spec1435");

        // Start the server and use the class name so we can find logs easily.
        // Many tests use the same server.
        jsf23CDIServer.startServer(JSF23ViewResourceTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf23CDIServer != null && jsf23CDIServer.isStarted()) {
            jsf23CDIServer.stopServer();
        }
    }

    /**
     * This test drives a request to an application that uses the new APIs and verifies the output.
     *
     * @throws Exception
     */
    @Test
    public void testSpec1435_getViews_getViewResources() throws Exception {
        try (WebClient webClient = new WebClient()) {
            webClient.getOptions().setThrowExceptionOnScriptError(false);

            String index1 = "/index.xhtml";
            String index2 = "/depth2/indexDepth2.xhtml";
            String index3 = "/depth2/depth3/indexDepth3.xhtml";
            String index4 = "/depth2/depth3/dept4/indexDepth4.xhtml";
            String webinf = "/WEB-INF/testindex.xhtml";
            String metainf = "/META-INF/testindex.xhtml";
            String template = "/templates/template.xhtml";

            // Construct the URL for the test
            String contextRoot = "JSF23ViewResource-Spec1435";
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            // Ensure that all of the ResourceHandler output is correct
            assertTrue(verifyOutput(page.getElementById("out1").asText(), index1, index2, template, webinf, metainf, index3, index4));
            assertTrue(verifyOutput(page.getElementById("out2").asText(), index1, index2, template, index3, index4));
            assertTrue(verifyOutput(page.getElementById("out3").asText(), index1));
            assertTrue(verifyOutput(page.getElementById("out4").asText(), index1, index2, template, webinf, metainf));
            assertTrue(verifyOutput(page.getElementById("out5").asText(), index1, index2, template, webinf, metainf, index3, index4));
            assertTrue(verifyOutput(page.getElementById("out6").asText(), index1, index2, template, webinf, metainf, index3, index4));
            assertTrue(verifyOutput(page.getElementById("out7").asText(), index1));

            // Ensure that all of the ViewHandler output is correct
            assertTrue(verifyOutput(page.getElementById("out8").asText(), index1, index2, index3, index4));
            assertTrue(verifyOutput(page.getElementById("out9").asText(), index1.substring(0, index1.indexOf(".")), index2.substring(0, index2.indexOf(".")),
                                    index3.substring(0, index3.indexOf(".")), index4.substring(0, index4.indexOf("."))));
            assertTrue(verifyOutput(page.getElementById("out10").asText(), index1));
            assertTrue(verifyOutput(page.getElementById("out11").asText(), index1, index2));
            assertTrue(verifyOutput(page.getElementById("out12").asText(), index1, index2, index3, index4));
            assertTrue(verifyOutput(page.getElementById("out13").asText(), index1, index2, index3, index4));
            assertTrue(verifyOutput(page.getElementById("out14").asText(), index1));

            // Ensure that all of the VDL output is correct
            assertTrue(verifyOutput(page.getElementById("out15").asText(), index1, index2, index3, index4));
            assertTrue(verifyOutput(page.getElementById("out16").asText(), index1.substring(0, index1.indexOf(".")), index2.substring(0, index2.indexOf(".")),
                                    index3.substring(0, index3.indexOf(".")), index4.substring(0, index4.indexOf("."))));
            assertTrue(verifyOutput(page.getElementById("out17").asText(), index1));
            assertTrue(verifyOutput(page.getElementById("out18").asText(), index1, index2));
            assertTrue(verifyOutput(page.getElementById("out19").asText(), index1, index2, index3, index4));
            assertTrue(verifyOutput(page.getElementById("out20").asText(), index1, index2, index3, index4));
            assertTrue(verifyOutput(page.getElementById("out21").asText(), index1));
        }
    }

    /*
     * If the output contains all of the expectedValues then true is returned, otherwsie
     * false is returned. In addition if there are any unexpected values false will be returned.
     */
    private boolean verifyOutput(String output, String... expectedValues) {
        boolean retVal = true;

        ArrayList<String> outputList = new ArrayList<String>(Arrays.asList(output.split(",")));

        for (String expectedValue : expectedValues) {
            // Return false if there is not a value that is expected.
            if (!output.contains(expectedValue)) {
                retVal = false;
                break;
            }

            // Remove the expectedValue from the outputList
            outputList.remove(expectedValue);
        }

        // Return false if there is additional output that is not expected.
        if (retVal && !outputList.isEmpty()) {
            retVal = false;
        }

        return retVal;
    }
}
