/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jdbc.heritage.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.annotation.Resource;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JDBCHeritageTestServlet")
public class JDBCHeritageTestServlet extends FATServlet {
    @Resource
    private DataSource defaultDataSource;

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * Utility method that accesses the internal field WSJdbcPreparedStatement.pstmtImpl.
     * This is useful for determining if we got the same statement from the statement cache.
     *
     * @param pstmt prepared statement wrapper (WSJdbcPreparedStatement).
     * @return prepared statement implementation.
     */
    private static PreparedStatement pstmtImpl(PreparedStatement pstmt) throws PrivilegedActionException {
        return AccessController.doPrivileged((PrivilegedExceptionAction<PreparedStatement>) () -> {
            for (Class<?> c = pstmt.getClass(); c != null; c = c.getSuperclass())
                try {
                    Field pstmtImpl = c.getDeclaredField("pstmtImpl");
                    pstmtImpl.setAccessible(true);
                    return (PreparedStatement) pstmtImpl.get(pstmt);
                } catch (NoSuchFieldException x) {
                    // ignore, try the super class
                }
            throw new NoSuchFieldException("Unable to find pstmtImpl on prepared statement wrapper. If the field has been renamed, you will need to update this test.");
        });
    }

    /**
     * Confirm that a dataSource that is configured with heritageSettings can be injected
     * and has the transaction isolation level that is assigned as default by the DataStoreHelper.
     */
    @Test
    public void testDefaultIsolationLevel() throws Exception {
        assertNotNull(defaultDataSource);
        try (Connection con = defaultDataSource.getConnection()) {
            assertEquals(Connection.TRANSACTION_SERIALIZABLE, con.getTransactionIsolation());
        }
    }

    /**
     * Confirm that doesStatementCacheIsoLevel causes prepared statements to be cached based on
     * the isolation level that was present on the connection at the time the statement was
     * created.
     */
    @Test
    public void testStatementCachingBasedOnIsolationLevel() throws Exception {
        try (Connection con = defaultDataSource.getConnection()) {
            String sql = "VALUES('testStatementCachingBasedOnIsolationLevel')";
            PreparedStatement pstmt;

            con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            pstmt = con.prepareStatement(sql);
            PreparedStatement pstmtRC = pstmtImpl(pstmt);
            pstmt.close();

            con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            pstmt = con.prepareStatement(sql);
            PreparedStatement pstmtRR = pstmtImpl(pstmt);
            pstmt.close();

            con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            pstmt = con.prepareStatement(sql);
            PreparedStatement pstmtRC2 = pstmtImpl(pstmt);
            pstmt.close();

            assertNotSame(pstmtRC, pstmtRR);
            assertSame(pstmtRC, pstmtRC2);
        }
    }
}