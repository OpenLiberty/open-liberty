/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.concurrent.Future;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

/**
 * To run the demo:
 * wlp\bin>server run com.ibm.ws.concurrent.fat.demo
 * observe which port number is being used, for example 9080,
 * then browse to:
 * localhost:9080/shipestimate
 */
@WebServlet(value = "/")
public class ShippingEstimateServlet extends HttpServlet {
    private static final long serialVersionUID = 6774745297364025318L;

    @Resource(name = "java:comp/env/jdbc/testdbRef", lookup = "jdbc/testdb")
    private DataSource dataSource;

    @Resource
    private ManagedExecutorService executor;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            PrintWriter out = response.getWriter();
            out.println("<h3>Shipping & Handling Calculator</h3>");

            String productName = request.getParameter("productName");
            String stateName = request.getParameter("stateName");
            if (productName != null && stateName != null) {
                Future<Float> baseShippingCharge = executor.submit(new BaseShippingChargeFinder(stateName));
                Future<Float> salesTaxRate = executor.submit(new SalesTaxRateFinder(stateName));
                float basePrice;
                float additionalShippingCharges = 0.0f;

                try {
                    Connection con = dataSource.getConnection();
                    try {
                        PreparedStatement pstmt = con.prepareStatement("select * from Products where productName=?");
                        pstmt.setString(1, productName);
                        ResultSet result = pstmt.executeQuery();
                        if (!result.next())
                            throw new ServletException(productName + " is not currently available for sale.");

                        basePrice = result.getFloat("price");
                        if (result.getInt("weight") > 20)
                            additionalShippingCharges += 10.0f;
                        if (result.getFloat("height") * result.getFloat("length") * result.getFloat("width") > 2500.0f)
                            additionalShippingCharges += 20.0f;
                        result.close();
                    } finally {
                        con.close();
                    }
                } catch (SQLException x) {
                    throw new ServletException(x);
                }

                float shippingCharges = baseShippingCharge.get() + additionalShippingCharges;
                float salesTax = salesTaxRate.get() * (basePrice + shippingCharges);
                float total = basePrice + shippingCharges + salesTax;

                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
                out.println("<table>");
                out.println("<tr><td><i>" + productName + "</i> base price</td><td align=right>" + currencyFormat.format(basePrice) + "</td></tr>");
                out.println("<tr><td>Shipping & Handling</td><td align=right>" + currencyFormat.format(shippingCharges) + "</td></tr>");
                out.println("<tr><td><i>" + stateName + "</i> sales tax</td><td align=right>" + currencyFormat.format(salesTax) + "</td></tr>");
                out.println("<tr><td><b>Total Price</b></td><td align=right><b>" + currencyFormat.format(total) + "</b></td></tr>");
                out.println("</table>");
                out.println("<p>Check shipping & handling costs for another product or state...</p>");
            }

            out.println("<form action=" + request.getRequestURL() + " method=GET>");
            out.println("Product: <select name=productName>");
            out.println("<option value=Unspecified selected></option>");
            out.println("<option value=Keyboard>Keyboard</option>");
            out.println("<option value=Monitor>Monitor</option>");
            out.println("<option value='Mouse Pad'>Mouse pad</option>");
            out.println("<option value='Office Chair'>Office chair</option>");
            out.println("<option value=Printer>Printer</option>");
            out.println("</select><br>");
            out.println("Ship to state: <select name=stateName>");
            out.println("<option value=Unspecified selected></option>");
            out.println("<option value=Illinois>Illinois</option>");
            out.println("<option value=Iowa>Iowa</option>");
            out.println("<option value=Minnesota>Minnesota</option>");
            out.println("<option value=Missouri>Missouri</option>");
            out.println("<option value='North Dakota'>North Dakota</option>");
            out.println("<option value='South Dakota'>South Dakota</option>");
            out.println("<option value=Wisconsin>Wisconsin</option>");
            out.println("</select><br>");
            out.println("<input type=submit value=Calculate>");
            out.println("</form>");

