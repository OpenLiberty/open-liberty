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

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/PostgreSQLTestServlet")
public class PostgreSQLTestServlet extends FATServlet {

    @Resource(lookup = "jdbc/postgres")
    DataSource postgresDS;

    @Test
    public void basicTest() throws Exception {
        System.out.println("Test is running in an HttpServlet");
    }

    @Test
    public void testGetConnection() throws Exception {
        postgresDS.getConnection().close();
    }

}
