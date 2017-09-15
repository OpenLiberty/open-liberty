/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.event.internal;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.event.Topic;

/**
 * Simple tests on the scheduled event service.
 */
public class ScheduledEventServiceTest {

    private final Mockery mocker = new JUnit4Mockery();
    private ScheduledEventServiceImpl svc;
    final private EventEngine engine = mocker.mock(EventEngine.class);
    final private ScheduledExecutorService exec = mocker.mock(ScheduledExecutorService.class);

    @Before
    public void initialize() {
        svc = new ScheduledEventServiceImpl();
        svc.setScheduledExecutor(exec);
        svc.setEventEngine(engine);
    }

    /**
     * Test a single, non-repeating, event.
     */
    @Test
    public void testSingleEvent() {
        final Topic testTopic = new Topic("single/event");
        final Map<?, ?> testProps = null;
        mocker.checking(new Expectations() {
            {
                exactly(2).of(exec).schedule(with(any(Runnable.class)),
                                             with(0L), with(TimeUnit.SECONDS));
                exactly(2).of(exec).schedule(with(any(Runnable.class)),
                                             with(2L), with(TimeUnit.SECONDS));
            }
        });

        svc.schedule(testTopic, 0L, TimeUnit.SECONDS);
        svc.schedule(testTopic, testProps, 0L, TimeUnit.SECONDS);
        svc.schedule(testTopic, 2L, TimeUnit.SECONDS);
        svc.schedule(testTopic, testProps, 2L, TimeUnit.SECONDS);

        mocker.assertIsSatisfied();
    }

    /**
     * Test events that are repeating at set intervals.
     */
    @Test
    public void testRepeatingEvents() {
        final Topic testTopic = new Topic("repeating/events");
        final Map<?, ?> testProps = null;

        mocker.checking(new Expectations() {
            {
                exactly(2).of(exec).scheduleAtFixedRate(with(any(Runnable.class)),
                                                        with(0L), with(5L), with(TimeUnit.SECONDS));
                exactly(2).of(exec).scheduleAtFixedRate(with(any(Runnable.class)),
                                                        with(2L), with(4L), with(TimeUnit.SECONDS));
            }
        });

        svc.schedule(testTopic, 0L, 5L, TimeUnit.SECONDS);
        svc.schedule(testTopic, testProps, 0L, 5L, TimeUnit.SECONDS);
        svc.schedule(testTopic, 2L, 4L, TimeUnit.SECONDS);
        svc.schedule(testTopic, testProps, 2L, 4L, TimeUnit.SECONDS);

        mocker.assertIsSatisfied();
    }

    @Test
    public void testErrors() {
        final Topic testTopic = new Topic("error");
        try {
            svc.schedule(null, 0L, TimeUnit.SECONDS);
            Assert.fail("Null topic should have thrown IllegalArg");
        } catch (IllegalArgumentException iae) {
            // expected error
        }

        svc.setEventEngine(null);
        try {
            svc.schedule(testTopic, 0L, TimeUnit.SECONDS);
            Assert.fail("Missing engine should have thrown IllegalState");
        } catch (IllegalStateException ise) {
            // expected error
        } finally {
            svc.setEventEngine(engine);
        }
    }
}
