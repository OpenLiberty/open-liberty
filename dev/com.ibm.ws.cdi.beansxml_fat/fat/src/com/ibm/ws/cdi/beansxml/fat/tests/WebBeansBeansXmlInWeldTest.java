/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
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
package com.ibm.ws.cdi.beansxml.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.beansxml.fat.apps.webBeansBeansXmlDecorators.DecoratorsTestServlet;
import com.ibm.ws.cdi.beansxml.fat.apps.webBeansBeansXmlInterceptors.InterceptorsTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WebBeansBeansXmlInWeldTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12WebBeansBeansXmlServer";

    public static final String INTERCEPTORS_APP_NAME = "webBeansBeansXmlInterceptors";
    public static final String DECORATORS_APP_NAME = "webBeansBeansXmlDecorators";

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE9, EERepeatActions.EE10, EERepeatActions.EE7); //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = InterceptorsTestServlet.class, contextRoot = INTERCEPTORS_APP_NAME),
                    @TestServlet(servlet = DecoratorsTestServlet.class, contextRoot = DECORATORS_APP_NAME) }) //FULL
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive webBeansBeansXmlInterceptors = ShrinkWrap.create(WebArchive.class, INTERCEPTORS_APP_NAME + ".war");
        webBeansBeansXmlInterceptors.addPackage(InterceptorsTestServlet.class.getPackage());
        CDIArchiveHelper.addBeansXML(webBeansBeansXmlInterceptors, InterceptorsTestServlet.class);

        EnterpriseArchive webBeansBeansXmlInterceptorsEar = ShrinkWrap.create(EnterpriseArchive.class,
                                                                              INTERCEPTORS_APP_NAME + ".ear");
        webBeansBeansXmlInterceptorsEar.setApplicationXML(InterceptorsTestServlet.class.getPackage(), "application.xml");
        webBeansBeansXmlInterceptorsEar.addAsModule(webBeansBeansXmlInterceptors);

        WebArchive webBeansBeansXmlDecorators = ShrinkWrap.create(WebArchive.class,
                                                                  DECORATORS_APP_NAME + ".war");
        webBeansBeansXmlDecorators.addPackage(DecoratorsTestServlet.class.getPackage());
        CDIArchiveHelper.addBeansXML(webBeansBeansXmlDecorators, DecoratorsTestServlet.class);

        EnterpriseArchive webBeansBeansXmlDecoratorsEar = ShrinkWrap.create(EnterpriseArchive.class,
                                                                            DECORATORS_APP_NAME + ".ear");
        webBeansBeansXmlDecoratorsEar.setApplicationXML(DecoratorsTestServlet.class.getPackage(), "application.xml");
        webBeansBeansXmlDecoratorsEar.addAsModule(webBeansBeansXmlDecorators);

        ShrinkHelper.exportDropinAppToServer(server, webBeansBeansXmlInterceptorsEar, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(server, webBeansBeansXmlDecoratorsEar, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    /**
     * Stop the server.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {

        /*
         * Ignore following exception as those are expected:
         * W WELD-001208: Error when validating
         * wsjar:file:/C:/workspaces/KateCDOpenStreamLiberty/build.image/wlp/usr/servers/cdi12WebBeansBeansXmlServer/workarea/org.eclipse.osgi
         * /65/data/cache/com.ibm.ws.app.manager_13/.cache/webBeansBeansXmlDecorators.war!/WEB-INF/beans.xml@4 against xsd. cvc-elt.1: Cannot find the declaration of element
         * 'WebBeans'.
         * W WELD-001208: Error when validating
         * wsjar:file:/C:/workspaces/KateCDOpenStreamLiberty/build.image/wlp/usr/servers/cdi12WebBeansBeansXmlServer/workarea/org.eclipse.osgi
         * /65/data/cache/com.ibm.ws.app.manager_12/.cache/webBeansBeansXmlInterceptors.war!/WEB-INF/beans.xml@4 against xsd. cvc-elt.1: Cannot find the declaration of element
         * 'WebBeans'.
         *
         * The following exception has been seen but as long as the test passes
         * then we are happy that the application did manage to start eventually
         * so we will also ignore the following exception:
         * CWWKZ0022W: Application webBeansBeansXmlInterceptors has not started in 30.001 seconds.
         */
        server.stopServer("WELD-001208", "CWWKZ0022W");

    }
}
