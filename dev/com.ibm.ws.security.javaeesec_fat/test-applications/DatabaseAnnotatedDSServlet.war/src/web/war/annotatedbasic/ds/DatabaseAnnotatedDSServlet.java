/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.war.annotatedbasic.ds;

import javax.annotation.sql.DataSourceDefinition;
import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;

import web.jar.base.FlexibleBaseServlet;

/*
 * Manually tested java:comp, java:app, java:global
 */
@DataSourceDefinition(
                      name = "java:module/jdbc/dsDef",
                      className = "org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource",
                      serverName = "localhost",
                      databaseName = "memory:ds1",
                      user = "dbuser1",
                      password = "dbpwd1")
@BasicAuthenticationMechanismDefinition(realmName = "JavaEESec Basic Realm")
@DatabaseIdentityStoreDefinition(
                                 callerQuery = "select password from callers where name = ?",
                                 groupsQuery = "select group_name from caller_groups where caller_name = ?",
                                 dataSourceLookup = "java:module/jdbc/dsDef")

public class DatabaseAnnotatedDSServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public DatabaseAnnotatedDSServlet() {

        super("DatabaseAnnotatedDSServlet");

        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteRolesStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WritePublicCredentialsStep());
        mySteps.add(new WriteRunAsSubjectStep());
        mySteps.add(new WriteCookiesStep());
    }

}
