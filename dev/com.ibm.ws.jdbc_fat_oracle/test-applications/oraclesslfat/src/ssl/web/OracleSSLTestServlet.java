/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ssl.web;

import java.security.Provider;
import java.security.Security;
import java.sql.Connection;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.rules.SkipJavaSemeruWithFipsEnabled.SkipJavaSemeruWithFipsEnabledRule;
import oracle.security.pki.OraclePKIProvider;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/OracleSSLTestServlet")
public class OracleSSLTestServlet extends FATServlet {

    @Resource
    private DataSource ds;

    @Resource(lookup = "jdbc/oracleWalletSSO")
    private DataSource ds_oracle_wallet_sso;

    @Resource(lookup = "jdbc/oracleWalletP12")
    private DataSource ds_oracle_wallet_p12;

    @Resource(lookup = "jdbc/oracleWalletJKS")
    private DataSource ds_oracle_wallet_jks;

    @Override
    public void init() throws ServletException {
        Security.insertProviderAt(new OraclePKIProvider(), 1);
        System.out.println("init method: OraclePKIProvider has been successfully instantiated");

        int i = 0;
        for (Provider p : Security.getProviders()) {
            System.out.println("provider." + ++i + ": " + p.getName());
        }
    }

    @Test
    public void testSimpleConnection() throws Exception {
        try (Connection con = ds.getConnection(); Statement stmt = con.createStatement()) {
            stmt.execute("SELECT 1 FROM DUAL");
        }
    }

    @Test
    @SkipJavaSemeruWithFipsEnabledRule
    public void testOracleWalletSSO() throws Exception {
        try (Connection con = ds_oracle_wallet_sso.getConnection(); Statement stmt = con.createStatement()) {
            stmt.execute("SELECT 1 FROM DUAL");
        }
    }

    @Test
    @SkipJavaSemeruWithFipsEnabledRule
    public void testOracleWalletP12() throws Exception {
        try (Connection con = ds_oracle_wallet_p12.getConnection(); Statement stmt = con.createStatement()) {
            stmt.execute("SELECT 1 FROM DUAL");
        }
    }

    @Test
    public void testOracleJKS() throws Exception {
        try (Connection con = ds_oracle_wallet_jks.getConnection(); Statement stmt = con.createStatement()) {
            stmt.execute("SELECT 1 FROM DUAL");
        }
    }
}
