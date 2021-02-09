package com.ibm.ws.commitPriority.common;

import java.sql.Connection;
//import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;
import javax.transaction.UserTransaction;

/**
 * Commit Priority test function
 */
public class CommitPriorityTestUtils {

    public static String test(UserTransaction ut, DataSource ds1, DataSource ds2, DataSource ds3) throws Exception {

        Connection con1 = ds1.getConnection();
        Connection con2 = ds2.getConnection();
        Connection con3 = ds3.getConnection();

        try {
            // Set up table
            Statement stmt1 = con1.createStatement();
            Statement stmt2 = con2.createStatement();
            Statement stmt3 = con3.createStatement();

            try {
                stmt1.executeUpdate("drop table bvtable");
                stmt2.executeUpdate("drop table bvtable");
                stmt3.executeUpdate("drop table bvtable");
            } catch (SQLException x) {
                // didn't exist
            }

            stmt1.executeUpdate("create table bvtable (col1 int not null primary key, col2 varchar(20))");
            stmt1.executeUpdate("insert into bvtable values (1, 'one')");
            stmt1.executeUpdate("insert into bvtable values (2, 'two')");
            stmt1.executeUpdate("insert into bvtable values (3, 'three')");
            stmt1.executeUpdate("insert into bvtable values (4, 'four')");

            stmt2.executeUpdate("create table bvtable (col1 int not null primary key, col2 varchar(20))");
            stmt2.executeUpdate("insert into bvtable values (1, 'one')");
            stmt2.executeUpdate("insert into bvtable values (2, 'two')");
            stmt2.executeUpdate("insert into bvtable values (3, 'three')");
            stmt2.executeUpdate("insert into bvtable values (4, 'four')");

            stmt3.executeUpdate("create table bvtable (col1 int not null primary key, col2 varchar(20))");
            stmt3.executeUpdate("insert into bvtable values (1, 'one')");
            stmt3.executeUpdate("insert into bvtable values (2, 'two')");
            stmt3.executeUpdate("insert into bvtable values (3, 'three')");
            stmt3.executeUpdate("insert into bvtable values (4, 'four')");

            // UserTransaction Commit
            con1.setAutoCommit(false);
            con2.setAutoCommit(false);
            con3.setAutoCommit(false);

            ut.begin();
            try {
                stmt1 = con1.createStatement();
                stmt1.executeUpdate("update bvtable set col2='uno' where col1=1");

                stmt2 = con2.createStatement();
                stmt2.executeUpdate("update bvtable set col2='uno' where col1=1");

                stmt3 = con3.createStatement();
                stmt3.executeUpdate("update bvtable set col2='uno' where col1=1");
            } finally {
                ut.commit();
            }
        } finally {
            con1.close();
            con2.close();
            con3.close();
        }

        return "";
    }
}