/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package jakarta.data.metamodel;

import jakarta.annotation.Nonnull;

/**
 * Method signatures are copied from Jakarta Data.
 */
record BooleanAttributeRecord<T>(
                @Nonnull Class<T> declaringType,
                @Nonnull String name,
                @Nonnull Class<Boolean> type)
                implements BooleanAttribute<T> {

    @Override
    @Nonnull
    public String toString() {
        return declaringType.getSimpleName().toLowerCase() + '.' + name;
    }
}