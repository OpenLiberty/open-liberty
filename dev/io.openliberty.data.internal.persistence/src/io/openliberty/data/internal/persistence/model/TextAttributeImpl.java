/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
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
package io.openliberty.data.internal.persistence.model;

import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.data.Sort;
import jakarta.data.metamodel.TextAttribute;

/**
 * Attribute information for the static metamodel.
 */
@Trivial
public record TextAttributeImpl<T>(
                String name,
                Sort<T> asc,
                Sort<T> ascIgnoreCase,
                Sort<T> desc,
                Sort<T> descIgnoreCase)
                implements TextAttribute<T> {

    public static <T> TextAttributeImpl<T> create(String name) {
        return new TextAttributeImpl<>(name, //
                        Sort.<T> asc(name), //
                        Sort.<T> ascIgnoreCase(name), //
                        Sort.<T> desc(name), //
                        Sort.<T> descIgnoreCase(name));
    }
}
