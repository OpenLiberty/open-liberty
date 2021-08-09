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

import java.util.HashSet;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.product.utility.extension.HelpCommandTask;

public class HelpCommandTaskTest {

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final CommandConsole commandConsole = mock.mock(CommandConsole.class, "commandConsole");

    private final CommandTaskRegistry commandTaskRegistry = new CommandTaskRegistry();

    @Before
    public void setUp() {
        commandTaskRegistry.registerCommandTask("test", TestCommandTask.class);
    }

    @After
    public void tearDown() {
        commandTaskRegistry.clear();
    }

    @Test
    public void testEmptyArguments() {
        ExecutionContextImpl context = createExecutionContext(new String[0]);
        mock.checking(new Expectations() {
            {
                one(commandConsole).printlnInfoMessage("");
                one(commandConsole).printlnInfoMessage("Usage: testScript {testTaskName} [options]");
                one(commandConsole).printlnInfoMessage(CommandConstants.LINE_SEPARATOR + "Actions:" + CommandConstants.LINE_SEPARATOR + CommandConstants.LINE_SEPARATOR
                                                       + "    " + "testTaskName" + CommandConstants.LINE_SEPARATOR + "testTaskDescription"
                                                       + CommandConstants.LINE_SEPARATOR + CommandConstants.LINE_SEPARATOR + "Options:"
                                                       + CommandConstants.LINE_SEPARATOR
                                                       + "\tUse help [actionName] for detailed option information of each action.");
            }
        });
        new HelpCommandTask().execute(context);
    }

    @Test
    public void testTaskName() {
        ExecutionContextImpl context = createExecutionContext(new String[] { "test" });
        mock.checking(new Expectations() {
            {
                one(commandConsole).printlnInfoMessage("testTaskHelp");
                one(commandConsole).printlnInfoMessage("");
            }
        });
        new HelpCommandTask().doExecute(context);
    }

    @Test
    public void testInvalidTaskName() {
        ExecutionContextImpl context = createExecutionContext(new String[] { "invalidTaskName" });
        mock.checking(new Expectations() {
            {
                one(commandConsole).printlnErrorMessage("");
                one(commandConsole).printlnErrorMessage("CWWKE0502E: Unknown task: invalidTaskName.");
            }
        });
        new HelpCommandTask().doExecute(context);
    }

    private ExecutionContextImpl createExecutionContext(String[] args) {
        ExecutionContextImpl context = new ExecutionContextImpl(commandConsole, args, commandTaskRegistry);
        context.setAttribute(CommandConstants.SCRIPT_NAME, "testScript");
        return context;
    }

    public static class TestCommandTask implements CommandTask {
        /** {@inheritDoc} */
        @Override
        public Set<String> getSupportedOptions() {
            return new HashSet<String>();
        }

        /** {@inheritDoc} */
        @Override
        public String getTaskName() {
            return "testTaskName";
        }

        /** {@inheritDoc} */
        @Override
        public String getTaskDescription() {
            return "testTaskDescription";
        }

        /** {@inheritDoc} */
        @Override
        public String getTaskHelp() {
            return "testTaskHelp";
        }

        /** {@inheritDoc} */
        @Override
        public void execute(ExecutionContext context) {}

    }
}
