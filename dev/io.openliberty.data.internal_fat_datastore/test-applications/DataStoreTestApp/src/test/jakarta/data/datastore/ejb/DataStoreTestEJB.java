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
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
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

    @Inject
    EJBModuleDSDRepo dsdRepo;

    // also exists in both web modules, but with different
    // container managed auth alias user id
    @Resource(name = "java:module/env/jdbc/ServerDataSourceRef",
              lookup = "jdbc/ServerDataSource")
    DataSource serverDSResRef;

    @Inject
    EJBModuleDSResRefRepo serverDSResRefRepo;

    @Inject
    DSDRepoEJB dsdRepoEJB;

    /**
     * Populate the database before running the test.
     */
    public void setup(@Observes Startup event) {
        System.out.println("DataStoreTestEJB observed Startup");

        dsdRepo.store(EJBModuleDSDEntity.of(1, "startup has been observed"));
    }

    /**
     * Use a repository, defined in an EJB, that specifies the JNDI name of a
     * DataSourceDefinition, also defined in the EJB, which has user id ejbuser1.
     * Use a resource accessor method to obtain a connection to the data source
     * and verify the user name matches and that the connection can access the
     * data that was inserted via the repository.
     */
    public void testDataSourceDefinitionInEJBModule() {
        assertEquals(false, dsdRepo.acquire(64).isPresent());

        dsdRepo.store(EJBModuleDSDEntity.of(64, "sixty-four"));

        assertEquals(true, dsdRepo.acquire(64).isPresent());

        try (Connection con = dsdRepo.acquireConnection()) {
            assertEquals("ejbuser1",
                         con.getMetaData().getUserName().toLowerCase());

            String sql = "SELECT value FROM EJBModuleDSDEntity WHERE id=64";
            ResultSet result = con
                            .createStatement()
                            .executeQuery(sql);
            assertEquals(true, result.next());
            assertEquals("sixty-four", result.getString(1));
        } catch (SQLException x) {
            throw new EJBException(x);
        }
    }

    /**
     * Use a repository, defined in an EJB, that specifies a resource reference
     * to a data source, defined in server.xml,
     * where the resource reference has a container managed authentication alias
     * that is defined in server.xml, ResRefAuth3, with user resrefuser3.
     * Use a resource accessor method to obtain the same data source
     * and verify the user name matches what is configured in server.xml and that
     * the connection to the data source can access the data that was inserted
     * via the repository.
     */
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

    /**
     * Use a repository, defined in an EJB, that specifies the JNDI name of a
     * DataSourceDefinition, defined by a servlet in a WAR module, which has
     * user id servletuser1. Use a resource accessor method to obtain a connection
     * to the data source and verify the user name matches.
     */
    public void testDataSourceDefinitionInWARModuleFromEJB() {
        dsdRepoEJB.put(DSDEntityEJB.of(14, "fourteen"));

        try {
            assertEquals("servletuser1", dsdRepoEJB.getUser());
        } catch (SQLException ex) {
            throw new EJBException(ex);
        }
    }

    /**
     * Verify that code in an EJB module can access a Jakarta Data repository from
     * a CDI Startup event.
     */
    public void testStartupEventObserverInEJBModuleUsesRepository() {

        EJBModuleDSDEntity found = dsdRepo.acquire(1).orElseThrow();
        assertEquals(1, found.id);
        assertEquals("startup has been observed", found.value);
    }
}
