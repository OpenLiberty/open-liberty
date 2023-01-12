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
public abstract class DualServerDynamicCoreTest1 extends DualServerDynamicTestBase {

    @Test
    @SkipForRepeat({ SkipForRepeat.NO_MODIFICATION, SkipForRepeat.EE8_FEATURES, SkipForRepeat.EE9_FEATURES })
    public void dynamicCloudRecovery001() throws Exception {
        dynamicTest(server1, server2, 1, 2);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE8_FEATURES, SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES })
    public void dynamicCloudRecovery002() throws Exception {
        dynamicTest(server1, server2, 2, 2);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES, SkipForRepeat.NO_MODIFICATION })
    public void dynamicCloudRecovery003() throws Exception {
        dynamicTest(server1, server2, 3, 2);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE10_FEATURES, SkipForRepeat.NO_MODIFICATION, SkipForRepeat.EE8_FEATURES })
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery004() throws Exception {
        dynamicTest(server1, server2, 4, 3);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.NO_MODIFICATION, SkipForRepeat.EE8_FEATURES, SkipForRepeat.EE9_FEATURES })
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery005() throws Exception {
        dynamicTest(server1, server2, 5, 3);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE8_FEATURES, SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES })
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery006() throws Exception {
        dynamicTest(server1, server2, 6, 3);
    }

    @Test
    @Mode(TestMode.LITE)
    public void dynamicCloudRecovery007() throws Exception {
        dynamicTest(server1, server2, 7, 2);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE9_FEATURES, SkipForRepeat.EE10_FEATURES, SkipForRepeat.NO_MODIFICATION })
    public void dynamicCloudRecovery008() throws Exception {
        dynamicTest(server1, server2, 8, 2);
    }
}
