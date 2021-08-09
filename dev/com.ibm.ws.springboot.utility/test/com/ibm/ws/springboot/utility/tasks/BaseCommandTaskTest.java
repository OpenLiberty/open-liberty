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
package com.ibm.ws.springboot.utility.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.PrintStream;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import com.ibm.ws.springboot.utility.SpringBootUtilityReturnCodes;
import com.ibm.ws.springboot.utility.utils.ConsoleWrapper;

/**
 *
 */
public class BaseCommandTaskTest {

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final ConsoleWrapper stdin = mock.mock(ConsoleWrapper.class, "stdin");
    private final PrintStream stdout = mock.mock(PrintStream.class, "stdout");

    static class TestTask extends BaseCommandTask {
        public static final String ARG_REQ = "--req";
        public static final String ARG_OPT = "--opt";

        public TestTask() {
            super("testUtil");
        }

        @Override
        public String getTaskName() {
            return null;
        }

        @Override
        public String getTaskHelp() {
            return null;
        }

        @Override
        public String getTaskDescription() {
            return null;
        }

        @Override
        public SpringBootUtilityReturnCodes handleTask(ConsoleWrapper stdin, PrintStream stdout, PrintStream stderr, String[] args) throws Exception {
            return SpringBootUtilityReturnCodes.OK;
        }

        @Override
        boolean isKnownArgument(String arg) {
            return ARG_OPT.equals(arg) || ARG_REQ.equals(arg);
        }

        @Override
        void checkRequiredArguments(String[] args) throws IllegalArgumentException {
            boolean foundReq = false;
            for (int i = 1; i < args.length; i++) {
                String arg = args[i].split("=")[0];
                if (ARG_REQ.equals(arg)) {
                    foundReq = true;
                }
            }
            if (!foundReq) {
                throw new IllegalArgumentException("Missing required arg " + ARG_OPT);
            }
        }
    }

    @Test
    public void getArgumentValue_nullValue() throws Exception {
        TestTask task = new TestTask();

        String value = task.getArgumentValue("--arg", new String[] { "testUtil", "--myArg=testValue" }, stdin, stdout);
        assertNull("FAIL: The argument was not specified and the default value was not returned", value);
    }

    @Test
    public void getArgumentValue_nearlyMatchingKey() throws Exception {
        TestTask task = new TestTask();

        String value = task.getArgumentValue("--arg", new String[] { "testUtil", "--arg2=testValue" }, stdin, stdout);
        assertNull("FAIL: The argument was not specified and the default value was not returned", value);
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#getArgumentValue(String, String[], String, String, ConsoleWrapper, PrintStream)}.
     */
    @Test
    public void getArgumentValue_setValue() throws Exception {
        TestTask task = new TestTask();

        String value = task.getArgumentValue("--arg", new String[] { "testUtil", "--arg=2" }, stdin, stdout);
        assertEquals("FAIL: The argument was specified and wrong value was returned",
                     "2", value);
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#validateArgumentList(String[], java.util.List)}.
     */
    @Test
    public void validateArgumentList_required() throws Exception {
        TestTask task = new TestTask();

        String[] args = new String[] { "testUtility", TestTask.ARG_REQ + "=val1" };
        task.validateArgumentList(args);
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#validateArgumentList(String[], java.util.List)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void validateArgumentList_requiredAndUknown() throws Exception {
        TestTask task = new TestTask();

        String[] args = new String[] { "testUtility", TestTask.ARG_REQ + "=val1", "--unknown" };
        task.validateArgumentList(args);
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#validateArgumentList(String[], java.util.List)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void validateArgumentList_optionalOnly() throws Exception {
        TestTask task = new TestTask();

        String[] args = new String[] { "testUtility", TestTask.ARG_OPT + "=val2" };
        task.validateArgumentList(args);
    }

}
