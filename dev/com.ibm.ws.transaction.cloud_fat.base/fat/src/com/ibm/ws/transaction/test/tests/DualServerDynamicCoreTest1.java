/*******************************************************************************
 * Copyright (c) 2019,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test.tests;

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
public abstract class DualServerDynamicCoreTest1 extends DualServerDynamicTestBase {

    @Test
    @SkipForRepeat({ SkipForRepeat.EE8_FEATURES, SkipForRepeat.NO_MODIFICATION })
    public void dynamicCloudRecovery001() throws Exception {
        dynamicTest(server1, server2, 1, 2);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE9_FEATURES, SkipForRepeat.NO_MODIFICATION })
    public void dynamicCloudRecovery002() throws Exception {
        dynamicTest(server1, server2, 2, 2);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE8_FEATURES, SkipForRepeat.EE9_FEATURES })
    public void dynamicCloudRecovery003() throws Exception {
        dynamicTest(server1, server2, 3, 2);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery004() throws Exception {
        dynamicTest(server1, server2, 4, 3);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE8_FEATURES, SkipForRepeat.NO_MODIFICATION })
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery005() throws Exception {
        dynamicTest(server1, server2, 5, 3);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE9_FEATURES, SkipForRepeat.NO_MODIFICATION })
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
    @SkipForRepeat({ SkipForRepeat.EE8_FEATURES, SkipForRepeat.EE9_FEATURES })
    public void dynamicCloudRecovery008() throws Exception {
        dynamicTest(server1, server2, 8, 2);
    }
}
