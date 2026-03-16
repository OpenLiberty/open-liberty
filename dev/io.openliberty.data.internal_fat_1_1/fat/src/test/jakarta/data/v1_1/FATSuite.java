/*******************************************************************************
 * Copyright (c) 2025,2026 IBM Corporation and others.
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
package test.jakarta.data.v1_1;

import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.JdbcDatabaseContainer;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.topology.database.container.DatabaseContainerType;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                Data_1_1_HibernateTest.class,
                Data_1_1_Test.class
})
public class FATSuite extends TestContainerSuite {

    /**
     * Enable case sensitive collation based on binary representation of
     * data to replicate default behavior of other databases
     *
     * @param cont - the container
     */
    public static void standardizeCollation(JdbcDatabaseContainer<?> cont) {
        if (DatabaseContainerType.valueOf(cont) == DatabaseContainerType.SQLServer) {
            cont.withUrlParam("databaseName", "TEST");
            try (Connection con = cont.createConnection(""); Statement stmt = con.createStatement()) {
                stmt.execute("ALTER DATABASE TEST COLLATE Latin1_General_bin");
            } catch (SQLException e) {
                fail("Unable to alter database collation. " + e.getMessage());
            }
        }
    }
}
