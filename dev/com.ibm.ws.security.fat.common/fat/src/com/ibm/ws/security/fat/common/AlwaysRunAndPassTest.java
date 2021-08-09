/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common;

import org.junit.Test;

/**
 * Test case created to make sure that at least 1 test in the suite runs
 * This test can be used in projects where the function can only be tested
 * if certain conditions apply.
 * For example: Some function is only available when tests are run with Java8 or above.
 * If the tests are run using an older version of Java, none of the test classes would
 * run and that would generate what the test framework considers a bad result (0 test run).
 * The project would be marked as failing. This test provides that positive result.
 */
public class AlwaysRunAndPassTest {
    @Test
    public void alwaysPass() throws Exception {
        System.out.println("This testcase always runs and succeeds!");
    }
}
