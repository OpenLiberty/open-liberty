/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data.internal.cdi;

/**
 * A key for a group of entities for the same backend database provider
 * that are loaded with the same class loader.
 */
class EntityGroupKey {
    private final int hash;
    final ClassLoader loader;
    final String provider;

    EntityGroupKey(String provider, ClassLoader loader) {
        this.loader = loader;
        this.provider = provider;
        hash = loader.hashCode() + provider.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        EntityGroupKey k;
        return o instanceof EntityGroupKey
               && provider.equals((k = (EntityGroupKey) o).provider)
               && loader.equals(k.loader);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}