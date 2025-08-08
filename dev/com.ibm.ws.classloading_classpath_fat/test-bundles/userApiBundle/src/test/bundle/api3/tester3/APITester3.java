/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.bundle.api3.tester3;

import test.bundle.api3.a.API_A3;
import test.bundle.api3.b.API_B3;
import test.bundle.api3.c.API_C3;

/**
 *
 */
public class APITester3 {
    public static API_C3 loadC() {
        return new API_C3();
    }
    public static API_B3 loadB() {
        return loadC();
    }
    public static API_A3 loadA() {
        return loadB();
    }
}
