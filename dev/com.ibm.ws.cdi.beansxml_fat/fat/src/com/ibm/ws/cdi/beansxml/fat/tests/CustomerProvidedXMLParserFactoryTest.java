/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.beansxml.fat.tests;

import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE7_FULL;
import static componenttest.rules.repeater.EERepeatTests.EEVersion.EE9;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.DiscoveryMode;
import com.ibm.ws.cdi.beansxml.fat.apps.userSAXParserFactory.MySAXParserFactory;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatTests;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests the case where a customer provides their own SAX parser factory.
 * It is possible that an application that contains a beans.xml might also
 * package their own implementation of SAXParserFactory. In that case Liberty
 * needs to ensure that it uses a Liberty-supplied parser factory, and not the
 * customer's. If we use the customer's then we run into classloading problems
 * because we have already loaded and use the JDK's version of <code>
 * javax.xml.parsers.SAXParserFactory</code> - if the application provides this
 * same class, we will have a ClassCastException. This test verifies that we
 * can parse the beans.xml file without loading the customer's SAXParserFactory
 * when one is supplied.
 */
@RunWith(FATRunner.class)
public class CustomerProvidedXMLParserFactoryTest {

    public static final String SERVER_NAME = "cdi12UserSAXParserFactory";

    public static final String USER_SAX_PARSER_APP_NAME = "userSAXParserFactory";

    @ClassRule
    public static RepeatTests r = EERepeatTests.with(SERVER_NAME, EE9, EE7_FULL); //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive userSAXParserFactory = ShrinkWrap.create(WebArchive.class, "userSAXParserFactory.war");
        userSAXParserFactory.addClass(MySAXParserFactory.class);
        userSAXParserFactory.addAsServiceProvider(javax.xml.parsers.SAXParserFactory.class, MySAXParserFactory.class);
        CDIArchiveHelper.addBeansXML(userSAXParserFactory, DiscoveryMode.ALL);
        ShrinkHelper.exportDropinAppToServer(server, userSAXParserFactory, DeployOptions.SERVER_ONLY);

        server.startServer(true, false);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Test bean manager can be looked up via java:comp/BeanManager
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testBeansXMLIsParsedWithoutUsingCustomerSAXParserFactory() throws Exception {
        assertTrue("App with custom SAXParserFactory did not start successfully",
                   server.findStringsInLogs("CWWKZ0001I.*userSAXParserFactory").size() > 0);
        assertEquals("User's SAXParserFactory impl was used instead of Liberty's", 0,
                     server.findStringsInLogs("FAILED").size());

    }

}
