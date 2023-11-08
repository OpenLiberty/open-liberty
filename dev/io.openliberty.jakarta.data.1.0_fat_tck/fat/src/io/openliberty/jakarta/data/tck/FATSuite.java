/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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
package io.openliberty.jakarta.data.tck;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.JdbcDatabaseContainer;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.topology.database.container.DatabaseContainerFactory;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class, //Need to have a passing test for java 8 & 11
                //DataCoreTckLauncher.class,
                //DataWebTckLauncher.class,
                //DataFullTckLauncher.class //full mode
})
public class FATSuite extends TestContainerSuite {
    @ClassRule
    public static JdbcDatabaseContainer<?> jdbcContainer = DatabaseContainerFactory.create();
}
