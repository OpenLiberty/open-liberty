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
package web.generic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class ContainersTestServlet extends FATServlet {

    @Resource(lookup = "jdbc/postgres")
    private DataSource ds_postgres;

    @Test
    public void testGenericContainer() throws Exception {
        try (Connection con = ds_postgres.getConnection(); Statement stmt = con.createStatement()) {
            stmt.execute("SELECT 1");
        }
    }

    @Test
    public void testGenericContainerStartupScript() throws Exception {
        try (Connection con = ds_postgres.getConnection(); Statement stmt = con.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT * FROM testtable WHERE PersonID=1")) {
                assertTrue(rs.next());
                assertEquals("Rochester", rs.getString("City"));
            }
        }
    }
}
