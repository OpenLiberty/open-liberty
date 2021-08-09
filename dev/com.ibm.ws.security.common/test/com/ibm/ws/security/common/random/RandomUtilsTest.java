/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import test.common.SharedOutputManager;

public class RandomUtilsTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.common.*=all");

    // 26 characters (uppercase and lowercase) + 10 digits = 62 alphanumeric characters
    private final int ALPHANUM_CHAR_COUNT = 62;

    private final char[] alphaNumChars = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z'
    };

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Before
    public void beforeTest() {
        System.out.println("Entering test: " + testName.getMethodName());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link RandomUtils#getRandomAlphaNumeric(int)}</li>
     * </ul>
     */
    @Test
    public void testGetRandomAlphaNumeric() {
        try {
            String result = RandomUtils.getRandomAlphaNumeric(-1);
            assertEquals("", result);

            result = RandomUtils.getRandomAlphaNumeric(0);
            assertEquals("", result);

            // Ensure that only alphanumeric characters are returned
            Set<Character> generatedChars = new HashSet<Character>();
            int iterations = 5000;
            for (int i = 0; i < iterations; i++) {
                result = RandomUtils.getRandomAlphaNumeric(1);
                assertEquals("Resulting string should be of length 1.", 1, result.length());
                char resultC = result.charAt(0);
                if (!((resultC >= '0' && resultC <= '9') || (resultC >= 'A' && resultC <= 'Z') || (resultC >= 'a' && resultC <= 'z'))) {
                    fail("Result [" + result + "] was outside of alphanumeric range.");
                }
                generatedChars.add(resultC);
            }
            // Ensure that after so many iterations, all possible alphanumeric characters should have come up at least once
            if (generatedChars.size() != ALPHANUM_CHAR_COUNT) {
                List<Character> missingChars = new ArrayList<Character>();
                for (char c : alphaNumChars) {
                    if (!generatedChars.contains(c)) {
                        missingChars.add(c);
                    }
                }
                fail("Failed to generate the following characters after " + iterations + " iterations: " + Arrays.toString(missingChars.toArray(new Character[missingChars.size()])));
            }

            // Pick a few random string lengths and make sure a string of the correct size is created each time
            Random r = new Random();
            iterations = 20;
            for (int i = 0; i < iterations; i++) {
                int maxLength = 40;
                int randomLength = r.nextInt(maxLength);
                result = RandomUtils.getRandomAlphaNumeric(randomLength);
                assertEquals("Did not get the expected random string length.", randomLength, result.length());
            }

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
