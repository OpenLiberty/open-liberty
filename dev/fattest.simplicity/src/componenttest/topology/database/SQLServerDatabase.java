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

import java.io.File;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.common.apiservices.Bootstrap;
import componenttest.common.apiservices.BootstrapProperty;

/**
 * SQLServer database setup.
 */
public class SQLServerDatabase extends Database {

    private final static Class<?> c = SQLServerDatabase.class;
    private final static int SQLSERVER_MAX_NAME_LENGTH = 8;

    public SQLServerDatabase(Bootstrap bootstrap, Properties dbProps, String testBucketPath) throws Exception {
        super(bootstrap, dbProps, testBucketPath);
    }

    @Override
    protected void createVendorDatabase() throws Exception {
        final String method = "createVendorDatabase";

        if (dbname == null) {
            String unused_name = get_unused_name();
            Log.info(c, method, "Attempt to create new database with name: " + unused_name);
            createDatabase(unused_name);
            // A new name is only created if the name is not provided in the bootstrapping.properties file.
            // Set the new name in the bootstrapping.properties so test bucket can retrieve these values.
            // Since the name was not provided, this means that the object should be dropped at the end
            // of the test run.
            dbname = unused_name;
            bootstrap.setValue("database.name", dbname);
            bootstrap.setValue("database.dropdatabase", "true");
        } else {
            Log.info(c, method, "Use specified database name: " + dbname);
            String db_dropandcreate = bootstrap.getValue(BootstrapProperty.DB_DROPANDCREATE.getPropertyName());
            Log.info(c, method, "Drop and create set to: " + db_dropandcreate);
            boolean dropandcreate = db_dropandcreate == null
                            ? false : db_dropandcreate.equalsIgnoreCase("true") ? true : false;
            Log.info(c, method, "Drop and create is: " + dropandcreate);
            if (dropandcreate) {
                Log.info(c, method, "Dropping and creating database");
                dropDatabase(dbname);
                createDatabase(dbname);
            } else {
                Log.info(c, method, "Creating database");
                try {
                    createDatabase(dbname);
                } catch (Exception e) {
                    // If the "database already exists" exception occurs, then go on
                    // otherwise throw the exception.
                    if (!e.getMessage().contains("Msg 1801")) {
                        throw e;
                    }
                }
            }
        }
    }

    @Override
    protected void dropVendorDatabase() throws Exception {
        final String method = "dropVendorDatabase";

        String dbname = bootstrap.getValue(BootstrapProperty.DB_NAME.getPropertyName());
        Log.info(c, method, "Drop database " + dbname);
        dropDatabase(dbname);
    }

    @Override
    protected void runVendorDDL(String testBucketDDLPath, String tempMachineDDLPath) throws Exception {
        final String method = "runVendorDDL";

        File bucketDir = new File(testBucketDDLPath);
        File[] ddlFiles = bucketDir.listFiles();
        ProgramOutput result;

        for (File ddlFile : ddlFiles) {
            if (ddlFile.isFile()) {
                Log.finer(c, method, "Local File Canonical Path is: " + ddlFile.getCanonicalPath());
                Log.info(c, method, "Remote File is: " + tempMachineDDLPath + ddlFile.getName());
                String windowsPath = Pattern.compile("/").matcher(tempMachineDDLPath + ddlFile.getName()).replaceAll("\\\\\\\\");
                Log.finer(c, method, "windows path: " + windowsPath);

                String[] cmd = new String[2];
                int numcmdlines = 0;
                cmd[numcmdlines++] = "sqlcmd -m-1 -b -d " + dbname +
                                     " -U " + dbuser1 + " -P " + dbuser1pwd +
                                     " -v schemaName=" + dbuser1 +
                                     " -i " + windowsPath;
                cmd[numcmdlines++] = "sqlcmd -m-1 -b -d " + dbname +
                                     " -U " + dbuser2 + " -P " + dbuser2pwd +
                                     " -v schemaName=" + dbuser2 +
                                     " -i " + windowsPath;

                for (int i = 0; i < numcmdlines; i++) {
                    Log.finer(c, method, "execute sqlserver ddl: " + cmd[i]);
                    result = databaseMachine.execute(cmd[i]);
                    // There really isn't a way to tell if this worked or not
                    // since DDL files often contain statements that can fail with no harm,
                    // for example DROP will fail if the object to be dropped has not yet been created.
                    // In case debugging is needed, displaying the message in stdout
                    //
                    // Msg 3701, Level 11, State 5, Server xxxx, Line xxx
                    // Cannot drop the table 'xxx', because it does not exist or you do not have permission.
                    if (result.getReturnCode() != 0
                        && !result.getStdout().contains("Msg 3701")) {
                        Log.info(c, method, "ddl execute std out: " + result.getStdout());
                        Log.info(c, method, "ddl execute std err: " + result.getStderr());
                    }
                }
            } else {
                Log.finer(c, method, "Local Directory Canonical Path is: " + ddlFile.getCanonicalPath());
                Log.finer(c, method, "Remote Directory Canonical Path is: " + tempMachineDDLPath + "/" + ddlFile.getName());
                runVendorDDL(ddlFile.getCanonicalPath(), tempMachineDDLPath + ddlFile.getName() + "/");
            }
        }
    }

