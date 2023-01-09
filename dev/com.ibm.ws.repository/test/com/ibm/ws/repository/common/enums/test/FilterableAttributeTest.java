/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.repository.common.enums.test;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.ibm.ws.repository.common.enums.FilterableAttribute;

public class FilterableAttributeTest {

    @Test
    public void testGetType() {

        for (FilterableAttribute attr : FilterableAttribute.values()) {
            Class<?> type = attr.getType();
            assertNotNull("All enum instances should have a real type '" + attr.getAttributeName() + "' did not", type);
        }

    }

}
