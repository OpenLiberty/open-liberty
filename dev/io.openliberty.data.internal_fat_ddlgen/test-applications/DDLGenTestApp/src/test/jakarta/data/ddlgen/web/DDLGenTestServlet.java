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

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
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
    public void executeDDL(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> files = Arrays.asList(Objects.requireNonNull(request.getParameter("scripts")).split(","));
        String withDatabaseStore = Objects.requireNonNull(request.getParameter("withDatabaseStore"));
        String usingDataSource = Objects.requireNonNull(request.getParameter("usingDataSource"));

        DataSource ds = InitialContext.doLookup(usingDataSource);

        for (String file : files) {
            if (!file.contains(withDatabaseStore))
                continue;

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                System.out.println("Execute DDL file: " + file);
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.equals("EXIT;"))
                        continue;

                    System.out.println("  Execute SQL Statement: " + line);
                    try (Connection con = ds.getConnection(); Statement stmt = con.createStatement()) {
                        stmt.execute(line);
                    }
                }
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

        int part2InitialModCount = part2.version();

        part2 = parts.save(new Part(//
                        part2.id(), //
                        part2.name(), //
                        part2.price() + 0.50f, //
                        part2InitialModCount));

        // expect automatic update to version:
        assertEquals(part2InitialModCount + 1, part2.version());

        part2 = parts.findById(new Part.Identifier("EI2303-W", "IBM"))
                        .orElseThrow();
        assertEquals("Second Part", part2.name());
        assertEquals(9.49f, part2.price(), 0.001f);
        assertEquals(new Part.Identifier("EI2303-W", "IBM"), part2.id());
        assertEquals(part2InitialModCount + 1, part2.version());

        Part.Identifier nonmatching = new Part.Identifier("EI3155-T", "Acme");
        assertEquals(false, parts.findById(nonmatching).isPresent());

        assertEquals(true, parts.findById(part1.id()).isPresent());

        parts.deleteById(part1.id());

        assertEquals(false, parts.findById(part1.id()).isPresent());

        parts.deleteAll(List.of(part2, part3));
    }
}
