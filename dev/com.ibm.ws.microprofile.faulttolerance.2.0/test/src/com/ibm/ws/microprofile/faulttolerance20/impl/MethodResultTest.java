/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance20.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MethodResultTest {

    @Test
    public void testSuccessfulMethodResult() {
        MethodResult<String> result = MethodResult.success("test");
        assertFalse("Sucessful result returned true on isFailure", result.isFailure());
        assertEquals("Sucessful result contained a failure", null, result.getFailure());
        assertThat("Sucessful result had the wrong toString", result.toString(), containsString("Method returned value"));
        assertEquals("Sucessful result had the wrong result", "test", result.getResult());
    }

    @Test
    public void testUnsuccessfulMethodResult() {
        Throwable testThrowable = new NullPointerException("test");
        MethodResult<String> result = MethodResult.failure(testThrowable);
        assertTrue("Unsucessful result returned false on isFailure", result.isFailure());
        assertEquals("Unsucessful result contained the wrong failure", testThrowable, result.getFailure());
        assertThat("Unsucessful result had the wrong toString", result.toString(), containsString("Method threw exeception"));
        assertEquals("Unsucessful result contained an actual result",  null, result.getResult());
    }

}
