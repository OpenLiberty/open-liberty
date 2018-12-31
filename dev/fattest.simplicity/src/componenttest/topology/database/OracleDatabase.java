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
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Variable;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.common.apiservices.Bootstrap;
import componenttest.common.apiservices.BootstrapProperty;
import componenttest.topology.impl.LibertyServer;

/**
 * Oracle database setup.
 */
public class OracleDatabase extends Database {

    private final static Class<?> c = OracleDatabase.class;
    private final static int NUM_USERS = 2;

    public OracleDatabase(Bootstrap bootstrap, Properties dbProps, String testBucketPath) throws Exception {
        super(bootstrap, dbProps, testBucketPath);
    }

    @Override
    protected void createVendorDatabase() throws Exception {
        final String method = "createVendorDatabase";

        if ((dbuser1 != null && dbuser2 == null) || (dbuser1 == null && dbuser2 != null))
            throw new Exception("either specify both dbuser1 and dbuser2 or neither");

        String[] dbuser_names = new String[NUM_USERS];
        String[] dbuser_passwords = new String[NUM_USERS];
        if (dbuser1 == null && dbuser2 == null) {
            for (int i = 0; i < NUM_USERS; i++) {
                dbuser_names[i] = get_unused_name();
                dbuser_passwords[i] = dbuser_names[i] + "PWD";
                Log.info(c, method, "Attempt to creating new schema with name: " + dbuser_names[i]);
                createSchema(dbuser_names[i], dbuser_passwords[i]);
            }
            // A new name is only created if the name is not provided in the bootstrapping.properties file.
            // Set the new name in the bootstrapping.properties so test bucket can retrieve these values.
            // Since the name was not provided, this means that the object should be dropped at the end
            // of the test run.
            bootstrap.setValue("database.user1", dbuser_names[0]);
            bootstrap.setValue("database.password1", dbuser_passwords[0]);
            bootstrap.setValue("database.user2", dbuser_names[1]);
            bootstrap.setValue("database.password2", dbuser_passwords[1]);
            bootstrap.setValue("database.dropdatabase", "true");
        } else {
            dbuser_names[0] = dbuser1;
            dbuser_names[1] = dbuser2;
            dbuser_passwords[0] = dbuser1pwd;
            dbuser_passwords[1] = dbuser2pwd;
            Log.info(c, method, "Use specified user name: " + dbuser_names[0]);
            Log.info(c, method, "Use specified user name: " + dbuser_names[1]);
            String db_dropandcreate = bootstrap.getValue(BootstrapProperty.DB_DROPANDCREATE.getPropertyName());
            Log.info(c, method, "Drop and create set to: " + db_dropandcreate);
            boolean dropandcreate = db_dropandcreate == null
                            ? false : db_dropandcreate.equalsIgnoreCase("true") ? true : false;
            Log.info(c, method, "Drop and create is: " + dropandcreate);
            if (dropandcreate) {
                Log.info(c, method, "Dropping and creating schemas");
                dropSchema(dbuser_names[0]);
                dropSchema(dbuser_names[1]);
                createSchema(dbuser_names[0], dbuser_passwords[0]);
                createSchema(dbuser_names[1], dbuser_passwords[1]);
            } else {
                Log.info(c, method, "Creating schemas");
                for (int i = 0; i < NUM_USERS; i++) {
                    try {
                        createSchema(dbuser_names[i], dbuser_passwords[i]);
                    } catch (Exception e) {
                        // If the "schema already exists" exception occurs, then go on
                        // otherwise throw the exception.
                        // ORA-01920: user name 'LIBRxxxx' conflicts with another user or role name
                        if (!e.getMessage().contains("ORA-01920")) {
                            throw e;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void dropVendorDatabase() throws Exception {
        final String method = "dropVendorDatabase";

        String dbuser_name1 = bootstrap.getValue(BootstrapProperty.DB_USER1.getPropertyName());
        Log.info(c, method, "Drop schema " + dbuser_name1);
        dropSchema(dbuser_name1);

        String dbuser_name2 = bootstrap.getValue(BootstrapProperty.DB_USER2.getPropertyName());
        Log.info(c, method, "Drop schema " + dbuser_name2);
        dropSchema(dbuser_name2);
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

                Log.finer(c, method, "Execute " + ddlFile.getName() + " for user " + dbuser1);
                String[] cmd = new String[10];
                int numcmdlines = 0;
                cmd[numcmdlines++] = "sqlplus -L -S " + dbuser1 + "/" + dbuser1pwd + "@" + dbname + " @" +
                                     tempMachineDDLPath + ddlFile.getName();
                cmd[numcmdlines++] = "sqlplus -L -S " + dbuser2 + "/" + dbuser2pwd + "@" + dbname + " @" +
                                     tempMachineDDLPath + ddlFile.getName();
                for (int i = 0; i < numcmdlines; i++) {
                    Log.finer(c, method, "execute oracle ddl: " + cmd[i]);
                    result = databaseMachine.execute(cmd[i]);
                    // There really isn't a way to tell if this worked or not
                    // since DDL files often contain statements that can fail with no harm,
                    // for example DROP will fail if the object to be dropped has not yet been created.
                    // In case debugging is needed, displaying the message in stdout
                    //
                    // ORA-00942: table or view does not exist
                    if (result.getStdout().contains("ORA-") && !result.getStdout().contains("ORA-00942")) {
                        Log.info(c, method, "ddl execute command: " + result.getCommand());
                        Log.info(c, method, "ddl execute std err: " + result.getStderr());
                        Log.info(c, method, "ddl execute std out: " + result.getStdout());
                        throw new Exception("ddl file: " + ddlFile + " failed to execute");
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
        Log.finer(c, method, "Return list of existing user names.");

        String OraUsersFile = "ora_users_libr_" + System.currentTimeMillis() + ".sql";

        int numcmdlines = 0;
        String[] cmd = new String[10];
        cmd[numcmdlines++] = "echo \"set head off\" > " + OraUsersFile;
        cmd[numcmdlines++] = "echo \"select username from DBA_USERS where lower(username) like 'libr%' and lower(username) not in 'librdba';\" >> " + OraUsersFile;
        cmd[numcmdlines++] = "echo \"exit;\" >> " + OraUsersFile;
        for (int cmdline = 0; cmdline < numcmdlines; cmdline++) {
            ProgramOutput pgmout = databaseMachine.execute(cmd[cmdline]);
            if (pgmout.getReturnCode() != 0) {
                Log.info(c, method, "echo std out: " + pgmout.getStdout());
                Log.info(c, method, "echo std err: " + pgmout.getStderr());
                throw new Exception("Was not able to obtain list of Oracle user names");
            }
        }

        ProgramOutput db_users_list = databaseMachine.execute("sqlplus -L -S " + dbauser + "/" + dbapwd + "@" + dbname + " @" + OraUsersFile);
        Log.info(c, method, "got list of existing users");
        if (db_users_list.getStdout().contains("ORA-"))
            throw new Exception("Failed to get list of users");

        int numusers = 0;
        String[] dbusers = new String[MAX_NUMBER_NAMES];
        StringTokenizer st = new StringTokenizer(db_users_list.getStdout(), "\n");
        while (st.hasMoreTokens()) {
            String line = st.nextToken();
            if (line.toLowerCase().startsWith("libr")) {
                dbusers[numusers] = line.trim();
                Log.info(c, method, "Add name to list of names: " + dbusers[numusers]);
                numusers++;
                if (numusers > MAX_NUMBER_NAMES)
                    throw new Exception("Exceeded maximum number of user names that can be created");
            }
        }
        return dbusers;
    }

    private void createSchema(String dbuser, String dbpwd) throws Exception {
        final String method = "createSchema";
        Log.info(c, method, "Creating schema " + dbuser + " on " + dbhostname);

        String OraCreateFile = "create_" + dbuser + ".sql";

        int numcmdlines = 0;
        String[] cmd = new String[10];
        cmd[numcmdlines++] = "echo \"CREATE USER " + dbuser + " IDENTIFIED BY " + dbpwd + " DEFAULT TABLESPACE users QUOTA UNLIMITED ON users ACCOUNT UNLOCK;\" >"
                             + OraCreateFile;
        cmd[numcmdlines++] = "echo \"Grant connect,resource,dba to " + dbuser + ";\" >> " + OraCreateFile;
        cmd[numcmdlines++] = "echo \"exit;\" >> " + OraCreateFile;
        cmd[numcmdlines++] = "sqlplus -L -S " + dbauser + "/" + dbapwd + "@" + dbname + " @" + OraCreateFile;

        for (int cmdline = 0; cmdline < numcmdlines; cmdline++) {
            Log.finer(c, method, "execute line " + cmd[cmdline]);
            ProgramOutput result = databaseMachine.execute(cmd[cmdline]);
            // Oracle sqlplus command always returns zero for return code, so need to check the stdout
            // Check the return code for the other commands            
            if (result.getStdout().contains("ORA-") || result.getReturnCode() != 0) {
                Log.info(c, method, "Create schema returncode: " + result.getReturnCode());
                Log.info(c, method, "Create schema stdout: " + result.getStdout());
                Log.info(c, method, "Create schema stderr: " + result.getStderr());
                throw new Exception("Creation of schema " + dbname + " failed with : " + result.getStdout());
            }
        }
    }

    private void dropSchema(String dbuser_name) throws Exception {
        final String method = "dropSchema";

        String dbuser = dbuser_name;
        String OraSessIdFile = "kill_dbuser_" + dbuser + ".sql";
        String OraDropTempFile = "drop_temp" + dbuser + ".sql";
        String OraDropFile = "drop_" + dbuser + ".sql";

        int numcmdlines = 0;
        String[] cmd = new String[10];
        cmd[numcmdlines++] = "echo \"select 'alter system kill session ''' || sid || ',' || serial# || ''';' from vDOLLARsession where lower(username) = lower('" + dbuser
                             + "');\" |sed 's/DOLLAR/$/g' >"
                             + OraSessIdFile;
        cmd[numcmdlines++] = "echo \"exit; \" >> " + OraSessIdFile;
        cmd[numcmdlines++] = "sqlplus -L -S " + dbauser + "/" + dbapwd + "@" + dbname + " @" + OraSessIdFile + " > " + OraDropTempFile;
        cmd[numcmdlines++] = "cat " + OraDropTempFile + " | grep alter | grep system > " + OraDropFile;
        cmd[numcmdlines++] = "echo \"drop user " + dbuser + " cascade;\" >> " + OraDropFile;
        cmd[numcmdlines++] = "echo \"exit;\" >> " + OraDropFile;
        cmd[numcmdlines++] = "sqlplus -L -S " + dbauser + "/" + dbapwd + "@" + dbname + " @" + OraDropFile;

        boolean showDroppedMsg = true;
        for (int cmdline = 0; cmdline < numcmdlines; cmdline++) {
            ProgramOutput result = databaseMachine.execute(cmd[cmdline]);
            int rc = result.getReturnCode();
            // cat will fail if the user session did not exist, which is normally the case
            if (rc != 0 && !cmd[cmdline].startsWith("cat ")) {
                Log.info(c, method, "Drop schema returncode: " + rc);
                Log.info(c, method, "Drop schema stdout: " + result.getStdout());
                Log.info(c, method, "Drop schema stderr: " + result.getStderr());
                throw new Exception("Dropping database " + dbname + " failed with return code " + rc +
                                    "   STDOUT: " + result.getStdout() +
                                    "   STDERR: " + result.getStderr());
            }
            if (result.getStdout().toLowerCase().contains("does not exist")) {
                Log.info(c, method, "Drop schema stdout: " + result.getStdout());
                showDroppedMsg = false;
            }
        }
        if (showDroppedMsg)
            Log.info(c, method, "Dropped schema " + dbuser_name);
    }

    @Override
    public void addConfigTo(LibertyServer server) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Variable> varList = config.getVariables();

        addOrUpdate(varList, "jdbc.URL", "jdbc:oracle:thin:@//" + dbhostname + ':' + dbport + '/' + dbname);
        addOrUpdate(varList, "jdbc.user", bootstrap.getValue("database.user1"));
        addOrUpdate(varList, "jdbc.password", bootstrap.getValue("database.password1"));
        addOrUpdate(varList, "jdbc.serverName", dbhostname);
        addOrUpdate(varList, "jdbc.portNumber", dbport);
        addOrUpdate(varList, "jdbc.databaseName", dbname);

        server.updateServerConfiguration(config);
    }

}
