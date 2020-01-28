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
import java.util.regex.Pattern;

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
 * JSF 2.3 test cases for component search
 */
@RunWith(FATRunner.class)
public class JSF23ComponentSearchTests {

    protected static final Class<?> c = JSF23ComponentSearchTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf23CDIServer")
    public static LibertyServer jsf23CDIServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsf23CDIServer,
                                      "ComponentSearchExpression.war",
                                      "com.ibm.ws.jsf23.fat.searchexpression",
                                      "com.ibm.ws.jsf23.fat.searchexpression.beans");

        // Start the server and use the class name so we can find logs easily.
        // Many tests use the same server
        jsf23CDIServer.startServer(JSF23ComponentSearchTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf23CDIServer != null && jsf23CDIServer.isStarted()) {
            jsf23CDIServer.stopServer();
        }
    }

    /**
     * Test search expressions using IDs and keywords inside an html page.
     *
     * This test will make sure that search keywords work as expected.
     *
     * @throws Exception
     */
    @Test
    public void testSearchExpressions() throws Exception {
        String contextRoot = "ComponentSearchExpression";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "index.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            String resultingXmlPage = page.asXml();

            // test @namingcontainer
            Pattern pattern = Pattern.compile("<label for=\"form1\">\\s*NamingContainer\\s*<\\/label>");
            assertTrue("The expected string was not found.", pattern.matcher(resultingXmlPage).find());

            // test :form1:@parent
            pattern = Pattern.compile("<label for=\"body\">\\s*Parent of the form\\s*<\\/label>");
            assertTrue("The expected string was not found.", pattern.matcher(resultingXmlPage).find());

            // test @next:@next
            pattern = Pattern.compile("<label for=\"form1:inputFirstNameId\">\\s*Next Next\\s*<\\/label>");
            assertTrue("The expected string was not found.", pattern.matcher(resultingXmlPage).find());

            // test @next
            pattern = Pattern.compile("<label for=\"form1:inputFirstNameId\">\\s*First Name - Next\\s*<\\/label>");
            assertTrue("The expected string was not found.", pattern.matcher(resultingXmlPage).find());

            // verify the id of the input form
            pattern = Pattern.compile("<input id=\"form1:inputFirstNameId\" name=\"form1:inputFirstNameId\" type=\"text\" value=\"\"\\/>");
            assertTrue("The expected string was not found.", pattern.matcher(resultingXmlPage).find());

            // test @previous:@previous
            pattern = Pattern.compile("<label for=\"form1:inputFirstNameId\">\\s*Previous Previous\\s*<\\/label>");
            assertTrue("The expected string was not found.", pattern.matcher(resultingXmlPage).find());

            // test @parent
            pattern = Pattern.compile("<label for=\"body\">\\s*Parent\\s*<\\/label>");
            assertTrue("The expected string was not found.", pattern.matcher(resultingXmlPage).find());

            // test body:@child(1)
            pattern = Pattern.compile("<label for=\"form1\">\\s*Body Child\\(1\\)\\s*<\\/label>");
            assertTrue("The expected string was not found.", pattern.matcher(resultingXmlPage).find());

            // test @child(0) and @previous
            pattern = Pattern.compile("<label for=\"inputTextChild\">\\s*Child\\(0\\)\\s*<input id=\"inputTextChild\" name=\"inputTextChild\" "
                                      + "type=\"text\" value=\"Child\"\\/>\\s*<label for=\"inputTextChild\">\\s*Previous\\s*<\\/label>\\s*<\\/label>");
            assertTrue("The expected string was not found.", pattern.matcher(resultingXmlPage).find());

            // test @id(inputTextId)
            pattern = Pattern.compile("<label for=\"inputTextId\">\\s*id\\(inputTextId\\)\\s*<input id=\"inputTextId\" name=\"inputTextId\" "
                                      + "type=\"text\" value=\"InputTextId\"\\/>\\s*<\\/label>");
            assertTrue("The expected string was not found.", pattern.matcher(resultingXmlPage).find());

            // test @root
            pattern = Pattern.compile("<label for=\"j_id__v_0\">\\s*Root\\s*<\\/label>");
            assertTrue("The expected string was not found.", pattern.matcher(resultingXmlPage).find());

            // test @composite
            pattern = Pattern.compile("<label for=\"compositeId\">\\s*Composite\\s*<\\/label>");
            assertTrue("The expected string was not found.", pattern.matcher(resultingXmlPage).find());
        }
    }

    /**
     * Test programmatic API of component search.
     *
     * This test will make sure that API can be used to perform component search.
     *
     * @throws Exception
     */
    @Test
    public void testProgramaticComponentSearch() throws Exception {
        String contextRoot = "ComponentSearchExpression";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "index.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            String resultingPage = page.asText();

            assertTrue("The expected string was not found.",
                       resultingPage.contains("TEST resolveClientId with search expression 'form1:@parent' -> body"));
            assertTrue("The expected string was not found.",
                       resultingPage.contains("TEST resolveClientIds with search expression 'form1:inputFirstNameId' -> form1:inputFirstNameId"));
            assertTrue("The expected string was not found.",
                       resultingPage.contains("TEST resolveComponent with search expression 'form1:@parent' -> body"));
            assertTrue("The expected string was not found.",
                       resultingPage.contains("TEST resolveComponents with search expression 'form1:@parent form1:submitButton' -> body"));
            assertTrue("The expected string was not found.",
                       resultingPage.contains("TEST resolveComponents with search expression 'form1:@parent form1:submitButton' -> submitButton"));
            assertTrue("The expected string was not found.",
                       resultingPage.contains("TEST if expression 'form1:@parent' is valid -> true"));
            assertTrue("The expected string was not found.",
                       resultingPage.contains("TEST if expression 'form1:@parent' is passthrough -> false"));
        }
    }

    /**
     * Test the new JSF 2.3 faces-config.xml elements
     *
     * search-keyword-resolver
     * search-expression-context-factory
     * search-expression-handler
     *
     * Make sure that the custom classes can be used.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testNewJSF23FacesConfigElements() throws Exception {
        String contextRoot = "ComponentSearchExpression";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "newFacesConfigElements.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            String resultingPage = page.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("The CustomSearchKeywordResolver was not used.",
                       resultingPage.contains("CustomSearchKeywordResolver: isResolverForKeyword Invoked!"));
            assertTrue("The CustomSearchExpressionContextFactory was not used.",
                       resultingPage.contains("CustomSearchExpressionContextFactory: getSearchExpressionContext Invoked!"));
            assertTrue("The CustomSearchExpressionHandler was not used.",
                       resultingPage.contains("CustomSearchExpressionHandler: resolveClientId Invoked!"));
        }
    }
}
