/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.cdi.jee.ejbWithJsp.servlet;

import java.util.List;
import java.util.regex.Pattern;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.cdi.jee.ejbWithJsp.ejb.MyManagedBean1;
import com.ibm.ws.cdi.jee.ejbWithJsp.ejb.MySessionBean1;
import com.ibm.ws.cdi.jee.ejbWithJsp.ejb.MySessionBean2;
import com.ibm.ws.cdi.jee.ejbWithJsp.ejb.interceptors.MyAnotherEJBInterceptor;
import com.ibm.ws.cdi.jee.ejbWithJsp.ejb.interceptors.MyEJBInterceptor;
import com.ibm.ws.cdi.jee.ejbWithJsp.ejb.interceptors.MyEJBJARXMLDefinedInterceptor;
import com.ibm.ws.cdi.jee.ejbWithJsp.ejb.interceptors.MyManagedBeanEJBInterceptor;

import componenttest.app.FATServlet;

@WebServlet("/log")
public class LoggerServlet extends FATServlet {

    @Inject
    private BeanManager beanManager;

    private static final long serialVersionUID = 8549700799591343964L;

    @Test
    public void testServletInjectionTargetEvent() throws Exception {

        JEEResourceExtension extension = beanManager.getExtension(JEEResourceExtension.class);

        List<String> messages = extension.getLogger();

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
                       "Session Bean! Injection Target Processed:.*class " + MySessionBean.class.getName(),
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("Session Bean MySessionBean1",
                       messages,
                       "Session Bean! Injection Target Processed:.*class " + MySessionBean1.class.getName(),
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("Session Bean MySessionBean2",
                       messages,
                       "Session Bean! Injection Target Processed:.*class " + MySessionBean2.class.getName(),
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("CDI Bean HelloWorldExtensionBean2",
                       messages,
                       "CDI Bean! Injection Target Processed:.*class " + HelloWorldExtensionBean2.class.getName(),
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("Managed Bean MyManagedBean",
                       messages,
                       "Managed Bean! Injection Target Processed:.*class " + MyManagedBean1.class.getName(),
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("Servlet JEEResourceTestServlet",
                       messages,
                       "Servlet! Injection Target Processed:.*@WebServlet class " + JEEResourceTestServlet.class.getName(),
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("Message Driven Bean MyMessageDrivenBean",
                       messages,
                       "Message Driven Bean! Injection Target Processed:.*class " + MyMessageDrivenBean.class.getName(),
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("Servlet JEEResourceTestServletCtorInjection",
                       messages,
                       "Servlet! Injection Target Processed:.*@WebServlet class " + JEEResourceTestServletCtorInjection.class.getName(),
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("WebSocket Server Endpoint MyServerEndpoint",
                       messages,
                       "Websocket Server Endpoint! Injection Target Processed:.*@ServerEndpoint class " + MyServerEndpoint.class.getName(),
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("JSP Tag Handler JspCdiHitMeTag",
                       messages,
                       "JSP Tag Handler! Injection Target Processed:.*class com.ibm.ws.cdi.jee.ejbWithJsp.jsp.tagHandlers.JspCdiHitMeTag",
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("Servlet Event Listener JspCdiTagLibraryEventListenerCI",
                       messages,
                       "Servlet Event Listener! Injection Target Processed:.*class com.ibm.ws.cdi.jee.ejbWithJsp.jsp.listeners.JspCdiTagLibraryEventListenerCI",
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("JSP Tag Handler MethodInjectionTag",
                       messages,
                       "JSP Tag Handler! Injection Target Processed:.*class com.ibm.ws.cdi.jee.ejbWithJsp.jsp.tagHandlers.MethodInjectionTag",
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("Servlet Event Listener JspCdiTagLibraryEventListenerMI",
                       messages,
                       "Servlet Event Listener! Injection Target Processed:.*class com.ibm.ws.cdi.jee.ejbWithJsp.jsp.listeners.JspCdiTagLibraryEventListenerMI",
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("Servlet SimpleTestServlet",
                       messages,
                       "Servlet! Injection Target Processed:.*@WebServlet class com.ibm.ws.cdi.jee.ejbWithJsp.jsp.SimpleTestServlet",
                       afterTypeDiscovery, afterBeanDiscovery);

        assertPosition("Servlet Event Listener JspCdiTagLibraryEventListenerFI",
                       messages,
                       "Servlet Event Listener! Injection Target Processed:.*class com.ibm.ws.cdi.jee.ejbWithJsp.jsp.listeners.JspCdiTagLibraryEventListenerFI",
                       afterTypeDiscovery, afterBeanDiscovery);
        assertPosition("Non CDI Interceptors",
                       messages,
                       "NonCDIInterceptor1! Injection Target Processed:.*" + MyEJBInterceptor.class.getName(),
                       afterTypeDiscovery, afterBeanDiscovery);
        assertPosition("Non CDI Interceptors",
                       messages,
                       "NonCDIInterceptor2! Injection Target Processed:.*" + MyAnotherEJBInterceptor.class.getName(),
                       afterTypeDiscovery, afterBeanDiscovery);
        assertPosition("Non CDI Interceptors",
                       messages,
                       "NonCDIInterceptor3! Injection Target Processed:.*" + MyManagedBeanEJBInterceptor.class.getName(),
                       afterTypeDiscovery, afterBeanDiscovery);
        assertPosition("Non CDI Interceptor defined in XML",
                       messages,
                       "NON CDI Interceptor defined In ejb-jar.xml! Injection Target Processed:.*" + MyEJBJARXMLDefinedInterceptor.class.getName(),
                       afterTypeDiscovery, afterBeanDiscovery);
    }

    private void assertPosition(String bean, List<String> strings, String regex, int before, int after) {
        int index = index(strings, regex);
        Assert.assertTrue("Process Injection Target Event not found for " + bean + "\n" + debugString(strings), index != -1);
        if (before != -1) {
            Assert.assertTrue("Process Injection Target Event for " + bean + " occurred too soon\n" + debugString(strings), index > before);
        }
        if (after != -1) {
            Assert.assertTrue("Process Injection Target Event for " + bean + " occurred too late\n" + debugString(strings), index < after);
        }
    }

    private static String debugString(List<String> strings) {
        StringBuilder b = new StringBuilder("---------------\n");
        for (String s : strings) {
            b.append(s);
            b.append("\n");
        }
        b.append("---------------");
        return b.toString();
    }

    private int index(List<String> strings, String regex) {
        int i = 0;
        for (String msg : strings) {
            boolean matches = Pattern.matches(regex, msg);
            if (matches) {
                return i;
            }
            i++;
        }
        return -1;
    }
}
