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
                BaselineSingletonUnitTest.class,
                //BaselineServletUnitTest.class, // fix unit tests
                //MicroProfileCrossPlatformUnitTest.class, // fix unit tests

                //VersionlessEnvVarErrorTest.class, // fix error tests

                //VersionlessResolutionTest.class, // needs servlet-3.0 cases

                VersionlessServletToMicroProfileTest.class,
                VersionlessJavaEEToMicroProfileTest.class,
})
public class FATSuite {
    // EMPTY
}
