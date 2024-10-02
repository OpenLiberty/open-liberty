/*******************************************************************************
 * Copyright (c) 2013, 2024 IBM Corporation and others.
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
package io.openliberty.checkpoint.messaging.JMS20.fat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.topology.impl.LibertyServer;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                BucketSet1CpClientTest.class,
                BucketSet2CpClientTest.class,
                BucketSet1CpEngineTest.class,
                BucketSet2CpEngineTest.class,
                JMSMDBTest.class

})

public class FATSuite {

    public static void addServerEnvPorts(LibertyServer server, List<PortSetting> portSettings, Map<String, String> envSettings) {

        File serverEnvFile = new File(server.getServerRoot() + "/server.env");
        try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile, true))) {
            portSettings.forEach((setting) -> {
                Integer port = Integer.getInteger(setting.lookupName, setting.defaultValue);
                System.out.println("Setting port " + setting.newEnvName + "=" + port + " for lookup name " +
                                   setting.lookupName + " defaults to " + setting.defaultValue);
                serverEnvWriter.println(setting.newEnvName + "=" + port);
            });
            envSettings.forEach((k, v) -> serverEnvWriter.println(k + "=" + v));
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Data transfer object to associated ports with property names */
    public static class PortSetting {
        /**
         *
         * @param name    lookup por configured in testports.properties
         * @param def     default value if lookup fails
         * @param newName Property name to define in server.env
         */
        PortSetting(String name, int def, String newName) {
            lookupName = name;
            defaultValue = def;
            newEnvName = newName;
        }

        public String lookupName;
        public int defaultValue;
        public String newEnvName;
    }
}
