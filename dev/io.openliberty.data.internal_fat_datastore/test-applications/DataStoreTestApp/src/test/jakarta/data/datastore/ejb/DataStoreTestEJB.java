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
package test.jakarta.data.datastore.ejb;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import javax.sql.DataSource;

import test.jakarta.data.datastore.lib.ServerDSEntity;

@DataSourceDefinition(name = "java:module/jdbc/DataSourceDef",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource",
                      databaseName = "memory:testdb",
                      user = "ejbuser1",
                      password = "ejbpwd1",
                      properties = "createDatabase=create")
@Stateless
public class DataStoreTestEJB {

    // also exists in both web modules, but with different
    // container managed auth alias user id
    @Resource(// TODO name = "java:module/env/jdbc/ServerDataSourceRef",
              name = "java:app/env/jdbc/ServerDataSourceRef", // TODO replace with above
              lookup = "jdbc/ServerDataSource")
    DataSource serverDSResRef;

    @Inject
    EJBModuleDSResRefRepo serverDSResRefRepo;

    public void testServerDataSourceByResRefInEJBModule() {
        ServerDSEntity ninety_seven = ServerDSEntity.of("ninety-seven", 97);

        assertEquals(false, serverDSResRefRepo.fetch("ninety-seven").isPresent());

        ninety_seven = serverDSResRefRepo.addItem(ninety_seven);

        DataSource ds = serverDSResRefRepo.obtainDataSource();
        try (Connection con = ds.getConnection()) {
            assertEquals("resrefuser3",
                         con.getMetaData().getUserName().toLowerCase());

            String sql = "SELECT value FROM ServerDSEntity WHERE id='ninety-seven'";
            ResultSet result = con
                            .createStatement()
                            .executeQuery(sql);
            assertEquals(true, result.next());
            assertEquals(97, result.getInt(1));
        } catch (SQLException x) {
            throw new EJBException(x);
        }

        ninety_seven = serverDSResRefRepo.fetch("ninety-seven").orElseThrow();
        assertEquals(97, ninety_seven.value);
        assertEquals("ninety-seven", ninety_seven.id);
    }
}
