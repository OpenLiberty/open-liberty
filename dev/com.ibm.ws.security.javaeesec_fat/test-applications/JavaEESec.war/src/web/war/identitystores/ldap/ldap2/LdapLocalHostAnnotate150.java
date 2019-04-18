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
package web.war.identitystores.ldap.ldap2;

import java.util.logging.Logger;

import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;

@LdapIdentityStoreDefinition(
                             url = "${ldapIDStorePropsBean.hostnameUrl}",
                             callerBaseDn = "",
                             callerSearchBase = "ou=anotherusers,o=ibm,c=us",
                             callerSearchScope = LdapIdentityStoreDefinition.LdapSearchScope.SUBTREE,
                             callerSearchFilter = "(&(objectclass=person)(uid=%s))",
                             callerNameAttribute = "uid",
                             groupNameAttribute = "cn",
                             groupSearchBase = "ou=anothergroups,o=ibm,c=us",
                             groupSearchScope = LdapIdentityStoreDefinition.LdapSearchScope.SUBTREE,
                             groupSearchFilter = "(objectclass=groupofnames)",
                             groupMemberAttribute = "member",
                             bindDn = "uid=admin,ou=anotherusers,o=ibm,c=us",
                             bindDnPassword = "an0thers3cur1ty",
                             priority = 150)
public class LdapLocalHostAnnotate150 {
    private static Logger log = Logger.getLogger(LdapLocalHostAnnotate150.class.getName());

    public LdapLocalHostAnnotate150() {
        log.info("<ctor>");
    }
}
