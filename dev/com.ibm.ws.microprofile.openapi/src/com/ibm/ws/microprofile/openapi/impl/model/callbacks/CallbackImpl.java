/**
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

package com.ibm.ws.microprofile.openapi.impl.model.callbacks;

import java.util.LinkedHashMap;
import java.util.Objects;

import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;

import com.ibm.ws.microprofile.openapi.Constants;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;

/**
 * Callback
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.md#callbackObject"
 */
public class CallbackImpl extends LinkedHashMap<String, PathItem> implements Callback {
    /**  */
    private static final long serialVersionUID = 1L;

    public CallbackImpl() {}

    private java.util.Map<String, Object> extensions = null;
    private String $ref = null;

    @Override
    public Callback addPathItem(String name, PathItem item) {
        this.put(name, item);
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
        CallbackImpl callback = (CallbackImpl) o;
        return Objects.equals(this.extensions, callback.extensions) &&
               super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(extensions, super.hashCode());
    }

    @Override
    public java.util.Map<String, Object> getExtensions() {
        return extensions;
    }

    @Override
    public void addExtension(String name, Object value) {
        if (this.extensions == null) {
            this.extensions = new java.util.HashMap<>();
        }
        this.extensions.put(name, value);
    }

    @Override
    public void setExtensions(java.util.Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Callback {\n");
        sb = (!toIndentedString(super.toString()).equals(Constants.NULL_VALUE)) ? sb.append("    ").append(toIndentedString(super.toString())).append("\n") : sb.append("");
        sb = (!toIndentedString($ref).equals(Constants.NULL_VALUE)) ? sb.append("    $ref: ").append(toIndentedString($ref)).append("\n") : sb.append("");
        sb = (!toIndentedString(extensions).equals(Constants.NULL_VALUE)) ? sb.append("    extensions: ").append(OpenAPIUtils.mapToString(extensions)).append("\n") : sb.append("");

        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

    /** {@inheritDoc} */
    @Override
    public String getRef() {
        return this.$ref;
    }

    /** {@inheritDoc} */
    @Override
    public void setRef(String ref) {
        if (ref != null && (ref.indexOf(".") == -1 && ref.indexOf("/") == -1)) {
            ref = "#/components/callbacks/" + ref;
        }
        this.$ref = ref;
    }

    /** {@inheritDoc} */
    @Override
    public Callback ref(String ref) {
        setRef(ref);
        return this;
    }

}
