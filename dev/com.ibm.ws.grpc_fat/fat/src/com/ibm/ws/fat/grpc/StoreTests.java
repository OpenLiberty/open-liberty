/**
 *
 */
package com.ibm.ws.fat.grpc;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class StoreTests extends FATServletClient {

    protected static final Class<?> c = StoreTests.class;

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
                                      "com.ibm.testapp.g3store.utilsStore");

        ShrinkHelper.defaultDropinApp(producerServer, "StoreProducerApp.war",
                                      "com.ibm.testapp.g3store.grpcProducer.api",
                                      "com.ibm.testapp.g3store.exception",
                                      "com.ibm.testapp.g3store.restProducer",
                                      "com.ibm.testapp.g3store.restProducer.api",
                                      "com.ibm.testapp.g3store.restProducer.model",
                                      "com.ibm.testapp.g3store.servletProducer");

        ShrinkHelper.defaultDropinApp(producerServer, "StoreConsumerApp.war",
                                      "com.ibm.testapp.g3store.grpcConsumer.api",
                                      "com.ibm.testapp.g3store.grpcConsumer.security",
                                      "com.ibm.testapp.g3store.exception",
                                      "com.ibm.testapp.g3store.restConsumer",
                                      "com.ibm.testapp.g3store.restConsumer.api",
                                      "com.ibm.testapp.g3store.restConsumer.model",
                                      "com.ibm.testapp.g3store.servletConsumer",
                                      "com.ibm.testapp.g3store.utilsConsumer");

        storeServer.startServer(StoreTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        storeServer.stopServer();
    }

}
