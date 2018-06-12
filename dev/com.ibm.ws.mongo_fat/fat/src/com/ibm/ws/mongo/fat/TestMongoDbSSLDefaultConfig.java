/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
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
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.MongoElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;

public class TestMongoDbSSLDefaultConfig extends AbstractMongoTestCase {
    private static final Class<?> c = TestMongoDbSSLDefaultConfig.class;

    @BeforeClass
    public static void b() throws Exception {
        before("com.ibm.ws.mongo.fat.server.ssl.default.config");
    }

    @AfterClass
    public static void a() throws Exception {
        after("SRVE0777E.*com.mongodb.CommandResult",
              "SRVE0777E.*com.mongodb.CommandFailureException",
              "SRVE0315E.*com.mongodb.CommandFailureException");
    }

    @Test
    public void testInsertFindNoSslRef() throws Exception {
        final String method = "testInsertFindNoSslRef";
        Log.info(c, method, "entering " + method);
        insertFindScenario(true, MongoServlet.NO_SSLREF);
        Log.info(c, method, "exiting " + method);
    }

    @AllowedFFDC({ "com.mongodb.CommandFailureException" })
    @Test
    public void testCertAuthUseDefaultSSL() throws Exception {
        final String method = "testCertAuthUseDefaultSSL";
        Log.info(c, method, "entering " + method);

        try {
            insertFindScenario(true, MongoServlet.CERT_INVALID_SCENARIOS);
            fail("Before changing config, servlet should not work");
        } catch (IOException ex) {
            // expected
        }

        ServerConfiguration working = config.clone();
        try {
            ConfigElementList<MongoElement> mongos = working.getMongos();
            for (MongoElement mongo : mongos) {
                if (mongo.getId().equals("mongo-invalid-certauth-scenarios")) {
                    // remove sslRef so we default to defaultSSL (which is configured to work correctly)
                    mongo.setSslRef(null);
                }
            }
            updateServerConfig(working, true, true, MongoServlet.CERT_INVALID_SCENARIOS, false, true);
            insertFindScenario(true, MongoServlet.CERT_INVALID_SCENARIOS);

        } finally {
            updateServerConfig(config, true, true, MongoServlet.CERT_INVALID_SCENARIOS, false, true);
        }
        Log.info(c, method, "exiting " + method);
    }

    @Override
    protected void updateApplication(ServerConfiguration sc, String libName) {
        for (Application app : sc.getApplications()) {
            if ("mongo.war".equals(app.getLocation())) {
                ClassloaderElement cl = app.getClassloader();
                Set<String> refs = cl.getCommonLibraryRefs();
                refs.clear();
                refs.add(libName);
            }
        }
    }
}
