/*******************************************************************************
 * Copyright (c) 2021,2024 IBM Corporation and others.
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

package com.ibm.ws.jpa.tests.spec10.injection.mdb;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.ws.jpa.tests.spec10.injection.common.RepeatWithJPA20;
import com.ibm.ws.jpa.tests.spec10.injection.common.RepeatWithJPA21;
import com.ibm.ws.jpa.tests.spec10.injection.common.RepeatWithJPA22;
import com.ibm.ws.jpa.tests.spec10.injection.common.RepeatWithJPA22Hibernate;
import com.ibm.ws.jpa.tests.spec10.injection.common.RepeatWithJPA22OpenJPA312;
import com.ibm.ws.jpa.tests.spec10.injection.common.RepeatWithJPA30;
import com.ibm.ws.jpa.tests.spec10.injection.common.RepeatWithJPA31;
import com.ibm.ws.jpa.tests.spec10.injection.common.RepeatWithJPA32;

import componenttest.containers.TestContainerSuite;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.database.container.DatabaseContainerFactory;

@RunWith(Suite.class)
@SuiteClasses({
                JPA10Injection_MDB.class,
                componenttest.custom.junit.runner.AlwaysPassesTest.class
})
public class FATSuite extends TestContainerSuite {
    public final static String[] JAXB_PERMS = { "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind.v2.runtime.reflect\";",
                                                "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind\";",
                                                "permission java.lang.RuntimePermission \"accessDeclaredMembers\";" };

    @ClassRule
    public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .with(new RepeatWithJPA21())
                    .andWith(new RepeatWithJPA22().fullFATOnly())
                    .andWith(new RepeatWithJPA20().fullFATOnly())
                    .andWith(new RepeatWithJPA22Hibernate().fullFATOnly())
                    .andWith(new RepeatWithJPA22OpenJPA312().fullFATOnly())
                    .andWith(new RepeatWithJPA30().fullFATOnly())
                    .andWith(new RepeatWithJPA31())
                    .andWith(new RepeatWithJPA32());

}
