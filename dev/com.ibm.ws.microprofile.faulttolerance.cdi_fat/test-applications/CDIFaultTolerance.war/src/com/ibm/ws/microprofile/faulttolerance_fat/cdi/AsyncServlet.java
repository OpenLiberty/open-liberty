/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance_fat.cdi;

import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.FutureAsserts.assertFutureDoesNotComplete;
import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.FutureAsserts.assertFutureGetsCancelled;
import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.FutureAsserts.assertFutureHasResult;
import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.FutureAsserts.assertFutureThrowsException;
import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.SyntheticTask.InterruptionAction.IGNORE;
import static com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.SyntheticTask.InterruptionAction.RETURN;
import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Future;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.Test;

import com.ibm.websphere.microprofile.faulttolerance_fat.suite.BasicTest;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncBean;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncBean2;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncCallableBean;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncConfigBean;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.AsyncThreadContextTestBean;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.FutureAsserts;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.SyntheticTask;
import com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans.SyntheticTaskManager;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;

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

    public SyntheticTaskManager syntheticTaskManager = new SyntheticTaskManager();

    @Override
    protected void after() throws Exception {
        assertFalse("Thread left with interrupted flag set", Thread.interrupted());
    }

    @BasicTest
    @Test
    public void testAsync() throws Exception {
        syntheticTaskManager.runTest(() -> {
            SyntheticTask<String> task = syntheticTaskManager.newTask();
            task.withResult("OK");

            // Call the method
            Future<String> result = bean.runTask(task);
            task.assertStarts();

            // Check that while it's running, the result does not complete and the future is not done
            assertFutureDoesNotComplete(result);
            assertFalse(result.isDone());
            assertFalse(result.isCancelled());

            // Check that after it's complete, the result is returned and the future is done
            task.complete();
            assertFutureHasResult(result, "OK");
            assertTrue(result.isDone());
            assertFalse(result.isCancelled());
        });
    }

    @Test
    public void testAsyncVoid() throws Exception {
        syntheticTaskManager.runTest(() -> {
            SyntheticTask<Void> task = syntheticTaskManager.newTask();
            task.withResult(null);

            // Call the method
            Future<Void> result = bean.runTaskVoid(task);
            task.assertStarts();

            // Check that while it's running, the result does not complete and the future is not done
            assertFutureDoesNotComplete(result);
            assertFalse(result.isDone());
            assertFalse(result.isCancelled());

            // Check that after it's complete, the result is returned and the future is done
            task.complete();
            assertFutureHasResult(result, null);
            assertTrue(result.isDone());
            assertFalse(result.isCancelled());
        });
    }

    @Test
    public void testAsyncTimeout() throws Exception {
        syntheticTaskManager.runTest(() -> {
            SyntheticTask<String> task = syntheticTaskManager.newTask();
            task.withResult("OK");
            task.onInterruption(RETURN);

            // Call the method
            Future<String> result = bean.runTaskTimeout(task);
            task.assertStarts();

            // Shouldn't be done immediately
            assertFalse(result.isDone());

            // Check that, although we don't complete the task, the result does throw a TimeoutException
            assertFutureThrowsException(result, TimeoutException.class);
        });
    }

    @Test
    public void testAsyncTimeoutNoInterrupt() throws Exception {
        syntheticTaskManager.runTest(() -> {
            SyntheticTask<String> task = syntheticTaskManager.newTask();
            task.withResult("OK");
            task.onInterruption(IGNORE);

            // Call the method
            Future<String> result = bean.runTaskTimeout(task);
            task.assertStarts();

            // Check that, although we don't complete the task, and the task ignores the interruption, the result does throw a TimeoutException
            assertFutureThrowsException(result, TimeoutException.class);
        });
    }

    //AsyncBean2 calls AsyncBean3 so that's a double thread jump
    @Test
    public void testAsyncDoubleJump() throws Exception {
        syntheticTaskManager.runTest(() -> {
            SyntheticTask<String> task = syntheticTaskManager.newTask();
            task.withResult("OK");
            Future<String> result = bean2.runTask(task);
            task.assertStarts();
            // Check that while it's running, the result does not complete and the future is not done
            assertFutureDoesNotComplete(result);
            assertFalse(result.isDone());
            assertFalse(result.isCancelled());

            // Check that after it's complete, the result is returned and the future is done
            task.complete();
            assertFutureHasResult(result, "OK");
            assertTrue(result.isDone());
            assertFalse(result.isCancelled());
        });
    }

    @Test
    @Mode(FULL)
    public void testAsyncCallable() throws Exception {
        syntheticTaskManager.runTest(() -> {
            // Async methods with a generic return type (e.g. Callable.call()) used to cause problems
            SyntheticTask<String> task = syntheticTaskManager.newTask();
            task.withResult("OK");
            callableBean.setTask(task);
            Future<String> result = callableBean.call();
            task.assertStarts();

            // Check that while it's running, the result does not complete and the future is not done
            assertFutureDoesNotComplete(result);
            assertFalse(result.isDone());
            assertFalse(result.isCancelled());

            // Check that after it's complete, the result is returned and the future is done
            task.complete();
            FutureAsserts.assertFutureHasResult(result, "OK");
            assertTrue(result.isDone());
            assertFalse(result.isCancelled());
        });
    }

    @Test
    public void testAsyncCancel() throws Exception {
        syntheticTaskManager.runTest(() -> {
            SyntheticTask<String> task = syntheticTaskManager.newTask();
            task.withResult("OK");
            task.onInterruption(RETURN); // Note this will aasert that an interruption occurs

            // Call the method
            Future<String> result = bean.runTask(task);
            task.assertStarts();

            // Check that while it's running, the result does not complete and the future is not done
            assertFutureDoesNotComplete(result);
            assertFalse(result.isDone());
            assertFalse(result.isCancelled());

            // Check that after it's cancelled, the result is returned and the future is done
            result.cancel(true);
            assertFutureGetsCancelled(result);
            assertTrue(result.isDone());
            assertTrue(result.isCancelled());
        });
    }

    @Test
    public void testAsyncConfig() throws Exception {
        Future<String> value = configBean.getValue();
        assertThat(value.get(), is("configuredAsyncValue"));
    }

    @Test
    public void testAsyncConfigInjected() throws Exception {
        Future<String> value = threadContextBean.getConfigValueFromInjectedBean();
        assertThat(value.get(), is("configuredAsyncValue"));
    }

    @Test
    public void testAsyncGetCdi() throws Exception {
        Future<CDI<Object>> value = threadContextBean.getCdi();
        assertThat(value.get(), notNullValue());
    }

    @Test
    public void testAsyncGetBeanManagerViaJndi() throws Exception {
        Future<BeanManager> value = threadContextBean.getBeanManagerViaJndi();
        assertThat(value.get(), notNullValue());
    }

    @Test
    public void testAsyncTccl() throws Exception {
        Future<Class<?>> value = threadContextBean.loadClassWithTccl();
        assertThat(value.get(), notNullValue());
    }

}
