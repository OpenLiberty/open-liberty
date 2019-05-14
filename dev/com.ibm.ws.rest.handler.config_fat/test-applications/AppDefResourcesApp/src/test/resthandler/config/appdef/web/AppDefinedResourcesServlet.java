/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.resthandler.config.appdef.web;

import java.sql.Connection;

import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;
import javax.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;

@DataSourceDefinitions({
                         @DataSourceDefinition(name = "java:app/env/jdbc/ds1",
                                               className = "org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource",
                                               databaseName = "memory:firstdb",
                                               properties = {
                                                              "agedTimeout=1:05:30",
                                                              "connectionSharing=MatchCurrentState",
                                                              "createDatabase=create"
                                               }),
                         @DataSourceDefinition(name = "java:module/env/jdbc/ds2",
                                               className = "org.apache.derby.jdbc.EmbeddedXADataSource",
                                               databaseName = "${shared.resource.dir}/data/configRHTestDB",
                                               isolationLevel = Connection.TRANSACTION_READ_COMMITTED,
                                               loginTimeout = 220,
                                               maxPoolSize = 2,
                                               maxStatements = 45,
                                               properties = {
                                                              "connectionTimeout=0",
                                                              "containerAuthDataRef=derbyAuth1",
                                                              "createDatabase=create",
                                                              "onConnect=DECLARE GLOBAL TEMPORARY TABLE TEMP2 (COL1 VARCHAR(80)) ON COMMIT PRESERVE ROWS NOT LOGGED",
                                                              "queryTimeout=1m22s",
                                                              "reapTime=2200s",
                                                              "recoveryAuthDataRef=derbyAuth2",
                                                              "syncQueryTimeoutWithTransactionTimeout=true"
                                               }),
                         @DataSourceDefinition(name = "java:comp/env/jdbc/ds3",
                                               className = "org.apache.derby.jdbc.EmbeddedDataSource",
                                               databaseName = "memory:thirddb;create=true")
})
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/AppDefinedResourcesServlet")
public class AppDefinedResourcesServlet extends FATServlet {
    /**
     * No-op servlet method that the test case uses to ensure the web module is loaded.
     */
    public void doSomething() {
        System.out.println("Servlet is running.");
    }
}
