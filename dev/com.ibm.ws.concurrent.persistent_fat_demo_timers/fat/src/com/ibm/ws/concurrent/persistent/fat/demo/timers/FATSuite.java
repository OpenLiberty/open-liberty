/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.concurrent.persistent.fat.demo.timers;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.JdbcDatabaseContainer;

import componenttest.containers.TestContainerSuite;
import componenttest.topology.database.container.DatabaseContainerFactory;

@RunWith(Suite.class)
@SuiteClasses(DemoTimerTest.class)
public class FATSuite extends TestContainerSuite {

    @ClassRule
    public static JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

}