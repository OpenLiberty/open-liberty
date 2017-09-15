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

import static org.junit.Assert.assertNotNull;

import java.io.PrintStream;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.utility.utils.ConsoleWrapper;

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

}
