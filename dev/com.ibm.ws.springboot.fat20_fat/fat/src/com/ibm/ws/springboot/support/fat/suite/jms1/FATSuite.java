/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
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
package com.ibm.ws.springboot.support.fat.suite.jms1;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.springboot.support.fat.JmsWebAppTests20;

import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                JmsWebAppTests20.class
})

public class FATSuite extends TestContainerSuite {
    // EMPTY
}
