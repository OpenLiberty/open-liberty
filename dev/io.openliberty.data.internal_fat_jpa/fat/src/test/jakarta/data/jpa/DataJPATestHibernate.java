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
package test.jakarta.data.jpa;

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
import test.jakarta.data.jpa.web.DataJPATestServlet;
import test.jakarta.data.jpa.web.hibernate.DataJPAHibernateServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
@MaximumJavaLevel(javaLevel = 25) // TODO remove once Hibernate upgrades their ByteBuddy dependency to a version that supports java 26+
public class DataJPATestHibernate extends FATServletClient {
    /**
     * Error messages, typically for invalid repository methods, that are
     * intentionally caused by tests to cover error paths.
     * These are ignored when checking the messages.log file for errors.
     */
    static final String[] EXPECTED_ERROR_MESSAGES = //
                    new String[] {
                                   "CWWKD1026E.*allSorted",
                                   "CWWKD1046E.*publicDebtAsBigInteger",
                                   "CWWKD1046E.*publicDebtAsByte",
                                   "CWWKD1046E.*publicDebtAsDouble",
                                   "CWWKD1046E.*publicDebtAsFloat",
                                   "CWWKD1046E.*publicDebtAsInt",
                                   "CWWKD1046E.*publicDebtAsLong",
                                   "CWWKD1046E.*publicDebtAsShort",
                                   "CWWKD1046E.*numFullTimeWorkersAsByte",
                                   "CWWKD1046E.*numFullTimeWorkersAsDouble",
                                   "CWWKD1046E.*numFullTimeWorkersAsFloat",
                                   "CWWKD1046E.*numFullTimeWorkersAsShort",
                                   "CWWKD1054E.*findByIsControlTrueAndNumericValueBetween",
                                   "CWWKD1075E.*Apartment2",
                                   "CWWKD1075E.*Apartment3",
                                   "CWWKD1091E.*countBySurgePriceGreaterThanEqual",
                                   // work around to prevent bad behavior from EclipseLink (see #30575)
                                   "CWWKD1103E.*findBankAccountsByFilingStatus"

                    };

    // TODO update tests to run in database rotation
//    @ClassRule
//    public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.createLatest();

    @ClassRule
    public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.createType(DatabaseContainerType.DerbyJava17Plus);

    @Server("io.openliberty.data.internal.fat.jpa.hibernate")
    @TestServlets({
                    @TestServlet(servlet = DataJPATestServlet.class,
                                 contextRoot = "DataJPATestApp"),
                    @TestServlet(servlet = DataJPAHibernateServlet.class,
                                 contextRoot = "DataJPATestApp")
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        FATSuite.standardizeCollation(testContainer);

        DatabaseContainerUtil.build(server, testContainer) //
                        .withDriverVariable() //
                        .withDatabaseProperties() //
                        .modify();

        WebArchive war = ShrinkHelper.buildDefaultApp("DataJPATestApp",
                                                      "test.jakarta.data.jpa.web",
                                                      "test.jakarta.data.jpa.web.hibernate");

        ShrinkHelper.exportAppToServer(server, war);

        server.addEnvVar("TEST_HIBERNATE", "true"); //TODO remove once all incompatibilities are resolved

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(EXPECTED_ERROR_MESSAGES);
    }
}
