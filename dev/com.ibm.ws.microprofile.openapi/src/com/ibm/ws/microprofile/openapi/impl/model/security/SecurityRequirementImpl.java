/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.microprofile.openapi.impl.model.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;

import com.ibm.ws.microprofile.openapi.Constants;

/**
 * SecurityRequirement
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.0-rc2/versions/3.0.md#securityRequirementObject"
 */

public class SecurityRequirementImpl extends LinkedHashMap<String, List<String>> implements SecurityRequirement {
    public SecurityRequirementImpl() {}

    @Override
    public SecurityRequirement addScheme(String name, String item) {
        this.put(name, Arrays.asList(item));
        return this;
    }

    @Override
    public SecurityRequirement addScheme(String name, List<String> item) {
        this.put(name, item);
        return this;
    }

    @Override
    public SecurityRequirement addScheme(String name) {
        this.put(name, new ArrayList<>());
        return this;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class SecurityRequirement {\n");
        sb = !toIndentedString(super.toString()).equals(Constants.NULL_VALUE) ? sb.append("    ").append(toIndentedString(super.toString())).append("\n") : sb.append("");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Converts the given object to string with each line indented by 4 spaces
     * (except the first line).
     * This method adds formatting to the general toString() method.
     *
     * @param o Java object to be represented as String
     * @return Formatted String representation of the object
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

}
