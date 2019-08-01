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

package com.ibm.websphere.security.wim.scim20.model.schemas;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ibm.ws.security.wim.scim20.model.schemas.SchemaAttributeImpl;

/**
 * A service provider attribute and its qualities.
 */
@JsonDeserialize(as = SchemaAttributeImpl.class)
public interface SchemaAttribute {

    public static final String MUTABILITY_IMMUTABLE = "immutable";
    public static final String MUTABILITY_READ_ONLY = "readOnly";
    public static final String MUTABILITY_READ_WRITE = "readWrite";
    public static final String MUTABILITY_WRITE_ONLY = "writeOnly";

    public static final String RETURNED_ALWAYS = "always";
    public static final String RETURNED_DEFAULT = "default";
    public static final String RETURNED_NEVER = "never";
    public static final String RETURNED_REQUEST = "request";

    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_COMPLEX = "complex";
    public static final String TYPE_DATE_TIME = "dateTime";
    public static final String TYPE_DECIMAL = "decimal";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_REFERENCE = "reference";
    public static final String TYPE_STRING = "string";

    public static final String UNIQUENESS_GLOBAL = "global";
    public static final String UNIQUENESS_NONE = "none";
    public static final String UNIQUENESS_SERVER = "server";

    /**
     * Get the collection of suggested canonical values that may be used (e.g.,
     * "work" and "home").
     *
     * @return A collection of suggested canonical values that may be used.
     */
    public List<String> getCanonicalValues();

    /**
     * Get a Boolean value that specifies whether or not a string attribute is
     * case sensitive.
     *
     * @return A Boolean value that specifies whether or not a string attribute
     *         is case sensitive.
     */
    public Boolean getCaseExact();

    /**
     * Get the attribute's human-readable description.
     *
     * @return The attribute's human-readable description.
     */
    public String getDescription();

    /**
     * Get a Boolean value indicating the attribute's plurality.
     *
     * @return A Boolean value indicating the attribute's plurality.
     */
    public Boolean getMultiValued();

    /**
     * Get the circumstances under which the value of the attribute can be
     * (re)defined.
     *
     * @return The circumstances under which the value of the attribute can be
     *         (re)defined.
     *
     * @see #MUTABILITY_IMMUTABLE
     * @see #MUTABILITY_READ_ONLY
     * @see #MUTABILITY_READ_WRITE
     * @see #MUTABILITY_WRITE_ONLY
     */
    public String getMutability();

    /**
     * Get the attribute's name.
     *
     * @return The attribute's name.
     */
    public String getName();

    /**
     * Get the list of SCIM resource types that may be referenced when the data
     * type is {@link #TYPE_REFERENCE}.
     *
     * @return The list of SCIM resource types that may be referenced.
     */
    public List<String> getReferenceTypes();

    /**
     * Get a Boolean value that specifies whether or not the attribute is
     * required.
     *
     * @return A Boolean value that specifies whether or not the attribute is
     *         required.
     */
    public Boolean getRequired();

    /**
     * Get when an attribute and associated values are returned in response to a
     * GET request or in response to a PUT, POST, or PATCH request.
     *
     * @return When an attribute and associated values are returned in response
     *         to a GET request or in response to a PUT, POST, or PATCH request.
     *
     * @see #RETURNED_ALWAYS
     * @see #RETURNED_DEFAULT
     * @see #RETURNED_NEVER
     * @see #RETURNED_REQUEST
     */
    public String getReturned();

    /**
     * Get the set of sub-attributes when the data type is
     * {@link #TYPE_COMPLEX}.
     *
     * @return The set of sub-attributes.
     */
    public List<SchemaAttribute> getSubAttributes();

    /**
     * Get the attribute's data type.
     *
     * @return The attribute's data type.
     *
     * @see #TYPE_STRING
     * @see #TYPE_BOOLEAN
     * @see #TYPE_DECIMAL
     * @see #TYPE_INTEGER
     * @see #TYPE_DATE_TIME
     * @see #TYPE_REFERENCE
     * @see #TYPE_COMPLEX
     */
    public String getType();

    /**
     * Get how the service provider enforces uniqueness of attribute values.
     *
     * @return How the service provider enforces uniqueness of attribute values.
     *
     * @see #UNIQUENESS_GLOBAL
     * @see #UNIQUENESS_NONE
     * @see #UNIQUENESS_SERVER
     */
    public String getUniqueness();
}
