/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.utility;

import java.io.PrintStream;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.springboot.utility.tasks.ThinAppTask;
import com.ibm.ws.springboot.utility.utils.ConsoleWrapper;

/**
 *
 */
public class SpringBootUtilityTest {
    private static final String NL = System.getProperty("line.separator");
    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final ConsoleWrapper stdin = mock.mock(ConsoleWrapper.class, "stdin");
    private final PrintStream stdout = mock.mock(PrintStream.class, "stdout");
    private final PrintStream stderr = mock.mock(PrintStream.class, "stderr");
    private final SpringBootUtilityTask task = mock.mock(SpringBootUtilityTask.class);
    private SpringBootUtility util;

    @Before
    public void setUp() {
        util = new SpringBootUtility(stdin, stdout, stderr);
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    /**
     * Verify that any missing I/O streams will not result in catastrophic
     * failure.
     */
    @Test
    public void nullStdin() {
        mock.checking(new Expectations() {
            {
                one(stderr).println(with(any(String.class)));
            }
        });
        SpringBootUtility util = new SpringBootUtility(null, stdout, stderr);
        util.runProgram(null);
    }

    /**
     * Verify that a missing Console streams will not result in catastrophic
     * failure.
     */
    @Test
    public void unavailableConsole() {
        final ConsoleWrapper stdin = mock.mock(ConsoleWrapper.class, "override_stdin");
        mock.checking(new Expectations() {
            {
                allowing(stdin).isInputStreamAvailable();
                will(returnValue(false));
                allowing(stdout).println(with(any(String.class)));
            }
        });
        SpringBootUtility util = new SpringBootUtility(stdin, stdout, stderr);
        util.runProgram(new String[] {});
    }

    /**
     * Verify that any missing I/O streams will not result in catastrophic
     * failure.
     */
    @Test
    public void nullStdout() {
        mock.checking(new Expectations() {
            {
                one(stderr).println(with(any(String.class)));
            }
        });
        SpringBootUtility util = new SpringBootUtility(stdin, null, stderr);
        util.runProgram(null);
    }

    /**
     * Verify that any missing I/O streams will not result in catastrophic
     * failure.
     */
    @Test
    public void nullStderr() {
        mock.checking(new Expectations() {
            {
                one(stdout).println(with(any(String.class)));
            }
        });
        SpringBootUtility util = new SpringBootUtility(stdin, stdout, null);
        util.runProgram(null);
    }

    /**
     * Verify that no arguments results in usage. Cover the case
     * where no tasks are registered.
     */
    @Test
    public void noArgumentsDrivesUsageWithNoTasks() {
        final String expectedMsg = NL + "Usage: " + SpringBootUtility.SCRIPT_NAME +
                                   " {help} [options]" + NL;
        mock.checking(new Expectations() {
            {
                one(stdout).println(expectedMsg);
            }
        });

        util.runProgram(new String[] {});
    }

    /**
     * Verify that no arguments results in usage.
     */
    @Test
    public void noArgumentsDrivesUsageWithKnownTasks() {
        final String expectedMsg = NL + "Usage: " + SpringBootUtility.SCRIPT_NAME +
                                   " {thin|help} [options]" + NL;
        mock.checking(new Expectations() {
            {
                one(stdout).println(expectedMsg);
            }
        });

        util.registerTask(new ThinAppTask(null, SpringBootUtility.SCRIPT_NAME));
        util.runProgram(new String[] {});
    }

    /**
     * Verify that no arguments results in usage.
     */
    @Test
    public void taskArgumentUnknownPrintsUsage() {
        final String unknownMsg = "Unknown task: unknown";
        final String usageMsg = NL + "Usage: " + SpringBootUtility.SCRIPT_NAME +
                                " {help} [options]" + NL;
        mock.checking(new Expectations() {
            {
                one(stderr).println(unknownMsg);
                one(stderr).println(usageMsg);
            }
        });

        util.runProgram(new String[] { "unknown" });
    }

    @Test
    public void taskArgumentDrivesLogic() {
        final String[] args = new String[] { "mockTask" };
        mock.checking(new Expectations() {
            {
                one(task).getTaskName();
                will(returnValue("mockTask"));
                try {
                    one(task).handleTask(stdin, stdout, stderr, args);
                } catch (Exception e) {

                }
            }
        });

        util.registerTask(task);
        util.runProgram(args);
    }

    @Test
    public void taskArgumentDrivesLogic_errorPath() {
        final String[] args = new String[] { "mockTask" };
        mock.checking(new Expectations() {
            {
                one(task).getTaskName();
                will(returnValue("mockTask"));
                try {
                    one(task).handleTask(stdin, stdout, stderr, args);
                    will(throwException(new IllegalArgumentException("bad args")));
                } catch (Exception e) {

                }
                one(stderr).println("");
                one(stderr).println("Error: bad args");
                one(task).getTaskHelp();
                will(returnValue("detailed help"));
                one(stderr).println(NL + NL + "detailed help");
            }
        });

        util.registerTask(task);
        util.runProgram(args);
    }

}
