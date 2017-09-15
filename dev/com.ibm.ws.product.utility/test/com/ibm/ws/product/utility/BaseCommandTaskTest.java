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
package com.ibm.ws.product.utility;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import junit.framework.Assert;

public class BaseCommandTaskTest {
    private static final String SUPPORTED_OPTION = "--supportedOption";
    private static final String BAD_PREFIX_OPTION = "attemptToSupportOptionWithBadPrefix";
    private static final String TOTALLY_UNSUPPORTED_OPTION = "GIBBERISH!$!^@$@%$";
    private static final String SUCCESS_MESSAGE = "SUCCESS!";

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final CommandConsole commandConsole = mock.mock(CommandConsole.class, "commandConsole");

    /*
     * The test command should only print a success message because no options should be detected.
     */
    @Test
    public void testNoOptions() {
        ExecutionContextImpl context = createExecutionContext(new String[] {});
        mock.checking(new Expectations() {
            {
                one(commandConsole).printlnInfoMessage(SUCCESS_MESSAGE);
            }
        });
        new TestCommandTask().execute(context);
    }

    /*
     * The test command should indicate that it found the supported option but it should not detect a
     * value for it.
     */
    @Test
    public void testSupportedOptionNoValue() {
        ExecutionContextImpl context = createExecutionContext(new String[] { SUPPORTED_OPTION });
        mock.checking(new Expectations() {
            {
                one(commandConsole).printlnInfoMessage("found" + SUPPORTED_OPTION);
                one(commandConsole).printlnInfoMessage(SUCCESS_MESSAGE);
            }
        });
        new TestCommandTask().execute(context);
    }

    /*
     * The test command should indicate that it found the supported option AND that it has an associated
     * value.
     */
    @Test
    public void testSupportedOptionWithValue() {
        ExecutionContextImpl context = createExecutionContext(new String[] { SUPPORTED_OPTION + "=test" });
        mock.checking(new Expectations() {
            {
                one(commandConsole).printlnInfoMessage("found" + SUPPORTED_OPTION);
                one(commandConsole).printlnInfoMessage("value=test");
                one(commandConsole).printlnInfoMessage(SUCCESS_MESSAGE);
            }
        });
        new TestCommandTask().execute(context);
    }

    /*
     * This one is a bit tricky.  The test command is *claiming* that it supports BAD_PREFIX_OPTION (look at the
     * "getSupportedOptions" method), but the expected result is that BAD_PREFIX_OPTION should not pass validation
     * and should result in an error message.  This is to protect against developers who add future command tasks and
     * who aren't aware that all options should be standardized and start with the "--" prefix.  They might code their
     * command task to support an option without the appropriate prefix, but the command will not actually work.
     */
    @Test
    public void testBadPrefixOption() {
        ExecutionContextImpl context = createExecutionContext(new String[] { SUPPORTED_OPTION + "=test", BAD_PREFIX_OPTION });
        mock.checking(new Expectations() {
            {
                one(commandConsole).printlnErrorMessage("CWWKE0514E: An invalid option, " + BAD_PREFIX_OPTION + ", was supplied on the command line.  Valid options are: [--output, " + SUPPORTED_OPTION + "]");
            }
        });
        new TestCommandTask().execute(context);
    }

    /**
     * Obviously, since the user is passing in a totally unsupported option, the test should complain about it.
     */
    @Test
    public void testTotallyUnsupportedOption() {
        ExecutionContextImpl context = createExecutionContext(new String[] { SUPPORTED_OPTION + "=test", TOTALLY_UNSUPPORTED_OPTION });
        mock.checking(new Expectations() {
            {
                one(commandConsole).printlnErrorMessage("CWWKE0514E: An invalid option, " + TOTALLY_UNSUPPORTED_OPTION + ", was supplied on the command line.  Valid options are: [--output, " + SUPPORTED_OPTION + "]");
            }
        });
        new TestCommandTask().execute(context);
    }

    private ExecutionContextImpl createExecutionContext(String[] args) {
        ExecutionContextImpl context = new ExecutionContextImpl(commandConsole, args, null);
        return context;
    }

    public static class TestCommandTask extends BaseCommandTask {
        /** {@inheritDoc} */
        @Override
        public Set<String> getSupportedOptions() {
            return new HashSet<String>(Arrays.asList(SUPPORTED_OPTION,
                                                     BAD_PREFIX_OPTION));
        }

        /** {@inheritDoc} */
        @Override
        public String getTaskName() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public String getTaskDescription() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public String getTaskHelp() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public void doExecute(ExecutionContext context) {
            if (context.optionExists(SUPPORTED_OPTION)) {
                context.getCommandConsole().printlnInfoMessage("found" + SUPPORTED_OPTION);
                String value = context.getOptionValue(SUPPORTED_OPTION);
                if (value != null && !value.isEmpty()) {
                    context.getCommandConsole().printlnInfoMessage("value=" + value);
                }
            }
            Assert.assertFalse("Option " + BAD_PREFIX_OPTION + " should not exist", context.optionExists(BAD_PREFIX_OPTION));
            context.getCommandConsole().printlnInfoMessage(SUCCESS_MESSAGE);
        }
    }
}
