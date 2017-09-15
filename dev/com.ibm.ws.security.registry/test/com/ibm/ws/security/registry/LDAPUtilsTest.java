/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * 
 */
public class LDAPUtilsTest {

    /**
     * Test method for {@link com.ibm.ws.security.registry.LDAPUtils.getCNFromDN(java.lang.String)}.
     */
    @Test
    public void getCNFromDN_upperFieldNames() {
        String cn = LDAPUtils.getCNFromDN("CN=Phil Schlobodnik,ou=WebSphere,o=ibm");
        assertEquals("CN not extracted correctly", "Phil Schlobodnik", cn);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.LDAPUtils.getCNFromDN(java.lang.String)}.
     */
    @Test
    public void getCNFromDN_lowerFieldNames() {
        String cn = LDAPUtils.getCNFromDN("cn=Phil Schlobodnik,ou=WebSphere,o=ibm");
        assertEquals("CN not extracted correctly", "Phil Schlobodnik", cn);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.LDAPUtils.getCNFromDN(java.lang.String)}.
     */
    @Test
    public void getCNFromDN_mixedCaseFieldNames() {
        String cn = LDAPUtils.getCNFromDN("cN=Phil Schlobodnik,ou=WebSphere,o=ibm");
        assertEquals("CN not extracted correctly", "Phil Schlobodnik", cn);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.LDAPUtils.getCNFromDN(java.lang.String)}.
     */
    @Test
    public void getCNFromDN_mixedCaseFieldNames2() {
        String cn = LDAPUtils.getCNFromDN("Cn=Phil Schlobodnik,ou=WebSphere,o=ibm");
        assertEquals("CN not extracted correctly", "Phil Schlobodnik", cn);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.LDAPUtils.getCNFromDN(java.lang.String)}.
     */
    @Test
    public void getCNFromDN_spacesInDN() {
        String cn = LDAPUtils.getCNFromDN("CN= Phil Schlobodnik ,ou=WebSphere,o=ibm");
        assertEquals("CN not extracted correctly", "Phil Schlobodnik", cn);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.LDAPUtils.getCNFromDN(java.lang.String)}.
     */
    @Test
    public void getCNFromDN_spacesInDN2() {
        String cn = LDAPUtils.getCNFromDN(" CN = Phil Schlobodnik ,ou=WebSphere,o=ibm");
        assertEquals("CN not extracted correctly", "Phil Schlobodnik", cn);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.LDAPUtils.getCNFromDN(java.lang.String)}.
     */
    @Test
    public void getCNFromDN_nonStandardOrdering() {
        String cn = LDAPUtils.getCNFromDN("ou=WebSphere,CN=Phil Schlobodnik,o=ibm");
        assertEquals("CN not extracted correctly", "Phil Schlobodnik", cn);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.LDAPUtils.getCNFromDN(java.lang.String)}.
     */
    @Test
    public void getCNFromDN_nonStandardOrderingWithSapces() {
        String cn = LDAPUtils.getCNFromDN("ou=WebSphere, CN = Phil Schlobodnik ,o=ibm");
        assertEquals("CN not extracted correctly", "Phil Schlobodnik", cn);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.LDAPUtils.getCNFromDN(java.lang.String)}.
     */
    @Test
    public void getCNFromDN_cnIsLastEntry() {
        String cn = LDAPUtils.getCNFromDN("o=ibm,ou=WebSphere,CN=Phil Schlobodnik");
        assertEquals("CN not extracted correctly", "Phil Schlobodnik", cn);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.LDAPUtils.getCNFromDN(java.lang.String)}.
     */
    @Test
    public void getCNFromDN_cnIsLastEntryWithSpaces() {
        String cn = LDAPUtils.getCNFromDN("o=ibm,ou=WebSphere, CN = Phil Schlobodnik ");
        assertEquals("CN not extracted correctly", "Phil Schlobodnik", cn);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.LDAPUtils.getCNFromDN(java.lang.String)}.
     */
    @Test
    public void getCNFromDN_exteremeSpaces() {
        String cn = LDAPUtils.getCNFromDN(" CN = Phil Schlobodnik , ou = WebSphere, o = ibm");
        assertEquals("CN not extracted correctly", "Phil Schlobodnik", cn);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.LDAPUtils.getCNFromDN(java.lang.String)}.
     */
    @Test
    public void getCNFromDN_noCN() {
        String cn = LDAPUtils.getCNFromDN("ou=WebSphere,o=ibm");
        assertNull("CN is not null: " + cn, cn);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.LDAPUtils.getCNFromDN(java.lang.String)}.
     */
    @Test
    public void getCNFromDN_invalidFormat() {
        String cn = LDAPUtils.getCNFromDN("==bob");
        assertNull("CN is not null: " + cn, cn);
    }
}
