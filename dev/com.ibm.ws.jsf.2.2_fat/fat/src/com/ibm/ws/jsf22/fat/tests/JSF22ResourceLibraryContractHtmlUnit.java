/*
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.List;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 * Tests to execute on the jsfTestServer1 that use HtmlUnit.
 */
@RunWith(FATRunner.class)
public class JSF22ResourceLibraryContractHtmlUnit {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "TestResourceContractsFromJar";

    protected static final Class<?> c = JSF22ResourceLibraryContractHtmlUnit.class;

    @Server("jsfTestServer1")
    public static LibertyServer jsfTestServer1;

    @BeforeClass
    public static void setup() throws Exception {

        JavaArchive ContractsJar = ShrinkHelper.buildJavaArchive("Contracts.jar", "");
        ShrinkHelper.addDirectory(ContractsJar, "test-applications" + "/Contracts.jar");

        WebArchive TestResourceContractsFromJarWar = ShrinkHelper.buildDefaultApp("TestResourceContractsFromJar.war", "com.ibm.ws.jsf22.fat.contractsfromjar.beans");
        TestResourceContractsFromJarWar.addAsLibraries(ContractsJar);

        WebArchive TestResourceContractsWar = ShrinkHelper.buildDefaultApp("TestResourceContracts.war");
        WebArchive TestResourceContractsDirectoryWar = ShrinkHelper.buildDefaultApp("TestResourceContractsDirectory.war");

        ShrinkHelper.exportDropinAppToServer(jsfTestServer1, TestResourceContractsWar);
        ShrinkHelper.exportDropinAppToServer(jsfTestServer1, TestResourceContractsDirectoryWar);
        ShrinkHelper.exportDropinAppToServer(jsfTestServer1, TestResourceContractsFromJarWar);

        jsfTestServer1.startServer(JSF22ResourceLibraryContractHtmlUnit.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfTestServer1 != null && jsfTestServer1.isStarted()) {
            /*
             * Avoiding the two errors below during the checkLogsForErrorsAndWarnings step.
             * [WARNING ] SESN0066E: The response is already committed to the client. The session cookie cannot be set.
             * [WARNING ] SRVE8114W: WARNING Cannot set session cookie. Response already committed.
             */
            jsfTestServer1.stopServer("SESN0066E", "SRVE8114W");
        }
    }

    /*
     * List of testcases
     *
     * Contracts defined in the application: team, manager, testContract
     *
     * Via URL-pattern
     * -- 1. Check for the contract "team" when the url-pattern is maps to '/user/*'
     * -- 2. Check for the contract "manager" when the url-pattern is maps to '/management/*'
     * -- 3. Check for the contract "team" when multiple url-pattern mapped to same contract
     * -- 4. Check for the contract "manager" when faces-config maps url to "teamContract" but f:view maps to "manager"
     * -- 5. Check for the contract "testContract" when the url-pattern is maps to '*'
     * -- 6. Check for the contract "testContract" when the url-pattern is maps to exact mapping to file
     * -- 7. Check for the exception for contract "testNewContract" which is not available to the application when the url-pattern is maps to exact mapping to file
     * -- 8. Check when multiple contracts mapped to same url-pattern
     *
     * 2. Set the parameter to make sure use non default contracts directory location. Run all the 8 tests again defined above.
     *
     * 3. Include all the contracts in jar file and include in WEB-INF/lib.
     * Try combination of dynamic using beans or f: view direct and try few tests defined above.
     */

