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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LoginAccountTest {

    @Test
    public void isMultiValuedProperty() {
        LoginAccount entity = new LoginAccount();
        isMultiValuedProperty(entity);
    }

    /**
     * Run 'isMultiValuedProperty' tests for LoginAccount entities.
     *
     * @param entity The entity to test.
     */
    public static void isMultiValuedProperty(LoginAccount entity) {
        assertFalse(entity.isMultiValuedProperty("principalName"));
        assertFalse(entity.isMultiValuedProperty("password"));
        assertFalse(entity.isMultiValuedProperty("realm"));
        assertTrue(entity.isMultiValuedProperty("certificate"));

        /*
         * Check super class properties.
         */
        PartyTest.isMultiValuedProperty(entity);
    }
}
