/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.wsspi.security.wim.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CountryTest {

    @Test
    public void isMultiValuedProperty() {
        Country entity = new Country();
        assertFalse(entity.isMultiValuedProperty("c"));
        assertFalse(entity.isMultiValuedProperty("countryName"));
        assertTrue(entity.isMultiValuedProperty("description"));

        /*
         * Check super class properties.
         */
        GeographicLocationTest.isMultiValuedProperty(entity);
    }
}