    /**
     * This test will find the contract when url pattern '/user/*' mapped to 'team' contract
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void Test1_Contract_viaURL_MapDirectory() throws Exception {
        assertTrue(getPageForURL("TestResourceContracts", "faces/user/index.xhtml").asText().contains("This must be template for team"));

        // the following messages should not appear in logs when jsf2.2 feature is enabled on server

        // Test to make sure  we do not have this exception in logs
        // com.ibm.wsspi.adaptable.module.UnableToAdaptException: com.ibm.ws.javaee.ddmodel.DDParser$ParseException:
        //CWWKC2262E: The server is unable to process the 2.2 version and the http://xmlns.jcp.org/xml/ns/javaee namespace in the /WEB-INF/faces-config.xml deployment descriptor on line 5.
        String msg = "unable to process the 2.2 version and the http://xmlns.jcp.org/xml/ns/javaee namespace";
        // Check the trace.log
        // There should not be a match so fail if there is.
        assertTrue(msg, jsfTestServer1.findStringsInLogs(msg).isEmpty());
        Log.info(c, name.getMethodName(), "Test1_Contract_viaURL_MapDirectory :: msg not found in log, test passed ");

        // test to make sure we do not have this exception in logs
        // com.ibm.wsspi.adaptable.module.UnableToAdaptException: com.ibm.ws.javaee.ddmodel.DDParser$ParseException:
        //CWWKC2259E: Unexpected child element resource-library-contracts of parent element application encountered in the /WEB-INF/faces-config.xml deployment descriptor on line 9.

        String msg2 = "Unexpected child element resource-library-contracts of parent element application encountered";
        // Check the trace.log
        // There should not be a match so fail if there is.
        assertTrue(msg2, jsfTestServer1.findStringsInLogs(msg2).isEmpty());
        Log.info(c, name.getMethodName(), "Test1_Contract_viaURL_MapDirectory :: msg2 not found in log , test passed ");
    }

    /**
     * This test will find the contract when url pattern '/management/*' mapped to 'manager' contract
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void Test2_Contract_viaURL_MapDirectory() throws Exception {
        assertTrue(getPageForURL("TestResourceContracts", "faces/management/index.xhtml").asText().contains("This must be template for manager"));
    }

    /**
     * This test will find the contract when url pattern mapped to '/developers/*'
     * Multiple url-pattern mapped to same contract.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void Test3_Contract_viaMultipleURL_MapSpecific() throws Exception {
        assertTrue(getPageForURL("TestResourceContracts", "faces/developers/index.xhtml").asText().contains("This must be template for team"));
    }

    /**
     * This test will find the contract when url pattern mapped to specific '/developers/index2.xhtml'
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void Test4_Contract_viaURL_MapSpecific() throws Exception {
        assertTrue(getPageForURL("TestResourceContracts", "faces/developers/index2.xhtml").asText().contains("This must be template for test1"));
    }

    /**
     * This test will throw exception and message in logs when url pattern mapped to specific '/developers/index1.xhtml'
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void Test5_Contract_viaURL_UnavailableContract() throws Exception {

        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, "TestResourceContracts", "faces/developers/index1.xhtml");
            HtmlPage page = null;

            try {
                page = (HtmlPage) webClient.getPage(url);

            } catch (FailingHttpStatusCodeException fsc) {
                Log.info(c, name.getMethodName(), "Test5_Contract_viaURL_UnavailableContract :: , statusCode -->" + fsc.getStatusCode());
                assertTrue(fsc.getStatusCode() == 500);

            }

            String msg = "Resource Library Contract testNewContract was not found while scanning for available contracts";
            // Check the trace.log
            // There should be a match so fail if there is not.
            assertFalse(msg, jsfTestServer1.findStringsInLogs(msg).isEmpty());

            Log.info(c, name.getMethodName(), "Test5_Contract_viaURL_UnavailableContract :: Found expected msg in log -->" + msg);
        }
    }

    /**
     * This test will find the contract when url pattern mapped to '/others/*' .
     * Here f:view contract will take precedence over faces-config contract
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void Test6_Contract_viaURL_withfView() throws Exception {
        assertTrue(getPageForURL("TestResourceContracts", "faces/others/index.xhtml").asText().contains("This must be template for manager"));
    }

    /**
     * This test will find the contract when url pattern mapped to '*' .
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void Test7_Contract_viaURL_StarMapping() throws Exception {
        assertTrue(getPageForURL("TestResourceContracts", "faces/forall/index.xhtml").asText().contains("This must be template for test1"));
    }

    /**
     * This test will find the contract when multiple contracts mapped to same uri .
     * http://localhost:9080/TestResourceContracts/faces/user/index1.xhtml
     * index1.html needs template1.xhtml which is part of testContract
     *
     * Enable this testcase when fixed by 166703 , and remove the comments
     *
     * @throws Exception
     */
    @Test
    public void Test8_Contract_viaURL_MultipleContract() throws Exception {
        assertTrue(getPageForURL("TestResourceContracts", "faces/user/index1.xhtml").asText().contains("This must be template for test1"));
    }

