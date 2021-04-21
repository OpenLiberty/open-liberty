/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.mongo.fat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.ibm.websphere.simplicity.config.MongoDBElement;
import com.ibm.websphere.simplicity.config.MongoElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.ExternalTestService;

public class MongoServerSelector {

    private static final Class<?> c = MongoServerSelector.class;
    private static final String TEST_USERNAME = "user";
    private static final String TEST_DATABASE = "default";
    private static final char[] KEYSTORE_PW = "passw0rd".toCharArray();
    private final LibertyServer _server;
    private final ServerConfiguration serverConfig;
    private final Map<String, List<MongoElement>> mongoElements = new HashMap<String, List<MongoElement>>();

    public MongoServerSelector(LibertyServer server) throws Exception {
        _server = server;
        serverConfig = _server.getServerConfiguration();
    }

    /**
     * Reads a server configuration and replaces the placeholder values with an available host and port as specified in the mongo.properties file
     *
     * @param libertyServer The server configuration to assign Mongo servers to
     * @throws Exception Can't connect to any of the specified Mongo servers
     */
    public static void assignMongoServers(LibertyServer libertyServer) throws Exception {
        MongoServerSelector mongoServerSelector = new MongoServerSelector(libertyServer);
        mongoServerSelector.updateLibertyServerMongos();
    }

    /**
     * @param mongoServerSelector
     * @throws Exception
     */
    private void updateLibertyServerMongos() throws Exception {
        for (String serverPlaceholder : getMongoElements().keySet()) {
            ExternalTestService mongoTestService = getAvailableMongoServer(serverPlaceholder);
            updateMongoElements(serverPlaceholder, mongoTestService);
            extractFiles(serverPlaceholder, mongoTestService);
        }
        _server.updateServerConfiguration(serverConfig);
    }

    /**
     * Creates a mapping of a Mongo server placeholder value to the one or more mongo elements in the server.xml containing that placeholder
     *
     * @param libertyServer
     * @return
     */
    private Map<String, List<MongoElement>> getMongoElements() throws Exception {
        for (MongoElement mongo : serverConfig.getMongos()) {
            addMongoElementToMap(mongo);
        }
        for (MongoDBElement mongoDB : serverConfig.getMongoDBs()) {
            if (mongoDB.getMongo() != null) {
                addMongoElementToMap(mongoDB.getMongo());
            }
        }
        return mongoElements;
    }

    /**
     * @param mongoElements
     * @param mongo
     * @return
     */
    private void addMongoElementToMap(MongoElement mongo) {
        String hostNamePlaceholder = mongo.getHostNames();
        if (mongoElements.containsKey(hostNamePlaceholder)) {
            mongoElements.get(hostNamePlaceholder).add(mongo);
        } else {
            List<MongoElement> newListMongoElement = new ArrayList<MongoElement>();
            newListMongoElement.add(mongo);
            mongoElements.put(hostNamePlaceholder, newListMongoElement);
        }
    }

    /**
     * For the mongo elements containing the given placeholder value, the host and port placeholder values in the mongo element are replaced with an actual host and port of an
     * available mongo server.
     *
     * @param availableMongoServer
     */
    private void updateMongoElements(String serverPlaceholder, ExternalTestService mongoService) {
        Integer[] port = { Integer.valueOf(mongoService.getPort()) };
        for (MongoElement mongo : mongoElements.get(serverPlaceholder)) {
            mongo.setHostNames(mongoService.getAddress());
            mongo.setPorts(port);

            String user = mongo.getUser();
            if (user != null) {
                String replacementPassword = mongoService.getProperties().get(user + "_password");
                if (replacementPassword != null) {
                    mongo.setPassword(replacementPassword);
                }
            }
        }
    }

    /**
     * @param serverPlaceholder
     * @param mongoTestService
     * @throws Exception
     */
    private void extractFiles(String serverPlaceholder, ExternalTestService mongoTestService) throws Exception {
        String securityPath = "resources/" + serverPlaceholder + "/security";
        extractFile("client_good_keystore", "client_good_keystore.jks", securityPath, mongoTestService);
        extractFile("client_not_known_keystore", "client_not_known_keystore.jks", securityPath, mongoTestService);
        extractFile("client_all_keystore", "client_all_keystore.jks", securityPath, mongoTestService);
        extractFile("ca_truststore", "truststore.jks", securityPath, mongoTestService);
    }

