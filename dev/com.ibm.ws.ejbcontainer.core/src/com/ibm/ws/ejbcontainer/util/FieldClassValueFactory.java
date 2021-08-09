/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.util;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

public abstract class FieldClassValueFactory {
    private static FieldClassValueFactory factory = createFactory();

    private static FieldClassValueFactory createFactory() {
        try {
            // ClassValue is available in Java 7 only.
            Class.forName("java.lang.ClassValue");
            return new Impl();
        } catch (ClassNotFoundException e) {
            return new ReflectionImpl();
        }
    }

    /**
     * Creates an accessor for declared fields that have been made accessible.
     *
     * @param fieldName the field name
     * @return an accessor that returns declared fields that have been made accessible
     */
    public static FieldClassValue create(String fieldName) {
        return factory.createImpl(fieldName);
    }

    static Field getDeclaredField(final Class<?> klass, final String fieldName) {
        return AccessController.doPrivileged(new PrivilegedAction<Field>() {
            @Override
            public Field run() {
                Field field;
                try {
                    field = klass.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    throw new IllegalStateException(e);
                }

                field.setAccessible(true);
                return field;
            }
        });
    }

    private static class Impl extends FieldClassValueFactory {
        @Override
        FieldClassValue createImpl(String fieldName) {
            return new FieldClassValueImpl(fieldName);
        }

        private static class FieldClassValueImpl extends ClassValue<Field> implements FieldClassValue {
            private final String fieldName;

            FieldClassValueImpl(String fieldName) {
                this.fieldName = fieldName;
            }

            @Override
            protected Field computeValue(Class<?> klass) {
                // Find the Field once, and ClassValue will cache it.
                return getDeclaredField(klass, fieldName);
            }
        }
    }

    private static class ReflectionImpl extends FieldClassValueFactory {
        @Override
        FieldClassValue createImpl(String fieldName) {
            return new ReflectionFieldClassValueImpl(fieldName);
        }

        private static class ReflectionFieldClassValueImpl implements FieldClassValue {
            private final String fieldName;

            ReflectionFieldClassValueImpl(String fieldName) {
                this.fieldName = fieldName;
            }

            @Override
            public Field get(Class<?> klass) {
                // Find the Field every time.
                return getDeclaredField(klass, fieldName);
            }
        }
    }

    FieldClassValueFactory() {}

    abstract FieldClassValue createImpl(String fieldName);
}
