/*
 * Copyright (c) 2016, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package jakarta.json;

import java.io.Serializable;

/**
 * Private implementation of {@link JsonValue} for simple {@link ValueType}s
 * allowing their usage in constants which are better to implement {@link Serializable}.
 *
 * @author Lukas Jungmann
 */
final class JsonValueImpl implements JsonValue, Serializable {

    /** for serialization */
    private static final long serialVersionUID = 83723433120886104L;

    /** Type of this JsonValue. */
    private final ValueType valueType;

    /**
     * Default constructor.
     * @param valueType Type of this JsonValue
     */
    JsonValueImpl(ValueType valueType) {
        this.valueType = valueType;
    }

    /**
     * Returns the value type of this JSON value.
     *
     * @return JSON value type
     */
    @Override
    public ValueType getValueType() {
        return valueType;
    }

    /**
     * Compares the specified object with this {@link JsonValue}
     * object for equality. Returns {@code true} if and only if the
     * specified object is also a JsonValue, and their
     * {@link #getValueType()} objects are <i>equal</i>.
     *
     * @param obj the object to be compared for equality with this JsonValue
     * @return {@code true} if the specified object is equal to this
     * JsonValue
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof JsonValue) {
            return getValueType().equals(((JsonValue) obj).getValueType());
        }
        return false;
    }

    /**
     * Returns the hash code value for this {@link JsonValue} object.
     * The hash code of the {@link JsonValue} object is defined to be
     * its {@link #getValueType()} object's hash code.
     *
     * @return the hash code value for this {@link JsonValue} object
     */
    @Override
    public int hashCode() {
        return valueType.hashCode();
    }

    @Override
    public String toString() {
        return valueType.name().toLowerCase();
    }

}
