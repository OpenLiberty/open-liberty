/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import com.ibm.ws.jpa.eclipselink.TestOLGH16772_EJB;
import com.ibm.ws.jpa.eclipselink.TestOLGH16772_Web;

import componenttest.containers.ExternalTestServiceDockerClientStrategy;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.database.container.DatabaseContainerFactory;

@RunWith(Suite.class)
@SuiteClasses({
//                TestExample_EJB.class,
//                TestExample_Web.class,
//                TestOLGH8014_EJB.class,
//                TestOLGH8014_Web.class,
//                TestOLGH8294_EJB.class,
//                TestOLGH8294_Web.class,
//                TestOLGH8461_EJB.class,
//                TestOLGH8461_Web.class,
//                TestOLGH8950_EJB.class,
//                TestOLGH8950_Web.class,
//                TestOLGH9018_EJB.class,
//                TestOLGH9018_Web.class,
//                TestOLGH9035_EJB.class,
//                TestOLGH9035_Web.class,
//                TestOLGH10068_EJB.class,
//                TestOLGH10068_Web.class,
//                TestOLGH14426_EJB.class,
//                TestOLGH14426_Web.class,
//                TestOLGH14457_EJB.class,
//                TestOLGH14457_Web.class,
//                TestOLGH16588_EJB.class,
//                TestOLGH16588_Web.class,
//                TestOLGH16685_EJB.class,
//                TestOLGH16685_Web.class,
                TestOLGH16772_EJB.class,
                TestOLGH16772_Web.class,
                componenttest.custom.junit.runner.AlwaysPassesTest.class
})
public class FATSuite {

    //Required to ensure we calculate the correct strategy each run even when
    //switching between local and remote docker hosts.
    static {
        ExternalTestServiceDockerClientStrategy.setupTestcontainers();
    }

    public final static String[] JAXB_PERMS = { "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind.v2.runtime.reflect\";",
                                                "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind\";" };

    @ClassRule
    public static JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE7_FEATURES())
                    .andWith(FeatureReplacementAction.EE9_FEATURES());
}
