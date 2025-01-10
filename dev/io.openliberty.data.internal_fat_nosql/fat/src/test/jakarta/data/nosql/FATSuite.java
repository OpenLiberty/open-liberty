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
package test.jakarta.data.nosql;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                DataContainerNoSQLTest.class,
                DataNoSQLTest.class
})
public class FATSuite extends TestContainerSuite {

    private static final DockerImageName mongoDBImage = DockerImageName.parse("public.ecr.aws/docker/library/mongo:6.0.6")
                    .asCompatibleSubstituteFor("mongo:6.0.6");

    @ClassRule
    public static MongoDBContainer mongoDBContainer = new MongoDBContainer(mongoDBImage);

    /**
     * Pre-bucket execution setup.
     */
    @BeforeClass
    public static void beforeSuite() throws Exception {
    }

    /**
     * Post-bucket execution setup.
     */
    @AfterClass
    public static void cleanUpSuite() throws Exception {
    }
}
