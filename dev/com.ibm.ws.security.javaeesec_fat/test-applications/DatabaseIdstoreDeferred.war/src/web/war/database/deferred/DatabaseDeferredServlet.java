/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
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
