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

    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return null;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive packagePrivateAccessApp =  ShrinkWrap.create(WebArchive.class, "packagePrivateAccessApp.war")
                        .addClass("jp.test.RunServlet")
                        .addClass("jp.test.bean.MyBeanHolder")
                        .addClass("jp.test.bean.MyExecutor")
                        .addClass("jp.test.bean.MyBean")
                        .add(new FileAsset(new File("test-applications/packagePrivateAccessApp.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");    
        server = LibertyServerFactory.getStartedLibertyServer("packagePrivateAccessServer");
        ShrinkHelper.exportDropinAppToServer(server, packagePrivateAccessApp);
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application packagePrivateAccessApp started");
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
