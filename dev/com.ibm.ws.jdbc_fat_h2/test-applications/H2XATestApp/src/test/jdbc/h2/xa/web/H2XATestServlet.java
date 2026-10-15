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
package test.jdbc.h2.xa.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class H2XATestServlet extends FATServlet {

	@Resource(lookup = "jdbc/h2xa-ds1")
	private DataSource ds1;

	@Resource(lookup = "jdbc/h2xa-ds2")
	private DataSource ds2;

	@Resource(lookup = "jdbc/h2xa-ds1", shareable = false)
	private DataSource ds1Unshareable;

	@Resource
	private UserTransaction ut;

	private TransactionManager tm;

	@Override
	public void init() throws ServletException {
		super.init();
		try {
			tm = (TransactionManager) new InitialContext().lookup("java:comp/TransactionManager");
			createTables(ds1);
			createTables(ds2);
		} catch (NamingException | SQLException e) {
			throw new ServletException("Failed to initialize H2XATestServlet", e);
		}
	}

	private void createTables(DataSource ds) throws SQLException {
		try (Connection con = ds.getConnection();
		     Statement st = con.createStatement()) {
			st.executeUpdate("CREATE TABLE IF NOT EXISTS xa_test (id VARCHAR(100) PRIMARY KEY, val VARCHAR(100))");
		}
	}

	private void cleanupRow(DataSource ds, String id) throws SQLException {
		try (Connection con = ds.getConnection();
		     PreparedStatement ps = con.prepareStatement("DELETE FROM xa_test WHERE id = ?")) {
			ps.setString(1, id);
			ps.executeUpdate();
		}
	}

	private boolean rowExists(DataSource ds, String id) throws SQLException {
		try (Connection con = ds.getConnection();
		     PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM xa_test WHERE id = ?")) {
			ps.setString(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt(1) > 0;
				}
			}
		}
		return false;
	}

	private String getRowValue(DataSource ds, String id) throws SQLException {
		try (Connection con = ds.getConnection();
		     PreparedStatement ps = con.prepareStatement("SELECT val FROM xa_test WHERE id = ?")) {
			ps.setString(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getString(1);
				}
			}
		}
		return null;
	}

	/**
	 * Stub XAResource for verifying XA lifecycle invocations, votes, and flags.
	 */
	public static class StubXAResource implements XAResource {
		private final List<String> calls = Collections.synchronizedList(new ArrayList<>());
		private int prepareVote = XAResource.XA_OK;
		private boolean throwExceptionOnPrepare = false;
		private int transactionTimeout = 0;

		public void setPrepareVote(int vote) {
			this.prepareVote = vote;
		}

		public void setThrowExceptionOnPrepare(boolean throwException) {
			this.throwExceptionOnPrepare = throwException;
		}

		public List<String> getCalls() {
			return calls;
		}

		@Override
		public void start(Xid xid, int flags) throws XAException {
			calls.add("start");
		}

		@Override
		public void end(Xid xid, int flags) throws XAException {
			calls.add("end");
		}

		@Override
		public int prepare(Xid xid) throws XAException {
			calls.add("prepare");
			if (throwExceptionOnPrepare) {
				XAException xae = new XAException("Simulated prepare failure");
				xae.errorCode = XAException.XAER_RMFAIL;
				throw xae;
			}
			return prepareVote;
		}

		@Override
		public void commit(Xid xid, boolean onePhase) throws XAException {
			calls.add("commit");
		}

		@Override
		public void rollback(Xid xid) throws XAException {
			calls.add("rollback");
		}

		@Override
		public void forget(Xid xid) throws XAException {
			calls.add("forget");
		}

		@Override
		public int getTransactionTimeout() throws XAException {
			return transactionTimeout;
		}

		@Override
		public boolean setTransactionTimeout(int seconds) throws XAException {
			this.transactionTimeout = seconds;
			return true;
		}

		@Override
		public boolean isSameRM(XAResource xares) throws XAException {
			return this == xares;
		}

		@Override
		public Xid[] recover(int flag) throws XAException {
			return new Xid[0];
		}
	}

	/**
	 * Full two-phase commit: H2 db1 and db2 are enlisted in one global transaction;
	 * upon commit, rows inserted in both databases are visible.
	 */
	@Test
	public void testTwoPhaseCommitTwoH2() throws Exception {
		String id1 = "twoH2_1";
		String id2 = "twoH2_2";
		cleanupRow(ds1, id1);
		cleanupRow(ds2, id2);

		ut.begin();
		try {
			try (Connection c1 = ds1.getConnection();
			     PreparedStatement ps1 = c1.prepareStatement("INSERT INTO xa_test (id, val) VALUES (?, ?)")) {
				ps1.setString(1, id1);
				ps1.setString(2, "val1");
				ps1.executeUpdate();
			}
			try (Connection c2 = ds2.getConnection();
			     PreparedStatement ps2 = c2.prepareStatement("INSERT INTO xa_test (id, val) VALUES (?, ?)")) {
				ps2.setString(1, id2);
				ps2.setString(2, "val2");
				ps2.executeUpdate();
			}
			ut.commit();
		} catch (Exception e) {
			try {
				ut.rollback();
			} catch (Exception ignore) {
			}
			throw e;
		}

		assertTrue("Row in ds1 should exist after 2PC commit", rowExists(ds1, id1));
		assertTrue("Row in ds2 should exist after 2PC commit", rowExists(ds2, id2));
		assertEquals("val1", getRowValue(ds1, id1));
		assertEquals("val2", getRowValue(ds2, id2));
	}

	/**
	 * Full two-phase commit with stub: H2 and stub XAResource are enlisted in one global transaction;
	 * stub records prepare and commit invocations, and H2 data is persisted.
	 */
	@Test
	public void testTwoPhaseCommitWithStub() throws Exception {
		String id = "stub2pc_1";
		cleanupRow(ds1, id);

		StubXAResource stub = new StubXAResource();
		ut.begin();
		try {
			Transaction tx = tm.getTransaction();
			tx.enlistResource(stub);

			try (Connection c = ds1.getConnection();
			     PreparedStatement ps = c.prepareStatement("INSERT INTO xa_test (id, val) VALUES (?, ?)")) {
				ps.setString(1, id);
				ps.setString(2, "val_stub");
				ps.executeUpdate();
			}

			ut.commit();
		} catch (Exception e) {
			try {
				ut.rollback();
			} catch (Exception ignore) {
			}
			throw e;
		}

		assertTrue("Row in ds1 should exist after commit", rowExists(ds1, id));
		assertEquals("val_stub", getRowValue(ds1, id));
		assertTrue("Stub should have recorded prepare", stub.getCalls().contains("prepare"));
		assertTrue("Stub should have recorded commit", stub.getCalls().contains("commit"));
	}

	/**
	 * Rollback-only transaction: H2 work is performed in a global transaction,
	 * setRollbackOnly() is invoked, and commit is attempted;
	 * RollbackException is thrown and no changes are persisted.
	 */
	@Test
	public void testRollbackOnly() throws Exception {
		String id = "rbOnly_1";
		cleanupRow(ds1, id);

		ut.begin();
		try (Connection c = ds1.getConnection();
		     PreparedStatement ps = c.prepareStatement("INSERT INTO xa_test (id, val) VALUES (?, ?)")) {
			ps.setString(1, id);
			ps.setString(2, "val_rbOnly");
			ps.executeUpdate();
		}

		ut.setRollbackOnly();

		boolean rollbackExceptionThrown = false;
		try {
			ut.commit();
		} catch (RollbackException e) {
			rollbackExceptionThrown = true;
		}

		assertTrue("Commit should throw RollbackException when marked rollback-only", rollbackExceptionThrown);
		assertFalse("Row in ds1 should not exist after rollback", rowExists(ds1, id));
	}

	/**
	 * Explicit rollback: H2 db1, db2 and stub perform work in a global transaction
	 * and rollback() is called; neither resource persists changes.
	 */
	@Test
	public void testExplicitRollback() throws Exception {
		String id1 = "expRb_1";
		String id2 = "expRb_2";
		cleanupRow(ds1, id1);
		cleanupRow(ds2, id2);

		StubXAResource stub = new StubXAResource();
		ut.begin();
		Transaction tx = tm.getTransaction();
		tx.enlistResource(stub);

		try (Connection c1 = ds1.getConnection();
		     PreparedStatement ps1 = c1.prepareStatement("INSERT INTO xa_test (id, val) VALUES (?, ?)")) {
			ps1.setString(1, id1);
			ps1.setString(2, "val1");
			ps1.executeUpdate();
		}

		try (Connection c2 = ds2.getConnection();
		     PreparedStatement ps2 = c2.prepareStatement("INSERT INTO xa_test (id, val) VALUES (?, ?)")) {
			ps2.setString(1, id2);
			ps2.setString(2, "val2");
			ps2.executeUpdate();
		}

		ut.rollback();

		assertFalse("Row in ds1 should not exist after rollback", rowExists(ds1, id1));
		assertFalse("Row in ds2 should not exist after rollback", rowExists(ds2, id2));
		assertTrue("Stub should have recorded rollback", stub.getCalls().contains("rollback"));
		assertFalse("Stub should not have recorded commit", stub.getCalls().contains("commit"));
	}

	/**
	 * Read-only branch: H2 is enlisted in a global transaction executing only a query (SELECT),
	 * while a second resource (db2) writes; the transaction commits cleanly without error
	 * and the second resource's write is persisted.
	 */
	@Test
	public void testReadOnlyBranch() throws Exception {
		String id = "roBranch_2";
		cleanupRow(ds2, id);

		ut.begin();
		try {
			// Enlist H2 db1 by executing only a query (read-only branch for db1)
			try (Connection c1 = ds1.getConnection();
			     Statement st = c1.createStatement();
			     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM xa_test")) {
				assertTrue(rs.next());
			}

			// Enlist H2 db2 by performing a write
			try (Connection c2 = ds2.getConnection();
			     PreparedStatement ps2 = c2.prepareStatement("INSERT INTO xa_test (id, val) VALUES (?, ?)")) {
				ps2.setString(1, id);
				ps2.setString(2, "ro_val");
				ps2.executeUpdate();
			}

			ut.commit();
		} catch (Exception e) {
			try {
				ut.rollback();
			} catch (Exception ignore) {
			}
			throw e;
		}

		assertTrue("Row in ds2 should exist after commit with read-only db1 branch", rowExists(ds2, id));
		assertEquals("ro_val", getRowValue(ds2, id));
	}

	/**
	 * Read-only vote: the stub votes XA_RDONLY at prepare while H2 writes; the stub is not
	 * sent a commit and the H2 write is persisted.
	 */
	@Test
	public void testReadOnlyVoteFromStub() throws Exception {
		String id = "roVote_1";
		cleanupRow(ds1, id);

		StubXAResource stub = new StubXAResource();
		stub.setPrepareVote(XAResource.XA_RDONLY);
		ut.begin();
		try {
			Transaction tx = tm.getTransaction();
			tx.enlistResource(stub);

			try (Connection c = ds1.getConnection();
			     PreparedStatement ps = c.prepareStatement("INSERT INTO xa_test (id, val) VALUES (?, ?)")) {
				ps.setString(1, id);
				ps.setString(2, "ro_vote");
				ps.executeUpdate();
			}

			ut.commit();
		} catch (Exception e) {
			try {
				ut.rollback();
			} catch (Exception ignore) {
			}
			throw e;
		}

		assertTrue("Stub should have recorded prepare", stub.getCalls().contains("prepare"));
		assertFalse("Stub voted read-only and should not receive commit", stub.getCalls().contains("commit"));
		assertTrue("H2 write should be persisted", rowExists(ds1, id));
		assertEquals("ro_vote", getRowValue(ds1, id));
	}

	/**
	 * Two non-shareable connections to the same database are enlisted in one global transaction
	 * and both perform writes. The transaction commits successfully and both writes are persisted.
	 *
	 * Note for H2 2.4.240: H2's JdbcXAConnection.isSameRM compares by identity only, so two distinct
	 * XA connections are not the same RM and each is treated as its own branch (two-phase commit).
	 * The test verifies only the commit outcome: Liberty successfully coordinates the global
	 * transaction across both non-shareable connection handles and persists both changes.
	 */
	@Test
	public void testIsSameRMNonShareableConnections() throws Exception {
		String id1 = "sameRM_1";
		String id2 = "sameRM_2";
		cleanupRow(ds1, id1);
		cleanupRow(ds1, id2);

		ut.begin();
		try {
			try (Connection c1 = ds1Unshareable.getConnection();
			     Connection c2 = ds1Unshareable.getConnection()) {

				try (PreparedStatement ps1 = c1.prepareStatement("INSERT INTO xa_test (id, val) VALUES (?, ?)")) {
					ps1.setString(1, id1);
					ps1.setString(2, "val1");
					ps1.executeUpdate();
				}

				try (PreparedStatement ps2 = c2.prepareStatement("INSERT INTO xa_test (id, val) VALUES (?, ?)")) {
					ps2.setString(1, id2);
					ps2.setString(2, "val2");
					ps2.executeUpdate();
				}
			}

			ut.commit();
		} catch (Exception e) {
			try {
				ut.rollback();
			} catch (Exception ignore) {
			}
			throw e;
		}

		assertTrue("First row should exist after commit", rowExists(ds1, id1));
		assertTrue("Second row should exist after commit", rowExists(ds1, id2));
		assertEquals("val1", getRowValue(ds1, id1));
		assertEquals("val2", getRowValue(ds1, id2));
	}

	/**
	 * Resource failure on prepare: stub throws XAException on prepare;
	 * the global transaction rolls back, commit fails with RollbackException,
	 * and H2 changes are not persisted.
	 */
	@Test
	@AllowedFFDC({"javax.transaction.xa.XAException",
	              "jakarta.transaction.RollbackException",
	              "javax.transaction.RollbackException"})
	public void testResourceFailureOnPrepare() throws Exception {
		String id = "failPrep_1";
		cleanupRow(ds1, id);

		StubXAResource stub = new StubXAResource();
		stub.setThrowExceptionOnPrepare(true);

		ut.begin();
		Transaction tx = tm.getTransaction();
		tx.enlistResource(stub);

		try (Connection c = ds1.getConnection();
		     PreparedStatement ps = c.prepareStatement("INSERT INTO xa_test (id, val) VALUES (?, ?)")) {
			ps.setString(1, id);
			ps.setString(2, "val_failPrep");
			ps.executeUpdate();
		}

		boolean exceptionThrown = false;
		try {
			ut.commit();
		} catch (RollbackException | HeuristicMixedException | HeuristicRollbackException | SystemException e) {
			exceptionThrown = true;
		}

		assertTrue("Commit should fail when resource prepare fails", exceptionThrown);
		assertFalse("Row in ds1 should not exist after failed prepare/rollback", rowExists(ds1, id));
	}

	/**
	 * Test XA_RDONLY round-trip behavior:
	 * Within a global transaction, enlist a StubXAResource alongside H2, execute only a SELECT query on H2, and commit.
	 * Assert whether the stub receives a phase-2 commit call, documenting whether H2 votes XA_OK instead of XA_RDONLY.
	 */
	@Test
	public void testReadOnlyVoteH2() throws Exception {
		StubXAResource stub = new StubXAResource();
		ut.begin();
		try {
			Transaction tx = tm.getTransaction();
			tx.enlistResource(stub);

			try (Connection c = ds1.getConnection();
			     Statement st = c.createStatement();
			     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM xa_test")) {
				assertTrue(rs.next());
			}

			ut.commit();
		} catch (Exception e) {
			try {
				ut.rollback();
			} catch (Exception ignore) {
			}
			throw e;
		}

		assertTrue("Stub should have recorded prepare", stub.getCalls().contains("prepare"));
		// Note on H2 behavior: H2 returns XA_OK upon prepare even for read-only (SELECT-only) transactions,
		// causing Liberty's transaction manager to execute a two-phase commit where the stub receives phase-2 commit.
		// This documents current H2 behavior; if a future H2 version votes XA_RDONLY for read-only branches,
		// this assertion is expected to fail and should be updated to assert that no commit is received.
		assertTrue("Stub should have recorded commit", stub.getCalls().contains("commit"));
	}

}
