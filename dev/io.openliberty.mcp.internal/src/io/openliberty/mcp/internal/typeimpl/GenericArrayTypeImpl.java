/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.typeimpl;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 *
 */
public class GenericArrayTypeImpl implements GenericArrayType {

    private Type genericComponentType;

    /**
     * @param genericComponentType
     */
    public GenericArrayTypeImpl(Type genericComponentType) {
        super();
        this.genericComponentType = genericComponentType;
    }

    /** {@inheritDoc} */
    @Override
    public Type getGenericComponentType() {
        return this.genericComponentType;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GenericArrayTypeImpl gat) {
            if (Objects.equals(gat.genericComponentType, this.genericComponentType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(genericComponentType);
    }

}
