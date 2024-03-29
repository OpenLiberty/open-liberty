/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.webcontainer.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ApplicationErrorUtilsTest {
    @Test
    public void printTrimmedStackA() {
        Exception e = new Exception();
        e.fillInStackTrace();
        String html = ApplicationErrorUtils.getTrimmedStackHtml(e);
        // Make sure we got something back
        assertNotNull("The HTML for the stack trace shouldn't be null", html);
        assertTrue("The HTML for the stack trace should be non-trivial: " + html, html.length() > 30);
    }

    @Test
    public void printTrimmedStackForExceptionWithCause() {
        Exception e = new Exception(new NullPointerException());
        e.fillInStackTrace();
        String html = ApplicationErrorUtils.getTrimmedStackHtml(e);
        // Make sure we got something back
        assertNotNull("The HTML for the stack trace shouldn't be null", html);
        assertTrue("The HTML for the stack trace should include a Caused by clause: " + html, html.contains("Caused by"));
    }
}
