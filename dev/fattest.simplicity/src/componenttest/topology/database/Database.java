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

import static componenttest.common.apiservices.BootstrapProperty.DB_DBAPASSWORD;
import static componenttest.common.apiservices.BootstrapProperty.DB_DBAUSER;
import static componenttest.common.apiservices.BootstrapProperty.DB_HOME;
import static componenttest.common.apiservices.BootstrapProperty.DB_HOSTNAME;
import static componenttest.common.apiservices.BootstrapProperty.DB_MACHINEPWD;
import static componenttest.common.apiservices.BootstrapProperty.DB_MACHINEUSER;
import static componenttest.common.apiservices.BootstrapProperty.DB_NAME;
import static componenttest.common.apiservices.BootstrapProperty.DB_PASSWORD1;
import static componenttest.common.apiservices.BootstrapProperty.DB_PASSWORD2;
import static componenttest.common.apiservices.BootstrapProperty.DB_PORT;
import static componenttest.common.apiservices.BootstrapProperty.DB_USER1;
import static componenttest.common.apiservices.BootstrapProperty.DB_USER2;
import static componenttest.common.apiservices.BootstrapProperty.DB_VENDORNAME;
import static componenttest.common.apiservices.BootstrapProperty.DB_VENDORVERSION;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.simplicity.ConnectionInfo;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Variable;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.common.apiservices.Bootstrap;
import componenttest.common.apiservices.BootstrapProperty;
import componenttest.exception.UnavailableDatabaseException;
import componenttest.topology.impl.LibertyServer;

/**
 * Base class that defines the methods for database test processing.
 */
public abstract class Database {

    private final static Class<?> c = Database.class;

    private final static int NUMBER_OF_CREATE_RETRIES = 3;

    private final static int NUMBER_OF_DROP_RETRIES = 3;

    /**
     * Maximum number of database or user id names available due to naming scheme used by Moonstone.
     */
    protected final static int MAX_NUMBER_NAMES = 10000;

    /**
     * Exception message text to indicate database name length exceeded
     */
    protected final static String DATABASE_NAME_LENGTH_EXCEEDED = "Database name must be ";

    protected final String dbhostname, dbhome, dbmachineuser, dbmachinepwd, dbport,
                    dbauser, dbapwd,
                    dbuser1, dbuser2, dbuser1pwd, dbuser2pwd,
                    dbtype, dbversion,
                    testBucketPath, testBucketName;
    protected String dbname;

    protected final Machine localMachine, databaseMachine;

    final Bootstrap bootstrap;
    final Properties dbProps;

    public Database(Bootstrap bootstrap, Properties dbProps, String testBucketPath) throws Exception {
        this.bootstrap = bootstrap;
        this.testBucketPath = testBucketPath;
        this.dbProps = dbProps;
        testBucketName = new File(new File(testBucketPath).getParent()).getName();

        dbhostname = readProp(DB_HOSTNAME);
        dbhome = readProp(DB_HOME);
        dbmachineuser = readProp(DB_MACHINEUSER);
        dbmachinepwd = readProp(DB_MACHINEPWD);
        dbport = readProp(DB_PORT);

        dbauser = readProp(DB_DBAUSER);
        dbapwd = readProp(DB_DBAPASSWORD);
        dbname = readProp(DB_NAME);

        dbuser1 = readProp(DB_USER1);
        dbuser2 = readProp(DB_USER2);
        dbuser1pwd = readProp(DB_PASSWORD1);
        dbuser2pwd = readProp(DB_PASSWORD2);

        dbtype = readProp(DB_VENDORNAME);
        dbversion = readProp(DB_VENDORVERSION);

        localMachine = Machine.getMachine(new ConnectionInfo("localhost", null, null));
        databaseMachine = Machine.getMachine(new ConnectionInfo(dbhostname, dbmachineuser, dbmachinepwd));
    }

