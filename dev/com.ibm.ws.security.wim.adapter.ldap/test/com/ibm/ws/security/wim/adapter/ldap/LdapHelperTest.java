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
package com.ibm.ws.security.wim.adapter.ldap;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LdapHelperTest {
    @Test
    public void replaceRDN() {

        /*
         * Test argument values that immediately return the input DN.
         */
        assertEquals("Null RDN attributes types should immediately return input DN.", "uid=user,ou=users,o=ibm.com",
                     LdapHelper.replaceRDN("uid=user,ou=users,o=ibm.com", null, new String[] { "newuser" }));
        assertEquals("Null RDN attributes values should immediately return input DN.", "uid=user,ou=users,o=ibm.com",
                     LdapHelper.replaceRDN("uid=user,ou=users,o=ibm.com", new String[] { "uid" }, null));
        assertEquals("Empty RDN attributes types should immediately return input DN.", "uid=user,ou=users,o=ibm.com",
                     LdapHelper.replaceRDN("uid=user,ou=users,o=ibm.com", new String[] {}, new String[] { "newuser" }));
        assertEquals("Empty RDN attributes values should immediately return input DN.", "uid=user,ou=users,o=ibm.com",
                     LdapHelper.replaceRDN("uid=user,ou=users,o=ibm.com", new String[] { "uid" }, new String[] {}));

        /*
         * Replace a value in the RDN.
         */
        assertEquals("RDN value should be replaced.", "uid=newuser,ou=users,o=ibm.com",
                     LdapHelper.replaceRDN("uid=user,ou=users,o=ibm.com", new String[] { "uid" }, new String[] { "newuser" }));

        /*
         * Replace a single attribute RDN with a multi-attribute RDN.
         */
        assertEquals("RDN value should be replaced.", "uid=uid1+cn=cn1,ou=users,o=ibm.com",
                     LdapHelper.replaceRDN("uid=user,ou=users,o=ibm.com", new String[] { "uid", "cn" }, new String[] { "uid1", "cn1" }));

        /*
         * Replace a multi-attribute RDN with new values.
         */
        assertEquals("RDN value should be replaced.", "uid=uid2+cn=cn2,ou=users,o=ibm.com",
                     LdapHelper.replaceRDN("uid=uid1+cn=cn1,ou=users,o=ibm.com", new String[] { "uid", "cn" }, new String[] { "uid2", "cn2" }));

        /*
         * Replace a value in the RDN.
         */
        assertEquals("No values to replace should result in no change.", "uid=newuser,ou=users,o=ibm.com",
                     LdapHelper.replaceRDN("uid=user,ou=users,o=ibm.com", new String[] { "uid" }, new String[] { "newuser" }));

        /*
         * Test to ensure there is no regression in a fix to an issue where the full DN was considered the RDN value when
         * RDN values were passed in as null. The
         * errant return value looked something like this: uid=user\,ou\=users\,o\=ibm.com,ou=users,o=ibm.com
         */
        assertEquals("Expected no change when there are null values.", "uid=user,ou=users,o=ibm.com",
                     LdapHelper.replaceRDN("uid=user,ou=users,o=ibm.com", new String[] { "uid" }, new String[] { null }));
    }
}
