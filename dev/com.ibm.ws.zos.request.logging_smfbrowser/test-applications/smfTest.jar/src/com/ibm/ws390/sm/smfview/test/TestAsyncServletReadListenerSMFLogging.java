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
package com.ibm.ws390.sm.smfview.test;

/**
 * Used by read listener tests where the response size might be zero.
 */
public class TestAsyncServletReadListenerSMFLogging extends TestAsyncServletSMFLogging {
    public TestAsyncServletReadListenerSMFLogging() {
        super(0, "TestAsyncServletReadListenerSMFLogging");
    }
}
