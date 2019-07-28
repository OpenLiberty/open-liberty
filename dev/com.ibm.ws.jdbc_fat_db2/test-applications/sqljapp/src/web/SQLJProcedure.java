/*@lineinfo:filename=SQLJProcedure*//*@lineinfo:user-code*//*@lineinfo:1^1*/package web;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import com.ibm.db2.jcc.SQLJContext;

import sqlj.runtime.ConnectionContext;
import sqlj.runtime.ExecutionContext;
import sqlj.runtime.ResultSetIterator;
import sqlj.runtime.profile.ConnectedProfile;
import sqlj.runtime.ref.DefaultContext;

//######################################################################
//###  NOTE: This is normally a generated file!                      ###
//###  This file should not be directly modified, as it is normally  ###
//###  generated from the SQLJProcedure.sqlj file in the same dir    ###
//###  as this file.                                                 ###
//###  If you need to make modifications to this file, make changes  ###
//###  to the .sqlj file instead, and then consult the build.gradle  ###
//###  file for instructions on how to re-generate this file.        ###
//######################################################################

// Whenever a new 'context' is added, be sure to add it to the
// Customizer args in com.ibm.ws.sqlj.fat.SQLJTest.setUpSQLJ()
/*@lineinfo:generated-code*//*@lineinfo:21^2*/

//  ************************************************************
//  SQLJ context declaration:
//  ************************************************************

class MyCtx extends com.ibm.db2.jcc.sqlj.DB2ConnectionContextImpl implements sqlj.runtime.ConnectionContext {
    private static java.util.Map m_typeMap = null;
    private static MyCtx defaultContext = null;

    public MyCtx(java.sql.Connection conn) throws java.sql.SQLException {
        super(conn);
    }

    public MyCtx(java.lang.String url, java.lang.String user, java.lang.String password, boolean autoCommit) throws java.sql.SQLException {
        super(url, user, password, autoCommit);
    }

    public MyCtx(java.lang.String url, java.util.Properties info, boolean autoCommit) throws java.sql.SQLException {
        super(url, info, autoCommit);
    }

    public MyCtx(java.lang.String url, boolean autoCommit) throws java.sql.SQLException {
        super(url, autoCommit);
    }

    public MyCtx(sqlj.runtime.ConnectionContext other) throws java.sql.SQLException {
        super(other);
    }

    public static MyCtx getDefaultContext() {
        if (defaultContext == null) {
            java.sql.Connection conn = sqlj.runtime.RuntimeContext.getRuntime().getDefaultConnection();
            if (conn != null) {
                try {
                    defaultContext = new MyCtx(conn);
                } catch (java.sql.SQLException e) {
                }
            }
        }
        return defaultContext;
    }

    public static void setDefaultContext(MyCtx ctx) {
        defaultContext = ctx;
    }

    @Override
    public java.util.Map getTypeMap() {
        return m_typeMap;
    }
}

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:21^18 */
/* @lineinfo:generated-code *//* @lineinfo:22^2 */

//  ************************************************************
//  SQLJ context declaration:
//  ************************************************************

class SqljCtx extends com.ibm.db2.jcc.sqlj.DB2ConnectionContextImpl implements sqlj.runtime.ConnectionContext {
    private static java.util.Map m_typeMap = null;
    private static SqljCtx defaultContext = null;

    public SqljCtx(java.sql.Connection conn) throws java.sql.SQLException {
        super(conn);
    }

    public SqljCtx(java.lang.String url, java.lang.String user, java.lang.String password, boolean autoCommit) throws java.sql.SQLException {
        super(url, user, password, autoCommit);
    }

    public SqljCtx(java.lang.String url, java.util.Properties info, boolean autoCommit) throws java.sql.SQLException {
        super(url, info, autoCommit);
    }

    public SqljCtx(java.lang.String url, boolean autoCommit) throws java.sql.SQLException {
        super(url, autoCommit);
    }

    public SqljCtx(sqlj.runtime.ConnectionContext other) throws java.sql.SQLException {
        super(other);
    }

    public static SqljCtx getDefaultContext() {
        if (defaultContext == null) {
            java.sql.Connection conn = sqlj.runtime.RuntimeContext.getRuntime().getDefaultConnection();
            if (conn != null) {
                try {
                    defaultContext = new SqljCtx(conn);
                } catch (java.sql.SQLException e) {
                }
            }
        }
        return defaultContext;
    }

    public static void setDefaultContext(SqljCtx ctx) {
        defaultContext = ctx;
    }

    @Override
    public java.util.Map getTypeMap() {
        return m_typeMap;
    }
}

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:22^20 */
/* @lineinfo:generated-code *//* @lineinfo:23^2 */

//  ************************************************************
//  SQLJ context declaration:
//  ************************************************************

class SqljDSCtx extends com.ibm.db2.jcc.sqlj.DB2ConnectionContextImpl implements sqlj.runtime.ConnectionContext {
    public static final java.lang.String dataSource = "java:comp/DefaultDataSource";
    private static java.util.Map m_typeMap = null;
    private static SqljDSCtx defaultContext = null;

    public SqljDSCtx(java.sql.Connection conn) throws java.sql.SQLException {
        super(conn);
    }

    public SqljDSCtx() throws java.sql.SQLException {
        super(dataSource);
    }

    public SqljDSCtx(java.lang.String user, java.lang.String password) throws java.sql.SQLException {
        super(dataSource, user, password);
    }

    public SqljDSCtx(sqlj.runtime.ConnectionContext other) throws java.sql.SQLException {
        super(other);
    }

    public static SqljDSCtx getDefaultContext() {
        if (defaultContext == null) {
            java.sql.Connection conn = sqlj.runtime.RuntimeContext.getRuntime().getDefaultConnection();
            if (conn != null) {
                try {
                    defaultContext = new SqljDSCtx(conn);
                } catch (java.sql.SQLException e) {
                }
            }
        }
        return defaultContext;
    }

    public static void setDefaultContext(SqljDSCtx ctx) {
        defaultContext = ctx;
    }

    @Override
    public java.util.Map getTypeMap() {
        return m_typeMap;
    }
}

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:23^70 */
/* @lineinfo:generated-code *//* @lineinfo:24^2 */

//  ************************************************************
//  SQLJ context declaration:
//  ************************************************************

class WrappedConContext extends com.ibm.db2.jcc.sqlj.DB2ConnectionContextImpl implements sqlj.runtime.ConnectionContext {
    private static java.util.Map m_typeMap = null;
    private static WrappedConContext defaultContext = null;

    public WrappedConContext(java.sql.Connection conn) throws java.sql.SQLException {
        super(conn);
    }

    public WrappedConContext(java.lang.String url, java.lang.String user, java.lang.String password, boolean autoCommit) throws java.sql.SQLException {
        super(url, user, password, autoCommit);
    }

    public WrappedConContext(java.lang.String url, java.util.Properties info, boolean autoCommit) throws java.sql.SQLException {
        super(url, info, autoCommit);
    }

    public WrappedConContext(java.lang.String url, boolean autoCommit) throws java.sql.SQLException {
        super(url, autoCommit);
    }

    public WrappedConContext(sqlj.runtime.ConnectionContext other) throws java.sql.SQLException {
        super(other);
    }

    public static WrappedConContext getDefaultContext() {
        if (defaultContext == null) {
            java.sql.Connection conn = sqlj.runtime.RuntimeContext.getRuntime().getDefaultConnection();
            if (conn != null) {
                try {
                    defaultContext = new WrappedConContext(conn);
                } catch (java.sql.SQLException e) {
                }
            }
        }
        return defaultContext;
    }

    public static void setDefaultContext(WrappedConContext ctx) {
        defaultContext = ctx;
    }

    @Override
    public java.util.Map getTypeMap() {
        return m_typeMap;
    }
}

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:24^30 */
/* @lineinfo:generated-code *//* @lineinfo:25^2 */

//  ************************************************************
//  SQLJ iterator declaration:
//  ************************************************************

class MyIter extends sqlj.runtime.ref.ResultSetIterImpl implements sqlj.runtime.NamedIterator {
    private int nameNdx;

    public MyIter(sqlj.runtime.profile.RTResultSet resultSet) throws java.sql.SQLException {
        super(resultSet);
        nameNdx = findColumn("name");
    }

    public MyIter(sqlj.runtime.profile.RTResultSet resultSet, int fetchSize, int maxRows) throws java.sql.SQLException {
        super(resultSet, fetchSize, maxRows);
        nameNdx = findColumn("name");
    }

    public String name() throws java.sql.SQLException {
        return resultSet.getString(nameNdx);
    }
}

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:25^32 */
/* @lineinfo:generated-code *//* @lineinfo:26^2 */

//  ************************************************************
//  SQLJ iterator declaration:
//  ************************************************************

class Value1Iter extends sqlj.runtime.ref.ResultSetIterImpl implements sqlj.runtime.NamedIterator {
    private int nameNdx;

    public Value1Iter(sqlj.runtime.profile.RTResultSet resultSet) throws java.sql.SQLException {
        super(resultSet);
        nameNdx = findColumn("name");
    }

    public Value1Iter(sqlj.runtime.profile.RTResultSet resultSet, int fetchSize, int maxRows) throws java.sql.SQLException {
        super(resultSet, fetchSize, maxRows);
        nameNdx = findColumn("name");
    }

    public String name() throws java.sql.SQLException {
        return resultSet.getString(nameNdx);
    }
}

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:26^36 */
/* @lineinfo:generated-code *//* @lineinfo:27^2 */

//  ************************************************************
//  SQLJ iterator declaration:
//  ************************************************************

class BasicIter extends sqlj.runtime.ref.ResultSetIterImpl implements sqlj.runtime.PositionedIterator {
    public BasicIter(sqlj.runtime.profile.RTResultSet resultSet) throws java.sql.SQLException {
        super(resultSet, 2);
    }

    public BasicIter(sqlj.runtime.profile.RTResultSet resultSet, int fetchSize, int maxRows) throws java.sql.SQLException {
        super(resultSet, fetchSize, maxRows, 2);
    }

    public int getCol1() throws java.sql.SQLException {
        return resultSet.getIntNoNull(1);
    }

    public String getCol2() throws java.sql.SQLException {
        return resultSet.getString(2);
    }
}

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:27^35 */
public class SQLJProcedure {
    // ######################################################################
    // ###  NOTE: Not a java file!                                        ###
    // ###  If you are debugging a failure in this bucket line numbers    ###
    // ###  will not match up to this file because this sqlj file gets    ###
    // ###  transformed from .sqlj->.java->.class file at runtime.        ###
    // ###  See autoFVT/test-applications/sqljapp/src/web/SQLJProcedure.java
    // ###  to look at the java file that this sqlj file turns into       ###
    // ######################################################################

