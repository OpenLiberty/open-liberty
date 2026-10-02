/*******************************************************************************
 * Copyright (c) 2016, 2025 IBM Corporation and others.
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
package com.ibm.ws.security.utility.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

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
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyEncryptor;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyFileUtility;
import com.ibm.ws.crypto.util.AesConfigFileParser;
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
        assertTrue("FAIL: The task help did not contain the option '--passwordBase64Key'",
                   help.contains("--passwordBase64Key"));
        assertTrue("FAIL: The task help did not contain the option '--aesConfigFile'",
                   help.contains("--aesConfigFile"));
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
    @Test
    public void handleTask_specifiedFile_fileCreated_aes() throws Exception {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "securityUtility", "--passwordEncoding=aes", "--password=Liberty", "--file=targetLtpaKeysFile" };

        mock.checking(new Expectations() {
            {
                one(fileUtil).exists("targetLtpaKeysFile");
                will(returnValue(false));

                one(ltpaKeyFileUtil).createLTPAKeysFile(with("targetLtpaKeysFile"), with(any(byte[].class)));

                one(stdout).println(with(stringContaining("<ltpa", "targetLtpaKeysFile", "{aes}")));
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
    public void handleTask_specifiedFile_fileCreated_base64_aes() throws Exception {

        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "securityUtility", "--passwordEncoding=aes", "--passwordBase64Key=JpOcjBKjoMlnXRNENZUrZODuAQxYIscJPtf7hDXBbuI=",
                                       "--password=Liberty",
                                       "--file=targetLtpaKeysFile" };

        mock.checking(new Expectations() {
            {
                one(fileUtil).exists("targetLtpaKeysFile");
                will(returnValue(false));

                one(ltpaKeyFileUtil).createLTPAKeysFile(with("targetLtpaKeysFile"), with(any(byte[].class)));

                one(stdout).println(with(stringContaining("<ltpa", "targetLtpaKeysFile", "{aes}")));
            }
        });

        assertEquals("FAIL: The task did not report execution OK",
                     SecurityUtilityReturnCodes.OK,
                     task.handleTask(stdin, stdout, stderr, args));
    }

    @Test
    public void handleTask_specifiedFile_fileCreated_aesConfigFile_aes() throws Exception {
        try (MockedStatic<AesConfigFileParser> passwordUtil = Mockito.mockStatic(AesConfigFileParser.class, Mockito.CALLS_REAL_METHODS)) {

            Map<String, String> props = new HashMap<>();
            props.put(PasswordUtil.PROPERTY_AES_KEY, "JpOcjBKjoMlnXRNENZUrZODuAQxYIscJPtf7hDXBbuI=");
            String aesConfigFilePath = "keys.xml";

            passwordUtil.when(() -> {
                AesConfigFileParser.parseAesEncryptionFile(aesConfigFilePath);
            }).thenReturn(props);
            CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
            String[] args = new String[] { "securityUtility", "--passwordEncoding=aes", "--aesConfigFile=" + aesConfigFilePath,
                                           "--password=Liberty",
                                           "--file=targetLtpaKeysFile" };

            mock.checking(new Expectations() {
                {
                    one(fileUtil).exists("targetLtpaKeysFile");
                    will(returnValue(false));

                    one(ltpaKeyFileUtil).createLTPAKeysFile(with("targetLtpaKeysFile"), with(any(byte[].class)));

                    one(stdout).println(with(stringContaining("<ltpa", "targetLtpaKeysFile", "{aes}")));

                }
            });

            assertEquals("FAIL: The task did not report execution OK",
                         SecurityUtilityReturnCodes.OK,
                         task.handleTask(stdin, stdout, stderr, args));
        }
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

    // -----------------------------------------------------------------------
    // checkRequiredArguments — new --useEncryptionKey validation
    // -----------------------------------------------------------------------

    /**
     * --useEncryptionKey=true + --password is a conflict error.
     */
    @Test
    public void checkRequiredArguments_useEncryptionKey_passwordConflict() {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "createLTPAKeys", "--useEncryptionKey=true", "--password=Liberty", "--passwordKey=mykey" };
        try {
            task.checkRequiredArguments(args);
            fail("Expected IllegalArgumentException for useEncryptionKey + password conflict");
        } catch (IllegalArgumentException e) {
            assertTrue("Expected passwordConflict message, got: " + e.getMessage(),
                       e.getMessage().contains("--useEncryptionKey=true") && e.getMessage().contains("--password"));
        }
    }

    /**
     * --useEncryptionKey=true with no AES config is an error.
     */
    @Test
    public void checkRequiredArguments_useEncryptionKey_missingAesConfig() {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "createLTPAKeys", "--useEncryptionKey=true", "--file=ltpa.keys" };
        try {
            task.checkRequiredArguments(args);
            fail("Expected IllegalArgumentException for useEncryptionKey + no AES config");
        } catch (IllegalArgumentException e) {
            assertTrue("Expected missingAesConfig message, got: " + e.getMessage(),
                       e.getMessage().contains("--passwordKey") || e.getMessage().contains("--useEncryptionKey"));
        }
    }

    /**
     * --useEncryptionKey=true + --passwordKey is valid (no exception).
     */
    @Test
    public void checkRequiredArguments_useEncryptionKey_withPasswordKey_valid() {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "createLTPAKeys", "--useEncryptionKey=true", "--passwordKey=mykey" };
        // Should not throw
        task.checkRequiredArguments(args);
    }

    /**
     * No --password and no --useEncryptionKey=true is still an error.
     */
    @Test
    public void checkRequiredArguments_noPassword_noUseEncryptionKey() {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "createLTPAKeys", "--file=ltpa.keys" };
        try {
            task.checkRequiredArguments(args);
            fail("Expected IllegalArgumentException for missing --password");
        } catch (IllegalArgumentException e) {
            assertTrue("Expected --password in message, got: " + e.getMessage(),
                       e.getMessage().contains("--password"));
        }
    }

    // -----------------------------------------------------------------------
    // handleICSFWithPasswordPath — ICSF + password (no --useEncryptionKey)
    // -----------------------------------------------------------------------

    /**
     * ICSF + password: LTPA file created with password bytes; snippet contains keysPassword.
     * Uses Mockito to stub AESKeyManager.setSecretKeyResolver and PasswordUtil.encode.
     */
    @Test
    public void handleTask_icsf_withPassword_fileCreated() throws Exception {
        // Subclass overrides isZOS() so the z/OS arg check passes on non-z/OS test machines.
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME) {
            @Override
            boolean isZOS() { return true; }
        };
        String[] args = new String[] { "securityUtility",
                                       "--keyringType=ICSF",
                                       "--keyLabel=MY.ICSF.LABEL",
                                       "--password=Liberty",
                                       "--file=ltpa.keys" };

        try (MockedStatic<com.ibm.ws.crypto.util.AESKeyManager> aesKeyManager =
                Mockito.mockStatic(com.ibm.ws.crypto.util.AESKeyManager.class, Mockito.CALLS_REAL_METHODS);
             MockedStatic<PasswordUtil> passwordUtil =
                Mockito.mockStatic(PasswordUtil.class, Mockito.CALLS_REAL_METHODS)) {

            aesKeyManager.when(() -> com.ibm.ws.crypto.util.AESKeyManager.setSecretKeyResolver(Mockito.any())).thenAnswer(inv -> null);
            passwordUtil.when(() -> PasswordUtil.encode(Mockito.eq("Liberty"), Mockito.eq("aes"), Mockito.<Map<String, String>>any()))
                        .thenReturn("{aes}encodedViaICSF");

            mock.checking(new Expectations() {
                {
                    one(fileUtil).exists("ltpa.keys");
                    will(returnValue(false));

                    one(ltpaKeyFileUtil).createLTPAKeysFile(with("ltpa.keys"), with(any(byte[].class)));

                    one(stdout).println(with(stringContaining("keysPassword", "{aes}encodedViaICSF")));
                }
            });

            assertEquals(SecurityUtilityReturnCodes.OK,
                         task.handleTask(stdin, stdout, stderr, args));
        }
    }

    // -----------------------------------------------------------------------
    // handleEncryptionKeyPath — --useEncryptionKey=true paths
    // -----------------------------------------------------------------------

    /**
     * --useEncryptionKey=true + --passwordKey: file created via encryptor; snippet has
     * wlp.password.encryption.key hint and useEncryptionKey="true".
     */
    @Test
    public void handleTask_useEncryptionKey_passwordKey_fileCreated() throws Exception {
        // A valid 32-char key string for AES_V1 PBKDF2 — actual value doesn't matter for snippet assertion.
        String passwordKey = "myTestEncryptionKey";

        // No z/OS args used here, standard task is fine.
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "securityUtility",
                                       "--useEncryptionKey=true",
                                       "--passwordKey=" + passwordKey,
                                       "--file=ltpa.keys" };

        try (MockedStatic<com.ibm.ws.crypto.util.AESKeyManager> aesKeyManager =
                Mockito.mockStatic(com.ibm.ws.crypto.util.AESKeyManager.class, Mockito.CALLS_REAL_METHODS)) {

            javax.crypto.spec.SecretKeySpec fakeKey =
                new javax.crypto.spec.SecretKeySpec(new byte[32], "AES");
            aesKeyManager.when(() -> com.ibm.ws.crypto.util.AESKeyManager.getKey(
                    com.ibm.ws.crypto.util.AESKeyManager.KeyVersion.AES_V1, passwordKey))
                         .thenReturn(fakeKey);

            mock.checking(new Expectations() {
                {
                    one(fileUtil).exists("ltpa.keys");
                    will(returnValue(false));

                    one(ltpaKeyFileUtil).createLTPAKeysFile(with("ltpa.keys"), with(any(LTPAKeyEncryptor.class)));

                    one(stdout).println(with(stringContaining("wlp.password.encryption.key", "useEncryptionKey=\"true\"")));
                }
            });

            assertEquals(SecurityUtilityReturnCodes.OK,
                         task.handleTask(stdin, stdout, stderr, args));
        }
    }

    /**
     * --useEncryptionKey=true + --passwordBase64Key: snippet has wlp.aes.encryption.key hint.
     */
    @Test
    public void handleTask_useEncryptionKey_passwordBase64Key_fileCreated() throws Exception {
        // Valid 32-byte Base64 key (256-bit).
        String base64Key = "JpOcjBKjoMlnXRNENZUrZODuAQxYIscJPtf7hDXBbuI=";

        // No z/OS args used here, standard task is fine.
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        String[] args = new String[] { "securityUtility",
                                       "--useEncryptionKey=true",
                                       "--passwordBase64Key=" + base64Key,
                                       "--file=ltpa.keys" };

        mock.checking(new Expectations() {
            {
                one(fileUtil).exists("ltpa.keys");
                will(returnValue(false));

                one(ltpaKeyFileUtil).createLTPAKeysFile(with("ltpa.keys"), with(any(LTPAKeyEncryptor.class)));

                one(stdout).println(with(stringContaining("wlp.aes.encryption.key", "useEncryptionKey=\"true\"")));
            }
        });

        assertEquals(SecurityUtilityReturnCodes.OK,
                     task.handleTask(stdin, stdout, stderr, args));
    }

    /**
     * --useEncryptionKey=true + ICSF (regression): snippet contains zosPasswordEncryptionKey
     * and useEncryptionKey="true". This is the existing handleICSFPath.
     */
    @Test
    public void handleTask_useEncryptionKey_icsf_regression() throws Exception {
        // Subclass overrides isZOS() so the z/OS arg check passes on non-z/OS test machines.
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME) {
            @Override
            boolean isZOS() { return true; }
        };
        String[] args = new String[] { "securityUtility",
                                       "--useEncryptionKey=true",
                                       "--keyringType=ICSF",
                                       "--keyLabel=MY.ICSF.LABEL",
                                       "--file=ltpa.keys" };

        try (MockedStatic<com.ibm.ws.crypto.util.AESKeyManager> aesKeyManager =
                Mockito.mockStatic(com.ibm.ws.crypto.util.AESKeyManager.class, Mockito.CALLS_REAL_METHODS)) {

            javax.crypto.spec.SecretKeySpec fakeKey =
                new javax.crypto.spec.SecretKeySpec(new byte[32], "AES");
            aesKeyManager.when(() -> com.ibm.ws.crypto.util.AESKeyManager.setSecretKeyResolver(Mockito.any())).thenAnswer(inv -> null);
            aesKeyManager.when(() -> com.ibm.ws.crypto.util.AESKeyManager.getKeyViaResolver(
                    com.ibm.ws.crypto.util.AESKeyManager.KeyVersion.AES_V2))
                         .thenReturn(fakeKey);

            mock.checking(new Expectations() {
                {
                    one(fileUtil).exists("ltpa.keys");
                    will(returnValue(false));

                    one(ltpaKeyFileUtil).createLTPAKeysFile(with("ltpa.keys"), with(any(LTPAKeyEncryptor.class)));

                    one(stdout).println(with(stringContaining("zosPasswordEncryptionKey", "MY.ICSF.LABEL", "useEncryptionKey=\"true\"")));
                }
            });

            assertEquals(SecurityUtilityReturnCodes.OK,
                         task.handleTask(stdin, stdout, stderr, args));
        }
    }

    /**
     * --useEncryptionKey=true is now recognised as a known argument.
     */
    @Test
    public void isKnownArgument_useEncryptionKey() {
        CreateLTPAKeysTask task = new CreateLTPAKeysTask(ltpaKeyFileUtil, fileUtil, TEST_UTILITY_NAME);
        assertTrue("FAIL: Did not recognize the --useEncryptionKey flag",
                   task.isKnownArgument("--useEncryptionKey"));
    }

}
