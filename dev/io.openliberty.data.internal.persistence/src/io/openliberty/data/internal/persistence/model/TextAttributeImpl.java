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
import jakarta.data.metamodel.TextAttribute;

/**
 * Attribute information for the static metamodel.
 */
@Trivial
public record TextAttributeImpl(
                String name,
                Sort asc,
                Sort ascIgnoreCase,
                Sort desc,
                Sort descIgnoreCase)
                implements TextAttribute {

    public static TextAttributeImpl create(String name) {
        return new TextAttributeImpl(name, //
                        Sort.asc(name), //
                        Sort.ascIgnoreCase(name), //
                        Sort.desc(name), //
                        Sort.descIgnoreCase(name));
    }
}
