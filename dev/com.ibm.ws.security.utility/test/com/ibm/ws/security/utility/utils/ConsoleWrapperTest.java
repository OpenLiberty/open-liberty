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
package com.ibm.ws.security.utility.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.ibm.ws.security.utility.utils.ConsoleWrapper;

/**
 * This has a limited amount of tests because we can not mock System.console.
 */
public class ConsoleWrapperTest {

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.ConsoleWrapper#isInputStreamAvailable()}.
     */
    @Test
    public void isConsoleAvailable_false() {
        ConsoleWrapper console = new ConsoleWrapper(null, null);
        assertFalse("Console should not be available when null",
                    console.isInputStreamAvailable());
    }

    /**
     * Test method for {@link com.ibm.ws.security.utility.utils.ConsoleWrapper#readMaskedText(java.lang.String)}.
     */
    @Test
    public void readMaskedText_noConsole() {
        ConsoleWrapper console = new ConsoleWrapper(null, System.err);
        assertNull(console.readMaskedText("My prompt"));
    }

}
