/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.ra.inbound.impl;

/**
 * Utility class for generating string representations of objects.
 */
final class SibRaStringGenerator {

    /**
     * A string buffer containing the string representation.
     */
    private final StringBuffer _buffer;

    /**
     * Flag indicating whether or not the representation has been completed.
     */
    private boolean _completed = false;

    /**
     * Constructor.
     * 
     * @param object
     *            the object for which a string representation is required
     */
    SibRaStringGenerator(final Object object) {

        _buffer = new StringBuffer("[");
        addObjectIdentity(object);

    }

    /**
     * Adds a string representation of the given parent object. Does not call
     * <code>toString</code> on the object to avoid infinite recursion.
     * 
     * @param name
     *            the name of the field
     * @param value
     *            the value of the field
     * @throws IllegalStateException
     *             if the string representation has already been completed
     */
    void addParent(final String name, final Object value)
            throws IllegalStateException {

        checkNotCompleted();
        _buffer.append(" <");
        _buffer.append(name);
        _buffer.append("=");
        addObjectIdentity(value);
        _buffer.append(">");

    }

    /**
     * Adds a string representation of the given object field.
     * 
     * @param name
     *            the name of the field
     * @param value
     *            the value of the field
     * @throws IllegalStateException
     *             if the string representation has already been completed
     */
    void addField(final String name, final Object value)
            throws IllegalStateException {

        checkNotCompleted();
        _buffer.append(" <");
        _buffer.append(name);
        _buffer.append("=");
        _buffer.append(value);
        _buffer.append(">");

    }

    /**
     * Adds a string representation of the given boolean field.
     * 
     * @param name
     *            the name of the field
     * @param value
     *            the value of the field
     * @throws IllegalStateException
     *             if the string representation has already been completed
     */
    void addField(final String name, final boolean value)
            throws IllegalStateException {

        addField(name, Boolean.toString(value));

    }

    /**
     * Adds a string representation of the given password field.
     * 
     * @param name
     *            the name of the field
     * @param value
     *            the value of the field
     * @throws IllegalStateException
     *             if the string representation has already been completed
     */
    void addPasswordField(final String name, final String value)
            throws IllegalStateException {

        addField(name, (value == null) ? null : "*****");

    }

    /**
     * Adds a string representation of the given long field.
     * 
     * @param name
     *            the name of the field
     * @param value
     *            the value of the field
     * @throws IllegalStateException
     *             if the string representation has already been completed
     */
    void addField(final String name, final long value)
            throws IllegalStateException {

        addField(name, Long.toString(value));

    }

    /**
     * Returns a string representation of the object passed on the constructor
     * and the requested fields. Once this method has been called further fields
     * may not be added.
     * 
     * @return a string representation
     */
    String getStringRepresentation() {

        _completed = true;
        _buffer.append("]");
        return _buffer.toString();

    }

    /**
     * Checks that the string represenation has not already been completed by
     * calling <code>getStringRepresentation</code>.
     * 
     * @throws IllegalStateException
     *             if the string representation has been completed
     */
    private void checkNotCompleted() throws IllegalStateException {

        if (_completed) {
            throw new IllegalStateException();
        }

    }

    /**
     * Adds a string identifying the given object to the buffer.
     * 
     * @param object
     *            the object to add
     */
    private void addObjectIdentity(final Object object) {

        if (object == null) {
            _buffer.append("null");
        } else {
            _buffer.append(object.getClass().getName());
            _buffer.append("@");
            _buffer
                    .append(Integer
                            .toHexString(System.identityHashCode(object)));
        }

    }

}
