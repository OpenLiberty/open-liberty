/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webserver.plugin.utility;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;

import java.io.PrintStream;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.webserver.plugin.utility.WebServerPluginUtility;
import com.ibm.ws.webserver.plugin.utility.tasks.GeneratePluginTask;
import com.ibm.ws.webserver.plugin.utility.tasks.MergePluginFilesTask;
import com.ibm.ws.webserver.plugin.utility.utils.CommandUtils;
import com.ibm.ws.webserver.plugin.utility.utils.ConsoleWrapper;
import com.ibm.ws.webserver.plugin.utility.utils.PluginUtilityConsole;

/**
 * This class will test the Plugin utility functions. Mainly this test class will
 * be used for tests that don't actually drive the merge or generate actions.
 *
 * For example testing parameter error conditions or tests of a more generic nature.
 *
 */
public class WebServerPluginUtilityTest {
    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final ConsoleWrapper stdin = mock.mock(ConsoleWrapper.class, "stdin");
    private final PrintStream stderr = mock.mock(PrintStream.class, "stderr");
    private final PluginUtilityConsole commandConsole = mock.mock(PluginUtilityConsole.class);
    private WebServerPluginUtility util;

    @Before
    public void setUp() {
        util = new WebServerPluginUtility(commandConsole);

        mock.checking(new Expectations() {
            {
                one(commandConsole).getStdin();
                will(returnValue(stdin));
                one(commandConsole).getStderr();
                will(returnValue(stderr));
            }
        });

        // register the generate plugin task
        util.registerTask(new GeneratePluginTask(WebServerPluginUtility.SCRIPT_NAME, commandConsole));
    }

    @After
    public void tearDown() {
        util = null;
        mock.assertIsSatisfied();
    }

    /*
     * generate --server=user:pass@:8080
     */
    @Test
    public void testPUGenerateMissingHost() {
        final String option = "--server";

        mock.checking(new Expectations() {
            {
                one(commandConsole).isStandardOutAvailable();
                will(returnValue(true));
                one(commandConsole).isStandardErrorAvailable();
                will(returnValue(true));
                one(commandConsole).printlnErrorMessage(with(equal("")));
                one(commandConsole).printlnErrorMessage(with(containsString(CommandUtils.getMessage("missingHostValue", option))));
                one(commandConsole).printlnInfoMessage(with(any(String.class)));
            }
        });

        String inputAddress = option + "=user:pass@:8080";
        util.runProgram(new String[] { "generate", inputAddress });
    }

    /*
     * generate --server=user:pass@testHost:
     */
    @Test
    public void testPUGenerateMissingPort() {
        final String option = "--server";

        mock.checking(new Expectations() {
            {
                one(commandConsole).isStandardOutAvailable();
                will(returnValue(true));
                one(commandConsole).isStandardErrorAvailable();
                will(returnValue(true));
                one(commandConsole).printlnErrorMessage(with(equal("")));
                one(commandConsole).printlnErrorMessage(with(containsString(CommandUtils.getMessage("missingPortValue", option))));
                one(commandConsole).printlnInfoMessage(with(any(String.class)));
            }
        });

        String inputAddress = option + "=user:pass@testHost:";
        util.runProgram(new String[] { "generate", inputAddress });
    }

    /*
     * generate --server=user:pass@testHost
     */
    @Test
    public void testPUGenerateMissingHostPort() {
        final String option = "--server";

        mock.checking(new Expectations() {
            {
                one(commandConsole).isStandardOutAvailable();
                will(returnValue(true));
                one(commandConsole).isStandardErrorAvailable();
                will(returnValue(true));
                one(commandConsole).printlnErrorMessage(with(equal("")));
                one(commandConsole).printlnErrorMessage(with(containsString(CommandUtils.getMessage("missingHostorPortValue", option))));
                one(commandConsole).printlnInfoMessage(with(any(String.class)));
            }
        });

        String inputAddress = option + "=user:pass@testHost";
        util.runProgram(new String[] { "generate", inputAddress });
    }

    /*
     * generate --server=user:pass@testHost:port
     */
    @Test
    public void testPUGenerateBadPort() {
        final String option = "--server";
        final String port = "BadPort";

        mock.checking(new Expectations() {
            {
                one(commandConsole).isStandardOutAvailable();
                will(returnValue(true));
                one(commandConsole).isStandardErrorAvailable();
                will(returnValue(true));
                one(commandConsole).printlnErrorMessage(with(equal("")));
                one(commandConsole).printlnErrorMessage(with(containsString(CommandUtils.getMessage("invalidPortArg", port, option))));
                one(commandConsole).printlnInfoMessage(with(any(String.class)));
            }
        });

        String inputAddress = option + "=user:pass@testHost:" + port;
        util.runProgram(new String[] { "generate", inputAddress });
    }

