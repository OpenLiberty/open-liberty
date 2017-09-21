/*COPYRIGHT_START***********************************************************
 *
 * IBM Confidential OCO Source Material
 * 5724-I63, 5724-H88, 5724-H89, 5655-N02, 5724-J08 (C) COPYRIGHT International Business Machines Corp. 1997, 2012
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
 *  @(#) 1.3 SERV1/ws/code/session.store/src/com/ibm/ws/session/store/db/DBPortability.java, WAS.session, WASX.SERV1, ff1146.05 2/22/11 13:27:28 [11/21/11 18:33:09]
 *
 * @(#)file   DBPortability.java
 * @(#)version   1.3
 * @(#)date      2/22/11
 *
 *COPYRIGHT_END*************************************************************/
package com.ibm.ws.session.store.db;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class DBPortability {

    /*
     * Instead of using database meta-data, an integer code
     * is used to identify a database in the hope of improving
     * performance.
     */
    public static final int UNKNOWN = 0;
    public static final int DB2_NT = 1;
    public static final int DB2_AS400 = 2;
    public static final int INSTANTDB = 3;
    public static final int DB2_AIX = 1;
    public static final int DB2_SUN = 1;
    public static final int DB2_HPUX = 1;
    public static final int ORACLE = 4;
    public static final int SYBASE = 5;
    public static final int DB2_CONNECT = 6;
    public static final int MICROSOFT_SQLSERVER = 7;
    public static final int INFORMIX = 8;
    public static final int CLOUDSCAPE = 9;
    public static final int DB2_zOS = 10; // LIDB2775.25 zOS
    public static final int DERBY = 11; //should be the same as cloudscape
    public static final int SOLIDDB = 12;
    public static final int POSTGRESQL = 13;
    public static final int MYSQL = 14;

    public static int getDBCode(DatabaseMetaData dbMetaData) throws SQLException {

        String dbProductName = dbMetaData.getDatabaseProductName();
        if (dbProductName.toUpperCase().indexOf("INSTANTDB") != -1)
            return INSTANTDB;
        else if (dbProductName.toUpperCase().indexOf("DB2/NT") != -1)
            return DB2_NT;
        else if (dbProductName.toUpperCase().indexOf("DB2/SUN") != -1)
            return DB2_SUN;
        else if (dbProductName.toUpperCase().indexOf("DB2/6000") != -1)
            return DB2_AIX;
        else if (dbProductName.toUpperCase().indexOf("DB2/HPUX") != -1)
            return DB2_HPUX;
        else if ((dbProductName.toUpperCase().indexOf("DB2 UDB FOR AS/400") != -1) || (dbProductName.toUpperCase().indexOf("DB2/400 SQL") != -1)
                 || (dbProductName.toUpperCase().indexOf("DB2 UDB for eServer iSeries") != -1)) //97919
            return DB2_AS400;
        else if (dbProductName.toUpperCase().indexOf("DB2/LINUX") != -1)
            return DB2_SUN;
        else if (dbProductName.toUpperCase().indexOf("ORACLE") != -1)
            return ORACLE;
        else if ((dbProductName.toUpperCase().indexOf("SYBASE SQL SERVER") != -1) || (dbProductName.toUpperCase().indexOf("ADAPTIVE SERVER ENTERPRISE") != -1))
            return SYBASE;
        else if (dbProductName.toUpperCase().trim().equals("DB2")) {
            String dbProductVersion = dbMetaData.getDatabaseProductVersion(); // cmd pok_PQ97422
            if (dbProductVersion.toUpperCase().indexOf("DSN") != -1) { // cmd pok_PQ97422 handle jcc driver
                return DB2_zOS;
            } else {
                return DB2_CONNECT;
            }
        } else if (dbProductName.toUpperCase().indexOf("MICROSOFT") != -1) {
            return MICROSOFT_SQLSERVER;
        } else if ((dbProductName.toUpperCase().indexOf("INFORMIX") != -1) || (dbProductName.toUpperCase().indexOf("IDS") != -1)) {
            return INFORMIX;
        } else if (dbProductName.toUpperCase().indexOf("DSN") != -1) { // LIDB2775.25 zOS
            return DB2_zOS; // LIDB2775.25 zOS
        } else if ((dbProductName.toUpperCase().indexOf("DERBY") != -1) || (dbProductName.toUpperCase().indexOf("DB2J") != -1)) {
            return DERBY;
        } else if ((dbProductName.toUpperCase().indexOf("CLOUDSCAPE") != -1) || (dbProductName.toUpperCase().indexOf("DB2J") != -1)) {
            return CLOUDSCAPE;
        } else if (dbProductName.toUpperCase().indexOf("IBM SOLIDDB") != -1) {
            return SOLIDDB;
        } else if (dbProductName.toUpperCase().indexOf("POSTGRESQL") != -1) {
            return POSTGRESQL;
        } else if (dbProductName.toUpperCase().indexOf("MYSQL") != -1) {
            return MYSQL;
        } else {
            return DB2_NT; // generic enough???
        }
    }

}