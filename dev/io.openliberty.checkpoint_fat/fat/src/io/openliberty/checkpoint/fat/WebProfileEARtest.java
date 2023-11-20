/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import ejbEARapp.StatelessBean;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import servletEARapp.ServletEARapp;
import webEARapp.WebEARapp;

@RunWith(FATRunner.class)
@CheckpointTest
public class WebProfileEARtest {

    public static final String EAR_NAME = "webProfileEARapp";
    public static final String EJB_APP_NAME = "ejbEARapp";
    public static final String WAR_SERVLET_APP1_NAME = "servletEARapp";
    public static final String WAR_SERVLET_APP2_NAME = "webEARapp";

    @Server("webProfileEARserver")
    public static LibertyServer server;

    @BeforeClass
    public static void exportWebApp() throws Exception {
        WebArchive webapp1War = ShrinkWrap.create(WebArchive.class, WAR_SERVLET_APP1_NAME + ".war")
                        .addClass(ServletEARapp.class);
        WebArchive webapp2War = ShrinkWrap.create(WebArchive.class, WAR_SERVLET_APP2_NAME + ".war")
                        .addClass(WebEARapp.class);
        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, EJB_APP_NAME + ".jar")
                        .addClass(StatelessBean.class);
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME + ".ear")
                        .setApplicationXML(new File(server.getInstallRoot() + "/usr/servers/" + server.getServerName() + "/application.xml"))
                        .addAsModule(webapp1War)
                        .addAsModule(webapp2War)
                        .addAsModule(ejbJar);
        ShrinkHelper.exportAppToServer(server, ear, DeployOptions.OVERWRITE);
    }

    @Test
    public void testEARdeployment() throws Exception {

        server.startServer();
        HttpUtils.findStringInUrl(server, "webApp1/EARappServlet", "Hello from EJB");
        HttpUtils.findStringInUrl(server, "webApp2/webEARapp", "Hello from webEARapp");
        server.stopServer();

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer();

        server.checkpointRestore();
        HttpUtils.findStringInUrl(server, "webApp1/EARappServlet", "Hello from EJB");
        HttpUtils.findStringInUrl(server, "webApp2/webEARapp", "Hello from webEARapp");
    }

    @After
    public void stopServer() throws Exception {
        server.stopServer();
    }

}
