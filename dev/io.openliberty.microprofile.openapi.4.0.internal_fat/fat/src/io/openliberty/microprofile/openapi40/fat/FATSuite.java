/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.openliberty.microprofile.openapi40.fat.validation.ValidationTestCrossVersion;
import io.openliberty.microprofile.openapi40.fat.validation.ValidationTestFive;
import io.openliberty.microprofile.openapi40.fat.validation.ValidationTestFour;
import io.openliberty.microprofile.openapi40.fat.validation.ValidationTestMissing;
import io.openliberty.microprofile.openapi40.fat.validation.ValidationTestNoErrors;
import io.openliberty.microprofile.openapi40.fat.validation.ValidationTestOne;
import io.openliberty.microprofile.openapi40.fat.validation.ValidationTestTwo;
import io.openliberty.microprofile.openapi40.fat.versionwarning.VersionWarningTest;

@SuiteClasses({
                ValidationTestOne.class,
                ValidationTestTwo.class,
                ValidationTestMissing.class,
                ValidationTestFour.class,
                ValidationTestFive.class,
                ValidationTestNoErrors.class,
                ValidationTestCrossVersion.class,
                VersionWarningTest.class,
})
@RunWith(Suite.class)
public class FATSuite {}
