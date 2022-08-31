/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.validator.fat;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EE9PackageReplacementHelper;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(Suite.class)
@SuiteClasses({
                ValidateDataSourceTest.class,
                ValidateJCATest.class,
                ValidateJMSTest.class,
                ValidateDSCustomLoginModuleTest.class,
                ValidateOpenApiSchemaTest.class
})

public class FATSuite {

    private static final EE9PackageReplacementHelper packageReplacementHelper = new EE9PackageReplacementHelper();

    @ClassRule
    public static RepeatTests r1 = MicroProfileActions.repeat(null, TestMode.FULL,
                                                              MicroProfileActions.MP60,
                                                              MicroProfileActions.MP50, // EE9
                                                              MicroProfileActions.MP40, // EE8
                                                              MicroProfileActions.MP30,
                                                              MicroProfileActions.MP20);

    @BeforeClass
    public static void setup() throws Exception {
        HttpUtils.trustAllCertificates();
        HttpUtils.trustAllHostnames();
        HttpUtils.setDefaultAuth("adminuser", "adminpwd");
    }

    public static void setupServerSideAnnotations(LibertyServer server) {
        if (JakartaEE9Action.isActive() || JakartaEE10Action.isActive()) {
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

    public static void assertClassEquals(String message, String expected, String actual) {
        if (JakartaEE9Action.isActive() || JakartaEE10Action.isActive()) {
            expected = packageReplacementHelper.replacePackages(expected);
        }
        assertEquals(message, expected, actual);
    }

    public static String expectedJmsProviderSpecVersion() {
        if (JakartaEE10Action.isActive()) {
            return "3.1";
        } else if (JakartaEE9Action.isActive()) {
            return "3.0";
        } else {
            return "2.0";
        }
    }
}