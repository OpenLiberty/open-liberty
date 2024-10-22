/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.test.merge.parts;

public class TestUtils {

    private static TestUtil current;

    public static TestUtil current() {
        return current;
    }

    public static void setCurrent(TestUtil newCurrent) {
        current = newCurrent;
    }
}
