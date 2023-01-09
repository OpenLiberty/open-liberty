/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package web.dbrotation;

import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class DbRotationServlet extends FATServlet {

    @Resource(lookup = "jdbc/dbRotation")
    private DataSource ds_dbRotation;

    @Resource(lookup = "jdbc/dbRotationContainerAuth")
    private DataSource ds_dbRotationContAuth;

    @Resource(lookup = "jdbc/dbRotationNestedContainerAuth")
    private DataSource ds_dbRotationNestContAuth;

    @Resource(name = "jdbc/dbRotationDDAuthRef", lookup = "jdbc/dbRotationDDAuth")
    private DataSource ds_dbRotationDDAuth;

    @Resource(lookup = "jdbc/dbRotationNoAuth")
    private DataSource ds_dbRotationNoAuth;

    @Test
    public void testDatabaseRotation() throws Exception {
        try (Connection con = ds_dbRotation.getConnection()) {
            con.getMetaData();
        }
    }

    @Test
    public void testDatabaseRotationWithContainerAuth() throws Exception {
        try (Connection con = ds_dbRotationContAuth.getConnection()) {
            con.getMetaData();
        }
    }

    @Test
    public void testDatabaseRotationWithNestedContainerAuth() throws Exception {
        try (Connection con = ds_dbRotationNestContAuth.getConnection()) {
            con.getMetaData();
        }
    }

    @Test
    public void testDatabaseRotationWithDDAuth() throws Exception {
        try (Connection con = ds_dbRotationDDAuth.getConnection()) {
            con.getMetaData();
        }
    }

    @Test
    @AllowedFFDC() //Ignore all FFDCs for this test
    public void testDatabaseRotationWithNoAuth() throws Exception {
        try (Connection con = ds_dbRotationNoAuth.getConnection()) {
            con.getMetaData();
        } catch (SQLException e) {
            //No auth data was provided, therefore expect an error!
        }
    }
}
