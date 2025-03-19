/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.suite;

import com.ibm.ws.microprofile.reactive.messaging.kafka.secure.fat.loginModuleClassloading.LoginModuleClassloadingTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.containers.TestContainerSuite;


@RunWith(Suite.class)
@SuiteClasses({
                SaslPlainTests.class,
                LoginModuleClassloadingTest.class,
                TlsTests.class,
                MtlsTests.class,
                MtlsMultipleKeyStoresTests.class,
})

public class FATSuite extends TestContainerSuite {

}
