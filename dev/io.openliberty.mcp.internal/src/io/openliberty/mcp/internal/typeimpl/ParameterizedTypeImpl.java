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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 *
 */
public class ParameterizedTypeImpl implements ParameterizedType {

    private Type[] actualtypeArguments;
    private Type rawType;
    private Type ownerType;

    /**
     * @param actualtypeArguments
     * @param rawType
     * @param ownerType
     */
    public ParameterizedTypeImpl(Type rawType, Type... actualtypeArguments) {
        super();
        this.actualtypeArguments = actualtypeArguments;
        this.rawType = rawType;
        this.ownerType = (rawType instanceof ParameterizedType pt) ? pt.getOwnerType() : null;
    }

    /** {@inheritDoc} */
    @Override
    public Type[] getActualTypeArguments() {
        return actualtypeArguments;
    }

    /** {@inheritDoc} */
    @Override
    public Type getRawType() {
        return rawType;
    }

    /** {@inheritDoc} */
    @Override
    public Type getOwnerType() {
        return rawType.getClass().getGenericSuperclass();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ParameterizedTypeImpl pt) {
            if (Objects.equals(pt.ownerType, this.ownerType) && Objects.equals(pt.actualtypeArguments, this.actualtypeArguments) && Objects.equals(pt.rawType, this.rawType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(actualtypeArguments, rawType, ownerType);
    }

}
