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
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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

    /**
     * Create a database with the given name. If the database already exists, this method will return false.
     *
     * @param dbname The name of the database to be created
     * @return whether the database was created or it already existed.
     * @throws Exception if cloudant responds to the PUT request with an error code
     */
    private boolean createDatabase(String dbname) throws Exception {
        final String method = "createDatabase";
        Log.info(c, method, "Creating database " + dbname + " on " + dbhostname);

        checkValidDBName(dbname);

        // Execute the PUT query on the unsecure port
        HttpURLConnection con = executeHTTPQuery("PUT", '/' + dbname, false);
        int rc = con.getResponseCode();
        if (rc == 201 || rc == 202) {
            Log.info(c, method, "Database " + dbname + " created successfully on unsecure port ");
        } else if (rc == 412) {
            Log.info(c, method, "Database " + dbname + " already exists on unsecure port ");
            return false;
        } else {
            throw new Exception("Creating database " + dbname + " failed on unsecure port with response code: " + rc);
        }

        // Execute the PUT query on the secure port since cloudant distinguishes between the databases on unsecure and secure ports
        con = executeHTTPQuery("PUT", '/' + dbname, true);
        rc = con.getResponseCode();
        if (rc == 201 || rc == 202) {
            Log.info(c, method, "Database " + dbname + " created successfully on secure port");
        } else if (rc == 412) {
            Log.info(c, method, "Database " + dbname + " already exists on secure port ");
            return false;
        } else {
            // If a problem occurs when creating the database on the secure port, make sure to delete the database we just created
            // on the unsecure port before throwing an exception.
            executeHTTPQuery("DELETE", '/' + dbname, false);
            throw new Exception("Creating database " + dbname + " failed on secure port with response code: " + rc);
        }
        return true;
    }

    @Override
    protected void createVendorDatabase() throws Exception {
        final String method = "createVendorDatabase";

        if (dbname == null) {
            String unused_name = get_unused_name();
            Log.info(c, method, "Attempt to create new database with name: " + unused_name);
            boolean isNewDB = createDatabase(unused_name);
            if (!isNewDB) {
                // There is a small window where two separate builds might request the list of unused names at the same time resulting in both
                // builds getting the same database name. In that case, retry querying for unused names and attempting to create a new database (5 times).
                for (int i = 0; i < 5 && !isNewDB; i++) {
                    unused_name = get_unused_name();
                    Log.info(c, method, "The database already existed. Attempting to create the database with a new name: " + unused_name);
                    isNewDB = createDatabase(unused_name);
                }

                if (!isNewDB) {
                    throw new Exception("Creating a new database failed. Unable to find an unused database name.");
                }
            }
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
        Log.info(c, method, "Dropping database " + dbname + " from unsecure and secure ports");

        // Execute the delete query on the database on the unsecure port
        HttpURLConnection con = executeHTTPQuery("DELETE", '/' + dbname, false);

        try {
            int rc = con.getResponseCode();
            if (rc == 200)
                Log.info(c, method, "Successfully dropped database " + dbname + " on unsecure port");
            else {
                dumpResponse(con);
                throw new Exception("Dropping database " + dbname + " failed on unsecure port with response code: " + rc);
            }
        } catch (FileNotFoundException ignore) {
            Log.info(c, method, "Database " + dbname + " did not exist on unsecure port");
        }

        // Execute the delete query on the database on the secure port
        con = executeHTTPQuery("DELETE", '/' + dbname, true);

        try {
            int rc = con.getResponseCode();
            if (rc == 200)
                Log.info(c, method, "Successfully dropped database " + dbname + " on secure port");
            else {
                dumpResponse(con);
                throw new Exception("Dropping database " + dbname + " failed on secure port with response code: " + rc);
            }
        } catch (FileNotFoundException ignore) {
            Log.info(c, method, "Database " + dbname + " did not exist on secure port");
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
        // Note: a simple dbs_list.contains call won't work since the names are sometimes surrounded by extra characters
        List<String> dbs_list = Arrays.asList(existingNames);
        for (int i = 0; i < MAX_NUMBER_NAMES; i++) {
            String candidate = String.format("libr%04d", i);

            boolean exists = false;
            for (String dbname : dbs_list) {
                if (dbname.contains(candidate)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                Log.info(c, method, String.format("libr%04d is not being used", i));
                return String.format("libr%04d", i);
            }
        }

        // This is unlikely unless something is terribly wrong with the Moonstone database maintenance procedure
        throw new Exception("Could not find available name");
    }

    @Override
    protected String[] existing_names() throws Exception {
        // Query for all the database names on the unsecure and secure port
        String[] dbnamesUnsecure = dumpResponse(executeHTTPQuery("GET", "/_all_dbs", false)).replace("[", "").replace("]", "").replace("\"", "").split(",");
        String[] dbnamesSecure = dumpResponse(executeHTTPQuery("GET", "/_all_dbs", true)).replace("[", "").replace("]", "").replace("\"", "").split(",");

        String[] dbnames = new String[dbnamesUnsecure.length + dbnamesSecure.length];
        System.arraycopy(dbnamesUnsecure, 0, dbnames, 0, dbnamesUnsecure.length);
        System.arraycopy(dbnamesSecure, 0, dbnames, dbnamesUnsecure.length, dbnamesSecure.length);

        return dbnames;
    }

    @Override
    public void testConnection() throws Exception {
        // Test the connection to the unsecure port
        HttpURLConnection con = executeHTTPQuery("GET", "/", false);

        try {
            int rc = con.getResponseCode();
            String response = dumpResponse(con);

            if (rc != 200)
                throw new UnavailableDatabaseException("Pinging database failed on the unsecure port with response code: " + rc);

            if (!response.contains("Welcome"))
                throw new UnavailableDatabaseException("Did not get valid data from the initial database ping to the unsecure port: " + response);
        } finally {
            con.disconnect();
        }

        // Test the connection to the secure port
        con = executeHTTPQuery("GET", "/", false);

        try {
            int rc = con.getResponseCode();
            String response = dumpResponse(con);

            if (rc != 200)
                throw new UnavailableDatabaseException("Pinging database failed on the secure port with response code: " + rc);

            if (!response.contains("Welcome"))
                throw new UnavailableDatabaseException("Did not get valid data from the initial database ping to the secure port: " + response);
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
        addOrUpdate(varList, "cloudant.server", dbhostname);
        addOrUpdate(varList, "cloudant.port.secure", securePort);

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

    private HttpURLConnection executeHTTPQuery(String requestMethod, String query, boolean secure) throws Exception {
        HttpURLConnection con;

        if (!secure) {
            con = (HttpURLConnection) new URL("http://" + dbhostname + ':' + dbport + query).openConnection();
        } else {
            HttpsURLConnection httpscon = (HttpsURLConnection) new URL("https://" + dbhostname + ':' + bootstrap.getValue(BootstrapProperty.DB_PORT_SECURE.getPropertyName())
                                                                       + query).openConnection();
            //All hosts are valid
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            // Install the all-trusting host verifier
            httpscon.setHostnameVerifier(allHostsValid);

            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            } };
            try {
                SSLContext sc = SSLContext.getInstance("TLSv1");
                sc.init(null, trustAllCerts, new SecureRandom());

                httpscon.setSSLSocketFactory(sc.getSocketFactory());
            } catch (GeneralSecurityException e) {
                System.err.println("CheckServerAvailability hit an error when trying to ignore certificates.");
                e.printStackTrace();
            }
            con = httpscon;
        }
        String auth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary((dbuser1 + ':' + dbuser1pwd).getBytes());

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
