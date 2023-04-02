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
package com.ibm.ws.jdbc.fat;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jdbc.fat.tests.ConfigTest;
import com.ibm.ws.jdbc.fat.tests.DataSourceJaasTest;
import com.ibm.ws.jdbc.fat.tests.DataSourceTest;

import componenttest.containers.TestContainerSuite;

@RunWith(Suite.class)
@SuiteClasses({
                ConfigTest.class,
                DataSourceTest.class,
                DataSourceJaasTest.class
})
public class FATSuite extends TestContainerSuite {

    @BeforeClass
    public static void beforeSuite() throws Exception {
        //Add TestLoginModule.jar to shared.resources.dir
        JavaArchive TestLoginModule = ShrinkHelper.buildJavaArchive("TestLoginModule", "loginmodule");
        ShrinkHelper.exportArtifact(TestLoginModule, "publish/shared/resources/loginmodule/");
    }
}
