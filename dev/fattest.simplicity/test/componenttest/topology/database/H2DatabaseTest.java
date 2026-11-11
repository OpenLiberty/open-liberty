/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.topology.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.junit.Test;

import componenttest.topology.database.H2Database.CIPHER;

public class H2DatabaseTest {

    // SHA-256 of UTF-16LE bytes of "@SECUSERsecpwd"
    private static final String KNOWN_HASH = "c8fb70828d3e11d0989b9e1e80599ad35f68c7772a6501ce22298d43cc0b1293"; //pragma: allowlist secret

    private static H2Database db() {
        return H2Database.create("secuser", "secpwd");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithCipherFilePasswordWithWhitespaceThrows() {
        db().withCipher(CIPHER.AES, "file password");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithCipherFilePasswordWithTabThrows() {
        db().withCipher(CIPHER.AES, "file\tpassword");
    }

    @Test
    public void testWithCipherImpliesFileMode() {
        String url = db().withCipher(CIPHER.AES, "fp").getURL();
        assertTrue(url, url.contains("CIPHER=AES"));
        assertFalse(url, url.contains("mem:"));
        assertTrue(url, url.contains("file:"));
    }

    @Test
    public void testWithCipherGetAdminPasswordReturnsPlainPassword() {
        H2Database db = db().withCipher(CIPHER.AES, "fp");
        assertEquals("secpwd", db.getAdminPassword());
        assertEquals("fp secpwd", db.getConnectionPassword());
    }

    @Test
    public void testWithPasswordHashKnownVector() {
        assertEquals(KNOWN_HASH, H2Database.hashPassword("secuser", "secpwd"));
        H2Database db = db().withPasswordHash();
        assertEquals(KNOWN_HASH, db.getConnectionPassword());
        assertEquals("secpwd", db.getAdminPassword());
    }

    @Test
    public void testWithCipherAndPasswordHashCombined() {
        H2Database db = db().withCipher(CIPHER.AES, "fp").withPasswordHash();
        String url = db.getURL();
        assertTrue(url, url.contains("CIPHER=AES"));
        assertTrue(url, url.contains("PASSWORD_HASH=TRUE"));
        assertEquals("secpwd", db.getAdminPassword());
        assertEquals("fp " + KNOWN_HASH, db.getConnectionPassword());
    }

    @Test
    public void testWithAllowLiteralsValidModes() {
        db().withAllowLiterals("ALL");
        db().withAllowLiterals("NUMBERS");
        db().withAllowLiterals("NONE");
    }

    @Test(expected = IllegalStateException.class)
    public void testWithFileLockInMemoryThrows() {
        db().withFileLock("NO");
    }

    @Test
    public void testWithFileLockSocketAppendedToUrl() {
        String url = db().withFileMode().withFileLock("SOCKET").getURL();
        assertTrue(url, url.contains("FILE_LOCK=SOCKET"));
    }

    @Test
    public void testWithFileLockAllValidMethods() {
        for (String method : new String[] { "FILE", "SOCKET", "NO", "FS" }) {
            String url = db().withFileMode().withFileLock(method).getURL();
            assertTrue(url, url.contains("FILE_LOCK=" + method));
        }
    }

    @Test
    public void testWithAllowLiteralsInMemoryImpliesFileMode() {
        H2Database db = db().withAllowLiterals("NONE");
        String url = db.getURL();
        assertTrue(url, url.contains("file:"));
        assertFalse(url, url.contains("mem:"));
    }

    @Test
    public void testWithConfigNonBlockedKeyStillWorks() {
        String url = db().withConfig("TRACE_LEVEL_SYSTEM_OUT", "1").getURL();
        assertTrue(url, url.contains("TRACE_LEVEL_SYSTEM_OUT=1"));
    }
}
