/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.mongo.fat;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.MongoDBElement;
import com.ibm.websphere.simplicity.config.MongoElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.mongo.fat.shared.MongoServletAction;

import componenttest.annotation.AllowedFFDC;

public abstract class TestMongoDb extends AbstractMongoTestCase {
    private static final Class<?> c = TestMongoDb.class;

    @Test
    public void testResourceEnvRef() throws IOException {
        final String method = "testResourceEnvRef";
        Log.info(c, method, "entering " + method);
        insertFindScenario(true, MongoServlet.RESOURCEENVREF);
        Log.info(c, method, "exiting " + method);
    }

    @Test
    public void testInsertFindAuthenticatedInject() throws IOException {
        final String method = "testInsertFindAuthenticatedInject";
        Log.info(c, method, "entering " + method);
        insertFindScenario(true, MongoServlet.INJECTED);
        Log.info(c, method, "exiting " + method);
    }

    // See RTC work item 238129
    // @Test
    public void testInsertFindUnauthenticatedInject() throws IOException {
        final String method = "testInsertFindUnauthenticatedInject";
        Log.info(c, method, "entering " + method);
        insertFindScenario(false, MongoServlet.INJECTED);
        Log.info(c, method, "exiting " + method);
    }

    // See RTC work item 238129
    // @Test
    public void testInsertFindUnauthenticatedJNDI() throws IOException {
        final String method = "testInsertFindUnauthenticatedJNDI";
        Log.info(c, method, "entering " + method);
        insertFindScenario(false, MongoServlet.JNDI);
        Log.info(c, method, "exiting " + method);
    }

    @Test
    public void testInsertFindAuthenticatedJNDI() throws IOException {
        final String method = "testInsertFindAuthenticatedJNDI";
        Log.info(c, method, "entering " + method);
        insertFindScenario(true, MongoServlet.JNDI);
        Log.info(c, method, "exiting " + method);
    }

    @Test
    public void testChangeDbInsertFindJNDI() throws Exception {
        final String method = "testChangeDbInsertFindJNDI";
        Log.info(c, method, "entering " + method);
        insertFindScenario(false, MongoServlet.JNDI);

        ServerConfiguration working = config.clone();

        ConfigElementList<MongoDBElement> mdbs = working.getMongoDBs();
        for (MongoDBElement mdb : mdbs) {
            // Change all MongoDB elemets to point at new db
            mdb.setDatabaseName("default2");
        }
        updateServerConfig(working, true, true, MongoServlet.JNDI, true, true);
        try {
            Map<String, String> conf = getConfig(MongoServlet.JNDI);
            if (!conf.get("databaseName").equals("default2")) {
                fail("databaseName should have been default2, but was " + conf.get("databaseName"));
            }
            insertFindScenario(false, MongoServlet.JNDI);
        } finally {
            updateServerConfig(config, true, true, MongoServlet.JNDI, true, true);
        }
        Log.info(c, method, "exiting " + method);
    }

    @Test
    public void testChangeDbInsertFindInject() throws Exception {
        final String method = "testChangeDbInsertFindInject";
        Log.info(c, method, "entering " + method);
        insertFindScenario(false, MongoServlet.INJECTED);

        ServerConfiguration working = config.clone();

        ConfigElementList<MongoDBElement> mdbs = working.getMongoDBs();
        for (MongoDBElement mdb : mdbs) {
            // Change all MongoDB elemets to point at new db
            mdb.setDatabaseName("default2");
        }
        updateServerConfig(working, true, true, MongoServlet.INJECTED, true, true);
        try {
            Map<String, String> conf = getConfig(MongoServlet.INJECTED);
            if (!conf.get("databaseName").equals("default2")) {
                fail("databaseName should have been default2, but was " + conf.get("databaseName"));
            }
            insertFindScenario(false, MongoServlet.INJECTED);
        } finally {
            updateServerConfig(config, true, true, MongoServlet.INJECTED, true, true);
        }
        Log.info(c, method, "exiting " + method);
    }

    @Test
    public void testUpdateLibRefJNDI() throws Exception {
        final String method = "testUpdateLibRefJNDI";
        Log.info(c, method, "entering " + method);
        ServerConfiguration working = config.clone();
        // Prime, make sure things are working
        insertFindScenario(false, MongoServlet.JNDI);

        // Update server.xml to point at new lib
        modifyLib(working, "mongo-lib-updated");

        updateServerConfig(working, true, true, MongoServlet.JNDI, false, true);
        insertFindScenario(false, MongoServlet.JNDI);

        // put config back!
        updateServerConfig(config, true, true, MongoServlet.JNDI, true, true);
        Log.info(c, method, "exiting " + method);
    }

