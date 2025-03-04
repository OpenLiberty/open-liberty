/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
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
package com.ibm.ws.jdbc.fat.derby;

import java.io.File;

import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.Library;
import com.ibm.websphere.simplicity.config.Path;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.fat.derby.web.JDBCDerbyServlet;

@RunWith(FATRunner.class)
public class JDBCDerbyPathToFolderLibTest extends FATServletClient {

    @Server(FATSuite.SERVER)
    @TestServlet(servlet = JDBCDerbyServlet.class, contextRoot = FATSuite.jdbcapp)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Library> libraries = config.getLibraries();
        Library library = libraries.get(0);

        // switch from using filesets to using path config to a folder and jar
        library.getFilesets().clear();

        // setting up a JavaArchive with derby so we can extract it to a folder
        RemoteFile derbyFile = server.getFileFromLibertySharedDir("resources/derby/derby.jar");
        JavaArchive derby = ShrinkWrap.create(JavaArchive.class, "derby.jar");
        derby.merge(ShrinkWrap.create(GenericArchive.class)).as(ZipImporter.class).importFrom(new File(derbyFile.getAbsolutePath())).as(GenericArchive.class);
        setupLibraryFolder(derby);

        // use a path to a folder with the derby driver
        ConfigElementList<Path> paths = library.getPaths();
        Path derbyPath = new Path();
        derbyPath.setName("derby/derby");
        paths.add(derbyPath);

        Path trandriverPath = new Path();
        trandriverPath.setName("${shared.resource.dir}/derby/trandriver.jar");
        paths.add(trandriverPath);

        server.updateServerConfiguration(config);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKE0701E", //expected by testTransactionalSetting
                          "DSRA4011E", //expected by testTNConfigIsoLvlReverse
                          "SRVE0319E"); //expected by testTNConfigTnsl
    }

    private static void setupLibraryFolder(JavaArchive library) throws Exception {
        ShrinkHelper.exportArtifact(library, "publish/libs", true, false, true);
        String libJarName = library.getName();
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), server.getInstallRoot() + "/usr/servers/" + FATSuite.SERVER + "/derby",
                                               libJarName.substring(0, libJarName.length() - 4),
                                               "publish/libs/" + libJarName, true, server.getServerRoot());
    }
}
