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

public class PersonAccountTest {

    @Test
    public void isMultiValuedProperty() {
        PersonAccount entity = new PersonAccount();
        PersonAccount.addExtendedProperty("extendedProperty1", "String", false, null);
        PersonAccount.addExtendedProperty("extendedProperty2", "String", true, null);

        /*
         * Test standard properties.
         */
        assertFalse(entity.isMultiValuedProperty("uid"));
        assertFalse(entity.isMultiValuedProperty("cn"));
        assertFalse(entity.isMultiValuedProperty("sn"));
        assertFalse(entity.isMultiValuedProperty("preferredLanguage"));
        assertTrue(entity.isMultiValuedProperty("displayName"));
        assertTrue(entity.isMultiValuedProperty("initials"));
        assertFalse(entity.isMultiValuedProperty("mail"));
        assertFalse(entity.isMultiValuedProperty("ibmPrimaryEmail"));
        assertTrue(entity.isMultiValuedProperty("jpegPhoto"));
        assertFalse(entity.isMultiValuedProperty("labeledURI"));
        assertTrue(entity.isMultiValuedProperty("carLicense"));
        assertTrue(entity.isMultiValuedProperty("telephoneNumber"));
        assertTrue(entity.isMultiValuedProperty("facsimileTelephoneNumber"));
        assertTrue(entity.isMultiValuedProperty("pager"));
        assertTrue(entity.isMultiValuedProperty("mobile"));
        assertTrue(entity.isMultiValuedProperty("homePostalAddress"));
        assertTrue(entity.isMultiValuedProperty("postalAddress"));
        assertTrue(entity.isMultiValuedProperty("roomNumber"));
        assertTrue(entity.isMultiValuedProperty("l"));
        assertTrue(entity.isMultiValuedProperty("localityName"));
        assertTrue(entity.isMultiValuedProperty("st"));
        assertTrue(entity.isMultiValuedProperty("stateOrProvinceName"));
        assertTrue(entity.isMultiValuedProperty("street"));
        assertTrue(entity.isMultiValuedProperty("postalCode"));
        assertTrue(entity.isMultiValuedProperty("city"));
        assertFalse(entity.isMultiValuedProperty("employeeType"));
        assertFalse(entity.isMultiValuedProperty("employeeNumber"));
        assertTrue(entity.isMultiValuedProperty("manager"));
        assertTrue(entity.isMultiValuedProperty("secretary"));
        assertTrue(entity.isMultiValuedProperty("departmentNumber"));
        assertTrue(entity.isMultiValuedProperty("title"));
        assertTrue(entity.isMultiValuedProperty("ibmJobTitle"));
        assertTrue(entity.isMultiValuedProperty("c"));
        assertTrue(entity.isMultiValuedProperty("countryName"));
        assertTrue(entity.isMultiValuedProperty("givenName"));
        assertFalse(entity.isMultiValuedProperty("homeStreet"));
        assertFalse(entity.isMultiValuedProperty("homeCity"));
        assertFalse(entity.isMultiValuedProperty("homeStateOrProvinceName"));
        assertFalse(entity.isMultiValuedProperty("homePostalCode"));
        assertFalse(entity.isMultiValuedProperty("homeCountryName"));
        assertFalse(entity.isMultiValuedProperty("businessStreet"));
        assertFalse(entity.isMultiValuedProperty("businessCity"));
        assertFalse(entity.isMultiValuedProperty("businessStateOrProvinceName"));
        assertFalse(entity.isMultiValuedProperty("businessPostalCode"));
        assertFalse(entity.isMultiValuedProperty("businessCountryName"));
        assertTrue(entity.isMultiValuedProperty("description"));
        assertTrue(entity.isMultiValuedProperty("businessCategory"));
        assertTrue(entity.isMultiValuedProperty("seeAlso"));
        assertFalse(entity.isMultiValuedProperty("kerberosId"));
        assertFalse(entity.isMultiValuedProperty("photoURL"));
        assertFalse(entity.isMultiValuedProperty("photoURLThumbnail"));
        assertFalse(entity.isMultiValuedProperty("middleName"));
        assertFalse(entity.isMultiValuedProperty("honorificPrefix"));
        assertFalse(entity.isMultiValuedProperty("honorificSuffix"));
        assertFalse(entity.isMultiValuedProperty("nickName"));
        assertFalse(entity.isMultiValuedProperty("profileUrl"));
        assertFalse(entity.isMultiValuedProperty("timezone"));
        assertFalse(entity.isMultiValuedProperty("locale"));
        assertTrue(entity.isMultiValuedProperty("ims"));
        assertFalse(entity.isMultiValuedProperty("active"));

        /*
         * Check extended properties.
         */
        assertFalse(entity.isMultiValuedProperty("extendedProperty1"));
        assertTrue(entity.isMultiValuedProperty("extendedProperty2"));

        /*
         * Check super class properties.
         */
        LoginAccountTest.isMultiValuedProperty(entity);
    }
}
