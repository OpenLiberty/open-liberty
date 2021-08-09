/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.request.timing;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

@WebServlet({ "/LoggedIn2" })
public class Login2 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Resource(name = "jdbc/exampleDS")
    DataSource ds1;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        Statement stmt = null;

        Connection con = null;
        try {
            PrintWriter pw = response.getWriter();
            System.out.println("Create DataSource connection");
            con = this.ds1.getConnection();
            System.out.println(" connection : " + con);
            stmt = con.createStatement();
            System.out.println(" statement " + stmt);
            try {
                stmt.executeUpdate("create table exampleDS1.cities (name varchar(50) not null, population int, county varchar(30))");
            } catch (Exception e) {

            }
            System.out.println(" over execute update.. ");
            for (int i = 1; i <= 3; i++) {

                System.out.println(stmt.executeUpdate("insert into exampleDS1.cities values ('myHomeCity_ " + i + "', " + i + ", 'myHomeCounty_" + i + "')"));

            }
            stmt.execute("select * from exampleDS1.cities");
            System.out.println("doGet completed Successfully");
        } catch (Exception e) {

        } finally {
            try {
                if (stmt != null) {
                    stmt.executeUpdate("drop table exampleDS1.cities");
                    stmt.close();
                } else
                    System.out.println("stmt is null");
                try {
                    System.out.println("Thread Sleeping..");
                    Thread.sleep(1000);
                    System.out.println("Thread Woke up..");
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (con != null)
                    con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}
}
