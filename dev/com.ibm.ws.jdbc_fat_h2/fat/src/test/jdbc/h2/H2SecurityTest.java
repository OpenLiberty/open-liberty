/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package test.jdbc.h2;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.H2Database;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jdbc.h2.security.web.H2SecurityTestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class H2SecurityTest extends FATServletClient {

    @Server("com.ibm.ws.jdbc.fat.h2.security")
    @TestServlet(servlet = H2SecurityTestServlet.class, contextRoot = "H2SecurityTestApp")
    public static LibertyServer server;

    private static final String CIPHER_FILE_PASSWORD = "filepwd";

    @ClassRule
    public static final H2Database cipherDb = H2Database.create("secuser", "secpwd")
                    .withCipher(H2Database.CIPHER.AES, CIPHER_FILE_PASSWORD)
                    .withDatabaseName("h2security-cipher");

    @ClassRule
    public static final H2Database hashDb = H2Database.create("secuser", "secpwd")
                    .withPasswordHash()
                    .withDatabaseName("h2security-hash");

    @ClassRule
    public static final H2Database literalsDb = H2Database.create("secuser", "secpwd")
                    .withFileMode()
                    .withAllowLiterals("NONE")
                    .withDatabaseName("h2security-literals");

    @ClassRule
    public static final H2Database fileLockDb = H2Database.create("secuser", "secpwd")
                    .withFileMode()
                    .withFileLock("SOCKET")
                    .withDatabaseName("h2security-filelock");

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp("H2SecurityTestApp",
                                                      "test.jdbc.h2.security.web");
        ShrinkHelper.exportAppToServer(server, war);

        // Cipher datasource: Liberty must pass "<filePassword> <userPassword>" as the
        // password because that is what H2 requires when CIPHER is active.
        server.addEnvVar("DB_URL_CIPHER_AES",      cipherDb.getURL());
        server.addEnvVar("DB_PASSWORD_CIPHER_AES", cipherDb.getConnectionPassword());

        // Hash datasource: with PASSWORD_HASH=TRUE H2 expects the already-hashed password.
        server.addEnvVar("DB_URL_PASSWORD_HASH",      hashDb.getURL());
        server.addEnvVar("DB_USER",                   "secuser");
        server.addEnvVar("DB_PASSWORD",               literalsDb.getAdminPassword());
        server.addEnvVar("DB_PASSWORD_PASSWORD_HASH", hashDb.getConnectionPassword());

        // ALLOW_LITERALS and FILE_LOCK datasources share DB_USER / DB_PASSWORD.
        server.addEnvVar("DB_URL_ALLOW_LITERALS",  literalsDb.getURL());
        server.addEnvVar("DB_URL_FILE_LOCK",       fileLockDb.getURL());

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