    @Test
    public void testRemoveAddMongoFeatureFromRunningServer() throws Exception {
        final String method = "testRemoveAddMongoFeatureFromRunningServer";
        Log.info(c, method, "entering " + method);
        ServerConfiguration working = config.clone();

        Set<String> features = working.getFeatureManager().getFeatures();
        for (String feature : features) {
            if ("mongodb-2.0".equals(feature)) {
                features.remove(feature);
                break;
            }
        }
        // Update config without mongo feature
        updateServerConfigWithParams(working, true, true, MongoServlet.JNDI, true, false, "test", "testRemoveAddMongoFeatureFromRunningServer()");

        // Don't need to call the app (it won't work without mongo), but at least need to
        // wait for it to finish restarting before moving on with the next update.
        if (server.waitForStringInLogUsingMark("CWWKZ000[13]I.* mongo") == null) {
            throw new Exception("Server application restart did not complete within the allotted interval");
        }

        // Update config with mongo feature
        updateServerConfigWithParams(config, true, true, MongoServlet.JNDI, false, false, "test", "testRemoveAddMongoFeatureFromRunningServer()");
        insertFindScenario(true, MongoServlet.JNDI);
        insertFindScenario(true, MongoServlet.INJECTED);
        insertFindScenario(false, MongoServlet.INJECTED);
        insertFindScenario(false, MongoServlet.JNDI);
        Log.info(c, method, "exiting " + method);
    }

    //@Test
    // -- remove this test till we figure out why the server log sometimes rolls over?
    // -- perhaps move to a separate test so it doesn't have to stop the server.
    //@Ignore
    public void testStartServerNoMongoFeatureAddFeatureToRunningServer() throws Exception {
        final String method = "testStartServerNoMongoFeatureAddFeatureToRunningServer";
        Log.info(c, method, "entering " + method);
        server.stopServer(true); // <- This must account for acceptable errors before re-enabling.
        ServerConfiguration working = config.clone();

        Set<String> features = working.getFeatureManager().getFeatures();
        for (String feature : features) {
            if ("mongodb-2.0".equals(feature)) {
                features.remove(feature);
                break;
            }
        }
        // Update config without mongo feature
        //updateServerConfig(working, true, true, MongoServlet.JNDI, false, false);
        server.updateServerConfiguration(working);
        server.startServer(false);

        // Update config with mongo feature
        updateServerConfig(config, true, true, MongoServlet.JNDI, false, false);

        insertFindScenario(true, MongoServlet.JNDI);
        insertFindScenario(true, MongoServlet.INJECTED);
        insertFindScenario(false, MongoServlet.INJECTED);
        insertFindScenario(false, MongoServlet.JNDI);

        Log.info(c, method, "exiting " + method);
    }

    @Test
    public void testSetAllPropertiesOnRunningServer() throws Exception {
        final String method = "testSetAllPropertiesOnRunningServer";
        Log.info(c, method, "entering " + method);
        ServerConfiguration working = config.clone();
        try {
            for (MongoElement element : working.getMongos()) {
                element.setAutoConnectRetry(true);
                element.setConnectionsPerHost(11);
                element.setConnectTimeout(1100);
                element.setCursorFinalizerEnabled(true);
                element.setDescription("descccccc");
                element.setMaxAutoConnectRetryTime(Long.valueOf(5666));
                element.setMaxWaitTime(1895);
                element.setReadPreference("nearest");
                element.setWriteConcern("ACKNOWLEDGED");
                element.setSocketKeepAlive(false);
                element.setSocketTimeout(1234);
                element.setThreadsAllowedToBlockForConnectionMultiplier(125);
            }
            updateServerConfig(working, true, true, MongoServlet.JNDI, true, true);
            Map<String, String> res = getConfig(MongoServlet.JNDI);

            String value = res.get("autoConnectRetry");
            assertEquals("autoConnectRetry should have been true, but was false.", "true", value);

            value = res.get("connectionsPerHost");
            assertEquals("connectionsPerHost should have been 11, but was " + value, "11", value);

            value = res.get("connectTimeout");
            assertEquals("connectTimeout should have been 1100, but was " + value, "1100", value);

            value = res.get("description");
            assertEquals("description should have been descccccc, but was " + value, "descccccc", value);

            value = res.get("maxAutoConnectRetryTime");
            assertEquals("maxAutoConnectRetryTime should have been 5666, but was " + value, "5666", value);

            value = res.get("maxWaitTime");
            assertEquals("maxWaitTime should have been 1895, but was " + value, "1895", value);

            value = res.get("readPreference");
            assertEquals("readPreference should have been nearest, but was " + value, "nearest", value);

            value = res.get("socketKeepAlive");
            assertEquals("socketKeepAlive should have been false, but was " + value, "false", value);

            value = res.get("socketTimeout");
            assertEquals("socketTimeout should have been 1234, but was " + value, "1234", value);

            value = res.get("threadsAllowedToBlockForConnectionMultiplier");
            assertEquals("threadsAllowedToBlockForConnectionMultiplier should have been 125, but was " + value, "125",
                         value);
        } finally {
            updateServerConfig(config, true, true, MongoServlet.JNDI, true, true);
        }
        Log.info(c, method, "exiting " + method);
    }

