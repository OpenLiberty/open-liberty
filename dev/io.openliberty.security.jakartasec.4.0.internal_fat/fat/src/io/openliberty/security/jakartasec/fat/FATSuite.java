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
import io.openliberty.security.jakartasec.fat.tests.AppBndRolesTests;
import io.openliberty.security.jakartasec.fat.tests.AppRolesTests;
import io.openliberty.security.jakartasec.fat.tests.HAMWithInBuiltTests;
import io.openliberty.security.jakartasec.fat.tests.InMemoryIdStoreAesEncodedPwdTests;
import io.openliberty.security.jakartasec.fat.tests.InMemoryIdStoreBadlyEncodedPwdTests;
import io.openliberty.security.jakartasec.fat.tests.InMemoryIdentityStoreELWarningTest;
import io.openliberty.security.jakartasec.fat.tests.InMemoryIdentityStoreEnablementTests;
import io.openliberty.security.jakartasec.fat.tests.InMemoryIdentityStorePropertyNotFoundTest;
import io.openliberty.security.jakartasec.fat.tests.InMemoryIdentityStoreTests;
import io.openliberty.security.jakartasec.fat.tests.MissingCustomHandlerTests;
import io.openliberty.security.jakartasec.fat.tests.MultipleAppsInMemoryIdStoresTests;
import io.openliberty.security.jakartasec.fat.tests.MultipleHAMCustomTests;
import io.openliberty.security.jakartasec.fat.tests.MultipleHAMDuplicateTests;
import io.openliberty.security.jakartasec.fat.tests.MultipleHAMInbuiltQualifiersTests;
import io.openliberty.security.jakartasec.fat.tests.MultipleHAMInbuiltTests;
import io.openliberty.security.jakartasec.fat.tests.MultipleInMemoryIdentityStoresTests;
import io.openliberty.security.jakartasec.fat.tests.SingleHAMInbuiltCustomQualifierTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                MultipleHAMCustomTests.class,
                MultipleHAMDuplicateTests.class,
                MultipleHAMInbuiltTests.class,
                MultipleHAMInbuiltQualifiersTests.class,
                HAMWithInBuiltTests.class,
                SingleHAMInbuiltCustomQualifierTests.class,
                MissingCustomHandlerTests.class,
                InMemoryIdentityStoreTests.class,
                InMemoryIdentityStoreELWarningTest.class,
                InMemoryIdentityStorePropertyNotFoundTest.class,
                InMemoryIdentityStoreEnablementTests.class,
                InMemoryIdStoreBadlyEncodedPwdTests.class,
                InMemoryIdStoreAesEncodedPwdTests.class,
                MultipleInMemoryIdentityStoresTests.class,
                MultipleAppsInMemoryIdStoresTests.class,
                AppRolesTests.class,
                AppBndRolesTests.class
})

public class FATSuite {
}
