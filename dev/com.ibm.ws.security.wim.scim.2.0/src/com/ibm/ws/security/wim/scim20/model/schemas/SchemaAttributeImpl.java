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

package com.ibm.ws.security.wim.scim20.model.schemas;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.ibm.websphere.security.wim.scim20.model.schemas.SchemaAttribute;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

// TODO Implement hashCode
@JsonInclude(Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
@JsonPropertyOrder(value = { "name", "type", "subAttributes", "multiValued", "description", "required",
                             "canonicalValues", "caseExact", "mutability", "returned", "uniqueness", "referenceTypes" })
public class SchemaAttributeImpl implements SchemaAttribute {

    @JsonProperty("canonicalValues")
    private List<String> canonicalValues;

    @JsonProperty("caseExact")
    private Boolean caseExact;

    @JsonProperty("description")
    private String description;

    @JsonProperty("multiValued")
    private Boolean multiValued;

    @JsonProperty("mutability")
    private String mutability;

    @JsonProperty("name")
    private String name;

    /**
     * Parent attribute for this attribute. This implies that this attribute is
     * a sub-attribute of the complex type parent.
     *
     * THIS IS NOT PART OF THE SCIM SCHEMA.
     */
    private SchemaAttributeImpl parent;

    @JsonProperty("referenceTypes")
    private List<String> referenceTypes;

    @JsonProperty("required")
    private Boolean required;

    @JsonProperty("returned")
    private String returned;

    /**
     * Schema URN for this attribute.
     *
     * THIS IS NOT PART OF THE SCIM SCHEMA.
     */
    private String schemaUrn;

    @JsonProperty("subAttributes")
    private List<SchemaAttribute> subAttributes;

    @JsonProperty("type")
    private String type;

    @JsonProperty("uniqueness")
    private String uniqueness;

    /**
     * The WIM property in PersonAccount or Group this attribute maps to.
     *
     * THIS IS NOT PART OF THE SCIM SCHEMA.
     */
    private String wimProperty;

    public SchemaAttributeImpl() {
        // Must exist for JSON deserialization.
    }

    public SchemaAttributeImpl(String name, String schemaUrn, String type, List<SchemaAttribute> subAttributes,
                               Boolean multiValued, String description, Boolean required, List<String> canonicalValues, Boolean caseExact,
                               String mutability, String returned, String uniqueness, List<String> referenceTypes) {
        this.name = name;
        this.schemaUrn = schemaUrn;
        this.type = type;
        this.subAttributes = subAttributes;
        this.multiValued = multiValued;
        this.description = description;
        this.required = required;
        this.canonicalValues = canonicalValues;
        this.caseExact = caseExact;
        this.mutability = mutability;
        this.returned = returned;
        this.uniqueness = uniqueness;
        this.referenceTypes = referenceTypes;

        /*
         * Set as parent of any sub-attributes.
         */
        if (this.subAttributes != null) {
            for (SchemaAttribute subAttr : subAttributes) {
                ((SchemaAttributeImpl) subAttr).parent = this;
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SchemaAttributeImpl other = (SchemaAttributeImpl) obj;
        if (canonicalValues == null) {
            if (other.canonicalValues != null) {
                return false;
            }
        } else if (!canonicalValues.equals(other.canonicalValues)) {
            return false;
        }
        if (caseExact == null) {
            if (other.caseExact != null) {
                return false;
            }
        } else if (!caseExact.equals(other.caseExact)) {
            return false;
        }
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (multiValued == null) {
            if (other.multiValued != null) {
                return false;
            }
        } else if (!multiValued.equals(other.multiValued)) {
            return false;
        }
        if (mutability == null) {
            if (other.mutability != null) {
                return false;
            }
        } else if (!mutability.equals(other.mutability)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
//
//  TODO See hashCode() for why this is commented out.
//        if (parent == null) {
//            if (other.parent != null) {
//                return false;
//            }
//        } else if (!parent.equals(other.parent)) {
//            return false;
//        }
//
        if (referenceTypes == null) {
            if (other.referenceTypes != null) {
                return false;
            }
        } else if (!referenceTypes.equals(other.referenceTypes)) {
            return false;
        }
        if (required == null) {
            if (other.required != null) {
                return false;
            }
        } else if (!required.equals(other.required)) {
            return false;
        }
        if (returned == null) {
            if (other.returned != null) {
                return false;
            }
        } else if (!returned.equals(other.returned)) {
            return false;
        }
        if (schemaUrn == null) {
            if (other.schemaUrn != null) {
                return false;
            }
        } else if (!schemaUrn.equals(other.schemaUrn)) {
            return false;
        }
        if (subAttributes == null) {
            if (other.subAttributes != null) {
                return false;
            }
        } else if (!subAttributes.equals(other.subAttributes)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        if (uniqueness == null) {
            if (other.uniqueness != null) {
                return false;
            }
        } else if (!uniqueness.equals(other.uniqueness)) {
            return false;
        }
        if (wimProperty == null) {
            if (other.wimProperty != null) {
                return false;
            }
        } else if (!wimProperty.equals(other.wimProperty)) {
            return false;
        }
        return true;
    }

    /**
     * Get the dot-annotated name for this attribute.
     *
     * THIS IS NOT PART OF THE SCIM SCHEMA.
     *
     * @return The dot-annotated name.
     */
    public String getAnnotatedName() {
        if (parent != null) {
            /*
             * Use dot notation for sub-attributes of complex types.
             */
            return parent.getName() + "." + this.name;
        } else {
            return this.name;
        }
    }

    @Override
    public List<String> getCanonicalValues() {
        return this.canonicalValues;
    }

    @Override
    public Boolean getCaseExact() {
        return this.caseExact;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public Boolean getMultiValued() {
        return this.multiValued;
    }

    @Override
    public String getMutability() {
        return this.mutability;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<String> getReferenceTypes() {
        return Collections.unmodifiableList(this.referenceTypes);
    }

    @Override
    public Boolean getRequired() {
        return this.required;
    }

    @Override
    public String getReturned() {
        return this.returned;
    }

    @Override
    public List<SchemaAttribute> getSubAttributes() {
        return this.subAttributes == null ? null : Collections.unmodifiableList(this.subAttributes);
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public String getUniqueness() {
        return this.uniqueness;
    }

    /**
     * Return the full URI for this attribute.
     *
     * THIS IS NOT PART OF THE SCIM SCHEMA.
     *
     * @return The attribute's full URI.
     */
    public String getUrn() {
        return schemaUrn + ":" + getAnnotatedName();
    }

    /**
     * Get the WIM property that this SCIM attribute is mapped to. The WIM
     * property can either be a property in Group or PersonAccount and can be
     * part of the core WIM schema or can be an extended property.
     *
     * THIS IS NOT PART OF THE SCIM SCHEMA.
     *
     * @return The WIM property this attribute is mapped to.
     */
    public String getWimProperty() {
        return this.wimProperty;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((canonicalValues == null) ? 0 : canonicalValues.hashCode());
        result = prime * result + ((caseExact == null) ? 0 : caseExact.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((multiValued == null) ? 0 : multiValued.hashCode());
        result = prime * result + ((mutability == null) ? 0 : mutability.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());

//
// Hashing parent will result in a StackOverflowException since parents and children reference eachother.
// We can skip this anyway since "schemaUrn" includes a representation of the parent attribute in the URN.
//
//        result = prime * result + ((parent == null) ? 0 : parent.hashCode());

        result = prime * result + ((referenceTypes == null) ? 0 : referenceTypes.hashCode());
        result = prime * result + ((required == null) ? 0 : required.hashCode());
        result = prime * result + ((returned == null) ? 0 : returned.hashCode());
        result = prime * result + ((schemaUrn == null) ? 0 : schemaUrn.hashCode());
        result = prime * result + ((subAttributes == null) ? 0 : subAttributes.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((uniqueness == null) ? 0 : uniqueness.hashCode());
        result = prime * result + ((wimProperty == null) ? 0 : wimProperty.hashCode());
        return result;
    }

    /**
     * Set the collection of suggested canonical values that may be used (e.g.,
     * "work" and "home").
     *
     * @param canonicalValues
     *            A collection of suggested canonical values that may be used.
     */
    public void setCanonicalValues(List<String> canonicalValues) {
        this.canonicalValues = canonicalValues;
    }

    /**
     * Set a Boolean value that specifies whether or not a string attribute is
     * case sensitive.
     *
     * @param caseExact
     *            A Boolean value that specifies whether or not a string
     *            attribute is case sensitive.
     */
    public void setCaseExact(Boolean caseExact) {
        this.caseExact = caseExact;
    }

    /**
     * Set the attribute's human-readable description.
     *
     * @param description
     *            The attribute's human-readable description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Set a Boolean value indicating the attribute's plurality.
     *
     * @param multiValued
     *            A Boolean value indicating the attribute's plurality.
     */
    public void setMultiValued(Boolean multiValued) {
        this.multiValued = multiValued;
    }

    /**
     * Set the circumstances under which the value of the attribute can be
     * (re)defined.
     *
     * @param mutability
     *            The circumstances under which the value of the attribute can
     *            be (re)defined.
     *
     * @see #MUTABILITY_IMMUTABLE
     * @see #MUTABILITY_READ_ONLY
     * @see #MUTABILITY_READ_WRITE
     * @see #MUTABILITY_WRITE_ONLY
     */
    public void setMutability(String mutability) {
        this.mutability = mutability;
    }

    /**
     * Set the attribute's name.
     *
     * @param name
     *            The attribute's name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set the list of SCIM resource types that may be referenced when the data
     * type is {@link #TYPE_REFERENCE}.
     *
     * @param referenceTypes
     *            The list of SCIM resource types that may be referenced.
     */
    public void setReferenceTypes(List<String> referenceTypes) {
        this.referenceTypes = referenceTypes;
    }

    /**
     * Set a Boolean value that specifies whether or not the attribute is
     * required.
     *
     * @param required
     *            A Boolean value that specifies whether or not the attribute is
     *            required.
     */
    public void setRequired(Boolean required) {
        this.required = required;
    }

    /**
     * Set when an attribute and associated values are returned in response to a
     * GET request or in response to a PUT, POST, or PATCH request.
     *
     * @param returned
     *            When an attribute and associated values are returned in
     *            response to a GET request or in response to a PUT, POST, or
     *            PATCH request.
     *
     * @see #RETURNED_ALWAYS
     * @see #RETURNED_DEFAULT
     * @see #RETURNED_NEVER
     * @see #RETURNED_REQUEST
     */
    public void setReturned(String returned) {
        this.returned = returned;
    }

    /**
     * Set the set of sub-attributes when the data type is
     * {@link #TYPE_COMPLEX}.
     *
     * @param subAttributes
     *            The set of sub-attributes.
     */
    public void setSubAttributes(List<SchemaAttribute> subAttributes) {
        this.subAttributes = subAttributes;
    }

    /**
     * Set the attribute's data type.
     *
     * @param type
     *            The attribute's data type.
     *
     * @see #TYPE_STRING
     * @see #TYPE_BOOLEAN
     * @see #TYPE_DECIMAL
     * @see #TYPE_INTEGER
     * @see #TYPE_DATE_TIME
     * @see #TYPE_REFERENCE
     * @see #TYPE_COMPLEX
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Set how the service provider enforces uniqueness of attribute values.
     *
     * @param uniqueness
     *            How the service provider enforces uniqueness of attribute
     *            values.
     *
     * @see #UNIQUENESS_GLOBAL
     * @see #UNIQUENESS_NONE
     * @see #UNIQUENESS_SERVER
     */
    public void setUniqueness(String uniqueness) {
        this.uniqueness = uniqueness;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SchemaAttributeImpl [");
        if (canonicalValues != null) {
            builder.append("canonicalValues=");
            builder.append(canonicalValues);
            builder.append(", ");
        }
        if (caseExact != null) {
            builder.append("caseExact=");
            builder.append(caseExact);
            builder.append(", ");
        }
        if (description != null) {
            builder.append("description=");
            builder.append(description);
            builder.append(", ");
        }
        if (multiValued != null) {
            builder.append("multiValued=");
            builder.append(multiValued);
            builder.append(", ");
        }
        if (mutability != null) {
            builder.append("mutability=");
            builder.append(mutability);
            builder.append(", ");
        }
        if (name != null) {
            builder.append("name=");
            builder.append(name);
            builder.append(", ");
        }
        if (parent != null) {
            builder.append("parent=");
            builder.append(parent);
            builder.append(", ");
        }
        if (referenceTypes != null) {
            builder.append("referenceTypes=");
            builder.append(referenceTypes);
            builder.append(", ");
        }
        if (required != null) {
            builder.append("required=");
            builder.append(required);
            builder.append(", ");
        }
        if (returned != null) {
            builder.append("returned=");
            builder.append(returned);
            builder.append(", ");
        }
        if (schemaUrn != null) {
            builder.append("schemaUrn=");
            builder.append(schemaUrn);
            builder.append(", ");
        }
        if (subAttributes != null) {
            builder.append("subAttributes=");
            builder.append(subAttributes);
            builder.append(", ");
        }
        if (type != null) {
            builder.append("type=");
            builder.append(type);
            builder.append(", ");
        }
        if (uniqueness != null) {
            builder.append("uniqueness=");
            builder.append(uniqueness);
            builder.append(", ");
        }
        if (wimProperty != null) {
            builder.append("wimProperty=");
            builder.append(wimProperty);
        }
        builder.append("]");
        return builder.toString();
    }
}
