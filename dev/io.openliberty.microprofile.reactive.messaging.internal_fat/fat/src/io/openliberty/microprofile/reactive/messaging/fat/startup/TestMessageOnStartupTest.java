/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.fat.startup;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties.simpleIncomingChannel;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaClientLibs;
import static com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.KafkaUtils.kafkaPermissions;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.common.ConnectorProperties;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.AbstractReceptionBean;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaTestClient;
import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.framework.KafkaWriter;
import com.ibm.ws.microprofile.reactive.messaging.fat.repeats.ReactiveMessagingActions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.reactive.messaging.fat.apps.startup.TestMessageOnStartupServlet;
import io.openliberty.microprofile.reactive.messaging.fat.suite.KafkaTests;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class TestMessageOnStartupTest extends FATServletClient {

    public static final String APP_NAME = "TestMessageOnStartup";
    public static final String SERVER_NAME = "TestMessageOnStartupServer";
    public static final String TOPIC_NAME = APP_NAME;

    @Server(SERVER_NAME)
    @TestServlet(contextRoot = APP_NAME, servlet = TestMessageOnStartupServlet.class)
    public static LibertyServer server;

    @ClassRule
    public static final RepeatTests r = ReactiveMessagingActions.reactive30Repeats(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        // Send five messages to the topic before the server starts, so they are
        // waiting in the queue when MyKafkaConsumer.consume begins processing.
        KafkaTestClient kafkaTestClient = new KafkaTestClient(KafkaTests.kafkaContainer.getBootstrapServers());
        try (KafkaWriter<String, String> writer = kafkaTestClient.writerFor(TOPIC_NAME)) {
            for (int i = 1; i <= 5; i++) {
                writer.sendMessage("test message " + i);
            }
        }

        ConnectorProperties inputConfig = simpleIncomingChannel(KafkaTests.connectionProperties(),
                                                                APP_NAME,
                                                                APP_NAME);

        PropertiesAsset appConfig = new PropertiesAsset()
                        .include(inputConfig);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsLibraries(kafkaClientLibs())
                        .addPackage(TestMessageOnStartupServlet.class.getPackage())
                        .addPackage(AbstractReceptionBean.class.getPackage())
                        .addAsResource(appConfig, "META-INF/microprofile-config.properties");
                        
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, APP_NAME + ".jar")
                        .addPackage(io.openliberty.microprofile.reactive.messaging.fat.apps.startup.extension.DelayingCDIExtension.class.getPackage());

        CDIArchiveHelper.addJakartaCDIExtensionService(jar, io.openliberty.microprofile.reactive.messaging.fat.apps.startup.extension.DelayingCDIExtension.class);

        String applicationXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<application version=\"9\" xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"\n" +
                                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                "    xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee\n" +
                                "    http://xmlns.jcp.org/xml/ns/javaee/application_9.xsd\">\n" +
                                "  <application-name>TestMessageOnStartupDefinedInEar</application-name>\n" +
                                "  <module>\n" +
                                "    <web>\n" +
                                "      <web-uri>" + APP_NAME + ".war</web-uri>\n" +
                                "      <context-root>/" + APP_NAME + "</context-root>\n" +
                                "    </web>\n" +
                                "  </module>\n" +
                                "</application>";

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear")
                        .addAsModule(war)
                        .addAsLibrary(jar)
                        .addAsManifestResource(kafkaPermissions(), "permissions.xml")
                        .addAsResource(new org.jboss.shrinkwrap.api.asset.StringAsset(applicationXml), "META-INF/application.xml");

        ShrinkHelper.exportAppToServer(server, ear, SERVER_ONLY, DeployOptions.DISABLE_VALIDATION);//we're testing tolerance for custom names so this needs to be manual.
        server.addInstalledAppForValidation("TestMessageOnStartupDefinedInServerXML");
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

}
