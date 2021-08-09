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

package com.ibm.ws.security.wim.scim20;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.websphere.security.wim.scim20.model.ListResponse;
import com.ibm.websphere.security.wim.scim20.model.schemas.Schema;
import com.ibm.ws.security.wim.scim20.model.extensions.EnterpriseUserImpl;
import com.ibm.ws.security.wim.scim20.model.extensions.WIMGroupImpl;
import com.ibm.ws.security.wim.scim20.model.extensions.WIMUserImpl;
import com.ibm.ws.security.wim.scim20.model.groups.GroupImpl;
import com.ibm.ws.security.wim.scim20.model.schemas.SchemaAttributeImpl;
import com.ibm.ws.security.wim.scim20.model.users.UserImpl;

public class SchemaHandlerTest {
    @Test
    public void getAllSchema() throws Exception {
        ListResponse<Schema> response = SchemaHandler.getSchemas();
//        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(response)); // TODO Delete
        assertEquals(5, response.getResources().size());

        Schema coreGroupSchema = response.getResources().get(0);
        assertEquals(2, coreGroupSchema.getAttributes().size());
        assertEquals(3, coreGroupSchema.getAttributes().get(1).getSubAttributes().size());
        assertEquals("Group", coreGroupSchema.getDescription());
        assertEquals(GroupImpl.SCHEMA_URN, coreGroupSchema.getId());
        assertEquals("Group", coreGroupSchema.getName());

        Schema wimUserSchema = response.getResources().get(1);
        assertEquals(9, wimUserSchema.getAttributes().size());
        assertEquals(5, wimUserSchema.getAttributes().get(0).getSubAttributes().size()); // identifier
        assertEquals("WIM User Account Extension", wimUserSchema.getDescription());
        assertEquals(WIMUserImpl.SCHEMA_URN, wimUserSchema.getId());
        assertEquals("WIM User", wimUserSchema.getName());

        Schema coreUserSchema = response.getResources().get(2);
        assertEquals(2, coreUserSchema.getAttributes().size());
        assertEquals(6, coreUserSchema.getAttributes().get(1).getSubAttributes().size());
        assertEquals("User Account", coreUserSchema.getDescription());
        assertEquals(UserImpl.SCHEMA_URN, coreUserSchema.getId());
        assertEquals("User", coreUserSchema.getName());

        Schema enterpriseUserSchema = response.getResources().get(3);
        assertEquals(6, enterpriseUserSchema.getAttributes().size());
        assertEquals(3, enterpriseUserSchema.getAttributes().get(5).getSubAttributes().size()); // manager
        assertEquals("Enterprise User", enterpriseUserSchema.getDescription());
        assertEquals(EnterpriseUserImpl.SCHEMA_URN, enterpriseUserSchema.getId());
        assertEquals("EnterpriseUser", enterpriseUserSchema.getName());

        Schema wimGroupSchema = response.getResources().get(4);
        assertEquals(1, wimGroupSchema.getAttributes().size());
        assertEquals(5, wimGroupSchema.getAttributes().get(0).getSubAttributes().size()); // identifier
        assertEquals("WIM Group Extension", wimGroupSchema.getDescription());
        assertEquals(WIMGroupImpl.SCHEMA_URN, wimGroupSchema.getId());
        assertEquals("WIM Group", wimGroupSchema.getName());

        /*
         * Serialize.
         */
        ObjectMapper objectMapper = new ObjectMapper();
        String serialized = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
//        System.out.println(serialized); // TODO REMOVE
    }

    @Test
    public void getUserSchema() throws Exception {
        Schema schema = SchemaHandler.getSchema(UserImpl.SCHEMA_URN);
        assertEquals(2, schema.getAttributes().size());
        assertEquals(6, schema.getAttributes().get(1).getSubAttributes().size());
        assertEquals("User Account", schema.getDescription());
        assertEquals(UserImpl.SCHEMA_URN, schema.getId());
        assertEquals("User", schema.getName());

        /*
         * Serialize.
         */
        ObjectMapper objectMapper = new ObjectMapper();
        String serialized = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
//        System.out.println(serialized); // TODO REMOVE
    }

