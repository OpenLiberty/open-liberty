/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.bundle.api2.tester2;

import test.bundle.api2.a.API_A2;
import test.bundle.api2.b.API_B2;
import test.bundle.api2.c.API_C2;

/**
 *
 */
public class APITester2 {
    public static API_C2 loadC() {
        return new API_C2();
    }
    public static API_B2 loadB() {
        return loadC();
    }
    public static API_A2 loadA() {
        return loadB();
    }
}
