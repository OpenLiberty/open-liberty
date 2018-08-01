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
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.enablement;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;

import componenttest.app.FATServlet;

@WebServlet("/DisableEnableTest")
public class DisableEnableServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    DisableEnableClient client;

    @Test
    public void testDisableAtClassLevel() {
        // Note, Retry is disabled for DisableEnableClient using config
        // so we expect no retries here
        assertThrows(ConnectException.class, client::failWithOneRetryAgain);
        assertThat(client.getFailWithOneRetryAgainCounter(), is(1));
    }

    @Test
    public void testDisableAtClassLevelEnableAtMethodLevel() {
        // Note, Retry is disabled for DisableEnableClient but explicitly enabled for the failWithOneRetry method using config
        // so we do expect a retry here
        assertThrows(ConnectException.class, client::failWithOneRetry);
        assertThat(client.getFailWithOneRetryCounter(), is(2));
    }

    private void assertThrows(Class<? extends Exception> expected, ThrowingRunnable runnable) {
        try {
            runnable.run();
            fail("Exception not thrown, expected " + expected.getClass().getSimpleName());
        } catch (Exception e) {
            assertThat("Wrong exception type thrown", e, instanceOf(expected));
        }
    }

    @FunctionalInterface
    private static interface ThrowingRunnable {
        public void run() throws Exception;
    }

}
