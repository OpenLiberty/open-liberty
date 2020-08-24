/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.ws.jpa.spec10.TestOLGH10310_EJB;
import com.ibm.ws.jpa.spec10.TestOLGH10310_Web;
import com.ibm.ws.jpa.spec10.TestOLGH11795_EJB;
import com.ibm.ws.jpa.spec10.TestOLGH11795_Web;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.database.container.DatabaseContainerFactory;
import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;

@RunWith(Suite.class)
@SuiteClasses({
                JPA10FATSuite.class,
                TestOLGH10310_Web.class,
                TestOLGH10310_EJB.class,
                TestOLGH11795_Web.class,
                TestOLGH11795_EJB.class,
                componenttest.custom.junit.runner.AlwaysPassesTest.class
})
public class FATSuite {
    public final static String[] JAXB_PERMS = { "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind.v2.runtime.reflect\";",
                                                "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind\";",
                                                "permission java.lang.RuntimePermission \"accessDeclaredMembers\";" };

    public static final JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

    @BeforeClass
    public static void beforeSuite() throws Exception {
        //Allows local tests to switch between using a local docker client, to using a remote docker client.
        ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();

        testContainer.start();
    }

    @AfterClass
    public static void afterSuite() {
        testContainer.stop();
    }

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE7_FEATURES())
                    .andWith(new RepeatWithJPA20())
                    .andWith(FeatureReplacementAction.EE9_FEATURES());
}
