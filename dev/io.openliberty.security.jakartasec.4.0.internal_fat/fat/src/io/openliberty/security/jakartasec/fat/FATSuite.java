/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import io.openliberty.security.jakartasec.fat.tests.InMemoryIdentityStoreTests;
import io.openliberty.security.jakartasec.fat.tests.MultipleHAMCustomTests;
import io.openliberty.security.jakartasec.fat.tests.MultipleHAMDuplicateTests;
import io.openliberty.security.jakartasec.fat.tests.MultipleHAMInbuiltQualifiersTests;
import io.openliberty.security.jakartasec.fat.tests.MultipleHAMInbuiltTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                MultipleHAMCustomTests.class,
                MultipleHAMDuplicateTests.class,
                MultipleHAMInbuiltTests.class,
                MultipleHAMInbuiltQualifiersTests.class,
                InMemoryIdentityStoreTests.class
})
public class FATSuite {
}
