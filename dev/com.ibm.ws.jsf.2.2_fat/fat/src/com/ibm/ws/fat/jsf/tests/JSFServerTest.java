/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.fat.jsf.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.OVERWRITE;
import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;


import org.jboss.shrinkwrap.api.spec.WebArchive;
import com.ibm.websphere.simplicity.ShrinkHelper;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import com.ibm.ws.fat.jsf.JSFUtils;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RunUnlessFeatureBeingTested;
import componenttest.annotation.Server;
import componenttest.topology.impl.LibertyServer;

/**
 * All JSF 2.2 tests with all applicable server features enabled.
 *
 * Tests that just need to drive a simple request using our WebBrowser object can be placed in this class.
 *
 * If a test needs HtmlUnit it should more than likely be placed in the JSFHtmlUnit test class.
 */
@MinimumJavaLevel(javaLevel = 7)
@RunWith(FATRunner.class)
public class JSFServerTest {
@Rule
public TestName name = new TestName();

    String contextRoot = "TestJSF2.2";

    protected static final Class<?> c = JSFServerTest.class;

    @Server("jsfTestServer1")
    public static LibertyServer jsfTestServer1;

    @BeforeClass
    public static void setup() throws Exception {

            WebArchive testWar = ShrinkHelper.buildDefaultApp("TestJSF2.2.war", "com.ibm.ws.fat.jsf.bean",
                                                            "com.ibm.ws.fat.jsf.cforeach",
                                                            "com.ibm.ws.fat.jsf.externalContext",
                                                            "com.ibm.ws.fat.jsf.html5",
                                                            "com.ibm.ws.fat.jsf.listener");

            WebArchive TestResourceContractsWar = ShrinkHelper.buildDefaultApp("TestResourceContracts.war");

            WebArchive TestResourceContractsDirectoryWar = ShrinkHelper.buildDefaultApp("TestResourceContractsDirectory.war");

            WebArchive TestResourceContractsFromJarWar = ShrinkHelper.buildDefaultApp("TestResourceContractsFromJar.war", "beans");

            WebArchive flashWar = ShrinkHelper.buildDefaultApp("JSF22FlashEvents.war", "com.ibm.ws.fat.jsf.factory",
                                                            "com.ibm.ws.fat.jsf.flash",
                                                            "com.ibm.ws.fat.jsf.listener");

            WebArchive viewActionWar = ShrinkHelper.buildDefaultApp("TestJSF22ViewAction.war", "com.ibm.ws.fat.jsf.viewAction",
                                                                    "com.ibm.ws.fat.jsf.viewAction.phaseListener");
                                                                    
            WebArchive ResourceResolverWar = ShrinkHelper.buildDefaultApp("JSF22FaceletsResourceResolverAnnotation.war", "com.ibm.ws.jsf");

            EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "TestJSF2.2.ear")
                                    .addAsModule(testWar)
                                    .addAsModule(viewActionWar)
                                    .addAsModule(ResourceResolverWar)
                                    .addAsModule(TestResourceContractsWar)
                                    .addAsModule(TestResourceContractsDirectoryWar)
                                    .addAsModule(TestResourceContractsFromJarWar).addAsModule(flashWar);

            String testAppResourcesDir = "test-applications/" + "TestJSF2.2.ear" + "/resources/";

            ShrinkHelper.addDirectory(ear, testAppResourcesDir);
            ShrinkHelper.exportDropinAppToServer(jsfTestServer1, ear, OVERWRITE);

