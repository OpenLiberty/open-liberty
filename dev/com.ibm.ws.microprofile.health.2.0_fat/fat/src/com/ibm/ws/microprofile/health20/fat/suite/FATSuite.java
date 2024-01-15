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
package com.ibm.ws.microprofile.health20.fat.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.microprofile.health20.fat.ApplicationStateHealthCheckTest;
import com.ibm.ws.microprofile.health20.fat.ConfigAdminHealthCheckTest;
import com.ibm.ws.microprofile.health20.fat.DelayAppStartupHealthCheckTest;
import com.ibm.ws.microprofile.health20.fat.DifferentApplicationNameHealthCheckTest;
import com.ibm.ws.microprofile.health20.fat.MultipleHealthCheckTest;

import componenttest.containers.TestContainerSuite;

@RunWith(Suite.class)
@SuiteClasses({
                ApplicationStateHealthCheckTest.class,
                DelayAppStartupHealthCheckTest.class,
                MultipleHealthCheckTest.class,
                DifferentApplicationNameHealthCheckTest.class,
                ConfigAdminHealthCheckTest.class
})

public class FATSuite extends TestContainerSuite {
}
