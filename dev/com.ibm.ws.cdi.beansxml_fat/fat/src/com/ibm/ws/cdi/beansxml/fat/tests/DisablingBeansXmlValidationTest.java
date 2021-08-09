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
package com.ibm.ws.cdi.beansxml.fat.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE7;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE8;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.beansxml.fat.apps.invalidBeansXML.InvalidBeansXMLTestServlet;
import com.ibm.ws.cdi.beansxml.fat.apps.invalidBeansXML.TestBean;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class DisablingBeansXmlValidationTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12NoBeansXmlValidationServer";

    public static final String INVALID_BEANS_XML_APP_NAME = "invalidBeansXml";

    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9, EE8, EE7);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = InvalidBeansXMLTestServlet.class, contextRoot = INVALID_BEANS_XML_APP_NAME) }) //FULL
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive invalidBeansXml = ShrinkWrap.create(WebArchive.class, "invalidBeansXml.war");
        invalidBeansXml.addClass(InvalidBeansXMLTestServlet.class);
        invalidBeansXml.addClass(TestBean.class);
        CDIArchiveHelper.addBeansXML(invalidBeansXml, InvalidBeansXMLTestServlet.class);

        ShrinkHelper.exportDropinAppToServer(server, invalidBeansXml, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
