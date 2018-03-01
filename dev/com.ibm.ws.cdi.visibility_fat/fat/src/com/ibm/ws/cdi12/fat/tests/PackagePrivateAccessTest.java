/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

public class PackagePrivateAccessTest extends LoggingTest {

    private static LibertyServer server;

    private static boolean hasSetup = false;

    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return null;
    }

    @BeforeClass
    public static void setUp() throws Exception {

        server = LibertyServerFactory.getStartedLibertyServer("packagePrivateAccessServer");

        if (hasSetup) {
            // Server already set up, app should have already been deployed when the server was started up, make sure the app has started
            assertNotNull("packagePrivateAccessApp started or updated message", server.waitForStringInLog("CWWKZ000[13]I.*packagePrivateAccessApp"));
            return;
        }

        WebArchive packagePrivateAccessApp =  ShrinkWrap.create(WebArchive.class, "packagePrivateAccessApp.war")
                        .addClass("jp.test.RunServlet")
                        .addClass("jp.test.bean.MyBeanHolder")
                        .addClass("jp.test.bean.MyExecutor")
                        .addClass("jp.test.bean.MyBean")
                        .add(new FileAsset(new File("test-applications/packagePrivateAccessApp.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");    
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        ShrinkHelper.exportDropinAppToServer(server, packagePrivateAccessApp);
        assertNotNull("packagePrivateAccessApp started or updated message", server.waitForStringInLogUsingMark("CWWKZ000[13]I.*packagePrivateAccessApp"));
        hasSetup = true;
    }

    @Test
    public void testAppServlet() throws Exception {

        HttpUtils.findStringInUrl(server, "/packagePrivateAccessApp", "PASSED:PASSED");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
