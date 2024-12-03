/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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
package io.openliberty.microprofile.health31.fat.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.openliberty.microprofile.health31.fat.ConfigAdminHealthCheckTest;
import io.openliberty.microprofile.health31.fat.DefaultOverallStartupStatusUpAppStartupFastTest;
import io.openliberty.microprofile.health31.fat.DefaultOverallStartupStatusUpAppStartupTest;
import io.openliberty.microprofile.health31.fat.SlowAppStartupHealthCheckFastTest;
import io.openliberty.microprofile.health31.fat.SlowAppStartupHealthCheckTest;

@RunWith(Suite.class)
@SuiteClasses({
                DefaultOverallStartupStatusUpAppStartupTest.class,
                DefaultOverallStartupStatusUpAppStartupFastTest.class,
                SlowAppStartupHealthCheckTest.class,
                SlowAppStartupHealthCheckFastTest.class,
                ConfigAdminHealthCheckTest.class
})

public class FATSuite {

}
