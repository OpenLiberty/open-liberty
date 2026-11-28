/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.pqc.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Collection of all PQC LTPA FAT tests.
 * 
 * This suite runs comprehensive tests for Post-Quantum Cryptography (PQC)
 * support in Liberty LTPA tokens, including:
 * - ML-DSA key generation and management
 * - PQC token creation and validation
 * - Hybrid mode (RSA + ML-DSA) operations
 * - Backward compatibility with classical LTPA
 */
@RunWith(Suite.class)
@SuiteClasses({
    PQCLTPATests.class
})
public class FATSuite {
    // Test suite - no implementation needed
}
