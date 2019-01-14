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
package com.ibm.ws.microprofile.metrics.helper;

import java.util.Map;

/**
 * @author hrupp
 */
@SuppressWarnings("unused")
public class Tag {
    String key;
    String value;

    public Tag() {}

    public Tag(String kvString) {
        if (kvString == null || kvString.isEmpty() || !kvString.contains("=")) {
            throw new IllegalArgumentException("Not a k=v pair: " + kvString);
        }
        String[] kv = kvString.split("=");
        if (kv.length != 2) {
            throw new IllegalArgumentException("Not a k=v pair: " + kvString);
        }
        key = kv[0].trim();
        value = kv[1].trim();
    }

    public Tag(String key, String value) {
        this.key = key.trim();
        this.value = value.trim();
    }

    public Tag(Map<String, String> tag) {
        this(tag.get("key"), tag.get("value"));
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Tag tag = (Tag) o;

        if (!key.equals(tag.key))
            return false;
        return value.equals(tag.value);
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Tag{");
        sb.append("key='").append(key).append('\'');
        sb.append(", value='").append(value).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
