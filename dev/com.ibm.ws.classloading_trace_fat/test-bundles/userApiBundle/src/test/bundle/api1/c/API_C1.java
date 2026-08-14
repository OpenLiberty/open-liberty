/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.bundle.api1.c;

import test.bundle.api1.b.API_B1;

/**
 * Test API class for classloading trace testing
 */
public class API_C1 extends API_B1 {
    public void testC() {

    }

    /**
     * Returns a message identifying this class
     * @return message containing class name
     */
    @Override
    public String getMessage() {
        return "Message from API_C1 class";
    }
}
