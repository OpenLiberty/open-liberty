/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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
package com.ibm.ws.cloudant.fat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.SimpleLogConsumer;
import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(Suite.class)
@SuiteClasses({
                CloudantDemoTest.class,
                CloudantTest.class,
                CloudantTestOutboundSSL.class,
                CloudantModifyConfigTest.class
})
public class FATSuite extends TestContainerSuite {

    private static final Class<?> c = FATSuite.class;

    //TODO Start using ImageBuilder
//    private static final DockerImageName COUCHDB_SSL = ImageBuilder.build("couchdb-ssl:3").getDockerImageName();

    private static final DockerImageName COUCHDB_SSL = DockerImageName.parse("kyleaure/couchdb-ssl:1.0");

    @ClassRule
    public static CouchDBContainer cloudant = new CouchDBContainer(COUCHDB_SSL)
                    .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "couchdb-ssl"));

    protected static void createKeystore(String destination, String serverCert) {
        final String m = "createKeystore";

        String[] command = new String[] {
                                          "keytool", "-import", //
                                          "-alias", "testcontainers", //
                                          "-file", serverCert, //
                                          "-keystore", destination, //
                                          "-storetype", "jks", //
                                          "-storepass", "liberty", //
                                          "-noprompt"
        };

        String errorPrelude = "Could not create client keystore: " + destination;
        try {
            Process p = Runtime.getRuntime().exec(command);
            if (!p.waitFor(FATRunner.FAT_TEST_LOCALRUN ? 10 : 20, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                dumpOutput(m, "Keytool process timed out", p);
                throw new RuntimeException(errorPrelude + " timed out waiting for process to finish.");
            }
            if (p.exitValue() != 0) {
                dumpOutput(m, "Non 0 exit code from keytool", p);
                throw new RuntimeException(errorPrelude + " see logs for details");
            }
            dumpOutput(m, "Keytool command completed successfully", p);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(errorPrelude, e);
        }
    }

    private static void dumpOutput(String method, String message, Process p) {
        String out = "stdOut:" + System.lineSeparator() + readInputStream(p.getInputStream());
        String err = "stdErr:" + System.lineSeparator() + readInputStream(p.getErrorStream());
        Log.info(c, method, message + //
                            System.lineSeparator() + out + //
                            System.lineSeparator() + err);
    }

    private static String readInputStream(InputStream is) {
        @SuppressWarnings("resource")
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}