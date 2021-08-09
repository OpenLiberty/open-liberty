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
package setupfat;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
public class CheckSetupTestServlet extends FATServlet {

    private static enum FATBOOTDATASOURCES {
        dsfatboot("java:comp/env/jdbc/dsfatboot"),
        dsfatboot1("java:comp/env/jdbc/dsfatboot1"),
        dsfatboot2("java:comp/env/jdbc/dsfatboot2"),
        dsfatboot3("java:comp/env/jdbc/dsfatboot3"),
        dsfatboot4("java:comp/env/jdbc/dsfatboot4"),
        dsfatboot5("java:comp/env/jdbc/dsfatboot5"),
        dsfatboot6("java:comp/env/jdbc/dsfatboot6");

        String jndiName;

        FATBOOTDATASOURCES(String jndiName) {
            this.jndiName = jndiName;
        }
    }

    /**
     * Test FAT servlet setup is working
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testServletWorking(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        out.println("CheckSetupTestServlet testServletWorking is working");
    }

    /**
     * Test FAT database is available
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testBootstrapDatabaseConnection(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        out.println("CheckSetupTestServlet testBootstrapDatabaseConnection is working");
        InitialContext context = new InitialContext();

        boolean printedOnce = false;
        for (FATBOOTDATASOURCES boot : FATBOOTDATASOURCES.values()) {
            Connection conn = ((DataSource) context.lookup(boot.jndiName)).getConnection();
            if (!printedOnce) {
                DatabaseMetaData metadata = conn.getMetaData();
                out.println("Database Name=" + metadata.getDatabaseProductName());
                out.println("Database Version=" + metadata.getDatabaseProductVersion());
                out.println("JDBC Driver Name=" + metadata.getDriverName());
                out.println("JDBC Driver Version=" + metadata.getDriverVersion());
                printedOnce = true;
            }
            conn.close();
        }
    }
}
