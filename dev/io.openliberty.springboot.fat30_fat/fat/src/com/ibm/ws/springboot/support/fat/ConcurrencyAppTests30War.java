/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class ConcurrencyAppTests30War extends ConcurrencyAppAbstractTests {

    @Test
    public void testConcurrencyScheduledTask1() throws Exception {
        assertNotNull("Did not find TESTS PASSED messages", server.waitForStringInLog("ScheduledTask: MANAGED THREAD VERIFICATION PASSED"));
    }

    @Test
    public void testConcurrencyScheduledTask2() throws Exception {
        assertNotNull("Did not find TESTS PASSED messages", server.waitForStringInLog("AppRunner: VERIFY SCHEDULED TASK REPETITION METHOD PASSED"));
    }

    @Test
    public void testConcurrencyScheduledTask3() throws Exception {
        assertNotNull("Did not find TESTS PASSED messages", server.waitForStringInLog("Async task 1: ASSERT ASYNC METHOD VERIFICATION PASSED"));
    }

    @Test
    public void testConcurrencyScheduledTask4() throws Exception {
        assertNotNull("Did not find TESTS PASSED messages", server.waitForStringInLog("Async task 2: ASSERT ASYNC METHOD VERIFICATION PASSED"));
    }

    @Test
    public void testConcurrencyScheduledTask5() throws Exception {
        assertNotNull("Did not find TESTS PASSED messages", server.waitForStringInLog("ScheduledTask2 Async task 3: ASSERT ASYNC METHOD VERIFICATION PASSED"));
    }

    @Test
    public void testConcurrencyAssertAsyncMethod1() throws Exception {
        assertNotNull("Did not find TESTS PASSED messages", server.waitForStringInLog("Assert Async Method: Async Task 1: MANAGED THREAD VERIFICATION PASSED"));
    }

    @Test
    public void testConcurrencyAssertAsyncMethod2() throws Exception {
        assertNotNull("Did not find TESTS PASSED messages", server.waitForStringInLog("Assert Async Method: Async Task 2: MANAGED THREAD VERIFICATION PASSED"));
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("servlet-6.0", "concurrent-3.0", "jndi-1.0", "connectors-2.1"));
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.WEB_APP_TAG;
    }
}
