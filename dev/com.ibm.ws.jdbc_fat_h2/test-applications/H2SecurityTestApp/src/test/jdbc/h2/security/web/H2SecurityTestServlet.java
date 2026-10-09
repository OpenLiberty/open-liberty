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
package test.jdbc.h2.security.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.annotation.Resource;
import jakarta.servlet.annotation.WebServlet;

import javax.sql.DataSource;

import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class H2SecurityTestServlet extends FATServlet {

	@Resource(lookup = "jdbc/h2ds-cipher-aes")
	DataSource cipherAesDs;

	@Resource(lookup = "jdbc/h2ds-cipher-aes",
	          authenticationType = Resource.AuthenticationType.APPLICATION)
	DataSource cipherAesAppAuthDs;

	@Resource(lookup = "jdbc/h2ds-password-hash")
	DataSource passwordHashDs;

	@Resource(lookup = "jdbc/h2ds-password-hash",
	          authenticationType = Resource.AuthenticationType.APPLICATION)
	DataSource passwordHashAppAuthDs;

	@Resource(lookup = "jdbc/h2ds-allow-literals-none")
	DataSource allowLiteralsDs;

	@Resource(lookup = "jdbc/h2ds-file-lock-socket")
	DataSource fileLockDs;

	@Test
	public void testCipherAES() throws Exception {
		try (Connection con = cipherAesDs.getConnection();
		     Statement st = con.createStatement();
		     ResultSet rs = st.executeQuery("VALUES 1")) {
			assertTrue(rs.next());
			assertEquals(1, rs.getInt(1));
		}
	}

	@Test
	public void testPasswordHash() throws Exception {
		try (Connection con = passwordHashDs.getConnection();
		     Statement st = con.createStatement();
		     ResultSet rs = st.executeQuery("VALUES 1")) {
			assertTrue(rs.next());
			assertEquals(1, rs.getInt(1));
		}
	}

	@Test
	@AllowedFFDC({ "jakarta.resource.spi.SecurityException",
	               "jakarta.resource.spi.ResourceAllocationException",
	               "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException" })
	public void testCipherAESWrongPassword() throws Exception {
		// Wrong user password (correct file password, wrong user password).
		// H2 error 28000: wrong user name or password.
		try (Connection con = cipherAesAppAuthDs.getConnection("secuser", "filepwd wrongpwd")) {
			con.close();
			fail("Expected SQLException for wrong user password with AES cipher");
		} catch (SQLException expected) {
			assertEquals("Expected H2 error 28000 (wrong user/password) for wrong user password",
			             28000, expected.getErrorCode());
		}

		// Wrong file password (wrong file password, correct user password).
		// H2 error 90049: wrong file password / decryption failure when the database file is opened.
		// H2 error 28000: the database is already open in this server JVM (pooled connections),
		// so the file password is validated as part of the user authentication instead.
		try (Connection con = cipherAesAppAuthDs.getConnection("secuser", "wrongfilepwd secpwd")) {
			con.close();
			fail("Expected SQLException for wrong file password with AES cipher");
		} catch (SQLException expected) {
			int code = expected.getErrorCode();
			assertTrue("Expected H2 error 90049 or 28000 for wrong file password but was " + code,
			           code == 90049 || code == 28000);
		}
	}

	@Test
	@AllowedFFDC({ "jakarta.resource.spi.SecurityException",
	               "jakarta.resource.spi.ResourceAllocationException",
	               "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException" })
	public void testPasswordHashWrongPassword() throws Exception {
		// Plain (unhashed) password with PASSWORD_HASH=TRUE.
		// H2 error 90004: password is not a valid hex string.
		try (Connection con = passwordHashAppAuthDs.getConnection("secuser", "wrongpwd")) {
			con.close();
			fail("Expected SQLException for unhashed password with PASSWORD_HASH=TRUE");
		} catch (SQLException expected) {
			assertEquals("Expected H2 error 90004 (invalid hex password) for unhashed password",
			             90004, expected.getErrorCode());
		}

		// Well-formed 64-char hex hash that does not match the user's password.
		// H2 error 28000: wrong user name or password.
		String wrongHash = "0".repeat(64);
		try (Connection con = passwordHashAppAuthDs.getConnection("secuser", wrongHash)) {
			con.close();
			fail("Expected SQLException for wrong password hash");
		} catch (SQLException expected) {
			assertEquals("Expected H2 error 28000 (wrong user/password) for wrong password hash",
			             28000, expected.getErrorCode());
		}
	}

	@Test
	public void testAllowLiteralsNone() throws Exception {
		try (Connection con = allowLiteralsDs.getConnection();
		     PreparedStatement ps = con.prepareStatement("SELECT ?")) {
			ps.setInt(1, 1);
			try (ResultSet rs = ps.executeQuery()) {
				assertTrue(rs.next());
				assertEquals(1, rs.getInt(1));
			}

			try (Statement st = con.createStatement()) {
				st.executeQuery("SELECT 1");
				fail("Expected SQLException for literal value under ALLOW_LITERALS NONE");
			} catch (SQLException expected) {
				// H2 error 90116: "Literals of this kind are not allowed"
				assertEquals(90116, expected.getErrorCode());
			}
		}
	}

	@Test
	public void testFileLockSocket() throws Exception {
		// Verify FILE_LOCK=SOCKET by checking that the .lock.db file written by H2's
		// socket lock mechanism exists and contains the string "socket".
		try (Connection con = fileLockDs.getConnection();
		     Statement st = con.createStatement();
		     ResultSet rs = st.executeQuery("CALL DATABASE_PATH()")) {
			assertTrue("Expected DATABASE_PATH() result", rs.next());
			String dbPath = rs.getString(1);
			Path lockFile = Paths.get(dbPath + ".lock.db");
			assertTrue("Expected .lock.db file to exist for FILE_LOCK=SOCKET database: " + lockFile,
			           Files.exists(lockFile));
			String lockContents = Files.readString(lockFile, StandardCharsets.UTF_8);
			assertTrue("Expected .lock.db file to contain 'socket' for FILE_LOCK=SOCKET database",
			           lockContents.toLowerCase().contains("socket"));
		}
	}
}
