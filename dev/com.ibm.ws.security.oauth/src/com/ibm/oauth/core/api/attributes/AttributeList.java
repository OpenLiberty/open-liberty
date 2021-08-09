/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.api.attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * An ordered list of attributes used throughout the component to communicate full
 * state and information in OAuth request and response. An attribute will be uniquely
 * identified in the list by its name and type. Therefore an attribute list
 * cannot have two attributes with the same name and type.
 */
public class AttributeList {
    List<Attribute> _attributes;

    /**
     * One and only constructor which builds a new empty list of attributes.
     */
    public AttributeList() {
        _attributes = new ArrayList<Attribute>();
    }

    /**
     * Returns the first string value of an attribute from the attribute list
     * identified by its name and type. Name and type checking are done as
     * string comparisons. If the type of an attribute is not defined (null)
     * then it will ONLY match if the requested type is also null. Similarly if
     * the requested type is null, only attributes with a null (undefined) type
     * will be matched.
     * 
     * @param name
     *            name of attribute
     * @param type
     *            type of attribute
     * @return first string value of a matching attribute from the attribute
     *         list or null if no matching attribute
     */
    public String getAttributeValueByNameAndType(String name, String type) {
        String result = null;
        ListIterator<Attribute> li = _attributes.listIterator();
        boolean found = false;
        while (li.hasNext() && !found) {
            Attribute a = li.next();
            if (stringsEqual(name, a.getName())
                    && stringsEqual(type, a.getType())) {
                result = a.getValue();
                found = true;
            }
        }
        return result;
    }

    /**
     * Returns the first string value of an attribute from the attribute list
     * identified by its name only. If more than one attribute in the attribute
     * list has the same name (and different types), the first value of the
     * first matching attribute will be returned.
     * 
     * @param name
     *            name of attribute
     * @return first string value of a matching attribute from the attribute
     *         list or null if no matching attribute
     */
    public String getAttributeValueByName(String name) {
        String result = null;
        ListIterator<Attribute> li = _attributes.listIterator();
        boolean found = false;
        while (li.hasNext() && !found) {
            Attribute a = li.next();
            if (stringsEqual(name, a.getName())) {
                result = a.getValue();
                found = true;
            }
        }
        return result;
    }

    /**
     * Returns the list of string values (as a string array) of an attribute
     * from the attribute list identified by its name and type. Name and type
     * checking are done as string comparisons. If the type of an attribute is
     * not defined (null) then it will ONLY match if the requested type is also
     * null. Similarly if the requested type is null, only attributes with a
     * null (undefined) type will be matched.
     * 
     * @param name
     *            name of attribute
     * @param type
     *            type of attribute
     * @return string array of values of a matching attribute from the attribute
     *         list or null if no matching attribute
     */
    public String[] getAttributeValuesByNameAndType(String name, String type) {
        String[] result = null;
        Attribute a = getAttributeByNameAndType(name, type);
        if (a != null) {
            List<String> vals = a.getValues();
            if (vals != null && vals.size() > 0) {
                result = new String[vals.size()];
                for (int i = 0; i < vals.size(); i++) {
                    result[i] = vals.get(i);
                }
            }
        }
        return result;
    }

    /**
     * Returns the list of string values (as a string array) of an attribute
     * from the attribute list identified by its name only. If more than one
     * attribute in the attribute list has the same name (and different types),
     * the values of the first matching attribute will be returned.
     * 
     * @param name
     *            name of attribute
     * @return string array of values of a matching attribute from the attribute
     *         list or null if no matching attribute
     */
    public String[] getAttributeValuesByName(String name) {
        String[] result = null;
        ListIterator<Attribute> li = _attributes.listIterator();
        boolean found = false;
        while (li.hasNext() && !found) {
            Attribute a = li.next();
            if (stringsEqual(name, a.getName())) {
                List<String> vals = a.getValues();
                if (vals != null && vals.size() > 0) {
                    result = new String[vals.size()];
                    for (int i = 0; i < vals.size(); i++) {
                        result[i] = vals.get(i);
                    }
                }
                found = true;
            }
        }
        return result;
    }

    /**
     * Set the values of an attribute in the attribute list identified by name
     * and type. If an existing attribute with that name and type already
     * appears in the attribute list its values will be replaced otherwise a new
     * attribute will be appended to the list.
     * 
     * @param name
     *            - name of attribute to update/add
     * @param type
     *            - type of attribute to update/add
     * @param values
     *            - array of string values of attribute to update/add
     */
    public void setAttribute(String name, String type, String[] values) {
        ListIterator<Attribute> li = _attributes.listIterator();
        Attribute existingAttribute = null;
        boolean found = false;
        while (li.hasNext() && !found) {
            Attribute a = li.next();
            if (stringsEqual(name, a.getName())
                    && stringsEqual(type, a.getType())) {
                existingAttribute = a;
                found = true;
            }
        }
        if (found) {
            existingAttribute.setValues(values);
        } else {
            Attribute newAttribute = new Attribute(name, type, values);
            _attributes.add(newAttribute);
        }
    }

    /**
     * Returns a reference to an attribute within the attribute list identified
     * by its name and type. If the attribute is modified, it will be modified
     * in the list. If no attribute with that name and type exists in the list,
     * returns null.
     * 
     * @param name
     *            - name of attribute
     * @param type
     *            - type of attribute
     * @return reference to the attribute in the list, or null if an attribute
     *         with that name and type cannot be located.
     */
    public Attribute getAttributeByNameAndType(String name, String type) {
        Attribute result = null;
        ListIterator<Attribute> li = _attributes.listIterator();
        boolean found = false;
        while (li.hasNext() && !found) {
            Attribute a = li.next();
            if (stringsEqual(name, a.getName())
                    && stringsEqual(type, a.getType())) {
                result = a;
                found = true;
            }
        }
        return result;
    }

    /**
     * Returns an array of attributes from the list that match a given type.
     * Each attribute is a reference so any modifications to attributes returned
     * in the array will be reflected in the attribute list.
     * 
     * @param type
     *            type of attribute to match
     * @return an array of attributes that match the given type, or null if
     *         there are no attributes with that type in the list.
     */
    public Attribute[] getAttributesByType(String type) {
        Attribute[] result = null;
        List<Attribute> l = new ArrayList<Attribute>();
        for (ListIterator<Attribute> li = _attributes.listIterator(); li
                .hasNext();) {
            Attribute a = li.next();
            if (stringsEqual(type, a.getType())) {
                l.add(a);
            }
        }
        if (l.size() > 0) {
            result = new Attribute[l.size()];
            for (int i = 0; i < l.size(); i++) {
                result[i] = l.get(i);
            }
        }
        return result;
    }

    /**
     * Gets all attributes in the attribute list.
     * @return a list of attributes
     */
    public List<Attribute> getAllAttributes() {
        return Collections.unmodifiableList(_attributes);
    }

    /**
     * Internal helper method to compare name and type strings.
     * 
     * @param s1
     * @param s2
     * @return true if s1 and s2 are both null, or both not null and equal.
     */
    boolean stringsEqual(String s1, String s2) {
        boolean result = false;
        if ((s1 == null && s2 == null)
                || (s1 != null && s2 != null && s1.equals(s2))) {
            result = true;
        }
        return result;
    }

    /**
     * String representation of the attribute list useful for debug tracing.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        for (ListIterator<Attribute> li = _attributes.listIterator(); li
                .hasNext();) {
            sb.append(li.next());
            if (li.hasNext()) {
                sb.append(",");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
