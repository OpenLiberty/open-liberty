/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.utility.tasks;

import static org.junit.Assert.assertEquals;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import com.ibm.ws.security.utility.SecurityUtilityReturnCodes;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;

/**
 *
 */
public class BaseCommandTaskTest {

    private final String TEXT_TO_READ = "readme!";

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final Sequence interactiveSequence = mock.sequence("passwordSequence");
    private final ConsoleWrapper stdin = mock.mock(ConsoleWrapper.class, "stdin");
    private final PrintStream stdout = mock.mock(PrintStream.class, "stdout");

    static class TestTask extends BaseCommandTask {
        public static final String ARG_REQ = "--req";
        public static final String ARG_OPT = "--opt";
        public static List<String> FLAG_ARGS = createFlagsArgs();

        public static List<String> createFlagsArgs() {
            return Arrays.asList(new String[] { TestTask.ARG_REQ });
        }

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
        public SecurityUtilityReturnCodes handleTask(ConsoleWrapper stdin, PrintStream stdout, PrintStream stderr, String[] args) throws Exception {
            return SecurityUtilityReturnCodes.OK;
        }

        @Override
        boolean isKnownArgument(String arg) {
            return ARG_REQ.equals(arg) || ARG_OPT.equals(arg);
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
                throw new IllegalArgumentException("Missing required arg " + ARG_REQ);
            }
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#promptForPassword(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void promptForPassword_promptWhenNoConsole() throws Exception {
        TestTask task = new TestTask();

