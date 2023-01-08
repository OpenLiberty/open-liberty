/*******************************************************************************
 * Copyright (c) 2012,2022 IBM Corporation and others.
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
package test.server.config;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                ServerConfigTest.class,
                CacheIdRestartTest.class,
                CacheIdUpdateTest.class,
                CacheIdRearrangeTest.class,
                ConfigExtensionsTest.class,
                ChildAliasTest.class,
                ProductExtensionsTest.class,
                BadConfigTests.class,
                MergedConfigTests.class,
                WSConfigurationHelperTest.class,
                //SchemaGeneratorMBeanTest.class,
                FeaturelistGeneratorMBeanTest.class,
                ServerXMLConfigurationMBeanTest.class,
                DropinsTest.class,
})
public class FATSuite {
    // EMPTY
}
