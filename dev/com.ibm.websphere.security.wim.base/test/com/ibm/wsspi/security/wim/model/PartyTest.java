/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.wim.model;

import org.junit.Test;

public class PartyTest {

    @Test
    public void isMultiValuedProperty() {
        Party entity = new Party();
        isMultiValuedProperty(entity);
    }

    /**
     * Run 'isMultiValuedProperty' tests for Party entities.
     *
     * @param entity The entity to test.
     */
    public static void isMultiValuedProperty(Party entity) {
        /*
         * Check super class properties.
         */
        RolePlayerTest.isMultiValuedProperty(entity);
    }
}
