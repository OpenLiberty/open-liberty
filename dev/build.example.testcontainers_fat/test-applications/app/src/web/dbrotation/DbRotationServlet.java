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
package web.dbrotation;

import java.sql.Connection;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class DbRotationServlet extends FATServlet {

    @Resource(lookup = "jdbc/dbRotation")
    private DataSource ds_dbRotation;

    @Test
    public void testDatabaseRotation() throws Exception {
        try (Connection con = ds_dbRotation.getConnection()) {
            con.getMetaData();
        }
    }

}
