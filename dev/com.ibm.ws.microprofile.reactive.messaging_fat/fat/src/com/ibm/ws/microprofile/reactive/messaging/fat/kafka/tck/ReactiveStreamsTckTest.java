/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package com.ibm.ws.microprofile.reactive.messaging.fat.kafka.tck;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsEngineResolver;
import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsFactoryImpl;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsFactoryResolver;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testng.ITestNGListener;
import org.testng.TestNG;
import org.testng.reporters.JUnitXMLReporter;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import io.smallrye.reactive.streams.Engine;

/**
 * Runs the Reactive Streams 1.0 TCK against our Kafka connector
 * <p>
 * The Reactive Streams TCK is written using TestNG, so this test is a wrapper which launches TestNG to run the tests.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ReactiveStreamsTckTest {

    @BeforeClass
    public static void setup() {
        ReactiveStreamsFactoryResolver.setInstance(new ReactiveStreamsFactoryImpl());
        ReactiveStreamsEngineResolver.setInstance(new Engine());
    }

    @AfterClass
    public static void teardown() {
        ReactiveStreamsFactoryResolver.setInstance(null);
        ReactiveStreamsEngineResolver.setInstance(null);
    }

    @Test
    public void runTck() {

        // Launch TestNG so that it runs our test classes, logs to stdout and produces JUnit test reports named as the FAT framework expects

        ITestNGListener junitReporter = new JUnitXMLReporter();
        ITestNGListener loggingReporter = new LoggingReporter();

        List<Class<?>> testClasses = Arrays.asList(KafkaPublisherVerification.class,
                                                   KafkaSubscriberVerification.class);

        TestNG testNg = new TestNG(false);
        testNg.setXmlSuites(Collections.singletonList(createSuiteForTestClasses(testClasses)));
        testNg.addListener(junitReporter);
        testNg.addListener(loggingReporter);
        testNg.setOutputDirectory("results/junit");
        testNg.run();
    }

    /**
     * Creates a test suite to run the given classes
     * <p>
     * The suite is structured so that the JUnit result files that it outputs matches what the FAT framework expects
     *
     * @param testClasses the TestNG test classes
     * @return the TestNG suite
     */
    private XmlSuite createSuiteForTestClasses(List<Class<?>> testClasses) {
        XmlSuite suite = new XmlSuite();
        // Ignore below, a new testng requires suit names
        suite.setName("ReactiveStreamsTckTest"); // No suite name so that result files are not put in a subdirectory

        for (Class<?> testClass : testClasses) {
            XmlTest test = new XmlTest(suite);
            test.setName("TEST-" + testClass.getName()); // Use name format that the FAT framework expects
            test.setClasses(Collections.singletonList(new XmlClass(testClass)));
        }

        return suite;
    }

}
