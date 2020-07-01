/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE7;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi12.alterablecontext.test.AlterableContextTestServlet;
import com.ibm.ws.cdi12.alterablecontext.test.extension.DirtySingleton;
import com.ibm.ws.cdi12.test.common.web.CDICurrentTestServlet;
import com.ibm.ws.cdi12.test.common.web.SimpleBean;
import com.ibm.ws.cdi12.test.current.extension.CDICurrentTestBean;
import com.ibm.ws.fat.cdi.injectInjectionPoint.InjectInjectionPointServlet;
import com.ibm.ws.fat.cdi.injectInjectionPointBeansXML.InjectInjectionPointBeansXMLServlet;
import com.ibm.ws.fat.cdi.injectInjectionPointParam.InjectInjectionPointAsParamServlet;
import com.ibm.ws.fat.cdi.injectInjectionPointXML.InjectInjectionPointXMLServlet;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebBrowserFactory;
import com.ibm.ws.fat.util.browser.WebResponse;

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
import test.conversation.filter.ConversationFilterServlet;

@RunWith(FATRunner.class)
public class CDIAPITests extends FATServletClient {

    public static final String SERVER_NAME = "cdi12APIServer";

    public static final String CDI_CURRENT_APP_NAME = "cdiCurrentTest";
    public static final String ALTERABLE_CONTEXT_APP_NAME = "alterableContextApp";
    public static final String CONVERSATION_FILTER_APP_NAME = "appConversationFilter";
    public static final String INJECT_IP_AS_PARAM_APP_NAME = "injectInjectionPointAsParam";
    public static final String INJECT_IP_BEANS_XML_APP_NAME = "injectInjectionPointBeansXML";
    public static final String INJECT_IP_APP_NAME = "injectInjectionPoint";
    public static final String INJECT_IP_XML_APP_NAME = "injectInjectionPointXML";

    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE7, EE9); //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = AlterableContextTestServlet.class, contextRoot = ALTERABLE_CONTEXT_APP_NAME), //FULL
                    @TestServlet(servlet = InjectInjectionPointAsParamServlet.class, contextRoot = INJECT_IP_AS_PARAM_APP_NAME) }) //FULL
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive cdiCurrentTest = ShrinkWrap.create(JavaArchive.class, CDI_CURRENT_APP_NAME + ".jar")
                                               .addPackage(CDICurrentTestBean.class.getPackage())
                                               .add(new FileAsset(new File("test-applications/cdiCurrentTest.jar/resources/META-INF/services/javax.enterprise.inject.spi.Extension")),
                                                    "/META-INF/services/javax.enterprise.inject.spi.Extension");

        WebArchive cdiCurrentWar = ShrinkWrap.create(WebArchive.class, CDI_CURRENT_APP_NAME + ".war")
                                             .addClass(CDICurrentTestServlet.class.getName())
                                             .addClass(SimpleBean.class.getName())
                                             .addAsLibrary(cdiCurrentTest);

        ShrinkHelper.exportDropinAppToServer(server, cdiCurrentWar, DeployOptions.SERVER_ONLY);

        if (TestModeFilter.shouldRun(TestMode.FULL)) {
            JavaArchive alterableContextExtension = ShrinkWrap.create(JavaArchive.class, "alterableContextExtension.jar");
            alterableContextExtension.addPackage(DirtySingleton.class.getPackage());
            alterableContextExtension.add(new FileAsset(new File("test-applications/alterableContextExtension.jar/resources/META-INF/services/javax.enterprise.inject.spi.Extension")),
                                          "/META-INF/services/javax.enterprise.inject.spi.Extension");
            alterableContextExtension.add(new FileAsset(new File("test-applications/alterableContextExtension.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

            WebArchive alterableContextApp = ShrinkWrap.create(WebArchive.class, ALTERABLE_CONTEXT_APP_NAME + ".war");
            alterableContextApp.addClass(AlterableContextTestServlet.class);
            alterableContextApp.add(new FileAsset(new File("test-applications/alterableContextApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
            alterableContextApp.addAsLibrary(alterableContextExtension);

            EnterpriseArchive alterableContextsEar = ShrinkWrap.create(EnterpriseArchive.class, "alterableContextsApp.ear");
            alterableContextsEar.add(new FileAsset(new File("test-applications/alterableContextsApp.ear/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml");
            alterableContextsEar.add(new FileAsset(new File("test-applications/alterableContextsApp.ear/resources/META-INF/application.xml")), "/META-INF/application.xml");
            alterableContextsEar.addAsModule(alterableContextApp);

            WebArchive appConversationFilter = ShrinkWrap.create(WebArchive.class, CONVERSATION_FILTER_APP_NAME + ".war")
                                                         .addPackage(ConversationFilterServlet.class.getPackage())
                                                         .add(new FileAsset(new File("test-applications/appConversationFilter.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");

            WebArchive injectInjectionPointAsParamWar = ShrinkWrap.create(WebArchive.class, INJECT_IP_AS_PARAM_APP_NAME + ".war")
                                                                  .addPackage(InjectInjectionPointAsParamServlet.class.getPackage())
                                                                  .add(new FileAsset(new File("test-applications/injectInjectionPointAsParam.war/resources/WEB-INF/beans.xml")),
                                                                       "/WEB-INF/beans.xml");

            WebArchive injectInjectionPointBeansXMLWar = ShrinkWrap.create(WebArchive.class, INJECT_IP_BEANS_XML_APP_NAME + ".war")
                                                                   .addClass(InjectInjectionPointBeansXMLServlet.class)
                                                                   .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

            WebArchive injectInjectionPointWar = ShrinkWrap.create(WebArchive.class, INJECT_IP_APP_NAME + ".war")
                                                           .addPackage(InjectInjectionPointServlet.class.getPackage());

            WebArchive injectInjectionPointXMLWar = ShrinkWrap.create(WebArchive.class, INJECT_IP_XML_APP_NAME + ".war")
                                                              .addClass(InjectInjectionPointXMLServlet.class)
                                                              .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                                                              .add(new FileAsset(new File("test-applications/injectInjectionPointXML.war/resources/WEB-INF/web.xml")),
                                                                   "/WEB-INF/web.xml");

            ShrinkHelper.exportDropinAppToServer(server, injectInjectionPointXMLWar, DeployOptions.SERVER_ONLY);
            ShrinkHelper.exportDropinAppToServer(server, injectInjectionPointWar, DeployOptions.SERVER_ONLY);
            ShrinkHelper.exportDropinAppToServer(server, injectInjectionPointBeansXMLWar, DeployOptions.SERVER_ONLY);
            ShrinkHelper.exportDropinAppToServer(server, injectInjectionPointAsParamWar, DeployOptions.SERVER_ONLY);
            ShrinkHelper.exportDropinAppToServer(server, appConversationFilter, DeployOptions.SERVER_ONLY);
            ShrinkHelper.exportDropinAppToServer(server, alterableContextsEar, DeployOptions.SERVER_ONLY);
        }

        server.startServer(false, false);
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
    public void testInjectInjectionPointBeansXML() throws Exception {
        List<String> logs = server.findStringsInLogs("CWWKZ0002E(?=.*injectInjectionPoint)(?=.*com.ibm.ws.container.service.state.StateChangeException)(?=.*org.jboss.weld.exceptions.DefinitionException)(?=.*WELD-001405)(?=.*BackedAnnotatedField)(?=.*com.ibm.ws.fat.cdi.injectInjectionPointBeansXML.InjectInjectionPointBeansXMLServlet.thisShouldFail)");
        assertEquals("DefinitionException not found", 1, logs.size());
    }

    @Test
    @Mode(TestMode.FULL)
    public void testInjectInjectionPoint() throws Exception {
        List<String> logs = server.findStringsInLogs("CWWKZ0002E(?=.*injectInjectionPoint)(?=.*com.ibm.ws.container.service.state.StateChangeException)(?=.*org.jboss.weld.exceptions.DefinitionException)(?=.*WELD-001405)(?=.*BackedAnnotatedField)(?=.*com.ibm.ws.fat.cdi.injectInjectionPoint.InjectInjectionPointServlet.thisShouldFail)");
        assertEquals("DefinitionException not found", 1, logs.size()); //Unlike the two sibling tests this only emits the message once.
    }

    @Test
    @Mode(TestMode.FULL)
    public void testInjectInjectionPointXML() throws Exception {
        List<String> logs = server.findStringsInLogs("CWWKZ0002E(?=.*injectInjectionPoint)(?=.*com.ibm.ws.container.service.state.StateChangeException)(?=.*org.jboss.weld.exceptions.DefinitionException)(?=.*WELD-001405)(?=.*BackedAnnotatedField)(?=.*com.ibm.ws.fat.cdi.injectInjectionPointXML.InjectInjectionPointXMLServlet.thisShouldFail)");
        assertEquals("DefinitionException not found", 1, logs.size());
    }

}
