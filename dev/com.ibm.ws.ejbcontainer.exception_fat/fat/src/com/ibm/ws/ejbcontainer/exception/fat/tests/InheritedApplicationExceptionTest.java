/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.exception.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.ejbcontainer.app_exception.ann.web.InheritedRTExRemoteServerServlet;
import com.ibm.ws.ejbcontainer.app_exception.ann.web.InheritedRTExServlet;
import com.ibm.ws.ejbcontainer.app_exception.ann.web.InheritedThrownExRemoteServerServlet;
import com.ibm.ws.ejbcontainer.app_exception.ann.web.InheritedThrownExServlet;
import com.ibm.ws.ejbcontainer.app_exception.mix.web.XMLOverrideInheritedRTExServlet;
import com.ibm.ws.ejbcontainer.app_exception.xml.web.XMLInheritedRTExServlet;
import com.ibm.ws.ejbcontainer.app_exception.xml.web.XMLInheritedThrownExServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class InheritedApplicationExceptionTest {
    @Server("com.ibm.ws.ejbcontainer.exception.fat.AppExceptionServer")
    @TestServlets({ @TestServlet(servlet = InheritedRTExRemoteServerServlet.class, contextRoot = "EJB31AppExAnnWeb"),
                    @TestServlet(servlet = InheritedRTExServlet.class, contextRoot = "EJB31AppExAnnWeb"),
                    @TestServlet(servlet = InheritedThrownExRemoteServerServlet.class, contextRoot = "EJB31AppExAnnWeb"),
                    @TestServlet(servlet = InheritedThrownExServlet.class, contextRoot = "EJB31AppExAnnWeb"),
                    @TestServlet(servlet = XMLOverrideInheritedRTExServlet.class, contextRoot = "EJB31AppExMixWeb"),
                    @TestServlet(servlet = XMLInheritedRTExServlet.class, contextRoot = "EJB31AppExXmlWeb"),
                    @TestServlet(servlet = XMLInheritedThrownExServlet.class, contextRoot = "EJB31AppExXmlWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.ejbcontainer.exception.fat.AppExceptionServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.exception.fat.AppExceptionServer"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the annotation based ear
        JavaArchive EJB31AppExAnnBean = ShrinkHelper.buildJavaArchive("EJB31AppExAnnBean.jar", "com.ibm.ws.ejbcontainer.app_exception.ann.ejb.");
        WebArchive EJB31AppExAnnWeb = ShrinkHelper.buildDefaultApp("EJB31AppExAnnWeb.war", "com.ibm.ws.ejbcontainer.app_exception.ann.web.");

        EnterpriseArchive EJB31AppExAnnApp = ShrinkWrap.create(EnterpriseArchive.class, "EJB31AppExAnnApp.ear");
        EJB31AppExAnnApp.addAsModule(EJB31AppExAnnBean).addAsModule(EJB31AppExAnnWeb);
        EJB31AppExAnnApp = (EnterpriseArchive) ShrinkHelper.addDirectory(EJB31AppExAnnApp, "test-applications/EJB31AppExAnnApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, EJB31AppExAnnApp);

        // Use ShrinkHelper to build the annotation and xml override based ear
        JavaArchive EJB31AppExMixBean = ShrinkHelper.buildJavaArchive("EJB31AppExMixBean.jar", "com.ibm.ws.ejbcontainer.app_exception.mix.ejb.");
        WebArchive EJB31AppExMixWeb = ShrinkHelper.buildDefaultApp("EJB31AppExMixWeb.war", "com.ibm.ws.ejbcontainer.app_exception.mix.web.");

        EnterpriseArchive EJB31AppExMixApp = ShrinkWrap.create(EnterpriseArchive.class, "EJB31AppExMixApp.ear");
        EJB31AppExMixApp.addAsModule(EJB31AppExMixBean).addAsModule(EJB31AppExMixWeb);

        ShrinkHelper.exportDropinAppToServer(server, EJB31AppExMixApp);

        // Use ShrinkHelper to build the xml based ear
        JavaArchive EJB31AppExXmlBean = ShrinkHelper.buildJavaArchive("EJB31AppExXmlBean.jar", "com.ibm.ws.ejbcontainer.app_exception.xml.ejb.");
        WebArchive EJB31AppExXmlWeb = ShrinkHelper.buildDefaultApp("EJB31AppExXmlWeb.war", "com.ibm.ws.ejbcontainer.app_exception.xml.web.");

        EnterpriseArchive EJB31AppExXmlApp = ShrinkWrap.create(EnterpriseArchive.class, "EJB31AppExXmlApp.ear");
        EJB31AppExXmlApp.addAsModule(EJB31AppExXmlBean).addAsModule(EJB31AppExXmlWeb);

        ShrinkHelper.exportDropinAppToServer(server, EJB31AppExXmlApp);

        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer("CNTR0020E");
    }
}
