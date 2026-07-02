/*******************************************************************************
 * Copyright (c) 2019, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.thirdparty.tests;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.cdi.thirdparty.apps.hibernateSearchWar.model.BasicFieldBridge;
import com.ibm.ws.cdi.thirdparty.apps.hibernateSearchWar.web.HibernateSearchTestServlet;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@MaximumJavaLevel(javaLevel = 25) //The version of bytebuddy used in the javax test app does not support java26
public class HibernateSearchTestJavax extends FATServletClient {

    public static final String HIBERNATE_SEARCH_APP_NAME = "hibernateSearchTest";
    public static final String SERVER_NAME = "cdi20HibernateSearchServer";

    @Server(SERVER_NAME)
    @TestServlet(servlet = HibernateSearchTestServlet.class, contextRoot = HIBERNATE_SEARCH_APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME,
                                                         EERepeatActions.EE8);

    @BeforeClass
    public static void setUp() throws Exception {

        //We used to use /tmp here, but we can't do that on some platforms which we build and test on
        //So we load the persistence.xml file, and give it a path from the server.
        String persistenceXML = readFile(server.getServerRoot() + "/persistence.xml", StandardCharsets.UTF_8);
        persistenceXML = persistenceXML.replaceAll("INDEX-PATH", server.getServerRoot() + "/lucene/indexes");

        //Hibernate Search Test
        WebArchive hibernateSearchTest = ShrinkWrap.create(WebArchive.class, HIBERNATE_SEARCH_APP_NAME + ".war")
                                                   .addPackages(true, BasicFieldBridge.class.getPackage())
                                                   .addPackages(true, HibernateSearchTestServlet.class.getPackage())
                                                   .addAsResource(new StringAsset(persistenceXML), "META-INF/persistence.xml")
                                                   .addAsResource("com/ibm/ws/cdi/thirdparty/apps/hibernateSearchWar/jpaorm.xml", "META-INF/jpaorm.xml");

        ShrinkHelper.exportAppToServer(server, hibernateSearchTest, DeployOptions.SERVER_ONLY);

        //Update the server.xml file to point to the new jakarta jars. This must be done after LibertyServerFactory.getLibertyServer() as the xml files in publish are unchanged.
        if (JakartaEEAction.isEE9OrLaterActive()) {
            server.swapInServerXMLFromPublish("jakarta.xml");
        }

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    private static String readFile(String path, Charset encoding) throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