    /**
     * Create new test database or user IDs based on the properties in the bootstrap.
     *
     * The create will be tried NUMBER_OF_CREATE_RETRIES times before giving up.
     *
     * @param bootstrap Contents of the bootstrapping.properties file.
     * @throws Exception If the database or user IDs could not be created. This may be due to problems with the
     *             database or because of collisions with other running test buckets selecting the same names.
     */
    final public void createDatabase() throws Exception {
        final String method = "createDatabase";

        testConnection();

        // Physically update the bootstrapping.properties file with the
        // java.util.Properties used to construct the object
        for (String key : dbProps.stringPropertyNames()) {
            String value = dbProps.getProperty(key);
            Log.info(c, method, "Update bootstrapping.properties: " + key + '=' + value);
            bootstrap.setValue(key, value);
        }

        for (int i = 1; i <= NUMBER_OF_CREATE_RETRIES; i++) {
            Log.info(c, method, "Attempt " + i + " to create database.");
            try {
                createVendorDatabase();
                return;
            } catch (Exception ex) {
                Log.warning(c, ex.getMessage());
                if (ex.getMessage().startsWith(DATABASE_NAME_LENGTH_EXCEEDED)) {
                    throw ex;
                }
                if (i == NUMBER_OF_CREATE_RETRIES) {
                    Log.warning(c, "Aborting create database after trying " + NUMBER_OF_CREATE_RETRIES + " times.");
                    throw ex;
                }
            }
        }
    }

    /**
     * Create test objects for a specific vendor.
     */
    abstract protected void createVendorDatabase() throws Exception;

    /**
     * Drop the test database or user IDs
     *
     * @throws Exception If the drop fails.
     */
    final public void dropDatabase() throws Exception {
        final String method = "dropDatabase";
        if (bootstrap.getValue("database.dropdatabase") != null) {
            for (int i = 1; i <= NUMBER_OF_DROP_RETRIES; i++) {
                Log.finer(c, method, "drop database attempt " + i);
                try {
                    dropVendorDatabase();
                    return;
                } catch (Exception e) {
                    if (i == NUMBER_OF_DROP_RETRIES)
                        throw e;
                    Log.finer(c, method, "Waiting for 30s before retrying drop");
                    TimeUnit.SECONDS.sleep(30);
                }
            }
        } else {
            Log.info(c, method,
                     "Drop database ignored since database.dropandcreate has been added to the bootstrapping.properties to save the database after tests have been run.");
        }
    }

    /**
     * Drop test objects for a specific vendor.
     */
    abstract protected void dropVendorDatabase() throws Exception;

    /**
     * Runs all the DDL files in the test bucket for the specific type of database.
     * The files must be located in a directory "ddl" under the project.
     * Under ddl, there should be a directory for each type of database.
     * For example:
     * <p>&nbsp;&nbsp;ddl
     * <br>&nbsp;&nbsp;&nbsp;&nbsp;/db2
     * <br>&nbsp;&nbsp;&nbsp;&nbsp;/informix
     * <br>&nbsp;&nbsp;&nbsp;&nbsp;/oracle
     * <br>&nbsp;&nbsp;&nbsp;&nbsp;/sqlserver
     * <br>&nbsp;&nbsp;&nbsp;&nbsp;/sybase
     *
     * <p>The files are copied to the database machine's temporary directory and
     * deleted after being executed on the test bucket database.
     *
     * @throws Exception
     */
    final public void runDDL() throws Exception {
        String method = "runDDL";

        Log.info(c, "runDDL", "OS temp path is: " + databaseMachine.getTempDir().getAbsolutePath());

        String ddlSourceFileName = testBucketPath + "/ddl/" + dbtype.toLowerCase();
        RemoteFile sourceFile = localMachine.getFile(ddlSourceFileName);
        if (!sourceFile.exists()) {
            Log.finer(c, method, "local file path: " + sourceFile.getAbsolutePath() + " does not exist, no DDL to process for this database type");
            return;
        }

        // The DDL files must be kept separate for each database install and each test bucket since
        // they are kept in one global location on the machine.
        // Either the database name or the user name will be unique, depending on the database type.
        //
        // Delete remote ddl database directory if it already exists to make sure we have a clean copy of
        // the current set of files.
        //
        // The DDL files are only copied under one user id because the user id is just part of making sure
        // the directory name is unique for a test bucket run.
        String remoteMachineDDLPath = databaseMachine.getTempDir().getAbsolutePath() +
                                      "/libertyfat/" + dbtype + "/" + dbversion + "/" + dbname + "/" + dbuser1 + "/" +
                                      testBucketName + "/ddl/" + dbtype.toLowerCase() + "/";
        RemoteFile destFile = new RemoteFile(databaseMachine, remoteMachineDDLPath);
        if (!destFile.delete())
            throw new Exception("The ddl directory was not successfully deleted before copy.");
        if (!sourceFile.copyToDestText(destFile, true, true))
            throw new Exception("DDL files were not copied to the database machine.");
        Log.info(c, method, "DDL files copied to the database machine.");

        runVendorDDL(ddlSourceFileName + "/", remoteMachineDDLPath);
    }

