/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
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
package com.ibm.ws.microprofile.reactive.messaging.fat.suite;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.ibm.ws.microprofile.reactive.messaging.fat.kafka.containers.ExtendedKafkaContainer;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class KafkaUtils {

    public static final String TRUSTSTORE_FILENAME = "kafka-truststore.jks";

    public static File[] kafkaClientLibs() {
        File libsDir = new File("lib/LibertyFATTestFiles/libs");
        return libsDir.listFiles();
    }

    public static URL kafkaPermissions() {
        return KafkaUtils.class.getResource("permissions.xml");
    }

    public static void copyTrustStore(ExtendedKafkaContainer container, LibertyServer server) throws Exception {
        copyFileToServer(container.getKeystoreFile(), server);
    }

    private static void copyFileToServer(File file, LibertyServer server) throws Exception {
        // Easiest to copy it to the desired filename, then copy it to the server
        Path tmpDest = Paths.get(TRUSTSTORE_FILENAME);
        Files.copy(file.toPath(), tmpDest, StandardCopyOption.REPLACE_EXISTING);
        server.copyFileToLibertyServerRootUsingTmp(server.getServerRoot(), tmpDest.toString());
        Files.delete(tmpDest);
    }

}
