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

package com.ibm.ws.microprofile.openapi.impl.model.media;

import java.util.Map;

import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.media.Encoding;

/**
 * Encoding
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.md#encodingObject"
 */

public class EncodingImpl implements Encoding {
    private String contentType;
    private Map<String, Header> headers;
    private Style style;
    private Boolean explode;
    private Boolean allowReserved;
    private java.util.Map<String, Object> extensions = null;

    public EncodingImpl() {}

    @Override
    public Encoding contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public Encoding headers(Map<String, Header> headers) {
        this.headers = headers;
        return this;
    }

    @Override
    public Map<String, Header> getHeaders() {
        return headers;
    }

    @Override
    public void setHeaders(Map<String, Header> headers) {
        this.headers = headers;
    }

    @Override
    public Encoding style(Style style) {
        this.style = style;
        return this;
    }

    @Override
    public Style getStyle() {
        return style;
    }

    @Override
    public void setStyle(Style style) {
        this.style = style;
    }

    @Override
    public Encoding explode(Boolean explode) {
        this.explode = explode;
        return this;
    }

    @Override
    public Boolean getExplode() {
        return explode;
    }

    @Override
    public void setExplode(Boolean explode) {
        this.explode = explode;
    }

    @Override
    public Encoding allowReserved(Boolean allowReserved) {
        this.allowReserved = allowReserved;
        return this;
    }

    @Override
    public Boolean getAllowReserved() {
        return allowReserved;
    }

    @Override
    public void setAllowReserved(Boolean allowReserved) {
        this.allowReserved = allowReserved;
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
        return "Encoding{" +
               "contentType='" + contentType + '\'' +
               ", headers=" + headers +
               ", style='" + style + '\'' +
               ", explode=" + explode +
               ", allowReserved=" + allowReserved +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EncodingImpl)) {
            return false;
        }

        EncodingImpl encoding = (EncodingImpl) o;

        if (contentType != null ? !contentType.equals(encoding.contentType) : encoding.contentType != null) {
            return false;
        }
        if (headers != null ? !headers.equals(encoding.headers) : encoding.headers != null) {
            return false;
        }
        if (style != null ? !style.equals(encoding.style) : encoding.style != null) {
            return false;
        }
        if (explode != null ? !explode.equals(encoding.explode) : encoding.explode != null) {
            return false;
        }
        if (extensions != null ? !extensions.equals(encoding.extensions) : encoding.extensions != null) {
            return false;
        }
        return allowReserved != null ? allowReserved.equals(encoding.allowReserved) : encoding.allowReserved == null;

    }

    @Override
    public int hashCode() {
        int result = contentType != null ? contentType.hashCode() : 0;
        result = 31 * result + (headers != null ? headers.hashCode() : 0);
        result = 31 * result + (style != null ? style.hashCode() : 0);
        result = 31 * result + (explode != null ? explode.hashCode() : 0);
        result = 31 * result + (extensions != null ? extensions.hashCode() : 0);
        result = 31 * result + (allowReserved != null ? allowReserved.hashCode() : 0);
        return result;
    }
}
