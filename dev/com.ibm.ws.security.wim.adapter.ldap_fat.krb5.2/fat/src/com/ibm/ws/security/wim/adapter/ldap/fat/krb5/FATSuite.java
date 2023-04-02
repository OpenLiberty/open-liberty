/*******************************************************************************
 * Copyright (c) 2021,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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
                TicketCacheBindTest.class,
                KeytabBadPrincipalNameTest.class,
                TicketCacheBadPrincipalNameTest.class,
                TicketCacheBindExpireTests.class,
                RealmNameJVMProp.class,
                TicketCacheBadPrincipalJava8.class,
                /*
                 * Do not add more tests to this suite or the FULL fat tends to time out on Window runs.
                 */
                /*
                 * vvv Leave Krb5ConfigJVMProp as the last test, the JVM prop changes the rest of the tests
                 */
                Krb5ConfigJVMProp.class
                /*
                 * ^^^ Leave Krb5ConfigJVMProp as the last test, the JVM prop changes the rest of the tests
                 */
                /*
                 * Do not add more tests to this suite or the FULL fat tends to time out on Window runs.
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
