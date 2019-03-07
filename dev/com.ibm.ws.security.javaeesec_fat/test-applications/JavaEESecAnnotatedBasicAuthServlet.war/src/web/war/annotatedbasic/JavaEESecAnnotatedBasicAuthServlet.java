/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.war.annotatedbasic;

import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;

import web.jar.base.FlexibleBaseServlet;

@BasicAuthenticationMechanismDefinition(realmName = "JavaEESec Basic Realm")
@LdapIdentityStoreDefinition(
                             url = "${ldapIDStorePropsBean.hostnameUrl}",
                             callerBaseDn = "",
                             callerSearchBase = "o=ibm,c=us",
                             callerSearchScope = LdapIdentityStoreDefinition.LdapSearchScope.SUBTREE,
                             callerSearchFilter = "(&(objectclass=person)(uid=%s))",
                             callerNameAttribute = "uid",
                             groupNameAttribute = "cn",
                             groupSearchBase = "o=ibm,c=us",
                             groupSearchScope = LdapIdentityStoreDefinition.LdapSearchScope.SUBTREE,
                             groupSearchFilter = "(objectclass=groupofnames)",
                             groupMemberAttribute = "member",
                             bindDn = "uid=jaspildapuser1,o=ibm,c=us",
                             bindDnPassword = "s3cur1ty")
public class JavaEESecAnnotatedBasicAuthServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public JavaEESecAnnotatedBasicAuthServlet() {
        super("JavaEESecAnnotatedBasicAuthServlet");

        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteRolesStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WritePublicCredentialsStep());
        mySteps.add(new WriteRunAsSubjectStep());
        mySteps.add(new WriteCookiesStep());
    }
}
