/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
package test.jakarta.data;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.spi.Extension;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import test.jakarta.data.inmemory.web.ProviderTestServlet;
import test.jakarta.data.web.DataTestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
@CheckpointTest
public class DataTestCheckpoint extends FATServletClient {
    @ClassRule
    public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

    @Server("io.openliberty.data.internal.checkpoint.fat")
    @TestServlets({ @TestServlet(servlet = DataTestServlet.class, contextRoot = "DataTestApp"),
                    @TestServlet(servlet = ProviderTestServlet.class, contextRoot = "ProviderTestApp") })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Set up server DataSource properties
        DatabaseContainerUtil.setupDataSourcePropertiesForCheckpoint(server, testContainer);

        WebArchive war = ShrinkHelper.buildDefaultApp("DataTestApp", "test.jakarta.data.web");
        ShrinkHelper.exportAppToServer(server, war);

        JavaArchive providerJar = ShrinkWrap.create(JavaArchive.class, "palindrome-data-provider.jar")
                        .addPackage("test.jakarta.data.inmemory.provider")
                        .addAsServiceProvider(BuildCompatibleExtension.class.getName(),
                                              "test.jakarta.data.inmemory.provider.CompositeBuildCompatibleExtension")
                        .addAsServiceProvider(Extension.class.getName(),
                                              "test.jakarta.data.inmemory.provider.PalindromeExtension");

        WebArchive providerWar = ShrinkHelper.buildDefaultApp("ProviderTestApp", "test.jakarta.data.inmemory.web")
                        .addAsLibrary(providerJar);
        ShrinkHelper.exportAppToServer(server, providerWar);

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer();

        //Server started, application started, checkpoint taken, server is now stopped.
        //Configure environment variables according to the test container
        DatabaseContainerUtil.setupDataSourceEnvForCheckpoint(server, testContainer);

        server.checkpointRestore();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
