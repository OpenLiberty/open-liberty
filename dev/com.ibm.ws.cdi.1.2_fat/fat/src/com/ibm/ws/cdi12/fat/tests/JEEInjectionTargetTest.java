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
package com.ibm.ws.cdi12.fat.tests;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;

public class JEEInjectionTargetTest extends LoggingTest {

    @ClassRule
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12JEEInjectionTargetTestServer");

    @Override
    protected ShutDownSharedServer getSharedServer() {
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