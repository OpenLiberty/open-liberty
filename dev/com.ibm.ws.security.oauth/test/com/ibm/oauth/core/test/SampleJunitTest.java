/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.test;

import junit.framework.TestCase;

public class SampleJunitTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSampleGood() {
        // Do nothing, method completes successfully
    }

    /*
     * public void testSampleBad() {
     * // fail intentionally for a negative example
     * fail("Intentional fail message");
     * }
     */

    public void testSampleAssertions() {
        // Show usage of some JUnit assertions
        String testMessage = "test message";
        assertNotNull(testMessage);
        assertEquals(testMessage, "test" + ' ' + "message");
        assertTrue(("test" + ' ' + "message").equals(testMessage));
    }

}
