/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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
package componenttest.topology.database.container;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;

/**
 * This is a factory class that creates database test-containers.
 * The test container returned will be based on the {fat.bucket.db.type}
 * system property. </br>
 *
 * The {fat.bucket.db.type} property is set to different databases
 * by our test infrastructure when a fat-suite is enlisted in
 * database rotation by setting 'databaseRotation' on the tested.features property in bnd.bnd.</br>
 *
 * <br> Container Information: <br>
 * Derby: Uses a derby no-op test container <br>
 * DerbyClient: Uses a derby no-op test container <br>
 * DB2: Uses <a href="https://hub.docker.com/repository/docker/kyleaure/db2">Custom DB2 container</a> <br>
 * Oracle: Uses <a href="https://github.com/gvenzl/oci-oracle-free/pkgs/container/oracle-free">Offical Oracle container</a> <br>
 * Postgres: Uses <a href="https://gallery.ecr.aws/docker/library/postgres">Offical Postgres Container</a> <br>
 * MS SQL Server: Uses <a href="https://hub.docker.com/_/microsoft-mssql-server">Offical Microsoft SQL Container</a> <br>
 *
 * @see DatabaseContainerType
 */
public class DatabaseContainerFactory {
    private static final Class<DatabaseContainerFactory> c = DatabaseContainerFactory.class;

    // Features in fat-metadata.json are transformed to lowercase by default
    private static final String databaseRotationTestFeature = "databaserotation";

    private static final String databaseRotationDatabaseType = "fat.bucket.db.type";

    /**
     * Used for <b>database rotation testing</b>.
     *
     * Reads the {fat.bucket.db.type} system property and
     * returns a container based on that property.
     * [Postgres, DB2, Oracle, SQLServer, Derby]
     *
     * If {fat.bucket.db.type} is not set with a value,
     * default to Derby Embedded.
     *
     * @return                          JdbcDatabaseContainer - The test container.
     *
     * @throws IllegalArgumentException - if databaseRotation is not set on tested.features,
     *                                      or database type {fat.bucket.db.type} is unsupported.
     */
    public static JdbcDatabaseContainer<?> create() throws IllegalArgumentException {
        return create(DatabaseContainerType.Derby);
    }