            out.println("<!--COMPLETED SUCCESSFULLY-->");
        } catch (Exception x) {
            throw new ServletException(x);
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        // pre-populate the database tables
        try {
            Connection con = dataSource.getConnection();
            try {
                Statement stmt = con.createStatement();

                stmt.executeUpdate("create table Products (productName varchar(80) not null primary key, price decimal(10,2), height decimal(5,2), length decimal(5,2), width decimal(5,2), weight integer)");
                PreparedStatement pstmt = con.prepareStatement("insert into Products values (?, ?, ?, ?, ?, ?)");
                pstmt.setString(1, "Keyboard");
                pstmt.setFloat(2, 39.99f);
                pstmt.setFloat(3, 1.5f);
                pstmt.setFloat(4, 8.0f);
                pstmt.setFloat(5, 21.0f);
                pstmt.setFloat(6, 3);
                pstmt.addBatch();
                pstmt.setString(1, "Monitor");
                pstmt.setFloat(2, 118.99f);
                pstmt.setFloat(3, 24.0f);
                pstmt.setFloat(4, 12.0f);
                pstmt.setFloat(5, 19.0f);
                pstmt.setFloat(6, 36);
                pstmt.addBatch();
                pstmt.setString(1, "Mouse Pad");
                pstmt.setFloat(2, 1.99f);
                pstmt.setFloat(3, 0.5f);
                pstmt.setFloat(4, 8.0f);
                pstmt.setFloat(5, 8.0f);
                pstmt.setFloat(6, 1);
                pstmt.addBatch();
                pstmt.setString(1, "Office Chair");
                pstmt.setFloat(2, 84.99f);
                pstmt.setFloat(3, 38.0f);
                pstmt.setFloat(4, 26.0f);
                pstmt.setFloat(5, 27.0f);
                pstmt.setFloat(6, 41);
                pstmt.addBatch();
                pstmt.setString(1, "Printer");
                pstmt.setFloat(2, 49.99f);
                pstmt.setFloat(3, 10.5f);
                pstmt.setFloat(4, 13.0f);
                pstmt.setFloat(5, 16.0f);
                pstmt.setFloat(6, 10);
                pstmt.addBatch();
                pstmt.executeBatch();

                stmt.executeUpdate("create table SalesTaxRates (stateName varchar(50) not null primary key, taxRate decimal(6,5))");
                pstmt = con.prepareStatement("insert into SalesTaxRates values (?, ?)");
                pstmt.setString(1, "Illinois");
                pstmt.setFloat(2, 0.0625f);
                pstmt.addBatch();
                pstmt.setString(1, "Iowa");
                pstmt.setFloat(2, 0.06f);
                pstmt.addBatch();
                pstmt.setString(1, "Minnesota");
                pstmt.setFloat(2, 0.06875f);
                pstmt.addBatch();
                pstmt.setString(1, "Missouri");
                pstmt.setFloat(2, 0.04225f);
                pstmt.addBatch();
                pstmt.setString(1, "North Dakota");
                pstmt.setFloat(2, 0.05f);
                pstmt.addBatch();
                pstmt.setString(1, "South Dakota");
                pstmt.setFloat(2, 0.04f);
                pstmt.addBatch();
                pstmt.setString(1, "Wisconsin");
                pstmt.setFloat(2, 0.05f);
                pstmt.addBatch();
                pstmt.executeBatch();

                stmt.executeUpdate("create table BaseShippingAmounts (stateName varchar(50) not null primary key, baseAmount decimal(5,2))");
                pstmt = con.prepareStatement("insert into BaseShippingAmounts values (?, ?)");
                pstmt.setString(1, "Illinois");
                pstmt.setFloat(2, 9.00f);
                pstmt.addBatch();
                pstmt.setString(1, "Iowa");
                pstmt.setFloat(2, 8.00f);
                pstmt.addBatch();
                pstmt.setString(1, "Minnesota");
                pstmt.setFloat(2, 8.00f);
                pstmt.addBatch();
                pstmt.setString(1, "Missouri");
                pstmt.setFloat(2, 10.00f);
                pstmt.addBatch();
                pstmt.setString(1, "North Dakota");
                pstmt.setFloat(2, 12.00f);
                pstmt.addBatch();
                pstmt.setString(1, "South Dakota");
                pstmt.setFloat(2, 10.00f);
                pstmt.addBatch();
                pstmt.setString(1, "Wisconsin");
                pstmt.setFloat(2, 9.00f);
                pstmt.addBatch();
                pstmt.executeBatch();
            } finally {
                con.close();
            }
        } catch (SQLException x) {
            throw new ServletException(x);
        }
    }
}