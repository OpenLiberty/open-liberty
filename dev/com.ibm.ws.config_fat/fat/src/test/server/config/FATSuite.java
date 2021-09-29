/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

}
