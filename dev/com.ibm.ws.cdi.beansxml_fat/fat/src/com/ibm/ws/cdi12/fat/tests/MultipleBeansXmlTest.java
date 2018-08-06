/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Assert;
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

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class MultipleBeansXmlTest extends LoggingTest {

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12MultipleBeansXmlServer");

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {
        return ShrinkWrap.create(WebArchive.class, "multipleBeansXml.war")
                        .addClass("com.ibm.ws.cdi12.multipleBeansXml.MultipleBeansXmlServlet")
                        .addClass("com.ibm.ws.cdi12.multipleBeansXml.MyBean")
                        .add(new FileAsset(new File("test-applications/multipleBeansXml.war/resources/WEB-INF/classes/META-INF/beans.xml")), "/WEB-INF/classes/META-INF/beans.xml")
                        .add(new FileAsset(new File("test-applications/multipleBeansXml.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testMultipleBeansXml() throws Exception {
        //part of multiModuleApp1
        this.verifyResponse(
                            "/multipleBeansXml/",
                            "MyBean");
    }

    @Test
    public void testMultipleBeansXmlWarningMessage() throws Exception {
        Assert.assertFalse("Test for extension loaded",
                           SHARED_SERVER.getLibertyServer().findStringsInLogs("CWOWB1001W(?=.*multipleBeansXml#multipleBeansXml.war)(?=.*WEB-INF/beans.xml)(?=.*WEB-INF/classes/META-INF/beans.xml)").isEmpty());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (SHARED_SERVER != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            //Expected warning about multiple beans.xml files.
            SHARED_SERVER.getLibertyServer().stopServer("CWOWB1001W");
        }
    }

}
