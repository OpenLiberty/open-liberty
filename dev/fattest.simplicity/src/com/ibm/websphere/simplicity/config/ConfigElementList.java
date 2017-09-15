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
package com.ibm.websphere.simplicity.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides helper methods for lists of ConfigElements
 *
 * @param <E> the ConfigElement type
 */
public class ConfigElementList<E extends ConfigElement> extends ArrayList<E> implements List<E> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an empty list with the specified initial capacity, as per the recommendation in the Collection interface specification.
     */
    public ConfigElementList() {
        super();
    }

    /**
     * Constructs a list containing the elements of the specified collection, in the order they are returned by the collection's iterator, as per the recommendation in the
     * Collection interface specification.
     *
     * @param c the collection whose elements are to be placed into this list
     */
    public ConfigElementList(Collection<E> c) {
        super(c);
    }

    /**
     * Deep copy of the config element list.
     * Each element in the list is cloned.
     *
     * @return deep copy of the config element list.
     */
    @Override
    @SuppressWarnings("unchecked")
    public ConfigElementList<E> clone() {
        ConfigElementList<E> clone = new ConfigElementList<E>();
        for (E element : this)
            try {
                clone.add((E) element.clone());
            } catch (CloneNotSupportedException x) {
                throw new UnsupportedOperationException(x);
            }
        return clone;
    }

    /**
     * Returns the first element in this list with a matching attribute value.
     *
     * @param attributeName the attribute for which to search (Example: jndiName)
     * @param attributeValue the value to match
     * @return the first element in this list with a matching attribute name/value, or null of no such element is found
     */
    public E getBy(String attributeName, String attributeValue) {
        String methodName = "get" + Character.toUpperCase(attributeName.charAt(0)) + attributeName.substring(1);
        for (E element : this)
            if (element != null)
                try {
                    Object value = element.getClass().getMethod(methodName).invoke(element);
                    if (value == attributeValue || value != null && value.equals(attributeValue))
                        return element;
                } catch (Exception x) {
                }
        return null;
    }

    /**
     * Returns the first element in this list with a matching identifier
     *
     * @param id the identifier to search for
     * @return the first element in this list with a matching identifier, or null of no such element is found
     */
    public E getById(String id) {
        if (id == null) {
            for (E element : this) {
                if (element != null && element.getId() == null) {
                    return element;
                }
            }
        } else {
            for (E element : this) {
                if (element != null && id.equals(element.getId())) {
                    return element;
                }
            }
        }
        return null;
    }

    /**
     * Removes the first element in this list with a matching attribute name/value
     *
     * @param attributeName the attribute for which to search (Example: jndiName)
     * @param attributeValue the value to match
     * @return the removed element, or null of no element was removed
     */
    public E removeBy(String attributeName, String attributeValue) {
        // traverses the list twice, but reuse code
        E element = this.getBy(attributeName, attributeValue);
        if (element != null) {
            this.remove(element); // this should always return true since we already found the element
        }
        return element;
    }

    /**
     * Removes the first element in this list with a matching identifier
     *
     * @param id the identifier to search for
     * @return the removed element, or null of no element was removed
     */
    public E removeById(String id) {
        // traverses the list twice, but reuse code
        E element = this.getById(id);
        if (element != null) {
            this.remove(element); // this should always return true since we already found the element
        }
        return element;
    }

    /**
     * Returns the first element in this list with a matching identifier, or adds a new element to the end of this list and sets the identifier
     *
     * @param id the identifier to search for
     * @param type the type of the element to add when no existing element is found. The type MUST have a public no-argument constructor (but it probably does since JAXB requires
     *            it anyway)
     * @return the first element in this list with a matching identifier. Never returns null.
     * @throws InstantiationException if the public no-argument constructor of the element type is not visible
     * @throws IllegalAccessException if the instance could not be created
     */
    public E getOrCreateById(String id, Class<E> type) throws IllegalAccessException, InstantiationException {
        E element = this.getById(id);
        if (element == null) {
            element = type.newInstance();
            element.setId(id);
            this.add(element);
        }
        return element;
    }

}
