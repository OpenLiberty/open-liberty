/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package com.ibm.ws.transaction.test.tests;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Transaction;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.Server;
import componenttest.annotation.SkipIfSysProp;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import servlets.BadLogServlet;

// Skip on IBMI as test servers run with elevated permissions and, consequently, can write anywhere
@SkipIfSysProp(SkipIfSysProp.OS_IBMI)
@RunWith(FATRunner.class)
public class BadLogTest extends FATServletClient {

    public static final String APP_NAME = "transaction";

    @Server("com.ibm.ws.transaction_badlog")
    @TestServlet(servlet = BadLogServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    /** Directory whose parent cannot be created; used as the tranlog location. */
    private static String unwritablePath;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "servlets.*");

        // Point the transaction log at a location that cannot be written to, so that
        // the recovery log open fails and 2PC is disabled.
        unwritablePath = getUnwritablePath();

        final ServerConfiguration serverConfig = server.getServerConfiguration();
        final Transaction tranConfig = serverConfig.getTransaction();
        tranConfig.setTransactionLogDirectory(unwritablePath);
        server.updateServerConfiguration(serverConfig);

        FATUtils.startServers(server);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FATUtils.stopServers(server);
        if (unwritablePath != null) {
            cleanup(unwritablePath);
        }
    }

    /**
     * Returns a path that the Liberty server process will be unable to use as a
     * transaction log directory.
     * <ul>
     * <li>On POSIX systems a real temporary directory is created and its write
     * bits are cleared; a non-existent sub-path inside it is returned.</li>
     * <li>On Windows a path on the first drive letter that does not correspond
     * to an existing root is returned without creating anything. This is the
     * most reliable approach because Windows ACL DENY entries can be bypassed
     * by processes running with administrative privileges.</li>
     * </ul>
     *
     * @throws Exception
     */
    private static String getUnwritablePath() throws Exception {
        switch (server.getMachine().getOperatingSystem()) {
            case WINDOWS:
                // Windows: find a drive letter whose root does not exist so the server
                // cannot create files there regardless of privilege level.
                for (char c = 'D'; c <= 'Z'; c++) {
                    File root = new File(c + ":\\");
                    if (!root.exists()) {
                        return c + ":\\TheRecoveryLogsOfJon\\tranlog";
                    }
                }
                // All drive letters D-Z exist (extremely unlikely); fall back to a deeply
                // nested path under a known non-writable Windows system location.
                return "C:\\Windows\\System32\\config\\nonexistent-badlog\\tranlog";
            default:
                // POSIX: create a real directory and remove write bits
                Path dir = Files.createTempDirectory("badlog-tranlog-");
                Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("r-xr-xr-x"));
                return dir.toAbsolutePath().toString() + "/tranlog";
        }
    }

    /** Restores write access on POSIX so the temp directory can be deleted. No-op on Windows. */
    private static void cleanup(String path) {
        try {
            // The path is "<tempDir>/tranlog"; the parent is the created temp dir.
            Path parent = new File(path).getParentFile().toPath();
            if (parent.getFileSystem().supportedFileAttributeViews().contains("posix") && parent.toFile().exists()) {
                Files.setPosixFilePermissions(parent, PosixFilePermissions.fromString("rwxr-xr-x"));
                parent.toFile().delete();
            }
        } catch (IOException e) {
            // Best-effort cleanup
        }
    }
}
