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

public class OrgContainerTest {

    @Test
    public void isMultiValuedProperty() {
        OrgContainer entity = new OrgContainer();
        isMultiValuedProperty(entity);
    }

    /**
     * Run 'isMultiValuedProperty' tests for OrgContainer entities.
     *
     * @param entity The entity to test.
     */
    public static void isMultiValuedProperty(OrgContainer entity) {
        assertFalse(entity.isMultiValuedProperty("o"));
        assertFalse(entity.isMultiValuedProperty("ou"));
        assertFalse(entity.isMultiValuedProperty("dc"));
        assertFalse(entity.isMultiValuedProperty("cn"));
        assertTrue(entity.isMultiValuedProperty("telephoneNumber"));
        assertTrue(entity.isMultiValuedProperty("facsimileTelephoneNumber"));
        assertTrue(entity.isMultiValuedProperty("postalAddress"));
        assertTrue(entity.isMultiValuedProperty("l"));
        assertTrue(entity.isMultiValuedProperty("localityName"));
        assertTrue(entity.isMultiValuedProperty("st"));
        assertTrue(entity.isMultiValuedProperty("stateOrProvinceName"));
        assertTrue(entity.isMultiValuedProperty("street"));
        assertTrue(entity.isMultiValuedProperty("postalCode"));
        assertFalse(entity.isMultiValuedProperty("businessAddress"));
        assertTrue(entity.isMultiValuedProperty("description"));
        assertTrue(entity.isMultiValuedProperty("businessCategory"));
        assertTrue(entity.isMultiValuedProperty("seeAlso"));

        /*
         * Check super class properties.
         */
        PartyTest.isMultiValuedProperty(entity);
    }
}
