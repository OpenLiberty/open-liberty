/*******************************************************************************
 * Copyright (c) 2017,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.config.fat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(Suite.class)
@SuiteClasses({
                ConfigRESTHandlerAppDefinedResourcesTest.class,
                ConfigRESTHandlerJCATest.class,
                ConfigRESTHandlerJMSTest.class,
                ConfigRESTHandlerTest.class,
                ConfigOpenApiSchemaTest.class
})

public class FATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification() // run all tests as-is (e.g. EE8 features)
                    .andWith(new JakartaEE9Action()); // run all tests again with EE9 features+packages

    @BeforeClass
    public static void setup() throws Exception {
        HttpUtils.trustAllCertificates();
        HttpUtils.trustAllHostnames();
        HttpUtils.setDefaultAuth("adminuser", "adminpwd");
    }

    public static void setupServerSideAnnotations(LibertyServer server) {
        if (JakartaEE9Action.isActive()) {
            server.addEnvVar("CONNECTION_FACTORY", "jakarta.resource.cci.ConnectionFactory");
            server.addEnvVar("QUEUE_FACTORY", "jakarta.jms.QueueConnectionFactory");
            server.addEnvVar("TOPIC_FACTORY", "jakarta.jms.TopicConnectionFactory");
            server.addEnvVar("QUEUE_INTERFACE", "jakarta.jms.Queue");
            server.addEnvVar("TOPIC_INTERFACE", "jakarta.jms.Topic");
            server.addEnvVar("DESTINATION_INTERFACE", "jakarta.jms.Destination");
        } else {
            server.addEnvVar("CONNECTION_FACTORY", "javax.resource.cci.ConnectionFactory");
            server.addEnvVar("QUEUE_FACTORY", "javax.jms.QueueConnectionFactory");
            server.addEnvVar("TOPIC_FACTORY", "javax.jms.TopicConnectionFactory");
            server.addEnvVar("QUEUE_INTERFACE", "javax.jms.Queue");
            server.addEnvVar("TOPIC_INTERFACE", "javax.jms.Topic");
            server.addEnvVar("DESTINATION_INTERFACE", "javax.jms.Destination");
        }
    }

}