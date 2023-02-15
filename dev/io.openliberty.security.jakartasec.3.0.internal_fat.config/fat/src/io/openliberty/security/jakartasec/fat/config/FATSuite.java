/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.security.jakartasec.fat.config;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import io.openliberty.security.jakartasec.fat.config.tests.ConfigurationClaimsDefinitionTests;
import io.openliberty.security.jakartasec.fat.config.tests.ConfigurationDisplayTests;
import io.openliberty.security.jakartasec.fat.config.tests.ConfigurationELValuesOverrideTests;
import io.openliberty.security.jakartasec.fat.config.tests.ConfigurationELValuesOverrideWithoutHttpSessionTests;
import io.openliberty.security.jakartasec.fat.config.tests.ConfigurationExtraParametersTests;
import io.openliberty.security.jakartasec.fat.config.tests.ConfigurationPromptTests;
import io.openliberty.security.jakartasec.fat.config.tests.ConfigurationProviderMetadataTests;
import io.openliberty.security.jakartasec.fat.config.tests.ConfigurationResponseModeTests;
import io.openliberty.security.jakartasec.fat.config.tests.ConfigurationScopeTests;
import io.openliberty.security.jakartasec.fat.config.tests.ConfigurationSigningTests;
import io.openliberty.security.jakartasec.fat.config.tests.ConfigurationTests;
import io.openliberty.security.jakartasec.fat.config.tests.ConfigurationTokenMinValidityTests;
import io.openliberty.security.jakartasec.fat.config.tests.ConfigurationUseNonceTests;
import io.openliberty.security.jakartasec.fat.config.tests.ConfigurationUserInfoTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                ConfigurationTests.class,
                ConfigurationClaimsDefinitionTests.class,
                ConfigurationScopeTests.class,
                ConfigurationResponseModeTests.class,
                ConfigurationUseNonceTests.class,
                ConfigurationExtraParametersTests.class,
                ConfigurationPromptTests.class,
                ConfigurationDisplayTests.class,
                ConfigurationTokenMinValidityTests.class,
                // LogoutDefinition tests are handled in a separate FAT project as the test use sleeps to wait for tokens to expire and that causes the tests to take quite some time to run
                ConfigurationELValuesOverrideTests.class,
                ConfigurationELValuesOverrideWithoutHttpSessionTests.class,
                ConfigurationUserInfoTests.class,
                ConfigurationSigningTests.class,
                ConfigurationProviderMetadataTests.class
})
public class FATSuite {

}
