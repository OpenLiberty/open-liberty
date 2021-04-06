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
package ssl.web;

import java.sql.Connection;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/OracleSSLTestServlet")
public class OracleSSLTestServlet extends FATServlet {

    @Resource
    private DataSource ds;

    @Resource(lookup = "jdbc/oracleWallet")
    private DataSource ds_oracle_wallet;

//    @Resource(lookup = "jdbc/oraclejks")
//    private DataSource ds_oracle_jks;

    @Test
    public void testSimpleConnection() throws Exception {
        try (Connection con = ds.getConnection(); Statement stmt = con.createStatement()) {
            stmt.execute("SELECT 1 FROM DUAL");
        }
    }

    @Test
    public void testOracleWallet() throws Exception {
        try (Connection con = ds_oracle_wallet.getConnection(); Statement stmt = con.createStatement()) {
            stmt.execute("SELECT 1 FROM DUAL");
        }
    }

    //TODO need to resolve handshake failure
//    @Test
//    public void testOracleJKS() throws Exception {
//        try (Connection con = ds_oracle_jks.getConnection(); Statement stmt = con.createStatement()) {
//            stmt.execute("SELECT 1 FROM DUAL");
//        }
//    }
}
