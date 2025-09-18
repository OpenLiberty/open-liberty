/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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
package jakarta.data.page;

import java.util.Arrays;
import java.util.List;

import jakarta.data.messages.Messages;

/**
 */
public class PageRequestCursor implements PageRequest.Cursor {
    private final Object[] keyComponents;

    PageRequestCursor(Object... keyComponents) {
        this.keyComponents = keyComponents;

        if (keyComponents == null || keyComponents.length < 1)
            throw new IllegalArgumentException(Messages.get("006.zero.size.key"));
    }

    @Override
    public List<?> elements() {
        return List.of(keyComponents);
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o != null && o.getClass().equals(getClass()) && Arrays.equals(((PageRequestCursor) o).keyComponents, keyComponents);
    }

    @Override
    public Object get(int index) {
        return keyComponents[index];
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(keyComponents);
    }

    @Override
    public int size() {
        return keyComponents.length;
    }

    @Override
    public String toString() {
        return new StringBuilder("Cursor@").append(Integer.toHexString(hashCode())) //
                        .append(" with ").append(keyComponents.length).append(" values") //
                        .toString();
    }
}
