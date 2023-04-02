/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/*
 * These tests are based on the original JTAREC recovery tests.
 * Test plan is attached to RTC WI 213854
 */
@Mode
public abstract class DualServerDynamicCoreTest2 extends DualServerDynamicTestBase {

    @Test
    @SkipForRepeat({ SkipForRepeat.NO_MODIFICATION, SkipForRepeat.EE8_FEATURES, SkipForRepeat.EE9_FEATURES })
    public void dynamicCloudRecovery009() throws Exception {
        dynamicTest(server1, server2, 9, 2);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE8_FEATURES, SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES })
    public void dynamicCloudRecovery010() throws Exception {
        dynamicTest(server1, server2, 10, 2);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES, SkipForRepeat.NO_MODIFICATION })
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void dynamicCloudRecovery011() throws Exception {
        dynamicTest(server1, server2, 11, 2);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE10_FEATURES, SkipForRepeat.NO_MODIFICATION, SkipForRepeat.EE8_FEATURES })
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void dynamicCloudRecovery012() throws Exception {
        dynamicTest(server1, server2, 12, 2);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.NO_MODIFICATION, SkipForRepeat.EE8_FEATURES, SkipForRepeat.EE9_FEATURES })
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void dynamicCloudRecovery013() throws Exception {
        dynamicTest(server1, server2, 13, 2);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE8_FEATURES, SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES })
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void dynamicCloudRecovery014() throws Exception {
        dynamicTest(server1, server2, 14, 2);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES, SkipForRepeat.NO_MODIFICATION })
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery015() throws Exception {
        dynamicTest(server1, server2, 15, 3);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE10_FEATURES, SkipForRepeat.NO_MODIFICATION, SkipForRepeat.EE8_FEATURES })
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery016() throws Exception {
        dynamicTest(server1, server2, 16, 3);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.NO_MODIFICATION, SkipForRepeat.EE8_FEATURES, SkipForRepeat.EE9_FEATURES })
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery017() throws Exception {
        dynamicTest(server1, server2, 17, 3);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE8_FEATURES, SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES })
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery018() throws Exception {
        dynamicTest(server1, server2, 18, 3);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES, SkipForRepeat.NO_MODIFICATION })
    public void dynamicCloudRecovery047() throws Exception {
        dynamicTest(server1, server2, 47, 4);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE10_FEATURES, SkipForRepeat.NO_MODIFICATION, SkipForRepeat.EE8_FEATURES })
    public void dynamicCloudRecovery048() throws Exception {
        dynamicTest(server1, server2, 48, 4);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.NO_MODIFICATION, SkipForRepeat.EE8_FEATURES, SkipForRepeat.EE9_FEATURES })
    public void dynamicCloudRecovery050() throws Exception {
        dynamicTest(server1, server2, 50, 10);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE8_FEATURES, SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES })
    public void dynamicCloudRecovery051() throws Exception {
        dynamicTest(server1, server2, 51, 10);
    }

    @Mode(TestMode.LITE)
    @Test
    public void dynamicCloudRecovery090() throws Exception {
        dynamicTest(server1, server2, 90, 3);
    }
}
