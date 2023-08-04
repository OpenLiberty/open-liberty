/*******************************************************************************
 * Copyright (c) 2018, 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
//This file is based on https://github.com/eclipse/microprofile-reactive-streams-operators/blob/1.0/tck/src/main/java/org/eclipse/microprofile/reactive/streams/operators/tck/arquillian/ReactiveStreamsArquillianTck.java
//It has been modified to be compatible with version 7.5.1 of TestNG. That change has been submitted upstream in https://github.com/eclipse/microprofile-reactive-streams-operators/pull/177
package org.eclipse.microprofile.reactive.streams.operators.tck.arquillian;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.reactive.streams.operators.tck.ReactiveStreamsTck;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.reactivestreams.example.unicast.AsyncIterablePublisher;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.IClassListener;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestObjectFactory;
import org.testng.ITestResult;
import org.testng.TestNG;
import org.testng.annotations.Test;

import javax.inject.Inject;

/**
 * Test runner for running the TCK in Arquillian.
 * <p>
 * To test your implementation in a MicroProfile container you need to:
 * <ol>
 * <li>Expose your {@code org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine} implementation
 * as an {@code ApplicationScoped} bean,</li>
 * <li>Define an Arquillian test /deployment containing the bean exposing the engine</li>
 * <li>Have an integration test extending
 * {@code org.eclipse.microprofile.reactive.streams.operators.tck.arquillian.ReactiveStreamsArquillianTck}</li>
 * </ol>
 */
public class ReactiveStreamsArquillianTck extends Arquillian {
    @Deployment
    public static JavaArchive tckDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                        // Add everything from the TCK
                        .addPackages(true, ReactiveStreamsTck.class.getPackage())
                        // And add the reactive streams TCK
                        .addPackages(true, TestEnvironment.class.getPackage())
                        .addPackages(true, AsyncIterablePublisher.class.getPackage())
                        // And we need a CDI descriptor
                        .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

    }

    @Inject
    private ReactiveStreamsCdiTck tck;

  //Update to work with TestNG 7.5.1
    private class TCKObjectFactory implements ITestObjectFactory {
        @Override
        public <T> T newInstance(Class<T> cls, Object... parameters) {
            if (cls.equals(ReactiveStreamsCdiTck.class)) {
                return (T) tck;
            } else {
                return ITestObjectFactory.super.newInstance(cls, parameters);
            }
        }

        @Override
        public <T> T newInstance(String clsName, Object... parameters) {
            if (clsName.equals(ReactiveStreamsCdiTck.class.getName())) {
                return (T) tck;
            } else {
                return ITestObjectFactory.super.newInstance(clsName, parameters);
            }
        }

        @Override
        public <T> T newInstance(Constructor<T> constructor, Object... parameters) {
            if (constructor.getDeclaringClass().equals(ReactiveStreamsCdiTck.class)) {
                return (T) tck;
            } else {
                return ITestObjectFactory.super.newInstance(constructor, parameters);
            }
        }
    }

    @Test
    public void runAllTckTests() throws Throwable {
        TestNG testng = new TestNG();

      //Update to work with TestNG 7.5.1
        testng.setObjectFactory(new TCKObjectFactory());

        testng.setUseDefaultListeners(false);
        ResultListener resultListener = new ResultListener();
        testng.addListener(resultListener);
        testng.setTestClasses(new Class[] { ReactiveStreamsCdiTck.class });
        testng.setMethodInterceptor(new IMethodInterceptor() {
            @Override
            public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
                methods.sort(Comparator.comparing(m -> m.getInstance().getClass().getName()));
                return methods;
            }
        });
        testng.run();
        int total = resultListener.success.get() + resultListener.failed.get() + resultListener.skipped.get();
        System.out.println(
                           String.format("Ran %d tests, %d passed, %d failed, %d skipped.", total, resultListener.success.get(),
                                         resultListener.failed.get(), resultListener.skipped.get()));
        System.out.println("Failed tests:");
        resultListener.failures.forEach(result -> {
            System.out.println(result.getInstance().getClass().getName() + "." + result.getMethod().getMethodName());
        });
        if (resultListener.failed.get() > 0) {
            if (resultListener.lastFailure.get() != null) {
                throw resultListener.lastFailure.get();
            } else {
                throw new Exception("Tests failed with no exception");
            }
        }
    }

    private static class ResultListener implements IClassListener, ITestListener {
        private final AtomicInteger success = new AtomicInteger();
        private final AtomicInteger failed = new AtomicInteger();
        private final AtomicInteger skipped = new AtomicInteger();
        private final AtomicReference<Throwable> lastFailure = new AtomicReference<>();
        private final List<ITestResult> failures = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void onBeforeClass(ITestClass testClass) {
            System.out.println(testClass.getName() + ":");
        }

        @Override
        public void onAfterClass(ITestClass testClass) {}

        @Override
        public void onTestStart(ITestResult result) {}

        @Override
        public void onTestSuccess(ITestResult result) {
            printResult(result, "SUCCESS");
            success.incrementAndGet();
        }

        @Override
        public void onTestFailure(ITestResult result) {
            printResult(result, "FAILED");
            if (result.getThrowable() != null) {
                result.getThrowable().printStackTrace(System.out);
                lastFailure.set(result.getThrowable());
            }
            failures.add(result);
            failed.incrementAndGet();
        }

        @Override
        public void onTestSkipped(ITestResult result) {
            printResult(result, "SKIPPED");
            skipped.incrementAndGet();
        }

        @Override
        public void onTestFailedButWithinSuccessPercentage(ITestResult result) {}

        @Override
        public void onStart(ITestContext context) {}

        @Override
        public void onFinish(ITestContext context) {

        }

        private static void printResult(ITestResult result, String status) {
            String methodName = String.format("%-100s", result.getMethod().getMethodName()).replace(' ', '.');
            System.out.println(" - " + methodName + "." + status);
        }
    }
}