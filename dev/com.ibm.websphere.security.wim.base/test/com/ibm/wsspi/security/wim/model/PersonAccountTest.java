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

    @Test
    public void testToString() {

        /*
         * Test empty entity.
         */
        StringBuffer sb = new StringBuffer();
        sb.append("<wim:PersonAccount " + RootTest.WIM_XMLNS + "/>");
        assertEquals(sb.toString(), new PersonAccount().toString());

        /*
         * PersonAccount properties.
         */
        PersonAccount personAccount = new PersonAccount();
        personAccount.setUid("uid");
        personAccount.setCn("cn");
        personAccount.setSn("sn");
        personAccount.setPreferredLanguage("preferredLanguage");
        personAccount.set("displayName", "displayName1");
        personAccount.set("displayName", "displayName2");
        personAccount.set("initials", "initials");
        personAccount.setMail("mail");
        personAccount.setIbmPrimaryEmail("ibmPrimaryEmail");
        personAccount.set("jpegPhoto", "jpegPhoto1".getBytes());
        personAccount.set("jpegPhoto", "jpegPhoto2".getBytes());
        personAccount.setLabeledURI("labeledURI");
        personAccount.set("carLicense", "carLicense1");
        personAccount.set("carLicense", "carLicense2");
        personAccount.set("telephoneNumber", "telephoneNumber1");
        personAccount.set("telephoneNumber", "telephoneNumber2");
        personAccount.set("facsimileTelephoneNumber", "facsimileTelephoneNumber1");
        personAccount.set("facsimileTelephoneNumber", "facsimileTelephoneNumber2");
        personAccount.set("pager", "pager1");
        personAccount.set("pager", "pager2");
        personAccount.set("mobile", "mobile1");
        personAccount.set("mobile", "mobile2");
        personAccount.set("homePostalAddress", "homePostalAddress1");
        personAccount.set("homePostalAddress", "homePostalAddress2");
        personAccount.set("postalAddress", "postalAddress1");
        personAccount.set("postalAddress", "postalAddress2");
        personAccount.set("roomNumber", "roomNumber1");
        personAccount.set("roomNumber", "roomNumber2");
        personAccount.set("l", "l1");
        personAccount.set("l", "l2");
        personAccount.set("localityName", "localityName1");
        personAccount.set("localityName", "localityName2");
        personAccount.set("st", "st1");
        personAccount.set("st", "st2");
        personAccount.set("stateOrProvinceName", "stateOrProvinceName1");
        personAccount.set("stateOrProvinceName", "stateOrProvinceName2");
        personAccount.set("street", "street1");
        personAccount.set("street", "street2");
        personAccount.set("postalCode", "postalCode1");
        personAccount.set("postalCode", "postalCode2");
        personAccount.set("city", "city1");
        personAccount.set("city", "city2");
        personAccount.setEmployeeType("employeeType");
        personAccount.setEmployeeNumber("employeeNumber");
        personAccount.set("manager", IdentifierTypeTest.TEST_IDENTIFIER_1);
        personAccount.set("manager", IdentifierTypeTest.TEST_IDENTIFIER_2);
        personAccount.set("secretary", IdentifierTypeTest.TEST_IDENTIFIER_1);
        personAccount.set("secretary", IdentifierTypeTest.TEST_IDENTIFIER_2);
        personAccount.set("departmentNumber", "departmentNumber1");
        personAccount.set("departmentNumber", "departmentNumber2");
        personAccount.set("title", "title1");
        personAccount.set("title", "title2");
        personAccount.set("ibmJobTitle", "ibmJobTitle1");
        personAccount.set("ibmJobTitle", "ibmJobTitle2");
        personAccount.set("c", "c1");
        personAccount.set("c", "c2");
        personAccount.set("countryName", "countryName1");
        personAccount.set("countryName", "countryName2");
        personAccount.set("givenName", "givenName1");
        personAccount.set("givenName", "givenName2");
        personAccount.setHomeStreet("homeStreet");
        personAccount.setHomeCity("homeCity");
        personAccount.setHomeStateOrProvinceName("homeStateOrProvinceName");
        personAccount.setHomePostalCode("homePostalCode");
        personAccount.setHomeCountryName("homeCountryName");
        personAccount.setBusinessStreet("businessStreet");
        personAccount.setBusinessCity("businessCity");
        personAccount.setBusinessStateOrProvinceName("businessStateOrProvinceName");
        personAccount.setBusinessPostalCode("businessPostalCode");
        personAccount.setBusinessCountryName("businessCountryName");
        personAccount.set("description", "description1");
        personAccount.set("description", "description2");
        personAccount.set("businessCategory", "businessCategory1");
        personAccount.set("businessCategory", "businessCategory2");
        personAccount.set("seeAlso", "seeAlso1");
        personAccount.set("seeAlso", "seeAlso2");
        personAccount.setKerberosId("kerberosId");
        personAccount.setPhotoUrl("photoUrl");
        personAccount.setPhotoUrlThumbnail("photoUrlThumbnail");
        personAccount.setMiddleName("middleName");
        personAccount.setHonorificPrefix("honorificPrefix");
        personAccount.setHonorificSuffix("honorificSuffix");
        personAccount.setNickName("nickName");
        personAccount.setProfileUrl("profileUrl");
        personAccount.setTimezone("timezone");
        personAccount.setLocale("locale");
        personAccount.set("ims", "ims1");
        personAccount.set("ims", "ims2");
        personAccount.setActive(true);
        PersonAccount.addExtendedProperty("multiValuedExtProp", "String", true, null);
        PersonAccount.addExtendedProperty("singleValuedExtProp", "String", false, null);
        personAccount.set("multiValuedExtProp", "multiValue1");
        personAccount.set("multiValuedExtProp", "multiValue2");
        personAccount.set("singleValuedExtProp", "singleValue");
        LoginAccountTest.configureProperties(personAccount);

        /*
         * Create the expected response.
         */
        sb = new StringBuffer();
        sb.append("<wim:PersonAccount " + RootTest.WIM_XMLNS + ">\n");
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
        sb.append("    <wim:principalName>principalName</wim:principalName>\n");
        sb.append("    <wim:password>*****</wim:password>\n");
        sb.append("    <wim:realm>realm</wim:realm>\n");
        sb.append("    <wim:certificate>Y2VydGlmaWNhdGUx</wim:certificate>\n");
        sb.append("    <wim:certificate>Y2VydGlmaWNhdGUy</wim:certificate>\n");
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
        sb.append("    <wim:homeStreet>homeStreet</wim:homeStreet>\n");
        sb.append("    <wim:homeCity>homeCity</wim:homeCity>\n");
        sb.append("    <wim:homeStateOrProvinceName>homeStateOrProvinceName</wim:homeStateOrProvinceName>\n");
        sb.append("    <wim:homePostalCode>homePostalCode</wim:homePostalCode>\n");
        sb.append("    <wim:homeCountryName>homeCountryName</wim:homeCountryName>\n");
        sb.append("    <wim:businessStreet>businessStreet</wim:businessStreet>\n");
        sb.append("    <wim:businessCity>businessCity</wim:businessCity>\n");
        sb.append("    <wim:businessStateOrProvinceName>businessStateOrProvinceName</wim:businessStateOrProvinceName>\n");
        sb.append("    <wim:businessPostalCode>businessPostalCode</wim:businessPostalCode>\n");
        sb.append("    <wim:businessCountryName>businessCountryName</wim:businessCountryName>\n");
        sb.append("    <wim:description>description1</wim:description>\n");
        sb.append("    <wim:description>description2</wim:description>\n");
        sb.append("    <wim:businessCategory>businessCategory1</wim:businessCategory>\n");
        sb.append("    <wim:businessCategory>businessCategory2</wim:businessCategory>\n");
        sb.append("    <wim:seeAlso>seeAlso1</wim:seeAlso>\n");
        sb.append("    <wim:seeAlso>seeAlso2</wim:seeAlso>\n");
        sb.append("    <wim:kerberosId>kerberosId</wim:kerberosId>\n");
        sb.append("    <wim:photoURL>photoUrl</wim:photoURL>\n");
        sb.append("    <wim:photoURLThumbnail>photoUrlThumbnail</wim:photoURLThumbnail>\n");
        sb.append("    <wim:middleName>middleName</wim:middleName>\n");
        sb.append("    <wim:honorificPrefix>honorificPrefix</wim:honorificPrefix>\n");
        sb.append("    <wim:honorificSuffix>honorificSuffix</wim:honorificSuffix>\n");
        sb.append("    <wim:nickName>nickName</wim:nickName>\n");
        sb.append("    <wim:profileUrl>profileUrl</wim:profileUrl>\n");
        sb.append("    <wim:timezone>timezone</wim:timezone>\n");
        sb.append("    <wim:locale>locale</wim:locale>\n");
        sb.append("    <wim:ims>ims1</wim:ims>\n");
        sb.append("    <wim:ims>ims2</wim:ims>\n");
        sb.append("    <wim:active>true</wim:active>\n");
        sb.append("    <wim:extendedProperties>\n");
        sb.append("        <item>\n");
        sb.append("            <key>multiValuedExtProp</key>\n");
        sb.append("            <values xsi:type=\"xs:string\">multiValue1</values>\n");
        sb.append("            <values xsi:type=\"xs:string\">multiValue2</values>\n");
        sb.append("        </item>\n");
        sb.append("        <item>\n");
        sb.append("            <key>singleValuedExtProp</key>\n");
        sb.append("            <value xsi:type=\"xs:string\">singleValue</value>\n");
        sb.append("        </item>\n");
        sb.append("    </wim:extendedProperties>\n");
        sb.append("</wim:PersonAccount>");
        assertEquals(sb.toString(), personAccount.toString());
    }
}
