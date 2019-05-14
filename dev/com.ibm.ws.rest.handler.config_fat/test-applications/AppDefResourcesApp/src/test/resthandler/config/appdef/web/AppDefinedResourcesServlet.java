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
import javax.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;

@DataSourceDefinition(name = "java:module/env/jdbc/ds2",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource",
                      databaseName = "${shared.resource.dir}/data/configRHTestDB",
                      isolationLevel = Connection.TRANSACTION_READ_COMMITTED,
                      loginTimeout = 220,
                      maxPoolSize = 2,
                      properties = {
                                     "connectionTimeout=0",
                                     "containerAuthDataRef=derbyAuth1",
                                     "createDatabase=create",
                                     "onConnect=DECLARE GLOBAL TEMPORARY TABLE TEMP2 (COL1 VARCHAR(80)) ON COMMIT PRESERVE ROWS NOT LOGGED",
                                     "queryTimeout=1m22s",
                                     "reapTime=2200ms",
                                     "recoveryAuthDataRef=derbyAuth2"
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
