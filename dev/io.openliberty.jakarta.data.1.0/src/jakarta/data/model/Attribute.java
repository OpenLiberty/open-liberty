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
package jakarta.data.model;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.data.Sort;
import jakarta.data.exceptions.MappingException;

/**
 * Method signatures are copied from Jakarta Data.
 */
public class Attribute {
    private final AtomicReference<AttributeInfo> info = new AtomicReference<AttributeInfo>();

    private Attribute() {
    }

    private final AttributeInfo attrInfo() {
        AttributeInfo attrInfo = info.get();
        if (attrInfo == null)
            throw new MappingException("Not found");
        return attrInfo;

    }

    public final Sort asc() {
        return attrInfo().asc();
    }

    public final Sort ascIgnoreCase() {
        return attrInfo().ascIgnoreCase();
    }

    public static final Attribute create() {
        return new Attribute();
    }

    public final Sort desc() {
        return attrInfo().desc();
    }

    public final Sort descIgnoreCase() {
        return attrInfo().descIgnoreCase();
    }

    public static final Attribute get() {
        return new Attribute();
    }

    public final boolean init(AttributeInfo attrInfo) {
        return info.compareAndSet(null, attrInfo);
    }

    public final String name() {
        return attrInfo().name();
    }
}
