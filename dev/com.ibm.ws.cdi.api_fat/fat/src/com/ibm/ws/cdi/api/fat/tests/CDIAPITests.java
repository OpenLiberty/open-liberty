/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.api.fat.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE7;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.beansxml.BeansAsset;
import com.ibm.ws.cdi.api.fat.apps.alterablecontext.AlterableContextTestServlet;
import com.ibm.ws.cdi.api.fat.apps.alterablecontext.extension.AlterableContextExtension;
import com.ibm.ws.cdi.api.fat.apps.alterablecontext.extension.DirtySingleton;
import com.ibm.ws.cdi.api.fat.apps.conversationfilter.ConversationFilterServlet;
import com.ibm.ws.cdi.api.fat.apps.current.CDICurrentTestServlet;
import com.ibm.ws.cdi.api.fat.apps.current.SimpleBean;
import com.ibm.ws.cdi.api.fat.apps.current.extension.CDICurrentTestBean;
import com.ibm.ws.cdi.api.fat.apps.current.extension.MyDeploymentVerifier;
import com.ibm.ws.cdi.api.fat.apps.injectInjectionPoint.InjectInjectionPointServlet;
import com.ibm.ws.cdi.api.fat.apps.injectInjectionPointBeansXML.InjectInjectionPointBeansXMLServlet;
import com.ibm.ws.cdi.api.fat.apps.injectInjectionPointParam.InjectInjectionPointAsParamServlet;
import com.ibm.ws.cdi.api.fat.apps.injectInjectionPointXML.InjectInjectionPointXMLServlet;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebBrowserFactory;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class CDIAPITests extends FATServletClient {

    public static final String SERVER_NAME = "cdi12APIServer";

    public static final String CDI_CURRENT_APP_NAME = "cdiCurrentTest";
    public static final String ALTERABLE_CONTEXT_APP_NAME = "alterableContextsApp";
    public static final String CONVERSATION_FILTER_APP_NAME = "appConversationFilter";
    public static final String INJECT_IP_AS_PARAM_APP_NAME = "injectInjectionPointAsParam";
    public static final String INJECT_IP_BEANS_XML_APP_NAME = "injectInjectionPointBeansXML";
    public static final String INJECT_IP_APP_NAME = "injectInjectionPoint";
    public static final String INJECT_IP_XML_APP_NAME = "injectInjectionPointXML";

    private static final String DEFINITION_EXCEPTION = "org.jboss.weld.exceptions.DefinitionException";
    private static final String STATE_CHANGE_EXCEPTION = "com.ibm.ws.container.service.state.StateChangeException";

    private static final String DEFINITION_EXCEPTION_PATTERN_PREFIX = "CWWKZ0002E(?=.*injectInjectionPoint)(?=.*" + STATE_CHANGE_EXCEPTION + ")(?=.*" + DEFINITION_EXCEPTION
                                                                      + ")(?=.*WELD-001405)(?=.*BackedAnnotatedField)(?=.*";
    private static final String THIS_SHOULD_FAIL_SUFFIX = ".thisShouldFail)";

    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9, EE7); //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = AlterableContextTestServlet.class, contextRoot = ALTERABLE_CONTEXT_APP_NAME), //FULL
                    @TestServlet(servlet = InjectInjectionPointAsParamServlet.class, contextRoot = INJECT_IP_AS_PARAM_APP_NAME) }) //FULL
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive cdiCurrentTest = ShrinkWrap.create(JavaArchive.class, CDI_CURRENT_APP_NAME + ".jar")
                                               .addPackage(CDICurrentTestBean.class.getPackage());
        CDIArchiveHelper.addCDIExtensionService(cdiCurrentTest, MyDeploymentVerifier.class);

        WebArchive cdiCurrentWar = ShrinkWrap.create(WebArchive.class, CDI_CURRENT_APP_NAME + ".war")
                                             .addClass(CDICurrentTestServlet.class.getName())
                                             .addClass(SimpleBean.class.getName())
                                             .addAsLibrary(cdiCurrentTest);

        ShrinkHelper.exportDropinAppToServer(server, cdiCurrentWar, DeployOptions.SERVER_ONLY);

        if (TestModeFilter.shouldRun(TestMode.FULL)) {
            JavaArchive alterableContextExtension = ShrinkWrap.create(JavaArchive.class, "alterableContextExtension.jar");
            alterableContextExtension.addPackage(DirtySingleton.class.getPackage());
            CDIArchiveHelper.addCDIExtensionService(alterableContextExtension, AlterableContextExtension.class);
            CDIArchiveHelper.addBeansXML(alterableContextExtension, BeansAsset.DiscoveryMode.ALL);

            WebArchive alterableContextApp = ShrinkWrap.create(WebArchive.class, ALTERABLE_CONTEXT_APP_NAME + ".war");
            alterableContextApp.addClass(AlterableContextTestServlet.class);
            CDIArchiveHelper.addBeansXML(alterableContextApp, BeansAsset.DiscoveryMode.ALL);
            alterableContextApp.addAsLibrary(alterableContextExtension);

            EnterpriseArchive alterableContextsEar = ShrinkWrap.create(EnterpriseArchive.class, "alterableContextsApp.ear");
            alterableContextsEar.addAsManifestResource(AlterableContextTestServlet.class.getPackage(), "permissions.xml", "permissions.xml");
            alterableContextsEar.addAsModule(alterableContextApp);

            WebArchive appConversationFilter = ShrinkWrap.create(WebArchive.class, CONVERSATION_FILTER_APP_NAME + ".war")
                                                         .addPackage(ConversationFilterServlet.class.getPackage())
                                                         .addAsWebInfResource(ConversationFilterServlet.class.getPackage(), "web.xml", "web.xml");

            WebArchive injectInjectionPointAsParamWar = ShrinkWrap.create(WebArchive.class, INJECT_IP_AS_PARAM_APP_NAME + ".war")
                                                                  .addPackage(InjectInjectionPointAsParamServlet.class.getPackage());
            CDIArchiveHelper.addEmptyBeansXML(injectInjectionPointAsParamWar);

            ShrinkHelper.exportDropinAppToServer(server, injectInjectionPointAsParamWar, DeployOptions.SERVER_ONLY);
            ShrinkHelper.exportDropinAppToServer(server, appConversationFilter, DeployOptions.SERVER_ONLY);
            ShrinkHelper.exportDropinAppToServer(server, alterableContextsEar, DeployOptions.SERVER_ONLY);
        }

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKZ0002E");
    }

    @Test
    @Mode(TestMode.LITE)
    public void testCDICurrent() throws Exception {
        runTest(server, CDI_CURRENT_APP_NAME, "testCDICurrent");

        server.restartApplication(CDI_CURRENT_APP_NAME);

        runTest(server, CDI_CURRENT_APP_NAME, "testCDICurrent");
    }

    @Test
    @Mode(TestMode.LITE)
    public void testCDICurrentViaMES() throws Exception {
        runTest(server, CDI_CURRENT_APP_NAME, "testCDICurrentViaMES");
    }

    @Test
    @Mode(TestMode.FULL)
    public void testConversationFilter() throws Exception {
        WebBrowser browser = WebBrowserFactory.getInstance().createWebBrowser((File) null);

        WebResponse response = browser.request(HttpUtils.createURL(server, "/appConversationFilter/test?op=begin").toString());
        String cid = response.getResponseBody();
        assertTrue("No cid: " + cid, cid != null && !!!cid.isEmpty());

        response = browser.request(HttpUtils.createURL(server, "/appConversationFilter/test?op=status&cid=" + cid).toString());
        String status = response.getResponseBody();
        assertEquals("Wrong status", Boolean.FALSE.toString(), status);
    }

    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC(DEFINITION_EXCEPTION)
    @AllowedFFDC(STATE_CHANGE_EXCEPTION)
    public void testInjectInjectionPointBeansXML() throws Exception {
        server.setMarkToEndOfLog();

        WebArchive injectInjectionPointBeansXMLWar = ShrinkWrap.create(WebArchive.class, INJECT_IP_BEANS_XML_APP_NAME + ".war")
                                                               .addClass(InjectInjectionPointBeansXMLServlet.class);
        CDIArchiveHelper.addEmptyBeansXML(injectInjectionPointBeansXMLWar);

        ShrinkHelper.exportToServer(server, "dropins", injectInjectionPointBeansXMLWar, DeployOptions.SERVER_ONLY);

        String log = server.waitForStringInLog(DEFINITION_EXCEPTION_PATTERN_PREFIX
                                               + InjectInjectionPointBeansXMLServlet.class.getName() + THIS_SHOULD_FAIL_SUFFIX);
        assertNotNull("DefinitionException not found", log);
    }

    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC(DEFINITION_EXCEPTION)
    @AllowedFFDC(STATE_CHANGE_EXCEPTION)
    public void testInjectInjectionPoint() throws Exception {
        server.setMarkToEndOfLog();

        WebArchive injectInjectionPointWar = ShrinkWrap.create(WebArchive.class, INJECT_IP_APP_NAME + ".war")
                                                       .addPackage(InjectInjectionPointServlet.class.getPackage());

        ShrinkHelper.exportToServer(server, "dropins", injectInjectionPointWar, DeployOptions.SERVER_ONLY);

        String log = server.waitForStringInLog(DEFINITION_EXCEPTION_PATTERN_PREFIX
                                               + InjectInjectionPointServlet.class.getName() + THIS_SHOULD_FAIL_SUFFIX);
        assertNotNull("DefinitionException not found", log);
    }

    @Test
    @Mode(TestMode.FULL)
    @ExpectedFFDC(DEFINITION_EXCEPTION)
    @AllowedFFDC(STATE_CHANGE_EXCEPTION)
    public void testInjectInjectionPointXML() throws Exception {
        server.setMarkToEndOfLog();

        WebArchive injectInjectionPointXMLWar = ShrinkWrap.create(WebArchive.class, INJECT_IP_XML_APP_NAME + ".war")
                                                          .addClass(InjectInjectionPointXMLServlet.class)
                                                          .addAsWebInfResource(InjectInjectionPointXMLServlet.class.getPackage(), "web.xml", "web.xml");
        CDIArchiveHelper.addEmptyBeansXML(injectInjectionPointXMLWar);

        ShrinkHelper.exportToServer(server, "dropins", injectInjectionPointXMLWar, DeployOptions.SERVER_ONLY);

        String log = server.waitForStringInLog(DEFINITION_EXCEPTION_PATTERN_PREFIX
                                               + InjectInjectionPointXMLServlet.class.getName() + THIS_SHOULD_FAIL_SUFFIX);
        assertNotNull("DefinitionException not found", log);
    }

}
