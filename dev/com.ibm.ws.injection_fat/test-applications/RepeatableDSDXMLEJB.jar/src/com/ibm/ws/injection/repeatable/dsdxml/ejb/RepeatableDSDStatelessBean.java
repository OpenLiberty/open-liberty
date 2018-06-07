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
package com.ibm.ws.injection.repeatable.dsdxml.ejb;

import static java.sql.Connection.TRANSACTION_SERIALIZABLE;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.sql.DataSourceDefinition;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

// NOTE: this is an XML only test and the following DS's are here
// to validate that when metadata-complete=true these annotations
// are ignored.
@DataSourceDefinition(name = "java:module/MetaDataCompleteValidDS",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource40",
                      loginTimeout = 1829,
                      isolationLevel = TRANSACTION_SERIALIZABLE)
@DataSourceDefinition(name = "java:module/AnnotationOnlyToBeIgnored",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource40",
                      databaseName = "repeatableDsdXMLTestMDCAnnOnly",
                      loginTimeout = 1886,
                      isolationLevel = TRANSACTION_SERIALIZABLE,
                      properties = { "createDatabase=create" })
public class RepeatableDSDStatelessBean {
    private static String CLASSNAME = RepeatableDSDStatelessBean.class.getName();
    private static Logger svLogger = Logger.getLogger(CLASSNAME);

    public boolean testDS(String jndi, int expectedLTO, int expectedIso) throws NamingException, SQLException {
        boolean result = true;
        Connection dsCon = null;
        try {
            svLogger.info("--> Attempting to lookup the DS defined in XML using: '" + jndi + "' ");
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

    public boolean testInvalidDS() {
        boolean result = false;
        try {
            svLogger.info("--> Attempting to lookup the invalid DS defined via annotation using: 'java:module/AnnotationOnlyToBeIgnored' ");
            InitialContext ctx = new InitialContext();
            ctx.lookup("java:module/AnnotationOnlyToBeIgnored");
        } catch (NamingException ne) {
            svLogger.log(Level.INFO, "--> Caught expected NamingException.", ne);
            svLogger.info("--> Setting result = true...");
            result = true;
        }
        return result;
    }
}