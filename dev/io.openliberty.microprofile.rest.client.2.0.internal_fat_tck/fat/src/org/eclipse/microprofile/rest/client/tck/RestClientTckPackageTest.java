/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.microprofile.rest.client.tck;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 * There is a detailed output on specific
 */
@RunWith(FATRunner.class)
public class RestClientTckPackageTest {
	private static final Class<?> c = RestClientTckPackageTest.class;

	@ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
    						                 .andWith(new SeRestClientReplacementAction());

    @Server("FATServer")
    public static LibertyServer server;

    private static List<String> standaloneJars = new ArrayList<>();
    @BeforeClass
    public static void setup() throws Exception {
    	String javaVersion = System.getProperty("java.version");
    	Log.info(c, "setup", "javaVersion: " + javaVersion);
    	Path cwd = Paths.get(".");
		Log.info(c, "setup", "cwd = " +  cwd.toAbsolutePath());
		if (SeRestClientReplacementAction.isActive()) {
    		Path seTckFile = Paths.get("publish/tckRunner/tck/tck-suite.xml-seRestClient");
    	    Path tckSuiteFile = Paths.get("publish/tckRunner/tck/tck-suite.xml");
    	    Log.info(c, "setup", "Copying " + seTckFile + " to " + tckSuiteFile);
    	    Files.copy(seTckFile, tckSuiteFile, StandardCopyOption.REPLACE_EXISTING);

    	    Path standaloneDir = Paths.get("publish/files");
    	    try (DirectoryStream<Path> stream = Files.newDirectoryStream(standaloneDir, "io.openliberty.standalone*jar")) {
    	    	for (Path src : stream) {
    	    		Log.info(c, "setup", "Copying " + src + " to lib/global");
    	    		server.copyFileToLibertyServerRoot("lib/global", src.getFileName().toString());
    	    		standaloneJars.add(src.getFileName().toString());
    	    	}
    	    }
    	} else {
    		if (javaVersion.startsWith("1.8")) {
        		Path java8File = Paths.get("publish/tckRunner/tck/tck-suite.xml-java8");
        	    Path tckSuiteFile = Paths.get("publish/tckRunner/tck/tck-suite.xml");
        	    Log.info(c, "setup", "Copying " + java8File + " to " + tckSuiteFile);
        	    Files.copy(java8File, tckSuiteFile, StandardCopyOption.REPLACE_EXISTING);
        	} else {
        		Path java11File = Paths.get("publish/tckRunner/tck/tck-suite.xml-java11");
        	    Path tckSuiteFile = Paths.get("publish/tckRunner/tck/tck-suite.xml");
        	    Log.info(c, "setup", "Copying " + java11File + " to " + tckSuiteFile);
        	    Files.copy(java11File, tckSuiteFile, StandardCopyOption.REPLACE_EXISTING);
        	}
    	}
    }

    @AfterClass
    public static void tearDown() throws Exception {
    	if (SeRestClientReplacementAction.isActive()) {
    		for (String file : standaloneJars) {
    			server.deleteFileFromLibertyServerRoot("lib/global/" + file);
    		}
    		
    		Path globalLibDir = Paths.get("publish/servers/FATServer/lib/global");
    	    try (DirectoryStream<Path> stream = Files.newDirectoryStream(globalLibDir, "io.openliberty.standalone*jar")) {
    	    	for (Path src : stream) {
    	    		Log.info(c, "tearDown", "Deleting " + src);
    	    		
    	    		Files.delete(src);
    	    	}
    	    }
    	}

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
