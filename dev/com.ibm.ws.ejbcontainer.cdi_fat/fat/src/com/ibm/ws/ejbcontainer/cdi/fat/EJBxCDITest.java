/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.cdi.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.ejbcontainer.cdi.jcdi.web.BeanManagerInjectionServlet;
import com.ibm.ws.ejbcontainer.cdi.jcdi.web.InjectMultiLocalEJBServlet;
import com.ibm.ws.ejbcontainer.cdi.jcdi.web.InterceptorIntegrationServlet;
import com.ibm.ws.ejbcontainer.cdi.jcdi.web.ResourceServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class EJBxCDITest extends FATServletClient {

    @Server("com.ibm.ws.ejbcontainer.cdi.fat.EJB-CDI-Server")
    @TestServlets({ @TestServlet(servlet = BeanManagerInjectionServlet.class, contextRoot = "EJB31JCDIWeb"),
                    @TestServlet(servlet = InjectMultiLocalEJBServlet.class, contextRoot = "EJB31JCDIWeb"),
                    @TestServlet(servlet = InterceptorIntegrationServlet.class, contextRoot = "EJB31JCDIWeb"),
                    @TestServlet(servlet = ResourceServlet.class, contextRoot = "EJB31JCDIWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.ejbcontainer.cdi.fat.EJB-CDI-Server")).andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.cdi.fat.EJB-CDI-Server")).andWith(new JakartaEE9Action().forServers("com.ibm.ws.ejbcontainer.cdi.fat.EJB-CDI-Server"));

    @BeforeClass
    public static void setUp() throws Exception {

        //Use ShrinkHelper to build the Ear
        JavaArchive EJB31JCDIBeanjar = ShrinkHelper.buildJavaArchive("EJB31JCDIBean.jar", "com.ibm.ws.ejbcontainer.cdi.jcdi.ejb.");
        JavaArchive EJB31InterceptorJCDIBean = ShrinkHelper.buildJavaArchive("EJB31InterceptorJCDIBean.jar", "com.ibm.ws.ejbcontainer.cdi.jcdi.ejb_int.");
        JavaArchive EJB31NonJCDIBean = ShrinkHelper.buildJavaArchive("EJB31NonJCDIBean.jar", "com.ibm.ws.ejbcontainer.cdi.jcdi.ejb2.");
        WebArchive EJB31JCDIWeb = ShrinkHelper.buildDefaultApp("EJB31JCDIWeb.war", "com.ibm.ws.ejbcontainer.cdi.jcdi.web.");
        EnterpriseArchive EJB31JCDITestApp = ShrinkWrap.create(EnterpriseArchive.class, "EJB31JCDITestApp.ear");
        EJB31JCDITestApp.addAsModule(EJB31JCDIBeanjar).addAsModule(EJB31InterceptorJCDIBean).addAsModule(EJB31NonJCDIBean).addAsModule(EJB31JCDIWeb);
        ShrinkHelper.addDirectory(EJB31JCDITestApp, "test-applications/EJB31JCDITestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, EJB31JCDITestApp, DeployOptions.SERVER_ONLY);

        server.startServer();

    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}
