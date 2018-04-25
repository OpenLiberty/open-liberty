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

public class EntityTest {

    @Test
    public void isMultiValuedProperty() {
        Entity entity = new Entity();
        isMultiValuedProperty(entity);
    }

    /**
     * Run 'isMultiValuedProperty' tests for Entity entities.
     *
     * @param entity The entity to test.
     */
    public static void isMultiValuedProperty(Entity entity) {
        assertFalse(entity.isMultiValuedProperty("identifier"));
        assertTrue(entity.isMultiValuedProperty("viewIdentifiers"));
        assertFalse(entity.isMultiValuedProperty("parent"));
        assertTrue(entity.isMultiValuedProperty("children"));
        assertTrue(entity.isMultiValuedProperty("groups"));
        assertFalse(entity.isMultiValuedProperty("createTimestamp"));
        assertFalse(entity.isMultiValuedProperty("modifyTimestamp"));
        assertFalse(entity.isMultiValuedProperty("entitlementInfo"));
        assertFalse(entity.isMultiValuedProperty("changeType"));
    }
}
