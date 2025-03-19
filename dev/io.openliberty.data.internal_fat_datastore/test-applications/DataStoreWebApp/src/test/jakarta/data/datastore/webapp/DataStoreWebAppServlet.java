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
package test.jakarta.data.datastore.webapp;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;
import test.jakarta.data.datastore.global.lib.GlobalLibEntity;
import test.jakarta.data.datastore.global.lib.GlobalLibRepo;

@SuppressWarnings("serial")
@WebServlet("/webapp/*")
public class DataStoreWebAppServlet extends FATServlet {
    @Inject
    GlobalLibRepo globalLibRepo;

    @Resource(name = "java:global/env/jdbc/ServerDataSourceRef",
              lookup = "jdbc/ServerDataSource")
    DataSource serverDSResRef;

    /**
     * Use a repository that is defined in a global library and is
     * configured with a dataStore that is a java:global scoped
     * data source resource reference, which has a container managed
     * authentication alias with user id resrefuser4. Use the repository
     * to insert and query for data. Use a resource accessor method of the
     * repository to obtain a connection and verify that the user id
     * matches the auth alias of the resource reference.
     */
    @Test
    public void testGlobalLibRepository() throws SQLException {

        assertEquals(false, globalLibRepo.request(122L).isPresent());

        GlobalLibEntity e122 = GlobalLibEntity.of(122L, "one hundred twenty-two");
        globalLibRepo.modifyOrAdd(e122);

        assertEquals(true, globalLibRepo.request(122L).isPresent());

        try (Connection con = globalLibRepo.requestConnection()) {
            assertEquals("resrefuser4",
                         con.getMetaData().getUserName().toLowerCase());

            String sql = "SELECT value FROM GlobalLibEntity WHERE id = 122";
            ResultSet result = con
                            .createStatement()
                            .executeQuery(sql);
            assertEquals(true, result.next());
            assertEquals("one hundred twenty-two", result.getString(1));
        }

    }
}
