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
                //  KeytabBind_UserLoginTest.class, // to-do: kristip, in-progress
                SimpleBindTest.class,
                TicketCacheBindTest.class,
                TicketCacheBindLongRunTest.class,
                TicketCacheBindMultiRegistryTest.class
})
public class FATSuite extends ApacheDSandKDC {
    /*
     * The ApacheDS Directory Service, Ldap and KDC are started globally in ApacheDSandKDC (beforeClass and afterClass).
     *
     * ApacheDS trace will appear in output.txt. To enable more ApacheDS trace, set the setup method in ApacheDSandKDC.
     *
     */

}
