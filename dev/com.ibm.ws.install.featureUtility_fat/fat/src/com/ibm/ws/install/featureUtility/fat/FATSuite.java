/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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

@RunWith(Suite.class)
@SuiteClasses({

	InstallFeatureTest.class, InstallServerTest.class, HelpActionTest.class

})
public class FATSuite {

    /**
     * Start of FAT suite processing.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void beforeSuite() throws Exception {
	FeatureUtilityToolTest.setupEnv();
	FeatureUtilityToolTest.constructLocalMavenRepo(Paths.get("publish/repo/userFeature/userFeature-1.0.zip"));
	FeatureUtilityToolTest.constructLocalMavenRepo(Paths.get("publish/repo/archive/Archive-1.0.zip"));
    }

    @AfterClass
    public static void afterSuite() throws Exception {
	FeatureUtilityToolTest.cleanUpTempFiles();
	FeatureUtilityToolTest.deleteRepo("afterSuite");
    }

}
