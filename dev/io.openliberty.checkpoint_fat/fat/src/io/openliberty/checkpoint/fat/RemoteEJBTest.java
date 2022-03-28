/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.fat;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfCheckpointNotSupported;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;
import ejbapp1.EJBEvent;
import ejbapp1.RemoteEJBServlet;
import ejbapp1.RemoteInterface;
import ejbapp1.TestObserver;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@SkipIfCheckpointNotSupported
public class RemoteEJBTest extends FATServletClient {

    public static final String SERVER_NAME = "checkpointEJB";
    public static final String REMOTE_EJB_APP_NAME = "ejbapp1";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = ejbapp1.RemoteEJBServlet.class, contextRoot = REMOTE_EJB_APP_NAME)
    })
    public static LibertyServer server;

    @Before
    public void setUp() throws Exception {
        System.out.println("***JTD: setUp ");
        WebArchive ejbMisc = ShrinkWrap.create(WebArchive.class, REMOTE_EJB_APP_NAME + ".war")
                        .addClass(RemoteEJBServlet.class)
                        .addClass(TestObserver.class)
                        .addClass(RemoteInterface.class)
                        .addClass(EJBEvent.class)
                        .addPackages(true, RemoteEJBServlet.class.getPackage())
                        .add(new FileAsset(new File("test-applications/" + REMOTE_EJB_APP_NAME + "/resources/META-INF/permissions.xml")),
                             "/META-INF/permissions.xml")
                        .add(new FileAsset(new File("test-applications/" + REMOTE_EJB_APP_NAME + "/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        ShrinkHelper.exportDropinAppToServer(server, ejbMisc, DeployOptions.SERVER_ONLY);
        server.setCheckpoint(CheckpointPhase.APPLICATIONS, true,
                             server -> {

                                 assertNotNull("'SRVE0169I: Loading Web Module: " + REMOTE_EJB_APP_NAME + "' message not found in log before restore",
                                               server.waitForStringInLogUsingMark("SRVE0169I: .*" + REMOTE_EJB_APP_NAME, 0));
                                 assertNotNull("'CWWKZ0001I: Application " + REMOTE_EJB_APP_NAME + " started' message not found in log.",
                                               server.waitForStringInLogUsingMark("CWWKZ0001I: .*" + REMOTE_EJB_APP_NAME, 0));

                             });
        server.startServer();
    }

    @Test
    public void testAtApplicationsMultiRestore() throws Exception {
        HttpUtils.findStringInUrl(server, REMOTE_EJB_APP_NAME, "Got RemoteEJBServlet");

        server.stopServer(false);
        server.checkpointRestore();
        HttpUtils.findStringInUrl(server, REMOTE_EJB_APP_NAME, "Got RemoteEJBServlet");

        server.stopServer(false);
        server.checkpointRestore();
        HttpUtils.findStringInUrl(server, REMOTE_EJB_APP_NAME, "Got RemoteEJBServlet");
    }

    @AfterClass
    public static void shutdown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
