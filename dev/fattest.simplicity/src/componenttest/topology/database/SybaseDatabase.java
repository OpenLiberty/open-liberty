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

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.common.apiservices.Bootstrap;
import componenttest.common.apiservices.BootstrapProperty;

/**
 * Sybase database setup.
 */
public class SybaseDatabase extends Database {

    private final static Class<?> c = SybaseDatabase.class;
    private final static int SYBASE_MAX_NAME_LENGTH = 8;
    private final static int dbsize = 100;
    private final static int logsize = 400;

    public SybaseDatabase(Bootstrap bootstrap, Properties dbProps, String testBucketPath) throws Exception {
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

                // TODO This needs to be done for both user IDs, but the tables are being created under "dbo".
                // Trying to create tables under the user id fails, looks like this has to be done
                // using the admin user id and the owner must be specified as part of the table create.
                String cmd = "$SYBASE/$SYBASE_OCS/bin/isql -D " + dbname + " -U " + dbauser + " -P " + dbapwd
                             + " -i " + tempMachineDDLPath + ddlFile.getName();
                Log.finer(c, method, "execute sybase ddl: " + cmd);
                result = databaseMachine.execute(cmd);
                // There really isn't a way to tell if this worked or not
                // since DDL files often contain statements that can fail with no harm,
                // for example DROP will fail if the object to be dropped has not yet been created.
                // In case debugging is needed, displaying the message in stdout
                //
                // Msg 3701, Level 11, State 1
                // Server 'MOONDB00', Line 1
                // Cannot drop the table 'MYJOBINSTANCEDATA4', because it doesn't exist in the
                // system catalogs.
                if (result.getReturnCode() != 0
                    && !result.getStderr().contains("Msg 3701")) {
                    Log.info(c, method, "ddl execute std out: " + result.getStdout());
                    Log.info(c, method, "ddl execute std err: " + result.getStderr());
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

        String cmd = sybcmd(null, "'sp_helpdb'");

        ProgramOutput dbs_list = databaseMachine.execute(cmd);
        if (dbs_list.getReturnCode() != 0) {
            Log.info(c, method, "sqsh std out: " + dbs_list.getStdout());
            Log.info(c, method, "sqsh std err: " + dbs_list.getStderr());
            throw new Exception("Was not able to obtain list of Sybase database names");
        }

        int numdbs = 0;
        String[] dbs = new String[MAX_NUMBER_NAMES];
        StringTokenizer st = new StringTokenizer(dbs_list.getStdout(), "\n");
        while (st.hasMoreTokens()) {
            String line = st.nextToken().trim();
            if (line.toLowerCase().startsWith("libr")) {
                dbs[numdbs] = line.substring(0, 8);
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

        if (dbname.length() > SYBASE_MAX_NAME_LENGTH)
            throw new Exception(DATABASE_NAME_LENGTH_EXCEEDED + SYBASE_MAX_NAME_LENGTH + " characters or less");

        int numcmdlines = 0;
        String[] cmd = new String[20];

        cmd[numcmdlines++] = sybcmd(null, "'CREATE DATABASE " + dbname + " on fvt = " + dbsize + "  log on fvtlog = " + logsize + "'");
        cmd[numcmdlines++] = sybcmd(dbname, "'sp_adduser " + dbuser1 + "'");
        cmd[numcmdlines++] = sybcmd(dbname, "'sp_adduser " + dbuser2 + "'");
        cmd[numcmdlines++] = sybcmd(dbname, "'sp_adduser jpaschema" + "'");
        cmd[numcmdlines++] = sybcmd(dbname, "'sp_adduser XMLSchName" + "'");
        cmd[numcmdlines++] = sybcmd(dbname, "'sp_adduser DefSchema" + "'");
        cmd[numcmdlines++] = sybcmd(null, "'sp_modifylogin " + dbuser1 + ", defdb, " + dbname + "'");
        cmd[numcmdlines++] = sybcmd(null, "'sp_modifylogin " + dbuser2 + ", defdb, " + dbname + "'");
        cmd[numcmdlines++] = sybcmd(dbname, "'grant all to " + dbuser1 + "'");
        cmd[numcmdlines++] = sybcmd(dbname, "'grant all to " + dbuser2 + "'");
        cmd[numcmdlines++] = sybcmd(dbname, "\"sp_role 'grant', dtm_tm_role, " + dbuser1 + "\"");
        cmd[numcmdlines++] = sybcmd(dbname, "\"sp_role 'grant', dtm_tm_role, " + dbuser2 + "\"");
        cmd[numcmdlines++] = sybcmd(dbname, "\"sp_role 'grant', sa_role, " + dbuser1 + "\"");
        cmd[numcmdlines++] = sybcmd(dbname, "\"sp_role 'grant', sa_role, " + dbuser2 + "\"");
        cmd[numcmdlines++] = sybcmd(null, "\"sp_dboption " + dbname + ", 'ddl in tran',true\"");

        for (int cmdline = 0; cmdline < numcmdlines; cmdline++) {
            Log.finer(c, method, "execute line " + cmd[cmdline]);
            ProgramOutput result = databaseMachine.execute(cmd[cmdline]);
            if (result.getReturnCode() != 0) {
                Log.info(c, method, "Create database returncode: " + result.getReturnCode());
                Log.info(c, method, "Create database stdout: " + result.getStdout());
                Log.info(c, method, "Create database stderr: " + result.getStderr());
                throw new Exception("Creation of database " + dbname + " failed with : " + result.getStderr());
            }
        }
    }

    private void dropDatabase(String dbname) throws Exception {
        final String method = "dropDatabase";

        String cmd = sybcmd(null, "'DROP DATABASE " + dbname + "'");

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

    /*
     * sqsh command is used in Moonstone. It is an open source alternative to isql.
     */
    private String sybcmd(String conndb, String command) throws Exception {
        if (conndb != null)
            return "sqsh -D " + conndb + " -U " + dbauser + " -P " + dbapwd + " -w 2047 -C " + command;
        else
            return "sqsh -U " + dbauser + " -P " + dbapwd + " -w 2047 -C " + command;
    }

}
