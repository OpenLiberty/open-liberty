/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.fallback;

import javax.ejb.Stateless;

import org.eclipse.microprofile.faulttolerance.Fallback;

import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.TestException;

@Stateless
public class FallbackOnEJB {

    public static final int FROM_FALL_BACK_METHOD = 1;

    @Fallback(fallbackMethod = "fallbackMethod")
    public int test() throws TestException {
        throw new TestException();
    }

    public int fallbackMethod() {
        return FROM_FALL_BACK_METHOD;
    }

}
