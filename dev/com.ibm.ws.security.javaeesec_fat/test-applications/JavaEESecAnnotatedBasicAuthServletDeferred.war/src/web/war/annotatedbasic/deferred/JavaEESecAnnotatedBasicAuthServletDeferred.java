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
package web.war.annotatedbasic.deferred;

import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;

import web.jar.base.FlexibleBaseServlet;

/**
 * This servlet is configured to allow all LDAP identity store settings to be
 * configured by EL expressions.
 */
@BasicAuthenticationMechanismDefinition(realmName = "JavaEESec Basic Realm")
@LdapIdentityStoreDefinition(bindDn = "#{ldapSettingsBean.getBindDn()}",
                             bindDnPassword = "#{ldapSettingsBean.getBindDnPassword()}",
                             callerBaseDn = "#{ldapSettingsBean.getCallerBaseDn()}",
                             callerNameAttribute = "#{ldapSettingsBean.getCallerNameAttribute()}",
                             callerSearchBase = "#{ldapSettingsBean.getCallerSearchBase()}",
                             callerSearchFilter = "#{ldapSettingsBean.getCallerSearchFilter()}",
                             callerSearchScopeExpression = "#{ldapSettingsBean.getCallerSearchScope()}",
                             groupMemberAttribute = "#{ldapSettingsBean.getGroupMemberAttribute()}",
                             groupMemberOfAttribute = "#{ldapSettingsBean.getGroupMemberOfAttribute()}",
                             groupNameAttribute = "#{ldapSettingsBean.getGroupNameAttribute()}",
                             groupSearchBase = "#{ldapSettingsBean.getGroupSearchBase()}",
                             groupSearchScopeExpression = "#{ldapSettingsBean.getGroupSearchScope()}",
                             groupSearchFilter = "#{ldapSettingsBean.getGroupSearchFilter()}",
                             readTimeoutExpression = "#{ldapSettingsBean.getReadTimeout()}",
                             url = "#{ldapSettingsBean.getUrl()}",
                             useForExpression = "#{ldapSettingsBean.getUseFor()}")
public class JavaEESecAnnotatedBasicAuthServletDeferred extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public JavaEESecAnnotatedBasicAuthServletDeferred() {
        super("JavaEESecAnnotatedBasicAuthServletDeferred");

        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteRolesStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WritePublicCredentialsStep());
        mySteps.add(new WriteRunAsSubjectStep());
        mySteps.add(new WriteCookiesStep());
    }
}