    private void extractFile(String keyName, String fileName, String path, ExternalTestService mongoTestService) throws Exception {
        File tmpFile = new File("lib/LibertyFATTestFiles/", fileName);
        tmpFile.getParentFile().mkdirs();
        try {
            mongoTestService.writePropertyAsFile(keyName, tmpFile);
            _server.copyFileToLibertyServerRoot(path, fileName);
            tmpFile.delete();
        } catch (IllegalStateException e) {
            // Thrown if the key name doesn't exist
            // Just skip this file for this server
        }
    }

    private static ExternalTestService getAvailableMongoServer(String serverPlaceholder) {
        try {
            while (true) {
                ExternalTestService mongoService = ExternalTestService.getService(serverPlaceholder);
                if (validateMongoConnection(mongoService)) {
                    return mongoService;
                }
            }
        } catch (Exception e) {
            // Thrown when there are no more services to try
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a connection to the mongo server at the given location using the mongo java client.
     *
     * @param location
     * @param auth
     * @param encrypted
     * @return
     * @throws Exception
     */
    private static boolean validateMongoConnection(ExternalTestService mongoService) {
        String method = "validateMongoConnection";
        MongoClient mongoClient = null;

        String host = mongoService.getAddress();
        int port = mongoService.getPort();

        File trustStore = null;

        MongoClientOptions.Builder optionsBuilder = new MongoClientOptions.Builder().connectTimeout(30000);
        optionsBuilder.maxWaitTime(30000);
        try {
            trustStore = File.createTempFile("mongoTrustStore", "jks");
            Map<String, String> serviceProperties = mongoService.getProperties();

            String password = serviceProperties.get(TEST_USERNAME + "_password"); // will be null if there's no auth for this server
            SSLSocketFactory sslSocketFactory = null;

            try {
                mongoService.writePropertyAsFile("ca_truststore", trustStore);
                sslSocketFactory = getSocketFactory(trustStore);
            } catch (IllegalStateException e) {
                // Ignore exception thrown if truststore is not present for this server
                // This indicates that we are not using SSL for this server and sslSocketFactory will be null
            }

            if (sslSocketFactory != null) {
                optionsBuilder.socketFactory(sslSocketFactory);
            }

            MongoClientOptions clientOptions = optionsBuilder.build();
            List<MongoCredential> credentials = Collections.emptyList();
            if (password != null) {
                MongoCredential credential = MongoCredential.createCredential(TEST_USERNAME, TEST_DATABASE, password.toCharArray());
                credentials = Collections.singletonList(credential);
            }

            Log.info(c, method,
                     "Attempting to contact server " + host + ":" + port + " with password " + (password != null ? "set" : "not set") + " and truststore "
                                + (sslSocketFactory != null ? "set" : "not set"));
            mongoClient = new MongoClient(new ServerAddress(host, port), credentials, clientOptions);
            mongoClient.getDB("default").getCollectionNames();
            mongoClient.close();
        } catch (Exception e) {
            Log.info(c, method, "Couldn't create a connection to " + mongoService.getServiceName() + " on " + mongoService.getAddress() + ". " + e.toString());
            mongoService.reportUnhealthy("Couldn't connect to server. Exception: " + e.toString());
            return false;
        } finally {
            if (trustStore != null) {
                trustStore.delete();
            }
        }
        return true;
    }

    /**
     * Returns an SSLSocketFactory needed to connect to an SSL mongo server. The socket factory is created using the testTruststore.jks.
     *
     * @return
     * @throws KeyStoreException
     * @throws FileNotFoundException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws KeyManagementException
     */
    private static SSLSocketFactory getSocketFactory(File trustStore) throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException {
        KeyStore keystore = KeyStore.getInstance("JKS");

        InputStream truststoreInputStream = null;
        try {
            truststoreInputStream = new FileInputStream(trustStore);
            keystore.load(truststoreInputStream, KEYSTORE_PW);
        } finally {
            if (truststoreInputStream != null) {
                truststoreInputStream.close();
            }
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
        trustManagerFactory.init(keystore);
        TrustManager[] trustMangers = trustManagerFactory.getTrustManagers();

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustMangers, null);

        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        return sslSocketFactory;
    }

}
