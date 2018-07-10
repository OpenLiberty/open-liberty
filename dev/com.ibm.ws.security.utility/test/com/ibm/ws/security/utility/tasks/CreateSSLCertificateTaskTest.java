/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.PrintStream;
import java.security.cert.CertificateException;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateCreator;
import com.ibm.ws.crypto.certificateutil.DefaultSubjectDN;
import com.ibm.ws.security.utility.IFileUtility;
import com.ibm.ws.security.utility.SecurityUtilityReturnCodes;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;

import test.common.junit.matchers.RegexMatcher;
import test.common.junit.rules.MaximumJavaLevelRule;

/**
 *
 */
public class CreateSSLCertificateTaskTest {

    @ClassRule
    public static MaximumJavaLevelRule maxLevel = new MaximumJavaLevelRule(8);

    private static final String NL = "\n";
    private static final String SERVER_NAME = "myServer";
    private static final String CLIENT_NAME = "myClient";
    private static final String GEN_SUBJECT_DN = new DefaultSubjectDN(null, SERVER_NAME).getSubjectDN();
    private static final String GEN_CLIENT_SUBJECT_DN = new DefaultSubjectDN(null, CLIENT_NAME).getSubjectDN();

    private static String EXPECTED_USR_SERVERS = "wlp/usr/servers";
    private static String EXPECTED_USR_CLIENTS = "wlp/usr/clients";
    private static String EXPECTED_SERVER_DIR = EXPECTED_USR_SERVERS + SERVER_NAME + CreateSSLCertificateTask.SLASH;
    private static String EXPECTED_CLIENT_DIR = EXPECTED_USR_CLIENTS + CLIENT_NAME + CreateSSLCertificateTask.SLASH;
    private static String EXPECTED_KEYSTORE_PATH;
    private static String EXPECTED_CLIENT_KEYSTORE_PATH;
    private static File EXPECTED_KEYSTORE_FILE;
    {
        EXPECTED_KEYSTORE_FILE = new File(EXPECTED_SERVER_DIR + "resources" + CreateSSLCertificateTask.SLASH + "security" + CreateSSLCertificateTask.SLASH + "key.jks");
        EXPECTED_KEYSTORE_PATH = EXPECTED_KEYSTORE_FILE.getAbsolutePath();
    }
    private static File EXPECTED_CLIENT_KEYSTORE_FILE;
    {
        EXPECTED_CLIENT_KEYSTORE_FILE = new File(EXPECTED_CLIENT_DIR + "resources" + CreateSSLCertificateTask.SLASH + "security" + CreateSSLCertificateTask.SLASH + "key.jks");
        EXPECTED_CLIENT_KEYSTORE_PATH = EXPECTED_CLIENT_KEYSTORE_FILE.getAbsolutePath();
    }
    private static final String PLAINTEXT = "encodeMe";
    private static final String CIPHERTEXT = "{xor}OjE8MDs6Ejo=";
    private static final String VALIDITY = "365";
    private static final String SUBJECT_DN = "CN=localhost";
    private static final String LONG_SUBJECT_DN = "CN=localhost,OU=myServer,O=IBM,C=US";

    private static final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private static final ConsoleWrapper stdin = mock.mock(ConsoleWrapper.class, "stdin");
    private static final PrintStream stdout = mock.mock(PrintStream.class, "stdout");
    private static final PrintStream stderr = mock.mock(PrintStream.class, "stderr");
    private static final DefaultSSLCertificateCreator creator = mock.mock(DefaultSSLCertificateCreator.class);
    private static final IFileUtility fileUtil = mock.mock(IFileUtility.class);
    private BaseCommandTask task;

    @Factory
    public static Matcher<String> matching(String regex) {
        return new RegexMatcher(regex);
    }

    private static final String TEST_UTILITY_NAME = "testUtility";
    private static final String TEST_TASK_NAME = "testTask";
    private static final String TEST_SERVER_DIR = "/wlp/usr/servers/defaultServer";
    private static final String TEST_SNIPPET = "<featureManager><feature>collectiveMember-1.0</feature></featureManager>";

