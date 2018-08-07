/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

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
public class CustomerProvidedXMLParserFactoryTest extends LoggingTest {

    @ClassRule
    // Create the server.
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12UserSAXParserFactory");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {
         
         return ShrinkWrap.create(WebArchive.class, "userSAXParserFactory.war")
                        .addPackage("my.parsers")
                        .add(new FileAsset(new File("test-applications/userSAXParserFactory.war/resources/META-INF/services/javax.xml.parsers.SAXParserFactory")), "/META-INF/services/javax.xml.parsers.SAXParserFactory")
                        .add(new FileAsset(new File("test-applications/userSAXParserFactory.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
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
                   SHARED_SERVER.getLibertyServer().findStringsInLogs("CWWKZ0001I.*userSAXParserFactory").size() > 0);
        assertEquals("User's SAXParserFactory impl was used instead of Liberty's", 0,
                     SHARED_SERVER.getLibertyServer().findStringsInLogs("FAILED").size());

    }

}
