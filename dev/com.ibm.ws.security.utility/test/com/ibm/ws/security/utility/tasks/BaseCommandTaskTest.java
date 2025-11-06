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

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException;
import com.ibm.ws.crypto.util.AesConfigFileParser;
import com.ibm.ws.crypto.util.UnsupportedConfigurationException;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.security.utility.SecurityUtilityReturnCodes;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;
import com.ibm.wsspi.security.crypto.PasswordEncryptException;

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

        @Override
        protected List<Set<String>> getExclusiveArguments() {
            return new ArrayList<Set<String>>();
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

    @Test
    public void validateEncodingProperties() throws Exception {
        Path tempFile = Files.createTempFile("test-keys", ".xml");

        try (MockedStatic<ProductInfo> productInfoMock = Mockito.mockStatic(ProductInfo.class);) {
            productInfoMock.when(() -> ProductInfo.getBetaEdition()).thenReturn(true);

            String testKey = "test";
            testEncodingPropertyConversion("--base64Key", PasswordUtil.PROPERTY_AES_KEY, testKey);
            testEncodingPropertyConversion("--passwordBase64Key", PasswordUtil.PROPERTY_AES_KEY, testKey);
            testEncodingPropertyConversion("--key", PasswordUtil.PROPERTY_CRYPTO_KEY, testKey);
            testEncodingPropertyConversion("--passwordKey", PasswordUtil.PROPERTY_CRYPTO_KEY, testKey);

            String aesKey = testKey;

            String xml = "<server>\n<variable name=\"wlp.aes.encryption.key\" value=\"" + aesKey + "\" />\n</server>";
            Files.write(tempFile, xml.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
            String aesConfigFileParam = "--aesConfigFile";
            testEncodingPropertyConversion(aesConfigFileParam, tempFile.toString(), PasswordUtil.PROPERTY_AES_KEY, aesKey);

            xml = "<server>\n<variable name=\"wlp.password.encryption.key\" value=\"" + aesKey + "\" />\n</server>";
            Files.write(tempFile, xml.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
            testEncodingPropertyConversion(aesConfigFileParam, tempFile.toString(), PasswordUtil.PROPERTY_CRYPTO_KEY, aesKey);

        } finally {
            Files.delete(tempFile);
        }
    }

    /**
     * This helper method checks that the input parameter is properly converted to the appropriate password util property where
     * the input value is expected to be the same as the property value.
     *
     * @param inputParam
     * @param propertyName
     * @param inputAndPropertyValue
     * @throws Exception
     */
    private void testEncodingPropertyConversion(String inputParam, String propertyName, String inputAndPropertyValue) throws Exception {
        testEncodingPropertyConversion(inputParam, inputAndPropertyValue, propertyName, inputAndPropertyValue);
    }

    /**
     * This is a helper method that tests the conversion of input parameters are converted to the appropriate password util properties
     *
     * @param inputParam    a securityUtility task parameter
     * @param inputValue    the value specified by inputParam
     * @param propertyName  The password utility property name as a result of the input parameter
     * @param propertyValue the passwordUtility property value as a result of the input parameter
     * @throws Exception
     */
    private void testEncodingPropertyConversion(String inputParam, String inputValue, String propertyName, String propertyValue) throws Exception {
        try (PrintStream printStreamMock = Mockito.mock(PrintStream.class)) {
            Map<String, String> args = new HashMap<>();
            args.put(inputParam, inputValue);
            Map<String, String> props = BaseCommandTask.convertToProperties(args, printStreamMock);
            assertTrue(props.get(propertyName).equals(propertyValue));
        }
    }

    @Test
    public void testHandleAesConfigFile() {
        String fileName = "test";
        Throwable[] parseExceptions = { new PasswordEncryptException(), new UnsupportedConfigurationException(),
                                        new UnsupportedCryptoAlgorithmException() };
        try (MockedStatic<AesConfigFileParser> aesConfigFileMock = Mockito.mockStatic(AesConfigFileParser.class)) {
            aesConfigFileMock.when(() -> AesConfigFileParser.parseAesEncryptionFile(fileName)).thenThrow(parseExceptions);
            for (int i = 0; i < parseExceptions.length; i++) {
                try {
                    BaseCommandTask.handleAesConfigFile(new HashMap<String, String>(), fileName);
                } catch (PasswordEncryptException e) {
                    assertEquals("PasswordEncryptException should occur when PasswordEncryptException is thrown by parseAesEncryptionFile.", 0, i);
                } catch (IllegalArgumentException e) {
                    assertTrue("IllegalArgumentException should occur when UnsupportedConfigurationException or UnsupportedCryptoAlgorithmException is thrown by parseAesEncryptionFile.",
                               i == 1 || i == 2);
                }
            }
        }
    }

}
