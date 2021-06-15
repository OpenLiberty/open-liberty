/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonMessageTools;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.ExternalTestService;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.Defaults;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.Timeout;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.RuntimeConfig;
import de.flapdoodle.embed.process.runtime.Network;

public class MongoDBUtils {

    private final static Class<?> thisClass = MongoDBUtils.class;

    public static CommonMessageTools msgUtils = new CommonMessageTools();

    // These should match the strings used in the oAuth20MongoSetup servlet
    final static String DB_NAME = "dbName";
    final static String DB_HOST = "dbHost";
    final static String DB_PORT = "dbPort";
    final static String DB_PWD = "dbPwd";
    final static String DB_USER = "dbUser";
    static String dbInfo = "";

    private static final String TEST_DATABASE = "oauthMongoDB";

    private static String dbHost = "localhost";
    private static int dbPort = -1;

    // from MongoServerSelector
    private static final String TEST_USERNAME = "user";
    private static final String TEST_DATABASE_REMOTE = "default";
    private static final String MONGODB_SERVICE = "mongo-auth";

    private static String dbName = "not set";
    private static String dbPwd = "not set";
    private static String dbUser = TEST_USERNAME;

    private static String mongoTableUid = "not set";

    public static String MONGO_PROPS_FILE = "not set"; // this name needs to match the one used in CustomStoreSample

    private static MongodExecutable mongodExecutable = null;

    private static boolean runRemote = true;

    static {
        /*
         * Local mongoDB does not run on z/OS and some other OS, use remote for now
         */
        //isZOS = LibertyServerUtils.isZOS();
        if (runRemote) {
            Log.info(thisClass, "staticSetup", "Running remote, will connect to remote mongoDB server.");
            dbName = TEST_DATABASE_REMOTE;
        } else {
            Log.info(thisClass, "staticSetup", "Running local mongoDB server.");
            dbName = TEST_DATABASE;

        }
    }

    /**
     * Stop the embedded mongoDB server
     *
     * @throws Exception
     */
    public static void stopMongoDB(String httpString, Integer httpPort) throws Exception {
        if (runRemote) {
            Log.info(thisClass, "mongoDBTeardownCleanup", "cleanupMongoDBEntries");
            MongoDBUtils.cleanupMongoDBEntries(httpString, httpPort);
        } else {
            Log.info(thisClass, "stopMongoDB", "Stopping the embedded MongoDB.");
            mongodExecutable.stop();
            Log.info(thisClass, "stopMongoDB", "Stopped the embedded MongoDB.");
        }
    }

    /**
     * Start the embedded MongoDB server. Will construct the mongoDB URL string to use while connecting to the
     * mongoDB helper servlet -- oAuth20MongoSetup.
     *
     * @param server
     * @param propsFile
     * @throws Exception
     */
    public static void startMongoDB(LibertyServer server, String propsFile, String uid) throws Exception {
        if (runRemote) {
            startMongoDBRemoteOnly(server, propsFile, uid);
            return;
        }
        String method = "startMongoDB";
        MONGO_PROPS_FILE = propsFile;

        Log.info(thisClass, method, "Get free port for MongoDB embedded database.");
        dbPort = Network.getFreeServerPort();

        Log.info(thisClass, method, "Populate mongo db props file for CustomStoreSample use. Port is " + dbPort);
        File tmpFile = new File("lib/LibertyFATTestFiles/", MONGO_PROPS_FILE);
        tmpFile.getParentFile().mkdirs();
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(tmpFile));
            try {
                out.write("DBNAME:" + dbName);
                out.write("\nHOST:" + dbHost);
                out.write("\nPORT:" + dbPort);
            } finally {
                out.close();
            }

