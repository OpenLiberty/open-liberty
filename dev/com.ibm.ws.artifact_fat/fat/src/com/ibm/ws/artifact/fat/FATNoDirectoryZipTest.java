/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.artifact.fat;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import nodirzip.fat.test.app.NoDirectoryZipServlet;

@RunWith(FATRunner.class)
public class FATNoDirectoryZipTest extends FATServletClient {

    public static final String APP_NAME = "noDirZip";
    public static final String APP_PACKAGE = NoDirectoryZipServlet.class.getPackage().getName();

    @Server("com.ibm.ws.artifact.nodir.zip")
    @TestServlet(servlet = NoDirectoryZipServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void createTestApp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, new DeployOptions[] { DeployOptions.OVERWRITE }, APP_PACKAGE);
        RemoteFile appFile = server.getFileFromLibertyServerRoot("apps/" + APP_NAME + ".war");
        // backup the original appFile which contains directory entries
        RemoteFile backupFile = server.getMachine().getFile(server.getFileFromLibertyServerRoot("apps"), "backup.war");
        appFile.rename(backupFile);

        // read the backup and write it back to the original, with no directory entries
        byte[] buf = new byte[1024];
        try (ZipOutputStream zipOut = new ZipOutputStream(appFile.openForWriting(false));
                ZipInputStream zipIn = new ZipInputStream(backupFile.openForReading())) {
            ZipEntry zipEntry;
            while ((zipEntry = zipIn.getNextEntry()) != null) {
                if (!zipEntry.isDirectory()) {
                    // only put non-directory entries in the final appFile
                    zipOut.putNextEntry(zipEntry);
                    int len;
                    while((len = zipIn.read(buf)) > 0) {
                        zipOut.write(buf, 0, len);
                    }
                }
            }
            // Add a library file with a single resource in the application package; also no directory entries
            zipOut.putNextEntry(new ZipEntry("WEB-INF/lib/inner.jar"));
            ByteArrayOutputStream saved;
            try (ByteArrayOutputStream innerBytes = saved  = new ByteArrayOutputStream();
                    ZipOutputStream innerZipOut = new ZipOutputStream(innerBytes)) {
                innerZipOut.putNextEntry(new ZipEntry(APP_PACKAGE.replace('.', '/') + "/test.txt"));
                innerZipOut.write("testing".getBytes());
            }
            zipOut.write(saved.toByteArray());
        }
        // Final appFile (noDirZip.war) has the following entries:
        // WEB-INF/classes/nodirzip/fat/test/app/NoDirectoryZipServlet.class
        // WEB-INF/lib/inner.jar
        // The inner.jar has the following entry:
        // nodirzip/fat/test/app/test.txt
    }

    @Before
    public void setUp() throws Exception {
        server.startServer(testName.getMethodName() + ".log");
    }

    @After
    public void checkForWarnings() throws Exception {
        // There are 4 sub-package (directory) elements in the application package nodirzip.fat.test.app
        // The root WAR and the inner.jar both have no directories and hava content in this package.
        // The test drives resource requests multiple times for each sub-package/folder which will
        // search the root WAR and the inner.jar multiple times.
        // The warning message is only displayed once for each directory request so we expect:
        // 8 warnings = 4 (directories) * 2 (archives with no directory entries)
        List<String> warnings = server.findStringsInLogs("CWWKM0129W");
        assertEquals("Wrong number of warnings", 8, warnings.size());
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKM0129W");
    }

}
