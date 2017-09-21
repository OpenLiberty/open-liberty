/*COPYRIGHT_START******************************initDBSettings*****************************
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
import java.sql.SQLException;
import java.sql.Statement;

public abstract class DatabaseHandler {

    public abstract int getSmallColumnSize();
    
    public abstract int getMediumColumnSize();
    
    public abstract int getLargeColumnSize();
    
    public abstract void createTable(Statement s, String tableName) throws SQLException;
    
    public abstract void createIndex(Connection con, Statement s, String tableName) throws SQLException; 
    
    public abstract boolean doesIndexExists(Connection con, String indexName, String tableName);

    /**
     * Checks if the given connection is still valid.
     * 
     * @param conn
     * @return Returns true if the connection is valid. False, otherwise.
     */
    public boolean isConnectionValid(Connection conn) {
        try {
            return conn.isValid(5);
        } catch (Exception e) {
            return false;
        }
    }
    
}
