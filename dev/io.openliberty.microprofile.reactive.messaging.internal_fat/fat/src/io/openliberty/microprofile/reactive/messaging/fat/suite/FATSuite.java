/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.openliberty.microprofile.reactive.messaging.fat.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import componenttest.containers.TestContainerSuite;
import io.openliberty.microprofile.reactive.messaging.fat.validation.ValidationTests;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    KafkaTests.class,
    ValidationTests.class
})
public class FATSuite extends TestContainerSuite {
}
