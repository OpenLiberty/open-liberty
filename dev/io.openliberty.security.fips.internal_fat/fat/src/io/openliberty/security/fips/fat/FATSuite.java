/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.openliberty.security.fips.fat;

import io.openliberty.security.fips.fat.tests.fips1403.client.FIPS1403ClientTest;
import io.openliberty.security.fips.fat.tests.fips1403.security.utility.FIPS1403SecurityUtilityTests;
import io.openliberty.security.fips.fat.tests.fips1403.server.FIPS1403ServerTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
        AlwaysPassesTest.class,
        FIPS1403ServerTest.class,
        FIPS1403ClientTest.class,
        FIPS1403SecurityUtilityTests.class
})
public class FATSuite {
}
