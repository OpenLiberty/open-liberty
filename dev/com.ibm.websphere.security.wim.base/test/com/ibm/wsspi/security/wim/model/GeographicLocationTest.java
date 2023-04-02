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

import org.junit.Test;

public class GeographicLocationTest {

    @Test
    public void isMultiValuedProperty() {
        GeographicLocation entity = new GeographicLocation();
        isMultiValuedProperty(entity);
    }

    /**
     * Run 'isMultiValuedProperty' tests for GeographicLocation entities.
     *
     * @param entity The entity to test.
     */
    public static void isMultiValuedProperty(GeographicLocation entity) {
        /*
         * Check super class properties.
         */
        EntityTest.isMultiValuedProperty(entity);
    }
}
