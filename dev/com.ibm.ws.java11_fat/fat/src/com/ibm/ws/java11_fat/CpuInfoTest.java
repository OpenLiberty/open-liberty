/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.java11_fat;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.kernel.service.util.CpuInfo;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FATServletClient;

/**
 * This is really just a unit test, but we will include it in a FAT instead
 * because FATs get run on a lot of different java levels and unit tests don't
 *
 * CpuInfo is difficult to test accurately without just duplicating the same logic so really this
 * is just a sniff test on multiple different JVMs
 */
@RunWith(FATRunner.class)
public class CpuInfoTest extends FATServletClient {

    @Test
    public void testGetAvailableProcessors() {
        int availableProcessors = CpuInfo.getAvailableProcessors();
        assertTrue(availableProcessors > 0);
    }

    @Test
    public void testGetJavaCpuUsage() {
        double javaCpuUsage = CpuInfo.getJavaCpuUsage();
        assertTrue(javaCpuUsage >= -1);
        assertTrue(javaCpuUsage <= 100);
    }

    @Test
    public void testGetSystemCpuUsage() {
        double systemCpuUsage = CpuInfo.getSystemCpuUsage();
        assertTrue(systemCpuUsage >= -1);
        assertTrue(systemCpuUsage <= 100);
    }
}
