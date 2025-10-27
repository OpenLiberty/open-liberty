/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.health.file.healthcheck.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
    AlwaysPassesTest.class,
    LongStartupHealthCheckTest.class,
    LongCheckIntervalHealthCheckTest.class
})
public class FATSuite {
}
