/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.internal;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JMock.class)
public class DeferrableScheduledExecutorTest {
    private final Mockery mockery = new Mockery();
    private ScheduledExecutorService executor;
    private DeferrableScheduledExecutorImpl deferrableExecutor;

    @Before
    public void before() {
        executor = mockery.mock(ScheduledExecutorService.class);
        deferrableExecutor = new DeferrableScheduledExecutorImpl();
        deferrableExecutor.setExecutor(executor);
    }

    private static long period(TimeUnit unit) {
        return unit.convert(DeferrableScheduledExecutorImpl.PERIOD_MILLISECONDS, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRoundUpDelay() {
        Assert.assertEquals(0, DeferrableScheduledExecutorImpl.roundUpDelay(0, TimeUnit.SECONDS, 0));
        Assert.assertEquals(0, DeferrableScheduledExecutorImpl.roundUpDelay(-1, TimeUnit.SECONDS, 0));
        Assert.assertEquals(0, DeferrableScheduledExecutorImpl.roundUpDelay(-Long.MAX_VALUE, TimeUnit.SECONDS, 0));
        Assert.assertEquals(period(TimeUnit.SECONDS), DeferrableScheduledExecutorImpl.roundUpDelay(1, TimeUnit.SECONDS, 0));
        Assert.assertEquals(period(TimeUnit.SECONDS), DeferrableScheduledExecutorImpl.roundUpDelay(period(TimeUnit.SECONDS), TimeUnit.SECONDS, 0));
        Assert.assertEquals(2 * period(TimeUnit.SECONDS), DeferrableScheduledExecutorImpl.roundUpDelay(period(TimeUnit.SECONDS) + 1, TimeUnit.SECONDS, 0));
        Assert.assertEquals(2 * period(TimeUnit.SECONDS), DeferrableScheduledExecutorImpl.roundUpDelay(2 * period(TimeUnit.SECONDS), TimeUnit.SECONDS, 0));
        Assert.assertEquals(period(TimeUnit.SECONDS) - 1,
                            DeferrableScheduledExecutorImpl.roundUpDelay(1, TimeUnit.SECONDS, TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS)));
        Assert.assertEquals(period(TimeUnit.MILLISECONDS) - 1,
                            DeferrableScheduledExecutorImpl.roundUpDelay(1, TimeUnit.MILLISECONDS, 1));
    }

    @Test
    public void testExecute() {
        mockery.checking(new Expectations() {
            {
                one(executor).execute(null);
            }
        });
        deferrableExecutor.execute(null);
    }

    @Test
    public void testShutdown() {
        mockery.checking(new Expectations() {
            {
                one(executor).shutdown();
            }
        });
        deferrableExecutor.shutdown();
    }

    @Test
    public void testShutdownNow() {
        mockery.checking(new Expectations() {
            {
                one(executor).shutdownNow();
            }
        });
        deferrableExecutor.shutdownNow();
    }

    @Test
    public void testIsShutdown() {
        mockery.checking(new Expectations() {
            {
                one(executor).isShutdown();
            }
        });
        deferrableExecutor.isShutdown();
    }

    @Test
    public void testIsTerminated() {
        mockery.checking(new Expectations() {
            {
                one(executor).isTerminated();
            }
        });
        deferrableExecutor.isTerminated();
    }

    @Test
    public void testAwaitTermination() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(executor).awaitTermination(0, null);
            }
        });
        deferrableExecutor.awaitTermination(0, null);
    }

    @Test
    public void testSubmitCallable() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(executor).submit((Callable<?>) null);
            }
        });
        deferrableExecutor.submit((Callable<?>) null);
    }

    @Test
    public void testSubmitRunnable() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(executor).submit((Runnable) null);
            }
        });
        deferrableExecutor.submit((Runnable) null);
    }

    @Test
    public void testSubmitRunnableResult() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(executor).submit((Runnable) null, null);
            }
        });
        deferrableExecutor.submit((Runnable) null, null);
    }

    @Test
    public void testInvokeAll() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(executor).invokeAll(null);
            }
        });
        deferrableExecutor.invokeAll(null);
    }

    @Test
    public void testInvokeAllWithTimeout() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(executor).invokeAll(null, 0, null);
            }
        });
        deferrableExecutor.invokeAll(null, 0, null);
    }

    @Test
    public void testInvokeAny() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(executor).invokeAny(null);
            }
        });
        deferrableExecutor.invokeAny(null);
    }

    @Test
    public void testInvokeAnyWithTimeout() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(executor).invokeAny(null, 0, null);
            }
        });
        deferrableExecutor.invokeAny(null, 0, null);
    }

    private static Matcher<Long> inRange(final long min, final long max) {
        return new BaseMatcher<Long>() {
            @Override
            public void describeTo(Description desc) {
                desc.appendValue(min);
                desc.appendText(" to ");
                desc.appendValue(max);
            }

            @Override
            public boolean matches(Object value) {
                if (value instanceof Long) {
                    long longValue = (Long) value;
                    return longValue >= min && longValue <= max;
                }
                return false;
            }
        };
    }

    private static Matcher<Long> delayInRange(TimeUnit unit) {
        return inRange(0, period(unit));
    }

    @Test
    public void testScheduleRunnable() throws Exception {
        final TimeUnit unit = TimeUnit.SECONDS;
        mockery.checking(new Expectations() {
            {
                one(executor).schedule(with((Runnable) null), with(delayInRange(unit)), with(unit));
            }
        });
        deferrableExecutor.schedule((Runnable) null, 0, unit);
    }

    @Test
    public void testScheduleCallable() throws Exception {
        final TimeUnit unit = TimeUnit.SECONDS;
        mockery.checking(new Expectations() {
            {
                one(executor).schedule(with((Callable<?>) null), with(delayInRange(unit)), with(unit));
            }
        });
        deferrableExecutor.schedule((Callable<?>) null, 0, unit);
    }

    @Test
    public void testScheduleAtFixedRate() throws Exception {
        final TimeUnit unit = TimeUnit.SECONDS;
        mockery.checking(new Expectations() {
            {
                one(executor).scheduleAtFixedRate(with((Runnable) null), with(delayInRange(unit)), with(period(unit)), with(unit));
            }
        });
        deferrableExecutor.scheduleAtFixedRate((Runnable) null, 0, 5, unit);
    }

    @Test
    public void testScheduleWithFixedDelay() throws Exception {
        final TimeUnit unit = TimeUnit.SECONDS;
        mockery.checking(new Expectations() {
            {
                one(executor).scheduleWithFixedDelay(with((Runnable) null), with(delayInRange(unit)), with(5L), with(unit));
            }
        });
        deferrableExecutor.scheduleWithFixedDelay((Runnable) null, 0, 5, unit);
    }
}
