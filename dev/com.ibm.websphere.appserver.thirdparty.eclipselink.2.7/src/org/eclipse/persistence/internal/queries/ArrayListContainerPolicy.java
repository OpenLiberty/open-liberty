/*******************************************************************************
 * Copyright (c) 1998, 2018 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     10/28/2008-1.1 James Sutherland - initial implementation
 ******************************************************************************/
package org.eclipse.persistence.internal.queries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import org.eclipse.persistence.internal.sessions.AbstractSession;

/**
 * PERF: Avoids reflection usage for ArrayList.
 */
public class ArrayListContainerPolicy extends ListContainerPolicy {
    /**
     * INTERNAL:
     * Construct a new policy.
     */
    public ArrayListContainerPolicy() {
        super();
    }

    /**
     * INTERNAL:
     * Construct a new policy for the specified class.
     */
    public ArrayListContainerPolicy(Class containerClass) {
        super(containerClass);
    }

    /**
     * INTERNAL:
     * Construct a new policy for the specified class name.
     */
    public ArrayListContainerPolicy(String containerClassName) {
        super(containerClassName);
    }

    /**
     * INTERNAL:
     * Return a clone of the specified container.
     */
    @Override
    public Object cloneFor(Object container) {
        if (container == null) {
            return null;
        }
        if (container.getClass() != ArrayList.class) {
            return super.cloneFor(container);
        }

        return ((ArrayList)container).clone();
    }

    /**
     * INTERNAL:
     * Return an ArrayList from the Vector.
     */
    @Override
    public Object buildContainerFromVector(Vector vector, AbstractSession session) {
        return new ArrayList(vector);
    }

    /**
     * INTERNAL:
     * Return a new ArrayList.
     */
    @Override
    public Object containerInstance() {
        return new ArrayList();
    }

    /**
     * INTERNAL:
     * Return a new ArrayList.
     */
    @Override
    public Object containerInstance(int initialCapacity) {
        return new ArrayList(initialCapacity);
    }
}
