/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.bundle.api1.tester1;

import test.bundle.api1.a.API_A1;
import test.bundle.api1.b.API_B1;
import test.bundle.api1.c.API_C1;

/**
 *
 */
public class APITester1 {
    public static API_C1 loadC() {
        return new API_C1();
    }
    public static API_B1 loadB() {
        return loadC();
    }
    public static API_A1 loadA() {
        return loadB();
    }
}
