/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.reactive.messaging30.tck;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.custom.junit.runner.Mode;
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatActions;
import componenttest.rules.repeater.RepeatTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import java.util.Arrays;
import java.util.List;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class, //ReactiveMessagingTCKLauncher is set to full mode. This will prevent a build failure from zero tests running.
                ReactiveMessagingTCKLauncher.class
})

public class FATSuite {
}