    @Test
    public void getGroupSchema() throws Exception {
        Schema schema = SchemaHandler.getSchema(GroupImpl.SCHEMA_URN);
        assertEquals(2, schema.getAttributes().size());
        assertEquals(3, schema.getAttributes().get(1).getSubAttributes().size());
        assertEquals("Group", schema.getDescription());
        assertEquals(GroupImpl.SCHEMA_URN, schema.getId());
        assertEquals("Group", schema.getName());

        /*
         * Serialize.
         */
        ObjectMapper objectMapper = new ObjectMapper();
        String serialized = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
//        System.out.println(serialized);// TODO REMOVE
    }

    @Test
    public void getUserAttribute() {
        /*
         * Test a core user root attribute.
         */
        String uri = UserImpl.SCHEMA_URN + ":name";
        SchemaAttributeImpl attr1 = SchemaHandler.getUserAttribute("name");
        SchemaAttributeImpl attr2 = SchemaHandler.getUserAttribute(uri);
        assertEquals(uri, attr1.getUrn());
        assertEquals(uri, attr2.getUrn());
        assertSame(attr1, attr2);

        /*
         * Test a core user sub-attribute.
         */
        uri = UserImpl.SCHEMA_URN + ":name.formatted";
        attr1 = SchemaHandler.getUserAttribute("name.formatted");
        attr2 = SchemaHandler.getUserAttribute(uri);
        assertEquals(uri, attr1.getUrn());
        assertEquals(uri, attr2.getUrn());
        assertSame(attr1, attr2);
    }

    @Test
    public void getGroupAttribute() {
        /*
         * Test a core group root attribute.
         */
        String uri = UserImpl.SCHEMA_URN + ":name";
        SchemaAttributeImpl attr1 = SchemaHandler.getUserAttribute("name");
        SchemaAttributeImpl attr2 = SchemaHandler.getUserAttribute(uri);
        assertEquals(uri, attr1.getUrn());
        assertEquals(uri, attr2.getUrn());
        assertSame(attr1, attr2);

        /*
         * Test a core group sub-attribute.
         */
        uri = UserImpl.SCHEMA_URN + ":name.formatted";
        attr1 = SchemaHandler.getUserAttribute("name.formatted");
        attr2 = SchemaHandler.getUserAttribute(uri);
        assertEquals(uri, attr1.getUrn());
        assertEquals(uri, attr2.getUrn());
        assertSame(attr1, attr2);
    }

    @Test
    public void getGroupDefaultAttributeSet() {
        Set<SchemaAttributeImpl> attributes = SchemaHandler.getGroupDefaultAttributeSet();
        assertEquals(9, attributes.size());

        Set<String> expected = new HashSet<String>();

        /* Core user schema */
        expected.add("displayName");
        expected.add("members.value");
        expected.add("members.type");
        expected.add("members.$ref");

        /* WIM user extension schema */
        expected.add("identifier.uniqueId");
        expected.add("identifier.uniqueName");
        expected.add("identifier.externalId");
        expected.add("identifier.externalName");
        expected.add("identifier.repositoryId");

        Set<String> actual = new HashSet<String>();
        for (SchemaAttributeImpl attribute : attributes) {
            actual.add(attribute.getAnnotatedName());
        }

        assertEquals(expected, actual);
    }

    @Test
    public void getGroupMinimumAttributeSet() {
        Set<SchemaAttributeImpl> attributes = SchemaHandler.getGroupMinimumAttributeSet();
        assertEquals(0, attributes.size());
    }

    @Test
    public void getGroupAttributeSetWithExclude() {
        Set<String> exclude = new HashSet<String>();
        exclude.add("displayName");
        exclude.add("members.value");
        exclude.add("identifier");

        Set<SchemaAttributeImpl> attributes = SchemaHandler.getGroupAttributeSetWithExclude(exclude);
        assertEquals(2, attributes.size());

        Set<String> expected = new HashSet<String>();
        expected.add("members.type");
        expected.add("members.$ref");

        Set<String> actual = new HashSet<String>();
        for (SchemaAttributeImpl attribute : attributes) {
            actual.add(attribute.getAnnotatedName());
        }

        assertEquals(expected, actual);
    }

    @Test
    @Ignore("Will fail until we can configure custom group attributes") // TODO
    public void getGroupAttributeSetWithInclude() {
        Set<String> include = new HashSet<String>();
        include.add("attribute1");
        include.add("attribute2");

        Set<SchemaAttributeImpl> attributes = SchemaHandler.getGroupAttributeSetWithInclude(include);
        assertEquals(2, attributes.size());

        Set<String> expected = new HashSet<String>();
        expected.add("attribute1");
        expected.add("attribute1");

        Set<String> actual = new HashSet<String>();
        for (SchemaAttributeImpl attribute : attributes) {
            actual.add(attribute.getAnnotatedName());
        }

        assertEquals(expected, actual);
    }

