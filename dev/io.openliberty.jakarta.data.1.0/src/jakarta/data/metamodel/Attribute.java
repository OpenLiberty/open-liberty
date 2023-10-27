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
package jakarta.data.metamodel;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.data.exceptions.MappingException;

/**
 * Method signatures are copied from Jakarta Data.
 */
public interface Attribute {

    public String name();

    public default boolean init(Attribute attr) {
        return false;
    }

    public static Attribute get() {
        return new Attribute() {
            private final AtomicReference<Attribute> impl = new AtomicReference<Attribute>();

            private final Attribute attr() {
                Attribute attr = impl.get();
                if (attr == null)
                    throw new MappingException("The static metamodel for this attribute has not been initialized by a Jakarta Data provider.");
                return attr;

            }

            @Override
            public boolean init(Attribute attr) {
                if (attr == null)
                    throw new NullPointerException();
                else
                    return impl.compareAndSet(null, attr);
            }

            @Override
            public String name() {
                return attr().name();
            }

            @Override
            public String toString() {
                Attribute attr = impl.get();
                String attrName = attr == null ? "[uninitialized]" : attr.name();
                String className = getClass().getSimpleName();
                return new StringBuilder(className.length() + 1 + attrName.length()).append(className).append(':').append(attrName).toString();
            }
        };
    }
}
