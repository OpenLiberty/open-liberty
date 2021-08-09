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

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.common.apiservices.Bootstrap;
import componenttest.common.apiservices.BootstrapProperty;

/**
 * Informix database setup.
 */
public class InformixDatabase extends Database {

    private final static Class<?> c = InformixDatabase.class;
    private final static int INFORMIX_MAX_NAME_LENGTH = 8;
    private final static String ifxSystemUser = "sysmaster";

    private final String dbifxservername, IfxRunCmdFile;
    private final String uniquifier;

    public InformixDatabase(Bootstrap bootstrap, Properties dbProps, String testBucketPath) throws Exception {
        super(bootstrap, dbProps, testBucketPath);

        final String method = "InformixDatabase constructor";
        dbifxservername = dbProps.getProperty(BootstrapProperty.DB_IFXSERVERNAME.getPropertyName());

        // build a file to pass to ifxcmd
        if (dbname != null) {
            uniquifier = dbname;
        } else {
            uniquifier = Long.toString(System.currentTimeMillis());
        }
        String remoteMachineIfxTempPath;
        if (databaseMachine.getOperatingSystem().equals(OperatingSystem.WINDOWS)) {
            remoteMachineIfxTempPath = databaseMachine.getTempDir().getAbsolutePath();
        } else {
            remoteMachineIfxTempPath = dbhome;
        }
        Log.finer(c, method, "database machine temp directory is: " + remoteMachineIfxTempPath);
        String remoteMachineIfxSqlPath = remoteMachineIfxTempPath
                                         + "/libertyfat/" + dbtype + "/" + dbversion + "/"
                                         + uniquifier + "/"
                                         + testBucketName + "/ifxsql/" + dbtype.toLowerCase() + "/";
        Log.finer(c, method, "Remote sql commands file path: " + remoteMachineIfxSqlPath);
        RemoteFile destFile = new RemoteFile(databaseMachine, remoteMachineIfxSqlPath);
        destFile.mkdirs();
        IfxRunCmdFile = remoteMachineIfxSqlPath + "runIFXCmd_" + uniquifier + ".sql";
        Log.finer(c, method, "IfxRunCmdFile: " + IfxRunCmdFile);
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
                    if (!e.getMessage().contains("330: Cannot create or rename the database")) {
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

        String runDDLFile = tempMachineDDLPath + "tempRunDDL.sql";
        for (File ddlFile : ddlFiles) {
            if (ddlFile.isFile()) {
                Log.finer(c, method, "Local File Canonical Path is: " + ddlFile.getCanonicalPath());
                Log.info(c, method, "Remote File is: " + tempMachineDDLPath + ddlFile.getName());

                String[] cmd = new String[10];
                int numcmdlines = 0;
                // TODO this currently only works on Informix installed on Windows
                // TODO only one user can own a table by the same name, so for now only
                // the dbuser1 will have tables created for it
                // build a file to pass to ifxcmd
                cmd[numcmdlines++] = "echo \"" + "connect to '" + dbname + "' user '" + dbuser1 + "' using '" + dbuser1pwd + "';\" > " + runDDLFile;
                cmd[numcmdlines++] = "cat \"" + tempMachineDDLPath + ddlFile.getName() + "\" >> " + runDDLFile;
                cmd[numcmdlines++] = ifxcmd(ifxSystemUser, runDDLFile);
                //cmd[numcmdlines++] = "echo \"" + "connect to '" + dbname + "' user '" + dbuser2 + "' using '" + dbuser2pwd + "';\" > " + runDDLFile;
                //cmd[numcmdlines++] = "cat \"" + tempMachineDDLPath + ddlFile.getName() + "\" >> " + runDDLFile;
                //cmd[numcmdlines++] = ifxcmd(ifxSystemUser, runDDLFile);
                for (int i = 0; i < numcmdlines; i++) {
                    Log.finer(c, method, "execute informix ddl: " + cmd[i]);
                    result = databaseMachine.execute(cmd[i]);
                    // There really isn't a way to tell if this worked or not
                    // since DDL files often contain statements that can fail with no harm,
                    // for example DROP will fail if the object to be dropped has not yet been created.
                    // In case debugging is needed, displaying the message in stdout and stderr
                    //
                    // 310: Table (xxxxx) already exists in database.
                    // 206: The specified table (xxxxx) is not in the database.
                    if (result.getReturnCode() != 0
                        && !result.getStderr().contains("310:")
                        && !result.getStderr().contains("206:")) {
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

        String[] cmd = new String[10];
        int numcmdlines = 0;
        cmd[numcmdlines++] = "echo \"" + "select name from sysdatabases;" + "\" > " + IfxRunCmdFile;
        cmd[numcmdlines++] = ifxcmd(ifxSystemUser, IfxRunCmdFile);

        ProgramOutput dbs_list = null;
        for (int i = 0; i < numcmdlines; i++) {
            Log.finer(c, method, "execute ifxsql: " + cmd[i]);
            dbs_list = databaseMachine.execute(cmd[i]);
            if (dbs_list.getReturnCode() != 0) {
                Log.info(c, method, "ifx sql execute std out: " + dbs_list.getStdout());
                Log.info(c, method, "ifx sql execute std err: " + dbs_list.getStderr());
                throw new Exception("Was not able to obtain list of Informix database names");
            }
        }

        int numdbs = 0;
        String[] dbs = new String[MAX_NUMBER_NAMES];
        StringTokenizer st = new StringTokenizer(dbs_list.getStdout(), "\n");
        while (st.hasMoreTokens()) {
            String line = st.nextToken().trim();
            if (line.toLowerCase().startsWith("name  libr")) {
                dbs[numdbs] = line.substring(6).toUpperCase();
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

        if (dbname.length() > INFORMIX_MAX_NAME_LENGTH)
            throw new Exception(DATABASE_NAME_LENGTH_EXCEEDED + INFORMIX_MAX_NAME_LENGTH + " characters or less");

        int numcmdlines = 0;
        String[] cmd = new String[10];

        cmd[numcmdlines++] = "echo \"" + "CREATE DATABASE " + dbname + " WITH LOG;" + "\" > " + IfxRunCmdFile;
        cmd[numcmdlines++] = ifxcmd(ifxSystemUser, IfxRunCmdFile);
        cmd[numcmdlines++] = "echo \"" + "GRANT DBA TO " + dbuser1 + "," + dbuser2 + ";" + "\" > " + IfxRunCmdFile;
        cmd[numcmdlines++] = ifxcmd(dbname, IfxRunCmdFile);

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

        int numcmdlines = 0;
        String[] cmd = new String[10];

        cmd[numcmdlines++] = "echo \"" + "DROP DATABASE " + dbname + ";\" > " + IfxRunCmdFile;
        cmd[numcmdlines++] = ifxcmd(ifxSystemUser, IfxRunCmdFile);

        for (int cmdline = 0; cmdline < numcmdlines; cmdline++) {
            Log.finer(c, method, "execute line " + cmd[cmdline]);
            ProgramOutput result = databaseMachine.execute(cmd[cmdline]);
            int rc = result.getReturnCode();
            if (rc != 0) {
                Log.info(c, method, "Drop database returncode: " + rc);
                Log.info(c, method, "Drop database stdout: " + result.getStdout());
                Log.info(c, method, "Drop database stderr: " + result.getStderr());
                throw new Exception("Dropping database " + dbname + " failed with return code " + rc +
                                    "   STDOUT: " + result.getStdout() +
                                    "   STDERR: " + result.getStderr());
            }
        }
        Log.info(c, method, "Dropped database " + dbname);
    }

    private String ifxcmd(String conndb, String command) throws Exception {
        final String method = "ifxcmd";
        // translate cygwin form of path to windows form
        // cygwin path is passed in to be consistent with the other databases
        String windowsPath = Pattern.compile("/cygdrive/c/").matcher(dbhome).replaceAll("c:/");
        Log.finer(c, method, "configured path: " + dbhome);
        Log.finer(c, method, "windows path: " + windowsPath);
        if (databaseMachine.getOperatingSystem().equals(OperatingSystem.WINDOWS))
            return "cmd /C \"" + windowsPath + "/" + dbifxservername + ".cmd && dbaccess " + conndb + " " + command + "\"";
        else
            return dbhome + "/" + dbifxservername + ".ksh && dbaccess " + conndb + " " + command;
    }

}
