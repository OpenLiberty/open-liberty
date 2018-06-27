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
package com.ibm.ws.injection.dsdann.ejb;

import java.sql.SQLException;
import java.util.logging.Logger;

import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;
import javax.ejb.Stateful;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

@DataSourceDefinitions({
                         @DataSourceDefinition(name = "java:module/ann_BasicModLevelDS",
                                               className = "org.apache.derby.jdbc.EmbeddedXADataSource40",
                                               databaseName = "dsdAnnTest",
                                               loginTimeout = 1814,
                                               properties = { "createDatabase=create" }),
                         @DataSourceDefinition(name = "java:app/ann_BasicAppLevelDS",
                                               className = "org.apache.derby.jdbc.EmbeddedXADataSource40",
                                               databaseName = "dsdAnnTest",
                                               loginTimeout = 1819,
                                               properties = { "createDatabase=create" }),
                         @DataSourceDefinition(name = "java:global/ann_BasicGlobalLevelDS", // change to java:global per RTC86337
                                               className = "org.apache.derby.jdbc.EmbeddedXADataSource40",
                                               databaseName = "dsdAnnTest",
                                               loginTimeout = 1806,
                                               properties = { "createDatabase=create" }),
                         @DataSourceDefinition(name = "ann_BasicCompLevelDS",
                                               className = "org.apache.derby.jdbc.EmbeddedXADataSource40",
                                               databaseName = "dsdAnnTest",
                                               loginTimeout = 1815,
                                               properties = { "createDatabase=create" })
})
@Stateful
public class DSDStatefulBean {
    private static String CLASSNAME = DSDStatefulBean.class.getName();
    private static Logger svLogger = Logger.getLogger(CLASSNAME);

    public int testDS(String jndi) throws NamingException, SQLException {
        svLogger.info("--> Attempting to lookup the DS defined via annotations using: '" + jndi + "' ");

        InitialContext ctx = new InitialContext();

        DataSource ds = (DataSource) ctx.lookup(jndi);
        svLogger.info("--> Successfully looked up the DS using: '" + jndi + "' ");

        svLogger.info("--> Access the DS to verify it is the one we want...");
        int loginTO = ds.getLoginTimeout();
        svLogger.info("--> The DS login timeout returned is: " + loginTO);
        return loginTO;
    }
}