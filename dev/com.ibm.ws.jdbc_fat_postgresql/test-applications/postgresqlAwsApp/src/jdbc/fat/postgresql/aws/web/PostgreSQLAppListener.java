/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package jdbc.fat.postgresql.aws.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;

/**
 * A test context listener which attempts to use a Datasource or Driver
 */
@WebListener
public class PostgreSQLAppListener implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        System.out.println("ENTER contextDestoryed");

        System.out.println("EXIT contextDestoryed");

    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        System.out.println("ENTER contextInitialized");

        for (String jndiName : Arrays.asList("jdbc/common-ds", "jdbc/driver-ds")) {
            System.out.println("Lookup: " + jndiName);

            DataSource ds = null;
            try {
                ds = InitialContext.doLookup(jndiName);
            } catch (NamingException e) {
                throw new RuntimeException(e);
            } finally {
                System.out.println("Finished lookup " + jndiName);
            }

            System.out.println("DataSource found: " + ds.toString());

            try (Connection con = ds.getConnection(); Statement stmt = con.createStatement()) {
                stmt.executeQuery("SELECT 1");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                System.out.println("Finished connection testing " + jndiName);
            }
        }

        System.out.println("EXIT contextInitialized");
    }

}
