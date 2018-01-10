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
package com.ibm.ws.repository.resolver.internal.resource;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import com.ibm.ws.repository.resolver.internal.resource.RequirementImpl;

/**
 * Tests for {@link RequirementImpl}
 */
public class RequirementImplTest {

    /**
     * Make sure to string works
     */
    @Test
    public void testToString() {
        String name = "Hello, World!";
        RequirementImpl testObject = new RequirementImpl(name) {

            @Override
            public String getNamespace() {
                return null;
            }

            @Override
            public Map<String, String> getDirectives() {
                return null;
            }

            @Override
            public Map<String, Object> getAttributes() {
                return null;
            }
        };
        assertEquals("The toString method should return the class name and name of the requirement", testObject.getClass().getName() + " [" + name + "]", testObject.toString());
    }

}