    @Test
    @AllowedFFDC({ "java.lang.RuntimeException", "com.ibm.wsspi.injectionengine.InjectionException" })
    public void testVersionOneMongoFailure() throws Exception {
        final String method = "testVersionOneMongoFailure";
        Log.info(c, method, "entering " + method);
        ServerConfiguration working = config.clone();
        try {
            modifyLib(working, "mongo-lib-10");
            updateServerConfig(working, true, true, MongoServlet.JNDI, true, false);
            // CWWKZ0009I: The application mongo has stopped successfully.
            if (server.waitForStringInLogUsingMark("CWWKZ0009I.* mongo") == null) {
                throw new Exception("Server application stop did not complete within the allotted interval");
            }
            // CWWKZ0001I: Application mongo started in 0.9 seconds.
            // CWWKZ0003I: The application mongo updated in 0.3 seconds.
            if (server.waitForStringInLogUsingMark("CWWKZ000[13]I.* mongo") == null) {
                throw new Exception("Server application restart did not complete within the allotted interval");
            }
            try {
                doRequest(MongoServletAction.DUMP_CONFIG, MongoServlet.JNDI, "key", "testVersionOneMongoFailure");
            } catch (Exception e) {
                // expected
            }
            // check the logs for
            if (server.waitForStringInLogUsingMark("CWKKD0013E|CWKKD0023E|CWKKD0017E") == null) {
                throw new Exception("Server exception for an exception indicating the server level was not high enough for standard, SSL or CertAuth");
            }

        } finally {
            updateServerConfig(config, true, true, MongoServlet.JNDI, false, true);
        }
        Log.info(c, method, "exiting " + method);
    }

    @Test
    @AllowedFFDC({ "java.lang.RuntimeException", "com.ibm.wsspi.injectionengine.InjectionException" })
    public void testVersionTwoNineThreeMongoFailure() throws Exception {
        final String method = "testVersionTwoNineThreeMongoFailure";
        Log.info(c, method, "entering " + method);
        ServerConfiguration working = config.clone();
        try {
            modifyLib(working, "mongo-lib-293");
            updateServerConfig(working, true, true, MongoServlet.JNDI, true, false);
            // CWWKZ0009I: The application mongo has stopped successfully.
            if (server.waitForStringInLogUsingMark("CWWKZ0009I.* mongo") == null) {
                throw new Exception("Server application stop did not complete within the allotted interval");
            }
            // CWWKZ0001I: Application mongo started in 0.9 seconds.
            // CWWKZ0003I: The application mongo updated in 0.3 seconds.
            if (server.waitForStringInLogUsingMark("CWWKZ000[13]I.* mongo") == null) {
                throw new Exception("Server application restart did not complete within the allotted interval");
            }
            try {
                doRequest(MongoServletAction.DUMP_CONFIG, MongoServlet.JNDI, "key",
                          "testVersionTwoNineThreeMongoFailure");
            } catch (Exception e) {
                // expected
            }
            // check the logs for
            if (server.waitForStringInLogUsingMark("CWKKD0013E|CWKKD0023E|CWKKD0017E") == null) {
                throw new Exception("Server exception for an exception indicating the server level was not high enough for standard, SSL or CertAuth");
            }

        } finally {
            updateServerConfig(config, true, true, MongoServlet.JNDI, false, true);
        }
        Log.info(c, method, "exiting " + method);
    }

}
