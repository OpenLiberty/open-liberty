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

import org.eclipse.microprofile.openapi.models.parameters.CookieParameter;

/**
 * CookieParameter
 */

public class CookieParameterImpl extends ParameterImpl<CookieParameter> implements CookieParameter {
    private CookieParameter.In in = CookieParameter.In.COOKIE;

    /**
     * returns the in property from a CookieParameter instance.
     *
     * @return String in
     **/

    @Override
    public CookieParameter.In getIn() {
        return in;
    }

    /**
     * Sets the in property of a CookieParameter instance
     * to the parameter.
     *
     * @param in
     */

    @Override
    public void setIn(CookieParameter.In in) {
        this.in = in;
    }

    /**
     * Sets the in property of a CookieParameter instance
     * to the parameter and returns the instance.
     *
     * @param in
     * @return CookieParameter instance with the modified in property
     */

    @Override
    public CookieParameterImpl in(CookieParameter.In in) {
        this.in = in;
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
        CookieParameterImpl cookieParameter = (CookieParameterImpl) o;
        return Objects.equals(this.in, cookieParameter.in) &&
               super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(in, super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class CookieParameter {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    in: ").append(toIndentedString(in)).append("\n");
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