    @Test
    public void getUserDefaultAttributeSet() {
        Set<SchemaAttributeImpl> attributes = SchemaHandler.getUserDefaultAttributeSet();
        assertEquals(28, attributes.size());

        Set<String> expected = new HashSet<String>();

        /* Core user schema */
        expected.add("name.formatted");
        expected.add("name.givenName");
        expected.add("name.familyName");
        expected.add("name.middleName");
        expected.add("name.honorificPrefix");
        expected.add("name.honorificSuffix");

        /* Enterprise user extension schema */
        expected.add("employeeNumber");
        expected.add("costCenter");
        expected.add("organization");
        expected.add("division");
        expected.add("department");
        expected.add("manager.displayName");
        expected.add("manager.$ref");
        expected.add("manager.value");
        expected.add("userName");

        /* WIM user extension schema */
        expected.add("identifier.uniqueId");
        expected.add("identifier.uniqueName");
        expected.add("identifier.externalId");
        expected.add("identifier.externalName");
        expected.add("identifier.repositoryId");
        expected.add("principalName");
        expected.add("uid");
        expected.add("cn");
        expected.add("sn");
        expected.add("preferredLanguage");
        expected.add("displayName");
        expected.add("initials");
        expected.add("mail");

        Set<String> actual = new HashSet<String>();
        for (SchemaAttributeImpl attribute : attributes) {
            actual.add(attribute.getAnnotatedName());
        }

        assertEquals(expected, actual);
    }

    @Test
    public void getUserMinimumAttributeSet() {
        Set<SchemaAttributeImpl> attributes = SchemaHandler.getUserMinimumAttributeSet();
        assertEquals(0, attributes.size());
    }

    @Test
    public void getUserAttributeSetWithExclude() {
        Set<String> exclude = new HashSet<String>();
        exclude.add("name.formatted");
        exclude.add("employeeNumber");
        exclude.add("identifier"); // excludes sub-attributes

        Set<SchemaAttributeImpl> attributes = SchemaHandler.getUserAttributeSetWithExclude(exclude);
        assertEquals(21, attributes.size());

        Set<String> expected = new HashSet<String>();

        /* Core user schema */
        // expected.add("name.formatted"); EXCLUDED
        expected.add("name.givenName");
        expected.add("name.familyName");
        expected.add("name.middleName");
        expected.add("name.honorificPrefix");
        expected.add("name.honorificSuffix");

        /* Enterprise user extension schema */
        // expected.add("employeeNumber"); EXCLUDED
        expected.add("costCenter");
        expected.add("organization");
        expected.add("division");
        expected.add("department");
        expected.add("manager.displayName");
        expected.add("manager.$ref");
        expected.add("manager.value");
        expected.add("userName");

        /* WIM user extension schema */
        // expected.add("identifier.uniqueId"); EXCLUDED
        // expected.add("identifier.uniqueName"); EXCLUDED
        // expected.add("identifier.externalId"); EXCLUDED
        // expected.add("identifier.externalName"); EXCLUDED
        // expected.add("identifier.repositoryId"); EXCLUDED
        expected.add("principalName");
        expected.add("uid");
        expected.add("cn");
        expected.add("sn");
        expected.add("preferredLanguage");
        expected.add("displayName");
        expected.add("initials");
        expected.add("mail");

        Set<String> actual = new HashSet<String>();
        for (SchemaAttributeImpl attribute : attributes) {
            actual.add(attribute.getAnnotatedName());
        }

        assertEquals(expected, actual);
    }

    @Test
    @Ignore("Will fail until we can configure custom user attributes") // TODO
    public void getUserAttributeSetWithInclude() {
        Set<String> include = new HashSet<String>();
        include.add("attribute1");
        include.add("attribute2");

        Set<SchemaAttributeImpl> attributes = SchemaHandler.getUserAttributeSetWithInclude(include);
        assertEquals(2, attributes.size());

        Set<String> expected = new HashSet<String>();
        expected.add("attribute1");
        expected.add("attribute1");

        Set<String> actual = new HashSet<String>();
        for (SchemaAttributeImpl attribute : attributes) {
            actual.add(attribute.getAnnotatedName());
        }

        assertEquals(expected, actual);
    }
}
