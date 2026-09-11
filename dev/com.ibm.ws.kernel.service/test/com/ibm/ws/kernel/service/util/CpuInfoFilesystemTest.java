/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.kernel.service.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ibm.ws.kernel.service.util.CpuInfo;

/**
 * Unit tests for the cgroup filesystem parsing logic in CpuInfo.
 * Tests cgroups v1, v2, hybrid, and no-cgroup scenarios by writing
 * temporary files and supplying their paths to the package-private overload.
 */
public class CpuInfoFilesystemTest {

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    // Paths supplied to the method under test; set per test case
    private String v1PeriodPath;
    private String v1QuotaPath;
    private String v2CpuMaxPath;

    // Non-existent sentinel path used when a file should be absent
    private String absentPath;

    @Before
    public void setUp() throws IOException {
        absentPath = new File(tmpFolder.getRoot(), "does-not-exist").getAbsolutePath();
        // Default: no files present (all absent)
        v1PeriodPath = absentPath;
        v1QuotaPath = absentPath;
        v2CpuMaxPath = absentPath;
    }

    @After
    public void tearDown() {
        // TemporaryFolder cleans up automatically
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private File writeFile(String name, String content) throws IOException {
        File f = tmpFolder.newFile(name);
        try (FileWriter fw = new FileWriter(f)) {
            fw.write(content);
        }
        return f;
    }

    private static void assertNearlyEqual(float expected, float actual) {
        assertTrue("Expected ~" + expected + " but got " + actual,
                   Math.abs(expected - actual) < 0.01f);
    }

    // -----------------------------------------------------------------------
    // cgroups v1 tests
    // -----------------------------------------------------------------------

    @Test
    public void testV1_halfCpu() throws IOException {
        // quota=50000, period=100000 → 0.50 CPUs
        v1QuotaPath = writeFile("cpu.cfs_quota_us", "50000\n").getAbsolutePath();
        v1PeriodPath = writeFile("cpu.cfs_period_us", "100000\n").getAbsolutePath();

        float result = CpuInfo.getAvailableProcessorsFromFilesystemFloat(v1PeriodPath, v1QuotaPath, v2CpuMaxPath);
        assertNearlyEqual(0.50f, result);
    }

    @Test
    public void testV1_twoCpus() throws IOException {
        // quota=200000, period=100000 → 2.00 CPUs
        v1QuotaPath = writeFile("cpu.cfs_quota_us_2", "200000\n").getAbsolutePath();
        v1PeriodPath = writeFile("cpu.cfs_period_us_2", "100000\n").getAbsolutePath();

        float result = CpuInfo.getAvailableProcessorsFromFilesystemFloat(v1PeriodPath, v1QuotaPath, v2CpuMaxPath);
        assertNearlyEqual(2.00f, result);
    }

    @Test
    public void testV1_negativeQuotaMeansUnlimited() throws IOException {
        // quota=-1 means no limit in v1; should return -1 so caller falls back to Runtime
        v1QuotaPath = writeFile("cpu.cfs_quota_us_neg", "-1\n").getAbsolutePath();
        v1PeriodPath = writeFile("cpu.cfs_period_us_neg", "100000\n").getAbsolutePath();

        float result = CpuInfo.getAvailableProcessorsFromFilesystemFloat(v1PeriodPath, v1QuotaPath, v2CpuMaxPath);
        assertTrue("Expected -1 for unlimited v1 quota, got: " + result, result <= 0);
    }

    // -----------------------------------------------------------------------
    // cgroups v2 tests
    // -----------------------------------------------------------------------

    @Test
    public void testV2_halfCpu() throws IOException {
        // cpu.max: "50000 100000" → 0.50 CPUs
        v2CpuMaxPath = writeFile("cpu.max", "50000 100000\n").getAbsolutePath();

        float result = CpuInfo.getAvailableProcessorsFromFilesystemFloat(v1PeriodPath, v1QuotaPath, v2CpuMaxPath);
        assertNearlyEqual(0.50f, result);
    }

    @Test
    public void testV2_twoCpus() throws IOException {
        // cpu.max: "200000 100000" → 2.00 CPUs
        v2CpuMaxPath = writeFile("cpu.max_2", "200000 100000\n").getAbsolutePath();

        float result = CpuInfo.getAvailableProcessorsFromFilesystemFloat(v1PeriodPath, v1QuotaPath, v2CpuMaxPath);
        assertNearlyEqual(2.00f, result);
    }

    @Test
    public void testV2_unlimitedQuota() throws IOException {
        // cpu.max: "max 100000" → no limit; should return -1
        v2CpuMaxPath = writeFile("cpu.max_unlimited", "max 100000\n").getAbsolutePath();

        float result = CpuInfo.getAvailableProcessorsFromFilesystemFloat(v1PeriodPath, v1QuotaPath, v2CpuMaxPath);
        assertTrue("Expected -1 for unlimited v2 quota, got: " + result, result <= 0);
    }

    @Test
    public void testV2_malformedFile() throws IOException {
        // cpu.max with unexpected content should not throw; should return -1
        v2CpuMaxPath = writeFile("cpu.max_bad", "not-a-number garbage extra\n").getAbsolutePath();

        float result = CpuInfo.getAvailableProcessorsFromFilesystemFloat(v1PeriodPath, v1QuotaPath, v2CpuMaxPath);
        assertTrue("Expected -1 for malformed v2 cpu.max, got: " + result, result <= 0);
    }

    // -----------------------------------------------------------------------
    // No cgroup files present
    // -----------------------------------------------------------------------

    @Test
    public void testNoCgroupFiles_returnsNegative() {
        // Neither v1 nor v2 files exist → -1 so caller uses Runtime
        float result = CpuInfo.getAvailableProcessorsFromFilesystemFloat(v1PeriodPath, v1QuotaPath, v2CpuMaxPath);
        assertTrue("Expected -1 when no cgroup files present, got: " + result, result <= 0);
    }

    // -----------------------------------------------------------------------
    // Hybrid: both v1 and v2 files present (e.g. AKS 1.25)
    // v1 should take priority
    // -----------------------------------------------------------------------

    @Test
    public void testHybrid_v1TakesPriority() throws IOException {
        // v1 says 0.5 CPU, v2 says 2.0 CPU — v1 must win
        v1QuotaPath = writeFile("cpu.cfs_quota_us_h", "50000\n").getAbsolutePath();
        v1PeriodPath = writeFile("cpu.cfs_period_us_h", "100000\n").getAbsolutePath();
        v2CpuMaxPath = writeFile("cpu.max_h", "200000 100000\n").getAbsolutePath();

        float result = CpuInfo.getAvailableProcessorsFromFilesystemFloat(v1PeriodPath, v1QuotaPath, v2CpuMaxPath);
        assertNearlyEqual(0.50f, result);
    }

    @Test
    public void testHybrid_v1UnlimitedFallsBackToV2() throws IOException {
        // v1 quota=-1 (unlimited), v2 says 1.5 CPUs — should fall back to v2
        v1QuotaPath = writeFile("cpu.cfs_quota_us_fb", "-1\n").getAbsolutePath();
        v1PeriodPath = writeFile("cpu.cfs_period_us_fb", "100000\n").getAbsolutePath();
        v2CpuMaxPath = writeFile("cpu.max_fb", "150000 100000\n").getAbsolutePath();

        float result = CpuInfo.getAvailableProcessorsFromFilesystemFloat(v1PeriodPath, v1QuotaPath, v2CpuMaxPath);
        assertNearlyEqual(1.50f, result);
    }
}
