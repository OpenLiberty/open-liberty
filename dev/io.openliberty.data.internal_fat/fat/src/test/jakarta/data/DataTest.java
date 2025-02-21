/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
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

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class DataTest extends FATServletClient {
    /**
     * Error messages, typically for invalid repository methods, that are
     * intentionally caused by tests to cover error paths.
     * These are ignored when checking the messages.log file for errors.
     */
    static final String[] EXPECTED_ERROR_MESSAGES = //
                    new String[] {
                                   "CWWKD1006E.*delete3",
                                   "CWWKD1006E.*delete4",
                                   "CWWKD1008E.*delete5",
                                   "CWWKD1028E.*findFirst2147483648",
                                   "CWWKD1041E.*findByNumberIdBetween",
                                   "CWWKD1046E.*minMaxSumCountAverageFloat",
                                   "CWWKD1046E.*singleHexDigit",
                                   "CWWKD1047E.*numberAsByte",
                                   "CWWKD1049E.*countAsBooleanByNumberIdLessThan",
                                   "CWWKD1054E.*deleteByDescription",
                                   "CWWKD1054E.*deleteFirst",
                                   "CWWKD1054E.*findAsLongBetween",
                                   "CWWKD1054E.*findByNumberIdBetween",
                                   "CWWKD1075E.*Apartment2",
                                   "CWWKD1075E.*Apartment3",
                                   // work around to prevent bad behavior from EclipseLink (see #30575)
                                   "CWWKD1103E.*romanNumeralSymbolsAsListOfArrayList",
                                   // work around to prevent bad behavior from EclipseLink (see #30575)
                                   "CWWKD1103E.*romanNumeralSymbolsAsSetOfArrayList"
                    };

    @ClassRule
    public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

    @Server("io.openliberty.data.internal.fat")
    @TestServlets({ @TestServlet(servlet = DataTestServlet.class, contextRoot = "DataTestApp"),
                    @TestServlet(servlet = ProviderTestServlet.class, contextRoot = "ProviderTestApp") })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Get driver type
        DatabaseContainerType type = DatabaseContainerType.valueOf(testContainer);
        server.addEnvVar("DB_DRIVER", type.getDriverName());

        // Set up server DataSource properties
        DatabaseContainerUtil.setupDataSourceDatabaseProperties(server, testContainer);

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

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(EXPECTED_ERROR_MESSAGES);
    }
}
