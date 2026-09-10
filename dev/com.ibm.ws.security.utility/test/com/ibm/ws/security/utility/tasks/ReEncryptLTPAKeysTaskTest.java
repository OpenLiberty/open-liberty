/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyEncryptor;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyFileUtility;
import com.ibm.ws.security.utility.SecurityUtilityReturnCodes;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;

/**
 * Unit tests for {@link ReEncryptLTPAKeysTask}.
 *
 * <p>The LTPA key file utility is mocked so no real files or crypto operations
 * are performed. Tests exercise argument validation ({@code checkRequiredArguments})
 * and the routing / encryptor-selection logic in {@code handleTask}.
 */
public class ReEncryptLTPAKeysTaskTest {

    private static final String TEST_UTILITY_NAME = "testUtility";

    final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    final LTPAKeyFileUtility ltpaKeyFileUtil = mock.mock(LTPAKeyFileUtility.class);
    final ConsoleWrapper stdin  = mock.mock(ConsoleWrapper.class, "stdin");
    final PrintStream    stdout = mock.mock(PrintStream.class,    "stdout");
    final PrintStream    stderr = mock.mock(PrintStream.class,    "stderr");

    private ReEncryptLTPAKeysTask task;

    @Factory
    public static Matcher<String> stringContaining(String... substrings) {
        return new StringContainsMatcher(substrings);
    }

    static class StringContainsMatcher extends TypeSafeMatcher<String> {
        private final String[] substrings;

        StringContainsMatcher(String... substrings) {
            this.substrings = substrings;
        }

        @Override
        public boolean matchesSafely(String s) {
            for (String sub : substrings) {
                if (!s.contains(sub)) return false;
            }
            return true;
        }

        @Override
        public void describeTo(Description d) {
            d.appendText("a string containing ");
            for (String sub : substrings) d.appendValue(sub);
        }
    }

