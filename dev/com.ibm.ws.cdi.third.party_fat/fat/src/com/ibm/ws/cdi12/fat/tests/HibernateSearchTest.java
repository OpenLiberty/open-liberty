/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE8;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.List;

import org.junit.Assert;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebBrowserFactory;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.rules.repeater.RepeatTests;
import componenttest.rules.repeater.EERepeatTests;
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
@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
public class HibernateSearchTest extends FATServletClient {

    public static final String HIBERNATE_SEARCH_APP_NAME = "hibernateSearchTest";
    public static final String SERVER_NAME = "cdi20HibernateSearchServer";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = cdi.hibernate.test.web.SimpleTestServlet.class, contextRoot = HIBERNATE_SEARCH_APP_NAME) 
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        //Hibernate Search Test
        WebArchive hibernateSearchTest = ShrinkWrap.create(WebArchive.class, HIBERNATE_SEARCH_APP_NAME+".war")
                        .addPackages(true, cdi.hibernate.test.model.BasicFieldBridge.class.getPackage())
                        .addPackages(true, cdi.hibernate.test.web.SimpleTestServlet.class.getPackage())
                        .add(new FileAsset(new File("test-applications/hibernateSearchTest.war/resources/WEB-INF/classes/META-INF/persistence.xml")), "/WEB-INF/classes/META-INF/persistence.xml")
                        .add(new FileAsset(new File("test-applications/hibernateSearchTest.war/resources/WEB-INF/classes/META-INF/jpaorm.xml")), "/WEB-INF/classes/META-INF/jpaorm.xml");

        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        ShrinkHelper.exportAppToServer(server, hibernateSearchTest);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    private String createURL(String path) {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + path;
    }

}
