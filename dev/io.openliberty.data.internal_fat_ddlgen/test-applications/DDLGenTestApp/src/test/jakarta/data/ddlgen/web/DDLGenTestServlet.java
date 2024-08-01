/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package test.jakarta.data.ddlgen.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class DDLGenTestServlet extends FATServlet {

    @Resource(name = "java:app/env/adminDataSourceRef",
              lookup = "jdbc/TestDataSource")
    DataSource adminDataSource;

    @Inject
    Parts parts;

    /**
     * Executes the DDL in the database as a database admin.
     * This method is intentionally not annotated with @Test.
     * The test bucket must arrange for it to run before other test
     * as part of setup.
     */
    public void executeDDL() throws SQLException {
        // TODO read from ddlgen output file
        String[] ddl = new String[] {
                                      "CREATE TABLE dbuser.TESTPartEntity (ID INTEGER NOT NULL, NAME VARCHAR(255), PRICE FLOAT NOT NULL, PRIMARY KEY (ID))"
        };
        try (Connection con = adminDataSource.getConnection()) {
            Statement stmt = con.createStatement();
            for (String s : ddl) {
                System.out.println("DDL: " + s);
                stmt.execute(s);
            }
        }
    }

    /**
     * TODO write a more useful test
     */
    @Test
    public void testUseRepository() {
        Part part = parts.save(new Part(1, "one", 10.99f));
        parts.delete(part);
    }
}
