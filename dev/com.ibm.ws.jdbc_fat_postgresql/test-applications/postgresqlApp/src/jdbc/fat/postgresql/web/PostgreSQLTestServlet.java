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
package jdbc.fat.postgresql.web;

import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/PostgreSQLTestServlet")
public class PostgreSQLTestServlet extends FATServlet {

    @Test
    public void testPostgresGenericProps() throws Exception {
        DataSource ds = InitialContext.doLookup("jdbc/postgres/genericprops");
        ds.getConnection().close();
    }

    @Test
    public void testAnonymousPostgresDriver() throws Exception {
        DataSource ds = InitialContext.doLookup("jdbc/anonymous/Driver");
        ds.getConnection().close();
    }

    @Test
    public void testAnonymousPostgresDS() throws Exception {
        DataSource ds = InitialContext.doLookup("jdbc/anonymous/XADataSource");
        ds.getConnection().close();
    }

    @Test
    public void testPostgresMinimal() throws Exception {
        DataSource ds = InitialContext.doLookup("jdbc/postgres/minimal");
        ds.getConnection().close();
    }

}
