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

package com.ibm.ws.jpa.tests.spec20.querylockmode;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.JdbcDatabaseContainer;

import componenttest.containers.TestContainerSuite;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.database.container.DatabaseContainerFactory;

@RunWith(Suite.class)
@SuiteClasses({
                JPA20QueryLockMode_EJB.class,
                JPA20QueryLockMode_WEB.class,
                componenttest.custom.junit.runner.AlwaysPassesTest.class
})
public class FATSuite extends TestContainerSuite {
    public final static String[] JAXB_PERMS = { "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind.v2.runtime.reflect\";",
                                                "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind\";" };

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE7_FEATURES())
                    .andWith(new RepeatWithJPA20())
                    .andWith(FeatureReplacementAction.EE9_FEATURES())
                    .andWith(FeatureReplacementAction.EE10_FEATURES());

    @ClassRule
    public static JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();
}
