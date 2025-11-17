/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
public abstract class DualServerDynamicCoreTest3 extends DualServerDynamicTestBase {

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery017() throws Exception {
        dynamicTest(server1, server2, 17, 3);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery018() throws Exception {
        dynamicTest(server1, server2, 18, 3);
    }

    @Test
    public void dynamicCloudRecovery047() throws Exception {
        dynamicTest(server1, server2, 47, 4);
    }

    @Test
    public void dynamicCloudRecovery048() throws Exception {
        dynamicTest(server1, server2, 48, 4);
    }

    @Test
    public void dynamicCloudRecovery050() throws Exception {
        dynamicTest(server1, server2, 50, 10);
    }

    @Test
    public void dynamicCloudRecovery051() throws Exception {
        dynamicTest(server1, server2, 51, 10);
    }

    @Test
    public void dynamicCloudRecovery090() throws Exception {
        dynamicTest(server1, server2, 90, 3);
    }
}
