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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.PrintStream;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyFileUtility;
import com.ibm.ws.security.utility.IFileUtility;
import com.ibm.ws.security.utility.SecurityUtilityReturnCodes;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;

/**
 *
 */
public class CreateLTPAKeysTaskTest {

    private static final String TEST_UTILITY_NAME = "testUtility";
    private static final String EXPECTED_USR_SERVERS = "wlp/usr/servers/";

    private final String PASSWORD_PLAINTEXT = "Liberty";
    private final String PASSWORD_CIPHERTEXT = "{xor}EzY9Oi0rJg==";

    final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    final LTPAKeyFileUtility ltpaKeyFileUtil = mock.mock(LTPAKeyFileUtility.class);
    final IFileUtility fileUtil = mock.mock(IFileUtility.class);
    final ConsoleWrapper stdin = mock.mock(ConsoleWrapper.class, "stdin");
    final PrintStream stdout = mock.mock(PrintStream.class, "stdout");
    final PrintStream stderr = mock.mock(PrintStream.class, "stderr");

    @Factory
    public static Matcher<String> stringContaining(String... substrings) {
        return new StringContainsMatcher(substrings);
    }

    static class StringContainsMatcher extends TypeSafeMatcher<String> {
        private final String[] substrings;

        public StringContainsMatcher(String... substrings) {
            this.substrings = substrings;
        }

