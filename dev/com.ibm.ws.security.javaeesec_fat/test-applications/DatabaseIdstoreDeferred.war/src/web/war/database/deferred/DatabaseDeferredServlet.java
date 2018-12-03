/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.war.database.deferred;

import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;

import web.jar.base.FlexibleBaseServlet;

/**
 * This servlet is configured to allow all database identity store settings to be
 * configured by EL expressions.
 */
@BasicAuthenticationMechanismDefinition(realmName = "JavaEESec Basic Realm")
@DatabaseIdentityStoreDefinition(callerQuery = "#{databaseSettingsBean.getCallerQuery()}",
                                 groupsQuery = "#{databaseSettingsBean.getGroupsQuery()}",
                                 dataSourceLookup = "#{databaseSettingsBean.getDataSourceLookup()}",
                                 hashAlgorithmParameters = "#{databaseSettingsBean.getHashAlgorithmParameters()}",
                                 priorityExpression = "#{databaseSettingsBean.getPriority()}",
                                 useForExpression = "#{databaseSettingsBean.getUseFor()}")
public class DatabaseDeferredServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public DatabaseDeferredServlet() {
        super("DatabaseDeferredServlet");

        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteRolesStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WritePublicCredentialsStep());
        mySteps.add(new WriteRunAsSubjectStep());
        mySteps.add(new WriteCookiesStep());
    }
}