    /**
     * This test will find the contract from the directory defined in the parameter in web.xml when url pattern '/user/*' mapped to 'team' contract
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void Test9_MyContract_viaURL_MapDirectory() throws Exception {
        assertTrue(getPageForURL("TestResourceContractsDirectory", "faces/user/index.xhtml").asText().contains("This must be template for team in MyContracts"));
    }

    /**
     * This test will find the contract from the directory defined in the parameter in web.xml when url pattern '/management/*' mapped to 'manager' contract
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void Test10_MyContract_viaURL_MapDirectory() throws Exception {
        assertTrue(getPageForURL("TestResourceContractsDirectory", "faces/management/index.xhtml").asText().contains("This must be template for manager in MyContracts"));
    }

    /**
     * This test will find the contract from the directory defined in the parameter in web.xml when url pattern mapped to '/developers/*'
     * Multiple url-pattern mapped to same contract.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void Test11_MyContract_viaMultipleURL_MapSpecific() throws Exception {
        assertTrue(getPageForURL("TestResourceContractsDirectory", "faces/developers/index.xhtml").asText().contains("This must be template for team in MyContracts"));
    }

    /**
     * This test will find the contract from the directory defined in the parameter in web.xml when url pattern mapped to specific '/developers/index2.xhtml'
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void Test12_Contract_viaURL_MapSpecific() throws Exception {
        assertTrue(getPageForURL("TestResourceContractsDirectory", "faces/developers/index2.xhtml").asText().contains("This must be template for test1 in MyContracts"));
    }

    /**
     * This test will throw exception and message in logs when url pattern mapped to specific '/developers/index1.xhtml'
     * Contract from the directory defined in the parameter in web.xml
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void Test13_MyContract_viaURL_UnavailableContract() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, "TestResourceContractsDirectory", "faces/developers/index1.xhtml");
            HtmlPage Page;

            try {
                HtmlPage page = (HtmlPage) webClient.getPage(url);

            } catch (FailingHttpStatusCodeException fsc) {
                Log.info(c, name.getMethodName(), "Test13_MyContract_viaURL_UnavailableContract :: , statusCode -->" + fsc.getStatusCode());
                assertTrue(fsc.getStatusCode() == 500);

            }

            String msg = "Resource Library Contract testNewContract was not found while scanning for available contracts";
            // Check the trace.log
            // There should be a match so fail if there is not.
            assertFalse(msg, jsfTestServer1.findStringsInLogs(msg).isEmpty());

            Log.info(c, name.getMethodName(), "Test13_MyContract_viaURL_UnavailableContract :: Found expected msg in log -->" + msg);
        }
    }

    /**
     * This test will find the contract from the directory defined in the parameter in web.xml when url pattern mapped to '/others/*' .
     * Here f:view contract will take precedence over faces-config contract
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void Test14_MyContract_viaURL_withfView() throws Exception {
        assertTrue(getPageForURL("TestResourceContractsDirectory", "faces/others/index.xhtml").asText().contains("This must be template for manager in MyContracts"));
    }

    /**
     * This test will find the contract from the directory defined in the parameter in web.xml when url pattern mapped to '*' .
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void Test15_MyContract_viaURL_StarMapping() throws Exception {
        assertTrue(getPageForURL("TestResourceContractsDirectory", "faces/forall/index.xhtml").asText().contains("This must be template for test1 in MyContracts"));
    }

    /**
     * This test will find the contract from the directory defined in the parameter in web.xml when multiple contracts mapped to same uri .
     * http://localhost:9080/TestResourceContracts/faces/user/index1.xhtml
     * index1.html needs template1.xhtml which is part of testContract
     *
     * Enable this testcase when fixed by 166703 , and remove the comments
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void Test16_MyContract_viaURL_MultipleContract() throws Exception {
        assertTrue(getPageForURL("TestResourceContractsDirectory", "faces/user/index1.xhtml").asText().contains("This must be template for test1 in MyContracts"));
    }

    /**
     * This test will find the contract , Choose manager contract.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void Test17_Contract_viaJar_MapDirectory() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, "TestResourceContractsFromJar", "faces/user/index.xhtml");
            HtmlPage htmlPage = (HtmlPage) webClient.getPage(url);

            assertTrue(htmlPage.asText().contains("This must be template for team"));

            //Log.info(c, name.getMethodName(), "JSF22ResourceLibraryContractHtmlUnit :: getPageForURL Page content. --> " + htmlPage.asXml()); //only needed for debug

            HtmlForm form = htmlPage.getFormByName("buttonForm");
            //Log.info(c, name.getMethodName(), " :: form Page content. --> " + form.asXml()); //only needed for debug

            List<HtmlRadioButtonInput> radioButtons = form.getRadioButtonsByName("buttonForm:myRadio");
            //Log.info(c, name.getMethodName(), " :: radioButtons. --> " + radioButtons.size()); //only needed for debug
            for (HtmlRadioButtonInput radioButton : radioButtons) {
                if (radioButton.getValueAttribute().equalsIgnoreCase("manager")) {
                    radioButton.setChecked(true);
                }
            }
            //Log.info(c, name.getMethodName(), " :: 2form Page content. --> " + form.asXml()); //only needed for debug
            //HtmlButton button = form.getButtonByName("buttonForm:saveContract");

            // Click the commandButton to execute the methods

            HtmlElement button = (HtmlElement) htmlPage.getElementById("buttonForm:saveContract");
            HtmlPage htmlPage2 = (HtmlPage) button.click();

            //Log.info(c, name.getMethodName(), "JSF22ResourceLibraryContractHtmlUnit :: 2getPageForURL Page content. --> " + htmlPage2.asXml()); //only needed for debug
            assertTrue(htmlPage2.asText().contains("This must be template for manager"));
        }
    }

    /**
     * This test will find the contract , Choose team contract.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void Test18_Contract_viaJar_MapDirectory() throws Exception {

        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, "TestResourceContractsFromJar", "faces/management/index.xhtml");
            HtmlPage htmlPage = (HtmlPage) webClient.getPage(url);

            //Log.info(c, name.getMethodName(), "JSF22ResourceLibraryContractHtmlUnit :: getPageForURL Page content. --> " + htmlPage.asXml()); //only needed for debug

            // default contract set to team
            assertTrue(htmlPage.asText().contains("This must be template for team"));

            HtmlForm form = htmlPage.getFormByName("buttonForm");
            //Log.info(c, name.getMethodName(), " :: form Page content. --> " + form.asXml()); //only needed for debug

            List<HtmlRadioButtonInput> radioButtons = form.getRadioButtonsByName("buttonForm:myRadio");
            //Log.info(c, name.getMethodName(), " :: radioButtons. --> " + radioButtons.size()); //only needed for debug
            for (HtmlRadioButtonInput radioButton : radioButtons) {
                if (radioButton.getValueAttribute().equalsIgnoreCase("team")) {
                    radioButton.setChecked(true);
                }
            }
            //Log.info(c, name.getMethodName(), " :: 2form Page content. --> " + form.asXml()); //only needed for debug
            //HtmlButton button = form.getButtonByName("buttonForm:saveContract");

            // Click the commandButton to execute the methods

            HtmlElement button = (HtmlElement) htmlPage.getElementById("buttonForm:saveContract");
            HtmlPage htmlPage2 = (HtmlPage) button.click();

            //Log.info(c, name.getMethodName(), "JSF22ResourceLibraryContractHtmlUnit :: 2getPageForURL Page content. --> " + htmlPage2.asXml()); //only needed for debug

            assertTrue(htmlPage.asText().contains("This must be template for team"));
        }
    }

    /**
     * This test will find the contract from the jar
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    public void Test19_Contract3_viaJar() throws Exception {
        assertTrue(getPageForURL("TestResourceContractsFromJar", "faces/forall/index.xhtml").asText().contains("This must be template for test1"));
    }

    // return the Page for the URL
    private HtmlPage getPageForURL(String contextRoot,
                                   String urlSubstring) throws GeneralSecurityException, FailingHttpStatusCodeException, MalformedURLException, IOException, Exception {

        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, urlSubstring);
            //Get the login Page
            HtmlPage htmlPage = (HtmlPage) webClient.getPage(url);

            //Log.info(c, name.getMethodName(), "JSF22ResourceLibraryContractHtmlUnit :: getPageForURL Page content. --> " + page.getWebResponse().getContentAsString()); //only needed for debug
            if (htmlPage == null) {
                Assert.fail(url + "  did not render properly.");
            }
            //webClient.closeAllWindows();
            return htmlPage;
        }
    }

}