    /**
     * Code from our Knowledge Center example with the following modifications:
     * - lookup default datasource instead of 'jdbc/myDS'
     * - table is named 'sqljtest' instead of 'mytable'
     * - added junit assertions to make sure we get the correct values
     * - inline code comments are exactly how they will appear in the Knowledge Center
     */
    public static void testDocExample() throws Throwable {
        DataSource ds = (DataSource) new InitialContext().lookup("java:comp/DefaultDataSource");
        Connection con = ds.getConnection();
        con.setAutoCommit(false);
        MyCtx ctx = new MyCtx(con);
        try {
            // Insert some data into the 'mytable' table.
            // Note that the java variables 'key' and 'val' get substituted into the #sql
            int key = 1;
            String val = "one";
            /* @lineinfo:generated-code *//* @lineinfo:56^12 */

//  ************************************************************
//  #sql [ctx] { INSERT into sqljtest VALUES (:key, :val)  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(0), 0);
                    try {
                        __sJT_stmt.setInt(1, key);
                        __sJT_stmt.setString(2, val);
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:56^66 */
            /* @lineinfo:generated-code *//* @lineinfo:57^12 */

//  ************************************************************
//  #sql [ctx] { COMMIT  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(0), 1);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:57^32 */

            MyIter iter;
            /* @lineinfo:generated-code *//* @lineinfo:60^12 */

//  ************************************************************
//  #sql [ctx] iter = { SELECT name FROM sqljtest WHERE id=1  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(0), 2);
                    try {
                        iter = new MyIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:60^69 */
            assertTrue("No results were returned.", iter.next());
            System.out.println("Found value: " + iter.name());
            assertEquals("one", iter.name());
            iter.close();
            /* @lineinfo:generated-code *//* @lineinfo:65^12 */

//  ************************************************************
//  #sql [ctx] { COMMIT  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(0), 3);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:65^32 */

            // Restore auto-commit true so a transaction is not leaked
            con.setAutoCommit(true);
        } finally {
            ctx.close();
        }
    }

    public static void testSQLJSimpleSelect(DataSource ds) throws Throwable {

        // Insert some data using SQLJ context
        Connection con = ds.getConnection();
        SqljCtx ctx = new SqljCtx(con);
        try {
            Map<Integer, String> entriesToInsert = new LinkedHashMap<Integer, String>();
            entriesToInsert.put(20, "twenty");
            entriesToInsert.put(30, "thirty");
            entriesToInsert.put(40, "fourty");
            entriesToInsert.put(70, "seventy");
            for (Map.Entry<Integer, String> entry : entriesToInsert.entrySet()) {
                int key = entry.getKey();
                String val = entry.getValue();
                /* @lineinfo:generated-code *//* @lineinfo:88^13 */

//  ************************************************************
//  #sql [ctx] { INSERT INTO sqljtest VALUES (:key, :val)  };
//  ************************************************************

                {
                    sqlj.runtime.ConnectionContext __sJT_connCtx = ctx;
                    if (__sJT_connCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                    sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                    if (__sJT_execCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                    synchronized (__sJT_execCtx) {
                        sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 0);
                        try {
                            __sJT_stmt.setInt(1, key);
                            __sJT_stmt.setString(2, val);
                            __sJT_execCtx.executeUpdate();
                        } finally {
                            __sJT_execCtx.releaseStatement();
                        }
                    }
                }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:88^67 */
            }
            /* @lineinfo:generated-code *//* @lineinfo:90^9 */

//  ************************************************************
//  #sql [ctx] { COMMIT };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 1);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:90^27 */

            // Select all records with a value above 25
            int min = 25;
            Value1Iter iter;
            /* @lineinfo:generated-code *//* @lineinfo:95^9 */

//  ************************************************************
//  #sql [ctx] iter = { SELECT name FROM sqljtest WHERE id > :min };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 2);
                    try {
                        __sJT_stmt.setInt(1, min);
                        iter = new Value1Iter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:95^69 */

            Set<String> foundValues = new HashSet<String>();
            while (iter.next()) {
                String val = iter.name();
                System.out.println("Found value: " + val);
                foundValues.add(val);
            }
            iter.close();

            /* @lineinfo:generated-code *//* @lineinfo:105^9 */

//  ************************************************************
//  #sql [ctx] { COMMIT };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 3);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:105^27 */

            if (!foundValues.contains("thirty") || !foundValues.contains("fourty") || !foundValues.contains("seventy"))
                throw new Exception("Did not find all expected values in the result iterator: " + foundValues);
        } finally {
            ctx.close();
        }
    }

    public static void testBasicCreateRead1(DataSource ds) throws Exception {

        UserTransaction utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

        // Insert some data in a global tran
        utx.begin();
        Connection con = ds.getConnection();
        SqljCtx cmctx1 = new SqljCtx(con);
        try {
            /* @lineinfo:generated-code */
            /* @lineinfo:123^9 */

//  ************************************************************
//  #sql [cmctx1] { INSERT INTO sqljtest VALUES (0, 'zero') };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 4);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:123^63 */
        } finally {
            cmctx1.close();
        }
        utx.commit();

        // Select the data back with a different SQLJ context in a different global tran
        utx.begin();
        con = ds.getConnection();
        SqljCtx cmctx2 = new SqljCtx(con);
        try {
            BasicIter cursor1;
            /* @lineinfo:generated-code *//* @lineinfo:135^9 */

//  ************************************************************
//  #sql [cmctx2] cursor1 = { SELECT id, name FROM sqljtest WHERE id=0 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 5);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:135^74 */

            ResultSet rs = cursor1.getResultSet();
            verifyRow(0, "zero", rs);
            rs.close();
        } finally {
            cmctx2.close();
        }
        utx.commit();
    }

    public static void testBasicCreateRead2(DataSource ds) throws Exception {

        // Insert some data in a local tran
        Connection con = ds.getConnection();
        con.setAutoCommit(false);
        SqljCtx cmctx1 = new SqljCtx(con);
        try {
            /* @lineinfo:generated-code */
            /* @lineinfo:153^9 */

//  ************************************************************
//  #sql [cmctx1] { INSERT INTO sqljtest VALUES (2, 'two') };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 6);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:153^62 */

            con.commit();
        } finally {
            con.setAutoCommit(true);
            cmctx1.close();
        }

        // Select the data back with a different SQLJ context
        con = ds.getConnection();
        con.setAutoCommit(false);
        SqljCtx cmctx2 = new SqljCtx(con);
        try {
            BasicIter cursor1;
            /* @lineinfo:generated-code *//* @lineinfo:167^9 */

//  ************************************************************
//  #sql [cmctx2] cursor1 = { SELECT id, name FROM sqljtest WHERE id=2 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 7);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:167^74 */

            ResultSet rs = cursor1.getResultSet();
            verifyRow(2, "two", rs);

            rs.close();
        } finally {
            con.setAutoCommit(true);
            cmctx2.close();
        }
    }

    // Tests sqlj context that is constructed with a ConnectionContext
    public static void testBasicCreateReadWithConnectionContext() throws Exception {
        SqljDSCtx dsctx = new SqljDSCtx();
        SqljCtx ccctx = new SqljCtx(dsctx);
        try {
            int id = 15;
            String name = "fifteen";
            /* @lineinfo:generated-code *//* @lineinfo:186^12 */

//  ************************************************************
//  #sql [ccctx] { INSERT INTO sqljtest VALUES (:id, :name) };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ccctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 8);
                    try {
                        __sJT_stmt.setInt(1, id);
                        __sJT_stmt.setString(2, name);
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:186^66 */

            BasicIter cursor;
            /* @lineinfo:generated-code *//* @lineinfo:189^9 */

//  ************************************************************
//  #sql [ccctx] cursor = { SELECT id, name FROM sqljtest WHERE id=:id };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ccctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 9);
                    try {
                        __sJT_stmt.setInt(1, id);
                        cursor = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:189^74 */

            ResultSet result = cursor.getResultSet();
            verifyRow(id, name, result);
            result.close();
        } finally {
            ccctx.close();
            dsctx.close();
        }
    }

    // Tests sqlj context that is constructed with a ConnectionContext wrapper supplied by the application
    public static void testBasicCreateReadWithConnectionContextWrapper(DataSource ds) throws Exception {
        Connection con = ds.getConnection();
        con.setAutoCommit(false);
        ConnectionContextWrapper ccw = new ConnectionContextWrapper(con);
        SqljCtx ccwctx = new SqljCtx(ccw);
        try {
            int id = 28;
            String name = "twenty-eight";
            /* @lineinfo:generated-code *//* @lineinfo:209^12 */

//  ************************************************************
//  #sql [ccwctx] { INSERT INTO sqljtest VALUES (:id, :name) };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ccwctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 10);
                    try {
                        __sJT_stmt.setInt(1, id);
                        __sJT_stmt.setString(2, name);
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:209^67 */
            ccw.commit();

            id = 29;
            name = "twenty-nine";
            /* @lineinfo:generated-code *//* @lineinfo:214^12 */

//  ************************************************************
//  #sql [ccwctx] { INSERT INTO sqljtest VALUES (:id, :name) };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ccwctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 11);
                    try {
                        __sJT_stmt.setInt(1, id);
                        __sJT_stmt.setString(2, name);
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:214^67 */
            ccw.rollback();

            ccwctx.getExecutionContext().setMaxRows(2);

            BasicIter cursor;
            /* @lineinfo:generated-code *//* @lineinfo:220^12 */

//  ************************************************************
//  #sql [ccwctx] cursor = { SELECT id, name FROM sqljtest WHERE id>=28 AND id<=29 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ccwctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 12);
                    try {
                        cursor = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:220^89 */

            ResultSet result = cursor.getResultSet();
            verifyRow(28, "twenty-eight", result);

            if (result.next())
                fail("Second entry should have been rolled back. Instead: " + result.getInt(1) + ',' + result.getString(2));

            result.close();
        } finally {
            if (!ccwctx.isClosed()) {
                con.setAutoCommit(true);
                ccwctx.close(ConnectionContext.CLOSE_CONNECTION);
            }
        }
    }

    // Tests sqlj context caching and sqlj context that is constructed with a ConnectionContext wrapper supplied by the application
    public static void testBasicCreateReadWithConnectionContextWrapperAndCachedSQLJContext(DataSource ds) throws Exception {
        Connection con = ds.getConnection();
        con.setAutoCommit(false);
        ConnectionContextWrapper ccw = new ConnectionContextWrapper(con);
        SqljCtx ccwctx = new SqljCtx(ccw);
        try {
            int id = 128;
            String name = "twenty-eight";
            /* @lineinfo:generated-code *//* @lineinfo:246^12 */

//  ************************************************************
//  #sql [ccwctx] { INSERT INTO sqljtest VALUES (:id, :name) };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ccwctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 13);
                    try {
                        __sJT_stmt.setInt(1, id);
                        __sJT_stmt.setString(2, name);
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:246^67 */
            ccw.commit();

            id = 129;
            name = "twenty-nine";
            /* @lineinfo:generated-code *//* @lineinfo:251^12 */

//  ************************************************************
//  #sql [ccwctx] { INSERT INTO sqljtest VALUES (:id, :name) };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ccwctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 14);
                    try {
                        __sJT_stmt.setInt(1, id);
                        __sJT_stmt.setString(2, name);
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:251^67 */
            ccw.rollback();

            ccwctx.getExecutionContext().setMaxRows(2);

            BasicIter cursor;
            /* @lineinfo:generated-code *//* @lineinfo:257^12 */

//  ************************************************************
//  #sql [ccwctx] cursor = { SELECT id, name FROM sqljtest WHERE id>=128 AND id<=129 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ccwctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 15);
                    try {
                        cursor = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:257^91 */

            ResultSet result = cursor.getResultSet();
            verifyRow(128, "twenty-eight", result);

            if (result.next())
                fail("Second entry should have been rolled back. Instead: " + result.getInt(1) + ',' + result.getString(2));

            result.close();
        } finally {
            con.setAutoCommit(true);
        }
    }

    // Tests sqlj context that specifies a data source JNDI name
    public static void testBasicCreateReadWithDataSource() throws Exception {
        SqljDSCtx dsctx = new SqljDSCtx();
        try {
            int id = 14;
            String name = "fourteen";
            /* @lineinfo:generated-code *//* @lineinfo:277^12 */

//  ************************************************************
//  #sql [dsctx] { INSERT INTO sqljtest VALUES (:id, :name) };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = dsctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(2), 0);
                    try {
                        __sJT_stmt.setInt(1, id);
                        __sJT_stmt.setString(2, name);
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:277^66 */

            BasicIter cursor;
            /* @lineinfo:generated-code *//* @lineinfo:280^9 */

//  ************************************************************
//  #sql [dsctx] cursor = { SELECT id, name FROM sqljtest WHERE id=:id };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = dsctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(2), 1);
                    try {
                        __sJT_stmt.setInt(1, id);
                        cursor = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:280^74 */

            ResultSet result = cursor.getResultSet();
            verifyRow(id, name, result);
            result.close();
        } finally {
            dsctx.close();
        }
    }

    // Unwrap connection as SQLJContext before constructing context
    public static void testBasicCreateReadWithSQLJContext(DataSource ds) throws Exception {
        SQLJContext con = ds.getConnection().unwrap(SQLJContext.class);
        try {
            SqljCtx sqljctx = new SqljCtx(con);
            try {
                int id = 16;
                String name = "sixteen";
                /* @lineinfo:generated-code *//* @lineinfo:298^13 */

//  ************************************************************
//  #sql [sqljctx] { INSERT INTO sqljtest VALUES (:id, :name) };
//  ************************************************************

                {
                    sqlj.runtime.ConnectionContext __sJT_connCtx = sqljctx;
                    if (__sJT_connCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                    sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                    if (__sJT_execCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                    synchronized (__sJT_execCtx) {
                        sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 16);
                        try {
                            __sJT_stmt.setInt(1, id);
                            __sJT_stmt.setString(2, name);
                            __sJT_execCtx.executeUpdate();
                        } finally {
                            __sJT_execCtx.releaseStatement();
                        }
                    }
                }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:298^69 */

                BasicIter cursor;
                /* @lineinfo:generated-code *//* @lineinfo:301^10 */

//  ************************************************************
//  #sql [sqljctx] cursor = { SELECT id, name FROM sqljtest WHERE id=:id };
//  ************************************************************

                {
                    sqlj.runtime.ConnectionContext __sJT_connCtx = sqljctx;
                    if (__sJT_connCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                    sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                    if (__sJT_execCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                    synchronized (__sJT_execCtx) {
                        sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 17);
                        try {
                            __sJT_stmt.setInt(1, id);
                            cursor = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                        } finally {
                            __sJT_execCtx.releaseStatement();
                        }
                    }
                }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:301^77 */

                ResultSet result = cursor.getResultSet();
                verifyRow(16, "sixteen", result);
                result.close();
            } finally {
                sqljctx.close();
            }
        } finally {
            con.close();
        }
    }

    public static void testBasicCreateUpdateDelete1(DataSource ds) throws Exception {

        // Insert some data in a local tran
        Connection con = ds.getConnection();
        con.setAutoCommit(false);
        SqljCtx cmctx1 = new SqljCtx(con);
        try {
            /* @lineinfo:generated-code */
            /* @lineinfo:321^9 */

//  ************************************************************
//  #sql [cmctx1] { INSERT INTO sqljtest VALUES (3, 'three') };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 18);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:321^64 */
            con.commit();

            // Update the data with a new value, then delete it
            String name = "newname";
            /* @lineinfo:generated-code *//* @lineinfo:326^9 */

//  ************************************************************
//  #sql [cmctx1] { UPDATE sqljtest SET name=:name WHERE id=3 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 19);
                    try {
                        __sJT_stmt.setString(1, name);
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:326^65 */
            con.commit();

            /* @lineinfo:generated-code *//* @lineinfo:329^9 */

//  ************************************************************
//  #sql [cmctx1] { DELETE FROM sqljtest WHERE name=:name };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 20);
                    try {
                        __sJT_stmt.setString(1, name);
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:329^61 */
            con.commit();

            // Verify that the data was deleted
            BasicIter cursor1;
            /* @lineinfo:generated-code *//* @lineinfo:334^9 */

//  ************************************************************
//  #sql [cmctx1] cursor1 = { SELECT id, name FROM sqljtest WHERE id=3 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 21);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:334^74 */
            ResultSet rs = cursor1.getResultSet();
            if (rs.next())
                fail("Object exists, DELETE did not happen!");
        } finally {
            con.setAutoCommit(true);
            cmctx1.close();
        }
    }

    public static void testBasicCreateUpdateDelete2(DataSource ds) throws Exception {

        UserTransaction utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

        // Insert some data in a global tran
        utx.begin();
        Connection con = ds.getConnection();
        SqljCtx cmctx1 = new SqljCtx(con);
        try {
            /* @lineinfo:generated-code */
            /* @lineinfo:353^9 */

//  ************************************************************
//  #sql [cmctx1] { INSERT INTO sqljtest VALUES (4, 'four') };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 22);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:353^63 */
            utx.commit();

            // Update the data with a new value and then delete it
            utx.begin();
            String name = "newname";
            /* @lineinfo:generated-code *//* @lineinfo:359^9 */

//  ************************************************************
//  #sql [cmctx1] { UPDATE sqljtest SET name=:name WHERE id=4 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 23);
                    try {
                        __sJT_stmt.setString(1, name);
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:359^65 */
            utx.commit();

            utx.begin();
            /* @lineinfo:generated-code *//* @lineinfo:363^9 */

//  ************************************************************
//  #sql [cmctx1] { DELETE FROM sqljtest WHERE name=:name };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 24);
                    try {
                        __sJT_stmt.setString(1, name);
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:363^61 */
            utx.commit();

            // Verify that the data was deleted
            BasicIter cursor1;
            /* @lineinfo:generated-code *//* @lineinfo:368^9 */

//  ************************************************************
//  #sql [cmctx1] cursor1 = { SELECT id, name FROM sqljtest WHERE id=4 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 25);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:368^74 */

            ResultSet rs = cursor1.getResultSet();
            if (rs.next())
                fail("Object exists, DELETE did not happen!");
        } finally {
            cmctx1.close();
        }
    }

    public static void testBasicCreateUpdateRollback1(DataSource ds) throws Exception {

        // Insert some data in a local tran
        Connection con = ds.getConnection();
        con.setAutoCommit(false);
        SqljCtx cmctx1 = new SqljCtx(con);
        try {
            /* @lineinfo:generated-code */
            /* @lineinfo:385^9 */

//  ************************************************************
//  #sql [cmctx1] { INSERT INTO sqljtest VALUES (5, 'five') };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 26);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:385^63 */
            con.commit();

            // Update the data, but then rollback
            String name = "newname";
            /* @lineinfo:generated-code *//* @lineinfo:390^9 */

//  ************************************************************
//  #sql [cmctx1] { UPDATE sqljtest SET name=:name WHERE id=5 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 27);
                    try {
                        __sJT_stmt.setString(1, name);
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:390^65 */
            con.rollback();
        } finally {
            con.setAutoCommit(true);
            cmctx1.close();
        }

        // Create a new context and select the data, expect the original value
        Connection con1 = ds.getConnection();
        SqljCtx cmctx2 = new SqljCtx(con1);
        try {
            BasicIter cursor1;
            /* @lineinfo:generated-code *//* @lineinfo:402^9 */

//  ************************************************************
//  #sql [cmctx2] cursor1 = { SELECT id, name FROM sqljtest WHERE id=5 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 28);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:402^74 */

            ResultSet rs = cursor1.getResultSet();
            verifyRow(5, "five", rs);
        } finally {
            con1.setAutoCommit(true);
            cmctx2.close();
        }
    }

    public static void testBasicCreateRollback1(DataSource ds) throws Exception {

        UserTransaction utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

        // Insert some data in a global tran, but then rollback
        utx.begin();
        Connection con = ds.getConnection();
        SqljCtx cmctx1 = new SqljCtx(con);
        try {
            /* @lineinfo:generated-code */
            /* @lineinfo:421^9 */

//  ************************************************************
//  #sql [cmctx1] { INSERT INTO sqljtest VALUES (6, 'six') };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 29);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:421^62 */
            utx.rollback();
        } finally {
            cmctx1.close();
        }

        // Try to select the rolled back data in a different global tran
        // with a different SQLJ context.  It should not exist.
        utx.begin();
        Connection con1 = ds.getConnection();
        SqljCtx cmctx2 = new SqljCtx(con1);
        try {
            BasicIter cursor1;
            /* @lineinfo:generated-code *//* @lineinfo:434^9 */

//  ************************************************************
//  #sql [cmctx2] cursor1 = { SELECT id, name FROM sqljtest WHERE id=6 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 30);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:434^74 */

            ResultSet rs = cursor1.getResultSet();
            if (rs.next())
                fail("Object exists, rollback did not happen!");
            utx.commit();
        } finally {
            cmctx2.close();
        }
    }

    public static void testBasicCreateRollback2(DataSource ds) throws Exception {

        // Insert some data in a local tran, but then rollback
        Connection con = ds.getConnection();
        con.setAutoCommit(false);
        SqljCtx cmctx1 = new SqljCtx(con);
        try {
            /* @lineinfo:generated-code */
            /* @lineinfo:452^9 */

//  ************************************************************
//  #sql [cmctx1] { INSERT INTO sqljtest VALUES (7, 'seven') };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 31);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:452^64 */
            con.rollback();
        } finally {
            con.setAutoCommit(true);
            cmctx1.close();
        }

        // Try to select the data using a different SQLJ context, it should not exist
        Connection con1 = ds.getConnection();
        SqljCtx cmctx2 = new SqljCtx(con1);
        try {
            BasicIter cursor1;
            /* @lineinfo:generated-code *//* @lineinfo:464^9 */

//  ************************************************************
//  #sql [cmctx2] cursor1 = { SELECT id, name FROM sqljtest WHERE id=7 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 32);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:464^74 */

            ResultSet rs = cursor1.getResultSet();
            if (rs.next())
                fail("Object exists, rollback did not happen!");
        } finally {
            cmctx2.close();
        }
    }

    public static void testBasicUpdateRollback2(DataSource ds) throws Exception {

        UserTransaction utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

        // Insert some data in a global tran
        utx.begin();
        Connection con = ds.getConnection();
        SqljCtx cmctx1 = new SqljCtx(con);
        try {
            /* @lineinfo:generated-code */
            /* @lineinfo:483^9 */

//  ************************************************************
//  #sql [cmctx1] { INSERT INTO sqljtest VALUES (8, 'eight') };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 33);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:483^64 */
            utx.commit();

            // Update the data with a different value, but then rollback
            utx.begin();

            String name = "newname";
            /* @lineinfo:generated-code *//* @lineinfo:490^9 */

//  ************************************************************
//  #sql [cmctx1] { UPDATE sqljtest SET name=:name WHERE id=8 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 34);
                    try {
                        __sJT_stmt.setString(1, name);
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:490^65 */

            utx.rollback();
        } finally {
            cmctx1.close();
        }

        // Select the data with a different SQLJ context, expect original value
        utx.begin();
        Connection con1 = ds.getConnection();
        SqljCtx cmctx2 = new SqljCtx(con1);
        try {
            BasicIter cursor1;
            /* @lineinfo:generated-code *//* @lineinfo:503^9 */

//  ************************************************************
//  #sql [cmctx2] cursor1 = { SELECT id, name FROM sqljtest WHERE id=8 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 35);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:503^74 */

            ResultSet rs = cursor1.getResultSet();
            verifyRow(8, "eight", rs);
            utx.commit();
        } finally {
            cmctx2.close();
        }
    }

    public static void testBasicDeleteRollback2(DataSource ds) throws Exception {
        UserTransaction utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

        // Insert some data in a global tran
        utx.begin();
        Connection con = ds.getConnection();
        SqljCtx cmctx1 = new SqljCtx(con);
        try {
            /* @lineinfo:generated-code */
            /* @lineinfo:521^9 */

//  ************************************************************
//  #sql [cmctx1] { INSERT INTO sqljtest VALUES (9, 'nine') };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 36);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:521^63 */
            utx.commit();

            // Delete the new data, but then rollback
            utx.begin();
            /* @lineinfo:generated-code *//* @lineinfo:526^9 */

//  ************************************************************
//  #sql [cmctx1] { DELETE FROM sqljtest WHERE id=9 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 37);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:526^55 */
            utx.rollback();
        } finally {
            cmctx1.close();
        }

        // Select the data with a new SQLJ context.
        // Expect data to exist because the delete was rolled back
        utx.begin();
        Connection con1 = ds.getConnection();
        SqljCtx cmctx2 = new SqljCtx(con1);
        try {
            BasicIter cursor1;
            /* @lineinfo:generated-code *//* @lineinfo:539^9 */

//  ************************************************************
//  #sql [cmctx2] cursor1 = { SELECT id, name FROM sqljtest WHERE id=9 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 38);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:539^74 */

            ResultSet rs = cursor1.getResultSet();
            verifyRow(9, "nine", rs);
            utx.commit();
        } finally {
            cmctx2.close();
        }
    }

    public static void testBasicDeleteRollback1(DataSource ds) throws Exception {
        // Insert some data in a local tran
        Connection con = ds.getConnection();
        con.setAutoCommit(false);
        SqljCtx cmctx1 = new SqljCtx(con);
        try {
            /* @lineinfo:generated-code */
            /* @lineinfo:555^9 */

//  ************************************************************
//  #sql [cmctx1] { INSERT INTO sqljtest VALUES (10, 'ten') };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 39);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:555^63 */
            con.commit();

            // Delete the data, but then rollback
            /* @lineinfo:generated-code *//* @lineinfo:559^9 */

//  ************************************************************
//  #sql [cmctx1] { DELETE FROM sqljtest WHERE id=10 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 40);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:559^56 */
            con.rollback();
        } finally {
            con.setAutoCommit(true);
            cmctx1.close();
        }

        // Select the data with a new SQLJ context
        // Expect data to exist because the delete was rolled back
        Connection con1 = ds.getConnection();
        SqljCtx cmctx2 = new SqljCtx(con1);
        try {
            BasicIter cursor1;
            /* @lineinfo:generated-code *//* @lineinfo:572^9 */

//  ************************************************************
//  #sql [cmctx2] cursor1 = { SELECT id, name FROM sqljtest WHERE id=10 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 41);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:572^75 */

            ResultSet rs = cursor1.getResultSet();
            verifyRow(10, "ten", rs);
            rs.close();
        } finally {
            con1.setAutoCommit(true);
            cmctx2.close();
        }
    }

    // Test coverage for SQLJ batching to drive the special DB2 addBatch/executeBatch methods
    public static void testBatching(DataSource ds) throws Exception {
        Connection con = ds.getConnection();
        try {
            con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            con.setAutoCommit(false);

            SqljCtx bctx = new SqljCtx(con);
            ExecutionContext xctx = bctx.getExecutionContext();
            xctx.setBatching(true);

            int id = 17;
            String name = "seventeen";
            /* @lineinfo:generated-code *//* @lineinfo:596^12 */

//  ************************************************************
//  #sql [bctx] { INSERT INTO sqljtest VALUES (:id, :name) };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = bctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 42);
                    try {
                        __sJT_stmt.setInt(1, id);
                        __sJT_stmt.setString(2, name);
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:596^65 */

            id = 18;
            name = "eighteen";
            /* @lineinfo:generated-code *//* @lineinfo:600^12 */

//  ************************************************************
//  #sql [bctx] { INSERT INTO sqljtest VALUES (:id, :name) };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = bctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 43);
                    try {
                        __sJT_stmt.setInt(1, id);
                        __sJT_stmt.setString(2, name);
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:600^65 */

            name = "EIGHTEEN";
            /* @lineinfo:generated-code *//* @lineinfo:603^12 */

//  ************************************************************
//  #sql [bctx] { UPDATE sqljtest SET name=:name WHERE id=:id };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = bctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 44);
                    try {
                        __sJT_stmt.setString(1, name);
                        __sJT_stmt.setInt(2, id);
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:603^68 */

            int[] updateCounts = xctx.executeBatch();
            if (!Arrays.equals(updateCounts, new int[] { 1, 1, 1 }))
                fail("Unexpected update counts " + Arrays.toString(updateCounts));

            xctx.setBatching(false);

            con.setAutoCommit(true); // commits the local transaction

            BasicIter cursor;
            int min = 17;
            int max = 18;
            /* @lineinfo:generated-code *//* @lineinfo:616^12 */

//  ************************************************************
//  #sql [bctx] cursor = { SELECT id, name FROM sqljtest WHERE id>=:min AND id<=:max ORDER BY id ASC };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = bctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 45);
                    try {
                        __sJT_stmt.setInt(1, min);
                        __sJT_stmt.setInt(2, max);
                        cursor = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:616^107 */

            ResultSet result = cursor.getResultSet();
            if (!result.next())
                fail("Did not find any results in the range of 17 to 18");
            assertEquals(result.getInt("id"), 17);
            assertEquals(result.getString("name"), "seventeen");
            if (!result.next())
                fail("Did not find second result");
            assertEquals(result.getInt("id"), 18);
            assertEquals(result.getString("name"), "EIGHTEEN");
            if (result.next())
                fail("Too many results. Extra is: " + result.getInt("id") + ',' + result.getString("name"));
            result.close();

            bctx.close();
        } finally {
            con.close();
        }
    }

    // Test coverage for BatchUpdateException path in SQLJ
    public static void testBatchUpdateException(DataSource ds) throws Exception {
        Connection con = ds.getConnection();
        try {
            con.setAutoCommit(false);

            SqljCtx bctx = new SqljCtx(con);
            ExecutionContext xctx = bctx.getExecutionContext();
            xctx.setBatching(true);

            int id = 21;
            String name = "twenty-one";
            /* @lineinfo:generated-code *//* @lineinfo:649^12 */

//  ************************************************************
//  #sql [bctx] { INSERT INTO sqljtest VALUES (:id, :name) };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = bctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 46);
                    try {
                        __sJT_stmt.setInt(1, id);
                        __sJT_stmt.setString(2, name);
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:649^65 */

            name = "twenty-one again";
            /* @lineinfo:generated-code *//* @lineinfo:652^12 */

//  ************************************************************
//  #sql [bctx] { INSERT INTO sqljtest VALUES (:id, :name) };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = bctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 47);
                    try {
                        __sJT_stmt.setInt(1, id);
                        __sJT_stmt.setString(2, name);
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:652^65 */

            try {
                int[] updateCounts = xctx.executeBatch();
                fail("not expecting executeBatch to succeed when non-unique primary key is used. Update counts: " + Arrays.toString(updateCounts));
            } catch (BatchUpdateException x) {
                int[] updateCounts = x.getUpdateCounts();
                assertEquals(updateCounts.length, 2);
                assertEquals(updateCounts[0], 1);
                assertEquals(updateCounts[1], Statement.EXECUTE_FAILED);
            }

            xctx.setBatching(false);

            con.rollback();
            con.setAutoCommit(true);

            BasicIter cursor;
            id = 21;
            /* @lineinfo:generated-code *//* @lineinfo:671^12 */

//  ************************************************************
//  #sql [bctx] cursor = { SELECT id, name FROM sqljtest WHERE id=:id };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = bctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 48);
                    try {
                        __sJT_stmt.setInt(1, id);
                        cursor = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:671^76 */

            ResultSet result = cursor.getResultSet();
            if (result.next())
                fail("Data should have been rolled back: " + result.getInt("id") + ',' + result.getString("name"));
            result.close();

            bctx.close();
        } finally {
            con.close();
        }
    }

    // Execute the same SQLJ callable statement 3 times. The second two should reuse the statement from the cache.
    public static void testCacheCallableStatement(DataSource ds) throws Exception {
        Connection con = ds.getConnection();
        try {
            testCacheCallableStatementInsert(con, 22, "twenty-two");

            UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
            tran.begin();
            try {
                testCacheCallableStatementInsert(con, 23, "twenty-three");
            } finally {
                tran.commit();
            }

            testCacheCallableStatementInsert(con, 24, "twenty-four");

            // Verify all 3 results are in the database
            SqljCtx ccsctx = new SqljCtx(con);
            try {
                int min = 22;
                int max = 24;
                BasicIter cursor;
                /* @lineinfo:generated-code *//* @lineinfo:707^13 */

//  ************************************************************
//  #sql [ccsctx] cursor = { SELECT id, name FROM sqljtest WHERE id>=:min AND id<=:max ORDER BY id ASC };
//  ************************************************************

                {
                    sqlj.runtime.ConnectionContext __sJT_connCtx = ccsctx;
                    if (__sJT_connCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                    sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                    if (__sJT_execCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                    synchronized (__sJT_execCtx) {
                        sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 49);
                        try {
                            __sJT_stmt.setInt(1, min);
                            __sJT_stmt.setInt(2, max);
                            cursor = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                        } finally {
                            __sJT_execCtx.releaseStatement();
                        }
                    }
                }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:707^110 */

                ResultSet result = cursor.getResultSet();
                if (!result.next())
                    fail("All records are missing");
                assertEquals(result.getInt(1), 22);
                assertEquals(result.getString(2), "twenty-two");
                if (!result.next())
                    fail("Second and third records are missing");
                assertEquals(result.getInt(1), 23);
                assertEquals(result.getString(2), "twenty-three");
                if (!result.next())
                    fail("Third record is missing");
                assertEquals(result.getInt(1), 24);
                assertEquals(result.getString(2), "twenty-four");
                if (result.next())
                    fail("Too many records returned by query. Extra is: " + result.getInt(1) + ',' + result.getString(2));
                result.close();
            } finally {
                ccsctx.close(ConnectionContext.KEEP_CONNECTION);
            }
        } finally {
            con.close();
        }
    }

    // With SQLJ context caching enabled, execute the same SQLJ callable statement 3 times. The second two should reuse the statement from the cache.
    public static void testCacheCallableStatementAndSQLJContext(DataSource ds) throws Exception {
        Connection con = ds.getConnection();
        try {
            testCacheCallableStatementInsert(con, 122, "twenty-two");

            UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
            tran.begin();
            try {
                testCacheCallableStatementInsert(con, 123, "twenty-three");
            } finally {
                tran.commit();
            }

            testCacheCallableStatementInsert(con, 124, "twenty-four");

            // Verify all 3 results are in the database
            SqljCtx ccsctx = new SqljCtx(con);

            int min = 122;
            int max = 124;
            BasicIter cursor;
            /* @lineinfo:generated-code *//* @lineinfo:756^9 */

//  ************************************************************
//  #sql [ccsctx] cursor = { SELECT id, name FROM sqljtest WHERE id>=:min AND id<=:max ORDER BY id ASC };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ccsctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 50);
                    try {
                        __sJT_stmt.setInt(1, min);
                        __sJT_stmt.setInt(2, max);
                        cursor = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:756^106 */

            ResultSet result = cursor.getResultSet();
            if (!result.next())
                fail("All records are missing");
            assertEquals(result.getInt(1), 122);
            assertEquals(result.getString(2), "twenty-two");
            if (!result.next())
                fail("Second and third records are missing");
            assertEquals(result.getInt(1), 123);
            assertEquals(result.getString(2), "twenty-three");
            if (!result.next())
                fail("Third record is missing");
            assertEquals(result.getInt(1), 124);
            assertEquals(result.getString(2), "twenty-four");
            if (result.next())
                fail("Too many records returned by query. Extra is: " + result.getInt(1) + ',' + result.getString(2));
            result.close();
        } finally {
            con.close();
        }
    }

    // With SQLJ context caching enabled, execute the same SQLJ callable statement 3 times, but roll back one of them.
    public static void testCacheCallableStatementAndSQLJContext2(DataSource ds) throws Exception {
        Connection con = ds.getConnection();
        try {
            UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
            tran.begin();
            try {
                testCacheCallableStatementInsert(con, 222, "twenty-two");
            } finally {
                tran.commit();
            }

            tran.begin();
            try {
                testCacheCallableStatementInsert(con, 223, "twenty-three");
            } finally {
                tran.rollback();
            }

            testCacheCallableStatementInsert(con, 224, "twenty-four");

            // Verify the first and last are in the database
            SqljCtx ccsctx = new SqljCtx(con);

            int min = 222;
            int max = 224;
            BasicIter cursor;
            /* @lineinfo:generated-code *//* @lineinfo:807^9 */

//  ************************************************************
//  #sql [ccsctx] cursor = { SELECT id, name FROM sqljtest WHERE id>=:min AND id<=:max ORDER BY id ASC };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ccsctx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 51);
                    try {
                        __sJT_stmt.setInt(1, min);
                        __sJT_stmt.setInt(2, max);
                        cursor = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:807^106 */

            ResultSet result = cursor.getResultSet();
            if (!result.next())
                fail("All records are missing");
            assertEquals(result.getInt(1), 222);
            assertEquals(result.getString(2), "twenty-two");
            if (!result.next())
                fail("Third records is missing");
            assertEquals(result.getInt(1), 224);
            assertEquals(result.getString(2), "twenty-four");
            if (result.next())
                fail("Too many records returned by query. Extra is: " + result.getInt(1) + ',' + result.getString(2));
            result.close();
        } finally {
            con.close();
        }
    }

    // This is done as a separate method so that SQLJ assigns the same section number so that the cached statement to matches
    private static void testCacheCallableStatementInsert(Connection con, int id, String name) throws SQLException {
        SqljCtx ccsictx = new SqljCtx(con);
        try {
            // The PUTNAME stored procedure gets registered using JSE JDBC in SQLJTest.setUpSQLJ()
            /* @lineinfo:generated-code *//* @lineinfo:831^12 */

//  ************************************************************
//  #sql [ccsictx] { CALL PUTNAME(:id, :name) };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ccsictx;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 52);
                    try {
                        __sJT_stmt.setInt(1, id);
                        __sJT_stmt.setString(2, name);
                        __sJT_execCtx.execute();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:831^52 */
        } finally {
            ccsictx.close(ConnectionContext.KEEP_CONNECTION);
        }
    }

    // Execute the same SQLJ prepared statement 3 times. The second two should reuse the statement from the cache.
    public static void testCachePreparedStatement(DataSource ds) throws Exception {
        int[] ids = new int[] { 25, 26, 27 };
        String[] names = new String[] { "twenty-five", "twenty-six", "twenty-seven" };

        Executor currentThreadExecutor = new Executor() {
            @Override
            public void execute(Runnable r) {
                r.run();
            }
        };

        Connection con = ds.getConnection();
        try {
            // Use reflection to invoke this operation, since at runtime we only have Java 6 sometimes:
            // con.setNetworkTimeout(currentThreadExecutor, (int) TimeUnit.MINUTES.toMillis(5));
            con.getClass()
                            .getMethod("setNetworkTimeout", Executor.class, int.class)
                            .invoke(con, currentThreadExecutor, (int) TimeUnit.MINUTES.toMillis(5));

            for (int i = 0; i < 3; i++) {
                int id = ids[i];
                String name = names[i];
                SqljCtx cpsctx1 = new SqljCtx(con);
                try {
                    /* @lineinfo:generated-code */
                    /* @lineinfo:861^20 */

//  ************************************************************
//  #sql [cpsctx1] { INSERT INTO sqljtest VALUES (:id, :name)  };
//  ************************************************************

                    {
                        sqlj.runtime.ConnectionContext __sJT_connCtx = cpsctx1;
                        if (__sJT_connCtx == null)
                            sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                        sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                        if (__sJT_execCtx == null)
                            sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                        synchronized (__sJT_execCtx) {
                            sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 53);
                            try {
                                __sJT_stmt.setInt(1, id);
                                __sJT_stmt.setString(2, name);
                                __sJT_execCtx.executeUpdate();
                            } finally {
                                __sJT_execCtx.releaseStatement();
                            }
                        }
                    }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:861^78 */
                } finally {
                    cpsctx1.close(ConnectionContext.KEEP_CONNECTION);
                }
            }

            // Verify all 3 results are in the database
            SqljCtx cpsctx2 = new SqljCtx(con);
            try {
                int min = 25;
                int max = 27;
                BasicIter cursor;
                /* @lineinfo:generated-code *//* @lineinfo:873^13 */

//  ************************************************************
//  #sql [cpsctx2] cursor = { SELECT id, name FROM sqljtest WHERE id>=:min AND id<=:max ORDER BY id ASC };
//  ************************************************************

                {
                    sqlj.runtime.ConnectionContext __sJT_connCtx = cpsctx2;
                    if (__sJT_connCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                    sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                    if (__sJT_execCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                    synchronized (__sJT_execCtx) {
                        sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 54);
                        try {
                            __sJT_stmt.setInt(1, min);
                            __sJT_stmt.setInt(2, max);
                            cursor = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                        } finally {
                            __sJT_execCtx.releaseStatement();
                        }
                    }
                }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:873^111 */

                ResultSet result = cursor.getResultSet();
                if (!result.next())
                    fail("All records are missing");
                assertEquals(result.getInt(1), 25);
                assertEquals(result.getString(2), "twenty-five");
                if (!result.next())
                    fail("Second and third records are missing");
                assertEquals(result.getInt(1), 26);
                assertEquals(result.getString(2), "twenty-six");
                if (!result.next())
                    fail("Third record is missing");
                assertEquals(result.getInt(1), 27);
                assertEquals(result.getString(2), "twenty-seven");
                if (result.next())
                    fail("Too many records returned by query. Extra is: " + result.getInt(1) + ',' + result.getString(2));
                result.close();
            } finally {
                cpsctx2.close(ConnectionContext.KEEP_CONNECTION);
            }
        } finally {
            con.close();
        }
    }

    // With SQLJ context caching enabled, execute the same SQLJ prepared statement 3 times. The second two should reuse the statement from the cache.
    public static void testCachePreparedStatementAndSQLJContext(DataSource ds) throws Exception {
        int[] ids = new int[] { 125, 126, 127 };
        String[] names = new String[] { "twenty-five", "twenty-six", "twenty-seven" };

        Connection con = ds.getConnection();
        try {
            for (int i = 0; i < 3; i++) {
                int id = ids[i];
                String name = names[i];
                SqljCtx cpsctx1 = new SqljCtx(con);
                /* @lineinfo:generated-code *//* @lineinfo:911^16 */

//  ************************************************************
//  #sql [cpsctx1] { INSERT INTO sqljtest VALUES (:id, :name)  };
//  ************************************************************

                {
                    sqlj.runtime.ConnectionContext __sJT_connCtx = cpsctx1;
                    if (__sJT_connCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                    sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                    if (__sJT_execCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                    synchronized (__sJT_execCtx) {
                        sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 55);
                        try {
                            __sJT_stmt.setInt(1, id);
                            __sJT_stmt.setString(2, name);
                            __sJT_execCtx.executeUpdate();
                        } finally {
                            __sJT_execCtx.releaseStatement();
                        }
                    }
                }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:911^74 */
            }

            // Verify all 3 results are in the database
            SqljCtx cpsctx2 = new SqljCtx(con);
            int min = 125;
            int max = 127;
            BasicIter cursor;
            /* @lineinfo:generated-code *//* @lineinfo:919^9 */

//  ************************************************************
//  #sql [cpsctx2] cursor = { SELECT id, name FROM sqljtest WHERE id>=:min AND id<=:max ORDER BY id ASC };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cpsctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 56);
                    try {
                        __sJT_stmt.setInt(1, min);
                        __sJT_stmt.setInt(2, max);
                        cursor = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:919^107 */

            ResultSet result = cursor.getResultSet();
            if (!result.next())
                fail("All records are missing");
            assertEquals(result.getInt(1), 125);
            assertEquals(result.getString(2), "twenty-five");
            if (!result.next())
                fail("Second and third records are missing");
            assertEquals(result.getInt(1), 126);
            assertEquals(result.getString(2), "twenty-six");
            if (!result.next())
                fail("Third record is missing");
            assertEquals(result.getInt(1), 127);
            assertEquals(result.getString(2), "twenty-seven");
            if (result.next())
                fail("Too many records returned by query. Extra is: " + result.getInt(1) + ',' + result.getString(2));
            result.close();
        } finally {
            con.close();
        }
    }

    // With SQLJ context caching enabled, execute the same SQLJ prepared statement 3 times, with different transaction isolation levels.
    public static void testCachePreparedStatementAndSQLJContextWithIsolationLevels(DataSource ds) throws Exception {
        int[] ids = new int[] { 225, 226, 227 };
        String[] names = new String[] { "twenty-five", "twenty-six", "twenty-seven" };
        int[] isolationLevels = new int[] { Connection.TRANSACTION_READ_UNCOMMITTED, Connection.TRANSACTION_READ_COMMITTED, Connection.TRANSACTION_SERIALIZABLE };

        Connection con = ds.getConnection();
        try {
            for (int i = 0; i < 3; i++) {
                int id = ids[i];
                String name = names[i];
                con.setTransactionIsolation(isolationLevels[i]);
                SqljCtx cpsctx1 = new SqljCtx(con);
                /* @lineinfo:generated-code *//* @lineinfo:956^16 */

//  ************************************************************
//  #sql [cpsctx1] { INSERT INTO sqljtest VALUES (:id, :name)  };
//  ************************************************************

                {
                    sqlj.runtime.ConnectionContext __sJT_connCtx = cpsctx1;
                    if (__sJT_connCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                    sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                    if (__sJT_execCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                    synchronized (__sJT_execCtx) {
                        sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 57);
                        try {
                            __sJT_stmt.setInt(1, id);
                            __sJT_stmt.setString(2, name);
                            __sJT_execCtx.executeUpdate();
                        } finally {
                            __sJT_execCtx.releaseStatement();
                        }
                    }
                }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:956^74 */
                assertEquals(cpsctx1.getConnection().getTransactionIsolation(), isolationLevels[i]);
            }
            con.close();
            con = ds.getConnection(); // WAS assigns new DB2 connections a default isolation level of repeatable read

            // Verify all 3 results are in the database
            SqljCtx cpsctx2 = new SqljCtx(con);
            int min = 225;
            int max = 227;
            BasicIter cursor;
            /* @lineinfo:generated-code *//* @lineinfo:967^9 */

//  ************************************************************
//  #sql [cpsctx2] cursor = { SELECT id, name FROM sqljtest WHERE id>=:min AND id<=:max ORDER BY id ASC };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cpsctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 58);
                    try {
                        __sJT_stmt.setInt(1, min);
                        __sJT_stmt.setInt(2, max);
                        cursor = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:967^107 */

            ResultSet result = cursor.getResultSet();
            if (!result.next())
                fail("All records are missing");
            assertEquals(result.getInt(1), 225);
            assertEquals(result.getString(2), "twenty-five");
            if (!result.next())
                fail("Second and third records are missing");
            assertEquals(result.getInt(1), 226);
            assertEquals(result.getString(2), "twenty-six");
            if (!result.next())
                fail("Third record is missing");
            assertEquals(result.getInt(1), 227);
            assertEquals(result.getString(2), "twenty-seven");
            if (result.next())
                fail("Too many records returned by query. Extra is: " + result.getInt(1) + ',' + result.getString(2));
            assertEquals(result.getStatement().getConnection().getTransactionIsolation(), Connection.TRANSACTION_REPEATABLE_READ);
            result.close();

            assertEquals(cpsctx2.getConnection().getTransactionIsolation(), Connection.TRANSACTION_REPEATABLE_READ);
        } finally {
            con.close();
        }
    }

    // Verify that ExecutionContext is reset when SQLJ context is cached and reused
    public static void testResetExecutionContextWhenCachingSQLJContext(DataSource ds) throws Exception {
        Connection con;
        boolean batching;
        int batchLimit;
        int fetchDirection;
        int fetchSize;
        int maxFieldSize;
        int maxRows;
        int queryTimeout;

        UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        tran.begin();
        try {
            con = ds.getConnection();
            SqljCtx recctx1 = new SqljCtx(con);
            ExecutionContext execCtx = recctx1.getExecutionContext();

            batching = execCtx.isBatching();
            batchLimit = execCtx.getBatchLimit();
            fetchDirection = execCtx.getFetchDirection();
            fetchSize = execCtx.getFetchSize();
            maxFieldSize = execCtx.getMaxFieldSize();
            maxRows = execCtx.getMaxRows();
            queryTimeout = execCtx.getQueryTimeout();

            execCtx.setBatching(true);
            execCtx.setBatchLimit(ExecutionContext.AUTO_BATCH);
            execCtx.setFetchDirection(ResultSetIterator.FETCH_REVERSE);
            execCtx.setFetchSize(5);
            execCtx.setMaxFieldSize(70);
            execCtx.setMaxRows(15);
            execCtx.setQueryTimeout(210);
        } finally {
            tran.commit();
        }

        tran.begin();
        try {
            SqljCtx recctx2 = new SqljCtx(con);
            ExecutionContext execCtx = recctx2.getExecutionContext();

            assertEquals(execCtx.isBatching(), batching);
            assertEquals(execCtx.getBatchLimit(), batchLimit);
            assertEquals(execCtx.getFetchDirection(), fetchDirection);
            assertEquals(execCtx.getFetchSize(), fetchSize);
            assertEquals(execCtx.getMaxFieldSize(), maxFieldSize);
            assertEquals(execCtx.getMaxRows(), maxRows);
            assertEquals(execCtx.getQueryTimeout(), queryTimeout);
        } finally {
            tran.commit();
        }
    }

    public static void testSQLJJDBCCombo1(DataSource ds) throws Exception {

        UserTransaction utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

        // Insert some data in a global tran with SQLJ
        utx.begin();
        Connection con = ds.getConnection();
        con.setAutoCommit(false);
        SqljCtx cmctx1 = new SqljCtx(con);
        try {
            /* @lineinfo:generated-code */
            /* @lineinfo:1057^9 */

//  ************************************************************
//  #sql [cmctx1] { INSERT INTO sqljtest VALUES (11, 'eleven') };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 59);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1057^66 */
            utx.commit();
        } finally {
            con.setAutoCommit(true);
            cmctx1.close();
        }

        // Verify the data was inserted by selecting with SQLJ
        utx.begin();
        Connection con1 = ds.getConnection();
        SqljCtx cmctx2 = new SqljCtx(con1);
        try {
            BasicIter cursor1;
            /* @lineinfo:generated-code *//* @lineinfo:1070^9 */

//  ************************************************************
//  #sql [cmctx2] cursor1 = { SELECT id, name FROM sqljtest WHERE id=11 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 60);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1070^75 */

            ResultSet rs = cursor1.getResultSet();
            verifyRow(11, "eleven", rs);
            rs.close();

            // Insert some data using JDBC, but with the same connection as the SQLJ context
            PreparedStatement ps = con1.prepareStatement("INSERT INTO sqljtest VALUES (?,?)");
            ps.setInt(1, 12);
            ps.setString(2, "twelve");
            ps.executeUpdate();
            ps.close();

            // Use JDBC to verify that the data inserted with SQLJ is present
            Statement stmt = con1.createStatement();
            stmt.execute("SELECT id, name FROM sqljtest WHERE id=12");
            rs = stmt.getResultSet();
            verifyRow(12, "twelve", rs);
            rs.close();

            utx.commit();
        } finally {
            cmctx2.close();
        }
    }

    public static void testBasicCallableStatement(DataSource ds) throws Exception {
        // Insert some data using a stored procedure
        // then select it back with SQLJ using the same context
        Connection con = ds.getConnection();
        SqljCtx cmctx1 = new SqljCtx(con);
        try {
            // The PUTNAME stored procedure gets registered using JSE JDBC in SQLJTest.setUpSQLJ()
            /* @lineinfo:generated-code *//* @lineinfo:1104^12 */

//  ************************************************************
//  #sql [cmctx1] { CALL PUTNAME(13, 'thirteen') };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 61);
                    try {
                        __sJT_execCtx.execute();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1104^55 */

            BasicIter cursor1;
            /* @lineinfo:generated-code *//* @lineinfo:1107^12 */

//  ************************************************************
//  #sql [cmctx1] cursor1 = { SELECT id, name FROM sqljtest WHERE id=13 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = cmctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 62);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1107^78 */

            ResultSet rs = cursor1.getResultSet();
            verifyRow(13, "thirteen", rs);
            rs.close();
        } finally {
            cmctx1.close();
        }
    }

    public static void testIsolation_RU(DataSource ds) throws Exception {
        Connection con1 = ds.getConnection();
        Connection con2 = ds.getConnection();
        con1.setAutoCommit(false);
        con2.setAutoCommit(false);
        SqljCtx ctx1 = new SqljCtx(con1);
        SqljCtx ctx2 = new SqljCtx(con2);
        try {
            /* @lineinfo:generated-code */
            /* @lineinfo:1126^6 */

//  ************************************************************
//  #sql [ctx1] { INSERT INTO sqljtest VALUES (50, 'fifty')  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 63);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1126^62 */

            assertEquals(Connection.TRANSACTION_REPEATABLE_READ, con2.getTransactionIsolation());

            // Should not be able to read uncommitted data with RR isolation
            BasicIter cursor1;
            /* @lineinfo:generated-code *//* @lineinfo:1132^9 */

//  ************************************************************
//  #sql [ctx2] cursor1 = { SELECT id, name FROM sqljtest WHERE id=50  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 64);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1132^75 */
            ResultSet rs = cursor1.getResultSet();
            if (rs.next())
                fail("Should not be able to read uncommitted data!");
            rs.close();

            // Now read uncommitted data
            /* @lineinfo:generated-code *//* @lineinfo:1139^9 */

//  ************************************************************
//  #sql [ctx2] { SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 65);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1139^72 */
            assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, con2.getTransactionIsolation());

            /* @lineinfo:generated-code *//* @lineinfo:1142^9 */

//  ************************************************************
//  #sql [ctx2] cursor1 = { SELECT id, name FROM sqljtest WHERE id=50 };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 66);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1142^73 */
            rs = cursor1.getResultSet();
            verifyRow(50, "fifty", rs);
            rs.close();
        } finally {
            con1.setAutoCommit(true);
            con2.setAutoCommit(true);
            ctx1.close();
            ctx2.close();
        }
    }

    public static void testIsolation_RC(DataSource ds) throws Exception {
        Connection con1 = ds.getConnection();
        Connection con2 = ds.getConnection();
        con1.setAutoCommit(false);
        con2.setAutoCommit(false);
        SqljCtx ctx1 = new SqljCtx(con1);
        SqljCtx ctx2 = new SqljCtx(con2);
        try {
            /* @lineinfo:generated-code */
            /* @lineinfo:1163^9 */

//  ************************************************************
//  #sql [ctx2] { SET TRANSACTION ISOLATION LEVEL READ COMMITTED  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 67);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1163^70 */
            assertEquals(Connection.TRANSACTION_READ_COMMITTED, con2.getTransactionIsolation());

            /* @lineinfo:generated-code *//* @lineinfo:1166^6 */

//  ************************************************************
//  #sql [ctx1] { INSERT INTO sqljtest VALUES (51, 'fifty-one')  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 68);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1166^66 */
            /* @lineinfo:generated-code *//* @lineinfo:1167^6 */

//  ************************************************************
//  #sql [ctx1] { COMMIT  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 69);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1167^27 */

            // Read some committed data in tran 2
            BasicIter cursor1;
            /* @lineinfo:generated-code *//* @lineinfo:1171^9 */

//  ************************************************************
//  #sql [ctx2] cursor1 = { SELECT id, name FROM sqljtest WHERE id=51  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 70);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1171^75 */
            ResultSet rs = cursor1.getResultSet();
            verifyRow(51, "fifty-one", rs);
            rs.close();

            // Update the data in tran 1
            String name = "NEW_VAL";
            /* @lineinfo:generated-code *//* @lineinfo:1178^9 */

//  ************************************************************
//  #sql [ctx1] { UPDATE sqljtest SET name=:name WHERE id=51  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 71);
                    try {
                        __sJT_stmt.setString(1, name);
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1178^66 */

            // Attempt a dirty read and see if we get the old or new data here
            /* @lineinfo:generated-code *//* @lineinfo:1181^9 */

//  ************************************************************
//  #sql [ctx2] cursor1 = { SELECT id, name FROM sqljtest WHERE id=51  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 72);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1181^75 */
            rs = cursor1.getResultSet();
            verifyRow(51, "fifty-one", rs);
            rs.close();

            // Now commit the tran, and verify we can read the committed data
            /* @lineinfo:generated-code *//* @lineinfo:1187^9 */

//  ************************************************************
//  #sql [ctx1] { COMMIT  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx1;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 73);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1187^30 */
            /* @lineinfo:generated-code *//* @lineinfo:1188^9 */

//  ************************************************************
//  #sql [ctx2] cursor1 = { SELECT id, name FROM sqljtest WHERE id=51  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 74);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1188^75 */
            rs = cursor1.getResultSet();
            verifyRow(51, "NEW_VAL", rs);
            rs.close();
        } finally {
            con1.setAutoCommit(true);
            con2.setAutoCommit(true);
            ctx1.close();
            ctx2.close();
        }
    }

    /**
     * This test uses 2 threads (this thread, and a secondary thread) to produce the following ordering:
     * [ Thread-A ] ENTER
     * [ Thread-A ] first SELECT completed
     * [ Thread-B ] ENTER
     * [ Thread-B ] attempting to UPDATE...
     * [ Thread-A ] Waiting for 3s (to give Thread-B time to reach its lock)
     * [ Thread-A ] Done waiting for Thread-B (it appears to be blocked, which is expected)
     * [ Thread-A ] second SELECT completed
     * [ Thread-A ] EXIT
     * [ Thread-B ] UPDATE is complete
     * [ Thread-B ] EXIT
     */
    public static void testIsolation_RR(final DataSource ds) throws Throwable {
        Future<Void> future = null;
        System.out.println("[ Thread-A ] ENTER");
        Connection con2 = ds.getConnection();
        con2.setAutoCommit(false);
        SqljCtx ctx2 = new SqljCtx(con2);
        try {
            assertEquals(Connection.TRANSACTION_REPEATABLE_READ, con2.getTransactionIsolation());

            /* @lineinfo:generated-code *//* @lineinfo:1223^9 */

//  ************************************************************
//  #sql [ctx2] { INSERT INTO sqljtest VALUES (54, 'fifty-four')  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 75);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1223^70 */
            /* @lineinfo:generated-code *//* @lineinfo:1224^9 */

//  ************************************************************
//  #sql [ctx2] { COMMIT  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 76);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1224^30 */

            // Read row 54
            BasicIter cursor1;
            /* @lineinfo:generated-code *//* @lineinfo:1228^9 */

//  ************************************************************
//  #sql [ctx2] cursor1 = { SELECT id, name FROM sqljtest WHERE id=54  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 77);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1228^75 */
            System.out.println("[ Thread-A ] first SELECT completed");
            ResultSet rs = cursor1.getResultSet();
            verifyRow(54, "fifty-four", rs);
            rs.close();
            cursor1.close();

            // Start another thread that will attempt an UPDATE in another transaction
            final CountDownLatch updateStarted = new CountDownLatch(1);
            final CountDownLatch updateFinished = new CountDownLatch(1);
            ExecutorService exec = (ExecutorService) new InitialContext().lookup("java:comp/DefaultManagedExecutorService");
            future = exec.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    System.out.println("[ Thread-B ] ENTER");
                    Connection con1 = ds.getConnection();
                    con1.setAutoCommit(false);
                    SqljCtx ctx1 = new SqljCtx(con1);
                    try {
                        // Add another row of data in tran 1 with id=54
                        updateStarted.countDown();
                        System.out.println("[ Thread-B ] attempting to UPDATE...");
                        /* @lineinfo:generated-code *//* @lineinfo:1250^21 */

//  ************************************************************
//  #sql [ctx1] { UPDATE sqljtest SET name='NEW_VAL' WHERE id=54  };
//  ************************************************************

                        {
                            sqlj.runtime.ConnectionContext __sJT_connCtx = ctx1;
                            if (__sJT_connCtx == null)
                                sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                            sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                            if (__sJT_execCtx == null)
                                sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                            synchronized (__sJT_execCtx) {
                                sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 78);
                                try {
                                    __sJT_execCtx.executeUpdate();
                                } finally {
                                    __sJT_execCtx.releaseStatement();
                                }
                            }
                        }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1250^82 */
                        /* @lineinfo:generated-code *//* @lineinfo:1251^21 */

//  ************************************************************
//  #sql [ctx1] { COMMIT  };
//  ************************************************************

                        {
                            sqlj.runtime.ConnectionContext __sJT_connCtx = ctx1;
                            if (__sJT_connCtx == null)
                                sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                            sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                            if (__sJT_execCtx == null)
                                sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                            synchronized (__sJT_execCtx) {
                                sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 79);
                                try {
                                    __sJT_execCtx.executeUpdate();
                                } finally {
                                    __sJT_execCtx.releaseStatement();
                                }
                            }
                        }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1251^42 */
                        System.out.println("[ Thread-B ] UPDATE is complete");
                        updateFinished.countDown();
                    } finally {
                        con1.setAutoCommit(true);
                        ctx1.close();
                        System.out.println("[ Thread-B ] EXIT");
                    }
                    return null;
                }
            });
            updateStarted.await(90, TimeUnit.SECONDS);

            // Give Thread-A time to attempt it's INSERT, expect it to block
            // we expect that it will block until Thread-B's tran completes
            System.out.println("[ Thread-A ] Waiting for 3s (to give Thread-B time to reach its lock)");
            if (updateFinished.await(3, TimeUnit.SECONDS))
                fail("Thread-B should not have been able to UPDATE in the middle of A's transaction");
            else
                System.out.println("[ Thread-A ] Done waiting for Thread-B (it appears to be blocked, which is expected)");

            // Verify we get a repeatable read of row 54
            /* @lineinfo:generated-code *//* @lineinfo:1273^9 */

//  ************************************************************
//  #sql [ctx2] cursor1 = { SELECT id, name FROM sqljtest WHERE id=54  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 80);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1273^75 */
            System.out.println("[ Thread-A ] second SELECT completed");
            rs = cursor1.getResultSet();
            verifyRow(54, "fifty-four", rs);
            rs.close();
        } finally {
            con2.setAutoCommit(true);
            ctx2.close();
            System.out.println("[ Thread-A ] EXIT");
        }
        future.get(90, TimeUnit.SECONDS);
    }

    /**
     * This test uses 2 threads (this thread, and a secondary thread) to produce the following ordering:
     * [ Thread-A ] ENTER
     * [ Thread-A ] first SELECT completed
     * [ Thread-B ] ENTER
     * [ Thread-B ] attempting to INSERT...
     * [ Thread-A ] Waiting for 3s (to give Thread B time to reach its lock)
     * [ Thread-A ] Done waiting for Thread B (it appears to be blocked, which is expected)
     * [ Thread-A ] second SELECT completed
     * [ Thread-A ] EXIT
     * [ Thread-B ] INSERT is complete
     * [ Thread-B ] EXIT
     */
    public static void testIsolation_SER(final DataSource ds) throws Exception {
        Future<Void> future = null;
        System.out.println("[ Thread-A ] ENTER");
        Connection con2 = ds.getConnection();
        con2.setAutoCommit(false);
        SqljCtx ctx2 = new SqljCtx(con2);
        try {
            /* @lineinfo:generated-code */
            /* @lineinfo:1307^9 */

//  ************************************************************
//  #sql [ctx2] { SET TRANSACTION ISOLATION LEVEL SERIALIZABLE  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 81);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1307^68 */
            assertEquals(Connection.TRANSACTION_SERIALIZABLE, con2.getTransactionIsolation());

            /* @lineinfo:generated-code *//* @lineinfo:1310^9 */

//  ************************************************************
//  #sql [ctx2] { INSERT INTO sqljtest VALUES (52, 'fifty-two')  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 82);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1310^69 */
            /* @lineinfo:generated-code *//* @lineinfo:1311^9 */

//  ************************************************************
//  #sql [ctx2] { COMMIT  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 83);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1311^30 */

            // Read data between id 52-53 in tran 1.  Expect to get 1 row back
            BasicIter cursor1;
            /* @lineinfo:generated-code *//* @lineinfo:1315^9 */

//  ************************************************************
//  #sql [ctx2] cursor1 = { SELECT id, name FROM sqljtest WHERE id>=52 AND id<=53  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 84);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1315^87 */
            System.out.println("[ Thread-A ] first SELECT completed");
            ResultSet rs = cursor1.getResultSet();
            verifyRow(52, "fifty-two", rs);
            if (rs.next())
                fail("Got too many results: id=" + rs.getInt("id") + " name=" + rs.getString("name"));
            rs.close();
            cursor1.close();

            // Start another thread that will attempt an INSERT in another transaction
            final CountDownLatch insertStarted = new CountDownLatch(1);
            final CountDownLatch insertFinished = new CountDownLatch(1);
            ExecutorService exec = (ExecutorService) new InitialContext().lookup("java:comp/DefaultManagedExecutorService");
            future = exec.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    System.out.println("[ Thread-B ] ENTER");
                    Connection con1 = ds.getConnection();
                    con1.setAutoCommit(false);
                    SqljCtx ctx1 = new SqljCtx(con1);
                    try {
                        // Add another row of data in tran 1 with id=53
                        insertStarted.countDown();
                        System.out.println("[ Thread-B ] attempting to INSERT...");
                        /* @lineinfo:generated-code *//* @lineinfo:1339^21 */

//  ************************************************************
//  #sql [ctx1] { INSERT INTO sqljtest VALUES (53, 'fifty-three')  };
//  ************************************************************

                        {
                            sqlj.runtime.ConnectionContext __sJT_connCtx = ctx1;
                            if (__sJT_connCtx == null)
                                sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                            sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                            if (__sJT_execCtx == null)
                                sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                            synchronized (__sJT_execCtx) {
                                sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 85);
                                try {
                                    __sJT_execCtx.executeUpdate();
                                } finally {
                                    __sJT_execCtx.releaseStatement();
                                }
                            }
                        }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1339^83 */
                        /* @lineinfo:generated-code *//* @lineinfo:1340^21 */

//  ************************************************************
//  #sql [ctx1] { COMMIT  };
//  ************************************************************

                        {
                            sqlj.runtime.ConnectionContext __sJT_connCtx = ctx1;
                            if (__sJT_connCtx == null)
                                sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                            sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                            if (__sJT_execCtx == null)
                                sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                            synchronized (__sJT_execCtx) {
                                sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 86);
                                try {
                                    __sJT_execCtx.executeUpdate();
                                } finally {
                                    __sJT_execCtx.releaseStatement();
                                }
                            }
                        }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1340^42 */
                        System.out.println("[ Thread-B ] INSERT is complete");
                        insertFinished.countDown();
                    } finally {
                        con1.setAutoCommit(true);
                        ctx1.close();
                        System.out.println("[ Thread-B ] EXIT");
                    }
                    return null;
                }
            });
            insertStarted.await(90, TimeUnit.SECONDS);

            // Give Thread-A time to attempt it's INSERT, expect it to block
            // we expect that it will block until Thread-B's tran completes
            System.out.println("[ Thread-A ] Waiting for 3s (to give Thread-B time to reach its lock)");
            if (insertFinished.await(3, TimeUnit.SECONDS))
                fail("Thread-B should not have been able to INSERT in the middle of A's transaction");
            else
                System.out.println("[ Thread-A ] Done waiting for Thread-B (it appears to be blocked, which is expected)");

            // Verify we do not get a phantom read of row id=53
            /* @lineinfo:generated-code *//* @lineinfo:1362^9 */

//  ************************************************************
//  #sql [ctx2] cursor1 = { SELECT id, name FROM sqljtest WHERE id>=52 AND id<=53  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = ctx2;
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(1), 87);
                    try {
                        cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1362^87 */
            System.out.println("[ Thread-A ] second SELECT completed");
            rs = cursor1.getResultSet();
            verifyRow(52, "fifty-two", rs);
            if (rs.next())
                fail("Got too many results: id=" + rs.getInt("id") + " name=" + rs.getString("name"));
            rs.close();
        } finally {
            con2.setAutoCommit(true);
            ctx2.close();
            System.out.println("[ Thread-A ] EXIT");
        }
        future.get(90, TimeUnit.SECONDS);
    }

    public static void testDefaultContext(final DataSource ds) throws Exception {
        UserTransaction utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

        // Should not be able to use the default context initially
        try {
            /* @lineinfo:generated-code */
            /* @lineinfo:1383^6 */

//  ************************************************************
//  #sql { INSERT INTO sqljtest VALUES (55, 'fifty-five')  };
//  ************************************************************

            {
                sqlj.runtime.ConnectionContext __sJT_connCtx = sqlj.runtime.ref.DefaultContext.getDefaultContext();
                if (__sJT_connCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_DEFAULT_CONN_CTX();
                sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                if (__sJT_execCtx == null)
                    sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                synchronized (__sJT_execCtx) {
                    sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(3), 0);
                    try {
                        __sJT_execCtx.executeUpdate();
                    } finally {
                        __sJT_execCtx.releaseStatement();
                    }
                }
            }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1383^60 */
            fail("Expected an SQLException for trying to use a null default context.");
        } catch (SQLException expected) {
        }

        final DefaultContext dctx = new DefaultContext(ds.getConnection());
        DefaultContext.setDefaultContext(dctx);
        try {
            utx.begin();
            try {
                // Basic usage of default context
                /* @lineinfo:generated-code *//* @lineinfo:1393^7 */

//  ************************************************************
//  #sql { INSERT INTO sqljtest VALUES (55, 'fifty-five')  };
//  ************************************************************

                {
                    sqlj.runtime.ConnectionContext __sJT_connCtx = sqlj.runtime.ref.DefaultContext.getDefaultContext();
                    if (__sJT_connCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_DEFAULT_CONN_CTX();
                    sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                    if (__sJT_execCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                    synchronized (__sJT_execCtx) {
                        sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(3), 1);
                        try {
                            __sJT_execCtx.executeUpdate();
                        } finally {
                            __sJT_execCtx.releaseStatement();
                        }
                    }
                }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1393^61 */

                BasicIter cursor1;
                /* @lineinfo:generated-code *//* @lineinfo:1396^10 */

//  ************************************************************
//  #sql cursor1 = { SELECT id, name FROM sqljtest WHERE id=55  };
//  ************************************************************

                {
                    sqlj.runtime.ConnectionContext __sJT_connCtx = sqlj.runtime.ref.DefaultContext.getDefaultContext();
                    if (__sJT_connCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_DEFAULT_CONN_CTX();
                    sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                    if (__sJT_execCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                    synchronized (__sJT_execCtx) {
                        sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(3), 2);
                        try {
                            cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                        } finally {
                            __sJT_execCtx.releaseStatement();
                        }
                    }
                }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1396^69 */
                ResultSet rs = cursor1.getResultSet();
                verifyRow(55, "fifty-five", rs);
                rs.close();

                /* @lineinfo:generated-code *//* @lineinfo:1401^7 */

//  ************************************************************
//  #sql { DELETE FROM sqljtest WHERE id=55  };
//  ************************************************************

                {
                    sqlj.runtime.ConnectionContext __sJT_connCtx = sqlj.runtime.ref.DefaultContext.getDefaultContext();
                    if (__sJT_connCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_DEFAULT_CONN_CTX();
                    sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                    if (__sJT_execCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                    synchronized (__sJT_execCtx) {
                        sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(3), 3);
                        try {
                            __sJT_execCtx.executeUpdate();
                        } finally {
                            __sJT_execCtx.releaseStatement();
                        }
                    }
                }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1401^47 */
                utx.commit();
            } catch (Exception e) {
                utx.rollback();
                throw e;
            }

            assertNull("ThreadLocal default context should be null when ThreadLocalStorage is not enabled.",
                       DefaultContext.getThreadLocalContext());

            // The default context should be available from another thread
            ExecutorService exec = (ExecutorService) new InitialContext().lookup("java:comp/DefaultManagedExecutorService");
            Future<Void> future = exec.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    /* @lineinfo:generated-code */
                    /* @lineinfo:1416^14 */

//  ************************************************************
//  #sql { INSERT INTO sqljtest VALUES (56, 'fifty-six')  };
//  ************************************************************

                    {
                        sqlj.runtime.ConnectionContext __sJT_connCtx = sqlj.runtime.ref.DefaultContext.getDefaultContext();
                        if (__sJT_connCtx == null)
                            sqlj.runtime.error.RuntimeRefErrors.raise_NULL_DEFAULT_CONN_CTX();
                        sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                        if (__sJT_execCtx == null)
                            sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                        synchronized (__sJT_execCtx) {
                            sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(3), 4);
                            try {
                                __sJT_execCtx.executeUpdate();
                            } finally {
                                __sJT_execCtx.releaseStatement();
                            }
                        }
                    }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1416^67 */

                    BasicIter cursor2;
                    /* @lineinfo:generated-code *//* @lineinfo:1419^17 */

//  ************************************************************
//  #sql cursor2 = { SELECT id, name FROM sqljtest WHERE id=56  };
//  ************************************************************

                    {
                        sqlj.runtime.ConnectionContext __sJT_connCtx = sqlj.runtime.ref.DefaultContext.getDefaultContext();
                        if (__sJT_connCtx == null)
                            sqlj.runtime.error.RuntimeRefErrors.raise_NULL_DEFAULT_CONN_CTX();
                        sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                        if (__sJT_execCtx == null)
                            sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                        synchronized (__sJT_execCtx) {
                            sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(3), 5);
                            try {
                                cursor2 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                            } finally {
                                __sJT_execCtx.releaseStatement();
                            }
                        }
                    }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1419^76 */
                    ResultSet rs2 = cursor2.getResultSet();
                    verifyRow(56, "fifty-six", rs2);
                    rs2.close();

                    /* @lineinfo:generated-code *//* @lineinfo:1424^14 */

//  ************************************************************
//  #sql { DELETE FROM sqljtest WHERE id=56  };
//  ************************************************************

                    {
                        sqlj.runtime.ConnectionContext __sJT_connCtx = sqlj.runtime.ref.DefaultContext.getDefaultContext();
                        if (__sJT_connCtx == null)
                            sqlj.runtime.error.RuntimeRefErrors.raise_NULL_DEFAULT_CONN_CTX();
                        sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                        if (__sJT_execCtx == null)
                            sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                        synchronized (__sJT_execCtx) {
                            sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(3), 6);
                            try {
                                __sJT_execCtx.executeUpdate();
                            } finally {
                                __sJT_execCtx.releaseStatement();
                            }
                        }
                    }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1424^54 */

                    assertNull("ThreadLocal default context should be null when ThreadLocalStorage is not enabled.",
                               DefaultContext.getThreadLocalContext());

                    assertEquals(dctx, DefaultContext.getDefaultContext());
                    return null;
                }
            });
            future.get(90, TimeUnit.SECONDS);

            // LTC will end and dissociate/clean up the handle asynchronously, which will cause trouble if it overlaps
            // subsequent usage. A sleep here will reduce the chance of hitting this timing window.
            Thread.sleep(1000);

            System.out.println("about to do an insert after the new thread");
            utx.begin();
            try {
                /* @lineinfo:generated-code */
                /* @lineinfo:1442^7 */

//  ************************************************************
//  #sql { INSERT INTO sqljtest VALUES (57, 'fifty-seven')  };
//  ************************************************************

                {
                    sqlj.runtime.ConnectionContext __sJT_connCtx = sqlj.runtime.ref.DefaultContext.getDefaultContext();
                    if (__sJT_connCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_DEFAULT_CONN_CTX();
                    sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                    if (__sJT_execCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                    synchronized (__sJT_execCtx) {
                        sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(3), 7);
                        try {
                            __sJT_execCtx.executeUpdate();
                        } finally {
                            __sJT_execCtx.releaseStatement();
                        }
                    }
                }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1442^62 */

                BasicIter cursor1;
                /* @lineinfo:generated-code *//* @lineinfo:1445^10 */

//  ************************************************************
//  #sql cursor1 = { SELECT id, name FROM sqljtest WHERE id=57  };
//  ************************************************************

                {
                    sqlj.runtime.ConnectionContext __sJT_connCtx = sqlj.runtime.ref.DefaultContext.getDefaultContext();
                    if (__sJT_connCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_DEFAULT_CONN_CTX();
                    sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                    if (__sJT_execCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                    synchronized (__sJT_execCtx) {
                        sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(3), 8);
                        try {
                            cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                        } finally {
                            __sJT_execCtx.releaseStatement();
                        }
                    }
                }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1445^69 */
                ResultSet rs = cursor1.getResultSet();
                verifyRow(57, "fifty-seven", rs);
                rs.close();

                /* @lineinfo:generated-code *//* @lineinfo:1450^7 */

//  ************************************************************
//  #sql { DELETE FROM sqljtest WHERE id=57  };
//  ************************************************************

                {
                    sqlj.runtime.ConnectionContext __sJT_connCtx = sqlj.runtime.ref.DefaultContext.getDefaultContext();
                    if (__sJT_connCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_DEFAULT_CONN_CTX();
                    sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                    if (__sJT_execCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                    synchronized (__sJT_execCtx) {
                        sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(3), 9);
                        try {
                            __sJT_execCtx.executeUpdate();
                        } finally {
                            __sJT_execCtx.releaseStatement();
                        }
                    }
                }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1450^47 */
                utx.commit();
            } catch (Exception e) {
                utx.rollback();
                throw e;
            }

            // Now close the default context and expect an exception when we try to use it
            dctx.close();

            try {
                /* @lineinfo:generated-code */
                /* @lineinfo:1461^10 */

//  ************************************************************
//  #sql { INSERT INTO sqljtest VALUES (57, 'fifty-seven')  };
//  ************************************************************

                {
                    sqlj.runtime.ConnectionContext __sJT_connCtx = sqlj.runtime.ref.DefaultContext.getDefaultContext();
                    if (__sJT_connCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_DEFAULT_CONN_CTX();
                    sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                    if (__sJT_execCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                    synchronized (__sJT_execCtx) {
                        sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(3), 10);
                        try {
                            __sJT_execCtx.executeUpdate();
                        } finally {
                            __sJT_execCtx.releaseStatement();
                        }
                    }
                }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1461^65 */
                fail("Expected an SQLException from trying to use a closed context.");
            } catch (SQLException expected) {
            }
        } finally {
            // Reset the default context
            DefaultContext.setDefaultContext(null);
        }
    }

    public static void testDefaultContext_threadlocal(final DataSource ds) throws Exception {
        UserTransaction utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");

        final DefaultContext dctx = new DefaultContext(ds.getConnection());
        DefaultContext.enableThreadLocalStorage();
        DefaultContext.setDefaultContext(dctx);
        try {
            utx.begin();
            try {
                // Verify basic usage of default context when thread local storage is enabled
                /* @lineinfo:generated-code *//* @lineinfo:1481^7 */

//  ************************************************************
//  #sql { INSERT INTO sqljtest VALUES (58, 'fifty-eight')  };
//  ************************************************************

                {
                    sqlj.runtime.ConnectionContext __sJT_connCtx = sqlj.runtime.ref.DefaultContext.getDefaultContext();
                    if (__sJT_connCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_DEFAULT_CONN_CTX();
                    sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                    if (__sJT_execCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                    synchronized (__sJT_execCtx) {
                        sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(3), 11);
                        try {
                            __sJT_execCtx.executeUpdate();
                        } finally {
                            __sJT_execCtx.releaseStatement();
                        }
                    }
                }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1481^62 */

                BasicIter cursor1;
                /* @lineinfo:generated-code *//* @lineinfo:1484^10 */

//  ************************************************************
//  #sql cursor1 = { SELECT id, name FROM sqljtest WHERE id=58  };
//  ************************************************************

                {
                    sqlj.runtime.ConnectionContext __sJT_connCtx = sqlj.runtime.ref.DefaultContext.getDefaultContext();
                    if (__sJT_connCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_DEFAULT_CONN_CTX();
                    sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                    if (__sJT_execCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                    synchronized (__sJT_execCtx) {
                        sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(3), 12);
                        try {
                            cursor1 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                        } finally {
                            __sJT_execCtx.releaseStatement();
                        }
                    }
                }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1484^69 */
                ResultSet rs = cursor1.getResultSet();
                verifyRow(58, "fifty-eight", rs);
                rs.close();

                /* @lineinfo:generated-code *//* @lineinfo:1489^7 */

//  ************************************************************
//  #sql { DELETE FROM sqljtest WHERE id=58  };
//  ************************************************************

                {
                    sqlj.runtime.ConnectionContext __sJT_connCtx = sqlj.runtime.ref.DefaultContext.getDefaultContext();
                    if (__sJT_connCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_DEFAULT_CONN_CTX();
                    sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                    if (__sJT_execCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                    synchronized (__sJT_execCtx) {
                        sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(3), 13);
                        try {
                            __sJT_execCtx.executeUpdate();
                        } finally {
                            __sJT_execCtx.releaseStatement();
                        }
                    }
                }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1489^47 */
                utx.commit();
            } catch (Exception e) {
                utx.rollback();
                throw e;
            }

            // The 'normal' and thread local contexts should be equal
            final DefaultContext dctx_threadlocal = DefaultContext.getThreadLocalContext().get();
            assertNotNull("ThreadLocal default context should not be null when ThreadLocalStorage is enabled.",
                          dctx_threadlocal);
            assertEquals(dctx, dctx_threadlocal);

            ExecutorService exec = (ExecutorService) new InitialContext().lookup("java:comp/DefaultManagedExecutorService");
            Future<Void> future = exec.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    // When thread local storage is enabled, each thread has to establish their own default context
                    // At first, this thread will not have any default context
                    try {
                        /* @lineinfo:generated-code */
                        /* @lineinfo:1509^15 */

//  ************************************************************
//  #sql { INSERT INTO sqljtest VALUES (59, 'fifty-nine')  };
//  ************************************************************

                        {
                            sqlj.runtime.ConnectionContext __sJT_connCtx = sqlj.runtime.ref.DefaultContext.getDefaultContext();
                            if (__sJT_connCtx == null)
                                sqlj.runtime.error.RuntimeRefErrors.raise_NULL_DEFAULT_CONN_CTX();
                            sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                            if (__sJT_execCtx == null)
                                sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                            synchronized (__sJT_execCtx) {
                                sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(3), 14);
                                try {
                                    __sJT_execCtx.executeUpdate();
                                } finally {
                                    __sJT_execCtx.releaseStatement();
                                }
                            }
                        }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1509^69 */
                        fail("Should not be able to use default context on a new thread when a ThreadLocal default context has not been set yet.");
                    } catch (SQLException expected) {
                    }

                    DefaultContext callableDefCtx = new DefaultContext(ds.getConnection());
                    DefaultContext.setDefaultContext(callableDefCtx);
                    /* @lineinfo:generated-code *//* @lineinfo:1515^14 */

//  ************************************************************
//  #sql { INSERT INTO sqljtest VALUES (59, 'fifty-nine')  };
//  ************************************************************

                    {
                        sqlj.runtime.ConnectionContext __sJT_connCtx = sqlj.runtime.ref.DefaultContext.getDefaultContext();
                        if (__sJT_connCtx == null)
                            sqlj.runtime.error.RuntimeRefErrors.raise_NULL_DEFAULT_CONN_CTX();
                        sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                        if (__sJT_execCtx == null)
                            sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                        synchronized (__sJT_execCtx) {
                            sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(3), 15);
                            try {
                                __sJT_execCtx.executeUpdate();
                            } finally {
                                __sJT_execCtx.releaseStatement();
                            }
                        }
                    }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1515^68 */

                    BasicIter cursor2;
                    /* @lineinfo:generated-code *//* @lineinfo:1518^17 */

//  ************************************************************
//  #sql cursor2 = { SELECT id, name FROM sqljtest WHERE id=59  };
//  ************************************************************

                    {
                        sqlj.runtime.ConnectionContext __sJT_connCtx = sqlj.runtime.ref.DefaultContext.getDefaultContext();
                        if (__sJT_connCtx == null)
                            sqlj.runtime.error.RuntimeRefErrors.raise_NULL_DEFAULT_CONN_CTX();
                        sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                        if (__sJT_execCtx == null)
                            sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                        synchronized (__sJT_execCtx) {
                            sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(3), 16);
                            try {
                                cursor2 = new BasicIter(__sJT_execCtx.executeQuery(), __sJT_execCtx.getFetchSize(), __sJT_execCtx.getMaxRows());
                            } finally {
                                __sJT_execCtx.releaseStatement();
                            }
                        }
                    }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1518^76 */
                    ResultSet rs2 = cursor2.getResultSet();
                    verifyRow(59, "fifty-nine", rs2);
                    rs2.close();

                    /* @lineinfo:generated-code *//* @lineinfo:1523^14 */

//  ************************************************************
//  #sql { DELETE FROM sqljtest WHERE id=59  };
//  ************************************************************

                    {
                        sqlj.runtime.ConnectionContext __sJT_connCtx = sqlj.runtime.ref.DefaultContext.getDefaultContext();
                        if (__sJT_connCtx == null)
                            sqlj.runtime.error.RuntimeRefErrors.raise_NULL_DEFAULT_CONN_CTX();
                        sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                        if (__sJT_execCtx == null)
                            sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                        synchronized (__sJT_execCtx) {
                            sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(3), 17);
                            try {
                                __sJT_execCtx.executeUpdate();
                            } finally {
                                __sJT_execCtx.releaseStatement();
                            }
                        }
                    }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1523^54 */

                    // Verify that the 'normal' and thread local default context are the same
                    assertEquals(DefaultContext.getDefaultContext(),
                                 DefaultContext.getThreadLocalContext().get());
                    assertNotNull("ThreadLocal default context should not be null when ThreadLocalStorage is enabled.",
                                  DefaultContext.getThreadLocalContext().get());
                    // Verify that two different threads are not using the same thread local context
                    assertNotSame(dctx_threadlocal, DefaultContext.getThreadLocalContext().get());

                    // Close and clear the default context in case the executor uses this thread again
                    callableDefCtx.close();
                    DefaultContext.setDefaultContext(null);
                    return null;
                }
            });
            future.get(90, TimeUnit.SECONDS);

            // After running the task, the default context for this thread should be unchanged
            assertEquals(dctx, DefaultContext.getThreadLocalContext().get());

            // Now close the default context and expect an exception
            dctx.close();
            try {
                /* @lineinfo:generated-code */
                /* @lineinfo:1547^10 */

//  ************************************************************
//  #sql { INSERT INTO sqljtest VALUES (60, 'sixty')  };
//  ************************************************************

                {
                    sqlj.runtime.ConnectionContext __sJT_connCtx = sqlj.runtime.ref.DefaultContext.getDefaultContext();
                    if (__sJT_connCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_DEFAULT_CONN_CTX();
                    sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
                    if (__sJT_execCtx == null)
                        sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
                    synchronized (__sJT_execCtx) {
                        sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(3), 18);
                        try {
                            __sJT_execCtx.executeUpdate();
                        } finally {
                            __sJT_execCtx.releaseStatement();
                        }
                    }
                }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1547^59 */
                fail("Expected an SQLException from trying to use a closed context.");
            } catch (SQLException expected) {
            }
        } finally {
            // Reset the default context
            DefaultContext.setDefaultContext(null);
        }
    }

    private static void verifyRow(int expID, String expName, ResultSet rs) throws Exception {
        if (rs.next()) {
            assertEquals(rs.getInt("id"), expID);
            assertEquals(rs.getString("name"), expName);
        } else {
            fail("Did not find a record with id=" + expID);
        }
    }
}

class ConnectionContextWrapper implements ConnectionContext {
    WrappedConContext wctx;

    ConnectionContextWrapper(Connection con) throws SQLException {
        wctx = new WrappedConContext(con);
    }

    @Override
    public void close() throws SQLException {
        wctx.close();
    }

    @Override
    public void close(boolean closeConnection) throws SQLException {
        wctx.close(closeConnection);
    }

    public void commit() throws SQLException {
        /* @lineinfo:generated-code */
        /* @lineinfo:1581^8 */

//  ************************************************************
//  #sql [wctx] { COMMIT };
//  ************************************************************

        {
            sqlj.runtime.ConnectionContext __sJT_connCtx = wctx;
            if (__sJT_connCtx == null)
                sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
            sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
            if (__sJT_execCtx == null)
                sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
            synchronized (__sJT_execCtx) {
                sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(4), 0);
                try {
                    __sJT_execCtx.executeUpdate();
                } finally {
                    __sJT_execCtx.releaseStatement();
                }
            }
        }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1581^27 */
    }

    @Override
    public ConnectedProfile getConnectedProfile(Object profile) throws SQLException {
        return wctx.getConnectedProfile(profile);
    }

    @Override
    public Connection getConnection() {
        return wctx.getConnection();
    }

    @Override
    public ExecutionContext getExecutionContext() {
        return wctx.getExecutionContext();
    }

    @Override
    public Map<?, ?> getTypeMap() {
        return wctx.getTypeMap();
    }

    @Override
    public boolean isClosed() {
        return wctx.isClosed();
    }

    public void rollback() throws SQLException {
        /* @lineinfo:generated-code */
        /* @lineinfo:1605^8 */

//  ************************************************************
//  #sql [wctx] { ROLLBACK };
//  ************************************************************

        {
            sqlj.runtime.ConnectionContext __sJT_connCtx = wctx;
            if (__sJT_connCtx == null)
                sqlj.runtime.error.RuntimeRefErrors.raise_NULL_CONN_CTX();
            sqlj.runtime.ExecutionContext __sJT_execCtx = __sJT_connCtx.getExecutionContext();
            if (__sJT_execCtx == null)
                sqlj.runtime.error.RuntimeRefErrors.raise_NULL_EXEC_CTX();
            synchronized (__sJT_execCtx) {
                sqlj.runtime.profile.RTStatement __sJT_stmt = __sJT_execCtx.registerStatement(__sJT_connCtx, SQLJProcedure_SJProfileKeys.getKey(4), 1);
                try {
                    __sJT_execCtx.executeUpdate();
                } finally {
                    __sJT_execCtx.releaseStatement();
                }
            }
        }

//  ************************************************************

/* @lineinfo:user-code *//* @lineinfo:1605^29 */
    }
}

/* @lineinfo:generated-code */class SQLJProcedure_SJProfileKeys {
    private java.lang.Object[] keys;
    private final sqlj.runtime.profile.Loader loader = sqlj.runtime.RuntimeContext.getRuntime().getLoaderForClass(getClass());
    private static SQLJProcedure_SJProfileKeys inst = null;

    public static java.lang.Object getKey(int keyNum) throws java.sql.SQLException {
        synchronized (web.SQLJProcedure_SJProfileKeys.class) {
            if (inst == null) {
                inst = new SQLJProcedure_SJProfileKeys();
            }
        }
        return inst.keys[keyNum];
    }

    private SQLJProcedure_SJProfileKeys() throws java.sql.SQLException {
        keys = new java.lang.Object[5];
        keys[0] = MyCtx.getProfileKey(loader, "web.SQLJProcedure_SJProfile0");
        keys[1] = SqljCtx.getProfileKey(loader, "web.SQLJProcedure_SJProfile1");
        keys[2] = SqljDSCtx.getProfileKey(loader, "web.SQLJProcedure_SJProfile2");
        keys[3] = sqlj.runtime.ref.DefaultContext.getProfileKey(loader, "web.SQLJProcedure_SJProfile3");
        keys[4] = WrappedConContext.getProfileKey(loader, "web.SQLJProcedure_SJProfile4");
    }
}
