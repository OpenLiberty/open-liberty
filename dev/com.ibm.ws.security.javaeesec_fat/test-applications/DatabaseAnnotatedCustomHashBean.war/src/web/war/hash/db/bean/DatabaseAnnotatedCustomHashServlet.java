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
package web.war.hash.db.bean;

import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;

import web.jar.base.FlexibleBaseServlet;

@BasicAuthenticationMechanismDefinition(realmName = "JavaEESec Basic Realm")
@DatabaseIdentityStoreDefinition(
                                 callerQuery = "select password from callers where name = ?",
                                 groupsQuery = "select group_name from caller_groups where caller_name = ?",
                                 hashAlgorithm = com.ibm.ws.security.pwdhash.bean.test.TestHash.class)

public class DatabaseAnnotatedCustomHashServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public DatabaseAnnotatedCustomHashServlet() {

        super("DatabaseAnnotatedCustomHashServlet");

        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteRolesStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WritePublicCredentialsStep());
        mySteps.add(new WriteRunAsSubjectStep());
        mySteps.add(new WriteCookiesStep());
    }

}