    @Before
    public void setUp() {
        task = new ReEncryptLTPAKeysTask(ltpaKeyFileUtil, TEST_UTILITY_NAME);
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    // -----------------------------------------------------------------------
    // getTaskName
    // -----------------------------------------------------------------------

    @Test
    public void getTaskName_returnsReEncryptLTPAKeys() {
        assertEquals("reEncryptLTPAKeys", task.getTaskName());
    }

    // -----------------------------------------------------------------------
    // isKnownArgument
    // -----------------------------------------------------------------------

    @Test
    public void isKnownArgument_currentFile() {
        assertTrue(task.isKnownArgument("--currentFile"));
    }

    @Test
    public void isKnownArgument_newFile() {
        assertTrue(task.isKnownArgument("--newFile"));
    }

    @Test
    public void isKnownArgument_currentPassword() {
        assertTrue(task.isKnownArgument("--currentPassword"));
    }

    @Test
    public void isKnownArgument_newPassword() {
        assertTrue(task.isKnownArgument("--newPassword"));
    }

    @Test
    public void isKnownArgument_ckdsLabel() {
        assertTrue(task.isKnownArgument("--ckdsLabel"));
    }

    // -----------------------------------------------------------------------
    // checkRequiredArguments — missing file arguments
    // -----------------------------------------------------------------------

    @Test
    public void checkRequiredArguments_missingCurrentFile_throws() {
        String[] args = { "reEncryptLTPAKeys",
                          "--newFile=ltpa-new.keys",
                          "--currentPassword=WebAS",
                          "--newPassword=NewPass" };
        try {
            task.checkRequiredArguments(args);
            fail("Expected IllegalArgumentException for missing --currentFile");
        } catch (IllegalArgumentException e) {
            assertTrue("Message must mention --currentFile, got: " + e.getMessage(),
                       e.getMessage().contains("--currentFile"));
        }
    }

    @Test
    public void checkRequiredArguments_missingNewFile_throws() {
        String[] args = { "reEncryptLTPAKeys",
                          "--currentFile=ltpa.keys",
                          "--currentPassword=WebAS",
                          "--newPassword=NewPass" };
        try {
            task.checkRequiredArguments(args);
            fail("Expected IllegalArgumentException for missing --newFile");
        } catch (IllegalArgumentException e) {
            assertTrue("Message must mention --newFile, got: " + e.getMessage(),
                       e.getMessage().contains("--newFile"));
        }
    }

    // -----------------------------------------------------------------------
    // checkRequiredArguments — key-material argument validation
    // -----------------------------------------------------------------------

    @Test
    public void checkRequiredArguments_onlyCurrentPassword_throws() {
        // Only one of the three key-material args — need exactly two.
        String[] args = { "reEncryptLTPAKeys",
                          "--currentFile=ltpa.keys",
                          "--newFile=ltpa-new.keys",
                          "--currentPassword=WebAS" };
        try {
            task.checkRequiredArguments(args);
            fail("Expected IllegalArgumentException for only one key-material arg");
        } catch (IllegalArgumentException e) {
            assertTrue("Message must mention key args, got: " + e.getMessage(),
                       e.getMessage().contains("--currentPassword") || e.getMessage().contains("--newPassword") || e.getMessage().contains("--ckdsLabel"));
        }
    }

    @Test
    public void checkRequiredArguments_allThreeKeyArgs_throws() {
        // All three supplied — ambiguous, reject.
        String[] args = { "reEncryptLTPAKeys",
                          "--currentFile=ltpa.keys",
                          "--newFile=ltpa-new.keys",
                          "--currentPassword=WebAS",
                          "--newPassword=NewPass",
                          "--ckdsLabel=MYKEY" };
        try {
            task.checkRequiredArguments(args);
            fail("Expected IllegalArgumentException for all three key-material args");
        } catch (IllegalArgumentException e) {
            assertTrue("Message must mention ckdsLabel conflict, got: " + e.getMessage(),
                       e.getMessage().contains("--ckdsLabel"));
        }
    }

    @Test
    public void checkRequiredArguments_currentAndNewPassword_valid() {
        // Exactly two: currentPassword + newPassword — must not throw.
        String[] args = { "reEncryptLTPAKeys",
                          "--currentFile=ltpa.keys",
                          "--newFile=ltpa-new.keys",
                          "--currentPassword=WebAS",
                          "--newPassword=NewPass" };
        task.checkRequiredArguments(args);
    }

    @Test
    public void checkRequiredArguments_currentPasswordAndCkdsLabel_valid() {
        // Exactly two: currentPassword + ckdsLabel — must not throw.
        String[] args = { "reEncryptLTPAKeys",
                          "--currentFile=ltpa.keys",
                          "--newFile=ltpa-new.keys",
                          "--currentPassword=WebAS",
                          "--ckdsLabel=MYKEY" };
        task.checkRequiredArguments(args);
    }

    @Test
    public void checkRequiredArguments_ckdsLabelAndNewPassword_valid() {
        // Exactly two: ckdsLabel + newPassword — must not throw.
        String[] args = { "reEncryptLTPAKeys",
                          "--currentFile=ltpa.keys",
                          "--newFile=ltpa-new.keys",
                          "--ckdsLabel=MYKEY",
                          "--newPassword=NewPass" };
        task.checkRequiredArguments(args);
    }

    // -----------------------------------------------------------------------
    // handleTask — password-to-password path
    // -----------------------------------------------------------------------

    /**
     * Password → password: both {@link com.ibm.ws.crypto.ltpakeyutil.KeyEncryptor}
     * instances are passed to {@code reEncryptLTPAKeysFile}; the task prints a
     * success message containing the new file name and returns OK.
     */
    @Test
    public void handleTask_passwordToPassword_callsReEncryptAndPrintsSuccess() throws Exception {
        String[] args = { "reEncryptLTPAKeys",
                          "--currentFile=ltpa.keys",
                          "--newFile=ltpa-new.keys",
                          "--currentPassword=WebAS",
                          "--newPassword=NewPass" };

        mock.checking(new Expectations() {
            {
                one(ltpaKeyFileUtil).reEncryptLTPAKeysFile(
                        with("ltpa.keys"),
                        with(any(LTPAKeyEncryptor.class)),
                        with("ltpa-new.keys"),
                        with(any(LTPAKeyEncryptor.class)));

                one(stdout).println(with(stringContaining("ltpa-new.keys")));
            }
        });

        assertEquals("Expected OK return code",
                     SecurityUtilityReturnCodes.OK,
                     task.handleTask(stdin, stdout, stderr, args));
    }

    // -----------------------------------------------------------------------
    // handleTask — wrong current password surfaces as exception
    // -----------------------------------------------------------------------

    /**
     * If the underlying {@code reEncryptLTPAKeysFile} throws (e.g. because the
     * supplied current password is wrong), the exception must propagate out of
     * {@code handleTask} rather than being silently swallowed.
     */
    @Test
    public void handleTask_wrongCurrentPassword_exceptionPropagates() throws Exception {
        String[] args = { "reEncryptLTPAKeys",
                          "--currentFile=ltpa.keys",
                          "--newFile=ltpa-new.keys",
                          "--currentPassword=WrongPassword",
                          "--newPassword=NewPass" };

        mock.checking(new Expectations() {
            {
                one(ltpaKeyFileUtil).reEncryptLTPAKeysFile(
                        with(any(String.class)),
                        with(any(LTPAKeyEncryptor.class)),
                        with(any(String.class)),
                        with(any(LTPAKeyEncryptor.class)));
                will(throwException(new javax.crypto.BadPaddingException("bad padding")));
            }
        });

        try {
            task.handleTask(stdin, stdout, stderr, args);
            fail("Expected exception to propagate from reEncryptLTPAKeysFile");
        } catch (javax.crypto.BadPaddingException e) {
            // Expected — the exception must not be swallowed.
        }
    }

}
