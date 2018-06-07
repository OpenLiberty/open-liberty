/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection.dsdmix.ejb;

import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static java.sql.Connection.TRANSACTION_SERIALIZABLE;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

@DataSourceDefinitions({
                         @DataSourceDefinition(name = "java:module/mix_MergeSLSBModLevelDS",
                                               className = "org.apache.derby.jdbc.EmbeddedXADataSource40",
                                               databaseName = "dsdMixTestMerge",
                                               loginTimeout = 1826,
                                               properties = { "createDatabase=create" },
                                               user = "dsdTesterMerge"),
                         @DataSourceDefinition(name = "java:module/mix_XMLOverrideSLSBModLevelDS",
                                               className = "org.apache.derby.jdbc.EmbeddedXADataSource40",
                                               databaseName = "dsdMixTestOverride",
                                               loginTimeout = 1827,
                                               isolationLevel = TRANSACTION_READ_COMMITTED,
                                               properties = { "createDatabase=create" },
                                               user = "dsdTesterAnn") })
@DataSourceDefinition(name = "java:module/mix_AnnOnlySLSBModLevelDS",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource40",
                      databaseName = "dsdMixTestAnn",
                      loginTimeout = 1829,
                      isolationLevel = TRANSACTION_SERIALIZABLE,
                      properties = { "createDatabase=create" },
                      user = "dsdTesterAnn")
public class DSDMixedBean {
    private static String CLASSNAME = DSDMixedBean.class.getName();
    private static Logger svLogger = Logger.getLogger(CLASSNAME);

    public boolean testDS(String jndi, int expectedLTO, int expectedIso, String expectedUser) throws NamingException, SQLException {
        boolean result = true;
        Connection dsCon = null;
        try {
            svLogger.info("--> Attempting to lookup the DS defined via annotations using: '" + jndi + "' ");

            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup(jndi);

            svLogger.info("--> Get connection...");
            dsCon = ds.getConnection();

            svLogger.info("--> Verify the loginTimeout value...");
            int loginTO = ds.getLoginTimeout();
            svLogger.info("--> The expected loginTimeout is: " + expectedLTO + ". The returned loginTimeout is: " + loginTO);
            if (expectedLTO != loginTO) {
                result = false;
            }

            svLogger.info("--> Verify the isolation level value...");
            int isoLevel = dsCon.getTransactionIsolation();
            svLogger.info("--> The expected isolation level is: " + expectedIso + ". The returned isolation level is: " + isoLevel);
            if (expectedIso != isoLevel) {
                result = false;
            }

            svLogger.info("--> Getting DatabaseMetaData...");
            DatabaseMetaData metaD = dsCon.getMetaData();

            svLogger.info("--> Verirfy the user name... ");
            String userName = metaD.getUserName();
            svLogger.info("--> The expected user name is: " + expectedUser + ". The returned user name is: " + userName);
            if (!(userName.equals(expectedUser))) {
                result = false;
            }

            return result;
        } finally {
            if (dsCon != null) {
                try {
                    dsCon.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    svLogger.log(Level.INFO, "--> Caught unexpected exception in the finally block", e);
                }
            }
        }
    }
}