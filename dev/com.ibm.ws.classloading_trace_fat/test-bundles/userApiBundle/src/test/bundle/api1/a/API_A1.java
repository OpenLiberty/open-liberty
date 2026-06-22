/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.bundle.api1.a;

/**
 * Test API class for classloading trace testing
 */
public class API_A1 {
    public void testA() {

    }

    /**
     * Returns a message identifying this class
     * @return message containing class name
     */
    public String getMessage() {
        return "Message from API_A1 class";
    }
}
