/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.injectparameters;

import static org.junit.Assert.assertEquals;

public class TestResources {

    public static final String EXPECTED_PREFIX = "test";
    public static final int EXPECTED_SIZE = 16;

    private final String[] values;

    public TestResources(String... values) {
        this.values = values;
    }

    public void validate() {
        assertEquals(EXPECTED_SIZE, values.length);
        for (int i = 1; i <= EXPECTED_SIZE; i++) {
            assertEquals(EXPECTED_PREFIX + i, values[i - 1]);
        }
    }
}
