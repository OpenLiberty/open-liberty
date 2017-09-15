/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.wim.base_test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.wsspi.security.wim.model.Group;

/**
 *
 */
public class GroupTest {

    @Test
    public void testCaseSentitive() {
        Group group = new Group();
        assertEquals("String", group.getDataType("cn"));
    }

    @Test
    public void testCaseInSentitive() {
        Group group = new Group();
        assertEquals(null, group.getDataType("CN"));
    }
}
