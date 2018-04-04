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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PersonTest {

    @Test
    public void isMultiValuedProperty() {
        Person entity = new Person();
        isMultiValuedProperty(entity);
    }

    public static void isMultiValuedProperty(Person entity) {
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
        assertTrue(entity.isMultiValuedProperty("homeAddress"));
        assertTrue(entity.isMultiValuedProperty("businessAddress"));
        assertTrue(entity.isMultiValuedProperty("description"));
        assertTrue(entity.isMultiValuedProperty("businessCategory"));
        assertTrue(entity.isMultiValuedProperty("seeAlso"));
        assertFalse(entity.isMultiValuedProperty("kerberosId"));
        assertFalse(entity.isMultiValuedProperty("photoURL"));
        assertFalse(entity.isMultiValuedProperty("photoURLThumbnail"));

        /*
         * Check super class properties.
         */
        PartyTest.isMultiValuedProperty(entity);
    }

    @Test
    public void testToString() {

        /*
         * Test empty entity.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:Person " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new Person().toString());

        /*
         * PersonAccount properties.
         */
        Person person = new Person();
        person.setUid("uid");
        person.setCn("cn");
        person.setSn("sn");
        person.setPreferredLanguage("preferredLanguage");
        person.set("displayName", "displayName1");
        person.set("displayName", "displayName2");
        person.set("initials", "initials");
        person.setMail("mail");
        person.setIbmPrimaryEmail("ibmPrimaryEmail");
        person.set("jpegPhoto", "jpegPhoto1".getBytes());
        person.set("jpegPhoto", "jpegPhoto2".getBytes());
        person.setLabeledURI("labeledURI");
        person.set("carLicense", "carLicense1");
        person.set("carLicense", "carLicense2");
        person.set("telephoneNumber", "telephoneNumber1");
        person.set("telephoneNumber", "telephoneNumber2");
        person.set("facsimileTelephoneNumber", "facsimileTelephoneNumber1");
        person.set("facsimileTelephoneNumber", "facsimileTelephoneNumber2");
        person.set("pager", "pager1");
        person.set("pager", "pager2");
        person.set("mobile", "mobile1");
        person.set("mobile", "mobile2");
        person.set("homePostalAddress", "homePostalAddress1");
        person.set("homePostalAddress", "homePostalAddress2");
        person.set("postalAddress", "postalAddress1");
        person.set("postalAddress", "postalAddress2");
        person.set("roomNumber", "roomNumber1");
        person.set("roomNumber", "roomNumber2");
        person.set("l", "l1");
        person.set("l", "l2");
        person.set("localityName", "localityName1");
        person.set("localityName", "localityName2");
        person.set("st", "st1");
        person.set("st", "st2");
        person.set("stateOrProvinceName", "stateOrProvinceName1");
        person.set("stateOrProvinceName", "stateOrProvinceName2");
        person.set("street", "street1");
        person.set("street", "street2");
        person.set("postalCode", "postalCode1");
        person.set("postalCode", "postalCode2");
        person.set("city", "city1");
        person.set("city", "city2");
        person.setEmployeeType("employeeType");
        person.setEmployeeNumber("employeeNumber");
        person.set("manager", IdentifierTypeTest.TEST_IDENTIFIER_1);
        person.set("manager", IdentifierTypeTest.TEST_IDENTIFIER_2);
        person.set("secretary", IdentifierTypeTest.TEST_IDENTIFIER_1);
        person.set("secretary", IdentifierTypeTest.TEST_IDENTIFIER_2);
        person.set("departmentNumber", "departmentNumber1");
        person.set("departmentNumber", "departmentNumber2");
        person.set("title", "title1");
        person.set("title", "title2");
        person.set("ibmJobTitle", "ibmJobTitle1");
        person.set("ibmJobTitle", "ibmJobTitle2");
        person.set("c", "c1");
        person.set("c", "c2");
        person.set("countryName", "countryName1");
        person.set("countryName", "countryName2");
        person.set("givenName", "givenName1");
        person.set("givenName", "givenName2");
        person.set("homeAddress", AddressTypeTest.TEST_ADDRESS_TYPE);
        person.set("homeAddress", AddressTypeTest.TEST_ADDRESS_TYPE);
        person.set("businessAddress", AddressTypeTest.TEST_ADDRESS_TYPE);
        person.set("businessAddress", AddressTypeTest.TEST_ADDRESS_TYPE);
        person.set("description", "description1");
        person.set("description", "description2");
        person.set("businessCategory", "businessCategory1");
        person.set("businessCategory", "businessCategory2");
        person.set("seeAlso", "seeAlso1");
        person.set("seeAlso", "seeAlso2");
        person.setKerberosId("kerberosId");
        person.setPhotoUrl("photoUrl");
        person.setPhotoUrlThumbnail("photoUrlThumbnail");
        RolePlayerTest.configureProperties(person);

        /*
         * Create the expected response.
         */
        sb = new StringBuffer();
        sb.append("<wim:Person " + RootTest.WIM_XMLNS + ">\n");
        sb.append("    <wim:identifier uniqueId=\"uniqueId1\" uniqueName=\"uniqueName1\" externalId=\"externalId1\" externalName=\"externalName1\" repositoryId=\"repositoryId1\"/>\n");
        sb.append("    <wim:viewIdentifiers viewName=\"viewName1\" viewEntryUniqueId=\"viewEntryUniqueId1\" viewEntryName=\"viewEntryName1\"/>\n");
        sb.append("    <wim:viewIdentifiers viewName=\"viewName2\" viewEntryUniqueId=\"viewEntryUniqueId2\" viewEntryName=\"viewEntryName2\"/>\n");
        sb.append("    <wim:parent xsi:type=\"wim:Container\">\n");
        sb.append("        <wim:cn>parent</wim:cn>\n");
        sb.append("    </wim:parent>\n");
        sb.append("    <wim:children xsi:type=\"wim:Container\">\n");
        sb.append("        <wim:cn>child1</wim:cn>\n");
        sb.append("    </wim:children>\n");
        sb.append("    <wim:children xsi:type=\"wim:Container\">\n");
        sb.append("        <wim:cn>child2</wim:cn>\n");
        sb.append("    </wim:children>\n");
        sb.append("    <wim:groups>\n");
        sb.append("        <wim:cn>group1</wim:cn>\n");
        sb.append("    </wim:groups>\n");
        sb.append("    <wim:groups>\n");
        sb.append("        <wim:cn>group2</wim:cn>\n");
        sb.append("    </wim:groups>\n");
        sb.append("    <wim:createTimestamp>" + RootTest.NOW_STRING + "</wim:createTimestamp>\n");
        sb.append("    <wim:modifyTimestamp>" + RootTest.NOW_STRING + "</wim:modifyTimestamp>\n");
        sb.append("    <wim:entitlementInfo>\n");
        sb.append("        <wim:roles>role1</wim:roles>\n");
        sb.append("        <wim:roles>role2</wim:roles>\n");
        sb.append("        <wim:entitlements method=\"method1\" object=\"object1\" attribute=\"attribute1\"/>\n");
        sb.append("        <wim:entitlements method=\"method2\" object=\"object2\" attribute=\"attribute2\"/>\n");
        sb.append("        <wim:entitlementCheckResult>true</wim:entitlementCheckResult>\n");
        sb.append("    </wim:entitlementInfo>\n");
        sb.append("    <wim:changeType>changeType</wim:changeType>\n");
        sb.append("    <wim:partyRoles>\n");
        sb.append("        <wim:parent xsi:type=\"wim:Container\">\n");
        sb.append("            <wim:cn>parent</wim:cn>\n");
        sb.append("        </wim:parent>\n");
        sb.append("    </wim:partyRoles>\n");
        sb.append("    <wim:uid>uid</wim:uid>\n");
        sb.append("    <wim:cn>cn</wim:cn>\n");
        sb.append("    <wim:sn>sn</wim:sn>\n");
        sb.append("    <wim:preferredLanguage>preferredLanguage</wim:preferredLanguage>\n");
        sb.append("    <wim:displayName>displayName1</wim:displayName>\n");
        sb.append("    <wim:displayName>displayName2</wim:displayName>\n");
        sb.append("    <wim:initials>initials</wim:initials>\n");
        sb.append("    <wim:mail>mail</wim:mail>\n");
        sb.append("    <wim:ibmPrimaryEmail>ibmPrimaryEmail</wim:ibmPrimaryEmail>\n");
        sb.append("    <wim:jpegPhoto>anBlZ1Bob3RvMQ==</wim:jpegPhoto>\n");
        sb.append("    <wim:jpegPhoto>anBlZ1Bob3RvMg==</wim:jpegPhoto>\n");
        sb.append("    <wim:labeledURI>labeledURI</wim:labeledURI>\n");
        sb.append("    <wim:carLicense>carLicense1</wim:carLicense>\n");
        sb.append("    <wim:carLicense>carLicense2</wim:carLicense>\n");
        sb.append("    <wim:telephoneNumber>telephoneNumber1</wim:telephoneNumber>\n");
        sb.append("    <wim:telephoneNumber>telephoneNumber2</wim:telephoneNumber>\n");
        sb.append("    <wim:facsimileTelephoneNumber>facsimileTelephoneNumber1</wim:facsimileTelephoneNumber>\n");
        sb.append("    <wim:facsimileTelephoneNumber>facsimileTelephoneNumber2</wim:facsimileTelephoneNumber>\n");
        sb.append("    <wim:pager>pager1</wim:pager>\n");
        sb.append("    <wim:pager>pager2</wim:pager>\n");
        sb.append("    <wim:mobile>mobile1</wim:mobile>\n");
        sb.append("    <wim:mobile>mobile2</wim:mobile>\n");
        sb.append("    <wim:homePostalAddress>homePostalAddress1</wim:homePostalAddress>\n");
        sb.append("    <wim:homePostalAddress>homePostalAddress2</wim:homePostalAddress>\n");
        sb.append("    <wim:postalAddress>postalAddress1</wim:postalAddress>\n");
        sb.append("    <wim:postalAddress>postalAddress2</wim:postalAddress>\n");
        sb.append("    <wim:roomNumber>roomNumber1</wim:roomNumber>\n");
        sb.append("    <wim:roomNumber>roomNumber2</wim:roomNumber>\n");
        sb.append("    <wim:l>l1</wim:l>\n");
        sb.append("    <wim:l>l2</wim:l>\n");
        sb.append("    <wim:localityName>localityName1</wim:localityName>\n");
        sb.append("    <wim:localityName>localityName2</wim:localityName>\n");
        sb.append("    <wim:st>st1</wim:st>\n");
        sb.append("    <wim:st>st2</wim:st>\n");
        sb.append("    <wim:stateOrProvinceName>stateOrProvinceName1</wim:stateOrProvinceName>\n");
        sb.append("    <wim:stateOrProvinceName>stateOrProvinceName2</wim:stateOrProvinceName>\n");
        sb.append("    <wim:street>street1</wim:street>\n");
        sb.append("    <wim:street>street2</wim:street>\n");
        sb.append("    <wim:postalCode>postalCode1</wim:postalCode>\n");
        sb.append("    <wim:postalCode>postalCode2</wim:postalCode>\n");
        sb.append("    <wim:city>city1</wim:city>\n");
        sb.append("    <wim:city>city2</wim:city>\n");
        sb.append("    <wim:employeeType>employeeType</wim:employeeType>\n");
        sb.append("    <wim:employeeNumber>employeeNumber</wim:employeeNumber>\n");
        sb.append("    <wim:manager uniqueId=\"uniqueId1\" uniqueName=\"uniqueName1\" externalId=\"externalId1\" externalName=\"externalName1\" repositoryId=\"repositoryId1\"/>\n");
        sb.append("    <wim:manager uniqueId=\"uniqueId2\" uniqueName=\"uniqueName2\" externalId=\"externalId2\" externalName=\"externalName2\" repositoryId=\"repositoryId2\"/>\n");
        sb.append("    <wim:secretary uniqueId=\"uniqueId1\" uniqueName=\"uniqueName1\" externalId=\"externalId1\" externalName=\"externalName1\" repositoryId=\"repositoryId1\"/>\n");
        sb.append("    <wim:secretary uniqueId=\"uniqueId2\" uniqueName=\"uniqueName2\" externalId=\"externalId2\" externalName=\"externalName2\" repositoryId=\"repositoryId2\"/>\n");
        sb.append("    <wim:departmentNumber>departmentNumber1</wim:departmentNumber>\n");
        sb.append("    <wim:departmentNumber>departmentNumber2</wim:departmentNumber>\n");
        sb.append("    <wim:title>title1</wim:title>\n");
        sb.append("    <wim:title>title2</wim:title>\n");
        sb.append("    <wim:ibmJobTitle>ibmJobTitle1</wim:ibmJobTitle>\n");
        sb.append("    <wim:ibmJobTitle>ibmJobTitle2</wim:ibmJobTitle>\n");
        sb.append("    <wim:c>c1</wim:c>\n");
        sb.append("    <wim:c>c2</wim:c>\n");
        sb.append("    <wim:countryName>countryName1</wim:countryName>\n");
        sb.append("    <wim:countryName>countryName2</wim:countryName>\n");
        sb.append("    <wim:givenName>givenName1</wim:givenName>\n");
        sb.append("    <wim:givenName>givenName2</wim:givenName>\n");
        sb.append("    <wim:homeAddress>\n");
        sb.append("        <wim:nickName>nickName</wim:nickName>\n");
        sb.append("        <wim:street>street1</wim:street>\n");
        sb.append("        <wim:street>street2</wim:street>\n");
        sb.append("        <wim:city>city</wim:city>\n");
        sb.append("        <wim:stateOrProvinceName>stateOrProvinceName</wim:stateOrProvinceName>\n");
        sb.append("        <wim:postalCode>postalCode</wim:postalCode>\n");
        sb.append("        <wim:countryName>countryName</wim:countryName>\n");
        sb.append("    </wim:homeAddress>\n");
        sb.append("    <wim:homeAddress>\n");
        sb.append("        <wim:nickName>nickName</wim:nickName>\n");
        sb.append("        <wim:street>street1</wim:street>\n");
        sb.append("        <wim:street>street2</wim:street>\n");
        sb.append("        <wim:city>city</wim:city>\n");
        sb.append("        <wim:stateOrProvinceName>stateOrProvinceName</wim:stateOrProvinceName>\n");
        sb.append("        <wim:postalCode>postalCode</wim:postalCode>\n");
        sb.append("        <wim:countryName>countryName</wim:countryName>\n");
        sb.append("    </wim:homeAddress>\n");
        sb.append("    <wim:businessAddress>\n");
        sb.append("        <wim:nickName>nickName</wim:nickName>\n");
        sb.append("        <wim:street>street1</wim:street>\n");
        sb.append("        <wim:street>street2</wim:street>\n");
        sb.append("        <wim:city>city</wim:city>\n");
        sb.append("        <wim:stateOrProvinceName>stateOrProvinceName</wim:stateOrProvinceName>\n");
        sb.append("        <wim:postalCode>postalCode</wim:postalCode>\n");
        sb.append("        <wim:countryName>countryName</wim:countryName>\n");
        sb.append("    </wim:businessAddress>\n");
        sb.append("    <wim:businessAddress>\n");
        sb.append("        <wim:nickName>nickName</wim:nickName>\n");
        sb.append("        <wim:street>street1</wim:street>\n");
        sb.append("        <wim:street>street2</wim:street>\n");
        sb.append("        <wim:city>city</wim:city>\n");
        sb.append("        <wim:stateOrProvinceName>stateOrProvinceName</wim:stateOrProvinceName>\n");
        sb.append("        <wim:postalCode>postalCode</wim:postalCode>\n");
        sb.append("        <wim:countryName>countryName</wim:countryName>\n");
        sb.append("    </wim:businessAddress>\n");
        sb.append("    <wim:description>description1</wim:description>\n");
        sb.append("    <wim:description>description2</wim:description>\n");
        sb.append("    <wim:businessCategory>businessCategory1</wim:businessCategory>\n");
        sb.append("    <wim:businessCategory>businessCategory2</wim:businessCategory>\n");
        sb.append("    <wim:seeAlso>seeAlso1</wim:seeAlso>\n");
        sb.append("    <wim:seeAlso>seeAlso2</wim:seeAlso>\n");
        sb.append("    <wim:kerberosId>kerberosId</wim:kerberosId>\n");
        sb.append("    <wim:photoURL>photoUrl</wim:photoURL>\n");
        sb.append("    <wim:photoURLThumbnail>photoUrlThumbnail</wim:photoURLThumbnail>\n");
        sb.append("</wim:Person>");
        assertEquals(sb.toString(), person.toString());
    }
}