        mock.checking(new Expectations() {
            {
                one(stdin).readMaskedText("Enter password: ");
                will(returnValue(null));
                one(stdin).readMaskedText("Re-enter password: ");
                will(returnValue(null));
            }
        });
        try {
            task.promptForPassword(stdin, stdout);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#promptForPassword(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream)}.
     */
    @Test
    public void promptForPassword_promptSuppliedMatch() {
        TestTask task = new TestTask();

        mock.checking(new Expectations() {
            {
                one(stdin).readMaskedText("Enter password: ");
                will(returnValue(TEXT_TO_READ));
                one(stdin).readMaskedText("Re-enter password: ");
                will(returnValue(TEXT_TO_READ));
            }
        });

        assertEquals("FAIL: Read-in password was not as expected",
                     TEXT_TO_READ, task.promptForPassword(stdin, stdout));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#promptForPassword(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream)}.
     */
    @Test
    public void promptForPassword_promptSuppliedErrorRetry() {
        TestTask task = new TestTask();

        mock.checking(new Expectations() {
            {
                one(stdin).readMaskedText("Enter password: ");
                inSequence(interactiveSequence);
                will(returnValue(TEXT_TO_READ));

                one(stdin).readMaskedText("Re-enter password: ");
                inSequence(interactiveSequence);
                will(returnValue(null));

                one(stdout).println("Error reading in password.");

                one(stdin).readMaskedText("Enter password: ");
                inSequence(interactiveSequence);
                will(returnValue(TEXT_TO_READ));

                one(stdin).readMaskedText("Re-enter password: ");
                inSequence(interactiveSequence);
                will(returnValue(TEXT_TO_READ));
            }
        });

        assertEquals("FAIL: Read-in password was not as expected",
                     TEXT_TO_READ, task.promptForPassword(stdin, stdout));

    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#promptForPassword(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream)}.
     */
    @Test
    public void promptForPassword_promptSuppliedDoNotMatch() {
        TestTask task = new TestTask();

        mock.checking(new Expectations() {
            {
                one(stdin).readMaskedText("Enter password: ");
                inSequence(interactiveSequence);
                will(returnValue(TEXT_TO_READ));

                one(stdin).readMaskedText("Re-enter password: ");
                inSequence(interactiveSequence);
                will(returnValue("iDontMatch"));

                one(stdout).println("Passwords did not match.");

                one(stdin).readMaskedText("Enter password: ");
                inSequence(interactiveSequence);
                will(returnValue(TEXT_TO_READ));

                one(stdin).readMaskedText("Re-enter password: ");
                inSequence(interactiveSequence);
                will(returnValue(TEXT_TO_READ));
            }
        });

        assertEquals("FAIL: Read-in password was not as expected",
                     TEXT_TO_READ, task.promptForPassword(stdin, stdout));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#promptForText(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void promptForText_promptWhenNoConsole() throws Exception {
        TestTask task = new TestTask();

        mock.checking(new Expectations() {
            {
                one(stdin).readMaskedText("Enter text: ");
                will(returnValue(null));
                one(stdin).readMaskedText("Re-enter text: ");
                will(returnValue(null));
            }
        });
        try {
            task.promptForText(stdin, stdout);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#promptForText(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream)}.
     */
    @Test
    public void promptForText_promptSuppliedMatch() {
        TestTask task = new TestTask();

        mock.checking(new Expectations() {
            {
                one(stdin).readMaskedText("Enter text: ");
                will(returnValue(TEXT_TO_READ));
                one(stdin).readMaskedText("Re-enter text: ");
                will(returnValue(TEXT_TO_READ));
            }
        });

        assertEquals("FAIL: Read-in text was not as expected",
                     TEXT_TO_READ, task.promptForText(stdin, stdout));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#promptForText(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream)}.
     */
    @Test
    public void promptForText_promptSuppliedErrorRetry() {
        TestTask task = new TestTask();

        mock.checking(new Expectations() {
            {
                one(stdin).readMaskedText("Enter text: ");
                inSequence(interactiveSequence);
                will(returnValue(TEXT_TO_READ));

                one(stdin).readMaskedText("Re-enter text: ");
                inSequence(interactiveSequence);
                will(returnValue(null));

                one(stdout).println("Error reading in text.");

                one(stdin).readMaskedText("Enter text: ");
                inSequence(interactiveSequence);
                will(returnValue(TEXT_TO_READ));

                one(stdin).readMaskedText("Re-enter text: ");
                inSequence(interactiveSequence);
                will(returnValue(TEXT_TO_READ));
            }
        });

        assertEquals("FAIL: Read-in text was not as expected",
                     TEXT_TO_READ, task.promptForText(stdin, stdout));

    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#promptForText(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream)}.
     */
    @Test
    public void promptForText_promptSuppliedDoNotMatch() {
        TestTask task = new TestTask();

        mock.checking(new Expectations() {
            {
                one(stdin).readMaskedText("Enter text: ");
                inSequence(interactiveSequence);
                will(returnValue(TEXT_TO_READ));

                one(stdin).readMaskedText("Re-enter text: ");
                inSequence(interactiveSequence);
                will(returnValue("iDontMatch"));

                one(stdout).println("Entries did not match.");

                one(stdin).readMaskedText("Enter text: ");
                inSequence(interactiveSequence);
                will(returnValue(TEXT_TO_READ));

                one(stdin).readMaskedText("Re-enter text: ");
                inSequence(interactiveSequence);
                will(returnValue(TEXT_TO_READ));
            }
        });

        assertEquals("FAIL: Read-in text was not as expected",
                     TEXT_TO_READ, task.promptForText(stdin, stdout));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#getArgumentValue(String, String[], String, String, ConsoleWrapper, PrintStream)}.
     */
    @Test
    public void getArgumentValue_defaultValue() throws Exception {
        TestTask task = new TestTask();

        final String defaultValue = "default";
        String value = task.getArgumentValue("--arg", new String[] { "testUtil", "--myArg" }, defaultValue, null, stdin, stdout);
        assertEquals("FAIL: The argument was not specified and the default value was not returned",
                     defaultValue, value);
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#getArgumentValue(String, String[], String, String, ConsoleWrapper, PrintStream)}.
     */
    @Test
    public void getArgumentValue_nearlyMatchingKey() throws Exception {
        TestTask task = new TestTask();

        final String defaultValue = "default";
        String value = task.getArgumentValue("--arg", new String[] { "testUtil", "--arg2" }, defaultValue, null, stdin, stdout);
        assertEquals("FAIL: The argument was not specified and the default value was not returned",
                     defaultValue, value);
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#getArgumentValue(String, String[], String, String, ConsoleWrapper, PrintStream)}.
     */
    @Test
    public void getArgumentValue_setValue() throws Exception {
        TestTask task = new TestTask();

        String value = task.getArgumentValue("--arg", new String[] { "testUtil", "--arg=2" }, "should_not_be_returned", null, stdin, stdout);
        assertEquals("FAIL: The argument was not specified and the default value was not returned",
                     "2", value);
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#getArgumentValue(String, String[], String, String, ConsoleWrapper, PrintStream)}.
     */
    @Test
    public void getArgumentValue_promptValue() throws Exception {
        TestTask task = new TestTask();

        mock.checking(new Expectations() {
            {
                one(stdin).readMaskedText("Enter password: ");
                will(returnValue(TEXT_TO_READ));
                one(stdin).readMaskedText("Re-enter password: ");
                will(returnValue(TEXT_TO_READ));
            }
        });

        String value = task.getArgumentValue("--arg", new String[] { "testUtil", "--arg" }, "should_not_be_returned", "--arg", stdin, stdout);
        assertEquals("FAIL: The argument was not specified and the default value was not returned",
                     TEXT_TO_READ, value);
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#validateArgumentList(String[], java.util.List)}.
     */
    @Test
    public void validateArgumentList_required() throws Exception {
        TestTask task = new TestTask();

        String[] args = new String[] { "testUtility", TestTask.ARG_REQ + "=val1" };
        task.validateArgumentList(args, TestTask.FLAG_ARGS);
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#validateArgumentList(String[], java.util.List)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void validateArgumentList_requiredAndUknown() throws Exception {
        TestTask task = new TestTask();

        String[] args = new String[] { "testUtility", TestTask.ARG_REQ + "=val1", "--unknown" };
        task.validateArgumentList(args, TestTask.FLAG_ARGS);
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#validateArgumentList(String[], java.util.List)}.
     */
    @Test
    public void validateArgumentList_requiredAndOptional() throws Exception {
        TestTask task = new TestTask();

        String[] args = new String[] { "testUtility", TestTask.ARG_REQ + "=val1", TestTask.ARG_OPT + "=val2" };
        task.validateArgumentList(args, TestTask.FLAG_ARGS);
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#validateArgumentList(String[], java.util.List)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void validateArgumentList_optionalOnly() throws Exception {
        TestTask task = new TestTask();

        String[] args = new String[] { "testUtility", TestTask.ARG_OPT + "=val2" };
        task.validateArgumentList(args, TestTask.FLAG_ARGS);
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#validateArgumentList(String[], java.util.List)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void validateArgumentList_requiredAndOptionalNoValue() throws Exception {
        TestTask task = new TestTask();

        String[] args = new String[] { "testUtility", TestTask.ARG_REQ + "=val1", TestTask.ARG_OPT };
        task.validateArgumentList(args, TestTask.FLAG_ARGS);
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.BaseCommandTask#validateArgumentList(String[], java.util.List)}.
     */
    @Test
    public void validateArgumentList_flagOnlyArg() throws Exception {
        TestTask task = new TestTask();

        String[] args = new String[] { "testUtility", TestTask.ARG_REQ };
        task.validateArgumentList(args, TestTask.FLAG_ARGS);
    }

}
