/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.grpc;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * @author anupag
 *
 */
@RunWith(FATRunner.class)
public class StoreServicesTests extends FATServletClient {

    protected static final Class<?> c = StoreServicesTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("StoreServer")
    public static LibertyServer storeServer;

    @Server("ProducerServer")
    public static LibertyServer producerServer;

    @Server("ConsumerServer")
    public static LibertyServer consumerServer;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(storeServer, "StoreApp.war",
                                      "com.ibm.testapp.g3store.cache",
                                      "com.ibm.testapp.g3store.exception",
                                      "com.ibm.testapp.g3store.interceptor",
                                      "com.ibm.testapp.g3store.serviceImpl",
                                      "com.ibm.testapp.g3store.servletStore",
                                      "com.ibm.testapp.g3store.utilsStore",
                                      "com.ibm.test.g3store.grpc"); // add generated src

        ShrinkHelper.defaultDropinApp(producerServer, "StoreProducerApp.war",
                                      "com.ibm.testapp.g3store.grpcProducer.api",
                                      "com.ibm.testapp.g3store.exception",
                                      "com.ibm.testapp.g3store.restProducer",
                                      "com.ibm.testapp.g3store.restProducer.api",
                                      "com.ibm.testapp.g3store.restProducer.model",
                                      "com.ibm.testapp.g3store.servletProducer",
                                      "com.ibm.test.g3store.grpc"); // add generated src

        // since the <application> element is used in server.xml for security, cannot use dropin
        ShrinkHelper.defaultApp(consumerServer, "StoreConsumerApp.war",
                                "com.ibm.testapp.g3store.grpcConsumer.api",
                                "com.ibm.testapp.g3store.grpcConsumer.security",
                                "com.ibm.testapp.g3store.exception",
                                "com.ibm.testapp.g3store.restConsumer",
                                "com.ibm.testapp.g3store.restConsumer.api",
                                "com.ibm.testapp.g3store.restConsumer.model",
                                "com.ibm.testapp.g3store.servletConsumer",
                                "com.ibm.testapp.g3store.utilsConsumer",
                                "com.ibm.test.g3store.grpc"); // add generated src

        storeServer.startServer(StoreServicesTests.class.getSimpleName() + ".log");
        producerServer.startServer(StoreServicesTests.class.getSimpleName() + ".log");
        consumerServer.startServer(StoreServicesTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        storeServer.stopServer();
        producerServer.stopServer();
        consumerServer.stopServer();

    }

    @Test
    public void testStoreWarStartWithGrpcService() throws Exception {
        Log.info(getClass(), "testStoreWarStartWithGrpcService", "Check if Store.war started");
        assertNotNull(storeServer.waitForStringInLog("CWWKZ0001I: Application StoreApp started"));

    }

    @Test
    public void testProducerWarStartWithGrpcService() throws Exception {
        Log.info(getClass(), "testProducerWarStartWithGrpcService", "Check if Prodcuer.war started");
        assertNotNull(producerServer.waitForStringInLog("CWWKZ0001I: Application StoreProducerApp started"));

    }

    @Test
    public void testConsumerWarStartWithGrpcService() throws Exception {
        Log.info(getClass(), "testConsumerWarStartWithGrpcService", "Check if Consumer.war started");
        assertNotNull(consumerServer.waitForStringInLog("CWWKZ0001I: Application StoreConsumerApp started"));

    }

}
