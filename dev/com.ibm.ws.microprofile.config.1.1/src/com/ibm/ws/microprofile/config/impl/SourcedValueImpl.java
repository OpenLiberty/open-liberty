/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.impl;

import java.lang.reflect.Type;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.interfaces.SourcedValue;

/**
 * A value, the type of the value and the id of its source
 */
public class SourcedValueImpl implements SourcedValue {

    private final Object value;
    private final Type type;
    private final String source;
    private final String key;
    private final Class<?> genericSubType;

    @Trivial
    public SourcedValueImpl(String key, Object value, Type type, String source) {
        this(key, value, type, null, source);
    }

    @Trivial
    public SourcedValueImpl(String key, Object value, Type type, Class<?> genericSubType, String source) {
        this.key = key;
        this.value = value;
        this.type = type;
        this.genericSubType = genericSubType;
        this.source = source;
    }

    @Trivial
    public SourcedValueImpl(SourcedValue original, Object value) {
        this.key = original.getKey();
        this.value = value;
        this.type = original.getType();
        this.genericSubType = original.getGenericSubType();
        this.source = original.getSource();
    }

    /**
     * Get the key
     *
     * @return the key
     */
    @Override
    @Trivial
    public String getKey() {
        return this.key;
    }

    /**
     * Get the actual value
     *
     * @return the value
     */
    @Override
    @Trivial
    public Object getValue() {
        return this.value;
    }

    /**
     * Get the type of the value
     *
     * @return
     */
    @Override
    @Trivial
    public Type getType() {
        return this.type;
    }

    /**
     * Get the ID of the source that provided the value
     *
     * @return the originating source ID
     */
    @Override
    @Trivial
    public String getSource() {
        return this.source;
    }

    @Override
    @Trivial
    public String toString() {
        return "[" + this.source + "; " + this.type + "] " + this.key + "=" + this.value;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public Class<?> getGenericSubType() {
        return this.genericSubType;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((this.genericSubType == null) ? 0 : this.genericSubType.hashCode());
        result = (prime * result) + ((this.key == null) ? 0 : this.key.hashCode());
        result = (prime * result) + ((this.source == null) ? 0 : this.source.hashCode());
        result = (prime * result) + ((this.type == null) ? 0 : this.type.hashCode());
        result = (prime * result) + ((this.value == null) ? 0 : this.value.hashCode());
        return result;
    }

    /** {@inheritDoc} */
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
        SourcedValueImpl other = (SourcedValueImpl) obj;
        if (this.genericSubType == null) {
            if (other.genericSubType != null) {
                return false;
            }
        } else if (!this.genericSubType.equals(other.genericSubType)) {
            return false;
        }
        if (this.key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!this.key.equals(other.key)) {
            return false;
        }
        if (this.source == null) {
            if (other.source != null) {
                return false;
            }
        } else if (!this.source.equals(other.source)) {
            return false;
        }
        if (this.type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!this.type.equals(other.type)) {
            return false;
        }
        if (this.value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!this.value.equals(other.value)) {
            return false;
        }
        return true;
    }

}
