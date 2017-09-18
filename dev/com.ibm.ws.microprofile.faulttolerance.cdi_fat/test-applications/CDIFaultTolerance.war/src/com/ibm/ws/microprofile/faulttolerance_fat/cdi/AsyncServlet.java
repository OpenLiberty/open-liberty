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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncBean;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncBean2;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncBean3;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncCallableBean;
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

    public void testAsync(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException, InterruptedException, ExecutionException, TimeoutException {
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

    public void testAsyncVoid(HttpServletRequest request,
                              HttpServletResponse response) throws ServletException, IOException, InterruptedException, ExecutionException, TimeoutException {
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

    public void testAsyncTimeout(HttpServletRequest request,
                                 HttpServletResponse response) throws ServletException, IOException, InterruptedException, ExecutionException, TimeoutException {
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

    public void testAsyncMethodTimeout(HttpServletRequest request,
                                       HttpServletResponse response) throws ServletException, IOException, InterruptedException, ExecutionException, TimeoutException {
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
    public void testAsyncDoubleJump(HttpServletRequest request,
                                    HttpServletResponse response) throws ServletException, IOException, InterruptedException, ExecutionException, TimeoutException {
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

    public void testAsyncCallable(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, ExecutionException, Exception {
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

    /**
     * This test should only pass if MP_Fault_Tolerance_NonFallback_Enabled is set to false
     */
    public void testAsyncDisabled(HttpServletRequest request, HttpServletResponse response) throws Exception {
        long start = System.currentTimeMillis();
        Future<Connection> future = bean.connectA();
        long end = System.currentTimeMillis();
        long duration = end - start;

        // Ensure that this method was executed synchronously
        assertThat("Call duration", duration, greaterThan(TestConstants.WORK_TIME - TestConstants.TEST_TWEAK_TIME_UNIT));
        assertThat("Call result", future.get(), is(notNullValue()));
        assertThat("Call result", future.get().getData(), equalTo(AsyncBean.CONNECT_A_DATA));
    }

}
