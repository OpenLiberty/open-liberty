/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.bundle.api4.tester4;

import test.bundle.api4.a.API_A4;
import test.bundle.api4.b.API_B4;
import test.bundle.api4.c.API_C4;

/**
 *
 */
public class APITester4 {
    public static API_C4 loadC() {
        return new API_C4();
    }
    public static API_B4 loadB() {
        return loadC();
    }
    public static API_A4 loadA() {
        return loadB();
    }
}
