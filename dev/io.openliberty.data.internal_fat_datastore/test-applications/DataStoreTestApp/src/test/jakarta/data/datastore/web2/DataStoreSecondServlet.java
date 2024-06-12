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
package test.jakarta.data.datastore.web2;

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
import test.jakarta.data.datastore.lib.DSDEntity;
import test.jakarta.data.datastore.lib.DSDRepo;
import test.jakarta.data.datastore.lib.ServerDSEntity;

@SuppressWarnings("serial")
@WebServlet("/second/*")
public class DataStoreSecondServlet extends FATServlet {

    @Inject
    DSDRepo dsdRepo;

    // also exists in other web module, but with different
    // container managed auth alias user id
    @Resource(name = "java:module/env/jdbc/ServerDataSourceRef",
              lookup = "jdbc/ServerDataSource")
    DataSource serverDSResRef;

    @Inject
    WebModule2DSResRefRepo serverDSResRefRepo;

    /**
     * Use a repository defined in a library of the application that uses
     * a java:app scoped DataSourceDefinition that is defined in the other
     * web module.
     */
    @Test
    public void testDataSourceDefinition2() throws SQLException {
        dsdRepo.put(DSDEntity.of(16, "sixteen"));

        assertEquals("servletuser1", dsdRepo.getUser());
    }

    /**
     * Use a repository that specifies a resource reference to a data source,
     * where the resource reference has a container managed authentication alias
     * that is defined in server.xml, ResRefAuth2, with user resrefuser2.
     * Use a resource accessor method to obtain the same data source
     * and verify the user name matches what is configured in server.xml and that
     * the connection to the data source can access the data that was inserted
     * via the repository.
     */
    @Test
    public void testServerDataSourceByResRefInWebModule2() throws SQLException {
        ServerDSEntity ninety_two = ServerDSEntity.of("ninety-two", 92);

        assertEquals(false, serverDSResRefRepo.read("ninety-two").isPresent());

        ninety_two = serverDSResRefRepo.write(ninety_two);

        DataSource ds = serverDSResRefRepo.getDataStore();
        try (Connection con = ds.getConnection()) {
            assertEquals("resrefuser2",
                         con.getMetaData().getUserName().toLowerCase());

            String sql = "SELECT value FROM ServerDSEntity WHERE id='ninety-two'";
            ResultSet result = con
                            .createStatement()
                            .executeQuery(sql);
            assertEquals(true, result.next());
            assertEquals(92, result.getInt(1));
        }

        ninety_two = serverDSResRefRepo.read("ninety-two").orElseThrow();
        assertEquals(92, ninety_two.value);
        assertEquals("ninety-two", ninety_two.id);
    }
}
