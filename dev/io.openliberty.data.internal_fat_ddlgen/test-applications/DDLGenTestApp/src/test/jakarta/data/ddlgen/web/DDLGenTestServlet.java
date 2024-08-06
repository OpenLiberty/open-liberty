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

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

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
                                      "CREATE TABLE dbuser.TESTPartEntity (MODVERSION INTEGER NOT NULL, NAME VARCHAR(255), PRICE FLOAT NOT NULL, IDENTIFIERVENDOR VARCHAR(255) NOT NULL, IDENTIFIERPARTNUM VARCHAR(255) NOT NULL, PRIMARY KEY (IDENTIFIERVENDOR, IDENTIFIERPARTNUM))"
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
     * Modify and query for a record entity based on its composite id using
     * BasicRepository.save and BasicRepository.findById.
     */
    @Test
    public void testSaveAndFindByEmbeddedId() {
        Part part1 = parts.save(Part.of("EI3155-T", "IBM", "First Part", 10.99f));
        Part part2 = parts.save(Part.of("EI2303-W", "IBM", "Second Part", 8.99f));
        Part part3 = parts.save(Part.of("EI2303-W", "Acme", "Third Part", 9.99f));

        int part2InitialModCount = part2.modVersion();

        part2 = parts.save(new Part(//
                        part2.id(), //
                        part2.name(), //
                        part2.price() + 0.50f, //
                        part2InitialModCount));

        // expect automatic update to version:
        assertEquals(part2InitialModCount + 1, part2.modVersion());

        part2 = parts.findById(new Part.Identifier("EI2303-W", "IBM"))
                        .orElseThrow();
        assertEquals("Second Part", part2.name());
        assertEquals(9.49f, part2.price(), 0.001f);
        assertEquals(new Part.Identifier("EI2303-W", "IBM"), part2.id());
        assertEquals(part2InitialModCount + 1, part2.modVersion());

        Part.Identifier nonmatching = new Part.Identifier("EI3155-T", "Acme");
        assertEquals(false, parts.findById(nonmatching).isPresent());

        parts.deleteAll(List.of(part1, part2, part3));
    }
}
