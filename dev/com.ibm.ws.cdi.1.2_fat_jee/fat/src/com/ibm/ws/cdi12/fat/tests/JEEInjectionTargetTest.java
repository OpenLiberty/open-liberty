/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import java.io.File;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;

public class JEEInjectionTargetTest extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12JEEInjectionTargetTestServer");

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {

        JavaArchive jeeInjectionTargetTestEJB = ShrinkWrap.create(JavaArchive.class,"jeeInjectionTargetTestEJB.jar")
                        .addClass("cdi12.helloworld.jeeResources.ejb.MySessionBean1")
                        .addClass("cdi12.helloworld.jeeResources.ejb.interceptors.MyAnotherEJBInterceptor")
                        .addClass("cdi12.helloworld.jeeResources.ejb.interceptors.MyEJBJARXMLDefinedInterceptor")
                        .addClass("cdi12.helloworld.jeeResources.ejb.interceptors.MyManagedBeanEJBInterceptor")
                        .addClass("cdi12.helloworld.jeeResources.ejb.interceptors.MyEJBInterceptor")
                        .addClass("cdi12.helloworld.jeeResources.ejb.MyEJBDefinedInXml")
                        .addClass("cdi12.helloworld.jeeResources.ejb.ManagedBeanInterface")
                        .addClass("cdi12.helloworld.jeeResources.ejb.MyCDIBean1")
                        .addClass("cdi12.helloworld.jeeResources.ejb.SessionBeanInterface")
                        .addClass("cdi12.helloworld.jeeResources.ejb.MyManagedBean1")
                        .addClass("cdi12.helloworld.jeeResources.ejb.MySessionBean2")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestEJB.jar/resources/META-INF/ejb-jar.xml")), "/META-INF/ejb-jar.xml")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestEJB.jar/resources/META-INF/ibm-managed-bean-bnd.xml")), "/META-INF/ibm-managed-bean-bnd.xml");

        WebArchive jeeInjectionTargetTest = ShrinkWrap.create(WebArchive.class, "jeeInjectionTargetTest.war")
                        .addClass("cdi12.helloworld.jeeResources.test.JEEResourceTestServletCtorInjection")
                        .addClass("cdi12.helloworld.jeeResources.test.MySessionBean")
                        .addClass("cdi12.helloworld.jeeResources.test.JEEResourceTestServletNoInjection")
                        .addClass("cdi12.helloworld.jeeResources.test.MyServerEndpoint")
                        .addClass("cdi12.helloworld.jeeResources.test.JEEResourceTestServlet")
                        .addClass("cdi12.helloworld.jeeResources.test.MyMessageDrivenBean")
                        .addClass("cdi12.helloworld.jeeResources.test.LoggerServlet")
                        .addClass("cdi12.helloworld.jeeResources.test.JEEResourceExtension")
                        .addClass("cdi12.helloworld.jeeResources.test.HelloWorldExtensionBean2")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTest.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTest.war/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension");

        WebArchive jeeInjectionTargetTestJSP = ShrinkWrap.create(WebArchive.class, "jeeInjectionTargetTestJSP2.3.war")
                        .addClass("tagHandler.JspCdiHitMeTag")
                        .addClass("tagHandler.MethodInjectionTag")
                        .addClass("beans.TestMethodInjectionApplicationScoped")
                        .addClass("beans.TestConstructorInjectionDependentScoped")
                        .addClass("beans.Employee")
                        .addClass("beans.TestConstructorInjectionApplicationScoped")
                        .addClass("beans.TestConstructorInjectionSessionScoped")
                        .addClass("beans.TestMethodInjectionRequestScoped")
                        .addClass("beans.TestTagInjectionRequestBean")
                        .addClass("beans.TestFieldInjectionSessionScoped")
                        .addClass("beans.TestFieldInjectionRequestScoped")
                        .addClass("beans.EL30StaticFieldsAndMethodsEnum")
                        .addClass("beans.TestMethodInjectionDependentScoped")
                        .addClass("beans.EL30CoercionRulesTestBean")
                        .addClass("beans.EL30StaticFieldsAndMethodsBean")
                        .addClass("beans.EL30InvocationMethodExpressionTestBean")
                        .addClass("beans.EL30ReserverdWordsTestBean")
                        .addClass("beans.TestTagInjectionSessionBean")
                        .addClass("beans.TestFieldInjectionDependentScoped")
                        .addClass("beans.TestMethodInjectionSessionScoped")
                        .addClass("beans.TestTagInjectionDependentBean")
                        .addClass("beans.TestTagInjectionApplicationBean")
                        .addClass("beans.Pojo1")
                        .addClass("beans.TestConstructorInjectionRequestScoped")
                        .addClass("beans.EL30MapCollectionObjectBean")
                        .addClass("beans.TestFieldInjectionApplicationScoped")
                        .addClass("listeners.JspCdiTagLibraryEventListenerCI")
                        .addClass("listeners.JspCdiTagLibraryEventListenerMI")
                        .addClass("listeners.JspCdiTagLibraryEventListenerFI")
                        .addClass("servlets.SimpleTestServlet")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30PropertyNotFoundException.jsp")), "/EL30PropertyNotFoundException.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30OperatorPrecedences.jsp")), "/EL30OperatorPrecedences.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/TagLibraryEventListenerCI.jsp")), "/TagLibraryEventListenerCI.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/WEB-INF/tlds/EventListeners.tld")), "/WEB-INF/tlds/EventListeners.tld")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/WEB-INF/tlds/Tag2Lib.tld")), "/WEB-INF/tlds/Tag2Lib.tld")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/WEB-INF/tlds/Tag1Lib.tld")), "/WEB-INF/tlds/Tag1Lib.tld")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/WEB-INF/tags/EL30CoercionRulesTest.tag")), "/WEB-INF/tags/EL30CoercionRulesTest.tag")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/TagLibraryEventListenerMI.jsp")), "/TagLibraryEventListenerMI.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/lt.jsp")), "/EL30ReservedWords/lt.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/gt.jsp")), "/EL30ReservedWords/gt.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/false.jsp")), "/EL30ReservedWords/false.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/mod.jsp")), "/EL30ReservedWords/mod.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/le.jsp")), "/EL30ReservedWords/le.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/or.jsp")), "/EL30ReservedWords/or.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/NonReservedWords.jsp")), "/EL30ReservedWords/NonReservedWords.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/ne.jsp")), "/EL30ReservedWords/ne.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/instanceof.jsp")), "/EL30ReservedWords/instanceof.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/not.jsp")), "/EL30ReservedWords/not.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/ge.jsp")), "/EL30ReservedWords/ge.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/empty.jsp")), "/EL30ReservedWords/empty.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/eq.jsp")), "/EL30ReservedWords/eq.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/null.jsp")), "/EL30ReservedWords/null.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/true.jsp")), "/EL30ReservedWords/true.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/and.jsp")), "/EL30ReservedWords/and.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/div.jsp")), "/EL30ReservedWords/div.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL22Operators.jsp")), "/EL22Operators.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30CoercionRules.jsp")), "/EL30CoercionRules.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30Lambda.jsp")), "/EL30Lambda.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30InvocationMethodExpressions.jsp")), "/EL30InvocationMethodExpressions.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/Tag2.jsp")), "/Tag2.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30MethodNotFoundException.jsp")), "/EL30MethodNotFoundException.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30PropertyNotWritableException.jsp")), "/EL30PropertyNotWritableException.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/TagLibraryEventListenerFI.jsp")), "/TagLibraryEventListenerFI.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30CollectionObjectOperations.jsp")), "/EL30CollectionObjectOperations.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30AssignmentOperatorException.jsp")), "/EL30AssignmentOperatorException.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30Operators.jsp")), "/EL30Operators.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/Tag1.jsp")), "/Tag1.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30StaticFieldsAndMethodsTests.jsp")), "/EL30StaticFieldsAndMethodsTests.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/Servlet31RequestResponseTest.jsp")), "/Servlet31RequestResponseTest.jsp");

        return ShrinkWrap.create(EnterpriseArchive.class,"jeeInjectionTargetTest.ear")
                        .addAsModule(jeeInjectionTargetTestEJB)
                        .addAsModule(jeeInjectionTargetTest)
                        .addAsModule(jeeInjectionTargetTestJSP);
    }


    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testServletInjectionTargetEvent() throws Exception {

        WebBrowser wb = createWebBrowserForTestCase();
        WebResponse response = getSharedServer().getResponse(wb, "/jeeInjectionTargetTest/log");
        String body = response.getResponseBody();
        String[] messages = body.split(",");

        int beforeBeanDiscovery = index(messages, "BeforeBeanDiscovery!");
        int afterTypeDiscovery = index(messages, "AfterTypeDiscovery!");
        int afterBeanDiscovery = index(messages, "AfterBeanDiscovery!");
        int afterDeploymentValidation = index(messages, "AfterDeploymentValidation!");

        Assert.assertTrue("Before Bean Discovery Event not found", beforeBeanDiscovery == 0);
        Assert.assertTrue("After Type Discovery Event not found", afterTypeDiscovery == 1);
        Assert.assertTrue("After Bean Discovery Event not found", afterBeanDiscovery > 1);
        Assert.assertTrue("After Deployment Validation Event not found", afterDeploymentValidation > afterBeanDiscovery);

        assertPosition("Session Bean MySessionBean",
                       messages,
                       "Session Bean! Injection Target Processed:.*class cdi12.helloworld.jeeResources.test.MySessionBean",
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("Session Bean MySessionBean1",
                       messages,
                       "Session Bean! Injection Target Processed:.*class cdi12.helloworld.jeeResources.ejb.MySessionBean1",
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("Session Bean MySessionBean2",
                       messages,
                       "Session Bean! Injection Target Processed:.*class cdi12.helloworld.jeeResources.ejb.MySessionBean2",
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("CDI Bean HelloWorldExtensionBean2",
                       messages,
                       "CDI Bean! Injection Target Processed:.*class cdi12.helloworld.jeeResources.test.HelloWorldExtensionBean2",
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("Managed Bean MyManagedBean",
                       messages,
                       "Managed Bean! Injection Target Processed:.*class cdi12.helloworld.jeeResources.ejb.MyManagedBean1",
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("Servlet JEEResourceTestServlet",
                       messages,
                       "Servlet! Injection Target Processed:.*@WebServlet class cdi12.helloworld.jeeResources.test.JEEResourceTestServlet",
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("Message Driven Bean MyMessageDrivenBean",
                       messages,
                       "Message Driven Bean! Injection Target Processed:.*class cdi12.helloworld.jeeResources.test.MyMessageDrivenBean",
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("Servlet JEEResourceTestServletCtorInjection",
                       messages,
                       "Servlet! Injection Target Processed:.*@WebServlet class cdi12.helloworld.jeeResources.test.JEEResourceTestServletCtorInjection",
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("WebSocket Server Endpoint MyServerEndpoint",
                       messages,
                       "Websocket Server Endpoint! Injection Target Processed:.*@ServerEndpoint class cdi12.helloworld.jeeResources.test.MyServerEndpoint",
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("JSP Tag Handler JspCdiHitMeTag",
                       messages,
                       "JSP Tag Handler! Injection Target Processed:.*class tagHandler.JspCdiHitMeTag",
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("Servlet Event Listener JspCdiTagLibraryEventListenerCI",
                       messages,
                       "Servlet Event Listener! Injection Target Processed:.*class listeners.JspCdiTagLibraryEventListenerCI",
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("JSP Tag Handler MethodInjectionTag",
                       messages,
                       "JSP Tag Handler! Injection Target Processed:.*class tagHandler.MethodInjectionTag",
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("Servlet Event Listener JspCdiTagLibraryEventListenerMI",
                       messages,
                       "Servlet Event Listener! Injection Target Processed:.*class listeners.JspCdiTagLibraryEventListenerMI",
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("Servlet SimpleTestServlet",
                       messages,
                       "Servlet! Injection Target Processed:.*@WebServlet class servlets.SimpleTestServlet",
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("Servlet Event Listener JspCdiTagLibraryEventListenerFI",
                       messages,
                       "Servlet Event Listener! Injection Target Processed:.*class listeners.JspCdiTagLibraryEventListenerFI",
                       afterTypeDiscovery, afterBeanDiscovery);
        assertPosition("Non CDI Interceptors",
                       messages,
                       "NonCDIInterceptor1! Injection Target Processed:.*cdi12.helloworld.jeeResources.ejb.interceptors.MyEJBInterceptor",
                       afterTypeDiscovery, afterBeanDiscovery);
        assertPosition("Non CDI Interceptors",
                       messages,
                       "NonCDIInterceptor2! Injection Target Processed:.*cdi12.helloworld.jeeResources.ejb.interceptors.MyAnotherEJBInterceptor",
                       afterTypeDiscovery, afterBeanDiscovery);
        assertPosition("Non CDI Interceptors",
                       messages,
                       "NonCDIInterceptor3! Injection Target Processed:.*cdi12.helloworld.jeeResources.ejb.interceptors.MyManagedBeanEJBInterceptor",
                       afterTypeDiscovery, afterBeanDiscovery);
        assertPosition("Non CDI Interceptor defined in XML",
                       messages,
                       "NON CDI Interceptor defined In ejb-jar.xml! Injection Target Processed:.*cdi12.helloworld.jeeResources.ejb.interceptors.MyEJBJARXMLDefinedInterceptor",
                       afterTypeDiscovery, afterBeanDiscovery);
    }

    private void assertPosition(String bean, String[] strings, String regex, int before, int after) {
        int index = index(strings, regex);
        Assert.assertTrue("Process Injection Target Event not found for " + bean, index != -1);
        if (before != -1) {
            Assert.assertTrue("Process Injection Target Event for " + bean + " occurred too soon", index > before);
        }
        if (after != -1) {
            Assert.assertTrue("Process Injection Target Event for " + bean + " occurred too late", index < after);
        }
    }

    private int index(String[] strings, String regex) {
        for (int i = 0; i < strings.length; i++) {
            boolean matches = Pattern.matches(regex, strings[i]);
            if (matches) {
                return i;
            }
        }
        return -1;
    }

}
