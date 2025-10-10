/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.PrintStream;

import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.utility.utils.ConsoleWrapper;
import com.ibm.ws.security.utility.utils.StringStartsWithMatcher;

/**
 *
 */
public class EncodeTaskTest {
    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final ConsoleWrapper stdin = mock.mock(ConsoleWrapper.class, "stdin");
    private final PrintStream stdout = mock.mock(PrintStream.class, "stdout");
    private final PrintStream stderr = mock.mock(PrintStream.class, "stderr");
    private EncodeTask encode;
    private final String plaintext = "encodeMe";
    private final String ciphertext = "{xor}OjE8MDs6Ejo=";

    @Before
    public void setUp() {
        encode = new EncodeTask("myScript");
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    @Test
    public void getTaskHelp() {
        assertNotNull(encode.getTaskHelp());
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.EncodeTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_promptWhenNoConsole() throws Exception {
        mock.checking(new Expectations() {
            {
                one(stdin).readMaskedText("Enter text: ");
                will(returnValue(null));
                one(stdin).readMaskedText("Re-enter text: ");
                will(returnValue(null));
            }
        });
        String[] args = { "encode" };
        try {
            encode.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.EncodeTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_promptSuppliedMatch() {
        mock.checking(new Expectations() {
            {
                one(stdin).readMaskedText("Enter text: ");
                will(returnValue(plaintext));
                one(stdin).readMaskedText("Re-enter text: ");
                will(returnValue(plaintext));

                one(stdout).println(ciphertext);
            }
        });
        String[] args = { "encode", "--encoding=xor" };
        try {
            encode.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {

        }

    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.EncodeTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     */
    @Test
    public void handleTask_oneArgText() {
        mock.checking(new Expectations() {
            {
                one(stdout).println(ciphertext);
            }
        });
        String[] args = { "encode", "--encoding=xor", plaintext };
        try {
            encode.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {

        }

    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.EncodeTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_multiArgText() throws Exception {
        String[] args = { "encode", plaintext, "extraArg" };
        try {
            encode.handleTask(stdin, stdout, stderr, args);
        } catch (Exception e) {
            throw e;
        }

    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.EncodeTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * This test exists to ensure our beta toggles are working as expected. When this feature leaves beta we can delete this test.
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void handleTask_base64KeyBetaOff() throws Exception {
        String[] args = { "encode", plaintext, "--encoding=aes", "--base64Key=pVB1v3IS07bsRBgbpoKJhB7OQZLVMFwIxBF5PrJctb0=" };
        try {
            encode.handleTask(stdin, stdout, stderr, args);
            fail();
        } catch (Exception e) {
            throw e;
        }

    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.utility.tasks.EncodeTask#handleTask(com.ibm.ws.security.utility.utils.ConsoleWrapper, java.io.PrintStream, java.io.PrintStream, java.lang.String[])}
     * .
     *
     * @throws Exception
     */
    @Test
    public void handleTask_base64Key_betaon() throws Exception {
        mock.checking(new Expectations() {
            {
                // we will not know the actual secret value, but if the command runs successfully it will return an {aes} password.
                one(stdout).println(with(aStringStartsWith("{aes}")));
            }
        });
        String[] args = { "encode", "--encoding=aes", "--base64Key=pVB1v3IS07bsRBgbpoKJhB7OQZLVMFwIxBF5PrJctb0=", plaintext };
        try {
            System.setProperty("com.ibm.ws.beta.edition", "true");
            encode.handleTask(stdin, stdout, stderr, args);
        } finally

        {
            System.setProperty("com.ibm.ws.beta.edition", "false");
        }

    }

    public static Matcher<String> aStringStartsWith(String prefix) {
        return new StringStartsWithMatcher(prefix);
    }
}
