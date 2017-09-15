/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.database;

import static componenttest.common.apiservices.BootstrapProperty.DB_DRIVERNAME;
import static componenttest.common.apiservices.BootstrapProperty.DB_DRIVERVERSION;
import static componenttest.common.apiservices.BootstrapProperty.DB_VENDORNAME;
import static componenttest.common.apiservices.BootstrapProperty.LIBERTY_DBJARS;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.common.apiservices.Bootstrap;
import componenttest.common.apiservices.BootstrapProperty;

/**
 * <h1>Database Setup and Cleanup</h1>
 *
 * <p>This class provides options for setting up testing for relational databases that are certified by WebSphere.
 *
 * <p>FAT buckets that test with databases setup their server.xml to test with Derby Embedded by default.
 * This class provides options for starting and stopping DerbyNetwork so Derby Client can be used as the
 * default instead of Derby Embedded. None of the other setup options provided by this class are processed
 * at this time for Derby.
 *
 * <p>Fat buckets can run with both Derby and another database specified in the bootstrapping.properties
 * at the same time.
 *
 * <p>For each test bucket run, the database is created, the DDL files are executed
 * against the database, the test bucket is executed, and the database is dropped.
 *
 * <p>There is also a way to create a database or use an existing database and have the database not
 * be dropped at the end of the test run that can be useful for sandbox testing.
 *
 * <p>See the <code>main</code> method javadoc for the options that are provided.
 *
 * <hr>
 * <h2>Property driven setup</h2>
 *
 * <p>Database setup is driven by properties specified on the build project or on the individual project.
 *
 * <p>The build properties have the same names as the bootstrapping.properties.
 * See <code>BootstrapProperty</code> for a list of property names.
 *
 * <p>The FAT infrastructure uses the options provided by this class to set up and tear down
 * the database environment.
 * This class depends on the Ant FAT testing infrastructure populating the bootstrapping.properties file
 * from Ant properties that contain the database connection and jdbc driver information
 * before it is called.
 *
 * <p>See the prereq.dbtest project for property files that contain database
 * connection properties and jdbc driver properties that are available for testing.
 * There is also a "create" properties file to use with sandbox testing that allows for a
 * database name to be specified and the database to be available after the test bucket has completed.
 *
 * <p>There are two types of setups created for use by a test bucket.
 * <ol>
 * <li>Databases access is managed by creating a unique database instance for each bucket.
 * <li>Databases access is managed by creating two unique user ids or schemas in the database
 * for each bucket because creating a unique database for each bucket is too expensive.
 * </ol>
 *
 * <p>As part of normal continuous test, for each test bucket that requires a database, the database or user IDs
 * will be setup and deleted. Unique names are created for each active database run so that each
 * test bucket is isolated from another running test bucket.
 * For Liberty, these names will start with "LIBR" and be followed by four digits, e.g. "LIBR0000".
 *
 * <p>A bootstrapping property, <code>database.dropandcreate</code>, is provided for sandbox testing
 * where the database or schemas can be created and saved after the run or if the database or schemas
 * already exists, then they can be used by the test bucket.
 * This can be useful for debugging, but care must be taken not to overload the database machines
 * with extra databases.
 * <ul>
 * <li>Note 1: There is no way to delete the database or schemas after they have been created using this option.
 * It is up to the tester to take care of cleanup.
 * <li>Note 2: When creating a database this way, the tester must provide the name of the database.
 * Do not use names that start with "MOON" or "LIBR" as these are created as part of
 * continuous test and may be deleted as part of regular clean up of the database machines.
 * <li>Note 3: For Oracle, a pair of user ids are created instead of a database. The same naming
 * rules are used for these names, so names that start with "MOON" or "LIBR" should be
 * avoided.
 * </ul>
 *
 * <hr>
 * <h2>DDL support</h2>
 * <p>The test bucket can provide a set of DDL files to be executed after the database is created.
 * <p>The files should be placed in a "ddl" directory that is under the main project directory.
 * <p>There should be a set of DDL files for each database type under the ddl directory.
 * <p>The directory and sub-directories will all be processed.
 * <p>The file can end in any suffix.
 * <p>For example:
 *
 * <pre>
 * com.ibm.ws.mytestproject_fat
 * &nbsp;&nbsp;ddl
 * &nbsp;&nbsp;&nbsp;&nbsp;db2
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;test1.ddl
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;dir1
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;test2.ddl
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;test3.sql
 * &nbsp;&nbsp;&nbsp;&nbsp;informix
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;test1.ddl
 * &nbsp;&nbsp;&nbsp;&nbsp;oracle
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;test1.ddl
 * &nbsp;&nbsp;&nbsp;&nbsp;sqlserver
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;test1.ddl
 * &nbsp;&nbsp;&nbsp;&nbsp;sybase
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;test1ddl
 * </pre>
 *
 * <hr>
 * <h2>Database setup details</h2>
 * <h3>1. Databases other than Oracle</h3>
 * <ul>
 * <li>The userid/pw pair must be specified in bootstrapping.properties
 * <li>If database name is not specified:
 * <p>Then the next available database name that starts with "LIBR" plus four digits will be determined.
 * <p>The database will be created before the test run starts.
 * <p>The database will be dropped after the test run completes.
 *
 * <li>If database name is specified:
 * <p>If database.dropandcreate = false or is not specified
 * and the database does not already exist then it will be created using the name provided
 * <p>If database.dropandcreate = true
 * <ul>
 * <li>The database will be dropped if it exists
 * <li>The database will be created using the name specified.
 * <li>The userid/pw pairs will be created in the database.
 * <li>The database will not be dropped after the test run completes and can be used for
 * additional runs or for debugging.
 * </ul>
 * </ul>
 *
 * <h3>2. Oracle</h3>
 * <ul>
 * <li>The database name must be specified and exist. This should be the Oracle "service name".
 * <li>If userid/pw pairs are not specified:
 * <p>Then the next available userid/pw pair names that starts with "LIBR" plus four digits will be determined.
 * <p>The userids will be created before the test run starts.
 * <p>The userids will be dropped after the test run completes.
 *
 * <li>If userid/pw pair is specified:
 * <p>If database.dropandcreate = false or is not specified
 * and the userids do not already exist then they will be created using the names provided.
 * <p>If database.dropandcreate = true
 * <ul>
 * <li>The user ids will be dropped if they exist.
 * <li>The user ids will be created using the names specified.
 * <li>The user ids will not be dropped after the test run completes and can be used for
 * additional runs or for debugging.
 * </ul>
 * </ul>
 *
 */
