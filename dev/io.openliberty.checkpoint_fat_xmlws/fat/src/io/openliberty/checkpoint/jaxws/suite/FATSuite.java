/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package io.openliberty.checkpoint.jaxws.suite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.checkpoint.jaxws.fat.EJBWSBasicTest;
import io.openliberty.checkpoint.jaxws.fat.LibertyCXFPositivePropertiesTest;
import io.openliberty.checkpoint.jaxws.fat.WebServiceRefTest;

/**
 * Collection of all example tests
 */
@RunWith(Suite.class)
/*
 * The classes specified in the @SuiteClasses annotation
 * below should represent all of the test cases for this FAT.
 */
@SuiteClasses({
                AlwaysPassesTest.class,
                EJBWSBasicTest.class,
                WebServiceRefTest.class,
                LibertyCXFPositivePropertiesTest.class
})
public class FATSuite {
    public static void configureEnvVariable(LibertyServer server, Map<String, String> newEnv) {
        try {
            Properties serverEnvProperties = new Properties();
            serverEnvProperties.putAll(newEnv);
            File serverEnvFile = new File(server.getFileFromLibertyServerRoot("server.env").getAbsolutePath());
            try (OutputStream out = new FileOutputStream(serverEnvFile)) {
                serverEnvProperties.store(out, "");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static RepeatTests defaultRepeat(String serverName) {
        return RepeatTests.withoutModification()
                        .andWith(new JakartaEE9Action().forServers(serverName).fullFATOnly())
                        .andWith(new JakartaEE10Action().forServers(serverName).fullFATOnly());
    }
}
