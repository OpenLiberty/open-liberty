/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
import jakarta.data.metamodel.SortableAttribute;

/**
 * Attribute information for the static metamodel.
 */
@Trivial
public record SortableAttributeImpl(
                String name,
                Sort asc,
                Sort desc)
                implements SortableAttribute {

    public static SortableAttributeImpl create(String name) {
        return new SortableAttributeImpl(name, //
                        Sort.asc(name), //
                        Sort.desc(name));
    }
}
