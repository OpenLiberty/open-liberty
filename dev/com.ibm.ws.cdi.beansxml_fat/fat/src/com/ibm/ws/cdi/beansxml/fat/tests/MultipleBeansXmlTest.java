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
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.beansxml.fat.apps.multipleBeansXml.MultipleBeansXmlServlet;

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
public class MultipleBeansXmlTest extends FATServletClient {

    public static final String SERVER_NAME = "cdi12MultipleBeansXmlServer";

    public static final String MULTIPLE_BEANS_APP_NAME = "multipleBeansXml";

    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9, EE7); //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = MultipleBeansXmlServlet.class, contextRoot = MULTIPLE_BEANS_APP_NAME) }) //FULL
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive multipleBeansXml = ShrinkWrap.create(WebArchive.class, "multipleBeansXml.war");
        multipleBeansXml.addClass(com.ibm.ws.cdi.beansxml.fat.apps.multipleBeansXml.MultipleBeansXmlServlet.class);
        multipleBeansXml.addClass(com.ibm.ws.cdi.beansxml.fat.apps.multipleBeansXml.MyBean.class);
        multipleBeansXml.add(EmptyAsset.INSTANCE, "/WEB-INF/classes/META-INF/beans.xml");
        multipleBeansXml.add(EmptyAsset.INSTANCE, "/WEB-INF/beans.xml");

        ShrinkHelper.exportDropinAppToServer(server, multipleBeansXml, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @Test
    public void testMultipleBeansXmlWarningMessage() throws Exception {
        Assert.assertFalse("Test for extension loaded",
                           server
                                 .findStringsInLogs("CWOWB1001W(?=.*multipleBeansXml#multipleBeansXml.war)(?=.*WEB-INF/beans.xml)(?=.*WEB-INF/classes/META-INF/beans.xml)")
                                 .isEmpty());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWOWB1001W");
    }

}
