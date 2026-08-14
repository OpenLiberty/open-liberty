/*******************************************************************************
 * Copyright (c) 2024,2026 IBM Corporation and others.
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
package jakarta.data.metamodel.impl;

import jakarta.annotation.Nonnull;
import jakarta.data.Sort;
import jakarta.data.metamodel.TextAttribute;

/**
 * Method signatures are copied from Jakarta Data.
 */
@Deprecated(since = "1.1")
public record TextAttributeRecord<T>(@Nonnull String name) implements TextAttribute<T> {

    @Override
    @Nonnull
    public Sort<T> asc() {
        return Sort.asc(name);
    }

    @Override
    @Nonnull
    public Sort<T> ascIgnoreCase() {
        return Sort.ascIgnoreCase(name);
    }

    @Override
    @Nonnull
    public Sort<T> desc() {
        return Sort.desc(name);
    }

    @Override
    @Nonnull
    public Sort<T> descIgnoreCase() {
        return Sort.descIgnoreCase(name);
    }
}
