/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance_fat.cdi;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.After;
import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncBean;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncBean2;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncBean3;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncCallableBean;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncConfigBean;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncThreadContextTestBean;
import com.ibm.ws.microprofile.faulttolerance_fat.util.Connection;

import componenttest.app.FATServlet;

/**
 * Servlet implementation class Test
 */
@WebServlet("/async")
public class AsyncServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    AsyncBean bean;

    @Inject
    AsyncBean2 bean2;

    @Inject
    AsyncCallableBean callableBean;

    @Inject
    AsyncConfigBean configBean;

    @Inject
    AsyncThreadContextTestBean threadContextBean;

    @After
    public void checkNotInterrupted() {
        assertFalse("Thread left with interrupted flag set", Thread.interrupted());
    }

    @Test
    public void testAsync() throws InterruptedException, ExecutionException, TimeoutException {
        //should return straight away even though the method has a 5s sleep in it
        long start = System.currentTimeMillis();
        System.out.println(start + " - calling AsyncBean.connectA");
        Future<Connection> future = bean.connectA();
        long end = System.currentTimeMillis();
        System.out.println(end + " - got future");
        long duration = end - start;
        if (duration > TestConstants.FUTURE_THRESHOLD) { //should have returned almost instantly, if it takes FUTURE_THRESHOLD then there is something wrong
            throw new AssertionError("Method did not return quickly enough: " + duration);
        }
        if (future.isDone()) {
            if (future.isCancelled()) {
                try {
                    Connection conn = future.get();
                    throw new AssertionError("Future was cancelled. Reason unknown.");
                } catch (ExecutionException e) {
                    throw new AssertionError("Future was cancelled. Exception was " + e.getCause());
                }
            } else {
                throw new AssertionError("Future completed too fast");
            }
        }

        Thread.sleep(TestConstants.EXECUTION_THRESHOLD); //long enough for the call to complete
        if (!future.isDone()) {
            throw new AssertionError("Future did not complete fast enough");
        }
        start = System.currentTimeMillis();
        System.out.println(start + " - calling future.get");
        //we shouldn't need the extra timeout but don't want the test to hang if it is broken
        Connection conn = future.get(TestConstants.TEST_TIME_UNIT, TimeUnit.MILLISECONDS);
        end = System.currentTimeMillis();
        System.out.println(end + " - got connection");

        duration = end - start;
        if (duration > TestConstants.FUTURE_THRESHOLD) { //should have returned almost instantly, if it takes FUTURE_THRESHOLD then there is something wrong
            throw new AssertionError("Method did not return quickly enough: " + duration);
        }
        if (conn == null) {
            throw new AssertionError("Result not properly returned: " + conn);
        }
        String data = conn.getData();
        if (!AsyncBean.CONNECT_A_DATA.equals(data)) {
            throw new AssertionError("Bad data: " + data);
        }
    }

    @Test
    public void testAsyncVoid() throws InterruptedException, ExecutionException, TimeoutException {
        //should return straight away even though the method has a 5s sleep in it
        long start = System.currentTimeMillis();
        System.out.println(start + " - calling AsyncBean.connectC");
        Future<Void> future = bean.connectC();
        long end = System.currentTimeMillis();
        System.out.println(end + " - got future");
        long duration = end - start;
        if (duration > TestConstants.FUTURE_THRESHOLD) { //should have returned almost instantly, if it takes FUTURE_THRESHOLD then there is something wrong
            throw new AssertionError("Method did not return quickly enough: " + duration);
        }
        if (future.isDone()) {
            throw new AssertionError("Future completed too fast");
        }

        Thread.sleep(TestConstants.EXECUTION_THRESHOLD); //long enough for the call to complete
        if (!future.isDone()) {
            throw new AssertionError("Future did not complete fast enough");
        }
        start = System.currentTimeMillis();
        System.out.println(start + " - calling future.get");
        //we shouldn't need the extra timeout but don't want the test to hang if it is broken
        Object obj = future.get(TestConstants.TEST_TIME_UNIT, TimeUnit.MILLISECONDS);
        end = System.currentTimeMillis();
        System.out.println(end + " - got connection");

        duration = end - start;
        if (duration > TestConstants.FUTURE_THRESHOLD) { //should have returned almost instantly, if it takes 1000ms then there is something wrong
            throw new AssertionError("Method did not return quickly enough: " + duration);
        }
        if (obj != null) {
            throw new AssertionError("Result should be null: " + obj);
        }
    }

    @Test
    public void testAsyncTimeout() throws InterruptedException, ExecutionException, TimeoutException {
        //should return straight away even though the method has a 5s sleep in it
        long start = System.currentTimeMillis();
        System.out.println(start + " - calling AsyncBean.connectB");
        Future<Connection> future = bean.connectB();
        long end = System.currentTimeMillis();
        System.out.println(end + " - got future");

        long duration = end - start;
        if (duration > TestConstants.FUTURE_THRESHOLD) { //should have returned almost instantly, if it takes FUTURE_THRESHOLD then there is something wrong
            throw new AssertionError("Method did not return quickly enough: " + duration);
        }
        if (future.isDone()) {
            throw new AssertionError("Future completed too fast");
        }

        Thread.sleep(TestConstants.TIMEOUT + TestConstants.TEST_TIME_UNIT); //long enough for the call to timeout but not to complete normally
        if (!future.isDone()) {
            throw new AssertionError("Future did not timeout fast enough");
        }

        try {
            //FaultTolerance should have already timedout the future so we're expecting the FT TimeoutException
            Connection conn = future.get(TestConstants.TEST_TIME_UNIT, TimeUnit.MILLISECONDS);
            throw new AssertionError("Future did not timeout properly");
        } catch (ExecutionException t) {
            //expected
            assertThat(t.getCause(), instanceOf(org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class));
        }
    }

    @Test
    public void testAsyncTimeoutNoInterrupt() throws InterruptedException, TimeoutException {
        //should return straight away even though the method has a 5s sleep in it
        long start = System.currentTimeMillis();
        System.out.println(start + " - calling AsyncBean.connectB");
        Future<Void> future = bean.waitNoInterrupt();
        long end = System.currentTimeMillis();
        System.out.println(end + " - got future");

        long duration = end - start;
        if (duration > TestConstants.FUTURE_THRESHOLD) { //should have returned almost instantly, if it takes FUTURE_THRESHOLD then there is something wrong
            throw new AssertionError("Method did not return quickly enough: " + duration);
        }
        if (future.isDone()) {
            throw new AssertionError("Future completed too fast");
        }

        Thread.sleep(TestConstants.TIMEOUT + TestConstants.TEST_TIME_UNIT); //long enough for the call to timeout but not to complete normally
        if (!future.isDone()) {
            throw new AssertionError("Future did not timeout fast enough");
        }

        try {
            //FaultTolerance should have already timedout the future so we're expecting the FT TimeoutException
            future.get(TestConstants.TEST_TIME_UNIT, TimeUnit.MILLISECONDS);
            throw new AssertionError("Future did not timeout properly");
        } catch (ExecutionException t) {
            //expected
            assertThat(t.getCause(), instanceOf(org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class));
        }
    }

    @Test
    public void testAsyncMethodTimeout() throws InterruptedException, ExecutionException, TimeoutException {
        //should return straight away even though the method has a 5s sleep in it
        long start = System.currentTimeMillis();
        System.out.println(start + " - calling AsyncBean.connectB");
        Future<Connection> future = bean.connectB();
        long end = System.currentTimeMillis();
        System.out.println(end + " - got future");

        long duration = end - start;
        if (duration > TestConstants.FUTURE_THRESHOLD) { //should have returned almost instantly, if it takes FUTURE_THRESHOLD then there is something wrong
            throw new AssertionError("Method did not return quickly enough: " + duration);
        }
        if (future.isDone()) {
            throw new AssertionError("Future completed too fast");
        }

        try {
            //FaultTolerance should NOT have timedout yet so the short timeout on the method should result in a concurrent TimeoutException
            Connection conn = future.get(100, TimeUnit.MILLISECONDS);
            throw new AssertionError("Future did not timeout properly");
        } catch (TimeoutException t) {
            //expected
        }
    }

    //AsyncBean2 calls AsyncBean3 so that's a double thread jump
    @Test
    public void testAsyncDoubleJump() throws InterruptedException, ExecutionException, TimeoutException {
        //should return straight away even though the method has a 5s sleep in it
        long start = System.currentTimeMillis();
        System.out.println(start + " - calling AsyncBean.connectA");
        Future<Connection> future = bean2.connectA();
        long end = System.currentTimeMillis();
        System.out.println(end + " - got future");
        long duration = end - start;
        if (duration > TestConstants.FUTURE_THRESHOLD) { //should have returned almost instantly, if it takes 1000ms then there is something wrong
            throw new AssertionError("Method did not return quickly enough: " + duration);
        }
        if (future.isDone()) {
            throw new AssertionError("Future completed too fast");
        }

        Thread.sleep(7000); //long enough for the call to complete
        if (!future.isDone()) {
            throw new AssertionError("Future did not complete fast enough");
        }
        start = System.currentTimeMillis();
        System.out.println(start + " - calling future.get");
        //we shouldn't need the extra timeout but don't want the test to hang if it is broken
        Connection conn = future.get(TestConstants.FUTURE_THRESHOLD, TimeUnit.MILLISECONDS);
        end = System.currentTimeMillis();
        System.out.println(end + " - got connection");

        duration = end - start;
        if (duration > TestConstants.FUTURE_THRESHOLD) { //should have returned almost instantly, if it takes 1000ms then there is something wrong
            throw new AssertionError("Method did not return quickly enough: " + duration);
        }
        if (conn == null) {
            throw new AssertionError("Result not properly returned: " + conn);
        }
        String data = conn.getData();
        if (!AsyncBean3.CONNECT_A_DATA.equals(data)) {
            throw new AssertionError("Bad data: " + data);
        }
    }

    @Test
    public void testAsyncCallable() throws InterruptedException, ExecutionException, Exception {
        // Async methods with a generic return type (e.g. Callable.call()) used to cause problems
        long start = System.currentTimeMillis();
        Future<String> future = callableBean.call();
        long end = System.currentTimeMillis();
        long duration = end - start;
        assertThat("Call duration", duration, lessThan(TestConstants.FUTURE_THRESHOLD));

        Thread.sleep(TestConstants.EXECUTION_THRESHOLD);
        assertThat("Future is done after waiting", future.isDone(), is(true));
        assertThat("Call result", future.get(), is("Done"));
    }

    public void testAsyncConfig() throws Exception {
        Future<String> value = configBean.getValue();
        assertThat(value.get(), is("configuredAsyncValue"));
    }

    public void testAsyncConfigInjected() throws Exception {
        Future<String> value = threadContextBean.getConfigValueFromInjectedBean();
        assertThat(value.get(), is("configuredAsyncValue"));
    }

    public void testAsyncGetCdi() throws Exception {
        Future<CDI<Object>> value = threadContextBean.getCdi();
        assertThat(value.get(), notNullValue());
    }

    public void testAsyncGetBeanManagerViaJndi() throws Exception {
        Future<BeanManager> value = threadContextBean.getBeanManagerViaJndi();
        assertThat(value.get(), notNullValue());
    }

    @Test
    public void testAsyncCancel() throws Exception {
        Future<Void> result = bean.waitCheckCancel();

        Thread.sleep(TestConstants.TEST_TIME_UNIT);

        result.cancel(true);

        assertThat("cancel", result.cancel(true), is(true));
        assertThat("isCancelled", result.isCancelled(), is(true));
        assertThat("isDone", result.isDone(), is(true));

        try {
            result.get(0, TimeUnit.SECONDS);
            fail("get() Did not throw cancellation exception");
        } catch (CancellationException e) {
            assertThat("exception from get()", e, instanceOf(CancellationException.class));
        }

        Thread.sleep(TestConstants.TEST_TWEAK_TIME_UNIT);

        assertThat(bean.wasInterrupted(), is(true));
    }

}
