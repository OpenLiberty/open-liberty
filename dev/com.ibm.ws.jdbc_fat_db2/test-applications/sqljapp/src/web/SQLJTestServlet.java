/*******************************************************************************
 * Copyright (c) 2017,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import com.ibm.db2.jcc.DB2Connection;
import com.ibm.db2.jcc.DB2Wrapper;
import com.ibm.db2.jcc.PDQConnection;
import com.ibm.db2.jcc.SQLJConnection;
import com.ibm.db2.jcc.SQLJContext;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/SQLJTestServlet")
public class SQLJTestServlet extends FATServlet {

    private static final String SQLJ_CLASS = "web.SQLJProcedure";

    @Resource(shareable = false)
    DataSource ds;

    @Resource(lookup = "jdbc/SQLJContextDS", shareable = true)
    DataSource dsWithSQLJContextConnections;

    @Override
    public void destroy() {
        System.out.println("Request garbage collection to encourage sqlj.runtime.ref.ConnectionContextImpl.finalize to be invoked");
        System.gc();
    }

    private void runSQLJMethod(String testName) throws Throwable {
        Class<?> sqljTest = Class.forName(SQLJ_CLASS);
        try {
            sqljTest.getMethod(testName).invoke(null);
        } catch (InvocationTargetException e) {
            throw e.getCause() == null ? e : e.getCause();
        }
    }

    private void runSQLJMethod(String testName, DataSource ds) throws Throwable {
        Class<?> sqljTest = Class.forName(SQLJ_CLASS);
        try {
            sqljTest.getMethod(testName, DataSource.class).invoke(null, ds);
        } catch (InvocationTargetException e) {
            throw e.getCause() == null ? e : e.getCause();
        }
    }

    public void testDocExample() throws Throwable {
        runSQLJMethod("testDocExample");
    }

    public void testSQLJSimpleSelect() throws Throwable {
        runSQLJMethod("testSQLJSimpleSelect", ds);
    }

    public void testBasicCreateRead1() throws Throwable {
        runSQLJMethod("testBasicCreateRead1", ds);
    }

    public void testBasicCreateRead2() throws Throwable {
        runSQLJMethod("testBasicCreateRead2", ds);
    }

    public void testBasicCreateReadWithConnectionContext() throws Throwable {
        runSQLJMethod("testBasicCreateReadWithConnectionContext");
    }

    public void testBasicCreateReadWithConnectionContextWrapper() throws Throwable {
        runSQLJMethod("testBasicCreateReadWithConnectionContextWrapper", ds);
    }

    public void testBasicCreateReadWithConnectionContextWrapperAndCachedSQLJContext() throws Throwable {
        runSQLJMethod("testBasicCreateReadWithConnectionContextWrapperAndCachedSQLJContext", dsWithSQLJContextConnections);
    }

    public void testBasicCreateReadWithDataSource() throws Throwable {
        runSQLJMethod("testBasicCreateReadWithDataSource");
    }

    public void testBasicCreateReadWithSQLJContext() throws Throwable {
        runSQLJMethod("testBasicCreateReadWithSQLJContext", ds);
    }

    public void testBasicCreateRollback1() throws Throwable {
        runSQLJMethod("testBasicCreateRollback1", ds);
    }

    public void testBasicCreateRollback2() throws Throwable {
        runSQLJMethod("testBasicCreateRollback2", ds);
    }

    public void testBasicCreateUpdateDelete1() throws Throwable {
        runSQLJMethod("testBasicCreateUpdateDelete1", ds);
    }

    public void testBasicCreateUpdateDelete2() throws Throwable {
        runSQLJMethod("testBasicCreateUpdateDelete2", ds);
    }

    public void testBasicCreateUpdateRollback1() throws Throwable {
        runSQLJMethod("testBasicCreateUpdateRollback1", ds);
    }

    public void testBasicDeleteRollback1() throws Throwable {
        runSQLJMethod("testBasicDeleteRollback1", ds);
    }

    public void testBasicDeleteRollback2() throws Throwable {
        runSQLJMethod("testBasicDeleteRollback2", ds);
    }

    public void testBatching() throws Throwable {
        runSQLJMethod("testBatching", ds);
    }

    public void testBatchUpdateException() throws Throwable {
        runSQLJMethod("testBatchUpdateException", ds);
    }

    public void testCacheCallableStatement() throws Throwable {
        runSQLJMethod("testCacheCallableStatement", ds);
    }

    public void testCacheCallableStatementAndSQLJContext() throws Throwable {
        runSQLJMethod("testCacheCallableStatementAndSQLJContext", dsWithSQLJContextConnections);
    }

    public void testCacheCallableStatementAndSQLJContext2() throws Throwable {
        runSQLJMethod("testCacheCallableStatementAndSQLJContext2", dsWithSQLJContextConnections);
    }

    public void testCachePreparedStatement() throws Throwable {
        runSQLJMethod("testCachePreparedStatement", ds);
    }

    public void testCachePreparedStatementAndSQLJContext() throws Throwable {
        runSQLJMethod("testCachePreparedStatementAndSQLJContext", dsWithSQLJContextConnections);
    }

    public void testCachePreparedStatementAndSQLJContextWithIsolationLevels() throws Throwable {
        runSQLJMethod("testCachePreparedStatementAndSQLJContextWithIsolationLevels", dsWithSQLJContextConnections);
    }

    // Verify that connections from a data source with enableConnectionCasting are castable to all interfaces implemented
    // by the vendor connection implementation.
    public void testConnectionCasting() throws Exception {
        // enableConnectionCasting=true
        Connection con = dsWithSQLJContextConnections.getConnection();
        try {
            assertTrue(con instanceof DB2Connection);
            assertTrue(con instanceof DB2Wrapper);
            assertTrue(con instanceof PDQConnection);
            assertTrue(con instanceof SQLJConnection);
            assertTrue(con instanceof SQLJContext);
        } finally {
            con.close();
        }

        // enableConnectionCasting unspecified (false)
        con = ds.getConnection();
        try {
            assertFalse(con instanceof DB2Connection);
            assertFalse(con instanceof DB2Wrapper);
            assertFalse(con instanceof PDQConnection);
            assertFalse(con instanceof SQLJConnection);
            assertFalse(con instanceof SQLJContext);
        } finally {
            con.close();
        }
    }

    public void testResetExecutionContextWhenCachingSQLJContext() throws Throwable {
        runSQLJMethod("testResetExecutionContextWhenCachingSQLJContext", dsWithSQLJContextConnections);
    }

    public void testSQLJJDBCCombo1() throws Throwable {
        runSQLJMethod("testSQLJJDBCCombo1", ds);
    }

    public void testBasicCallableStatement() throws Throwable {
        runSQLJMethod("testBasicCallableStatement", ds);
    }

    public void testIsolation_RU() throws Throwable {
        runSQLJMethod("testIsolation_RU", ds);
    }

    public void testIsolation_RC() throws Throwable {
        runSQLJMethod("testIsolation_RC", ds);
    }

    public void testIsolation_RR() throws Throwable {
        runSQLJMethod("testIsolation_RR", ds);
    }

    public void testIsolation_SER() throws Throwable {
        runSQLJMethod("testIsolation_SER", ds);
    }

    public void testDefaultContext() throws Throwable {
        runSQLJMethod("testDefaultContext", ds);
    }

    public void testDefaultContext_caching() throws Throwable {
        runSQLJMethod("testDefaultContext", dsWithSQLJContextConnections);
    }

    public void testDefaultContext_threadlocal() throws Throwable {
        runSQLJMethod("testDefaultContext_threadlocal", ds);
    }

    public void testDefaultContext_caching_threadlocal() throws Throwable {
        runSQLJMethod("testDefaultContext_threadlocal", dsWithSQLJContextConnections);
    }

    public void testWarmStart() throws Throwable {
        // Run any test case that is repeatable
        runSQLJMethod("testBasicCreateUpdateDelete1", ds);
    }
}
