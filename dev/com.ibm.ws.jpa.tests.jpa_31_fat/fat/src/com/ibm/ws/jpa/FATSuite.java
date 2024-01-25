/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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

package com.ibm.ws.jpa;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.ws.jpa.jpa31.AsmServiceTest;
import com.ibm.ws.jpa.jpa31.JPA31Test;
import com.ibm.ws.jpa.jpa31.JPABootstrapTest;
import com.ibm.ws.jpa.jpa31.JPAJSONTest;

import componenttest.containers.TestContainerSuite;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.database.container.DatabaseContainerFactory;

@RunWith(Suite.class)
@SuiteClasses({
                JPABootstrapTest.class,
                JPA31Test.class,
                JPAJSONTest.class,
                AsmServiceTest.class,
                componenttest.custom.junit.runner.AlwaysPassesTest.class
})

public class FATSuite extends TestContainerSuite {
    public final static String[] JAXB_PERMS = { "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind.v2.runtime.reflect\";",
                                                "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind\";" };

    @ClassRule
    public static JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE10_FEATURES())
                    .andWith(FeatureReplacementAction.EE11_FEATURES());;

}