            jsfTestServer1.startServer(JSFServerTest.class.getSimpleName() + ".log");
    }


    @AfterClass
    public static void tearDown() throws Exception {
            // Stop the server
            if (jsfTestServer1 != null && jsfTestServer1.isStarted()) {
                    jsfTestServer1.stopServer();
            }
    }

    /**
     * Sample test
     *
     * @throws Exception
     *             if something goes horribly wrong
     */
    @Test
    public void testServlet() throws Exception {
        WebClient webClient = new WebClient();
        URL url = JSFUtils.createHttpUrl(jsfTestServer1, contextRoot, "");
        HtmlPage page = (HtmlPage) webClient.getPage(url);
        assertTrue(page.asText().contains("Hello World"));
    }

    /**
     * Test to ensure that the com.ibm.ws.jsf.ext.LibertyWebConfigProviderFactory is being used. If there is not entry trace
     * then we know that something happened and the LibertyWebConfigProviderFactory is not being used as it should be.
     *
     * @throws Exception
     */
    @RunUnlessFeatureBeingTested("jsf-2.3")
    @Test
    public void testLibertyWebConfigProviderFactory() throws Exception {
        String msgToSearchFor = "getWebConfigProvider Entry";

        // Check the trace.log to see if the LibertyWebConfigProviderFactory has any entry trace.
        String isLibertyWebConfigProviderFactoryBeingUsed = jsfTestServer1.waitForStringInTrace(msgToSearchFor);

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the trace log: " + msgToSearchFor, isLibertyWebConfigProviderFactoryBeingUsed);
    }

    /**
     * Test to ensure that the com.ibm.ws.jsf.ext.LibertyWebConfigProvider is being used. If there is not entry trace
     * then we know that something happened and the LibertyWebConfigProviderFactory and LibertyWebConfigProvider are not
     * being used as they should be.
     *
     * @throws Exception
     */
    @RunUnlessFeatureBeingTested("jsf-2.3")
    @Test
    public void testLibertyWebConfigProvider() throws Exception {
        String msgToSearchFor = "isErrorPagePresent ENTRY";

        // Check the trace.log to see if the LibertyWebConfigProvider has any entry trace.
        String isLibertyWebConfigProviderBeingUsed = jsfTestServer1.waitForStringInTrace(msgToSearchFor);

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the trace log: " + msgToSearchFor, isLibertyWebConfigProviderBeingUsed);
    }

    /**
     * Test to ensure that the com.ibm.ws.jsf.config.resource.LibertyFaceletConfigResourceProvider is being used. If there is not any
     * entry trace then we know that something happened and the LibertyFaceletConfigResourceProvider is not being used as it should be.
     *
     * @throws Exception
     */
    @Test
    public void testLibertyFaceletConfigResourceProvider() throws Exception {
        String msgToSearchFor = "getFaceletTagLibConfigurationResources ENTRY";

        // Check the trace.log to see if the LibertyFaceletConfigResourceProvider has any entry trace.
        String isLibertyFaceletConfigResourceProviderBeingUsed = jsfTestServer1.waitForStringInTrace(msgToSearchFor);

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the trace logs: " + msgToSearchFor,
                      isLibertyFaceletConfigResourceProviderBeingUsed);
    }

    /**
     * Test to ensure that the com.ibm.ws.jsfconfig.annotation.WASMyFacesAnnotationProvider is being used. If there is not any
     * entry trace then we know that something happened and the WASMyFacesAnnotationProvider is not being used as it should be.
     *
     * @throws Exception
     */
    @Test
    public void testWASMyFacesAnnotationProvider() throws Exception {
        String msgToSearchFor = "com.ibm.ws.jsf.config.annotation.WASMyFacesAnnotationProvider <init> ENTRY";

        // Check the trace.log to see if the WASMyFacesAnnotationProvider has any entry trace.
        String isWASMyFacesAnnotationProviderBeingUsed = jsfTestServer1.waitForStringInTrace(msgToSearchFor);

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the trace logs: " + msgToSearchFor,
                      isWASMyFacesAnnotationProviderBeingUsed);
    }

    /**
     * Test to ensure that the com.ibm.ws.jsfconfig.annotation.WebSphereLifecycleProviderFactory is being used. If there is not any
     * entry trace then we know that something happened and the WebSphereLifecycleProviderFactory is not being used as it should be.
     *
     * @throws Exception
     */
    @Test
    public void testWebSphereLifecycleProviderFactory() throws Exception {
        String msgToSearchFor = "com.ibm.ws.jsf.config.annotation.WebSphereLifecycleProviderFactory <init> ENTRY";

        // Check the trace.log to see if the WebSphereLifecycleProviderFactory has any entry trace.
        String isWebSphereLifecycleProviderFactoryBeingUsed = jsfTestServer1.waitForStringInTrace(msgToSearchFor);

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the trace logs: " + msgToSearchFor,
                      isWebSphereLifecycleProviderFactoryBeingUsed);
    }

    /**
     * Test to ensure that the com.ibm.ws.jsfconfig.annotation.WebSphereAnnotationLifecycleProvider is being used. If there is not any
     * entry trace then we know that something happened and the WebSphereAnnotationLifecycleProvider is not being used as it should be.
     *
     * @throws Exception
     */
    @Test
    public void testWebSphereAnnotationLifecycleProvider() throws Exception {
        String msgToSearchFor = "com.ibm.ws.jsf.config.annotation.WebSphereAnnotationLifecycleProvider <init> ENTRY";

        // Check the trace.log to see if the WebSphereAnnotationLifecycleProvider has any entry trace.
        String isWebSphereAnnotationLifecycleProviderBeingUsed = jsfTestServer1.waitForStringInTrace(msgToSearchFor);

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the trace logs: " + msgToSearchFor,
                      isWebSphereAnnotationLifecycleProviderBeingUsed);
    }

    /**
     * Test to ensure that when the jsf-2.2 is enabled, beanValidation-1.1 is disabled
     * We do this by looking for a message in the logs
     *
     * @throws Exception
     */
    @Test
    public void testBeanValidation11Disabled() throws Exception {
        String msgToSearchFor = "MyFaces Bean Validation support disabled";

        Log.info(c, name.getMethodName(), "Looking for : " + msgToSearchFor);
        // Check the trace.log to see if the LibertyWebConfigProviderFactory has any entry trace.
        String isBeanValidationDisabled = jsfTestServer1.waitForStringInLog(msgToSearchFor);

        Log.info(c, name.getMethodName(), "Message found after searching logs : " + isBeanValidationDisabled);

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the trace log: " + msgToSearchFor, isBeanValidationDisabled);
    }

    /**
     * Test to ensure that a class annotated with the FaceletsResourceResolver annotation is used by the MyFaces runtime.
     *
     * @throws Exception
     */
    @Test
    public void testFaceletsResourceResolverAnnotation() throws Exception {
        String msgToSearchFor = "FaceletsResourceResolver annotation worked, using custom ResourceResolver";

        // Use the SharedServer to verify a response.
        WebClient webClient = new WebClient();
        URL url = JSFUtils.createHttpUrl(jsfTestServer1, "JSF22FaceletsResourceResolverAnnotation", "index.xhtml");
        HtmlPage page = (HtmlPage) webClient.getPage(url);
        assertTrue(page.asText().contains("Hello World"));

        // There should be a match so fail if there is not.
        assertNotNull("The following message was not found in the logs: " + msgToSearchFor,
                      jsfTestServer1.waitForStringInLog(msgToSearchFor));

    }
}