/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.ldap.fat.krb5;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                /*
                 * Currently all tests are > Java8 as ApacheDS fails to authenticate the Kerberos token on Java8
                 */
                Krb5ConfigTest.class,
                KeytabBindTest.class,
                KeytabBindLongRunTest.class,
                KeytabBindMultiRegistryTest.class,
                SimpleBindTest.class,
                TicketCacheBindTest.class,
                TicketCacheBindLongRunTest.class,
                TicketCacheBindMultiRegistryTest.class,
                KeytabBadPrincipalNameTest.class,
                TicketCacheBadPrincipalNameTest.class,
                TicketCacheBindExpireTests.class,
                RealmNameJVMProp.class,
                TicketCacheBadPrincipalJava8.class,
                /*
                 * vvv Leave Krb5ConfigJVMProp as the last test, the JVM prop changes the rest of the tests
                 */
                Krb5ConfigJVMProp.class
                /*
                 * ^^^ Leave Krb5ConfigJVMProp as the last test, the JVM prop changes the rest of the tests
                 */

})
public class FATSuite extends LdapApacheDSandKDC {
    /*
     * The ApacheDS Directory Service, Ldap and KDC are started globally in ApacheDSandKDC (beforeClass and afterClass).
     *
     * ApacheDS trace will appear in output.txt. To enable more ApacheDS trace, see the setupService method in ApacheDSandKDC.
     *
     */

}
