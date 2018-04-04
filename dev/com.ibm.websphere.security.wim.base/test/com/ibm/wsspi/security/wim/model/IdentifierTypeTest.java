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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IdentifierTypeTest {

    /** Test {@link IdentifierType} instance 1. */
    public static final IdentifierType TEST_IDENTIFIER_1 = new IdentifierType();

    /** Test {@link IdentifierType} instance 2. */
    public static final IdentifierType TEST_IDENTIFIER_2 = new IdentifierType();

    static {
        TEST_IDENTIFIER_1.setUniqueId("uniqueId1");
        TEST_IDENTIFIER_1.setUniqueName("uniqueName1");
        TEST_IDENTIFIER_1.setExternalId("externalId1");
        TEST_IDENTIFIER_1.setExternalName("externalName1");
        TEST_IDENTIFIER_1.setRepositoryId("repositoryId1");

        TEST_IDENTIFIER_2.setUniqueId("uniqueId2");
        TEST_IDENTIFIER_2.setUniqueName("uniqueName2");
        TEST_IDENTIFIER_2.setExternalId("externalId2");
        TEST_IDENTIFIER_2.setExternalName("externalName2");
        TEST_IDENTIFIER_2.setRepositoryId("repositoryId2");
    }

    @Test
    public void testToString() {
        /*
         * Test empty object.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:IdentifierType " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new IdentifierType().toString());

        /*
         * Test fully set object.
         */
        sb = new StringBuffer();
        sb.append("<wim:IdentifierType uniqueId=\"uniqueId1\" uniqueName=\"uniqueName1\" externalId=\"externalId1\" externalName=\"externalName1\" repositoryId=\"repositoryId1\" "
                  + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), TEST_IDENTIFIER_1.toString());

        sb = new StringBuffer();
        sb.append("<wim:IdentifierType uniqueId=\"uniqueId2\" uniqueName=\"uniqueName2\" externalId=\"externalId2\" externalName=\"externalName2\" repositoryId=\"repositoryId2\" "
                  + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), TEST_IDENTIFIER_2.toString());
    }
}
