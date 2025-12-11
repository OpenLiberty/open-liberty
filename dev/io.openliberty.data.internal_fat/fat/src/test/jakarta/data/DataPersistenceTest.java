/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jakarta.data.inmemory.web.ProviderTestServlet;
import test.jakarta.data.web.DataTestServlet;
import test.jakarta.data.web.hibernate.DataHibernateServlet;

/**
 * Runs the tests with a third-party Persistence provider (Hibernate)
 * instead of the built-in Persistence provider (EclipseLink).
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
@MaximumJavaLevel(javaLevel = 25) // TODO remove once Hibernate upgrades their ByteBuddy dependency to a version that supports java 26+
public class DataPersistenceTest extends FATServletClient {
    /**
     * Error messages, typically for invalid repository methods, that are
     * intentionally caused by tests to cover error paths.
     * These are ignored when checking the messages.log file for errors.
     */
    static final String[] EXPECTED_ERROR_MESSAGES = //
                    new String[] {
                                   "CWWKD1075E.*Apartment2",
                                   "CWWKD1075E.*Apartment3",
                                   // work around to prevent bad behavior from EclipseLink (see #30575)
                                   "CWWKD1103E.*romanNumeralSymbolsAsListOfArrayList",
                                   // work around to prevent bad behavior from EclipseLink (see #30575)
                                   "CWWKD1103E.*romanNumeralSymbolsAsSetOfArrayList",
                                   "CWWKD1119E.*minNumberOfEachNameLength", // cannot infer count for GROUP BY
                                   "CWWJP9991W.*DatabaseException", // various intentional error paths
                                   // TODO the following works around warnings from EclipseLink:
                                   // W CWWJP9991W: [eclipselink.weaver] Weaver encountered an exception
                                   // while trying to weave class test.jakarta.data.web.Participant$Name.
                                   // The exception was: Cannot invoke "String.hashCode()" because "value"
                                   // is null
                                   // W CWWJP9991W: [eclipselink.weaver] Weaver encountered an exception
                                   // while trying to weave class test.jakarta.data.web.Receipt.
                                   // The exception was: Cannot invoke "String.hashCode()" because "value"
                                   // is null
                                   "CWWJP9991W.*Participant",
                                   "CWWJP9991W.*Receipt"
                    };

    //TODO enable database rotation
//    @ClassRule
//    public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.createLatest();

    @ClassRule
    public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.createType(DatabaseContainerType.DerbyJava17Plus);

    @Server("io.openliberty.data.internal.fat.persistence")
    @TestServlets({ @TestServlet(servlet = DataTestServlet.class,
                                 contextRoot = "DataPersistenceApp"),
                    @TestServlet(servlet = ProviderTestServlet.class,
                                 contextRoot = "ProviderTestApp"),
                    @TestServlet(servlet = DataHibernateServlet.class,
                                 contextRoot = "DataHibernateServlet") })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        DatabaseContainerUtil.build(server, testContainer)
                        .withDatabaseProperties()
                        .withDriverVariable()
                        .modify();

        WebArchive war = ShrinkHelper.buildDefaultApp("DataPersistenceApp",
                                                      "test.jakarta.data.web",
                                                      "test.jakarta.data.web.hibernate");
        ShrinkHelper.exportAppToServer(server, war);

        JavaArchive providerJar = ShrinkWrap.create(JavaArchive.class,
                                                    "palindrome-data-provider.jar")
                        .addPackage("test.jakarta.data.inmemory.provider")
                        .addAsServiceProvider(BuildCompatibleExtension.class.getName(),
                                              "test.jakarta.data.inmemory.provider.CompositeBuildCompatibleExtension")
                        .addAsServiceProvider(Extension.class.getName(),
                                              "test.jakarta.data.inmemory.provider.PalindromeExtension");

        WebArchive providerWar = ShrinkHelper.buildDefaultApp("ProviderTestApp",
                                                              "test.jakarta.data.inmemory.web")
                        .addAsLibrary(providerJar);
        ShrinkHelper.exportAppToServer(server, providerWar);

        server.addEnvVar("TEST_HIBERNATE", "true"); //TODO remove once all incompatibilities are resolved

        server.startServerAndValidate(LibertyServer.DEFAULT_PRE_CLEAN,
                                      LibertyServer.DEFAULT_CLEANSTART,
                                      true);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(EXPECTED_ERROR_MESSAGES);
    }
}
