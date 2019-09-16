/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test;

import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/*
 * These tests are based on the original JTAREC recovery tests.
 * Test plan is attached to RTC WI 213854
 */
@Mode
public abstract class DualServerDynamicCoreTest extends DualServerDynamicTestBase {

    @Test
    public void dynamicCloudRecovery001() throws Exception {
        dynamicTest(1, 2);
    }

    @Test
    public void dynamicCloudRecovery002() throws Exception {
        dynamicTest(2, 2);
    }

    @Test
    public void dynamicCloudRecovery003() throws Exception {
        dynamicTest(3, 2);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery004() throws Exception {
        dynamicTest(4, 3);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery005() throws Exception {
        dynamicTest(5, 3);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery006() throws Exception {
        dynamicTest(6, 3);
    }

    @Test
    @Mode(TestMode.LITE)
    public void dynamicCloudRecovery007() throws Exception {
        dynamicTest(7, 2);
    }

    @Test
    public void dynamicCloudRecovery008() throws Exception {
        dynamicTest(8, 2);
    }

    @Test
    public void dynamicCloudRecovery009() throws Exception {
        dynamicTest(9, 2);
    }

    @Test
    public void dynamicCloudRecovery010() throws Exception {
        dynamicTest(10, 2);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void dynamicCloudRecovery011() throws Exception {
        dynamicTest(11, 2);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void dynamicCloudRecovery012() throws Exception {
        dynamicTest(12, 2);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void dynamicCloudRecovery013() throws Exception {
        dynamicTest(13, 2);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void dynamicCloudRecovery014() throws Exception {
        dynamicTest(14, 2);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery015() throws Exception {
        dynamicTest(15, 3);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery016() throws Exception {
        dynamicTest(16, 3);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery017() throws Exception {
        dynamicTest(17, 3);
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException" })
    public void dynamicCloudRecovery018() throws Exception {
        dynamicTest(18, 3);
    }

    @Test
    public void dynamicCloudRecovery047() throws Exception {
        dynamicTest(47, 4);
    }

    @Test
    public void dynamicCloudRecovery048() throws Exception {
        dynamicTest(48, 4);
    }

    @Test
    public void dynamicCloudRecovery050() throws Exception {
        dynamicTest(50, 10);
    }

    @Test
    public void dynamicCloudRecovery051() throws Exception {
        dynamicTest(51, 10);
    }

    @Mode(TestMode.LITE)
    @Test
    public void dynamicCloudRecovery090() throws Exception {
        dynamicTest(90, 3);
    }

}