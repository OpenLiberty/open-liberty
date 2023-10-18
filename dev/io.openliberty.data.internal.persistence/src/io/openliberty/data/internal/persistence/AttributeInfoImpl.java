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
package io.openliberty.data.internal.persistence;

import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.data.Sort;
import jakarta.data.model.AttributeInfo;

/**
 * Attribute information for the static metamodel.
 */
@Trivial
record AttributeInfoImpl(
                String name,
                Sort asc,
                Sort ascIgnoreCase,
                Sort desc,
                Sort descIgnoreCase)
                implements AttributeInfo {

    static AttributeInfoImpl create(String name) {
        return new AttributeInfoImpl(name, //
                        Sort.asc(name), //
                        Sort.ascIgnoreCase(name), //
                        Sort.desc(name), //
                        Sort.descIgnoreCase(name));
    }
}
