/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.reporting.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReporterTaskTest {

    @Test
    public void testGetCause() {
        Throwable message1 = new Exception("TEST1");

        String response = ReporterTask.buildExceptionMessage(message1);

        assertEquals("java.lang.Exception: TEST1", response);
    }

    @Test
    public void testGetMultipeCauses() {
        Throwable message1 = new Exception("TEST1");
        Throwable message2 = new Exception("TEST2", message1);
        Throwable message3 = new Exception("TEST3", message2);

        String response = ReporterTask.buildExceptionMessage(message3);

        assertEquals("java.lang.Exception: TEST3: java.lang.Exception: TEST2: java.lang.Exception: TEST1", response);
    }

}
