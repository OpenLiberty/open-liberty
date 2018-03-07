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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RolePlayerTest {

    @Test
    public void isMultiValuedProperty() {
        RolePlayer entity = new RolePlayer();
        isMultiValuedProperty(entity);
    }

    /**
     * Run 'isMultiValuedProperty' tests for RolePlayer entities.
     *
     * @param entity The entity to test.
     */
    public static void isMultiValuedProperty(RolePlayer entity) {
        assertTrue(entity.isMultiValuedProperty("partyRoles"));

        /*
         * Check super class properties.
         */
        EntityTest.isMultiValuedProperty(entity);
    }
}
