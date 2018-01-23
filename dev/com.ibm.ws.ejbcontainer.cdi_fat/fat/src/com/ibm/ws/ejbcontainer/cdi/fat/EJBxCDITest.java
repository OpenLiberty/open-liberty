/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
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
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import suite.r80.base.ejb31misc.jcdi.web.BeanManagerInjectionServlet;
import suite.r80.base.ejb31misc.jcdi.web.InjectMultiLocalEJBServlet;
import suite.r80.base.ejb31misc.jcdi.web.InterceptorIntegrationServlet;

@RunWith(FATRunner.class)
public class EJBxCDITest extends FATServletClient {

    @Server("com.ibm.ws.ejbcontainer.cdi.fat.EJB-CDI-Server")
    @TestServlets({ @TestServlet(servlet = BeanManagerInjectionServlet.class, contextRoot = "EJB31JCDIWeb"),
                    @TestServlet(servlet = InjectMultiLocalEJBServlet.class, contextRoot = "EJB31JCDIWeb"),
                    @TestServlet(servlet = InterceptorIntegrationServlet.class, contextRoot = "EJB31JCDIWeb") })
    public static LibertyServer server;

    //TODO: Get repeat tests to work
    //@ClassRule
    //public static RepeatTests r = RepeatTests.withoutModification().andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.cdi.fat.EJB31-CDI10-Server"));

    @BeforeClass
    public static void setUp() throws Exception {

        //Use ShrinkHelper to build the Ear
        JavaArchive EJB31JCDIBeanjar = ShrinkHelper.buildJavaArchive("EJB31JCDIBean.jar", "suite.r80.base.ejb31misc.jcdi.ejb.");
        JavaArchive EJB31InterceptorJCDIBean = ShrinkHelper.buildJavaArchive("EJB31InterceptorJCDIBean.jar", "suite.r80.base.ejb31misc.jcdi.ejb_int.");
        JavaArchive EJB31NonJCDIBean = ShrinkHelper.buildJavaArchive("EJB31NonJCDIBean.jar", "suite.r80.base.ejb31misc.jcdi.ejb2.");
        WebArchive EJB31JCDIWeb = ShrinkHelper.buildDefaultApp("EJB31JCDIWeb.war", "suite.r80.base.ejb31misc.jcdi.web.");
        EnterpriseArchive EJB31JCDITestApp = ShrinkWrap.create(EnterpriseArchive.class, "EJB31JCDITestApp.ear");
        EJB31JCDITestApp.addAsModule(EJB31JCDIBeanjar).addAsModule(EJB31InterceptorJCDIBean).addAsModule(EJB31NonJCDIBean).addAsModule(EJB31JCDIWeb);
        ShrinkHelper.addDirectory(EJB31JCDITestApp, "test-applications/EJB31JCDITestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, EJB31JCDITestApp);

        server.startServer();

    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWNEN0054E");
        }
    }
}
