/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.utility.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.ibm.ws.crypto.util.AESKeyManager;
import com.ibm.ws.security.utility.IFileUtility;
import com.ibm.ws.security.utility.tasks.GenerateAesKeyTask.PasswordEncryptionConfigBuilder;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;

/**
 *
 */
public class GenerateAesKeyTest {

    private final ConsoleWrapper stdin = mock(ConsoleWrapper.class, "stdin");
    private final PrintStream stdout = mock(PrintStream.class, "stdout");
    private final PrintStream stderr = mock(PrintStream.class, "stderr");
    final IFileUtility fileUtil = mock(IFileUtility.class);
    private GenerateAesKeyTask generate;

    @Before
    public void setUp() {
        generate = new GenerateAesKeyTask(fileUtil, "myScript");
    }

    @After
    public void tearDown() {
        reset(fileUtil);
    }

    @Test
    public void testNoKeySpecifiedConfigFileSpecified() throws Exception {

        try (MockedStatic<PasswordEncryptionConfigBuilder> xmlBuilder = Mockito.mockStatic(PasswordEncryptionConfigBuilder.class, Mockito.CALLS_REAL_METHODS)) {
            String outfile = "/path/keys.xml";
            generate.handleTask(stdin, stdout, stderr, new String[] { GenerateAesKeyTask.TASK_NAME, "--createConfigFile=" + outfile });
            xmlBuilder.verify(() -> PasswordEncryptionConfigBuilder.generateRandomAes256Key(), times(1));
            xmlBuilder.verifyNoMoreInteractions();
            verify(fileUtil, times(1)).writeToFile(any(), anyString(), any());

        }
    }

    @Test
    public void testKeySpecifiedConfigFileSpecified() throws Exception {

        String pass = "passw0rd";
        String expectedKey = "lcJWjIt38ZjBBvYfNWLEgp/I0DQFTbFmA5zFl6zCU30=";
        String outFile = "/path/keys.xml";
        StringBuilder xml = new StringBuilder();
        xml.append("<server>\n");
        xml.append("    <variable name=\"").append(AESKeyManager.NAME_WLP_BASE64_AES_ENCRYPTION_KEY).append("\" value=\"").append(expectedKey).append("\" />\n");
        xml.append("</server>");
        try (MockedStatic<PasswordEncryptionConfigBuilder> xmlBuilder = Mockito.mockStatic(PasswordEncryptionConfigBuilder.class, Mockito.CALLS_REAL_METHODS)) {
            generate.handleTask(stdin, stdout, stderr, new String[] { GenerateAesKeyTask.TASK_NAME, "--key=" + pass, "--createConfigFile=/path/keys.xml" });
            xmlBuilder.verify(() -> PasswordEncryptionConfigBuilder.generateAes256KeyWithPBKDF2(pass), times(1));
            xmlBuilder.verifyNoMoreInteractions();
            verify(fileUtil, times(1)).writeToFile(stderr, xml.toString(), new File(outFile));
        }
    }

    @Test
    public void testKeySpecifiedNoConfigFile() throws Exception {

        String pass = "passw0rd";
        String expectedKey = "lcJWjIt38ZjBBvYfNWLEgp/I0DQFTbFmA5zFl6zCU30=";
        try (MockedStatic<PasswordEncryptionConfigBuilder> xmlBuilder = Mockito.mockStatic(PasswordEncryptionConfigBuilder.class, Mockito.CALLS_REAL_METHODS)) {
            generate.handleTask(stdin, stdout, stderr, new String[] { GenerateAesKeyTask.TASK_NAME, "--key=" + pass });
            xmlBuilder.verify(() -> PasswordEncryptionConfigBuilder.generateAes256KeyWithPBKDF2(pass), times(1));
            xmlBuilder.verifyNoMoreInteractions();
            verify(stdout, times(1)).println(expectedKey);
        }
    }

    @Test
    public void testNoKeyNoConfigFile() throws Exception {

        try (MockedStatic<PasswordEncryptionConfigBuilder> xmlBuilder = Mockito.mockStatic(PasswordEncryptionConfigBuilder.class, Mockito.CALLS_REAL_METHODS)) {
            generate.handleTask(stdin, stdout, stderr, new String[] { GenerateAesKeyTask.TASK_NAME, });
            xmlBuilder.verify(() -> PasswordEncryptionConfigBuilder.generateRandomAes256Key(), times(1));
            xmlBuilder.verifyNoMoreInteractions();
            verify(stdout, times(1)).println(anyString());
        }
    }

    @Test
    public void testEmptyKey() throws Exception {

        try (MockedStatic<PasswordEncryptionConfigBuilder> xmlBuilder = Mockito.mockStatic(PasswordEncryptionConfigBuilder.class, Mockito.CALLS_REAL_METHODS)) {
            generate.handleTask(stdin, stdout, stderr, new String[] { GenerateAesKeyTask.TASK_NAME, "--key=" });
            fail("task should throw an exception if key isn't specified");
        } catch (IllegalArgumentException iae) {
            assertEquals("Wrong message returned when specifying an empty key.", BaseCommandTask.getMessage("missingValue", "--key"), iae.getMessage());
        }
    }

    @Test
    public void testEmptyConfigFile() throws Exception {

        try (MockedStatic<PasswordEncryptionConfigBuilder> xmlBuilder = Mockito.mockStatic(PasswordEncryptionConfigBuilder.class, Mockito.CALLS_REAL_METHODS)) {
            generate.handleTask(stdin, stdout, stderr, new String[] { GenerateAesKeyTask.TASK_NAME, "--createConfigFile=" });
            fail("task should throw an exception if config file isn't specified");
        } catch (IllegalArgumentException iae) {
            assertEquals("Wrong message returned when specifying an empty value for --createConfigFile.", BaseCommandTask.getMessage("missingValue", "--createConfigFile"),
                         iae.getMessage());
        }
    }

    @Test
    public void testUnknownArg() {
        String unknownArg = "testarg=abc";

        try {
            generate.handleTask(stdin, stdout, stderr, new String[] { GenerateAesKeyTask.TASK_NAME, unknownArg });
            fail("task should output invalidArg message");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("Wrong message returned when specifying an empty key.", BaseCommandTask.getMessage("invalidArg", unknownArg), e.getMessage());

        }
    }

    @Test
    public void testUnknownParam() {
        String badParam = "--testarg";
        String unknownArg = badParam + "=abc";

        try {
            generate.handleTask(stdin, stdout, stderr, new String[] { GenerateAesKeyTask.TASK_NAME, unknownArg });
            fail("task should output invalidArg message");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("Wrong message returned when specifying an empty key.", BaseCommandTask.getMessage("invalidArg", badParam), e.getMessage());

        }
    }

}
