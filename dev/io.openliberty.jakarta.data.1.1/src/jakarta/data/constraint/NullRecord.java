/*******************************************************************************
 * Copyright (c) 2025,2026 IBM Corporation and others.
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
package jakarta.data.constraint;

import jakarta.annotation.Nonnull;

/**
 * Method signatures are copied from Jakarta Data.
 */
record NullRecord<V>() implements Null<V> {

    @Nonnull
    static final NullRecord<?> INSTANCE = new NullRecord<>();

    @Override
    @Nonnull
    public NotNull<V> negate() {
        return NotNull.instance();
    }

    @Override
    @Nonnull
    public String toString() {
        return "IS NULL";
    }
}