    private static class ConfigFileTestTask extends CreateSSLCertificateTask {
        ConfigFileTestTask() {
            super(null, CreateSSLCertificateTaskTest.fileUtil, TEST_UTILITY_NAME);
            this.stdin = CreateSSLCertificateTaskTest.stdin;
            this.stdout = CreateSSLCertificateTaskTest.stdout;
            this.stderr = CreateSSLCertificateTaskTest.stderr;
        }

        @Override
        public String getTaskName() {
            return TEST_TASK_NAME;
        }

        @Override
        public SecurityUtilityReturnCodes handleTask(ConsoleWrapper stdin, PrintStream stdout, PrintStream stderr, String[] args) {
            stdout.println(createConfigFileIfNeeded(TEST_SERVER_DIR, args, TEST_SNIPPET));
            return SecurityUtilityReturnCodes.OK;
        }
    }

    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(fileUtil).getClientsDirectory();
                will(returnValue(EXPECTED_USR_CLIENTS));
                allowing(fileUtil).getServersDirectory();
                will(returnValue(EXPECTED_USR_SERVERS));
            }
        });
        task = new CreateSSLCertificateTask(creator, fileUtil, "myScript");
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    @Test
    public void getTaskHelp() {
        assertNotNull(task.getTaskHelp());
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_noArguments() throws Exception {
        String[] args = new String[] { task.getTaskName() };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_unDashknownArgument() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "-unknown" };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;

        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_unknownArgument() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--unknown" };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;

        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_unknownValue() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "unknown" };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;

        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_singleArgument() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--server" };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;

        }

    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_noServerArgument() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--password=" + PLAINTEXT };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;

        }

    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_noPasswordArgument() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--server=" + SERVER_NAME };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;

        }

    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_noPasswordArgumentWithClient() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--client=" + CLIENT_NAME };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;

        }

    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_justRequiredFlagsNoValues() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--server",
                                       "--password" };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;

        }

    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_justServerAndClientFlags() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--server" + SERVER_NAME,
                                       "--client" + CLIENT_NAME,
                                       "--password=liberty" };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;

        }
    }

    /**
     * Create the expectations for a successful execution of the task.
     *
     * @throws Exception
     */
    private void createSuccessfulStdoutExpectations(String subject_dn) throws Exception {
        final String final_subject_dn = subject_dn;
        mock.checking(new Expectations() {
            {
                one(stdout).println("Creating keystore " + EXPECTED_KEYSTORE_FILE.getAbsolutePath() + NL);
                one(stdout).println("Created SSL certificate for server " + SERVER_NAME + ". " + "The certificate is created with " + final_subject_dn + " as the SubjectDN." + NL +
                                    NL +
                                    "Add the following lines to the server.xml to enable SSL:");
                one(stdout).println(BaseCommandTask.NL +
                                    "    <featureManager>" + BaseCommandTask.NL +
                                    "        <feature>ssl-1.0</feature>" + BaseCommandTask.NL +
                                    "    </featureManager>" + BaseCommandTask.NL +
                                    "    <keyStore id=\"defaultKeyStore\" password=\"" + CIPHERTEXT + "\" />" + BaseCommandTask.NL +
                                    BaseCommandTask.NL);
            }
        });
    }

    /**
     * Create the expectations for a successful execution of the task.
     *
     * @throws Exception
     */
    private void createSuccessfulStdoutExpectationsClient(String subject_dn) throws Exception {
        final String final_subject_dn = subject_dn;
        mock.checking(new Expectations() {
            {
                one(stdout).println("Creating keystore " + EXPECTED_CLIENT_KEYSTORE_FILE.getAbsolutePath() + NL);
                one(stdout).println("Created SSL certificate for client " + CLIENT_NAME + ". " + "The certificate is created with " + final_subject_dn + " as the SubjectDN." + NL +
                                    NL +
                                    "Add the following lines to the client.xml to enable SSL:");
                one(stdout).println(BaseCommandTask.NL +
                                    "    <featureManager>" + BaseCommandTask.NL +
                                    "        <feature>appSecurityClient-1.0</feature>" + BaseCommandTask.NL +
                                    "    </featureManager>" + BaseCommandTask.NL +
                                    "    <keyStore id=\"defaultKeyStore\" password=\"" + CIPHERTEXT + "\" />" + BaseCommandTask.NL +
                                    BaseCommandTask.NL);
            }
        });
    }

    /**
     * matchingFile compares two files.
     *
     * @param file
     * @return boolean if the cookie's properties match
     * @see FileMatcher
     */
    @Factory
    public static Matcher<File> matchingFile(File file) {
        return new FileMatcher(file);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_justRequiredFlags() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--server=" + SERVER_NAME,
                                       "--password=" + PLAINTEXT,
                                       "--passwordEncoding=xor" };

        createSuccessfulStdoutExpectations(GEN_SUBJECT_DN);
        mock.checking(new Expectations() {
            {
                one(fileUtil).exists(EXPECTED_SERVER_DIR);
                will(returnValue(true));
                one(fileUtil).resolvePath(EXPECTED_KEYSTORE_FILE);
                will(returnValue(EXPECTED_KEYSTORE_PATH));
                one(fileUtil).createParentDirectory(with(stdout), with(matchingFile(EXPECTED_KEYSTORE_FILE)));
                will(returnValue(true));
                one(creator).createDefaultSSLCertificate(EXPECTED_KEYSTORE_PATH,
                                                         PLAINTEXT,
                                                         DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                                         new DefaultSubjectDN(null, SERVER_NAME).getSubjectDN(),
                                                         DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                                         DefaultSSLCertificateCreator.SIGALG);
            }
        });

        task.handleTask(stdin, stdout, stderr, args);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_justRequiredFlags_client() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--client=" + CLIENT_NAME,
                                       "--password=" + PLAINTEXT,
                                       "--passwordEncoding=xor" };

        createSuccessfulStdoutExpectationsClient(GEN_CLIENT_SUBJECT_DN);
        mock.checking(new Expectations() {
            {
                one(fileUtil).exists(EXPECTED_CLIENT_DIR);
                will(returnValue(true));
                one(fileUtil).resolvePath(EXPECTED_CLIENT_KEYSTORE_FILE);
                will(returnValue(EXPECTED_CLIENT_KEYSTORE_PATH));
                one(fileUtil).createParentDirectory(with(stdout), with(matchingFile(EXPECTED_CLIENT_KEYSTORE_FILE)));
                will(returnValue(true));
                one(creator).createDefaultSSLCertificate(EXPECTED_CLIENT_KEYSTORE_PATH,
                                                         PLAINTEXT,
                                                         DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                                         new DefaultSubjectDN(null, CLIENT_NAME).getSubjectDN(),
                                                         DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                                         DefaultSSLCertificateCreator.SIGALG);
            }
        });

        task.handleTask(stdin, stdout, stderr, args);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.EncodeTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_promptWhenNoConsole() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--server=" + SERVER_NAME,
                                       "--password" };

        mock.checking(new Expectations() {
            {
                one(fileUtil).exists(EXPECTED_SERVER_DIR);
                will(returnValue(true));
                one(fileUtil).resolvePath(EXPECTED_KEYSTORE_FILE);
                will(returnValue(EXPECTED_KEYSTORE_PATH));
                one(fileUtil).createParentDirectory(with(stdout), with(matchingFile(EXPECTED_KEYSTORE_FILE)));
                will(returnValue(true));

                one(stdin).readMaskedText("Enter password: ");
                will(returnValue(null));
                one(stdin).readMaskedText("Re-enter password: ");
                will(returnValue(null));
            }
        });

        task.handleTask(stdin, stdout, stderr, args);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_promptForPassword() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--server=" + SERVER_NAME,
                                       "--password",
                                       "--passwordEncoding=xor" };

        createSuccessfulStdoutExpectations(GEN_SUBJECT_DN);
        mock.checking(new Expectations() {
            {
                one(fileUtil).exists(EXPECTED_SERVER_DIR);
                will(returnValue(true));
                one(fileUtil).resolvePath(EXPECTED_KEYSTORE_FILE);
                will(returnValue(EXPECTED_KEYSTORE_PATH));
                one(fileUtil).createParentDirectory(with(stdout), with(matchingFile(EXPECTED_KEYSTORE_FILE)));
                will(returnValue(true));

                one(stdin).readMaskedText("Enter password: ");
                will(returnValue(PLAINTEXT));
                one(stdin).readMaskedText("Re-enter password: ");
                will(returnValue(PLAINTEXT));

                one(creator).createDefaultSSLCertificate(EXPECTED_KEYSTORE_PATH,
                                                         PLAINTEXT,
                                                         DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                                         new DefaultSubjectDN(null, SERVER_NAME).getSubjectDN(),
                                                         DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                                         DefaultSSLCertificateCreator.SIGALG);
            }
        });

        task.handleTask(stdin, stdout, stderr, args);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_passwordAndDays() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--server=" + SERVER_NAME,
                                       "--password=" + PLAINTEXT,
                                       "--validity=" + VALIDITY,
                                       "--passwordEncoding=xor" };

        createSuccessfulStdoutExpectations(GEN_SUBJECT_DN);
        mock.checking(new Expectations() {
            {
                one(fileUtil).exists(EXPECTED_SERVER_DIR);
                will(returnValue(true));
                one(fileUtil).resolvePath(EXPECTED_KEYSTORE_FILE);
                will(returnValue(EXPECTED_KEYSTORE_PATH));
                one(fileUtil).createParentDirectory(with(stdout), with(matchingFile(EXPECTED_KEYSTORE_FILE)));
                will(returnValue(true));
                one(creator).createDefaultSSLCertificate(EXPECTED_KEYSTORE_PATH,
                                                         PLAINTEXT,
                                                         Integer.valueOf(VALIDITY),
                                                         new DefaultSubjectDN(null, SERVER_NAME).getSubjectDN(),
                                                         DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                                         DefaultSSLCertificateCreator.SIGALG);
            }
        });

        task.handleTask(stdin, stdout, stderr, args);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_passwordAndSubject() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--server=" + SERVER_NAME,
                                       "--password=" + PLAINTEXT,
                                       "--subject=" + SUBJECT_DN,
                                       "--passwordEncoding=xor" };

        createSuccessfulStdoutExpectations(SUBJECT_DN);
        mock.checking(new Expectations() {
            {
                one(fileUtil).exists(EXPECTED_SERVER_DIR);
                will(returnValue(true));
                one(fileUtil).resolvePath(EXPECTED_KEYSTORE_FILE);
                will(returnValue(EXPECTED_KEYSTORE_PATH));
                one(fileUtil).createParentDirectory(with(stdout), with(matchingFile(EXPECTED_KEYSTORE_FILE)));
                will(returnValue(true));
                one(creator).createDefaultSSLCertificate(EXPECTED_KEYSTORE_PATH,
                                                         PLAINTEXT,
                                                         DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                                         SUBJECT_DN,
                                                         DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                                         DefaultSSLCertificateCreator.SIGALG);
            }
        });

        task.handleTask(stdin, stdout, stderr, args);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_passwordAndLongSubject() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--server=" + SERVER_NAME,
                                       "--password=" + PLAINTEXT,
                                       "--subject=" + LONG_SUBJECT_DN,
                                       "--passwordEncoding=xor" };

        createSuccessfulStdoutExpectations(LONG_SUBJECT_DN);
        mock.checking(new Expectations() {
            {
                one(fileUtil).exists(EXPECTED_SERVER_DIR);
                will(returnValue(true));
                one(fileUtil).resolvePath(EXPECTED_KEYSTORE_FILE);
                will(returnValue(EXPECTED_KEYSTORE_PATH));
                one(fileUtil).createParentDirectory(with(stdout), with(matchingFile(EXPECTED_KEYSTORE_FILE)));
                will(returnValue(true));
                one(creator).createDefaultSSLCertificate(EXPECTED_KEYSTORE_PATH,
                                                         PLAINTEXT,
                                                         DefaultSSLCertificateCreator.DEFAULT_VALIDITY,
                                                         LONG_SUBJECT_DN,
                                                         DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                                         DefaultSSLCertificateCreator.SIGALG);
            }
        });

        task.handleTask(stdin, stdout, stderr, args);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_passwordAndDaysAndSubject() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--server=" + SERVER_NAME,
                                       "--password=" + PLAINTEXT,
                                       "--validity=" + VALIDITY,
                                       "--subject=" + SUBJECT_DN,
                                       "--passwordEncoding=xor" };

        createSuccessfulStdoutExpectations(SUBJECT_DN);
        mock.checking(new Expectations() {
            {
                one(fileUtil).exists(EXPECTED_SERVER_DIR);
                will(returnValue(true));
                one(fileUtil).resolvePath(EXPECTED_KEYSTORE_FILE);
                will(returnValue(EXPECTED_KEYSTORE_PATH));
                one(fileUtil).createParentDirectory(with(stdout), with(matchingFile(EXPECTED_KEYSTORE_FILE)));
                will(returnValue(true));
                one(creator).createDefaultSSLCertificate(EXPECTED_KEYSTORE_PATH,
                                                         PLAINTEXT,
                                                         Integer.valueOf(VALIDITY),
                                                         SUBJECT_DN,
                                                         DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                                         DefaultSSLCertificateCreator.SIGALG);
            }
        });

        task.handleTask(stdin, stdout, stderr, args);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_anyOrder() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--validity=" + VALIDITY,
                                       "--password=" + PLAINTEXT,
                                       "--subject=" + SUBJECT_DN,
                                       "--server=" + SERVER_NAME,
                                       "--passwordEncoding=xor" };

        createSuccessfulStdoutExpectations(SUBJECT_DN);
        mock.checking(new Expectations() {
            {
                one(fileUtil).exists(EXPECTED_SERVER_DIR);
                will(returnValue(true));
                one(fileUtil).resolvePath(EXPECTED_KEYSTORE_FILE);
                will(returnValue(EXPECTED_KEYSTORE_PATH));
                one(fileUtil).createParentDirectory(with(stdout), with(matchingFile(EXPECTED_KEYSTORE_FILE)));
                will(returnValue(true));
                one(creator).createDefaultSSLCertificate(EXPECTED_KEYSTORE_PATH,
                                                         PLAINTEXT,
                                                         Integer.valueOf(VALIDITY),
                                                         SUBJECT_DN,
                                                         DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                                         DefaultSSLCertificateCreator.SIGALG);
            }
        });

        task.handleTask(stdin, stdout, stderr, args);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_anyOrderPrompt() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--validity=" + VALIDITY,
                                       "--password",
                                       "--subject=" + SUBJECT_DN,
                                       "--server=" + SERVER_NAME,
                                       "--passwordEncoding=xor" };

        createSuccessfulStdoutExpectations(SUBJECT_DN);
        mock.checking(new Expectations() {
            {
                one(stdin).readMaskedText("Enter password: ");
                will(returnValue(PLAINTEXT));
                one(stdin).readMaskedText("Re-enter password: ");
                will(returnValue(PLAINTEXT));

                one(fileUtil).exists(EXPECTED_SERVER_DIR);
                will(returnValue(true));
                one(fileUtil).resolvePath(EXPECTED_KEYSTORE_FILE);
                will(returnValue(EXPECTED_KEYSTORE_PATH));
                one(fileUtil).createParentDirectory(with(stdout), with(matchingFile(EXPECTED_KEYSTORE_FILE)));
                will(returnValue(true));
                one(creator).createDefaultSSLCertificate(EXPECTED_KEYSTORE_PATH,
                                                         PLAINTEXT,
                                                         Integer.valueOf(VALIDITY),
                                                         SUBJECT_DN,
                                                         DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                                         DefaultSSLCertificateCreator.SIGALG);
            }
        });

        task.handleTask(stdin, stdout, stderr, args);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_extraValueFront() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "extraValue",
                                       "-validity", VALIDITY,
                                       "-password", PLAINTEXT,
                                       "-subject", SUBJECT_DN,
                                       "-server", SERVER_NAME };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;
        }

    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_extraValueMiddle() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "-validity", VALIDITY,
                                       "-password", PLAINTEXT,
                                       "-subject", SUBJECT_DN,
                                       "extraValue",
                                       "-server", SERVER_NAME };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;
        }

    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_unknownArgumentMiddle() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "-validity", VALIDITY,
                                       "-password", PLAINTEXT,
                                       "-subject", SUBJECT_DN,
                                       "-unknown",
                                       "-server", SERVER_NAME };
        try {
            task.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;
        } ;
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_serverDoesntExist() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--validity=" + VALIDITY,
                                       "--password=" + PLAINTEXT,
                                       "--subject=" + SUBJECT_DN,
                                       "--server=" + SERVER_NAME };

        mock.checking(new Expectations() {
            {
                one(fileUtil).exists(with(any(String.class)));
                will(returnValue(false));
                one(fileUtil).resolvePath(EXPECTED_USR_SERVERS);
                will(returnValue(EXPECTED_USR_SERVERS));
                one(stdout).println("Aborting certificate creation:");
                one(stdout).println("Specified server " + SERVER_NAME + " could not be found at location " + EXPECTED_USR_SERVERS);
            }
        });

        task.handleTask(stdin, stdout, stderr, args);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.CreateSSLCertificateTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    // @Test
    public void handleTask_failedCreate() throws Exception {
        String[] args = new String[] { task.getTaskName(),
                                       "--validity=" + VALIDITY,
                                       "--password=" + PLAINTEXT,
                                       "--subject=" + SUBJECT_DN,
                                       "--server=" + SERVER_NAME };

        mock.checking(new Expectations() {
            {
                one(stdout).println("Creating keystore " + EXPECTED_KEYSTORE_FILE.getAbsolutePath() + NL);
                one(fileUtil).exists(EXPECTED_SERVER_DIR);
                will(returnValue(true));
                one(fileUtil).resolvePath(EXPECTED_KEYSTORE_FILE);
                will(returnValue(EXPECTED_KEYSTORE_PATH));
                one(fileUtil).createParentDirectory(with(stdout), with(matchingFile(EXPECTED_KEYSTORE_FILE)));
                will(returnValue(true));
                one(creator).createDefaultSSLCertificate(EXPECTED_KEYSTORE_PATH,
                                                         PLAINTEXT,
                                                         Integer.valueOf(VALIDITY),
                                                         SUBJECT_DN,
                                                         DefaultSSLCertificateCreator.DEFAULT_SIZE,
                                                         DefaultSSLCertificateCreator.KEYALG);
                will(throwException(new CertificateException("Expected")));

                one(stdout).println("Unable to create default SSL certificate:" +
                                    NL +
                                    "Expected");
            }
        });
        task.handleTask(stdin, stdout, stderr, args);
    }

    @Test
    public void generateConfigFileName_noTargetFilename() {
        mock.checking(new Expectations() {
            {
                oneOf(fileUtil).isDirectory(with(any(File.class)));
                will(returnValue(true));

                oneOf(fileUtil).exists(with(any(File.class)));
                will(returnValue(false));
            }
        });
        ConfigFileTestTask task = new ConfigFileTestTask();
        String path = task.generateConfigFileName(TEST_UTILITY_NAME, TEST_TASK_NAME, TEST_SERVER_DIR, null).getPath();
        path = path.replace('\\', '/');
        String expected = TEST_SERVER_DIR + "/" + TEST_UTILITY_NAME + "-" + TEST_TASK_NAME + "-include.xml";
        assertEquals(expected, path);
        mock.assertIsSatisfied();
    }

    @Test
    public void generateConfigFileName_targetFilenameIsDirectory() {
        mock.checking(new Expectations() {
            {
                oneOf(fileUtil).isDirectory(with(any(File.class)));
                will(returnValue(true));

                oneOf(fileUtil).exists(with(any(File.class)));
                will(returnValue(false));
            }
        });
        ConfigFileTestTask task = new ConfigFileTestTask();
        String path = task.generateConfigFileName(TEST_UTILITY_NAME, TEST_TASK_NAME, TEST_SERVER_DIR, "fooDir/").getPath();
        path = path.replace('\\', '/');
        String expected = "fooDir/" + TEST_UTILITY_NAME + "-" + TEST_TASK_NAME + "-include.xml";
        assertEquals(expected, path);
        mock.assertIsSatisfied();
    }

    @Test
    public void generateConfigFileName_targetFilenameIsFile() {
        mock.checking(new Expectations() {
            {
                oneOf(fileUtil).isDirectory(with(any(File.class)));
                will(returnValue(false));

                oneOf(fileUtil).exists(with(any(File.class)));
                will(returnValue(false));
            }
        });
        ConfigFileTestTask task = new ConfigFileTestTask();
        String path = task.generateConfigFileName(TEST_UTILITY_NAME, TEST_TASK_NAME, TEST_SERVER_DIR, "fooDir/foo.file").getPath();
        path = path.replace('\\', '/');
        String expected = "fooDir/foo.file";
        assertEquals(expected, path);
        mock.assertIsSatisfied();
    }

    @Test
    public void generateConfigFileName_targetFilenameExists() {
        mock.checking(new Expectations() {
            {
                oneOf(fileUtil).isDirectory(with(any(File.class)));
                will(returnValue(false));

                oneOf(fileUtil).exists(with(any(File.class)));
                will(returnValue(true));

                oneOf(fileUtil).exists(with(any(File.class)));
                will(returnValue(true));

                oneOf(fileUtil).exists(with(any(File.class)));
                will(returnValue(false));
            }
        });
        ConfigFileTestTask task = new ConfigFileTestTask();
        String path = task.generateConfigFileName(TEST_UTILITY_NAME, TEST_TASK_NAME, TEST_SERVER_DIR, "fooDir/foo.file").getPath();
        path = path.replace('\\', '/');
        String expected = "fooDir/foo2.file";
        assertEquals(expected, path);
        mock.assertIsSatisfied();
    }

    @Test
    public void createConfigFileIfNeeded_configOptionNotProvided() throws Exception {
        mock.checking(new Expectations() {
            {
                oneOf(stdout).println(with(TEST_SNIPPET));
            }
        });
        ConfigFileTestTask task = new ConfigFileTestTask();
        task.handleTask(stdin, stdout, stderr, new String[] { TEST_TASK_NAME, "foo" });
        mock.assertIsSatisfied();
    }

    @Test
    public void createConfigFileIfNeeded_configOptionValueNotProvided() throws Exception {
        mock.checking(new Expectations() {
            {
                oneOf(fileUtil).isDirectory(with(any(File.class)));
                will(returnValue(true));

                oneOf(fileUtil).exists(with(any(File.class)));
                will(returnValue(false));

                oneOf(fileUtil).createParentDirectory(with(stdout), with(any(File.class)));

                oneOf(fileUtil).writeToFile(with(stderr), with(matching("feature")), with(any(File.class)));

                oneOf(stdout).println(with(matching("include")));
            }
        });
        ConfigFileTestTask task = new ConfigFileTestTask();
        task.handleTask(stdin, stdout, stderr, new String[] { TEST_TASK_NAME, "foo", "--createConfigFile" });
        mock.assertIsSatisfied();
    }

    @Test
    public void createConfigFileIfNeeded_configOptionValueProvided() throws Exception {
        mock.checking(new Expectations() {
            {
                oneOf(fileUtil).isDirectory(with(any(File.class)));
                will(returnValue(true));

                oneOf(fileUtil).exists(with(any(File.class)));
                will(returnValue(false));

                oneOf(fileUtil).createParentDirectory(with(stdout), with(any(File.class)));

                oneOf(fileUtil).writeToFile(with(stderr), with(matching("feature")), with(any(File.class)));

                oneOf(stdout).println(with(matching("include")));
            }
        });
        ConfigFileTestTask task = new ConfigFileTestTask();
        task.handleTask(stdin, stdout, stderr, new String[] { TEST_TASK_NAME, "foo", "--createConfigFile=bar/baz.xml" });
        mock.assertIsSatisfied();
    }

}
