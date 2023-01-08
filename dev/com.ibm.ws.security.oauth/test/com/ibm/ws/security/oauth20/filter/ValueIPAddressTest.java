/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.filter;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ValueIPAddressTest {

    @Test
    public void unknownHostExceptionMessage() {
        try {
            new ValueIPAddress("testUnknownHostIBM");
        } catch (FilterException e) {
            assertTrue("Expected error message was not logged, but was instead: " + e.getMessage(),
                       e.getMessage().contains("CWTAI0045E"));
        }
    }

}
