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
package web.war.identitystores.ldap.ldap1;

import java.util.logging.Logger;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;

@LdapIdentityStoreDefinition(
                             url = "ldap://127.0.0.1:10389/",
                             callerBaseDn = "",
                             callerSearchBase = "ou=users,o=ibm,c=us",
                             callerSearchScope = LdapIdentityStoreDefinition.LdapSearchScope.SUBTREE,
                             callerSearchFilter = "(objectclass=person)",
                             callerNameAttribute = "uid",
                             groupNameAttribute = "cn",
                             groupSearchBase = "ou=groups,o=ibm,c=us",
                             groupSearchScope = LdapIdentityStoreDefinition.LdapSearchScope.SUBTREE,
                             groupSearchFilter = "(objectclass=groupofnames)",
                             groupMemberAttribute = "member",
                             bindDn = "uid=admin,ou=users,o=ibm,c=us",
                             bindDnPassword = "s3cur1ty",
                             priority = 100
                             )
public class LdapIpAddressAnnotate100 {
    private static Logger log = Logger.getLogger(LdapIpAddressAnnotate100.class.getName());
    public LdapIpAddressAnnotate100() {
        log.info("<ctor>");
    }
}