public class DatabaseTester {

    private final static Class<?> c = DatabaseTester.class;

    /**
     * Options for database environment set up and tear down.
     *
     * @param args Set of strings<ul>
     *            <li>[0] bootstrapping file name
     *            <li>[1] option
     *            <br><b>create</b> - create the database and/or user IDs.
     *            <br>&nbsp;&nbsp;&nbsp;The database or user IDs are NOT created if database.dropandcreate=false
     *            and the names are provided.
     *            <br>
     *            <br><b>drop</b> - drop the database or user IDs.
     *            <br>&nbsp;&nbsp;&nbsp;The database or user IDs are NOT dropped if database.keepdatabase was added to the
     *            bootstrapping.properties file during the create option. It is then the tester's responsibility
     *            to drop the database or user IDs when they are no longer needed.
     *            <br>
     *            <br><b>runDDL</b> - execute all DDL files in the ddl directory for the database type
     *            <br>
     *            <br><b>startDerbyNet</b> - start Derby Network Server
     *            <br>
     *            <br><b>stopDerbyNet</b> - stop Derby Network Server
     *            <li>[2] test bucket path name
     *            <li>[3] (optional) database properties/jars directory (i.e. /path/to/prereq.dbtest/lib/DB2 ).
     *            Omit this parameter to reuse the database information that is stored in bootstrapping.properties
     *            after a test has previously invoked the create operation.
     *            <li>[4] (optional) The path to the JDBC driver jar to use. If this argument is null,<br>
     *            then the database jar will be used from the location specified in arg [3]. Note that if
     *            the bootstrapping.properties file already has a liberty.db_jars property set, that value
     *            will always be used and not overridden by arg[3] or arg[4].
     *            </ul>
     *
     * @throws Exception see exception for details
     */
    public static void main(String[] args) throws Exception {
        try {
            doMain(args);
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw e;
        }
    }

