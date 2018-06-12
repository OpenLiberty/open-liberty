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

import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ClassloaderElement;
import com.ibm.websphere.simplicity.config.FeatureManager;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class TestMongoDbNoSSLFeature extends AbstractMongoTestCase {
    private static final Class<?> c = TestMongoDbNoSSLFeature.class;

    @BeforeClass
    public static void b() throws Exception {
        before("com.ibm.ws.mongo.fat.server.no.ssl.feature");
    }

    @AfterClass
    public static void a() throws Exception {
        after("CWKKD0015E:.*",
              "CWWKE0701E" // TODO: Circular reference detected trying to get service {org.osgi.service.cm.ManagedServiceFactory, com.ibm.wsspi.logging.Introspector, com.ibm.ws.runtime.update.RuntimeUpdateListener, com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator}
        );
    }

    @Test
    @AllowedFFDC({ "java.lang.RuntimeException", "com.ibm.wsspi.injectionengine.InjectionException", "javax.servlet.UnavailableException" })
    public void testInsertFindNoSslFeature() throws Exception {
        final String method = "testInsertFindNoSslFeature";
        Log.info(c, method, "entering " + method);
        try {
            server.setMarkToEndOfLog();
            insertFindScenario(true, MongoServlet.NO_SSL_FEATURE);
            fail("Expected Exception when trying to create a mongo ssl connection without the ssl-1.0 feature");
        } catch (Exception e) {
        }
        //Check that warning about no ssl feature appears
        if (server.waitForStringInLogUsingMark("CWKKD0015E") == null) {
            fail("Server exception for error CWKKD0015E was not found within the allotted interval");
        }

        // set mark to end of trace log
        RemoteFile rf = server.getMatchingLogFile("trace.log");
        server.setMarkToEndOfLog(rf);

        // now add ssl-1.0 and ensure the problem is fixed
        ServerConfiguration working = config.clone();
        try {
            FeatureManager fm = working.getFeatureManager();
            Set<String> features = fm.getFeatures();
            features.add(FeatureManager.FEATURE_SSL_1_0);
            updateServerConfig(working, true, true, MongoServlet.NO_SSL_FEATURE, false, false);

            // Just because SSL feature is reported as starting doesn't mean it is really ready,
            // nor that Mongo is functional, so wait for MongoDBService to activate and register
            // with JNDI (the order may vary, so need to watch for both). Note, it is still
            // possible the second and third searches to find something in the logs earlier than
            // the search for "sslRef set", so we may need to start comparing order of lines found.
            long waitFor = FATRunner.FAT_TEST_LOCALRUN ? 7000 : 30000;
            server.waitForStringInTraceUsingMark("MongoService * 3 sslRef set to ssl", waitFor);
            server.waitForStringInTraceUsingMark("MongoDBService * < activate", waitFor);
            server.waitForStringInTraceUsingMark("ResourceFactoryTracker * < addingService", waitFor);

            try {
                server.setMarkToEndOfLog();
                insertFindScenario(true, MongoServlet.NO_SSL_FEATURE);
            } catch (Exception e) {
                fail("Exception thrown after adding ssl-1.0: " + e.toString());
            }
        } finally {
            updateServerConfig(config, true, true, MongoServlet.NO_SSL_FEATURE, false, false);
        }
        Log.info(c, method, "exiting " + method);
    }

    @Override
    protected void updateApplication(ServerConfiguration sc, String libName) {
        for (Application app : sc.getApplications()) {
            if ((AbstractMongoTestCase.APP_NAME + ".war").equals(app.getLocation())) {
                ClassloaderElement cl = app.getClassloader();
                Set<String> refs = cl.getCommonLibraryRefs();
                refs.clear();
                refs.add(libName);
            }
        }
    }
}
