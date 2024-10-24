/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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
package com.ibm.ws.install.featureUtility.fat;

import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.containers.TestContainerSuite;

@RunWith(Suite.class)
@SuiteClasses({

	InstallFeatureTest.class, InstallServerTest.class, HelpActionTest.class, InstallVersionlessServerTest.class, FindActionTest.class

})
public class FATSuite extends TestContainerSuite {

    /**
     * Start of FAT suite processing.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void beforeSuite() throws Exception {
	FeatureUtilityToolTest.setupEnv();
	FeatureUtilityToolTest.constructLocalMavenRepo(Paths.get("publish/repo/").toAbsolutePath().toString(),Paths.get("publish/repo/userFeature/userFeature-1.0.zip"));
	FeatureUtilityToolTest.constructLocalMavenRepo(Paths.get("publish/repo/").toAbsolutePath().toString(),Paths.get("publish/repo/archive1/Archive-1.0.zip"));
	FeatureUtilityToolTest.constructLocalMavenRepo(Paths.get("publish/repo2/").toAbsolutePath().toString(),Paths.get("publish/repo/archive2/Archive-2.0.zip")); //New repo has versionless features
    }

    @AfterClass
    public static void afterSuite() throws Exception {
	FeatureUtilityToolTest.cleanUpTempFiles();
	FeatureUtilityToolTest.deleteRepo("afterSuite");
    }

}
