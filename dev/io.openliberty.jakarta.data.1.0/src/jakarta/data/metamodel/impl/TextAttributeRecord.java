/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

import jakarta.data.Sort;
import jakarta.data.metamodel.TextAttribute;

/**
 * Method signatures are copied from Jakarta Data.
 */
public record TextAttributeRecord<T>(String name) implements TextAttribute<T> {

    @Override
    public Sort<T> asc() {
        return Sort.asc(name);
    }

    @Override
    public Sort<T> ascIgnoreCase() {
        return Sort.ascIgnoreCase(name);
    }

    @Override
    public Sort<T> desc() {
        return Sort.desc(name);
    }

    @Override
    public Sort<T> descIgnoreCase() {
        return Sort.descIgnoreCase(name);
    }
}
