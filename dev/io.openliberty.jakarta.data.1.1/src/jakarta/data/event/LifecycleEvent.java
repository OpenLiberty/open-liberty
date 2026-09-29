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
package jakarta.data.event;

import jakarta.annotation.Nonnull;
import jakarta.data.messages.Messages;

public abstract class LifecycleEvent<E> {

    @Nonnull
    private final E entityInstance;

    public LifecycleEvent(@Nonnull E entity) {
        Messages.requireNonNull(entity, "entity");
        entityInstance = entity;
    }

    @Nonnull
    public E entity() {
        return entityInstance;
    }
}