    @Override
    protected String[] existing_names() throws Exception {
        final String method = "existing_names";
        Log.finer(c, method, "Return list of existing databases.");

        String cmd = "sqlcmd -m-1 -b -Q \"select name from sys.databases\"";

        ProgramOutput dbs_list = databaseMachine.execute(cmd);
        if (dbs_list.getReturnCode() != 0) {
            Log.info(c, method, "sqlcmd std out: " + dbs_list.getStdout());
            Log.info(c, method, "sqlcmd std err: " + dbs_list.getStderr());
            throw new Exception("Was not able to obtain list of SQLServer database names");
        }

        int numdbs = 0;
        String[] dbs = new String[MAX_NUMBER_NAMES];
        StringTokenizer st = new StringTokenizer(dbs_list.getStdout(), "\n");
        while (st.hasMoreTokens()) {
            String line = st.nextToken().trim();
            if (line.toLowerCase().startsWith("libr")) {
                dbs[numdbs] = line;
                Log.info(c, method, "Add name to list of names: " + dbs[numdbs]);
                numdbs++;
                if (numdbs > MAX_NUMBER_NAMES)
                    throw new Exception("Exceeded maximum number of database names that can be created");
            }
        }
        return dbs;
    }

    private void createDatabase(String dbname) throws Exception {
        final String method = "createDatabase";
        Log.info(c, method, "Creating database " + dbname + " on " + dbhostname);

        if (dbname.length() > SQLSERVER_MAX_NAME_LENGTH)
            throw new Exception(DATABASE_NAME_LENGTH_EXCEEDED + SQLSERVER_MAX_NAME_LENGTH + " characters or less");

        int numcmdlines = 0;
        String[] cmd = new String[10];

        cmd[numcmdlines++] = "sqlcmd -m-1 -b -Q \"CREATE DATABASE " + dbname + "\"";

        cmd[numcmdlines++] = "sqlcmd -m-1 -b -Q \"use " + dbname + ";" +
                             "create user " + dbuser1 + " for login " + dbuser1 + ";" +
                             "grant alter to " + dbuser1 + ";" +
                             "grant control to " + dbuser1 + ";" +
                             "grant execute to " + dbuser1 + ";" +
                             "\"";
        cmd[numcmdlines++] = "sqlcmd -m-1 -b -d " + dbname + " -Q \"create schema " + dbuser1 + "\"";
        cmd[numcmdlines++] = "sqlcmd -m-1 -b -d " + dbname + " -Q \"alter user " + dbuser1 +
                             " with default_schema = " + dbuser1 + "\"";

        cmd[numcmdlines++] = "sqlcmd -m-1 -b -Q \"use " + dbname + ";" +
                             "create user " + dbuser2 + " for login " + dbuser2 + ";" +
                             "grant alter to " + dbuser2 + ";" +
                             "grant control to " + dbuser2 + ";" +
                             "grant execute to " + dbuser2 + ";" +
                             "\"";
        cmd[numcmdlines++] = "sqlcmd -m-1 -b -d " + dbname + " -Q \"create schema " + dbuser2 + "\"";
        cmd[numcmdlines++] = "sqlcmd -m-1 -b -d " + dbname + " -Q \"alter user " + dbuser2 +
                             " with default_schema = " + dbuser2 + "\"";

        for (int cmdline = 0; cmdline < numcmdlines; cmdline++) {
            Log.finer(c, method, "execute line " + cmd[cmdline]);
            ProgramOutput result = databaseMachine.execute(cmd[cmdline]);
            if (result.getReturnCode() != 0) {
                Log.info(c, method, "Create database returncode: " + result.getReturnCode());
                Log.info(c, method, "Create database stdout: " + result.getStdout());
                Log.info(c, method, "Create database stderr: " + result.getStderr());
                throw new Exception("Creation of database " + dbname + " failed with : " + result.getStdout());
            }
        }
    }

    private void dropDatabase(String dbname) throws Exception {
        final String method = "dropDatabase";

        String cmd = "sqlcmd -m-1 -b -Q \"DROP DATABASE " + dbname + "\"";

        ProgramOutput result = databaseMachine.execute(cmd);
        int rc = result.getReturnCode();
        if (rc != 0) {
            Log.info(c, method, "Drop database returncode: " + rc);
            Log.info(c, method, "Drop database stdout: " + result.getStdout());
            Log.info(c, method, "Drop database stderr: " + result.getStderr());
            throw new Exception("Dropping database " + dbname + " failed with return code " + rc +
                                "   STDOUT: " + result.getStdout() +
                                "   STDERR: " + result.getStderr());
        } else {
            Log.info(c, method, "Dropped database " + dbname);
        }
    }

}
