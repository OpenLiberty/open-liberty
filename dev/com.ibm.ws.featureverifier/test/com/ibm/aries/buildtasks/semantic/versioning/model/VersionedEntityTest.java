/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
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
