/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.microprofile.rest.client.tck;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@RunWith(FATRunner.class)
public class RestClientTckPackageTest {

    @Server("FATServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
    	String javaVersion = System.getProperty("java.version");
    	Log.info(RestClientTckPackageTest.class, "setup", "javaVersion: " + javaVersion);
    	System.out.println("java.version = " + javaVersion);
    	if (javaVersion.startsWith("1.8")) {
    		Path cwd = Paths.get(".");
    		Log.info(RestClientTckPackageTest.class, "setup", "cwd = " +  cwd.toAbsolutePath());
    		Path java8File = Paths.get("publish/tckRunner/tck/tck-suite.xml-java8");
    	    Path tckSuiteFile = Paths.get("publish/tckRunner/tck/tck-suite.xml");
    	    Files.copy(java8File, tckSuiteFile, StandardCopyOption.REPLACE_EXISTING);
    	}
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.postStopServerArchive(); // must explicitly collect since arquillian is starting/stopping the server
//            server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWWKZ0002E");
        }
    }

    @Test
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void testRestClientTck() throws Exception {
        MvnUtils.runTCKMvnCmd(server, "io.openliberty.microprofile.rest.client.2.0.internal_fat_tck", this.getClass() + ":testRestClientTck");
    }

}