    private static void doMain(String[] args) throws Exception {
        String method = "main";
        if (args.length < 3)
            throw new Exception("At least three arguments are required, see javadoc");

        Log.info(c, method, "[bootstrapLoc, option, testBucketPath, dbPropsPath, dbJarPath]  " + Arrays.toString(args));

        String bootstrapLoc = args[0];
        String option = args[1];
        String testBucketPath = args[2];
        // Assume the dbPropsPath is in the same location as the testBucketPath unless otherwise specified
        String dbPropsPath = args.length == 3 || args[3] == null ? args[2] : args[3];

        // Initialize the bootstrap singleton instance as soon as args have been parsed
        Bootstrap bootstrap = Bootstrap.getInstance(new File(bootstrapLoc));

        // Run start/stop Derby Network options
        if (option.equalsIgnoreCase("startDerbyNet")) {
            DerbyNetworkUtilities.startDerbyNetwork();
            return;
        } else if (option.equalsIgnoreCase("stopDerbyNet")) {
            DerbyNetworkUtilities.stopDerbyNetwork();
            return;
        }

        // Exit if we are Derby and nothing needs to be done
        if (isDerby(dbPropsPath))
            return;

        DatabaseCluster dbCluster;

        if (!dbPropsPath.isEmpty())
            dbCluster = new DatabaseCluster(bootstrap, testBucketPath, dbPropsPath);
        else {
            // reuse values that were previously saved to bootstrapping.properties
            Properties dbProps = new Properties();
            for (BootstrapProperty bp : BootstrapProperty.values()) {
                String key = bp.getPropertyName();
                String value = bootstrap.getValue(key);
                if (value != null)
                    dbProps.put(key, value);
            }
            dbCluster = new DatabaseCluster(bootstrap, testBucketPath, dbProps);
        }

        if (bootstrap.getValue(LIBERTY_DBJARS.getPropertyName()) == null) {
            // If the DB jar path is not defined, set it in the bootstrapping.properties
            String drivername = bootstrap.getValue(DB_DRIVERNAME.getPropertyName());
            String driververs = bootstrap.getValue(DB_DRIVERVERSION.getPropertyName());
            if (drivername != null && driververs != null) {
                String dbJarPath = args.length >= 5 && args[4] != null ? args[4] : args[3];
                String dbJars = dbJarPath + "/jars/" + drivername + '/' + driververs;
                Log.info(c, method, "Setting liberty.db_jars to: " + dbJars);
                bootstrap.setValue(LIBERTY_DBJARS.getPropertyName(), dbJars);
            }
        }

        if (option.equalsIgnoreCase("create")) {
            dbCluster.createDatabase();
        } else if (option.equalsIgnoreCase("drop")) {
            dbCluster.dropDatabase();
        } else if (option.equalsIgnoreCase("runDDL")) {
            dbCluster.runDDL();
        } else
            throw new Exception("Invalid configuration option specified: " + option);
    }

    private static boolean isDerby(String dbPropsPath) throws Exception {
        // First, check if the database.vendorname was directly added to the boostrapping.properties file
        String bootstrapVendorName = Bootstrap.getInstance().getValue(DB_VENDORNAME.getPropertyName());
        if (bootstrapVendorName != null && !bootstrapVendorName.equalsIgnoreCase("derby")) {
            return false;
        }

        File dbPropsDir = new File(dbPropsPath);
        if (!dbPropsDir.exists() || !dbPropsDir.isDirectory()) {
            blowUpUnlessDerby("Database properties directory did not exit at: " + dbPropsPath);
            return true;
        }

        String[] dbProps = DatabaseCluster.getAllDbProps(dbPropsPath);
        if (dbProps == null || dbProps.length == 0) {
            blowUpUnlessDerby("No *.dbprops files found at location: " + dbPropsPath);
            return true;
        }

        boolean allDerbySoFar = true;
        for (String dbProp : dbProps) {
            Properties prop = loadProperties(new File(dbPropsPath, dbProp), null);
            String vendorName = prop.getProperty(DB_VENDORNAME.getPropertyName());
            if (vendorName != null && !vendorName.equalsIgnoreCase("derby")) {
                allDerbySoFar = false;
            }
        }

        if (allDerbySoFar) {
            blowUpUnlessDerby("No database.vendorname specified.");
            return true;
        } else {
            return false;
        }
    }

    private static void blowUpUnlessDerby(String msg) throws Exception {
        // To run with a different DB, fat.test.databases=true must be set
        String fat_test_databases = System.getProperty("fat.test.databases");
        if (!"true".equals(fat_test_databases)) {
            Log.info(c, "blowUpUnlessDerby", msg + " This is OK because we are running with Derby.");
            return;
        }

        String fat_bucket_db_type = System.getProperty("fat.bucket.db.type", "Derby");
        if ("Derby".equals(fat_bucket_db_type)) {
            Log.info(c, "blowUpUnlessDerby", msg + " This is OK because we are running with Derby.");
            return;
        }

        throw new Exception(msg);
    }

    private static Properties loadProperties(File propsFile, String jarLoc) throws IOException {
        if (!propsFile.exists())
            return null;

        Properties props = new Properties();
        InputStream is = new FileInputStream(propsFile);
        props.load(is);
        is.close();

        return props;
    }
}