    /**
     * Execute DDL using specific vendor techniques.
     */
    abstract protected void runVendorDDL(String testBucketDDLPath, String tempMachineDDLPath) throws Exception;

    /**
     * Generate a name that is not already in a list of existing names obtained from the database.
     *
     * There is a possible window where the name is selected and then the name is created
     * by another running bucket since buckets can be run asynchronously.
     */
    protected String get_unused_name() throws Exception {
        final String method = "get_unused_name";
        String[] existingNames = existing_names();

        // Search for a name that does not exist
        List<String> dbs_list = Arrays.asList(existingNames);
        for (int i = 0; i < MAX_NUMBER_NAMES; i++) {
            if (!dbs_list.contains(String.format("LIBR%04d", i))) {
                Log.info(c, method, String.format("LIBR%04d is not being used", i));
                return String.format("LIBR%04d", i);
            }
        }
        // This is unlikely unless something is terribly wrong with the Moonstone database maintenance procedure
        throw new Exception("Could not find available name");
    }

    /**
     * List of existing names in the database for the type of object that is created by
     * the specific database test setup object.
     * <p>
     * For example, the list of existing database names could be returned.
     * <p>
     * Database object should implement this to provide a list of existing names
     * for the type of object that will be created.
     *
     * @return list of existing names
     */
    abstract protected String[] existing_names() throws Exception;

    public void testConnection() throws Exception {
        final String m = "testConnection";
        try {
            Log.info(c, m, "Attempting to connect to database server " + dbhostname + " to make sure it's available.");
            databaseMachine.connect();
            Log.info(c, m, "Successfully connected to database server " + dbhostname);
        } catch (Exception ex) {
            // TODO we should have a more prominent way of letting people know that we had to failover?
            // perhaps sending an email out?
            UnavailableDatabaseException unavailableMachine = new UnavailableDatabaseException("The database server " + dbhostname + " is unreachable.", ex);
            Log.error(c, m, unavailableMachine);
            throw unavailableMachine;
        }
    }

    public void addConfigTo(LibertyServer server) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Variable> varList = config.getVariables();

        addOrUpdate(varList, "jdbc.serverName", dbhostname);
        addOrUpdate(varList, "jdbc.portNumber", dbport);
        addOrUpdate(varList, "jdbc.databaseName", dbname);
        addOrUpdate(varList, "jdbc.user", dbuser1);
        addOrUpdate(varList, "jdbc.password", dbuser1pwd);

        server.updateServerConfiguration(config);
    }

    void addOrUpdate(ConfigElementList<Variable> vars, String name, String value) {
        Variable var = vars.getBy("name", name);
        if (var == null)
            vars.add(new Variable(name, value));
        else
            var.setValue(value);
    }

    public String getName() {
        return dbname;
    }

    public String getHostname() {
        return dbhostname;
    }

    public String getPort() {
        return dbport;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + '{' + dbuser1 + '@' + dbhostname + ':' + dbport + '}';
    }

    /**
     * Look for a property in bootstrapping.properties and then the .dbprops file
     */
    private String readProp(BootstrapProperty propName) {
        String bootstrapProp = bootstrap.getValue(propName.getPropertyName());
        if (bootstrapProp != null)
            return bootstrapProp;
        else
            return dbProps.getProperty(propName.getPropertyName());
    }
}