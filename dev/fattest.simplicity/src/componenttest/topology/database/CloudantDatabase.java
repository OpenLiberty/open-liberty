/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.database;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Variable;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.common.apiservices.Bootstrap;
import componenttest.common.apiservices.BootstrapProperty;
import componenttest.exception.UnavailableDatabaseException;
import componenttest.topology.impl.LibertyServer;

/**
 * @see https://docs.cloudant.com/database.html
 */
public class CloudantDatabase extends Database {

    /**
     * If you ever need to do manual DB administration, run this main method to invoke
     * whatever list/delete/create operations that you need.
     */
    public static void main(String args[]) throws Exception {

        Properties props = new Properties();
        props.put("database.hostname", "<MACHINE HOSTNAME HERE>");
        props.put("database.port", "5984");
        props.put("database.port.secure", "6984");
        props.put("database.user1", "username");
        props.put("database.password1", "password");
        CloudantDatabase db = new CloudantDatabase(Bootstrap.getInstance(), props, System.getProperty("user.dir"));
        db.testConnection();

        for (String dbName : db.existing_names())
            System.out.println("found db: " + dbName);
    }

    private final static Class<?> c = CloudantDatabase.class;

    public CloudantDatabase(Bootstrap bootstrap, Properties dbProps, String testBucketPath) throws Exception {
        super(bootstrap, dbProps, testBucketPath);
    }

    private void createDatabase(String dbname) throws Exception {
        final String method = "createDatabase";
        Log.info(c, method, "Creating database " + dbname + " on " + dbhostname);

        checkValidDBName(dbname);

        HttpURLConnection con = executeHTTPQuery("PUT", '/' + dbname);
        int rc = con.getResponseCode();
        if (rc == 201 || rc == 202 || rc == 412)
            Log.info(c, method, "Database " + dbname + " created successfully");
        else
            throw new Exception("Creating database " + dbname + " failed with response code: " + rc);
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
            boolean dropandcreate = db_dropandcreate == null ? false : db_dropandcreate.equalsIgnoreCase("true") ? true : false;
            Log.info(c, method, "Drop and create is: " + dropandcreate);
            if (dropandcreate) {
                Log.info(c, method, "Dropping and creating database");
                dropDatabase(dbname);
                createDatabase(dbname);
            } else {
                Log.info(c, method, "Creating database");
                createDatabase(dbname);
            }
        }
    }

    @Override
    protected void dropVendorDatabase() throws Exception {
        String dbname = bootstrap.getValue(BootstrapProperty.DB_NAME.getPropertyName());
        dropDatabase(dbname);
    }

    private void dropDatabase(String dbname) throws Exception {
        final String method = "dropDatabase";
        Log.info(c, method, "Dropping database " + dbname);

        HttpURLConnection con = executeHTTPQuery("DELETE", '/' + dbname);

        try {
            int rc = con.getResponseCode();
            if (rc == 200)
                Log.info(c, method, "Successfully dropped database " + dbname);
            else {
                dumpResponse(con);
                throw new Exception("Dropping database " + dbname + " failed with response code: " + rc);
            }
        } catch (FileNotFoundException ignore) {
            Log.info(c, method, "Database " + dbname + " did not exist");
        }
    }

    @Override
    protected void runVendorDDL(String testBucketDDLPath, String tempMachineDDLPath) throws Exception {
        // no-op
    }

    @Override
    protected String get_unused_name() throws Exception {
        final String method = "get_unused_name";
        String[] existingNames = existing_names();

        // Search for a name that does not exist
        List<String> dbs_list = Arrays.asList(existingNames);
        for (int i = 0; i < MAX_NUMBER_NAMES; i++)
            if (!dbs_list.contains(String.format("libr%04d", i))) {
                Log.info(c, method, String.format("libr%04d is not being used", i));
                return String.format("libr%04d", i);
            }

        // This is unlikely unless something is terribly wrong with the Moonstone database maintenance procedure
        throw new Exception("Could not find available name");
    }

    @Override
    protected String[] existing_names() throws Exception {
        HttpURLConnection con = executeHTTPQuery("GET", "/_all_dbs");
        return dumpResponse(con).replace("[", "").replace("]", "").replace("\"", "").split(",");
    }

    @Override
    public void testConnection() throws Exception {
        HttpURLConnection con = executeHTTPQuery("GET", "/");

        try {
            int rc = con.getResponseCode();
            String response = dumpResponse(con);

            if (rc != 200)
                throw new UnavailableDatabaseException("Pinging database failed with response code: " + rc);

            if (!response.contains("Welcome"))
                throw new UnavailableDatabaseException("Did not get valid data from the initial database ping: " + response);
        } finally {
            con.disconnect();
        }
    }

    @Override
    public void addConfigTo(LibertyServer server) throws Exception {
        String securePort = bootstrap.getValue(BootstrapProperty.DB_PORT_SECURE.getPropertyName());
        ServerConfiguration config = server.getServerConfiguration();

        ConfigElementList<Variable> varList = config.getVariables();
        addOrUpdate(varList, "cloudant.url", "http://" + dbhostname + ':' + dbport);
        addOrUpdate(varList, "cloudant.url.secure", "https://" + dbhostname + ':' + securePort);
        addOrUpdate(varList, "cloudant.username", dbuser1);
        addOrUpdate(varList, "cloudant.password", dbuser1pwd);
        addOrUpdate(varList, "cloudant.databaseName", dbname);

        server.updateServerConfiguration(config);
    }

    private static String dumpResponse(HttpURLConnection con) throws Exception {
        StringBuffer sb = new StringBuffer();
        InputStream is = con.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        // Send output from servlet to console output
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            sb.append(line);
            Log.info(c, "dumpResponse", line);
        }

        br.close();
        isr.close();
        is.close();

        return sb.toString();
    }

    private HttpURLConnection executeHTTPQuery(String requestMethod, String query) throws Exception {

        String auth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary((dbuser1 + ':' + dbuser1pwd).getBytes());
        URL url = new URL("http://" + dbhostname + ':' + dbport + query);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("Authorization", auth);
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("Content-type", "application/json");
        con.setRequestProperty("User-Agent", "java-cloudant/unknown");

        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);

        con.setRequestMethod(requestMethod);

        return con;
    }

    private void checkValidDBName(String dbName) {
        if (dbName == null)
            throw new IllegalArgumentException("Database name cannot be null.");

        if (!Pattern.matches("^[a-z0-9_$()+-/]*$", dbName))
            throw new IllegalArgumentException("Database name '" + dbName + "' must only contain: [a-z] [0-9] and chars: _$()+-/");
    }
}
