/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
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

// Needs to be refactored into normal build test suite - not as FAT's
/*
 * ReportFeaturesUnitTest.class,
 * ReportImagesUnitTest.class,
 * 
 * FeatureDetailsUnitTest.class,
 * 
 * BaselineResolutionSingletonUnitTest.class,
 * BaselineResolutionServletUnitTest.class,
 * BaselineResolutionMicroProfileUnitTest.class,
 * 
 * BaselineResolutionGenerationTest.class,
 */

        AlwaysPassTest.class
// VersionlessServletToMicroProfileTest.class,
// VersionlessJavaEEToMicroProfileTest.class,
// VersionlessEnvVarErrorTest.class
})

public class FATSuite {
    // EMPTY
}
