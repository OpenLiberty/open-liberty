/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal_fat_tck;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.utils.tck.TCKUtilities;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                Telemetry20TCKLauncher.class,
                Telemetry20MetricsConfigTCKLauncher.class,
                Telemetry20LogsConfigTCKLauncher.class
})
@MinimumJavaLevel(javaLevel = 11)
public class FATSuite {

    public static RepeatTests allMPTel20Repeats(String serverName) {
        return MicroProfileActions
                        .repeatIf(serverName, TCKUtilities::areAllFeaturesPresent, MicroProfileActions.MP70_EE11, MicroProfileActions.MP70_EE10);
    }
}
