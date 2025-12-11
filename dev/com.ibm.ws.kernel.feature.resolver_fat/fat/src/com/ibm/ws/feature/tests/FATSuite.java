/*******************************************************************************
 * Copyright (c) 2023,2025 IBM Corporation and others.
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
package com.ibm.ws.feature.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                BaselineResolutionErrorCaseTest.class,
                ReportFeaturesUnitTest.class,
                // ReportImagesUnitTest.class, // Disabled: See issue 29079
                // FeatureDetailsUnitTest.class, // Disabled: Not getting features in WL.

                // BaselineResolutionGenerationTest.class,
                BaselineResolutionSingletonUnitTest.class,
                BaselineVersionlessSingletonUnitTest.class,
                BaselineResolutionServletUnitTest.class,
                BaselineResolutionMicroProfileUnitTest.class,

                VersionlessJavaEEToMicroProfileTest.class,
                VersionlessServletToMicroProfileTest.class,
                VersionlessCachingTest.class,
                VersionlessMessagesTest.class,
                VersionlessPlatformTest.class
})

public class FATSuite {
    // EMPTY
}