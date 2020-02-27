/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.persistent.fat.errorpaths;

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ProgramOutput;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({
    PersistentExecutorErrorPathsTest.class,
    PersistentExecutorErrorPathsTestWithFailoverAndPollingEnabled.class,
    PersistentExecutorErrorPathsTestWithFailoverEnabledNoPolling.class
    })
public class FATSuite {
    static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.concurrent.persistent.fat.errorpaths");

    /**
     * Utility method to dump the server and collect the persistent executor introspector output.
     *
     * @return list of lines of the persistent executor introspector output.
     */
    static List<String> persistentExecutorIntrospectorDump() throws Exception {
        ProgramOutput output = server.serverDump();
        assertEquals(0, output.getReturnCode());
        assertEquals("", output.getStderr());

        // Parse standard output. Examples:
        // Server com.ibm.ws.session.cache.fat.config.infinispan dump complete in /Users/user/lgit/open-liberty/dev/build.image/wlp/usr/servers/com.ibm.ws.session.cache.fat.config.infinispan/com.ibm.ws.session.cache.fat.config.infinispan.dump-18.04.11_14.30.55.zip.
        // Server com.ibm.ws.session.cache.fat.config.infinispan dump complete in C:\\jazz-build-engines\\wasrtc-proxy.hursley.ibm.com\\EBC.PROD.WASRTC\\build\\dev\\image\\output\\wlp\\usr\\servers\\com.ibm.ws.session.cache.fat.config.infinispan\\com.ibm.ws.session.cache.fat.config.infinispan.dump-18.06.10_00.16.59.zip.

        String out = output.getStdout();
        int end = out.lastIndexOf('.');
        int begin = out.lastIndexOf(' ', end) + 1;

        String dumpFileName = out.substring(begin, end);

        System.out.println("Dump file name: " + dumpFileName);

        // Example of file within the zip:
        // dump_18.04.11_14.30.55/introspections/PersistentExecutorIntrospector.txt

        end = dumpFileName.indexOf(".zip");
        String prefix = "com.ibm.ws.concurrent.persistent.fat.errorpaths.dump-";
        begin = dumpFileName.lastIndexOf(prefix, end) + prefix.length();

        String introspectorFileName = "dump_" + dumpFileName.substring(begin, end) + "/introspections/PersistentExecutorIntrospector.txt";

        System.out.println("Looking for intropspector entry: " + introspectorFileName);

        List<String> lines = new ArrayList<String>();
        try (ZipFile dumpFile = new ZipFile(dumpFileName)) {
            ZipEntry entry = dumpFile.getEntry(introspectorFileName);
            System.out.println("Found: " + entry);
            try (BufferedInputStream in = new BufferedInputStream(dumpFile.getInputStream(entry))) {
                for (Scanner scanner = new Scanner(in); scanner.hasNextLine();) {
                    String line = scanner.nextLine();
                    System.out.println(line);
                    lines.add(line);
                }
            }
        }

        return lines;
    }
}