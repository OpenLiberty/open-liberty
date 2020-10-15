/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.utility.tasks;

import static org.junit.Assert.assertNotNull;

import java.io.PrintStream;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.utility.utils.ConsoleWrapper;
import com.ibm.ws.security.utility.utils.StringStartsWithMatcher;

/**
 *
 */
public class TLSProfilerTaskTest {
    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final ConsoleWrapper stdin = mock.mock(ConsoleWrapper.class, "stdin");
    private final PrintStream stdout = mock.mock(PrintStream.class, "stdout");
    private final PrintStream stderr = mock.mock(PrintStream.class, "stderr");
    private TLSProfilerTask tlsprofiler;
    private final String host = "--host=ibm.com";
    private final String port = "--port=443";
    private final String verbose = "--verbose";

    @Before
    public void setUp() {
        tlsprofiler = new TLSProfilerTask("myScript");
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    @Factory
    public static Matcher<String> aStringStartsWith(String prefix) {
        return new StringStartsWithMatcher(prefix);
    }

    @Test
    public void getTaskHelp() {
        assertNotNull(tlsprofiler.getTaskHelp());
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.TLSProfiler#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_HostAndPortArgs() {
        mock.checking(new Expectations() {
            {
                one(stdout).println(with(aStringStartsWith("Successful handshakes to the target host and port")));
                allowing(stdout).println(with(aStringStartsWith("TLS")));
            }
        });
        String[] args = { "tlsprofiler", host, port };
        try {
            tlsprofiler.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {

        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.TLSProfiler#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    //Not sure we really need this test for adding --verbose. It adds an extra 90 seconds to the test time.
    //@Test
    public void handleTask_HostPortVerboseArgs() {
        mock.checking(new Expectations() {
            {
                one(stdout).println(with(aStringStartsWith("Successful handshakes to the target host and port")));
                allowing(stdout).println(with(aStringStartsWith("TLS")));
                one(stdout).println(with(aStringStartsWith("Unsuccessful handshakes to the target host and port")));
                allowing(stdout).println(with(aStringStartsWith("TLS")));
                allowing(stdout).println(with(aStringStartsWith("WARNING:")));
                allowing(stdout).println(with(aStringStartsWith("TLS")));
                allowing(stdout).println(with(aStringStartsWith("WARNING:")));
                allowing(stdout).println(with(aStringStartsWith("TLS")));
            }
        });
        String[] args = { "tlsprofiler", host, port, verbose };
        try {
            tlsprofiler.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {

        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.TLSProfiler#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_missingArgText() throws Exception {
        String[] args = { "tlsprofiler", host };
        try {
            tlsprofiler.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;
        }

    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.TLSProfiler#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_multiArgText() throws Exception {
        String[] args = { "tlsprofiler", host, port, "extraArg" };
        try {
            tlsprofiler.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;
        }

    }

}
