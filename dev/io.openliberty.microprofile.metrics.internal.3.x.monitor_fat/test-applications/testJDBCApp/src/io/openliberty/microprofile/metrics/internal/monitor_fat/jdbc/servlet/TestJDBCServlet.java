/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.metrics.internal.monitor_fat.jdbc.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;


@WebServlet("/testJDBCServlet")
public class TestJDBCServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(TestJDBCServlet.class.toString());
	
	@Resource(name="jdbc/exampleDS1")
	DataSource ds1;

	@Resource(name="jdbc/exampleDS2")
	DataSource ds2;
  
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    
		StringWriter body = new StringWriter();
	  
		String operation = request.getParameter("operation");
		if (operation == null) {
			body.append("No operation");
		} else if (operation.equalsIgnoreCase("create")) {
			Connection con1 = null;
			Connection con2 = null;
			try {
				con1 = this.ds1.getConnection();
				con2 = this.ds2.getConnection();
				Statement stmt1 = con1.createStatement();
				Statement stmt2 = con2.createStatement();
				String sql1 = "create table cities (name varchar(50) not null primary key, population int, county varchar(30))";
				int result1 = stmt1.executeUpdate(sql1);
				con1.commit();
				logger.info(sql1);
				body.append("sql: " + sql1).append("<br>").append("result: " + result1).append("<br>");
				String sql2 = "create table customers (id varchar(10) not null primary key, age int, name varchar(50))";
				int result2 = stmt2.executeUpdate(sql2);
				con2.commit();
				logger.info(sql2);
				body.append("sql: " + sql2).append("<br>").append("result: " + result2).append("<br>");
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				closeConnection(con1);
				closeConnection(con2);
			}
		} else if (operation.equalsIgnoreCase("insert")) {
			Connection con1 = null;
			Connection con2 = null;
			try {
				con1 = this.ds1.getConnection();
				con2 = this.ds2.getConnection();
				Statement stmt1 = con1.createStatement();
				Statement stmt2 = con2.createStatement();

				String city = request.getParameter("city");
				String population = request.getParameter("population");
				String county = request.getParameter("county");
				String sql1 = "insert into cities values ('" + city + "', " + population + ", '" + county + "')";
				int result1 = stmt1.executeUpdate(sql1);
				con1.commit();
				logger.info(sql1);
				body.append("sql: " + sql1).append("<br>").append("result: " + result1).append("<br>");
				
				String id = request.getParameter("id");
				String age = request.getParameter("age");
				String name = request.getParameter("name");
				String sql2 = "insert into customers values ('" + id + "', " + age + ", '" + name + "')";
				int result2 = stmt2.executeUpdate(sql2);
				con2.commit();
				logger.info(sql2);
				body.append("sql: " + sql2).append("<br>").append("result: " + result2).append("<br>");
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				closeConnection(con1);
				closeConnection(con2);
			}
		} else if (operation.equalsIgnoreCase("select")) {
			Connection con1 = null;
			Connection con2 = null;
			try {
				con1 = this.ds1.getConnection();
				con2 = this.ds2.getConnection();
				Statement stmt1 = con1.createStatement();
				Statement stmt2 = con2.createStatement();
				
				String city = request.getParameter("city");
				String sql1 = city == null ? "select * from cities" : "select county from cities where name='" + city + "'";
				ResultSet result1 = stmt1.executeQuery(sql1);
				boolean n1 = result1.next();
				logger.info(sql1);
				body.append("sql: " + sql1).append("<br>").append("result: " + n1).append("<br>");

				String id = request.getParameter("id");
				String sql2 = id == null? "select * from customers" : "select name from customers where id='" + id + "'";
				ResultSet result2 = stmt2.executeQuery(sql2);
				boolean n2 = result2.next();
				logger.info(sql2);
				body.append("sql: " + sql2).append("<br>").append("result: " + n2).append("<br>");
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				closeConnection(con1);
				closeConnection(con2);
			}
		} else if (operation.equalsIgnoreCase("drop")) {
			Connection con1 = null;
			Connection con2 = null;
			try {
				con1 = this.ds1.getConnection();
				con2 = this.ds2.getConnection();
				Statement stmt1 = con1.createStatement();
				Statement stmt2 = con2.createStatement();
				String sql1 = "drop table cities";
				int result1 = stmt1.executeUpdate(sql1);
				con1.commit();
				logger.info(sql1);
				body.append("sql: " + sql1).append("<br>").append("result: " + result1).append("<br>");
				String sql2 = "drop table customers";
				int result2 = stmt2.executeUpdate(sql2);
				con2.commit();
				logger.info(sql2);
				body.append("sql: " + sql2).append("<br>").append("result: " + result2).append("<br>");
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				closeConnection(con1);
				closeConnection(con2);
			}
		} else {
			body.append("Unknown operation");
		}

		PrintWriter out = response.getWriter();
		out.println("<html>");
    	out.println("<head><title>Test JDBC Head</title></head>");
    	out.println("<body>Test JDBC Body<br/>");
		out.println(body.toString());
		out.println("</body>");
		out.println("</html>");
	}

	private void closeConnection(Connection c) {
		if (c != null) {
			try {
				c.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
}