    /**
     * @see #create()
     *
     *      This method let's you specify the default database type if one is not provided.
     *      This should mainly be used if you want to use derby client instead of derby embedded as your default.
     */
    public static JdbcDatabaseContainer<?> create(DatabaseContainerType defaultType) throws IllegalArgumentException {
        Path testedFeatures = new File("fat-metadata.json").toPath();
        String dbProperty = System.getProperty(databaseRotationDatabaseType, defaultType.name());

        boolean validateDatabaseRotationFeature;
        try {
            validateDatabaseRotationFeature = Files.lines(testedFeatures)
                            .filter(line -> line.contains(databaseRotationTestFeature))
                            .count() > 0;
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to validate tested features", e);
        }

        Log.info(c, "create", "fat-metadata.json: contains databaseRoation " + validateDatabaseRotationFeature);
        Log.info(c, "create", "System property: fat.bucket.db.type is " + dbProperty);

        if (!validateDatabaseRotationFeature) {
            throw new IllegalArgumentException("To use a generic database, the FAT must be opted into database rotation by setting 'tested.features: " //
                                               + databaseRotationTestFeature + "' in the FAT project's bnd.bnd file");
        }

        DatabaseContainerType type = null;
        try {
            type = DatabaseContainerType.valueOfAlias(dbProperty);
            Log.info(c, "create", "FOUND: database test-container type: " + type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("No database test-container supported for " + dbProperty, e);
        }

        return initContainer(type);
    }

    public static JdbcDatabaseContainer<?> createType(DatabaseContainerType type) throws IllegalArgumentException {
        Log.info(c, "createType", "Database Container Type is " + type);

        return initContainer(type);
    }

    //Private Method: used to initialize test container.
    private static JdbcDatabaseContainer<?> initContainer(DatabaseContainerType dbContainerType) {
        //Check to see if JDBC Driver is available.
        isJdbcDriverAvailable(dbContainerType);

        //Create container
        JdbcDatabaseContainer<?> cont = null;
        Class<?> clazz = dbContainerType.getContainerClass();

        try {
            cont = (JdbcDatabaseContainer<?>) clazz.getConstructor(DockerImageName.class).newInstance(dbContainerType.getImageName());

            switch (dbContainerType) {
                case DB2:
                    Db2Container db2 = dbContainerType.cast(cont);

                    //Accept License agreement
                    db2.acceptLicense();
                    //Add startup timeout since DB2 tends to take longer than the default 3 minutes on build machines.
                    // TODO figure out if there is a way to create a 'fast-start' image that has the database already created.
                    db2.withStartupTimeout(getContainerTimeout(5, 35));

                    break;
                case Derby:
                    break;
                case DerbyClient:
                    break;
                case Oracle:
                    OracleContainer oracle = dbContainerType.cast(cont);

                    //Keep behavior the same as we did before by using a SID instead of pluggable db
                    oracle.usingSid();
                    //Add startup timeout since Oracle tends to take longer than the default 3 minutes on build machines.
                    oracle.withStartupTimeout(getContainerTimeout(3, 25));

                    break;
                case Postgres:
                    //This allows postgres by default to participate in XA transactions (2PC).
                    //Documentation on the Prepare Transaction action in postgres: https://www.postgresql.org/docs/9.3/sql-prepare-transaction.html

                    //If a test is failing that is using XA connections check to see if postgres is failing due to:
                    // ERROR: prepared transaction with identifier "???" does not exist STATEMENT: ROLLBACK PREPARED '???'
                    // then this value may need to be increased.
                    PostgreSQLContainer postgre = dbContainerType.cast(cont);

                    postgre.withCommand("postgres -c max_prepared_transactions=5");

                    break;
                case SQLServer:
                    MSSQLServerContainer<?> sqlserver = dbContainerType.cast(cont);

                    //Accept license agreement
                    sqlserver.acceptLicense();

                    //Init Script
                    sqlserver.withInitScript("init-sqlserver.sql");

                    break;
                default:
                    break;
            }

            //Allow each container to log to output.txt
            cont.withLogConsumer(dbContainerType::log);

        } catch (Exception e) {
            throw new RuntimeException("Unable to create a " + dbContainerType.name() + " TestContainer instance.", e);
        }

        return cont;
    }

    /**
     * Check to see if the JDBC driver necessary for this test-container is in the location
     * where the server expects to find it. <br>
     *
     * JDBC drivers are not publicly available for some databases. In those cases the
     * driver will need to be provided by the user to run this test-container.
     *
     * @return boolean - true if and only if driver exists. Otherwise, false.
     */
    private static boolean isJdbcDriverAvailable(DatabaseContainerType type) {
        File temp = new File("publish/shared/resources/jdbc/" + type.getDriverName());
        boolean result = temp.exists();

        if (result) {
            Log.info(c, "isJdbcDriverAvailable", "FOUND: " + type + " JDBC driver in location: " + temp.getAbsolutePath());
        } else {
            Log.warning(c, "MISSING: " + type + " JDBC driver not in location: " + temp.getAbsolutePath());
        }

        return result;
    }

    /**
     * Creates a container timeout duration (in minutes) based on where the test is being run.
     *
     * @param  fastTimeout - For fast systems: typically your local system
     * @param  slowTimeout - For slow systems: typically our build systems
     *
     * @return             The timeout duration
     */
    private static Duration getContainerTimeout(int fastTimeout, int slowTimeout) {
        boolean isFast = FATRunner.FAT_TEST_LOCALRUN && !FATRunner.ARM_ARCHITECTURE;
        Duration result = Duration.ofMinutes(isFast ? fastTimeout : slowTimeout);
        Log.info(c, "getContainerTimeout", "Returning container timeout of " + result.toMinutes() + " minutes, because"
                                           + " FAT_TEST_LOCALRUN = " + FATRunner.FAT_TEST_LOCALRUN + " and"
                                           + " ARM_ARCHITECTURE = " + FATRunner.ARM_ARCHITECTURE);
        return result;
    }
}
