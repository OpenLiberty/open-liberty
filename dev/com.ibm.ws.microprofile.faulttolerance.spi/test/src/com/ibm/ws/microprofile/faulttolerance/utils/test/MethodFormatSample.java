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
package com.ibm.ws.microprofile.faulttolerance.utils.test;

import java.util.concurrent.Future;

import com.ibm.ws.microprofile.faulttolerance.utils.test.TestFormatMethod;

/**
 * Sample class with methods to support {@link TestFormatMethod}
 */
public class MethodFormatSample {

    public void simpleMethod() {}

    public void methodWithParameters(String a, int b) {}

    public void genericParameter(Future<String> future) {}

    public static class InnerClass {
        public void innerClassMethod() {}
    }
}
