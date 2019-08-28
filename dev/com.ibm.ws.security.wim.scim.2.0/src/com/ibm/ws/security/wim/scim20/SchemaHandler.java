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

package com.ibm.ws.security.wim.scim20;

import static com.ibm.websphere.security.wim.scim20.model.schemas.SchemaAttribute.MUTABILITY_IMMUTABLE;
import static com.ibm.websphere.security.wim.scim20.model.schemas.SchemaAttribute.MUTABILITY_READ_WRITE;
import static com.ibm.websphere.security.wim.scim20.model.schemas.SchemaAttribute.RETURNED_DEFAULT;
import static com.ibm.websphere.security.wim.scim20.model.schemas.SchemaAttribute.TYPE_COMPLEX;
import static com.ibm.websphere.security.wim.scim20.model.schemas.SchemaAttribute.TYPE_REFERENCE;
import static com.ibm.websphere.security.wim.scim20.model.schemas.SchemaAttribute.TYPE_STRING;
import static com.ibm.websphere.security.wim.scim20.model.schemas.SchemaAttribute.UNIQUENESS_NONE;
import static com.ibm.websphere.security.wim.scim20.model.schemas.SchemaAttribute.UNIQUENESS_SERVER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.security.wim.scim20.model.ListResponse;
import com.ibm.websphere.security.wim.scim20.model.schemas.Schema;
import com.ibm.websphere.security.wim.scim20.model.schemas.SchemaAttribute;
import com.ibm.ws.security.wim.scim20.model.ListResponseImpl;
import com.ibm.ws.security.wim.scim20.model.extensions.EnterpriseUserImpl;
import com.ibm.ws.security.wim.scim20.model.extensions.WIMGroupImpl;
import com.ibm.ws.security.wim.scim20.model.extensions.WIMUserImpl;
import com.ibm.ws.security.wim.scim20.model.groups.GroupImpl;
import com.ibm.ws.security.wim.scim20.model.schemas.SchemaAttributeImpl;
import com.ibm.ws.security.wim.scim20.model.schemas.SchemaImpl;
import com.ibm.ws.security.wim.scim20.model.users.UserImpl;

// TODO Localize descriptions
// TODO Attribute to WIM entity property mappings
public class SchemaHandler {

    private static Map<String, SchemaAttributeImpl> GROUP_ATTRIBUTE_MAP = new HashMap<String, SchemaAttributeImpl>();

    private static Map<String, SchemaImpl> SCHEMA_MAP = new HashMap<String, SchemaImpl>();

    private static Map<String, SchemaAttributeImpl> USER_ATTRIBUTE_MAP = new HashMap<String, SchemaAttributeImpl>();

    static {
        configureCoreUser();
        configureCoreGroup();
        configureEnterpriseUser();
        configureWimUser();
        configureWimGroup();
    }

    /**
     * Add mapping in the attribute name to the user attribute map. This method
     * will add a mapping for the full URN as well as a mapping for the short
     * name if there is not one already.
     *
     * @param attribute
     *            The attribute to add to the map.
     */
    private static void addToAttributeMap(Map<String, SchemaAttributeImpl> map, SchemaAttributeImpl attribute) {
        String annotatedName = attribute.getAnnotatedName().toLowerCase();
        String urn = attribute.getUrn().toLowerCase();

        /*
         * Add the short attribute name mapping. Check for short name conflicts.
         * Keep the first attribute added.
         */
        if (!map.containsKey(annotatedName)) {
            map.put(annotatedName, attribute);
        }

        /*
         * Add the full attribute URN mapping.
         */
        map.put(urn, attribute);
    }

    /**
     * Add mapping in the attribute name to the user attribute map. This method
     * will add a mapping for the full URN as well as a mapping for the short
     * name if there is not one already.
     *
     * @param attribute
     *            The attribute to add to the map.
     */
    private static void addToGroupAttributeMap(SchemaAttributeImpl attribute) {
        addToAttributeMap(GROUP_ATTRIBUTE_MAP, attribute);
    }

    /**
     * Add mapping in the attribute name to the user attribute map. This method
     * will add a mapping for the full URN as well as a mapping for the short
     * name if there is not one already.
     *
     * @param attribute
     *            The attribute to add to the map.
     */
    private static void addToUserAttributeMap(SchemaAttributeImpl attribute) {
        addToAttributeMap(USER_ATTRIBUTE_MAP, attribute);
    }

