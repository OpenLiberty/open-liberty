/*COPYRIGHT_START***********************************************************
 *
 * IBM Confidential OCO Source Material
 * 5724-J08, 5724-I63, 5724-H88, 5724-H89, 5655-N02, 5733-W70 (C) COPYRIGHT International Business Machines Corp. 1997, 2012
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 *
 *   IBM DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING
 *   ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *   PURPOSE. IN NO EVENT SHALL IBM BE LIABLE FOR ANY SPECIAL, INDIRECT OR
 *   CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF
 *   USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
 *   OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE
 *   OR PERFORMANCE OF THIS SOFTWARE.
 *
 *  @(#) 1.4 SERV1/ws/code/session.store/src/com/ibm/ws/session/store/db/DatabaseSession.java, WAS.session, WAS70.SERV1, cf070931.26 3/12/08 09:22:04 [8/10/09 20:07:47]
 *
 * @(#)file   DatabaseSession.java
 * @(#)version   1.4
 * @(#)date      3/12/08
 *
 *COPYRIGHT_END*************************************************************/
package com.ibm.ws.session.store.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import com.ibm.ws.session.store.common.LoggingUtil;

public class PostgreSQLHandler extends DatabaseHandler {

    private static final String methodClassName = "PosgreSQLHandler";
    private static final String indexQuery = "select indexdef from pg_indexes " + 
                                             "where indexname = ? and tablename = ?";
    
    @Override
    public int getSmallColumnSize() {
        /* set to 10M since to force use of small column since no size is associated with a column */
        return 10 * 1024 * 1024; 
    }
    
    @Override    
    public int getMediumColumnSize() {
        return 1;
    }
    
    @Override
    public int getLargeColumnSize() {
        return 1;
    }
    
    @Override
    public void createTable(Statement s, String tableName) throws SQLException {
        s.executeUpdate("create table "
                        + tableName
                        + " (id varchar(128) not null, propid varchar(128) not null, appname varchar(128) not null, listenercnt smallint, lastaccess bigint, creationtime bigint, maxinactivetime integer, username varchar(255), small bytea, medium bytea, large bytea)");
    }
    
    @Override
    public void createIndex(Connection con, Statement s, String tableName) throws SQLException {
        String methodName = "createIndex";
        if (doesIndexExists(con, "sess_index", tableName)) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.FINE, methodClassName, methodName, "Skip index creation");
        } else {
            s.executeUpdate("create unique index sess_index on " + tableName + " (id, propid, appname)");
        }
    }
    
    @Override
    public boolean doesIndexExists(Connection con, String indexName, String tableName) {
        String methodName = "doesIndexExists";
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.entering(methodClassName, methodName);
        }
        
        boolean indexExists = false;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = con.prepareStatement(indexQuery);
            ps.setString(1, indexName);
            ps.setString(2, tableName);
            rs = ps.executeQuery();
            if (rs.next()) {
                indexExists = true;
            }
        } catch (Throwable th) {
            LoggingUtil.SESSION_LOGGER_WAS.logp(Level.SEVERE, methodClassName, methodName, "CommonMessage.exception", th);
        } finally {
            if (rs != null) {
                DatabaseHashMap.closeResultSet(rs);
            }
            if (ps != null) {
                DatabaseHashMap.closeStatement(ps);
            }
        }
        
        if (com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_WAS.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_WAS.exiting(methodClassName, methodName, indexExists);
        }
        return indexExists;        
    }
}