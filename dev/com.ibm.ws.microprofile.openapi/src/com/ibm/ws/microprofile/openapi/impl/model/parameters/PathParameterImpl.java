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
package com.ibm.ws.microprofile.openapi.impl.model.parameters;

import java.util.Objects;

import org.eclipse.microprofile.openapi.models.parameters.PathParameter;

/**
 * PathParameter
 */

public class PathParameterImpl extends ParameterImpl<PathParameter> implements PathParameter {
    private In in = In.PATH;
    private Boolean required = true;

    /**
     * returns the in property from a PathParameter instance.
     *
     * @return String in
     **/

    @Override
    public In getIn() {
        return in;
    }

    /**
     * Sets the in property of a PathParameter instance
     * to the parameter.
     *
     * @param in
     */

    @Override
    public void setIn(In in) {
        this.in = in;
    }

    /**
     * Sets the in property of a PathParameter instance
     * to the parameter and returns the instance.
     *
     * @param in
     * @return PathParameter instance with the modified in property
     */

    @Override
    public PathParameterImpl in(In in) {
        this.in = in;
        return this;
    }

    /**
     * returns the required property from a PathParameter instance.
     *
     * @return Boolean required
     **/

    @Override
    public Boolean getRequired() {
        return required;
    }

    /**
     * Sets the required property of a PathParameter instance
     * to the parameter.
     *
     * @param required
     */

    @Override
    public void setRequired(Boolean required) {
        this.required = required;
    }

    /**
     * Sets the required property of a PathParameter instance
     * to the parameter and returns the instance.
     *
     * @param required
     * @return PathParameter instance with the modified required property
     */

    @Override
    public PathParameterImpl required(Boolean required) {
        this.required = required;
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
        PathParameterImpl pathParameter = (PathParameterImpl) o;
        return Objects.equals(this.in, pathParameter.in) &&
               Objects.equals(this.required, pathParameter.required) &&
               super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(in, required, super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class PathParameter {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    in: ").append(toIndentedString(in)).append("\n");
        sb.append("    required: ").append(toIndentedString(required)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
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
