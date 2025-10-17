/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
import com.ibm.ws.cdi.thirdparty.apps.hibernateCompatibilityWar.model.TestEntity;
import com.ibm.ws.cdi.thirdparty.apps.hibernateCompatibilityWar.web.HibernateCompatibilityTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
//@Mode(TestMode.FULL)
public class HibernateCDICompatibilityTest extends FATServletClient {

    public static final String HIBERNATE_COMPAT_APP_NAME = "hibernateCompatibilityTest";
    public static final String SERVER_NAME = "cdiHibernateCompatibilityServer";

    @Server(SERVER_NAME)
    @TestServlet(servlet = HibernateCompatibilityTestServlet.class, contextRoot = HIBERNATE_COMPAT_APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME,
                                                         EERepeatActions.EE11);

    @BeforeClass
    public static void setUp() throws Exception {
        // To prevent errors on Windows because of file locks, we create a separate set of transformed jakarta jars
        // This must happen before LibertyServerFactory.getLibertyServer as that method copies the publish directory into the server
        if (JakartaEEAction.isEE9OrLaterActive()) {
            List<Path> files = Files.list(Paths.get("publish/shared/resources/hibernate6Untransformed")).collect(Collectors.toList());
            for (Path file : files) {
                File dir = new File("publish/shared/resources/hibernate6Transformed/");
                dir.mkdir();
                String newPathString = "publish/shared/resources/hibernate6Transformed/" + file.getFileName();
                Path newPath = Paths.get(newPathString);
                JakartaEEAction.transformApp(file, newPath);
            }
            // This will copy our newly transformed library to the server
            LibertyServerFactory.getLibertyServer(SERVER_NAME);
        }

        // Load the persistence.xml file and give it a path from the server
        String persistenceXML = readFile(server.getServerRoot() + "/persistence.xml", StandardCharsets.UTF_8);
        persistenceXML = persistenceXML.replaceAll("INDEX-PATH", server.getServerRoot() + "/lucene/indexes");

        // Create the test application
        WebArchive hibernateCompatTest = ShrinkWrap.create(WebArchive.class, HIBERNATE_COMPAT_APP_NAME + ".war")
                                                   .addPackages(true, TestEntity.class.getPackage())
                                                   .addPackages(true, HibernateCompatibilityTestServlet.class.getPackage())
                                                   .addAsResource(new StringAsset(persistenceXML), "META-INF/persistence.xml")
                                                   .addAsResource("com/ibm/ws/cdi/thirdparty/apps/hibernateCompatibilityWar/jpaorm.xml", "META-INF/jpaorm.xml");

        ShrinkHelper.exportAppToServer(server, hibernateCompatTest, DeployOptions.SERVER_ONLY);

        // Update the server.xml file to point to the new jakarta jars if needed
        if (JakartaEEAction.isEE9OrLaterActive()) {
            server.swapInServerXMLFromPublish("jakarta.xml");
        }

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    private static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