        @Override
        public boolean matchesSafely(String s) {
            boolean containsAll = true;
            for (String substring : substrings) {
                containsAll = containsAll & s.contains(substring);
            }
            return containsAll;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("a string containing ");
            for (String substring : substrings) {
                description.appendValue(substring);
            }
        }
    }

    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(fileUtil).getServersDirectory();
                will(returnValue(EXPECTED_USR_SERVERS));
            }
        });
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#getTaskName()}.
     */
    @Test
    public void getTaskName() {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        assertEquals("FAIL: The task did not have the expected task name",
                     "createLTPAKeys", task.getTaskName());
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#getTaskDescription()}.
     */
    @Test
    public void getTaskDescription() {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String desc = task.getTaskDescription();
        System.out.println(desc);

        assertTrue("FAIL: The task description did not contain the word 'LTPA'",
                   desc.contains("LTPA"));
        assertTrue("FAIL: The task description did not contain the word 'ltpa.keys'",
                   desc.contains("ltpa.keys"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#getTaskHelp()}.
     */
    @Test
    public void getTaskHelp() {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String help = task.getTaskHelp();
        System.out.println(help);

        assertTrue("FAIL: The task help did not contain the option '" + TEST_UTILITY_NAME + " createLTPAKeys'",
                   help.contains(TEST_UTILITY_NAME + " createLTPAKeys"));
        assertTrue("FAIL: The task help did not contain the word 'LTPA'",
                   help.contains("LTPA"));
        assertTrue("FAIL: The task help did not contain the word 'ltpa.keys'",
                   help.contains("ltpa.keys"));
        assertTrue("FAIL: The task help did not contain the option '--password'",
                   help.contains("--password"));
        assertTrue("FAIL: The task help did not contain the option '--server'",
                   help.contains("--server"));
        assertTrue("FAIL: The task help did not contain the option '--file'",
                   help.contains("--file"));
        assertTrue("FAIL: The task help did not contain the option '--passwordEncoding'",
                   help.contains("--passwordEncoding"));
        assertTrue("FAIL: The task help did not contain the option '--passwordKey'",
                   help.contains("--passwordKey"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#isKnownArgument(String)}
     */
    @Test
    public void isKnownArgument_password() throws Exception {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        assertTrue("FAIL: Did not recognize the --password flag",
                   task.isKnownArgument("--password"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#isKnownArgument(String)}
     */
    @Test
    public void isKnownArgument_server() throws Exception {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        assertTrue("FAIL: Did not recognize the --server flag",
                   task.isKnownArgument("--server"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#isKnownArgument(String)}
     */
    @Test
    public void isKnownArgument_file() throws Exception {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        assertTrue("FAIL: Did not recognize the --file flag",
                   task.isKnownArgument("--file"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#isKnownArgument(String)}
     */
    @Test
    public void isKnownArgument_passwordEncoding() throws Exception {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        assertTrue("FAIL: Did not recognize the --passwordEncoding flag",
                   task.isKnownArgument("--passwordEncoding"));
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#isKnownArgument(String)}
     */
    @Test
    public void isKnownArgument_passwordKey() throws Exception {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        assertTrue("FAIL: Did not recognize the --passwordKey flag",
                   task.isKnownArgument("--passwordKey"));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_noPassword() throws Exception {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "securityUtility" };

        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (IllegalArgumentException e) {
            assertTrue("FAIL: The thrown exception did not specify --password in its message",
                       e.getMessage().contains("--password"));
        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_incorrectPasswordFlag() throws Exception {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "securityUtility", "--passwords=Liberty" };

        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (IllegalArgumentException e) {
            assertTrue("FAIL: The thrown exception did not specify --password in its message",
                       e.getMessage().contains("--password"));
        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_providedPassword_fileExists() throws Exception {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "securityUtility", "--password=Liberty" };

        mock.checking(new Expectations() {
            {
                one(fileUtil).exists("ltpa.keys");
                will(returnValue(true));

                one(stdout).println(with(stringContaining("LTPA")));
                one(stdout).println(with(stringContaining("ltpa.keys")));
                never(stdout).println(with(stringContaining("<ltpa")));
            }
        });

        assertEquals("FAIL: The task did not report execution failed due to file exists",
                     SecurityUtilityReturnCodes.ERR_FILE_EXISTS,
                     task.handleTask(stdin, stdout, stderr, args));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_providedPassword_fileCreated() throws Exception {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "securityUtility", "--password=Liberty" };

        mock.checking(new Expectations() {
            {
                one(fileUtil).exists("ltpa.keys");
                will(returnValue(false));

                one(ltpaKeyFileUtil).createLTPAKeysFile(with("ltpa.keys"), with(any(byte[].class)));

                one(stdout).println(with(stringContaining("<ltpa", "ltpa.keys", PASSWORD_CIPHERTEXT)));
            }
        });

        assertEquals("FAIL: The task did not report execution OK",
                     SecurityUtilityReturnCodes.OK,
                     task.handleTask(stdin, stdout, stderr, args));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_promptPassword_fileExists() throws Exception {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "securityUtility", "--password" };

        mock.checking(new Expectations() {
            {
                one(fileUtil).exists("ltpa.keys");
                will(returnValue(true));

                one(stdout).println(with(stringContaining("LTPA")));
                one(stdout).println(with(stringContaining("ltpa.keys")));
                never(stdout).println(with(stringContaining("<ltpa")));
            }
        });

        assertEquals("FAIL: The task did not report execution failed due to file exists",
                     SecurityUtilityReturnCodes.ERR_FILE_EXISTS,
                     task.handleTask(stdin, stdout, stderr, args));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_promptPassword_fileCreated() throws Exception {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "securityUtility", "--password" };

        mock.checking(new Expectations() {
            {
                one(fileUtil).exists("ltpa.keys");
                will(returnValue(false));

                one(stdin).readMaskedText("Enter password: ");
                will(returnValue(PASSWORD_PLAINTEXT));
                one(stdin).readMaskedText("Re-enter password: ");
                will(returnValue(PASSWORD_PLAINTEXT));

                one(ltpaKeyFileUtil).createLTPAKeysFile(with("ltpa.keys"), with(any(byte[].class)));

                one(stdout).println(with(stringContaining("<ltpa", "ltpa.keys", PASSWORD_CIPHERTEXT)));
            }
        });

        assertEquals("FAIL: The task did not report execution OK",
                     SecurityUtilityReturnCodes.OK,
                     task.handleTask(stdin, stdout, stderr, args));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_specifiedFile_fileExists() throws Exception {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "securityUtility", "--password=Liberty", "--file=targetLtpaKeysFile" };

        mock.checking(new Expectations() {
            {
                one(fileUtil).exists("targetLtpaKeysFile");
                will(returnValue(true));

                one(stdout).println(with(stringContaining("LTPA")));
                one(stdout).println(with(stringContaining("targetLtpaKeysFile")));
                never(stdout).println(with(stringContaining("<ltpa")));
            }
        });

        assertEquals("FAIL: The task did not report execution failed due to file exists",
                     SecurityUtilityReturnCodes.ERR_FILE_EXISTS,
                     task.handleTask(stdin, stdout, stderr, args));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_specifiedFile_fileCreated() throws Exception {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "securityUtility", "--password=Liberty", "--file=targetLtpaKeysFile" };

        mock.checking(new Expectations() {
            {
                one(fileUtil).exists("targetLtpaKeysFile");
                will(returnValue(false));

                one(ltpaKeyFileUtil).createLTPAKeysFile(with("targetLtpaKeysFile"), with(any(byte[].class)));

                one(stdout).println(with(stringContaining("<ltpa", "targetLtpaKeysFile", PASSWORD_CIPHERTEXT)));
            }
        });

        assertEquals("FAIL: The task did not report execution OK",
                     SecurityUtilityReturnCodes.OK,
                     task.handleTask(stdin, stdout, stderr, args));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test()
    public void handleTask_specifiedFileAndServer() throws Exception {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "securityUtility", "--password=Liberty", "--file=targetLtpaKeysFile", "--server=targetServer" };

        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (IllegalArgumentException e) {
            assertTrue("FAIL: The thrown exception did not specify --server in its message",
                       e.getMessage().contains("--server"));
            assertTrue("FAIL: The thrown exception did not specify --file in its message",
                       e.getMessage().contains("--file"));
        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test()
    public void handleTask_specifiedServer_serverDoesNotExist() throws Exception {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "securityUtility", "--password=Liberty", "--server=targetServer" };

        mock.checking(new Expectations() {
            {
                one(fileUtil).exists("wlp/usr/servers/targetServer" + File.separator);
                will(returnValue(false));

                one(fileUtil).resolvePath("wlp/usr/servers/");
                will(returnValue("wlp/usr/servers/"));

                one(stdout).println(with(stringContaining("LTPA")));
                one(stdout).println(with(stringContaining("targetServer", "wlp/usr/servers/")));
            }
        });

        assertEquals("FAIL: The task did not report execution error due to server not found",
                     SecurityUtilityReturnCodes.ERR_SERVER_NOT_FOUND,
                     task.handleTask(stdin, stdout, stderr, args));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test()
    public void handleTask_specifiedServer_canNotCreatePath() throws Exception {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "securityUtility", "--password=Liberty", "--server=targetServer" };

        mock.checking(new Expectations() {
            {
                one(fileUtil).exists("wlp/usr/servers/targetServer" + File.separator);
                will(returnValue(true));

                String ltpaKeysPath = "wlp/usr/servers/targetServer" + File.separator + "resources" + File.separator + "security" + File.separator + "ltpa.keys";

                one(fileUtil).resolvePath(ltpaKeysPath);
                will(returnValue(ltpaKeysPath));

                one(fileUtil).createParentDirectory(with(stdout), with(any(File.class)));
                will(returnValue(false));

                one(stdout).println(with(stringContaining("LTPA")));
                one(stdout).println(with(stringContaining(ltpaKeysPath)));
            }
        });

        assertEquals("FAIL: The task did not report execution error due to path not created",
                     SecurityUtilityReturnCodes.ERR_PATH_CANNOT_BE_CREATED,
                     task.handleTask(stdin, stdout, stderr, args));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test()
    public void handleTask_specifiedServer_fileExists() throws Exception {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "securityUtility", "--password=Liberty", "--server=targetServer" };

        mock.checking(new Expectations() {
            {
                one(fileUtil).exists("wlp/usr/servers/targetServer" + File.separator);
                will(returnValue(true));

                String ltpaKeysPath = "wlp/usr/servers/targetServer" + File.separator + "resources" + File.separator + "security" + File.separator + "ltpa.keys";

                one(fileUtil).resolvePath(ltpaKeysPath);
                will(returnValue(ltpaKeysPath));

                one(fileUtil).createParentDirectory(with(stdout), with(any(File.class)));
                will(returnValue(true));

                one(fileUtil).exists(ltpaKeysPath);
                will(returnValue(true));

                one(stdout).println(with(stringContaining("LTPA")));
                one(stdout).println(with(stringContaining(ltpaKeysPath)));
            }
        });

        assertEquals("FAIL: The task did not report execution error due to file exists",
                     SecurityUtilityReturnCodes.ERR_FILE_EXISTS,
                     task.handleTask(stdin, stdout, stderr, args));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateLTPAKeysTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test()
    public void handleTask_specifiedServer_fileCreated() throws Exception {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "securityUtility", "--password=Liberty", "--server=targetServer" };

        mock.checking(new Expectations() {
            {
                one(fileUtil).exists("wlp/usr/servers/targetServer" + File.separator);
                will(returnValue(true));

                String ltpaKeysPath = "wlp/usr/servers/targetServer" + File.separator + "resources" + File.separator + "security" + File.separator + "ltpa.keys";

                one(fileUtil).resolvePath(ltpaKeysPath);
                will(returnValue(ltpaKeysPath));

                one(fileUtil).createParentDirectory(with(stdout), with(any(File.class)));
                will(returnValue(true));

                one(fileUtil).exists(ltpaKeysPath);
                will(returnValue(false));

                one(ltpaKeyFileUtil).createLTPAKeysFile(with(ltpaKeysPath), with(any(byte[].class)));

                one(stdout).println(with(stringContaining("<ltpa", PASSWORD_CIPHERTEXT)));
            }
        });

        assertEquals("FAIL: The task did not report execution OK",
                     SecurityUtilityReturnCodes.OK,
                     task.handleTask(stdin, stdout, stderr, args));
    }

}
