/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jakarta.data.restrict;

import java.util.List;

/**
 * Method signatures are copied from Jakarta Data.
 */
class Unmatchable<T> implements CompositeRestriction<T> {
    static final Unmatchable<?> INSTANCE = new Unmatchable<>();

    // prevent instantiation by others
    private Unmatchable() {
    }

    @Override
    public boolean isNegated() {
        return false;
    }

    @Override
    public CompositeRestriction<T> negate() {
        @SuppressWarnings("unchecked")
        CompositeRestriction<T> r = (CompositeRestriction<T>) Unrestricted.INSTANCE;
        return r;
    }

    @Override
    public List<Restriction<? super T>> restrictions() {
        return List.of();
    }

    @Override
    public String toString() {
        return "UNMATCHABLE";
    }

    @Override
    public Type type() {
        return Type.ANY;
    }

}