    private static void configureCoreGroup() {

        List<SchemaAttribute> rootAttributes = new ArrayList<SchemaAttribute>();

        /*
         * urn:ietf:params:scim:schemas:core:2.0:Group:displayName
         */
        SchemaAttributeImpl displayName = new SchemaAttributeImpl("displayName", GroupImpl.SCHEMA_URN, TYPE_STRING, null, false, "A human-readable name for the Group. REQUIRED.", true, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        rootAttributes.add(displayName);

        /*
         * urn:ietf:params:scim:schemas:core:2.0:Group:members.value
         */
        SchemaAttributeImpl members_value = new SchemaAttributeImpl("value", GroupImpl.SCHEMA_URN, TYPE_STRING, null, false, "Identifier of the member of this Group.", false, null, false, MUTABILITY_IMMUTABLE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);

        /*
         * urn:ietf:params:scim:schemas:core:2.0:Group:members.$ref
         */
        SchemaAttributeImpl members_ref = new SchemaAttributeImpl("$ref", GroupImpl.SCHEMA_URN, TYPE_REFERENCE, null, false, "The URI corresponding to a SCIM resource that is a member of this Group.", false, null, false, MUTABILITY_IMMUTABLE, RETURNED_DEFAULT, UNIQUENESS_NONE, Arrays.asList(new String[] { "User",
                                                                                                                                                                                                                                                                                                                   "Group" }));

        /*
         * urn:ietf:params:scim:schemas:core:2.0:Group:members.type
         */
        SchemaAttributeImpl members_type = new SchemaAttributeImpl("type", GroupImpl.SCHEMA_URN, TYPE_STRING, null, false, "A label indicating the type of resource, e.g., 'User' or 'Group'.", false, Arrays.asList(new String[] { "User",
                                                                                                                                                                                                                                    "Group" }), false, MUTABILITY_IMMUTABLE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);

        /*
         * urn:ietf:params:scim:schemas:core:2.0:Group:members
         */
        List<SchemaAttribute> subAttributes = Arrays.asList(new SchemaAttribute[] { members_value, members_ref, members_type });
        SchemaAttributeImpl members = new SchemaAttributeImpl("members", GroupImpl.SCHEMA_URN, TYPE_COMPLEX, subAttributes, true, "description", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        rootAttributes.add(members);

        /*
         * Add the attributes to the group attribute name map. We do this later
         * b/c the sub-attribute/parent relationships are set when adding the
         * sub-attributes to the parent. If we add these earlier, the annotated
         * names might not include the parent and the mappings will fail.
         */
        addToGroupAttributeMap(displayName);
        addToGroupAttributeMap(members);
        addToGroupAttributeMap(members_value);
        addToGroupAttributeMap(members_ref);
        addToGroupAttributeMap(members_type);

        /*
         * Create the Schema instance and store it in the map.
         */
        SchemaImpl schema = new SchemaImpl();
        schema.setDescription("Group");
        schema.setId(GroupImpl.SCHEMA_URN);
        schema.setName("Group");
        schema.setAttributes(rootAttributes);

        SCHEMA_MAP.put(GroupImpl.SCHEMA_URN, schema);
    }

    private static void configureCoreUser() {

        List<SchemaAttribute> rootAttributes = new ArrayList<SchemaAttribute>();

        /*
         * urn:ietf:params:scim:schemas:core:2.0:User:userName
         */
        SchemaAttributeImpl userName = new SchemaAttributeImpl("userName", UserImpl.SCHEMA_URN, TYPE_STRING, null, false, "description", true, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_SERVER, null);
        rootAttributes.add(userName);

        /*
         * urn:ietf:params:scim:schemas:core:2.0:User:name.formatted
         */
        SchemaAttributeImpl name_formatted = new SchemaAttributeImpl("formatted", UserImpl.SCHEMA_URN, TYPE_STRING, null, false, "description", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);

        /*
         * urn:ietf:params:scim:schemas:core:2.0:User:name.familyName
         */
        SchemaAttributeImpl name_familyName = new SchemaAttributeImpl("familyName", UserImpl.SCHEMA_URN, TYPE_STRING, null, false, "description", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);

        /*
         * urn:ietf:params:scim:schemas:core:2.0:User:name.givenName
         */
        SchemaAttributeImpl name_givenName = new SchemaAttributeImpl("givenName", UserImpl.SCHEMA_URN, TYPE_STRING, null, false, "description", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);

        /*
         * urn:ietf:params:scim:schemas:core:2.0:User:name.middleName
         */
        SchemaAttributeImpl name_middleName = new SchemaAttributeImpl("middleName", UserImpl.SCHEMA_URN, TYPE_STRING, null, false, "description", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);

        /*
         * urn:ietf:params:scim:schemas:core:2.0:User:name.honorificPrefix
         */
        SchemaAttributeImpl name_honorificPrefix = new SchemaAttributeImpl("honorificPrefix", UserImpl.SCHEMA_URN, TYPE_STRING, null, false, "description", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);

        /*
         * urn:ietf:params:scim:schemas:core:2.0:User:name.honorificSuffix
         */
        SchemaAttributeImpl name_honorificSuffix = new SchemaAttributeImpl("honorificSuffix", UserImpl.SCHEMA_URN, TYPE_STRING, null, false, "description", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);

        /*
         * urn:ietf:params:scim:schemas:core:2.0:User:name
         */
        List<SchemaAttribute> subAttributes = Arrays.asList(new SchemaAttribute[] { name_formatted, name_familyName,
                                                                                    name_givenName, name_middleName, name_honorificPrefix, name_honorificSuffix });
        SchemaAttributeImpl name = new SchemaAttributeImpl("name", UserImpl.SCHEMA_URN, TYPE_COMPLEX, subAttributes, false, "description", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        rootAttributes.add(name);

        // TODO ALL THE OTHER CORE USER ATTRIBUTES.

        /*
         * Add the attributes to the user attribute name map. We do this later
         * b/c the sub-attribute/parent relationships are set when adding the
         * sub-attributes to the parent. If we add these earlier, the annotated
         * names might not include the parent and the mappings will fail.
         */
        addToUserAttributeMap(userName);
        addToUserAttributeMap(name);
        addToUserAttributeMap(name_formatted);
        addToUserAttributeMap(name_familyName);
        addToUserAttributeMap(name_givenName);
        addToUserAttributeMap(name_middleName);
        addToUserAttributeMap(name_honorificPrefix);
        addToUserAttributeMap(name_honorificSuffix);

        /*
         * Create the Schema instance and store it in the map.
         */
        SchemaImpl schema = new SchemaImpl();
        schema.setDescription("User Account");
        schema.setId(UserImpl.SCHEMA_URN);
        schema.setName("User");
        schema.setAttributes(rootAttributes);

        SCHEMA_MAP.put(UserImpl.SCHEMA_URN, schema);
    }

    private static void configureEnterpriseUser() {

        List<SchemaAttribute> rootAttributes = new ArrayList<SchemaAttribute>();

        /*
         * urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:
         * employeeNumber
         */
        SchemaAttributeImpl employeeNumber = new SchemaAttributeImpl("employeeNumber", EnterpriseUserImpl.SCHEMA_URN, TYPE_STRING, null, false, "Numeric or alphanumeric identifier assigned to a person, typically based on order of hire or association with an organization.", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        rootAttributes.add(employeeNumber);

        /*
         * urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:costCenter
         */
        SchemaAttributeImpl costCenter = new SchemaAttributeImpl("costCenter", EnterpriseUserImpl.SCHEMA_URN, TYPE_STRING, null, false, "Identifies the name of a cost center.", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        rootAttributes.add(costCenter);

        /*
         * urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:
         * organization
         */
        SchemaAttributeImpl organization = new SchemaAttributeImpl("organization", EnterpriseUserImpl.SCHEMA_URN, TYPE_STRING, null, false, "Identifies the name of an organization.", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        rootAttributes.add(organization);

        /*
         * urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:division
         */
        SchemaAttributeImpl division = new SchemaAttributeImpl("division", EnterpriseUserImpl.SCHEMA_URN, TYPE_STRING, null, false, "Identifies the name of a division.", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        rootAttributes.add(division);

        /*
         * urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:department
         */
        SchemaAttributeImpl department = new SchemaAttributeImpl("department", EnterpriseUserImpl.SCHEMA_URN, TYPE_STRING, null, false, "Identifies the name of a department.", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        rootAttributes.add(department);

        /*
         * urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.
         * value
         */
        SchemaAttributeImpl manager_value = new SchemaAttributeImpl("value", EnterpriseUserImpl.SCHEMA_URN, TYPE_STRING, null, false, "The id of the SCIM resource representing the User's manager.  REQUIRED.", false, null, false, MUTABILITY_IMMUTABLE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);

        /*
         * urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.
         * $ref
         */
        SchemaAttributeImpl manager_ref = new SchemaAttributeImpl("$ref", EnterpriseUserImpl.SCHEMA_URN, TYPE_REFERENCE, null, false, "The URI of the SCIM resource representing the User's manager.  REQUIRED.", false, null, false, MUTABILITY_IMMUTABLE, RETURNED_DEFAULT, UNIQUENESS_NONE, Arrays.asList(new String[] { "User",
                                                                                                                                                                                                                                                                                                                            "Group" }));

        /*
         * urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.
         * displayName
         */
        SchemaAttributeImpl manager_displayName = new SchemaAttributeImpl("displayName", EnterpriseUserImpl.SCHEMA_URN, TYPE_STRING, null, false, "The displayName of the User's manager. OPTIONAL and READ-ONLY.", false, Arrays.asList(new String[] { "User",
                                                                                                                                                                                                                                                        "Group" }), false, MUTABILITY_IMMUTABLE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);

        /*
         * urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager
         */
        List<SchemaAttribute> subAttributes = Arrays.asList(new SchemaAttribute[] { manager_value, manager_ref, manager_displayName });
        SchemaAttributeImpl manager = new SchemaAttributeImpl("manager", EnterpriseUserImpl.SCHEMA_URN, TYPE_COMPLEX, subAttributes, false, "The User's manager. A complex type that optionally allows service providers to represent organizational hierarchy by referencing the 'id' attribute of another User.", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        rootAttributes.add(manager);

        /*
         * Add the attributes to the use attribute name map. We do this later
         * b/c the sub-attribute/parent relationships are set when adding the
         * sub-attributes to the parent. If we add these earlier, the annotated
         * names might not include the parent and the mappings will fail.
         */
        addToUserAttributeMap(employeeNumber);
        addToUserAttributeMap(costCenter);
        addToUserAttributeMap(organization);
        addToUserAttributeMap(division);
        addToUserAttributeMap(department);
        addToUserAttributeMap(manager);
        addToUserAttributeMap(manager_value);
        addToUserAttributeMap(manager_ref);
        addToUserAttributeMap(manager_displayName);

        /*
         * Create the Schema instance and store it in the map.
         */
        SchemaImpl schema = new SchemaImpl();
        schema.setDescription("Enterprise User");
        schema.setId(EnterpriseUserImpl.SCHEMA_URN);
        schema.setName("EnterpriseUser");
        schema.setAttributes(rootAttributes);

        SCHEMA_MAP.put(EnterpriseUserImpl.SCHEMA_URN, schema);
    }

    /**
     * Get a group attribute by either the attribute's annotated name or URN.
     *
     * @param name
     *            The annotated name or URN of the attribute to retrieve.
     * @return The attribute or null if not found.
     */
    public static SchemaAttributeImpl getGroupAttribute(String name) {
        return GROUP_ATTRIBUTE_MAP.get(name.toLowerCase());
    }

    /**
     * Get the set of group attributes to return when a request has excluded a
     * set of attributes to return. The results will include the minimum set and
     * the set difference of the default set and the exclude list.
     *
     * @param exclue
     *            The set of group attributes to exclude.
     * @return The set of group attributes that comprise the minimum set of
     *         attributes.
     */
    public static Set<SchemaAttributeImpl> getGroupAttributeSetWithExclude(Set<String> exclude) {
        // TODO Would be nice to cache these.
        // TODO Should leave required attributes

        Set<SchemaAttributeImpl> attributes = new HashSet<SchemaAttributeImpl>(getGroupDefaultAttributeSet());
        for (String name : exclude) {
            SchemaAttributeImpl attribute = getGroupAttribute(name);
            if (attribute != null) {
                /*
                 * If the attribute is complex, exclude its sub-attributes,
                 * otherwise, exclude the attribute itself.
                 */
                if (!SchemaAttribute.TYPE_COMPLEX.equalsIgnoreCase(attribute.getType())) {
                    attributes.remove(attribute);
                } else {
                    attributes.removeAll(attribute.getSubAttributes());
                }
            }
        }

        attributes.addAll(getGroupMinimumAttributeSet());
        return Collections.unmodifiableSet(attributes);
    }

    /**
     * Get the set of group attributes to return when a request has included a
     * set of attributes to return. The results will include the minimum set
     * plus any explicitly defined attributes.
     *
     * @param include
     *            The set of group attributes to include.
     * @return The set of group attributes that comprise the minimum set of
     *         attributes.
     */
    public static Set<SchemaAttributeImpl> getGroupAttributeSetWithInclude(Set<String> include) {
        // TODO Would be nice to cache these.

        Set<SchemaAttributeImpl> attributes = new HashSet<SchemaAttributeImpl>();
        for (String name : include) {
            SchemaAttributeImpl attribute = getGroupAttribute(name);
            if (attribute != null && !SchemaAttribute.RETURNED_NEVER.equalsIgnoreCase(attribute.getReturned())) {
                attributes.add(attribute);
            }
        }

        attributes.addAll(getGroupMinimumAttributeSet());
        return Collections.unmodifiableSet(attributes);
    }

    /**
     * Get the default set of group attributes to return. The minimum set is
     * composed of those attributes that have their returned characteristic set
     * to "default".
     *
     * @return The set of group attributes that comprise the default set of
     *         group attributes.
     */
    static Set<SchemaAttributeImpl> getGroupDefaultAttributeSet() {
        // TODO Would be nice to cache these.

        Set<SchemaAttributeImpl> attributes = new HashSet<SchemaAttributeImpl>();
        for (SchemaAttributeImpl attribute : GROUP_ATTRIBUTE_MAP.values()) {
            /*
             * Only include non-complex attributes (their sub-attributes will be
             * returned) and attributes that have "returned" set to "default".
             */
            if (SchemaAttribute.RETURNED_DEFAULT.equalsIgnoreCase(attribute.getReturned())
                && !SchemaAttribute.TYPE_COMPLEX.equalsIgnoreCase(attribute.getType())) {
                attributes.add(attribute);
            }
        }
        return Collections.unmodifiableSet(attributes);
    }

    /**
     * Get the minimum set of group attributes to return. The minimum set is
     * composed of those attributes that have their returned characteristic set
     * to "always".
     *
     * @return The set of group attributes that comprise the minimum set of
     *         attributes.
     */
    static Set<SchemaAttributeImpl> getGroupMinimumAttributeSet() {
        // TODO Would be nice to cache these.

        Set<SchemaAttributeImpl> attributes = new HashSet<SchemaAttributeImpl>();
        for (SchemaAttributeImpl attribute : GROUP_ATTRIBUTE_MAP.values()) {
            /*
             * Only include non-complex attributes (their sub-attributes will be
             * returned) and attributes that have "returned" set to "default".
             */
            if (SchemaAttribute.RETURNED_ALWAYS.equalsIgnoreCase(attribute.getReturned())
                && !SchemaAttribute.TYPE_COMPLEX.equalsIgnoreCase(attribute.getType())) {
                attributes.add(attribute);
            }
        }
        return Collections.unmodifiableSet(attributes);
    }

    public static Schema getSchema(String uri) {
        return SCHEMA_MAP.get(uri);
    }

    public static ListResponse<Schema> getSchemas() {
        List<Schema> schemas = new ArrayList<Schema>();
        for (SchemaImpl schema : SCHEMA_MAP.values()) {
            schemas.add(schema);
        }

        ListResponseImpl<Schema> response = new ListResponseImpl<Schema>();
        response.setTotalResults(SCHEMA_MAP.size());
        response.setItemsPerPage(10);
        response.setStartIndex(1);
        response.setResources(schemas);

        return response;
    }

    /**
     * Get a user attribute by either the attribute's annotated name or URN.
     *
     * @param name
     *            The annotated name or URN of the attribute to retrieve.
     * @return The attribute or null if not found.
     */
    public static SchemaAttributeImpl getUserAttribute(String name) {
        return USER_ATTRIBUTE_MAP.get(name.toLowerCase());
    }

    /**
     * Get the set of user attributes to return when a request has excluded a
     * set of attributes to return. The results will include the minimum set and
     * the set difference of the default set and the exclude list.
     *
     * @param exclue
     *            The set of user attributes to exclude.
     * @return The set of user attributes that comprise the minimum set of
     *         attributes.
     */
    public static Set<SchemaAttributeImpl> getUserAttributeSetWithExclude(Set<String> exclude) {
        // TODO Would be nice to cache these.
        // TODO Should leave required attributes

        Set<SchemaAttributeImpl> attributes = new HashSet<SchemaAttributeImpl>(getUserDefaultAttributeSet());
        for (String name : exclude) {
            SchemaAttributeImpl attribute = getUserAttribute(name);
            if (attribute != null) {
                /*
                 * If the attribute is complex, exclude its sub-attributes,
                 * otherwise, exclude the attribute itself.
                 */
                if (!SchemaAttribute.TYPE_COMPLEX.equalsIgnoreCase(attribute.getType())) {
                    attributes.remove(attribute);
                } else {
                    attributes.removeAll(attribute.getSubAttributes());
                }
            }
        }

        attributes.addAll(getUserMinimumAttributeSet());
        return Collections.unmodifiableSet(attributes);
    }

    /**
     * Get the set of user attributes to return when a request has included a
     * set of attributes to return. The results will include the minimum set
     * plus any explicitly defined attributes.
     *
     * @param include
     *            The set of user attributes to include.
     * @return The set of user attributes that comprise the minimum set of
     *         attributes.
     */
    public static Set<SchemaAttributeImpl> getUserAttributeSetWithInclude(Set<String> include) {
        // TODO Would be nice to cache these.
        // TODO include parent --> include sub-attributes

        Set<SchemaAttributeImpl> attributes = new HashSet<SchemaAttributeImpl>();
        for (String name : include) {
            SchemaAttributeImpl attribute = getUserAttribute(name);
            if (attribute != null && !SchemaAttribute.RETURNED_NEVER.equalsIgnoreCase(attribute.getReturned())) {
                attributes.add(attribute);
            }
        }

        attributes.addAll(getUserMinimumAttributeSet());
        return Collections.unmodifiableSet(attributes);
    }

    /**
     * Get the default set of user attributes to return. The minimum set is
     * composed of those attributes that have their returned characteristic set
     * to "default".
     *
     * @return The set of user attributes that comprise the default set of user
     *         attributes.
     */
    static Set<SchemaAttributeImpl> getUserDefaultAttributeSet() {
        // TODO Would be nice to cache these.

        Set<SchemaAttributeImpl> attributes = new HashSet<SchemaAttributeImpl>();
        for (SchemaAttributeImpl attribute : USER_ATTRIBUTE_MAP.values()) {
            /*
             * Only include non-complex attributes (their sub-attributes will be
             * returned) and attributes that have "returned" set to "default".
             */
            if (SchemaAttribute.RETURNED_DEFAULT.equalsIgnoreCase(attribute.getReturned())
                && !SchemaAttribute.TYPE_COMPLEX.equalsIgnoreCase(attribute.getType())) {
                attributes.add(attribute);
            }
        }
        return Collections.unmodifiableSet(attributes);
    }

    /**
     * Get the minimum set of user attributes to return. The minimum set is
     * composed of those attributes that have their returned characteristic set
     * to "always".
     *
     * @return The set of user attributes that comprise the minimum set of
     *         attributes.
     */
    static Set<SchemaAttributeImpl> getUserMinimumAttributeSet() {
        // TODO Would be nice to cache these.

        Set<SchemaAttributeImpl> attributes = new HashSet<SchemaAttributeImpl>();
        for (SchemaAttributeImpl attribute : USER_ATTRIBUTE_MAP.values()) {
            /*
             * Only include non-complex attributes (their sub-attributes will be
             * returned) and attributes that have "returned" set to "always".
             */
            if (SchemaAttribute.RETURNED_ALWAYS.equalsIgnoreCase(attribute.getReturned())
                && !SchemaAttribute.TYPE_COMPLEX.equalsIgnoreCase(attribute.getType())) {
                attributes.add(attribute);
            }
        }
        return Collections.unmodifiableSet(attributes);
    }

    private static void configureWimUser() {

        List<SchemaAttribute> rootAttributes = new ArrayList<SchemaAttribute>();

        /*
         * urn:ietf:params:scim:schemas:extension:wim:2.0:User:identifier.
         * uniqueId
         */
        SchemaAttributeImpl identifier_uniqueId = new SchemaAttributeImpl("uniqueId", WIMUserImpl.SCHEMA_URN, TYPE_STRING, null, false, "", false, null, false, MUTABILITY_IMMUTABLE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);

        /*
         * urn:ietf:params:scim:schemas:extension:wim:2.0:User:identifier.
         * uniqueName
         */
        SchemaAttributeImpl identifier_uniqueName = new SchemaAttributeImpl("uniqueName", WIMUserImpl.SCHEMA_URN, TYPE_STRING, null, false, "", false, null, false, MUTABILITY_IMMUTABLE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        /*
         * urn:ietf:params:scim:schemas:extension:wim:2.0:User:identifier.
         * externalId
         */
        SchemaAttributeImpl identifier_externalId = new SchemaAttributeImpl("externalId", WIMUserImpl.SCHEMA_URN, TYPE_STRING, null, false, "", false, null, false, MUTABILITY_IMMUTABLE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);

        /*
         * urn:ietf:params:scim:schemas:extension:wim:2.0:User:identifier.
         * externalName
         */
        SchemaAttributeImpl identifier_externalName = new SchemaAttributeImpl("externalName", WIMUserImpl.SCHEMA_URN, TYPE_STRING, null, false, "", false, null, false, MUTABILITY_IMMUTABLE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);

        /*
         * urn:ietf:params:scim:schemas:extension:wim:2.0:User:identifier.
         * repositoryId
         */
        SchemaAttributeImpl identifier_repositoryId = new SchemaAttributeImpl("repositoryId", WIMUserImpl.SCHEMA_URN, TYPE_STRING, null, false, "", false, null, false, MUTABILITY_IMMUTABLE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);

        /*
         * urn:ietf:params:scim:schemas:extension:wim:2.0:User:identifier
         */
        List<SchemaAttribute> subAttributes = Arrays.asList(new SchemaAttribute[] { identifier_uniqueId,
                                                                                    identifier_uniqueName, identifier_externalId, identifier_externalName,
                                                                                    identifier_repositoryId });
        SchemaAttributeImpl identifier = new SchemaAttributeImpl("identifier", WIMUserImpl.SCHEMA_URN, TYPE_COMPLEX, subAttributes, false, "", false, null, false, MUTABILITY_IMMUTABLE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        rootAttributes.add(identifier);

        // TODO "viewIdentifiers", (Entity) -- Not used in Liberty
        // TODO "parent", (Entity) -- Not used in Liberty
        // TODO "children", (Entity) -- Not used in Liberty
        // TODO "groups", (Entity) -- Already exposed in SCIM model
        // TODO "createTimestamp", (Entity) -- Already exposed in SCIM model
        // TODO "modifyTimestamp", (Entity) -- Already exposed in SCIM model
        // TODO "entitlementInfo", (Entity) -- Not used in Liberty
        // TODO "changeType" (Entity) -- Not used in Liberty
        // TODO "partyRoles" (RolePlayer) -- Not used in Liberty

        /*
         * urn:ietf:params:scim:schemas:extension:wim:2.0:User:principalName
         */
        SchemaAttributeImpl principalName = new SchemaAttributeImpl("principalName", WIMUserImpl.SCHEMA_URN, TYPE_STRING, null, false, "", false, null, false, MUTABILITY_IMMUTABLE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        rootAttributes.add(principalName);

        // TODO byte[] "password", (LoginAccount) -- Already exposed in SCIM
        // model
        // TODO String "realm", (LoginAccount)
        // TODO List<byte[]> "certificate" (LoginAccount) -- Already exposed in
        // SCIM model

        /*
         * urn:ietf:params:scim:schemas:extension:wim:2.0:User:uid
         */
        SchemaAttributeImpl uid = new SchemaAttributeImpl("uid", WIMUserImpl.SCHEMA_URN, TYPE_STRING, null, false, "", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        rootAttributes.add(uid);

        /*
         * urn:ietf:params:scim:schemas:extension:wim:2.0:User:cn
         */
        SchemaAttributeImpl cn = new SchemaAttributeImpl("cn", WIMUserImpl.SCHEMA_URN, TYPE_STRING, null, false, "", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        rootAttributes.add(cn);

        /*
         * urn:ietf:params:scim:schemas:extension:wim:2.0:User:sn
         */
        SchemaAttributeImpl sn = new SchemaAttributeImpl("sn", WIMUserImpl.SCHEMA_URN, TYPE_STRING, null, false, "", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        rootAttributes.add(sn);

        /*
         * urn:ietf:params:scim:schemas:extension:wim:2.0:User:preferredLanguage
         */
        SchemaAttributeImpl preferredLanguage = new SchemaAttributeImpl("preferredLanguage", WIMUserImpl.SCHEMA_URN, TYPE_STRING, null, false, "", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        rootAttributes.add(preferredLanguage);

        /*
         * urn:ietf:params:scim:schemas:extension:wim:2.0:User:displayName
         */
        SchemaAttributeImpl displayName = new SchemaAttributeImpl("displayName", WIMUserImpl.SCHEMA_URN, TYPE_STRING, null, true, "", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        rootAttributes.add(displayName);

        /*
         * urn:ietf:params:scim:schemas:extension:wim:2.0:User:initials
         */
        SchemaAttributeImpl initials = new SchemaAttributeImpl("initials", WIMUserImpl.SCHEMA_URN, TYPE_STRING, null, true, "", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        rootAttributes.add(initials);

        /*
         * urn:ietf:params:scim:schemas:extension:wim:2.0:User:mail
         */
        SchemaAttributeImpl mail = new SchemaAttributeImpl("mail", WIMUserImpl.SCHEMA_URN, TYPE_STRING, null, false, "", false, null, false, MUTABILITY_READ_WRITE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        rootAttributes.add(mail);

        // TODO MORE WIM PROPERTIES
        // "ibmPrimaryEmail" --- ibm-primaryEmail???
        // "jpegPhoto",
        // "labeledURI",
        // "carLicense",
        // "telephoneNumber",
        // "facsimileTelephoneNumber",
        // "pager",
        // "mobile",
        // "homePostalAddress",
        // "postalAddress",
        // "roomNumber",
        // "l",
        // "localityName",
        // "st",
        // "stateOrProvinceName",
        // "street",
        // "postalCode",
        // "city",
        // "employeeType",
        // "employeeNumber",
        // "manager",
        // "secretary",
        // "departmentNumber",
        // "title",
        // "ibmJobTitle",
        // "c",
        // "countryName",
        // "givenName",
        // "homeStreet",
        // "homeCity",
        // "homeStateOrProvinceName",
        // "homePostalCode",
        // "homeCountryName",
        // "businessStreet",
        // "businessCity",
        // "businessStateOrProvinceName",
        // "businessPostalCode",
        // "businessCountryName",
        // "description",
        // "businessCategory",
        // "seeAlso",
        // "kerberosId",
        // "photoURL",
        // "photoURLThumbnail",
        // "middleName",
        // "honorificPrefix",
        // "honorificSuffix",
        // "nickName",
        // "profileUrl",
        // "timezone",
        // "locale",
        // "ims",
        // "active"

        /*
         * Add the attributes to the user attribute name map. We do this later
         * b/c the sub-attribute/parent relationships are set when adding the
         * sub-attributes to the parent. If we add these earlier, the annotated
         * names might not include the parent and the mappings will fail.
         */
        addToUserAttributeMap(identifier);
        addToUserAttributeMap(identifier_uniqueId);
        addToUserAttributeMap(identifier_uniqueName);
        addToUserAttributeMap(identifier_externalId);
        addToUserAttributeMap(identifier_externalName);
        addToUserAttributeMap(identifier_repositoryId);
        addToUserAttributeMap(principalName);
        addToUserAttributeMap(uid);
        addToUserAttributeMap(cn);
        addToUserAttributeMap(sn);
        addToUserAttributeMap(preferredLanguage);
        addToUserAttributeMap(displayName);
        addToUserAttributeMap(initials);
        addToUserAttributeMap(mail);

        /*
         * Create the Schema instance and store it in the map.
         */
        SchemaImpl schema = new SchemaImpl();
        schema.setDescription("WIM User Account Extension");
        schema.setId(WIMUserImpl.SCHEMA_URN);
        schema.setName("WIM User");
        schema.setAttributes(rootAttributes);

        SCHEMA_MAP.put(WIMUserImpl.SCHEMA_URN, schema);
    }

    public static void configureWimGroup() {

        List<SchemaAttribute> rootAttributes = new ArrayList<SchemaAttribute>();

        /*
         * urn:ietf:params:scim:schemas:extension:wim:2.0:User:identifier.
         * uniqueId
         */
        SchemaAttributeImpl identifier_uniqueId = new SchemaAttributeImpl("uniqueId", WIMGroupImpl.SCHEMA_URN, TYPE_STRING, null, false, "", false, null, false, MUTABILITY_IMMUTABLE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);

        /*
         * urn:ietf:params:scim:schemas:extension:wim:2.0:User:identifier.
         * uniqueName
         */
        SchemaAttributeImpl identifier_uniqueName = new SchemaAttributeImpl("uniqueName", WIMGroupImpl.SCHEMA_URN, TYPE_STRING, null, false, "", false, null, false, MUTABILITY_IMMUTABLE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        /*
         * urn:ietf:params:scim:schemas:extension:wim:2.0:User:identifier.
         * externalId
         */
        SchemaAttributeImpl identifier_externalId = new SchemaAttributeImpl("externalId", WIMGroupImpl.SCHEMA_URN, TYPE_STRING, null, false, "", false, null, false, MUTABILITY_IMMUTABLE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);

        /*
         * urn:ietf:params:scim:schemas:extension:wim:2.0:User:identifier.
         * externalName
         */
        SchemaAttributeImpl identifier_externalName = new SchemaAttributeImpl("externalName", WIMGroupImpl.SCHEMA_URN, TYPE_STRING, null, false, "", false, null, false, MUTABILITY_IMMUTABLE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);

        /*
         * urn:ietf:params:scim:schemas:extension:wim:2.0:User:identifier.
         * repositoryId
         */
        SchemaAttributeImpl identifier_repositoryId = new SchemaAttributeImpl("repositoryId", WIMGroupImpl.SCHEMA_URN, TYPE_STRING, null, false, "", false, null, false, MUTABILITY_IMMUTABLE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);

        /*
         * urn:ietf:params:scim:schemas:extension:wim:2.0:User:identifier
         */
        List<SchemaAttribute> subAttributes = Arrays.asList(new SchemaAttribute[] { identifier_uniqueId,
                                                                                    identifier_uniqueName, identifier_externalId, identifier_externalName,
                                                                                    identifier_repositoryId });
        SchemaAttributeImpl identifier = new SchemaAttributeImpl("identifier", WIMGroupImpl.SCHEMA_URN, TYPE_COMPLEX, subAttributes, false, "", false, null, false, MUTABILITY_IMMUTABLE, RETURNED_DEFAULT, UNIQUENESS_NONE, null);
        rootAttributes.add(identifier);

        // TODO Remaining attributes.

        /*
         * Add the attributes to the user attribute name map. We do this later
         * b/c the sub-attribute/parent relationships are set when adding the
         * sub-attributes to the parent. If we add these earlier, the annotated
         * names might not include the parent and the mappings will fail.
         */
        addToGroupAttributeMap(identifier);
        addToGroupAttributeMap(identifier_uniqueId);
        addToGroupAttributeMap(identifier_uniqueName);
        addToGroupAttributeMap(identifier_externalId);
        addToGroupAttributeMap(identifier_externalName);
        addToGroupAttributeMap(identifier_repositoryId);

        /*
         * Create the Schema instance and store it in the map.
         */
        SchemaImpl schema = new SchemaImpl();
        schema.setDescription("WIM Group Extension");
        schema.setId(WIMGroupImpl.SCHEMA_URN);
        schema.setName("WIM Group");
        schema.setAttributes(rootAttributes);

        SCHEMA_MAP.put(WIMGroupImpl.SCHEMA_URN, schema);
    }
}