            server.copyFileToLibertyServerRoot(MONGO_PROPS_FILE);
        } catch (IllegalStateException e) {
            Log.info(thisClass, method, "Failed to create props file, mongoDB related tests will fail. " + MONGO_PROPS_FILE);
            e.printStackTrace();
        } finally {
            tmpFile.delete();
        }

        /*
         * Startup a MondoDB instance.
         */

        Log.info(thisClass, method, "Start embedded mongoDB server.");
        RuntimeConfig runtimeConfig = Defaults.runtimeConfigFor(Command.MongoD, LoggerFactory.getLogger(thisClass.getName()))
                        .build();
        MongodStarter starter = MongodStarter.getInstance(runtimeConfig);
        MongodConfig builder = MongodConfig.builder()
                        .version(Version.Main.PRODUCTION)
                        .net(new Net(dbHost, dbPort, Network.localhostIsIPv6()))
                        .timeout(new Timeout(30000))
                        .build();
        mongodExecutable = starter.prepare(builder);
        mongodExecutable.start();
        Log.info(thisClass, method, "Started embedded mongoDB server.");

        // build variables to send to the setup servlet
        dbInfo = "&" + DB_NAME + "=" + dbName + "&" + DB_HOST + "=" + dbHost + "&" + DB_PORT + "=" + dbPort;

        Log.info(thisClass, method, "MongoDB servlet props are " + dbInfo);
    }

    /**
     * Added back in to support running on z/OS only
     *
     * @param server
     * @param propsFile
     * @param uid
     * @throws Exception
     */
    public static void startMongoDBRemoteOnly(LibertyServer server, String propsFile, String uid) throws Exception {
        String method = "startMongoDBRemoteOnly";
        MONGO_PROPS_FILE = propsFile;
        mongoTableUid = uid;

        Log.info(thisClass, method, "Running get service for " + MONGODB_SERVICE);
        getAvailableMongoServer(MONGODB_SERVICE);

        Log.info(thisClass, method, "Populate mongo db props file for CustomStoreSample use.");
        File tmpFile = new File("lib/LibertyFATTestFiles/", MONGO_PROPS_FILE);
        tmpFile.getParentFile().mkdirs();
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(tmpFile));
            try {
                out.write("DBNAME:" + dbName);
                out.write("\nHOST:" + dbHost);
                out.write("\nPWD:" + dbPwd);
                out.write("\nPORT:" + dbPort);
                out.write("\nUSER:" + dbUser);
                out.write("\nUID:" + mongoTableUid);
            } finally {
                out.close();
            }

            server.copyFileToLibertyServerRoot(MONGO_PROPS_FILE);
        } catch (IllegalStateException e) {
            Log.info(thisClass, method, "Failed to create props file " + MONGO_PROPS_FILE);
            e.printStackTrace();
            throw new Exception("Could not set up " + MONGO_PROPS_FILE + ". The CustomStoreSample will likely fail to connect to the database.");
        } finally {
            tmpFile.delete();
        }

        // build variables to send to the setup servlet
        dbInfo = "&" + DB_NAME + "=" + dbName + "&" + DB_HOST + "=" + dbHost + "&" + DB_PWD + "=" + dbPwd + "&" + DB_PORT + "=" + dbPort + "&" + DB_USER + "=" + dbUser + "&uid="
                 + uid;

        Log.info(thisClass, method, "MongoDB props are " + dbInfo);
    }

    private static ExternalTestService getAvailableMongoServer(String serverPlaceholder) {
        String method = "getAvailableMongoServer";

        Exception failure = null;
        ExternalTestService mongoService = null;

        for (int i = 1; i < 4; i++) {
            Log.info(thisClass, method, "Request for mongoDB service, attempt #" + i);
            try {
                while (true) {
                    mongoService = ExternalTestService.getService(serverPlaceholder);
                    Log.info(thisClass, method, "Found service, testing if valid");
                    if (validateMongoConnectionAndSetConfig(mongoService)) {
                        Log.info(thisClass, method, "Found service, ping successful");
                        return mongoService;
                    }
                }
            } catch (Exception e) {
                Log.error(thisClass, method, e, "Exeption trying to get MongoDB service.");
                failure = e;
                if (i < 3) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        // fine
                    }
                }
            }
        }

        if (failure != null) {
            // Thrown when there are no more services to try
            Log.warning(thisClass, method + " Despite a retry, could not get requested MongoDB server");
            throw new RuntimeException(failure);
        }

        return mongoService;
    }

    private static boolean validateMongoConnectionAndSetConfig(ExternalTestService mongoService) {
        String method = "validateMongoConnectionAndSetConfig";
        MongoClient mongoClient = null;

        dbHost = mongoService.getAddress();
        int port = mongoService.getPort();
        dbPort = port;

        File trustStore = null;

        MongoClientOptions.Builder optionsBuilder = new MongoClientOptions.Builder().connectTimeout(30000);
        try {
            trustStore = File.createTempFile("mongoTrustStore", "jks");
            Map<String, String> serviceProperties = mongoService.getProperties();

            String password = serviceProperties.get(TEST_USERNAME + "_password"); // will be null if there's no auth for this server

            MongoClientOptions clientOptions = optionsBuilder.build();
            List<MongoCredential> credentials = Collections.emptyList();
            if (password != null) {
                MongoCredential credential = MongoCredential.createCredential(TEST_USERNAME, TEST_DATABASE_REMOTE, password.toCharArray());
                credentials = Collections.singletonList(credential);
                dbPwd = password;
            }

            Log.info(thisClass, method,
                     "Attempting to contact server " + dbHost + ":" + port + " with password " + (password != null ? "set" : "not set") + " and truststore "
                                        + "not set");
            mongoClient = new MongoClient(new ServerAddress(dbHost, port), credentials, clientOptions);
            mongoClient.getDB(TEST_DATABASE_REMOTE).getCollectionNames();
            dbName = TEST_DATABASE_REMOTE;
            mongoClient.close();
        } catch (Exception e) {
            // "Timed out" is checked in the output.txt and can cause a spurious failure
            String exceptionMsg = e.toString().replaceAll("Timed out", "Took too long");
            Log.info(thisClass, method, "Couldn't create a connection to " + mongoService.getServiceName() + " on " + mongoService.getAddress() + ". " + exceptionMsg);
            mongoService.reportUnhealthy("Couldn't connect to server. Exception: " + exceptionMsg);
            return false;
        } finally {
            if (trustStore != null) {
                trustStore.delete();
            }
        }
        return true;
    }

    /**
     * Prepopulate the mongoDB database with some users for later testing.
     *
     * @param httpString
     * @param defaultPort
     * @throws Exception
     */
    public static void setupMongoDBEntries(String httpString, Integer defaultPort, String uid) throws Exception {
        String method = "setupMongoDBEntries";

        HttpURLConnection con = null;

        try {
            msgUtils.printMethodName(method);
            Log.info(thisClass, method, "Create DataBases through the server");
            URL setupURL = AutomationTools.getNewUrl(httpString + "/oAuth20MongoSetup?port=" + defaultPort
                                                     + "&schemaName=OAuthDBSchema" + "&uid=" + uid + dbInfo);
            Log.info(thisClass, method, "setupURL: " + setupURL);
            con = (HttpURLConnection) setupURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(thisClass, method, line);
            }

        } catch (Exception e) {
            Log.error(thisClass, method, e, "Exception occurred while pre-populating up the mongoDB database.");
            System.err.println("Exception: " + e);
            if (con != null) {
                con.disconnect();
            }
            throw e;
        }

    }

    /**
     * Add the requested client to the mongoDB database
     *
     * @param httpString
     * @param defaultPort
     * @param clientID
     * @param secret
     * @param compID
     * @throws Exception
     */
    public static void addMongoDBEntry(String httpString, Integer defaultPort, String clientID, String secret,
                                       String compID) throws Exception {
        addMongoDBEntry(httpString, defaultPort, clientID, secret, compID, null, null);

    }

    /**
     * Add the requested client to the mongoDB database
     *
     * @param httpString
     * @param defaultPort
     * @param clientID
     * @param secret
     * @param compID
     * @param salt
     * @param alg
     * @throws Exception
     */
    public static void addMongoDBEntry(String httpString, Integer defaultPort, String clientID, String secret,
                                       String compID, String salt, String alg) throws Exception {
        String METHOD = "addMongoDBEntry";
        HttpURLConnection con = null;
        try {
            msgUtils.printMethodName(METHOD);
            Log.info(thisClass, METHOD, "Add new entry for " + clientID);

            URL setupURL = AutomationTools.getNewUrl(httpString + "/oAuth20MongoSetup?port=" + defaultPort
                                                     + "&schemaName=OAuthDBSchema&addClient=true" + "&clientID=" + clientID + "&secret=" + secret
                                                     + "&compID=" + compID + (salt != null ? ("&checkSalt=" + salt) : "")
                                                     + (alg != null ? ("&checkAlgorithm=" + alg) : "") + dbInfo);

            Log.info(thisClass, METHOD, "setupURL: " + setupURL);
            con = (HttpURLConnection) setupURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(thisClass, METHOD, line);
            }

            con.disconnect();

        } catch (Exception e) {

            Log.info(thisClass, METHOD, "Exception occurred: " + e.toString());
            Log.error(thisClass, METHOD, e, "Exception occurred");
            System.err.println("Exception: " + e);
            if (con != null) {
                con.disconnect();
            }
            throw e;
        }

    }

    /**
     * Get the type of secret stored in the database (hashed, xor, ext) for the supplied clientID.
     *
     * @param httpString
     * @param defaultPort
     * @param clientID
     * @param compID
     * @return
     * @throws Exception
     */
    public static String checkSecretType(String httpString, Integer defaultPort, String clientID, String compID) throws Exception {
        return checkEntry(httpString, defaultPort, clientID, compID, "checkSecret");
    }

    /**
     * Get the salt value for the supplied clientID
     *
     * @param httpString
     * @param defaultPort
     * @param clientID
     * @param compID
     * @return
     * @throws Exception
     */
    public static String checkSalt(String httpString, Integer defaultPort, String clientID, String compID) throws Exception {
        return checkEntry(httpString, defaultPort, clientID, compID, "checkSalt");
    }

    /**
     * Get the algorithm type for the supplied clientID
     *
     * @param httpString
     * @param defaultPort
     * @param clientID
     * @param compID
     * @return
     * @throws Exception
     */
    public static String checkAlgorithm(String httpString, Integer defaultPort, String clientID, String compID) throws Exception {
        return checkEntry(httpString, defaultPort, clientID, compID, "checkAlgorithm");
    }

    /**
     * Get the iteration value for the supplied clientID
     *
     * @param httpString
     * @param defaultPort
     * @param clientID
     * @param compID
     * @return
     * @throws Exception
     */
    public static String checkIteration(String httpString, Integer defaultPort, String clientID, String compID) throws Exception {
        return checkEntry(httpString, defaultPort, clientID, compID, "checkIteration");
    }

    /**
     * Get the requested value specified by checkType for the supplied clientID.
     *
     * @param httpString
     * @param defaultPort
     * @param clientID
     * @param compID
     * @param checkType
     * @return
     * @throws Exception
     */
    public static String checkEntry(String httpString, Integer defaultPort, String clientID, String compID,
                                    String checkType) throws Exception {

        String METHOD = "checkEntry";
        HttpURLConnection con = null;

        String msg = null;
        try {
            msgUtils.printMethodName(METHOD);

            Log.info(thisClass, METHOD, "Check the " + checkType + " on client " + clientID);
            URL setupURL = AutomationTools
                            .getNewUrl(httpString + "/oAuth20MongoSetup?port=" + defaultPort + "&schemaName=OAuthDBSchema"
                                       + "&clientID=" + clientID + (compID != null ? ("&compID=" + compID) : "") + "&" + checkType + "=true" + dbInfo);

            Log.info(thisClass, METHOD, "setupURL: " + setupURL);
            con = (HttpURLConnection) setupURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(thisClass, METHOD, line);
                msg = line;
            }

            con.disconnect();

        } catch (Exception e) {

            Log.info(thisClass, METHOD, "Exception occurred: " + e.toString());
            Log.error(thisClass, METHOD, e, "Exception occurred");
            System.err.println("Exception: " + e);
            if (con != null) {
                con.disconnect();
            }
            throw e;

        }

        return msg;

    }

    public static void cleanupMongoDBEntries(String httpString, Integer defaultPort) throws Exception {

        HttpURLConnection con = null;
        try {
            msgUtils.printMethodName("cleanupMongoDBEntries");
            Log.info(thisClass, "cleanupMongoDBEntries", "Drop DataBases through the server");
            URL setupURL = AutomationTools
                            .getNewUrl(httpString + "/oAuth20MongoSetup?port=" + defaultPort + "&dropDB=true" + dbInfo);
            Log.info(thisClass, "cleanupMongoDBEntries", "cleanupURL: " + setupURL);
            con = (HttpURLConnection) setupURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(thisClass, "cleanupMongoDBEntries", line);
            }

            con.disconnect();

        } catch (Exception e) {

            Log.info(thisClass, "cleanupMongoDBEntries", "Exception occurred: " + e.toString());
            Log.error(thisClass, "cleanupMongoDBEntries", e, "Exception occurred");
            System.err.println("Exception: " + e);
            if (con != null) {
                con.disconnect();
            }
            // throw e;
        }

    }

    /**
     * Remove existing clients from the database and recreate the default clients.
     *
     * @param httpString
     * @param defaultPort
     * @throws Exception
     */
    public static void clearClientEntries(String httpString, Integer defaultPort) throws Exception {
        String METHOD = "clearClientEntries";
        HttpURLConnection con = null;
        try {
            msgUtils.printMethodName(METHOD);
            Log.info(thisClass, METHOD, "Remove client entries from DB and add them again.");
            URL setupURL = AutomationTools.getNewUrl(httpString + "/oAuth20MongoSetup?port=" + defaultPort + "&clearClients=true" + "&schemaName=OAuthDBSchema" + dbInfo);
            Log.info(thisClass, METHOD, "cleanupURL: " + setupURL);
            con = (HttpURLConnection) setupURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(thisClass, METHOD, line);
            }

            con.disconnect();

        } catch (Exception e) {

            Log.info(thisClass, METHOD, "Exception occurred: " + e.toString());
            Log.error(thisClass, METHOD, e, "Exception occurred");
            System.err.println("Exception: " + e);
            if (con != null) {
                con.disconnect();
            }
            // throw e;
        }

    }
}