    /*
     * generate --collective=<user>:<password>@:<port> --cluster=<clusterName>
     */
    @Test
    public void testPUGenerateMissingHostCollective() {
        final String option = "--server";

        mock.checking(new Expectations() {
            {
                one(commandConsole).isStandardOutAvailable();
                will(returnValue(true));
                one(commandConsole).isStandardErrorAvailable();
                will(returnValue(true));
                one(commandConsole).printlnErrorMessage(with(equal("")));
                one(commandConsole).printlnErrorMessage(with(containsString(CommandUtils.getMessage("missingHostValue", option))));
                one(commandConsole).printlnInfoMessage(with(any(String.class)));
            }
        });

        String inputAddress = option + "=user:pass@:8080";
        String clusterName = "--cluster=abc";
        util.runProgram(new String[] { "generate", inputAddress, clusterName });
    }

    /*
     * generate --collective=<user>:<password>@<host>: --cluster=<clusterName>
     */
    @Test
    public void testPUGenerateMissingPortCollective() {
        final String option = "--server";

        mock.checking(new Expectations() {
            {
                one(commandConsole).isStandardOutAvailable();
                will(returnValue(true));
                one(commandConsole).isStandardErrorAvailable();
                will(returnValue(true));
                one(commandConsole).printlnErrorMessage(with(equal("")));
                one(commandConsole).printlnErrorMessage(with(containsString(CommandUtils.getMessage("missingPortValue", option))));
                one(commandConsole).printlnInfoMessage(with(any(String.class)));
            }
        });

        String inputAddress = option + "=user:pass@testHost:";
        String clusterName = "--cluster=abc";
        util.runProgram(new String[] { "generate", inputAddress, clusterName });
    }

    /*
     * generate --collective=<user>:<password>@ --cluster=<clusterName>
     */
    @Test
    public void testPUGenerateMissingHostPortCollective() {
        final String option = "--server";

        mock.checking(new Expectations() {
            {
                one(commandConsole).isStandardOutAvailable();
                will(returnValue(true));
                one(commandConsole).isStandardErrorAvailable();
                will(returnValue(true));
                one(commandConsole).printlnErrorMessage(with(equal("")));
                one(commandConsole).printlnErrorMessage(with(containsString(CommandUtils.getMessage("missingHostorPortValue", option))));
                one(commandConsole).printlnInfoMessage(with(any(String.class)));
            }
        });

        String inputAddress = option + "=user:pass@testHost";
        String clusterName = "--cluster=abc";
        util.runProgram(new String[] { "generate", inputAddress, clusterName });
    }

    /*
     * generate --collective=<user>:<password>@<host>:blah --cluster=<clusterName>
     */
    @Test
    public void testPUGenerateBadPortCollective() {
        final String option = "--server";
        final String port = "BadPort";

        mock.checking(new Expectations() {
            {
                one(commandConsole).isStandardOutAvailable();
                will(returnValue(true));
                one(commandConsole).isStandardErrorAvailable();
                will(returnValue(true));
                one(commandConsole).printlnErrorMessage(with(equal("")));
                one(commandConsole).printlnErrorMessage(with(containsString(CommandUtils.getMessage("invalidPortArg", port, option))));
                one(commandConsole).printlnInfoMessage(with(any(String.class)));
            }
        });

        String inputAddress = option + "=user:pass@testHost:" + port;
        String clusterName = "--cluster=abc";
        util.runProgram(new String[] { "generate", inputAddress, clusterName });
    }

    /**
     * This is a test to ensure that the following command works properly:
     * pluginUtility help
     *
     * @throws Exception
     */
    @Test
    public void testPluginUtilityHelp() throws Exception {

        mock.checking(new Expectations() {
            {
                one(commandConsole).isStandardOutAvailable();
                will(returnValue(true));
                one(commandConsole).isStandardErrorAvailable();
                will(returnValue(true));
                one(commandConsole).printInfoMessage(with(allOf(containsString(CommandUtils.getMessage("usage", WebServerPluginUtility.SCRIPT_NAME)),
                                                                containsString(CommandUtils.getOption("help.desc")))));
            }
        });

        int returnCode = util.runProgram(new String[] { "help" });

        assertEquals("pluginUtility task should complete with return code as 0.", 0, returnCode);
    }

    /**
     * This is a test to ensure that the following command results in an error condition:
     * pluginUtility merge
     *
     * @throws Exception
     */
    @Test
    public void testPluginUtilityMerge() throws Exception {
        final String argument = "--sourcePath";

        mock.checking(new Expectations() {
            {
                one(commandConsole).isStandardOutAvailable();
                will(returnValue(true));
                one(commandConsole).isStandardErrorAvailable();
                will(returnValue(true));
                one(commandConsole).printlnErrorMessage(with(equal("")));
                one(commandConsole).printlnErrorMessage(with(allOf(containsString(CommandUtils.getMessage("insufficientArgs")),
                                                                   containsString(CommandUtils.getMessage("missingArg", argument)))));
                allowing(commandConsole).printlnInfoMessage(with(any(String.class)));
            }
        });

        // register the merge plugin task
        util.registerTask(new MergePluginFilesTask(WebServerPluginUtility.SCRIPT_NAME));

        int returnCode = util.runProgram(new String[] { "merge" });

        assertEquals("pluginUtility task should complete with return code as 20.", 20, returnCode);
    }

}
