/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.config.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.rest.handler.config.fat.audit.ConfigRESTHandlerAuditTest;
import com.ibm.ws.rest.handler.config.fat.audit.ConfigRestHandlerAuditFeatureTest;

import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpsRequest;

@RunWith(Suite.class)
@SuiteClasses({
                ConfigRESTHandlerAppDefinedResourcesTest.class,
                ConfigRESTHandlerJCATest.class,
                ConfigRESTHandlerJMSTest.class,
                ConfigRESTHandlerTest.class,
                ConfigOpenApiSchemaTest.class,
                ConfigRESTHandlerAuditTest.class,
                ConfigRestHandlerAuditFeatureTest.class
})

public class FATSuite {

    public static HttpsRequest createHttpsRequestWithAdminUser(LibertyServer server, String path) {
        return new HttpsRequest(server, path).allowInsecure().basicAuth("adminuser", "adminpwd");
    }

    public static void setupServerSideAnnotations(LibertyServer server) {
        if (JakartaEEAction.isEE9OrLaterActive()) {
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
