/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.data.internal;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.data.event.LifecycleEvent;
import jakarta.enterprise.util.TypeLiteral;

/**
 * A TypeLiteral that can be created at run time for the CDI events that
 * Jakarta Data must send for Delete, Insert, Save, and Update life cycle
 * operations.
 *
 * @param <L> type of LifecycleEvent, such as PreDeleteEvent
 * @param <E> entity class to use as the type parameter of the LifecycleEvent
 */
@Trivial
final class EventTypeLiteral<L extends LifecycleEvent<E>, E> //
                extends TypeLiteral<L> {
    private static final long serialVersionUID = 1;

    /**
     * Construct a TypeLiteral at run time for a CDI event for a life cycle
     * operation.
     *
     * @param lifeCycleEventClass one of the life cycle event classes, such as
     *                                PreDeleteEvent or PostDeleteEvent
     * @param entityClass         the entity class for which the event is fired
     */
    public EventTypeLiteral(Class<?> lifeCycleEventClass,
                            Class<E> entityClass) {
        // According to Bob, WELD uses this same workaround for the
        // TypeLiteral.getType() method being final and non-overridable.
        // Reflection is used to assign the field value before it lazily
        // initializes.
        try {
            // actualType = PreDeleteEvent<MyEntity>
            Field f = TypeLiteral.class.getDeclaredField("actualType");
            f.setAccessible(true);
            f.set(this, //
                  new ParameterizedLifeCycleEventType( //
                                  lifeCycleEventClass, //
                                  entityClass));
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    /**
     * Represents a LifeCycle event type with type parameters.
     * For example, PreDeleteEvent<MyEntity>
     */
    private static class ParameterizedLifeCycleEventType //
                    implements ParameterizedType {
        private final Class<?> eventClass;
        private final Type[] eventTypeParamType;

        private ParameterizedLifeCycleEventType(Class<?> eventClass, //
                                                Class<?> entityClass) {
            this.eventClass = eventClass;
            this.eventTypeParamType = new Type[] { entityClass };
        }

        /**
         * equals and hashCode are needed because CDI containers use type equality
         * when matching observers, so they must be consistent with how
         * sun.reflect.generics implements them.
         *
         * @param other instance against which to compare
         * @return true if equal; otherwise false
         */
        @Override
        public boolean equals(Object other) {
            return other instanceof ParameterizedType p
                   && eventClass.equals(p.getRawType())
                   && Arrays.equals(eventTypeParamType, p.getActualTypeArguments());
        }

        @Override
        public Type[] getActualTypeArguments() {
            return eventTypeParamType;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

        @Override
        public Type getRawType() {
            return eventClass;
        }

        /**
         * hashCode and equals are needed because CDI containers use type equality
         * when matching observers, so they must be consistent with how
         * sun.reflect.generics implements them (XOR of raw type hash and args hash).
         *
         * @return the computed hash code
         */
        @Override
        public int hashCode() {
            return Arrays.hashCode(eventTypeParamType) ^ eventClass.hashCode();
        }
    }
}
