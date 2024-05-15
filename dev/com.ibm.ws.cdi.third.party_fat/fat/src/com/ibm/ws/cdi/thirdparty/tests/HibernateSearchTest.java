/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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
import com.ibm.ws.cdi.thirdparty.apps.hibernateSearchWar.model.BasicFieldBridge;
import com.ibm.ws.cdi.thirdparty.apps.hibernateSearchWar.web.HibernateSearchTestServlet;

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

/*

The following error occurs on EE9 because the hibernate jars we are using in this test are built on javax
At the time of writing there are no available hibernate release that is built on jakarta.

Caused by: java.lang.NoClassDefFoundError: javax.persistence.spi.PersistenceProvider
	<java classes>
	at com.ibm.ws.jpa.management.JPAPUnitInfo.createEMFactory(JPAPUnitInfo.java:914)
	at com.ibm.ws.jpa.management.JPAPUnitInfo.initialize(JPAPUnitInfo.java:766)
	at com.ibm.ws.jpa.management.JPAPxmlInfo.extractPersistenceUnits(JPAPxmlInfo.java:182)
	at com.ibm.ws.jpa.management.JPAScopeInfo.processPersistenceUnit(JPAScopeInfo.java:88)
	at com.ibm.ws.jpa.management.JPAApplInfo.addPersistenceUnits(JPAApplInfo.java:119)
	at com.ibm.ws.jpa.container.osgi.internal.JPAComponentImpl.processWebModulePersistenceXml(JPAComponentImpl.java:518)
	at com.ibm.ws.jpa.container.osgi.internal.JPAComponentImpl.applicationStarting(JPAComponentImpl.java:305)
	at com.ibm.ws.container.service.state.internal.ApplicationStateManager.fireStarting(ApplicationStateManager.java:51)

*/

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class HibernateSearchTest extends FATServletClient {

    public static final String HIBERNATE_SEARCH_APP_NAME = "hibernateSearchTest";
    public static final String SERVER_NAME = "cdi20HibernateSearchServer";

    @Server(SERVER_NAME)
    @TestServlet(servlet = HibernateSearchTestServlet.class, contextRoot = HIBERNATE_SEARCH_APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME,
                                                         EERepeatActions.EE10,
                                                         EERepeatActions.EE11,
                                                         EERepeatActions.EE8);

    @BeforeClass
    public static void setUp() throws Exception {

        //To prevent errors on Windows because of file locks, we create a seperate set of transformed jakarta jars rather than transform the existing ones.
        //This must happen before LibertyServerFactory.getLibertyServer as that method is what copies the publish directory into the server.
        if (JakartaEEAction.isEE9OrLaterActive()) {
            List<Path> files = Files.list(Paths.get("publish/shared/resources/hibernatejavax")).collect(Collectors.toList());
            for (Path file : files) {
                File dir = new File("publish/shared/resources/hibernatejakarta/");
                dir.mkdir();
                String newPathString = "publish/shared/resources/hibernatejakarta/" + file.getFileName();
                Path newPath = Paths.get(newPathString);
                JakartaEEAction.transformApp(file, newPath);
            }
            // This will copy our newly transformed library to the server
            LibertyServerFactory.getLibertyServer(SERVER_NAME);
        }

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
