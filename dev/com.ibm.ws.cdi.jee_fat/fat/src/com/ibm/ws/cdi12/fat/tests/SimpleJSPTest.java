/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
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
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

public class SimpleJSPTest extends LoggingTest {
    public static LibertyServer server;

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("cdi12BasicServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive simpleJSPApp = ShrinkWrap.create(WebArchive.class, "simpleJSPApp.war")
                        .addClass("com.ibm.ws.cdi12.test.jsp.SimpleJspBean")
                        .add(new FileAsset(new File("test-applications/simpleJSPApp.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/simpleJSPApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .add(new FileAsset(new File("test-applications/simpleJSPApp.war/resources/index.jsp")), "/index.jsp");
        server = SHARED_SERVER.getLibertyServer();
        ShrinkHelper.exportDropinAppToServer(server, simpleJSPApp);
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application simpleJSPApp started");
    }

    @Test
    public void testSimpleJSP() throws Exception {
        HttpUtils.findStringInUrl(server, "/simpleJSPApp/", "Test Sucessful!");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
