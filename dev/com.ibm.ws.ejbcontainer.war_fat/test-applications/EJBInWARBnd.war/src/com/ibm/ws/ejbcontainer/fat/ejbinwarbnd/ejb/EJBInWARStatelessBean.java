/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.fat.ejbinwarbnd.ejb;

import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.annotation.sql.DataSourceDefinition;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.sql.DataSource;

import org.junit.Assert;

@Stateless
@DataSourceDefinition(name = "jdbc/dsdOverrideBinding", className = "invalid")
public class EJBInWARStatelessBean implements EJBInWARBndLocal {
    @Resource
    SessionContext context;

    @EJB(name = "ejb/statelessdef/stateless", beanName = "EJBInWARStatelessBean")
    EJBInWARBndLocal stateless;

    @Override
    public Class<?> getEJBClass() {
        return getClass();
    }

    @Resource(name = "boolAnnotationInjectionBinding")
    private boolean boolAnnotationInjectionBinding;

    @Override
    public void verifyEnvEntryBinding() {
        Assert.assertTrue("EnvEntry not retrieved from bindings.", boolAnnotationInjectionBinding);
    }

    @Resource(name = "jdbc/ejbResRefDefaultIsoDS")
    private DataSource _bindEJBDefaultDS;

    @Resource(name = "jdbc/ejbResRefEjbIsoDS")
    private DataSource _bindEjbEjbTransactionReadUncommitDS;

    @Resource(name = "jdbc/webResRefEjbIsoDS")
    private DataSource _bindWebEjbTransactionSerializableDS;

    @Resource(name = "jdbc/ejbResRefWebIsoDS")
    private DataSource _bindEjbWebTransactionReadCommitDS;

    @Resource(name = "jdbc/dupBindingDS")
    private DataSource _bindDupTranactionReadUncommitDS;

    @Override
    public void verifyResourceBinding() throws Exception {
        verifyDataSource(_bindEJBDefaultDS, Connection.TRANSACTION_REPEATABLE_READ);
    }

    @Override
    public void verifyResourceIsolationBindingMerge() throws Exception {

        verifyDataSource(_bindEjbEjbTransactionReadUncommitDS, Connection.TRANSACTION_READ_UNCOMMITTED);

        verifyDataSource(_bindWebEjbTransactionSerializableDS, Connection.TRANSACTION_SERIALIZABLE);

        verifyDataSource(_bindEjbWebTransactionReadCommitDS, Connection.TRANSACTION_READ_COMMITTED);

        verifyDataSource(_bindDupTranactionReadUncommitDS, Connection.TRANSACTION_READ_UNCOMMITTED);
    }

    public static void verifyDataSource(DataSource ds, int expectedIsolationLevel) throws SQLException {
        if (ds == null) {
            throw new IllegalStateException("DataSource is null");
        }
        // verify that a connection can be obtained from the data source
        Connection conn = ds.getConnection();
        if (conn == null) {
            throw new IllegalStateException("Failed to get connection");
        }
        try {
            // Verify that the isolation level is read from the extension file
            // the WebSphere RRA default isolation level is TRANSACTION_REPEATABLE_READ=4.
            int isolationLevel = conn.getTransactionIsolation();
            Assert.assertEquals("Isolation level should have been set.", expectedIsolationLevel, isolationLevel);

            conn.close();
        } catch (Exception e) {
            // swallow this exception
        }
    }

    @Resource(lookup = "java:comp/env/jdbc/dsdOverrideBinding")
    private DataSource _bindDataSource;

    @Override
    public void verifyDataSourceBinding() throws Exception {
        verifyDataSource(_bindDataSource, Connection.TRANSACTION_REPEATABLE_READ);
    }

}
