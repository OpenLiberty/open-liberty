/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.service.test;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.kernel.service.util.MemoryInformation;
import com.ibm.ws.kernel.service.util.OperatingSystem;
import com.ibm.ws.kernel.service.util.OperatingSystemType;

/**
 * Various unit tests.
 */
public class MemoryInformationTests {
    @Test
    public void testOSMemoryStatistics() throws Exception {
        // Since unit tests are part of builds, we're conservative in which
        // ones we always test
        if (OperatingSystem.instance().getOperatingSystemType() == OperatingSystemType.Linux) {
            long totalMemory = MemoryInformation.instance().getTotalMemory();
            System.out.println("Total memory: " + totalMemory);
            Assert.assertTrue("getTotalMemory invalid",
                              totalMemory > 0);

            long availableMemory = MemoryInformation.instance().getAvailableMemory();
            System.out.println("Available memory: " + availableMemory);
            Assert.assertTrue("getAvailableMemory invalid",
                              availableMemory > 0);

            float availableMemoryRatio = MemoryInformation.instance().getAvailableMemoryRatio();
            System.out.println("Available memory %: " + (availableMemoryRatio * 100.0));
            Assert.assertTrue("getAvailableMemoryRatio invalid",
                              availableMemoryRatio > 0);
        }
    }
}
