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
package web.war.database.immediate;

import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;

import web.jar.base.FlexibleBaseServlet;

/**
 * This servlet is configured to database identity store settings to be
 * configured by immediate EL expressions.
 */
@BasicAuthenticationMechanismDefinition(realmName = "JavaEESec Basic Realm")
@DatabaseIdentityStoreDefinition(callerQuery = "${databaseSettingsBean.getCallerQuery()}",
                                 groupsQuery = "${databaseSettingsBean.getGroupsQuery()}",
                                 dataSourceLookup = "${databaseSettingsBean.getDataSourceLookup()}")
public class DatabaseImmediateServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public DatabaseImmediateServlet() {
        super("DatabaseImmediateServlet");

        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteRolesStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WritePublicCredentialsStep());
        mySteps.add(new WriteRunAsSubjectStep());
        mySteps.add(new WriteCookiesStep());
    }
}
