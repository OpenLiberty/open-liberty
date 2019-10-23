/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.aries.buildtasks.semantic.versioning.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 *
 */
public class VersionedEntityTest {
    @Test
    public void testVersionedEntityStandard() {
        VersionedEntity ve = new VersionedEntity("name", "1.0.0");
        assertEquals("Name did not match", "name", ve.getName());
        assertEquals("Version did not match", "1.0.0", ve.getVersion());
    }

    @Test
    public void testVersionedEntityExpandVersion() {
        //shorter versions should be expanded.
        VersionedEntity shortVer = new VersionedEntity("name", "1.0");
        assertEquals("Name did not match", "name", shortVer.getName());
        assertEquals("Version did not match", "1.0.0", shortVer.getVersion());
    }

    @Test
    public void testVersionedEntityNullPreserve() {
        //null should be preserved
        VersionedEntity nullP = new VersionedEntity("name", null);
        assertNull("Version should be null", nullP.getVersion());
        nullP = new VersionedEntity("name", "");
        assertNull("Version should be null", nullP.getVersion());
        nullP = new VersionedEntity("name", "  ");
        assertNull("Version should be null", nullP.getVersion());
    }
}
