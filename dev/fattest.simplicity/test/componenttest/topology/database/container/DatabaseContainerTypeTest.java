/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.topology.database.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

public class DatabaseContainerTypeTest {

    @Test
    public void testValueOfContainer() {
        assertEquals(DatabaseContainerType.DB2, //
                     DatabaseContainerType.valueOf(new Db2Container(DockerImageName.parse("icr.io/db2_community/db2:12.1.1.0")
                                     .asCompatibleSubstituteFor("icr.io/db2_community/db2"))));
        assertEquals(DatabaseContainerType.Derby, //
                     DatabaseContainerType.valueOf(new DerbyNoopContainer()));
        assertEquals(DatabaseContainerType.DerbyClient, //
                     DatabaseContainerType.valueOf(new DerbyClientContainer()));
        assertEquals(DatabaseContainerType.Oracle, //
                     DatabaseContainerType.valueOf(new OracleContainer(DockerImageName.parse("ghcr.io/gvenzl/oracle-free:23-full-faststart")
                                     .asCompatibleSubstituteFor("gvenzl/oracle-free"))));
        assertEquals(DatabaseContainerType.Postgres, //
                     DatabaseContainerType.valueOf(new PostgreSQLContainer(DockerImageName.parse("public.ecr.aws/docker/library/postgres:17-alpine")
                                     .asCompatibleSubstituteFor("postgres"))));
        assertEquals(DatabaseContainerType.SQLServer,
                     DatabaseContainerType.valueOf(new MSSQLServerContainer<>(DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest")
                                     .asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server"))));
    }

    @Test
    public void testValueOfNative() {
        assertEquals(DatabaseContainerType.DB2, DatabaseContainerType.valueOf("DB2"));
        assertEquals(DatabaseContainerType.Derby, DatabaseContainerType.valueOf("Derby"));
        assertEquals(DatabaseContainerType.DerbyClient, DatabaseContainerType.valueOf("DerbyClient"));
        assertEquals(DatabaseContainerType.Oracle, DatabaseContainerType.valueOf("Oracle"));
        assertEquals(DatabaseContainerType.Postgres, DatabaseContainerType.valueOf("Postgres"));
        assertEquals(DatabaseContainerType.SQLServer, DatabaseContainerType.valueOf("SQLServer"));
    }

    @Test
    public void testValueOfNativeException() {
        try {
            DatabaseContainerType.valueOf("DerbyEmbedded");
            fail("Should not have been able to find type based on alias: DerbyEmbedded");
        } catch (IllegalArgumentException e) {
            //expected
        }

        try {
            DatabaseContainerType.valueOf("OracleDB");
            fail("Should not have been able to find type based on alias: OracleDB");
        } catch (IllegalArgumentException e) {
            //expected
        }

        try {
            DatabaseContainerType.valueOf("Postgre");
            fail("Should not have been able to find type based on alias: Postgre");
        } catch (IllegalArgumentException e) {
            //expected
        }

        try {
            DatabaseContainerType.valueOf("PostgreSQL");
            fail("Should not have been able to find type based on alias: PostgreSQL");
        } catch (IllegalArgumentException e) {
            //expected
        }

        try {
            DatabaseContainerType.valueOf("MSSQLServer");
            fail("Should not have been able to find type based on alias: MSSQLServer");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }

    @Test
    public void testValueOfAlias() {
        assertEquals(DatabaseContainerType.DB2, DatabaseContainerType.valueOfAlias("db2"));
        assertEquals(DatabaseContainerType.Derby, DatabaseContainerType.valueOfAlias("DerbyEmbedded"));
        assertEquals(DatabaseContainerType.DerbyClient, DatabaseContainerType.valueOfAlias("Derbyclient"));
        assertEquals(DatabaseContainerType.Oracle, DatabaseContainerType.valueOfAlias("OracleDB"));
        assertEquals(DatabaseContainerType.Postgres, DatabaseContainerType.valueOfAlias("Postgre"));
        assertEquals(DatabaseContainerType.Postgres, DatabaseContainerType.valueOfAlias("PostgreSQL"));
        assertEquals(DatabaseContainerType.SQLServer, DatabaseContainerType.valueOfAlias("MSSQLServer"));
    }

    @Test
    public void testValueOfAliasException() {
        try {
            DatabaseContainerType.valueOf("db23");
            fail("Should not have been able to find type based on alias: db23");
        } catch (IllegalArgumentException e) {
            //expected
        }

        try {
            DatabaseContainerType.valueOf("derbyEmbed");
            fail("Should not have been able to find type based on alias: derbyEmbed");
        } catch (IllegalArgumentException e) {
            //expected
        }

        try {
            DatabaseContainerType.valueOf("DerbyC");
            fail("Should not have been able to find type based on alias: DerbyC");
        } catch (IllegalArgumentException e) {
            //expected
        }

        try {
            DatabaseContainerType.valueOf("Ora");
            fail("Should not have been able to find type based on alias: Ora");
        } catch (IllegalArgumentException e) {
            //expected
        }

        try {
            DatabaseContainerType.valueOf("postgresql3");
            fail("Should not have been able to find type based on alias: postgresql3");
        } catch (IllegalArgumentException e) {
            //expected
        }

        try {
            DatabaseContainerType.valueOf("mssqlservers");
            fail("Should not have been able to find type based on alias: mssqlservers");
        } catch (IllegalArgumentException e) {
            //expected
        }
    }
}
