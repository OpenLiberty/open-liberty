/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.TestConstants;
import test.common.SharedOutputManager;

import com.ibm.websphere.ras.SharedTr;
import com.ibm.ws.ffdc.FFDC;
import com.ibm.ws.ffdc.FFDCConfigurator;
import com.ibm.wsspi.logging.Incident;
import com.ibm.wsspi.logging.IncidentForwarder;
import com.ibm.wsspi.logprovider.FFDCFilterService;
import com.ibm.wsspi.logprovider.LogProviderConfig;

public class IncidentForwarderTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().logTo(TestConstants.BUILD_TMP);

    @Rule
    public TestRule outputRule = outputMgr;

    private final List<Incident> processedIncidents = new ArrayList<Incident>();
    public List<Throwable> processedThrowables = new ArrayList<Throwable>();;

    @Test
    public void testIncidentForwarded() {

        TestIncidentForwarder forwarder = new TestIncidentForwarder();

        Exception th = new Exception();

        LogProviderConfig config = SharedTr.getDefaultConfig();
        FFDCConfigurator.init(config);

        FFDCFilterService ffdc = FFDCConfigurator.getDelegate();
        FFDC.registerIncidentForwarder(forwarder);

        // Keep track of the time before the exception is processed
        Date beforeException = new Date();
        // Process the exception
        ffdc.processException(th, getClass().getName(), "100");

        assertEquals("One incident should have been forwarded", 1, processedIncidents.size());
        Incident incident = processedIncidents.get(0);
        assertEquals("The sourceId should be IncidentForwarderTest", getClass().getName(), incident.getSourceId());
        assertEquals("The probeId should be 100", "100", incident.getProbeId());

        // Get the current time, post exception processing
        Date afterException = new Date();
        // Dates are not all that accurate, so based on timing they may be the same
        assertTrue("The incident should have been processed after the start time. Start: " + beforeException + " incident: " + incident.getDateOfFirstOccurrence(),
                   beforeException.equals(incident.getDateOfFirstOccurrence()) || beforeException.before(incident.getDateOfFirstOccurrence()));
        assertTrue("The incident should have been processed before the current time",
                   incident.getDateOfFirstOccurrence().before(afterException) || incident.getDateOfFirstOccurrence().equals(afterException));

        assertEquals("One incident should have been processed", 1, incident.getCount());

        long time = System.currentTimeMillis();
        assertTrue("The timestamp [ " + incident.getTimeStamp() + " ]  should be before the current time [ " + time + " ]",
                   time >= incident.getTimeStamp());

        assertEquals("The exception name should be Throwable",
                     th.getClass().getName(), incident.getExceptionName());

        assertEquals("There should have been one throwable sent to the forwarder",
                     1, processedThrowables.size());
        assertEquals("The exception forwarded should be equal to the test exception",
                     th, processedThrowables.get(0));

        ffdc.processException(th, getClass().getName(), "100");

        assertEquals("There should be two incidents sent to the forwarder", 2, processedIncidents.size());
        Incident incident2 = processedIncidents.get(1);
        assertEquals(2, incident2.getCount());
        assertEquals("The two incidents should have the same date of first occurrence",
                     incident.getDateOfFirstOccurrence(), incident2.getDateOfFirstOccurrence());

        FFDC.deregisterIncidentForwarder(forwarder);
        FFDCConfigurator.stop();

    }

    private class TestIncidentForwarder implements IncidentForwarder {

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.wsspi.logging.IncidentForwarder#process(com.ibm.wsspi.logging.Incident, java.lang.Throwable)
         */
        @Override
        public void process(Incident incident, Throwable th) {
            processedIncidents.add(incident);
            processedThrowables.add(th);
        }

    }

}
