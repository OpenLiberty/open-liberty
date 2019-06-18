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

import java.sql.Connection;

import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/PostgreSQLSSLTestServlet")
public class PostgreSQLSSLTestServlet extends FATServlet {

    @Test
    public void testSSLBasic() throws Exception {
        DataSource ds = InitialContext.doLookup("jdbc/postgres/ssl/basic");
        try (Connection con = ds.getConnection()) {
            con.createStatement().execute("INSERT INTO people(id,name) VALUES(2,'sampledata')");
        }
    }

}
