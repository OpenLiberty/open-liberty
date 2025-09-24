/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.topology.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for ExternalTestServiceReporter
 */
public class ExternalTestServiceReporterTest {

    @Mock
    ExternalTestService service;

    @Mock
    ExternalTestService additionalService;

    @Rule
    public TestName testName = new TestName();

    private AutoCloseable mockedObjets;

    @Before
    public void init() {
        mockedObjets = MockitoAnnotations.openMocks(this);
    }

    @After
    public void close() throws Exception {
        mockedObjets.close();
    }

    @Test
    public void testUnhealthyReportInitializesCollection() {
        Collection<String> unhealthyReport = ExternalTestServiceReporter.getUnhealthyReport(testName.getMethodName());
        assertTrue(unhealthyReport.isEmpty());

        //Ensure unmodifiable
        try {
            unhealthyReport.add("example.com");
            fail("Should not have been able to add an element to an unmodifiable collection.");
        } catch (UnsupportedOperationException e) {
            //expected
        } catch (Exception e) {
            fail("Caugh something other than UnsupportedOperationException: " + e.getMessage());
        }
    }

    @Test
    public void testReportUnhealyInitializesCollection() {
        when(service.getServiceName()).thenReturn(testName.getMethodName());
        when(service.getAddress()).thenReturn("example.1.com");

        ExternalTestServiceReporter.reportUnhealthy(service, "Test purposes");

        Collection<String> unhealthyReport = ExternalTestServiceReporter.getUnhealthyReport(testName.getMethodName());
        assertFalse(unhealthyReport.isEmpty());
        assertEquals(1, unhealthyReport.size());
        assertEquals("example.1.com", unhealthyReport.iterator().next());
    }

    @Test
    public void testUnhealthyReportUpdates() {
        Collection<String> unhealthyReport = ExternalTestServiceReporter.getUnhealthyReport(testName.getMethodName());
        assertTrue(unhealthyReport.isEmpty());

        when(service.getServiceName()).thenReturn(testName.getMethodName());
        when(service.getAddress()).thenReturn("example.2.com");

        ExternalTestServiceReporter.reportUnhealthy(service, "Test purposes");
        assertFalse(unhealthyReport.isEmpty());
        assertEquals(1, unhealthyReport.size());
        assertEquals("example.2.com", unhealthyReport.iterator().next());
    }

    @Test
    public void testUnhealthyReportsGrowCollection() {
        when(service.getServiceName()).thenReturn(testName.getMethodName());
        when(service.getAddress()).thenReturn("example.3.com");
        ExternalTestServiceReporter.reportUnhealthy(service, "Test purposes");

        when(additionalService.getServiceName()).thenReturn(testName.getMethodName());
        when(additionalService.getAddress()).thenReturn("example.4.com");
        ExternalTestServiceReporter.reportUnhealthy(additionalService, "Test purposes");

        Collection<String> unhealthyReport = ExternalTestServiceReporter.getUnhealthyReport(testName.getMethodName());
        assertFalse(unhealthyReport.isEmpty());
        assertEquals(2, unhealthyReport.size());
    }
}
