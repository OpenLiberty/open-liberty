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
package io.openliberty.security.jakartasec.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import io.openliberty.security.jakartasec.fat.tests.AuthenticationTests;
import io.openliberty.security.jakartasec.fat.tests.BasicOIDCAnnotationUseCallbacksTests;
import io.openliberty.security.jakartasec.fat.tests.BasicOIDCAnnotationUseRedirectToOriginalResourceTests;
import io.openliberty.security.jakartasec.fat.tests.BasicOIDCAnnotationWithOidcClientConfigTests;
import io.openliberty.security.jakartasec.fat.tests.IdentityStoreTests;
import io.openliberty.security.jakartasec.fat.tests.InjectionScopedTests;
import io.openliberty.security.jakartasec.fat.tests.TokenValidationTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                BasicOIDCAnnotationUseCallbacksTests.class,
                BasicOIDCAnnotationUseRedirectToOriginalResourceTests.class,
                BasicOIDCAnnotationWithOidcClientConfigTests.class,
                InjectionScopedTests.class,
                IdentityStoreTests.class,
                TokenValidationTests.class,
                AuthenticationTests.class
})
public class FATSuite {

}
