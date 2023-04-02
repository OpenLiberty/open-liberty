/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.kernel.service.test;

import org.junit.Test;

import com.ibm.ws.kernel.service.util.CpuInfo;

import junit.framework.Assert;

/**
 *
 */
public class CpuInfoTest {

    @Test
    public void test() throws Exception {

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < 2000);
            }
        });

        // prime the initial CPU usage.
        CpuInfo.getJavaCpuUsage();
        t.start();
        t.join();

        double cpuUsage = CpuInfo.getJavaCpuUsage();
        System.out.println("CPU usage is " + cpuUsage);
        if (cpuUsage == -1) {
            Assert.fail("CpuInfo returned -1");
        }
    }

}
