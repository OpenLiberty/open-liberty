/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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
package tests;

import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;

/*
 * These tests are based on the original JTAREC recovery tests.
 * Test plan is attached to RTC WI 213854
 */
public abstract class DualServerDynamicCoreTest2 extends DualServerDynamicTestBase {

    @Test
    public void dynamicCloudRecovery009() throws Exception {
        dynamicTest(server1, server2, 9, 2);
    }

    @Test
    public void dynamicCloudRecovery010() throws Exception {
        dynamicTest(server1, server2, 10, 2);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void dynamicCloudRecovery011() throws Exception {
        dynamicTest(server1, server2, 11, 2);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void dynamicCloudRecovery012() throws Exception {
        dynamicTest(server1, server2, 12, 2);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void dynamicCloudRecovery013() throws Exception {
        dynamicTest(server1, server2, 13, 2);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void dynamicCloudRecovery014() throws Exception {
        dynamicTest(server1, server2, 14, 2);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery015() throws Exception {
        dynamicTest(server1, server2, 15, 3);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery016() throws Exception {
        dynamicTest(server1, server2, 16, 3);
    }
}
